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

import java.time.Duration;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import org.apache.commons.lang3.time.DurationFormatUtils;
import cn.hutool.core.util.EnumUtil;
import com.datasophon.api.converter.ClusterServiceCommandConverter;
import com.datasophon.api.converter.ClusterServiceCommandHostCommandConverter;
import com.datasophon.common.dto.ClusterServiceCommandDTO;
import com.datasophon.common.dto.FrameServiceDTO;
import com.datasophon.common.dto.ClusterServiceInstanceDTO;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.common.enums.RoleType;
import com.datasophon.common.enums.Status;
import com.datasophon.api.master.ActorUtils;

import java.time.LocalDateTime;

import com.datasophon.api.master.DAGBuildActor;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceCommandHostCommandService;
import com.datasophon.api.service.ClusterServiceCommandHostService;
import com.datasophon.api.service.ClusterServiceCommandService;
import com.datasophon.api.service.ClusterServiceInstanceService;
import com.datasophon.api.service.RoleInstanceQueryService;
import com.datasophon.api.service.FrameServiceService;
import com.datasophon.api.utils.CacheOperateUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.command.StartExecuteCommandCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.enums.TypeRefs;
import com.datasophon.common.model.PageResult;
import com.datasophon.common.model.RollingRestartInfo;

import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceCommandEntity;
import com.datasophon.dao.entity.ClusterServiceCommandHostCommandEntity;
import com.datasophon.dao.entity.ClusterServiceCommandHostEntity;
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.entity.FrameServiceRoleEntity;
import com.datasophon.common.dto.FrameServiceRoleDTO;
import com.datasophon.api.service.FrameServiceRoleService;
import com.datasophon.api.converter.FrameServiceRoleConverter;
import com.datasophon.common.enums.CommandState;
import com.datasophon.dao.mapper.ClusterServiceCommandMapper;
import com.datasophon.dao.mapper.ClusterServiceCommandHostMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.apache.pekko.actor.ActorRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 集群服务命令服务实现
 * 提供集群服务命令的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Service("clusterServiceCommandService")
public class ClusterServiceCommandServiceImpl
        extends
        ServiceImpl<ClusterServiceCommandMapper, ClusterServiceCommandEntity>
        implements
        ClusterServiceCommandService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterServiceCommandServiceImpl.class);

    @Autowired
    private ClusterServiceCommandConverter converter;

    @Autowired
    private ClusterInfoService clusterInfoService;

    @Autowired
    private ClusterServiceCommandHostCommandConverter clusterServiceCommandHostCommandConverter;

    @Autowired
    private ClusterServiceCommandHostService commandHostService;

    @Autowired
    private ClusterServiceCommandHostCommandService hostCommandService;

    @Autowired
    private FrameServiceService frameServiceService;

    @Autowired
    private FrameServiceRoleService frameServiceRoleService;

    @Autowired
    private FrameServiceRoleConverter frameServiceRoleConverter;


    @Autowired
    private ClusterServiceCommandService commandService;

    @Autowired
    private ClusterServiceInstanceService serviceInstanceService;

    @Autowired
    private RoleInstanceQueryService roleInstanceQueryService;

    @Override
    @Transactional
    public String generateCommand(Long clusterId, CommandType commandType, List<String> serviceNames) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);

        List<ClusterServiceCommandEntity> list = new ArrayList<>();
        List<ClusterServiceCommandHostEntity> commandHostList = new ArrayList<>();
        List<ClusterServiceCommandHostCommandEntity> hostCommandList = new ArrayList<>();
        List<Long> commandIds = new ArrayList<>();

        Map<String, List<String>> serviceRoleHostMap = CacheOperateUtils
                .getGeneric(clusterInfo.getClusterCode() + Constants.UNDERLINE + Constants.SERVICE_ROLE_HOST_MAPPING,
                        TypeRefs.MAP_STRING_LIST_STRING);

        for (String serviceName : serviceNames) {
            // 1、生成操作指令
            ClusterServiceInstanceDTO serviceInstanceDto = serviceInstanceService
                    .getServiceInstanceByClusterIdAndServiceName(clusterId, serviceName);
            ClusterServiceInstanceEntity serviceInstance = convertServiceInstanceToEntity(serviceInstanceDto);

            ClusterServiceCommandEntity commandEntity = generateCommandEntity(clusterId, commandType,
                    serviceName);
            commandEntity.setServiceInstanceId(serviceInstance.getId());
            list.add(commandEntity);
            Long commandId = commandEntity.getId();
            commandIds.add(commandId);

            // 查询服务的服务角色 - 使用正确的Service方法
            FrameServiceDTO frameServiceDTO = frameServiceService.getServiceByFrameCodeAndServiceName(
                    clusterInfo.getClusterFrame(), serviceName);
            if (frameServiceDTO == null) {
                logger.warn("未找到框架服务: {} - {}", clusterInfo.getClusterFrame(), serviceName);
                continue;
            }

            // 获取服务角色列表
            HashMap<String, ClusterServiceCommandHostEntity> map = new HashMap<>();
            List<FrameServiceRoleDTO> serviceRoleDTOs = frameServiceRoleService.getAllServiceRoleList(frameServiceDTO.id());
            List<FrameServiceRoleEntity> serviceRoleList = serviceRoleDTOs.stream()
                    .map(dto -> frameServiceRoleConverter.dtoToEntity(dto))
                    .toList();
            for (FrameServiceRoleEntity serviceRole : serviceRoleList) {
                if (Objects.nonNull(serviceRoleHostMap)
                        && serviceRoleHostMap.containsKey(serviceRole.getServiceRoleName())) {
                    List<String> hosts =
                            serviceRoleHostMap.get(serviceRole.getServiceRoleName());
                    for (String hostname : hosts) {
                        if (alreadyExistsServiceRole(serviceRole.getServiceRoleName(), hostname,
                                clusterId)) {
                        } else {
                            ClusterServiceCommandHostEntity commandHost;
                            if (map.containsKey(hostname)) {
                                commandHost = map.get(hostname);
                            } else {
                                commandHost = generateCommandHostEntity(commandId, hostname);
                                commandHostList.add(commandHost);
                                map.put(hostname, commandHost);
                            }
                            // 4、生成主机操作指令
                            ClusterServiceCommandHostCommandEntity hostCommand = generateCommandHostCommandEntity(
                                    commandType, commandId,
                                    serviceRole.getServiceRoleName(), serviceRole.getServiceRoleType(),
                                    commandHost);
                            hostCommandList.add(hostCommand);
                        }
                    }
                }
            }

        }

        // 检查是否有命令主机
        if (commandHostList.isEmpty()) {
            logger.warn("No service role selected");
            throw new RuntimeException(Status.NO_SERVICE_ROLE_SELECTED.getMsg());
        }

        // 保存命令数据
        commandService.saveBatch(list);
        commandHostService.saveBatch(commandHostList);
        hostCommandService.saveBatch(hostCommandList);

        return commandIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }

    private boolean alreadyExistsServiceRole(String serviceRoleName, String hostname, Long clusterId) {
        ClusterServiceRoleInstanceDTO serviceRoleDto = roleInstanceQueryService.getOneServiceRole(serviceRoleName,
                hostname,
                clusterId);
        return Objects.nonNull(serviceRoleDto);
    }

    @Override
    public PageResult<ClusterServiceCommandDTO> getServiceCommandlist(Long clusterId, Integer page,
                                                                      Integer pageSize) {
        // 使用分页对象
        Page<ClusterServiceCommandEntity> flexPage = new Page<>(
                page, pageSize);

        // 构建查询条件
        com.mybatisflex.core.query.QueryChain<ClusterServiceCommandEntity> query = com.mybatisflex.core.query.QueryChain
                .of(ClusterServiceCommandEntity.class)
                .where(ClusterServiceCommandEntity::getClusterId).eq(clusterId)
                .orderBy(ClusterServiceCommandEntity::getCreateTime).desc();

        // 执行分页查询
        Page<ClusterServiceCommandEntity> resultPage = query.page(flexPage);
        List<ClusterServiceCommandEntity> list = resultPage.getRecords();
        long total = resultPage.getTotalRow();

        for (ClusterServiceCommandEntity commandEntity : list) {
            // 实时聚合命令进度和状态，并更新数据库
            calculateCommandActualProgress(commandEntity);
            calculateRealTimeCommandState(commandEntity);
            // 设置状态码用于前端显示
            commandEntity.setCommandStateCode(commandEntity.getCommandState().getValue());
            // 计算实际时间（安全处理，避免负数持续时间）
            LocalDateTime createTime = commandEntity.getCreateTime();
            LocalDateTime endTime = commandEntity.getEndTime();
            
            // 安全检查：如果创建时间为null，设置默认持续时间
            if (Objects.isNull(createTime)) {
                commandEntity.setDurationTime("0 seconds");
                continue;
            }
            
            if (Objects.isNull(endTime)) {
                endTime = LocalDateTime.now();
            }
            
            // 计算持续时间，确保不为负数
            long between = Duration.between(createTime, endTime).toMillis();
            if (between < 0) {
                // 如果持续时间为负数，说明数据异常，设置为0
                between = 0;
            }
            
            String durationTime = DurationFormatUtils.formatDurationWords(between, true, true);
            commandEntity.setDurationTime(durationTime);
        }

        // 转换为DTO列表
        List<ClusterServiceCommandDTO> dtoList = converter.entityListToDtoList(list);
        return PageResult.of(dtoList, total, page, pageSize);
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
                    logger.info("命令 {} 状态已成功，进度设为100%并更新数据库", commandEntity.getId());
                }
                return;
            } else if (CommandState.FAILED.equals(commandEntity.getCommandState())) {
                commandEntity.setCommandProgress(100L);
                if ((oldProgress == null || oldProgress != 100L)) {
                    this.updateById(commandEntity);
                    logger.info("命令 {} 状态已失败，进度设为100%并更新数据库", commandEntity.getId());
                }
                return;
            } else if (CommandState.CANCEL.equals(commandEntity.getCommandState())) {
                commandEntity.setCommandProgress(100L);
                if ((oldProgress == null || oldProgress != 100L)) {
                    this.updateById(commandEntity);
                    logger.info("命令 {} 状态已取消，进度设为100%并更新数据库", commandEntity.getId());
                }
                return;
            }
            // 获取该命令下所有主机命令，SQL逻辑已迁移到DAO层
            ClusterServiceCommandHostMapper hostMapper = (ClusterServiceCommandHostMapper) commandHostService
                    .getMapper();
            List<ClusterServiceCommandHostEntity> hostCommands = hostMapper
                    .selectByCommandId(commandEntity.getId());

            if (hostCommands == null || hostCommands.isEmpty()) {
                commandEntity.setCommandProgress(0L);
                if ((oldProgress == null || oldProgress != 0L)) {
                    this.updateById(commandEntity);
                    logger.info("命令 {} 无主机命令，进度设为0%并更新数据库", commandEntity.getId());
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
                logger.info("命令 {} 进度更新为 {}% 并更新数据库", commandEntity.getId(), finalProgress);
            }
        } catch (Exception e) {
            logger.error("计算命令进度时出错: {}", e.getMessage(), e);
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
            // SQL逻辑已迁移到DAO层
            ClusterServiceCommandHostMapper hostMapper = (ClusterServiceCommandHostMapper) commandHostService
                    .getMapper();
            List<ClusterServiceCommandHostEntity> hostCommands = hostMapper
                    .selectByCommandId(commandEntity.getId());

            if (hostCommands == null || hostCommands.isEmpty()) {
                commandEntity.setCommandState(CommandState.RUNNING);
                commandEntity.setCommandStateCode(CommandState.RUNNING.getValue());
                return;
            }
            boolean allCompleted = true;
            int failedCount = 0;
            int canceledCount = 0;
            for (ClusterServiceCommandHostEntity hostCommand : hostCommands) {
                // 实时聚合主机命令状态
                commandHostService.calculateRealTimeHostCommandState(hostCommand, true);
                if (CommandState.RUNNING.equals(hostCommand.getCommandState())) {
                    allCompleted = false;
                } else if (CommandState.FAILED.equals(hostCommand.getCommandState())) {
                    failedCount++;
                } else if (CommandState.CANCEL.equals(hostCommand.getCommandState())) {
                    canceledCount++;
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
                    commandEntity.setEndTime(LocalDateTime.now());
                    logger.info("命令 {} 状态变为 {}, 设置结束时间为 {}",
                            commandEntity.getId(),
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
                        commandEntity.getId(), commandEntity.getCommandState());
            }
        } catch (Exception e) {
            logger.error("实时计算命令状态出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 1、生成指令
     * 2、生成主机指令
     * 3、生产主机上操作指令
     */
    @Override
    public String generateServiceCommand(Long clusterId, CommandType commandType,
                                         List<String> serviceInstanceIds) {
        List<ClusterServiceCommandEntity> list = new ArrayList<>();
        List<ClusterServiceCommandHostEntity> commandHostList = new ArrayList<>();
        List<ClusterServiceCommandHostCommandEntity> hostCommandList = new ArrayList<>();
        List<Long> commandIds = new ArrayList<>();
        for (String serviceInstanceId : serviceInstanceIds) {
            Long id = Long.parseLong(serviceInstanceId);
            // 查询服务对应的服务角色实例
            List<ClusterServiceRoleInstanceDTO> roleInstanceDtoList = roleInstanceQueryService
                    .getServiceRoleInstanceListByServiceId(id);
            List<ClusterServiceRoleInstanceEntity> roleInstanceList = convertServiceRoleInstanceListToEntity(
                    roleInstanceDtoList);
            if (Objects.isNull(roleInstanceList) || roleInstanceList.isEmpty()) {
                continue;
            }
            ClusterServiceInstanceEntity serviceInstance = serviceInstanceService.getById(id);
            ClusterServiceCommandEntity commandEntity = generateCommandEntity(clusterId, commandType,
                    serviceInstance.getServiceName());
            Long commandId = commandEntity.getId();
            commandEntity.setServiceInstanceId(id);
            commandIds.add(commandId);
            list.add(commandEntity);

            HashMap<String, ClusterServiceCommandHostEntity> map = new HashMap<>();
            for (ClusterServiceRoleInstanceEntity roleInstance : roleInstanceList) {
                ClusterServiceCommandHostEntity commandHost;
                if (map.containsKey(roleInstance.getHostname())) {
                    commandHost = map.get(roleInstance.getHostname());
                } else {
                    commandHost = generateCommandHostEntity(commandId, roleInstance.getHostname());
                    commandHostList.add(commandHost);
                }
                ClusterServiceCommandHostCommandEntity hostCommand = generateCommandHostCommandEntity(
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
        return commandIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }

    @Override
    public String generateServiceRoleCommands(Long clusterId, CommandType commandType,
                                              Map<Long, List<String>> instanceIdMap) {
        String result = null;
        for (Map.Entry<Long, List<String>> entry : instanceIdMap.entrySet()) {
            result = generateServiceRoleCommand(clusterId, commandType, entry.getKey(), entry.getValue(), null);
        }
        return result;
    }

    @Override
    public String generateServiceRoleCommand(Long clusterId, CommandType commandType,
                                             Long serviceInstanceId,
                                             List<String> serviceRoleInstanceIds, RollingRestartInfo rollingRestartInfo) {
        List<ClusterServiceCommandEntity> list = new ArrayList<>();
        List<ClusterServiceCommandHostEntity> commandHostList = new ArrayList<>();
        List<ClusterServiceCommandHostCommandEntity> hostCommandList = new ArrayList<>();
        List<Long> commandIds = new ArrayList<>();

        ClusterServiceInstanceEntity serviceInstance = serviceInstanceService.getById(serviceInstanceId);
        ClusterServiceCommandEntity commandEntity = generateCommandEntity(clusterId, commandType,
                serviceInstance.getServiceName());
        Long commandId = commandEntity.getId();
        commandEntity.setServiceInstanceId(serviceInstanceId);
        commandIds.add(commandId);
        list.add(commandEntity);
        // 查询服务对应的服务角色实例
        HashMap<String, ClusterServiceCommandHostEntity> map = new HashMap<>();
        for (String serviceRoleInstanceId : serviceRoleInstanceIds) {
            Long id = Long.parseLong(serviceRoleInstanceId);
            ClusterServiceRoleInstanceDTO roleInstanceDto = roleInstanceQueryService.getByIdAsDto(id);
            ClusterServiceRoleInstanceEntity roleInstance = convertServiceRoleInstanceToEntity(roleInstanceDto);

            ClusterServiceCommandHostEntity commandHost;
            if (map.containsKey(roleInstance.getHostname())) {
                commandHost = map.get(roleInstance.getHostname());
            } else {
                commandHost = generateCommandHostEntity(commandId, roleInstance.getHostname());
                commandHostList.add(commandHost);
            }
            ClusterServiceCommandHostCommandEntity hostCommand = generateCommandHostCommandEntity(
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
        return commandIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }

    @Override
    public void startExecuteCommand(Long clusterId, String commandType, String commandIds) {
        List<Long> list = Arrays.stream(StrUtil.splitToLong(commandIds,",")).boxed().toList();
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
    public ClusterServiceCommandDTO getLastRestartCommand(Long serviceInstanceId) {
        // 创建基础查询条件
        int restartValue = CommandType.RESTART_SERVICE.getValue();
        int installValue = CommandType.INSTALL_SERVICE.getValue();

        // 先获取RESTART_SERVICE类型的命令
        ClusterServiceCommandEntity result = com.mybatisflex.core.query.QueryChain
                .of(ClusterServiceCommandEntity.class)
                .where(ClusterServiceCommandEntity::getServiceInstanceId).eq(serviceInstanceId)
                .and(ClusterServiceCommandEntity::getCommandType).eq(restartValue)
                .orderBy(ClusterServiceCommandEntity::getCreateTime).desc()
                .limit(1)
                .one();

        // 如果没有找到RESTART_SERVICE类型的命令，尝试获取INSTALL_SERVICE类型的命令
        if (result == null) {
            // SQL逻辑迁移到DAO层 - 需要在Mapper中添加对应方法
            result = getMapper().selectLatestByServiceInstanceIdAndCommandType(serviceInstanceId, installValue);
        }

        return result != null ? converter.entityToDto(result) : null;
    }

    @Override
    public ClusterServiceCommandDTO getCommandById(Long commandId) {
        // 将String类型的commandId转换为Long
        ClusterServiceCommandEntity entity = getById(commandId);
        return entity != null ? converter.entityToDto(entity) : null;
    }

    // DTO相关的CRUD方法实现
    @Override
    public ClusterServiceCommandDTO getByIdAsDto(String id) {
        // 将String类型的id转换为Long
        Long longId = Long.parseLong(id);
        ClusterServiceCommandEntity entity = getById(longId);
        return entity != null ? converter.entityToDto(entity) : null;
    }

    @Override
    public ClusterServiceCommandDTO saveCommand(ClusterServiceCommandDTO dto) {
        ClusterServiceCommandEntity entity = converter.dtoToEntity(dto);
        save(entity);
        return converter.entityToDto(entity);
    }

    @Override
    public void updateCommand(ClusterServiceCommandDTO dto) {
        ClusterServiceCommandEntity entity = converter.dtoToEntity(dto);
        updateById(entity);
    }

    // 辅助转换方法
    private ClusterServiceInstanceEntity convertServiceInstanceToEntity(ClusterServiceInstanceDTO dto) {
        if (dto == null)
            return null;
        ClusterServiceInstanceEntity entity = new ClusterServiceInstanceEntity();
        entity.setId(dto.id());
        entity.setServiceName(dto.serviceName());
        entity.setClusterId(dto.clusterId());
        return entity;
    }

    private ClusterServiceRoleInstanceEntity convertServiceRoleInstanceToEntity(ClusterServiceRoleInstanceDTO dto) {
        if (dto == null)
            return null;
        ClusterServiceRoleInstanceEntity entity = new ClusterServiceRoleInstanceEntity();
        entity.setId(dto.id());
        entity.setHostname(dto.hostname());
        entity.setServiceRoleName(dto.serviceRoleName());
        // 假设RoleType是枚举，需要从Integer转换
        entity.setRoleType(clusterServiceCommandHostCommandConverter.integerToRoleType(dto.roleType()));
        return entity;
    }

    private List<ClusterServiceRoleInstanceEntity> convertServiceRoleInstanceListToEntity(
            List<ClusterServiceRoleInstanceDTO> dtoList) {
        if (dtoList == null)
            return null;
        return dtoList.stream().map(this::convertServiceRoleInstanceToEntity).toList();
    }

    @Override
    public void updateCommandProgress(Long commandId, long progress) {
        // 将String类型的commandId转换为Long
        ClusterServiceCommandEntity entity = getById(commandId);
        if (entity != null) {
            entity.setCommandProgress(progress);
            updateById(entity);
        }
    }

    @Override
    public void updateCommandStateAndEndTime(Long commandId, CommandState commandState, LocalDateTime endTime) {
        // 将String类型的commandId转换为Long
        ClusterServiceCommandEntity entity = getById(commandId);
        if (entity != null) {
            entity.setCommandState(commandState);
            entity.setEndTime(endTime);
            updateById(entity);
        }
    }

    /**
     * 生成命令实体
     */
    private ClusterServiceCommandEntity generateCommandEntity(Long clusterId, CommandType commandType, String serviceName) {
        ClusterServiceCommandEntity command = new ClusterServiceCommandEntity();
        command.setId(IdUtil.getSnowflakeNextId()); // 手动生成ID，确保后续可以正确引用
        command.setClusterId(clusterId);
        command.setCommandType(commandType.getValue());
        command.setCommandName(serviceName);
        command.setServiceName(serviceName);
        command.setCommandState(CommandState.WAIT);
        command.setCommandProgress(0L);
        // 审计字段由监听器自动填充
        return command;
    }

    /**
     * 生成命令主机实体
     */
    private ClusterServiceCommandHostEntity generateCommandHostEntity(Long commandId, String hostname) {
        ClusterServiceCommandHostEntity commandHost = new ClusterServiceCommandHostEntity();
        commandHost.setId(IdUtil.getSnowflakeNextId()); // 手动生成ID，确保后续可以正确引用
        commandHost.setCommandId(commandId);
        commandHost.setHostname(hostname);
        commandHost.setCommandState(CommandState.WAIT);
        commandHost.setCommandProgress(0L);
        // 审计字段由监听器自动填充
        return commandHost;
    }

    /**
     * 生成命令主机命令实体
     */
    private ClusterServiceCommandHostCommandEntity generateCommandHostCommandEntity(
            CommandType commandType,
            Long commandId,
            String serviceRoleName,
            RoleType roleType,
            ClusterServiceCommandHostEntity commandHost) {

        ClusterServiceCommandHostCommandEntity hostCommand = new ClusterServiceCommandHostCommandEntity();
        hostCommand.setId(IdUtil.getSnowflakeNextId()); // 手动生成ID
        hostCommand.setCommandHostId(commandHost.getId());
        hostCommand.setCommandId(commandId);
        hostCommand.setCommandName(commandType.name());
        hostCommand.setCommandType(commandType.getValue());
        hostCommand.setServiceRoleName(serviceRoleName);
        hostCommand.setServiceRoleType(roleType);
        hostCommand.setCommandState(CommandState.WAIT);
        hostCommand.setCommandProgress(0);
        // 🔧 修复：设置hostname字段
        hostCommand.setHostname(commandHost.getHostname());
        // 审计字段由监听器自动填充
        return hostCommand;
    }


}
