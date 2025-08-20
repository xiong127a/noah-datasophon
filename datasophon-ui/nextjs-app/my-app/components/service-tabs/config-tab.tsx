"use client"

import { Settings, Save, RotateCcw } from 'lucide-react'
import { Button } from '@/components/ui/button'

interface ConfigTabProps {
  serviceId: string
  serviceName: string
}

export default function ConfigTab({ serviceId, serviceName }: ConfigTabProps) {
  return (
    <div className="p-6">
      <div className="mb-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-2">服务配置</h2>
        <p className="text-sm text-gray-600">配置 {serviceName} 服务的参数</p>
      </div>

      {/* 工具栏 */}
      <div className="bg-white rounded-lg shadow-sm border p-4 mb-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <Button size="sm">
              <Save className="w-4 h-4 mr-2" />
              保存配置
            </Button>
            <Button size="sm" variant="outline">
              <RotateCcw className="w-4 h-4 mr-2" />
              重置
            </Button>
          </div>
          <Button size="sm" variant="outline">
            导入/导出
          </Button>
        </div>
      </div>

      {/* 配置内容 */}
      <div className="bg-white rounded-lg shadow-sm border">
        <div className="p-4 border-b">
          <h3 className="font-medium text-gray-900">配置参数</h3>
        </div>
        <div className="p-8 text-center">
          <Settings className="w-12 h-12 text-gray-400 mx-auto mb-4" />
          <h4 className="text-sm font-medium text-gray-900 mb-2">配置页面开发中</h4>
          <p className="text-xs text-gray-500">
            此页面将显示 {serviceName} 服务的所有配置参数，支持在线编辑和保存
          </p>
        </div>
      </div>
    </div>
  )
}
