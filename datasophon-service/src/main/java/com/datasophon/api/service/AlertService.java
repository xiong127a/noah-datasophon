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

/**
 * 告警处理服务
 * 替代AlertActor，使用Spring Service实现
 * 负责处理集群告警消息，更新主机和服务状态，记录告警历史
 */
public interface AlertService {
    
    /**
     * 处理告警消息
     * @param alertMessage JSON格式的告警消息
     */
    void handleAlertMessage(String alertMessage);
}

