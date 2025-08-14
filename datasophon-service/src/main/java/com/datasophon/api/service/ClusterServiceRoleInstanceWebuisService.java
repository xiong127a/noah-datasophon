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

import com.datasophon.common.dto.ClusterServiceRoleInstanceWebuisDTO;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceWebuisEntity;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 集群服务角色实例WebUI服务接口
 * 继承IService<ClusterServiceRoleInstanceWebuisEntity>，提供标准CRUD操作
 * 按照架构重构规范，Service层返回DTO，不返回Result
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-06
 */
public interface ClusterServiceRoleInstanceWebuisService extends IService<ClusterServiceRoleInstanceWebuisEntity> {

    /**
     * 根据服务实例ID获取WebUI列表
     * 
     * @param serviceInstanceId 服务实例ID
     * @return WebUI DTO列表
     */
    List<ClusterServiceRoleInstanceWebuisDTO> getWebUis(Long serviceInstanceId);

    /**
     * 根据服务实例ID删除WebUI
     * 
     * @param serviceInstanceId 服务实例ID
     */
    void removeByServiceInsId(Long serviceInstanceId);

    /**
     * 更新WebUI状态为活跃
     * 
     * @param roleInstanceId 角色实例ID
     */
    void updateWebUiToActive(Long roleInstanceId);

    /**
     * 根据角色实例ID获取WebUI
     * 
     * @param roleInstanceId 角色实例ID
     * @return WebUI DTO
     */
    ClusterServiceRoleInstanceWebuisDTO getRoleInstanceWebUi(Long roleInstanceId);

    /**
     * 批量删除角色实例WebUI
     * 
     * @param roleInstanceIds 角色实例ID列表
     */
    void removeByRoleInsIds(List<Long> roleInstanceIds);

    /**
     * 更新WebUI状态为待机
     * 
     * @param roleInstanceId 角色实例ID
     */
    void updateWebUiToStandby(Long roleInstanceId);

    /**
     * 根据服务实例ID获取WebUI列表（别名方法）
     * 
     * @param serviceInstanceId 服务实例ID
     * @return WebUI DTO列表
     */
    List<ClusterServiceRoleInstanceWebuisDTO> listWebUisByServiceInstanceId(Long serviceInstanceId);
    
    /**
     * 创建WebUI
     * 
     * @param webuisDTO WebUI DTO
     * @return 创建的WebUI DTO
     */
    ClusterServiceRoleInstanceWebuisDTO createWebUI(ClusterServiceRoleInstanceWebuisDTO webuisDTO);
    
    /**
     * 更新WebUI
     * 
     * @param webuisDTO WebUI DTO
     * @return 更新的WebUI DTO
     */
    ClusterServiceRoleInstanceWebuisDTO updateWebUI(ClusterServiceRoleInstanceWebuisDTO webuisDTO);
    
    /**
     * 根据ID获取WebUI
     * 
     * @param id WebUI ID
     * @return WebUI DTO
     * @throws com.datasophon.common.exception.BusinessException WebUI不存在
     */
    ClusterServiceRoleInstanceWebuisDTO getWebUIById(Long id);
}
