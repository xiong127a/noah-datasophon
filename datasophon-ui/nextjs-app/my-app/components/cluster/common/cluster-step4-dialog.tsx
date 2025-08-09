"use client"

import React, { useState, useEffect, useMemo } from 'react'
import { 
  Loader2, RefreshCw, AlertCircle, Package, Search
} from 'lucide-react'
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Checkbox } from "@/components/ui/checkbox"
import ServiceIcon from "@/components/ui/service-icon"
import { clusterApiV1 } from "@/lib/api-utils-v1"
import { toast } from 'sonner'
import ClusterStepLayout from './cluster-step-layout'
import ClusterStepActionBar, { type ActionButton, type StatusInfo } from './cluster-step-action-bar'
import { createClusterHeaders } from '@/lib/cluster-id-header'

import { SERVICE_TYPE_OPTIONS, ServiceType } from '@/types/step3'
import type { 
  ClusterStep3DialogProps, 
  Service, 
  ServiceSelection,
  Step3Data
} from '@/types/step3'

/**
 * 集群步骤3：大数据服务选择对话框
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 */

const ClusterStep3Dialog: React.FC<ClusterStep3DialogProps> = ({
  open,
  onOpenChange,
  cluster,
  clusterType = '',
  onComplete,
  onPrevious
}) => {
  // 状态管理
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [services, setServices] = useState<Service[]>([])
  const [selectedServiceIds, setSelectedServiceIds] = useState<number[]>([])
  const [serviceTypeFilter, setServiceTypeFilter] = useState<ServiceType>(ServiceType.MINIMAL)
  const [searchTerm, setSearchTerm] = useState('')

  // 获取服务列表
  const fetchServices = async () => {
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
        
        // 根据服务类型自动选择服务
        if (serviceTypeFilter === ServiceType.MINIMAL) {
          const requiredServices = response.data.data.filter((service: Service) => service.isRequired)
          setSelectedServiceIds(requiredServices.map((service: Service) => service.id))
        }
      } else {
        throw new Error(response.data?.message || '获取服务列表失败')
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '未知错误'
      setError(errorMessage)
      toast.error(`获取服务列表失败: ${errorMessage}`)
    } finally {
      setLoading(false)
    }
  }

  // 初始化和服务类型变化时获取服务
  useEffect(() => {
    if (open) {
      fetchServices()
    }
  }, [open, serviceTypeFilter, cluster?.id])

  // 过滤服务
  const filteredServices = useMemo(() => {
    return services.filter(service => {
      // 搜索过滤
      const matchesSearch = searchTerm === '' || 
        service.serviceName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        service.serviceDesc?.toLowerCase().includes(searchTerm.toLowerCase())

      return matchesSearch
    })
  }, [services, searchTerm])

  // 已选择的服务
  const selectedServices = useMemo(() => {
    return services.filter(service => selectedServiceIds.includes(service.id))
  }, [services, selectedServiceIds])

  // 服务切换处理
  const handleServiceToggle = (serviceId: number) => {
    setSelectedServiceIds(prev => {
      if (prev.includes(serviceId)) {
        return prev.filter(id => id !== serviceId)
      } else {
        return [...prev, serviceId]
      }
    })
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
    const available = filteredServices.filter(s => !s.installed).length

    return { total, selected, required, available }
  }, [filteredServices, selectedServiceIds])

  // 计算当前步骤编号（K8s模式跳过Agent分发）
  const isK8s = clusterType?.toLowerCase() === 'kubernetes'
  const currentStepNumber = isK8s ? 3 : 4

  // 状态信息配置
  const statusInfo: StatusInfo = {
    text: "已选择",
    value: stats.selected,
    total: "个服务",
    pulse: true
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
      onClick: handleNext,
      disabled: selectedServiceIds.length === 0,
      variant: "primary"
    }
  ]

  return (
    <ClusterStepLayout
      open={open}
      onClose={() => onOpenChange(false)}
      clusterName={cluster?.clusterName || ''}
      clusterType={clusterType}
      stepTitle="选择大数据服务"
      stepDescription="根据您的需求选择要部署的大数据服务组件"
      currentStep={currentStepNumber}
      dialogTitle={`服务选择 - ${cluster?.clusterName}`}
      actionBar={
        <ClusterStepActionBar
          statusInfo={statusInfo}
          statusBadge={{
            text: `包含 ${stats.required} 个必需服务`,
            variant: "warning",
            show: stats.required > 0
          }}
          buttons={buttons}
        />
      }
    >
      <div className="flex-1 overflow-hidden flex flex-col">
        {/* 顶部过滤区域 */}
        <div className="flex-shrink-0 p-6 sm:p-8 pb-4">
          <div className="flex items-center space-x-6 bg-white/70 backdrop-blur-xl rounded-2xl p-6 shadow-lg shadow-black/5 border border-white/20">
              {/* 服务类型选择 */}
              <div className="flex items-center space-x-3">
                <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-blue-600 rounded-xl flex items-center justify-center shadow-lg shadow-blue-500/25">
                  <Package className="w-4 h-4 text-white" />
                </div>
                <div>
                  <div className="text-sm font-bold text-gray-900 mb-1">服务类型</div>
                  <Select value={serviceTypeFilter} onValueChange={(value: string) => setServiceTypeFilter(value as ServiceType)}>
                    <SelectTrigger className="w-[200px] bg-white/80 border border-gray-200/60 rounded-xl shadow-sm">
                      <SelectValue>
                        {SERVICE_TYPE_OPTIONS.find(option => option.value === serviceTypeFilter)?.label}
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      {SERVICE_TYPE_OPTIONS.map((option) => (
                        <SelectItem key={option.value} value={option.value}>
                          <div className="flex flex-col py-1">
                            <span className="font-medium text-gray-900">{option.label}</span>
                            <span className="text-xs text-gray-500 mt-0.5">{option.description}</span>
                          </div>
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              {/* 搜索框 */}
              <div className="flex items-center space-x-3 flex-1">
                <div className="w-8 h-8 bg-gradient-to-br from-green-500 to-green-600 rounded-xl flex items-center justify-center shadow-lg shadow-green-500/25">
                  <Search className="w-4 h-4 text-white" />
                </div>
                <div className="flex-1">
                  <div className="text-sm font-bold text-gray-900 mb-1">搜索服务</div>
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
                    <Input
                      type="text"
                      placeholder="输入服务名称进行搜索..."
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      className="pl-10 bg-white/80 border border-gray-200/60 rounded-xl shadow-sm focus:shadow-md transition-shadow"
                    />
                  </div>
                </div>
              </div>

              {/* 刷新按钮 */}
              <div className="flex items-center">
                <button
                  onClick={fetchServices}
                  disabled={loading}
                  className="w-10 h-10 bg-gradient-to-br from-purple-500 to-purple-600 rounded-xl flex items-center justify-center shadow-lg shadow-purple-500/25 text-white hover:shadow-xl hover:shadow-purple-500/30 transition-all duration-200 disabled:opacity-50"
                >
                  <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
                </button>
              </div>
            </div>
        </div>

        {/* 主要内容区域 */}
        <div className="flex-1 flex gap-6 min-h-0 px-6 sm:px-8">
          {/* 服务选择区域 */}
          <div className="flex-1 flex flex-col min-w-0">
            {loading ? (
              <div className="flex items-center justify-center h-64">
                <div className="flex items-center space-x-3">
                  <Loader2 className="w-6 h-6 animate-spin text-blue-500" />
                  <span className="text-gray-600 font-medium">正在加载服务列表...</span>
                </div>
              </div>
            ) : error ? (
              <div className="flex flex-col items-center justify-center h-64 text-red-500">
                <AlertCircle className="w-12 h-12 mb-4" />
                <p className="text-lg font-semibold mb-2">加载失败</p>
                <p className="text-gray-600">{error}</p>
                <button 
                  onClick={fetchServices}
                  className="mt-4 px-6 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors"
                >
                  重试
                </button>
              </div>
            ) : filteredServices.length === 0 ? (
              <div className="flex flex-col items-center justify-center h-64 text-gray-500">
                <Package className="w-12 h-12 mb-4" />
                <p className="text-lg font-semibold">暂无服务</p>
                <p>没有找到符合条件的服务</p>
              </div>
            ) : (
              <div className="h-full overflow-y-auto scrollbar-thin scrollbar-thumb-gray-300 scrollbar-track-gray-100 pt-2 pb-6">
                <div className="grid grid-cols-4 gap-6 px-2">
                  {filteredServices.map((service) => {
                    const isSelected = selectedServiceIds.includes(service.id)
                    
                    return (
                      <div
                        key={service.id}
                        className="group cursor-pointer transition-all duration-300"
                        onClick={() => handleServiceToggle(service.id)}
                      >
                        <div className={`relative overflow-hidden rounded-2xl transition-all duration-300 h-full transform ${
                          isSelected
                            ? 'bg-gradient-to-br from-blue-500/15 via-blue-400/10 to-blue-600/15 border-2 border-blue-500 shadow-2xl shadow-blue-500/30 scale-105'
                            : 'bg-white/90 backdrop-blur-xl border border-gray-200/60 shadow-lg shadow-black/5 hover:shadow-xl hover:shadow-blue-500/10 hover:border-blue-300/40 hover:scale-[1.02]'
                        }`}>
                          {/* 必需服务标识 */}
                          {service.isRequired && (
                            <div className="absolute top-2 right-2 z-10">
                              <div className="bg-gradient-to-r from-red-500 to-red-600 text-white text-xs font-bold px-2 py-1 rounded-full shadow-lg shadow-red-500/40 border border-red-400">
                                必需
                              </div>
                            </div>
                          )}
                          
                          {/* 已安装标识 */}
                          {service.installed && (
                            <div className="absolute top-2 left-2 z-10">
                              <div className="bg-gradient-to-r from-green-500 to-green-600 text-white text-xs font-bold px-2 py-1 rounded-full shadow-lg shadow-green-500/40 border border-green-400">
                                已装
                              </div>
                            </div>
                          )}

                          <div className="p-4 h-full flex flex-col">
                            {/* 服务图标 */}
                            <div className="flex justify-center mb-3">
                              <div className={`w-14 h-14 rounded-2xl flex items-center justify-center transition-all duration-300 ${
                                isSelected
                                  ? 'bg-gradient-to-br from-blue-500 to-blue-600 shadow-xl shadow-blue-500/40'
                                  : 'bg-gradient-to-br from-gray-100 to-gray-200 group-hover:from-blue-100 group-hover:to-blue-200'
                              }`}>
                                <ServiceIcon 
                                  serviceName={service.serviceName}
                                  size={28}
                                  className={isSelected ? 'text-white' : 'text-gray-600 group-hover:text-blue-600'}
                                />
                              </div>
                            </div>

                            {/* 服务信息 */}
                            <div className="text-center flex-1 flex flex-col justify-between">
                              <div>
                                <h3 className={`text-sm font-bold mb-1 transition-colors duration-200 ${
                                  isSelected ? 'text-blue-800' : 'text-gray-900 group-hover:text-blue-700'
                                }`}>
                                  {service.serviceName}
                                </h3>
                                
                                {service.serviceDesc && (
                                  <p className={`text-xs line-clamp-2 leading-relaxed mb-3 transition-colors duration-200 ${
                                    isSelected ? 'text-blue-600/80' : 'text-gray-600'
                                  }`}>
                                    {service.serviceDesc}
                                  </p>
                                )}
                                
                                {/* 服务状态标签 */}
                                <div className="flex justify-center gap-1 mb-2">
                                  {service.isRequired && (
                                    <div className="text-xs text-red-600 font-medium bg-red-50 px-2 py-0.5 rounded-full border border-red-200">
                                      核心
                                    </div>
                                  )}
                                  {service.installed && (
                                    <div className="text-xs text-green-600 font-medium bg-green-50 px-2 py-0.5 rounded-full border border-green-200">
                                      已装
                                    </div>
                                  )}
                                </div>
                              </div>

                              {/* 选择复选框 */}
                              <div className="flex justify-center">
                                <Checkbox
                                  checked={isSelected}
                                  onCheckedChange={() => {}} // 由父级div处理点击
                                  className={`w-5 h-5 transition-all duration-200 ${
                                    isSelected
                                      ? 'border-blue-500 bg-blue-500'
                                      : 'border-gray-300 group-hover:border-blue-400'
                                  }`}
                                />
                              </div>
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

          {/* 右侧：统计和已选服务面板 */}
          <div className="w-80 flex-shrink-0 space-y-4">
            {/* 统计面板 */}
              <div className="bg-gradient-to-br from-white/90 to-gray-50/80 backdrop-blur-xl rounded-2xl shadow-xl shadow-black/10 border border-white/40 p-5">
                <div className="flex items-center space-x-3 mb-5">
                  <div className="w-10 h-10 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-xl flex items-center justify-center shadow-lg shadow-indigo-500/30">
                    <Package className="w-5 h-5 text-white" />
                  </div>
                  <div>
                    <h3 className="text-base font-bold text-gray-900">服务统计</h3>
                    <p className="text-xs text-gray-600">当前选择状态</p>
                  </div>
                </div>
                
                <div className="space-y-3">
                  <div className="bg-gradient-to-r from-blue-500/10 to-blue-600/10 rounded-xl p-4 border border-blue-200/50 backdrop-blur-sm">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center space-x-2">
                        <div className="w-6 h-6 bg-blue-500 rounded-lg flex items-center justify-center">
                          <Package className="w-3 h-3 text-white" />
                        </div>
                        <span className="text-sm font-medium text-blue-800">总数</span>
                      </div>
                      <div className="text-xl font-bold text-blue-900">{stats.total}</div>
                    </div>
                  </div>
                  
                  <div className="bg-gradient-to-r from-green-500/10 to-emerald-600/10 rounded-xl p-4 border border-green-200/50 backdrop-blur-sm">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center space-x-2">
                        <div className="w-6 h-6 bg-green-500 rounded-lg flex items-center justify-center">
                          <Checkbox className="w-3 h-3 text-white" />
                        </div>
                        <span className="text-sm font-medium text-green-800">已选</span>
                      </div>
                      <div className="text-xl font-bold text-green-900">{stats.selected}</div>
                    </div>
                  </div>
                  
                  <div className="bg-gradient-to-r from-red-500/10 to-red-600/10 rounded-xl p-4 border border-red-200/50 backdrop-blur-sm">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center space-x-2">
                        <div className="w-6 h-6 bg-red-500 rounded-lg flex items-center justify-center">
                          <AlertCircle className="w-3 h-3 text-white" />
                        </div>
                        <span className="text-sm font-medium text-red-800">必需</span>
                      </div>
                      <div className="text-xl font-bold text-red-900">{stats.required}</div>
                    </div>
                    <div className="mt-2 pt-2 border-t border-red-200/50">
                      <p className="text-xs text-red-600">
                        核心服务，不可取消选择
                      </p>
                    </div>
                  </div>
                </div>
              </div>
              
              {/* 已选择服务列表 */}
              {selectedServices.length > 0 && (
                <div className="bg-gradient-to-br from-white/90 to-blue-50/50 backdrop-blur-xl rounded-2xl shadow-xl shadow-black/10 border border-white/40 p-4 flex-1 min-h-0">
                  <div className="flex items-center space-x-3 mb-4">
                    <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-blue-600 rounded-xl flex items-center justify-center shadow-lg shadow-blue-500/30">
                      <span className="text-white font-bold text-sm">{stats.selected}</span>
                    </div>
                    <div>
                      <h4 className="text-sm font-bold text-gray-900">已选服务</h4>
                      <p className="text-xs text-gray-600">当前选择的服务</p>
                    </div>
                  </div>
                  
                  <div className="space-y-2 max-h-[calc(100%-3rem)] overflow-y-auto scrollbar-thin scrollbar-thumb-blue-300/50 scrollbar-track-blue-100/30">
                    {selectedServices.map((service) => (
                      <div 
                        key={service.id} 
                        className="group flex items-center space-x-3 p-3 bg-gradient-to-r from-white/80 to-blue-50/60 rounded-xl border border-blue-200/40 shadow-sm hover:shadow-md transition-all duration-200 hover:scale-[1.02]"
                      >
                        <div className="w-8 h-8 bg-gradient-to-br from-blue-100 to-blue-200 rounded-lg flex items-center justify-center group-hover:from-blue-200 group-hover:to-blue-300 transition-all duration-200">
                          <ServiceIcon 
                            serviceName={service.serviceName}
                            size={18}
                            className="text-blue-600 group-hover:text-blue-700"
                          />
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="text-sm font-semibold text-gray-900 truncate group-hover:text-blue-800 transition-colors duration-200">
                            {service.serviceName}
                          </div>
                          <div className="flex items-center gap-1 mt-1">
                            {service.isRequired && (
                              <div className="text-xs text-red-600 font-medium bg-red-50 px-1.5 py-0.5 rounded border border-red-200">
                                必需
                              </div>
                            )}
                            {service.installed && (
                              <div className="text-xs text-green-600 font-medium bg-green-50 px-1.5 py-0.5 rounded border border-green-200">
                                已装
                              </div>
                            )}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
          </div>
        </div>
      </div>
    </ClusterStepLayout>
  )
}

export default ClusterStep3Dialog
