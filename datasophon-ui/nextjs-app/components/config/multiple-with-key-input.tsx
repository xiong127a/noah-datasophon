"use client"

import React, { useState, useCallback } from 'react'
import { Plus, X } from 'lucide-react'
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

/**
 * 键值对类型配置输入组件
 * 支持动态添加/删除键值对
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 * 日期：2024-01-20
 */

export interface KeyValuePair {
  key: string
  value: string
}

export interface MultipleWithKeyInputProps {
  value?: KeyValuePair[] | Record<string, string>[]
  onChange?: (value: KeyValuePair[]) => void
  disabled?: boolean
  placeholder?: string
  className?: string
  keyPlaceholder?: string
  valuePlaceholder?: string
  minItems?: number
  maxItems?: number
}

const MultipleWithKeyInput: React.FC<MultipleWithKeyInputProps> = ({
  value = [],
  onChange,
  disabled = false,
  placeholder = "请输入键值对",
  className,
  keyPlaceholder = "键",
  valuePlaceholder = "值", 
  minItems = 0,
  maxItems = 20
}) => {
  // 规范化输入值为统一格式
  const normalizeValue = useCallback((inputValue: KeyValuePair[] | Record<string, string>[]): KeyValuePair[] => {
    if (!Array.isArray(inputValue)) {
      return []
    }
    
    return inputValue.map(item => {
      // 如果是 {key: "xx", value: "yy"} 格式
      if ('key' in item && 'value' in item) {
        return { key: item.key || '', value: item.value || '' }
      }
      
      // 如果是 {"port": "9090"} 格式，转换为标准格式
      const entries = Object.entries(item)
      if (entries.length > 0) {
        const [key, value] = entries[0]
        return { key: key || '', value: String(value) || '' }
      }
      
      return { key: '', value: '' }
    })
  }, [])

  const [items, setItems] = useState<KeyValuePair[]>(() => {
    const normalized = normalizeValue(value)
    return normalized.length === 0 ? [{ key: '', value: '' }] : normalized
  })

  // 同步外部值变化 - 移除items依赖避免无限循环
  React.useEffect(() => {
    const normalized = normalizeValue(value)
    
    // 仅在外部值有效且与当前状态不同时更新
    if (normalized.length > 0) {
      setItems(normalized)
    } else if (normalized.length === 0 && value.length === 0) {
      // 外部值为空时，确保至少有一个空项
      setItems([{ key: '', value: '' }])
    }
  }, [value, normalizeValue]) // 仅依赖value和normalizeValue



  // 触发onChange的安全方法，避免渲染期间的状态更新
  const triggerChange = useCallback((items: KeyValuePair[]) => {
    // 只有包含实际内容的项才传递给父组件
    const filteredItems = items.filter(item => item.key.trim() || item.value.trim())
    // 使用setTimeout延迟到下一个事件循环，避免渲染期间的状态更新
    setTimeout(() => {
      onChange?.(filteredItems)
    }, 0)
  }, [onChange])

  // 添加新的键值对 - 使用函数式更新避免items依赖
  const addItem = useCallback(() => {
    setItems(currentItems => {
      if (currentItems.length >= maxItems) return currentItems
      
      const newItems = [...currentItems, { key: '', value: '' }]
      // 添加新项时不立即触发onChange，避免空项被过滤掉
      return newItems
    })
  }, [maxItems])

  // 删除键值对 - 使用函数式更新避免items依赖  
  const removeItem = useCallback((index: number) => {
    setItems(currentItems => {
      if (currentItems.length <= Math.max(1, minItems)) return currentItems
      
      const newItems = currentItems.filter((_, i) => i !== index)
      // 删除时要触发onChange，更新父组件状态
      triggerChange(newItems)
      return newItems
    })
  }, [minItems, triggerChange])

  // 更新键值对的键 - 使用函数式更新避免items依赖
  const updateKey = useCallback((index: number, key: string) => {
    setItems(currentItems => {
      const newItems = [...currentItems]
      newItems[index] = { ...newItems[index], key }
      triggerChange(newItems)
      return newItems
    })
  }, [triggerChange])

  // 更新键值对的值 - 使用函数式更新避免items依赖
  const updateValue = useCallback((index: number, value: string) => {
    setItems(currentItems => {
      const newItems = [...currentItems]
      newItems[index] = { ...newItems[index], value }
      triggerChange(newItems)
      return newItems
    })
  }, [triggerChange])

  return (
    <div className={cn("space-y-3", className)}>
      {items.map((item, index) => (
        <div key={index} className="flex items-center gap-2">
          {/* 键输入框 */}
          <div className="flex-1">
            <Input
              value={item.key}
              onChange={(e) => updateKey(index, e.target.value)}
              placeholder={keyPlaceholder}
              disabled={disabled}
              className="text-sm"
            />
          </div>
          
          {/* 分隔符 */}
          <div className="text-gray-400 text-sm">:</div>
          
          {/* 值输入框 */}
          <div className="flex-1">
            <Input
              value={item.value}
              onChange={(e) => updateValue(index, e.target.value)}
              placeholder={valuePlaceholder}
              disabled={disabled}
              className="text-sm"
            />
          </div>
          
          {/* 删除按钮 */}
          {items.length > Math.max(1, minItems) && (
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => removeItem(index)}
              disabled={disabled}
              className="h-9 w-9 p-0 text-red-500 hover:text-red-700 hover:bg-red-50"
            >
              <X className="h-4 w-4" />
            </Button>
          )}
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
          className="w-full text-blue-600 border-blue-200 hover:bg-blue-50 hover:border-blue-300 border-dashed"
        >
          <Plus className="h-4 w-4 mr-1" />
          添加键值对
        </Button>
      )}
      
      {/* 提示信息 */}
      {items.length === 0 && (
        <div className="text-center text-sm text-gray-500 py-4">
          {placeholder}
        </div>
      )}
    </div>
  )
}

export default MultipleWithKeyInput
