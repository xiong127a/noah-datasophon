"use client"

import React, { useState, useCallback } from 'react'
import { Plus, Minus, Info } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Badge } from '@/components/ui/badge'
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip'
import MultipleWithKeyInput from './multiple-with-key-input'
import PortMappingInput from './port-mapping-input'

interface ConfigItem {
  name: string
  value: unknown
  type: string
  label?: string
  description?: string
  required?: boolean
  hidden?: boolean
  defaultValue?: unknown
  options?: Array<{ label: string; value: unknown }>
  selectValue?: string[]
  templateContent?: string
  minValue?: number
  maxValue?: number
  unit?: string
  placeholder?: string
  heightMultiple?: number
  configType?: string
}

interface ConfigItemRendererProps {
  item: ConfigItem
  value: unknown
  onChange: (value: unknown) => void
  error?: string
}

export function ConfigItemRenderer({ item, value, onChange, error }: ConfigItemRendererProps) {
  const [localValue, setLocalValue] = useState(value)

  // 处理值变化
  const handleChange = useCallback((newValue: unknown) => {
    setLocalValue(newValue)
    onChange(newValue)
  }, [onChange])



  // 渲染输入框
  const renderInput = () => {
    const isTextarea = item.heightMultiple && item.heightMultiple > 1
    
    const commonProps = {
      value: String(localValue || ''),
      onChange: (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => 
        handleChange(e.target.value),
      placeholder: item.placeholder || '请输入',
      className: error ? 'border-red-300 focus:border-red-400' : ''
    }

    if (isTextarea) {
      return (
        <Textarea
          {...commonProps}
          rows={item.heightMultiple}
          className={`resize-none ${commonProps.className}`}
        />
      )
    }

    return <Input {...commonProps} />
  }



  // 渲染开关
  const renderSwitch = () => (
    <button
      type="button"
      onClick={() => handleChange(!localValue)}
      className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 ${
        localValue ? 'bg-blue-600' : 'bg-gray-200'
      }`}
    >
      <span
        className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform duration-200 ease-in-out ${
          localValue ? 'translate-x-6' : 'translate-x-1'
        }`}
      />
    </button>
  )

  // 渲染下拉选择
  const renderSelect = () => (
    <Select value={String(localValue || '')} onValueChange={handleChange}>
      <SelectTrigger className={error ? 'border-red-300 focus:border-red-400' : ''}>
        <SelectValue placeholder="请选择" />
      </SelectTrigger>
      <SelectContent>
        {item.selectValue?.map((option, index) => (
          <SelectItem key={index} value={option}>
            {option}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )

  // 渲染多选下拉
  const renderMultipleSelect = () => {
    const selectedValues = Array.isArray(localValue) ? localValue : []
    
    return (
      <div className="space-y-2">
        <Select 
          onValueChange={(newValue) => {
            if (!selectedValues.includes(newValue)) {
              handleChange([...selectedValues, newValue])
            }
          }}
        >
          <SelectTrigger className={error ? 'border-red-300 focus:border-red-400' : ''}>
            <SelectValue placeholder="请选择" />
          </SelectTrigger>
          <SelectContent>
            {item.selectValue?.filter(option => !selectedValues.includes(option)).map((option, index) => (
              <SelectItem key={index} value={option}>
                {option}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        
        {/* 显示已选择的项 */}
        {selectedValues.length > 0 && (
          <div className="flex flex-wrap gap-2">
            {selectedValues.map((selected, index) => (
              <Badge key={index} variant="secondary" className="flex items-center gap-1">
                {selected}
                <button
                  type="button"
                  onClick={() => {
                    const newValues = selectedValues.filter(v => v !== selected)
                    handleChange(newValues)
                  }}
                  className="ml-1 hover:text-red-500"
                >
                  <Minus className="w-3 h-3" />
                </button>
              </Badge>
            ))}
          </div>
        )}
      </div>
    )
  }

  // 渲染滑块
  const renderSlider = () => {
    const numValue = parseFloat(String(localValue)) || 0
    const min = item.minValue || 0
    const max = item.maxValue || 100
    
    return (
      <div className="space-y-4">
        <Input
          type="number"
          value={numValue}
          onChange={(e) => handleChange(parseFloat(e.target.value) || 0)}
          min={min}
          max={max}
          className={error ? 'border-red-300 focus:border-red-400' : ''}
        />
        
        <div className="px-2">
          <input
            type="range"
            min={min}
            max={max}
            value={numValue}
            onChange={(e) => handleChange(parseFloat(e.target.value))}
            className="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer slider"
          />
          <div className="flex justify-between text-xs text-gray-500 mt-1">
            <span>{min}</span>
            <span>{max}</span>
          </div>
        </div>
      </div>
    )
  }

  // 渲染多个输入项
  const renderMultiple = () => {
    const values = Array.isArray(localValue) ? localValue : ['']
    
    return (
      <div className="space-y-3">
        {values.map((val, index) => (
          <div key={index} className="flex items-center gap-2">
            <Input
              value={val || ''}
              onChange={(e) => {
                const newValues = [...values]
                newValues[index] = e.target.value
                handleChange(newValues)
              }}
              placeholder="请输入"
              className={`flex-1 ${error ? 'border-red-300 focus:border-red-400' : ''}`}
            />
            {values.length > 1 && (
              <Button
                type="button"
                size="sm"
                variant="outline"
                onClick={() => {
                  const newValues = values.filter((_, i) => i !== index)
                  handleChange(newValues)
                }}
                className="text-red-500 hover:text-red-700 hover:bg-red-50"
              >
                <Minus className="w-4 h-4" />
              </Button>
            )}
          </div>
        ))}
        
        <Button
          type="button"
          size="sm"
          variant="outline"
          onClick={() => handleChange([...values, ''])}
          className="text-blue-600 hover:text-blue-700 hover:bg-blue-50"
        >
          <Plus className="w-4 h-4 mr-1" />
          添加属性
        </Button>
      </div>
    )
  }

  // 渲染键值对输入
  const renderMultipleWithKey = () => {
    // 检查是否是端口映射类型
    const isPortMapping = item.name.endsWith('_port_mappings') || 
                         item.name.endsWith('node_port_mappings') || 
                         item.name.endsWith('cluster_port_mappings') ||
                         item.name.endsWith('load_balancer_port_mappings')

    if (isPortMapping) {
      return (
        <PortMappingInput
          value={Array.isArray(localValue) ? localValue : []}
          onChange={handleChange}
          mappingType={
            item.name.endsWith('node_port_mappings') ? 'node_port' :
            item.name.endsWith('cluster_port_mappings') ? 'cluster_port' :
            'load_balancer'
          }
        />
      )
    }

    return (
      <MultipleWithKeyInput
        value={Array.isArray(localValue) ? localValue : []}
        onChange={handleChange}
      />
    )
  }

  // 根据类型渲染控件
  const renderControl = () => {
    switch (item.type) {
      case 'switch':
        return renderSwitch()
      case 'select':
        return renderSelect()
      case 'multipleSelect':
        return renderMultipleSelect()
      case 'slider':
        return renderSlider()
      case 'multiple':
        return renderMultiple()
      case 'multipleWithKey':
        return renderMultipleWithKey()
      case 'input':
      default:
        return renderInput()
    }
  }

  // 主渲染
  const control = renderControl()
  
  const content = (
    <div className="config-item space-y-2">
      <div className="flex items-center justify-between">
        <label className="text-sm font-medium text-gray-800 hover:text-blue-600 transition-colors duration-200">
          {item.label || item.name}
          {item.required && <span className="text-red-500 ml-1">*</span>}
        </label>
        {item.description && (
          <Info className="w-4 h-4 text-gray-400 cursor-help" />
        )}
      </div>
      
      <div className="control-wrapper">
        {control}
      </div>
      
      {error && (
        <div className="text-xs text-red-500 mt-1">{error}</div>
      )}
    </div>
  )

  if (item.description) {
    return (
      <TooltipProvider>
        <Tooltip>
          <TooltipTrigger asChild>
            {content}
          </TooltipTrigger>
          <TooltipContent className="max-w-sm">
            <div className="space-y-2">
              <div className="font-medium text-gray-900">
                {item.label || item.name}
              </div>
              <div className="text-sm text-gray-600">
                字段名: <code className="text-blue-600 bg-blue-50 px-1 rounded">
                  {item.name.replaceAll('!', '.')}
                </code>
              </div>
              <div className="text-sm text-gray-700">
                {item.description}
              </div>
            </div>
          </TooltipContent>
        </Tooltip>
      </TooltipProvider>
    )
  }

  return content
}

// 样式
const styles = `
.slider {
  background: linear-gradient(to right, #3b82f6 0%, #3b82f6 var(--value, 0%), #e5e7eb var(--value, 0%), #e5e7eb 100%);
}

.slider::-webkit-slider-thumb {
  appearance: none;
  height: 20px;
  width: 20px;
  border-radius: 50%;
  background: #3b82f6;
  cursor: pointer;
  border: 2px solid #ffffff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.slider::-moz-range-thumb {
  height: 20px;
  width: 20px;
  border-radius: 50%;
  background: #3b82f6;
  cursor: pointer;
  border: 2px solid #ffffff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}
`

// 注入样式
if (typeof document !== 'undefined' && !document.getElementById('config-item-styles')) {
  const styleSheet = document.createElement('style')
  styleSheet.id = 'config-item-styles'
  styleSheet.textContent = styles
  document.head.appendChild(styleSheet)
}