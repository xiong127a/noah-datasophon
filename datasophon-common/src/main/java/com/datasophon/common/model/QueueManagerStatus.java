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
 * 队列管理器状态
 * 包含所有队列、执行器和定时任务的状态信息
 */
@Data
public class QueueManagerStatus {
    // 基本状态
    private boolean running;
    private boolean scheduledTasksEnabled;
    private boolean queueEmpty;
    private boolean fixQueueEmpty;
    
    // 队列详情
    private int queueSize;
    private int fixQueueSize;
    private int runningTasksCount;
    private int runningFixTasksCount;
    
    // 线程池信息
    private int mainExecutorActiveCount;
    private int mainExecutorQueueSize;
    private int itemExecutorActiveCount;
    private int itemExecutorQueueSize;
    private int fixExecutorActiveCount;
    private int fixExecutorQueueSize;
    
    // 统计信息
    private long tasksProcessed;
    private long tasksSucceeded;
    private long tasksFailed;
    
    // 定时任务状态
    private boolean queueHealthMonitorActive;
    private boolean taskTimeoutMonitorActive;
    
    // 定时任务间隔（毫秒）
    private long queueHealthMonitorIntervalMs;
    private long taskTimeoutMonitorIntervalMs;
    
    // 定时任务间隔（可读格式）
    private String queueHealthMonitorInterval;
    private String taskTimeoutMonitorInterval;
    
    // 处理线程状态
    private boolean queueProcessorThreadAlive;
    private boolean fixQueueProcessorThreadAlive;
    private String queueProcessorStartTime;
    private String fixQueueProcessorStartTime;
    
    // SSH连接池状态
    private int connectionPoolSize;
    private String lastConnectionCleanupTime;
    private long connectionCleanupIntervalMs;
    private String connectionCleanupInterval;
    
    // 任务清理状态
    private String lastTaskCleanupTime;
    private long taskCleanupIntervalMs;
    private String taskCleanupInterval;
} 