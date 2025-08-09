"use client"

import React from 'react'
import { ChevronLeft, ChevronRight, Loader2 } from 'lucide-react'

/**
 * 统一的集群步骤操作栏组件
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 */

export interface ActionButton {
  /** 按钮文本 */
  text: string
  /** 点击回调 */
  onClick: () => void
  /** 是否禁用 */
  disabled?: boolean
  /** 是否显示加载状态 */
  loading?: boolean
  /** 加载时的文本 */
  loadingText?: string
  /** 按钮类型 */
  variant?: 'primary' | 'secondary'
  /** 图标位置 */
  iconPosition?: 'left' | 'right'
  /** 自定义图标 */
  icon?: React.ComponentType<{ className?: string }>
}

export interface StatusInfo {
  /** 状态文本 */
  text: string
  /** 数值 */
  value?: number | string
  /** 总数 */
  total?: number | string
  /** 脉冲动画 */
  pulse?: boolean
}

export interface StatusBadge {
  /** 徽章文本 */
  text: string
  /** 徽章类型 */
  variant?: 'success' | 'warning' | 'info'
  /** 是否显示 */
  show: boolean
}

interface ClusterStepActionBarProps {
  /** 左侧状态信息 */
  statusInfo?: StatusInfo
  /** 状态徽章 */
  statusBadge?: StatusBadge
  /** 按钮配置 */
  buttons: ActionButton[]
}

const ClusterStepActionBar: React.FC<ClusterStepActionBarProps> = ({
  statusInfo,
  statusBadge,
  buttons,
}) => {
  const getButtonClass = (button: ActionButton) => {
    const baseClass = "flex items-center px-6 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 shadow-md hover:shadow-lg"
    
    if (button.disabled || button.loading) {
      return `${baseClass} bg-gray-200 text-gray-400 cursor-not-allowed`
    }
    
    if (button.variant === 'secondary') {
      return `${baseClass} px-5 bg-gray-50 hover:bg-gray-100 border border-gray-200 hover:border-gray-300 text-gray-700 shadow-sm hover:shadow-md`
    }
    
    return `${baseClass} bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 text-white transform hover:scale-105`
  }

  const getBadgeClass = (variant?: string) => {
    const baseClass = "flex items-center space-x-2 px-3 py-1.5 rounded-lg border"
    
    switch (variant) {
      case 'success':
        return `${baseClass} bg-green-50 border-green-200`
      case 'warning':
        return `${baseClass} bg-orange-50 border-orange-200`
      case 'info':
      default:
        return `${baseClass} bg-blue-50 border-blue-200`
    }
  }

  const getBadgeTextClass = (variant?: string) => {
    switch (variant) {
      case 'success':
        return "text-sm font-medium text-green-700"
      case 'warning':
        return "text-sm font-medium text-orange-700"
      case 'info':
      default:
        return "text-sm font-medium text-blue-700"
    }
  }

  const getBadgeDotClass = (variant?: string) => {
    switch (variant) {
      case 'success':
        return "w-2 h-2 rounded-full bg-green-500"
      case 'warning':
        return "w-2 h-2 rounded-full bg-orange-500"
      case 'info':
      default:
        return "w-2 h-2 rounded-full bg-blue-500"
    }
  }

  const renderIcon = (button: ActionButton) => {
    if (button.loading) {
      return <Loader2 className="w-4 h-4 animate-spin" />
    }
    
    if (button.icon) {
      const IconComponent = button.icon
      return <IconComponent className="w-4 h-4" />
    }
    
    // 默认图标
    if (button.variant === 'secondary' || button.text.includes('上一步')) {
      return <ChevronLeft className="w-4 h-4" />
    }
    
    return <ChevronRight className="w-4 h-4" />
  }

  return (
    <div className="bg-white/95 backdrop-blur-md border-t border-gray-200/80 p-4 shadow-lg">
      <div className="flex items-center justify-between">
        {/* 左侧状态信息 */}
        <div className="flex items-center space-x-4">
          {statusInfo && (
            <div className="flex items-center space-x-3">
              <div className={`w-3 h-3 rounded-full bg-blue-500 ${statusInfo.pulse ? 'animate-pulse' : ''}`}></div>
              <span className="text-sm font-medium text-gray-700">
                {statusInfo.text}
                {statusInfo.value !== undefined && (
                  <span className="mx-1 px-2 py-0.5 bg-blue-100 text-blue-700 rounded-full text-xs font-semibold">
                    {statusInfo.value}
                  </span>
                )}
                {statusInfo.total !== undefined && (
                  <span className="text-gray-600">
                    {statusInfo.value !== undefined ? ' / ' : ''}{statusInfo.total}
                  </span>
                )}
              </span>
            </div>
          )}
          
          {statusBadge?.show && (
            <div className={getBadgeClass(statusBadge.variant)}>
              <div className={getBadgeDotClass(statusBadge.variant)}></div>
              <span className={getBadgeTextClass(statusBadge.variant)}>
                {statusBadge.text}
              </span>
            </div>
          )}
        </div>
        
        {/* 右侧按钮 */}
        <div className="flex items-center space-x-3">
          {buttons.map((button, index) => (
            <button
              key={index}
              onClick={button.onClick}
              disabled={button.disabled || button.loading}
              className={getButtonClass(button)}
            >
              {button.iconPosition === 'left' || button.variant === 'secondary' || button.text.includes('上一步') ? (
                <>
                  {renderIcon(button)}
                  <span className="ml-2">
                    {button.loading ? (button.loadingText || button.text) : button.text}
                  </span>
                </>
              ) : (
                <>
                  <span className="mr-2">
                    {button.loading ? (button.loadingText || button.text) : button.text}
                  </span>
                  {renderIcon(button)}
                </>
              )}
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}

export default ClusterStepActionBar
