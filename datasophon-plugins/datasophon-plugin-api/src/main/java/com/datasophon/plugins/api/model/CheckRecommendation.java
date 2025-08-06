package com.datasophon.plugins.api.model;

import lombok.Builder;
import lombok.Data;

/**
 * 检查建议模型
 * 
 * @author DataSophon Team
 */
@Data
@Builder
public class CheckRecommendation {
    
    /**
     * 建议类型
     */
    private RecommendationType type;
    
    /**
     * 建议描述
     */
    private String description;
    
    /**
     * 执行命令
     */
    private String actionCommand;
    
    /**
     * 优先级
     */
    private Priority priority;
    
    /**
     * 预期结果
     */
    private String expectedResult;
    
    /**
     * 风险级别
     */
    private String riskLevel;
    
    /**
     * 是否自动执行
     */
    @Builder.Default
    private boolean autoExecute = false;
}