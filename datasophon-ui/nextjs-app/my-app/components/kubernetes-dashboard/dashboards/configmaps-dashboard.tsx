/**
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 * @description Kubernetes ConfigMaps管理面板
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
  FileText,
  Settings,
  Key,
  AlertCircle,
  CheckCircle,
  Clock,
  Box,
  ChevronDown,
  ChevronRight,
  Copy,
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

interface ConfigMapsDashboardProps {
  clusterId: string;
  namespace: string;
  className?: string;
}

interface ConfigMap {
  name: string;
  namespace: string;
  data: Record<string, string>;
  creationTimestamp: string;
  age: string;
  keysCount: number;
  size: string;
}

const ConfigMapsDashboard: React.FC<ConfigMapsDashboardProps> = ({
  clusterId,
  namespace,
  className
}) => {
  const [configMaps, setConfigMaps] = useState<ConfigMap[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedConfigMap, setSelectedConfigMap] = useState<ConfigMap | null>(null);
  const [showDetails, setShowDetails] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize] = useState(20);
  const [total, setTotal] = useState(0);

  // 筛选和搜索ConfigMaps
  const filteredConfigMaps = useMemo(() => {
    return configMaps.filter(cm => 
      cm.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      cm.namespace.toLowerCase().includes(searchTerm.toLowerCase())
    );
  }, [configMaps, searchTerm]);

  // 统计信息
  const stats = useMemo(() => {
    return {
      total: configMaps.length,
      totalKeys: configMaps.reduce((sum, cm) => sum + cm.keysCount, 0),
      avgKeysPerConfigMap: configMaps.length > 0 
        ? Math.round(configMaps.reduce((sum, cm) => sum + cm.keysCount, 0) / configMaps.length)
        : 0
    };
  }, [configMaps]);

  // 获取ConfigMaps数据
  const fetchConfigMaps = async () => {
    if (!clusterId) return;
    
    setLoading(true);
    setError(null);
    try {
      const response: K8sResourceListResponse = await KubernetesAPI.getConfigMaps(
        clusterId,
        namespace || undefined,
        pageNum,
        pageSize
      );

      // 转换API响应为组件需要的ConfigMap格式
      const convertedConfigMaps: ConfigMap[] = response.data.map((resource: K8sResource) => ({
        name: resource.name,
        namespace: resource.namespace,
        data: (resource.spec as any)?.data || {},
        creationTimestamp: resource.creationTimestamp,
        age: resource.age || '-',
        keysCount: Object.keys((resource.spec as any)?.data || {}).length,
        size: calculateConfigMapSize((resource.spec as any)?.data || {})
      }));

      setConfigMaps(convertedConfigMaps);
      setTotal(response.total || convertedConfigMaps.length);
    } catch (error) {
      console.error('获取ConfigMaps失败:', error);
      setError(error instanceof Error ? error.message : '获取ConfigMaps失败');
      setConfigMaps([]);
    } finally {
      setLoading(false);
    }
  };

  // 计算ConfigMap大小
  const calculateConfigMapSize = (data: Record<string, string>): string => {
    const totalSize = Object.values(data).join('').length;
    if (totalSize < 1024) return `${totalSize}B`;
    if (totalSize < 1024 * 1024) return `${(totalSize / 1024).toFixed(1)}KB`;
    return `${(totalSize / (1024 * 1024)).toFixed(1)}MB`;
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

  // 刷新数据
  const handleRefresh = async () => {
    await fetchConfigMaps();
  };



  // 组件挂载和依赖更新时获取数据
  useEffect(() => {
    fetchConfigMaps();
  }, [clusterId, namespace, pageNum]);

  if (loading && configMaps.length === 0) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex items-center space-x-2">
          <RefreshCw className="w-6 h-6 animate-spin text-blue-500" />
          <span className="text-gray-600">加载ConfigMaps...</span>
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
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {[
          { 
            title: "总计", 
            count: stats.total, 
            color: "blue", 
            icon: Box,
            description: "ConfigMaps总数"
          },
          { 
            title: "配置项", 
            count: stats.totalKeys, 
            color: "green", 
            icon: Key,
            description: "总配置键数量"
          },
          { 
            title: "平均密度", 
            count: stats.avgKeysPerConfigMap, 
            color: "purple", 
            icon: Settings,
            description: "每个ConfigMap平均配置数"
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
              <CardTitle className="text-lg">ConfigMaps</CardTitle>
              <CardDescription>管理Kubernetes配置映射</CardDescription>
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
                placeholder="搜索ConfigMaps..."
                className="pl-10"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
            <Button variant="outline" size="sm">
              <Filter className="w-4 h-4 mr-2" />
              过滤
            </Button>
            <Button variant="outline" size="sm">
              <Download className="w-4 h-4 mr-2" />
              导出
            </Button>
          </div>

          {/* ConfigMaps表格 */}
          <div className="border rounded-lg">
            <Table>
              <TableHeader>
                <TableRow className="bg-gray-50">
                  <TableHead>名称</TableHead>
                  <TableHead>命名空间</TableHead>
                  <TableHead>配置项数量</TableHead>
                  <TableHead>大小</TableHead>
                  <TableHead>创建时间</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <AnimatePresence>
                  {filteredConfigMaps.map((configMap, index) => (
                    <motion.tr
                      key={configMap.name}
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      exit={{ opacity: 0 }}
                      transition={{ delay: index * 0.05 }}
                      className="hover:bg-gray-50 transition-colors duration-200"
                    >
                      <TableCell>
                        <div className="flex items-center space-x-3">
                          <div className="w-8 h-8 bg-blue-100 rounded-lg flex items-center justify-center">
                            <FileText className="w-4 h-4 text-blue-600" />
                          </div>
                          <div>
                            <div className="font-medium text-gray-900">{configMap.name}</div>
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge variant="outline" className="text-xs">
                          {configMap.namespace}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center space-x-2">
                          <Key className="w-4 h-4 text-gray-400" />
                          <span>{configMap.keysCount}</span>
                        </div>
                      </TableCell>
                      <TableCell>
                        <span className="text-sm text-gray-600">{configMap.size}</span>
                      </TableCell>
                      <TableCell>
                        <div className="text-sm text-gray-600">
                          <div>{getAge(configMap.creationTimestamp)}</div>
                        </div>
                      </TableCell>
                      
                    </motion.tr>
                  ))}
                </AnimatePresence>
              </TableBody>
            </Table>
          </div>

          {filteredConfigMaps.length === 0 && !loading && (
            <div className="text-center py-8">
              <FileText className="w-12 h-12 text-gray-400 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">暂无ConfigMaps</h3>
              <p className="text-gray-500 mb-4">
                {searchTerm ? '没有找到匹配的ConfigMaps' : '当前命名空间中没有ConfigMaps'}
              </p>

            </div>
          )}
        </CardContent>
      </Card>

      {/* ConfigMap详情模态框 */}
      {showDetails && selectedConfigMap && (
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
                  <h2 className="text-xl font-semibold">{selectedConfigMap.name}</h2>
                  <p className="text-gray-600">ConfigMap详细信息</p>
                </div>
                <Button variant="ghost" onClick={() => setShowDetails(false)}>
                  ✕
                </Button>
              </div>
            </div>
            <div className="p-6 overflow-y-auto" style={{ maxHeight: 'calc(90vh - 140px)' }}>
              <Tabs defaultValue="data">
                <TabsList>
                  <TabsTrigger value="data">配置数据</TabsTrigger>
                  <TabsTrigger value="metadata">元数据</TabsTrigger>
                </TabsList>
                <TabsContent value="data" className="space-y-4">
                  <div className="grid gap-4">
                    {Object.entries(selectedConfigMap.data).map(([key, value]) => (
                      <Card key={key}>
                        <CardHeader className="pb-2">
                          <div className="flex items-center justify-between">
                            <CardTitle className="text-sm font-medium">{key}</CardTitle>
                            <Button variant="ghost" size="sm">
                              <Copy className="w-4 h-4" />
                            </Button>
                          </div>
                        </CardHeader>
                        <CardContent>
                          <pre className="text-xs bg-gray-50 p-3 rounded border overflow-x-auto">
                            {value}
                          </pre>
                        </CardContent>
                      </Card>
                    ))}
                  </div>
                </TabsContent>
                <TabsContent value="metadata" className="space-y-4">
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-sm">基本信息</CardTitle>
                    </CardHeader>
                    <CardContent className="grid grid-cols-2 gap-4">
                      <div>
                        <label className="text-xs font-medium text-gray-500">名称</label>
                        <p className="text-sm">{selectedConfigMap.name}</p>
                      </div>
                      <div>
                        <label className="text-xs font-medium text-gray-500">命名空间</label>
                        <p className="text-sm">{selectedConfigMap.namespace}</p>
                      </div>
                      <div>
                        <label className="text-xs font-medium text-gray-500">创建时间</label>
                        <p className="text-sm">{new Date(selectedConfigMap.creationTimestamp).toLocaleString()}</p>
                      </div>
                      <div>
                        <label className="text-xs font-medium text-gray-500">配置项数量</label>
                        <p className="text-sm">{selectedConfigMap.keysCount}</p>
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

export default ConfigMapsDashboard;
