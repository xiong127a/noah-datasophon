"use client"

import React, { useState, useEffect, useRef, useMemo } from 'react'
import { 
  X, ChevronLeft, ChevronRight, Loader2, RefreshCw, Info
} from 'lucide-react'
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { clusterApi } from "@/lib/api"
import { toast } from 'sonner'
import ClusterWizardSidebar from '../common/cluster-wizard-sidebar'
import { getStepsByType, StepsType } from '@/lib/cluster-wizard-steps'
import { createClusterHeaders } from '@/lib/cluster-id-header'
import { ManagementStatus, ManagementStatusUtil, ClusterType, ClusterTypeUtil } from '@/types'

import type { 
  Host, 
  HostStatus, 
  Pagination,
  QueueStatus,
  CheckItem
} from '@/types/host-validation'

import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { DIALOG_STYLES } from '../common/shared-styles'
import type { K8sStep1Data, K8sClusterInfo } from './k8s-host-config-dialog'

// K8S Step2弹窗属性接口
export interface K8sHostValidationDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  cluster: K8sClusterInfo | null
  step1Data: K8sStep1Data
  onSuccess: (data?: Record<string, unknown>) => void
  onPrevious: () => void
}

export default function K8sHostValidationDialog({
  open,
  onOpenChange,
  cluster,
  step1Data,
  onSuccess,
  onPrevious
}: K8sHostValidationDialogProps) {
  const clusterType = ClusterType.KUBERNETES
  
  // 使用标准化的步骤配置
  const steps = getStepsByType(StepsType.NORMAL, clusterType)
  
  // 基础状态
  const [loading, setLoading] = useState(false)
  const [isRequesting, setIsRequesting] = useState(false)
  const [, setHasStartedCheck] = useState(false)
  const [dataSource, setDataSource] = useState<Host[]>([])
  const [selectedRowKeys, setSelectedRowKeys] = useState<string[]>([])
  const [, setQueueStatus] = useState<QueueStatus>({
    queueSize: 0,
    runningTasks: 0,
    processorThreadAlive: true
  })

  // 分页状态 - 默认每页20条，合理的分页选项
  const [pagination, setPagination] = useState<Pagination>({
    current: 1,
    pageSize: 20,
    total: 0,
    showSizeChanger: true,
    pageSizeOptions: ['10', '20', '50', '100'],
    showTotal: (total) => `共 ${total} 条`
  })

  // 搜索和筛选状态
  const [searchTerm, setSearchTerm] = useState('')
  const [statusFilter, setStatusFilter] = useState('all')
  const [roleFilter, setRoleFilter] = useState('all')
  
  // 后端筛选选项
  const [backendFilterOptions, setBackendFilterOptions] = useState<{
    statuses: string[]
    roles: string[]
  }>({
    statuses: [],
    roles: []
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

  // 筛选数据计算
  const filteredData = useMemo(() => {
    return dataSource.filter(host => {
      // 搜索筛选
      const searchMatch = searchTerm === '' || 
        host.hostname?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        host.ip?.toLowerCase().includes(searchTerm.toLowerCase())
      
      // 状态筛选
      const statusMatch = statusFilter === 'all' || (host.status || 'Ready') === statusFilter
      
      // 角色筛选
      const roleMatch = roleFilter === 'all' || 
        (host.roles || '<none>').includes(roleFilter)
      
      return searchMatch && statusMatch && roleMatch
    })
  }, [dataSource, searchTerm, statusFilter, roleFilter])

  // 全选功能
  const handleSelectAll = () => {
    if (ClusterTypeUtil.isKubernetes(clusterType)) {
      // K8S模式：选择所有未受管主机
      const unmanagedHostIps = filteredData
        .filter(host => 
          ManagementStatusUtil.isUnmanaged(host.managementStatus || ManagementStatus.UNMANAGED)
        )
        .map(host => host.ip)
      setSelectedRowKeys(unmanagedHostIps)
    } else {
      // PVM模式：选择所有主机
      const allHostIps = filteredData.map(host => host.ip)
      setSelectedRowKeys(allHostIps)
    }
  }

  const handleDeselectAll = () => {
    setSelectedRowKeys([])
  }

  // 计算可选择的主机数量和状态
  const selectableCount = ClusterTypeUtil.isKubernetes(clusterType) 
    ? filteredData.filter(host => 
        ManagementStatusUtil.isUnmanaged(host.managementStatus || ManagementStatus.UNMANAGED)
      ).length
    : filteredData.length

  const isAllSelected = selectedRowKeys.length === selectableCount && selectableCount > 0

  // 计算统计信息 - 基于筛选后的数据
  const managedCount = filteredData.filter(host => 
    ManagementStatusUtil.isManaged(host.managementStatus || ManagementStatus.UNMANAGED)
  ).length
  const unmanagedCount = filteredData.filter(host => 
    ManagementStatusUtil.isUnmanaged(host.managementStatus || ManagementStatus.UNMANAGED)
  ).length
  // configuringCount 已移除 - 后端内部逻辑，前端不展示
  
  // 检查是否有失败项（用于UI显示）
  const hasFailedItems = ClusterTypeUtil.isKubernetes(clusterType) 
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

      let response: {data: {code: number, data?: {hosts?: Host[], totalCount?: number, filterOptions?: {statuses: string[], roles: string[]}}, total?: number, queueStatus?: QueueStatus, msg?: string}}
      let res: {code: number, data?: {hosts?: Host[], totalCount?: number, filterOptions?: {statuses: string[], roles: string[]}}, total?: number, queueStatus?: QueueStatus, msg?: string}
      
      // 构造Step1配置数据
      const step1Config = {
        clusterType: clusterType.toString(),
        // PVM配置参数（K8S模式下为空）
        hosts: '',
        sshUser: '',
        sshPort: '',
        sshPassword: '',
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
          const resData = res.data
          // 新API返回的数据结构：res.data.hosts
          setDataSource(resData?.hosts || [])
          
          // 更新筛选选项 - 从后端返回的数据中获取
          if (resData?.filterOptions) {
            setBackendFilterOptions({
              statuses: resData.filterOptions.statuses || [],
              roles: resData.filterOptions.roles || []
            })
          }
          
          // 更新分页信息和队列状态
          if (!ClusterTypeUtil.isKubernetes(clusterType)) {
            // PVM模式使用传统的分页数据
            setPagination(prev => ({ ...prev, total: res.total || 0 }))
            if (res.queueStatus) {
              setQueueStatus(res.queueStatus)
            }
          } else {
            // K8S模式使用新API返回的totalCount
            setPagination(prev => ({ ...prev, total: resData?.totalCount || 0 }))
          }
          
          // 检查是否有主机已完成检查
          if (resData?.hosts && resData.hosts.length > 0) {
            const hasCompletedChecks = resData.hosts.some((host: Host) => {
              const checkItems = host.checkItems || []
              return checkItems.length > 0 && 
                     checkItems.some((item: CheckItem) => item.status !== 'WAITING')
            })

            if (hasCompletedChecks) {
              setHasStartedCheck(true)
            }
          }

          // K8S模式下不自动选中主机，让用户手动选择
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

  // 其他必要的函数
  const refreshK8sHosts = () => {
    console.log('refreshK8sHosts方法被调用了！')
    getEnvironmentListRef.current(true)
  }

  const getSuccessfulHosts = () => {
    return dataSource.filter((host: Host) => 
      selectedRowKeys.includes(host.ip) && ManagementStatusUtil.isUnmanaged(host.managementStatus || ManagementStatus.UNMANAGED)
    )
  }

  const saveK8sConfigAndHosts = async () => {
    try {
      if (!clusterId) {
        throw new Error('集群ID不能为空')
      }

      if (ClusterTypeUtil.isKubernetes(clusterType) && step1Data) {
        const kubeConfigParams = {
          kubeConfig: step1Data.kubeConfigContent,
          namespace: step1Data.namespace,
          customNamespace: step1Data.customNamespace
        }
        
        console.log('保存K8S配置:', kubeConfigParams)
        const configRes = await clusterApi.config.saveKubeConfig(
          clusterId,
          kubeConfigParams.kubeConfig || '',
          kubeConfigParams.namespace || '',
          kubeConfigParams.customNamespace
        )
        
        if (configRes.data?.code !== 200) {
          throw new Error('保存K8S配置失败: ' + (configRes.data?.msg || '未知错误'))
        }
      }
      
      toast.success('配置保存成功')
      return Promise.resolve()
      
    } catch (error) {
      console.error('保存K8S配置和主机列表失败:', error)
      throw error
    }
  }

  const handleNext = async () => {
    setLoading(true)
    try {
      const checkResult = await hostCheckCompleted()
      
      if (!checkResult.hostCheckCompleted) {
        toast.warning(checkResult.data || '存在未校验成功的主机')
        return
      }
      
      await saveK8sConfigAndHosts()
      
      toast.success('Step2 校验完成，进入下一步')
      
      onSuccess?.({
        step1Data,
        selectedHosts: getSuccessfulHosts(),
        clusterType: clusterType.toString(),
        timestamp: new Date().toISOString()
      })
      
    } catch (error) {
      console.error('下一步处理失败:', error)
      toast.error('保存配置失败，请重试')
    } finally {
      setLoading(false)
    }
  }

  const hostCheckCompleted = async (): Promise<{ hostCheckCompleted: boolean; data: string }> => {
    if (ClusterTypeUtil.isKubernetes(clusterType)) {
      try {
        const response = await clusterApi.unifiedHost.validateForNextStep({ headers: createClusterHeaders(clusterId) })
        const result = response.data
        
        if (result.code === 200 && result.data) {
          const validationResult = result.data
          return {
            hostCheckCompleted: validationResult.valid,
            data: validationResult.message || '校验完成'
          }
        } else {
          return {
            hostCheckCompleted: false,
            data: result.msg || '校验失败，请重试'
          }
        }
      } catch (error) {
        console.error('调用后端校验接口失败:', error)
        return {
          hostCheckCompleted: false,
          data: '无法连接后端进行校验，请检查网络连接'
        }
      }
    } else {
      return { hostCheckCompleted: false, data: 'PVM模式暂未实现' }
    }
  }

  const onSelectChange = (selectedKeys: string[]) => {
    setSelectedRowKeys(selectedKeys)
  }

  useEffect(() => {
    setPagination(prev => ({ ...prev, current: 1 }))
  }, [searchTerm, statusFilter, roleFilter])

  useEffect(() => {
    if (open && clusterId) {
      getEnvironmentListRef.current()
    }
  }, [open, clusterId])

  useEffect(() => {
    if (ClusterTypeUtil.isKubernetes(clusterType)) {
      if (timerRef.current) {
        clearInterval(timerRef.current)
        timerRef.current = null
      }
      return
    }

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
  }, [open, clusterId, clusterType])

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className={DIALOG_STYLES.content}>
        <DialogTitle className="sr-only">
          主机环境校验 - {cluster?.clusterName}
        </DialogTitle>
        
        <div className="flex h-full max-h-[min(calc(100vh-96px),900px)] sm:max-h-[min(95vh,900px)]">
          <ClusterWizardSidebar 
            steps={steps}
            currentStep={2}
            title="主机环境校验"
            clusterName={cluster?.clusterName || ''}
            isK8s={ClusterTypeUtil.isKubernetes(clusterType)}
            onClose={() => onOpenChange(false)}
          />

          <div className="flex-1 flex flex-col min-h-0">
            <div className="flex items-center justify-between p-6 border-b border-gray-100">
              <div>
                <h2 className="text-2xl font-semibold text-gray-900">
                  Kubernetes主机校验
                </h2>
                <p className="text-gray-600 mt-1">
                  验证Kubernetes集群中的主机状态，确保可以正常部署服务
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

            <div className="flex-1 p-4 min-h-0">
              <div className="h-full flex flex-col">
                <div className="flex-1 grid grid-cols-4 gap-6 min-h-0 max-h-[calc(100vh-200px)]">
                  <div className="col-span-3">
                    <Card className="h-full flex flex-col">
                      <CardHeader className="pb-3 flex-shrink-0">
                        <div className="flex items-center justify-between">
                          <div className="flex items-center space-x-4">
                            <CardTitle className="text-lg">
                              主机列表
                              {dataSource.length > 0 && (
                                <span className="ml-2 text-sm font-normal text-gray-500">
                                  （共 {dataSource.length} 台）
                                </span>
                              )}
                            </CardTitle>
                            <Button 
                              onClick={refreshK8sHosts}
                              disabled={loading}
                              variant="outline"
                              size="sm"
                            >
                              {loading ? (
                                <Loader2 className="w-3 h-3 mr-1.5 animate-spin" />
                              ) : (
                                <RefreshCw className="w-3 h-3 mr-1.5" />
                              )}
                              重新校验
                            </Button>
                          </div>
                            
                          {/* 全选按钮和搜索筛选栏 */}
                          <div className="flex items-center space-x-3">
                            {/* 全选按钮 */}
                            <div className="flex items-center space-x-2">
                              <button
                                onClick={isAllSelected ? handleDeselectAll : handleSelectAll}
                                disabled={selectableCount === 0}
                                className={`group relative overflow-hidden h-9 px-3 py-2 rounded-xl text-sm font-medium transition-all duration-300 shadow-sm hover:shadow-md disabled:opacity-50 disabled:cursor-not-allowed ${
                                  isAllSelected
                                    ? 'bg-gradient-to-r from-blue-500 to-blue-600 text-white hover:from-blue-600 hover:to-blue-700 border border-blue-500'
                                    : 'bg-white/90 text-gray-700 hover:bg-blue-50 border border-gray-200/80 hover:border-blue-300'
                                }`}
                              >
                                <div className="flex items-center space-x-1.5">
                                  <div className={`w-3.5 h-3.5 rounded border-2 transition-all duration-200 flex items-center justify-center ${
                                    isAllSelected 
                                      ? 'border-white bg-white/20' 
                                      : 'border-gray-400 group-hover:border-blue-500'
                                  }`}>
                                    {isAllSelected && (
                                      <svg className="w-2.5 h-2.5 text-white" fill="currentColor" viewBox="0 0 20 20">
                                        <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                                      </svg>
                                    )}
                                  </div>
                                  <span>{isAllSelected ? '取消全选' : '全选'}</span>
                                  {selectableCount > 0 && (
                                    <span className={`text-[10px] px-1.5 py-0.5 rounded-full ${
                                      isAllSelected 
                                        ? 'bg-white/20 text-white' 
                                        : 'bg-gray-100 text-gray-600'
                                    }`}>
                                      {selectableCount}
                                    </span>
                                  )}
                                </div>
                              </button>
                            </div>
                            
                            <div className="relative group">
                              <input
                                type="text"
                                placeholder="搜索主机名或IP..."
                                className="h-9 w-56 pl-10 pr-4 py-2 text-sm font-medium border-2 border-gray-200/60 rounded-2xl bg-white/95 backdrop-blur-md shadow-sm hover:shadow-xl focus:outline-none focus:ring-3 focus:ring-blue-400/25 focus:border-blue-400 hover:border-gray-300/80 focus:bg-white placeholder:text-gray-400 transition-all duration-300 ease-out group-hover:bg-white group-hover:border-gray-300 group-hover:shadow-lg"
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                              />
                              <div className="absolute left-3.5 top-1/2 transform -translate-y-1/2 pointer-events-none">
                                <svg className="w-4.5 h-4.5 text-gray-400 group-hover:text-gray-600 transition-all duration-200" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                                </svg>
                              </div>
                            </div>
                            <Select value={statusFilter} onValueChange={setStatusFilter}>
                              <SelectTrigger className="h-9 min-w-[90px] border-2 border-gray-200/60 rounded-2xl bg-white/95 backdrop-blur-md shadow-sm hover:shadow-xl focus:ring-3 focus:ring-blue-400/25 focus:border-blue-400 hover:border-gray-300/80 transition-all duration-300 ease-out">
                                <SelectValue placeholder="全部状态" />
                              </SelectTrigger>
                              <SelectContent className="rounded-2xl border-2 border-gray-200/60 bg-white/95 backdrop-blur-md shadow-xl p-2 min-w-[120px]">
                                <SelectItem value="all" className="rounded-xl mx-1 my-0.5 hover:bg-blue-50 focus:bg-blue-50 transition-colors duration-200 cursor-pointer">全部状态</SelectItem>
                                {backendFilterOptions.statuses?.map((status) => (
                                  <SelectItem key={status} value={status} className="rounded-xl mx-1 my-0.5 hover:bg-blue-50 focus:bg-blue-50 transition-colors duration-200 cursor-pointer">
                                    {status}
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                            <Select value={roleFilter} onValueChange={setRoleFilter}>
                              <SelectTrigger className="h-9 min-w-[90px] border-2 border-gray-200/60 rounded-2xl bg-white/95 backdrop-blur-md shadow-sm hover:shadow-xl focus:ring-3 focus:ring-blue-400/25 focus:border-blue-400 hover:border-gray-300/80 transition-all duration-300 ease-out">
                                <SelectValue placeholder="全部角色" />
                              </SelectTrigger>
                              <SelectContent className="rounded-2xl border-2 border-gray-200/60 bg-white/95 backdrop-blur-md shadow-xl p-2 min-w-[140px]">
                                <SelectItem value="all" className="rounded-xl mx-1 my-0.5 hover:bg-blue-50 focus:bg-blue-50 transition-colors duration-200 cursor-pointer">全部角色</SelectItem>
                                {backendFilterOptions.roles?.map((role) => (
                                  <SelectItem key={role} value={role} className="rounded-xl mx-1 my-0.5 hover:bg-blue-50 focus:bg-blue-50 transition-colors duration-200 cursor-pointer">
                                    {role === '<none>' ? '无角色' : 
                                     role === 'control-plane' ? 'Control Plane' :
                                     role === 'worker' ? 'Worker' : role}
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                          </div>
                        </div>
                      </CardHeader>
                      <CardContent className="pt-0 flex-1 flex flex-col min-h-0">
                        {loading ? (
                          <div className="flex items-center justify-center h-64">
                            <Loader2 className="w-6 h-6 animate-spin mr-2" />
                            <span>加载中...</span>
                          </div>
                        ) : dataSource.length === 0 ? (
                          <div className="flex flex-col items-center justify-center h-64 text-gray-500">
                            <div className="text-4xl mb-4">🖥️</div>
                            <div className="text-lg mb-2">暂无主机数据</div>
                            <div className="text-sm">请检查Kubernetes集群配置或点击重新校验</div>
                          </div>
                        ) : filteredData.length === 0 ? (
                          <div className="flex flex-col items-center justify-center h-64 text-gray-500">
                            <div className="text-4xl mb-4">🔍</div>
                            <div className="text-lg mb-2">没有找到匹配的主机</div>
                            <div className="text-sm">请尝试调整搜索条件或筛选条件</div>
                            <Button
                              variant="outline"
                              size="sm"
                              className="mt-4"
                              onClick={() => {
                                setSearchTerm('')
                                setStatusFilter('')
                                setRoleFilter('')
                              }}
                            >
                              清除筛选条件
                            </Button>
                          </div>
                        ) : (
                          <div className="flex-1 flex flex-col min-h-0">
                            {/* Apple风格的现代化表格设计 */}
                            <div className="flex-1 flex flex-col bg-white min-h-0">
                              
                              {/* 表格头部 - 优化列宽分配 */}
                              <div className="border-b border-gray-100 bg-gradient-to-r from-gray-50/80 to-white/80 backdrop-blur-sm flex-shrink-0">
                                <div className="grid grid-cols-12 gap-2 px-3 py-2 text-xs font-semibold text-gray-700">
                                  <div className="col-span-2 flex items-center space-x-1">
                                    <span className="w-1.5 h-1.5 rounded-full bg-blue-500"></span>
                                    <span>节点名称</span>
                                  </div>
                                  <div className="col-span-2 flex items-center space-x-1">
                                    <span className="w-1.5 h-1.5 rounded-full bg-amber-500"></span>
                                    <span>资源</span>
                                  </div>
                                  <div className="col-span-1 flex items-center space-x-1">
                                    <span className="w-1.5 h-1.5 rounded-full bg-green-500"></span>
                                    <span>状态</span>
                                  </div>
                                  <div className="col-span-4 flex items-center space-x-1">
                                    <span className="w-1.5 h-1.5 rounded-full bg-purple-500"></span>
                                    <span>角色</span>
                                  </div>
                                  <div className="col-span-1 flex items-center space-x-1">
                                    <span className="w-1.5 h-1.5 rounded-full bg-orange-500"></span>
                                    <span>时间</span>
                                  </div>
                                  <div className="col-span-1 flex items-center space-x-1">
                                    <span className="w-1.5 h-1.5 rounded-full bg-indigo-500"></span>
                                    <span>版本</span>
                                  </div>
                                  <div className="col-span-1 flex items-center justify-center space-x-1">
                                    <span className="w-1.5 h-1.5 rounded-full bg-rose-500"></span>
                                    <span>受管</span>
                                  </div>
                                </div>
                              </div>
                              
                              {/* 节点列表容器 - 修复高度问题 */}
                              <div className="flex-1 flex flex-col min-h-0 max-h-full">
                                {/* 分页数据显示区域 */}
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
                                    {filteredData
                                      .slice((pagination.current - 1) * pagination.pageSize, pagination.current * pagination.pageSize)
                                      .map((host) => {
                                        const isSelected = selectedRowKeys.includes(host.ip)
                                        const statusColor = (host.status || 'Ready') === 'Ready' ? 'green' : 'red'
                                        const managementStatus = host.managementStatus || ManagementStatus.UNMANAGED
                                        const managedStatus = ManagementStatusUtil.getLabel(managementStatus)
                                        const managedColor = ManagementStatusUtil.getColor(managementStatus)
                                        
                                        return (
                                          <div 
                                            key={host.ip}
                                            className={`group relative transform transition-all duration-200 ease-out hover:scale-[1.01] ${
                                              isSelected ? 'scale-[1.01]' : ''
                                            }`}
                                          >
                                            {/* 主卡片容器 - 紧凑设计 */}
                                            <div 
                                              className={`relative rounded-xl border transition-all duration-300 cursor-pointer overflow-hidden ${
                                                isSelected 
                                                  ? 'border-blue-300 bg-gradient-to-br from-blue-50 via-white to-blue-50/50 shadow-lg shadow-blue-100/50' 
                                                  : 'border-gray-200 bg-white hover:border-gray-300 hover:shadow-md shadow-sm'
                                              }`}
                                              onClick={() => {
                                                const newSelected = isSelected
                                                  ? selectedRowKeys.filter(key => key !== host.ip)
                                                  : [...selectedRowKeys, host.ip]
                                                onSelectChange(newSelected)
                                              }}
                                            >
                                              {/* 选中状态的左侧指示条 */}
                                              <div className={`absolute left-0 top-0 bottom-0 w-1 transition-all duration-300 ${
                                                isSelected ? 'bg-gradient-to-b from-blue-500 to-blue-600' : 'bg-transparent'
                                              }`} />
                                              
                                              <div className="grid grid-cols-12 gap-2 px-3 py-2">
                                                {/* 节点名称 */}
                                                <div className="col-span-2 flex items-center space-x-2">
                                                  {/* 选择指示器 */}
                                                  <div className={`w-3 h-3 rounded-full border-2 transition-all duration-200 flex items-center justify-center ${
                                                    isSelected 
                                                      ? 'border-blue-500 bg-blue-500' 
                                                      : 'border-gray-300 group-hover:border-blue-300'
                                                  }`}>
                                                    {isSelected && (
                                                      <svg className="w-2 h-2 text-white" viewBox="0 0 20 20" fill="currentColor">
                                                        <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                                                      </svg>
                                                    )}
                                                  </div>
                                                  
                                                  <div className="flex-1 min-w-0">
                                                    <div className="font-medium text-gray-900 truncate text-sm leading-tight">
                                                      {host.hostname || host.ip}
                                                    </div>
                                                    <div className="text-xs text-gray-500 font-mono truncate">
                                                      {host.ip}
                                                    </div>
                                                  </div>
                                                </div>
                                                
                                                {/* 资源信息 */}
                                                <div className="col-span-2 flex items-center">
                                                  <div className="text-[9px] text-gray-600 font-mono space-y-0.5">
                                                    <div className="flex items-center space-x-2">
                                                      <span className="inline-flex items-center">
                                                        <span className="w-1 h-1 rounded-full bg-blue-400 mr-1"></span>
                                                        <span>CPU: {host.cpuCore || 0}C</span>
                                                      </span>
                                                      <span className="inline-flex items-center">
                                                        <span className="w-1 h-1 rounded-full bg-green-400 mr-1"></span>
                                                        <span>MEM: {host.totalMem || 0}G</span>
                                                      </span>
                                                    </div>
                                                    <div className="flex items-center space-x-2">
                                                      <span className="inline-flex items-center">
                                                        <span className="w-1 h-1 rounded-full bg-orange-400 mr-1"></span>
                                                        <span>DISK: {host.totalDisk || 0}G</span>
                                                      </span>
                                                      <span className="inline-flex items-center">
                                                        <span className="w-1 h-1 rounded-full bg-purple-400 mr-1"></span>
                                                        <span>{(host.cpuArchitecture || 'x64').slice(0, 6)}</span>
                                                      </span>
                                                    </div>
                                                  </div>
                                                </div>
                                                
                                                {/* 状态 */}
                                                <div className="col-span-1 flex items-center">
                                                  <div className={`inline-flex items-center px-1.5 py-0.5 rounded-md text-[10px] font-medium transition-all duration-200 ${
                                                    statusColor === 'green'
                                                      ? 'bg-emerald-100 text-emerald-800 border border-emerald-200'
                                                      : 'bg-rose-100 text-rose-800 border border-rose-200'
                                                  }`}>
                                                    <div className={`w-1 h-1 rounded-full mr-1 ${
                                                      statusColor === 'green' ? 'bg-emerald-500' : 'bg-rose-500'
                                                    }`}></div>
                                                    {host.status || 'Ready'}
                                                  </div>
                                                </div>
                                                
                                                {/* 角色 */}
                                                <div className="col-span-4 flex items-center">
                                                  <div className="flex flex-wrap gap-0.5 w-full" title={host.roles || '<none>'}>
                                                    {(host.roles || '<none>').split(',').map((role: string, idx: number) => (
                                                      <span 
                                                        key={idx}
                                                        className={`inline-flex items-center px-1.5 py-0.5 rounded text-[9px] font-medium transition-all duration-200 ${
                                                          role.trim() === '<none>' || role.trim() === '' 
                                                            ? 'bg-gray-100 text-gray-600'
                                                            : role.includes('control-plane') || role.includes('master')
                                                              ? 'bg-purple-100 text-purple-700'
                                                              : 'bg-blue-100 text-blue-700'
                                                        }`}
                                                      >
                                                        {role.trim() === '<none>' ? 'none' : 
                                                         role.trim() || 'worker'}
                                                      </span>
                                                    ))}
                                                  </div>
                                                </div>
                                                
                                                {/* 运行时间 */}
                                                <div className="col-span-1 flex items-center">
                                                  <div className="text-[10px] text-gray-500 font-mono">
                                                    {host.age || '未知'}
                                                  </div>
                                                </div>
                                                
                                                {/* 版本 */}
                                                <div className="col-span-1 flex items-center">
                                                  <div className="text-[10px] text-gray-500 font-mono">
                                                    {host.version ? 
                                                      host.version.replace('v', '') : 
                                                      '未知'}
                                                  </div>
                                                </div>
                                                
                                                {/* 受管状态 */}
                                                <div className="col-span-1 flex items-center justify-center">
                                                  <div className={`inline-flex items-center px-1.5 py-0.5 rounded text-[9px] font-medium border transition-all duration-200 ${
                                                    managedColor === 'emerald'
                                                      ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                                                      : 'bg-rose-50 text-rose-700 border-rose-200'
                                                  }`}>
                                                    {managedStatus}
                                                  </div>
                                                </div>
                                              </div>
                                            </div>
                                          </div>
                                        )
                                      })}
                                  </div>
                                </div>
                                
                                {/* 分页组件 - Apple风格底栏 */}
                                {filteredData.length > 0 && (
                                  <div className="bg-white backdrop-blur-md border-t border-gray-200/80 p-3 flex-shrink-0 shadow-lg">
                                    <div className="flex items-center justify-between">
                                      <div className="text-sm text-gray-700 font-medium">
                                        <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold bg-blue-50 text-blue-700 border border-blue-200">
                                          {Math.min((pagination.current - 1) * pagination.pageSize + 1, filteredData.length)} - {Math.min(pagination.current * pagination.pageSize, filteredData.length)}
                                        </span>
                                        <span className="mx-2 text-gray-500">共</span>
                                        <span className="font-semibold text-gray-900">{filteredData.length}</span>
                                        <span className="text-gray-500">条记录</span>
                                        {filteredData.length !== dataSource.length && (
                                          <span className="ml-2 inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium bg-amber-50 text-amber-700 border border-amber-200">
                                            已过滤 {dataSource.length - filteredData.length} 条
                                          </span>
                                        )}
                                      </div>
                                      <div className="flex items-center space-x-4">
                                        {/* 每页显示数量 - Apple风格 */}
                                        <div className="flex items-center space-x-3">
                                          <span className="text-sm text-gray-600 font-medium">每页</span>
                                          <div className="relative">
                                            <select 
                                              value={pagination.pageSize}
                                              onChange={(e) => {
                                                const newPageSize = parseInt(e.target.value)
                                                setPagination(prev => ({
                                                  ...prev,
                                                  pageSize: newPageSize,
                                                  current: 1
                                                }))
                                              }}
                                              className="appearance-none bg-gray-50 border border-gray-200 rounded-lg px-3 py-1.5 pr-8 text-sm font-medium text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-400 cursor-pointer transition-all duration-200"
                                            >
                                              <option value="10">10</option>
                                              <option value="20">20</option>
                                              <option value="50">50</option>
                                              <option value="100">100</option>
                                            </select>
                                            <svg className="absolute right-2 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                                            </svg>
                                          </div>
                                          <span className="text-sm text-gray-600">条</span>
                                        </div>
                                        
                                        {/* 分页按钮 - Apple风格 */}
                                        <div className="flex items-center space-x-2">
                                          <button
                                            disabled={pagination.current === 1}
                                            onClick={() => setPagination(prev => ({ ...prev, current: prev.current - 1 }))}
                                            className={`px-4 py-2 rounded-lg text-sm font-medium transition-all duration-200 ${
                                              pagination.current === 1
                                                ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                                                : 'bg-gray-50 text-gray-700 hover:bg-blue-50 hover:text-blue-600 border border-gray-200 hover:border-blue-300'
                                            }`}
                                          >
                                            上一页
                                          </button>
                                          
                                          <div className="flex items-center space-x-1 bg-gray-50 rounded-lg p-1">
                                            {/* 页码按钮 */}
                                            {Array.from({ length: Math.ceil(filteredData.length / pagination.pageSize) }, (_, i) => i + 1)
                                              .filter(page => {
                                                const current = pagination.current
                                                return page === 1 || page === Math.ceil(filteredData.length / pagination.pageSize) || 
                                                       (page >= current - 2 && page <= current + 2)
                                              })
                                              .map((page, index, array) => (
                                                <div key={page} className="flex items-center">
                                                  {index > 0 && array[index - 1] < page - 1 && (
                                                    <span className="text-gray-400 px-2">⋯</span>
                                                  )}
                                                  <button
                                                    onClick={() => setPagination(prev => ({ ...prev, current: page }))}
                                                    className={`w-8 h-8 rounded-md text-sm font-medium transition-all duration-200 ${
                                                      pagination.current === page
                                                        ? 'bg-blue-500 text-white shadow-sm'
                                                        : 'text-gray-600 hover:bg-white hover:text-blue-600 hover:shadow-sm'
                                                    }`}
                                                  >
                                                    {page}
                                                  </button>
                                                </div>
                                              ))
                                            }
                                          </div>
                                          
                                          <button
                                            disabled={pagination.current === Math.ceil(filteredData.length / pagination.pageSize)}
                                            onClick={() => setPagination(prev => ({ ...prev, current: prev.current + 1 }))}
                                            className={`px-4 py-2 rounded-lg text-sm font-medium transition-all duration-200 ${
                                              pagination.current === Math.ceil(filteredData.length / pagination.pageSize)
                                                ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                                                : 'bg-gray-50 text-gray-700 hover:bg-blue-50 hover:text-blue-600 border border-gray-200 hover:border-blue-300'
                                            }`}
                                          >
                                            下一页
                                          </button>
                                        </div>
                                      </div>
                                    </div>
                                  </div>
                                )}
                              </div>
                            </div>
                          </div>
                        )}
                      </CardContent>
                    </Card>
                  </div>

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
                          <span className="text-gray-600">显示主机数</span>
                          <span className="font-semibold">{filteredData.length}</span>
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
            </div>

            {/* 底部操作栏 - 统一美观样式 */}
            <div className={DIALOG_STYLES.footer}>
              {/* 装饰性光效 */}
              <div className={DIALOG_STYLES.footerGlow}></div>
              {/* 顶部分割线光效 */}
              <div className={DIALOG_STYLES.footerTopLine}></div>
              
              <div className={DIALOG_STYLES.footerContent}>
                <div className="flex items-center space-x-4">
                  <div className="flex items-center space-x-3">
                    <div className="w-3 h-3 rounded-full bg-blue-500 animate-pulse"></div>
                    <span className="text-sm font-medium text-gray-700">
                      已选择 
                      <span className="mx-1 px-2 py-0.5 bg-blue-100 text-blue-700 rounded-full text-xs font-semibold">
                        {selectedRowKeys.length}
                      </span>
                      台主机
                    </span>
                  </div>
                  {ClusterTypeUtil.isKubernetes(clusterType) && unmanagedCount > 0 && (
                    <div className="flex items-center space-x-2 px-3 py-1.5 bg-green-50 rounded-lg border border-green-200">
                      <div className="w-2 h-2 rounded-full bg-green-500"></div>
                      <span className="text-sm font-medium text-green-700">
                        {unmanagedCount} 台可部署
                      </span>
                    </div>
                  )}
                </div>
                <div className="flex items-center space-x-3">
                  <button
                    onClick={() => {
                      if (onPrevious) {
                        onPrevious()
                      } else {
                        onOpenChange(false)
                      }
                    }}
                    className="flex items-center px-5 py-2.5 bg-gray-50 hover:bg-gray-100 border border-gray-200 hover:border-gray-300 rounded-xl text-sm font-medium text-gray-700 transition-all duration-200 shadow-sm hover:shadow-md"
                  >
                    <ChevronLeft className="w-4 h-4 mr-2" />
                    上一步
                  </button>
                  <button
                    onClick={handleNext}
                    disabled={selectedRowKeys.length === 0 || loading}
                    className={`flex items-center px-6 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 shadow-md hover:shadow-lg ${
                      selectedRowKeys.length === 0 || loading
                        ? 'bg-gray-200 text-gray-400 cursor-not-allowed'
                        : 'bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 text-white transform hover:scale-105'
                    }`}
                  >
                    {loading ? (
                      <>
                        <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                        保存中...
                      </>
                    ) : (
                      <>
                        下一步
                        <ChevronRight className="w-4 h-4 ml-2" />
                      </>
                    )}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}
