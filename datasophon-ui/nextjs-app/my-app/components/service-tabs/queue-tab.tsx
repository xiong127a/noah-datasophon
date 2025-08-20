"use client"

import { Monitor, Plus, Edit, Trash2, BarChart } from 'lucide-react'
import { Button } from '@/components/ui/button'

interface QueueTabProps {
  serviceId: string
  serviceName: string
}

export default function QueueTab({ serviceId, serviceName }: QueueTabProps) {
  return (
    <div className="p-6">
      <div className="mb-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-2">YARN 资源配置</h2>
        <p className="text-sm text-gray-600">管理 YARN 队列和资源分配策略</p>
      </div>

      {/* 工具栏 */}
      <div className="bg-white rounded-lg shadow-sm border p-4 mb-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <Button size="sm">
              <Plus className="w-4 h-4 mr-2" />
              创建队列
            </Button>
            <Button size="sm" variant="outline">
              <Edit className="w-4 h-4 mr-2" />
              编辑调度器
            </Button>
            <Button size="sm" variant="outline">
              <BarChart className="w-4 h-4 mr-2" />
              资源使用情况
            </Button>
          </div>
          <Button size="sm" variant="outline">
            刷新配置
          </Button>
        </div>
      </div>

      {/* 队列统计卡片 */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
        <div className="bg-white rounded-lg shadow-sm border p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">总队列数</p>
              <p className="text-2xl font-bold text-gray-900">--</p>
            </div>
            <Monitor className="w-8 h-8 text-blue-500" />
          </div>
        </div>
        
        <div className="bg-white rounded-lg shadow-sm border p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">活跃队列</p>
              <p className="text-2xl font-bold text-green-600">--</p>
            </div>
            <BarChart className="w-8 h-8 text-green-500" />
          </div>
        </div>
        
        <div className="bg-white rounded-lg shadow-sm border p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">总内存</p>
              <p className="text-2xl font-bold text-purple-600">--</p>
            </div>
            <Monitor className="w-8 h-8 text-purple-500" />
          </div>
        </div>
        
        <div className="bg-white rounded-lg shadow-sm border p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">总CPU核数</p>
              <p className="text-2xl font-bold text-orange-600">--</p>
            </div>
            <Monitor className="w-8 h-8 text-orange-500" />
          </div>
        </div>
      </div>

      {/* 队列管理 */}
      <div className="bg-white rounded-lg shadow-sm border">
        <div className="p-4 border-b">
          <h3 className="font-medium text-gray-900">队列管理</h3>
        </div>
        <div className="p-8 text-center">
          <Monitor className="w-12 h-12 text-gray-400 mx-auto mb-4" />
          <h4 className="text-sm font-medium text-gray-900 mb-2">YARN 资源配置页面开发中</h4>
          <p className="text-xs text-gray-500">
            此页面将提供完整的 YARN 资源管理功能，包括：
            <br />• 队列创建和管理
            <br />• 资源分配策略配置
            <br />• 调度器参数设置
            <br />• 资源使用情况监控
          </p>
        </div>
      </div>
    </div>
  )
}
