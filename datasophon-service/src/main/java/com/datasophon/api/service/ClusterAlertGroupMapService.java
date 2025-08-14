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

import com.datasophon.common.dto.ClusterAlertGroupMapDTO;
import com.datasophon.dao.entity.ClusterAlertGroupMapEntity;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 集群告警组映射服务接口
 * 提供集群告警组映射的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
public interface ClusterAlertGroupMapService extends IService<ClusterAlertGroupMapEntity> {

    /**
     * 根据集群ID获取告警组映射列表
     *
     * @param clusterId 集群ID
     * @return 告警组映射DTO列表
     */
    List<ClusterAlertGroupMapDTO> getByClusterId(Long clusterId);

    /**
     * 根据ID获取告警组映射DTO
     *
     * @param id 告警组映射ID
     * @return 告警组映射DTO
     */
    ClusterAlertGroupMapDTO getByIdAsDto(Long id);

    /**
     * 保存告警组映射
     *
     * @param dto 告警组映射DTO
     */
    void saveAlertGroupMap(ClusterAlertGroupMapDTO dto);

    /**
     * 更新告警组映射
     *
     * @param dto 告警组映射DTO
     */
    void updateAlertGroupMap(ClusterAlertGroupMapDTO dto);
}