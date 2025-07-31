"use client"

import { useState, useEffect } from "react"
import { X, CheckCircle, Loader2, Database, Settings, Star, Sparkles, Cloud, Server } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog"
import { Card, CardContent } from "@/components/ui/card"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Badge } from "@/components/ui/badge"
import { apiClient, API_PATHS } from "@/lib/api-config"

interface CreateClusterDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSuccess?: () => void
  editData?: {
    id: number
    clusterName: string
    clusterCode: string
    clusterFrame: string
    depType: string
  } | null
}

interface Framework {
  frameCode: string
  frameName?: string
}

export default function CreateClusterDialogEnhanced({ 
  open, 
  onOpenChange, 
  onSuccess,
  editData = null 
}: CreateClusterDialogProps) {
  const [formData, setFormData] = useState({
    clusterName: "",
    clusterCode: "",
    clusterFrame: "",
    depType: "",
  })

  const [focusedField, setFocusedField] = useState<string>("")
  const [frameworks, setFrameworks] = useState<Framework[]>([])
  const [loading, setLoading] = useState(false)
  const [frameworkLoading, setFrameworkLoading] = useState(false)

  const isEdit = editData !== null

  const deploymentOptions = [
    {
      id: "PVM",
      title: "裸金属/虚拟机",
      description: "传统部署方式，直接在物理机或虚拟机上运行",
      iconPath: "/images/cluster/linux-tux.svg",
      fallbackIcon: Server,
      gradient: "from-emerald-500 via-green-600 to-teal-500",
      bgGradient: "from-emerald-50/80 via-green-100/40 to-teal-50/80",
      shadowColor: "shadow-emerald-500/25",
      glowColor: "from-emerald-400/30 to-teal-400/30",
      badgeColor: "bg-emerald-100 text-emerald-700 border-emerald-200",
    },
    {
      id: "Kubernetes", 
      title: "Kubernetes",
      description: "容器化部署，支持自动化运维和弹性伸缩",
      iconPath: "/images/cluster/kubernetes-logo.svg",
      fallbackIcon: Cloud,
      gradient: "from-blue-500 via-blue-600 to-cyan-500",
      bgGradient: "from-blue-50/80 via-blue-100/40 to-cyan-50/80",
      shadowColor: "shadow-blue-500/25",
      glowColor: "from-blue-400/30 to-cyan-400/30",
      badgeColor: "bg-blue-100 text-blue-700 border-blue-200",
    },
  ]

  // 获取框架列表
  const fetchFrameworks = async () => {
    setFrameworkLoading(true)
    try {
      const response = await apiClient.post(API_PATHS.FRAME_LIST, {})
      if (response.data && response.data.code === 200) {
        setFrameworks(response.data.data || [])
      } else {
        console.error('获取框架列表失败:', response.data?.msg)
      }
    } catch (error) {
      console.error('获取框架列表失败:', error)
    } finally {
      setFrameworkLoading(false)
    }
  }

  // 当对话框打开时，获取框架列表并初始化表单
  useEffect(() => {
    if (open) {
      fetchFrameworks()
      
      if (editData) {
        // 编辑模式，填充表单数据
        setFormData({
          clusterName: editData.clusterName || "",
          clusterCode: editData.clusterCode || "",
          clusterFrame: editData.clusterFrame || "",
          depType: editData.depType || "",
        })
      } else {
        // 新建模式，重置表单
        setFormData({
          clusterName: "",
          clusterCode: "",
          clusterFrame: "",
          depType: "",
        })
      }
    }
  }, [open, editData])

  const handleCreate = async () => {
    if (formData.clusterName && formData.clusterCode && formData.clusterFrame && formData.depType) {
      setLoading(true)
      try {
        // 获取当前用户信息（修复localStorage key）
        const userStr = typeof window !== 'undefined' ? localStorage.getItem('user_info') : null
        const currentUser = userStr ? JSON.parse(userStr) : null

        // 检查用户登录状态
        if (!currentUser || !currentUser.id) {
          alert('用户登录信息异常，请重新登录')
          console.error('用户信息异常:', { userStr, currentUser })
          return
        }

        const params: any = {
          clusterName: formData.clusterName.trim(),
          clusterCode: formData.clusterCode.trim(),
          clusterFrame: formData.clusterFrame,
          depType: formData.depType,
          createBy: currentUser.username,
          // 默认将当前用户设置为集群管理员
          clusterManagerList: [{
            id: currentUser.id
          }]
        }

        // 如果是编辑模式，添加集群ID
        if (isEdit && editData) {
          params.id = editData.id
        }

        const apiUrl = isEdit ? API_PATHS.CLUSTER_UPDATE : API_PATHS.CLUSTER_SAVE
        const url = isEdit ? `${apiUrl}?clusterId=${editData?.id}` : apiUrl
        
        const response = await apiClient.post(url, params)
        
        if (response.data && response.data.code === 200) {
          console.log(`${isEdit ? '更新' : '创建'}集群成功`)
          onSuccess?.()
          handleCancel()
        } else {
          console.error(`${isEdit ? '更新' : '创建'}集群失败:`, response.data?.msg)
          alert(response.data?.msg || `${isEdit ? '更新' : '创建'}集群失败`)
        }
      } catch (error) {
        console.error(`${isEdit ? '更新' : '创建'}集群失败:`, error)
        alert(`${isEdit ? '更新' : '创建'}集群失败，请稍后重试`)
      } finally {
        setLoading(false)
      }
    }
  }

  const handleCancel = () => {
    onOpenChange(false)
    setFormData({
      clusterName: "",
      clusterCode: "",
      clusterFrame: "",
      depType: "",
    })
    setFocusedField("")
  }

  const isFieldValid = (field: string, value: string) => {
    return value.trim().length > 0
  }

  const isFormValid = () => {
    return formData.clusterName.trim() && 
           formData.clusterCode.trim() && 
           formData.clusterFrame && 
           formData.depType
  }

  return (
    <Dialog open={open} onOpenChange={() => {}}>
      <DialogContent className="max-w-3xl bg-white rounded-3xl border-0 shadow-2xl overflow-hidden [&>button]:hidden">
        {/* 头部设计 - 仿照集群列表风格 */}
        <div className="relative -m-6 mb-6 overflow-hidden rounded-t-3xl">
          <div className="absolute inset-0 bg-gradient-to-br from-slate-100 via-blue-50 to-indigo-50" />
          
          {/* 装饰性光效 */}
          <div className="absolute top-0 right-0 w-40 h-40 bg-gradient-to-br from-blue-400/20 to-purple-400/20 rounded-full blur-3xl transform translate-x-20 -translate-y-20" />
          <div className="absolute bottom-0 left-0 w-32 h-32 bg-gradient-to-tr from-indigo-400/20 to-pink-400/20 rounded-full blur-2xl transform -translate-x-16 translate-y-16" />
          
          {/* 边框光效 */}
          <div className="absolute inset-0 rounded-t-3xl bg-gradient-to-r from-blue-500 via-indigo-500 to-purple-500 opacity-10" />

          <div className="relative p-6">
            <button
              onClick={handleCancel}
              className="absolute right-4 top-4 w-10 h-10 rounded-full bg-white/80 backdrop-blur-sm hover:bg-white border border-white/50 hover:border-white/70 flex items-center justify-center transition-all duration-300 shadow-lg hover:shadow-xl group"
            >
              <X className="h-5 w-5 text-slate-600 group-hover:text-slate-800 transition-colors" />
            </button>
            <DialogTitle className="text-2xl font-bold bg-gradient-to-r from-slate-800 to-slate-600 bg-clip-text text-transparent pr-12">
              {isEdit ? '编辑集群' : '创建新集群'}
            </DialogTitle>
            <DialogDescription className="text-slate-600 mt-2">
              {isEdit ? '修改集群的基本配置信息' : '配置您的大数据平台集群环境'}
            </DialogDescription>
          </div>
        </div>

        {/* 表单内容 */}
        <div className="px-6 pb-6 space-y-8">
          {/* 基本信息区域 */}
          <div className="space-y-6">
            <div className="flex items-center space-x-3 mb-4">
              <div className="w-8 h-8 rounded-full bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center shadow-lg">
                <Database className="h-4 w-4 text-white" />
              </div>
              <div>
                <h3 className="text-xl font-bold bg-gradient-to-r from-slate-800 to-slate-600 bg-clip-text text-transparent">
                  基本信息
                </h3>
                <p className="text-sm text-slate-600">设置集群的基本标识信息</p>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* 集群名称 */}
              <div className="space-y-2">
                <Label htmlFor="clusterName" className="text-sm font-semibold text-slate-700 flex items-center space-x-2">
                  <span>集群名称</span>
                  <div className={`w-2 h-2 rounded-full shadow-lg transition-all duration-300 ${
                    isFieldValid("clusterName", formData.clusterName) 
                      ? "bg-gradient-to-r from-green-400 to-emerald-400" 
                      : "bg-gradient-to-r from-red-400 to-pink-400 animate-pulse"
                  }`} />
                </Label>
                <div className="relative group">
                  <Input
                    id="clusterName"
                    placeholder="请输入集群名称"
                    maxLength={10}
                    value={formData.clusterName}
                    onChange={(e) => setFormData({ ...formData, clusterName: e.target.value })}
                    onFocus={() => setFocusedField("clusterName")}
                    onBlur={() => setFocusedField("")}
                    className={`rounded-xl h-10 transition-all duration-300 border-2 ${
                      focusedField === "clusterName"
                        ? "border-blue-300 bg-blue-50/50 ring-2 ring-blue-100"
                        : isFieldValid("clusterName", formData.clusterName)
                        ? "border-green-300 bg-green-50/30"
                        : "border-slate-200 bg-white/80 hover:border-slate-300"
                    }`}
                  />
                  {isFieldValid("clusterName", formData.clusterName) && (
                    <CheckCircle className="absolute right-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-green-500" />
                  )}
                </div>
              </div>

              {/* 集群编码 */}
              <div className="space-y-2">
                <Label htmlFor="clusterCode" className="text-sm font-semibold text-slate-700 flex items-center space-x-2">
                  <span>集群编码</span>
                  <div className={`w-2 h-2 rounded-full shadow-lg transition-all duration-300 ${
                    isFieldValid("clusterCode", formData.clusterCode) 
                      ? "bg-gradient-to-r from-green-400 to-emerald-400" 
                      : "bg-gradient-to-r from-red-400 to-pink-400 animate-pulse"
                  }`} />
                </Label>
                <div className="relative group">
                  <Input
                    id="clusterCode"
                    placeholder="请输入集群编码"
                    maxLength={10}
                    disabled={isEdit}
                    value={formData.clusterCode}
                    onChange={(e) => setFormData({ ...formData, clusterCode: e.target.value })}
                    onFocus={() => setFocusedField("clusterCode")}
                    onBlur={() => setFocusedField("")}
                    className={`rounded-xl h-10 transition-all duration-300 border-2 ${
                      isEdit 
                        ? "bg-slate-50 border-slate-200 text-slate-500 cursor-not-allowed"
                        : focusedField === "clusterCode"
                        ? "border-blue-300 bg-blue-50/50 ring-2 ring-blue-100"
                        : isFieldValid("clusterCode", formData.clusterCode)
                        ? "border-green-300 bg-green-50/30"
                        : "border-slate-200 bg-white/80 hover:border-slate-300"
                    }`}
                  />
                  {isFieldValid("clusterCode", formData.clusterCode) && (
                    <CheckCircle className="absolute right-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-green-500" />
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* 技术配置区域 */}
          <div className="space-y-6">
            <div className="flex items-center space-x-3 mb-4">
              <div className="w-8 h-8 rounded-full bg-gradient-to-br from-purple-500 to-pink-600 flex items-center justify-center shadow-lg">
                <Settings className="h-4 w-4 text-white" />
              </div>
              <div>
                <h3 className="text-xl font-bold bg-gradient-to-r from-slate-800 to-slate-600 bg-clip-text text-transparent">
                  技术配置
                </h3>
                <p className="text-sm text-slate-600">选择集群的技术框架和部署方式</p>
              </div>
            </div>

            {/* 集群框架 */}
            <div className="space-y-2">
              <Label className="text-sm font-semibold text-slate-700 flex items-center space-x-2">
                <span>集群框架</span>
                <div className={`w-2 h-2 rounded-full shadow-lg transition-all duration-300 ${
                  formData.clusterFrame
                    ? "bg-gradient-to-r from-green-400 to-emerald-400" 
                    : "bg-gradient-to-r from-red-400 to-pink-400 animate-pulse"
                }`} />
              </Label>
              <div className="relative">
                <Select
                  value={formData.clusterFrame}
                  onValueChange={(value) => setFormData({ ...formData, clusterFrame: value })}
                  disabled={isEdit || frameworkLoading}
                >
                  <SelectTrigger className={`rounded-xl h-10 transition-all duration-300 border-2 ${
                    isEdit 
                      ? "bg-slate-50 border-slate-200 text-slate-500 cursor-not-allowed"
                      : formData.clusterFrame
                      ? "border-green-300 bg-green-50/30"
                      : "border-slate-200 bg-white/80 hover:border-blue-300"
                  }`}>
                    {frameworkLoading ? (
                      <div className="flex items-center space-x-2">
                        <Loader2 className="h-4 w-4 animate-spin" />
                        <span>加载框架列表...</span>
                      </div>
                    ) : (
                      <SelectValue placeholder="请选择集群框架" />
                    )}
                  </SelectTrigger>
                  <SelectContent className="rounded-xl border-0 shadow-2xl bg-white/95 backdrop-blur-xl">
                    {frameworks.map((framework) => (
                      <SelectItem
                        key={framework.frameCode}
                        value={framework.frameCode}
                        className="rounded-lg m-1 hover:bg-slate-50 transition-colors"
                      >
                        <div className="flex flex-col">
                          <span className="font-medium">{framework.frameCode}</span>
                          {framework.frameName && (
                            <span className="text-xs text-slate-500">{framework.frameName}</span>
                          )}
                        </div>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {formData.clusterFrame && (
                  <CheckCircle className="absolute right-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-green-500 pointer-events-none" />
                )}
              </div>
            </div>

            {/* 部署方式 - 仿照集群列表风格 */}
            <div className="space-y-4">
              <Label className="text-sm font-semibold text-slate-700 flex items-center space-x-2">
                <span>集群部署方式</span>
                <div className={`w-2 h-2 rounded-full shadow-lg transition-all duration-300 ${
                  formData.depType
                    ? "bg-gradient-to-r from-green-400 to-emerald-400" 
                    : "bg-gradient-to-r from-red-400 to-pink-400 animate-pulse"
                }`} />
              </Label>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {deploymentOptions.map((option) => {
                  const isSelected = formData.depType === option.id
                  const isDisabled = isEdit
                  const FallbackIcon = option.fallbackIcon
                  return (
                    <Card
                      key={option.id}
                      className={`group relative overflow-hidden rounded-2xl border-0 bg-white shadow-lg hover:shadow-xl transition-all duration-500 cursor-pointer h-36 ${
                        isDisabled 
                          ? "opacity-60 cursor-not-allowed"
                          : "hover:-translate-y-2"
                      } ${isSelected ? option.shadowColor : ""}`}
                      onClick={() => !isDisabled && setFormData({ ...formData, depType: option.id })}
                    >
                      {/* 主背景渐变 */}
                      <div className={`absolute inset-0 bg-gradient-to-br ${isSelected ? option.bgGradient : "from-slate-50/80 to-white"}`} />
                      
                      {/* 动态光效背景 */}
                      <div className={`absolute inset-0 bg-gradient-to-br ${option.glowColor} opacity-0 group-hover:opacity-100 transition-opacity duration-700`} />
                      
                      {/* 装饰性光效 */}
                      <div className="absolute top-0 right-0 w-24 h-24 bg-gradient-to-br from-white/30 to-transparent rounded-full blur-2xl transform translate-x-12 -translate-y-12 group-hover:scale-125 transition-transform duration-700" />
                      
                      {/* 边框光效 */}
                      <div className={`absolute inset-0 rounded-2xl bg-gradient-to-r ${option.gradient} opacity-0 group-hover:opacity-20 transition-opacity duration-500 blur-sm`} />

                      <CardContent className="relative p-4 z-10 h-full flex flex-col">
                        <div className="flex items-start space-x-3 flex-1">
                          {/* 图标容器 - 3D效果 */}
                          <div className="relative perspective-1000">
                            <div className={`relative p-2.5 rounded-xl bg-gradient-to-br ${option.gradient} shadow-lg group-hover:scale-110 transition-all duration-500 preserve-3d`}>
                              <img 
                                src={option.iconPath}
                                alt={option.title}
                                width={24}
                                height={24}
                                className="relative z-10 group-hover:rotate-12 transition-transform duration-500"
                                onError={(e) => {
                                  // SVG加载失败时显示fallback图标
                                  e.currentTarget.style.display = 'none';
                                  const fallback = e.currentTarget.nextElementSibling as HTMLElement;
                                  if (fallback) fallback.style.display = 'block';
                                }}
                              />
                              <FallbackIcon 
                                className="w-6 h-6 text-white hidden relative z-10 group-hover:rotate-12 transition-transform duration-500" 
                              />
                              <div className="absolute inset-0 rounded-xl bg-white/25 backdrop-blur-sm" />
                            </div>
                            {/* 悬浮装饰 */}
                            {isSelected && (
                              <div className="absolute -top-1 -right-1 w-4 h-4 bg-gradient-to-r from-yellow-400 to-orange-400 rounded-full flex items-center justify-center animate-bounce">
                                <Star className="h-2 w-2 text-white" />
                              </div>
                            )}
                          </div>
                          
                          <div className="flex-1 min-w-0">
                            <h4 className="text-base font-bold bg-gradient-to-r from-slate-800 to-slate-600 bg-clip-text text-transparent mb-1">
                              {option.title}
                            </h4>
                            <p className="text-xs text-slate-600 leading-relaxed mb-2 line-clamp-2">{option.description}</p>
                            <Badge className={`${option.badgeColor} border-0 rounded-full px-2 py-0.5 text-xs font-medium shadow-sm`}>
                              {option.id === "PVM" ? "物理机部署" : "容器化部署"}
                            </Badge>
                          </div>
                          
                          {/* 选中状态指示 */}
                          {isSelected && (
                            <div className="relative">
                              <div className="w-5 h-5 rounded-full bg-green-400 shadow-lg relative z-10 flex items-center justify-center">
                                <CheckCircle className="h-3 w-3 text-white" />
                                <div className="absolute inset-0 rounded-full bg-green-400 animate-ping" />
                              </div>
                              <div className="absolute inset-0 w-5 h-5 rounded-full bg-green-400 blur-md opacity-75" />
                            </div>
                          )}
                        </div>
                      </CardContent>
                    </Card>
                  )
                })}
              </div>
            </div>
          </div>
        </div>

        {/* 底部按钮 */}
        <div className="px-6 py-4 bg-gradient-to-r from-slate-50 to-blue-50 border-t border-slate-100 flex justify-end space-x-3">
          <Button
            variant="outline"
            onClick={handleCancel}
            disabled={loading}
            className="px-6 h-10 rounded-xl border-slate-200 bg-white/80 backdrop-blur-sm hover:bg-white hover:border-slate-300 transition-all duration-300 shadow-lg hover:shadow-xl"
          >
            取消
          </Button>
          <Button
            onClick={handleCreate}
            disabled={!isFormValid() || loading}
            className={`px-6 h-10 rounded-xl border-0 transition-all duration-300 relative overflow-hidden ${
              isFormValid() && !loading
                ? "bg-gradient-to-r from-blue-500 to-indigo-600 hover:from-blue-600 hover:to-indigo-700 text-white shadow-lg hover:shadow-xl hover:-translate-y-1"
                : "bg-slate-200 text-slate-400 cursor-not-allowed"
            }`}
          >
            {/* 按钮光效 */}
            {isFormValid() && !loading && (
              <div className="absolute inset-0 bg-gradient-to-r from-white/0 via-white/25 to-white/0 translate-x-[-100%] hover:translate-x-[100%] transition-transform duration-1000" />
            )}
            <span className="relative z-10 flex items-center">
              {loading ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin mr-2" />
                  {isEdit ? '保存中...' : '创建中...'}
                </>
              ) : (
                <>
                  <Sparkles className="h-4 w-4 mr-2" />
                  {isEdit ? '保存修改' : '创建集群'}
                </>
              )}
            </span>
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  )
}
