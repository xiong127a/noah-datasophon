/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.datasophon.api.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.datasophon.api.converter.ClusterServiceInstanceConverter;
import com.datasophon.api.kubernetes.handler.KubernetesServiceConfigureHandler;
import com.datasophon.api.load.ServiceRoleJmxMap;
import com.datasophon.api.master.handler.service.ServiceConfigureHandler;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceInstanceService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.service.PrometheusIntegrationService;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.utils.ClusterInfoUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.GenerateAlertConfigCommand;
import com.datasophon.common.command.GeneratePrometheusConfigCommand;
import com.datasophon.common.command.GenerateSRPromConfigCommand;
import com.datasophon.common.dto.ClusterServiceInstanceDTO;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.common.enums.ClusterType;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.kubernetes.util.CommonUtil;
import com.datasophon.kubernetes.util.KubernetesFreeMakerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Prometheus集成服务实现
 * 替代PrometheusActor，负责生成和管理Prometheus的监控配置文件
 */
@Service
public class PrometheusIntegrationServiceImpl implements PrometheusIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(PrometheusIntegrationServiceImpl.class);
    
    private static final String PROMETHEUS_PORT = "9090";
    private static final String PROMETHEUS_NODE_PORT = "30909";
    private static final String RELOAD_PATH = "/-/reload";
    private static final int HTTP_TIMEOUT_MS = 5000;
    private static final String PROMETHEUS_SERVICE_NAME = "Prometheus";
    private static final String PROMETHEUS_SYMBOL_NAME = "prometheus";
    private static final String UPDATE_SYMBOL_NAME = "update";
    private static final String CONFIG_OUTPUT_DIRECTORY = "configs";
    private static final String CONFIG_FORMAT_CUSTOM = "custom";
    private static final String SCRAPE_TEMPLATE_NAME = "scrape.ftl";
    private static final String PROMETHEUS_PACKAGE_NAME = "prometheus-2.17.2";
    private static final String WORKER_PORT = "8585";
    private static final String NODE_PORT = "9100";
    private static final String MASTER_PORT = "8586";
    private static final String WORKER_CONFIG_FILENAME = "worker.json";
    private static final String NODE_CONFIG_FILENAME = "linux.json";
    private static final String MASTER_CONFIG_FILENAME = "master.json";
    private static final String STARROCKS_TEMPLATE_NAME = "starrocks-prom.ftl";
    private static final String CONFIG_UPDATE_REASON = "prometheus-update";

    @Autowired
    private ClusterServiceInstanceService serviceInstanceService;
    
    @Autowired
    @Lazy
    private ClusterServiceRoleInstanceService roleInstanceService;
    
    @Autowired
    private ClusterServiceInstanceConverter serviceInstanceConverter;
    
    @Autowired
    private ClusterInfoService clusterInfoService;
    
    @Autowired
    private ClusterHostService clusterHostService;

    @Override
    @Async("taskExecutor")
    public void generatePrometheusConfig(GeneratePrometheusConfigCommand command) {
        try {
            Long clusterId = command.getClusterId();
            String namespace = ClusterInfoUtils.getKubernetesNamespace(clusterId);
            
            ClusterServiceInstanceDTO serviceInstance = serviceInstanceConverter
                    .entityToDto(serviceInstanceService.getById(command.getServiceInstanceId()));
            List<ClusterServiceRoleInstanceDTO> roleInstanceList = roleInstanceService
                    .getServiceRoleInstanceListByServiceId(serviceInstance.id());

            ClusterServiceRoleInstanceDTO prometheusInstance = roleInstanceService.getOneServiceRole(
                    PROMETHEUS_SERVICE_NAME, null, clusterId);

            ClusterType depType = clusterInfoService.getById(clusterId).getDepType();
            boolean isKubernetes = depType == ClusterType.KUBERNETES;
            
            logger.info("开始生成 {} prometheus配置", serviceInstance.serviceName());
            HashMap<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();
            HashMap<String, List<String>> roleMap = new HashMap<>();
            Map<String, Integer> roleIndexMap = new HashMap<>();

            String symbolName = PROMETHEUS_SERVICE_NAME;
            for (ClusterServiceRoleInstanceDTO roleInstanceEntity : roleInstanceList) {
                String serviceRoleFullName = CommonUtil.generateServiceRoleFullName(
                        roleInstanceEntity.serviceName(),
                        roleInstanceEntity.serviceRoleName());
                        
                if (StrUtil.equals("prometheus-prometheus", serviceRoleFullName)) {
                    symbolName = PROMETHEUS_SYMBOL_NAME;
                } else {
                    symbolName = UPDATE_SYMBOL_NAME;
                }
                
                String hostname = roleInstanceEntity.hostname();

                if (isKubernetes) {
                    int roleIndex = roleIndexMap.getOrDefault(roleInstanceEntity.serviceRoleName(), 0);

                    if ("ZKFC".equals(roleInstanceEntity.serviceRoleName())) {
                        String namenodeRoleName = "NameNode";
                        String namenodeFullName = CommonUtil
                                .generateServiceRoleFullName(roleInstanceEntity.serviceName(), namenodeRoleName);
                        hostname = namenodeFullName + "-" + roleIndex + "." + namenodeFullName + "."
                                + namespace + ".svc.cluster.local";
                        logger.info("使用NameNode的FQDN for ZKFC: {} for service role {}", hostname,
                                roleInstanceEntity.serviceRoleName());
                    } else {
                        hostname = serviceRoleFullName + "-" + roleIndex + "." + serviceRoleFullName + "."
                                + namespace + ".svc.cluster.local";
                        logger.info("使用Kubernetes FQDN with role-specific index: {} for service role {}",
                                roleIndex, roleInstanceEntity.serviceRoleName());
                    }

                    roleIndexMap.put(roleInstanceEntity.serviceRoleName(), roleIndex + 1);
                }

                roleMap.computeIfAbsent(roleInstanceEntity.serviceRoleName(), k -> new ArrayList<>())
                        .add(hostname);
            }

            for (Map.Entry<String, List<String>> roleEntry : roleMap.entrySet()) {
                Generators generators = new Generators();
                generators.setFilename(roleEntry.getKey().toLowerCase() + ".json");
                generators.setOutputDirectory(CONFIG_OUTPUT_DIRECTORY);
                generators.setConfigFormat(CONFIG_FORMAT_CUSTOM);
                generators.setTemplateName(SCRAPE_TEMPLATE_NAME);
                
                List<String> value = roleEntry.getValue();
                ArrayList<ServiceConfig> serviceConfigs = new ArrayList<>();
                String serviceName = serviceInstance.serviceName();
                String serviceRoleName = roleEntry.getKey();
                String clusterFrame = command.getClusterFrame();
                
                for (String hostname : value) {
                    String jmxKey = clusterFrame + Constants.UNDERLINE + serviceName 
                            + Constants.UNDERLINE + serviceRoleName;
                    if (ServiceRoleJmxMap.exists(jmxKey)) {
                        ServiceConfig serviceConfig = new ServiceConfig();
                        serviceConfig.setName(roleEntry.getKey() + Constants.UNDERLINE + hostname);
                        serviceConfig.setValue(hostname + ":" + ServiceRoleJmxMap.get(jmxKey));
                        serviceConfig.setRequired(true);
                        serviceConfigs.add(serviceConfig);
                    }
                }
                configFileMap.put(generators, serviceConfigs);
            }
            
            ServiceRoleInfo serviceRoleInfo = new ServiceRoleInfo();
            serviceRoleInfo.setClusterId(clusterId);
            serviceRoleInfo.setName(symbolName);
            serviceRoleInfo.setParentName("PROMETHEUS");
            serviceRoleInfo.setConfigFileMap(configFileMap);
            serviceRoleInfo.setDecompressPackageName(PROMETHEUS_PACKAGE_NAME);
            
            if (Objects.nonNull(prometheusInstance)) {
                serviceRoleInfo.setClusterId(prometheusInstance.clusterId());
                serviceRoleInfo.setHostname(prometheusInstance.hostname());
                reloadPrometheusConfig(prometheusInstance, isKubernetes, serviceRoleInfo);
            }
        } catch (Exception e) {
            logger.error("生成Prometheus配置失败", e);
        }
    }

    @Override
    @Async("taskExecutor")
    public void generateStarRocksPrometheusConfig(GenerateSRPromConfigCommand command) {
        try {
            ClusterServiceInstanceDTO serviceInstance = serviceInstanceConverter
                    .entityToDto(serviceInstanceService.getById(command.getServiceInstanceId()));
            List<ClusterServiceRoleInstanceDTO> roleInstanceList = roleInstanceService
                    .getServiceRoleInstanceListByServiceId(serviceInstance.id());

            ClusterServiceRoleInstanceDTO prometheusInstance = roleInstanceService.getOneServiceRole(
                    PROMETHEUS_SERVICE_NAME, null, command.getClusterId());

            ClusterType depType = clusterInfoService.getById(command.getClusterId()).getDepType();

            logger.info("开始生成 {} prometheus配置", serviceInstance.serviceName());
            HashMap<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();

            ArrayList<String> feList = new ArrayList<>();
            ArrayList<String> beList = new ArrayList<>();

            for (ClusterServiceRoleInstanceDTO roleInstanceEntity : roleInstanceList) {
                String jmxKey = command.getClusterFrame() + Constants.UNDERLINE
                        + serviceInstance.serviceName() + Constants.UNDERLINE
                        + roleInstanceEntity.serviceRoleName();
                logger.info("jmxKey is {}", jmxKey);
                
                if ("SRFE".equals(roleInstanceEntity.serviceRoleName())
                        || "SRFEObserver".equals(roleInstanceEntity.serviceRoleName())
                        || "DorisFE".equals(roleInstanceEntity.serviceRoleName())
                        || "DorisFEObserver".equals(roleInstanceEntity.serviceRoleName())) {
                    logger.info(ServiceRoleJmxMap.get(jmxKey));
                    feList.add(roleInstanceEntity.hostname() + ":" + ServiceRoleJmxMap.get(jmxKey));
                } else {
                    beList.add(roleInstanceEntity.hostname() + ":" + ServiceRoleJmxMap.get(jmxKey));
                }
            }
            
            ArrayList<ServiceConfig> serviceConfigs = new ArrayList<>();
            Generators generators = new Generators();
            generators.setFilename(command.getFilename());
            generators.setOutputDirectory(CONFIG_OUTPUT_DIRECTORY);
            generators.setConfigFormat(CONFIG_FORMAT_CUSTOM);
            generators.setTemplateName(STARROCKS_TEMPLATE_NAME);

            ServiceConfig feServiceConfig = new ServiceConfig();
            feServiceConfig.setName("feList");
            feServiceConfig.setValue(feList);
            feServiceConfig.setRequired(true);
            feServiceConfig.setConfigType("map");

            ServiceConfig beServiceConfig = new ServiceConfig();
            beServiceConfig.setName("beList");
            beServiceConfig.setValue(beList);
            beServiceConfig.setConfigType("map");
            beServiceConfig.setRequired(true);
            
            serviceConfigs.add(feServiceConfig);
            serviceConfigs.add(beServiceConfig);
            configFileMap.put(generators, serviceConfigs);

            ServiceRoleInfo serviceRoleInfo = new ServiceRoleInfo();
            serviceRoleInfo.setClusterId(command.getClusterId());
            serviceRoleInfo.setName(PROMETHEUS_SERVICE_NAME);
            serviceRoleInfo.setParentName("PROMETHEUS");
            serviceRoleInfo.setConfigFileMap(configFileMap);
            serviceRoleInfo.setDecompressPackageName(PROMETHEUS_PACKAGE_NAME);
            serviceRoleInfo.setHostname(prometheusInstance.hostname());
            
            boolean isKubernetes = depType == ClusterType.KUBERNETES;
            reloadPrometheusConfig(prometheusInstance, isKubernetes, serviceRoleInfo);
        } catch (Exception e) {
            logger.error("生成StarRocks Prometheus配置失败", e);
        }
    }

    private void reloadPrometheusConfig(ClusterServiceRoleInstanceDTO prometheusInstance,
                                       boolean isKubernetes,
                                       ServiceRoleInfo serviceRoleInfo) throws Exception {
        Long clusterId = serviceRoleInfo.getClusterId();
        
        if (prometheusInstance == null || prometheusInstance.hostname() == null) {
            throw new IllegalArgumentException("Invalid prometheus instance");
        }
        
        ExecResult execResult;
        if (isKubernetes) {
            KubernetesServiceConfigureHandler kubernetesServiceConfigureHandler = new KubernetesServiceConfigureHandler();
            execResult = kubernetesServiceConfigureHandler.handlerRequest(serviceRoleInfo);
        } else {
            ServiceConfigureHandler configureHandler = new ServiceConfigureHandler();
            execResult = configureHandler.handlerRequest(serviceRoleInfo);
        }

        String kubeConfig = clusterInfoService.getKubeConfigByClusterId(clusterId);
        String namespace = ClusterInfoUtils.getKubernetesNamespace(clusterId);
        KubernetesFreeMakerUtils.flushPrometheusConfigsToPVC(kubeConfig, CONFIG_UPDATE_REASON, namespace);
        
        if (execResult != null && execResult.getExecResult()) {
            String reloadUrl = buildReloadUrl(prometheusInstance.hostname(), isKubernetes);
            HttpUtil.post(reloadUrl, "", HTTP_TIMEOUT_MS);
        }
    }

    private String buildReloadUrl(String hostname, boolean isKubernetes) {
        return "http://" + hostname + ":" + (isKubernetes ? PROMETHEUS_NODE_PORT : PROMETHEUS_PORT) + RELOAD_PATH;
    }
    
    @Override
    @Async("taskExecutor")
    public void generateAlertConfig(GenerateAlertConfigCommand command) {
        try {
            Long clusterId = command.getClusterId();
            
            ClusterServiceRoleInstanceDTO prometheusInstance = roleInstanceService.getOneServiceRole(
                    PROMETHEUS_SERVICE_NAME, null, clusterId);
            
            if (prometheusInstance == null) {
                logger.warn("未找到Prometheus实例，跳过告警配置生成: clusterId={}", clusterId);
                return;
            }
            
            ClusterType depType = clusterInfoService.getById(clusterId).getDepType();
            boolean isKubernetes = depType == ClusterType.KUBERNETES;
            
            logger.info("开始生成Prometheus告警配置: clusterId={}", clusterId);
            
            ServiceRoleInfo serviceRoleInfo = new ServiceRoleInfo();
            serviceRoleInfo.setClusterId(clusterId);
            serviceRoleInfo.setName("alertmanager");
            serviceRoleInfo.setParentName("PROMETHEUS");
            serviceRoleInfo.setAlertFileMap(command.getConfigFileMap());
            serviceRoleInfo.setDecompressPackageName(PROMETHEUS_PACKAGE_NAME);
            serviceRoleInfo.setHostname(prometheusInstance.hostname());
            
            reloadPrometheusConfig(prometheusInstance, isKubernetes, serviceRoleInfo);
            
            logger.info("Prometheus告警配置生成完成: clusterId={}", clusterId);
        } catch (Exception e) {
            logger.error("生成Prometheus告警配置失败: clusterId={}", command.getClusterId(), e);
        }
    }
    
    @Override
    public void generateHostPrometheusConfigDelayed(Long clusterId, int delaySeconds) {
        // 使用异步方式延迟生成主机Prometheus配置
        CompletableFuture.runAsync(() -> {
            try {
                if (delaySeconds > 0) {
                    Thread.sleep(delaySeconds * 1000L);
                }
                logger.info("开始生成主机Prometheus配置: clusterId={}", clusterId);
                // TODO: 实现主机Prometheus配置生成逻辑
                // 这里需要根据实际需求调用相应的配置生成方法
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("生成主机Prometheus配置被中断: clusterId={}", clusterId, e);
            } catch (Exception e) {
                logger.error("生成主机Prometheus配置失败: clusterId={}", clusterId, e);
            }
        });
    }
}

