/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.datasophon.api.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 存储库URL构建工具类
 * 统一管理所有包文件的URL/路径拼接逻辑，使用Hutool成熟库
 * 
 * @author DataSophon Team
 */
public class RepositoryUrlBuilder {
    
    private static final Logger logger = LoggerFactory.getLogger(RepositoryUrlBuilder.class);
    
    /**
     * 构建包文件的完整路径
     * 格式: baseUrl/frameVersion/packageName
     * 
     * @param baseUrl 存储库基础URL
     * @param frameVersion 框架版本号
     * @param packageName 包名
     * @return 完整的包路径
     */
    public static String buildPackageUrl(String baseUrl, String frameVersion, String packageName) {
        if (StrUtil.isBlank(baseUrl)) {
            throw new IllegalArgumentException("存储库URL不能为空");
        }
        if (StrUtil.isBlank(frameVersion)) {
            throw new IllegalArgumentException("框架版本号不能为空");
        }
        if (StrUtil.isBlank(packageName)) {
            throw new IllegalArgumentException("包名不能为空");
        }
        
        // 判断是本地路径还是HTTP URL
        if (isHttpUrl(baseUrl)) {
            return buildHttpUrl(baseUrl, frameVersion, packageName);
        } else {
            return buildLocalPath(baseUrl, frameVersion, packageName);
        }
    }
    
    /**
     * 构建HTTP URL
     * 使用Hutool的URL工具类确保正确拼接
     */
    private static String buildHttpUrl(String baseUrl, String frameVersion, String packageName) {
        try {
            // 标准化baseUrl（去掉末尾斜杠）
            String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            
            // 使用Hutool的URL拼接（自动处理斜杠）
            String fullUrl = URLUtil.normalize(normalizedBase + "/" + frameVersion + "/" + packageName);
            
            logger.debug("构建HTTP URL: base={}, version={}, package={}, result={}", 
                        baseUrl, frameVersion, packageName, fullUrl);
            
            return fullUrl;
        } catch (Exception e) {
            logger.error("构建HTTP URL失败: base={}, version={}, package={}", 
                        baseUrl, frameVersion, packageName, e);
            throw new RuntimeException("构建HTTP URL失败", e);
        }
    }
    
    /**
     * 构建本地文件路径
     * 使用Java NIO Path确保跨平台兼容性
     */
    private static String buildLocalPath(String baseDir, String frameVersion, String packageName) {
        try {
            Path path = Paths.get(baseDir, frameVersion, packageName);
            String fullPath = path.toString();
            
            logger.debug("构建本地路径: base={}, version={}, package={}, result={}", 
                        baseDir, frameVersion, packageName, fullPath);
            
            return fullPath;
        } catch (Exception e) {
            logger.error("构建本地路径失败: base={}, version={}, package={}", 
                        baseDir, frameVersion, packageName, e);
            throw new RuntimeException("构建本地路径失败", e);
        }
    }
    
    /**
     * 构建MD5文件的完整路径
     * 
     * @param packageUrl 包文件的完整URL
     * @return MD5文件的完整URL
     */
    public static String buildMd5Url(String packageUrl) {
        if (StrUtil.isBlank(packageUrl)) {
            throw new IllegalArgumentException("包URL不能为空");
        }
        return packageUrl + ".md5";
    }
    
    /**
     * 判断是否为HTTP URL
     */
    private static boolean isHttpUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }
    
    /**
     * 从完整URL中提取存储库基础URL
     * 例如: http://192.168.1.30/BDP/packages/DDP-1.2.1/grafana.tar.gz
     *      -> http://192.168.1.30/BDP/packages
     */
    public static String extractBaseUrl(String fullUrl, String frameVersion, String packageName) {
        if (StrUtil.isBlank(fullUrl)) {
            return null;
        }
        
        // 移除末尾的 /frameVersion/packageName
        String suffix = "/" + frameVersion + "/" + packageName;
        if (fullUrl.endsWith(suffix)) {
            return fullUrl.substring(0, fullUrl.length() - suffix.length());
        }
        
        return null;
    }
}

