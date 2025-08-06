package com.datasophon.plugins.api.model;

/**
 * 严重程度枚举
 * 
 * @author DataSophon Team
 */
public enum Severity {
    
    /**
     * 信息级别
     */
    INFO("info", "信息", 1),
    
    /**
     * 警告级别
     */
    WARNING("warning", "警告", 2),
    
    /**
     * 错误级别
     */
    ERROR("error", "错误", 3),
    
    /**
     * 致命级别
     */
    CRITICAL("critical", "致命", 4);
    
    private final String code;
    private final String description;
    private final int level;
    
    Severity(String code, String description, int level) {
        this.code = code;
        this.description = description;
        this.level = level;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getLevel() {
        return level;
    }
    
    /**
     * 是否比指定级别更严重
     */
    public boolean isMoreSevereThan(Severity other) {
        return this.level > other.level;
    }
    
    /**
     * 是否是严重级别（错误或致命）
     */
    public boolean isSevere() {
        return this == ERROR || this == CRITICAL;
    }
}