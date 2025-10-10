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

package com.datasophon.common.model.kubernetes;

import lombok.Data;

import java.util.List;

@Data
public class KubernetesNamespaceDto {

    /**
     * 是否显示命名空间选择器
     */
    private Boolean showNamespaceSelector;

    /**
     * 默认命名空间
     */
    private String defaultNamespace;

    /**
     * 集群版本
     */
    private String clusterVersion;

    /**
     * 命名空间列表
     */
    private List<String> namespaces;

    public KubernetesNamespaceDto() {
        this.showNamespaceSelector = true;
        this.defaultNamespace = "datasophon";
    }

    public KubernetesNamespaceDto(List<String> namespaces, String clusterVersion) {
        this();
        this.namespaces = namespaces;
        this.clusterVersion = clusterVersion;
    }

    public KubernetesNamespaceDto(List<String> namespaces, String clusterVersion, String defaultNamespace) {
        this(namespaces, clusterVersion);
        this.defaultNamespace = defaultNamespace;
    }

    public KubernetesNamespaceDto(List<String> namespaces, String clusterVersion, String defaultNamespace,
            Boolean showNamespaceSelector) {
        this(namespaces, clusterVersion, defaultNamespace);
        this.showNamespaceSelector = showNamespaceSelector;
    }
}