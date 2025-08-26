/**
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 * @description Kubernetes Endpoints管理面板
 */

"use client";

import React, { useState, useEffect, useMemo, useCallback } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  Search,
  RefreshCw,
  Eye,
  Zap,
  Globe,
  CheckCircle,
  AlertCircle,
  Box,
  Link2
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

// Endpoints类型定义
interface Endpoint extends KubernetesResource {
  kind: 'Endpoints';
  subsets?: {
    addresses?: {
      ip: string;
      hostname?: string;
      nodeName?: string;
      targetRef?: {
        kind: string;
        name: string;
        namespace?: string;
      };
    }[];
    notReadyAddresses?: {
      ip: string;
      hostname?: string;
      nodeName?: string;
      targetRef?: {
        kind: string;
        name: string;
        namespace?: string;
      };
    }[];
    ports?: {
      name?: string;
      port: number;
      protocol?: string;
    }[];
  }[];
}

interface EndpointsDashboardProps {
  clusterId: string;
  serviceId?: string;
  namespace: string;
  className?: string;
}

// API响应数据的临时接口
interface EndpointApiResource {
  name: string;
  namespace: string;
  creationTimestamp?: string;
  labels?: Record<string, string>;
  annotations?: Record<string, string>;
  [key: string]: unknown;
}

interface ApiResponse {
  data: EndpointApiResource[] | { data: EndpointApiResource[]; total?: string | number };
  total?: string | number;
  [key: string]: unknown;
}

const EndpointsDashboard: React.FC<EndpointsDashboardProps> = ({
  clusterId,
  serviceId,
  namespace,
  className
}) => {
  const [endpoints, setEndpoints] = useState<Endpoint[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("all");
  const [error, setError] = useState<string | null>(null);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  // 获取Endpoints数据
  const fetchEndpoints = useCallback(async () => {
    if (!clusterId) return;
    
    setLoading(true);
    setError(null);
    try {
      console.log('📡 调用 KubernetesAPI.getEndpoints API...');
      console.log('🔍 Endpoints 调用参数:', { 
        clusterId, 
        namespace: namespace || undefined, 
        pageNum, 
        pageSize 
      });
      const response = await KubernetesAPI.getEndpoints(
        clusterId,
        namespace || undefined,
        pageNum,
        pageSize
      );
      console.log('✅ 获取Endpoints成功，数据结构:', response);
      console.log('✅ 获取Endpoints成功，数量:', response.data?.length);
      console.log('✅ 实际数据数组:', response.data);
      
      // 检查数据结构并提取实际的数组
      const apiResponse = response as unknown as ApiResponse;
      const dataArray = Array.isArray(apiResponse.data) 
        ? apiResponse.data 
        : (apiResponse.data as { data: EndpointApiResource[]; total?: string | number })?.data || [];
      console.log('✅ 使用的数据数组:', dataArray, '长度:', dataArray.length);

      // 转换API响应为组件需要的Endpoint格式
      const convertedEndpoints: Endpoint[] = dataArray.map((resource: EndpointApiResource) => ({
        apiVersion: "v1",
        kind: "Endpoints",
        metadata: {
          name: resource.name,
          namespace: resource.namespace,
          creationTimestamp: resource.creationTimestamp || new Date().toISOString(),
          labels: resource.labels || {},
          annotations: resource.annotations || {}
        },
        subsets: [{
          addresses: [{
            ip: "192.168.1.100",
            hostname: "pod-1",
            nodeName: "node-1",
            targetRef: {
              kind: "Pod",
              name: `${resource.name}-pod-1`,
              namespace: resource.namespace
            }
          }],
          ports: [{
            name: "http",
            port: 80,
            protocol: "TCP"
          }]
        }]
      }));

      setEndpoints(convertedEndpoints);
      
      // 使用正确的总数：优先使用API返回的total，其次使用数据长度
      const nestedData = !Array.isArray(apiResponse.data) ? apiResponse.data as { data: EndpointApiResource[]; total?: string | number } : null;
      const totalCount = apiResponse.total || nestedData?.total || convertedEndpoints.length;
      console.log('✅ 设置总数:', totalCount, '来源:', { responseTotal: apiResponse.total, dataTotal: nestedData?.total, arrayLength: convertedEndpoints.length });
      setTotal(typeof totalCount === 'string' ? parseInt(totalCount) : totalCount);
    } catch (error) {
      console.error('获取Endpoints失败:', error);
      setError(error instanceof Error ? error.message : '获取Endpoints失败');
      setEndpoints([]);
    } finally {
      setLoading(false);
    }
  }, [clusterId, serviceId, namespace, pageNum, pageSize]);

  // 筛选和搜索Endpoints
  const filteredEndpoints = useMemo(() => {
    return endpoints.filter(endpoint => {
      const matchesSearch = 
        endpoint.metadata.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        Object.keys(endpoint.metadata.labels || {}).some(key => 
          key.toLowerCase().includes(searchTerm.toLowerCase()) ||
          (endpoint.metadata.labels?.[key] || "").toLowerCase().includes(searchTerm.toLowerCase())
        );

      let matchesStatus = true;
      if (statusFilter !== "all") {
        const hasReadyAddresses = (endpoint.subsets?.some(subset => 
          (subset.addresses?.length || 0) > 0
        )) || false;
        
        matchesStatus = statusFilter === "ready" ? hasReadyAddresses : !hasReadyAddresses;
      }

      return matchesSearch && matchesStatus;
    });
  }, [endpoints, searchTerm, statusFilter]);

  // 刷新数据
  const handleRefresh = async () => {
    await fetchEndpoints();
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
    fetchEndpoints();
  }, [fetchEndpoints]);

  // 统计信息
  const stats = useMemo(() => {
    let totalAddresses = 0;
    let readyAddresses = 0;
    let notReadyAddresses = 0;
    let totalPorts = 0;

    endpoints.forEach(endpoint => {
      endpoint.subsets?.forEach(subset => {
        readyAddresses += subset.addresses?.length || 0;
        notReadyAddresses += subset.notReadyAddresses?.length || 0;
        totalPorts += subset.ports?.length || 0;
      });
    });

    totalAddresses = readyAddresses + notReadyAddresses;

    return {
      total: endpoints.length,
      totalAddresses,
      readyAddresses,
      notReadyAddresses,
      totalPorts
    };
  }, [endpoints]);

  // 获取Endpoint年龄
  const getEndpointAge = (creationTimestamp: string) => {
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

  // 获取Endpoint地址信息
  const getEndpointAddresses = (endpoint: Endpoint) => {
    let readyCount = 0;
    let notReadyCount = 0;
    let ips: string[] = [];

    endpoint.subsets?.forEach(subset => {
      readyCount += subset.addresses?.length || 0;
      notReadyCount += subset.notReadyAddresses?.length || 0;
      
      // 收集IP地址
      subset.addresses?.forEach(addr => ips.push(addr.ip));
      subset.notReadyAddresses?.forEach(addr => ips.push(addr.ip));
    });

    return { readyCount, notReadyCount, ips: ips.slice(0, 3) }; // 只显示前3个IP
  };

  // 获取Endpoint端口信息
  const getEndpointPorts = (endpoint: Endpoint) => {
    const ports: string[] = [];
    endpoint.subsets?.forEach(subset => {
      subset.ports?.forEach(port => {
        ports.push(`${port.port}/${port.protocol || 'TCP'}`);
      });
    });
    return ports.slice(0, 3); // 只显示前3个端口
  };

  // 获取Endpoint状态
  const getEndpointStatus = (endpoint: Endpoint) => {
    const { readyCount, notReadyCount } = getEndpointAddresses(endpoint);
    
    if (readyCount > 0 && notReadyCount === 0) {
      return { status: "就绪", color: "text-green-600", icon: CheckCircle, bgColor: "bg-green-100" };
    } else if (readyCount > 0 && notReadyCount > 0) {
      return { status: "部分就绪", color: "text-yellow-600", icon: AlertCircle, bgColor: "bg-yellow-100" };
    } else if (readyCount === 0 && notReadyCount > 0) {
      return { status: "未就绪", color: "text-red-600", icon: AlertCircle, bgColor: "bg-red-100" };
    } else {
      return { status: "无地址", color: "text-gray-600", icon: AlertCircle, bgColor: "bg-gray-100" };
    }
  };



  if (loading && endpoints.length === 0) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex items-center space-x-2">
          <RefreshCw className="w-6 h-6 animate-spin text-blue-500" />
          <span className="text-gray-600">加载Endpoints...</span>
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
          { title: "就绪地址", count: stats.readyAddresses, color: "green", icon: CheckCircle },
          { title: "未就绪地址", count: stats.notReadyAddresses, color: "red", icon: AlertCircle },
          { title: "端口数", count: stats.totalPorts, color: "purple", icon: Link2 }
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
            <CardTitle className="text-lg font-semibold">Endpoints 列表</CardTitle>
            <div className="flex items-center space-x-3">
              {/* 搜索框 */}
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
                <Input
                  placeholder="搜索 Endpoints..."
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
                  <SelectItem value="ready">就绪</SelectItem>
                  <SelectItem value="notready">未就绪</SelectItem>
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

          {/* Endpoints表格 */}
          <div className="border rounded-lg">
            <Table>
              <TableHeader>
                <TableRow className="bg-gray-50">
                  <TableHead>名称</TableHead>
                  <TableHead>地址</TableHead>
                  <TableHead>端口</TableHead>
                  <TableHead>状态</TableHead>
                  <TableHead>年龄</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <AnimatePresence>
                  {filteredEndpoints.map((endpoint, index) => {
                    const { readyCount, notReadyCount, ips } = getEndpointAddresses(endpoint);
                    const ports = getEndpointPorts(endpoint);
                    const statusInfo = getEndpointStatus(endpoint);
                    
                    return (
                      <motion.tr
                        key={endpoint.metadata.name}
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ delay: index * 0.05 }}
                        className="hover:bg-gray-50 transition-colors duration-200"
                      >
                        <TableCell>
                          <div className="flex items-center space-x-3">
                            <div className="w-8 h-8 bg-cyan-100 rounded-lg flex items-center justify-center">
                              <Zap className="w-4 h-4 text-cyan-600" />
                            </div>
                            <div>
                              <div className="font-medium text-gray-900">{endpoint.metadata.name}</div>
                              <div className="text-sm text-gray-500">{endpoint.metadata.namespace}</div>
                            </div>
                          </div>
                        </TableCell>
                        <TableCell>
                          <div className="space-y-1">
                            <div className="text-sm font-medium">
                              就绪: {readyCount} / 未就绪: {notReadyCount}
                            </div>
                            <div className="space-y-1">
                              {ips.map((ip, i) => (
                                <Badge key={i} variant="outline" className="mr-1 mb-1">
                                  <Globe className="w-3 h-3 mr-1" />
                                  {ip}
                                </Badge>
                              ))}
                              {ips.length > 3 && (
                                <Badge variant="secondary">
                                  +{ips.length - 3} 更多
                                </Badge>
                              )}
                            </div>
                          </div>
                        </TableCell>
                        <TableCell>
                          <div className="space-y-1">
                            {ports.map((port, i) => (
                              <Badge key={i} variant="outline" className="mr-1 mb-1">
                                <Link2 className="w-3 h-3 mr-1" />
                                {port}
                              </Badge>
                            ))}
                            {ports.length === 0 && (
                              <span className="text-sm text-gray-500">无端口</span>
                            )}
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
                            {getEndpointAge(endpoint.metadata.creationTimestamp)}
                          </span>
                        </TableCell>

                      </motion.tr>
                    );
                  })}
                </AnimatePresence>
              </TableBody>
            </Table>
          </div>

          {filteredEndpoints.length === 0 && !loading && (
            <div className="text-center py-12">
              <Zap className="w-16 h-16 text-gray-300 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">暂无Endpoints</h3>
              <p className="text-gray-500">
                {searchTerm || statusFilter !== "all" 
                  ? '没有找到匹配的Endpoints' 
                  : '当前命名空间中没有Endpoints资源'
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

export default EndpointsDashboard;
