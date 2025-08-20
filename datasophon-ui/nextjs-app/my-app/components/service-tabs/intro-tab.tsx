"use client"

import { Info, ExternalLink } from 'lucide-react'
import { Button } from '@/components/ui/button'

interface IntroTabProps {
  serviceId: string
  serviceName: string
}

export default function IntroTab({ serviceId, serviceName }: IntroTabProps) {
  return (
    <div className="p-6">
      <div className="mb-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-2">组件介绍</h2>
        <p className="text-sm text-gray-600">了解 {serviceName} 组件的功能特性和架构设计</p>
      </div>

      {/* 快速链接 */}
      <div className="bg-white rounded-lg shadow-sm border p-4 mb-6">
        <div className="flex items-center justify-between">
          <h3 className="font-medium text-gray-900">快速链接</h3>
          <div className="flex items-center space-x-3">
            <Button size="sm" variant="outline">
              <ExternalLink className="w-4 h-4 mr-2" />
              官方文档
            </Button>
            <Button size="sm" variant="outline">
              <ExternalLink className="w-4 h-4 mr-2" />
              GitHub
            </Button>
          </div>
        </div>
      </div>

      {/* 介绍内容 */}
      <div className="bg-white rounded-lg shadow-sm border">
        <div className="p-4 border-b">
          <h3 className="font-medium text-gray-900">{serviceName} 组件详情</h3>
        </div>
        <div className="p-8 text-center">
          <Info className="w-12 h-12 text-gray-400 mx-auto mb-4" />
          <h4 className="text-sm font-medium text-gray-900 mb-2">组件介绍页面开发中</h4>
          <p className="text-xs text-gray-500">
            此页面将详细介绍 {serviceName} 组件，包括：
            <br />• 组件架构和设计原理
            <br />• 主要功能特性
            <br />• 使用场景和最佳实践
            <br />• 版本信息和更新日志
          </p>
        </div>
      </div>
    </div>
  )
}
