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
        
        // 删除旧的软链接（如果存在），避免解压时出现"符号连接的层数过多"错误
        String symlinkPath = installPath + "/jdk";
        String removeOldSymlinkCommand = String.format("[ -L %s ] && (rm -f %s 2>&1 || sudo rm -f %s 2>&1) || echo 'No symlink to remove'", 
                symlinkPath, symlinkPath, symlinkPath);
        logWriter.logRepairCommand(context.getClusterId(), context.getHostIp(), "java", 
                "删除旧软链接（如果存在）: " + removeOldSymlinkCommand);
        
        var removeResult = sshService.executeCommand(pluginContext, removeOldSymlinkCommand);
        logWriter.logRepairOutput(context.getClusterId(), context.getHostIp(), "java", removeResult.output());
        log.info("清理旧软链接: {}", removeResult.output());
        
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
        
        // 检测实际解压的目录名（tar包内部目录名可能与文件名不同）
        // 例如：jdk-8u333-linux-x64.tar.gz 解压后实际目录可能是 jdk1.8.0_333
        // 重要：只查找真实目录，排除软链接（使用 find -type d）
        String detectCommand = String.format(
                "find %s -maxdepth 1 -type d -name 'jdk*' ! -name 'jdk' 2>/dev/null | sort -r | head -1", 
                installPath);
        logWriter.logRepairCommand(context.getClusterId(), context.getHostIp(), "java", 
                "检测实际JDK目录（排除软链接）: " + detectCommand);
        
        log.info("检测JDK实际解压目录名（排除软链接）");
        var detectResult = sshService.executeCommand(pluginContext, detectCommand);
        logWriter.logRepairOutput(context.getClusterId(), context.getHostIp(), "java", detectResult.output());
        
        if (!detectResult.isSuccess() || detectResult.output().trim().isEmpty()) {
            throw new Exception("无法检测JDK解压目录，请确认JDK已成功解压");
        }
        
        String actualJdkDir = detectResult.output().trim();
        log.info("检测到JDK实际目录: {}", actualJdkDir);
        
        // 创建统一的软链接 /usr/local/jdk -> 实际目录
        // 注意：此时旧软链接已被删除，可以安全创建新的
        String newSymlinkCommand = String.format("ln -sfn %s %s 2>&1 || sudo ln -sfn %s %s 2>&1", 
                actualJdkDir, symlinkPath, actualJdkDir, symlinkPath);
        
        Map<String, Object> symlinkInfo = new HashMap<>();
        symlinkInfo.put("actualDir", actualJdkDir);
        symlinkInfo.put("symlinkPath", symlinkPath);
        symlinkInfo.put("note", "创建新的软链接指向实际目录");
        logWriter.logRepairCommand(context.getClusterId(), context.getHostIp(), "java", newSymlinkCommand);
        
        log.info("创建JDK统一软链接: {} -> {}", symlinkPath, actualJdkDir);
        var symlinkResult = sshService.executeCommand(pluginContext, newSymlinkCommand);
        logWriter.logRepairOutput(context.getClusterId(), context.getHostIp(), "java", symlinkResult.output());
        
        if (!symlinkResult.isSuccess()) {
            log.warn("创建软链接失败，但不影响继续: {}", symlinkResult.error());
        } else {
            // 验证软链接是否真的创建成功
            String verifySymlinkCommand = String.format("ls -l %s | grep -E '^l'", symlinkPath);
            var verifySymlinkResult = sshService.executeCommand(pluginContext, verifySymlinkCommand);
            
            logWriter.logRepairCommand(context.getClusterId(), context.getHostIp(), "java",
                    "验证软链接: " + verifySymlinkCommand);
            logWriter.logRepairOutput(context.getClusterId(), context.getHostIp(), "java",
                    verifySymlinkResult.output());
            
            if (verifySymlinkResult.isSuccess()) {
                Map<String, Object> successInfo = new HashMap<>();
                successInfo.put("actualDir", actualJdkDir);
                successInfo.put("symlinkPath", symlinkPath);
                successInfo.put("verification", verifySymlinkResult.output());
                logWriter.logRepairInfo(context.getClusterId(), context.getHostIp(), "java",
                        "✅ 验证成功：JDK统一软链接已创建", successInfo);
            } else {
                log.warn("软链接验证失败，但不阻塞流程");
            }
        }
    }
    
    private com.datasophon.plugins.api.model.HostCheckContext toPluginContext(HostCheckContext context) {
        return com.datasophon.plugins.api.model.HostCheckContext.builder()
                .hostIp(context.getHostIp())
                .clusterId(context.getClusterId() != null ? context.getClusterId() : null)
                .sshUser(context.getSshUser())
                .sshPort(context.getSshPort())
                .sshPassword(context.getSshPassword())
                .build();
    }
}

