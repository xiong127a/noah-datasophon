"use client"

import React, { useState, useEffect, useRef } from 'react'
import { 
  X, ChevronRight, CheckCircle, Loader2, Upload, Search, Check, 
  AlertCircle, Info, Network, Cloud
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
import { ClusterType } from '@/types'
import { DIALOG_STYLES } from '../common/shared-styles'
import type { 
  KubernetesStep1Data, 
  KubernetesStep1DialogProps
} from './kubernetes-types'

/**
 * Kubernetes集群配置第一步 - 专用于Kubernetes集群
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 * 日期：2024-01-20
 */

const KubernetesClusterStep1Dialog: React.FC<KubernetesStep1DialogProps> = ({
  open,
  onOpenChange,
  cluster,
  onStep1Complete
}) => {
  const clusterType = ClusterType.KUBERNETES // 固定为Kubernetes类型
  
  // 使用标准化的步骤配置
  const steps = getStepsByType(StepsType.NORMAL, clusterType)

  // Kubernetes配置状态
  const [kubeConfigContent, setKubeConfigContent] = useState('')
  const [namespace, setNamespace] = useState('')
  const [namespaces, setNamespaces] = useState<string[]>([])
  const [isCreatingNewNamespace, setIsCreatingNewNamespace] = useState(false)
  const [customNamespace, setCustomNamespace] = useState('')
  const [clusterVersion, setClusterVersion] = useState('')

  // 界面状态
  const [loading, setLoading] = useState(false)
  const [currentStep] = useState(1)
  const [namespaceLoading, setNamespaceLoading] = useState(false)
  
  // 文件上传引用
  const fileInputRef = useRef<HTMLInputElement>(null)

  // 验证Kubernetes配置
  const validateKubernetesConfig = (): boolean => {
    if (!kubeConfigContent.trim()) {
      toast.error('请提供Kubernetes配置文件内容')
      return false
    }

    if (isCreatingNewNamespace) {
      if (!customNamespace.trim()) {
        toast.error('请输入自定义命名空间名称')
        return false
      }
      if (!/^[a-z0-9]([-a-z0-9]*[a-z0-9])?$/.test(customNamespace)) {
        toast.error('命名空间名称格式不正确（仅支持小写字母、数字和连字符）')
        return false
      }
    } else {
      if (!namespace) {
        toast.error('请选择一个命名空间')
        return false
      }
    }

    return true
  }

  // 获取命名空间列表
  const fetchNamespaces = async () => {
    if (!kubeConfigContent.trim()) {
      toast.warning('请先提供Kubernetes配置文件内容')
      return
    }

    setNamespaceLoading(true)
    try {
      console.log('正在获取命名空间列表...')
      const response = await clusterApi.config.getNamespaces(kubeConfigContent)
      
      if (response.data && response.data.code === 200) {
        const namespaceList = response.data.data || []
        setNamespaces(namespaceList)
        console.log('获取到命名空间列表:', namespaceList)
        
        if (namespaceList.length > 0 && !namespace) {
          setNamespace(namespaceList.includes('default') ? 'default' : namespaceList[0])
        }
        
        toast.success(`成功获取 ${namespaceList.length} 个命名空间`)
      } else {
        console.error('获取命名空间失败:', response.data)
        toast.error(response.data?.msg || '获取命名空间失败，请检查配置文件')
        setNamespaces([])
      }
    } catch (error) {
      console.error('获取命名空间出错:', error)
      toast.error('获取命名空间失败，请检查网络连接和配置文件')
      setNamespaces([])
    } finally {
      setNamespaceLoading(false)
    }
  }

  // 文件上传处理
  const handleFileUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return

    const reader = new FileReader()
    reader.onload = (e) => {
      const content = e.target?.result as string
      setKubeConfigContent(content)
      toast.success('配置文件上传成功')
    }
    reader.onerror = () => {
      toast.error('读取配置文件失败')
    }
    reader.readAsText(file)
  }

  // 构建Step1数据
  const buildStep1Data = (): KubernetesStep1Data => ({
    kubeConfigContent: kubeConfigContent.trim(),
    namespace: isCreatingNewNamespace ? customNamespace.trim() : namespace,
    namespaces,
    isCreatingNewNamespace,
    customNamespace: customNamespace.trim(),
    clusterVersion: clusterVersion || '未知'
  })

  // 下一步处理
  const handleNext = async () => {
    if (!validateKubernetesConfig()) {
      return
    }
    
    setLoading(true)
    try {
      console.log('Kubernetes Step1 验证通过，准备进入下一步')
      
      const step1Data = buildStep1Data()
      console.log('Step1数据:', step1Data)
      
      // 调用回调函数，传递step1数据给父组件
      onStep1Complete?.(step1Data)
    } finally {
      setLoading(false)
    }
  }

  // 智能解析KubeConfig并提取集群版本信息
  useEffect(() => {
    if (kubeConfigContent) {
      try {
        // 尝试解析YAML中的集群信息
        const lines = kubeConfigContent.split('\n')
        const serverLine = lines.find(line => line.includes('server:'))
        if (serverLine) {
          // 简单的版本检测，实际使用时可能需要调用K8S API
          setClusterVersion('检测中...')
        }
      } catch (error) {
        console.log('解析KubeConfig时出错:', error)
      }
    }
  }, [kubeConfigContent])

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="!max-w-none !w-[min(calc(100vw-32px),1900px)] !max-h-[calc(100vh-32px)] sm:!w-[min(98vw,1900px)] sm:!max-h-[calc(98vh-32px)] border-0 shadow-2xl bg-white rounded-3xl !fixed !top-1/2 !left-1/2 !-translate-x-1/2 !-translate-y-1/2 !m-0 [&>button]:hidden flex flex-col p-0 gap-0">
        <DialogTitle className="sr-only">
          Kubernetes集群配置 - {cluster?.clusterName}
        </DialogTitle>
        
        <div className="flex h-full max-h-[calc(100vh-32px)] sm:max-h-[calc(98vh-32px)]">
          {/* 左侧导航 */}
          <ClusterWizardSidebar 
            steps={steps}
            currentStep={currentStep}
            title="Kubernetes集群配置"
            clusterName={cluster?.clusterName || ''}
            isK8s={true}
            onClose={() => onOpenChange(false)}
          />

          {/* 右侧内容 */}
          <div className="flex-1 flex flex-col min-w-0">
            {/* 头部 */}
            <div className={DIALOG_STYLES.header}>
              <div>
                <h2 className={DIALOG_STYLES.title}>
                  <div className={`${DIALOG_STYLES.iconContainer} bg-blue-600`}>
                    <Cloud className="w-4 h-4" />
                  </div>
                  Kubernetes集群配置
                </h2>
                <p className="text-gray-600 mt-1">
                  配置Kubernetes集群连接信息和命名空间
                </p>
              </div>
              <Button
                variant="ghost"
                size="icon"
                onClick={() => onOpenChange(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-5 h-5" />
              </Button>
            </div>

            {/* 主内容区域 */}
            <div className="flex-1 p-6 overflow-y-auto">
              <div className="max-w-4xl mx-auto space-y-8">
                {/* K8S配置文件上传 */}
                <Card className="border-blue-200 bg-blue-50/50">
                  <CardHeader className="pb-4">
                    <CardTitle className="text-lg flex items-center text-blue-800">
                      <Upload className="w-5 h-5 mr-2" />
                      Kubernetes配置文件
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      {/* 配置文件上传 */}
                      <div className="space-y-4">
                        <Label className="text-sm font-medium text-gray-700">
                          上传KubeConfig文件 <span className="text-red-500">*</span>
                        </Label>
                        <div className="space-y-3">
                          <Button
                            type="button"
                            variant="outline"
                            onClick={() => fileInputRef.current?.click()}
                            className="w-full h-12 border-dashed border-blue-300 text-blue-700 hover:bg-blue-50"
                          >
                            <Upload className="w-4 h-4 mr-2" />
                            选择配置文件
                          </Button>
                          <input
                            ref={fileInputRef}
                            type="file"
                            accept=".yaml,.yml,.config"
                            onChange={handleFileUpload}
                            className="hidden"
                          />
                        </div>
                      </div>

                      {/* 或者手动输入 */}
                      <div className="space-y-4">
                        <Label className="text-sm font-medium text-gray-700">
                          或直接粘贴配置内容
                        </Label>
                        <Textarea
                          placeholder="粘贴KubeConfig配置内容..."
                          value={kubeConfigContent}
                          onChange={(e) => setKubeConfigContent(e.target.value)}
                          className="min-h-[100px] font-mono text-sm"
                        />
                      </div>
                    </div>

                    {kubeConfigContent && (
                      <div className="mt-4 p-3 bg-green-50 rounded-lg border border-green-200">
                        <div className="flex items-center text-green-700">
                          <CheckCircle className="w-4 h-4 mr-2" />
                          <span className="text-sm font-medium">
                            配置文件已加载 ({kubeConfigContent.length} 字符)
                          </span>
                        </div>
                      </div>
                    )}
                  </CardContent>
                </Card>

                {/* 命名空间配置 */}
                {kubeConfigContent && (
                  <Card>
                    <CardHeader className="pb-4">
                      <div className="flex items-center justify-between">
                        <CardTitle className="text-lg flex items-center">
                          <Network className="w-5 h-5 mr-2 text-purple-600" />
                          命名空间配置
                        </CardTitle>
                        <Button
                          onClick={fetchNamespaces}
                          disabled={namespaceLoading}
                          variant="outline"
                          size="sm"
                        >
                          {namespaceLoading ? (
                            <>
                              <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                              获取中...
                            </>
                          ) : (
                            <>
                              <Search className="w-4 h-4 mr-2" />
                              获取命名空间
                            </>
                          )}
                        </Button>
                      </div>
                    </CardHeader>
                    <CardContent className="space-y-4">
                      {/* 命名空间选择模式 */}
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div 
                          className={`p-4 rounded-lg border-2 cursor-pointer transition-all ${
                            !isCreatingNewNamespace 
                              ? 'border-blue-500 bg-blue-50' 
                              : 'border-gray-200 hover:border-gray-300'
                          }`}
                          onClick={() => setIsCreatingNewNamespace(false)}
                        >
                          <div className="flex items-center space-x-3">
                            <div className={`w-4 h-4 rounded-full border-2 flex items-center justify-center ${
                              !isCreatingNewNamespace 
                                ? 'border-blue-500 bg-blue-500' 
                                : 'border-gray-300'
                            }`}>
                              {!isCreatingNewNamespace && <Check className="w-2.5 h-2.5 text-white" />}
                            </div>
                            <div>
                              <h4 className="font-medium text-gray-900">使用现有命名空间</h4>
                              <p className="text-sm text-gray-600">从集群中选择已存在的命名空间</p>
                            </div>
                          </div>
                        </div>

                        <div 
                          className={`p-4 rounded-lg border-2 cursor-pointer transition-all ${
                            isCreatingNewNamespace 
                              ? 'border-purple-500 bg-purple-50' 
                              : 'border-gray-200 hover:border-gray-300'
                          }`}
                          onClick={() => setIsCreatingNewNamespace(true)}
                        >
                          <div className="flex items-center space-x-3">
                            <div className={`w-4 h-4 rounded-full border-2 flex items-center justify-center ${
                              isCreatingNewNamespace 
                                ? 'border-purple-500 bg-purple-500' 
                                : 'border-gray-300'
                            }`}>
                              {isCreatingNewNamespace && <Check className="w-2.5 h-2.5 text-white" />}
                            </div>
                            <div>
                              <h4 className="font-medium text-gray-900">创建新命名空间</h4>
                              <p className="text-sm text-gray-600">为此集群创建专用命名空间</p>
                            </div>
                          </div>
                        </div>
                      </div>

                      {/* 命名空间选择/输入 */}
                      {!isCreatingNewNamespace ? (
                        <div className="space-y-2">
                          <Label className="text-sm font-medium text-gray-700">
                            选择命名空间 <span className="text-red-500">*</span>
                          </Label>
                          <Select value={namespace} onValueChange={setNamespace}>
                            <SelectTrigger className="w-full">
                              <SelectValue placeholder="请选择命名空间" />
                            </SelectTrigger>
                            <SelectContent>
                              {namespaces.map((ns) => (
                                <SelectItem key={ns} value={ns}>
                                  <div className="flex items-center space-x-2">
                                    <Badge 
                                      variant={ns === 'default' ? 'default' : 'secondary'}
                                      className="text-xs"
                                    >
                                      {ns === 'default' ? '默认' : '自定义'}
                                    </Badge>
                                    <span>{ns}</span>
                                  </div>
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                          {namespaces.length === 0 && (
                            <p className="text-sm text-amber-600 flex items-center">
                              <AlertCircle className="w-4 h-4 mr-1" />
                              请先点击 &ldquo;获取命名空间&rdquo; 按钮
                            </p>
                          )}
                        </div>
                      ) : (
                        <div className="space-y-2">
                          <Label className="text-sm font-medium text-gray-700">
                            新命名空间名称 <span className="text-red-500">*</span>
                          </Label>
                          <Input
                            placeholder="例如: datasophon-cluster"
                            value={customNamespace}
                            onChange={(e) => setCustomNamespace(e.target.value)}
                            className="w-full"
                          />
                          <p className="text-xs text-gray-500">
                            命名空间名称只能包含小写字母、数字和连字符（-）
                          </p>
                        </div>
                      )}
                    </CardContent>
                  </Card>
                )}

                {/* 集群信息预览 */}
                {kubeConfigContent && namespace && (
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-lg flex items-center">
                        <Info className="w-5 h-5 mr-2 text-green-600" />
                        配置预览
                      </CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div className="space-y-2">
                          <Label className="text-sm text-gray-600">集群类型</Label>
                          <div className="flex items-center space-x-2">
                            <Image 
                              src="/images/cluster/kubernetes-logo.svg"
                              alt="Kubernetes"
                              width={20}
                              height={20}
                            />
                            <span className="font-medium">Kubernetes</span>
                          </div>
                        </div>
                        <div className="space-y-2">
                          <Label className="text-sm text-gray-600">目标命名空间</Label>
                          <div className="font-medium text-blue-700">
                            {isCreatingNewNamespace ? customNamespace : namespace}
                            {isCreatingNewNamespace && (
                              <Badge variant="outline" className="ml-2 text-xs">
                                新建
                              </Badge>
                            )}
                          </div>
                        </div>
                        <div className="space-y-2">
                          <Label className="text-sm text-gray-600">配置文件大小</Label>
                          <span className="font-medium">{kubeConfigContent.length} 字符</span>
                        </div>
                        <div className="space-y-2">
                          <Label className="text-sm text-gray-600">集群版本</Label>
                          <span className="font-medium">{clusterVersion || '待检测'}</span>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                )}
              </div>
            </div>

            {/* 底部按钮 */}
            <div className="border-t bg-white p-6">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-4">
                  <div className="flex items-center space-x-2">
                    <div className="w-3 h-3 rounded-full bg-blue-500"></div>
                    <span className="text-sm text-gray-600">Kubernetes集群配置</span>
                  </div>
                </div>
                
                <div className="flex items-center space-x-3">
                  <Button
                    variant="outline"
                    onClick={() => onOpenChange(false)}
                    className="px-6"
                  >
                    取消
                  </Button>
                  <Button
                    onClick={handleNext}
                    disabled={loading || !kubeConfigContent.trim() || (!namespace && !customNamespace)}
                    className="px-6 bg-blue-600 hover:bg-blue-700 text-white"
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
                  </Button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}

export default KubernetesClusterStep1Dialog
