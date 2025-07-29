"use client"

import { useState } from "react"
import { HardDrive, Plus, Search, Settings, Trash2, ExternalLink, Database, Cloud, Server } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import EnhancedNavbar from "./noah-navbar-enhanced"

const storageRepositories = [
  {
    id: 1,
    name: "官方存储库",
    type: "official",
    url: "file://opt/datasophon/DDP/packages",
    packages: 156,
    size: "2.3 GB",
    status: "active",
    description: "Noah平台官方组件存储库",
    icon: Database,
    color: "from-blue-500 to-cyan-500",
  },
  {
    id: 2,
    name: "Apache镜像库",
    type: "mirror",
    url: "https://mirrors.apache.org/hadoop",
    packages: 89,
    size: "1.8 GB",
    status: "active",
    description: "Apache Hadoop生态组件镜像",
    icon: Cloud,
    color: "from-orange-500 to-red-500",
  },
  {
    id: 3,
    name: "企业私有库",
    type: "private",
    url: "https://repo.company.com/bigdata",
    packages: 34,
    size: "856 MB",
    status: "syncing",
    description: "企业内部定制化组件库",
    icon: Server,
    color: "from-purple-500 to-pink-500",
  },
]

const RepositoryCard = ({ repo }: { repo: any }) => {
  const Icon = repo.icon
  const isActive = repo.status === "active"

  return (
    <Card className="group relative overflow-hidden rounded-3xl border-0 bg-white shadow-lg hover:shadow-2xl transition-all duration-500 hover:-translate-y-1">
      {/* 背景渐变 */}
      <div className={`absolute inset-0 bg-gradient-to-br ${repo.color} opacity-5`} />

      {/* 装饰性光效 */}
      <div className="absolute top-0 right-0 w-24 h-24 bg-gradient-to-br from-white/10 to-transparent rounded-full blur-2xl transform translate-x-12 -translate-y-12 group-hover:scale-150 transition-transform duration-700" />

      <CardContent className="relative p-6">
        {/* 头部 */}
        <div className="flex items-start justify-between mb-4">
          <div className="flex items-center space-x-3">
            <div className={`p-3 rounded-2xl bg-gradient-to-br ${repo.color} shadow-lg`}>
              <Icon className="h-6 w-6 text-white" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-slate-800">{repo.name}</h3>
              <Badge
                variant={repo.type === "official" ? "default" : "secondary"}
                className="mt-1 rounded-full px-2 py-1 text-xs"
              >
                {repo.type === "official" ? "官方" : repo.type === "mirror" ? "镜像" : "私有"}
              </Badge>
            </div>
          </div>

          {/* 状态指示器 */}
          <div className="flex items-center space-x-2">
            <div
              className={`w-2 h-2 rounded-full ${
                isActive ? "bg-green-400" : "bg-orange-400"
              } ${!isActive && "animate-pulse"}`}
            />
            <span className="text-xs text-slate-500">{isActive ? "活跃" : "同步中"}</span>
          </div>
        </div>

        {/* URL */}
        <div className="mb-4 p-3 bg-slate-50 rounded-2xl">
          <p className="text-sm text-slate-600 font-mono break-all">{repo.url}</p>
        </div>

        {/* 描述 */}
        <p className="text-sm text-slate-600 mb-4">{repo.description}</p>

        {/* 统计信息 */}
        <div className="grid grid-cols-2 gap-4 mb-4">
          <div className="text-center p-3 bg-slate-50 rounded-2xl">
            <p className="text-lg font-bold text-slate-800">{repo.packages}</p>
            <p className="text-xs text-slate-500">组件包</p>
          </div>
          <div className="text-center p-3 bg-slate-50 rounded-2xl">
            <p className="text-lg font-bold text-slate-800">{repo.size}</p>
            <p className="text-xs text-slate-500">存储大小</p>
          </div>
        </div>

        {/* 操作按钮 */}
        <div className="flex space-x-2">
          <Button
            size="sm"
            className="flex-1 rounded-xl bg-gradient-to-r from-blue-500 to-cyan-500 hover:from-blue-600 hover:to-cyan-600 text-white border-0"
          >
            <ExternalLink className="mr-1 h-3 w-3" />
            访问
          </Button>
          <Button variant="outline" size="sm" className="rounded-xl border-slate-200 hover:bg-slate-50 bg-transparent">
            <Settings className="h-3 w-3" />
          </Button>
          <Button
            variant="outline"
            size="sm"
            className="rounded-xl border-slate-200 hover:bg-red-50 hover:text-red-600 bg-transparent"
          >
            <Trash2 className="h-3 w-3" />
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}

const AddRepositoryCard = () => {
  return (
    <Card className="group relative overflow-hidden rounded-3xl border-2 border-dashed border-slate-200 bg-slate-50/50 hover:border-blue-300 hover:bg-blue-50/50 transition-all duration-300 cursor-pointer">
      <CardContent className="p-8 h-full flex flex-col justify-center items-center text-center">
        <div className="w-16 h-16 rounded-3xl bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center shadow-lg mb-4 group-hover:scale-110 transition-transform duration-300">
          <Plus className="h-8 w-8 text-white" />
        </div>
        <h3 className="text-lg font-bold text-slate-700 mb-2">添加存储库</h3>
        <p className="text-sm text-slate-500 mb-4">连接新的组件存储库</p>
        <Button className="rounded-2xl bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700 text-white border-0">
          <Plus className="mr-2 h-4 w-4" />
          立即添加
        </Button>
      </CardContent>
    </Card>
  )
}

export default function ClusterStorage() {
  const [builtInPath, setBuiltInPath] = useState("file://opt/datasophon/DDP/packages")
  const [thirdPartyUrl, setThirdPartyUrl] = useState("")

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-slate-50">
      <EnhancedNavbar />

      {/* 页面头部 */}
      <div className="relative overflow-hidden bg-white border-b border-slate-200/50">
        <div className="absolute inset-0 bg-gradient-to-r from-green-50/50 via-white to-blue-50/50" />
        <div className="relative max-w-7xl mx-auto px-8 py-12">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-4xl font-bold bg-gradient-to-r from-slate-800 to-slate-600 bg-clip-text text-transparent mb-2">
                集群存储库
              </h1>
              <p className="text-slate-600 text-lg">管理和配置集群组件存储库</p>
            </div>
            <div className="flex items-center space-x-4">
              <Badge variant="outline" className="px-4 py-2 rounded-full border-green-200 text-green-700 bg-green-50">
                <HardDrive className="w-4 h-4 mr-2" />
                {storageRepositories.length} 个存储库
              </Badge>
            </div>
          </div>
        </div>
      </div>

      {/* 主要内容 */}
      <div className="max-w-7xl mx-auto px-8 py-12">
        <Tabs defaultValue="management" className="space-y-8">
          <TabsList className="grid w-full max-w-md grid-cols-2 rounded-2xl bg-slate-100 p-1">
            <TabsTrigger
              value="management"
              className="rounded-xl data-[state=active]:bg-white data-[state=active]:shadow-sm"
            >
              集群管理
            </TabsTrigger>
            <TabsTrigger
              value="storage"
              className="rounded-xl data-[state=active]:bg-white data-[state=active]:shadow-sm"
            >
              存储库管理
            </TabsTrigger>
          </TabsList>

          <TabsContent value="management" className="space-y-8">
            {/* 内置存储库配置 */}
            <Card className="rounded-3xl border-0 shadow-lg bg-white">
              <CardContent className="p-8">
                <div className="flex items-center space-x-3 mb-6">
                  <div className="p-3 rounded-2xl bg-gradient-to-br from-blue-500 to-cyan-500">
                    <Database className="h-6 w-6 text-white" />
                  </div>
                  <div>
                    <h2 className="text-xl font-bold text-slate-800">内置存储库</h2>
                    <p className="text-slate-600">配置系统内置组件存储路径</p>
                  </div>
                </div>

                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-2">存储路径:</label>
                    <Input
                      value={builtInPath}
                      onChange={(e) => setBuiltInPath(e.target.value)}
                      className="rounded-2xl border-slate-200 bg-slate-50 h-12"
                      placeholder="请输入存储路径"
                    />
                  </div>
                  <Button className="rounded-2xl bg-gradient-to-r from-blue-500 to-cyan-500 hover:from-blue-600 hover:to-cyan-600 text-white border-0">
                    <Settings className="mr-2 h-4 w-4" />
                    更新配置
                  </Button>
                </div>
              </CardContent>
            </Card>

            {/* 添加第三方存储库 */}
            <Card className="rounded-3xl border-0 shadow-lg bg-white">
              <CardContent className="p-8">
                <div className="flex items-center space-x-3 mb-6">
                  <div className="p-3 rounded-2xl bg-gradient-to-br from-purple-500 to-pink-500">
                    <Cloud className="h-6 w-6 text-white" />
                  </div>
                  <div>
                    <h2 className="text-xl font-bold text-slate-800">添加第三方存储库</h2>
                    <p className="text-slate-600">连接外部组件存储库扩展功能</p>
                  </div>
                </div>

                <div className="flex space-x-4">
                  <Input
                    value={thirdPartyUrl}
                    onChange={(e) => setThirdPartyUrl(e.target.value)}
                    className="flex-1 rounded-2xl border-slate-200 bg-slate-50 h-12"
                    placeholder="请输入第三方存储库URL"
                  />
                  <Button className="rounded-2xl bg-gradient-to-r from-purple-500 to-pink-500 hover:from-purple-600 hover:to-pink-600 text-white border-0 px-8">
                    <Search className="mr-2 h-4 w-4" />
                    搜索
                  </Button>
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="storage" className="space-y-8">
            {/* 存储库列表 */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {storageRepositories.map((repo) => (
                <RepositoryCard key={repo.id} repo={repo} />
              ))}
              <AddRepositoryCard />
            </div>
          </TabsContent>
        </Tabs>
      </div>
    </div>
  )
}
