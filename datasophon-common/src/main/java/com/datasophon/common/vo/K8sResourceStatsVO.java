/**
 * Kubernetes资源统计VO
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 * 日期：@date
 */
package com.datasophon.common.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class K8sResourceStatsVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // Pods统计
    private Integer podCount;
    private Integer runningPodCount;
    private Integer pendingPodCount;
    private Integer failedPodCount;
    private Integer succeededPodCount;
    
    // Services统计  
    private Integer serviceCount;
    private Integer clusterIpServiceCount;
    private Integer nodePortServiceCount;
    private Integer loadBalancerServiceCount;
    
    // Deployments统计
    private Integer deploymentCount;
    private Integer availableDeploymentCount;
    private Integer unavailableDeploymentCount;
    
    // ConfigMaps统计
    private Integer configMapCount;
    
    // Secrets统计
    private Integer secretCount;
    
    // StatefulSets统计
    private Integer statefulSetCount;
    private Integer readyStatefulSetCount;
    
    // DaemonSets统计
    private Integer daemonSetCount;
    private Integer readyDaemonSetCount;
    
    // Jobs统计
    private Integer jobCount;
    private Integer completedJobCount;
    private Integer activeJobCount;
    private Integer failedJobCount;
    
    // CronJobs统计
    private Integer cronJobCount;
    private Integer activeCronJobCount;
    private Integer suspendedCronJobCount;
    
    // PersistentVolumes统计
    private Integer persistentVolumeCount;
    private Integer boundPvCount;
    private Integer availablePvCount;
    
    // PersistentVolumeClaims统计
    private Integer persistentVolumeClaimCount;
    private Integer boundPvcCount;
    private Integer pendingPvcCount;
    
    // StorageClasses统计
    private Integer storageClassCount;
    
    // Ingresses统计
    private Integer ingressCount;
    
    // IngressClasses统计
    private Integer ingressClassCount;
    
    // ReplicaSets统计
    private Integer replicaSetCount;
    private Integer readyReplicaSetCount;
}