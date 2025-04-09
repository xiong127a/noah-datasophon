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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 服务连接信息实体
 * 包括基本连接信息、JDBC URL、代码示例等
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 基本连接信息，如主机名、端口等
     */
    private Map<String, String> basicInfo;

    /**
     * JDBC URL
     */
    private String jdbcUrl;

    /**
     * Java代码示例
     */
    private String javaCode;

    /**
     * Python代码示例
     */
    private String pythonCode;

    /**
     * 命令行连接示例（如beeline命令）
     */
    private String beelineCommand;

    /**
     * 基础连接信息列表（用于适配前端的表格显示）
     * 每个元素包含label和value
     */
    private List<Map<String, String>> basicInfoList;

    /**
     * JDBC URL列表（用于适配前端的多个JDBC URL显示）
     * 每个元素包含label和value
     */
    private List<Map<String, String>> jdbcUrls;

    /**
     * 命令行连接示例列表（用于适配前端的多个命令行示例显示）
     * 每个元素包含label和value
     */
    private List<Map<String, String>> commandLines;
}