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

package com.datasophon.common.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serial;
import java.io.Serializable;

/**
 * 服务文档视图对象
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServiceDocVO(
        Long clusterId,
        Long serviceId,
        String serviceName,
        String docType,
        String docTypeDisplayName,
        String docContent,
        String formattedContent,  // 格式化后的HTML内容
        String docPath,
        Long contentLength,
        String contentLengthText, // 格式化的文件大小文本
        String encoding,
        Boolean hasContent,
        String lastModified      // 最后修改时间
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 获取格式化的文件大小
     */
    public String getContentLengthText() {
        if (contentLengthText != null) {
            return contentLengthText;
        }
        if (contentLength == null || contentLength == 0) {
            return "0 字节";
        }
        
        double size = contentLength.doubleValue();
        if (size < 1024) {
            return String.format("%.0f 字节", size);
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024);
        } else {
            return String.format("%.1f MB", size / (1024 * 1024));
        }
    }

    /**
     * 获取文档类型显示名称
     */
    public String getDocTypeDisplayName() {
        if (docTypeDisplayName != null) {
            return docTypeDisplayName;
        }
        return switch (docType != null ? docType.toLowerCase() : "") {
            case "component" -> "组件介绍";
            case "guide" -> "用户指南";
            case "help" -> "帮助文档";
            default -> "未知类型";
        };
    }

    /**
     * 检查是否有内容
     */
    public Boolean hasContent() {
        if (hasContent != null) {
            return hasContent;
        }
        return docContent != null && !docContent.isEmpty();
    }

    /**
     * 创建基础文档VO
     */
    public static ServiceDocVO of(Long clusterId, Long serviceId, String serviceName, String docType) {
        String docTypeDisplayName = getDisplayNameByType(docType);
        return new ServiceDocVO(clusterId, serviceId, serviceName, docType, docTypeDisplayName,
                null, null, null, 0L, "0 字节", "UTF-8", false, null);
    }

    /**
     * 创建包含内容的文档VO
     */
    public static ServiceDocVO withContent(Long clusterId, Long serviceId, String serviceName,
            String docType, String docContent, String formattedContent, String docPath, String lastModified) {
        String docTypeDisplayName = getDisplayNameByType(docType);
        Long contentLength = docContent != null ? (long) docContent.length() : 0L;
        String contentLengthText = formatSize(contentLength);
        Boolean hasContent = docContent != null && !docContent.isEmpty();
        
        return new ServiceDocVO(clusterId, serviceId, serviceName, docType, docTypeDisplayName,
                docContent, formattedContent, docPath, contentLength, contentLengthText,
                "UTF-8", hasContent, lastModified);
    }

    /**
     * 根据类型获取显示名称
     */
    private static String getDisplayNameByType(String docType) {
        return switch (docType != null ? docType.toLowerCase() : "") {
            case "component" -> "组件介绍";
            case "guide" -> "用户指南";
            case "help" -> "帮助文档";
            default -> "未知类型";
        };
    }

    /**
     * 格式化文件大小
     */
    private static String formatSize(Long size) {
        if (size == null || size == 0) {
            return "0 字节";
        }
        
        double sizeDouble = size.doubleValue();
        if (sizeDouble < 1024) {
            return String.format("%.0f 字节", sizeDouble);
        } else if (sizeDouble < 1024 * 1024) {
            return String.format("%.1f KB", sizeDouble / 1024);
        } else {
            return String.format("%.1f MB", sizeDouble / (1024 * 1024));
        }
    }
}