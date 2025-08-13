/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.datasophon.api.service;

import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.enums.AlertLevel;
import com.datasophon.common.enums.ServiceRoleState;

/**
 * 服务状态管理服务
 * 负责服务状态更新和告警管理相关的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
public interface ServiceStateManagementService {

    /**
     * 更新服务角色状态
     *
     * @param commandType      命令类型
     * @param serviceRoleName  服务角色名称
     * @param hostname         主机名
     * @param clusterId        集群ID
     * @param serviceRoleState 服务角色状态
     */
    void updateServiceRoleState(CommandType commandType, String serviceRoleName, String hostname,
            Long clusterId, ServiceRoleState serviceRoleState);

    /**
     * 保存告警信息
     *
     * @param roleInstanceDto 角色实例DTO
     * @param alertTargetName 告警目标名称
     * @param alertLevel      告警级别
     * @param alertAdvice     告警建议
     */
    void saveAlert(ClusterServiceRoleInstanceDTO roleInstanceDto, String alertTargetName,
            AlertLevel alertLevel, String alertAdvice);

    /**
     * 恢复告警
     *
     * @param roleInstanceDto 角色实例DTO
     */
    void recoverAlert(ClusterServiceRoleInstanceDTO roleInstanceDto);
}