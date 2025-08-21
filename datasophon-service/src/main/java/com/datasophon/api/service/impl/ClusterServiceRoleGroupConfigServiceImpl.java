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

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.datasophon.api.converter.ClusterServiceRoleGroupConfigConverter;
import com.datasophon.api.service.ClusterServiceRoleGroupConfigService;
import com.datasophon.common.dto.ClusterServiceRoleGroupConfigDTO;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfigEntity;
import com.datasophon.dao.mapper.ClusterServiceRoleGroupConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 集群服务角色组配置服务实现类
 * 提供集群服务角色组配置的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Service("clusterServiceRoleGroupConfigService")
public class ClusterServiceRoleGroupConfigServiceImpl
        extends ServiceImpl<ClusterServiceRoleGroupConfigMapper, ClusterServiceRoleGroupConfigEntity>
        implements ClusterServiceRoleGroupConfigService {

    @Autowired
    private ClusterServiceRoleGroupConfigConverter clusterServiceRoleGroupConfigConverter;

    @Override
    public ClusterServiceRoleGroupConfigDTO getConfigByRoleGroupId(Long roleGroupId) {
        ClusterServiceRoleGroupConfigEntity entity = getMapper().selectByRoleGroupId(roleGroupId);
        return clusterServiceRoleGroupConfigConverter.entityToDto(entity);
    }

    @Override
    public ClusterServiceRoleGroupConfigDTO getConfigByRoleGroupIdAndVersion(Long roleGroupId, Integer version) {
        ClusterServiceRoleGroupConfigEntity entity = getMapper().selectByRoleGroupIdAndVersion(roleGroupId, version);
        return clusterServiceRoleGroupConfigConverter.entityToDto(entity);
    }

    @Override
    public void removeAllByRoleGroupId(Long roleGroupId) {
        getMapper().deleteByRoleGroupId(roleGroupId);
    }

    @Override
    public List<ClusterServiceRoleGroupConfigDTO> listRoleGroupConfigsByRoleGroupIds(List<Integer> roleGroupIds) {
        List<ClusterServiceRoleGroupConfigEntity> entities = getMapper().selectByRoleGroupIds(roleGroupIds);
        return entities.stream()
                .map(clusterServiceRoleGroupConfigConverter::entityToDto)
                .toList();
    }

    @Override
    public ClusterServiceRoleGroupConfigDTO getByIdAsDto(Long id) {
        // Service层：Entity → DTO转换
        ClusterServiceRoleGroupConfigEntity entity = this.getById(id);
        return clusterServiceRoleGroupConfigConverter.entityToDto(entity);
    }

    @Override
    public void saveConfig(ClusterServiceRoleGroupConfigDTO dto) {
        // Service层：DTO → Entity转换
        ClusterServiceRoleGroupConfigEntity entity = clusterServiceRoleGroupConfigConverter.dtoToEntity(dto);
        this.save(entity);
    }

    @Override
    public void updateConfig(ClusterServiceRoleGroupConfigDTO dto) {
        // Service层：DTO → Entity转换
        ClusterServiceRoleGroupConfigEntity entity = clusterServiceRoleGroupConfigConverter.dtoToEntity(dto);
        this.updateById(entity);
    }

    @Override
    public List<ClusterServiceRoleGroupConfigEntity> getConfigVersionsByRoleGroupId(Long roleGroupId) {
        return getMapper().selectConfigVersionsByRoleGroupId(roleGroupId);
    }

    @Override
    public List<ClusterServiceRoleGroupConfigEntity> getLatestTwoConfigsByRoleGroupId(Long roleGroupId) {
        return getMapper().selectLatestTwoConfigsByRoleGroupId(roleGroupId);
    }
}
