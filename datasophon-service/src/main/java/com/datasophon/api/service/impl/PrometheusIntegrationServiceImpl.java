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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    
    @Autowired
    private com.datasophon.api.scheduler.AsyncTaskScheduler asyncTaskScheduler;

    @Override
    // @Async removed - 改为同步执行，避免Spring线程池卡死问题
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
    // @Async removed - 改为同步执行，避免Spring线程池卡死问题
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
    // @Async removed - 改为同步执行，避免Spring线程池卡死问题
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
        // 使用 db-scheduler 异步执行，避免Spring @Async线程池卡死问题
        logger.info("提交主机Prometheus配置生成任务到db-scheduler: clusterId={}, delaySeconds={}", 
                clusterId, delaySeconds);
        asyncTaskScheduler.executeAsync("prometheus-host-config-gen", () -> {
            try {
                logger.info("开始生成主机Prometheus配置（db-scheduler异步任务）: clusterId={}", clusterId);
                generateHostPrometheusConfigInternal(clusterId);
                logger.info("主机Prometheus配置生成完成: clusterId={}", clusterId);
            } catch (Exception e) {
                logger.error("生成主机Prometheus配置失败: clusterId={}", clusterId, e);
                throw new RuntimeException("生成主机Prometheus配置失败", e);
            }
        }, delaySeconds);
    }
    
    /**
     * 内部实际执行主机Prometheus配置生成逻辑
     * 从PrometheusActor恢复的GenerateHostPrometheusConfig处理逻辑
     * 
     * 生成配置文件：
     * - worker.json: Worker节点监控配置（仅PVM模式）
     * - linux.json: Linux节点监控配置（node_exporter）
     * - master.json: Master节点监控配置
     */
    private void generateHostPrometheusConfigInternal(Long clusterId) throws Exception {
        logger.info("内部执行主机Prometheus配置生成: clusterId={}", clusterId);
        
        HashMap<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();
        
        // 获取集群部署类型
        ClusterType depType = clusterInfoService.getById(clusterId).getDepType();
        boolean isKubernetes = depType == ClusterType.KUBERNETES;
        
        // 获取所有管理的主机列表
        List<ClusterHostEntity> hostList = clusterHostService.getAllManagedHostsByClusterId(clusterId);
        if (hostList == null || hostList.isEmpty()) {
            logger.warn("集群 {} 没有管理的主机，跳过配置生成", clusterId);
            return;
        }
        
        logger.info("集群 {} 共有 {} 个主机，部署类型: {}", clusterId, hostList.size(), depType);
        
        // 获取Prometheus实例
        ClusterServiceRoleInstanceDTO prometheusInstance = roleInstanceService.getOneServiceRole(
                PROMETHEUS_SERVICE_NAME, null, clusterId);
        
        if (prometheusInstance == null) {
            logger.warn("集群 {} 未找到Prometheus实例，跳过配置生成", clusterId);
            return;
        }
        
        // 创建Worker配置生成器（仅PVM模式）
        Generators workerGenerators = new Generators();
        workerGenerators.setFilename(WORKER_CONFIG_FILENAME);
        workerGenerators.setOutputDirectory(CONFIG_OUTPUT_DIRECTORY);
        workerGenerators.setConfigFormat(CONFIG_FORMAT_CUSTOM);
        workerGenerators.setTemplateName(SCRAPE_TEMPLATE_NAME);

        // 创建Node配置生成器（Linux节点监控）
        Generators nodeGenerators = new Generators();
        nodeGenerators.setFilename(NODE_CONFIG_FILENAME);
        nodeGenerators.setOutputDirectory(CONFIG_OUTPUT_DIRECTORY);
        nodeGenerators.setConfigFormat(CONFIG_FORMAT_CUSTOM);
        nodeGenerators.setTemplateName(SCRAPE_TEMPLATE_NAME);

        // 创建Master配置生成器
        Generators masterGenerators = new Generators();
        masterGenerators.setFilename(MASTER_CONFIG_FILENAME);
        masterGenerators.setOutputDirectory(CONFIG_OUTPUT_DIRECTORY);
        masterGenerators.setConfigFormat(CONFIG_FORMAT_CUSTOM);
        masterGenerators.setTemplateName(SCRAPE_TEMPLATE_NAME);

        ArrayList<ServiceConfig> workerServiceConfigs = new ArrayList<>();
        ArrayList<ServiceConfig> nodeServiceConfigs = new ArrayList<>();
        ArrayList<ServiceConfig> masterServiceConfigs = new ArrayList<>();

        // 添加Master配置
        ServiceConfig masterConfig = new ServiceConfig();
        masterConfig.setName("master_" + CacheUtils.get(Constants.HOSTNAME));
        masterConfig.setValue(CacheUtils.get(Constants.HOSTNAME) + ":" + MASTER_PORT);
        masterConfig.setRequired(true);
        masterServiceConfigs.add(masterConfig);

        // 为每个主机添加Worker和Node配置
        for (ClusterHostEntity clusterHostEntity : hostList) {
            // 非Kubernetes模式才生成worker配置
            if (!isKubernetes) {
                ServiceConfig serviceConfig = new ServiceConfig();
                serviceConfig.setName("worker_" + clusterHostEntity.getHostname());
                serviceConfig.setValue(clusterHostEntity.getHostname() + ":" + WORKER_PORT);
                serviceConfig.setRequired(true);
                workerServiceConfigs.add(serviceConfig);
            }

            // 所有模式都生成node_exporter配置
            ServiceConfig nodeConfig = new ServiceConfig();
            nodeConfig.setName("node_" + clusterHostEntity.getHostname());
            nodeConfig.setValue(clusterHostEntity.getHostname() + ":" + NODE_PORT);
            nodeConfig.setRequired(true);
            nodeServiceConfigs.add(nodeConfig);
        }

        // 添加配置到configFileMap
        configFileMap.put(masterGenerators, masterServiceConfigs);
        configFileMap.put(nodeGenerators, nodeServiceConfigs);
        if (!isKubernetes) {
            configFileMap.put(workerGenerators, workerServiceConfigs);
            logger.info("生成Worker配置: {} 个Worker节点", workerServiceConfigs.size());
        }
        
        logger.info("生成Node配置: {} 个Node节点", nodeServiceConfigs.size());
        logger.info("生成Master配置: {} 个Master节点", masterServiceConfigs.size());

        // 准备ServiceRoleInfo
        ServiceRoleInfo serviceRoleInfo = new ServiceRoleInfo();
        serviceRoleInfo.setClusterId(clusterId);
        serviceRoleInfo.setName(PROMETHEUS_SERVICE_NAME);
        serviceRoleInfo.setParentName("PROMETHEUS");
        serviceRoleInfo.setConfigFileMap(configFileMap);
        serviceRoleInfo.setDecompressPackageName(PROMETHEUS_PACKAGE_NAME);
        serviceRoleInfo.setHostname(prometheusInstance.hostname());
        
        // 重新加载Prometheus配置
        reloadPrometheusConfig(prometheusInstance, isKubernetes, serviceRoleInfo);
    }
}

