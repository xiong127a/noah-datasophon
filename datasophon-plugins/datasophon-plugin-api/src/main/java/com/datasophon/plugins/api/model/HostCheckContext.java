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

package com.datasophon.plugins.api.model;

import com.datasophon.common.enums.OsType;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 主机检查上下文
 * 包含插件执行所需的所有信息
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-28
 */
@Data
@Builder
public class HostCheckContext implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 主机IP地址
     */
    private String hostIp;
    
    /**
     * 集群ID
     */
    private String clusterId;
    
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
     * SSH私钥
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
    
    /**
     * 操作系统类型（可选，运行时检测）
     */
    private OsType osType;
    
    /**
     * 主机名（可选，运行时获取）
     */
    private String hostname;
    
    /**
     * SSH私钥文件路径（用于免密连接）
     */
    private String privateKeyPath;
    
    /**
     * 重试次数
     */
    @Builder.Default
    private Integer retryCount = 3;
    
    /**
     * 重试间隔（毫秒）
     */
    @Builder.Default
    private Integer retryInterval = 5000;
    
    /**
     * 创建时间
     */
    @Builder.Default
    private LocalDateTime createTime = LocalDateTime.now();
    
    /**
     * 最后更新时间
     */
    @Builder.Default
    private LocalDateTime lastUpdateTime = LocalDateTime.now();
    
    /**
     * 是否启用详细日志
     */
    @Builder.Default
    private Boolean verboseLogging = false;
    
    /**
     * 执行状态
     */
    @Builder.Default
    private ContextStatus status = ContextStatus.CREATED;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 额外的上下文参数
     */
    @Builder.Default
    private Map<String, Object> parameters = new ConcurrentHashMap<>();
    
    /**
     * 检查是否有SSH密码或私钥
     */
    public boolean hasAuthenticationInfo() {
        return (sshPassword != null && !sshPassword.trim().isEmpty()) ||
               (privateKey != null && !privateKey.trim().isEmpty());
    }
    
    /**
     * 检查是否使用私钥认证
     */
    public boolean usePrivateKeyAuth() {
        return privateKey != null && !privateKey.trim().isEmpty();
    }
    
    /**
     * 检查是否使用密码认证
     */
    public boolean usePasswordAuth() {
        return sshPassword != null && !sshPassword.trim().isEmpty();
    }
    
    /**
     * 获取参数值
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, Class<T> type) {
        if (parameters == null) {
            return null;
        }
        Object value = parameters.get(key);
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * 获取参数值（带默认值）
     */
    public <T> T getParameter(String key, Class<T> type, T defaultValue) {
        T value = getParameter(key, type);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 获取SSH连接信息
     */
    public SshConnectionInfo getSshConnectionInfo() {
        return SshConnectionInfo.builder()
                .username(this.sshUser)
                .password(this.sshPassword)
                .port(this.sshPort != null ? this.sshPort : 22)
                .privateKey(this.privateKey)
                .connectionTimeout(this.connectionTimeout != null ? this.connectionTimeout : 30000)
                .commandTimeout(this.commandTimeout != null ? this.commandTimeout : 60000)
                .build();
    }
    
    /**
     * 更新状态
     */
    public void updateStatus(ContextStatus newStatus) {
        this.status = newStatus;
        this.lastUpdateTime = LocalDateTime.now();
    }
    
    /**
     * 更新状态和错误信息
     */
    public void updateStatus(ContextStatus newStatus, String errorMessage) {
        this.status = newStatus;
        this.errorMessage = errorMessage;
        this.lastUpdateTime = LocalDateTime.now();
    }
    
    /**
     * 设置操作系统信息
     */
    public void setOsInfo(OsType osType, String hostname) {
        this.osType = osType;
        this.hostname = hostname;
        this.lastUpdateTime = LocalDateTime.now();
    }
    
    /**
     * 添加参数
     */
    public void setParameter(String key, Object value) {
        if (parameters == null) {
            parameters = new ConcurrentHashMap<>();
        }
        parameters.put(key, value);
        this.lastUpdateTime = LocalDateTime.now();
    }
    
    /**
     * 检查是否可以重试
     */
    public boolean canRetry() {
        return retryCount != null && retryCount > 0;
    }
    
    /**
     * 减少重试次数
     */
    public void decrementRetry() {
        if (retryCount != null && retryCount > 0) {
            retryCount--;
        }
    }
    
    /**
     * 获取连接显示名称
     */
    public String getConnectionDisplayName() {
        if (hostname != null && !hostname.isEmpty()) {
            return String.format("%s (%s)", hostname, hostIp);
        }
        return hostIp;
    }
    
    /**
     * 验证上下文信息
     */
    public boolean isValid() {
        return hostIp != null && !hostIp.trim().isEmpty() &&
               sshUser != null && !sshUser.trim().isEmpty() &&
               hasAuthenticationInfo();
    }
    
    /**
     * 获取认证类型
     */
    public String getAuthType() {
        if (usePrivateKeyAuth()) {
            return "私钥认证";
        } else if (usePasswordAuth()) {
            return "密码认证";
        }
        return "无认证信息";
    }
    
    /**
     * 上下文状态枚举
     */
    public enum ContextStatus {
        CREATED("已创建"),
        CONNECTING("连接中"),
        CONNECTED("已连接"),
        EXECUTING("执行中"),
        COMPLETED("已完成"),
        FAILED("失败"),
        TIMEOUT("超时"),
        CANCELLED("已取消");
        
        private final String description;
        
        ContextStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}