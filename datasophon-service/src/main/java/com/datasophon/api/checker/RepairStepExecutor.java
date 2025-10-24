package com.datasophon.api.checker;

import com.datasophon.api.checker.util.CheckLogWriter;
import com.datasophon.common.vo.environment.RepairResult;
import com.datasophon.plugins.api.factory.SshConnectionServiceFactory;
import com.datasophon.plugins.api.service.SshConnectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 修复步骤执行器
 * 负责按顺序执行修复步骤，记录日志，处理异常
 * 
 * @author 任相鹏
 * @date 2025-10-24
 */
@Slf4j
@Component
public class RepairStepExecutor {
    
    private SshConnectionService sshService;
    
    /**
     * 获取SSH服务（延迟加载）
     */
    private SshConnectionService getSshService() {
        if (sshService == null) {
            sshService = SshConnectionServiceFactory.getInstance().getDefaultSshConnectionService();
        }
        return sshService;
    }
    
    /**
     * 执行修复步骤列表
     * 
     * @param steps 步骤列表
     * @param context 主机检查上下文
     * @param logWriter 日志写入器
     * @param checkKey 检查项key（用于日志）
     * @return 修复结果
     */
    public RepairResult executeSteps(List<RepairStep> steps, HostCheckContext context, 
                                     CheckLogWriter logWriter, String checkKey) {
        log.info("开始执行修复步骤: 主机={}, 检查项={}, 步骤数={}", 
                context.getHostIp(), checkKey, steps.size());
        
        int totalSteps = steps.size();
        
        for (int i = 0; i < totalSteps; i++) {
            RepairStep step = steps.get(i);
            int stepIndex = i + 1;
            
            try {
                // 记录步骤开始
                Map<String, Object> stepInfo = new HashMap<>();
                stepInfo.put("stepIndex", stepIndex);
                stepInfo.put("totalSteps", totalSteps);
                stepInfo.put("stepName", step.getStepName());
                stepInfo.put("stepDescription", step.getStepDescription());
                
                logWriter.logRepairInfo(
                        context.getClusterId(),
                        context.getHostIp(),
                        checkKey,
                        String.format("[步骤 %d/%d] %s", stepIndex, totalSteps, step.getStepName()),
                        stepInfo
                );
                
                log.info("执行修复步骤 [{}/{}]: {} - {}", 
                        stepIndex, totalSteps, step.getStepName(), step.getStepDescription());
                
                // 执行步骤
                long startTime = System.currentTimeMillis();
                step.execute(context, getSshService(), logWriter);
                long duration = System.currentTimeMillis() - startTime;
                
                // 记录步骤成功
                Map<String, Object> successInfo = new HashMap<>();
                successInfo.put("duration", duration + "ms");
                logWriter.logRepairSuccess(
                        context.getClusterId(),
                        context.getHostIp(),
                        checkKey,
                        String.format("[步骤 %d/%d] 完成: %s", stepIndex, totalSteps, step.getStepName()),
                        successInfo
                );
                
                log.info("修复步骤 [{}/{}] 执行成功: {} (耗时: {}ms)", 
                        stepIndex, totalSteps, step.getStepName(), duration);
                
            } catch (Exception e) {
                // 记录步骤失败
                log.error("修复步骤 [{}/{}] 执行失败: {} - {}", 
                        stepIndex, totalSteps, step.getStepName(), e.getMessage(), e);
                
                Map<String, Object> errorInfo = new HashMap<>();
                errorInfo.put("stepIndex", stepIndex);
                errorInfo.put("totalSteps", totalSteps);
                errorInfo.put("stepName", step.getStepName());
                errorInfo.put("error", e.getMessage());
                
                logWriter.logRepairError(
                        context.getClusterId(),
                        context.getHostIp(),
                        checkKey,
                        String.format("[步骤 %d/%d] 失败: %s - %s", 
                                stepIndex, totalSteps, step.getStepName(), e.getMessage()),
                        errorInfo
                );
                
                // 返回失败结果，终止后续步骤
                return RepairResult.builder()
                        .success(false)
                        .message(String.format("修复失败于步骤 %d/%d: %s - %s", 
                                stepIndex, totalSteps, step.getStepName(), e.getMessage()))
                        .build();
            }
        }
        
        // 所有步骤执行成功
        log.info("所有修复步骤执行成功: 主机={}, 检查项={}, 步骤数={}", 
                context.getHostIp(), checkKey, totalSteps);
        
        return RepairResult.builder()
                .success(true)
                .message(String.format("修复成功，共执行 %d 个步骤", totalSteps))
                .build();
    }
}

