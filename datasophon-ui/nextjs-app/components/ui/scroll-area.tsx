/**
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 * @description 美观滚动区域组件 - 应用自定义滚动条样式
 */

"use client"

import * as React from "react"
import { cn } from "@/lib/utils"

interface ScrollAreaProps extends React.HTMLAttributes<HTMLDivElement> {
  children: React.ReactNode
  /** 滚动条样式类型 */
  scrollType?: 'default' | 'k8s' | 'table' | 'namespace' | 'hidden'
  /** 是否显示渐变边缘效果 */
  showGradient?: boolean
}

const ScrollArea = React.forwardRef<HTMLDivElement, ScrollAreaProps>(
  ({ className, children, scrollType = 'default', showGradient = false, ...props }, ref) => {
    const scrollClassMap = {
      default: '',
      k8s: 'k8s-scroll',
      table: 'table-scroll', 
      namespace: 'namespace-scroll',
      hidden: 'scroll-hidden'
    }

    return (
      <div
        ref={ref}
        className={cn(
          "relative overflow-auto",
          scrollClassMap[scrollType],
          showGradient && "after:content-[''] after:absolute after:bottom-0 after:left-0 after:right-0 after:h-4 after:bg-gradient-to-t after:from-white/80 after:to-transparent after:pointer-events-none",
          className
        )}
        {...props}
      >
        {children}
      </div>
    )
  }
)
ScrollArea.displayName = "ScrollArea"

export { ScrollArea }
