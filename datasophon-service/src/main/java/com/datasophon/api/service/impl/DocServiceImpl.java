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

package com.datasophon.api.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.datasophon.api.service.ClusterServiceInstanceService;
import com.datasophon.api.service.DocService;
import com.datasophon.common.dto.ServiceDocDTO;
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 文档服务实现类
 * 按照架构重构规范，返回DTO对象，抛出业务异常
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocServiceImpl implements DocService {

    /**
     * 文档类型枚举 - 使用JDK21现代特性
     */
    @Getter
    public enum DocType {
        COMPONENT("components", "-introduce"),
        GUIDE("guides", "-user-guide"),
        HELP("help", "-help");

        private final String dirName;
        private final String suffix;

        DocType(String dirName, String suffix) {
            this.dirName = dirName;
            this.suffix = suffix;
        }

        public static DocType fromString(String typeStr) {
            if (typeStr == null) {
                return null;
            }

            String type = typeStr.toLowerCase();
            return switch (type) {
                case "component" -> COMPONENT;
                case "guide" -> GUIDE;
                case "help" -> HELP;
                default -> null;
            };
        }
    }

    // 特殊服务ID常量
    private static final Long ALARM_MANAGEMENT_SERVICE_ID = -991L;

    // 文档目录常量
    private static final String DOC_ROOT_DIR = "docs";

    // 依赖注入 - 使用构造器注入
    private final ClusterServiceInstanceService serviceInstanceService;
    private final ResourceLoader resourceLoader;

    @Override
    public ServiceDocDTO getServiceDoc(Long clusterId, Long serviceId, String typeStr) {
        // 检查基本参数
        if (clusterId == null) {
            throw new RuntimeException("集群ID不能为空");
        }
        if (serviceId == null) {
            throw new RuntimeException("服务ID不能为空");
        }
        if (StrUtil.isBlank(typeStr)) {
            throw new RuntimeException("文档类型不能为空");
        }

        // 获取文档类型
        DocType docType = DocType.fromString(typeStr);
        if (docType == null) {
            throw new RuntimeException("不支持的文档类型: " + typeStr);
        }

        // 获取服务名称
        String serviceName = getServiceName(serviceId);
        if (StrUtil.isBlank(serviceName)) {
            throw new RuntimeException("未找到服务ID为 " + serviceId + " 的服务信息");
        }

        // 读取文档内容
        String docContent = readDocContent(serviceName.toLowerCase(), docType.getDirName(), docType.getSuffix());
        if (docContent == null) {
            throw new RuntimeException("服务 " + serviceName + " 的 " + docType.getDirName() + " 文档不存在");
        }

        // 构建文档路径
        String docFileName = serviceName.toLowerCase() + docType.getSuffix() + ".md";
        String docPath = DOC_ROOT_DIR + "/" + docType.getDirName() + "/" + docFileName;

        return ServiceDocDTO.withContent(clusterId, serviceId, serviceName, typeStr, docContent, docPath);
    }

    @Override
    public String getServiceName(Long serviceId) {
        if (serviceId == null) {
            throw new RuntimeException("服务ID不能为空");
        }
        
        // 特殊处理：告警管理
        if (serviceId.equals(ALARM_MANAGEMENT_SERVICE_ID)) {
            log.debug("获取告警管理帮助文档服务名称");
            return "alarm-management";
        }

        // 获取服务实例信息
        ClusterServiceInstanceEntity serviceInstance = getServiceInstance(serviceId);
        if (serviceInstance == null) {
            throw new RuntimeException("未找到ID为 " + serviceId + " 的服务实例");
        }

        // 获取服务名称
        String serviceName = serviceInstance.getServiceName();
        if (StrUtil.isBlank(serviceName)) {
            throw new RuntimeException("服务实例的服务名称为空");
        }

        // 处理特殊服务名称映射
        if ("DS".equals(serviceName)) {
            return "DolphinScheduler";
        }
        return serviceName;
    }

    @Override
    public boolean hasServiceDoc(Long clusterId, Long serviceId, String type) {
        try {
            // 基本参数检查
            if (clusterId == null || serviceId == null || StrUtil.isBlank(type)) {
                return false;
            }

            // 获取文档类型
            DocType docType = DocType.fromString(type);
            if (docType == null) {
                return false;
            }

            // 获取服务名称
            String serviceName = getServiceName(serviceId);
            if (StrUtil.isBlank(serviceName)) {
                return false;
            }

            // 检查文档是否存在
            String docContent = readDocContent(serviceName.toLowerCase(), docType.getDirName(), docType.getSuffix());
            return docContent != null;
        } catch (Exception e) {
            log.debug("检查服务文档存在性时出错: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取服务实例
     * 
     * @param serviceId 服务ID
     * @return 服务实例，不存在则返回null
     */
    private ClusterServiceInstanceEntity getServiceInstance(Long serviceId) {
        ClusterServiceInstanceEntity serviceInstance = serviceInstanceService.getById(serviceId);
        if (serviceInstance == null) {
            log.warn("服务实例不存在: serviceId={}", serviceId);
            return null;
        }
        return serviceInstance;
    }

    /**
     * 读取文档内容
     *
     * @param serviceName 服务名称
     * @param docDir      文档目录
     * @param suffix      文件名后缀
     * @return 文档内容
     */
    private String readDocContent(String serviceName, String docDir, String suffix) {
        try {
            // 构建文档文件路径
            String docFileName = serviceName + suffix + ".md";
            String docPath = DOC_ROOT_DIR + "/" + docDir + "/" + docFileName;
            log.info("查找文档路径: {}", docPath);

            // 尝试从文件系统读取
            File file = FileUtil.file(docPath);
            return FileUtil.readUtf8String(file);
        } catch (Exception e) {
            log.error("读取文档内容出错", e);
            return null;
        }
    }

    @Override
    public Resource getImageResource(String imagePath) {
        log.debug("获取图片资源: {}", imagePath);

        // 参数检查
        if (StrUtil.isBlank(imagePath)) {
            throw new RuntimeException("图片路径不能为空");
        }

        try {
            // 处理HTTP/HTTPS链接
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                log.debug("处理远程图片URL: {}", imagePath);
                Resource resource = resourceLoader.getResource(imagePath);
                if (!resource.exists()) {
                    throw new RuntimeException("远程图片资源不存在: " + imagePath);
                }
                return resource;
            }

            // 解码URL，处理多重编码情况
            String decodedPath = decodeUrlPath(imagePath);
            log.debug("解码后的图片路径: {}", decodedPath);

            // 安全性检查：去除可能的../前缀，防止路径遍历攻击
            String normalizedPath = StrUtil.removePrefix(decodedPath, "../");
            if (normalizedPath.contains("..")) {
                throw new RuntimeException("图片路径包含非法字符");
            }

            // 分离路径和文件名
            String fileName = FileUtil.getName(normalizedPath);
            String dirPath = StrUtil.removeSuffix(normalizedPath, fileName);
            dirPath = StrUtil.removeSuffix(dirPath, "/"); // 去除可能的尾部斜杠

            // 构建完整文件路径
            String fullFilePath = DOC_ROOT_DIR + "/" + dirPath + "/" + fileName;
            File file = FileUtil.file(fullFilePath);

            if (!file.exists()) {
                throw new RuntimeException("图片文件不存在: " + normalizedPath);
            }
            if (!file.canRead()) {
                throw new RuntimeException("图片文件无法读取: " + normalizedPath);
            }

            log.debug("找到图片文件: {}", file.getAbsolutePath());
            return resourceLoader.getResource("file:" + file.getAbsolutePath());
            
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取图片资源出错: {}", e.getMessage(), e);
            throw new RuntimeException("获取图片资源失败: " + e.getMessage());
        }
    }

    /**
     * 解码URL路径，处理多重编码
     */
    private String decodeUrlPath(String path) {
        String decodedPath = path;
        String previousPath = "";
        
        // 循环解码直到路径不再变化
        while (!decodedPath.equals(previousPath)) {
            previousPath = decodedPath;
            try {
                decodedPath = java.net.URLDecoder.decode(previousPath, StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.debug("URL解码结束或出错: {}", e.getMessage());
                break;
            }
        }
        
        return decodedPath;
    }

}