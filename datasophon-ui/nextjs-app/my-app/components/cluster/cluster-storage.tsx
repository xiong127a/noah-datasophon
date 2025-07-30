"use client"

import { useState, useEffect } from "react"
import { HardDrive, Plus, Search, Trash2, Download, Package, AlertCircle, CheckCircle, LoaderIcon } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent, CardHeader } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Progress } from "@/components/ui/progress"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import EnhancedNavbar from "../layout/navbar-enhanced"
import { API_PATHS, apiClient } from "@/lib/api-config"

// 定义数据类型
interface Component {
  name: string
  label: string
  version: string
  description?: string
  state?: 'executing' | 'success' | 'fail'
  step?: 'download' | 'install'
  process?: number
  md5?: string
}

interface Parcel {
  parcelId: number
  parcelName: string
  parcelPath: string
  parcelFit: number
  frame: string
  components: Component[]
}

export default function ClusterStorage() {
  const [loading, setLoading] = useState(false)
  const [parcelList, setParcelList] = useState<Parcel[]>([])
  const [ddhParcelPath] = useState("file:///opt/datasophon/DDP/packages")
  const [addingRepo, setAddingRepo] = useState(false)

  // 获取存储库列表
  const fetchParcelList = async () => {
    setLoading(true)
    try {
      const response = await apiClient.post(API_PATHS.PARCEL_LIST, {})
      if (response.data.code === 200) {
        setParcelList(response.data.data || [])
      }
    } catch (error) {
      console.error('获取存储库列表失败:', error)
    } finally {
      setLoading(false)
    }
  }

  // 添加新存储库
  const addNewRepo = () => {
    const newRepo: Parcel = {
      parcelId: Date.now(),
      parcelName: `第三方存储库 ${parcelList.length + 1}`,
      parcelPath: "",
      parcelFit: 1,
      frame: "DDP-1.2.1",
      components: []
    }
    setParcelList([...parcelList, newRepo])
  }

  // 删除存储库
  const removeRepo = (parcel: Parcel) => {
    if (window.confirm(`确定要删除存储库 "${parcel.parcelName}" 吗？`)) {
      setParcelList(parcelList.filter(p => p.parcelId !== parcel.parcelId))
    }
  }

  // 解析存储库URL
  const parseParcelUrl = async (parcel: Parcel) => {
    if (!parcel.parcelPath.trim()) {
      alert('请输入存储库URL地址')
      return
    }

    setAddingRepo(true)
    try {
      const response = await apiClient.post(API_PATHS.PARCEL_PARSE, {
        url: parcel.parcelPath
      })
      if (response.data.code === 200) {
        // 更新对应存储库的组件列表
        setParcelList(prevList => 
          prevList.map(p => 
            p.parcelId === parcel.parcelId 
              ? { ...p, components: response.data.data.components || [] }
              : p
          )
        )
      }
    } catch (error) {
      console.error('解析存储库失败:', error)
      alert('解析存储库失败，请检查URL是否正确')
    } finally {
      setAddingRepo(false)
    }
  }

  // 下载组件
  const handleDownload = async (comp: Component, url: string) => {
    try {
      const response = await apiClient.post(API_PATHS.PARCEL_DOWNLOAD, {
        url: url,
        parcelName: comp.name
      })
      if (response.data.code === 200) {
        // 更新组件状态
        updateComponentState(comp.name, {
          md5: response.data.data.md5,
          process: response.data.data.process * 100,
          state: response.data.data.state,
          step: response.data.data.step
        })
      }
    } catch (error) {
      console.error('下载组件失败:', error)
    }
  }

  // 安装组件
  const handleInstall = async (comp: Component) => {
    try {
      const response = await apiClient.post(API_PATHS.PARCEL_INSTALL, {
        md5: comp.md5,
        packageName: comp.name
      })
      if (response.data.code === 200) {
        // 更新组件状态
        updateComponentState(comp.name, {
          process: response.data.data.process * 100,
          state: response.data.data.state,
          step: response.data.data.step
        })
      }
    } catch (error) {
      console.error('安装组件失败:', error)
    }
  }

  // 更新组件状态
  const updateComponentState = (componentName: string, updates: Partial<Component>) => {
    setParcelList(prevList =>
      prevList.map(parcel => ({
        ...parcel,
        components: parcel.components.map(comp =>
          comp.name === componentName ? { ...comp, ...updates } : comp
        )
      }))
    )
  }

  // 格式化进度状态
  const formatState = (percent: number, comp: Component) => {
    if (comp.step === 'download') {
      if (comp.state === 'executing') return `正在下载：${percent}%`
      if (comp.state === 'success') return "下载成功"
      return "下载失败"
    } else if (comp.step === 'install') {
      if (comp.state === 'executing') return `正在安装：${percent}%`
      if (comp.state === 'success') return "安装成功"
      return "安装失败"
    }
    return `${percent}%`
  }

  useEffect(() => {
    fetchParcelList()
  }, [])

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-slate-50">
      <EnhancedNavbar />

      {/* 页面头部 - 减少垂直空间 */}
      <div className="relative overflow-hidden bg-white border-b border-slate-200/50">
        <div className="absolute inset-0 bg-gradient-to-r from-green-50/50 via-white to-blue-50/50" />
        <div className="relative max-w-7xl mx-auto px-6 py-6">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold bg-gradient-to-r from-slate-800 to-slate-600 bg-clip-text text-transparent mb-1">
                集群存储库
              </h1>
              <p className="text-slate-600">管理和配置存储库，查看可用的组件包</p>
            </div>
            <Badge variant="outline" className="px-3 py-1.5 rounded-full border-green-200 text-green-700 bg-green-50">
              <HardDrive className="w-4 h-4 mr-1.5" />
              {parcelList.length + 1} 个存储库
            </Badge>
          </div>
        </div>
      </div>

      {/* 主要内容 - 减少垂直间距 */}
      <div className="max-w-7xl mx-auto px-6 py-6">
        {loading ? (
          <div className="flex items-center justify-center py-12">
            <LoaderIcon className="w-8 h-8 animate-spin text-blue-500" />
            <span className="ml-2 text-slate-600">加载中...</span>
          </div>
        ) : (
          <div className="space-y-5">
            {/* 内置存储库 - 紧凑布局 */}
            <Card className="rounded-2xl border-0 shadow-md bg-white">
              <CardHeader className="pb-3">
                <div className="flex items-center space-x-3">
                  <div className="p-2.5 rounded-xl bg-gradient-to-br from-blue-500 to-cyan-500">
                    <HardDrive className="h-5 w-5 text-white" />
                  </div>
                  <div>
                    <h2 className="text-lg font-bold text-slate-800">内置存储库</h2>
                    <p className="text-sm text-slate-600">系统默认组件存储库</p>
                  </div>
                  <Badge variant="secondary" className="ml-auto rounded-full px-2.5 py-1 text-xs">
                    系统默认
                  </Badge>
                </div>
              </CardHeader>
              <CardContent className="pt-0">
                <div className="p-3 bg-slate-50 rounded-xl">
                  <div className="flex items-center space-x-3">
                    <div className="w-2 h-2 bg-green-400 rounded-full"></div>
                    <span className="text-sm text-slate-600 font-mono flex-1">{ddhParcelPath}</span>
                    <span className="text-xs text-green-600 font-medium">已连接</span>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* 第三方存储库 - 紧凑布局 */}
            <Card className="rounded-2xl border-0 shadow-md bg-white">
              <CardHeader className="pb-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-3">
                    <div className="p-2.5 rounded-xl bg-gradient-to-br from-purple-500 to-pink-500">
                      <Package className="h-5 w-5 text-white" />
                    </div>
                    <div>
                      <h2 className="text-lg font-bold text-slate-800">第三方存储库</h2>
                      <p className="text-sm text-slate-600">管理外部组件存储库</p>
                    </div>
                  </div>
                  <Button
                    onClick={addNewRepo}
                    size="sm"
                    className="rounded-xl bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700 text-white border-0"
                  >
                    <Plus className="mr-1.5 h-4 w-4" />
                    添加存储库
                  </Button>
                </div>
              </CardHeader>
              <CardContent className="pt-0">
                {parcelList.length === 0 ? (
                  <div className="text-center py-12">
                    <Package className="w-10 h-10 text-slate-300 mx-auto mb-3" />
                    <p className="text-slate-500 mb-3">暂无第三方存储库</p>
                    <Button
                      onClick={addNewRepo}
                      size="sm"
                      className="rounded-xl bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700 text-white border-0"
                    >
                      <Plus className="mr-1.5 h-4 w-4" />
                      添加第一个存储库
                    </Button>
                  </div>
                ) : (
                  <div className="space-y-4">
                    {parcelList.map((parcel) => (
                      <Card key={parcel.parcelId} className="rounded-xl border border-slate-200 bg-slate-50/50">
                        <CardHeader className="pb-3">
                          <div className="flex items-center justify-between">
                            <div className="flex items-center space-x-2.5">
                              <Package className="w-4 h-4 text-slate-600" />
                              <h3 className="font-semibold text-slate-800">{parcel.parcelName}</h3>
                            </div>
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => removeRepo(parcel)}
                              className="h-8 w-8 p-0 rounded-lg border-red-200 text-red-600 hover:bg-red-50"
                            >
                              <Trash2 className="h-3.5 w-3.5" />
                            </Button>
                          </div>
                        </CardHeader>
                        <CardContent className="pt-0 space-y-3">
                          {/* URL输入 - 紧凑布局 */}
                          <div className="flex space-x-2">
                            <Input
                              value={parcel.parcelPath}
                              onChange={(e) => {
                                const newPath = e.target.value
                                setParcelList(prevList =>
                                  prevList.map(p =>
                                    p.parcelId === parcel.parcelId
                                      ? { ...p, parcelPath: newPath }
                                      : p
                                  )
                                )
                              }}
                              placeholder="请输入存储库URL地址"
                              className="h-9 rounded-lg border-slate-200 bg-white text-sm"
                            />
                            <Button
                              onClick={() => parseParcelUrl(parcel)}
                              disabled={addingRepo}
                              size="sm"
                              className="h-9 w-9 p-0 rounded-lg bg-blue-500 hover:bg-blue-600 text-white"
                            >
                              {addingRepo ? (
                                <LoaderIcon className="h-4 w-4 animate-spin" />
                              ) : (
                                <Search className="h-4 w-4" />
                              )}
                            </Button>
                          </div>

                          {/* 组件列表 - 优化密度 */}
                          {parcel.components.length > 0 && (
                            <div className="space-y-2">
                              <h4 className="text-sm font-semibold text-slate-700 border-l-3 border-blue-500 pl-2">
                                可用组件 ({parcel.components.length})
                              </h4>
                              <div className="grid gap-2">
                                {parcel.components.map((comp) => (
                                  <div key={comp.name} className="bg-white rounded-lg p-3 border border-slate-200">
                                    <div className="flex items-start justify-between gap-3">
                                      <div className="flex-1 min-w-0">
                                        <div className="flex items-center space-x-2 mb-1">
                                          <h5 className="font-medium text-slate-800 truncate">{comp.label}</h5>
                                          <Badge variant="secondary" className="text-xs shrink-0">
                                            {comp.version}
                                          </Badge>
                                        </div>
                                        <p className="text-sm text-slate-600 line-clamp-2">{comp.description || '暂无描述'}</p>
                                        
                                        {/* 进度条 - 更紧凑 */}
                                        {comp.state !== undefined && (
                                          <div className="mt-2">
                                            <Progress value={comp.process || 0} className="h-1.5" />
                                            <p className="text-xs text-slate-500 mt-1">
                                              {formatState(comp.process || 0, comp)}
                                            </p>
                                          </div>
                                        )}
                                      </div>
                                      
                                      {/* 操作按钮 - 紧凑布局 */}
                                      <div className="flex items-center shrink-0">
                                        {comp.state === undefined ? (
                                          <Button
                                            size="sm"
                                            onClick={() => handleDownload(comp, parcel.parcelPath)}
                                            className="h-8 px-3 rounded-lg bg-blue-500 hover:bg-blue-600 text-white text-xs"
                                          >
                                            <Download className="w-3.5 h-3.5 mr-1" />
                                            下载
                                          </Button>
                                        ) : comp.state === 'success' && comp.step === 'download' ? (
                                          <Button
                                            size="sm"
                                            onClick={() => handleInstall(comp)}
                                            disabled={comp.state === 'executing' && comp.step === 'install'}
                                            className="h-8 px-3 rounded-lg bg-green-500 hover:bg-green-600 text-white text-xs"
                                          >
                                            {comp.state === 'executing' && comp.step === 'install' ? (
                                              <LoaderIcon className="w-3.5 h-3.5 mr-1 animate-spin" />
                                            ) : (
                                              <Package className="w-3.5 h-3.5 mr-1" />
                                            )}
                                            安装
                                          </Button>
                                        ) : comp.state === 'success' && comp.step === 'install' ? (
                                          <div className="flex items-center space-x-1.5 px-2.5 py-1 bg-green-100 rounded-lg">
                                            <CheckCircle className="w-3.5 h-3.5 text-green-600" />
                                            <span className="text-xs text-green-700">已安装</span>
                                          </div>
                                        ) : null}
                                      </div>
                                    </div>
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}
                        </CardContent>
                      </Card>
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
