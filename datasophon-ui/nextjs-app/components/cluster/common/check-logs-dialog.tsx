'use client'

import { useEffect, useState } from 'react'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { FileText, Wrench, RefreshCw } from 'lucide-react'
import { clusterApiV1 } from '@/lib/api-utils-v1'

interface CheckLogsDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  hostIp: string
  checkKey: string
  checkName: string
}

export function CheckLogsDialog({
  open,
  onOpenChange,
  hostIp,
  checkKey,
  checkName
}: CheckLogsDialogProps) {
  const [activeTab, setActiveTab] = useState<'check' | 'repair'>('check')
  const [checkLog, setCheckLog] = useState('')
  const [repairLog, setRepairLog] = useState('')
  const [loading, setLoading] = useState(false)
  
  // 加载日志
  useEffect(() => {
    if (open) {
      loadLogs()
    }
  }, [open, hostIp, checkKey])
  
  const loadLogs = async () => {
    setLoading(true)
    try {
      const response = await clusterApiV1.environmentCheck.getLogs(hostIp, checkKey)
      if (response.data) {
        setCheckLog(response.data.checkLog || '暂无检查日志')
        setRepairLog(response.data.repairLog || '暂无修复日志')
      }
    } catch (error) {
      console.error('加载日志失败:', error)
      setCheckLog('加载日志失败')
      setRepairLog('加载日志失败')
    } finally {
      setLoading(false)
    }
  }
  
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl max-h-[80vh]">
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
            <div className="flex justify-end">
              <Button size="sm" variant="outline" onClick={loadLogs} disabled={loading}>
                <RefreshCw className={`h-4 w-4 mr-1 ${loading ? 'animate-spin' : ''}`} />
                刷新
              </Button>
            </div>
            <div className="bg-gray-900 text-green-400 p-4 rounded-lg font-mono text-sm overflow-auto max-h-96">
              <pre className="whitespace-pre-wrap">{checkLog}</pre>
            </div>
          </TabsContent>
          
          <TabsContent value="repair" className="space-y-2">
            <div className="flex justify-end">
              <Button size="sm" variant="outline" onClick={loadLogs} disabled={loading}>
                <RefreshCw className={`h-4 w-4 mr-1 ${loading ? 'animate-spin' : ''}`} />
                刷新
              </Button>
            </div>
            <div className="bg-gray-900 text-green-400 p-4 rounded-lg font-mono text-sm overflow-auto max-h-96">
              <pre className="whitespace-pre-wrap">{repairLog}</pre>
            </div>
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

