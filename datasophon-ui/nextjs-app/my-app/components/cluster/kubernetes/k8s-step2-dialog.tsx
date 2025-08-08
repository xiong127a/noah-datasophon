"use client"

import React, { useState, useEffect } from 'react'
import { 
  ChevronLeft, ChevronRight, CheckCircle, Loader2, RefreshCw,
  AlertCircle, Info, Clock, Network, Server, Activity
} from 'lucide-react'
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { clusterApi } from "@/lib/api"
import { toast } from 'sonner'
import ClusterWizardSidebar from '../common/cluster-wizard-sidebar'
import { getStepsByType, StepsType } from '@/lib/cluster-steps'
import { DIALOG_STYLES } from '../common/shared-styles'
import type { K8sStep1Data, K8sClusterInfo } from './k8s-step1-dialog'

// K8S节点信息接口
export interface K8sNode {
  name: string
  status: 'Ready' | 'NotReady' | 'Unknown'
  role: string
  age: string
  version: string
  internalIP: string
  externalIP?: string
  os: string
  arch: string
  cpuCapacity: string
  memoryCapacity: string
}

// K8S Step2弹窗属性接口
export interface K8sStep2DialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  cluster: K8sClusterInfo | null
  step1Data: K8sStep1Data
  onSuccess: (data?: Record<string, unknown>) => void
  onPrevious: () => void
}

export default function K8sStep2Dialog({
  open,
  onOpenChange,
  cluster,
  step1Data,
  onSuccess,
  onPrevious
}: K8sStep2DialogProps) {
  const [loading, setLoading] = useState(false)
  const [nodes, setNodes] = useState<K8sNode[]>([])
  const [checkStatus, setCheckStatus] = useState<'idle' | 'checking' | 'completed' | 'failed'>('idle')

  const steps = getStepsByType('kubernetes' as StepsType)
  const currentStep = 2

  // 清空数据
  const clearData = () => {
    setNodes([])
    setCheckStatus('idle')
  }

  // 监听弹窗关闭，清空数据
  useEffect(() => {
    if (!open) {
      clearData()
    }
  }, [open])

  // 监听弹窗打开，自动开始检查
  useEffect(() => {
    if (open && step1Data.kubeConfigContent) {
      handleCheckNodes()
    }
  }, [open, step1Data.kubeConfigContent])

  // 检查Kubernetes节点
  const handleCheckNodes = async () => {
    if (!step1Data.kubeConfigContent) {
      toast.error('缺少Kubernetes配置信息')
      return
    }

    setLoading(true)
    setCheckStatus('checking')
    
    try {
      // 调用API获取K8S节点信息
      const response = await clusterApi.kubernetes.getNodes({
        kubeconfig: step1Data.kubeConfigContent,
        namespace: step1Data.namespace
      })

      if (response.success && response.data) {
        const nodeList = response.data.nodes || []
        setNodes(nodeList)
        setCheckStatus('completed')
        
        const readyNodes = nodeList.filter(node => node.status === 'Ready')
        toast.success(`成功检测到 ${nodeList.length} 个节点，其中 ${readyNodes.length} 个节点就绪`)
      } else {
        console.error('获取节点信息失败:', response.message)
        toast.error(`节点检查失败: ${response.message || '无法连接到Kubernetes集群'}`)
        setCheckStatus('failed')
      }
    } catch (error) {
      console.error('检查节点异常:', error)
      toast.error('节点检查失败，请检查kubeconfig配置是否正确')
      setCheckStatus('failed')
    } finally {
      setLoading(false)
    }
  }

  // 处理下一步
  const handleNext = async () => {
    if (checkStatus !== 'completed') {
      toast.error('请先完成节点检查')
      return
    }

    const readyNodes = nodes.filter(node => node.status === 'Ready')
    if (readyNodes.length === 0) {
      toast.error('没有可用的就绪节点，无法继续')
      return
    }

    setLoading(true)
    try {
      // 传递节点信息到下一步
      onSuccess({
        nodes: nodes,
        readyNodes: readyNodes,
        kubeconfig: step1Data.kubeConfigContent,
        namespace: step1Data.namespace
      })
    } catch (error) {
      console.error('Step2处理异常:', error)
      toast.error('保存节点信息失败，请重试')
    } finally {
      setLoading(false)
    }
  }

  // 获取节点状态样式
  const getNodeStatusStyle = (status: string) => {
    switch (status) {
      case 'Ready':
        return 'bg-green-100 text-green-800 border-green-200'
      case 'NotReady':
        return 'bg-red-100 text-red-800 border-red-200'
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200'
    }
  }

  // 获取节点状态图标
  const getNodeStatusIcon = (status: string) => {
    switch (status) {
      case 'Ready':
        return <CheckCircle className="w-4 h-4 text-green-600" />
      case 'NotReady':
        return <AlertCircle className="w-4 h-4 text-red-600" />
      default:
        return <Clock className="w-4 h-4 text-gray-600" />
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className={DIALOG_STYLES.content}>
        <DialogTitle className="sr-only">
          Kubernetes节点验证 - {cluster?.clusterName}
        </DialogTitle>
        
        <div className="flex h-full max-h-[min(calc(100vh-96px),900px)] sm:max-h-[min(95vh,900px)]">
          {/* 左侧导航 */}
          <ClusterWizardSidebar 
            steps={steps}
            currentStep={currentStep}
            title="Kubernetes集群配置"
            clusterName={cluster?.clusterName || ''}
            isK8s={true}
            onClose={() => onOpenChange(false)}
          />

          {/* 右侧内容区域 */}
          <div className="flex-1 flex flex-col min-h-0">
            {/* 当前步骤标题 */}
            <div className="p-6 sm:p-8 border-b border-slate-200/70 bg-gradient-to-r from-white via-indigo-50/30 to-purple-50/30 relative">
              {/* 装饰性光效 */}
              <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/60 to-transparent"></div>
              {/* 分割线光效 */}
              <div className="absolute bottom-0 left-6 right-6 h-px bg-gradient-to-r from-transparent via-indigo-200/80 to-transparent"></div>
              <div className="flex items-center justify-between relative z-10">
                <div>
                  <h2 className="text-lg sm:text-xl lg:text-2xl font-bold text-gray-900">
                    Kubernetes节点验证
                  </h2>
                  <p className="text-gray-600 mt-1">
                    验证Kubernetes集群中的节点状态，确保可以正常部署服务
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
                  {/* 集群信息概览 */}
                  <Card className="border-0 shadow-lg bg-white/80 backdrop-blur-sm rounded-3xl">
                    <CardHeader className="pb-4">
                      <CardTitle className="text-lg flex items-center">
                        <Info className="w-5 h-5 mr-2 text-blue-600" />
                        集群信息
                      </CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                        <div className="text-center">
                          <div className="text-2xl font-bold text-gray-900">{cluster?.clusterName}</div>
                          <div className="text-sm text-gray-600">集群名称</div>
                        </div>
                        <div className="text-center">
                          <div className="text-2xl font-bold text-blue-600">{step1Data.namespace}</div>
                          <div className="text-sm text-gray-600">命名空间</div>
                        </div>
                        <div className="text-center">
                          <div className="text-2xl font-bold text-purple-600">{step1Data.clusterVersion || 'Unknown'}</div>
                          <div className="text-sm text-gray-600">K8S版本</div>
                        </div>
                        <div className="text-center">
                          <div className="text-2xl font-bold text-green-600">{nodes.filter(n => n.status === 'Ready').length}</div>
                          <div className="text-sm text-gray-600">就绪节点</div>
                        </div>
                      </div>
                    </CardContent>
                  </Card>

                  {/* 节点检查状态 */}
                  <Card className="border-0 shadow-lg bg-white/80 backdrop-blur-sm rounded-3xl">
                    <CardHeader className="pb-4">
                      <CardTitle className="text-lg flex items-center justify-between">
                        <div className="flex items-center">
                          <Network className="w-5 h-5 mr-2 text-indigo-600" />
                          节点检查
                        </div>
                        <Button
                          onClick={handleCheckNodes}
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
                          {loading ? '检查中' : '重新检查'}
                        </Button>
                      </CardTitle>
                    </CardHeader>
                    <CardContent>
                      {checkStatus === 'idle' && (
                        <div className="text-center py-8">
                          <Clock className="mx-auto w-12 h-12 text-gray-400 mb-4" />
                          <p className="text-gray-600">准备检查Kubernetes节点...</p>
                        </div>
                      )}

                      {checkStatus === 'checking' && (
                        <div className="text-center py-8">
                          <Loader2 className="mx-auto w-12 h-12 text-blue-600 mb-4 animate-spin" />
                          <p className="text-gray-600">正在检查节点状态...</p>
                        </div>
                      )}

                      {checkStatus === 'failed' && (
                        <div className="text-center py-8">
                          <AlertCircle className="mx-auto w-12 h-12 text-red-500 mb-4" />
                          <p className="text-red-600 mb-4">节点检查失败</p>
                          <Button onClick={handleCheckNodes} variant="outline">
                            <RefreshCw className="w-4 h-4 mr-2" />
                            重试检查
                          </Button>
                        </div>
                      )}

                      {checkStatus === 'completed' && nodes.length > 0 && (
                        <div className="space-y-4">
                          <div className="flex items-center justify-between">
                            <h4 className="font-medium text-gray-900">
                              检测到 {nodes.length} 个Kubernetes节点
                            </h4>
                            <Badge className="bg-green-100 text-green-800">
                              检查完成
                            </Badge>
                          </div>
                          
                          <div className="space-y-3">
                            {nodes.map((node, index) => (
                              <div key={index} className="p-4 border rounded-xl bg-gray-50/50">
                                <div className="flex items-center justify-between mb-2">
                                  <div className="flex items-center space-x-2">
                                    {getNodeStatusIcon(node.status)}
                                    <span className="font-medium text-gray-900">{node.name}</span>
                                    <Badge className={getNodeStatusStyle(node.status)}>
                                      {node.status}
                                    </Badge>
                                    {node.role && (
                                      <Badge variant="outline" className="text-xs">
                                        {node.role}
                                      </Badge>
                                    )}
                                  </div>
                                  <span className="text-xs text-gray-500">{node.version}</span>
                                </div>
                                
                                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                                  <div>
                                    <span className="text-gray-600">内网IP:</span>
                                    <span className="ml-1 font-mono">{node.internalIP}</span>
                                  </div>
                                  {node.externalIP && (
                                    <div>
                                      <span className="text-gray-600">外网IP:</span>
                                      <span className="ml-1 font-mono">{node.externalIP}</span>
                                    </div>
                                  )}
                                  <div>
                                    <span className="text-gray-600">操作系统:</span>
                                    <span className="ml-1">{node.os}/{node.arch}</span>
                                  </div>
                                  <div>
                                    <span className="text-gray-600">资源:</span>
                                    <span className="ml-1">{node.cpuCapacity} CPU, {node.memoryCapacity}</span>
                                  </div>
                                </div>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}

                      {checkStatus === 'completed' && nodes.length === 0 && (
                        <div className="text-center py-8">
                          <Server className="mx-auto w-12 h-12 text-gray-400 mb-4" />
                          <p className="text-gray-600">未检测到任何节点</p>
                        </div>
                      )}
                    </CardContent>
                  </Card>

                  {/* 统计信息 */}
                  {checkStatus === 'completed' && nodes.length > 0 && (
                    <Card className="border-0 shadow-lg bg-white/80 backdrop-blur-sm rounded-3xl">
                      <CardHeader className="pb-4">
                        <CardTitle className="text-lg flex items-center">
                          <Activity className="w-5 h-5 mr-2 text-purple-600" />
                          集群统计
                        </CardTitle>
                      </CardHeader>
                      <CardContent>
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                          <div className="text-center p-4 bg-green-50 rounded-xl">
                            <div className="text-2xl font-bold text-green-600">
                              {nodes.filter(n => n.status === 'Ready').length}
                            </div>
                            <div className="text-sm text-green-700">就绪节点</div>
                          </div>
                          <div className="text-center p-4 bg-red-50 rounded-xl">
                            <div className="text-2xl font-bold text-red-600">
                              {nodes.filter(n => n.status === 'NotReady').length}
                            </div>
                            <div className="text-sm text-red-700">未就绪节点</div>
                          </div>
                          <div className="text-center p-4 bg-blue-50 rounded-xl">
                            <div className="text-2xl font-bold text-blue-600">
                              {nodes.filter(n => n.role?.includes('master') || n.role?.includes('control-plane')).length}
                            </div>
                            <div className="text-sm text-blue-700">主节点</div>
                          </div>
                          <div className="text-center p-4 bg-purple-50 rounded-xl">
                            <div className="text-2xl font-bold text-purple-600">
                              {nodes.filter(n => n.role?.includes('worker') || (!n.role?.includes('master') && !n.role?.includes('control-plane'))).length}
                            </div>
                            <div className="text-sm text-purple-700">工作节点</div>
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  )}
                </div>
              </div>
            </div>

            {/* 底部按钮 */}
            <div className="p-6 sm:p-8 border-t border-slate-200/50 bg-white/90 backdrop-blur-sm relative">
              {/* 装饰性光效 */}
              <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/80 to-transparent"></div>
              {/* 顶部分割线光效 */}
              <div className="absolute top-0 left-6 right-6 h-px bg-gradient-to-r from-transparent via-indigo-200/60 to-transparent"></div>
              <div className="flex justify-between space-x-4 relative z-10">
                <Button
                  onClick={onPrevious}
                  variant="outline"
                  className="px-6 py-3 rounded-2xl"
                >
                  <ChevronLeft className="w-4 h-4 mr-2" />
                  上一步
                </Button>
                
                <Button
                  onClick={handleNext}
                  disabled={loading || checkStatus !== 'completed' || nodes.filter(n => n.status === 'Ready').length === 0}
                  className="px-8 py-3 bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-700 hover:to-purple-700 text-white font-semibold rounded-2xl shadow-lg hover:shadow-xl transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed relative overflow-hidden group"
                >
                  {loading ? (
                    <>
                      <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                      处理中...
                    </>
                  ) : (
                    <>
                      下一步
                      <ChevronRight className="w-4 h-4 ml-2 group-hover:translate-x-1 transition-transform duration-300" />
                    </>
                  )}
                  {/* 按钮光效 */}
                  <div className="absolute inset-0 bg-gradient-to-r from-white/20 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
                </Button>
              </div>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}
