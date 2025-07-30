// 机架相关类型定义
export interface Rack {
  id: number
  rack: string
  clusterId: number
}

export interface RackListResponse {
  code: number
  data: Rack[]
  message?: string
}

export interface AddRackRequest {
  rack: string
  clusterId: number
}

export interface DeleteRackRequest {
  rackId: number
}