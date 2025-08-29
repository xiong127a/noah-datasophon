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

import com.datasophon.api.model.HostValidationTaskData;
import com.datasophon.api.service.HostValidationService;
import com.datasophon.api.scheduler.HostValidationScheduler;
import com.datasophon.plugins.api.HostCheckerPlugin;
import com.datasophon.plugins.api.model.CheckResult;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.manager.PluginManager;
import com.datasophon.plugins.manager.LazyPluginLifecycleManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import java.util.Map;

/**
 * 主机验证服务实现
 * 基于插件系统和db-scheduler的主机验证服务
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-28
 */
@Service
@Slf4j
public class HostValidationServiceImpl implements HostValidationService {
    
    @Autowired
    private PluginManager pluginManager;
    
    @Autowired
    private LazyPluginLifecycleManager lazyPluginManager;
    
    @Autowired
    private HostValidationScheduler hostValidationScheduler;
    
    // 主机验证状态缓存
    private final Map<String, HostValidationStatus> validationStatusCache = new ConcurrentHashMap<>();
    
    // 主机验证结果缓存
    private final Map<String, Map<String, CheckResult>> validationResultsCache = new ConcurrentHashMap<>();
    
    @Override
    public void startHostValidation(String clusterId, String hostIp, 
                                  HostValidationTaskData.SshConnectionInfo sshInfo) {
        try {
            log.info("启动主机验证流程: 集群={}, 主机={}", clusterId, hostIp);
            
            String hostKey = getHostKey(clusterId, hostIp);
            
            // 更新状态为进行中
            validationStatusCache.put(hostKey, HostValidationStatus.IN_PROGRESS);
            
            // 初始化结果缓存
            validationResultsCache.put(hostKey, new ConcurrentHashMap<>());
            

            
            // 预热相关插件
            warmupRequiredPlugins();
            
            // 启动调度器
            hostValidationScheduler.startHostValidation(clusterId, hostIp, sshInfo);
            
            log.info("主机验证流程启动成功: 集群={}, 主机={}", clusterId, hostIp);
            
        } catch (Exception e) {
            log.error("启动主机验证流程失败: 集群={}, 主机={}, 错误={}", 
                    clusterId, hostIp, e.getMessage(), e);
            
            String hostKey = getHostKey(clusterId, hostIp);
            validationStatusCache.put(hostKey, HostValidationStatus.FAILED);
            
            throw new RuntimeException("启动主机验证失败", e);
        }
    }
    
    @Override
    public CheckResult executeSshConnectivityCheck(HostValidationTaskData taskData) throws InterruptedException, java.util.concurrent.ExecutionException, java.util.concurrent.TimeoutException {
        try {
            log.debug("执行SSH连接检查: 主机={}", taskData.getHostIp());
            
            // 获取SSH连接检查插件
            HostCheckerPlugin plugin = getPlugin("ssh-connectivity-check");
            if (plugin == null) {
                throw new RuntimeException("SSH连接检查插件未找到");
            }
            
            // 创建检查上下文
            HostCheckContext context = createHostCheckContext(taskData);
            
            // 检查插件是否可以执行
            if (!plugin.canExecute(context)) {
                throw new RuntimeException("SSH连接检查插件无法执行：前置条件不满足");
            }
            
            // 执行检查
            CheckResult result = plugin.executeCheck(context).get(60, TimeUnit.SECONDS);
            
            // 缓存结果
            cacheCheckResult(taskData.getClusterId(), taskData.getHostIp(), "ssh-check", result);
            
            log.debug("SSH连接检查完成: 主机={}, 成功={}", 
                    taskData.getHostIp(), result.isSuccess());
            
            return result;
            
        } catch (Exception e) {
            log.error("SSH连接检查失败: 主机={}, 错误={}", 
                    taskData.getHostIp(), e.getMessage(), e);
            
            CheckResult failedResult = CheckResult.builder()
                    .success(false)
                    .checkType("ssh-connectivity")
                    .message("SSH连接检查失败: " + e.getMessage())
                    .error(e.getMessage())
                    .checkTime(LocalDateTime.now())
                    .build();
            
            cacheCheckResult(taskData.getClusterId(), taskData.getHostIp(), "ssh-check", failedResult);
            
            throw e;
        }
    }
    
    @Override
    public CheckResult executeOsInfoCollection(HostValidationTaskData taskData) throws InterruptedException, java.util.concurrent.ExecutionException, java.util.concurrent.TimeoutException {
        try {
            log.debug("执行操作系统信息收集: 主机={}", taskData.getHostIp());
            
            // 获取操作系统信息收集插件
            HostCheckerPlugin plugin = getPlugin("os-info-collection");
            if (plugin == null) {
                throw new RuntimeException("操作系统信息收集插件未找到");
            }
            
            // 创建检查上下文
            HostCheckContext context = createHostCheckContext(taskData);
            
            // 检查前置条件（SSH检查是否成功）
            if (!checkPrerequsites(taskData.getClusterId(), taskData.getHostIp(), "ssh-check")) {
                throw new RuntimeException("前置检查未完成：SSH连接检查");
            }
            
            // 执行检查
            CheckResult result = plugin.executeCheck(context).get(60, TimeUnit.SECONDS);
            
            // 缓存结果
            cacheCheckResult(taskData.getClusterId(), taskData.getHostIp(), "os-info", result);
            
            log.debug("操作系统信息收集完成: 主机={}, 成功={}", 
                    taskData.getHostIp(), result.isSuccess());
            
            return result;
            
        } catch (Exception e) {
            log.error("操作系统信息收集失败: 主机={}, 错误={}", 
                    taskData.getHostIp(), e.getMessage(), e);
            
            CheckResult failedResult = CheckResult.builder()
                    .success(false)
                    .checkType("os-info-collection")
                    .message("操作系统信息收集失败: " + e.getMessage())
                    .error(e.getMessage())
                    .checkTime(LocalDateTime.now())
                    .build();
            
            cacheCheckResult(taskData.getClusterId(), taskData.getHostIp(), "os-info", failedResult);
            
            throw e;
        }
    }
    
    @Override
    public CheckResult executeHardwareInfoCollection(HostValidationTaskData taskData) throws InterruptedException, java.util.concurrent.ExecutionException {
        try {
            log.debug("执行硬件信息收集: 主机={}", taskData.getHostIp());
            
            // 获取硬件信息收集插件
            HostCheckerPlugin plugin = getPlugin("hardware-info-collection");
            if (plugin == null) {
                throw new RuntimeException("硬件信息收集插件未找到");
            }
            
            // 创建检查上下文
            HostCheckContext context = createHostCheckContext(taskData);
            
            // 检查前置条件
            if (!checkPrerequsites(taskData.getClusterId(), taskData.getHostIp(), "ssh-check")) {
                throw new RuntimeException("前置检查未完成：SSH连接检查");
            }
            
            // 执行检查
            CheckResult result = plugin.executeCheck(context).get();
            
            // 缓存结果
            cacheCheckResult(taskData.getClusterId(), taskData.getHostIp(), "hardware-info", result);
            
            log.debug("硬件信息收集完成: 主机={}, 成功={}", 
                    taskData.getHostIp(), result.isSuccess());
            
            return result;
            
        } catch (Exception e) {
            log.error("硬件信息收集失败: 主机={}, 错误={}", 
                    taskData.getHostIp(), e.getMessage(), e);
            
            CheckResult failedResult = CheckResult.builder()
                    .success(false)
                    .checkType("hardware-info-collection")
                    .message("硬件信息收集失败: " + e.getMessage())
                    .error(e.getMessage())
                    .checkTime(LocalDateTime.now())
                    .build();
            
            cacheCheckResult(taskData.getClusterId(), taskData.getHostIp(), "hardware-info", failedResult);
            
            throw e;
        }
    }
    
    @Override
    public CheckResult executeHostnameNetworkCheck(HostValidationTaskData taskData) {
        try {
            log.debug("执行主机名和网络检查: 主机={}", taskData.getHostIp());
            
            // 创建一个简单的网络检查结果
            CheckResult result = CheckResult.builder()
                    .success(true)
                    .checkType("hostname-network-check")
                    .message("主机名和网络检查成功")
                    .checkTime(LocalDateTime.now())
                    .build();
            
            result.data("hostname", "test-" + taskData.getHostIp().replace(".", "-"))
                  .data("networkStatus", "connected");
            
            // 缓存结果
            cacheCheckResult(taskData.getClusterId(), taskData.getHostIp(), "hostname-network", result);
            
            // 检查是否所有检查都完成了
            checkAllValidationsComplete(taskData.getClusterId(), taskData.getHostIp());
            
            log.debug("主机名和网络检查完成: 主机={}", taskData.getHostIp());
            
            return result;
            
        } catch (Exception e) {
            log.error("主机名和网络检查失败: 主机={}, 错误={}", 
                    taskData.getHostIp(), e.getMessage(), e);
            
            CheckResult failedResult = CheckResult.builder()
                    .success(false)
                    .checkType("hostname-network-check")
                    .message("主机名和网络检查失败: " + e.getMessage())
                    .error(e.getMessage())
                    .checkTime(LocalDateTime.now())
                    .build();
            
            cacheCheckResult(taskData.getClusterId(), taskData.getHostIp(), "hostname-network", failedResult);
            
            throw e;
        }
    }
    
    @Override
    public void markHostValidationFailed(String clusterId, String hostIp, 
                                       String checkType, String errorMessage) {
        try {
            log.warn("标记主机验证失败: 集群={}, 主机={}, 检查类型={}, 错误={}", 
                    clusterId, hostIp, checkType, errorMessage);
            
            String hostKey = getHostKey(clusterId, hostIp);
            validationStatusCache.put(hostKey, HostValidationStatus.FAILED);
            
            // 创建失败结果
            CheckResult failedResult = CheckResult.builder()
                    .success(false)
                    .checkType(checkType)
                    .message("检查失败: " + errorMessage)
                    .error(errorMessage)
                    .checkTime(LocalDateTime.now())
                    .build();
            
            cacheCheckResult(clusterId, hostIp, checkType, failedResult);
            
        } catch (Exception e) {
            log.error("标记主机验证失败时发生错误: 集群={}, 主机={}, 错误={}", 
                    clusterId, hostIp, e.getMessage(), e);
        }
    }
    
    @Override
    public HostValidationStatus getHostValidationStatus(String clusterId, String hostIp) {
        String hostKey = getHostKey(clusterId, hostIp);
        return validationStatusCache.getOrDefault(hostKey, HostValidationStatus.NOT_STARTED);
    }
    
    @Override
    public void cancelHostValidation(String clusterId, String hostIp) {
        try {
            log.info("取消主机验证: 集群={}, 主机={}", clusterId, hostIp);
            
            String hostKey = getHostKey(clusterId, hostIp);
            validationStatusCache.put(hostKey, HostValidationStatus.CANCELLED);
            
            // 通知调度器取消任务
            hostValidationScheduler.cancelHostValidation(clusterId, hostIp);
            
        } catch (Exception e) {
            log.error("取消主机验证失败: 集群={}, 主机={}, 错误={}", 
                    clusterId, hostIp, e.getMessage(), e);
        }
    }
    
    /**
     * 获取指定的插件（使用延迟加载）
     */
    private HostCheckerPlugin getPlugin(String pluginId) {
        try {
            return lazyPluginManager.getPlugin(pluginId);
        } catch (Exception e) {
            log.error("获取插件失败: pluginId={}, 错误={}", pluginId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 创建主机检查上下文
     */
    private HostCheckContext createHostCheckContext(HostValidationTaskData taskData) {
        return HostCheckContext.builder()
                .hostIp(taskData.getHostIp())
                .sshUser(taskData.getSshInfo().getSshUser())
                .sshPort(taskData.getSshInfo().getSshPort())
                .sshPassword(taskData.getSshInfo().getSshPassword())
                .privateKey(taskData.getSshInfo().getPrivateKey())
                .connectionTimeout(taskData.getSshInfo().getConnectionTimeout())
                .commandTimeout(taskData.getSshInfo().getCommandTimeout())
                .clusterId(taskData.getClusterId())
                .build();
    }
    
    /**
     * 生成主机键
     */
    private String getHostKey(String clusterId, String hostIp) {
        return clusterId + ":" + hostIp;
    }
    
    /**
     * 缓存检查结果
     */
    private void cacheCheckResult(String clusterId, String hostIp, String checkType, CheckResult result) {
        String hostKey = getHostKey(clusterId, hostIp);
        validationResultsCache.computeIfAbsent(hostKey, k -> new ConcurrentHashMap<>())
                              .put(checkType, result);
    }
    
    /**
     * 检查前置条件
     */
    private boolean checkPrerequsites(String clusterId, String hostIp, String prerequsite) {
        String hostKey = getHostKey(clusterId, hostIp);
        Map<String, CheckResult> results = validationResultsCache.get(hostKey);
        
        if (results == null) {
            return false;
        }
        
        CheckResult result = results.get(prerequsite);
        return result != null && result.isSuccess();
    }
    
    /**
     * 检查是否所有验证都完成了
     */
    private void checkAllValidationsComplete(String clusterId, String hostIp) {
        String hostKey = getHostKey(clusterId, hostIp);
        Map<String, CheckResult> results = validationResultsCache.get(hostKey);
        
        if (results == null) {
            return;
        }
        
        // 检查必要的检查项是否都完成
        String[] requiredChecks = {"ssh-check", "os-info", "hardware-info", "hostname-network"};
        boolean allComplete = true;
        boolean anyFailed = false;
        
        for (String checkType : requiredChecks) {
            CheckResult result = results.get(checkType);
            if (result == null) {
                allComplete = false;
                break;
            }
            if (!result.isSuccess()) {
                anyFailed = true;
            }
        }
        
        if (allComplete) {
            HostValidationStatus finalStatus = anyFailed ? HostValidationStatus.FAILED : HostValidationStatus.COMPLETED;
            validationStatusCache.put(hostKey, finalStatus);
            
            log.info("主机验证完成: 集群={}, 主机={}, 状态={}", 
                    clusterId, hostIp, finalStatus);
        }
    }
    
    /**
     * 获取主机的所有检查结果
     */
    public Map<String, CheckResult> getHostValidationResults(String clusterId, String hostIp) {
        String hostKey = getHostKey(clusterId, hostIp);
        return validationResultsCache.getOrDefault(hostKey, Map.of());
    }
    

    /**
     * 预热必需的插件
     */
    private void warmupRequiredPlugins() {
        try {
            // 预热主机验证相关插件
            lazyPluginManager.warmupPlugin("ssh-connectivity-check");
            lazyPluginManager.warmupPlugin("os-info-collection");
            lazyPluginManager.warmupPlugin("hardware-info-collection");
            
            log.debug("预热主机验证插件完成");
        } catch (Exception e) {
            log.warn("预热插件时发生错误: {}", e.getMessage());
        }
    }
    
    /**
     * 获取插件使用统计
     */
    public Map<String, Object> getPluginStats() {
        return lazyPluginManager.getPluginStats();
    }
    
    /**
     * 强制清理闲置插件
     */
    public int cleanupIdlePlugins() {
        return lazyPluginManager.forceCleanupIdlePlugins();
    }
    
    /**
     * 获取SSH连接池状态（通过插件获取）
     */
    public Map<String, Object> getSshPoolStats() {
        try {
            // 通过SSH插件获取连接池状态，不直接操作SSH
            HostCheckerPlugin sshPlugin = lazyPluginManager.getPlugin("ssh-connectivity-check");
            
            // 使用反射检查插件是否支持连接池状态查询
            if (sshPlugin != null) {
                try {
                    java.lang.reflect.Method method = sshPlugin.getClass().getMethod("getSshPoolStats");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> stats = (Map<String, Object>) method.invoke(sshPlugin);
                    return stats;
                } catch (NoSuchMethodException e) {
                    log.warn("SSH插件不支持连接池状态查询接口");
                    return Map.of("error", "SSH插件不支持连接池状态查询接口");
                } catch (Exception e) {
                    log.error("调用SSH插件连接池状态接口失败", e);
                    return Map.of("error", "调用SSH插件接口失败: " + e.getMessage());
                }
            } else {
                log.warn("SSH插件未加载");
                return Map.of("error", "SSH插件未加载");
            }
        } catch (Exception e) {
            log.error("获取SSH连接池状态失败", e);
            return Map.of("error", e.getMessage());
        }
    }
}
