"use client"

/**
 * 组件介绍页面 - 导航到全屏文档页面 (超级美化版)
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-20
 */

import { useSearchParams } from 'next/navigation'
import { useState, useEffect } from 'react'
import { BookOpen, FileText, ArrowRight, Sparkles, Zap, Settings } from 'lucide-react'

interface IntroTabProps {
  serviceId: string
  serviceName: string
}

export default function IntroTab({ serviceId, serviceName }: IntroTabProps) {
  const searchParams = useSearchParams()
  const clusterId = searchParams.get('clusterId') || ''
  const [mounted, setMounted] = useState(false)
  const [isHovered, setIsHovered] = useState(false)
  
  // 页面加载动画控制
  useEffect(() => {
    setMounted(true)
  }, [])
  
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
    <div className="h-full relative overflow-hidden">
      {/* 超级动态背景系统 */}
      <div className="absolute inset-0 bg-gradient-to-br from-slate-50 via-blue-50/40 to-indigo-100/80">
        {/* 主背景渐变层 */}
        <div className="absolute inset-0 bg-gradient-to-r from-blue-50/30 via-transparent to-purple-50/20"></div>
        <div className="absolute inset-0 bg-gradient-to-b from-transparent via-white/10 to-blue-100/40"></div>
      </div>
      
      {/* 动态装饰粒子系统 */}
      <div className="absolute inset-0 pointer-events-none">
        {/* 大型光晕背景 */}
        <div className="absolute top-1/4 right-1/4 w-96 h-96 bg-gradient-to-r from-blue-200/15 via-indigo-200/10 to-purple-200/15 rounded-full blur-3xl animate-pulse"></div>
        <div className="absolute bottom-1/4 left-1/4 w-80 h-80 bg-gradient-to-r from-indigo-200/10 via-blue-200/15 to-cyan-200/10 rounded-full blur-3xl animate-pulse" style={{ animationDelay: '1s' }}></div>
        
        {/* 中等光晕 */}
        <div className="absolute top-1/3 left-1/3 w-48 h-48 bg-blue-300/20 rounded-full blur-2xl animate-pulse" style={{ animationDelay: '2s' }}></div>
        <div className="absolute bottom-1/3 right-1/3 w-32 h-32 bg-indigo-300/15 rounded-full blur-xl animate-pulse" style={{ animationDelay: '0.5s' }}></div>
        
        {/* 精致几何装饰 */}
        <div className="absolute top-8 left-8 w-2 h-2 bg-gradient-to-r from-blue-400 to-indigo-400 rounded-full opacity-60 animate-pulse"></div>
        <div className="absolute top-16 right-12 w-1.5 h-1.5 bg-gradient-to-r from-indigo-400 to-purple-400 rounded-full opacity-50 animate-pulse" style={{ animationDelay: '1.5s' }}></div>
        <div className="absolute bottom-20 right-16 w-3 h-3 bg-gradient-to-r from-purple-400 to-pink-400 rounded-full opacity-40 animate-pulse" style={{ animationDelay: '2.5s' }}></div>
        <div className="absolute bottom-32 left-20 w-1 h-1 bg-gradient-to-r from-blue-500 to-cyan-500 rounded-full opacity-70 animate-pulse" style={{ animationDelay: '3s' }}></div>
        
        {/* 装饰性几何线条 */}
        <div className="absolute top-24 left-0 w-16 h-px bg-gradient-to-r from-transparent via-blue-300/40 to-transparent animate-pulse"></div>
        <div className="absolute top-32 right-0 w-20 h-px bg-gradient-to-l from-transparent via-indigo-300/50 to-transparent animate-pulse" style={{ animationDelay: '1s' }}></div>
        <div className="absolute bottom-40 left-0 w-12 h-px bg-gradient-to-r from-transparent via-purple-300/30 to-transparent animate-pulse" style={{ animationDelay: '2s' }}></div>
        
        {/* 动态浮动元素 */}
        <div className="absolute top-1/4 right-8 w-6 h-6 bg-gradient-to-br from-blue-400/20 to-indigo-400/20 rounded-full animate-bounce" style={{ animationDelay: '0.5s', animationDuration: '3s' }}></div>
        <div className="absolute bottom-1/4 left-8 w-4 h-4 bg-gradient-to-br from-indigo-400/30 to-purple-400/30 rounded-full animate-bounce" style={{ animationDelay: '1.5s', animationDuration: '2.5s' }}></div>
      </div>
      
      {/* 主内容区域 */}
      <div className="relative z-10 h-full flex items-center justify-center p-4">
        <div className={`max-w-4xl w-full transform transition-all duration-1000 ${mounted ? 'translate-y-0 opacity-100' : 'translate-y-8 opacity-0'}`}>
          {/* 主卡片 - 超级毛玻璃效果 - 宽屏布局 */}
          <div 
            className="relative bg-white/95 backdrop-blur-2xl rounded-3xl p-6 shadow-2xl border border-white/40 overflow-hidden transition-all duration-500 hover:shadow-3xl hover:scale-[1.01] group"
            onMouseEnter={() => setIsHovered(true)}
            onMouseLeave={() => setIsHovered(false)}
          >
            {/* 卡片内部光效 */}
            <div className="absolute inset-0 bg-gradient-to-r from-blue-500/5 via-transparent to-indigo-500/5 opacity-0 group-hover:opacity-100 transition-opacity duration-500"></div>
            <div className={`absolute inset-0 bg-gradient-to-br from-white/20 via-transparent to-blue-100/10 transition-opacity duration-300 ${isHovered ? 'opacity-100' : 'opacity-0'}`}></div>
            
            {/* 顶部彩色装饰条 - 增强版 */}
            <div className="absolute top-0 left-0 right-0 h-1.5 bg-gradient-to-r from-blue-500 via-indigo-500 via-purple-500 to-pink-500 rounded-t-3xl opacity-90 group-hover:opacity-100 transition-opacity duration-300"></div>
            <div className="absolute top-0 left-0 right-0 h-1.5 bg-gradient-to-r from-blue-400 via-indigo-400 via-purple-400 to-pink-400 rounded-t-3xl opacity-50 animate-pulse"></div>
            
            {/* 主要内容区 - 左右分栏布局 */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-center">
              {/* 左侧：标题和按钮区域 */}
              <div className={`transition-all duration-700 ${mounted ? 'translate-x-0 opacity-100' : 'translate-x-4 opacity-0'}`} style={{ transitionDelay: '0.2s' }}>
                {/* 主图标 - 3D效果 */}
                <div className="relative inline-flex items-center justify-center w-16 h-16 mb-6 group-hover:scale-110 transition-transform duration-500">
                  <div className="absolute inset-0 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-2xl shadow-lg group-hover:shadow-xl transition-shadow duration-300"></div>
                  <div className="absolute inset-0 bg-gradient-to-t from-transparent to-white/20 rounded-2xl"></div>
                  <div className="absolute -inset-1 bg-gradient-to-r from-blue-400 via-indigo-500 to-purple-500 rounded-2xl blur-md opacity-0 group-hover:opacity-50 transition-opacity duration-500"></div>
                  <BookOpen className="relative w-8 h-8 text-white transform group-hover:rotate-12 transition-transform duration-500" />
                  {/* 图标光效 */}
                  <div className="absolute inset-0 bg-gradient-to-r from-white/30 to-transparent rounded-2xl opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
                </div>
                
                {/* 主标题 - 渐变文字效果 */}
                <h2 className="text-2xl font-bold bg-gradient-to-r from-gray-900 via-blue-800 to-indigo-900 bg-clip-text text-transparent mb-4 group-hover:from-blue-900 group-hover:to-purple-900 transition-all duration-500">
                  {serviceName} 组件介绍
                </h2>
                
                {/* 副标题 - 淡入效果 */}
                <p className="text-sm text-gray-600 leading-relaxed mb-6 group-hover:text-gray-700 transition-colors duration-300">
                  深入了解组件架构设计、核心功能特性，掌握最佳配置实践
                </p>
                
                {/* 操作按钮区域 - 超级按钮 */}
                <div className={`transition-all duration-700 ${mounted ? 'translate-y-0 opacity-100' : 'translate-y-4 opacity-0'}`} style={{ transitionDelay: '1s' }}>
                  <button
                    onClick={handleOpenDocs}
                    className="relative group/btn bg-gradient-to-r from-blue-600 via-indigo-600 to-purple-600 hover:from-blue-700 hover:via-indigo-700 hover:to-purple-700 text-white py-3 px-6 rounded-xl font-semibold shadow-xl hover:shadow-2xl transition-all duration-500 transform hover:scale-[1.03] hover:-translate-y-1 overflow-hidden"
                  >
                    {/* 按钮内部光效 */}
                    <div className="absolute inset-0 bg-gradient-to-r from-white/20 via-transparent to-white/20 translate-x-[-100%] group-hover/btn:translate-x-[100%] transition-transform duration-700"></div>
                    <div className="absolute inset-0 bg-gradient-to-t from-black/10 to-transparent opacity-0 group-hover/btn:opacity-100 transition-opacity duration-300"></div>
                    
                    {/* 按钮内容 */}
                    <div className="relative flex items-center justify-center space-x-3">
                      <BookOpen className="w-5 h-5 group-hover/btn:rotate-12 group-hover/btn:scale-110 transition-transform duration-300" />
                      <span className="text-sm">查看完整文档</span>
                      <ArrowRight className="w-4 h-4 group-hover/btn:translate-x-1 group-hover/btn:scale-110 transition-transform duration-300" />
                    </div>
                    
                    {/* 按钮边框光晕 */}
                    <div className="absolute -inset-1 bg-gradient-to-r from-blue-500 via-indigo-500 to-purple-500 rounded-xl blur-sm opacity-0 group-hover/btn:opacity-50 transition-opacity duration-300"></div>
                  </button>
                  
                  {/* 精致提示信息 - 动态效果 */}
                  <div className={`mt-4 text-xs text-gray-500 flex items-center space-x-2 transition-all duration-500 ${mounted ? 'opacity-100' : 'opacity-0'}`} style={{ transitionDelay: '1.2s' }}>
                    <div className="w-1.5 h-1.5 bg-gradient-to-r from-blue-400 to-indigo-400 rounded-full animate-pulse"></div>
                    <Sparkles className="w-3 h-3 text-indigo-400 animate-pulse" />
                    <span className="group-hover:text-gray-600 transition-colors duration-300">文档将在新窗口中打开</span>
                  </div>
                </div>
              </div>

              {/* 右侧：功能特性列表 - 水平网格布局 */}
              <div className={`transition-all duration-700 ${mounted ? 'translate-x-0 opacity-100' : 'translate-x-4 opacity-0'}`} style={{ transitionDelay: '0.4s' }}>
                <div className="grid grid-cols-1 gap-4">
                  {/* 功能卡片1 - 完整文档 */}
                  <div className="group/card flex items-center space-x-4 p-4 bg-gradient-to-r from-blue-50/80 to-indigo-50/60 rounded-xl border border-blue-100/60 hover:border-blue-200/80 hover:shadow-lg hover:scale-[1.02] transition-all duration-300 cursor-pointer">
                    <div className="relative flex-shrink-0">
                      <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-xl flex items-center justify-center shadow-md group-hover/card:shadow-lg group-hover/card:scale-110 transition-all duration-300">
                        <FileText className="w-5 h-5 text-white group-hover/card:rotate-12 transition-transform duration-300" />
                      </div>
                      <div className="absolute -inset-1 bg-gradient-to-r from-blue-400 to-indigo-500 rounded-xl blur-md opacity-0 group-hover/card:opacity-30 transition-opacity duration-300"></div>
                    </div>
                    <div className="min-w-0 flex-1">
                      <h4 className="font-semibold text-gray-900 mb-1 group-hover/card:text-blue-900 transition-colors duration-300">完整文档</h4>
                      <p className="text-xs text-gray-600 leading-relaxed group-hover/card:text-gray-700 transition-colors duration-300">包含详细介绍、架构设计图、核心特性和技术规格</p>
                    </div>
                  </div>
                  
                  {/* 功能卡片2 - 配置指南 */}
                  <div className="group/card flex items-center space-x-4 p-4 bg-gradient-to-r from-emerald-50/80 to-green-50/60 rounded-xl border border-emerald-100/60 hover:border-emerald-200/80 hover:shadow-lg hover:scale-[1.02] transition-all duration-300 cursor-pointer">
                    <div className="relative flex-shrink-0">
                      <div className="w-10 h-10 bg-gradient-to-br from-emerald-500 to-green-600 rounded-xl flex items-center justify-center shadow-md group-hover/card:shadow-lg group-hover/card:scale-110 transition-all duration-300">
                        <Settings className="w-5 h-5 text-white group-hover/card:rotate-180 transition-transform duration-500" />
                      </div>
                      <div className="absolute -inset-1 bg-gradient-to-r from-emerald-400 to-green-500 rounded-xl blur-md opacity-0 group-hover/card:opacity-30 transition-opacity duration-300"></div>
                    </div>
                    <div className="min-w-0 flex-1">
                      <h4 className="font-semibold text-gray-900 mb-1 group-hover/card:text-emerald-900 transition-colors duration-300">配置指南</h4>
                      <p className="text-xs text-gray-600 leading-relaxed group-hover/card:text-gray-700 transition-colors duration-300">详细的配置参数说明、环境要求和最佳实践</p>
                    </div>
                  </div>
                  
                  {/* 功能卡片3 - 快速上手 */}
                  <div className="group/card flex items-center space-x-4 p-4 bg-gradient-to-r from-purple-50/80 to-violet-50/60 rounded-xl border border-purple-100/60 hover:border-purple-200/80 hover:shadow-lg hover:scale-[1.02] transition-all duration-300 cursor-pointer">
                    <div className="relative flex-shrink-0">
                      <div className="w-10 h-10 bg-gradient-to-br from-purple-500 to-violet-600 rounded-xl flex items-center justify-center shadow-md group-hover/card:shadow-lg group-hover/card:scale-110 transition-all duration-300">
                        <Zap className="w-5 h-5 text-white group-hover/card:scale-125 transition-transform duration-300" />
                      </div>
                      <div className="absolute -inset-1 bg-gradient-to-r from-purple-400 to-violet-500 rounded-xl blur-md opacity-0 group-hover/card:opacity-30 transition-opacity duration-300"></div>
                    </div>
                    <div className="min-w-0 flex-1">
                      <h4 className="font-semibold text-gray-900 mb-1 group-hover/card:text-purple-900 transition-colors duration-300">快速上手</h4>
                      <p className="text-xs text-gray-600 leading-relaxed group-hover/card:text-gray-700 transition-colors duration-300">示例代码、场景演示、故障排除和性能优化</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
