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

package com.datasophon.api.service;

import com.datasophon.common.dto.ClusterServiceCommandDTO;
import com.datasophon.common.enums.CommandState;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.model.PageResult;
import com.datasophon.common.model.RollingRestartInfo;

import com.datasophon.dao.entity.ClusterServiceCommandEntity;
import com.mybatisflex.core.service.IService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 集群服务命令服务接口
 * 提供集群服务命令的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
public interface ClusterServiceCommandService extends IService<ClusterServiceCommandEntity> {

        /**
         * 生成命令
         */
        String generateCommand(Long clusterId, CommandType commandType, List<String> serviceNames);

        /**
         * 获取服务命令列表（分页）
         */
        PageResult<ClusterServiceCommandDTO> getServiceCommandlist(Long clusterId, Integer page, Integer pageSize);

        /**
         * 生成服务命令
         */
        String generateServiceCommand(Long clusterId, CommandType command, List<String> ids);

        /**
         * 生成服务角色命令集合
         */
        String generateServiceRoleCommands(Long clusterId, CommandType commandType,
                        Map<Long, List<String>> instanceIdMap);

        /**
         * 生成服务角色命令
         */
        String generateServiceRoleCommand(Long clusterId, CommandType command, Long serviceIntanceId,
                        List<String> ids, RollingRestartInfo rollingRestartInfo);

        /**
         * 启动执行命令
         */
        void startExecuteCommand(Long clusterId, String commandType, String commandIds);

        /**
         * 取消命令
         */
        void cancelCommand(String commandId);

        /**
         * 获取最后重启命令
         */
        ClusterServiceCommandDTO getLastRestartCommand(Long id);

        /**
         * 根据命令ID获取命令
         */
        ClusterServiceCommandDTO getCommandById(String commandId);

        /**
         * 根据ID获取命令DTO
         */
        ClusterServiceCommandDTO getByIdAsDto(String id);

        /**
         * 保存命令DTO
         */
        ClusterServiceCommandDTO saveCommand(ClusterServiceCommandDTO dto);

        /**
         * 更新命令
         */
        void updateCommand(ClusterServiceCommandDTO dto);

        /**
         * 更新命令进度
         */
        void updateCommandProgress(String commandId, long progress);

        /**
         * 更新命令状态和结束时间
         */
        void updateCommandStateAndEndTime(String commandId, CommandState commandState, LocalDateTime endTime);
}
