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

package com.datasophon.plugins.api;

import lombok.Getter;

/**
 * 插件ID枚举
 * 统一管理所有插件的唯一标识符
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-09-01
 */
@Getter
public enum PluginId {
    
    /**
     * SSH连接器插件
     * 提供SSH连接检查和命令执行功能
     */
    SSH_CONNECTOR("ssh-connector", "SSH连接器插件"),
    
    /**
     * 主机修复插件
     * 提供主机问题自动修复功能
     */
    HOST_REPAIR("host-repair", "主机修复插件"),
    
    /**
     * 主机校验插件
     * 提供主机环境校验功能
     */
    HOST_VALIDATION("host-validation", "主机校验插件"),
    
    /**
     * 系统信息收集插件
     * 提供系统信息收集功能
     */
    SYSTEM_INFO_COLLECTOR("system-info-collector", "系统信息收集插件");
    
    /**
     * 插件ID字符串
     */
    private final String id;
    
    /**
     * 插件显示名称
     */
    private final String displayName;
    
    PluginId(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }
    
    /**
     * 根据ID字符串查找对应的枚举
     * 
     * @param id 插件ID字符串
     * @return 对应的枚举，如果未找到则返回null
     */
    public static PluginId fromId(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        
        for (PluginId pluginId : values()) {
            if (pluginId.getId().equals(id.trim())) {
                return pluginId;
            }
        }
        return null;
    }
    
    /**
     * 检查指定ID是否为有效的插件ID
     * 
     * @param id 插件ID字符串
     * @return 如果是有效的插件ID返回true，否则返回false
     */
    public static boolean isValidId(String id) {
        return fromId(id) != null;
    }
    
    /**
     * 获取所有插件ID的字符串列表
     * 
     * @return 所有插件ID字符串的列表
     */
    public static java.util.List<String> getAllIds() {
        return java.util.Arrays.stream(values())
                .map(PluginId::getId)
                .collect(java.util.stream.Collectors.toList());
    }
    
    @Override
    public String toString() {
        return id;
    }
}
