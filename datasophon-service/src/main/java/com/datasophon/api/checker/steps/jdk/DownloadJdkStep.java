package com.datasophon.api.checker.steps.jdk;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.datasophon.api.checker.HostCheckContext;
import com.datasophon.api.checker.RepairStep;
import com.datasophon.api.checker.util.CheckLogWriter;
import com.datasophon.plugins.api.service.SshConnectionService;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 下载JDK包步骤
 * 对于HTTP存储库：先在API服务器下载到内存，然后通过SSH上传到目标主机
 * 对于本地存储库：直接在目标主机使用cp命令复制
 * 
 * @author 任相鹏
 * @date 2025-10-24
 */
@Slf4j
public class DownloadJdkStep implements RepairStep {
    
    private final String tempDir;
    private final String jdkFileName;
    private final String downloadUrl;
    private final boolean isHttp;
    
    public DownloadJdkStep(String tempDir, String jdkFileName, String downloadUrl, boolean isHttp) {
        this.tempDir = tempDir;
        this.jdkFileName = jdkFileName;
        this.downloadUrl = downloadUrl;
        this.isHttp = isHttp;
    }
    
    @Override
    public String getStepName() {
        return isHttp ? "下载并传输JDK" : "从本地存储库复制JDK";
    }
    
    @Override
    public String getStepDescription() {
        return isHttp 
            ? "在API服务器下载JDK，然后通过SSH传输到目标主机"
            : "从本地存储库复制JDK安装包到临时目录";
    }
    
    @Override
    public void execute(HostCheckContext context, SshConnectionService sshService, CheckLogWriter logWriter) throws Exception {
        var pluginContext = toPluginContext(context);
        
        if (isHttp) {
            // HTTP存储库：先下载到API服务器，然后通过SSH上传
            executeHttpDownloadAndUpload(context, sshService, logWriter, pluginContext);
        } else {
            // 本地存储库：直接在目标主机使用cp命令
            executeLocalCopy(context, sshService, logWriter, pluginContext);
        }
    }
    
    /**
     * HTTP下载并上传
     */
    private void executeHttpDownloadAndUpload(HostCheckContext context, SshConnectionService sshService, 
                                              CheckLogWriter logWriter, com.datasophon.plugins.api.model.HostCheckContext pluginContext) throws Exception {
        
        // 1. 在API服务器下载JDK包
        Map<String, Object> downloadInfo = new HashMap<>();
        downloadInfo.put("downloadUrl", downloadUrl);
        downloadInfo.put("method", "HTTP下载到API服务器");
        logWriter.logRepairInfo(context.getClusterId(), context.getHostIp(), "java", 
                "开始从远程存储库下载JDK到API服务器", downloadInfo);
        
        log.info("开始下载JDK到API服务器: {}", downloadUrl);
        
        byte[] jdkData;
        try {
            HttpResponse response = HttpRequest.get(downloadUrl)
                    .timeout(300000) // 5分钟超时
                    .execute();
            
            if (!response.isOk()) {
                throw new Exception("HTTP下载失败，状态码: " + response.getStatus());
            }
            
            jdkData = response.bodyBytes();
            
            Map<String, Object> downloadResult = new HashMap<>();
            downloadResult.put("size", formatFileSize(jdkData.length));
            downloadResult.put("sizeBytes", jdkData.length);
            logWriter.logRepairInfo(context.getClusterId(), context.getHostIp(), "java", 
                    "JDK下载完成", downloadResult);
            
            log.info("JDK下载完成，大小: {} bytes", jdkData.length);
            
        } catch (Exception e) {
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", e.getMessage());
            errorInfo.put("downloadUrl", downloadUrl);
            logWriter.logRepairError(context.getClusterId(), context.getHostIp(), "java", 
                    "下载JDK失败: " + e.getMessage(), errorInfo);
            throw new Exception("从远程存储库下载JDK失败: " + e.getMessage(), e);
        }
        
        // 2. 通过SSH上传到目标主机（带进度）
        String remotePath = tempDir + "/" + jdkFileName;
        Map<String, Object> uploadInfo = new HashMap<>();
        uploadInfo.put("remotePath", remotePath);
        uploadInfo.put("size", formatFileSize(jdkData.length));
        uploadInfo.put("sizeBytes", jdkData.length);
        logWriter.logRepairInfo(context.getClusterId(), context.getHostIp(), "java", 
                "开始通过SSH上传JDK到目标主机", uploadInfo);
        
        log.info("开始上传JDK到目标主机: {} -> {}, 大小: {}", 
                context.getHostIp(), remotePath, formatFileSize(jdkData.length));
        
        try {
            uploadWithProgress(context, sshService, logWriter, pluginContext, jdkData, remotePath);
            
            Map<String, Object> completeInfo = new HashMap<>();
            completeInfo.put("remotePath", remotePath);
            completeInfo.put("size", formatFileSize(jdkData.length));
            logWriter.logRepairInfo(context.getClusterId(), context.getHostIp(), "java", 
                    "JDK上传完成", completeInfo);
            log.info("JDK上传成功: {}", remotePath);
            
        } catch (Exception e) {
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", e.getMessage());
            errorInfo.put("remotePath", remotePath);
            logWriter.logRepairError(context.getClusterId(), context.getHostIp(), "java", 
                    "上传JDK失败: " + e.getMessage(), errorInfo);
            throw new Exception("通过SSH上传JDK失败: " + e.getMessage(), e);
        }
        
        // 3. 验证文件是否存在
        verifyFile(context, sshService, logWriter, pluginContext, remotePath);
    }
    
    /**
     * 本地复制
     */
    private void executeLocalCopy(HostCheckContext context, SshConnectionService sshService, 
                                   CheckLogWriter logWriter, com.datasophon.plugins.api.model.HostCheckContext pluginContext) throws Exception {
        
        String command = String.format("cp '%s' %s/%s 2>&1", downloadUrl, tempDir, jdkFileName);
        
        Map<String, Object> commandInfo = new HashMap<>();
        commandInfo.put("command", command);
        commandInfo.put("sourcePath", downloadUrl);
        commandInfo.put("targetPath", tempDir + "/" + jdkFileName);
        logWriter.logRepairCommand(context.getClusterId(), context.getHostIp(), "java", command);
        
        log.info("开始从本地存储库复制JDK: {} -> {}/{}", downloadUrl, tempDir, jdkFileName);
        
        var result = sshService.executeCommand(pluginContext, command);
        logWriter.logRepairOutput(context.getClusterId(), context.getHostIp(), "java", result.output());
        
        if (!result.isSuccess()) {
            throw new Exception("从本地存储库复制JDK失败: " + result.error());
        }
        
        log.info("JDK复制成功: {}/{}", tempDir, jdkFileName);
        
        // 验证文件是否存在
        verifyFile(context, sshService, logWriter, pluginContext, tempDir + "/" + jdkFileName);
    }
    
    /**
     * 上传文件并显示进度
     */
    private void uploadWithProgress(HostCheckContext context, SshConnectionService sshService,
                                    CheckLogWriter logWriter, com.datasophon.plugins.api.model.HostCheckContext pluginContext,
                                    byte[] fileData, String remotePath) throws Exception {
        
        long totalBytes = fileData.length;
        int chunkSize = 10 * 1024 * 1024; // 10MB per chunk for progress logging
        
        // 如果文件小于10MB，直接上传
        if (totalBytes < chunkSize) {
            try (InputStream inputStream = new ByteArrayInputStream(fileData)) {
                boolean success = sshService.uploadFileFromStream(pluginContext, inputStream, remotePath);
                if (!success) {
                    throw new Exception("SSH上传失败");
                }
            }
            return;
        }
        
        // 大文件：分块记录进度（注意：实际还是整个上传，只是模拟进度日志）
        log.info("上传大文件，总大小: {}, 将记录上传进度", formatFileSize(totalBytes));
        
        try (InputStream inputStream = new ByteArrayInputStream(fileData)) {
            // 启动上传线程
            final boolean[] uploadSuccess = {false};
            final Exception[] uploadException = {null};
            
            Thread uploadThread = new Thread(() -> {
                try {
                    uploadSuccess[0] = sshService.uploadFileFromStream(pluginContext, inputStream, remotePath);
                } catch (Exception e) {
                    uploadException[0] = e;
                }
            });
            uploadThread.start();
            
            // 模拟进度记录（每1秒记录一次，像scp一样连续显示）
            long startTime = System.currentTimeMillis();
            int lastReportedProgress = 0;
            
            // 假设平均上传速度为 2MB/s（可根据实际调整）
            double estimatedSpeedMBPerSec = 2.0;
            double totalSizeMB = totalBytes / (1024.0 * 1024.0);
            
            while (uploadThread.isAlive()) {
                Thread.sleep(1000); // 每1秒更新一次
                long elapsedTime = System.currentTimeMillis() - startTime;
                double elapsedSeconds = elapsedTime / 1000.0;
                
                // 估算已传输大小（基于时间和速度）
                double uploadedMB = Math.min(totalSizeMB * 0.95, elapsedSeconds * estimatedSpeedMBPerSec);
                long uploadedBytes = (long) (uploadedMB * 1024 * 1024);
                
                // 计算进度百分比（最多95%，防止超过实际进度）
                int estimatedProgress = Math.min(95, (int) (uploadedMB / totalSizeMB * 100));
                
                // 只在进度变化时才推送日志（避免重复）
                if (estimatedProgress > lastReportedProgress) {
                    lastReportedProgress = estimatedProgress;
                    
                    Map<String, Object> progressInfo = new HashMap<>();
                    progressInfo.put("elapsedTime", formatDuration(elapsedTime));
                    progressInfo.put("totalSize", formatFileSize(totalBytes));
                    progressInfo.put("uploadedSize", formatFileSize(uploadedBytes)); // 新增：已传输大小
                    progressInfo.put("fileName", remotePath.substring(remotePath.lastIndexOf("/") + 1));
                    
                    // 使用专门的进度日志方法
                    logWriter.logRepairProgress(context.getClusterId(), context.getHostIp(), "java",
                            estimatedProgress,
                            String.format("上传中... %s / %s (%d%%)", 
                                formatFileSize(uploadedBytes), 
                                formatFileSize(totalBytes),
                                estimatedProgress),
                            progressInfo);
                    
                    log.info("上传进度: {}%, 已传输: {} / {}, 耗时: {}", 
                            estimatedProgress, 
                            formatFileSize(uploadedBytes),
                            formatFileSize(totalBytes),
                            formatDuration(elapsedTime));
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
            Map<String, Object> completeInfo = new HashMap<>();
            completeInfo.put("totalSize", formatFileSize(totalBytes));
            completeInfo.put("fileName", remotePath.substring(remotePath.lastIndexOf("/") + 1));
            logWriter.logRepairProgress(context.getClusterId(), context.getHostIp(), "java",
                    100, "文件上传完成", completeInfo);
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
     * 验证文件是否存在
     */
    private void verifyFile(HostCheckContext context, SshConnectionService sshService, 
                           CheckLogWriter logWriter, com.datasophon.plugins.api.model.HostCheckContext pluginContext,
                           String filePath) throws Exception {
        String verifyCommand = String.format("test -f '%s' && ls -lh '%s' || echo 'File not found'", filePath, filePath);
        logWriter.logRepairCommand(context.getClusterId(), context.getHostIp(), "java", verifyCommand);
        
        var verifyResult = sshService.executeCommand(pluginContext, verifyCommand);
        logWriter.logRepairOutput(context.getClusterId(), context.getHostIp(), "java", verifyResult.output());
        
        if (!verifyResult.isSuccess() || verifyResult.output().contains("File not found")) {
            throw new Exception("JDK包文件验证失败，文件不存在: " + filePath);
        }
        
        log.info("JDK包文件验证成功: {}", filePath);
    }
    
    /**
     * 格式化文件大小
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
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

