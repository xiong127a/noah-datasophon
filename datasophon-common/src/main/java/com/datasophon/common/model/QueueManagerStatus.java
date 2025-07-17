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
import java.util.List;

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
    private List<String> queueNames;

    // 线程池信息
    private int mainExecutorActiveCount;
    private int mainExecutorQueueSize;
    private int itemExecutorActiveCount;
    private int itemExecutorQueueSize;
    private int fixExecutorActiveCount;
    private int fixExecutorQueueSize;

    // 主机信息收集线程池状态
    private int osInfoExecutorActiveCount;
    private int osInfoExecutorQueueSize;
    private long osInfoExecutorCompletedTasks;

    // 硬件信息收集线程池状态
    private int hardwareInfoExecutorActiveCount;
    private int hardwareInfoExecutorQueueSize;
    private long hardwareInfoExecutorCompletedTasks;

    // hosts文件设置线程池状态
    private int hostsFileExecutorActiveCount;
    private int hostsFileExecutorQueueSize;
    private long hostsFileExecutorCompletedTasks;

    // 主机名设置线程池状态
    private int hostnameExecutorActiveCount;
    private int hostnameExecutorQueueSize;
    private long hostnameExecutorCompletedTasks;

    // 线程池总统计
    private int totalActiveThreads;
    private int totalPoolSize;
    private long totalCompletedTasks;
    private int totalQueuedTasks;

    // 系统运行时间
    private String systemStartTime; // 系统启动时间
    private long systemUptimeMs; // 系统运行时间(毫秒)
    private String systemUptime; // 格式化的系统运行时间

    // 统计信息
    private long tasksProcessed;
    private long tasksSucceeded;
    private long tasksFailed;
    private long fixTasksProcessed;
    private long fixTasksSucceeded;
    private long fixTasksFailed;

    // 任务执行时间统计
    private long fixTasksAvgExecutionTimeMs;
    private long fixTasksMaxExecutionTimeMs;
    private String fixTasksAvgExecutionTime;
    private String fixTasksMaxExecutionTime;

    private long tasksAvgExecutionTimeMs;
    private long tasksMaxExecutionTimeMs;
    private String tasksAvgExecutionTime;
    private String tasksMaxExecutionTime;

    // 定时任务状态
    private boolean queueHealthMonitorActive;
    private boolean taskTimeoutMonitorActive;

    // 定时任务间隔（毫秒）
    private long queueHealthMonitorIntervalMs;
    private long taskTimeoutMonitorIntervalMs;

    // 定时任务间隔（可读格式）
    private String queueHealthMonitorInterval;
    private String taskTimeoutMonitorInterval;

    // 上次执行时间
    private String lastQueueHealthMonitorTime;
    private String lastTaskTimeoutMonitorTime;

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