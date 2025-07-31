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

import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.enums.NeedRestart;
import com.datasophon.dao.enums.RoleType;
import com.datasophon.dao.enums.ServiceRoleState;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.core.update.UpdateChain;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 集群服务角色实例表
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-24 16:25:17
 */
@Mapper
public interface ClusterServiceRoleInstanceMapper extends BaseMapper<ClusterServiceRoleInstanceEntity> {

    /**
     * 将指定角色组的所有服务角色实例更新为需要重启状态
     *
     * @param roleGroupId 角色组ID
     */
    default void updateToNeedRestart(@Param("roleGroupId") Integer roleGroupId) {
        UpdateChain.of(ClusterServiceRoleInstanceEntity.class)
                .set(ClusterServiceRoleInstanceEntity::getNeedRestart, NeedRestart.YES)
                .where(ClusterServiceRoleInstanceEntity::getRoleGroupId).eq(roleGroupId)
                .update();
    }

    /**
     * 将指定角色组和服务角色名称的服务角色实例更新为需要重启状态
     *
     * @param roleGroupId     角色组ID
     * @param serviceRoleName 服务角色名称
     */
    default void updateToNeedRestartByServiceRoleName(@Param("roleGroupId") Integer roleGroupId,
            @Param("serviceRoleName") String serviceRoleName) {
        UpdateChain.of(ClusterServiceRoleInstanceEntity.class)
                .set(ClusterServiceRoleInstanceEntity::getNeedRestart, NeedRestart.YES)
                .where(ClusterServiceRoleInstanceEntity::getRoleGroupId).eq(roleGroupId)
                .and(ClusterServiceRoleInstanceEntity::getServiceRoleName).eq(serviceRoleName)
                .update();
    }

    /**
     * 将指定主机上的所有服务角色实例更新为需要重启状态
     *
     * @param hostName 主机名
     */
    default void updateToNeedRestartByHost(@Param("hostName") String hostName) {
        UpdateChain.of(ClusterServiceRoleInstanceEntity.class)
                .set(ClusterServiceRoleInstanceEntity::getNeedRestart, NeedRestart.YES)
                .where(ClusterServiceRoleInstanceEntity::getHostname).eq(hostName)
                .update();
    }

    /**
     * 查询指定集群和主机上正在运行的非客户端服务角色实例
     *
     * @param clusterId 集群ID
     * @param hostname  主机名
     * @return 正在运行的服务角色实例列表
     */
    default List<ClusterServiceRoleInstanceEntity> selectRunningNonClientRolesByClusterIdAndHostname(
            @Param("clusterId") Integer clusterId,
            @Param("hostname") String hostname) {
        return QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getClusterId).eq(clusterId)
                .and(ClusterServiceRoleInstanceEntity::getHostname).eq(hostname)
                .and(ClusterServiceRoleInstanceEntity::getServiceRoleState).eq(ServiceRoleState.RUNNING)
                .and(ClusterServiceRoleInstanceEntity::getRoleType).ne(RoleType.CLIENT)
                .list();
    }
}
