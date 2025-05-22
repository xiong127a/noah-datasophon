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
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceInstanceService;
import com.datasophon.api.service.DocService;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 文档服务实现
 */
@Service
@Slf4j
public class DocServiceImpl implements DocService {

    @Autowired
    private ClusterInfoService clusterInfoService;

    @Autowired
    private ClusterServiceInstanceService serviceInstanceService;

    @Autowired
    private ResourceLoader resourceLoader;

    /**
     * 文档根目录
     */
    private static final String DOC_ROOT_DIR = "docs";

    /**
     * 组件介绍文档目录
     */
    private static final String COMPONENT_DOC_DIR = "components";

    /**
     * 用户指南文档目录
     */
    private static final String GUIDE_DOC_DIR = "guides";

    /**
     * 图片目录
     */
    private static final String IMAGES_DIR = "images";

    @Override
    public Result getServiceDoc(Integer clusterId, Integer serviceId, String type) {
        try {
            // 检查参数
            if (clusterId == null || serviceId == null || StrUtil.isBlank(type)) {
                return Result.error("参数错误，请检查参数");
            }

            // 特殊处理：获取告警管理帮助文档
            if (serviceId.equals(-991) && "guide".equals(type)) {
                log.info("获取告警管理帮助文档");
                return getAlarmManagementHelp();
            }

            // 获取集群信息
            ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
            if (clusterInfo == null) {
                return Result.error("集群不存在");
            }

            // 获取服务实例信息
            ClusterServiceInstanceEntity serviceInstance = serviceInstanceService.getById(serviceId);
            if (serviceInstance == null) {
                return Result.error("服务实例不存在");
            }

            // 获取服务名称
            String serviceName = serviceInstance.getServiceName();

            if (StrUtil.isBlank(serviceName)) {
                return Result.error("服务名称不能为空");
            }

            if (StrUtil.equals("DS", serviceName)) {
                serviceName = "DolphinScheduler";
            }

            // 确定文档类型目录
            String docTypeDir;
            String suffix;
            if ("component".equals(type)) {
                docTypeDir = COMPONENT_DOC_DIR;
                suffix = "-introduce";
            } else if ("guide".equals(type)) {
                docTypeDir = GUIDE_DOC_DIR;
                suffix = "-user-guide";
            } else {
                return Result.error("文档类型错误");
            }

            String docContent = readDocContent(serviceName.toLowerCase(), docTypeDir, suffix);

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
     * 获取告警管理帮助文档
     * 
     * @return 告警管理帮助文档内容
     */
    private Result getAlarmManagementHelp() {
        try {
            // 尝试从多个位置读取告警管理帮助文档
            String[] possiblePaths = {
                    "docs/alarm-management-help.md", // 相对路径
                    "datasophon-api/src/main/resources/docs/alarm-management-help.md", // 项目结构下的路径
                    System.getProperty("user.dir") + "/docs/alarm-management-help.md", // 当前用户目录下
                    System.getProperty("user.dir") + "/datasophon-api/src/main/resources/docs/alarm-management-help.md" // 完整路径
            };

            for (String path : possiblePaths) {
                File file = new File(path);
                if (file.exists()) {
                    log.info("找到告警管理帮助文档：{}", path);
                    String content = FileUtil.readString(file, StandardCharsets.UTF_8);
                    return Result.success(content);
                }
            }

            // 作为最后的尝试，直接从类路径资源中加载
            try (InputStream is = DocServiceImpl.class.getClassLoader()
                    .getResourceAsStream("docs/alarm-management-help.md")) {
                if (is != null) {
                    log.info("从类路径资源加载告警管理帮助文档");
                    String content = IoUtil.read(is, StandardCharsets.UTF_8);
                    return Result.success(content);
                }
            } catch (Exception e) {
                log.warn("从类路径资源加载告警管理帮助文档失败：{}", e.getMessage());
            }

            log.warn("所有路径均未找到告警管理帮助文档");
            return Result.error("告警管理帮助文档不存在");
        } catch (Exception e) {
            log.error("获取告警管理帮助文档出错", e);
            return Result.error("获取告警管理帮助文档出错: " + e.getMessage());
        }
    }

    /**
     * 读取文档内容
     *
     * @param serviceName 服务名称
     * @param docType     文档类型目录
     * @return 文档内容
     */
    private String readDocContent(String serviceName, String docType, String suffix) {
        try {
            // 构建文档路径，如：/docs/components/hdfs-introduce.md
            String docName = String.format("%s.md", serviceName + suffix);

            File[] ls = FileUtil.ls(DOC_ROOT_DIR + "/" + docType);

            for (File file : ls) {
                if (StrUtil.equals(docName, file.getName())) {
                    return FileUtil.readString(file, StandardCharsets.UTF_8);
                }
            }
            log.warn("文档不存在: {}", docName);
            return null;
        } catch (Exception e) {
            log.error("读取文档出错: {}", e.getMessage(), e);
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

            // 去除可能的../前缀
            String normalizedPath = StrUtil.removePrefix(imagePath, "../");

            // 分离路径和文件名
            String fileName = FileUtil.getName(normalizedPath);
            String dirPath = StrUtil.removeSuffix(normalizedPath, fileName);
            dirPath = StrUtil.removeSuffix(dirPath, "/"); // 去除可能的尾部斜杠

            // 构建目录路径
            String fullDirPath = DOC_ROOT_DIR + "/" + dirPath;
            log.debug("查找目录: {}, 文件名: {}", fullDirPath, fileName);

            // 列出目录下所有文件
            File[] files = FileUtil.ls(fullDirPath);

            // 查找匹配的文件
            for (File file : files) {
                if (StrUtil.equals(fileName, file.getName())) {
                    log.info("找到匹配的图片: {}", file.getAbsolutePath());
                    return resourceLoader.getResource("file:" + file.getAbsolutePath());
                }
            }

            log.warn("未找到匹配的图片: {}", fileName);
            return null;
        } catch (Exception e) {
            log.error("获取图片资源出错: {}", e.getMessage(), e);
            return null;
        }
    }
}