"use client"
import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import {
  Server,
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
  Grid3X3,
} from "lucide-react"
import { ClusterTypeUtil } from '@/types'
import { Step3Data } from '@/types/service-selection'
import type { Step3AgentData } from '@/types/agent-deployment'
import type { Step5Data } from '@/types/master-role-assign'
import type { Step6Data } from '@/types/worker-role-assign'

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
import { CARD_STYLES, BUTTON_STYLES, BADGE_STYLES, DIALOG_STYLES } from "./common/shared-styles"
// K8S专用组件
import K8sHostConfigDialog, { K8sStep1Data } from "./kubernetes/k8s-host-config-dialog"
import K8sHostValidationDialog from "./kubernetes/k8s-host-validation-dialog"
// PVM专用组件
import PvmHostConfigDialog, { PvmStep1Data } from "./pvm/pvm-host-config-dialog"
import PvmHostValidationDialog from "./pvm/pvm-host-validation-dialog"
// 通用组件
import AgentDeploymentDialog from "./common/agent-deployment-dialog"
import ServiceSelectionDialog from "./common/service-selection-dialog"
import MasterRoleAssignDialog from "./common/master-role-assign-dialog"
import WorkerRoleAssignDialog from "./common/worker-role-assign-dialog"
import ServiceConfigDialog from "./common/service-config-dialog"
import ServiceInstallDialog from "./common/service-install-dialog"
import { Step7Data } from '@/types/service-config'
import { apiClient, API_PATHS } from "@/lib/api"
import { useCluster } from "@/hooks/useCluster"
import Image from "next/image"

// 集群类型定义
interface ClusterManager {
  id: string;
  username: string;
}

interface ClusterItem {
  id: string;
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
  const router = useRouter()
  
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
      <Card className={`${CARD_STYLES.rounded} group relative overflow-hidden ${colors.shadowColor} shadow-xl hover:shadow-2xl transition-all duration-700 hover:-translate-y-3 animate-scale-in h-[560px]`}>
        {/* 主背景渐变 */}
        <div className={`absolute inset-0 bg-gradient-to-br ${colors.bgGradient}`} />
        
        {/* 动态光效背景 */}
        <div className={`absolute inset-0 bg-gradient-to-br ${colors.glowColor} opacity-0 group-hover:opacity-100 transition-opacity duration-1000`} />
        
        {/* 装饰性光效 - 多层设计 */}
        <div className="absolute top-0 right-0 w-40 h-40 bg-gradient-to-br from-white/30 to-transparent rounded-full blur-3xl transform translate-x-20 -translate-y-20 group-hover:scale-150 transition-transform duration-1000" />
        <div className="absolute bottom-0 left-0 w-32 h-32 bg-gradient-to-tr from-white/20 to-transparent rounded-full blur-2xl transform -translate-x-16 translate-y-16 group-hover:scale-125 transition-transform duration-1000" />
        
        {/* 边框光效 */}
        <div className={`absolute inset-0 rounded-3xl bg-gradient-to-r ${colors.gradient} opacity-0 group-hover:opacity-20 transition-opacity duration-500 blur-sm`} />

        <CardContent className={`${CARD_STYLES.content} relative z-10 h-full flex flex-col`}>
          {/* 头部信息 - 增强设计 */}
          <div className="flex items-start justify-between mb-6">
            <div className="flex items-center space-x-5">
              {/* 图标容器 - 3D效果 */}
              <div className="relative perspective-1000">
                <div className={`relative p-4 rounded-3xl bg-gradient-to-br ${colors.gradient} shadow-2xl group-hover:scale-110 transition-all duration-500 preserve-3d`}>
                  <Image 
                    src={iconPath}
                    alt={ClusterTypeUtil.getLabel(ClusterTypeUtil.fromString(cluster.depType || 'PVM'))}
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
                <Badge className={`${BADGE_STYLES.base} ${colors.badgeColor} shadow-lg group-hover:scale-105 transition-transform duration-300`}>
                  {ClusterTypeUtil.getLabel(ClusterTypeUtil.fromString(cluster.depType || 'PVM'))}
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
              className={`${BUTTON_STYLES.next} w-full h-12 font-semibold text-lg group/btn relative overflow-hidden ${
                cluster.clusterStateCode && cluster.clusterStateCode <= 4
                  ? `${BUTTON_STYLES.nextEnabled} bg-gradient-to-r ${colors.gradient} hover:shadow-2xl hover:scale-105`
                  : `${BUTTON_STYLES.nextDisabled} cursor-not-allowed`
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
            {ClusterTypeUtil.isKubernetes(cluster.depType || '') ? (
              // Kubernetes 集群显示 3 个按钮
              <div className="grid grid-cols-3 gap-2">
                <Button
                  variant="secondary"
                  onClick={() => onSetup(cluster)}
                  className={`${BUTTON_STYLES.previous} h-11 hover:shadow-lg hover:scale-105 backdrop-blur-sm`}
                >
                  <Settings className="mr-1 h-4 w-4" />
                  配置
                </Button>
                <Button
                  variant="secondary"
                  onClick={() => {
                    setAuthDialogOpen(true);
                    onAuth(cluster);
                  }}
                  className={`${BUTTON_STYLES.previous} h-11 hover:shadow-lg hover:scale-105 backdrop-blur-sm`}
                >
                  <Shield className="mr-1 h-4 w-4" />
                  授权
                </Button>
                <Button
                  variant="secondary"
                  onClick={() => {
                    router.push(`/kubernetes-dashboard?clusterId=${cluster.id}&clusterName=${encodeURIComponent(cluster.clusterName)}`);
                  }}
                  className={`${BUTTON_STYLES.previous} h-11 hover:shadow-lg hover:scale-105 backdrop-blur-sm bg-blue-50/80 hover:bg-blue-100 text-blue-700 border-blue-200`}
                >
                  <Grid3X3 className="mr-1 h-4 w-4" />
                  K8s
                </Button>
              </div>
            ) : (
              // 非 Kubernetes 集群显示 2 个按钮
              <div className="grid grid-cols-2 gap-3">
                <Button
                  variant="secondary"
                  onClick={() => onSetup(cluster)}
                  className={`${BUTTON_STYLES.previous} h-11 hover:shadow-lg hover:scale-105 backdrop-blur-sm`}
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
                  className={`${BUTTON_STYLES.previous} h-11 hover:shadow-lg hover:scale-105 backdrop-blur-sm`}
                >
                  <Shield className="mr-2 h-4 w-4" />
                  用户授权
                </Button>
              </div>
            )}

            {/* 更多操作 */}
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button
                  variant="secondary"
                  className={`${BUTTON_STYLES.previous} w-full h-10 bg-gray-50/80 hover:bg-gray-100 hover:shadow-lg text-gray-600`}
                >
                  <MoreHorizontal className="mr-2 h-4 w-4" />
                  更多操作
                  <ChevronRight className="ml-2 h-4 w-4 group-hover:translate-x-1 transition-transform" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent className={`w-56 ${DIALOG_STYLES.content} backdrop-blur-xl p-2`}>
                <DropdownMenuItem 
                  className="rounded-xl m-1 hover:bg-gray-50 p-3 transition-all duration-200"
                  onClick={() => onEdit(cluster)}
                >
                  <Edit className="mr-3 h-4 w-4" />
                  编辑集群配置
                </DropdownMenuItem>

                <DropdownMenuSeparator className="my-2" />
                <DropdownMenuItem 
                  className="rounded-xl m-1 text-red-600 hover:bg-red-50 p-3 transition-all duration-200"
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
      className={`${CARD_STYLES.rounded} group relative overflow-hidden shadow-xl hover:shadow-2xl transition-all duration-700 hover:-translate-y-3 cursor-pointer animate-scale-in h-[560px]`}
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

        <CardContent className={`${CARD_STYLES.content} relative h-full flex flex-col justify-between items-center text-center z-10`}>
          {/* 主图标 - 增强3D效果 */}
          <div className="relative mb-6 perspective-1000">
            <div className="w-24 h-24 rounded-3xl bg-gradient-to-br from-gray-100 to-gray-200 flex items-center justify-center shadow-2xl group-hover:scale-110 group-hover:rotate-6 transition-all duration-500 preserve-3d overflow-hidden">
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
                          <div className="flex items-center justify-center text-gray-600 group-hover:text-gray-800 transition-colors bg-white/60 rounded-2xl p-3 backdrop-blur-sm border border-white/30">
                <Rocket className="h-5 w-5 mr-3 text-indigo-500" />
                <span className="font-medium">快速部署全新环境</span>
              </div>
              <div className="flex items-center justify-center text-gray-600 group-hover:text-gray-800 transition-colors bg-white/60 rounded-2xl p-3 backdrop-blur-sm border border-white/30">
                <Brain className="h-5 w-5 mr-3 text-purple-500" />
                <span className="font-medium">AI智能配置优化</span>
              </div>
              <div className="flex items-center justify-center text-gray-600 group-hover:text-gray-800 transition-colors bg-white/60 rounded-2xl p-3 backdrop-blur-sm border border-white/30">
                <Zap className="h-5 w-5 mr-3 text-pink-500" />
                <span className="font-medium">高性能计算集群</span>
              </div>
                      </div>
          </div>

          {/* 底部区域 */}
          <div className="space-y-4">
            {/* 创建按钮 - 增强样式 */}
            <Button className={`${BUTTON_STYLES.next} ${BUTTON_STYLES.nextEnabled} w-full h-12 bg-gradient-to-r from-indigo-500 via-purple-600 to-pink-500 hover:from-indigo-600 hover:via-purple-700 hover:to-pink-600 text-white shadow-2xl hover:shadow-3xl transition-all duration-500 group-hover:scale-105 font-semibold text-lg relative overflow-hidden group/btn`}>
            {/* 按钮光效 */}
            <div className="absolute inset-0 bg-gradient-to-r from-white/0 via-white/25 to-white/0 translate-x-[-100%] group-hover/btn:translate-x-[100%] transition-transform duration-1000" />
                          <Plus className="mr-3 h-5 w-5 relative z-10" />
              <span className="relative z-10">立即创建集群</span>
              <ChevronRight className="ml-3 h-5 w-5 group-hover:translate-x-1 transition-transform relative z-10" />
          </Button>

                      {/* 底部提示 */}
            <div className="mt-6 flex items-center justify-center space-x-4">
              <Badge variant="outline" className={`${BADGE_STYLES.base} bg-white/80 border-indigo-200 text-indigo-700`}>
                <Server className="h-3 w-3 mr-1" />
                Kubernetes
              </Badge>
              <Badge variant="outline" className={`${BADGE_STYLES.base} bg-white/80 border-emerald-200 text-emerald-700`}>
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
  const [hostValidationDialogOpen, setHostValidationDialogOpen] = useState(false);
  const [agentDeploymentDialogOpen, setAgentDeploymentDialogOpen] = useState(false);
  const [serviceSelectionDialogOpen, setServiceSelectionDialogOpen] = useState(false);
  const [masterRoleAssignDialogOpen, setMasterRoleAssignDialogOpen] = useState(false);
  const [workerRoleAssignDialogOpen, setWorkerRoleAssignDialogOpen] = useState(false);
  const [serviceConfigDialogOpen, setServiceConfigDialogOpen] = useState(false);
  const [serviceInstallDialogOpen, setServiceInstallDialogOpen] = useState(false);
  const [editingCluster, setEditingCluster] = useState<ClusterItem | null>(null);
  const [setupCluster, setSetupCluster] = useState<ClusterItem | null>(null);
  // K8S专用状态
  const [k8sStep1Data, setK8sStep1Data] = useState<K8sStep1Data | null>(null);
  // PVM专用状态
  const [pvmStep1Data, setPvmStep1Data] = useState<PvmStep1Data | null>(null);
  // 通用状态
  const [hostValidationData, setHostValidationData] = useState<Record<string, unknown> | null>(null);
  const [agentDeploymentData, setAgentDeploymentData] = useState<Step3AgentData | null>(null);
  const [serviceSelectionData, setServiceSelectionData] = useState<Step3Data | null>(null);
  const [, setMasterRoleAssignData] = useState<Step5Data | null>(null);
  const [workerRoleAssignData, setWorkerRoleAssignData] = useState<Step6Data | null>(null);
  const [serviceConfigData, setServiceConfigData] = useState<Step7Data | null>(null);
  const router = useRouter();
  const { setCluster } = useCluster();

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
    } catch (err: unknown) {
      console.error("获取集群列表出错:", err);
      setError(err instanceof Error ? err.message : "网络错误，请稍后重试");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchClusters();
  }, []);

  // 事件处理函数
  const handleEnterCluster = (cluster: ClusterItem) => {
    try {
      // 设置集群状态，与集群选择器逻辑保持一致
      setCluster({
        id: cluster.id.toString(),
        name: cluster.clusterName,
        clusterName: cluster.clusterName,
        depType: cluster.depType || 'PVM',
        isK8s: cluster.depType ? ClusterTypeUtil.isKubernetes(cluster.depType) : false,
      });
      
      // 跳转到主页
      router.push('/');
    } catch (err: unknown) {
      console.error("进入集群失败:", err);
      alert("进入集群失败，请稍后重试");
    }
  };

  const handleEditCluster = (cluster: ClusterItem) => {
    setEditingCluster(cluster);
    setEditDialogOpen(true);
  };

  const handleAuthCluster = () => {
    // 打开授权对话框
  };

  const handleDeleteCluster = (cluster: ClusterItem) => {
    if (confirm(`确定要删除集群 "${cluster.clusterName}" 吗？此操作不可撤销。`)) {
      deleteCluster(cluster.id);
    }
  };

  const deleteCluster = async (clusterId: string) => {
    try {
      const response = await apiClient.post(API_PATHS.CLUSTER_DELETE, [clusterId]);
      
      if (response.data && response.data.code === 200) {
        alert("删除集群成功");
        fetchClusters();
      } else {
        alert(response.data?.msg || "删除集群失败");
      }
    } catch (err: unknown) {
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

  // 处理K8S主机配置完成
  const handleK8sHostConfigComplete = (data: K8sStep1Data) => {
    setK8sStep1Data(data);
    setSetupDialogOpen(false);
    setHostValidationDialogOpen(true);
  };

  // 处理PVM主机配置完成
  const handlePvmHostConfigComplete = (data: PvmStep1Data) => {
    setPvmStep1Data(data);
    setSetupDialogOpen(false);
    setHostValidationDialogOpen(true);
  };

  // 处理主机校验完成  
  const handleHostValidationComplete = (hostValidationCompletionData?: Record<string, unknown>) => {
    // 主机校验完成
    
    setHostValidationData(hostValidationCompletionData || null);
    setHostValidationDialogOpen(false);
    
    // 检查集群类型，进入相应的下一步
    if (setupCluster && ClusterTypeUtil.isKubernetes(setupCluster.depType || '')) {
      setServiceSelectionDialogOpen(true);
    } else {
      // PVM模式进入Agent分发
      setAgentDeploymentDialogOpen(true);
    }
  };

  // 处理Agent分发完成
  const handleAgentDeploymentComplete = (agentDeploymentCompletionData: Step3AgentData) => {
    setAgentDeploymentData(agentDeploymentCompletionData);
    setAgentDeploymentDialogOpen(false);
    
    // PVM模式进入服务选择
    setServiceSelectionDialogOpen(true);
  };

  // 处理服务选择完成
  const handleServiceSelectionComplete = (serviceSelectionCompletionData: Step3Data) => {
    setServiceSelectionData(serviceSelectionCompletionData);
    setServiceSelectionDialogOpen(false);
    
    // 进入Master角色分配
    setMasterRoleAssignDialogOpen(true);
  };

  // 处理Master角色分配完成
  const handleMasterRoleAssignComplete = (masterRoleAssignCompletionData: Step5Data) => {
    setMasterRoleAssignDialogOpen(false);
    setMasterRoleAssignData(masterRoleAssignCompletionData);
    
    // 进入Worker角色分配
    setWorkerRoleAssignDialogOpen(true);
  };

  // 处理Worker角色分配完成
  const handleWorkerRoleAssignComplete = (workerRoleAssignCompletionData: Step6Data) => {
    setWorkerRoleAssignDialogOpen(false);
    setWorkerRoleAssignData(workerRoleAssignCompletionData);
    
    // 进入服务配置步骤
    setServiceConfigDialogOpen(true);
  };

  // 处理服务配置完成
  const handleServiceConfigComplete = (step7Data: Step7Data) => {
    setServiceConfigDialogOpen(false);
    setServiceConfigData(step7Data);
    
    // 进入服务安装步骤
    setServiceInstallDialogOpen(true);
  };

  // 处理服务安装完成
  const handleServiceInstallComplete = () => {
    setServiceInstallDialogOpen(false);
    
    // 安装完成，重置状态并刷新列表
    handleFlowComplete();
  };

  // 完成整个流程
  const handleFlowComplete = () => {
    setK8sStep1Data(null);
    setPvmStep1Data(null);
    setHostValidationData(null);
    setAgentDeploymentData(null);
    setServiceSelectionData(null);
    setMasterRoleAssignData(null);
    setWorkerRoleAssignData(null);
    setServiceConfigData(null);
    setSetupCluster(null);
    handleClusterSuccess();
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 via-blue-50/30 to-indigo-50/50 relative overflow-hidden">
      {/* 背景装饰 */}
      <div className="absolute top-0 left-0 w-96 h-96 bg-gradient-to-br from-blue-400/10 to-indigo-400/10 rounded-full blur-3xl transform -translate-x-48 -translate-y-48" />
      <div className="absolute bottom-0 right-0 w-80 h-80 bg-gradient-to-br from-purple-400/10 to-pink-400/10 rounded-full blur-3xl transform translate-x-40 translate-y-40" />

      {/* 页面头部 - 增强设计 */}
      <div className="relative overflow-hidden bg-white/80 backdrop-blur-xl border-b border-gray-200/50 shadow-lg">
        <div className="absolute inset-0 bg-gradient-to-r from-blue-50/80 via-white/90 to-purple-50/80" />
        <div className="relative w-full px-8 py-12">
          <div className="flex items-center justify-between">
            <div className="space-y-2">
              <h1 className="text-4xl font-bold bg-gradient-to-r from-gray-800 via-gray-700 to-gray-600 bg-clip-text text-transparent">
                集群管理中心
              </h1>
              <p className="text-lg text-gray-600">统一管理和监控您的大数据集群环境</p>
              <div className="flex items-center space-x-2 pt-2">
                <div className="w-2 h-2 bg-green-400 rounded-full animate-pulse" />
                <span className="text-sm text-gray-500">实时监控 • 智能管理 • 高效运维</span>
              </div>
            </div>
            
            {/* 统计信息已移至集群选择器 */}
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
                    // 保存集群ID到localStorage，以便拦截器自动注入到所有请求
                    if (typeof window !== 'undefined') {
                      localStorage.setItem('clusterId', cluster.id.toString());
                      console.log('🔧 开始配置集群，集群ID已保存到localStorage:', cluster.id);
                    }
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

      {/* 配置集群主机配置弹窗 - 根据集群类型选择组件 */}
      {setupCluster && ClusterTypeUtil.isKubernetes(setupCluster.depType || '') ? (
        <K8sHostConfigDialog
          open={setupDialogOpen}
          onOpenChange={setSetupDialogOpen}
          cluster={setupCluster ? {
            id: setupCluster.id,
            clusterName: setupCluster.clusterName,
            depType: setupCluster.depType || '',
            clusterCode: setupCluster.clusterCode || ''
          } : null}
          onStep1Complete={handleK8sHostConfigComplete}
        />
      ) : (
        <PvmHostConfigDialog
          open={setupDialogOpen}
          onOpenChange={setSetupDialogOpen}
          cluster={setupCluster ? {
            id: setupCluster.id,
            clusterName: setupCluster.clusterName,
            depType: setupCluster.depType || '',
            clusterCode: setupCluster.clusterCode || ''
          } : null}
          onStep1Complete={handlePvmHostConfigComplete}
        />
      )}

      {/* 主机校验弹窗 - 根据集群类型选择组件 */}
      {k8sStep1Data && setupCluster && ClusterTypeUtil.isKubernetes(setupCluster.depType || '') && (
        <K8sHostValidationDialog
          open={hostValidationDialogOpen}
          onOpenChange={setHostValidationDialogOpen}
          cluster={setupCluster ? {
            id: setupCluster.id,
            clusterName: setupCluster.clusterName,
            depType: setupCluster.depType || '',
            clusterCode: setupCluster.clusterCode || ''
          } : null}
          step1Data={k8sStep1Data}
          onSuccess={(data) => handleHostValidationComplete(data)}
          onPrevious={() => {
            setHostValidationDialogOpen(false);
            setSetupDialogOpen(true);
          }}
        />
      )}

      {pvmStep1Data && setupCluster && ClusterTypeUtil.isPvm(setupCluster.depType || '') && (
        <PvmHostValidationDialog
          open={hostValidationDialogOpen}
          onOpenChange={setHostValidationDialogOpen}
          cluster={setupCluster ? {
            id: setupCluster.id,
            clusterName: setupCluster.clusterName,
            depType: setupCluster.depType || '',
            clusterCode: setupCluster.clusterCode || ''
          } : null}
          step1Data={pvmStep1Data}
          onSuccess={(data) => handleHostValidationComplete(data)}
          onPrevious={() => {
            setHostValidationDialogOpen(false);
            setSetupDialogOpen(true);
          }}
        />
      )}

      {/* Agent分发弹窗 (仅PVM模式) */}
      {hostValidationData && setupCluster && ClusterTypeUtil.isPvm(setupCluster.depType || '') && (
        <AgentDeploymentDialog
          open={agentDeploymentDialogOpen}
          onOpenChange={setAgentDeploymentDialogOpen}
          cluster={setupCluster ? {
            id: setupCluster.id,
            clusterName: setupCluster.clusterName,
            depType: setupCluster.depType || '',
            clusterCode: setupCluster.clusterCode || ''
          } : null}
          clusterType={setupCluster.depType || ''}
          step2Data={hostValidationData || undefined}
          onComplete={handleAgentDeploymentComplete}
          onPrevious={() => {
            setAgentDeploymentDialogOpen(false);
            setHostValidationDialogOpen(true);
          }}
        />
      )}

      {/* 服务选择弹窗 (K8s和PVM都需要) */}
      {((k8sStep1Data && hostValidationData && ClusterTypeUtil.isKubernetes(setupCluster?.depType || '')) || 
        (agentDeploymentData && ClusterTypeUtil.isPvm(setupCluster?.depType || ''))) && setupCluster && (
        <ServiceSelectionDialog
          open={serviceSelectionDialogOpen}
          onOpenChange={setServiceSelectionDialogOpen}
          cluster={setupCluster ? {
            id: setupCluster.id,
            clusterName: setupCluster.clusterName,
            depType: setupCluster.depType || '',
            clusterCode: setupCluster.clusterCode || ''
          } : null}
          clusterType={setupCluster.depType || ''}
          step2Data={hostValidationData || undefined}
          onComplete={handleServiceSelectionComplete}
          onPrevious={() => {
            setServiceSelectionDialogOpen(false);
            if (ClusterTypeUtil.isKubernetes(setupCluster.depType || '')) {
              setHostValidationDialogOpen(true);
            } else {
              setAgentDeploymentDialogOpen(true);
            }
          }}
        />
      )}

      {/* Master角色分配弹窗 */}
      {serviceSelectionData && setupCluster && (
        <MasterRoleAssignDialog
          open={masterRoleAssignDialogOpen}
          onClose={() => setMasterRoleAssignDialogOpen(false)}
          cluster={{
            id: setupCluster.id,
            name: setupCluster.clusterName,
            clusterName: setupCluster.clusterName,
            depType: setupCluster.depType || ''
          }}
          step4Data={{
            serviceIds: serviceSelectionData.serviceIds || [],
            selectedServices: serviceSelectionData.serviceNames?.map((service: { serviceId: number; serviceName: string }) => ({
              id: service.serviceId,
              name: service.serviceName
            })) || []
          }}
          onComplete={handleMasterRoleAssignComplete}
          onPrevious={() => {
            setMasterRoleAssignDialogOpen(false);
            setServiceSelectionDialogOpen(true);
          }}
        />
      )}

      {/* Worker角色分配弹窗 */}
      {serviceSelectionData && setupCluster && (
        <WorkerRoleAssignDialog
          open={workerRoleAssignDialogOpen}
          onOpenChange={setWorkerRoleAssignDialogOpen}
          cluster={{
            id: setupCluster.id,
            clusterName: setupCluster.clusterName,
            depType: setupCluster.depType || ''
          }}
          clusterType={setupCluster.depType || ''}
          step4Data={{
            serviceIds: serviceSelectionData.serviceIds || [],
            serviceNames: serviceSelectionData.serviceNames || [],
            serviceType: serviceSelectionData.serviceType || ''
          }}
          onComplete={handleWorkerRoleAssignComplete}
          onPrevious={() => {
            setWorkerRoleAssignDialogOpen(false);
            setMasterRoleAssignDialogOpen(true);
          }}
        />
      )}

      {/* 服务配置弹窗 */}
      {workerRoleAssignData && setupCluster && serviceSelectionData && (
        <ServiceConfigDialog
          open={serviceConfigDialogOpen}
          onOpenChange={setServiceConfigDialogOpen}
          cluster={{
            id: setupCluster!.id,
            clusterName: setupCluster!.clusterName,
            depType: setupCluster!.depType || ''
          }}
          clusterType={setupCluster!.depType || ''}
          step6Data={{
            ...workerRoleAssignData,
            serviceNames: serviceSelectionData!.serviceNames || []
          }}
          onComplete={handleServiceConfigComplete}
          onPrevious={() => {
            setServiceConfigDialogOpen(false);
            setWorkerRoleAssignDialogOpen(true);
          }}
        />
      )}

      {/* 服务安装弹窗 */}
      {setupCluster && serviceConfigData && (
        <ServiceInstallDialog
          open={serviceInstallDialogOpen}
          onOpenChange={setServiceInstallDialogOpen}
          cluster={{
            id: setupCluster!.id,
            clusterName: setupCluster!.clusterName,
            depType: setupCluster!.depType || ''
          }}
          clusterType={setupCluster!.depType || ''}
          serviceConfigData={serviceConfigData}
          onComplete={handleServiceInstallComplete}
          onPrevious={() => {
            setServiceInstallDialogOpen(false);
            setServiceConfigDialogOpen(true);
          }}
        />
      )}
    </div>
  )
} 