import { useEffect, useRef, useState, useCallback } from 'react'
import { toast } from 'sonner'
import { buildWebSocketURL, WS_PATHS_V1 } from '@/lib/api-config-v1'

interface LogMessage {
  type: string
  data: string
  level: string
  timestamp: number
}

interface UseLogWebSocketProps {
  clusterId?: string
  hostCommandId?: string
  onLogUpdate?: (logContent: string) => void
  enabled?: boolean
}

export const useLogWebSocket = ({
  clusterId,
  hostCommandId,
  onLogUpdate,
  enabled = true
}: UseLogWebSocketProps) => {
  const [isConnected, setIsConnected] = useState(false)
  const [isConnecting, setIsConnecting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [logContent, setLogContent] = useState('')
  
  const wsRef = useRef<WebSocket | null>(null)
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null)
  const reconnectAttempts = useRef(0)
  const maxReconnectAttempts = 5
  
  // 使用ref来存储回调函数，避免重新创建导致的依赖问题
  const onLogUpdateRef = useRef(onLogUpdate)
  useEffect(() => {
    onLogUpdateRef.current = onLogUpdate
  }, [onLogUpdate])
  
  const connect = useCallback(() => {
    if (!enabled || !clusterId || !hostCommandId || wsRef.current?.readyState === WebSocket.OPEN) {
      return
    }
    
    setIsConnecting(true)
    setError(null)
    
    try {
      // 使用统一配置构建WebSocket URL
      let wsUrl = buildWebSocketURL(WS_PATHS_V1.LOG)
      
      // 从localStorage获取JWT token
      const token = localStorage.getItem('jwt_token')
      if (token) {
        // 由于浏览器WebSocket API不支持自定义header，通过查询参数传递token
        wsUrl += `?token=${encodeURIComponent(token)}`
      }
      
      wsRef.current = new WebSocket(wsUrl)
      
      wsRef.current.onopen = () => {
        setIsConnected(true)
        setIsConnecting(false)
        setError(null)
        reconnectAttempts.current = 0
        
        // 立即发送心跳保持连接活跃
        if (wsRef.current) {
          wsRef.current.send(JSON.stringify({ action: 'ping' }))
        }
        
        // 启动日志流（如果参数齐全）
        if (wsRef.current && clusterId && hostCommandId) {
          wsRef.current.send(JSON.stringify({
            action: 'startLog',
            clusterId,
            hostCommandId
          }))
        }
      }
      
      wsRef.current.onmessage = (event) => {
        try {
          const message: LogMessage = JSON.parse(event.data)
          
          switch (message.type) {
            case 'log':
              setLogContent(message.data)
              onLogUpdateRef.current?.(message.data)
              break
            case 'error':
              console.error('服务器错误:', message.data)
              setError(message.data)
              toast.error(`日志获取失败: ${message.data}`)
              break
            case 'started':
              toast.success('实时日志已启动')
              break
            case 'stopped':
              break
            case 'connection':
            case 'pong':
              // 连接和心跳消息，无需特殊处理
              break
            default:
              // 忽略未知消息类型
          }
        } catch (e) {
          console.error('解析WebSocket消息失败:', e)
        }
      }
      
      wsRef.current.onclose = (event) => {
        setIsConnected(false)
        setIsConnecting(false)
        
        // 如果是认证失败（403），不再重连
        if (event.code === 1008 || event.reason === 'Unauthorized') {
          setError('认证失败，请重新登录')
          toast.error('WebSocket认证失败，请重新登录')
          return
        }
        
        // 如果不是正常关闭且启用了连接，尝试重连
        if (enabled && event.code !== 1000 && reconnectAttempts.current < maxReconnectAttempts) {
          const delay = Math.min(1000 * Math.pow(2, reconnectAttempts.current), 10000)
          
          reconnectTimeoutRef.current = setTimeout(() => {
            reconnectAttempts.current++
            connect()
          }, delay)
        } else if (reconnectAttempts.current >= maxReconnectAttempts) {
          setError('连接失败次数过多，已停止重连')
          toast.error('WebSocket连接失败，请刷新页面重试')
        }
      }
      
      wsRef.current.onerror = (event) => {
        console.error('WebSocket错误:', event)
        setError('连接错误')
        setIsConnecting(false)
      }
      
    } catch (err) {
      console.error('创建WebSocket连接失败:', err)
      setError('创建连接失败')
      setIsConnecting(false)
    }
  }, [enabled, clusterId, hostCommandId])
  
  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current)
      reconnectTimeoutRef.current = null
    }
    
    if (wsRef.current) {
      // 发送停止日志流消息
      if (wsRef.current.readyState === WebSocket.OPEN) {
        wsRef.current.send(JSON.stringify({ action: 'stopLog' }))
      }
      
      wsRef.current.close(1000, '主动断开连接')
      wsRef.current = null
    }
    
    setIsConnected(false)
    setIsConnecting(false)
    setError(null)
    reconnectAttempts.current = 0
  }, [])
  
  const sendPing = useCallback(() => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({ action: 'ping' }))
    }
  }, [])
  
  useEffect(() => {
    if (enabled && clusterId && hostCommandId) {
      connect()
    } else {
      disconnect()
    }
    
    return () => {
      disconnect()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled, clusterId, hostCommandId])
  
  // 定期发送心跳（20秒间隔，避免30秒后端超时）
  useEffect(() => {
    if (isConnected) {
      const heartbeat = setInterval(sendPing, 20000)
      return () => clearInterval(heartbeat)
    }
  }, [isConnected, sendPing])
  
  return {
    isConnected,
    isConnecting,
    error,
    logContent,
    connect,
    disconnect
  }
}
