import type { HostInfo } from './step5'

export interface WorkerRoleAssignDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  cluster: { id: string; clusterName: string; depType?: string } | null
  clusterType: string
  step4Data: Step4Data | null
  onComplete: (step6Data: Step6Data) => void
  onPrevious?: () => void
}

export interface Step4Data {
  serviceIds: string[] // 修复：20位long精度问题
  serviceNames: { serviceId: string; serviceName: string }[] // 修复：20位long精度问题
  serviceType: string
}

export interface Step6Data {
  roleHostMappings: ServiceRoleHostMapping[]
  selectedRoles: string[]
  assignedHosts: HostRoleAssignment[]
  serviceNames?: { serviceName: string }[] // 可选：用于后续步骤的服务名称列表
}

export interface ServiceRoleHostMapping {
  serviceRole: string
  hosts: string[]
}

export interface HostRoleAssignment {
  hostname: string
  roles: string[]
}

export interface NonMasterRole {
  id: string // 修复：20位long精度问题
  serviceRoleName: string
  serviceName: string
  serviceRoleType: number
  hosts: string[]
}

export interface NonMasterRoleResponse {
  success: boolean
  data: NonMasterRole[]
  message?: string
}

export interface TableRowData {
  id: string // 修复：20位long精度问题
  hostname: string
  checkedList: string[]
  [key: string]: any // 动态添加角色字段
}

export interface TableColumn {
  title: string | ((text: any, row: any, index: number) => JSX.Element)
  key: string
  dataIndex?: string
  width?: number
  customRender?: (text: any, row: any, index: number) => JSX.Element
}

export interface SaveRoleHostMappingRequest {
  serviceRole: string
  hosts: string[]
}

export interface SaveRoleHostMappingResponse {
  success: boolean
  message?: string
}
