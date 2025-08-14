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

import com.datasophon.api.converter.ClusterYarnSchedulerConverter;
import com.datasophon.api.service.ClusterYarnSchedulerService;
import com.datasophon.common.dto.ClusterYarnSchedulerDTO;
import com.datasophon.dao.entity.ClusterYarnSchedulerEntity;
import com.datasophon.dao.mapper.ClusterYarnSchedulerMapper;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 集群Yarn调度器服务实现
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Service("clusterYarnSchedulerService")
@Transactional
public class ClusterYarnSchedulerServiceImpl extends ServiceImpl<ClusterYarnSchedulerMapper, ClusterYarnSchedulerEntity>
        implements ClusterYarnSchedulerService {

    // 定义常量
    private static final String CAPACITY_SCHEDULER = "capacity";
    private static final int SCHEDULER_IN_USE = 1;

    @Autowired
    private ClusterYarnSchedulerConverter clusterYarnSchedulerConverter;

    @Override
    public ClusterYarnSchedulerDTO getScheduler(Long clusterId) {
        // SQL逻辑迁移到DAO层
        ClusterYarnSchedulerEntity entity = getMapper().selectByClusterId(clusterId);
        return Objects.nonNull(entity) ? clusterYarnSchedulerConverter.entityToDto(entity) : null;
    }

    @Override
    public ClusterYarnSchedulerDTO createDefaultYarnScheduler(Long clusterId) {
        ClusterYarnSchedulerEntity scheduler = new ClusterYarnSchedulerEntity();
        scheduler.setScheduler(CAPACITY_SCHEDULER);
        scheduler.setClusterId(clusterId);
        scheduler.setInUse(SCHEDULER_IN_USE);
        this.save(scheduler);
        return clusterYarnSchedulerConverter.entityToDto(scheduler);
    }

    @Override
    public ClusterYarnSchedulerDTO getByIdAsDto(Long id) {
        ClusterYarnSchedulerEntity entity = getById(id);
        return Objects.nonNull(entity) ? clusterYarnSchedulerConverter.entityToDto(entity) : null;
    }

    @Override
    public List<ClusterYarnSchedulerDTO> getSchedulersByClusterId(Long clusterId) {
        // SQL逻辑迁移到DAO层
        List<ClusterYarnSchedulerEntity> entities = getMapper().selectAllByClusterId(clusterId);
        return clusterYarnSchedulerConverter.entityListToDtoList(entities);
    }

    @Override
    public ClusterYarnSchedulerDTO saveOrUpdateScheduler(ClusterYarnSchedulerDTO dto) {
        ClusterYarnSchedulerEntity entity = clusterYarnSchedulerConverter.dtoToEntity(dto);
        saveOrUpdate(entity);
        return clusterYarnSchedulerConverter.entityToDto(entity);
    }

    @Override
    public boolean deleteScheduler(Long id) {
        return removeById(id);
    }
}
