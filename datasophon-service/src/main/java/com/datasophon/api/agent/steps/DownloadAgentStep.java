package com.datasophon.api.agent.steps;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.datasophon.api.agent.AgentDistributionContext;
import com.datasophon.api.agent.AgentDistributionStep;
import com.datasophon.api.agent.util.AgentLogWriter;
import com.datasophon.common.Constants;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * 下载Agent包到Master本地步骤
 * - 如果是HTTP存储库：下载Agent包到Master本地
 * - 如果是本地存储库：复制Agent包
 * 
 * @author DataSophon Team
 * @date 2025-01-24
 */
@Slf4j
public class DownloadAgentStep implements AgentDistributionStep {
    
    @Override
    public String getStepName() {
        return "准备Agent包";
    }
    
    @Override
    public void execute(AgentDistributionContext context) throws Exception {
        AgentLogWriter logWriter = context.getLogWriter();
        Long clusterId = context.getClusterId();
        String hostIp = context.getHostIp();
        
        // Master本地Agent包路径
        String localPackagePath = Constants.MASTER_MANAGE_PACKAGE_PATH + Constants.SLASH + Constants.WORKER_PACKAGE_NAME;
        context.setLocalPackagePath(localPackagePath);
        
        File localPackageFile = new File(localPackagePath);
        
        // 检查Master本地是否已有Agent包
        if (localPackageFile.exists()) {
            Map<String, Object> existsInfo = new HashMap<>();
            existsInfo.put("path", localPackagePath);
            existsInfo.put("size", formatFileSize(localPackageFile.length()));
            logWriter.logInfo(clusterId, hostIp, "download", 
                    "Master本地已存在Agent包，跳过下载", existsInfo);
            log.info("Master本地已存在Agent包: {}, 大小: {}", localPackagePath, formatFileSize(localPackageFile.length()));
            return;
        }
        
        // 确保目录存在
        File packageDir = localPackageFile.getParentFile();
        if (!packageDir.exists()) {
            packageDir.mkdirs();
            log.info("创建Agent包目录: {}", packageDir.getAbsolutePath());
        }
        
        // 根据存储库类型下载或复制
        if (context.isLocalRepository()) {
            downloadFromLocal(context);
        } else {
            downloadFromHttp(context);
        }
    }
    
    /**
     * 从HTTP存储库下载Agent包
     */
    private void downloadFromHttp(AgentDistributionContext context) throws Exception {
        AgentLogWriter logWriter = context.getLogWriter();
        Long clusterId = context.getClusterId();
        String hostIp = context.getHostIp();
        String downloadUrl = context.getAgentPackageUrl();
        String localPackagePath = context.getLocalPackagePath();
        
        Map<String, Object> downloadInfo = new HashMap<>();
        downloadInfo.put("downloadUrl", downloadUrl);
        downloadInfo.put("localPath", localPackagePath);
        logWriter.logInfo(clusterId, hostIp, "download", 
                "开始从HTTP存储库下载Agent包到Master", downloadInfo);
        
        log.info("开始从HTTP存储库下载Agent包: {} -> {}", downloadUrl, localPackagePath);
        
        try {
            // 使用流式下载，支持大文件
            HttpResponse response = HttpRequest.get(downloadUrl)
                    .timeout(600000) // 10分钟超时
                    .executeAsync();
            
            if (!response.isOk()) {
                throw new Exception("HTTP下载失败，状态码: " + response.getStatus());
            }
            
            // 获取文件大小
            long totalSize = response.contentLength();
            
            // 创建临时文件
            File tempFile = new File(localPackagePath + ".tmp");
            long downloadedSize = 0;
            int lastReportedProgress = 0;
            long startTime = System.currentTimeMillis();
            
            try (InputStream inputStream = response.bodyStream();
                 FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    downloadedSize += bytesRead;
                    
                    // 每秒或每10%推送一次进度
                    int progress = totalSize > 0 ? (int) ((downloadedSize * 100) / totalSize) : 0;
                    long currentTime = System.currentTimeMillis();
                    
                    if (progress > lastReportedProgress && (currentTime - startTime) >= 1000) {
                        lastReportedProgress = progress;
                        startTime = currentTime;
                        
                        logWriter.logProgress(clusterId, hostIp, "download",
                                progress, downloadedSize, totalSize,
                                String.format("下载中... %s / %s (%d%%)",
                                        formatFileSize(downloadedSize),
                                        formatFileSize(totalSize),
                                        progress));
                        
                        log.info("下载进度: {}%, {} / {}", progress, 
                                formatFileSize(downloadedSize), formatFileSize(totalSize));
                    }
                }
            }
            
            // 重命名临时文件
            Files.move(tempFile.toPath(), Paths.get(localPackagePath), StandardCopyOption.REPLACE_EXISTING);
            
            Map<String, Object> completeInfo = new HashMap<>();
            completeInfo.put("localPath", localPackagePath);
            completeInfo.put("size", formatFileSize(downloadedSize));
            logWriter.logSuccess(clusterId, hostIp, "download", 
                    "Agent包下载完成", completeInfo);
            log.info("Agent包下载完成: {}, 大小: {}", localPackagePath, formatFileSize(downloadedSize));
            
        } catch (Exception e) {
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", e.getMessage());
            errorInfo.put("downloadUrl", downloadUrl);
            logWriter.logError(clusterId, hostIp, "download", 
                    "下载Agent包失败: " + e.getMessage(), errorInfo);
            throw new Exception("从HTTP存储库下载Agent包失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从本地存储库复制Agent包
     */
    private void downloadFromLocal(AgentDistributionContext context) throws Exception {
        AgentLogWriter logWriter = context.getLogWriter();
        Long clusterId = context.getClusterId();
        String hostIp = context.getHostIp();
        String sourcePackagePath = context.getAgentPackageUrl();
        String localPackagePath = context.getLocalPackagePath();
        
        Map<String, Object> copyInfo = new HashMap<>();
        copyInfo.put("sourcePath", sourcePackagePath);
        copyInfo.put("destPath", localPackagePath);
        logWriter.logInfo(clusterId, hostIp, "download", 
                "开始从本地存储库复制Agent包", copyInfo);
        
        log.info("开始从本地存储库复制Agent包: {} -> {}", sourcePackagePath, localPackagePath);
        
        try {
            Path source = Paths.get(sourcePackagePath);
            Path dest = Paths.get(localPackagePath);
            
            if (!Files.exists(source)) {
                throw new IOException("源文件不存在: " + sourcePackagePath);
            }
            
            long fileSize = Files.size(source);
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
            
            Map<String, Object> completeInfo = new HashMap<>();
            completeInfo.put("destPath", localPackagePath);
            completeInfo.put("size", formatFileSize(fileSize));
            logWriter.logSuccess(clusterId, hostIp, "download", 
                    "Agent包复制完成", completeInfo);
            log.info("Agent包复制完成: {}, 大小: {}", localPackagePath, formatFileSize(fileSize));
            
        } catch (Exception e) {
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", e.getMessage());
            errorInfo.put("sourcePath", sourcePackagePath);
            logWriter.logError(clusterId, hostIp, "download", 
                    "复制Agent包失败: " + e.getMessage(), errorInfo);
            throw new Exception("从本地存储库复制Agent包失败: " + e.getMessage(), e);
        }
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

