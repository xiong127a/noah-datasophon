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

import com.datasophon.api.service.ClusterServiceCommandHostCommandService;
import com.datasophon.api.service.ClusterServiceCommandHostService;
import com.datasophon.common.Constants;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterServiceCommandHostCommandEntity;
import com.datasophon.dao.entity.ClusterServiceCommandHostEntity;
import com.datasophon.dao.enums.CommandState;
import com.datasophon.dao.mapper.ClusterServiceCommandHostMapper;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service("clusterServiceCommandHostService")
public class ClusterServiceCommandHostServiceImpl
        extends
        ServiceImpl<ClusterServiceCommandHostMapper, ClusterServiceCommandHostEntity>
        implements
        ClusterServiceCommandHostService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterServiceCommandHostServiceImpl.class);

    // 定义命令进度常量
    private static final Long PROGRESS_COMPLETE = 100L;
    private static final Long PROGRESS_INITIAL = 0L;

    private final ClusterServiceCommandHostCommandService hostCommandService;

    private final ClusterServiceCommandHostMapper hostMapper;
    @Autowired
    public ClusterServiceCommandHostServiceImpl(ClusterServiceCommandHostCommandService hostCommandService, ClusterServiceCommandHostMapper hostMapper) {
        this.hostCommandService = hostCommandService;
        this.hostMapper = hostMapper;
    }

    @Override
    public Result getCommandHostList(Integer clusterId, String commandId, Integer page, Integer pageSize) {
        // 使用分页对象代替手动计算偏移量
        com.mybatisflex.core.paginate.Page<ClusterServiceCommandHostEntity> flexPage = new com.mybatisflex.core.paginate.Page<>(
                page, pageSize);

        // 构建查询条件并执行分页查询
        com.mybatisflex.core.paginate.Page<ClusterServiceCommandHostEntity> resultPage = QueryChain
                .of(ClusterServiceCommandHostEntity.class)
                .where(ClusterServiceCommandHostEntity::getCommandId).eq(commandId)
                .orderBy(ClusterServiceCommandHostEntity::getCreateTime).desc()
                .page(flexPage);

        // 处理查询结果
        List<ClusterServiceCommandHostEntity> list = resultPage.getRecords();
        for (ClusterServiceCommandHostEntity commandHostEntity : list) {
            // 实时聚合主机命令进度和状态（只做内存聚合，不做数据库update）
            calculateHostCommandActualProgress(commandHostEntity, false);
            calculateRealTimeHostCommandState(commandHostEntity, false);
            commandHostEntity.setCommandStateCode(commandHostEntity.getCommandState().getValue());
        }

        return Result.success(list).put(Constants.TOTAL, resultPage.getTotalRow());
    }

    /**
     * 计算主机命令的实际进度（支持只做内存聚合或同时更新数据库）
     */
    @Override
    public void calculateHostCommandActualProgress(ClusterServiceCommandHostEntity commandHostEntity,
                                                   boolean updateDb) {
        try {
            Long oldProgress = commandHostEntity.getCommandProgress();
            CommandState currentState = commandHostEntity.getCommandState();

            // 对于已完成状态（成功、失败、取消），直接设置进度为100%
            if (isTerminalState(currentState)) {
                commandHostEntity.setCommandProgress(PROGRESS_COMPLETE);
                if (shouldUpdateProgress(updateDb, oldProgress, PROGRESS_COMPLETE)) {
                    this.updateById(commandHostEntity);
                    logProgressUpdate(commandHostEntity, currentState.toString());
                }
                return;
            }

            // 获取该主机命令下的所有子命令
            List<ClusterServiceCommandHostCommandEntity> hostCommands = getHostCommands(
                    commandHostEntity.getCommandHostId());

            // 如果没有子命令，设置进度为0%
            if (hostCommands == null || hostCommands.isEmpty()) {
                commandHostEntity.setCommandProgress(PROGRESS_INITIAL);
                if (shouldUpdateProgress(updateDb, oldProgress, PROGRESS_INITIAL)) {
                    this.updateById(commandHostEntity);
                    logger.info("主机命令 {} 无子命令，进度设为0%并更新数据库", commandHostEntity.getCommandHostId());
                }
                return;
            }

            // 计算所有子命令的平均进度
            long finalProgress = getFinalProgress(hostCommands);

            commandHostEntity.setCommandProgress(finalProgress);

            // 如果需要更新数据库且进度有变化
            if (shouldUpdateProgress(updateDb, oldProgress, finalProgress)) {
                this.updateById(commandHostEntity);
                logger.info("主机命令 {} 进度更新为 {}% 并更新数据库",
                        commandHostEntity.getCommandHostId(), finalProgress);
            }
        } catch (Exception e) {
            logger.error("计算主机命令进度时出错: {}", e.getMessage(), e);
        }
    }

    private long getFinalProgress(List<ClusterServiceCommandHostCommandEntity> hostCommands) {
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

        // 计算平均进度和完成百分比
        long avgProgress = calculateAverage(totalProgress, totalCount);
        long completedProgress = calculatePercentage(completedCount, totalCount);
        return Math.max(avgProgress, completedProgress);
    }

    /**
     * 实时计算主机命令状态（支持只做内存聚合或同时更新数据库）
     */
    @Override
    public void calculateRealTimeHostCommandState(ClusterServiceCommandHostEntity hostCommandEntity, boolean updateDb) {
        try {
            CommandState oldState = hostCommandEntity.getCommandState();

            // 获取该主机命令下的所有子命令
            List<ClusterServiceCommandHostCommandEntity> subCommands = getHostCommands(
                    hostCommandEntity.getCommandHostId());

            if (subCommands == null || subCommands.isEmpty()) {
                updateEntityState(hostCommandEntity, CommandState.RUNNING);
                return;
            }

            // 统计子命令状态
            boolean allCompleted = true;
            int failedCount = 0;
            int canceledCount = 0;
            int successCount = 0;

            for (ClusterServiceCommandHostCommandEntity subCommand : subCommands) {
                // 检查是否所有子命令都已完成
                if (isCommandIncomplete(subCommand)) {
                    allCompleted = false;
                }

                // 统计不同状态的子命令数量
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

            // 根据统计结果确定主机命令的状态
            CommandState newState;
            if (allCompleted) {
                if (failedCount > 0) {
                    newState = CommandState.FAILED;
                } else if (canceledCount > 0) {
                    newState = CommandState.CANCEL;
                } else {
                    newState = CommandState.SUCCESS;
                }
            } else {
                newState = CommandState.RUNNING;
            }

            boolean stateChanged = !Objects.equals(oldState, newState);
            updateEntityState(hostCommandEntity, newState);

            // 如果需要更新数据库且状态发生了变化
            if (updateDb && stateChanged) {
                this.updateById(hostCommandEntity);
                logger.info("主机命令 {} 状态从 {} 变为 {}，已更新数据库",
                        hostCommandEntity.getCommandHostId(), oldState, newState);
            }
        } catch (Exception e) {
            logger.error("实时计算主机命令状态出错: {}", e.getMessage(), e);
        }
    }

    @Override
    public Long getCommandHostSizeByCommandId(String commandId) {
        return QueryChain.of(ClusterServiceCommandHostEntity.class)
                .where(ClusterServiceCommandHostEntity::getCommandId).eq(commandId)
                .count();
    }

    @Override
    public Integer getCommandHostTotalProgressByCommandId(String commandId) {
        return hostMapper.getCommandHostTotalProgressByCommandId(commandId);
    }

    @Override
    public List<ClusterServiceCommandHostEntity> findFailedCommandHost(String commandId) {
        return QueryChain.of(ClusterServiceCommandHostEntity.class)
                .where(ClusterServiceCommandHostEntity::getCommandId).eq(commandId)
                .and(ClusterServiceCommandHostEntity::getCommandState).eq(CommandState.FAILED)
                .list();
    }

    @Override
    public List<ClusterServiceCommandHostEntity> findCanceledCommandHost(String commandId) {
        return QueryChain.of(ClusterServiceCommandHostEntity.class)
                .where(ClusterServiceCommandHostEntity::getCommandId).eq(commandId)
                .and(ClusterServiceCommandHostEntity::getCommandState).eq(CommandState.CANCEL)
                .list();
    }

    // ========== 辅助方法 ==========

    /**
     * 判断命令是否处于终止状态（成功、失败或取消）
     */
    private boolean isTerminalState(CommandState state) {
        return CommandState.SUCCESS.equals(state)
                || CommandState.FAILED.equals(state)
                || CommandState.CANCEL.equals(state);
    }

    /**
     * 判断是否需要更新进度
     */
    private boolean shouldUpdateProgress(boolean updateDb, Long oldProgress, Long newProgress) {
        return updateDb && (oldProgress == null || !oldProgress.equals(newProgress));
    }

    /**
     * 记录进度更新日志
     */
    private void logProgressUpdate(ClusterServiceCommandHostEntity entity, String state) {
        logger.info("主机命令 {} 状态为{}，进度设为100%并更新数据库",
                entity.getCommandHostId(), state);
    }

    /**
     * 获取主机命令的所有子命令
     */
    private List<ClusterServiceCommandHostCommandEntity> getHostCommands(String commandHostId) {
        return hostCommandService.list(
                QueryWrapper.create()
                        .where(ClusterServiceCommandHostCommandEntity::getCommandHostId).eq(commandHostId));
    }

    /**
     * 计算平均值，避免除零错误
     */
    private long calculateAverage(long total, int count) {
        return count > 0 ? total / count : 0;
    }

    /**
     * 计算百分比，避免除零错误
     */
    private long calculatePercentage(int part, int total) {
        return total > 0 ? (part * PROGRESS_COMPLETE) / total : 0;
    }

    /**
     * 判断命令是否未完成
     */
    private boolean isCommandIncomplete(ClusterServiceCommandHostCommandEntity command) {
        return command.getCommandProgress() == null || command.getCommandProgress() < PROGRESS_COMPLETE;
    }

    /**
     * 更新实体状态
     */
    private void updateEntityState(ClusterServiceCommandHostEntity entity, CommandState state) {
        entity.setCommandState(state);
        entity.setCommandStateCode(state.getValue());
    }
}
