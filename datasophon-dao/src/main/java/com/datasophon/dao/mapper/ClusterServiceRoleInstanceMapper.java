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
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 集群服务角色实例映射器
 * 只保留业务特定的查询方法，标准CRUD使用IService提供
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Mapper
public interface ClusterServiceRoleInstanceMapper extends BaseMapper<ClusterServiceRoleInstanceEntity> {

    /**
     * 根据集群ID、服务名和服务角色名查询服务角色实例
     *
     * @param clusterId       集群ID
     * @param serviceName     服务名
     * @param serviceRoleName 服务角色名
     * @return 服务角色实例列表
     */
    default List<ClusterServiceRoleInstanceEntity> selectByClusterIdAndServiceNameAndServiceRoleName(
            @Param("clusterId") Integer clusterId,
            @Param("serviceName") String serviceName,
            @Param("serviceRoleName") String serviceRoleName) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterServiceRoleInstanceEntity::getClusterId).eq(clusterId);

        if (serviceName != null && !serviceName.isEmpty()) {
            query.and(ClusterServiceRoleInstanceEntity::getServiceName).eq(serviceName);
        }

        if (serviceRoleName != null && !serviceRoleName.isEmpty()) {
            query.and(ClusterServiceRoleInstanceEntity::getServiceRoleName).eq(serviceRoleName);
        }

        return this.selectListByQuery(query);
    }

    /**
     * 统计指定角色组的服务角色实例数量
     *
     * @param roleGroupId 角色组ID
     * @return 实例数量
     */
    default long countByRoleGroupId(@Param("roleGroupId") Integer roleGroupId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterServiceRoleInstanceEntity::getRoleGroupId).eq(roleGroupId);
        return this.selectCountByQuery(query);
    }

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
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterServiceRoleInstanceEntity::getClusterId).eq(clusterId)
                .and(ClusterServiceRoleInstanceEntity::getHostname).eq(hostname)
                .and(ClusterServiceRoleInstanceEntity::getServiceRoleState).eq(ServiceRoleState.RUNNING)
                .and(ClusterServiceRoleInstanceEntity::getRoleType).ne(RoleType.CLIENT);
        return this.selectListByQuery(query);
    }

    /**
     * 根据集群ID和主机名查询已停止的服务角色实例
     *
     * @param clusterId 集群ID
     * @param hostname  主机名
     * @return 已停止的服务角色实例列表
     */
    default List<ClusterServiceRoleInstanceEntity> selectStoppedServiceRolesByClusterIdAndHostname(
            @Param("clusterId") Integer clusterId,
            @Param("hostname") String hostname) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterServiceRoleInstanceEntity::getClusterId).eq(clusterId)
                .and(ClusterServiceRoleInstanceEntity::getHostname).eq(hostname)
                .and(ClusterServiceRoleInstanceEntity::getServiceRoleState).eq(ServiceRoleState.STOP);
        return this.selectListByQuery(query);
    }

    /**
     * 根据集群ID和主机名查询所有服务角色实例
     *
     * @param clusterId 集群ID
     * @param hostname  主机名
     * @return 服务角色实例列表
     */
    default List<ClusterServiceRoleInstanceEntity> selectByClusterIdAndHostname(
            @Param("clusterId") Integer clusterId,
            @Param("hostname") String hostname) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterServiceRoleInstanceEntity::getClusterId).eq(clusterId)
                .and(ClusterServiceRoleInstanceEntity::getHostname).eq(hostname);
        return this.selectListByQuery(query);
    }

    /**
     * 根据服务ID和服务角色状态查询服务角色实例
     *
     * @param serviceId 服务ID
     * @param roleState 服务角色状态
     * @return 服务角色实例列表
     */
    default List<ClusterServiceRoleInstanceEntity> selectByServiceIdAndRoleState(
            @Param("serviceId") Integer serviceId,
            @Param("roleState") ServiceRoleState roleState) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterServiceRoleInstanceEntity::getServiceId).eq(serviceId)
                .and(ClusterServiceRoleInstanceEntity::getServiceRoleState).eq(roleState);
        return this.selectListByQuery(query);
    }

    /**
     * 根据服务角色名称、集群ID和主机名查询服务角色实例（主机名可选）
     *
     * @param serviceRoleName 服务角色名称
     * @param clusterId       集群ID
     * @param hostname        主机名（可为null）
     * @return 服务角色实例
     */
    default ClusterServiceRoleInstanceEntity selectByServiceRoleNameAndClusterIdAndHostname(
            @Param("serviceRoleName") String serviceRoleName,
            @Param("clusterId") Integer clusterId,
            @Param("hostname") String hostname) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterServiceRoleInstanceEntity::getServiceRoleName).eq(serviceRoleName)
                .and(ClusterServiceRoleInstanceEntity::getClusterId).eq(clusterId);

        if (hostname != null && !hostname.trim().isEmpty()) {
            query.and(ClusterServiceRoleInstanceEntity::getHostname).eq(hostname);
        }

        return this.selectOneByQuery(query);
    }

    /**
     * 根据服务ID和需要重启状态查询
     */
    default List<ClusterServiceRoleInstanceEntity> selectByServiceIdAndNeedRestart(
            @Param("serviceId") Integer serviceId, @Param("needRestart") NeedRestart needRestart) {
        return QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getServiceId).eq(serviceId)
                .and(ClusterServiceRoleInstanceEntity::getNeedRestart).eq(needRestart)
                .list();
    }

    /**
     * 根据集群ID、主机名和状态查询
     */
    default List<ClusterServiceRoleInstanceEntity> selectByClusterIdAndHostnameAndState(
            @Param("clusterId") Integer clusterId, @Param("hostname") String hostname,
            @Param("state") ServiceRoleState state) {
        return QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getClusterId).eq(clusterId)
                .and(ClusterServiceRoleInstanceEntity::getHostname).eq(hostname)
                .and(ClusterServiceRoleInstanceEntity::getServiceRoleState).eq(state)
                .list();
    }

    /**
     * 根据集群ID和服务角色名称查询
     */
    default ClusterServiceRoleInstanceEntity selectByClusterIdAndServiceRoleName(@Param("clusterId") Integer clusterId,
            @Param("serviceRoleName") String serviceRoleName) {
        return QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getClusterId).eq(clusterId)
                .and(ClusterServiceRoleInstanceEntity::getServiceRoleName).eq(serviceRoleName)
                .one();
    }

    /**
     * 根据服务角色名称查询
     */
    default List<ClusterServiceRoleInstanceEntity> selectByServiceRoleName(
            @Param("serviceRoleName") String serviceRoleName) {
        return QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getServiceRoleName).eq(serviceRoleName)
                .list();
    }

    /**
     * 根据主机名和服务角色名称查询
     */
    default ClusterServiceRoleInstanceEntity selectByHostnameAndServiceRoleName(@Param("hostname") String hostname,
            @Param("serviceRoleName") String serviceRoleName) {
        return QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getHostname).eq(hostname)
                .and(ClusterServiceRoleInstanceEntity::getServiceRoleName).eq(serviceRoleName)
                .one();
    }

    /**
     * 根据主机名和服务名称查询
     */
    default List<ClusterServiceRoleInstanceEntity> selectByHostnameAndServiceName(@Param("hostname") String hostname,
            @Param("serviceName") String serviceName) {
        return QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getHostname).eq(hostname)
                .and(ClusterServiceRoleInstanceEntity::getServiceName).eq(serviceName)
                .list();
    }

    /**
     * 根据集群ID、服务ID和角色名称查询
     */
    default List<ClusterServiceRoleInstanceEntity> selectByClusterIdAndServiceIdAndRoleName(
            @Param("clusterId") Integer clusterId, @Param("serviceId") Integer serviceId,
            @Param("roleName") String roleName) {
        QueryChain<ClusterServiceRoleInstanceEntity> query = QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getClusterId).eq(clusterId)
                .and(ClusterServiceRoleInstanceEntity::getServiceRoleName).eq(roleName);

        if (serviceId != null) {
            query.and(ClusterServiceRoleInstanceEntity::getServiceId).eq(serviceId);
        }

        return query.list();
    }
}
