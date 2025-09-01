"use client"

import React, { useState, useEffect, useCallback, useRef } from 'react'
import { 
  CheckCircle, Loader2, RefreshCw,
  AlertCircle, Clock, Server, Activity, AlertTriangle
} from 'lucide-react'
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { clusterApi } from "@/lib/api"
import { toast } from 'sonner'
import { createClusterHeaders } from "@/lib/cluster-id-header"
import ClusterWizardLayout from '../common/cluster-wizard-layout'
import ClusterWizardActionBar from '../common/cluster-wizard-action-bar'
import { BADGE_STYLES } from '../common/shared-styles'
import type { CheckItem } from './host-check-items'
import type { PvmStep1Data, PvmClusterInfo } from './pvm-host-config-dialog'

// PVM主机信息接口（扩展版，支持K8s风格显示）
export interface PvmHost {
  id?: string | null
  ip: string
  hostname?: string
  status: 'success' | 'failed' | 'checking' | 'waiting' | 'NotReady' | 'Ready'
  message?: string
  managementStatus?: number  // 受管状态：1=已受管, 2=未受管, 3=配置中
  createTime?: string
  checkItems?: CheckItem[]
  // 资源信息
  coreNum?: number
  totalMem?: number // GB
  totalDisk?: number // GB
  averageLoad?: string
  cpuArchitecture?: string
  checkTime?: string
  // 运行时间（格式化显示用）
  age?: string
}

// 后端返回的主机数据接口
interface BackendHostData {
  id?: string | null
  ip: string
  hostname?: string
  status: string
  managementStatus?: number
  createTime?: string
  coreNum?: number
  totalMem?: number
  totalDisk?: number
  averageLoad?: string
  cpuArchitecture?: string
  checkTime?: string
}

// PVM Step2弹窗属性接口
export interface PvmHostValidationDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  cluster: PvmClusterInfo | null
  step1Data: PvmStep1Data
  onSuccess: (data?: Record<string, unknown>) => void
  onPrevious: () => void
}

export default function PvmHostValidationDialog({
  open,
  onOpenChange,
  cluster,
  step1Data,
  onSuccess,
  onPrevious
}: PvmHostValidationDialogProps) {
  const [loading, setLoading] = useState(false)
  const [hosts, setHosts] = useState<PvmHost[]>([])
  const [checkStatus, setCheckStatus] = useState<'idle' | 'checking' | 'completed' | 'failed'>('idle')
  const [hostsLoaded, setHostsLoaded] = useState(false)
  const hostsRef = useRef<PvmHost[]>([])
  
  // 同步hosts状态到ref
  useEffect(() => {
    hostsRef.current = hosts
  }, [hosts])

  const currentStep = 2

  // 状态转换函数：将后端状态转换为前端状态
  const mapHostStatus = useCallback((backendStatus: string, managementStatus?: number): PvmHost['status'] => {
    // 根据后端状态和管理状态映射为前端状态
    if (backendStatus === 'Ready') return 'success'
    if (backendStatus === 'NotReady') return 'waiting'
    if (managementStatus === 1) return 'checking'
    return 'waiting'
  }, [])

  // 格式化主机消息
  const getHostMessage = useCallback((host: BackendHostData): string => {
    if (host.status === 'Ready') return '主机就绪'
    if (host.status === 'NotReady') return '等待检查'
    if (host.managementStatus === 1) return '检查中'
    return '等待检查'
  }, [])

  // 格式化时间差
  const formatTimeDuration = useCallback((date: Date): string => {
    const now = new Date()
    const diffMs = now.getTime() - date.getTime()
    const diffMinutes = Math.floor(diffMs / (1000 * 60))
    const diffHours = Math.floor(diffMinutes / 60)
    const diffDays = Math.floor(diffHours / 24)

    if (diffDays > 0) return `${diffDays}天`
    if (diffHours > 0) return `${diffHours}小时`
    if (diffMinutes > 0) return `${diffMinutes}分钟`
    return '刚刚'
  }, [])

  // 格式化资源信息
  const formatResourceInfo = useCallback((host: PvmHost): string => {
    const parts = []
    if (host.coreNum && host.coreNum > 0) parts.push(`${host.coreNum}核`)
    if (host.totalMem && host.totalMem > 0) parts.push(`${host.totalMem}GB内存`)
    if (host.totalDisk && host.totalDisk > 0) parts.push(`${host.totalDisk}GB磁盘`)
    return parts.length > 0 ? parts.join(' / ') : '资源未知'
  }, [])

  // 清空数据
  const clearData = () => {
    setHosts([])
    setCheckStatus('idle')
    setHostsLoaded(false)
  }

  // 获取主机列表（不执行检查）
  const loadHostList = useCallback(async () => {
    setLoading(true)
    try {
      if (!cluster?.id) {
        throw new Error('集群ID不能为空')
      }
      
      const step1Config = {
        clusterType: 'PVM',
        hosts: step1Data.hosts,
        sshUser: step1Data.sshUser,
        sshPort: step1Data.sshPort,
        sshPassword: step1Data.sshPassword
      }
      
      const headers = createClusterHeaders(cluster.id.toString())
      const response = await clusterApi.unifiedHost.discoverFromStep1(step1Config, { headers })

      if (response.data?.success && response.data?.data?.hosts) {
        const hostList: PvmHost[] = response.data.data.hosts.map((hostData: BackendHostData) => {
          const mappedStatus = mapHostStatus(hostData.status, hostData.managementStatus)
          
          // 计算运行时间
          const age = hostData.createTime ? 
            formatTimeDuration(new Date(hostData.createTime)) : 
            '未知'
          
          return {
            id: hostData.id,
            ip: hostData.ip,
            hostname: hostData.hostname && hostData.hostname !== hostData.ip ? hostData.hostname : undefined,
            status: mappedStatus,
            message: getHostMessage(hostData),
            managementStatus: hostData.managementStatus,
            createTime: hostData.createTime,
            // 资源信息
            coreNum: hostData.coreNum,
            totalMem: hostData.totalMem,
            totalDisk: hostData.totalDisk,
            averageLoad: hostData.averageLoad,
            cpuArchitecture: hostData.cpuArchitecture,
            checkTime: hostData.checkTime,
            age: age
          }
        })
        
        setHosts(hostList)
        setHostsLoaded(true)
        toast.success(`已获取 ${hostList.length} 台主机信息`)
      } else {
        toast.error('获取主机列表失败')
      }
    } catch (error: unknown) {
      console.error('获取主机列表失败:', error)
      const errorMessage = error instanceof Error ? error.message : '未知错误'
      toast.error(`获取主机列表失败: ${errorMessage}`)
    } finally {
      setLoading(false)
    }
  }, [cluster?.id, step1Data, mapHostStatus, getHostMessage, formatTimeDuration])

  // 监听弹窗关闭，清空数据
  useEffect(() => {
    if (!open) {
      clearData()
    }
  }, [open])

  // 简化的主机检查方法
  const checkSingleHost = useCallback(async (host: PvmHost): Promise<PvmHost> => {
    try {
      if (!cluster?.id) {
        throw new Error('集群ID不能为空')
      }
      
      const step1Config = {
        clusterType: 'PVM',
        hosts: host.ip,
        sshUser: step1Data.sshUser,
        sshPort: step1Data.sshPort,
        sshPassword: step1Data.sshPassword
      }
      
      const headers = createClusterHeaders(cluster.id.toString())
      const response = await clusterApi.unifiedHost.discoverFromStep1(step1Config, { headers })

      if (response.data?.success && response.data?.data?.hosts?.length > 0) {
        const hostData = response.data.data.hosts[0]
        
        return {
          ...host,
          status: mapHostStatus(hostData.status, hostData.managementStatus),
          message: getHostMessage(hostData),
          hostname: hostData.hostname && hostData.hostname !== hostData.ip ? hostData.hostname : undefined,
          managementStatus: hostData.managementStatus,
          createTime: hostData.createTime
        }
      }
      
      return {
        ...host,
        status: 'failed',
        message: '主机检查失败'
      }
      
    } catch {
      return {
        ...host,
        status: 'failed',
        message: '连接失败，请检查网络连接和SSH配置'
      }
    }
  }, [step1Data, cluster?.id, mapHostStatus, getHostMessage])

  // 监听弹窗打开，获取主机列表
  useEffect(() => {
    if (open && step1Data.hosts && !hostsLoaded) {
      // 获取主机列表，但不自动开始检查
      loadHostList()
    }
  }, [open, step1Data.hosts, hostsLoaded, loadHostList])

  // 批量检查主机
  const handleCheckHosts = useCallback(async (hostList?: PvmHost[]) => {
    const currentHosts = hostList || hostsRef.current
    if (currentHosts.length === 0) {
      toast.error('没有要检查的主机')
      return
    }

    setLoading(true)
    setCheckStatus('checking')
    
    // 重置所有主机状态为checking
    const checkingHosts = currentHosts.map(host => ({ ...host, status: 'checking' as const }))
    setHosts(checkingHosts)

    try {
      // 并发检查所有主机，但限制并发数
      const concurrency = 5 // 最多同时检查5台主机
      const results: PvmHost[] = []
      
      for (let i = 0; i < checkingHosts.length; i += concurrency) {
        const batch = checkingHosts.slice(i, i + concurrency)
        const batchResults = await Promise.all(
          batch.map(host => checkSingleHost(host))
        )
        results.push(...batchResults)
        
        // 更新进度
        setHosts([...results, ...checkingHosts.slice(results.length).map(h => ({ ...h, status: 'waiting' as const }))])
      }

      setHosts(results)
      setCheckStatus('completed')
      
      const successCount = results.filter(h => h.status === 'success').length
      toast.success(`主机检查完成！成功: ${successCount}/${results.length}`)
      
    } catch (error) {
      console.error('批量检查主机失败:', error)
      setCheckStatus('failed')
      toast.error('主机检查失败')
    } finally {
      setLoading(false)
    }
  }, [checkSingleHost])

  // 处理下一步
  const handleNext = async () => {
    if (checkStatus !== 'completed') {
      toast.error('请先完成主机检查')
      return
    }

    const successHosts = hosts.filter(host => host.status === 'success')
    if (successHosts.length === 0) {
      toast.error('没有可用的主机，无法继续')
      return
    }

    setLoading(true)
    try {
      // 传递主机信息到下一步
      onSuccess({
        validHosts: successHosts,
        totalHosts: hosts.length,
        step2Data: {
          checkStatus,
          hostValidationResults: hosts
        }
      })
      toast.success('主机验证完成，进入下一步')
    } catch (error) {
      console.error('进入下一步失败:', error)
      toast.error('进入下一步失败')
    } finally {
      setLoading(false)
    }
  }

  // 获取主机状态Badge样式（框架化）
  const getHostStatusBadgeStyle = (status: string) => {
    switch (status) {
      case 'success':
      case 'Ready':
        return BADGE_STYLES.status.ready
      case 'failed':
        return BADGE_STYLES.status.notReady
      case 'checking':
        return BADGE_STYLES.management.configuring // 使用配置中状态的黄色
      case 'NotReady':
        return BADGE_STYLES.status.unknown
      default:
        return BADGE_STYLES.status.unknown
    }
  }

  // 获取主机状态图标
  const getHostStatusIcon = (status: string) => {
    switch (status) {
      case 'success':
      case 'Ready':
        return <CheckCircle className="w-4 h-4 text-green-600" />
      case 'failed':
        return <AlertCircle className="w-4 h-4 text-red-600" />
      case 'checking':
        return <Loader2 className="w-4 h-4 text-blue-600 animate-spin" />
      case 'NotReady':
        return <Activity className="w-4 h-4 text-yellow-600" />
      default:
        return <Clock className="w-4 h-4 text-gray-600" />
    }
  }

  // 创建统一的ActionBar
  const actionBar = (
    <ClusterWizardActionBar
      statusInfo={{
        text: `主机验证 (${hosts.filter(h => h.status === 'success').length}/${hosts.length})`,
        value: hosts.filter(h => h.status === 'success').length,
        total: hosts.length,
        pulse: checkStatus === 'checking'
      }}
      buttons={[
        ...(onPrevious ? [{
          text: "上一步",
          onClick: onPrevious,
          variant: 'secondary' as const,
          disabled: loading
        }] : []),
        // 根据不同状态显示不同的按钮
        ...(checkStatus === 'idle' && hostsLoaded ? [{
          text: "开始检查",
          onClick: () => handleCheckHosts(),
          disabled: loading || hosts.length === 0,
          loading: loading,
          loadingText: "检查中..."
        }] : []),
        ...(checkStatus === 'checking' ? [{
          text: "检查中...",
          onClick: () => {},
          disabled: true,
          loading: true,
          loadingText: "检查中..."
        }] : []),
        ...(checkStatus === 'completed' ? [{
          text: "下一步",
          onClick: handleNext,
          disabled: loading || hosts.filter(h => h.status === 'success').length === 0,
          loading: loading,
          loadingText: "处理中..."
        }] : [])
      ]}
    />
  )

  return (
    <ClusterWizardLayout
      open={open}
      onClose={() => onOpenChange(false)}
      clusterName={cluster?.clusterName || ''}
      clusterType="PVM"
      stepTitle="主机验证"
      stepDescription="PVM主机验证 - 验证所有主机的SSH连接和系统状态"
      currentStep={currentStep}
      dialogTitle={`PVM主机验证 - ${cluster?.clusterName}`}
      actionBar={actionBar}
    >
      {/* 当前步骤内容 */}
      <div className="flex-1 overflow-y-auto bg-gradient-to-b from-white to-slate-50/50 min-h-0 [&::-webkit-scrollbar]:w-2 [&::-webkit-scrollbar-track]:bg-transparent [&::-webkit-scrollbar-thumb]:bg-indigo-200/60 [&::-webkit-scrollbar-thumb]:rounded-full [&::-webkit-scrollbar-thumb:hover]:bg-indigo-300/80 [&::-webkit-scrollbar]:transition-all">
        <div className="p-6 sm:p-8 lg:p-10">
          <div className="space-y-6">
            {/* 步骤内容 */}
            <div className="space-y-6">
              {/* 主机检查状态 */}
              <Card className="border-0 shadow-lg bg-white/80 backdrop-blur-sm rounded-3xl">
                <CardHeader className="pb-4">
                  <CardTitle className="text-lg flex items-center justify-between">
                    <div className="flex items-center">
                      <Server className="w-5 h-5 mr-2 text-indigo-600" />
                      主机检查 ({hosts.length} 台主机)
                    </div>
                    <Button
                      onClick={() => {
                        if (!hostsLoaded) {
                          loadHostList()
                        } else {
                          handleCheckHosts()
                        }
                      }}
                      disabled={loading}
                      variant="outline"
                      size="sm"
                      className="h-8"
                    >
                      {loading ? (
                        <Loader2 className="w-4 h-4 mr-1 animate-spin" />
                      ) : (
                        <RefreshCw className="w-4 h-4 mr-1" />
                      )}
                      {loading 
                        ? (hostsLoaded ? '检查中' : '获取中') 
                        : (hostsLoaded 
                          ? (checkStatus === 'idle' ? '开始检查' : '重新检查')
                          : '获取主机列表'
                        )
                      }
                    </Button>
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  {!hostsLoaded && hosts.length === 0 && (
                        <div className="text-center py-8">
                          <Server className="mx-auto w-12 h-12 text-gray-400 mb-4" />
                          <p className="text-gray-600">
                            {loading ? '正在获取主机列表...' : '等待获取主机列表'}
                          </p>
                        </div>
                      )}
                      
                      {hostsLoaded && hosts.length === 0 && (
                        <div className="text-center py-8">
                          <Server className="mx-auto w-12 h-12 text-gray-400 mb-4" />
                          <p className="text-gray-600">没有可用的主机</p>
                          <Button
                            onClick={loadHostList}
                            variant="outline"
                            size="sm"
                            className="mt-2"
                          >
                            重新获取
                          </Button>
                        </div>
                      )}

                      {hosts.length > 0 && (
                        <div className="flex-1 flex flex-col bg-white min-h-0">
                          {/* Apple风格的现代化表格设计 */}
                          {/* 表格头部 - 优化列宽分配 */}
                          <div className="border-b border-gray-100 bg-gradient-to-r from-gray-50/80 to-white/80 backdrop-blur-sm flex-shrink-0">
                            <div className="grid grid-cols-12 gap-2 px-3 py-2 text-xs font-semibold text-gray-700">
                              <div className="col-span-2 flex items-center space-x-1">
                                <span className="w-1.5 h-1.5 rounded-full bg-blue-500"></span>
                                <span>主机信息</span>
                              </div>
                              <div className="col-span-2 flex items-center space-x-1">
                                <span className="w-1.5 h-1.5 rounded-full bg-amber-500"></span>
                                <span>资源</span>
                              </div>
                              <div className="col-span-1 flex items-center space-x-1">
                                <span className="w-1.5 h-1.5 rounded-full bg-green-500"></span>
                                <span>状态</span>
                              </div>
                              <div className="col-span-2 flex items-center space-x-1">
                                <span className="w-1.5 h-1.5 rounded-full bg-purple-500"></span>
                                <span>架构/负载</span>
                              </div>
                              <div className="col-span-2 flex items-center space-x-1">
                                <span className="w-1.5 h-1.5 rounded-full bg-orange-500"></span>
                                <span>时间</span>
                              </div>
                              <div className="col-span-3 flex items-center justify-center space-x-1">
                                <span className="w-1.5 h-1.5 rounded-full bg-rose-500"></span>
                                <span>受管状态</span>
                              </div>
                            </div>
                          </div>
                          
                          {/* 主机列表容器 */}
                          <div className="flex-1 overflow-y-auto min-h-0 max-h-[calc(100vh-380px)] 
                                         scrollbar-thin scrollbar-track-transparent 
                                         scrollbar-thumb-gray-200 hover:scrollbar-thumb-gray-300
                                         [&::-webkit-scrollbar]:w-1.5
                                         [&::-webkit-scrollbar-track]:bg-transparent
                                         [&::-webkit-scrollbar-thumb]:bg-gray-200
                                         [&::-webkit-scrollbar-thumb]:rounded-full
                                         [&::-webkit-scrollbar-thumb:hover]:bg-gray-300"
                               style={{ 
                                 scrollbarWidth: 'thin',
                                 scrollbarColor: 'rgba(156, 163, 175, 0.4) transparent'
                               }}>

                            <div className="p-2 space-y-1">
                              {hosts.map((host) => {
                                const managedStatus = host.managementStatus === 1 ? '已受管' : '未受管'
                                
                                return (
                                  <div 
                                    key={host.ip}
                                    className="group relative transform transition-all duration-200 ease-out hover:scale-[1.01]"
                                  >
                                    {/* 主卡片容器 - 紧凑设计 */}
                                    <div 
                                      className="relative rounded-xl border transition-all duration-300 overflow-hidden border-gray-200 bg-white hover:border-gray-300 hover:shadow-md shadow-sm"
                                    >
                                      <div className="grid grid-cols-12 gap-2 px-3 py-2">
                                        {/* 主机信息 */}
                                        <div className="col-span-2 flex items-center space-x-2">
                                          <div className="flex-shrink-0">
                                            {getHostStatusIcon(host.status)}
                                          </div>
                                          <div className="min-w-0 flex-1">
                                            <div className="text-sm font-medium text-gray-900 truncate">
                                              {host.ip}
                                            </div>
                                            {host.hostname && host.hostname !== host.ip && (
                                              <div className="text-xs text-gray-500 truncate">
                                                {host.hostname}
                                              </div>
                                            )}
                                          </div>
                                        </div>
                                        
                                        {/* 资源信息 */}
                                        <div className="col-span-2 flex items-center">
                                          <div className="text-xs text-gray-600">
                                            {formatResourceInfo(host)}
                                          </div>
                                        </div>
                                        
                                        {/* 状态 */}
                                        <div className="col-span-1 flex items-center">
                                          <Badge className={`${BADGE_STYLES.base} ${getHostStatusBadgeStyle(host.status)} text-xs`}>
                                            {host.status === 'success' || host.status === 'Ready' ? '就绪' : 
                                             host.status === 'failed' ? '失败' : 
                                             host.status === 'checking' ? '检查中' : '等待'}
                                          </Badge>
                                        </div>
                                        
                                        {/* 架构/负载 */}
                                        <div className="col-span-2 flex items-center">
                                          <div className="text-xs text-gray-600">
                                            <div>{host.cpuArchitecture || '未知'}</div>
                                            {host.averageLoad && (
                                              <div className="text-xs text-gray-500">负载: {host.averageLoad}</div>
                                            )}
                                          </div>
                                        </div>
                                        
                                        {/* 时间信息 */}
                                        <div className="col-span-2 flex items-center">
                                          <div className="text-xs text-gray-500">
                                            <div>{host.age || '未知'}</div>
                                            {host.checkTime && (
                                              <div>检查: {new Date(host.checkTime).toLocaleTimeString()}</div>
                                            )}
                                          </div>
                                        </div>
                                        
                                        {/* 受管状态 */}
                                        <div className="col-span-3 flex items-center justify-center">
                                          <span className={`text-xs px-2 py-1 rounded-full font-medium ${
                                            host.managementStatus === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700'
                                          }`}>
                                            {managedStatus}
                                          </span>
                                          
                                          {/* 错误信息图标 */}
                                          {host.status === 'failed' && (
                                            <AlertTriangle className="w-4 h-4 text-red-500 ml-2" />
                                          )}
                                        </div>
                                      </div>
                                      
                                      {/* 错误信息展开区域 */}
                                      {host.status === 'failed' && host.message && (
                                        <div className="px-3 pb-2">
                                          <div className="text-xs text-red-600 bg-red-50 px-2 py-1 rounded">
                                            {host.message}
                                          </div>
                                        </div>
                                      )}
                                    </div>
                                  </div>
                                )
                              })}
                            </div>
                          </div>
                        </div>
                      )}
                </CardContent>
              </Card>

              {/* 统计信息 */}
              {checkStatus === 'completed' && hosts.length > 0 && (
                <Card className="border-0 shadow-lg bg-white/80 backdrop-blur-sm rounded-3xl">
                  <CardHeader className="pb-4">
                    <CardTitle className="text-lg flex items-center">
                      <CheckCircle className="w-5 h-5 mr-2 text-green-600" />
                      检查统计
                    </CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-center">
                      <div className="p-4 bg-green-50 rounded-xl">
                        <div className="text-2xl font-bold text-green-600">
                          {hosts.filter(h => h.status === 'success').length}
                        </div>
                        <div className="text-sm text-green-700">成功</div>
                      </div>
                      <div className="p-4 bg-red-50 rounded-xl">
                        <div className="text-2xl font-bold text-red-600">
                          {hosts.filter(h => h.status === 'failed').length}
                        </div>
                        <div className="text-sm text-red-700">失败</div>
                      </div>
                      <div className="p-4 bg-blue-50 rounded-xl">
                        <div className="text-2xl font-bold text-blue-600">
                          {hosts.length}
                        </div>
                        <div className="text-sm text-blue-700">总计</div>
                      </div>
                      <div className="p-4 bg-purple-50 rounded-xl">
                        <div className="text-2xl font-bold text-purple-600">
                          {Math.round((hosts.filter(h => h.status === 'success').length / hosts.length) * 100)}%
                        </div>
                        <div className="text-sm text-purple-700">成功率</div>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              )}
            </div>
          </div>
        </div>
      </div>
    </ClusterWizardLayout>
  )
}