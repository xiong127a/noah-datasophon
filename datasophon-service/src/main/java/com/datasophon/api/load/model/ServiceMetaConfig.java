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

package com.datasophon.api.load.model;

import com.datasophon.common.model.ServiceInfo;
import com.datasophon.dao.entity.FrameInfoEntity;

import java.util.List;
import java.util.Map;

/**
 * JDK 21 Record: 服务元数据配置
 * 使用record提供不可变的数据载体，替代传统的DTO类
 * 
 * @param frameCode 框架代码
 * @param frameInfo 框架信息实体
 * @param serviceName 服务名称
 * @param serviceDdl 服务DDL定义
 * @param serviceInfo 解析后的服务信息
 * @param serviceInfoMd5 服务信息的MD5值
 * @param configFileMap 配置文件映射
 */
public record ServiceMetaConfig(
    String frameCode,
    FrameInfoEntity frameInfo,
    String serviceName,
    String serviceDdl,
    ServiceInfo serviceInfo,
    String serviceInfoMd5,
    Map<com.datasophon.common.model.Generators, List<com.datasophon.common.model.ServiceConfig>> configFileMap
) {
    
    /**
     * 创建ServiceMetaConfig的便捷工厂方法
     */
    public static ServiceMetaConfig of(String frameCode, 
                                     FrameInfoEntity frameInfo, 
                                     String serviceName, 
                                     String serviceDdl, 
                                     ServiceInfo serviceInfo, 
                                     String serviceInfoMd5,
                                     Map<com.datasophon.common.model.Generators, List<com.datasophon.common.model.ServiceConfig>> configFileMap) {
        return new ServiceMetaConfig(frameCode, frameInfo, serviceName, serviceDdl, serviceInfo, serviceInfoMd5, configFileMap);
    }
    
    /**
     * 获取解压包名称
     */
    public String decompressPackageName() {
        return serviceInfo.getDecompressPackageName();
    }
    
    /**
     * 获取服务参数列表
     */
    public List<com.datasophon.common.model.ServiceConfig> parameters() {
        return serviceInfo.getParameters();
    }
    
    /**
     * 获取服务角色列表
     */
    public List<com.datasophon.common.model.ServiceRoleInfo> serviceRoles() {
        return serviceInfo.getRoles();
    }
}