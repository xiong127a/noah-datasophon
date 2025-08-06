package com.datasophon.plugins.manager;

import lombok.Builder;
import lombok.Data;

/**
 * 插件健康信息
 * 
 * @author DataSophon Team
 */
@Data
@Builder
public class PluginHealthInfo {
    
    /**
     * 插件ID
     */
    private String pluginId;
    
    /**
     * 插件状态
     */
    private PluginStatus status;
    
    /**
     * 是否健康
     */
    private boolean healthy;
    
    /**
     * 最后检查时间
     */
    private long lastCheckTime;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 创建未找到的健康信息
     */
    public static PluginHealthInfo notFound(String pluginId) {
        return PluginHealthInfo.builder()
                .pluginId(pluginId)
                .status(PluginStatus.UNLOADED)
                .healthy(false)
                .lastCheckTime(System.currentTimeMillis())
                .message("插件未找到")
                .build();
    }
    
    /**
     * 创建错误的健康信息
     */
    public static PluginHealthInfo error(String pluginId, String errorMessage) {
        return PluginHealthInfo.builder()
                .pluginId(pluginId)
                .status(PluginStatus.ERROR)
                .healthy(false)
                .lastCheckTime(System.currentTimeMillis())
                .message("插件异常")
                .errorMessage(errorMessage)
                .build();
    }
}