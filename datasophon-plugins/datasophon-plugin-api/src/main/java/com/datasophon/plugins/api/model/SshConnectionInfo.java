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

/**
 * SSH连接信息
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-28
 */
@Data
@Builder
public class SshConnectionInfo implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * SSH用户名
     */
    private String username;
    
    /**
     * SSH密码
     */
    private String password;
    
    /**
     * SSH端口
     */
    @Builder.Default
    private int port = 22;
    
    /**
     * SSH私钥
     */
    private String privateKey;
    
    /**
     * 连接超时时间（毫秒）
     */
    @Builder.Default
    private int connectionTimeout = 30000;
    
    /**
     * 命令执行超时时间（毫秒）
     */
    @Builder.Default
    private int commandTimeout = 60000;
    
    /**
     * 检查是否有认证信息
     */
    public boolean hasAuthenticationInfo() {
        return (password != null && !password.trim().isEmpty()) ||
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
        return password != null && !password.trim().isEmpty();
    }
}
