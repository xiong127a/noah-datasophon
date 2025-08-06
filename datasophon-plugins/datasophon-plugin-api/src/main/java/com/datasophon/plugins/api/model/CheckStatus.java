package com.datasophon.plugins.api.model;

/**
 * 检查状态枚举
 * 
 * @author DataSophon Team
 */
public enum CheckStatus {
    
    /**
     * 检查成功
     */
    SUCCESS("success", "检查成功"),
    
    /**
     * 检查失败
     */
    FAILED("failed", "检查失败"),
    
    /**
     * 检查出错
     */
    ERROR("error", "检查出错"),
    
    /**
     * 跳过检查
     */
    SKIPPED("skipped", "跳过检查"),
    
    /**
     * 检查中
     */
    RUNNING("running", "检查中"),
    
    /**
     * 超时
     */
    TIMEOUT("timeout", "检查超时");
    
    private final String code;
    private final String description;
    
    CheckStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
}