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

package com.datasophon.api.service.host.strategy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * PVM模式连接参数
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PvmConnectionParams {

    /**
     * 主机IP地址列表
     * 支持格式：
     * - 单个IP: 192.168.1.100
     * - 逗号分隔: 192.168.1.100,192.168.1.101  
     * - 范围批量: 192.168.1.[100-110]
     * - 换行分隔: 多行IP
     */
    @NotBlank(message = "主机IP地址列表不能为空")
    private String hosts;

    /**
     * SSH用户名
     */
    @NotBlank(message = "SSH用户名不能为空")
    private String sshUser;

    /**
     * SSH端口
     */
    @Min(value = 1, message = "SSH端口必须大于0")
    @Max(value = 65535, message = "SSH端口必须小于65536")
    private Integer sshPort;

    /**
     * SSH密码
     */
    @NotBlank(message = "SSH密码不能为空")
    private String sshPassword;

    /**
     * SSH私钥路径（可选）
     */
    private String privateKeyPath;

    /**
     * 连接超时时间（秒）
     */
    @Builder.Default
    private Integer timeoutSeconds = 30;

    /**
     * 获取SSH端口字符串形式
     */
    public String getSshPortString() {
        return sshPort != null ? sshPort.toString() : "22";
    }

    /**
     * 验证连接参数
     */
    public void validate() {
        if (hosts == null || hosts.trim().isEmpty()) {
            throw new IllegalArgumentException("主机IP地址列表不能为空");
        }
        if (sshUser == null || sshUser.trim().isEmpty()) {
            throw new IllegalArgumentException("SSH用户名不能为空");
        }
        if (sshPort == null || sshPort < 1 || sshPort > 65535) {
            throw new IllegalArgumentException("SSH端口范围应为1-65535");
        }
        if (sshPassword == null || sshPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("SSH密码不能为空");
        }
    }
}
