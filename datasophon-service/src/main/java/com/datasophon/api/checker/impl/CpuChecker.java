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
 * CPU核心数检查器
 * 检查CPU核心数是否满足最小要求
 * 
 * @author 任相鹏
 * @date 2025-01-23
 */
@Slf4j
@Component
public class CpuChecker implements EnvironmentCheckItem {
    
    @Value("${datasophon.checker.cpu.min-cores:4}")
    private int minCores;
    
    @Value("${datasophon.checker.cpu.recommended-cores:8}")
    private int recommendedCores;
    
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
                .clusterId(context.getClusterId() != null ? context.getClusterId().toString() : null)
                .sshUser(context.getSshUser())
                .sshPort(context.getSshPort())
                .sshPassword(context.getSshPassword())
                .build();
    }
    
    @Override
    public String getCheckKey() {
        return "cpu";
    }
    
    @Override
    public String getDisplayName() {
        return "CPU核心数检查";
    }
    
    @Override
    public int getPriority() {
        return 14; // 参考 checker-config.yml 中的配置
    }
    
    @Override
    public CheckResult execute(HostCheckContext context) {
        log.info("开始检查主机 {} 的CPU核心数", context.getHostIp());
        
        // 清理旧日志
        checkLogWriter.clearLogs(context.getClusterId(), context.getHostIp(), getCheckKey());
        
        // 记录检查开始
        checkLogWriter.logCheckStart(context.getClusterId(), context.getHostIp(), 
                getCheckKey(), "开始检查CPU核心数");
        
        try {
            // 执行命令获取CPU核心数
            var pluginContext = toPluginContext(context);
            String command = "nproc";
            
            // 记录执行命令
            checkLogWriter.logCheckCommand(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), command);
            
            var result = getSshService().executeCommand(pluginContext, command);
            
            // 记录命令输出
            checkLogWriter.logCheckOutput(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), result.output());
            
            if (!result.isSuccess()) {
                String errorMsg = "无法获取CPU核心数: " + result.error();
                Map<String, Object> errorDetails = new HashMap<>();
                errorDetails.put("error", result.error());
                checkLogWriter.logCheckError(context.getClusterId(), context.getHostIp(),
                        getCheckKey(), errorMsg, errorDetails);
                return CheckResult.failure(
                        errorMsg,
                        "请检查SSH连接和系统命令是否可用",
                        false, // 不能跳过
                        false  // 不能修复
                );
            }
            
            int actualCores = Integer.parseInt(result.output().trim());
            
            var details = new HashMap<String, Object>();
            details.put("actual", actualCores);
            details.put("required", minCores);
            details.put("recommended", recommendedCores);
            
            // 记录检查详情
            checkLogWriter.logCheckInfo(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), String.format("CPU检查详情: 实际=%d核, 要求=%d核, 推荐=%d核", 
                            actualCores, minCores, recommendedCores), details);
            
            if (actualCores < minCores) {
                String failMsg = String.format("CPU核心数不足：实际 %d 核，最少需要 %d 核", actualCores, minCores);
                details.put("recommendation", String.format("建议使用 %d 核或更多CPU", recommendedCores));
                checkLogWriter.logCheckError(context.getClusterId(), context.getHostIp(),
                        getCheckKey(), failMsg, details);
                
                var checkResult = CheckResult.failure(
                        failMsg,
                        String.format("建议使用 %d 核或更多CPU", recommendedCores),
                        true,  // 可以跳过（允许用户忽略）
                        false  // 不能自动修复
                );
                checkResult.setDetails(details);
                return checkResult;
            }
            
            String successMsg = String.format("CPU核心数检查通过：%d 核（最少需要 %d 核）", actualCores, minCores);
            checkLogWriter.logCheckSuccess(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), successMsg, details);
            
            var checkResult = CheckResult.success(successMsg);
            checkResult.setDetails(details);
            return checkResult;
            
        } catch (NumberFormatException e) {
            log.error("解析CPU核心数失败: {}", e.getMessage());
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("error", e.getMessage());
            checkLogWriter.logCheckError(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), "解析CPU核心数失败", errorDetails);
            return CheckResult.failure(
                    "解析CPU核心数失败",
                    "请检查系统命令输出格式",
                    false,
                    false
            );
        } catch (Exception e) {
            log.error("检查CPU核心数时发生异常: {}", e.getMessage(), e);
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("error", e.getMessage());
            checkLogWriter.logCheckError(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), "检查CPU核心数时发生异常", errorDetails);
            return CheckResult.failure(
                    "检查CPU核心数时发生异常: " + e.getMessage(),
                    "请检查SSH连接和系统状态",
                    false,
                    false
            );
        }
    }
    
    @Override
    public RepairResult repair(HostCheckContext context, Map<String, Object> params) {
        // CPU核心数无法自动修复
        return RepairResult.builder()
                .success(false)
                .message("CPU核心数无法自动修复，请升级硬件配置")
                .build();
    }
}

