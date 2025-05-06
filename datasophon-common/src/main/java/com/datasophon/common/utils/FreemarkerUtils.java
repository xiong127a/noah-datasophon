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

package com.datasophon.common.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.datasophon.common.Constants;
import com.datasophon.common.model.AlertItem;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Freemarker工具类
 * 用于生成配置文件，支持多种格式和模板加载方式
 */
// TODO 业务逻辑代码重复，需要重构。 ConfigureServiceHandler K8sConfigureServiceHandler
// FreemarkerUtils 后期需要重构
@UtilityClass
public class FreemarkerUtils {

    private static final Logger logger = LoggerFactory.getLogger(FreemarkerUtils.class);

    /**
     * 生成配置文件
     *
     * @param generators            配置文件生成器
     * @param configs               配置项列表
     * @param decompressPackageName 解压后的包名
     * @throws IOException       IO异常
     * @throws TemplateException 模板异常
     */
    public static void generateConfigFile(Generators generators,
            List<ServiceConfig> configs,
            String decompressPackageName) throws IOException, TemplateException {
        generateConfigFile(generators, configs, decompressPackageName, null, false);
    }

    /**
     * 生成配置文件，支持直接模式
     *
     * @param generators            配置文件生成器
     * @param configs               配置项列表
     * @param decompressPackageName 解压后的包名
     * @param extPath               附加模板目录
     * @param directMode            是否直接模式（不进行prepareTemplateData处理）
     * @throws IOException       IO异常
     * @throws TemplateException 模板异常
     */
    public static void generateConfigFile(Generators generators,
            List<ServiceConfig> configs,
            String decompressPackageName,
            String extPath,
            boolean directMode) throws IOException, TemplateException {
        // 创建核心配置对象
        Configuration config = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);

        // 设置加载的目录
        List<TemplateLoader> loaderList = new ArrayList<>();
        loaderList.add(new ClassTemplateLoader(FreemarkerUtils.class, "/worker/templates"));
        if (StringUtils.isNotBlank(extPath) && new File(extPath).exists()) {
            // 如果第三方package中存在templates模板，则直接加载
            loaderList.add(new FileTemplateLoader(new File(extPath)));
        }
        config.setTemplateLoader(new MultiTemplateLoader(loaderList.toArray(new TemplateLoader[0])));

        // 获取模板对象
        String templateName = determineTemplateName(generators);
        if (templateName == null) {
            throw new IllegalArgumentException("不支持的配置格式: " + generators.getConfigFormat());
        }

        Template template = config.getTemplate(templateName);
        logger.info("load template: {} success.", Objects.requireNonNull(template).getSourceName());

        // 处理数据
        Map<String, Object> data;
        if (directMode) {
            // 直接模式，不进行数据处理
            data = new HashMap<>();
            data.put("itemList", configs);
        } else {
            // 标准模式，使用prepareRenderData处理
            // 获取主机名和IP用于变量替换
            Map<String, String> paramMap = new HashMap<>();
            try {
                String hostName = InetAddress.getLocalHost().getHostName();
                String ip = NetUtil.getIpByHost(hostName);
                paramMap.put("${host}", hostName);
                paramMap.put("${ip}", ip);
                paramMap.put("${user}", "root");
            } catch (Exception e) {
                logger.error("获取主机信息失败: {}", e.getMessage());
            }

            // 使用prepareRenderData方法处理数据
            data = prepareRenderData(generators, configs, paramMap, logger);
        }

        // 生成输出
        processOut(generators, template, data, decompressPackageName);
    }

    /**
     * 生成Prometheus告警规则文件
     *
     * @param generators  配置文件生成器
     * @param configs     告警项列表
     * @param serviceName 服务名称
     * @throws IOException       IO异常
     * @throws TemplateException 模板异常
     */
    public static void generatePromAlertFile(Generators generators, List<AlertItem> configs,
            String serviceName) throws IOException, TemplateException {
        // 创建核心配置对象
        Configuration config = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        config.setClassForTemplateLoading(FreemarkerUtils.class, "/worker/templates");

        // 获取模板对象
        String templateName = determineTemplateName(generators);
        if (templateName == null) {
            throw new IllegalArgumentException("不支持的配置格式: " + generators.getConfigFormat());
        }

        Template template = config.getTemplate(templateName);

        Map<String, Object> data = new HashMap<>();
        data.put("itemList", configs);
        data.put("serviceName", serviceName);
        // 生成输出
        processOut(generators, template, data, "prometheus-2.17.2");
    }

    /**
     * 使用字符串模板生成配置文件，带处理模式选项
     *
     * @param generators            配置文件生成器
     * @param configs               配置项列表
     * @param templateContent       模板内容字符串
     * @param templateName          模板名称
     * @param decompressPackageName 解压后的包名
     * @param directMode            是否直接模式（不进行prepareTemplateData处理）
     * @throws IOException       IO异常
     * @throws TemplateException 模板异常
     */
    public static void generateConfigFileFromString(Generators generators,
            List<ServiceConfig> configs,
            String templateContent,
            String templateName,
            String decompressPackageName) throws IOException, TemplateException {
        if (StringUtils.isBlank(templateContent)) {
            // 如果模板内容为空，使用标准方法
            generateConfigFile(generators, configs, decompressPackageName);
            return;
        }

        // 创建核心配置对象
        Configuration config = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);

        // 创建字符串模板加载器
        StringTemplateLoader stringLoader = new StringTemplateLoader();
        stringLoader.putTemplate(templateName, templateContent);
        config.setTemplateLoader(stringLoader);

        // 获取模板
        Template template = config.getTemplate(templateName);
        logger.info("从字符串加载模板成功: {}", templateName);

        // 获取主机名和IP用于变量替换
        Map<String, String> paramMap = new HashMap<>();
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            String ip = NetUtil.getIpByHost(hostName);
            paramMap.put("${host}", hostName);
            paramMap.put("${ip}", ip);
            paramMap.put("${user}", "root");
        } catch (Exception e) {
            logger.error("获取主机信息失败: {}", e.getMessage());
        }

        // 使用prepareRenderData方法处理数据
        Map<String, Object> data = prepareRenderData(generators, configs, paramMap, logger);

        // 生成输出
        processOut(generators, template, data, decompressPackageName);
    }

    /**
     * 使用字符串模板生成Prometheus告警规则文件
     *
     * @param generators      配置文件生成器
     * @param configs         告警项列表
     * @param serviceName     服务名称
     * @param templateContent 模板内容字符串
     * @throws IOException       IO异常
     * @throws TemplateException 模板异常
     */
    public static void generatePromAlertFileFromString(Generators generators, List<AlertItem> configs,
            String serviceName, String templateContent) throws IOException, TemplateException {
        if (StringUtils.isBlank(templateContent)) {
            // 如果模板内容为空，使用标准方法
            generatePromAlertFile(generators, configs, serviceName);
            return;
        }

        // 获取模板名称
        String templateName = determineTemplateName(generators);
        if (templateName == null) {
            throw new IllegalArgumentException("不支持的配置格式: " + generators.getConfigFormat());
        }

        // 创建核心配置对象
        Configuration config = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);

        // 创建字符串模板加载器
        StringTemplateLoader stringLoader = new StringTemplateLoader();
        stringLoader.putTemplate(templateName, templateContent);
        config.setTemplateLoader(stringLoader);

        // 获取模板
        Template template = config.getTemplate(templateName);
        logger.info("从字符串加载告警模板成功");

        Map<String, Object> data = new HashMap<>();
        data.put("itemList", configs);
        data.put("serviceName", serviceName);
        // 生成输出
        processOut(generators, template, data, "prometheus-2.17.2");
    }

    /**
     * 处理配置文件输出
     *
     * @param generators            配置文件生成器
     * @param template              模板对象
     * @param data                  数据映射
     * @param decompressPackageName 解压后的包名
     * @throws IOException       IO异常
     * @throws TemplateException 模板异常
     */
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
    public static void writeToTemplate(Template template, Map<String, Object> data,
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

    /**
     * 根据配置格式确定要使用的模板名称
     *
     * @param generators 配置文件生成器
     * @return 模板名称，如果不支持的配置格式则返回null
     * @throws IllegalArgumentException 如果CUSTOM类型但templateName为空时抛出
     */
    public static String determineTemplateName(Generators generators) {
        String templateName = null;
        String configFormat = generators.getConfigFormat();

        // 确定需要加载的模板名称
        if (Constants.XML.equals(configFormat)) {
            templateName = "xml.ftl";
        } else if (Constants.PROPERTIES.equals(configFormat)) {
            templateName = "properties.ftl";
        } else if (Constants.PROPERTIES2.equals(configFormat)) {
            templateName = "properties2.ftl";
        } else if (Constants.PROPERTIES3.equals(configFormat)) {
            templateName = "properties3.ftl";
        } else if (Constants.PROMETHEUS.equals(configFormat)) {
            templateName = "alert.yml";
        } else if (Constants.CUSTOM.equals(configFormat)) {
            if (StringUtils.isBlank(generators.getTemplateName())) {
                throw new IllegalArgumentException("CUSTOM类型配置必须指定templateName");
            }

            templateName = generators.getTemplateName();
            // 对模板名称进行简单校验
            if (!templateName.contains(".")) {
                logger.warn("自定义模板名称缺少文件扩展名: {}", templateName);
            }
        } else {
            logger.warn("不支持的配置格式: {}", configFormat);
        }

        return templateName;
    }

    /**
     * 处理配置值，确保类型正确
     * 
     * @param config   配置项
     * @param paramMap 参数映射
     */
    private static void processConfigValue(ServiceConfig config, Map<String, String> paramMap) {
        if (config == null || config.getValue() == null) {
            return;
        }

        // 处理不同类型的值
        if (StringUtils.isNotBlank(config.getType())) {
            switch (config.getType()) {
                case Constants.INPUT:
                    String value = PlaceholderUtils.replacePlaceholders(
                            String.valueOf(config.getValue()),
                            paramMap,
                            Constants.REGEX_VARIABLE);
                    config.setValue(value);
                    break;
                case Constants.MULTIPLE:
                    processMultipleValue(config);
                    break;
                default:
                    break;
            }
        }

        // 处理布尔值和整数
        if (config.getValue() instanceof Boolean || config.getValue() instanceof Integer) {
            config.setValue(String.valueOf(config.getValue()));
        }
    }

    /**
     * 处理数组类型的值
     * 
     * @param config 配置项
     */
    private static void processMultipleValue(ServiceConfig config) {
        try {
            if (config.getValue() instanceof JSONArray) {
                JSONArray value = (JSONArray) config.getValue();
                List<String> strs = value.toJavaList(String.class);
                String separator = config.getSeparator() != null ? config.getSeparator() : ",";
                String joinValue = String.join(separator, strs);

                // 处理开闭符号
                if (StrUtil.isAllNotBlank(config.getOpen(), config.getClose())) {
                    joinValue = config.getOpen() + joinValue + config.getClose();
                }
                config.setValue(joinValue);
            } else if (config.getValue() instanceof List) {
                List<?> list = (List<?>) config.getValue();
                String separator = config.getSeparator() != null ? config.getSeparator() : ",";
                String joinValue = list.stream()
                        .map(item -> item == null ? "" : String.valueOf(item))
                        .collect(Collectors.joining(separator));

                // 处理开闭符号
                if (StrUtil.isAllNotBlank(config.getOpen(), config.getClose())) {
                    joinValue = config.getOpen() + joinValue + config.getClose();
                }
                config.setValue(joinValue);
            }
        } catch (Exception e) {
            logger.error("处理数组类型值失败: {}", e.getMessage());
        }
    }

    /**
     * 准备模板数据，处理配置项并返回用于模板渲染的数据Map
     */
    public static Map<String, Object> prepareTemplateData(String configFormat, List<ServiceConfig> configs) {
        Map<String, Object> data = new HashMap<>();
        if (configs == null) {
            return data;
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
            logger.error("获取主机信息失败: {}", e.getMessage());
        }

        // 处理每个配置项的值
        List<ServiceConfig> processedConfigs = new ArrayList<>();
        for (ServiceConfig config : configs) {
            ServiceConfig processed = new ServiceConfig();
            BeanUtil.copyProperties(config, processed);
            processConfigValue(processed, paramMap);
            processedConfigs.add(processed);
        }

        // 处理CUSTOM类型的配置
        if (Constants.CUSTOM.equals(configFormat)) {
            // 将configType为"map"的配置项转换为键值对形式
            Map<Boolean, List<ServiceConfig>> partitionedConfigs = processedConfigs.stream()
                    .collect(Collectors.partitioningBy(e -> "map".equals(e.getConfigType())));

            // map类型的配置直接放入data
            data.putAll(partitionedConfigs.get(true).stream()
                    .collect(Collectors.toMap(ServiceConfig::getName, ServiceConfig::getValue)));

            // 非map类型的配置放入itemList
            processedConfigs = partitionedConfigs.get(false);
        }

        data.put("itemList", processedConfigs);
        return data;
    }

    /**
     * 渲染模板并返回生成的字符串内容
     *
     * @param template 模板对象
     * @param data     模板数据
     * @return 渲染后的内容字符串
     * @throws TemplateException 模板处理异常
     * @throws IOException       IO异常
     */
    public static String renderTemplateToString(Template template, Map<String, Object> data)
            throws TemplateException, IOException {
        StringWriter writer = new StringWriter();
        template.process(data, writer);
        writer.close();
        return writer.toString();
    }

    /**
     * 渲染模板并返回生成的内容
     *
     * @param template 模板对象
     * @param data     模板数据
     * @return 渲染后的内容字节数组
     * @throws TemplateException 模板处理异常
     * @throws IOException       IO异常
     */
    public static byte[] renderTemplateToBytes(Template template, Map<String, Object> data)
            throws TemplateException, IOException {
        String content = renderTemplateToString(template, data);
        return StrUtil.utf8Bytes(content);
    }

    /**
     * 从模板内容字符串创建Template对象
     *
     * @param templateContent 模板内容
     * @param templateName    模板名称
     * @return Template对象
     * @throws IOException 创建模板过程中的IO异常
     */
    public static Template createTemplateFromContent(String templateContent, String templateName) throws IOException {
        if (StringUtils.isBlank(templateContent)) {
            throw new IllegalArgumentException("模板内容不能为空");
        }

        // 创建核心配置对象
        Configuration config = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);

        // 创建字符串模板加载器
        StringTemplateLoader stringLoader = new StringTemplateLoader();
        stringLoader.putTemplate(templateName, templateContent);
        config.setTemplateLoader(stringLoader);

        // 获取并返回模板
        return config.getTemplate(templateName);
    }

    /**
     * 统一处理配置列表，包括值转换、自定义配置处理等
     * 供Worker和K8s模块使用
     * 
     * @param generators 配置生成器
     * @param configs    配置列表
     * @param paramMap   参数映射表
     * @param logger     日志对象
     * @return 处理后的配置列表，包括自定义配置
     */
    public static List<ServiceConfig> processConfigList(Generators generators,
            List<ServiceConfig> configs,
            Map<String, String> paramMap,
            Logger logger) {
        if (configs == null || configs.isEmpty()) {
            return new ArrayList<>();
        }

        String dataDir = "";
        ArrayList<ServiceConfig> customConfList = new ArrayList<>();
        Iterator<ServiceConfig> iterator = configs.iterator();

        while (iterator.hasNext()) {
            ServiceConfig config = iterator.next();

            // 处理不同类型的值
            if (StringUtils.isNotBlank(config.getType())) {
                switch (config.getType()) {
                    case Constants.INPUT:
                        String value = PlaceholderUtils.replacePlaceholders(
                                config.getValue() instanceof String ? (String) config.getValue()
                                        : String.valueOf(config.getValue()),
                                paramMap,
                                Constants.REGEX_VARIABLE);
                        config.setValue(value);
                        break;
                    case Constants.MULTIPLE:
                        conventToStr(config, logger);
                        break;
                    default:
                        break;
                }
            }

            // 处理自定义配置
            if (Constants.CUSTOM.equals(config.getConfigType())) {
                addToCustomList(iterator, customConfList, config, logger);
            }

            // 移除不需要的配置项
            if (!config.isRequired() && !Constants.CUSTOM.equals(config.getConfigType())) {
                if (StrUtil.equals("map2", config.getConfigType())) {
                    config.setConfigType("map");
                } else {
                    iterator.remove();
                    continue;
                }
            }

            // 处理布尔值和整数类型
            if (config.getValue() instanceof Boolean || config.getValue() instanceof Integer) {
                if (logger != null) {
                    logger.info("Convert boolean and integer to string");
                }
                config.setValue(String.valueOf(config.getValue()));
            }

            // 记录dataDir
            if ("dataDir".equals(config.getName())) {
                if (logger != null) {
                    logger.info("Find dataDir : {}", config.getValue());
                }
                dataDir = (String) config.getValue();
            }
        }

        // 添加自定义配置
        configs.addAll(customConfList);
        return configs;
    }

    /**
     * 处理数组类型的值
     * 
     * @param config 配置项
     * @param logger 日志对象
     * @return 处理后的字符串值
     */
    public static String conventToStr(ServiceConfig config, Logger logger) {
        try {
            if (config.getValue() instanceof JSONArray) {
                JSONArray value = (JSONArray) config.getValue();
                List<String> strs = value.toJavaList(String.class);

                if (logger != null) {
                    logger.info("Array size is: {}", strs.size());
                }

                String separator = config.getSeparator() != null ? config.getSeparator() : ",";
                String joinValue = String.join(separator, strs);
                String finalValue = joinValue;

                if (StrUtil.isAllNotBlank(config.getOpen(), config.getClose())) {
                    finalValue = config.getOpen() + joinValue + config.getClose();
                }

                config.setValue(finalValue);

                if (logger != null) {
                    logger.info("Config set value to: {}", config.getValue());
                }

                return finalValue;
            } else if (config.getValue() instanceof List) {
                List<?> list = (List<?>) config.getValue();

                if (logger != null) {
                    logger.info("List size is: {}", list.size());
                }

                String separator = config.getSeparator() != null ? config.getSeparator() : ",";
                String joinValue = list.stream()
                        .map(item -> item == null ? "" : String.valueOf(item))
                        .collect(Collectors.joining(separator));

                String finalValue = joinValue;
                if (StrUtil.isAllNotBlank(config.getOpen(), config.getClose())) {
                    finalValue = config.getOpen() + joinValue + config.getClose();
                }

                config.setValue(finalValue);

                if (logger != null) {
                    logger.info("Config set value to: {}", config.getValue());
                }

                return finalValue;
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.error("处理数组类型值失败: {}", e.getMessage(), e);
            }
        }
        return String.valueOf(config.getValue());
    }

    /**
     * 处理自定义配置列表
     * 
     * @param iterator       配置迭代器
     * @param customConfList 自定义配置列表
     * @param config         当前配置
     * @param logger         日志对象
     */
    public static void addToCustomList(Iterator<ServiceConfig> iterator,
            List<ServiceConfig> customConfList,
            ServiceConfig config,
            Logger logger) {
        try {
            List<JSONObject> list = (List<JSONObject>) config.getValue();
            iterator.remove();

            for (JSONObject json : list) {
                if (Objects.nonNull(json)) {
                    Set<String> set = json.keySet();
                    for (String key : set) {
                        if (StringUtils.isNotBlank(key)) {
                            ServiceConfig serviceConfig = new ServiceConfig();
                            serviceConfig.setName(key);
                            serviceConfig.setValue(json.get(key));
                            customConfList.add(serviceConfig);
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.error("处理自定义配置失败: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 提取公共的配置处理逻辑，供WorkerFreemarkerUtils和K8sFreeMakerUtils使用
     *
     * @param generators 配置生成器
     * @param configs    配置列表
     * @param params     参数映射
     * @param logger     日志对象
     * @return 处理后的模板数据
     */
    public static Map<String, Object> prepareRenderData(Generators generators,
            List<ServiceConfig> configs,
            Map<String, String> params,
            Logger logger) {
        // 处理配置列表
        List<ServiceConfig> processedConfigs = processConfigList(generators, configs, params, logger);

        // 准备渲染数据
        return prepareTemplateData(generators.getConfigFormat(), processedConfigs);
    }

    /**
     * 直接使用配置列表生成配置文件，不进行额外处理
     * 用于保持原始业务逻辑一致性，避免prepareTemplateData的处理逻辑
     *
     * @param generators            配置文件生成器
     * @param configs               配置项列表
     * @param templateContent       模板内容
     * @param templateName          模板名称
     * @param decompressPackageName 解压后的包名
     * @throws IOException       IO异常
     * @throws TemplateException 模板异常
     */
    public static void generateConfigFileDirectly(Generators generators,
            List<ServiceConfig> configs,
            String templateContent,
            String templateName,
            String decompressPackageName) throws IOException, TemplateException {
        // 从模板内容创建Template对象
        Template template = createTemplateFromContent(templateContent, templateName);

        // 创建数据模型，直接将原始配置列表放入itemList，不做任何处理
        Map<String, Object> data = new HashMap<>();
        data.put("itemList", configs);

        // 处理文件输出
        String packagePath = Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName + Constants.SLASH;
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
            // 调用方法将数据模板写入到输出文件中
            writeToTemplate(template, data, outputFile);
        }
    }

    /**
     * 直接根据模板内容字符串和变量映射渲染为结果字符串
     * 
     * @param templateContent 模板内容字符串
     * @param variables       变量映射（键值对）
     * @return 渲染后的内容字符串
     * @throws IOException       IO异常
     * @throws TemplateException 模板异常
     */
    public static String renderFromTemplateContent(String templateContent, Map<String, Object> variables)
            throws IOException, TemplateException {
        if (StringUtils.isBlank(templateContent)) {
            return "";
        }

        // 创建模板
        Template template = createTemplateFromContent(templateContent, "dynamic_template");

        // 渲染并返回结果
        return renderTemplateToString(template, variables);
    }

    /**
     * 直接根据模板内容字符串和String类型变量映射渲染为结果字符串
     * 
     * @param templateContent 模板内容字符串
     * @param stringVariables 字符串变量映射（键值对）
     * @return 渲染后的内容字符串
     * @throws IOException       IO异常
     * @throws TemplateException 模板异常
     */
    public static String renderFromTemplateContentWithStringVars(String templateContent,
            Map<String, String> stringVariables)
            throws IOException, TemplateException {
        // 将String类型的变量Map转换为Object类型的变量Map
        Map<String, Object> variables = new HashMap<>();
        if (stringVariables != null) {
            for (Map.Entry<String, String> entry : stringVariables.entrySet()) {
                variables.put(entry.getKey(), entry.getValue());
            }
        }

        return renderFromTemplateContent(templateContent, variables);
    }

}