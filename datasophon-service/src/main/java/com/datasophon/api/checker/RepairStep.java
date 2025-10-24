package com.datasophon.api.checker;

import com.datasophon.api.checker.util.CheckLogWriter;
import com.datasophon.plugins.api.service.SshConnectionService;

/**
 * 修复步骤接口
 * 每个修复步骤应该执行单一、明确的操作
 * 
 * @author 任相鹏
 * @date 2025-10-24
 */
public interface RepairStep {
    
    /**
     * 获取步骤名称（用于日志和UI显示）
     */
    String getStepName();
    
    /**
     * 获取步骤描述（详细说明）
     */
    String getStepDescription();
    
    /**
     * 执行修复步骤
     * 
     * @param context 主机检查上下文（包含SSH认证信息）
     * @param sshService SSH连接服务
     * @param logWriter 日志写入器
     * @throws Exception 执行失败时抛出异常
     */
    void execute(HostCheckContext context, SshConnectionService sshService, CheckLogWriter logWriter) throws Exception;
}

