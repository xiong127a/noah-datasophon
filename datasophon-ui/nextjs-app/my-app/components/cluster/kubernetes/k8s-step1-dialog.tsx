"use client"

import React, { useState, useEffect, useRef } from 'react'
import { 
  ChevronRight, CheckCircle, Loader2, Upload, Search, 
  X, Info, Network, Cpu, Shield, Server, FileText, Zap,
  Cloud, Plus, Check, AlertCircle
} from 'lucide-react'
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { clusterApi } from "@/lib/api"
import { toast } from 'sonner'
import ClusterWizardSidebar from '../common/cluster-wizard-sidebar'
import Image from "next/image"
import { getStepsByType, StepsType } from '@/lib/cluster-steps'
import { DIALOG_STYLES } from '../common/shared-styles'

// K8S集群信息接口
export interface K8sClusterInfo {
  id: number
  clusterName: string
  depType: string
  clusterCode: string
}

// K8S Step1数据接口
export interface K8sStep1Data {
  kubeConfigContent: string
  namespace: string
  namespaces: string[]
  isCreatingNewNamespace: boolean
  customNamespace?: string
  clusterVersion?: string
}

// K8S Step1弹窗属性接口
export interface K8sStep1DialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  cluster: K8sClusterInfo | null
  onStep1Complete: (data: K8sStep1Data) => void
}

export default function K8sStep1Dialog({
  open,
  onOpenChange,
  cluster,
  onStep1Complete
}: K8sStep1DialogProps) {
  const [step1Data, setStep1Data] = useState<K8sStep1Data>({
    kubeConfigContent: '',
    namespace: '',
    namespaces: [],
    isCreatingNewNamespace: false,
    customNamespace: '',
    clusterVersion: ''
  })
  
  const [loading, setLoading] = useState(false)
  const [namespacesLoading, setNamespacesLoading] = useState(false)
  const [namespaceSearch, setNamespaceSearch] = useState('')
  const fileInputRef = useRef<HTMLInputElement>(null)
  const searchInputRef = useRef<HTMLInputElement>(null)

  const steps = getStepsByType('kubernetes' as StepsType)
  const currentStep = 1

  // 获取集群类型图标路径
  const getIconPath = () => "/images/cluster/kubernetes-logo.svg"

  // 清空表单数据
  const clearFormData = () => {
    setStep1Data({
      kubeConfigContent: '',
      namespace: '',
      namespaces: [],
      isCreatingNewNamespace: false,
      clusterVersion: ''
    })
    setNamespaceSearch('')
  }

  // 监听弹窗关闭，清空数据
  useEffect(() => {
    if (!open) {
      clearFormData()
    }
  }, [open])

  // 文件上传处理
  const handleFileUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (file) {
      const reader = new FileReader()
      reader.onload = (e) => {
        const content = e.target?.result as string
        setStep1Data(prev => ({ ...prev, kubeConfigContent: content }))
        parseKubeConfigNamespaces(content)
      }
      reader.readAsText(file)
    }
  }

  // 解析kubeconfig获取命名空间
  const parseKubeConfigNamespaces = async (kubeConfigContent: string) => {
    if (!kubeConfigContent.trim()) return

    setNamespacesLoading(true)
    try {
      const response = await clusterApi.config.getNamespaces(kubeConfigContent)
      
      if (response.data?.success) {
        const responseData = response.data.data || {}
        const namespaces = responseData.namespaces || []
        const defaultNamespace = responseData.defaultNamespace
        const clusterVersion = responseData.clusterVersion || ''
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
          // 设置是否为新建命名空间的标记
          isCreatingNewNamespace: defaultNamespace ? !namespaces.includes(defaultNamespace) : prev.isCreatingNewNamespace,
          // 存储集群版本信息
          clusterVersion: clusterVersion || prev.clusterVersion
        }))
        
        toast.success(`成功获取到 ${namespaces.length} 个命名空间${clusterVersion ? ` (Kubernetes ${clusterVersion})` : ''}${defaultNamespace ? `，已自动选择默认命名空间：${defaultNamespace}` : ''}`)
      } else {
        console.error('获取命名空间失败:', response.data?.message)
        toast.error(`无法获取命名空间: ${response.data?.message || '请检查配置文件格式'}`)
        setStep1Data(prev => ({ 
          ...prev, 
          namespaces: [], 
          clusterVersion: '' 
        }))
      }
    } catch (error) {
      console.error('解析kubeconfig异常:', error)
      toast.error('解析配置文件失败，请检查文件格式是否正确')
      setStep1Data(prev => ({ 
        ...prev, 
        namespaces: [], 
        clusterVersion: '' 
      }))
    } finally {
      setNamespacesLoading(false)
    }
  }

  // 监听kubeconfig内容变化
  useEffect(() => {
    if (step1Data.kubeConfigContent?.trim()) {
      const timer = setTimeout(() => {
        parseKubeConfigNamespaces(step1Data.kubeConfigContent)
      }, 1000) // 防抖
      return () => clearTimeout(timer)
    } else {
      setStep1Data(prev => ({ 
        ...prev, 
        namespaces: [], 
        namespace: '', 
        clusterVersion: '' 
      }))
    }
  }, [step1Data.kubeConfigContent])

  // 创建新命名空间
  const handleCreateNamespace = () => {
    setStep1Data(prev => ({ ...prev, isCreatingNewNamespace: true, namespace: '', customNamespace: '' }))
  }

  // 取消创建命名空间
  const handleCancelCreateNamespace = () => {
    setStep1Data(prev => ({ 
      ...prev, 
      isCreatingNewNamespace: false, 
      customNamespace: '',
      namespace: ''
    }))
  }

  // 选择命名空间
  const handleSelectNamespace = (namespace: string) => {
    setStep1Data(prev => ({ 
      ...prev, 
      namespace,
      // 判断是否为新创建的命名空间
      isCreatingNewNamespace: !step1Data.namespaces.includes(namespace)
    }))
  }

  // 过滤命名空间
  const filteredNamespaces = step1Data.namespaces.filter(ns => 
    ns.toLowerCase().includes(namespaceSearch.toLowerCase())
  ) || []

  // 验证Step1数据
  const validateStep1 = (): boolean => {
    if (!step1Data.kubeConfigContent?.trim()) {
      toast.error('请输入或上传Kubernetes配置文件')
      return false
    }
    if (!step1Data.namespace?.trim()) {
      toast.error('请选择或输入命名空间')
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
          Kubernetes集群配置 - {cluster?.clusterName}
        </DialogTitle>
        
        <div className="flex h-full max-h-[min(calc(100vh-96px),900px)] sm:max-h-[min(95vh,900px)]">
          {/* 左侧导航 */}
          <ClusterWizardSidebar 
            steps={steps}
            currentStep={currentStep}
            title="Kubernetes集群配置"
            clusterName={cluster?.clusterName || ''}
            isK8s={true}
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
                    Kubernetes集群配置
                  </h2>
                  <p className="text-gray-600 mt-1">
                    配置Kubernetes集群连接信息和命名空间
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
                  <div className="grid grid-cols-1 xl:grid-cols-2 gap-8">
                    {/* Kubeconfig Configuration */}
                    <div>
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
                                    title="清除内容"
                                  >
                                    <X className="w-4 h-4 text-red-600" />
                                  </Button>
                                </div>
                              )}
                            </div>
                          </div>
                        </CardContent>
                      </Card>
                    </div>

                    {/* Namespace Configuration */}
                    <div>
                      <Card className="border-0 shadow-2xl bg-white/80 backdrop-blur-sm rounded-3xl">
                        <CardHeader className="pb-4">
                          <CardTitle className="text-lg flex items-center">
                            <Info className="w-5 h-5 mr-2 text-indigo-600" />
                            命名空间
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
                                          // 清空搜索框
                                          setNamespaceSearch('')
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
                                              ref={searchInputRef}
                                              placeholder="搜索命名空间..."
                                              value={namespaceSearch}
                                              onChange={(e) => {
                                                // 正常的输入处理
                                                setNamespaceSearch(e.target.value)
                                                // 强制保持焦点
                                                setTimeout(() => {
                                                  if (searchInputRef.current && document.activeElement !== searchInputRef.current) {
                                                    searchInputRef.current.focus()
                                                  }
                                                }, 0)
                                              }}
                                              className="pl-8 h-9 rounded-lg"
                                              onKeyDown={(e) => {
                                                // 只阻止特定的导航键，让输入正常进行
                                                if (['ArrowUp', 'ArrowDown', 'Tab'].includes(e.key)) {
                                                  e.stopPropagation()
                                                }
                                                
                                                if (e.key === 'Enter') {
                                                  e.preventDefault()
                                                  e.stopPropagation()
                                                  if (namespaceSearch && !filteredNamespaces.includes(namespaceSearch)) {
                                                    handleSelectNamespace(namespaceSearch)
                                                    ;(e.target as HTMLInputElement).blur()
                                                  }
                                                } else if (e.key === 'Escape') {
                                                  e.stopPropagation()
                                                  setNamespaceSearch('')
                                                }
                                              }}
                                              onInput={(e) => {
                                                // 强制保持焦点在输入时
                                                e.stopPropagation()
                                              }}
                                              onFocus={(e) => {
                                                e.stopPropagation()
                                              }}
                                              onClick={(e) => {
                                                e.stopPropagation()
                                                // 确保点击时获得焦点
                                                ;(e.target as HTMLInputElement).focus()
                                              }}
                                              autoFocus
                                            />
                                          </div>
                                        </div>
                                        <div className="my-1 border-t border-gray-200" />
                                        
                                        {/* 创建新命名空间选项 */}
                                        <SelectItem value="__create_new__" className="font-medium text-green-600 hover:bg-green-50">
                                          <div className="flex items-center">
                                            <div className="w-6 h-6 bg-green-100 rounded-full flex items-center justify-center mr-2">
                                              <Plus className="w-3 h-3 text-green-600" />
                                            </div>
                                            <span>创建新的命名空间</span>
                                          </div>
                                        </SelectItem>
                                        
                                        {/* 搜索匹配的新建选项 */}
                                        {namespaceSearch && !filteredNamespaces.includes(namespaceSearch) && (
                                          <SelectItem value={namespaceSearch} className="font-medium text-green-600 hover:bg-green-50">
                                            <div className="flex items-center">
                                              <div className="w-6 h-6 bg-green-100 rounded-full flex items-center justify-center mr-2">
                                                <Plus className="w-3 h-3 text-green-600" />
                                              </div>
                                              <span>{namespaceSearch} (新建)</span>
                                            </div>
                                          </SelectItem>
                                        )}
                                        
                                        {/* 现有命名空间列表 */}
                                        {filteredNamespaces.length > 0 && (
                                          <>
                                            <div className="my-1 border-t border-gray-200" />
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
                  disabled={loading || !step1Data.kubeConfigContent || !step1Data.namespace}
                  className={`flex items-center px-6 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 shadow-md hover:shadow-lg ${
                    loading || !step1Data.kubeConfigContent || !step1Data.namespace
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
