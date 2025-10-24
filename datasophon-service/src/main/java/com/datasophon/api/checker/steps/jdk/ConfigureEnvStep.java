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
    
    private final String installBasePath;
    
    public ConfigureEnvStep(String installBasePath) {
        this.installBasePath = installBasePath;
    }
    
    @Override
    public String getStepName() {
        return "配置JAVA环境变量";
    }
    
    @Override
    public String getStepDescription() {
        return "将JAVA_HOME等环境变量写入用户配置文件 ~/.bashrc";
    }
    
    @Override
    public void execute(HostCheckContext context, SshConnectionService sshService, CheckLogWriter logWriter) throws Exception {
        var pluginContext = toPluginContext(context);
        
        // 检测实际的JDK目录名（解压后的实际目录，如 jdk1.8.0_333）
        // 重要：只查找真实目录，排除软链接（避免旧软链接干扰）
        String detectCommand = String.format(
                "find %s -maxdepth 1 -type d -name 'jdk*' ! -name 'jdk' 2>/dev/null | sort -r | head -1", 
                installBasePath);
        logWriter.logRepairCommand(context.getClusterId(), context.getHostIp(), "java",
                "检测实际JDK目录（排除软链接）: " + detectCommand);
        
        var detectResult = sshService.executeCommand(pluginContext, detectCommand);
        logWriter.logRepairOutput(context.getClusterId(), context.getHostIp(), "java", detectResult.output());
        
        if (!detectResult.isSuccess() || detectResult.output().trim().isEmpty()) {
            throw new Exception("无法检测JDK目录，请确认JDK已解压到 " + installBasePath);
        }
        
        String javaHome = detectResult.output().trim();
        log.info("检测到实际JDK目录（非软链接）: {}", javaHome);
        
        Map<String, Object> detectedInfo = new HashMap<>();
        detectedInfo.put("installBasePath", installBasePath);
        detectedInfo.put("actualJavaHome", javaHome);
        detectedInfo.put("note", "此为实际目录路径，非软链接");
        logWriter.logRepairInfo(context.getClusterId(), context.getHostIp(), "java",
                "检测到实际JDK目录", detectedInfo);
        
        // 检查是否已经配置过JDK环境变量（检查 ~/.bashrc）
        String checkCommand = "grep -q 'JAVA_HOME=" + javaHome + "' ~/.bashrc && echo 'EXISTS' || echo 'NOT_EXISTS'";
        var checkResult = sshService.executeCommand(pluginContext, checkCommand);
        
        if (checkResult.output().contains("EXISTS")) {
            log.info("检测到JAVA_HOME已存在，验证配置内容...");
            
            // 即使跳过配置，也要验证并显示 ~/.bashrc 内容
            String verifyCommand = "tail -15 ~/.bashrc";
            var verifyResult = sshService.executeCommand(pluginContext, verifyCommand);
            
            Map<String, Object> skipInfo = new HashMap<>();
            skipInfo.put("javaHome", javaHome);
            skipInfo.put("bashrcContent", verifyResult.output());
            
            logWriter.logRepairCommand(context.getClusterId(), context.getHostIp(), "java",
                    "验证已存在的环境变量: " + verifyCommand);
            logWriter.logRepairOutput(context.getClusterId(), context.getHostIp(), "java",
                    verifyResult.output());
            
            // 检查是否真的包含正确的 JAVA_HOME
            if (verifyResult.output().contains("JAVA_HOME=" + javaHome)) {
                logWriter.logRepairInfo(context.getClusterId(), context.getHostIp(), "java",
                        "✅ 验证通过：JAVA环境变量已存在且正确，跳过配置", skipInfo);
                log.info("JAVA环境变量已存在且验证通过，跳过配置");
                return;
            } else {
                // 检测到配置但路径不对，需要删除旧配置重新添加
                log.warn("检测到旧的JAVA配置但路径不匹配，将删除旧配置重新添加");
                logWriter.logRepairInfo(context.getClusterId(), context.getHostIp(), "java",
                        "⚠️ 检测到旧配置但路径不匹配，将删除DataSophon添加的旧配置", skipInfo);
                
                // 删除旧的 DataSophon 添加的配置
                String removeCommand = "sed -i '/# JDK Environment - Added by DataSophon/,+4d' ~/.bashrc";
                logWriter.logRepairCommand(context.getClusterId(), context.getHostIp(), "java", removeCommand);
                var removeResult = sshService.executeCommand(pluginContext, removeCommand);
                logWriter.logRepairOutput(context.getClusterId(), context.getHostIp(), "java", removeResult.output());
                
                log.info("旧配置已删除，继续添加新配置");
            }
        }
        
        // 配置环境变量到用户的 ~/.bashrc（不需要root权限）
        StringBuilder envConfig = new StringBuilder();
        envConfig.append("cat >> ~/.bashrc << 'EOF'\n");
        envConfig.append("\n# JDK Environment - Added by DataSophon\n");
        envConfig.append("export JAVA_HOME=").append(javaHome).append("\n");
        envConfig.append("export JRE_HOME=${JAVA_HOME}/jre\n");
        envConfig.append("export CLASSPATH=.:${JAVA_HOME}/lib:${JRE_HOME}/lib\n");
        envConfig.append("export PATH=${JAVA_HOME}/bin:$PATH\n");
        envConfig.append("EOF\n");
        
        String command = envConfig.toString();
        
        Map<String, Object> commandInfo = new HashMap<>();
        commandInfo.put("javaHome", javaHome);
        commandInfo.put("configFile", "~/.bashrc");
        commandInfo.put("note", "无需root权限");
        logWriter.logRepairCommand(context.getClusterId(), context.getHostIp(), "java", command);
        
        log.info("开始配置JAVA环境变量到 ~/.bashrc: JAVA_HOME={}", javaHome);
        
        var result = sshService.executeCommand(pluginContext, command);
        logWriter.logRepairOutput(context.getClusterId(), context.getHostIp(), "java", result.output());
        
        if (!result.isSuccess()) {
            throw new Exception("配置环境变量失败: " + result.error());
        }
        
        log.info("环境变量写入命令执行完成，开始验证...");
        
        // 验证步骤1：检查 ~/.bashrc 是否包含 JAVA_HOME
        String verifyCommand1 = "grep 'JAVA_HOME=" + javaHome + "' ~/.bashrc";
        var verifyResult1 = sshService.executeCommand(pluginContext, verifyCommand1);
        
        logWriter.logRepairCommand(context.getClusterId(), context.getHostIp(), "java", 
                "验证环境变量: " + verifyCommand1);
        logWriter.logRepairOutput(context.getClusterId(), context.getHostIp(), "java", 
                verifyResult1.output());
        
        if (!verifyResult1.isSuccess() || !verifyResult1.output().contains("JAVA_HOME")) {
            throw new Exception("验证失败：~/.bashrc 中未找到 JAVA_HOME 配置");
        }
        
        // 验证步骤2：显示 ~/.bashrc 的最后几行（JDK配置部分）
        String verifyCommand2 = "tail -10 ~/.bashrc";
        var verifyResult2 = sshService.executeCommand(pluginContext, verifyCommand2);
        
        Map<String, Object> verifyInfo = new HashMap<>();
        verifyInfo.put("bashrcContent", verifyResult2.output());
        verifyInfo.put("javaHome", javaHome);
        
        logWriter.logRepairInfo(context.getClusterId(), context.getHostIp(), "java",
                "✅ 验证成功：环境变量已写入 ~/.bashrc", verifyInfo);
        
        log.info("JAVA环境变量配置成功并已验证（已写入 ~/.bashrc）");
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

