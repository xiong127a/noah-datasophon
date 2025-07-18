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

import akka.actor.AbstractActor;
import akka.actor.ActorSelection;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.Patterns;
import akka.util.Timeout;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.http.HttpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datasophon.api.kubernetes.handler.KubernetesServiceConfigureHandler;
import com.datasophon.api.load.ServiceRoleJmxMap;
import com.datasophon.api.master.handler.service.ServiceConfigureHandler;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceInstanceService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
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
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.kubernetes.util.CommonUtil;
import com.datasophon.kubernetes.util.KubernetesFreeMakerUtils;
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

public class PrometheusActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(PrometheusActor.class);
    private static final String PROMETHEUS_PORT = "9090";
    private static final String PROMETHEUS_NODE_PORT = "30909";
    private static final String RELOAD_PATH = "/-/reload";
    private static final int HTTP_TIMEOUT_MS = 5000;

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
                    ClusterServiceInstanceEntity serviceInstance = serviceInstanceService
                            .getById(command.getServiceInstanceId());
                    List<ClusterServiceRoleInstanceEntity> roleInstanceList = roleInstanceService
                            .getServiceRoleInstanceListByServiceId(
                                    serviceInstance.getId());

                    ClusterServiceRoleInstanceEntity prometheusInstance = roleInstanceService.getOneServiceRole(
                            "Prometheus", null, clusterId);

                    ClusterInfoService clusterInfoService = SpringUtil
                            .getBean(ClusterInfoService.class);

                    String depType = clusterInfoService.getById(clusterId).getDepType();
                    boolean isKubernetes = Constants.KUBERNETES_MODE.equals(depType);
                    logger.info("start to generate {} prometheus config", serviceInstance.getServiceName());
                    HashMap<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();

                    HashMap<String, List<String>> roleMap = new HashMap<>();
                    Map<String, Integer> roleIndexMap = new HashMap<>();

                    // 添加特殊处理ZKFC的映射，将其关联到对应的NameNode
                    String symbolName = "Prometheus";
                    for (ClusterServiceRoleInstanceEntity roleInstanceEntity : roleInstanceList) {
                        String serviceRoleFullName = CommonUtil.generateServiceRoleFullName(
                                roleInstanceEntity.getServiceName(),
                                roleInstanceEntity.getServiceRoleName());
                        if (StrUtil.equals("prometheus-prometheus", serviceRoleFullName)) {
                            symbolName = "prometheus";
                        } else {
                            symbolName = "update";
                        }
                        String hostname = roleInstanceEntity.getHostname();

                        if (isKubernetes) {
                            // 使用服务角色的FQDN命名方式，确保稳定性
                            // 格式:
                            // serviceRoleFullName-{index}.serviceRoleFullName.namespace.svc.cluster.local
                            // 获取当前服务角色类型的索引
                            int roleIndex = roleIndexMap.getOrDefault(roleInstanceEntity.getServiceRoleName(), 0);

                            // 特殊处理ZKFC，使用NameNode的FQDN
                            if ("ZKFC".equals(roleInstanceEntity.getServiceRoleName())) {
                                // 查找对应的NameNode FQDN
                                String namenodeRoleName = "NameNode";
                                String namenodeFullName = CommonUtil
                                        .generateServiceRoleFullName(roleInstanceEntity.getServiceName(),
                                                namenodeRoleName);
                                // ZKFC使用与NameNode相同的索引
                                hostname = namenodeFullName + "-" + roleIndex + "." + namenodeFullName + "."
                                        + namespace + ".svc.cluster.local";
                                logger.info("Using NameNode's FQDN for ZKFC: {} for service role {}", hostname,
                                        roleInstanceEntity.getServiceRoleName());
                            } else {
                                hostname = serviceRoleFullName + "-" + roleIndex + "." + serviceRoleFullName + "."
                                        + namespace + ".svc.cluster.local";
                                logger.info("Using Kubernetes FQDN with role-specific index: {} for service role {}",
                                        roleIndex,
                                        roleInstanceEntity.getServiceRoleName());
                            }

                            // 更新该服务角色类型的索引
                            roleIndexMap.put(roleInstanceEntity.getServiceRoleName(), roleIndex + 1);
                        }

                        if (roleMap.containsKey(roleInstanceEntity.getServiceRoleName())) {
                            List<String> list = roleMap.get(roleInstanceEntity.getServiceRoleName());
                            list.add(hostname);
                        } else {
                            List<String> list = new ArrayList<>();
                            list.add(hostname);
                            roleMap.put(roleInstanceEntity.getServiceRoleName(), list);
                        }
                    }

                    for (Map.Entry<String, List<String>> roleEntry : roleMap.entrySet()) {
                        Generators generators = new Generators();
                        generators.setFilename(roleEntry.getKey().toLowerCase() + ".json");
                        generators.setOutputDirectory("configs");
                        generators.setConfigFormat("custom");
                        generators.setTemplateName("scrape.ftl");
                        List<String> value = roleEntry.getValue();
                        ArrayList<ServiceConfig> serviceConfigs = new ArrayList<>();
                        String serviceName = serviceInstance.getServiceName();
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
                    serviceRoleInfo.setDecompressPackageName("prometheus-2.17.2");
                    if (Objects.nonNull(prometheusInstance)) {
                        serviceRoleInfo.setClusterId(prometheusInstance.getClusterId());
                        serviceRoleInfo.setHostname(prometheusInstance.getHostname());
                        reloadPrometheusConfig(prometheusInstance, isKubernetes, serviceRoleInfo);
                    }
                })
                .match(GenerateHostPrometheusConfig.class, command -> {
                    Integer clusterId = command.getClusterId();
                    HashMap<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();
                    ClusterHostService hostService = SpringUtil.getBean(ClusterHostService.class);
                    ClusterServiceRoleInstanceService roleInstanceService = SpringUtil
                            .getBean(ClusterServiceRoleInstanceService.class);
                    ClusterInfoService clusterInfoService = SpringUtil
                            .getBean(ClusterInfoService.class);
                    String depType = clusterInfoService.getById(command.getClusterId()).getDepType();
                    List<ClusterHostDO> hostList = hostService.list(
                            new QueryWrapper<ClusterHostDO>()
                                    .eq(Constants.MANAGED, 1)
                                    .eq(Constants.CLUSTER_ID, clusterId));
                    ClusterServiceRoleInstanceEntity prometheusInstance = roleInstanceService.getOneServiceRole(
                            "Prometheus", null, command.getClusterId());
                    boolean isKubernetes = Constants.KUBERNETES_MODE.equals(depType);
                    if (Objects.nonNull(prometheusInstance)) {

                        Generators workerGenerators = new Generators();
                        workerGenerators.setFilename("worker.json");
                        workerGenerators.setOutputDirectory("configs");
                        workerGenerators.setConfigFormat("custom");
                        workerGenerators.setTemplateName("scrape.ftl");

                        Generators nodeGenerators = new Generators();
                        nodeGenerators.setFilename("linux.json");
                        nodeGenerators.setOutputDirectory("configs");
                        nodeGenerators.setConfigFormat("custom");
                        nodeGenerators.setTemplateName("scrape.ftl");

                        Generators masterGenerators = new Generators();
                        masterGenerators.setFilename("master.json");
                        masterGenerators.setOutputDirectory("configs");
                        masterGenerators.setConfigFormat("custom");
                        masterGenerators.setTemplateName("scrape.ftl");

                        ArrayList<ServiceConfig> workerServiceConfigs = new ArrayList<>();
                        ArrayList<ServiceConfig> nodeServiceConfigs = new ArrayList<>();
                        ArrayList<ServiceConfig> masterServiceConfigs = new ArrayList<>();

                        ServiceConfig masterConfig = new ServiceConfig();
                        masterConfig.setName("master_" + CacheUtils.get(Constants.HOSTNAME));
                        masterConfig.setValue(CacheUtils.get(Constants.HOSTNAME) + ":8586");
                        masterConfig.setRequired(true);
                        masterServiceConfigs.add(masterConfig);

                        for (ClusterHostDO clusterHostDO : hostList) {
                            // 非Kubernetes模式才生成worker配置
                            if (!isKubernetes) {
                                ServiceConfig serviceConfig = new ServiceConfig();
                                serviceConfig.setName("worker_" + clusterHostDO.getHostname());
                                serviceConfig.setValue(clusterHostDO.getHostname() + ":8585");
                                serviceConfig.setRequired(true);
                                workerServiceConfigs.add(serviceConfig);
                            }

                            ServiceConfig nodeConfig = new ServiceConfig();
                            nodeConfig.setName("node_" + clusterHostDO.getHostname());
                            nodeConfig.setValue(clusterHostDO.getHostname() + ":9100");
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
                        serviceRoleInfo.setName("Prometheus");
                        serviceRoleInfo.setParentName("PROMETHEUS");
                        serviceRoleInfo.setConfigFileMap(configFileMap);
                        serviceRoleInfo.setDecompressPackageName("prometheus-2.17.2");
                        serviceRoleInfo.setHostname(prometheusInstance.getHostname());
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
                    boolean isKubernetes = Constants.KUBERNETES_MODE.equals(depType);

                    ClusterServiceRoleInstanceEntity prometheusInstance = roleInstanceService.getOneServiceRole(
                            "Prometheus", null, clusterId);
                    if (Objects.nonNull(prometheusInstance)) {
                        ExecResult configResult;
                        if (Constants.KUBERNETES_MODE.equals(depType)) {
                            return;
                        } else {
                            ActorSelection alertConfigActor = ActorUtils.actorSystem.actorSelection(
                                    "akka.tcp://datasophon@"
                                            + prometheusInstance.getHostname()
                                            + ":2552/user/worker/alertConfigActor");
                            Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
                            Future<Object> configureFuture = Patterns.ask(alertConfigActor, command, timeout);
                            configResult = (ExecResult) Await.result(configureFuture, timeout.duration());
                        }
                        if (configResult.getExecResult()) {
                            logger.info("Generate prometheus alert config success , now start to reload prometheus");
                            // reload prometheus config
                            HttpUtil.post(
                                    "http://" + prometheusInstance.getHostname() + ":9090/-/reload", "");
                        }
                    }
                })
                .match(GenerateSRPromConfigCommand.class, command -> {
                    ClusterServiceInstanceService serviceInstanceService = SpringUtil
                            .getBean(ClusterServiceInstanceService.class);
                    ClusterServiceRoleInstanceService roleInstanceService = SpringUtil
                            .getBean(ClusterServiceRoleInstanceService.class);
                    ClusterServiceInstanceEntity serviceInstance = serviceInstanceService
                            .getById(command.getServiceInstanceId());
                    List<ClusterServiceRoleInstanceEntity> roleInstanceList = roleInstanceService
                            .getServiceRoleInstanceListByServiceId(
                                    serviceInstance.getId());

                    ClusterServiceRoleInstanceEntity prometheusInstance = roleInstanceService.getOneServiceRole(
                            "Prometheus", null, command.getClusterId());

                    ClusterInfoService clusterInfoService = SpringUtil
                            .getBean(ClusterInfoService.class);
                    String depType = clusterInfoService.getById(command.getClusterId()).getDepType();

                    logger.info("start to genetate {} prometheus config", serviceInstance.getServiceName());
                    HashMap<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();

                    ArrayList<String> feList = new ArrayList<>();
                    ArrayList<String> beList = new ArrayList<>();

                    for (ClusterServiceRoleInstanceEntity roleInstanceEntity : roleInstanceList) {
                        String jmxKey = command.getClusterFrame()
                                + Constants.UNDERLINE
                                + serviceInstance.getServiceName()
                                + Constants.UNDERLINE
                                + roleInstanceEntity.getServiceRoleName();
                        logger.info("jmxKey is {}", jmxKey);
                        if ("SRFE".equals(roleInstanceEntity.getServiceRoleName())
                                || "SRFEObserver".equals(roleInstanceEntity.getServiceRoleName())
                                || "DorisFE".equals(roleInstanceEntity.getServiceRoleName())
                                || "DorisFEObserver".equals(roleInstanceEntity.getServiceRoleName())) {
                            logger.info(ServiceRoleJmxMap.get(jmxKey));
                            feList.add(
                                    roleInstanceEntity.getHostname() + ":" + ServiceRoleJmxMap.get(jmxKey));
                        } else {
                            beList.add(
                                    roleInstanceEntity.getHostname() + ":" + ServiceRoleJmxMap.get(jmxKey));
                        }
                    }
                    ArrayList<ServiceConfig> serviceConfigs = new ArrayList<>();
                    Generators generators = new Generators();
                    generators.setFilename(command.getFilename());
                    generators.setOutputDirectory("configs");
                    generators.setConfigFormat("custom");
                    generators.setTemplateName("starrocks-prom.ftl");

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
                    serviceRoleInfo.setName("Prometheus");
                    serviceRoleInfo.setParentName("PROMETHEUS");
                    serviceRoleInfo.setConfigFileMap(configFileMap);
                    serviceRoleInfo.setDecompressPackageName("prometheus-2.17.2");
                    serviceRoleInfo.setHostname(prometheusInstance.getHostname());
                    boolean isKubernetes = Constants.KUBERNETES_MODE.equals(depType);
                    reloadPrometheusConfig(prometheusInstance, isKubernetes, serviceRoleInfo);
                })
                .matchAny(this::unhandled)
                .build();
    }

    private void reloadPrometheusConfig(ClusterServiceRoleInstanceEntity prometheusInstance,
            boolean isKubernetes,
            ServiceRoleInfo serviceRoleInfo) throws Exception {
        Integer clusterId = serviceRoleInfo.getClusterId();
        // Validate critical parameters
        if (prometheusInstance == null || prometheusInstance.getHostname() == null) {
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
        KubernetesFreeMakerUtils.flushPrometheusConfigsToPVC(kubeConfig, "prometheus-update", clusterId);
        if (execResult != null && execResult.getExecResult()) {
            String reloadUrl = buildReloadUrl(prometheusInstance.getHostname(), isKubernetes);
            HttpUtil.post(reloadUrl, "", HTTP_TIMEOUT_MS);
        }
    }

    private String buildReloadUrl(String hostname, boolean isKubernetes) {
        return "http://" + hostname + ":" + (isKubernetes ? PROMETHEUS_NODE_PORT : PROMETHEUS_PORT) + RELOAD_PATH;
    }
}
