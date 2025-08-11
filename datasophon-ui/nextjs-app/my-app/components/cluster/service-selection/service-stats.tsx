"use client"

import React from 'react'
import { Badge } from '@/components/ui/badge'
import type { Service } from '@/types/service-selection'

/**
 * 简化的服务统计组件 - 仅占用一行空间
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 * 日期：2024-01-20
 */

interface ServiceStatsProps {
  services: Service[]
  selectedServices: Service[]
  filteredServices: Service[]
  requiredServices: Service[]
  className?: string
}

// 简化的统计组件 - 只占用一行
const ServiceStats: React.FC<ServiceStatsProps> = ({
  services = [],
  selectedServices = [],
  filteredServices = [],
  requiredServices = [],
  className = ''
}) => {
  const totalServices = services?.length || 0
  const selectedCount = selectedServices?.length || 0
  const requiredCount = requiredServices?.length || 0
  const selectedRequiredCount = selectedServices?.filter(s => s?.isRequired)?.length || 0
  const meetsRequirements = selectedRequiredCount === requiredCount

  return (
    <div className={`flex items-center gap-4 text-sm ${className}`}>
      <div className="flex items-center gap-2">
        <span className="text-muted-foreground">已选择:</span>
        <Badge variant="secondary">{selectedCount} / {totalServices}</Badge>
      </div>
      
      <div className="flex items-center gap-2">
        <span className="text-muted-foreground">必需服务:</span>
        <Badge variant={meetsRequirements ? "default" : "destructive"}>
          {selectedRequiredCount} / {requiredCount}
        </Badge>
      </div>
      
      {(filteredServices?.length || 0) < totalServices && (
        <div className="flex items-center gap-2">
          <span className="text-muted-foreground">显示:</span>
          <Badge variant="outline">{filteredServices?.length || 0}</Badge>
        </div>
      )}
      
      {meetsRequirements && (
        <Badge variant="default" className="text-green-600">
          ✓ 可部署
        </Badge>
      )}
    </div>
  )
}

// 紧凑版统计 - 用于狭小空间
export const CompactServiceStats: React.FC<ServiceStatsProps> = ({
  selectedServices = [],
  requiredServices = []
}) => {
  const selectedCount = selectedServices?.length || 0
  const requiredCount = requiredServices?.length || 0
  const selectedRequiredCount = selectedServices?.filter(s => s?.isRequired)?.length || 0
  const meetsRequirements = selectedRequiredCount === requiredCount

  return (
    <div className="flex items-center gap-2 text-xs">
      <Badge variant="secondary" className="text-xs">
        {selectedCount} 已选
      </Badge>
      <Badge variant={meetsRequirements ? "default" : "destructive"} className="text-xs">
        {selectedRequiredCount}/{requiredCount} 必需
      </Badge>
    </div>
  )
}

export default ServiceStats