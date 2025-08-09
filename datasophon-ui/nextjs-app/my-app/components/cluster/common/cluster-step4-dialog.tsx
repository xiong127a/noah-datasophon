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
      <div className="flex h-full">
        {/* 左侧：服务选择区域 */}
        <div className="flex-1 flex flex-col p-8 min-w-0">
          {/* 顶部过滤区域 */}
          <div className="mb-6">
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
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {SERVICE_TYPE_OPTIONS.map((option) => (
                        <SelectItem key={option.value} value={option.value}>
                          <div className="flex flex-col">
                            <span className="font-medium">{option.label}</span>
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

          {/* 服务网格 */}
          <div className="flex-1 min-h-0">
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
              <div className="h-full overflow-y-auto pr-2" style={{scrollbarWidth: 'thin'}}>
                <div className="grid grid-cols-6 gap-4 pb-4">
                  {filteredServices.map((service) => {
                    const isSelected = selectedServiceIds.includes(service.id)
                    
                    return (
                      <div
                        key={service.id}
                        className={`group cursor-pointer transition-all duration-300 transform hover:scale-105 ${
                          isSelected ? 'ring-2 ring-blue-500 ring-offset-2' : ''
                        }`}
                        onClick={() => handleServiceToggle(service.id)}
                      >
                        <div className={`relative overflow-hidden rounded-2xl transition-all duration-300 ${
                          isSelected
                            ? 'bg-gradient-to-br from-blue-50 to-blue-100 border-2 border-blue-300 shadow-xl shadow-blue-500/20'
                            : 'bg-white/80 backdrop-blur-xl border border-gray-200/60 shadow-lg shadow-black/5 hover:shadow-xl hover:shadow-blue-500/10 hover:border-blue-300/40'
                        }`}>
                          {/* 必需服务标识 */}
                          {service.isRequired && (
                            <div className="absolute top-2 right-2 z-10">
                              <div className="w-3 h-3 bg-gradient-to-br from-red-400 to-red-500 rounded-full shadow-lg shadow-red-500/50"></div>
                            </div>
                          )}
                          
                          {/* 已安装标识 */}
                          {service.installed && (
                            <div className="absolute top-2 left-2 z-10">
                              <div className="w-3 h-3 bg-gradient-to-br from-green-400 to-green-500 rounded-full shadow-lg shadow-green-500/50"></div>
                            </div>
                          )}

                          <div className="p-4">
                            {/* 服务图标 */}
                            <div className="flex justify-center mb-3">
                              <div className={`w-12 h-12 rounded-2xl flex items-center justify-center transition-all duration-300 ${
                                isSelected
                                  ? 'bg-gradient-to-br from-blue-500 to-blue-600 shadow-lg shadow-blue-500/30'
                                  : 'bg-gradient-to-br from-gray-100 to-gray-200 group-hover:from-blue-100 group-hover:to-blue-200'
                              }`}>
                                <ServiceIcon 
                                  serviceName={service.serviceName}
                                  size={24}
                                  className={isSelected ? 'text-white' : 'text-gray-600 group-hover:text-blue-600'}
                                />
                              </div>
                            </div>

                            {/* 服务信息 */}
                            <div className="text-center">
                              <h3 className={`text-sm font-bold mb-1 transition-colors duration-200 ${
                                isSelected ? 'text-blue-700' : 'text-gray-900 group-hover:text-blue-600'
                              }`}>
                                {service.serviceName}
                              </h3>
                              
                              {service.serviceDesc && (
                                <p className="text-xs text-gray-600 line-clamp-2 leading-relaxed">
                                  {service.serviceDesc}
                                </p>
                              )}
                            </div>

                            {/* 选择复选框 */}
                            <div className="flex justify-center mt-3">
                              <Checkbox
                                checked={isSelected}
                                onCheckedChange={() => {}} // 由父级div处理点击
                                className={`w-4 h-4 transition-all duration-200 ${
                                  isSelected
                                    ? 'border-blue-500 bg-blue-500'
                                    : 'border-gray-300 group-hover:border-blue-400'
                                }`}
                              />
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

        {/* 右侧：统计和已选服务面板 */}
        <div className="w-80 flex-shrink-0 p-8 space-y-6">
          {/* 统计面板 */}
          <div className="bg-white/70 backdrop-blur-xl rounded-2xl shadow-lg shadow-black/5 border border-white/20 p-4">
            <div className="flex items-center space-x-2 mb-4">
              <div className="w-8 h-8 bg-gradient-to-br from-purple-500 to-purple-600 rounded-xl flex items-center justify-center shadow-lg shadow-purple-500/25">
                <AlertCircle className="w-4 h-4 text-white" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-gray-900">统计</h3>
              </div>
            </div>
            
            <div className="space-y-3">
              <div className="bg-gradient-to-br from-blue-50/80 to-blue-100/60 rounded-xl p-3 border border-blue-200/30">
                <div className="text-xs font-medium text-blue-600 mb-1">总数</div>
                <div className="text-xl font-bold text-blue-900">{stats.total}</div>
              </div>
              <div className="bg-gradient-to-br from-green-50/80 to-green-100/60 rounded-xl p-3 border border-green-200/30">
                <div className="text-xs font-medium text-green-600 mb-1">已选</div>
                <div className="text-xl font-bold text-green-900">{stats.selected}</div>
              </div>
              <div className="bg-gradient-to-br from-orange-50/80 to-orange-100/60 rounded-xl p-3 border border-green-200/30">
                <div className="text-xs font-medium text-orange-600 mb-1">必需</div>
                <div className="text-xl font-bold text-orange-900">{stats.required}</div>
              </div>
            </div>
          </div>
          
          {/* 紧凑已选择服务列表 */}
          {selectedServices.length > 0 && (
            <div className="bg-white/70 backdrop-blur-xl rounded-2xl shadow-lg shadow-black/5 border border-white/20 p-4">
              <div className="flex items-center space-x-2 mb-3">
                <div className="w-6 h-6 bg-gradient-to-br from-blue-500 to-blue-600 rounded-lg flex items-center justify-center shadow-lg shadow-blue-500/25">
                  <span className="text-white font-bold text-xs">{stats.selected}</span>
                </div>
                <h4 className="text-sm font-bold text-gray-900">已选服务</h4>
              </div>
              
              <div className="space-y-2 max-h-48 overflow-y-auto pr-1" style={{scrollbarWidth: 'thin'}}>
                {selectedServices.map((service) => (
                  <div 
                    key={service.id} 
                    className="flex items-center space-x-2 p-2 bg-gradient-to-r from-blue-50/80 to-blue-100/50 rounded-lg border border-blue-200/30 transition-all duration-200"
                  >
                    <ServiceIcon 
                      serviceName={service.serviceName}
                      size={16}
                      className="flex-shrink-0"
                    />
                    <div className="flex-1 min-w-0">
                      <div className="text-xs font-semibold text-gray-900 truncate">
                        {service.serviceName}
                      </div>
                    </div>
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

export default ClusterStep3Dialog
