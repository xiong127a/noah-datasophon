"use client"

import React from 'react'
import { 
  ChevronRight, Loader2
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
// import { ScrollArea } from '@/components/ui/scroll-area' // 暂时注释，使用div代替
import Image from 'next/image'

interface ServiceConfigNavigationProps {
  services: string[]
  activeService: string
  onServiceChange: (serviceName: string) => void
  loading: boolean
}

/**
 * 服务配置导航组件 - 现代化设计
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 */

const ServiceConfigNavigation: React.FC<ServiceConfigNavigationProps> = ({
  services,
  activeService,
  onServiceChange,
  loading
}) => {
  // 获取服务图标
  const getServiceIcon = (serviceName: string) => {
    const service = serviceName.toLowerCase()
    
    const iconMap: Record<string, string> = {
      'hdfs': '/icons/hdfs.svg',
      'yarn': '/icons/yarn.svg', 
      'hive': '/icons/hive.svg',
      'hbase': '/icons/hbase.svg',
      'kafka': '/icons/kafka.svg',
      'zookeeper': '/icons/zookeeper.svg',
      'spark': '/icons/spark3.svg',
      'flink': '/icons/flink.svg',
      'elasticsearch': '/icons/elasticsearch.svg',
      'kibana': '/icons/kibana.svg',
      'prometheus': '/icons/prometheus.svg',
      'grafana': '/icons/grafana.svg',
      'redis': '/icons/redis.svg',
      'doris': '/icons/doris.svg',
      'clickhouse': '/icons/clickhouse.svg',
      'trino': '/icons/trino.svg'
    }
    
    for (const [component, icon] of Object.entries(iconMap)) {
      if (service.includes(component)) {
        return icon
      }
    }
    
    return '/icons/service-default.svg'
  }



  return (
    <Card className="h-full flex flex-col border-gray-200/60 shadow-lg bg-white/90 backdrop-blur-sm rounded-xl overflow-hidden">
      <CardHeader className="pb-3 bg-gradient-to-r from-gray-50/50 to-blue-50/30 border-b border-gray-100">
        <CardTitle className="text-base font-medium text-gray-800">
          服务列表
        </CardTitle>
      </CardHeader>
      
      <CardContent className="flex-1 p-0">
        <div className="h-full overflow-y-auto">
          <div className="space-y-2 p-4">
            {services.map((serviceName) => {
              const isActive = activeService === serviceName

              return (
                <Button
                  key={serviceName}
                  variant="ghost"
                  onClick={() => onServiceChange(serviceName)}
                  disabled={loading}
                  className={`w-full h-auto p-0 justify-start hover:bg-transparent ${
                    isActive ? 'ring-2 ring-blue-500 ring-offset-2' : ''
                  }`}
                >
                  <div className={`w-full p-3 rounded-xl border transition-all duration-300 ${
                    isActive 
                      ? 'bg-gradient-to-br from-blue-50 to-indigo-50/80 border-blue-200/80 shadow-lg shadow-blue-100/50 scale-[1.02]'
                      : 'bg-white/80 border-gray-200/60 hover:border-blue-200/60 hover:shadow-md hover:bg-gradient-to-br hover:from-white hover:to-blue-50/20 hover:scale-[1.01]'
                  }`}>
                    <div className="flex items-center gap-3">
                      {/* 服务图标 */}
                      <div className={`w-8 h-8 flex-shrink-0 relative p-1 rounded-lg transition-all duration-300 ${
                        isActive 
                          ? 'bg-white/60 shadow-sm' 
                          : 'group-hover:bg-white/40'
                      }`}>
                        <Image
                          src={getServiceIcon(serviceName)}
                          alt={serviceName}
                          width={32}
                          height={32}
                          className="w-full h-full object-contain filter transition-all duration-300"
                        />
                      </div>
                      
                      {/* 服务信息 */}
                      <div className="flex-1 min-w-0 text-left">
                        <div className="font-medium text-sm text-gray-900 truncate">
                          {serviceName}
                        </div>
                      </div>
                      
                      {/* 选中指示器 */}
                      {isActive && (
                        <ChevronRight className="h-4 w-4 text-blue-600 flex-shrink-0" />
                      )}
                      
                      {/* 加载指示器 */}
                      {loading && isActive && (
                        <Loader2 className="h-4 w-4 animate-spin text-blue-600 flex-shrink-0" />
                      )}
                    </div>
                  </div>
                </Button>
              )
            })}
          </div>
        </div>
      </CardContent>
    </Card>
  )
}

export default ServiceConfigNavigation
