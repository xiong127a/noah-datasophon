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
    <Card className="
      h-full flex flex-col border-0 shadow-xl 
      bg-gradient-to-b from-white via-gray-50/30 to-white
      ring-1 ring-gray-200/40
      rounded-2xl overflow-hidden backdrop-blur-xl
    ">
      <CardHeader className="pb-4 bg-gradient-to-r from-blue-50/60 via-indigo-50/40 to-purple-50/30 border-b border-gray-200/40">
        <CardTitle className="text-lg font-semibold text-gray-800">
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
                  type="button"
                  variant="ghost"
                  onClick={() => onServiceChange(serviceName)}
                  disabled={loading}
                  className={`w-full h-auto p-0 justify-start hover:bg-transparent ${
                    isActive ? 'ring-2 ring-blue-500 ring-offset-2' : ''
                  }`}
                >
                  <div className={`w-full p-4 rounded-2xl border-0 transition-all duration-400 ${
                    isActive 
                      ? 'bg-gradient-to-br from-blue-600/10 via-indigo-500/8 to-purple-500/10 ring-2 ring-blue-300/60 shadow-xl shadow-blue-200/30 scale-[1.03]'
                      : 'bg-gradient-to-br from-white/90 via-gray-50/40 to-white/90 ring-1 ring-gray-200/40 hover:ring-blue-200/50 hover:shadow-lg hover:bg-gradient-to-br hover:from-blue-50/30 hover:to-indigo-50/20 hover:scale-[1.02]'
                  }`}>
                    <div className="flex items-center gap-3">
                      {/* 服务图标 */}
                      <div className={`w-10 h-10 flex-shrink-0 relative p-2 rounded-xl transition-all duration-300 ${
                        isActive 
                          ? 'bg-gradient-to-br from-white/80 to-blue-50/60 shadow-lg ring-1 ring-blue-200/40' 
                          : 'bg-gradient-to-br from-white/60 to-gray-50/40 group-hover:bg-gradient-to-br group-hover:from-white/80 group-hover:to-blue-50/40 group-hover:shadow-md'
                      }`}>
                        <Image
                          src={getServiceIcon(serviceName)}
                          alt={serviceName}
                          width={24}
                          height={24}
                          className="w-full h-full object-contain filter transition-all duration-300"
                        />
                      </div>
                      
                      {/* 服务信息 */}
                      <div className="flex-1 min-w-0 text-left">
                        <div className={`font-semibold text-sm truncate transition-colors duration-300 ${
                          isActive ? 'text-blue-800' : 'text-gray-800 group-hover:text-blue-700'
                        }`}>
                          {serviceName}
                        </div>
                      </div>
                      
                      {/* 选中指示器 */}
                      {isActive && !loading && (
                        <div className="p-1.5 rounded-lg bg-gradient-to-br from-blue-500/10 to-indigo-500/20 ring-1 ring-blue-200/30">
                          <ChevronRight className="h-3 w-3 text-blue-600 flex-shrink-0" />
                        </div>
                      )}
                      
                      {/* 加载指示器 */}
                      {loading && isActive && (
                        <div className="p-1.5 rounded-lg bg-gradient-to-br from-blue-500/10 to-indigo-500/20 ring-1 ring-blue-200/30">
                          <Loader2 className="h-3 w-3 animate-spin text-blue-600 flex-shrink-0" />
                        </div>
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
