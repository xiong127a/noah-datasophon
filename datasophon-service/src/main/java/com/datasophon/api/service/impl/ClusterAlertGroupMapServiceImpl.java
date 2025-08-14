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

import com.datasophon.api.converter.ClusterAlertGroupMapConverter;
import com.datasophon.api.service.ClusterAlertGroupMapService;
import com.datasophon.common.dto.ClusterAlertGroupMapDTO;
import com.datasophon.dao.entity.ClusterAlertGroupMapEntity;
import com.datasophon.dao.mapper.ClusterAlertGroupMapMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 集群告警组映射服务实现类
 * 提供集群告警组映射的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Service("clusterAlertGroupMapService")
public class ClusterAlertGroupMapServiceImpl extends ServiceImpl<ClusterAlertGroupMapMapper, ClusterAlertGroupMapEntity>
        implements ClusterAlertGroupMapService {

    @Autowired
    private ClusterAlertGroupMapConverter clusterAlertGroupMapConverter;

    @Override
    public List<ClusterAlertGroupMapDTO> getByClusterId(Long clusterId) {
        // DAO层：使用Mapper查询
        List<ClusterAlertGroupMapEntity> entities = getMapper().selectByClusterId(clusterId);
        // Service层：Entity → DTO转换
        return entities.stream()
                .map(clusterAlertGroupMapConverter::entityToDto)
                .toList();
    }

    @Override
    public ClusterAlertGroupMapDTO getByIdAsDto(Long id) {
        // Service层：Entity → DTO转换
        ClusterAlertGroupMapEntity entity = this.getById(id);
        return clusterAlertGroupMapConverter.entityToDto(entity);
    }

    @Override
    public void saveAlertGroupMap(ClusterAlertGroupMapDTO dto) {
        // Service层：DTO → Entity转换
        ClusterAlertGroupMapEntity entity = clusterAlertGroupMapConverter.dtoToEntity(dto);
        this.save(entity);
    }

    @Override
    public void updateAlertGroupMap(ClusterAlertGroupMapDTO dto) {
        // Service层：DTO → Entity转换
        ClusterAlertGroupMapEntity entity = clusterAlertGroupMapConverter.dtoToEntity(dto);
        this.updateById(entity);
    }
}