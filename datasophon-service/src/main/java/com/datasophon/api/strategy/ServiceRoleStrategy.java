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

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import cn.hutool.core.util.StrUtil;
import com.datasophon.api.load.ServiceInfoMap;
import com.datasophon.api.load.ServiceRoleMap;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceRoleInstanceWebuisService;
import com.datasophon.api.utils.ProcessUtils;
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
import com.datasophon.dao.enums.AlertLevel;
import com.datasophon.k8s.util.K8sUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

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
    default void handler(Integer clusterId, List<String> hosts) {
    }

    /**
     * 保存服务配置时根据ServiceName调用
     */
    default void handlerConfig(Integer clusterId, List<ServiceConfig> list) {
    }

    /**
     * 获取服务配置时修改配置，根据ServiceName调用
     * handler之后handlerConfig之前调用
     * 提取角色本身配置和handler中自定义的变量
     */
    default void getConfig(Integer clusterId, List<ServiceConfig> list) {
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
    default void handlerServiceRoleCheck(ClusterServiceRoleInstanceEntity roleInstanceEntity,
            Map<String, ClusterServiceRoleInstanceEntity> map) {
    }

    /**
     * 获取组件的连接信息，包括连接地址、JDBC URL和示例代码等
     * 
     * @param clusterId         集群ID
     * @param serviceInstanceId 服务实例ID
     * @return 连接信息对象
     */
    default ConnectionInfo getConnectionInfo(Integer clusterId, Integer serviceInstanceId) {
        // 默认返回空对象，具体组件在各自实现中提供连接信息
        return ConnectionInfo.builder().build();
    }

    /**
     * 定期检查角色处理（K8S）
     */
    default void handlerK8sServiceRoleCheck(ClusterServiceRoleInstanceEntity roleInstanceEntity,
            Map<String, ClusterServiceRoleInstanceEntity> map) {
        handlerServiceRoleCheck(roleInstanceEntity, map);
    }

    default String getKubeConfig(ClusterServiceRoleInstanceEntity roleInstanceEntity) {
        ClusterInfoService clusterInfoService = SpringTool.getApplicationContext().getBean(ClusterInfoService.class);
        return clusterInfoService.getKubeConfigByClusterId(roleInstanceEntity.getClusterId());
    }

    default ExecuteCmdCommand getCommand(ClusterServiceRoleInstanceEntity roleInstanceEntity) {
        Integer clusterId = roleInstanceEntity.getClusterId();
        ClusterInfoEntity cluster = ProcessUtils.getClusterInfo(clusterId);
        String frameCode = cluster.getClusterFrame();
        String key = frameCode + Constants.UNDERLINE + roleInstanceEntity.getServiceName() + Constants.UNDERLINE
                + roleInstanceEntity.getServiceRoleName();
        ServiceRoleInfo serviceRoleInfo = ServiceRoleMap.get(key);
        ServiceInfo serviceInfo = ServiceInfoMap
                .get(frameCode + Constants.UNDERLINE + roleInstanceEntity.getServiceName());
        ExecuteCmdCommand cmdCommand = new ExecuteCmdCommand();
        ArrayList<String> commandList = new ArrayList<>();
        commandList.add(serviceInfo.getDecompressPackageName() + Constants.SLASH
                + serviceRoleInfo.getStatusRunner().getProgram());
        commandList.addAll(serviceRoleInfo.getStatusRunner().getArgs());
        cmdCommand.setCommands(commandList);
        return cmdCommand;
    }

    // 提取的通用方法
    default void performServiceRoleCheck(ClusterServiceRoleInstanceEntity roleInstanceEntity, String actorName) {
        // 获取命令
        ExecuteCmdCommand cmdCommand = getCommand(roleInstanceEntity);

        // 执行命令
        ExecResult execResult = executeCommand(roleInstanceEntity, cmdCommand, actorName);

        // 处理执行结果
        handleExecResult(roleInstanceEntity, execResult);
    }

    default void handleExecResult(ClusterServiceRoleInstanceEntity roleInstanceEntity, ExecResult execResult) {
        if (StrUtil.equalsAnyIgnoreCase(roleInstanceEntity.getServiceRoleName(),
                "NameNode",
                "ResourceManager")) {
            ClusterServiceRoleInstanceWebuisService webuisService = SpringTool.getApplicationContext()
                    .getBean(ClusterServiceRoleInstanceWebuisService.class);
            if (execResult.getExecResult()) {
                if (execResult.getExecOut().contains(ACTIVE)) {
                    webuisService.updateWebUiToActive(roleInstanceEntity.getId());
                } else {
                    webuisService.updateWebUiToStandby(roleInstanceEntity.getId());
                }
            } else {
                webuisService.updateWebUiToStandby(roleInstanceEntity.getId());
            }
        }

        if (StrUtil.equalsAnyIgnoreCase(roleInstanceEntity.getServiceRoleName(),
                "Krb5Kdc",
                "KAdmin",
                "Prometheus")) {
            if (execResult.getExecResult()) {
                ProcessUtils.recoverAlert(roleInstanceEntity);
            } else {
                String alertTargetName = roleInstanceEntity.getServiceRoleName() + " Survive";
                ProcessUtils.saveAlert(roleInstanceEntity, alertTargetName, AlertLevel.EXCEPTION, "restart");
            }
        }
    }

    default ExecResult executeCommand(ClusterServiceRoleInstanceEntity roleInstanceEntity, ExecuteCmdCommand cmdCommand,
            String actorName) {
        ExecResult execResult = new ExecResult();

        try {
            if (StrUtil.isBlank(actorName)) {
                // 对于 K8s 服务，使用 K8sUtil 执行命令
                execResult = K8sUtil.exec(roleInstanceEntity, getKubeConfig(roleInstanceEntity), cmdCommand);
            } else {
                // 对于非 K8s 服务，使用 Actor 系统执行命令
                Timeout timeout = new Timeout(Duration.create(30, TimeUnit.SECONDS));
                ActorRef actorRef = ActorUtils.getRemoteActor(roleInstanceEntity.getHostname(), actorName);
                Future<Object> execFuture = Patterns.ask(actorRef, cmdCommand, timeout);
                execResult = (ExecResult) Await.result(execFuture, timeout.duration());
            }
        } catch (Exception e) {
            log.error("exec command error", e);
        }
        return execResult;
    }

}
