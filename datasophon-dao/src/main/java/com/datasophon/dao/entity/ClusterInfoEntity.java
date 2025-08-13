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

import com.datasophon.common.enums.ClusterState;
import com.datasophon.common.enums.ClusterType;
import com.datasophon.dao.entity.base.BaseEntity;

import java.io.Serial;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;

/**
 * 集群信息实体类
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("t_ddh_cluster_info")
public class ClusterInfoEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 集群名称
     */
    private String clusterName;
    /**
     * 集群编码
     */
    private String clusterCode;
    /**
     * 集群框架
     */
    private String clusterFrame;
    /**
     * 集群版本
     */
    private String frameVersion;
    /**
     * 集群状态 1:待配置 3:正在运行 4:停止 5:删除中
     */
    private ClusterState clusterState;
    /**
     * 集群框架id
     */
    private Integer frameId;
    /**
     * 集群部署模式
     */
    private ClusterType depType;
    /**
     * Kubernetes配置
     */
    private String kubeConfig;
    /**
     * Kubernetes命令空间
     */
    private String namespace;

    @Column(ignore = true)
    private List<UserInfoEntity> clusterManagerList;

    @Column(ignore = true)
    private Integer clusterStateCode;

}
