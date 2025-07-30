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

interface RestrictedMenuLinkProps {
  href: string
  icon: LucideIcon
  colorClass: string
  children: React.ReactNode
  className?: string
  requiresCluster?: boolean
}

const RestrictedMenuLink: React.FC<RestrictedMenuLinkProps> = ({
  href,
  icon: Icon,
  colorClass,
  children,
  className = "",
  requiresCluster = false
}) => {
  const { hasCluster, requiresCluster: checkRequiresCluster } = useCluster()
  
  // 检查是否需要集群权限
  const needsCluster = requiresCluster || checkRequiresCluster(href)
  const isDisabled = needsCluster && !hasCluster

  const linkContent = (
    <div
      className={`
        group flex items-center rounded-2xl px-4 py-3 text-sm font-medium transition-all duration-200
        ${isDisabled 
          ? 'cursor-not-allowed opacity-50 bg-gray-50 text-gray-400' 
          : `hover:bg-gradient-to-r hover:shadow-lg transition-all duration-200 ${getHoverClasses(colorClass)}`
        }
        ${className}
      `}
    >
      <Icon className={`mr-3 h-4 w-4 ${isDisabled ? 'text-gray-400' : getIconClasses(colorClass)}`} />
      <span className={isDisabled ? 'text-gray-400' : ''}>{children}</span>
      {isDisabled && (
        <AlertCircle className="ml-auto h-4 w-4 text-gray-400" />
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
            side="right" 
            className="bg-yellow-50 border-yellow-200 text-yellow-800 shadow-lg"
          >
            <div className="flex items-center space-x-2">
              <AlertCircle className="h-4 w-4" />
              <span>请先选择要管理的集群</span>
            </div>
            <div className="text-xs mt-1 text-yellow-700">
              前往"集群管理 → 集群列表"选择集群
            </div>
          </TooltipContent>
        </Tooltip>
      </TooltipProvider>
    )
  }

  return (
    <Link href={href} className="block w-full">
      {linkContent}
    </Link>
  )
}

// 获取悬停样式类
const getHoverClasses = (colorClass: string): string => {
  const hoverMap: Record<string, string> = {
    blue: 'hover:from-blue-50 hover:to-indigo-50 hover:shadow-blue-100/50',
    green: 'hover:from-green-50 hover:to-emerald-50 hover:shadow-green-100/50',
    purple: 'hover:from-purple-50 hover:to-violet-50 hover:shadow-purple-100/50',
    orange: 'hover:from-orange-50 hover:to-amber-50 hover:shadow-orange-100/50',
    slate: 'hover:from-slate-50 hover:to-gray-50 hover:shadow-slate-100/50',
    red: 'hover:from-red-50 hover:to-pink-50 hover:shadow-red-100/50',
  }
  return hoverMap[colorClass] || hoverMap.blue
}

// 获取图标样式类
const getIconClasses = (colorClass: string): string => {
  const iconMap: Record<string, string> = {
    blue: 'text-slate-600 group-hover:text-blue-600',
    green: 'text-slate-600 group-hover:text-green-600',
    purple: 'text-slate-600 group-hover:text-purple-600',
    orange: 'text-slate-600 group-hover:text-orange-600',
    slate: 'text-slate-600 group-hover:text-slate-600',
    red: 'text-slate-600 group-hover:text-red-600',
  }
  return iconMap[colorClass] || iconMap.blue
}

export default RestrictedMenuLink