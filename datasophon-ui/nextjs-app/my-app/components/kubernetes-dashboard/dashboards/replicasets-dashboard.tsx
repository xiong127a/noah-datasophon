/**
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 * @description Kubernetes ReplicaSets管理面板
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
  Copy,
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
  GitBranch
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

interface ReplicaSetsDashboardProps {
  clusterId: string;
  namespace: string;
  className?: string;
}

interface ReplicaSet {
  name: string;
  namespace: string;
  desired: number;
  current: number;
  ready: number;
  age: string;
  creationTimestamp: string;
  ownerReferences: Array<{
    kind: string;
    name: string;
  }>;
  generation: number;
  observedGeneration: number;
  status: 'Ready' | 'ScalingUp' | 'ScalingDown' | 'Failed' | 'Pending';
  selector: Record<string, string>;
}

const ReplicaSetsDashboard: React.FC<ReplicaSetsDashboardProps> = ({
  clusterId,
  namespace,
  className
}) => {
  const [replicaSets, setReplicaSets] = useState<ReplicaSet[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("all");
  const [ownerFilter, setOwnerFilter] = useState<string>("all");
  const [selectedReplicaSet, setSelectedReplicaSet] = useState<ReplicaSet | null>(null);
  const [showDetails, setShowDetails] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize] = useState(20);
  const [total, setTotal] = useState(0);

  // 筛选和搜索ReplicaSets
  const filteredReplicaSets = useMemo(() => {
    return replicaSets.filter(rs => {
      const matchesSearch = 
        rs.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        rs.namespace.toLowerCase().includes(searchTerm.toLowerCase()) ||
        rs.ownerReferences.some(owner => 
          owner.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
          owner.kind.toLowerCase().includes(searchTerm.toLowerCase())
        );

      const matchesStatus = statusFilter === "all" || rs.status.toLowerCase() === statusFilter.toLowerCase();
      
      const matchesOwner = ownerFilter === "all" || 
        rs.ownerReferences.some(owner => owner.kind.toLowerCase() === ownerFilter.toLowerCase());

      return matchesSearch && matchesStatus && matchesOwner;
    });
  }, [replicaSets, searchTerm, statusFilter, ownerFilter]);

  // 统计信息
  const stats = useMemo(() => {
    return {
      total: replicaSets.length,
      ready: replicaSets.filter(rs => rs.status === 'Ready').length,
      scaling: replicaSets.filter(rs => rs.status === 'ScalingUp' || rs.status === 'ScalingDown').length,
      failed: replicaSets.filter(rs => rs.status === 'Failed').length,
      totalReplicas: replicaSets.reduce((sum, rs) => sum + rs.desired, 0),
      readyReplicas: replicaSets.reduce((sum, rs) => sum + rs.ready, 0),
      orphaned: replicaSets.filter(rs => rs.ownerReferences.length === 0).length
    };
  }, [replicaSets]);

  // 获取ReplicaSets数据
  const fetchReplicaSets = async () => {
    if (!clusterId) return;
    
    setLoading(true);
    setError(null);
    try {
      const response: K8sResourceListResponse = await KubernetesAPI.getReplicaSets(
        clusterId,
        namespace || undefined,
        pageNum,
        pageSize
      );

      // 转换API响应为组件需要的ReplicaSet格式
      const convertedReplicaSets: ReplicaSet[] = response.data.map((resource: K8sResource) => {
        const spec = resource.spec as any;
        const status = resource.metadata as any;
        const metadata = resource.metadata as any;
        
        const desired = spec?.replicas || 0;
        const current = status?.replicas || 0;
        const ready = status?.readyReplicas || 0;
        
        return {
          name: resource.name,
          namespace: resource.namespace,
          desired,
          current,
          ready,
          age: resource.age || '-',
          creationTimestamp: resource.creationTimestamp,
          ownerReferences: metadata?.ownerReferences || [],
          generation: metadata?.generation || 1,
          observedGeneration: status?.observedGeneration || 1,
          status: determineReplicaSetStatus(desired, current, ready),
          selector: spec?.selector?.matchLabels || {}
        };
      });

      setReplicaSets(convertedReplicaSets);
      setTotal(response.total || convertedReplicaSets.length);
    } catch (error) {
      console.error('获取ReplicaSets失败:', error);
      setError(error instanceof Error ? error.message : '获取ReplicaSets失败');
      setReplicaSets([]);
    } finally {
      setLoading(false);
    }
  };

  // 确定ReplicaSet状态
  const determineReplicaSetStatus = (
    desired: number,
    current: number,
    ready: number
  ): 'Ready' | 'ScalingUp' | 'ScalingDown' | 'Failed' | 'Pending' => {
    if (desired === 0 && current === 0) return 'Ready';
    if (ready === desired && current === desired) return 'Ready';
    if (current > desired) return 'ScalingDown';
    if (current < desired) return 'ScalingUp';
    if (ready < current && current > 0) return 'Failed';
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
      'ScalingUp': { color: 'text-blue-600', bgColor: 'bg-blue-100', icon: Activity },
      'ScalingDown': { color: 'text-orange-600', bgColor: 'bg-orange-100', icon: Activity },
      'Failed': { color: 'text-red-600', bgColor: 'bg-red-100', icon: AlertCircle },
      'Pending': { color: 'text-yellow-600', bgColor: 'bg-yellow-100', icon: Clock }
    };
    return displays[status as keyof typeof displays] || displays['Pending'];
  };

  // 计算就绪率
  const getReadyPercentage = (ready: number, desired: number): number => {
    if (desired === 0) return 100; // 如果期望为0，认为是100%就绪
    return Math.round((ready / desired) * 100);
  };

  // 获取拥有者信息
  const getOwnerInfo = (ownerReferences: Array<{kind: string; name: string}>): string => {
    if (!ownerReferences || ownerReferences.length === 0) {
      return '无拥有者';
    }
    return ownerReferences.map(owner => `${owner.kind}/${owner.name}`).join(', ');
  };

  // 格式化选择器
  const formatSelector = (selector: Record<string, string>): string => {
    if (!selector || Object.keys(selector).length === 0) {
      return '无选择器';
    }
    return Object.entries(selector)
      .map(([key, value]) => `${key}=${value}`)
      .join(', ');
  };

  // 刷新数据
  const handleRefresh = async () => {
    await fetchReplicaSets();
  };

  // ReplicaSet操作
  const handleReplicaSetAction = (action: string, rs: ReplicaSet) => {
    console.log(`执行操作: ${action} on ReplicaSet: ${rs.name}`);
    switch (action) {
      case 'view':
        setSelectedReplicaSet(rs);
        setShowDetails(true);
        break;
      case 'scale':
        // 实现扩缩容逻辑
        break;
      case 'delete':
        // 实现删除逻辑
        break;
    }
  };

  // 获取可用的拥有者类型
  const ownerTypes = useMemo(() => {
    const types = [...new Set(replicaSets.flatMap(rs => 
      rs.ownerReferences.map(owner => owner.kind)
    ))];
    return types;
  }, [replicaSets]);

  // 组件挂载和依赖更新时获取数据
  useEffect(() => {
    fetchReplicaSets();
  }, [clusterId, namespace, pageNum]);

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
          { 
            title: "总计", 
            count: stats.total, 
            color: "blue", 
            icon: Box,
            description: "ReplicaSets总数"
          },
          { 
            title: "就绪", 
            count: stats.ready, 
            color: "green", 
            icon: CheckCircle,
            description: "就绪状态"
          },
          { 
            title: "扩缩容中", 
            count: stats.scaling, 
            color: "yellow", 
            icon: Activity,
            description: "正在扩缩容"
          },
          { 
            title: "孤立", 
            count: stats.orphaned, 
            color: "red", 
            icon: GitBranch,
            description: "无拥有者"
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
              <CardTitle className="text-lg">ReplicaSets</CardTitle>
              <CardDescription>管理Kubernetes副本集</CardDescription>
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
                <Copy className="w-4 h-4 mr-2" />
                新建ReplicaSet
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
                placeholder="搜索ReplicaSets..."
                className="pl-10"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger className="w-32">
                <SelectValue placeholder="状态" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">所有状态</SelectItem>
                <SelectItem value="ready">就绪</SelectItem>
                <SelectItem value="scalingup">扩容中</SelectItem>
                <SelectItem value="scalingdown">缩容中</SelectItem>
                <SelectItem value="failed">失败</SelectItem>
                <SelectItem value="pending">等待中</SelectItem>
              </SelectContent>
            </Select>
            <Select value={ownerFilter} onValueChange={setOwnerFilter}>
              <SelectTrigger className="w-32">
                <SelectValue placeholder="拥有者" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">所有</SelectItem>
                {ownerTypes.map(type => (
                  <SelectItem key={type} value={type.toLowerCase()}>
                    {type}
                  </SelectItem>
                ))}
                <SelectItem value="orphaned">孤立</SelectItem>
              </SelectContent>
            </Select>
            <Button variant="outline" size="sm">
              <Download className="w-4 h-4 mr-2" />
              导出
            </Button>
          </div>

          {/* ReplicaSets表格 */}
          <div className="border rounded-lg">
            <Table>
              <TableHeader>
                <TableRow className="bg-gray-50">
                  <TableHead>名称</TableHead>
                  <TableHead>命名空间</TableHead>
                  <TableHead>副本状态</TableHead>
                  <TableHead>就绪率</TableHead>
                  <TableHead>拥有者</TableHead>
                  <TableHead>代数</TableHead>
                  <TableHead>创建时间</TableHead>
                  <TableHead>操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <AnimatePresence>
                  {filteredReplicaSets.map((rs, index) => {
                    const statusDisplay = getStatusDisplay(rs.status);
                    const readyPercentage = getReadyPercentage(rs.ready, rs.desired);
                    return (
                      <motion.tr
                        key={rs.name}
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ delay: index * 0.05 }}
                        className="hover:bg-gray-50 transition-colors duration-200"
                      >
                        <TableCell>
                          <div className="flex items-center space-x-3">
                            <div className="w-8 h-8 bg-orange-100 rounded-lg flex items-center justify-center">
                              <Copy className="w-4 h-4 text-orange-600" />
                            </div>
                            <div>
                              <div className="font-medium text-gray-900">{rs.name}</div>
                              <div className="text-xs text-gray-500">
                                Gen: {rs.generation}/{rs.observedGeneration}
                              </div>
                            </div>
                          </div>
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline" className="text-xs">
                            {rs.namespace}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center space-x-2">
                            <statusDisplay.icon className={`w-4 h-4 ${statusDisplay.color}`} />
                            <span className="text-sm">
                              {rs.ready}/{rs.desired}
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
                          <span className="text-sm text-gray-600 max-w-32 truncate block" title={getOwnerInfo(rs.ownerReferences)}>
                            {getOwnerInfo(rs.ownerReferences)}
                          </span>
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline" className="text-xs">
                            {rs.generation}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <div className="text-sm text-gray-600">
                            <div>{getAge(rs.creationTimestamp)}</div>
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
                              <DropdownMenuItem onClick={() => handleReplicaSetAction('view', rs)}>
                                <Eye className="w-4 h-4 mr-2" />
                                查看详情
                              </DropdownMenuItem>
                              <DropdownMenuItem onClick={() => handleReplicaSetAction('scale', rs)}>
                                <Layers className="w-4 h-4 mr-2" />
                                扩缩容
                              </DropdownMenuItem>
                              <DropdownMenuSeparator />
                              <DropdownMenuItem 
                                onClick={() => handleReplicaSetAction('delete', rs)}
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

          {filteredReplicaSets.length === 0 && !loading && (
            <div className="text-center py-8">
              <Copy className="w-12 h-12 text-gray-400 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">暂无ReplicaSets</h3>
              <p className="text-gray-500 mb-4">
                {searchTerm ? '没有找到匹配的ReplicaSets' : '当前命名空间中没有ReplicaSets'}
              </p>
              {!searchTerm && (
                <Button>
                  <Copy className="w-4 h-4 mr-2" />
                  创建第一个ReplicaSet
                </Button>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      {/* ReplicaSet详情模态框 */}
      {showDetails && selectedReplicaSet && (
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
                  <h2 className="text-xl font-semibold">{selectedReplicaSet.name}</h2>
                  <p className="text-gray-600">ReplicaSet详细信息</p>
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
                          <p className="text-sm">{selectedReplicaSet.name}</p>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">命名空间</label>
                          <p className="text-sm">{selectedReplicaSet.namespace}</p>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">状态</label>
                          <div className="flex items-center space-x-2">
                            {(() => {
                              const display = getStatusDisplay(selectedReplicaSet.status);
                              return (
                                <>
                                  <display.icon className={`w-4 h-4 ${display.color}`} />
                                  <span className="text-sm">{selectedReplicaSet.status}</span>
                                </>
                              );
                            })()}
                          </div>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">代数</label>
                          <p className="text-sm">{selectedReplicaSet.generation}/{selectedReplicaSet.observedGeneration}</p>
                        </div>
                      </CardContent>
                    </Card>
                    <Card>
                      <CardHeader>
                        <CardTitle className="text-sm">副本状态</CardTitle>
                      </CardHeader>
                      <CardContent className="space-y-3">
                        <div>
                          <div className="flex items-center justify-between text-xs text-gray-600 mb-1">
                            <span>副本就绪率</span>
                            <span>{getReadyPercentage(selectedReplicaSet.ready, selectedReplicaSet.desired)}%</span>
                          </div>
                          <Progress value={getReadyPercentage(selectedReplicaSet.ready, selectedReplicaSet.desired)} className="h-2" />
                        </div>
                        <div className="grid grid-cols-3 gap-2 text-center">
                          <div>
                            <p className="text-xs text-gray-500">期望</p>
                            <p className="text-sm font-medium">{selectedReplicaSet.desired}</p>
                          </div>
                          <div>
                            <p className="text-xs text-gray-500">当前</p>
                            <p className="text-sm font-medium">{selectedReplicaSet.current}</p>
                          </div>
                          <div>
                            <p className="text-xs text-gray-500">就绪</p>
                            <p className="text-sm font-medium">{selectedReplicaSet.ready}</p>
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  </div>
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-sm">拥有者引用</CardTitle>
                    </CardHeader>
                    <CardContent>
                      {selectedReplicaSet.ownerReferences.length > 0 ? (
                        <div className="space-y-2">
                          {selectedReplicaSet.ownerReferences.map((owner, index) => (
                            <div key={index} className="flex items-center space-x-2 p-2 bg-gray-50 rounded">
                              <GitBranch className="w-4 h-4 text-gray-400" />
                              <span className="text-sm">{owner.kind}: {owner.name}</span>
                            </div>
                          ))}
                        </div>
                      ) : (
                        <p className="text-sm text-gray-500">此ReplicaSet没有拥有者（孤立状态）</p>
                      )}
                    </CardContent>
                  </Card>
                </TabsContent>
                <TabsContent value="replicas">
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-sm">副本详情</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="space-y-4">
                        <div className="grid grid-cols-3 gap-4 text-center">
                          <div className="p-3 bg-blue-50 rounded-lg">
                            <p className="text-xs text-blue-600 font-medium">期望副本</p>
                            <p className="text-2xl font-bold text-blue-700">{selectedReplicaSet.desired}</p>
                          </div>
                          <div className="p-3 bg-green-50 rounded-lg">
                            <p className="text-xs text-green-600 font-medium">当前副本</p>
                            <p className="text-2xl font-bold text-green-700">{selectedReplicaSet.current}</p>
                          </div>
                          <div className="p-3 bg-purple-50 rounded-lg">
                            <p className="text-xs text-purple-600 font-medium">就绪副本</p>
                            <p className="text-2xl font-bold text-purple-700">{selectedReplicaSet.ready}</p>
                          </div>
                        </div>
                        <div className="mt-4">
                          <label className="text-xs font-medium text-gray-500">选择器</label>
                          <div className="mt-1 p-2 bg-gray-50 rounded text-sm font-mono">
                            {formatSelector(selectedReplicaSet.selector)}
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
                        <p className="text-sm">{new Date(selectedReplicaSet.creationTimestamp).toLocaleString()}</p>
                      </div>
                      <div>
                        <label className="text-xs font-medium text-gray-500">年龄</label>
                        <p className="text-sm">{getAge(selectedReplicaSet.creationTimestamp)}</p>
                      </div>
                      <div>
                        <label className="text-xs font-medium text-gray-500">代数</label>
                        <p className="text-sm">{selectedReplicaSet.generation}</p>
                      </div>
                      <div>
                        <label className="text-xs font-medium text-gray-500">观察代数</label>
                        <p className="text-sm">{selectedReplicaSet.observedGeneration}</p>
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

export default ReplicaSetsDashboard;
