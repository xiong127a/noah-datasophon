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

package com.datasophon.api.service.checker;

import com.datasophon.common.enums.ScopeCode;
import com.datasophon.common.utils.Result;

/**
 * 队列管理器服务接口
 * 负责处理与队列管理相关的业务逻辑
 */
public interface QueueManagerService {

    /**
     * 获取队列系统状态
     * @return 包含队列管理器和异步服务状态的结果
     */
    Result getQueueSystemStatus();
    
    /**
     * 暂停队列处理/定时任务
     * @param scope 作用范围: all(所有), queue(仅队列), scheduler(仅定时任务)
     * @return 操作结果
     */
    Result pauseQueueSystem(String scope);
    
    /**
     * 恢复队列处理/定时任务
     * @param scope 作用范围: all(所有), queue(仅队列), scheduler(仅定时任务)
     * @return 操作结果
     */
    Result resumeQueueSystem(String scope);
    
    /**
     * 关闭队列系统
     * @return 操作结果
     */
    Result shutdownQueueSystem();
    
    /**
     * 处理队列管理器请求
     * @param action 操作类型: status(获取状态), pause(暂停), resume(恢复), shutdown(关闭)
     * @param scopeCode 作用范围: all(所有), queue(仅队列), scheduler(仅定时任务)，默认为all
     * @return 操作结果
     */
    Result manageQueueSystem(String action, String scopeCode);
    
    /**
     * 暂停指定的定时任务
     * @param taskId 任务ID
     * @return 操作结果
     */
    Result pauseScheduledTask(String taskId);
    
    /**
     * 恢复指定的定时任务
     * @param taskId 任务ID
     * @return 操作结果
     */
    Result resumeScheduledTask(String taskId);
    
    /**
     * 清理不活跃的SSH连接
     * @return 操作结果，包含清理的连接数
     */
    Result cleanupConnections();
    
    /**
     * 获取检查任务队列详情
     * @return 任务队列中的详细任务信息
     */
    Result getCheckQueueTasks();
    
    /**
     * 获取修复任务队列详情
     * @return 修复任务队列中的详细任务信息
     */
    Result getFixQueueTasks();
} 