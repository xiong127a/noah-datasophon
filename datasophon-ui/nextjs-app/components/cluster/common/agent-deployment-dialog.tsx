"use client"

import React, { useState, useEffect, useCallback, useMemo } from 'react'

/**
 * 🔧 修复Long类型精度丢失的JSON解析器
 */
function parseJsonWithLongSupport(jsonString: string): unknown {
  try {
    const safeMaxInt = Number.MAX_SAFE_INTEGER;
    const longIntRegex = /"?(-?\d{15,})"?/g;
    const processedJson = jsonString.replace(longIntRegex, (match, number) => {
      const num = Math.abs(parseInt(number));
      if (num > safeMaxInt) {
        return `"${number}"`;
      }
      return match;
    });
    return JSON.parse(processedJson);
  } catch (error) {
    console.warn('JSON解析失败，使用原生解析:', error);
    return JSON.parse(jsonString);
  }
}
import { 
  Loader2, RefreshCw, AlertCircle, Server, CheckCircle2, XCircle, Play, 
  Pause, Clock, Info, Download, Zap
} from 'lucide-react'
import { Progress } from "@/components/ui/progress"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { toast } from 'sonner'
import ClusterWizardLayout from './cluster-wizard-layout'
import ClusterWizardActionBar, { type ActionButton, type StatusInfo } from './cluster-wizard-action-bar'
import { ClusterTypeUtil } from '@/types'
import { CARD_STYLES, BADGE_STYLES } from './shared-styles'

import type { 
  AgentDeploymentDialogProps, 
  HostInfo, 
  AgentDistributionTask,
  Step3AgentData
} from '@/types/agent-deployment'

/**
 * 集群步骤3：主机Agent分发对话框
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 */

const AgentDeploymentDialog: React.FC<AgentDeploymentDialogProps> = ({
  open,
  onOpenChange,
  cluster,
  clusterType = '',
  hostList,
  connectionParams,
  step2Data,
  onComplete,
  onPrevious
}) => {
  // 状态管理
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [hosts, setHosts] = useState<HostInfo[]>([])
  const [distributionTasks, setDistributionTasks] = useState<AgentDistributionTask[]>([])
  const [isDistributing, setIsDistributing] = useState(false)
  const [overallStatus, setOverallStatus] = useState<'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED'>('NOT_STARTED')

  // 判断是否为K8s模式
  const isK8s = ClusterTypeUtil.isKubernetes(clusterType || cluster?.depType || '')

  // 初始化主机列表（从props获取，不再调用API）
  useEffect(() => {
    if (open && hostList && hostList.length > 0) {
      console.log('🔄 Agent分发对话框打开，初始化主机列表:', hostList)
      const initializedHosts: HostInfo[] = hostList.map((host: any) => ({
        id: host.id || host.ip, // 如果没有id，使用ip作为id
        hostname: host.hostname || host.ip,
        ip: host.ip,
        sshUser: connectionParams.sshUser,
        sshPort: parseInt(connectionParams.sshPort),
        agentStatus: 'NOT_INSTALLED',
        progress: 0
      }))
      setHosts(initializedHosts)
      console.log('✅ 主机列表已初始化:', initializedHosts)
    } else if (open) {
      setHosts([])
    }
  }, [open, hostList, connectionParams])

  // 开始Agent分发
  const startAgentDistribution = async () => {
    if (!cluster?.id || hosts.length === 0) return

    setIsDistributing(true)
    setOverallStatus('IN_PROGRESS')
    
    try {
      // 初始化分发任务
      const tasks: AgentDistributionTask[] = hosts.map(host => ({
        taskId: `agent-${host.id}-${Date.now()}`,
        hostId: host.id,
        hostIp: host.ip,
        status: 'PENDING',
        progress: 0,
        startTime: new Date().toISOString()
      }))
      
      setDistributionTasks(tasks)

      // 模拟分发过程（实际应该调用后端接口）
      for (let i = 0; i < tasks.length; i++) {
        const task = tasks[i]
        const host = hosts[i]
        
        // 更新任务状态为运行中
        task.status = 'RUNNING'
        setDistributionTasks([...tasks])
        
        // 更新主机Agent状态
        host.agentStatus = 'INSTALLING'
        setHosts([...hosts])

        // 模拟分发进度
        for (let progress = 0; progress <= 100; progress += 20) {
          await new Promise(resolve => setTimeout(resolve, 300))
          task.progress = progress
          host.progress = progress
          setDistributionTasks([...tasks])
          setHosts([...hosts])
        }

        // 随机模拟成功或失败（实际根据后端返回结果）
        const isSuccess = Math.random() > 0.2 // 80%成功率
        if (isSuccess) {
          task.status = 'SUCCESS'
          task.endTime = new Date().toISOString()
          host.agentStatus = 'INSTALLED'
          toast.success(`${host.hostname} Agent安装成功`)
        } else {
          task.status = 'FAILED'
          task.endTime = new Date().toISOString()
          task.errorMessage = '连接超时或认证失败'
          host.agentStatus = 'FAILED'
          host.errorMessage = '连接超时或认证失败'
          toast.error(`${host.hostname} Agent安装失败`)
        }
        
        setDistributionTasks([...tasks])
        setHosts([...hosts])
      }

      // 检查分发结果
      const successCount = tasks.filter(t => t.status === 'SUCCESS').length
      const failedCount = tasks.filter(t => t.status === 'FAILED').length
      
      if (failedCount === 0) {
        setOverallStatus('COMPLETED')
        toast.success('所有主机Agent分发完成')
      } else if (successCount > 0) {
        setOverallStatus('COMPLETED')
        toast.warning(`部分主机Agent分发完成 (成功: ${successCount}, 失败: ${failedCount})`)
      } else {
        setOverallStatus('FAILED')
        toast.error('所有主机Agent分发失败')
      }

    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '未知错误'
      setError(errorMessage)
      setOverallStatus('FAILED')
      toast.error(`Agent分发失败: ${errorMessage}`)
    } finally {
      setIsDistributing(false)
    }
  }

  // 重试失败的主机
  const retryFailedHosts = () => {
    const failedHosts = hosts.filter(h => h.agentStatus === 'FAILED')
    if (failedHosts.length > 0) {
      toast.info(`重试 ${failedHosts.length} 台失败主机的Agent分发`)
      startAgentDistribution()
    }
  }

  // 下一步处理
  const handleNext = () => {
    if (isK8s) {
      // K8s模式直接进入下一步
      const step3Data: Step3AgentData = {
        hosts: [],
        tasks: [],
        overallStatus: 'COMPLETED',
        successCount: 0,
        failedCount: 0
      }
      onComplete(step3Data)
    } else {
      // PVM模式检查分发状态
      if (overallStatus === 'NOT_STARTED') {
        toast.error('请先开始Agent分发')
        return
      }
      
      if (overallStatus === 'IN_PROGRESS') {
        toast.error('Agent分发正在进行中，请等待完成')
        return
      }

      const successCount = hosts.filter(h => h.agentStatus === 'INSTALLED').length
      const failedCount = hosts.filter(h => h.agentStatus === 'FAILED').length
      
      const step3Data: Step3AgentData = {
        hosts,
        tasks: distributionTasks,
        overallStatus,
        successCount,
        failedCount
      }
      
      onComplete(step3Data)
    }
  }

  // 统计信息
  const stats = useMemo(() => {
    if (isK8s) {
      return { total: 0, success: 0, failed: 0, pending: 0 }
    }
    
    const total = hosts.length
    const success = hosts.filter(h => h.agentStatus === 'INSTALLED').length
    const failed = hosts.filter(h => h.agentStatus === 'FAILED').length
    const pending = hosts.filter(h => h.agentStatus === 'NOT_INSTALLED' || h.agentStatus === 'INSTALLING').length

    return { total, success, failed, pending }
  }, [hosts, isK8s])

  // 状态信息配置
  const statusInfo: StatusInfo = {
    text: isK8s ? "Kubernetes模式" : "已完成",
    value: isK8s ? "无需分发" : stats.success,
    total: isK8s ? "" : `/ ${stats.total} 台主机`,
    pulse: !isK8s && isDistributing
  }

  // 按钮配置
  const buttons: ActionButton[] = [
    {
      text: "上一步",
      onClick: () => {
        if (onPrevious) {
          onPrevious()
        } else {
          onOpenChange(false)
        }
      },
      variant: "secondary"
    },
    {
      text: "下一步",
      onClick: handleNext,
      disabled: !isK8s && (overallStatus === 'IN_PROGRESS' || (overallStatus === 'NOT_STARTED' && hosts.length > 0)),
      variant: "primary"
    }
  ]

  return (
    <ClusterWizardLayout
      open={open}
      onClose={() => onOpenChange(false)}
      clusterName={cluster?.clusterName || ''}
      clusterType={clusterType}
      stepTitle="主机Agent分发"
      stepDescription={isK8s ? "主机Agent分发 - Kubernetes模式无需手动分发Agent" : "主机Agent分发 - 向选定的主机分发和安装Agent程序"}
      currentStep={4}
      dialogTitle={`主机Agent分发 - ${cluster?.clusterName}`}
      actionBar={
        <ClusterWizardActionBar
          statusInfo={statusInfo}
          statusBadge={!isK8s ? {
            text: overallStatus === 'COMPLETED' ? '分发完成' : overallStatus === 'IN_PROGRESS' ? '分发中...' : '待分发',
            variant: overallStatus === 'COMPLETED' ? 'success' : overallStatus === 'IN_PROGRESS' ? 'warning' : 'default',
            show: true
          } : undefined}
          buttons={buttons}
        />
      }
    >
      <div className="p-6 sm:p-8 flex-1 overflow-hidden">
        <div className="h-full flex flex-col">
          {/* K8s模式提示 */}
          {isK8s ? (
            <div className="flex-1 flex items-center justify-center">
              <Card className={`max-w-md mx-auto ${CARD_STYLES.base} ${CARD_STYLES.info} shadow-xl`}>
                <CardHeader className={`${CARD_STYLES.header} text-center`}>
                  <div className="w-16 h-16 bg-gradient-to-br from-blue-500 to-blue-600 rounded-2xl flex items-center justify-center mx-auto mb-4 shadow-lg">
                    <CheckCircle2 className="w-8 h-8 text-white" />
                  </div>
                  <CardTitle className={`${CARD_STYLES.title} text-center font-bold text-gray-900`}>
                    Kubernetes模式
                  </CardTitle>
                  <CardDescription className="text-gray-600">
                    无需手动分发Agent程序
                  </CardDescription>
                </CardHeader>
                <CardContent className={`${CARD_STYLES.content} text-center`}>
                  <div className="bg-white/70 backdrop-blur-sm rounded-xl p-4 border border-blue-200/30">
                    <Info className="w-6 h-6 text-blue-500 mx-auto mb-2" />
                    <p className="text-sm text-gray-700 leading-relaxed">
                      在Kubernetes集群中，Agent程序将通过DaemonSet方式自动部署到各个节点，无需手动分发安装。
                    </p>
                  </div>
                  <div className="text-xs text-gray-500">
                    点击"下一步"继续服务配置
                  </div>
                </CardContent>
              </Card>
            </div>
          ) : (
            /* PVM模式Agent分发 */
            <div className="space-y-6">
              {/* 操作栏 - 框架化样式 */}
              <div className="flex items-center justify-between bg-white/70 backdrop-blur-xl rounded-xl p-4 shadow-lg border border-white/20">
                <div className="flex items-center space-x-4">
                  <div className="w-10 h-10 bg-gradient-to-br from-emerald-500 to-green-600 rounded-xl flex items-center justify-center shadow-lg">
                    <Server className="w-5 h-5 text-white" />
                  </div>
                  <div>
                    <h3 className="text-lg font-bold text-gray-900">主机Agent分发</h3>
                    <p className="text-sm text-gray-600">
                      共 {stats.total} 台主机，成功 {stats.success} 台，失败 {stats.failed} 台
                    </p>
                  </div>
                </div>
                
                <div className="flex items-center space-x-3">
                  {overallStatus === 'COMPLETED' && stats.failed > 0 && (
                    <Button
                      onClick={retryFailedHosts}
                      disabled={isDistributing}
                      variant="outline"
                      size="sm"
                      className="text-amber-600 border-amber-200 hover:bg-amber-50"
                    >
                      <RefreshCw className="w-4 h-4 mr-2" />
                      重试失败
                    </Button>
                  )}
                  
                  <Button
                    onClick={startAgentDistribution}
                    disabled={isDistributing || hosts.length === 0 || overallStatus === 'COMPLETED'}
                    className="bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 text-white shadow-lg"
                  >
                    {isDistributing ? (
                      <>
                        <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                        分发中...
                      </>
                    ) : overallStatus === 'COMPLETED' ? (
                      <>
                        <CheckCircle2 className="w-4 h-4 mr-2" />
                        分发完成
                      </>
                    ) : (
                      <>
                        <Play className="w-4 h-4 mr-2" />
                        开始分发
                      </>
                    )}
                  </Button>
                </div>
              </div>

              {/* 主机列表 */}
              {loading ? (
                <div className="flex items-center justify-center h-64">
                  <div className="flex items-center space-x-3">
                    <Loader2 className="w-6 h-6 animate-spin text-blue-500" />
                    <span className="text-gray-600 font-medium">正在加载主机列表...</span>
                  </div>
                </div>
              ) : error ? (
                <div className="flex flex-col items-center justify-center h-64 text-red-500">
                  <AlertCircle className="w-12 h-12 mb-4" />
                  <p className="text-lg font-semibold mb-2">加载失败</p>
                  <p className="text-gray-600">{error}</p>
                  <Button 
                    onClick={fetchHosts}
                    className="mt-4"
                    variant="outline"
                  >
                    重试
                  </Button>
                </div>
              ) : hosts.length === 0 ? (
                <div className="flex flex-col items-center justify-center h-64 text-gray-500">
                  <Server className="w-12 h-12 mb-4" />
                  <p className="text-lg font-semibold">暂无主机</p>
                  <p>请先在上一步添加主机</p>
                </div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 overflow-y-auto max-h-[500px] pr-2">
                  {hosts.map((host) => (
                    <Card key={host.id} className={`${CARD_STYLES.base} bg-white/70 backdrop-blur-xl shadow-lg hover:shadow-xl transition-all duration-200`}>
                      <CardHeader className={`${CARD_STYLES.header} pb-3`}>
                        <div className="flex items-center justify-between">
                          <div className="flex items-center space-x-3">
                            <div className="w-8 h-8 bg-gradient-to-br from-gray-400 to-gray-500 rounded-lg flex items-center justify-center">
                              <Server className="w-4 h-4 text-white" />
                            </div>
                            <div>
                              <CardTitle className={`${CARD_STYLES.title} text-sm font-bold text-gray-900`}>
                                {host.hostname}
                              </CardTitle>
                              <CardDescription className="text-xs text-gray-600">
                                {host.ip}
                              </CardDescription>
                            </div>
                          </div>
                          
                          {/* Agent状态标识 */}
                          <Badge variant={
                            host.agentStatus === 'INSTALLED' ? 'default' :
                            host.agentStatus === 'INSTALLING' ? 'secondary' :
                            host.agentStatus === 'FAILED' ? 'destructive' : 'outline'
                          } className="text-xs">
                            {host.agentStatus === 'INSTALLED' && <CheckCircle2 className="w-3 h-3 mr-1" />}
                            {host.agentStatus === 'INSTALLING' && <Loader2 className="w-3 h-3 mr-1 animate-spin" />}
                            {host.agentStatus === 'FAILED' && <XCircle className="w-3 h-3 mr-1" />}
                            {host.agentStatus === 'NOT_INSTALLED' && <Clock className="w-3 h-3 mr-1" />}
                            {
                              host.agentStatus === 'INSTALLED' ? '已安装' :
                              host.agentStatus === 'INSTALLING' ? '安装中' :
                              host.agentStatus === 'FAILED' ? '失败' : '待安装'
                            }
                          </Badge>
                        </div>
                      </CardHeader>
                      
                      <CardContent className="pt-0">
                        {/* 安装进度 */}
                        {(host.agentStatus === 'INSTALLING' || host.progress! > 0) && (
                          <div className="mb-3">
                            <div className="flex items-center justify-between text-xs text-gray-600 mb-1">
                              <span>安装进度</span>
                              <span>{host.progress || 0}%</span>
                            </div>
                            <Progress 
                              value={host.progress || 0} 
                              className="h-2"
                            />
                          </div>
                        )}
                        
                        {/* 错误信息 */}
                        {host.errorMessage && (
                          <div className={`${CARD_STYLES.error} rounded-lg p-2 mt-2`}>
                            <div className="flex items-center space-x-2">
                              <AlertCircle className="w-4 h-4 text-red-500 flex-shrink-0" />
                              <span className="text-xs text-red-700">{host.errorMessage}</span>
                            </div>
                          </div>
                        )}
                        
                        {/* 主机信息 */}
                        <div className="grid grid-cols-2 gap-2 text-xs text-gray-600 mt-2">
                          {host.cpuCore && (
                            <div>CPU: {host.cpuCore}核</div>
                          )}
                          {host.memory && (
                            <div>内存: {host.memory}GB</div>
                          )}
                        </div>
                      </CardContent>
                    </Card>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </ClusterWizardLayout>
  )
}

export default AgentDeploymentDialog
