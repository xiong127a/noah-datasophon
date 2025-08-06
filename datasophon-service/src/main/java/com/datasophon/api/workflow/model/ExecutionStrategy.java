package com.datasophon.api.workflow.model;

/**
 * 执行策略枚举
 * 
 * @author DataSophon Team
 */
public enum ExecutionStrategy {
    
    /**
     * 串行执行
     */
    SERIAL("serial", "串行执行"),
    
    /**
     * 并行执行
     */
    PARALLEL("parallel", "并行执行"),
    
    /**
     * 优先级执行
     */
    PRIORITY_BASED("priority_based", "优先级执行"),
    
    /**
     * 条件执行
     */
    CONDITIONAL("conditional", "条件执行"),
    
    /**
     * 管道执行
     */
    PIPELINE("pipeline", "管道执行"),
    
    /**
     * 快速检查
     */
    FAST_CHECK("fast_check", "快速检查"),
    
    /**
     * 全面检查
     */
    COMPREHENSIVE_CHECK("comprehensive_check", "全面检查");
    
    private final String code;
    private final String description;
    
    ExecutionStrategy(String code, String description) {
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