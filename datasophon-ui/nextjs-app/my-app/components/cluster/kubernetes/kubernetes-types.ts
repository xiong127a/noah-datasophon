/**
 * Kubernetes集群相关类型定义
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 * 日期：2024-01-20
 */

// Kubernetes Step1 数据接口
export interface KubernetesStep1Data {
  // Kubernetes专用配置
  kubeConfigContent: string
  namespace: string
  namespaces: string[]
  isCreatingNewNamespace: boolean
  customNamespace: string
  clusterVersion: string
}

// Kubernetes Step2 数据接口
export interface KubernetesStep2Data {
  step1Data: KubernetesStep1Data
  selectedHosts: KubernetesHost[]
  clusterType: 'Kubernetes'
  timestamp: string
}

// Kubernetes主机信息接口
export interface KubernetesHost {
  ip: string
  hostname: string
  status: 'Ready' | 'NotReady' | 'Unknown'
  roles: string  // 例如: "control-plane,etcd" 或 "worker" 或 "<none>"
  age: string    // 例如: "15d"
  version: string // 例如: "v1.28.2"
  cpuCore?: number
  totalMem?: number
  totalDisk?: number
  cpuArchitecture?: string
  managementStatus?: import('@/types').ManagementStatus
}

// Kubernetes集群基础信息接口
export interface KubernetesClusterInfo {
  id: number
  clusterName: string
  depType: 'Kubernetes'
  clusterCode: string
}

// Kubernetes Step1 对话框Props
export interface KubernetesStep1DialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  cluster: KubernetesClusterInfo | null
  onSuccess?: () => void
  onStep1Complete?: (step1Data: KubernetesStep1Data) => void
}

// Kubernetes Step2 对话框Props
export interface KubernetesStep2DialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  cluster: KubernetesClusterInfo | null
  step1Data: KubernetesStep1Data
  onSuccess?: (data?: Record<string, unknown>) => void
  onPrevious?: () => void
}

// Kubernetes配置验证结果
export interface KubernetesConfigValidation {
  valid: boolean
  message: string
  clusterVersion?: string
  serverUrl?: string
  namespaces?: string[]
}

// Kubernetes命名空间信息
export interface KubernetesNamespace {
  name: string
  status: 'Active' | 'Terminating'
  age: string
  labels?: Record<string, string>
  annotations?: Record<string, string>
}

// Kubernetes节点详细信息
export interface KubernetesNodeDetail extends KubernetesHost {
  // 资源信息
  capacity: {
    cpu: string
    memory: string
    storage: string
    pods: string
  }
  allocatable: {
    cpu: string
    memory: string
    storage: string
    pods: string
  }
  
  // 系统信息
  nodeInfo: {
    machineID: string
    systemUUID: string
    bootID: string
    kernelVersion: string
    osImage: string
    containerRuntimeVersion: string
    kubeletVersion: string
    kubeProxyVersion: string
    operatingSystem: string
    architecture: string
  }
  
  // 条件状态
  conditions: Array<{
    type: string
    status: 'True' | 'False' | 'Unknown'
    lastTransitionTime: string
    reason: string
    message: string
  }>
  
  // 污点信息
  taints?: Array<{
    key: string
    value: string
    effect: 'NoSchedule' | 'PreferNoSchedule' | 'NoExecute'
  }>
  
  // 标签
  labels: Record<string, string>
  
  // 注解
  annotations: Record<string, string>
}

// Kubernetes集群健康检查结果
export interface KubernetesHealthCheck {
  overall: 'Healthy' | 'Warning' | 'Critical'
  components: Array<{
    name: string
    status: 'Healthy' | 'Warning' | 'Critical'
    message?: string
  }>
  nodes: Array<{
    name: string
    status: 'Ready' | 'NotReady'
    message?: string
  }>
  pods: {
    total: number
    running: number
    pending: number
    failed: number
  }
}

// Kubernetes资源使用情况
export interface KubernetesResourceUsage {
  cpu: {
    total: string
    used: string
    percentage: number
  }
  memory: {
    total: string
    used: string
    percentage: number
  }
  storage: {
    total: string
    used: string
    percentage: number
  }
  pods: {
    total: number
    used: number
    percentage: number
  }
}

// Kubernetes服务发现配置
export interface KubernetesServiceDiscovery {
  enableAutoDiscovery: boolean
  discoveryNamespaces: string[]
  labelSelectors: Record<string, string>
  annotationSelectors: Record<string, string>
  excludeSystemNamespaces: boolean
}

// Kubernetes存储配置
export interface KubernetesStorageConfig {
  storageClass: string
  storageClasses: Array<{
    name: string
    provisioner: string
    allowVolumeExpansion: boolean
    volumeBindingMode: string
    reclaimPolicy: string
  }>
  defaultStorageClass?: string
}

// Kubernetes网络配置
export interface KubernetesNetworkConfig {
  clusterCIDR: string
  serviceCIDR: string
  podCIDR?: string
  cniPlugin: string
  cniVersion: string
  networkPolicies: boolean
}

// Kubernetes安全配置
export interface KubernetesSecurityConfig {
  rbacEnabled: boolean
  podSecurityPolicy: boolean
  networkPolicies: boolean
  serviceAccountTokens: boolean
  secretEncryption: boolean
}

// Kubernetes部署配置
export interface KubernetesDeploymentConfig {
  namespace: string
  resourceQuotas: {
    cpu: string
    memory: string
    storage: string
    pods: number
  }
  nodeSelector?: Record<string, string>
  tolerations?: Array<{
    key: string
    operator: 'Equal' | 'Exists'
    value?: string
    effect: 'NoSchedule' | 'PreferNoSchedule' | 'NoExecute'
  }>
  affinity?: {
    nodeAffinity?: unknown
    podAffinity?: unknown
    podAntiAffinity?: unknown
  }
}

// Kubernetes错误类型
export enum KubernetesErrorType {
  CONFIG_INVALID = 'CONFIG_INVALID',
  CONNECTION_FAILED = 'CONNECTION_FAILED', 
  UNAUTHORIZED = 'UNAUTHORIZED',
  NAMESPACE_NOT_FOUND = 'NAMESPACE_NOT_FOUND',
  RESOURCE_NOT_FOUND = 'RESOURCE_NOT_FOUND',
  API_ERROR = 'API_ERROR',
  TIMEOUT = 'TIMEOUT'
}

// Kubernetes错误接口
export interface KubernetesError {
  type: KubernetesErrorType
  message: string
  details?: string
  code?: number
  timestamp: string
}

// 导出所有类型的联合类型
export type KubernetesTypes = 
  | KubernetesStep1Data
  | KubernetesStep2Data
  | KubernetesHost
  | KubernetesClusterInfo
  | KubernetesConfigValidation
  | KubernetesNamespace
  | KubernetesNodeDetail
  | KubernetesHealthCheck
  | KubernetesResourceUsage
  | KubernetesServiceDiscovery
  | KubernetesStorageConfig
  | KubernetesNetworkConfig
  | KubernetesSecurityConfig
  | KubernetesDeploymentConfig
  | KubernetesError
