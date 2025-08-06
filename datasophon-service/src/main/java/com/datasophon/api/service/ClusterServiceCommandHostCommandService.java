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

import com.datasophon.common.dto.ClusterServiceCommandHostCommandDTO;
import com.datasophon.dao.entity.ClusterServiceCommandHostCommandEntity;
import com.datasophon.common.model.PageResult;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 集群服务命令主机命令服务接口
 * 提供集群服务命令主机命令的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
public interface ClusterServiceCommandHostCommandService extends IService<ClusterServiceCommandHostCommandEntity> {

    /**
     * 获取主机命令列表（分页）
     * 按照架构重构规范，返回DTO分页结果
     */
    PageResult<ClusterServiceCommandHostCommandDTO> getHostCommandList(String hostname, String commandHostId,
            Integer page, Integer pageSize);

    /**
     * 根据命令ID获取主机命令列表
     */
    List<ClusterServiceCommandHostCommandDTO> getHostCommandListByCommandId(String id);

    /**
     * 根据主机命令ID获取命令
     */
    ClusterServiceCommandHostCommandDTO getByHostCommandId(String hostCommandId);

    /**
     * 根据主机命令ID更新命令
     * 按照架构重构规范，接收DTO而不是Entity
     */
    void updateByHostCommandId(ClusterServiceCommandHostCommandDTO hostCommandDTO);

    /**
     * 获取主机命令大小
     */
    Long getHostCommandSizeByHostnameAndCommandHostId(String hostname, String commandHostId);

    /**
     * 获取主机命令总进度
     */
    Integer getHostCommandTotalProgressByHostnameAndCommandHostId(String hostname, String commandHostId);

    /**
     * 获取主机命令日志
     */
    String getHostCommandLog(Integer clusterId, String hostCommandId) throws Exception;

    /**
     * 查找失败的主机命令
     */
    List<ClusterServiceCommandHostCommandDTO> findFailedHostCommand(String hostname, String commandHostId);

    /**
     * 查找取消的主机命令
     */
    List<ClusterServiceCommandHostCommandDTO> findCanceledHostCommand(String hostname, String commandHostId);

    /**
     * 根据ID获取主机命令DTO
     */
    ClusterServiceCommandHostCommandDTO getByIdAsDto(String id);

    /**
     * 保存主机命令DTO
     */
    ClusterServiceCommandHostCommandDTO saveHostCommand(ClusterServiceCommandHostCommandDTO dto);

    /**
     * 更新主机命令
     */
    void updateHostCommand(ClusterServiceCommandHostCommandDTO dto);
}
