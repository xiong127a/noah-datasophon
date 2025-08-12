"use client"

import React, { useState, useEffect, useCallback } from 'react'
import { 
  CheckCircle, Loader2, RefreshCw,
  AlertCircle, Clock, Server, Activity, Shield, AlertTriangle
} from 'lucide-react'
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { clusterApi } from "@/lib/api"
import { toast } from 'sonner'
import ClusterWizardLayout from '../common/cluster-wizard-layout'
import ClusterWizardActionBar from '../common/cluster-wizard-action-bar'
import { getStepsByType, StepsType } from '@/lib/cluster-wizard-steps'
import { BADGE_STYLES } from '../common/shared-styles'
import type { PvmStep1Data, PvmClusterInfo } from './pvm-host-config-dialog'

// PVM主机信息接口
export interface PvmHost {
  ip: string
  hostname?: string
  status: 'success' | 'failed' | 'checking' | 'pending'
  message?: string
  osInfo?: {
    system: string
    version: string
    arch: string
  }
  resources?: {
    cpu: string
    memory: string
    disk: string
  }
  services?: string[]
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

  const steps = getStepsByType('pvm' as StepsType)
  const currentStep = 2

  // 解析主机列表
  const parseHostRange = (hostInput: string): string[] => {
    const hosts: string[] = []
    const lines = hostInput.split('\n').filter(line => line.trim())
    
    for (const line of lines) {
      const trimmed = line.trim()
      if (trimmed.includes('[') && trimmed.includes(']')) {
        // 解析范围格式
        const match = trimmed.match(/^(.+)\[(\d+)-(\d+)\](.*)$/)
        if (match) {
          const [, prefix, start, end, suffix] = match
          const startNum = parseInt(start)
          const endNum = parseInt(end)
          for (let i = startNum; i <= endNum; i++) {
            hosts.push(`${prefix}${i}${suffix}`)
          }
        } else {
          hosts.push(trimmed)
        }
      } else if (trimmed.includes(',')) {
        // 逗号分隔
        hosts.push(...trimmed.split(',').map(h => h.trim()).filter(h => h))
      } else if (trimmed) {
        // 单个主机
        hosts.push(trimmed)
      }
    }
    return hosts
  }

  // 初始化主机列表
  const initializeHosts = useCallback(() => {
    const hostIPs = parseHostRange(step1Data.hosts)
    const hostList: PvmHost[] = hostIPs.map(ip => ({
      ip: ip.trim(),
      status: 'pending'
    }))
    setHosts(hostList)
    return hostList
  }, [step1Data.hosts])

  // 清空数据
  const clearData = () => {
    setHosts([])
    setCheckStatus('idle')
  }

  // 监听弹窗关闭，清空数据
  useEffect(() => {
    if (!open) {
      clearData()
    }
  }, [open])

  // 检查单个主机
  const checkSingleHost = useCallback(async (host: PvmHost): Promise<PvmHost> => {
    try {
      // 使用现有的主机分析API
      const response = await clusterApi.host.analysisHostList({
        ips: host.ip,
        sshUser: step1Data.sshUser,
        sshPort: step1Data.sshPort,
        sshPassword: step1Data.sshPassword,
        page: 1,
        pageSize: 1
      })

      if (response.data?.success && response.data?.data?.data?.length > 0) {
        const hostData = response.data.data.data[0]
        return {
          ...host,
          status: 'success',
          hostname: hostData.hostname || host.ip,
          osInfo: {
            system: hostData.osType || 'Linux',
            version: hostData.osVersion || 'Unknown',
            arch: hostData.cpuArchitecture || 'x86_64'
          },
          resources: {
            cpu: hostData.cpuCores ? `${hostData.cpuCores} 核` : 'Unknown',
            memory: hostData.totalMem ? `${Math.round(hostData.totalMem / 1024 / 1024 / 1024)}GB` : 'Unknown',
            disk: hostData.totalDisk ? `${Math.round(hostData.totalDisk / 1024 / 1024 / 1024)}GB` : 'Unknown'
          },
          checkTime: new Date().toLocaleString(),
          message: '连接成功'
        }
      } else {
        return {
          ...host,
          status: 'failed',
          message: response.data?.message || '连接失败',
          checkTime: new Date().toLocaleString()
        }
      }
    } catch {
      return {
        ...host,
        status: 'failed',
        message: 'SSH连接异常',
        checkTime: new Date().toLocaleString()
      }
    }
  }, [step1Data.sshUser, step1Data.sshPort, step1Data.sshPassword])

  // 检查所有主机
  const handleCheckHosts = useCallback(async (hostList?: PvmHost[]) => {
    const currentHosts = hostList || hosts
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
        const batchPromises = batch.map(host => checkSingleHost(host))
        const batchResults = await Promise.all(batchPromises)
        results.push(...batchResults)
        
        // 实时更新结果
        setHosts(prev => {
          const updated = [...prev]
          batchResults.forEach((result, index) => {
            const globalIndex = i + index
            updated[globalIndex] = result
          })
          return updated
        })
      }

      setHosts(results)
      setCheckStatus('completed')
      
      const successCount = results.filter(host => host.status === 'success').length
      const failedCount = results.filter(host => host.status === 'failed').length
      
      if (successCount > 0) {
        toast.success(`主机检查完成：${successCount} 台成功，${failedCount} 台失败`)
      } else {
        toast.error('所有主机检查都失败了，请检查SSH连接信息')
        setCheckStatus('failed')
      }
    } catch {
      toast.error('主机检查失败，请重试')
      setCheckStatus('failed')
    } finally {
      setLoading(false)
    }
  }, [hosts, checkSingleHost])

  // 监听弹窗打开，初始化主机列表
  useEffect(() => {
    if (open && step1Data.hosts) {
      const hostList = initializeHosts()
      if (hostList.length > 0) {
        // 自动开始检查
        setTimeout(() => {
          handleCheckHosts(hostList)
        }, 500)
      }
    }
  }, [open, step1Data.hosts, initializeHosts, handleCheckHosts])



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
        hosts: hosts,
        successHosts: successHosts,
        sshConfig: {
          sshUser: step1Data.sshUser,
          sshPort: step1Data.sshPort,
          sshPassword: step1Data.sshPassword
        }
      })
    } catch {
      toast.error('保存主机信息失败，请重试')
    } finally {
      setLoading(false)
    }
  }

  // 获取主机状态Badge样式（框架化）
  const getHostStatusBadgeStyle = (status: string) => {
    switch (status) {
      case 'success':
        return BADGE_STYLES.status.ready
      case 'failed':
        return BADGE_STYLES.status.notReady
      case 'checking':
        return BADGE_STYLES.management.configuring // 使用配置中状态的黄色
      default:
        return BADGE_STYLES.status.unknown
    }
  }

  // 获取主机状态图标
  const getHostStatusIcon = (status: string) => {
    switch (status) {
      case 'success':
        return <CheckCircle className="w-4 h-4 text-green-600" />
      case 'failed':
        return <AlertCircle className="w-4 h-4 text-red-600" />
      case 'checking':
        return <Loader2 className="w-4 h-4 text-blue-600 animate-spin" />
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
        {
          text: checkStatus === 'completed' ? "下一步" : "检查主机",
          onClick: checkStatus === 'completed' ? handleNext : handleCheckHosts,
          disabled: loading || (checkStatus === 'completed' && hosts.filter(h => h.status === 'success').length === 0),
          loading: loading,
          loadingText: checkStatus === 'completed' ? "处理中..." : "检查中..."
        }
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
      stepDescription="验证主机连通性和系统资源"
      currentStep={currentStep}
      dialogTitle={`PVM主机验证 - ${cluster?.clusterName}`}
      actionBar={actionBar}
    >
      {/* 当前步骤内容 */}
      <div className="flex-1 overflow-y-auto bg-gradient-to-b from-white to-slate-50/50 min-h-0 [&::-webkit-scrollbar]:w-2 [&::-webkit-scrollbar-track]:bg-transparent [&::-webkit-scrollbar-thumb]:bg-indigo-200/60 [&::-webkit-scrollbar-thumb]:rounded-full [&::-webkit-scrollbar-thumb:hover]:bg-indigo-300/80 [&::-webkit-scrollbar]:transition-all">
        <div className="p-6 sm:p-8 lg:p-10">
          <div className="space-y-8">
              {/* 装饰性光效 */}
              <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/60 to-transparent"></div>
              {/* 分割线光效 */}
              <div className="absolute bottom-0 left-6 right-6 h-px bg-gradient-to-r from-transparent via-indigo-200/80 to-transparent"></div>
              <div className="flex items-center justify-between relative z-10">
                <div>
                  <h2 className="text-lg sm:text-xl lg:text-2xl font-bold text-gray-900">
                    PVM主机验证
                  </h2>
                  <p className="text-gray-600 mt-1">
                    验证所有主机的SSH连接和系统状态
                  </p>
                </div>
                <Badge variant="outline" className="text-indigo-600 border-indigo-200 bg-white/80 backdrop-blur-sm">
                  步骤 {currentStep}/{steps.length}
                </Badge>
              </div>
            </div>

            {/* 步骤内容 */}
            <div className="flex-1 overflow-y-auto bg-gradient-to-b from-white to-slate-50/50 min-h-0 [&::-webkit-scrollbar]:w-2 [&::-webkit-scrollbar-track]:bg-transparent [&::-webkit-scrollbar-thumb]:bg-indigo-200/60 [&::-webkit-scrollbar-thumb]:rounded-full [&::-webkit-scrollbar-thumb:hover]:bg-indigo-300/80 [&::-webkit-scrollbar]:transition-all">
              <div className="p-6 sm:p-8 lg:p-10">
                <div className="space-y-6">
                  {/* SSH配置信息概览 */}
                  <Card className="border-0 shadow-lg bg-white/80 backdrop-blur-sm rounded-3xl">
                    <CardHeader className="pb-4">
                      <CardTitle className="text-lg flex items-center">
                        <Shield className="w-5 h-5 mr-2 text-purple-600" />
                        SSH连接配置
                      </CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                        <div className="text-center">
                          <div className="text-lg font-bold text-gray-900">{step1Data.sshUser}</div>
                          <div className="text-sm text-gray-600">用户名</div>
                        </div>
                        <div className="text-center">
                          <div className="text-lg font-bold text-blue-600">{step1Data.sshPort}</div>
                          <div className="text-sm text-gray-600">端口</div>
                        </div>
                        <div className="text-center">
                          <div className="text-lg font-bold text-purple-600">{hosts.length}</div>
                          <div className="text-sm text-gray-600">总主机数</div>
                        </div>
                        <div className="text-center">
                          <div className="text-lg font-bold text-green-600">{hosts.filter(h => h.status === 'success').length}</div>
                          <div className="text-sm text-gray-600">可用主机</div>
                        </div>
                      </div>
                    </CardContent>
                  </Card>

                  {/* 主机检查状态 */}
                  <Card className="border-0 shadow-lg bg-white/80 backdrop-blur-sm rounded-3xl">
                    <CardHeader className="pb-4">
                      <CardTitle className="text-lg flex items-center justify-between">
                        <div className="flex items-center">
                          <Server className="w-5 h-5 mr-2 text-indigo-600" />
                          主机检查 ({hosts.length} 台主机)
                        </div>
                        <Button
                          onClick={() => handleCheckHosts()}
                          disabled={loading || hosts.length === 0}
                          variant="outline"
                          size="sm"
                          className="h-8"
                        >
                          {loading ? (
                            <Loader2 className="w-4 h-4 mr-1 animate-spin" />
                          ) : (
                            <RefreshCw className="w-4 h-4 mr-1" />
                          )}
                          {loading ? '检查中' : '重新检查'}
                        </Button>
                      </CardTitle>
                    </CardHeader>
                    <CardContent>
                      {hosts.length === 0 && (
                        <div className="text-center py-8">
                          <Server className="mx-auto w-12 h-12 text-gray-400 mb-4" />
                          <p className="text-gray-600">没有要检查的主机</p>
                        </div>
                      )}

                      {hosts.length > 0 && (
                        <div className="space-y-3">
                          {hosts.map((host, index) => (
                            <div key={index} className="p-4 border rounded-xl bg-gray-50/50">
                              <div className="flex items-center justify-between mb-2">
                                <div className="flex items-center space-x-2">
                                  {getHostStatusIcon(host.status)}
                                  <span className="font-medium text-gray-900">{host.ip}</span>
                                  {host.hostname && (
                                    <span className="text-sm text-gray-600">({host.hostname})</span>
                                  )}
                                  <Badge className={`${BADGE_STYLES.base} ${getHostStatusBadgeStyle(host.status)}`}>
                                    {host.status === 'success' ? '成功' : 
                                     host.status === 'failed' ? '失败' : 
                                     host.status === 'checking' ? '检查中' : '等待'}
                                  </Badge>
                                </div>
                                {host.checkTime && (
                                  <span className="text-xs text-gray-500">{host.checkTime}</span>
                                )}
                              </div>
                              
                              {host.message && (
                                <div className={`text-sm mb-2 ${
                                  host.status === 'success' ? 'text-green-700' : 'text-red-700'
                                }`}>
                                  {host.message}
                                </div>
                              )}

                              {host.status === 'success' && (host.osInfo || host.resources) && (
                                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
                                  {host.osInfo && (
                                    <div>
                                      <span className="text-gray-600">操作系统:</span>
                                      <span className="ml-1">{host.osInfo.system} {host.osInfo.version}</span>
                                    </div>
                                  )}
                                  {host.resources && (
                                    <>
                                      <div>
                                        <span className="text-gray-600">CPU:</span>
                                        <span className="ml-1">{host.resources.cpu}</span>
                                      </div>
                                      <div>
                                        <span className="text-gray-600">内存:</span>
                                        <span className="ml-1">{host.resources.memory}</span>
                                      </div>
                                    </>
                                  )}
                                </div>
                              )}

                              {host.status === 'failed' && (
                                <div className="flex items-center text-sm text-red-600">
                                  <AlertTriangle className="w-4 h-4 mr-1" />
                                  请检查主机网络连接和SSH配置
                                </div>
                              )}
                            </div>
                          ))}
                        </div>
                      )}
                    </CardContent>
                  </Card>

                  {/* 统计信息 */}
                  {checkStatus === 'completed' && hosts.length > 0 && (
                    <Card className="border-0 shadow-lg bg-white/80 backdrop-blur-sm rounded-3xl">
                      <CardHeader className="pb-4">
                        <CardTitle className="text-lg flex items-center">
                          <Activity className="w-5 h-5 mr-2 text-purple-600" />
                          检查统计
                        </CardTitle>
                      </CardHeader>
                      <CardContent>
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                          <div className="text-center p-4 bg-green-50 rounded-xl">
                            <div className="text-2xl font-bold text-green-600">
                              {hosts.filter(h => h.status === 'success').length}
                            </div>
                            <div className="text-sm text-green-700">成功连接</div>
                          </div>
                          <div className="text-center p-4 bg-red-50 rounded-xl">
                            <div className="text-2xl font-bold text-red-600">
                              {hosts.filter(h => h.status === 'failed').length}
                            </div>
                            <div className="text-sm text-red-700">连接失败</div>
                          </div>
                          <div className="text-center p-4 bg-blue-50 rounded-xl">
                            <div className="text-2xl font-bold text-blue-600">
                              {Math.round((hosts.filter(h => h.status === 'success').length / hosts.length) * 100)}%
                            </div>
                            <div className="text-sm text-blue-700">成功率</div>
                          </div>
                          <div className="text-center p-4 bg-purple-50 rounded-xl">
                            <div className="text-2xl font-bold text-purple-600">
                              {hosts.filter(h => h.osInfo?.system).length}
                            </div>
                            <div className="text-sm text-purple-700">获取系统信息</div>
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  )}
                </div>
              </div>
          </div>
        </div>
      </div>
    </ClusterWizardLayout>
  )
}
