/**
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 * @description Kubernetes PersistentVolumeClaims管理面板
 */

"use client";

import React, { useState, useEffect, useMemo, useCallback } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  Search,
  Download,
  RefreshCw,
  Database,
  CheckCircle,
  AlertCircle,
  Clock,
  Box,
  Link
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
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

import { KubernetesAPI, K8sResourceListResponse } from '@/lib/kubernetes-api';

interface PersistentVolumeClaimsDashboardProps {
  clusterId: string;
  namespace: string;
  className?: string;
}

interface PersistentVolumeClaim {
  name: string;
  namespace: string;
  status: 'Bound' | 'Pending' | 'Lost' | 'Available';
  volume?: string;
  capacity?: string;
  requestedCapacity: string;
  accessModes: string[];
  storageClass: string;
  volumeMode: string;
  age: string;
  creationTimestamp: string;
  conditions: Array<{
    type: string;
    status: string;
    reason?: string;
    message?: string;
    lastTransitionTime: string;
  }>;
  selector?: Record<string, string>;
  finalizers: string[];
}

const PersistentVolumeClaimsDashboard: React.FC<PersistentVolumeClaimsDashboardProps> = ({
  clusterId,
  namespace,
  className
}) => {
  const [pvcs, setPVCs] = useState<PersistentVolumeClaim[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("all");
  const [selectedPVC] = useState<PersistentVolumeClaim | null>(null);
  const [showDetails, setShowDetails] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pageNum] = useState(1);
  const [pageSize] = useState(20);

  // 筛选和搜索PVCs
  const filteredPVCs = useMemo(() => {
    return pvcs.filter(pvc => {
      const matchesSearch = 
        pvc.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        pvc.namespace.toLowerCase().includes(searchTerm.toLowerCase()) ||
        pvc.storageClass.toLowerCase().includes(searchTerm.toLowerCase()) ||
        (pvc.volume && pvc.volume.toLowerCase().includes(searchTerm.toLowerCase()));

      const matchesStatus = statusFilter === "all" || pvc.status.toLowerCase() === statusFilter.toLowerCase();

      return matchesSearch && matchesStatus;
    });
  }, [pvcs, searchTerm, statusFilter]);

  // 统计信息
  const stats = useMemo(() => {
    const totalRequestedCapacity = pvcs.reduce((sum, pvc) => {
      const capacity = parseCapacity(pvc.requestedCapacity);
      return sum + capacity;
    }, 0);

    const totalBoundCapacity = pvcs
      .filter(pvc => pvc.status === 'Bound' && pvc.capacity)
      .reduce((sum, pvc) => {
        const capacity = parseCapacity(pvc.capacity!);
        return sum + capacity;
      }, 0);

    return {
      total: pvcs.length,
      bound: pvcs.filter(pvc => pvc.status === 'Bound').length,
      pending: pvcs.filter(pvc => pvc.status === 'Pending').length,
      lost: pvcs.filter(pvc => pvc.status === 'Lost').length,
      totalRequestedCapacity: formatCapacity(totalRequestedCapacity),
      totalBoundCapacity: formatCapacity(totalBoundCapacity),
      bindingRate: pvcs.length > 0 
        ? Math.round((pvcs.filter(pvc => pvc.status === 'Bound').length / pvcs.length) * 100)
        : 0
    };
  }, [pvcs]);

  // 解析容量（转换为字节）
  const parseCapacity = (capacityStr: string): number => {
    const units: Record<string, number> = {
      'Ki': 1024,
      'Mi': 1024 ** 2,
      'Gi': 1024 ** 3,
      'Ti': 1024 ** 4,
      'Pi': 1024 ** 5,
      'K': 1000,
      'M': 1000 ** 2,
      'G': 1000 ** 3,
      'T': 1000 ** 4,
      'P': 1000 ** 5
    };

    const match = capacityStr.match(/^(\d+(?:\.\d+)?)\s*([KMGTPE]i?)?$/);
    if (!match) return 0;

    const value = parseFloat(match[1]);
    const unit = match[2] || '';
    const multiplier = units[unit] || 1;

    return value * multiplier;
  };

  // 格式化容量显示
  const formatCapacity = (bytes: number): string => {
    if (bytes === 0) return '0B';
    
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    
    return `${parseFloat((bytes / Math.pow(k, i)).toFixed(1))}${sizes[i]}`;
  };

  // 获取PersistentVolumeClaims数据
  const fetchPVCs = useCallback(async () => {
    if (!clusterId) return;
    
    setLoading(true);
    setError(null);
    try {
      const response: K8sResourceListResponse = await KubernetesAPI.getPersistentVolumeClaims(
        clusterId,
        namespace || undefined,
        pageNum,
        pageSize
      );

      // 转换API响应为组件需要的PVC格式
      const convertedPVCs: PersistentVolumeClaim[] = response.data.map((resource: any) => {
        // 从多个可能的位置获取PVC的实际数据
        const spec = resource.spec || resource.additionalProperties?.spec || {};
        const status = resource.status || resource.metadata || resource.additionalProperties?.status || {};
        
        console.log('🔍 PVC原始数据:', {
          name: resource.name,
          hasSpec: !!resource.spec,
          hasStatus: !!resource.status,
          hasMetadata: !!resource.metadata,
          hasAdditionalProps: !!resource.additionalProperties,
          fullResource: resource
        });
        
        return {
          name: resource.name || '',
          namespace: resource.namespace || '',
          status: (status?.phase || resource.status || 'Pending') as 'Bound' | 'Pending' | 'Lost' | 'Available',
          volume: status?.volumeName,
          capacity: status?.capacity?.storage,
          requestedCapacity: spec?.resources?.requests?.storage || '0Gi',
          accessModes: spec?.accessModes || [],
          storageClass: spec?.storageClassName || 'default',
          volumeMode: spec?.volumeMode || 'Filesystem',
          age: resource.age || getAge(resource.creationTimestamp || ''),
          creationTimestamp: resource.creationTimestamp || '',
          conditions: status?.conditions || [],
          selector: spec?.selector?.matchLabels,
          finalizers: status?.finalizers || []
        };
      });

      setPVCs(convertedPVCs);
    } catch (error) {
      console.error('获取PersistentVolumeClaims失败:', error);
      setError(error instanceof Error ? error.message : '获取PersistentVolumeClaims失败');
      setPVCs([]);
    } finally {
      setLoading(false);
    }
  }, [clusterId, pageNum, pageSize]);

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
      'Bound': { color: 'text-green-600', bgColor: 'bg-green-100', icon: CheckCircle },
      'Pending': { color: 'text-yellow-600', bgColor: 'bg-yellow-100', icon: Clock },
      'Lost': { color: 'text-red-600', bgColor: 'bg-red-100', icon: AlertCircle },
      'Available': { color: 'text-blue-600', bgColor: 'bg-blue-100', icon: Database }
    };
    return displays[status as keyof typeof displays] || displays['Pending'];
  };

  // 格式化访问模式
  const formatAccessModes = (accessModes: string[]): string => {
    const modeMap: Record<string, string> = {
      'ReadWriteOnce': 'RWO',
      'ReadOnlyMany': 'ROX', 
      'ReadWriteMany': 'RWX',
      'ReadWriteOncePod': 'RWOP'
    };
    
    return accessModes.map(mode => modeMap[mode] || mode).join(', ');
  };

  // 获取条件状态图标
  const getConditionIcon = (type: string, status: string) => {
    if (status === 'True') {
      return <CheckCircle className="w-4 h-4 text-green-500" />;
    } else if (status === 'False') {
      return <AlertCircle className="w-4 h-4 text-red-500" />;
    }
    return <Clock className="w-4 h-4 text-yellow-500" />;
  };

  // 刷新数据
  const handleRefresh = async () => {
    await fetchPVCs();
  };



  // 组件挂载和依赖更新时获取数据
  useEffect(() => {
    fetchPVCs();
  }, [fetchPVCs]);

  if (loading && pvcs.length === 0) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex items-center space-x-2">
          <RefreshCw className="w-6 h-6 animate-spin text-blue-500" />
          <span className="text-gray-600">加载PersistentVolumeClaims...</span>
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
            description: "PVC总数"
          },
          { 
            title: "已绑定", 
            count: stats.bound, 
            color: "green", 
            icon: CheckCircle,
            description: "成功绑定PV"
          },
          { 
            title: "等待中", 
            count: stats.pending, 
            color: "yellow", 
            icon: Clock,
            description: "等待绑定"
          },
          { 
            title: "绑定率", 
            count: `${stats.bindingRate}%`, 
            color: "purple", 
            icon: Link,
            description: "PVC绑定成功率",
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
              <CardTitle className="text-lg">PersistentVolumeClaims</CardTitle>
              <CardDescription>管理Kubernetes持久卷声明</CardDescription>
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
                placeholder="搜索PersistentVolumeClaims..."
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
                <SelectItem value="bound">已绑定</SelectItem>
                <SelectItem value="pending">等待中</SelectItem>
                <SelectItem value="lost">已丢失</SelectItem>
                <SelectItem value="available">可用</SelectItem>
              </SelectContent>
            </Select>
            <Button variant="outline" size="sm">
              <Download className="w-4 h-4 mr-2" />
              导出
            </Button>
          </div>

          {/* PVCs表格 */}
          <div className="border rounded-lg">
            <Table>
              <TableHeader>
                <TableRow className="bg-gray-50">
                  <TableHead>名称</TableHead>
                  <TableHead>命名空间</TableHead>
                  <TableHead>状态</TableHead>
                  <TableHead>绑定卷</TableHead>
                  <TableHead>容量</TableHead>
                  <TableHead>访问模式</TableHead>
                  <TableHead>存储类</TableHead>
                  <TableHead>创建时间</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <AnimatePresence>
                  {filteredPVCs.map((pvc, index) => {
                    const statusDisplay = getStatusDisplay(pvc.status);
                    return (
                      <motion.tr
                        key={`${pvc.namespace}-${pvc.name}`}
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
                              <div className="font-medium text-gray-900">{pvc.name}</div>
                              <div className="text-xs text-gray-500">
                                {pvc.volumeMode}
                              </div>
                            </div>
                          </div>
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline" className="text-xs">
                            {pvc.namespace}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center space-x-2">
                            <statusDisplay.icon className={`w-4 h-4 ${statusDisplay.color}`} />
                            <span className="text-sm">{pvc.status}</span>
                          </div>
                        </TableCell>
                        <TableCell>
                          {pvc.volume ? (
                            <div className="flex items-center space-x-2">
                              <Link className="w-4 h-4 text-green-500" />
                              <span className="text-sm text-green-600">{pvc.volume}</span>
                            </div>
                          ) : (
                            <span className="text-sm text-gray-400">未绑定</span>
                          )}
                        </TableCell>
                        <TableCell>
                          <div className="space-y-1">
                            <div className="text-sm">
                              {pvc.capacity ? (
                                <span className="font-medium text-green-600">{pvc.capacity}</span>
                              ) : (
                                <span className="text-gray-400">-</span>
                              )}
                            </div>
                            <div className="text-xs text-gray-500">
                              请求: {pvc.requestedCapacity}
                            </div>
                          </div>
                        </TableCell>
                        <TableCell>
                          <span className="text-sm">{formatAccessModes(pvc.accessModes)}</span>
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline" className="text-xs">
                            {pvc.storageClass}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <div className="text-sm text-gray-600">
                            <div>{getAge(pvc.creationTimestamp)}</div>
                          </div>
                        </TableCell>

                      </motion.tr>
                    );
                  })}
                </AnimatePresence>
              </TableBody>
            </Table>
          </div>

          {filteredPVCs.length === 0 && !loading && (
            <div className="text-center py-8">
              <Database className="w-12 h-12 text-gray-400 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">暂无PersistentVolumeClaims</h3>
              <p className="text-gray-500 mb-4">
                {searchTerm ? '没有找到匹配的PersistentVolumeClaims' : '当前命名空间中没有PersistentVolumeClaims'}
              </p>

            </div>
          )}
        </CardContent>
      </Card>

      {/* PVC详情模态框 */}
      {showDetails && selectedPVC && (
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
                  <h2 className="text-xl font-semibold">{selectedPVC.name}</h2>
                  <p className="text-gray-600">PersistentVolumeClaim详细信息</p>
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
                  <TabsTrigger value="conditions">条件状态</TabsTrigger>
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
                          <p className="text-sm">{selectedPVC.name}</p>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">命名空间</label>
                          <p className="text-sm">{selectedPVC.namespace}</p>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">状态</label>
                          <div className="flex items-center space-x-2">
                            {(() => {
                              const display = getStatusDisplay(selectedPVC.status);
                              return (
                                <>
                                  <display.icon className={`w-4 h-4 ${display.color}`} />
                                  <span className="text-sm">{selectedPVC.status}</span>
                                </>
                              );
                            })()}
                          </div>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">存储类</label>
                          <p className="text-sm">{selectedPVC.storageClass}</p>
                        </div>
                      </CardContent>
                    </Card>
                    <Card>
                      <CardHeader>
                        <CardTitle className="text-sm">存储信息</CardTitle>
                      </CardHeader>
                      <CardContent className="space-y-3">
                        <div>
                          <label className="text-xs font-medium text-gray-500">请求容量</label>
                          <p className="text-sm font-medium">{selectedPVC.requestedCapacity}</p>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">实际容量</label>
                          <p className="text-sm font-medium text-green-600">
                            {selectedPVC.capacity || '未分配'}
                          </p>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">访问模式</label>
                          <div className="flex flex-wrap gap-1 mt-1">
                            {selectedPVC.accessModes.map((mode, idx) => (
                              <Badge key={idx} variant="outline" className="text-xs">
                                {mode}
                              </Badge>
                            ))}
                          </div>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">卷模式</label>
                          <p className="text-sm">{selectedPVC.volumeMode}</p>
                        </div>
                      </CardContent>
                    </Card>
                  </div>
                  {selectedPVC.volume && (
                    <Card>
                      <CardHeader>
                        <CardTitle className="text-sm">绑定信息</CardTitle>
                      </CardHeader>
                      <CardContent>
                        <div className="flex items-center space-x-2">
                          <Link className="w-4 h-4 text-green-500" />
                          <span className="text-sm">绑定到PV: {selectedPVC.volume}</span>
                        </div>
                      </CardContent>
                    </Card>
                  )}
                </TabsContent>
                <TabsContent value="conditions">
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-sm">条件状态</CardTitle>
                    </CardHeader>
                    <CardContent>
                      {selectedPVC.conditions.length > 0 ? (
                        <div className="space-y-3">
                          {selectedPVC.conditions.map((condition, index) => (
                            <div key={index} className="p-3 border rounded-lg">
                              <div className="flex items-center justify-between">
                                <div className="flex items-center space-x-2">
                                  {getConditionIcon(condition.type, condition.status)}
                                  <span className="font-medium text-sm">{condition.type}</span>
                                </div>
                                <Badge variant="outline" className={`text-xs ${
                                  condition.status === 'True' ? 'border-green-200 text-green-700' :
                                  condition.status === 'False' ? 'border-red-200 text-red-700' :
                                  'border-yellow-200 text-yellow-700'
                                }`}>
                                  {condition.status}
                                </Badge>
                              </div>
                              {condition.reason && (
                                <p className="text-xs text-gray-600 mt-1">
                                  原因: {condition.reason}
                                </p>
                              )}
                              {condition.message && (
                                <p className="text-xs text-gray-500 mt-1">
                                  消息: {condition.message}
                                </p>
                              )}
                              <p className="text-xs text-gray-400 mt-1">
                                转换时间: {new Date(condition.lastTransitionTime).toLocaleString()}
                              </p>
                            </div>
                          ))}
                        </div>
                      ) : (
                        <p className="text-sm text-gray-500">暂无条件信息</p>
                      )}
                    </CardContent>
                  </Card>
                </TabsContent>
                <TabsContent value="metadata">
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-sm">元数据</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                      <div className="grid grid-cols-2 gap-4">
                        <div>
                          <label className="text-xs font-medium text-gray-500">创建时间</label>
                          <p className="text-sm">{new Date(selectedPVC.creationTimestamp).toLocaleString()}</p>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">年龄</label>
                          <p className="text-sm">{getAge(selectedPVC.creationTimestamp)}</p>
                        </div>
                      </div>
                      
                      {selectedPVC.finalizers.length > 0 && (
                        <div>
                          <label className="text-xs font-medium text-gray-500">Finalizers</label>
                          <div className="mt-1 space-y-1">
                            {selectedPVC.finalizers.map((finalizer, idx) => (
                              <div key={idx} className="text-xs bg-gray-100 px-2 py-1 rounded font-mono">
                                {finalizer}
                              </div>
                            ))}
                          </div>
                        </div>
                      )}
                      
                      {selectedPVC.selector && (
                        <div>
                          <label className="text-xs font-medium text-gray-500">选择器</label>
                          <div className="mt-1 p-2 bg-gray-50 rounded">
                            {Object.entries(selectedPVC.selector).map(([key, value]) => (
                              <div key={key} className="text-xs">
                                <span className="text-gray-600">{key}:</span>
                                <span className="ml-2 font-mono">{value}</span>
                              </div>
                            ))}
                          </div>
                        </div>
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

export default PersistentVolumeClaimsDashboard;
