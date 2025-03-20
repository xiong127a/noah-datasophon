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
 * 队列管理器状态实体类
 * 用于封装队列管理器的状态信息
 */
@Data
public class QueueManagerStatus {
    /**
     * 队列大小
     */
    private Integer queueSize;
    
    /**
     * 成功任务数
     */
    private Long tasksSucceeded;
    
    /**
     * 失败任务数
     */
    private Long tasksFailed;
    
    /**
     * 线程池大小
     */
    private Integer poolSize;
    
    /**
     * 活动线程数
     */
    private Integer activeCount;
    
    /**
     * 已完成任务数
     */
    private Long completedTaskCount;
    
    /**
     * 任务总数
     */
    private Long taskCount;
    
    /**
     * 线程池历史最大大小
     */
    private Integer largestPoolSize;
    
    /**
     * 是否运行中
     */
    private Boolean running;
} 