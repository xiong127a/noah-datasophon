import { useState, useEffect, useCallback, useRef } from 'react'
import { message } from 'antd'

// API 接口定义
interface HostValidationRequest {
  clusterId: string  // 使用 string 避免 Long 精度丢失
  hostIps: string[]
  sshUser: string
  sshPassword?: string
  sshPort: number
  privateKeyPath?: string
}

interface CheckItem {
  checkType: string
  displayName: string
  status: 'PENDING' | 'CHECKING' | 'SUCCESS' | 'FAILED' | 'REPAIRING'
  message: string
  details?: Record<string, any>
  updateTime: string
  repairAvailable: boolean
  repairAction?: string
}

interface HostStatus {
  hostIp: string
  hostname?: string
  overallStatus: 'PENDING' | 'CHECKING' | 'SUCCESS' | 'FAILED' | 'REPAIRING'
  checkItems: CheckItem[]
  logs: string[]
  lastUpdateTime: string
  canRepair: boolean
  paused: boolean
  cancelled: boolean
}

interface LogMessage {
  type: 'log'
  clusterId: string  // 使用 string 避免 Long 精度丢失
  hostIp: string
  logLevel: 'INFO' | 'WARN' | 'ERROR' | 'DEBUG'
  message: string
  source: string
  timestamp: string
}

/**
 * 主机校验Hook
 * 管理主机校验状态、SSE连接和操作控制
 */
export const useHostValidation = (clusterId: string) => {
  // 状态管理
  const [hostStatuses, setHostStatuses] = useState<HostStatus[]>([])
  const [isConnected, setIsConnected] = useState(false)
  const [isValidating, setIsValidating] = useState(false)
  const [logs, setLogs] = useState<LogMessage[]>([])
  
  // SSE 连接引用
  const statusSSE = useRef<EventSource | null>(null)
  const logSSE = useRef<EventSource | null>(null)

  // 建立状态SSE连接
  const connectStatusSSE = useCallback(() => {
    if (statusSSE.current) {
      statusSSE.current.close()
    }

    const url = `/ddh/api/v1/sse/host-validation/status/${clusterId}`
    statusSSE.current = new EventSource(url)

    statusSSE.current.onopen = () => {
      setIsConnected(true)
      console.log('主机校验状态SSE连接已建立')
    }

    statusSSE.current.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        
        if (data.type === 'connected') {
          console.log('SSE连接确认:', data.message)
        } else if (data.type === 'status') {
          // 更新主机状态
          setHostStatuses(prev => {
            const index = prev.findIndex(h => h.hostIp === data.hostIp)
            if (index >= 0) {
              const newStatuses = [...prev]
              newStatuses[index] = { ...newStatuses[index], ...data }
              return newStatuses
            } else {
              return [...prev, data as HostStatus]
            }
          })
        }
      } catch (error) {
        console.error('解析SSE状态数据失败:', error)
      }
    }

    statusSSE.current.onerror = (error) => {
      console.error('状态SSE连接错误:', error)
      setIsConnected(false)
      
      // 3秒后重连
      setTimeout(() => {
        if (statusSSE.current?.readyState !== EventSource.OPEN) {
          connectStatusSSE()
        }
      }, 3000)
    }
  }, [clusterId])

  // 建立日志SSE连接
  const connectLogSSE = useCallback((hostIp?: string) => {
    if (logSSE.current) {
      logSSE.current.close()
    }

    const url = hostIp 
      ? `/ddh/api/v1/sse/host-validation/logs/${clusterId}?hostIp=${hostIp}`
      : `/ddh/api/v1/sse/host-validation/logs/${clusterId}`
    
    logSSE.current = new EventSource(url)

    logSSE.current.onopen = () => {
      console.log('主机校验日志SSE连接已建立')
    }

    logSSE.current.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data) as LogMessage
        
        if (data.type === 'log-connected') {
          console.log('日志SSE连接确认:', data.message)
        } else if (data.type === 'log') {
          setLogs(prev => [...prev, data].slice(-1000)) // 保留最近1000条日志
        }
      } catch (error) {
        console.error('解析SSE日志数据失败:', error)
      }
    }

    logSSE.current.onerror = (error) => {
      console.error('日志SSE连接错误:', error)
    }
  }, [clusterId])

  // 启动校验
  const startValidation = useCallback(async (request: HostValidationRequest) => {
    try {
      const response = await fetch('/ddh/api/v1/host-validation/start', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request)
      })

      const result = await response.json()
      
      if (result.success) {
        setIsValidating(true)
        // 初始化主机状态
        setHostStatuses(request.hostIps.map(ip => ({
          hostIp: ip,
          overallStatus: 'PENDING' as const,
          checkItems: [],
          logs: [],
          lastUpdateTime: new Date().toISOString(),
          canRepair: false,
          paused: false,
          cancelled: false
        })))
        message.success(result.data)
      } else {
        message.error(result.message)
      }
    } catch (error) {
      console.error('启动校验失败:', error)
      message.error('启动校验失败')
    }
  }, [])

  // 暂停校验
  const pauseValidation = useCallback(async (hostIp?: string) => {
    try {
      const url = hostIp 
        ? `/ddh/api/v1/host-validation/pause/${clusterId}?hostIp=${hostIp}`
        : `/ddh/api/v1/host-validation/pause/${clusterId}`
      
      const response = await fetch(url, { method: 'POST' })
      const result = await response.json()
      
      if (result.success) {
        message.success(result.data)
      } else {
        message.error(result.message)
      }
    } catch (error) {
      console.error('暂停校验失败:', error)
      message.error('暂停校验失败')
    }
  }, [clusterId])

  // 继续校验
  const resumeValidation = useCallback(async (hostIp?: string) => {
    try {
      const url = hostIp 
        ? `/ddh/api/v1/host-validation/resume/${clusterId}?hostIp=${hostIp}`
        : `/ddh/api/v1/host-validation/resume/${clusterId}`
      
      const response = await fetch(url, { method: 'POST' })
      const result = await response.json()
      
      if (result.success) {
        message.success(result.data)
      } else {
        message.error(result.message)
      }
    } catch (error) {
      console.error('继续校验失败:', error)
      message.error('继续校验失败')
    }
  }, [clusterId])

  // 停止校验
  const stopValidation = useCallback(async (hostIp?: string) => {
    try {
      const url = hostIp 
        ? `/ddh/api/v1/host-validation/stop/${clusterId}?hostIp=${hostIp}`
        : `/ddh/api/v1/host-validation/stop/${clusterId}`
      
      const response = await fetch(url, { method: 'POST' })
      const result = await response.json()
      
      if (result.success) {
        setIsValidating(false)
        message.success(result.data)
      } else {
        message.error(result.message)
      }
    } catch (error) {
      console.error('停止校验失败:', error)
      message.error('停止校验失败')
    }
  }, [clusterId])

  // 重新检查
  const recheckItem = useCallback(async (hostIp: string, checkType: string) => {
    try {
      const response = await fetch(
        `/ddh/api/v1/host-validation/recheck/${clusterId}?hostIp=${hostIp}&checkType=${checkType}`,
        { method: 'POST' }
      )
      const result = await response.json()
      
      if (result.success) {
        message.success(result.data)
      } else {
        message.error(result.message)
      }
    } catch (error) {
      console.error('重新检查失败:', error)
      message.error('重新检查失败')
    }
  }, [clusterId])

  // 修复项目
  const repairItem = useCallback(async (hostIp: string, checkType: string) => {
    try {
      const response = await fetch(
        `/ddh/api/v1/host-validation/repair/${clusterId}?hostIp=${hostIp}&checkType=${checkType}`,
        { method: 'POST' }
      )
      const result = await response.json()
      
      if (result.success) {
        message.success(result.data)
      } else {
        message.error(result.message)
      }
    } catch (error) {
      console.error('修复失败:', error)
      message.error('修复失败')
    }
  }, [clusterId])

  // 批量修复
  const batchRepair = useCallback(async (hostIps: string[]) => {
    try {
      const response = await fetch(`/ddh/api/v1/host-validation/repair-batch/${clusterId}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(hostIps)
      })
      const result = await response.json()
      
      if (result.success) {
        message.success(result.data)
      } else {
        message.error(result.message)
      }
    } catch (error) {
      console.error('批量修复失败:', error)
      message.error('批量修复失败')
    }
  }, [clusterId])

  // 清理会话
  const cleanupSession = useCallback(async () => {
    try {
      const response = await fetch(`/ddh/api/v1/host-validation/cleanup/${clusterId}`, {
        method: 'DELETE'
      })
      const result = await response.json()
      
      if (result.success) {
        setHostStatuses([])
        setLogs([])
        message.success(result.data)
      } else {
        message.error(result.message)
      }
    } catch (error) {
      console.error('清理会话失败:', error)
      message.error('清理会话失败')
    }
  }, [clusterId])

  // 组件挂载时建立连接
  useEffect(() => {
    connectStatusSSE()
    connectLogSSE()

    return () => {
      statusSSE.current?.close()
      logSSE.current?.close()
    }
  }, [connectStatusSSE, connectLogSSE])

  return {
    // 状态
    hostStatuses,
    isConnected,
    isValidating,
    logs,
    
    // 操作方法
    startValidation,
    pauseValidation,
    resumeValidation,
    stopValidation,
    recheckItem,
    repairItem,
    batchRepair,
    cleanupSession,
    
    // SSE连接方法
    connectLogSSE
  }
}
