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

/**
 * Kubernetes资源信息模型
 * 用于表示CPU、内存、存储等资源的容量和可分配量
 */
@Data
@Builder
public class K8sResourceInfo {

    /**
     * 资源名称（cpu, memory, ephemeral-storage等）
     */
    private String resourceName;

    /**
     * 资源总容量
     */
    private Long capacity;

    /**
     * 可分配资源量
     */
    private Long allocatable;

    /**
     * 已使用资源量（计算得出：capacity - allocatable）
     */
    private Long used;

    /**
     * 资源单位（cores, Ki, Mi, Gi等）
     */
    private String unit;

    /**
     * 资源的数值（去除单位后的纯数字）
     */
    private Long numericalValue;
}