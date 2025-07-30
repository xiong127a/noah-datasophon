import React from 'react'

interface SvgIconProps {
  name: string
  className?: string
  size?: number
}

// 服务名称映射表 - 将服务名称映射到对应的SVG文件名
const SERVICE_ICON_MAP: { [key: string]: string } = {
  'SPARK': 'spark3',           // SPARK服务使用spark3.svg
  'SPARK3': 'spark3',
  'REDISSENTINEL': 'redissentinel',
  'NOAHJOB': 'noahjob',
  'NOAHSYNC': 'noahsync',
  'NEBULAGRAPH': 'nebulagraph',
  'OPENLDAP': 'openldap',
  'PUSHGATEWAY': 'pushgateway',
  'STREAMPARK': 'streampark',
  'STARROCKS': 'starrocks',
  'SEATUNNEL': 'seatunnel',
  'JUICEFS': 'juicefs',
  'ICEBERG': 'iceberg',
  'POSTGRESQL': 'postgresql',
  'PROMETHEUS': 'prometheus',
  'KYUUBI': 'kyuubi',
  'LOGSTASH': 'logstash',
  'MINIO': 'minio',
  'NEO4J': 'neo4j',
  'PAIMON': 'paimon',
  'RANGER': 'ranger',
  'REDIS': 'redis',
  'TRINO': 'trino',
  'ZEPPELIN': 'zeppelin',
  'KERBEROS': 'kerberos',
}

export const SvgIcon: React.FC<SvgIconProps> = ({ 
  name, 
  className,
  size = 24 
}) => {
  // 服务名称标准化处理
  const normalizedName = name.toUpperCase().trim()
  
  // 获取图标文件名
  const getIconFileName = (serviceName: string): string => {
    // 首先检查映射表
    if (SERVICE_ICON_MAP[serviceName]) {
      return SERVICE_ICON_MAP[serviceName]
    }
    
    // 如果没有映射，使用小写的服务名称
    return serviceName.toLowerCase()
  }
  
  const iconFileName = getIconFileName(normalizedName)
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