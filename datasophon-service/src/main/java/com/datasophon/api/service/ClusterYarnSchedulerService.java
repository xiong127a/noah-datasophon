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

import com.datasophon.common.dto.ClusterYarnSchedulerDTO;
import com.datasophon.dao.entity.ClusterYarnSchedulerEntity;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 集群Yarn调度器服务接口
 * 提供Yarn调度器的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
public interface ClusterYarnSchedulerService extends IService<ClusterYarnSchedulerEntity> {

    /**
     * 根据集群ID获取调度器
     */
    ClusterYarnSchedulerDTO getScheduler(Long clusterId);

    /**
     * 创建默认Yarn调度器
     */
    ClusterYarnSchedulerDTO createDefaultYarnScheduler(Long clusterId);

    /**
     * 根据ID获取调度器DTO
     */
    ClusterYarnSchedulerDTO getByIdAsDto(Long id);

    /**
     * 根据集群ID获取所有调度器
     */
    List<ClusterYarnSchedulerDTO> getSchedulersByClusterId(Long clusterId);

    /**
     * 保存或更新调度器
     */
    ClusterYarnSchedulerDTO saveOrUpdateScheduler(ClusterYarnSchedulerDTO dto);

    /**
     * 删除调度器
     */
    boolean deleteScheduler(Long id);
}
