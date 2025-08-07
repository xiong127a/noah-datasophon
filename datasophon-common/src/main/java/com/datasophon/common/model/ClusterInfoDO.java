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

package com.datasophon.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 集群信息业务对象
 * 用于业务逻辑层处理
 *
 * @author 数据大平台团队
 * @date 2025-01-01
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClusterInfoDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Integer id;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    private Date createTime;

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
     * 集群状态 1:待配置 2：正在运行 3: 停止 4: 删除中 5: 已删除
     */
    private Integer clusterState;

    /**
     * 集群框架id
     */
    private Integer frameId;

    /**
     * 集群部署模式
     */
    private String depType;

    /**
     * Kubernetes配置
     */
    private String kubeConfig;

    /**
     * Kubernetes命令空间
     */
    private String namespace;

    /**
     * 集群状态码（业务扩展字段）
     */
    private Integer clusterStateCode;

    /**
     * 检查是否为Kubernetes模式
     */
    public boolean isKubernetesMode() {
        return "kubernetes".equalsIgnoreCase(this.depType) || 
               "k8s".equalsIgnoreCase(this.depType);
    }

    /**
     * 检查是否为PVM模式
     */
    public boolean isPvmMode() {
        return !isKubernetesMode();
    }

    /**
     * 获取集群类型描述
     */
    public String getClusterTypeDescription() {
        return isKubernetesMode() ? "Kubernetes集群" : "物理机集群";
    }
}