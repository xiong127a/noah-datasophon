/**
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 * @description Kubernetes ReplicaSets管理面板
 */

"use client";

import React, { useState, useEffect, useMemo, useCallback } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  Search,
  RefreshCw,
  Eye,
  Server,
  Users,
  CheckCircle,
  AlertCircle,
  Box
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

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

import { Progress } from "@/components/ui/progress";

import type { KubernetesResource } from "../types";
import { KubernetesAPI } from '@/lib/kubernetes-api';
import KubernetesPagination from "../components/kubernetes-pagination";

// ReplicaSet类型定义
interface ReplicaSet extends KubernetesResource {
  kind: 'ReplicaSet';
  spec: {
    replicas: number;
    selector: {
      matchLabels: Record<string, string>;
    };
    template: {
      metadata: {
        labels: Record<string, string>;
      };
      spec: {
        containers: Array<{
          name: string;
          image: string;
        }>;
      };
    };
  };
  status?: {
    replicas?: number;
    fullyLabeledReplicas?: number;
    readyReplicas?: number;
    availableReplicas?: number;
  };
}

interface ReplicaSetsDashboardProps {
  clusterId: string;
  serviceId?: string;
  namespace: string;
  className?: string;
}

// API响应数据的临时接口
interface ReplicaSetApiResource {
  name: string;
  namespace: string;
  creationTimestamp?: string;
  labels?: Record<string, string>;
  replicas?: string;
  available?: string;
  [key: string]: unknown;
}

interface ApiResponse {
  data: ReplicaSetApiResource[] | { data: ReplicaSetApiResource[]; total?: string | number };
  total?: string | number;
  [key: string]: unknown;
}

const ReplicaSetsDashboard: React.FC<ReplicaSetsDashboardProps> = ({
  clusterId,
  serviceId,
  namespace,
  className
}) => {
  const [replicaSets, setReplicaSets] = useState<ReplicaSet[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("all");
  const [error, setError] = useState<string | null>(null);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  // 获取ReplicaSets数据
  const fetchReplicaSets = useCallback(async () => {
    if (!clusterId) return;
    
    setLoading(true);
    setError(null);
    try {
      console.log('📡 调用 KubernetesAPI.getReplicaSets API...');
      console.log('🔍 ReplicaSets 调用参数:', { 
        clusterId, 
        namespace: namespace || undefined, 
        serviceId: serviceId || undefined, 
        pageNum, 
        pageSize 
      });
      const response = await KubernetesAPI.getReplicaSets(
        clusterId,
        namespace || undefined,
        serviceId || undefined,
        pageNum,
        pageSize
      );
      console.log('✅ 获取ReplicaSets成功，数据结构:', response);
      console.log('✅ 获取ReplicaSets成功，数量:', response.data?.length);
      console.log('✅ 实际数据数组:', response.data);
      
      // 检查数据结构并提取实际的数组
      const apiResponse = response as unknown as ApiResponse;
      const dataArray = Array.isArray(apiResponse.data) 
        ? apiResponse.data 
        : (apiResponse.data as { data: ReplicaSetApiResource[]; total?: string | number })?.data || [];
      console.log('✅ 使用的数据数组:', dataArray, '长度:', dataArray.length);

      // 转换API响应为组件需要的ReplicaSet格式
      const convertedReplicaSets: ReplicaSet[] = dataArray.map((resource: ReplicaSetApiResource) => ({
        apiVersion: "apps/v1",
        kind: "ReplicaSet",
        metadata: {
          name: resource.name,
          namespace: resource.namespace,
          creationTimestamp: resource.creationTimestamp || new Date().toISOString(),
          labels: resource.labels || {}
        },
        spec: {
          replicas: parseInt((resource.replicas as string)?.split('/')[2]) || 1,
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
          replicas: parseInt((resource.replicas as string)?.split('/')[2]) || 1,
          readyReplicas: parseInt((resource.replicas as string)?.split('/')[0]) || 0,
          fullyLabeledReplicas: parseInt((resource.replicas as string)?.split('/')[1]) || 0,
          availableReplicas: parseInt(resource.available as string) || 0
        }
      }));

      setReplicaSets(convertedReplicaSets);
      
      // 使用正确的总数：优先使用API返回的total，其次使用数据长度
      const nestedData = !Array.isArray(apiResponse.data) ? apiResponse.data as { data: ReplicaSetApiResource[]; total?: string | number } : null;
      const totalCount = apiResponse.total || nestedData?.total || convertedReplicaSets.length;
      console.log('✅ 设置总数:', totalCount, '来源:', { responseTotal: apiResponse.total, dataTotal: nestedData?.total, arrayLength: convertedReplicaSets.length });
      setTotal(typeof totalCount === 'string' ? parseInt(totalCount) : totalCount);
    } catch (error) {
      console.error('获取ReplicaSets失败:', error);
      setError(error instanceof Error ? error.message : '获取ReplicaSets失败');
      setReplicaSets([]);
    } finally {
      setLoading(false);
    }
  }, [clusterId, serviceId, namespace, pageNum, pageSize]);

  // 筛选和搜索ReplicaSets
  const filteredReplicaSets = useMemo(() => {
    return replicaSets.filter(replicaSet => {
      const matchesSearch = 
        replicaSet.metadata.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        Object.keys(replicaSet.metadata.labels || {}).some(key => 
          key.toLowerCase().includes(searchTerm.toLowerCase()) ||
          (replicaSet.metadata.labels?.[key] || "").toLowerCase().includes(searchTerm.toLowerCase())
        );

      let matchesStatus = true;
      if (statusFilter !== "all") {
        const isHealthy = (replicaSet.status?.readyReplicas || 0) === (replicaSet.spec?.replicas || 0);
        matchesStatus = statusFilter === "healthy" ? isHealthy : !isHealthy;
      }

      return matchesSearch && matchesStatus;
    });
  }, [replicaSets, searchTerm, statusFilter]);

  // 刷新数据
  const handleRefresh = async () => {
    await fetchReplicaSets();
  };

  // 分页处理函数
  const handlePageChange = (page: number) => {
    setPageNum(page);
  };

  const handlePageSizeChange = (size: number) => {
    setPageSize(size);
    setPageNum(1); // 重置到第一页
  };

  // 组件挂载和依赖更新时获取数据
  useEffect(() => {
    fetchReplicaSets();
  }, [fetchReplicaSets]);

  // 统计信息
  const stats = useMemo(() => {
    return {
      total: replicaSets.length,
      healthy: replicaSets.filter(r => (r.status?.readyReplicas || 0) === (r.spec?.replicas || 0)).length,
      unhealthy: replicaSets.filter(r => (r.status?.readyReplicas || 0) !== (r.spec?.replicas || 0)).length,
      totalReplicas: replicaSets.reduce((sum, r) => sum + (r.spec?.replicas || 0), 0)
    };
  }, [replicaSets]);

  // 获取ReplicaSet年龄
  const getReplicaSetAge = (creationTimestamp: string) => {
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
  const getReplicaStatus = (replicaSet: ReplicaSet) => {
    const desired = replicaSet.spec?.replicas || 0;
    const ready = replicaSet.status?.readyReplicas || 0;
    const fullyLabeled = replicaSet.status?.fullyLabeledReplicas || 0;
    const available = replicaSet.status?.availableReplicas || 0;
    
    return { desired, ready, fullyLabeled, available };
  };

  // 获取ReplicaSet状态颜色和图标
  const getReplicaSetStatus = (replicaSet: ReplicaSet) => {
    const { desired, ready } = getReplicaStatus(replicaSet);
    if (ready === desired && desired > 0) {
      return { status: "健康", color: "text-green-600", icon: CheckCircle, bgColor: "bg-green-100" };
    } else if (ready > 0) {
      return { status: "部分就绪", color: "text-yellow-600", icon: AlertCircle, bgColor: "bg-yellow-100" };
    } else {
      return { status: "未就绪", color: "text-red-600", icon: AlertCircle, bgColor: "bg-red-100" };
    }
  };

  // ReplicaSet操作
  const handleReplicaSetAction = (action: string, replicaSet: ReplicaSet) => {
    console.log(`执行操作: ${action} on ReplicaSet: ${replicaSet.metadata.name}`);
    switch (action) {
      case 'view':
        console.log('查看ReplicaSet详情:', replicaSet.metadata.name);
        break;
      default:
        break;
    }
  };

  if (loading && replicaSets.length === 0) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex items-center space-x-2">
          <RefreshCw className="w-6 h-6 animate-spin text-blue-500" />
          <span className="text-gray-600">加载ReplicaSets...</span>
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
          { title: "总副本", count: stats.totalReplicas, color: "cyan", icon: Users }
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
            <CardTitle className="text-lg font-semibold">ReplicaSets 列表</CardTitle>
            <div className="flex items-center space-x-3">
              {/* 搜索框 */}
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
                <Input
                  placeholder="搜索 ReplicaSets..."
                  className="pl-10 w-64"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>

              {/* 状态筛选 */}
              <Select value={statusFilter} onValueChange={setStatusFilter}>
                <SelectTrigger className="w-32">
                  <SelectValue placeholder="状态筛选" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">全部</SelectItem>
                  <SelectItem value="healthy">健康</SelectItem>
                  <SelectItem value="unhealthy">异常</SelectItem>
                </SelectContent>
              </Select>

              {/* 刷新按钮 */}
              <Button variant="outline" size="icon" onClick={handleRefresh} disabled={loading}>
                <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
              </Button>
            </div>
          </div>
        </CardHeader>

        <CardContent className="p-0">

          {/* ReplicaSets表格 */}
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
                  {filteredReplicaSets.map((replicaSet, index) => {
                    const { desired, ready, fullyLabeled, available } = getReplicaStatus(replicaSet);
                    const statusInfo = getReplicaSetStatus(replicaSet);
                    const readyPercent = desired > 0 ? Math.round((ready / desired) * 100) : 0;
                    
                    return (
                      <motion.tr
                        key={replicaSet.metadata.name}
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ delay: index * 0.05 }}
                        className="hover:bg-gray-50 transition-colors duration-200"
                      >
                        <TableCell>
                          <div className="flex items-center space-x-3">
                            <div className="w-8 h-8 bg-cyan-100 rounded-lg flex items-center justify-center">
                              <Server className="w-4 h-4 text-cyan-600" />
                            </div>
                            <div>
                              <div className="font-medium text-gray-900">{replicaSet.metadata.name}</div>
                              <div className="text-sm text-gray-500">{replicaSet.metadata.namespace}</div>
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
                            <div>FullyLabeled: {fullyLabeled}</div>
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
                            {getReplicaSetAge(replicaSet.metadata.creationTimestamp)}
                          </span>
                        </TableCell>
                        <TableCell>
                          <Button 
                            variant="ghost" 
                            size="sm" 
                            onClick={() => handleReplicaSetAction('view', replicaSet)}
                            className="hover:bg-blue-50"
                          >
                            <Eye className="w-4 h-4 mr-1" />
                            查看
                          </Button>
                        </TableCell>
                      </motion.tr>
                    );
                  })}
                </AnimatePresence>
              </TableBody>
            </Table>
          </div>

          {filteredReplicaSets.length === 0 && !loading && (
            <div className="text-center py-12">
              <Server className="w-16 h-16 text-gray-300 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">暂无ReplicaSets</h3>
              <p className="text-gray-500">
                {searchTerm || statusFilter !== "all" 
                  ? '没有找到匹配的ReplicaSets' 
                  : '当前命名空间中没有ReplicaSets资源'
                }
              </p>
            </div>
          )}
        </CardContent>

        {/* 分页控件 */}
        {total > 0 && (
          <div className="p-4 border-t border-gray-100 bg-gray-50/50">
            <KubernetesPagination
              currentPage={pageNum}
              pageSize={pageSize}
              total={total}
              onPageChange={handlePageChange}
              onPageSizeChange={handlePageSizeChange}
              loading={loading}
              className="!bg-transparent !border-0 !shadow-none !p-0"
            />
          </div>
        )}
      </Card>
    </div>
  );
};

export default ReplicaSetsDashboard;