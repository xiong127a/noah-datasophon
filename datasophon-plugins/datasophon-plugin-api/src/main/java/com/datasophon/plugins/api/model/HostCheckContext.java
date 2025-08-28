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

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

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
     * 额外的上下文参数
     */
    private Map<String, Object> parameters;
    
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
}