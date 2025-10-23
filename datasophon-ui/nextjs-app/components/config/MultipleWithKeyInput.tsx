"use client"

import React, { useState, useCallback } from 'react'
import { Plus, Minus } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

interface KeyValuePair {
  key: string
  value: string
}

interface MultipleWithKeyInputProps {
  value: KeyValuePair[]
  onChange: (value: KeyValuePair[]) => void
  keyLabel?: string
  valueLabel?: string
  keyPlaceholder?: string
  valuePlaceholder?: string
  addButtonText?: string
  required?: boolean
  error?: string
}

export function MultipleWithKeyInput({
  value = [],
  onChange,
  keyLabel = '键',
  valueLabel = '值',
  keyPlaceholder = '请输入键',
  valuePlaceholder = '请输入值',
  addButtonText = '添加键值对',
  required = false,
  error
}: MultipleWithKeyInputProps) {
  // 确保value至少有一个空的键值对
  const values = Array.isArray(value) && value.length > 0 ? value : [{ key: '', value: '' }]

  // 更新指定索引的键
  const updateKey = useCallback((index: number, key: string) => {
    const newValues = [...values]
    newValues[index] = { ...newValues[index], key }
    onChange(newValues)
  }, [values, onChange])

  // 更新指定索引的值
  const updateValue = useCallback((index: number, val: string) => {
    const newValues = [...values]
    newValues[index] = { ...newValues[index], value: val }
    onChange(newValues)
  }, [values, onChange])

  // 添加新的键值对
  const addItem = useCallback(() => {
    onChange([...values, { key: '', value: '' }])
  }, [values, onChange])

  // 删除指定索引的键值对
  const removeItem = useCallback((index: number) => {
    if (values.length > 1) {
      const newValues = values.filter((_, i) => i !== index)
      onChange(newValues)
    }
  }, [values, onChange])

  return (
    <div className="space-y-3">
      {values.map((item, index) => (
        <div key={index} className="space-y-2">
          {/* 第一行显示标签 */}
          {index === 0 && (
            <div className="flex items-center justify-between">
              <div className="flex gap-4 flex-1">
                <div className="flex-1">
                  <Label className="text-sm font-medium text-blue-600">
                    {keyLabel}
                  </Label>
                </div>
                <div className="flex-1">
                  <Label className="text-sm font-medium text-green-600">
                    {valueLabel}
                  </Label>
                </div>
              </div>
              <div className="w-10"></div> {/* 占位符，对齐删除按钮 */}
            </div>
          )}

          {/* 键值对输入行 */}
          <div className="flex items-center gap-2">
            <div className="flex-1">
              <Input
                value={item.key || ''}
                onChange={(e) => updateKey(index, e.target.value)}
                placeholder={keyPlaceholder}
                className={`${
                  error && required && !item.key 
                    ? 'border-red-300 focus:border-red-400' 
                    : 'border-blue-200 focus:border-blue-400'
                } bg-blue-50/30`}
              />
            </div>
            
            {/* 箭头指示器 */}
            <div className="flex items-center justify-center w-8 h-8">
              <div className="w-6 h-0.5 bg-gradient-to-r from-blue-400 to-green-400 relative">
                <div className="absolute right-0 top-1/2 transform -translate-y-1/2 w-0 h-0 border-l-2 border-l-green-400 border-t border-t-transparent border-b border-b-transparent"></div>
              </div>
            </div>
            
            <div className="flex-1">
              <Input
                value={item.value || ''}
                onChange={(e) => updateValue(index, e.target.value)}
                placeholder={valuePlaceholder}
                className={`${
                  error && required && !item.value 
                    ? 'border-red-300 focus:border-red-400' 
                    : 'border-green-200 focus:border-green-400'
                } bg-green-50/30`}
              />
            </div>

            {/* 删除按钮 */}
            {values.length > 1 && (
              <Button
                type="button"
                size="sm"
                variant="outline"
                onClick={() => removeItem(index)}
                className="w-8 h-8 p-0 text-red-500 hover:text-red-700 hover:bg-red-50 border-red-200"
              >
                <Minus className="w-4 h-4" />
              </Button>
            )}
            
            {/* 占位符，保持对齐 */}
            {values.length === 1 && (
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
          onClick={addItem}
          className="text-blue-600 hover:text-blue-700 hover:bg-blue-50 border-blue-200"
        >
          <Plus className="w-4 h-4 mr-1" />
          {addButtonText}
        </Button>
      </div>

      {/* 错误信息 */}
      {error && (
        <div className="text-xs text-red-500 mt-1">{error}</div>
      )}
    </div>
  )
}
