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

import org.apache.pekko.actor.ActorRef;
import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.EnumUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datasophon.api.enums.Status;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.DAGBuildActor;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceCommandHostCommandService;
import com.datasophon.api.service.ClusterServiceCommandHostService;
import com.datasophon.api.service.ClusterServiceCommandService;
import com.datasophon.api.service.ClusterServiceInstanceService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.service.FrameServiceRoleService;
import com.datasophon.api.service.FrameServiceService;
import com.datasophon.api.utils.CacheOperateUtils;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.command.StartExecuteCommandCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.model.RollingRestartInfo;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceCommandEntity;
import com.datasophon.dao.entity.ClusterServiceCommandHostCommandEntity;
import com.datasophon.dao.entity.ClusterServiceCommandHostEntity;
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.entity.FrameServiceEntity;
import com.datasophon.dao.entity.FrameServiceRoleEntity;
import com.datasophon.dao.enums.CommandState;
import com.datasophon.dao.mapper.ClusterServiceCommandMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service("clusterServiceCommandService")
public class ClusterServiceCommandServiceImpl
        extends
        ServiceImpl<ClusterServiceCommandMapper, ClusterServiceCommandEntity>
        implements
        ClusterServiceCommandService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterServiceCommandServiceImpl.class);

    @Autowired
    private ClusterInfoService clusterInfoService;

    @Autowired
    private ClusterServiceCommandHostService commandHostService;

    @Autowired
    private ClusterServiceCommandHostCommandService hostCommandService;

    @Autowired
    private FrameServiceService frameServiceService;

    @Autowired
    private FrameServiceRoleService frameServiceRoleService;

    @Autowired
    private ClusterServiceCommandService commandService;

    @Autowired
    private ClusterServiceInstanceService serviceInstanceService;

    @Autowired
    private ClusterServiceRoleInstanceService roleInstanceService;

    @Override
    @Transactional
    public Result generateCommand(Integer clusterId, CommandType commandType, List<String> serviceNames) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);

        List<ClusterServiceCommandEntity> list = new ArrayList<>();
        List<ClusterServiceCommandHostEntity> commandHostList = new ArrayList<>();
        List<ClusterServiceCommandHostCommandEntity> hostCommandList = new ArrayList<>();
        List<String> commandIds = new ArrayList<>();

        Map<String, List<String>> serviceRoleHostMap = CacheOperateUtils
                .getWithType(clusterInfo.getClusterCode() + Constants.UNDERLINE + Constants.SERVICE_ROLE_HOST_MAPPING,
                        new TypeReference<Map<String, List<String>>>() {
                        });

        for (String serviceName : serviceNames) {
            // 1、生成操作指令
            ClusterServiceInstanceEntity serviceInstance = serviceInstanceService
                    .getServiceInstanceByClusterIdAndServiceName(clusterId, serviceName);

            ClusterServiceCommandEntity commandEntity = ProcessUtils.generateCommandEntity(clusterId, commandType,
                    serviceName);
            commandEntity.setServiceInstanceId(serviceInstance.getId());
            list.add(commandEntity);
            String commandId = commandEntity.getCommandId();
            commandIds.add(commandId);

            // 查询服务的服务角色
            FrameServiceEntity frameService = frameServiceService
                    .getServiceByFrameCodeAndServiceName(clusterInfo.getClusterFrame(), serviceName);
            Result result = frameServiceRoleService.getServiceRoleList(clusterId, String.valueOf(frameService.getId()),
                    null);
            List<FrameServiceRoleEntity> serviceRoleList = (List<FrameServiceRoleEntity>) result.getData();
            HashMap<String, ClusterServiceCommandHostEntity> map = new HashMap<>();
            for (FrameServiceRoleEntity serviceRole : serviceRoleList) {
                if (Objects.nonNull(serviceRoleHostMap)
                        && serviceRoleHostMap.containsKey(serviceRole.getServiceRoleName())) {
                    List<String> hosts = serviceRoleHostMap.get(serviceRole.getServiceRoleName());
                    for (String hostname : hosts) {
                        if (alreadyExistsServiceRole(serviceRole.getServiceRoleName(), hostname, clusterId)) {
                        } else {
                            ClusterServiceCommandHostEntity commandHost;
                            if (map.containsKey(hostname)) {
                                commandHost = map.get(hostname);
                            } else {
                                commandHost = ProcessUtils.generateCommandHostEntity(commandId, hostname);
                                commandHostList.add(commandHost);
                                map.put(hostname, commandHost);
                            }
                            // 4、生成主机操作指令
                            ClusterServiceCommandHostCommandEntity hostCommand = ProcessUtils
                                    .generateCommandHostCommandEntity(commandType, commandId,
                                            serviceRole.getServiceRoleName(), serviceRole.getServiceRoleType(),
                                            commandHost);
                            hostCommandList.add(hostCommand);
                        }
                    }
                }
            }
        }
        if (commandHostList.isEmpty()) {
            logger.warn("No service role selected");
            return Result.error(Status.NO_SERVICE_ROLE_SELECTED.getMsg());
        }
        commandService.saveBatch(list);
        commandHostService.saveBatch(commandHostList);
        hostCommandService.saveBatch(hostCommandList);
        return Result.success(String.join(",", commandIds));
    }

    private boolean alreadyExistsServiceRole(String serviceRoleName, String hostname, Integer clusterId) {
        ClusterServiceRoleInstanceEntity serviceRole = roleInstanceService.getOneServiceRole(serviceRoleName, hostname,
                clusterId);
        return Objects.nonNull(serviceRole);
    }

    @Override
    public Result getServiceCommandlist(Integer clusterId, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        List<ClusterServiceCommandEntity> list = this.list(new QueryWrapper<ClusterServiceCommandEntity>()
                .eq(Constants.CLUSTER_ID, clusterId)
                .orderByDesc(Constants.CREATE_TIME).last("limit " + offset + "," + pageSize));
        long total = this.count(new QueryWrapper<ClusterServiceCommandEntity>()
                .eq(Constants.CLUSTER_ID, clusterId));
        for (ClusterServiceCommandEntity commandEntity : list) {
            // 实时聚合命令进度和状态，并更新数据库
            calculateCommandActualProgress(commandEntity);
            calculateRealTimeCommandState(commandEntity);
            // 设置状态码用于前端显示
            commandEntity.setCommandStateCode(commandEntity.getCommandState().getValue());
            // 计算实际时间
            Date createTime = commandEntity.getCreateTime();
            Date endTime = commandEntity.getEndTime();
            if (Objects.isNull(endTime)) {
                endTime = new Date();
            }
            long between = DateUtil.between(createTime, endTime, DateUnit.MS);
            String durationTime = DateUtil.formatBetween(between, BetweenFormatter.Level.SECOND);
            commandEntity.setDurationTime(durationTime);
        }
        return Result.success(list).put(Constants.TOTAL, total);
    }

    /**
     * 计算命令的实际进度
     * 通过查询主机命令进度计算命令的整体进度
     *
     * @param commandEntity 命令实体
     */
    private void calculateCommandActualProgress(ClusterServiceCommandEntity commandEntity) {
        try {
            Long oldProgress = commandEntity.getCommandProgress();

            if (CommandState.SUCCESS.equals(commandEntity.getCommandState())) {
                commandEntity.setCommandProgress(100L);
                if ((oldProgress == null || oldProgress != 100L)) {
                    this.updateById(commandEntity);
                    logger.info("命令 {} 状态已成功，进度设为100%并更新数据库", commandEntity.getCommandId());
                }
                return;
            } else if (CommandState.FAILED.equals(commandEntity.getCommandState())) {
                commandEntity.setCommandProgress(100L);
                if ((oldProgress == null || oldProgress != 100L)) {
                    this.updateById(commandEntity);
                    logger.info("命令 {} 状态已失败，进度设为100%并更新数据库", commandEntity.getCommandId());
                }
                return;
            } else if (CommandState.CANCEL.equals(commandEntity.getCommandState())) {
                commandEntity.setCommandProgress(100L);
                if ((oldProgress == null || oldProgress != 100L)) {
                    this.updateById(commandEntity);
                    logger.info("命令 {} 状态已取消，进度设为100%并更新数据库", commandEntity.getCommandId());
                }
                return;
            }
            // 获取该命令下所有主机命令
            List<ClusterServiceCommandHostEntity> hostCommands = commandHostService.list(
                    new QueryWrapper<ClusterServiceCommandHostEntity>()
                            .eq("command_id", commandEntity.getCommandId()));
            if (hostCommands == null || hostCommands.isEmpty()) {
                commandEntity.setCommandProgress(0L);
                if ((oldProgress == null || oldProgress != 0L)) {
                    this.updateById(commandEntity);
                    logger.info("命令 {} 无主机命令，进度设为0%并更新数据库", commandEntity.getCommandId());
                }
                return;
            }
            long totalProgress = 0;
            int completedCount = 0;
            int totalCount = hostCommands.size();
            for (ClusterServiceCommandHostEntity hostCommand : hostCommands) {
                // 实时聚合主机命令进度
                commandHostService.calculateHostCommandActualProgress(hostCommand, true);
                if (hostCommand.getCommandProgress() != null) {
                    totalProgress += hostCommand.getCommandProgress();
                    if (CommandState.SUCCESS.equals(hostCommand.getCommandState())) {
                        completedCount++;
                    }
                }
            }
            long avgProgress = totalProgress / totalCount;
            long completedProgress = completedCount * 100L / totalCount;
            long finalProgress = Math.max(avgProgress, completedProgress);
            commandEntity.setCommandProgress(finalProgress);

            // 如果需要更新数据库且进度有变化
            if ((oldProgress == null || oldProgress != finalProgress)) {
                this.updateById(commandEntity);
                logger.info("命令 {} 进度更新为 {}% 并更新数据库", commandEntity.getCommandId(), finalProgress);
            }
        } catch (Exception e) {
            logger.error("计算命令进度时出错: " + e.getMessage(), e);
        }
    }

    /**
     * 实时计算命令状态（从内层计算外层状态）
     * 该方法可以选择是否更新数据库
     *
     * @param commandEntity 命令实体
     */
    private void calculateRealTimeCommandState(ClusterServiceCommandEntity commandEntity) {
        try {
            List<ClusterServiceCommandHostEntity> hostCommands = commandHostService.list(
                    new QueryWrapper<ClusterServiceCommandHostEntity>()
                            .eq("command_id", commandEntity.getCommandId()));
            if (hostCommands == null || hostCommands.isEmpty()) {
                commandEntity.setCommandState(CommandState.RUNNING);
                commandEntity.setCommandStateCode(CommandState.RUNNING.getValue());
                return;
            }
            boolean allCompleted = true;
            int failedCount = 0;
            int canceledCount = 0;
            int successCount = 0;
            for (ClusterServiceCommandHostEntity hostCommand : hostCommands) {
                // 实时聚合主机命令状态
                commandHostService.calculateRealTimeHostCommandState(hostCommand, true);
                if (CommandState.RUNNING.equals(hostCommand.getCommandState())) {
                    allCompleted = false;
                } else if (CommandState.FAILED.equals(hostCommand.getCommandState())) {
                    failedCount++;
                } else if (CommandState.CANCEL.equals(hostCommand.getCommandState())) {
                    canceledCount++;
                } else if (CommandState.SUCCESS.equals(hostCommand.getCommandState())) {
                    successCount++;
                }
            }

            boolean stateChanged = false;
            CommandState oldState = commandEntity.getCommandState();

            if (allCompleted) {
                if (failedCount > 0) {
                    commandEntity.setCommandState(CommandState.FAILED);
                    commandEntity.setCommandStateCode(CommandState.FAILED.getValue());
                    stateChanged = !CommandState.FAILED.equals(oldState);
                } else if (canceledCount > 0) {
                    commandEntity.setCommandState(CommandState.CANCEL);
                    commandEntity.setCommandStateCode(CommandState.CANCEL.getValue());
                    stateChanged = !CommandState.CANCEL.equals(oldState);
                } else {
                    commandEntity.setCommandState(CommandState.SUCCESS);
                    commandEntity.setCommandStateCode(CommandState.SUCCESS.getValue());
                    stateChanged = !CommandState.SUCCESS.equals(oldState);
                }

                // 如果状态变为完成状态且endTime未设置，则设置endTime
                if (stateChanged && commandEntity.getEndTime() == null) {
                    commandEntity.setEndTime(new Date());
                    logger.info("命令 {} 状态变为 {}, 设置结束时间为 {}",
                            commandEntity.getCommandId(),
                            commandEntity.getCommandState(),
                            commandEntity.getEndTime());
                }
            } else {
                commandEntity.setCommandState(CommandState.RUNNING);
                commandEntity.setCommandStateCode(CommandState.RUNNING.getValue());
            }

            // 如果需要更新数据库且状态发生了变化或者是结束状态
            if ((stateChanged || allCompleted)) {
                this.updateById(commandEntity);
                logger.trace("命令 {} 实时计算状态后更新数据库，状态: {}",
                        commandEntity.getCommandId(), commandEntity.getCommandState());
            }
        } catch (Exception e) {
            logger.error("实时计算命令状态出错: " + e.getMessage(), e);
        }
    }

    /**
     * 1、生成指令
     * 2、生成主机指令
     * 3、生产主机上操作指令
     *
     */
    @Override
    public Result generateServiceCommand(Integer clusterId, CommandType commandType, List<String> serviceInstanceIds) {
        List<ClusterServiceCommandEntity> list = new ArrayList<>();
        List<ClusterServiceCommandHostEntity> commandHostList = new ArrayList<>();
        List<ClusterServiceCommandHostCommandEntity> hostCommandList = new ArrayList<>();
        List<String> commandIds = new ArrayList<>();
        for (String serviceInstanceId : serviceInstanceIds) {
            int id = Integer.parseInt(serviceInstanceId);
            // 查询服务对应的服务角色实例
            List<ClusterServiceRoleInstanceEntity> roleInstanceList = roleInstanceService
                    .getServiceRoleInstanceListByServiceId(id);
            if (Objects.isNull(roleInstanceList) || roleInstanceList.isEmpty()) {
                continue;
            }
            ClusterServiceInstanceEntity serviceInstance = serviceInstanceService.getById(id);
            ClusterServiceCommandEntity commandEntity = ProcessUtils.generateCommandEntity(clusterId, commandType,
                    serviceInstance.getServiceName());
            String commandId = commandEntity.getCommandId();
            commandEntity.setServiceInstanceId(id);
            commandIds.add(commandId);
            list.add(commandEntity);

            HashMap<String, ClusterServiceCommandHostEntity> map = new HashMap<>();
            for (ClusterServiceRoleInstanceEntity roleInstance : roleInstanceList) {
                ClusterServiceCommandHostEntity commandHost;
                if (map.containsKey(roleInstance.getHostname())) {
                    commandHost = map.get(roleInstance.getHostname());
                } else {
                    commandHost = ProcessUtils.generateCommandHostEntity(commandId, roleInstance.getHostname());
                    commandHostList.add(commandHost);
                }
                ClusterServiceCommandHostCommandEntity hostCommand = ProcessUtils.generateCommandHostCommandEntity(
                        commandType, commandId,
                        roleInstance.getServiceRoleName(), roleInstance.getRoleType(), commandHost);
                hostCommandList.add(hostCommand);
                map.put(roleInstance.getHostname(), commandHost);
            }
        }
        if (!list.isEmpty()) {
            commandService.saveBatch(list);
            commandHostService.saveBatch(commandHostList);
            hostCommandService.saveBatch(hostCommandList);

            // 通知commandActor执行命令
            ActorRef dagBuildActor = ActorUtils.getLocalActor(DAGBuildActor.class,
                    ActorUtils.getActorRefName(DAGBuildActor.class));
            dagBuildActor.tell(new StartExecuteCommandCommand(commandIds, clusterId, commandType), ActorRef.noSender());
        }
        return Result.success(String.join(",", commandIds));
    }

    @Override
    public Result generateServiceRoleCommands(Integer clusterId, CommandType commandType,
            Map<Integer, List<String>> instanceIdMap) {
        Result result = null;
        for (Map.Entry<Integer, List<String>> entry : instanceIdMap.entrySet()) {
            result = generateServiceRoleCommand(clusterId, commandType, entry.getKey(), entry.getValue(), null);
        }
        return result;
    }

    @Override
    public Result generateServiceRoleCommand(Integer clusterId, CommandType commandType, Integer serviceInstanceId,
            List<String> serviceRoleInstanceIds, RollingRestartInfo rollingRestartInfo) {
        List<ClusterServiceCommandEntity> list = new ArrayList<>();
        List<ClusterServiceCommandHostEntity> commandHostList = new ArrayList<>();
        List<ClusterServiceCommandHostCommandEntity> hostCommandList = new ArrayList<>();
        List<String> commandIds = new ArrayList<>();

        ClusterServiceInstanceEntity serviceInstance = serviceInstanceService.getById(serviceInstanceId);
        ClusterServiceCommandEntity commandEntity = ProcessUtils.generateCommandEntity(clusterId, commandType,
                serviceInstance.getServiceName());
        String commandId = commandEntity.getCommandId();
        commandEntity.setServiceInstanceId(serviceInstanceId);
        commandIds.add(commandId);
        list.add(commandEntity);
        // 查询服务对应的服务角色实例
        HashMap<String, ClusterServiceCommandHostEntity> map = new HashMap<>();
        for (String serviceRoleInstanceId : serviceRoleInstanceIds) {
            int id = Integer.parseInt(serviceRoleInstanceId);
            ClusterServiceRoleInstanceEntity roleInstance = roleInstanceService.getById(id);

            ClusterServiceCommandHostEntity commandHost;
            if (map.containsKey(roleInstance.getHostname())) {
                commandHost = map.get(roleInstance.getHostname());
            } else {
                commandHost = ProcessUtils.generateCommandHostEntity(commandId, roleInstance.getHostname());
                commandHostList.add(commandHost);
            }
            ClusterServiceCommandHostCommandEntity hostCommand = ProcessUtils.generateCommandHostCommandEntity(
                    commandType, commandId, roleInstance.getServiceRoleName(), roleInstance.getRoleType(), commandHost);
            hostCommandList.add(hostCommand);
            map.put(roleInstance.getHostname(), commandHost);
        }
        commandService.saveBatch(list);
        commandHostService.saveBatch(commandHostList);
        hostCommandService.saveBatch(hostCommandList);

        // 通知commandActor执行命令
        ActorRef dagBuildActor = ActorUtils.getLocalActor(DAGBuildActor.class,
                ActorUtils.getActorRefName(DAGBuildActor.class));
        dagBuildActor.tell(new StartExecuteCommandCommand(commandIds, clusterId, commandType, rollingRestartInfo),
                ActorRef.noSender());
        return Result.success(String.join(",", commandIds));
    }

    @Override
    public void startExecuteCommand(Integer clusterId, String commandType, String commandIds) {
        List<String> list = Arrays.asList(commandIds.split(","));
        CommandType command = EnumUtil.fromString(CommandType.class, commandType);
        // 通知commandActor执行命令
        ActorRef dagBuildActor = ActorUtils.getLocalActor(DAGBuildActor.class,
                ActorUtils.getActorRefName(DAGBuildActor.class));
        dagBuildActor.tell(new StartExecuteCommandCommand(list, clusterId, command), ActorRef.noSender());
    }

    @Override
    public void cancelCommand(String commandId) {
        // command , command host, host command状态置为取消

    }

    @Override
    public ClusterServiceCommandEntity getLastRestartCommand(Integer serviceInstanceId) {
        return this.getOne(
                new QueryWrapper<ClusterServiceCommandEntity>().eq(Constants.SERVICE_INSTANCE_ID, serviceInstanceId)
                        .eq(Constants.COMMAND_TYPE, CommandType.RESTART_SERVICE.getValue()).or()
                        .eq(Constants.COMMAND_TYPE, CommandType.INSTALL_SERVICE.getValue())
                        .orderByDesc(Constants.CREATE_TIME).last("limit 1"));
    }

    @Override
    public ClusterServiceCommandEntity getCommandById(String commandId) {
        return this.getOne(
                new QueryWrapper<ClusterServiceCommandEntity>().eq("command_id", commandId));
    }
}
