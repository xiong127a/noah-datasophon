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

package com.datasophon.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serial;
import java.io.Serializable;

/**
 * 服务文档数据传输对象
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServiceDocDTO(
        Long clusterId,
        Integer serviceId,
        String serviceName,
        String docType,
        String docContent,
        String docPath,
        Long contentLength,
        String encoding) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 创建基础文档DTO
     */
    public static ServiceDocDTO of(Long clusterId, Integer serviceId, String serviceName, String docType) {
        return new ServiceDocDTO(clusterId, serviceId, serviceName, docType, null, null, null, "UTF-8");
    }

    /**
     * 创建包含内容的文档DTO
     */
    public static ServiceDocDTO withContent(Long clusterId, Integer serviceId, String serviceName,
            String docType, String docContent, String docPath) {
        Long contentLength = docContent != null ? (long) docContent.length() : 0L;
        return new ServiceDocDTO(clusterId, serviceId, serviceName, docType, docContent, docPath, contentLength,
                "UTF-8");
    }

    /**
     * 获取文档大小（字节）
     */
    public long getContentSize() {
        return contentLength != null ? contentLength : 0L;
    }

    /**
     * 检查是否有文档内容
     */
    public boolean hasContent() {
        return docContent != null && !docContent.isEmpty();
    }

    /**
     * 获取文档类型显示名称
     */
    public String getDocTypeDisplayName() {
        return switch (docType != null ? docType.toLowerCase() : "") {
            case "component" -> "组件介绍";
            case "guide" -> "用户指南";
            case "help" -> "帮助文档";
            default -> "未知类型";
        };
    }
}