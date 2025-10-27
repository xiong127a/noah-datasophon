package com.datasophon.api.agent.steps;

import cn.hutool.core.io.FileUtil;
import com.datasophon.api.agent.AgentDistributionContext;
import com.datasophon.api.agent.AgentDistributionStep;
import com.datasophon.api.agent.util.AgentLogWriter;
import com.datasophon.common.Constants;
import com.datasophon.plugins.api.model.CommandResult;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.service.SshConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * MD5校验步骤
 * 验证上传到目标主机的Agent包完整性
 * 
 * @author DataSophon Team
 * @date 2025-01-24
 */
@Slf4j
@RequiredArgsConstructor
public class VerifyMd5Step implements AgentDistributionStep {
    
    private final SshConnectionService sshService;
    
    @Override
    public String getStepName() {
        return "MD5校验";
    }
    
    @Override
    public void execute(AgentDistributionContext context) throws Exception {
        AgentLogWriter logWriter = context.getLogWriter();
        Long clusterId = context.getClusterId();
        String hostIp = context.getHostIp();
        
        logWriter.logInfo(clusterId, hostIp, "md5", "开始MD5校验", null);
        log.info("开始MD5校验: {}", hostIp);
        
        try {
            // 转换为Plugin需要的Context
            HostCheckContext pluginContext = toPluginContext(context);
            
            // 1. 执行远程MD5命令
            String md5Command = Constants.CHECK_WORKER_MD5_CMD;
            
            Map<String, Object> commandInfo = new HashMap<>();
            commandInfo.put("command", md5Command);
            logWriter.logCommand(clusterId, hostIp, "md5", md5Command);
            
            CommandResult md5Result = sshService.executeCommand(pluginContext, md5Command);
            
            if (!md5Result.isSuccess()) {
                throw new Exception("执行MD5命令失败: " + md5Result.output());
            }
            
            String remoteMd5 = md5Result.output().trim().split("\\s+")[0]; // 取第一个字段（MD5值）
            
            Map<String, Object> remoteMd5Info = new HashMap<>();
            remoteMd5Info.put("remoteMd5", remoteMd5);
            logWriter.logInfo(clusterId, hostIp, "md5", 
                    "远程MD5: " + remoteMd5, remoteMd5Info);
            
            // 2. 读取本地MD5文件
            String localMd5FilePath = Constants.MASTER_MANAGE_PACKAGE_PATH + 
                    Constants.SLASH + Constants.WORKER_PACKAGE_NAME + ".md5";
            String localMd5 = FileUtil.readString(localMd5FilePath, Charset.defaultCharset()).trim();
            
            Map<String, Object> localMd5Info = new HashMap<>();
            localMd5Info.put("localMd5", localMd5);
            logWriter.logInfo(clusterId, hostIp, "md5", 
                    "本地MD5: " + localMd5, localMd5Info);
            
            // 3. 比对MD5值
            if (!localMd5.equals(remoteMd5)) {
                Map<String, Object> mismatchInfo = new HashMap<>();
                mismatchInfo.put("localMd5", localMd5);
                mismatchInfo.put("remoteMd5", remoteMd5);
                logWriter.logError(clusterId, hostIp, "md5", 
                        "MD5校验失败：值不匹配", mismatchInfo);
                throw new Exception("MD5校验失败：本地=" + localMd5 + ", 远程=" + remoteMd5);
            }
            
            Map<String, Object> successInfo = new HashMap<>();
            successInfo.put("md5", localMd5);
            logWriter.logSuccess(clusterId, hostIp, "md5", 
                    "MD5校验成功", successInfo);
            log.info("MD5校验成功: {}, MD5={}", hostIp, localMd5);
            
        } catch (Exception e) {
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", e.getMessage());
            logWriter.logError(clusterId, hostIp, "md5", 
                    "MD5校验失败: " + e.getMessage(), errorInfo);
            throw new Exception("MD5校验失败: " + e.getMessage(), e);
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

