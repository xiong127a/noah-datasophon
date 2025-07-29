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
  Terminal,
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
import { useRouter } from "next/navigation"
import axios from "axios"

// API基础URL和路径配置
const API_BASE_URL = "http://192.168.200.3:8081";
const API_PREFIX = "/ddh";

// 创建axios实例 - 注意这里不包含/api，因为下面的接口路径中已包含
const apiClient = axios.create({
  baseURL: API_BASE_URL + API_PREFIX,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 设置拦截器自动添加token
apiClient.interceptors.request.use(config => {
  if (typeof window !== 'undefined') {
    const token = localStorage.getItem('jwt_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});

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
  depType?: string;  // 使用实际值: 'Hadoop', 'Spark', 'Kubernetes', 'TensorFlow'
  clusterState: string;
  clusterStateCode: number; // 1: 未配置, 2: 运行中, 3: 异常
  createTime: string;
  clusterManagerList: ClusterManager[];
  userManageName?: string;
}

const ClusterCard = ({ cluster, onEdit, onAuth, onDelete, onEnter }: { 
  cluster: ClusterItem;
  onEdit: (cluster: ClusterItem) => void;
  onAuth: (cluster: ClusterItem) => void;
  onDelete: (cluster: ClusterItem) => void;
  onEnter: (cluster: ClusterItem) => void;
}) => {
  const getIcon = () => {
    switch (cluster.depType) {
      case "Hadoop":
        return Database;
      case "Spark":
        return Zap;
      case "Kubernetes":
        return Cloud;
      case "TensorFlow":
        return Brain;
      default:
        return Database;
    }
  }

  const Icon = getIcon();
  const isConfigured = cluster.clusterStateCode === 2; // 2: 运行中
  const isError = cluster.clusterStateCode === 3; // 3: 错误

  // 根据集群类型获取颜色
  const getClusterTypeColors = () => {
    switch (cluster.depType) {
      case "Hadoop":
        return {
          color: "from-blue-500 to-cyan-500",
          bgColor: "from-blue-50 to-cyan-50",
        };
      case "Spark":
        return {
          color: "from-orange-500 to-red-500",
          bgColor: "from-orange-50 to-red-50",
        };
      case "Kubernetes":
        return {
          color: "from-purple-500 to-pink-500",
          bgColor: "from-purple-50 to-pink-50",
        };
      case "TensorFlow":
        return {
          color: "from-green-500 to-emerald-500",
          bgColor: "from-green-50 to-emerald-50",
        };
      default:
        return {
          color: "from-blue-500 to-cyan-500",
          bgColor: "from-blue-50 to-cyan-50",
        };
    }
  }

  const { color, bgColor } = getClusterTypeColors();

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
    <Card className="group relative overflow-hidden rounded-3xl border-0 bg-white shadow-lg hover:shadow-2xl transition-all duration-500 hover:-translate-y-2">
      {/* 背景渐变 */}
      <div className={`absolute inset-0 bg-gradient-to-br ${bgColor} opacity-50`} />

      {/* 装饰性光效 */}
      <div className="absolute top-0 right-0 w-32 h-32 bg-gradient-to-br from-white/20 to-transparent rounded-full blur-2xl transform translate-x-16 -translate-y-16 group-hover:scale-150 transition-transform duration-700" />

      <CardContent className="relative p-8">
        {/* 头部信息 */}
        <div className="flex items-start justify-between mb-6">
          <div className="flex items-center space-x-4">
            <div className={`relative p-4 rounded-2xl bg-gradient-to-br ${color} shadow-lg`}>
              <Icon className="h-8 w-8 text-white" />
              <div className="absolute inset-0 rounded-2xl bg-white/20 backdrop-blur-sm" />
            </div>
            <div>
              <h3 className="text-xl font-bold text-slate-800 mb-1">{cluster.clusterName}</h3>
              <Badge variant="secondary" className="bg-white/80 text-slate-600 border-0 rounded-full px-3 py-1">
                {cluster.depType}
              </Badge>
            </div>
          </div>

          {/* 状态指示器 */}
          <div className={`w-3 h-3 rounded-full ${isConfigured ? "bg-green-400" : isError ? "bg-red-400" : "bg-orange-400"} shadow-lg`}>
            <div className={`w-3 h-3 rounded-full ${isConfigured ? "bg-green-400" : isError ? "bg-red-400" : "bg-orange-400"} animate-ping`} />
          </div>
        </div>

        {/* 详细信息 */}
        <div className="space-y-3 mb-8">
          <div className="flex items-center text-slate-600">
            <Users className="h-4 w-4 mr-3 text-slate-400" />
            <span className="text-sm">管理员: {cluster.userManageName || '未分配'}</span>
          </div>
          <div className="flex items-center text-slate-600">
            <Calendar className="h-4 w-4 mr-3 text-slate-400" />
            <span className="text-sm">创建时间: {formatDate(cluster.createTime)}</span>
          </div>
        </div>

        {/* 按钮组 */}
        <div className="space-y-3">
          {/* 进入集群按钮 - 占一行 */}
          <Button
            disabled={!isConfigured}
            className={`w-full h-12 rounded-2xl font-medium transition-all duration-300 ${
              isConfigured
                ? `bg-gradient-to-r ${color} hover:shadow-lg hover:shadow-blue-200 text-white border-0`
                : "bg-slate-100 text-slate-400 cursor-not-allowed border-0"
            }`}
            onClick={() => onEnter(cluster)}
          >
            <Play className="mr-2 h-4 w-4" />
            {isConfigured ? "进入集群" : isError ? "集群异常" : "配置中..."}
          </Button>

          {/* 其他按钮 - 第二行 */}
          <div className="flex space-x-2">
            <Button
              variant="outline"
              size="sm"
              className="flex-1 h-10 rounded-xl border-slate-200 hover:bg-slate-50 transition-all duration-200 bg-transparent"
              onClick={() => onEdit(cluster)}
            >
              <Edit className="mr-1 h-3 w-3" />
              编辑
            </Button>
            <Button
              variant="outline"
              size="sm"
              className="flex-1 h-10 rounded-xl border-slate-200 hover:bg-slate-50 transition-all duration-200 bg-transparent"
              onClick={() => onAuth(cluster)}
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
                <DropdownMenuItem 
                  className="rounded-xl m-1 hover:bg-slate-50"
                  disabled={isConfigured}
                >
                  <Settings className="mr-2 h-4 w-4" />
                  配置集群
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem 
                  className="rounded-xl m-1 text-red-600 hover:bg-red-50"
                  disabled={isConfigured}
                  onClick={() => onDelete(cluster)}
                >
                  <Trash2 className="mr-2 h-4 w-4" />
                  删除集群
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}

const CreateClusterCard = ({ onClick }: { onClick: () => void }) => {
  return (
    <Card 
      className="group relative overflow-hidden rounded-3xl border-0 bg-gradient-to-br from-slate-50 to-white shadow-lg hover:shadow-2xl transition-all duration-500 hover:-translate-y-2 cursor-pointer"
      onClick={onClick}
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
          支持 裸金属/虚拟机 • Kubernetes
        </p>
      </CardContent>
    </Card>
  )
}

export default function ClusterList() {
  const [clusters, setClusters] = useState<ClusterItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<ClusterItem | null>(null);
  const router = useRouter();

  // 获取集群列表
  const fetchClusters = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.get("/api/cluster/list");
      
      if (response.data && response.data.code === 200) {
        // 处理集群管理员名称
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
      
      // 临时：如果API调用失败，使用示例数据以便UI测试
      const mockData = [
        {
          id: 1,
          clusterName: "生产环境集群",
          depType: "Hadoop",
          clusterState: "运行中",
          clusterStateCode: 2,
          createTime: "2024-01-15",
          clusterManagerList: [{id: 1, username: "张三"}],
          userManageName: "张三"
        },
        {
          id: 2,
          clusterName: "开发测试集群",
          depType: "Spark",
          clusterState: "运行中",
          clusterStateCode: 2,
          createTime: "2024-01-20",
          clusterManagerList: [{id: 2, username: "李四"}],
          userManageName: "李四"
        },
        {
          id: 3,
          clusterName: "数据分析集群",
          depType: "Kubernetes",
          clusterState: "配置中",
          clusterStateCode: 1,
          createTime: "2024-01-25",
          clusterManagerList: [{id: 3, username: "王五"}],
          userManageName: "王五"
        },
        {
          id: 4,
          clusterName: "机器学习集群",
          depType: "TensorFlow",
          clusterState: "运行中",
          clusterStateCode: 2,
          createTime: "2024-02-01",
          clusterManagerList: [{id: 4, username: "赵六"}],
          userManageName: "赵六"
        }
      ];
      setClusters(mockData);
    } finally {
      setLoading(false);
    }
  };

  // 组件挂载时获取数据
  useEffect(() => {
    fetchClusters();
  }, []);

  // 处理进入集群
  const handleEnterCluster = async (cluster: ClusterItem) => {
    try {
      const response = await apiClient.post("/api/cluster/service/list", { clusterId: cluster.id });
      if (response.data && response.data.code === 200) {
        // 保存集群信息到localStorage
        localStorage.setItem('current_cluster_id', cluster.id.toString());
        localStorage.setItem('current_cluster_name', cluster.clusterName);
        
        // 跳转到集群详情页
        router.push(`/clusters/${cluster.id}`);
      } else {
        alert(response.data?.msg || "进入集群失败");
      }
    } catch (err) {
      console.error("进入集群失败:", err);
      alert("进入集群失败，请稍后重试");
    }
  };

  // 处理编辑集群
  const handleEditCluster = (cluster: ClusterItem) => {
    router.push(`/clusters/edit/${cluster.id}`);
  };

  // 处理授权集群
  const handleAuthCluster = (cluster: ClusterItem) => {
    router.push(`/clusters/auth/${cluster.id}`);
  };

  // 处理删除集群
  const handleDeleteCluster = (cluster: ClusterItem) => {
    setConfirmDelete(cluster);
  };

  // 确认删除集群
  const confirmDeleteCluster = async () => {
    if (!confirmDelete) return;
    
    try {
      const response = await apiClient.post("/api/cluster/delete", [confirmDelete.id]);
      
      if (response.data && response.data.code === 200) {
        alert("删除集群成功");
        fetchClusters(); // 重新加载集群列表
      } else {
        alert(response.data?.msg || "删除集群失败");
      }
    } catch (err) {
      console.error("删除集群失败:", err);
      alert("删除集群失败，请稍后重试");
    } finally {
      setConfirmDelete(null);
    }
  };

  // 创建新集群
  const handleCreateCluster = () => {
    router.push("/clusters/create");
  };

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
                {clusters.filter((c) => c.clusterStateCode === 2).length} 个集群运行中
              </Badge>
            </div>
          </div>
        </div>
      </div>

      {/* 加载状态或错误信息 */}
      {loading && (
        <div className="max-w-7xl mx-auto px-8 py-12 text-center">
          <div className="inline-block animate-spin mr-2 h-8 w-8 border-4 rounded-full border-blue-600 border-t-transparent"></div>
          <p className="text-slate-600">正在加载集群数据...</p>
        </div>
      )}

      {!loading && error && (
        <div className="max-w-7xl mx-auto px-8 py-12 text-center">
          <div className="bg-red-50 text-red-700 p-4 rounded-xl">
            <p>{error}</p>
            <Button 
              variant="outline" 
              className="mt-4"
              onClick={fetchClusters}
            >
              重试
            </Button>
          </div>
        </div>
      )}

      {/* 集群列表 */}
      {!loading && !error && (
        <div className="max-w-7xl mx-auto px-8 py-12">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-8">
            {/* 现有集群卡片 */}
            {clusters.map((cluster) => (
              <ClusterCard 
                key={cluster.id} 
                cluster={cluster} 
                onEdit={handleEditCluster}
                onAuth={handleAuthCluster}
                onDelete={handleDeleteCluster}
                onEnter={handleEnterCluster}
              />
            ))}

            {/* 创建新集群卡片 */}
            <CreateClusterCard onClick={handleCreateCluster} />
          </div>
        </div>
      )}

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
                  {clusters.filter((c) => c.clusterStateCode === 2).length}
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
                  {clusters.filter((c) => c.clusterStateCode === 1).length}
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
                <p className="text-slate-600 text-sm">异常集群</p>
                <p className="text-2xl font-bold text-red-600">
                  {clusters.filter((c) => c.clusterStateCode === 3).length}
                </p>
              </div>
              <div className="w-8 h-8 bg-red-100 rounded-full flex items-center justify-center">
                <div className="w-3 h-3 bg-red-500 rounded-full" />
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 删除确认对话框 */}
      {confirmDelete && (
        <div className="fixed inset-0 bg-black/30 backdrop-blur-sm z-50 flex items-center justify-center">
          <div className="bg-white rounded-xl shadow-xl p-8 max-w-md w-full mx-4">
            <h2 className="text-2xl font-bold mb-4">确认删除</h2>
            <p className="mb-6 text-slate-600">
              您确定要删除集群 <span className="font-semibold text-slate-800">{confirmDelete.clusterName}</span> 吗？此操作不可撤销。
            </p>
            <div className="flex justify-end space-x-4">
              <Button
                variant="outline"
                onClick={() => setConfirmDelete(null)}
              >
                取消
              </Button>
              <Button
                variant="destructive"
                onClick={confirmDeleteCluster}
              >
                确认删除
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
