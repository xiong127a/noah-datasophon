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

import com.datasophon.common.dto.ClusterServiceInstanceConfigDTO;
import com.datasophon.common.dto.ConfigVersionDTO;
import com.datasophon.common.dto.ServiceInstanceConfigResultDTO;
import com.datasophon.common.model.PageResult;
import com.datasophon.dao.entity.ClusterServiceInstanceConfigEntity;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 集群服务实例配置服务接口
 * 继承IService<ClusterServiceInstanceConfigEntity>，提供标准CRUD操作
 * 按照架构重构规范，Service层返回DTO，不返回Result
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-06
 */
public interface ClusterServiceInstanceConfigService extends IService<ClusterServiceInstanceConfigEntity> {

    /**
     * 获取服务实例配置
     * 
     * @param serviceInstanceId 服务实例ID
     * @param version           配置版本
     * @param roleGroupId       角色组ID
     * @param page             页码
     * @param pageSize         每页大小
     * @return 服务实例配置结果DTO
     */
    ServiceInstanceConfigResultDTO getServiceInstanceConfig(
            Long serviceInstanceId, Integer version, Integer roleGroupId,
            Integer page, Integer pageSize);

    /**
     * 根据服务ID获取服务配置
     * 
     * @param serviceId 服务ID
     * @return 服务实例配置DTO
     */
    ClusterServiceInstanceConfigDTO getServiceConfigByServiceId(Long serviceId);

    /**
     * 获取配置版本列表
     * 
     * @param serviceInstanceId 服务实例ID
     * @param roleGroupId       角色组ID
     * @return 配置版本DTO列表
     */
    List<ConfigVersionDTO> getConfigVersion(Long serviceInstanceId, Long roleGroupId);
    
    /**
     * 创建服务实例配置
     * 
     * @param configDTO 配置DTO
     * @return 创建的配置DTO
     */
    ClusterServiceInstanceConfigDTO createServiceInstanceConfig(ClusterServiceInstanceConfigDTO configDTO);
    
    /**
     * 更新服务实例配置
     * 
     * @param configDTO 配置DTO
     * @return 更新的配置DTO
     */
    ClusterServiceInstanceConfigDTO updateServiceInstanceConfig(ClusterServiceInstanceConfigDTO configDTO);
    
    /**
     * 根据ID获取服务实例配置
     * 
     * @param id 配置ID
     * @return 配置DTO
     * @throws com.datasophon.common.exception.BusinessException 配置不存在
     */
    ClusterServiceInstanceConfigDTO getServiceInstanceConfigById(Long id);
    
    /**
     * 分页查询服务实例配置列表
     * 
     * @param clusterId 集群ID
     * @param serviceId 服务ID（可选）
     * @param page      页码
     * @param pageSize  每页大小
     * @return 分页结果
     */
    PageResult<ClusterServiceInstanceConfigDTO> getServiceInstanceConfigListByPage(
            Long clusterId, Long serviceId, Integer page, Integer pageSize);
}
