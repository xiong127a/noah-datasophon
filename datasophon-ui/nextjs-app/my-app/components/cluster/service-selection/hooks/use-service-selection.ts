"use client"

import { useState, useEffect, useCallback, useMemo } from 'react'
import { toast } from 'sonner'
import { apiV1, API_PATHS_V1 } from '@/lib/api-config-v1'
import { createClusterHeaders } from '@/lib/cluster-id-header'
import { ServiceType } from '@/types/service-selection'
import type { Service, Step3Data } from '@/types/service-selection'

/**
 * 服务选择业务逻辑Hook
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 * 日期：2024-01-20
 */

interface UseServiceSelectionOptions {
  clusterId?: number
  initialServiceType?: ServiceType
  onComplete?: (data: Step3Data) => void
}

interface UseServiceSelectionReturn {
  // 数据状态
  services: Service[]
  loading: boolean
  error: string | null
  
  // 选择状态
  selectedServiceIds: number[]
  serviceTypeFilter: ServiceType
  
  // 计算属性
  selectedServices: Service[]
  requiredServices: Service[]
  stats: {
    total: number
    selected: number
    required: number
  }
  
  // 操作方法
  setServiceTypeFilter: (type: ServiceType) => void
  toggleService: (serviceId: number) => void
  selectAllRequired: () => void
  clearSelection: () => void
  fetchServices: () => Promise<void>
  handleNext: () => void
  
  // 状态检查
  canProceed: boolean
  hasRequiredServices: boolean
}

export const useServiceSelection = ({
  clusterId,
  initialServiceType = ServiceType.MINIMAL,
  onComplete
}: UseServiceSelectionOptions = {}): UseServiceSelectionReturn => {
  // 基础状态
  const [services, setServices] = useState<Service[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [selectedServiceIds, setSelectedServiceIds] = useState<number[]>([])
  const [serviceTypeFilter, setServiceTypeFilter] = useState<ServiceType>(initialServiceType)

  // 计算派生数据
  const selectedServices = useMemo(() => 
    services.filter(service => selectedServiceIds.includes(service.id)),
    [services, selectedServiceIds]
  )

  const requiredServices = useMemo(() => {
    return services.filter(service => service.isRequired)
  }, [services])

  const stats = useMemo(() => ({
    total: services.length,
    selected: selectedServiceIds.length,
    required: requiredServices.length,
  }), [services.length, selectedServiceIds.length, requiredServices.length])

  // 状态检查
  const canProceed = selectedServiceIds.length > 0
  const hasRequiredServices = requiredServices.every(service => 
    selectedServiceIds.includes(service.id)
  )

  // 获取服务列表
  const fetchServices = useCallback(async () => {
    if (!clusterId) return
    
    setLoading(true)
    setError(null)
    
    try {
      const headers = createClusterHeaders(clusterId)
      const requestData = { type: serviceTypeFilter }
      
      let response
      try {
        // 尝试主要API
        const params = new URLSearchParams(requestData).toString()
        const url = `${API_PATHS_V1.FRAME_SERVICE_LIST_WITH_REQUIRED}?${params}`
        response = await apiV1.get(url, { headers })
      } catch {
        // 尝试备用API
        const params = new URLSearchParams(requestData).toString()
        const url = `${API_PATHS_V1.CLUSTER_SERVICE_LIST}?${params}`
        response = await apiV1.get(url, { headers })
      }
      
      if (response.data?.success && response.data?.data) {
        const rawServicesData = response.data.data
        
        // 直接使用后端返回的数据，不进行任何逻辑转换
        const servicesData = rawServicesData.map((s: Record<string, unknown>) => ({
          ...s,
          isRequired: Boolean(s.isRequired) // 简单的布尔值转换
        }))
        
        setServices(servicesData)
        
        // 数据加载完成后，根据新的必需服务列表重新设置选中状态
        const newRequiredServices = servicesData.filter((s: Service) => s.isRequired)
        const newRequiredIds = newRequiredServices.map((s: Service) => s.id)
        
        // 自动选择所有必需服务，取消所有非必需服务
        setSelectedServiceIds(newRequiredIds)
        
        // 显示切换完成的Toast消息
        if (serviceTypeFilter === ServiceType.MINIMAL) {
          toast.success(`✅ 最小化模式：已自动选择 ${newRequiredIds.length} 个必需服务`)
        } else {
          toast.success(`✅ 自定义模式：已自动选择 ${newRequiredIds.length} 个必需服务，可继续选择其他服务`)
        }
      } else if (response.data?.code === 200 && response.data?.data) {
        const rawServicesData = response.data.data
        
        // 直接使用后端返回的数据，不进行任何逻辑转换
        const servicesData = rawServicesData.map((s: Record<string, unknown>) => ({
          ...s,
          isRequired: Boolean(s.isRequired) // 简单的布尔值转换
        }))
        
        setServices(servicesData)
        
        // 数据加载完成后，根据新的必需服务列表重新设置选中状态
        const newRequiredServices = servicesData.filter((s: Service) => s.isRequired)
        const newRequiredIds = newRequiredServices.map((s: Service) => s.id)
        
        // 自动选择所有必需服务，取消所有非必需服务
        setSelectedServiceIds(newRequiredIds)
        
        // 显示切换完成的Toast消息
        if (serviceTypeFilter === ServiceType.MINIMAL) {
          toast.success(`✅ 最小化模式：已自动选择 ${newRequiredIds.length} 个必需服务`)
        } else {
          toast.success(`✅ 自定义模式：已自动选择 ${newRequiredIds.length} 个必需服务，可继续选择其他服务`)
        }
      } else {
        const errorMsg = response.data?.message || response.data?.msg || '获取服务列表失败'
        throw new Error(errorMsg)
      }
    } catch (err: unknown) {
      const errorMessage = (() => {
        if (err instanceof Error) {
          return err.message
        }
        const errorResponse = err as { response?: { data?: { message?: string; msg?: string } } }
        return errorResponse?.response?.data?.message || errorResponse?.response?.data?.msg || '获取服务列表失败'
      })()
      setError(errorMessage)
      toast.error(errorMessage)
    } finally {
      setLoading(false)
    }
  }, [clusterId, serviceTypeFilter])

  // 服务选择切换
  const toggleService = useCallback((serviceId: number) => {
    const service = services.find(s => s.id === serviceId)
    const isCurrentlySelected = selectedServiceIds.includes(serviceId)
    
    // 切换服务选择状态
    
    // 如果是必需服务且要取消选择，则不允许
    if (service?.isRequired && isCurrentlySelected) {
      toast.warning('必需服务不能取消选择')
      return
    }

    setSelectedServiceIds(prev => {
      const newSelection = prev.includes(serviceId)
        ? prev.filter(id => id !== serviceId)
        : [...prev, serviceId]
      
      // 选择状态已更新
      
      return newSelection
    })
  }, [services, selectedServiceIds])

  // 选择所有必需服务
  const selectAllRequired = useCallback(() => {
    const requiredIds = requiredServices.map(service => service.id)
    setSelectedServiceIds(requiredIds)
  }, [requiredServices])

  // 清空选择
  const clearSelection = useCallback(() => {
    const requiredIds = requiredServices.map(service => service.id)
    setSelectedServiceIds(requiredIds) // 保留必需服务
  }, [requiredServices])

  // 下一步处理
  const handleNext = useCallback(() => {
    if (!canProceed) {
      toast.error('请至少选择一个服务')
      return
    }

    if (!hasRequiredServices) {
      toast.error('请确保已选择所有必需服务')
      return
    }

    try {
      const step3Data: Step3Data = {
        serviceIds: selectedServiceIds,
        serviceNames: selectedServices.map(service => ({
          serviceId: service.id,
          serviceName: service.serviceName
        })),
        serviceType: serviceTypeFilter
      }


      onComplete?.(step3Data)
    } catch (error) {
      console.error('处理服务选择失败:', error)
      toast.error('处理服务选择失败，请重试')
    }
  }, [canProceed, hasRequiredServices, selectedServiceIds, selectedServices, serviceTypeFilter, onComplete])

  // 优化的服务类型切换处理
  const handleServiceTypeChange = useCallback((newType: ServiceType) => {
    // 切换模式，等待新的服务数据加载完成后会自动重新设置选中状态
    setServiceTypeFilter(newType)
    
    // Toast提示
    if (newType === ServiceType.MINIMAL) {
      toast.success(`正在切换到最小化模式...`)
    } else {
      toast.success(`正在切换到自定义模式...`)
    }
  }, [serviceTypeFilter]) // eslint-disable-line react-hooks/exhaustive-deps

  // 已移除：初始化时自动选择必需服务的逻辑
  // 现在在每次数据加载完成后都会自动设置选中状态，不需要额外的初始化逻辑

  // 监控选中状态变化和数据一致性检查
  useEffect(() => {
    if (services.length > 0 && process.env.NODE_ENV === 'development') {
      // 仅在开发环境进行数据一致性检查
      const missingRequiredServices = requiredServices.filter(s => !selectedServiceIds.includes(s.id))
      const selectedServices = selectedServiceIds.map(id => services.find(s => s.id === id)).filter(Boolean)
      const extraSelectedServices = selectedServices.filter(s => s && !s.isRequired)
      
      if (missingRequiredServices.length > 0) {
        console.warn('数据不一致：有必需服务未被选中:', missingRequiredServices.map(s => s.serviceName))
      }
      
      if (extraSelectedServices.length > 0) {
        console.warn('数据不一致：有非必需服务被选中:', extraSelectedServices.map(s => s?.serviceName).filter(Boolean))
      }
    }
  }, [selectedServiceIds, services, requiredServices, serviceTypeFilter])

  // 初始化加载
  useEffect(() => {
    if (clusterId) {
      fetchServices()
    }
  }, [clusterId, fetchServices])

  return {
    // 数据状态
    services,
    loading,
    error,
    
    // 选择状态
    selectedServiceIds,
    serviceTypeFilter,
    
    // 计算属性
    selectedServices,
    requiredServices,
    stats,
    
    // 操作方法
    setServiceTypeFilter: handleServiceTypeChange,
    toggleService,
    selectAllRequired,
    clearSelection,
    fetchServices,
    handleNext,
    
    // 状态检查
    canProceed,
    hasRequiredServices
  }
}

export default useServiceSelection
