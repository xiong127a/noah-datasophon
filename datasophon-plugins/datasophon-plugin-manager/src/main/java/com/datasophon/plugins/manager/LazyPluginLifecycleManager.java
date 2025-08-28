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

package com.datasophon.plugins.manager;

import com.datasophon.plugins.api.HostCheckerPlugin;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Map;

/**
 * 延迟加载插件生命周期管理器
 * 
 * 功能特性：
 * 1. 按需加载插件，不使用时不占用内存
 * 2. 自动卸载闲置插件
 * 3. 插件预热和缓存
 * 4. 内存使用监控
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-28
 */
@Component
@Slf4j
public class LazyPluginLifecycleManager {
    
    private final PluginManager pluginManager;
    private final ScheduledExecutorService cleanupExecutor;
    
    // 插件使用记录
    private final Map<String, PluginUsageRecord> pluginUsageMap = new ConcurrentHashMap<>();
    
    // 配置参数
    private static final long PLUGIN_IDLE_TIMEOUT = TimeUnit.MINUTES.toMillis(30); // 30分钟闲置后卸载
    private static final long CLEANUP_INTERVAL = TimeUnit.MINUTES.toMillis(10);    // 10分钟清理一次
    private static final long PLUGIN_WARMUP_DELAY = TimeUnit.SECONDS.toMillis(5);  // 5秒预热延迟
    
    public LazyPluginLifecycleManager(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Plugin-Cleanup-Thread");
            t.setDaemon(true);
            return t;
        });
        
        // 启动定期清理任务
        startCleanupTask();
        
        log.info("延迟加载插件生命周期管理器已启动");
    }
    
    @PreDestroy
    public void shutdown() {
        log.info("关闭插件生命周期管理器...");
        
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // 卸载所有插件
        unloadAllPlugins();
        
        log.info("插件生命周期管理器已关闭");
    }
    
    /**
     * 获取插件（按需加载）
     */
    public HostCheckerPlugin getPlugin(String pluginId) {
        try {
            // 记录使用
            recordPluginUsage(pluginId);
            
            // 确保插件管理器已初始化
            ensurePluginManagerInitialized();
            
            // 从PluginManager的活跃插件中获取
            HostCheckerPlugin plugin = pluginManager.getActivePlugins().get(pluginId);
            
            if (plugin == null) {
                log.warn("插件未找到或未加载: pluginId={}，可用插件: {}", 
                        pluginId, pluginManager.getActivePlugins().keySet());
            }
            
            return plugin;
            
        } catch (Exception e) {
            log.error("获取插件失败: pluginId={}, 错误={}", pluginId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 预热指定插件（提前加载但不立即使用）
     */
    public void warmupPlugin(String pluginId) {
        cleanupExecutor.schedule(() -> {
            try {
                log.debug("预热插件: {}", pluginId);
                getPlugin(pluginId);
            } catch (Exception e) {
                log.warn("预热插件失败: pluginId={}, 错误={}", pluginId, e.getMessage());
            }
        }, PLUGIN_WARMUP_DELAY, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 手动卸载插件
     */
    public void unloadPlugin(String pluginId) {
        try {
            HostCheckerPlugin plugin = pluginManager.getActivePlugins().remove(pluginId);
            
            if (plugin != null) {
                plugin.cleanup();
                pluginUsageMap.remove(pluginId);
                
                log.info("手动卸载插件: {}", pluginId);
                
                // 触发垃圾回收建议
                suggestGarbageCollection();
            }
            
        } catch (Exception e) {
            log.error("卸载插件失败: pluginId={}, 错误={}", pluginId, e.getMessage(), e);
        }
    }
    
    /**
     * 获取插件使用统计
     */
    public Map<String, Object> getPluginStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        int loadedPlugins = pluginManager.getActivePlugins().size();
        int trackedPlugins = pluginUsageMap.size();
        
        stats.put("loadedPlugins", loadedPlugins);
        stats.put("trackedPlugins", trackedPlugins);
        stats.put("idleTimeoutMinutes", PLUGIN_IDLE_TIMEOUT / 60000);
        stats.put("cleanupIntervalMinutes", CLEANUP_INTERVAL / 60000);
        
        // 详细使用记录
        Map<String, Map<String, Object>> usageDetails = new ConcurrentHashMap<>();
        pluginUsageMap.forEach((pluginId, record) -> {
            Map<String, Object> detail = new ConcurrentHashMap<>();
            detail.put("lastUsed", record.getLastUsed());
            detail.put("usageCount", record.getUsageCount());
            detail.put("idleMinutes", (System.currentTimeMillis() - record.getLastUsedTime()) / 60000);
            usageDetails.put(pluginId, detail);
        });
        stats.put("usageDetails", usageDetails);
        
        return stats;
    }
    
    /**
     * 强制清理所有闲置插件
     */
    public int forceCleanupIdlePlugins() {
        return cleanupIdlePlugins();
    }
    
    // ================== 私有方法 ==================
    
    /**
     * 确保插件管理器已初始化
     */
    private void ensurePluginManagerInitialized() {
        if (!pluginManager.isInitialized()) {
            log.info("插件管理器尚未初始化，开始初始化...");
            pluginManager.initializePlugins();
        }
    }
    

    
    /**
     * 记录插件使用
     */
    private void recordPluginUsage(String pluginId) {
        pluginUsageMap.computeIfAbsent(pluginId, k -> new PluginUsageRecord())
                     .recordUsage();
    }
    
    /**
     * 启动清理任务
     */
    private void startCleanupTask() {
        cleanupExecutor.scheduleWithFixedDelay(
                this::cleanupIdlePlugins,
                CLEANUP_INTERVAL,
                CLEANUP_INTERVAL,
                TimeUnit.MILLISECONDS
        );
        
        log.info("插件清理任务已启动，间隔: {} 分钟", CLEANUP_INTERVAL / 60000);
    }
    
    /**
     * 清理闲置插件
     */
    private int cleanupIdlePlugins() {
        int unloadedCount = 0;
        long currentTime = System.currentTimeMillis();
        
        for (Map.Entry<String, PluginUsageRecord> entry : pluginUsageMap.entrySet()) {
            String pluginId = entry.getKey();
            PluginUsageRecord record = entry.getValue();
            
            if (currentTime - record.getLastUsedTime() > PLUGIN_IDLE_TIMEOUT) {
                // 插件已闲置超时，卸载它
                unloadPlugin(pluginId);
                unloadedCount++;
            }
        }
        
        if (unloadedCount > 0) {
            log.info("清理闲置插件完成，卸载数量: {}", unloadedCount);
            suggestGarbageCollection();
        }
        
        return unloadedCount;
    }
    
    /**
     * 卸载所有插件
     */
    private void unloadAllPlugins() {
        for (String pluginId : pluginManager.getActivePlugins().keySet()) {
            unloadPlugin(pluginId);
        }
    }
    
    /**
     * 建议进行垃圾回收
     */
    private void suggestGarbageCollection() {
        // 获取当前内存使用情况
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        log.debug("内存使用情况: 已用={}MB, 总计={}MB, 使用率={}%",
                usedMemory / 1024 / 1024,
                totalMemory / 1024 / 1024,
                (usedMemory * 100) / totalMemory);
        
        // 建议JVM进行垃圾回收
        System.gc();
    }
    
    /**
     * 插件使用记录
     */
    @Getter
    private static class PluginUsageRecord {
        private volatile long lastUsedTime;
        private volatile int usageCount;
        
        public PluginUsageRecord() {
            this.lastUsedTime = System.currentTimeMillis();
            this.usageCount = 0;
        }
        
        public void recordUsage() {
            this.lastUsedTime = System.currentTimeMillis();
            this.usageCount++;
        }

        public LocalDateTime getLastUsed() {
            return LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(lastUsedTime),
                    java.time.ZoneId.systemDefault()
            );
        }
    }
}
