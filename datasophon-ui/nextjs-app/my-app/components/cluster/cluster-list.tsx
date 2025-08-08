"use client"
import { useState, useEffect } from "react"
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
  Star,
  Activity,
  TrendingUp,
} from "lucide-react"

// 自定义创建集群图标组件
const CreateClusterIcon = ({ className }: { className?: string }) => (
  <svg 
    viewBox="0 0 120 120" 
    className={className}
    xmlns="http://www.w3.org/2000/svg"
  >
    <defs>
      <linearGradient id="serverGradient" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stopColor="#6366f1" />
        <stop offset="50%" stopColor="#8b5cf6" />
        <stop offset="100%" stopColor="#d946ef" />
      </linearGradient>
      <linearGradient id="lightGradient" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stopColor="#ffffff" stopOpacity="0.8" />
        <stop offset="100%" stopColor="#ffffff" stopOpacity="0.2" />
      </linearGradient>
      <filter id="glow">
        <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
        <feMerge> 
          <feMergeNode in="coloredBlur"/>
          <feMergeNode in="SourceGraphic"/>
        </feMerge>
      </filter>
    </defs>
    
    {/* 主服务器机架 */}
    <rect x="25" y="30" width="70" height="60" rx="8" fill="url(#serverGradient)" filter="url(#glow)" />
    
    {/* 服务器机架细节线条 */}
    <rect x="30" y="40" width="60" height="8" rx="2" fill="url(#lightGradient)" />
    <rect x="30" y="52" width="60" height="8" rx="2" fill="url(#lightGradient)" />
    <rect x="30" y="64" width="60" height="8" rx="2" fill="url(#lightGradient)" />
    <rect x="30" y="76" width="60" height="8" rx="2" fill="url(#lightGradient)" />
    
    {/* 服务器指示灯 */}
    <circle cx="85" cy="44" r="2" fill="#10f2c4" />
    <circle cx="85" cy="56" r="2" fill="#10f2c4" />
    <circle cx="85" cy="68" r="2" fill="#fbbf24" />
    <circle cx="85" cy="80" r="2" fill="#10f2c4" />
    
    {/* 加号背景圆 */}
    <circle cx="85" cy="25" r="15" fill="url(#serverGradient)" filter="url(#glow)" />
    <circle cx="85" cy="25" r="12" fill="url(#lightGradient)" />
    
    {/* 加号 */}
    <rect x="82" y="18" width="6" height="14" rx="1" fill="url(#serverGradient)" />
    <rect x="78" y="22" width="14" height="6" rx="1" fill="url(#serverGradient)" />
    
    {/* 连接线 */}
    <path d="M 60 35 Q 75 15 70 25" stroke="url(#serverGradient)" strokeWidth="2" fill="none" strokeDasharray="3,2" />
    
    {/* 装饰性光点 */}
    <circle cx="35" cy="20" r="1.5" fill="#6366f1" opacity="0.6" />
    <circle cx="45" cy="15" r="1" fill="#8b5cf6" opacity="0.8" />
    <circle cx="75" cy="12" r="1.5" fill="#d946ef" opacity="0.6" />
  </svg>
)

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
import ClusterAuthorizationDialogSuper from "./authorization-dialog"
import CreateClusterDialogEnhanced from "./create-dialog"
import ClusterStep1Dialog, { Step1Data } from "./cluster-step1-dialog"
import ClusterStep2Dialog from "./cluster-step2-dialog"
import { apiClient, API_PATHS } from "@/lib/api"
import { useRouter } from "next/navigation"
import Image from "next/image"

// 集群类型定义
interface ClusterManager {
  id: string | number;
  username: string;
}

interface ClusterItem {
  id: string | number;
  clusterName: string;
  clusterCode?: string;
  clusterFrame?: string;
  depType?: string;
  clusterState: string;
  clusterStateCode: number;
  createTime: string;
  clusterManagerList: ClusterManager[];
  userManageName?: string;
}

const ClusterCard = ({ cluster, onEnter, onEdit, onSetup, onAuth, onDelete }: { 
  cluster: ClusterItem; 
  onEnter: (cluster: ClusterItem) => void;
  onEdit: (cluster: ClusterItem) => void;
  onSetup: (cluster: ClusterItem) => void;
  onAuth: (cluster: ClusterItem) => void;
  onDelete: (cluster: ClusterItem) => void;
}) => {
  const [authDialogOpen, setAuthDialogOpen] = useState(false)
  
  // 根据集群类型获取图标路径
  const getIconPath = () => {
    switch (cluster.depType) {
      case "Kubernetes":
        return "/images/cluster/kubernetes-logo.svg";
      case "PVM":
        return "/images/cluster/linux-tux.svg";
      default:
        return "/images/cluster/kubernetes-logo.svg";
    }
  }

  const iconPath = getIconPath();

  // 根据集群类型获取增强的颜色方案
  const getClusterTypeColors = () => {
    switch (cluster.depType) {
      case "Kubernetes":
        return {
          gradient: "from-blue-500 via-blue-600 to-cyan-500",
          bgGradient: "from-blue-50/80 via-blue-100/40 to-cyan-50/80",
          shadowColor: "shadow-blue-500/25",
          glowColor: "from-blue-400/30 to-cyan-400/30",
          accentColor: "text-blue-600",
          badgeColor: "bg-blue-100 text-blue-700 border-blue-200",
        };
      case "PVM":
        return {
          gradient: "from-emerald-500 via-green-600 to-teal-500",
          bgGradient: "from-emerald-50/80 via-green-100/40 to-teal-50/80",
          shadowColor: "shadow-emerald-500/25",
          glowColor: "from-emerald-400/30 to-teal-400/30",
          accentColor: "text-emerald-600",
          badgeColor: "bg-emerald-100 text-emerald-700 border-emerald-200",
        };
      default:
        return {
          gradient: "from-purple-500 via-violet-600 to-indigo-500",
          bgGradient: "from-purple-50/80 via-violet-100/40 to-indigo-50/80",
          shadowColor: "shadow-purple-500/25",
          glowColor: "from-purple-400/30 to-indigo-400/30",
          accentColor: "text-purple-600",
          badgeColor: "bg-purple-100 text-purple-700 border-purple-200",
        };
    }
  }

  const colors = getClusterTypeColors();

  // 格式化日期
  const formatDate = (dateString: string) => {
    if (!dateString) return "-";
    const date = new Date(dateString);
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    });
  };



  return (
    <>
      <Card className={`group relative overflow-hidden rounded-3xl border-0 bg-white ${colors.shadowColor} shadow-xl hover:shadow-2xl transition-all duration-700 hover:-translate-y-3 animate-scale-in h-[560px]`}>
        {/* 主背景渐变 */}
        <div className={`absolute inset-0 bg-gradient-to-br ${colors.bgGradient}`} />
        
        {/* 动态光效背景 */}
        <div className={`absolute inset-0 bg-gradient-to-br ${colors.glowColor} opacity-0 group-hover:opacity-100 transition-opacity duration-1000`} />
        
        {/* 装饰性光效 - 多层设计 */}
        <div className="absolute top-0 right-0 w-40 h-40 bg-gradient-to-br from-white/30 to-transparent rounded-full blur-3xl transform translate-x-20 -translate-y-20 group-hover:scale-150 transition-transform duration-1000" />
        <div className="absolute bottom-0 left-0 w-32 h-32 bg-gradient-to-tr from-white/20 to-transparent rounded-full blur-2xl transform -translate-x-16 translate-y-16 group-hover:scale-125 transition-transform duration-1000" />
        
        {/* 边框光效 */}
        <div className={`absolute inset-0 rounded-3xl bg-gradient-to-r ${colors.gradient} opacity-0 group-hover:opacity-20 transition-opacity duration-500 blur-sm`} />

        <CardContent className="relative p-8 z-10 h-full flex flex-col">
          {/* 头部信息 - 增强设计 */}
          <div className="flex items-start justify-between mb-6">
            <div className="flex items-center space-x-5">
              {/* 图标容器 - 3D效果 */}
              <div className="relative perspective-1000">
                <div className={`relative p-4 rounded-3xl bg-gradient-to-br ${colors.gradient} shadow-2xl group-hover:scale-110 transition-all duration-500 preserve-3d`}>
                  <Image 
                    src={iconPath}
                    alt={cluster.depType || "集群类型"}
                    width={48}
                    height={48}
                    className="relative z-10 group-hover:rotate-12 transition-transform duration-500"
                  />
                  <div className="absolute inset-0 rounded-3xl bg-white/25 backdrop-blur-sm" />
                  {/* 发光效果 */}
                  <div className={`absolute -inset-2 rounded-3xl bg-gradient-to-br ${colors.gradient} blur-xl opacity-50 group-hover:opacity-75 transition-opacity duration-500`} />
                </div>
                {/* 悬浮装饰 */}
                <div className="absolute -top-2 -right-2 w-6 h-6 bg-gradient-to-r from-yellow-400 to-orange-400 rounded-full flex items-center justify-center transform scale-0 group-hover:scale-100 transition-transform duration-300 delay-200">
                  <Star className="h-3 w-3 text-white" />
                </div>
              </div>
              
              <div className="space-y-2">
                <h3 className="text-2xl font-bold bg-gradient-to-r from-slate-800 to-slate-600 bg-clip-text text-transparent group-hover:from-slate-900 group-hover:to-slate-700 transition-all duration-300">
                  {cluster.clusterName}
                </h3>
                <Badge className={`${colors.badgeColor} border-0 rounded-full px-4 py-1 font-medium shadow-lg group-hover:scale-105 transition-transform duration-300`}>
                  {cluster.depType === "PVM" ? "裸金属/虚拟机" : cluster.depType}
                </Badge>
              </div>
            </div>

            {/* 状态指示器 - 增强动画 */}
            <div className="relative">
              <div className={`w-4 h-4 rounded-full shadow-lg relative z-10 ${
                cluster.clusterStateCode === 1 ? "bg-gray-400" :
                cluster.clusterStateCode === 3 ? "bg-green-400" :
                cluster.clusterStateCode === 4 ? "bg-red-400" : "bg-slate-400"
              }`}>
                <div className={`absolute inset-0 rounded-full animate-ping ${
                  cluster.clusterStateCode === 1 ? "bg-gray-400" :
                  cluster.clusterStateCode === 3 ? "bg-green-400" :
                  cluster.clusterStateCode === 4 ? "bg-red-400" : "bg-slate-400"
                }`} />
              </div>
              <div className={`absolute inset-0 w-4 h-4 rounded-full blur-md opacity-75 ${
                cluster.clusterStateCode === 1 ? "bg-gray-400" :
                cluster.clusterStateCode === 3 ? "bg-green-400" :
                cluster.clusterStateCode === 4 ? "bg-red-400" : "bg-slate-400"
              }`} />
            </div>
          </div>



          {/* 详细信息 - 重新设计 */}
          <div className="space-y-4 mb-6">
            <div className="flex items-center justify-between bg-white/70 backdrop-blur-sm rounded-2xl p-4 border border-white/30 group-hover:bg-white/90 transition-all duration-300">
              <div className="flex items-center space-x-3">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-100 to-blue-200 flex items-center justify-center">
                  <Users className="h-5 w-5 text-blue-600" />
                </div>
                <div>
                  <p className="text-xs text-slate-500 font-medium">管理员</p>
                  <p className="text-sm font-semibold text-slate-700">{cluster.userManageName || '未分配'}</p>
                </div>
              </div>
              <Activity className="h-4 w-4 text-green-500 animate-pulse" />
            </div>
            
            <div className="flex items-center justify-between bg-white/70 backdrop-blur-sm rounded-2xl p-4 border border-white/30 group-hover:bg-white/90 transition-all duration-300">
              <div className="flex items-center space-x-3">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-purple-100 to-purple-200 flex items-center justify-center">
                  <Calendar className="h-5 w-5 text-purple-600" />
                </div>
                <div>
                  <p className="text-xs text-slate-500 font-medium">创建时间</p>
                  <p className="text-sm font-semibold text-slate-700">{formatDate(cluster.createTime)}</p>
                </div>
              </div>
              <TrendingUp className="h-4 w-4 text-green-500" />
            </div>
          </div>

          {/* 按钮组 - 增强样式 */}
          <div className="space-y-4 mt-auto">
            {/* 主操作按钮 */}
            <Button
              disabled={!cluster.clusterStateCode || cluster.clusterStateCode > 4}
              onClick={() => {
                if (cluster.clusterStateCode === 1) {
                  // 待配置 - 开始配置
                  onSetup(cluster);
                } else if (cluster.clusterStateCode === 3) {
                  // 正在运行 - 进入集群
                  onEnter(cluster);
                } else if (cluster.clusterStateCode === 4) {
                  // 停止 - 启动集群
                  onEnter(cluster); // 暂时使用onEnter，后续可以添加专门的启动函数
                }
              }}
              className={`w-full h-12 rounded-2xl font-semibold text-lg transition-all duration-500 group/btn relative overflow-hidden ${
                cluster.clusterStateCode && cluster.clusterStateCode <= 4
                  ? `bg-gradient-to-r ${colors.gradient} hover:shadow-2xl hover:shadow-blue-200 text-white border-0 hover:scale-105`
                  : "bg-slate-100 text-slate-400 cursor-not-allowed border-0"
              }`}
            >
              {/* 按钮发光效果 */}
              {(cluster.clusterStateCode && cluster.clusterStateCode <= 4) && (
                <div className="absolute inset-0 bg-gradient-to-r from-white/0 via-white/25 to-white/0 translate-x-[-100%] group-hover/btn:translate-x-[100%] transition-transform duration-1000" />
              )}
              <Play className="mr-3 h-5 w-5 relative z-10" />
              <span className="relative z-10">
                {cluster.clusterStateCode === 1 && "开始配置"}
                {cluster.clusterStateCode === 3 && "进入集群"}
                {cluster.clusterStateCode === 4 && "启动集群"}
                {(!cluster.clusterStateCode || cluster.clusterStateCode > 4) && "状态异常"}
              </span>
            </Button>

            {/* 次要操作按钮 */}
            <div className="grid grid-cols-2 gap-3">
              <Button
                variant="secondary"
                onClick={() => onSetup(cluster)}
                className="h-11 rounded-2xl bg-white/80 hover:bg-white border border-white/50 hover:shadow-lg transition-all duration-300 text-slate-700 hover:scale-105 backdrop-blur-sm"
              >
                <Settings className="mr-2 h-4 w-4" />
                配置集群
              </Button>
              <Button
                variant="secondary"
                onClick={() => {
                  setAuthDialogOpen(true);
                  onAuth(cluster);
                }}
                className="h-11 rounded-2xl bg-white/80 hover:bg-white border border-white/50 hover:shadow-lg transition-all duration-300 text-slate-700 hover:scale-105 backdrop-blur-sm"
              >
                <Shield className="mr-2 h-4 w-4" />
                用户授权
              </Button>
            </div>

            {/* 更多操作 */}
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button
                  variant="secondary"
                  className="w-full h-10 rounded-2xl bg-slate-50/80 hover:bg-slate-100 border border-slate-200/50 hover:shadow-lg transition-all duration-300 text-slate-600"
                >
                  <MoreHorizontal className="mr-2 h-4 w-4" />
                  更多操作
                  <ChevronRight className="ml-2 h-4 w-4 group-hover:translate-x-1 transition-transform" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent className="w-56 rounded-3xl border-0 shadow-2xl bg-white/95 backdrop-blur-xl p-2">
                <DropdownMenuItem 
                  className="rounded-2xl m-1 hover:bg-slate-50 p-3 transition-all duration-200"
                  onClick={() => onEdit(cluster)}
                >
                  <Edit className="mr-3 h-4 w-4" />
                  编辑集群配置
                </DropdownMenuItem>

                <DropdownMenuSeparator className="my-2" />
                <DropdownMenuItem 
                  className="rounded-2xl m-1 text-red-600 hover:bg-red-50 p-3 transition-all duration-200"
                  onClick={() => onDelete(cluster)}
                >
                  <Trash2 className="mr-3 h-4 w-4" />
                  删除集群
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </CardContent>
      </Card>

      {/* 授权弹窗 */}
      <ClusterAuthorizationDialogSuper 
        open={authDialogOpen} 
        onOpenChange={setAuthDialogOpen} 
        clusterName={cluster.clusterName}
        clusterId={cluster.id}
      />
    </>
  )
}

const CreateClusterCard = ({ onClick }: { onClick: () => void }) => {
  return (
    <Card
      className="group relative overflow-hidden rounded-3xl border-0 bg-white shadow-xl hover:shadow-2xl transition-all duration-700 hover:-translate-y-3 cursor-pointer animate-scale-in h-[560px]"
      onClick={onClick}
    >
        {/* 动态背景渐变 */}
        <div className="absolute inset-0 bg-gradient-to-br from-indigo-50/80 via-purple-50/80 to-pink-50/80" />
        <div className="absolute inset-0 bg-gradient-to-br from-indigo-500/5 via-purple-500/5 to-pink-500/5 opacity-0 group-hover:opacity-100 transition-opacity duration-1000" />

        {/* 多层装饰性光效 */}
        <div className="absolute top-0 right-0 w-48 h-48 bg-gradient-to-br from-indigo-400/20 to-purple-400/20 rounded-full blur-3xl transform translate-x-24 -translate-y-24 group-hover:scale-150 transition-transform duration-1000" />
        <div className="absolute bottom-0 left-0 w-40 h-40 bg-gradient-to-tr from-pink-400/20 to-orange-400/20 rounded-full blur-2xl transform -translate-x-20 translate-y-20 group-hover:scale-125 transition-transform duration-1000" />
        
        {/* 边框发光效果 */}
        <div className="absolute inset-0 rounded-3xl bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-500 opacity-0 group-hover:opacity-20 transition-opacity duration-500 blur-sm" />

        <CardContent className="relative p-8 h-full flex flex-col justify-between items-center text-center z-10">
          {/* 主图标 - 增强3D效果 */}
          <div className="relative mb-6 perspective-1000">
            <div className="w-24 h-24 rounded-3xl bg-gradient-to-br from-slate-100 to-slate-200 flex items-center justify-center shadow-2xl group-hover:scale-110 group-hover:rotate-6 transition-all duration-500 preserve-3d overflow-hidden">
              <CreateClusterIcon className="h-16 w-16 group-hover:scale-110 transition-transform duration-300" />
              <div className="absolute inset-0 rounded-3xl bg-white/30 backdrop-blur-sm" />
            </div>
            <div className="absolute -inset-3 rounded-3xl bg-gradient-to-br from-indigo-500 via-purple-600 to-pink-500 blur-2xl opacity-20 group-hover:opacity-40 transition-opacity duration-500" />
            
            {/* 浮动装饰 */}
            <div className="absolute -top-3 -right-3 w-8 h-8 bg-gradient-to-r from-yellow-400 to-orange-400 rounded-full flex items-center justify-center transform scale-0 group-hover:scale-100 transition-transform duration-300 delay-200 animate-float">
              <Sparkles className="h-4 w-4 text-white" />
            </div>
          </div>

          {/* 中间内容区域 */}
          <div className="flex-1 flex flex-col justify-center">
            {/* 标题 - 渐变文字 */}
            <h3 className="text-3xl font-bold bg-gradient-to-r from-indigo-600 via-purple-600 to-pink-600 bg-clip-text text-transparent mb-6 group-hover:scale-105 transition-transform duration-300">
              创建新集群
            </h3>

            {/* 特性列表 - 重新设计 */}
            <div className="space-y-4 mb-6 w-full">
                          <div className="flex items-center justify-center text-slate-600 group-hover:text-slate-800 transition-colors bg-white/60 rounded-2xl p-3 backdrop-blur-sm border border-white/30">
                <Rocket className="h-5 w-5 mr-3 text-indigo-500" />
                <span className="font-medium">快速部署全新环境</span>
              </div>
              <div className="flex items-center justify-center text-slate-600 group-hover:text-slate-800 transition-colors bg-white/60 rounded-2xl p-3 backdrop-blur-sm border border-white/30">
                <Brain className="h-5 w-5 mr-3 text-purple-500" />
                <span className="font-medium">AI智能配置优化</span>
              </div>
              <div className="flex items-center justify-center text-slate-600 group-hover:text-slate-800 transition-colors bg-white/60 rounded-2xl p-3 backdrop-blur-sm border border-white/30">
                <Zap className="h-5 w-5 mr-3 text-pink-500" />
                <span className="font-medium">高性能计算集群</span>
              </div>
                      </div>
          </div>

          {/* 底部区域 */}
          <div className="space-y-4">
            {/* 创建按钮 - 增强样式 */}
            <Button className="w-full h-12 rounded-2xl bg-gradient-to-r from-indigo-500 via-purple-600 to-pink-500 hover:from-indigo-600 hover:via-purple-700 hover:to-pink-600 text-white border-0 shadow-2xl hover:shadow-3xl transition-all duration-500 group-hover:scale-105 font-semibold text-lg relative overflow-hidden group/btn">
            {/* 按钮光效 */}
            <div className="absolute inset-0 bg-gradient-to-r from-white/0 via-white/25 to-white/0 translate-x-[-100%] group-hover/btn:translate-x-[100%] transition-transform duration-1000" />
                          <Plus className="mr-3 h-5 w-5 relative z-10" />
              <span className="relative z-10">立即创建集群</span>
              <ChevronRight className="ml-3 h-5 w-5 group-hover:translate-x-1 transition-transform relative z-10" />
          </Button>

                      {/* 底部提示 */}
            <div className="mt-6 flex items-center justify-center space-x-4">
              <Badge variant="outline" className="bg-white/80 border-indigo-200 text-indigo-700 rounded-full">
                <Server className="h-3 w-3 mr-1" />
                Kubernetes
              </Badge>
              <Badge variant="outline" className="bg-white/80 border-emerald-200 text-emerald-700 rounded-full">
                <Cloud className="h-3 w-3 mr-1" />
                虚拟机
              </Badge>
            </div>
          </div>
        </CardContent>
      </Card>
    )
}

export default function ClusterListEnhanced() {
  const [clusters, setClusters] = useState<ClusterItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [setupDialogOpen, setSetupDialogOpen] = useState(false);
  const [step2DialogOpen, setStep2DialogOpen] = useState(false);
  const [editingCluster, setEditingCluster] = useState<ClusterItem | null>(null);
  const [setupCluster, setSetupCluster] = useState<ClusterItem | null>(null);
  const [step1Data, setStep1Data] = useState<Step1Data | null>(null);
  const router = useRouter();

  // 获取集群列表
  const fetchClusters = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.get(API_PATHS.CLUSTER_LIST);
      
      if (response.data && response.data.code === 200) {
        const processedClusters = response.data.data.map((item: ClusterItem) => {
          const managerNames = item.clusterManagerList?.map((manager: ClusterManager) => manager.username).filter(Boolean) || [];
          return {
            ...item,
            userManageName: managerNames.join(', ') || '未分配'
          };
        });
        
        setClusters(processedClusters);
      } else {
        setError(response.data?.msg || "获取集群列表失败");
      }
    } catch (err: any) {
      console.error("获取集群列表出错:", err);
      setError(err.message || "网络错误，请稍后重试");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchClusters();
  }, []);

  // 事件处理函数
  const handleEnterCluster = async (cluster: ClusterItem) => {
    try {
      const response = await apiClient.post(API_PATHS.CLUSTER_SERVICE_LIST, { clusterId: cluster.id });
      if (response.data && response.data.code === 200) {
        localStorage.setItem('current_cluster_id', cluster.id.toString());
        localStorage.setItem('current_cluster_name', cluster.clusterName);
        router.push(`/clusters/${cluster.id}`);
      } else {
        alert(response.data?.msg || "进入集群失败");
      }
    } catch (err) {
      console.error("进入集群失败:", err);
      alert("进入集群失败，请稍后重试");
    }
  };

  const handleEditCluster = (cluster: ClusterItem) => {
    setEditingCluster(cluster);
    setEditDialogOpen(true);
  };

  const handleAuthCluster = (cluster: ClusterItem) => {
    // 授权对话框会通过组件内部状态打开
  };

  const handleDeleteCluster = (cluster: ClusterItem) => {
    if (confirm(`确定要删除集群 "${cluster.clusterName}" 吗？此操作不可撤销。`)) {
      deleteCluster(cluster.id);
    }
  };

  const deleteCluster = async (clusterId: string | number) => {
    try {
      const response = await apiClient.post(API_PATHS.CLUSTER_DELETE, [clusterId]);
      
      if (response.data && response.data.code === 200) {
        alert("删除集群成功");
        fetchClusters();
      } else {
        alert(response.data?.msg || "删除集群失败");
      }
    } catch (err) {
      console.error("删除集群失败:", err);
      alert("删除集群失败，请稍后重试");
    }
  };

  const handleCreateCluster = () => {
    setCreateDialogOpen(true);
  };

  const handleClusterSuccess = () => {
    // 集群创建/编辑成功后刷新列表
    fetchClusters();
  };

  // 处理Step1完成，打开Step2
  const handleStep1Complete = (data: Step1Data) => {
    console.log('Step1完成，准备打开Step2:', data);
    setStep1Data(data);
    setSetupDialogOpen(false);
    setStep2DialogOpen(true);
  };

  // 处理Step2完成
  const handleStep2Complete = () => {
    console.log('Step2完成');
    setStep2DialogOpen(false);
    setStep1Data(null);
    handleClusterSuccess();
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50/30 to-indigo-50/50 relative overflow-hidden">
      {/* 背景装饰 */}
      <div className="absolute top-0 left-0 w-96 h-96 bg-gradient-to-br from-blue-400/10 to-indigo-400/10 rounded-full blur-3xl transform -translate-x-48 -translate-y-48" />
      <div className="absolute bottom-0 right-0 w-80 h-80 bg-gradient-to-br from-purple-400/10 to-pink-400/10 rounded-full blur-3xl transform translate-x-40 translate-y-40" />

      {/* 页面头部 - 增强设计 */}
      <div className="relative overflow-hidden bg-white/80 backdrop-blur-xl border-b border-slate-200/50 shadow-lg">
        <div className="absolute inset-0 bg-gradient-to-r from-blue-50/80 via-white/90 to-purple-50/80" />
        <div className="relative w-full px-8 py-12">
          <div className="flex items-center justify-between">
            <div className="space-y-2">
              <h1 className="text-4xl font-bold bg-gradient-to-r from-slate-800 via-slate-700 to-slate-600 bg-clip-text text-transparent">
                集群管理中心
              </h1>
              <p className="text-lg text-slate-600">统一管理和监控您的大数据集群环境</p>
              <div className="flex items-center space-x-2 pt-2">
                <div className="w-2 h-2 bg-green-400 rounded-full animate-pulse" />
                <span className="text-sm text-slate-500">实时监控 • 智能管理 • 高效运维</span>
              </div>
            </div>
            
            <div className="flex items-center space-x-6">
              <div className="bg-white/90 backdrop-blur-sm rounded-3xl p-6 shadow-xl border border-white/50">
                <div className="flex items-center space-x-4">
                  <Badge className="px-6 py-3 rounded-2xl border-blue-200 text-blue-700 bg-blue-50/80 text-lg font-semibold">
                    <Server className="h-5 w-5 mr-3 text-blue-600" />
                    总集群: {clusters.length}
                  </Badge>
                  <Badge className="px-6 py-3 rounded-2xl border-green-200 text-green-700 bg-green-50/80 text-lg font-semibold">
                    <div className="w-3 h-3 bg-green-400 rounded-full mr-3 animate-pulse" />
                    运行中: {clusters.filter((c) => c.clusterStateCode === 3).length}
                  </Badge>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 加载状态 */}
      {loading && (
        <div className="max-w-7xl mx-auto px-8 py-16 text-center">
          <div className="inline-flex items-center space-x-4 bg-white/90 backdrop-blur-sm rounded-3xl p-8 shadow-xl">
            <div className="animate-spin h-10 w-10 border-4 rounded-full border-blue-600 border-t-transparent"></div>
            <p className="text-xl text-slate-600 font-medium">正在加载集群数据...</p>
          </div>
        </div>
      )}

      {/* 错误状态 */}
      {!loading && error && (
        <div className="max-w-7xl mx-auto px-8 py-16 text-center">
          <div className="bg-red-50/90 backdrop-blur-sm text-red-700 p-8 rounded-3xl shadow-xl">
            <p className="text-lg">{error}</p>
            <Button 
              variant="outline" 
              className="mt-6 rounded-2xl"
              onClick={fetchClusters}
            >
              重新加载
            </Button>
          </div>
        </div>
      )}

      {/* 集群网格 */}
      {!loading && !error && (
        <div className="w-full px-8 py-8">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5 gap-8">
            {clusters.map((cluster, index) => (
              <div
                key={cluster.id}
                style={{ animationDelay: `${index * 100}ms` }}
                className="animate-scale-in"
              >
                <ClusterCard 
                  cluster={cluster} 
                  onEnter={handleEnterCluster}
                  onEdit={handleEditCluster}
                  onSetup={(cluster) => {
                    setSetupCluster(cluster);
                    setSetupDialogOpen(true);
                  }}
                  onAuth={handleAuthCluster}
                  onDelete={handleDeleteCluster}
                />
              </div>
            ))}

            {/* 创建新集群卡片 */}
            <div
              style={{ animationDelay: `${clusters.length * 100}ms` }}
              className="animate-scale-in"
            >
              <CreateClusterCard onClick={handleCreateCluster} />
            </div>
          </div>
        </div>
      )}

      {/* 创建集群弹窗 */}
      <CreateClusterDialogEnhanced 
        open={createDialogOpen} 
        onOpenChange={setCreateDialogOpen}
        onSuccess={handleClusterSuccess}
      />

      {/* 编辑集群弹窗 */}
      <CreateClusterDialogEnhanced 
        open={editDialogOpen} 
        onOpenChange={setEditDialogOpen}
        onSuccess={handleClusterSuccess}
        editData={editingCluster ? {
          id: Number(editingCluster.id),
          clusterName: editingCluster.clusterName,
          clusterCode: editingCluster.clusterCode || '',
          clusterFrame: editingCluster.clusterFrame || '',
          depType: editingCluster.depType || ''
        } : null}
      />

                {/* 配置集群Step1弹窗 */}
          <ClusterStep1Dialog
            open={setupDialogOpen}
            onOpenChange={setSetupDialogOpen}
            cluster={setupCluster ? {
              id: typeof setupCluster.id === 'string' ? parseInt(setupCluster.id) : setupCluster.id,
              clusterName: setupCluster.clusterName,
              depType: setupCluster.depType || '',
              clusterCode: setupCluster.clusterCode || ''
            } : null}
            onSuccess={handleClusterSuccess}
            onStep1Complete={handleStep1Complete}
          />

          {/* 配置集群Step2弹窗 */}
          {step1Data && (
            <ClusterStep2Dialog
              open={step2DialogOpen}
              onOpenChange={setStep2DialogOpen}
              cluster={setupCluster ? {
                id: typeof setupCluster.id === 'string' ? parseInt(setupCluster.id) : setupCluster.id,
                clusterName: setupCluster.clusterName,
                depType: setupCluster.depType || '',
                clusterCode: setupCluster.clusterCode || ''
              } : null}
              step1Data={step1Data}
              onSuccess={handleStep2Complete}
              onPrevious={() => {
                setStep2DialogOpen(false);
                setSetupDialogOpen(true);
              }}
            />
          )}
    </div>
  )
} 