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

package com.datasophon.plugins.ssh;

import com.datasophon.common.enums.CheckType;
import com.datasophon.common.enums.OsType;
import com.datasophon.plugins.api.HostCheckerPlugin;
import com.datasophon.plugins.api.PluginId;
import com.datasophon.plugins.api.model.CheckResult;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.model.PluginMetadata;
import com.datasophon.plugins.ssh.service.SshConnectionPoolManager;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * SSH连接检查插件
 * 
 * 核心功能：
 * 1. 验证SSH连接性和基本命令执行能力
 * 2. 内部完全管理SSH连接池，使用Apache SSHJ + Commons Pool2
 * 3. 主程序只通过插件接口交互，不直接操作SSH
 * 4. 提供连接池状态监控接口
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-28
 */
@Extension
@Slf4j
public class SshConnectivityCheckPlugin implements HostCheckerPlugin {
    
    // SSH连接池管理器（插件内部组件）
    private SshConnectionPoolManager sshPoolManager;
    
    // 插件元数据
    private static final PluginMetadata METADATA = PluginMetadata.builder()
            .pluginId(PluginId.SSH_CONNECTOR.getId())
            .name(PluginId.SSH_CONNECTOR.getDisplayName())
            .description("验证主机SSH连接性，使用Apache SSHJ + Commons Pool2实现高性能连接池")
            .version("1.0.0")
            .author("任相鹏")
            .build();
    
    @Override
    public void initialize() {
        try {
            log.info("【SSH插件】初始化SSH连接检查插件...");
            
            // 初始化SSH连接池管理器
            sshPoolManager = new SshConnectionPoolManager();
            sshPoolManager.init();
            
            log.info("【SSH插件】SSH连接检查插件初始化完成 - 使用Apache SSHJ + Commons Pool2");
        } catch (Exception e) {
            log.error("【SSH插件】初始化失败", e);
            throw new RuntimeException("SSH插件初始化失败", e);
        }
    }
    
    @Override
    public void cleanup() {
        try {
            log.info("【SSH插件】清理SSH连接检查插件...");
            
            if (sshPoolManager != null) {
                sshPoolManager.destroy();
            }
            
            log.info("【SSH插件】SSH连接检查插件清理完成");
        } catch (Exception e) {
            log.error("【SSH插件】清理失败", e);
        }
    }
    
    @Override
    public Set<OsType> getSupportedOperatingSystems() {
        // 支持的Linux操作系统SSH连接
        return Set.of(
            OsType.CENTOS,
            OsType.RHEL,
            OsType.UBUNTU,
            OsType.DEBIAN,
            OsType.KYLIN_V10,
            OsType.KYLIN_V4
        );
    }
    
    @Override
    public int getPriority() {
        // 最高优先级，SSH连接是所有检查的基础
        return 1;
    }
    
    @Override
    public CompletableFuture<CheckResult> executeCheck(HostCheckContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("【SSH插件】开始SSH连接检查: {}", context.getHostIp());
                
                String hostIp = context.getHostIp();
                int port = context.getSshConnectionInfo().getPort();
                String user = context.getSshConnectionInfo().getUsername();
                String password = context.getSshConnectionInfo().getPassword();
                
                // 执行SSH连接测试
                return performSshConnectivityTest(hostIp, port, user, password);
                
            } catch (Exception e) {
                log.error("【SSH插件】SSH连接检查失败: {}, 错误: {}", 
                        context.getHostIp(), e.getMessage(), e);
                
                CheckResult result = CheckResult.builder()
                        .checkType(CheckType.SSH_CONNECTIVITY)
                        .success(false)
                        .message("SSH连接检查失败: " + e.getMessage())
                        .error(e.getMessage())
                        .checkTime(LocalDateTime.now())
                        .build();
                result.data("error_type", e.getClass().getSimpleName());
                return result;
            }
        });
    }
    
    @Override
    public boolean canExecute(HostCheckContext context) {
        // 检查SSH连接信息是否完整
        if (context.getSshConnectionInfo() == null) {
            log.warn("【SSH插件】缺少SSH连接信息: {}", context.getHostIp());
            return false;
        }
        
        String hostIp = context.getHostIp();
        String username = context.getSshConnectionInfo().getUsername();
        String password = context.getSshConnectionInfo().getPassword();
        
        boolean canExecute = hostIp != null && !hostIp.trim().isEmpty() &&
                           username != null && !username.trim().isEmpty() &&
                           password != null && !password.trim().isEmpty();
        
        if (!canExecute) {
            log.warn("【SSH插件】SSH连接信息不完整: hostIp={}, username={}, password={}",
                    hostIp, username, password != null ? "***" : "null");
        }
        
        return canExecute;
    }
    
    @Override
    public PluginMetadata getMetadata() {
        return METADATA;
    }
    
    @Override
    public boolean isHealthy() {
        try {
            return sshPoolManager != null;
        } catch (Exception e) {
            log.warn("【SSH插件】健康检查失败", e);
            return false;
        }
    }
    
    @Override
    public String getPluginId() {
        return PluginId.SSH_CONNECTOR.getId();
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    /**
     * 直接执行SSH命令（适配器专用接口）
     * 
     * @param hostIp 主机IP
     * @param port SSH端口
     * @param username 用户名
     * @param password 密码
     * @param command 要执行的命令
     * @return 命令输出
     * @throws Exception 执行失败
     */
    public String executeCommandDirectly(String hostIp, int port, String username, String password, String command) throws Exception {
        log.debug("【SSH插件】直接执行命令: {}@{}:{} -> {}", username, hostIp, port, command);
        
        if (sshPoolManager == null) {
            throw new RuntimeException("SSH连接池管理器未初始化");
        }
        
        try {
            // 使用连接池管理器执行命令
            String result = sshPoolManager.executeCommand(hostIp, port, username, password, command);
            
            log.debug("【SSH插件】命令执行完成: {} -> {} chars", command, 
                    result != null ? result.length() : 0);
            
            return result != null ? result : "";
            
        } catch (Exception e) {
            log.error("【SSH插件】直接命令执行失败: {}@{}:{} -> {}, 错误: {}", 
                    username, hostIp, port, command, e.getMessage(), e);
            throw e;
        }
    }
    

    
    /**
     * 获取SSH连接池状态（通过反射调用）
     */
    public Map<String, Object> getSshPoolStats() {
        try {
            if (sshPoolManager != null) {
                Map<String, Object> stats = sshPoolManager.getPoolStats();
                stats.put("pluginId", getPluginId());
                stats.put("lastCheck", LocalDateTime.now());
                return stats;
            } else {
                return Map.of("error", "SSH连接池管理器未初始化");
            }
        } catch (Exception e) {
            log.error("【SSH插件】获取连接池状态失败", e);
            return Map.of("error", e.getMessage());
        }
    }
    
    // ================== 私有方法 ==================
    
    /**
     * 执行SSH连接性测试
     */
    private CheckResult performSshConnectivityTest(String hostIp, int port, String username, String password) {
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            log.debug("【SSH插件】测试SSH连接: {}@{}:{}", username, hostIp, port);
            
            // 使用连接池执行基础连接测试
            String testResult = sshPoolManager.executeCommand(hostIp, port, username, password, "echo 'ssh_test_ok'");
            
            // 验证结果
            boolean success = testResult != null && testResult.trim().contains("ssh_test_ok");
            
            LocalDateTime endTime = LocalDateTime.now();
            long durationMs = java.time.Duration.between(startTime, endTime).toMillis();
            
            CheckResult result;
            
            if (success) {
                result = CheckResult.builder()
                        .checkType(CheckType.SSH_CONNECTIVITY)
                        .success(true)
                        .message("SSH连接检查成功，连接正常")
                        .checkTime(endTime)
                        .build();
                log.info("【SSH插件】SSH连接检查成功: {}@{}:{}, 耗时: {}ms", 
                        username, hostIp, port, durationMs);
            } else {
                result = CheckResult.builder()
                        .checkType(CheckType.SSH_CONNECTIVITY)
                        .success(false)
                        .message("SSH连接检查失败，测试命令执行异常")
                        .error("期望输出包含'ssh_test_ok'，实际输出: " + testResult)
                        .checkTime(endTime)
                        .build();
                log.warn("【SSH插件】SSH连接检查失败: {}@{}:{}, 实际输出: {}", 
                        username, hostIp, port, testResult);
            }
            
            result.data("duration_ms", String.valueOf(durationMs))
                  .data("test_command", "echo 'ssh_test_ok'")
                  .data("test_result", testResult != null ? testResult.trim() : "null");
            
            return result;
            
        } catch (Exception e) {
            LocalDateTime endTime = LocalDateTime.now();
            long durationMs = java.time.Duration.between(startTime, endTime).toMillis();
            
            log.error("【SSH插件】SSH连接测试异常: {}@{}:{}, 耗时: {}ms, 错误: {}", 
                    username, hostIp, port, durationMs, e.getMessage());
            
            CheckResult result = CheckResult.builder()
                    .checkType(CheckType.SSH_CONNECTIVITY)
                    .success(false)
                    .message("SSH连接测试异常: " + e.getMessage())
                    .error(e.getMessage())
                    .checkTime(endTime)
                    .build();
            
            result.data("duration_ms", String.valueOf(durationMs))
                  .data("error_type", e.getClass().getSimpleName());
            
            return result;
        }
    }
}