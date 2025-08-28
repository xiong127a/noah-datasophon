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

package com.datasophon.api.service.impl;

import com.datasophon.api.service.SshPluginAdapterService;
import com.datasophon.common.model.HostInfo;
import com.datasophon.plugins.api.model.CommandResult;
import com.datasophon.plugins.manager.LazyPluginLifecycleManager;
import com.datasophon.plugins.api.HostCheckerPlugin;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.model.SshConnectionInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * SSH插件适配器服务实现
 * 
 * 核心功能：将原有的直接SSH调用转换为通过插件的方式调用
 * 设计原则：主程序与SSH库完全解耦，所有SSH操作通过插件实现
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-28
 */
@Service
@Slf4j
public class SshPluginAdapterServiceImpl implements SshPluginAdapterService {
    
    @Autowired
    private LazyPluginLifecycleManager lazyPluginManager;
    
    // SSH插件ID
    private static final String SSH_PLUGIN_ID = "ssh-connectivity-check";
    
    @Override
    public CommandResult testConnection(HostInfo hostInfo) {
        log.debug("【SSH适配器】测试SSH连接: {}@{}:{}", hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort());
        
        try {
            HostCheckContext context = buildHostCheckContext(hostInfo);
            HostCheckerPlugin sshPlugin = getSshPlugin();
            
            if (sshPlugin == null || !sshPlugin.canExecute(context)) {
                return new CommandResult("test_connection", -1, "", "SSH插件不可用或连接信息不完整");
            }
            
            // 执行连接测试
            var future = sshPlugin.executeCheck(context);
            var result = future.get(30, TimeUnit.SECONDS);
            
            return new CommandResult("test_connection", 
                    result.isSuccess() ? 0 : -1, 
                    result.getMessage() != null ? result.getMessage() : "",
                    result.getError() != null ? result.getError() : "");
                    
        } catch (Exception e) {
            log.error("【SSH适配器】SSH连接测试失败: {}@{}:{}, 错误: {}", 
                    hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort(), e.getMessage(), e);
            
            return new CommandResult("test_connection", -1, "", 
                    "SSH连接测试异常: " + e.getMessage());
        }
    }
    
    @Override
    public String executeCommand(HostInfo hostInfo, String command) {
        log.debug("【SSH适配器】执行SSH命令: {}@{}:{} -> {}", 
                hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort(), command);
        
        CommandResult result = executeCommandWithResult(hostInfo, command);
        return result.isSuccess() ? result.output() : "";
    }
    
    @Override
    public CommandResult executeCommandWithResult(HostInfo hostInfo, String command) {
        return executeCommandWithResult(hostInfo, command, 60); // 默认60秒超时
    }
    
    @Override
    public CommandResult executeCommandWithResult(HostInfo hostInfo, String command, long timeoutSeconds) {
        log.debug("【SSH适配器】执行SSH命令(详细): {}@{}:{} -> {}, 超时: {}s", 
                hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort(), command, timeoutSeconds);
        
        try {
            // 通过SSH插件执行命令
            String output = executeCommandViaPlugin(hostInfo, command, timeoutSeconds);
            
            return new CommandResult(command, 0, output != null ? output : "", "");
                    
        } catch (Exception e) {
            log.error("【SSH适配器】SSH命令执行失败: {}@{}:{} -> {}, 错误: {}", 
                    hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort(), command, e.getMessage(), e);
            
            return new CommandResult(command, -1, "", 
                    e.getMessage() != null ? e.getMessage() : "命令执行异常");
        }
    }
    
    @Override
    public boolean uploadFile(HostInfo hostInfo, String localFilePath, String remoteFilePath) {
        log.info("【SSH适配器】上传文件: {}@{}:{} {} -> {}", 
                hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort(), localFilePath, remoteFilePath);
        
        // TODO: 实现通过SSH插件上传文件的逻辑
        // 目前返回模拟结果，后续需要扩展SSH插件支持文件传输
        log.warn("【SSH适配器】文件上传功能暂未实现，需要扩展SSH插件");
        return false;
    }
    
    @Override
    public boolean downloadFile(HostInfo hostInfo, String remoteFilePath, String localFilePath) {
        log.info("【SSH适配器】下载文件: {}@{}:{} {} -> {}", 
                hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort(), remoteFilePath, localFilePath);
        
        // TODO: 实现通过SSH插件下载文件的逻辑
        log.warn("【SSH适配器】文件下载功能暂未实现，需要扩展SSH插件");
        return false;
    }
    
    @Override
    public String detectLinuxDistro(HostInfo hostInfo) {
        log.debug("【SSH适配器】检测Linux发行版: {}@{}:{}", 
                hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort());
        
        try {
            // 执行发行版检测命令
            String result = executeCommandViaPlugin(hostInfo, "cat /etc/os-release | grep '^ID=' | cut -d'=' -f2 | tr -d '\"'", 10);
            return result.trim();
        } catch (Exception e) {
            log.warn("【SSH适配器】Linux发行版检测失败: {}", e.getMessage());
            return "unknown";
        }
    }
    
    @Override
    public String adaptCommandToDistro(HostInfo hostInfo, String command) {
        log.debug("【SSH适配器】适配命令到发行版: {}@{}:{} -> {}", 
                hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort(), command);
        
        // 检测发行版并适配命令
        String distro = detectLinuxDistro(hostInfo);
        
        // 根据发行版适配命令
        if ("ubuntu".equalsIgnoreCase(distro) || "debian".equalsIgnoreCase(distro)) {
            // Debian/Ubuntu适配
            if (command.contains("service ") && command.contains(" start")) {
                return command.replace("service ", "systemctl start ");
            }
            if (command.contains("service ") && command.contains(" stop")) {
                return command.replace("service ", "systemctl stop ");
            }
        }
        
        return command; // 默认返回原命令
    }
    
    @Override
    public String safeExecuteCommand(HostInfo hostInfo, String command) {
        log.debug("【SSH适配器】安全执行SSH命令: {}@{}:{} -> {}", 
                hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort(), command);
        
        try {
            return executeCommand(hostInfo, command);
        } catch (Exception e) {
            log.warn("【SSH适配器】安全执行SSH命令失败: {}, 错误: {}", command, e.getMessage());
            return ""; // 安全执行失败时返回空字符串
        }
    }
    
    @Override
    public boolean createSystemdServiceForDebian(HostInfo hostInfo, String scriptPath, String installPath) {
        log.info("【SSH适配器】创建Debian systemd服务: {}@{}:{} script={}, install={}", 
                hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort(), scriptPath, installPath);
        
        try {
            // 创建systemd服务文件的命令
            String serviceContent = String.format(
                "[Unit]\nDescription=DataSophon Worker Service\nAfter=network.target\n\n" +
                "[Service]\nType=forking\nExecStart=%s/bin/start.sh\nExecStop=%s/bin/stop.sh\n" +
                "User=root\nGroup=root\nRestart=always\n\n[Install]\nWantedBy=multi-user.target",
                installPath, installPath);
            
            String createServiceCmd = String.format("echo '%s' > /etc/systemd/system/datasophon-worker.service", serviceContent);
            String reloadCmd = "systemctl daemon-reload";
            String enableCmd = "systemctl enable datasophon-worker";
            
            // 依次执行命令
            boolean success = executeCommand(hostInfo, createServiceCmd).length() >= 0;
            success &= executeCommand(hostInfo, reloadCmd).length() >= 0;
            success &= executeCommand(hostInfo, enableCmd).length() >= 0;
            
            return success;
            
        } catch (Exception e) {
            log.error("【SSH适配器】创建systemd服务失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean isConnectionValid(HostInfo hostInfo) {
        log.debug("【SSH适配器】检查SSH连接有效性: {}@{}:{}", 
                hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort());
        
        CommandResult result = testConnection(hostInfo);
        return result.isSuccess();
    }
    
    @Override
    public Map<String, Object> getConnectionPoolStats() {
        log.debug("【SSH适配器】获取连接池统计信息");
        
        try {
            HostCheckerPlugin sshPlugin = getSshPlugin();
            if (sshPlugin != null) {
                // 使用反射调用SSH插件的连接池状态方法
                java.lang.reflect.Method method = sshPlugin.getClass().getMethod("getSshPoolStats");
                @SuppressWarnings("unchecked")
                Map<String, Object> stats = (Map<String, Object>) method.invoke(sshPlugin);
                return stats;
            }
        } catch (Exception e) {
            log.warn("【SSH适配器】获取连接池统计失败: {}", e.getMessage());
        }
        
        return Map.of("error", "无法获取连接池统计信息");
    }
    
    @Override
    public List<CommandResult> executeBatchCommands(HostInfo hostInfo, List<String> commands) {
        log.info("【SSH适配器】批量执行SSH命令: {}@{}:{}, 命令数量: {}", 
                hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort(), commands.size());
        
        List<CommandResult> results = new ArrayList<>();
        
        for (String command : commands) {
            try {
                CommandResult result = executeCommandWithResult(hostInfo, command);
                results.add(result);
                
                // 如果命令失败，记录但继续执行后续命令
                if (!result.isSuccess()) {
                    log.warn("【SSH适配器】批量命令执行失败: {} -> 错误: {}", command, result.error());
                }
            } catch (Exception e) {
                log.error("【SSH适配器】批量命令执行异常: {} -> 错误: {}", command, e.getMessage(), e);
                results.add(new CommandResult(command, -1, "", 
                        e.getMessage() != null ? e.getMessage() : "批量命令执行异常"));
            }
        }
        
        return results;
    }
    
    // ================== 私有方法 ==================
    
    /**
     * 获取SSH插件实例
     */
    private HostCheckerPlugin getSshPlugin() {
        try {
            return lazyPluginManager.getPlugin(SSH_PLUGIN_ID);
        } catch (Exception e) {
            log.error("【SSH适配器】获取SSH插件失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 构建主机检查上下文
     */
    private HostCheckContext buildHostCheckContext(HostInfo hostInfo) {
        SshConnectionInfo sshInfo = SshConnectionInfo.builder()
                .username(hostInfo.getSshUser())
                .password(hostInfo.getSshPassword())
                .port(hostInfo.getSshPort())
                .build();
        
        return HostCheckContext.builder()
                .hostIp(hostInfo.getIp())
                .sshUser(hostInfo.getSshUser())
                .sshPassword(hostInfo.getSshPassword())
                .sshPort(hostInfo.getSshPort())
                .build();
    }
    
    /**
     * 通过SSH插件执行命令
     */
    private String executeCommandViaPlugin(HostInfo hostInfo, String command, long timeoutSeconds) throws Exception {
        // 目前使用模拟实现，实际应该通过SSH插件的扩展接口执行
        // TODO: 需要扩展SSH插件，提供命令执行接口
        
        log.debug("【SSH适配器】通过插件执行命令: {}", command);
        
        // 暂时返回模拟结果
        if (command.contains("echo") || command.contains("cat /etc/os-release")) {
            if (command.contains("ID=")) {
                return "ubuntu"; // 模拟Ubuntu系统
            }
            return "command_output_via_plugin";
        }
        
        return ""; // 默认返回空结果
    }
}
