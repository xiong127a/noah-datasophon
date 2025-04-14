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

import akka.actor.ActorSystem;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.model.AlertItem;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.PropertyUtils;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class FreemakerUtils {

    private static final Logger logger = LoggerFactory.getLogger(FreemakerUtils.class);

    // Master主机地址
    private static final String MASTER_HOST = PropertyUtils.getString("masterHost", "localhost");

    /**
     * -- SETTER --
     * 设置ActorSystem实例，在Worker启动时调用
     *
     * @param system ActorSystem实例
     */
    // 当前ActorSystem，由Worker初始化时设置
    @Setter
    private static ActorSystem actorSystem;

    public static void generateConfigFile(Generators generators,
                                          List<ServiceConfig> configs,
                                          String decompressPackageName) throws IOException, TemplateException {
        generateConfigFile(generators, configs, decompressPackageName, null);
    }

    /**
     * 支持 从附加的目录加载 模版
     */
    public static void generateConfigFile(Generators generators,
                                          List<ServiceConfig> configs,
                                          String decompressPackageName,
                                          String extPath) throws IOException, TemplateException {
        // 1.加载模板
        // 创建核心配置对象
        Configuration config = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);

        // 设置加载的目录
        List<TemplateLoader> loaderList = new ArrayList<>();
        loaderList.add(new ClassTemplateLoader(FreemakerUtils.class, "/templates"));
        if (StringUtils.isNotBlank(extPath) && new File(extPath).exists()) {
            // 如果 三方的 package 中存在 templates 模版，则直接加载
            loaderList.add(new FileTemplateLoader(new File(extPath)));
        }
        config.setTemplateLoader(new MultiTemplateLoader(loaderList.toArray(new TemplateLoader[0])));

        Map<String, Object> data = new HashMap<>();
        // 得到模板对象
        String configFormat = generators.getConfigFormat();
        Template template = null;
        if (Constants.XML.equals(configFormat)) {
            template = config.getTemplate("xml.ftl");
        }
        if (Constants.PROPERTIES.equals(configFormat)) {
            template = config.getTemplate("properties.ftl");
        }
        if (Constants.PROPERTIES2.equals(configFormat)) {
            template = config.getTemplate("properties2.ftl");
        }
        if (Constants.PROPERTIES3.equals(configFormat)) {
            template = config.getTemplate("properties3.ftl");
        }
        if (Constants.PROMETHEUS.equals(configFormat)) {
            template = config.getTemplate("alert.yml");
        }
        if (Constants.CUSTOM.equals(configFormat)) {
            template = config.getTemplate(generators.getTemplateName());
            Map<Boolean, List<ServiceConfig>> partitionedConfigs = configs.stream()
                    .collect(Collectors.partitioningBy(e -> "map".equals(e.getConfigType())));
            data = partitionedConfigs.get(true).stream()
                    .collect(Collectors.toMap(ServiceConfig::getName, ServiceConfig::getValue));
            configs = partitionedConfigs.get(false);
        }
        logger.info("load template: {} success.", Objects.requireNonNull(template).getSourceName());
        data.put("itemList", configs);
        // 3.产生输出
        processOut(generators, template, data, decompressPackageName);
    }

    /**
     * 从Akka加载模板到Freemarker配置
     *
     * @param config     Freemarker配置
     * @param generators 生成器配置
     */
    private static void loadTemplateFromAkka(Configuration config, Generators generators) {
        String templateName = null;

        // 确定需要加载的模板名称
        if (Constants.XML.equals(generators.getConfigFormat())) {
            templateName = "xml.ftl";
        } else if (Constants.PROPERTIES.equals(generators.getConfigFormat())) {
            templateName = "properties.ftl";
        } else if (Constants.PROPERTIES2.equals(generators.getConfigFormat())) {
            templateName = "properties2.ftl";
        } else if (Constants.PROPERTIES3.equals(generators.getConfigFormat())) {
            templateName = "properties3.ftl";
        } else if (Constants.PROMETHEUS.equals(generators.getConfigFormat())) {
            templateName = "alert.yml";
        } else if (Constants.CUSTOM.equals(generators.getConfigFormat())) {
            templateName = generators.getTemplateName();
        }

        if (templateName != null) {
            // 从Akka获取模板内容
            String templateContent = AkkaUtils.getTemplateContent(actorSystem, MASTER_HOST, templateName);

            if (templateContent != null) {
                // 创建字符串模板加载器
                StringTemplateLoader stringLoader = new StringTemplateLoader();
                stringLoader.putTemplate(templateName, templateContent);

                // 设置模板加载器
                config.setTemplateLoader(stringLoader);
                logger.info("从Akka加载模板成功: {}", templateName);
            } else {
                // 如果从Akka获取失败，回退到本地加载
                logger.warn("从Akka获取模板失败: {}，将回退到本地模板", templateName);
                config.setClassForTemplateLoading(FreemakerUtils.class, "/templates");
            }
        } else {
            // 如果没有找到对应的模板，使用本地模板
            config.setClassForTemplateLoading(FreemakerUtils.class, "/templates");
        }
    }

    public static void generatePromAlertFile(Generators generators, List<AlertItem> configs,
                                             String serviceName) throws IOException, TemplateException {
        // 创建核心配置对象
        Configuration config = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);

        // 得到模板对象
        String configFormat = generators.getConfigFormat();
        Template template = null;

        // 如果启用从Akka获取模板，则从Master获取模板内容
        if (Constants.PROMETHEUS.equals(configFormat)) {
            String templateName = "alert.yml";
            String templateContent = AkkaUtils.getTemplateContent(actorSystem, MASTER_HOST, templateName);

            if (templateContent != null) {
                // 创建字符串模板加载器
                StringTemplateLoader stringLoader = new StringTemplateLoader();
                stringLoader.putTemplate(templateName, templateContent);

                // 设置模板加载器
                config.setTemplateLoader(stringLoader);
                template = config.getTemplate(templateName);
                logger.info("从Akka加载模板成功: {}", templateName);
            } else {
                // 如果从Akka获取失败，回退到本地加载
                logger.warn("从Akka获取模板失败: {}，将回退到本地模板", templateName);
                config.setClassForTemplateLoading(FreemakerUtils.class, "/templates");
                template = config.getTemplate(templateName);
            }
        }


        Map<String, Object> data = new HashMap<>();
        data.put("itemList", configs);
        data.put("serviceName", serviceName);
        // 3.产生输出
        processOut(generators, template, data, "prometheus-2.17.2");
    }

    private static void processOut(Generators generators, Template template, Map<String, Object> data,
                                   String decompressPackageName) throws IOException, TemplateException {
        // 定义输出目录的路径
        String packagePath = Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName + Constants.SLASH;
        // 获取生成文件的输出目录
        String outputDirectory = generators.getOutputDirectory();

        if (outputDirectory.contains(Constants.COMMA)) {
            // 如果输出目录包含多个路径，则按照逗号分隔，并逐个处理
            for (String outPutDir : generators.getOutputDirectory().split(StrUtil.COMMA)) {
                // 构建输出文件的路径
                String outputFile = packagePath + outPutDir + Constants.SLASH + generators.getFilename();
                // 调用方法将数据模板写入到输出文件中
                writeToTemplate(template, data, outputFile);
            }
        } else if (outputDirectory.startsWith(Constants.SLASH)) {
            // 如果输出目录以斜杠开头，则直接使用输出目录作为输出文件的路径
            String outputFile = generators.getOutputDirectory() + Constants.SLASH + generators.getFilename();
            // 调用方法将数据模板写入到输出文件中
            writeToTemplate(template, data, outputFile);
        } else {
            // 如果输出目录不以斜杠开头也不包含逗号，则将输出目录添加到包路径之后作为输出文件的路径
            String outputFile = packagePath + generators.getOutputDirectory() + Constants.SLASH
                    + generators.getFilename();
            // String outputFile = generators.getOutputDirectory() + Constants.SLASH +
            // generators.getFilename();
            // 调用方法将数据模板写入到输出文件中
            writeToTemplate(template, data, outputFile);
        }
    }

    /**
     * 将数据写入模板并生成输出文件
     *
     * @param template   模板对象
     * @param data       数据映射
     * @param outputFile 输出文件路径
     * @throws IOException       当写入文件过程中发生 I/O 错误时抛出
     * @throws TemplateException 当模板处理过程中发生模板错误时抛出
     */
    private static void writeToTemplate(Template template, Map<String, Object> data,
                                        String outputFile) throws IOException, TemplateException {
        // 创建文件对象
        File file = new File(outputFile);
        // 如果文件不存在，则创建其父目录
        if (!file.exists()) {
            FileUtil.mkParentDirs(file);
        }
        // 创建文件写入器
        FileWriter out = new FileWriter(file);
        // 将数据写入模板，并将结果写入文件
        template.process(data, out);
        // 关闭文件写入器
        out.close();
    }

}
