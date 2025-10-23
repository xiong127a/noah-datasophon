package com.datasophon.common.enums;

/**
 * 检查项状态枚举
 * 
 * @author 任相鹏
 * @date 2025-01-23
 */
public enum CheckItemStatus {
    
    /**
     * 待检查
     */
    PENDING("待检查"),
    
    /**
     * 检查中
     */
    RUNNING("检查中"),
    
    /**
     * 检查成功
     */
    SUCCESS("通过"),
    
    /**
     * 检查失败
     */
    FAILED("失败"),
    
    /**
     * 已跳过
     */
    SKIPPED("已跳过");
    
    private final String description;
    
    CheckItemStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

