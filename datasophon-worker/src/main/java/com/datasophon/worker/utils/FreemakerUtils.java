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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.model.AlertItem;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
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
import java.util.stream.Collectors;

public class FreemakerUtils {

    private static final Logger logger = LoggerFactory.getLogger(FreemakerUtils.class);

    public static void generateConfigFile(Generators generators,
                                          List<ServiceConfig> configs,
                                          String decompressPackageName) throws IOException, TemplateException {
        generateConfigFile(generators, configs, decompressPackageName, null);
    }

    /**
     *
     * 支持 从附加的目录加载 模版
     *
     * @param generators
     * @param configs
     * @param decompressPackageName
     * @param extPath
     * @throws IOException
     * @throws TemplateException
     */
    public static void generateConfigFile(Generators generators,
                                          List<ServiceConfig> configs,
                                          String decompressPackageName,
                                          String extPath) throws IOException, TemplateException {
        // 1.加载模板
        // 创建核心配置对象
        Configuration config = new Configuration(Configuration.getVersion());
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
            data = configs.stream().filter(e -> "map".equals(e.getConfigType()))
                    .collect(Collectors.toMap(key -> key.getName(), value -> value.getValue()));
            configs = configs.stream().filter(e -> !"map".equals(e.getConfigType())).collect(Collectors.toList());
        }
        logger.info("load template: {} success.", template.getSourceName());
        data.put("itemList", configs);
        // 3.产生输出
        processOut(generators, template, data, decompressPackageName);
    }


    public static void generatePromAlertFile(Generators generators, List<AlertItem> configs,
                                             String serviceName) throws IOException, TemplateException {
        // 创建核心配置对象
        Configuration config = new Configuration(Configuration.getVersion());
        // 设置加载的目录
        // ""代表当前包
        config.setClassForTemplateLoading(FreemakerUtils.class, "/templates");
        // 得到模板对象
        String configFormat = generators.getConfigFormat();
        Template template = null;

        if (Constants.PROMETHEUS.equals(configFormat)) {
            template = config.getTemplate("alert.yml");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("itemList", configs);
        data.put("serviceName", serviceName);
        // 3.产生输出
        processOut(generators, template, data, "prometheus-2.17.2");
    }

    public static void generatePromScrapeConfig(Generators generators, List<ServiceConfig> configs,
                                                String serviceName) throws IOException, TemplateException {
        // 创建核心配置对象
        Configuration config = new Configuration(Configuration.getVersion());
        // 设置加载的目录
        // ""代表当前包
        config.setClassForTemplateLoading(FreemakerUtils.class, "/templates");
        // 得到模板对象
        Template template = config.getTemplate("scrape.ftl");

        Map<String, Object> data = new HashMap<>();
        data.put("itemList", configs);
        // 3.产生输出
        processOut(generators, template, data, serviceName);
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
            String outputFile = packagePath + generators.getOutputDirectory() + Constants.SLASH + generators.getFilename();
//            String outputFile = generators.getOutputDirectory() + Constants.SLASH + generators.getFilename();
            // 调用方法将数据模板写入到输出文件中
            writeToTemplate(template, data, outputFile);
        }
    }


    /**
     * 将数据写入模板并生成输出文件
     *
     * @param template 模板对象
     * @param data 数据映射
     * @param outputFile 输出文件路径
     * @throws IOException 当写入文件过程中发生 I/O 错误时抛出
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
