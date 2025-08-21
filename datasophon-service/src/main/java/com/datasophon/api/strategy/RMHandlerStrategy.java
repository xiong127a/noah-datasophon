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

package com.datasophon.api.strategy;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.load.ServiceConfigMap;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterYarnSchedulerService;
import com.datasophon.api.service.SimpleClusterVariableService;
import com.datasophon.common.Constants;
import com.datasophon.common.command.ExecuteCmdCommand;
import com.datasophon.common.model.ConnectionInfo;
import com.datasophon.common.model.InfoItem;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.common.dto.ClusterYarnSchedulerDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ResourceManager处理策略
 */
@Service("RMHandlerStrategy")
public class RMHandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {

        private static final Logger logger = LoggerFactory.getLogger(RMHandlerStrategy.class);

        @Override
        public void handler(Long clusterId, List<String> hosts) {

                Map<String, String> globalVariables = GlobalVariables.get(clusterId);

                        SimpleClusterVariableService simpleClusterVariableService = SpringUtil.getBean(SimpleClusterVariableService.class);
        simpleClusterVariableService.generateClusterVariable(globalVariables, clusterId, "${rm1}", hosts.get(0));
        simpleClusterVariableService.generateClusterVariable(globalVariables, clusterId, "${rm2}", hosts.get(1));
        simpleClusterVariableService.generateClusterVariable(
                                globalVariables, clusterId, "${rmHost}", String.join(",", hosts));
        }

        @Override
        public void handlerConfig(Long clusterId, List<ServiceConfig> list) {
                ClusterYarnSchedulerService schedulerService = SpringUtil
                                .getBean(ClusterYarnSchedulerService.class);
                Map<String, String> globalVariables = GlobalVariables.get(clusterId);
                ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
                boolean enableKerberos = false;
                Map<String, ServiceConfig> map = list.stream()
                .collect(java.util.stream.Collectors.toMap(ServiceConfig::getName, config -> config));
                for (ServiceConfig config : list) {
                        if ("yarn.resourcemanager.scheduler.class".equals(config.getName())) {
                                ClusterYarnSchedulerDTO schedulerDto = schedulerService.getScheduler(clusterId);
                                if ("org.apache.hadoop.yarn.server.resourcemanager.scheduler.fair.FairScheduler"
                                                .equals(config.getValue())) {
                                        if ("capacity".equals(schedulerDto.scheduler())) {
                                                // 需要更新为fair调度器 - 使用合适的更新方法
                                                // TODO: 实现调度器类型更新逻辑
                                                log.info("需要将调度器从capacity更新为fair，clusterId: {}", clusterId);
                                        }
                                } else {
                                        if ("fair".equals(schedulerDto.scheduler())) {
                                                // 需要更新为capacity调度器 - 使用合适的更新方法
                                                // TODO: 实现调度器类型更新逻辑
                                                log.info("需要将调度器从fair更新为capacity，clusterId: {}", clusterId);
                                        }
                                }
                        }
                        if ("enableKerberos".equals(config.getName())) {
                                enableKerberos = isEnableKerberos(
                                                clusterId, globalVariables, enableKerberos, config, "YARN");
                        }
                }
                String key = clusterInfo.getClusterFrame() + Constants.UNDERLINE + "YARN" + Constants.CONFIG;
                List<ServiceConfig> configs = ServiceConfigMap.get(key);
                ArrayList<ServiceConfig> kbConfigs = new ArrayList<>();
                if (enableKerberos) {
                        addConfigWithKerberos(globalVariables, map, configs, kbConfigs);
                } else {
                        removeConfigWithKerberos(list, map, configs);
                }
                list.addAll(kbConfigs);
        }

        @Override
        public void handlerServiceRoleCheck(
                        ClusterServiceRoleInstanceDTO roleInstanceDto,
                        Map<String, ClusterServiceRoleInstanceDTO> map) {
                // 调用通用方法，传递特定的actorPath
                performServiceRoleCheck(roleInstanceDto, "rMStateActor");
        }

        @Override
        public void handlerKubernetesServiceRoleCheck(ClusterServiceRoleInstanceDTO roleInstanceDto,
                                                      Map<String, ClusterServiceRoleInstanceDTO> map) {
                // 调用通用方法，传递特定的actorPath
                performServiceRoleCheck(roleInstanceDto, "");
        }

        @Override
        public ExecuteCmdCommand getCommand(ClusterServiceRoleInstanceDTO roleInstanceDto) {
                Map<String, String> globalVariable = GlobalVariables.get(roleInstanceDto.clusterId());
                String rm2 = globalVariable.get("${rm2}");
                String commandLine = globalVariable.get("${HADOOP_HOME}") + "/bin/yarn rmadmin -getServiceState rm1";
                if (rm2.equals(roleInstanceDto.hostname())) {
                        commandLine = globalVariable.get("${HADOOP_HOME}") + "/bin/yarn rmadmin -getServiceState rm2";
                }
                ExecuteCmdCommand command = new ExecuteCmdCommand();
                command.setCommandLine(commandLine);
                return command;
        }

        @Override
        protected ConnectionInfo.ConnectionInfoBuilder getServiceSpecificConnectionInfo(
                        Long clusterId, Long serviceInstanceId, Map<String, String> configMap) {
                try {
                        logger.info("开始获取ResourceManager服务连接信息，集群ID: {}, 服务实例ID: {}", clusterId, serviceInstanceId);

                        // 获取全局变量
                        Map<String, String> globalVariables = GlobalVariables.get(clusterId);

                        // 获取ResourceManager主备节点
                        String rm1 = globalVariables.get("${rm1}");
                        String rm2 = globalVariables.get("${rm2}");

                        // 获取服务角色主机列表
                        List<String> rmList = getRoleHosts(clusterId, serviceInstanceId, "ResourceManager");

                        // 判断是否启用了HA
                        boolean enableHA = rmList.size() > 1;

                        // 判断是否启用了Kerberos
                        boolean enableKerberos = false;
                        if (configMap.containsKey("enableKerberos")) {
                                enableKerberos = Boolean.parseBoolean(configMap.get("enableKerberos"));
                        }

                        // 获取YARN端口
                        String webPort = configMap.getOrDefault("yarn.resourcemanager.webapp.address.rm1", "8088")
                                        .split(":")[1];
                        String submissionPort = configMap.getOrDefault("yarn.resourcemanager.address.rm1", "8032")
                                        .split(":")[1];

                        // 主节点信息
                        String masterNode = rmList.isEmpty() ? "localhost" : rmList.getFirst();

                        // 构建连接字符串
                        String rmAddress = masterNode + ":" + submissionPort;
                        String rmWebAddress = masterNode + ":" + webPort;

                        // 如果启用了HA，修改地址格式
                        String haId = configMap.getOrDefault("yarn.resourcemanager.ha.id", "");
                        String clusterIds = configMap.getOrDefault("yarn.resourcemanager.ha.rm-ids", "");

                        if (enableHA && rm2 != null) {
                                rmAddress = rm1 + ":" + submissionPort + "," + rm2 + ":" + submissionPort;
                                // 集群ID可能格式为 "rm1,rm2"
                                if (!StrUtil.isBlank(clusterIds)) {
                                        rmAddress = clusterIds.split(",")[0] + "=" + rm1 + ":" + submissionPort
                                                        + "," + clusterIds.split(",")[1] + "=" + rm2 + ":"
                                                        + submissionPort;
                                }
                        }

                        // 获取调度器类型
                        String schedulerType = "Capacity Scheduler"; // 默认值
                        if (configMap.containsKey("yarn.resourcemanager.scheduler.class")) {
                                String schedulerClass = configMap.get("yarn.resourcemanager.scheduler.class");
                                if (schedulerClass.contains("FairScheduler")) {
                                        schedulerType = "Fair Scheduler";
                                }
                        }

                        // 构建基本信息项列表
                        List<InfoItem> basicInfoItems = new ArrayList<>();
                        basicInfoItems.add(new InfoItem("host", "主机", masterNode));
                        basicInfoItems.add(new InfoItem("port", "提交端口", submissionPort));
                        basicInfoItems.add(new InfoItem("webPort", "Web UI端口", webPort));
                        basicInfoItems.add(new InfoItem("connectString", "连接地址", rmAddress));
                        basicInfoItems.add(new InfoItem("webAddress", "Web UI地址", "http://" + rmWebAddress));
                        basicInfoItems.add(new InfoItem("schedulerType", "调度器类型", schedulerType));
                        basicInfoItems.add(new InfoItem("deployMode", "部署模式", enableHA ? "高可用模式" : "单节点模式"));

                        if (enableHA) {
                                basicInfoItems.add(new InfoItem("rm1", "主节点", rm1));
                                basicInfoItems.add(new InfoItem("rm2", "备节点", rm2));
                                if (!StrUtil.isBlank(clusterIds)) {
                                        basicInfoItems.add(new InfoItem("clusterIds", "集群ID", clusterIds));
                                }
                                if (!StrUtil.isBlank(haId)) {
                                        basicInfoItems.add(new InfoItem("haId", "HA ID", haId));
                                }
                        }

                        // 构建安全信息项列表
                        List<InfoItem> securityInfoItems = new ArrayList<>();
                        securityInfoItems.add(new InfoItem("kerberos", "启用Kerberos", enableKerberos ? "是" : "否"));

                        // 获取Kerberos配置
                        String kerberosUser = configMap.getOrDefault("yarn.resourcemanager.principal", "");
                        String kerberosKeytab = configMap.getOrDefault("yarn.resourcemanager.keytab", "");

                        if (enableKerberos) {
                                securityInfoItems.add(new InfoItem("principal", "Kerberos主体", kerberosUser));
                                securityInfoItems.add(new InfoItem("keytab", "Keytab路径", kerberosKeytab));
                        }

                        // 构建连接信息项列表
                        List<InfoItem> connectInfoItems = new ArrayList<>();
                        connectInfoItems.add(new InfoItem("rmAddress", "ResourceManager地址", rmAddress));
                        connectInfoItems.add(new InfoItem("webUI", "Web UI地址", "http://" + rmWebAddress));
                        connectInfoItems.add(new InfoItem("restAPI", "REST API地址",
                                        "http://" + rmWebAddress + "/ws/v1/cluster"));

                        // 命令行工具地址
                        connectInfoItems.add(new InfoItem("commandLine", "命令行工具", "${HADOOP_HOME}/bin/yarn"));

                        // 构建重要键列表
                        List<String> importantKeys = Arrays.asList("connectString", "webAddress");

                        // 构建连接信息对象
                        logger.info("ResourceManager连接信息生成成功");
                        return ConnectionInfo.builder()
                                        .basicInfoItems(basicInfoItems)
                                        .securityInfoItems(securityInfoItems)
                                        .connectInfoItems(connectInfoItems)
                                        .hostName(masterNode)
                                        .importantKeys(importantKeys);
                } catch (Exception e) {
                        logger.error("获取ResourceManager连接信息出错: {}", e.getMessage(), e);
                        return ConnectionInfo.builder();
                }
        }

        /**
         * 创建模板变量，用于渲染模板
         */
        private Map<String, Object> createTemplateVariables(String rmAddress, String rmWebAddress,
                        boolean enableHA, boolean enableKerberos, String principal,
                        String keytabPath, String schedulerType, String rm1, String rm2) {
                Map<String, Object> templateVariables = new HashMap<>();
                templateVariables.put("rmAddress", rmAddress);
                templateVariables.put("rmWebAddress", rmWebAddress);
                templateVariables.put("enableHA", enableHA);
                templateVariables.put("enableKerberos", enableKerberos);
                templateVariables.put("schedulerType", schedulerType);
                templateVariables.put("rm1", rm1);
                templateVariables.put("rm2", rm2);

                if (enableKerberos) {
                        templateVariables.put("principal", principal);
                        templateVariables.put("keytabPath", keytabPath);
                }

                return templateVariables;
        }

}
