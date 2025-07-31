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
import { clusterApi } from "@/lib/api"
import { toast } from 'sonner'
import ClusterWizardSidebar from './cluster-wizard-sidebar'
import Image from "next/image"
import { getStepsByType, StepsType, DepType } from '@/lib/cluster-steps'

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
  onStep1Complete?: (step1Data: Step1Data) => void
}

export interface Step1Data {
  // Traditional cluster (PVM)
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
  clusterVersion?: string // K8S集群版本信息
}

const ClusterStep1Dialog: React.FC<ClusterSetupDialogProps> = ({
  open,
  onOpenChange,
  cluster,
  onSuccess,
  onStep1Complete
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
    customNamespace: '',
    clusterVersion: ''
  })

  const isK8s = cluster?.depType?.toLowerCase() === 'kubernetes'

  // 使用标准化的步骤配置
  const steps = getStepsByType(
    StepsType.NORMAL,
    isK8s ? DepType.KUBERNETES : DepType.PVM
  )

  // 根据集群类型获取图标路径 (与集群列表保持一致)
  const getIconPath = () => {
    switch (cluster?.depType) {
      case "Kubernetes":
        return "/images/cluster/kubernetes-logo.svg";
      case "PVM":
        return "/images/cluster/linux-tux.svg";
      default:
        return "/images/cluster/kubernetes-logo.svg";
    }
  }

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
      customNamespace: '',
      clusterVersion: ''
    })
    // 清除文件输入框的值
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
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
        // 自动解析命名空间
        parseKubeConfigNamespaces(content)
        setLoading(false)
        // 清除文件输入框的值，确保下次选择同一文件也能触发onChange
        if (event.target) {
          event.target.value = ''
        }
      }
      reader.onerror = () => {
        toast.error('文件读取失败')
        setLoading(false)
        // 清除文件输入框的值
        if (event.target) {
          event.target.value = ''
        }
      }
      reader.readAsText(file)
    }
  }

  // 解析kubeconfig中的命名空间 (按照老项目的逻辑)
  const parseKubeConfigNamespaces = async (content: string) => {
    if (!content?.trim()) {
      console.log('kubeConfigContent为空，跳过命名空间获取')
      return
    }
    
    console.log('开始获取命名空间，kubeConfigContent长度:', content.length)
    setNamespacesLoading(true)
    try {
      // 按照老项目的API调用方式
      const response = await clusterApi.config.getNamespaces(content)
      console.log('获取命名空间API响应:', response.data)
      
      if (response.data?.code === 200) {
        // 按照实际的响应格式处理 - 数据在嵌套的data对象中
        const responseData = response.data.data || {}
        const namespaces = responseData.namespaces || []
        const defaultNamespace = responseData.defaultNamespace
        const clusterVersion = responseData.clusterVersion
        const showNamespaceSelector = responseData.showNamespaceSelector
        
        console.log('解析到的数据:', { 
          namespaces: namespaces.length, 
          defaultNamespace, 
          clusterVersion,
          showNamespaceSelector 
        })
        
        setStep1Data(prev => ({ 
          ...prev, 
          namespaces,
          // 如果有默认命名空间，自动设置
          namespace: defaultNamespace || prev.namespace,
          // 存储集群版本信息
          clusterVersion: clusterVersion || prev.clusterVersion
        }))
        
        toast.success(`成功获取到 ${namespaces.length} 个命名空间${clusterVersion ? ` (Kubernetes ${clusterVersion})` : ''}`)
      } else {
        toast.error(response.data?.msg || '获取命名空间失败')
      }
    } catch (error: any) {
      console.error('获取命名空间失败:', error)
      console.error('错误详情:', error.response?.data)
      toast.error(`获取命名空间失败: ${error.response?.data?.msg || error.message}`)
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
      if (!step1Data.sshPort?.trim()) {
        toast.error('请输入SSH端口')
        return false
      }
      if (!step1Data.sshPassword?.trim()) {
        toast.error('请输入SSH密码')
        return false
      }
    }
    return true
  }

  // 下一步 (按照原Vue2项目的逻辑: step1完成后打开step2对话框)
  const handleNext = async () => {
    if (currentStep === 1) {
      if (!validateStep1()) {
        return
      }
      
      // 按照原项目逻辑，step1验证通过后打开step2对话框
      console.log('Step1 验证通过，打开Step2主机环境校验')
      
      // 调用回调函数，传递step1数据给父组件
      onStep1Complete?.(step1Data)
      
      // 关闭step1对话框
      onOpenChange(false)
      return
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
            <div className="mx-auto w-20 h-20 bg-gradient-to-br from-blue-500 via-indigo-600 to-purple-500 rounded-3xl flex items-center justify-center mb-6 shadow-2xl">
              <div className="w-12 h-12 relative">
                <Image
                  src={getIconPath()}
                  alt="Kubernetes"
                  width={48}
                  height={48}
                  className="object-contain"
                />
              </div>
            </div>
            <h3 className="text-2xl font-bold bg-gradient-to-r from-blue-600 via-indigo-600 to-purple-600 bg-clip-text text-transparent mb-2">Kubernetes 集群配置</h3>
            <p className="text-slate-600 max-w-md mx-auto">
              上传或输入 Kubernetes 配置文件，系统将自动识别可用的命名空间
            </p>
          </div>

          {/* 主配置区域 - 使用左右分栏布局 */}
          <div className="grid grid-cols-1 xl:grid-cols-3 gap-8">
            {/* Kubeconfig Configuration */}
            <div className="xl:col-span-2">
              <Card className="border-0 shadow-2xl bg-white/80 backdrop-blur-sm rounded-3xl">
                <CardHeader className="pb-4">
                  <CardTitle className="text-lg flex items-center">
                    <FileText className="w-5 h-5 mr-2 text-indigo-600" />
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
                        accept="*"
                        onChange={handleFileUpload}
                        className="hidden"
                      />
                      <Button
                        type="button"
                        variant="outline"
                        onClick={() => fileInputRef.current?.click()}
                        className="h-12 border-dashed border-2 hover:border-indigo-400 hover:bg-indigo-50/50 transition-all duration-300 rounded-2xl"
                        disabled={loading}
                      >
                        {loading ? (
                          <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                        ) : (
                          <Upload className="w-4 h-4 mr-2" />
                        )}
                        选择 kubeconfig 文件
                      </Button>
                      <p className="text-xs text-slate-500 mt-2">
                        支持 ~/.kube/config 文件（无扩展名）或 .yaml/.yml 格式
                      </p>
                    </div>
                    
                    <div className="relative">
                      <Textarea
                        placeholder="或者直接粘贴 kubeconfig 内容...&#10;&#10;支持标准的 ~/.kube/config 文件（无扩展名）&#10;也支持 .yaml、.yml 等格式的配置文件"
                        value={step1Data.kubeConfigContent || ''}
                        onChange={(e) => {
                          setStep1Data(prev => ({ ...prev, kubeConfigContent: e.target.value }))
                          if (e.target.value.trim()) {
                            parseKubeConfigNamespaces(e.target.value)
                          }
                        }}
                        rows={12}
                        className="font-mono text-sm resize-none border-gray-200 focus:border-indigo-400 focus:ring-indigo-400 rounded-2xl"
                      />
                      {step1Data.kubeConfigContent && (
                        <div className="absolute top-2 right-2 flex space-x-1">
                          <Button
                            variant="ghost"
                            size="sm"
                            className="h-8 w-8 p-0 hover:bg-blue-100"
                            onClick={() => parseKubeConfigNamespaces(step1Data.kubeConfigContent || '')}
                            disabled={namespacesLoading}
                            title="测试获取命名空间"
                          >
                            {namespacesLoading ? (
                              <Loader2 className="w-4 h-4 text-blue-600 animate-spin" />
                            ) : (
                              <CheckCircle className="w-4 h-4 text-blue-600" />
                            )}
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            className="h-8 w-8 p-0 hover:bg-red-100"
                            onClick={() => {
                              // 清除状态
                              setStep1Data(prev => ({ 
                                ...prev, 
                                kubeConfigContent: '', 
                                namespace: '', 
                                namespaces: [],
                                clusterVersion: ''
                              }))
                              // 清除文件输入框的值
                              if (fileInputRef.current) {
                                fileInputRef.current.value = ''
                              }
                            }}
                          >
                            <X className="w-4 h-4 text-gray-400 hover:text-red-600" />
                          </Button>
                        </div>
                      )}
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>

            {/* Namespace Selection */}
            <div className="xl:col-span-1 w-full">
              <Card className="border-0 shadow-2xl bg-white/80 backdrop-blur-sm rounded-3xl w-full">
                <CardHeader className="pb-4">
                  <CardTitle className="text-lg flex items-center">
                    <Cloud className="w-5 h-5 mr-2 text-purple-600" />
                    命名空间配置
                  </CardTitle>
                </CardHeader>
                <CardContent className="space-y-6 w-full">
                  <div className="space-y-3 w-full">
                    <Label className="text-sm font-medium flex items-center flex-wrap gap-2">
                      命名空间
                      {step1Data.kubeConfigContent && (
                        <>
                          <Badge variant="secondary" className="text-xs">
                            {namespacesLoading ? '加载中...' : `${step1Data.namespaces?.length || 0} 个可用`}
                          </Badge>
                          {step1Data.clusterVersion && (
                            <Badge variant="outline" className="text-xs bg-blue-50 text-blue-700 border-blue-200">
                              Kubernetes {step1Data.clusterVersion}
                            </Badge>
                          )}
                        </>
                      )}
                    </Label>
                    
                    {!step1Data.kubeConfigContent ? (
                      <div className="p-4 bg-gray-50 rounded-lg border-2 border-dashed border-gray-200 w-full">
                        <div className="flex items-center text-gray-500">
                          <Info className="w-4 h-4 mr-2" />
                          <span className="text-sm">请先输入 Kubernetes 配置文件</span>
                        </div>
                      </div>
                    ) : (
                      <div className="space-y-3 w-full">
                        {step1Data.isCreatingNewNamespace ? (
                          /* Create New Namespace Mode */
                          <div className="space-y-4 w-full">
                            {/* 新建模式标题 */}
                            <div className="flex items-center p-3 bg-gradient-to-r from-green-50 to-emerald-50 rounded-xl border border-green-200">
                              <Plus className="w-4 h-4 mr-2 text-green-600" />
                              <span className="text-sm font-medium text-green-800">创建新命名空间</span>
                            </div>
                            
                            <div className="relative w-full">
                              <Input
                                placeholder="输入新的命名空间名称 (例如: my-project)"
                                value={step1Data.customNamespace || ''}
                                onChange={(e) => setStep1Data(prev => ({ 
                                  ...prev, 
                                  customNamespace: e.target.value,
                                  namespace: e.target.value 
                                }))}
                                className="w-full h-12 pr-20 rounded-xl border-green-200 focus:border-green-400 focus:ring-green-200"
                              />
                              <Button
                                variant="ghost"
                                size="sm"
                                className="absolute right-1 top-1 h-8 px-3 text-xs hover:bg-gray-100"
                                onClick={handleCancelCreateNamespace}
                              >
                                <X className="w-3 h-3 mr-1" />
                                取消
                              </Button>
                            </div>
                            
                            {step1Data.customNamespace && (
                              <div className="bg-gradient-to-r from-green-50 to-emerald-50 border border-green-200 rounded-xl p-4">
                                <div className="flex items-start space-x-3">
                                  <div className="flex-shrink-0 w-8 h-8 bg-green-100 rounded-lg flex items-center justify-center">
                                    <Plus className="w-4 h-4 text-green-600" />
                                  </div>
                                  <div className="flex-1">
                                    <h4 className="text-sm font-medium text-green-900 mb-1">将创建新命名空间</h4>
                                    <p className="text-sm text-green-700">
                                      命名空间名称: <code className="bg-green-100 px-2 py-1 rounded text-green-800 font-mono">{step1Data.customNamespace}</code>
                                    </p>
                                    <p className="text-xs text-green-600 mt-2">
                                      ✓ 将在 Kubernetes 集群中自动创建此命名空间
                                    </p>
                                  </div>
                                </div>
                              </div>
                            )}
                          </div>
                        ) : (
                          /* Select Existing Namespace Mode */
                          <div className="space-y-4 w-full">
                            {/* 选择模式标题 */}
                            <div className="flex items-center p-3 bg-gradient-to-r from-blue-50 to-cyan-50 rounded-xl border border-blue-200">
                              <Cloud className="w-4 h-4 mr-2 text-blue-600" />
                              <span className="text-sm font-medium text-blue-800">选择现有命名空间</span>
                              {step1Data.namespaces && step1Data.namespaces.length > 0 && (
                                <Badge variant="secondary" className="ml-2 text-xs bg-blue-100 text-blue-700">
                                  {step1Data.namespaces.length} 个可用
                                </Badge>
                              )}
                            </div>
                            
                            <Select
                              value={step1Data.namespace}
                              onValueChange={(value) => {
                                if (value === '__create_new__') {
                                  handleCreateNamespace()
                                } else {
                                  handleSelectNamespace(value)
                                }
                              }}
                              disabled={namespacesLoading}
                            >
                              <SelectTrigger className="w-full h-12 rounded-xl border-blue-200 focus:border-blue-400 focus:ring-blue-200">
                                <SelectValue placeholder={
                                  namespacesLoading ? "🔄 加载命名空间中..." : "🔍 选择或搜索命名空间"
                                } />
                              </SelectTrigger>
                              <SelectContent className="w-full min-w-[400px]">
                                <div className="p-2">
                                  <div className="relative">
                                    <Search className="absolute left-2 top-2.5 h-4 w-4 text-gray-400" />
                                    <Input
                                      placeholder="搜索命名空间..."
                                      value={namespaceSearch}
                                      onChange={(e) => setNamespaceSearch(e.target.value)}
                                      className="pl-8 h-9 rounded-lg"
                                    />
                                  </div>
                                </div>
                                <Separator className="my-1" />
                                
                                {/* 创建新命名空间选项 */}
                                <SelectItem value="__create_new__" className="font-medium text-green-600 hover:bg-green-50">
                                  <div className="flex items-center">
                                    <div className="w-6 h-6 bg-green-100 rounded-full flex items-center justify-center mr-2">
                                      <Plus className="w-3 h-3 text-green-600" />
                                    </div>
                                    <span>创建新的命名空间</span>
                                  </div>
                                </SelectItem>
                                
                                {/* 现有命名空间列表 */}
                                {filteredNamespaces.length > 0 && (
                                  <>
                                    <Separator className="my-1" />
                                    <div className="px-2 py-1">
                                      <span className="text-xs text-gray-500 font-medium">现有命名空间</span>
                                    </div>
                                    {filteredNamespaces.map((ns) => (
                                      <SelectItem key={ns} value={ns} className="hover:bg-blue-50">
                                        <div className="flex items-center justify-between w-full">
                                          <div className="flex items-center">
                                            <div className="w-6 h-6 bg-blue-100 rounded-full flex items-center justify-center mr-2">
                                              <Cloud className="w-3 h-3 text-blue-600" />
                                            </div>
                                            <span className="font-mono text-sm">{ns}</span>
                                          </div>
                                          {step1Data.namespace === ns && (
                                            <div className="flex items-center">
                                              <Check className="w-4 h-4 text-green-600" />
                                              <span className="text-xs text-green-600 ml-1">已选择</span>
                                            </div>
                                          )}
                                        </div>
                                      </SelectItem>
                                    ))}
                                  </>
                                )}
                                
                                {/* 无匹配结果 */}
                                {filteredNamespaces.length === 0 && namespaceSearch && (
                                  <div className="p-4 text-center">
                                    <div className="text-gray-400 mb-2">
                                      <Search className="w-8 h-8 mx-auto" />
                                    </div>
                                    <p className="text-sm text-gray-500">没有找到匹配的命名空间</p>
                                    <p className="text-xs text-gray-400 mt-1">尝试创建新的命名空间</p>
                                  </div>
                                )}
                                
                                {/* 暂无命名空间 */}
                                {!namespacesLoading && step1Data.namespaces?.length === 0 && (
                                  <div className="p-4 text-center">
                                    <div className="text-yellow-400 mb-2">
                                      <AlertCircle className="w-8 h-8 mx-auto" />
                                    </div>
                                    <p className="text-sm text-gray-600">暂无可用的命名空间</p>
                                    <p className="text-xs text-gray-400 mt-1">请创建一个新的命名空间</p>
                                  </div>
                                )}
                              </SelectContent>
                            </Select>
                          </div>
                        )}
                      </div>
                    )}
                  </div>

                  {/* 命名空间信息预览 */}
                  {step1Data.namespace && (
                    <div className="mt-6 w-full">
                      <div className={`p-4 rounded-xl border-2 ${
                        step1Data.isCreatingNewNamespace 
                          ? 'bg-gradient-to-r from-green-50 to-emerald-50 border-green-200' 
                          : 'bg-gradient-to-r from-blue-50 to-cyan-50 border-blue-200'
                      }`}>
                        <div className="flex items-start space-x-3">
                          <div className={`flex-shrink-0 w-10 h-10 rounded-lg flex items-center justify-center ${
                            step1Data.isCreatingNewNamespace 
                              ? 'bg-green-100' 
                              : 'bg-blue-100'
                          }`}>
                            {step1Data.isCreatingNewNamespace ? (
                              <Plus className="w-5 h-5 text-green-600" />
                            ) : (
                              <Cloud className="w-5 h-5 text-blue-600" />
                            )}
                          </div>
                          <div className="flex-1">
                            <h4 className={`text-sm font-semibold mb-2 ${
                              step1Data.isCreatingNewNamespace ? 'text-green-900' : 'text-blue-900'
                            }`}>
                              命名空间配置确认
                            </h4>
                            <div className="space-y-2">
                              <div className="flex items-center justify-between">
                                <span className="text-xs text-gray-600 font-medium">名称:</span>
                                <code className={`text-sm font-mono px-2 py-1 rounded ${
                                  step1Data.isCreatingNewNamespace 
                                    ? 'bg-green-100 text-green-800' 
                                    : 'bg-blue-100 text-blue-800'
                                }`}>
                                  {step1Data.namespace}
                                </code>
                              </div>
                              <div className="flex items-center justify-between">
                                <span className="text-xs text-gray-600 font-medium">状态:</span>
                                <div className="flex items-center">
                                  {step1Data.isCreatingNewNamespace ? (
                                    <>
                                      <div className="w-2 h-2 bg-green-500 rounded-full mr-2"></div>
                                      <span className="text-xs font-medium text-green-700">将创建</span>
                                    </>
                                  ) : (
                                    <>
                                      <div className="w-2 h-2 bg-blue-500 rounded-full mr-2"></div>
                                      <span className="text-xs font-medium text-blue-700">已存在</span>
                                    </>
                                  )}
                                </div>
                              </div>
                              {step1Data.clusterVersion && (
                                <div className="flex items-center justify-between">
                                  <span className="text-xs text-gray-600 font-medium">集群版本:</span>
                                  <Badge variant="outline" className="text-xs bg-purple-50 text-purple-700 border-purple-200">
                                    Kubernetes {step1Data.clusterVersion}
                                  </Badge>
                                </div>
                              )}
                            </div>
                            <div className={`mt-3 p-2 rounded-lg text-xs ${
                              step1Data.isCreatingNewNamespace 
                                ? 'bg-green-100 text-green-700' 
                                : 'bg-blue-100 text-blue-700'
                            }`}>
                              {step1Data.isCreatingNewNamespace ? (
                                <>✨ 系统将在部署时自动创建此命名空间</>
                              ) : (
                                <>🎯 将使用现有的命名空间进行部署</>
                              )}
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  )}
                </CardContent>
              </Card>
            </div>
          </div>

          {/* K8s Auto-Discovery Info */}
          <Card className="border-0 shadow-2xl bg-white/80 backdrop-blur-sm rounded-3xl">
            <CardHeader className="pb-4">
              <CardTitle className="text-lg flex items-center text-indigo-700">
                <Zap className="w-5 h-5 mr-2" />
                自动获取集群信息
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
                <div className="flex items-center space-x-3">
                  <div className="w-8 h-8 bg-indigo-100 rounded-xl flex items-center justify-center">
                    <Network className="w-4 h-4 text-indigo-600" />
                  </div>
                  <div>
                    <div className="font-medium text-slate-900">节点信息</div>
                    <div className="text-sm text-slate-600">自动识别 worker 节点</div>
                  </div>
                </div>
                <div className="flex items-center space-x-3">
                  <div className="w-8 h-8 bg-purple-100 rounded-xl flex items-center justify-center">
                    <Cpu className="w-4 h-4 text-purple-600" />
                  </div>
                  <div>
                    <div className="font-medium text-slate-900">CPU 架构</div>
                    <div className="text-sm text-slate-600">识别处理器架构</div>
                  </div>
                </div>
                <div className="flex items-center space-x-3">
                  <div className="w-8 h-8 bg-pink-100 rounded-xl flex items-center justify-center">
                    <Shield className="w-4 h-4 text-pink-600" />
                  </div>
                  <div>
                    <div className="font-medium text-slate-900">节点状态</div>
                    <div className="text-sm text-slate-600">实时资源信息</div>
                  </div>
                </div>
                <div className="flex items-center space-x-3">
                  <div className="w-8 h-8 bg-indigo-100 rounded-xl flex items-center justify-center">
                    <Server className="w-4 h-4 text-indigo-600" />
                  </div>
                  <div>
                    <div className="font-medium text-slate-900">网络配置</div>
                    <div className="text-sm text-slate-600">IP 地址映射</div>
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
    <Dialog open={open} onOpenChange={() => {}}>
      <DialogContent className="!max-w-none !w-[min(calc(100vw-64px),1800px)] !max-h-[min(calc(100vh-96px),900px)] sm:!w-[min(95vw,1800px)] sm:!max-h-[min(95vh,900px)] border-0 shadow-2xl bg-white rounded-3xl !fixed !top-1/2 !left-1/2 !-translate-x-1/2 !-translate-y-1/2 !m-0 [&>button]:hidden overflow-hidden">
        <div className="flex h-full max-h-[min(calc(100vh-96px),900px)] sm:max-h-[min(95vh,900px)]">
          <ClusterWizardSidebar
            steps={steps}
            currentStep={currentStep}
            title="配置集群"
            clusterName={cluster.clusterName}
            isK8s={isK8s}
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
                    {steps[currentStep - 1]?.title}
                  </h2>
                  <p className="text-gray-600 mt-1">
                    {steps[currentStep - 1]?.description}
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
                {renderStepContent()}
              </div>
            </div>

            {/* 底部操作按钮 */}
            <div className="p-6 sm:p-8 border-t border-slate-200/50 bg-white/90 backdrop-blur-sm relative">
              {/* 装饰性分割线光效 */}
              <div className="absolute top-0 left-8 right-8 h-px bg-gradient-to-r from-transparent via-slate-300/60 to-transparent"></div>
              <div className="flex items-center justify-between gap-4 relative z-10">
                <div>
                  {currentStep > 1 && (
                    <Button 
                      variant="outline" 
                      onClick={handlePrevious}
                      className="px-8 py-3 rounded-2xl bg-white/80 hover:bg-white border border-white/50 hover:shadow-lg transition-all duration-300 text-slate-700 hover:scale-105 backdrop-blur-sm"
                    >
                      <ChevronLeft className="w-4 h-4 mr-2" />
                      上一步
                    </Button>
                  )}
                </div>
                <Button 
                  onClick={handleNext} 
                  disabled={loading}
                  className="px-10 py-3 bg-gradient-to-r from-indigo-500 via-purple-600 to-pink-500 hover:from-indigo-600 hover:via-purple-700 hover:to-pink-600 text-white font-medium rounded-2xl border-0 shadow-xl hover:shadow-2xl transition-all duration-500 hover:scale-105 relative overflow-hidden group"
                >
                  {/* 按钮光效 */}
                  <div className="absolute inset-0 bg-gradient-to-r from-white/0 via-white/25 to-white/0 translate-x-[-100%] group-hover:translate-x-[100%] transition-transform duration-1000" />
                  {loading && <Loader2 className="w-4 h-4 mr-2 animate-spin relative z-10" />}
                  {currentStep < steps.length ? (
                    <>
                      <span className="relative z-10">下一步</span>
                      <ChevronRight className="w-4 h-4 ml-2 relative z-10" />
                    </>
                  ) : (
                    <span className="relative z-10">完成配置</span>
                  )}
                </Button>
              </div>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}

export default ClusterStep1Dialog