"use client"

import { Server, Play, Pause, RotateCcw, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'

interface InstancesTabProps {
  serviceId: string
  serviceName: string
}

export default function InstancesTab({ serviceId, serviceName }: InstancesTabProps) {
  return (
    <div className="p-6">
      <div className="mb-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-2">服务实例</h2>
        <p className="text-sm text-gray-600">管理 {serviceName} 服务的所有实例</p>
      </div>

      {/* 工具栏 */}
      <div className="bg-white rounded-lg shadow-sm border p-4 mb-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <Button size="sm" variant="outline">
              <Play className="w-4 h-4 mr-2" />
              启动所有
            </Button>
            <Button size="sm" variant="outline">
              <Pause className="w-4 h-4 mr-2" />
              停止所有
            </Button>
            <Button size="sm" variant="outline">
              <RotateCcw className="w-4 h-4 mr-2" />
              重启所有
            </Button>
          </div>
          <Button size="sm">
            刷新
          </Button>
        </div>
      </div>

      {/* 实例列表 */}
      <div className="bg-white rounded-lg shadow-sm border">
        <div className="p-4 border-b">
          <h3 className="font-medium text-gray-900">实例列表</h3>
        </div>
        <div className="p-8 text-center">
          <Server className="w-12 h-12 text-gray-400 mx-auto mb-4" />
          <h4 className="text-sm font-medium text-gray-900 mb-2">实例列表开发中</h4>
          <p className="text-xs text-gray-500">
            此页面将显示 {serviceName} 服务的所有实例信息，包括实例状态、主机信息等
          </p>
        </div>
      </div>
    </div>
  )
}
