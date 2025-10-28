'use client'

import React, { useState, useEffect } from 'react'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { clusterApiV1 } from '@/lib/api-utils-v1'
import { API_BASE_URL, API_PATHS_V1 } from '@/lib/api-config-v1'
import { CheckCircle2, XCircle, Loader2, Eye, Play, AlertCircle } from 'lucide-react'

interface HostnameBatchEditDialogProps {
  open: boolean
  onClose: () => void
  clusterId: string
  hostIps: string[]
  connectionParams: any
  onSuccess?: () => void
}

interface HostnameConfig {
  recommendedPrefixes: string[]
  defaultPrefix: string
  defaultFormatIndex: number
  suffixFormats: Array<{
    name: string
    pattern: string
    example: string
  }>
}

interface HostProgress {
  hostIp: string
  status: 'pending' | 'processing' | 'success' | 'failed'
  message: string
  oldHostname?: string
  newHostname?: string
  error?: string
}

export default function HostnameBatchEditDialog({
  open,
  onClose,
  clusterId,
  hostIps,
  connectionParams,
  onSuccess
}: HostnameBatchEditDialogProps) {
  const [config, setConfig] = useState<HostnameConfig | null>(null)
  const [loading, setLoading] = useState(false)
  const [prefix, setPrefix] = useState('')
  const [suffixFormatIndex, setSuffixFormatIndex] = useState(0)
  const [startIndex, setStartIndex] = useState(1)
  const [preview, setPreview] = useState<Record<string, string>>({})
  const [showPreview, setShowPreview] = useState(false)
  const [executing, setExecuting] = useState(false)
  const [progress, setProgress] = useState<HostProgress[]>([])
  const [taskId, setTaskId] = useState<string | null>(null)
  const [completed, setCompleted] = useState(false)

  // 加载配置
  useEffect(() => {
    if (open && !config) {
      loadConfig()
    }
  }, [open])

  // 初始化前缀
  useEffect(() => {
    if (config && !prefix) {
      setPrefix(config.defaultPrefix)
      setSuffixFormatIndex(config.defaultFormatIndex)
    }
  }, [config])

  const loadConfig = async () => {
    try {
      const response = await clusterApiV1.hostManagement.getHostnameConfig()
      if (response.code === 200) {
        setConfig(response.data)
      }
    } catch (error) {
      console.error('加载主机名配置失败:', error)
    }
  }

  // 预览主机名变更
  const handlePreview = async () => {
    if (!prefix.trim()) {
      alert('请输入主机名前缀')
      return
    }

    setLoading(true)
    try {
      const response = await clusterApiV1.hostManagement.previewHostnameChanges({
        prefix: prefix.trim(),
        suffixFormatIndex,
        startIndex,
        hostIps
      })

      if (response.code === 200) {
        setPreview(response.data)
        setShowPreview(true)
      } else {
        alert(response.msg || '预览失败')
      }
    } catch (error) {
      console.error('预览失败:', error)
      alert('预览失败，请重试')
    } finally {
      setLoading(false)
    }
  }

  // 执行批量修改
  const handleExecute = async () => {
    if (!showPreview) {
      alert('请先预览变更')
      return
    }

    setExecuting(true)
    setCompleted(false)
    setProgress(hostIps.map(ip => ({
      hostIp: ip,
      status: 'pending',
      message: '等待中...',
      newHostname: preview[ip]
    })))

    try {
      const response = await clusterApiV1.hostManagement.batchChangeHostnames({
        prefix: prefix.trim(),
        suffixFormatIndex,
        startIndex,
        hostIps,
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
      setShowPreview(false)
      setPreview({})
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

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>批量修改主机名</DialogTitle>
        </DialogHeader>
        
        {/* 配置加载中 */}
        {!config && (
          <div className="flex items-center justify-center py-12">
            <div className="text-center space-y-3">
              <Loader2 className="h-8 w-8 animate-spin mx-auto text-blue-500" />
              <p className="text-sm text-gray-600">加载配置中...</p>
            </div>
          </div>
        )}
        
        {/* 配置加载完成，显示内容 */}
        {config && (
        <>

        <div className="space-y-6">
          {/* 配置区域 */}
          {!executing && !completed && (
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>主机名前缀</Label>
                <Select value={prefix} onValueChange={setPrefix}>
                  <SelectTrigger>
                    <SelectValue placeholder="选择或输入前缀" />
                  </SelectTrigger>
                  <SelectContent>
                    {config.recommendedPrefixes.map(p => (
                      <SelectItem key={p} value={p}>
                        {p}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <Input
                  placeholder="或输入自定义前缀"
                  value={prefix}
                  onChange={(e) => setPrefix(e.target.value)}
                />
              </div>

              <div className="space-y-2">
                <Label>后缀格式</Label>
                <Select
                  value={suffixFormatIndex.toString()}
                  onValueChange={(v) => setSuffixFormatIndex(parseInt(v))}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {config.suffixFormats.map((format, index) => (
                      <SelectItem key={index} value={index.toString()}>
                        {format.name} (示例: {format.example})
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label>起始编号</Label>
                <Input
                  type="number"
                  min="1"
                  value={startIndex}
                  onChange={(e) => setStartIndex(parseInt(e.target.value) || 1)}
                />
              </div>

              <div className="flex items-end">
                <Button
                  onClick={handlePreview}
                  disabled={loading || !prefix.trim()}
                  className="w-full"
                >
                  <Eye className="mr-2 h-4 w-4" />
                  {loading ? '预览中...' : '预览变更'}
                </Button>
              </div>
            </div>
          )}

          {/* 预览表格 */}
          {showPreview && !executing && !completed && (
            <div className="space-y-2">
              <Alert>
                <AlertDescription>
                  以下是预览的主机名变更，确认无误后点击"开始执行"按钮
                </AlertDescription>
              </Alert>
              <div className="border rounded-md">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>主机IP</TableHead>
                      <TableHead>新主机名</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {Object.entries(preview).map(([ip, hostname]) => (
                      <TableRow key={ip}>
                        <TableCell className="font-mono">{ip}</TableCell>
                        <TableCell className="font-semibold text-blue-600">{hostname}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
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
              <div className="border rounded-md">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>主机IP</TableHead>
                      <TableHead>新主机名</TableHead>
                      <TableHead>状态</TableHead>
                      <TableHead>消息</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {progress.map((item) => (
                      <TableRow key={item.hostIp}>
                        <TableCell className="font-mono">{item.hostIp}</TableCell>
                        <TableCell className="font-semibold">{item.newHostname}</TableCell>
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
                              <div className="text-xs text-red-600">{item.error}</div>
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

          {/* 说明信息 */}
          {!executing && !completed && (
            <Alert>
              <AlertDescription className="text-sm">
                <ul className="list-disc list-inside space-y-1">
                  <li>主机名将按照"前缀+编号"的格式生成</li>
                  <li>编号将从起始编号开始递增</li>
                  <li>修改主机名需要root权限，请确保连接参数正确</li>
                  <li>修改后可能需要重新连接SSH</li>
                </ul>
              </AlertDescription>
            </Alert>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={handleClose} disabled={executing}>
            {completed ? '关闭' : '取消'}
          </Button>
          {showPreview && !executing && !completed && (
            <Button onClick={handleExecute} disabled={executing}>
              <Play className="mr-2 h-4 w-4" />
              开始执行
            </Button>
          )}
        </DialogFooter>
        </>
        )}
      </DialogContent>
    </Dialog>
  )
}

