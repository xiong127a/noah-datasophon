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

package com.datasophon.kubernetes.model;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * Kubernetes节点信息模型
 * 独立于DAO层的K8S领域模型
 */
@Data
@Builder
public class K8sNodeInfo {

    /**
     * 节点IP地址
     */
    private String ip;

    /**
     * 主机名
     */
    private String hostname;

    /**
     * CPU核心数
     */
    private Integer coreNum;

    /**
     * 总内存(GB)
     */
    private Integer totalMem;

    /**
     * 已使用内存(GB)
     */
    private Integer usedMem;

    /**
     * 总磁盘空间(GB)
     */
    private Integer totalDisk;

    /**
     * 已使用磁盘空间(GB)
     */
    private Integer usedDisk;

    /**
     * CPU架构
     */
    private String cpuArchitecture;

    /**
     * 节点状态（Ready, NotReady等）
     */
    private String status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 节点标签
     */
    private String labels;

    /**
     * 可分配的CPU资源
     */
    private String allocatableCpu;

    /**
     * 可分配的内存资源
     */
    private String allocatableMemory;

    /**
     * 可分配的存储资源
     */
    private String allocatableStorage;
}