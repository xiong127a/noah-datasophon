package com.datasophon.api.workflow.model;

/**
 * 工作流状态枚举
 * 
 * @author DataSophon Team
 */
public enum WorkflowStatus {
    
    /**
     * 等待执行
     */
    PENDING("pending", "等待执行"),
    
    /**
     * 执行中
     */
    RUNNING("running", "执行中"),
    
    /**
     * 已暂停
     */
    PAUSED("paused", "已暂停"),
    
    /**
     * 执行成功
     */
    COMPLETED("completed", "执行成功"),
    
    /**
     * 执行失败
     */
    FAILED("failed", "执行失败"),
    
    /**
     * 已取消
     */
    CANCELLED("cancelled", "已取消"),
    
    /**
     * 超时
     */
    TIMEOUT("timeout", "超时"),
    
    /**
     * 部分成功
     */
    PARTIAL_SUCCESS("partial_success", "部分成功");
    
    private final String code;
    private final String description;
    
    WorkflowStatus(String code, String description) {
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
     * 检查是否是终止状态
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED || this == TIMEOUT;
    }
    
    /**
     * 检查是否是成功状态
     */
    public boolean isSuccess() {
        return this == COMPLETED || this == PARTIAL_SUCCESS;
    }
}