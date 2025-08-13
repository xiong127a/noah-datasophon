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

import com.datasophon.api.converter.ClusterRackConverter;
import com.datasophon.common.dto.ClusterRackDTO;
import com.datasophon.common.enums.Status;
import com.datasophon.api.service.ClusterRackService;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.utils.string.validator.GeneralValidator;
import com.datasophon.api.utils.string.validator.LengthValidator;
import com.datasophon.api.utils.string.validator.NotEmptyValidator;
import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.dao.entity.ClusterRackEntity;
import com.datasophon.dao.mapper.ClusterRackMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 集群机架服务实现类
 * 提供集群机架的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Service("clusterRackService")
public class ClusterRackServiceImpl extends ServiceImpl<ClusterRackMapper, ClusterRackEntity>
        implements ClusterRackService {

    @Autowired
    private ClusterRackConverter clusterRackConverter;

    @Autowired
    private ClusterHostService hostService;

    @Override
    public List<ClusterRackDTO> queryClusterRack(Long clusterId) {
        List<ClusterRackEntity> entities = getMapper().selectByClusterId(clusterId);
        return clusterRackConverter.entityListToDtoList(entities);
    }

    @Override
    public ClusterRackDTO saveRack(Long clusterId, String rack) {
        // 机架名校验
        NotEmptyValidator notEmptyValidator = new NotEmptyValidator();
        GeneralValidator generalValidator = new GeneralValidator();
        LengthValidator lengthValidator = new LengthValidator();
        notEmptyValidator.setNext(generalValidator);
        generalValidator.setNext(lengthValidator);
        try {
            notEmptyValidator.validate(rack);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        // 重复校验
        boolean exists = getMapper().existsByClusterIdAndRack(clusterId, rack);

        if (exists) {
            throw new RuntimeException("机架名称重复");
        }

        ClusterRackEntity clusterRackEntity = new ClusterRackEntity();
        clusterRackEntity.setRack(rack);
        clusterRackEntity.setClusterId(clusterId);
        this.save(clusterRackEntity);
        // Service层：Entity → DTO转换
        return clusterRackConverter.entityToDto(clusterRackEntity);
    }

    @Override
    public boolean deleteRack(Integer rackId) {
        ClusterRackEntity clusterRackEntity = this.getById(rackId);
        if (clusterRackEntity == null) {
            throw new RuntimeException("Rack not found with id: " + rackId);
        }
        if (rackInUse(clusterRackEntity)) {
            throw new RuntimeException(Status.RACK_IS_USING.getMsg());
        }
        this.removeById(rackId);
        return true;
    }

    @Override
    public void createDefaultRack(Long clusterId) {
        ClusterRackEntity clusterRackEntity = new ClusterRackEntity();
        clusterRackEntity.setRack("/default-rack");
        clusterRackEntity.setClusterId(clusterId);
        this.save(clusterRackEntity);
    }

    private boolean rackInUse(ClusterRackEntity clusterRackEntity) {
        List<ClusterHostEntity> list = hostService.getClusterHostByRack(clusterRackEntity.getClusterId(), clusterRackEntity.getRack());
        return !list.isEmpty();
    }

    // 新增DTO方法实现
    @Override
    public ClusterRackDTO getByIdAsDto(Integer id) {
        ClusterRackEntity entity = this.getById(id);
        return clusterRackConverter.entityToDto(entity);
    }

    @Override
    public ClusterRackDTO saveRackDto(ClusterRackDTO dto) {
        ClusterRackEntity entity = clusterRackConverter.dtoToEntity(dto);
        this.save(entity);
        return clusterRackConverter.entityToDto(entity);
    }

    @Override
    public void updateRack(ClusterRackDTO dto) {
        ClusterRackEntity entity = clusterRackConverter.dtoToEntity(dto);
        this.updateById(entity);
    }
}
