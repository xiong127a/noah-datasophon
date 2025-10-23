import { useEffect, useRef, useState, useCallback } from 'react'
import { toast } from 'sonner'
import { API_BASE_URL, API_PATHS_V1 } from '@/lib/api-config-v1'

interface UseLogSSEProps {
  clusterId?: string
  hostCommandId?: string
  onLogUpdate?: (logContent: string, updateType?: 'replace' | 'append') => void
  enabled?: boolean
}

export const useLogSSE = ({
  clusterId,
  hostCommandId,
  onLogUpdate,
  enabled = true
}: UseLogSSEProps) => {
  const [isConnected, setIsConnected] = useState(false)
  const [isConnecting, setIsConnecting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [logContent, setLogContent] = useState('')
  
  const eventSourceRef = useRef<EventSource | null>(null)
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null)
  const reconnectAttempts = useRef(0)
  const maxReconnectAttempts = 5
  
  // 使用ref来存储回调函数，避免重新创建导致的依赖问题
  const onLogUpdateRef = useRef(onLogUpdate)
  useEffect(() => {
    onLogUpdateRef.current = onLogUpdate
  }, [onLogUpdate])
  
  const cleanup = useCallback(() => {
    // 清理EventSource连接
    if (eventSourceRef.current) {
      eventSourceRef.current.close()
      eventSourceRef.current = null
    }
    
    // 清理重连定时器
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current)
      reconnectTimeoutRef.current = null
    }
    
    setIsConnected(false)
    setIsConnecting(false)
  }, [])
  
  const connect = useCallback(() => {
    if (!enabled || !clusterId || !hostCommandId || eventSourceRef.current) {
      return
    }
    
    setIsConnecting(true)
    setError(null)
    
    try {
      // 🔧 构建SSE URL - 使用API配置中的路径
      const sseUrl = new URL(`${API_BASE_URL}${API_PATHS_V1.LOG_STREAM_SSE}`, window.location.origin)
      sseUrl.searchParams.set('clusterId', clusterId)
      sseUrl.searchParams.set('hostCommandId', hostCommandId)
      
      // 添加认证token（EventSource不支持自定义headers，只能用URL参数）
      const token = localStorage.getItem('jwt_token')
      if (token) {
        sseUrl.searchParams.set('token', token)
      }
      
      console.log('建立SSE连接:', sseUrl.toString())
      
      // 创建EventSource连接
      const eventSource = new EventSource(sseUrl.toString())
      eventSourceRef.current = eventSource
      
      // 🔧 清空日志内容，准备接收新内容
      setLogContent('')
      onLogUpdateRef.current?.('', 'replace')
      
      // 连接打开
      eventSource.onopen = () => {
        console.log('SSE连接建立成功')
        setIsConnected(true)
        setIsConnecting(false)
        setError(null)
        reconnectAttempts.current = 0
      }
      
      // 接收消息
      eventSource.onmessage = (event) => {
        console.log('收到SSE消息:', event)
        handleSSEMessage(event.type || 'message', event.data)
      }
      
      // 监听特定事件类型
      eventSource.addEventListener('connection', (event: MessageEvent) => {
        console.log('SSE连接确认:', event.data)
        toast.success('日志连接建立成功')
      })
      
      eventSource.addEventListener('log', (event: MessageEvent) => {
        // 日志内容 - 追加模式（SSE统一处理）
        setLogContent(prevContent => {
          const newContent = prevContent + event.data
          onLogUpdateRef.current?.(newContent, 'append')
          return newContent
        })
      })
      
      eventSource.addEventListener('error', (event: MessageEvent) => {
        console.error('收到服务器错误:', event.data)
        setError(event.data)
        toast.error(`日志获取失败: ${event.data}`)
      })
      
      // 连接错误
      eventSource.onerror = (event) => {
        console.error('SSE连接错误:', event)
        setIsConnected(false)
        setIsConnecting(false)
        
        // 检查是否是认证失败
        if (eventSource.readyState === EventSource.CLOSED) {
          const errorMsg = '连接已关闭，可能是认证失败'
          setError(errorMsg)
          console.error(errorMsg)
          
          // 清理并尝试重连
          cleanup()
          setTimeout(() => scheduleReconnect(), 100)
        } else {
          setError('SSE连接发生错误')
          setTimeout(() => scheduleReconnect(), 100)
        }
      }
      
    } catch (error) {
      console.error('创建SSE连接失败:', error)
      setError('创建SSE连接失败')
      setIsConnecting(false)
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled, clusterId, hostCommandId, cleanup])
  
  const scheduleReconnect = useCallback(() => {
    if (reconnectAttempts.current >= maxReconnectAttempts) {
      console.error('达到最大重连次数，停止重连')
      setError('连接失败次数过多，已停止重连')
      return
    }
    
    reconnectAttempts.current++
    const delay = Math.min(1000 * Math.pow(2, reconnectAttempts.current - 1), 30000) // 指数退避，最大30秒
    
    console.log(`计划${delay}ms后进行第${reconnectAttempts.current}次重连`)
    
    reconnectTimeoutRef.current = setTimeout(() => {
      console.log(`开始第${reconnectAttempts.current}次重连`)
      cleanup()
      // 延迟少许后重连，避免立即重连
      setTimeout(() => {
        connect()
      }, 100)
    }, delay)
  }, [cleanup, connect])
  
  const handleSSEMessage = (type: string, data: string) => {
    switch (type) {
      case 'connection':
        // 连接消息
        break
      case 'log':
        // 日志内容已在addEventListener中处理
        break
      case 'error':
        console.error('服务器错误:', data)
        setError(data)
        toast.error(`日志获取失败: ${data}`)
        break
      default:
        console.log('未知SSE消息类型:', type, data)
    }
  }
  
  const disconnect = useCallback(() => {
    console.log('主动断开SSE连接')
    cleanup()
    reconnectAttempts.current = maxReconnectAttempts // 阻止自动重连
  }, [cleanup])
  
  // 自动连接
  useEffect(() => {
    if (enabled && clusterId && hostCommandId) {
      connect()
    } else {
      disconnect()
    }
    
    return () => {
      cleanup()
    }
  }, [enabled, clusterId, hostCommandId, connect, disconnect, cleanup])
  
  return {
    // 状态
    isConnected,
    isConnecting,
    error,
    logContent,
    
    // 方法
    connect,
    disconnect,
    
    // 内部状态（调试用）
    reconnectAttempts: reconnectAttempts.current
  }
}
