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

import com.datasophon.common.dto.ClusterYarnQueueDTO;
import com.datasophon.common.exception.BusinessException;
import com.datasophon.common.model.PageResult;
import com.datasophon.dao.entity.ClusterYarnQueue;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 集群Yarn队列服务接口
 * 提供Yarn队列的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
public interface ClusterYarnQueueService extends IService<ClusterYarnQueue> {

    /**
     * 分页查询队列列表
     */
    PageResult<ClusterYarnQueueDTO> listByPage(Integer clusterId, Integer page, Integer pageSize);

    /**
     * 保存队列
     */
    ClusterYarnQueueDTO saveQueue(ClusterYarnQueueDTO clusterYarnQueueDTO) throws BusinessException;

    /**
     * 刷新队列到Yarn
     */
    void refreshQueues(Integer clusterId) throws BusinessException;

    /**
     * 根据队列名称获取队列
     */
    ClusterYarnQueueDTO getQueueByName(Integer clusterId, String queueName);

    /**
     * 根据ID获取队列DTO
     */
    ClusterYarnQueueDTO getByIdAsDto(Integer id);

    /**
     * 根据集群ID获取所有队列
     */
    List<ClusterYarnQueueDTO> getQueuesByClusterId(Integer clusterId);

    /**
     * 更新队列
     */
    ClusterYarnQueueDTO updateQueue(ClusterYarnQueueDTO dto) throws BusinessException;

    /**
     * 删除队列
     */
    boolean deleteQueue(Integer id);
}
