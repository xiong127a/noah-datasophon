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

import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.load.ServiceConfigMap;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.SimpleClusterVariableService;
import com.datasophon.common.Constants;
import com.datasophon.common.command.ExecuteCmdCommand;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.common.model.ConnectionInfo;
import com.datasophon.common.model.InfoItem;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.dao.entity.ClusterInfoEntity;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class NameNodeHandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {

        private static final String ENABLE_RACK = "enableRack";

        private static final String ENABLE_KERBEROS = "enableKerberos";

        @Override
        public void handler(Long clusterId, List<String> hosts) {

                Map<String, String> globalVariables = GlobalVariables.get(clusterId);

                        SimpleClusterVariableService simpleClusterVariableService = SpringUtil.getBean(SimpleClusterVariableService.class);
        simpleClusterVariableService.generateClusterVariable(globalVariables, clusterId, "${nn1}", hosts.get(0));
        simpleClusterVariableService.generateClusterVariable(globalVariables, clusterId, "${nn2}", hosts.get(1));
        }

        @Override
        public void handlerConfig(Long clusterId, List<ServiceConfig> list) {
                Map<String, String> globalVariables = GlobalVariables.get(clusterId);
                ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);

                boolean enableRack = false;
                boolean enableKerberos = false;
                Map<String, ServiceConfig> map = list.stream()
                .collect(java.util.stream.Collectors.toMap(ServiceConfig::getName, config -> config));

                String key = clusterInfo.getClusterFrame() + Constants.UNDERLINE + "HDFS" + Constants.CONFIG;
                List<ServiceConfig> configs = ServiceConfigMap.get(key);

                for (ServiceConfig config : list) {
                        if (ENABLE_RACK.equals(config.getName())) {
                                if ((Boolean) config.getValue()) {
                                        enableRack = isEnableRack(enableRack, config);
                                }
                        }
                        if (ENABLE_KERBEROS.equals(config.getName())) {
                                enableKerberos = isEnableKerberos(
                                                clusterId, globalVariables, enableKerberos, config, "HDFS");
                        }
                }
                List<ServiceConfig> rackConfigs = new ArrayList<>();
                if (enableRack) {
                        log.info("start to add rack config");
                        addConfigWithRack(globalVariables, map, configs, rackConfigs);
                } else {
                        removeConfigWithRack(list, map, configs);
                }
                list.addAll(rackConfigs);

                ArrayList<ServiceConfig> kbConfigs = new ArrayList<>();
                if (enableKerberos) {
                        addConfigWithKerberos(globalVariables, map, configs, kbConfigs);
                } else {
                        removeConfigWithKerberos(list, map, configs);
                }
                list.addAll(kbConfigs);
        }

        @Override
        public void handlerServiceRoleInfo(ServiceRoleInfo serviceRoleInfo, String hostname) {
                Map<String, String> globalVariables = GlobalVariables.get(serviceRoleInfo.getClusterId());
                if (hostname != null && hostname.equals(globalVariables.get("${nn2}"))) {
                        log.info("set to slave namenode");
                        serviceRoleInfo.setSlave(true);
                        serviceRoleInfo.setSortNum(5);
                }
        }

        @Override
        public void handlerServiceRoleCheck(ClusterServiceRoleInstanceDTO roleInstanceDto,
                                     Map<String, ClusterServiceRoleInstanceDTO> map) {
        // 调用通用方法，传递 executeCmdActor
        performServiceRoleCheck(roleInstanceDto, "executeCmdActor");
    }

    @Override
    public void handlerKubernetesServiceRoleCheck(ClusterServiceRoleInstanceDTO roleInstanceDto,
                                                  Map<String, ClusterServiceRoleInstanceDTO> map) {
        // 调用通用方法，传递空字符串
        performServiceRoleCheck(roleInstanceDto, "");
    }

    @Override
    public ExecuteCmdCommand getCommand(ClusterServiceRoleInstanceDTO roleInstanceDto) {
        Map<String, String> globalVariable = GlobalVariables.get(roleInstanceDto.clusterId());
        String nn2 = globalVariable.get("${nn2}");
        String commandLine = globalVariable.get("${HADOOP_HOME}") + "/bin/hdfs haadmin -getServiceState nn1";
        if (nn2.equals(roleInstanceDto.hostname())) {
            commandLine = globalVariable.get("${HADOOP_HOME}") + "/bin/hdfs haadmin -getServiceState nn2";
        }
        ExecuteCmdCommand cmdCommand = new ExecuteCmdCommand();
        cmdCommand.setCommandLine(commandLine);
        return cmdCommand;
    }

    @Override
    public ConnectionInfo getConnectionInfo(Long clusterId, Integer serviceInstanceId, String serviceHome,
                    Map<String, String> configMap) {
        // 直接调用父类的getConnectionInfo方法
        return super.getConnectionInfo(clusterId, serviceInstanceId, serviceHome, configMap);
    }

    /**
     * 获取HDFS服务特定的连接信息
     */
    @Override
    protected ConnectionInfo.ConnectionInfoBuilder getServiceSpecificConnectionInfo(
                    Long clusterId, Integer serviceInstanceId, Map<String, String> configMap) {
                try {
                        // 1. 获取全局变量
                        Map<String, String> globalVariables = GlobalVariables.get(clusterId);

                        // 2. 获取NameNode主备节点
                        String nn1 = globalVariables.get("${nn1}");
                        String nn2 = globalVariables.get("${nn2}");

                        // 3. 获取服务角色主机列表
                        List<String> nameNodeList = getRoleHosts(clusterId, serviceInstanceId, "NameNode");

                        // 4. 判断是否启用了HA
                        boolean enableHA = nameNodeList.size() > 1;

                        // 5. 判断是否启用了Kerberos
                        boolean enableKerberos = false;
                        // 从configMap中获取Kerberos配置
                        if (configMap.containsKey(ENABLE_KERBEROS)) {
                                enableKerberos = Boolean.parseBoolean(configMap.get(ENABLE_KERBEROS));
                        }

                        // 6. 获取HDFS端口，默认为8020 (RPC端口)，9870 (HTTP端口)
                        String rpcPort = "8020";
                        String httpPort = "9870";

                        // 7. 构建HDFS URI
                        String hdfsUri = "hdfs://" + nn1 + ":" + rpcPort;

                        // 8. 如果启用了HA，修改URI格式
                        String nameservice = globalVariables.get("${nameservice}");
                        // 如果globalVariables中没有nameservice，则尝试从configMap中获取
                        if (nameservice == null && enableHA) {
                                Object nameserviceObj = configMap.get("dfs.nameservices");
                                if (nameserviceObj != null) {
                                        nameservice = nameserviceObj.toString();
                                        log.info("从configMap中获取nameservice: {}", nameservice);
                                }
                        }

                        if (enableHA && nameservice != null) {
                                hdfsUri = "hdfs://" + nameservice;
                        }

                        // 9. 构建WebHDFS URI
                        String webhdfsUri = "http://" + nn1 + ":" + httpPort + "/webhdfs/v1";
                        if (enableHA && nameservice != null) {
                                webhdfsUri = "http://" + nameservice + "/webhdfs/v1";
                        }

                        // 10. 构建基本信息项列表
                        List<InfoItem> basicInfoItems = new ArrayList<>();
                        basicInfoItems.add(new InfoItem("host", "主机", nn1));
                        basicInfoItems.add(new InfoItem("port", "RPC端口", rpcPort));
                        basicInfoItems.add(new InfoItem("httpPort", "HTTP端口", httpPort));
                        basicInfoItems.add(new InfoItem("highAvailability", "高可用", enableHA ? "true" : "false"));

                        // 添加主节点信息（明确标识为主节点）
                        if (nn1 != null) {
                                basicInfoItems.add(new InfoItem("masterNode", "主节点服务器", nn1));
                        }

                        // 添加从节点信息
                        if (enableHA && nn2 != null) {
                                basicInfoItems.add(new InfoItem("slaveNode", "从节点服务器", nn2));
                        }

                        // 如果启用了HA，添加Nameservice信息
                        if (enableHA && nameservice != null) {
                                basicInfoItems.add(new InfoItem("nameservice", "Nameservice", nameservice));
                        }

                        // 11. 构建安全信息项列表
                        List<InfoItem> securityInfoItems = new ArrayList<>();
                        securityInfoItems.add(new InfoItem("kerberos.enabled", "启用Kerberos",
                                        enableKerberos ? "true" : "false"));

                        if (enableKerberos) {
                                // 获取主体信息
                                String principal = configMap.getOrDefault("dfs.namenode.kerberos.principal",
                                                "hdfs/_HOST@HADOOP.COM");
                                principal = principal.replace("_HOST", Objects.requireNonNull(nn1));
                                securityInfoItems.add(new InfoItem("principal", "服务主体", principal));

                                // 将krb5配置文件路径添加到安全信息中
                                securityInfoItems.add(new InfoItem("krb5.conf.path", "Kerberos配置文件", "/etc/krb5.conf"));

                                // 如果配置中有keytab相关配置，也添加到安全信息中
                                String keytabPath = configMap.getOrDefault("dfs.namenode.keytab.file", "");
                                if (keytabPath != null && !keytabPath.isEmpty()) {
                                        securityInfoItems.add(new InfoItem("keytab.path", "密钥表文件", keytabPath));
                                }
                        }

                        // 12. 构建连接信息项列表
                        List<InfoItem> connectInfoItems = new ArrayList<>();
                        connectInfoItems.add(new InfoItem("hdfsUri", "HDFS URI", hdfsUri));
                        connectInfoItems.add(new InfoItem("webhdfsUri", "WebHDFS URI", webhdfsUri));

                        if (enableHA) {
                                // 为每个namenode添加单独的连接URI
                                connectInfoItems.add(new InfoItem("nn1Uri", "NameNode1 URI",
                                                "hdfs://" + nn1 + ":" + rpcPort));
                                if (nn2 != null) {
                                        connectInfoItems.add(new InfoItem("nn2Uri", "NameNode2 URI",
                                                        "hdfs://" + nn2 + ":" + rpcPort));
                                }
                        }

                        // 13. 构建并返回ConnectionInfo.ConnectionInfoBuilder对象
                        return ConnectionInfo.builder()
                                        .basicInfoItems(basicInfoItems)
                                        .securityInfoItems(securityInfoItems)
                                        .connectInfoItems(connectInfoItems)
                                        .hostName(nn1)
                                        // 添加重要键列表，将HDFS URI和WebHDFS URI设置为高亮显示
                                        .importantKeys(Arrays.asList("hdfsUri", "webhdfsUri"));
                } catch (Exception e) {
                        log.error("获取HDFS连接信息出错: {}", e.getMessage(), e);
                        return ConnectionInfo.builder();
                }
        }

}
