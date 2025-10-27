package com.datasophon.api.repository;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * HTTP存储库下载器
 * 用于从HTTP/HTTPS服务器下载文件
 * 
 * @author DataSophon Team
 * @date 2025-01-24
 */
@Slf4j
public class HttpRepositoryDownloader implements RepositoryDownloader {
    
    @Override
    public void download(String sourceUrl, String localPath, DownloadProgressCallback progressCallback) throws Exception {
        log.info("从HTTP存储库下载文件: {} -> {}", sourceUrl, localPath);
        
        // 创建临时文件
        File tempFile = new File(localPath + ".tmp");
        
        try {
            // 使用流式下载，支持大文件
            HttpResponse response = HttpRequest.get(sourceUrl)
                    .timeout(600000) // 10分钟超时
                    .executeAsync();
            
            if (!response.isOk()) {
                throw new Exception("HTTP下载失败，状态码: " + response.getStatus());
            }
            
            // 获取文件大小
            long totalSize = response.contentLength();
            long downloadedSize = 0;
            int lastReportedProgress = 0;
            long lastReportTime = System.currentTimeMillis();
            
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
                    
                    if (progress > lastReportedProgress && (currentTime - lastReportTime) >= 1000) {
                        lastReportedProgress = progress;
                        lastReportTime = currentTime;
                        
                        if (progressCallback != null) {
                            progressCallback.onProgress(downloadedSize, totalSize, progress);
                        }
                        
                        log.debug("下载进度: {}%, {} / {} bytes", progress, downloadedSize, totalSize);
                    }
                }
            }
            
            // 重命名临时文件为目标文件
            Files.move(tempFile.toPath(), Paths.get(localPath), StandardCopyOption.REPLACE_EXISTING);
            
            // 最终进度回调
            if (progressCallback != null) {
                progressCallback.onProgress(downloadedSize, downloadedSize, 100);
            }
            
            log.info("HTTP文件下载完成: {}, 大小: {} bytes", localPath, downloadedSize);
            
        } catch (Exception e) {
            // 清理临时文件
            if (tempFile.exists()) {
                tempFile.delete();
            }
            throw e;
        }
    }
    
    @Override
    public String getType() {
        return "http";
    }
}

