package com.datasophon.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serial;
import java.io.Serializable;

/**
 * Kubernetes资源统计数据传输对象
 * 用于Service层数据传输
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record K8sResourceStatsDTO(
        Integer namespaceCount,
        Integer deploymentCount,
        Integer podCount,
        Integer serviceCount,
        Integer configMapCount,
        Integer secretCount,
        Integer persistentVolumeCount,
        Integer persistentVolumeClaimCount,
        Integer storageClassCount,
        Integer ingressCount,
        Integer ingressClassCount,
        Integer daemonSetCount,
        Integer statefulSetCount,
        Integer replicaSetCount,
        Integer replicationControllerCount,
        Integer jobCount,
        Integer cronJobCount,
        Integer runningPodCount,
        Integer pendingPodCount,
        Integer failedPodCount) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 获取总Pod数
     */
    public Integer getTotalPodCount() {
        return (runningPodCount != null ? runningPodCount : 0) +
                (pendingPodCount != null ? pendingPodCount : 0) +
                (failedPodCount != null ? failedPodCount : 0);
    }

    /**
     * 获取Pod健康率
     */
    public Double getPodHealthRate() {
        int total = getTotalPodCount();
        if (total == 0) {
            return 100.0;
        }
        int running = runningPodCount != null ? runningPodCount : 0;
        return (double) running / total * 100;
    }

    /**
     * 检查是否有失败的Pod
     */
    public boolean hasFailedPods() {
        return failedPodCount != null && failedPodCount > 0;
    }
}