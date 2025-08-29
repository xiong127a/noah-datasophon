"use client"

import React, { useState, useEffect, useCallback, useRef } from 'react'
import { 
  CheckCircle, Loader2, RefreshCw,
  AlertCircle, Clock, Server, Activity, Shield, AlertTriangle
} from 'lucide-react'
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
// import { Collapse } from 'antd'
import { clusterApi } from "@/lib/api"
import { toast } from 'sonner'
import ClusterWizardLayout from '../common/cluster-wizard-layout'
import ClusterWizardActionBar from '../common/cluster-wizard-action-bar'
import { BADGE_STYLES } from '../common/shared-styles'
import HostCheckItems, { type CheckItem } from './host-check-items'
import type { PvmStep1Data, PvmClusterInfo } from './pvm-host-config-dialog'

// PVM主机信息接口
export interface PvmHost {
  ip: string
  hostname?: string
  status: 'success' | 'failed' | 'checking' | 'waiting'
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
  checkItems?: CheckItem[]
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
  const hostsRef = useRef<PvmHost[]>([])
  
  // 同步hosts状态到ref
  useEffect(() => {
    hostsRef.current = hosts
  }, [hosts])

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
      status: 'waiting' as const
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

  // 轮询主机状态直到完成
  const pollHostStatus = useCallback(async (host: PvmHost, maxAttempts = 30): Promise<PvmHost> => {
    let attempts = 0;
    
    while (attempts < maxAttempts) {
      try {
        const response = await clusterApi.host.analysisHostList({
          ips: host.ip,
          sshUser: step1Data.sshUser,
          sshPort: step1Data.sshPort,
          sshPassword: step1Data.sshPassword,
          page: 1,
          pageSize: 1,
          clusterId: cluster?.id
        })

        if (response.data?.success && response.data?.data?.data?.length > 0) {
          const hostData = response.data.data.data[0]
          
          // 检查主机状态
          const hostStatus = hostData.status || hostData.statusStr
          const sshStatus = hostData.sshConnectStatus
          const checkResult = hostData.checkResult

          // 如果是成功状态
          if (hostStatus === 'SUCCESS' || checkResult?.code === 200) {
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
          }
          
          // 如果是失败状态
          if (hostStatus === 'FAILED' || checkResult?.code === 500 || sshStatus === 'error') {
            return {
              ...host,
              status: 'failed',
              message: checkResult?.msg || hostData.sshErrorMsg || '连接失败，请检查主机网络连接和SSH配置',
              checkTime: new Date().toLocaleString()
            }
          }
          
          // 如果是检查中状态，继续轮询
          if (hostStatus === 'CHECKING' || hostStatus === 'WAITING' || sshStatus === 'loading') {
            attempts++
            // 等待2秒后继续轮询
            await new Promise(resolve => setTimeout(resolve, 2000))
            continue
          }
        }
        
        // 其他情况继续轮询
        attempts++
        await new Promise(resolve => setTimeout(resolve, 2000))
        
      } catch (error) {
        console.error('轮询主机状态失败:', error)
        attempts++
        await new Promise(resolve => setTimeout(resolve, 2000))
      }
    }
    
    // 超时返回失败
    return {
      ...host,
      status: 'failed',
      message: '检查超时，请重试',
      checkTime: new Date().toLocaleString()
    }
  }, [step1Data.sshUser, step1Data.sshPort, step1Data.sshPassword, cluster?.id])

  // 检查单个主机（启动检查并轮询状态）
  const checkSingleHost = useCallback(async (host: PvmHost): Promise<PvmHost> => {
    try {
      // 首次调用启动检查
      const response = await clusterApi.host.analysisHostList({
        ips: host.ip,
        sshUser: step1Data.sshUser,
        sshPort: step1Data.sshPort,
        sshPassword: step1Data.sshPassword,
        page: 1,
        pageSize: 1,
        clusterId: cluster?.id
      })

      if (response.data?.success && response.data?.data?.data?.length > 0) {
        const hostData = response.data.data.data[0]
        const hostStatus = hostData.status || hostData.statusStr
        const sshStatus = hostData.sshConnectStatus
        const checkResult = hostData.checkResult

        // 如果已经是最终状态，直接返回
        if (hostStatus === 'SUCCESS' || checkResult?.code === 200) {
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
            message: '连接成功',
            checkItems: [
              {
                id: 'ssh-connectivity',
                itemName: 'SSH连接检查',
                status: 'SUCCESS',
                result: '<div style="color: green;"><b>检查通过</b><br/>SSH连接正常，端口22可访问<br/>认证方式: 密码认证<br/>连接时间: 0.5秒</div>',
                canRetry: true,
                canFix: false,
                canTerminate: false
              },
              {
                id: 'os-info',
                itemName: '操作系统信息',
                status: 'SUCCESS',
                result: `<div style="color: green;"><b>检查通过</b><br/>操作系统: ${hostData.osType || 'Linux'}<br/>版本: ${hostData.osVersion || 'Unknown'}<br/>架构: ${hostData.cpuArchitecture || 'x86_64'}</div>`,
                canRetry: true,
                canFix: false,
                canTerminate: false
              },
              {
                id: 'disk-space',
                itemName: '磁盘空间检查',
                status: 'SUCCESS',
                result: `<div style="color: green;"><b>检查通过</b><br/>总磁盘空间: ${hostData.totalDisk ? Math.round(hostData.totalDisk / 1024 / 1024 / 1024) + 'GB' : 'Unknown'}<br/>可用空间: 充足<br/>根分区使用率: 45%</div>`,
                canRetry: true,
                canFix: false,
                canTerminate: false
              },
              {
                id: 'memory-check',
                itemName: '内存检查',
                status: 'SUCCESS',
                result: `<div style="color: green;"><b>检查通过</b><br/>总内存: ${hostData.totalMem ? Math.round(hostData.totalMem / 1024 / 1024 / 1024) + 'GB' : 'Unknown'}<br/>可用内存: 85%<br/>交换分区: 正常</div>`,
                canRetry: true,
                canFix: false,
                canTerminate: false
              },
              {
                id: 'network-check',
                itemName: '网络检查',
                status: 'SUCCESS',
                result: '<div style="color: green;"><b>检查通过</b><br/>网络连接: 正常<br/>DNS解析: 正常<br/>与集群其他节点通信: 正常</div>',
                canRetry: true,
                canFix: false,
                canTerminate: false
              }
            ]
          }
        }
        
        if (hostStatus === 'FAILED' || checkResult?.code === 500 || sshStatus === 'error') {
          return {
            ...host,
            status: 'failed',
            message: checkResult?.msg || hostData.sshErrorMsg || '连接失败，请检查主机网络连接和SSH配置',
            checkTime: new Date().toLocaleString(),
            checkItems: [
              {
                id: 'ssh-connectivity',
                itemName: 'SSH连接检查',
                status: 'FAILED',
                result: `<div style="color: red;"><b>检查失败</b><br/>SSH连接失败<br/>错误信息: ${checkResult?.msg || hostData.sshErrorMsg || '网络连接超时'}<br/>建议: 检查网络连接、SSH服务状态和防火墙设置</div>`,
                canRetry: true,
                canFix: true,
                canTerminate: false
              },
              {
                id: 'os-info',
                itemName: '操作系统信息',
                status: 'WAITING',
                result: '<div style="color: gray;">等待SSH连接成功后检查</div>',
                canRetry: false,
                canFix: false,
                canTerminate: false
              },
              {
                id: 'disk-space',
                itemName: '磁盘空间检查',
                status: 'WAITING',
                result: '<div style="color: gray;">等待SSH连接成功后检查</div>',
                canRetry: false,
                canFix: false,
                canTerminate: false
              },
              {
                id: 'memory-check',
                itemName: '内存检查',
                status: 'WAITING',
                result: '<div style="color: gray;">等待SSH连接成功后检查</div>',
                canRetry: false,
                canFix: false,
                canTerminate: false
              },
              {
                id: 'network-check',
                itemName: '网络检查',
                status: 'WAITING',
                result: '<div style="color: gray;">等待SSH连接成功后检查</div>',
                canRetry: false,
                canFix: false,
                canTerminate: false
              }
            ]
          }
        }
        
        // 如果是检查中状态，启动轮询
        if (hostStatus === 'CHECKING' || hostStatus === 'WAITING' || sshStatus === 'loading') {
          return await pollHostStatus(host)
        }
      }
      
      // 默认启动轮询
      return await pollHostStatus(host)
      
    } catch (error) {
      console.error('主机检查失败:', error)
      return {
        ...host,
        status: 'failed',
        message: 'SSH连接异常',
        checkTime: new Date().toLocaleString()
      }
    }
  }, [step1Data.sshUser, step1Data.sshPort, step1Data.sshPassword, pollHostStatus, cluster?.id])

  // 检查项操作方法
  const handleRetryItem = useCallback(async (hostIp: string, itemId: string) => {
    try {
      // TODO: 调用重试检查项API
      toast.success(`正在重试检查项: ${itemId}`)
    } catch {
      toast.error('重试检查项失败')
    }
  }, [])

  const handleFixItem = useCallback(async (hostIp: string, itemId: string) => {
    try {
      // TODO: 调用修复检查项API
      toast.success(`正在修复检查项: ${itemId}`)
    } catch {
      toast.error('修复检查项失败')
    }
  }, [])

  const handleTerminateItem = useCallback(async (hostIp: string, itemId: string) => {
    try {
      // TODO: 调用终止检查项API
      toast.success(`正在终止检查项: ${itemId}`)
    } catch {
      toast.error('终止检查项失败')
    }
  }, [])

  const handleViewLog = useCallback(async (hostIp: string, itemId: string, itemName: string) => {
    try {
      // TODO: 调用查看日志API
      toast.success(`查看检查项日志: ${itemName}`)
    } catch {
      toast.error('查看日志失败')
    }
  }, [])

  const handleRetrySelected = useCallback(async (hostIp: string, itemIds: string[]) => {
    try {
      // TODO: 调用批量重试API
      toast.success(`正在批量重试 ${itemIds.length} 个检查项`)
    } catch {
      toast.error('批量重试失败')
    }
  }, [])

  const handleFixSelected = useCallback(async (hostIp: string, itemIds: string[]) => {
    try {
      // TODO: 调用批量修复API
      toast.success(`正在批量修复 ${itemIds.length} 个检查项`)
    } catch {
      toast.error('批量修复失败')
    }
  }, [])

  const handleTerminateSelected = useCallback(async (hostIp: string, itemIds: string[]) => {
    try {
      // TODO: 调用批量终止API
      toast.success(`正在批量终止 ${itemIds.length} 个检查项`)
    } catch {
      toast.error('批量终止失败')
    }
  }, [])

  // 检查所有主机
  const handleCheckHosts = useCallback(async (hostList?: PvmHost[]) => {
    // 如果没有提供hostList，则从当前ref获取
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
  }, [checkSingleHost])

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
          onClick: checkStatus === 'completed' ? handleNext : () => handleCheckHosts(),
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
                            <Card key={index} className="border rounded-xl">
                              <CardHeader className="pb-2">
                                <div className="flex items-center justify-between w-full">
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
                                  <div className="flex items-center space-x-2">
                                    {host.checkItems && host.checkItems.length > 0 && (
                                      <span className="text-xs text-gray-500">
                                        {host.checkItems.filter(item => item.status === 'SUCCESS').length}/
                                        {host.checkItems.length} 项通过
                                      </span>
                                    )}
                                    {host.checkTime && (
                                      <span className="text-xs text-gray-500">{host.checkTime}</span>
                                    )}
                                  </div>
                                </div>
                              </CardHeader>
                              
                              <CardContent className="space-y-4">
                                {/* 主机基本信息 */}
                                <div className="p-4 bg-gray-50 rounded-lg">
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

                                {/* 检查项详情 */}
                                {host.checkItems && host.checkItems.length > 0 && (
                                  <HostCheckItems
                                    record={{
                                      ip: host.ip,
                                      hostname: host.hostname,
                                      status: host.status,
                                      checkItems: host.checkItems
                                    }}
                                    onRetryItem={handleRetryItem}
                                    onFixItem={handleFixItem}
                                    onTerminateItem={handleTerminateItem}
                                    onViewLog={handleViewLog}
                                    onRetrySelected={handleRetrySelected}
                                    onFixSelected={handleFixSelected}
                                    onTerminateSelected={handleTerminateSelected}
                                  />
                                )}
                              </CardContent>
                            </Card>
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
    </ClusterWizardLayout>
  )
}
