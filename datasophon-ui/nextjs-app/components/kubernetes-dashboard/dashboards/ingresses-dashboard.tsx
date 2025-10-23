/**
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 * @description Kubernetes Ingresses管理面板
 */

"use client";

import React, { useState, useEffect, useMemo, useCallback } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  Search,
  RefreshCw,

  Settings,
  Globe,
  CheckCircle,
  AlertCircle,
  Box,
  Link
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

import { Badge } from "@/components/ui/badge";

import { Ingress } from "../types";
import { KubernetesAPI } from '@/lib/kubernetes-api';
import KubernetesPagination from "../components/kubernetes-pagination";

interface IngressesDashboardProps {
  clusterId: string;
  serviceId?: string;
  namespace: string;
  className?: string;
}

// API响应数据的临时接口
interface IngressApiResource {
  name: string;
  namespace: string;
  creationTimestamp?: string;
  labels?: Record<string, string>;
  annotations?: Record<string, string>;
  [key: string]: unknown;
}

interface ApiResponse {
  data: IngressApiResource[] | { data: IngressApiResource[]; total?: string | number };
  total?: string | number;
  [key: string]: unknown;
}

const IngressesDashboard: React.FC<IngressesDashboardProps> = ({
  clusterId,

  namespace,
  className
}) => {
  const [ingresses, setIngresses] = useState<Ingress[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("all");
  const [error, setError] = useState<string | null>(null);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  // 获取Ingresses数据
  const fetchIngresses = useCallback(async () => {
    if (!clusterId) return;
    
    setLoading(true);
    setError(null);
    try {
      const response = await KubernetesAPI.getIngresses(
        clusterId,
        namespace || undefined,
        pageNum,
        pageSize
      );
      
      // 检查数据结构并提取实际的数组
      const apiResponse = response as unknown as ApiResponse;
      const dataArray = Array.isArray(apiResponse.data) 
        ? apiResponse.data 
        : (apiResponse.data as { data: IngressApiResource[]; total?: string | number })?.data || [];


      // 转换API响应为组件需要的Ingress格式
      const convertedIngresses: Ingress[] = dataArray.map((resource: IngressApiResource) => ({
        apiVersion: "networking.k8s.io/v1",
        kind: "Ingress",
        metadata: {
          name: resource.name,
          namespace: resource.namespace,
          creationTimestamp: resource.creationTimestamp || new Date().toISOString(),
          labels: resource.labels || {},
          annotations: resource.annotations || {}
        },
        spec: {
          rules: [{
            host: "example.com",
            http: {
              paths: [{
                path: "/",
                pathType: "Prefix",
                backend: {
                  service: {
                    name: "default-service",
                    port: {
                      number: 80
                    }
                  }
                }
              }]
            }
          }]
        },
        status: {
          loadBalancer: {
            ingress: []
          }
        }
      }));

      setIngresses(convertedIngresses);
      
      // 使用正确的总数：优先使用API返回的total，其次使用数据长度
      const nestedData = !Array.isArray(apiResponse.data) ? apiResponse.data as { data: IngressApiResource[]; total?: string | number } : null;
      const totalCount = apiResponse.total || nestedData?.total || convertedIngresses.length;

      setTotal(typeof totalCount === 'string' ? parseInt(totalCount) : totalCount);
    } catch (error) {
      console.error('获取Ingresses失败:', error);
      setError(error instanceof Error ? error.message : '获取Ingresses失败');
      setIngresses([]);
    } finally {
      setLoading(false);
    }
  }, [clusterId, namespace, pageNum, pageSize]);

  // 筛选和搜索Ingresses
  const filteredIngresses = useMemo(() => {
    return ingresses.filter(ingress => {
      const matchesSearch = 
        ingress.metadata.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        Object.keys(ingress.metadata.labels || {}).some(key => 
          key.toLowerCase().includes(searchTerm.toLowerCase()) ||
          (ingress.metadata.labels?.[key] || "").toLowerCase().includes(searchTerm.toLowerCase())
        ) ||
        ingress.spec.rules?.some(rule => 
          rule.host?.toLowerCase().includes(searchTerm.toLowerCase())
        );

      let matchesStatus = true;
      if (statusFilter !== "all") {
        const hasLoadBalancer = (ingress.status?.loadBalancer?.ingress?.length || 0) > 0;
        matchesStatus = statusFilter === "active" ? hasLoadBalancer : !hasLoadBalancer;
      }

      return matchesSearch && matchesStatus;
    });
  }, [ingresses, searchTerm, statusFilter]);

  // 刷新数据
  const handleRefresh = async () => {
    await fetchIngresses();
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
    fetchIngresses();
  }, [fetchIngresses]);

  // 统计信息
  const stats = useMemo(() => {
    return {
      total: ingresses.length,
      active: ingresses.filter(i => (i.status?.loadBalancer?.ingress?.length || 0) > 0).length,
      inactive: ingresses.filter(i => (i.status?.loadBalancer?.ingress?.length || 0) === 0).length,
      totalRules: ingresses.reduce((sum, i) => sum + (i.spec.rules?.length || 0), 0)
    };
  }, [ingresses]);

  // 获取Ingress年龄
  const getIngressAge = (creationTimestamp: string) => {
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

  // 获取Ingress状态
  const getIngressStatus = (ingress: Ingress) => {
    const hasLoadBalancer = (ingress.status?.loadBalancer?.ingress?.length || 0) > 0;
    if (hasLoadBalancer) {
      return { status: "活跃", color: "text-green-600", icon: CheckCircle, bgColor: "bg-green-100" };
    } else {
      return { status: "待分配", color: "text-yellow-600", icon: AlertCircle, bgColor: "bg-yellow-100" };
    }
  };

  // 获取Ingress主机列表
  const getIngressHosts = (ingress: Ingress) => {
    return ingress.spec.rules?.map(rule => rule.host).filter(Boolean) || ["未设置"];
  };



  if (loading && ingresses.length === 0) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex items-center space-x-2">
          <RefreshCw className="w-6 h-6 animate-spin text-blue-500" />
          <span className="text-gray-600">加载Ingresses...</span>
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
          { title: "活跃", count: stats.active, color: "green", icon: CheckCircle },
          { title: "待分配", count: stats.inactive, color: "yellow", icon: AlertCircle },
          { title: "规则数", count: stats.totalRules, color: "purple", icon: Settings }
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
            <CardTitle className="text-lg font-semibold">Ingresses 列表</CardTitle>
            <div className="flex items-center space-x-3">
              {/* 搜索框 */}
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
                <Input
                  placeholder="搜索 Ingresses..."
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
                  <SelectItem value="active">活跃</SelectItem>
                  <SelectItem value="inactive">待分配</SelectItem>
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

          {/* Ingresses表格 */}
          <div className="border rounded-lg">
            <Table>
              <TableHeader>
                <TableRow className="bg-gray-50">
                  <TableHead>名称</TableHead>
                  <TableHead>主机</TableHead>
                  <TableHead>规则数</TableHead>
                  <TableHead>状态</TableHead>
                  <TableHead>年龄</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <AnimatePresence>
                  {filteredIngresses.map((ingress, index) => {
                    const hosts = getIngressHosts(ingress);
                    const rulesCount = ingress.spec.rules?.length || 0;
                    const statusInfo = getIngressStatus(ingress);
                    
                    return (
                      <motion.tr
                        key={ingress.metadata.name}
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ delay: index * 0.05 }}
                        className="hover:bg-gray-50 transition-colors duration-200"
                      >
                        <TableCell>
                          <div className="flex items-center space-x-3">
                            <div className="w-8 h-8 bg-purple-100 rounded-lg flex items-center justify-center">
                              <Globe className="w-4 h-4 text-purple-600" />
                            </div>
                            <div>
                              <div className="font-medium text-gray-900">{ingress.metadata.name}</div>
                              <div className="text-sm text-gray-500">{ingress.metadata.namespace}</div>
                            </div>
                          </div>
                        </TableCell>
                        <TableCell>
                          <div className="space-y-1">
                            {hosts.slice(0, 2).map((host, i) => (
                              <Badge key={i} variant="outline" className="mr-1 mb-1">
                                <Link className="w-3 h-3 mr-1" />
                                {host}
                              </Badge>
                            ))}
                            {hosts.length > 2 && (
                              <Badge variant="secondary">
                                +{hosts.length - 2} 更多
                              </Badge>
                            )}
                          </div>
                        </TableCell>
                        <TableCell>
                          <span className="text-sm font-medium">
                            {rulesCount} 个规则
                          </span>
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center space-x-2">
                            <statusInfo.icon className={`w-4 h-4 ${statusInfo.color}`} />
                            <span className="text-sm">{statusInfo.status}</span>
                          </div>
                        </TableCell>
                        <TableCell>
                          <span className="text-sm text-gray-600">
                            {getIngressAge(ingress.metadata.creationTimestamp)}
                          </span>
                        </TableCell>

                      </motion.tr>
                    );
                  })}
                </AnimatePresence>
              </TableBody>
            </Table>
          </div>

          {filteredIngresses.length === 0 && !loading && (
            <div className="text-center py-12">
              <Globe className="w-16 h-16 text-gray-300 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">暂无Ingresses</h3>
              <p className="text-gray-500">
                {searchTerm || statusFilter !== "all" 
                  ? '没有找到匹配的Ingresses' 
                  : '当前命名空间中没有Ingresses资源'
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

export default IngressesDashboard;