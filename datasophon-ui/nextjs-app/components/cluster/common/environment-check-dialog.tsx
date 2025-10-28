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
  SkipForward,
  Wrench,
  FileText,
  Globe
} from 'lucide-react'
import { clusterApiV1 } from '@/lib/api-utils-v1'
import { API_BASE_URL, API_PATHS_V1 } from '@/lib/api-config-v1'
import ClusterWizardLayout from './cluster-wizard-layout'
import ClusterWizardActionBar from './cluster-wizard-action-bar'
import { CheckItemDetailCard } from './check-item-detail-card'
import { CheckLogsDialog } from './check-logs-dialog'
import { HostLogsDialog } from './host-logs-dialog'
import { RepairOptionsDialog, type RepairOptions } from './repair-options-dialog'
import HostnameBatchEditDialog from './hostname-batch-edit-dialog'
import HostsFileSyncDialog from './hosts-file-sync-dialog'
import { GlobalCheckDetails } from './global-check-details'

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
  const [expandedHosts, setExpandedHosts] = useState<Set<string>>(new Set())
  const [actualHostList, setActualHostList] = useState<Array<{ ip: string; hostname?: string }>>([])
  const [logsDialogOpen, setLogsDialogOpen] = useState(false)
  const [selectedCheckItem, setSelectedCheckItem] = useState<{ hostIp: string; checkKey: string; checkName: string } | null>(null)
  const [hostLogsDialogOpen, setHostLogsDialogOpen] = useState(false)
  const [selectedHost, setSelectedHost] = useState<{ hostIp: string; hostname: string } | null>(null)
  const [repairOptionsOpen, setRepairOptionsOpen] = useState(false)
  const [pendingRepair, setPendingRepair] = useState<{ hostIp: string; checkKey: string; checkName: string } | null>(null)
  const [validation, setValidation] = useState<{
    canProceed: boolean
    reason?: string
    summary?: any
  } | null>(null)
  
  // 全局检查相关状态
  const [globalCheckResults, setGlobalCheckResults] = useState<any[]>([])
  
  // 主机管理对话框状态
  const [hostnameEditDialogOpen, setHostnameEditDialogOpen] = useState(false)
  const [hostsFileSyncDialogOpen, setHostsFileSyncDialogOpen] = useState(false)

  // 当对话框打开时，重置所有状态
  useEffect(() => {
    if (open) {
      console.log('🔄 环境检查对话框打开，重置所有状态')
      // 重置所有检查相关的状态
      setCheckStatus([])
      setIsChecking(false)
      setExpandedHosts(new Set())
      
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

  // 重新检查（检查完成后可以点击）
  const handleRestartCheck = async () => {
    try {
      const hostsToCheck = actualHostList.length > 0 ? actualHostList : hostList
      
      if (!hostsToCheck || hostsToCheck.length === 0) {
        alert('没有可检查的主机')
        return
      }
      
      console.log('🔄 重新启动环境检查')
      
      // 重置状态
      setCheckStatus([])
      setValidation(null)
      
      // 调用重新检查API（会自动清理旧数据）
      const response = await clusterApiV1.environmentCheck.restart({
        hostIps: hostsToCheck.map(h => h.ip),
        connectionParams
      })
      
      console.log('✅ 重新检查API响应:', response)
      
      if (response.code === 200) {
        console.log('环境检查已重新启动，任务ID:', response.data)
        setIsChecking(true)
      } else {
        throw new Error(response.msg || '重新启动检查失败')
      }
    } catch (error: any) {
      console.error('重新启动环境检查失败:', error)
      alert('重新启动环境检查失败: ' + (error.message || '未知错误'))
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
          
          // 同时更新验证结果（不再需要额外 API 调用）
          if (data.validation) {
            setValidation(data.validation)
            console.log('验证结果已更新:', data.validation)
          }
          
          // 检测是否所有主机都已完成检查（状态不是RUNNING）
          const allHostsCompleted = data.data.every((host: any) => 
            host.overallStatus !== 'RUNNING'
          )
          
          if (allHostsCompleted && data.data.length > 0) {
            console.log('✅ 所有主机检查已完成，停止检查状态')
            setIsChecking(false)
          }
        }
      } catch (error) {
        console.error('解析SSE消息失败:', error)
      }
    })

    es.onerror = (error) => {
      console.error('SSE连接错误:', error)
      es.close()
    }

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

  // 查看日志
  const handleViewLogs = (hostIp: string, checkKey: string, checkName: string) => {
    setSelectedCheckItem({ hostIp, checkKey, checkName })
    setLogsDialogOpen(true)
  }
  
  // 加载全局检查结果（提取为独立函数，可被多处调用）
  const loadGlobalCheckResults = async () => {
    try {
      console.log('🌐 加载全局检查结果...')
      const response = await clusterApiV1.environmentCheck.getGlobalCheckResults()
      if (response.code === 200 && response.data) {
        setGlobalCheckResults(response.data)
        console.log('✅ 全局检查结果已加载:', response.data)
      } else {
        console.log('⚠️ 暂无全局检查结果')
      }
    } catch (error) {
      console.error('❌ 加载全局检查结果失败:', error)
    }
  }
  
  // 自动加载全局检查结果（当检查停止后）
  useEffect(() => {
    if (!isChecking && checkStatus.length > 0) {
      // 检查停止，自动加载全局检查结果
      const loadGlobalResults = async () => {
        await loadGlobalCheckResults()
      }
      
      // 延迟1.5秒加载，给后端足够时间执行全局检查
      const timer = setTimeout(loadGlobalResults, 1500)
      return () => clearTimeout(timer)
    }
  }, [isChecking, checkStatus.length])

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

  // 刷新检查状态（修复成功后调用）
  const refreshCheckStatus = async () => {
    try {
      console.log('刷新检查状态...')
      const response = await clusterApiV1.environmentCheck.getStatus()
      if (response && Array.isArray(response)) {
        setCheckStatus(response)
        console.log('检查状态已更新:', response)
        
        // 同时刷新验证结果（SSE 可能断开时使用）
        const validationResponse = await clusterApiV1.environmentCheck.validate()
        if (validationResponse.code === 200) {
          setValidation(validationResponse.data)
          console.log('验证结果已更新:', validationResponse.data)
        }
      }
    } catch (error) {
      console.error('刷新检查状态失败:', error)
    }
  }
  
  // 执行修复（带参数）
  const executeRepair = async (hostIp: string, checkKey: string, checkName: string, repairParams: RepairOptions | Record<string, never>) => {
    try {
      // 1. 立即打开日志对话框，默认显示修复日志标签页
      setSelectedCheckItem({ hostIp, checkKey, checkName })
      setLogsDialogOpen(true)
      
      // 2. 调用修复API（异步执行，不阻塞）
      clusterApiV1.environmentCheck.repairItem({
        hostIp,
        checkItemKey: checkKey,
        repairParams
      }).then(result => {
        console.log('修复结果:', result)
      }).catch(error => {
        console.error('修复检查项失败:', error)
      })
    } catch (error: any) {
      console.error('修复检查项异常:', error)
    }
  }
  
  // 修复检查项（Java检查项需要选择选项）
  const handleRepairItem = async (hostIp: string, checkKey: string, checkName: string) => {
    // Java检查项需要让用户选择修复选项
    if (checkKey === 'java') {
      setPendingRepair({ hostIp, checkKey, checkName })
      setRepairOptionsOpen(true)
    } else {
      // 其他检查项直接修复
      executeRepair(hostIp, checkKey, checkName, {})
    }
  }
  
  // 确认修复选项（仅Java使用）
  const handleRepairOptionsConfirm = (options: RepairOptions) => {
    if (pendingRepair) {
      executeRepair(pendingRepair.hostIp, pendingRepair.checkKey, pendingRepair.checkName, options)
      setPendingRepair(null)
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
    // 判断主机是否完成：主机检查流程已结束（状态不是RUNNING）
    const completedHosts = checkStatus.filter(h => 
      h.overallStatus !== 'RUNNING'
    ).length
    const successHosts = checkStatus.filter(h => h.overallStatus === 'SUCCESS').length
    const partialSuccessHosts = checkStatus.filter(h => h.overallStatus === 'PARTIAL_SUCCESS').length
    const failedHosts = checkStatus.filter(h => h.overallStatus === 'FAILED').length
    
    return { totalHosts, completedHosts, successHosts, partialSuccessHosts, failedHosts }
  }

  const overallProgress = calculateOverallProgress()
  const progressPercentage = overallProgress.totalHosts > 0 
    ? Math.round((overallProgress.completedHosts / overallProgress.totalHosts) * 100)
    : 0

  // 调试：输出进度信息
  useEffect(() => {
    if (checkStatus.length > 0) {
      console.log('📊 检查进度:', {
        completedHosts: overallProgress.completedHosts,
        totalHosts: overallProgress.totalHosts,
        percentage: progressPercentage,
        hosts: checkStatus.map(h => ({
          ip: h.hostIp,
          status: h.overallStatus,
          completed: h.completedItems,
          total: h.totalItems,
          isComplete: h.completedItems === h.totalItems
        }))
      })
    }
  }, [checkStatus, overallProgress, progressPercentage])

  // 处理上一步：清理检查数据
  const handlePrevious = async () => {
    try {
      console.log('返回上一步，清理检查数据...')
      await clusterApiV1.environmentCheck.cleanup()
      console.log('检查数据已清理')
    } catch (error) {
      console.error('清理检查数据失败:', error)
    } finally {
      // 无论清理成功与否，都返回上一步
      onPrevious()
    }
  }

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
          onClick: handlePrevious,
          variant: 'secondary' as const,
          disabled: isChecking
        },
        {
          text: "下一步：Agent分发",
          onClick: onNext,
          disabled: !validation?.canProceed,
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
                  ) : overallProgress.completedHosts >= overallProgress.totalHosts && overallProgress.totalHosts > 0 ? (
                    // 检查完成，显示重新检查按钮
                    <>
                      <Badge variant="outline" className="text-green-600 border-green-600">
                        <CheckCircle2 className="h-4 w-4 mr-1" />
                        检查完成
                      </Badge>
                      <Button 
                        onClick={handleRestartCheck} 
                        size="sm"
                        variant="outline"
                      >
                        <Play className="h-4 w-4 mr-2" />
                        重新检查
                      </Button>
                    </>
                  ) : (
                    // 检查进行中，不显示暂停/恢复按钮（暂不支持暂停功能）
                    <Badge variant="outline" className="text-blue-600 border-blue-600">
                      <Loader2 className="h-4 w-4 mr-1 animate-spin" />
                      检查中...
                    </Badge>
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

          {/* 主机管理操作 - 始终显示在最顶部 */}
          <Card className="border-indigo-200 bg-gradient-to-br from-indigo-50 to-purple-50">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Wrench className="h-5 w-5 text-indigo-600" />
                主机管理操作
              </CardTitle>
              <p className="text-sm text-gray-600 mt-1">
                批量管理主机名称和hosts文件配置
              </p>
            </CardHeader>
            <CardContent>
              <div className="flex gap-3">
                <Button 
                  onClick={() => setHostnameEditDialogOpen(true)} 
                  size="sm"
                  className="bg-indigo-600 hover:bg-indigo-700"
                >
                  批量修改主机名
                </Button>
                <Button 
                  onClick={() => setHostsFileSyncDialogOpen(true)} 
                  size="sm"
                  className="bg-purple-600 hover:bg-purple-700"
                >
                  同步Hosts文件
                </Button>
              </div>
            </CardContent>
          </Card>

          {/* 第一部分：单主机检查 */}
          <div className="space-y-4 mt-6">
          {/* 提示信息 */}
          {!isChecking && checkStatus.length === 0 && (
            <Alert>
              <AlertTriangle className="h-4 w-4" />
              <AlertDescription>
                点击&ldquo;开始检查&rdquo;按钮，系统将对所有主机进行环境检查，包括CPU、内存、JDK、防火墙等配置项。
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
              <CardHeader>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3 cursor-pointer hover:opacity-80 transition-opacity flex-1"
                       onClick={() => toggleHostExpand(host.hostIp)}>
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
                      检查项: {host.successItems + host.skippedItems} / {host.totalItems}
                    </div>
                    <Progress 
                      value={host.totalItems > 0 ? ((host.successItems + host.skippedItems) / host.totalItems) * 100 : 0} 
                      className="w-32"
                    />
                    {/* 查看日志按钮 */}
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={(e) => {
                        e.stopPropagation()
                        setSelectedHost({ hostIp: host.hostIp, hostname: host.hostname })
                        setHostLogsDialogOpen(true)
                      }}
                    >
                      <FileText className="h-4 w-4 mr-1" />
                      查看日志
                    </Button>
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
                                        onClick={() => handleRepairItem(host.hostIp, item.checkKey, item.displayName)}
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

          {/* 第二部分：综合检查（检查停止后自动显示） */}
          {!isChecking && checkStatus.length > 0 && (
            <div className="space-y-4 mt-8">
              <div className="flex items-center gap-3 mb-4">
                <div className="h-px flex-1 bg-gradient-to-r from-transparent via-gray-300 to-transparent"></div>
                <div className="flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-blue-50 to-indigo-50 rounded-full border border-blue-200">
                  <Globe className="h-5 w-5 text-blue-600" />
                  <span className="font-semibold text-blue-900">综合检查</span>
                  {globalCheckResults.length > 0 && (
                    <Badge variant="outline" className="ml-1 bg-white">
                      {globalCheckResults.length}项
                    </Badge>
                  )}
                </div>
                <div className="h-px flex-1 bg-gradient-to-r from-transparent via-gray-300 to-transparent"></div>
              </div>

              <Alert className="border-blue-200 bg-blue-50">
                <AlertTriangle className="h-4 w-4 text-blue-600" />
                <AlertDescription className="text-blue-900">
                  主机检查已完成（{overallProgress.successHosts}台成功，{overallProgress.failedHosts}台失败），系统自动执行集群级别的综合检查（主机名唯一性、hosts文件一致性等）
                </AlertDescription>
              </Alert>
              
              {globalCheckResults.length > 0 && (
                <div className="space-y-4">
                  {globalCheckResults.map((result) => {
                    const isSuccess = result.status === 'SUCCESS'
                    const isFailed = result.status === 'FAILED'
                    const isWarning = result.status === 'WARNING'
                    
                    return (
                      <Card key={result.checkKey} className={`
                        ${isSuccess ? 'border-green-200 bg-green-50' : ''}
                        ${isFailed ? 'border-red-200 bg-red-50' : ''}
                        ${isWarning ? 'border-yellow-200 bg-yellow-50' : ''}
                      `}>
                        <CardHeader>
                          <div className="flex items-center justify-between">
                            <div className="flex items-center gap-2">
                              {isSuccess && <CheckCircle2 className="h-5 w-5 text-green-500" />}
                              {isFailed && <XCircle className="h-5 w-5 text-red-500" />}
                              {isWarning && <AlertTriangle className="h-5 w-5 text-yellow-500" />}
                              <CardTitle className="text-base">{result.displayName}</CardTitle>
                            </div>
                            {isSuccess && <Badge variant="outline" className="border-green-600 text-green-600">正常</Badge>}
                            {isFailed && <Badge variant="destructive">异常</Badge>}
                            {isWarning && <Badge variant="outline" className="border-yellow-600 text-yellow-600">警告</Badge>}
                          </div>
                        </CardHeader>
                        <CardContent>
                          <div className="space-y-2">
                            <p className="text-sm text-gray-700">{result.message}</p>
                            {result.recommendation && (
                              <Alert variant={isFailed ? 'destructive' : 'default'}>
                                <AlertDescription className="text-sm">
                                  <strong>建议：</strong>{result.recommendation}
                                </AlertDescription>
                              </Alert>
                            )}
                            {result.details && Object.keys(result.details).length > 0 && (
                              <details className="mt-2">
                                <summary className="text-sm font-medium cursor-pointer text-blue-600 hover:text-blue-700">
                                  查看详细信息
                                </summary>
                                <div className="mt-3">
                                  <GlobalCheckDetails 
                                    checkKey={result.checkKey} 
                                    details={result.details} 
                                  />
                                </div>
                              </details>
                            )}
                          </div>
                        </CardContent>
                      </Card>
                    )
                  })}
                </div>
              )}
              
              {globalCheckResults.length === 0 && (
                <Alert>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  <AlertDescription>
                    正在执行综合检查，请稍候...
                  </AlertDescription>
                </Alert>
              )}
            </div>
          )}
          </div>
        </div>
      </div>
    </ClusterWizardLayout>
    
    {/* 修复选项对话框（仅Java使用） */}
    {pendingRepair && (
      <RepairOptionsDialog
        open={repairOptionsOpen}
        onOpenChange={setRepairOptionsOpen}
        checkName={pendingRepair.checkName}
        onConfirm={handleRepairOptionsConfirm}
      />
    )}
    
    {/* 日志查看对话框 */}
    {selectedCheckItem && (
      <CheckLogsDialog
        open={logsDialogOpen}
        onOpenChange={setLogsDialogOpen}
        clusterId={cluster?.id || 0}
        hostIp={selectedCheckItem.hostIp}
        checkKey={selectedCheckItem.checkKey}
        checkName={selectedCheckItem.checkName}
        onRepairSuccess={() => {
          console.log('修复成功，重新加载检查结果')
          refreshCheckStatus()
        }}
      />
    )}
    
    {/* 主机日志查看对话框 */}
    {selectedHost && (
      <HostLogsDialog
        open={hostLogsDialogOpen}
        onOpenChange={setHostLogsDialogOpen}
        clusterId={cluster?.id?.toString() || '0'}
        hostIp={selectedHost.hostIp}
        hostname={selectedHost.hostname}
      />
    )}
    
    {/* 主机名批量修改对话框 */}
    <HostnameBatchEditDialog
      open={hostnameEditDialogOpen}
      onClose={() => setHostnameEditDialogOpen(false)}
      clusterId={cluster?.id?.toString() || '0'}
      hostIps={actualHostList.map(h => h.ip)}
      connectionParams={connectionParams}
      onSuccess={() => {
        console.log('主机名修改成功')
        // 可以选择刷新检查状态或重新运行全局检查
        loadGlobalCheckResults()
      }}
    />
    
    {/* Hosts文件同步对话框 */}
    <HostsFileSyncDialog
      open={hostsFileSyncDialogOpen}
      onClose={() => setHostsFileSyncDialogOpen(false)}
      clusterId={cluster?.id?.toString() || '0'}
      hostIps={actualHostList.map(h => h.ip)}
      hostList={actualHostList}
      connectionParams={connectionParams}
      onSuccess={() => {
        console.log('Hosts文件同步成功')
        // 可以选择刷新检查状态或重新运行全局检查
        loadGlobalCheckResults()
      }}
    />
    </>
  )
}

