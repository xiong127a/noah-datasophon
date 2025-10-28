/**
 * 集群配置步骤常量定义
 * 基于原Vue2项目的步骤配置逻辑进行迁移
 */

import { ClusterType } from '@/types/cluster-type'

export interface ClusterStep {
  number: number
  title: string
  description: string
}

// 完整的步骤列表（对应原Vue2项目）
export const ALL_STEPS: ClusterStep[] = [
  { number: 1, title: '安装主机', description: '配置集群主机和连接信息' },
  { number: 2, title: '主机校验', description: '校验主机SSH连接和基本状态' },
  { number: 3, title: '环境检查', description: '检查CPU/内存/JDK/防火墙等环境配置' },
  { number: 4, title: '主机Agent分发', description: '分发和安装主机Agent' },
  { number: 5, title: '选择服务', description: '选择要安装的大数据服务' },
  { number: 6, title: '分配服务Master角色', description: '为服务分配Master节点' },
  { number: 7, title: '分配服务Worker与Client角色', description: '为服务分配Worker和Client节点' },
  { number: 8, title: '服务配置', description: '配置服务参数和资源分配' },
  { number: 9, title: '安装并启动服务', description: '安装并启动所有服务' }
]

// 步骤类型枚举
export enum StepsType {
  NORMAL = 'normal',           // 完整流程
  HOST_MANAGE = 'hostManage',  // 主机管理流程
  ADD_SERVICE = 'addService',  // 添加服务流程  
  SERVICE_EXAMPLE = 'service-example' // 服务示例流程
}

// 导出集群类型枚举
export { ClusterType }

/**
 * 根据步骤类型和部署类型获取对应的步骤列表
 * @param stepsType 步骤类型
 * @param depType 部署类型
 * @returns 过滤后的步骤列表
 */
export function getStepsByType(
  stepsType: StepsType = StepsType.NORMAL, 
  depType: ClusterType = ClusterType.PVM
): ClusterStep[] {
  let steps = [...ALL_STEPS]
  
  // 根据步骤类型过滤
  switch (stepsType) {
    case StepsType.HOST_MANAGE:
      // 只保留前4步（安装主机、主机校验、环境检查、Agent分发）
      steps = steps.slice(0, 4)
      break
    case StepsType.ADD_SERVICE:
      // 添加服务模式：从"选择服务"步骤开始（跳过前4个主机相关步骤）
      steps = steps.slice(4)  // 从index 4开始，即"选择服务"
      // 重新编号：第1步 = 选择服务，第2步 = 分配Master角色，依此类推
      steps = steps.map((step, index) => ({
        ...step,
        number: index + 1
      }))
      break
    case StepsType.SERVICE_EXAMPLE:
      // 从第5步开始
      steps = steps.slice(5)
      // 重新编号
      steps = steps.map((step, index) => ({
        ...step,
        number: index + 1
      }))
      break
    default:
      // 完整流程，保持原样
      break
  }
  
  // ✅ 不再根据部署类型过滤和重新编号
  // K8S和PVM使用相同的步骤编号，K8S在步骤3（环境检查）和步骤4（Agent分发）时显示"无需操作"即可
  // if (depType === ClusterType.KUBERNETES) {
  //   // Kubernetes模式：过滤掉'环境检查'和'主机Agent分发'步骤
  //   steps = steps.filter(step => 
  //     step.title !== '环境检查' && step.title !== '主机Agent分发'
  //   )
  //   // 重新编号
  //   steps = steps.map((step, index) => ({
  //     ...step,
  //     number: index + 1
  //   }))
  // }
  
  return steps
}

/**
 * 获取步骤标题列表（用于向后兼容）
 * @param stepsType 步骤类型
 * @param depType 部署类型
 * @returns 步骤标题数组
 */
export function getStepTitles(
  stepsType: StepsType = StepsType.NORMAL,
  clusterType: ClusterType = ClusterType.PVM
): string[] {
  return getStepsByType(stepsType, clusterType).map(step => step.title)
}

// 导出常用的步骤配置
export const STEPS_CONFIG = {
  // 主机管理流程（3步）
  HOST_MANAGE: getStepsByType(StepsType.HOST_MANAGE, ClusterType.PVM),
  HOST_MANAGE_K8S: getStepsByType(StepsType.HOST_MANAGE, ClusterType.KUBERNETES),
  
  // 完整流程（8步/7步）
  FULL_PVM: getStepsByType(StepsType.NORMAL, ClusterType.PVM),
  FULL_K8S: getStepsByType(StepsType.NORMAL, ClusterType.KUBERNETES),
  
  // 添加服务流程
  ADD_SERVICE: getStepsByType(StepsType.ADD_SERVICE, ClusterType.PVM),
  ADD_SERVICE_K8S: getStepsByType(StepsType.ADD_SERVICE, ClusterType.KUBERNETES),
  
  // 服务示例流程
  SERVICE_EXAMPLE: getStepsByType(StepsType.SERVICE_EXAMPLE, ClusterType.PVM),
  SERVICE_EXAMPLE_K8S: getStepsByType(StepsType.SERVICE_EXAMPLE, ClusterType.KUBERNETES)
}