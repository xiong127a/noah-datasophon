"use client"

import React from 'react'
import { ChevronLeft, ChevronRight, Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { DIALOG_STYLES, BUTTON_STYLES } from './shared-styles'

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

interface ClusterWizardActionBarProps {
  /** 左侧状态信息 */
  statusInfo?: StatusInfo
  /** 状态徽章 */
  statusBadge?: StatusBadge
  /** 按钮配置 */
  buttons: ActionButton[]
}

const ClusterWizardActionBar: React.FC<ClusterWizardActionBarProps> = ({
  statusInfo,
  statusBadge,
  buttons,
}) => {
  // 获取框架化按钮样式
  const getButtonStyles = (button: ActionButton) => {
    if (button.disabled || button.loading) {
      return `${BUTTON_STYLES.next} ${BUTTON_STYLES.nextDisabled}`
    }
    
    if (button.variant === 'secondary' || button.text.includes('上一步')) {
      return BUTTON_STYLES.previous
    }
    
    return `${BUTTON_STYLES.next} ${BUTTON_STYLES.nextEnabled}`
  }

  // 获取框架化Badge样式
  const getBadgeVariant = (variant?: string) => {
    switch (variant) {
      case 'success':
        return 'default' // 绿色成功样式
      case 'warning':
        return 'secondary' // 橙色警告样式  
      case 'info':
      default:
        return 'outline' // 蓝色信息样式
    }
  }

  const getBadgeDotColor = (variant?: string) => {
    switch (variant) {
      case 'success':
        return "bg-green-500"
      case 'warning':
        return "bg-orange-500"
      case 'info':
      default:
        return "bg-blue-500"
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
    <div className={DIALOG_STYLES.footer}>
      {/* 装饰性光效 */}
      <div className={DIALOG_STYLES.footerGlow}></div>
      {/* 顶部分割线光效 */}
      <div className={DIALOG_STYLES.footerTopLine}></div>
      
      <div className={DIALOG_STYLES.footerContent}>
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
            <Badge 
              variant={getBadgeVariant(statusBadge.variant)}
              className="flex items-center space-x-2 px-3 py-1.5"
            >
              <div className={`w-2 h-2 rounded-full ${getBadgeDotColor(statusBadge.variant)}`}></div>
              <span className="text-sm font-medium">
                {statusBadge.text}
              </span>
            </Badge>
          )}
        </div>
        
        {/* 右侧按钮 */}
        <div className="flex items-center gap-3">
          {buttons.map((button, index) => (
            <Button
              key={index}
              onClick={button.onClick}
              disabled={button.disabled || button.loading}
              variant={button.variant === 'secondary' || button.text.includes('上一步') ? 'outline' : 'default'}
              className={getButtonStyles(button)}
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
            </Button>
          ))}
        </div>
      </div>
    </div>
  )
}

export default ClusterWizardActionBar
