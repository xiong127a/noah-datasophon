"use client"

import React, { useState, useEffect, useRef, useCallback, useMemo } from 'react'
import { 
  ChevronLeft, X, RefreshCw, ChevronRight, AlertTriangle, 
  Play, Clock, CheckCircle2, XCircle, AlertCircle
} from 'lucide-react'
import { toast } from 'sonner'
import { Dialog, DialogTitle } from '@/components/ui/dialog'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Progress } from '@/components/ui/progress'
import { Card, CardContent } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'

import ClusterWizardLayout from './cluster-wizard-layout'
import ClusterWizardActionBar from './cluster-wizard-action-bar'
import { apiV1, API_PATHS_V1 } from "@/lib/api-config-v1"
import { createClusterHeaders } from '@/lib/cluster-id-header'
import { Step7Data } from '@/types/service-config'

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
  [key: string]: any // 支持动态属性访问
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
  const [commandName, setCommandName] = useState("")
  const [logData, setLogData] = useState("")
  const [error, setError] = useState<string | null>(null)

  // 定时器引用
  const timer1 = useRef<NodeJS.Timeout | null>(null)
  const timer2 = useRef<NodeJS.Timeout | null>(null)
  const timer3 = useRef<NodeJS.Timeout | null>(null)

  // 计算步骤信息
  const isK8s = clusterType?.toLowerCase() === 'kubernetes'
  const currentStepNumber = isK8s ? 7 : 8

  // 获取服务列表（对应原Vue2的getServiceList方法）
  const getServiceList = useCallback(async (flag?: boolean) => {
    if (!flag) setLoading(true)
    setError(null)
    
    const params: Record<string, any> = {
      pageSize: pagination.pageSize,
      page: pagination.current,
      // 注意：clusterId从请求头传递，不需要在params中
    }
    
    if (currentPage === 2) params.commandId = commandId
    if (currentPage === 3) {
      params.hostname = hostname
      params.commandHostId = commandHostId
    }
    
    const apiPath = currentPage === 1 
      ? API_PATHS_V1.GET_SERVICE_COMMAND_LIST
      : currentPage === 2
      ? API_PATHS_V1.GET_SERVICE_HOST_LIST
      : API_PATHS_V1.GET_SERVICE_ROLE_ORDER_LIST

    try {
      const headers = createClusterHeaders(cluster.id)
      
      // 根据不同页面使用不同的HTTP方法
      const response = currentPage === 1
        ? await apiV1.get(apiPath, { headers, params }) // GET方法，参数作为查询参数
        : await apiV1.post(apiPath, params, { headers }) // 其他页面仍使用POST方法
      
      if (response.data?.code === 200) {
        // API返回的数据结构：{ code: 200, data: { records: [...], total: "4", ... } }
        const responseData = response.data.data || {}
        setDataSource(responseData.records || [])
        setPagination(prev => ({
          ...prev,
          total: parseInt(responseData.total) || 0
        }))
      } else {
        throw new Error(response.data?.message || '获取数据失败')
      }
    } catch (err: any) {
      const errorMsg = err.response?.data?.message || err.message || '获取数据失败'
      setError(errorMsg)
      toast.error(errorMsg)
    } finally {
      setLoading(false)
    }
  }, [pagination.pageSize, pagination.current, cluster.id, currentPage, commandId, hostname, commandHostId])



  // 返回上一级（对应原Vue2的goBack方法）
  const goBack = useCallback(() => {
    // 清除定时器
    if (timer1.current) clearInterval(timer1.current)
    if (timer2.current) clearInterval(timer2.current)
    if (timer3.current) clearInterval(timer3.current)
    
    setCurrentPage(prev => {
      const newPage = prev - 1
      setLoading(true)
      
      if (newPage === 2) {
        setTitle(commandName)
      }
      if (newPage === 1) {
        setTitle("安装并启动服务")
      }
      if (newPage === 3) {
        setTitle(hostname)
      }
      
      setDataSource([])
      setPagination(prev => ({
        ...prev,
        total: 0,
        current: 1
      }))
      
      return newPage
    })
  }, [commandName, hostname])

  // 查看详情（对应原Vue2的seeDetail方法）
  const seeDetail = useCallback(async (row: DataItem) => {
    // 清除定时器
    if (timer1.current) clearInterval(timer1.current)
    if (timer2.current) clearInterval(timer2.current)
    if (timer3.current) clearInterval(timer3.current)
    
    setPagination(prev => ({ ...prev, current: 1 }))
    
    if (currentPage === 3) {
      setLoading(true)
      setHostname(row.hostname || "")
      setCommandHostId(row.hostCommandId || "")
      
      // 获取日志
      try {
        const headers = createClusterHeaders(cluster.id)
        const response = await apiV1.post(API_PATHS_V1.GET_HOST_COMMAND_LOG, {
          hostCommandId: row.hostCommandId,
          clusterId: cluster.id,
        }, { headers })
        
        if (response.data?.code === 200) {
          setLogData(response.data.data || "")
          setCurrentPage(4)
          setTitle("查看日志")
        } else {
          throw new Error(response.data?.message || '获取日志失败')
        }
      } catch (err: any) {
        const errorMsg = err.response?.data?.message || err.message || '获取日志失败'
        setError(errorMsg)
        toast.error(errorMsg)
      } finally {
        setLoading(false)
      }
      return
    }
    
    setCurrentPage(prev => {
      const newPage = prev + 1
      setLoading(true)
      
      if (newPage === 2) {
        setCommandName(row.commandName || "")
        setTitle(row.commandName || "")
        setCommandId(row.commandId || "")
      }
      if (newPage === 3) {
        setTitle(row.hostname || "")
        setCommandHostId(row.hostCommandId || "")
        setHostname(row.hostname || "")
      }
      
      setDataSource([])
      setPagination(prev => ({
        ...prev,
        total: 0
      }))
      
      return newPage
    })
  }, [currentPage, cluster.id])

  // 轮询搜索（对应原Vue2的pollingSearch方法）
  const pollingSearch = useCallback(() => {
    getServiceList() // 先立马刷一次
    
    const currentTimer = currentPage === 1 ? timer1 : currentPage === 2 ? timer2 : timer3
    
    if (currentTimer.current) {
      clearInterval(currentTimer.current)
    }
    
    currentTimer.current = setInterval(() => {
      getServiceList(true)
    }, 3000) // 3秒间隔
  }, [getServiceList, currentPage])

  // 重试主机（对应原Vue2的retryHost方法）
  const retryHost = useCallback(async (row: DataItem | "all") => {
    let commandIds = ""
    
    if (row === "all") {
      if (selectedRowKeys.length < 1) {
        toast.warning("请至少选择一条命令！")
        return
      }
      commandIds = selectedRowKeys.join(",")
    } else {
      commandIds = row.commandId || ""
    }
    
    const params = {
      commandIds,
      commandType: "INSTALL_SERVICE", // 固定值，对应原Vue2的steps.commandType
      clusterId: cluster.id,
    }
    
    try {
      const headers = createClusterHeaders(cluster.id)
      const response = await apiV1.post(API_PATHS_V1.START_EXECUTE_COMMAND, params, { headers })
      
      if (response.data?.code === 200) {
        setSelectedRowKeys([])
        toast.success("操作成功")
        pollingSearch()
      } else {
        throw new Error(response.data?.message || '重试失败')
      }
    } catch (err: any) {
      const errorMsg = err.response?.data?.message || err.message || '重试失败'
      setError(errorMsg)
      toast.error(errorMsg)
    }
  }, [selectedRowKeys, cluster.id, pollingSearch])

  // 表格分页变化
  const tableChange = useCallback((newPagination: { current: number; pageSize: number }) => {
    setPagination(prev => ({
      ...prev,
      current: newPagination.current,
      pageSize: newPagination.pageSize
    }))
  }, [])

  // 表格列定义（对应原Vue2的computed columns）
  const columns = useMemo(() => {
    const baseColumns = [
      {
        title: "序号",
        key: "index",
        width: 120,
        render: (_: unknown, __: DataItem, index: number) => {
          const pageIndex = pagination.current === 1 
            ? index + 1 
            : index + 1 + pagination.pageSize * (pagination.current - 1)
          return <span className="font-medium text-slate-600">{pageIndex}</span>
        },
      },
      {
        title: currentPage === 1 
          ? "命令" 
          : currentPage === 2 
          ? "主机" 
          : "指令名称",
        key: currentPage === 2 ? "hostname" : "commandName",
        dataIndex: currentPage === 2 ? "hostname" : "commandName",
        width: 300,
        render: (text: string, row: DataItem) => {
          return currentPage !== 3 ? (
            <button
              type="button"
              onClick={() => seeDetail(row)}
              className="text-blue-600 hover:text-blue-800 font-medium hover:underline transition-colors"
            >
              {text}
            </button>
          ) : (
            <span className="font-medium text-slate-700">{text}</span>
          )
        },
      },
      {
        title: "状态",
        key: "commandProgress",
        dataIndex: "commandProgress",
        render: (progress: number, row: DataItem) => {
          const getProgressStatus = (stateCode: number) => {
            switch (stateCode) {
              case 1: return { color: "blue", status: "进行中", icon: <Play className="h-4 w-4" /> }
              case 2: return { color: "green", status: "完成", icon: <CheckCircle2 className="h-4 w-4" /> }
              case 3: return { color: "red", status: "失败", icon: <XCircle className="h-4 w-4" /> }
              case 4: return { color: "yellow", status: "警告", icon: <AlertCircle className="h-4 w-4" /> }
              default: return { color: "gray", status: "未知", icon: <AlertTriangle className="h-4 w-4" /> }
            }
          }
          
          const { color, status, icon } = getProgressStatus(row.commandStateCode)
          
          return (
            <div className="flex items-center gap-3">
              <div className="flex items-center gap-1">
                <span className={`text-${color}-600`}>{icon}</span>
                <span className={`text-xs font-medium text-${color}-600`}>{status}</span>
              </div>
              <Progress 
                value={progress} 
                className="flex-1 h-2"
                style={{
                  backgroundColor: color === 'blue' ? '#3b82f6' : 
                                   color === 'green' ? '#10b981' : 
                                   color === 'red' ? '#ef4444' : 
                                   color === 'yellow' ? '#f59e0b' : '#6b7280'
                }}
              />
              <span className="text-xs font-medium text-slate-600 min-w-[3rem]">{progress}%</span>
            </div>
          )
        },
      },
    ]
    
    // 添加额外列
    if (currentPage === 1) {
      baseColumns.push(
        {
          title: "开始时间",
          key: "createTime",
          dataIndex: "createTime",
          width: 180,
          render: (text: string) => (
            <span className="text-sm text-slate-600">{text}</span>
          )
        },
        {
          title: "持续时间",
          key: "durationTime", 
          dataIndex: "durationTime",
          width: 160,
          render: (text: string) => (
            <div className="flex items-center gap-1">
              <Clock className="h-3 w-3 text-slate-400" />
              <span className="text-sm text-slate-600">{text}</span>
            </div>
          )
        }
      )
    }
    
    if (currentPage === 3) {
      baseColumns.push({
        title: "日志信息",
        key: "resultMsg",
        dataIndex: "resultMsg",
        render: (_: unknown, row: DataItem) => (
          <button
            type="button"
            onClick={() => seeDetail(row)}
            className="text-blue-600 hover:text-blue-800 font-medium hover:underline transition-colors"
          >
            查看日志
          </button>
        ),
      })
    }
    
    return baseColumns
  }, [currentPage, pagination.current, pagination.pageSize, seeDetail])

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
      pollingSearch()
    }
    
    return () => {
      if (timer1.current) clearInterval(timer1.current)
      if (timer2.current) clearInterval(timer2.current)
      if (timer3.current) clearInterval(timer3.current)
    }
  }, [open, pollingSearch])

  // 当currentPage变化时重新开始轮询
  useEffect(() => {
    if (open && currentPage > 0) {
      pollingSearch()
    }
  }, [currentPage, open, pollingSearch])

  // 分页变化时重新获取数据
  useEffect(() => {
    if (open) {
      pollingSearch()
    }
  }, [pagination.current, pagination.pageSize, open, pollingSearch])

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogTitle className="sr-only">服务安装 - {cluster?.clusterName}</DialogTitle>
      <ClusterWizardLayout
        open={open}
        onClose={handleCancel}
        clusterName={cluster?.clusterName}
        stepTitle="安装并启动服务"
        stepDescription="监控服务安装和启动进度"
        dialogTitle={`服务安装 - ${cluster?.clusterName}`}
        currentStep={currentStepNumber}
        actionBar={actionBar}
      >
        <div className="flex-1 flex flex-col min-h-0 p-6 bg-gradient-to-br from-slate-50/30 via-white/50 to-blue-50/20">
          {/* 标题区域 */}
          <div className="flex items-center justify-between mb-6 pb-4 border-b border-slate-200">
            <div className="flex items-center gap-4">
              {currentPage !== 1 && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={goBack}
                  className="flex items-center gap-2 text-blue-600 hover:text-blue-800 hover:bg-blue-50"
                >
                  <ChevronLeft className="h-4 w-4" />
                  返回
                </Button>
              )}
              <div>
                <h1 className="text-2xl font-bold bg-gradient-to-r from-slate-900 to-slate-600 bg-clip-text text-transparent">
                  {title}
                </h1>
                {currentPage === 1 && (
                  <p className="text-sm text-slate-600 mt-1">监控服务安装和启动进度</p>
                )}
              </div>
            </div>
            
            <Button
              variant="ghost"
              size="sm"
              onClick={handleCancel}
              className="h-8 w-8 p-0 hover:bg-red-50 hover:text-red-600 transition-colors"
            >
              <X className="h-4 w-4" />
            </Button>
          </div>

          {/* 错误提示 */}
          {error && (
            <Alert variant="destructive" className="mb-6">
              <AlertTriangle className="h-4 w-4" />
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          {/* 主要内容区域 */}
          <div className="flex-1 min-h-0">
            {currentPage <= 3 ? (
              <Card className="h-full bg-white/80 backdrop-blur-sm border-0 shadow-xl">
                <CardContent className="p-0 h-full flex flex-col">
                  {/* 工具栏 */}
                  {currentPage === 1 && selectedRowKeys.length > 0 && (
                    <div className="p-4 border-b bg-blue-50/50 flex items-center justify-between">
                      <span className="text-sm text-slate-600">
                        已选择 {selectedRowKeys.length} 项
                      </span>
                      <Button
                        size="sm"
                        onClick={() => retryHost("all")}
                        className="bg-blue-600 hover:bg-blue-700"
                      >
                        <RefreshCw className="h-4 w-4 mr-2" />
                        批量重试
                      </Button>
                    </div>
                  )}
                  
                  {/* 表格容器 */}
                  <div className="flex-1 overflow-hidden">
                    <div className="h-full overflow-auto">
                      <table className="w-full">
                        <thead className="bg-slate-50 sticky top-0 z-10">
                          <tr>
                            {currentPage === 1 && (
                              <th className="w-12 p-4">
                                <Checkbox
                                  checked={selectedRowKeys.length === dataSource.length && dataSource.length > 0}
                                  onCheckedChange={(checked) => {
                                    if (checked) {
                                      setSelectedRowKeys(dataSource.map(item => item.commandId || "").filter(Boolean))
                                    } else {
                                      setSelectedRowKeys([])
                                    }
                                  }}
                                />
                              </th>
                            )}
                            {columns.map((col) => (
                              <th
                                key={col.key}
                                className="text-left p-4 font-semibold text-slate-700 border-b"
                                style={{ width: col.width }}
                              >
                                {col.title}
                              </th>
                            ))}
                          </tr>
                        </thead>
                        <tbody>
                          {loading ? (
                            <tr>
                              <td colSpan={columns.length + (currentPage === 1 ? 1 : 0)} className="p-8 text-center">
                                <div className="flex items-center justify-center gap-2">
                                  <RefreshCw className="h-4 w-4 animate-spin text-blue-600" />
                                  <span className="text-slate-600">加载中...</span>
                                </div>
                              </td>
                            </tr>
                          ) : dataSource.length === 0 ? (
                            <tr>
                              <td colSpan={columns.length + (currentPage === 1 ? 1 : 0)} className="p-8 text-center text-slate-500">
                                暂无数据
                              </td>
                            </tr>
                          ) : (
                            dataSource.map((item, index) => (
                              <tr 
                                key={item.commandId || item.hostCommandId || index}
                                className="hover:bg-slate-50/80 transition-colors border-b border-slate-100"
                              >
                                {currentPage === 1 && (
                                  <td className="p-4">
                                    <Checkbox
                                      checked={selectedRowKeys.includes(item.commandId || "")}
                                      onCheckedChange={(checked) => {
                                        const commandId = item.commandId || ""
                                        if (checked) {
                                          setSelectedRowKeys(prev => [...prev, commandId])
                                        } else {
                                          setSelectedRowKeys(prev => prev.filter(key => key !== commandId))
                                        }
                                      }}
                                    />
                                  </td>
                                )}
                                {columns.map((col) => (
                                  <td key={col.key} className="p-4">
                                    {col.render ? col.render(item[col.dataIndex || col.key], item, index) : item[col.dataIndex || col.key]}
                                  </td>
                                ))}
                              </tr>
                            ))
                          )}
                        </tbody>
                      </table>
                    </div>
                  </div>
                  
                  {/* 分页 */}
                  {pagination.total > 0 && (
                    <div className="p-4 border-t bg-slate-50/50 flex justify-between items-center">
                      <span className="text-sm text-slate-600">
                        {pagination.showTotal(pagination.total)}
                      </span>
                      <div className="flex items-center gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          disabled={pagination.current <= 1}
                          onClick={() => tableChange({ 
                            current: pagination.current - 1, 
                            pageSize: pagination.pageSize 
                          })}
                        >
                          上一页
                        </Button>
                        <span className="text-sm text-slate-600">
                          {pagination.current} / {Math.ceil(pagination.total / pagination.pageSize)}
                        </span>
                        <Button
                          variant="outline"
                          size="sm"
                          disabled={pagination.current >= Math.ceil(pagination.total / pagination.pageSize)}
                          onClick={() => tableChange({ 
                            current: pagination.current + 1, 
                            pageSize: pagination.pageSize 
                          })}
                        >
                          下一页
                        </Button>
                      </div>
                    </div>
                  )}
                </CardContent>
              </Card>
            ) : (
              /* 日志查看 */
              <Card className="h-full bg-white/80 backdrop-blur-sm border-0 shadow-xl">
                <CardContent className="p-6 h-full">
                  <div className="h-full bg-slate-900 rounded-lg p-4 overflow-auto">
                    <pre className="text-green-400 font-mono text-sm whitespace-pre-wrap">
                      {logData || "暂无日志数据"}
                    </pre>
                  </div>
                </CardContent>
              </Card>
            )}
          </div>
        </div>
      </ClusterWizardLayout>
    </Dialog>
  )
}

export default ServiceInstallDialog
