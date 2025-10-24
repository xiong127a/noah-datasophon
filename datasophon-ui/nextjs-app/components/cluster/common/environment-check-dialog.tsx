'use client'

import { useState, useEffect } from 'react'
import { Button } from '@/components/ui/button'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Progress } from '@/components/ui/progress'
import { Badge } from '@/components/ui/badge'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { 
  CheckCircle2, 
  XCircle, 
  Clock, 
  Loader2, 
  AlertTriangle,
  ChevronDown,
  ChevronUp,
  Play,
  Pause,
  SkipForward,
  Wrench
} from 'lucide-react'
import { clusterApiV1 } from '@/lib/api-utils-v1'
import { API_BASE_URL, API_PATHS_V1 } from '@/lib/api-config-v1'
import ClusterWizardLayout from './cluster-wizard-layout'
import ClusterWizardActionBar from './cluster-wizard-action-bar'
import { CheckItemDetailCard } from './check-item-detail-card'
import { CheckLogsDialog } from './check-logs-dialog'

interface EnvironmentCheckDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  cluster: any
  hostList: Array<{ ip: string; hostname?: string }>
  connectionParams: any
  onNext: () => void
  onPrevious: () => void
}

interface CheckItemStatus {
  checkKey: string
  displayName: string
  priority: number
  status: 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'SKIPPED'
  message: string
  recommendation?: string
  canSkip: boolean
  canRepair: boolean
  checkResult?: any
  startTime?: number
  endTime?: number
}

interface HostCheckStatus {
  hostIp: string
  hostname: string
  overallStatus: 'PENDING' | 'RUNNING' | 'SUCCESS' | 'PARTIAL_SUCCESS' | 'FAILED'
  checkItems: CheckItemStatus[]
  totalItems: number
  completedItems: number
  successItems: number
  failedItems: number
  skippedItems: number
  startTime?: number
  endTime?: number
  errorMessage?: string
}

export default function EnvironmentCheckDialog({
  open,
  onOpenChange,
  cluster,
  hostList,
  connectionParams,
  onNext,
  onPrevious
}: EnvironmentCheckDialogProps) {
  const [checkStatus, setCheckStatus] = useState<HostCheckStatus[]>([])
  const [isChecking, setIsChecking] = useState(false)
  const [isPaused, setIsPaused] = useState(false)
  const [expandedHosts, setExpandedHosts] = useState<Set<string>>(new Set())
  const [expandedCheckItems, setExpandedCheckItems] = useState<Set<string>>(new Set()) // 格式: "hostIp-checkKey"
  const [eventSource, setEventSource] = useState<EventSource | null>(null)
  const [actualHostList, setActualHostList] = useState<Array<{ ip: string; hostname?: string }>>([])
  const [logsDialogOpen, setLogsDialogOpen] = useState(false)
  const [selectedCheckItem, setSelectedCheckItem] = useState<{ hostIp: string; checkKey: string; checkName: string } | null>(null)

  // 当对话框打开时，重置所有状态
  useEffect(() => {
    if (open) {
      console.log('🔄 环境检查对话框打开，重置所有状态')
      // 重置所有检查相关的状态
      setCheckStatus([])
      setIsChecking(false)
      setIsPaused(false)
      setExpandedHosts(new Set())
      setExpandedCheckItems(new Set())
      
      // 初始化主机列表
      if (hostList && hostList.length > 0) {
        const hosts = hostList.map((h: any) => ({
          ip: h.ip,
          hostname: h.hostname || h.ip
        }))
        setActualHostList(hosts)
        console.log('✅ 主机列表已初始化:', hosts)
      } else {
        setActualHostList([])
      }
    } else {
      // 对话框关闭时，清理SSE连接
      setEventSource(prev => {
        if (prev) {
          prev.close()
        }
        return null
      })
    }
  }, [open, hostList])

  // 启动环境检查
  const handleStartCheck = async () => {
    try {
      // 使用最新获取的主机列表
      const hostsToCheck = actualHostList.length > 0 ? actualHostList : hostList
      
      if (!hostsToCheck || hostsToCheck.length === 0) {
        alert('没有可检查的主机')
        return
      }
      
      console.log('🚀 准备调用环境检查API:', {
        api: 'clusterApiV1.environmentCheck.start',
        path: API_PATHS_V1.ENVIRONMENT_CHECK_START,
        hostIps: hostsToCheck.map(h => h.ip),
        connectionParams
      })
      
      // 先调用启动检查API
      const response = await clusterApiV1.environmentCheck.start({
        hostIps: hostsToCheck.map(h => h.ip),
        connectionParams
      })
      
      console.log('✅ 环境检查API响应:', response)
      
      if (response.code === 200) {
        console.log('环境检查已启动，任务ID:', response.data)
        // 启动成功后，设置检查状态，触发SSE连接
        setIsChecking(true)
      } else {
        throw new Error(response.msg || '启动检查失败')
      }
    } catch (error: any) {
      console.error('启动环境检查失败:', error)
      alert('启动环境检查失败: ' + (error.message || '未知错误'))
      setIsChecking(false)
    }
  }

  // SSE连接，接收实时进度
  useEffect(() => {
    if (!isChecking || !cluster?.id) return

    // 建立SSE连接
    const clusterId = cluster.id
    const sseUrl = `${API_BASE_URL}${API_PATHS_V1.ENVIRONMENT_CHECK_SSE}/${clusterId}`
    
    console.log('建立SSE连接:', sseUrl)
    const es = new EventSource(sseUrl, { withCredentials: true })

    es.addEventListener('connected', (event: any) => {
      console.log('SSE连接成功:', event.data)
    })

    es.addEventListener('progress', (event: any) => {
      try {
        const data = JSON.parse(event.data)
        if (data.type === 'progress' && data.data) {
          setCheckStatus(data.data)
        }
      } catch (error) {
        console.error('解析SSE消息失败:', error)
      }
    })

    es.onerror = (error) => {
      console.error('SSE连接错误:', error)
      es.close()
      setEventSource(null)
    }

    setEventSource(es)

    return () => {
      console.log('关闭SSE连接')
      es.close()
    }
  }, [isChecking, cluster?.id])

  // 切换主机展开/收起
  const toggleHostExpand = (hostIp: string) => {
    setExpandedHosts(prev => {
      const next = new Set(prev)
      if (next.has(hostIp)) {
        next.delete(hostIp)
      } else {
        next.add(hostIp)
      }
      return next
    })
  }

  // 切换检查项展开/收起
  const toggleCheckItemExpand = (hostIp: string, checkKey: string) => {
    const key = `${hostIp}-${checkKey}`
    setExpandedCheckItems(prev => {
      const next = new Set(prev)
      if (next.has(key)) {
        next.delete(key)
      } else {
        next.add(key)
      }
      return next
    })
  }

  // 检查检查项是否展开
  const isCheckItemExpanded = (hostIp: string, checkKey: string) => {
    return expandedCheckItems.has(`${hostIp}-${checkKey}`)
  }

  // 查看日志
  const handleViewLogs = (hostIp: string, checkKey: string, checkName: string) => {
    setSelectedCheckItem({ hostIp, checkKey, checkName })
    setLogsDialogOpen(true)
  }

  // 跳过检查项
  const handleSkipItem = async (hostIp: string, checkKey: string) => {
    try {
      await clusterApiV1.environmentCheck.skipItem({
        hostIp,
        checkItemKey: checkKey,
        reason: '用户手动跳过'
      })
      console.log(`已跳过检查项: ${hostIp} - ${checkKey}`)
    } catch (error: any) {
      console.error('跳过检查项失败:', error)
      alert('跳过失败: ' + (error.message || '未知错误'))
    }
  }

  // 修复检查项
  const handleRepairItem = async (hostIp: string, checkKey: string) => {
    try {
      const result = await clusterApiV1.environmentCheck.repairItem({
        hostIp,
        checkItemKey: checkKey,
        repairParams: {}
      })
      console.log('修复结果:', result)
      
      if (result.success) {
        alert('修复成功！')
      } else {
        alert('修复失败: ' + result.message)
      }
    } catch (error: any) {
      console.error('修复检查项失败:', error)
      alert('修复失败: ' + (error.message || '未知错误'))
    }
  }

  // 暂停检查
  const handlePause = async () => {
    try {
      await clusterApiV1.environmentCheck.pause()
      setIsPaused(true)
    } catch (error: any) {
      console.error('暂停失败:', error)
    }
  }

  // 恢复检查
  const handleResume = async () => {
    try {
      await clusterApiV1.environmentCheck.resume()
      setIsPaused(false)
    } catch (error: any) {
      console.error('恢复失败:', error)
    }
  }

  // 获取状态图标
  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'SUCCESS':
        return <CheckCircle2 className="h-5 w-5 text-green-500" />
      case 'FAILED':
        return <XCircle className="h-5 w-5 text-red-500" />
      case 'RUNNING':
        return <Loader2 className="h-5 w-5 text-blue-500 animate-spin" />
      case 'SKIPPED':
        return <SkipForward className="h-5 w-5 text-gray-400" />
      case 'PENDING':
      default:
        return <Clock className="h-5 w-5 text-gray-300" />
    }
  }

  // 获取状态徽章
  const getStatusBadge = (status: string) => {
    const variants: Record<string, any> = {
      'SUCCESS': { variant: 'default', className: 'bg-green-500' },
      'PARTIAL_SUCCESS': { variant: 'default', className: 'bg-yellow-500' },
      'FAILED': { variant: 'destructive' },
      'RUNNING': { variant: 'default', className: 'bg-blue-500' },
      'PENDING': { variant: 'secondary' }
    }
    
    const config = variants[status] || variants['PENDING']
    
    return (
      <Badge {...config}>
        {status === 'PARTIAL_SUCCESS' ? '部分通过' : 
         status === 'SUCCESS' ? '通过' :
         status === 'FAILED' ? '失败' :
         status === 'RUNNING' ? '检查中' : '待检查'}
      </Badge>
    )
  }

  // 计算整体进度（按主机维度统计）
  const calculateOverallProgress = () => {
    if (checkStatus.length === 0) {
      // 如果还没有检查状态，但有主机列表，显示主机数量
      return { 
        totalHosts: actualHostList.length, 
        completedHosts: 0, 
        successHosts: 0, 
        partialSuccessHosts: 0,
        failedHosts: 0 
      }
    }
    
    const totalHosts = checkStatus.length
    const completedHosts = checkStatus.filter(h => 
      h.overallStatus === 'SUCCESS' || 
      h.overallStatus === 'PARTIAL_SUCCESS' || 
      h.overallStatus === 'FAILED'
    ).length
    const successHosts = checkStatus.filter(h => h.overallStatus === 'SUCCESS').length
    const partialSuccessHosts = checkStatus.filter(h => h.overallStatus === 'PARTIAL_SUCCESS').length
    const failedHosts = checkStatus.filter(h => h.overallStatus === 'FAILED').length
    
    return { totalHosts, completedHosts, successHosts, partialSuccessHosts, failedHosts }
  }

  // 判断是否可以进入下一步
  const canProceed = () => {
    if (checkStatus.length === 0) return false
    
    // 所有主机的所有检查项都必须是成功或已跳过
    return checkStatus.every(host => {
      // 检查所有检查项是否都已解决（成功或跳过）
      const allItemsResolved = host.checkItems.every(item => 
        item.status === 'SUCCESS' || item.status === 'SKIPPED'
      )
      
      return allItemsResolved && (
        host.overallStatus === 'SUCCESS' || 
        host.overallStatus === 'PARTIAL_SUCCESS'
      )
    })
  }

  const overallProgress = calculateOverallProgress()
  const progressPercentage = overallProgress.totalHosts > 0 
    ? Math.round((overallProgress.completedHosts / overallProgress.totalHosts) * 100)
    : 0

  // 创建统一的ActionBar
  const actionBar = (
    <ClusterWizardActionBar
      statusInfo={{
        text: `主机检查进度 (${overallProgress.completedHosts}/${overallProgress.totalHosts} 台)`,
        value: overallProgress.completedHosts,
        total: overallProgress.totalHosts,
        pulse: isChecking
      }}
      buttons={[
        {
          text: "上一步",
          onClick: onPrevious,
          variant: 'secondary' as const,
          disabled: isChecking
        },
        {
          text: "下一步：Agent分发",
          onClick: onNext,
          disabled: !canProceed(),
          loading: false
        }
      ]}
    />
  )

  return (
    <>
    <ClusterWizardLayout
      open={open}
      onClose={() => onOpenChange(false)}
      clusterName={cluster?.clusterName || ''}
      clusterType={cluster?.depType || 'PVM'}
      stepTitle="环境检查"
      stepDescription="环境检查 - 检查主机CPU、内存、JDK、防火墙等环境配置，确保满足大数据服务部署要求"
      currentStep={3}
      dialogTitle={`环境检查 - ${cluster?.clusterName || ''}`}
      actionBar={actionBar}
    >
      <div className="flex-1 overflow-y-auto bg-gradient-to-b from-white to-slate-50/50 min-h-0 [&::-webkit-scrollbar]:w-2 [&::-webkit-scrollbar-track]:bg-transparent [&::-webkit-scrollbar-thumb]:bg-indigo-200/60 [&::-webkit-scrollbar-thumb]:rounded-full [&::-webkit-scrollbar-thumb:hover]:bg-indigo-300/80 [&::-webkit-scrollbar]:transition-all">
        <div className="p-6 sm:p-8 lg:p-10">
          <div className="space-y-6">
          {/* 整体进度卡片 */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span>检查进度</span>
                  {actualHostList.length > 0 && (
                    <Badge variant="outline" className="text-xs">
                      {actualHostList.length} 台主机
                    </Badge>
                  )}
                </div>
                <div className="flex gap-2">
                  {!isChecking ? (
                    <Button 
                      onClick={handleStartCheck} 
                      size="sm"
                      disabled={actualHostList.length === 0}
                    >
                      <Play className="h-4 w-4 mr-2" />
                      开始检查
                    </Button>
                  ) : (
                    <>
                      {isPaused ? (
                        <Button onClick={handleResume} size="sm" variant="outline">
                          <Play className="h-4 w-4 mr-2" />
                          恢复
                        </Button>
                      ) : (
                        <Button onClick={handlePause} size="sm" variant="outline">
                          <Pause className="h-4 w-4 mr-2" />
                          暂停
                        </Button>
                      )}
                    </>
                  )}
                </div>
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="flex items-center justify-between text-sm">
                <span>主机检查进度: {overallProgress.completedHosts} / {overallProgress.totalHosts} 台</span>
                <span>{progressPercentage}%</span>
              </div>
              <Progress value={progressPercentage} />
              <div className="flex gap-4 text-sm">
                <span className="text-green-600">✓ 完全通过: {overallProgress.successHosts} 台</span>
                <span className="text-yellow-600">⚠ 部分通过: {overallProgress.partialSuccessHosts} 台</span>
                <span className="text-red-600">✗ 未通过: {overallProgress.failedHosts} 台</span>
              </div>
            </CardContent>
          </Card>

          {/* 提示信息 */}
          {!isChecking && checkStatus.length === 0 && (
            <Alert>
              <AlertTriangle className="h-4 w-4" />
              <AlertDescription>
                点击"开始检查"按钮，系统将对所有主机进行环境检查，包括CPU、内存、JDK、防火墙等配置项。
              </AlertDescription>
            </Alert>
          )}

          {/* 当checkStatus为空时显示初始主机列表 */}
          {checkStatus.length === 0 && actualHostList.length > 0 && actualHostList.map((host) => (
            <Card key={host.ip} className="border-2">
              <CardHeader>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div>
                      <CardTitle className="text-lg">
                        {host.hostname || host.ip}
                      </CardTitle>
                      <p className="text-sm text-gray-500">{host.ip}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant="outline" className={isChecking ? "text-blue-500" : "text-gray-500"}>
                      <Clock className="h-3 w-3 mr-1" />
                      {isChecking ? '检查中...' : '待检查'}
                    </Badge>
                  </div>
                </div>
              </CardHeader>
            </Card>
          ))}

          {/* 主机检查列表（检查中或已完成） */}
          {checkStatus.map((host) => (
            <Card key={host.hostIp} className="border-2">
              <CardHeader 
                className="cursor-pointer hover:bg-gray-50 transition-colors"
                onClick={() => toggleHostExpand(host.hostIp)}
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    {expandedHosts.has(host.hostIp) ? (
                      <ChevronUp className="h-5 w-5 text-gray-400" />
                    ) : (
                      <ChevronDown className="h-5 w-5 text-gray-400" />
                    )}
                    <div>
                      <CardTitle className="text-lg">
                        {host.hostname || host.hostIp}
                      </CardTitle>
                      <p className="text-sm text-gray-500">{host.hostIp}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-4">
                    {getStatusBadge(host.overallStatus)}
                    <div className="text-sm text-gray-600">
                      检查项: {host.completedItems} / {host.totalItems}
                    </div>
                    <Progress 
                      value={host.totalItems > 0 ? (host.completedItems / host.totalItems) * 100 : 0} 
                      className="w-32"
                    />
                  </div>
                </div>
              </CardHeader>

              {/* 展开显示检查项 */}
              {expandedHosts.has(host.hostIp) && (
                <CardContent className="space-y-2 pt-0">
                  {host.errorMessage && (
                    <Alert variant="destructive">
                      <AlertDescription>{host.errorMessage}</AlertDescription>
                    </Alert>
                  )}
                  
                  {host.checkItems
                    .sort((a, b) => a.priority - b.priority)
                    .map((item) => {
                      const isSkipped = item.status === 'SKIPPED'
                      
                      return (
                        <Popover key={item.checkKey}>
                          <PopoverTrigger asChild>
                            <div 
                              className={`
                                p-3 rounded-lg border cursor-pointer hover:shadow-md transition-all
                                ${isSkipped ? 'opacity-50 bg-gray-100' : ''}
                                ${item.status === 'FAILED' ? 'border-red-200 bg-red-50' : ''}
                                ${item.status === 'SUCCESS' ? 'border-green-200 bg-green-50' : ''}
                                ${item.status === 'RUNNING' ? 'border-blue-200 bg-blue-50' : ''}
                              `}
                            >
                              <div className="flex items-center justify-between">
                                <div className="flex items-center gap-2">
                                  {getStatusIcon(item.status)}
                                  <span className="font-medium">{item.displayName}</span>
                                  {item.status === 'FAILED' && (
                                    <Badge variant="destructive" className="text-xs">失败</Badge>
                                  )}
                                  {item.status === 'SKIPPED' && (
                                    <Badge variant="secondary" className="text-xs">已跳过</Badge>
                                  )}
                                </div>
                                
                                {!isSkipped && item.status === 'FAILED' && (
                                  <div className="flex gap-2" onClick={(e) => e.stopPropagation()}>
                                    {item.canRepair && (
                                      <Button 
                                        size="sm" 
                                        onClick={() => handleRepairItem(host.hostIp, item.checkKey)}
                                      >
                                        <Wrench className="h-4 w-4 mr-1" />
                                        修复
                                      </Button>
                                    )}
                                    {item.canSkip && (
                                      <Button 
                                        size="sm" 
                                        variant="outline" 
                                        onClick={() => handleSkipItem(host.hostIp, item.checkKey)}
                                      >
                                        <SkipForward className="h-4 w-4 mr-1" />
                                        忽略
                                      </Button>
                                    )}
                                  </div>
                                )}
                              </div>
                            </div>
                          </PopoverTrigger>
                          
                          <PopoverContent className="w-96">
                            <CheckItemDetailCard 
                              checkKey={item.checkKey}
                              checkResult={{
                                message: item.message,
                                details: item.checkResult || {},
                                recommendation: item.recommendation
                              }}
                              status={item.status}
                              onViewLogs={() => handleViewLogs(host.hostIp, item.checkKey, item.displayName)}
                            />
                          </PopoverContent>
                        </Popover>
                      )
                    })}
                </CardContent>
              )}
            </Card>
          ))}
          </div>
        </div>
      </div>
    </ClusterWizardLayout>
    
    {/* 日志查看对话框 */}
    {selectedCheckItem && (
      <CheckLogsDialog
        open={logsDialogOpen}
        onOpenChange={setLogsDialogOpen}
        hostIp={selectedCheckItem.hostIp}
        checkKey={selectedCheckItem.checkKey}
        checkName={selectedCheckItem.checkName}
      />
    )}
    </>
  )
}

