package com.datasophon.kubernetes.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.common.command.ExecuteCmdCommand;
import com.datasophon.common.model.VolumeMountDTO;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.mapper.ClusterInfoMapper;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.HostPathVolumeSource;
import io.fabric8.kubernetes.api.model.HostPathVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import io.fabric8.kubernetes.client.dsl.ScalableResource;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
public class KubernetesUtil {

    public static ExecResult executeCommand(String namespace, KubernetesClient client, String serviceRoleFullName,
            String hostname,
            List<String> commands) {
        List<Pod> pods = client.pods().inNamespace(namespace).withLabel("app", serviceRoleFullName).list().getItems();
        ExecResult execResult = new ExecResult();
        List<String> hostList = pods.stream().map(pod -> pod.getSpec().getNodeName()).toList();

        if (CollUtil.isEmpty(pods) || !hostList.contains(hostname)) {
            log.debug("host {} pods {} is null", hostname, serviceRoleFullName);
            execResult.setExecResult(false);
            return execResult;
        }

        try {
            for (Pod pod : pods) {
                String nodeName = pod.getSpec().getNodeName();
                if (nodeName != null && nodeName.equals(hostname)) {
                    String podName = pod.getMetadata().getName();
                    long startTime = System.currentTimeMillis(); // Start timing

                    // 增加详细日志，显示完整命令及其参数
                    String fullCommand = String.join(" ", commands);
                    log.debug("Executing command in pod {} on node {}: {}", podName, hostname, fullCommand);
                    log.debug("Command array: {}", commands);

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

                        log.debug("Command exit code: {}, output: {}, error: {}", exitCode, out, error);

                        if (exitCode != 0) {
                            execResult.setExecResult(false);
                            if (StrUtil.isBlank(error)) {
                                error = out;
                            }
                            // 如果错误输出为空，尝试获取更多信息
                            if (StrUtil.isBlank(error)) {
                                error = "Execution failed with exit code: " + exitCode;
                            }
                            execResult.setExecErrOut(error);
                            log.error("Execution failed with exit code {}: {}", exitCode, error);
                        } else {
                            execResult.setExecResult(true);
                            execResult.setExecOut(out);
                            log.debug("Execution successful: {}", out);
                        }
                    }

                    long endTime = System.currentTimeMillis(); // End timing
                    long duration = endTime - startTime; // Calculate duration
                    log.debug("Command execution time: {} milliseconds", duration);
                }
            }
        } catch (Exception e) {
            log.error("Error executing command: {}", e.getMessage(), e);
            execResult.setExecResult(false);
            execResult.setExecErrOut("Error executing command: " + e.getMessage());
            throw new RuntimeException(e);
        }

        return execResult;
    }

    public static ExecResult runCmd(String namespace, KubernetesClient client, String serviceRoleFullName,
            String hostname,
            String cmd) {
        List<String> commands = handlerCommand(cmd);

        // 调用公共的 executeCommand 方法
        return executeCommand(namespace, client, serviceRoleFullName, hostname, commands);
    }

    private static List<String> handlerCommand(String cmd) {
        // 对命令进行适当的处理，确保其在执行时不会被拆分
        // 对于带有特殊字符的命令，使用sh -c确保命令完整性
        return Arrays.asList("sh", "-c", cmd);
    }

    public static ExecResult runCmd(String namespace, KubernetesClient client, String serviceRoleFullName,
            String hostname,
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
        return executeCommand(namespace, client, serviceRoleFullName, hostname, commands);
    }

    public static ExecResult exec(ClusterServiceRoleInstanceEntity roleInstanceEntity, String kubeConfig,
            ExecuteCmdCommand cmdCommand) {
        KubernetesClient kubeClient = KubeUtil.getKubeClientByConfig(kubeConfig);
        return runCmd(getKubernetesNamespace(roleInstanceEntity.getClusterId()),
                kubeClient,
                (roleInstanceEntity.getServiceName() + "-" + roleInstanceEntity.getServiceRoleName()).toLowerCase(),
                roleInstanceEntity.getHostname(),
                cmdCommand);
    }

    public static void runJob(String namespace, String name, KubernetesClient client, VolumeMountDTO[] volumeMounts,
            String serviceRoleFullName, String cmd, String hostname) throws Exception {
        // delete job
        log.debug("delete job if need ,job name: " + name);
        client.batch().v1().jobs()
                .inNamespace(namespace)
                .withName(name)
                .delete();

        long timeout = 300; // Timeout in seconds
        long startTime = System.currentTimeMillis();

        // 尝试删除已存在的同名 Job，循环等待 Job 和相关 Pod 被删除完成
        waitForDeleteJob(namespace, name, client, timeout, startTime);

        // 提交一个新的 Job
        submitJob(namespace, name, client, volumeMounts, serviceRoleFullName, cmd, hostname);

        long waitPodTimeout = 300; // Timeout in seconds
        long waitPodStartTime = System.currentTimeMillis();

        // 进入一个循环等待 Pod 从创建到运行的状态。如果 Pod 处于 Pending 状态，方法会继续等待，直到 Pod 变为 Running 状态。
        String podName;
        podName = waitForCreatePodOfJob(namespace, name, client, waitPodStartTime, waitPodTimeout);
        log.debug("Pod name: " + podName);

        CountDownLatch jobCompletionLatch = new CountDownLatch(1);

        // 使用 Watch 机制监控 Job 的状态变化，判断 Job 是否成功或失败，并相应地记录日志。
        AtomicBoolean isJobEndSuccess = new AtomicBoolean(false);
        Watcher<Job> watcher = new Watcher<>() {
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
        try (Watch ignored = client.batch().v1().jobs()
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
            long waitPodStartTime, long waitPodTimeout) {
        // 循环等待创建pod成功
        String podName;
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
                Pod pod = pods.getFirst();
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
            String serviceRoleFullName, String cmd, String hostname) {
        List<Volume> volumeList = Arrays.stream(volumeMounts).map(volumeMount -> {
            // 1. 定义 HostPathVolumeSource
            HostPathVolumeSource hostPathVolume = new HostPathVolumeSourceBuilder()
                    .withPath(volumeMount.getHostPath())
                    .build();

            return new VolumeBuilder()
                    .withName(volumeMount.getVolumeName())
                    .withHostPath(hostPathVolume) // 本地目录
                    .build();
        }).collect(Collectors.toList());

        List<VolumeMount> mountList = Arrays.stream(volumeMounts).map(volumeMountDTO -> new VolumeMountBuilder()
                .withName(volumeMountDTO.getVolumeName())
                .withMountPath(volumeMountDTO.getContainerPath())
                .build()).collect(Collectors.toList());

        Container container = new ContainerBuilder()
                .withName("init")
                .withImage(serviceRoleFullName)
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
    }

    /**
     * 获取可伸缩资源（StatefulSet）
     *
     * @param client          Kubernetes客户端
     * @param namespace       命名空间
     * @param statefulSetName StatefulSet名称
     * @return 可伸缩资源对象
     */
    public static ScalableResource<?> getScalableResource(KubernetesClient client,
            String namespace,
            String statefulSetName) {
        return client.apps().statefulSets()
                .inNamespace(namespace)
                .withName(statefulSetName);
    }

    /**
     * 统一伸缩 StatefulSet 方法（增加定时伸缩策略参数）
     *
     * @param kubeConfig      Kubernetes客户端
     * @param namespace       命名空间
     * @param statefulSetName StatefulSet名称
     * @param replicas        目标副本数
     * @param schedulePolicy  定时策略描述（新增参数）
     */
    public static void scaleStatefulSet(String kubeConfig,
            String namespace,
            String statefulSetName,
            int replicas,
            String schedulePolicy) {
        try (KubernetesClient client = KubeUtil.getKubeClientByConfig(kubeConfig)) {
            ScalableResource<?> resource = getScalableResource(client, namespace, statefulSetName);
            if (resource != null) {
                log.info("Scaling {} to {} replicas with policy: {}", statefulSetName, replicas, schedulePolicy);
                resource.scale(replicas);
            }
        }

    }

    /**
     * 读取容器日志（返回值改为ExecResult类型）
     *
     * @param namespace           命名空间
     * @param client              Kubernetes客户端
     * @param serviceRoleFullName 应用标签
     * @param hostname            节点主机名
     * @param tailLines           日志行数
     * @return ExecResult 包含执行状态和日志内容
     */
    public static ExecResult getContainerLog(String namespace,
            KubernetesClient client,
            String serviceRoleFullName,
            String hostname,
            int tailLines) {
        // long startTime = System.currentTimeMillis();
        // log.info("Getting container log - namespace: {}, serviceRoleFullName: {},
        // hostname: {},
        // tailLines: {}",
        // namespace, serviceRoleFullName, hostname, tailLines);

        ExecResult execResult = new ExecResult(); // 创建返回对象
        try {
            // 通过标签和节点名定位Pod
            List<Pod> pods = client.pods()
                    .inNamespace(namespace)
                    .withLabel("app", serviceRoleFullName)
                    .list()
                    .getItems();

            log.debug("Found {} pods with label 'app={}'", pods.size(), serviceRoleFullName);

            // 过滤出指定节点上的Pod
            Pod targetPod = null;
            for (Pod pod : pods) {
                if (pod.getSpec() != null &&
                        hostname.equals(pod.getSpec().getNodeName())) {
                    targetPod = pod;
                    break;
                }
            }

            if (targetPod == null) {
                String errorMsg = "No pod found for serviceRoleFullName: " + serviceRoleFullName + " on host: "
                        + hostname;
                log.warn(errorMsg);
                // 设置失败状态和错误信息
                execResult.setExecResult(false);
                execResult.setExecOut(errorMsg);
                return execResult;
            }

            String podName = targetPod.getMetadata().getName();
            // log.debug("Target pod found: {} on node {}", podName, hostname);

            // 获取日志内容
            int actualLines = tailLines >= 0 ? tailLines : Integer.MAX_VALUE;
            // log.debug("Fetching {} lines of log from pod: {}, container: {}",
            // actualLines, podName, serviceRoleFullName);

            String logContent = client.pods()
                    .inNamespace(namespace)
                    .withName(podName)
                    .inContainer(serviceRoleFullName)
                    .tailingLines(actualLines)
                    .getLog();

            log.info("Successfully got {} lines of log from pod: {}",
                    actualLines == Integer.MAX_VALUE ? "all" : actualLines, podName);
            execResult.setExecResult(true); // 标记执行成功
            execResult.setExecOut(logContent); // 日志内容存入execOut
            return execResult;
        } catch (Exception e) {
            String errorMsg = String.format(
                    "Get container log error - namespace: %s, serviceRoleFullName: %s, hostname: %s",
                    namespace, serviceRoleFullName, hostname);
            log.error(errorMsg, e);
            // 设置异常状态和错误详情
            execResult.setExecResult(false);
            execResult.setExecOut(errorMsg + ": " + e.getMessage());
            return execResult;
        }
    }

    /**
     * 直接在指定Pod中执行命令
     *
     * @param namespace Kubernetes命名空间
     * @param client    Kubernetes客户端
     * @param podName   Pod名称
     * @param commands  要执行的命令列表
     * @return 执行结果
     */
    public static ExecResult executeCommandInPod(String namespace, KubernetesClient client, String podName,
            List<String> commands) {
        ExecResult execResult = new ExecResult();

        try {
            // 检查Pod是否存在
            Pod pod = client.pods().inNamespace(namespace).withName(podName).get();
            if (pod == null) {
                log.error("Pod {} 不存在", podName);
                execResult.setExecResult(false);
                execResult.setExecErrOut("Pod不存在: " + podName);
                return execResult;
            }

            long startTime = System.currentTimeMillis();

            // 记录完整命令
            String fullCommand = String.join(" ", commands);
            log.info("在Pod [{}] 中执行命令: {}", podName, fullCommand);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

            // 执行命令
            try (ExecWatch exec = client.pods()
                    .inNamespace(namespace)
                    .withName(podName)
                    .writingOutput(outputStream)
                    .writingError(errorStream)
                    .exec(commands.toArray(new String[0]))) {

                int exitCode = exec.exitCode().get();
                String out = IoUtil.toStr(outputStream, Charset.defaultCharset());
                String error = IoUtil.toStr(errorStream, Charset.defaultCharset());

                log.debug("命令执行结果: exitCode={}, output={}, error={}", exitCode, out, error);

                if (exitCode != 0) {
                    execResult.setExecResult(false);
                    if (StrUtil.isBlank(error)) {
                        error = out;
                    }
                    if (StrUtil.isBlank(error)) {
                        error = "执行失败，退出码: " + exitCode;
                    }
                    execResult.setExecErrOut(error);
                    log.error("命令执行失败，退出码 {}: {}", exitCode, error);
                } else {
                    execResult.setExecResult(true);
                    execResult.setExecOut(out);
                    log.debug("命令执行成功: {}", out);
                }
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            log.debug("命令执行时间: {} 毫秒", duration);

        } catch (Exception e) {
            log.error("执行命令时发生错误: {}", e.getMessage(), e);
            execResult.setExecResult(false);
            execResult.setExecErrOut("执行命令错误: " + e.getMessage());
        }

        return execResult;
    }

    /**
     * 直接在指定Pod中执行命令字符串
     *
     * @param namespace Kubernetes命名空间
     * @param client    Kubernetes客户端
     * @param podName   Pod名称
     * @param cmd       要执行的命令字符串
     * @return 执行结果
     */
    public static ExecResult runCmdInPod(String namespace, KubernetesClient client, String podName, String cmd) {
        List<String> commands = handlerCommand(cmd);
        return executeCommandInPod(namespace, client, podName, commands);
    }

    public static String getKubernetesNamespace(Integer clusterId) {
        ClusterInfoMapper clusterInfoMapper = SpringUtil.getBean(ClusterInfoMapper.class);
        ClusterInfoEntity clusterInfoEntity = clusterInfoMapper.selectById(clusterId);
        return clusterInfoEntity.getNamespace();
    }

}
