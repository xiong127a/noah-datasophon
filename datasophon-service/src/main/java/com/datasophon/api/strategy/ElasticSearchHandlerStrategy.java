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

package com.datasophon.api.strategy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.service.SimpleClusterVariableService;
import com.datasophon.api.utils.ClusterInfoUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.enums.ClusterType;
import com.datasophon.common.model.ConnectionInfo;
import com.datasophon.common.model.InfoItem;
import com.datasophon.common.model.ServiceConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.datasophon.api.utils.ProcessUtils.getDepMode;
import static com.datasophon.common.utils.HostUtils.generateDnsName;

public class ElasticSearchHandlerStrategy extends ServiceHandlerAbstract implements ServiceRoleStrategy {

    @Override
    public void handler(Long clusterId, List<String> hosts) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        ClusterType depMode = getDepMode(clusterId);
        String namespace = ClusterInfoUtils.getKubernetesNamespace(clusterId);
        SimpleClusterVariableService simpleClusterVariableService = SpringUtil.getBean(SimpleClusterVariableService.class);
        
        if (depMode == ClusterType.KUBERNETES) {
            hosts = generateDnsName(hosts, "elasticsearch-elasticsearch",namespace);
        }
        simpleClusterVariableService.generateClusterVariable(globalVariables, clusterId, "${initMasterNodes}",
                                String.join(",", hosts));
        String seedHosts = hosts.stream()
                .map(host -> host + ":9300")
                .collect(Collectors.joining(","));
        simpleClusterVariableService.generateClusterVariable(globalVariables, clusterId, "${seedHosts}", seedHosts);
        if (CollUtil.isNotEmpty(hosts)) {
            simpleClusterVariableService.generateClusterVariable(globalVariables, clusterId, "${esSingleHost}",
                                        hosts.getFirst());
        }

    }

    @Override
    public void handlerConfig(Long clusterId, List<ServiceConfig> list) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        SimpleClusterVariableService simpleClusterVariableService = SpringUtil.getBean(SimpleClusterVariableService.class);
        
        for (ServiceConfig config : list) {
            if ("http.port".equals(config.getName())) {
                simpleClusterVariableService.generateClusterVariable(globalVariables, clusterId, "${esHttpPort}",
                                                Convert.toStr(config.getValue()));
            }
        }
    }

    /**
     * 获取ElasticSearch连接信息
     *
     * @param clusterId         集群ID
     * @param serviceInstanceId 服务实例ID
         * @param configMap         配置参数Map
         * @return ConnectionInfo.ConnectionInfoBuilder 对象
     */
    @Override
        protected ConnectionInfo.ConnectionInfoBuilder getServiceSpecificConnectionInfo(
                        Long clusterId, Integer serviceInstanceId, Map<String, String> configMap) {
        try {
                        log.info("开始获取ElasticSearch服务连接信息，集群ID: {}, 服务实例ID: {}", clusterId, serviceInstanceId);

                        // 获取ES节点列表
            List<String> esNodes = getRoleHosts(clusterId, serviceInstanceId, "ElasticSearch");
            if (CollUtil.isEmpty(esNodes)) {
                log.warn("未找到ElasticSearch节点，集群ID: {}", clusterId);
                                return ConnectionInfo.builder();
                        }

                        log.info("ElasticSearch节点数量: {}", esNodes.size());

                        // 获取端口配置
                        String httpPort = configMap.getOrDefault("es_http_port", "9200");
                        String tcpPort = configMap.getOrDefault("es_transport_port", "9300");

                        // 判断是否启用了安全认证
                        boolean enableSecurity = !StrUtil.isBlank(configMap.get("es_username")) &&
                                        !StrUtil.isBlank(configMap.get("es_password"));
                        String securityUser = configMap.getOrDefault("es_username", "elastic");
                        String securityPassword = configMap.getOrDefault("es_password", "");

                        // 获取第一个节点作为主要连接节点
                        String primaryNode = esNodes.getFirst();
                        log.info("ElasticSearch主节点: {}:{}", primaryNode, httpPort);

                        // 构建节点列表（用于连接字符串）
                        StringBuilder nodeList = new StringBuilder();
                        for (int i = 0; i < esNodes.size(); i++) {
                                nodeList.append(esNodes.get(i)).append(":").append(httpPort);
                                if (i < esNodes.size() - 1) {
                                        nodeList.append(",");
                                }
                        }

                        // 构建基本信息项列表
                        List<InfoItem> basicInfoItems = new ArrayList<>();
                        basicInfoItems.add(new InfoItem("host", "主机", primaryNode));
                        basicInfoItems.add(new InfoItem("httpPort", "HTTP端口", httpPort));
                        basicInfoItems.add(new InfoItem("tcpPort", "TCP端口", tcpPort));
                        basicInfoItems.add(new InfoItem("clusterName", "集群名称",
                                        configMap.getOrDefault("cluster_name", "elasticsearch")));

                        // 如果有多个节点，添加节点列表信息
                        if (esNodes.size() > 1) {
                                basicInfoItems.add(new InfoItem("nodeList", "节点列表", nodeList.toString()));
                        }

                        // 构建安全信息项列表
                        List<InfoItem> securityInfoItems = new ArrayList<>();
        if (enableSecurity) {
                                securityInfoItems.add(new InfoItem("authMode", "认证模式", "用户名密码"));
                                securityInfoItems.add(new InfoItem("username", "用户名", securityUser));
                                securityInfoItems.add(new InfoItem("password", "密码", securityPassword));
                        } else {
                                securityInfoItems.add(new InfoItem("authMode", "认证模式", "无认证"));
                                securityInfoItems.add(new InfoItem("username", "用户名", ""));
                                securityInfoItems.add(new InfoItem("password", "密码", ""));
                        }

                        // 构建连接信息项列表
                        List<InfoItem> connectInfoItems = new ArrayList<>();
                        String httpUrl = "http://" + primaryNode + ":" + httpPort;
                        String restUrl = "http://" + (enableSecurity ? securityUser + ":" + securityPassword + "@" : "")
                                        +
                                        primaryNode + ":" + httpPort;

                        // 构建curl命令
                        String curlCommand;
        if (enableSecurity) {
                                curlCommand = "curl -u " + securityUser + ":" + securityPassword + " " + httpUrl;
        } else {
                                curlCommand = "curl " + httpUrl;
                        }

                        connectInfoItems.add(new InfoItem("httpUrl", "HTTP URL", httpUrl));
                        connectInfoItems.add(new InfoItem("restUrl", "REST URL", restUrl));
                        connectInfoItems.add(new InfoItem("curlCommand", "CURL命令", curlCommand));

                        // 构建重要键列表
                        List<String> importantKeys = Arrays.asList("httpUrl", "restUrl", "curlCommand");

                        // 返回连接信息构建器
                        log.info("ElasticSearch连接信息生成成功");
                        return ConnectionInfo.builder()
                                        .basicInfoItems(basicInfoItems)
                                        .securityInfoItems(securityInfoItems)
                                        .connectInfoItems(connectInfoItems)
                                        .hostName(primaryNode)
                                        .importantKeys(importantKeys);
                } catch (Exception e) {
                        log.error("获取ElasticSearch连接信息失败: {}", e.getMessage(), e);
                        return ConnectionInfo.builder();
                }
        }

}
