package com.datasophon.api.checker.steps.jdk;

import com.datasophon.api.checker.HostCheckContext;
import com.datasophon.api.checker.RepairStep;
import com.datasophon.api.checker.util.CheckLogWriter;
import com.datasophon.plugins.api.service.SshConnectionService;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置环境变量步骤
 * 
 * @author 任相鹏
 * @date 2025-10-24
 */
@Slf4j
public class ConfigureEnvStep implements RepairStep {
    
    private final String javaHome;
    
    public ConfigureEnvStep(String javaHome) {
        this.javaHome = javaHome;
    }
    
    @Override
    public String getStepName() {
        return "配置JAVA环境变量";
    }
    
    @Override
    public String getStepDescription() {
        return "将JAVA_HOME等环境变量写入/etc/profile";
    }
    
    @Override
    public void execute(HostCheckContext context, SshConnectionService sshService, CheckLogWriter logWriter) throws Exception {
        var pluginContext = toPluginContext(context);
        
        // 检查是否已经配置过JDK环境变量
        String checkCommand = "grep -q 'JAVA_HOME=" + javaHome + "' /etc/profile && echo 'EXISTS' || echo 'NOT_EXISTS'";
        var checkResult = sshService.executeCommand(pluginContext, checkCommand);
        
        if (checkResult.output().contains("EXISTS")) {
            log.info("JAVA环境变量已存在，跳过配置");
            logWriter.logRepairInfo(context.getClusterId(), context.getHostIp(), "java", 
                    "JAVA环境变量已存在，跳过配置", null);
            return;
        }
        
        // 配置环境变量
        StringBuilder envConfig = new StringBuilder();
        envConfig.append("sudo bash -c 'cat >> /etc/profile << \"EOF\"\n");
        envConfig.append("\n# JDK Environment - Added by DataSophon\n");
        envConfig.append("export JAVA_HOME=").append(javaHome).append("\n");
        envConfig.append("export JRE_HOME=\\${JAVA_HOME}/jre\n");
        envConfig.append("export CLASSPATH=.:\\${JAVA_HOME}/lib:\\${JRE_HOME}/lib\n");
        envConfig.append("export PATH=\\${JAVA_HOME}/bin:\\$PATH\n");
        envConfig.append("EOF'\n");
        
        String command = envConfig.toString();
        
        Map<String, Object> commandInfo = new HashMap<>();
        commandInfo.put("javaHome", javaHome);
        commandInfo.put("configFile", "/etc/profile");
        logWriter.logRepairCommand(context.getClusterId(), context.getHostIp(), "java", command);
        
        log.info("开始配置JAVA环境变量: JAVA_HOME={}", javaHome);
        
        var result = sshService.executeCommand(pluginContext, command);
        logWriter.logRepairOutput(context.getClusterId(), context.getHostIp(), "java", result.output());
        
        if (!result.isSuccess()) {
            throw new Exception("配置环境变量失败: " + result.error());
        }
        
        log.info("JAVA环境变量配置成功");
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

