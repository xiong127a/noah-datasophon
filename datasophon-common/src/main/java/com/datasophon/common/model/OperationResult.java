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
 * 操作结果实体类
 * 用于封装队列管理器等操作的结果信息和状态
 */
@Data
public class OperationResult {
    
    /**
     * 操作消息
     */
    private String message;
    
    /**
     * 操作后的系统状态
     */
    private QueueSystemStatus status;
    
    /**
     * 是否成功
     */
    private boolean success = true;
    
    /**
     * 操作代码
     */
    private String code;
    
    /**
     * 操作时间戳
     */
    private long timestamp = System.currentTimeMillis();
} 