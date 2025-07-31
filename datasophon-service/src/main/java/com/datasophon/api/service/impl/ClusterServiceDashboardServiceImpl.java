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
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceDashboardService;
import com.datasophon.common.Constants;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.api.vo.Result;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceDashboard;
import com.datasophon.dao.mapper.ClusterServiceDashboardMapper;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("clusterServiceDashboardService")
public class ClusterServiceDashboardServiceImpl
                extends
                ServiceImpl<ClusterServiceDashboardMapper, ClusterServiceDashboard>
                implements
                ClusterServiceDashboardService {


        @Override
        public Result getDashboardUrl(Integer clusterId) {
                ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
                ClusterInfoEntity clusterInfoEntity = clusterInfoService.getById(clusterId);
                String depType = clusterInfoEntity.getDepType();
                String serviceName = "TOTAL";
                if (StrUtil.equals(depType, Constants.KUBERNETES_MODE)) {
                        serviceName = "KUBERNETES";
                }
                Map<String, String> globalVariables = GlobalVariables.get(clusterId);
                ClusterServiceDashboard dashboard = QueryChain.of(ClusterServiceDashboard.class)
                                .where(ClusterServiceDashboard::getServiceName).eq(serviceName)
                                .one();

                String dashboardUrl = PlaceholderUtils.replacePlaceholders(dashboard.getDashboardUrl(), globalVariables,
                                Constants.REGEX_VARIABLE);
                return Result.success(dashboardUrl);
        }

        @Override
        public Result getDatasophonDashboard(Integer clusterId) {
                Map<String, String> globalVariables = GlobalVariables.get(clusterId);
                ClusterServiceDashboard dashboard = QueryChain.of(ClusterServiceDashboard.class)
                                .where(ClusterServiceDashboard::getServiceName).eq("DATASOPHON")
                                .one();

                String dashboardUrl = PlaceholderUtils.replacePlaceholders(dashboard.getDashboardUrl(), globalVariables,
                                Constants.REGEX_VARIABLE);
                return Result.success(dashboardUrl);
        }
}
