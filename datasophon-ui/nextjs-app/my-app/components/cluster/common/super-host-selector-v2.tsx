"use client"

import React, { useState, useMemo } from 'react'
import { ChevronDown, Search, CheckCircle, Circle } from 'lucide-react'
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from '@/components/ui/badge'
import { cn } from "@/lib/utils"
import Image from 'next/image'

/**
 * 超级主机选择器 V2 - 全新设计的主机选择组件
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 * @date 2024
 */

export interface HostInfo {
  id: number
  hostname: string
  ip: string
  cpuCore?: number
  memory?: number
  disk?: number
  cpuArchitecture?: string
  osInfo?: {
    system: string
    version: string
  }
  used?: {
    cpu: number    // 使用率 0-100
    memory: number // 使用率 0-100
    disk: number   // 使用率 0-100
  }
}

interface SuperHostSelectorV2Props {
  hosts: HostInfo[]
  selectedHosts: string[]
  onSelectionChange: (hostnames: string[]) => void
  placeholder?: string
  multiple?: boolean
  className?: string
  serviceName?: string // 服务名称，用于显示对应的组件图标
}

const SuperHostSelectorV2: React.FC<SuperHostSelectorV2Props> = ({
  hosts,
  selectedHosts,
  onSelectionChange,
  placeholder = "选择主机",
  multiple = false,
  className,
  serviceName = ""
}) => {
  const [isOpen, setIsOpen] = useState(false)
  const [searchTerm, setSearchTerm] = useState('')

  // 过滤主机列表
  const filteredHosts = useMemo(() => {
    if (!searchTerm) return hosts
    
    return hosts.filter(host => 
      host.hostname.toLowerCase().includes(searchTerm.toLowerCase()) ||
      host.ip.toLowerCase().includes(searchTerm.toLowerCase()) ||
      host.cpuArchitecture?.toLowerCase().includes(searchTerm.toLowerCase())
    )
  }, [hosts, searchTerm])

  // 获取大数据组件图标
  const getComponentIcon = (serviceName: string = '') => {
    const service = serviceName.toLowerCase()
    
    // 大数据组件图标映射
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
      'logstash': '/icons/logstash.svg',
      'prometheus': '/icons/prometheus.svg',
      'grafana': '/icons/grafana.svg',
      'alertmanager': '/icons/alertmanager.svg',
      'redis': '/icons/redis.svg',
      'doris': '/icons/doris.svg',
      'clickhouse': '/icons/clickhouse.svg',
      'trino': '/icons/trino.svg',
      'presto': '/icons/presto.svg',
      'alluxio': '/icons/alluxio.svg',
      'juicefs': '/icons/juicefs.svg',
      'minio': '/icons/minio.svg',
      'ranger': '/icons/ranger.svg',
      'kerberos': '/icons/kerberos.svg',
      'flume': '/icons/flume.svg',
      'kyuubi': '/icons/kyuubi.svg',
      'hue': '/icons/hue.svg',
      'zeppelin': '/icons/zeppelin.svg',
      'streampark': '/icons/streampark.svg',
      'seatunnel': '/icons/seatunnel.svg',
      'starrocks': '/icons/starrocks.svg',
      'postgresql': '/icons/postgresql.svg',
      'neo4j': '/icons/neo4j.svg',
      'nebulagraph': '/icons/nebulagraph.svg',
      'iceberg': '/icons/iceberg.svg',
      'hudi': '/icons/hudi.svg',
      'paimon': '/icons/paimon.svg',
      'tez': '/icons/tez.svg',
      'openldap': '/icons/openldap.svg'
    }
    
    // 从服务名称中提取主要组件名
    for (const [component, icon] of Object.entries(iconMap)) {
      if (service.includes(component)) {
        return icon
      }
    }
    
    // 默认图标
    return '/icons/service-default.svg'
  }

  // 获取资源使用率颜色和状态
  const getResourceStatus = (usage: number) => {
    if (usage < 50) return { 
      color: 'text-green-600', 
      bgColor: 'bg-green-100', 
      barColor: 'bg-green-500',
      status: '优秀' 
    }
    if (usage < 75) return { 
      color: 'text-yellow-600', 
      bgColor: 'bg-yellow-100', 
      barColor: 'bg-yellow-500',
      status: '良好' 
    }
    return { 
      color: 'text-red-600', 
      bgColor: 'bg-red-100', 
      barColor: 'bg-red-500',
      status: '繁忙' 
    }
  }

  // 处理主机选择
  const handleHostSelect = (hostname: string) => {
    if (multiple) {
      if (selectedHosts.includes(hostname)) {
        onSelectionChange(selectedHosts.filter(h => h !== hostname))
      } else {
        onSelectionChange([...selectedHosts, hostname])
      }
    } else {
      onSelectionChange([hostname])
      setIsOpen(false)
    }
  }

  // 生成显示文本（不显示IP）
  const getDisplayText = () => {
    if (selectedHosts.length === 0) return placeholder
    if (selectedHosts.length === 1) {
      return selectedHosts[0] // 只显示主机名
    }
    return `已选择 ${selectedHosts.length} 台主机`
  }

  return (
    <div className={cn("relative w-full max-w-md", className)}>
      {/* 触发器 */}
      <Button
        variant="outline"
        onClick={() => setIsOpen(!isOpen)}
        className={cn(
          "w-full h-11 px-4 justify-between font-normal",
          "bg-white/80 backdrop-blur-sm border-gray-200/60",
          "hover:bg-white/95 hover:border-gray-300/80 hover:shadow-md",
          "focus:ring-2 focus:ring-blue-500/20 focus:border-blue-400",
          "transition-all duration-200",
          "text-left"
        )}
      >
        <span className={cn(
          "truncate",
          selectedHosts.length === 0 ? "text-gray-500" : "text-gray-900"
        )}>
          {getDisplayText()}
        </span>
        <ChevronDown className={cn(
          "h-4 w-4 shrink-0 transition-transform duration-200",
          isOpen && "rotate-180"
        )} />
      </Button>

      {/* 下拉面板 - 使用Portal避免容器限制 */}
      {isOpen && (
        <>
          <div 
            className="fixed inset-0 z-[999998]" 
            onClick={() => setIsOpen(false)}
          />
          <div className="absolute z-[999999] w-full mt-1 bg-white border border-gray-200 rounded-xl shadow-xl overflow-hidden min-w-[400px]">
            {/* 搜索框 */}
            <div className="p-3 border-b border-gray-100 bg-gray-50/50">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
                <Input
                  placeholder="搜索主机名或IP..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-10 h-9 bg-white border-gray-200 focus:border-blue-400 focus:ring-1 focus:ring-blue-400"
                />
              </div>
            </div>

            {/* 主机列表 */}
            <div className="max-h-[60vh] overflow-y-auto">
              {filteredHosts.length === 0 ? (
                <div className="p-6 text-center text-gray-500">
                  <div className="w-12 h-12 mx-auto mb-3 opacity-50">
                    <Image
                      src="/icons/host.svg"
                      alt="No hosts"
                      width={48}
                      height={48}
                    />
                  </div>
                  <p className="font-medium">未找到匹配的主机</p>
                  <p className="text-sm text-gray-400">请尝试其他搜索条件</p>
                </div>
              ) : (
                <div className="p-2">
                  {filteredHosts.map((host) => {
                    const isSelected = selectedHosts.includes(host.hostname)
                    const avgUsage = host.used ? (host.used.cpu + host.used.memory + host.used.disk) / 3 : 25
                    const resourceStatus = getResourceStatus(avgUsage)
                    
                    return (
                      <div
                        key={host.id}
                        onClick={() => handleHostSelect(host.hostname)}
                        className={cn(
                          "group relative p-3 rounded-lg cursor-pointer transition-all duration-200",
                          "hover:bg-blue-50/70 hover:shadow-sm border border-transparent",
                          isSelected && "bg-blue-50 border-blue-200 shadow-sm"
                        )}
                      >
                        <div className="flex items-center justify-between">
                          {/* 左侧：主机信息 */}
                          <div className="flex items-center gap-3 flex-1 min-w-0">
                            {/* 选择状态指示 */}
                            <div className="flex-shrink-0 w-8 h-8 flex items-center justify-center">
                              {isSelected && (
                                <div className="w-4 h-4 bg-blue-500 rounded-full flex items-center justify-center">
                                  <div className="w-2 h-2 bg-white rounded-full" />
                                </div>
                              )}
                            </div>
                            
                            {/* 主机名和系统信息 */}
                            <div className="flex-1 min-w-0">
                              <div className="flex items-center gap-2">
                                <h4 className="font-semibold text-gray-900 truncate">
                                  {host.hostname}
                                </h4>
                                <Badge 
                                  className={cn(
                                    "text-xs px-2 py-0.5",
                                    resourceStatus.color,
                                    resourceStatus.bgColor
                                  )}
                                >
                                  {resourceStatus.status}
                                </Badge>
                              </div>
                              <div className="flex items-center gap-2 text-xs text-gray-500">
                                <span className="font-mono">{host.ip}</span>
                                <span>•</span>
                                <span>{host.cpuArchitecture || 'x64'}</span>
                                {host.osInfo && (
                                  <>
                                    <span>•</span>
                                    <span>{host.osInfo.system}</span>
                                  </>
                                )}
                              </div>
                            </div>
                          </div>

                          {/* 右侧：资源信息（紧凑横向布局）*/}
                          <div className="flex items-center gap-4 ml-4 flex-shrink-0">
                            {/* CPU */}
                            <div className="text-center min-w-[60px]">
                              <div className="text-xs font-medium text-gray-700 mb-1">
                                CPU {host.cpuCore || 0}核
                              </div>
                              {host.used && (
                                <div className="flex items-center gap-1">
                                  <div className="w-12 h-1.5 bg-gray-200 rounded-full overflow-hidden">
                                    <div 
                                      className={cn("h-full transition-all", resourceStatus.barColor)}
                                      style={{ width: `${host.used.cpu}%` }}
                                    />
                                  </div>
                                  <span className="text-xs text-gray-500 w-8 text-right">
                                    {host.used.cpu}%
                                  </span>
                                </div>
                              )}
                            </div>

                            {/* 内存 */}
                            <div className="text-center min-w-[60px]">
                              <div className="text-xs font-medium text-gray-700 mb-1">
                                内存 {host.memory || 0}GB
                              </div>
                              {host.used && (
                                <div className="flex items-center gap-1">
                                  <div className="w-12 h-1.5 bg-gray-200 rounded-full overflow-hidden">
                                    <div 
                                      className={cn("h-full transition-all", resourceStatus.barColor)}
                                      style={{ width: `${host.used.memory}%` }}
                                    />
                                  </div>
                                  <span className="text-xs text-gray-500 w-8 text-right">
                                    {host.used.memory}%
                                  </span>
                                </div>
                              )}
                            </div>

                            {/* 磁盘 */}
                            <div className="text-center min-w-[60px]">
                              <div className="text-xs font-medium text-gray-700 mb-1">
                                磁盘 {host.disk || 0}GB
                              </div>
                              {host.used && (
                                <div className="flex items-center gap-1">
                                  <div className="w-12 h-1.5 bg-gray-200 rounded-full overflow-hidden">
                                    <div 
                                      className={cn("h-full transition-all", resourceStatus.barColor)}
                                      style={{ width: `${host.used.disk}%` }}
                                    />
                                  </div>
                                  <span className="text-xs text-gray-500 w-8 text-right">
                                    {host.used.disk}%
                                  </span>
                                </div>
                              )}
                            </div>

                            {/* 选择状态 */}
                            <div className="flex-shrink-0 ml-2">
                              {isSelected ? (
                                <CheckCircle className="h-5 w-5 text-blue-600" />
                              ) : (
                                <Circle className="h-5 w-5 text-gray-300 group-hover:text-blue-400" />
                              )}
                            </div>
                          </div>
                        </div>
                      </div>
                    )
                  })}
                </div>
              )}
            </div>

            {/* 底部操作（仅多选模式） */}
            {multiple && selectedHosts.length > 0 && (
              <div className="p-3 border-t border-gray-100 bg-gray-50/30">
                <div className="flex items-center justify-between">
                  <span className="text-sm text-gray-600 font-medium">
                    已选择 {selectedHosts.length} 台主机
                  </span>
                  <Button
                    size="sm"
                    onClick={() => setIsOpen(false)}
                    className="h-8 px-4 bg-blue-600 hover:bg-blue-700 text-white"
                  >
                    确定
                  </Button>
                </div>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  )
}

export default SuperHostSelectorV2
