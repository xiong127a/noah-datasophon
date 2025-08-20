"use client"

import { Link, Copy, Code, Terminal } from 'lucide-react'
import { Button } from '@/components/ui/button'

interface ConnectionTabProps {
  serviceId: string
  serviceName: string
}

export default function ConnectionTab({ serviceId, serviceName }: ConnectionTabProps) {
  return (
    <div className="p-6">
      <div className="mb-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-2">连接信息</h2>
        <p className="text-sm text-gray-600">获取连接 {serviceName} 服务的方式和代码示例</p>
      </div>

      {/* 连接信息卡片 */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
        <div className="bg-white rounded-lg shadow-sm border p-4">
          <div className="flex items-center space-x-2 mb-3">
            <Code className="w-5 h-5 text-blue-600" />
            <h3 className="font-medium text-gray-900">Java 连接</h3>
          </div>
          <p className="text-sm text-gray-600">Java SDK 连接示例和依赖配置</p>
        </div>

        <div className="bg-white rounded-lg shadow-sm border p-4">
          <div className="flex items-center space-x-2 mb-3">
            <Terminal className="w-5 h-5 text-green-600" />
            <h3 className="font-medium text-gray-900">命令行</h3>
          </div>
          <p className="text-sm text-gray-600">命令行工具使用方法</p>
        </div>
      </div>

      {/* 主要内容区域 */}
      <div className="bg-white rounded-lg shadow-sm border">
        <div className="p-4 border-b">
          <div className="flex items-center justify-between">
            <h3 className="font-medium text-gray-900">连接详情</h3>
            <Button size="sm" variant="outline">
              <Copy className="w-4 h-4 mr-2" />
              复制连接信息
            </Button>
          </div>
        </div>
        <div className="p-8 text-center">
          <Link className="w-12 h-12 text-gray-400 mx-auto mb-4" />
          <h4 className="text-sm font-medium text-gray-900 mb-2">连接信息页面开发中</h4>
          <p className="text-xs text-gray-500">
            此页面将提供 {serviceName} 服务的详细连接信息，包括：
            <br />• 连接地址和端口
            <br />• 各种语言的SDK示例
            <br />• 命令行工具使用方法
          </p>
        </div>
      </div>
    </div>
  )
}
