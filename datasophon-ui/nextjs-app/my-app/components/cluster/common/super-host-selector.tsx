"use client"

import React, { useState, useMemo } from 'react'
import { ChevronDown, Search, Server, Cpu, HardDrive, MemoryStick, CheckCircle, Circle } from 'lucide-react'
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from '@/components/ui/badge'
import { cn } from "@/lib/utils"

/**
 * 超级主机选择器 - 比苹果设计还要好看的主机选择组件
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

interface SuperHostSelectorProps {
  hosts: HostInfo[]
  selectedHosts: string[]
  onSelectionChange: (hostnames: string[]) => void
  placeholder?: string
  multiple?: boolean
  className?: string
}

const SuperHostSelector: React.FC<SuperHostSelectorProps> = ({
  hosts,
  selectedHosts,
  onSelectionChange,
  placeholder = "选择主机",
  multiple = false,
  className
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

  // 获取资源使用率颜色
  const getResourceColor = (usage: number) => {
    if (usage < 60) return 'bg-gradient-to-r from-green-400 to-green-500'
    if (usage < 80) return 'bg-gradient-to-r from-yellow-400 to-orange-400'
    return 'bg-gradient-to-r from-red-400 to-red-500'
  }

  // 获取资源状态徽章
  const getResourceBadge = (usage: number) => {
    if (usage < 60) return { text: '优', variant: 'default' as const, color: 'text-green-600 bg-green-50' }
    if (usage < 80) return { text: '良', variant: 'secondary' as const, color: 'text-yellow-600 bg-yellow-50' }
    return { text: '高', variant: 'destructive' as const, color: 'text-red-600 bg-red-50' }
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

  // 生成显示文本
  const getDisplayText = () => {
    if (selectedHosts.length === 0) return placeholder
    if (selectedHosts.length === 1) {
      const host = hosts.find(h => h.hostname === selectedHosts[0])
      return host ? `${host.hostname} (${host.ip})` : selectedHosts[0]
    }
    return `已选择 ${selectedHosts.length} 台主机`
  }

  return (
    <div className={cn("relative", className)} style={{ zIndex: 99999 }}>
      {/* 触发器 */}
      <Button
        variant="outline"
        onClick={() => setIsOpen(!isOpen)}
        className={cn(
          "w-full max-w-80 h-11 px-4 justify-between font-normal",
          "bg-white/70 backdrop-blur-sm border-gray-200/60",
          "hover:bg-white/90 hover:border-gray-300/80 hover:shadow-lg",
          "focus:ring-2 focus:ring-blue-500/20 focus:border-blue-400",
          "transition-all duration-300 ease-out",
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

      {/* 下拉面板 */}
      {isOpen && (
        <div className="absolute z-[99999] w-full max-w-96 min-w-80 mt-2 bg-white/95 backdrop-blur-xl border border-gray-200/60 rounded-xl shadow-2xl overflow-hidden">
          {/* 搜索框 */}
          <div className="p-4 border-b border-gray-100/80 bg-gradient-to-r from-gray-50/50 to-gray-50/30">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
              <Input
                placeholder="搜索主机名、IP地址..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10 h-9 bg-white/80 border-gray-200/60 focus:border-blue-400 focus:ring-2 focus:ring-blue-500/20"
              />
            </div>
          </div>

          {/* 主机列表 */}
          <div className="max-h-[calc(100vh-200px)] overflow-y-auto">
            {filteredHosts.length === 0 ? (
              <div className="p-6 text-center text-gray-500">
                <Server className="h-8 w-8 mx-auto mb-2 text-gray-300" />
                <p>未找到匹配的主机</p>
              </div>
            ) : (
              <div className="p-2 space-y-1">
                {filteredHosts.map((host) => {
                  const isSelected = selectedHosts.includes(host.hostname)
                  const avgUsage = host.used ? (host.used.cpu + host.used.memory + host.used.disk) / 3 : 30
                  const resourceBadge = getResourceBadge(avgUsage)
                  
                  return (
                    <div
                      key={host.id}
                      onClick={() => handleHostSelect(host.hostname)}
                      className={cn(
                        "group relative p-4 rounded-lg cursor-pointer transition-all duration-200",
                        "hover:bg-gradient-to-r hover:from-blue-50/50 hover:to-indigo-50/30",
                        "hover:shadow-md hover:scale-[1.02]",
                        isSelected && "bg-gradient-to-r from-blue-50 to-indigo-50 ring-2 ring-blue-200/60"
                      )}
                    >
                      {/* 主机基本信息 */}
                      <div className="flex items-start justify-between mb-3">
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 mb-1">
                            <Server className="h-4 w-4 text-blue-600 flex-shrink-0" />
                            <h4 className="font-semibold text-gray-900 truncate">
                              {host.hostname}
                            </h4>
                            <Badge className={cn("text-xs px-2 py-0.5", resourceBadge.color)}>
                              {resourceBadge.text}
                            </Badge>
                          </div>
                          <p className="text-sm text-gray-600 font-mono">
                            {host.ip}
                          </p>
                          {host.osInfo && (
                            <p className="text-xs text-gray-500 mt-1">
                              {host.osInfo.system} • {host.cpuArchitecture || 'x64'}
                            </p>
                          )}
                        </div>
                        
                        {/* 选择状态 */}
                        <div className="flex-shrink-0 ml-3">
                          {isSelected ? (
                            <CheckCircle className="h-5 w-5 text-blue-600" />
                          ) : (
                            <Circle className="h-5 w-5 text-gray-300 group-hover:text-blue-400" />
                          )}
                        </div>
                      </div>

                      {/* 资源信息 */}
                      <div className="grid grid-cols-3 gap-3">
                        {/* CPU */}
                        <div key="cpu" className="text-center">
                          <div className="flex items-center justify-center gap-1 mb-1">
                            <Cpu className="h-3 w-3 text-blue-500" />
                            <span className="text-xs font-medium text-gray-700">CPU</span>
                          </div>
                          <div className="text-xs font-semibold text-gray-900">
                            {host.cpuCore || 0}核
                          </div>
                          {host.used && (
                            <div className="mt-1">
                              <div className="h-1.5 bg-gray-200 rounded-full overflow-hidden">
                                <div 
                                  className={cn("h-full transition-all duration-500", getResourceColor(host.used.cpu))}
                                  style={{ width: `${host.used.cpu}%` }}
                                />
                              </div>
                              <div className="text-xs text-gray-500 mt-0.5">
                                {host.used.cpu}%
                              </div>
                            </div>
                          )}
                        </div>

                        {/* Memory */}
                        <div key="memory" className="text-center">
                          <div className="flex items-center justify-center gap-1 mb-1">
                            <MemoryStick className="h-3 w-3 text-green-500" />
                            <span className="text-xs font-medium text-gray-700">内存</span>
                          </div>
                          <div className="text-xs font-semibold text-gray-900">
                            {host.memory || 0}GB
                          </div>
                          {host.used && (
                            <div className="mt-1">
                              <div className="h-1.5 bg-gray-200 rounded-full overflow-hidden">
                                <div 
                                  className={cn("h-full transition-all duration-500", getResourceColor(host.used.memory))}
                                  style={{ width: `${host.used.memory}%` }}
                                />
                              </div>
                              <div className="text-xs text-gray-500 mt-0.5">
                                {host.used.memory}%
                              </div>
                            </div>
                          )}
                        </div>

                        {/* Disk */}
                        <div key="disk" className="text-center">
                          <div className="flex items-center justify-center gap-1 mb-1">
                            <HardDrive className="h-3 w-3 text-purple-500" />
                            <span className="text-xs font-medium text-gray-700">磁盘</span>
                          </div>
                          <div className="text-xs font-semibold text-gray-900">
                            {host.disk || 0}GB
                          </div>
                          {host.used && (
                            <div className="mt-1">
                              <div className="h-1.5 bg-gray-200 rounded-full overflow-hidden">
                                <div 
                                  className={cn("h-full transition-all duration-500", getResourceColor(host.used.disk))}
                                  style={{ width: `${host.used.disk}%` }}
                                />
                              </div>
                              <div className="text-xs text-gray-500 mt-0.5">
                                {host.used.disk}%
                              </div>
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </div>

          {/* 底部操作 */}
          {multiple && selectedHosts.length > 0 && (
            <div className="p-3 border-t border-gray-100/80 bg-gradient-to-r from-gray-50/30 to-gray-50/20">
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600">
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
      )}

      {/* 背景遮罩 */}
      {isOpen && (
        <div 
          className="fixed inset-0 z-[99998]" 
          onClick={() => setIsOpen(false)}
        />
      )}
    </div>
  )
}

export default SuperHostSelector
