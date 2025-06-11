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
import io.fabric8.kubernetes.api.model.batch.v1.JobCondition;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    public static void runJobWithInitContainersAndEnv(String namespace, String name, KubernetesClient client,
            VolumeMountDTO[] volumeMounts, String image, String cmd, String hostname, List<String> initContainers,
            List<String> initContainerNames, String initImage, Map<String, String> envVars) throws Exception {
        // 删除已存在的job
        log.debug("删除已存在的job(如果有), job名称: " + name);
        client.batch().v1().jobs()
                .inNamespace(namespace)
                .withName(name)
                .delete();

        long timeout = 300; // 超时时间（秒）
        long startTime = System.currentTimeMillis();

        // 等待旧Job和相关Pod删除完成
        waitForDeleteJob(namespace, name, client, timeout, startTime);

        // 提交一个包含初始化容器和环境变量的新Job
        submitJobWithInitContainersAndEnv(namespace, name, client, volumeMounts, image, cmd, hostname, initContainers,
                initContainerNames, initImage, envVars);

        long waitPodTimeout = 300; // Pod等待超时时间（秒）
        long waitPodStartTime = System.currentTimeMillis();

        // 等待Pod从创建到运行状态
        String podName = "";
        podName = waitForCreatePodOfJob(namespace, name, client, podName, waitPodStartTime, waitPodTimeout);
        log.debug("Pod名称: " + podName);

        CountDownLatch jobCompletionLatch = new CountDownLatch(1);

        // 监控Job状态变化
        AtomicBoolean isJobEndSuccess = new AtomicBoolean(false);
        Watcher<Job> watcher = new Watcher<Job>() {
            @Override
            public void eventReceived(Action action, Job resource) {
                if (resource.getStatus() != null && resource.getStatus().getConditions() != null) {
                    for (JobCondition condition : resource.getStatus().getConditions()) {
                        if ("Complete".equals(condition.getType()) && "True".equals(condition.getStatus())) {
                            log.info("Job执行成功");
                            isJobEndSuccess.set(true);
                            jobCompletionLatch.countDown();
                            return;
                        } else if ("Failed".equals(condition.getType()) && "True".equals(condition.getStatus())) {
                            log.error("Job执行失败: {}", condition.getMessage());
                            isJobEndSuccess.set(false);
                            jobCompletionLatch.countDown();
                            return;
                        }
                    }
                }
            }

            @Override
            public void onClose(WatcherException e) {
                if (e != null) {
                    log.error("Watcher关闭异常: {}", e.getMessage(), e);
                }
            }
        };

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
                    log.info("p> " + line);
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                throw new IOException();
            } finally {
                logWatch.close();
            }

            // 等待Job完成
            log.info("等待Job完成...");
            jobCompletionLatch.await();

        } catch (InterruptedException | IOException e) {
            log.error(e.getMessage(), e);
            throw new InterruptedException();
        }

        boolean flag = isJobEndSuccess.get();
        log.info("Job执行完成，执行状态: " + (flag ? "成功" : "失败"));
        if (!flag) {
            throw new RuntimeException("Job执行失败");
        }
    }

    /**
     * 提交一个包含初始化容器和环境变量的Job
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
     */
    private static void submitJobWithInitContainersAndEnv(String namespace, String name, KubernetesClient client,
            VolumeMountDTO[] volumeMounts, String image, String cmd, String hostname, List<String> initContainers,
            List<String> initContainerNames, String initImage, Map<String, String> envVars) {
        log.info("提交带有命名初始化容器和环境变量的Job: {}", name);

        // 创建环境变量列表
        List<EnvVar> envVarList = new ArrayList<>();
        if (envVars != null && !envVars.isEmpty()) {
            for (Map.Entry<String, String> entry : envVars.entrySet()) {
                envVarList.add(new EnvVarBuilder()
                        .withName(entry.getKey())
                        .withValue(entry.getValue())
                        .build());
            }
        }

        // 创建Job的Pod模板
        PodTemplateSpec podTemplateSpec = new PodTemplateSpecBuilder()
                .withNewSpec()
                .withRestartPolicy("Never")
                .withNodeSelector(Collections.singletonMap("kubernetes.io/hostname", hostname))
                .withContainers(new ContainerBuilder()
                        .withName(name)
                        .withImage(image)
                        .withCommand("sh", "-c", cmd)
                        .withVolumeMounts(convertVolumeMounts(volumeMounts))
                        .withEnv(envVarList) // 添加环境变量到主容器
                        .build())
                .endSpec()
                .build();

        // 添加初始化容器
        List<Container> initContainersList = new ArrayList<>();
        for (int i = 0; i < initContainers.size(); i++) {
            String containerName = i < initContainerNames.size() ? initContainerNames.get(i) : "init-container-" + i;
            String containerScript = initContainers.get(i);

            // 使用YAML的多行字符串格式，确保脚本正确换行
            Container container = new ContainerBuilder()
                    .withName(containerName)
                    .withImage(initImage)
                    .withCommand("sh", "-c", containerScript)
                    .withVolumeMounts(convertVolumeMounts(volumeMounts))
                    .withEnv(envVarList) // 添加环境变量到初始化容器
                    .build();

            initContainersList.add(container);
        }

        // 设置初始化容器
        podTemplateSpec.getSpec().setInitContainers(initContainersList);

        // 添加卷
        List<Volume> volumes = new ArrayList<>();
        if (volumeMounts != null) {
            for (VolumeMountDTO volumeMount : volumeMounts) {
                String volumeName = volumeMount.getVolumeName();
                String hostPath = volumeMount.getHostPath();

                if (volumeName.startsWith("configmap-")) {
                    // 处理ConfigMap卷
                    volumes.add(new VolumeBuilder()
                            .withName(volumeName)
                            .withNewConfigMap()
                            .withName(hostPath)
                            .endConfigMap()
                            .build());
                } else {
                    // 处理常规hostPath卷
                    volumes.add(new VolumeBuilder()
                            .withName(volumeName)
                            .withNewHostPath()
                            .withPath(hostPath)
                            .endHostPath()
                            .build());
                }
            }
        }
        podTemplateSpec.getSpec().setVolumes(volumes);

        // 创建Job
        Job job = new JobBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withBackoffLimit(0)
                .withTtlSecondsAfterFinished(300)
                .withTemplate(podTemplateSpec)
                .endSpec()
                .build();

        // 提交Job
        client.batch().v1().jobs().inNamespace(namespace).resource(job).create();
        log.info("Job {} 已创建", name);
    }

    /**
     * 保存Job的YAML配置到本地文件
     * 
     * @param job 要保存的Job对象
     */
    public static void saveJobYaml(Job job) {
        try {
            // 创建保存目录，使用Paths.get正确处理路径拼接
            Path dirPath = Paths.get(StrUtil.blankToDefault(Constants.YAML_PATH, Constants.INSTALL_PATH), "k8sDep",
                    "jobs");
            File dir = dirPath.toFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 生成文件名，使用Paths.get拼接路径
            Path filePath = Paths.get(dirPath.toString(),
                    job.getMetadata().getName() + ".yaml");

            // 使用K8s客户端序列化为YAML
            String yamlContent = KubeUtil.getKubernetesYaml(job);

            // 写入文件
            Files.write(filePath, yamlContent.getBytes());

            log.info("保存Job YAML文件成功: {}", filePath);

            // 添加彩色日志输出
            ColorLogUtils.printResourceCreated("JobYaml", job.getMetadata().getName(), "local");
        } catch (Exception e) {
            log.error("保存Job YAML文件失败: {}", e.getMessage(), e);
            ColorLogUtils.printError("保存Job YAML文件失败: " + e.getMessage());
        }
    }

    /**
     * 提交一个包含初始化容器的Job（使用自定义初始化容器名称）
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
     */
    public static void runJobWithInitContainers(String namespace, String name, KubernetesClient client,
            VolumeMountDTO[] volumeMounts, String image, String cmd, String hostname, List<String> initContainers,
            List<String> initContainerNames, String initImage) throws Exception {
        log.info("Submitting job with named init containers: {}", name);

        // 创建Job的Pod模板
        PodTemplateSpec podTemplateSpec = new PodTemplateSpecBuilder()
                .withNewSpec()
                .withRestartPolicy("Never")
                .withNodeSelector(Collections.singletonMap("kubernetes.io/hostname", hostname))
                .withContainers(new ContainerBuilder()
                        .withName(name)
                        .withImage(image)
                        .withCommand("sh", "-c", cmd)
                        .withVolumeMounts(convertVolumeMounts(volumeMounts))
                        .build())
                .endSpec()
                .build();

        // 添加初始化容器
        List<Container> initContainersList = new ArrayList<>();
        for (int i = 0; i < initContainers.size(); i++) {
            String containerName = i < initContainerNames.size() ? initContainerNames.get(i) : "init-container-" + i;
            String containerScript = initContainers.get(i);

            // 使用YAML的多行字符串格式，确保脚本正确换行
            Container container = new ContainerBuilder()
                    .withName(containerName)
                    .withImage(initImage)
                    .withCommand("sh", "-c", containerScript)
                    .withVolumeMounts(convertVolumeMounts(volumeMounts))
                    .build();

            initContainersList.add(container);
        }

        // 设置初始化容器
        podTemplateSpec.getSpec().setInitContainers(initContainersList);

        // 添加卷
        List<Volume> volumes = new ArrayList<>();
        for (VolumeMountDTO volumeMount : volumeMounts) {
            String volumeName = volumeMount.getVolumeName();

            // 检查是否为ConfigMap类型的卷
            if (volumeName.startsWith("configmap-")) {
                String configMapName = volumeMount.getHostPath();
                Volume volume = new VolumeBuilder()
                        .withName(volumeName)
                        .withNewConfigMap()
                        .withName(configMapName)
                        .endConfigMap()
                        .build();
                volumes.add(volume);
            }
        }
        podTemplateSpec.getSpec().setVolumes(volumes);

        // 创建并提交Job
        Job job = new JobBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withBackoffLimit(0)
                .withTemplate(podTemplateSpec)
                .withTtlSecondsAfterFinished(300) // 5分钟后自动删除已完成的Job
                .endSpec()
                .build();

        // 保存Job的YAML到本地文件
        saveJobYaml(job);

        // 提交Job到Kubernetes
        client.batch().v1().jobs().inNamespace(namespace).create(job);
        log.info("Job {} submitted successfully", name);
    }

    private static List<VolumeMount> convertVolumeMounts(VolumeMountDTO[] volumeMounts) {
        List<VolumeMount> mountList = new ArrayList<>();
        for (VolumeMountDTO volumeMount : volumeMounts) {
            String volumeName = volumeMount.getVolumeName();
            String containerPath = volumeMount.getContainerPath();

            if (volumeName.startsWith("configmap-")) {
                // 对于ConfigMap类型的卷，需要设置subPath为文件名
                String fileName = containerPath.substring(containerPath.lastIndexOf("/") + 1);
                mountList.add(new VolumeMountBuilder()
                        .withName(volumeName)
                        .withMountPath(containerPath)
                        .withSubPath(fileName)
                        .build());
            } else {
                // 原有的普通卷挂载
                mountList.add(new VolumeMountBuilder()
                        .withName(volumeName)
                        .withMountPath(containerPath)
                        .build());
            }
        }
        return mountList;
    }

}
