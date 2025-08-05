/*
 *
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
 *
 */

package com.datasophon.api.master;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.http.HttpUtil;

import com.datasophon.api.kubernetes.handler.KubernetesServiceConfigureHandler;
import com.datasophon.api.load.ServiceRoleJmxMap;
import com.datasophon.api.master.handler.service.ServiceConfigureHandler;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceInstanceService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.converter.ClusterServiceInstanceConverter;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.utils.ClusterInfoUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.GenerateAlertConfigCommand;
import com.datasophon.common.command.GenerateHostPrometheusConfig;
import com.datasophon.common.command.GeneratePrometheusConfigCommand;
import com.datasophon.common.command.GenerateSRPromConfigCommand;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterHostDO;
import com.datasophon.common.dto.ClusterServiceInstanceDTO;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.kubernetes.util.CommonUtil;
import com.datasophon.kubernetes.util.KubernetesFreeMakerUtils;
import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.japi.pf.ReceiveBuilder;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Prometheus监控配置Actor
 * 负责生成和管理Prometheus的监控配置文件，包括服务发现、告警规则等配置
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-05
 */
public class PrometheusActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(PrometheusActor.class);
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

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(GeneratePrometheusConfigCommand.class, command -> {
                    Integer clusterId = command.getClusterId();
                    String namespace = ClusterInfoUtils.getKubernetesNamespace(clusterId);
                    ClusterServiceInstanceService serviceInstanceService = SpringUtil
                            .getBean(ClusterServiceInstanceService.class);
                    ClusterServiceRoleInstanceService roleInstanceService = SpringUtil
                            .getBean(ClusterServiceRoleInstanceService.class);
                    ClusterServiceInstanceConverter serviceInstanceConverter = SpringUtil
                            .getBean(ClusterServiceInstanceConverter.class);
                    ClusterServiceInstanceDTO serviceInstance = serviceInstanceConverter
                            .entityToDto(serviceInstanceService.getById(command.getServiceInstanceId()));
                    List<ClusterServiceRoleInstanceDTO> roleInstanceList = roleInstanceService
                            .getServiceRoleInstanceListByServiceId(
                                    serviceInstance.id());

                    ClusterServiceRoleInstanceDTO prometheusInstance = roleInstanceService.getOneServiceRole(
                            PROMETHEUS_SERVICE_NAME, null, clusterId);

                    ClusterInfoService clusterInfoService = SpringUtil
                            .getBean(ClusterInfoService.class);

                    String depType = clusterInfoService.getById(clusterId).getDepType();
                    boolean isKubernetes = Constants.KUBERNETES_MODE.equals(depType);
                    logger.info("start to generate {} prometheus config", serviceInstance.serviceName());
                    HashMap<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();

                    HashMap<String, List<String>> roleMap = new HashMap<>();
                    Map<String, Integer> roleIndexMap = new HashMap<>();

                    // 添加特殊处理ZKFC的映射，将其关联到对应的NameNode
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
                            // 使用服务角色的FQDN命名方式，确保稳定性
                            // 格式:
                            // serviceRoleFullName-{index}.serviceRoleFullName.namespace.svc.cluster.local
                            // 获取当前服务角色类型的索引
                            int roleIndex = roleIndexMap.getOrDefault(roleInstanceEntity.serviceRoleName(), 0);

                            // 特殊处理ZKFC，使用NameNode的FQDN
                            if ("ZKFC".equals(roleInstanceEntity.serviceRoleName())) {
                                // 查找对应的NameNode FQDN
                                String namenodeRoleName = "NameNode";
                                String namenodeFullName = CommonUtil
                                        .generateServiceRoleFullName(roleInstanceEntity.serviceName(),
                                                namenodeRoleName);
                                // ZKFC使用与NameNode相同的索引
                                hostname = namenodeFullName + "-" + roleIndex + "." + namenodeFullName + "."
                                        + namespace + ".svc.cluster.local";
                                logger.info("Using NameNode's FQDN for ZKFC: {} for service role {}", hostname,
                                        roleInstanceEntity.serviceRoleName());
                            } else {
                                hostname = serviceRoleFullName + "-" + roleIndex + "." + serviceRoleFullName + "."
                                        + namespace + ".svc.cluster.local";
                                logger.info("Using Kubernetes FQDN with role-specific index: {} for service role {}",
                                        roleIndex,
                                        roleInstanceEntity.serviceRoleName());
                            }

                            // 更新该服务角色类型的索引
                            roleIndexMap.put(roleInstanceEntity.serviceRoleName(), roleIndex + 1);
                        }

                        if (roleMap.containsKey(roleInstanceEntity.serviceRoleName())) {
                            List<String> list = roleMap.get(roleInstanceEntity.serviceRoleName());
                            list.add(hostname);
                        } else {
                            List<String> list = new ArrayList<>();
                            list.add(hostname);
                            roleMap.put(roleInstanceEntity.serviceRoleName(), list);
                        }
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
                            String jmxKey = clusterFrame
                                    + Constants.UNDERLINE
                                    + serviceName
                                    + Constants.UNDERLINE
                                    + serviceRoleName;
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
                })
                .match(GenerateHostPrometheusConfig.class, command -> {
                    Integer clusterId = command.getClusterId();
                    HashMap<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();
                    ClusterServiceRoleInstanceService roleInstanceService = SpringUtil
                            .getBean(ClusterServiceRoleInstanceService.class);
                    ClusterInfoService clusterInfoService = SpringUtil
                            .getBean(ClusterInfoService.class);
                    ClusterHostService clusterHostService = SpringUtil
                            .getBean(ClusterHostService.class);
                    String depType = clusterInfoService.getById(command.getClusterId()).getDepType();

                    List<ClusterHostDO> hostList = clusterHostService
                            .getAllManagedHostsByClusterId(clusterId);

                    ClusterServiceRoleInstanceDTO prometheusInstance = roleInstanceService.getOneServiceRole(
                            PROMETHEUS_SERVICE_NAME, null, command.getClusterId());
                    boolean isKubernetes = Constants.KUBERNETES_MODE.equals(depType);
                    if (Objects.nonNull(prometheusInstance)) {

                        Generators workerGenerators = new Generators();
                        workerGenerators.setFilename(WORKER_CONFIG_FILENAME);
                        workerGenerators.setOutputDirectory(CONFIG_OUTPUT_DIRECTORY);
                        workerGenerators.setConfigFormat(CONFIG_FORMAT_CUSTOM);
                        workerGenerators.setTemplateName(SCRAPE_TEMPLATE_NAME);

                        Generators nodeGenerators = new Generators();
                        nodeGenerators.setFilename(NODE_CONFIG_FILENAME);
                        nodeGenerators.setOutputDirectory(CONFIG_OUTPUT_DIRECTORY);
                        nodeGenerators.setConfigFormat(CONFIG_FORMAT_CUSTOM);
                        nodeGenerators.setTemplateName(SCRAPE_TEMPLATE_NAME);

                        Generators masterGenerators = new Generators();
                        masterGenerators.setFilename(MASTER_CONFIG_FILENAME);
                        masterGenerators.setOutputDirectory(CONFIG_OUTPUT_DIRECTORY);
                        masterGenerators.setConfigFormat(CONFIG_FORMAT_CUSTOM);
                        masterGenerators.setTemplateName(SCRAPE_TEMPLATE_NAME);

                        ArrayList<ServiceConfig> workerServiceConfigs = new ArrayList<>();
                        ArrayList<ServiceConfig> nodeServiceConfigs = new ArrayList<>();
                        ArrayList<ServiceConfig> masterServiceConfigs = new ArrayList<>();

                        ServiceConfig masterConfig = new ServiceConfig();
                        masterConfig.setName("master_" + CacheUtils.get(Constants.HOSTNAME));
                        masterConfig.setValue(CacheUtils.get(Constants.HOSTNAME) + ":" + MASTER_PORT);
                        masterConfig.setRequired(true);
                        masterServiceConfigs.add(masterConfig);

                        for (ClusterHostDO clusterHostDO : hostList) {
                            // 非Kubernetes模式才生成worker配置
                            if (!isKubernetes) {
                                ServiceConfig serviceConfig = new ServiceConfig();
                                serviceConfig.setName("worker_" + clusterHostDO.getHostname());
                                serviceConfig.setValue(clusterHostDO.getHostname() + ":" + WORKER_PORT);
                                serviceConfig.setRequired(true);
                                workerServiceConfigs.add(serviceConfig);
                            }

                            ServiceConfig nodeConfig = new ServiceConfig();
                            nodeConfig.setName("node_" + clusterHostDO.getHostname());
                            nodeConfig.setValue(clusterHostDO.getHostname() + ":" + NODE_PORT);
                            nodeConfig.setRequired(true);
                            nodeServiceConfigs.add(nodeConfig);
                        }

                        configFileMap.put(masterGenerators, masterServiceConfigs);
                        configFileMap.put(nodeGenerators, nodeServiceConfigs);
                        if (!isKubernetes) {
                            configFileMap.put(workerGenerators, workerServiceConfigs);
                        }

                        ServiceRoleInfo serviceRoleInfo = new ServiceRoleInfo();
                        serviceRoleInfo.setClusterId(clusterId);
                        serviceRoleInfo.setName(PROMETHEUS_SERVICE_NAME);
                        serviceRoleInfo.setParentName("PROMETHEUS");
                        serviceRoleInfo.setConfigFileMap(configFileMap);
                        serviceRoleInfo.setDecompressPackageName(PROMETHEUS_PACKAGE_NAME);
                        serviceRoleInfo.setHostname(prometheusInstance.hostname());
                        reloadPrometheusConfig(prometheusInstance, isKubernetes, serviceRoleInfo);
                    }
                })
                .match(GenerateAlertConfigCommand.class, command -> {
                    Integer clusterId = command.getClusterId();
                    ClusterServiceRoleInstanceService roleInstanceService = SpringUtil
                            .getBean(ClusterServiceRoleInstanceService.class);
                    ClusterInfoService clusterInfoService = SpringUtil
                            .getBean(ClusterInfoService.class);
                    String depType = clusterInfoService.getById(clusterId).getDepType();

                    ClusterServiceRoleInstanceDTO prometheusInstance = roleInstanceService.getOneServiceRole(
                            PROMETHEUS_SERVICE_NAME, null, clusterId);
                    if (Objects.nonNull(prometheusInstance)) {
                        ExecResult configResult;
                        if (Constants.KUBERNETES_MODE.equals(depType)) {
                            return;
                        } else {
                            ActorSelection alertConfigActor = ActorUtils.actorSystem.actorSelection(
                                    "akka.tcp://datasophon@"
                                            + prometheusInstance.hostname()
                                            + ":2552/user/worker/alertConfigActor");
                            Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
                            Future<Object> configureFuture = Patterns.ask(alertConfigActor, command, timeout);
                            configResult = (ExecResult) Await.result(configureFuture, timeout.duration());
                        }
                        if (configResult.getExecResult()) {
                            logger.info("Generate prometheus alert config success , now start to reload prometheus");
                            // reload prometheus config
                            HttpUtil.post(
                                    "http://" + prometheusInstance.hostname() + ":" + PROMETHEUS_PORT + RELOAD_PATH,
                                    "");
                        }
                    }
                })
                .match(GenerateSRPromConfigCommand.class, command -> {
                    ClusterServiceInstanceService serviceInstanceService = SpringUtil
                            .getBean(ClusterServiceInstanceService.class);
                    ClusterServiceRoleInstanceService roleInstanceService = SpringUtil
                            .getBean(ClusterServiceRoleInstanceService.class);
                    ClusterServiceInstanceConverter serviceInstanceConverter = SpringUtil
                            .getBean(ClusterServiceInstanceConverter.class);
                    ClusterServiceInstanceDTO serviceInstance = serviceInstanceConverter
                            .entityToDto(serviceInstanceService.getById(command.getServiceInstanceId()));
                    List<ClusterServiceRoleInstanceDTO> roleInstanceList = roleInstanceService
                            .getServiceRoleInstanceListByServiceId(
                                    serviceInstance.id());

                    ClusterServiceRoleInstanceDTO prometheusInstance = roleInstanceService.getOneServiceRole(
                            PROMETHEUS_SERVICE_NAME, null, command.getClusterId());

                    ClusterInfoService clusterInfoService = SpringUtil
                            .getBean(ClusterInfoService.class);
                    String depType = clusterInfoService.getById(command.getClusterId()).getDepType();

                    logger.info("start to genetate {} prometheus config", serviceInstance.serviceName());
                    HashMap<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();

                    ArrayList<String> feList = new ArrayList<>();
                    ArrayList<String> beList = new ArrayList<>();

                    for (ClusterServiceRoleInstanceDTO roleInstanceEntity : roleInstanceList) {
                        String jmxKey = command.getClusterFrame()
                                + Constants.UNDERLINE
                                + serviceInstance.serviceName()
                                + Constants.UNDERLINE
                                + roleInstanceEntity.serviceRoleName();
                        logger.info("jmxKey is {}", jmxKey);
                        if ("SRFE".equals(roleInstanceEntity.serviceRoleName())
                                || "SRFEObserver".equals(roleInstanceEntity.serviceRoleName())
                                || "DorisFE".equals(roleInstanceEntity.serviceRoleName())
                                || "DorisFEObserver".equals(roleInstanceEntity.serviceRoleName())) {
                            logger.info(ServiceRoleJmxMap.get(jmxKey));
                            feList.add(
                                    roleInstanceEntity.hostname() + ":" + ServiceRoleJmxMap.get(jmxKey));
                        } else {
                            beList.add(
                                    roleInstanceEntity.hostname() + ":" + ServiceRoleJmxMap.get(jmxKey));
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
                    boolean isKubernetes = Constants.KUBERNETES_MODE.equals(depType);
                    reloadPrometheusConfig(prometheusInstance, isKubernetes, serviceRoleInfo);
                })
                .matchAny(this::unhandled)
                .build();
    }

    private void reloadPrometheusConfig(ClusterServiceRoleInstanceDTO prometheusInstance,
            boolean isKubernetes,
            ServiceRoleInfo serviceRoleInfo) {
        Integer clusterId = serviceRoleInfo.getClusterId();
        // Validate critical parameters
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

        ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
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
}
