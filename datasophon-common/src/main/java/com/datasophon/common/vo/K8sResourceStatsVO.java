package com.datasophon.common.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serial;
import java.io.Serializable;

/**
 * Kubernetes资源统计视图对象
 * 用于Controller层响应前端
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record K8sResourceStatsVO(
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
        Integer failedPodCount,
        Integer totalPodCount, // 计算字段：总Pod数
        Double podHealthRate, // 计算字段：Pod健康率
        Boolean hasFailedPods, // 计算字段：是否有失败Pod
        String healthStatus, // 计算字段：健康状态文本
        String healthStatusColor // 计算字段：健康状态颜色
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 从DTO创建VO，包含计算字段
     */
    public static K8sResourceStatsVO from(Integer namespaceCount, Integer deploymentCount, Integer podCount,
            Integer serviceCount, Integer configMapCount, Integer secretCount,
            Integer persistentVolumeCount, Integer persistentVolumeClaimCount,
            Integer storageClassCount, Integer ingressCount, Integer ingressClassCount,
            Integer daemonSetCount, Integer statefulSetCount, Integer replicaSetCount,
            Integer replicationControllerCount, Integer jobCount, Integer cronJobCount,
            Integer runningPodCount, Integer pendingPodCount, Integer failedPodCount) {

        // 计算字段
        Integer totalPods = (runningPodCount != null ? runningPodCount : 0) +
                (pendingPodCount != null ? pendingPodCount : 0) +
                (failedPodCount != null ? failedPodCount : 0);

        Double healthRate = totalPods == 0 ? 100.0
                : (double) (runningPodCount != null ? runningPodCount : 0) / totalPods * 100;

        Boolean hasFailed = failedPodCount != null && failedPodCount > 0;

        String healthStatus = getHealthStatus(healthRate, hasFailed);
        String healthColor = getHealthStatusColor(healthRate, hasFailed);

        return new K8sResourceStatsVO(namespaceCount, deploymentCount, podCount, serviceCount,
                configMapCount, secretCount, persistentVolumeCount, persistentVolumeClaimCount,
                storageClassCount, ingressCount, ingressClassCount, daemonSetCount,
                statefulSetCount, replicaSetCount, replicationControllerCount, jobCount,
                cronJobCount, runningPodCount, pendingPodCount, failedPodCount,
                totalPods, healthRate, hasFailed, healthStatus, healthColor);
    }

    /**
     * 获取健康状态文本
     */
    private static String getHealthStatus(Double healthRate, Boolean hasFailed) {
        if (hasFailed) {
            return "异常";
        }
        if (healthRate >= 95.0) {
            return "健康";
        } else if (healthRate >= 80.0) {
            return "良好";
        } else {
            return "警告";
        }
    }

    /**
     * 获取健康状态颜色
     */
    private static String getHealthStatusColor(Double healthRate, Boolean hasFailed) {
        if (hasFailed) {
            return "red";
        }
        if (healthRate >= 95.0) {
            return "green";
        } else if (healthRate >= 80.0) {
            return "blue";
        } else {
            return "orange";
        }
    }
}