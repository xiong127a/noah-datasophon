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

package com.datasophon.common.command;

import com.datasophon.common.model.Generators;
import com.datasophon.common.model.RunAs;
import com.datasophon.common.model.ServiceConfig;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class GenerateServiceConfigCommand implements Serializable {

    @Serial
    private static final long serialVersionUID = -4211566568993105684L;

    private String serviceName;

    private String decompressPackageName;

    private Integer myid;

    Map<Generators, List<ServiceConfig>> cofigFileMap;

    private String serviceRoleName;

    private RunAs runAs;

    private String hostName;

    private String kubeConfig;

    private String namespace;
    
    private Long clusterId;
    
    /**
     * 模板内容映射：templateName -> templateContent
     * API 端将模板内容预先打包在命令中，避免 Worker 回连 API 获取
     */
    private Map<String, String> templateContents = new HashMap<>();
}
