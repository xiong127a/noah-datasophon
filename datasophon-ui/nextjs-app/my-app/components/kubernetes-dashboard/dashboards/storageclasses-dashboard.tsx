/**
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 * @description Kubernetes StorageClasses管理面板
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
  HardDrive,
  Database,
  CheckCircle,
  AlertCircle,
  Clock,
  Box,
  ChevronDown,
  ChevronRight,
  Activity,
  Storage,
  Settings,
  Shield,
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

import { KubernetesAPI, K8sResource, K8sResourceListResponse } from '@/lib/kubernetes-api';

interface StorageClassesDashboardProps {
  clusterId: string;
  namespace: string;
  className?: string;
}

interface StorageClass {
  name: string;
  provisioner: string;
  reclaimPolicy: string;
  volumeBindingMode: string;
  allowVolumeExpansion: boolean;
  parameters: Record<string, string>;
  mountOptions?: string[];
  allowedTopologies?: Array<Record<string, any>>;
  isDefault: boolean;
  age: string;
  creationTimestamp: string;
  usageCount: number; // PVC使用此存储类的数量
}

const StorageClassesDashboard: React.FC<StorageClassesDashboardProps> = ({
  clusterId,
  namespace,
  className
}) => {
  const [storageClasses, setStorageClasses] = useState<StorageClass[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [provisionerFilter, setProvisionerFilter] = useState<string>("all");
  const [selectedSC, setSelectedSC] = useState<StorageClass | null>(null);
  const [showDetails, setShowDetails] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize] = useState(20);
  const [total, setTotal] = useState(0);

  // 筛选和搜索StorageClasses
  const filteredSCs = useMemo(() => {
    return storageClasses.filter(sc => {
      const matchesSearch = 
        sc.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        sc.provisioner.toLowerCase().includes(searchTerm.toLowerCase()) ||
        sc.reclaimPolicy.toLowerCase().includes(searchTerm.toLowerCase());

      const matchesProvisioner = provisionerFilter === "all" || 
        sc.provisioner.toLowerCase().includes(provisionerFilter.toLowerCase());

      return matchesSearch && matchesProvisioner;
    });
  }, [storageClasses, searchTerm, provisionerFilter]);

  // 统计信息
  const stats = useMemo(() => {
    const provisionerTypes = [...new Set(storageClasses.map(sc => sc.provisioner))].length;
    const defaultStorageClass = storageClasses.find(sc => sc.isDefault);
    
    return {
      total: storageClasses.length,
      hasDefault: storageClasses.filter(sc => sc.isDefault).length,
      expandable: storageClasses.filter(sc => sc.allowVolumeExpansion).length,
      provisionerTypes,
      totalUsage: storageClasses.reduce((sum, sc) => sum + sc.usageCount, 0),
      defaultName: defaultStorageClass?.name || 'None'
    };
  }, [storageClasses]);

  // 获取StorageClasses数据
  const fetchStorageClasses = async () => {
    if (!clusterId) return;
    
    setLoading(true);
    setError(null);
    try {
      const response: K8sResourceListResponse = await KubernetesAPI.getStorageClasses(
        clusterId,
        pageNum,
        pageSize
      );

      // 转换API响应为组件需要的StorageClass格式
      const convertedSCs: StorageClass[] = response.data.map((resource: K8sResource) => {
        const spec = resource.spec as any;
        const metadata = resource.metadata as any;
        
        return {
          name: resource.name,
          provisioner: spec?.provisioner || 'unknown',
          reclaimPolicy: spec?.reclaimPolicy || 'Delete',
          volumeBindingMode: spec?.volumeBindingMode || 'Immediate',
          allowVolumeExpansion: spec?.allowVolumeExpansion || false,
          parameters: spec?.parameters || {},
          mountOptions: spec?.mountOptions,
          allowedTopologies: spec?.allowedTopologies,
          isDefault: checkIsDefault(metadata?.annotations),
          age: resource.age || '-',
          creationTimestamp: resource.creationTimestamp,
          usageCount: generateUsageCount() // 实际应该从API获取
        };
      });

      setStorageClasses(convertedSCs);
      setTotal(response.total || convertedSCs.length);
    } catch (error) {
      console.error('获取StorageClasses失败:', error);
      setError(error instanceof Error ? error.message : '获取StorageClasses失败');
      setStorageClasses([]);
    } finally {
      setLoading(false);
    }
  };

  // 检查是否为默认存储类
  const checkIsDefault = (annotations: Record<string, string> = {}): boolean => {
    return annotations['storageclass.kubernetes.io/is-default-class'] === 'true' ||
           annotations['storageclass.beta.kubernetes.io/is-default-class'] === 'true';
  };

  // 生成模拟使用数量（实际应从API获取相关PVC数量）
  const generateUsageCount = (): number => {
    return Math.floor(Math.random() * 20);
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

  // 获取回收策略颜色
  const getReclaimPolicyColor = (policy: string) => {
    const colors: Record<string, string> = {
      'Delete': 'bg-red-100 text-red-700',
      'Retain': 'bg-blue-100 text-blue-700'
    };
    return colors[policy] || 'bg-gray-100 text-gray-700';
  };

  // 获取绑定模式颜色
  const getBindingModeColor = (mode: string) => {
    const colors: Record<string, string> = {
      'Immediate': 'bg-green-100 text-green-700',
      'WaitForFirstConsumer': 'bg-orange-100 text-orange-700'
    };
    return colors[mode] || 'bg-gray-100 text-gray-700';
  };

  // 获取供应商简化名称
  const getProvisionerDisplayName = (provisioner: string): string => {
    const provisionerMap: Record<string, string> = {
      'kubernetes.io/aws-ebs': 'AWS EBS',
      'kubernetes.io/gce-pd': 'GCE PD',
      'kubernetes.io/azure-disk': 'Azure Disk',
      'kubernetes.io/azure-file': 'Azure File',
      'kubernetes.io/vsphere-volume': 'vSphere',
      'kubernetes.io/no-provisioner': 'Manual',
      'ebs.csi.aws.com': 'AWS EBS CSI',
      'pd.csi.storage.gke.io': 'GKE PD CSI',
      'disk.csi.azure.com': 'Azure Disk CSI'
    };
    
    return provisionerMap[provisioner] || provisioner;
  };

  // 刷新数据
  const handleRefresh = async () => {
    await fetchStorageClasses();
  };



  // 获取可用的供应商类型
  const provisionerTypes = useMemo(() => {
    const types = [...new Set(storageClasses.map(sc => getProvisionerDisplayName(sc.provisioner)))];
    return types;
  }, [storageClasses]);

  // 组件挂载和依赖更新时获取数据
  useEffect(() => {
    fetchStorageClasses();
  }, [clusterId, pageNum]);

  if (loading && storageClasses.length === 0) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex items-center space-x-2">
          <RefreshCw className="w-6 h-6 animate-spin text-blue-500" />
          <span className="text-gray-600">加载StorageClasses...</span>
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
            description: "存储类总数"
          },
          { 
            title: "供应商类型", 
            count: stats.provisionerTypes, 
            color: "green", 
            icon: Shield,
            description: "不同供应商类型"
          },
          { 
            title: "可扩容", 
            count: stats.expandable, 
            color: "purple", 
            icon: Activity,
            description: "支持扩容的存储类"
          },
          { 
            title: "总使用量", 
            count: stats.totalUsage, 
            color: "orange", 
            icon: Storage,
            description: "PVC使用总数"
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
              <CardTitle className="text-lg">StorageClasses</CardTitle>
              <CardDescription>管理Kubernetes存储类</CardDescription>
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
                placeholder="搜索StorageClasses..."
                className="pl-10"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
            <Select value={provisionerFilter} onValueChange={setProvisionerFilter}>
              <SelectTrigger className="w-40">
                <SelectValue placeholder="供应商" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">所有供应商</SelectItem>
                {provisionerTypes.map(type => (
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

          {/* 默认存储类提示 */}
          {stats.hasDefault > 0 && (
            <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg">
              <div className="flex items-center space-x-2">
                <CheckCircle className="w-4 h-4 text-blue-500" />
                <span className="text-sm text-blue-700">
                  默认存储类: <span className="font-medium">{stats.defaultName}</span>
                </span>
              </div>
            </div>
          )}

          {/* StorageClasses表格 */}
          <div className="border rounded-lg">
            <Table>
              <TableHeader>
                <TableRow className="bg-gray-50">
                  <TableHead>名称</TableHead>
                  <TableHead>供应商</TableHead>
                  <TableHead>回收策略</TableHead>
                  <TableHead>绑定模式</TableHead>
                  <TableHead>扩容</TableHead>
                  <TableHead>使用量</TableHead>
                  <TableHead>创建时间</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <AnimatePresence>
                  {filteredSCs.map((sc, index) => (
                    <motion.tr
                      key={sc.name}
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      exit={{ opacity: 0 }}
                      transition={{ delay: index * 0.05 }}
                      className="hover:bg-gray-50 transition-colors duration-200"
                    >
                      <TableCell>
                        <div className="flex items-center space-x-3">
                          <div className="w-8 h-8 bg-indigo-100 rounded-lg flex items-center justify-center">
                            <Settings className="w-4 h-4 text-indigo-600" />
                          </div>
                          <div>
                            <div className="flex items-center space-x-2">
                              <span className="font-medium text-gray-900">{sc.name}</span>
                              {sc.isDefault && (
                                <Badge className="text-xs bg-blue-100 text-blue-700">
                                  默认
                                </Badge>
                              )}
                            </div>
                            <div className="text-xs text-gray-500">
                              {sc.parameters ? Object.keys(sc.parameters).length : 0} 个参数
                            </div>
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="space-y-1">
                          <div className="text-sm font-medium">
                            {getProvisionerDisplayName(sc.provisioner)}
                          </div>
                          <div className="text-xs text-gray-500 font-mono">
                            {sc.provisioner}
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge className={`text-xs ${getReclaimPolicyColor(sc.reclaimPolicy)}`}>
                          {sc.reclaimPolicy}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <Badge className={`text-xs ${getBindingModeColor(sc.volumeBindingMode)}`}>
                          {sc.volumeBindingMode === 'WaitForFirstConsumer' ? 'Wait' : 'Immediate'}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center space-x-2">
                          {sc.allowVolumeExpansion ? (
                            <>
                              <CheckCircle className="w-4 h-4 text-green-500" />
                              <span className="text-sm text-green-600">支持</span>
                            </>
                          ) : (
                            <>
                              <AlertCircle className="w-4 h-4 text-red-500" />
                              <span className="text-sm text-red-600">不支持</span>
                            </>
                          )}
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center space-x-2">
                          <Storage className="w-4 h-4 text-gray-400" />
                          <span className="text-sm">{sc.usageCount}</span>
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="text-sm text-gray-600">
                          <div>{getAge(sc.creationTimestamp)}</div>
                        </div>
                      </TableCell>

                    </motion.tr>
                  ))}
                </AnimatePresence>
              </TableBody>
            </Table>
          </div>

          {filteredSCs.length === 0 && !loading && (
            <div className="text-center py-8">
              <Settings className="w-12 h-12 text-gray-400 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">暂无StorageClasses</h3>
              <p className="text-gray-500 mb-4">
                {searchTerm ? '没有找到匹配的StorageClasses' : '集群中没有StorageClasses'}
              </p>

            </div>
          )}
        </CardContent>
      </Card>

      {/* StorageClass详情模态框 */}
      {showDetails && selectedSC && (
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
                  <h2 className="text-xl font-semibold">{selectedSC.name}</h2>
                  <p className="text-gray-600">StorageClass详细信息</p>
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
                  <TabsTrigger value="topology">拓扑约束</TabsTrigger>
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
                          <p className="text-sm">{selectedSC.name}</p>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">供应商</label>
                          <p className="text-sm">{getProvisionerDisplayName(selectedSC.provisioner)}</p>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">回收策略</label>
                          <Badge className={`text-xs ${getReclaimPolicyColor(selectedSC.reclaimPolicy)}`}>
                            {selectedSC.reclaimPolicy}
                          </Badge>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">绑定模式</label>
                          <Badge className={`text-xs ${getBindingModeColor(selectedSC.volumeBindingMode)}`}>
                            {selectedSC.volumeBindingMode}
                          </Badge>
                        </div>
                      </CardContent>
                    </Card>
                    <Card>
                      <CardHeader>
                        <CardTitle className="text-sm">功能特性</CardTitle>
                      </CardHeader>
                      <CardContent className="space-y-3">
                        <div className="flex items-center justify-between">
                          <span className="text-sm text-gray-600">默认存储类</span>
                          <div className="flex items-center space-x-2">
                            {selectedSC.isDefault ? (
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
                        <div className="flex items-center justify-between">
                          <span className="text-sm text-gray-600">支持扩容</span>
                          <div className="flex items-center space-x-2">
                            {selectedSC.allowVolumeExpansion ? (
                              <>
                                <CheckCircle className="w-4 h-4 text-green-500" />
                                <span className="text-sm text-green-600">支持</span>
                              </>
                            ) : (
                              <>
                                <AlertCircle className="w-4 h-4 text-red-500" />
                                <span className="text-sm text-red-600">不支持</span>
                              </>
                            )}
                          </div>
                        </div>
                        <div className="flex items-center justify-between">
                          <span className="text-sm text-gray-600">使用量</span>
                          <div className="flex items-center space-x-2">
                            <Storage className="w-4 h-4 text-gray-400" />
                            <span className="text-sm">{selectedSC.usageCount} PVCs</span>
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  </div>
                  {selectedSC.mountOptions && selectedSC.mountOptions.length > 0 && (
                    <Card>
                      <CardHeader>
                        <CardTitle className="text-sm">挂载选项</CardTitle>
                      </CardHeader>
                      <CardContent>
                        <div className="flex flex-wrap gap-2">
                          {selectedSC.mountOptions.map((option, idx) => (
                            <Badge key={idx} variant="outline" className="text-xs">
                              {option}
                            </Badge>
                          ))}
                        </div>
                      </CardContent>
                    </Card>
                  )}
                </TabsContent>
                <TabsContent value="parameters">
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-sm">供应商参数</CardTitle>
                    </CardHeader>
                    <CardContent>
                      {Object.keys(selectedSC.parameters).length > 0 ? (
                        <div className="space-y-3">
                          <div className="text-xs text-gray-500 mb-2">
                            供应商: {selectedSC.provisioner}
                          </div>
                          {Object.entries(selectedSC.parameters).map(([key, value]) => (
                            <div key={key} className="p-3 border rounded-lg">
                              <div className="flex items-center justify-between">
                                <span className="font-medium text-sm">{key}</span>
                                <span className="text-sm font-mono bg-gray-100 px-2 py-1 rounded">
                                  {value}
                                </span>
                              </div>
                            </div>
                          ))}
                        </div>
                      ) : (
                        <p className="text-sm text-gray-500">无自定义参数配置</p>
                      )}
                    </CardContent>
                  </Card>
                </TabsContent>
                <TabsContent value="topology">
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-sm">拓扑约束</CardTitle>
                    </CardHeader>
                    <CardContent>
                      {selectedSC.allowedTopologies && selectedSC.allowedTopologies.length > 0 ? (
                        <div className="space-y-3">
                          {selectedSC.allowedTopologies.map((topology, idx) => (
                            <div key={idx} className="p-3 border rounded-lg">
                              <h4 className="text-sm font-medium mb-2">拓扑约束 {idx + 1}</h4>
                              <div className="space-y-2">
                                {Object.entries(topology).map(([key, value]) => (
                                  <div key={key} className="flex justify-between text-sm">
                                    <span className="text-gray-600">{key}:</span>
                                    <span className="font-mono">{JSON.stringify(value)}</span>
                                  </div>
                                ))}
                              </div>
                            </div>
                          ))}
                        </div>
                      ) : (
                        <p className="text-sm text-gray-500">无拓扑约束配置</p>
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

export default StorageClassesDashboard;
