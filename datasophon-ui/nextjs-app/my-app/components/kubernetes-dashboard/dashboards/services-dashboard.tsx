/**
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 * @description Kubernetes Services管理面板
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
  Network,
  Globe,
  CheckCircle,
  AlertCircle,
  Clock,
  Box,
  ChevronDown,
  ChevronRight,
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

import { Service } from "../types";
import { KubernetesAPI } from '@/lib/kubernetes-api';

interface ServicesDashboardProps {
  clusterId: string;
  namespace: string;
  className?: string;
}

const ServicesDashboard: React.FC<ServicesDashboardProps> = ({
  clusterId,
  namespace,
  className
}) => {
  const [services, setServices] = useState<Service[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [typeFilter, setTypeFilter] = useState<string>("all");
  const [selectedService, setSelectedService] = useState<Service | null>(null);
  const [showDetails, setShowDetails] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize] = useState(20);
  const [total, setTotal] = useState(0);

  // 获取Services数据
  const fetchServices = async () => {
    if (!clusterId) return;
    
    console.log('🔄 开始获取Services列表:', { clusterId, namespace, pageNum, pageSize });
    setLoading(true);
    setError(null);
    try {
      console.log('📡 调用 KubernetesAPI.getServices API...');
      const response = await KubernetesAPI.getServices(
        clusterId,
        namespace || undefined,
        undefined, // serviceId
        pageNum,
        pageSize
      );
      console.log('✅ 获取Services成功，数量:', response.data.length);

      // 转换API响应为组件需要的Service格式
      const convertedServices: Service[] = response.data.map((resource: any) => ({
        apiVersion: "v1",
        kind: "Service",
        metadata: {
          name: resource.name,
          namespace: resource.namespace,
          creationTimestamp: resource.creationTimestamp || new Date().toISOString(),
          labels: resource.labels || {}
        },
        spec: {
          type: resource.type || 'ClusterIP',
          selector: resource.spec?.selector || {},
          ports: resource.ports ? resource.ports.split(',').map((port: string, idx: number) => ({
            name: `port-${idx}`,
            port: parseInt(port.split('/')[0]) || 80,
            targetPort: parseInt(port.split('/')[0]) || 80,
            protocol: port.split('/')[1] || 'TCP'
          })) : [],
          clusterIP: resource.clusterIp || 'None'
        },
        status: resource.externalIp ? {
          loadBalancer: {
            ingress: [{ ip: resource.externalIp }]
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
  };

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

  // 获取Service类型颜色
  const getServiceTypeColor = (type?: string) => {
    switch (type) {
      case "ClusterIP": return "bg-blue-100 text-blue-700";
      case "NodePort": return "bg-green-100 text-green-700";
      case "LoadBalancer": return "bg-purple-100 text-purple-700";
      case "ExternalName": return "bg-orange-100 text-orange-700";
      default: return "bg-gray-100 text-gray-700";
    }
  };

  // 刷新数据
  const handleRefresh = async () => {
    await fetchServices();
  };

  // Service操作
  const handleServiceAction = (action: string, service: Service) => {
    console.log(`执行操作: ${action} on Service: ${service.metadata.name}`);
    switch (action) {
      case 'view':
        setSelectedService(service);
        setShowDetails(true);
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
    fetchServices();
  }, [clusterId, namespace, pageNum]);

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
              <Button size="sm">
                <Network className="w-4 h-4 mr-2" />
                新建Service
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
            <Button variant="outline" size="sm">
              <Download className="w-4 h-4 mr-2" />
              导出
            </Button>
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
                  <TableHead>操作</TableHead>
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
                        <Badge className={`text-xs ${getServiceTypeColor(service.spec?.type)}`}>
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
                      <TableCell>
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="sm">
                              <MoreHorizontal className="w-4 h-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <DropdownMenuItem onClick={() => handleServiceAction('view', service)}>
                              <Eye className="w-4 h-4 mr-2" />
                              查看详情
                            </DropdownMenuItem>
                            <DropdownMenuItem onClick={() => handleServiceAction('edit', service)}>
                              <Edit className="w-4 h-4 mr-2" />
                              编辑
                            </DropdownMenuItem>
                            <DropdownMenuSeparator />
                            <DropdownMenuItem 
                              onClick={() => handleServiceAction('delete', service)}
                              className="text-red-600"
                            >
                              <Trash2 className="w-4 h-4 mr-2" />
                              删除
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
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
              {!searchTerm && (
                <Button>
                  <Network className="w-4 h-4 mr-2" />
                  创建第一个Service
                </Button>
              )}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default ServicesDashboard;