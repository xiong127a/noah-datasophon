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
  filterStats
}) => {
  return (
    <div className="bg-white rounded-lg border border-gray-200 p-4 space-y-4 shadow-sm">
      {/* 顶部：搜索框和服务类型 */}
      <div className="flex flex-col sm:flex-row gap-4">
        {/* 搜索框 */}
        <div className="flex-1">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
            <Input
              type="text"
              placeholder="搜索服务名称或描述..."
              value={searchTerm}
              onChange={(e) => onSearchChange(e.target.value)}
              className="pl-10 h-10"
            />
            {searchTerm && (
              <button
                onClick={() => onSearchChange('')}
                className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
              >
                <X className="w-4 h-4" />
              </button>
            )}
          </div>
        </div>

        {/* 服务类型选择 */}
        <div className="w-full sm:w-64">
          <Select 
            value={serviceTypeFilter} 
            onValueChange={(value: string) => onServiceTypeChange(value as ServiceType)}
          >
            <SelectTrigger className="h-10">
              <div className="flex items-center gap-2">
                <Package className="w-4 h-4 text-gray-500" />
                <SelectValue>
                  {SERVICE_TYPE_OPTIONS.find(option => option.value === serviceTypeFilter)?.label}
                </SelectValue>
              </div>
            </SelectTrigger>
            <SelectContent>
              {SERVICE_TYPE_OPTIONS.map((option) => (
                <SelectItem key={option.value} value={option.value}>
                  <div className="flex flex-col py-1">
                    <span className="font-medium">{option.label}</span>
                    <span className="text-xs text-gray-500">{option.description}</span>
                  </div>
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* 中部：高级过滤选项 */}
      <div className="flex flex-wrap items-center gap-4">
        {/* 快速过滤复选框 */}
        <div className="flex items-center space-x-4">
          <div className="flex items-center space-x-2">
            <Checkbox
              id="required-only"
              checked={showRequiredOnly}
              onCheckedChange={onShowRequiredOnlyChange}
            />
            <label 
              htmlFor="required-only" 
              className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70 cursor-pointer"
            >
              仅显示必需服务
            </label>
          </div>

          <div className="flex items-center space-x-2">
            <Checkbox
              id="selected-only"
              checked={showSelectedOnly}
              onCheckedChange={onShowSelectedOnlyChange}
            />
            <label 
              htmlFor="selected-only" 
              className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70 cursor-pointer"
            >
              仅显示已选服务
            </label>
          </div>
        </div>

        {/* 分类过滤器（如果有分类） */}
        {availableCategories.length > 0 && onCategoryChange && (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="outline" size="sm" className="h-9">
                <Layers className="w-4 h-4 mr-2" />
                分类
                {selectedCategory && (
                  <Badge variant="secondary" className="ml-2 h-5">
                    {selectedCategory}
                  </Badge>
                )}
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="start" className="w-48">
              <DropdownMenuLabel>服务分类</DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuCheckboxItem
                checked={!selectedCategory}
                onCheckedChange={() => onCategoryChange(null)}
              >
                全部分类
              </DropdownMenuCheckboxItem>
              {availableCategories.map((category) => (
                <DropdownMenuCheckboxItem
                  key={category}
                  checked={selectedCategory === category}
                  onCheckedChange={(checked) => 
                    onCategoryChange(checked ? category : null)
                  }
                >
                  {category}
                </DropdownMenuCheckboxItem>
              ))}
            </DropdownMenuContent>
          </DropdownMenu>
        )}

        {/* 清空过滤器按钮 */}
        {filterStats.hasActiveFilters && (
          <Button
            variant="ghost"
            size="sm"
            onClick={onClearFilters}
            className="h-9 text-gray-500 hover:text-gray-700"
          >
            <X className="w-4 h-4 mr-1" />
            清空过滤器
          </Button>
        )}
      </div>

      {/* 底部：统计信息和活跃过滤器标签 */}
      <div className="flex flex-wrap items-center justify-between gap-4 pt-2 border-t border-gray-100">
        {/* 统计信息 */}
        <div className="flex items-center gap-4 text-sm text-gray-600">
          <div className="flex items-center gap-1">
            <Package className="w-4 h-4" />
            显示 {filterStats.filtered} / {filterStats.total} 个服务
          </div>
        </div>

        {/* 活跃过滤器标签 */}
        <div className="flex items-center gap-2">
          {searchTerm && (
            <Badge variant="outline" className="gap-1">
              <Search className="w-3 h-3" />
              搜索: {searchTerm}
              <button 
                onClick={() => onSearchChange('')}
                className="ml-1 hover:bg-gray-200 rounded-full p-0.5"
              >
                <X className="w-3 h-3" />
              </button>
            </Badge>
          )}
          
          {showRequiredOnly && (
            <Badge variant="outline" className="gap-1">
              <AlertCircle className="w-3 h-3" />
              必需服务
              <button 
                onClick={() => onShowRequiredOnlyChange(false)}
                className="ml-1 hover:bg-gray-200 rounded-full p-0.5"
              >
                <X className="w-3 h-3" />
              </button>
            </Badge>
          )}
          
          {showSelectedOnly && (
            <Badge variant="outline" className="gap-1">
              <CheckCircle2 className="w-3 h-3" />
              已选服务
              <button 
                onClick={() => onShowSelectedOnlyChange(false)}
                className="ml-1 hover:bg-gray-200 rounded-full p-0.5"
              >
                <X className="w-3 h-3" />
              </button>
            </Badge>
          )}
          
          {selectedCategory && (
            <Badge variant="outline" className="gap-1">
              <Layers className="w-3 h-3" />
              分类: {selectedCategory}
              <button 
                onClick={() => onCategoryChange?.(null)}
                className="ml-1 hover:bg-gray-200 rounded-full p-0.5"
              >
                <X className="w-3 h-3" />
              </button>
            </Badge>
          )}
        </div>
      </div>
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
