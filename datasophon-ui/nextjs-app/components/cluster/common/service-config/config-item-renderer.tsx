"use client"

import React from 'react'
import { AlertTriangle } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Checkbox } from '@/components/ui/checkbox'
import { Alert, AlertDescription } from '@/components/ui/alert'
import AppleTooltip from '@/components/ui/apple-tooltip'
import MultipleWithKeyInput from '@/components/config/multiple-with-key-input'
import PortMappingInput, { type PortMappingType } from '@/components/config/port-mapping-input'

import type { ConfigItem } from '@/types/service-config'
import { ConfigType } from '@/types/service-config'

interface ConfigItemRendererProps {
  config: ConfigItem
  value: unknown
  error?: string
  onChange: (value: unknown) => void
}

/**
 * 配置项渲染器 - 现代化设计
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 */

const ConfigItemRenderer: React.FC<ConfigItemRendererProps> = ({
  config,
  value,
  error,
  onChange
}) => {
  // 检测是否为端口映射配置
  const detectPortMappingType = (configName: string): PortMappingType | null => {
    const name = configName.toLowerCase()
    if (name.includes('_node_port_mappings')) {
      return 'node_port'
    }
    if (name.includes('_cluster_port_mappings')) {
      return 'cluster_port'
    }
    if (name.includes('_load_balancer_port_mappings')) {
      return 'load_balancer'
    }
    return null
  }

  // 渲染不同类型的配置项
  const renderInput = () => {
    // 优先检测是否为Kubernetes端口映射配置
    const portMappingType = detectPortMappingType(config.name || '')
    if (portMappingType) {
      return (
        <PortMappingInput
          mappingType={portMappingType}
          value={value}
          onChange={onChange}
          disabled={config.disabled}
          className={error ? 'ring-2 ring-red-400/30 border-red-300' : ''}
        />
      )
    }

    const commonProps = {
      value: value as string,
      onChange: (newValue: unknown) => onChange(newValue),
      placeholder: config.placeholder || `请输入${config.label}`,
      disabled: config.disabled,
      className: error 
        ? 'border-red-400 focus:border-red-500 focus:ring-red-400/20 shadow-red-100/60 bg-red-50/30' 
        : 'border-gray-200/60 focus:border-blue-400 focus:ring-blue-400/20 hover:border-blue-300/60 transition-all duration-300 shadow-sm hover:shadow-lg bg-white/80 backdrop-blur-sm rounded-xl'
    }

    switch (config.type) {
      case ConfigType.SELECT:
        return (
          <Select value={value as string} onValueChange={onChange}>
            <SelectTrigger className={`${commonProps.className} h-10`}>
              <SelectValue placeholder={commonProps.placeholder} />
            </SelectTrigger>
            <SelectContent>
              {config.options?.map(option => (
                <SelectItem 
                  key={option.value} 
                  value={option.value}
                  title={option.description || ''}
                >
                  <div className="flex flex-col">
                    <span>{option.label}</span>
                    {option.description && (
                      <span className="text-xs text-gray-500 mt-0.5">{option.description}</span>
                    )}
                  </div>
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        )

      case ConfigType.SWITCH:
      case ConfigType.BOOLEAN:
        return (
          <div className="flex items-center space-x-3 p-2 rounded-lg hover:bg-gray-50 transition-colors duration-200">
            <Checkbox
              checked={value === true || value === 'true'}
              onCheckedChange={(checked) => onChange(checked)}
              disabled={commonProps.disabled}
              className={error ? 'border-red-500' : 'border-gray-300 data-[state=checked]:bg-blue-600 data-[state=checked]:border-blue-600'}
            />
            <span className="text-sm text-gray-700 font-medium">启用</span>
          </div>
        )

      case ConfigType.TEXTAREA:
        return (
          <Textarea
            {...commonProps}
            rows={4}
            className={`resize-none ${commonProps.className}`}
          />
        )

      case ConfigType.NUMBER:
        return (
          <div className="relative">
            <Input
              type="number"
              {...commonProps}
              min={config.minValue}
              max={config.maxValue}
              className={`${commonProps.className} ${config.unit ? 'pr-12' : ''}`}
            />
            {config.unit && (
              <span className="absolute right-3 top-1/2 transform -translate-y-1/2 text-sm text-gray-500 font-medium">
                {config.unit}
              </span>
            )}
          </div>
        )

      case ConfigType.PASSWORD:
        return (
          <Input
            type="password"
            {...commonProps}
          />
        )

      case ConfigType.MULTIPLE_WITH_KEY:
        return (
          <MultipleWithKeyInput
            value={value}
            onChange={onChange}
            disabled={commonProps.disabled}
            placeholder={commonProps.placeholder}
            className={commonProps.className}
          />
        )

      default:
        return (
          <Input
            {...commonProps}
          />
        )
    }
  }

  // 获取端口映射的友好显示名称
  const getPortMappingDisplayName = (mappingType: PortMappingType): string => {
    switch (mappingType) {
      case 'node_port':
        return 'NodePort 端口映射'
      case 'cluster_port':
        return '集群内部端口映射'
      case 'load_balancer':
        return '负载均衡端口映射'
      default:
        return '端口映射'
    }
  }

  // 构建详细信息的悬浮提示内容
  const buildTooltipContent = () => {
    const parts = []
    
    // 检测端口映射类型并优化显示
    const portMappingType = detectPortMappingType(config.name || '')
    if (portMappingType) {
      parts.push(`配置类型: ${getPortMappingDisplayName(portMappingType)}`)
      parts.push(`配置项: ${config.name}`)
      
      // 端口映射的特殊说明
      switch (portMappingType) {
        case 'node_port':
          parts.push(`说明: 将容器端口映射到集群节点的指定端口，支持外部访问`)
          parts.push(`格式: 容器端口:节点端口（如 8080:30080）`)
          break
        case 'cluster_port':
          parts.push(`说明: 配置集群内部服务间的端口映射关系`)
          parts.push(`格式: 源端口:目标端口（如 80:8080）`)
          break
        case 'load_balancer':
          parts.push(`说明: 配置负载均衡器的端口映射和流量分发规则`)
          parts.push(`格式: 外部端口:服务端口（如 443:8443）`)
          break
      }
    } else {
      // 普通配置项的信息
      if (config.name) {
        parts.push(`配置项: ${config.name}`)
      }
      
      // 配置类型
      if (config.type) {
        parts.push(`类型: ${config.type}`)
      }
      
      // 描述
      if (config.description) {
        parts.push(`说明: ${config.description}`)
      }
    }
    
    // 默认值
    if (config.defaultValue !== undefined && config.defaultValue !== '') {
      parts.push(`默认值: ${config.defaultValue}`)
    }
    
    // 取值范围
    if (config.minValue !== undefined || config.maxValue !== undefined) {
      parts.push(`取值范围: ${config.minValue ?? '无限制'} ~ ${config.maxValue ?? '无限制'}`)
    }
    
    // 单位
    if (config.unit) {
      parts.push(`单位: ${config.unit}`)
    }
    
    return parts.join('\n')
  }

  return (
    <div className="space-y-2">
      {/* 精美的苹果风格标签 */}
      <AppleTooltip 
        content={buildTooltipContent()}
        placement="top"
        maxWidth={700}
        showIcon={true}
      >
        <label className="text-sm font-medium text-gray-800 hover:text-blue-600 transition-colors duration-200">
          {(() => {
            // 为端口映射配置显示友好名称
            const portMappingType = detectPortMappingType(config.name || '')
            if (portMappingType) {
              return getPortMappingDisplayName(portMappingType)
            }
            return config.label || config.name
          })()}
          {config.required && <span className="text-red-500 ml-1">*</span>}
        </label>
      </AppleTooltip>

      {/* 输入控件 */}
      <div className="relative">
        {renderInput()}
      </div>

      {/* 错误信息 */}
      {error && (
        <Alert variant="destructive" className="py-2">
          <AlertTriangle className="h-4 w-4" />
          <AlertDescription className="text-sm">
            {error}
          </AlertDescription>
        </Alert>
      )}
    </div>
  )
}

export default ConfigItemRenderer
