package com.datasophon.api.repository;

import lombok.extern.slf4j.Slf4j;

/**
 * HDFS存储库下载器
 * 用于从HDFS文件系统下载文件（暂未实现）
 * 
 * @author DataSophon Team
 * @date 2025-01-24
 */
@Slf4j
public class HdfsRepositoryDownloader implements RepositoryDownloader {
    
    @Override
    public void download(String sourceUrl, String localPath, DownloadProgressCallback progressCallback) throws Exception {
        log.warn("HDFS存储库下载功能暂未实现: {}", sourceUrl);
        throw new UnsupportedOperationException("HDFS存储库下载功能暂未实现，敬请期待");
    }
    
    @Override
    public String getType() {
        return "hdfs";
    }
}

