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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datasophon.api.service.ClusterServiceCommandHostCommandService;
import com.datasophon.api.service.ClusterServiceCommandHostService;
import com.datasophon.common.Constants;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterServiceCommandHostEntity;
import com.datasophon.dao.entity.ClusterServiceCommandHostCommandEntity;
import com.datasophon.dao.enums.CommandState;
import com.datasophon.dao.mapper.ClusterServiceCommandHostMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("clusterServiceCommandHostService")
public class ClusterServiceCommandHostServiceImpl
        extends
        ServiceImpl<ClusterServiceCommandHostMapper, ClusterServiceCommandHostEntity>
        implements
        ClusterServiceCommandHostService {

    @Autowired
    private ClusterServiceCommandHostCommandService hostCommandService;

    @Autowired
    private ClusterServiceCommandHostMapper hostMapper;

    @Override
    public Result getCommandHostList(Integer clusterId, String commandId, Integer page, Integer pageSize) {
        Integer offset = (page - 1) * pageSize;
        LambdaQueryChainWrapper<ClusterServiceCommandHostEntity> wrapper = this.lambdaQuery()
                .eq(ClusterServiceCommandHostEntity::getCommandId, commandId);
        long total = wrapper.count();
        List<ClusterServiceCommandHostEntity> list = wrapper
                .orderByDesc(ClusterServiceCommandHostEntity::getCreateTime)
                .last("limit " + offset + "," + pageSize)
                .list();
        for (ClusterServiceCommandHostEntity commandHostEntity : list) {
            // 实时聚合主机命令进度和状态（只做内存聚合，不做数据库update）
            calculateHostCommandActualProgress(commandHostEntity, false);
            calculateRealTimeHostCommandState(commandHostEntity, false);
            commandHostEntity.setCommandStateCode(commandHostEntity.getCommandState().getValue());
        }
        return Result.success(list).put(Constants.TOTAL, total);
    }

    /**
     * 计算主机命令的实际进度（支持只做内存聚合或同时更新数据库）
     */
    public void calculateHostCommandActualProgress(ClusterServiceCommandHostEntity commandHostEntity,
            boolean updateDb) {
        try {
            Long oldProgress = commandHostEntity.getCommandProgress();

            if (CommandState.SUCCESS.equals(commandHostEntity.getCommandState())) {
                commandHostEntity.setCommandProgress(100L);
                if (updateDb && (oldProgress == null || oldProgress != 100L)) {
                    this.updateById(commandHostEntity);
                    System.out.println("主机命令 " + commandHostEntity.getCommandHostId() +
                            " 状态为成功，进度设为100%并更新数据库");
                }
                return;
            } else if (CommandState.FAILED.equals(commandHostEntity.getCommandState())) {
                commandHostEntity.setCommandProgress(100L);
                if (updateDb && (oldProgress == null || oldProgress != 100L)) {
                    this.updateById(commandHostEntity);
                    System.out.println("主机命令 " + commandHostEntity.getCommandHostId() +
                            " 状态为失败，进度设为100%并更新数据库");
                }
                return;
            } else if (CommandState.CANCEL.equals(commandHostEntity.getCommandState())) {
                commandHostEntity.setCommandProgress(100L);
                if (updateDb && (oldProgress == null || oldProgress != 100L)) {
                    this.updateById(commandHostEntity);
                    System.out.println("主机命令 " + commandHostEntity.getCommandHostId() +
                            " 状态为取消，进度设为100%并更新数据库");
                }
                return;
            }
            List<ClusterServiceCommandHostCommandEntity> hostCommands = hostCommandService.list(
                    new QueryWrapper<ClusterServiceCommandHostCommandEntity>()
                            .eq("command_host_id", commandHostEntity.getCommandHostId()));
            if (hostCommands == null || hostCommands.isEmpty()) {
                commandHostEntity.setCommandProgress(0L);
                if (updateDb && (oldProgress == null || oldProgress != 0L)) {
                    this.updateById(commandHostEntity);
                    System.out.println("主机命令 " + commandHostEntity.getCommandHostId() +
                            " 无子命令，进度设为0%并更新数据库");
                }
                return;
            }
            long totalProgress = 0;
            int completedCount = 0;
            int totalCount = hostCommands.size();
            for (ClusterServiceCommandHostCommandEntity hostCommand : hostCommands) {
                if (hostCommand.getCommandProgress() != null) {
                    totalProgress += hostCommand.getCommandProgress();
                    if (CommandState.SUCCESS.equals(hostCommand.getCommandState())) {
                        completedCount++;
                    }
                }
            }
            long avgProgress = totalCount > 0 ? totalProgress / totalCount : 0;
            long completedProgress = totalCount > 0 ? (completedCount * 100L) / totalCount : 0;
            long finalProgress = Math.max(avgProgress, completedProgress);
            commandHostEntity.setCommandProgress(finalProgress);

            // 如果需要更新数据库且进度有变化
            if (updateDb && (oldProgress == null || oldProgress != finalProgress)) {
                this.updateById(commandHostEntity);
                System.out.println("主机命令 " + commandHostEntity.getCommandHostId() +
                        " 进度更新为 " + finalProgress + "%并更新数据库");
            }
        } catch (Exception e) {
            System.err.println("计算主机命令进度时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 实时计算主机命令状态（支持只做内存聚合或同时更新数据库）
     */
    public void calculateRealTimeHostCommandState(ClusterServiceCommandHostEntity hostCommandEntity, boolean updateDb) {
        try {
            CommandState oldState = hostCommandEntity.getCommandState();

            List<ClusterServiceCommandHostCommandEntity> subCommands = hostCommandService.list(
                    new QueryWrapper<ClusterServiceCommandHostCommandEntity>()
                            .eq("command_host_id", hostCommandEntity.getCommandHostId()));
            if (subCommands == null || subCommands.isEmpty()) {
                hostCommandEntity.setCommandState(CommandState.RUNNING);
                hostCommandEntity.setCommandStateCode(CommandState.RUNNING.getValue());
                return;
            }
            boolean allCompleted = true;
            int failedCount = 0;
            int canceledCount = 0;
            int successCount = 0;
            for (ClusterServiceCommandHostCommandEntity subCommand : subCommands) {
                if (subCommand.getCommandProgress() == null || subCommand.getCommandProgress() < 100) {
                    allCompleted = false;
                }
                if (CommandState.FAILED.equals(subCommand.getCommandState())) {
                    failedCount++;
                } else if (CommandState.CANCEL.equals(subCommand.getCommandState())) {
                    canceledCount++;
                } else if (CommandState.SUCCESS.equals(subCommand.getCommandState())) {
                    successCount++;
                } else {
                    allCompleted = false;
                }
            }

            boolean stateChanged = false;

            if (allCompleted) {
                if (failedCount > 0) {
                    hostCommandEntity.setCommandState(CommandState.FAILED);
                    hostCommandEntity.setCommandStateCode(CommandState.FAILED.getValue());
                    stateChanged = !CommandState.FAILED.equals(oldState);
                } else if (canceledCount > 0) {
                    hostCommandEntity.setCommandState(CommandState.CANCEL);
                    hostCommandEntity.setCommandStateCode(CommandState.CANCEL.getValue());
                    stateChanged = !CommandState.CANCEL.equals(oldState);
                } else {
                    hostCommandEntity.setCommandState(CommandState.SUCCESS);
                    hostCommandEntity.setCommandStateCode(CommandState.SUCCESS.getValue());
                    stateChanged = !CommandState.SUCCESS.equals(oldState);
                }
            } else {
                hostCommandEntity.setCommandState(CommandState.RUNNING);
                hostCommandEntity.setCommandStateCode(CommandState.RUNNING.getValue());
                stateChanged = !CommandState.RUNNING.equals(oldState);
            }

            // 如果需要更新数据库且状态发生了变化
            if (updateDb && stateChanged) {
                this.updateById(hostCommandEntity);
                System.out.println("主机命令 " + hostCommandEntity.getCommandHostId() +
                        " 状态从 " + oldState + " 变为 " + hostCommandEntity.getCommandState() +
                        "，已更新数据库");
            }
        } catch (Exception e) {
            System.err.println("实时计算主机命令状态出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Long getCommandHostSizeByCommandId(String commandId) {
        return this.lambdaQuery().eq(ClusterServiceCommandHostEntity::getCommandId, commandId).count();
    }

    @Override
    public Integer getCommandHostTotalProgressByCommandId(String commandId) {
        return hostMapper.getCommandHostTotalProgressByCommandId(commandId);
    }

    @Override
    public List<ClusterServiceCommandHostEntity> findFailedCommandHost(String commandId) {
        return this.lambdaQuery()
                .eq(ClusterServiceCommandHostEntity::getCommandId, commandId)
                .eq(ClusterServiceCommandHostEntity::getCommandState, CommandState.FAILED)
                .list();
    }

    @Override
    public List<ClusterServiceCommandHostEntity> findCanceledCommandHost(String commandId) {
        return this.lambdaQuery()
                .eq(ClusterServiceCommandHostEntity::getCommandId, commandId)
                .eq(ClusterServiceCommandHostEntity::getCommandState, CommandState.CANCEL)
                .list();
    }

}
