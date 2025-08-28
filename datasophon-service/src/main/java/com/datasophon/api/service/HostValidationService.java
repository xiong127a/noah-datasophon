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

import com.datasophon.api.model.HostValidationTaskData;
import com.datasophon.plugins.api.model.CheckResult;

/**
 * 主机验证服务接口
 * 基于插件系统和db-scheduler的主机验证核心服务
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-28
 */
public interface HostValidationService {
    
    /**
     * 启动主机验证流程
     * 
     * @param clusterId 集群ID
     * @param hostIp 主机IP
     * @param sshInfo SSH连接信息
     */
    void startHostValidation(String clusterId, String hostIp, 
                           HostValidationTaskData.SshConnectionInfo sshInfo);
    
    /**
     * 执行SSH连接检查
     * 
     * @param taskData 任务数据
     * @return 检查结果
     * @throws InterruptedException 执行被中断
     * @throws java.util.concurrent.ExecutionException 执行异常
     * @throws java.util.concurrent.TimeoutException 执行超时
     */
    CheckResult executeSshConnectivityCheck(HostValidationTaskData taskData) 
            throws InterruptedException, java.util.concurrent.ExecutionException, java.util.concurrent.TimeoutException;
    
    /**
     * 执行操作系统信息收集
     * 
     * @param taskData 任务数据
     * @return 检查结果
     * @throws InterruptedException 执行被中断
     * @throws java.util.concurrent.ExecutionException 执行异常
     * @throws java.util.concurrent.TimeoutException 执行超时
     */
    CheckResult executeOsInfoCollection(HostValidationTaskData taskData)
            throws InterruptedException, java.util.concurrent.ExecutionException, java.util.concurrent.TimeoutException;
    
    /**
     * 执行硬件信息收集
     * 
     * @param taskData 任务数据
     * @return 检查结果
     * @throws InterruptedException 执行被中断
     * @throws java.util.concurrent.ExecutionException 执行异常
     */
    CheckResult executeHardwareInfoCollection(HostValidationTaskData taskData)
            throws InterruptedException, java.util.concurrent.ExecutionException;
    
    /**
     * 执行主机名和网络检查
     * 
     * @param taskData 任务数据
     * @return 检查结果
     */
    CheckResult executeHostnameNetworkCheck(HostValidationTaskData taskData);
    
    /**
     * 标记主机验证失败
     * 
     * @param clusterId 集群ID
     * @param hostIp 主机IP
     * @param checkType 检查类型
     * @param errorMessage 错误信息
     */
    void markHostValidationFailed(String clusterId, String hostIp, 
                                 String checkType, String errorMessage);
    
    /**
     * 获取主机验证状态
     * 
     * @param clusterId 集群ID
     * @param hostIp 主机IP
     * @return 验证状态
     */
    HostValidationStatus getHostValidationStatus(String clusterId, String hostIp);
    
    /**
     * 取消主机验证
     * 
     * @param clusterId 集群ID
     * @param hostIp 主机IP
     */
    void cancelHostValidation(String clusterId, String hostIp);
    
    /**
     * 主机验证状态枚举
     */
    enum HostValidationStatus {
        NOT_STARTED,    // 未开始
        IN_PROGRESS,    // 进行中
        COMPLETED,      // 已完成
        FAILED,         // 失败
        CANCELLED       // 已取消
    }
}
