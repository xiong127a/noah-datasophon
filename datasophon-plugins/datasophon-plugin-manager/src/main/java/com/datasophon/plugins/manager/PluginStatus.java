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

package com.datasophon.plugins.manager;

/**
 * 插件状态枚举
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
public enum PluginStatus {
    /**
     * 活跃状态 - 插件已加载且正常运行
     */
    ACTIVE,
    
    /**
     * 非活跃状态 - 插件已加载但未启动
     */
    INACTIVE,
    
    /**
     * 错误状态 - 插件加载或运行时发生错误
     */
    ERROR,
    
    /**
     * 加载中状态 - 插件正在加载过程中
     */
    LOADING
}