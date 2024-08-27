package com.datasophon.k8s.util;

import com.datasophon.common.model.VolumeMountDTO;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

public class K8sUtil {
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
        waitForDeleteJob(namespace,name, client, timeout, startTime, logger);

        // 提交一个新的 Job
        submitJob(namespace,name, client, volumeMounts, image, cmd, hostname);


        long waitPodTimeout = 300; // Timeout in seconds
        long waitPodStartTime = System.currentTimeMillis();

        // 进入一个循环等待 Pod 从创建到运行的状态。如果 Pod 处于 Pending 状态，方法会继续等待，直到 Pod 变为 Running 状态。
        String podName = "";
        podName = waitForCreatePodOfJob(namespace,name, client, logger, podName, waitPodStartTime, waitPodTimeout);
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
                    logger.info("p> "+line);  // You can replace this with your desired logging mechanism
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
}
