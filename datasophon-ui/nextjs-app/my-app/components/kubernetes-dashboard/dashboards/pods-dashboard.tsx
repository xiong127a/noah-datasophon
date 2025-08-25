/**
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 * @description Kubernetes Pods管理面板
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
  Play,
  Pause,
  Trash2,
  Eye,
  Terminal,
  Activity,
  Cpu,
  MemoryStick,
  HardDrive,
  Clock,
  Box,
  AlertCircle,
  CheckCircle,
  Info,
  ChevronDown,
  ChevronRight,
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
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { ScrollArea } from "@/components/ui/scroll-area";

import StatusIndicator from "../components/status-indicator";
import { Pod, PodStatus } from "../types";

interface PodsDashboardProps {
  clusterId: string;
  namespace: string;
  className?: string;
}

import { KubernetesAPI } from '@/lib/kubernetes-api';

const PodsDashboard: React.FC<PodsDashboardProps> = ({
  clusterId,
  namespace,
  className
}) => {
  const [pods, setPods] = useState<Pod[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("all");
  const [selectedPod, setSelectedPod] = useState<Pod | null>(null);
  const [showDetails, setShowDetails] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize] = useState(20);
  const [total, setTotal] = useState(0);

  // 筛选和搜索Pods
  const filteredPods = useMemo(() => {
    return pods.filter(pod => {
      const matchesSearch = 
        pod.metadata.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        pod.spec.nodeName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        Object.keys(pod.metadata.labels || {}).some(key => 
          key.toLowerCase().includes(searchTerm.toLowerCase()) ||
          (pod.metadata.labels?.[key] || "").toLowerCase().includes(searchTerm.toLowerCase())
        );

      const matchesStatus = statusFilter === "all" || 
        pod.status?.phase?.toLowerCase() === statusFilter.toLowerCase();

      return matchesSearch && matchesStatus;
    });
  }, [pods, searchTerm, statusFilter]);

  // 统计信息
  const stats = useMemo(() => {
    return {
      total: pods.length,
      running: pods.filter(p => p.status?.phase === "Running").length,
      pending: pods.filter(p => p.status?.phase === "Pending").length,
      failed: pods.filter(p => p.status?.phase === "Failed").length,
      succeeded: pods.filter(p => p.status?.phase === "Succeeded").length
    };
  }, [pods]);

  // 获取Pod年龄
  const getPodAge = (creationTimestamp: string) => {
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

  // 获取容器状态摘要
  const getContainerSummary = (pod: Pod) => {
    const statuses = pod.status?.containerStatuses || [];
    const total = statuses.length;
    const ready = statuses.filter(s => s.ready).length;
    return `${ready}/${total}`;
  };

  // 获取重启次数
  const getRestartCount = (pod: Pod) => {
    const statuses = pod.status?.containerStatuses || [];
    return statuses.reduce((sum, s) => sum + (s.restartCount || 0), 0);
  };

  // 获取Pods数据
  const fetchPods = async () => {
    if (!clusterId) return;
    
    console.log('🔄 开始获取Pods列表:', { clusterId, namespace, pageNum, pageSize });
    setLoading(true);
    setError(null);
    try {
      // 动态导入API工具类
      const { KubernetesAPI } = await import('@/lib/kubernetes-api');
      console.log('📡 调用 KubernetesAPI.getPods API...');
      const response = await KubernetesAPI.getPods(
        clusterId,
        namespace || undefined,
        undefined, // serviceId
        pageNum,
        pageSize
      );
      console.log('✅ 获取Pods成功，数量:', response.data.length);

      // 转换API响应为组件需要的Pod格式
      const convertedPods: Pod[] = response.data.map((resource: any) => ({
        apiVersion: "v1",
        kind: "Pod",
        metadata: {
          name: resource.name,
          namespace: resource.namespace,
          creationTimestamp: resource.creationTimestamp || new Date().toISOString(),
          labels: resource.labels || {}
        },
        spec: {
          containers: [{
            name: resource.name.split('-')[0] || 'container',
            image: 'unknown',
            ports: []
          }],
          nodeName: resource.node || 'unknown'
        },
        status: {
          phase: resource.status || 'Unknown',
          conditions: [{
            type: "Ready",
            status: resource.ready?.includes('1/1') ? "True" : "False",
            lastTransitionTime: new Date().toISOString()
          }],
          containerStatuses: [{
            name: resource.name.split('-')[0] || 'container',
            ready: resource.ready?.includes('1/1') || false,
            restartCount: resource.restarts || 0,
            state: resource.status === 'Running' 
              ? { running: { startedAt: resource.creationTimestamp || new Date().toISOString() } }
              : {}
          }]
        }
      }));

      setPods(convertedPods);
      setTotal(response.total || convertedPods.length);
    } catch (error) {
      console.error('获取Pods失败:', error);
      setError(error instanceof Error ? error.message : '获取Pods失败');
      setPods([]);
    } finally {
      setLoading(false);
    }
  };

  // 刷新数据
  const handleRefresh = async () => {
    await fetchPods();
  };

  // 组件挂载和依赖更新时获取数据
  useEffect(() => {
    fetchPods();
  }, [clusterId, namespace, pageNum]);

  // Pod操作
  const handlePodAction = (action: string, pod: Pod) => {
    console.log(`执行操作: ${action} on Pod: ${pod.metadata.name}`);
    // 这里实现具体的Pod操作逻辑
  };

  return (
    <div className={`space-y-6 ${className || ''}`}>
      {/* 统计卡片 */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        {[
          { title: "总计", count: stats.total, color: "blue", icon: Box },
          { title: "运行中", count: stats.running, color: "green", icon: CheckCircle },
          { title: "等待中", count: stats.pending, color: "yellow", icon: Clock },
          { title: "失败", count: stats.failed, color: "red", icon: AlertCircle }
        ].map((stat, index) => (
          <motion.div
            key={stat.title}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: index * 0.1 }}
          >
            <Card className="hover:shadow-md transition-shadow duration-200">
              <CardContent className="p-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-600">{stat.title}</p>
                    <p className="text-2xl font-bold text-gray-900 mt-1">{stat.count}</p>
                  </div>
                  <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${
                    stat.color === 'blue' ? 'bg-blue-100 text-blue-600' :
                    stat.color === 'green' ? 'bg-green-100 text-green-600' :
                    stat.color === 'yellow' ? 'bg-yellow-100 text-yellow-600' :
                    'bg-red-100 text-red-600'
                  }`}>
                    <stat.icon className="w-6 h-6" />
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
            <CardTitle className="text-lg font-semibold">Pods 列表</CardTitle>
            <div className="flex items-center space-x-3">
              {/* 搜索框 */}
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
                <Input
                  placeholder="搜索 Pods..."
                  className="pl-10 w-64"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>

              {/* 状态筛选 */}
              <Select value={statusFilter} onValueChange={setStatusFilter}>
                <SelectTrigger className="w-40">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">全部状态</SelectItem>
                  <SelectItem value="running">运行中</SelectItem>
                  <SelectItem value="pending">等待中</SelectItem>
                  <SelectItem value="failed">失败</SelectItem>
                  <SelectItem value="succeeded">成功</SelectItem>
                </SelectContent>
              </Select>

              {/* 操作按钮 */}
              <Button variant="outline" size="icon" onClick={handleRefresh} disabled={loading}>
                <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
              </Button>

              <Button variant="outline" size="icon">
                <Download className="w-4 h-4" />
              </Button>
            </div>
          </div>
        </CardHeader>

        <CardContent className="p-0">
          {/* 数据表格 */}
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-12"></TableHead>
                  <TableHead>名称</TableHead>
                  <TableHead>状态</TableHead>
                  <TableHead>就绪</TableHead>
                  <TableHead>重启次数</TableHead>
                  <TableHead>节点</TableHead>
                  <TableHead>年龄</TableHead>
                  <TableHead className="w-12"></TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <AnimatePresence>
                  {filteredPods.map((pod, index) => (
                    <motion.tr
                      key={pod.metadata.name}
                      initial={{ opacity: 0, y: 20 }}
                      animate={{ opacity: 1, y: 0 }}
                      exit={{ opacity: 0, y: -20 }}
                      transition={{ delay: index * 0.05 }}
                      className="hover:bg-gray-50 cursor-pointer"
                      onClick={() => {
                        setSelectedPod(pod);
                        setShowDetails(true);
                      }}
                    >
                      <TableCell>
                        <StatusIndicator 
                          status={pod.status?.phase as any || 'Unknown'} 
                          size="sm" 
                        />
                      </TableCell>
                      <TableCell>
                        <div className="flex flex-col">
                          <span className="font-medium text-gray-900">
                            {pod.metadata.name}
                          </span>
                          <div className="flex flex-wrap gap-1 mt-1">
                            {Object.entries(pod.metadata.labels || {}).slice(0, 2).map(([key, value]) => (
                              <Badge key={key} variant="outline" className="text-xs">
                                {key}={value}
                              </Badge>
                            ))}
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge 
                          variant={
                            pod.status?.phase === 'Running' ? 'default' :
                            pod.status?.phase === 'Pending' ? 'secondary' :
                            pod.status?.phase === 'Failed' ? 'destructive' :
                            'outline'
                          }
                          className="font-mono"
                        >
                          {pod.status?.phase || 'Unknown'}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <span className="font-mono text-sm">
                          {getContainerSummary(pod)}
                        </span>
                      </TableCell>
                      <TableCell>
                        <span className={`font-mono text-sm ${
                          getRestartCount(pod) > 0 ? 'text-orange-600' : 'text-gray-600'
                        }`}>
                          {getRestartCount(pod)}
                        </span>
                      </TableCell>
                      <TableCell>
                        <span className="text-sm text-gray-600">
                          {pod.spec.nodeName || '-'}
                        </span>
                      </TableCell>
                      <TableCell>
                        <span className="text-sm text-gray-500">
                          {getPodAge(pod.metadata.creationTimestamp)}
                        </span>
                      </TableCell>
                      <TableCell>
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button 
                              variant="ghost" 
                              size="icon"
                              className="w-8 h-8"
                              onClick={(e) => e.stopPropagation()}
                            >
                              <MoreHorizontal className="w-4 h-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <DropdownMenuItem onClick={(e) => {
                              e.stopPropagation();
                              handlePodAction('view', pod);
                            }}>
                              <Eye className="w-4 h-4 mr-2" />
                              查看详情
                            </DropdownMenuItem>
                            <DropdownMenuItem onClick={(e) => {
                              e.stopPropagation();
                              handlePodAction('logs', pod);
                            }}>
                              <Terminal className="w-4 h-4 mr-2" />
                              查看日志
                            </DropdownMenuItem>
                            <DropdownMenuItem onClick={(e) => {
                              e.stopPropagation();
                              handlePodAction('exec', pod);
                            }}>
                              <Terminal className="w-4 h-4 mr-2" />
                              进入容器
                            </DropdownMenuItem>
                            <DropdownMenuSeparator />
                            {pod.status?.phase === 'Running' ? (
                              <DropdownMenuItem 
                                onClick={(e) => {
                                  e.stopPropagation();
                                  handlePodAction('restart', pod);
                                }}
                                className="text-orange-600"
                              >
                                <RefreshCw className="w-4 h-4 mr-2" />
                                重启
                              </DropdownMenuItem>
                            ) : null}
                            <DropdownMenuItem 
                              onClick={(e) => {
                                e.stopPropagation();
                                handlePodAction('delete', pod);
                              }}
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

          {/* 空状态 */}
          {filteredPods.length === 0 && (
            <div className="text-center py-12">
              <Box className="w-12 h-12 text-gray-400 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">未找到Pod</h3>
              <p className="text-gray-500 mb-4">
                {searchTerm ? '尝试调整搜索条件' : '当前命名空间中没有Pod'}
              </p>
              {searchTerm && (
                <Button variant="outline" onClick={() => setSearchTerm('')}>
                  清除搜索
                </Button>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Pod详情侧边栏 - 这里可以进一步实现详细的Pod信息展示 */}
      {showDetails && selectedPod && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          className="fixed inset-y-0 right-0 w-96 bg-white shadow-xl border-l border-gray-200 z-50 overflow-y-auto"
        >
          <div className="p-6">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-xl font-semibold">Pod 详情</h2>
              <Button
                variant="ghost"
                size="icon"
                onClick={() => setShowDetails(false)}
              >
                <ChevronRight className="w-4 h-4" />
              </Button>
            </div>

            <div className="space-y-6">
              {/* 基本信息 */}
              <div>
                <h3 className="text-sm font-medium text-gray-900 mb-3">基本信息</h3>
                <div className="space-y-2">
                  <div className="flex justify-between">
                    <span className="text-sm text-gray-600">名称:</span>
                    <span className="text-sm font-mono">{selectedPod.metadata.name}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm text-gray-600">命名空间:</span>
                    <span className="text-sm font-mono">{selectedPod.metadata.namespace}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm text-gray-600">状态:</span>
                    <StatusIndicator 
                      status={selectedPod.status?.phase as any || 'Unknown'} 
                      size="sm" 
                      showText 
                    />
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm text-gray-600">节点:</span>
                    <span className="text-sm">{selectedPod.spec.nodeName || '-'}</span>
                  </div>
                </div>
              </div>

              {/* 标签 */}
              {selectedPod.metadata.labels && Object.keys(selectedPod.metadata.labels).length > 0 && (
                <div>
                  <h3 className="text-sm font-medium text-gray-900 mb-3">标签</h3>
                  <div className="flex flex-wrap gap-2">
                    {Object.entries(selectedPod.metadata.labels).map(([key, value]) => (
                      <Badge key={key} variant="outline" className="text-xs">
                        {key}={value}
                      </Badge>
                    ))}
                  </div>
                </div>
              )}

              {/* 容器信息 */}
              <div>
                <h3 className="text-sm font-medium text-gray-900 mb-3">容器</h3>
                <div className="space-y-3">
                  {selectedPod.spec.containers.map((container, index) => (
                    <div key={container.name} className="p-3 bg-gray-50 rounded-lg">
                      <div className="flex items-center justify-between mb-2">
                        <span className="text-sm font-medium">{container.name}</span>
                        <Badge variant="outline" className="text-xs">
                          {selectedPod.status?.containerStatuses?.[index]?.ready ? 'Ready' : 'NotReady'}
                        </Badge>
                      </div>
                      <div className="text-xs text-gray-600 space-y-1">
                        <div>镜像: {container.image}</div>
                        {container.ports && (
                          <div>端口: {container.ports.map(p => `${p.containerPort}/${p.protocol || 'TCP'}`).join(', ')}</div>
                        )}
                        {container.resources && (
                          <div>
                            资源: CPU({container.resources.requests?.cpu || '-'}/{container.resources.limits?.cpu || '-'})
                            内存({container.resources.requests?.memory || '-'}/{container.resources.limits?.memory || '-'})
                          </div>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </motion.div>
      )}
    </div>
  );
};

export default PodsDashboard;
