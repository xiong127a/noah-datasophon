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

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceInstanceRoleGroupService;
import com.datasophon.api.service.ClusterServiceInstanceService;
import com.datasophon.api.service.ServiceConfigFileService;
import com.datasophon.api.utils.CompressUtils;
import com.datasophon.api.utils.MetaPathUtils;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.common.Constants;
import com.datasophon.common.model.ConfigFile;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.FreemarkerUtils;
import com.datasophon.common.utils.TemplatePathUtils;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import com.datasophon.dao.entity.ClusterServiceInstanceRoleGroup;
import freemarker.template.Template;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务配置文件服务实现
 */
@Service
@Slf4j
public class ServiceConfigFileServiceImpl implements ServiceConfigFileService {

    @Autowired
    private ClusterServiceInstanceService serviceInstanceService;

    /**
     * 配置文件内容缓存，5分钟过期
     */
    Cache<String, byte[]> CONFIG_FILE_CACHE = CacheUtil.newTimedCache(5 * 60 * 1000);

    /**
     * 获取服务配置文件列表
     *
     * @param serviceInstanceId 服务实例ID
     * @return 配置文件列表
     */
    @Override
    public List<ConfigFile> getServiceConfigFiles(Integer serviceInstanceId) {
        // 获取服务实例信息
        try {
            // 获取服务元数据信息
            ServiceMetaInfo metaInfo = getServiceMetaInfo(serviceInstanceId);
            if (metaInfo == null) {
                return new ArrayList<>();
            }

            // 从元数据获取配置文件生成器信息
            JSONArray generators = metaInfo.getGenerators();
            if (generators.isEmpty()) {
                log.warn("未找到服务{}的配置文件生成器定义", metaInfo.getServiceName());
                return new ArrayList<>();
            }

            // 构建配置文件列表
            List<ConfigFile> configFiles = new ArrayList<>();
            for (int i = 0; i < generators.size(); i++) {
                JSONObject generator = generators.getJSONObject(i);

                String fileName = generator.getString("filename");
                // 这里是建议的新字段，用于描述配置文件
                String fileDescription = generator.getString("fileDescription");

                if (StrUtil.startWith(fileName, Constants.KUBERNETES_CONFIG_PREFIX)) {
                    continue;
                }

                // 如果没有描述信息，提供一个默认描述
                if (CharSequenceUtil.isBlank(fileDescription)) {
                    fileDescription = fileName + " 配置文件";
                }

                // 计算文件大小
                String fileSize = "0 KB";
                try {
                    // 获取文件内容并计算大小
                    byte[] content = getServiceConfigFileContent(serviceInstanceId, fileName);
                    if (ArrayUtil.isNotEmpty(content)) {
                        long sizeInKB = content.length / 1024;
                        if (sizeInKB == 0 && content.length > 0) {
                            sizeInKB = 1; // 最小显示1KB
                        }
                        fileSize = sizeInKB + " KB";
                    }
                } catch (Exception e) {
                    log.warn("计算文件大小失败: {}", fileName, e);
                }

                ConfigFile configFile = ConfigFile.builder()
                        .fileName(fileName)
                        .description(fileDescription)
                        .fileSize(fileSize)
                        .build();

                configFiles.add(configFile);
            }

            return configFiles;
        } catch (Exception e) {
            log.error("获取服务配置文件列表失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取服务元数据信息
     * 
     * @param serviceInstanceId 服务实例ID
     * @return 服务元数据信息
     */
    private ServiceMetaInfo getServiceMetaInfo(Integer serviceInstanceId) {
        try {
            // 获取服务相关信息
            ClusterServiceInstanceRoleGroupService roleGroupService = SpringUtil
                    .getBean(ClusterServiceInstanceRoleGroupService.class);
            ClusterServiceInstanceRoleGroup roleGroup = roleGroupService
                    .getRoleGroupByServiceInstanceId(serviceInstanceId);
            if (roleGroup == null) {
                log.warn("未找到服务实例ID{}对应的角色组", serviceInstanceId);
                return null;
            }

            // 获取集群信息
            ClusterInfoService clusterInfoService = SpringUtil
                    .getBean(ClusterInfoService.class);
            ClusterInfoEntity clusterInfo = clusterInfoService.getById(roleGroup.getClusterId());
            if (clusterInfo == null) {
                log.warn("未找到集群信息: {}", roleGroup.getClusterId());
                return null;
            }

            // 获取服务名称和框架代码
            String serviceName = roleGroup.getServiceName();
            String frameCode = clusterInfo.getClusterFrame();

            // 从元数据获取配置文件生成器信息
            JSONArray generators = MetaPathUtils.getServiceConfigGenerators(frameCode, serviceName);

            // 创建并返回元数据信息
            ServiceMetaInfo metaInfo = new ServiceMetaInfo();
            metaInfo.setServiceName(serviceName);
            metaInfo.setFrameCode(frameCode);
            metaInfo.setClusterId(clusterInfo.getId());
            metaInfo.setGenerators(generators);
            metaInfo.setRoleGroup(roleGroup);
            return metaInfo;
        } catch (Exception e) {
            log.error("获取服务元数据信息失败: {}", serviceInstanceId, e);
            return null;
        }
    }

    /**
     * 查找指定文件名对应的生成器配置
     * 
     * @param generators 生成器配置列表
     * @param fileName   文件名
     * @return 生成器配置，未找到则返回null
     */
    private JSONObject findGeneratorByFileName(JSONArray generators, String fileName) {
        if (generators == null || generators.isEmpty()) {
            return null;
        }

        for (int i = 0; i < generators.size(); i++) {
            JSONObject generator = generators.getJSONObject(i);
            if (fileName.equals(generator.getString("filename"))) {
                return generator;
            }
        }
        return null;
    }

    /**
     * 获取所有配置文件并根据指定格式打包，支持密码保护
     *
     * @param serviceInstanceId 服务实例ID
     * @param format            压缩格式（zip, tar.gz, 7z）
     * @param password          密码（可为空）
     * @return 压缩文件内容
     */
    @Override
    public byte[] getAllServiceConfigFiles(Integer serviceInstanceId, String format, String password) {
        // 直接获取文件名和内容的映射
        Map<String, byte[]> configFilesWithContent = getServiceConfigFilesWithContent(serviceInstanceId);

        if (configFilesWithContent == null || configFilesWithContent.isEmpty()) {
            log.warn("服务实例{}没有配置文件", serviceInstanceId);
            return new byte[0];
        }

        // 使用新的压缩工具类，支持密码保护和进度跟踪
        return CompressUtils.getCompressedFiles(configFilesWithContent, format, password, serviceInstanceId);
    }

    /**
     * 获取打包进度
     *
     * @param serviceInstanceId 服务实例ID
     * @return 打包进度（0-100）
     */
    @Override
    public Integer getCompressProgress(Integer serviceInstanceId) {
        // 从CompressUtils获取进度
        return CompressUtils.getCompressProgress(serviceInstanceId);
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
     * 获取所有配置文件名称和内容的映射关系
     *
     * @param serviceInstanceId 服务实例ID
     * @return 配置文件名和内容的映射
     */
    public Map<String, byte[]> getServiceConfigFilesWithContent(Integer serviceInstanceId) {
        // 默认实现：基于已有方法获取配置文件列表，然后逐个获取内容
        Map<String, byte[]> result = new HashMap<>();
        List<ConfigFile> configFiles = getServiceConfigFiles(serviceInstanceId);

        if (configFiles != null && !configFiles.isEmpty()) {
            for (ConfigFile configFile : configFiles) {
                byte[] content = getServiceConfigFileContent(serviceInstanceId, configFile.getFileName());
                if (ArrayUtil.isNotEmpty(content)) {
                    result.put(configFile.getFileName(), content);
                }
            }
        }
        return result;
    }

    public byte[] getServiceConfigFileContent(Integer serviceInstanceId, String fileName) {
        // 生成缓存键
        String cacheKey = serviceInstanceId + "_" + fileName;

        // 从缓存获取文件内容
        byte[] content = CONFIG_FILE_CACHE.get(cacheKey);
        if (ArrayUtil.isNotEmpty(content)) {
            log.debug("从缓存获取配置文件内容: {}", fileName);
            return content;
        }

        // 缓存未命中，生成文件内容
        log.debug("缓存未命中，生成配置文件内容: {}", fileName);

        // 这里应该是各服务实现的获取文件内容的具体逻辑
        // 由于是默认实现，这里返回null，实际应由各服务重写
        content = generateConfigFileContent(serviceInstanceId, fileName);

        // 如果成功生成内容，则放入缓存
        if (ArrayUtil.isNotEmpty(content)) {
            CONFIG_FILE_CACHE.put(cacheKey, content);
        }

        return content;
    }

    /**
     * 生成配置文件内容的方法，需要子类实现
     *
     * @param serviceInstanceId 服务实例ID
     * @param fileName          文件名
     * @return 文件内容
     */
    public byte[] generateConfigFileContent(Integer serviceInstanceId, String fileName) {
        try {
            // 获取服务元数据信息
            ServiceMetaInfo metaInfo = getServiceMetaInfo(serviceInstanceId);
            if (metaInfo == null) {
                log.warn("未找到服务实例{}的元数据信息", serviceInstanceId);
                return new byte[0];
            }

            // 查找匹配当前文件名的生成器
            JSONObject targetGenerator = findGeneratorByFileName(metaInfo.getGenerators(), fileName);
            if (targetGenerator == null) {
                log.warn("未找到文件{}的生成器定义", fileName);
                return new byte[0];
            }

            // 获取服务配置列表
            Map.Entry<String, List<ServiceConfig>> pair = SpringTool
                    .listServiceConfigByServiceInstance(serviceInstanceId);
            List<ServiceConfig> configList = pair.getValue();

            // 使用FreemarkerUtils确定模板名称
            Generators generators = JSONObject.toJavaObject(targetGenerator, Generators.class);
            String templateName = FreemarkerUtils.determineTemplateName(generators);

            if (templateName == null) {
                log.warn("无法确定配置文件格式对应的模板: {}", generators.getConfigFormat());
                return new byte[0];
            }

            // 获取模板内容
            String templateContent = TemplatePathUtils.getTemplateContent(templateName);
            if (templateContent == null) {
                log.warn("未能获取到模板内容: {}", templateName);
                return new byte[0];
            }

            // 获取主机名和IP用于变量替换
            Map<String, String> paramMap = new HashMap<>();
            try {
                String hostName = InetAddress.getLocalHost().getHostName();
                String ip = NetUtil.getIpByHost(hostName);
                paramMap.put("${host}", hostName);
                paramMap.put("${ip}", ip);
                paramMap.put("${user}", "root");
            } catch (Exception e) {
                log.error("获取主机信息失败: {}", e.getMessage());
            }

            // 使用FreemarkerUtils.prepareRenderData处理配置数据，确保与ConfigureServiceHandler逻辑一致
            Map<String, Object> data = FreemarkerUtils.prepareRenderData(generators, configList, paramMap, log);
            try {
                // 从模板内容创建Template对象
                Template template = FreemarkerUtils.createTemplateFromContent(templateContent, templateName);
                byte[] content = FreemarkerUtils.renderTemplateToBytes(template, data);
                return ArrayUtil.isNotEmpty(content) ? content : new byte[0];
            } catch (Exception e) {
                log.error("处理模板失败: {}", templateName, e);
                return new byte[0];
            }
        } catch (Exception e) {
            log.error("生成配置文件内容失败: {}", fileName, e);
            return new byte[0];
        }
    }

    /**
     * 服务元数据信息内部类，用于封装服务相关的元数据
     */
    @Data
    private static class ServiceMetaInfo {
        private String serviceName;
        private String frameCode;
        private Integer clusterId;
        private JSONArray generators;
        private ClusterServiceInstanceRoleGroup roleGroup;
    }

}