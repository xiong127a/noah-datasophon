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

import com.datasophon.common.dto.FrameServiceDTO;
import com.datasophon.dao.entity.FrameServiceEntity;
import com.mybatisflex.core.service.IService;
import com.datasophon.common.enums.ServiceType;

import java.util.List;

/**
 * 集群框架版本服务表服务接口
 * 继承IService提供基础CRUD操作，返回DTO进行数据传输
 * 按照架构重构规范，Service层不返回Result，抛出业务异常
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
public interface FrameServiceService extends IService<FrameServiceEntity> {

    /**
     * 获取指定集群的所有框架服务（包含安装状态）
     */
    List<FrameServiceDTO> getAllFrameService(Long clusterId);

    /**
     * 获取指定集群的所有框架服务，包含必选组件标识
     */
    List<FrameServiceDTO> getAllFrameServiceWithRequired(Long clusterId, ServiceType type);

    /**
     * 根据服务ID列表获取服务信息
     */
    List<FrameServiceDTO> getServiceListByServiceIds(List<Long> serviceIds);

    /**
     * 根据框架ID和服务名称获取服务信息
     */
    FrameServiceDTO getServiceByFrameIdAndServiceName(Long frameId, String serviceName);

    /**
     * 根据框架ID和服务名称查找服务信息（用于服务发现，不抛出异常）
     * @param frameId 框架ID
     * @param serviceName 服务名称
     * @return Optional包装的服务信息，如果不存在则返回空Optional
     */
    java.util.Optional<FrameServiceDTO> findServiceByFrameIdAndServiceName(Long frameId, String serviceName);

    /**
     * 根据框架代码和服务名称获取服务信息
     */
    FrameServiceDTO getServiceByFrameCodeAndServiceName(String frameCode, String serviceName);

    /**
     * 根据框架代码获取所有服务信息
     */
    List<FrameServiceDTO> getAllFrameServiceByFrameCode(String frameCode);

    /**
     * 根据服务ID字符串获取服务列表
     */
    List<FrameServiceDTO> listServices(String serviceIds);

    /**
     * 根据ID获取服务信息
     */
    FrameServiceDTO getFrameServiceById(Long id);

    /**
     * 保存服务信息
     */
    FrameServiceDTO saveFrameService(FrameServiceDTO frameServiceDTO);

    /**
     * 更新服务信息
     */
    FrameServiceDTO updateFrameService(FrameServiceDTO frameServiceDTO);

    /**
     * 批量删除服务信息
     */
    boolean removeFrameServiceByIds(List<Integer> ids);

    /**
     * 删除单个服务（包含文件清理）
     */
    boolean removeFrameServiceById(Long id);

    /**
     * 检查服务是否被集群实例使用
     * 
     * @param serviceId 服务ID
     * @return 如果被使用返回true，否则返回false
     */
    boolean isServiceInUse(Long serviceId);

    /**
     * 批量查询指定框架和服务名的服务列表
     * 用于批量优化数据库操作，减少SQL执行次数
     * 
     * @param frameId 框架ID
     * @param serviceNames 服务名称列表
     * @return 服务信息列表
     */
    List<FrameServiceDTO> findServicesByFrameIdAndNames(Long frameId, List<String> serviceNames);
}
