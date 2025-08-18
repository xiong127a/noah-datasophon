export interface Tag {
  id: string // 修复：20位long精度问题
  nodeLabel: string
  clusterId: string // 修复：20位long精度问题
}

export interface CreateTagRequest {
  nodeLabel: string
  clusterId: string // 修复：20位long精度问题
}

export interface DeleteTagRequest {
  nodeLabelId: string // 修复：20位long精度问题
}

export interface AssignTagRequest {
  nodeLabelId: string // 修复：20位long精度问题
  clusterId: string // 修复：20位long精度问题
  hostIds: string[] // 修复：20位long精度问题
}

export interface TagListResponse {
  code: number
  data: Tag[]
  message?: string
}

export interface TagOperationResponse {
  code: number
  message?: string
}