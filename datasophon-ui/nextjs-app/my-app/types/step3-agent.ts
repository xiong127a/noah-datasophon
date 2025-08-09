/**
 * Step3 主机Agent分发相关类型定义
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 * 日期：2024-01-20
 */

export interface HostInfo {
  /** 主机ID */
  id: number
  /** 主机名 */
  hostname: string
  /** IP地址 */
  ip: string
  /** CPU核数 */
  cpuCore?: number
  /** 内存大小(GB) */
  memory?: number
  /** 磁盘大小(GB) */
  disk?: number
  /** SSH端口 */
  sshPort?: number
  /** SSH用户 */
  sshUser?: string
  /** 管理状态 */
  managementStatus?: string
  /** Agent状态 */
  agentStatus?: 'NOT_INSTALLED' | 'INSTALLING' | 'INSTALLED' | 'FAILED'
  /** 分发进度 */
  progress?: number
  /** 错误信息 */
  errorMessage?: string
}

export interface AgentDistributionTask {
  /** 任务ID */
  taskId: string
  /** 主机ID */
  hostId: number
  /** 主机IP */
  hostIp: string
  /** 任务状态 */
  status: 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED'
  /** 进度 */
  progress: number
  /** 开始时间 */
  startTime?: string
  /** 结束时间 */
  endTime?: string
  /** 错误信息 */
  errorMessage?: string
  /** 详细日志 */
  logs?: string[]
}

export interface Step3AgentData {
  /** 分发的主机列表 */
  hosts: HostInfo[]
  /** 分发任务列表 */
  tasks: AgentDistributionTask[]
  /** 总体状态 */
  overallStatus: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED'
  /** 成功数量 */
  successCount: number
  /** 失败数量 */
  failedCount: number
}

export interface ClusterStep3AgentDialogProps {
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
  /** Step3完成回调 */
  onComplete: (step3Data: Step3AgentData) => void
  /** 返回上一步回调 */
  onPrevious?: () => void
}

export interface AgentDistributionResponse {
  /** 响应码 */
  code: number
  /** 响应消息 */
  msg?: string
  /** 分发结果数据 */
  data: {
    taskId: string
    hosts: HostInfo[]
  }
}

export interface AgentTaskStatusResponse {
  /** 响应码 */
  code: number
  /** 响应消息 */
  msg?: string
  /** 任务状态数据 */
  data: AgentDistributionTask[]
}
