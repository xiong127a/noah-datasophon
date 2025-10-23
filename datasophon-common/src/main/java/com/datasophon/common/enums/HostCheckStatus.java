package com.datasophon.common.enums;

/**
 * 主机整体检查状态枚举
 * 
 * @author 任相鹏
 * @date 2025-01-23
 */
public enum HostCheckStatus {
    
    /**
     * 待检查
     */
    PENDING("待检查"),
    
    /**
     * 检查中
     */
    RUNNING("检查中"),
    
    /**
     * 全部成功
     */
    SUCCESS("全部通过"),
    
    /**
     * 部分成功（有失败项但已被跳过）
     */
    PARTIAL_SUCCESS("部分通过"),
    
    /**
     * 失败
     */
    FAILED("检查失败");
    
    private final String description;
    
    HostCheckStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

