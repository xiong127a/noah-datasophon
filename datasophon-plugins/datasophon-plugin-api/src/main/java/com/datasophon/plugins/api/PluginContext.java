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

import java.util.Map;

/**
 * 插件上下文信息
 * 为所有插件提供运行时上下文数据
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
public record PluginContext(
    String pluginId,
    String version,
    String description,
    Map<String, Object> properties,
    Map<String, String> configuration
) {
    
    /**
     * 获取配置项
     */
    public String getConfig(String key) {
        return configuration.get(key);
    }
    
    /**
     * 获取配置项，带默认值
     */
    public String getConfig(String key, String defaultValue) {
        return configuration.getOrDefault(key, defaultValue);
    }
    
    /**
     * 获取属性
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }
    
    /**
     * 检查是否启用
     */
    public boolean isEnabled() {
        return Boolean.parseBoolean(getConfig("enabled", "true"));
    }
}
