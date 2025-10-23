"use client"

/**
 * 阅读进度指示器 - 苹果风格设计
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-20
 */

import React, { useState, useEffect } from 'react'
import { ReadingProgressProps } from './types'

const ReadingProgress: React.FC<ReadingProgressProps> = ({ targetRef }) => {
  const [progress, setProgress] = useState(0)
  const [isVisible, setIsVisible] = useState(false)

  useEffect(() => {
    const target = targetRef.current
    if (!target) return

    const handleScroll = () => {
      const rect = target.getBoundingClientRect()
      const windowHeight = window.innerHeight
      const documentHeight = target.scrollHeight
      const scrollTop = window.pageYOffset || document.documentElement.scrollTop

      // 计算滚动进度
      const scrolled = scrollTop
      const maxScroll = documentHeight - windowHeight
      const progressPercent = Math.min(100, Math.max(0, (scrolled / maxScroll) * 100))

      setProgress(progressPercent)
      
      // 当滚动超过一定位置时显示进度条
      setIsVisible(scrolled > 100)
    }

    // 防抖处理
    let timeoutId: NodeJS.Timeout
    const debouncedHandleScroll = () => {
      clearTimeout(timeoutId)
      timeoutId = setTimeout(handleScroll, 10)
    }

    window.addEventListener('scroll', debouncedHandleScroll, { passive: true })
    
    // 初始化
    handleScroll()

    return () => {
      window.removeEventListener('scroll', debouncedHandleScroll)
      clearTimeout(timeoutId)
    }
  }, [targetRef])

  if (!isVisible) return null

  return (
    <>
      {/* 顶部进度条 */}
      <div className="fixed top-0 left-0 right-0 z-50">
        <div className="h-1 bg-gray-200">
          <div 
            className="h-full bg-gradient-to-r from-blue-500 to-blue-600 transition-all duration-300 ease-out"
            style={{ width: `${progress}%` }}
          />
        </div>
      </div>

      {/* 圆形进度指示器 */}
      <div className="fixed bottom-6 right-6 z-40">
        <div className="relative w-12 h-12">
          {/* 背景圆圈 */}
          <svg 
            className="w-12 h-12 transform -rotate-90" 
            viewBox="0 0 36 36"
          >
            <circle
              cx="18"
              cy="18"
              r="16"
              fill="none"
              stroke="rgb(229, 231, 235)"
              strokeWidth="2"
            />
            <circle
              cx="18"
              cy="18"
              r="16"
              fill="none"
              stroke="rgb(59, 130, 246)"
              strokeWidth="2"
              strokeDasharray={`${progress * 1.005}, 100.5`}
              strokeLinecap="round"
              className="transition-all duration-300 ease-out"
            />
          </svg>
          
          {/* 中心百分比文本 */}
          <div className="absolute inset-0 flex items-center justify-center">
            <span className="text-xs font-semibold text-blue-600">
              {Math.round(progress)}%
            </span>
          </div>
          
          {/* 悬浮效果 */}
          <div className="absolute inset-0 bg-white rounded-full shadow-lg opacity-90" />
          <div className="absolute inset-0 flex items-center justify-center">
            <span className="text-xs font-semibold text-blue-600 relative z-10">
              {Math.round(progress)}%
            </span>
          </div>
        </div>
      </div>
    </>
  )
}

export default ReadingProgress
