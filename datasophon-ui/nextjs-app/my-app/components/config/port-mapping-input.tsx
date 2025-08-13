"use client"

import React from 'react'
import { Network, Server, Scale } from 'lucide-react'
import VisualPortMappingInput, { type VisualPortMappingInputProps } from './visual-port-mapping-input'

/**
 * Kubernetes端口映射输入组件
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 * 日期：2024-01-31
 * 
 * 特点：
 * - 支持三种Kubernetes端口映射类型
 * - 专业的视觉设计和类型识别
 * - 基于现有键值对组件的增强版本
 */

export type PortMappingType = 'node_port' | 'cluster_port' | 'load_balancer'

interface PortMappingInputProps extends Omit<VisualPortMappingInputProps, 'mappingType'> {
  mappingType: PortMappingType
}

const PortMappingInput: React.FC<PortMappingInputProps> = ({
  mappingType,
  ...props
}) => {
  // 获取端口映射类型的配置信息
  const getMappingTypeConfig = (type: PortMappingType) => {
    switch (type) {
      case 'node_port':
        return {
          gradientFrom: 'from-orange-500/10',
          gradientTo: 'to-amber-500/10',
          borderColor: 'border-orange-200/60',
        }
      case 'cluster_port':
        return {
          gradientFrom: 'from-blue-500/10',
          gradientTo: 'to-indigo-500/10',
          borderColor: 'border-blue-200/60',
        }
      case 'load_balancer':
        return {
          gradientFrom: 'from-green-500/10',
          gradientTo: 'to-emerald-500/10',
          borderColor: 'border-green-200/60',
        }
      default:
        return {
          gradientFrom: 'from-gray-500/10',
          gradientTo: 'to-slate-500/10',
          borderColor: 'border-gray-200/60',
        }
    }
  }

  const config = getMappingTypeConfig(mappingType)

  return (
    <div className={`
      relative overflow-hidden rounded-2xl border ${config.borderColor}
      bg-gradient-to-br ${config.gradientFrom} ${config.gradientTo}
      backdrop-blur-sm shadow-lg hover:shadow-xl
      transition-all duration-300
    `}>
      {/* 端口映射输入区域 */}
      <div className="p-4">
        <VisualPortMappingInput
          {...props}
          mappingType={mappingType}
          className="space-y-3"
        />
      </div>
      
      {/* 背景装饰 */}
      <div className="absolute inset-0 bg-gradient-to-br from-white/10 via-transparent to-white/5 pointer-events-none" />
    </div>
  )
}

export default PortMappingInput
