"use client"

import React, { useState, useCallback } from 'react'
import { Plus, Minus, ArrowRight } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Separator } from '@/components/ui/separator'

interface PortMapping {
  key: string    // 容器端口/配置名
  value: string  // 节点端口/配置值
}

interface PortMappingInputProps {
  value: PortMapping[]
  onChange: (value: PortMapping[]) => void
  type: 'nodePort' | 'clusterIP' | 'loadBalancer' | 'custom'
  error?: string
}

export function PortMappingInput({
  value = [],
  onChange,
  type,
  error
}: PortMappingInputProps) {
  // 确保value至少有一个空的端口映射
  const mappings = Array.isArray(value) && value.length > 0 ? value : [{ key: '', value: '' }]

  // 根据类型获取标签和占位符
  const getLabelsAndPlaceholders = () => {
    switch (type) {
      case 'nodePort':
        return {
          leftLabel: '容器端口',
          rightLabel: '节点端口',
          leftPlaceholder: '容器内部端口',
          rightPlaceholder: '节点暴露端口',
          addButtonText: '添加NodePort端口映射',
          leftColor: 'blue',
          rightColor: 'green'
        }
      case 'clusterIP':
        return {
          leftLabel: '集群内部端口',
          rightLabel: '集群端口',
          leftPlaceholder: '集群内部端口',
          rightPlaceholder: '集群端口',
          addButtonText: '添加集群内部端口',
          leftColor: 'blue',
          rightColor: 'green'
        }
      case 'loadBalancer':
        return {
          leftLabel: '容器端口',
          rightLabel: '负载均衡器端口',
          leftPlaceholder: '容器内部端口',
          rightPlaceholder: '负载均衡器端口',
          addButtonText: '添加负载均衡器端口映射',
          leftColor: 'blue',
          rightColor: 'green'
        }
      case 'custom':
      default:
        return {
          leftLabel: '配置名',
          rightLabel: '配置值',
          leftPlaceholder: '请输入配置名',
          rightPlaceholder: '请输入配置值',
          addButtonText: '添加自定义配置',
          leftColor: 'blue',
          rightColor: 'green'
        }
    }
  }

  const config = getLabelsAndPlaceholders()

  // 更新指定索引的左侧值（端口/配置名）
  const updateLeftValue = useCallback((index: number, key: string) => {
    const newMappings = [...mappings]
    newMappings[index] = { ...newMappings[index], key }
    onChange(newMappings)
  }, [mappings, onChange])

  // 更新指定索引的右侧值（端口/配置值）
  const updateRightValue = useCallback((index: number, val: string) => {
    const newMappings = [...mappings]
    newMappings[index] = { ...newMappings[index], value: val }
    onChange(newMappings)
  }, [mappings, onChange])

  // 添加新的端口映射
  const addMapping = useCallback(() => {
    onChange([...mappings, { key: '', value: '' }])
  }, [mappings, onChange])

  // 删除指定索引的端口映射
  const removeMapping = useCallback((index: number) => {
    if (mappings.length > 1) {
      const newMappings = mappings.filter((_, i) => i !== index)
      onChange(newMappings)
    }
  }, [mappings, onChange])

  // 获取颜色类名
  const getColorClasses = (color: string, isInput = false) => {
    const baseClasses = isInput ? 'border focus:border' : ''
    switch (color) {
      case 'blue':
        return isInput 
          ? `${baseClasses}-blue-200 focus:border-blue-400 bg-blue-50/30`
          : 'text-blue-600'
      case 'green':
        return isInput 
          ? `${baseClasses}-green-200 focus:border-green-400 bg-green-50/30`
          : 'text-green-600'
      default:
        return isInput ? `${baseClasses}-gray-300` : 'text-gray-600'
    }
  }

  return (
    <div className="space-y-4">
      {mappings.map((mapping, index) => (
        <div key={index} className="space-y-3">
          {/* 第一行显示标签 */}
          {index === 0 && (
            <div className="flex items-center justify-between">
              <div className="flex gap-4 flex-1">
                <div className="flex-1">
                  <Label className={`text-sm font-medium ${getColorClasses(config.leftColor)}`}>
                    {config.leftLabel}
                  </Label>
                </div>
                <div className="flex-1 ml-8"> {/* 增加左边距以对齐箭头 */}
                  <Label className={`text-sm font-medium ${getColorClasses(config.rightColor)}`}>
                    {config.rightLabel}
                  </Label>
                </div>
              </div>
              <div className="w-10"></div> {/* 占位符，对齐删除按钮 */}
            </div>
          )}

          {/* 端口映射输入行 */}
          <div className="flex items-center gap-2">
            {/* 左侧输入框 */}
            <div className="flex-1">
              <Input
                value={mapping.key || ''}
                onChange={(e) => updateLeftValue(index, e.target.value)}
                placeholder={config.leftPlaceholder}
                className={getColorClasses(config.leftColor, true)}
                type={type !== 'custom' ? 'number' : 'text'}
              />
            </div>
            
            {/* 增强版箭头 */}
            <div className="flex items-center justify-center w-8 h-8">
              <div className="enhanced-arrow-container">
                <div className="enhanced-arrow-line">
                  <div className="enhanced-flow-effect"></div>
                </div>
                <div className="enhanced-arrow-head"></div>
              </div>
            </div>
            
            {/* 右侧输入框 */}
            <div className="flex-1">
              <Input
                value={mapping.value || ''}
                onChange={(e) => updateRightValue(index, e.target.value)}
                placeholder={config.rightPlaceholder}
                className={getColorClasses(config.rightColor, true)}
                type={type !== 'custom' ? 'number' : 'text'}
              />
            </div>

            {/* 删除按钮 */}
            {mappings.length > 1 && (
              <Button
                type="button"
                size="sm"
                variant="outline"
                onClick={() => removeMapping(index)}
                className="w-8 h-8 p-0 text-red-500 hover:text-red-700 hover:bg-red-50 border-red-200"
              >
                <Minus className="w-4 h-4" />
              </Button>
            )}
            
            {/* 占位符，保持对齐 */}
            {mappings.length === 1 && (
              <div className="w-8 h-8"></div>
            )}
          </div>
        </div>
      ))}
      
      {/* 添加按钮 */}
      <div className="pt-2">
        <Button
          type="button"
          size="sm"
          variant="outline"
          onClick={addMapping}
          className={`${getColorClasses(config.rightColor)} hover:bg-${config.rightColor === 'green' ? 'green' : 'blue'}-50 border-${config.rightColor === 'green' ? 'green' : 'blue'}-200`}
        >
          <Plus className="w-4 h-4 mr-1" />
          {config.addButtonText}
        </Button>
      </div>

      {/* 端口映射分隔线 */}
      {(type === 'nodePort' || type === 'loadBalancer') && (
        <div className="pt-4">
          <Separator className="border-dashed" />
        </div>
      )}

      {/* 错误信息 */}
      {error && (
        <div className="text-xs text-red-500 mt-1">{error}</div>
      )}

      {/* 内联样式 */}
      <style jsx>{`
        .enhanced-arrow-container {
          width: 100%;
          height: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          position: relative;
        }

        .enhanced-arrow-line {
          height: 3px;
          width: 100%;
          background-color: rgba(24, 144, 255, 0.4);
          position: relative;
          overflow: hidden;
          flex: 1;
          box-shadow: 0 0 3px rgba(24, 144, 255, 0.5);
          border-radius: 1.5px;
        }

        .enhanced-flow-effect {
          position: absolute;
          top: 0;
          height: 100%;
          width: 50px;
          background: linear-gradient(to right, rgba(24, 144, 255, 0), rgba(24, 144, 255, 1), rgba(24, 144, 255, 0));
          animation: enhanced-flow-animation 1s infinite linear;
          box-shadow: 0 0 15px rgba(24, 144, 255, 0.9);
          filter: blur(0.5px);
        }

        .enhanced-arrow-head {
          width: 0;
          height: 0;
          border-top: 8px solid transparent;
          border-bottom: 8px solid transparent;
          border-left: 12px solid rgba(24, 144, 255, 0.8);
          margin-left: 0;
          filter: drop-shadow(0 0 4px rgba(24, 144, 255, 0.9));
        }

        @keyframes enhanced-flow-animation {
          0% {
            left: -50px;
            opacity: 0.7;
          }
          50% {
            opacity: 1;
          }
          100% {
            left: 100%;
            opacity: 0.7;
          }
        }
      `}</style>
    </div>
  )
}
