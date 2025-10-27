package com.datasophon.api.agent;

/**
 * Agent分发步骤接口
 * 定义了Agent分发过程中每个步骤的标准行为
 */
public interface AgentDistributionStep {
    
    /**
     * 获取步骤名称
     * @return 步骤名称（用于日志和状态展示）
     */
    String getStepName();
    
    /**
     * 执行步骤逻辑
     * @param context Agent分发上下文，包含所有必要的参数和状态
     * @throws Exception 执行过程中的任何异常
     */
    void execute(AgentDistributionContext context) throws Exception;
}

