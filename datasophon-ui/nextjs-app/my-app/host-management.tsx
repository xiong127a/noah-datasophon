"use client"

import { useState } from "react"
import { Server, Search, Plus, Settings, Cpu, Activity, CheckCircle } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Progress } from "@/components/ui/progress"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import FinalNavbar from "./noah-navbar-final"

// 主机数据
const hosts = [
  {
    id: 1,
    status: "正常",
    hostname: "bigdata03",
    ip: "192.168.30.18",
    cores: 26,
    memoryUsed: 49,
    memoryTotal: 188,
    diskUsed: 105,
    diskTotal: 176,
    avgLoad: 2.29,
    tags: "default",
    rack: "/default-rack",
    arch: "x86_64",
    roles: 26,
  },
  {
    id: 2,
    status: "正常",
    hostname: "bigdata04",
    ip: "192.168.1.54",
    cores: 2,
    memoryUsed: 9,
    memoryTotal: 31,
    diskUsed: 16,
    diskTotal: 19,
    avgLoad: 0.2,
    tags: "default",
    rack: "/default-rack",
    arch: "x86_64",
    roles: 2,
  },
  {
    id: 3,
    status: "正常",
    hostname: "bigdata02",
    ip: "192.168.30.15",
    cores: 17,
    memoryUsed: 30,
    memoryTotal: 125,
    diskUsed: 49,
    diskTotal: 88,
    avgLoad: 1.4,
    tags: "default",
    rack: "/default-rack",
    arch: "x86_64",
    roles: 17,
  },
  {
    id: 4,
    status: "正常",
    hostname: "bigdata06",
    ip: "192.168.1.56",
    cores: 2,
    memoryUsed: 7,
    memoryTotal: 31,
    diskUsed: 13,
    diskTotal: 19,
    avgLoad: 0.41,
    tags: "default",
    rack: "/default-rack",
    arch: "x86_64",
    roles: 2,
  },
  {
    id: 5,
    status: "正常",
    hostname: "bigdata05",
    ip: "192.168.1.55",
    cores: 2,
    memoryUsed: 8,
    memoryTotal: 31,
    diskUsed: 15,
    diskTotal: 19,
    avgLoad: 0.52,
    tags: "default",
    rack: "/default-rack",
    arch: "x86_64",
    roles: 2,
  },
  {
    id: 6,
    status: "正常",
    hostname: "bigdata01",
    ip: "192.168.30.17",
    cores: 17,
    memoryUsed: 62,
    memoryTotal: 125,
    diskUsed: 121,
    diskTotal: 162,
    avgLoad: 1.11,
    tags: "default",
    rack: "/default-rack",
    arch: "x86_64",
    roles: 17,
  },
]

const HostRow = ({ host }: { host: any }) => {
  const memoryPercent = (host.memoryUsed / host.memoryTotal) * 100
  const diskPercent = (host.diskUsed / host.diskTotal) * 100

  const getUsageColor = (percent: number) => {
    if (percent < 50) return "bg-green-500"
    if (percent < 80) return "bg-orange-500"
    return "bg-red-500"
  }

  return (
    <TableRow className="hover:bg-slate-50/50 transition-colors group">
      <TableCell>
        <input type="checkbox" className="rounded border-slate-300" />
      </TableCell>
      <TableCell className="font-medium text-slate-700">{host.id}</TableCell>
      <TableCell>
        <div className="flex items-center space-x-2">
          <CheckCircle className="h-4 w-4 text-green-500" />
          <span className="text-green-600 font-medium">{host.status}</span>
        </div>
      </TableCell>
      <TableCell>
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-blue-500 to-cyan-500 flex items-center justify-center shadow-lg">
            <Server className="h-5 w-5 text-white" />
          </div>
          <span className="font-semibold text-slate-800">{host.hostname}</span>
        </div>
      </TableCell>
      <TableCell className="font-mono text-slate-600">{host.ip}</TableCell>
      <TableCell>
        <div className="flex items-center space-x-2">
          <Cpu className="h-4 w-4 text-blue-500" />
          <span className="font-medium">{host.cores}</span>
        </div>
      </TableCell>
      <TableCell>
        <div className="space-y-1">
          <div className="flex justify-between text-xs">
            <span>
              {host.memoryUsed}GB/{host.memoryTotal}GB
            </span>
            <span>{memoryPercent.toFixed(0)}%</span>
          </div>
          <Progress value={memoryPercent} className="h-2" />
        </div>
      </TableCell>
      <TableCell>
        <div className="space-y-1">
          <div className="flex justify-between text-xs">
            <span>
              {host.diskUsed}GB/{host.diskTotal}GB
            </span>
            <span>{diskPercent.toFixed(0)}%</span>
          </div>
          <Progress value={diskPercent} className="h-2" />
        </div>
      </TableCell>
      <TableCell>
        <div className="flex items-center space-x-2">
          <Activity className="h-4 w-4 text-orange-500" />
          <span className="font-medium">{host.avgLoad}</span>
        </div>
      </TableCell>
      <TableCell>
        <Badge variant="outline" className="rounded-full text-xs">
          {host.tags}
        </Badge>
      </TableCell>
      <TableCell className="text-slate-600 text-sm">{host.rack}</TableCell>
      <TableCell>
        <Badge variant="secondary" className="rounded-full text-xs">
          {host.arch}
        </Badge>
      </TableCell>
      <TableCell>
        <Badge variant="default" className="rounded-full text-xs bg-blue-100 text-blue-700">
          {host.roles}
        </Badge>
      </TableCell>
    </TableRow>
  )
}

export default function HostManagement() {
  const [searchTerm, setSearchTerm] = useState("")
  const [cpuFilter, setCpuFilter] = useState("")
  const [statusFilter, setStatusFilter] = useState("")

  const filteredHosts = hosts.filter(
    (host) =>
      host.hostname.toLowerCase().includes(searchTerm.toLowerCase()) ||
      host.ip.includes(searchTerm) ||
      host.arch.toLowerCase().includes(searchTerm.toLowerCase()),
  )

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-slate-50">
      <FinalNavbar />

      {/* 页面头部 */}
      <div className="relative overflow-hidden bg-white border-b border-slate-200/50">
        <div className="absolute inset-0 bg-gradient-to-r from-green-50/50 via-white to-blue-50/50" />
        <div className="relative max-w-7xl mx-auto px-8 py-12">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-4xl font-bold bg-gradient-to-r from-slate-800 to-slate-600 bg-clip-text text-transparent mb-2">
                主机管理
              </h1>
              <p className="text-slate-600 text-lg">监控和管理集群主机资源</p>
            </div>
            <div className="flex items-center space-x-4">
              <Badge variant="outline" className="px-4 py-2 rounded-full border-green-200 text-green-700 bg-green-50">
                <Server className="w-4 h-4 mr-2" />
                {hosts.length} 台主机在线
              </Badge>
            </div>
          </div>
        </div>
      </div>

      {/* 主要内容 */}
      <div className="max-w-7xl mx-auto px-8 py-12">
        {/* 搜索和筛选栏 */}
        <div className="flex items-center justify-between mb-8 space-x-4">
          <div className="flex items-center space-x-4 flex-1">
            <div className="relative flex-1 max-w-md">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-slate-400" />
              <Input
                placeholder="请输入IP、主机名、CPU架构等"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10 rounded-2xl border-slate-200 bg-white h-12"
              />
            </div>
            <Select value={cpuFilter} onValueChange={setCpuFilter}>
              <SelectTrigger className="w-48 rounded-2xl border-slate-200 h-12">
                <SelectValue placeholder="请选择CPU架构" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="x86_64">x86_64</SelectItem>
                <SelectItem value="arm64">ARM64</SelectItem>
              </SelectContent>
            </Select>
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger className="w-32 rounded-2xl border-slate-200 h-12">
                <SelectValue placeholder="请选择状态" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="normal">正常</SelectItem>
                <SelectItem value="warning">警告</SelectItem>
                <SelectItem value="error">异常</SelectItem>
              </SelectContent>
            </Select>
            <Button className="rounded-2xl bg-blue-500 hover:bg-blue-600 h-12 px-6">
              <Search className="mr-2 h-4 w-4" />
              搜索
            </Button>
          </div>
          <div className="flex space-x-3">
            <Button variant="outline" className="rounded-2xl border-slate-200 h-12 bg-transparent">
              <Settings className="mr-2 h-4 w-4" />
              连接操作
            </Button>
            <Button className="rounded-2xl bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 text-white border-0 shadow-lg h-12">
              <Plus className="mr-2 h-4 w-4" />
              添加主机
            </Button>
          </div>
        </div>

        {/* 主机表格 */}
        <Card className="rounded-3xl border-0 shadow-lg bg-white overflow-hidden">
          <CardContent className="p-0">
            <div className="p-6 bg-gradient-to-r from-green-50 to-blue-50 border-b border-slate-100">
              <div className="flex items-center space-x-3">
                <div className="p-3 rounded-2xl bg-gradient-to-br from-green-500 to-blue-600">
                  <Server className="h-6 w-6 text-white" />
                </div>
                <div>
                  <h2 className="text-xl font-bold text-slate-800">主机列表</h2>
                  <p className="text-slate-600">共 {filteredHosts.length} 台主机</p>
                </div>
              </div>
            </div>

            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow className="border-slate-100">
                    <TableHead className="w-12">
                      <input type="checkbox" className="rounded border-slate-300" />
                    </TableHead>
                    <TableHead className="font-semibold text-slate-700">序号</TableHead>
                    <TableHead className="font-semibold text-slate-700">状态</TableHead>
                    <TableHead className="font-semibold text-slate-700">主机名</TableHead>
                    <TableHead className="font-semibold text-slate-700">IP地址</TableHead>
                    <TableHead className="font-semibold text-slate-700">核数</TableHead>
                    <TableHead className="font-semibold text-slate-700">内存使用</TableHead>
                    <TableHead className="font-semibold text-slate-700">磁盘使用</TableHead>
                    <TableHead className="font-semibold text-slate-700">平均负载</TableHead>
                    <TableHead className="font-semibold text-slate-700">标签</TableHead>
                    <TableHead className="font-semibold text-slate-700">机架</TableHead>
                    <TableHead className="font-semibold text-slate-700">CPU架构</TableHead>
                    <TableHead className="font-semibold text-slate-700">角色</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredHosts.map((host) => (
                    <HostRow key={host.id} host={host} />
                  ))}
                </TableBody>
              </Table>
            </div>

            {/* 分页 */}
            <div className="flex items-center justify-between p-6 border-t border-slate-100">
              <div className="text-sm text-slate-600">共 {filteredHosts.length} 条</div>
              <div className="flex items-center space-x-2">
                <Button variant="outline" size="sm" className="rounded-xl bg-transparent">
                  上一页
                </Button>
                <Button variant="outline" size="sm" className="rounded-xl bg-blue-50 text-blue-600 border-blue-200">
                  1
                </Button>
                <Button variant="outline" size="sm" className="rounded-xl bg-transparent">
                  下一页
                </Button>
                <span className="text-sm text-slate-600 ml-4">10 条/页</span>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
