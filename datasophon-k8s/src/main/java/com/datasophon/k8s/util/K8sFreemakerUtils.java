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

package com.datasophon.k8s.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.model.AlertItem;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.k8s.constants.Constant;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

public class K8sFreemakerUtils {

    private static final Logger logger = LoggerFactory.getLogger(K8sFreemakerUtils.class);

    public static void generateConfigFile(Generators generators,
                                          List<ServiceConfig> configs,
                                          String serviceRoleName,
                                          String kubeConfig) throws IOException, TemplateException {
        generateConfigFile(generators, configs, serviceRoleName, null, kubeConfig);
    }

    /**
     * 支持 从附加的目录加载 模版
     *
     * @param generators
     * @param configs
     * @param serviceRoleName
     * @param extPath
     * @throws IOException
     * @throws TemplateException
     */
    public static void generateConfigFile(Generators generators,
                                          List<ServiceConfig> configs,
                                          String serviceRoleName,
                                          String extPath,
                                          String kubeConfig) throws IOException, TemplateException {
        // 1.加载模板
        // 创建核心配置对象
        Configuration config = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        // 设置加载的目录
        List<TemplateLoader> loaderList = new ArrayList<>();
        loaderList.add(new ClassTemplateLoader(K8sFreemakerUtils.class, "/templates"));
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
        String configMapName = generateConfigMapName(serviceRoleName, generators);
        writeToConfigMap(template, data, configMapName, generators.getFilename(), kubeConfig);
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
    public static void writeToTemplate(Template template, Map<String, Object> data, String outputFile, String hostname)
            throws IOException, TemplateException {
        // 使用 StringWriter 合并模板和数据
        StringWriter stringWriter = new StringWriter();
        template.process(data, stringWriter);

        // 获取生成的内容
        String generatedContent = stringWriter.toString();

        // 将内容写入到远程系统
        K8sMinaUtils.writeUtf8String(hostname, generatedContent, outputFile);
    }

    public static void writeToTemplateLocal(Template template, Map<String, Object> data, String outputFile)
            throws IOException, TemplateException {
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
     * 将数据写入模板并生成 ConfigMap
     *
     * @param template      模板对象
     * @param data          数据映射
     * @param configMapName ConfigMap 的名称
     * @throws IOException       当写入文件过程中发生 I/O 错误时抛出
     * @throws TemplateException 当模板处理过程中发生模板错误时抛出
     */
    public static void writeToConfigMap(Template template, Map<String, Object> data, String configMapName, String fileName, String kubeConfig)
            throws IOException, TemplateException {
        // 使用 StringWriter 合并模板和数据
        StringWriter stringWriter = new StringWriter();
        template.process(data, stringWriter);

        // 获取生成的内容
        String generatedContent = stringWriter.toString();
        // 将内容创建为 ConfigMap
        createConfigMap(configMapName, generatedContent, kubeConfig, fileName);
    }

    /**
     * 创建 Kubernetes ConfigMap
     *
     * @param configMapName    ConfigMap 的名称
     * @param generatedContent 渲染后的配置内容
     * @throws IOException
     */
    public static void createConfigMap(String configMapName, String generatedContent, String kubeConfig, String fileName) throws IOException {
        // 获取 Kubernetes 客户端
        KubernetesClient client = KubeUtil.getKubeClientByConfig(kubeConfig);

        // 创建 ConfigMap 对象
        ConfigMap configMap = new ConfigMap();
        configMap.setMetadata(new ObjectMeta());
        configMap.getMetadata().setName(configMapName);  // 设置 ConfigMap 名称
        configMap.getMetadata().setNamespace(Constant.K8S_NAMESPACE); // 设置 ConfigMap 命名空间
        if (generatedContent.contains("{{HOST}}")){
            fileName+=".example";
        }
        // 将渲染后的内容加入到 ConfigMap 的 data 中
        configMap.setData(Collections.singletonMap(fileName, generatedContent));

        // 创建新的 ConfigMap
        try {
            client.configMaps()
                    .inNamespace(Constant.K8S_NAMESPACE)
                    .withName(configMapName)
                    .createOrReplace(configMap);
            System.out.println("ConfigMap " + configMapName + " created in namespace " + Constant.K8S_NAMESPACE + ".");
        } catch (Exception e) {
            // 处理创建过程中的异常
            System.err.println("Error creating ConfigMap: " + e.getMessage());
            throw new IOException("Error creating ConfigMap", e);
        }
    }

    public static String generateConfigMapName(String serviceRoleName, Generators generators) {
        if (serviceRoleName == null || generators == null) {
            throw new IllegalArgumentException("serviceRoleName and generators must not be null");
        }
        return serviceRoleName.toLowerCase() + "-" + generators.getFilename().replace('.', '-');
    }
}
