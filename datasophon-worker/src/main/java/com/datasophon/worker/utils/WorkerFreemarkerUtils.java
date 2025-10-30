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

package com.datasophon.worker.utils;

import com.datasophon.common.Constants;
import com.datasophon.common.model.AlertItem;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.FreemarkerUtils;
import com.datasophon.common.utils.PropertyUtils;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import static com.datasophon.common.utils.HostUtils.GetMasterHost;

/**
 * Worker扩展版Freemarker工具类
 * 负责从命令对象获取模板内容，然后传递给common中的FreemarkerUtils处理
 */
@UtilityClass
public class WorkerFreemarkerUtils {

    private static final Logger logger = LoggerFactory.getLogger(WorkerFreemarkerUtils.class);

    // Master主机地址
    private static final String MASTER_HOST = PropertyUtils.getString(GetMasterHost().getFirst(), "localhost");

    /**
     * 生成配置文件（从命令对象获取模板）
     *
     * @param generators            配置文件生成器
     * @param configs               配置项列表
     * @param decompressPackageName 解压后的包名
     * @param templateContents      模板内容映射 (templateName -> templateContent)
     * @throws IOException IO异常
     * @throws freemarker.template.TemplateException 模板处理异常
     */
    public static void generateConfigFile(Generators generators,
            List<ServiceConfig> configs,
            String decompressPackageName,
            Map<String, String> templateContents) throws IOException, freemarker.template.TemplateException {

        // 获取模板名称
        var templateName = FreemarkerUtils.determineTemplateName(generators);

        if (templateName != null) {
            // 从命令对象中获取模板内容
            var templateContent = templateContents != null ? templateContents.get(templateName) : null;
            
            if (templateContent != null) {
                // 使用字符串模板生成配置，使用直接模式，避免prepareTemplateData处理
                FreemarkerUtils.generateConfigFileFromString(generators, configs, templateContent, templateName,
                        decompressPackageName);
                logger.info("成功从命令对象获取模板: {}, 内容长度: {}", templateName, templateContent.length());
                return;
            } else {
                // 获取失败时抛出异常
                var errorMsg = "从命令对象获取模板失败: " + templateName;
                logger.error(errorMsg);
                throw new IOException(errorMsg);
            }
        }

        // 模板名称未确定时抛出异常
        var errorMsg = "模板名称未确定";
        logger.error(errorMsg);
        throw new IOException(errorMsg);
    }

    /**
     * 生成Prometheus告警规则文件（从命令对象获取模板）
     *
     * @param generators       配置文件生成器
     * @param configs          告警项列表
     * @param serviceName      服务名称
     * @param templateContents 模板内容映射
     * @throws IOException IO异常
     * @throws freemarker.template.TemplateException 模板处理异常
     */
    public static void generatePromAlertFile(Generators generators, List<AlertItem> configs,
            String serviceName, Map<String, String> templateContents) throws IOException, freemarker.template.TemplateException {

        if (Constants.PROMETHEUS.equals(generators.getConfigFormat())) {
            // 从命令对象中获取模板内容
            var templateContent = templateContents != null ? templateContents.get("alert.yml") : null;
            
            if (templateContent != null) {
                // 使用字符串模板处理告警项
                FreemarkerUtils.generatePromAlertFileFromString(generators, configs, serviceName, templateContent);
                logger.info("成功从命令对象获取alert.yml模板");
                return;
            } else {
                var errorMsg = "从命令对象获取alert.yml模板失败";
                logger.error(errorMsg);
                throw new IOException(errorMsg);
            }
        }

        // 不是Prometheus格式时抛出异常
        throw new IOException("生成告警规则文件失败：配置格式不正确");
    }
}