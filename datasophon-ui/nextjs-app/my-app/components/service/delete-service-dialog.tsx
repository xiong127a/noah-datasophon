"use client"

import React, { useState } from "react"
import { AlertTriangle, Trash2, Server, Activity, X, Zap, AlertCircle } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { VisuallyHidden } from "@radix-ui/react-visually-hidden"
import { SvgIcon } from '@/components/ui/svg-icon'

interface ServiceItem {
  id: string
  name: string
  serviceName: string
  icon?: string
  serviceId: string
  serviceStateCode: number
  alertNum: number
  needRestart: boolean
}

interface DeleteServiceDialogProps {
  open: boolean
  onClose: () => void
  onConfirm: () => Promise<void>
  service: ServiceItem | null
}

// 服务状态配置 - 与后端ServiceState枚举保持一致
const getServiceStatusInfo = (stateCode: number) => {
  switch (stateCode) {
    case 1:
      return { 
        label: '待安装',
        color: 'text-blue-600',
        bgColor: 'bg-blue-100',
        dotColor: 'bg-blue-500'
      }
    case 2:
      return { 
        label: '正在运行',
        color: 'text-green-600',
        bgColor: 'bg-green-100',
        dotColor: 'bg-green-500'
      }
    case 3:
      return { 
        label: '存在告警',
        color: 'text-yellow-600',
        bgColor: 'bg-yellow-100',
        dotColor: 'bg-yellow-500'
      }
    case 4:
      return { 
        label: '存在异常',
        color: 'text-red-600',
        bgColor: 'bg-red-100',
        dotColor: 'bg-red-500'
      }
    default:
      return { 
        label: '未知状态',
        color: 'text-gray-600',
        bgColor: 'bg-gray-100',
        dotColor: 'bg-gray-400'
      }
  }
}

function DeleteServiceDialog({
  open,
  onClose,
  onConfirm,
  service,
}: DeleteServiceDialogProps) {
  const [loading, setLoading] = useState(false)

  // 删除服务
  const handleDelete = async () => {
    if (!service) return

    setLoading(true)
    try {
      await onConfirm()
    } finally {
      setLoading(false)
    }
  }

  // 取消删除
  const handleCancel = () => {
    if (!loading) {
      onClose()
    }
  }

  if (!service) return null

  const statusInfo = getServiceStatusInfo(service.serviceStateCode)
  const isRunning = service.serviceStateCode === 2 // RUNNING状态
  const canDelete = !isRunning

  return (
    <Dialog open={open} onOpenChange={() => {}}>
      <DialogContent 
        className="w-[95vw] sm:max-w-[520px] max-h-[85vh] border-0 shadow-2xl bg-white/95 backdrop-blur-xl rounded-3xl overflow-hidden mx-auto my-4 [&>button]:hidden"
        aria-describedby={undefined}
      >
        {/* 可访问性标题 - 对屏幕阅读器可见，视觉上隐藏 */}
        <DialogHeader>
          <VisuallyHidden>
            <DialogTitle>删除服务确认</DialogTitle>
            <DialogDescription>
              此操作将永久删除服务实例和相关配置，此操作不可撤销。
            </DialogDescription>
          </VisuallyHidden>
        </DialogHeader>

        {/* 自定义关闭按钮 */}
        <div className="absolute top-4 right-4 z-20">
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={handleCancel}
            disabled={loading}
            className="group h-10 w-10 rounded-2xl bg-white/80 backdrop-blur-sm border border-slate-200/50 hover:bg-red-50 hover:border-red-200 transition-all duration-300 shadow-lg hover:shadow-xl"
          >
            <X className="h-4 w-4 text-slate-600 group-hover:text-red-500 group-hover:rotate-90 transition-all duration-300" />
          </Button>
        </div>

        {/* 装饰性背景 */}
        <div className="absolute inset-0 overflow-hidden">
          <div className="absolute -top-20 -right-20 w-40 h-40 bg-gradient-to-br from-red-400/20 to-orange-400/20 rounded-full blur-3xl animate-pulse" />
          <div className="absolute -bottom-20 -left-20 w-40 h-40 bg-gradient-to-tr from-purple-400/20 to-red-400/20 rounded-full blur-3xl animate-pulse" />
        </div>

        {/* 主要内容 */}
        <div className="relative z-10 p-6">
          {/* 标题区域 */}
          <div className="text-center mb-8">
            <div className="relative mb-6">
              <div className="flex items-center justify-center w-20 h-20 mx-auto mb-4 bg-gradient-to-br from-red-500 to-orange-600 rounded-full shadow-lg animate-pulse">
                <AlertTriangle className="h-10 w-10 text-white" />
              </div>
              <div className="absolute inset-0 w-20 h-20 mx-auto bg-gradient-to-br from-red-500/30 to-orange-600/30 rounded-full blur-xl animate-ping" />
            </div>
            <h2 className="text-2xl font-bold bg-gradient-to-r from-red-600 to-orange-600 bg-clip-text text-transparent mb-2">
              删除服务确认
            </h2>
            <p className="text-slate-600 text-sm leading-relaxed">
              此操作将永久删除服务实例和相关配置，<br />
              <span className="font-medium text-red-600">此操作不可撤销</span>
            </p>
          </div>

          {/* 服务信息卡片 */}
          <div className="bg-gradient-to-br from-slate-50 to-slate-100/50 rounded-2xl p-6 mb-6 border border-slate-200/50">
            <div className="flex items-start space-x-4">
              {/* 服务图标 */}
              <div className="relative">
                <div className="w-16 h-16 bg-gradient-to-br from-white to-gray-50 rounded-xl shadow-md border border-gray-200 flex items-center justify-center ring-4 ring-red-100 ring-offset-2 ring-offset-white">
                  <SvgIcon name={service.icon || service.serviceName.toLowerCase()} className="w-8 h-8" />
                </div>
                <div className={`absolute -top-1 -right-1 w-4 h-4 rounded-full border-2 border-white shadow-sm ${statusInfo.dotColor}`} />
              </div>
              
              {/* 服务详细信息 */}
              <div className="flex-1 space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <Server className="h-4 w-4 text-slate-500" />
                    <span className="text-sm text-slate-500">服务名称</span>
                  </div>
                  <span className="font-semibold text-slate-800">{service.name}</span>
                </div>
                
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <Activity className="h-4 w-4 text-slate-500" />
                    <span className="text-sm text-slate-500">运行状态</span>
                  </div>
                  <span className={`px-2 py-1 text-xs ${statusInfo.bgColor} ${statusInfo.color} rounded-full font-medium flex items-center space-x-1`}>
                    <div className={`w-1.5 h-1.5 ${statusInfo.dotColor} rounded-full`} />
                    <span>{statusInfo.label}</span>
                  </span>
                </div>
                
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <Zap className="h-4 w-4 text-slate-500" />
                    <span className="text-sm text-slate-500">服务类型</span>
                  </div>
                  <span className="font-medium text-slate-700">{service.serviceName}</span>
                </div>

                {service.alertNum > 0 && (
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-2">
                      <AlertCircle className="h-4 w-4 text-slate-500" />
                      <span className="text-sm text-slate-500">告警数量</span>
                    </div>
                    <span className="px-2 py-1 text-xs bg-red-100 text-red-700 rounded-full font-bold">
                      {service.alertNum} 个告警
                    </span>
                  </div>
                )}

                {service.needRestart && (
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-2">
                      <AlertTriangle className="h-4 w-4 text-orange-500" />
                      <span className="text-sm text-slate-500">重启状态</span>
                    </div>
                    <span className="px-2 py-1 text-xs bg-orange-100 text-orange-700 rounded-full font-medium">
                      需要重启
                    </span>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* 警告提示 */}
          {isRunning ? (
            /* 运行中服务的特殊警告 */
            <div className="bg-gradient-to-r from-amber-50 to-yellow-50 border-2 border-amber-200/50 rounded-2xl p-4 mb-6">
              <div className="flex items-start space-x-3">
                <div className="flex-shrink-0 w-8 h-8 bg-amber-100 rounded-full flex items-center justify-center">
                  <AlertTriangle className="h-4 w-4 text-amber-600" />
                </div>
                <div className="flex-1">
                  <h4 className="font-semibold text-amber-800 mb-1">无法删除正在运行的服务</h4>
                  <p className="text-sm text-amber-700 leading-relaxed">
                    服务 <span className="font-bold text-amber-800">&ldquo;{service.name}&rdquo;</span> 当前状态为 <span className="font-bold text-green-600">正在运行</span>，无法直接删除。
                  </p>
                  <div className="mt-3 p-3 bg-amber-100/50 rounded-lg">
                    <p className="text-sm text-amber-700">
                      <strong>请先停止服务：</strong>
                    </p>
                    <ul className="mt-2 text-sm text-amber-600 space-y-1">
                      <li className="flex items-center space-x-2">
                        <div className="w-1.5 h-1.5 bg-amber-500 rounded-full" />
                        <span>在服务管理页面停止所有运行中的组件</span>
                      </li>
                      <li className="flex items-center space-x-2">
                        <div className="w-1.5 h-1.5 bg-amber-500 rounded-full" />
                        <span>确认服务状态变为&ldquo;已停止&rdquo;</span>
                      </li>
                      <li className="flex items-center space-x-2">
                        <div className="w-1.5 h-1.5 bg-amber-500 rounded-full" />
                        <span>然后再尝试删除操作</span>
                      </li>
                    </ul>
                  </div>
                </div>
              </div>
            </div>
          ) : (
            /* 普通删除警告 */
            <div className="bg-gradient-to-r from-red-50 to-orange-50 border-2 border-red-200/50 rounded-2xl p-4 mb-6">
              <div className="flex items-start space-x-3">
                <div className="flex-shrink-0 w-8 h-8 bg-red-100 rounded-full flex items-center justify-center">
                  <AlertTriangle className="h-4 w-4 text-red-600" />
                </div>
                <div className="flex-1">
                  <h4 className="font-semibold text-red-800 mb-1">危险操作警告</h4>
                  <p className="text-sm text-red-700 leading-relaxed">
                    删除服务 <span className="font-bold text-red-800">&ldquo;{service.name}&rdquo;</span> 将会：
                  </p>
                  <ul className="mt-2 text-sm text-red-600 space-y-1">
                    <li className="flex items-center space-x-2">
                      <div className="w-1.5 h-1.5 bg-red-500 rounded-full" />
                      <span>永久删除服务实例和所有组件</span>
                    </li>
                    <li className="flex items-center space-x-2">
                      <div className="w-1.5 h-1.5 bg-red-500 rounded-full" />
                      <span>清除所有配置文件和数据</span>
                    </li>
                    <li className="flex items-center space-x-2">
                      <div className="w-1.5 h-1.5 bg-red-500 rounded-full" />
                      <span>停止所有相关的后台进程</span>
                    </li>
                    <li className="flex items-center space-x-2">
                      <div className="w-1.5 h-1.5 bg-red-500 rounded-full" />
                      <span>此操作无法撤销或恢复</span>
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          )}

          {/* 操作按钮 */}
          <div className="flex items-center justify-end space-x-4">
            <Button
              type="button"
              variant="outline"
              onClick={handleCancel}
              disabled={loading}
              className="group relative h-12 px-6 rounded-2xl border-0 bg-gradient-to-r from-slate-100 via-gray-100 to-slate-100 hover:from-slate-200 hover:via-gray-200 hover:to-slate-200 shadow-md hover:shadow-lg transition-all duration-300 font-medium overflow-hidden"
            >
              {/* 装饰性背景 */}
              <div className="absolute inset-0 bg-gradient-to-r from-slate-400/10 via-gray-400/5 to-slate-400/10 opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
              
              {/* 按钮内容 */}
              <div className="relative flex items-center space-x-2 text-slate-700 group-hover:text-slate-800 transition-colors duration-300">
                <X className="h-4 w-4 group-hover:rotate-90 group-hover:scale-110 transition-transform duration-300" />
                <span>取消</span>
              </div>
              
              {/* 悬停边框效果 */}
              <div className="absolute inset-0 rounded-2xl border-2 border-slate-300/0 group-hover:border-slate-300/50 transition-all duration-300" />
            </Button>
            <Button
              type="button"
              onClick={handleDelete}
              disabled={loading || !canDelete}
              className={`h-12 px-6 rounded-2xl border-0 shadow-lg hover:shadow-xl transition-all duration-300 font-medium flex items-center space-x-2 ${
                canDelete 
                  ? 'bg-gradient-to-r from-red-500 to-orange-600 hover:from-red-600 hover:to-orange-700 text-white' 
                  : 'bg-gradient-to-r from-gray-400 to-gray-500 text-gray-300 cursor-not-allowed'
              }`}
            >
              {loading ? (
                <>
                  <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  <span>删除中...</span>
                </>
              ) : canDelete ? (
                <>
                  <Trash2 className="h-4 w-4" />
                  <span>确认删除</span>
                </>
              ) : (
                <>
                  <AlertTriangle className="h-4 w-4" />
                  <span>无法删除</span>
                </>
              )}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}

export default DeleteServiceDialog
