/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.datasophon.api.converter;

import com.datasophon.common.dto.ServiceDocDTO;
import com.datasophon.common.vo.ServiceDocVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 服务文档转换器
 * 负责ServiceDocDTO、ServiceDocVO之间的转换
 * 特别处理文档内容格式化和显示优化
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper(componentModel = "spring")
public interface ServiceDocConverter {

    /**
     * DTO转换为VO，添加前端展示优化字段
     */
    @Mapping(target = "formattedContent", source = "docContent", qualifiedByName = "formatContent")
    @Mapping(target = "contentLengthText", source = "contentLength", qualifiedByName = "formatSize")
    @Mapping(target = "docTypeDisplayName", source = "docType", qualifiedByName = "getDisplayName")
    @Mapping(target = "hasContent", source = ".", qualifiedByName = "checkHasContent")
    @Mapping(target = "lastModified", expression = "java(getCurrentTimeString())")
    ServiceDocVO dtoToVo(ServiceDocDTO dto);

    /**
     * 格式化文档内容（简单处理，可以扩展为Markdown转HTML）
     */
    @Named("formatContent")
    default String formatContent(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        
        // 简单的Markdown到HTML转换
        String formatted = content
                .replaceAll("\\n\\n", "</p><p>")  // 段落
                .replaceAll("\\n", "<br/>")       // 换行
                .replaceAll("^", "<p>")           // 开始段落
                .replaceAll("$", "</p>");        // 结束段落
                
        // 处理标题
        formatted = formatted.replaceAll("<p># (.*?)</p>", "<h1>$1</h1>");
        formatted = formatted.replaceAll("<p>## (.*?)</p>", "<h2>$1</h2>");
        formatted = formatted.replaceAll("<p>### (.*?)</p>", "<h3>$1</h3>");
        
        // 处理代码块
        formatted = formatted.replaceAll("`([^`]+)`", "<code>$1</code>");
        
        return formatted;
    }

    /**
     * 格式化文件大小
     */
    @Named("formatSize")
    default String formatSize(Long size) {
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

    /**
     * 获取文档类型显示名称
     */
    @Named("getDisplayName")
    default String getDisplayName(String docType) {
        if (docType == null) {
            return "未知类型";
        }
        
        String type = docType.toLowerCase();
        if ("component".equals(type)) {
            return "组件介绍";
        } else if ("guide".equals(type)) {
            return "用户指南";
        } else if ("help".equals(type)) {
            return "帮助文档";
        }
        return "未知类型";
    }

    /**
     * 检查是否有内容
     */
    @Named("checkHasContent")
    default Boolean checkHasContent(ServiceDocDTO dto) {
        return dto.hasContent();
    }

    /**
     * 获取当前时间字符串
     */
    default String getCurrentTimeString() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}