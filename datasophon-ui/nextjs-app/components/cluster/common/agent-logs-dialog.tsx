"use client"

import React, { useState, useEffect, useRef } from 'react'
import { X, Terminal, Loader2, CheckCircle2, AlertCircle, WifiOff } from 'lucide-react'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Badge } from "@/components/ui/badge"
import { Progress } from "@/components/ui/progress"
import { API_BASE_URL } from '@/lib/api-config-v1'

/**
 * Agent分发日志对话框
 * 显示单个主机的Agent分发实时日志
 * 
 * @author DataSophon Team
 * @date 2025-01-24
 */

interface AgentLogsDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  clusterId: string  // 使用 string 避免 Long 精度丢失
  hostIp: string
  hostname: string
}

interface LogEntry {
  timestamp: string
  level: string
  stage: string
  message: string
  details?: {
    progress?: number
    uploadedSize?: number
    totalSize?: number
    isProgress?: boolean
    [key: string]: any
  }
}

type ConnectionState = 'connecting' | 'loading-history' | 'connected' | 'error'

export function AgentLogsDialog({
  open,
  onOpenChange,
  clusterId,
  hostIp,
  hostname
}: AgentLogsDialogProps) {
  const [logs, setLogs] = useState<LogEntry[]>([])
  const [connectionState, setConnectionState] = useState<ConnectionState>('connecting')
  const scrollAreaRef = useRef<HTMLDivElement>(null)
  const eventSourceRef = useRef<EventSource | null>(null)

  // 自动滚动到底部
  useEffect(() => {
    if (scrollAreaRef.current) {
      const scrollContainer = scrollAreaRef.current.querySelector('[data-radix-scroll-area-viewport]')
      if (scrollContainer) {
        scrollContainer.scrollTop = scrollContainer.scrollHeight
      }
    }
  }, [logs])

  // 建立SSE连接
  useEffect(() => {
    if (!open) {
      // 关闭对话框时清理连接
      if (eventSourceRef.current) {
        eventSourceRef.current.close()
        eventSourceRef.current = null
      }
      setLogs([])
      setConnectionState('connecting')
      return
    }

    console.log('建立Agent分发日志SSE连接:', { clusterId, hostIp })
    setConnectionState('connecting')
    setLogs([])

    // 构建SSE URL（使用绝对路径）
    const sseUrl = `${API_BASE_URL}/api/v1/sse/agent-distribution/stream/${hostIp}?clusterId=${clusterId}`
    const eventSource = new EventSource(sseUrl, { withCredentials: true })
    eventSourceRef.current = eventSource

    // 连接打开
    eventSource.onopen = () => {
      console.log('SSE连接已建立')
      setConnectionState('loading-history')
    }

    // 接收日志事件
    eventSource.addEventListener('log', (event) => {
      try {
        const logEntry = JSON.parse(event.data) as LogEntry
        console.log('收到日志:', logEntry)
        
        setLogs(prev => {
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
      } catch (e) {
        console.error('解析SSE日志失败:', e)
      }
    })

    // 历史日志加载完成
    eventSource.addEventListener('history-loaded', () => {
      console.log('历史日志加载完成')
      setConnectionState('connected')
    })

    // 连接错误
    eventSource.onerror = (error) => {
      console.error('SSE连接错误:', error)
      setConnectionState('error')
      eventSource.close()
      eventSourceRef.current = null
    }

    // 清理函数
    return () => {
      console.log('关闭SSE连接')
      eventSource.close()
      eventSourceRef.current = null
    }
  }, [open, clusterId, hostIp])

  /**
   * 格式化文件大小
   */
  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)} KB`
    if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
    return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`
  }

  /**
   * 获取日志级别对应的颜色
   */
  const getLevelColor = (level: string) => {
    switch (level) {
      case 'SUCCESS':
        return 'text-green-600'
      case 'ERROR':
        return 'text-red-600'
      case 'WARNING':
        return 'text-amber-600'
      case 'INFO':
        return 'text-blue-600'
      case 'DEBUG':
        return 'text-gray-500'
      default:
        return 'text-gray-700'
    }
  }

  /**
   * 获取日志级别对应的Badge样式
   */
  const getLevelBadge = (level: string) => {
    switch (level) {
      case 'SUCCESS':
        return <Badge className="bg-green-100 text-green-700 text-xs">SUCCESS</Badge>
      case 'ERROR':
        return <Badge className="bg-red-100 text-red-700 text-xs">ERROR</Badge>
      case 'WARNING':
        return <Badge className="bg-amber-100 text-amber-700 text-xs">WARNING</Badge>
      case 'INFO':
        return <Badge className="bg-blue-100 text-blue-700 text-xs">INFO</Badge>
      case 'DEBUG':
        return <Badge className="bg-gray-100 text-gray-600 text-xs">DEBUG</Badge>
      default:
        return <Badge className="bg-gray-100 text-gray-700 text-xs">{level}</Badge>
    }
  }

  /**
   * 格式化输出文本，处理转义字符
   */
  const formatOutput = (output: string) => {
    return output
      .replace(/\\r\\n/g, '\n')  // 替换 \r\n 为真实换行
      .replace(/\\n/g, '\n')      // 替换 \n 为真实换行
      .replace(/\\"/g, '"')       // 替换 \" 为 "
      .replace(/\\t/g, '  ')      // 替换 \t 为空格
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl max-h-[85vh] p-0 flex flex-col">
        <DialogHeader className="px-6 py-4 border-b bg-gradient-to-r from-blue-50 to-indigo-50">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-lg flex items-center justify-center">
                <Terminal className="w-5 h-5 text-white" />
              </div>
              <div>
                <DialogTitle className="text-lg font-bold text-gray-900">
                  Agent分发日志 - {hostname}
                </DialogTitle>
                <p className="text-sm text-gray-600">{hostIp}</p>
              </div>
            </div>
            
            {/* 连接状态指示器 */}
            <div className="flex items-center space-x-2">
              {connectionState === 'connecting' && (
                <Badge className="bg-yellow-100 text-yellow-700 flex items-center space-x-1">
                  <Loader2 className="w-3 h-3 animate-spin" />
                  <span>连接中</span>
                </Badge>
              )}
              {connectionState === 'loading-history' && (
                <Badge className="bg-blue-100 text-blue-700 flex items-center space-x-1">
                  <Loader2 className="w-3 h-3 animate-spin" />
                  <span>加载历史</span>
                </Badge>
              )}
              {connectionState === 'connected' && (
                <Badge className="bg-green-100 text-green-700 flex items-center space-x-1">
                  <CheckCircle2 className="w-3 h-3" />
                  <span>已连接</span>
                </Badge>
              )}
              {connectionState === 'error' && (
                <Badge className="bg-red-100 text-red-700 flex items-center space-x-1">
                  <WifiOff className="w-3 h-3" />
                  <span>连接失败</span>
                </Badge>
              )}
            </div>
          </div>
        </DialogHeader>

        <ScrollArea ref={scrollAreaRef} className="flex-1 px-6 py-4">
          <div className="space-y-2 font-mono text-sm">
            {logs.length === 0 && connectionState === 'connecting' && (
              <div className="flex items-center justify-center py-8 text-gray-500">
                <Loader2 className="w-5 h-5 animate-spin mr-2" />
                正在连接日志服务器...
              </div>
            )}
            
            {logs.length === 0 && connectionState === 'error' && (
              <div className="flex items-center justify-center py-8 text-red-500">
                <AlertCircle className="w-5 h-5 mr-2" />
                连接日志服务器失败
              </div>
            )}
            
            {logs.map((log, index) => (
              <div key={index} className="border-l-2 border-gray-200 pl-3 py-2 hover:bg-gray-50 transition-colors">
                <div className="flex items-start space-x-2">
                  <span className="text-gray-400 text-xs min-w-[180px]">{log.timestamp}</span>
                  {getLevelBadge(log.level)}
                  <span className="text-gray-600 text-xs min-w-[120px]">[{log.stage}]</span>
                </div>
                <div className={`mt-1 ${getLevelColor(log.level)}`}>
                  {log.message}
                </div>
                
                {/* 进度条显示 */}
                {log.details?.isProgress && log.details.progress !== undefined && (
                  <div className="mt-2 space-y-1">
                    <Progress value={log.details.progress} className="h-2" />
                    <div className="flex justify-between text-xs text-gray-500">
                      <span>进度: {log.details.progress}%</span>
                      {log.details.uploadedSize !== undefined && log.details.totalSize !== undefined && (
                        <span>
                          {formatFileSize(log.details.uploadedSize)} / {formatFileSize(log.details.totalSize)}
                        </span>
                      )}
                    </div>
                  </div>
                )}
                
                {/* 其他详情显示 */}
                {log.details && !log.details.isProgress && (
                  <div className="mt-2 text-xs bg-gray-50 rounded p-3 border border-gray-200">
                    {/* 特殊处理 command 字段 */}
                    {log.details.command && (
                      <div className="mb-3">
                        <div className="text-gray-600 font-semibold mb-1">执行命令:</div>
                        <pre className="whitespace-pre-wrap text-gray-700 leading-relaxed font-mono text-xs bg-white rounded p-2 border border-blue-200">
                          {formatOutput(log.details.command)}
                        </pre>
                      </div>
                    )}
                    
                    {/* 特殊处理 output 字段 */}
                    {log.details.output && (
                      <div className="mb-3">
                        <div className="text-gray-600 font-semibold mb-1">执行输出:</div>
                        <pre className="whitespace-pre-wrap text-gray-700 leading-relaxed font-mono text-xs bg-white rounded p-2 border border-gray-200 max-h-60 overflow-y-auto">
                          {formatOutput(log.details.output)}
                        </pre>
                      </div>
                    )}
                    
                    {/* 特殊处理 status 字段 */}
                    {log.details.status && (
                      <div className="mb-2">
                        <span className="text-gray-600 font-semibold">状态: </span>
                        <Badge className={
                          log.details.status === 'running' ? 'bg-blue-100 text-blue-700' :
                          log.details.status === 'success' ? 'bg-green-100 text-green-700' :
                          'bg-gray-100 text-gray-700'
                        }>
                          {log.details.status}
                        </Badge>
                      </div>
                    )}
                    
                    {/* 其他字段 */}
                    {Object.entries(log.details)
                      .filter(([key]) => !['output', 'command', 'status', 'isProgress', 'progress', 'uploadedSize', 'totalSize'].includes(key))
                      .map(([key, value]) => (
                        <div key={key} className="mb-1">
                          <span className="text-gray-600 font-semibold">{key}: </span>
                          <span className="text-gray-700">{typeof value === 'string' ? formatOutput(value) : String(value)}</span>
                        </div>
                      ))
                    }
                  </div>
                )}
              </div>
            ))}
          </div>
        </ScrollArea>
      </DialogContent>
    </Dialog>
  )
}

