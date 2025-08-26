/**
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 * @description Kubernetes CronJobs管理面板
 */

"use client";

import React, { useState, useEffect, useMemo, useCallback } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  Search,
  RefreshCw,
  Eye,
  Clock,
  Calendar,
  CheckCircle,
  AlertCircle,
  Box,
  Play,
  Pause
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
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

import { CronJob } from "../types";
import { KubernetesAPI } from '@/lib/kubernetes-api';
import KubernetesPagination from "../components/kubernetes-pagination";

interface CronJobsDashboardProps {
  clusterId: string;
  serviceId?: string;
  namespace: string;
  className?: string;
}

// API响应数据的临时接口
interface CronJobApiResource {
  name: string;
  namespace: string;
  creationTimestamp?: string;
  labels?: Record<string, string>;
  schedule?: string;
  suspend?: boolean;
  [key: string]: unknown;
}

interface ApiResponse {
  data: CronJobApiResource[] | { data: CronJobApiResource[]; total?: string | number };
  total?: string | number;
  [key: string]: unknown;
}

const CronJobsDashboard: React.FC<CronJobsDashboardProps> = ({
  clusterId,
  serviceId,
  namespace,
  className
}) => {
  const [cronJobs, setCronJobs] = useState<CronJob[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("all");
  const [error, setError] = useState<string | null>(null);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  // 获取CronJobs数据
  const fetchCronJobs = useCallback(async () => {
    if (!clusterId) return;
    
    setLoading(true);
    setError(null);
    try {
      console.log('📡 调用 KubernetesAPI.getCronJobs API...');
      console.log('🔍 CronJobs 调用参数:', { 
        clusterId, 
        namespace: namespace || undefined, 
        serviceId: serviceId || undefined, 
        pageNum, 
        pageSize 
      });
      const response = await KubernetesAPI.getCronJobs(
        clusterId,
        namespace || undefined,
        serviceId || undefined,
        pageNum,
        pageSize
      );
      console.log('✅ 获取CronJobs成功，数据结构:', response);
      console.log('✅ 获取CronJobs成功，数量:', response.data?.length);
      console.log('✅ 实际数据数组:', response.data);
      
      // 检查数据结构并提取实际的数组
      const apiResponse = response as unknown as ApiResponse;
      const dataArray = Array.isArray(apiResponse.data) 
        ? apiResponse.data 
        : (apiResponse.data as { data: CronJobApiResource[]; total?: string | number })?.data || [];
      console.log('✅ 使用的数据数组:', dataArray, '长度:', dataArray.length);

      // 转换API响应为组件需要的CronJob格式
      const convertedCronJobs: CronJob[] = dataArray.map((resource: CronJobApiResource) => ({
        apiVersion: "batch/v1",
        kind: "CronJob",
        metadata: {
          name: resource.name,
          namespace: resource.namespace,
          creationTimestamp: resource.creationTimestamp || new Date().toISOString(),
          labels: resource.labels || {}
        },
        spec: {
          schedule: resource.schedule || "0 0 * * *",
          jobTemplate: {
            spec: {
              template: {
                spec: {
                  containers: [{
                    name: resource.name,
                    image: "unknown:latest"
                  }],
                  restartPolicy: "OnFailure"
                }
              }
            }
          },
          suspend: resource.suspend || false
        },
        status: {
          lastScheduleTime: resource.creationTimestamp,
          active: []
        }
      }));

      setCronJobs(convertedCronJobs);
      
      // 使用正确的总数：优先使用API返回的total，其次使用数据长度
      const nestedData = !Array.isArray(apiResponse.data) ? apiResponse.data as { data: CronJobApiResource[]; total?: string | number } : null;
      const totalCount = apiResponse.total || nestedData?.total || convertedCronJobs.length;
      console.log('✅ 设置总数:', totalCount, '来源:', { responseTotal: apiResponse.total, dataTotal: nestedData?.total, arrayLength: convertedCronJobs.length });
      setTotal(typeof totalCount === 'string' ? parseInt(totalCount) : totalCount);
    } catch (error) {
      console.error('获取CronJobs失败:', error);
      setError(error instanceof Error ? error.message : '获取CronJobs失败');
      setCronJobs([]);
    } finally {
      setLoading(false);
    }
  }, [clusterId, serviceId, namespace, pageNum, pageSize]);

  // 筛选和搜索CronJobs
  const filteredCronJobs = useMemo(() => {
    return cronJobs.filter(cronJob => {
      const matchesSearch = 
        cronJob.metadata.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        cronJob.spec.schedule.toLowerCase().includes(searchTerm.toLowerCase()) ||
        Object.keys(cronJob.metadata.labels || {}).some(key => 
          key.toLowerCase().includes(searchTerm.toLowerCase()) ||
          (cronJob.metadata.labels?.[key] || "").toLowerCase().includes(searchTerm.toLowerCase())
        );

      let matchesStatus = true;
      if (statusFilter !== "all") {
        const isSuspended = cronJob.spec.suspend || false;
        const isActive = (cronJob.status?.active?.length || 0) > 0;
        
        switch (statusFilter) {
          case "active":
            matchesStatus = !isSuspended && isActive;
            break;
          case "suspended":
            matchesStatus = isSuspended;
            break;
          case "idle":
            matchesStatus = !isSuspended && !isActive;
            break;
          default:
            matchesStatus = true;
        }
      }

      return matchesSearch && matchesStatus;
    });
  }, [cronJobs, searchTerm, statusFilter]);

  // 刷新数据
  const handleRefresh = async () => {
    await fetchCronJobs();
  };

  // 分页处理函数
  const handlePageChange = (page: number) => {
    setPageNum(page);
  };

  const handlePageSizeChange = (size: number) => {
    setPageSize(size);
    setPageNum(1); // 重置到第一页
  };

  // 组件挂载和依赖更新时获取数据
  useEffect(() => {
    fetchCronJobs();
  }, [fetchCronJobs]);

  // 统计信息
  const stats = useMemo(() => {
    return {
      total: cronJobs.length,
      active: cronJobs.filter(c => !c.spec.suspend && (c.status?.active?.length || 0) > 0).length,
      suspended: cronJobs.filter(c => c.spec.suspend).length,
      idle: cronJobs.filter(c => !c.spec.suspend && (c.status?.active?.length || 0) === 0).length
    };
  }, [cronJobs]);

  // 获取CronJob年龄
  const getCronJobAge = (creationTimestamp: string) => {
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

  // 获取最后执行时间
  const getLastScheduleTime = (cronJob: CronJob) => {
    if (cronJob.status?.lastScheduleTime) {
      return getCronJobAge(cronJob.status.lastScheduleTime);
    }
    return "从未执行";
  };

  // 获取CronJob状态
  const getCronJobStatus = (cronJob: CronJob) => {
    const isSuspended = cronJob.spec.suspend || false;
    const isActive = (cronJob.status?.active?.length || 0) > 0;
    
    if (isSuspended) {
      return { status: "已暂停", color: "text-gray-600", icon: Pause, bgColor: "bg-gray-100" };
    } else if (isActive) {
      return { status: "运行中", color: "text-green-600", icon: Play, bgColor: "bg-green-100" };
    } else {
      return { status: "空闲", color: "text-blue-600", icon: Clock, bgColor: "bg-blue-100" };
    }
  };



  if (loading && cronJobs.length === 0) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex items-center space-x-2">
          <RefreshCw className="w-6 h-6 animate-spin text-blue-500" />
          <span className="text-gray-600">加载CronJobs...</span>
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
          { title: "总计", count: stats.total, color: "blue", icon: Box },
          { title: "运行中", count: stats.active, color: "green", icon: Play },
          { title: "已暂停", count: stats.suspended, color: "gray", icon: Pause },
          { title: "空闲", count: stats.idle, color: "blue", icon: Clock }
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
            <CardTitle className="text-lg font-semibold">CronJobs 列表</CardTitle>
            <div className="flex items-center space-x-3">
              {/* 搜索框 */}
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
                <Input
                  placeholder="搜索 CronJobs..."
                  className="pl-10 w-64"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>

              {/* 状态筛选 */}
              <Select value={statusFilter} onValueChange={setStatusFilter}>
                <SelectTrigger className="w-32">
                  <SelectValue placeholder="状态筛选" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">全部</SelectItem>
                  <SelectItem value="active">运行中</SelectItem>
                  <SelectItem value="suspended">已暂停</SelectItem>
                  <SelectItem value="idle">空闲</SelectItem>
                </SelectContent>
              </Select>

              {/* 刷新按钮 */}
              <Button variant="outline" size="icon" onClick={handleRefresh} disabled={loading}>
                <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
              </Button>
            </div>
          </div>
        </CardHeader>

        <CardContent className="p-0">

          {/* CronJobs表格 */}
          <div className="border rounded-lg">
            <Table>
              <TableHeader>
                <TableRow className="bg-gray-50">
                  <TableHead>名称</TableHead>
                  <TableHead>调度规则</TableHead>
                  <TableHead>最后执行</TableHead>
                  <TableHead>活跃任务</TableHead>
                  <TableHead>状态</TableHead>
                  <TableHead>年龄</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <AnimatePresence>
                  {filteredCronJobs.map((cronJob, index) => {
                    const activeJobs = cronJob.status?.active?.length || 0;
                    const statusInfo = getCronJobStatus(cronJob);
                    
                    return (
                      <motion.tr
                        key={cronJob.metadata.name}
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ delay: index * 0.05 }}
                        className="hover:bg-gray-50 transition-colors duration-200"
                      >
                        <TableCell>
                          <div className="flex items-center space-x-3">
                            <div className="w-8 h-8 bg-indigo-100 rounded-lg flex items-center justify-center">
                              <Clock className="w-4 h-4 text-indigo-600" />
                            </div>
                            <div>
                              <div className="font-medium text-gray-900">{cronJob.metadata.name}</div>
                              <div className="text-sm text-gray-500">{cronJob.metadata.namespace}</div>
                            </div>
                          </div>
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center space-x-2">
                            <Calendar className="w-4 h-4 text-gray-400" />
                            <code className="bg-gray-100 px-2 py-1 rounded text-xs font-mono">
                              {cronJob.spec.schedule}
                            </code>
                          </div>
                        </TableCell>
                        <TableCell>
                          <span className="text-sm text-gray-600">
                            {getLastScheduleTime(cronJob)}
                          </span>
                        </TableCell>
                        <TableCell>
                          <span className="text-sm font-medium">
                            {activeJobs} 个任务
                          </span>
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center space-x-2">
                            <statusInfo.icon className={`w-4 h-4 ${statusInfo.color}`} />
                            <span className="text-sm">{statusInfo.status}</span>
                          </div>
                        </TableCell>
                        <TableCell>
                          <span className="text-sm text-gray-600">
                            {getCronJobAge(cronJob.metadata.creationTimestamp)}
                          </span>
                        </TableCell>

                      </motion.tr>
                    );
                  })}
                </AnimatePresence>
              </TableBody>
            </Table>
          </div>

          {filteredCronJobs.length === 0 && !loading && (
            <div className="text-center py-12">
              <Clock className="w-16 h-16 text-gray-300 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">暂无CronJobs</h3>
              <p className="text-gray-500">
                {searchTerm || statusFilter !== "all" 
                  ? '没有找到匹配的CronJobs' 
                  : '当前命名空间中没有CronJobs资源'
                }
              </p>
            </div>
          )}
        </CardContent>

        {/* 分页控件 */}
        {total > 0 && (
          <div className="p-4 border-t border-gray-100 bg-gray-50/50">
            <KubernetesPagination
              currentPage={pageNum}
              pageSize={pageSize}
              total={total}
              onPageChange={handlePageChange}
              onPageSizeChange={handlePageSizeChange}
              loading={loading}
              className="!bg-transparent !border-0 !shadow-none !p-0"
            />
          </div>
        )}
      </Card>
    </div>
  );
};

export default CronJobsDashboard;