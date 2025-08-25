/**
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 * @description Kubernetes Deployments管理面板
 */

"use client";

import React, { useState, useEffect, useMemo } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  Search,
  Filter,
  Download,
  RefreshCw,
  MoreHorizontal,
  Eye,
  Edit,
  Trash2,
  Play,
  Pause,
  Package,
  Users,
  CheckCircle,
  AlertCircle,
  Clock,
  Box,
  ChevronDown,
  ChevronRight,
  Activity,
  TrendingUp,
  TrendingDown
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Progress } from "@/components/ui/progress";

import { Deployment } from "../types";
import { KubernetesAPI } from '@/lib/kubernetes-api';

interface DeploymentsDashboardProps {
  clusterId: string;
  namespace: string;
  className?: string;
}

const DeploymentsDashboard: React.FC<DeploymentsDashboardProps> = ({
  clusterId,
  namespace,
  className
}) => {
  const [deployments, setDeployments] = useState<Deployment[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("all");
  const [selectedDeployment, setSelectedDeployment] = useState<Deployment | null>(null);
  const [showDetails, setShowDetails] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize] = useState(20);
  const [total, setTotal] = useState(0);

  // 获取Deployments数据
  const fetchDeployments = async () => {
    if (!clusterId) return;
    
    setLoading(true);
    setError(null);
    try {
      const response = await KubernetesAPI.getDeployments(
        clusterId,
        namespace || undefined,
        undefined, // serviceId
        pageNum,
        pageSize
      );

      // 转换API响应为组件需要的Deployment格式
      const convertedDeployments: Deployment[] = response.data.map((resource: any) => ({
        apiVersion: "apps/v1",
        kind: "Deployment",
        metadata: {
          name: resource.name,
          namespace: resource.namespace,
          creationTimestamp: resource.creationTimestamp || new Date().toISOString(),
          labels: resource.labels || {}
        },
        spec: {
          replicas: parseInt(resource.replicas?.split('/')[2]) || 1,
          selector: { matchLabels: { app: resource.name } },
          template: {
            metadata: { labels: { app: resource.name } },
            spec: {
              containers: [{
                name: resource.name,
                image: "unknown:latest"
              }]
            }
          }
        },
        status: {
          replicas: parseInt(resource.replicas?.split('/')[2]) || 1,
          readyReplicas: parseInt(resource.replicas?.split('/')[0]) || 0,
          updatedReplicas: parseInt(resource.replicas?.split('/')[1]) || 0,
          availableReplicas: parseInt(resource.available) || 0,
          unavailableReplicas: (parseInt(resource.replicas?.split('/')[2]) || 1) - (parseInt(resource.available) || 0),
          conditions: [{
            type: "Available",
            status: (parseInt(resource.available) || 0) > 0 ? "True" : "False",
            reason: "MinimumReplicasAvailable",
            message: `Deployment has ${resource.available || 0} available replica(s).`
          }]
        }
      }));

      setDeployments(convertedDeployments);
      setTotal(response.total || convertedDeployments.length);
    } catch (error) {
      console.error('获取Deployments失败:', error);
      setError(error instanceof Error ? error.message : '获取Deployments失败');
      setDeployments([]);
    } finally {
      setLoading(false);
    }
  };

  // 筛选和搜索Deployments
  const filteredDeployments = useMemo(() => {
    return deployments.filter(deployment => {
      const matchesSearch = 
        deployment.metadata.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        Object.keys(deployment.metadata.labels || {}).some(key => 
          key.toLowerCase().includes(searchTerm.toLowerCase()) ||
          (deployment.metadata.labels?.[key] || "").toLowerCase().includes(searchTerm.toLowerCase())
        );

      let matchesStatus = true;
      if (statusFilter !== "all") {
        const isHealthy = (deployment.status?.readyReplicas || 0) === (deployment.spec?.replicas || 0);
        matchesStatus = statusFilter === "healthy" ? isHealthy : !isHealthy;
      }

      return matchesSearch && matchesStatus;
    });
  }, [deployments, searchTerm, statusFilter]);

  // 统计信息
  const stats = useMemo(() => {
    return {
      total: deployments.length,
      healthy: deployments.filter(d => (d.status?.readyReplicas || 0) === (d.spec?.replicas || 0)).length,
      unhealthy: deployments.filter(d => (d.status?.readyReplicas || 0) !== (d.spec?.replicas || 0)).length,
      totalReplicas: deployments.reduce((sum, d) => sum + (d.spec?.replicas || 0), 0)
    };
  }, [deployments]);

  // 获取Deployment年龄
  const getDeploymentAge = (creationTimestamp: string) => {
    const created = new Date(creationTimestamp);
    const now = new Date();
    const diffMs = now.getTime() - created.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    const diffHours = Math.floor((diffMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
    const diffMinutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));

    if (diffDays > 0) return `${diffDays}天前`;
    if (diffHours > 0) return `${diffHours}小时前`;
    return `${diffMinutes}分钟前`;
  };

  // 获取副本状态
  const getReplicaStatus = (deployment: Deployment) => {
    const desired = deployment.spec?.replicas || 0;
    const ready = deployment.status?.readyReplicas || 0;
    const updated = deployment.status?.updatedReplicas || 0;
    const available = deployment.status?.availableReplicas || 0;
    
    return { desired, ready, updated, available };
  };

  // 获取部署状态颜色和图标
  const getDeploymentStatus = (deployment: Deployment) => {
    const { desired, ready } = getReplicaStatus(deployment);
    if (ready === desired && desired > 0) {
      return { status: "健康", color: "text-green-600", icon: CheckCircle, bgColor: "bg-green-100" };
    } else if (ready > 0) {
      return { status: "部分就绪", color: "text-yellow-600", icon: AlertCircle, bgColor: "bg-yellow-100" };
    } else {
      return { status: "未就绪", color: "text-red-600", icon: AlertCircle, bgColor: "bg-red-100" };
    }
  };

  // 刷新数据
  const handleRefresh = async () => {
    await fetchDeployments();
  };

  // Deployment操作
  const handleDeploymentAction = (action: string, deployment: Deployment) => {
    console.log(`执行操作: ${action} on Deployment: ${deployment.metadata.name}`);
    switch (action) {
      case 'view':
        setSelectedDeployment(deployment);
        setShowDetails(true);
        break;
      case 'scale':
        // 实现扩缩容逻辑
        break;
      case 'restart':
        // 实现重启逻辑
        break;
      case 'edit':
        // 实现编辑逻辑
        break;
      case 'delete':
        // 实现删除逻辑
        break;
    }
  };

  // 组件挂载和依赖更新时获取数据
  useEffect(() => {
    fetchDeployments();
  }, [clusterId, namespace, pageNum]);

  if (loading && deployments.length === 0) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex items-center space-x-2">
          <RefreshCw className="w-6 h-6 animate-spin text-blue-500" />
          <span className="text-gray-600">加载Deployments...</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex items-center space-x-2 text-red-500">
          <AlertCircle className="w-6 h-6" />
          <span>{error}</span>
          <Button 
            variant="outline" 
            size="sm" 
            onClick={handleRefresh}
            className="ml-4"
          >
            重试
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className={`space-y-6 ${className || ''}`}>
      {/* 统计卡片 */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        {[
          { title: "总计", count: stats.total, color: "blue", icon: Box },
          { title: "健康", count: stats.healthy, color: "green", icon: CheckCircle },
          { title: "异常", count: stats.unhealthy, color: "red", icon: AlertCircle },
          { title: "总副本", count: stats.totalReplicas, color: "purple", icon: Users }
        ].map((stat, index) => (
          <motion.div
            key={stat.title}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: index * 0.1 }}
          >
            <Card>
              <CardContent className="p-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-600">{stat.title}</p>
                    <p className="text-2xl font-bold text-gray-900">{stat.count}</p>
                  </div>
                  <div className={`p-3 rounded-full bg-${stat.color}-100`}>
                    <stat.icon className={`w-6 h-6 text-${stat.color}-600`} />
                  </div>
                </div>
              </CardContent>
            </Card>
          </motion.div>
        ))}
      </div>

      {/* 工具栏 */}
      <Card>
        <CardHeader className="pb-4">
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="text-lg">Deployments</CardTitle>
              <CardDescription>管理Kubernetes部署</CardDescription>
            </div>
            <div className="flex items-center space-x-2">
              <Button
                variant="outline"
                size="sm"
                onClick={handleRefresh}
                disabled={loading}
              >
                <RefreshCw className={`w-4 h-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
                刷新
              </Button>
              <Button size="sm">
                <Package className="w-4 h-4 mr-2" />
                新建Deployment
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* 搜索和过滤 */}
          <div className="flex items-center space-x-4">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
              <Input
                placeholder="搜索Deployments..."
                className="pl-10"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger className="w-40">
                <SelectValue placeholder="选择状态" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">所有状态</SelectItem>
                <SelectItem value="healthy">健康</SelectItem>
                <SelectItem value="unhealthy">异常</SelectItem>
              </SelectContent>
            </Select>
            <Button variant="outline" size="sm">
              <Download className="w-4 h-4 mr-2" />
              导出
            </Button>
          </div>

          {/* Deployments表格 */}
          <div className="border rounded-lg">
            <Table>
              <TableHeader>
                <TableRow className="bg-gray-50">
                  <TableHead>名称</TableHead>
                  <TableHead>副本状态</TableHead>
                  <TableHead>就绪情况</TableHead>
                  <TableHead>状态</TableHead>
                  <TableHead>年龄</TableHead>
                  <TableHead>操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <AnimatePresence>
                  {filteredDeployments.map((deployment, index) => {
                    const { desired, ready, updated, available } = getReplicaStatus(deployment);
                    const statusInfo = getDeploymentStatus(deployment);
                    const readyPercent = desired > 0 ? Math.round((ready / desired) * 100) : 0;
                    
                    return (
                      <motion.tr
                        key={deployment.metadata.name}
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ delay: index * 0.05 }}
                        className="hover:bg-gray-50 transition-colors duration-200"
                      >
                        <TableCell>
                          <div className="flex items-center space-x-3">
                            <div className="w-8 h-8 bg-blue-100 rounded-lg flex items-center justify-center">
                              <Package className="w-4 h-4 text-blue-600" />
                            </div>
                            <div>
                              <div className="font-medium text-gray-900">{deployment.metadata.name}</div>
                              <div className="text-sm text-gray-500">{deployment.metadata.namespace}</div>
                            </div>
                          </div>
                        </TableCell>
                        <TableCell>
                          <div className="space-y-1">
                            <div className="text-sm font-medium">{ready}/{desired}</div>
                            <Progress value={readyPercent} className="w-20 h-2" />
                          </div>
                        </TableCell>
                        <TableCell>
                          <div className="text-sm space-y-1">
                            <div>Updated: {updated}</div>
                            <div>Available: {available}</div>
                          </div>
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center space-x-2">
                            <statusInfo.icon className={`w-4 h-4 ${statusInfo.color}`} />
                            <span className="text-sm">{statusInfo.status}</span>
                          </div>
                        </TableCell>
                        <TableCell>
                          <span className="text-sm text-gray-600">
                            {getDeploymentAge(deployment.metadata.creationTimestamp)}
                          </span>
                        </TableCell>
                        <TableCell>
                          <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                              <Button variant="ghost" size="sm">
                                <MoreHorizontal className="w-4 h-4" />
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                              <DropdownMenuItem onClick={() => handleDeploymentAction('view', deployment)}>
                                <Eye className="w-4 h-4 mr-2" />
                                查看详情
                              </DropdownMenuItem>
                              <DropdownMenuItem onClick={() => handleDeploymentAction('scale', deployment)}>
                                <TrendingUp className="w-4 h-4 mr-2" />
                                扩缩容
                              </DropdownMenuItem>
                              <DropdownMenuItem onClick={() => handleDeploymentAction('restart', deployment)}>
                                <RefreshCw className="w-4 h-4 mr-2" />
                                重启
                              </DropdownMenuItem>
                              <DropdownMenuItem onClick={() => handleDeploymentAction('edit', deployment)}>
                                <Edit className="w-4 h-4 mr-2" />
                                编辑
                              </DropdownMenuItem>
                              <DropdownMenuSeparator />
                              <DropdownMenuItem 
                                onClick={() => handleDeploymentAction('delete', deployment)}
                                className="text-red-600"
                              >
                                <Trash2 className="w-4 h-4 mr-2" />
                                删除
                              </DropdownMenuItem>
                            </DropdownMenuContent>
                          </DropdownMenu>
                        </TableCell>
                      </motion.tr>
                    );
                  })}
                </AnimatePresence>
              </TableBody>
            </Table>
          </div>

          {filteredDeployments.length === 0 && !loading && (
            <div className="text-center py-8">
              <Package className="w-12 h-12 text-gray-400 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">暂无Deployments</h3>
              <p className="text-gray-500 mb-4">
                {searchTerm ? '没有找到匹配的Deployments' : '当前命名空间中没有Deployments'}
              </p>
              {!searchTerm && (
                <Button>
                  <Package className="w-4 h-4 mr-2" />
                  创建第一个Deployment
                </Button>
              )}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default DeploymentsDashboard;
