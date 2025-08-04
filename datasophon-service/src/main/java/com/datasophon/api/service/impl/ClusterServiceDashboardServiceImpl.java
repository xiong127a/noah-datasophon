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

import cn.hutool.core.util.StrUtil;
import com.datasophon.api.converter.ClusterServiceDashboardConverter;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceDashboardService;
import com.datasophon.common.Constants;
import com.datasophon.common.dto.ClusterServiceDashboardDTO;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceDashboard;
import com.datasophon.dao.mapper.ClusterServiceDashboardMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 集群服务仪表盘服务实现
 * 按照架构重构规范，迁移QueryChain到DAO层
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Service("clusterServiceDashboardService")
public class ClusterServiceDashboardServiceImpl
                extends ServiceImpl<ClusterServiceDashboardMapper, ClusterServiceDashboard>
                implements ClusterServiceDashboardService {

        private static final Logger logger = LoggerFactory.getLogger(ClusterServiceDashboardServiceImpl.class);

        @Autowired
        private ClusterServiceDashboardConverter converter;

        @Autowired
        private ClusterInfoService clusterInfoService;

        @Override
        public String getDashboardUrl(Integer clusterId) {
                ClusterInfoEntity clusterInfoEntity = clusterInfoService.getById(clusterId);
                String depType = clusterInfoEntity.getDepType();
                String serviceName = "TOTAL";
                if (StrUtil.equals(depType, Constants.KUBERNETES_MODE)) {
                        serviceName = "KUBERNETES";
                }
                Map<String, String> globalVariables = GlobalVariables.get(clusterId);
                ClusterServiceDashboard dashboard = getMapper().selectByServiceName(serviceName);

                String dashboardUrl = PlaceholderUtils.replacePlaceholders(dashboard.getDashboardUrl(), globalVariables,
                                Constants.REGEX_VARIABLE);
                return dashboardUrl;
        }

        @Override
        public String getDatasophonDashboard(Integer clusterId) {
                Map<String, String> globalVariables = GlobalVariables.get(clusterId);
                ClusterServiceDashboard dashboard = getMapper().selectByServiceName("DATASOPHON");

                String dashboardUrl = PlaceholderUtils.replacePlaceholders(dashboard.getDashboardUrl(), globalVariables,
                                Constants.REGEX_VARIABLE);
                return dashboardUrl;
        }

        // DTO相关的CRUD方法实现
        @Override
        public ClusterServiceDashboardDTO getByIdAsDto(Integer id) {
                ClusterServiceDashboard entity = getById(id);
                return entity != null ? converter.entityToDto(entity) : null;
        }

        @Override
        public ClusterServiceDashboardDTO saveDashboard(ClusterServiceDashboardDTO dto) {
                ClusterServiceDashboard entity = converter.dtoToEntity(dto);
                save(entity);
                return converter.entityToDto(entity);
        }

        @Override
        public void updateDashboard(ClusterServiceDashboardDTO dto) {
                ClusterServiceDashboard entity = converter.dtoToEntity(dto);
                updateById(entity);
        }
}