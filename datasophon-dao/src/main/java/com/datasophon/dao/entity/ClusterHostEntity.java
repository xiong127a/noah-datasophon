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

package com.datasophon.dao.entity;

import com.datasophon.common.enums.HostState;
import com.datasophon.common.enums.ManagementStatus;
import com.datasophon.dao.entity.base.BaseEntity;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 集群主机实体类
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-13
 */
@Table("t_ddh_cluster_host")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ClusterHostEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 主机名
     */
    private String hostname;
    /**
     * IP
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
     * 总内存
     */
    private Integer totalMem;
    /**
     * 总磁盘
     */
    private Integer totalDisk;

    /**
     * 平均负载
     */
    private String averageLoad;
    /**
     * 检测时间
     */
    private LocalDateTime checkTime;
    /**
     * 集群id
     */
    private Long clusterId;
    /**
     * 1:正常运行 2：断线 3、存在告警
     */
    private HostState hostState;
    // MANAGED字段已删除，统一使用managementStatus

    /**
     * 主机管理状态：1-受管，2-未受管，3-配置中
     */
    private ManagementStatus managementStatus;

    private String cpuArchitecture;

    /**
     * 主机标签（用户自定义标签）
     */
    private String nodeLabel;

    /**
     * Kubernetes节点扩展信息JSON
     * 格式：{"status": "Ready", "roles": "<none>", "age": "43d", "version": "v1.28.9"}
     */
    private String k8sNodeInfo;

    @Column(ignore = true)
    private Integer serviceRoleNum;

    // 管理状态同步方法已移除，统一使用managementStatus字段

}
