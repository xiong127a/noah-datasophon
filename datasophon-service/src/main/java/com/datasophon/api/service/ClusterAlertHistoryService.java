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

import com.datasophon.common.dto.ClusterAlertHistoryDTO;
import com.datasophon.common.model.PageResult;
import com.datasophon.dao.entity.ClusterAlertHistory;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 集群告警历史服务接口
 * 提供集群告警历史的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
public interface ClusterAlertHistoryService extends IService<ClusterAlertHistory> {

    /**
     * 保存告警历史（异步处理告警消息）
     * 
     * @param alertMessage 告警消息
     */
    void saveAlertHistory(String alertMessage);

    /**
     * 根据服务实例ID获取告警列表
     * 
     * @param serviceInstanceId 服务实例ID
     * @return 告警历史DTO列表
     */
    List<ClusterAlertHistoryDTO> getAlertList(Integer serviceInstanceId);

    /**
     * 分页查询所有告警历史
     * 
     * @param clusterId 集群ID
     * @param page      页码
     * @param pageSize  页大小
     * @return 分页DTO结果
     */
    PageResult<ClusterAlertHistoryDTO> getAllAlertList(Integer clusterId, Integer page, Integer pageSize);

    /**
     * 根据角色实例ID列表删除告警（业务逻辑：删除+重新配置Prometheus）
     * 
     * @param ids 角色实例ID列表
     */
    void removeAlertByRoleInstanceIds(List<Integer> ids);

    /**
     * 根据ID获取告警历史DTO
     *
     * @param id 告警历史ID
     * @return 告警历史DTO
     */
    ClusterAlertHistoryDTO getByIdAsDto(Integer id);

    /**
     * 保存告警历史
     *
     * @param dto 告警历史DTO
     */
    void saveAlertHistoryDto(ClusterAlertHistoryDTO dto);

    /**
     * 更新告警历史
     *
     * @param dto 告警历史DTO
     */
    void updateAlertHistory(ClusterAlertHistoryDTO dto);
}