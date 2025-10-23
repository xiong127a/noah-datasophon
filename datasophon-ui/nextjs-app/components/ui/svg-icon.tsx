import React from 'react'
import { getServiceIconPath } from '@/lib/service-icon-utils'

interface SvgIconProps {
  name: string
  className?: string
  size?: number
}

export const SvgIcon: React.FC<SvgIconProps> = ({ 
  name, 
  className,
  size = 24 
}) => {
  // 使用统一的图标路径工具函数
  const iconPath = getServiceIconPath(name)
  
  return (
    <img
      src={iconPath}
      alt={name}
      width={size}
      height={size}
      className={className}
      onError={(e) => {
        const target = e.target as HTMLImageElement
        // 如果主图标加载失败，尝试默认图标
        if (!target.src.endsWith('service-default.svg')) {
          target.src = '/icons/service-default.svg'
        }
      }}
      title={`${name} 服务图标`}
    />
  )
}

export default SvgIcon 