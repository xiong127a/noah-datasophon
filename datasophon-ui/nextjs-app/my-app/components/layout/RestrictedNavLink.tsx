"use client"

import React from 'react'
import Link from 'next/link'
import { LucideIcon, AlertCircle } from 'lucide-react'
import { useCluster } from '@/hooks/useCluster'
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip"

interface RestrictedNavLinkProps {
  href: string
  icon: LucideIcon
  colorClass: string
  children: React.ReactNode
}

const RestrictedNavLink: React.FC<RestrictedNavLinkProps> = ({
  href,
  icon: Icon,
  colorClass,
  children,
}) => {
  const { hasCluster, requiresCluster } = useCluster()
  
  // 检查是否需要集群权限
  const needsCluster = requiresCluster(href)
  const isDisabled = needsCluster && !hasCluster

  const linkContent = (
    <div
      className={`
        group inline-flex h-12 items-center justify-center rounded-2xl px-6 py-2 text-sm font-medium transition-all duration-200
        ${isDisabled 
          ? 'cursor-not-allowed opacity-50' 
          : `hover:bg-gradient-to-r hover:shadow-lg ${getHoverClasses(colorClass)} focus:bg-gradient-to-r ${getFocusClasses(colorClass)} focus:outline-none`
        }
      `}
    >
      <Icon className={`mr-2 h-4 w-4 transition-colors ${
        isDisabled ? 'text-gray-400' : getIconClasses(colorClass)
      }`} />
      <span className={isDisabled ? 'text-gray-400' : 'text-slate-700 group-hover:text-slate-900'}>
        {children}
      </span>
      {isDisabled && (
        <AlertCircle className="ml-2 h-4 w-4 text-gray-400" />
      )}
    </div>
  )

  if (isDisabled) {
    return (
      <TooltipProvider>
        <Tooltip>
          <TooltipTrigger asChild>
            <div onClick={(e) => e.preventDefault()}>
              {linkContent}
            </div>
          </TooltipTrigger>
          <TooltipContent 
            side="bottom" 
            className="bg-yellow-50 border-yellow-200 text-yellow-800 shadow-lg max-w-xs"
          >
            <div className="flex items-center space-x-2">
              <AlertCircle className="h-4 w-4" />
              <div>
                <div className="font-medium">请先选择集群</div>
                <div className="text-xs mt-1 text-yellow-700">
                  前往"集群管理 → 集群列表"选择要管理的集群
                </div>
              </div>
            </div>
          </TooltipContent>
        </Tooltip>
      </TooltipProvider>
    )
  }

  return (
    <Link href={href}>
      {linkContent}
    </Link>
  )
}

// 获取悬停样式类
const getHoverClasses = (colorClass: string): string => {
  const hoverMap: Record<string, string> = {
    green: 'hover:from-green-50 hover:to-emerald-50 hover:shadow-green-100/50',
    orange: 'hover:from-orange-50 hover:to-red-50 hover:shadow-orange-100/50',
    blue: 'hover:from-blue-50 hover:to-indigo-50 hover:shadow-blue-100/50',
    purple: 'hover:from-purple-50 hover:to-violet-50 hover:shadow-purple-100/50',
    slate: 'hover:from-slate-50 hover:to-gray-50 hover:shadow-slate-100/50',
  }
  return hoverMap[colorClass] || hoverMap.blue
}

// 获取焦点样式类
const getFocusClasses = (colorClass: string): string => {
  const focusMap: Record<string, string> = {
    green: 'focus:from-green-50 focus:to-emerald-50',
    orange: 'focus:from-orange-50 focus:to-red-50',
    blue: 'focus:from-blue-50 focus:to-indigo-50',
    purple: 'focus:from-purple-50 focus:to-violet-50',
    slate: 'focus:from-slate-50 focus:to-gray-50',
  }
  return focusMap[colorClass] || focusMap.blue
}

// 获取图标样式类
const getIconClasses = (colorClass: string): string => {
  const iconMap: Record<string, string> = {
    green: 'text-slate-600 group-hover:text-green-600',
    orange: 'text-slate-600 group-hover:text-orange-600',
    blue: 'text-slate-600 group-hover:text-blue-600',
    purple: 'text-slate-600 group-hover:text-purple-600',
    slate: 'text-slate-600 group-hover:text-slate-600',
  }
  return iconMap[colorClass] || iconMap.blue
}

export default RestrictedNavLink