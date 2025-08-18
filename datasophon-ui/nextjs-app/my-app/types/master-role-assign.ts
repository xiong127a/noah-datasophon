/**
 * Step5 - 分配服务Master角色相关类型定义
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 */

export interface ServiceRole {
  /** 服务名称 */
  serviceName: string
  /** 服务角色名称 */
  serviceRoleName: string
  /** 基数限制：1表示单选，大于1表示多选 */
  cardinality: string
  /** 服务角色类型：master/worker/client */
  serviceRoleType: string
  /** 已分配的主机列表 */
  hosts?: string[]
}

export interface HostMapping {
  /** 服务角色名称 */
  serviceRole: string
  /** 分配的主机列表 */
  hosts: string[]
}

export interface Step5Data {
  /** 服务角色到主机的映射 */
  roleMappings: HostMapping[]
  /** 可用主机列表 */
  availableHosts: string[]
  /** 服务角色列表 */
  serviceRoles: ServiceRole[]
}

export interface Step5FormData {
  [key: string]: string | string[]
}

// 表单项类型定义
export interface FormItem {
  /** 表单项标签 */
  label: string
  /** 表单项名称 */
  name: string
  /** 当前值 */
  value: string | string[]
  /** 默认值 */
  defaultValue: string | string[]
  /** 可选值列表 */
  selectValue: string[] | any[]
  /** 控件类型：select/multipleSelect */
  type: 'select' | 'multipleSelect'
  /** 是否隐藏 */
  isHidden: boolean
  /** 是否必填 */
  required: boolean
}

// API 响应类型
export interface GetServiceRoleListResponse {
  code: number
  data: ServiceRole[]
  message: string
  success: boolean
}

export interface GetAllHostResponse {
  code: number
  data: Array<{
    hostname: string
    ip: string
    [key: string]: any
  }>
  message: string
  success: boolean
}

export interface SaveServiceRoleHostMappingResponse {
  code: number
  data: any
  message: string
  success: boolean
}

// API 请求参数类型
export interface GetServiceRoleListParams {
  clusterId: string
  serviceIds: string
  /** 1: Master角色, 2: Worker角色, 3: Client角色 */
  serviceRoleType: number
}

export interface GetAllHostParams {
  clusterId: string
}
