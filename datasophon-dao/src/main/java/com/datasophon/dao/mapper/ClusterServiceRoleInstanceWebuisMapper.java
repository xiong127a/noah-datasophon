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

import com.datasophon.dao.entity.ClusterServiceRoleInstanceWebuisEntity;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.List;

import static com.datasophon.dao.entity.table.ClusterServiceRoleInstanceWebuisEntityTableDef.CLUSTER_SERVICE_ROLE_INSTANCE_WEBUIS_ENTITY;

/**
 * 集群服务角色对应web ui表
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Mapper
public interface ClusterServiceRoleInstanceWebuisMapper extends BaseMapper<ClusterServiceRoleInstanceWebuisEntity> {

    /**
     * 根据服务实例ID查询WebUI列表
     */
    default List<ClusterServiceRoleInstanceWebuisEntity> selectByServiceInstanceId(Long serviceInstanceId) {
        return selectListByQuery(QueryWrapper.create()
                .where(CLUSTER_SERVICE_ROLE_INSTANCE_WEBUIS_ENTITY.SERVICE_INSTANCE_ID.eq(serviceInstanceId)));
    }

    /**
     * 根据服务实例ID删除WebUI记录
     */
    default int deleteByServiceInstanceId(Long serviceInstanceId) {
        return deleteByQuery(QueryWrapper.create()
                .where(CLUSTER_SERVICE_ROLE_INSTANCE_WEBUIS_ENTITY.SERVICE_INSTANCE_ID.eq(serviceInstanceId)));
    }

    /**
     * 根据角色实例ID查询单个WebUI
     */
    default ClusterServiceRoleInstanceWebuisEntity selectByServiceRoleInstanceId(Long roleInstanceId) {
        return selectOneByQuery(QueryWrapper.create()
                .where(CLUSTER_SERVICE_ROLE_INSTANCE_WEBUIS_ENTITY.SERVICE_ROLE_INSTANCE_ID.eq(roleInstanceId)));
    }

    /**
     * 根据角色实例ID列表删除WebUI记录
     */
    default int deleteByServiceRoleInstanceIds(ArrayList<Long> roleInstanceIds) {
        if (roleInstanceIds == null || roleInstanceIds.isEmpty()) {
            return 0;
        }
        return deleteByQuery(QueryWrapper.create()
                .where(CLUSTER_SERVICE_ROLE_INSTANCE_WEBUIS_ENTITY.SERVICE_ROLE_INSTANCE_ID.in(roleInstanceIds)));
    }

    /**
     * 根据角色实例ID查询WebUI列表（用于状态更新）
     */
    default List<ClusterServiceRoleInstanceWebuisEntity> selectListByServiceRoleInstanceId(Long roleInstanceId) {
        return selectListByQuery(QueryWrapper.create()
                .where(CLUSTER_SERVICE_ROLE_INSTANCE_WEBUIS_ENTITY.SERVICE_ROLE_INSTANCE_ID.eq(roleInstanceId)));
    }
}
