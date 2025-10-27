package com.datasophon.api.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 存储库下载器工厂
 * 根据存储库类型创建对应的下载器实例
 * 
 * @author DataSophon Team
 * @date 2025-01-24
 */
@Slf4j
@Component
public class RepositoryDownloaderFactory {
    
    /**
     * 根据存储库类型获取下载器
     * 
     * @param repoType 存储库类型（local, http, hdfs等）
     * @return 对应的下载器实例
     * @throws IllegalArgumentException 如果存储库类型不支持
     */
    public RepositoryDownloader getDownloader(String repoType) {
        if (repoType == null || repoType.isEmpty()) {
            throw new IllegalArgumentException("存储库类型不能为空");
        }
        
        String type = repoType.toLowerCase();
        log.debug("创建存储库下载器: type={}", type);
        
        switch (type) {
            case "local":
                return new LocalRepositoryDownloader();
                
            case "http":
            case "https":
                return new HttpRepositoryDownloader();
                
            case "hdfs":
                return new HdfsRepositoryDownloader();
                
            default:
                throw new IllegalArgumentException("不支持的存储库类型: " + repoType + 
                        "。支持的类型: local, http, hdfs");
        }
    }
    
    /**
     * 判断存储库类型是否支持
     * 
     * @param repoType 存储库类型
     * @return 是否支持
     */
    public boolean isSupported(String repoType) {
        if (repoType == null || repoType.isEmpty()) {
            return false;
        }
        
        String type = repoType.toLowerCase();
        return "local".equals(type) || "http".equals(type) || "https".equals(type) || "hdfs".equals(type);
    }
}

