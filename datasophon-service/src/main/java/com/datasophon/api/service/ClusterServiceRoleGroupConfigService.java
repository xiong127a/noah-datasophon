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

import com.datasophon.common.dto.ClusterServiceRoleGroupConfigDTO;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfigEntity;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 集群服务角色组配置服务接口
 * 提供集群服务角色组配置的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
public interface ClusterServiceRoleGroupConfigService extends IService<ClusterServiceRoleGroupConfigEntity> {

    /**
     * 根据角色组ID获取配置（最新版本）
     *
     * @param roleGroupId 角色组ID
     * @return 配置DTO
     */
    ClusterServiceRoleGroupConfigDTO getConfigByRoleGroupId(Long roleGroupId);

    /**
     * 根据角色组ID和版本号获取配置
     *
     * @param roleGroupId 角色组ID
     * @param version     版本号
     * @return 配置DTO
     */
    ClusterServiceRoleGroupConfigDTO getConfigByRoleGroupIdAndVersion(Integer roleGroupId, Integer version);

    /**
     * 删除指定角色组的所有配置
     *
     * @param roleGroupId 角色组ID
     */
    void removeAllByRoleGroupId(Integer roleGroupId);

    /**
     * 根据角色组ID列表获取配置列表
     *
     * @param roleGroupIds 角色组ID列表
     * @return 配置DTO列表
     */
    List<ClusterServiceRoleGroupConfigDTO> listRoleGroupConfigsByRoleGroupIds(List<Integer> roleGroupIds);

    /**
     * 根据ID获取配置DTO
     *
     * @param id 配置ID
     * @return 配置DTO
     */
    ClusterServiceRoleGroupConfigDTO getByIdAsDto(Long id);

    /**
     * 保存配置
     *
     * @param dto 配置DTO
     */
    void saveConfig(ClusterServiceRoleGroupConfigDTO dto);

    /**
     * 更新配置
     *
     * @param dto 配置DTO
     */
    void updateConfig(ClusterServiceRoleGroupConfigDTO dto);

    /**
     * 根据角色组ID获取配置版本列表（按版本号降序）
     *
     * @param roleGroupId 角色组ID
     * @return 配置列表
     */
    List<ClusterServiceRoleGroupConfigEntity> getConfigVersionsByRoleGroupId(Long roleGroupId);

    /**
     * 根据角色组ID获取最新的两个配置版本（用于版本比较）
     *
     * @param roleGroupId 角色组ID
     * @return 最新的两个配置版本列表
     */
    List<ClusterServiceRoleGroupConfigEntity> getLatestTwoConfigsByRoleGroupId(Long roleGroupId);
}
