/**
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 * @description Kubernetes StatefulSets管理面板
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
  RotateCcw,
  Database,
  Layers,
  CheckCircle,
  AlertCircle,
  Clock,
  Box,
  ChevronDown,
  ChevronRight,
  Activity,
  HardDrive,
  Network,
  ExternalLink
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

import { KubernetesAPI, K8sResource, K8sResourceListResponse } from '@/lib/kubernetes-api';

interface StatefulSetsDashboardProps {
  clusterId: string;
  namespace: string;
  className?: string;
}

interface StatefulSet {
  name: string;
  namespace: string;
  replicas: {
    desired: number;
    current: number;
    ready: number;
  };
  serviceName: string;
  strategy: string;
  creationTimestamp: string;
  age: string;
  status: 'Ready' | 'Updating' | 'Failed' | 'Pending';
  updateRevision: string;
}

const StatefulSetsDashboard: React.FC<StatefulSetsDashboardProps> = ({
  clusterId,
  namespace,
  className
}) => {
  const [statefulSets, setStatefulSets] = useState<StatefulSet[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("all");
  const [selectedStatefulSet, setSelectedStatefulSet] = useState<StatefulSet | null>(null);
  const [showDetails, setShowDetails] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize] = useState(20);
  const [total, setTotal] = useState(0);

  // 筛选和搜索StatefulSets
  const filteredStatefulSets = useMemo(() => {
    return statefulSets.filter(sts => {
      const matchesSearch = 
        sts.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        sts.namespace.toLowerCase().includes(searchTerm.toLowerCase()) ||
        sts.serviceName.toLowerCase().includes(searchTerm.toLowerCase());

      const matchesStatus = statusFilter === "all" || sts.status.toLowerCase() === statusFilter.toLowerCase();

      return matchesSearch && matchesStatus;
    });
  }, [statefulSets, searchTerm, statusFilter]);

  // 统计信息
  const stats = useMemo(() => {
    return {
      total: statefulSets.length,
      ready: statefulSets.filter(sts => sts.status === 'Ready').length,
      updating: statefulSets.filter(sts => sts.status === 'Updating').length,
      failed: statefulSets.filter(sts => sts.status === 'Failed').length,
      totalReplicas: statefulSets.reduce((sum, sts) => sum + sts.replicas.desired, 0),
      readyReplicas: statefulSets.reduce((sum, sts) => sum + sts.replicas.ready, 0)
    };
  }, [statefulSets]);

  // 获取StatefulSets数据
  const fetchStatefulSets = async () => {
    if (!clusterId) return;
    
    setLoading(true);
    setError(null);
    try {
      const response: K8sResourceListResponse = await KubernetesAPI.getStatefulSets(
        clusterId,
        namespace || undefined,
        pageNum,
        pageSize
      );

      // 转换API响应为组件需要的StatefulSet格式
      const convertedStatefulSets: StatefulSet[] = response.data.map((resource: K8sResource) => {
        const spec = resource.spec as any;
        const status = resource.metadata as any;
        
        return {
          name: resource.name,
          namespace: resource.namespace,
          replicas: {
            desired: spec?.replicas || 1,
            current: status?.currentReplicas || 0,
            ready: status?.readyReplicas || 0
          },
          serviceName: spec?.serviceName || 'N/A',
          strategy: spec?.updateStrategy?.type || 'RollingUpdate',
          creationTimestamp: resource.creationTimestamp,
          age: resource.age || '-',
          status: determineStatefulSetStatus(
            spec?.replicas || 1,
            status?.currentReplicas || 0,
            status?.readyReplicas || 0,
            status?.updatedReplicas || 0
          ),
          updateRevision: status?.updateRevision || 'N/A'
        };
      });

      setStatefulSets(convertedStatefulSets);
      setTotal(response.total || convertedStatefulSets.length);
    } catch (error) {
      console.error('获取StatefulSets失败:', error);
      setError(error instanceof Error ? error.message : '获取StatefulSets失败');
      setStatefulSets([]);
    } finally {
      setLoading(false);
    }
  };

  // 确定StatefulSet状态
  const determineStatefulSetStatus = (
    desired: number,
    current: number, 
    ready: number,
    updated: number
  ): 'Ready' | 'Updating' | 'Failed' | 'Pending' => {
    if (ready === desired && current === desired) return 'Ready';
    if (updated < desired || current !== desired) return 'Updating';
    if (ready === 0 && current > 0) return 'Failed';
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
      'Updating': { color: 'text-blue-600', bgColor: 'bg-blue-100', icon: Activity },
      'Failed': { color: 'text-red-600', bgColor: 'bg-red-100', icon: AlertCircle },
      'Pending': { color: 'text-yellow-600', bgColor: 'bg-yellow-100', icon: Clock }
    };
    return displays[status as keyof typeof displays] || displays['Pending'];
  };

  // 计算就绪率
  const getReadyPercentage = (ready: number, desired: number): number => {
    if (desired === 0) return 0;
    return Math.round((ready / desired) * 100);
  };

  // 刷新数据
  const handleRefresh = async () => {
    await fetchStatefulSets();
  };

  // StatefulSet操作
  const handleStatefulSetAction = (action: string, sts: StatefulSet) => {
    console.log(`执行操作: ${action} on StatefulSet: ${sts.name}`);
    switch (action) {
      case 'view':
        setSelectedStatefulSet(sts);
        setShowDetails(true);
        break;
      case 'scale':
        // 实现扩缩容逻辑
        break;
      case 'restart':
        // 实现重启逻辑
        break;
      case 'delete':
        // 实现删除逻辑
        break;
    }
  };

  // 组件挂载和依赖更新时获取数据
  useEffect(() => {
    fetchStatefulSets();
  }, [clusterId, namespace, pageNum]);

  if (loading && statefulSets.length === 0) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex items-center space-x-2">
          <RefreshCw className="w-6 h-6 animate-spin text-blue-500" />
          <span className="text-gray-600">加载StatefulSets...</span>
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
            description: "StatefulSets总数"
          },
          { 
            title: "就绪", 
            count: stats.ready, 
            color: "green", 
            icon: CheckCircle,
            description: "就绪状态"
          },
          { 
            title: "更新中", 
            count: stats.updating, 
            color: "yellow", 
            icon: Activity,
            description: "正在更新"
          },
          { 
            title: "副本就绪率", 
            count: `${stats.readyReplicas}/${stats.totalReplicas}`, 
            color: "purple", 
            icon: Database,
            description: "总副本就绪情况",
            isText: true
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
              <CardTitle className="text-lg">StatefulSets</CardTitle>
              <CardDescription>管理Kubernetes有状态应用</CardDescription>
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
                <Database className="w-4 h-4 mr-2" />
                新建StatefulSet
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
                placeholder="搜索StatefulSets..."
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
                <SelectItem value="ready">就绪</SelectItem>
                <SelectItem value="updating">更新中</SelectItem>
                <SelectItem value="failed">失败</SelectItem>
                <SelectItem value="pending">等待中</SelectItem>
              </SelectContent>
            </Select>
            <Button variant="outline" size="sm">
              <Download className="w-4 h-4 mr-2" />
              导出
            </Button>
          </div>

          {/* StatefulSets表格 */}
          <div className="border rounded-lg">
            <Table>
              <TableHeader>
                <TableRow className="bg-gray-50">
                  <TableHead>名称</TableHead>
                  <TableHead>命名空间</TableHead>
                  <TableHead>副本状态</TableHead>
                  <TableHead>就绪率</TableHead>
                  <TableHead>服务名</TableHead>
                  <TableHead>更新策略</TableHead>
                  <TableHead>创建时间</TableHead>
                  <TableHead>操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <AnimatePresence>
                  {filteredStatefulSets.map((sts, index) => {
                    const statusDisplay = getStatusDisplay(sts.status);
                    const readyPercentage = getReadyPercentage(sts.replicas.ready, sts.replicas.desired);
                    return (
                      <motion.tr
                        key={sts.name}
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ delay: index * 0.05 }}
                        className="hover:bg-gray-50 transition-colors duration-200"
                      >
                        <TableCell>
                          <div className="flex items-center space-x-3">
                            <div className="w-8 h-8 bg-blue-100 rounded-lg flex items-center justify-center">
                              <Database className="w-4 h-4 text-blue-600" />
                            </div>
                            <div>
                              <div className="font-medium text-gray-900">{sts.name}</div>
                              <div className="text-xs text-gray-500">{sts.updateRevision}</div>
                            </div>
                          </div>
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline" className="text-xs">
                            {sts.namespace}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center space-x-2">
                            <statusDisplay.icon className={`w-4 h-4 ${statusDisplay.color}`} />
                            <span className="text-sm">
                              {sts.replicas.ready}/{sts.replicas.desired}
                            </span>
                          </div>
                        </TableCell>
                        <TableCell>
                          <div className="space-y-1">
                            <div className="flex items-center justify-between text-xs text-gray-600">
                              <span>{readyPercentage}%</span>
                            </div>
                            <Progress value={readyPercentage} className="h-1.5" />
                          </div>
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center space-x-2">
                            <Network className="w-4 h-4 text-gray-400" />
                            <span className="text-sm text-gray-600">{sts.serviceName}</span>
                          </div>
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline" className="text-xs">
                            {sts.strategy}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <div className="text-sm text-gray-600">
                            <div>{getAge(sts.creationTimestamp)}</div>
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
                              <DropdownMenuItem onClick={() => handleStatefulSetAction('view', sts)}>
                                <Eye className="w-4 h-4 mr-2" />
                                查看详情
                              </DropdownMenuItem>
                              <DropdownMenuItem onClick={() => handleStatefulSetAction('scale', sts)}>
                                <Layers className="w-4 h-4 mr-2" />
                                扩缩容
                              </DropdownMenuItem>
                              <DropdownMenuItem onClick={() => handleStatefulSetAction('restart', sts)}>
                                <RotateCcw className="w-4 h-4 mr-2" />
                                重启
                              </DropdownMenuItem>
                              <DropdownMenuSeparator />
                              <DropdownMenuItem 
                                onClick={() => handleStatefulSetAction('delete', sts)}
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

          {filteredStatefulSets.length === 0 && !loading && (
            <div className="text-center py-8">
              <Database className="w-12 h-12 text-gray-400 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">暂无StatefulSets</h3>
              <p className="text-gray-500 mb-4">
                {searchTerm ? '没有找到匹配的StatefulSets' : '当前命名空间中没有StatefulSets'}
              </p>
              {!searchTerm && (
                <Button>
                  <Database className="w-4 h-4 mr-2" />
                  创建第一个StatefulSet
                </Button>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      {/* StatefulSet详情模态框 */}
      {showDetails && selectedStatefulSet && (
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
                  <h2 className="text-xl font-semibold">{selectedStatefulSet.name}</h2>
                  <p className="text-gray-600">StatefulSet详细信息</p>
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
                  <TabsTrigger value="replicas">副本</TabsTrigger>
                  <TabsTrigger value="metadata">元数据</TabsTrigger>
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
                          <p className="text-sm">{selectedStatefulSet.name}</p>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">命名空间</label>
                          <p className="text-sm">{selectedStatefulSet.namespace}</p>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">服务名</label>
                          <p className="text-sm">{selectedStatefulSet.serviceName}</p>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">更新策略</label>
                          <p className="text-sm">{selectedStatefulSet.strategy}</p>
                        </div>
                      </CardContent>
                    </Card>
                    <Card>
                      <CardHeader>
                        <CardTitle className="text-sm">状态信息</CardTitle>
                      </CardHeader>
                      <CardContent className="space-y-3">
                        <div>
                          <div className="flex items-center justify-between text-xs text-gray-600 mb-1">
                            <span>副本就绪率</span>
                            <span>{getReadyPercentage(selectedStatefulSet.replicas.ready, selectedStatefulSet.replicas.desired)}%</span>
                          </div>
                          <Progress value={getReadyPercentage(selectedStatefulSet.replicas.ready, selectedStatefulSet.replicas.desired)} className="h-2" />
                        </div>
                        <div className="grid grid-cols-3 gap-2 text-center">
                          <div>
                            <p className="text-xs text-gray-500">期望</p>
                            <p className="text-sm font-medium">{selectedStatefulSet.replicas.desired}</p>
                          </div>
                          <div>
                            <p className="text-xs text-gray-500">当前</p>
                            <p className="text-sm font-medium">{selectedStatefulSet.replicas.current}</p>
                          </div>
                          <div>
                            <p className="text-xs text-gray-500">就绪</p>
                            <p className="text-sm font-medium">{selectedStatefulSet.replicas.ready}</p>
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  </div>
                </TabsContent>
                <TabsContent value="replicas">
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-sm">副本状态详情</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="space-y-4">
                        <div className="grid grid-cols-4 gap-4 text-center">
                          <div className="p-3 bg-blue-50 rounded-lg">
                            <p className="text-xs text-blue-600 font-medium">期望副本</p>
                            <p className="text-2xl font-bold text-blue-700">{selectedStatefulSet.replicas.desired}</p>
                          </div>
                          <div className="p-3 bg-green-50 rounded-lg">
                            <p className="text-xs text-green-600 font-medium">当前副本</p>
                            <p className="text-2xl font-bold text-green-700">{selectedStatefulSet.replicas.current}</p>
                          </div>
                          <div className="p-3 bg-purple-50 rounded-lg">
                            <p className="text-xs text-purple-600 font-medium">就绪副本</p>
                            <p className="text-2xl font-bold text-purple-700">{selectedStatefulSet.replicas.ready}</p>
                          </div>
                          <div className="p-3 bg-gray-50 rounded-lg">
                            <p className="text-xs text-gray-600 font-medium">就绪率</p>
                            <p className="text-2xl font-bold text-gray-700">
                              {getReadyPercentage(selectedStatefulSet.replicas.ready, selectedStatefulSet.replicas.desired)}%
                            </p>
                          </div>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                </TabsContent>
                <TabsContent value="metadata">
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-sm">元数据</CardTitle>
                    </CardHeader>
                    <CardContent className="grid grid-cols-2 gap-4">
                      <div>
                        <label className="text-xs font-medium text-gray-500">创建时间</label>
                        <p className="text-sm">{new Date(selectedStatefulSet.creationTimestamp).toLocaleString()}</p>
                      </div>
                      <div>
                        <label className="text-xs font-medium text-gray-500">年龄</label>
                        <p className="text-sm">{getAge(selectedStatefulSet.creationTimestamp)}</p>
                      </div>
                      <div>
                        <label className="text-xs font-medium text-gray-500">更新版本</label>
                        <p className="text-sm font-mono">{selectedStatefulSet.updateRevision}</p>
                      </div>
                      <div>
                        <label className="text-xs font-medium text-gray-500">状态</label>
                        <div className="flex items-center space-x-2">
                          {(() => {
                            const display = getStatusDisplay(selectedStatefulSet.status);
                            return (
                              <>
                                <display.icon className={`w-4 h-4 ${display.color}`} />
                                <span className="text-sm">{selectedStatefulSet.status}</span>
                              </>
                            );
                          })()}
                        </div>
                      </div>
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

export default StatefulSetsDashboard;
