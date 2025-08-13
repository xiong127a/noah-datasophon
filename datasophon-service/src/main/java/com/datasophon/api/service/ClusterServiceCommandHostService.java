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

import com.datasophon.common.dto.ClusterServiceCommandHostDTO;
import com.datasophon.common.enums.CommandState;
import com.datasophon.dao.entity.ClusterServiceCommandHostEntity;
import com.datasophon.common.model.PageResult;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 集群服务命令主机服务接口
 * 提供集群服务命令主机的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
public interface ClusterServiceCommandHostService extends IService<ClusterServiceCommandHostEntity> {

    /**
     * 获取命令主机列表（分页）
     */
    PageResult<ClusterServiceCommandHostDTO> getCommandHostList(Long clusterId, String commandId, Integer page,
            Integer pageSize);

    /**
     * 根据命令ID获取命令主机大小
     */
    Long getCommandHostSizeByCommandId(String commandId);

    /**
     * 根据命令ID获取命令主机总进度
     */
    Integer getCommandHostTotalProgressByCommandId(String commandId);

    /**
     * 查找失败的命令主机
     */
    List<ClusterServiceCommandHostDTO> findFailedCommandHost(String commandId);

    /**
     * 查找取消的命令主机
     */
    List<ClusterServiceCommandHostDTO> findCanceledCommandHost(String commandId);

    /**
     * 计算主机命令的实际进度
     * 
     * @param commandHostEntity 主机命令实体
     * @param updateDb          是否更新数据库
     */
    void calculateHostCommandActualProgress(ClusterServiceCommandHostEntity commandHostEntity, boolean updateDb);

    /**
     * 实时计算主机命令状态
     * 
     * @param hostCommandEntity 主机命令实体
     * @param updateDb          是否更新数据库
     */
    void calculateRealTimeHostCommandState(ClusterServiceCommandHostEntity hostCommandEntity, boolean updateDb);

    /**
     * 根据ID获取命令主机DTO
     */
    ClusterServiceCommandHostDTO getByIdAsDto(String id);

    /**
     * 保存命令主机DTO
     */
    ClusterServiceCommandHostDTO saveCommandHost(ClusterServiceCommandHostDTO dto);

    /**
     * 更新命令主机
     */
    void updateCommandHost(ClusterServiceCommandHostDTO dto);

    /**
     * 根据命令主机ID获取命令主机信息
     */
    ClusterServiceCommandHostDTO getCommandHostByCommandHostId(String commandHostId);

    /**
     * 更新命令主机进度
     */
    void updateCommandHostProgress(String commandHostId, long progress);

    /**
     * 更新命令主机状态
     */
    void updateCommandHostState(String commandHostId, CommandState commandState);
}
