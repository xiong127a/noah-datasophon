'use client'

import { useEffect, useState, useRef, useCallback } from 'react'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Progress } from '@/components/ui/progress'
import { FileText, Wrench, RefreshCw, Copy, Filter } from 'lucide-react'
import { clusterApiV1 } from '@/lib/api-utils-v1'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

interface CheckLogsDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  clusterId: number
  hostIp: string
  checkKey: string
  checkName: string
}

interface LogEntry {
  timestamp: string
  level: 'DEBUG' | 'INFO' | 'SUCCESS' | 'WARNING' | 'ERROR'
  type: 'check' | 'repair'
  stage: string
  message: string
  details?: Record<string, any>
}

export function CheckLogsDialog({
  open,
  onOpenChange,
  clusterId,
  hostIp,
  checkKey,
  checkName
}: CheckLogsDialogProps) {
  const [activeTab, setActiveTab] = useState<'check' | 'repair'>('check')
  const [checkLogs, setCheckLogs] = useState<LogEntry[]>([])
  const [repairLogs, setRepairLogs] = useState<LogEntry[]>([])
  const [loading, setLoading] = useState(false)
  const [levelFilter, setLevelFilter] = useState<string>('all')
  const logsEndRef = useRef<HTMLDivElement>(null)
  const eventSourceRef = useRef<EventSource | null>(null)
  
  const loadLogs = async () => {
    setLoading(true)
    try {
      const response = await clusterApiV1.environmentCheck.getLogs(hostIp, checkKey)
      if (response.data) {
        const checkLogText = response.data.checkLog || '暂无检查日志'
        const repairLogText = response.data.repairLog || '暂无修复日志'
        
        // 解析JSON Lines格式
        setCheckLogs(parseJsonLines(checkLogText))
        setRepairLogs(parseJsonLines(repairLogText))
      }
    } catch (error) {
      console.error('加载日志失败:', error)
      setCheckLogs([])
      setRepairLogs([])
    } finally {
      setLoading(false)
    }
  }
  
  // 加载日志并建立SSE连接
  useEffect(() => {
    if (open) {
      // 1. 首次加载历史日志
      loadLogs()
      
      // 2. 建立SSE连接接收实时日志（检查日志 + 修复日志）
      const sseUrl = `/ddh/api/v1/environment-logs-sse/stream/${clusterId}/${hostIp}/${checkKey}`
      console.log('建立日志SSE连接:', sseUrl)
      
      const eventSource = new EventSource(sseUrl)
      eventSourceRef.current = eventSource
      
      // 连接建立
      eventSource.addEventListener('connected', (event) => {
        console.log('SSE连接已建立:', event.data)
      })
      
      // 接收实时日志（根据type字段分发）
      eventSource.addEventListener('log', (event) => {
        try {
          const logEntry = JSON.parse(event.data) as LogEntry
          console.log('收到实时日志:', logEntry)
          
          // 根据日志类型追加到对应的日志列表
          if (logEntry.type === 'check') {
            setCheckLogs(prev => [...prev, logEntry])
          } else if (logEntry.type === 'repair') {
            setRepairLogs(prev => [...prev, logEntry])
          }
        } catch (e) {
          console.error('解析SSE日志失败:', e)
        }
      })
      
      // 修复完成
      eventSource.addEventListener('complete', (event) => {
        console.log('修复完成:', event.data)
        // SSE连接会自动关闭
      })
      
      // 连接错误
      eventSource.onerror = (error) => {
        console.error('SSE连接错误:', error)
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
  
  const parseJsonLines = (text: string): LogEntry[] => {
    if (!text || text === '暂无检查日志' || text === '暂无修复日志') {
      return []
    }
    
    return text.split('\n')
      .filter(line => line.trim())
      .map(line => {
        try {
          return JSON.parse(line) as LogEntry
        } catch {
          // 如果解析失败，返回一个默认的日志条目
          return {
            timestamp: new Date().toISOString(),
            level: 'INFO' as const,
            type: 'check' as const,
            stage: 'unknown',
            message: line
          }
        }
      })
  }
  
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
        
        {/* 显示进度条 */}
        {isProgressLog && (
          <div className="ml-8 mt-2 w-full max-w-2xl">
            <div className="flex items-center gap-3">
              <Progress value={progress} className="flex-1 h-3" />
              <span className="text-blue-400 font-semibold text-sm whitespace-nowrap">{progress}%</span>
            </div>
            {log.details?.fileName && (
              <div className="mt-1 text-xs text-gray-400">
                <span className="text-gray-500">文件:</span> <span className="text-gray-300">{log.details.fileName}</span>
                {log.details?.totalSize && (
                  <span className="ml-3 text-gray-500">大小: <span className="text-gray-300">{log.details.totalSize}</span></span>
                )}
                {log.details?.elapsedTime && (
                  <span className="ml-3 text-gray-500">耗时: <span className="text-gray-300">{log.details.elapsedTime}</span></span>
                )}
              </div>
            )}
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
          
          <div className="flex gap-2">
            <Button size="sm" variant="outline" onClick={() => copyLogs(filteredLogs)}>
              <Copy className="h-4 w-4 mr-1" />
              复制
            </Button>
            <Button size="sm" variant="outline" onClick={loadLogs} disabled={loading}>
              <RefreshCw className={`h-4 w-4 mr-1 ${loading ? 'animate-spin' : ''}`} />
              刷新
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
        
        <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as any)} className="flex-1 flex flex-col min-h-0">
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
