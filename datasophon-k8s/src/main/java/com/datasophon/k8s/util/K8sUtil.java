package com.datasophon.k8s.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.command.ExecuteCmdCommand;
import com.datasophon.common.model.VolumeMountDTO;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
public class K8sUtil {

    public static ExecResult executeCommand(String namespace, KubernetesClient client, String image, String hostname,
            List<String> commands) {
        List<Pod> pods = client.pods().inNamespace(namespace).withLabel("app", image).list().getItems();
        ExecResult execResult = new ExecResult();
        List<String> hostList = pods.stream().map(pod -> pod.getSpec().getNodeName()).collect(Collectors.toList());

        if (CollUtil.isEmpty(pods) || !hostList.contains(hostname)) {
            log.debug("host {} pods {} is null", hostname, image);
            execResult.setExecResult(false);
            return execResult;
        }

        try {
            for (Pod pod : pods) {
                String nodeName = pod.getSpec().getNodeName();
                if (nodeName != null && nodeName.equals(hostname)) {
                    String podName = pod.getMetadata().getName();
                    long startTime = System.currentTimeMillis(); // Start timing
                    log.debug("Command is {}", commands);

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

                    // Execute the command
                    try (ExecWatch exec = client.pods()
                            .inNamespace(namespace)
                            .withName(podName)
                            .writingOutput(outputStream)
                            .writingError(errorStream)
                            .exec(commands.toArray(new String[0]))) {
                        int exitCode = exec.exitCode().get();
                        String out = IoUtil.toStr(outputStream, Charset.defaultCharset());
                        String error = IoUtil.toStr(errorStream, Charset.defaultCharset());

                        if (exitCode != 0) {
                            execResult.setExecResult(false);
                            if (StrUtil.isBlank(error)) {
                                error = out;
                            }
                            execResult.setExecErrOut(error);
                            log.error("exec result: {}", error);
                        } else {
                            execResult.setExecResult(true);
                            execResult.setExecOut(out);
                            log.debug("exec result: {}", out);
                        }
                    }

                    long endTime = System.currentTimeMillis(); // End timing
                    long duration = endTime - startTime; // Calculate duration
                    log.debug("Command execution time: {} milliseconds", duration);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return execResult;
    }

    public static ExecResult runCmd(String namespace, KubernetesClient client, String image, String hostname,
            String cmd) {
        List<String> commands = handlerCommand(cmd);

        // 调用公共的 executeCommand 方法
        return executeCommand(namespace, client, image, hostname, commands);
    }

    private static List<String> handlerCommand(String cmd) {
        // 对命令进行适当的处理，确保其在执行时不会被拆分
        return Arrays.asList("sh", "-c", cmd);
    }

    public static ExecResult runCmd(String namespace, KubernetesClient client, String image, String hostname,
            ExecuteCmdCommand cmdCommand) {
        // 获取 ExecuteCmdCommand 中的命令列表
        List<String> commands = cmdCommand.getCommands();
        if (CollUtil.isNotEmpty(commands)) {
            String commandLine = String.join(" ", commands); // 合并命令为单一字符串
            commands = handlerCommand(commandLine);
        }

        if (StrUtil.isNotBlank(cmdCommand.getCommandLine())) {
            commands = handlerCommand(cmdCommand.getCommandLine());
        }

        // 调用公共的 executeCommand 方法
        return executeCommand(namespace, client, image, hostname, commands);
    }

    public static ExecResult exec(ClusterServiceRoleInstanceEntity roleInstanceEntity, String kubeConfig,
            ExecuteCmdCommand cmdCommand) {
        KubernetesClient kubeClient = KubeUtil.getKubeClientByConfig(kubeConfig);
        return runCmd(Constants.DATASOPHON,
                kubeClient,
                (roleInstanceEntity.getServiceName() + "-" + roleInstanceEntity.getServiceRoleName()).toLowerCase(),
                roleInstanceEntity.getHostname(),
                cmdCommand);
    }

    public static void runJob(String namespace, String name, KubernetesClient client, VolumeMountDTO[] volumeMounts,
            String image, String cmd, String hostname) throws Exception {
        // delete job
        log.debug("delete job if need ,job name: " + name);
        List<StatusDetails> statusDetailsList = client.batch().v1().jobs()
                .inNamespace(namespace)
                .withName(name)
                .delete();

        long timeout = 300; // Timeout in seconds
        long startTime = System.currentTimeMillis();

        // 尝试删除已存在的同名 Job，循环等待 Job 和相关 Pod 被删除完成
        waitForDeleteJob(namespace, name, client, timeout, startTime);

        // 提交一个新的 Job
        submitJob(namespace, name, client, volumeMounts, image, cmd, hostname);

        long waitPodTimeout = 300; // Timeout in seconds
        long waitPodStartTime = System.currentTimeMillis();

        // 进入一个循环等待 Pod 从创建到运行的状态。如果 Pod 处于 Pending 状态，方法会继续等待，直到 Pod 变为 Running 状态。
        String podName = "";
        podName = waitForCreatePodOfJob(namespace, name, client, podName, waitPodStartTime, waitPodTimeout);
        log.debug("Pod name: " + podName);

        CountDownLatch jobCompletionLatch = new CountDownLatch(1);

        // 使用 Watch 机制监控 Job 的状态变化，判断 Job 是否成功或失败，并相应地记录日志。
        AtomicBoolean isJobEndSuccess = new AtomicBoolean(false);
        Watcher<Job> watcher = new Watcher<Job>() {
            @Override
            public void eventReceived(Action action, Job job) {

                if (action == Action.ADDED || action == Action.MODIFIED) {
                    JobStatus status = job.getStatus();
                    if (status != null) {
                        boolean isJobSuccessful = status.getSucceeded() != null && status.getSucceeded() > 0;
                        boolean isJobFailed = status.getFailed() != null && status.getFailed() > 0;

                        if (isJobSuccessful) {
                            isJobEndSuccess.set(true);
                            jobCompletionLatch.countDown();
                        } else if (isJobFailed) {
                            isJobEndSuccess.set(false);
                            jobCompletionLatch.countDown();
                        }
                    }
                }
            }

            @Override
            public void onClose(WatcherException cause) {
                log.info("Watcher closed");
                if (cause != null) {
                    log.error(cause.getMessage(), cause);
                }
            }

        };

        // 使用 LogWatch 输出 Pod 的运行日志，直到 Job 完成
        try (Watch watch = client.batch().v1().jobs()
                .inNamespace(namespace)
                .withName(name)
                .watch(watcher);
                LogWatch logWatch = client.pods()
                        .inNamespace(namespace)
                        .withName(podName)
                        .watchLog()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(logWatch.getOutput()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("p> " + line); // You can replace this with your desired logging mechanism
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                throw new IOException();
            } finally {
                logWatch.close();
            }

            // Wait for the job to complete
            log.info("Waiting  for job to complete...");
            jobCompletionLatch.await();

        } catch (InterruptedException | IOException e) {
            log.error(e.getMessage(), e);
            throw new InterruptedException();
        }

        boolean flag = isJobEndSuccess.get();
        log.info("Job completed with success status: " + flag);
        if (!flag) {
            throw new RuntimeException("Job failed.");
        }
    }

    private static String waitForCreatePodOfJob(String namespace, String jobName, KubernetesClient client,
            String podName, long waitPodStartTime, long waitPodTimeout) {
        // 循环等待创建pod成功
        while (true) {
            // 需要考虑，有可能pod还没创建出来
            // 正在创建也不行
            // {"kind":"Status","apiVersion":"v1","metadata":{},"status":"Failure","message":"container
            // \"init\" in pod \"init-flinkdir-hdfs-6c9r5\" is waiting to start:
            // ContainerCreating","reason":"BadRequest","code":400}
            // Get the pod name associated with the job
            List<Pod> pods = client.pods()
                    .inNamespace(namespace)
                    .withLabel("job-name", jobName)
                    .list().getItems();

            if (!pods.isEmpty()) {
                Pod pod = pods.get(0);
                podName = pod.getMetadata().getName();
                // 检查pod是否正在创建
                String phase = pod.getStatus().getPhase();
                if (phase.equals("Pending")) {
                    log.info("Pod {} is pending, waiting for it to be running...", podName);
                } else if (phase.equals("Running")) {
                    log.info("Pod {} is running.", podName);
                    break;
                }
            }

            // Check timeout
            long elapsedTime = System.currentTimeMillis() - waitPodStartTime;
            if (TimeUnit.MILLISECONDS.toSeconds(elapsedTime) > waitPodTimeout) {
                throw new RuntimeException("Timeout reached. Job Pod create failed.");
            }

            // Polling interval
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return podName;
    }

    private static void waitForDeleteJob(String namespace, String jobName, KubernetesClient client, long timeout,
            long startTime) {

        // 循环等待删除成功
        while (true) {
            Job deletedJob = client.batch().v1().jobs()
                    .inNamespace(namespace)
                    .withName(jobName)
                    .get();

            List<Pod> podList = client.pods()
                    .inNamespace(namespace)
                    .withLabel("job-name", jobName)
                    .list().getItems();

            if (deletedJob == null && podList.isEmpty()) {
                log.info("Job {} has been deleted.", jobName);
                return;
            }

            // Check timeout
            long elapsedTime = System.currentTimeMillis() - startTime;
            if (TimeUnit.MILLISECONDS.toSeconds(elapsedTime) > timeout) {
                throw new RuntimeException("Timeout reached. Job deletion failed.");
            }

            // Polling interval
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void submitJob(String namespace, String name, KubernetesClient client, VolumeMountDTO[] volumeMounts,
            String image, String cmd, String hostname) {
        List<Volume> volumeList = Arrays.stream(volumeMounts).map(volumeMount -> {
            // 判断是否是ConfigMap类型
            if (volumeMount.getVolumeName().startsWith("configmap-")) {
                // ConfigMap类型的卷，此时hostPath字段存储ConfigMap名称
                String configMapName = volumeMount.getHostPath();
                return new VolumeBuilder()
                        .withName(volumeMount.getVolumeName())
                        .withConfigMap(new ConfigMapVolumeSourceBuilder()
                                .withName(configMapName)
                                .build())
                        .build();
            } else {
                // 原有的HostPath类型
                HostPathVolumeSource hostPathVolume = new HostPathVolumeSourceBuilder()
                        .withPath(volumeMount.getHostPath())
                        .build();

                return new VolumeBuilder()
                        .withName(volumeMount.getVolumeName())
                        .withHostPath(hostPathVolume) // 本地目录
                        .build();
            }
        }).collect(Collectors.toList());

        List<VolumeMount> mountList = Arrays.stream(volumeMounts).map(volumeMountDTO -> {
            String volumeName = volumeMountDTO.getVolumeName();
            if (volumeName.startsWith("configmap-")) {
                // 对于ConfigMap类型的卷，需要设置subPath为文件名
                String containerPath = volumeMountDTO.getContainerPath();
                String fileName = containerPath.substring(containerPath.lastIndexOf("/") + 1);
                return new VolumeMountBuilder()
                        .withName(volumeName)
                        .withMountPath(containerPath)
                        .withSubPath(fileName) // 使用文件名作为subPath
                        .build();
            } else {
                // 原有的普通卷挂载
                return new VolumeMountBuilder()
                        .withName(volumeMountDTO.getVolumeName())
                        .withMountPath(volumeMountDTO.getContainerPath())
                        .build();
            }
        }).collect(Collectors.toList());

        Container container = new ContainerBuilder()
                .withName("init")
                .withImage(image)
                .withCommand("sh", "-c", cmd)
                .withVolumeMounts(mountList) // 挂载
                .build();

        Job job = new JobBuilder()
                .withNewMetadata()
                .withName(name)
                .endMetadata()
                .withNewSpec()
                .withBackoffLimit(0)
                .withNewTemplate()
                .withNewSpec()
                .withVolumes(volumeList)
                .withNodeSelector(Collections.singletonMap("kubernetes.io/hostname", hostname))
                .withContainers(container).withHostNetwork(true)
                .withRestartPolicy("Never")
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();

        client.batch().v1().jobs()
                .inNamespace(namespace)
                .resource(job).create();

        // 添加彩色日志输出
        ColorLogUtils.printResourceCreated("Job", name, namespace);
    }

    /**
     * 运行一个带有初始化容器和环境变量的Kubernetes Job
     * 
     * @param namespace          Kubernetes命名空间
     * @param name               Job名称
     * @param client             Kubernetes客户端
     * @param volumeMounts       卷挂载配置
     * @param image              容器镜像
     * @param cmd                容器执行的命令
     * @param hostname           主机名
     * @param initContainers     初始化容器执行的命令列表
     * @param initContainerNames 初始化容器名称列表
     * @param initImage          初始化容器使用的镜像
     * @param envVars            环境变量映射
     * @throws Exception 执行过程中可能出现的异常
     */
    public static void runJobWithInitContainersAndEnv(String namespace, String jobName, KubernetesClient client,
            VolumeMountDTO[] volumeMounts, String image, String command, String host, List<String> initContainers,
            List<String> initContainerNames, String initContainerImage, Map<String, String> envVars) {

        try {
            log.info("Creating job: {} in namespace: {}", jobName, namespace);

            // 构建环境变量列表
            List<EnvVar> envVarList = new ArrayList<>();
            for (Map.Entry<String, String> entry : envVars.entrySet()) {
                envVarList.add(new EnvVarBuilder()
                        .withName(entry.getKey())
                        .withValue(entry.getValue())
                        .build());
            }

            // 设置主容器
            ContainerBuilder mainContainerBuilder = new ContainerBuilder()
                    .withName(jobName)
                    .withImage(image);

            // 添加命令，确保正确处理多行脚本
            List<String> commandList = new ArrayList<>();
            commandList.add("sh");
            commandList.add("-c");
            commandList.add(command.replaceAll("\\\\n", "\n"));
            mainContainerBuilder.withCommand(commandList);

            // 添加环境变量
            mainContainerBuilder.withEnv(envVarList);

            // 设置卷挂载
            if (volumeMounts != null && volumeMounts.length > 0) {
                List<VolumeMount> containerVolumeMounts = new ArrayList<>();
                for (VolumeMountDTO mount : volumeMounts) {
                    containerVolumeMounts.add(new VolumeMountBuilder()
                            .withName(mount.getVolumeName())
                            .withMountPath(mount.getContainerPath())
                            .withSubPath(new File(mount.getContainerPath()).getName())
                            .build());
                }
                mainContainerBuilder.withVolumeMounts(containerVolumeMounts);
            }

            // 构建init containers
        List<Container> initContainersList = new ArrayList<>();
        for (int i = 0; i < initContainers.size(); i++) {
                String initContainerName = initContainerNames.get(i);
                String initContainerCommand = initContainers.get(i);

                // 设置初始化容器
                ContainerBuilder initContainerBuilder = new ContainerBuilder()
                        .withName(initContainerName)
                        .withImage(initContainerImage);

                // 添加命令，确保正确处理多行脚本
                List<String> initCommandList = new ArrayList<>();
                initCommandList.add("sh");
                initCommandList.add("-c");
                initCommandList.add(initContainerCommand.replaceAll("\\\\n", "\n"));
                initContainerBuilder.withCommand(initCommandList);

                // 添加环境变量
                initContainerBuilder.withEnv(envVarList);

                // 设置init container的卷挂载
                if (volumeMounts != null && volumeMounts.length > 0) {
                    List<VolumeMount> containerVolumeMounts = new ArrayList<>();
                    for (VolumeMountDTO mount : volumeMounts) {
                        containerVolumeMounts.add(new VolumeMountBuilder()
                                .withName(mount.getVolumeName())
                                .withMountPath(mount.getContainerPath())
                                .withSubPath(new File(mount.getContainerPath()).getName())
                                .build());
                    }
                    initContainerBuilder.withVolumeMounts(containerVolumeMounts);
                }

                initContainersList.add(initContainerBuilder.build());
            }

            // 构建卷列表
        List<Volume> volumes = new ArrayList<>();
            if (volumeMounts != null && volumeMounts.length > 0) {
                for (VolumeMountDTO mount : volumeMounts) {
                    if (mount.getVolumeName().startsWith("configmap-")) {
                        // 处理ConfigMap类型的卷
                        volumes.add(new VolumeBuilder()
                                .withName(mount.getVolumeName())
                                .withConfigMap(new ConfigMapVolumeSourceBuilder()
                                        .withName(mount.getHostPath())
                                        .build())
                                .build());
                    } else {
                        // 处理hostPath类型的卷
                        volumes.add(new VolumeBuilder()
                                .withName(mount.getVolumeName())
                                .withHostPath(new HostPathVolumeSourceBuilder()
                                        .withPath(mount.getHostPath())
                                        .build())
                                .build());
                    }
                }
            }

            // 构建和创建Job
        Job job = new JobBuilder()
                .withNewMetadata()
                    .withName(jobName)
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withBackoffLimit(0)
                    .withTtlSecondsAfterFinished(300)
                    .withNewTemplate()
                    .withNewSpec()
                    .withContainers(mainContainerBuilder.build())
                    .withInitContainers(initContainersList)
                    .withRestartPolicy("Never")
                    .withVolumes(volumes)
                    .withNodeSelector(Collections.singletonMap("kubernetes.io/hostname", host))
                    .endSpec()
                    .endTemplate()
                .endSpec()
                .build();

            // 将Job保存为YAML文件（用于调试）
            saveJobAsYaml(job, jobName);

            // 创建Job
        client.batch().v1().jobs().inNamespace(namespace).create(job);
            log.info("Job created successfully: {}", jobName);

        } catch (Exception e) {
            log.error("Failed to create job: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create job: " + e.getMessage(), e);
        }
    }

    /**
     * 将Job保存为YAML文件（用于调试）
     * 
     * @param job     Job对象
     * @param jobName 作业名称
     */
    private static void saveJobAsYaml(Job job, String jobName) {
        try {
            // 确保k8sDep/jobs目录存在
            File jobsDir = new File("k8sDep/jobs");
            if (!jobsDir.exists()) {
                jobsDir.mkdirs();
            }

            // 创建YAML文件
            File yamlFile = new File(jobsDir, jobName + ".yaml");

            // 配置YAML序列化选项
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            options.setDefaultScalarStyle(DumperOptions.ScalarStyle.LITERAL); // 使用|风格的多行字符串

            // 将Job序列化为YAML
            Yaml yaml = new Yaml(options);
            FileWriter writer = new FileWriter(yamlFile);
            writer.write("---\n");
            yaml.dump(job, writer);
            writer.close();

            log.info("Job YAML saved to: {}", yamlFile.getAbsolutePath());
        } catch (Exception e) {
            log.warn("Failed to save job YAML: {}", e.getMessage());
            // 不抛出异常，保持主流程继续执行
        }
    }

    /**
     * 从ConfigMap中提取配置项的值
     * 
     * @param configMap ConfigMap对象
     * @param fileName  配置文件名称（如core-site.xml）
     * @param key       配置项键名
     * @return 配置项的值，如果不存在则返回null
     */
    public static String getConfigValueFromConfigMap(ConfigMap configMap, String fileName, String key) {
        if (configMap == null || configMap.getData() == null || !configMap.getData().containsKey(fileName)) {
            log.warn("ConfigMap中不存在文件: {}", fileName);
            return null;
        }

        String fileContent = configMap.getData().get(fileName);
        if (fileContent == null || fileContent.isEmpty()) {
            log.warn("ConfigMap中的文件内容为空: {}", fileName);
            return null;
        }

        // 根据文件类型选择不同的解析方法
        if (fileName.endsWith(".xml")) {
            return getPropertyFromXmlString(fileContent, key);
        } else if (fileName.endsWith(".properties")) {
            return getPropertyFromPropertiesString(fileContent, key);
            } else {
            log.warn("不支持解析的文件类型: {}", fileName);
            return null;
        }
    }

    /**
     * 从ConfigMap中获取多个键的值
     * 
     * @param client        Kubernetes客户端
     * @param namespace     命名空间
     * @param configMapName ConfigMap名称
     * @param fileName      配置文件名称
     * @param keys          要获取的键列表
     * @return 键值映射
     */
    public static Map<String, String> getConfigValuesFromConfigMap(
            KubernetesClient client, String namespace,
            String configMapName, String fileName,
            String... keys) {
        Map<String, String> values = new HashMap<>();

        ConfigMap configMap = client.configMaps()
                .inNamespace(namespace)
                .withName(configMapName)
                .get();

        if (configMap == null) {
            log.warn("未找到ConfigMap: {}", configMapName);
            return values;
        }

        for (String key : keys) {
            String value = getConfigValueFromConfigMap(configMap, fileName, key);
            if (value != null) {
                values.put(key, value);
                log.info("从ConfigMap {} 的文件 {} 中获取配置项: {} = {}",
                        configMapName, fileName, key, value);
            }
        }

        return values;
    }

    /**
     * 从XML字符串中获取指定属性的值
     * 
     * @param xmlContent   XML内容字符串
     * @param propertyName 属性名
     * @return 属性值，如果不存在则返回null
     */
    public static String getPropertyFromXmlString(String xmlContent, String propertyName) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // 禁用外部实体引用，防止XXE攻击
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes()));
            doc.getDocumentElement().normalize();

            NodeList propertyList = doc.getElementsByTagName("property");
            for (int i = 0; i < propertyList.getLength(); i++) {
                Element property = (Element) propertyList.item(i);
                NodeList nameNodes = property.getElementsByTagName("name");
                if (nameNodes.getLength() > 0) {
                    String name = nameNodes.item(0).getTextContent();
                    if (propertyName.equals(name)) {
                        NodeList valueNodes = property.getElementsByTagName("value");
                        if (valueNodes.getLength() > 0) {
                            return valueNodes.item(0).getTextContent();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析XML字符串时出错: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 从properties格式的字符串中获取指定键的值
     * 
     * @param propertiesContent properties格式的内容字符串
     * @param key               要查找的键
     * @return 键对应的值，如果不存在则返回null
     */
    public static String getPropertyFromPropertiesString(String propertiesContent, String key) {
        try {
            java.util.Properties props = new java.util.Properties();
            props.load(new ByteArrayInputStream(propertiesContent.getBytes()));
            return props.getProperty(key);
        } catch (Exception e) {
            log.error("解析Properties字符串时出错: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从ConfigMap中获取指定文件的所有配置项
     * 
     * @param configMap ConfigMap对象
     * @param fileName  配置文件名称（如core-site.xml）
     * @return 配置项的键值映射
     */
    public static Map<String, String> getAllPropertiesFromConfigMap(ConfigMap configMap, String fileName) {
        Map<String, String> properties = new HashMap<>();

        if (configMap == null || configMap.getData() == null || !configMap.getData().containsKey(fileName)) {
            log.warn("ConfigMap中不存在文件: {}", fileName);
            return properties;
        }

        String fileContent = configMap.getData().get(fileName);
        if (fileContent == null || fileContent.isEmpty()) {
            log.warn("ConfigMap中的文件内容为空: {}", fileName);
            return properties;
        }

        // 根据文件类型选择不同的解析方法
        if (fileName.endsWith(".xml")) {
            return getAllPropertiesFromXmlString(fileContent);
        } else if (fileName.endsWith(".properties")) {
            return getAllPropertiesFromPropertiesString(fileContent);
        } else {
            log.warn("不支持解析的文件类型: {}", fileName);
            return properties;
        }
    }

    /**
     * 从XML字符串中获取所有属性的键值对
     * 
     * @param xmlContent XML内容字符串
     * @return 所有属性的键值映射
     */
    public static Map<String, String> getAllPropertiesFromXmlString(String xmlContent) {
        Map<String, String> properties = new HashMap<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // 禁用外部实体引用，防止XXE攻击
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes()));
            doc.getDocumentElement().normalize();

            NodeList propertyList = doc.getElementsByTagName("property");
            for (int i = 0; i < propertyList.getLength(); i++) {
                Element property = (Element) propertyList.item(i);
                NodeList nameNodes = property.getElementsByTagName("name");
                NodeList valueNodes = property.getElementsByTagName("value");

                if (nameNodes.getLength() > 0 && valueNodes.getLength() > 0) {
                    String name = nameNodes.item(0).getTextContent();
                    String value = valueNodes.item(0).getTextContent();
                    properties.put(name, value);
                }
            }
        } catch (Exception e) {
            log.error("解析XML字符串时出错: {}", e.getMessage(), e);
        }
        return properties;
    }

    /**
     * 从properties格式的字符串中获取所有属性的键值对
     * 
     * @param propertiesContent properties格式的内容字符串
     * @return 所有属性的键值映射
     */
    public static Map<String, String> getAllPropertiesFromPropertiesString(String propertiesContent) {
        Map<String, String> properties = new HashMap<>();
        try {
            java.util.Properties props = new java.util.Properties();
            props.load(new ByteArrayInputStream(propertiesContent.getBytes()));

            for (String key : props.stringPropertyNames()) {
                properties.put(key, props.getProperty(key));
            }
        } catch (Exception e) {
            log.error("解析Properties字符串时出错: {}", e.getMessage(), e);
            return properties;
        }
        return properties;
    }

}
