/**
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 * @description Kubernetes Ingresses管理面板
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
  Globe,
  Network,
  CheckCircle,
  AlertCircle,
  Clock,
  Box,
  ChevronDown,
  ChevronRight,
  Activity,
  Shield,
  Link,
  ExternalLink,
  Route
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

import { KubernetesAPI, K8sResource, K8sResourceListResponse } from '@/lib/kubernetes-api';

interface IngressesDashboardProps {
  clusterId: string;
  namespace: string;
  className?: string;
}

interface Ingress {
  name: string;
  namespace: string;
  className?: string;
  hosts: string[];
  addresses: string[];
  ports: Array<{
    port: number;
    protocol: string;
  }>;
  rules: Array<{
    host?: string;
    paths: Array<{
      path: string;
      pathType: string;
      backend: {
        serviceName: string;
        servicePort: number | string;
      };
    }>;
  }>;
  tls: Array<{
    hosts: string[];
    secretName: string;
  }>;
  annotations: Record<string, string>;
  age: string;
  creationTimestamp: string;
  status: 'Ready' | 'Pending' | 'Error' | 'Unknown';
}

const IngressesDashboard: React.FC<IngressesDashboardProps> = ({
  clusterId,
  namespace,
  className
}) => {
  const [ingresses, setIngresses] = useState<Ingress[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [classFilter, setClassFilter] = useState<string>("all");
  const [selectedIngress, setSelectedIngress] = useState<Ingress | null>(null);
  const [showDetails, setShowDetails] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize] = useState(20);
  const [total, setTotal] = useState(0);

  // 筛选和搜索Ingresses
  const filteredIngresses = useMemo(() => {
    return ingresses.filter(ing => {
      const matchesSearch = 
        ing.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        ing.namespace.toLowerCase().includes(searchTerm.toLowerCase()) ||
        ing.hosts.some(host => host.toLowerCase().includes(searchTerm.toLowerCase())) ||
        ing.addresses.some(addr => addr.includes(searchTerm));

      const matchesClass = classFilter === "all" || 
        (ing.className && ing.className.toLowerCase() === classFilter.toLowerCase());

      return matchesSearch && matchesClass;
    });
  }, [ingresses, searchTerm, classFilter]);

  // 统计信息
  const stats = useMemo(() => {
    return {
      total: ingresses.length,
      ready: ingresses.filter(ing => ing.status === 'Ready').length,
      withTLS: ingresses.filter(ing => ing.tls.length > 0).length,
      totalHosts: ingresses.reduce((sum, ing) => sum + ing.hosts.length, 0),
      totalRules: ingresses.reduce((sum, ing) => sum + ing.rules.length, 0),
      classes: [...new Set(ingresses.map(ing => ing.className).filter(Boolean))].length
    };
  }, [ingresses]);

  // 获取Ingresses数据
  const fetchIngresses = async () => {
    if (!clusterId) return;
    
    setLoading(true);
    setError(null);
    try {
      const response: K8sResourceListResponse = await KubernetesAPI.getIngresses(
        clusterId,
        namespace || undefined,
        pageNum,
        pageSize
      );

      // 转换API响应为组件需要的Ingress格式
      const convertedIngresses: Ingress[] = response.data.map((resource: K8sResource) => {
        const spec = resource.spec as any;
        const status = resource.metadata as any;
        const metadata = resource.metadata as any;
        
        return {
          name: resource.name,
          namespace: resource.namespace,
          className: spec?.ingressClassName,
          hosts: extractHosts(spec?.rules || []),
          addresses: status?.loadBalancer?.ingress?.map((ing: any) => ing.ip || ing.hostname).filter(Boolean) || [],
          ports: extractPorts(spec),
          rules: spec?.rules || [],
          tls: spec?.tls || [],
          annotations: metadata?.annotations || {},
          age: resource.age || '-',
          creationTimestamp: resource.creationTimestamp,
          status: determineIngressStatus(status, spec)
        };
      });

      setIngresses(convertedIngresses);
      setTotal(response.total || convertedIngresses.length);
    } catch (error) {
      console.error('获取Ingresses失败:', error);
      setError(error instanceof Error ? error.message : '获取Ingresses失败');
      setIngresses([]);
    } finally {
      setLoading(false);
    }
  };

  // 提取主机列表
  const extractHosts = (rules: any[]): string[] => {
    const hosts = rules
      .map(rule => rule.host)
      .filter(Boolean);
    return [...new Set(hosts)];
  };

  // 提取端口信息
  const extractPorts = (spec: any): Array<{port: number; protocol: string}> => {
    // 从TLS配置推断HTTPS端口
    const hasHttps = spec?.tls?.length > 0;
    const ports = [];
    
    if (hasHttps) {
      ports.push({ port: 443, protocol: 'HTTPS' });
    }
    
    // 默认HTTP端口
    ports.push({ port: 80, protocol: 'HTTP' });
    
    return ports;
  };

  // 确定Ingress状态
  const determineIngressStatus = (status: any, spec: any): 'Ready' | 'Pending' | 'Error' | 'Unknown' => {
    if (!status) return 'Unknown';
    
    const loadBalancer = status.loadBalancer;
    if (loadBalancer?.ingress && loadBalancer.ingress.length > 0) {
      return 'Ready';
    }
    
    return 'Pending';
  };

  // 获取年龄显示
  const getAge = (creationTimestamp: string): string => {
    const created = new Date(creationTimestamp);
    const now = new Date();
    const diffMs = now.getTime() - created.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    const diffHours = Math.floor((diffMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
    const diffMinutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));

    if (diffDays > 0) return `${diffDays}d`;
    if (diffHours > 0) return `${diffHours}h`;
    return `${diffMinutes}m`;
  };

  // 获取状态颜色和图标
  const getStatusDisplay = (status: string) => {
    const displays = {
      'Ready': { color: 'text-green-600', bgColor: 'bg-green-100', icon: CheckCircle },
      'Pending': { color: 'text-yellow-600', bgColor: 'bg-yellow-100', icon: Clock },
      'Error': { color: 'text-red-600', bgColor: 'bg-red-100', icon: AlertCircle },
      'Unknown': { color: 'text-gray-600', bgColor: 'bg-gray-100', icon: Activity }
    };
    return displays[status as keyof typeof displays] || displays['Unknown'];
  };

  // 格式化主机列表
  const formatHosts = (hosts: string[]): string => {
    if (hosts.length === 0) return '*';
    if (hosts.length === 1) return hosts[0];
    return `${hosts[0]} +${hosts.length - 1}`;
  };

  // 格式化地址列表
  const formatAddresses = (addresses: string[]): string => {
    if (addresses.length === 0) return '未分配';
    if (addresses.length === 1) return addresses[0];
    return `${addresses[0]} +${addresses.length - 1}`;
  };

  // 获取TLS状态
  const getTLSStatus = (tls: Array<{hosts: string[]; secretName: string}>): string => {
    if (tls.length === 0) return '无';
    return `${tls.length}个证书`;
  };

  // 获取路径总数
  const getPathsCount = (rules: Array<{paths: any[]}>): number => {
    return rules.reduce((sum, rule) => sum + (rule.paths?.length || 0), 0);
  };

  // 刷新数据
  const handleRefresh = async () => {
    await fetchIngresses();
  };

  // Ingress操作
  const handleIngressAction = (action: string, ingress: Ingress) => {
    console.log(`执行操作: ${action} on Ingress: ${ingress.name}`);
    switch (action) {
      case 'view':
        setSelectedIngress(ingress);
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

  // 获取可用的Ingress类
  const ingressClasses = useMemo(() => {
    const classes = [...new Set(ingresses.map(ing => ing.className).filter(Boolean))];
    return classes;
  }, [ingresses]);

  // 组件挂载和依赖更新时获取数据
  useEffect(() => {
    fetchIngresses();
  }, [clusterId, namespace, pageNum]);

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
          { 
            title: "总计", 
            count: stats.total, 
            color: "blue", 
            icon: Box,
            description: "Ingress总数"
          },
          { 
            title: "就绪", 
            count: stats.ready, 
            color: "green", 
            icon: CheckCircle,
            description: "已分配地址"
          },
          { 
            title: "TLS启用", 
            count: stats.withTLS, 
            color: "purple", 
            icon: Shield,
            description: "启用TLS的Ingress"
          },
          { 
            title: "总路由", 
            count: stats.totalRules, 
            color: "orange", 
            icon: Route,
            description: "路由规则总数"
          }
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
                    <p className="text-xs text-gray-500 mt-1">{stat.description}</p>
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
              <CardTitle className="text-lg">Ingresses</CardTitle>
              <CardDescription>管理Kubernetes入口资源</CardDescription>
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
                <Globe className="w-4 h-4 mr-2" />
                新建Ingress
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
                placeholder="搜索Ingresses..."
                className="pl-10"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
            <Select value={classFilter} onValueChange={setClassFilter}>
              <SelectTrigger className="w-40">
                <SelectValue placeholder="Ingress类" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">所有类</SelectItem>
                {ingressClasses.map(className => (
                  <SelectItem key={className} value={className.toLowerCase()}>
                    {className}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Button variant="outline" size="sm">
              <Download className="w-4 h-4 mr-2" />
              导出
            </Button>
          </div>

          {/* Ingresses表格 */}
          <div className="border rounded-lg">
            <Table>
              <TableHeader>
                <TableRow className="bg-gray-50">
                  <TableHead>名称</TableHead>
                  <TableHead>命名空间</TableHead>
                  <TableHead>类</TableHead>
                  <TableHead>主机</TableHead>
                  <TableHead>地址</TableHead>
                  <TableHead>端口</TableHead>
                  <TableHead>状态</TableHead>
                  <TableHead>创建时间</TableHead>
                  <TableHead>操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <AnimatePresence>
                  {filteredIngresses.map((ingress, index) => {
                    const statusDisplay = getStatusDisplay(ingress.status);
                    return (
                      <motion.tr
                        key={`${ingress.namespace}-${ingress.name}`}
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ delay: index * 0.05 }}
                        className="hover:bg-gray-50 transition-colors duration-200"
                      >
                        <TableCell>
                          <div className="flex items-center space-x-3">
                            <div className="w-8 h-8 bg-teal-100 rounded-lg flex items-center justify-center">
                              <Globe className="w-4 h-4 text-teal-600" />
                            </div>
                            <div>
                              <div className="font-medium text-gray-900">{ingress.name}</div>
                              <div className="text-xs text-gray-500">
                                {getPathsCount(ingress.rules)} 路径
                              </div>
                            </div>
                          </div>
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline" className="text-xs">
                            {ingress.namespace}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          {ingress.className ? (
                            <Badge variant="outline" className="text-xs">
                              {ingress.className}
                            </Badge>
                          ) : (
                            <span className="text-xs text-gray-400">默认</span>
                          )}
                        </TableCell>
                        <TableCell>
                          <div className="space-y-1">
                            <div className="text-sm">
                              {formatHosts(ingress.hosts)}
                            </div>
                            <div className="text-xs text-gray-500">
                              {getTLSStatus(ingress.tls)}
                            </div>
                          </div>
                        </TableCell>
                        <TableCell>
                          <span className="text-sm">{formatAddresses(ingress.addresses)}</span>
                        </TableCell>
                        <TableCell>
                          <div className="flex flex-wrap gap-1">
                            {ingress.ports.map((port, idx) => (
                              <Badge key={idx} variant="outline" className="text-xs">
                                {port.port}/{port.protocol}
                              </Badge>
                            ))}
                          </div>
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center space-x-2">
                            <statusDisplay.icon className={`w-4 h-4 ${statusDisplay.color}`} />
                            <span className="text-sm">{ingress.status}</span>
                          </div>
                        </TableCell>
                        <TableCell>
                          <div className="text-sm text-gray-600">
                            <div>{getAge(ingress.creationTimestamp)}</div>
                          </div>
                        </TableCell>
                        <TableCell>
                          <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                              <Button variant="ghost" size="sm">
                                <MoreHorizontal className="w-4 h-4" />
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                              <DropdownMenuItem onClick={() => handleIngressAction('view', ingress)}>
                                <Eye className="w-4 h-4 mr-2" />
                                查看详情
                              </DropdownMenuItem>
                              <DropdownMenuItem onClick={() => handleIngressAction('edit', ingress)}>
                                <Edit className="w-4 h-4 mr-2" />
                                编辑
                              </DropdownMenuItem>
                              <DropdownMenuSeparator />
                              <DropdownMenuItem 
                                onClick={() => handleIngressAction('delete', ingress)}
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

          {filteredIngresses.length === 0 && !loading && (
            <div className="text-center py-8">
              <Globe className="w-12 h-12 text-gray-400 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">暂无Ingresses</h3>
              <p className="text-gray-500 mb-4">
                {searchTerm ? '没有找到匹配的Ingresses' : '当前命名空间中没有Ingresses'}
              </p>
              {!searchTerm && (
                <Button>
                  <Globe className="w-4 h-4 mr-2" />
                  创建第一个Ingress
                </Button>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Ingress详情模态框 */}
      {showDetails && selectedIngress && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
          onClick={() => setShowDetails(false)}
        >
          <motion.div
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0.9, opacity: 0 }}
            className="bg-white rounded-lg shadow-xl max-w-6xl w-full mx-4 max-h-[90vh] overflow-hidden"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="p-6 border-b">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-xl font-semibold">{selectedIngress.name}</h2>
                  <p className="text-gray-600">Ingress详细信息</p>
                </div>
                <Button variant="ghost" onClick={() => setShowDetails(false)}>
                  ✕
                </Button>
              </div>
            </div>
            <div className="p-6 overflow-y-auto" style={{ maxHeight: 'calc(90vh - 140px)' }}>
              <Tabs defaultValue="overview">
                <TabsList>
                  <TabsTrigger value="overview">概览</TabsTrigger>
                  <TabsTrigger value="rules">路由规则</TabsTrigger>
                  <TabsTrigger value="tls">TLS配置</TabsTrigger>
                  <TabsTrigger value="annotations">注解</TabsTrigger>
                </TabsList>
                <TabsContent value="overview" className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <Card>
                      <CardHeader>
                        <CardTitle className="text-sm">基本信息</CardTitle>
                      </CardHeader>
                      <CardContent className="grid grid-cols-2 gap-4">
                        <div>
                          <label className="text-xs font-medium text-gray-500">名称</label>
                          <p className="text-sm">{selectedIngress.name}</p>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">命名空间</label>
                          <p className="text-sm">{selectedIngress.namespace}</p>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">Ingress类</label>
                          <p className="text-sm">{selectedIngress.className || '默认'}</p>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">状态</label>
                          <div className="flex items-center space-x-2">
                            {(() => {
                              const display = getStatusDisplay(selectedIngress.status);
                              return (
                                <>
                                  <display.icon className={`w-4 h-4 ${display.color}`} />
                                  <span className="text-sm">{selectedIngress.status}</span>
                                </>
                              );
                            })()}
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                    <Card>
                      <CardHeader>
                        <CardTitle className="text-sm">网络信息</CardTitle>
                      </CardHeader>
                      <CardContent className="space-y-3">
                        <div>
                          <label className="text-xs font-medium text-gray-500">主机列表</label>
                          <div className="mt-1">
                            {selectedIngress.hosts.length > 0 ? (
                              <div className="space-y-1">
                                {selectedIngress.hosts.map((host, idx) => (
                                  <div key={idx} className="text-sm bg-gray-100 px-2 py-1 rounded">
                                    {host}
                                  </div>
                                ))}
                              </div>
                            ) : (
                              <p className="text-sm text-gray-500">所有主机 (*)</p>
                            )}
                          </div>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">负载均衡地址</label>
                          <div className="mt-1">
                            {selectedIngress.addresses.length > 0 ? (
                              selectedIngress.addresses.map((addr, idx) => (
                                <div key={idx} className="text-sm">{addr}</div>
                              ))
                            ) : (
                              <p className="text-sm text-gray-500">未分配</p>
                            )}
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  </div>
                </TabsContent>
                <TabsContent value="rules">
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-sm">路由规则</CardTitle>
                    </CardHeader>
                    <CardContent>
                      {selectedIngress.rules.length > 0 ? (
                        <div className="space-y-4">
                          {selectedIngress.rules.map((rule, ruleIdx) => (
                            <div key={ruleIdx} className="border rounded-lg p-4">
                              <div className="flex items-center space-x-2 mb-3">
                                <Route className="w-4 h-4 text-blue-500" />
                                <span className="font-medium text-sm">
                                  {rule.host || '*'}
                                </span>
                              </div>
                              {rule.paths && rule.paths.length > 0 && (
                                <div className="space-y-2">
                                  {rule.paths.map((path, pathIdx) => (
                                    <div key={pathIdx} className="flex items-center justify-between p-2 bg-gray-50 rounded">
                                      <div className="flex items-center space-x-2">
                                        <span className="text-sm font-mono">{path.path}</span>
                                        <Badge variant="outline" className="text-xs">
                                          {path.pathType}
                                        </Badge>
                                      </div>
                                      <div className="flex items-center space-x-2">
                                        <Link className="w-4 h-4 text-gray-400" />
                                        <span className="text-sm">
                                          {path.backend.serviceName}:{path.backend.servicePort}
                                        </span>
                                      </div>
                                    </div>
                                  ))}
                                </div>
                              )}
                            </div>
                          ))}
                        </div>
                      ) : (
                        <p className="text-sm text-gray-500">无路由规则</p>
                      )}
                    </CardContent>
                  </Card>
                </TabsContent>
                <TabsContent value="tls">
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-sm">TLS配置</CardTitle>
                    </CardHeader>
                    <CardContent>
                      {selectedIngress.tls.length > 0 ? (
                        <div className="space-y-4">
                          {selectedIngress.tls.map((tlsConfig, idx) => (
                            <div key={idx} className="border rounded-lg p-4">
                              <div className="flex items-center space-x-2 mb-3">
                                <Shield className="w-4 h-4 text-green-500" />
                                <span className="font-medium text-sm">
                                  证书: {tlsConfig.secretName}
                                </span>
                              </div>
                              <div>
                                <label className="text-xs font-medium text-gray-500">保护的主机</label>
                                <div className="mt-1 space-y-1">
                                  {tlsConfig.hosts.map((host, hostIdx) => (
                                    <div key={hostIdx} className="text-sm bg-green-50 px-2 py-1 rounded">
                                      {host}
                                    </div>
                                  ))}
                                </div>
                              </div>
                            </div>
                          ))}
                        </div>
                      ) : (
                        <p className="text-sm text-gray-500">未配置TLS</p>
                      )}
                    </CardContent>
                  </Card>
                </TabsContent>
                <TabsContent value="annotations">
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-sm">注解</CardTitle>
                    </CardHeader>
                    <CardContent>
                      {Object.keys(selectedIngress.annotations).length > 0 ? (
                        <div className="space-y-3">
                          {Object.entries(selectedIngress.annotations).map(([key, value]) => (
                            <div key={key} className="border rounded-lg p-3">
                              <div className="font-medium text-sm mb-1">{key}</div>
                              <div className="text-sm text-gray-600 font-mono bg-gray-100 p-2 rounded">
                                {value}
                              </div>
                            </div>
                          ))}
                        </div>
                      ) : (
                        <p className="text-sm text-gray-500">无注解配置</p>
                      )}
                    </CardContent>
                  </Card>
                </TabsContent>
              </Tabs>
            </div>
          </motion.div>
        </motion.div>
      )}
    </div>
  );
};

export default IngressesDashboard;
