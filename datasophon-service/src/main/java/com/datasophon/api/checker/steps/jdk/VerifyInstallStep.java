package com.datasophon.api.checker.steps.jdk;

import com.datasophon.api.checker.HostCheckContext;
import com.datasophon.api.checker.RepairStep;
import com.datasophon.api.checker.util.CheckLogWriter;
import com.datasophon.plugins.api.service.SshConnectionService;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 验证JDK安装步骤
 * 
 * @author 任相鹏
 * @date 2025-10-24
 */
@Slf4j
public class VerifyInstallStep implements RepairStep {
    
    @Override
    public String getStepName() {
        return "验证JDK安装";
    }
    
    @Override
    public String getStepDescription() {
        return "执行java -version验证JDK安装是否成功";
    }
    
    @Override
    public void execute(HostCheckContext context, SshConnectionService sshService, CheckLogWriter logWriter) throws Exception {
        var pluginContext = toPluginContext(context);
        
        // 重新加载用户环境变量并执行java -version
        String command = "source ~/.bashrc && java -version 2>&1";
        
        logWriter.logRepairCommand(context.getClusterId(), context.getHostIp(), "java", command);
        
        log.info("验证JDK安装: 加载 ~/.bashrc 后执行 java -version");
        
        var result = sshService.executeCommand(pluginContext, command);
        
        Map<String, Object> verifyInfo = new HashMap<>();
        verifyInfo.put("output", result.output());
        logWriter.logRepairOutput(context.getClusterId(), context.getHostIp(), "java", result.output());
        
        if (!result.isSuccess() || !result.output().contains("version")) {
            throw new Exception("JDK安装验证失败，无法执行java命令: " + result.error());
        }
        
        // 解析版本号
        String versionOutput = result.output();
        log.info("JDK安装验证成功，版本信息: {}", versionOutput.split("\n")[0]);
        
        Map<String, Object> successInfo = new HashMap<>();
        successInfo.put("versionInfo", versionOutput.split("\n")[0]);
        successInfo.put("configFile", "~/.bashrc");
        logWriter.logRepairSuccess(context.getClusterId(), context.getHostIp(), "java", 
                "JDK安装验证成功", successInfo);
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

