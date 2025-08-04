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

import com.datasophon.common.dto.ClusterServiceDashboardDTO;
import com.datasophon.dao.entity.ClusterServiceDashboard;
import com.mybatisflex.core.service.IService;

/**
 * 集群服务仪表盘服务接口
 * 提供集群服务仪表盘的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
public interface ClusterServiceDashboardService extends IService<ClusterServiceDashboard> {

    /**
     * 获取仪表盘URL
     */
    String getDashboardUrl(Integer clusterId);

    /**
     * 获取Datasophon仪表盘URL
     */
    String getDatasophonDashboard(Integer clusterId);

    /**
     * 根据ID获取仪表盘DTO
     */
    ClusterServiceDashboardDTO getByIdAsDto(Integer id);

    /**
     * 保存仪表盘DTO
     */
    ClusterServiceDashboardDTO saveDashboard(ClusterServiceDashboardDTO dto);

    /**
     * 更新仪表盘
     */
    void updateDashboard(ClusterServiceDashboardDTO dto);

    /**
     * 根据服务名称获取仪表盘
     * 
     * @param serviceName 服务名称
     * @return 仪表盘实体
     */
    ClusterServiceDashboard getByServiceName(String serviceName);
}
