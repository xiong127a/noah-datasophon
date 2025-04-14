/*
 *
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
 *
 */

package com.datasophon.api.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.datasophon.api.service.ClusterServiceInstanceService;
import com.datasophon.api.service.ServiceConfigFileService;
import com.datasophon.api.strategy.ServiceRoleStrategy;
import com.datasophon.api.strategy.ServiceRoleStrategyContext;
import com.datasophon.common.model.ConfigFile;
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * 服务配置文件服务实现
 */
@Service
@Slf4j
public class ServiceConfigFileServiceImpl implements ServiceConfigFileService {

    @Autowired
    private ClusterServiceInstanceService serviceInstanceService;

    /**
     * 获取服务配置文件列表
     *
     * @param serviceInstanceId 服务实例ID
     * @return 配置文件列表
     */
    @Override
    public List<ConfigFile> getServiceConfigFiles(Integer serviceInstanceId) {
        // 获取服务实例信息
        AbstractMap.SimpleEntry<ClusterServiceInstanceEntity, ServiceRoleStrategy> instanceId = getServiceRoleStrategyByInstanceId(
                serviceInstanceId);
        ServiceRoleStrategy strategy = instanceId.getValue();

        return strategy.getServiceConfigFiles(serviceInstanceId);
    }

    /**
     * 获取配置文件内容
     *
     * @param serviceInstanceId 服务实例ID
     * @param fileName          文件名
     * @return 文件内容
     */
    @Override
    public byte[] getServiceConfigFileContent(Integer serviceInstanceId, String fileName) {
        // TODO: 实现获取配置文件内容的逻辑
        AbstractMap.SimpleEntry<ClusterServiceInstanceEntity, ServiceRoleStrategy> instanceId = getServiceRoleStrategyByInstanceId(
                serviceInstanceId);
        ServiceRoleStrategy strategy = instanceId.getValue();

        return strategy.getServiceConfigFileContent(serviceInstanceId, fileName);
    }

    /**
     * 获取所有配置文件并根据指定格式打包
     *
     * @param serviceInstanceId 服务实例ID
     * @param format            压缩格式（zip, tar.gz, 7z）
     * @return 压缩文件内容
     */
    @Override
    public byte[] getAllServiceConfigFiles(Integer serviceInstanceId, String format) {
        // 获取服务实例信息和策略
        AbstractMap.SimpleEntry<ClusterServiceInstanceEntity, ServiceRoleStrategy> instanceId = getServiceRoleStrategyByInstanceId(
                serviceInstanceId);
        ServiceRoleStrategy strategy = instanceId.getValue();

        // 直接获取文件名和内容的映射
        Map<String, byte[]> configFilesWithContent = strategy.getServiceConfigFilesWithContent(serviceInstanceId);

        if (configFilesWithContent == null || configFilesWithContent.isEmpty()) {
            log.warn("服务实例{}没有配置文件", serviceInstanceId);
            return new byte[0];
        }

        try {
            // 创建临时目录存放配置文件
            Path tempDir = Files.createTempDirectory("service_configs_");

            // 将文件内容写入临时目录
            for (Map.Entry<String, byte[]> entry : configFilesWithContent.entrySet()) {
                try {
                    String fileName = entry.getKey();
                    byte[] content = entry.getValue();

                    if (content != null && content.length > 0) {
                        Path filePath = tempDir.resolve(fileName);
                        Files.write(filePath, content);
                    }
                } catch (Exception e) {
                    log.error("写入配置文件失败: {}", entry.getKey(), e);
                }
            }

            // 根据格式不同，使用不同的压缩方法
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if ("zip".equals(format)) {
                // 使用ZIP格式压缩
                try (java.util.zip.ZipOutputStream zipOut = new java.util.zip.ZipOutputStream(baos)) {
                    Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            // 计算文件相对于临时目录的路径
                            Path relativePath = tempDir.relativize(file);

                            // 创建zip条目
                            java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(relativePath.toString());
                            zipOut.putNextEntry(zipEntry);

                            // 写入文件内容
                            Files.copy(file, zipOut);
                            zipOut.closeEntry();

                            return FileVisitResult.CONTINUE;
                        }
                    });
                }
            } else if ("tar.gz".equals(format)) {
                // 使用tar.gz格式压缩
                try (TarArchiveOutputStream taos = new TarArchiveOutputStream(
                        new GZIPOutputStream(baos))) {
                    taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

                    Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            // 计算文件相对于临时目录的路径
                            Path relativePath = tempDir.relativize(file);

                            // 创建tar条目
                            TarArchiveEntry entry = new TarArchiveEntry(file.toFile(), relativePath.toString());
                            taos.putArchiveEntry(entry);

                            // 写入文件内容
                            Files.copy(file, taos);
                            taos.closeArchiveEntry();

                            return FileVisitResult.CONTINUE;
                        }
                    });
                }
            } else if ("7z".equals(format)) {
                // 使用7z格式压缩 (注意：这需要使用第三方库如sevenzipjbinding)
                // 此处返回zip格式作为备选，实际使用需要依赖额外的库
                log.warn("7z格式暂不支持，将返回zip格式");
                return getAllServiceConfigFiles(serviceInstanceId, "zip");
            } else {
                // 默认使用zip格式
                return getAllServiceConfigFiles(serviceInstanceId, "zip");
            }

            // 清理临时目录
            Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("打包配置文件失败", e);
            // 出错时返回空字节数组
            return new byte[0];
        }
    }

    /**
     * 获取服务名称
     *
     * @param serviceInstanceId 服务实例ID
     * @return 服务名称
     */
    @Override
    public String getServiceName(Integer serviceInstanceId) {
        ClusterServiceInstanceEntity serviceInstance = serviceInstanceService.getById(serviceInstanceId);
        return serviceInstance != null ? serviceInstance.getServiceName() : "unknown";
    }

    /**
     * 根据服务实例ID获取服务角色策略
     *
     * @param serviceInstanceId 服务实例ID
     * @return 服务角色策略
     */
    public AbstractMap.SimpleEntry<ClusterServiceInstanceEntity, ServiceRoleStrategy> getServiceRoleStrategyByInstanceId(
            Integer serviceInstanceId) {
        // 获取服务实例信息
        ClusterServiceInstanceEntity serviceInstance = serviceInstanceService.getById(serviceInstanceId);
        if (serviceInstance == null) {
            throw new RuntimeException("服务实例不存在，serviceInstanceId: " + serviceInstanceId);
        }
        // 获取服务名称
        String serviceName = serviceInstance.getServiceName();
        if (CharSequenceUtil.isBlank(serviceName)) {
            throw new RuntimeException("服务名称不能为空，serviceInstanceId: " + serviceInstanceId);
        }

        // 使用策略模式获取对应服务的连接信息
        ServiceRoleStrategy serviceRoleHandler = ServiceRoleStrategyContext.getServiceRoleHandler(serviceName);
        if (serviceRoleHandler == null) {
            throw new RuntimeException("未找到服务角色策略，serviceName: " + serviceName);
        }
        return new AbstractMap.SimpleEntry<>(serviceInstance, serviceRoleHandler);
    }




}