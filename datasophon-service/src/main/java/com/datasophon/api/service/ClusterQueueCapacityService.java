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

import com.datasophon.common.dto.ClusterQueueCapacityDTO;
import com.datasophon.dao.entity.ClusterQueueCapacityEntity;
import com.datasophon.dao.model.ClusterQueueCapacityList;
import com.mybatisflex.core.service.IService;

/**
 * 集群队列容量服务接口
 * 提供集群队列容量的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
public interface ClusterQueueCapacityService extends IService<ClusterQueueCapacityEntity> {

    /**
     * 刷新队列配置到YARN
     */
    boolean refreshToYarn(Long clusterId) throws Exception;

    /**
     * 创建默认队列
     */
    void createDefaultQueue(Long clusterId);

    /**
     * 列表查询队列容量（返回树形结构）
     */
    ClusterQueueCapacityList listCapacityQueue(Long clusterId);

    /**
     * 根据ID获取队列容量DTO
     */
    ClusterQueueCapacityDTO getByIdAsDto(Long id);

    /**
     * 保存队列容量DTO
     */
    ClusterQueueCapacityDTO saveQueueCapacity(ClusterQueueCapacityDTO dto);

    /**
     * 更新队列容量
     */
    void updateQueueCapacity(ClusterQueueCapacityDTO dto);

    /**
     * 根据集群ID、队列名称和父队列名称获取队列
     */
    ClusterQueueCapacityEntity getByClusterIdAndQueueName(Long clusterId, String queueName, String parentQueueName);
}
