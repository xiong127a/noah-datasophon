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
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
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
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@UtilityClass
@Slf4j
public class K8sFreeMakerUtils {

    @Getter
    @Setter
    private static Map<String, Map<String, ConfigMap>> configMapCache = new HashMap<>();

    // 添加Secret缓存
    @Getter
    @Setter
    private static Map<String, Map<String, Secret>> secretCache = new HashMap<>();

    // 添加Prometheus配置文件缓存
    @Getter
    @Setter
    private static Map<String, Map<String, String>> prometheusConfigCache = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(K8sFreeMakerUtils.class);

    /**
     * 支持 从附加的目录加载 模版
     *
     * @param generators 生成器对象，包含模板配置信息
     * @param configs    服务配置列表，包含需要渲染到模板中的配置项
     * @param extPath    附加模板路径，用于加载额外的模板文件
     * @throws IOException       当模板加载或写入过程中发生I/O错误时抛出
     * @throws TemplateException 当模板处理过程中发生模板错误时抛出
     */

    public static void generateConfigFile(Generators generators,
            List<ServiceConfig> configs,
            String extPath, String serviceRoleFullName) throws IOException, TemplateException {
        // 1.加载模板
        // 创建核心配置对象
        Configuration config = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        // 使用方括号语法替代后，不再需要特别设置命名约定
        // 设置加载的目录
        List<TemplateLoader> loaderList = new ArrayList<>();
        loaderList.add(new ClassTemplateLoader(K8sFreeMakerUtils.class, "/worker/templates"));
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
                    .collect(Collectors.toMap(ServiceConfig::getName, ServiceConfig::getValue));
            configs = configs.stream().filter(e -> !"map".equals(e.getConfigType())).collect(Collectors.toList());
        }
        logger.info("load template: {} success.", Objects.requireNonNull(template).getSourceName());
        data.put("itemList", configs);
        // 3.产生输出
        String configMapName = generateConfigMapName(serviceRoleFullName, generators);
        writeToConfigMap(template, data, configMapName, generators.getFilename(), serviceRoleFullName);
    }

    /**
     * 将数据写入模板并生成本地文件
     *
     * @param template   模板对象
     * @param data       数据映射
     * @param outputFile 输出文件路径
     * @throws IOException       当写入文件过程中发生 I/O 错误时抛出
     * @throws TemplateException 当模板处理过程中发生模板错误时抛出
     */
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
    public static void writeToConfigMap(Template template, Map<String, Object> data,
            String configMapName,
            String fileName, String serviceRoleFullName)
            throws IOException, TemplateException {
        // 使用 StringWriter 合并模板和数据
        StringWriter stringWriter = new StringWriter();
        UnixNewlineWriter unixNewlineWriter = new UnixNewlineWriter(stringWriter);

        template.process(data, unixNewlineWriter);

        // 获取生成的内容
        String generatedContent = unixNewlineWriter.target.toString();
        // 将内容创建为 ConfigMap
        cacheConfigMap(configMapName, generatedContent, fileName, serviceRoleFullName);
    }

    /**
     * 创建 Kubernetes ConfigMap
     *
     * @param configMapName    ConfigMap 的名称
     * @param generatedContent 渲染后的配置内容
     */
    public static void cacheConfigMap(String configMapName, String generatedContent,
            String fileName, String serviceRoleFullName) {
        if (StrUtil.startWith(fileName, Constants.K8S_CONFIG_PREFIX)) {
            return;
        }
        // 处理prometheus配置写入PVC
        if (StrUtil.equals(serviceRoleFullName, "prometheus-update")) {
            // 将prometheus配置文件保存到PVC中
            savePrometheusConfigToPVC(fileName, generatedContent, serviceRoleFullName);
            log.info("Prometheus配置文件 {} 已准备好写入PVC", fileName);
            return;
        }
        // 创建 ConfigMap 对象
        ConfigMap configMap = new ConfigMap();
        configMap.setMetadata(new ObjectMeta());
        configMap.getMetadata().setName(configMapName); // 设置 ConfigMap 名称
        configMap.getMetadata().setNamespace(Constant.K8S_NAMESPACE); // 设置 ConfigMap 命名空间
        if (StrUtil.isNotBlank(serviceRoleFullName)) {
            Map<String, String> labels = configMap.getMetadata().getLabels();
            if (labels == null) {
                labels = new HashMap<>();
                configMap.getMetadata().setLabels(labels);
            }
            labels.put("app", serviceRoleFullName);
        }
        // 将渲染后的内容加入到 ConfigMap 的 data 中
        configMap.setData(Collections.singletonMap(fileName, generatedContent));
        Map<String, ConfigMap> cache = configMapCache.get(serviceRoleFullName);
        if (ObjectUtil.isNull(cache)) {
            cache = new HashMap<>();
        }
        cache.put(configMapName, configMap);
        configMapCache.put(serviceRoleFullName, cache);
    }

    public static void createConfigMap(String serviceRoleFullName, KubernetesClient client) {
        Map<String, ConfigMap> cache = configMapCache.get(serviceRoleFullName);
        if (cache == null || cache.isEmpty()) {
            log.info("No ConfigMaps found for {}", serviceRoleFullName);
            return;
        }

        Set<String> keySet = cache.keySet();
        for (String configMapName : keySet) {
            ConfigMap configMap = cache.get(configMapName);
            // 保存ConfigMap YAML到本地
            K8sServiceHandler.saveConfigMapYaml(configMap);
            // 创建新的 ConfigMap
            try {
                // 使用 createOrReplace 创建或替换ConfigMap
                client.configMaps().inNamespace(Constant.K8S_NAMESPACE).createOrReplace(configMap);
                log.info("ConfigMap {} 已成功创建在命名空间 {}", configMapName, Constant.K8S_NAMESPACE);

                // 添加彩色日志输出
                ColorLogUtils.printResourceCreated("ConfigMap", configMapName, Constant.K8S_NAMESPACE);
            } catch (Exception e) {
                log.error("创建ConfigMap时出错: {}", e.getMessage());
                ColorLogUtils.printError("创建ConfigMap " + configMapName + " 失败：" + e.getMessage());
                throw new RuntimeException("创建ConfigMap时出错: " + e.getMessage());
            }
        }
        configMapCache.remove(serviceRoleFullName);
    }

    /**
     * 创建数据库凭据Secret
     * 
     * @param serviceRoleFullName 服务角色全名，用作Secret名称前缀
     * @param secretData          包含Secret数据的Map
     * @param secretSuffix        Secret名称后缀，通常为"-db-secret"
     */
    public static void cacheDatabaseSecret(String serviceRoleFullName, Map<String, String> secretData,
            String secretSuffix) {
        if (secretData == null || secretData.isEmpty()) {
            log.warn("数据库凭据为空，不创建Secret");
            return;
        }

        String secretName = serviceRoleFullName.toLowerCase() + secretSuffix;

        // 将普通字符串值转换为base64编码
        Map<String, String> encodedData = new HashMap<>();
        for (Map.Entry<String, String> entry : secretData.entrySet()) {
            encodedData.put(entry.getKey(),
                    Base64.getEncoder().encodeToString(entry.getValue().getBytes(StandardCharsets.UTF_8)));
        }

        // 创建Secret对象
        Secret secret = new Secret();
        secret.setMetadata(new ObjectMeta());
        secret.getMetadata().setName(secretName);
        secret.getMetadata().setNamespace(Constant.K8S_NAMESPACE);

        // 设置标签
        Map<String, String> labels = new HashMap<>();
        labels.put("app", serviceRoleFullName);
        secret.getMetadata().setLabels(labels);

        // 设置数据
        secret.setData(encodedData);
        secret.setType("Opaque");

        // 添加到缓存
        Map<String, Secret> cache = secretCache.get(serviceRoleFullName);
        if (cache == null) {
            cache = new HashMap<>();
        }
        cache.put(secretName, secret);
        secretCache.put(serviceRoleFullName, cache);

        log.info("数据库Secret {} 已添加到缓存", secretName);
    }

    /**
     * 创建所有缓存的Secret
     * 
     * @param serviceRoleFullName 服务角色全名
     * @param client              Kubernetes客户端
     */
    public static void createSecrets(String serviceRoleFullName, KubernetesClient client) {
        Map<String, Secret> cache = secretCache.get(serviceRoleFullName);
        if (cache == null || cache.isEmpty()) {
            log.info("No Secrets found for {}", serviceRoleFullName);
            return;
        }

        for (Map.Entry<String, Secret> entry : cache.entrySet()) {
            String secretName = entry.getKey();
            Secret secret = entry.getValue();

            try {
                // 使用createOrReplace创建或替换Secret
                client.secrets().inNamespace(Constant.K8S_NAMESPACE).createOrReplace(secret);
                log.info("Secret {} 已成功创建在命名空间 {}", secretName, Constant.K8S_NAMESPACE);

                // 添加彩色日志输出
                ColorLogUtils.printResourceCreated("Secret", secretName, Constant.K8S_NAMESPACE);
            } catch (Exception e) {
                log.error("创建Secret时出错: {}", e.getMessage());
                ColorLogUtils.printError("创建Secret " + secretName + " 失败：" + e.getMessage());
                throw new RuntimeException("创建Secret时出错: " + e.getMessage());
            }
        }

        secretCache.remove(serviceRoleFullName);
    }

    // 获取或创建缓存客户端[8](@ref)

    public static String generateConfigMapName(String serviceRoleFullName, Generators generators) {
        if (serviceRoleFullName == null || generators == null) {
            throw new IllegalArgumentException("serviceRoleFullName and generators must not be null");
        }
        return serviceRoleFullName.toLowerCase() + "-" + generators.getFilename().replace('.', '-').replace("_", "-");
    }

    /**
     * 将Prometheus配置文件保存到缓存，等待一次性写入PVC
     *
     * @param fileName            文件名
     * @param fileContent         文件内容
     * @param serviceRoleFullName 服务角色全名
     */
    private static void savePrometheusConfigToPVC(String fileName, String fileContent,
            String serviceRoleFullName) {
        try {
            // 获取该服务角色的配置缓存，如果不存在则创建
            Map<String, String> configsCache = prometheusConfigCache.computeIfAbsent(serviceRoleFullName,
                    k -> new HashMap<>());

            // 将配置文件添加到缓存
            configsCache.put(fileName, fileContent);

            log.info("Prometheus配置文件 {} 已添加到缓存，当前缓存文件数: {}", fileName, configsCache.size());

        } catch (Exception e) {
            log.error("缓存Prometheus配置时出错: {}", e.getMessage(), e);
            throw new RuntimeException("缓存Prometheus配置失败", e);
        }
    }

    /**
     * 监控Job执行完成情况
     *
     * @param client  Kubernetes客户端
     * @param jobName Job名称
     */
    private static void watchJobCompletion(KubernetesClient client, String jobName) {
        try {
            // 等待Job完成，最多等待30秒
            int maxRetries = 30;
            int retryCount = 0;

            while (retryCount < maxRetries) {
                Job job = client.batch().jobs().inNamespace(Constant.K8S_NAMESPACE).withName(jobName).get();
                if (job == null) {
                    log.warn("Job {} 不存在", jobName);
                    break;
                }

                JobStatus status = job.getStatus();
                if (status != null) {
                    Integer succeeded = status.getSucceeded();
                    Integer failed = status.getFailed();

                    if (succeeded != null && succeeded > 0) {
                        log.info("Prometheus配置文件 {} 更新成功", "批量配置文件");
                        break;
                    }

                    if (failed != null && failed > 0) {
                        log.error("更新Prometheus配置文件 {} 失败", "批量配置文件");
                        // 获取Job的Pod日志
                        try {
                            PodList podList = client.pods().inNamespace(Constant.K8S_NAMESPACE)
                                    .withLabel("job-name", jobName).list();
                            if (podList != null && !podList.getItems().isEmpty()) {
                                String podName = podList.getItems().get(0).getMetadata().getName();
                                String logs = client.pods().inNamespace(Constant.K8S_NAMESPACE)
                                        .withName(podName).getLog();
                                log.error("Job Pod {} 日志: {}", podName, logs);
                            }
                        } catch (Exception e) {
                            log.error("无法获取Job Pod日志", e);
                        }
                        break;
                    }
                }

                // 等待1秒再检查
                Thread.sleep(1000);
                retryCount++;
            }

            if (retryCount >= maxRetries) {
                log.warn("监控Job {} 超时，状态未知", jobName);
            }

        } catch (Exception e) {
            log.error("监控Job执行状态时出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 将缓存的所有Prometheus配置文件一次性写入PVC
     * 
     * @param kubeConfig          Kubernetes配置
     * @param serviceRoleFullName 服务角色全名
     */
    public static void flushPrometheusConfigsToPVC(String kubeConfig, String serviceRoleFullName) {
        // 获取该服务角色的配置缓存
        Map<String, String> configsCache = prometheusConfigCache.get(serviceRoleFullName);
        if(StrUtil.equals("prometheus-update" ,serviceRoleFullName)){
            serviceRoleFullName="prometheus-prometheus";
        }
        // 如果缓存为空，直接返回
        if (configsCache == null || configsCache.isEmpty()) {
            log.info("没有需要写入PVC的Prometheus配置文件");
            return;
        }

        try {
            // 构建Kubernetes API客户端
            KubernetesClient client = KubeUtil.getKubeClientByConfig(kubeConfig);

            // 创建临时Job来更新Prometheus配置文件
            String jobName = "prometheus-configs-updater-" + System.currentTimeMillis();

            // 确定PVC名称
            String pvcName = serviceRoleFullName + "-pvc";

            // 确定Pod名称 - 使用索引为0的Pod
            String podName = serviceRoleFullName + "-0";

            // 配置挂载路径 - 与Prometheus Pod相同
            String configMountPath = "/opt/datasophon/prometheus/configs";

            log.info("准备创建Job {} 写入 {} 个配置文件到PVC: {}", jobName, configsCache.size(), pvcName);

            // 构建命令脚本
            StringBuilder scriptBuilder = new StringBuilder();
            scriptBuilder.append("#!/bin/sh\n");
            scriptBuilder.append("echo \"[$(date '+%Y-%m-%d %H:%M:%S')] 开始更新 ").append(configsCache.size())
                    .append(" 个Prometheus配置文件\"\n");

            // 为每个配置文件添加写入命令
            int fileIndex = 0;
            for (Map.Entry<String, String> entry : configsCache.entrySet()) {
                String fileName = entry.getKey();
                String fileContent = entry.getValue();
                String configPath = configMountPath + "/" + fileName;
                String fileBase64 = Base64.getEncoder().encodeToString(fileContent.getBytes(StandardCharsets.UTF_8));

                fileIndex++;
                scriptBuilder.append("\n# 处理文件 ").append(fileIndex).append(": ").append(fileName).append("\n");
                scriptBuilder.append("echo \"[$(date '+%Y-%m-%d %H:%M:%S')] 处理文件 ").append(fileIndex).append("/")
                        .append(configsCache.size()).append(": ").append(fileName).append("\"\n");
                scriptBuilder.append("echo \"[$(date '+%Y-%m-%d %H:%M:%S')] 目标路径: ").append(configPath).append("\"\n");
                scriptBuilder.append("mkdir -p $(dirname ").append(configPath).append(")\n");
                scriptBuilder.append("echo ").append(fileBase64).append(" | base64 -d > ").append(configPath)
                        .append("\n");
                scriptBuilder.append("if [ $? -eq 0 ]; then\n");
                scriptBuilder.append("  echo \"[$(date '+%Y-%m-%d %H:%M:%S')] 成功写入配置文件 ").append(fileName)
                        .append("\"\n");
                scriptBuilder.append("  echo \"[$(date '+%Y-%m-%d %H:%M:%S')] 文件详情: $(ls -la ").append(configPath)
                        .append(")\"\n");
                scriptBuilder.append("  echo \"[$(date '+%Y-%m-%d %H:%M:%S')] 文件大小: $(stat -c %s ").append(configPath)
                        .append(") 字节\"\n");
                scriptBuilder.append("else\n");
                scriptBuilder.append("  echo \"[$(date '+%Y-%m-%d %H:%M:%S')] 错误: 写入配置文件 ").append(fileName)
                        .append(" 失败\"\n");
                scriptBuilder.append("  exit 1\n");
                scriptBuilder.append("fi\n");
            }

            scriptBuilder.append("\necho \"[$(date '+%Y-%m-%d %H:%M:%S')] 所有 ").append(configsCache.size())
                    .append(" 个配置文件更新成功完成\"\n");

            // 创建Job对象
            Job job = new JobBuilder()
                    .withNewMetadata()
                    .withName(jobName)
                    .withNamespace(Constant.K8S_NAMESPACE)
                    .addToLabels("app", serviceRoleFullName)
                    .addToLabels("managed-by", "datasophon")
                    .addToLabels("job-type", "config-update-batch")
                    .endMetadata()
                    .withNewSpec()
                    .withBackoffLimit(2) // 失败重试次数
                    .withTtlSecondsAfterFinished(300) // 完成后5分钟删除
                    .withNewTemplate()
                    .withNewMetadata()
                    .addToLabels("app", jobName)
                    .endMetadata()
                    .withNewSpec()
                    .addNewContainer()
                    .withName("config-updater")
                    .withImage(DockerImageUtils.getString("BUSYBOX"))
                    .addNewEnv()
                    .withName("POD_NAME")
                    .withValue(podName)
                    .endEnv()
                    .addNewEnv()
                    .withName("POD_NAMESPACE")
                    .withValue(Constant.K8S_NAMESPACE)
                    .endEnv()
                    .withCommand("/bin/sh", "-c")
                    .withArgs(scriptBuilder.toString())
                    .addNewVolumeMount()
                    .withName("prometheus-data")
                    .withMountPath(configMountPath)
                    .withSubPathExpr("$(POD_NAMESPACE)/$(POD_NAME)")
                    .endVolumeMount()
                    .endContainer()
                    .addNewVolume()
                    .withName("prometheus-data")
                    .withNewPersistentVolumeClaim()
                    .withClaimName(pvcName)
                    .endPersistentVolumeClaim()
                    .endVolume()
                    .withRestartPolicy("Never")
                    .endSpec()
                    .endTemplate()
                    .endSpec()
                    .build();

            // 提交Job到Kubernetes
            client.batch().jobs().inNamespace(Constant.K8S_NAMESPACE).createOrReplace(job);

            log.info("创建批量配置更新Job: {}, 写入 {} 个配置文件", jobName, configsCache.size());

            // 监控Job执行状态
            watchJobCompletion(client, jobName);

            // 清空缓存
            prometheusConfigCache.remove(serviceRoleFullName);

        } catch (Exception e) {
            log.error("批量保存Prometheus配置到PVC时出错: {}", e.getMessage(), e);
            throw new RuntimeException("批量保存Prometheus配置失败", e);
        }
    }
}
