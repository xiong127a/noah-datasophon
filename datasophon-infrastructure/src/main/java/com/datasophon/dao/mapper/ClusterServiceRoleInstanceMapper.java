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

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.yulichang.base.MPJBaseMapper;

/**
 * 集群服务角色实例表
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-24 16:25:17
 */
@Mapper
public interface ClusterServiceRoleInstanceMapper extends MPJBaseMapper<ClusterServiceRoleInstanceEntity> {

    /**
     * 将指定角色组的所有服务角色实例更新为需要重启状态
     *
     * @param roleGroupId 角色组ID
     */
    default void updateToNeedRestart(@Param("roleGroupId") Integer roleGroupId) {
        LambdaUpdateWrapper<ClusterServiceRoleInstanceEntity> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(ClusterServiceRoleInstanceEntity::getRoleGroupId, roleGroupId)
                .set(ClusterServiceRoleInstanceEntity::getNeedRestart, NeedRestart.YES);

        update(updateWrapper);
    }

    /**
     * 将指定角色组和服务角色名称的服务角色实例更新为需要重启状态
     *
     * @param roleGroupId     角色组ID
     * @param serviceRoleName 服务角色名称
     */
    default void updateToNeedRestartByServiceRoleName(@Param("roleGroupId") Integer roleGroupId,
                                                      @Param("serviceRoleName") String serviceRoleName) {
        LambdaUpdateWrapper<ClusterServiceRoleInstanceEntity> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(ClusterServiceRoleInstanceEntity::getRoleGroupId, roleGroupId)
                .eq(ClusterServiceRoleInstanceEntity::getServiceRoleName, serviceRoleName)
                .set(ClusterServiceRoleInstanceEntity::getNeedRestart, NeedRestart.YES);

        update(updateWrapper);
    }

    /**
     * 将指定主机上的所有服务角色实例更新为需要重启状态
     *
     * @param hostName 主机名
     */
    default void updateToNeedRestartByHost(@Param("hostName") String hostName) {
        LambdaUpdateWrapper<ClusterServiceRoleInstanceEntity> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(ClusterServiceRoleInstanceEntity::getHostname, hostName)
                .set(ClusterServiceRoleInstanceEntity::getNeedRestart, NeedRestart.YES);

        update(updateWrapper);
    }
}
