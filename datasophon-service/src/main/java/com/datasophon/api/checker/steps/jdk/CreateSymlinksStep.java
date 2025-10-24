package com.datasophon.api.checker.steps.jdk;

import com.datasophon.api.checker.HostCheckContext;
import com.datasophon.api.checker.RepairStep;
import com.datasophon.api.checker.util.CheckLogWriter;
import com.datasophon.plugins.api.service.SshConnectionService;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 创建软链接步骤
 * 
 * @author 任相鹏
 * @date 2025-10-24
 */
@Slf4j
public class CreateSymlinksStep implements RepairStep {
    
    private final String javaHome;
    
    public CreateSymlinksStep(String javaHome) {
        this.javaHome = javaHome;
    }
    
    @Override
    public String getStepName() {
        return "创建JAVA命令软链接";
    }
    
    @Override
    public String getStepDescription() {
        return "创建java和javac命令的系统软链接";
    }
    
    @Override
    public void execute(HostCheckContext context, SshConnectionService sshService, CheckLogWriter logWriter) throws Exception {
        var pluginContext = toPluginContext(context);
        
        // 创建java软链接
        String javaCommand = String.format("sudo ln -sf %s/bin/java /usr/bin/java 2>&1", javaHome);
        logWriter.logRepairCommand(context.getClusterId(), context.getHostIp(), "java", javaCommand);
        
        log.info("创建java命令软链接: {} -> /usr/bin/java", javaHome + "/bin/java");
        
        var javaResult = sshService.executeCommand(pluginContext, javaCommand);
        logWriter.logRepairOutput(context.getClusterId(), context.getHostIp(), "java", javaResult.output());
        
        if (!javaResult.isSuccess()) {
            throw new Exception("创建java软链接失败: " + javaResult.error());
        }
        
        // 创建javac软链接
        String javacCommand = String.format("sudo ln -sf %s/bin/javac /usr/bin/javac 2>&1", javaHome);
        logWriter.logRepairCommand(context.getClusterId(), context.getHostIp(), "java", javacCommand);
        
        log.info("创建javac命令软链接: {} -> /usr/bin/javac", javaHome + "/bin/javac");
        
        var javacResult = sshService.executeCommand(pluginContext, javacCommand);
        logWriter.logRepairOutput(context.getClusterId(), context.getHostIp(), "java", javacResult.output());
        
        if (!javacResult.isSuccess()) {
            throw new Exception("创建javac软链接失败: " + javacResult.error());
        }
        
        // 验证软链接
        String verifyCommand = "ls -l /usr/bin/java /usr/bin/javac 2>&1";
        var verifyResult = sshService.executeCommand(pluginContext, verifyCommand);
        logWriter.logRepairOutput(context.getClusterId(), context.getHostIp(), "java", verifyResult.output());
        
        log.info("软链接创建成功");
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

