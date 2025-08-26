/**
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 * @description Kubernetes IngressClasses管理面板
 */

"use client";

import React, { useState, useEffect, useMemo, useCallback } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  Search,
  RefreshCw,

  Shield,
  Settings,
  CheckCircle,
  AlertCircle,
  Box,
  Tag
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

import type { KubernetesResource } from "../types";
import { KubernetesAPI } from '@/lib/kubernetes-api';
import KubernetesPagination from "../components/kubernetes-pagination";

// IngressClass类型定义
interface IngressClass extends KubernetesResource {
  kind: 'IngressClass';
  spec: {
    controller: string;
    parameters?: {
      apiGroup?: string;
      kind: string;
      name: string;
      namespace?: string;
      scope?: string;
    };
  };
}

interface IngressClassesDashboardProps {
  clusterId: string;
  serviceId?: string;
  namespace: string;
  className?: string;
}

// API响应数据的临时接口
interface IngressClassApiResource {
  name: string;
  namespace?: string;
  creationTimestamp?: string;
  labels?: Record<string, string>;
  annotations?: Record<string, string>;
  controller?: string;
  [key: string]: unknown;
}

interface ApiResponse {
  data: IngressClassApiResource[] | { data: IngressClassApiResource[]; total?: string | number };
  total?: string | number;
  [key: string]: unknown;
}

const IngressClassesDashboard: React.FC<IngressClassesDashboardProps> = ({
  clusterId,

  className
}) => {
  const [ingressClasses, setIngressClasses] = useState<IngressClass[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [controllerFilter, setControllerFilter] = useState<string>("all");
  const [error, setError] = useState<string | null>(null);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  // 获取IngressClasses数据
  const fetchIngressClasses = useCallback(async () => {
    if (!clusterId) return;
    
    setLoading(true);
    setError(null);
    try {
      console.log('📡 调用 KubernetesAPI.getIngressClasses API...');
      console.log('🔍 IngressClasses 调用参数:', { 
        clusterId, 
        pageNum, 
        pageSize 
      });
      const response = await KubernetesAPI.getIngressClasses(
        clusterId,
        pageNum,
        pageSize
      );
      console.log('✅ 获取IngressClasses成功，数据结构:', response);
      console.log('✅ 获取IngressClasses成功，数量:', response.data?.length);
      console.log('✅ 实际数据数组:', response.data);
      
      // 检查数据结构并提取实际的数组
      const apiResponse = response as unknown as ApiResponse;
      const dataArray = Array.isArray(apiResponse.data) 
        ? apiResponse.data 
        : (apiResponse.data as { data: IngressClassApiResource[]; total?: string | number })?.data || [];
      console.log('✅ 使用的数据数组:', dataArray, '长度:', dataArray.length);

      // 转换API响应为组件需要的IngressClass格式
      const convertedIngressClasses: IngressClass[] = dataArray.map((resource: IngressClassApiResource) => ({
        apiVersion: "networking.k8s.io/v1",
        kind: "IngressClass",
        metadata: {
          name: resource.name,
          namespace: resource.namespace || "",
          creationTimestamp: resource.creationTimestamp || new Date().toISOString(),
          labels: resource.labels || {},
          annotations: resource.annotations || {}
        },
        spec: {
          controller: resource.controller || "unknown-controller",
          parameters: {
            kind: "ConfigMap",
            name: `${resource.name}-config`
          }
        }
      }));

      setIngressClasses(convertedIngressClasses);
      
      // 使用正确的总数：优先使用API返回的total，其次使用数据长度
      const nestedData = !Array.isArray(apiResponse.data) ? apiResponse.data as { data: IngressClassApiResource[]; total?: string | number } : null;
      const totalCount = apiResponse.total || nestedData?.total || convertedIngressClasses.length;
      console.log('✅ 设置总数:', totalCount, '来源:', { responseTotal: apiResponse.total, dataTotal: nestedData?.total, arrayLength: convertedIngressClasses.length });
      setTotal(typeof totalCount === 'string' ? parseInt(totalCount) : totalCount);
    } catch (error) {
      console.error('获取IngressClasses失败:', error);
      setError(error instanceof Error ? error.message : '获取IngressClasses失败');
      setIngressClasses([]);
    } finally {
      setLoading(false);
    }
  }, [clusterId, pageNum, pageSize]);

  // 筛选和搜索IngressClasses
  const filteredIngressClasses = useMemo(() => {
    return ingressClasses.filter(ingressClass => {
      const matchesSearch = 
        ingressClass.metadata.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        ingressClass.spec.controller.toLowerCase().includes(searchTerm.toLowerCase()) ||
        Object.keys(ingressClass.metadata.labels || {}).some(key => 
          key.toLowerCase().includes(searchTerm.toLowerCase()) ||
          (ingressClass.metadata.labels?.[key] || "").toLowerCase().includes(searchTerm.toLowerCase())
        );

      let matchesController = true;
      if (controllerFilter !== "all") {
        const controller = ingressClass.spec.controller.toLowerCase();
        switch (controllerFilter) {
          case "nginx":
            matchesController = controller.includes("nginx");
            break;
          case "traefik":
            matchesController = controller.includes("traefik");
            break;
          case "haproxy":
            matchesController = controller.includes("haproxy");
            break;
          case "istio":
            matchesController = controller.includes("istio");
            break;
          default:
            matchesController = true;
        }
      }

      return matchesSearch && matchesController;
    });
  }, [ingressClasses, searchTerm, controllerFilter]);

  // 刷新数据
  const handleRefresh = async () => {
    await fetchIngressClasses();
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
    fetchIngressClasses();
  }, [fetchIngressClasses]);

  // 统计信息
  const stats = useMemo(() => {
    const controllers = ingressClasses.reduce((acc, ic) => {
      const controller = ic.spec.controller;
      acc[controller] = (acc[controller] || 0) + 1;
      return acc;
    }, {} as Record<string, number>);

    return {
      total: ingressClasses.length,
      nginx: Object.keys(controllers).filter(c => c.includes("nginx")).reduce((sum, key) => sum + controllers[key], 0),
      traefik: Object.keys(controllers).filter(c => c.includes("traefik")).reduce((sum, key) => sum + controllers[key], 0),
      others: ingressClasses.length - Object.keys(controllers).filter(c => c.includes("nginx") || c.includes("traefik")).reduce((sum, key) => sum + controllers[key], 0)
    };
  }, [ingressClasses]);

  // 获取IngressClass年龄
  const getIngressClassAge = (creationTimestamp: string) => {
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

  // 获取控制器类型和颜色
  const getControllerInfo = (controller: string) => {
    const lowerController = controller.toLowerCase();
    if (lowerController.includes("nginx")) {
      return { type: "NGINX", color: "bg-green-100 text-green-800" };
    } else if (lowerController.includes("traefik")) {
      return { type: "Traefik", color: "bg-blue-100 text-blue-800" };
    } else if (lowerController.includes("haproxy")) {
      return { type: "HAProxy", color: "bg-purple-100 text-purple-800" };
    } else if (lowerController.includes("istio")) {
      return { type: "Istio", color: "bg-orange-100 text-orange-800" };
    } else {
      return { type: "Other", color: "bg-gray-100 text-gray-800" };
    }
  };



  if (loading && ingressClasses.length === 0) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex items-center space-x-2">
          <RefreshCw className="w-6 h-6 animate-spin text-blue-500" />
          <span className="text-gray-600">加载IngressClasses...</span>
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
          { title: "NGINX", count: stats.nginx, color: "green", icon: CheckCircle },
          { title: "Traefik", count: stats.traefik, color: "blue", icon: Settings },
          { title: "其他", count: stats.others, color: "gray", icon: Tag }
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
            <CardTitle className="text-lg font-semibold">IngressClasses 列表</CardTitle>
            <div className="flex items-center space-x-3">
              {/* 搜索框 */}
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
                <Input
                  placeholder="搜索 IngressClasses..."
                  className="pl-10 w-64"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>

              {/* 控制器筛选 */}
              <Select value={controllerFilter} onValueChange={setControllerFilter}>
                <SelectTrigger className="w-32">
                  <SelectValue placeholder="控制器筛选" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">全部</SelectItem>
                  <SelectItem value="nginx">NGINX</SelectItem>
                  <SelectItem value="traefik">Traefik</SelectItem>
                  <SelectItem value="haproxy">HAProxy</SelectItem>
                  <SelectItem value="istio">Istio</SelectItem>
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

          {/* IngressClasses表格 */}
          <div className="border rounded-lg">
            <Table>
              <TableHeader>
                <TableRow className="bg-gray-50">
                  <TableHead>名称</TableHead>
                  <TableHead>控制器</TableHead>
                  <TableHead>类型</TableHead>
                  <TableHead>参数</TableHead>
                  <TableHead>年龄</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <AnimatePresence>
                  {filteredIngressClasses.map((ingressClass, index) => {
                    const controllerInfo = getControllerInfo(ingressClass.spec.controller);
                    const hasParameters = ingressClass.spec.parameters?.name;
                    
                    return (
                      <motion.tr
                        key={ingressClass.metadata.name}
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ delay: index * 0.05 }}
                        className="hover:bg-gray-50 transition-colors duration-200"
                      >
                        <TableCell>
                          <div className="flex items-center space-x-3">
                            <div className="w-8 h-8 bg-indigo-100 rounded-lg flex items-center justify-center">
                              <Shield className="w-4 h-4 text-indigo-600" />
                            </div>
                            <div>
                              <div className="font-medium text-gray-900">{ingressClass.metadata.name}</div>
                              <div className="text-sm text-gray-500">集群级别</div>
                            </div>
                          </div>
                        </TableCell>
                        <TableCell>
                          <code className="bg-gray-100 px-2 py-1 rounded text-xs font-mono">
                            {ingressClass.spec.controller}
                          </code>
                        </TableCell>
                        <TableCell>
                          <Badge className={controllerInfo.color}>
                            {controllerInfo.type}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          {hasParameters ? (
                            <div className="flex items-center space-x-1">
                              <CheckCircle className="w-4 h-4 text-green-500" />
                              <span className="text-sm">已配置</span>
                            </div>
                          ) : (
                            <div className="flex items-center space-x-1">
                              <AlertCircle className="w-4 h-4 text-gray-400" />
                              <span className="text-sm text-gray-500">无参数</span>
                            </div>
                          )}
                        </TableCell>
                        <TableCell>
                          <span className="text-sm text-gray-600">
                            {getIngressClassAge(ingressClass.metadata.creationTimestamp)}
                          </span>
                        </TableCell>

                      </motion.tr>
                    );
                  })}
                </AnimatePresence>
              </TableBody>
            </Table>
          </div>

          {filteredIngressClasses.length === 0 && !loading && (
            <div className="text-center py-12">
              <Shield className="w-16 h-16 text-gray-300 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">暂无IngressClasses</h3>
              <p className="text-gray-500">
                {searchTerm || controllerFilter !== "all" 
                  ? '没有找到匹配的IngressClasses' 
                  : '当前集群中没有IngressClasses资源'
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

export default IngressClassesDashboard;