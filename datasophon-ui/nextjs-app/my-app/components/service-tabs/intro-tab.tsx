"use client"

/**
 * 组件介绍页面 - 导航到全屏文档页面
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-20
 */

import { useSearchParams } from 'next/navigation'
import { BookOpen, FileText, ArrowRight } from 'lucide-react'

interface IntroTabProps {
  serviceId: string
  serviceName: string
}

export default function IntroTab({ serviceId, serviceName }: IntroTabProps) {
  const searchParams = useSearchParams()
  const clusterId = searchParams.get('clusterId') || ''
  
  // 在小窗中打开文档
  const handleOpenDocs = () => {
    const url = `/docs?serviceId=${serviceId}&serviceName=${encodeURIComponent(serviceName)}&docType=component&clusterId=${clusterId}`
    
    // 计算窗口尺寸和位置
    const windowWidth = 1200
    const windowHeight = 800
    const screenWidth = window.screen.width
    const screenHeight = window.screen.height
    const left = Math.round((screenWidth - windowWidth) / 2)
    const top = Math.round((screenHeight - windowHeight) / 2)
    
    // 设置窗口参数
    const windowFeatures = [
      `width=${windowWidth}`,
      `height=${windowHeight}`,
      `left=${left}`,
      `top=${top}`,
      'resizable=yes',
      'scrollbars=yes',
      'status=yes',
      'menubar=no',
      'toolbar=no',
      'location=no'
    ].join(',')
    
    window.open(url, 'docsWindow', windowFeatures)
  }
  
  return (
    <div className="h-full bg-gradient-to-br from-blue-50 via-white to-indigo-50 flex items-center justify-center p-8">
      <div className="max-w-2xl w-full">
        <div className="bg-white rounded-2xl shadow-xl p-8 border border-gray-100">
          {/* 图标和标题 */}
          <div className="flex flex-col items-center text-center mb-8">
            <div className="w-20 h-20 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-2xl flex items-center justify-center mb-4 shadow-lg">
              <BookOpen className="w-10 h-10 text-white" />
            </div>
            <h2 className="text-3xl font-bold text-gray-900 mb-2">
              {serviceName} 组件介绍
            </h2>
            <p className="text-gray-600">
              查看详细的组件架构、配置说明和使用方法
            </p>
          </div>
          
          {/* 功能说明 */}
          <div className="space-y-4 mb-8">
            <div className="flex items-start space-x-3">
              <div className="w-8 h-8 bg-blue-100 rounded-lg flex items-center justify-center flex-shrink-0 mt-0.5">
                <FileText className="w-4 h-4 text-blue-600" />
              </div>
              <div>
                <h3 className="font-semibold text-gray-900 mb-1">完整文档</h3>
                <p className="text-sm text-gray-600">包含组件的详细介绍、架构设计和核心特性说明</p>
              </div>
            </div>
            
            <div className="flex items-start space-x-3">
              <div className="w-8 h-8 bg-green-100 rounded-lg flex items-center justify-center flex-shrink-0 mt-0.5">
                <svg className="w-4 h-4 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <div>
                <h3 className="font-semibold text-gray-900 mb-1">配置指南</h3>
                <p className="text-sm text-gray-600">详细的配置参数说明和最佳实践建议</p>
              </div>
            </div>
            
            <div className="flex items-start space-x-3">
              <div className="w-8 h-8 bg-purple-100 rounded-lg flex items-center justify-center flex-shrink-0 mt-0.5">
                <svg className="w-4 h-4 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                </svg>
              </div>
              <div>
                <h3 className="font-semibold text-gray-900 mb-1">快速上手</h3>
                <p className="text-sm text-gray-600">提供示例代码和常见使用场景的解决方案</p>
              </div>
            </div>
          </div>
          
          {/* 操作按钮 */}
          <div className="flex justify-center">
            <button
              onClick={handleOpenDocs}
              className="px-8 py-3 bg-gradient-to-r from-blue-600 to-indigo-600 text-white rounded-xl hover:from-blue-700 hover:to-indigo-700 transition-all duration-200 shadow-lg hover:shadow-xl transform hover:scale-105 flex items-center space-x-2"
            >
              <BookOpen className="w-5 h-5" />
              <span className="font-semibold">查看文档</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>
          
          {/* 提示信息 */}
          <div className="mt-6 p-3 bg-blue-50 border border-blue-200 rounded-lg">
            <p className="text-sm text-blue-700 flex items-center">
              <svg className="w-4 h-4 mr-2 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
              </svg>
              文档将在独立窗口中打开，您可以调整窗口大小，方便阅读
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
