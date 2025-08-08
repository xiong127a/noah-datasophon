"use client"

import { useState, useEffect, useCallback, useMemo } from 'react'
import { toast } from 'sonner'
import { AlertCircle, Users, Settings, Loader2, ChevronLeft, ChevronRight, User } from 'lucide-react'
import { Dialog, DialogContent, DialogTitle } from '@/components/ui/dialog'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Badge } from '@/components/ui/badge'
import { DIALOG_STYLES } from './shared-styles'
import { clusterApiV1 } from '@/lib/api-utils-v1'
import type { ServiceRole, FormItem, HostMapping, Step5Data } from '@/types/step5'
import type { ClusterInfo } from '@/hooks/useCluster'
import ClusterWizardSidebar from './cluster-wizard-sidebar'
import { getStepsByType, StepsType } from '@/lib/cluster-steps'
import { ClusterTypeUtil } from '@/types'

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
  const isK8s = ClusterTypeUtil.isKubernetes(cluster.depType || '')
  const steps = getStepsByType(StepsType.NORMAL)
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

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className={DIALOG_STYLES.content}>
        <DialogTitle className="sr-only">
          分配服务Master角色 - {cluster.clusterName}
        </DialogTitle>

        <div className="flex h-full max-h-[min(calc(100vh-96px),900px)] sm:max-h-[min(95vh,900px)]">
          {/* 左侧导航 */}
          <ClusterWizardSidebar 
            steps={steps}
            currentStep={currentStep}
            title="集群配置向导"
            clusterName={cluster.clusterName}
            isK8s={isK8s}
            onClose={onClose}
          />

          {/* 右侧内容区域 */}
          <div className="flex-1 flex flex-col min-h-0">
            {/* 当前步骤标题 */}
            <div className="p-6 sm:p-8 border-b border-slate-200/70 bg-gradient-to-r from-white via-indigo-50/30 to-purple-50/30 relative">
              {/* 装饰性光效 */}
              <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/60 to-transparent"></div>
              {/* 分割线光效 */}
              <div className="absolute bottom-0 left-6 right-6 h-px bg-gradient-to-r from-transparent via-indigo-200/80 to-transparent"></div>
              <div className="flex items-center justify-between relative z-10">
                <div>
                  <h2 className="text-lg sm:text-xl lg:text-2xl font-bold text-gray-900">
                    分配服务Master角色
                  </h2>
                  <p className="text-gray-600 mt-1">
                    为所选服务指定Master节点主机
                  </p>
                </div>
                <Badge variant="outline" className="text-indigo-600 border-indigo-200 bg-white/80 backdrop-blur-sm">
                  步骤 {currentStep}/{steps.length}
                </Badge>
              </div>
            </div>

            {/* 步骤内容 */}
            <div className="flex-1 flex flex-col min-h-0 bg-gradient-to-b from-white to-slate-50/50">
              <div className="p-6 sm:p-8 flex-1 overflow-hidden">
                {/* 主要内容区域 */}
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
            </div>

            {/* 底部操作栏 */}
            <div className="bg-white/95 backdrop-blur-md border-t border-gray-200/80 p-4 shadow-lg">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-4">
                  <div className="flex items-center space-x-3">
                    <div className="w-3 h-3 rounded-full bg-blue-500 animate-pulse"></div>
                    <span className="text-sm font-medium text-gray-700">
                      已分配 
                      <span className="mx-1 px-2 py-0.5 bg-blue-100 text-blue-700 rounded-full text-xs font-semibold">
                        {stats.assignedRoles}
                      </span>
                      / {stats.totalRoles} 个角色
                    </span>
                  </div>
                  {stats.assignedRoles === stats.totalRoles && (
                    <div className="flex items-center space-x-2 px-3 py-1.5 bg-green-50 rounded-lg border border-green-200">
                      <div className="w-2 h-2 rounded-full bg-green-500"></div>
                      <span className="text-sm font-medium text-green-700">
                        分配完成
                      </span>
                    </div>
                  )}
                </div>
                <div className="flex items-center space-x-3">
                  <button
                    onClick={onClose}
                    className="flex items-center px-5 py-2.5 bg-gray-50 hover:bg-gray-100 border border-gray-200 hover:border-gray-300 rounded-xl text-sm font-medium text-gray-700 transition-all duration-200 shadow-sm hover:shadow-md"
                  >
                    <ChevronLeft className="w-4 h-4 mr-2" />
                    上一步
                  </button>
                  <button
                    onClick={handleSave}
                    disabled={saving || loading || stats.assignedRoles !== stats.totalRoles}
                    className={`flex items-center px-6 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 shadow-md hover:shadow-lg ${
                      saving || loading || stats.assignedRoles !== stats.totalRoles
                        ? 'bg-gray-200 text-gray-400 cursor-not-allowed'
                        : 'bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 text-white transform hover:scale-105'
                    }`}
                  >
                    {saving ? (
                      <>
                        <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                        保存中...
                      </>
                    ) : (
                      <>
                        下一步
                        <ChevronRight className="w-4 h-4 ml-2" />
                      </>
                    )}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}

export default ClusterStep5Dialog
