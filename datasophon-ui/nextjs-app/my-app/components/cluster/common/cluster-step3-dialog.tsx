"use client"

import React, { useState, useEffect, useMemo } from 'react'
import { 
  X, ChevronLeft, ChevronRight, CheckCircle, Loader2, RefreshCw,
  AlertCircle, Package, Database, Search
} from 'lucide-react'
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Checkbox } from "@/components/ui/checkbox"
import { clusterApi } from "@/lib/api"
import { toast } from 'sonner'
import ClusterWizardSidebar from './cluster-wizard-sidebar'
import { getStepsByType, StepsType } from '@/lib/cluster-steps'
import { createClusterHeaders } from '@/lib/cluster-id-header'
import { ClusterType, ClusterTypeUtil } from '@/types'

import { SERVICE_TYPE_OPTIONS, ServiceType } from '@/types/step3'
import type { 
  ClusterStep3DialogProps, 
  Service, 
  ServiceSelection,
  Step3Data,
  ServiceTypeOption,
  ServiceListResponse
} from '@/types/step3'

/**
 * 集群步骤3：大数据服务选择对话框
 * 用于K8S模式下选择需要安装的大数据服务
 * 
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 * 日期：2024-01-20
 */
const ClusterStep3Dialog: React.FC<ClusterStep3DialogProps> = ({
  open,
  onOpenChange,
  cluster,
  step2Data,
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
  const [serviceTypeFilter, setServiceTypeFilter] = useState<ServiceType>(ServiceType.CORE)
  const [searchTerm, setSearchTerm] = useState('')

  // 集群ID
  const clusterId = cluster?.id

  // 搜索和筛选后的服务列表
  const filteredServices = useMemo(() => {
    return services.filter(service => {
      // 搜索筛选
      const searchMatch = searchTerm === '' || 
        service.label?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        service.serviceName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        service.serviceDesc?.toLowerCase().includes(searchTerm.toLowerCase())
      
      // 服务类型筛选（根据必需状态进行筛选）
      const typeMatch = serviceTypeFilter === ServiceType.CORE 
        ? service.isRequired  // 核心服务：显示必需的服务
        : !service.isRequired // 自定义服务：显示非必需的服务
      
      return searchMatch && typeMatch
    })
  }, [services, searchTerm, serviceTypeFilter])

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
        
        // 如果是K8S模式，自动选中必需的服务（如果有的话）
        if (ClusterTypeUtil.isKubernetes(clusterType)) {
          const requiredServices = serviceData
            .filter((service: Service) => service.isRequired && !service.installed)
          
          if (requiredServices.length > 0) {
            const requiredIds = requiredServices.map((service: Service) => service.id)
            const requiredSelections = requiredServices.map((service: Service) => ({
              serviceId: service.id,
              serviceName: service.serviceName
            }))
            
            setSelectedServiceIds(prev => [...new Set([...prev, ...requiredIds])])
            setSelectedServices(prev => {
              const combined = [...prev, ...requiredSelections]
              // 去重
              const unique = combined.filter((item, index, self) => 
                index === self.findIndex(t => t.serviceId === item.serviceId)
              )
              return unique
            })
          }
        }
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
  const handleServiceSelection = (serviceId: number, checked: boolean) => {
    const service = services.find(s => s.id === serviceId)
    if (!service) return

    if (checked) {
      // 添加选择
      setSelectedServiceIds(prev => [...prev, serviceId])
      setSelectedServices(prev => [
        ...prev,
        {
          serviceId: service.id,
          serviceName: service.serviceName
        }
      ])
    } else {
      // 移除选择
      setSelectedServiceIds(prev => prev.filter(id => id !== serviceId))
      setSelectedServices(prev => prev.filter(s => s.serviceId !== serviceId))
    }
  }

  // 全选/取消全选
  const handleSelectAll = () => {
    const selectableServices = filteredServices.filter(service => 
      ClusterTypeUtil.isKubernetes(clusterType) ? true : !service.installed
    )
    
    const allSelected = selectableServices.every(service => 
      selectedServiceIds.includes(service.id)
    )

    if (allSelected) {
      // 取消全选
      const selectableIds = selectableServices.map(s => s.id)
      setSelectedServiceIds(prev => prev.filter(id => !selectableIds.includes(id)))
      setSelectedServices(prev => prev.filter(s => !selectableIds.includes(s.serviceId)))
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

  // 组件挂载时获取服务列表
  useEffect(() => {
    if (open && clusterId) {
      fetchServiceList()
    }
  }, [open, clusterId, serviceTypeFilter])

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

          {/* 右侧内容 */}
          <div className="flex-1 flex flex-col min-w-0">
            {/* 头部 */}
            <div className="flex items-center justify-between p-6 border-b border-gray-100">
              <div>
                <h2 className="text-2xl font-semibold text-gray-900 flex items-center">
                  <Package className="w-6 h-6 mr-3 text-blue-600" />
                  大数据服务选择
                </h2>
                <p className="text-gray-600 mt-1">
                  选择需要在Kubernetes集群中部署的大数据服务组件
                </p>
              </div>
              <Button
                variant="ghost"
                size="icon"
                onClick={() => onOpenChange(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-5 h-5" />
              </Button>
            </div>

            {/* 主内容区域 */}
            <div className="flex-1 p-6 min-h-0">
              <div className="h-full flex flex-col">
                <div className="flex-1 grid grid-cols-4 gap-6 min-h-0">
                  {/* 服务列表表格 */}
                  <div className="col-span-3">
                    <Card className="h-full flex flex-col">
                      <CardHeader className="pb-4 flex-shrink-0">
                        <div className="flex items-center justify-between">
                          <div className="flex items-center space-x-4">
                            <CardTitle className="text-lg flex items-center">
                              <Database className="w-5 h-5 mr-2 text-blue-600" />
                              可用服务
                              {services.length > 0 && (
                                <span className="ml-2 text-sm font-normal text-gray-500">
                                  （共 {stats.total} 个）
                                </span>
                              )}
                            </CardTitle>
                            <Button 
                              onClick={() => fetchServiceList(true)}
                              disabled={loading}
                              variant="outline"
                              size="sm"
                            >
                              {loading ? (
                                <Loader2 className="w-3 h-3 mr-1.5 animate-spin" />
                              ) : (
                                <RefreshCw className="w-3 h-3 mr-1.5" />
                              )}
                              刷新
                            </Button>
                          </div>
                        </div>
                        
                        {/* 搜索和筛选栏 */}
                        <div className="flex items-center space-x-4 mt-4">
                          <div className="relative flex-1 max-w-md">
                            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
                            <Input
                              type="text"
                              placeholder="搜索服务名称或描述..."
                              className="pl-10"
                              value={searchTerm}
                              onChange={(e) => setSearchTerm(e.target.value)}
                            />
                          </div>
                          
                          <Select value={serviceTypeFilter} onValueChange={setServiceTypeFilter}>
                            <SelectTrigger className="w-48">
                              <SelectValue placeholder="选择服务类型" />
                            </SelectTrigger>
                            <SelectContent>
                              {serviceTypeOptions.map((option) => (
                                <SelectItem key={option.value} value={option.value}>
                                  <div>
                                    <div className="font-medium">{option.label}</div>
                                    {option.description && (
                                      <div className="text-xs text-gray-500">{option.description}</div>
                                    )}
                                  </div>
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>

                          <Button
                            onClick={handleSelectAll}
                            variant="outline"
                            size="sm"
                            disabled={filteredServices.length === 0}
                          >
                            {filteredServices.every(s => selectedServiceIds.includes(s.id)) ? '取消全选' : '全选'}
                          </Button>
                        </div>
                      </CardHeader>
                      
                      <CardContent className="pt-0 flex-1 flex flex-col min-h-0">
                        {loading ? (
                          <div className="flex items-center justify-center h-64">
                            <Loader2 className="w-6 h-6 animate-spin mr-2" />
                            <span>加载中...</span>
                          </div>
                        ) : filteredServices.length === 0 ? (
                          <div className="flex flex-col items-center justify-center h-64 text-gray-500">
                            <Package className="w-16 h-16 mb-4 text-gray-300" />
                            <div className="text-lg mb-2">暂无可用服务</div>
                            <div className="text-sm">请尝试调整筛选条件或检查服务配置</div>
                          </div>
                        ) : (
                          <div className="flex-1 overflow-y-auto">
                            <div className="grid gap-3">
                              {filteredServices.map((service) => (
                                <div
                                  key={service.id}
                                  className={`group relative rounded-lg border p-4 transition-all duration-200 cursor-pointer ${
                                    selectedServiceIds.includes(service.id)
                                      ? 'border-blue-300 bg-blue-50/50 shadow-sm'
                                      : 'border-gray-200 hover:border-gray-300 hover:shadow-sm'
                                  }`}
                                  onClick={() => handleServiceSelection(service.id, !selectedServiceIds.includes(service.id))}
                                >
                                  <div className="flex items-start space-x-3">
                                    <Checkbox
                                      checked={selectedServiceIds.includes(service.id)}
                                      onCheckedChange={(checked) => 
                                        handleServiceSelection(service.id, checked === true)
                                      }
                                      className="mt-0.5"
                                      onClick={(e) => e.stopPropagation()}
                                    />
                                    
                                    <div className="flex-1 min-w-0">
                                      <div className="flex items-center justify-between">
                                        <h4 className="text-sm font-medium text-gray-900 truncate">
                                          {service.label || service.serviceName}
                                        </h4>
                                        <div className="flex items-center space-x-2 ml-2">
                                          {service.isRequired && (
                                            <Badge variant="secondary" className="text-xs">必需</Badge>
                                          )}
                                          {service.installed && (
                                            <Badge variant="outline" className="text-xs">已安装</Badge>
                                          )}
                                          {service.serviceVersion && (
                                            <Badge variant="outline" className="text-xs font-mono">
                                              v{service.serviceVersion}
                                            </Badge>
                                          )}
                                        </div>
                                      </div>
                                      
                                      <div className="mt-1 text-xs text-gray-600 truncate">
                                        {service.serviceName}
                                      </div>
                                      
                                      {service.serviceDesc && (
                                        <p className="mt-2 text-sm text-gray-600 line-clamp-2">
                                          {service.serviceDesc}
                                        </p>
                                      )}
                                    </div>
                                  </div>
                                </div>
                              ))}
                            </div>
                          </div>
                        )}
                      </CardContent>
                    </Card>
                  </div>

                  {/* 右侧统计面板 */}
                  <div className="col-span-1">
                    <Card className="h-full">
                      <CardHeader className="pb-4">
                        <CardTitle className="text-lg flex items-center">
                          <AlertCircle className="w-5 h-5 text-blue-600 mr-2" />
                          选择统计
                        </CardTitle>
                      </CardHeader>
                      <CardContent className="space-y-4">
                        <div className="flex justify-between items-center">
                          <span className="text-gray-600">总服务数</span>
                          <span className="font-semibold text-lg">{stats.total}</span>
                        </div>
                        <div className="flex justify-between items-center">
                          <span className="text-gray-600">已选择</span>
                          <span className="font-semibold text-lg text-blue-600">{stats.selected}</span>
                        </div>
                        <div className="flex justify-between items-center">
                          <span className="text-gray-600">必需服务</span>
                          <span className="font-semibold text-lg text-orange-600">{stats.required}</span>
                        </div>
                        <div className="flex justify-between items-center">
                          <span className="text-gray-600">可用服务</span>
                          <span className="font-semibold text-lg text-green-600">{stats.available}</span>
                        </div>
                        
                        {selectedServices.length > 0 && (
                          <div className="pt-4 border-t">
                            <h4 className="font-medium text-gray-900 mb-3">已选择的服务：</h4>
                            <div className="space-y-2 max-h-48 overflow-y-auto">
                              {selectedServices.map((service) => (
                                <div key={service.serviceId} className="text-sm text-gray-700 bg-blue-50 rounded p-2">
                                  {service.serviceName}
                                </div>
                              ))}
                            </div>
                          </div>
                        )}
                      </CardContent>
                    </Card>
                  </div>
                </div>
              </div>
            </div>

            {/* 底部操作栏 */}
            <div className="bg-white/95 backdrop-blur-md border-t border-gray-200/80 p-6 shadow-lg">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-4">
                  <div className="flex items-center space-x-3">
                    <div className="w-3 h-3 rounded-full bg-blue-500 animate-pulse"></div>
                    <span className="text-sm font-medium text-gray-700">
                      已选择 
                      <span className="mx-1 px-2 py-0.5 bg-blue-100 text-blue-700 rounded-full text-xs font-semibold">
                        {stats.selected}
                      </span>
                      个服务
                    </span>
                  </div>
                  {stats.required > 0 && (
                    <div className="flex items-center space-x-2 px-3 py-1.5 bg-orange-50 rounded-lg border border-orange-200">
                      <div className="w-2 h-2 rounded-full bg-orange-500"></div>
                      <span className="text-sm font-medium text-orange-700">
                        {stats.required} 个必需服务
                      </span>
                    </div>
                  )}
                </div>
                <div className="flex items-center space-x-3">
                  <Button
                    onClick={() => {
                      if (onPrevious) {
                        onPrevious()
                      } else {
                        onOpenChange(false)
                      }
                    }}
                    variant="outline"
                    className="px-6 py-2"
                  >
                    <ChevronLeft className="w-4 h-4 mr-2" />
                    上一步
                  </Button>
                  <Button
                    onClick={handleNext}
                    disabled={selectedServiceIds.length === 0}
                    className={`px-6 py-2 ${
                      selectedServiceIds.length === 0
                        ? 'bg-gray-200 text-gray-400 cursor-not-allowed'
                        : 'bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 text-white transform hover:scale-105'
                    }`}
                  >
                    下一步
                    <ChevronRight className="w-4 h-4 ml-2" />
                  </Button>
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
