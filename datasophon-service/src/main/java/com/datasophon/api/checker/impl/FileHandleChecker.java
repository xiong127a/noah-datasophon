package com.datasophon.api.checker.impl;

import com.datasophon.api.checker.CheckResult;
import com.datasophon.api.checker.EnvironmentCheckItem;
import com.datasophon.api.checker.HostCheckContext;
import com.datasophon.api.checker.util.CheckLogWriter;
import com.datasophon.common.vo.environment.RepairResult;
import com.datasophon.plugins.api.factory.SshConnectionServiceFactory;
import com.datasophon.plugins.api.service.SshConnectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件句柄数检查器
 * 检查系统文件句柄限制是否满足要求
 * 
 * @author 任相鹏
 * @date 2025-01-24
 */
@Slf4j
@Component
public class FileHandleChecker implements EnvironmentCheckItem {
    
    @Value("${datasophon.checker.file-handle.min-limit:65535}")
    private int minLimit;
    
    @Value("${datasophon.checker.file-handle.recommended-limit:655350}")
    private int recommendedLimit;
    
    @Autowired
    private CheckLogWriter checkLogWriter;
    
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
     * 转换为插件API的HostCheckContext
     */
    private com.datasophon.plugins.api.model.HostCheckContext toPluginContext(HostCheckContext context) {
        return com.datasophon.plugins.api.model.HostCheckContext.builder()
                .hostIp(context.getHostIp())
                .clusterId(context.getClusterId() != null ? context.getClusterId() : null)
                .sshUser(context.getSshUser())
                .sshPort(context.getSshPort())
                .sshPassword(context.getSshPassword())
                .build();
    }
    
    @Override
    public String getCheckKey() {
        return "file-handle";
    }
    
    @Override
    public String getDisplayName() {
        return "文件句柄数检查";
    }
    
    @Override
    public int getPriority() {
        return 51; // 参考配置
    }
    
    @Override
    public CheckResult execute(HostCheckContext context) {
        log.info("开始检查主机 {} 的文件句柄限制", context.getHostIp());
        
        // 清理旧日志
        checkLogWriter.clearLogs(context.getClusterId(), context.getHostIp(), getCheckKey());
        
        // 记录检查开始
        checkLogWriter.logCheckStart(context.getClusterId(), context.getHostIp(), 
                getCheckKey(), "开始检查文件句柄限制");
        
        try {
            var pluginContext = toPluginContext(context);
            
            // 检查当前用户的文件句柄限制
            // 使用 bash -l 模拟登录环境，确保加载 /etc/security/limits.conf 中的配置
            // 这对于修复后的验证很重要，因为修改 limits.conf 需要重新登录才能生效
            var command = "bash -l -c 'ulimit -n'";
            checkLogWriter.logCheckCommand(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), "检查文件句柄限制（模拟登录环境）: " + command);
            
            var result = getSshService().executeCommand(pluginContext, command);
            
            checkLogWriter.logCheckOutput(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), result.output());
            
            if (!result.isSuccess()) {
                String errorMsg = "无法获取文件句柄限制: " + result.error();
                Map<String, Object> errorDetails = new HashMap<>();
                errorDetails.put("error", result.error());
                checkLogWriter.logCheckError(context.getClusterId(), context.getHostIp(),
                        getCheckKey(), errorMsg, errorDetails);
                return CheckResult.failure(
                        errorMsg,
                        "请检查SSH连接和系统命令是否可用",
                        false,
                        false
                );
            }
            
            int currentLimit;
            try {
                currentLimit = Integer.parseInt(result.output().trim());
            } catch (NumberFormatException e) {
                String errorMsg = "解析文件句柄限制失败: " + result.output();
                Map<String, Object> errorDetails = new HashMap<>();
                errorDetails.put("output", result.output());
                errorDetails.put("error", e.getMessage());
                checkLogWriter.logCheckError(context.getClusterId(), context.getHostIp(),
                        getCheckKey(), errorMsg, errorDetails);
                return CheckResult.failure(
                        errorMsg,
                        "请检查系统配置",
                        false,
                        false
                );
            }
            
            var details = new HashMap<String, Object>();
            details.put("currentLimit", currentLimit);
            details.put("minLimit", minLimit);
            details.put("recommendedLimit", recommendedLimit);
            
            // 记录检查详情
            checkLogWriter.logCheckInfo(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), String.format("文件句柄限制: 当前=%d, 要求=%d, 推荐=%d", 
                            currentLimit, minLimit, recommendedLimit), details);
            
            if (currentLimit < minLimit) {
                String failMsg = String.format("文件句柄限制过低：当前 %d，要求至少 %d", currentLimit, minLimit);
                details.put("recommendation", String.format("建议配置至少 %d，推荐 %d", minLimit, recommendedLimit));
                checkLogWriter.logCheckError(context.getClusterId(), context.getHostIp(),
                        getCheckKey(), failMsg, details);
                
                var checkResult = CheckResult.failure(
                        failMsg,
                        String.format("建议配置至少 %d，推荐 %d", minLimit, recommendedLimit),
                        true, // 可以跳过
                        true  // 可以修复
                );
                checkResult.setDetails(details);
                return checkResult;
            }
            
            String successMsg = String.format("文件句柄检查通过：当前限制 %d（要求 %d）", currentLimit, minLimit);
            checkLogWriter.logCheckSuccess(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), successMsg, details);
            
            var checkResult = CheckResult.success(successMsg);
            checkResult.setDetails(details);
            return checkResult;
            
        } catch (Exception e) {
            log.error("检查文件句柄时发生异常: {}", e.getMessage(), e);
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("error", e.getMessage());
            checkLogWriter.logCheckError(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), "检查文件句柄时发生异常", errorDetails);
            return CheckResult.failure(
                    "检查文件句柄时发生异常: " + e.getMessage(),
                    "请检查SSH连接和系统状态",
                    false,
                    false
            );
        }
    }
    
    /**
     * 修复文件句柄限制
     * 
     * 注意：此方法仅执行修复操作（修改 /etc/security/limits.conf），不包含验证逻辑。
     * 验证由框架在修复成功后自动调用 execute() 方法完成。
     * execute() 使用 bash -l 模拟登录环境来验证新的限制值。
     * 
     * @param context 主机检查上下文
     * @param params 修复参数（当前未使用）
     * @return RepairResult 修复结果（仅表示修改配置文件是否成功）
     */
    @Override
    public RepairResult repair(HostCheckContext context, Map<String, Object> params) {
        log.info("开始修复主机 {} 的文件句柄限制", context.getHostIp());
        
        // 记录修复开始
        checkLogWriter.logRepairStart(context.getClusterId(), context.getHostIp(),
                getCheckKey(), "开始修复文件句柄限制");
        
        try {
            var pluginContext = toPluginContext(context);
            
            // 修改 /etc/security/limits.conf
            var limitsCommand = String.format(
                    "echo '*    soft    nofile    %d' | sudo tee -a /etc/security/limits.conf && " +
                    "echo '*    hard    nofile    %d' | sudo tee -a /etc/security/limits.conf",
                    recommendedLimit, recommendedLimit);
            
            checkLogWriter.logRepairCommand(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), limitsCommand);
            
            var result = getSshService().executeCommand(pluginContext, limitsCommand);
            checkLogWriter.logRepairOutput(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), result.output());
            
            if (!result.isSuccess()) {
                String errorMsg = "修改limits.conf失败: " + result.error();
                Map<String, Object> errorDetails = new HashMap<>();
                errorDetails.put("error", result.error());
                checkLogWriter.logRepairError(context.getClusterId(), context.getHostIp(),
                        getCheckKey(), errorMsg, errorDetails);
                return RepairResult.builder()
                        .success(false)
                        .message(errorMsg)
                        .build();
            }
            
            checkLogWriter.logRepairInfo(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), "成功修改limits.conf，修复需要重新登录后生效", null);
            
            String successMsg = String.format("文件句柄限制已修改为 %d，需要重新登录后生效", recommendedLimit);
            Map<String, Object> successDetails = new HashMap<>();
            successDetails.put("newLimit", recommendedLimit);
            successDetails.put("note", "修改已生效，但需要重新登录SSH会话");
            checkLogWriter.logRepairSuccess(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), successMsg, successDetails);
            
            return RepairResult.builder()
                    .success(true)
                    .message(successMsg)
                    .details("修改已生效，但需要重新登录SSH会话")
                    .build();
                    
        } catch (Exception e) {
            log.error("修复文件句柄限制时发生异常: {}", e.getMessage(), e);
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("exception", e.getMessage());
            checkLogWriter.logRepairError(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), "修复失败", errorDetails);
            return RepairResult.builder()
                    .success(false)
                    .message("修复失败: " + e.getMessage())
                    .build();
        }
    }
}
