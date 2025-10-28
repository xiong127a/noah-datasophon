"use client"

import React, { useState, useEffect, useCallback, useMemo } from 'react'
import { 
  Loader2, AlertCircle
} from 'lucide-react'
import { Button } from "@/components/ui/button"

import { Badge } from '@/components/ui/badge'

import { toast } from 'sonner'
import ClusterWizardLayout from './cluster-wizard-layout'
import ClusterWizardActionBar, { type ActionButton, type StatusInfo, type StatusBadge } from './cluster-wizard-action-bar'
import SuperHostSelector, { type HostInfo } from './super-host-selector'
import ServiceIcon from '@/components/ui/service-icon'
import { ClusterTypeUtil } from '@/types'
import { clusterApiV1 } from '@/lib/api-utils-v1'

import type {
  WorkerRoleAssignDialogProps,
  Step6Data,
  NonMasterRole,

  ServiceRoleHostMapping
} from '@/types/worker-role-assign'

/**
 * 从服务角色名中提取服务名
 * 例如：HDFS_DataNode → HDFS，YARN_NodeManager → YARN
 */
const extractServiceName = (serviceRoleName: string): string => {
  if (!serviceRoleName) return ''
  
  // 服务角色名格式通常是：ServiceName_RoleName
  const parts = serviceRoleName.split('_')
  return parts[0] || serviceRoleName
}

/**
 * 集群步骤6：分配服务Worker与Client角色对话框
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 */

const WorkerRoleAssignDialog: React.FC<WorkerRoleAssignDialogProps> = ({
  open,
  onOpenChange,
  cluster,
  clusterType,
  step4Data,
  onComplete,
  onPrevious
}) => {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [roles, setRoles] = useState<NonMasterRole[]>([])
  const [availableHosts, setAvailableHosts] = useState<HostInfo[]>([])
  const [formData, setFormData] = useState<Record<string, string[]>>({})
  const [errors, setErrors] = useState<Record<string, string>>({})

  // 计算当前步骤编号
  const isK8s = ClusterTypeUtil.isKubernetes(clusterType)
  const currentStepNumber = 7  // ✅ 分配服务Worker与Client角色 = 第7步

  // 表单项生成
  const formItems = useMemo(() => {
    // 调试：检查重复数据
    const roleNames = roles.map(role => role.serviceRoleName)
    const uniqueRoleNames = [...new Set(roleNames)]
    if (roleNames.length !== uniqueRoleNames.length) {
      console.warn('⚠️ Worker角色数据重复:', roleNames)
      console.warn('去重后:', uniqueRoleNames)
    }
    
    // 去重处理：基于serviceRoleName去重
    const uniqueRoles = roles.filter((role, index, self) => 
      index === self.findIndex(r => r.serviceRoleName === role.serviceRoleName)
    )
    
    return uniqueRoles.map((role, index) => ({
      name: role.serviceRoleName,
      label: role.serviceRoleName,
      value: formData[role.serviceRoleName] || [],
      defaultValue: role.hosts || [],
      selectValue: availableHosts,
      type: 'multipleSelect' as const, // Worker/Client角色支持多选
      isHidden: false,
      required: false,
      uniqueKey: `${role.serviceRoleName}-${index}` // 添加唯一key
    }))
  }, [roles, availableHosts, formData])



  // 表单数据处理
  const handleFormChange = useCallback((name: string, value: string[]) => {
    setFormData(prev => ({
      ...prev,
      [name]: value
    }))
    // 清除该字段的错误
    if (errors[name]) {
      setErrors(prev => {
        const newErrors = { ...prev }
        delete newErrors[name]
        return newErrors
      })
    }
  }, [errors])

  // 获取所有主机
  const getAllHosts = useCallback(async () => {
    if (!cluster?.id) return
    
    setLoading(true)
    try {
      const response = await clusterApiV1.serviceRole.getAllHosts({
        clusterId: cluster.id
      })
      
      if (response?.success && response?.data) {
        // 构建完整的主机信息，使用后端返回的真实字段
        const hostsWithResources = response.data
          .filter((host: Record<string, unknown>) => host.id && host.hostname && host.ip) // 过滤掉必须字段为空的数据
          .map((host: Record<string, unknown>) => ({
            id: String(host.id),
            hostname: String(host.hostname),
            ip: String(host.ip),
            cpuCore: typeof host.coreNum === 'number' ? host.coreNum : undefined,
            memory: typeof host.totalMem === 'number' ? host.totalMem : undefined,
            disk: typeof host.totalDisk === 'number' ? host.totalDisk : undefined,
            cpuArchitecture: typeof host.cpuArchitecture === 'string' ? host.cpuArchitecture : undefined,
            osInfo: host.osType || host.osVersion ? {
              system: typeof host.osType === 'string' ? host.osType : undefined,
              version: typeof host.osVersion === 'string' ? host.osVersion : undefined
            } : undefined
          }))
        
        setAvailableHosts(hostsWithResources)
      } else {
        throw new Error(response?.message || '获取主机列表失败')
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '获取主机列表失败'
      setError(errorMessage)
      toast.error(errorMessage)
      return []
    } finally {
      setLoading(false)
    }
  }, [cluster?.id])

  // 获取非Master角色列表
  const getNonMasterRoleList = useCallback(async () => {
    if (!cluster?.id || !step4Data?.serviceIds?.length) return
    
    try {
      setLoading(true)
      
      // 防御性编程：确保serviceIds是数组
      const serviceIdsArray = Array.isArray(step4Data?.serviceIds) ? step4Data.serviceIds : []
      if (serviceIdsArray.length === 0) {
        setError('服务ID列表为空')
        return
      }
      const response = await clusterApiV1.serviceRole.getNonMasterRoleList(
        cluster.id,
        serviceIdsArray // 直接传递数组，不再转换为逗号分隔字符串
      )
      
      if (response?.success && response?.data) {
        const roleData = response.data
        

        
        // API响应数据处理
        
        setRoles(roleData)
        
        // 初始化表单数据
        const initialFormData: Record<string, string[]> = {}
        roleData.forEach((role: NonMasterRole) => {
          // 防御性编程：确保hosts是数组
          const hosts = Array.isArray(role.hosts) ? role.hosts : []
          initialFormData[role.serviceRoleName] = hosts
        })
        
        setFormData(initialFormData)
      } else {
        throw new Error(response?.message || '获取角色列表失败')
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '获取角色列表失败'
      setError(errorMessage)
      toast.error(errorMessage)
    } finally {
      setLoading(false)
    }
  }, [cluster?.id, step4Data?.serviceIds])

  // 初始化数据
  useEffect(() => {
    const serviceIds = Array.isArray(step4Data?.serviceIds) ? step4Data.serviceIds : []
    if (open && cluster?.id && serviceIds.length > 0) {
      const init = async () => {
        await getAllHosts()
        await getNonMasterRoleList()
      }
      init()
    }
  }, [open, cluster?.id, step4Data?.serviceIds, getAllHosts, getNonMasterRoleList])





  // 统计信息
  const stats = useMemo(() => {
    const totalRoles = roles.length
    const assignedRoles = Object.keys(formData).filter(key => {
      const value = formData[key]
      return value && Array.isArray(value) && value.length > 0
    }).length
    
    return {
      totalRoles,
      assignedRoles,
      availableHosts: availableHosts.length
    }
  }, [roles.length, formData, availableHosts.length])

  // 状态信息配置
  const statusInfo: StatusInfo = {
    text: "已分配",
    value: stats.assignedRoles,
    total: `${stats.totalRoles} 个角色`,
    pulse: true
  }

  // 状态徽章配置
  const statusBadge: StatusBadge = {
    text: "分配完成",
    variant: "success",
    show: stats.assignedRoles === stats.totalRoles
  }

  // 按钮配置
  const buttons: ActionButton[] = [
    {
      text: "上一步",
      onClick: () => {
        if (onPrevious) {
          onPrevious()
        } else {
          onOpenChange(false)
        }
      },
      variant: "secondary"
    },
    {
      text: "下一步",
      onClick: async () => {
        if (!cluster?.id || !onComplete) return
        
        try {
          setLoading(true)
          
          // 构建保存数据格式（参考Vue2项目）
          const mappings = Object.entries(formData).map(([serviceRole, hosts]) => ({
            serviceRole,
            hosts
          }))
          
          console.log('=== 保存Worker角色分配 ===')
          console.log('clusterId:', cluster.id)
          console.log('mappings:', mappings)
          
          // 调用保存接口（关键步骤：保存到缓存中）
          const response = await clusterApiV1.serviceRole.saveMapping(mappings)
          
          console.log('保存响应:', response)
          
          if (response?.success) {
            toast.success('Worker角色分配保存成功')
            
            // 构建步骤数据
            const roleHostMappings: ServiceRoleHostMapping[] = mappings.map(({serviceRole, hosts}) => ({
              serviceRole,
              hosts
            }))
            
            const step6Data: Step6Data = {
              roleHostMappings,
              selectedRoles: Object.keys(formData),
              assignedHosts: [],
              // 添加服务名称列表（用于后续步骤）
              serviceNames: [...new Set(mappings.map(m => m.serviceRole.split('_')[0]))].map(serviceName => ({
                serviceName
              }))
            }
            
            onComplete(step6Data)
          } else {
            throw new Error(response?.message || '保存失败')
          }
        } catch (error) {
          const errorMessage = error instanceof Error ? error.message : '保存Worker角色分配失败'
          console.error('保存Worker角色分配失败:', error)
          toast.error(errorMessage)
        } finally {
          setLoading(false)
        }
      },
      disabled: loading || stats.assignedRoles !== stats.totalRoles,
      loading: loading,
      loadingText: "保存中...",
      variant: "primary"
    }
  ]

  return (
    <ClusterWizardLayout
      open={open}
      onClose={() => onOpenChange(false)}
      clusterName={cluster?.clusterName || ''}
      clusterType={cluster?.depType}
      stepTitle="分配服务Worker与Client角色"
      stepDescription="分配服务Worker与Client角色 - 请为每个服务的Worker和Client角色选择部署的主机"
      currentStep={currentStepNumber}
      dialogTitle={`分配服务Worker与Client角色 - ${cluster?.clusterName}`}
      actionBar={
        <ClusterWizardActionBar
          statusInfo={statusInfo}
          statusBadge={statusBadge}
          buttons={buttons}
        />
      }
    >
      <div className="p-6 sm:p-8 flex-1" style={{ overflow: 'visible' }}>
        <div className="h-full flex flex-col">
          {loading ? (
            <div className="flex items-center justify-center h-40">
              <div className="flex items-center gap-3 text-gray-500">
                <Loader2 className="w-5 h-5 animate-spin" />
                <span>正在加载角色分配数据...</span>
              </div>
            </div>
          ) : error ? (
            <div className="flex flex-col items-center justify-center h-40 text-red-500">
              <AlertCircle className="w-8 h-8 mb-3 text-red-400" />
              <p className="text-lg font-medium mb-2">加载失败</p>
              <p className="text-sm text-gray-600">{error}</p>
              <Button 
                onClick={() => window.location.reload()}
                className="mt-4"
              >
                重试
              </Button>
            </div>
          ) : formItems.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-40 text-gray-500">
              <AlertCircle className="w-8 h-8 mb-3 text-gray-400" />
              <p className="text-lg font-medium">暂无服务角色</p>
              <p className="text-sm">请确保已选择服务</p>
            </div>
          ) : (
            <div className="flex-1 overflow-y-auto overflow-x-visible">
              <div className="space-y-3">
                {formItems.map((item) => (
                  <div key={item.uniqueKey || item.name} className="flex items-center gap-4 p-4 bg-white/80 backdrop-blur-sm border border-gray-200/60 rounded-lg hover:shadow-md transition-all duration-200">
                    {/* 角色名称 */}
                    <div className="flex items-center gap-2 min-w-0 w-48 flex-shrink-0">
                      <ServiceIcon
                        serviceName={roles.find(r => r.serviceRoleName === item.name)?.serviceName || extractServiceName(item.name)}
                        size={16}
                        className="w-4 h-4 flex-shrink-0"
                      />
                      <span className="font-medium text-gray-900 truncate">{item.label}</span>
                      {item.required && (
                        <Badge variant="secondary" className="text-xs px-1.5 py-0.5 flex-shrink-0">必需</Badge>
                      )}
                    </div>

                    {/* 类型标识 */}
                    <div className="w-12 flex-shrink-0">
                      <Badge variant="outline" className="text-xs">
                        多选
                      </Badge>
                    </div>

                    {/* 超级主机选择器 */}
                    <div className="flex-1">
                      <SuperHostSelector
                        hosts={item.selectValue || []}
                        selectedHosts={formData[item.name] || []}
                        onSelectionChange={(hostnames) => {
                          console.log(`🔄 Worker角色[${item.name}]选择变化:`, hostnames)
                          console.log(`   当前formData[${item.name}]:`, formData[item.name])
                          console.log(`   可用主机数量:`, (item.selectValue || []).length)
                          handleFormChange(item.name, hostnames)
                        }}
                        placeholder="选择多台主机"
                        multiple={true}
                        serviceName={item.name}
                      />
                    </div>

                    {/* 错误信息 */}
                    {errors[item.name] && (
                      <div className="w-48 flex-shrink-0">
                        <p className="text-red-500 text-xs flex items-center gap-1">
                          <AlertCircle className="w-3 h-3" />
                          {errors[item.name]}
                        </p>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </ClusterWizardLayout>
  )
}

export default WorkerRoleAssignDialog
