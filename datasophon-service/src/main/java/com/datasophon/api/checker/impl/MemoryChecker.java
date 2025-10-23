package com.datasophon.api.checker.impl;

import com.datasophon.api.checker.CheckResult;
import com.datasophon.api.checker.EnvironmentCheckItem;
import com.datasophon.api.checker.HostCheckContext;
import com.datasophon.common.vo.environment.RepairResult;
import com.datasophon.plugins.api.factory.SshConnectionServiceFactory;
import com.datasophon.plugins.api.service.SshConnectionService;
import lombok.extern.slf4j.Slf4j;
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
        
        try {
            // 获取内存信息（以MB为单位）
            var pluginContext = toPluginContext(context);
            var result = getSshService().executeCommand(pluginContext, "free -m | grep -E 'Mem|Swap' | awk '{print $2}'");
            
            if (!result.isSuccess()) {
                return CheckResult.failure(
                        "无法获取内存信息: " + result.error(),
                        "请检查SSH连接和系统命令是否可用",
                        false,
                        false
                );
            }
            
            var lines = result.output().trim().split("\n");
            if (lines.length < 2) {
                return CheckResult.failure(
                        "内存信息格式异常",
                        "请检查系统命令输出格式",
                        false,
                        false
                );
            }
            
            int actualMemory = Integer.parseInt(lines[0].trim());
            int actualSwap = Integer.parseInt(lines[1].trim());
            
            var details = new HashMap<String, Object>();
            details.put("actualMemory", actualMemory);
            details.put("actualSwap", actualSwap);
            details.put("requiredMemory", minMemory);
            details.put("requiredSwap", minSwap);
            details.put("recommendedMemory", recommendedMemory);
            
            var messages = new StringBuilder();
            boolean passed = true;
            
            // 检查物理内存
            if (actualMemory < minMemory) {
                messages.append(String.format("物理内存不足：实际 %d MB，最少需要 %d MB；", 
                        actualMemory, minMemory));
                passed = false;
            }
            
            // 检查交换区
            if (actualSwap < minSwap) {
                messages.append(String.format("交换区不足：实际 %d MB，最少需要 %d MB；", 
                        actualSwap, minSwap));
                passed = false;
            }
            
            if (!passed) {
                var checkResult = CheckResult.failure(
                        messages.toString(),
                        String.format("建议配置至少 %d MB 物理内存和 %d MB 交换区", 
                                recommendedMemory, minSwap),
                        true,  // 可以跳过
                        false  // 不能自动修复
                );
                checkResult.setDetails(details);
                return checkResult;
            }
            
            var checkResult = CheckResult.success(
                    String.format("内存检查通过：物理内存 %d MB，交换区 %d MB", 
                            actualMemory, actualSwap)
            );
            checkResult.setDetails(details);
            return checkResult;
            
        } catch (NumberFormatException e) {
            log.error("解析内存信息失败: {}", e.getMessage());
            return CheckResult.failure(
                    "解析内存信息失败",
                    "请检查系统命令输出格式",
                    false,
                    false
            );
        } catch (Exception e) {
            log.error("检查内存时发生异常: {}", e.getMessage(), e);
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

