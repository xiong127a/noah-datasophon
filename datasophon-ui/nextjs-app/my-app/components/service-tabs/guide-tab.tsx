"use client"

/**
 * 用户指南页面 - 导航到全屏文档页面
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-20
 */

import { useSearchParams } from 'next/navigation'
import { FileText, ArrowRight, Book } from 'lucide-react'

interface GuideTabProps {
  serviceId: string
  serviceName: string
}

export default function GuideTab({ serviceId, serviceName }: GuideTabProps) {
  const searchParams = useSearchParams()
  const clusterId = searchParams.get('clusterId') || ''
  
  // 在小窗中打开文档
  const handleOpenDocs = () => {
    const url = `/docs?serviceId=${serviceId}&serviceName=${encodeURIComponent(serviceName)}&docType=guide&clusterId=${clusterId}`
    
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
    <div className="h-full bg-gradient-to-br from-green-50 via-white to-emerald-50 flex items-center justify-center p-8">
      <div className="max-w-2xl w-full">
        <div className="bg-white rounded-2xl shadow-xl p-8 border border-gray-100">
          {/* 图标和标题 */}
          <div className="flex flex-col items-center text-center mb-8">
            <div className="w-20 h-20 bg-gradient-to-br from-green-500 to-emerald-600 rounded-2xl flex items-center justify-center mb-4 shadow-lg">
              <Book className="w-10 h-10 text-white" />
            </div>
            <h2 className="text-3xl font-bold text-gray-900 mb-2">
              {serviceName} 用户指南
            </h2>
            <p className="text-gray-600">
              查看详细的使用说明、操作指南和最佳实践
            </p>
          </div>
          
          {/* 功能说明 */}
          <div className="space-y-4 mb-8">
            <div className="flex items-start space-x-3">
              <div className="w-8 h-8 bg-green-100 rounded-lg flex items-center justify-center flex-shrink-0 mt-0.5">
                <FileText className="w-4 h-4 text-green-600" />
              </div>
              <div>
                <h3 className="font-semibold text-gray-900 mb-1">使用教程</h3>
                <p className="text-sm text-gray-600">提供从入门到精通的完整使用教程</p>
              </div>
            </div>
            
            <div className="flex items-start space-x-3">
              <div className="w-8 h-8 bg-blue-100 rounded-lg flex items-center justify-center flex-shrink-0 mt-0.5">
                <svg className="w-4 h-4 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                </svg>
              </div>
              <div>
                <h3 className="font-semibold text-gray-900 mb-1">操作手册</h3>
                <p className="text-sm text-gray-600">包含所有功能的详细操作步骤说明</p>
              </div>
            </div>
            
            <div className="flex items-start space-x-3">
              <div className="w-8 h-8 bg-yellow-100 rounded-lg flex items-center justify-center flex-shrink-0 mt-0.5">
                <svg className="w-4 h-4 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                </svg>
              </div>
              <div>
                <h3 className="font-semibold text-gray-900 mb-1">最佳实践</h3>
                <p className="text-sm text-gray-600">分享行业最佳实践和优化建议</p>
              </div>
            </div>
          </div>
          
          {/* 操作按钮 */}
          <div className="flex justify-center">
            <button
              onClick={handleOpenDocs}
              className="px-8 py-3 bg-gradient-to-r from-green-600 to-emerald-600 text-white rounded-xl hover:from-green-700 hover:to-emerald-700 transition-all duration-200 shadow-lg hover:shadow-xl transform hover:scale-105 flex items-center space-x-2"
            >
              <Book className="w-5 h-5" />
              <span className="font-semibold">查看指南</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>
          
          {/* 提示信息 */}
          <div className="mt-6 p-3 bg-green-50 border border-green-200 rounded-lg">
            <p className="text-sm text-green-700 flex items-center">
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
