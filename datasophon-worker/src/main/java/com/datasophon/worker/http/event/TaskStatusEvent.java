/*
 *
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
 *
 */

package com.datasophon.worker.http.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 任务状态事件
 * 用于推送任务状态变化
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class TaskStatusEvent extends WorkerEvent {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 任务状态：PENDING, RUNNING, COMPLETED, FAILED
     */
    private String status;
    
    /**
     * 进度百分比 (0-100)
     */
    private Integer progress;
    
    /**
     * 状态描述信息
     */
    private String message;
    
    public TaskStatusEvent(String taskId, String status, Integer progress, String message) {
        super("task-status", taskId);
        this.status = status;
        this.progress = progress;
        this.message = message;
    }
    
    public TaskStatusEvent(String taskId, String status, String message) {
        this(taskId, status, null, message);
    }
}

