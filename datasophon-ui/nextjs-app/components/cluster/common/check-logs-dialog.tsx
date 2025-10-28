'use client'

import { useEffect, useState, useRef } from 'react'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { FileText, Wrench, RefreshCw, Copy, Filter } from 'lucide-react'
import { API_BASE_URL } from '@/lib/api-config-v1'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

interface CheckLogsDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  clusterId: string  // 使用 string 避免 Long 精度丢失
  hostIp: string
  checkKey: string
  checkName: string
  onRepairSuccess?: () => void  // 修复成功后的回调
}

interface LogDetails {
  isProgress?: boolean
  progress?: number
  fileName?: string
  totalSize?: string
  uploadedSize?: string
  elapsedTime?: string
  [key: string]: unknown
}

interface LogEntry {
  timestamp: string
  level: 'DEBUG' | 'INFO' | 'SUCCESS' | 'WARNING' | 'ERROR'
  type: 'check' | 'repair'
  stage: string
  message: string
  details?: LogDetails
}

export function CheckLogsDialog({
  open,
  onOpenChange,
  clusterId,
  hostIp,
  checkKey,
  checkName,
  onRepairSuccess
}: CheckLogsDialogProps) {
  const [activeTab, setActiveTab] = useState<'check' | 'repair'>('repair') // 默认显示修复日志
  const [checkLogs, setCheckLogs] = useState<LogEntry[]>([])
  const [repairLogs, setRepairLogs] = useState<LogEntry[]>([])
  const [connectionState, setConnectionState] = useState<'connecting' | 'loading-history' | 'connected' | 'error'>('connecting')
  const [levelFilter, setLevelFilter] = useState<string>('all')
  const logsEndRef = useRef<HTMLDivElement>(null)
  const eventSourceRef = useRef<EventSource | null>(null)
  
  // 建立SSE连接（作为唯一数据源）
  useEffect(() => {
    if (open) {
      // 重置状态
      setCheckLogs([])
      setRepairLogs([])
      setConnectionState('connecting')
      
      // 建立SSE连接（历史日志和实时日志都通过SSE推送）
      const sseUrl = `${API_BASE_URL}/api/v1/sse/environment-logs/stream/${clusterId}/${hostIp}/${checkKey}`
      console.log('建立日志SSE连接:', sseUrl)
      
      const eventSource = new EventSource(sseUrl)
      eventSourceRef.current = eventSource
      
      // 连接建立
      eventSource.addEventListener('connected', (event) => {
        console.log('SSE连接已建立:', event.data)
        setConnectionState('loading-history')
      })
      
      // 历史日志加载完成
      eventSource.addEventListener('history-loaded', (event) => {
        console.log('历史日志加载完成:', event.data)
        setConnectionState('connected')
      })
      
      // 接收实时日志（根据type字段分发）
      eventSource.addEventListener('log', (event) => {
        try {
          const logEntry = JSON.parse(event.data) as LogEntry
          console.log('收到日志:', logEntry)
          
          // 根据日志类型追加到对应的日志列表
          if (logEntry.type === 'check') {
            setCheckLogs(prev => {
              // 如果是进度日志，更新同一stage的最新进度
              if (logEntry.details?.isProgress) {
                const lastIndex = prev.findLastIndex(
                  log => log.stage === logEntry.stage && log.details?.isProgress
                )
                if (lastIndex !== -1) {
                  // 替换同一stage的进度日志
                  const newLogs = [...prev]
                  newLogs[lastIndex] = logEntry
                  return newLogs
                }
              }
              // 其他日志正常追加
              return [...prev, logEntry]
            })
          } else if (logEntry.type === 'repair') {
            setRepairLogs(prev => {
              // 如果是进度日志，更新同一stage的最新进度
              if (logEntry.details?.isProgress) {
                const lastIndex = prev.findLastIndex(
                  log => log.stage === logEntry.stage && log.details?.isProgress
                )
                if (lastIndex !== -1) {
                  // 替换同一stage的进度日志
                  const newLogs = [...prev]
                  newLogs[lastIndex] = logEntry
                  return newLogs
                }
              }
              // 其他日志正常追加
              return [...prev, logEntry]
            })
          }
        } catch (e) {
          console.error('解析SSE日志失败:', e)
        }
      })
      
      // 修复完成
      eventSource.addEventListener('complete', (event) => {
        console.log('修复完成:', event.data)
        try {
          const data = JSON.parse(event.data)
          // 如果修复成功，通知父组件刷新检查状态
          if (data.success && onRepairSuccess) {
            console.log('修复成功，通知父组件刷新检查状态')
            onRepairSuccess()
          }
        } catch (e) {
          console.error('解析修复完成事件失败:', e)
        }
      })
      
      // 连接错误
      eventSource.onerror = (error) => {
        console.error('SSE连接错误:', error)
        setConnectionState('error')
        eventSource.close()
      }
      
      // 清理函数
      return () => {
        console.log('关闭SSE连接')
        eventSource.close()
        eventSourceRef.current = null
      }
    }
  }, [open, clusterId, hostIp, checkKey])
  
  // 自动滚动到底部
  useEffect(() => {
    logsEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [checkLogs, repairLogs])
  
  const getLevelColor = (level: string) => {
    switch (level) {
      case 'DEBUG':
        return 'text-gray-400'
      case 'INFO':
        return 'text-blue-400'
      case 'SUCCESS':
        return 'text-green-400'
      case 'WARNING':
        return 'text-yellow-400'
      case 'ERROR':
        return 'text-red-400'
      default:
        return 'text-gray-400'
    }
  }
  
  const getLevelBg = (level: string) => {
    switch (level) {
      case 'DEBUG':
        return 'bg-gray-700'
      case 'INFO':
        return 'bg-blue-700'
      case 'SUCCESS':
        return 'bg-green-700'
      case 'WARNING':
        return 'bg-yellow-700'
      case 'ERROR':
        return 'bg-red-700'
      default:
        return 'bg-gray-700'
    }
  }
  
  const filterLogs = (logs: LogEntry[]) => {
    if (levelFilter === 'all') {
      return logs
    }
    return logs.filter(log => log.level === levelFilter)
  }
  
  const copyLogs = (logs: LogEntry[]) => {
    const text = logs.map(log => 
      `[${log.timestamp}] [${log.level}] ${log.message}`
    ).join('\n')
    navigator.clipboard.writeText(text)
  }
  
  const renderLogEntry = (log: LogEntry, index: number) => {
    // 检测是否为进度日志
    const isProgressLog = log.details?.isProgress === true
    const progress = isProgressLog ? (log.details?.progress || 0) : 0
    
    return (
      <div key={index} className="mb-3 font-mono text-sm w-full max-w-none">
        <div className="flex items-start gap-2 w-full">
          <span className="text-gray-500 text-xs whitespace-nowrap flex-shrink-0">{log.timestamp}</span>
          <span className={`px-2 py-0.5 rounded text-xs font-bold whitespace-nowrap flex-shrink-0 ${getLevelBg(log.level)}`}>
            {log.level}
          </span>
          <span className={`flex-1 break-words min-w-0 ${getLevelColor(log.level)}`}>{log.message}</span>
        </div>
        
        {/* 显示进度条 - 优化样式和动画 */}
        {isProgressLog && (
          <div className="ml-8 mt-3 w-full max-w-3xl">
            {/* 文件信息 */}
            {log.details?.fileName && (
              <div className="mb-2 flex items-center gap-3 text-sm">
                <span className="text-cyan-400 font-medium">📦 {log.details.fileName}</span>
                {log.details?.uploadedSize && log.details?.totalSize ? (
                  <span className="text-emerald-400 font-semibold">
                    {log.details.uploadedSize} / {log.details.totalSize}
                  </span>
                ) : log.details?.totalSize ? (
                  <span className="text-gray-400">({log.details.totalSize})</span>
                ) : null}
              </div>
            )}
            
            {/* 进度条容器 */}
            <div className="relative">
              {/* 背景轨道 */}
              <div className="h-6 bg-gray-800 rounded-full overflow-hidden border border-gray-700 shadow-inner">
                {/* 进度条 - 添加过渡动画 */}
                <div 
                  className="h-full bg-gradient-to-r from-cyan-500 via-blue-500 to-indigo-600 transition-all duration-500 ease-out relative overflow-hidden"
                  style={{ width: `${progress}%` }}
                >
                  {/* 动画效果 - 闪光 */}
                  <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white to-transparent opacity-30 animate-pulse"></div>
                </div>
              </div>
              
              {/* 百分比显示 */}
              <div className="absolute inset-0 flex items-center justify-center">
                <span className="text-white font-bold text-sm drop-shadow-lg">{progress}%</span>
              </div>
            </div>
            
            {/* 统计信息 */}
            <div className="mt-2 flex items-center gap-4 text-xs text-gray-400 flex-wrap">
              {/* 已传输 / 总大小 */}
              {log.details?.uploadedSize && log.details?.totalSize && (
                <span className="flex items-center gap-1">
                  <span className="text-gray-500">📊 已传输:</span>
                  <span className="text-emerald-400 font-semibold">
                    {log.details.uploadedSize} / {log.details.totalSize}
                  </span>
                </span>
              )}
              
              {/* 已用时 */}
              {log.details?.elapsedTime && (
                <span className="flex items-center gap-1">
                  <span className="text-gray-500">⏱️ 耗时:</span>
                  <span className="text-cyan-400 font-medium">{log.details.elapsedTime}</span>
                </span>
              )}
              
              {/* 状态 */}
              {progress > 0 && progress < 100 && (
                <span className="flex items-center gap-1">
                  <span className="text-green-400 font-medium">⚡ 上传中...</span>
                </span>
              )}
              {progress === 100 && (
                <span className="flex items-center gap-1">
                  <span className="text-green-400 font-bold">✅ 上传完成</span>
                </span>
              )}
            </div>
          </div>
        )}
        
        {/* 显示详细信息（非进度日志） */}
        {log.details && Object.keys(log.details).length > 0 && !isProgressLog && (
          <div className="ml-8 mt-1 text-xs text-gray-400 bg-gray-800 p-3 rounded border border-gray-700 break-all w-full max-w-none">
            {Object.entries(log.details).map(([key, value]) => (
              <div key={key} className="mb-1 last:mb-0 w-full">
                <span className="text-blue-400 font-semibold">{key}:</span>{' '}
                <span className="text-gray-300 break-all">{typeof value === 'string' ? value : JSON.stringify(value, null, 2)}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    )
  }
  
  const renderLogsTab = (logs: LogEntry[], type: 'check' | 'repair') => {
    const filteredLogs = filterLogs(logs)
    
    return (
      <div className="flex flex-col h-full space-y-2">
        <div className="flex justify-between items-center flex-shrink-0">
          <div className="flex items-center gap-2">
            <Filter className="h-4 w-4 text-gray-400" />
            <Select value={levelFilter} onValueChange={setLevelFilter}>
              <SelectTrigger className="w-32 h-8">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">全部</SelectItem>
                <SelectItem value="DEBUG">DEBUG</SelectItem>
                <SelectItem value="INFO">INFO</SelectItem>
                <SelectItem value="SUCCESS">SUCCESS</SelectItem>
                <SelectItem value="WARNING">WARNING</SelectItem>
                <SelectItem value="ERROR">ERROR</SelectItem>
              </SelectContent>
            </Select>
          </div>
          
          <div className="flex gap-2 items-center">
            {/* 连接状态指示器 */}
            <div className="text-xs text-gray-500 flex items-center gap-1">
              {connectionState === 'connecting' && (
                <>
                  <RefreshCw className="h-3 w-3 animate-spin" />
                  连接中...
                </>
              )}
              {connectionState === 'loading-history' && (
                <>
                  <RefreshCw className="h-3 w-3 animate-spin" />
                  加载历史日志...
                </>
              )}
              {connectionState === 'connected' && (
                <>
                  <div className="h-2 w-2 rounded-full bg-green-500 animate-pulse" />
                  实时推送中
                </>
              )}
              {connectionState === 'error' && (
                <>
                  <div className="h-2 w-2 rounded-full bg-red-500" />
                  连接错误
                </>
              )}
            </div>
            
            <Button size="sm" variant="outline" onClick={() => copyLogs(filteredLogs)}>
              <Copy className="h-4 w-4 mr-1" />
              复制
            </Button>
          </div>
        </div>
        
        <div className="bg-gray-900 text-gray-100 p-4 rounded-lg overflow-auto flex-1 w-full max-w-none">
          {filteredLogs.length === 0 ? (
            <div className="text-gray-500 text-center py-8">
              {logs.length === 0 ? (type === 'check' ? '暂无检查日志' : '暂无修复日志') : '无匹配的日志'}
            </div>
          ) : (
            <div className="w-full max-w-none">
              {filteredLogs.map((log, index) => renderLogEntry(log, index))}
              {/* 滚动锚点 */}
              <div ref={logsEndRef} />
            </div>
          )}
        </div>
      </div>
    )
  }
  
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="w-[90vw] max-w-[1400px] h-[85vh] flex flex-col sm:max-w-[1400px]">
        <DialogHeader className="flex-shrink-0">
          <DialogTitle>{checkName} - 日志查看</DialogTitle>
          <DialogDescription>
            主机: {hostIp}
          </DialogDescription>
        </DialogHeader>
        
        <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as 'check' | 'repair')} className="flex-1 flex flex-col min-h-0">
          <TabsList className="grid w-full grid-cols-2 flex-shrink-0">
            <TabsTrigger value="check">
              <FileText className="h-4 w-4 mr-2" />
              检查日志
            </TabsTrigger>
            <TabsTrigger value="repair">
              <Wrench className="h-4 w-4 mr-2" />
              修复日志
            </TabsTrigger>
          </TabsList>
          
          <TabsContent value="check" className="flex-1 flex flex-col min-h-0 mt-2">
            {renderLogsTab(checkLogs, 'check')}
          </TabsContent>
          
          <TabsContent value="repair" className="flex-1 flex flex-col min-h-0 mt-2">
            {renderLogsTab(repairLogs, 'repair')}
          </TabsContent>
        </Tabs>
        
        <DialogFooter className="flex-shrink-0">
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            关闭
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
