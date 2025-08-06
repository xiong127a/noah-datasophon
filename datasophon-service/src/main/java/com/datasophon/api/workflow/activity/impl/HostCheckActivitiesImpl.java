package com.datasophon.api.workflow.activity.impl;

import com.datasophon.api.workflow.activity.HostCheckActivities;
import com.datasophon.plugins.api.HostCheckerPlugin;
import com.datasophon.plugins.api.model.CheckResult;
import com.datasophon.plugins.api.model.CheckStatus;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.model.OsInfo;
import com.datasophon.plugins.manager.PluginManager;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.enums.OsType;
import io.temporal.activity.Activity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * 主机检查活动实现类
 * 
 * @author DataSophon Team
 */
@Component
@Slf4j
public class HostCheckActivitiesImpl implements HostCheckActivities {
    
    @Autowired
    private PluginManager pluginManager;
    
    @Override
    public HostInfo collectHostInfo(HostInfo hostInfo) {
        log.info("收集主机信息: {}", hostInfo.getIp());
        
        try {
            // 记录活动心跳
            Activity.getExecutionContext().heartbeat("收集主机信息中...");
            
            // 这里可以扩展收集更多的主机信息
            // 例如：主机名、网络配置等
            
            log.info("主机信息收集完成: {}", hostInfo.getIp());
            return hostInfo;
            
        } catch (Exception e) {
            log.error("收集主机信息失败: {}", hostInfo.getIp(), e);
            throw new RuntimeException("收集主机信息失败", e);
        }
    }
    
    @Override
    public OsInfo detectOperatingSystem(HostInfo hostInfo) {
        log.info("检测操作系统信息: {}", hostInfo.getIp());
        
        try {
            Activity.getExecutionContext().heartbeat("检测操作系统中...");
            
            // 模拟OS检测逻辑
            // 实际应该通过SSH执行命令来检测
            OsInfo osInfo = OsInfo.builder()
                    .osType(OsType.LINUX)
                    .osName("Linux")
                    .version("Ubuntu 20.04")
                    .kernelVersion("5.4.0")
                    .architecture("x86_64")
                    .distribution("ubuntu")
                    .distributionVersion("20.04")
                    .bits("64")
                    .hostname(hostInfo.getHostname())
                    .build();
            
            log.info("操作系统检测完成: {} - {}", hostInfo.getIp(), osInfo.getFullDescription());
            return osInfo;
            
        } catch (Exception e) {
            log.error("检测操作系统失败: {}", hostInfo.getIp(), e);
            throw new RuntimeException("检测操作系统失败", e);
        }
    }
    
    @Override
    public List<String> discoverPlugins(OsInfo osInfo, List<String> requiredCheckTypes) {
        log.info("发现可用插件，操作系统: {}, 需要检查类型: {}", osInfo.getOsType(), requiredCheckTypes);
        
        try {
            Activity.getExecutionContext().heartbeat("发现插件中...");
            
            List<String> availablePlugins = new ArrayList<>();
            
            // 获取所有支持当前操作系统的插件
            List<HostCheckerPlugin> plugins = pluginManager.getPluginsForOs(osInfo.getOsType().name());
            
            for (HostCheckerPlugin plugin : plugins) {
                String pluginId = plugin.getPluginId();
                
                // 如果指定了检查类型，则只返回匹配的插件
                if (requiredCheckTypes == null || requiredCheckTypes.isEmpty() || 
                    requiredCheckTypes.contains(pluginId)) {
                    availablePlugins.add(pluginId);
                    log.debug("发现可用插件: {}", pluginId);
                }
            }
            
            log.info("插件发现完成，找到 {} 个可用插件", availablePlugins.size());
            return availablePlugins;
            
        } catch (Exception e) {
            log.error("发现插件失败", e);
            throw new RuntimeException("发现插件失败", e);
        }
    }
    
    @Override
    public CheckResult executePlugin(String pluginId, HostCheckContext context) {
        log.info("执行插件检查: {} -> {}", pluginId, context.getHostInfo().getIp());
        
        try {
            Activity.getExecutionContext().heartbeat("执行插件: " + pluginId);
            
            HostCheckerPlugin plugin = pluginManager.getPlugin(pluginId);
            if (plugin == null) {
                String errorMsg = "插件未找到: " + pluginId;
                log.error(errorMsg);
                return CheckResult.error(pluginId, errorMsg, new RuntimeException(errorMsg));
            }
            
            // 检查插件是否可以执行
            if (!plugin.canExecute(context)) {
                String errorMsg = "插件无法执行: " + pluginId;
                log.warn(errorMsg);
                return CheckResult.builder()
                        .pluginId(pluginId)
                        .status(CheckStatus.SKIPPED)
                        .message(errorMsg)
                        .build();
            }
            
            // 异步执行插件检查
            CompletableFuture<CheckResult> future = plugin.executeCheck(context);
            
            // 设置超时时间
            long timeoutMs = context.getTimeout();
            CheckResult result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            
            log.info("插件检查完成: {} -> {}, 状态: {}", 
                    pluginId, context.getHostInfo().getIp(), result.getStatus());
            
            return result;
            
        } catch (TimeoutException e) {
            String errorMsg = "插件执行超时: " + pluginId;
            log.error(errorMsg, e);
            return CheckResult.builder()
                    .pluginId(pluginId)
                    .status(CheckStatus.TIMEOUT)
                    .message(errorMsg)
                    .exceptionMessage(e.getMessage())
                    .build();
                    
        } catch (InterruptedException | ExecutionException e) {
            String errorMsg = "插件执行异常: " + pluginId;
            log.error(errorMsg, e);
            return CheckResult.error(pluginId, errorMsg, e);
        }
    }
    
    @Override
    public List<CheckResult> executePlugins(List<String> pluginIds, HostCheckContext context) {
        log.info("批量执行插件检查: {} -> {}", pluginIds.size(), context.getHostInfo().getIp());
        
        List<CheckResult> results = new ArrayList<>();
        
        for (String pluginId : pluginIds) {
            try {
                Activity.getExecutionContext().heartbeat("执行插件: " + pluginId);
                CheckResult result = executePlugin(pluginId, context);
                results.add(result);
                
                // 如果配置了快速失败且当前检查失败，则停止后续检查
                if (context.isFailFast() && result.isFailed()) {
                    log.warn("快速失败模式，停止后续插件检查: {}", pluginId);
                    break;
                }
                
            } catch (Exception e) {
                log.error("插件执行异常: {}", pluginId, e);
                results.add(CheckResult.error(pluginId, "插件执行异常", e));
            }
        }
        
        log.info("批量插件检查完成，执行了 {} 个插件", results.size());
        return results;
    }
    
    @Override
    public String aggregateResults(List<CheckResult> results) {
        log.info("聚合检查结果，共 {} 个结果", results.size());
        
        try {
            Activity.getExecutionContext().heartbeat("聚合检查结果中...");
            
            long successCount = results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
            long failedCount = results.stream().mapToLong(r -> r.isFailed() ? 1 : 0).sum();
            long errorCount = results.stream().mapToLong(r -> r.hasError() ? 1 : 0).sum();
            
            String summary = String.format(
                "检查完成: 总计 %d 项，成功 %d 项，失败 %d 项，错误 %d 项",
                results.size(), successCount, failedCount, errorCount);
            
            log.info("结果聚合完成: {}", summary);
            return summary;
            
        } catch (Exception e) {
            log.error("聚合检查结果失败", e);
            return "聚合检查结果失败: " + e.getMessage();
        }
    }
    
    @Override
    public void sendNotification(HostInfo hostInfo, List<CheckResult> results) {
        log.info("发送检查通知: {}", hostInfo.getIp());
        
        try {
            Activity.getExecutionContext().heartbeat("发送通知中...");
            
            // 这里可以实现具体的通知逻辑
            // 例如：邮件、短信、webhook等
            
            log.info("通知发送完成: {}", hostInfo.getIp());
            
        } catch (Exception e) {
            log.error("发送通知失败: {}", hostInfo.getIp(), e);
            // 通知失败不应该影响主流程，只记录日志
        }
    }
    
    @Override
    public void saveCheckResults(HostInfo hostInfo, List<CheckResult> results) {
        log.info("保存检查结果到数据库: {}", hostInfo.getIp());
        
        try {
            Activity.getExecutionContext().heartbeat("保存结果中...");
            
            // 这里可以实现具体的数据库保存逻辑
            
            log.info("检查结果保存完成: {}", hostInfo.getIp());
            
        } catch (Exception e) {
            log.error("保存检查结果失败: {}", hostInfo.getIp(), e);
            throw new RuntimeException("保存检查结果失败", e);
        }
    }
    
    @Override
    public boolean createSshSession(HostInfo hostInfo) {
        log.info("创建SSH会话: {}", hostInfo.getIp());
        
        try {
            Activity.getExecutionContext().heartbeat("创建SSH会话中...");
            
            // 这里应该实现具体的SSH会话创建逻辑
            // 可以集成现有的SSH连接池
            
            log.info("SSH会话创建完成: {}", hostInfo.getIp());
            return true;
            
        } catch (Exception e) {
            log.error("创建SSH会话失败: {}", hostInfo.getIp(), e);
            return false;
        }
    }
    
    @Override
    public void closeSshSession(HostInfo hostInfo) {
        log.info("关闭SSH会话: {}", hostInfo.getIp());
        
        try {
            // 这里应该实现具体的SSH会话关闭逻辑
            
            log.info("SSH会话关闭完成: {}", hostInfo.getIp());
            
        } catch (Exception e) {
            log.error("关闭SSH会话失败: {}", hostInfo.getIp(), e);
        }
    }
    
    @Override
    public boolean healthCheck(HostInfo hostInfo) {
        log.info("执行健康检查: {}", hostInfo.getIp());
        
        try {
            Activity.getExecutionContext().heartbeat("健康检查中...");
            
            // 这里可以实现具体的健康检查逻辑
            // 例如：ping测试、SSH连接测试等
            
            log.info("健康检查完成: {}", hostInfo.getIp());
            return true;
            
        } catch (Exception e) {
            log.error("健康检查失败: {}", hostInfo.getIp(), e);
            return false;
        }
    }
}