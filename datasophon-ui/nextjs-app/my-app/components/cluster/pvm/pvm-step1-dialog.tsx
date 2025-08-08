"use client"

import React, { useState, useEffect } from 'react'
import { 
  ChevronRight, Loader2, Info, Server, Shield, Eye, EyeOff
} from 'lucide-react'
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { toast } from 'sonner'
import ClusterWizardSidebar from '../common/cluster-wizard-sidebar'
import Image from "next/image"
import { getStepsByType, StepsType } from '@/lib/cluster-steps'
import { DIALOG_STYLES } from '../common/shared-styles'

// PVM集群信息接口
export interface PvmClusterInfo {
  id: number
  clusterName: string
  depType: string
  clusterCode: string
}

// PVM Step1数据接口
export interface PvmStep1Data {
  hosts: string
  sshUser: string
  sshPort: string
  sshPassword: string
}

// PVM Step1弹窗属性接口
export interface PvmStep1DialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  cluster: PvmClusterInfo | null
  onStep1Complete: (data: PvmStep1Data) => void
}

export default function PvmStep1Dialog({
  open,
  onOpenChange,
  cluster,
  onStep1Complete
}: PvmStep1DialogProps) {
  const [step1Data, setStep1Data] = useState<PvmStep1Data>({
    hosts: '',
    sshUser: 'root',
    sshPort: '22',
    sshPassword: ''
  })
  
  const [loading, setLoading] = useState(false)
  const [passwordVisible, setPasswordVisible] = useState(false)

  const steps = getStepsByType('pvm' as StepsType)
  const currentStep = 1

  // 获取集群类型图标路径
  const getIconPath = () => "/images/cluster/linux-tux.svg"

  // 清空表单数据
  const clearFormData = () => {
    setStep1Data({
      hosts: '',
      sshUser: 'root',
      sshPort: '22',
      sshPassword: ''
    })
    setPasswordVisible(false)
  }

  // 监听弹窗关闭，清空数据
  useEffect(() => {
    if (!open) {
      clearFormData()
    }
  }, [open])

  // 主机范围解析（支持 10.3.144.[19-23] 格式）
  const parseHostRange = (hostInput: string): string[] => {
    const hosts: string[] = []
    const lines = hostInput.split('\n').filter(line => line.trim())
    
    for (const line of lines) {
      const trimmed = line.trim()
      if (trimmed.includes('[') && trimmed.includes(']')) {
        // 解析范围格式
        const match = trimmed.match(/^(.+)\[(\d+)-(\d+)\](.*)$/)
        if (match) {
          const [, prefix, start, end, suffix] = match
          const startNum = parseInt(start)
          const endNum = parseInt(end)
          for (let i = startNum; i <= endNum; i++) {
            hosts.push(`${prefix}${i}${suffix}`)
          }
        } else {
          hosts.push(trimmed)
        }
      } else if (trimmed.includes(',')) {
        // 逗号分隔
        hosts.push(...trimmed.split(',').map(h => h.trim()).filter(h => h))
      } else if (trimmed) {
        // 单个主机
        hosts.push(trimmed)
      }
    }
    return hosts
  }

  // 切换密码可见性
  const togglePasswordVisible = () => {
    setPasswordVisible(!passwordVisible)
  }

  // 验证Step1数据
  const validateStep1 = (): boolean => {
    if (!step1Data.hosts?.trim()) {
      toast.error('请输入主机地址')
      return false
    }
    if (!step1Data.sshUser?.trim()) {
      toast.error('请输入SSH用户名')
      return false
    }
    if (!step1Data.sshPort?.trim()) {
      toast.error('请输入SSH端口')
      return false
    }
    if (!step1Data.sshPassword?.trim()) {
      toast.error('请输入SSH密码')
      return false
    }
    
    // 验证端口号
    const port = parseInt(step1Data.sshPort)
    if (isNaN(port) || port < 1 || port > 65535) {
      toast.error('请输入有效的端口号（1-65535）')
      return false
    }

    // 验证主机列表
    const hosts = parseHostRange(step1Data.hosts)
    if (hosts.length === 0) {
      toast.error('请输入有效的主机地址')
      return false
    }

    return true
  }

  // 处理下一步
  const handleNext = async () => {
    if (!validateStep1()) return

    setLoading(true)
    try {
      onStep1Complete(step1Data)
    } catch (error) {
      console.error('Step1处理异常:', error)
      toast.error('配置保存失败，请重试')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className={DIALOG_STYLES.content}>
        <DialogTitle className="sr-only">
          PVM集群配置 - {cluster?.clusterName}
        </DialogTitle>
        
        <div className="flex h-full max-h-[min(calc(100vh-96px),900px)] sm:max-h-[min(95vh,900px)]">
          {/* 左侧导航 */}
          <ClusterWizardSidebar 
            steps={steps}
            currentStep={currentStep}
            title="PVM集群配置"
            clusterName={cluster?.clusterName || ''}
            isK8s={false}
            onClose={() => onOpenChange(false)}
          />

          {/* 右侧内容区域 */}
          <div className="flex-1 flex flex-col min-h-0">
            {/* 当前步骤标题 */}
            <div className="p-6 sm:p-8 border-b border-slate-200/70 bg-gradient-to-r from-white via-indigo-50/30 to-purple-50/30 relative">
              {/* 装饰性光效 */}
              <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/60 to-transparent"></div>
              {/* 分割线光效 */}
              <div className="absolute bottom-0 left-6 right-6 h-px bg-gradient-to-r from-transparent via-indigo-200/80 to-transparent"></div>
              <div className="flex items-center justify-between relative z-10">
                <div>
                  <h2 className="text-lg sm:text-xl lg:text-2xl font-bold text-gray-900">
                    PVM集群配置
                  </h2>
                  <p className="text-gray-600 mt-1">
                    配置集群主机列表和 SSH 连接信息
                  </p>
                </div>
                <Badge variant="outline" className="text-indigo-600 border-indigo-200 bg-white/80 backdrop-blur-sm">
                  步骤 {currentStep}/{steps.length}
                </Badge>
              </div>
            </div>

            {/* 步骤内容 */}
            <div className="flex-1 overflow-y-auto bg-gradient-to-b from-white to-slate-50/50 min-h-0 [&::-webkit-scrollbar]:w-2 [&::-webkit-scrollbar-track]:bg-transparent [&::-webkit-scrollbar-thumb]:bg-indigo-200/60 [&::-webkit-scrollbar-thumb]:rounded-full [&::-webkit-scrollbar-thumb:hover]:bg-indigo-300/80 [&::-webkit-scrollbar]:transition-all">
              <div className="p-6 sm:p-8 lg:p-10">
                <div className="space-y-8">
                  {/* Header */}
                  <div className="text-center pb-4">
                    <div className="mx-auto w-20 h-20 bg-gradient-to-br from-green-500 via-emerald-600 to-teal-500 rounded-3xl flex items-center justify-center mb-6 shadow-2xl">
                      <div className="w-12 h-12 relative">
                        <Image
                          src={getIconPath()}
                          alt="Linux"
                          width={48}
                          height={48}
                          className="object-contain"
                        />
                      </div>
                    </div>
                    <h3 className="text-2xl font-bold bg-gradient-to-r from-green-600 via-emerald-600 to-teal-600 bg-clip-text text-transparent mb-2">传统集群配置</h3>
                    <p className="text-slate-600 max-w-md mx-auto">
                      配置集群主机列表和 SSH 连接信息，支持批量主机管理
                    </p>
                  </div>

                  {/* 主配置区域 - 使用左右分栏布局 */}
                  <div className="grid grid-cols-1 xl:grid-cols-2 gap-8">
                    {/* Host Configuration */}
                    <Card className="border-0 shadow-2xl bg-white/80 backdrop-blur-sm rounded-3xl">
                      <CardHeader className="pb-4">
                        <CardTitle className="text-lg flex items-center">
                          <Server className="w-5 h-5 mr-2 text-indigo-600" />
                          主机列表
                        </CardTitle>
                      </CardHeader>
                      <CardContent className="space-y-6">
                        <div className="space-y-3">
                          <Label className="text-sm font-medium">主机地址</Label>
                          <Textarea
                            placeholder="输入主机 IP 或主机名，支持以下格式：&#10;&#10;• 每行一个地址：&#10;  192.168.1.100&#10;  192.168.1.101&#10;&#10;• 逗号分隔：&#10;  192.168.1.100,192.168.1.101&#10;&#10;• 范围批量（推荐）：&#10;  10.3.144.[19-23]  →  10.3.144.19 到 10.3.144.23"
                            value={step1Data.hosts}
                            onChange={(e) => setStep1Data(prev => ({ ...prev, hosts: e.target.value }))}
                            rows={12}
                            className="font-mono text-sm resize-none rounded-2xl"
                          />
                          {step1Data.hosts && (
                            <div className="text-xs text-gray-500">
                              预计主机数量: {parseHostRange(step1Data.hosts).length} 台
                            </div>
                          )}
                        </div>
                      </CardContent>
                    </Card>

                    {/* SSH Credentials */}
                    <Card className="border-0 shadow-2xl bg-white/80 backdrop-blur-sm rounded-3xl">
                      <CardHeader className="pb-4">
                        <CardTitle className="text-lg flex items-center">
                          <Shield className="w-5 h-5 mr-2 text-purple-600" />
                          SSH 连接凭证
                        </CardTitle>
                      </CardHeader>
                      <CardContent className="space-y-6">
                        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                          <div className="space-y-3">
                            <Label htmlFor="sshUser" className="text-sm font-medium">SSH 用户名</Label>
                            <Input
                              id="sshUser"
                              placeholder="root"
                              value={step1Data.sshUser}
                              onChange={(e) => setStep1Data(prev => ({ ...prev, sshUser: e.target.value }))}
                              className="h-12 rounded-2xl"
                            />
                          </div>
                          <div className="space-y-3">
                            <Label htmlFor="sshPort" className="text-sm font-medium">SSH 端口</Label>
                            <Input
                              id="sshPort"
                              type="number"
                              placeholder="22"
                              value={step1Data.sshPort}
                              onChange={(e) => setStep1Data(prev => ({ ...prev, sshPort: e.target.value }))}
                              className="h-12 rounded-2xl"
                            />
                          </div>
                        </div>
                        
                        <div className="space-y-3">
                          <Label htmlFor="sshPassword" className="text-sm font-medium">SSH 密码</Label>
                          <div className="relative">
                            <Input
                              id="sshPassword"
                              type={passwordVisible ? "text" : "password"}
                              placeholder="输入 SSH 连接密码"
                              value={step1Data.sshPassword}
                              onChange={(e) => setStep1Data(prev => ({ ...prev, sshPassword: e.target.value }))}
                              className="h-12 pr-12 rounded-2xl"
                            />
                            <Button
                              type="button"
                              variant="ghost"
                              size="sm"
                              className="absolute right-1 top-1 h-10 w-10 p-0"
                              onClick={togglePasswordVisible}
                            >
                              {passwordVisible ? (
                                <EyeOff className="w-4 h-4 text-gray-400" />
                              ) : (
                                <Eye className="w-4 h-4 text-gray-400" />
                              )}
                            </Button>
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  </div>

                  {/* Tips */}
                  <Card className="border-0 shadow-2xl bg-white/80 backdrop-blur-sm rounded-3xl">
                    <CardContent className="pt-6">
                      <div className="flex items-start space-x-3">
                        <Info className="w-5 h-5 text-indigo-600 mt-0.5 flex-shrink-0" />
                        <div className="space-y-2">
                          <div className="font-medium text-indigo-900">配置提示</div>
                          <ul className="text-sm text-slate-700 space-y-1">
                            <li>• 确保所有主机可通过 SSH 连接，且使用相同的用户名和密码</li>
                            <li>• 如需使用不同密码的主机，请分批添加和配置</li>
                            <li>• 支持IP范围批量输入，如：192.168.1.[10-20] 表示 192.168.1.10 到 192.168.1.20</li>
                            <li>• 建议使用默认SSH端口22，如需修改请确保所有主机使用相同端口</li>
                          </ul>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                </div>
              </div>
            </div>

            {/* 底部按钮 */}
            <div className="p-6 sm:p-8 border-t border-slate-200/50 bg-white/90 backdrop-blur-sm relative">
              {/* 装饰性光效 */}
              <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/80 to-transparent"></div>
              {/* 顶部分割线光效 */}
              <div className="absolute top-0 left-6 right-6 h-px bg-gradient-to-r from-transparent via-indigo-200/60 to-transparent"></div>
              <div className="flex justify-end space-x-3 relative z-10">
                <button
                  onClick={handleNext}
                  disabled={loading || !step1Data.hosts || !step1Data.sshUser || !step1Data.sshPort || !step1Data.sshPassword}
                  className={`flex items-center px-6 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 shadow-md hover:shadow-lg ${
                    loading || !step1Data.hosts || !step1Data.sshUser || !step1Data.sshPort || !step1Data.sshPassword
                      ? 'bg-gray-200 text-gray-400 cursor-not-allowed'
                      : 'bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 text-white transform hover:scale-105'
                  }`}
                >
                  {loading ? (
                    <>
                      <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                      处理中...
                    </>
                  ) : (
                    <>
                      下一步
                      <ChevronRight className="w-4 h-4 ml-2" />
                    </>
                  )}
                </button>
              </div>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}
