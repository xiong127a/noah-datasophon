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

package com.datasophon.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 集群服务命令主机命令数据传输对象
 * 用于服务间数据传输
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
public record ClusterServiceCommandHostCommandDTO(
        Long hostCommandId,
        String commandName,
        Integer commandState,
        Integer commandStateCode,
        Integer commandProgress,
        Long commandHostId,
        Long commandId,
        String hostname,
        String serviceName, // 🔧 添加服务名称字段（用于显示SVG图标）
        String serviceRoleName,
        Integer serviceRoleType,
        String resultMsg,
        LocalDateTime createTime,
        Integer commandType) implements Serializable {
}