"use client"

import { useState, useEffect, useCallback, useMemo } from 'react'
import { toast } from 'sonner'
import { AlertCircle, Loader2 } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { clusterApiV1 } from '@/lib/api-utils-v1'
import type { ServiceRole, FormItem, HostMapping, Step5Data } from '@/types/master-role-assign'
import type { ClusterInfo } from '@/hooks/useCluster'
import ClusterWizardLayout from './cluster-wizard-layout'
import ClusterWizardActionBar, { type ActionButton, type StatusInfo, type StatusBadge } from './cluster-wizard-action-bar'
import SuperHostSelector, { type HostInfo } from './super-host-selector'
import ServiceIcon from '@/components/ui/service-icon'

/**
 * 从服务角色名中提取服务名
 * 例如：HDFS_NameNode → HDFS，YARN_ResourceManager → YARN
 */
const extractServiceName = (serviceRoleName: string): string => {
  if (!serviceRoleName) return ''
  
  // 服务角色名格式通常是：ServiceName_RoleName
  const parts = serviceRoleName.split('_')
  return parts[0] || serviceRoleName
}

/**
 * Step5 Dialog组件 - 分配服务Master角色
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 */

interface MasterRoleAssignDialogProps {
  /** 是否显示弹窗 */
  open: boolean
  /** 关闭弹窗回调 */
  onClose: () => void
  /** 集群信息 */
  cluster: ClusterInfo
  /** Step4数据 - 选择的服务 */
  step4Data: {
    serviceIds: string[] // 修复：20位long精度问题
    selectedServices: Array<{
      id: string // 修复：20位long精度问题
      name: string
      [key: string]: unknown
    }>
  }
  /** Step5完成回调 */
  onComplete: (step5Data: Step5Data) => void
  /** 上一步回调 */
  onPrevious?: () => void
}

const MasterRoleAssignDialog: React.FC<MasterRoleAssignDialogProps> = ({
  open,
  onClose,
  cluster,
  step4Data,
  onComplete,
  onPrevious,
}) => {
  // 状态管理
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [serviceRoles, setServiceRoles] = useState<ServiceRole[]>([])
  const [availableHosts, setAvailableHosts] = useState<HostInfo[]>([])
  const [formData, setFormData] = useState<Record<string, string | string[]>>({})
  const [errors, setErrors] = useState<Record<string, string>>({})

  // 步骤配置（K8s模式跳过Agent分发）
  const isK8s = cluster.depType?.toLowerCase() === 'kubernetes'
  const currentStep = isK8s ? 4 : 5 // Step5: 分配服务Master角色

  // 表单项配置生成
  const formItems = useMemo((): FormItem[] => {
    return serviceRoles.map(role => ({
      label: role.serviceRoleName,
      name: role.serviceRoleName,
      value: formData[role.serviceRoleName] || (role.cardinality === "1" ? "" : []),
      defaultValue: role.hosts || (role.cardinality === "1" ? (availableHosts[0]?.hostname || "") : []),
      selectValue: availableHosts,
      type: role.cardinality === "1" ? 'select' : 'multipleSelect',
      isHidden: false,
      required: role.serviceRoleType === "master"
    }))
  }, [serviceRoles, availableHosts, formData])

  // 获取所有主机（完整信息）
  const fetchAllHosts = useCallback(async () => {
    try {
      // 添加调试日志

      
      const response = await clusterApiV1.serviceRole.getAllHosts({
        clusterId: cluster.id
      })
      

      
      if (response.success && response.data) {
        // 构建完整的主机信息，使用后端返回的真实字段
        console.log('🔍 [调试] 后端返回的原始数据:', response.data[0])
        
        const hostsWithResources = response.data
          .filter((host: Record<string, unknown>) => host.id && host.hostname && host.ip) // 过滤掉必须字段为空的数据
          .map((host: Record<string, unknown>) => {
            const mappedHost = {
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
              // 移除used字段，因为后端返回的不是使用率而是绝对值
            }
            
            console.log('🔍 [调试] 映射后的主机数据:', {
              原始: { coreNum: host.coreNum, totalMem: host.totalMem, totalDisk: host.totalDisk, ip: host.ip },
              映射: { cpuCore: mappedHost.cpuCore, memory: mappedHost.memory, disk: mappedHost.disk, ip: mappedHost.ip }
            })
            
            return mappedHost
          })
        
        
        setAvailableHosts(hostsWithResources)
        return hostsWithResources // 返回完整的主机对象，而不是只返回主机名
      } else {
        throw new Error(response.message || '获取主机列表失败')
      }
    } catch (error) {
      toast.error(`获取主机列表失败: ${error instanceof Error ? error.message : '未知错误'}`)
      return []
    }
  }, [cluster.id])

  // 获取服务角色列表
  const fetchServiceRoles = useCallback(async (hosts: HostInfo[]) => {
    try {
      setLoading(true)
      
      // 防御性编程：确保serviceIds是数组
      const serviceIds = Array.isArray(step4Data?.serviceIds) ? step4Data.serviceIds : []
      if (serviceIds.length === 0) {
        toast.error('服务ID列表为空')
        setLoading(false)
        return
      }
      
      const serviceIdsStr = serviceIds.join(',')
      
      const response = await clusterApiV1.serviceRole.getList({
        clusterId: cluster.id,
        serviceIds: serviceIdsStr,
        serviceRoleType: 1 // 1表示Master角色
      })
      
      if (response.success && response.data) {
        setServiceRoles(response.data)
        setAvailableHosts(hosts)
        
        // 初始化表单数据
        const initialFormData: Record<string, string | string[]> = {}
        response.data.forEach((role: ServiceRole) => {
          if (role.hosts && role.hosts.length > 0) {
            initialFormData[role.serviceRoleName] = role.cardinality === "1" ? role.hosts[0] : role.hosts
          } else if (hosts.length > 0) {
            initialFormData[role.serviceRoleName] = role.cardinality === "1" ? hosts[0].hostname : []
          }
        })
        setFormData(initialFormData)
      } else {
        throw new Error(response.message || '获取服务角色列表失败')
      }
    } catch (error) {
      toast.error(`获取服务角色列表失败: ${error instanceof Error ? error.message : '未知错误'}`)
    } finally {
      setLoading(false)
    }
  }, [cluster.id, step4Data?.serviceIds])

  // 初始化数据
  useEffect(() => {
    const serviceIds = Array.isArray(step4Data?.serviceIds) ? step4Data.serviceIds : []
    if (open && serviceIds.length > 0) {
      const init = async () => {
        const hostsWithResources = await fetchAllHosts() // 现在返回完整的主机对象
        
        // 直接使用完整的主机对象
        if (hostsWithResources.length > 0) {
          await fetchServiceRoles(hostsWithResources) // 传递完整的主机数据
        }
      }
      init()
    } else if (!open) {
      // 重置状态
      setServiceRoles([])
      setAvailableHosts([])
      setFormData({})
      setErrors({})
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, step4Data?.serviceIds])



  // 表单值变更处理
  const handleFormChange = useCallback((name: string, value: string | string[]) => {
    setFormData(prev => ({
      ...prev,
      [name]: value
    }))
    
    // 清除错误
    if (errors[name]) {
      setErrors(prev => {
        const newErrors = { ...prev }
        delete newErrors[name]
        return newErrors
      })
    }
  }, [errors])

  // 表单验证
  const validateForm = useCallback((): boolean => {
    const newErrors: Record<string, string> = {}
    
    formItems.forEach(item => {
      if (item.required) {
        const value = formData[item.name]
        if (!value || (Array.isArray(value) && value.length === 0)) {
          newErrors[item.name] = `${item.label}不能为空`
        }
      }
    })
    
    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }, [formItems, formData])

  // 保存配置
  const handleSave = useCallback(async () => {
    if (!validateForm()) {
      toast.error('请检查表单数据')
      return
    }

    setSaving(true)
    try {
      // 构建保存数据
      const mappings: HostMapping[] = Object.keys(formData).map(serviceRole => ({
        serviceRole,
        hosts: Array.isArray(formData[serviceRole]) 
          ? formData[serviceRole] as string[]
          : [formData[serviceRole] as string]
      }))

      const response = await clusterApiV1.serviceRole.saveMapping(cluster.id, mappings)
      
      if (response.success) {
        toast.success('Master角色分配完成')
        
        // 构建Step5数据
        const step5Data: Step5Data = {
          roleMappings: mappings,
          availableHosts: availableHosts.map(h => h.hostname),
          serviceRoles
        }
        
        onComplete(step5Data)
      } else {
        throw new Error(response.message || '保存失败')
      }
    } catch (error) {
      toast.error(`保存失败: ${error instanceof Error ? error.message : '未知错误'}`)
    } finally {
      setSaving(false)
    }
  }, [validateForm, formData, cluster.id, availableHosts, serviceRoles, onComplete])

  // 统计信息
  const stats = useMemo(() => {
    const totalRoles = serviceRoles.length
    const assignedRoles = Object.keys(formData).filter(key => {
      const value = formData[key]
      return value && (Array.isArray(value) ? value.length > 0 : value !== "")
    }).length
    
    return {
      totalRoles,
      assignedRoles,
      availableHosts: availableHosts.length
    }
  }, [serviceRoles.length, formData, availableHosts.length])

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
          onClose()
        }
      },
      variant: "secondary"
    },
    {
      text: "下一步",
      onClick: handleSave,
      disabled: saving || loading || stats.assignedRoles !== stats.totalRoles,
      loading: saving,
      loadingText: "保存中...",
      variant: "primary"
    }
  ]

  return (
    <ClusterWizardLayout
      open={open}
      onClose={onClose}
      clusterName={cluster.clusterName}
      clusterType={cluster.depType}
      stepTitle="分配服务Master角色"
      stepDescription="分配服务Master角色 - 为所选服务指定Master节点主机"
      currentStep={currentStep}
      dialogTitle={`分配服务Master角色 - ${cluster.clusterName}`}
      actionBar={
        <ClusterWizardActionBar
          statusInfo={statusInfo}
          statusBadge={statusBadge}
          buttons={buttons}
        />
      }
    >
      <div className="p-6 sm:p-8 flex-1" style={{ overflow: 'visible' }}>
        <div className="h-full flex flex-col" style={{ overflow: 'visible' }}>
          {loading ? (
            <div className="flex items-center justify-center h-40">
              <div className="flex items-center gap-3 text-gray-500">
                <Loader2 className="w-5 h-5 animate-spin" />
                <span>正在加载服务角色...</span>
              </div>
            </div>
          ) : formItems.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-40 text-gray-500">
              <AlertCircle className="w-8 h-8 mb-3 text-gray-400" />
              <p className="text-lg font-medium">暂无服务角色</p>
              <p className="text-sm">请确保已选择服务</p>
            </div>
          ) : (
            <div className="flex-1 overflow-y-auto" style={{ overflowX: 'visible' }}>
              <div className="space-y-3">
                {formItems.map((item) => (
                  <div key={item.name} className="flex items-center gap-4 p-4 bg-white/80 backdrop-blur-sm border border-gray-200/60 rounded-lg hover:shadow-md transition-all duration-200">
                    {/* 角色名称 */}
                    <div className="flex items-center gap-2 min-w-0 w-48 flex-shrink-0">
                      <ServiceIcon
                        serviceName={serviceRoles.find(r => r.serviceRoleName === item.name)?.serviceName || extractServiceName(item.name)}
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
                        {item.type === 'select' ? '单选' : '多选'}
                      </Badge>
                    </div>

                    {/* 超级主机选择器 */}
                    <div className="flex-1 min-w-0">

                      <SuperHostSelector
                        hosts={availableHosts}
                        selectedHosts={
                          item.type === 'select' 
                            ? (formData[item.name] as string ? [formData[item.name] as string] : [])
                            : (formData[item.name] as string[] || [])
                        }
                        onSelectionChange={(hostnames) => {
                          if (item.type === 'select') {
                            handleFormChange(item.name, hostnames[0] || '')
                          } else {
                            handleFormChange(item.name, hostnames)
                          }
                        }}
                        placeholder={item.type === 'select' ? "选择主机" : "选择多台主机"}
                        multiple={item.type !== 'select'}
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

export default MasterRoleAssignDialog
