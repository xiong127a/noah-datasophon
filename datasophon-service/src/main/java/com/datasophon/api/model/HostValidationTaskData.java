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

package com.datasophon.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 主机验证任务数据模型
 * 用于db-scheduler任务的数据传递
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-28
 */
@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class HostValidationTaskData implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 集群ID
     */
    private String clusterId;
    
    /**
     * 任务ID（用于跟踪整个验证流程）
     */
    private String taskId;
    
    /**
     * 主机IP地址
     */
    private String hostIp;
    
    /**
     * SSH连接信息
     */
    private SshConnectionInfo sshInfo;
    
    /**
     * 检查项类型（ssh-check, os-info, hardware-info等）
     */
    private String checkType;
    
    /**
     * 任务优先级
     */
    @Builder.Default
    private Integer priority = 0;
    
    /**
     * 最大重试次数
     */
    @Builder.Default
    private Integer maxRetries = 3;
    
    /**
     * 当前重试次数
     */
    @Builder.Default
    private Integer currentRetry = 0;
    
    /**
     * 任务创建时间
     */
    @Builder.Default
    private LocalDateTime createTime = LocalDateTime.now();
    
    /**
     * 任务超时时间（秒）
     */
    @Builder.Default
    private Integer timeoutSeconds = 300;
    
    /**
     * 依赖的检查项（需要先完成的检查）
     */
    private List<String> dependencies;
    
    /**
     * 额外的任务参数
     */
    private Map<String, Object> parameters;
    
    /**
     * SSH连接信息内部类
     */
    @Data
    @Builder
    @Jacksonized
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SshConnectionInfo implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        /**
         * SSH用户名
         */
        private String sshUser;
        
        /**
         * SSH端口
         */
        @Builder.Default
        private Integer sshPort = 22;
        
        /**
         * SSH密码
         */
        private String sshPassword;
        
        /**
         * SSH私钥（可选）
         */
        private String privateKey;
        
        /**
         * 连接超时时间（毫秒）
         */
        @Builder.Default
        private Integer connectionTimeout = 30000;
        
        /**
         * 命令执行超时时间（毫秒）
         */
        @Builder.Default
        private Integer commandTimeout = 60000;
    }
    
    /**
     * 创建SSH检查任务数据
     */
    public static HostValidationTaskData createSshCheckTask(
            String clusterId, 
            String hostIp, 
            SshConnectionInfo sshInfo) {
        return HostValidationTaskData.builder()
                .clusterId(clusterId)
                .taskId(generateTaskId(clusterId, hostIp))
                .hostIp(hostIp)
                .sshInfo(sshInfo)
                .checkType("ssh-check")
                .priority(1) // SSH检查优先级最高
                .build();
    }
    
    /**
     * 创建操作系统信息收集任务数据
     */
    public static HostValidationTaskData createOsInfoTask(
            String clusterId, 
            String hostIp, 
            SshConnectionInfo sshInfo) {
        return HostValidationTaskData.builder()
                .clusterId(clusterId)
                .taskId(generateTaskId(clusterId, hostIp))
                .hostIp(hostIp)
                .sshInfo(sshInfo)
                .checkType("os-info")
                .priority(2)
                .dependencies(List.of("ssh-check")) // 依赖SSH检查
                .build();
    }
    
    /**
     * 创建硬件信息收集任务数据
     */
    public static HostValidationTaskData createHardwareInfoTask(
            String clusterId, 
            String hostIp, 
            SshConnectionInfo sshInfo) {
        return HostValidationTaskData.builder()
                .clusterId(clusterId)
                .taskId(generateTaskId(clusterId, hostIp))
                .hostIp(hostIp)
                .sshInfo(sshInfo)
                .checkType("hardware-info")
                .priority(3)
                .dependencies(List.of("ssh-check")) // 依赖SSH检查
                .build();
    }
    
    /**
     * 生成任务ID
     */
    private static String generateTaskId(String clusterId, String hostIp) {
        return String.format("host-validation-%s-%s-%d", clusterId, hostIp, System.currentTimeMillis());
    }
    
    /**
     * 获取任务实例ID（db-scheduler使用）
     */
    public String getTaskInstanceId() {
        return String.format("%s-%s-%s", clusterId, hostIp, checkType);
    }
    
    /**
     * 检查是否可以重试
     */
    public boolean canRetry() {
        return currentRetry < maxRetries;
    }
    
    /**
     * 增加重试次数
     */
    public void incrementRetry() {
        this.currentRetry++;
    }
}
