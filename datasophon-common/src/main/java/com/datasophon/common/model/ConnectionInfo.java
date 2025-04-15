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
import java.util.List;
import java.util.Map;

/**
 * 连接信息实体类
 * 用于展示服务的连接信息，包括基本信息、JDBC URL、代码示例等
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 基本连接信息Map，已弃用，请使用basicInfoList
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
     * 命令行示例列表
     */
    private List<CommandLineItem> commandLines;

    /**
     * 有序的基本信息列表
     */
    private List<Map<String, String>> basicInfoList;

    /**
     * 多个JDBC URL列表
     */
    private List<Map<String, String>> jdbcUrls;
    /**
     * 主机名
     */
    private String hostName;

    /**
     * Java代码示例标题
     */
    private String javaTitle;

    /**
     * Python代码示例标题
     */
    private String pythonTitle;

    /**
     * 命令行示例标题
     */
    private String commandTitle;

    /**
     * Java示例文件名
     */
    private String javaFileName;

    /**
     * Python示例文件名
     */
    private String pythonFileName;

    /**
     * 服务安装目录
     */
    private String serviceHome;
}