"use client"

/**
 * 文档全屏显示页面
 * 用于独立显示组件介绍和用户指南
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-20
 */

import { useSearchParams } from 'next/navigation'
import { Suspense } from 'react'
import MarkdownViewer from '@/components/markdown-viewer/markdown-viewer'
import ServiceIcon from '@/components/ui/service-icon'

function DocsContent() {
  const searchParams = useSearchParams()
  
  // 从URL参数获取必要信息
  const serviceId = searchParams.get('serviceId') || ''
  const serviceName = searchParams.get('serviceName') || ''
  const docType = searchParams.get('docType') as 'component' | 'guide' || 'component'
  
  return (
    <div className="h-screen flex flex-col bg-gradient-to-br from-slate-50 via-blue-50/30 to-indigo-50/20">
      {/* 现代化的顶部标题栏 */}
      <div className="h-14 bg-white/95 backdrop-blur-lg border-b border-blue-100/60 flex items-center justify-center px-6 shadow-lg shadow-blue-500/10">
        <div className="flex items-center space-x-4">
          {/* 服务Logo图标 */}
          <div className="relative">
            <div className="p-2 rounded-xl bg-gradient-to-br from-white via-gray-50 to-gray-100 shadow-lg border border-gray-200/50 transform hover:scale-105 transition-transform duration-200">
              <ServiceIcon 
                serviceName={serviceName}
                size={20}
                className="drop-shadow-sm"
              />
            </div>
            {/* 文档类型装饰点 */}
            <div className={`absolute -top-1 -right-1 w-3 h-3 rounded-full shadow-md animate-pulse ${
              docType === 'component' 
                ? 'bg-gradient-to-br from-blue-400 to-indigo-600' 
                : 'bg-gradient-to-br from-green-400 to-emerald-600'
            }`}></div>
          </div>
          
          {/* 优化的页面标题 */}
          <div className="text-center">
            <h1 className="text-lg font-bold bg-gradient-to-r from-gray-900 via-blue-800 to-indigo-700 bg-clip-text text-transparent">
              {serviceName}
            </h1>
            <div className="flex items-center space-x-2">
              <span className="text-xs text-gray-500">
                {docType === 'component' ? '组件介绍' : '用户指南'}
              </span>
              <div className="w-1 h-1 bg-blue-400 rounded-full"></div>
              <span className="text-xs text-blue-600 font-medium">文档窗口</span>
            </div>
          </div>
        </div>
      </div>
      
      {/* 文档内容区域 */}
      <div className="flex-1 overflow-hidden">
        <MarkdownViewer
          serviceId={serviceId}
          serviceName={serviceName}
          docType={docType}
          isFullScreen={true}
        />
      </div>
    </div>
  )
}

export default function DocsPage() {
  return (
    <Suspense fallback={
      <div className="h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="w-16 h-16 border-4 border-blue-200 border-t-blue-600 rounded-full animate-spin mx-auto mb-4"></div>
          <p className="text-gray-600">正在加载文档...</p>
        </div>
      </div>
    }>
      <DocsContent />
    </Suspense>
  )
}
