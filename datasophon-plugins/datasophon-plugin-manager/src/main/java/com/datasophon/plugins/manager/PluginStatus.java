package com.datasophon.plugins.manager;

/**
 * 插件状态枚举
 * 
 * @author DataSophon Team
 */
public enum PluginStatus {
    
    /**
     * 未加载
     */
    UNLOADED("unloaded", "未加载"),
    
    /**
     * 已加载
     */
    LOADED("loaded", "已加载"),
    
    /**
     * 已启动
     */
    STARTED("started", "已启动"),
    
    /**
     * 已停止
     */
    STOPPED("stopped", "已停止"),
    
    /**
     * 活跃状态
     */
    ACTIVE("active", "活跃"),
    
    /**
     * 错误状态
     */
    ERROR("error", "错误"),
    
    /**
     * 禁用状态
     */
    DISABLED("disabled", "禁用");
    
    private final String code;
    private final String description;
    
    PluginStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 检查插件是否可用
     */
    public boolean isAvailable() {
        return this == ACTIVE;
    }
    
    /**
     * 检查插件是否运行中
     */
    public boolean isRunning() {
        return this == STARTED || this == ACTIVE;
    }
}