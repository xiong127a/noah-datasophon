/**
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 * @description Kubernetes Services管理面板
 */

"use client";

import React, { useState, useEffect, useMemo, useCallback } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  Search,
  RefreshCw,

  Network,
  Globe,
  AlertCircle,
  Box,
  Activity
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
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

import { Service } from "../types";
import { KubernetesAPI } from '@/lib/kubernetes-api';
import { getServiceStatusConfig } from "@/lib/kubernetes-status-utils";
import KubernetesPagination from "../components/kubernetes-pagination";

interface ServicesDashboardProps {
  clusterId: string;
  serviceId?: string;
  namespace: string;
  className?: string;
}

// API响应数据的临时接口
interface ServiceApiResource {
  name?: string;
  namespace?: string;
  creationTimestamp?: string;
  labels?: Record<string, string>;
  type?: string;
  spec?: {
    selector?: Record<string, string>;
  };
  ports?: string;
  clusterIp?: string;
  externalIp?: string;
  [key: string]: unknown;
}

const ServicesDashboard: React.FC<ServicesDashboardProps> = ({
  clusterId,
  serviceId,
  namespace,
  className
}) => {
  const [services, setServices] = useState<Service[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [typeFilter, setTypeFilter] = useState<string>("all");
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [error, setError] = useState<string | null>(null);

  // 获取Services数据
  const fetchServices = useCallback(async () => {
    if (!clusterId) return;
    
    console.log('🔄 开始获取Services列表:', { clusterId, namespace, pageNum, pageSize });
    setLoading(true);
    setError(null);
    try {
      console.log('📡 调用 KubernetesAPI.getServices API...');
      const response = await KubernetesAPI.getServices(
        clusterId,
        namespace || undefined,
        serviceId || undefined,
        pageNum,
        pageSize
      );
      console.log('✅ 获取Services成功，数量:', response.data.length);

      // 转换API响应为组件需要的Service格式
      const convertedServices: Service[] = (response.data as unknown as ServiceApiResource[]).map((resource: ServiceApiResource) => ({
        apiVersion: "v1",
        kind: "Service",
        metadata: {
          name: String(resource.name || ''),
          namespace: String(resource.namespace || ''),
          creationTimestamp: resource.creationTimestamp || new Date().toISOString(),
          labels: resource.labels || {}
        },
        spec: {
          type: (resource.type || 'ClusterIP') as "ClusterIP" | "NodePort" | "LoadBalancer" | "ExternalName",
          selector: (resource.spec?.selector) || {},
          ports: resource.ports ? String(resource.ports).split(',').map((port: string, idx: number) => ({
            name: `port-${idx}`,
            port: parseInt(port.split('/')[0]) || 80,
            targetPort: parseInt(port.split('/')[0]) || 80,
            protocol: port.split('/')[1] || 'TCP'
          })) : [],
          clusterIP: String(resource.clusterIp || 'None')
        },
        status: resource.externalIp ? {
          loadBalancer: {
            ingress: [{ ip: String(resource.externalIp) }]
          }
        } : undefined
      }));

      setServices(convertedServices);
      setTotal(response.total || convertedServices.length);
    } catch (error) {
      console.error('获取Services失败:', error);
      setError(error instanceof Error ? error.message : '获取Services失败');
      setServices([]);
    } finally {
      setLoading(false);
    }
  }, [clusterId, serviceId, namespace, pageNum, pageSize]);

  // 筛选和搜索Services
  const filteredServices = useMemo(() => {
    return services.filter(service => {
      const matchesSearch = 
        service.metadata.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        Object.keys(service.metadata.labels || {}).some(key => 
          key.toLowerCase().includes(searchTerm.toLowerCase()) ||
          (service.metadata.labels?.[key] || "").toLowerCase().includes(searchTerm.toLowerCase())
        );

      const matchesType = typeFilter === "all" || 
        service.spec?.type?.toLowerCase() === typeFilter.toLowerCase();

      return matchesSearch && matchesType;
    });
  }, [services, searchTerm, typeFilter]);

  // 统计信息
  const stats = useMemo(() => {
    return {
      total: services.length,
      clusterip: services.filter(s => s.spec?.type === "ClusterIP").length,
      nodeport: services.filter(s => s.spec?.type === "NodePort").length,
      loadbalancer: services.filter(s => s.spec?.type === "LoadBalancer").length
    };
  }, [services]);

  // 获取Service年龄
  const getServiceAge = (creationTimestamp: string) => {
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

  // 获取端口信息
  const getPortsInfo = (service: Service) => {
    if (!service.spec?.ports || service.spec.ports.length === 0) return "无";
    return service.spec.ports.map(p => `${p.port}/${p.protocol}`).join(", ");
  };

  // 获取Service类型颜色 - 已优化使用统一配色系统

  // 刷新数据
  const handleRefresh = async () => {
    await fetchServices();
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
    fetchServices();
  }, [fetchServices]);

  if (loading && services.length === 0) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex items-center space-x-2">
          <RefreshCw className="w-6 h-6 animate-spin text-blue-500" />
          <span className="text-gray-600">加载Services...</span>
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
          { title: "ClusterIP", count: stats.clusterip, color: "green", icon: Network },
          { title: "NodePort", count: stats.nodeport, color: "purple", icon: Globe },
          { title: "LoadBalancer", count: stats.loadbalancer, color: "orange", icon: Activity }
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
              <CardTitle className="text-lg">Services</CardTitle>
              <CardDescription>管理Kubernetes服务</CardDescription>
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

            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* 搜索和过滤 */}
          <div className="flex items-center space-x-4">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
              <Input
                placeholder="搜索Services..."
                className="pl-10"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
            <Select value={typeFilter} onValueChange={setTypeFilter}>
              <SelectTrigger className="w-40">
                <SelectValue placeholder="选择类型" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">所有类型</SelectItem>
                <SelectItem value="clusterip">ClusterIP</SelectItem>
                <SelectItem value="nodeport">NodePort</SelectItem>
                <SelectItem value="loadbalancer">LoadBalancer</SelectItem>
              </SelectContent>
            </Select>

          </div>

          {/* Services表格 */}
          <div className="border rounded-lg">
            <Table>
              <TableHeader>
                <TableRow className="bg-gray-50">
                  <TableHead>名称</TableHead>
                  <TableHead>类型</TableHead>
                  <TableHead>Cluster IP</TableHead>
                  <TableHead>端口</TableHead>
                  <TableHead>年龄</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <AnimatePresence>
                  {filteredServices.map((service, index) => (
                    <motion.tr
                      key={service.metadata.name}
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      exit={{ opacity: 0 }}
                      transition={{ delay: index * 0.05 }}
                      className="hover:bg-gray-50 transition-colors duration-200"
                    >
                      <TableCell>
                        <div className="flex items-center space-x-3">
                          <div className="w-8 h-8 bg-green-100 rounded-lg flex items-center justify-center">
                            <Network className="w-4 h-4 text-green-600" />
                          </div>
                          <div>
                            <div className="font-medium text-gray-900">{service.metadata.name}</div>
                            <div className="text-sm text-gray-500">{service.metadata.namespace}</div>
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge 
                          variant={getServiceStatusConfig(service.spec?.type || 'ClusterIP').variant}
                          className={`text-xs ${getServiceStatusConfig(service.spec?.type || 'ClusterIP').className}`}
                        >
                          {service.spec?.type || 'ClusterIP'}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <span className="font-mono text-sm">{service.spec?.clusterIP || 'None'}</span>
                      </TableCell>
                      <TableCell>
                        <span className="text-sm">{getPortsInfo(service)}</span>
                      </TableCell>
                      <TableCell>
                        <span className="text-sm text-gray-600">
                          {getServiceAge(service.metadata.creationTimestamp)}
                        </span>
                      </TableCell>

                    </motion.tr>
                  ))}
                </AnimatePresence>
              </TableBody>
            </Table>
          </div>

          {filteredServices.length === 0 && !loading && (
            <div className="text-center py-8">
              <Network className="w-12 h-12 text-gray-400 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">暂无Services</h3>
              <p className="text-gray-500 mb-4">
                {searchTerm ? '没有找到匹配的Services' : '当前命名空间中没有Services'}
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

export default ServicesDashboard;