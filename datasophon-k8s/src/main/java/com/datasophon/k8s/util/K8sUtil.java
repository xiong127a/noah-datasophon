package com.datasophon.k8s.util;

import akka.protobuf.ByteString;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.stream.StreamUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.command.ExecuteCmdCommand;
import com.datasophon.common.model.VolumeMountDTO;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.IOUtils;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.k8s.constants.Constant;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.Execable;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import java.io.*;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class K8sUtil {
    /**
     * 封装的方法，用于在指定 Pod 容器中执行命令。
     *
     * @param client     KubernetesClient 实例
     * @param namespace  Pod 所在的命名空间
     * @param deployment 指定的 Deployment 名称
     * @param hostname   Pod 所在的 hostname
     * @param command    需要执行的命令
     * @param logger     日志记录实例
     */
    public static String executeCommandInPod(KubernetesClient client, String namespace, String deployment, String hostname, String command, Logger logger) {
        try {
            List<Pod> pods = client.pods().inNamespace(namespace).withLabel("app", deployment).list().getItems();
            Pod targetPod = null;

            for (Pod pod : pods) {
                if (pod.getStatus().getHostIP().equals(hostname)) {
                    targetPod = pod;
                    break;
                }
            }

            if (targetPod == null) {
                throw new RuntimeException("Pod with hostname " + hostname + " not found in namespace " + namespace + " for deployment " + deployment);
            }

            String podName = targetPod.getMetadata().getName();
            logger.info("Executing command in Pod: " + podName + " on host: " + hostname);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayOutputStream err = new ByteArrayOutputStream();

            client.pods().inNamespace(namespace).withName(podName)
                    .writingOutput(out)
                    .writingError(err)
                    .usingListener(new SimpleListener())
                    .exec("sh", "-c", command);

            return out.toString();

        } catch (Exception e) {
            logger.error("Command execution failed", e);
            return "Command execution failed: " + e.getMessage();
        }
    }

    public static ExecResult runCmd(String namespace, KubernetesClient client, String image, Logger logger, String hostname, String cmd) {
        List<Pod> pods = client.pods().inNamespace(Constant.K8S_NAMESPACE).withLabel("app", image).list().getItems();
        ExecResult execResult = new ExecResult();
        List<String> hostList = pods.stream().map(pod -> pod.getSpec().getNodeName()).collect(Collectors.toList());
        if (CollUtil.isEmpty(pods) || !hostList.contains(hostname)) {
            logger.info("host {} pods {} is null", hostname, image);
            execResult.setExecResult(false);
            return execResult;
        }
        try {
            for (Pod pod : pods) {
                String nodeName = pod.getSpec().getNodeName();
                if (nodeName != null && nodeName.equals(hostname)) {

                    String podName = pod.getMetadata().getName();

                    long startTime = System.currentTimeMillis(); // Start timing
                    logger.info("Command is " + cmd);

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();


                    try (ExecWatch exec = client.pods()
                            .inNamespace(namespace)
                            .withName(podName)
                            .writingOutput(outputStream)
                            .writingError(System.err)
                            .exec("sh", "-c", cmd)
                    ) {
                        int exitCode = exec.exitCode().get();
                        String out = IoUtil.toStr(outputStream, Charset.defaultCharset());

                        if (exitCode != 0) {
                            execResult.setExecResult(false);
                            execResult.setExecErrOut(out);
                            logger.error("exec result: {}", out);
                        } else {
                            execResult.setExecResult(true);
                            execResult.setExecOut(out);
                            logger.info("exec result: {}", out);
                        }
                    }

                    long endTime = System.currentTimeMillis(); // End timing
                    long duration = endTime - startTime; // Calculate duration

                    logger.info("Command execution time: " + duration + " milliseconds");

                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return execResult;
    }


    public static ExecResult exec(ClusterServiceRoleInstanceEntity roleInstanceEntity, String kubeConfig, String commandLine) {
        KubernetesClient kubeClient = KubeUtil.getKubeClientByConfig(kubeConfig);
        return runCmd(Constants.DATASOPHON,
                kubeClient,
                (roleInstanceEntity.getServiceName() + "-" + roleInstanceEntity.getServiceRoleName()).toLowerCase(),
                log,
                roleInstanceEntity.getHostname(),
                commandLine
        );
    }

    public static void runJob(String namespace, String name, KubernetesClient client, VolumeMountDTO[] volumeMounts, String image, String cmd, Logger logger, String hostname) throws Exception {
        // delete job
        logger.info("delete job if need ,job name: " + name);
        List<StatusDetails> statusDetailsList = client.batch().v1().jobs()
                .inNamespace(namespace)
                .withName(name)
                .delete();

        long timeout = 300; // Timeout in seconds
        long startTime = System.currentTimeMillis();

        // 尝试删除已存在的同名 Job，循环等待 Job 和相关 Pod 被删除完成
        waitForDeleteJob(namespace, name, client, timeout, startTime, logger);

        // 提交一个新的 Job
        submitJob(namespace, name, client, volumeMounts, image, cmd, hostname);


        long waitPodTimeout = 300; // Timeout in seconds
        long waitPodStartTime = System.currentTimeMillis();

        // 进入一个循环等待 Pod 从创建到运行的状态。如果 Pod 处于 Pending 状态，方法会继续等待，直到 Pod 变为 Running 状态。
        String podName = "";
        podName = waitForCreatePodOfJob(namespace, name, client, logger, podName, waitPodStartTime, waitPodTimeout);
        logger.info("Pod name: " + podName);

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
                logger.info("Watcher closed");
                if (cause != null) {
                    logger.error(cause.getMessage(), cause);
                }
            }


        };

        Watch watch = client.batch().v1().jobs()
                .inNamespace(namespace)
                .withName(name)
                .watch(watcher);

        // 使用 LogWatch 输出 Pod 的运行日志，直到 Job 完成
        try (LogWatch logWatch = client.pods()
                .inNamespace(namespace)
                .withName(podName)
                .watchLog()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(logWatch.getOutput()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("p> " + line);  // You can replace this with your desired logging mechanism
                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                throw new IOException();
            } finally {
                logWatch.close();
            }

            // Wait for the job to complete
            logger.info("Waiting  for job to complete...");
            jobCompletionLatch.await();

        } catch (InterruptedException | IOException e) {
            logger.error(e.getMessage(), e);
            throw new InterruptedException();
        } finally {
            watch.close();
        }

        boolean flag = isJobEndSuccess.get();
        logger.info("Job completed with success status: " + flag);
        if (!flag) {
            throw new RuntimeException("Job failed.");
        }
    }

    private static String waitForCreatePodOfJob(String namespace, String jobName, KubernetesClient client, Logger logger, String podName, long waitPodStartTime, long waitPodTimeout) {
        // 循环等待创建pod成功
        while (true) {
            // 需要考虑，有可能pod还没创建出来
            //  正在创建也不行 {"kind":"Status","apiVersion":"v1","metadata":{},"status":"Failure","message":"container \"init\" in pod \"init-flinkdir-hdfs-6c9r5\" is waiting to start: ContainerCreating","reason":"BadRequest","code":400}
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
                    logger.info("Pod {} is pending, waiting for it to be running...", podName);
                } else if (phase.equals("Running")) {
                    logger.info("Pod {} is running.", podName);
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

    private static void waitForDeleteJob(String namespace, String jobName, KubernetesClient client, long timeout, long startTime, Logger logger) {


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
                logger.info("Job {} has been deleted.", jobName);
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

    private static void submitJob(String namespace, String name, KubernetesClient client, VolumeMountDTO[] volumeMounts, String image, String cmd, String hostname) {
        List<Volume> volumeList = Arrays.stream(volumeMounts).map(new Function<VolumeMountDTO, Volume>() {
            @Override
            public Volume apply(VolumeMountDTO volumeMount) {
                // 1. 定义 HostPathVolumeSource
                HostPathVolumeSource hostPathVolume = new HostPathVolumeSourceBuilder()
                        .withPath(volumeMount.getHostPath())
                        .build();

                return new VolumeBuilder()
                        .withName(volumeMount.getVolumeName())
                        .withHostPath(hostPathVolume) //本地目录
                        .build();
            }
        }).collect(Collectors.toList());

        List<VolumeMount> mountList = Arrays.stream(volumeMounts).map(new Function<VolumeMountDTO, VolumeMount>() {
            @Override
            public VolumeMount apply(VolumeMountDTO volumeMountDTO) {
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
    }

    static class SimpleListener implements ExecListener {

        @Override
        public void onOpen() {
            log.info("Connection opened");
        }

        @Override
        public void onFailure(Throwable t, Response response) {
            log.error("Exec command failed: " + t.getMessage());
        }

        @Override
        public void onClose(int code, String reason) {
            log.info("Connection closed with exit code: " + code + ", reason: " + reason);
        }


    }

}
