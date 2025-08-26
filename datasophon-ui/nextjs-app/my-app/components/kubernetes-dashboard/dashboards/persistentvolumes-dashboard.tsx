/**
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 * @description Kubernetes PersistentVolumes管理面板
 */

"use client";

import React, { useState, useEffect, useMemo } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  Search,

  Download,
  RefreshCw,

  HardDrive,
  Database,
  CheckCircle,
  AlertCircle,

  Box,

  Activity,

  Shield,

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


import { KubernetesAPI, K8sResource, K8sResourceListResponse } from '@/lib/kubernetes-api';

interface PersistentVolumesDashboardProps {
  clusterId: string;
  namespace: string;
  className?: string;
}

interface PersistentVolume {
  name: string;
  capacity: string;
  accessModes: string[];
  reclaimPolicy: string;
  status: 'Available' | 'Bound' | 'Released' | 'Failed';
  claim?: string;
  storageClass: string;
  reason?: string;
  age: string;
  creationTimestamp: string;
  volumeMode: string;
  nodeAffinity?: Record<string, unknown>;
  source: {
    type: string;
    details: Record<string, unknown>;
  };
}

const PersistentVolumesDashboard: React.FC<PersistentVolumesDashboardProps> = ({
  clusterId,

  className
}) => {
  const [persistentVolumes, setPersistentVolumes] = useState<PersistentVolume[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("all");
  const [reclaimFilter, setReclaimFilter] = useState<string>("all");

  // const [selectedPV, setSelectedPV] = useState<PersistentVolume | null>(null);
  const [showDetails, setShowDetails] = useState(false);
  // const [pageNum, setPageNum] = useState(1);
  // const [total, setTotal] = useState(0);
  const [error, setError] = useState<string | null>(null);

  const [pageSize] = useState(20);


  // 筛选和搜索PersistentVolumes
  const filteredPVs = useMemo(() => {
    return persistentVolumes.filter(pv => {
      const matchesSearch = 
        pv.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        pv.storageClass.toLowerCase().includes(searchTerm.toLowerCase()) ||
        (pv.claim && pv.claim.toLowerCase().includes(searchTerm.toLowerCase()));

      const matchesStatus = statusFilter === "all" || pv.status.toLowerCase() === statusFilter.toLowerCase();
      const matchesReclaim = reclaimFilter === "all" || pv.reclaimPolicy.toLowerCase() === reclaimFilter.toLowerCase();

      return matchesSearch && matchesStatus && matchesReclaim;
    });
  }, [persistentVolumes, searchTerm, statusFilter, reclaimFilter]);

  // 统计信息
  const stats = useMemo(() => {
    const totalCapacity = persistentVolumes.reduce((sum, pv) => {
      const capacity = parseCapacity(pv.capacity);
      return sum + capacity;
    }, 0);

    return {
      total: persistentVolumes.length,
      available: persistentVolumes.filter(pv => pv.status === 'Available').length,
      bound: persistentVolumes.filter(pv => pv.status === 'Bound').length,
      released: persistentVolumes.filter(pv => pv.status === 'Released').length,
      failed: persistentVolumes.filter(pv => pv.status === 'Failed').length,
      totalCapacity: formatCapacity(totalCapacity)
    };
  }, [persistentVolumes]);

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

  // 获取PersistentVolumes数据
  const fetchPersistentVolumes = async () => {
    if (!clusterId) return;
    
    setLoading(true);
    setError(null);
    try {
      const response: K8sResourceListResponse = await KubernetesAPI.getPersistentVolumes(
        clusterId,
        pageNum,
        pageSize
      );

      // 转换API响应为组件需要的PV格式
      const convertedPVs: PersistentVolume[] = response.data.map((resource: K8sResource) => {
        const spec = resource.spec as any;
        const status = resource.metadata as any;
        
        return {
          name: resource.name,
          capacity: spec?.capacity?.storage || '0Gi',
          accessModes: spec?.accessModes || [],
          reclaimPolicy: spec?.persistentVolumeReclaimPolicy || 'Retain',
          status: status?.phase || 'Pending',
          claim: status?.claimRef ? `${status.claimRef.namespace}/${status.claimRef.name}` : undefined,
          storageClass: spec?.storageClassName || 'default',
          reason: status?.reason,
          age: resource.age || '-',
          creationTimestamp: resource.creationTimestamp,
          volumeMode: spec?.volumeMode || 'Filesystem',
          nodeAffinity: spec?.nodeAffinity,
          source: determineVolumeSource(spec)
        };
      });

      setPersistentVolumes(convertedPVs);
      setTotal(response.total || convertedPVs.length);
    } catch (error) {
      console.error('获取PersistentVolumes失败:', error);
      setError(error instanceof Error ? error.message : '获取PersistentVolumes失败');
      setPersistentVolumes([]);
    } finally {
      setLoading(false);
    }
  };

  // 确定存储卷源类型
  const determineVolumeSource = (spec: Record<string, unknown>): { type: string; details: Record<string, unknown> } => {
    if (spec?.hostPath) {
      return { type: 'HostPath', details: { path: spec.hostPath.path } };
    } else if (spec?.nfs) {
      return { type: 'NFS', details: { server: spec.nfs.server, path: spec.nfs.path } };
    } else if (spec?.iscsi) {
      return { type: 'iSCSI', details: { targetPortal: spec.iscsi.targetPortal, iqn: spec.iscsi.iqn } };
    } else if (spec?.awsElasticBlockStore) {
      return { type: 'AWS EBS', details: { volumeID: spec.awsElasticBlockStore.volumeID } };
    } else if (spec?.gcePersistentDisk) {
      return { type: 'GCE PD', details: { pdName: spec.gcePersistentDisk.pdName } };
    } else if (spec?.azureDisk) {
      return { type: 'Azure Disk', details: { diskName: spec.azureDisk.diskName } };
    } else if (spec?.csi) {
      return { type: 'CSI', details: { driver: spec.csi.driver, volumeHandle: spec.csi.volumeHandle } };
    }
    
    return { type: 'Unknown', details: {} };
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
      'Available': { color: 'text-green-600', bgColor: 'bg-green-100', icon: CheckCircle },
      'Bound': { color: 'text-blue-600', bgColor: 'bg-blue-100', icon: Database },
      'Released': { color: 'text-orange-600', bgColor: 'bg-orange-100', icon: Activity },
      'Failed': { color: 'text-red-600', bgColor: 'bg-red-100', icon: AlertCircle }
    };
    return displays[status as keyof typeof displays] || displays['Available'];
  };

  // 获取回收策略颜色
  const getReclaimPolicyColor = (policy: string) => {
    const colors: Record<string, string> = {
      'Retain': 'bg-blue-100 text-blue-700',
      'Recycle': 'bg-yellow-100 text-yellow-700',
      'Delete': 'bg-red-100 text-red-700'
    };
    return colors[policy] || 'bg-gray-100 text-gray-700';
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

  // 刷新数据
  const handleRefresh = async () => {
    await fetchPersistentVolumes();
  };



  // 获取可用的回收策略
  const reclaimPolicies = useMemo(() => {
    const policies = [...new Set(persistentVolumes.map(pv => pv.reclaimPolicy))];
    return policies;
  }, [persistentVolumes]);

  // 组件挂载和依赖更新时获取数据
  useEffect(() => {
    fetchPersistentVolumes();
  }, [clusterId, pageNum]);

  if (loading && persistentVolumes.length === 0) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex items-center space-x-2">
          <RefreshCw className="w-6 h-6 animate-spin text-blue-500" />
          <span className="text-gray-600">加载PersistentVolumes...</span>
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
      <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
        {[
          { 
            title: "总计", 
            count: stats.total, 
            color: "blue", 
            icon: Box,
            description: "PV总数"
          },
          { 
            title: "可用", 
            count: stats.available, 
            color: "green", 
            icon: CheckCircle,
            description: "可用状态"
          },
          { 
            title: "已绑定", 
            count: stats.bound, 
            color: "blue", 
            icon: Database,
            description: "已绑定PVC"
          },
          { 
            title: "已释放", 
            count: stats.released, 
            color: "orange", 
            icon: Activity,
            description: "已释放"
          },
          { 
            title: "总容量", 
            count: stats.totalCapacity, 
            color: "purple", 
            icon: HardDrive,
            description: "存储容量",
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
              <CardTitle className="text-lg">PersistentVolumes</CardTitle>
              <CardDescription>管理Kubernetes持久化存储卷</CardDescription>
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
                placeholder="搜索PersistentVolumes..."
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
                <SelectItem value="available">可用</SelectItem>
                <SelectItem value="bound">已绑定</SelectItem>
                <SelectItem value="released">已释放</SelectItem>
                <SelectItem value="failed">失败</SelectItem>
              </SelectContent>
            </Select>
            <Select value={reclaimFilter} onValueChange={setReclaimFilter}>
              <SelectTrigger className="w-32">
                <SelectValue placeholder="回收策略" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">所有策略</SelectItem>
                {reclaimPolicies.map(policy => (
                  <SelectItem key={policy} value={policy.toLowerCase()}>
                    {policy}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Button variant="outline" size="sm">
              <Download className="w-4 h-4 mr-2" />
              导出
            </Button>
          </div>

          {/* PersistentVolumes表格 */}
          <div className="border rounded-lg">
            <Table>
              <TableHeader>
                <TableRow className="bg-gray-50">
                  <TableHead>名称</TableHead>
                  <TableHead>容量</TableHead>
                  <TableHead>访问模式</TableHead>
                  <TableHead>回收策略</TableHead>
                  <TableHead>状态</TableHead>
                  <TableHead>声明</TableHead>
                  <TableHead>存储类</TableHead>
                  <TableHead>创建时间</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <AnimatePresence>
                  {filteredPVs.map((pv, index) => {
                    const statusDisplay = getStatusDisplay(pv.status);
                    return (
                      <motion.tr
                        key={pv.name}
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ delay: index * 0.05 }}
                        className="hover:bg-gray-50 transition-colors duration-200"
                      >
                        <TableCell>
                          <div className="flex items-center space-x-3">
                            <div className="w-8 h-8 bg-purple-100 rounded-lg flex items-center justify-center">
                              <HardDrive className="w-4 h-4 text-purple-600" />
                            </div>
                            <div>
                              <div className="font-medium text-gray-900">{pv.name}</div>
                              <div className="text-xs text-gray-500">
                                {pv.source.type}
                              </div>
                            </div>
                          </div>
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center space-x-2">
                            <HardDrive className="w-4 h-4 text-gray-400" />
                            <span className="text-sm font-medium">{pv.capacity}</span>
                          </div>
                        </TableCell>
                        <TableCell>
                          <span className="text-sm">{formatAccessModes(pv.accessModes)}</span>
                        </TableCell>
                        <TableCell>
                          <Badge className={`text-xs ${getReclaimPolicyColor(pv.reclaimPolicy)}`}>
                            {pv.reclaimPolicy}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center space-x-2">
                            <statusDisplay.icon className={`w-4 h-4 ${statusDisplay.color}`} />
                            <span className="text-sm">{pv.status}</span>
                          </div>
                        </TableCell>
                        <TableCell>
                          {pv.claim ? (
                            <span className="text-sm text-blue-600">{pv.claim}</span>
                          ) : (
                            <span className="text-sm text-gray-400">无</span>
                          )}
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline" className="text-xs">
                            {pv.storageClass}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <div className="text-sm text-gray-600">
                            <div>{getAge(pv.creationTimestamp)}</div>
                          </div>
                        </TableCell>

                      </motion.tr>
                    );
                  })}
                </AnimatePresence>
              </TableBody>
            </Table>
          </div>

          {filteredPVs.length === 0 && !loading && (
            <div className="text-center py-8">
              <HardDrive className="w-12 h-12 text-gray-400 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">暂无PersistentVolumes</h3>
              <p className="text-gray-500 mb-4">
                {searchTerm ? '没有找到匹配的PersistentVolumes' : '集群中没有PersistentVolumes'}
              </p>

            </div>
          )}
        </CardContent>
      </Card>

      {/* PV详情模态框 */}
      {showDetails && selectedPV && (
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
                  <h2 className="text-xl font-semibold">{selectedPV.name}</h2>
                  <p className="text-gray-600">PersistentVolume详细信息</p>
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
                  <TabsTrigger value="source">存储源</TabsTrigger>
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
                          <p className="text-sm">{selectedPV.name}</p>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">容量</label>
                          <p className="text-sm font-medium">{selectedPV.capacity}</p>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">状态</label>
                          <div className="flex items-center space-x-2">
                            {(() => {
                              const display = getStatusDisplay(selectedPV.status);
                              return (
                                <>
                                  <display.icon className={`w-4 h-4 ${display.color}`} />
                                  <span className="text-sm">{selectedPV.status}</span>
                                </>
                              );
                            })()}
                          </div>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">存储类</label>
                          <p className="text-sm">{selectedPV.storageClass}</p>
                        </div>
                      </CardContent>
                    </Card>
                    <Card>
                      <CardHeader>
                        <CardTitle className="text-sm">访问配置</CardTitle>
                      </CardHeader>
                      <CardContent className="space-y-3">
                        <div>
                          <label className="text-xs font-medium text-gray-500">访问模式</label>
                          <div className="flex flex-wrap gap-1 mt-1">
                            {selectedPV.accessModes.map((mode, idx) => (
                              <Badge key={idx} variant="outline" className="text-xs">
                                {mode}
                              </Badge>
                            ))}
                          </div>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">回收策略</label>
                          <Badge className={`text-xs mt-1 ${getReclaimPolicyColor(selectedPV.reclaimPolicy)}`}>
                            {selectedPV.reclaimPolicy}
                          </Badge>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">卷模式</label>
                          <p className="text-sm">{selectedPV.volumeMode}</p>
                        </div>
                      </CardContent>
                    </Card>
                  </div>
                  {selectedPV.claim && (
                    <Card>
                      <CardHeader>
                        <CardTitle className="text-sm">绑定信息</CardTitle>
                      </CardHeader>
                      <CardContent>
                        <div className="flex items-center space-x-2">
                          <Database className="w-4 h-4 text-blue-500" />
                          <span className="text-sm">绑定到PVC: {selectedPV.claim}</span>
                        </div>
                      </CardContent>
                    </Card>
                  )}
                </TabsContent>
                <TabsContent value="source">
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-sm">存储源详情</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="space-y-4">
                        <div>
                          <label className="text-xs font-medium text-gray-500">存储类型</label>
                          <div className="flex items-center space-x-2 mt-1">
                            <Shield className="w-4 h-4 text-gray-400" />
                            <span className="text-sm font-medium">{selectedPV.source.type}</span>
                          </div>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">源详情</label>
                          <div className="mt-1 p-3 bg-gray-50 rounded-lg">
                            {Object.entries(selectedPV.source.details).length > 0 ? (
                              <div className="space-y-2">
                                {Object.entries(selectedPV.source.details).map(([key, value]) => (
                                  <div key={key} className="flex justify-between text-sm">
                                    <span className="text-gray-600 capitalize">{key}:</span>
                                    <span className="font-mono text-gray-900">{String(value)}</span>
                                  </div>
                                ))}
                              </div>
                            ) : (
                              <p className="text-sm text-gray-500">无额外详情</p>
                            )}
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
                        <p className="text-sm">{new Date(selectedPV.creationTimestamp).toLocaleString()}</p>
                      </div>
                      <div>
                        <label className="text-xs font-medium text-gray-500">年龄</label>
                        <p className="text-sm">{getAge(selectedPV.creationTimestamp)}</p>
                      </div>
                      {selectedPV.reason && (
                        <div className="col-span-2">
                          <label className="text-xs font-medium text-gray-500">状态原因</label>
                          <p className="text-sm text-orange-600">{selectedPV.reason}</p>
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

export default PersistentVolumesDashboard;
