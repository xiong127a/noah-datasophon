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

  const requiredServices = useMemo(() => 
    services.filter(service => service.isRequired),
    [services]
  )

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
      } catch (primaryError: any) {
        // 尝试备用API
        const params = new URLSearchParams(requestData).toString()
        const url = `${API_PATHS_V1.CLUSTER_SERVICE_LIST}?${params}`
        response = await apiV1.get(url, { headers })
      }
      
      if (response.data?.success && response.data?.data) {
        setServices(response.data.data)
      } else if (response.data?.code === 200 && response.data?.data) {
        setServices(response.data.data)
      } else {
        const errorMsg = response.data?.message || response.data?.msg || '获取服务列表失败'
        throw new Error(errorMsg)
      }
    } catch (err: any) {
      const errorMessage = err?.response?.data?.message || err?.response?.data?.msg || err?.message || '获取服务列表失败'
      setError(errorMessage)
      toast.error(errorMessage)
    } finally {
      setLoading(false)
    }
  }, [clusterId, serviceTypeFilter])

  // 服务选择切换
  const toggleService = useCallback((serviceId: number) => {
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
  }, [services])

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

      toast.error('处理服务选择失败，请重试')
    }
  }, [canProceed, hasRequiredServices, selectedServiceIds, selectedServices, serviceTypeFilter, onComplete])

  // 自动选择必需服务
  useEffect(() => {
    if (services.length > 0) {
      const requiredIds = requiredServices.map(service => service.id)
      
      // 如果是最小化模式，只选择必需服务
      if (serviceTypeFilter === ServiceType.MINIMAL) {
        setSelectedServiceIds(requiredIds)
        toast.success(`最小化模式：已选择 ${requiredIds.length} 个必需服务`)
      } else {
        // 自定义模式：确保必需服务被选中，但保留其他已选服务
        setSelectedServiceIds(prev => {
          const nonRequiredSelected = prev.filter(id => 
            !requiredIds.includes(id) && services.find(s => s.id === id && !s.isRequired)
          )
          const newSelection = [...requiredIds, ...nonRequiredSelected]
          
          if (prev.length === 0) {
            toast.success(`自定义模式：已选择 ${requiredIds.length} 个必需服务，可继续选择其他服务`)
          }
          
          return newSelection
        })
      }
    }
  }, [services, requiredServices, serviceTypeFilter])

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
    setServiceTypeFilter,
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
