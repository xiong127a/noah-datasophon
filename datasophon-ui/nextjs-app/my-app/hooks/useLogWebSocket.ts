import { useEffect, useRef, useState, useCallback } from 'react'
import { toast } from 'sonner'
import { buildWebSocketURL, WS_PATHS_V1 } from '@/lib/api-config-v1'
import { Client, IMessage } from '@stomp/stompjs'

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
  
  const stompClientRef = useRef<Client | null>(null)
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null)
  const reconnectAttempts = useRef(0)
  
  // 使用ref来存储回调函数，避免重新创建导致的依赖问题
  const onLogUpdateRef = useRef(onLogUpdate)
  useEffect(() => {
    onLogUpdateRef.current = onLogUpdate
  }, [onLogUpdate])
  
  const connect = useCallback(() => {
    if (!enabled || !clusterId || !hostCommandId || stompClientRef.current?.connected) {
      return
    }
    
    setIsConnecting(true)
    setError(null)
    
    try {
      // 使用STOMP协议连接
      const wsUrl = buildWebSocketURL(WS_PATHS_V1.STOMP)
      const token = localStorage.getItem('jwt_token')
      
      const client = new Client({
        brokerURL: wsUrl,
        connectHeaders: {
          Authorization: token ? `Bearer ${token}` : '',
          token: token || '' // 备用方式
        },
        debug: () => {
          // 生产环境关闭调试日志
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 20000,
        heartbeatOutgoing: 20000,
        
        onConnect: (frame) => {
          console.log('STOMP连接成功:', frame)
          setIsConnected(true)
          setIsConnecting(false)
          setError(null)
          reconnectAttempts.current = 0
          
          // 订阅用户的私有日志队列
          client.subscribe('/user/queue/logs', (message: IMessage) => {
            try {
              const logMessage: LogMessage = JSON.parse(message.body)
              
              switch (logMessage.type) {
                case 'history':
                case 'full':
                  // 历史日志或完整日志 - 直接覆盖
                  setLogContent(logMessage.data)
                  onLogUpdateRef.current?.(logMessage.data)
                  break
                case 'increment':
                case 'log':
                  // 增量日志 - 追加模式
                  setLogContent(prevContent => {
                    const newContent = prevContent + logMessage.data
                    onLogUpdateRef.current?.(newContent)
                    return newContent
                  })
                  break
                case 'error':
                  console.error('服务器错误:', logMessage.data)
                  setError(logMessage.data)
                  toast.error(`日志获取失败: ${logMessage.data}`)
                  break
                case 'started':
                  toast.success('实时日志已启动')
                  break
                case 'stopped':
                  break
                case 'connection':
                  // 连接消息
                  break
                default:
                  // 忽略未知消息类型
              }
            } catch (e) {
              console.error('解析STOMP消息失败:', e)
            }
          })
          
          // 启动日志流
          if (clusterId && hostCommandId) {
            client.publish({
              destination: '/app/logs/start',
              body: JSON.stringify({
                clusterId: clusterId,     // 保持字符串格式，避免精度丢失
                hostCommandId: hostCommandId  // 保持字符串格式
              })
            })
          }
        },
        
        onStompError: (frame) => {
          console.error('STOMP协议错误:', frame)
          setError('STOMP协议错误')
          setIsConnecting(false)
        },
        
        onWebSocketClose: (event) => {
          console.log('WebSocket连接关闭:', event.code, event.reason)
          setIsConnected(false)
          setIsConnecting(false)
          
          // 如果是认证失败，不再重连
          if (event.code === 1008 || event.reason === 'Unauthorized') {
            setError('认证失败，请重新登录')
            toast.error('WebSocket认证失败，请重新登录')
            return
          }
          
          // 重连逻辑（STOMP客户端会自动处理）
        },
        
        onWebSocketError: (event) => {
          console.error('WebSocket错误:', event)
          setError('连接错误')
          setIsConnecting(false)
        }
      })
      
      stompClientRef.current = client
      client.activate()
      
    } catch (err) {
      console.error('创建STOMP连接失败:', err)
      setError('创建连接失败')
      setIsConnecting(false)
    }
  }, [enabled, clusterId, hostCommandId])
  
  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current)
      reconnectTimeoutRef.current = null
    }
    
    if (stompClientRef.current && stompClientRef.current.connected) {
      // 发送停止日志流消息
      if (clusterId && hostCommandId) {
        stompClientRef.current.publish({
          destination: '/app/logs/stop',
          body: JSON.stringify({
            clusterId: clusterId,        // 保持字符串格式
            hostCommandId: hostCommandId // 保持字符串格式
          })
        })
      }
      
      // 断开STOMP连接
      stompClientRef.current.deactivate()
      stompClientRef.current = null
    }
    
    setIsConnected(false)
    setIsConnecting(false)
    setError(null)
    reconnectAttempts.current = 0
  }, [clusterId, hostCommandId])
  
  const sendPing = useCallback(() => {
    if (stompClientRef.current?.connected) {
      stompClientRef.current.publish({
        destination: '/app/logs/ping',
        body: JSON.stringify({ action: 'ping' })
      })
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
  
  // STOMP有内置心跳机制，通常不需要手动心跳
  // 但如果需要，可以保留这个逻辑
  useEffect(() => {
    if (isConnected) {
      const heartbeat = setInterval(sendPing, 30000)
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
