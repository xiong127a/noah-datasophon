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

import akka.actor.ActorSelection;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;
import akka.util.Timeout;
import cn.hutool.http.HttpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datasophon.api.k8s.handler.K8sServiceConfigureHandler;
import com.datasophon.api.load.ServiceRoleJmxMap;
import com.datasophon.api.master.handler.service.ServiceConfigureHandler;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceInstanceService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.utils.SpringTool;
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
import com.datasophon.k8s.constants.Constant;
import com.datasophon.k8s.util.CommonUtil;
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

public class PrometheusActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(PrometheusActor.class);
    private static final String PROMETHEUS_PORT = "9090";
    private static final String PROMETHEUS_NODE_PORT = "30909";
    private static final String RELOAD_PATH = "/-/reload";
    private static final int HTTP_TIMEOUT_MS = 5000;

    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg instanceof GeneratePrometheusConfigCommand) {

            GeneratePrometheusConfigCommand command = (GeneratePrometheusConfigCommand) msg;
            ClusterServiceInstanceService serviceInstanceService =
                    SpringTool.getApplicationContext().getBean(ClusterServiceInstanceService.class);
            ClusterServiceRoleInstanceService roleInstanceService =
                    SpringTool.getApplicationContext()
                            .getBean(ClusterServiceRoleInstanceService.class);
            ClusterServiceInstanceEntity serviceInstance =
                    serviceInstanceService.getById(command.getServiceInstanceId());
            List<ClusterServiceRoleInstanceEntity> roleInstanceList =
                    roleInstanceService.getServiceRoleInstanceListByServiceId(
                            serviceInstance.getId());

            ClusterServiceRoleInstanceEntity prometheusInstance =
                    roleInstanceService.getOneServiceRole(
                            "Prometheus", null, command.getClusterId());

            ClusterInfoService clusterInfoService = SpringTool.getApplicationContext().getBean(ClusterInfoService.class);
            String depType = clusterInfoService.getById(command.getClusterId()).getDepType();
            boolean isK8s = Constants.K8S_MODE.equals(depType);
            logger.info("start to generate {} prometheus config", serviceInstance.getServiceName());
            HashMap<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();

            HashMap<String, List<String>> roleMap = new HashMap<>();
            int index = 0;
            for (ClusterServiceRoleInstanceEntity roleInstanceEntity : roleInstanceList) {
                String serviceRoleFullName = CommonUtil.generateServiceRoleFullName(roleInstanceEntity.getServiceName(), roleInstanceEntity.getServiceRoleName());

                List<String> podNames = (List<String>) CacheUtils.get(serviceRoleFullName + "_" + Constant.POD_NAME);

                String hostname = roleInstanceEntity.getHostname();

                if (isK8s && Objects.nonNull(podNames) && !podNames.isEmpty()) {
                    if (podNames.size() > index) {
                        hostname = podNames.get(index);
                        index++;
                    }
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
            serviceRoleInfo.setName("Prometheus");
            serviceRoleInfo.setParentName("PROMETHEUS");
            serviceRoleInfo.setConfigFileMap(configFileMap);
            serviceRoleInfo.setDecompressPackageName("prometheus-2.17.2");
            if (Objects.nonNull(prometheusInstance)) {
                serviceRoleInfo.setClusterId(prometheusInstance.getClusterId());
                serviceRoleInfo.setHostname(prometheusInstance.getHostname());
                reloadPrometheusConfig(prometheusInstance, isK8s, serviceRoleInfo);
            }
        } else if (msg instanceof GenerateHostPrometheusConfig) {
            GenerateHostPrometheusConfig command = (GenerateHostPrometheusConfig) msg;
            Integer clusterId = command.getClusterId();
            HashMap<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();
            ClusterHostService hostService =
                    SpringTool.getApplicationContext().getBean(ClusterHostService.class);
            ClusterServiceRoleInstanceService roleInstanceService =
                    SpringTool.getApplicationContext()
                            .getBean(ClusterServiceRoleInstanceService.class);
            ClusterInfoService clusterInfoService = SpringTool.getApplicationContext().getBean(ClusterInfoService.class);
            String depType = clusterInfoService.getById(command.getClusterId()).getDepType();
            List<ClusterHostDO> hostList =
                    hostService.list(
                            new QueryWrapper<ClusterHostDO>()
                                    .eq(Constants.MANAGED, 1)
                                    .eq(Constants.CLUSTER_ID, clusterId));
            ClusterServiceRoleInstanceEntity prometheusInstance =
                    roleInstanceService.getOneServiceRole(
                            "Prometheus", null, command.getClusterId());
            boolean isK8s = Constants.K8S_MODE.equals(depType);
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
                    // 非k8s模式才生成worker配置
                    if (!isK8s) {
                        ServiceConfig serviceConfig = new ServiceConfig();
                        serviceConfig.setName("worker_" + clusterHostDO.getHostname());
                        serviceConfig.setValue(clusterHostDO.getHostname() + ":8585");
                        serviceConfig.setRequired(true);
                        workerServiceConfigs.add(serviceConfig);
                    }

                    ServiceConfig nodeServiceConfig = new ServiceConfig();
                    nodeServiceConfig.setName("node_" + clusterHostDO.getHostname());
                    nodeServiceConfig.setValue(clusterHostDO.getHostname() + ":9100");
                    nodeServiceConfig.setRequired(true);
                    nodeServiceConfigs.add(nodeServiceConfig);
                }

                if (!isK8s) {
                    // 如果是非k8s模式，则添加worker配置
                    configFileMap.put(workerGenerators, workerServiceConfigs);
                }
                configFileMap.put(masterGenerators, masterServiceConfigs);
                configFileMap.put(workerGenerators, nodeServiceConfigs);
                ServiceRoleInfo serviceRoleInfo = new ServiceRoleInfo();
                serviceRoleInfo.setName("Prometheus");
                serviceRoleInfo.setParentName("PROMETHEUS");
                serviceRoleInfo.setConfigFileMap(configFileMap);
                serviceRoleInfo.setDecompressPackageName("prometheus-2.17.2");
                serviceRoleInfo.setHostname(prometheusInstance.getHostname());

                reloadPrometheusConfig(prometheusInstance, isK8s, serviceRoleInfo);
            }

        } else if (msg instanceof GenerateAlertConfigCommand) {

            GenerateAlertConfigCommand command = (GenerateAlertConfigCommand) msg;
            ClusterServiceRoleInstanceService roleInstanceService =
                    SpringTool.getApplicationContext()
                            .getBean(ClusterServiceRoleInstanceService.class);
            ClusterServiceRoleInstanceEntity prometheusInstance =
                    roleInstanceService.getOneServiceRole(
                            "Prometheus", null, command.getClusterId());
            ClusterInfoService clusterInfoService = SpringTool.getApplicationContext().getBean(ClusterInfoService.class);
            String depType = clusterInfoService.getById(command.getClusterId()).getDepType();
            if (Objects.nonNull(prometheusInstance)) {
                ExecResult configResult;
                if (Constants.K8S_MODE.equals(depType)) {
                    return;
                } else {
                    ActorSelection alertConfigActor =
                            ActorUtils.actorSystem.actorSelection(
                                    "akka.tcp://datasophon@"
                                            + prometheusInstance.getHostname()
                                            + ":2552/user/worker/alertConfigActor");
                    Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
                    Future<Object> configureFuture = Patterns.ask(alertConfigActor, command, timeout);
                    configResult =
                            (ExecResult) Await.result(configureFuture, timeout.duration());
                }
                if (configResult.getExecResult()) {
                    logger.info("Generate prometheus alert config success , now start to reload prometheus");
                    // reload prometheus config
                    HttpUtil.post(
                            "http://" + prometheusInstance.getHostname() + ":9090/-/reload", "");
                }
            }

        } else if (msg instanceof GenerateSRPromConfigCommand) {
            GenerateSRPromConfigCommand command = (GenerateSRPromConfigCommand) msg;
            ClusterServiceInstanceService serviceInstanceService =
                    SpringTool.getApplicationContext().getBean(ClusterServiceInstanceService.class);
            ClusterServiceRoleInstanceService roleInstanceService =
                    SpringTool.getApplicationContext()
                            .getBean(ClusterServiceRoleInstanceService.class);
            ClusterServiceInstanceEntity serviceInstance =
                    serviceInstanceService.getById(command.getServiceInstanceId());
            List<ClusterServiceRoleInstanceEntity> roleInstanceList =
                    roleInstanceService.getServiceRoleInstanceListByServiceId(
                            serviceInstance.getId());

            ClusterServiceRoleInstanceEntity prometheusInstance =
                    roleInstanceService.getOneServiceRole(
                            "Prometheus", null, command.getClusterId());

            ClusterInfoService clusterInfoService = SpringTool.getApplicationContext().getBean(ClusterInfoService.class);
            String depType = clusterInfoService.getById(command.getClusterId()).getDepType();

            logger.info("start to genetate {} prometheus config", serviceInstance.getServiceName());
            HashMap<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();

            ArrayList<String> feList = new ArrayList<>();
            ArrayList<String> beList = new ArrayList<>();

            for (ClusterServiceRoleInstanceEntity roleInstanceEntity : roleInstanceList) {
                String jmxKey =
                        command.getClusterFrame()
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
            serviceRoleInfo.setName("Prometheus");
            serviceRoleInfo.setParentName("PROMETHEUS");
            serviceRoleInfo.setConfigFileMap(configFileMap);
            serviceRoleInfo.setDecompressPackageName("prometheus-2.17.2");
            serviceRoleInfo.setHostname(prometheusInstance.getHostname());
            boolean isK8s = Constants.K8S_MODE.equals(depType);
            reloadPrometheusConfig(prometheusInstance, isK8s, serviceRoleInfo);
        } else {
            unhandled(msg);
        }
    }

    private void reloadPrometheusConfig(ClusterServiceRoleInstanceEntity prometheusInstance,
                                        boolean isK8s,
                                        ServiceRoleInfo serviceRoleInfo) throws Exception {
        // Validate critical parameters
        if (prometheusInstance == null || prometheusInstance.getHostname() == null) {
            throw new IllegalArgumentException("Invalid prometheus instance");
        }
        ExecResult execResult;
        if (isK8s) {
            K8sServiceConfigureHandler k8sServiceConfigureHandler = new K8sServiceConfigureHandler();
            execResult= k8sServiceConfigureHandler.handlerRequest(serviceRoleInfo);

        } else {
            ServiceConfigureHandler configureHandler = new ServiceConfigureHandler();
            execResult= configureHandler.handlerRequest(serviceRoleInfo);
        }

        if (execResult != null && execResult.getExecResult()) {
            String reloadUrl = buildReloadUrl(prometheusInstance.getHostname(),isK8s);
            HttpUtil.post(reloadUrl, "", HTTP_TIMEOUT_MS);
        }
    }


    private String buildReloadUrl(String hostname,boolean isK8s) {
        return "http://" + hostname + ":" + (isK8s?PROMETHEUS_NODE_PORT:PROMETHEUS_PORT) + RELOAD_PATH;
    }
}
