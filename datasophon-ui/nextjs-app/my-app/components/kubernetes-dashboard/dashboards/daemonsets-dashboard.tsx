/**
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 * @description Kubernetes DaemonSets管理面板
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
  Monitor,
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
  ExternalLink,
  Server
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

interface DaemonSetsDashboardProps {
  clusterId: string;
  namespace: string;
  className?: string;
}

interface DaemonSet {
  name: string;
  namespace: string;
  desired: number;
  current: number;
  ready: number;
  upToDate: number;
  available: number;
  nodeSelector: Record<string, string>;
  creationTimestamp: string;
  age: string;
  status: 'Ready' | 'Updating' | 'Failed' | 'Pending';
  updateStrategy: string;
}

const DaemonSetsDashboard: React.FC<DaemonSetsDashboardProps> = ({
  clusterId,
  namespace,
  className
}) => {
  const [daemonSets, setDaemonSets] = useState<DaemonSet[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("all");
  const [selectedDaemonSet, setSelectedDaemonSet] = useState<DaemonSet | null>(null);
  const [showDetails, setShowDetails] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize] = useState(20);
  const [total, setTotal] = useState(0);

  // 筛选和搜索DaemonSets
  const filteredDaemonSets = useMemo(() => {
    return daemonSets.filter(ds => {
      const matchesSearch = 
        ds.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        ds.namespace.toLowerCase().includes(searchTerm.toLowerCase());

      const matchesStatus = statusFilter === "all" || ds.status.toLowerCase() === statusFilter.toLowerCase();

      return matchesSearch && matchesStatus;
    });
  }, [daemonSets, searchTerm, statusFilter]);

  // 统计信息
  const stats = useMemo(() => {
    return {
      total: daemonSets.length,
      ready: daemonSets.filter(ds => ds.status === 'Ready').length,
      updating: daemonSets.filter(ds => ds.status === 'Updating').length,
      failed: daemonSets.filter(ds => ds.status === 'Failed').length,
      totalNodes: daemonSets.reduce((sum, ds) => sum + ds.desired, 0),
      readyNodes: daemonSets.reduce((sum, ds) => sum + ds.ready, 0)
    };
  }, [daemonSets]);

  // 获取DaemonSets数据
  const fetchDaemonSets = async () => {
    if (!clusterId) return;
    
    setLoading(true);
    setError(null);
    try {
      const response: K8sResourceListResponse = await KubernetesAPI.getDaemonSets(
        clusterId,
        namespace || undefined,
        undefined, // serviceId
        pageNum,
        pageSize
      );

      // 转换API响应为组件需要的DaemonSet格式
      const convertedDaemonSets: DaemonSet[] = response.data.map((resource: K8sResource) => {
        const spec = resource.spec as any;
        const status = resource.metadata as any;
        
        const desired = status?.desiredNumberScheduled || 0;
        const current = status?.currentNumberScheduled || 0;
        const ready = status?.numberReady || 0;
        const upToDate = status?.updatedNumberScheduled || 0;
        const available = status?.numberAvailable || 0;
        
        return {
          name: resource.name,
          namespace: resource.namespace,
          desired,
          current,
          ready,
          upToDate,
          available,
          nodeSelector: spec?.template?.spec?.nodeSelector || {},
          creationTimestamp: resource.creationTimestamp,
          age: resource.age || '-',
          status: determineDaemonSetStatus(desired, current, ready, upToDate, available),
          updateStrategy: spec?.updateStrategy?.type || 'RollingUpdate'
        };
      });

      setDaemonSets(convertedDaemonSets);
      setTotal(response.total || convertedDaemonSets.length);
    } catch (error) {
      console.error('获取DaemonSets失败:', error);
      setError(error instanceof Error ? error.message : '获取DaemonSets失败');
      setDaemonSets([]);
    } finally {
      setLoading(false);
    }
  };

  // 确定DaemonSet状态
  const determineDaemonSetStatus = (
    desired: number,
    current: number,
    ready: number,
    upToDate: number,
    available: number
  ): 'Ready' | 'Updating' | 'Failed' | 'Pending' => {
    if (desired === 0) return 'Pending';
    if (ready === desired && upToDate === desired && available === desired) return 'Ready';
    if (upToDate < desired || current !== desired) return 'Updating';
    if (ready < desired && current > 0) return 'Failed';
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

  // 格式化NodeSelector
  const formatNodeSelector = (nodeSelector: Record<string, string>): string => {
    if (!nodeSelector || Object.keys(nodeSelector).length === 0) {
      return '无限制';
    }
    return Object.entries(nodeSelector)
      .map(([key, value]) => `${key}=${value}`)
      .join(', ');
  };

  // 刷新数据
  const handleRefresh = async () => {
    await fetchDaemonSets();
  };

  // DaemonSet操作
  const handleDaemonSetAction = (action: string, ds: DaemonSet) => {
    console.log(`执行操作: ${action} on DaemonSet: ${ds.name}`);
    switch (action) {
      case 'view':
        setSelectedDaemonSet(ds);
        setShowDetails(true);
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
    fetchDaemonSets();
  }, [clusterId, namespace, pageNum]);

  if (loading && daemonSets.length === 0) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex items-center space-x-2">
          <RefreshCw className="w-6 h-6 animate-spin text-blue-500" />
          <span className="text-gray-600">加载DaemonSets...</span>
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
            description: "DaemonSets总数"
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
            title: "节点覆盖", 
            count: `${stats.readyNodes}/${stats.totalNodes}`, 
            color: "purple", 
            icon: Server,
            description: "节点覆盖情况",
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
              <CardTitle className="text-lg">DaemonSets</CardTitle>
              <CardDescription>管理Kubernetes守护进程集</CardDescription>
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
                <Monitor className="w-4 h-4 mr-2" />
                新建DaemonSet
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
                placeholder="搜索DaemonSets..."
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

          {/* DaemonSets表格 */}
          <div className="border rounded-lg">
            <Table>
              <TableHeader>
                <TableRow className="bg-gray-50">
                  <TableHead>名称</TableHead>
                  <TableHead>命名空间</TableHead>
                  <TableHead>节点状态</TableHead>
                  <TableHead>就绪率</TableHead>
                  <TableHead>节点选择器</TableHead>
                  <TableHead>更新策略</TableHead>
                  <TableHead>创建时间</TableHead>
                  <TableHead>操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <AnimatePresence>
                  {filteredDaemonSets.map((ds, index) => {
                    const statusDisplay = getStatusDisplay(ds.status);
                    const readyPercentage = getReadyPercentage(ds.ready, ds.desired);
                    return (
                      <motion.tr
                        key={ds.name}
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ delay: index * 0.05 }}
                        className="hover:bg-gray-50 transition-colors duration-200"
                      >
                        <TableCell>
                          <div className="flex items-center space-x-3">
                            <div className="w-8 h-8 bg-purple-100 rounded-lg flex items-center justify-center">
                              <Monitor className="w-4 h-4 text-purple-600" />
                            </div>
                            <div>
                              <div className="font-medium text-gray-900">{ds.name}</div>
                              <div className="text-xs text-gray-500">
                                {ds.current}/{ds.desired} 已调度
                              </div>
                            </div>
                          </div>
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline" className="text-xs">
                            {ds.namespace}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center space-x-2">
                            <statusDisplay.icon className={`w-4 h-4 ${statusDisplay.color}`} />
                            <span className="text-sm">
                              {ds.ready}/{ds.desired}
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
                          <span className="text-sm text-gray-600 max-w-32 truncate block" title={formatNodeSelector(ds.nodeSelector)}>
                            {formatNodeSelector(ds.nodeSelector)}
                          </span>
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline" className="text-xs">
                            {ds.updateStrategy}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <div className="text-sm text-gray-600">
                            <div>{getAge(ds.creationTimestamp)}</div>
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
                              <DropdownMenuItem onClick={() => handleDaemonSetAction('view', ds)}>
                                <Eye className="w-4 h-4 mr-2" />
                                查看详情
                              </DropdownMenuItem>
                              <DropdownMenuItem onClick={() => handleDaemonSetAction('restart', ds)}>
                                <RotateCcw className="w-4 h-4 mr-2" />
                                重启
                              </DropdownMenuItem>
                              <DropdownMenuSeparator />
                              <DropdownMenuItem 
                                onClick={() => handleDaemonSetAction('delete', ds)}
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

          {filteredDaemonSets.length === 0 && !loading && (
            <div className="text-center py-8">
              <Monitor className="w-12 h-12 text-gray-400 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">暂无DaemonSets</h3>
              <p className="text-gray-500 mb-4">
                {searchTerm ? '没有找到匹配的DaemonSets' : '当前命名空间中没有DaemonSets'}
              </p>
              {!searchTerm && (
                <Button>
                  <Monitor className="w-4 h-4 mr-2" />
                  创建第一个DaemonSet
                </Button>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      {/* DaemonSet详情模态框 */}
      {showDetails && selectedDaemonSet && (
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
                  <h2 className="text-xl font-semibold">{selectedDaemonSet.name}</h2>
                  <p className="text-gray-600">DaemonSet详细信息</p>
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
                  <TabsTrigger value="nodes">节点</TabsTrigger>
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
                          <p className="text-sm">{selectedDaemonSet.name}</p>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">命名空间</label>
                          <p className="text-sm">{selectedDaemonSet.namespace}</p>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">更新策略</label>
                          <p className="text-sm">{selectedDaemonSet.updateStrategy}</p>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">状态</label>
                          <div className="flex items-center space-x-2">
                            {(() => {
                              const display = getStatusDisplay(selectedDaemonSet.status);
                              return (
                                <>
                                  <display.icon className={`w-4 h-4 ${display.color}`} />
                                  <span className="text-sm">{selectedDaemonSet.status}</span>
                                </>
                              );
                            })()}
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                    <Card>
                      <CardHeader>
                        <CardTitle className="text-sm">节点覆盖</CardTitle>
                      </CardHeader>
                      <CardContent className="space-y-3">
                        <div>
                          <div className="flex items-center justify-between text-xs text-gray-600 mb-1">
                            <span>节点就绪率</span>
                            <span>{getReadyPercentage(selectedDaemonSet.ready, selectedDaemonSet.desired)}%</span>
                          </div>
                          <Progress value={getReadyPercentage(selectedDaemonSet.ready, selectedDaemonSet.desired)} className="h-2" />
                        </div>
                        <div className="grid grid-cols-2 gap-2 text-center">
                          <div>
                            <p className="text-xs text-gray-500">期望</p>
                            <p className="text-sm font-medium">{selectedDaemonSet.desired}</p>
                          </div>
                          <div>
                            <p className="text-xs text-gray-500">就绪</p>
                            <p className="text-sm font-medium">{selectedDaemonSet.ready}</p>
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  </div>
                </TabsContent>
                <TabsContent value="nodes">
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-sm">节点部署状态</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="space-y-4">
                        <div className="grid grid-cols-5 gap-4 text-center">
                          <div className="p-3 bg-blue-50 rounded-lg">
                            <p className="text-xs text-blue-600 font-medium">期望节点</p>
                            <p className="text-2xl font-bold text-blue-700">{selectedDaemonSet.desired}</p>
                          </div>
                          <div className="p-3 bg-green-50 rounded-lg">
                            <p className="text-xs text-green-600 font-medium">当前节点</p>
                            <p className="text-2xl font-bold text-green-700">{selectedDaemonSet.current}</p>
                          </div>
                          <div className="p-3 bg-purple-50 rounded-lg">
                            <p className="text-xs text-purple-600 font-medium">就绪节点</p>
                            <p className="text-2xl font-bold text-purple-700">{selectedDaemonSet.ready}</p>
                          </div>
                          <div className="p-3 bg-orange-50 rounded-lg">
                            <p className="text-xs text-orange-600 font-medium">最新节点</p>
                            <p className="text-2xl font-bold text-orange-700">{selectedDaemonSet.upToDate}</p>
                          </div>
                          <div className="p-3 bg-gray-50 rounded-lg">
                            <p className="text-xs text-gray-600 font-medium">可用节点</p>
                            <p className="text-2xl font-bold text-gray-700">{selectedDaemonSet.available}</p>
                          </div>
                        </div>
                        <div className="mt-4">
                          <label className="text-xs font-medium text-gray-500">节点选择器</label>
                          <div className="mt-1 p-2 bg-gray-50 rounded text-sm font-mono">
                            {formatNodeSelector(selectedDaemonSet.nodeSelector)}
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
                        <p className="text-sm">{new Date(selectedDaemonSet.creationTimestamp).toLocaleString()}</p>
                      </div>
                      <div>
                        <label className="text-xs font-medium text-gray-500">年龄</label>
                        <p className="text-sm">{getAge(selectedDaemonSet.creationTimestamp)}</p>
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

export default DaemonSetsDashboard;
