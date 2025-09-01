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
import com.datasophon.plugins.api.PluginId;
import com.datasophon.plugins.api.model.CommandResult;
import com.datasophon.plugins.manager.LazyPluginLifecycleManager;
import com.datasophon.plugins.api.HostCheckerPlugin;
import com.datasophon.plugins.api.model.HostCheckContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
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
    private static final PluginId SSH_PLUGIN_ID = PluginId.SSH_CONNECTOR;
    
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
        
        try {
            // 通过SSH插件执行文件上传
            HostCheckerPlugin sshPlugin = getSshPlugin();
            if (sshPlugin == null) {
                throw new RuntimeException("SSH插件不可用");
            }
            
            // 使用反射调用SSH插件的文件上传方法
            java.lang.reflect.Method method = sshPlugin.getClass().getMethod("uploadFile", 
                    String.class, int.class, String.class, String.class, String.class, String.class);
            
            Boolean result = (Boolean) method.invoke(sshPlugin, 
                    hostInfo.getIp(),
                    hostInfo.getSshPort(), 
                    hostInfo.getSshUser(),
                    hostInfo.getSshPassword(),
                    localFilePath,
                    remoteFilePath);
            
            log.info("【SSH适配器】文件上传{}：{} -> {}", 
                    result ? "成功" : "失败", localFilePath, remoteFilePath);
            return result != null && result;
            
        } catch (Exception e) {
            log.error("【SSH适配器】文件上传失败: {} -> {}, 错误: {}", 
                    localFilePath, remoteFilePath, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean uploadFile(HostInfo hostInfo, java.io.InputStream inputStream, String remoteFilePath) {
        log.info("【SSH适配器】上传文件流: {}@{}:{} -> {}", 
                hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort(), remoteFilePath);
        
        try {
            // 通过SSH插件执行文件流上传
            HostCheckerPlugin sshPlugin = getSshPlugin();
            if (sshPlugin == null) {
                throw new RuntimeException("SSH插件不可用");
            }
            
            // 使用反射调用SSH插件的文件流上传方法
            java.lang.reflect.Method method = sshPlugin.getClass().getMethod("uploadFileFromStream", 
                    String.class, int.class, String.class, String.class, java.io.InputStream.class, String.class);
            
            Boolean result = (Boolean) method.invoke(sshPlugin, 
                    hostInfo.getIp(),
                    hostInfo.getSshPort(), 
                    hostInfo.getSshUser(),
                    hostInfo.getSshPassword(),
                    inputStream,
                    remoteFilePath);
            
            log.info("【SSH适配器】文件流上传{}：{}", 
                    result ? "成功" : "失败", remoteFilePath);
            return result != null && result;
            
        } catch (Exception e) {
            log.error("【SSH适配器】文件流上传失败: {}, 错误: {}", 
                    remoteFilePath, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean downloadFile(HostInfo hostInfo, String remoteFilePath, String localFilePath) {
        log.info("【SSH适配器】下载文件: {}@{}:{} {} -> {}", 
                hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort(), remoteFilePath, localFilePath);
        
        try {
            // 通过SSH插件执行文件下载
            HostCheckerPlugin sshPlugin = getSshPlugin();
            if (sshPlugin == null) {
                throw new RuntimeException("SSH插件不可用");
            }
            
            // 使用反射调用SSH插件的文件下载方法
            java.lang.reflect.Method method = sshPlugin.getClass().getMethod("downloadFile", 
                    String.class, int.class, String.class, String.class, String.class, String.class);
            
            Boolean result = (Boolean) method.invoke(sshPlugin, 
                    hostInfo.getIp(),
                    hostInfo.getSshPort(), 
                    hostInfo.getSshUser(),
                    hostInfo.getSshPassword(),
                    remoteFilePath,
                    localFilePath);
            
            log.info("【SSH适配器】文件下载{}：{} -> {}", 
                    result ? "成功" : "失败", remoteFilePath, localFilePath);
            return result != null && result;
            
        } catch (Exception e) {
            log.error("【SSH适配器】文件下载失败: {} -> {}, 错误: {}", 
                    remoteFilePath, localFilePath, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean createDirectory(HostInfo hostInfo, String remotePath) {
        log.info("【SSH适配器】创建目录: {}@{}:{} -> {}", 
                hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort(), remotePath);
        
        try {
            // 通过SSH插件执行目录创建
            HostCheckerPlugin sshPlugin = getSshPlugin();
            if (sshPlugin == null) {
                throw new RuntimeException("SSH插件不可用");
            }
            
            // 使用反射调用SSH插件的目录创建方法
            java.lang.reflect.Method method = sshPlugin.getClass().getMethod("createDirectory", 
                    String.class, int.class, String.class, String.class, String.class);
            
            Boolean result = (Boolean) method.invoke(sshPlugin, 
                    hostInfo.getIp(),
                    hostInfo.getSshPort(), 
                    hostInfo.getSshUser(),
                    hostInfo.getSshPassword(),
                    remotePath);
            
            log.info("【SSH适配器】目录创建{}：{}", 
                    result ? "成功" : "失败", remotePath);
            return result != null && result;
            
        } catch (Exception e) {
            log.error("【SSH适配器】目录创建失败: {}, 错误: {}", 
                    remotePath, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean deleteFile(HostInfo hostInfo, String remoteFilePath) {
        log.info("【SSH适配器】删除文件: {}@{}:{} -> {}", 
                hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort(), remoteFilePath);
        
        try {
            // 通过SSH插件执行文件删除
            HostCheckerPlugin sshPlugin = getSshPlugin();
            if (sshPlugin == null) {
                throw new RuntimeException("SSH插件不可用");
            }
            
            // 使用反射调用SSH插件的文件删除方法
            java.lang.reflect.Method method = sshPlugin.getClass().getMethod("deleteFile", 
                    String.class, int.class, String.class, String.class, String.class);
            
            Boolean result = (Boolean) method.invoke(sshPlugin, 
                    hostInfo.getIp(),
                    hostInfo.getSshPort(), 
                    hostInfo.getSshUser(),
                    hostInfo.getSshPassword(),
                    remoteFilePath);
            
            log.info("【SSH适配器】文件删除{}：{}", 
                    result ? "成功" : "失败", remoteFilePath);
            return result != null && result;
            
        } catch (Exception e) {
            log.error("【SSH适配器】文件删除失败: {}, 错误: {}", 
                    remoteFilePath, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean checkPathExists(HostInfo hostInfo, String remotePath) {
        log.debug("【SSH适配器】检查路径存在: {}@{}:{} -> {}", 
                hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort(), remotePath);
        
        try {
            // 通过SSH插件执行路径检查
            HostCheckerPlugin sshPlugin = getSshPlugin();
            if (sshPlugin == null) {
                throw new RuntimeException("SSH插件不可用");
            }
            
            // 使用反射调用SSH插件的路径检查方法
            java.lang.reflect.Method method = sshPlugin.getClass().getMethod("checkPathExists", 
                    String.class, int.class, String.class, String.class, String.class);
            
            Boolean result = (Boolean) method.invoke(sshPlugin, 
                    hostInfo.getIp(),
                    hostInfo.getSshPort(), 
                    hostInfo.getSshUser(),
                    hostInfo.getSshPassword(),
                    remotePath);
            
            log.debug("【SSH适配器】路径{}存在：{}", remotePath, result);
            return result != null && result;
            
        } catch (Exception e) {
            log.error("【SSH适配器】路径检查失败: {}, 错误: {}", 
                    remotePath, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean createFile(HostInfo hostInfo, String remoteFilePath) {
        log.info("【SSH适配器】创建文件: {}@{}:{} -> {}", 
                hostInfo.getSshUser(), hostInfo.getIp(), hostInfo.getSshPort(), remoteFilePath);
        
        try {
            // 通过SSH插件执行文件创建
            HostCheckerPlugin sshPlugin = getSshPlugin();
            if (sshPlugin == null) {
                throw new RuntimeException("SSH插件不可用");
            }
            
            // 使用反射调用SSH插件的文件创建方法
            java.lang.reflect.Method method = sshPlugin.getClass().getMethod("createFile", 
                    String.class, int.class, String.class, String.class, String.class);
            
            Boolean result = (Boolean) method.invoke(sshPlugin, 
                    hostInfo.getIp(),
                    hostInfo.getSshPort(), 
                    hostInfo.getSshUser(),
                    hostInfo.getSshPassword(),
                    remoteFilePath);
            
            log.info("【SSH适配器】文件创建{}：{}", 
                    result ? "成功" : "失败", remoteFilePath);
            return result != null && result;
            
        } catch (Exception e) {
            log.error("【SSH适配器】文件创建失败: {}, 错误: {}", 
                    remoteFilePath, e.getMessage(), e);
            return false;
        }
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
                    """
                            [Unit]
                            Description=DataSophon Worker Service
                            After=network.target
                            
                            [Service]
                            Type=forking
                            ExecStart=%s/bin/start.sh
                            ExecStop=%s/bin/stop.sh
                            User=root
                            Group=root
                            Restart=always
                            
                            [Install]
                            WantedBy=multi-user.target""",
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
            return lazyPluginManager.getPlugin(SSH_PLUGIN_ID.getId());
        } catch (Exception e) {
            log.error("【SSH适配器】获取SSH插件失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 构建主机检查上下文
     */
    private HostCheckContext buildHostCheckContext(HostInfo hostInfo) {
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
        log.debug("【SSH适配器】通过插件执行命令: {} -> {}", hostInfo.getIp(), command);
        
        try {
            // 获取SSH插件实例
            HostCheckerPlugin sshPlugin = getSshPlugin();
            if (sshPlugin == null) {
                throw new RuntimeException("SSH插件不可用");
            }
            
            // 使用反射调用SSH插件的命令执行方法
            // SSH插件内部的 SshConnectionPoolManager 有 executeCommand 方法
            java.lang.reflect.Method method = sshPlugin.getClass().getMethod("executeCommandDirectly", 
                    String.class, int.class, String.class, String.class, String.class);
            
            String result = (String) method.invoke(sshPlugin, 
                    hostInfo.getIp(),
                    hostInfo.getSshPort(), 
                    hostInfo.getSshUser(),
                    hostInfo.getSshPassword(),
                    command);
            
            log.debug("【SSH适配器】SSH命令执行完成: {} -> {}", command, result != null ? result.length() + " chars" : "null");
            return result != null ? result : "";
            
        } catch (java.lang.NoSuchMethodException e) {
            // 如果SSH插件没有直接执行命令的方法，回退到连接测试方式
            log.warn("【SSH适配器】SSH插件缺少直接命令执行方法，使用备用方案");
            return executeCommandFallback(hostInfo, command);
            
        } catch (Exception e) {
            log.error("【SSH适配器】通过插件执行命令失败: {} -> {}, 错误: {}", 
                    hostInfo.getIp(), command, e.getMessage(), e);
            throw new RuntimeException("SSH插件命令执行失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 命令执行备用方案
     * 通过修改测试命令的方式来执行任意命令
     */
    private String executeCommandFallback(HostInfo hostInfo, String command) throws Exception {
        log.debug("【SSH适配器】使用备用方案执行命令: {}", command);
        
        // 构建上下文，但将测试命令替换为要执行的命令
        HostCheckContext context = buildHostCheckContextWithCommand(hostInfo, command);
        HostCheckerPlugin sshPlugin = getSshPlugin();
        
        if (sshPlugin == null || !sshPlugin.canExecute(context)) {
            throw new RuntimeException("SSH插件不可用或连接信息不完整");
        }
        
        // 执行检查（实际执行我们的命令）
        var future = sshPlugin.executeCheck(context);
        var result = future.get(30, TimeUnit.SECONDS); // 备用方案使用30秒固定超时
        
        if (result.isSuccess()) {
            // 从检查结果中提取命令输出
            Map<String, Object> data = result.getData();
            if (data != null && data.containsKey("command_output")) {
                Object output = data.get("command_output");
                return output != null ? output.toString() : "";
            }
            return result.getMessage() != null ? result.getMessage() : "";
        } else {
            throw new RuntimeException("命令执行失败: " + result.getError());
        }
    }
    
    /**
     * 构建自定义命令的主机检查上下文
     */
    private HostCheckContext buildHostCheckContextWithCommand(HostInfo hostInfo, String command) {
        return HostCheckContext.builder()
                .hostIp(hostInfo.getIp())
                .sshUser(hostInfo.getSshUser())
                .sshPassword(hostInfo.getSshPassword())
                .sshPort(hostInfo.getSshPort())
                .parameters(Map.of("customCommand", command))  // 通过parameters传入自定义命令
                .build();
    }
}
