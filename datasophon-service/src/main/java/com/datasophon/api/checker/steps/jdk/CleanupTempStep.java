package com.datasophon.api.checker.steps.jdk;

import com.datasophon.api.checker.HostCheckContext;
import com.datasophon.api.checker.RepairStep;
import com.datasophon.api.checker.util.CheckLogWriter;
import com.datasophon.plugins.api.service.SshConnectionService;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 清理临时文件步骤
 * 
 * @author 任相鹏
 * @date 2025-10-24
 */
@Slf4j
public class CleanupTempStep implements RepairStep {
    
    private final String tempDir;
    
    public CleanupTempStep(String tempDir) {
        this.tempDir = tempDir;
    }
    
    @Override
    public String getStepName() {
        return "清理临时文件";
    }
    
    @Override
    public String getStepDescription() {
        return "删除临时目录及下载的安装包";
    }
    
    @Override
    public void execute(HostCheckContext context, SshConnectionService sshService, CheckLogWriter logWriter) throws Exception {
        var pluginContext = toPluginContext(context);
        
        String command = String.format("rm -rf %s && echo 'Temp directory cleaned: %s'", tempDir, tempDir);
        
        Map<String, Object> commandInfo = new HashMap<>();
        commandInfo.put("command", command);
        commandInfo.put("tempDir", tempDir);
        logWriter.logRepairCommand(context.getClusterId(), context.getHostIp(), "java", command);
        
        log.info("开始清理临时目录: {}", tempDir);
        
        var result = sshService.executeCommand(pluginContext, command);
        logWriter.logRepairOutput(context.getClusterId(), context.getHostIp(), "java", result.output());
        
        if (!result.isSuccess()) {
            // 清理失败不应该导致整个修复失败，只记录警告
            log.warn("清理临时目录失败: {}", result.error());
            logWriter.logRepairInfo(context.getClusterId(), context.getHostIp(), "java", 
                    "清理临时目录失败（不影响安装）: " + result.error(), null);
        } else {
            log.info("临时目录清理成功: {}", tempDir);
        }
    }
    
    private com.datasophon.plugins.api.model.HostCheckContext toPluginContext(HostCheckContext context) {
        return com.datasophon.plugins.api.model.HostCheckContext.builder()
                .hostIp(context.getHostIp())
                .clusterId(context.getClusterId() != null ? context.getClusterId().toString() : null)
                .sshUser(context.getSshUser())
                .sshPort(context.getSshPort())
                .sshPassword(context.getSshPassword())
                .build();
    }
}

