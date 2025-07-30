export interface Tag {
  id: number
  nodeLabel: string
  clusterId: number
}

export interface CreateTagRequest {
  nodeLabel: string
  clusterId: number
}

export interface DeleteTagRequest {
  nodeLabelId: number
}

export interface AssignTagRequest {
  nodeLabelId: number
  clusterId: number
  hostIds: number[]
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