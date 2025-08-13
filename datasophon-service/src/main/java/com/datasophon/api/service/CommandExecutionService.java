/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.datasophon.api.service;

import com.datasophon.common.enums.CommandType;
import com.datasophon.common.enums.ServiceExecuteState;
import com.datasophon.common.command.ExecuteServiceRoleCommand;
import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.model.DAGGraph;
import com.datasophon.common.model.ServiceNode;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.dao.entity.ClusterServiceCommandEntity;
import com.datasophon.dao.entity.ClusterServiceCommandHostEntity;
import com.datasophon.dao.entity.ClusterServiceCommandHostCommandEntity;
import com.datasophon.common.enums.RoleType;
import org.apache.pekko.actor.ActorRef;

import java.util.List;
import java.util.Map;

/**
 * 命令执行管理服务
 * 负责服务命令执行和状态管理相关的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
public interface CommandExecutionService {

    /**
     * 更新命令状态为失败
     * 取消正在运行的命令并更新状态
     *
     * @param commandIds 命令ID列表
     */
    void updateCommandStateToFailed(List<String> commandIds);

    /**
     * 处理命令执行结果
     * 更新命令状态和进度
     *
     * @param hostCommandId 主机命令ID
     * @param execResult    执行结果
     * @param execOut       执行输出
     */
    void handleCommandResult(String hostCommandId, Boolean execResult, String execOut);

    /**
     * 通知命令执行结果
     * 向Actor发送服务执行结果消息
     *
     * @param serviceName              服务名称
     * @param executeServiceRoleCommand 执行服务角色命令
     * @param state                    执行状态
     */
    void tellCommandActorResult(String serviceName, ExecuteServiceRoleCommand executeServiceRoleCommand,
                               ServiceExecuteState state);

    /**
     * 构建执行服务角色命令
     *
     * @param clusterId             集群ID
     * @param commandType           命令类型
     * @param clusterCode           集群代码
     * @param dag                   DAG图
     * @param activeTaskList        活跃任务列表
     * @param errorTaskList         错误任务列表
     * @param readyToSubmitTaskList 准备提交任务列表
     * @param completeTaskList      完成任务列表
     * @param node                  节点
     * @param masterRoles           主节点角色列表
     * @param workerRole            工作节点角色
     * @param serviceActor          服务Actor
     * @param serviceRoleType       服务角色类型
     */
    void buildExecuteServiceRoleCommand(
            Long clusterId,
            CommandType commandType,
            String clusterCode,
            DAGGraph<String, ServiceNode, String> dag,
            Map<String, ServiceExecuteState> activeTaskList,
            Map<String, String> errorTaskList,
            Map<String, String> readyToSubmitTaskList,
            Map<String, String> completeTaskList,
            String node,
            List<ServiceRoleInfo> masterRoles,
            ServiceRoleInfo workerRole,
            ActorRef serviceActor,
            ServiceRoleType serviceRoleType);

    /**
     * 生成命令实体
     *
     * @param clusterId   集群ID
     * @param commandType 命令类型
     * @param serviceName 服务名称
     * @return 命令实体
     */
    ClusterServiceCommandEntity generateCommandEntity(Long clusterId, CommandType commandType,
                                                      String serviceName);

    /**
     * 生成命令主机实体
     *
     * @param commandId 命令ID
     * @param hostname  主机名
     * @return 命令主机实体
     */
    ClusterServiceCommandHostEntity generateCommandHostEntity(String commandId, String hostname);

    /**
     * 生成命令主机命令实体
     *
     * @param commandType     命令类型
     * @param commandId       命令ID
     * @param serviceRoleName 服务角色名称
     * @param serviceRoleType 服务角色类型
     * @param commandHost     命令主机实体
     * @return 命令主机命令实体
     */
    ClusterServiceCommandHostCommandEntity generateCommandHostCommandEntity(CommandType commandType,
                                                                           String commandId,
                                                                           String serviceRoleName,
                                                                           RoleType serviceRoleType,
                                                                           ClusterServiceCommandHostEntity commandHost);
}