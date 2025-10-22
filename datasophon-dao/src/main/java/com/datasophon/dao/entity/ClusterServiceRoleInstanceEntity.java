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

import com.datasophon.dao.entity.base.BaseEntity;
import com.datasophon.common.enums.NeedRestart;
import com.datasophon.common.enums.RoleType;
import com.datasophon.common.enums.ServiceRoleState;

import java.io.Serial;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;

/**
 * 集群服务角色实例实体类
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-13
 */
@Table("t_ddh_cluster_service_role_instance")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ClusterServiceRoleInstanceEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /*
      主键
     */

    /**
     * 服务角色名称
     */
    private String serviceRoleName;
    /**
     * 主机
     */
    private String hostname;
    /**
     * 服务角色状态 1:正在运行 2:停止 3:存在告警 4:退役中 5:已退役
     */
    private ServiceRoleState serviceRoleState;

    @Column(ignore = true)
    private Integer serviceRoleStateCode;
    /*
      更新时间
     */

    /*
      创建时间
     */

    /**
     * 服务id
     */
    private Long serviceId;
    /**
     * 角色类型 1:master2:worker3:client
     */
    private RoleType roleType;
    /**
     * 集群id
     */
    private Long clusterId;
    /**
     * 服务名称
     */
    private String serviceName;

    private Long roleGroupId;

    private NeedRestart needRestart;

    @Column(ignore = true)
    private String roleGroupName;

    /**
     * 是否已添加到集群（用于 OLAP 类服务的节点管理）
     * 0: 未添加, 1: 已添加
     */
    private Boolean addedToCluster;

    /**
     * 添加到集群的时间
     */
    private LocalDateTime addToClusterTime;

}
