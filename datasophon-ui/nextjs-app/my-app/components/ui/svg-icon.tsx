import React from 'react'

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
  // 服务名称标准化处理：转为小写
  const iconFileName = name.toUpperCase().trim().toLowerCase()
  const iconPath = `/icons/${iconFileName}.svg`
  
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