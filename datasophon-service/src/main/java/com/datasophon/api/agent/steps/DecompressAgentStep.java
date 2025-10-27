package com.datasophon.api.agent.steps;

import com.datasophon.api.agent.AgentDistributionContext;
import com.datasophon.api.agent.AgentDistributionStep;
import com.datasophon.api.agent.util.AgentLogWriter;
import com.datasophon.common.Constants;
import com.datasophon.plugins.api.model.CommandResult;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.service.SshConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 解压Agent包步骤
 * 在目标主机上解压Agent安装包
 * 
 * @author DataSophon Team
 * @date 2025-01-24
 */
@Slf4j
@RequiredArgsConstructor
public class DecompressAgentStep implements AgentDistributionStep {
    
    private final SshConnectionService sshService;
    
    @Override
    public String getStepName() {
        return "解压Agent包";
    }
    
    @Override
    public void execute(AgentDistributionContext context) throws Exception {
        AgentLogWriter logWriter = context.getLogWriter();
        Long clusterId = context.getClusterId();
        String hostIp = context.getHostIp();
        
        String remoteInstallPath = context.getRemoteInstallPath();
        
        Map<String, Object> decompressInfo = new HashMap<>();
        decompressInfo.put("installPath", remoteInstallPath);
        logWriter.logInfo(clusterId, hostIp, "decompress", 
                "开始解压Agent包", decompressInfo);
        log.info("开始解压Agent包: {} -> {}", hostIp, remoteInstallPath);
        
        try {
            // 转换为Plugin需要的Context
            HostCheckContext pluginContext = toPluginContext(context);
            
            // 执行解压命令
            String decompressCommand = Constants.UNZIP_DDH_WORKER_CMD;
            
            Map<String, Object> commandInfo = new HashMap<>();
            commandInfo.put("command", decompressCommand);
            logWriter.logCommand(clusterId, hostIp, "decompress", decompressCommand);
            
            CommandResult decompressResult = sshService.executeCommand(pluginContext, decompressCommand);
            
            if (!decompressResult.isSuccess()) {
                Map<String, Object> outputInfo = new HashMap<>();
                outputInfo.put("output", decompressResult.output());
                outputInfo.put("error", decompressResult.error());
                logWriter.logError(clusterId, hostIp, "decompress", 
                        "解压失败: " + decompressResult.error(), outputInfo);
                throw new Exception("解压Agent包失败: " + decompressResult.error());
            }
            
            // 验证解压结果
            String verifyCommand = "ls -la " + remoteInstallPath + "/datasophon-worker";
            CommandResult verifyResult = sshService.executeCommand(pluginContext, verifyCommand);
            
            if (!verifyResult.isSuccess()) {
                throw new Exception("解压后目录验证失败，datasophon-worker目录不存在");
            }
            
            Map<String, Object> successInfo = new HashMap<>();
            successInfo.put("installPath", remoteInstallPath);
            successInfo.put("workerPath", remoteInstallPath + "/datasophon-worker");
            logWriter.logSuccess(clusterId, hostIp, "decompress", 
                    "Agent包解压成功", successInfo);
            log.info("Agent包解压成功: {} -> {}/datasophon-worker", hostIp, remoteInstallPath);
            
        } catch (Exception e) {
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", e.getMessage());
            errorInfo.put("installPath", remoteInstallPath);
            logWriter.logError(clusterId, hostIp, "decompress", 
                    "解压Agent包失败: " + e.getMessage(), errorInfo);
            throw new Exception("解压Agent包失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将AgentDistributionContext转换为HostCheckContext
     */
    private HostCheckContext toPluginContext(AgentDistributionContext context) {
        return HostCheckContext.builder()
                .clusterId(context.getClusterId())
                .hostIp(context.getHostIp())
                .hostname(context.getHostname())
                .sshUser(context.getSshUser())
                .sshPort(context.getSshPort())
                .sshPassword(context.getSshPassword())
                .build();
    }
}

