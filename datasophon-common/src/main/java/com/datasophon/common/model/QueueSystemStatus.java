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
 * 队列系统状态实体类，包含队列管理器和异步服务两部分状态
 */
@Data
public class QueueSystemStatus {
    
    // 队列管理器状态
    private QueueManagerStatus queueManager;
    
    // 异步服务状态
    private AsyncServiceStatus asyncService;
    
    // 构建方法
    public QueueSystemStatus(QueueManagerStatus queueManager, AsyncServiceStatus asyncService) {
        this.queueManager = queueManager;
        this.asyncService = asyncService;
    }
    
    // 无参构造方法
    public QueueSystemStatus() {
    }
} 