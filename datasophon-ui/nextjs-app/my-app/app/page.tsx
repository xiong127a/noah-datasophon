"use client"

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { 
  Server, 
  Database, 
  Activity, 
  Users, 
  BarChart3,
  Settings,
  AlertTriangle,
  CheckCircle,
  Clock,
  ArrowRight
} from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { useCluster } from '@/hooks/useCluster'
import FinalNavbar from '@/components/layout/navbar-final'
import Image from 'next/image'

export default function Home() {
  const router = useRouter()
  const { currentCluster, hasCluster, loading } = useCluster()
  const [greeting, setGreeting] = useState('')

  useEffect(() => {
    // 检查用户是否已登录
    const token = localStorage.getItem('jwt_token')
    
    if (!token) {
      // 如果未登录，重定向到登录页面
      router.push('/login')
      return
    }

    // 设置问候语
    const hour = new Date().getHours()
    if (hour < 12) {
      setGreeting('早上好')
    } else if (hour < 18) {
      setGreeting('下午好')
    } else {
      setGreeting('晚上好')
    }
  }, [router])

  // 获取集群图标路径
  const getClusterIcon = () => {
    if (!currentCluster) return "/images/cluster/kubernetes-logo.svg"
    return currentCluster.isK8s 
      ? "/images/cluster/kubernetes-logo.svg" 
      : "/images/cluster/linux-tux.svg"
  }

  // 快捷操作
  const quickActions = [
    {
      title: "集群管理",
      description: "查看和管理所有集群",
      icon: Server,
      href: "/clusters/list",
      color: "from-blue-500 to-blue-600"
    },
    {
      title: "主机管理", 
      description: "管理集群主机节点",
      icon: Database,
      href: "/hosts",
      color: "from-green-500 to-green-600",
      requiresCluster: true
    },
    {
      title: "用户管理",
      description: "管理系统用户权限",
      icon: Users,
      href: "/system/users",
      color: "from-purple-500 to-purple-600"
    },
    {
      title: "系统设置",
      description: "配置系统参数",
      icon: Settings,
      href: "/system/racks",
      color: "from-orange-500 to-orange-600",
      requiresCluster: true
    }
  ]

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">正在加载...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-gray-100">
      <FinalNavbar />
      
      <div className="container mx-auto px-6 py-8">
        {/* 欢迎区域 */}
        <div className="mb-8">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h1 className="text-4xl font-bold text-gray-900 mb-2">
                {greeting}！👋
              </h1>
              <p className="text-lg text-gray-600">
                欢迎使用 Noah 大数据基础平台
              </p>
            </div>
            
            {/* 当前时间 */}
            <div className="bg-white/80 backdrop-blur-sm rounded-2xl p-4 shadow-lg border border-white/50">
              <div className="flex items-center space-x-2 text-gray-600">
                <Clock className="h-4 w-4" />
                <span className="text-sm font-medium">
                  {new Date().toLocaleString('zh-CN', {
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric',
                    weekday: 'long',
                    hour: '2-digit',
                    minute: '2-digit'
                  })}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* 当前集群信息 */}
        {hasCluster && currentCluster ? (
          <Card className="mb-8 bg-gradient-to-r from-blue-50 to-indigo-50 border-blue-200/50">
            <CardHeader>
              <CardTitle className="flex items-center space-x-3">
                <Image 
                  src={getClusterIcon()}
                  alt="集群图标"
                  width={24}
                  height={24}
                />
                <span>当前集群: {currentCluster.name}</span>
                <Badge className="bg-green-100 text-green-700 border-green-200">
                  <CheckCircle className="h-3 w-3 mr-1" />
                  已连接
                </Badge>
              </CardTitle>
              <CardDescription>
                类型: {currentCluster.isK8s ? 'Kubernetes 集群' : '物理/虚拟机集群'}
              </CardDescription>
            </CardHeader>
          </Card>
        ) : (
          <Card className="mb-8 bg-gradient-to-r from-amber-50 to-orange-50 border-amber-200/50">
            <CardHeader>
              <CardTitle className="flex items-center space-x-3">
                <AlertTriangle className="h-5 w-5 text-amber-500" />
                <span>未选择集群</span>
              </CardTitle>
              <CardDescription>
                请先选择一个集群以使用完整功能
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Button 
                onClick={() => router.push('/clusters/list')} 
                className="bg-amber-500 hover:bg-amber-600"
              >
                前往集群管理
                <ArrowRight className="h-4 w-4 ml-2" />
              </Button>
            </CardContent>
          </Card>
        )}

        {/* 系统状态概览 */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <Card className="bg-white/80 backdrop-blur-sm border-white/50">
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center space-x-2 text-sm font-medium text-gray-600">
                <Server className="h-4 w-4 text-blue-500" />
                <span>系统状态</span>
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-green-600 mb-1">运行正常</div>
              <p className="text-sm text-gray-500">所有核心服务运行中</p>
            </CardContent>
          </Card>

          <Card className="bg-white/80 backdrop-blur-sm border-white/50">
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center space-x-2 text-sm font-medium text-gray-600">
                <Activity className="h-4 w-4 text-emerald-500" />
                <span>性能指标</span>
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-blue-600 mb-1">优秀</div>
              <p className="text-sm text-gray-500">CPU: 45% | 内存: 62%</p>
            </CardContent>
          </Card>

          <Card className="bg-white/80 backdrop-blur-sm border-white/50">
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center space-x-2 text-sm font-medium text-gray-600">
                <BarChart3 className="h-4 w-4 text-purple-500" />
                <span>数据处理</span>
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-purple-600 mb-1">活跃</div>
              <p className="text-sm text-gray-500">今日处理 2.3TB 数据</p>
            </CardContent>
          </Card>
        </div>

        {/* 快捷操作 */}
        <div className="mb-8">
          <h2 className="text-2xl font-bold text-gray-900 mb-6">快捷操作</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {quickActions.map((action, index) => {
              const disabled = action.requiresCluster && !hasCluster
              return (
                <Card 
                  key={index}
                  className={`cursor-pointer transition-all duration-200 hover:scale-105 hover:shadow-lg ${
                    disabled ? 'opacity-50 cursor-not-allowed' : ''
                  }`}
                  onClick={() => !disabled && router.push(action.href)}
                >
                  <CardHeader className="pb-3">
                    <div className={`w-12 h-12 rounded-xl bg-gradient-to-r ${action.color} flex items-center justify-center mb-3`}>
                      <action.icon className="h-6 w-6 text-white" />
                    </div>
                    <CardTitle className="text-lg">{action.title}</CardTitle>
                    <CardDescription>{action.description}</CardDescription>
                  </CardHeader>
                </Card>
              )
            })}
          </div>
        </div>

        {/* 最近活动 */}
        <Card className="bg-white/80 backdrop-blur-sm border-white/50">
          <CardHeader>
            <CardTitle>最近活动</CardTitle>
            <CardDescription>系统最新的操作和事件</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <div className="flex items-center space-x-3 p-3 bg-blue-50 rounded-lg">
                <CheckCircle className="h-5 w-5 text-green-500" />
                <div>
                  <p className="font-medium">集群连接成功</p>
                  <p className="text-sm text-gray-500">刚刚</p>
                </div>
              </div>
              <div className="flex items-center space-x-3 p-3 bg-gray-50 rounded-lg">
                <Activity className="h-5 w-5 text-blue-500" />
                <div>
                  <p className="font-medium">系统启动完成</p>
                  <p className="text-sm text-gray-500">5 分钟前</p>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}