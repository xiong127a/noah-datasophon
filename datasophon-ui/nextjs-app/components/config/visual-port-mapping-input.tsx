"use client"

import React, { useState, useCallback } from 'react'
import { Plus, X, ArrowRight, Container, Globe, Server } from 'lucide-react'
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

/**
 * 可视化端口映射输入组件
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 * 日期：2024-01-31
 * 
 * 特点：
 * - 直观的端口映射可视化界面
 * - 清晰的源端口→目标端口指示
 * - 端口类型标签和颜色编码
 * - 专业的映射方向展示
 */

export interface PortMapping {
  sourcePort: string
  targetPort: string
}

export type PortMappingType = 'node_port' | 'cluster_port' | 'load_balancer'

export interface VisualPortMappingInputProps {
  value?: PortMapping[] | any[]
  onChange?: (value: PortMapping[]) => void
  disabled?: boolean
  className?: string
  mappingType: PortMappingType
  minItems?: number
  maxItems?: number
}

const VisualPortMappingInput: React.FC<VisualPortMappingInputProps> = ({
  value = [],
  onChange,
  disabled = false,
  className,
  mappingType,
  minItems = 0,
  maxItems = 20
}) => {
  // 获取端口映射类型配置
  const getMappingConfig = (type: PortMappingType) => {
    switch (type) {
      case 'node_port':
        return {
          sourceLabel: '容器端口',
          targetLabel: '节点端口',
          sourceIcon: Container,
          targetIcon: Globe,
          sourceColor: 'border-blue-300 bg-blue-50/80 text-blue-700',
          targetColor: 'border-orange-300 bg-orange-50/80 text-orange-700',
          arrowColor: 'text-orange-500',
          sourcePlaceholder: '8080',
          targetPlaceholder: '30080'
        }
      case 'cluster_port':
        return {
          sourceLabel: '源端口',
          targetLabel: '目标端口',
          sourceIcon: Server,
          targetIcon: Server,
          sourceColor: 'border-indigo-300 bg-indigo-50/80 text-indigo-700',
          targetColor: 'border-blue-300 bg-blue-50/80 text-blue-700',
          arrowColor: 'text-blue-500',
          sourcePlaceholder: '80',
          targetPlaceholder: '8080'
        }
      case 'load_balancer':
        return {
          sourceLabel: '外部端口',
          targetLabel: '服务端口',
          sourceIcon: Globe,
          targetIcon: Container,
          sourceColor: 'border-emerald-300 bg-emerald-50/80 text-emerald-700',
          targetColor: 'border-green-300 bg-green-50/80 text-green-700',
          arrowColor: 'text-green-500',
          sourcePlaceholder: '443',
          targetPlaceholder: '8443'
        }
      default:
        return {
          sourceLabel: '源端口',
          targetLabel: '目标端口',
          sourceIcon: Server,
          targetIcon: Server,
          sourceColor: 'border-gray-300 bg-gray-50/80 text-gray-700',
          targetColor: 'border-gray-300 bg-gray-50/80 text-gray-700',
          arrowColor: 'text-gray-500',
          sourcePlaceholder: '8080',
          targetPlaceholder: '80'
        }
    }
  }

  const config = getMappingConfig(mappingType)
  const SourceIcon = config.sourceIcon
  const TargetIcon = config.targetIcon

  // 规范化输入值
  const normalizeValue = useCallback((inputValue: any[]): PortMapping[] => {
    if (!Array.isArray(inputValue)) {
      return []
    }
    
    return inputValue.map(item => {
      // 如果是 {sourcePort: "xx", targetPort: "yy"} 格式
      if ('sourcePort' in item && 'targetPort' in item) {
        return { 
          sourcePort: item.sourcePort || '', 
          targetPort: item.targetPort || '' 
        }
      }
      
      // 如果是 {key: "xx", value: "yy"} 格式，转换为端口映射格式
      if ('key' in item && 'value' in item) {
        return { 
          sourcePort: item.key || '', 
          targetPort: item.value || '' 
        }
      }
      
      // 如果是 {"8080": "30080"} 格式，转换为标准格式
      const entries = Object.entries(item)
      if (entries.length > 0) {
        const [key, value] = entries[0]
        return { 
          sourcePort: key || '', 
          targetPort: String(value) || '' 
        }
      }
      
      return { sourcePort: '', targetPort: '' }
    })
  }, [])

  const [items, setItems] = useState<PortMapping[]>(() => {
    const normalized = normalizeValue(value)
    return normalized.length === 0 ? [{ sourcePort: '', targetPort: '' }] : normalized
  })

  // 同步外部值变化
  React.useEffect(() => {
    const normalized = normalizeValue(value)
    
    if (normalized.length > 0) {
      setItems(normalized)
    } else if (normalized.length === 0 && value.length === 0) {
      setItems([{ sourcePort: '', targetPort: '' }])
    }
  }, [value, normalizeValue])

  // 触发onChange，转换为键值对格式以兼容现有接口
  const triggerChange = useCallback((newItems: PortMapping[]) => {
    // 只有包含实际内容的项才传递给父组件
    const filteredItems = newItems.filter(item => 
      item.sourcePort.trim() || item.targetPort.trim()
    )
    
    // 转换为键值对格式以兼容现有接口
    const keyValuePairs = filteredItems.map(item => ({
      key: item.sourcePort,
      value: item.targetPort
    }))
    
    // 使用setTimeout延迟到下一个事件循环，避免渲染期间的状态更新
    setTimeout(() => {
      onChange?.(keyValuePairs as any)
    }, 0)
  }, [onChange])

  // 添加新的端口映射
  const addItem = useCallback(() => {
    setItems(currentItems => {
      if (currentItems.length >= maxItems) return currentItems
      
      const newItems = [...currentItems, { sourcePort: '', targetPort: '' }]
      // 添加新项时不立即触发onChange，避免空项被过滤掉
      return newItems
    })
  }, [maxItems])

  // 删除端口映射
  const removeItem = useCallback((index: number) => {
    setItems(currentItems => {
      if (currentItems.length <= Math.max(1, minItems)) return currentItems
      
      const newItems = currentItems.filter((_, i) => i !== index)
      // 删除时要触发onChange，更新父组件状态
      triggerChange(newItems)
      return newItems
    })
  }, [minItems, triggerChange])

  // 更新源端口
  const updateSourcePort = useCallback((index: number, sourcePort: string) => {
    setItems(currentItems => {
      const newItems = [...currentItems]
      newItems[index] = { ...newItems[index], sourcePort }
      triggerChange(newItems)
      return newItems
    })
  }, [triggerChange])

  // 更新目标端口
  const updateTargetPort = useCallback((index: number, targetPort: string) => {
    setItems(currentItems => {
      const newItems = [...currentItems]
      newItems[index] = { ...newItems[index], targetPort }
      triggerChange(newItems)
      return newItems
    })
  }, [triggerChange])

  return (
    <div className={cn("space-y-4", className)}>
      {items.map((item, index) => (
        <div key={index} className="relative">
          {/* 端口映射卡片 */}
          <div className="
            p-4 rounded-2xl border border-gray-200/60 
            bg-gradient-to-r from-white/90 via-gray-50/30 to-white/90
            shadow-sm hover:shadow-md transition-all duration-300
            backdrop-blur-sm
          ">
            {/* 映射关系标题 */}
            <div className="flex items-center justify-center mb-3">
              <div className="flex items-center gap-2 text-xs font-medium text-gray-600">
                <SourceIcon className="h-3 w-3" />
                <span>{config.sourceLabel}</span>
                <ArrowRight className={`h-3 w-3 ${config.arrowColor} mx-1`} />
                <span>{config.targetLabel}</span>
                <TargetIcon className="h-3 w-3" />
              </div>
            </div>
            
            {/* 端口输入区域 */}
            <div className="flex items-center gap-4">
              {/* 源端口输入 */}
              <div className="flex-1">
                <div className="relative">
                  <div className={`
                    absolute left-3 top-1/2 transform -translate-y-1/2 
                    px-2 py-0.5 rounded-md text-xs font-medium
                    ${config.sourceColor}
                  `}>
                    <SourceIcon className="h-3 w-3 inline mr-1" />
                    {config.sourceLabel}
                  </div>
                  <Input
                    value={item.sourcePort}
                    onChange={(e) => updateSourcePort(index, e.target.value)}
                    placeholder={config.sourcePlaceholder}
                    disabled={disabled}
                    className="
                      pl-20 h-11 text-center font-mono text-sm
                      border-gray-200/60 focus:border-blue-400 focus:ring-blue-400/20 
                      hover:border-blue-300/60 transition-all duration-300 
                      shadow-sm hover:shadow-lg bg-white/80 backdrop-blur-sm rounded-xl
                    "
                  />
                </div>
              </div>
              
              {/* 映射箭头 */}
              <div className="flex items-center justify-center">
                <div className={`
                  p-2 rounded-xl bg-gradient-to-r from-gray-50 to-white
                  border border-gray-200/40 shadow-sm
                `}>
                  <ArrowRight className={`h-5 w-5 ${config.arrowColor}`} />
                </div>
              </div>
              
              {/* 目标端口输入 */}
              <div className="flex-1">
                <div className="relative">
                  <Input
                    value={item.targetPort}
                    onChange={(e) => updateTargetPort(index, e.target.value)}
                    placeholder={config.targetPlaceholder}
                    disabled={disabled}
                    className="
                      pr-20 h-11 text-center font-mono text-sm
                      border-gray-200/60 focus:border-blue-400 focus:ring-blue-400/20 
                      hover:border-blue-300/60 transition-all duration-300 
                      shadow-sm hover:shadow-lg bg-white/80 backdrop-blur-sm rounded-xl
                    "
                  />
                  <div className={`
                    absolute right-3 top-1/2 transform -translate-y-1/2 
                    px-2 py-0.5 rounded-md text-xs font-medium
                    ${config.targetColor}
                  `}>
                    {config.targetLabel}
                    <TargetIcon className="h-3 w-3 inline ml-1" />
                  </div>
                </div>
              </div>
              
              {/* 删除按钮 */}
              {items.length > Math.max(1, minItems) && (
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={() => removeItem(index)}
                  disabled={disabled}
                  className="h-9 w-9 p-0 text-red-500 hover:text-red-700 hover:bg-red-50 rounded-xl"
                >
                  <X className="h-4 w-4" />
                </Button>
              )}
            </div>
          </div>
        </div>
      ))}
      
      {/* 添加按钮 */}
      {items.length < maxItems && (
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={addItem}
          disabled={disabled}
          className="
            w-full text-blue-600 border-blue-200 hover:bg-blue-50 hover:border-blue-300 
            border-dashed rounded-xl h-11 font-medium
            transition-all duration-300 hover:shadow-md
          "
        >
          <Plus className="h-4 w-4 mr-2" />
          添加端口映射
        </Button>
      )}
      
      {/* 空状态提示 */}
      {items.length === 0 && (
        <div className="text-center text-sm text-gray-500 py-8">
          暂无端口映射配置
        </div>
      )}
    </div>
  )
}

export default VisualPortMappingInput
