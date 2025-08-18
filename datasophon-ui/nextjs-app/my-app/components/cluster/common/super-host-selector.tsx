"use client"

import React, { useState, useMemo, useCallback, useEffect } from "react"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { ChevronDown, Search, Server } from "lucide-react"


// 全局下拉框管理器 - 实现互斥逻辑
class DropdownManager {
  private static instance: DropdownManager
  private openDropdowns = new Set<string>()
  private listeners = new Map<string, (isOpen: boolean) => void>()

  static getInstance() {
    if (!DropdownManager.instance) {
      DropdownManager.instance = new DropdownManager()
    }
    return DropdownManager.instance
  }

  register(id: string, callback: (isOpen: boolean) => void) {
    this.listeners.set(id, callback)
  }

  unregister(id: string) {
    this.listeners.delete(id)
    this.openDropdowns.delete(id)
  }

  open(id: string) {
    // 关闭其他所有下拉框
    this.openDropdowns.forEach(openId => {
      if (openId !== id) {
        const callback = this.listeners.get(openId)
        if (callback) {
          callback(false)
        }
      }
    })
    this.openDropdowns.clear()
    this.openDropdowns.add(id)
  }

  close(id: string) {
    this.openDropdowns.delete(id)
  }
}

export interface HostInfo {
  id: string
  hostname: string
  ip: string
  cpuCore?: number
  memory?: number // GB
  disk?: number   // GB
  cpuArchitecture?: string
  osInfo?: {
    system?: string
    version?: string
  }
  // 资源使用率
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
  serviceName?: string // 服务名称，用于显示对应的组件图标
}

const SuperHostSelector: React.FC<SuperHostSelectorProps> = ({
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
  
  // 生成唯一ID和管理器实例
  const dropdownId = useMemo(() => `dropdown-${serviceName}-${Math.random().toString(36).substr(2, 9)}`, [serviceName])
  const manager = useMemo(() => DropdownManager.getInstance(), [])

  // 注册下拉框管理
  useEffect(() => {
    manager.register(dropdownId, (shouldOpen) => {
      if (!shouldOpen) {
        setIsOpen(false)
        setSearchTerm('') // 关闭时清空搜索
      }
    })

    return () => {
      manager.unregister(dropdownId)
    }
  }, [dropdownId, manager])

  // 处理开关状态
  const handleOpenChange = useCallback((newIsOpen: boolean) => {
    if (newIsOpen) {
      manager.open(dropdownId)
    } else {
      manager.close(dropdownId)
    }
    setIsOpen(newIsOpen)
    if (!newIsOpen) {
      setSearchTerm('')
    }
  }, [dropdownId, manager])

  // 过滤主机列表
  const filteredHosts = useMemo(() => {
    // 临时调试：查看主机数据
    if (hosts.length > 0) {
      console.log('🔍 [SuperHostSelector] 主机数据检查:', {
        总数: hosts.length,
        第一个主机: hosts[0],
        主机字段: Object.keys(hosts[0] || {}),
        资源字段检查: {
          cpuCore: hosts[0]?.cpuCore,
          memory: hosts[0]?.memory, 
          disk: hosts[0]?.disk,
          ip: hosts[0]?.ip
        }
      })
    }
    
    if (!searchTerm) return hosts
    return hosts.filter(host => 
      host.hostname.toLowerCase().includes(searchTerm.toLowerCase()) ||
      host.ip.toLowerCase().includes(searchTerm.toLowerCase()) ||
      host.cpuArchitecture?.toLowerCase().includes(searchTerm.toLowerCase())
    )
  }, [hosts, searchTerm])



  // 获取显示文本
  const getDisplayText = () => {
    if (selectedHosts.length === 0) {
      return placeholder
    }
    if (selectedHosts.length === 1) {
      return selectedHosts[0]
    }
    return `已选择 ${selectedHosts.length} 台主机`
  }

  // 处理主机选择
  const handleHostSelect = useCallback((hostname: string) => {
    if (multiple) {
      const newSelection = selectedHosts.includes(hostname)
        ? selectedHosts.filter(h => h !== hostname)
        : [...selectedHosts, hostname]
      onSelectionChange(newSelection)
    } else {
      onSelectionChange([hostname])
      handleOpenChange(false)
    }
  }, [selectedHosts, multiple, onSelectionChange, handleOpenChange])

  return (
    <div className={cn("relative w-full", className)}>
      <Popover open={isOpen} onOpenChange={handleOpenChange}>
        <PopoverTrigger asChild>
          <Button
            variant="outline"
            className={cn(
              "w-full h-12 px-4 justify-between font-normal",
              "bg-gradient-to-r from-white to-gray-50/50 border-gray-200/80",
              "hover:from-gray-50 hover:to-gray-100/50 hover:border-gray-300/80 hover:shadow-lg",
              "focus:ring-2 focus:ring-blue-500/20 focus:border-blue-400",
              "transition-all duration-300 ease-out",
              "text-left group"
            )}
          >
            <div className="flex items-center gap-3 flex-1 min-w-0">
              {/* 显示文字 */}
              <span className={cn(
                "truncate transition-colors duration-200",
                selectedHosts.length === 0 ? "text-gray-500" : "text-gray-900 font-medium"
              )}>
                {getDisplayText()}
              </span>
              
              {/* 选择数量指示器 */}
              {selectedHosts.length > 0 && (
                <Badge className="bg-gradient-to-r from-blue-500 to-indigo-500 text-white text-xs px-2 py-0.5 flex-shrink-0">
                  {selectedHosts.length}
                </Badge>
              )}
            </div>
            
            <ChevronDown className={cn(
              "h-4 w-4 shrink-0 transition-all duration-300 ease-out text-gray-400 group-hover:text-gray-600",
              isOpen && "rotate-180 text-blue-500"
            )} />
          </Button>
        </PopoverTrigger>

        <PopoverContent 
          className="w-[var(--radix-popover-trigger-width)] min-w-[500px] p-0 border-gray-200/80 shadow-2xl bg-white/95 backdrop-blur-xl"
          align="start"
          side="bottom"
          sideOffset={8}
          onWheel={(e) => {
            // 阻止事件冒泡，确保滚轮事件在Popover内正常工作
            e.stopPropagation()
          }}
        >
          {/* 搜索栏 */}
          <div className="p-4 border-b border-gray-100/80 bg-gradient-to-r from-gray-50/50 to-blue-50/30 rounded-t-lg">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
              <Input
                placeholder="搜索主机名或IP..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10 h-10 border-gray-200/60 bg-white/80 focus:border-blue-400 focus:ring-blue-400/20"
              />
            </div>
          </div>

          {/* 主机列表 */}
          <div 
            className="max-h-[400px] overflow-y-auto"
            onWheel={(e) => {
              // 确保滚轮事件可以正常工作
              e.stopPropagation()
            }}
          >
            {filteredHosts.length === 0 ? (
              <div className="p-8 text-center text-gray-500">
                <Server className="mx-auto h-12 w-12 text-gray-300 mb-3" />
                <p className="text-sm font-medium">未找到匹配的主机</p>
                <p className="text-xs text-gray-400 mt-1">请尝试其他搜索条件</p>
              </div>
            ) : (
              <div className="p-2">
                {filteredHosts.map((host) => {
                  const isSelected = selectedHosts.includes(host.hostname)

                  return (
                    <div
                      key={host.id}
                      className={cn(
                        "relative group p-3 m-1 rounded-lg border transition-all duration-200 cursor-pointer",
                        "hover:shadow-md hover:border-blue-200/60",
                        isSelected 
                          ? "bg-gradient-to-r from-blue-50 to-indigo-50/50 border-blue-300/60 shadow-md ring-1 ring-blue-200/50"
                          : "bg-white/50 border-gray-200/60 hover:bg-white/80 hover:from-blue-50/30 hover:to-indigo-50/20"
                      )}
                      onClick={() => handleHostSelect(host.hostname)}
                    >
                      {/* 主机信息 - 恢复原有简洁样式 */}
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                          <div className={cn(
                            "w-4 h-4 rounded-full border-2 flex items-center justify-center transition-all duration-200",
                            isSelected 
                              ? "bg-blue-500 border-blue-500" 
                              : "border-gray-300 group-hover:border-blue-400"
                          )}>
                            {isSelected && <div className="w-1.5 h-1.5 bg-white rounded-full" />}
                          </div>
                          <div className="min-w-0 flex-1">
                            <h4 className="text-sm font-medium text-gray-900 truncate">{host.hostname}</h4>
                            {host.ip && (
                              <p className="text-xs text-gray-500 font-mono truncate">{host.ip}</p>
                            )}
                            {/* 资源信息 - 简洁显示 */}
                            {(host.cpuCore !== undefined || host.memory !== undefined || host.disk !== undefined) && (
                              <p className="text-xs text-gray-400 mt-0.5">
                                {host.cpuCore !== undefined ? `${host.cpuCore}C ` : ''}
                                {host.memory !== undefined ? `${host.memory}G ` : ''}
                                {host.disk !== undefined ? `${host.disk}GB` : ''}
                              </p>
                            )}
                          </div>
                        </div>
                      </div>

                      {/* 选中状态边框 */}
                      {isSelected && (
                        <div className="absolute inset-0 rounded-lg border-2 border-blue-400/40 pointer-events-none" />
                      )}
                    </div>
                  )
                })}
              </div>
            )}
            
            {/* 底部统计 */}
            <div className="p-4 border-t border-gray-100/80 bg-gradient-to-r from-gray-50/50 to-blue-50/30 rounded-b-lg">
              <div className="flex items-center justify-between text-xs text-gray-600">
                <span>共 {filteredHosts.length} 台主机</span>
                <span>已选择 <span className="font-semibold text-blue-600">{selectedHosts.length}</span> 台</span>
              </div>
            </div>
          </div>
        </PopoverContent>
      </Popover>
    </div>
  )
}

export default SuperHostSelector