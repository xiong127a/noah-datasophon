package com.datasophon.api.agent.steps;

import com.datasophon.api.agent.AgentDistributionContext;
import com.datasophon.api.agent.AgentDistributionStep;
import com.datasophon.api.agent.util.AgentLogWriter;
import com.datasophon.common.Constants;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.service.SshConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 上传Agent包到目标主机步骤
 * 通过SSH将Agent包从Master传输到目标主机
 * 
 * @author DataSophon Team
 * @date 2025-01-24
 */
@Slf4j
@RequiredArgsConstructor
public class UploadAgentStep implements AgentDistributionStep {
    
    private final SshConnectionService sshService;
    
    @Override
    public String getStepName() {
        return "上传Agent包";
    }
    
    @Override
    public void execute(AgentDistributionContext context) throws Exception {
        AgentLogWriter logWriter = context.getLogWriter();
        Long clusterId = context.getClusterId();
        String hostIp = context.getHostIp();
        
        String localPackagePath = context.getLocalPackagePath();
        String remoteInstallPath = context.getRemoteInstallPath();
        String remotePackagePath = remoteInstallPath + Constants.SLASH + Constants.WORKER_PACKAGE_NAME;
        
        File localFile = new File(localPackagePath);
        if (!localFile.exists()) {
            throw new Exception("本地Agent包不存在: " + localPackagePath);
        }
        
        long totalSize = localFile.length();
        
        Map<String, Object> uploadInfo = new HashMap<>();
        uploadInfo.put("localPath", localPackagePath);
        uploadInfo.put("remotePath", remotePackagePath);
        uploadInfo.put("size", formatFileSize(totalSize));
        logWriter.logInfo(clusterId, hostIp, "upload", 
                "开始上传Agent包到目标主机", uploadInfo);
        
        log.info("开始上传Agent包: {} -> {}:{}, 大小: {}", 
                localPackagePath, hostIp, remotePackagePath, formatFileSize(totalSize));
        
        try {
            // ====== 1. 上传Agent包 ======
            uploadWithProgress(context, localPackagePath, remotePackagePath, totalSize);
            log.info("Agent包上传成功: {}", remotePackagePath);
            
            // ====== 2. 上传MD5文件 ======
            String localMd5Path = localPackagePath + ".md5";
            String remoteMd5Path = remotePackagePath + ".md5";
            File md5File = new File(localMd5Path);
            
            if (md5File.exists()) {
                log.info("上传MD5文件: {} -> {}", localMd5Path, remoteMd5Path);
                HostCheckContext pluginContext = toPluginContext(context);
                boolean md5Success = sshService.uploadFile(pluginContext, localMd5Path, remoteMd5Path);
                
                if (md5Success) {
                    log.info("MD5文件上传成功: {}", remoteMd5Path);
                } else {
                    log.warn("MD5文件上传失败: {}", remoteMd5Path);
                }
            } else {
                log.warn("本地MD5文件不存在，跳过上传: {}", localMd5Path);
            }
            
            Map<String, Object> completeInfo = new HashMap<>();
            completeInfo.put("packagePath", remotePackagePath);
            completeInfo.put("packageSize", formatFileSize(totalSize));
            completeInfo.put("md5Path", remoteMd5Path);
            logWriter.logSuccess(clusterId, hostIp, "upload", 
                    "Agent包和MD5文件上传完成", completeInfo);
            
        } catch (Exception e) {
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", e.getMessage());
            errorInfo.put("remotePath", remotePackagePath);
            logWriter.logError(clusterId, hostIp, "upload", 
                    "上传Agent包失败: " + e.getMessage(), errorInfo);
            throw new Exception("上传Agent包失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 带进度的文件上传
     */
    private void uploadWithProgress(AgentDistributionContext context, String localPath, 
                                     String remotePath, long totalSize) throws Exception {
        AgentLogWriter logWriter = context.getLogWriter();
        Long clusterId = context.getClusterId();
        String hostIp = context.getHostIp();
        
        // 转换为Plugin需要的Context
        HostCheckContext pluginContext = toPluginContext(context);
        
        // 小文件直接上传
        if (totalSize < 10 * 1024 * 1024) { // 小于10MB
            boolean success = sshService.uploadFile(pluginContext, localPath, remotePath);
            if (!success) {
                throw new Exception("SSH上传失败");
            }
            return;
        }
        
        // 大文件：启动异步上传并模拟进度记录
        log.info("上传大文件，总大小: {}, 将记录上传进度", formatFileSize(totalSize));
        
        final boolean[] uploadSuccess = {false};
        final Exception[] uploadException = {null};
        
        // 启动上传线程
        Thread uploadThread = new Thread(() -> {
            try {
                uploadSuccess[0] = sshService.uploadFile(pluginContext, localPath, remotePath);
            } catch (Exception e) {
                uploadException[0] = e;
            }
        });
        uploadThread.start();
        
        // 模拟进度记录（每1秒记录一次）
        long startTime = System.currentTimeMillis();
        int lastReportedProgress = 0;
        
        // 假设平均上传速度为 2MB/s
        double estimatedSpeedMBPerSec = 2.0;
        double totalSizeMB = totalSize / (1024.0 * 1024.0);
        
        while (uploadThread.isAlive()) {
            Thread.sleep(1000); // 每1秒更新一次
            long elapsedTime = System.currentTimeMillis() - startTime;
            double elapsedSeconds = elapsedTime / 1000.0;
            
            // 估算已传输大小（基于时间和速度，最多95%）
            double uploadedMB = Math.min(totalSizeMB * 0.95, elapsedSeconds * estimatedSpeedMBPerSec);
            long uploadedBytes = (long) (uploadedMB * 1024 * 1024);
            
            // 计算进度百分比（最多95%，防止超过实际进度）
            int estimatedProgress = Math.min(95, (int) (uploadedMB / totalSizeMB * 100));
            
            // 只在进度变化时才推送日志
            if (estimatedProgress > lastReportedProgress) {
                lastReportedProgress = estimatedProgress;
                
                logWriter.logProgress(clusterId, hostIp, "upload",
                        estimatedProgress, uploadedBytes, totalSize,
                        String.format("上传中... %s / %s (%d%%)",
                                formatFileSize(uploadedBytes),
                                formatFileSize(totalSize),
                                estimatedProgress));
                
                log.info("上传进度: {}%, {} / {}, 耗时: {}s", 
                        estimatedProgress, 
                        formatFileSize(uploadedBytes),
                        formatFileSize(totalSize),
                        elapsedSeconds);
            }
        }
        
        // 检查上传结果
        if (uploadException[0] != null) {
            throw uploadException[0];
        }
        
        if (!uploadSuccess[0]) {
            throw new Exception("SSH上传失败");
        }
        
        // 上传完成，记录100%进度
        logWriter.logProgress(clusterId, hostIp, "upload",
                100, totalSize, totalSize, "文件上传完成");
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
    
    /**
     * 格式化文件大小
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}

