"use client"

import { useState } from "react"
import { X, Server, Cloud } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogTitle } from "@/components/ui/dialog"
import { Card, CardContent } from "@/components/ui/card"

interface CreateClusterDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
}

export default function CreateClusterDialog({ open, onOpenChange }: CreateClusterDialogProps) {
  const [formData, setFormData] = useState({
    clusterName: "",
    clusterPassword: "",
    framework: "",
    deploymentType: "",
  })

  const deploymentOptions = [
    {
      id: "bare-metal",
      title: "裸金属/虚拟机",
      description: "部署到Linux裸金属或虚拟机上",
      icon: Server,
      color: "from-orange-500 to-red-500",
    },
    {
      id: "kubernetes",
      title: "Kubernetes",
      description: "容器化部署，支持自动化和弹性伸缩",
      icon: Cloud,
      color: "from-blue-500 to-cyan-500",
    },
  ]

  const handleCreate = () => {
    if (formData.clusterName && formData.clusterPassword && formData.deploymentType) {
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

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="rounded-3xl border-0 shadow-2xl max-w-2xl max-h-[90vh] overflow-y-auto">
        {/* 头部 */}
        <div className="relative bg-gradient-to-r from-blue-500 to-purple-600 -m-6 mb-6 p-8 rounded-t-3xl">
          <button
            onClick={handleCancel}
            className="absolute right-6 top-6 w-8 h-8 rounded-full bg-white/20 hover:bg-white/30 flex items-center justify-center transition-colors"
          >
            <X className="h-4 w-4 text-white" />
          </button>
          <DialogTitle className="text-2xl font-bold text-white pr-10">创建新集群</DialogTitle>
          <DialogDescription className="text-white/80 mt-2">配置您的大数据平台集群信息</DialogDescription>
        </div>

        <div className="space-y-8">
          {/* 基本信息 */}
          <div className="space-y-4">
            <div className="flex items-center space-x-2 mb-4">
              <div className="w-6 h-6 rounded-full bg-blue-500 flex items-center justify-center text-white text-sm font-bold">
                1
              </div>
              <h3 className="text-lg font-bold text-slate-800">基本信息</h3>
            </div>
            <p className="text-slate-600 text-sm mb-4">设置集群的基本标识信息</p>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="clusterName" className="text-sm font-medium text-slate-700">
                  集群名称 <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="clusterName"
                  placeholder="请输入集群名称"
                  value={formData.clusterName}
                  onChange={(e) => setFormData({ ...formData, clusterName: e.target.value })}
                  className="rounded-2xl border-slate-200 h-12"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="clusterPassword" className="text-sm font-medium text-slate-700">
                  集群密码 <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="clusterPassword"
                  type="password"
                  placeholder="请输入集群密码"
                  value={formData.clusterPassword}
                  onChange={(e) => setFormData({ ...formData, clusterPassword: e.target.value })}
                  className="rounded-2xl border-slate-200 h-12"
                />
              </div>
            </div>
          </div>

          {/* 技术配置 */}
          <div className="space-y-4">
            <div className="flex items-center space-x-2 mb-4">
              <div className="w-6 h-6 rounded-full bg-purple-500 flex items-center justify-center text-white text-sm font-bold">
                2
              </div>
              <h3 className="text-lg font-bold text-slate-800">技术配置</h3>
            </div>
            <p className="text-slate-600 text-sm mb-4">选择集群的技术框架和部署方式</p>

            {/* 集群框架 */}
            <div className="space-y-2">
              <Label htmlFor="framework" className="text-sm font-medium text-slate-700">
                集群框架 <span className="text-red-500">*</span>
              </Label>
              <Input
                id="framework"
                placeholder="请选择集群框架"
                value={formData.framework}
                onChange={(e) => setFormData({ ...formData, framework: e.target.value })}
                className="rounded-2xl border-slate-200 h-12"
              />
            </div>

            {/* 集群部署方式 */}
            <div className="space-y-3">
              <Label className="text-sm font-medium text-slate-700">
                集群部署方式 <span className="text-red-500">*</span>
              </Label>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {deploymentOptions.map((option) => {
                  const Icon = option.icon
                  const isSelected = formData.deploymentType === option.id
                  return (
                    <Card
                      key={option.id}
                      className={`cursor-pointer transition-all duration-200 rounded-2xl border-2 ${
                        isSelected
                          ? "border-blue-300 bg-blue-50 shadow-lg"
                          : "border-slate-200 hover:border-slate-300 hover:shadow-md"
                      }`}
                      onClick={() => setFormData({ ...formData, deploymentType: option.id })}
                    >
                      <CardContent className="p-6">
                        <div className="flex items-start space-x-4">
                          <div className={`p-3 rounded-2xl bg-gradient-to-br ${option.color} shadow-lg`}>
                            <Icon className="h-6 w-6 text-white" />
                          </div>
                          <div className="flex-1">
                            <h4 className="font-bold text-slate-800 mb-2">{option.title}</h4>
                            <p className="text-sm text-slate-600">{option.description}</p>
                          </div>
                          {isSelected && (
                            <div className="w-6 h-6 rounded-full bg-blue-500 flex items-center justify-center">
                              <div className="w-2 h-2 bg-white rounded-full" />
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

        <DialogFooter className="flex space-x-3 mt-8">
          <Button
            variant="outline"
            onClick={handleCancel}
            className="flex-1 rounded-2xl border-slate-200 h-12 bg-transparent"
          >
            取消
          </Button>
          <Button
            onClick={handleCreate}
            disabled={!formData.clusterName || !formData.clusterPassword || !formData.deploymentType}
            className="flex-1 rounded-2xl bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700 text-white border-0 h-12"
          >
            创建集群
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
