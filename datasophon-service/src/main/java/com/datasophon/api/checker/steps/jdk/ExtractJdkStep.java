package com.datasophon.api.checker.steps.jdk;

import com.datasophon.api.checker.HostCheckContext;
import com.datasophon.api.checker.RepairStep;
import com.datasophon.api.checker.util.CheckLogWriter;
import com.datasophon.plugins.api.service.SshConnectionService;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 解压JDK步骤
 * 
 * @author 任相鹏
 * @date 2025-10-24
 */
@Slf4j
public class ExtractJdkStep implements RepairStep {
    
    private final String tempDir;
    private final String jdkFileName;
    private final String installPath;
    
    public ExtractJdkStep(String tempDir, String jdkFileName, String installPath) {
        this.tempDir = tempDir;
        this.jdkFileName = jdkFileName;
        this.installPath = installPath;
    }
    
    @Override
    public String getStepName() {
        return "解压JDK安装包";
    }
    
    @Override
    public String getStepDescription() {
        return String.format("解压JDK安装包到目标目录 %s", installPath);
    }
    
    @Override
    public void execute(HostCheckContext context, SshConnectionService sshService, CheckLogWriter logWriter) throws Exception {
        var pluginContext = toPluginContext(context);
        
        // 创建安装目录（如果用户有权限，不使用sudo）
        String mkdirCommand = String.format("mkdir -p %s 2>&1 || sudo mkdir -p %s", installPath, installPath);
        logWriter.logRepairCommand(context.getClusterId(), context.getHostIp(), "java", mkdirCommand);
        
        var mkdirResult = sshService.executeCommand(pluginContext, mkdirCommand);
        logWriter.logRepairOutput(context.getClusterId(), context.getHostIp(), "java", mkdirResult.output());
        
        if (!mkdirResult.isSuccess()) {
            throw new Exception("创建安装目录失败: " + mkdirResult.error());
        }
        
        // 解压JDK（先尝试不用sudo，如果失败则使用sudo）
        String extractCommand = String.format("cd %s && tar -zxf %s -C %s 2>&1 || sudo tar -zxf %s -C %s 2>&1", 
                tempDir, jdkFileName, installPath, jdkFileName, installPath);
        
        Map<String, Object> commandInfo = new HashMap<>();
        commandInfo.put("command", extractCommand);
        commandInfo.put("sourceFile", tempDir + "/" + jdkFileName);
        commandInfo.put("targetDir", installPath);
        commandInfo.put("note", "优先使用用户权限，必要时使用sudo");
        logWriter.logRepairCommand(context.getClusterId(), context.getHostIp(), "java", extractCommand);
        
        log.info("开始解压JDK: {}/{} -> {}", tempDir, jdkFileName, installPath);
        
        var result = sshService.executeCommand(pluginContext, extractCommand);
        logWriter.logRepairOutput(context.getClusterId(), context.getHostIp(), "java", result.output());
        
        if (!result.isSuccess()) {
            throw new Exception("解压JDK失败: " + result.error());
        }
        
        log.info("JDK解压成功: {}", installPath);
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

