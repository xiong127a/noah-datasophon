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

import cn.hutool.core.collection.CollUtil;
import com.datasophon.api.converter.ClusterVariableConverter;
import com.datasophon.api.service.ClusterVariableService;
import com.datasophon.common.dto.ClusterVariableDTO;
import com.datasophon.dao.entity.ClusterVariable;
import com.datasophon.dao.mapper.ClusterVariableMapper;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 集群变量服务实现
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Service("clusterVariableService")
@Transactional
public class ClusterVariableServiceImpl extends ServiceImpl<ClusterVariableMapper, ClusterVariable>
        implements ClusterVariableService {

    @Autowired
    private ClusterVariableConverter clusterVariableConverter;

    @Override
    public ClusterVariableDTO getVariableByVariableName(String variableName, Integer clusterId) {
        // SQL逻辑迁移到DAO层
        List<ClusterVariable> list = getMapper().selectByVariableNameAndClusterId(variableName, clusterId);

        if (CollUtil.isNotEmpty(list)) {
            ClusterVariable entity = list.getFirst();
            return clusterVariableConverter.entityToDto(entity);
        }
        return null;
    }

    @Override
    public List<ClusterVariableDTO> getVariablesByClusterId(Integer clusterId) {
        // SQL逻辑迁移到DAO层
        List<ClusterVariable> entities = getMapper().selectByClusterId(clusterId);

        return clusterVariableConverter.entityListToDtoList(entities);
    }

    @Override
    public ClusterVariableDTO saveOrUpdateVariable(ClusterVariableDTO dto) {
        ClusterVariable entity = clusterVariableConverter.dtoToEntity(dto);
        saveOrUpdate(entity);
        return clusterVariableConverter.entityToDto(entity);
    }

    @Override
    public ClusterVariableDTO getByIdAsDto(Integer id) {
        ClusterVariable entity = getById(id);
        return Objects.nonNull(entity) ? clusterVariableConverter.entityToDto(entity) : null;
    }

    @Override
    public boolean deleteVariable(Integer id) {
        return removeById(id);
    }
}
