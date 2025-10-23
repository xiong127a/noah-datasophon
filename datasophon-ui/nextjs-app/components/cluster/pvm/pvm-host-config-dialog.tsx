"use client"

import React, { useState, useEffect } from 'react'
import { 
  Info, Server, Shield, Eye, EyeOff
} from 'lucide-react'
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { toast } from 'sonner'
import ClusterWizardLayout from '../common/cluster-wizard-layout'
import ClusterWizardActionBar from '../common/cluster-wizard-action-bar'
import Image from "next/image"

import { CARD_STYLES } from '../common/shared-styles'

// PVM集群信息接口
export interface PvmClusterInfo {
  id: string
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
export interface PvmHostConfigDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  cluster: PvmClusterInfo | null
  onStep1Complete: (data: PvmStep1Data) => void
}

export default function PvmHostConfigDialog({
  open,
  onOpenChange,
  cluster,
  onStep1Complete
}: PvmHostConfigDialogProps) {
  const [step1Data, setStep1Data] = useState<PvmStep1Data>({
    hosts: '192.168.1.54,192.168.1.55,192.168.1.56',
    sshUser: 'root',
    sshPort: '22',
    sshPassword: 'Jd2019'
  })
  
  const [loading, setLoading] = useState(false)
  const [passwordVisible, setPasswordVisible] = useState(false)

  const currentStep = 1

  // 获取集群类型图标路径
  const getIconPath = () => "/images/cluster/linux-tux.svg"

  // 清空表单数据
  const clearFormData = () => {
    setStep1Data({
      hosts: '192.168.1.54,192.168.1.55,192.168.1.56',
      sshUser: 'root',
      sshPort: '22',
      sshPassword: 'Jd2019'
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

  // IP地址格式验证
  const isValidIP = (ip: string): boolean => {
    const ipPattern = /^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/
    return ipPattern.test(ip)
  }

  // 验证Step1数据
  const validateStep1 = (): boolean => {
    if (!step1Data.hosts?.trim()) {
      toast.error('请输入主机IP地址')
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
      toast.error('请输入有效的IP地址')
      return false
    }

    // 验证每个主机地址都是有效的IP
    for (const host of hosts) {
      if (!isValidIP(host.trim())) {
        toast.error(`无效的IP地址格式: ${host}，请输入有效的IPv4地址`)
        return false
      }
    }

    // 检查IP数量限制
    if (hosts.length > 100) {
      toast.error('IP地址数量过多，最大支持100个')
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
    } catch {
      toast.error('配置保存失败，请重试')
    } finally {
      setLoading(false)
    }
  }

  // 创建统一的ActionBar
  const actionBar = (
    <ClusterWizardActionBar
      statusInfo={{
        text: "PVM 集群配置",
        pulse: true
      }}
      buttons={[
        {
          text: loading ? "处理中..." : "下一步",
          onClick: handleNext,
          disabled: loading || !step1Data.hosts || !step1Data.sshUser || !step1Data.sshPort || !step1Data.sshPassword,
          loading: loading,
          loadingText: "处理中..."
        }
      ]}
    />
  )

  return (
    <ClusterWizardLayout
      open={open}
      onClose={() => onOpenChange(false)}
      clusterName={cluster?.clusterName || ''}
      clusterType="PVM"
      stepTitle="安装主机"
      stepDescription="传统集群配置 - 配置集群IP地址列表和SSH连接信息，支持批量IP管理"
      currentStep={currentStep}
      dialogTitle={`PVM集群配置 - ${cluster?.clusterName}`}
      actionBar={actionBar}
    >
      {/* 步骤内容 */}
      <div className="flex-1 overflow-y-auto bg-gradient-to-b from-white to-slate-50/50 min-h-0 [&::-webkit-scrollbar]:w-2 [&::-webkit-scrollbar-track]:bg-transparent [&::-webkit-scrollbar-thumb]:bg-indigo-200/60 [&::-webkit-scrollbar-thumb]:rounded-full [&::-webkit-scrollbar-thumb:hover]:bg-indigo-300/80 [&::-webkit-scrollbar]:transition-all">
        <div className="p-6 sm:p-8 lg:p-10">
          <div className="space-y-8">
                  {/* Header - 框架化样式 */}
                  <div className="text-center pb-4">
                    <div className="mx-auto w-20 h-20 bg-gradient-to-br from-emerald-500 via-green-600 to-teal-500 rounded-2xl flex items-center justify-center mb-6 shadow-xl">
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
                    <h3 className="text-2xl font-bold bg-gradient-to-r from-emerald-600 via-green-600 to-teal-600 bg-clip-text text-transparent mb-2">传统集群配置</h3>
                    <p className="text-gray-600 max-w-md mx-auto">
                      配置集群IP地址列表和 SSH 连接信息，支持批量IP管理
                    </p>
                  </div>

                  {/* 主配置区域 - 使用左右分栏布局 */}
                  <div className="grid grid-cols-1 xl:grid-cols-2 gap-8">
                    {/* Host Configuration - 框架化卡片 */}
                    <Card className={`${CARD_STYLES.base} shadow-xl rounded-2xl`}>
                      <CardHeader className={CARD_STYLES.header}>
                        <CardTitle className={`${CARD_STYLES.title} flex items-center`}>
                          <Server className="w-5 h-5 mr-2 text-indigo-600" />
                          IP地址列表
                        </CardTitle>
                      </CardHeader>
                      <CardContent className="space-y-6">
                        <div className="space-y-3">
                          <Label className="text-sm font-medium">IP地址</Label>
                          <Textarea
                            placeholder={`输入主机IP地址，支持以下格式：

📍 单个IP：
   192.168.1.54

📍 每行一个IP：
   192.168.1.54
   192.168.1.55
   192.168.1.56

📍 逗号分隔：
   192.168.1.54, 192.168.1.55, 192.168.1.56

📍 范围批量（推荐）：
   192.168.1.[54-56]    ➤   自动展开为 54-56
   10.0.0.[1-50]        ➤   自动展开为 1-50

`}
                            value={step1Data.hosts}
                            onChange={(e) => setStep1Data(prev => ({ ...prev, hosts: e.target.value }))}
                            rows={12}
                            className="font-mono text-sm resize-none rounded-xl"
                          />
                          {step1Data.hosts && (
                            <div className="text-xs text-gray-500">
                              预计IP数量: {parseHostRange(step1Data.hosts).length} 个
                            </div>
                          )}
                        </div>
                      </CardContent>
                    </Card>

                    {/* SSH Credentials - 框架化卡片 */}
                    <Card className={`${CARD_STYLES.base} shadow-xl rounded-2xl`}>
                      <CardHeader className={CARD_STYLES.header}>
                        <CardTitle className={`${CARD_STYLES.title} flex items-center`}>
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
                              className="h-12 rounded-xl"
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
                              className="h-12 rounded-xl"
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
                              className="h-12 pr-12 rounded-xl"
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

                  {/* Tips - 框架化卡片 */}
                  <Card className={`${CARD_STYLES.base} ${CARD_STYLES.info} shadow-xl rounded-2xl`}>
                    <CardContent className={CARD_STYLES.content}>
                      <div className="flex items-start space-x-3">
                        <Info className="w-5 h-5 text-indigo-600 mt-0.5 flex-shrink-0" />
                        <div className="space-y-2">
                          <div className="font-medium text-indigo-900">配置提示</div>
                          <ul className="text-sm text-slate-700 space-y-1">
                            <li>• 确保所有IP地址对应的主机可通过 SSH 连接，且使用相同的用户名和密码</li>
                            <li>• 如需使用不同密码的主机，请分批添加和配置</li>
                            <li>• 支持IP范围批量输入，如：192.168.1.[54-56] 表示 192.168.1.54 到 192.168.1.56</li>
                            <li>• 建议使用默认SSH端口22，如需修改请确保所有主机使用相同端口</li>
                            <li>• 只支持IPv4地址格式，不支持主机名或域名</li>
                          </ul>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                </div>
              </div>
            </div>
        </ClusterWizardLayout>
  )
}
