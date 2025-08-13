"use client"

import React, { useState } from 'react'
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
 * 苹果风格悬浮提示组件 - 优化版
 * 作者：任相鹏  
 * 邮箱：635887935@qq.com
 * 日期：2024-01-31
 * 
 * 特点：
 * - 简化的实现，确保样式生效
 * - 苹果风格的毛玻璃效果
 * - 精美的阴影和动画
 * - 可选的悬浮指示图标
 */

const AppleTooltip: React.FC<AppleTooltipProps> = ({
  content,
  children,
  delay = 300,
  placement = 'top',
  maxWidth = 320,
  showIcon = true
}) => {
  const [isVisible, setIsVisible] = useState(false)

  const handleMouseEnter = () => {
    setIsVisible(true)
  }

  const handleMouseLeave = () => {
    setIsVisible(false)
  }

  // 获取位置样式
  const getPositionStyles = () => {
    switch (placement) {
      case 'top':
        return {
          container: 'bottom-full left-1/2 transform -translate-x-1/2 mb-2',
          arrow: 'top-full left-1/2 transform -translate-x-1/2 border-l-transparent border-r-transparent border-b-transparent'
        }
      case 'bottom':
        return {
          container: 'top-full left-1/2 transform -translate-x-1/2 mt-2',
          arrow: 'bottom-full left-1/2 transform -translate-x-1/2 border-l-transparent border-r-transparent border-t-transparent'
        }
      case 'left':
        return {
          container: 'right-full top-1/2 transform -translate-y-1/2 mr-2',
          arrow: 'left-full top-1/2 transform -translate-y-1/2 border-t-transparent border-b-transparent border-r-transparent'
        }
      case 'right':
        return {
          container: 'left-full top-1/2 transform -translate-y-1/2 ml-2',
          arrow: 'right-full top-1/2 transform -translate-y-1/2 border-t-transparent border-b-transparent border-l-transparent'
        }
      default:
        return {
          container: 'bottom-full left-1/2 transform -translate-x-1/2 mb-2',
          arrow: 'top-full left-1/2 transform -translate-x-1/2'
        }
    }
  }

  const positionStyles = getPositionStyles()

  return (
    <div className="relative inline-block">
      {/* 触发元素 */}
      <div 
        className="flex items-center gap-1 cursor-help group"
        onMouseEnter={handleMouseEnter}
        onMouseLeave={handleMouseLeave}
      >
        {children}
        {showIcon && (
          <HelpCircle className="h-3 w-3 text-gray-400 opacity-60 group-hover:opacity-100 group-hover:text-blue-500 transition-all duration-200" />
        )}
      </div>

      {/* 苹果风格的悬浮提示 */}
      {isVisible && (
        <div 
          className={`
            absolute z-50 pointer-events-none animate-in fade-in-0 zoom-in-95 duration-200
            ${positionStyles.container}
          `}
          style={{ maxWidth: `${maxWidth}px` }}
        >
          {/* 主要内容卡片 */}
          <div className="relative">
            {/* 苹果风格背景 */}
            <div className="
              relative overflow-hidden rounded-2xl
              bg-gradient-to-br from-white via-white/95 to-gray-50/90
              backdrop-blur-xl backdrop-saturate-150
              border border-white/20
              shadow-2xl shadow-black/10
              ring-1 ring-black/5
            ">
              {/* 内容区域 */}
              <div className="relative p-4">
                {/* 精美的背景纹理 */}
                <div className="absolute inset-0 bg-gradient-to-br from-transparent via-white/10 to-transparent pointer-events-none" />
                
                {/* 实际内容 */}
                <div className="relative z-10">
                  {typeof content === 'string' ? (
                    <div className="space-y-3">
                      {content.split('\n').map((line, index) => {
                        const [label, value] = line.split(': ')
                        return (
                          <div key={index}>
                            {value ? (
                              <div className="space-y-1">
                                <div className="text-xs font-semibold text-gray-900 tracking-wide uppercase opacity-70">
                                  {label}
                                </div>
                                <div className="text-sm text-gray-800 leading-relaxed font-medium break-all">
                                  {value}
                                </div>
                              </div>
                            ) : (
                              <div className="text-sm text-gray-700 leading-relaxed">
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
              bg-gradient-to-br from-white to-gray-50
              border border-white/20
              rotate-45
              ${placement === 'top' ? 'top-full left-1/2 transform -translate-x-1/2 -mt-1.5' :
                placement === 'bottom' ? 'bottom-full left-1/2 transform -translate-x-1/2 -mb-1.5' :
                placement === 'left' ? 'left-full top-1/2 transform -translate-y-1/2 -ml-1.5' :
                'right-full top-1/2 transform -translate-y-1/2 -mr-1.5'
              }
            `} />
          </div>
        </div>
      )}
    </div>
  )
}

export default AppleTooltip
