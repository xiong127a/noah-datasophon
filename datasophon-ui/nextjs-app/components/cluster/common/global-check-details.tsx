'use client'

import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { AlertTriangle } from 'lucide-react'

/**
 * 解析主机名冲突
 */
function renderHostnameConflicts(conflicts: any[]) {
  return (
    <div className="space-y-3">
      {conflicts.map((conflict, idx) => (
        <Card key={idx} className="border-orange-200 bg-orange-50">
          <CardContent className="pt-4">
            <div className="flex items-start gap-3">
              <AlertTriangle className="h-5 w-5 text-orange-500 mt-0.5 flex-shrink-0" />
              <div className="flex-1 min-w-0">
                <div className="font-medium text-orange-900 mb-2">
                  主机名: <code className="px-2 py-1 bg-white rounded text-sm">{conflict.hostname}</code>
                </div>
                <div className="text-sm text-orange-700 mb-2">
                  被 <strong className="text-orange-900">{conflict.count}</strong> 台主机使用
                </div>
                <div className="flex flex-wrap gap-2">
                  {conflict.ips.map((ip: string) => (
                    <Badge key={ip} variant="outline" className="bg-white font-mono text-xs">
                      {ip}
                    </Badge>
                  ))}
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  )
}

/**
 * 解析hosts文件不一致
 */
function renderHostsFileIssues(details: any) {
  const { uniqueSections, missingHosts, failedHosts } = details
  
  return (
    <div className="space-y-4">
      {uniqueSections > 1 && (
        <Alert variant="destructive" className="border-red-300">
          <AlertTriangle className="h-4 w-4" />
          <AlertDescription>
            发现 <strong>{uniqueSections}</strong> 个不同版本的hosts文件内容，各主机的hosts文件不一致
          </AlertDescription>
        </Alert>
      )}
      
      {missingHosts && Object.keys(missingHosts).length > 0 && (
        <div className="space-y-2">
          <div className="font-medium text-sm text-gray-700 flex items-center gap-2">
            <AlertTriangle className="h-4 w-4 text-amber-500" />
            缺少主机条目的主机：
          </div>
          <div className="space-y-2">
            {Object.entries(missingHosts).map(([ip, missing]: [string, any]) => (
              <Card key={ip} className="border-red-200 bg-red-50">
                <CardContent className="pt-3 pb-3">
                  <div className="text-sm">
                    <div className="font-medium text-red-900 mb-1.5 font-mono">{ip}</div>
                    <div className="text-red-700 flex items-start gap-2">
                      <span className="text-red-500 font-medium flex-shrink-0">缺失的IP:</span>
                      <div className="flex flex-wrap gap-1.5">
                        {Array.from(missing).map((missingIp: any, idx: number) => (
                          <Badge key={idx} variant="outline" className="bg-white text-red-600 border-red-300 font-mono text-xs">
                            {missingIp}
                          </Badge>
                        ))}
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      )}
      
      {failedHosts && failedHosts.length > 0 && (
        <Alert variant="destructive" className="border-red-300">
          <AlertTriangle className="h-4 w-4" />
          <AlertDescription>
            <div className="space-y-1">
              <div>无法读取 <strong>{failedHosts.length}</strong> 台主机的hosts文件</div>
              <div className="flex flex-wrap gap-1.5 mt-2">
                {failedHosts.map((ip: string) => (
                  <Badge key={ip} variant="outline" className="bg-white font-mono text-xs">
                    {ip}
                  </Badge>
                ))}
              </div>
            </div>
          </AlertDescription>
        </Alert>
      )}
    </div>
  )
}

/**
 * 解析空主机名列表
 */
function renderEmptyHostnames(emptyHostnames: string[]) {
  if (!emptyHostnames || emptyHostnames.length === 0) return null
  
  return (
    <Alert variant="destructive" className="border-yellow-300 bg-yellow-50">
      <AlertTriangle className="h-4 w-4 text-yellow-600" />
      <AlertDescription className="text-yellow-900">
        <div className="space-y-1">
          <div>以下 <strong>{emptyHostnames.length}</strong> 台主机无法获取主机名</div>
          <div className="flex flex-wrap gap-1.5 mt-2">
            {emptyHostnames.map((ip: string) => (
              <Badge key={ip} variant="outline" className="bg-white font-mono text-xs border-yellow-400">
                {ip}
              </Badge>
            ))}
          </div>
        </div>
      </AlertDescription>
    </Alert>
  )
}

/**
 * 全局检查详情显示组件
 */
export function GlobalCheckDetails({ checkKey, details }: { checkKey: string; details: any }) {
  // 主机名唯一性检查
  if (checkKey === 'hostname') {
    return (
      <div className="space-y-3">
        {details.conflicts && details.conflicts.length > 0 && renderHostnameConflicts(details.conflicts)}
        {details.emptyHostnames && details.emptyHostnames.length > 0 && renderEmptyHostnames(details.emptyHostnames)}
        
        {/* 如果没有冲突，显示统计信息 */}
        {(!details.conflicts || details.conflicts.length === 0) && 
         (!details.emptyHostnames || details.emptyHostnames.length === 0) && (
          <div className="grid grid-cols-2 gap-3">
            <Card className="border-green-200 bg-green-50">
              <CardContent className="pt-3 pb-3">
                <div className="text-sm text-green-700">总主机数</div>
                <div className="text-2xl font-bold text-green-900">{details.totalHosts || 0}</div>
              </CardContent>
            </Card>
            <Card className="border-blue-200 bg-blue-50">
              <CardContent className="pt-3 pb-3">
                <div className="text-sm text-blue-700">唯一主机名</div>
                <div className="text-2xl font-bold text-blue-900">{details.uniqueHostnames || 0}</div>
              </CardContent>
            </Card>
          </div>
        )}
      </div>
    )
  }
  
  // Hosts文件一致性检查
  if (checkKey === 'hosts-file') {
    return renderHostsFileIssues(details)
  }
  
  // 默认显示JSON（用于其他类型的检查）
  return (
    <pre className="text-xs bg-gray-50 p-3 rounded overflow-auto max-h-60 border border-gray-200">
      {JSON.stringify(details, null, 2)}
    </pre>
  )
}

