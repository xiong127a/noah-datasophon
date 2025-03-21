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

import lombok.Data;

/**
 * 异步服务状态实体类
 */
@Data
public class AsyncServiceStatus {
    
    // 定时任务是否启用
    private Boolean scheduledTasksEnabled;
    
    // 上次任务清理时间
    private String lastTaskCleanupTime;
    
    // 运行中的任务数量
    private Integer runningTasksCount;
    
    // 连接池大小
    private Integer connectionPoolSize;
    
    // 任务清理是否活跃
    private Boolean taskCleanupActive;
    
    // 连接清理是否活跃
    private Boolean connectionCleanupActive;
    
    // 上次连接清理时间
    private String lastConnectionCleanupTime;
    
    // 任务清理间隔（毫秒）
    private Long taskCleanupIntervalMs;
    
    // 连接清理间隔（毫秒）
    private Long connectionCleanupIntervalMs;
    
    // 任务清理间隔（可读字符串）
    private String taskCleanupInterval;
    
    // 连接清理间隔（可读字符串）
    private String connectionCleanupInterval;
} 