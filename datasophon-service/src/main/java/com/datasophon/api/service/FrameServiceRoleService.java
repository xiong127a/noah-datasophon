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

import com.datasophon.common.dto.FrameServiceRoleDTO;
import com.datasophon.dao.entity.FrameServiceRoleEntity;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 框架服务角色表服务接口
 * 继承IService提供基础CRUD操作，返回DTO进行数据传输
 * 按照架构重构规范，Service层不返回Result，抛出业务异常
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
public interface FrameServiceRoleService extends IService<FrameServiceRoleEntity> {

    /**
     * 根据集群ID、服务ID列表和角色类型获取服务角色列表（包含主机信息）
     * 
     * @param clusterId       集群ID
     * @param serviceIds      服务ID列表（逗号分隔）
     * @param serviceRoleType 服务角色类型
     * @return 服务角色DTO列表
     */
    List<FrameServiceRoleDTO> getServiceRoleList(Integer clusterId, String serviceIds, Integer serviceRoleType);

    /**
     * 根据服务ID和服务角色名称获取服务角色
     * 
     * @param serviceId 服务ID
     * @param roleName  角色名称
     * @return 服务角色DTO
     */
    FrameServiceRoleDTO getServiceRoleByServiceIdAndServiceRoleName(Integer serviceId, String roleName);

    /**
     * 根据服务ID和服务角色名称查找服务角色（用于角色发现，不抛出异常）
     * 
     * @param serviceId 服务ID
     * @param roleName  角色名称
     * @return Optional包装的服务角色DTO，如果不存在则返回空Optional
     */
    java.util.Optional<FrameServiceRoleDTO> findServiceRoleByServiceIdAndServiceRoleName(Integer serviceId, String roleName);

    /**
     * 根据集群框架和服务角色名称获取服务角色
     * 
     * @param clusterFrame    集群框架
     * @param serviceRoleName 服务角色名称
     * @return 服务角色DTO
     */
    FrameServiceRoleDTO getServiceRoleByFrameCodeAndServiceRoleName(String clusterFrame, String serviceRoleName);

    /**
     * 获取非Master角色列表（包含主机信息）
     * 
     * @param clusterId  集群ID
     * @param serviceIds 服务ID列表（逗号分隔）
     * @return 非Master角色DTO列表
     */
    List<FrameServiceRoleDTO> getNonMasterRoleList(Integer clusterId, String serviceIds);

    /**
     * 根据服务名称获取服务角色列表
     * 
     * @param clusterId   集群ID
     * @param serviceName 服务名称
     * @return 服务角色DTO列表
     */
    List<FrameServiceRoleDTO> getServiceRoleByServiceName(Integer clusterId, String serviceName);

    /**
     * 获取所有服务角色列表
     * 
     * @param frameServiceId 框架服务ID
     * @return 服务角色DTO列表
     */
    List<FrameServiceRoleDTO> getAllServiceRoleList(Integer frameServiceId);

    /**
     * 根据ID获取服务角色DTO
     * 
     * @param id 主键ID
     * @return 服务角色DTO
     */
    FrameServiceRoleDTO getFrameServiceRoleById(Integer id);

    /**
     * 保存服务角色
     * 
     * @param frameServiceRoleDTO 服务角色DTO
     * @return 保存后的服务角色DTO
     */
    FrameServiceRoleDTO saveFrameServiceRole(FrameServiceRoleDTO frameServiceRoleDTO);

    /**
     * 更新服务角色
     * 
     * @param frameServiceRoleDTO 服务角色DTO
     * @return 更新后的服务角色DTO
     */
    FrameServiceRoleDTO updateFrameServiceRole(FrameServiceRoleDTO frameServiceRoleDTO);

    /**
     * 根据ID列表批量删除服务角色
     * 
     * @param ids ID列表
     * @return 是否删除成功
     */
    boolean removeFrameServiceRoleByIds(List<Integer> ids);

    /**
     * 根据服务ID删除相关的服务角色配置
     * 
     * @param serviceId 服务ID
     * @return 是否删除成功
     */
    boolean removeByServiceId(Integer serviceId);
}
