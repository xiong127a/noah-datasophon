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

package com.datasophon.api.service;

import com.datasophon.common.model.ServiceExecuteResultMessage;

/**
 * 服务执行结果处理服务
 * 替代ServiceExecuteResultActor，使用Spring Service实现
 * 负责处理服务执行结果，管理DAG任务流程和状态转换
 */
public interface ServiceExecuteResultService {
    
    /**
     * 处理服务执行结果
     * 根据执行结果更新DAG任务状态，提交下一批可执行任务
     */
    void handleServiceExecuteResult(ServiceExecuteResultMessage result);
}

