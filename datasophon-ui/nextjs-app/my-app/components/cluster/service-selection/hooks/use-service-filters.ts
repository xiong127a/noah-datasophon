"use client"

import { useState, useMemo, useCallback, useEffect } from 'react'
import type { Service } from '@/types/service-selection'

/**
 * 服务过滤和搜索Hook
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 * 日期：2024-01-20
 */

interface UseServiceFiltersOptions {
  services: Service[]
}

interface UseServiceFiltersReturn {
  // 过滤状态
  searchTerm: string
  showRequiredOnly: boolean
  showSelectedOnly: boolean
  
  // 搜索防抖值
  debouncedSearchTerm: string
  
  // 过滤后的服务
  filteredServices: Service[]
  
  // 操作方法
  setSearchTerm: (term: string) => void
  setShowRequiredOnly: (show: boolean) => void
  setShowSelectedOnly: (show: boolean) => void
  clearFilters: () => void
  
  // 统计信息
  filterStats: {
    total: number
    filtered: number
    hasActiveFilters: boolean
  }
}

export const useServiceFilters = ({ 
  services 
}: UseServiceFiltersOptions): UseServiceFiltersReturn => {
  // 过滤状态
  const [searchTerm, setSearchTerm] = useState('')
  const [showRequiredOnly, setShowRequiredOnly] = useState(false)
  const [showSelectedOnly, setShowSelectedOnly] = useState(false)
  
  // 搜索防抖处理（300ms延迟）
  const [debouncedSearchTerm, setDebouncedSearchTerm] = useState(searchTerm)
  
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearchTerm(searchTerm)
    }, 300)
    
    return () => clearTimeout(timer)
  }, [searchTerm])

  // 过滤逻辑
  const filteredServices = useMemo(() => {
    let filtered = [...services]

    // 文本搜索过滤
    if (debouncedSearchTerm.trim()) {
      const searchLower = debouncedSearchTerm.toLowerCase().trim()
      filtered = filtered.filter(service => 
        service.serviceName.toLowerCase().includes(searchLower) ||
        service.serviceDesc?.toLowerCase().includes(searchLower) ||
        service.label?.toLowerCase().includes(searchLower)
      )
    }

    // 必需服务过滤
    if (showRequiredOnly) {
      filtered = filtered.filter(service => service.isRequired)
    }

    // 注意：showSelectedOnly 在基础过滤器中不使用，在高级过滤器中使用
    
    return filtered
  }, [services, debouncedSearchTerm, showRequiredOnly])

  // 清空所有过滤器
  const clearFilters = useCallback(() => {
    setSearchTerm('')
    setShowRequiredOnly(false)
    setShowSelectedOnly(false)
  }, [])

  // 统计信息
  const filterStats = useMemo(() => {
    const hasActiveFilters = Boolean(
      searchTerm.trim() || 
      showRequiredOnly || 
      showSelectedOnly
    )
    
    return {
      total: services.length,
      filtered: filteredServices.length,
      hasActiveFilters
    }
  }, [services.length, filteredServices.length, searchTerm, showRequiredOnly, showSelectedOnly])

  return {
    // 过滤状态
    searchTerm,
    showRequiredOnly,
    showSelectedOnly,
    
    // 搜索防抖值
    debouncedSearchTerm,
    
    // 过滤后的服务
    filteredServices,
    
    // 操作方法
    setSearchTerm,
    setShowRequiredOnly,
    setShowSelectedOnly,
    clearFilters,
    
    // 统计信息
    filterStats
  }
}

// 高级过滤器hook，支持更多过滤条件
interface UseAdvancedServiceFiltersOptions extends UseServiceFiltersOptions {
  selectedServiceIds: number[]
  categories?: string[]
}

interface UseAdvancedServiceFiltersReturn extends UseServiceFiltersReturn {
  // 额外的过滤状态
  selectedCategory: string | null
  
  // 额外的操作方法
  setSelectedCategory: (category: string | null) => void
  
  // 分类信息
  availableCategories: string[]
}

export const useAdvancedServiceFilters = ({ 
  services,
  selectedServiceIds,
  categories = []
}: UseAdvancedServiceFiltersOptions): UseAdvancedServiceFiltersReturn => {
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null)
  
  // 获取基础过滤功能
  const baseFilters = useServiceFilters({ services })
  
  // 提取可用的分类
  const availableCategories = useMemo(() => {
    if (categories.length > 0) return categories
    
    // 从服务中自动提取分类（基于服务名前缀）
    const serviceCategories = Array.from(new Set(
      services.map(service => {
        const serviceName = service.serviceName || service.label || ''
        // 简单分类：大数据服务通常以特定前缀开头
        if (serviceName.toLowerCase().includes('hadoop')) return 'Hadoop生态'
        if (serviceName.toLowerCase().includes('spark')) return 'Spark'
        if (serviceName.toLowerCase().includes('kafka')) return '消息队列'
        return '其他'
      })
        .filter(Boolean)
    )).sort()
    
    return serviceCategories
  }, [services, categories])
  
  // 高级过滤逻辑
  const filteredServices = useMemo(() => {
    let filtered = [...baseFilters.filteredServices]
    
    // 分类过滤
    if (selectedCategory) {
      filtered = filtered.filter(service => {
        const serviceName = service.serviceName || service.label || ''
        const serviceCategory = serviceName.toLowerCase().includes('hadoop') ? 'Hadoop生态' :
                              serviceName.toLowerCase().includes('spark') ? 'Spark' :
                              serviceName.toLowerCase().includes('kafka') ? '消息队列' : '其他'
        return serviceCategory === selectedCategory
      })
    }
    
    // 已选择服务过滤
    if (baseFilters.showSelectedOnly) {
      filtered = filtered.filter(service => 
        selectedServiceIds.includes(service.id)
      )
    }
    
    return filtered
  }, [baseFilters.filteredServices, selectedCategory, baseFilters.showSelectedOnly, selectedServiceIds])
  
  // 重新计算统计信息
  const filterStats = useMemo(() => {
    const hasActiveFilters = Boolean(
      baseFilters.filterStats.hasActiveFilters || 
      selectedCategory
    )
    
    return {
      total: services.length,
      filtered: filteredServices.length,
      hasActiveFilters
    }
  }, [services.length, filteredServices.length, baseFilters.filterStats.hasActiveFilters, selectedCategory])
  
  // 清空所有过滤器
  const clearFilters = useCallback(() => {
    baseFilters.clearFilters()
    setSelectedCategory(null)
  }, [baseFilters])

  return {
    ...baseFilters,
    
    // 覆盖计算属性
    filteredServices,
    filterStats,
    clearFilters,
    
    // 额外的状态和方法
    selectedCategory,
    setSelectedCategory,
    availableCategories
  }
}

export default useServiceFilters
