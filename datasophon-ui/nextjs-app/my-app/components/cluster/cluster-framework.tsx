"use client"

import { useState, useEffect } from "react"
import { Boxes, Trash2, Search, Package, CheckCircle, LoaderIcon } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent, CardHeader } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog"
import FinalNavbar from "../layout/navbar-final"
import { SvgIcon } from "@/components/ui/svg-icon"
import { API_PATHS, apiClient } from "@/lib/api-config"

// 定义数据类型
interface FrameworkService {
  id: number
  serviceName: string
  serviceVersion: string
  serviceDesc?: string
}

interface Framework {
  frameCode: string
  frameVersion?: string
  frameServiceList: FrameworkService[]
}

const ServiceCard = ({ service, onDelete }: { service: FrameworkService; onDelete: (id: number) => void }) => {
  return (
    <Card className="rounded-xl border border-slate-200 bg-white hover:shadow-md transition-all duration-200">
      <CardContent className="p-4">
        <div className="flex items-start justify-between">
          <div className="flex items-center space-x-3 flex-1 min-w-0">
            <SvgIcon 
              name={service.serviceName} 
              size={28} 
              className="shrink-0"
            />
            <div className="flex-1 min-w-0">
              <div className="flex items-center space-x-2 mb-1">
                <h3 className="font-semibold text-slate-800 truncate">{service.serviceName}</h3>
                <Badge variant="outline" className="text-xs font-mono shrink-0">
                  v{service.serviceVersion}
                </Badge>
              </div>
              <p className="text-sm text-slate-600 line-clamp-2">
                {service.serviceDesc || '暂无描述'}
              </p>
            </div>
          </div>
          
          <AlertDialog>
            <AlertDialogTrigger asChild>
              <Button
                variant="ghost"
                size="sm"
                className="h-8 w-8 p-0 text-red-600 hover:text-red-700 hover:bg-red-50 rounded-lg shrink-0 ml-2"
              >
                <Trash2 className="h-4 w-4" />
              </Button>
            </AlertDialogTrigger>
            <AlertDialogContent className="rounded-2xl border-0 shadow-2xl max-w-md">
              <AlertDialogHeader>
                <AlertDialogTitle className="flex items-center space-x-2">
                  <Trash2 className="h-5 w-5 text-red-500" />
                  <span>确认删除服务</span>
                </AlertDialogTitle>
                <AlertDialogDescription>
                  您确定要删除 <strong>{service.serviceName}</strong> v{service.serviceVersion} 吗？此操作不可撤销。
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel className="rounded-xl">取消</AlertDialogCancel>
                <AlertDialogAction
                  onClick={() => onDelete(service.id)}
                  className="rounded-xl bg-red-600 hover:bg-red-700"
                >
                  确认删除
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        </div>
      </CardContent>
    </Card>
  )
}

export default function ClusterFramework() {
  const [loading, setLoading] = useState(false)
  const [frameworks, setFrameworks] = useState<Framework[]>([])
  const [activeFrameCode, setActiveFrameCode] = useState<string>("")
  const [searchTerm, setSearchTerm] = useState("")

  // 获取框架列表
  const fetchFrameworks = async () => {
    setLoading(true)
    try {
      const response = await apiClient.post(API_PATHS.FRAME_LIST, {})
      if (response.data.code === 200) {
        const frameList = response.data.data || []
        setFrameworks(frameList)
        // 自动选择第一个框架
        if (frameList.length > 0 && !activeFrameCode) {
          setActiveFrameCode(frameList[0].frameCode)
        }
      }
    } catch (error) {
      console.error('获取框架列表失败:', error)
    } finally {
      setLoading(false)
    }
  }

  // 删除服务
  const handleDeleteService = async (serviceId: number) => {
    try {
      const response = await apiClient.get(`${API_PATHS.FRAME_SERVICE_DELETE}/${serviceId}`)
      if (response.data.code === 200) {
        // 重新获取框架列表
        await fetchFrameworks()
      }
    } catch (error) {
      console.error('删除服务失败:', error)
      alert('删除服务失败，请稍后再试')
    }
  }

  // 获取当前框架的服务列表
  const getCurrentFrameServices = () => {
    if (!activeFrameCode) return []
    const currentFrame = frameworks.find(frame => frame.frameCode === activeFrameCode)
    return currentFrame?.frameServiceList || []
  }

  // 过滤服务
  const filteredServices = getCurrentFrameServices().filter(
    (service) =>
      service.serviceName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (service.serviceDesc && service.serviceDesc.toLowerCase().includes(searchTerm.toLowerCase()))
  )

  // 统计数据
  const totalServices = getCurrentFrameServices().length

  useEffect(() => {
    fetchFrameworks()
  }, [])

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50/30 to-indigo-50/50 relative overflow-hidden">
      {/* 背景装饰 */}
      <div className="absolute top-0 left-0 w-96 h-96 bg-gradient-to-br from-purple-400/10 to-indigo-400/10 rounded-full blur-3xl transform -translate-x-48 -translate-y-48" />
      <div className="absolute bottom-0 right-0 w-80 h-80 bg-gradient-to-br from-blue-400/10 to-cyan-400/10 rounded-full blur-3xl transform translate-x-40 translate-y-40" />
      
      <FinalNavbar />

      {/* 页面头部 - 全宽布局 */}
      <div className="relative overflow-hidden bg-white/80 backdrop-blur-xl border-b border-slate-200/50 shadow-lg">
        <div className="absolute inset-0 bg-gradient-to-r from-purple-50/80 via-white/90 to-blue-50/80" />
        <div className="relative w-full px-8 py-12">
          <div className="flex items-center justify-between">
            <div className="space-y-2">
              <h1 className="text-4xl font-bold bg-gradient-to-r from-slate-800 via-slate-700 to-slate-600 bg-clip-text text-transparent">
                集群框架
              </h1>
              <p className="text-lg text-slate-600">管理和查看集群框架组件版本</p>
              <div className="flex items-center space-x-2 pt-2">
                <div className="w-2 h-2 bg-purple-400 rounded-full animate-pulse" />
                <span className="text-sm text-slate-500">框架管理 • 版本控制 • 服务配置</span>
              </div>
            </div>
            <div className="bg-white/90 backdrop-blur-sm rounded-3xl p-6 shadow-xl border border-white/50">
              <Badge className="px-6 py-3 rounded-2xl border-purple-200 text-purple-700 bg-purple-50/80 text-lg font-semibold">
                <CheckCircle className="h-5 w-5 mr-3 text-purple-600" />
                {totalServices} 个组件
              </Badge>
            </div>
          </div>
        </div>
      </div>

      {/* 主要内容 - 全宽布局 */}
      <div className="w-full px-8 py-8">
        {loading ? (
          <div className="flex items-center justify-center py-12">
            <LoaderIcon className="w-8 h-8 animate-spin text-blue-500" />
            <span className="ml-2 text-slate-600">加载中...</span>
          </div>
        ) : frameworks.length === 0 ? (
          <div className="text-center py-12">
            <Package className="w-12 h-12 text-slate-300 mx-auto mb-3" />
            <p className="text-slate-500">暂无集群框架</p>
          </div>
        ) : (
          <div className="space-y-5">
            {/* 框架选择和搜索 */}
            <div className="flex items-center justify-between gap-4">
              <Tabs value={activeFrameCode} onValueChange={setActiveFrameCode}>
                <TabsList className="grid grid-cols-2 rounded-xl bg-slate-100 p-1 h-10">
                  {frameworks.slice(0, 2).map((framework) => (
                    <TabsTrigger
                      key={framework.frameCode}
                      value={framework.frameCode}
                      className="rounded-lg data-[state=active]:bg-white data-[state=active]:shadow-sm text-sm"
                    >
                      {framework.frameCode}
                    </TabsTrigger>
                  ))}
                </TabsList>
              </Tabs>

              {/* 搜索框 */}
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-slate-400" />
                <Input
                  placeholder="搜索组件..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-10 w-64 h-10 rounded-xl border-slate-200 bg-white text-sm"
                />
              </div>
            </div>

            {/* 框架内容 */}
            <Card className="rounded-2xl border-0 shadow-md bg-white overflow-hidden">
              <CardHeader className="pb-4">
                <div className="flex items-center space-x-3">
                  <div className="p-2.5 rounded-xl bg-gradient-to-br from-blue-500 to-purple-600">
                    <Boxes className="h-5 w-5 text-white" />
                  </div>
                  <div>
                    <h2 className="text-lg font-bold text-slate-800">{activeFrameCode} 框架组件</h2>
                    <p className="text-sm text-slate-600">
                      {searchTerm ? `找到 ${filteredServices.length} 个匹配组件` : `包含 ${totalServices} 个组件`}
                    </p>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="pt-0">
                {filteredServices.length === 0 ? (
                  <div className="text-center py-12">
                    <Package className="w-10 h-10 text-slate-300 mx-auto mb-3" />
                    <p className="text-slate-500">
                      {searchTerm ? '未找到匹配的组件' : '该框架下暂无服务组件'}
                    </p>
                  </div>
                ) : (
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    {filteredServices.map((service) => (
                      <ServiceCard
                        key={service.id}
                        service={service}
                        onDelete={handleDeleteService}
                      />
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          </div>
        )}
      </div>
    </div>
  )
}
