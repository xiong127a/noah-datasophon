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
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceInstanceService;
import com.datasophon.api.service.DocService;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
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

    @Override
    public Result getServiceDoc(Integer clusterId, Integer serviceId, String type) {
        try {
            // 检查参数
            if (clusterId == null || serviceId == null || StrUtil.isBlank(type)) {
                return Result.error("参数错误，请检查参数");
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

            // 确定文档类型目录
            String docTypeDir;
            if ("component".equals(type)) {
                docTypeDir = COMPONENT_DOC_DIR;
            } else if ("guide".equals(type)) {
                docTypeDir = GUIDE_DOC_DIR;
            } else {
                return Result.error("文档类型错误");
            }

            String docContent = readDocContent(serviceName.toLowerCase(), docTypeDir);

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
     * 读取文档内容
     *
     * @param serviceName 服务名称
     * @param docType     文档类型目录
     * @return 文档内容
     */
    private String readDocContent(String serviceName, String docType) {
        try {
            // 构建文档路径，如：/docs/components/hdfs.md
            String docName = String.format("%s.md", serviceName);

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
}