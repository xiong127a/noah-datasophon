'use client'

import React, { useState, useEffect } from 'react'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { clusterApiV1 } from '@/lib/api-utils-v1'
import { API_BASE_URL, API_PATHS_V1 } from '@/lib/api-config-v1'
import { CheckCircle2, XCircle, Loader2, Play, AlertCircle, Info, RefreshCw } from 'lucide-react'

interface HostsFileSyncDialogProps {
  open: boolean
  onClose: () => void
  clusterId: string
  hostIps: string[]
  hostList?: Array<{ ip: string; hostname?: string }>  // 添加主机列表，包含主机名
  connectionParams: any
  onSuccess?: () => void
}

interface HostProgress {
  hostIp: string
  status: 'pending' | 'processing' | 'success' | 'failed'
  message: string
  error?: string
}

export default function HostsFileSyncDialog({
  open,
  onClose,
  clusterId,
  hostIps,
  hostList,
  connectionParams,
  onSuccess
}: HostsFileSyncDialogProps) {
  const [executing, setExecuting] = useState(false)
  const [progress, setProgress] = useState<HostProgress[]>([])
  const [taskId, setTaskId] = useState<string | null>(null)
  const [completed, setCompleted] = useState(false)
  const [hostsContent, setHostsContent] = useState('')
  const [hostsEdited, setHostsEdited] = useState(false)

  // 生成默认hosts文件内容
  useEffect(() => {
    if (open && !hostsEdited) {
      generateDefaultHostsContent()
    }
  }, [open, hostIps, hostList])

  const generateDefaultHostsContent = () => {
    // 使用传入的hostList生成hosts内容
    let hostsEntries: string
    
    if (hostList && hostList.length > 0) {
      // 有主机列表，使用IP和主机名映射
      hostsEntries = hostList.map((host: any) => {
        const hostname = host.hostname || host.ip
        return `${host.ip}\t${hostname}`
      }).join('\n')
    } else {
      // 没有主机列表，使用IP列表
      hostsEntries = hostIps.map(ip => `${ip}\t${ip}`).join('\n')
    }
    
    const defaultContent = `# DataSophon Managed Hosts - START
# 此部分由DataSophon管理，请勿手动修改
${hostsEntries}
# DataSophon Managed Hosts - END`
    
    setHostsContent(defaultContent)
  }

  // 执行同步
  const handleExecute = async () => {
    if (!hostsContent.trim()) {
      alert('Hosts文件内容不能为空')
      return
    }

    setExecuting(true)
    setCompleted(false)
    setProgress(hostIps.map(ip => ({
      hostIp: ip,
      status: 'pending',
      message: '等待中...'
    })))

    try {
      const response = await clusterApiV1.hostManagement.syncHostsFile({
        hostIps,
        hostsContent,  // 传递用户编辑的hosts内容
        connectionParams
      })

      if (response.code === 200) {
        const tid = response.data
        setTaskId(tid)
        // 建立SSE连接
        connectSSE(tid)
      } else {
        alert(response.msg || '启动任务失败')
        setExecuting(false)
      }
    } catch (error) {
      console.error('执行失败:', error)
      alert('执行失败，请重试')
      setExecuting(false)
    }
  }

  // 建立SSE连接
  const connectSSE = (tid: string) => {
    const url = `${API_BASE_URL}${API_PATHS_V1.HOST_MANAGEMENT_SSE}/${tid}?clusterId=${clusterId}`
    const eventSource = new EventSource(url, { withCredentials: true })

    eventSource.addEventListener('connected', () => {
      console.log('SSE连接已建立')
    })

    eventSource.addEventListener('progress', (event) => {
      try {
        const data = JSON.parse(event.data)
        updateProgress(data)
      } catch (error) {
        console.error('解析进度数据失败:', error)
      }
    })

    eventSource.addEventListener('complete', (event) => {
      try {
        const data = JSON.parse(event.data)
        console.log('任务完成:', data)
        setExecuting(false)
        setCompleted(true)
        eventSource.close()

        if (data.success && onSuccess) {
          setTimeout(() => {
            onSuccess()
          }, 1000)
        }
      } catch (error) {
        console.error('解析完成事件失败:', error)
      }
    })

    eventSource.onerror = (error) => {
      console.error('SSE连接错误:', error)
      eventSource.close()
      setExecuting(false)
    }
  }

  // 更新进度
  const updateProgress = (data: HostProgress) => {
    setProgress(prev => {
      const index = prev.findIndex(p => p.hostIp === data.hostIp)
      if (index >= 0) {
        const updated = [...prev]
        updated[index] = { ...updated[index], ...data }
        return updated
      }
      return prev
    })
  }

  const handleClose = () => {
    if (!executing) {
      // 重置状态
      setProgress([])
      setTaskId(null)
      setCompleted(false)
      onClose()
    }
  }

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'success':
        return <CheckCircle2 className="h-5 w-5 text-green-500" />
      case 'failed':
        return <XCircle className="h-5 w-5 text-red-500" />
      case 'processing':
        return <Loader2 className="h-5 w-5 text-blue-500 animate-spin" />
      default:
        return <AlertCircle className="h-5 w-5 text-gray-400" />
    }
  }

  const getStatusBadge = (status: string) => {
    const variants: Record<string, any> = {
      pending: { variant: 'outline', text: '等待中' },
      processing: { variant: 'default', text: '处理中' },
      success: { variant: 'success', text: '成功' },
      failed: { variant: 'destructive', text: '失败' }
    }
    const config = variants[status] || variants.pending
    return <Badge variant={config.variant}>{config.text}</Badge>
  }

  const getStatistics = () => {
    const total = progress.length
    const success = progress.filter(p => p.status === 'success').length
    const failed = progress.filter(p => p.status === 'failed').length
    const processing = progress.filter(p => p.status === 'processing').length
    const pending = progress.filter(p => p.status === 'pending').length

    return { total, success, failed, processing, pending }
  }

  const stats = getStatistics()

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>同步Hosts文件</DialogTitle>
        </DialogHeader>

        <div className="space-y-4">
          {/* 说明信息 */}
          {!executing && !completed && (
            <Alert>
              <Info className="h-4 w-4" />
              <AlertDescription className="text-sm">
                <div className="space-y-2">
                  <p className="font-semibold">操作说明：</p>
                  <ul className="list-disc list-inside space-y-1">
                    <li>将统一的hosts文件内容同步到所有选中的主机</li>
                    <li>系统会智能合并hosts文件，保留系统默认条目</li>
                    <li>仅修改DataSophon管理的hosts段（标记区域）</li>
                    <li>同步前会自动备份原hosts文件</li>
                    <li>需要root权限，请确保连接参数正确</li>
                  </ul>
                </div>
              </AlertDescription>
            </Alert>
          )}

          {/* Hosts文件编辑器（未执行时） */}
          {!executing && !completed && (
            <div className="border rounded-md p-4">
              <div className="flex items-center justify-between mb-2">
                <h3 className="font-semibold">Hosts文件内容</h3>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => {
                    generateDefaultHostsContent()
                    setHostsEdited(false)
                  }}
                >
                  <RefreshCw className="h-4 w-4 mr-1" />
                  重新生成
                </Button>
              </div>
              <Textarea
                value={hostsContent}
                onChange={(e) => {
                  setHostsContent(e.target.value)
                  setHostsEdited(true)
                }}
                placeholder="请输入hosts文件内容..."
                className="font-mono text-sm"
                rows={12}
              />
              <p className="text-xs text-gray-500 mt-2">
                ⚠️ 此内容将被同步到所有主机的 /etc/hosts 文件。系统会智能合并，仅修改DataSophon管理的部分。
              </p>
            </div>
          )}

          {/* 主机列表（未执行时） */}
          {!executing && !completed && (
            <div className="border rounded-md p-4">
              <h3 className="font-semibold mb-2">将同步到以下主机：</h3>
              <div className="flex flex-wrap gap-2">
                {hostIps.map(ip => (
                  <Badge key={ip} variant="outline" className="font-mono">
                    {ip}
                  </Badge>
                ))}
              </div>
              <p className="text-sm text-gray-600 mt-2">
                共 {hostIps.length} 台主机
              </p>
            </div>
          )}

          {/* 进度统计 */}
          {(executing || completed) && progress.length > 0 && (
            <div className="grid grid-cols-5 gap-2">
              <div className="bg-gray-50 p-3 rounded-md text-center">
                <div className="text-2xl font-bold">{stats.total}</div>
                <div className="text-xs text-gray-600">总计</div>
              </div>
              <div className="bg-green-50 p-3 rounded-md text-center">
                <div className="text-2xl font-bold text-green-600">{stats.success}</div>
                <div className="text-xs text-gray-600">成功</div>
              </div>
              <div className="bg-red-50 p-3 rounded-md text-center">
                <div className="text-2xl font-bold text-red-600">{stats.failed}</div>
                <div className="text-xs text-gray-600">失败</div>
              </div>
              <div className="bg-blue-50 p-3 rounded-md text-center">
                <div className="text-2xl font-bold text-blue-600">{stats.processing}</div>
                <div className="text-xs text-gray-600">处理中</div>
              </div>
              <div className="bg-gray-50 p-3 rounded-md text-center">
                <div className="text-2xl font-bold text-gray-600">{stats.pending}</div>
                <div className="text-xs text-gray-600">等待中</div>
              </div>
            </div>
          )}

          {/* 进度表格 */}
          {(executing || completed) && progress.length > 0 && (
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <h3 className="font-semibold">执行进度</h3>
                {completed && (
                  <Badge variant="success">已完成</Badge>
                )}
              </div>
              <div className="border rounded-md max-h-96 overflow-y-auto">
                <Table>
                  <TableHeader className="sticky top-0 bg-white">
                    <TableRow>
                      <TableHead>主机IP</TableHead>
                      <TableHead>状态</TableHead>
                      <TableHead>消息</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {progress.map((item) => (
                      <TableRow key={item.hostIp}>
                        <TableCell className="font-mono">{item.hostIp}</TableCell>
                        <TableCell>
                          <div className="flex items-center gap-2">
                            {getStatusIcon(item.status)}
                            {getStatusBadge(item.status)}
                          </div>
                        </TableCell>
                        <TableCell>
                          <div className="space-y-1">
                            <div className="text-sm">{item.message}</div>
                            {item.error && (
                              <div className="text-xs text-red-600 font-mono bg-red-50 p-2 rounded">
                                {item.error}
                              </div>
                            )}
                          </div>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            </div>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={handleClose} disabled={executing}>
            {completed ? '关闭' : '取消'}
          </Button>
          {!executing && !completed && (
            <Button onClick={handleExecute} disabled={executing}>
              <Play className="mr-2 h-4 w-4" />
              开始同步
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

