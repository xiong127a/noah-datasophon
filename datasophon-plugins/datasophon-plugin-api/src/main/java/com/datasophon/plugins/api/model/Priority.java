package com.datasophon.plugins.api.model;

/**
 * 优先级枚举
 * 
 * @author DataSophon Team
 */
public enum Priority {
    
    /**
     * 低优先级
     */
    LOW("low", "低", 1),
    
    /**
     * 中等优先级
     */
    MEDIUM("medium", "中", 2),
    
    /**
     * 高优先级
     */
    HIGH("high", "高", 3),
    
    /**
     * 紧急优先级
     */
    URGENT("urgent", "紧急", 4);
    
    private final String code;
    private final String description;
    private final int level;
    
    Priority(String code, String description, int level) {
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
     * 是否比指定优先级更高
     */
    public boolean isHigherThan(Priority other) {
        return this.level > other.level;
    }
}