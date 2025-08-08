"use client"

import React, { useState, useEffect, useMemo } from 'react'
import { 
  ChevronLeft, ChevronRight, Loader2, RefreshCw,
  AlertCircle, Package, Search
} from 'lucide-react'
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Checkbox } from "@/components/ui/checkbox"
import ServiceIcon from "@/components/ui/service-icon"
import { clusterApi } from "@/lib/api"
import { toast } from 'sonner'
import ClusterWizardSidebar from './cluster-wizard-sidebar'
import { getStepsByType, StepsType } from '@/lib/cluster-steps'
import { createClusterHeaders } from '@/lib/cluster-id-header'
import { ClusterTypeUtil } from '@/types'

import { SERVICE_TYPE_OPTIONS, ServiceType } from '@/types/step3'
import type { 
  ClusterStep3DialogProps, 
  Service, 
  ServiceSelection,
  Step3Data
} from '@/types/step3'

/**
 * 集群步骤3：大数据服务选择对话框
 * 用于K8S模式下选择需要安装的大数据服务
 * Apple风格设计
 * 
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 * 日期：2024-01-20
 */
const ClusterStep3Dialog: React.FC<ClusterStep3DialogProps> = ({
  open,
  onOpenChange,
  cluster,
  onSuccess,
  onPrevious
}) => {
  const clusterType = ClusterTypeUtil.fromString(cluster?.depType || 'PVM')
  
  // 使用标准化的步骤配置
  const steps = getStepsByType(StepsType.NORMAL, clusterType)
  
  // 基础状态
  const [loading, setLoading] = useState(false)
  const [services, setServices] = useState<Service[]>([])
  const [selectedServiceIds, setSelectedServiceIds] = useState<number[]>([])
  const [selectedServices, setSelectedServices] = useState<ServiceSelection[]>([])
  const [serviceTypeFilter, setServiceTypeFilter] = useState<ServiceType>(ServiceType.MINIMAL)
  const [searchTerm, setSearchTerm] = useState('')

  // 集群ID
  const clusterId = cluster?.id

  // 搜索筛选后的服务列表（显示所有服务）
  const filteredServices = useMemo(() => {
    return services.filter(service => {
      // 只进行搜索筛选，显示所有服务
      return searchTerm === '' || 
        service.label?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        service.serviceName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        service.serviceDesc?.toLowerCase().includes(searchTerm.toLowerCase())
    })
  }, [services, searchTerm])

  // 获取服务列表
  const fetchServiceList = async (showLoading = true) => {
    if (showLoading) setLoading(true)

    try {
      if (!clusterId) {
        throw new Error('集群ID不能为空')
      }

      // 构建请求参数（集群ID通过请求头传递，服务类型用于设置必需标识）
      const params = {
        type: serviceTypeFilter
      }
      
      // 创建包含集群ID的请求头
      const headers = createClusterHeaders(clusterId)
      
      // 调用服务列表API
      const response = await clusterApi.service.listWithRequired(params, { headers })
      
      if (response.data && response.data.code === 200) {
        const serviceData = response.data.data || []
        setServices(serviceData)
        
        // 设置服务数据，自动勾选逻辑由 useEffect 处理
      } else {
        console.error('获取服务列表失败:', response.data?.msg || '未知错误')
        toast.error('获取服务列表失败')
        setServices([])
      }
    } catch (error) {
      console.error('请求服务列表失败:', error)
      toast.error('获取服务列表失败')
      setServices([])
    } finally {
      setLoading(false)
    }
  }

  // 处理服务选择
  const handleServiceSelection = (serviceId: number, selected: boolean) => {
    const service = services.find(s => s.id === serviceId)
    if (!service) return

    if (selected) {
      setSelectedServiceIds(prev => [...new Set([...prev, serviceId])])
      setSelectedServices(prev => {
        const exists = prev.some(s => s.serviceId === serviceId)
        if (!exists) {
          return [...prev, {
            serviceId: service.id,
            serviceName: service.serviceName
          }]
        }
        return prev
      })
    } else {
      setSelectedServiceIds(prev => prev.filter(id => id !== serviceId))
      setSelectedServices(prev => prev.filter(s => s.serviceId !== serviceId))
    }
  }

  // 全选功能
  const handleSelectAll = () => {
    const selectableServices = filteredServices.filter(service => 
      ClusterTypeUtil.isKubernetes(clusterType) ? true : !service.installed
    )
    
    const allSelected = selectableServices.every(s => selectedServiceIds.includes(s.id))
    
    if (allSelected) {
      // 取消全选
      const idsToRemove = selectableServices.map(s => s.id)
      setSelectedServiceIds(prev => prev.filter(id => !idsToRemove.includes(id)))
      setSelectedServices(prev => prev.filter(s => !idsToRemove.includes(s.serviceId)))
    } else {
      // 全选
      const newIds = selectableServices.map(s => s.id)
      const newSelections = selectableServices.map(s => ({
        serviceId: s.id,
        serviceName: s.serviceName
      }))
      
      setSelectedServiceIds(prev => [...new Set([...prev, ...newIds])])
      setSelectedServices(prev => {
        const combined = [...prev, ...newSelections]
        // 去重
        const unique = combined.filter((item, index, self) => 
          index === self.findIndex(t => t.serviceId === item.serviceId)
        )
        return unique
      })
    }
  }

  // 服务类型选项（只有核心和自定义两个选项）
  const serviceTypeOptions = useMemo(() => SERVICE_TYPE_OPTIONS, [])

  // 下一步处理
  const handleNext = async () => {
    if (selectedServiceIds.length === 0) {
      toast.warning('请至少选择一个服务')
      return
    }

    try {
      const step3Data: Step3Data = {
        serviceIds: selectedServiceIds,
        serviceNames: selectedServices,
        serviceType: serviceTypeFilter
      }

      toast.success('服务选择完成，进入下一步')
      
      // 调用成功回调
      onSuccess?.(step3Data)
      
    } catch (error) {
      console.error('处理下一步失败:', error)
      toast.error('处理失败，请重试')
    }
  }

  // 根据服务类型自动勾选对应服务
  useEffect(() => {
    if (services.length > 0) {
      // 根据选择的服务类型自动勾选对应服务
      const requiredServices = services.filter(service => {
        if (serviceTypeFilter === ServiceType.MINIMAL) {
          // 最小化：选择基础必需服务（监控和核心大数据组件）
          return ['PROMETHEUS', 'GRAFANA', 'ZOOKEEPER', 'HDFS', 'YARN'].includes(service.serviceName)
        } else if (serviceTypeFilter === ServiceType.CUSTOM) {
          // 自定义：选择基础监控服务
          return ['PROMETHEUS', 'GRAFANA'].includes(service.serviceName)
        }
        return false
      })
      
      if (requiredServices.length > 0) {
        const requiredIds = requiredServices.map(service => service.id)
        const requiredSelections = requiredServices.map(service => ({
          serviceId: service.id,
          serviceName: service.serviceName
        }))
        
        setSelectedServiceIds(requiredIds)
        setSelectedServices(requiredSelections)
      } else {
        // 如果没有匹配的必需服务，清空选择
        setSelectedServiceIds([])
        setSelectedServices([])
      }
    }
  }, [services, serviceTypeFilter])

  // 组件挂载时获取服务列表
  useEffect(() => {
    if (open && clusterId) {
      fetchServiceList()
    }
  }, [open, clusterId, serviceTypeFilter]) // eslint-disable-line react-hooks/exhaustive-deps

  // 计算统计信息
  const stats = useMemo(() => {
    const total = filteredServices.length
    const selected = selectedServiceIds.length
    const required = filteredServices.filter(s => s.isRequired).length
    const available = filteredServices.filter(s => !s.installed).length

    return { total, selected, required, available }
  }, [filteredServices, selectedServiceIds])

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="!max-w-none !w-[min(calc(100vw-32px),1900px)] !max-h-[calc(100vh-32px)] sm:!w-[min(98vw,1900px)] sm:!max-h-[calc(98vh-32px)] border-0 shadow-2xl bg-white rounded-3xl !fixed !top-1/2 !left-1/2 !-translate-x-1/2 !-translate-y-1/2 !m-0 [&>button]:hidden flex flex-col p-0 gap-0">
        <DialogTitle className="sr-only">
          服务选择 - {cluster?.clusterName}
        </DialogTitle>
        
        <div className="flex h-full max-h-[calc(100vh-32px)] sm:max-h-[calc(98vh-32px)]">
          {/* 左侧导航 */}
          <ClusterWizardSidebar 
            steps={steps}
            currentStep={3}
            title="服务选择"
            clusterName={cluster?.clusterName || ''}
            isK8s={ClusterTypeUtil.isKubernetes(clusterType)}
            onClose={() => onOpenChange(false)}
          />

          {/* 主要内容区域 - Apple风格设计 */}
          <div className="flex-1 flex flex-col min-h-0">
            {/* 渐变背景 */}
            <div className="absolute inset-0 bg-gradient-to-br from-blue-50/30 via-white/10 to-purple-50/20"></div>
            
            <div className="relative flex-1 p-10 overflow-hidden">
              <div className="h-full">
                <div className="grid grid-cols-6 gap-8 h-full">
                  {/* 左侧服务列表 - Apple卡片设计 */}
                  <div className="col-span-5 flex flex-col min-h-0">
                    <div className="flex-1 flex flex-col min-h-0 bg-white/70 backdrop-blur-xl rounded-3xl shadow-2xl shadow-black/5 border border-white/20">
                      {/* 头部区域 */}
                      <div className="p-8 pb-6 border-b border-gray-100/50">
                        <div className="flex items-center justify-between mb-6">
                          <div className="flex items-center space-x-4">
                            <div className="w-12 h-12 bg-gradient-to-br from-blue-500 to-blue-600 rounded-2xl flex items-center justify-center shadow-lg shadow-blue-500/25">
                              <Package className="w-6 h-6 text-white" />
                            </div>
                            <div>
                              <h2 className="text-2xl font-bold bg-gradient-to-r from-gray-900 to-gray-600 bg-clip-text text-transparent">
                                大数据服务
                              </h2>
                              {stats.total > 0 && (
                                <p className="text-sm text-gray-500 mt-1">
                                  共 {stats.total} 个可用服务
                                </p>
                              )}
                            </div>
                          </div>
                          
                          <Button 
                            onClick={() => fetchServiceList(true)}
                            disabled={loading}
                            className="bg-white/80 hover:bg-white/90 text-gray-700 border-0 shadow-lg shadow-black/5 rounded-2xl px-6 py-3 transition-all duration-300 hover:shadow-xl hover:scale-105"
                          >
                            {loading ? (
                              <>
                                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                                刷新中...
                              </>
                            ) : (
                              <>
                                <RefreshCw className="w-4 h-4 mr-2" />
                                刷新
                              </>
                            )}
                          </Button>
                        </div>
                        
                        {/* Apple风格搜索和筛选栏 */}
                        <div className="flex items-center space-x-6">
                          <div className="relative flex-1 max-w-lg">
                            <Search className="absolute left-4 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
                            <Input
                              type="text"
                              placeholder="搜索服务名称或描述..."
                              className="pl-12 pr-4 py-4 bg-gray-50/80 border-0 rounded-2xl text-base focus:bg-white/90 focus:shadow-lg focus:ring-2 focus:ring-blue-500/20 transition-all duration-300"
                              value={searchTerm}
                              onChange={(e) => setSearchTerm(e.target.value)}
                            />
                          </div>
                          
                          <Select value={serviceTypeFilter} onValueChange={(value) => setServiceTypeFilter(value as ServiceType)}>
                            <SelectTrigger className="w-56 h-12 bg-gray-50/80 border-0 rounded-2xl focus:bg-white/90 focus:shadow-lg focus:ring-2 focus:ring-blue-500/20 transition-all duration-300">
                              <SelectValue placeholder="选择服务类型">
                                {/* 自定义显示内容，只显示标签，不显示描述 */}
                                {serviceTypeFilter && (
                                  <span className="font-semibold text-gray-900">
                                    {serviceTypeOptions.find(option => option.value === serviceTypeFilter)?.label}
                                  </span>
                                )}
                              </SelectValue>
                            </SelectTrigger>
                            <SelectContent className="bg-white/95 backdrop-blur-xl border-0 rounded-2xl shadow-2xl shadow-black/10">
                              {serviceTypeOptions.map((option) => (
                                <SelectItem 
                                  key={option.value} 
                                  value={option.value}
                                  className="rounded-xl p-4 focus:bg-blue-50/80"
                                >
                                  <div>
                                    <div className="font-semibold text-gray-900">{option.label}</div>
                                    {option.description && (
                                      <div className="text-sm text-gray-500 mt-1">{option.description}</div>
                                    )}
                                  </div>
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>

                          <Button
                            onClick={handleSelectAll}
                            disabled={filteredServices.length === 0}
                            className="h-12 px-6 bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 text-white border-0 rounded-2xl shadow-lg shadow-blue-500/25 transition-all duration-300 hover:shadow-xl hover:shadow-blue-500/30 hover:scale-105 disabled:opacity-50 disabled:scale-100"
                          >
                            {filteredServices.every(s => selectedServiceIds.includes(s.id)) ? '取消全选' : '全选'}
                          </Button>
                        </div>
                      </div>

                      {/* 服务列表内容区域 */}
                      <div className="pt-0 flex-1 flex flex-col min-h-0 p-8">
                        {loading ? (
                          <div className="flex items-center justify-center h-96">
                            <div className="flex flex-col items-center space-y-4">
                              <div className="relative">
                                <div className="w-16 h-16 bg-gradient-to-br from-blue-500 to-blue-600 rounded-3xl flex items-center justify-center">
                                  <Loader2 className="w-8 h-8 text-white animate-spin" />
                                </div>
                              </div>
                              <p className="text-gray-600 font-medium">正在加载服务列表...</p>
                            </div>
                          </div>
                        ) : filteredServices.length === 0 ? (
                          <div className="flex flex-col items-center justify-center h-96 text-gray-500">
                            <div className="w-24 h-24 bg-gradient-to-br from-gray-100 to-gray-200 rounded-3xl flex items-center justify-center mb-6">
                              <Package className="w-12 h-12 text-gray-400" />
                            </div>
                            <h3 className="text-xl font-semibold text-gray-700 mb-2">暂无可用服务</h3>
                            <p className="text-gray-500">请尝试调整筛选条件或检查服务配置</p>
                          </div>
                        ) : (
                          <div className="flex-1 overflow-y-auto pr-2" style={{scrollbarWidth: 'thin'}}>
                            <div className="grid grid-cols-6 gap-3">
                              {filteredServices.map((service) => (
                                <div
                                  key={service.id}
                                  className={`group relative rounded-xl p-4 transition-all duration-300 cursor-pointer transform hover:scale-[1.02] ${
                                    selectedServiceIds.includes(service.id)
                                      ? 'bg-gradient-to-r from-blue-50/90 to-blue-100/60 shadow-lg shadow-blue-500/10 border-2 border-blue-200/50'
                                      : 'bg-white/60 hover:bg-white/80 shadow-md hover:shadow-lg shadow-black/5 border border-gray-100/50 hover:border-gray-200/50'
                                  }`}
                                  onClick={() => handleServiceSelection(service.id, !selectedServiceIds.includes(service.id))}
                                  title={service.serviceDesc || service.serviceName}
                                >
                                  {/* 选择状态指示器 */}
                                  <div className="absolute top-2 right-2">
                                    <Checkbox
                                      checked={selectedServiceIds.includes(service.id)}
                                      onCheckedChange={(checked) => 
                                        handleServiceSelection(service.id, checked === true)
                                      }
                                      className="w-4 h-4 rounded-md"
                                    />
                                  </div>
                                  
                                  {/* 服务图标和信息 */}
                                  <div className="flex flex-col items-center text-center space-y-3">
                                    <ServiceIcon 
                                      serviceName={service.serviceName}
                                      size={40}
                                      className="opacity-80 group-hover:opacity-100 transition-opacity duration-300"
                                    />
                                    
                                    <div className="flex-1 min-w-0">
                                      <h4 className="text-sm font-bold text-gray-900 truncate mb-1">
                                        {service.label || service.serviceName}
                                      </h4>
                                      
                                      {/* 状态标识 */}
                                      <div className="flex flex-wrap items-center justify-center gap-1 text-xs">
                                        {service.isRequired && (
                                          <span className="px-2 py-0.5 bg-gradient-to-r from-orange-400 to-orange-500 text-white font-semibold rounded-full">
                                            必需
                                          </span>
                                        )}
                                        {service.installed && (
                                          <span className="px-2 py-0.5 bg-gradient-to-r from-green-400 to-green-500 text-white font-semibold rounded-full">
                                            已安装
                                          </span>
                                        )}
                                        {service.serviceVersion && (
                                          <span className="px-2 py-0.5 bg-gradient-to-r from-gray-400 to-gray-500 text-white font-mono font-bold rounded-full">
                                            v{service.serviceVersion}
                                          </span>
                                        )}
                                      </div>
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

                  {/* 右侧紧凑统计面板 */}
                  <div className="col-span-1 flex flex-col space-y-4">
                    {/* 紧凑统计卡片 */}
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
                        <div className="bg-gradient-to-br from-orange-50/80 to-orange-100/60 rounded-xl p-3 border border-orange-200/30">
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
                              key={service.serviceId} 
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
              </div>
            </div>

            {/* Apple风格底部操作栏 */}
            <div className="relative">
              {/* 渐变分隔线 */}
              <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-gray-200/60 to-transparent"></div>
              
              <div className="bg-white/90 backdrop-blur-xl border-t border-white/20 p-8 shadow-2xl shadow-black/5">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-6">
                    <div className="flex items-center space-x-4">
                      <div className="relative">
                        <div className="w-4 h-4 rounded-full bg-gradient-to-r from-blue-500 to-blue-600 shadow-lg shadow-blue-500/30 animate-pulse"></div>
                      </div>
                      <div className="flex items-center space-x-2">
                        <span className="text-lg font-semibold text-gray-700">已选择</span>
                        <div className="px-4 py-2 bg-gradient-to-r from-blue-500 to-blue-600 text-white rounded-2xl shadow-lg shadow-blue-500/30 font-bold text-lg min-w-[3rem] text-center">
                          {stats.selected}
                        </div>
                        <span className="text-lg font-semibold text-gray-700">个服务</span>
                      </div>
                    </div>
                    
                    {stats.required > 0 && (
                      <div className="flex items-center space-x-3 px-6 py-3 bg-gradient-to-r from-orange-50/90 to-orange-100/70 rounded-2xl border border-orange-200/50 shadow-lg shadow-orange-500/10">
                        <div className="w-3 h-3 rounded-full bg-gradient-to-r from-orange-400 to-orange-500 shadow-lg shadow-orange-500/50"></div>
                        <span className="text-orange-700 font-semibold">
                          包含 {stats.required} 个必需服务
                        </span>
                      </div>
                    )}
                  </div>
                  
                  <div className="flex items-center space-x-4">
                    <Button
                      onClick={() => {
                        if (onPrevious) {
                          onPrevious()
                        } else {
                          onOpenChange(false)
                        }
                      }}
                      className="px-8 py-4 bg-white/80 hover:bg-white/90 text-gray-700 border-0 rounded-2xl shadow-lg shadow-black/5 font-semibold text-base transition-all duration-300 hover:shadow-xl hover:scale-105"
                    >
                      <ChevronLeft className="w-5 h-5 mr-2" />
                      上一步
                    </Button>
                    
                    <Button
                      onClick={handleNext}
                      disabled={selectedServiceIds.length === 0}
                      className="px-10 py-4 bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 text-white border-0 rounded-2xl shadow-xl shadow-blue-500/30 font-bold text-base transition-all duration-300 hover:shadow-2xl hover:shadow-blue-500/40 hover:scale-105 disabled:opacity-50 disabled:scale-100 disabled:cursor-not-allowed"
                    >
                      下一步
                      <ChevronRight className="w-5 h-5 ml-2" />
                    </Button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}

export default ClusterStep3Dialog