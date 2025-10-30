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
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Worker事件基类
 * 所有SSE推送事件的父类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkerEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 事件类型
     */
    private String eventType;
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 集群ID（可选）
     */
    private Long clusterId;
    
    /**
     * 事件时间戳
     */
    private LocalDateTime timestamp;
    
    /**
     * 构造函数
     */
    public WorkerEvent(String eventType, String taskId) {
        this.eventType = eventType;
        this.taskId = taskId;
        this.timestamp = LocalDateTime.now();
    }
}

