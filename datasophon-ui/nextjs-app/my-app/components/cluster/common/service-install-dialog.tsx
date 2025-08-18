"use client"

import React, { useState, useEffect, useRef, useCallback } from 'react'
import { 
  ChevronLeft, ChevronRight, AlertTriangle, 
  Play, Clock, CheckCircle2, XCircle, AlertCircle, 
  Activity, ArrowRight, ArrowDown,
  Eye, Terminal, Cpu, Wifi, WifiOff
} from 'lucide-react'
import { toast } from 'sonner'
import { Dialog, DialogTitle } from '@/components/ui/dialog'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Progress } from '@/components/ui/progress'
import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import ServiceIcon from '@/components/ui/service-icon'

// 简单的Skeleton组件
const Skeleton = ({ className }: { className?: string }) => (
  <div className={`animate-pulse bg-slate-200 rounded ${className}`} />
)
import { Checkbox } from '@/components/ui/checkbox'

import ClusterWizardLayout from './cluster-wizard-layout'
import ClusterWizardActionBar from './cluster-wizard-action-bar'
import { useLogWebSocket } from '@/hooks/useLogWebSocket'
import { apiV1, API_PATHS_V1 } from "@/lib/api-config-v1"
import { createClusterHeaders } from '@/lib/cluster-id-header'

// 时间格式化工具函数
const formatRelativeTime = (dateString: string): string => {
  if (!dateString) return '未知时间'
  
  const date = new Date(dateString)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMinutes = Math.floor(diffMs / (1000 * 60))
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60))
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))
  
  if (diffMinutes < 1) return '刚刚'
  if (diffMinutes < 60) return `${diffMinutes}分钟前`
  if (diffHours < 24) return `${diffHours}小时前`
  if (diffDays < 7) return `${diffDays}天前`
  
  return date.toLocaleDateString('zh-CN', { 
    year: 'numeric', 
    month: 'short', 
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatDuration = (durationText: string): { text: string; color: string } => {
  if (!durationText || durationText === '0 seconds') {
    return { text: '即时', color: 'text-emerald-600' }
  }
  
  if (durationText.includes('second')) {
    return { text: durationText.replace(' seconds', '秒').replace(' second', '秒'), color: 'text-blue-600' }
  }
  if (durationText.includes('minute')) {
    return { text: durationText.replace(' minutes', '分钟').replace(' minute', '分钟'), color: 'text-amber-600' }
  }
  if (durationText.includes('hour')) {
    return { text: durationText.replace(' hours', '小时').replace(' hour', '小时'), color: 'text-orange-600' }
  }
  
  return { text: durationText, color: 'text-slate-600' }
}


import { Step7Data } from '@/types/service-config'

// 安装状态类型定义
interface InstallStatus {
  isInstalling: boolean
  hasStarted: boolean
  commandIds?: string
  error?: string
}

// 类型定义（保持与原API一致）
interface DataItem {
  commandId?: string
  hostCommandId?: string
  commandName: string
  hostname?: string
  commandProgress: number
  commandStateCode: number
  createTime?: string
  durationTime?: string
  resultMsg?: string
  [key: string]: unknown // 支持动态属性访问
}
interface ClusterInfo {
  id: number
  clusterName: string
  depType: string
}



interface PaginationState {
  total: number
  pageSize: number
  current: number
  showSizeChanger: boolean
  pageSizeOptions: string[]
  showTotal: (total: number) => string
}

interface ServiceInstallDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  cluster: ClusterInfo
  clusterType: string
  serviceConfigData: Step7Data
  onComplete: () => void
  onPrevious?: () => void
}

/**
 * 服务安装对话框 - 迁移自Vue2 step8.vue
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 * @date 2024-12-19
 */
const ServiceInstallDialog: React.FC<ServiceInstallDialogProps> = ({
  open,
  onOpenChange,
  cluster,
  clusterType,
  serviceConfigData,
  onComplete,
  onPrevious
}) => {
  // 状态管理（对应原Vue2的data）
  const [title, setTitle] = useState("安装并启动服务")
  const [selectedRowKeys, setSelectedRowKeys] = useState<string[]>([])
  const [pagination, setPagination] = useState<PaginationState>({
    total: 0,
    pageSize: 10,
    current: 1,
    showSizeChanger: true,
    pageSizeOptions: ["10", "20", "50", "100"],
    showTotal: (total) => `共 ${total} 条`,
  })
  const [dataSource, setDataSource] = useState<DataItem[]>([])
  const [loading, setLoading] = useState(false)

  const [currentPage, setCurrentPage] = useState(1)
  const [commandId, setCommandId] = useState("") // 第二个列表请求页面需要的参数
  const [hostname, setHostname] = useState("") // 第三个列表请求页面需要的参数
  const [commandHostId, setCommandHostId] = useState("") // 第三个列表请求页面需要的参数

  const [logData, setLogData] = useState("")
  const [error, setError] = useState<string | null>(null)
  const [userScrolledUp, setUserScrolledUp] = useState(false) // 跟踪用户是否向上滚动
  
  // 自动滚动到底部的函数
  const scrollToBottom = useCallback((force = false) => {
    const container = logScrollContainerRef.current
    if (!container) return
    
    // 如果用户手动向上滚动了，且不是强制滚动，则不自动滚动
    if (userScrolledUp && !force) return
    
    // 使用requestAnimationFrame确保DOM已更新
    requestAnimationFrame(() => {
      container.scrollTo({
        top: container.scrollHeight,
        behavior: 'smooth'
      })
    })
  }, [userScrolledUp])
  
  // 检查是否滚动到底部
  const checkScrollPosition = useCallback(() => {
    const container = logScrollContainerRef.current
    if (!container) return
    
    const { scrollTop, scrollHeight, clientHeight } = container
    const isAtBottom = scrollHeight - scrollTop - clientHeight < 20 // 减小容差到20px
    const hasScrolled = !isAtBottom
    
    // 调试信息
    console.log('🔍 滚动位置检查:', {
      scrollTop,
      scrollHeight, 
      clientHeight,
      差值: scrollHeight - scrollTop - clientHeight,
      isAtBottom,
      hasScrolled,
      userScrolledUp: hasScrolled
    })
    
    setUserScrolledUp(hasScrolled)
  }, [])
  
  // WebSocket实时日志连接
  const {
    isConnected: wsConnected,
    isConnecting: wsConnecting,
    error: wsError
  } = useLogWebSocket({
    clusterId: cluster.id.toString(),
    hostCommandId: currentPage === 4 && commandHostId ? commandHostId : undefined,
    onLogUpdate: (content, updateType) => {
      setLogData(content)
      
      // 只有在追加新日志时才自动滚动
      if (updateType === 'append') {
        scrollToBottom()
      } else if (updateType === 'replace') {
        // 历史日志加载完成，强制滚动到底部
        scrollToBottom(true)
        setUserScrolledUp(false)
      }
    },
    enabled: currentPage === 4 && !!commandHostId
  })
  
  const [installStatus, setInstallStatus] = useState<InstallStatus>({
    isInstalling: false,
    hasStarted: false
  })
  // 添加一个锁，确保安装只启动一次
  const installationStartedRef = useRef(false)
  
  // 定时器引用
  const timer1 = useRef<NodeJS.Timeout | null>(null)
  const timer2 = useRef<NodeJS.Timeout | null>(null)
  const timer3 = useRef<NodeJS.Timeout | null>(null)
  
  // 日志滚动容器引用
  const logScrollContainerRef = useRef<HTMLDivElement>(null)
  
  // 当切换到日志页面时，重置滚动状态并自动滚动到底部
  useEffect(() => {
    if (currentPage === 4) {
      setUserScrolledUp(false)
      // 延迟一点确保日志内容已加载
      setTimeout(() => {
        scrollToBottom(true)
      }, 200)
    }
  }, [currentPage, scrollToBottom])
  // 保存最新的getServiceList函数引用，避免useEffect依赖问题
  const getServiceListRef = useRef<(flag?: boolean) => Promise<void>>(async () => {})
  // 保存最新的启动安装函数引用
  const startInstallationRef = useRef<() => Promise<void>>(async () => {})

  // 计算步骤信息
  const isK8s = clusterType?.toLowerCase() === 'kubernetes'
  const currentStepNumber = isK8s ? 7 : 8

  // 启动服务安装（对应Vue2的submitAllServices逻辑）
  const startInstallation = useCallback(async () => {
    // 使用ref锁来防止重复调用
    if (installationStartedRef.current) {
      console.log('安装已经启动过，跳过重复启动');
      return;
    }
    
    installationStartedRef.current = true; // 设置锁

    console.log('开始启动服务安装流程');
    setInstallStatus(prev => ({ ...prev, isInstalling: true }));
    setError(null);

    try {
      // 从serviceConfigData获取服务名称列表  
      const serviceNames = Object.keys(serviceConfigData?.serviceConfigs || {});
      console.log('准备安装的服务:', serviceNames);

      if (serviceNames.length === 0) {
        throw new Error('没有找到要安装的服务');
      }

      const headers = createClusterHeaders(cluster.id.toString());

      // 1. 生成安装命令
      console.log('正在生成安装命令...');
      const generateResponse = await apiV1.post(
        `${API_PATHS_V1.GENERATE_SERVICE_INSTALL_COMMAND}?commandType=INSTALL_SERVICE`,
        serviceNames, // 直接传递数组，不用包装成对象
        { headers }
      );

      if (generateResponse.data?.code !== 200) {
        throw new Error(generateResponse.data?.msg || '生成安装命令失败');
      }

      const commandIds = generateResponse.data.data;
      console.log('生成的命令ID:', commandIds);

      // 2. 启动执行命令
      console.log('正在启动执行命令...');
      const executeResponse = await apiV1.post(
        API_PATHS_V1.START_EXECUTE_COMMAND,
        {
          commandType: 'INSTALL_SERVICE',
          commandIds
        },
        { headers }
      );

      if (executeResponse.data?.code !== 200) {
        throw new Error(executeResponse.data?.msg || '启动执行命令失败');
      }

      console.log('服务安装已成功启动');
      setInstallStatus({
        isInstalling: false,
        hasStarted: true,
        commandIds
      });

      // 启动成功后开始监控
      setTimeout(() => {
        getServiceListRef.current();
        timer1.current = setInterval(() => {
          getServiceListRef.current(true);
        }, 3000);
      }, 500);

    } catch (err: unknown) {
      console.error('启动服务安装失败:', err);
      const error = err as { response?: { data?: { message?: string } }; message?: string };
      const errorMsg = error.response?.data?.message || error.message || '启动服务安装失败';
      
      // 清除所有定时器，防止无限重试
      if (timer1.current) {
        clearInterval(timer1.current);
        timer1.current = null;
      }
      if (timer2.current) {
        clearInterval(timer2.current);
        timer2.current = null;
      }
      if (timer3.current) {
        clearInterval(timer3.current);
        timer3.current = null;
      }
      
      setError(errorMsg);
      setInstallStatus({
        isInstalling: false,
        hasStarted: false,
        error: errorMsg
      });
      // 重置锁，允许重新尝试
      installationStartedRef.current = false;
      toast.error(errorMsg);
    }
  }, [cluster.id, serviceConfigData?.serviceConfigs]);

  // 更新启动安装函数引用
  useEffect(() => {
    startInstallationRef.current = startInstallation;
  }, [startInstallation]);

  // 获取服务列表（对应原Vue2的getServiceList方法）
  const getServiceList = useCallback(async (flag?: boolean) => {
    // 第4页是纯日志查看页面，不需要调用任何API
    if (currentPage === 4) {
      console.log('第4页是日志查看页面，跳过API调用');
      return;
    }
    
    if (!flag) setLoading(true)
    setError(null)
    
    const params: Record<string, unknown> = {
      pageSize: pagination.pageSize,
      page: pagination.current,
      // 注意：clusterId从请求头传递，不需要在params中
    }
    
    if (currentPage === 2) {
      if (!commandId) {
        console.warn('第2页缺少commandId参数，跳过API调用');
        setLoading(false);
        return;
      }
      params.commandId = commandId
    }
    
    if (currentPage === 3) {
      console.log('=== 第3页API调用参数 ===');
      console.log('hostname:', hostname);
      console.log('commandHostId:', commandHostId);
      
      if (!hostname || !commandHostId) {
        console.warn('第3页缺少必需参数，跳过API调用', { hostname, commandHostId });
        setLoading(false);
        return;
      }
      
      params.hostname = hostname
      params.commandHostId = commandHostId
    }
    
    const apiPath = currentPage === 1 
      ? API_PATHS_V1.GET_SERVICE_COMMAND_LIST
      : currentPage === 2
      ? API_PATHS_V1.GET_SERVICE_HOST_LIST
      : API_PATHS_V1.GET_SERVICE_ROLE_ORDER_LIST

    try {
      const headers = createClusterHeaders(cluster.id.toString())
      
      // 所有页面都使用GET方法，参数作为查询参数
      // 第1页: /cluster/service/command/list
      // 第2页: /cluster/service/command/host/list  
      // 第3页: /cluster/service/command/host/command/list
      const response = await apiV1.get(apiPath, { headers, params })
      
      if (response.data?.code === 200) {
        // API返回的数据结构：{ code: 200, data: { records: [...], total: "4", ... } }
        const responseData = response.data.data || {}
        // 前端只负责展示，不修改后端数据
        setDataSource(responseData.records || [])

        setPagination(prev => ({
          ...prev,
          total: parseInt(responseData.total) || 0
        }))
      } else {
        throw new Error(response.data?.message || '获取数据失败')
      }
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } }; message?: string }
      const errorMsg = error.response?.data?.message || error.message || '获取数据失败'
      setError(errorMsg)
      toast.error(errorMsg)
    } finally {
      setLoading(false)
    }
  }, [cluster.id, currentPage, commandId, hostname, commandHostId, pagination])

  // 更新函数引用
  useEffect(() => {
    getServiceListRef.current = getServiceList
  }, [getServiceList])





  // 查看详情（对应原Vue2的seeDetail方法）
  const seeDetail = useCallback(async (row: DataItem) => {
    // 清除定时器
    if (timer1.current) {
      clearInterval(timer1.current);
      timer1.current = null;
    }
    if (timer2.current) {
      clearInterval(timer2.current);
      timer2.current = null;
    }
    if (timer3.current) {
      clearInterval(timer3.current);
      timer3.current = null;
    }
    
    setPagination(prev => ({ ...prev, current: 1 }))
    
    if (currentPage === 3) {
      // 设置日志查看页面参数，WebSocket会自动连接获取日志
      setHostname(row.hostname || "")
      setCommandHostId(String(row.commandHostId || ""))
          setCurrentPage(4)
          setTitle("查看日志")
      // 清空旧日志，WebSocket会推送新日志
      setLogData("")
      return
    }
    
    // 立即清理数据，避免key重复
    setDataSource([])
    setSelectedRowKeys([])

    setPagination(prev => ({
      ...prev,
      current: 1,
      total: 0
    }))
    
    const newPage = currentPage + 1
    
    // 同步设置页面状态和必需参数
    if (newPage === 2) {
      console.log('=== 进入第2页 ===');
      console.log('设置commandId:', row.commandId);
        setTitle(`${row.commandName || '服务'} - 主机列表`)
      setCommandId(row.commandId || "")
      setCurrentPage(2)
    } else if (newPage === 3) {
      console.log('=== 进入第3页 ===');
      console.log('设置hostname:', row.hostname);
      console.log('设置commandHostId:', row.commandHostId);
        setTitle(`${row.hostname || '主机'} - 执行日志`)
      setCommandHostId(String(row.commandHostId || ""))
      setHostname(row.hostname || "")
      setCurrentPage(3)
    }
    
    // useEffect会自动检测参数变化并触发数据加载
  }, [currentPage])





  // 表格分页变化
  const tableChange = useCallback((newPagination: { current: number; pageSize: number }) => {
    setPagination(prev => ({
      ...prev,
      current: newPagination.current,
      pageSize: newPagination.pageSize
    }))
  }, [])



  // 处理完成
  const handleNext = useCallback(async () => {
    onComplete()
  }, [onComplete])

  // 处理取消
  const handleCancel = useCallback(() => {
    onOpenChange(false)
  }, [onOpenChange])

  // 动作栏配置
  const actionBar = (
    <ClusterWizardActionBar
      buttons={[
        ...(onPrevious ? [{
          text: "上一步",
          onClick: onPrevious,
          disabled: loading,
          variant: "secondary" as const
        }] : []),
        {
          text: "完成",
          onClick: handleNext,
          disabled: loading,
          loading: loading,
          loadingText: "处理中...",
          variant: "primary" as const,
          icon: ChevronRight
        }
      ]}
    />
  )

  // 初始化和清理
  useEffect(() => {
    if (open) {
      // 每次打开对话框时重置到第1页，去除记忆功能
      setCurrentPage(1)
      setTitle("服务安装状态")
      setDataSource([])
      setSelectedRowKeys([])
      setCommandId("")
      setHostname("")
      setCommandHostId("")
      setLogData("")
      setError(null)
      setInstallStatus({
        isInstalling: false,
        hasStarted: false
      })
      // 重置安装锁
      installationStartedRef.current = false

      setPagination(prev => ({
        ...prev,
        current: 1,
        total: 0
      }))
      
      // 延迟一点再检查并启动安装流程
      const timeoutId = setTimeout(() => {
        // 直接启动安装，不先检查状态（避免重复调用）
        console.log('对话框打开，直接启动安装流程');
        startInstallationRef.current();
      }, 100)
    
      return () => {
        clearTimeout(timeoutId)
      }
    } else {
      // 🔧 修复：对话框关闭时立即清理所有定时器
      console.log('对话框关闭（open=false），清理定时器...');
      if (timer1.current) {
        clearInterval(timer1.current);
        timer1.current = null;
      }
      if (timer2.current) {
        clearInterval(timer2.current);
        timer2.current = null;
      }
      if (timer3.current) {
        clearInterval(timer3.current);
        timer3.current = null;
      }
    }
    
    // 🔧 加强：组件卸载时的最终清理
    return () => {
      console.log('组件卸载，最终清理定时器...');
      if (timer1.current) {
        clearInterval(timer1.current);
        timer1.current = null;
      }
      if (timer2.current) {
        clearInterval(timer2.current);
        timer2.current = null;
      }
      if (timer3.current) {
        clearInterval(timer3.current);
        timer3.current = null;
      }
    }
  }, [open]) // 移除状态依赖，避免无限循环

  // 页面切换完成后的数据加载（只在安装已经开始后才进行）
  useEffect(() => {
    // 只有在安装已经开始且不是第4页时才进行数据加载
    if (open && installStatus.hasStarted && currentPage > 0 && currentPage < 4) {
      // 检查是否有必需的参数
      let canLoad = true
      if (currentPage === 2 && !commandId) {
        console.log('第2页等待commandId设置');
        canLoad = false
      }
      if (currentPage === 3 && (!hostname || !commandHostId)) {
        console.log('第3页等待hostname和commandHostId设置');
        canLoad = false
      }
      
      if (canLoad) {
        console.log(`第${currentPage}页参数就绪，开始加载数据`);
        
        // 清除之前的定时器
        const timer1Ref = timer1.current;
        const timer2Ref = timer2.current;
        const timer3Ref = timer3.current;
        if (timer1Ref) {
          clearInterval(timer1Ref);
          timer1.current = null;
        }
        if (timer2Ref) {
          clearInterval(timer2Ref);
          timer2.current = null;
        }
        if (timer3Ref) {
          clearInterval(timer3Ref);
          timer3.current = null;
        }
        
        // 立即加载一次数据
        getServiceListRef.current()
        
        // 启动对应的定时器
        const newTimer = setInterval(() => {
          getServiceListRef.current(true) // flag=true表示静默刷新
        }, 3000)
        
        if (currentPage === 1) {
          timer1.current = newTimer
        } else if (currentPage === 2) {
          timer2.current = newTimer
      } else {
          timer3.current = newTimer
      }
    }
    }
  }, [currentPage, open, commandId, hostname, commandHostId, installStatus.hasStarted])

  // 分页变化时重新获取数据
  useEffect(() => {
    if (open && currentPage > 0) {
      // 第4页是纯日志查看页面，不需要分页功能
      if (currentPage === 4) {
        return;
      }
      
      // 只有真正的分页操作（不是初始化）才触发数据重新加载
      const isPaginationChange = pagination.current !== 1 || pagination.pageSize !== 10
      
      if (isPaginationChange) {
        console.log('分页参数变化，重新加载数据:', pagination);
        // 直接重新加载数据，不需要重启轮询
        getServiceListRef.current()
      }
    }
  }, [pagination.pageSize, open, currentPage, pagination])

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogTitle className="sr-only">服务安装 - {cluster?.clusterName}</DialogTitle>
      <ClusterWizardLayout
        open={open}
        onClose={handleCancel}
        clusterName={cluster?.clusterName}
        stepTitle="安装并启动服务"
        stepDescription=""
        dialogTitle={`服务安装 - ${cluster?.clusterName}`}
        currentStep={currentStepNumber}
        actionBar={actionBar}
      >
        {/* 顶栏描述区域 - 与其他步骤页面保持一致 */}
        <div className="bg-white border-b border-gray-200 shadow-sm flex-shrink-0">
          <div className="w-full px-4 py-3 sm:px-6 sm:py-4">
            <div className="flex items-center justify-between">
              <div className="space-y-1">
                {/* 面包屑导航 */}
                <div className="flex items-center gap-2 text-sm text-gray-500">
                  <span className={`transition-colors ${currentPage === 1 ? 'text-blue-600 font-medium' : 'cursor-pointer hover:text-blue-600'}`} 
                        onClick={() => {
                          if (currentPage > 1) {
                            // 清理数据状态，避免页面间数据混乱
                            setDataSource([])
                            setSelectedRowKeys([])
                            setPagination(prev => ({ ...prev, current: 1, total: 0 }))
                            setCurrentPage(1)
                            setTitle("服务安装状态")
                          }
                        }}>
                    服务列表
                  </span>
                  {currentPage > 1 && (
                    <>
                      <ChevronRight className="h-3 w-3 text-gray-400" />
                      <span className={`transition-colors ${currentPage === 2 ? 'text-blue-600 font-medium' : 'cursor-pointer hover:text-blue-600'}`}
                            onClick={() => {
                              if (currentPage > 2) {
                                // 清理数据状态，避免页面间数据混乱
                                setDataSource([])
                                setSelectedRowKeys([])
                                setPagination(prev => ({ ...prev, current: 1, total: 0 }))
                                setCurrentPage(2)
                                setTitle(`${commandId ? '主机列表' : '服务'} - 主机列表`)
                              }
                            }}>
                        主机详情
                      </span>
                    </>
                  )}
                  {currentPage > 2 && (
                    <>
                      <ChevronRight className="h-3 w-3 text-gray-400" />
                      <span className={`transition-colors ${currentPage === 3 ? 'text-blue-600 font-medium' : 'cursor-pointer hover:text-blue-600'}`}
                            onClick={() => {
                              if (currentPage > 3) {
                                // 清理数据状态，避免页面间数据混乱
                                setDataSource([])
                                setSelectedRowKeys([])
                                setPagination(prev => ({ ...prev, current: 1, total: 0 }))
                                setCurrentPage(3)
                                setTitle(`${hostname ? hostname : '主机'} - 执行日志`)
                              }
                            }}>
                        命令日志
                      </span>
                    </>
                  )}
                  {currentPage > 3 && (
                    <>
                      <ChevronRight className="h-3 w-3 text-gray-400" />
                      <span className="text-blue-600 font-medium">
                        查看日志
                      </span>
                    </>
                  )}
                </div>
                
                {/* 标题 */}
                <div className="flex items-center gap-3">
                  <h1 className="text-lg sm:text-xl font-semibold text-gray-900 leading-tight">
                  {title}
                </h1>
              </div>
            </div>
            </div>
          </div>
          </div>

        {/* 主要内容区域 */}
        <div className="flex-1 min-h-0 bg-gradient-to-b from-white to-slate-50/50 overflow-y-auto">
          <div className="p-6">

          {/* 错误提示 */}
          {error && (
            <div className="mb-6">
              <Alert variant="destructive" className="border-red-200 bg-gradient-to-r from-red-50 to-rose-50">
                <div className="flex items-start gap-3">
                  <div className="p-1 bg-red-100 rounded-full">
                    <AlertTriangle className="h-4 w-4 text-red-600" />
                  </div>
                  <div className="flex-1">
                    <h4 className="font-semibold text-red-800 mb-1">操作失败</h4>
                    <AlertDescription className="text-red-700">
                      {error}
                    </AlertDescription>
                    <div className="mt-2 flex items-center gap-2 text-xs text-red-600">
                      <Clock className="h-3 w-3" />
                      <span>{new Date().toLocaleTimeString('zh-CN')}</span>
                    </div>
                  </div>
                </div>
            </Alert>
            </div>
          )}

          {/* 主要内容区域 */}
          <div className="flex-1 min-h-0">
            {currentPage <= 3 ? (
              <Card className="h-full bg-white/80 backdrop-blur-sm border-0 shadow-xl">
                <CardContent className="p-0 h-full flex flex-col">

                  
                  {/* 现代化卡片列表容器 */}
                  <div className="flex-1 overflow-hidden">
                    <div className="h-full overflow-auto">
                      {loading ? (
                        <div className="p-8 space-y-4">
                          {[...Array(3)].map((_, i) => (
                            <div key={i} className="space-y-3">
                              <Skeleton className="h-4 w-[250px]" />
                              <Skeleton className="h-4 w-[200px]" />
                              <Skeleton className="h-8 w-full" />
                            </div>
                          ))}
                        </div>
                      ) : dataSource.length === 0 ? (
                        <div className="flex flex-col items-center justify-center h-64 text-slate-500">
                          <Activity className="h-12 w-12 mb-4 text-slate-300" />
                          <p className="text-lg font-medium mb-2">
                            {installStatus.isInstalling ? '正在启动安装...' : '暂无数据'}
                          </p>
                          <p className="text-sm">
                            {installStatus.isInstalling 
                              ? '正在生成和启动安装命令，请稍候...' 
                              : installStatus.hasStarted 
                              ? '等待安装任务生成...' 
                              : '准备启动服务安装...'}
                          </p>
                          {installStatus.isInstalling && (
                            <div className="mt-4">
                              <div className="animate-spin h-8 w-8 border-4 rounded-full border-blue-600 border-t-transparent"></div>
                            </div>
                          )}
                        </div>
                      ) : (
                                                <div className="p-3 space-y-2">
                          {/* 全选控制器 (仅第1页) */}
                          {currentPage === 1 && dataSource.length > 0 && (
                            <div className="flex items-center justify-between p-2 bg-slate-50 rounded border border-slate-200">
                              <span className="text-sm text-slate-600">
                                共 {dataSource.length} 个服务
                              </span>
                              <div className="flex items-center gap-2">
                                <Checkbox
                                  checked={selectedRowKeys.length === dataSource.length}
                                  onCheckedChange={(checked) => {
                                    if (checked) {
                                      setSelectedRowKeys(dataSource.map(item => item.commandId || "").filter(Boolean))
                                    } else {
                                      setSelectedRowKeys([])
                                    }
                                  }}
                                />
                                <span className="text-xs text-slate-600">
                                  全选
                                </span>
                              </div>
                            </div>
                          )}
                          
                          {/* 卡片列表 */}
                          {dataSource.map((item, index) => {
                            const isSelected = selectedRowKeys.includes(item.commandId || "")
                            // 稳定的key，避免图标闪烁 - 添加index确保唯一性
                            const stableKey = `${currentPage}-${index}-${item.commandId || item.commandName || 'unknown'}`
                            
                            const getStatusConfig = (stateCode: number) => {
                              switch (stateCode) {
                                case 1: return { 
                                  color: "blue", 
                                  bg: "bg-blue-50", 
                                  border: "border-blue-200", 
                                  text: "text-blue-700",
                                  icon: <Play className="h-4 w-4" />,
                                  status: "运行中",
                                  pulse: true
                                }
                                case 2: return { 
                                  color: "green", 
                                  bg: "bg-emerald-50", 
                                  border: "border-emerald-200", 
                                  text: "text-emerald-700",
                                  icon: <CheckCircle2 className="h-4 w-4" />,
                                  status: "已完成",
                                  pulse: false
                                }
                                case 3: return { 
                                  color: "red", 
                                  bg: "bg-red-50", 
                                  border: "border-red-200", 
                                  text: "text-red-700",
                                  icon: <XCircle className="h-4 w-4" />,
                                  status: "失败",
                                  pulse: false
                                }
                                case 4: return { 
                                  color: "yellow", 
                                  bg: "bg-amber-50", 
                                  border: "border-amber-200", 
                                  text: "text-amber-700",
                                  icon: <AlertCircle className="h-4 w-4" />,
                                  status: "警告",
                                  pulse: false
                                }
                                default: return { 
                                  color: "gray", 
                                  bg: "bg-slate-50", 
                                  border: "border-slate-200", 
                                  text: "text-slate-700",
                                  icon: <AlertTriangle className="h-4 w-4" />,
                                  status: "未知",
                                  pulse: false
                                }
                              }
                            }
                            
                            const statusConfig = getStatusConfig(item.commandStateCode)
                            const duration = formatDuration(item.durationTime || '')
                            
                                                        return (
                              <div
                                key={stableKey}
                                className={`group flex items-center gap-3 p-3 rounded-lg border transition-all duration-200 hover:shadow-sm cursor-pointer ${
                                  isSelected 
                                    ? 'border-blue-300 bg-blue-50/50' 
                                    : 'border-slate-200 hover:border-blue-300 hover:bg-blue-50/20'
                                }`}
                                onClick={() => seeDetail(item)}
                              >
                                {/* 选择框 (仅第1页) */}
                                {currentPage === 1 && (
                                  <div onClick={(e) => e.stopPropagation()}>
                                    <Checkbox
                                      checked={isSelected}
                                      onCheckedChange={(checked) => {
                                        const commandId = item.commandId || ""
                                        if (checked) {
                                          setSelectedRowKeys(prev => [...prev, commandId])
                                        } else {
                                          setSelectedRowKeys(prev => prev.filter(key => key !== commandId))
                                        }
                                      }}
                                    />
                                  </div>
                                )}
                                
                                {/* 服务图标 */}
                                <div className={`flex items-center justify-center w-8 h-8 rounded ${statusConfig.bg} ${statusConfig.border} border flex-shrink-0`}>
                                  {currentPage === 1 ? (
                                    <ServiceIcon
                                      serviceName={item.commandName || ''}
                                      size={16}
                                      className="w-4 h-4"
                                    />
                                  ) : currentPage === 2 ? (
                                    <Cpu className={`h-4 w-4 ${statusConfig.text}`} />
                                  ) : (
                                    <Terminal className={`h-4 w-4 ${statusConfig.text}`} />
                                  )}
                  </div>
                  
                                {/* 主要内容 */}
                                <div className="flex-1 min-w-0">
                                  <div className="flex items-center justify-between mb-1">
                                    {/* 标题 */}
                                    <div className="flex items-center gap-2 min-w-0">
                                      <span className="font-medium text-slate-800 truncate">
                                        {currentPage === 1 ? item.commandName : currentPage === 2 ? item.hostname : item.commandName}
                      </span>
                                      <ArrowRight className="h-3 w-3 text-slate-400 group-hover:text-blue-500 transition-colors flex-shrink-0" />
                                    </div>
                                    
                                    {/* 状态标签 */}
                                    <Badge 
                                      variant="secondary" 
                                      className={`${statusConfig.bg} ${statusConfig.text} border-0 text-xs flex-shrink-0 ${statusConfig.pulse ? 'animate-pulse' : ''}`}
                                    >
                                      {statusConfig.icon}
                                      <span className="ml-1">{statusConfig.status}</span>
                                    </Badge>
                                  </div>
                                  
                                  {/* 进度条 */}
                                  <div className="mb-1">
                                    <Progress 
                                      value={item.commandProgress || 0} 
                                      className="h-1 bg-slate-100"
                                    />
                                  </div>
                                  
                                  {/* 底部信息 */}
                                  <div className="flex items-center justify-between text-xs text-slate-500">
                      <div className="flex items-center gap-2">
                                      {item.createTime && (
                                        <span>{formatRelativeTime(String(item.createTimeFormatted || item.createTime || ''))}</span>
                                      )}
                                      {currentPage === 1 && item.durationTime && (
                                        <span className={duration.color}>• {duration.text}</span>
                                      )}
                                    </div>
                                    
                                    <div className="flex items-center gap-2">
                                      <span>{item.commandProgress || 0}%</span>
                                      {currentPage === 3 && (
                                        <div className="flex items-center gap-1 text-blue-600">
                                          <Eye className="h-3 w-3" />
                                          <span>查看日志</span>
                                        </div>
                                      )}
                                    </div>
                                  </div>
                                </div>
                              </div>
                            )
                          })}
                        </div>
                      )}
                    </div>
                  </div>
                  
                  {/* 现代化分页器 */}
                  {pagination.total > 0 && (() => {
                    const totalPages = Math.ceil(pagination.total / pagination.pageSize)
                    const currentPage = pagination.current
                    
                    // 生成页码数组（显示当前页前后各2页）
                    const getPageNumbers = () => {
                      const pages = []
                      const start = Math.max(1, currentPage - 2)
                      const end = Math.min(totalPages, currentPage + 2)
                      
                      // 如果开始页不是1，添加1和省略号
                      if (start > 1) {
                        pages.push(1)
                        if (start > 2) pages.push('...')
                      }
                      
                      // 添加中间页码
                      for (let i = start; i <= end; i++) {
                        pages.push(i)
                      }
                      
                      // 如果结束页不是最后一页，添加省略号和最后一页
                      if (end < totalPages) {
                        if (end < totalPages - 1) pages.push('...')
                        pages.push(totalPages)
                      }
                      
                      return pages
                    }
                    
                    const pageNumbers = getPageNumbers()
                    
                    return (
                      <div className="border-t bg-gradient-to-r from-white via-slate-50/50 to-white">
                        <div className="px-4 py-3">
                          {/* 分页信息 */}
                          <div className="flex items-center justify-between">
                            <div className="flex items-center gap-3 text-sm text-slate-600">
                              <span className="font-medium">
                                共 <span className="text-blue-600 font-semibold">{pagination.total}</span> 项
                              </span>
                              <span className="text-slate-400">•</span>
                              <span>
                                第 <span className="text-blue-600 font-semibold">{currentPage}</span> / {totalPages} 页
                              </span>
                            </div>
                            
                            {/* 分页控件 */}
                            <div className="flex items-center gap-1">
                              {/* 上一页 */}
                        <Button
                          variant="outline"
                          size="sm"
                                disabled={currentPage <= 1}
                          onClick={() => tableChange({ 
                                  current: currentPage - 1, 
                            pageSize: pagination.pageSize 
                          })}
                                className="h-8 w-8 p-0 border-slate-200 hover:border-blue-300 hover:bg-blue-50 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                                <ChevronLeft className="h-4 w-4" />
                        </Button>
                              
                              {/* 页码按钮 */}
                              <div className="flex items-center gap-1 mx-2">
                                {pageNumbers.map((page, index) => {
                                  if (page === '...') {
                                    return (
                                      <span key={`ellipsis-${index}`} className="px-2 py-1 text-slate-400 text-sm">
                                        •••
                        </span>
                                    )
                                  }
                                  
                                  const isActive = page === currentPage
                                  return (
                                    <Button
                                      key={page}
                                      variant={isActive ? "default" : "ghost"}
                                      size="sm"
                                      onClick={() => tableChange({ 
                                        current: page as number, 
                                        pageSize: pagination.pageSize 
                                      })}
                                      className={`h-8 w-8 p-0 transition-all duration-200 ${
                                        isActive 
                                          ? 'bg-blue-600 hover:bg-blue-700 text-white shadow-sm' 
                                          : 'hover:bg-blue-50 hover:text-blue-600 border-0'
                                      }`}
                                    >
                                      {page}
                                    </Button>
                                  )
                                })}
                              </div>
                              
                              {/* 下一页 */}
                        <Button
                          variant="outline"
                          size="sm"
                                disabled={currentPage >= totalPages}
                          onClick={() => tableChange({ 
                                  current: currentPage + 1, 
                            pageSize: pagination.pageSize 
                          })}
                                className="h-8 w-8 p-0 border-slate-200 hover:border-blue-300 hover:bg-blue-50 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                                <ChevronRight className="h-4 w-4" />
                        </Button>
                      </div>
                    </div>
                          
                          {/* 每页显示数量选择 */}
                          <div className="flex items-center justify-end mt-3 pt-3 border-t border-slate-100">
                            <div className="flex items-center gap-2 text-sm text-slate-600">
                              <span>每页显示:</span>
                              <div className="flex items-center gap-1">
                                {[10, 20, 50].map(size => (
                                  <Button
                                    key={size}
                                    variant={pagination.pageSize === size ? "default" : "ghost"}
                                    size="sm"
                                    onClick={() => tableChange({ 
                                      current: 1, 
                                      pageSize: size 
                                    })}
                                    className={`h-7 px-3 text-xs transition-all duration-200 ${
                                      pagination.pageSize === size 
                                        ? 'bg-blue-600 hover:bg-blue-700 text-white shadow-sm' 
                                        : 'hover:bg-blue-50 hover:text-blue-600 border-0'
                                    }`}
                                  >
                                    {size}
                                  </Button>
                                ))}
                              </div>
                              <span className="text-slate-400">条/页</span>
                            </div>
                          </div>
                        </div>
                      </div>
                    )
                  })()}
                </CardContent>
              </Card>
            ) : (
              /* 第4页：日志查看器 - 保持卡片风格 */
              <Card className="h-full bg-white/80 backdrop-blur-sm border-0 shadow-xl">
                <CardContent className="p-0 h-full flex flex-col">
                  <div className="p-3 space-y-2">
                    {/* 日志信息卡片 */}
                    <div className="flex items-center gap-3 p-3 rounded-lg border border-slate-200 bg-slate-50/50">
                      <div className="flex items-center justify-center w-8 h-8 rounded bg-blue-50 border border-blue-200 flex-shrink-0">
                        <Terminal className="h-4 w-4 text-blue-700" />
                      </div>
                      
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between mb-1">
                          <span className="font-medium text-slate-800 truncate">
                            执行日志详情
                          </span>
                          <Badge variant="secondary" className="bg-blue-50 text-blue-700 border-0 text-xs flex-shrink-0">
                            <Terminal className="h-3 w-3 mr-1" />
                            实时日志
                          </Badge>
                        </div>
                        
                        <div className="text-xs text-slate-500">
                          主机命令执行的详细日志输出
                        </div>
                      </div>
                    </div>
                  </div>
                  
                  {/* 日志内容区域 */}
                  <div className="flex-1 mx-3 mb-3 rounded-lg overflow-hidden border border-slate-200 relative">
                    {/* WebSocket连接状态栏 */}
                    <div className="bg-slate-800 px-4 py-2 border-b border-slate-600 flex items-center justify-between">
                      <div className="flex items-center space-x-2">
                        {wsConnected ? (
                          <Wifi className="w-4 h-4 text-green-400" />
                        ) : wsConnecting ? (
                          <div className="w-4 h-4 border-2 border-blue-400 border-t-transparent rounded-full animate-spin" />
                        ) : (
                          <WifiOff className="w-4 h-4 text-red-400" />
                        )}
                        <span className="text-sm text-slate-300">
                          {wsConnected ? '实时日志连接已建立' : wsConnecting ? '正在连接...' : '连接已断开'}
                        </span>
                      </div>
                      {(wsError || error) && (
                        <span className="text-sm text-red-400">
                          {wsError || error}
                        </span>
                      )}
                    </div>
                    
                    {/* 日志内容 */}
                    <div 
                      ref={logScrollContainerRef}
                      onScroll={checkScrollPosition}
                      className="h-full bg-slate-900 p-4 overflow-auto rounded-b-md scrollbar-thin scrollbar-thumb-slate-700 scrollbar-track-slate-800"
                    >
                      {logData ? (
                        <pre className="text-gray-300 font-mono text-sm leading-relaxed whitespace-pre-wrap">
                          {logData}
                        </pre>
                      ) : (
                        <div className="flex flex-col items-center justify-center h-full text-slate-400">
                          <Terminal className="h-8 w-8 mb-3 opacity-50" />
                          <p className="text-sm">暂无日志数据</p>
                          {wsConnecting && (
                            <p className="text-xs mt-1 text-blue-400">正在连接实时日志...</p>
                          )}
                          {!wsConnecting && !wsConnected && (
                            <p className="text-xs mt-1 opacity-75">等待命令执行生成日志...</p>
                          )}
                        </div>
                      )}
                    </div>
                    

                    
                    {/* 临时调试信息 - 显示当前状态 */}
                    {logData && (
                      <div className="absolute top-4 right-4 bg-black/70 text-white text-xs px-2 py-1 rounded z-40">
                        滚动状态: {userScrolledUp ? '已向上滚动' : '在底部'} | 差值检查等待控制台
                      </div>
                    )}
                    


                  </div>
                </CardContent>
              </Card>
            )}
          </div>
          </div>
        </div>
      </ClusterWizardLayout>
      
      {/* 苹果风格悬浮按钮 - 使用固定定位相对于整个视窗 */}
      {(() => {
        console.log('🔍 按钮显示条件:', { userScrolledUp, hasLogData: !!logData, currentPage })
        return null
      })()}
      {currentPage === 4 && (userScrolledUp || true) && logData && (
        <div className="fixed bottom-6 right-6 z-[9999]">
          <div 
            onClick={() => {
              console.log('🔄 点击回到底部按钮')
              scrollToBottom(true)
            }}
            className="group cursor-pointer"
          >
            {/* iOS风格的精美圆形悬浮按钮 */}
            <div className="w-12 h-12 bg-white/15 backdrop-blur-xl border border-white/25 rounded-full flex items-center justify-center shadow-2xl hover:bg-white/25 transition-all duration-300 hover:scale-110 active:scale-95 shadow-black/20">
              <ArrowDown className="h-5 w-5 text-white drop-shadow-lg group-hover:translate-y-1 transition-transform duration-300" />
            </div>
            
            {/* 悬停提示 - iOS风格 */}
            <div className="absolute -top-12 left-1/2 transform -translate-x-1/2 bg-black/90 backdrop-blur-sm text-white text-sm px-3 py-1.5 rounded-lg opacity-0 group-hover:opacity-100 transition-all duration-300 whitespace-nowrap shadow-lg">
              回到底部
              {/* 小箭头 */}
              <div className="absolute top-full left-1/2 transform -translate-x-1/2 w-0 h-0 border-l-4 border-r-4 border-t-4 border-l-transparent border-r-transparent border-t-black/90"></div>
            </div>
          </div>
        </div>
      )}
    </Dialog>
  )
}

export default ServiceInstallDialog
