export interface OperationLog {
  id: number
  url: string
  ip: string
  operationModule: string
  operationType: string
  param: string
  clusterId: number
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