package com.datasophon.common.constants;

/**
 * @author 任相鹏
 * @email 635887935@qq.com  
 * @date 2024-01-15
 * @description Kubernetes资源状态常量定义
 */
public final class K8sResourceConstants {

    private K8sResourceConstants() {
        // 防止实例化
    }

    /**
     * Pod状态常量
     */
    public static final class PodPhase {
        public static final String RUNNING = "Running";
        public static final String PENDING = "Pending";
        public static final String SUCCEEDED = "Succeeded";
        public static final String FAILED = "Failed";
        public static final String UNKNOWN = "Unknown";
    }

    /**
     * Service类型常量
     */
    public static final class ServiceType {
        public static final String CLUSTER_IP = "ClusterIP";
        public static final String NODE_PORT = "NodePort";
        public static final String LOAD_BALANCER = "LoadBalancer";
        public static final String EXTERNAL_NAME = "ExternalName";
    }

    /**
     * PersistentVolume状态常量
     */
    public static final class PersistentVolumePhase {
        public static final String AVAILABLE = "Available";
        public static final String BOUND = "Bound";
        public static final String RELEASED = "Released";
        public static final String FAILED = "Failed";
    }

    /**
     * PersistentVolumeClaim状态常量
     */
    public static final class PersistentVolumeClaimPhase {
        public static final String PENDING = "Pending";
        public static final String BOUND = "Bound";
        public static final String LOST = "Lost";
    }

    /**
     * Job状态常量
     */
    public static final class JobConditionType {
        public static final String COMPLETE = "Complete";
        public static final String FAILED = "Failed";
        public static final String SUSPENDED = "Suspended";
    }

    /**
     * Deployment状态常量
     */
    public static final class DeploymentConditionType {
        public static final String AVAILABLE = "Available";
        public static final String PROGRESSING = "Progressing";
        public static final String REPLICA_FAILURE = "ReplicaFailure";
    }

    /**
     * DaemonSet状态常量
     */
    public static final class DaemonSetConditionType {
        public static final String AVAILABLE = "Available";
    }

    /**
     * StatefulSet状态常量
     */
    public static final class StatefulSetConditionType {
        public static final String AVAILABLE = "Available";
    }

    /**
     * ReplicaSet状态常量
     */
    public static final class ReplicaSetConditionType {
        public static final String REPLICA_FAILURE = "ReplicaFailure";
    }

    /**
     * 常用标签和注解键常量
     */
    public static final class CommonLabels {
        public static final String APP = "app";
        public static final String NAME = "name";
        public static final String VERSION = "version";
        public static final String COMPONENT = "component";
        public static final String PART_OF = "app.kubernetes.io/part-of";
        public static final String MANAGED_BY = "app.kubernetes.io/managed-by";
        public static final String INSTANCE = "app.kubernetes.io/instance";
    }

    /**
     * 常用注解键常量
     */
    public static final class CommonAnnotations {
        public static final String KUBECTL_LAST_APPLIED_CONFIGURATION = "kubectl.kubernetes.io/last-applied-configuration";
        public static final String DEPLOYMENT_REVISION = "deployment.kubernetes.io/revision";
    }
}
