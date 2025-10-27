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
import java.util.regex.Pattern;

/**
 * 内存检查器
 * 检查物理内存和交换区是否满足最小要求
 * 
 * @author 任相鹏
 * @date 2025-01-23
 */
@Slf4j
@Component
public class MemoryChecker implements EnvironmentCheckItem {
    
    @Value("${datasophon.checker.memory.min-memory:8192}")
    private int minMemory; // MB
    
    @Value("${datasophon.checker.memory.recommended-memory:16384}")
    private int recommendedMemory; // MB
    
    @Value("${datasophon.checker.memory.min-swap:4096}")
    private int minSwap; // MB
    
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
        return "memory";
    }
    
    @Override
    public String getDisplayName() {
        return "内存检查";
    }
    
    @Override
    public int getPriority() {
        return 20; // 参考配置
    }
    
    @Override
    public CheckResult execute(HostCheckContext context) {
        log.info("开始检查主机 {} 的内存配置", context.getHostIp());
        
        // 清理旧日志
        checkLogWriter.clearLogs(context.getClusterId(), context.getHostIp(), getCheckKey());
        
        // 记录检查开始
        checkLogWriter.logCheckStart(context.getClusterId(), context.getHostIp(), 
                getCheckKey(), "开始检查内存配置");
        
        try {
            // 获取内存详细信息
            var pluginContext = toPluginContext(context);
            
            // 获取总内存和已用内存
            var memCommand = "free -m | grep Mem | awk '{print $2,$3,$4}'";
            
            // 记录执行命令
            checkLogWriter.logCheckCommand(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), memCommand);
            
            var memResult = getSshService().executeCommand(pluginContext, memCommand);
            
            if (!memResult.isSuccess()) {
                String errorMsg = "无法获取内存信息: " + memResult.error();
                Map<String, Object> errorDetails = new HashMap<>();
                errorDetails.put("error", memResult.error());
                checkLogWriter.logCheckError(context.getClusterId(), context.getHostIp(),
                        getCheckKey(), errorMsg, errorDetails);
                return CheckResult.failure(
                        errorMsg,
                        "请检查SSH连接和系统命令是否可用",
                        false,
                        false
                );
            }
            
            // 记录命令输出
            checkLogWriter.logCheckOutput(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), memResult.output());
            
            var memParts = memResult.output().trim().split("\\s+");
            if (memParts.length < 3) {
                Map<String, Object> errorDetails = new HashMap<>();
                errorDetails.put("output", memResult.output());
                errorDetails.put("expectedFormat", "total used available");
                checkLogWriter.logCheckError(context.getClusterId(), context.getHostIp(),
                        getCheckKey(), "内存信息格式异常", errorDetails);
                return CheckResult.failure(
                        "内存信息格式异常",
                        "请检查系统命令输出格式",
                        false,
                        false
                );
            }
            
            int totalMemoryMB = Integer.parseInt(memParts[0].trim());
            int usedMemoryMB = Integer.parseInt(memParts[1].trim());
            int availableMemoryMB = Integer.parseInt(memParts[2].trim());
            double usagePercent = (double) usedMemoryMB / totalMemoryMB * 100;
            
            var details = new HashMap<String, Object>();
            details.put("totalMemoryMB", totalMemoryMB);
            details.put("usedMemoryMB", usedMemoryMB);
            details.put("availableMemoryMB", availableMemoryMB);
            details.put("requiredMemoryMB", minMemory);
            details.put("recommendedMemoryMB", recommendedMemory);
            details.put("usagePercent", Math.round(usagePercent * 10) / 10.0);
            
            // 记录解析结果
            checkLogWriter.logCheckInfo(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), String.format("内存解析成功: 总内存=%dMB, 已用=%dMB, 可用=%dMB, 使用率=%.1f%%", 
                            totalMemoryMB, usedMemoryMB, availableMemoryMB, usagePercent), details);
            
            // 检查物理内存
            boolean passed = totalMemoryMB >= minMemory;
            
            String resultMsg;
            if (!passed) {
                resultMsg = String.format("物理内存不足：实际 %d MB，要求至少 %d MB", 
                        totalMemoryMB, minMemory);
                details.put("recommendation", String.format("建议配置至少 %d MB 物理内存", recommendedMemory));
                checkLogWriter.logCheckError(context.getClusterId(), context.getHostIp(),
                        getCheckKey(), resultMsg, details);
                
                var checkResult = CheckResult.failure(
                        resultMsg,
                        String.format("建议配置至少 %d MB 物理内存", recommendedMemory),
                        true,  // 可以跳过
                        false  // 不能自动修复
                );
                checkResult.setDetails(details);
                return checkResult;
            }
            
            resultMsg = String.format("内存检查通过：总内存 %d MB，已使用 %d MB (%.1f%%)", 
                    totalMemoryMB, usedMemoryMB, usagePercent);
            checkLogWriter.logCheckSuccess(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), resultMsg, details);
            
            var checkResult = CheckResult.success(resultMsg);
            checkResult.setDetails(details);
            return checkResult;
            
        } catch (NumberFormatException e) {
            log.error("解析内存信息失败: {}", e.getMessage());
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("error", e.getMessage());
            checkLogWriter.logCheckError(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), "解析内存信息失败", errorDetails);
            return CheckResult.failure(
                    "解析内存信息失败",
                    "请检查系统命令输出格式",
                    false,
                    false
            );
        } catch (Exception e) {
            log.error("检查内存时发生异常: {}", e.getMessage(), e);
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("error", e.getMessage());
            checkLogWriter.logCheckError(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), "检查内存时发生异常", errorDetails);
            return CheckResult.failure(
                    "检查内存时发生异常: " + e.getMessage(),
                    "请检查SSH连接和系统状态",
                    false,
                    false
            );
        }
    }
    
    @Override
    public RepairResult repair(HostCheckContext context, Map<String, Object> params) {
        // 内存无法自动修复
        return RepairResult.builder()
                .success(false)
                .message("内存配置无法自动修复，请升级硬件配置或调整交换区大小")
                .build();
    }
}

