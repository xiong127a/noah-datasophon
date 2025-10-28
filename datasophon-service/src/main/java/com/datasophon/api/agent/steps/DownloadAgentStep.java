package com.datasophon.api.agent.steps;

import com.datasophon.api.agent.AgentDistributionContext;
import com.datasophon.api.agent.AgentDistributionStep;
import com.datasophon.api.agent.util.AgentLogWriter;
import com.datasophon.api.repository.RepositoryDownloader;
import com.datasophon.api.repository.RepositoryDownloaderFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 下载Agent包到Master本地步骤
 * 使用工厂模式根据存储库类型获取对应的下载器
 * 
 * @author DataSophon Team
 * @date 2025-01-24
 */
@Slf4j
public class DownloadAgentStep implements AgentDistributionStep {
    
    private final RepositoryDownloaderFactory downloaderFactory;
    
    public DownloadAgentStep(RepositoryDownloaderFactory downloaderFactory) {
        this.downloaderFactory = downloaderFactory;
    }
    
    @Override
    public String getStepName() {
        return "准备Agent包";
    }
    
    @Override
    public void execute(AgentDistributionContext context) throws Exception {
        AgentLogWriter logWriter = context.getLogWriter();
        Long clusterId = context.getClusterId();
        String hostIp = context.getHostIp();
        String agentPackageUrl = context.getAgentPackageUrl();
        String localPackagePath = context.getLocalPackagePath();
        
        // 记录开始日志
        Map<String, Object> startInfo = new HashMap<>();
        startInfo.put("sourceUrl", agentPackageUrl);
        startInfo.put("localPath", localPackagePath);
        startInfo.put("isLocal", context.isLocalRepository());
        logWriter.logInfo(clusterId, hostIp, "download", 
                "开始准备Agent包", startInfo);
        
        log.info("开始准备Agent包: {} -> {}", agentPackageUrl, localPackagePath);
        
        // 确保目标目录存在（从localPackagePath中提取目录路径）
        Path packagePath = Paths.get(localPackagePath);
        File packageDir = packagePath.getParent().toFile();
        if (!packageDir.exists()) {
            packageDir.mkdirs();
            log.info("创建Agent包目录: {}", packageDir.getAbsolutePath());
        }
        
        // 使用工厂模式获取对应的下载器
        String repoType = context.isLocalRepository() ? "local" : "http";
        RepositoryDownloader downloader = downloaderFactory.getDownloader(repoType);
        
        log.info("使用{}存储库下载器", repoType);
        
        // ====== 1. 下载Agent包 ======
        log.info("下载Agent包: {}", agentPackageUrl);
        
        // 用于计算速率和剩余时间的变量
        final long[] lastDownloadedBytes = {0};
        final long[] lastUpdateTime = {System.currentTimeMillis()};
        
        downloader.download(
                agentPackageUrl,
                localPackagePath,
                (downloadedBytes, totalBytes, progress) -> {
                    long currentTime = System.currentTimeMillis();
                    long timeDelta = currentTime - lastUpdateTime[0];
                    
                    // 至少间隔500ms才计算一次速率（避免频繁计算）
                    if (timeDelta >= 500) {
                        long bytesDelta = downloadedBytes - lastDownloadedBytes[0];
                        
                        // 计算实时速率 (bytes/s)
                        double currentSpeed = timeDelta > 0 ? (bytesDelta * 1000.0 / timeDelta) : 0;
                        
                        // 计算预计剩余时间
                        long remainingBytes = totalBytes - downloadedBytes;
                        long estimatedRemainingSeconds = currentSpeed > 0 ? (long) (remainingBytes / currentSpeed) : 0;
                        
                        // 格式化速率和剩余时间
                        String speedStr = formatSpeed(currentSpeed);
                        String remainingTimeStr = formatDuration(estimatedRemainingSeconds * 1000);
                        
                        // 进度回调
                        logWriter.logProgress(clusterId, hostIp, "download",
                                progress, downloadedBytes, totalBytes,
                                String.format("下载Agent包... %s / %s (%d%%) | 速率: %s | 剩余: %s",
                                        formatFileSize(downloadedBytes),
                                        formatFileSize(totalBytes),
                                        progress,
                                        speedStr,
                                        remainingTimeStr));
                        
                        // 更新上次记录的值
                        lastDownloadedBytes[0] = downloadedBytes;
                        lastUpdateTime[0] = currentTime;
                    }
                }
        );
        
        File downloadedFile = new File(localPackagePath);
        log.info("Agent包下载完成: {}, 大小: {}", localPackagePath, formatFileSize(downloadedFile.length()));
        
        // ====== 2. 下载MD5文件 ======
        String md5Url = agentPackageUrl + ".md5";
        String md5Path = localPackagePath + ".md5";
        
        log.info("下载MD5文件: {}", md5Url);
        try {
            downloader.download(md5Url, md5Path, null); // MD5文件很小，不需要进度回调
            log.info("MD5文件下载完成: {}", md5Path);
        } catch (Exception e) {
            log.warn("MD5文件下载失败: {}, 将跳过MD5校验", e.getMessage());
            // MD5文件不存在不影响安装，只是跳过校验
        }
        
        // 下载完成日志
        Map<String, Object> completeInfo = new HashMap<>();
        completeInfo.put("packagePath", localPackagePath);
        completeInfo.put("packageSize", formatFileSize(downloadedFile.length()));
        completeInfo.put("md5Path", md5Path);
        logWriter.logSuccess(clusterId, hostIp, "download",
                "Agent包和MD5文件准备完成", completeInfo);
        log.info("Agent包准备完成: {}, 大小: {}", localPackagePath, formatFileSize(downloadedFile.length()));
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
}
