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

package com.datasophon.api.utils;

import com.datasophon.api.service.SshPluginAdapterService;
import com.datasophon.common.model.HostInfo;
import com.datasophon.plugins.api.model.CommandResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * SSH插件工具类
 * 替代原有的MinaUtils，通过SSH插件适配器实现SSH功能
 * 
 * 设计原则：
 * 1. 保持与MinaUtils相同的接口，确保兼容性
 * 2. 内部通过SSH插件适配器调用SSH插件
 * 3. 完全隔离直接SSH库调用
 * 4. 提供更好的错误处理和日志记录
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-28
 */
@Component
@Slf4j
public class SshPluginUtils {
    
    @Autowired
    private SshPluginAdapterService sshPluginAdapter;
    
    /**
     * 打开SSH连接测试
     * 替代 MinaUtils.openConnection
     */
    public CommandResult openConnection(HostInfo hostInfo) {
        log.debug("【SSH插件工具】打开SSH连接: {}@{}:{}", 
                hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort());
        
        return sshPluginAdapter.testConnection(hostInfo);
    }
    
    /**
     * 使用密码打开SSH连接测试  
     * 替代 MinaUtils.openConnectionWithPassword
     */
    public CommandResult openConnectionWithPassword(HostInfo hostInfo) {
        log.debug("【SSH插件工具】使用密码打开SSH连接: {}@{}:{}", 
                hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort());
        
        return sshPluginAdapter.testConnection(hostInfo);
    }
    
    /**
     * 执行SSH命令并返回结果
     * 替代 MinaUtils.execCmdWithResult
     */
    public String execCmdWithResult(HostInfo hostInfo, String command) {
        log.debug("【SSH插件工具】执行SSH命令: {}@{}:{} -> {}", 
                hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort(), command);
        
        return sshPluginAdapter.executeCommand(hostInfo, command);
    }
    
    /**
     * 执行SSH命令并返回详细结果对象
     * 替代 MinaUtils.execCmdWithResultObject
     */
    public CommandResult execCmdWithResultObject(HostInfo hostInfo, String command) {
        log.debug("【SSH插件工具】执行SSH命令(详细): {}@{}:{} -> {}", 
                hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort(), command);
        
        return sshPluginAdapter.executeCommandWithResult(hostInfo, command);
    }
    
    /**
     * 执行SSH命令并返回详细结果对象（带超时）
     * 替代 MinaUtils.execCmdWithResultObject(session, command, timeout)
     */
    public CommandResult execCmdWithResultObject(HostInfo hostInfo, String command, long timeoutSeconds) {
        log.debug("【SSH插件工具】执行SSH命令(详细+超时): {}@{}:{} -> {}, 超时: {}s", 
                hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort(), command, timeoutSeconds);
        
        return sshPluginAdapter.executeCommandWithResult(hostInfo, command, timeoutSeconds);
    }
    
    /**
     * 上传文件到远程主机
     * 替代 MinaUtils.uploadFile
     */
    public boolean uploadFile(HostInfo hostInfo, String localFilePath, String remoteFilePath) {
        log.info("【SSH插件工具】上传文件: {}@{}:{} {} -> {}", 
                hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort(), localFilePath, remoteFilePath);
        
        return sshPluginAdapter.uploadFile(hostInfo, localFilePath, remoteFilePath);
    }
    
    /**
     * 从远程主机下载文件
     * 新增功能
     */
    public boolean downloadFile(HostInfo hostInfo, String remoteFilePath, String localFilePath) {
        log.info("【SSH插件工具】下载文件: {}@{}:{} {} -> {}", 
                hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort(), remoteFilePath, localFilePath);
        
        return sshPluginAdapter.downloadFile(hostInfo, remoteFilePath, localFilePath);
    }
    
    /**
     * 检测Linux发行版
     * 替代 MinaUtils.detectLinuxDistro
     */
    public String detectLinuxDistro(HostInfo hostInfo) {
        log.debug("【SSH插件工具】检测Linux发行版: {}@{}:{}", 
                hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort());
        
        return sshPluginAdapter.detectLinuxDistro(hostInfo);
    }
    
    /**
     * 根据发行版适配命令
     * 替代 MinaUtils.adaptCommandToDistro
     */
    public String adaptCommandToDistro(HostInfo hostInfo, String command) {
        log.debug("【SSH插件工具】适配命令到发行版: {}@{}:{} -> {}", 
                hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort(), command);
        
        return sshPluginAdapter.adaptCommandToDistro(hostInfo, command);
    }
    
    /**
     * 安全执行SSH命令
     * 替代 MinaUtils.safeExecCommand
     */
    public String safeExecCommand(HostInfo hostInfo, String command) {
        log.debug("【SSH插件工具】安全执行SSH命令: {}@{}:{} -> {}", 
                hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort(), command);
        
        return sshPluginAdapter.safeExecuteCommand(hostInfo, command);
    }
    
    /**
     * 为Debian系统创建systemd服务
     * 替代 MinaUtils.createSystemdServiceForDebian
     */
    public boolean createSystemdServiceForDebian(HostInfo hostInfo, String scriptPath, String installPath) {
        log.info("【SSH插件工具】创建Debian systemd服务: {}@{}:{} script={}, install={}", 
                hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort(), scriptPath, installPath);
        
        return sshPluginAdapter.createSystemdServiceForDebian(hostInfo, scriptPath, installPath);
    }
    
    /**
     * 检查SSH连接是否有效
     * 替代 MinaUtils.isSessionValid
     */
    public boolean isConnectionValid(HostInfo hostInfo) {
        log.debug("【SSH插件工具】检查SSH连接有效性: {}@{}:{}", 
                hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort());
        
        return sshPluginAdapter.isConnectionValid(hostInfo);
    }
    
    /**
     * 获取SSH连接池统计信息
     * 新增功能
     */
    public java.util.Map<String, Object> getConnectionPoolStats() {
        log.debug("【SSH插件工具】获取连接池统计信息");
        
        return sshPluginAdapter.getConnectionPoolStats();
    }
    
    /**
     * 批量执行SSH命令
     * 新增功能，提升性能
     */
    public java.util.List<CommandResult> executeBatchCommands(HostInfo hostInfo, java.util.List<String> commands) {
        log.info("【SSH插件工具】批量执行SSH命令: {}@{}:{}, 命令数量: {}", 
                hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort(), commands.size());
        
        return sshPluginAdapter.executeBatchCommands(hostInfo, commands);
    }
    
    // ================== 兼容性方法 ==================
    
    /**
     * 关闭连接（兼容性方法）
     * 由于插件化SSH连接由插件管理，这里只做日志记录
     */
    public void closeConnection(Object session) {
        log.debug("【SSH插件工具】关闭连接 - 插件化SSH连接由插件自动管理");
        // 插件化SSH连接池会自动管理连接生命周期，无需手动关闭
    }
    
    /**
     * 检查会话是否有效（兼容性方法）
     * 转换为主机连接有效性检查
     */
    public boolean isSessionValid(Object session, HostInfo hostInfo) {
        log.debug("【SSH插件工具】检查会话有效性 - 转换为连接有效性检查");
        return isConnectionValid(hostInfo);
    }
}
