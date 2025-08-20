"use client"

import { BookOpen, Search, Download } from 'lucide-react'
import { Button } from '@/components/ui/button'

interface GuideTabProps {
  serviceId: string
  serviceName: string
}

export default function GuideTab({ serviceId, serviceName }: GuideTabProps) {
  return (
    <div className="p-6">
      <div className="mb-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-2">用户指南</h2>
        <p className="text-sm text-gray-600">{serviceName} 服务的使用指南和最佳实践</p>
      </div>

      {/* 搜索和工具栏 */}
      <div className="bg-white rounded-lg shadow-sm border p-4 mb-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                type="text"
                placeholder="搜索指南内容..."
                className="pl-10 pr-4 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>
          <Button size="sm" variant="outline">
            <Download className="w-4 h-4 mr-2" />
            下载PDF
          </Button>
        </div>
      </div>

      {/* 指南内容 */}
      <div className="bg-white rounded-lg shadow-sm border">
        <div className="p-4 border-b">
          <h3 className="font-medium text-gray-900">使用指南目录</h3>
        </div>
        
        {/* 目录导航 */}
        <div className="flex">
          <div className="w-64 border-r p-4">
            <nav className="space-y-2">
              <a href="#" className="block px-3 py-2 text-sm text-blue-600 bg-blue-50 rounded-md">
                快速开始
              </a>
              <a href="#" className="block px-3 py-2 text-sm text-gray-700 hover:bg-gray-50 rounded-md">
                安装配置
              </a>
              <a href="#" className="block px-3 py-2 text-sm text-gray-700 hover:bg-gray-50 rounded-md">
                基础操作
              </a>
              <a href="#" className="block px-3 py-2 text-sm text-gray-700 hover:bg-gray-50 rounded-md">
                高级功能
              </a>
              <a href="#" className="block px-3 py-2 text-sm text-gray-700 hover:bg-gray-50 rounded-md">
                故障排查
              </a>
            </nav>
          </div>
          
          {/* 内容区域 */}
          <div className="flex-1 p-8 text-center">
            <BookOpen className="w-12 h-12 text-gray-400 mx-auto mb-4" />
            <h4 className="text-sm font-medium text-gray-900 mb-2">用户指南页面开发中</h4>
            <p className="text-xs text-gray-500">
              此页面将提供 {serviceName} 的完整使用指南，包括：
              <br />• 快速入门教程
              <br />• 详细操作步骤
              <br />• 常见问题解答
              <br />• 最佳实践建议
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
