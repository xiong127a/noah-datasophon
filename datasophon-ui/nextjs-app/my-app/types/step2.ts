/**
 * Step2 主机环境校验相关类型定义
 */

import { ManagementStatus } from './management-status'

// 主机检查项状态
export type CheckItemStatus = 'WAITING' | 'CHECKING' | 'SUCCESS' | 'FAILED' | 'SKIPPED'

// 主机状态
export type HostStatus = 'SUCCESS' | 'FAILED' | 'CHECKING' | 'WAITING' | 'SKIPPED' | 'MIXED'

// 检查项接口
export interface CheckItem {
  id: string
  name: string
  status: CheckItemStatus
  result?: string
  errorMsg?: string
  createTime?: string
  updateTime?: string
}

// 主机接口
export interface Host {
  id?: number
  ip: string
  hostname?: string
  os?: string
  cpuCore?: number
  totalMem?: number
  totalDisk?: number
  usedMem?: number
  usedDisk?: number
  cpuArchitecture?: string
  managementStatus?: ManagementStatus // 主机管理状态
  status?: HostStatus
  statusStr?: string
  checkItems?: CheckItem[]
  CheckResult?: {
    code: string
    msg?: string
  }
  note?: string
  // K8s节点扩展字段 (从k8sNodeInfo JSON中解析)
  roles?: string
  version?: string
  age?: string
  status?: string // K8s节点状态
}

// 队列状态接口
export interface QueueStatus {
  queueSize: number
  runningTasks: number
  processorThreadAlive: boolean
}

// Step1传递的数据接口
export interface Step1Data {
  hosts?: string
  sshUser?: string
  sshPort?: string
  sshPassword?: string
  kubeConfigContent?: string
  namespace?: string
  namespaces?: string[]
  isCreatingNewNamespace?: boolean
  customNamespace?: string
  clusterVersion?: string
}

// 分页接口
export interface Pagination {
  current: number
  pageSize: number
  total: number
  showSizeChanger?: boolean
  pageSizeOptions?: string[]
  showTotal?: (total: number) => string
}

// Step2 props接口
export interface ClusterStep2DialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  cluster: {
    id: number
    clusterName: string
    depType: string
    clusterCode: string
  } | null
  step1Data: Step1Data
  onSuccess?: () => void
  onPrevious?: () => void  // 新增：上一步回调
}

// API响应接口
export interface HostListResponse {
  code: number
  data: {
    hosts?: Host[]
    filterOptions?: {
      statuses?: string[]
      roles?: string[]
    }
    totalCount?: number
  }
  total?: number
  queueStatus?: QueueStatus
  msg?: string
}

// 主机校验完成响应接口
export interface HostCheckCompletedResponse {
  code: number
  hostCheckCompleted: boolean
  data?: string
  msg?: string
}