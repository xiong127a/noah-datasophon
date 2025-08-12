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

package com.datasophon.api.service;

import com.datasophon.common.model.HostServiceRoleMapping;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleHostMapping;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 服务安装服务接口
 * 按照架构重构规范，Service层不返回Result，而返回DTO或抛出异常
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
public interface ServiceInstallService {

    /**
     * 获取服务配置选项
     * 返回扁平化的配置列表，确保前端兼容性
     * 
     * @param clusterId   集群ID
     * @param serviceName 服务名称
     * @return 服务配置列表
     */
    List<ServiceConfig> getServiceConfigOption(Integer clusterId, String serviceName);

    /**
     * 保存服务角色主机映射
     * 
     * @param clusterId 集群ID
     * @param list      服务角色主机映射列表
     * @return 是否创建了新版本
     */
    boolean saveServiceRoleHostMapping(Integer clusterId, List<ServiceRoleHostMapping> list);

    /**
     * 保存服务配置
     * 
     * @param clusterId   集群ID
     * @param serviceName 服务名称
     * @param configJson  配置列表
     * @param roleGroupId 角色组ID
     * @param description 描述
     * @param userId      用户ID
     * @param username    用户名
     * @return 是否创建了新版本
     */
    boolean saveServiceConfig(Integer clusterId, String serviceName, List<ServiceConfig> configJson,
            Integer roleGroupId, String description, Integer userId, String username);

    /**
     * 保存主机服务角色映射
     * 
     * @param clusterId 集群ID
     * @param list      主机服务角色映射列表
     */
    void saveHostServiceRoleMapping(Integer clusterId, List<HostServiceRoleMapping> list);

    /**
     * 获取服务角色部署概览
     * 
     * @param clusterId 集群ID
     * @return 服务角色主机映射
     */
    Map<String, List<String>> getServiceRoleDeployOverview(Integer clusterId);

    /**
     * 开始安装服务
     * 
     * @param clusterId  集群ID
     * @param commandIds 命令ID列表
     * @return 安装结果映射
     */
    Map<String, Object> startInstallService(Integer clusterId, List<String> commandIds);

    /**
     * 下载包
     * 
     * @param packageName 包名
     * @param response    HTTP响应
     * @throws IOException IO异常
     */
    void downloadPackage(String packageName, HttpServletResponse response) throws IOException;

    /**
     * 获取服务角色主机映射
     * 
     * @param clusterId 集群ID
     */
    void getServiceRoleHostMapping(Integer clusterId);

    /**
     * 检查服务依赖
     * 
     * @param clusterId  集群ID
     * @param serviceIds 服务ID字符串
     */
    void checkServiceDependency(Integer clusterId, String serviceIds);
}
