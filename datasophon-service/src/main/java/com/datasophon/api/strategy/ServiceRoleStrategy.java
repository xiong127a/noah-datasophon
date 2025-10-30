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
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.converter.ServiceRoleToK8sConverter;
import com.datasophon.api.master.handler.service.WorkerTaskHelper;
import com.datasophon.kubernetes.model.K8sServiceRoleInfo;
import com.datasophon.api.load.ServiceInfoMap;
import com.datasophon.api.load.ServiceRoleMap;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.converter.ClusterServiceRoleInstanceConverter;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceRoleInstanceWebuisService;
import com.datasophon.api.service.ServiceStateManagementService;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.common.Constants;
import com.datasophon.common.command.ExecuteCmdCommand;
import com.datasophon.common.model.ConnectionInfo;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceInfo;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.common.enums.AlertLevel;
import com.datasophon.kubernetes.util.KubernetesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface ServiceRoleStrategy {

    Logger log = LoggerFactory.getLogger(ServiceRoleStrategy.class);

    String ACTIVE = "active";

    /**
     * 保存角色host映射关系时根据roleName调用
     */
    default void handler(Long clusterId, List<String> hosts) {
    }

    /**
     * 保存服务配置时根据ServiceName调用
     */
    default void handlerConfig(Long clusterId, List<ServiceConfig> list) {
    }

    /**
     * 获取服务配置时修改配置，根据ServiceName调用
     * handler之后handlerConfig之前调用
     * 提取角色本身配置和handler中自定义的变量
     */
    default void getConfig(Long clusterId, List<ServiceConfig> list) {
    }

    /**
     * 构建DAG时处理角色关系，例如设置主从角色，设置搭建顺序等。
     * <p>
     * 可以将自定义角色配置传递给worker
     */
    default void handlerServiceRoleInfo(ServiceRoleInfo serviceRoleInfo, String hostname) {
    }

    /**
     * 定期检查角色处理
     */
    default void handlerServiceRoleCheck(ClusterServiceRoleInstanceDTO roleInstanceDto,
            Map<String, ClusterServiceRoleInstanceDTO> map) {
    }

    /**
     * 获取组件的连接信息，包括连接地址、JDBC URL和示例代码等
     *
     * @param clusterId         集群ID
     * @param serviceInstanceId 服务实例ID
     * @return 连接信息对象
     */
    default ConnectionInfo getConnectionInfo(Long clusterId, Long serviceInstanceId, String serviceHome,
            Map<String, String> configMap) {
        // 默认返回空对象，具体组件在各自实现中提供连接信息
        return ConnectionInfo.builder().build();
    }

    /**
     * 定期检查角色处理（Kubernetes）
     */
    default void handlerKubernetesServiceRoleCheck(ClusterServiceRoleInstanceDTO roleInstanceDto,
            Map<String, ClusterServiceRoleInstanceDTO> map) {
        handlerServiceRoleCheck(roleInstanceDto, map);
    }

    default ExecuteCmdCommand getCommand(ClusterServiceRoleInstanceDTO roleInstanceDto) {
        Long clusterId = roleInstanceDto.clusterId();
        ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
        ClusterInfoEntity cluster = clusterInfoService.getById(clusterId);
        String frameCode = cluster.getClusterFrame();
        String key = frameCode + Constants.UNDERLINE + roleInstanceDto.serviceName() + Constants.UNDERLINE
                + roleInstanceDto.serviceRoleName();
        ServiceRoleInfo serviceRoleInfo = ServiceRoleMap.get(key);
        ServiceInfo serviceInfo = ServiceInfoMap
                .get(frameCode + Constants.UNDERLINE + roleInstanceDto.serviceName());
        ExecuteCmdCommand cmdCommand = new ExecuteCmdCommand();
        ArrayList<String> commandList = new ArrayList<>();
        commandList.add(serviceInfo.getDecompressPackageName() + Constants.SLASH
                + serviceRoleInfo.getStatusRunner().getProgram());
        commandList.addAll(serviceRoleInfo.getStatusRunner().getArgs());
        cmdCommand.setCommands(commandList);
        return cmdCommand;
    }

    // 提取的通用方法
    default void performServiceRoleCheck(ClusterServiceRoleInstanceDTO roleInstanceDto, String actorName) {
        // 获取命令
        ExecuteCmdCommand cmdCommand = getCommand(roleInstanceDto);

        // 执行命令
        ExecResult execResult = executeCommand(roleInstanceDto, cmdCommand, actorName);

        // 处理执行结果
        handleExecResult(roleInstanceDto, execResult);
    }

    default void handleExecResult(ClusterServiceRoleInstanceDTO roleInstanceDto, ExecResult execResult) {
        if (StrUtil.equalsAnyIgnoreCase(roleInstanceDto.serviceRoleName(),
                "NameNode",
                "ResourceManager")) {
            ClusterServiceRoleInstanceWebuisService webuisService = SpringUtil
                    .getBean(ClusterServiceRoleInstanceWebuisService.class);
            if (execResult.getExecResult()) {
                if (execResult.getExecOut().contains(ACTIVE)) {
                    webuisService.updateWebUiToActive(roleInstanceDto.id());
                } else {
                    webuisService.updateWebUiToStandby(roleInstanceDto.id());
                }
            } else {
                webuisService.updateWebUiToStandby(roleInstanceDto.id());
            }
        }

        if (StrUtil.equalsAnyIgnoreCase(roleInstanceDto.serviceRoleName(),
                "Krb5Kdc",
                "KAdmin",
                "Prometheus")) {
            ServiceStateManagementService serviceStateManagementService = SpringUtil.getBean(ServiceStateManagementService.class);
            if (execResult.getExecResult()) {
                serviceStateManagementService.recoverAlert(roleInstanceDto);
            } else {
                String alertTargetName = roleInstanceDto.serviceRoleName() + " Survive";
                serviceStateManagementService.saveAlert(roleInstanceDto, alertTargetName, AlertLevel.EXCEPTION, "restart");
            }
        }
    }

    default ExecResult executeCommand(ClusterServiceRoleInstanceDTO roleInstanceDto, ExecuteCmdCommand cmdCommand,
            String actorName) {
        ExecResult execResult = new ExecResult();

        try {
            if (StrUtil.isBlank(actorName)) {
                // 对于 Kubernetes 服务，使用 KubernetesUtil 执行命令
                ServiceRoleToK8sConverter converter = SpringUtil.getBean(ServiceRoleToK8sConverter.class);
                ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
                ClusterServiceRoleInstanceConverter roleInstanceConverter = SpringUtil.getBean(ClusterServiceRoleInstanceConverter.class);
                ClusterInfoEntity clusterInfo = clusterInfoService.getById(roleInstanceDto.clusterId());

                // 需要转换DTO为Entity以匹配converter的期望参数类型
                ClusterServiceRoleInstanceEntity roleInstanceEntity = roleInstanceConverter.dtoToEntity(roleInstanceDto);
                K8sServiceRoleInfo k8sServiceRoleInfo = converter.convertToK8sServiceRoleInfo(roleInstanceEntity,
                        clusterInfo);
                String kubeConfig = clusterInfoService.getKubeConfigByClusterId(roleInstanceDto.clusterId());

                execResult = KubernetesUtil.exec(k8sServiceRoleInfo, kubeConfig, cmdCommand);
            } else {
                // 对于非 Kubernetes 服务，使用HTTP方式执行命令
                execResult = WorkerTaskHelper.submitAndWait(roleInstanceDto.hostname(), cmdCommand, 30);
            }
        } catch (Exception e) {
            log.error("exec command error", e);
        }
        return execResult;
    }

    default Map.Entry<String, List<ServiceConfig>> listServiceConfigByServiceInstance(Long serviceInstanceId) {
        return SpringTool.listServiceConfigByServiceInstance(serviceInstanceId);
    }

    default List<String> getRoleHosts(Long clusterId, Long serviceInstanceId, String roleName) {
        return CollUtil.empty(List.class);
    }

    /**
     * 获取服务配置并解析为Map
     *
     * @param serviceInstanceId 服务实例ID
     * @return 包含服务主目录和配置Map的对象
     */
    default Map.Entry<String, Map<String, String>> getServiceConfigMap(Long serviceInstanceId) {
        // 1. 获取服务配置
        Map.Entry<String, List<ServiceConfig>> pair = listServiceConfigByServiceInstance(serviceInstanceId);
        List<ServiceConfig> serviceConfigs = pair.getValue();
        String serviceHome = pair.getKey();

        // 2. 从配置中解析配置到map，方便快速查询
        Map<String, String> configMap = new HashMap<>();
        for (ServiceConfig config : serviceConfigs) {
            if (config.getValue() != null) {
                configMap.put(config.getName(), String.valueOf(config.getValue()));
            }
        }

        // 返回包含serviceHome和configMap的Entry
        return new AbstractMap.SimpleEntry<>(serviceHome, configMap);
    }

}
