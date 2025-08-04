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

package com.datasophon.dao.mapper;

import com.datasophon.dao.entity.ClusterServiceDashboard;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import static com.datasophon.dao.entity.table.ClusterServiceDashboardTableDef.CLUSTER_SERVICE_DASHBOARD;

/**
 * 集群服务仪表盘数据访问对象
 * 提供集群服务仪表盘的数据库操作
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Mapper
public interface ClusterServiceDashboardMapper extends BaseMapper<ClusterServiceDashboard> {

    /**
     * 根据服务名称查询仪表盘配置
     *
     * @param serviceName 服务名称
     * @return 仪表盘配置
     */
    default ClusterServiceDashboard selectByServiceName(String serviceName) {
        return selectOneByQuery(QueryWrapper.create()
            .where(CLUSTER_SERVICE_DASHBOARD.SERVICE_NAME.eq(serviceName)));
    }
}
