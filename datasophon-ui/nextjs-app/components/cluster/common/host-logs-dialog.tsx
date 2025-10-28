'use client'

import { useEffect, useState, useRef, useMemo } from 'react'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { FileText, Wrench, Filter, Copy, CheckCircle2, XCircle, Clock } from 'lucide-react'
import { API_BASE_URL } from '@/lib/api-config-v1'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Badge } from '@/components/ui/badge'
import { ScrollArea } from '@/components/ui/scroll-area'

interface HostLogsDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  clusterId: string
  hostIp: string
  hostname: string
}

interface LogDetails {
  isProgress?: boolean
  progress?: number
  command?: string
  output?: string
  error?: string
  [key: string]: unknown
}

interface LogEntry {
  timestamp: string
  level: 'DEBUG' | 'INFO' | 'SUCCESS' | 'WARNING' | 'ERROR'
  type: 'check' | 'repair'
  checkKey: string  // 新增：标识是哪个检查项的日志
  checkName: string  // 新增：检查项显示名称
  stage: string
  message: string
  details?: LogDetails
}

export function HostLogsDialog({
  open,
  onOpenChange,
  clusterId,
  hostIp,
  hostname
}: HostLogsDialogProps) {
  const [activeTab, setActiveTab] = useState<'check' | 'repair'>('check')
  const [logs, setLogs] = useState<LogEntry[]>([])
  const [connectionState, setConnectionState] = useState<'connecting' | 'loading-history' | 'connected' | 'error'>('connecting')
  const [levelFilter, setLevelFilter] = useState<string>('all')
  const [checkKeyFilter, setCheckKeyFilter] = useState<string>('all')
  const logsEndRef = useRef<HTMLDivElement>(null)
  const eventSourceRef = useRef<EventSource | null>(null)
  
  // 获取所有唯一的检查项
  const availableCheckKeys = useMemo(() => {
    const keys = new Map<string, string>()  // checkKey -> checkName
    logs.forEach(log => {
      if (log.checkKey && log.checkName && !keys.has(log.checkKey)) {
        keys.set(log.checkKey, log.checkName)
      }
    })
    return Array.from(keys.entries()).sort((a, b) => a[1].localeCompare(b[1]))
  }, [logs])
  
  // 过滤后的日志
  const filteredLogs = useMemo(() => {
    return logs.filter(log => {
      // 类型过滤（check/repair）
      if (log.type !== activeTab) return false
      
      // 检查项过滤
      if (checkKeyFilter !== 'all' && log.checkKey !== checkKeyFilter) return false
      
      // 日志级别过滤
      if (levelFilter !== 'all' && log.level !== levelFilter) return false
      
      return true
    })
  }, [logs, activeTab, checkKeyFilter, levelFilter])
  
  // 建立SSE连接（主机级别）
  useEffect(() => {
    if (open) {
      // 重置状态
      setLogs([])
      setCheckKeyFilter('all')
      setConnectionState('connecting')
      
      // 建立SSE连接（主机级别，返回所有检查项的日志）
      const sseUrl = `${API_BASE_URL}/api/v1/sse/environment-logs/host/${clusterId}/${hostIp}`
      console.log('建立主机日志SSE连接:', sseUrl)
      
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
      
      // 接收实时日志
      eventSource.addEventListener('log', (event) => {
        try {
          const logEntry = JSON.parse(event.data) as LogEntry
          console.log('收到日志:', logEntry)
          
          setLogs(prev => {
            // 如果是进度日志，更新同一checkKey+stage的最新进度
            if (logEntry.details?.isProgress) {
              const lastIndex = prev.findLastIndex(
                log => log.checkKey === logEntry.checkKey && 
                       log.stage === logEntry.stage && 
                       log.type === logEntry.type &&
                       log.details?.isProgress
              )
              if (lastIndex !== -1) {
                const newLogs = [...prev]
                newLogs[lastIndex] = logEntry
                return newLogs
              }
            }
            // 其他日志正常追加
            return [...prev, logEntry]
          })
        } catch (e) {
          console.error('解析SSE日志失败:', e)
        }
      })
      
      // 错误处理
      eventSource.onerror = (error) => {
        console.error('SSE连接错误:', error)
        setConnectionState('error')
      }
      
      // 清理函数
      return () => {
        console.log('关闭SSE连接')
        eventSource.close()
      }
    }
  }, [open, clusterId, hostIp])
  
  // 自动滚动到底部
  useEffect(() => {
    if (logsEndRef.current) {
      logsEndRef.current.scrollIntoView({ behavior: 'smooth' })
    }
  }, [filteredLogs])
  
  // 复制日志
  const handleCopyLogs = () => {
    const text = filteredLogs.map(log => 
      `[${log.timestamp}] [${log.level}] [${log.checkName}] ${log.message}`
    ).join('\n')
    navigator.clipboard.writeText(text)
  }
  
  // 渲染日志级别徽章
  const renderLevelBadge = (level: string) => {
    const colors: Record<string, string> = {
      'DEBUG': 'bg-gray-500',
      'INFO': 'bg-blue-500',
      'SUCCESS': 'bg-green-500',
      'WARNING': 'bg-yellow-500',
      'ERROR': 'bg-red-500'
    }
    return (
      <Badge className={`${colors[level]} text-white text-xs`}>
        {level}
      </Badge>
    )
  }
  
  // 渲染日志条目
  const renderLogEntry = (log: LogEntry, index: number) => {
    return (
      <div key={index} className="mb-4 p-3 bg-gray-50 dark:bg-gray-800 rounded-md border border-gray-200 dark:border-gray-700">
        <div className="flex items-start gap-2 mb-2">
          <span className="text-xs text-gray-500 dark:text-gray-400 font-mono whitespace-nowrap">
            {log.timestamp}
          </span>
          {renderLevelBadge(log.level)}
          <Badge variant="outline" className="text-xs">
            {log.checkName}
          </Badge>
          <span className="text-sm font-medium flex-1">{log.message}</span>
        </div>
        
        {/* 详细信息 */}
        {log.details && Object.keys(log.details).length > 0 && (
          <div className="mt-2 pl-4 border-l-2 border-gray-300 dark:border-gray-600">
            {/* 进度条 */}
            {log.details.isProgress && typeof log.details.progress === 'number' && (
              <div className="mb-2">
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-xs text-gray-600">进度: {log.details.progress}%</span>
                </div>
                <div className="h-2 bg-gray-200 rounded-full overflow-hidden">
                  <div 
                    className="h-full bg-blue-500 transition-all duration-300"
                    style={{ width: `${log.details.progress}%` }}
                  />
                </div>
              </div>
            )}
            
            {/* 命令 */}
            {log.details.command && (
              <div className="mb-2">
                <span className="text-xs text-gray-600 dark:text-gray-400">命令:</span>
                <pre className="mt-1 p-2 bg-gray-900 text-green-400 rounded text-xs overflow-x-auto">
                  {log.details.command}
                </pre>
              </div>
            )}
            
            {/* 输出 */}
            {log.details.output && (
              <div className="mb-2">
                <span className="text-xs text-gray-600 dark:text-gray-400">输出:</span>
                <pre className="mt-1 p-2 bg-gray-100 dark:bg-gray-900 rounded text-xs overflow-x-auto whitespace-pre-wrap">
                  {log.details.output}
                </pre>
              </div>
            )}
            
            {/* 错误 */}
            {log.details.error && (
              <div className="mb-2">
                <span className="text-xs text-red-600 dark:text-red-400">错误:</span>
                <pre className="mt-1 p-2 bg-red-50 dark:bg-red-900/20 text-red-700 dark:text-red-400 rounded text-xs overflow-x-auto whitespace-pre-wrap">
                  {log.details.error}
                </pre>
              </div>
            )}
            
            {/* 其他详情 */}
            {Object.entries(log.details).map(([key, value]) => {
              if (['isProgress', 'progress', 'command', 'output', 'error'].includes(key)) return null
              return (
                <div key={key} className="text-xs text-gray-600 dark:text-gray-400">
                  <span className="font-medium">{key}:</span> {String(value)}
                </div>
              )
            })}
          </div>
        )}
      </div>
    )
  }
  
  // 渲染日志列表
  const renderLogsTab = (logType: 'check' | 'repair') => {
    const currentLogs = filteredLogs
    
    return (
      <div className="flex flex-col h-full min-h-0">
        {/* 过滤器工具栏 */}
        <div className="flex gap-2 mb-3 flex-shrink-0">
          <Select value={checkKeyFilter} onValueChange={setCheckKeyFilter}>
            <SelectTrigger className="w-[200px]">
              <Filter className="h-4 w-4 mr-2" />
              <SelectValue placeholder="筛选检查项" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">所有检查项</SelectItem>
              {availableCheckKeys.map(([key, name]) => (
                <SelectItem key={key} value={key}>{name}</SelectItem>
              ))}
            </SelectContent>
          </Select>
          
          <Select value={levelFilter} onValueChange={setLevelFilter}>
            <SelectTrigger className="w-[150px]">
              <Filter className="h-4 w-4 mr-2" />
              <SelectValue placeholder="日志级别" />
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
          
          <Button 
            variant="outline" 
            size="sm" 
            onClick={handleCopyLogs}
            className="ml-auto"
          >
            <Copy className="h-4 w-4 mr-2" />
            复制日志
          </Button>
        </div>
        
        {/* 日志内容 */}
        <ScrollArea className="flex-1 min-h-0">
          <div className="pr-4">
            {connectionState === 'connecting' && (
              <div className="flex items-center justify-center p-8">
                <Clock className="h-5 w-5 mr-2 animate-spin" />
                <span className="text-sm text-gray-600">连接中...</span>
              </div>
            )}
            
            {connectionState === 'loading-history' && (
              <div className="flex items-center justify-center p-8">
                <Clock className="h-5 w-5 mr-2 animate-spin" />
                <span className="text-sm text-gray-600">加载历史日志...</span>
              </div>
            )}
            
            {connectionState === 'error' && (
              <div className="flex items-center justify-center p-8 text-red-600">
                <XCircle className="h-5 w-5 mr-2" />
                <span className="text-sm">连接失败</span>
              </div>
            )}
            
            {connectionState === 'connected' && currentLogs.length === 0 && (
              <div className="flex items-center justify-center p-8 text-gray-500">
                <FileText className="h-5 w-5 mr-2" />
                <span className="text-sm">
                  暂无{logType === 'check' ? '检查' : '修复'}日志
                </span>
              </div>
            )}
            
            {connectionState === 'connected' && currentLogs.length > 0 && (
              <>
                {currentLogs.map((log, index) => renderLogEntry(log, index))}
                <div ref={logsEndRef} />
              </>
            )}
          </div>
        </ScrollArea>
      </div>
    )
  }
  
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="w-[90vw] max-w-[1400px] h-[85vh] flex flex-col sm:max-w-[1400px]">
        <DialogHeader className="flex-shrink-0">
          <DialogTitle>主机日志查看</DialogTitle>
          <DialogDescription>
            主机: {hostname || hostIp} ({hostIp})
          </DialogDescription>
        </DialogHeader>
        
        <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as 'check' | 'repair')} className="flex-1 flex flex-col min-h-0">
          <TabsList className="grid w-full grid-cols-2 flex-shrink-0">
            <TabsTrigger value="check">
              <FileText className="h-4 w-4 mr-2" />
              检查日志
              {logs.filter(l => l.type === 'check').length > 0 && (
                <Badge variant="secondary" className="ml-2">
                  {logs.filter(l => l.type === 'check').length}
                </Badge>
              )}
            </TabsTrigger>
            <TabsTrigger value="repair">
              <Wrench className="h-4 w-4 mr-2" />
              修复日志
              {logs.filter(l => l.type === 'repair').length > 0 && (
                <Badge variant="secondary" className="ml-2">
                  {logs.filter(l => l.type === 'repair').length}
                </Badge>
              )}
            </TabsTrigger>
          </TabsList>
          
          <TabsContent value="check" className="flex-1 flex flex-col min-h-0 mt-2">
            {renderLogsTab('check')}
          </TabsContent>
          
          <TabsContent value="repair" className="flex-1 flex flex-col min-h-0 mt-2">
            {renderLogsTab('repair')}
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

