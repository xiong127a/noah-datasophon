"use client"

import React from 'react'
import { CheckCircle2, Clock, Settings, AlertTriangle } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Progress } from '@/components/ui/progress'
// import { ScrollArea } from '@/components/ui/scroll-area' // 暂时注释，使用div代替

import type { ServiceTemplate, FormData } from '@/types/service-config'

interface ConfigurationSummaryProps {
  services: string[]
  configurationStatus: Record<string, 'pending' | 'configured' | 'saved'>
  serviceTemplates: ServiceTemplate
  formData: FormData
}

/**
 * 配置摘要组件 - 现代化设计
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 */

const ConfigurationSummary: React.FC<ConfigurationSummaryProps> = ({
  services,
  configurationStatus,
  serviceTemplates,
  formData
}) => {
  // 计算配置统计
  const getConfigStats = () => {
    const stats = {
      total: services.length,
      pending: 0,
      configured: 0,
      saved: 0
    }
    
    Object.values(configurationStatus).forEach(status => {
      stats[status]++
    })
    
    return stats
  }

  // 获取服务的配置项数量
  const getServiceConfigCount = (serviceName: string) => {
    return serviceTemplates[serviceName]?.length || 0
  }

  // 获取服务的已配置项数量
  const getConfiguredCount = (serviceName: string) => {
    const configs = serviceTemplates[serviceName] || []
    return configs.filter(config => {
      const fieldName = (config.name || '').replace(/\./g, '!')
      const value = formData[fieldName]
      return value !== undefined && value !== '' && value !== null
    }).length
  }

  // 获取状态信息
  const getStatusInfo = (status: 'pending' | 'configured' | 'saved') => {
    switch (status) {
      case 'saved':
        return {
          icon: CheckCircle2,
          color: 'text-green-600',
          bgColor: 'bg-green-50',
          label: '已保存'
        }
      case 'configured':
        return {
          icon: Settings,
          color: 'text-blue-600',
          bgColor: 'bg-blue-50',
          label: '已配置'
        }
      default:
        return {
          icon: Clock,
          color: 'text-gray-400',
          bgColor: 'bg-gray-50',
          label: '待配置'
        }
    }
  }

  const stats = getConfigStats()
  const overallProgress = stats.total > 0 ? ((stats.configured + stats.saved) / stats.total) * 100 : 0

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg font-semibold flex items-center gap-2">
          <Settings className="h-5 w-5 text-blue-600" />
          配置摘要
        </CardTitle>
      </CardHeader>
      
      <CardContent className="space-y-6">
        {/* 整体进度 */}
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <h4 className="font-medium text-gray-900">整体配置进度</h4>
            <span className="text-sm text-gray-500">
              {Math.round(overallProgress)}% 完成
            </span>
          </div>
          <Progress value={overallProgress} className="h-2" />
          
          <div className="flex items-center gap-4 text-sm">
            <div className="flex items-center gap-1">
              <div className="w-2 h-2 rounded-full bg-green-500"></div>
              <span>已保存: {stats.saved}</span>
            </div>
            <div className="flex items-center gap-1">
              <div className="w-2 h-2 rounded-full bg-blue-500"></div>
              <span>已配置: {stats.configured}</span>
            </div>
            <div className="flex items-center gap-1">
              <div className="w-2 h-2 rounded-full bg-gray-400"></div>
              <span>待配置: {stats.pending}</span>
            </div>
          </div>
        </div>

        {/* 服务详情 */}
        <div className="space-y-3">
          <h4 className="font-medium text-gray-900">服务配置详情</h4>
          
          <div className="h-64 overflow-y-auto">
            <div className="space-y-3 pr-4">
              {services.map(serviceName => {
                const status = configurationStatus[serviceName] || 'pending'
                const statusInfo = getStatusInfo(status)
                const StatusIcon = statusInfo.icon
                const totalConfigs = getServiceConfigCount(serviceName)
                const configuredConfigs = getConfiguredCount(serviceName)
                const configProgress = totalConfigs > 0 ? (configuredConfigs / totalConfigs) * 100 : 0

                return (
                  <div key={serviceName} className={`p-3 rounded-lg border ${statusInfo.bgColor}`}>
                    <div className="flex items-center justify-between mb-2">
                      <div className="flex items-center gap-2">
                        <StatusIcon className={`h-4 w-4 ${statusInfo.color}`} />
                        <span className="font-medium text-gray-900">{serviceName}</span>
                        <Badge variant="outline" className="text-xs">
                          {statusInfo.label}
                        </Badge>
                      </div>
                      
                      <span className="text-sm text-gray-500">
                        {configuredConfigs}/{totalConfigs} 项
                      </span>
                    </div>
                    
                    {totalConfigs > 0 && (
                      <div className="space-y-1">
                        <Progress value={configProgress} className="h-1" />
                        <p className="text-xs text-gray-500">
                          配置完成度: {Math.round(configProgress)}%
                        </p>
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          </div>
        </div>

        {/* 配置提醒 */}
        {stats.pending > 0 && (
          <div className="p-3 bg-amber-50 border border-amber-200 rounded-lg">
            <div className="flex items-start gap-2">
              <AlertTriangle className="h-4 w-4 text-amber-600 mt-0.5 flex-shrink-0" />
              <div className="text-sm">
                <p className="font-medium text-amber-800">配置提醒</p>
                <p className="text-amber-700 mt-1">
                  还有 {stats.pending} 个服务未完成配置，请逐一配置后再继续下一步。
                </p>
              </div>
            </div>
          </div>
        )}
        
        {stats.pending === 0 && (
          <div className="p-3 bg-green-50 border border-green-200 rounded-lg">
            <div className="flex items-start gap-2">
              <CheckCircle2 className="h-4 w-4 text-green-600 mt-0.5 flex-shrink-0" />
              <div className="text-sm">
                <p className="font-medium text-green-800">配置完成</p>
                <p className="text-green-700 mt-1">
                  所有服务配置已完成，可以继续下一步操作。
                </p>
              </div>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  )
}

export default ConfigurationSummary
