"use client"

import { useState, useEffect, useCallback, useMemo } from 'react'
import { toast } from 'sonner'
import { Check, X, AlertCircle, Users, Settings, ChevronDown, Loader2 } from 'lucide-react'
import { Dialog, DialogContent, DialogTitle } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Badge } from '@/components/ui/badge'
import { Label } from '@/components/ui/label'
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
  /** 当前集群信息 */
  cluster: ClusterInfo
  /** 从Step4传递的服务选择数据 */
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
        const hostnames = response.data.map(host => host.hostname)
        setAvailableHosts(hostnames)
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
  const fetchServiceRoles = useCallback(async (hosts: string[] = []) => {
    try {
      console.log('调用API获取服务角色列表，参数:', {
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
        
        // 初始化表单数据，使用传入的hosts参数而不是依赖availableHosts
        const initialFormData: Record<string, string | string[]> = {}
        response.data.forEach(role => {
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
    }
  }, [cluster.id, step4Data.serviceIds])

  // 初始化数据
  const initializeData = useCallback(async () => {
    setLoading(true)
    try {
      // 先获取主机列表
      const hosts = await fetchAllHosts()
      if (hosts.length > 0) {
        // 再获取服务角色列表，传入hosts参数
        await fetchServiceRoles(hosts)
      }
    } finally {
      setLoading(false)
    }
  }, [fetchAllHosts, fetchServiceRoles])

  // 弹窗打开时初始化数据
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
  }, [open]) // 移除initializeData依赖，避免循环

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
            <div className="flex-1 overflow-y-auto bg-gradient-to-b from-white to-slate-50/50 min-h-0 [&::-webkit-scrollbar]:w-2 [&::-webkit-scrollbar-track]:bg-transparent [&::-webkit-scrollbar-thumb]:bg-indigo-200/60 [&::-webkit-scrollbar-thumb]:rounded-full [&::-webkit-scrollbar-thumb:hover]:bg-indigo-300/80 [&::-webkit-scrollbar]:transition-all">
              <div className="p-6 sm:p-8 lg:p-10">
                {/* 主要内容区域 */}
                <div className="grid grid-cols-5 gap-6">
                  {/* 左侧：角色分配表单 */}
                  <div className="col-span-4">
                    {/* 添加调试信息 */}
                    <div className="mb-4 p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
                      <p className="text-sm text-yellow-800">
                        <strong>调试信息：</strong> 
                        集群ID: {cluster.id}, 
                        服务IDs: {step4Data.serviceIds.join(',')}, 
                        服务数量: {step4Data.selectedServices.length}
                      </p>
                    </div>
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
                      <div className="space-y-4 max-h-[calc(100vh-400px)] overflow-y-auto scrollbar-thin scrollbar-thumb-gray-300 scrollbar-track-gray-100">
                        {formItems.map((item) => (
                    <Card key={item.name} className="bg-white/70 backdrop-blur-sm border border-gray-200/60 hover:shadow-md transition-all duration-200">
                      <CardHeader className="pb-3">
                        <CardTitle className="flex items-center justify-between">
                          <div className="flex items-center gap-2">
                            <Settings className="w-4 h-4 text-gray-600" />
                            <span className="text-sm font-medium">{item.label}</span>
                            {item.required && (
                              <Badge variant="secondary" className="text-xs px-2 py-0.5">必需</Badge>
                            )}
                          </div>
                          <Badge variant="outline" className="text-xs">
                            {item.type === 'select' ? '单选' : '多选'}
                          </Badge>
                        </CardTitle>
                      </CardHeader>
                      <CardContent className="pt-0">
                        <div className="space-y-2">
                          <Label htmlFor={item.name} className="text-sm text-gray-700">
                            选择主机
                          </Label>
                          
                          {item.type === 'select' ? (
                            <Select
                              value={formData[item.name] as string || ""}
                              onValueChange={(value) => handleFormChange(item.name, value)}
                            >
                              <SelectTrigger className="w-full h-10 bg-white/80 border-gray-200 focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20">
                                <SelectValue placeholder="请选择主机" />
                                <ChevronDown className="w-4 h-4 text-gray-400" />
                              </SelectTrigger>
                              <SelectContent className="bg-white/95 backdrop-blur-xl border-0 rounded-xl shadow-2xl">
                                {item.selectValue.map((host) => (
                                  <SelectItem key={host} value={host} className="text-sm">
                                    {host}
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                          ) : (
                            <Select
                              value={Array.isArray(formData[item.name]) && (formData[item.name] as string[]).length > 0 
                                ? (formData[item.name] as string[])[0] 
                                : ""}
                              onValueChange={(value) => {
                                const currentValue = formData[item.name] as string[] || []
                                if (value && !currentValue.includes(value)) {
                                  handleFormChange(item.name, [...currentValue, value])
                                }
                              }}
                            >
                              <SelectTrigger className="w-full h-10 bg-white/80 border-gray-200 focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20">
                                <SelectValue placeholder="选择多个主机" />
                                <ChevronDown className="w-4 h-4 text-gray-400" />
                              </SelectTrigger>
                              <SelectContent className="bg-white/95 backdrop-blur-xl border-0 rounded-xl shadow-2xl">
                                {item.selectValue.map((host) => (
                                  <SelectItem key={host} value={host} className="text-sm">
                                    {host}
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                          )}
                          
                          {/* 多选显示已选择的主机 */}
                          {item.type === 'multipleSelect' && Array.isArray(formData[item.name]) && (
                            <div className="flex flex-wrap gap-2 mt-2">
                              {(formData[item.name] as string[]).map((host) => (
                                <Badge 
                                  key={host} 
                                  variant="secondary" 
                                  className="text-xs px-2 py-1 flex items-center gap-1"
                                >
                                  {host}
                                  <X 
                                    className="w-3 h-3 cursor-pointer hover:text-red-500" 
                                    onClick={() => {
                                      const currentValue = formData[item.name] as string[]
                                      handleFormChange(item.name, currentValue.filter(h => h !== host))
                                    }}
                                  />
                                </Badge>
                              ))}
                            </div>
                          )}
                          
                          {errors[item.name] && (
                            <p className="text-xs text-red-500 mt-1">{errors[item.name]}</p>
                          )}
                        </div>
                      </CardContent>
                    </Card>
                        ))}
                      </div>
                    )}
                  </div>

                  {/* 右侧：统计面板 */}
                  <div className="col-span-1 space-y-4">
            <Card className="bg-gradient-to-br from-blue-50 to-indigo-50 border-blue-200/60">
              <CardHeader className="pb-3">
                <CardTitle className="text-sm font-medium text-blue-900 flex items-center gap-2">
                  <Users className="w-4 h-4" />
                  分配统计
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="flex justify-between items-center">
                  <span className="text-xs text-blue-700">服务角色</span>
                  <span className="text-sm font-semibold text-blue-900">{stats.totalRoles}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-xs text-blue-700">已分配</span>
                  <span className="text-sm font-semibold text-blue-900">{stats.assignedRoles}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-xs text-blue-700">可用主机</span>
                  <span className="text-sm font-semibold text-blue-900">{stats.availableHosts}</span>
                </div>
                
                {/* 进度指示 */}
                <div className="mt-4 pt-3 border-t border-blue-200">
                  <div className="flex items-center gap-2 mb-2">
                    <div className={`w-2 h-2 rounded-full ${stats.assignedRoles === stats.totalRoles ? 'bg-green-500' : 'bg-orange-400'}`} />
                    <span className="text-xs text-blue-700">
                      {stats.assignedRoles === stats.totalRoles ? '分配完成' : '待分配'}
                    </span>
                  </div>
                  <div className="w-full bg-blue-200 rounded-full h-1.5">
                    <div 
                      className="bg-blue-500 h-1.5 rounded-full transition-all duration-300"
                      style={{ 
                        width: stats.totalRoles > 0 ? `${(stats.assignedRoles / stats.totalRoles) * 100}%` : '0%' 
                      }}
                    />
                  </div>
                </div>
              </CardContent>
                    </Card>
                  </div>
                </div>
              </div>
            </div>

            {/* 底部操作栏 */}
            <div className="flex items-center justify-between px-6 py-4 bg-gray-50/80 backdrop-blur-sm border-t border-gray-200/60">
          <div className="flex items-center gap-2 text-sm text-gray-600">
            <AlertCircle className="w-4 h-4" />
            <span>请为所有必需的Master角色分配主机</span>
          </div>
          
          <div className="flex items-center gap-3">
            <Button 
              variant="outline" 
              onClick={onClose}
              className="px-6 py-2 h-10 rounded-xl border-gray-300 hover:bg-gray-100 transition-colors"
            >
              取消
            </Button>
            <Button 
              onClick={handleSave}
              disabled={saving || loading || stats.assignedRoles !== stats.totalRoles}
              className="px-6 py-2 h-10 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white shadow-lg hover:shadow-xl transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {saving ? (
                <>
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                  保存中...
                </>
              ) : (
                <>
                  <Check className="w-4 h-4 mr-2" />
                  完成分配
                </>
              )}
              </Button>
            </div>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}

export default ClusterStep5Dialog
