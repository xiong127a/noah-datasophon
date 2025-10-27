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
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 磁盘空间检查器
 * 检查关键目录的磁盘空间是否充足
 * 
 * @author 任相鹏
 * @date 2025-01-23
 */
@Slf4j
@Component
public class DiskChecker implements EnvironmentCheckItem {
    
    @Autowired
    private CheckLogWriter checkLogWriter;
    
    private SshConnectionService sshService;
    
    // 检查的目录及其最小可用空间(GB)
    private static final Map<String, Integer> CHECK_DIRECTORIES = Map.of(
            "/opt", 100,
            "/var/log", 50,
            "/tmp", 30
    );
    
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
        return "disk";
    }
    
    @Override
    public String getDisplayName() {
        return "磁盘空间检查";
    }
    
    @Override
    public int getPriority() {
        return 38; // 参考配置
    }
    
    @Override
    public CheckResult execute(HostCheckContext context) {
        log.info("开始检查主机 {} 的磁盘空间", context.getHostIp());
        
        // 清理旧日志
        checkLogWriter.clearLogs(context.getClusterId(), context.getHostIp(), getCheckKey());
        
        // 记录检查开始
        checkLogWriter.logCheckStart(context.getClusterId(), context.getHostIp(), 
                getCheckKey(), "开始检查磁盘空间");
        
        try {
            var pluginContext = toPluginContext(context);
            var details = new HashMap<String, Object>();
            var failedDirs = new HashMap<String, String>();
            
            for (var entry : CHECK_DIRECTORIES.entrySet()) {
                var dir = entry.getKey();
                var requiredGB = entry.getValue();
                
                // 获取目录可用空间（GB）
                var command = String.format("df -BG %s | tail -1 | awk '{print $4}' | sed 's/G//'", dir);
                checkLogWriter.logCheckCommand(context.getClusterId(), context.getHostIp(),
                        getCheckKey(), command);
                
                var result = getSshService().executeCommand(pluginContext, command);
                
                if (result.isSuccess()) {
                    checkLogWriter.logCheckOutput(context.getClusterId(), context.getHostIp(),
                            getCheckKey(), String.format("%s: %s", dir, result.output()));
                    
                    try {
                        int availableGB = Integer.parseInt(result.output().trim());
                        details.put(dir, Map.of(
                                "available", availableGB,
                                "required", requiredGB
                        ));
                        
                        if (availableGB < requiredGB) {
                            failedDirs.put(dir, String.format("可用 %d GB，需要 %d GB", availableGB, requiredGB));
                        }
                    } catch (NumberFormatException e) {
                        log.warn("解析目录 {} 的磁盘空间失败: {}", dir, e.getMessage());
                        Map<String, Object> warnDetails = new HashMap<>();
                        warnDetails.put("dir", dir);
                        warnDetails.put("error", e.getMessage());
                        checkLogWriter.logCheckWarning(context.getClusterId(), context.getHostIp(),
                                getCheckKey(), String.format("解析目录 %s 的磁盘空间失败", dir), warnDetails);
                    }
                }
            }
            
            // 记录检查结果详情
            checkLogWriter.logCheckInfo(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), String.format("磁盘空间检查完成: 不足的目录=%s", failedDirs.keySet()), details);
            
            if (!failedDirs.isEmpty()) {
                var message = new StringBuilder("以下目录磁盘空间不足：");
                failedDirs.forEach((dir, info) -> 
                        message.append(String.format("\n  %s: %s", dir, info)));
                
                details.put("failedDirs", failedDirs);
                details.put("recommendation", "建议清理磁盘空间或扩展存储容量");
                checkLogWriter.logCheckError(context.getClusterId(), context.getHostIp(),
                        getCheckKey(), message.toString(), details);
                
                var checkResult = CheckResult.failure(
                        message.toString(),
                        "建议清理磁盘空间或扩展存储容量",
                        true, // 可以跳过
                        false // 不能自动修复
                );
                checkResult.setDetails(details);
                return checkResult;
            }
            
            String successMsg = "磁盘空间检查通过：所有关键目录空间充足";
            checkLogWriter.logCheckSuccess(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), successMsg, details);
            
            var checkResult = CheckResult.success(successMsg);
            checkResult.setDetails(details);
            return checkResult;
            
        } catch (Exception e) {
            log.error("检查磁盘空间时发生异常: {}", e.getMessage(), e);
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("error", e.getMessage());
            checkLogWriter.logCheckError(context.getClusterId(), context.getHostIp(),
                    getCheckKey(), "检查磁盘空间时发生异常", errorDetails);
            return CheckResult.failure(
                    "检查磁盘空间时发生异常: " + e.getMessage(),
                    "请检查SSH连接和系统状态",
                    true,
                    false
            );
        }
    }
    
    @Override
    public RepairResult repair(HostCheckContext context, Map<String, Object> params) {
        // 磁盘空间无法自动修复
        return RepairResult.builder()
                .success(false)
                .message("磁盘空间无法自动修复，请手动清理或扩展存储容量")
                .build();
    }
}

