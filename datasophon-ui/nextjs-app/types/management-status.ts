/**
 * 主机管理状态枚举
 * 对应后端 ManagementStatus 枚举
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-31
 */

/**
 * 主机管理状态枚举值
 */
export enum ManagementStatus {
  /** 受管 - 主机已正式纳入集群管理 */
  MANAGED = 1,
  /** 未受管 - 主机已发现但未纳入管理 */
  UNMANAGED = 2,
  /** 配置中 - 主机正在进行配置，暂时不计入受管统计 */
  CONFIGURING = 3
}

/**
 * 主机管理状态描述映射
 */
export const MANAGEMENT_STATUS_LABELS = {
  [ManagementStatus.MANAGED]: '受管',
  [ManagementStatus.UNMANAGED]: '未受管',
  [ManagementStatus.CONFIGURING]: '配置中'
} as const

/**
 * 主机管理状态颜色映射（用于UI显示）
 */
export const MANAGEMENT_STATUS_COLORS = {
  [ManagementStatus.MANAGED]: 'rose',      // 受管 - 红色
  [ManagementStatus.UNMANAGED]: 'emerald', // 未受管 - 绿色
  [ManagementStatus.CONFIGURING]: 'amber'  // 配置中 - 黄色
} as const

/**
 * 管理状态工具函数
 */
export class ManagementStatusUtil {
  /**
   * 判断是否为受管状态
   */
  static isManaged(status: ManagementStatus): boolean {
    return status === ManagementStatus.MANAGED
  }

  /**
   * 判断是否为未受管状态
   */
  static isUnmanaged(status: ManagementStatus): boolean {
    return status === ManagementStatus.UNMANAGED
  }

  /**
   * 判断是否为配置中状态
   */
  static isConfiguring(status: ManagementStatus): boolean {
    return status === ManagementStatus.CONFIGURING
  }

  /**
   * 判断是否可以进行配置操作
   */
  static canConfigure(status: ManagementStatus): boolean {
    return status === ManagementStatus.UNMANAGED || status === ManagementStatus.CONFIGURING
  }

  /**
   * 获取状态标签
   */
  static getLabel(status: ManagementStatus): string {
    return MANAGEMENT_STATUS_LABELS[status] || '未知'
  }

  /**
   * 获取状态颜色
   */
  static getColor(status: ManagementStatus): string {
    return MANAGEMENT_STATUS_COLORS[status] || 'gray'
  }

  /**
   * 从数值转换为枚举
   */
  static fromValue(value: number): ManagementStatus {
    switch (value) {
      case 1: return ManagementStatus.MANAGED
      case 2: return ManagementStatus.UNMANAGED  
      case 3: return ManagementStatus.CONFIGURING
      default: return ManagementStatus.UNMANAGED
    }
  }
}

/**
 * 管理状态统计信息
 */
export interface ManagementStatusStats {
  total: number
  managed: number
  unmanaged: number
  configuring: number
}
