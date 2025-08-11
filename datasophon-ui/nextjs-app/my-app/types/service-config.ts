/**
 * 服务配置相关类型定义
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 * 日期：2024-01-20
 */

// 基础配置项类型
export interface ConfigItem {
  name: string
  value: any
  type: string
  label?: string
  description?: string
  required?: boolean
  hidden?: boolean
  defaultValue?: any
  options?: Array<{ label: string; value: any }>
  templateContent?: string
  minValue?: number
  maxValue?: number
  unit?: string
  placeholder?: string
}

// 配置组类型
export interface ConfigGroup {
  items: ConfigItem[]
  displayName: string
  templateContent?: string | null
  hasKubernetesConfig?: boolean
  kubernetesSubGroups?: Record<string, ConfigSubGroup>
}

// Kubernetes配置子组
export interface ConfigSubGroup {
  items: ConfigItem[]
  displayName: string
  templateContent?: string | null
  roleGroup?: string
}

// 服务配置模板
export interface ServiceTemplate {
  [serviceName: string]: ConfigItem[]
}

// 分组后的模板数据
export interface GroupedTemplateData {
  [serviceName: string]: Record<string, ConfigGroup>
}

// 服务配置选项API响应
export interface ServiceConfigOptionResponse {
  code: number
  message?: string
  data?: ConfigItem[]
}

// 保存配置API参数
export interface SaveConfigParams {
  clusterId: number
  serviceName: string
  serviceConfig: string // JSON字符串
}

// 保存配置API响应
export interface SaveConfigResponse {
  code: number
  message?: string
  name?: string
  msg?: string
}

// 生成命令API参数
export interface GenerateCommandParams {
  clusterId: number
  serviceNames: string[]
  commandType: string
}

// 生成命令API响应
export interface GenerateCommandResponse {
  code: number
  message?: string
  data?: {
    commandIds: string
  }
}

// 步骤6数据（来自前一步） - 导入自worker-role-assign
// export interface Step6Data - 此类型已在worker-role-assign.ts中定义

// 步骤7数据（传递给下一步）
export interface Step7Data {
  serviceConfigs: Record<string, any[]>
  commandIds?: string
  commandType: string
}

// 服务配置对话框属性
export interface ServiceConfigDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  cluster: {
    id: number
    clusterName: string
    depType?: string
  }
  clusterType?: string
  step6Data: any // 使用any暂时避免类型冲突，在组件中会正确导入Step6Data
  onComplete: (step7Data: Step7Data) => void
  onPrevious?: () => void
}

// 表单数据类型
export interface FormData {
  [key: string]: any
}

// 验证错误类型
export interface ValidationErrors {
  [serviceName: string]: {
    [groupName: string]: {
      [fieldName: string]: string
    }
  }
}

// 展开状态类型
export interface ExpandedKeys {
  [serviceName: string]: string[]
}

// Kubernetes标签页状态
export interface KubernetesTabState {
  [key: string]: string // key格式: serviceName_groupName
}

// 中文子组名映射
export const KUBERNETES_SUBGROUP_CHINESE_NAMES: Record<string, string> = {
  'persistentVolumeClaims': '持久卷声明',
  'resources': '资源规格', 
  'services': '服务暴露',
  'configMaps': '配置映射',
  'secrets': '密钥管理',
  'volumes': '存储卷',
  'nodeSelector': '节点选择',
  'affinity': '亲和性',
  'tolerations': '容忍度',
  'securityContext': '安全上下文'
}

// 配置类型枚举
export enum ConfigType {
  INPUT = 'input',
  SELECT = 'select',
  SWITCH = 'switch',
  BOOLEAN = 'boolean',
  NUMBER = 'number',
  TEXTAREA = 'textarea',
  PASSWORD = 'password',
  MULTIPLE = 'multiple'
}

// 命令类型枚举
export enum CommandType {
  INSTALL_SERVICE = 'INSTALL_SERVICE',
  START_SERVICE = 'START_SERVICE',
  STOP_SERVICE = 'STOP_SERVICE',
  RESTART_SERVICE = 'RESTART_SERVICE'
}
