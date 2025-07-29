"use client"
import { useState } from "react"
import {
  Server,
  Database,
  Cloud,
  Zap,
  Users,
  Calendar,
  Play,
  Edit,
  Shield,
  MoreHorizontal,
  Settings,
  Trash2,
  Plus,
  Sparkles,
  Rocket,
  Brain,
  ChevronRight,
} from "lucide-react"

import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Card, CardContent } from "@/components/ui/card"
import ClusterAuthorizationDialogEnhanced from "./cluster-authorization-dialog-enhanced"
import CreateClusterDialogEnhanced from "./create-cluster-dialog-enhanced"

// 模拟集群数据
const clusters = [
  {
    id: 1,
    name: "生产环境集群",
    type: "Hadoop",
    icon: Database,
    admin: "张三",
    createdAt: "2024-01-15",
    status: "configured",
    color: "from-blue-500 to-cyan-500",
    bgColor: "from-blue-50 to-cyan-50",
  },
  {
    id: 2,
    name: "开发测试集群",
    type: "Spark",
    icon: Zap,
    admin: "李四",
    createdAt: "2024-01-20",
    status: "configured",
    color: "from-orange-500 to-red-500",
    bgColor: "from-orange-50 to-red-50",
  },
  {
    id: 3,
    name: "数据分析集群",
    type: "Kubernetes",
    icon: Cloud,
    admin: "王五",
    createdAt: "2024-01-25",
    status: "unconfigured",
    color: "from-purple-500 to-pink-500",
    bgColor: "from-purple-50 to-pink-50",
  },
  {
    id: 4,
    name: "机器学习集群",
    type: "TensorFlow",
    icon: Brain,
    admin: "赵六",
    createdAt: "2024-02-01",
    status: "configured",
    color: "from-green-500 to-emerald-500",
    bgColor: "from-green-50 to-emerald-50",
  },
]

const ClusterCard = ({ cluster }: { cluster: any }) => {
  const [authDialogOpen, setAuthDialogOpen] = useState(false)
  const Icon = cluster.icon
  const isConfigured = cluster.status === "configured"

  return (
    <>
      <Card className="group relative overflow-hidden rounded-3xl border-0 bg-white shadow-lg hover:shadow-2xl transition-all duration-500 hover:-translate-y-2">
        {/* 背景渐变 */}
        <div className={`absolute inset-0 bg-gradient-to-br ${cluster.bgColor} opacity-50`} />

        {/* 装饰性光效 */}
        <div className="absolute top-0 right-0 w-32 h-32 bg-gradient-to-br from-white/20 to-transparent rounded-full blur-2xl transform translate-x-16 -translate-y-16 group-hover:scale-150 transition-transform duration-700" />

        <CardContent className="relative p-8">
          {/* 头部信息 */}
          <div className="flex items-start justify-between mb-6">
            <div className="flex items-center space-x-4">
              <div className={`relative p-4 rounded-2xl bg-gradient-to-br ${cluster.color} shadow-lg`}>
                <Icon className="h-8 w-8 text-white" />
                <div className="absolute inset-0 rounded-2xl bg-white/20 backdrop-blur-sm" />
              </div>
              <div>
                <h3 className="text-xl font-bold text-slate-800 mb-1">{cluster.name}</h3>
                <Badge variant="secondary" className="bg-white/80 text-slate-600 border-0 rounded-full px-3 py-1">
                  {cluster.type}
                </Badge>
              </div>
            </div>

            {/* 状态指示器 */}
            <div className={`w-3 h-3 rounded-full ${isConfigured ? "bg-green-400" : "bg-orange-400"} shadow-lg`}>
              <div className={`w-3 h-3 rounded-full ${isConfigured ? "bg-green-400" : "bg-orange-400"} animate-ping`} />
            </div>
          </div>

          {/* 详细信息 */}
          <div className="space-y-3 mb-8">
            <div className="flex items-center text-slate-600">
              <Users className="h-4 w-4 mr-3 text-slate-400" />
              <span className="text-sm">管理员: {cluster.admin}</span>
            </div>
            <div className="flex items-center text-slate-600">
              <Calendar className="h-4 w-4 mr-3 text-slate-400" />
              <span className="text-sm">创建时间: {cluster.createdAt}</span>
            </div>
          </div>

          {/* 按钮组 */}
          <div className="space-y-3">
            {/* 进入集群按钮 - 占一行 */}
            <Button
              disabled={!isConfigured}
              className={`w-full h-12 rounded-2xl font-medium transition-all duration-300 ${
                isConfigured
                  ? `bg-gradient-to-r ${cluster.color} hover:shadow-lg hover:shadow-blue-200 text-white border-0`
                  : "bg-slate-100 text-slate-400 cursor-not-allowed border-0"
              }`}
            >
              <Play className="mr-2 h-4 w-4" />
              {isConfigured ? "进入集群" : "配置中..."}
            </Button>

            {/* 其他按钮 - 第二行 */}
            <div className="flex space-x-2">
              <Button
                variant="outline"
                size="sm"
                className="flex-1 h-10 rounded-xl border-slate-200 hover:bg-slate-50 transition-all duration-200 bg-transparent"
              >
                <Settings className="mr-1 h-3 w-3" />
                配置
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setAuthDialogOpen(true)}
                className="flex-1 h-10 rounded-xl border-slate-200 hover:bg-slate-50 transition-all duration-200 bg-transparent"
              >
                <Shield className="mr-1 h-3 w-3" />
                授权
              </Button>
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button
                    variant="outline"
                    size="sm"
                    className="h-10 w-10 rounded-xl border-slate-200 hover:bg-slate-50 transition-all duration-200 p-0 bg-transparent"
                  >
                    <MoreHorizontal className="h-4 w-4" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent className="w-48 rounded-2xl border-0 shadow-2xl bg-white/95 backdrop-blur-xl">
                  <DropdownMenuItem className="rounded-xl m-1 hover:bg-slate-50">
                    <Edit className="mr-2 h-4 w-4" />
                    编辑集群
                  </DropdownMenuItem>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem className="rounded-xl m-1 text-red-600 hover:bg-red-50">
                    <Trash2 className="mr-2 h-4 w-4" />
                    删除集群
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* 授权弹窗 */}
      <ClusterAuthorizationDialogEnhanced
        open={authDialogOpen}
        onOpenChange={setAuthDialogOpen}
        clusterName={cluster.name}
      />
    </>
  )
}

const CreateClusterCard = () => {
  const [createDialogOpen, setCreateDialogOpen] = useState(false)

  return (
    <>
      <Card
        className="group relative overflow-hidden rounded-3xl border-0 bg-gradient-to-br from-slate-50 to-white shadow-lg hover:shadow-2xl transition-all duration-500 hover:-translate-y-2 cursor-pointer"
        onClick={() => setCreateDialogOpen(true)}
      >
        {/* 动态背景效果 */}
        <div className="absolute inset-0 bg-gradient-to-br from-blue-500/5 via-purple-500/5 to-pink-500/5" />

        {/* 装饰性元素 */}
        <div className="absolute top-0 right-0 w-40 h-40 bg-gradient-to-br from-blue-400/10 to-purple-400/10 rounded-full blur-3xl transform translate-x-20 -translate-y-20 group-hover:scale-150 transition-transform duration-700" />
        <div className="absolute bottom-0 left-0 w-32 h-32 bg-gradient-to-tr from-pink-400/10 to-orange-400/10 rounded-full blur-2xl transform -translate-x-16 translate-y-16 group-hover:scale-125 transition-transform duration-700" />

        <CardContent className="relative p-8 h-full flex flex-col justify-center items-center text-center">
          {/* 主图标 */}
          <div className="relative mb-6">
            <div className="w-20 h-20 rounded-3xl bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center shadow-2xl group-hover:scale-110 transition-transform duration-300">
              <Plus className="h-10 w-10 text-white" />
            </div>
            <div className="absolute inset-0 rounded-3xl bg-gradient-to-br from-blue-500 to-purple-600 blur-xl opacity-30 group-hover:opacity-50 transition-opacity duration-300" />
          </div>

          {/* 标题 */}
          <h3 className="text-2xl font-bold bg-gradient-to-r from-slate-800 to-slate-600 bg-clip-text text-transparent mb-4">
            创建新集群
          </h3>

          {/* 特性列表 */}
          <div className="space-y-3 mb-8">
            <div className="flex items-center justify-center text-slate-600 group-hover:text-slate-800 transition-colors">
              <Rocket className="h-4 w-4 mr-2 text-blue-500" />
              <span className="text-sm font-medium">快速部署全新环境</span>
            </div>
            <div className="flex items-center justify-center text-slate-600 group-hover:text-slate-800 transition-colors">
              <Brain className="h-4 w-4 mr-2 text-purple-500" />
              <span className="text-sm font-medium">一键智能配置</span>
            </div>
            <div className="flex items-center justify-center text-slate-600 group-hover:text-slate-800 transition-colors">
              <Sparkles className="h-4 w-4 mr-2 text-pink-500" />
              <span className="text-sm font-medium">企业级安全保障</span>
            </div>
          </div>

          {/* 创建按钮 */}
          <Button className="w-full h-12 rounded-2xl bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700 text-white border-0 shadow-lg hover:shadow-xl transition-all duration-300 group-hover:scale-105">
            <Plus className="mr-2 h-4 w-4" />
            立即创建
            <ChevronRight className="ml-2 h-4 w-4 group-hover:translate-x-1 transition-transform" />
          </Button>

          {/* 底部提示 */}
          <p className="text-xs text-slate-400 mt-4 group-hover:text-slate-500 transition-colors">
            支持 Hadoop • Spark • Kubernetes • TensorFlow
          </p>
        </CardContent>
      </Card>

      {/* 创建集群弹窗 */}
      <CreateClusterDialogEnhanced open={createDialogOpen} onOpenChange={setCreateDialogOpen} />
    </>
  )
}

export default function ClusterListFinal() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-slate-50">
      {/* 页面头部 */}
      <div className="relative overflow-hidden bg-white border-b border-slate-200/50">
        <div className="absolute inset-0 bg-gradient-to-r from-blue-50/50 via-white to-purple-50/50" />
        <div className="relative max-w-7xl mx-auto px-8 py-12">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-4xl font-bold bg-gradient-to-r from-slate-800 to-slate-600 bg-clip-text text-transparent mb-2">
                集群管理
              </h1>
              <p className="text-slate-600 text-lg">管理和监控您的大数据集群环境</p>
            </div>
            <div className="flex items-center space-x-4">
              <Badge variant="outline" className="px-4 py-2 rounded-full border-green-200 text-green-700 bg-green-50">
                <div className="w-2 h-2 bg-green-400 rounded-full mr-2" />
                {clusters.filter((c) => c.status === "configured").length} 个集群运行中
              </Badge>
            </div>
          </div>
        </div>
      </div>

      {/* 集群列表 */}
      <div className="max-w-7xl mx-auto px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-8">
          {/* 现有集群卡片 */}
          {clusters.map((cluster) => (
            <ClusterCard key={cluster.id} cluster={cluster} />
          ))}

          {/* 创建新集群卡片 */}
          <CreateClusterCard />
        </div>
      </div>

      {/* 底部统计信息 */}
      <div className="max-w-7xl mx-auto px-8 pb-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          <div className="bg-white rounded-2xl p-6 shadow-lg border border-slate-100">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-slate-600 text-sm">总集群数</p>
                <p className="text-2xl font-bold text-slate-800">{clusters.length}</p>
              </div>
              <Server className="h-8 w-8 text-blue-500" />
            </div>
          </div>
          <div className="bg-white rounded-2xl p-6 shadow-lg border border-slate-100">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-slate-600 text-sm">运行中</p>
                <p className="text-2xl font-bold text-green-600">
                  {clusters.filter((c) => c.status === "configured").length}
                </p>
              </div>
              <div className="w-8 h-8 bg-green-100 rounded-full flex items-center justify-center">
                <div className="w-3 h-3 bg-green-500 rounded-full" />
              </div>
            </div>
          </div>
          <div className="bg-white rounded-2xl p-6 shadow-lg border border-slate-100">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-slate-600 text-sm">配置中</p>
                <p className="text-2xl font-bold text-orange-600">
                  {clusters.filter((c) => c.status === "unconfigured").length}
                </p>
              </div>
              <div className="w-8 h-8 bg-orange-100 rounded-full flex items-center justify-center">
                <div className="w-3 h-3 bg-orange-500 rounded-full animate-pulse" />
              </div>
            </div>
          </div>
          <div className="bg-white rounded-2xl p-6 shadow-lg border border-slate-100">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-slate-600 text-sm">集群类型</p>
                <p className="text-2xl font-bold text-purple-600">4</p>
              </div>
              <Cloud className="h-8 w-8 text-purple-500" />
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
