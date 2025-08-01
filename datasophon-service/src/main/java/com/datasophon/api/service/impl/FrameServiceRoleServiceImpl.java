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
import com.datasophon.api.service.FrameServiceRoleService;
import com.datasophon.api.utils.CacheOperateUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.enums.TypeRefs;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.entity.FrameServiceRoleEntity;
import com.datasophon.dao.mapper.ClusterServiceRoleInstanceMapper;
import com.datasophon.dao.mapper.FrameServiceRoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service("frameServiceRoleService")
public class FrameServiceRoleServiceImpl implements FrameServiceRoleService {

    // 定义常量
    private static final String SERVICE_NODE = "NODE";
    private static final String ROLE_NODE = "node";
    private static final String SERVICE_ROLE_CACHE_KEY_FORMAT = "%d_%s";

    @Autowired
    private FrameServiceRoleMapper frameServiceRoleMapper;

    @Autowired
    private ClusterServiceRoleInstanceMapper clusterServiceRoleInstanceMapper;

    @Override
    public List<FrameServiceRoleEntity> getServiceRoleList(Integer clusterId, String serviceIds,
            Integer serviceRoleType) {
        // 分割服务ID字符串为列表
        List<String> ids = Arrays.asList(serviceIds.split(","));

        // 调用Dao层方法查询服务角色
        List<FrameServiceRoleEntity> roles = frameServiceRoleMapper.selectByServiceIdsAndRoleType(ids, serviceRoleType);

        // 生成缓存键
        String cacheKey = String.format(SERVICE_ROLE_CACHE_KEY_FORMAT, clusterId, Constants.SERVICE_ROLE_HOST_MAPPING);

        // 为每个角色查询主机信息
        for (FrameServiceRoleEntity role : roles) {
            // 暂时跳过服务查询，因为需要重新设计这部分逻辑
            // FrameServiceEntity service = frameService.getById(role.getServiceId());

            // 查询已安装的角色实例 (这里需要通过专门的方法查询)
            List<ClusterServiceRoleInstanceEntity> roleInstances = getClusterServiceRoleInstances(
                    clusterId, "", role.getServiceRoleName());

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

        return roles;
    }

    /**
     * 辅助方法：获取集群服务角色实例
     */
    private List<ClusterServiceRoleInstanceEntity> getClusterServiceRoleInstances(Integer clusterId,
            String serviceName, String serviceRoleName) {
        return clusterServiceRoleInstanceMapper.selectByClusterIdAndServiceNameAndServiceRoleName(
                clusterId, serviceName, serviceRoleName);
    }

    @Override
    public FrameServiceRoleEntity getServiceRoleByServiceIdAndServiceRoleName(Integer serviceId, String roleName) {
        return frameServiceRoleMapper.selectByServiceIdAndRoleName(serviceId, roleName);
    }

    @Override
    public FrameServiceRoleEntity getServiceRoleByFrameCodeAndServiceRoleName(String clusterFrame,
            String serviceRoleName) {
        return frameServiceRoleMapper.selectByFrameCodeAndRoleName(clusterFrame, serviceRoleName);
    }

    @Override
    public List<FrameServiceRoleEntity> getNonMasterRoleList(Integer clusterId, String serviceIds) {
        // 分割服务ID字符串为列表
        List<String> ids = Arrays.asList(serviceIds.split(","));

        // 调用Dao层方法查询非MASTER角色
        List<FrameServiceRoleEntity> roles = frameServiceRoleMapper.selectNonMasterRoles(ids);

        // 暂时使用clusterId作为缓存键的一部分，简化集群信息获取逻辑
        // 生成缓存键
        String cacheKey = String.format(SERVICE_ROLE_CACHE_KEY_FORMAT, clusterId,
                Constants.SERVICE_ROLE_HOST_MAPPING);

        // 为每个角色查询主机信息
        for (FrameServiceRoleEntity role : roles) {
            List<String> hosts = new ArrayList<>();
            // 暂时跳过服务查询，因为需要重新设计这部分逻辑
            // FrameServiceEntity service = frameService.getById(role.getServiceId());

            // 查询已安装的角色实例
            List<ClusterServiceRoleInstanceEntity> roleInstances = getClusterServiceRoleInstances(
                    clusterId, "", role.getServiceRoleName());

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

        return roles;
    }

    @Override
    public List<FrameServiceRoleEntity> getServiceRoleByServiceName(Integer clusterId, String serviceName) {
        // 特殊处理NODE服务
        if (SERVICE_NODE.equals(serviceName)) {
            FrameServiceRoleEntity nodeRole = new FrameServiceRoleEntity();
            nodeRole.setServiceRoleName(ROLE_NODE);
            return Collections.singletonList(nodeRole);
        }

        // 获取集群信息 - 暂时返回空列表，因为需要重新设计集群信息获取逻辑
        // Result clusterResult = clusterInfoService.getClusterById(clusterId);

        // 由于集群信息获取逻辑需要重新设计，暂时返回空列表
        // 后续需要根据实际业务需求调整
        return new ArrayList<>();
    }

    @Override
    public List<FrameServiceRoleEntity> getAllServiceRoleList(Integer frameServiceId) {
        return frameServiceRoleMapper.selectByServiceId(frameServiceId);
    }

    // 基础CRUD方法实现

    @Override
    public FrameServiceRoleEntity getById(Integer id) {
        return frameServiceRoleMapper.selectById(id);
    }

    @Override
    public boolean save(FrameServiceRoleEntity entity) {
        return frameServiceRoleMapper.insertEntity(entity) > 0;
    }

    @Override
    public boolean updateById(FrameServiceRoleEntity entity) {
        return frameServiceRoleMapper.updateByIdEntity(entity) > 0;
    }

    @Override
    public boolean removeByIds(List<Integer> ids) {
        return frameServiceRoleMapper.deleteByIds(ids) > 0;
    }
}
