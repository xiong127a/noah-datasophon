package com.datasophon.plugins.api.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 检查配置模型
 * 
 * @author DataSophon Team
 */
@Data
@Builder
public class CheckConfiguration {
    
    /**
     * 启用的检查项
     */
    private Set<String> enabledChecks;
    
    /**
     * 禁用的检查项
     */
    private Set<String> disabledChecks;
    
    /**
     * 超时时间（毫秒）
     */
    @Builder.Default
    private long timeoutMs = 300000; // 5分钟
    
    /**
     * 重试次数
     */
    @Builder.Default
    private int retryCount = 3;
    
    /**
     * 重试间隔（毫秒）
     */
    @Builder.Default
    private long retryIntervalMs = 1000;
    
    /**
     * 是否快速失败
     */
    @Builder.Default
    private boolean failFast = false;
    
    /**
     * 并发执行数量
     */
    @Builder.Default
    private int concurrency = Runtime.getRuntime().availableProcessors();
    
    /**
     * 自定义参数
     */
    @Builder.Default
    private Map<String, Object> customParameters = new ConcurrentHashMap<>();
    
    /**
     * 检查是否启用了指定检查项
     */
    public boolean isCheckEnabled(String checkName) {
        if (disabledChecks != null && disabledChecks.contains(checkName)) {
            return false;
        }
        return enabledChecks == null || enabledChecks.isEmpty() || enabledChecks.contains(checkName);
    }
    
    /**
     * 获取自定义参数
     */
    @SuppressWarnings("unchecked")
    public <T> T getCustomParameter(String key, Class<T> type) {
        return (T) customParameters.get(key);
    }
    
    /**
     * 设置自定义参数
     */
    public void setCustomParameter(String key, Object value) {
        customParameters.put(key, value);
    }
}