"use client"

import { useState } from "react"
import { Boxes, Trash2, AlertCircle, CheckCircle, Package, Search } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
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
import EnhancedNavbar from "./noah-navbar-enhanced"

// 组件数据
const frameworkComponents = {
  "DDP-1.2.0": [
    {
      id: 1,
      service: "ALERTMANAGER",
      version: "0.23.0",
      description: "告警通知管理系统",
      logo: "🚨",
      status: "stable",
    },
    { id: 2, service: "ALLUXIO", version: "2.9.3", description: "分布式内存文件系统", logo: "💾", status: "stable" },
    {
      id: 3,
      service: "CLICKHOUSE",
      version: "23.9.1.1854",
      description: "联机分析(OLAP)列式数据库",
      logo: "🏠",
      status: "stable",
    },
    {
      id: 4,
      service: "DORIS",
      version: "1.2.6",
      description: "简单易用、高性能和统一的分析数据库",
      logo: "🎯",
      status: "stable",
    },
    {
      id: 5,
      service: "DS",
      version: "3.1.8",
      description: "分布式计算广展的可视化工作流任务调度平台",
      logo: "🔄",
      status: "beta",
    },
    { id: 6, service: "ELASTICSEARCH", version: "7.16.2", description: "高性能搜索引擎", logo: "🔍", status: "stable" },
    { id: 7, service: "FLINK", version: "1.16.2", description: "实时计算引擎", logo: "⚡", status: "stable" },
    {
      id: 8,
      service: "FLUME",
      version: "1.11.0",
      description: "分布式海量日志采集、聚合和传输的系统",
      logo: "🌊",
      status: "stable",
    },
    {
      id: 9,
      service: "GRAFANA",
      version: "9.1.6",
      description: "监控分析数据可视化套件",
      logo: "📊",
      status: "stable",
    },
    {
      id: 10,
      service: "HBASE",
      version: "2.2.7",
      description: "分布式列式海量存储数据库",
      logo: "🗄️",
      status: "stable",
    },
    { id: 11, service: "HDFS", version: "3.3.3", description: "分布式大数据存储", logo: "📁", status: "stable" },
    { id: 12, service: "HIVE", version: "3.1.0", description: "数据仓库软件", logo: "🐝", status: "stable" },
  ],
  "DDP-1.2.1": [
    {
      id: 1,
      service: "ALERTMANAGER",
      version: "0.24.0",
      description: "告警通知管理系统",
      logo: "🚨",
      status: "stable",
    },
    { id: 2, service: "ALLUXIO", version: "2.9.4", description: "分布式内存文件系统", logo: "💾", status: "stable" },
    {
      id: 3,
      service: "CLICKHOUSE",
      version: "23.10.1.1976",
      description: "联机分析(OLAP)列式数据库",
      logo: "🏠",
      status: "stable",
    },
    {
      id: 4,
      service: "DORIS",
      version: "1.2.7",
      description: "简单易用、高性能和统一的分析数据库",
      logo: "🎯",
      status: "stable",
    },
    {
      id: 5,
      service: "DS",
      version: "3.1.9",
      description: "分布式计算广展的可视化工作流任务调度平台",
      logo: "🔄",
      status: "stable",
    },
    { id: 6, service: "ELASTICSEARCH", version: "7.17.0", description: "高性能搜索引擎", logo: "🔍", status: "stable" },
    { id: 7, service: "FLINK", version: "1.17.0", description: "实时计算引擎", logo: "⚡", status: "stable" },
    {
      id: 8,
      service: "FLUME",
      version: "1.11.0",
      description: "分布式海量日志采集、聚合和传输的系统",
      logo: "🌊",
      status: "stable",
    },
    {
      id: 9,
      service: "GRAFANA",
      version: "9.2.0",
      description: "监控分析数据可视化套件",
      logo: "📊",
      status: "stable",
    },
    {
      id: 10,
      service: "HBASE",
      version: "2.4.0",
      description: "分布式列式海量存储数据库",
      logo: "🗄️",
      status: "stable",
    },
    { id: 11, service: "HDFS", version: "3.3.4", description: "分布式大数据存储", logo: "📁", status: "stable" },
    { id: 12, service: "HIVE", version: "3.1.2", description: "数据仓库软件", logo: "🐝", status: "stable" },
  ],
}

const ComponentRow = ({ component, onDelete }: { component: any; onDelete: (id: number) => void }) => {
  return (
    <TableRow className="hover:bg-slate-50/50 transition-colors group">
      <TableCell className="font-medium text-slate-700">{component.id}</TableCell>
      <TableCell>
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center text-lg shadow-lg">
            {component.logo}
          </div>
          <div>
            <p className="font-semibold text-slate-800">{component.service}</p>
            <Badge
              variant={component.status === "stable" ? "default" : "secondary"}
              className="mt-1 text-xs rounded-full"
            >
              {component.status === "stable" ? "稳定版" : "测试版"}
            </Badge>
          </div>
        </div>
      </TableCell>
      <TableCell>
        <Badge variant="outline" className="rounded-full font-mono text-xs">
          v{component.version}
        </Badge>
      </TableCell>
      <TableCell className="text-slate-600 max-w-xs">
        <p className="truncate">{component.description}</p>
      </TableCell>
      <TableCell>
        <AlertDialog>
          <AlertDialogTrigger asChild>
            <Button
              variant="ghost"
              size="sm"
              className="text-red-600 hover:text-red-700 hover:bg-red-50 rounded-xl opacity-0 group-hover:opacity-100 transition-all duration-200"
            >
              <Trash2 className="h-4 w-4" />
            </Button>
          </AlertDialogTrigger>
          <AlertDialogContent className="rounded-3xl border-0 shadow-2xl">
            <AlertDialogHeader>
              <AlertDialogTitle className="flex items-center space-x-2">
                <AlertCircle className="h-5 w-5 text-red-500" />
                <span>确认删除组件</span>
              </AlertDialogTitle>
              <AlertDialogDescription>
                您确定要删除 <strong>{component.service}</strong> v{component.version} 吗？此操作不可撤销。
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel className="rounded-2xl">取消</AlertDialogCancel>
              <AlertDialogAction
                onClick={() => onDelete(component.id)}
                className="rounded-2xl bg-red-600 hover:bg-red-700"
              >
                确认删除
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </TableCell>
    </TableRow>
  )
}

export default function ClusterFramework() {
  const [activeTab, setActiveTab] = useState("DDP-1.2.0")
  const [searchTerm, setSearchTerm] = useState("")
  const [components, setComponents] = useState(frameworkComponents)

  const handleDelete = (id: number) => {
    setComponents((prev) => ({
      ...prev,
      [activeTab]: prev[activeTab as keyof typeof prev].filter((comp) => comp.id !== id),
    }))
  }

  const filteredComponents = components[activeTab as keyof typeof components].filter(
    (comp) =>
      comp.service.toLowerCase().includes(searchTerm.toLowerCase()) ||
      comp.description.toLowerCase().includes(searchTerm.toLowerCase()),
  )

  const stableCount = components[activeTab as keyof typeof components].filter((comp) => comp.status === "stable").length
  const betaCount = components[activeTab as keyof typeof components].filter((comp) => comp.status === "beta").length

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-slate-50">
      <EnhancedNavbar />

      {/* 页面头部 */}
      <div className="relative overflow-hidden bg-white border-b border-slate-200/50">
        <div className="absolute inset-0 bg-gradient-to-r from-purple-50/50 via-white to-blue-50/50" />
        <div className="relative max-w-7xl mx-auto px-8 py-12">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-4xl font-bold bg-gradient-to-r from-slate-800 to-slate-600 bg-clip-text text-transparent mb-2">
                集群框架
              </h1>
              <p className="text-slate-600 text-lg">管理和查看集群框架组件版本</p>
            </div>
            <div className="flex items-center space-x-4">
              <Badge variant="outline" className="px-4 py-2 rounded-full border-green-200 text-green-700 bg-green-50">
                <CheckCircle className="w-4 h-4 mr-2" />
                {stableCount} 个稳定组件
              </Badge>
              {betaCount > 0 && (
                <Badge
                  variant="outline"
                  className="px-4 py-2 rounded-full border-orange-200 text-orange-700 bg-orange-50"
                >
                  <Package className="w-4 h-4 mr-2" />
                  {betaCount} 个测试组件
                </Badge>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* 主要内容 */}
      <div className="max-w-7xl mx-auto px-8 py-12">
        <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-8">
          <div className="flex items-center justify-between">
            <TabsList className="grid grid-cols-2 rounded-2xl bg-slate-100 p-1">
              <TabsTrigger
                value="DDP-1.2.0"
                className="rounded-xl data-[state=active]:bg-white data-[state=active]:shadow-sm"
              >
                DDP-1.2.0
              </TabsTrigger>
              <TabsTrigger
                value="DDP-1.2.1"
                className="rounded-xl data-[state=active]:bg-white data-[state=active]:shadow-sm"
              >
                DDP-1.2.1
              </TabsTrigger>
            </TabsList>

            {/* 搜索框 */}
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-slate-400" />
              <Input
                placeholder="搜索组件..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10 w-80 rounded-2xl border-slate-200 bg-white"
              />
            </div>
          </div>

          <TabsContent value="DDP-1.2.0" className="space-y-6">
            <Card className="rounded-3xl border-0 shadow-lg bg-white overflow-hidden">
              <CardContent className="p-0">
                <div className="p-6 bg-gradient-to-r from-blue-50 to-purple-50 border-b border-slate-100">
                  <div className="flex items-center space-x-3">
                    <div className="p-3 rounded-2xl bg-gradient-to-br from-blue-500 to-purple-600">
                      <Boxes className="h-6 w-6 text-white" />
                    </div>
                    <div>
                      <h2 className="text-xl font-bold text-slate-800">DDP-1.2.0 框架组件</h2>
                      <p className="text-slate-600">当前版本包含 {filteredComponents.length} 个组件</p>
                    </div>
                  </div>
                </div>

                <Table>
                  <TableHeader>
                    <TableRow className="border-slate-100">
                      <TableHead className="font-semibold text-slate-700">序号</TableHead>
                      <TableHead className="font-semibold text-slate-700">服务</TableHead>
                      <TableHead className="font-semibold text-slate-700">版本</TableHead>
                      <TableHead className="font-semibold text-slate-700">描述</TableHead>
                      <TableHead className="font-semibold text-slate-700">操作</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {filteredComponents.map((component) => (
                      <ComponentRow key={component.id} component={component} onDelete={handleDelete} />
                    ))}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="DDP-1.2.1" className="space-y-6">
            <Card className="rounded-3xl border-0 shadow-lg bg-white overflow-hidden">
              <CardContent className="p-0">
                <div className="p-6 bg-gradient-to-r from-green-50 to-blue-50 border-b border-slate-100">
                  <div className="flex items-center space-x-3">
                    <div className="p-3 rounded-2xl bg-gradient-to-br from-green-500 to-blue-600">
                      <Boxes className="h-6 w-6 text-white" />
                    </div>
                    <div>
                      <h2 className="text-xl font-bold text-slate-800">DDP-1.2.1 框架组件</h2>
                      <p className="text-slate-600">最新版本包含 {filteredComponents.length} 个组件</p>
                    </div>
                  </div>
                </div>

                <Table>
                  <TableHeader>
                    <TableRow className="border-slate-100">
                      <TableHead className="font-semibold text-slate-700">序号</TableHead>
                      <TableHead className="font-semibold text-slate-700">服务</TableHead>
                      <TableHead className="font-semibold text-slate-700">版本</TableHead>
                      <TableHead className="font-semibold text-slate-700">描述</TableHead>
                      <TableHead className="font-semibold text-slate-700">操作</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {filteredComponents.map((component) => (
                      <ComponentRow key={component.id} component={component} onDelete={handleDelete} />
                    ))}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>
    </div>
  )
}
