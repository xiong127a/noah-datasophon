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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ZipUtil;
import com.datasophon.api.service.ClusterServiceInstanceService;
import com.datasophon.api.service.ServiceConfigFileService;
import com.datasophon.common.model.ConfigFile;
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
        ClusterServiceInstanceEntity serviceInstance = serviceInstanceService.getById(serviceInstanceId);
        if (serviceInstance == null) {
            log.error("服务实例不存在：{}", serviceInstanceId);
            return new ArrayList<>();
        }

        // TODO: 根据服务类型和实例ID获取配置文件列表
        // 这里只是实现骨架，具体实现逻辑由您完成
        List<ConfigFile> configFiles = new ArrayList<>();

        // 模拟数据，实际实现中请替换为真实逻辑
        configFiles.add(ConfigFile.builder()
                .fileName("core-site.xml")
                .description("Hadoop核心配置文件")
                .fileSize("5.2 KB")
                .filePath("/etc/hadoop/conf/core-site.xml")
                .lastModified("2023-05-20 10:30:00")
                .build());

        if ("HDFS".equals(serviceInstance.getServiceName())) {
            configFiles.add(ConfigFile.builder()
                    .fileName("hdfs-site.xml")
                    .description("HDFS配置文件")
                    .fileSize("8.7 KB")
                    .filePath("/etc/hadoop/conf/hdfs-site.xml")
                    .lastModified("2023-05-20 10:30:00")
                    .build());
        }

        return configFiles;
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
        // 这里只是返回模拟数据，实际实现中请替换为真实逻辑


        return new byte[0];
    }

    /**
     * 获取所有配置文件并打包成zip
     *
     * @param serviceInstanceId 服务实例ID
     * @return zip文件内容
     */
    @Override
    public byte[] getAllServiceConfigFilesAsZip(Integer serviceInstanceId) {
        // TODO: 实现获取所有配置文件并打包成zip的逻辑
        // 这里只是骨架代码，具体实现由您完成

        try {
            // 创建临时目录
            Path tempDir = Files.createTempDirectory("service_configs");

            // 获取所有配置文件
            List<ConfigFile> configFiles = getServiceConfigFiles(serviceInstanceId);

            // 写入文件到临时目录
            for (ConfigFile configFile : configFiles) {
                byte[] content = getServiceConfigFileContent(serviceInstanceId, configFile.getFileName());
                if (content != null && content.length > 0) {
                    Path filePath = tempDir.resolve(configFile.getFileName());
                    Files.write(filePath, content);
                }
            }

            // 打包成zip
            Path zipFile = Paths.get(tempDir.toString() + ".zip");
            // 使用hutool的zip方法
            File zip = ZipUtil.zip(tempDir.toFile());

            // 读取zip文件内容
            byte[] zipContent = Files.readAllBytes(zip.toPath());

            // 清理临时文件
            FileUtil.del(tempDir);
            FileUtil.del(zipFile);

            return zipContent;
        } catch (IOException e) {
            log.error("打包配置文件失败", e);
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
}