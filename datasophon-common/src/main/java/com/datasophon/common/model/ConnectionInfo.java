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
import java.util.List;
import java.util.Map;

/**
 * 连接信息实体类
 * 用于展示服务的连接信息，基本信息按照分组进行展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 基础信息项列表
     * 包含主机名、端口、集群名等基本配置信息
     */
    private List<InfoItem> basicInfoItems;

    /**
     * 安全信息项列表
     * 包含认证模式、用户名、密码等安全认证信息
     */
    private List<InfoItem> securityInfoItems;

    /**
     * 连接信息项列表
     * 包含连接URL、连接字符串等直接用于连接的信息
     */
    private List<InfoItem> connectInfoItems;

    /**
     * 重要信息标识
     * 保存需要重点突出显示的信息键名列表
     */
    private List<String> importantKeys;

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

    /**
     * Java依赖信息
     * 包含Maven依赖等信息
     */
    private String javaDependencies;

    /**
     * Python依赖信息
     * 包含pip安装命令等信息
     */
    private String pythonDependencies;

    /**
     * Java依赖摘要
     * 简短描述依赖信息
     */
    private String javaDependenciesSummary;

    /**
     * Python依赖摘要
     * 简短描述依赖信息
     */
    private String pythonDependenciesSummary;

    /**
     * 模板变量
     * 用于存储传递给代码模板的变量
     */
    private Map<String, Object> templateVariables;

    /**
     * 从基本信息项列表中获取指定键的值
     *
     * @param key          键名
     * @param defaultValue 默认值
     * @return 查找到的值或默认值
     */
    public String getBasicInfoValue(String key, String defaultValue) {
        if (basicInfoItems == null)
            return defaultValue;
        return basicInfoItems.stream()
                .filter(item -> key.equals(item.getKey()))
                .map(InfoItem::getValue)
                .findFirst().orElse(defaultValue);
    }

    /**
     * 从安全信息项列表中获取指定键的值
     *
     * @param key          键名
     * @param defaultValue 默认值
     * @return 查找到的值或默认值
     */
    public String getSecurityInfoValue(String key, String defaultValue) {
        if (securityInfoItems == null)
            return defaultValue;
        return securityInfoItems.stream()
                .filter(item -> key.equals(item.getKey()))
                .map(InfoItem::getValue)
                .findFirst().orElse(defaultValue);
    }

    /**
     * 从连接信息项列表中获取指定键的值
     *
     * @param key          键名
     * @param defaultValue 默认值
     * @return 查找到的值或默认值
     */
    public String getConnectInfoValue(String key, String defaultValue) {
        if (connectInfoItems == null)
            return defaultValue;
        return connectInfoItems.stream()
                .filter(item -> key.equals(item.getKey()))
                .map(InfoItem::getValue)
                .findFirst().orElse(defaultValue);
    }
}