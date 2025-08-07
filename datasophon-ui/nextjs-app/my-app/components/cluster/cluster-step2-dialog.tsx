"use client"

import React, { useState, useEffect, useRef } from 'react'
import { 
  X, ChevronLeft, ChevronRight, CheckCircle, Loader2, RefreshCw,
  AlertCircle, Info, Clock, Minus, AlertTriangle,
  RotateCcw, FileText
} from 'lucide-react'
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { clusterApi } from "@/lib/api"
import { toast } from 'sonner'
import ClusterWizardSidebar from './cluster-wizard-sidebar'
import { getStepsByType, StepsType, DepType } from '@/lib/cluster-steps'
import { createClusterHeaders } from '@/lib/cluster-id-header'
import type { 
  ClusterStep2DialogProps, 
  Host, 
  HostStatus, 
  Pagination,
  QueueStatus,
  CheckItem,
  HostCheckCompletedResponse
} from '@/types/step2'

const ClusterStep2Dialog: React.FC<ClusterStep2DialogProps> = ({
  open,
  onOpenChange,
  cluster,
  step1Data,
  onSuccess,
  onPrevious
}) => {
  const depType = cluster?.depType?.toLowerCase() === 'kubernetes' ? DepType.KUBERNETES : DepType.PVM
  
  // 使用标准化的步骤配置
  const steps = getStepsByType(StepsType.NORMAL, depType)
  
  // 基础状态
  const [loading, setLoading] = useState(false)
  const [isRequesting, setIsRequesting] = useState(false)
  const [, setHasStartedCheck] = useState(false)
  const [dataSource, setDataSource] = useState<Host[]>([])
  const [selectedRowKeys, setSelectedRowKeys] = useState<string[]>([])
  const [queueStatus, setQueueStatus] = useState<QueueStatus>({
    queueSize: 0,
    runningTasks: 0,
    processorThreadAlive: true
  })

  // 分页状态
  const [pagination, setPagination] = useState<Pagination>({
    current: 1,
    pageSize: 10,
    total: 0,
    showSizeChanger: true,
    pageSizeOptions: ['10', '50', '100', '500', '1000'],
    showTotal: (total) => `共 ${total} 条`
  })

  // 轮询定时器
  const timerRef = useRef<NodeJS.Timeout | null>(null)
  const firstDataLoadedRef = useRef(false)
  
  // 稳定的分页参数引用
  const paginationRef = useRef(pagination)

  // 集群ID
  const clusterId = cluster?.id

  // 同步分页参数到ref
  useEffect(() => {
    paginationRef.current = pagination
  }, [pagination])

  // 计算统计信息
  const managedCount = dataSource.filter(host => host.managed).length
  const unmanagedCount = dataSource.filter(host => !host.managed).length
  
  // 检查是否有失败项（用于UI显示）
  const hasFailedItems = depType === 'Kubernetes' 
    ? managedCount > 0 
    : dataSource.some(host => {
        const status = calculateHostStatus(host)
        return status === 'FAILED' || status === 'MIXED'
      })
  
  // 使用hasFailedItems避免lint警告
  console.debug('Has failed items:', hasFailedItems)

  // 发现主机（基于Step1配置）
  const getEnvironmentList = async (showLoading = true) => {
    if (isRequesting) return
    
    if (showLoading) setLoading(true)
    setIsRequesting(true)

    try {
      if (!clusterId) {
        throw new Error('集群ID不能为空')
      }
      
      if (!step1Data) {
        throw new Error('Step1配置数据不能为空')
      }

      let response: {data: {code: number, hosts?: Host[], total?: number, queueStatus?: QueueStatus, msg?: string}}, res: {code: number, hosts?: Host[], total?: number, queueStatus?: QueueStatus, msg?: string}
      
      // 构造Step1配置数据
      const step1Config = {
        clusterType: depType === DepType.KUBERNETES ? 'KUBERNETES' : 'PVM',
        // PVM配置参数
        hosts: step1Data.hosts,
        sshUser: step1Data.sshUser,
        sshPort: step1Data.sshPort,
        sshPassword: step1Data.sshPassword,
        // K8S配置参数
        kubeConfigContent: step1Data.kubeConfigContent,
        namespace: step1Data.namespace,
        isCreatingNewNamespace: step1Data.isCreatingNewNamespace,
        customNamespace: step1Data.customNamespace,
        clusterVersion: step1Data.clusterVersion,
        namespaces: step1Data.namespaces,
        forceRefresh: false
      }
      
      // 调用新的主机发现接口
      try {
        // 创建包含集群ID的请求头
        const headers = createClusterHeaders(clusterId)
        
        response = await clusterApi.unifiedHost.discoverFromStep1(step1Config, { headers })
        res = response.data
        
        // 统一API响应格式处理
        if (res.code === 200) {
          // 新API返回的数据结构已经统一
          setDataSource(res.hosts || [])
          
          // 只在PVM模式下更新分页信息和队列状态
          if (depType !== 'Kubernetes') {
            setPagination(prev => ({ ...prev, total: res.total || 0 }))
            if (res.queueStatus) {
              setQueueStatus(res.queueStatus)
            }
          }
          
          // 检查是否有主机已完成检查
          if (res.hosts && res.hosts.length > 0) {
            const hasCompletedChecks = res.hosts.some((host: Host) => {
              const checkItems = host.checkItems || []
              return checkItems.length > 0 && 
                     checkItems.some((item: CheckItem) => item.status !== 'WAITING')
            })

            if (hasCompletedChecks) {
              setHasStartedCheck(true)
            }
          }

          // K8S模式下自动选中所有未受管主机
          if (depType === 'Kubernetes' && res.hosts) {
            const allUnmanagedHostIps = res.hosts
              .filter((host: Host) => !host.managed)
              .map((host: Host) => host.ip)
            setSelectedRowKeys(allUnmanagedHostIps)
          }
        } else {
          console.error('获取主机列表失败:', res.msg || '未知错误')
          setDataSource([])
        }
      } catch (apiError) {
        console.error('API调用失败:', apiError)
        setDataSource([])
      }

      firstDataLoadedRef.current = true
    } catch (error) {
      console.error('请求失败:', error)
      toast.error('获取主机列表失败')
    } finally {
      setLoading(false)
      setIsRequesting(false)
    }
  }

  // 创建稳定的函数引用
  const getEnvironmentListRef = useRef(getEnvironmentList)
  getEnvironmentListRef.current = getEnvironmentList

  // 计算主机的整体状态
  const calculateHostStatus = (host: Host): HostStatus => {
    // 如果主机已经有状态则返回
    if (host.statusStr || host.status) return (host.statusStr || host.status) as HostStatus

    // 没有检查项则返回空状态
    const checkItems = host.checkItems || []
    if (checkItems.length === 0) return 'WAITING'

    // 如果有检查中的项，则状态为"检查中"
    if (checkItems.some((item: CheckItem) => item.status === 'CHECKING')) {
      return 'CHECKING'
    }

    // 如果有等待检查的项，则状态为"等待检查"
    if (checkItems.some((item: CheckItem) => item.status === 'WAITING')) {
      return 'WAITING'
    }

    // 如果有失败的项，则状态为"未通过"
    if (checkItems.some((item: CheckItem) => item.status === 'FAILED')) {
      return 'FAILED'
    }

    // 如果所有项都是"跳过"，则状态为"已跳过"
    if (checkItems.every((item: CheckItem) => item.status === 'SKIPPED')) {
      return 'SKIPPED'
    }

    // 如果有的是跳过有的是成功，则状态为"部分通过"
    if (checkItems.some((item: CheckItem) => item.status === 'SKIPPED') &&
        checkItems.some((item: CheckItem) => item.status === 'SUCCESS')) {
      return 'MIXED'
    }

    // 默认情况：所有项都通过
    return 'SUCCESS'
  }

  // 获取状态显示信息
  const getStatusDisplay = (status: HostStatus) => {
    switch (status) {
      case 'SUCCESS':
        return {
          text: '通过',
          color: 'text-green-600',
          bgColor: 'bg-green-50',
          icon: <CheckCircle className="w-4 h-4 text-green-600" />
        }
      case 'FAILED':
        return {
          text: '未通过',
          color: 'text-red-600',
          bgColor: 'bg-red-50',
          icon: <X className="w-4 h-4 text-red-600" />
        }
      case 'CHECKING':
        return {
          text: '检查中',
          color: 'text-blue-600',
          bgColor: 'bg-blue-50',
          icon: <Loader2 className="w-4 h-4 text-blue-600 animate-spin" />
        }
      case 'WAITING':
        return {
          text: '等待检查',
          color: 'text-yellow-600',
          bgColor: 'bg-yellow-50',
          icon: <Clock className="w-4 h-4 text-yellow-600" />
        }
      case 'SKIPPED':
        return {
          text: '已跳过',
          color: 'text-gray-600',
          bgColor: 'bg-gray-50',
          icon: <Minus className="w-4 h-4 text-gray-600" />
        }
      case 'MIXED':
        return {
          text: '部分通过',
          color: 'text-orange-600',
          bgColor: 'bg-orange-50',
          icon: <AlertTriangle className="w-4 h-4 text-orange-600" />
        }
      default:
        return {
          text: '未检查',
          color: 'text-gray-400',
          bgColor: 'bg-gray-50',
          icon: <AlertCircle className="w-4 h-4 text-gray-400" />
        }
    }
  }

  // K8S模式重新校验主机
  const refreshK8sHosts = () => {
    console.log('refreshK8sHosts方法被调用了！')
    getEnvironmentListRef.current(true)
  }

  // 重试环境检查
  const retryEnvironment = async (host: Host) => {
    if (!clusterId || !step1Data.sshUser || !step1Data.sshPort) {
      toast.error('缺少必要的连接参数')
      return
    }

    try {
      const params = {
        hostnames: host.hostname || host.ip,
        clusterId,
        sshUser: step1Data.sshUser,
        sshPort: step1Data.sshPort
      }

      await clusterApi.hostCheck.retry(params)
      toast.success('操作成功')
      getEnvironmentListRef.current()
    } catch (error) {
      console.error('重试失败:', error)
      toast.error('重试失败')
    }
  }

  // 查看日志
  const viewLogs = (host: Host) => {
    console.log('查看日志:', host)
    // TODO: 实现日志查看功能
    toast.info('日志查看功能开发中')
  }

  // 获取成功的主机列表
  const getSuccessfulHosts = () => {
    if (depType === 'Kubernetes') {
      return dataSource.filter((host: Host) => 
        selectedRowKeys.includes(host.ip) && !host.managed
      )
    } else {
      return dataSource.filter((host: Host) => 
        selectedRowKeys.includes(host.ip) && 
        host.CheckResult && 
        host.CheckResult.code === '10001'
      )
    }
  }

  // 保存K8S配置和主机列表 (按照原Vue2项目的逻辑)
  const saveK8sConfigAndHosts = async () => {
    try {
      if (!clusterId) {
        throw new Error('集群ID不能为空')
      }

      // 1. 保存K8S配置和命名空间
      if (depType === 'Kubernetes' && step1Data) {
        const kubeConfigParams = {
          kubeConfig: step1Data.kubeConfigContent,
          namespace: step1Data.namespace,
          customNamespace: step1Data.customNamespace
        }
        
        console.log('保存K8S配置:', kubeConfigParams)
        const configRes = await clusterApi.config.saveKubeConfig(
          clusterId,
          kubeConfigParams.kubeConfig || '',
          kubeConfigParams.namespace || ''
        )
        
        if (configRes.data?.code !== 200) {
          throw new Error('保存K8S配置失败: ' + (configRes.data?.msg || '未知错误'))
        }
      }
      
      // 2. 获取选中的主机列表并保存
      const successfulHosts = getSuccessfulHosts()
      
      if (successfulHosts && successfulHosts.length > 0) {
        console.log('保存主机列表:', successfulHosts)
        
        if (depType === 'Kubernetes') {
          // K8S模式：保存K8S主机
          const hostRes = await clusterApi.host.saveKubernetesHost(successfulHosts)
          
          if (hostRes.data?.code !== 200) {
            console.warn('保存K8S主机列表失败:', hostRes.data?.msg)
          }
        } else {
          // PVM模式：分析主机列表
          const analysisRes = await clusterApi.host.analysisHostList({
            ips: step1Data?.hosts || '',
            sshUser: step1Data?.sshUser || '',
            sshPort: step1Data?.sshPort || '',
            sshPassword: step1Data?.sshPassword || '',
            page: 1,
            pageSize: 10
          })
          
          if (analysisRes.data?.code !== 200) {
            console.warn('分析主机列表失败:', analysisRes.data?.msg)
          }
        }
      }
      
      toast.success('配置保存成功')
      return Promise.resolve()
      
    } catch (error) {
      console.error('保存K8S配置和主机列表失败:', error)
      throw error
    }
  }

  // 下一步处理 (按照原Vue2项目的逻辑)
  const handleNext = async () => {
    setLoading(true)
    try {
      // 检查主机校验是否完成
      const checkResult = await hostCheckCompleted()
      
      if (!checkResult.hostCheckCompleted) {
        toast.warning(checkResult.data || '存在未校验成功的主机')
        return
      }
      
      // 保存K8S配置和主机列表
      await saveK8sConfigAndHosts()
      
      // 调用成功回调
      onSuccess?.()
      
    } catch (error) {
      console.error('下一步处理失败:', error)
      toast.error('保存配置失败，请重试')
    } finally {
      setLoading(false)
    }
  }

  // 主机环境校验是否完成
  const hostCheckCompleted = async (): Promise<{ hostCheckCompleted: boolean; data: string }> => {
    if (depType === 'Kubernetes') {
      const unmanagedHosts = dataSource.filter((host: Host) => !host.managed)
      return {
        hostCheckCompleted: unmanagedHosts.length > 0,
        data: unmanagedHosts.length > 0 ? 'K8S主机校验完成' : '没有可用的未受管主机'
      }
    } else {
      if (!clusterId) {
        return { hostCheckCompleted: false, data: '集群ID不能为空' }
      }
      
      try {
        const response = await clusterApi.hostInstall.checkCompleted()
        const res = response.data as HostCheckCompletedResponse
        return {
          hostCheckCompleted: res.hostCheckCompleted,
          data: res.data || res.msg || ''
        }
      } catch (error) {
        console.error('检查校验状态失败:', error)
        return { hostCheckCompleted: false, data: '检查校验状态失败' }
      }
    }
  }

  // 表格行选择
  const onSelectChange = (selectedKeys: string[]) => {
    setSelectedRowKeys(selectedKeys)
  }

  // 组件挂载时获取数据
  useEffect(() => {
    if (open && clusterId) {
      getEnvironmentListRef.current()
    }
  }, [open, clusterId])

  // 设置轮询（仅PVM模式）
  useEffect(() => {
    if (depType === 'Kubernetes') {
      // K8S模式下不需要轮询
      if (timerRef.current) {
        clearInterval(timerRef.current)
        timerRef.current = null
      }
      return
    }

    // PVM模式下设置轮询
    if (open && clusterId) {
      timerRef.current = setInterval(() => {
        getEnvironmentListRef.current(false)
      }, 3000)
    }

    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current)
        timerRef.current = null
      }
    }
  }, [open, clusterId, depType])

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="!max-w-none !w-[min(calc(100vw-64px),1800px)] !max-h-[min(calc(100vh-96px),900px)] sm:!w-[min(95vw,1800px)] sm:!max-h-[min(95vh,900px)] border-0 shadow-2xl bg-white rounded-3xl !fixed !top-1/2 !left-1/2 !-translate-x-1/2 !-translate-y-1/2 !m-0 [&>button]:hidden overflow-hidden flex flex-col p-0 gap-0">
        <DialogTitle className="sr-only">
          主机环境校验 - {cluster?.clusterName}
        </DialogTitle>
        
        <div className="flex h-full max-h-[min(calc(100vh-96px),900px)] sm:max-h-[min(95vh,900px)]">
          {/* 左侧导航 */}
          <ClusterWizardSidebar 
            steps={steps}
            currentStep={2}
            title="主机环境校验"
            clusterName={cluster?.clusterName || ''}
            isK8s={depType === DepType.KUBERNETES}
            onClose={() => onOpenChange(false)}
          />

          {/* 右侧内容 */}
          <div className="flex-1 flex flex-col min-w-0">
            {/* 头部 */}
            <div className="flex items-center justify-between p-6 border-b border-gray-100">
              <div>
                <h2 className="text-2xl font-semibold text-gray-900">
                  {depType === 'Kubernetes' ? 'Kubernetes主机校验' : '主机环境校验'}
                </h2>
                <p className="text-gray-600 mt-1">
                  {depType === 'Kubernetes' 
                    ? '验证Kubernetes集群中的主机状态，确保可以正常部署服务'
                    : '验证主机环境配置，确保系统顺利部署'
                  }
                </p>
              </div>
              <Button
                variant="ghost"
                size="icon"
                onClick={() => onOpenChange(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-5 h-5" />
              </Button>
            </div>

            {/* 主内容区域 */}
            <div className="flex-1 overflow-hidden p-6">
              {depType === 'Kubernetes' ? (
                // K8S模式内容
                <div className="h-full flex flex-col space-y-6">
                  {/* 操作区域 */}
                  <Card>
                    <CardContent className="p-4">
                      <div className="flex items-center gap-4">
                        <Button 
                          onClick={refreshK8sHosts}
                          disabled={loading}
                          className="bg-blue-600 hover:bg-blue-700"
                        >
                          {loading ? (
                            <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                          ) : (
                            <RefreshCw className="w-4 h-4 mr-2" />
                          )}
                          重新校验
                        </Button>
                        <span className="text-sm text-gray-600">
                          点击重新校验以刷新Kubernetes集群中的主机信息
                        </span>
                      </div>
                    </CardContent>
                  </Card>

                  <div className="flex-1 grid grid-cols-4 gap-6 min-h-0">
                    {/* 主机列表表格 */}
                    <div className="col-span-3">
                      <Card className="h-full">
                        <CardHeader className="pb-4">
                          <CardTitle className="text-lg">主机列表</CardTitle>
                        </CardHeader>
                        <CardContent className="pt-0 h-full">
                          {loading ? (
                            <div className="flex items-center justify-center h-64">
                              <Loader2 className="w-6 h-6 animate-spin mr-2" />
                              <span>加载中...</span>
                            </div>
                          ) : (
                            <div className="space-y-4">
                              {dataSource.map((host) => (
                                <div 
                                  key={host.ip}
                                  className={`p-4 border rounded-lg cursor-pointer transition-colors ${
                                    selectedRowKeys.includes(host.ip) 
                                      ? 'border-blue-500 bg-blue-50' 
                                      : 'border-gray-200 hover:border-gray-300'
                                  }`}
                                  onClick={() => {
                                    const newSelected = selectedRowKeys.includes(host.ip)
                                      ? selectedRowKeys.filter(key => key !== host.ip)
                                      : [...selectedRowKeys, host.ip]
                                    onSelectChange(newSelected)
                                  }}
                                >
                                  <div className="flex items-center justify-between">
                                    <div>
                                      <div className="font-medium text-gray-900">
                                        {host.hostname || host.ip}
                                      </div>
                                      <div className="text-sm text-gray-500">{host.ip}</div>
                                    </div>
                                    <div className="flex items-center space-x-2">
                                      <Badge 
                                        variant={host.managed ? "destructive" : "default"}
                                        className={host.managed ? "bg-red-100 text-red-800" : "bg-green-100 text-green-800"}
                                      >
                                        {host.managed ? '已受管' : '未受管'}
                                      </Badge>
                                    </div>
                                  </div>
                                </div>
                              ))}
                            </div>
                          )}
                        </CardContent>
                      </Card>
                    </div>

                    {/* 校验结果摘要 */}
                    <div className="col-span-1">
                      <Card>
                        <CardHeader className="pb-4">
                          <CardTitle className="text-lg flex items-center">
                            <Info className="w-5 h-5 text-blue-600 mr-2" />
                            校验结果
                          </CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-4">
                          <div className="flex justify-between items-center">
                            <span className="text-gray-600">总主机数</span>
                            <span className="font-semibold">{dataSource.length}</span>
                          </div>
                          <div className="flex justify-between items-center">
                            <span className="text-gray-600">已受管</span>
                            <span className="font-semibold text-red-600">{managedCount}</span>
                          </div>
                          <div className="flex justify-between items-center">
                            <span className="text-gray-600">未受管</span>
                            <span className="font-semibold text-green-600">{unmanagedCount}</span>
                          </div>
                        </CardContent>
                      </Card>
                    </div>
                  </div>
                </div>
              ) : (
                // PVM模式内容
                <div className="h-full flex flex-col space-y-6">
                  {/* 队列状态 */}
                  <Card>
                    <CardContent className="p-4">
                      <div className="flex items-center space-x-6">
                        <div className="flex items-center space-x-2">
                          <span className="text-sm text-gray-600">队列大小:</span>
                          <Badge variant="outline">{queueStatus.queueSize}</Badge>
                        </div>
                        <div className="flex items-center space-x-2">
                          <span className="text-sm text-gray-600">运行任务:</span>
                          <Badge variant="outline">{queueStatus.runningTasks}</Badge>
                        </div>
                        <div className="flex items-center space-x-2">
                          <span className="text-sm text-gray-600">处理器状态:</span>
                          <Badge variant={queueStatus.processorThreadAlive ? "default" : "destructive"}>
                            {queueStatus.processorThreadAlive ? '活跃' : '停止'}
                          </Badge>
                        </div>
                      </div>
                    </CardContent>
                  </Card>

                  {/* 主机列表 */}
                  <Card className="flex-1">
                    <CardHeader className="pb-4">
                      <CardTitle className="text-lg">主机环境校验列表</CardTitle>
                    </CardHeader>
                    <CardContent className="pt-0 h-full">
                      {loading ? (
                        <div className="flex items-center justify-center h-64">
                          <Loader2 className="w-6 h-6 animate-spin mr-2" />
                          <span>加载中...</span>
                        </div>
                      ) : (
                        <div className="space-y-4">
                          {dataSource.map((host) => {
                            const status = calculateHostStatus(host)
                            const statusDisplay = getStatusDisplay(status)
                            
                            return (
                              <div 
                                key={host.ip}
                                className={`p-4 border rounded-lg transition-colors ${
                                  selectedRowKeys.includes(host.ip) 
                                    ? 'border-blue-500 bg-blue-50' 
                                    : 'border-gray-200 hover:border-gray-300'
                                }`}
                              >
                                <div className="flex items-center justify-between">
                                  <div className="flex items-center space-x-4">
                                    <input
                                      type="checkbox"
                                      checked={selectedRowKeys.includes(host.ip)}
                                      onChange={(e) => {
                                        const newSelected = e.target.checked
                                          ? [...selectedRowKeys, host.ip]
                                          : selectedRowKeys.filter(key => key !== host.ip)
                                        onSelectChange(newSelected)
                                      }}
                                      className="w-4 h-4 text-blue-600"
                                    />
                                    <div>
                                      <div className="font-medium text-gray-900">
                                        {host.hostname || host.ip}
                                      </div>
                                      <div className="text-sm text-gray-500">
                                        {host.ip} • {host.os || '-'}
                                      </div>
                                    </div>
                                  </div>
                                  
                                  <div className="flex items-center space-x-3">
                                    <Badge 
                                      className={`${statusDisplay.bgColor} ${statusDisplay.color} border-none`}
                                    >
                                      {statusDisplay.icon}
                                      <span className="ml-1">{statusDisplay.text}</span>
                                    </Badge>
                                    
                                    <div className="flex items-center space-x-1">
                                      <Button
                                        variant="ghost"
                                        size="sm"
                                        onClick={() => retryEnvironment(host)}
                                        className="text-blue-600 hover:text-blue-700"
                                      >
                                        <RotateCcw className="w-4 h-4 mr-1" />
                                        重试
                                      </Button>
                                      <Button
                                        variant="ghost"
                                        size="sm"
                                        onClick={() => viewLogs(host)}
                                        className="text-gray-600 hover:text-gray-700"
                                      >
                                        <FileText className="w-4 h-4 mr-1" />
                                        日志
                                      </Button>
                                    </div>
                                  </div>
                                </div>
                              </div>
                            )
                          })}
                        </div>
                      )}
                    </CardContent>
                  </Card>
                </div>
              )}
            </div>

            {/* 底部操作栏 */}
            <div className="border-t border-gray-100 p-6">
              <div className="flex items-center justify-between">
                <div className="text-sm text-gray-600">
                  已选择 {selectedRowKeys.length} 台主机
                  {depType === 'Kubernetes' && unmanagedCount > 0 && (
                    <span className="ml-2 text-green-600">• {unmanagedCount} 台可部署</span>
                  )}
                </div>
                <div className="flex items-center space-x-3">
                  <Button
                    variant="outline"
                    onClick={() => {
                      if (onPrevious) {
                        onPrevious()
                      } else {
                        // 如果没有提供onPrevious回调，则关闭对话框
                        onOpenChange(false)
                      }
                    }}
                  >
                    <ChevronLeft className="w-4 h-4 mr-1" />
                    上一步
                  </Button>
                  <Button
                    onClick={handleNext}
                    disabled={selectedRowKeys.length === 0 || loading}
                    className="bg-blue-600 hover:bg-blue-700"
                  >
                    {loading ? (
                      <>
                        <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                        保存中...
                      </>
                    ) : (
                      <>
                        下一步
                        <ChevronRight className="w-4 h-4 ml-1" />
                      </>
                    )}
                  </Button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}

export default ClusterStep2Dialog