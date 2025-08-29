'use client'

import React, { useState, useEffect } from 'react'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { 
  CheckCircle, 
  XCircle, 
  Clock, 
  Loader, 
  AlertTriangle, 
  RotateCcw, 
  Wrench, 
  Square, 
  FileText 
} from 'lucide-react'

// 检查项状态枚举
export type CheckItemStatus = 'WAITING' | 'CHECKING' | 'SUCCESS' | 'FAILED' | 'SKIPPED' | 'TERMINATING' | 'FIXING'

// 检查项接口
export interface CheckItem {
  id: string
  itemName: string
  status: CheckItemStatus
  result: string
  createTime?: string
  updateTime?: string
  canRetry?: boolean
  canFix?: boolean
  canTerminate?: boolean
}

// 主机信息接口
export interface HostRecord {
  ip: string
  hostname?: string
  status: 'checking' | 'success' | 'failed' | 'waiting'
  checkItems: CheckItem[]
}

interface HostCheckItemsProps {
  record: HostRecord
  onRetryItem: (hostIp: string, itemId: string) => void
  onFixItem: (hostIp: string, itemId: string) => void
  onTerminateItem: (hostIp: string, itemId: string) => void
  onViewLog: (hostIp: string, itemId: string, itemName: string) => void
  onRetrySelected: (hostIp: string, itemIds: string[]) => void
  onFixSelected: (hostIp: string, itemIds: string[]) => void
  onTerminateSelected: (hostIp: string, itemIds: string[]) => void
}

export default function HostCheckItems({
  record,
  onRetryItem,
  onFixItem,
  onTerminateItem,
  onViewLog,
  onRetrySelected,
  onFixSelected,
  onTerminateSelected
}: HostCheckItemsProps) {
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  const [detailModal, setDetailModal] = useState<{
    visible: boolean
    title: string
    content: string
    status: CheckItemStatus
  }>({
    visible: false,
    title: '',
    content: '',
    status: 'WAITING'
  })

  const checkItems = record.checkItems || []

  // 计算各状态数量
  const successCount = checkItems.filter(item => item.status === 'SUCCESS').length
  const failedCount = checkItems.filter(item => item.status === 'FAILED').length
  const waitingCount = checkItems.filter(item => item.status === 'WAITING').length
  const checkingCount = checkItems.filter(item => item.status === 'CHECKING').length
  const skippedCount = checkItems.filter(item => item.status === 'SKIPPED').length

  // 状态配置
  const statusConfig = {
    WAITING: { 
      text: '待检查', 
      color: 'orange', 
      icon: <Clock size={14} />, 
      bgColor: 'rgba(255, 149, 0, 0.1)' 
    },
    SUCCESS: { 
      text: '通过', 
      color: 'green', 
      icon: <CheckCircle size={14} />, 
      bgColor: 'rgba(52, 199, 89, 0.1)' 
    },
    FAILED: { 
      text: '未通过', 
      color: 'red', 
      icon: <XCircle size={14} />, 
      bgColor: 'rgba(255, 59, 48, 0.1)' 
    },
    CHECKING: { 
      text: '检查中', 
      color: 'blue', 
      icon: <Loader size={14} className="animate-spin" />, 
      bgColor: 'rgba(0, 122, 255, 0.1)' 
    },
    SKIPPED: { 
      text: '已跳过', 
      color: 'default', 
      icon: <AlertTriangle size={14} />, 
      bgColor: 'rgba(142, 142, 147, 0.1)' 
    },
    TERMINATING: { 
      text: '终止中', 
      color: 'orange', 
      icon: <Loader size={14} className="animate-spin" />, 
      bgColor: 'rgba(255, 149, 0, 0.1)' 
    },
    FIXING: { 
      text: '修复中', 
      color: 'purple', 
      icon: <Loader size={14} className="animate-spin" />, 
      bgColor: 'rgba(88, 86, 214, 0.1)' 
    }
  }

  // 去除HTML标签的函数
  const stripHtml = (html: string) => {
    return html.replace(/<[^>]*>/g, '')
  }

  // 显示详情模态框
  const showDetailModal = (item: CheckItem) => {
    setDetailModal({
      visible: true,
      title: item.itemName,
      content: item.result,
      status: item.status
    })
  }

  // 检查选中项是否有可操作的项
  const selectedItems = checkItems.filter(item => selectedRowKeys.includes(item.id))
  const hasRetryableSelected = selectedItems.some(item => 
    item.canRetry && (item.status === 'FAILED' || item.status === 'SUCCESS' || item.status === 'SKIPPED')
  )
  const hasFixableSelected = selectedItems.some(item => 
    item.canFix && item.status === 'FAILED'
  )
  const hasTerminatableSelected = selectedItems.some(item => 
    item.canTerminate && item.status === 'CHECKING'
  )

  return (
    <div className="check-items-container bg-white rounded-lg shadow-sm border">
      {/* 头部汇总信息 */}
      <div className="p-4 border-b bg-gray-50">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <span className="text-lg font-semibold">
              共 {checkItems.length} 项检查
            </span>
            
            <div className="flex space-x-3">
              <div className="flex items-center space-x-1 text-green-600">
                <CheckCircle size={16} />
                <span>{successCount} 项通过</span>
              </div>
              
              <div className="flex items-center space-x-1 text-red-600">
                <XCircle size={16} />
                <span>{failedCount} 项失败</span>
              </div>
              
              <div className="flex items-center space-x-1 text-orange-600">
                <Clock size={16} />
                <span>{waitingCount} 项待检查</span>
              </div>
              
              <div className="flex items-center space-x-1 text-blue-600">
                <Loader size={16} className="animate-spin" />
                <span>{checkingCount} 项检查中</span>
              </div>
              
              <div className="flex items-center space-x-1 text-gray-600">
                <AlertTriangle size={16} />
                <span>{skippedCount} 项已跳过</span>
              </div>
            </div>
          </div>

          {/* 批量操作按钮 */}
          <div className="flex space-x-2">
            <Button
              size="small"
              type="primary"
              icon={<RotateCcw size={14} />}
              disabled={!hasRetryableSelected}
              onClick={() => onRetrySelected(record.ip, selectedRowKeys as string[])}
            >
              批量重试
            </Button>
            
            <Button
              size="small"
              icon={<Wrench size={14} />}
              disabled={!hasFixableSelected}
              onClick={() => onFixSelected(record.ip, selectedRowKeys as string[])}
            >
              批量修复
            </Button>
            
            <Button
              size="small"
              danger
              icon={<Square size={14} />}
              disabled={!hasTerminatableSelected}
              onClick={() => onTerminateSelected(record.ip, selectedRowKeys as string[])}
            >
              批量终止
            </Button>
          </div>
        </div>
      </div>

      {/* 检查项列表 */}
      <div className="divide-y">
        {checkItems.map((item) => (
          <div key={item.id} className="p-4 hover:bg-gray-50">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-3 flex-1">
                <input
                  type="checkbox"
                  checked={selectedRowKeys.includes(item.id)}
                  onChange={(e) => {
                    if (e.target.checked) {
                      setSelectedRowKeys([...selectedRowKeys, item.id])
                    } else {
                      setSelectedRowKeys(selectedRowKeys.filter(key => key !== item.id))
                    }
                  }}
                  className="rounded border-gray-300"
                />
                <div className="flex-1">
                  <div className="font-medium text-gray-900 mb-1">{item.itemName}</div>
                  <div className="flex items-center space-x-2">
                    {statusConfig[item.status]?.icon}
                    <Badge variant="outline" className={`text-xs ${
                      item.status === 'SUCCESS' ? 'text-green-700 border-green-300' :
                      item.status === 'FAILED' ? 'text-red-700 border-red-300' :
                      item.status === 'CHECKING' ? 'text-blue-700 border-blue-300' :
                      'text-gray-700 border-gray-300'
                    }`}>
                      {statusConfig[item.status]?.text}
                    </Badge>
                  </div>
                  <div className="mt-2">
                    <div 
                      className="text-sm text-gray-600 cursor-pointer hover:text-blue-600"
                      onClick={() => showDetailModal(item)}
                    >
                      {stripHtml(item.result).length > 50 
                        ? stripHtml(item.result).substr(0, 50) + '...' 
                        : stripHtml(item.result)
                      }
                    </div>
                  </div>
                </div>
                <div className="flex space-x-2">
                  {/* 终止/重试按钮 */}
                  {item.status === 'CHECKING' ? (
                    <Button
                      size="sm"
                      variant="destructive"
                      disabled={!item.canTerminate}
                      onClick={() => onTerminateItem(record.ip, item.id)}
                    >
                      <Square className="w-4 h-4 mr-1" />
                      终止
                    </Button>
                  ) : (
                    <Button
                      size="sm"
                      variant="default"
                      disabled={!item.canRetry || !(item.status === 'FAILED' || item.status === 'SUCCESS' || item.status === 'SKIPPED')}
                      onClick={() => onRetryItem(record.ip, item.id)}
                    >
                      <RotateCcw className="w-4 h-4 mr-1" />
                      重试
                    </Button>
                  )}

                  {/* 修复按钮 */}
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={!item.canFix || item.status !== 'FAILED'}
                    onClick={() => onFixItem(record.ip, item.id)}
                  >
                    <Wrench className="w-4 h-4 mr-1" />
                    修复
                  </Button>

                  {/* 查看日志按钮 */}
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => onViewLog(record.ip, item.id, item.itemName)}
                  >
                    <FileText className="w-4 h-4 mr-1" />
                    日志
                  </Button>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* 详情对话框 */}
      <Dialog 
        open={detailModal.visible} 
        onOpenChange={(open) => setDetailModal(prev => ({ ...prev, visible: open }))}
      >
        <DialogContent className="max-w-4xl max-h-[80vh]">
          <DialogHeader>
            <DialogTitle className="flex items-center space-x-2">
              {statusConfig[detailModal.status]?.icon}
              <span>检查结果详情 - {detailModal.title}</span>
            </DialogTitle>
          </DialogHeader>
          <div 
            dangerouslySetInnerHTML={{ __html: detailModal.content }}
            className="text-sm leading-relaxed overflow-y-auto p-4 bg-gray-50 rounded"
          />
        </DialogContent>
      </Dialog>
    </div>
  )
}
