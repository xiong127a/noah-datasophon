"use client"

import React, { useState, useRef, useEffect } from 'react'
import { createPortal } from 'react-dom'
import { HelpCircle } from 'lucide-react'

interface AppleTooltipProps {
  content: React.ReactNode
  children: React.ReactElement
  delay?: number
  placement?: 'top' | 'bottom' | 'left' | 'right'
  maxWidth?: number
  showIcon?: boolean
}

/**
 * 苹果风格悬浮提示组件 - 智能定位版
 * 作者：任相鹏  
 * 邮箱：635887935@qq.com
 * 日期：2024-01-31
 * 
 * 特点：
 * - 智能边界检测和位置调整
 * - 苹果风格的毛玻璃效果
 * - 精美的阴影和动画
 * - 可选的悬浮指示图标
 */

const AppleTooltip: React.FC<AppleTooltipProps> = ({
  content,
  children,
  delay = 300,
  placement = 'top',
  maxWidth = 700,
  showIcon = true
}) => {
  const [isVisible, setIsVisible] = useState(false)
  const [position, setPosition] = useState({ x: 0, y: 0 })
  const [actualPlacement, setActualPlacement] = useState(placement)
  const triggerRef = useRef<HTMLDivElement>(null)
  const tooltipRef = useRef<HTMLDivElement>(null)

  const handleMouseEnter = () => {
    setIsVisible(true)
  }

  const handleMouseLeave = () => {
    setIsVisible(false)
  }

  // 计算智能位置
  const calculatePosition = () => {
    if (!triggerRef.current || !tooltipRef.current) return

    const triggerRect = triggerRef.current.getBoundingClientRect()
    const tooltipRect = tooltipRef.current.getBoundingClientRect()
    const viewport = {
      width: window.innerWidth,
      height: window.innerHeight
    }

    const offset = 12
    let x = 0
    let y = 0
    let finalPlacement = placement

    // 首选位置计算
    switch (placement) {
      case 'top':
        x = triggerRect.left + triggerRect.width / 2 - tooltipRect.width / 2
        y = triggerRect.top - tooltipRect.height - offset
        break
      case 'bottom':
        x = triggerRect.left + triggerRect.width / 2 - tooltipRect.width / 2
        y = triggerRect.bottom + offset
        break
      case 'left':
        x = triggerRect.left - tooltipRect.width - offset
        y = triggerRect.top + triggerRect.height / 2 - tooltipRect.height / 2
        break
      case 'right':
        x = triggerRect.right + offset
        y = triggerRect.top + triggerRect.height / 2 - tooltipRect.height / 2
        break
    }

    // 边界检测和智能调整
    const padding = 16

    // 水平边界检测
    if (x < padding) {
      x = padding
      if (placement === 'top' || placement === 'bottom') {
        // 如果是顶部或底部显示，调整水平位置但保持原方向
        x = Math.max(padding, Math.min(viewport.width - tooltipRect.width - padding, x))
      } else if (placement === 'left') {
        // 如果左侧空间不足，改为右侧显示
        x = triggerRect.right + offset
        finalPlacement = 'right'
      }
    } else if (x + tooltipRect.width > viewport.width - padding) {
      if (placement === 'top' || placement === 'bottom') {
        x = viewport.width - tooltipRect.width - padding
      } else if (placement === 'right') {
        // 如果右侧空间不足，改为左侧显示
        x = triggerRect.left - tooltipRect.width - offset
        finalPlacement = 'left'
      }
    }

    // 垂直边界检测
    if (y < padding) {
      if (placement === 'top') {
        // 如果顶部空间不足，改为底部显示
        y = triggerRect.bottom + offset
        finalPlacement = 'bottom'
      } else {
        y = padding
      }
    } else if (y + tooltipRect.height > viewport.height - padding) {
      if (placement === 'bottom') {
        // 如果底部空间不足，改为顶部显示
        y = triggerRect.top - tooltipRect.height - offset
        finalPlacement = 'top'
      } else {
        y = viewport.height - tooltipRect.height - padding
      }
    }

    setPosition({ x, y })
    setActualPlacement(finalPlacement)
  }

  useEffect(() => {
    if (isVisible) {
      calculatePosition()
      const handleResize = () => calculatePosition()
      const handleScroll = () => calculatePosition()
      
      window.addEventListener('resize', handleResize)
      window.addEventListener('scroll', handleScroll, true)
      
      return () => {
        window.removeEventListener('resize', handleResize)
        window.removeEventListener('scroll', handleScroll, true)
      }
    }
  }, [isVisible])

  // 获取箭头位置
  const getArrowPosition = () => {
    switch (actualPlacement) {
      case 'top':
        return 'top-full left-1/2 transform -translate-x-1/2 -mt-1.5'
      case 'bottom':
        return 'bottom-full left-1/2 transform -translate-x-1/2 -mb-1.5'
      case 'left':
        return 'left-full top-1/2 transform -translate-y-1/2 -ml-1.5'
      case 'right':
        return 'right-full top-1/2 transform -translate-y-1/2 -mr-1.5'
      default:
        return 'top-full left-1/2 transform -translate-x-1/2 -mt-1.5'
    }
  }

  const tooltipContent = isVisible && (
    <div
      ref={tooltipRef}
      className="fixed z-50 pointer-events-none animate-in fade-in-0 zoom-in-95 duration-200"
      style={{
        left: position.x,
        top: position.y,
        maxWidth: `${maxWidth}px`,
        minWidth: '300px'
      }}
    >
      {/* 主要内容卡片 */}
      <div className="relative">
        {/* 苹果风格背景 */}
        <div className="
          relative overflow-hidden rounded-2xl
          bg-gradient-to-br from-white via-gray-50/80 to-white/90
          backdrop-blur-2xl backdrop-saturate-200
          border border-gray-200/60
          shadow-2xl shadow-blue-500/10
          ring-1 ring-gray-300/20
        ">
          {/* 内容区域 */}
          <div className="relative p-5">
            {/* 精美的背景纹理 */}
            <div className="absolute inset-0 bg-gradient-to-br from-transparent via-white/10 to-transparent pointer-events-none" />
            
            {/* 实际内容 */}
            <div className="relative z-10 min-w-0">
              {typeof content === 'string' ? (
                <div className="space-y-3">
                  {content.split('\n').map((line, index) => {
                    const [label, value] = line.split(': ')
                    return (
                      <div key={index}>
                        {value ? (
                          <div className="space-y-1.5">
                            <div className="text-xs font-bold text-blue-800 tracking-wider uppercase opacity-80">
                              {label}
                            </div>
                            <div 
                              className="text-sm text-gray-900 leading-relaxed font-medium bg-gray-50/60 px-4 py-2.5 rounded-lg border border-gray-200/40"
                              style={{
                                wordBreak: 'keep-all',
                                overflowWrap: 'break-word',
                                hyphens: 'auto'
                              }}
                            >
                              {value}
                            </div>
                          </div>
                        ) : (
                          <div className="text-sm text-gray-800 leading-relaxed font-medium">
                            {line}
                          </div>
                        )}
                      </div>
                    )
                  })}
                </div>
              ) : (
                content
              )}
            </div>
          </div>
        </div>

        {/* 箭头指示器 */}
        <div className={`
          absolute w-3 h-3 
          bg-gradient-to-br from-white via-gray-50/80 to-white/90
          border border-gray-200/60
          rotate-45 shadow-lg
          ${getArrowPosition()}
        `} />
      </div>
    </div>
  )

  return (
    <>
      {/* 触发元素 */}
      <div 
        ref={triggerRef}
        className="flex items-center gap-1 cursor-help group"
        onMouseEnter={handleMouseEnter}
        onMouseLeave={handleMouseLeave}
      >
        {children}
        {showIcon && (
          <HelpCircle className="h-3 w-3 text-gray-400 opacity-60 group-hover:opacity-100 group-hover:text-blue-500 transition-all duration-200" />
        )}
      </div>

      {/* Portal渲染悬浮提示 */}
      {typeof document !== 'undefined' && tooltipContent && createPortal(tooltipContent, document.body)}
    </>
  )
}

export default AppleTooltip
