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
    List<FrameServiceDTO> getAllFrameService(Integer clusterId);

    /**
     * 获取指定集群的所有框架服务，包含必选组件标识
     */
    List<FrameServiceDTO> getAllFrameServiceWithRequired(Integer clusterId, String type);

    /**
     * 根据服务ID列表获取服务信息
     */
    List<FrameServiceDTO> getServiceListByServiceIds(List<Integer> serviceIds);

    /**
     * 根据框架ID和服务名称获取服务信息
     */
    FrameServiceDTO getServiceByFrameIdAndServiceName(Integer frameId, String serviceName);

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
    FrameServiceDTO getFrameServiceById(Integer id);

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
    boolean removeFrameServiceById(Integer id);

    /**
     * 检查服务是否被集群实例使用
     * 
     * @param serviceId 服务ID
     * @return 如果被使用返回true，否则返回false
     */
    boolean isServiceInUse(Integer serviceId);
}
