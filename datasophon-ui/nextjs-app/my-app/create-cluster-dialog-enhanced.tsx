"use client"

import { useState } from "react"
import { X, Server, Cloud } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogTitle } from "@/components/ui/dialog"
import { Card, CardContent } from "@/components/ui/card"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"

interface CreateClusterDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
}

const frameworks = [
  { value: "hadoop", label: "Apache Hadoop", description: "分布式存储和计算框架" },
  { value: "spark", label: "Apache Spark", description: "快速通用的大数据处理引擎" },
  { value: "flink", label: "Apache Flink", description: "流处理和批处理统一框架" },
  { value: "kafka", label: "Apache Kafka", description: "分布式流处理平台" },
  { value: "elasticsearch", label: "Elasticsearch", description: "分布式搜索和分析引擎" },
  { value: "clickhouse", label: "ClickHouse", description: "列式数据库管理系统" },
]

export default function CreateClusterDialogEnhanced({ open, onOpenChange }: CreateClusterDialogProps) {
  const [formData, setFormData] = useState({
    clusterName: "",
    clusterPassword: "",
    framework: "",
    deploymentType: "",
  })

  const [focusedField, setFocusedField] = useState<string>("")

  const deploymentOptions = [
    {
      id: "bare-metal",
      title: "裸金属/虚拟机",
      description: "部署到Linux裸金属或虚拟机上，提供最大的性能和控制权",
      icon: Server,
      color: "from-orange-500 to-red-500",
      bgColor: "from-orange-50 to-red-50",
    },
    {
      id: "kubernetes",
      title: "Kubernetes",
      description: "容器化部署，支持自动化运维和弹性伸缩",
      icon: Cloud,
      color: "from-blue-500 to-cyan-500",
      bgColor: "from-blue-50 to-cyan-50",
    },
  ]

  const handleCreate = () => {
    if (formData.clusterName && formData.clusterPassword && formData.framework && formData.deploymentType) {
      console.log("创建集群:", formData)
      onOpenChange(false)
      setFormData({
        clusterName: "",
        clusterPassword: "",
        framework: "",
        deploymentType: "",
      })
    }
  }

  const handleCancel = () => {
    onOpenChange(false)
    setFormData({
      clusterName: "",
      clusterPassword: "",
      framework: "",
      deploymentType: "",
    })
  }

  const isFieldValid = (field: string, value: string) => {
    return value.length > 0
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="rounded-3xl border-0 shadow-2xl max-w-4xl bg-white/95 backdrop-blur-xl">
        {/* 优雅的头部设计 */}
        <div className="relative -m-6 mb-8 overflow-hidden rounded-t-3xl">
          <div className="absolute inset-0 bg-gradient-to-br from-slate-50 via-white to-blue-50" />
          <div className="absolute inset-0 bg-gradient-to-r from-blue-500/5 via-purple-500/5 to-pink-500/5" />

          {/* 装饰性元素 */}
          <div className="absolute top-0 right-0 w-32 h-32 bg-gradient-to-br from-blue-400/10 to-purple-400/10 rounded-full blur-2xl" />
          <div className="absolute bottom-0 left-0 w-24 h-24 bg-gradient-to-tr from-pink-400/10 to-orange-400/10 rounded-full blur-xl" />

          <div className="relative p-8">
            <button
              onClick={handleCancel}
              className="absolute right-6 top-6 w-10 h-10 rounded-full bg-white/80 backdrop-blur-sm hover:bg-white border border-slate-200 hover:border-slate-300 flex items-center justify-center transition-all duration-200 shadow-lg hover:shadow-xl group"
            >
              <X className="h-5 w-5 text-slate-600 group-hover:text-slate-800 transition-colors" />
            </button>
            <DialogTitle className="text-3xl font-bold bg-gradient-to-r from-slate-800 to-slate-600 bg-clip-text text-transparent pr-16">
              创建新集群
            </DialogTitle>
            <DialogDescription className="text-slate-600 mt-3 text-lg">
              配置您的大数据平台集群，开启智能化数据处理之旅
            </DialogDescription>
          </div>
        </div>

        <div className="space-y-10 px-2">
          {/* 基本信息 */}
          <div className="space-y-6">
            <div className="flex items-center space-x-3 mb-6">
              <div className="w-8 h-8 rounded-full bg-gradient-to-r from-blue-500 to-indigo-600 flex items-center justify-center text-white text-sm font-bold shadow-lg">
                1
              </div>
              <div>
                <h3 className="text-xl font-bold text-slate-800">基本信息</h3>
                <p className="text-slate-600 text-sm">设置集群的基本标识信息</p>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-3">
                <Label
                  htmlFor="clusterName"
                  className="text-sm font-semibold text-slate-700 flex items-center space-x-2"
                >
                  <span>集群名称</span>
                  <div className="w-2 h-2 rounded-full bg-gradient-to-r from-red-400 to-pink-400 shadow-lg animate-pulse" />
                </Label>
                <div className="relative group">
                  <Input
                    id="clusterName"
                    placeholder="请输入集群名称"
                    value={formData.clusterName}
                    onChange={(e) => setFormData({ ...formData, clusterName: e.target.value })}
                    onFocus={() => setFocusedField("clusterName")}
                    onBlur={() => setFocusedField("")}
                    className={`rounded-2xl h-12 transition-all duration-300 ${
                      focusedField === "clusterName" || formData.clusterName
                        ? "border-blue-300 bg-blue-50/50 ring-2 ring-blue-100"
                        : "border-slate-200 bg-white/80"
                    } ${isFieldValid("clusterName", formData.clusterName) ? "border-green-300 bg-green-50/30" : ""}`}
                  />
                  {isFieldValid("clusterName", formData.clusterName) && (
                    <div className="absolute right-3 top-1/2 transform -translate-y-1/2 w-2 h-2 bg-green-400 rounded-full animate-pulse" />
                  )}
                </div>
              </div>

              <div className="space-y-3">
                <Label
                  htmlFor="clusterPassword"
                  className="text-sm font-semibold text-slate-700 flex items-center space-x-2"
                >
                  <span>集群密码</span>
                  <div className="w-2 h-2 rounded-full bg-gradient-to-r from-red-400 to-pink-400 shadow-lg animate-pulse" />
                </Label>
                <div className="relative group">
                  <Input
                    id="clusterPassword"
                    type="password"
                    placeholder="请输入集群密码"
                    value={formData.clusterPassword}
                    onChange={(e) => setFormData({ ...formData, clusterPassword: e.target.value })}
                    onFocus={() => setFocusedField("clusterPassword")}
                    onBlur={() => setFocusedField("")}
                    className={`rounded-2xl h-12 transition-all duration-300 ${
                      focusedField === "clusterPassword" || formData.clusterPassword
                        ? "border-blue-300 bg-blue-50/50 ring-2 ring-blue-100"
                        : "border-slate-200 bg-white/80"
                    } ${
                      isFieldValid("clusterPassword", formData.clusterPassword) ? "border-green-300 bg-green-50/30" : ""
                    }`}
                  />
                  {isFieldValid("clusterPassword", formData.clusterPassword) && (
                    <div className="absolute right-3 top-1/2 transform -translate-y-1/2 w-2 h-2 bg-green-400 rounded-full animate-pulse" />
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* 技术配置 */}
          <div className="space-y-6">
            <div className="flex items-center space-x-3 mb-6">
              <div className="w-8 h-8 rounded-full bg-gradient-to-r from-purple-500 to-pink-600 flex items-center justify-center text-white text-sm font-bold shadow-lg">
                2
              </div>
              <div>
                <h3 className="text-xl font-bold text-slate-800">技术配置</h3>
                <p className="text-slate-600 text-sm">选择集群的技术框架和部署方式</p>
              </div>
            </div>

            {/* 集群框架选择 */}
            <div className="space-y-3">
              <Label className="text-sm font-semibold text-slate-700 flex items-center space-x-2">
                <span>集群框架</span>
                <div className="w-2 h-2 rounded-full bg-gradient-to-r from-red-400 to-pink-400 shadow-lg animate-pulse" />
              </Label>
              <Select
                value={formData.framework}
                onValueChange={(value) => setFormData({ ...formData, framework: value })}
              >
                <SelectTrigger
                  className={`rounded-2xl h-12 transition-all duration-300 ${
                    formData.framework
                      ? "border-green-300 bg-green-50/30"
                      : "border-slate-200 bg-white/80 hover:border-blue-300"
                  }`}
                >
                  <SelectValue placeholder="请选择集群框架" />
                </SelectTrigger>
                <SelectContent className="rounded-2xl border-0 shadow-2xl bg-white/95 backdrop-blur-xl">
                  {frameworks.map((framework) => (
                    <SelectItem
                      key={framework.value}
                      value={framework.value}
                      className="rounded-xl m-1 hover:bg-slate-50"
                    >
                      <div className="flex flex-col">
                        <span className="font-medium">{framework.label}</span>
                        <span className="text-xs text-slate-500">{framework.description}</span>
                      </div>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* 集群部署方式 */}
            <div className="space-y-4">
              <Label className="text-sm font-semibold text-slate-700 flex items-center space-x-2">
                <span>集群部署方式</span>
                <div className="w-2 h-2 rounded-full bg-gradient-to-r from-red-400 to-pink-400 shadow-lg animate-pulse" />
              </Label>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {deploymentOptions.map((option) => {
                  const Icon = option.icon
                  const isSelected = formData.deploymentType === option.id
                  return (
                    <Card
                      key={option.id}
                      className={`cursor-pointer transition-all duration-300 rounded-3xl border-2 transform hover:scale-105 ${
                        isSelected
                          ? "border-blue-400 shadow-2xl bg-gradient-to-br from-blue-50 to-indigo-50"
                          : "border-slate-200 hover:border-slate-300 hover:shadow-xl bg-white/80 backdrop-blur-sm"
                      }`}
                      onClick={() => setFormData({ ...formData, deploymentType: option.id })}
                    >
                      <CardContent className="p-6">
                        <div className="flex items-start space-x-4">
                          <div
                            className={`p-4 rounded-2xl bg-gradient-to-br ${option.color} shadow-xl ${isSelected ? "scale-110" : ""} transition-transform duration-300`}
                          >
                            <Icon className="h-8 w-8 text-white" />
                          </div>
                          <div className="flex-1">
                            <h4 className="font-bold text-slate-800 mb-2 text-lg">{option.title}</h4>
                            <p className="text-sm text-slate-600 leading-relaxed">{option.description}</p>
                          </div>
                          {isSelected && (
                            <div className="w-8 h-8 rounded-full bg-gradient-to-r from-blue-500 to-indigo-600 flex items-center justify-center shadow-lg animate-bounce">
                              <div className="w-3 h-3 bg-white rounded-full" />
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

        <DialogFooter className="flex space-x-4 mt-10">
          <Button
            variant="outline"
            onClick={handleCancel}
            className="flex-1 rounded-2xl border-slate-200 h-12 bg-white/80 backdrop-blur-sm hover:bg-white hover:border-slate-300 transition-all duration-200"
          >
            取消
          </Button>
          <Button
            onClick={handleCreate}
            disabled={
              !formData.clusterName || !formData.clusterPassword || !formData.framework || !formData.deploymentType
            }
            className={`flex-1 rounded-2xl border-0 h-12 transition-all duration-300 ${
              formData.clusterName && formData.clusterPassword && formData.framework && formData.deploymentType
                ? "bg-gradient-to-r from-blue-500 to-indigo-600 hover:from-blue-600 hover:to-indigo-700 text-white shadow-lg hover:shadow-xl transform hover:scale-105"
                : "bg-slate-200 text-slate-400 cursor-not-allowed"
            }`}
          >
            创建集群
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
