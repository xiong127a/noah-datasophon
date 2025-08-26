/**
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 * @description Kubernetes Secrets管理面板
 */

"use client";

import React, { useState, useEffect, useMemo } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  Search,

  Download,
  RefreshCw,

  Eye,
  EyeOff,

  Shield,
  Key,
  Lock,
  AlertCircle,

  Box,

  Copy,

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

interface SecretsDashboardProps {
  clusterId: string;
  namespace: string;
  className?: string;
}

interface Secret {
  name: string;
  namespace: string;
  type: string;
  data: Record<string, string>;
  creationTimestamp: string;
  age: string;
  keysCount: number;
  size: string;
}

const SecretsDashboard: React.FC<SecretsDashboardProps> = ({
  clusterId,
  namespace,
  className
}) => {
  const [secrets, setSecrets] = useState<Secret[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [typeFilter, setTypeFilter] = useState<string>("all");

  const [showDetails, setShowDetails] = useState(false);
  const [showValues, setShowValues] = useState<Record<string, boolean>>({});
  // const [selectedSecret, setSelectedSecret] = useState<Secret | null>(null);
  // const [pageNum, setPageNum] = useState(1);
  // const [total, setTotal] = useState(0);
  const [error, setError] = useState<string | null>(null);

  const [pageSize] = useState(20);


  // 筛选和搜索Secrets
  const filteredSecrets = useMemo(() => {
    return secrets.filter(secret => {
      const matchesSearch = 
        secret.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        secret.namespace.toLowerCase().includes(searchTerm.toLowerCase()) ||
        secret.type.toLowerCase().includes(searchTerm.toLowerCase());

      const matchesType = typeFilter === "all" || secret.type === typeFilter;

      return matchesSearch && matchesType;
    });
  }, [secrets, searchTerm, typeFilter]);

  // 统计信息
  const stats = useMemo(() => {
    const typeStats = secrets.reduce((acc, secret) => {
      acc[secret.type] = (acc[secret.type] || 0) + 1;
      return acc;
    }, {} as Record<string, number>);

    return {
      total: secrets.length,
      totalKeys: secrets.reduce((sum, secret) => sum + secret.keysCount, 0),
      types: Object.keys(typeStats).length,
      mostCommonType: Object.entries(typeStats).sort(([,a], [,b]) => b - a)[0]?.[0] || 'N/A'
    };
  }, [secrets]);

  // 获取Secrets数据
  const fetchSecrets = async () => {
    if (!clusterId) return;
    
    setLoading(true);
    setError(null);
    try {
      const response: K8sResourceListResponse = await KubernetesAPI.getSecrets(
        clusterId,
        namespace || undefined,
        pageNum,
        pageSize
      );

      // 转换API响应为组件需要的Secret格式
      const convertedSecrets: Secret[] = response.data.map((resource: K8sResource) => ({
        name: resource.name,
        namespace: resource.namespace,
        type: (resource.spec as any)?.type || 'Opaque',
        data: (resource.spec as any)?.data || {},
        creationTimestamp: resource.creationTimestamp,
        age: resource.age || '-',
        keysCount: Object.keys((resource.spec as any)?.data || {}).length,
        size: calculateSecretSize((resource.spec as any)?.data || {})
      }));

      setSecrets(convertedSecrets);
      setTotal(response.total || convertedSecrets.length);
    } catch (error) {
      console.error('获取Secrets失败:', error);
      setError(error instanceof Error ? error.message : '获取Secrets失败');
      setSecrets([]);
    } finally {
      setLoading(false);
    }
  };

  // 计算Secret大小
  const calculateSecretSize = (data: Record<string, string>): string => {
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

  // 获取Secret类型颜色
  const getSecretTypeColor = (type: string) => {
    const colors: Record<string, string> = {
      'Opaque': 'bg-gray-100 text-gray-700 border-gray-200',
      'kubernetes.io/service-account-token': 'bg-blue-100 text-blue-700 border-blue-200',
      'kubernetes.io/dockercfg': 'bg-green-100 text-green-700 border-green-200',
      'kubernetes.io/dockerconfigjson': 'bg-green-100 text-green-700 border-green-200',
      'kubernetes.io/basic-auth': 'bg-yellow-100 text-yellow-700 border-yellow-200',
      'kubernetes.io/ssh-auth': 'bg-purple-100 text-purple-700 border-purple-200',
      'kubernetes.io/tls': 'bg-red-100 text-red-700 border-red-200'
    };
    return colors[type] || colors['Opaque'];
  };

  // 获取Secret类型图标
  const getSecretTypeIcon = (type: string) => {
    const icons: Record<string, React.ElementType> = {
      'Opaque': Shield,
      'kubernetes.io/service-account-token': Key,
      'kubernetes.io/dockercfg': Box,
      'kubernetes.io/dockerconfigjson': Box,
      'kubernetes.io/basic-auth': Lock,
      'kubernetes.io/ssh-auth': Key,
      'kubernetes.io/tls': Shield
    };
    return icons[type] || Shield;
  };

  // Base64解码
  const decodeBase64 = (str: string): string => {
    try {
      return atob(str);
    } catch {
      return '[无法解码]';
    }
  };

  // 切换值显示
  const toggleShowValue = (secretName: string, key: string) => {
    const toggleKey = `${secretName}-${key}`;
    setShowValues(prev => ({
      ...prev,
      [toggleKey]: !prev[toggleKey]
    }));
  };

  // 刷新数据
  const handleRefresh = async () => {
    await fetchSecrets();
  };



  // 获取可用的Secret类型
  const secretTypes = useMemo(() => {
    const types = [...new Set(secrets.map(s => s.type))];
    return types;
  }, [secrets]);

  // 组件挂载和依赖更新时获取数据
  useEffect(() => {
    fetchSecrets();
  }, [clusterId, namespace, pageNum]);

  if (loading && secrets.length === 0) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex items-center space-x-2">
          <RefreshCw className="w-6 h-6 animate-spin text-blue-500" />
          <span className="text-gray-600">加载Secrets...</span>
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
            description: "Secrets总数"
          },
          { 
            title: "密钥数", 
            count: stats.totalKeys, 
            color: "green", 
            icon: Key,
            description: "总密钥数量"
          },
          { 
            title: "类型数", 
            count: stats.types, 
            color: "purple", 
            icon: Shield,
            description: "Secret类型数"
          },
          { 
            title: "主要类型", 
            count: stats.mostCommonType, 
            color: "orange", 
            icon: Lock,
            description: "最常用类型",
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
                    <p className="text-2xl font-bold text-gray-900">
                      {stat.isText ? String(stat.count).split('.').pop() : stat.count}
                    </p>
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
              <CardTitle className="text-lg">Secrets</CardTitle>
              <CardDescription>管理Kubernetes密钥</CardDescription>
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
                placeholder="搜索Secrets..."
                className="pl-10"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
            <Select value={typeFilter} onValueChange={setTypeFilter}>
              <SelectTrigger className="w-40">
                <SelectValue placeholder="选择类型" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">所有类型</SelectItem>
                {secretTypes.map(type => (
                  <SelectItem key={type} value={type}>
                    {type.split('/').pop()}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Button variant="outline" size="sm">
              <Download className="w-4 h-4 mr-2" />
              导出
            </Button>
          </div>

          {/* Secrets表格 */}
          <div className="border rounded-lg">
            <Table>
              <TableHeader>
                <TableRow className="bg-gray-50">
                  <TableHead>名称</TableHead>
                  <TableHead>命名空间</TableHead>
                  <TableHead>类型</TableHead>
                  <TableHead>密钥数量</TableHead>
                  <TableHead>大小</TableHead>
                  <TableHead>创建时间</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <AnimatePresence>
                  {filteredSecrets.map((secret, index) => {
                    const Icon = getSecretTypeIcon(secret.type);
                    return (
                      <motion.tr
                        key={secret.name}
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ delay: index * 0.05 }}
                        className="hover:bg-gray-50 transition-colors duration-200"
                      >
                        <TableCell>
                          <div className="flex items-center space-x-3">
                            <div className="w-8 h-8 bg-red-100 rounded-lg flex items-center justify-center">
                              <Icon className="w-4 h-4 text-red-600" />
                            </div>
                            <div>
                              <div className="font-medium text-gray-900">{secret.name}</div>
                            </div>
                          </div>
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline" className="text-xs">
                            {secret.namespace}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <Badge className={`text-xs ${getSecretTypeColor(secret.type)}`}>
                            {secret.type.split('/').pop()}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center space-x-2">
                            <Key className="w-4 h-4 text-gray-400" />
                            <span>{secret.keysCount}</span>
                          </div>
                        </TableCell>
                        <TableCell>
                          <span className="text-sm text-gray-600">{secret.size}</span>
                        </TableCell>
                        <TableCell>
                          <div className="text-sm text-gray-600">
                            <div>{getAge(secret.creationTimestamp)}</div>
                          </div>
                        </TableCell>

                      </motion.tr>
                    );
                  })}
                </AnimatePresence>
              </TableBody>
            </Table>
          </div>

          {filteredSecrets.length === 0 && !loading && (
            <div className="text-center py-8">
              <Shield className="w-12 h-12 text-gray-400 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">暂无Secrets</h3>
              <p className="text-gray-500 mb-4">
                {searchTerm ? '没有找到匹配的Secrets' : '当前命名空间中没有Secrets'}
              </p>

            </div>
          )}
        </CardContent>
      </Card>

      {/* Secret详情模态框 */}
      {showDetails && selectedSecret && (
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
                  <h2 className="text-xl font-semibold">{selectedSecret.name}</h2>
                  <p className="text-gray-600">Secret详细信息</p>
                </div>
                <Button variant="ghost" onClick={() => setShowDetails(false)}>
                  ✕
                </Button>
              </div>
            </div>
            <div className="p-6 overflow-y-auto" style={{ maxHeight: 'calc(90vh - 140px)' }}>
              <Tabs defaultValue="data">
                <TabsList>
                  <TabsTrigger value="data">密钥数据</TabsTrigger>
                  <TabsTrigger value="metadata">元数据</TabsTrigger>
                </TabsList>
                <TabsContent value="data" className="space-y-4">
                  <div className="grid gap-4">
                    {Object.entries(selectedSecret.data).map(([key, value]) => {
                      const toggleKey = `${selectedSecret.name}-${key}`;
                      const shouldShow = showValues[toggleKey];
                      return (
                        <Card key={key}>
                          <CardHeader className="pb-2">
                            <div className="flex items-center justify-between">
                              <CardTitle className="text-sm font-medium">{key}</CardTitle>
                              <div className="flex items-center space-x-2">
                                <Button 
                                  variant="ghost" 
                                  size="sm"
                                  onClick={() => toggleShowValue(selectedSecret.name, key)}
                                >
                                  {shouldShow ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                                </Button>
                                <Button variant="ghost" size="sm">
                                  <Copy className="w-4 h-4" />
                                </Button>
                              </div>
                            </div>
                          </CardHeader>
                          <CardContent>
                            <pre className="text-xs bg-gray-50 p-3 rounded border overflow-x-auto">
                              {shouldShow ? decodeBase64(value) : '••••••••••••••••••••'}
                            </pre>
                          </CardContent>
                        </Card>
                      );
                    })}
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
                        <p className="text-sm">{selectedSecret.name}</p>
                      </div>
                      <div>
                        <label className="text-xs font-medium text-gray-500">命名空间</label>
                        <p className="text-sm">{selectedSecret.namespace}</p>
                      </div>
                      <div>
                        <label className="text-xs font-medium text-gray-500">类型</label>
                        <p className="text-sm">{selectedSecret.type}</p>
                      </div>
                      <div>
                        <label className="text-xs font-medium text-gray-500">密钥数量</label>
                        <p className="text-sm">{selectedSecret.keysCount}</p>
                      </div>
                      <div className="col-span-2">
                        <label className="text-xs font-medium text-gray-500">创建时间</label>
                        <p className="text-sm">{new Date(selectedSecret.creationTimestamp).toLocaleString()}</p>
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

export default SecretsDashboard;
