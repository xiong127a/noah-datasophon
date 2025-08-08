/**
 * 集群部署类型枚举
 * 与后端 ClusterType 枚举保持一致
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-31
 */

/**
 * 集群部署类型枚举
 */
export enum ClusterType {
  /** 物理/虚拟机集群模式 */
  PVM = 'PVM',
  /** Kubernetes集群模式 */
  KUBERNETES = 'Kubernetes'
}

/**
 * 集群类型描述映射
 */
export const CLUSTER_TYPE_LABELS = {
  [ClusterType.PVM]: '裸金属/虚拟机',
  [ClusterType.KUBERNETES]: 'Kubernetes'
} as const

/**
 * 集群类型图标映射
 */
export const CLUSTER_TYPE_ICONS = {
  [ClusterType.PVM]: '/images/cluster/linux-tux.svg',
  [ClusterType.KUBERNETES]: '/images/cluster/kubernetes-logo.svg'
} as const

/**
 * 集群类型颜色映射
 */
export const CLUSTER_TYPE_COLORS = {
  [ClusterType.PVM]: {
    gradient: 'from-gray-500 via-gray-600 to-gray-700',
    bgGradient: 'from-gray-50/80 via-gray-100/40 to-gray-50/80',
    badgeColor: 'bg-gray-100 text-gray-700'
  },
  [ClusterType.KUBERNETES]: {
    gradient: 'from-blue-500 via-blue-600 to-cyan-500', 
    bgGradient: 'from-blue-50/80 via-blue-100/40 to-cyan-50/80',
    badgeColor: 'bg-blue-100 text-blue-700'
  }
} as const

/**
 * 集群类型工具函数
 */
export class ClusterTypeUtil {
  /**
   * 判断是否为PVM类型
   */
  static isPvm(clusterType: string | ClusterType): boolean {
    if (typeof clusterType === 'string') {
      return clusterType.toUpperCase() === 'PVM'
    }
    return clusterType === ClusterType.PVM
  }

  /**
   * 判断是否为Kubernetes类型
   */
  static isKubernetes(clusterType: string | ClusterType): boolean {
    if (typeof clusterType === 'string') {
      return clusterType.toLowerCase() === 'kubernetes' || clusterType.toLowerCase() === 'k8s'
    }
    return clusterType === ClusterType.KUBERNETES
  }

  /**
   * 从字符串转换为枚举
   */
  static fromString(clusterType: string): ClusterType {
    const normalized = clusterType.toLowerCase().trim()
    switch (normalized) {
      case 'pvm':
        return ClusterType.PVM
      case 'kubernetes':
      case 'k8s':
        return ClusterType.KUBERNETES
      default:
        throw new Error(`不支持的集群类型: ${clusterType}`)
    }
  }

  /**
   * 获取类型标签
   */
  static getLabel(clusterType: ClusterType): string {
    return CLUSTER_TYPE_LABELS[clusterType]
  }

  /**
   * 获取类型图标
   */
  static getIcon(clusterType: ClusterType): string {
    return CLUSTER_TYPE_ICONS[clusterType]
  }

  /**
   * 获取类型颜色配置
   */
  static getColors(clusterType: ClusterType) {
    return CLUSTER_TYPE_COLORS[clusterType]
  }
}


