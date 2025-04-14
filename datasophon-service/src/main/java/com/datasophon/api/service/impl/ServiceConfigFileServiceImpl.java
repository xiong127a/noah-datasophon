/*
 *
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
 *
 */

package com.datasophon.api.service.impl;

import cn.hutool.core.util.StrUtil;
import com.datasophon.api.service.ClusterServiceInstanceService;
import com.datasophon.api.service.ServiceConfigFileService;
import com.datasophon.api.strategy.ServiceRoleStrategy;
import com.datasophon.api.strategy.ServiceRoleStrategyContext;
import com.datasophon.common.model.ConfigFile;
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.AbstractMap;
import java.util.List;

/**
 * 服务配置文件服务实现
 */
@Service
@Slf4j
public class ServiceConfigFileServiceImpl implements ServiceConfigFileService {

    @Autowired
    private ClusterServiceInstanceService serviceInstanceService;

    /**
     * 获取服务配置文件列表
     *
     * @param serviceInstanceId 服务实例ID
     * @return 配置文件列表
     */
    @Override
    public List<ConfigFile> getServiceConfigFiles(Integer serviceInstanceId) {
        // 获取服务实例信息
        AbstractMap.SimpleEntry<ClusterServiceInstanceEntity, ServiceRoleStrategy> instanceId = getServiceRoleStrategyByInstanceId(serviceInstanceId);
        ServiceRoleStrategy strategy = instanceId.getValue();

        return strategy.getServiceConfigFiles(serviceInstanceId);
    }

    /**
     * 获取配置文件内容
     *
     * @param serviceInstanceId 服务实例ID
     * @param fileName          文件名
     * @return 文件内容
     */
    @Override
    public byte[] getServiceConfigFileContent(Integer serviceInstanceId, String fileName) {
        // TODO: 实现获取配置文件内容的逻辑
        AbstractMap.SimpleEntry<ClusterServiceInstanceEntity, ServiceRoleStrategy> instanceId = getServiceRoleStrategyByInstanceId(serviceInstanceId);
        ServiceRoleStrategy strategy = instanceId.getValue();

        return strategy.getServiceConfigFileContent(serviceInstanceId,fileName);
    }

    /**
     * 获取所有配置文件并打包成zip
     *
     * @param serviceInstanceId 服务实例ID
     * @return zip文件内容
     */
    @Override
    public byte[] getAllServiceConfigFilesAsZip(Integer serviceInstanceId) {
        // TODO: 实现获取所有配置文件并打包成zip的逻辑
        // 这里只是骨架代码，具体实现由您完成
        AbstractMap.SimpleEntry<ClusterServiceInstanceEntity, ServiceRoleStrategy> instanceId = getServiceRoleStrategyByInstanceId(serviceInstanceId);
        ServiceRoleStrategy strategy = instanceId.getValue();
        return strategy.getAllServiceConfigFilesAsZip(serviceInstanceId);
    }

    /**
     * 获取服务名称
     *
     * @param serviceInstanceId 服务实例ID
     * @return 服务名称
     */
    @Override
    public String getServiceName(Integer serviceInstanceId) {
        ClusterServiceInstanceEntity serviceInstance = serviceInstanceService.getById(serviceInstanceId);
        return serviceInstance != null ? serviceInstance.getServiceName() : "unknown";
    }



    /**
     * 根据服务实例ID获取服务角色策略
     *
     * @param serviceInstanceId 服务实例ID
     * @return 服务角色策略
     */
    public AbstractMap.SimpleEntry<ClusterServiceInstanceEntity, ServiceRoleStrategy> getServiceRoleStrategyByInstanceId(Integer serviceInstanceId){
        // 获取服务实例信息
        ClusterServiceInstanceEntity serviceInstance = serviceInstanceService.getById(serviceInstanceId);
        if (serviceInstance == null) {
            throw new RuntimeException("服务实例不存在，serviceInstanceId: " + serviceInstanceId);
        }
        // 获取服务名称
        String serviceName = serviceInstance.getServiceName();
        if (StrUtil.isBlank(serviceName)) {
            throw new RuntimeException("服务名称不能为空，serviceInstanceId: " + serviceInstanceId);
        }

        // 使用策略模式获取对应服务的连接信息
        ServiceRoleStrategy serviceRoleHandler = ServiceRoleStrategyContext.getServiceRoleHandler(serviceName);
        if (serviceRoleHandler == null) {
            throw new RuntimeException("未找到服务角色策略，serviceName: " + serviceName);
        }
        return new AbstractMap.SimpleEntry<>(serviceInstance, serviceRoleHandler);
    }
}