"use client"

import React from 'react'
import Image from 'next/image'
import { Package } from 'lucide-react'

interface ServiceIconProps {
  /** 服务名称 */
  serviceName: string
  /** 图标大小 */
  size?: number
  /** 额外的CSS类名 */
  className?: string
}

/**
 * 服务图标组件
 * 根据服务名称自动加载对应的SVG图标
 * 
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 * 日期：2024-01-20
 */
const ServiceIcon: React.FC<ServiceIconProps> = ({ 
  serviceName, 
  size = 32, 
  className = "" 
}) => {
  // 将服务名称转换为图标文件名（小写）
  const iconName = serviceName.toLowerCase()
  const iconPath = `/icons/${iconName}.svg`
  
  // 错误处理：如果图标加载失败，显示默认图标
  const handleImageError = (e: React.SyntheticEvent<HTMLImageElement>) => {
    e.currentTarget.src = '/icons/service-default.svg'
  }

  return (
    <div className={`flex-shrink-0 ${className}`}>
      <Image
        src={iconPath}
        alt={`${serviceName} 图标`}
        width={size}
        height={size}
        className="object-contain"
        onError={handleImageError}
        priority={false}
        // 如果SVG加载失败，显示默认图标
        onLoadingComplete={(result) => {
          if (result.naturalWidth === 0) {
            handleImageError(result as any)
          }
        }}
      />
    </div>
  )
}

export default ServiceIcon
