'use client'

import { Button } from '@/components/ui/button'
import { Progress } from '@/components/ui/progress'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { FileText, CheckCircle2, XCircle, AlertCircle, HardDrive, Cpu, MemoryStick } from 'lucide-react'

interface CheckItemDetailCardProps {
  checkKey: string
  checkResult: any
  status: string
  onViewLogs: () => void
}

const getStatusIcon = (status: string) => {
  switch (status) {
    case 'SUCCESS':
      return <CheckCircle2 className="h-5 w-5 text-green-600" />
    case 'FAILED':
      return <XCircle className="h-5 w-5 text-red-600" />
    case 'CHECKING':
      return <div className="h-5 w-5 border-2 border-blue-600 border-t-transparent rounded-full animate-spin" />
    case 'SKIPPED':
      return <AlertCircle className="h-5 w-5 text-gray-400" />
    default:
      return <div className="h-5 w-5 border-2 border-gray-300 rounded-full" />
  }
}

export function CheckItemDetailCard({ 
  checkKey, 
  checkResult, 
  status, 
  onViewLogs 
}: CheckItemDetailCardProps) {
  
  // 内存检查详情
  if (checkKey === 'memory') {
    const details = checkResult?.details || {}
    return (
      <div className="p-4 space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <MemoryStick className="h-5 w-5 text-blue-600" />
            <h4 className="font-semibold">内存检查</h4>
          </div>
          <Button size="sm" variant="ghost" onClick={onViewLogs}>
            <FileText className="h-4 w-4 mr-1" />
            查看日志
          </Button>
        </div>
        
        <div className="space-y-2">
          <div className="flex justify-between text-sm">
            <span>总内存</span>
            <span className="font-medium">{details.totalMemoryMB || 0} MB</span>
          </div>
          
          <div className="space-y-1">
            <div className="flex justify-between text-sm">
              <span>已使用</span>
              <span>{details.usedMemoryMB || 0} MB ({details.usagePercent || 0}%)</span>
            </div>
            <Progress value={details.usagePercent || 0} className="h-2" />
          </div>
          
          <div className="flex justify-between text-sm text-gray-600">
            <span>系统要求</span>
            <span>≥ {details.requiredMemoryMB || 0} MB</span>
          </div>
          
          {status === 'FAILED' && checkResult?.message && (
            <Alert variant="destructive">
              <AlertDescription className="text-xs">
                {checkResult.message}
              </AlertDescription>
            </Alert>
          )}
        </div>
      </div>
    )
  }
  
  // 磁盘检查详情
  if (checkKey === 'disk') {
    const details = checkResult?.details || {}
    const checks = details.checks || []
    
    return (
      <div className="p-4 space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <HardDrive className="h-5 w-5 text-purple-600" />
            <h4 className="font-semibold">磁盘空间检查</h4>
          </div>
          <Button size="sm" variant="ghost" onClick={onViewLogs}>
            <FileText className="h-4 w-4 mr-1" />
            查看日志
          </Button>
        </div>
        
        <div className="space-y-3">
          {checks.map((disk: any, idx: number) => (
            <div key={idx} className="space-y-1">
              <div className="flex justify-between text-sm">
                <span className="font-medium">{disk.path}</span>
                <span className={disk.availableGB < disk.requiredGB ? 'text-red-600' : 'text-green-600'}>
                  {disk.availableGB} / {disk.totalGB} GB
                </span>
              </div>
              <Progress 
                value={(disk.availableGB / disk.totalGB) * 100} 
                className={`h-2 ${disk.availableGB < disk.requiredGB ? 'bg-red-100' : ''}`}
              />
              <div className="text-xs text-gray-500">
                要求: ≥ {disk.requiredGB} GB
              </div>
            </div>
          ))}
        </div>
        
        {status === 'FAILED' && checkResult?.message && (
          <Alert variant="destructive">
            <AlertDescription className="text-xs">
              {checkResult.message}
            </AlertDescription>
          </Alert>
        )}
      </div>
    )
  }
  
  // CPU检查详情
  if (checkKey === 'cpu') {
    const details = checkResult?.details || {}
    return (
      <div className="p-4 space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Cpu className="h-5 w-5 text-orange-600" />
            <h4 className="font-semibold">CPU核心数检查</h4>
          </div>
          <Button size="sm" variant="ghost" onClick={onViewLogs}>
            <FileText className="h-4 w-4 mr-1" />
            查看日志
          </Button>
        </div>
        
        <div className="space-y-2">
          <div className="flex justify-between text-sm">
            <span>实际核心数</span>
            <span className="font-medium">{details.actual || 0} 核</span>
          </div>
          
          <div className="flex justify-between text-sm text-gray-600">
            <span>最低要求</span>
            <span>{details.required || 0} 核</span>
          </div>
          
          <div className="flex justify-between text-sm text-gray-600">
            <span>推荐配置</span>
            <span>{details.recommended || 0} 核</span>
          </div>
          
          {status === 'FAILED' && checkResult?.message && (
            <Alert variant="destructive">
              <AlertDescription className="text-xs">
                {checkResult.message}
              </AlertDescription>
            </Alert>
          )}
        </div>
      </div>
    )
  }
  
  // 其他检查项的通用展示
  return (
    <div className="p-4 space-y-3">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          {getStatusIcon(status)}
          <h4 className="font-semibold">{checkResult?.displayName || checkKey}</h4>
        </div>
        <Button size="sm" variant="ghost" onClick={onViewLogs}>
          <FileText className="h-4 w-4 mr-1" />
          查看日志
        </Button>
      </div>
      
      <div className="space-y-2">
        {checkResult?.message && (
          <div className="text-sm">{checkResult.message}</div>
        )}
        
        {checkResult?.recommendation && (
          <Alert>
            <AlertDescription className="text-xs">
              <strong>建议：</strong>{checkResult.recommendation}
            </AlertDescription>
          </Alert>
        )}
        
        {checkResult?.details && Object.keys(checkResult.details).length > 0 && (
          <div className="text-xs text-gray-600 space-y-1">
            <div className="font-semibold">详细信息：</div>
            {Object.entries(checkResult.details).map(([key, value]) => (
              <div key={key} className="flex justify-between">
                <span>{key}:</span>
                <span className="font-mono">{JSON.stringify(value)}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

