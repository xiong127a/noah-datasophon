package com.datasophon.api.repository;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 本地存储库下载器
 * 用于本地文件系统的文件复制
 * 
 * @author DataSophon Team
 * @date 2025-01-24
 */
@Slf4j
public class LocalRepositoryDownloader implements RepositoryDownloader {
    
    @Override
    public void download(String sourceUrl, String localPath, DownloadProgressCallback progressCallback) throws Exception {
        log.info("从本地存储库复制文件: {} -> {}", sourceUrl, localPath);
        
        Path source = Paths.get(sourceUrl);
        Path dest = Paths.get(localPath);
        
        if (!Files.exists(source)) {
            throw new IOException("源文件不存在: " + sourceUrl);
        }
        
        long fileSize = Files.size(source);
        
        // 本地复制很快，直接复制
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        
        // 回调进度（直接100%）
        if (progressCallback != null) {
            progressCallback.onProgress(fileSize, fileSize, 100);
        }
        
        log.info("本地文件复制完成: {}, 大小: {} bytes", localPath, fileSize);
    }
    
    @Override
    public String getType() {
        return "local";
    }
}

