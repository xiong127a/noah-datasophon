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
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Table("t_ddh_cluster_host")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClusterHostDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id
    private Integer id;
    /**
     * 创建时间
     */
    private Date createTime;
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
     * 已用内存
     */
    private Integer usedMem;
    /**
     * 已用磁盘
     */
    private Integer usedDisk;
    /**
     * 平均负载
     */
    private String averageLoad;
    /**
     * 检测时间
     */
    private Date checkTime;
    /**
     * 集群id
     */
    private Integer clusterId;
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
     * Kubernetes节点名称
     */
    private String k8sNodeName;

    /**
     * Kubernetes节点版本
     */
    private String k8sNodeVersion;

    /**
     * Kubernetes节点运行时长
     */
    private String k8sNodeAge;

    @Column(ignore = true)
    private Integer serviceRoleNum;

    // 管理状态同步方法已移除，统一使用managementStatus字段

}
