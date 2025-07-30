"use client"

import React, { useState, useEffect, useRef } from 'react'
import { 
  X, ChevronLeft, ChevronRight, CheckCircle, Loader2, Upload, FileText, 
  Server, Cloud, Eye, EyeOff, Search, Plus, Check, 
  AlertCircle, Info, Zap, Shield, Network, Cpu
} from 'lucide-react'
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Separator } from "@/components/ui/separator"
// import { clusterApi } from '@/lib/api-utils'  // 暂时注释掉，后续会用到
import { toast } from 'sonner'

interface ClusterSetupDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  cluster: {
    id: number
    clusterName: string
    depType: string
    clusterCode: string
  } | null
  onSuccess?: () => void
}

interface Step1Data {
  // Traditional cluster
  hosts: string
  sshUser: string
  sshPort: string
  sshPassword: string
  // Kubernetes cluster
  kubeConfigContent?: string
  namespace?: string
  namespaces?: string[]
  isCreatingNewNamespace?: boolean
  customNamespace?: string
}

const ClusterSetupDialog: React.FC<ClusterSetupDialogProps> = ({
  open,
  onOpenChange,
  cluster,
  onSuccess
}) => {
  const [currentStep, setCurrentStep] = useState(1)
  const [loading, setLoading] = useState(false)
  const [namespacesLoading, setNamespacesLoading] = useState(false)
  const [passwordVisible, setPasswordVisible] = useState(false)
  const [namespaceSearch, setNamespaceSearch] = useState('')
  const fileInputRef = useRef<HTMLInputElement>(null)
  
  const [step1Data, setStep1Data] = useState<Step1Data>({
    hosts: '',
    sshUser: 'root',
    sshPort: '22',
    sshPassword: '',
    kubeConfigContent: '',
    namespace: '',
    namespaces: [],
    isCreatingNewNamespace: false,
    customNamespace: ''
  })

  const steps = [
    { number: 1, title: '安装主机', description: '配置集群主机和连接信息' },
    { number: 2, title: '选择服务', description: '选择要安装的大数据服务' },
    { number: 3, title: '配置服务', description: '配置服务参数和资源分配' },
    { number: 4, title: '完成安装', description: '确认配置并开始安装' }
  ]

  const isK8s = cluster?.depType === 'kubernetes'

  // 重置表单数据
  const resetForm = () => {
    setCurrentStep(1)
    setPasswordVisible(false)
    setNamespaceSearch('')
    setStep1Data({
      hosts: '',
      sshUser: 'root', 
      sshPort: '22',
      sshPassword: '',
      kubeConfigContent: '',
      namespace: '',
      namespaces: [],
      isCreatingNewNamespace: false,
      customNamespace: ''
    })
  }

  // 当对话框打开时重置表单
  useEffect(() => {
    if (open && cluster) {
      resetForm()
    }
  }, [open, cluster])

  // 文件上传处理
  const handleFileUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (file) {
      setLoading(true)
      const reader = new FileReader()
      reader.onload = (e) => {
        const content = e.target?.result as string
        setStep1Data(prev => ({ ...prev, kubeConfigContent: content }))
        // 自动解析命名空间（模拟）
        parseKubeConfigNamespaces(content)
        setLoading(false)
      }
      reader.onerror = () => {
        toast.error('文件读取失败')
        setLoading(false)
      }
      reader.readAsText(file)
    }
  }

  // 解析kubeconfig中的命名空间（模拟）
  const parseKubeConfigNamespaces = async (_content: string) => {
    setNamespacesLoading(true)
    try {
      // 模拟API调用解析命名空间
      await new Promise(resolve => setTimeout(resolve, 1000))
      const mockNamespaces = ['default', 'kube-system', 'kube-public', 'datasophon', 'monitoring']
      setStep1Data(prev => ({ ...prev, namespaces: mockNamespaces }))
    } catch {
      toast.error('获取命名空间失败')
    } finally {
      setNamespacesLoading(false)
    }
  }

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
        }
      } else {
        // 普通格式，按逗号分割
        hosts.push(...trimmed.split(',').map(h => h.trim()).filter(Boolean))
      }
    }
    return hosts
  }

  // 切换密码可见性
  const togglePasswordVisible = () => {
    setPasswordVisible(!passwordVisible)
  }

  // 创建新命名空间
  const handleCreateNamespace = () => {
    setStep1Data(prev => ({ ...prev, isCreatingNewNamespace: true, namespace: '' }))
  }

  // 取消创建命名空间
  const handleCancelCreateNamespace = () => {
    setStep1Data(prev => ({ 
      ...prev, 
      isCreatingNewNamespace: false, 
      customNamespace: '' 
    }))
  }

  // 选择命名空间
  const handleSelectNamespace = (namespace: string) => {
    setStep1Data(prev => ({ ...prev, namespace }))
  }

  // 过滤命名空间
  const filteredNamespaces = step1Data.namespaces?.filter(ns => 
    ns.toLowerCase().includes(namespaceSearch.toLowerCase())
  ) || []

  // 验证Step1数据
  const validateStep1 = (): boolean => {
    if (isK8s) {
      if (!step1Data.kubeConfigContent?.trim()) {
        toast.error('请输入或上传Kubernetes配置文件')
        return false
      }
      if (!step1Data.namespace?.trim()) {
        toast.error('请输入命名空间')
        return false
      }
    } else {
      if (!step1Data.hosts?.trim()) {
        toast.error('请输入主机地址')
        return false
      }
      if (!step1Data.sshUser?.trim()) {
        toast.error('请输入SSH用户名')
        return false
      }
      if (!step1Data.sshPassword?.trim()) {
        toast.error('请输入SSH密码')
        return false
      }
    }
    return true
  }

  // 下一步
  const handleNext = async () => {
    if (currentStep === 1) {
      if (!validateStep1()) {
        return
      }
      // TODO: 这里可以添加Step1数据保存逻辑
    }
    
    if (currentStep < steps.length) {
      setCurrentStep(prev => prev + 1)
    } else {
      // 完成配置
      handleFinish()
    }
  }

  // 上一步
  const handlePrevious = () => {
    if (currentStep > 1) {
      setCurrentStep(prev => prev - 1)
    }
  }

  // 完成配置
  const handleFinish = async () => {
    setLoading(true)
    try {
      // TODO: 调用完成配置的API
      toast.success('集群配置完成！')
      onSuccess?.()
      onOpenChange(false)
    } catch (error) {
      console.error('配置集群失败:', error)
      toast.error('配置集群失败，请重试')
    } finally {
      setLoading(false)
    }
  }

  // 渲染Step1内容
  const renderStep1 = () => {
    if (isK8s) {
      return (
        <div className="space-y-8">
          {/* Header */}
          <div className="text-center pb-4">
            <div className="mx-auto w-20 h-20 bg-gradient-to-br from-blue-500 to-cyan-500 rounded-2xl flex items-center justify-center mb-6 shadow-lg">
              <Cloud className="w-10 h-10 text-white" />
            </div>
            <h3 className="text-2xl font-bold text-gray-900 mb-2">Kubernetes 集群配置</h3>
            <p className="text-gray-600 max-w-md mx-auto">
              上传或输入 Kubernetes 配置文件，系统将自动识别可用的命名空间
            </p>
          </div>

          {/* Kubeconfig Configuration */}
          <Card className="border-0 shadow-xl bg-gradient-to-br from-white to-blue-50/30">
            <CardHeader className="pb-4">
              <CardTitle className="text-lg flex items-center">
                <FileText className="w-5 h-5 mr-2 text-blue-600" />
                Kubernetes 配置文件
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              {/* File Upload */}
              <div className="space-y-3">
                <Label className="text-sm font-medium">配置文件</Label>
                <div className="grid grid-cols-1 gap-3">
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept=".yaml,.yml,.config"
                    onChange={handleFileUpload}
                    className="hidden"
                  />
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => fileInputRef.current?.click()}
                    className="h-12 border-dashed border-2 hover:border-blue-400 hover:bg-blue-50/50 transition-all duration-200"
                    disabled={loading}
                  >
                    {loading ? (
                      <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    ) : (
                      <Upload className="w-4 h-4 mr-2" />
                    )}
                    从文件上传 kubeconfig
                  </Button>
                </div>
                
                <div className="relative">
                  <Textarea
                    placeholder="或者直接粘贴 kubeconfig 内容...&#10;&#10;支持标准的 ~/.kube/config 格式"
                    value={step1Data.kubeConfigContent || ''}
                    onChange={(e) => {
                      setStep1Data(prev => ({ ...prev, kubeConfigContent: e.target.value }))
                      if (e.target.value.trim()) {
                        parseKubeConfigNamespaces(e.target.value)
                      }
                    }}
                    rows={8}
                    className="font-mono text-sm resize-none border-gray-200 focus:border-blue-400 focus:ring-blue-400"
                  />
                  {step1Data.kubeConfigContent && (
                    <Button
                      variant="ghost"
                      size="sm"
                      className="absolute top-2 right-2 h-8 w-8 p-0 hover:bg-red-100"
                      onClick={() => setStep1Data(prev => ({ ...prev, kubeConfigContent: '', namespace: '', namespaces: [] }))}
                    >
                      <X className="w-4 h-4 text-gray-400 hover:text-red-600" />
                    </Button>
                  )}
                </div>
              </div>

              {/* Namespace Selection */}
              <div className="space-y-3">
                <Label className="text-sm font-medium flex items-center">
                  命名空间
                  {step1Data.kubeConfigContent && (
                    <Badge variant="secondary" className="ml-2 text-xs">
                      {namespacesLoading ? '加载中...' : `${step1Data.namespaces?.length || 0} 个可用`}
                    </Badge>
                  )}
                </Label>
                
                {!step1Data.kubeConfigContent ? (
                  <div className="p-4 bg-gray-50 rounded-lg border-2 border-dashed border-gray-200">
                    <div className="flex items-center text-gray-500">
                      <Info className="w-4 h-4 mr-2" />
                      <span className="text-sm">请先输入 Kubernetes 配置文件</span>
                    </div>
                  </div>
                ) : (
                  <div className="space-y-3">
                    {step1Data.isCreatingNewNamespace ? (
                      /* Create New Namespace Mode */
                      <div className="space-y-3">
                        <div className="relative">
                          <Input
                            placeholder="输入新的命名空间名称"
                            value={step1Data.customNamespace || ''}
                            onChange={(e) => setStep1Data(prev => ({ 
                              ...prev, 
                              customNamespace: e.target.value,
                              namespace: e.target.value 
                            }))}
                            className="pr-20"
                          />
                          <Button
                            variant="ghost"
                            size="sm"
                            className="absolute right-1 top-1 h-8 px-3 text-xs"
                            onClick={handleCancelCreateNamespace}
                          >
                            取消
                          </Button>
                        </div>
                        {step1Data.customNamespace && (
                          <div className="flex items-center p-3 bg-blue-50 rounded-lg border border-blue-200">
                            <AlertCircle className="w-4 h-4 mr-2 text-blue-600" />
                            <span className="text-sm text-blue-800">
                              将创建新命名空间: <strong>{step1Data.customNamespace}</strong>
                            </span>
                          </div>
                        )}
                      </div>
                    ) : (
                      /* Select Existing Namespace Mode */
                      <div className="space-y-3">
                        <Select
                          value={step1Data.namespace}
                          onValueChange={(value) => {
                            if (value === '__create_new__') {
                              handleCreateNamespace()
                            } else {
                              handleSelectNamespace(value)
                            }
                          }}
                        >
                          <SelectTrigger className="h-12">
                            <SelectValue placeholder={
                              namespacesLoading ? "加载命名空间中..." : "选择或搜索命名空间"
                            } />
                          </SelectTrigger>
                          <SelectContent>
                            <div className="p-2">
                              <div className="relative">
                                <Search className="absolute left-2 top-2.5 h-4 w-4 text-gray-400" />
                                <Input
                                  placeholder="搜索命名空间..."
                                  value={namespaceSearch}
                                  onChange={(e) => setNamespaceSearch(e.target.value)}
                                  className="pl-8 h-9"
                                />
                              </div>
                            </div>
                            <Separator className="my-1" />
                            <SelectItem value="__create_new__" className="font-medium text-blue-600">
                              <div className="flex items-center">
                                <Plus className="w-4 h-4 mr-2" />
                                创建新的命名空间
                              </div>
                            </SelectItem>
                            {filteredNamespaces.length > 0 && (
                              <>
                                <Separator className="my-1" />
                                {filteredNamespaces.map((ns) => (
                                  <SelectItem key={ns} value={ns}>
                                    <div className="flex items-center justify-between w-full">
                                      <span>{ns}</span>
                                      {step1Data.namespace === ns && (
                                        <Check className="w-4 h-4 text-green-600" />
                                      )}
                                    </div>
                                  </SelectItem>
                                ))}
                              </>
                            )}
                            {filteredNamespaces.length === 0 && namespaceSearch && (
                              <div className="p-2 text-sm text-gray-500 text-center">
                                没有找到匹配的命名空间
                              </div>
                            )}
                          </SelectContent>
                        </Select>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </CardContent>
          </Card>

          {/* K8s Auto-Discovery Info */}
          <Card className="border-0 shadow-lg bg-gradient-to-br from-green-50 to-emerald-50">
            <CardHeader className="pb-4">
              <CardTitle className="text-lg flex items-center text-green-700">
                <Zap className="w-5 h-5 mr-2" />
                自动获取集群信息
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="flex items-center space-x-3">
                  <div className="w-8 h-8 bg-green-100 rounded-lg flex items-center justify-center">
                    <Network className="w-4 h-4 text-green-600" />
                  </div>
                  <div>
                    <div className="font-medium text-gray-900">节点信息</div>
                    <div className="text-sm text-gray-600">自动识别 worker 节点</div>
                  </div>
                </div>
                <div className="flex items-center space-x-3">
                  <div className="w-8 h-8 bg-green-100 rounded-lg flex items-center justify-center">
                    <Cpu className="w-4 h-4 text-green-600" />
                  </div>
                  <div>
                    <div className="font-medium text-gray-900">CPU 架构</div>
                    <div className="text-sm text-gray-600">识别处理器架构</div>
                  </div>
                </div>
                <div className="flex items-center space-x-3">
                  <div className="w-8 h-8 bg-green-100 rounded-lg flex items-center justify-center">
                    <Shield className="w-4 h-4 text-green-600" />
                  </div>
                  <div>
                    <div className="font-medium text-gray-900">节点状态</div>
                    <div className="text-sm text-gray-600">实时资源信息</div>
                  </div>
                </div>
                <div className="flex items-center space-x-3">
                  <div className="w-8 h-8 bg-green-100 rounded-lg flex items-center justify-center">
                    <Server className="w-4 h-4 text-green-600" />
                  </div>
                  <div>
                    <div className="font-medium text-gray-900">网络配置</div>
                    <div className="text-sm text-gray-600">IP 地址映射</div>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )
    }

    // Traditional Cluster Configuration
    return (
      <div className="space-y-8">
        {/* Header */}
        <div className="text-center pb-4">
          <div className="mx-auto w-20 h-20 bg-gradient-to-br from-emerald-500 to-green-500 rounded-2xl flex items-center justify-center mb-6 shadow-lg">
            <Server className="w-10 h-10 text-white" />
          </div>
          <h3 className="text-2xl font-bold text-gray-900 mb-2">传统集群配置</h3>
          <p className="text-gray-600 max-w-md mx-auto">
            配置集群主机列表和 SSH 连接信息，支持批量主机管理
          </p>
        </div>

        {/* Host Configuration */}
        <Card className="border-0 shadow-xl bg-gradient-to-br from-white to-green-50/30">
          <CardHeader className="pb-4">
            <CardTitle className="text-lg flex items-center">
              <Server className="w-5 h-5 mr-2 text-green-600" />
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
                rows={8}
                className="font-mono text-sm resize-none"
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
        <Card className="border-0 shadow-xl bg-gradient-to-br from-white to-gray-50/30">
          <CardHeader className="pb-4">
            <CardTitle className="text-lg flex items-center">
              <Shield className="w-5 h-5 mr-2 text-gray-600" />
              SSH 连接凭证
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-3">
                <Label htmlFor="sshUser" className="text-sm font-medium">SSH 用户名</Label>
                <Input
                  id="sshUser"
                  placeholder="root"
                  value={step1Data.sshUser}
                  onChange={(e) => setStep1Data(prev => ({ ...prev, sshUser: e.target.value }))}
                  className="h-12"
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
                  className="h-12"
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
                  className="h-12 pr-12"
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

        {/* Tips */}
        <Card className="border-0 shadow-lg bg-gradient-to-br from-blue-50 to-indigo-50">
          <CardContent className="pt-6">
            <div className="flex items-start space-x-3">
              <Info className="w-5 h-5 text-blue-600 mt-0.5 flex-shrink-0" />
              <div className="space-y-2">
                <div className="font-medium text-blue-900">配置提示</div>
                <ul className="text-sm text-blue-800 space-y-1">
                  <li>• 确保所有主机可通过 SSH 连接，且使用相同的用户名和密码</li>
                  <li>• 如需使用不同密码的主机，请分批添加和配置</li>
                  <li>• 建议使用 SSH 密钥认证以提高安全性（后续步骤配置）</li>
                </ul>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    )
  }

  // 渲染其他步骤的占位内容
  const renderStepContent = () => {
    switch (currentStep) {
      case 1:
        return renderStep1()
      case 2:
        return (
          <div className="text-center py-12">
            <FileText className="mx-auto w-16 h-16 text-gray-400 mb-4" />
            <h3 className="text-xl font-semibold text-gray-900 mb-2">选择服务</h3>
            <p className="text-gray-600">此步骤正在开发中...</p>
          </div>
        )
      case 3:
        return (
          <div className="text-center py-12">
            <FileText className="mx-auto w-16 h-16 text-gray-400 mb-4" />
            <h3 className="text-xl font-semibold text-gray-900 mb-2">配置服务</h3>
            <p className="text-gray-600">此步骤正在开发中...</p>
          </div>
        )
      case 4:
        return (
          <div className="text-center py-12">
            <CheckCircle className="mx-auto w-16 h-16 text-green-500 mb-4" />
            <h3 className="text-xl font-semibold text-gray-900 mb-2">完成安装</h3>
            <p className="text-gray-600">准备开始安装集群...</p>
          </div>
        )
      default:
        return null
    }
  }

  if (!cluster) return null

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-hidden">
        <div className="flex flex-col h-full">
          {/* 头部 */}
          <div className="flex items-center justify-between p-6 border-b">
            <div>
              <DialogTitle className="text-2xl font-bold">配置集群</DialogTitle>
              <p className="text-gray-600 mt-1">
                集群: {cluster.clusterName} 
                <Badge className="ml-2">
                  {isK8s ? 'Kubernetes' : '传统部署'}
                </Badge>
              </p>
            </div>
            <Button variant="ghost" size="sm" onClick={() => onOpenChange(false)}>
              <X className="w-5 h-5" />
            </Button>
          </div>

          {/* 步骤指示器 */}
          <div className="px-6 py-4 border-b bg-gray-50">
            <div className="flex items-center justify-between">
              {steps.map((step, index) => (
                <div key={step.number} className="flex items-center">
                  <div className={`flex items-center justify-center w-8 h-8 rounded-full text-sm font-medium ${
                    currentStep >= step.number 
                      ? 'bg-blue-600 text-white' 
                      : 'bg-gray-200 text-gray-600'
                  }`}>
                    {currentStep > step.number ? (
                      <CheckCircle className="w-5 h-5" />
                    ) : (
                      step.number
                    )}
                  </div>
                  <div className="ml-3">
                    <p className={`text-sm font-medium ${
                      currentStep >= step.number ? 'text-gray-900' : 'text-gray-500'
                    }`}>
                      {step.title}
                    </p>
                    <p className="text-xs text-gray-500">{step.description}</p>
                  </div>
                  {index < steps.length - 1 && (
                    <div className={`w-12 h-px mx-4 ${
                      currentStep > step.number ? 'bg-blue-600' : 'bg-gray-200'
                    }`} />
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* 步骤内容 */}
          <div className="flex-1 overflow-y-auto p-6">
            {renderStepContent()}
          </div>

          {/* 底部操作按钮 */}
          <div className="flex items-center justify-between p-6 border-t bg-gray-50">
            <Button variant="outline" onClick={() => onOpenChange(false)}>
              取消
            </Button>
            <div className="flex items-center space-x-3">
              {currentStep > 1 && (
                <Button variant="outline" onClick={handlePrevious}>
                  <ChevronLeft className="w-4 h-4 mr-2" />
                  上一步
                </Button>
              )}
              <Button onClick={handleNext} disabled={loading}>
                {loading && <Loader2 className="w-4 h-4 mr-2 animate-spin" />}
                {currentStep < steps.length ? (
                  <>
                    下一步
                    <ChevronRight className="w-4 h-4 ml-2" />
                  </>
                ) : (
                  '完成配置'
                )}
              </Button>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}

export default ClusterSetupDialog