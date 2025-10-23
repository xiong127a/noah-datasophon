"use client"

import React from 'react'
import { 
  Search, 
  Filter, 
  X, 
  Package, 
  CheckCircle2,
  AlertCircle,
  Layers
} from 'lucide-react'
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { 
  Select, 
  SelectContent, 
  SelectItem, 
  SelectTrigger, 
  SelectValue 
} from '@/components/ui/select'
import { 
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuCheckboxItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Checkbox } from '@/components/ui/checkbox'
import { ServiceType, SERVICE_TYPE_OPTIONS } from '@/types/service-selection'

/**
 * 现代化服务过滤器组件
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 * 日期：2024-01-20
 */

interface ServiceFiltersProps {
  // 搜索相关
  searchTerm: string
  onSearchChange: (term: string) => void
  
  // 服务类型过滤
  serviceTypeFilter: ServiceType
  onServiceTypeChange: (type: ServiceType) => void
  
  // 高级过滤
  showRequiredOnly: boolean
  onShowRequiredOnlyChange: (show: boolean) => void
  showSelectedOnly: boolean
  onShowSelectedOnlyChange: (show: boolean) => void
  
  // 分类过滤
  selectedCategory?: string | null
  onCategoryChange?: (category: string | null) => void
  availableCategories?: string[]
  
  // 清空过滤器
  onClearFilters: () => void
  
  // 统计信息
  filterStats: {
    total: number
    filtered: number
    hasActiveFilters: boolean
  }

  // 是否隐藏服务类型过滤器（添加服务模式下使用）
  hideServiceTypeFilter?: boolean
}

const ServiceFilters: React.FC<ServiceFiltersProps> = ({
  searchTerm,
  onSearchChange,
  serviceTypeFilter,
  onServiceTypeChange,
  showRequiredOnly,
  onShowRequiredOnlyChange,
  showSelectedOnly,
  onShowSelectedOnlyChange,
  selectedCategory,
  onCategoryChange,
  availableCategories = [],
  onClearFilters,
  filterStats,
  hideServiceTypeFilter = false
}) => {
  return (
    <div className="bg-white rounded-xl border border-gray-200 shadow-sm">
      {/* 紧凑的单行布局：搜索框和模式切换 */}
      <div className="flex items-center gap-2 p-2">
        {/* 搜索框 */}
        <div className="flex-1 relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
          <Input
            type="text"
            placeholder="搜索服务..."
            value={searchTerm}
            onChange={(e) => onSearchChange(e.target.value)}
            className="pl-10 h-8 text-sm border-0 bg-gray-50/50 focus:bg-white transition-colors"
          />
          {searchTerm && (
            <button
              onClick={() => onSearchChange('')}
              className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
            >
              <X className="w-3 h-3" />
            </button>
          )}
        </div>

        {/* 模式切换按钮组 - 仅在非添加服务模式下显示 */}
        {!hideServiceTypeFilter && (
          <TooltipProvider>
            <div className="flex bg-gray-100 rounded-lg p-0.5">
              {SERVICE_TYPE_OPTIONS.map((option) => (
                <Tooltip key={option.value}>
                  <TooltipTrigger asChild>
                    <button
                      onClick={() => onServiceTypeChange(option.value as ServiceType)}
                      className={`px-3 py-1 text-xs font-medium rounded-md transition-all ${
                        serviceTypeFilter === option.value
                          ? 'bg-white text-blue-600 shadow-sm border border-blue-200'
                          : 'text-gray-600 hover:text-gray-800'
                      }`}
                    >
                      {option.label}
                    </button>
                  </TooltipTrigger>
                  <TooltipContent>
                    <p>{option.description}</p>
                  </TooltipContent>
                </Tooltip>
              ))}
            </div>
          </TooltipProvider>
        )}
      </div>

      {/* 搜索状态提示 */}
      {searchTerm && (
        <div className="px-3 py-2 border-t border-gray-100 bg-blue-50/30">
          <div className="flex items-center justify-between text-xs text-blue-700">
            <span>搜索结果: "{searchTerm}"</span>
            <button 
              onClick={() => onSearchChange('')}
              className="text-blue-500 hover:text-blue-700"
            >
              清除
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

// 简化版过滤器（用于移动端或紧凑布局）
interface CompactServiceFiltersProps {
  searchTerm: string
  onSearchChange: (term: string) => void
  serviceTypeFilter: ServiceType
  onServiceTypeChange: (type: ServiceType) => void
  onOpenAdvancedFilters: () => void
  filterStats: {
    total: number
    filtered: number
    hasActiveFilters: boolean
  }
}

export const CompactServiceFilters: React.FC<CompactServiceFiltersProps> = ({
  searchTerm,
  onSearchChange,
  serviceTypeFilter,
  onServiceTypeChange,
  onOpenAdvancedFilters,
  filterStats
}) => {
  return (
    <div className="flex items-center gap-2">
      {/* 搜索框 */}
      <div className="flex-1 relative">
        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
        <Input
          type="text"
          placeholder="搜索服务..."
          value={searchTerm}
          onChange={(e) => onSearchChange(e.target.value)}
          className="pl-10 h-9"
        />
      </div>

      {/* 服务类型 */}
      <Select 
        value={serviceTypeFilter} 
        onValueChange={(value: string) => onServiceTypeChange(value as ServiceType)}
      >
        <SelectTrigger className="w-32 h-9">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {SERVICE_TYPE_OPTIONS.map((option) => (
            <SelectItem key={option.value} value={option.value}>
              {option.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      {/* 高级过滤按钮 */}
      <Button
        variant="outline"
        size="sm"
        onClick={onOpenAdvancedFilters}
        className="h-9"
      >
        <Filter className="w-4 h-4 mr-1" />
        过滤
        {filterStats.hasActiveFilters && (
          <Badge variant="destructive" className="ml-2 h-4 w-4 p-0 text-xs">
            !
          </Badge>
        )}
      </Button>
    </div>
  )
}

export default ServiceFilters
