/**
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 * @description Kubernetes IngressClasses管理面板
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
  Settings,
  ExternalLink,
  Route,
  Layers
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

interface IngressClassesDashboardProps {
  clusterId: string;
  namespace: string;
  className?: string;
}

interface IngressClass {
  name: string;
  controller: string;
  parameters?: {
    apiGroup?: string;
    kind: string;
    name: string;
    namespace?: string;
    scope?: string;
  };
  isDefault: boolean;
  age: string;
  creationTimestamp: string;
  usageCount: number; // 使用此IngressClass的Ingress数量
  annotations: Record<string, string>;
  description?: string;
}

const IngressClassesDashboard: React.FC<IngressClassesDashboardProps> = ({
  clusterId,
  namespace,
  className
}) => {
  const [ingressClasses, setIngressClasses] = useState<IngressClass[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [controllerFilter, setControllerFilter] = useState<string>("all");
  const [selectedIC, setSelectedIC] = useState<IngressClass | null>(null);
  const [showDetails, setShowDetails] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize] = useState(20);
  const [total, setTotal] = useState(0);

  // 筛选和搜索IngressClasses
  const filteredICs = useMemo(() => {
    return ingressClasses.filter(ic => {
      const matchesSearch = 
        ic.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        ic.controller.toLowerCase().includes(searchTerm.toLowerCase()) ||
        (ic.description && ic.description.toLowerCase().includes(searchTerm.toLowerCase()));

      const matchesController = controllerFilter === "all" || 
        ic.controller.toLowerCase().includes(controllerFilter.toLowerCase());

      return matchesSearch && matchesController;
    });
  }, [ingressClasses, searchTerm, controllerFilter]);

  // 统计信息
  const stats = useMemo(() => {
    const controllers = [...new Set(ingressClasses.map(ic => ic.controller))].length;
    const defaultIngressClass = ingressClasses.find(ic => ic.isDefault);
    
    return {
      total: ingressClasses.length,
      hasDefault: ingressClasses.filter(ic => ic.isDefault).length,
      withParameters: ingressClasses.filter(ic => ic.parameters).length,
      controllers,
      totalUsage: ingressClasses.reduce((sum, ic) => sum + ic.usageCount, 0),
      defaultName: defaultIngressClass?.name || 'None'
    };
  }, [ingressClasses]);

  // 获取IngressClasses数据
  const fetchIngressClasses = async () => {
    if (!clusterId) return;
    
    setLoading(true);
    setError(null);
    try {
      const response: K8sResourceListResponse = await KubernetesAPI.getIngressClasses(
        clusterId,
        pageNum,
        pageSize
      );

      // 转换API响应为组件需要的IngressClass格式
      const convertedICs: IngressClass[] = response.data.map((resource: K8sResource) => {
        const spec = resource.spec as any;
        const metadata = resource.metadata as any;
        
        return {
          name: resource.name,
          controller: spec?.controller || 'unknown',
          parameters: spec?.parameters,
          isDefault: checkIsDefault(metadata?.annotations),
          age: resource.age || '-',
          creationTimestamp: resource.creationTimestamp,
          usageCount: generateUsageCount(), // 实际应从API获取
          annotations: metadata?.annotations || {},
          description: metadata?.annotations?.['kubernetes.io/description'] || 
                      metadata?.annotations?.['ingress.class/description']
        };
      });

      setIngressClasses(convertedICs);
      setTotal(response.total || convertedICs.length);
    } catch (error) {
      console.error('获取IngressClasses失败:', error);
      setError(error instanceof Error ? error.message : '获取IngressClasses失败');
      setIngressClasses([]);
    } finally {
      setLoading(false);
    }
  };

  // 检查是否为默认IngressClass
  const checkIsDefault = (annotations: Record<string, string> = {}): boolean => {
    return annotations['ingressclass.kubernetes.io/is-default-class'] === 'true';
  };

  // 生成模拟使用数量（实际应从API获取相关Ingress数量）
  const generateUsageCount = (): number => {
    return Math.floor(Math.random() * 15);
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

  // 获取控制器显示名称
  const getControllerDisplayName = (controller: string): string => {
    const controllerMap: Record<string, string> = {
      'nginx.org/ingress-controller': 'NGINX',
      'k8s.io/ingress-nginx': 'Ingress NGINX',
      'traefik.io/ingress-controller': 'Traefik',
      'istio.io/ingress': 'Istio',
      'kong': 'Kong',
      'haproxy-ingress.github.io/controller': 'HAProxy',
      'aws-load-balancer-controller': 'AWS ALB',
      'gce-controller': 'GCE'
    };
    
    return controllerMap[controller] || controller.split('/').pop() || controller;
  };

  // 获取控制器图标颜色
  const getControllerColor = (controller: string): string => {
    if (controller.includes('nginx')) return 'text-green-600';
    if (controller.includes('traefik')) return 'text-blue-600';
    if (controller.includes('istio')) return 'text-purple-600';
    if (controller.includes('kong')) return 'text-orange-600';
    if (controller.includes('aws')) return 'text-yellow-600';
    if (controller.includes('gce')) return 'text-red-600';
    return 'text-gray-600';
  };

  // 刷新数据
  const handleRefresh = async () => {
    await fetchIngressClasses();
  };

  // IngressClass操作
  const handleICAction = (action: string, ic: IngressClass) => {
    console.log(`执行操作: ${action} on IngressClass: ${ic.name}`);
    switch (action) {
      case 'view':
        setSelectedIC(ic);
        setShowDetails(true);
        break;
      case 'setDefault':
        // 实现设为默认逻辑
        break;
      case 'edit':
        // 实现编辑逻辑
        break;
      case 'delete':
        // 实现删除逻辑
        break;
    }
  };

  // 获取可用的控制器类型
  const controllerTypes = useMemo(() => {
    const types = [...new Set(ingressClasses.map(ic => getControllerDisplayName(ic.controller)))];
    return types;
  }, [ingressClasses]);

  // 组件挂载和依赖更新时获取数据
  useEffect(() => {
    fetchIngressClasses();
  }, [clusterId, pageNum]);

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
          { 
            title: "总计", 
            count: stats.total, 
            color: "blue", 
            icon: Box,
            description: "IngressClass总数"
          },
          { 
            title: "控制器类型", 
            count: stats.controllers, 
            color: "green", 
            icon: Network,
            description: "不同控制器类型"
          },
          { 
            title: "配置参数", 
            count: stats.withParameters, 
            color: "purple", 
            icon: Settings,
            description: "有自定义参数"
          },
          { 
            title: "总使用量", 
            count: stats.totalUsage, 
            color: "orange", 
            icon: Route,
            description: "Ingress使用总数"
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
              <CardTitle className="text-lg">IngressClasses</CardTitle>
              <CardDescription>管理Kubernetes入口控制器类</CardDescription>
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
                <Layers className="w-4 h-4 mr-2" />
                新建IngressClass
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
                placeholder="搜索IngressClasses..."
                className="pl-10"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
            <Select value={controllerFilter} onValueChange={setControllerFilter}>
              <SelectTrigger className="w-40">
                <SelectValue placeholder="控制器" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">所有控制器</SelectItem>
                {controllerTypes.map(type => (
                  <SelectItem key={type} value={type.toLowerCase()}>
                    {type}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Button variant="outline" size="sm">
              <Download className="w-4 h-4 mr-2" />
              导出
            </Button>
          </div>

          {/* 默认IngressClass提示 */}
          {stats.hasDefault > 0 && (
            <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg">
              <div className="flex items-center space-x-2">
                <CheckCircle className="w-4 h-4 text-blue-500" />
                <span className="text-sm text-blue-700">
                  默认IngressClass: <span className="font-medium">{stats.defaultName}</span>
                </span>
              </div>
            </div>
          )}

          {/* IngressClasses表格 */}
          <div className="border rounded-lg">
            <Table>
              <TableHeader>
                <TableRow className="bg-gray-50">
                  <TableHead>名称</TableHead>
                  <TableHead>控制器</TableHead>
                  <TableHead>参数配置</TableHead>
                  <TableHead>使用量</TableHead>
                  <TableHead>描述</TableHead>
                  <TableHead>创建时间</TableHead>
                  <TableHead>操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <AnimatePresence>
                  {filteredICs.map((ic, index) => (
                    <motion.tr
                      key={ic.name}
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      exit={{ opacity: 0 }}
                      transition={{ delay: index * 0.05 }}
                      className="hover:bg-gray-50 transition-colors duration-200"
                    >
                      <TableCell>
                        <div className="flex items-center space-x-3">
                          <div className="w-8 h-8 bg-cyan-100 rounded-lg flex items-center justify-center">
                            <Layers className="w-4 h-4 text-cyan-600" />
                          </div>
                          <div>
                            <div className="flex items-center space-x-2">
                              <span className="font-medium text-gray-900">{ic.name}</span>
                              {ic.isDefault && (
                                <Badge className="text-xs bg-blue-100 text-blue-700">
                                  默认
                                </Badge>
                              )}
                            </div>
                            <div className="text-xs text-gray-500">
                              {ic.annotations ? Object.keys(ic.annotations).length : 0} 个注解
                            </div>
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="space-y-1">
                          <div className={`text-sm font-medium ${getControllerColor(ic.controller)}`}>
                            {getControllerDisplayName(ic.controller)}
                          </div>
                          <div className="text-xs text-gray-500 font-mono">
                            {ic.controller}
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        {ic.parameters ? (
                          <div className="space-y-1">
                            <Badge variant="outline" className="text-xs">
                              {ic.parameters.kind}
                            </Badge>
                            <div className="text-xs text-gray-500">
                              {ic.parameters.name}
                            </div>
                          </div>
                        ) : (
                          <span className="text-xs text-gray-400">无参数</span>
                        )}
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center space-x-2">
                          <Route className="w-4 h-4 text-gray-400" />
                          <span className="text-sm">{ic.usageCount}</span>
                        </div>
                      </TableCell>
                      <TableCell>
                        <span className="text-sm text-gray-600 max-w-32 truncate block" title={ic.description}>
                          {ic.description || '无描述'}
                        </span>
                      </TableCell>
                      <TableCell>
                        <div className="text-sm text-gray-600">
                          <div>{getAge(ic.creationTimestamp)}</div>
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
                            <DropdownMenuItem onClick={() => handleICAction('view', ic)}>
                              <Eye className="w-4 h-4 mr-2" />
                              查看详情
                            </DropdownMenuItem>
                            {!ic.isDefault && (
                              <DropdownMenuItem onClick={() => handleICAction('setDefault', ic)}>
                                <CheckCircle className="w-4 h-4 mr-2" />
                                设为默认
                              </DropdownMenuItem>
                            )}
                            <DropdownMenuItem onClick={() => handleICAction('edit', ic)}>
                              <Edit className="w-4 h-4 mr-2" />
                              编辑
                            </DropdownMenuItem>
                            <DropdownMenuSeparator />
                            <DropdownMenuItem 
                              onClick={() => handleICAction('delete', ic)}
                              className="text-red-600"
                              disabled={ic.usageCount > 0}
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

          {filteredICs.length === 0 && !loading && (
            <div className="text-center py-8">
              <Layers className="w-12 h-12 text-gray-400 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">暂无IngressClasses</h3>
              <p className="text-gray-500 mb-4">
                {searchTerm ? '没有找到匹配的IngressClasses' : '集群中没有IngressClasses'}
              </p>
              {!searchTerm && (
                <Button>
                  <Layers className="w-4 h-4 mr-2" />
                  创建第一个IngressClass
                </Button>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      {/* IngressClass详情模态框 */}
      {showDetails && selectedIC && (
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
            className="bg-white rounded-lg shadow-xl max-w-4xl w-full mx-4 max-h-[90vh] overflow-hidden"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="p-6 border-b">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-xl font-semibold">{selectedIC.name}</h2>
                  <p className="text-gray-600">IngressClass详细信息</p>
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
                  <TabsTrigger value="parameters">参数配置</TabsTrigger>
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
                          <p className="text-sm">{selectedIC.name}</p>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">控制器</label>
                          <div className="space-y-1">
                            <p className={`text-sm font-medium ${getControllerColor(selectedIC.controller)}`}>
                              {getControllerDisplayName(selectedIC.controller)}
                            </p>
                            <p className="text-xs text-gray-500 font-mono">{selectedIC.controller}</p>
                          </div>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">默认类</label>
                          <div className="flex items-center space-x-2">
                            {selectedIC.isDefault ? (
                              <>
                                <CheckCircle className="w-4 h-4 text-green-500" />
                                <span className="text-sm text-green-600">是</span>
                              </>
                            ) : (
                              <>
                                <AlertCircle className="w-4 h-4 text-gray-400" />
                                <span className="text-sm text-gray-600">否</span>
                              </>
                            )}
                          </div>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">使用量</label>
                          <div className="flex items-center space-x-2">
                            <Route className="w-4 h-4 text-gray-400" />
                            <span className="text-sm">{selectedIC.usageCount} Ingresses</span>
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                    <Card>
                      <CardHeader>
                        <CardTitle className="text-sm">控制器信息</CardTitle>
                      </CardHeader>
                      <CardContent className="space-y-3">
                        {selectedIC.description && (
                          <div>
                            <label className="text-xs font-medium text-gray-500">描述</label>
                            <p className="text-sm text-gray-600 mt-1">{selectedIC.description}</p>
                          </div>
                        )}
                        <div className="grid grid-cols-2 gap-2 text-center">
                          <div className="p-2 bg-gray-50 rounded">
                            <p className="text-xs text-gray-500">注解数量</p>
                            <p className="text-sm font-medium">{Object.keys(selectedIC.annotations).length}</p>
                          </div>
                          <div className="p-2 bg-gray-50 rounded">
                            <p className="text-xs text-gray-500">参数配置</p>
                            <p className="text-sm font-medium">{selectedIC.parameters ? '有' : '无'}</p>
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  </div>
                </TabsContent>
                <TabsContent value="parameters">
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-sm">参数配置</CardTitle>
                    </CardHeader>
                    <CardContent>
                      {selectedIC.parameters ? (
                        <div className="space-y-4">
                          <div className="p-4 border rounded-lg">
                            <div className="grid grid-cols-2 gap-4">
                              {selectedIC.parameters.apiGroup && (
                                <div>
                                  <label className="text-xs font-medium text-gray-500">API Group</label>
                                  <p className="text-sm font-mono">{selectedIC.parameters.apiGroup}</p>
                                </div>
                              )}
                              <div>
                                <label className="text-xs font-medium text-gray-500">Kind</label>
                                <p className="text-sm font-mono">{selectedIC.parameters.kind}</p>
                              </div>
                              <div>
                                <label className="text-xs font-medium text-gray-500">Name</label>
                                <p className="text-sm font-mono">{selectedIC.parameters.name}</p>
                              </div>
                              {selectedIC.parameters.namespace && (
                                <div>
                                  <label className="text-xs font-medium text-gray-500">Namespace</label>
                                  <p className="text-sm font-mono">{selectedIC.parameters.namespace}</p>
                                </div>
                              )}
                              {selectedIC.parameters.scope && (
                                <div>
                                  <label className="text-xs font-medium text-gray-500">Scope</label>
                                  <p className="text-sm font-mono">{selectedIC.parameters.scope}</p>
                                </div>
                              )}
                            </div>
                          </div>
                        </div>
                      ) : (
                        <p className="text-sm text-gray-500">无参数配置</p>
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
                      {Object.keys(selectedIC.annotations).length > 0 ? (
                        <div className="space-y-3">
                          {Object.entries(selectedIC.annotations).map(([key, value]) => (
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

export default IngressClassesDashboard;
