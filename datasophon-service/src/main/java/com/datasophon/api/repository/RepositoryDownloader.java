package com.datasophon.api.repository;

/**
 * 存储库下载器接口
 * 定义统一的下载接口，支持不同类型的存储库（本地、HTTP、HDFS等）
 * 
 * @author DataSophon Team
 * @date 2025-01-24
 */
public interface RepositoryDownloader {
    
    /**
     * 下载文件到本地
     * 
     * @param sourceUrl 源文件URL或路径
     * @param localPath 本地目标路径
     * @param progressCallback 进度回调（可选）
     * @throws Exception 下载失败时抛出异常
     */
    void download(String sourceUrl, String localPath, DownloadProgressCallback progressCallback) throws Exception;
    
    /**
     * 获取存储库类型
     * 
     * @return 存储库类型（local, http, hdfs等）
     */
    String getType();
    
    /**
     * 下载进度回调接口
     */
    @FunctionalInterface
    interface DownloadProgressCallback {
        /**
         * 进度回调
         * 
         * @param downloadedBytes 已下载字节数
         * @param totalBytes 总字节数
         * @param progress 进度百分比（0-100）
         */
        void onProgress(long downloadedBytes, long totalBytes, int progress);
    }
}

