package com.datasophon.api.checker.steps.jdk;

import com.datasophon.api.checker.HostCheckContext;
import com.datasophon.api.checker.RepairStep;
import com.datasophon.api.checker.util.CheckLogWriter;
import com.datasophon.plugins.api.service.SshConnectionService;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 创建临时目录步骤
 * 
 * @author 任相鹏
 * @date 2025-10-24
 */
@Slf4j
public class CreateTempDirStep implements RepairStep {
    
    private final String tempDir;
    
    public CreateTempDirStep(String tempDir) {
        this.tempDir = tempDir;
    }
    
    @Override
    public String getStepName() {
        return "创建临时目录";
    }
    
    @Override
    public String getStepDescription() {
        return "创建临时目录用于下载和解压JDK安装包";
    }
    
    @Override
    public void execute(HostCheckContext context, SshConnectionService sshService, CheckLogWriter logWriter) throws Exception {
        var pluginContext = toPluginContext(context);
        String command = String.format("mkdir -p %s && echo 'Temp directory created: %s'", tempDir, tempDir);
        
        Map<String, Object> commandInfo = new HashMap<>();
        commandInfo.put("command", command);
        commandInfo.put("tempDir", tempDir);
        logWriter.logRepairCommand(context.getClusterId(), context.getHostIp(), "java", command);
        
        var result = sshService.executeCommand(pluginContext, command);
        
        Map<String, Object> outputInfo = new HashMap<>();
        outputInfo.put("output", result.output());
        logWriter.logRepairOutput(context.getClusterId(), context.getHostIp(), "java", result.output());
        
        if (!result.isSuccess()) {
            throw new Exception("创建临时目录失败: " + result.error());
        }
        
        log.info("临时目录创建成功: {}", tempDir);
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

