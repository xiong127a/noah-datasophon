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

import com.datasophon.common.dto.AutoScaleTaskDTO;
import com.datasophon.common.model.PageResult;
import com.datasophon.dao.entity.AutoScaleTaskEntity;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 自动伸缩服务
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-01
 */
public interface AutoScaleService extends IService<AutoScaleTaskEntity> {

    /**
     * 创建自动伸缩任务
     * 
     * @param taskDTO 任务DTO
     * @return 创建的任务DTO
     */
    AutoScaleTaskDTO createAutoScaleTask(AutoScaleTaskDTO taskDTO);

    /**
     * 更新自动伸缩任务
     * 
     * @param taskDTO 任务DTO
     * @return 更新的任务DTO
     */
    AutoScaleTaskDTO updateAutoScaleTask(AutoScaleTaskDTO taskDTO);

    /**
     * 分页查询自动伸缩任务
     * 
     * @param clusterId 集群ID
     * @param page      页码
     * @param pageSize  页大小
     * @return 分页结果
     */
    PageResult<AutoScaleTaskDTO> getAutoScaleTasks(Long clusterId, Integer page, Integer pageSize);

    /**
     * 根据集群ID获取所有启用的自动伸缩任务
     * 
     * @param clusterId 集群ID
     * @return 任务列表
     */
    List<AutoScaleTaskDTO> getEnabledTasksByClusterId(Long clusterId);

    /**
     * 删除自动伸缩任务
     * 
     * @param taskId 任务ID
     * @return 是否删除成功
     */
    boolean deleteAutoScaleTask(Long taskId);

    /**
     * 启用/禁用自动伸缩任务
     * 
     * @param taskId  任务ID
     * @param enabled 是否启用
     * @return 更新的任务DTO
     */
    AutoScaleTaskDTO toggleAutoScaleTask(Long taskId, Boolean enabled);

    /**
     * 检查集群的自动伸缩是否启用
     * 
     * @param clusterId 集群ID
     * @return 是否启用
     */
    boolean isAutoScaleEnabled(Long clusterId);
}