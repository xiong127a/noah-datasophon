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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 主机信息数据传输对象
 * 用于系统内部各模块之间的主机信息传输，包含K8S扩展字段
 *
 * @author DataSophon Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HostInfoDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 主机名
     */
    private String hostname;

    /**
     * IP地址
     */
    private String ip;

    /**
     * 机架
     */
    private String rack;

    /**
     * 核数
     */
    private Integer coreNum;

    /**
     * 总内存(GB)
     */
    private Integer totalMem;

    /**
     * 总磁盘(GB)
     */
    private Integer totalDisk;

    /**
     * 已用内存(GB)
     */
    private Integer usedMem;

    /**
     * 已用磁盘(GB)
     */
    private Integer usedDisk;

    /**
     * 平均负载
     */
    private String averageLoad;

    /**
     * 检测时间
     */
    private LocalDateTime checkTime;

    /**
     * 集群ID
     */
    private Long clusterId;

    /**
     * 主机状态枚举
     */
    private HostState hostState;

    /**
     * 管理状态枚举
     */
    private ManagementStatus managementStatus;

    /**
     * CPU架构
     */
    private String cpuArchitecture;

    /**
     * 节点标签
     */
    private String nodeLabel;

    /**
     * 服务角色数量
     */
    private Integer serviceRoleNum;

    // =============== K8S扩展字段 ===============
    /**
     * 节点角色（control-plane, master, <none>）
     */
    private String roles;

    /**
     * Kubernetes版本
     */
    private String version;

    /**
     * 节点年龄（如：43d, 2h30m）
     */
    private String age;

    /**
     * 节点状态字符串（Ready, NotReady）
     */
    private String status;

}