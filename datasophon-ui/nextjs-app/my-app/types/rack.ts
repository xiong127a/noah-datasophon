// 机架相关类型定义
export interface Rack {
  id: string // 修复：20位long精度问题
  rack: string
  clusterId: string // 修复：20位long精度问题
}

export interface RackListResponse {
  code: number
  data: Rack[]
  message?: string
}

export interface AddRackRequest {
  rack: string
  clusterId: string // 修复：20位long精度问题
}