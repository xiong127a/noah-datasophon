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

import com.datasophon.api.converter.ClusterServiceRoleInstanceWebuisConverter;
import com.datasophon.api.service.ClusterServiceRoleInstanceWebuisService;
import com.datasophon.common.dto.ClusterServiceRoleInstanceWebuisDTO;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceWebuisEntity;
import com.datasophon.dao.mapper.ClusterServiceRoleInstanceWebuisMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 集群服务角色实例WebUI服务实现
 * 继承ServiceImpl<ClusterServiceRoleInstanceWebuisMapper, ClusterServiceRoleInstanceWebuisEntity>，获得标准CRUD能力
 * 按照架构重构规范，ServiceImpl返回DTO，不返回Result，使用JDK21现代特性
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-06
 */
@Slf4j
@Service("clusterServiceRoleInstanceWebuisService")
public class ClusterServiceRoleInstanceWebuisServiceImpl
        extends ServiceImpl<ClusterServiceRoleInstanceWebuisMapper, ClusterServiceRoleInstanceWebuisEntity>
        implements ClusterServiceRoleInstanceWebuisService {

    @Autowired
    private ClusterServiceRoleInstanceWebuisConverter webuisConverter;

    private static final String ACTIVE = "(Active)";

    private static final String STANDBY = "(Standby)";

    @Override
    public List<ClusterServiceRoleInstanceWebuisDTO> getWebUis(Long serviceInstanceId) {
        log.debug("根据服务实例ID获取WebUI列表: {}", serviceInstanceId);
        
        // 调用DAO层方法，获取Entity列表
        var webuisEntities = getMapper().selectByServiceInstanceId(serviceInstanceId);
        
        // Entity列表转DTO列表 - 使用JDK21特性
        return webuisEntities.stream()
                .map(webuisConverter::entityToDto)
                .toList();
    }

    @Override
    public void removeByServiceInsId(Long serviceInstanceId) {
        getMapper().deleteByServiceInstanceId(serviceInstanceId);
    }

    @Override
    public void updateWebUiToActive(Long roleInstanceId) {
        updateWebUiName(roleInstanceId, ACTIVE);
    }

    @Override
    public ClusterServiceRoleInstanceWebuisDTO getRoleInstanceWebUi(Long roleInstanceId) {
        log.debug("根据角色实例ID获取WebUI: {}", roleInstanceId);
        
        var webuisEntity = getMapper().selectByServiceRoleInstanceId(roleInstanceId);
        return webuisEntity != null ? webuisConverter.entityToDto(webuisEntity) : null;
    }

    @Override
    public void removeByRoleInsIds(List<Long> roleInstanceIds) {
        log.debug("批量删除角色实例WebUI: {}", roleInstanceIds);
        getMapper().deleteByServiceRoleInstanceIds(new java.util.ArrayList<>(roleInstanceIds));
    }

    @Override
    public void updateWebUiToStandby(Long roleInstanceId) {
        updateWebUiName(roleInstanceId, STANDBY);
    }

    @Override
    public List<ClusterServiceRoleInstanceWebuisDTO> listWebUisByServiceInstanceId(Long serviceInstanceId) {
        log.debug("根据服务实例ID获取WebUI列表（别名方法）: {}", serviceInstanceId);
        
        // 复用getWebUis方法
        return getWebUis(serviceInstanceId);
    }

    private void updateWebUiName(Long roleInstanceId, String state) {
        List<ClusterServiceRoleInstanceWebuisEntity> webuisList = getMapper()
                .selectListByServiceRoleInstanceId(roleInstanceId);

        if (webuisList.isEmpty()) {
            return;
        }

        for (ClusterServiceRoleInstanceWebuisEntity webuis : webuisList) {
            String webuiName = webuis.getName();
            boolean needUpdate = false;

            if (webuiName.contains(ACTIVE) && STANDBY.equals(state)) {
                webuiName = webuiName.replace(ACTIVE, STANDBY);
                needUpdate = true;
            }

            if (webuiName.contains(STANDBY) && ACTIVE.equals(state)) {
                webuiName = webuiName.replace(STANDBY, ACTIVE);
                needUpdate = true;
            }

            webuis.setName(webuiName);

            if (!webuiName.contains(ACTIVE) && !webuiName.contains(STANDBY)) {
                webuis.setName(webuis.getName() + state);
                needUpdate = true;
            }

            if (needUpdate) {
                this.updateById(webuis);
            }
        }
    }
    
    @Override
    public ClusterServiceRoleInstanceWebuisDTO createWebUI(ClusterServiceRoleInstanceWebuisDTO webuisDTO) {
        log.debug("创建WebUI: {}", webuisDTO.name());
        
        // DTO转Entity
        var webuisEntity = webuisConverter.dtoToEntity(webuisDTO);
        
        // 保存到数据库
        save(webuisEntity);
        
        // Entity转DTO返回
        return webuisConverter.entityToDto(webuisEntity);
    }
    
    @Override
    public ClusterServiceRoleInstanceWebuisDTO updateWebUI(ClusterServiceRoleInstanceWebuisDTO webuisDTO) {
        log.debug("更新WebUI: {}", webuisDTO.id());
        
        // 检查WebUI是否存在
        var existingEntity = getById(webuisDTO.id());
        if (existingEntity == null) {
            throw new com.datasophon.common.exception.BusinessException("WebUI不存在: " + webuisDTO.id());
        }
        
        // DTO转Entity
        var webuisEntity = webuisConverter.dtoToEntity(webuisDTO);
        
        // 更新数据库
        updateById(webuisEntity);
        
        // Entity转DTO返回
        return webuisConverter.entityToDto(webuisEntity);
    }
    
    @Override
    public ClusterServiceRoleInstanceWebuisDTO getWebUIById(Long id) {
        log.debug("根据ID获取WebUI: {}", id);
        
        var webuisEntity = getById(id);
        if (webuisEntity == null) {
            throw new com.datasophon.common.exception.BusinessException("WebUI不存在: " + id);
        }
        
        return webuisConverter.entityToDto(webuisEntity);
    }
}
