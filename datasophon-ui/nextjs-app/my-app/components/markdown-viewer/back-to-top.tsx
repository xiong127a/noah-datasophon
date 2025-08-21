"use client"

/**
 * 返回顶部按钮组件
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-20
 */

import { useState, useEffect } from 'react'
import { ChevronUp } from 'lucide-react'

interface BackToTopProps {
  targetRef: React.RefObject<HTMLElement>
  showThreshold?: number // 显示按钮的滚动阈值，默认300px
}

const BackToTop: React.FC<BackToTopProps> = ({ 
  targetRef, 
  showThreshold = 300 
}) => {
  const [isVisible, setIsVisible] = useState(false)

  // 监听滚动事件
  useEffect(() => {
    const handleScroll = () => {
      if (targetRef.current) {
        const scrollTop = targetRef.current.scrollTop
        setIsVisible(scrollTop > showThreshold)
      }
    }

    const element = targetRef.current
    if (element) {
      element.addEventListener('scroll', handleScroll)
      return () => element.removeEventListener('scroll', handleScroll)
    }
  }, [targetRef, showThreshold])

  // 滚动到顶部
  const scrollToTop = () => {
    if (targetRef.current) {
      targetRef.current.scrollTo({
        top: 0,
        behavior: 'smooth'
      })
    }
  }

  if (!isVisible) return null

  return (
    <button
      onClick={scrollToTop}
      className="fixed bottom-20 right-8 z-50 group"
      title="返回顶部"
    >
      {/* 主按钮 */}
      <div className="w-12 h-12 bg-gradient-to-r from-blue-500 to-indigo-600 rounded-full shadow-lg hover:shadow-xl transform transition-all duration-300 ease-out group-hover:scale-110 group-active:scale-95 flex items-center justify-center">
        <ChevronUp className="w-6 h-6 text-white transition-transform duration-200 group-hover:-translate-y-0.5" />
      </div>
      
      {/* 发光效果 */}
      <div className="absolute inset-0 w-12 h-12 bg-gradient-to-r from-blue-400 to-indigo-500 rounded-full opacity-0 group-hover:opacity-20 transition-opacity duration-300 animate-pulse"></div>
      
      {/* 提示文字 */}
      <div className="absolute right-full mr-3 top-1/2 -translate-y-1/2 opacity-0 group-hover:opacity-100 transition-all duration-200 pointer-events-none">
        <div className="bg-gray-800 text-white text-sm px-3 py-1.5 rounded-lg shadow-lg whitespace-nowrap">
          返回顶部
          {/* 小箭头 */}
          <div className="absolute left-full top-1/2 -translate-y-1/2 w-0 h-0 border-l-4 border-l-gray-800 border-y-4 border-y-transparent"></div>
        </div>
      </div>
    </button>
  )
}

export default BackToTop
