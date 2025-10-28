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
        
        // 小文件直接上传（小于10MB）
        if (totalSize < 10 * 1024 * 1024) {
            boolean success = sshService.uploadFile(pluginContext, localPath, remotePath);
            if (!success) {
                throw new Exception("SSH上传失败");
            }
            logWriter.logProgress(clusterId, hostIp, "upload",
                    100, totalSize, totalSize, "文件上传完成");
            return;
        }
        
        // 大文件：使用带进度的真实上传
        log.info("上传大文件，总大小: {}, 将记录实时上传进度", formatFileSize(totalSize));
        
        // 用于计算速率和剩余时间的变量
        final long[] lastUploadedBytes = {0};
        final long[] lastUpdateTime = {System.currentTimeMillis()};
        
        try (InputStream inputStream = new FileInputStream(localPath)) {
            boolean success = sshService.uploadFileFromStream(
                    pluginContext, 
                    inputStream, 
                    remotePath, 
                    totalSize,
                    (uploadedBytes, totalBytesParam, progress) -> {
                        long currentTime = System.currentTimeMillis();
                        long timeDelta = currentTime - lastUpdateTime[0];
                        
                        // 至少间隔500ms或每传512KB才更新一次（避免过于频繁）
                        if (timeDelta >= 500 || uploadedBytes - lastUploadedBytes[0] >= 512 * 1024) {
                            long bytesDelta = uploadedBytes - lastUploadedBytes[0];
                            
                            // 计算实时速率 (bytes/s)
                            double currentSpeed = timeDelta > 0 ? (bytesDelta * 1000.0 / timeDelta) : 0;
                            
                            // 计算预计剩余时间
                            long remainingBytes = totalBytesParam - uploadedBytes;
                            long estimatedRemainingSeconds = currentSpeed > 0 ? (long) (remainingBytes / currentSpeed) : 0;
                            
                            // 格式化速率和剩余时间
                            String speedStr = formatSpeed(currentSpeed);
                            String remainingTimeStr = formatDuration(estimatedRemainingSeconds * 1000);
                            
                            // 记录进度
                            logWriter.logProgress(clusterId, hostIp, "upload",
                                    progress, uploadedBytes, totalBytesParam,
                                    String.format("上传Agent包... %s / %s (%d%%) | 速率: %s | 剩余: %s",
                                            formatFileSize(uploadedBytes),
                                            formatFileSize(totalBytesParam),
                                            progress,
                                            speedStr,
                                            remainingTimeStr));
                            
                            // 更新上次记录的值
                            lastUploadedBytes[0] = uploadedBytes;
                            lastUpdateTime[0] = currentTime;
                        }
                    }
            );
            
            if (!success) {
                throw new Exception("SSH上传失败");
            }
            
            // 上传完成，记录100%进度
            logWriter.logProgress(clusterId, hostIp, "upload",
                    100, totalSize, totalSize, "文件上传完成");
        }
    }
    
    /**
     * 格式化速率
     */
    private String formatSpeed(double bytesPerSecond) {
        if (bytesPerSecond < 1024) {
            return String.format("%.0f B/s", bytesPerSecond);
        } else if (bytesPerSecond < 1024 * 1024) {
            return String.format("%.2f KB/s", bytesPerSecond / 1024.0);
        } else {
            return String.format("%.2f MB/s", bytesPerSecond / (1024.0 * 1024.0));
        }
    }
    
    /**
     * 格式化时长
     */
    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        if (seconds < 60) {
            return seconds + "秒";
        } else {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            return String.format("%d分%d秒", minutes, remainingSeconds);
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

