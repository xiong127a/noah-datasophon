'use client'

import { useEffect, useState } from 'react'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { FileText, Wrench, RefreshCw, Copy, Filter } from 'lucide-react'
import { clusterApiV1 } from '@/lib/api-utils-v1'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

interface CheckLogsDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
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
  hostIp,
  checkKey,
  checkName
}: CheckLogsDialogProps) {
  const [activeTab, setActiveTab] = useState<'check' | 'repair'>('check')
  const [checkLogs, setCheckLogs] = useState<LogEntry[]>([])
  const [repairLogs, setRepairLogs] = useState<LogEntry[]>([])
  const [loading, setLoading] = useState(false)
  const [levelFilter, setLevelFilter] = useState<string>('all')
  
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
  
  // 加载日志
  useEffect(() => {
    if (open) {
      loadLogs()
    }
  }, [open, hostIp, checkKey, loadLogs])
  
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
    return (
      <div key={index} className="mb-3 font-mono text-sm">
        <div className="flex items-start gap-2">
          <span className="text-gray-500 text-xs whitespace-nowrap">{log.timestamp}</span>
          <span className={`px-2 py-0.5 rounded text-xs font-bold whitespace-nowrap ${getLevelBg(log.level)}`}>
            {log.level}
          </span>
          <span className={`flex-1 break-words ${getLevelColor(log.level)}`}>{log.message}</span>
        </div>
        {log.details && Object.keys(log.details).length > 0 && (
          <div className="ml-8 mt-1 text-xs text-gray-400 bg-gray-800 p-3 rounded border border-gray-700 break-all">
            {Object.entries(log.details).map(([key, value]) => (
              <div key={key} className="mb-1 last:mb-0">
                <span className="text-blue-400 font-semibold">{key}:</span>{' '}
                <span className="text-gray-300">{typeof value === 'string' ? value : JSON.stringify(value, null, 2)}</span>
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
      <div className="space-y-2">
        <div className="flex justify-between items-center">
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
        
        <div className="bg-gray-900 text-gray-100 p-4 rounded-lg overflow-auto flex-1 min-h-0">
          {filteredLogs.length === 0 ? (
            <div className="text-gray-500 text-center py-8">
              {logs.length === 0 ? (type === 'check' ? '暂无检查日志' : '暂无修复日志') : '无匹配的日志'}
            </div>
          ) : (
            filteredLogs.map((log, index) => renderLogEntry(log, index))
          )}
        </div>
      </div>
    )
  }
  
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-[90vw] w-[1400px] h-[85vh] flex flex-col">
        <DialogHeader>
          <DialogTitle>{checkName} - 日志查看</DialogTitle>
          <DialogDescription>
            主机: {hostIp}
          </DialogDescription>
        </DialogHeader>
        
        <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as any)}>
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="check">
              <FileText className="h-4 w-4 mr-2" />
              检查日志
            </TabsTrigger>
            <TabsTrigger value="repair">
              <Wrench className="h-4 w-4 mr-2" />
              修复日志
            </TabsTrigger>
          </TabsList>
          
          <TabsContent value="check" className="space-y-2">
            {renderLogsTab(checkLogs, 'check')}
          </TabsContent>
          
          <TabsContent value="repair" className="space-y-2">
            {renderLogsTab(repairLogs, 'repair')}
          </TabsContent>
        </Tabs>
        
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            关闭
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
