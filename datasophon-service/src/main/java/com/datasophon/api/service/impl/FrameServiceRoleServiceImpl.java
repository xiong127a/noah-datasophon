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

import cn.hutool.core.collection.CollUtil;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.FrameServiceRoleService;
import com.datasophon.api.service.FrameServiceService;
import com.datasophon.api.utils.CacheOperateUtils;
import com.datasophon.common.Constants;
import com.datasophon.api.vo.Result;
import com.datasophon.common.enums.TypeRefs;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.entity.FrameServiceEntity;
import com.datasophon.dao.entity.FrameServiceRoleEntity;
import com.datasophon.dao.enums.RoleType;
import com.datasophon.dao.mapper.FrameServiceRoleMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service("frameServiceRoleService")
public class FrameServiceRoleServiceImpl extends ServiceImpl<FrameServiceRoleMapper, FrameServiceRoleEntity>
        implements
        FrameServiceRoleService {

    // 定义常量
    private static final String SERVICE_NODE = "NODE";
    private static final String ROLE_NODE = "node";
    private static final String SERVICE_ROLE_CACHE_KEY_FORMAT = "%d_%s";

    @Autowired
    private ClusterInfoService clusterInfoService;

    @Autowired
    private FrameServiceService frameService;


    @Override
    public Result getServiceRoleList(Integer clusterId, String serviceIds, Integer serviceRoleType) {
        // 分割服务ID字符串为列表
        List<String> ids = Arrays.asList(serviceIds.split(","));

        // 构建查询条件
        QueryChain<FrameServiceRoleEntity> query = QueryChain.of(FrameServiceRoleEntity.class)
                .where(FrameServiceRoleEntity::getServiceId).in(ids);

        // 如果指定了角色类型，添加角色类型筛选条件
        if (Objects.nonNull(serviceRoleType)) {
            query.and(FrameServiceRoleEntity::getServiceRoleType).eq(serviceRoleType);
        }

        // 执行查询
        List<FrameServiceRoleEntity> roles = query.list();

        // 生成缓存键
        String cacheKey = String.format(SERVICE_ROLE_CACHE_KEY_FORMAT, clusterId, Constants.SERVICE_ROLE_HOST_MAPPING);

        // 为每个角色查询主机信息
        for (FrameServiceRoleEntity role : roles) {
            FrameServiceEntity service = frameService.getById(role.getServiceId());

            // 查询已安装的角色实例
            List<ClusterServiceRoleInstanceEntity> roleInstances = QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                    .where(ClusterServiceRoleInstanceEntity::getServiceName).eq(service.getServiceName())
                    .and(ClusterServiceRoleInstanceEntity::getServiceRoleName).eq(role.getServiceRoleName())
                    .and(ClusterServiceRoleInstanceEntity::getClusterId).eq(clusterId)
                    .list();

            // 如果有角色实例，从实例中获取主机列表
            if (CollUtil.isNotEmpty(roleInstances)) {
                List<String> hosts = roleInstances.stream()
                        .map(ClusterServiceRoleInstanceEntity::getHostname)
                        .collect(Collectors.toList());
                role.setHosts(hosts);
            }
            // 否则，尝试从缓存中获取
            else if (CacheOperateUtils.containsKey(cacheKey)) {
                Map<String, List<String>> roleToHostsMap = CacheOperateUtils.getGeneric(
                        cacheKey,
                        TypeRefs.MAP_STRING_LIST_STRING);

                if (roleToHostsMap.containsKey(role.getServiceRoleName())) {
                    role.setHosts(roleToHostsMap.get(role.getServiceRoleName()));
                }
            }
        }

        return Result.success(roles);
    }

    @Override
    public FrameServiceRoleEntity getServiceRoleByServiceIdAndServiceRoleName(Integer serviceId, String roleName) {
        return QueryChain.of(FrameServiceRoleEntity.class)
                .where(FrameServiceRoleEntity::getServiceId).eq(serviceId)
                .and(FrameServiceRoleEntity::getServiceRoleName).eq(roleName)
                .one();
    }

    @Override
    public FrameServiceRoleEntity getServiceRoleByFrameCodeAndServiceRoleName(String clusterFrame,
            String serviceRoleName) {
        return QueryChain.of(FrameServiceRoleEntity.class)
                .where(FrameServiceRoleEntity::getFrameCode).eq(clusterFrame)
                .and(FrameServiceRoleEntity::getServiceRoleName).eq(serviceRoleName)
                .one();
    }

    @Override
    public Result getNonMasterRoleList(Integer clusterId, String serviceIds) {
        // 分割服务ID字符串为列表
        List<String> ids = Arrays.asList(serviceIds.split(","));

        // 查询非MASTER角色
        List<FrameServiceRoleEntity> roles = QueryChain.of(FrameServiceRoleEntity.class)
                .where(FrameServiceRoleEntity::getServiceRoleType).ne(RoleType.MASTER)
                .and(FrameServiceRoleEntity::getServiceId).in(ids)
                .list();

        // 获取集群信息
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);

        // 生成缓存键
        String cacheKey = String.format(SERVICE_ROLE_CACHE_KEY_FORMAT, clusterInfo.getId(),
                Constants.SERVICE_ROLE_HOST_MAPPING);

        // 为每个角色查询主机信息
        for (FrameServiceRoleEntity role : roles) {
            List<String> hosts = new ArrayList<>();
            FrameServiceEntity service = frameService.getById(role.getServiceId());

            // 查询已安装的角色实例
            List<ClusterServiceRoleInstanceEntity> roleInstances = QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                    .where(ClusterServiceRoleInstanceEntity::getServiceName).eq(service.getServiceName())
                    .and(ClusterServiceRoleInstanceEntity::getServiceRoleName).eq(role.getServiceRoleName())
                    .and(ClusterServiceRoleInstanceEntity::getClusterId).eq(clusterId)
                    .list();

            // 如果有角色实例，从实例中获取主机列表
            if (CollUtil.isNotEmpty(roleInstances)) {
                hosts = roleInstances.stream()
                        .map(ClusterServiceRoleInstanceEntity::getHostname)
                        .collect(Collectors.toList());
            }
            // 否则，尝试从缓存中获取
            else if (CacheOperateUtils.containsKey(cacheKey)) {
                Map<String, List<String>> roleToHostsMap = CacheOperateUtils.getGeneric(
                        cacheKey,
                        TypeRefs.MAP_STRING_LIST_STRING);

                if (roleToHostsMap.containsKey(role.getServiceRoleName())) {
                    hosts = roleToHostsMap.get(role.getServiceRoleName());
                }
            }

            role.setHosts(hosts);
        }

        return Result.success(roles);
    }

    @Override
    public Result getServiceRoleByServiceName(Integer clusterId, String serviceName) {
        // 特殊处理NODE服务
        if (SERVICE_NODE.equals(serviceName)) {
            List<FrameServiceRoleEntity> nodeRoles = new ArrayList<>();
            FrameServiceRoleEntity nodeRole = new FrameServiceRoleEntity();
            nodeRole.setServiceRoleName(ROLE_NODE);
            nodeRoles.add(nodeRole);
            return Result.success(nodeRoles);
        }

        // 获取集群信息
        ClusterInfoEntity cluster = clusterInfoService.getById(clusterId);

        // 获取服务信息
        FrameServiceEntity service = frameService.getServiceByFrameCodeAndServiceName(
                cluster.getClusterFrame(),
                serviceName);

        // 查询服务角色列表
        List<FrameServiceRoleEntity> roles = QueryChain.of(FrameServiceRoleEntity.class)
                .where(FrameServiceRoleEntity::getServiceId).eq(service.getId())
                .list();

        return Result.success(roles);
    }

    @Override
    public List<FrameServiceRoleEntity> getAllServiceRoleList(Integer frameServiceId) {
        return QueryChain.of(FrameServiceRoleEntity.class)
                .where(FrameServiceRoleEntity::getServiceId).eq(frameServiceId)
                .list();
    }
}
