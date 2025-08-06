package com.datasophon.api.workflow.model;

/**
 * 批量执行模式枚举
 * 
 * @author DataSophon Team
 */
public enum BatchExecutionMode {
    
    /**
     * 全并行执行
     */
    ALL_PARALLEL("all_parallel", "全并行执行"),
    
    /**
     * 分批并行执行
     */
    BATCH_PARALLEL("batch_parallel", "分批并行执行"),
    
    /**
     * 串行执行
     */
    SEQUENTIAL("sequential", "串行执行"),
    
    /**
     * 优先级分组执行
     */
    PRIORITY_GROUP("priority_group", "优先级分组执行"),
    
    /**
     * 滚动执行
     */
    ROLLING("rolling", "滚动执行");
    
    private final String code;
    private final String description;
    
    BatchExecutionMode(String code, String description) {
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