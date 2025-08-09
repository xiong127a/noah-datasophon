"use client"

import { useState, useEffect, useCallback, useMemo } from 'react'
import { toast } from 'sonner'
import { AlertCircle, Loader2, User } from 'lucide-react'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Badge } from '@/components/ui/badge'
import { clusterApiV1 } from '@/lib/api-utils-v1'
import type { ServiceRole, FormItem, HostMapping, Step5Data } from '@/types/step5'
import type { ClusterInfo } from '@/hooks/useCluster'
import ClusterStepLayout from './cluster-step-layout'
import ClusterStepActionBar, { type ActionButton, type StatusInfo, type StatusBadge } from './cluster-step-action-bar'

/**
 * Step5 Dialog组件 - 分配服务Master角色
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 */

interface ClusterStep5DialogProps {
  /** 是否显示弹窗 */
  open: boolean
  /** 关闭弹窗回调 */
  onClose: () => void
  /** 集群信息 */
  cluster: ClusterInfo
  /** Step4数据 - 选择的服务 */
  step4Data: {
    serviceIds: number[]
    selectedServices: Array<{
      id: number
      name: string
      [key: string]: unknown
    }>
  }
  /** Step5完成回调 */
  onComplete: (step5Data: Step5Data) => void
}

const ClusterStep5Dialog: React.FC<ClusterStep5DialogProps> = ({
  open,
  onClose,
  cluster,
  step4Data,
  onComplete,
}) => {
  // 状态管理
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [serviceRoles, setServiceRoles] = useState<ServiceRole[]>([])
  const [availableHosts, setAvailableHosts] = useState<string[]>([])
  const [formData, setFormData] = useState<Record<string, string | string[]>>({})
  const [errors, setErrors] = useState<Record<string, string>>({})

  // 步骤配置
  const currentStep = 5 // Step5: 分配服务Master角色

  // 表单项配置生成
  const formItems = useMemo((): FormItem[] => {
    return serviceRoles.map(role => ({
      label: role.serviceRoleName,
      name: role.serviceRoleName,
      value: formData[role.serviceRoleName] || (role.cardinality === "1" ? "" : []),
      defaultValue: role.hosts || (role.cardinality === "1" ? (availableHosts[0] || "") : []),
      selectValue: availableHosts,
      type: role.cardinality === "1" ? 'select' : 'multipleSelect',
      isHidden: false,
      required: role.serviceRoleType === "master"
    }))
  }, [serviceRoles, availableHosts, formData])

  // 获取所有主机
  const fetchAllHosts = useCallback(async () => {
    try {
      console.log('调用API获取主机列表，集群ID:', cluster.id)
      
      const response = await clusterApiV1.serviceRole.getAllHosts({
        clusterId: cluster.id
      })
      
      console.log('主机列表API响应:', response)
      
      if (response.success && response.data) {
        const hostnames = response.data.map((host: any) => host.hostname).filter(Boolean)
        return hostnames
      } else {
        throw new Error(response.message || '获取主机列表失败')
      }
    } catch (error) {
      console.error('获取主机列表失败:', error)
      toast.error(`获取主机列表失败: ${error instanceof Error ? error.message : '未知错误'}`)
      return []
    }
  }, [cluster.id])

  // 获取服务角色列表
  const fetchServiceRoles = useCallback(async (hosts: string[]) => {
    try {
      setLoading(true)
      
      console.log('调用API获取服务角色，参数:', {
        clusterId: cluster.id,
        serviceIds: step4Data.serviceIds.join(','),
        serviceRoleType: 1
      })
      
      const response = await clusterApiV1.serviceRole.getList({
        clusterId: cluster.id,
        serviceIds: step4Data.serviceIds.join(','),
        serviceRoleType: 1 // 1表示Master角色
      })
      
      console.log('API响应:', response)
      
      if (response.success && response.data) {
        setServiceRoles(response.data)
        setAvailableHosts(hosts)
        
        // 初始化表单数据
        const initialFormData: Record<string, string | string[]> = {}
        response.data.forEach((role: ServiceRole) => {
          if (role.hosts && role.hosts.length > 0) {
            initialFormData[role.serviceRoleName] = role.cardinality === "1" ? role.hosts[0] : role.hosts
          } else if (hosts.length > 0) {
            initialFormData[role.serviceRoleName] = role.cardinality === "1" ? hosts[0] : []
          }
        })
        setFormData(initialFormData)
      } else {
        throw new Error(response.message || '获取服务角色列表失败')
      }
    } catch (error) {
      console.error('获取服务角色列表失败:', error)
      toast.error(`获取服务角色列表失败: ${error instanceof Error ? error.message : '未知错误'}`)
    } finally {
      setLoading(false)
    }
  }, [cluster.id, step4Data.serviceIds])

  // 初始化数据
  const initializeData = useCallback(async () => {
    if (open && step4Data.serviceIds.length > 0) {
      const hosts = await fetchAllHosts()
      if (hosts.length > 0) {
        await fetchServiceRoles(hosts)
      }
    }
  }, [open, step4Data.serviceIds, fetchAllHosts, fetchServiceRoles])

  useEffect(() => {
    if (open) {
      initializeData()
    } else {
      // 重置状态
      setServiceRoles([])
      setAvailableHosts([])
      setFormData({})
      setErrors({})
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open])

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
          availableHosts,
          serviceRoles
        }
        
        onComplete(step5Data)
      } else {
        throw new Error(response.message || '保存失败')
      }
    } catch (error) {
      console.error('保存Master角色分配失败:', error)
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
      onClick: onClose,
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
    <ClusterStepLayout
      open={open}
      onClose={onClose}
      clusterName={cluster.clusterName}
      clusterType={cluster.depType}
      stepTitle="分配服务Master角色"
      stepDescription="为所选服务指定Master节点主机"
      currentStep={currentStep}
      dialogTitle={`分配服务Master角色 - ${cluster.clusterName}`}
      actionBar={
        <ClusterStepActionBar
          statusInfo={statusInfo}
          statusBadge={statusBadge}
          buttons={buttons}
        />
      }
    >
      <div className="p-6 sm:p-8 flex-1 overflow-hidden">
        <div className="h-full flex flex-col">
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
            <div className="flex-1 overflow-y-auto">
              <div className="space-y-3">
                {formItems.map((item) => (
                  <div key={item.name} className="flex items-center gap-4 p-4 bg-white/80 backdrop-blur-sm border border-gray-200/60 rounded-lg hover:shadow-md transition-all duration-200">
                    {/* 角色名称 */}
                    <div className="flex items-center gap-2 min-w-0 w-48 flex-shrink-0">
                      <User className="w-4 h-4 text-blue-600" />
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

                    {/* 主机选择 */}
                    <div className="flex-1">
                      {item.type === 'select' ? (
                        <Select
                          value={formData[item.name] as string || ''}
                          onValueChange={(value) => handleFormChange(item.name, value)}
                        >
                          <SelectTrigger className="w-full h-9">
                            <SelectValue placeholder="选择主机" />
                          </SelectTrigger>
                          <SelectContent>
                            {item.selectValue?.map((host) => (
                              <SelectItem key={host} value={host}>
                                {host}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      ) : (
                        <div className="relative">
                          <Select>
                            <SelectTrigger className="w-full h-9">
                              <SelectValue placeholder={
                                (formData[item.name] as string[] || []).length > 0 
                                  ? `已选择 ${(formData[item.name] as string[] || []).length} 台主机`
                                  : "选择主机"
                              } />
                            </SelectTrigger>
                            <SelectContent>
                              <div className="p-2 space-y-1 max-h-48 overflow-y-auto">
                                {item.selectValue?.map((host) => (
                                  <label key={host} className="flex items-center space-x-2 cursor-pointer p-1 hover:bg-gray-50 rounded text-sm">
                                    <input
                                      type="checkbox"
                                      checked={(formData[item.name] as string[] || []).includes(host)}
                                      onChange={(e) => {
                                        const currentValue = formData[item.name] as string[] || []
                                        if (e.target.checked) {
                                          handleFormChange(item.name, [...currentValue, host])
                                        } else {
                                          handleFormChange(item.name, currentValue.filter(h => h !== host))
                                        }
                                      }}
                                      className="rounded border-gray-300 h-3 w-3"
                                    />
                                    <span>{host}</span>
                                  </label>
                                ))}
                              </div>
                            </SelectContent>
                          </Select>
                        </div>
                      )}
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
    </ClusterStepLayout>
  )
}

export default ClusterStep5Dialog
