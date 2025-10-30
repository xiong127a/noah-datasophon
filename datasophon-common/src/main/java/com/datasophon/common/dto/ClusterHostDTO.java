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

package com.datasophon.common.dto;

import com.datasophon.common.enums.HostState;
import com.datasophon.common.enums.ManagementStatus;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 集群主机DTO
 * Controller层使用的数据传输对象
 * 
 * @author DataSophon Team
 */
public record ClusterHostDTO(
    Long id,
    String hostname,
    String ip,
    String rack,
    Integer coreNum,
    Integer totalMem,
    Integer totalDisk,
    String averageLoad,
    LocalDateTime checkTime,
    Long clusterId,
    HostState hostState,
    ManagementStatus managementStatus,
    String cpuArchitecture,
    String nodeLabel,
    String k8sNodeInfo,
    Integer serviceRoleNum,
    LocalDateTime createTime,
    LocalDateTime updateTime
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}

