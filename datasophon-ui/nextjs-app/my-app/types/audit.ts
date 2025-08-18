export interface OperationLog {
  id: string // 修复：20位long精度问题
  url: string
  ip: string
  operationModule: string
  operationType: string
  param: string
  clusterId: string // 修复：20位long精度问题
  hostIds: string
  serviceName: string
  serviceRoleInstancesIds: string
  returnCode: number
  returnCodeMsg?: string
  returnMsg: string
  operateUser: string
  startTime: string
  endTime: string
}

export interface AuditLogFilters {
  operationModule?: string
  serviceName?: string
  operateUser?: string
}

export interface PageInfo {
  current: number
  size: number
  total: number
}

export interface AuditLogListRequest {
  current: number
  size: number
  param: AuditLogFilters
}

export interface AuditLogListResponse {
  records: OperationLog[]
  total: number
  size: number
  current: number
  pages: number
}