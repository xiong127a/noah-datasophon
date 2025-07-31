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
import com.datasophon.api.vo.Result;
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 文档服务实现类
 */
@Service
@Slf4j
public class DocServiceImpl implements DocService {


    /**
     * 文档类型枚举
     */
    @Getter
    private enum DocType {
        COMPONENT("components", "-introduce"),
        GUIDE("guides", "-user-guide"),
        HELP("help", "-help");


    private String dirName;

    private String suffix;

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
    private static final int ALARM_MANAGEMENT_SERVICE_ID = -991;

    // 文档目录常量
    private static final String DOC_ROOT_DIR = "docs";

    // 依赖注入

    @Autowired
    private ClusterServiceInstanceService serviceInstanceService;

    @Autowired
    private ResourceLoader resourceLoader;

    @Override
    public Result getServiceDoc(Integer clusterId, Integer serviceId, String typeStr) {
        try {
            // 检查基本参数
            if (clusterId == null || serviceId == null || StrUtil.isBlank(typeStr)) {
                return Result.error("参数错误，请检查参数");
            }

            // 获取文档类型
            DocType docType = DocType.fromString(typeStr);
            if (docType == null) {
                return Result.error("文档类型错误");
            }

            // 获取服务名称
            String serviceName = getServiceName(serviceId);
            if (StrUtil.isBlank(serviceName)) {
                return Result.error("服务名称不能为空");
            }

            // 读取文档内容
            String docContent = readDocContent(serviceName.toLowerCase(), docType.getDirName(), docType.getSuffix());

            if (docContent != null) {
                return Result.success(docContent);
            } else {
                return Result.error("文档不存在");
            }
        } catch (Exception e) {
            log.error("获取服务文档出错", e);
            return Result.error("获取服务文档出错: " + e.getMessage());
        }
    }

    /**
     * 获取服务名称
     * 
     * @param serviceId 服务ID
     * @return 服务名称
     */
    private String getServiceName(Integer serviceId) {
        // 特殊处理：告警管理
        if (serviceId.equals(ALARM_MANAGEMENT_SERVICE_ID)) {
            log.info("获取告警管理帮助文档");
            return "alarm-management";
        }

        // 获取服务实例信息
        ClusterServiceInstanceEntity serviceInstance = getServiceInstance(serviceId);
        if (serviceInstance == null) {
            return null;
        }

        // 获取服务名称
        String serviceName = serviceInstance.getServiceName();

        // 处理特殊服务名称
        if (StrUtil.equals("DS", serviceName)) {
            serviceName = "DolphinScheduler";
        }

        return serviceName;
    }

    /**
     * 获取服务实例
     * 
     * @param serviceId 服务ID
     * @return 服务实例，不存在则返回null
     */
    private ClusterServiceInstanceEntity getServiceInstance(Integer serviceId) {
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
        log.info("获取图片资源: {}", imagePath);

        try {
            // 参数检查
            if (StrUtil.isBlank(imagePath)) {
                log.warn("图片路径为空");
                return null;
            }

            // 处理HTTP/HTTPS链接
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                log.info("处理远程图片URL: {}", imagePath);
                return resourceLoader.getResource(imagePath);
            }

            // 解码URL，处理多重编码情况
            // 循环解码直到路径不再变化
            String decodedPath = imagePath;
            String previousPath = "";
            while (!decodedPath.equals(previousPath)) {
                previousPath = decodedPath;
                try {
                    decodedPath = java.net.URLDecoder.decode(previousPath, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    // 如果解码出错，说明已经不需要再解码了或格式不正确
                    log.debug("URL解码结束或出错: {}", e.getMessage());
                    break;
                }
            }
            log.info("解码后的图片路径: {}", decodedPath);

            // 去除可能的../前缀
            String normalizedPath = StrUtil.removePrefix(decodedPath, "../");

            // 分离路径和文件名
            String fileName = FileUtil.getName(normalizedPath);
            String dirPath = StrUtil.removeSuffix(normalizedPath, fileName);
            dirPath = StrUtil.removeSuffix(dirPath, "/"); // 去除可能的尾部斜杠

            // 构建目录路径
            String fullDirPath = DOC_ROOT_DIR + "/" + dirPath;
            log.debug("查找目录: {}, 文件名: {}", fullDirPath, fileName);

            // 列出目录下所有文件
            File file = FileUtil.file(fullDirPath+"/"+fileName);

            log.info("找到匹配的图片: {}", file.getAbsolutePath());
            return resourceLoader.getResource("file:" + file.getAbsolutePath());
        } catch (Exception e) {
            log.error("获取图片资源出错: {}", e.getMessage(), e);
            return null;
        }
    }

}