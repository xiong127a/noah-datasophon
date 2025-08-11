/**
 * Step3 大数据服务选择相关类型定义
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 * 日期：2024-01-20
 */

export interface Service {
  /** 服务ID */
  id: number
  /** 服务名称 */
  serviceName: string
  /** 服务显示标签 */
  label: string
  /** 服务描述 */
  serviceDesc?: string
  /** 服务版本 */
  serviceVersion?: string
  /** 是否已安装 */
  installed: boolean
  /** 是否必需 */
  isRequired: boolean
  /** 服务类型 */
  serviceType?: string
}

export interface ServiceSelection {
  /** 选中的服务ID */
  serviceId: number
  /** 选中的服务名称 */
  serviceName: string
}

export interface Step3Data {
  /** 选中的服务ID列表 */
  serviceIds: number[]
  /** 选中的服务信息列表 */
  serviceNames: ServiceSelection[]
  /** 服务类型筛选 */
  serviceType?: string
}

export interface ServiceSelectionDialogProps {
  /** 对话框是否打开 */
  open: boolean
  /** 对话框打开状态改变回调 */
  onOpenChange: (open: boolean) => void
  /** 集群信息 */
  cluster: {
    id: number
    clusterName: string
    depType: string
    clusterCode: string
  } | null
  /** 集群类型 */
  clusterType?: string
  /** Step2传递的数据 */
  step2Data?: Record<string, unknown>
  /** 成功回调（进入下一步） */
  onSuccess?: (step3Data: Step3Data) => void
  /** Step3完成回调 */
  onComplete: (step3Data: Step3Data) => void
  /** 返回上一步回调 */
  onPrevious?: () => void
}

export interface ServiceListResponse {
  /** 响应码 */
  code: number
  /** 响应消息 */
  msg?: string
  /** 服务列表数据 */
  data: Service[]
}

/** 服务类型枚举 */
export enum ServiceType {
  /** 最小化服务 - 最少的必需服务组件 */
  MINIMAL = 'minimal',
  /** 自定义服务 - 用户自定义的服务组件 */
  CUSTOM = 'custom'
}

export interface ServiceTypeOption {
  /** 类型值 */
  value: ServiceType
  /** 类型显示标签 */
  label: string
  /** 类型描述 */
  description?: string
}

// 预定义的服务类型选项（只有核心和自定义两个选项）
export const SERVICE_TYPE_OPTIONS: ServiceTypeOption[] = [
  {
    value: ServiceType.MINIMAL,
    label: '最小化', 
    description: '最少的必需大数据服务组件'
  },
  {
    value: ServiceType.CUSTOM,
    label: '自定义',
    description: '用户自定义的大数据服务组件'
  }
]
