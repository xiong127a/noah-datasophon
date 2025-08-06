package com.datasophon.api.workflow;

import com.datasophon.api.workflow.model.*;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * 主机检查工作流接口
 * 定义主机检查的工作流方法
 * 
 * @author DataSophon Team
 */
@WorkflowInterface
public interface HostCheckWorkflow {
    
    /**
     * 执行单个主机检查工作流
     * @param request 检查请求
     * @return 检查结果
     */
    @WorkflowMethod
    HostCheckResult executeHostCheck(HostCheckRequest request);
    
    /**
     * 执行批量主机检查工作流
     * @param request 批量检查请求
     * @return 批量检查结果
     */
    @WorkflowMethod(name = "BatchHostCheck")
    BatchCheckResult executeBatchCheck(BatchCheckRequest request);
    
    /**
     * 暂停检查工作流
     */
    @SignalMethod
    void pauseCheck();
    
    /**
     * 恢复检查工作流
     */
    @SignalMethod
    void resumeCheck();
    
    /**
     * 停止检查工作流
     */
    @SignalMethod
    void stopCheck();
    
    /**
     * 获取检查进度
     * @return 检查进度信息
     */
    @QueryMethod
    CheckProgress getProgress();
    
    /**
     * 获取当前状态
     * @return 工作流状态
     */
    @QueryMethod
    WorkflowStatus getStatus();
}