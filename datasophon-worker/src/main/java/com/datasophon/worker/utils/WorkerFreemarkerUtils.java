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

import org.apache.pekko.actor.ActorSystem;
import com.datasophon.common.Constants;
import com.datasophon.common.model.AlertItem;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.FreemarkerUtils;
import com.datasophon.common.utils.PropertyUtils;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import static com.datasophon.common.utils.HostUtils.GetMasterHost;

/**
 * Worker扩展版Freemarker工具类
 * 作为桥接器，负责从Akka获取模板内容，然后传递给common中的FreemarkerUtils处理
 */
@UtilityClass
public class WorkerFreemarkerUtils {

    private static final Logger logger = LoggerFactory.getLogger(WorkerFreemarkerUtils.class);

    // Master主机地址
    private static final String MASTER_HOST = PropertyUtils.getString(GetMasterHost().get(0), "localhost");


    /**
     * 设置ActorSystem实例，在Worker启动时调用
     *
     */
    @Setter
    private static ActorSystem actorSystem;

    /**
     * 生成配置文件（从Akka获取模板，支持扩展路径）
     *
     * @param generators            配置文件生成器
     * @param configs               配置项列表
     * @param decompressPackageName 解压后的包名
     * @throws IOException IO异常
     */
    public static void generateConfigFile(Generators generators,
            List<ServiceConfig> configs,
            String decompressPackageName) throws IOException {

        // 获取模板名称
        String templateName = FreemarkerUtils.determineTemplateName(generators);

        if (templateName != null && actorSystem != null) {
            try {
                // 从Akka获取模板内容
                String templateContent = AkkaUtils.getTemplateContent(actorSystem, MASTER_HOST, templateName);
                if (templateContent != null) {
                    // 使用字符串模板生成配置，使用直接模式，避免prepareTemplateData处理
                    FreemarkerUtils.generateConfigFileFromString(generators, configs, templateContent, templateName,
                            decompressPackageName);
                    return;
                } else {
                    // 获取失败时直接抛出异常，不再回退到本地模板
                    String errorMsg = "从Akka获取模板失败: " + templateName;
                    logger.error(errorMsg);
                    throw new IOException(errorMsg);
                }
            } catch (Exception e) {
                logger.error("通过Akka获取模板时发生异常: {}", templateName, e);
                throw new IOException("通过Akka获取模板时发生异常: " + templateName, e);
            }
        }

        // 模板名称未确定时抛出异常
        String errorMsg = "模板名称未确定";
        logger.error(errorMsg);
        throw new IOException(errorMsg);
    }

    /**
     * 生成Prometheus告警规则文件（从Akka获取模板）
     *
     * @param generators  配置文件生成器
     * @param configs     告警项列表
     * @param serviceName 服务名称
     * @throws IOException IO异常
     */
    public static void generatePromAlertFile(Generators generators, List<AlertItem> configs,
            String serviceName) throws IOException {

        if (actorSystem != null && Constants.PROMETHEUS.equals(generators.getConfigFormat())) {
            try {
                // 从Akka获取模板内容
                String templateContent = AkkaUtils.getTemplateContent(actorSystem, MASTER_HOST, "alert.yml");
                if (templateContent != null) {
                    // 使用字符串模板处理告警项
                    FreemarkerUtils.generatePromAlertFileFromString(generators, configs, serviceName, templateContent);
                    return;
                } else {
                    // 获取失败时直接抛出异常，不再回退到本地模板
                    String errorMsg = "从Akka获取告警模板失败: alert.yml";
                    logger.error(errorMsg);
                    throw new IOException(errorMsg);
                }
            } catch (Exception e) {
                logger.error("通过Akka获取告警模板时发生异常", e);
                throw new IOException("通过Akka获取告警模板时发生异常", e);
            }
        }

        // 不符合条件时抛出异常
        String errorMsg = "ActorSystem未初始化或非Prometheus配置格式";
        logger.error(errorMsg);
        throw new IOException(errorMsg);
    }
}