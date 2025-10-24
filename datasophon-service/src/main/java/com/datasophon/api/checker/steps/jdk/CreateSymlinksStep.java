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
    
    private final String installBasePath;
    
    public CreateSymlinksStep(String installBasePath) {
        this.installBasePath = installBasePath;
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
        
        // 检测实际的JDK目录名（排除软链接）
        String detectCommand = String.format(
                "find %s -maxdepth 1 -type d -name 'jdk*' ! -name 'jdk' 2>/dev/null | sort -r | head -1", 
                installBasePath);
        logWriter.logRepairCommand(context.getClusterId(), context.getHostIp(), "java",
                "检测实际JDK目录（排除软链接）: " + detectCommand);
        
        var detectResult = sshService.executeCommand(pluginContext, detectCommand);
        logWriter.logRepairOutput(context.getClusterId(), context.getHostIp(), "java", detectResult.output());
        
        if (!detectResult.isSuccess() || detectResult.output().trim().isEmpty()) {
            throw new Exception("无法检测JDK目录");
        }
        
        String javaHome = detectResult.output().trim();
        log.info("检测到实际JDK目录（非软链接）: {}", javaHome);
        
        // 创建java软链接（尝试sudo，如果失败不阻塞安装）
        String javaCommand = String.format("sudo ln -sf %s/bin/java /usr/bin/java 2>&1", javaHome);
        logWriter.logRepairCommand(context.getClusterId(), context.getHostIp(), "java", javaCommand);
        
        log.info("尝试创建java命令软链接: {} -> /usr/bin/java", javaHome + "/bin/java");
        
        var javaResult = sshService.executeCommand(pluginContext, javaCommand);
        logWriter.logRepairOutput(context.getClusterId(), context.getHostIp(), "java", javaResult.output());
        
        if (!javaResult.isSuccess()) {
            log.warn("创建java软链接失败（可能缺少sudo权限），但不影响JDK使用: {}", javaResult.error());
            Map<String, Object> warnInfo = new HashMap<>();
            warnInfo.put("warning", "软链接创建失败，JDK已安装但需要通过完整路径或环境变量使用");
            warnInfo.put("javaPath", javaHome + "/bin/java");
            logWriter.logRepairInfo(context.getClusterId(), context.getHostIp(), "java",
                    "软链接创建失败（缺少权限），不影响JDK使用", warnInfo);
        } else {
            log.info("java软链接创建成功");
        }
        
        // 创建javac软链接
        String javacCommand = String.format("sudo ln -sf %s/bin/javac /usr/bin/javac 2>&1", javaHome);
        logWriter.logRepairCommand(context.getClusterId(), context.getHostIp(), "java", javacCommand);
        
        log.info("尝试创建javac命令软链接: {} -> /usr/bin/javac", javaHome + "/bin/javac");
        
        var javacResult = sshService.executeCommand(pluginContext, javacCommand);
        logWriter.logRepairOutput(context.getClusterId(), context.getHostIp(), "java", javacResult.output());
        
        if (!javacResult.isSuccess()) {
            log.warn("创建javac软链接失败（可能缺少sudo权限），但不影响JDK使用: {}", javacResult.error());
        } else {
            log.info("javac软链接创建成功");
        }
        
        // 验证软链接是否创建成功
        String verifyCommand = "ls -l /usr/bin/java /usr/bin/javac 2>&1";
        logWriter.logRepairCommand(context.getClusterId(), context.getHostIp(), "java", 
                "验证系统软链接: " + verifyCommand);
        
        var verifyResult = sshService.executeCommand(pluginContext, verifyCommand);
        logWriter.logRepairOutput(context.getClusterId(), context.getHostIp(), "java", verifyResult.output());
        
        // 检查验证结果
        if (verifyResult.isSuccess() && verifyResult.output().contains("java") && verifyResult.output().contains("javac")) {
            Map<String, Object> verifyInfo = new HashMap<>();
            verifyInfo.put("verification", verifyResult.output());
            logWriter.logRepairInfo(context.getClusterId(), context.getHostIp(), "java",
                    "✅ 验证成功：系统软链接已创建", verifyInfo);
            log.info("✅ 系统软链接验证成功");
        } else {
            logWriter.logRepairInfo(context.getClusterId(), context.getHostIp(), "java",
                    "⚠️ 软链接验证失败或部分失败（但不影响JDK使用，可通过环境变量访问）", null);
            log.warn("⚠️ 软链接验证失败，但不影响JDK通过环境变量使用");
        }
        
        log.info("软链接创建步骤完成");
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

