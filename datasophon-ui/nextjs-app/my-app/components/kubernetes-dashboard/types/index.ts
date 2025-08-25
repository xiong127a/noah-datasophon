/**
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 * @description Kubernetes Dashboard类型定义
 */

// 基础K8s资源类型
export interface KubernetesResource {
  apiVersion: string;
  kind: string;
  metadata: {
    name: string;
    namespace?: string;
    creationTimestamp: string;
    labels?: Record<string, string>;
    annotations?: Record<string, string>;
  };
}

// Pod相关类型
export interface PodStatus {
  phase: 'Pending' | 'Running' | 'Succeeded' | 'Failed' | 'Unknown';
  conditions?: Array<{
    type: string;
    status: string;
    lastTransitionTime: string;
    reason?: string;
    message?: string;
  }>;
  containerStatuses?: Array<{
    name: string;
    ready: boolean;
    restartCount: number;
          state?: Record<string, unknown>;
  }>;
}

export interface Pod extends KubernetesResource {
  kind: 'Pod';
  spec: {
    containers: Array<{
      name: string;
      image: string;
      ports?: Array<{ containerPort: number; protocol?: string }>;
      resources?: {
        requests?: Record<string, string>;
        limits?: Record<string, string>;
      };
    }>;
    nodeName?: string;
  };
  status?: PodStatus;
}

// Service相关类型
export interface Service extends KubernetesResource {
  kind: 'Service';
  spec: {
    type: 'ClusterIP' | 'NodePort' | 'LoadBalancer' | 'ExternalName';
    selector?: Record<string, string>;
    ports: Array<{
      name?: string;
      port: number;
      targetPort?: number | string;
      nodePort?: number;
      protocol?: string;
    }>;
    clusterIP?: string;
  };
  status?: {
    loadBalancer?: {
      ingress?: Array<{ ip?: string; hostname?: string }>;
    };
  };
}

// Deployment相关类型
export interface Deployment extends KubernetesResource {
  kind: 'Deployment';
  spec: {
    replicas: number;
    selector: {
      matchLabels: Record<string, string>;
    };
    template: {
      metadata: {
        labels: Record<string, string>;
      };
      spec: {
        containers: Array<{
          name: string;
          image: string;
          ports?: Array<{ containerPort: number }>;
        }>;
      };
    };
  };
  status?: {
    replicas?: number;
    readyReplicas?: number;
    availableReplicas?: number;
    unavailableReplicas?: number;
    updatedReplicas?: number;
  };
}

// ConfigMap类型
export interface ConfigMap extends KubernetesResource {
  kind: 'ConfigMap';
  data?: Record<string, string>;
  binaryData?: Record<string, string>;
}

// Secret类型
export interface Secret extends KubernetesResource {
  kind: 'Secret';
  type: string;
  data?: Record<string, string>;
}

// StatefulSet类型
export interface StatefulSet extends KubernetesResource {
  kind: 'StatefulSet';
  spec: {
    replicas: number;
    serviceName: string;
    selector: {
      matchLabels: Record<string, string>;
    };
    template: {
      metadata: {
        labels: Record<string, string>;
      };
      spec: {
        containers: Array<{
          name: string;
          image: string;
        }>;
      };
    };
  };
  status?: {
    replicas?: number;
    readyReplicas?: number;
    currentReplicas?: number;
    updatedReplicas?: number;
  };
}

// DaemonSet类型
export interface DaemonSet extends KubernetesResource {
  kind: 'DaemonSet';
  spec: {
    selector: {
      matchLabels: Record<string, string>;
    };
    template: {
      metadata: {
        labels: Record<string, string>;
      };
      spec: {
        containers: Array<{
          name: string;
          image: string;
        }>;
      };
    };
  };
  status?: {
    currentNumberScheduled?: number;
    desiredNumberScheduled?: number;
    numberReady?: number;
    numberAvailable?: number;
  };
}

// Job类型
export interface Job extends KubernetesResource {
  kind: 'Job';
  spec: {
    parallelism?: number;
    completions?: number;
    template: {
      spec: {
        containers: Array<{
          name: string;
          image: string;
        }>;
        restartPolicy: string;
      };
    };
  };
  status?: {
    active?: number;
    succeeded?: number;
    failed?: number;
    startTime?: string;
    completionTime?: string;
  };
}

// CronJob类型
export interface CronJob extends KubernetesResource {
  kind: 'CronJob';
  spec: {
    schedule: string;
    jobTemplate: {
      spec: Job['spec'];
    };
    suspend?: boolean;
  };
  status?: {
    lastScheduleTime?: string;
    active?: Array<{ name: string; namespace: string }>;
  };
}

// PersistentVolume类型
export interface PersistentVolume extends KubernetesResource {
  kind: 'PersistentVolume';
  spec: {
    capacity: {
      storage: string;
    };
    accessModes: string[];
    persistentVolumeReclaimPolicy?: string;
    storageClassName?: string;
  };
  status?: {
    phase?: 'Available' | 'Bound' | 'Released' | 'Failed';
  };
}

// PersistentVolumeClaim类型
export interface PersistentVolumeClaim extends KubernetesResource {
  kind: 'PersistentVolumeClaim';
  spec: {
    resources: {
      requests: {
        storage: string;
      };
    };
    accessModes: string[];
    storageClassName?: string;
  };
  status?: {
    phase?: 'Pending' | 'Bound' | 'Lost';
    capacity?: {
      storage: string;
    };
  };
}

// Ingress类型
export interface Ingress extends KubernetesResource {
  kind: 'Ingress';
  spec: {
    rules?: Array<{
      host?: string;
      http?: {
        paths: Array<{
          path?: string;
          pathType?: string;
          backend: {
            service?: {
              name: string;
              port: {
                number: number;
              };
            };
          };
        }>;
      };
    }>;
    tls?: Array<{
      hosts?: string[];
      secretName?: string;
    }>;
  };
  status?: {
    loadBalancer?: {
      ingress?: Array<{ ip?: string; hostname?: string }>;
    };
  };
}

// 通用响应类型
export interface KubernetesResponse<T = unknown> {
  code: number;
  message: string;
  data: T;
}

export interface KubernetesListResponse<T extends KubernetesResource> {
  items: T[];
  metadata: {
    resourceVersion: string;
    continue?: string;
  };
}

// 命名空间类型（用于内部组件）
export interface Namespace extends KubernetesResource {
  kind: 'Namespace';
  status?: {
    phase: 'Active' | 'Terminating';
  };
}

// K8s命名空间API响应类型已移动到 @/lib/kubernetes-api

// 仪表盘状态类型
export interface DashboardState {
  selectedNamespace: string;
  refreshInterval: number;
  lastRefresh: string;
  isLoading: boolean;
  error: string | null;
}

// 资源统计类型
export interface ResourceStats {
  total: number;
  running?: number;
  pending?: number;
  failed?: number;
  ready?: number;
}

// 事件类型
export interface Event extends KubernetesResource {
  kind: 'Event';
  involvedObject: {
    kind: string;
    name: string;
    namespace?: string;
  };
  reason: string;
  message: string;
  source: {
    component: string;
  };
  firstTimestamp: string;
  lastTimestamp: string;
  count: number;
  type: 'Normal' | 'Warning';
}
