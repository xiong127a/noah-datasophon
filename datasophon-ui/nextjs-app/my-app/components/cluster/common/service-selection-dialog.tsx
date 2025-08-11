"use client"

import React, { useState, useEffect, useMemo } from 'react'
import { 
  Loader2, RefreshCw, AlertCircle, Package, Search, ChevronLeft, ChevronRight
} from 'lucide-react'
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import ServiceIcon from "@/components/ui/service-icon"
import { clusterApiV1 } from "@/lib/api-utils-v1"
import { toast } from 'sonner'
import ClusterWizardSidebar from './cluster-wizard-sidebar'
import { Badge } from '@/components/ui/badge'
import { getStepsByType, StepsType } from '@/lib/cluster-wizard-steps'
import { ClusterTypeUtil, ClusterType } from '@/types'
import { Dialog, DialogContent, DialogTitle } from '@/components/ui/dialog'
import { createClusterHeaders } from '@/lib/cluster-id-header'
import { DIALOG_STYLES } from './shared-styles'

import { SERVICE_TYPE_OPTIONS, ServiceType } from '@/types/service-selection'
import type { 
  ServiceSelectionDialogProps, 
  Service, 
  Step3Data
} from '@/types/service-selection'

/**
 * 集群步骤4：大数据服务选择对话框
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 */

const ServiceSelectionDialog: React.FC<ServiceSelectionDialogProps> = ({
  open,
  onOpenChange,
  cluster,
  clusterType,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  step2Data,
  onComplete,
  onPrevious
}) => {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [services, setServices] = useState<Service[]>([])
  const [selectedServiceIds, setSelectedServiceIds] = useState<number[]>([])
  const [serviceTypeFilter, setServiceTypeFilter] = useState<ServiceType>(ServiceType.MINIMAL)
  const [searchTerm, setSearchTerm] = useState('')

  // 获取服务列表
  const fetchServices = React.useCallback(async () => {
    if (!cluster?.id) return
    setLoading(true)
    setError(null)
    try {
      const headers = createClusterHeaders(cluster.id)
      const response = await clusterApiV1.service.listWithRequired({
        type: serviceTypeFilter
      }, { headers })
      if (response.data?.success && response.data?.data) {
        setServices(response.data.data)
        // 自动选择逻辑由 useEffect 处理
      } else {
        throw new Error(response.data?.message || '获取服务列表失败')
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '获取服务列表失败'
      setError(errorMessage)
      toast.error(errorMessage)
    } finally {
      setLoading(false)
    }
  }, [cluster?.id, serviceTypeFilter])

  // 初始化加载
  useEffect(() => {
    if (open && cluster?.id) {
      fetchServices()
    }
  }, [open, cluster?.id, fetchServices])

  // 服务类型变化时重新加载并自动选择
  useEffect(() => {
    if (services.length > 0) {
      const requiredServices = services.filter(service => service.isRequired)
      const requiredServiceIds = requiredServices.map(service => service.id)
      
      // 无论哪种模式，都只选择必需服务，清除其他选择
      setSelectedServiceIds(requiredServiceIds)
      
      // 提示用户
    if (serviceTypeFilter === ServiceType.MINIMAL) {
        toast.success(`最小化模式：已选择 ${requiredServices.length} 个必需服务`)
    } else {
        toast.success(`自定义模式：已重置为 ${requiredServices.length} 个必需服务，可继续选择其他服务`)
      }
    }
  }, [serviceTypeFilter, services])

  // 过滤服务
  const filteredServices = useMemo(() => {
    return services.filter(service => 
      service.serviceName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      service.serviceDesc?.toLowerCase().includes(searchTerm.toLowerCase())
    )
  }, [services, searchTerm])

  // 已选择的服务
  const selectedServices = useMemo(() => {
    return services.filter(service => selectedServiceIds.includes(service.id))
  }, [services, selectedServiceIds])

  // 服务选择处理
  const handleServiceToggle = (serviceId: number) => {
    const service = services.find(s => s.id === serviceId)
    if (service?.isRequired) {
      toast.warning('必需服务不能取消选择')
      return
    }

    setSelectedServiceIds(prev => 
      prev.includes(serviceId)
        ? prev.filter(id => id !== serviceId)
        : [...prev, serviceId]
    )
  }

  // 下一步处理
  const handleNext = () => {
    if (selectedServiceIds.length === 0) {
      toast.error('请至少选择一个服务')
      return
    }

    const step3Data: Step3Data = {
      serviceIds: selectedServiceIds,
      serviceNames: selectedServices.map(service => ({
        serviceId: service.id,
        serviceName: service.serviceName
      })),
      serviceType: serviceTypeFilter
    }

    onComplete(step3Data)
  }

  // 统计信息
  const stats = useMemo(() => {
    const total = filteredServices.length
    const selected = selectedServiceIds.length
    const required = filteredServices.filter(s => s.isRequired).length

    return { total, selected, required }
  }, [filteredServices, selectedServiceIds])

  // 计算当前步骤编号
  const safeClusterType = clusterType || ''
  const isK8s = ClusterTypeUtil.isKubernetes(safeClusterType)
  const depType = isK8s ? ClusterType.KUBERNETES : ClusterType.PVM
  const steps = getStepsByType(StepsType.NORMAL, depType)
  const currentStepNumber = isK8s ? 3 : 4

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className={DIALOG_STYLES.content}>
        <DialogTitle className="sr-only">
          选择大数据服务 - {cluster?.clusterName}
        </DialogTitle>
        
        <div className="flex h-full max-h-[min(calc(100vh-96px),900px)] sm:max-h-[min(95vh,900px)]">
          <ClusterWizardSidebar 
            steps={steps}
            currentStep={currentStepNumber}
            title="集群配置向导"
            clusterName={cluster?.clusterName || ''}
            isK8s={isK8s}
            onClose={() => onOpenChange(false)}
          />

          <div className="flex-1 flex flex-col min-h-0">
            {/* 标题区域 */}
            <div className="p-6 sm:p-8 border-b border-slate-200/70 bg-gradient-to-r from-white via-indigo-50/30 to-purple-50/30 relative">
              <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/60 to-transparent"></div>
              <div className="absolute bottom-0 left-6 right-6 h-px bg-gradient-to-r from-transparent via-indigo-200/80 to-transparent"></div>
              <div className="flex items-center justify-between relative z-10">
                <div>
                  <h2 className="text-lg sm:text-xl lg:text-2xl font-bold text-gray-900">
                    选择大数据服务
                  </h2>
                  <p className="text-gray-600 mt-1">
                    根据您的需求选择要部署的大数据服务组件
                  </p>
                </div>
                <Badge variant="outline" className="text-indigo-600 border-indigo-200 bg-white/80 backdrop-blur-sm">
                  步骤 {currentStepNumber}/{steps.length}
                </Badge>
              </div>
            </div>

            {/* 主要内容区域 */}
            <div className="flex-1 flex flex-col min-h-0">
              {/* 顶部过滤区域 - 精美设计 */}
              <div className="flex-shrink-0 p-4 pb-0">
                <div className="bg-gradient-to-r from-white via-blue-50/30 to-indigo-50/50 rounded-2xl p-4 border border-blue-100/50 shadow-sm">
                  <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4">
                    {/* 服务类型选择 */}
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-xl flex items-center justify-center shadow-lg shadow-blue-500/30">
                        <Package className="w-5 h-5 text-white" />
                      </div>
                      <div>
                        <label className="text-sm font-semibold text-gray-900 mb-1 block">服务类型</label>
                        <Select value={serviceTypeFilter} onValueChange={(value: string) => setServiceTypeFilter(value as ServiceType)}>
                          <SelectTrigger className="w-40 h-9 text-sm border-blue-200 rounded-lg bg-white shadow-sm hover:shadow-md transition-shadow">
                            <SelectValue>
                              {SERVICE_TYPE_OPTIONS.find(option => option.value === serviceTypeFilter)?.label}
                            </SelectValue>
                          </SelectTrigger>
                          <SelectContent className="rounded-xl border-blue-100 shadow-xl">
                            {SERVICE_TYPE_OPTIONS.map((option) => (
                              <SelectItem key={option.value} value={option.value} className="rounded-lg">
                                <div className="flex flex-col py-1">
                                  <span className="font-semibold text-gray-900">{option.label}</span>
                                  <span className="text-xs text-blue-600/80">{option.description}</span>
                                </div>
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>
                    </div>

                    {/* 搜索框 */}
                    <div className="w-full sm:flex-1">
                      <label className="text-sm font-semibold text-gray-900 mb-1 block">快速搜索</label>
                      <div className="relative">
                        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-indigo-400 w-4 h-4" />
                        <Input
                          type="text"
                          placeholder="输入服务名称或描述搜索..."
                          value={searchTerm}
                          onChange={(e) => setSearchTerm(e.target.value)}
                          className="pl-10 h-9 text-sm border-blue-200 rounded-lg bg-white shadow-sm hover:shadow-md focus:border-indigo-400 focus:ring-indigo-200 transition-all"
                        />
                      </div>
                    </div>

                    {/* 统计信息卡片 */}
                    <div className="flex items-center gap-3">
                      <div className="hidden sm:flex items-center gap-3">
                        <div className="bg-white rounded-lg px-3 py-2 border border-blue-100 shadow-sm">
                          <div className="text-xs text-gray-600">总数</div>
                          <div className="text-lg font-bold text-blue-600">{stats.total}</div>
                        </div>
                        <div className="bg-white rounded-lg px-3 py-2 border border-emerald-100 shadow-sm">
                          <div className="text-xs text-gray-600">已选</div>
                          <div className="text-lg font-bold text-emerald-600">{stats.selected}</div>
                        </div>
                        <div className="bg-white rounded-lg px-3 py-2 border border-red-100 shadow-sm">
                          <div className="text-xs text-gray-600">必需</div>
                          <div className="text-lg font-bold text-red-600">{stats.required}</div>
                        </div>
                      </div>
                      
                      {/* 移动端统计 */}
                      <div className="sm:hidden bg-white rounded-lg px-3 py-2 border border-blue-100 shadow-sm">
                        <div className="text-xs text-gray-600 text-center">已选/总数</div>
                        <div className="text-lg font-bold text-center">
                          <span className="text-emerald-600">{stats.selected}</span>
                          <span className="text-gray-400 mx-1">/</span>
                          <span className="text-blue-600">{stats.total}</span>
                        </div>
                      </div>

                      {/* 刷新按钮 */}
                      <button
                        onClick={fetchServices}
                        disabled={loading}
                        className="w-10 h-10 bg-gradient-to-br from-purple-500 to-purple-600 rounded-xl flex items-center justify-center text-white shadow-lg shadow-purple-500/30 hover:shadow-xl hover:shadow-purple-500/40 transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        <RefreshCw className={`w-5 h-5 ${loading ? 'animate-spin' : ''}`} />
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              {/* 服务选择区域 - 高密度网格布局 */}
              <div className="flex-1 p-4 pt-4 pb-0 min-h-0">
                {loading ? (
                  <div className="flex items-center justify-center h-48">
                    <div className="flex items-center space-x-3">
                      <Loader2 className="w-5 h-5 animate-spin text-blue-500" />
                      <span className="text-gray-600">正在加载服务列表...</span>
                    </div>
                  </div>
                ) : error ? (
                  <div className="flex flex-col items-center justify-center h-48">
                    <AlertCircle className="w-8 h-8 mb-2 text-red-500" />
                    <p className="text-gray-900 font-medium mb-1">加载失败</p>
                    <p className="text-gray-600 text-sm mb-3">{error}</p>
                    <button 
                      onClick={fetchServices}
                      className="px-4 py-2 bg-blue-500 text-white text-sm rounded hover:bg-blue-600 transition-colors"
                    >
                      重试
                    </button>
                  </div>
                ) : filteredServices.length === 0 ? (
                  <div className="flex flex-col items-center justify-center h-48">
                    <Package className="w-8 h-8 mb-2 text-gray-400" />
                    <p className="text-gray-900 font-medium">暂无服务</p>
                    <p className="text-gray-600 text-sm">没有找到符合条件的服务</p>
                  </div>
                ) : (
                  <div className="h-full overflow-y-auto pr-2">
                    <div className="grid grid-cols-3 sm:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4 pb-4">
                      {filteredServices.map((service) => {
                        const isSelected = selectedServiceIds.includes(service.id)
                        
                        return (
                          <div
                            key={service.id}
                            className={`relative cursor-pointer rounded-xl p-4 transition-all duration-200 group ${
                              isSelected
                                ? 'border-2 border-blue-500 bg-gradient-to-br from-blue-50 to-indigo-50 shadow-lg shadow-blue-500/20'
                                : 'border border-gray-200 bg-white hover:border-indigo-300 hover:shadow-md hover:bg-gradient-to-br hover:from-gray-50 hover:to-blue-50'
                            }`}
                            onClick={() => handleServiceToggle(service.id)}
                          >
                            {/* 状态标识 */}
                            <div className="absolute top-2 right-2 flex flex-col gap-1">
                              {service.isRequired && (
                                <span className="px-1.5 py-0.5 bg-red-500 text-white text-xs font-medium rounded text-center leading-none">
                                  必选
                                </span>
                              )}
                              {service.installed && (
                                <span className="px-1.5 py-0.5 bg-emerald-500 text-white text-xs font-medium rounded text-center leading-none">
                                  已装
                                </span>
                              )}
                            </div>

                            {/* 服务图标 */}
                            <div className="flex justify-center mb-3">
                              <div className={`w-12 h-12 rounded-xl flex items-center justify-center transition-all duration-200 ${
                                isSelected
                                  ? 'bg-gradient-to-br from-blue-500 to-indigo-600 shadow-lg shadow-blue-500/30' 
                                  : 'bg-gradient-to-br from-gray-100 to-gray-200 group-hover:from-indigo-100 group-hover:to-blue-100'
                              }`}>
                                <ServiceIcon 
                                  serviceName={service.serviceName}
                                  size={24}
                                  className={isSelected ? 'text-white' : 'text-gray-700 group-hover:text-indigo-600'}
                                />
                              </div>
                            </div>

                            {/* 服务信息 */}
                            <div className="text-center space-y-2">
                              <h3 className={`text-sm font-semibold line-clamp-1 ${
                                isSelected ? 'text-blue-900' : 'text-gray-900'
                              }`} title={service.serviceName}>
                                {service.serviceName}
                              </h3>
                              
                              {/* 服务描述 */}
                              {service.serviceDesc && (
                                <p className={`text-xs line-clamp-2 leading-relaxed ${
                                  isSelected ? 'text-blue-700/80' : 'text-gray-600'
                                }`} title={service.serviceDesc}>
                                  {service.serviceDesc}
                                </p>
                              )}
                              
                              {/* 选择状态 */}
                              <div className="flex justify-center pt-1">
                                <div className={`w-5 h-5 rounded-md border-2 flex items-center justify-center transition-all ${
                                  isSelected
                                    ? 'border-blue-500 bg-blue-500'
                                    : 'border-gray-300 group-hover:border-indigo-400'
                                }`}>
                                  {isSelected && (
                                    <svg className="w-3 h-3 text-white" fill="currentColor" viewBox="0 0 20 20">
                                      <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                                    </svg>
                                  )}
                                </div>
                              </div>
                            </div>
                          </div>
                        )
                      })}
                    </div>
                  </div>
                )}
              </div>
            </div>
                      
            {/* 底部操作栏 - 统一美观样式 */}
            <div className={DIALOG_STYLES.footer}>
              {/* 装饰性光效 */}
              <div className={DIALOG_STYLES.footerGlow}></div>
              {/* 顶部分割线光效 */}
              <div className={DIALOG_STYLES.footerTopLine}></div>
              
              <div className={DIALOG_STYLES.footerContent}>
                {/* 左侧已选服务信息 */}
                <div className="flex items-center gap-4 flex-1 min-w-0">
                  {selectedServices.length > 0 ? (
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="w-8 h-8 bg-gradient-to-br from-emerald-500 to-emerald-600 rounded-lg flex items-center justify-center shadow-sm">
                        <span className="text-white text-xs font-bold">{selectedServices.length}</span>
                      </div>
                      <div className="min-w-0">
                        <div className="text-sm font-semibold text-gray-900">已选择 {selectedServices.length} 个服务</div>
                        <div className="flex flex-wrap gap-1 mt-1 min-w-0">
                          {selectedServices.slice(0, 3).map((service) => (
                            <span key={service.id} className="inline-flex items-center px-2 py-1 bg-gradient-to-br from-blue-100 to-indigo-100 text-blue-800 text-xs rounded-full border border-blue-200 shrink-0">
                              <span className="truncate max-w-16">{service.serviceName}</span>
                              {service.isRequired && <span className="ml-1 text-red-500 font-bold">*</span>}
                            </span>
                          ))}
                          {selectedServices.length > 3 && (
                            <span className="px-2 py-1 bg-gradient-to-br from-gray-100 to-gray-200 text-gray-600 text-xs rounded-full border border-gray-300 shrink-0">
                              +{selectedServices.length - 3} 个
                            </span>
                          )}
                        </div>
                      </div>
                    </div>
                  ) : (
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 bg-gray-200 rounded-lg flex items-center justify-center">
                        <Package className="w-4 h-4 text-gray-500" />
                      </div>
                      <div className="text-sm text-gray-600">请选择需要安装的服务</div>
                    </div>
                  )}
                </div>

                {/* 右侧按钮 */}
                <div className="flex items-center gap-3">
                  <button
                    onClick={() => {
                      if (onPrevious) {
                        onPrevious()
                      } else {
                        onOpenChange(false)
                      }
                    }}
                    className="flex items-center px-6 py-2.5 border border-gray-300 rounded-xl text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 hover:border-gray-400 transition-all shadow-sm hover:shadow-md"
                  >
                    <ChevronLeft className="w-4 h-4 mr-2" />
                    上一步
                  </button>
                  <button
                    onClick={handleNext}
                    disabled={selectedServiceIds.length === 0}
                    className={`flex items-center px-6 py-2.5 rounded-xl text-sm font-medium transition-all shadow-md hover:shadow-lg ${
                      selectedServiceIds.length === 0
                        ? 'bg-gray-200 text-gray-400 cursor-not-allowed'
                        : 'bg-gradient-to-r from-blue-500 to-indigo-600 text-white hover:from-blue-600 hover:to-indigo-700 shadow-blue-500/30 hover:shadow-blue-500/40'
                    }`}
                  >
                    下一步
                    <ChevronRight className="w-4 h-4 ml-2" />
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

export default ServiceSelectionDialog