/**
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 * @description Kubernetes Jobs管理面板
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
  Clock,
  Briefcase,
  CheckCircle,
  AlertCircle,
  XCircle,
  Box,
  ChevronDown,
  ChevronRight,
  Activity,
  Calendar,
  Timer,
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

interface JobsDashboardProps {
  clusterId: string;
  namespace: string;
  className?: string;
}

interface Job {
  name: string;
  namespace: string;
  completions: number;
  successful: number;
  parallelism: number;
  backoffLimit: number;
  activeDeadlineSeconds?: number;
  startTime?: string;
  completionTime?: string;
  duration: string;
  age: string;
  creationTimestamp: string;
  status: 'Complete' | 'Failed' | 'Running' | 'Suspended' | 'Pending';
  conditions: Array<{
    type: string;
    status: string;
    reason?: string;
    message?: string;
  }>;
}

const JobsDashboard: React.FC<JobsDashboardProps> = ({
  clusterId,
  namespace,
  className
}) => {
  const [jobs, setJobs] = useState<Job[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("all");
  const [selectedJob, setSelectedJob] = useState<Job | null>(null);
  const [showDetails, setShowDetails] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize] = useState(20);
  const [total, setTotal] = useState(0);

  // 筛选和搜索Jobs
  const filteredJobs = useMemo(() => {
    return jobs.filter(job => {
      const matchesSearch = 
        job.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        job.namespace.toLowerCase().includes(searchTerm.toLowerCase());

      const matchesStatus = statusFilter === "all" || job.status.toLowerCase() === statusFilter.toLowerCase();

      return matchesSearch && matchesStatus;
    });
  }, [jobs, searchTerm, statusFilter]);

  // 统计信息
  const stats = useMemo(() => {
    return {
      total: jobs.length,
      complete: jobs.filter(job => job.status === 'Complete').length,
      failed: jobs.filter(job => job.status === 'Failed').length,
      running: jobs.filter(job => job.status === 'Running').length,
      suspended: jobs.filter(job => job.status === 'Suspended').length,
      successRate: jobs.length > 0 
        ? Math.round((jobs.filter(job => job.status === 'Complete').length / jobs.length) * 100)
        : 0
    };
  }, [jobs]);

  // 获取Jobs数据
  const fetchJobs = async () => {
    if (!clusterId) return;
    
    setLoading(true);
    setError(null);
    try {
      const response: K8sResourceListResponse = await KubernetesAPI.getJobs(
        clusterId,
        namespace || undefined,
        undefined, // serviceId
        pageNum,
        pageSize
      );

      // 转换API响应为组件需要的Job格式
      const convertedJobs: Job[] = response.data.map((resource: K8sResource) => {
        const spec = resource.spec as any;
        const status = resource.metadata as any;
        
        const startTime = status?.startTime;
        const completionTime = status?.completionTime;
        const duration = calculateDuration(startTime, completionTime);
        
        return {
          name: resource.name,
          namespace: resource.namespace,
          completions: spec?.completions || 1,
          successful: status?.succeeded || 0,
          parallelism: spec?.parallelism || 1,
          backoffLimit: spec?.backoffLimit || 6,
          activeDeadlineSeconds: spec?.activeDeadlineSeconds,
          startTime,
          completionTime,
          duration,
          age: resource.age || '-',
          creationTimestamp: resource.creationTimestamp,
          status: determineJobStatus(status),
          conditions: status?.conditions || []
        };
      });

      setJobs(convertedJobs);
      setTotal(response.total || convertedJobs.length);
    } catch (error) {
      console.error('获取Jobs失败:', error);
      setError(error instanceof Error ? error.message : '获取Jobs失败');
      setJobs([]);
    } finally {
      setLoading(false);
    }
  };

  // 确定Job状态
  const determineJobStatus = (status: any): 'Complete' | 'Failed' | 'Running' | 'Suspended' | 'Pending' => {
    if (!status) return 'Pending';
    
    const conditions = status.conditions || [];
    const completedCondition = conditions.find((c: any) => c.type === 'Complete');
    const failedCondition = conditions.find((c: any) => c.type === 'Failed');
    const suspendedCondition = conditions.find((c: any) => c.type === 'Suspended');
    
    if (completedCondition && completedCondition.status === 'True') return 'Complete';
    if (failedCondition && failedCondition.status === 'True') return 'Failed';
    if (suspendedCondition && suspendedCondition.status === 'True') return 'Suspended';
    if (status.active > 0) return 'Running';
    
    return 'Pending';
  };

  // 计算持续时间
  const calculateDuration = (startTime?: string, completionTime?: string): string => {
    if (!startTime) return '-';
    
    const start = new Date(startTime);
    const end = completionTime ? new Date(completionTime) : new Date();
    const diffMs = end.getTime() - start.getTime();
    
    const hours = Math.floor(diffMs / (1000 * 60 * 60));
    const minutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((diffMs % (1000 * 60)) / 1000);
    
    if (hours > 0) return `${hours}h${minutes}m`;
    if (minutes > 0) return `${minutes}m${seconds}s`;
    return `${seconds}s`;
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
      'Complete': { color: 'text-green-600', bgColor: 'bg-green-100', icon: CheckCircle },
      'Failed': { color: 'text-red-600', bgColor: 'bg-red-100', icon: XCircle },
      'Running': { color: 'text-blue-600', bgColor: 'bg-blue-100', icon: Activity },
      'Suspended': { color: 'text-orange-600', bgColor: 'bg-orange-100', icon: Pause },
      'Pending': { color: 'text-yellow-600', bgColor: 'bg-yellow-100', icon: Clock }
    };
    return displays[status as keyof typeof displays] || displays['Pending'];
  };

  // 计算完成率
  const getCompletionPercentage = (successful: number, completions: number): number => {
    if (completions === 0) return 100;
    return Math.min(Math.round((successful / completions) * 100), 100);
  };

  // 格式化超时时间
  const formatDeadline = (seconds?: number): string => {
    if (!seconds) return '无限制';
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    if (hours > 0) return `${hours}h${minutes > 0 ? minutes + 'm' : ''}`;
    return `${minutes}m`;
  };

  // 刷新数据
  const handleRefresh = async () => {
    await fetchJobs();
  };

  // Job操作
  const handleJobAction = (action: string, job: Job) => {
    console.log(`执行操作: ${action} on Job: ${job.name}`);
    switch (action) {
      case 'view':
        setSelectedJob(job);
        setShowDetails(true);
        break;
      case 'suspend':
        // 实现暂停逻辑
        break;
      case 'resume':
        // 实现恢复逻辑
        break;
      case 'delete':
        // 实现删除逻辑
        break;
    }
  };

  // 组件挂载和依赖更新时获取数据
  useEffect(() => {
    fetchJobs();
  }, [clusterId, namespace, pageNum]);

  if (loading && jobs.length === 0) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex items-center space-x-2">
          <RefreshCw className="w-6 h-6 animate-spin text-blue-500" />
          <span className="text-gray-600">加载Jobs...</span>
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
            description: "Jobs总数"
          },
          { 
            title: "完成", 
            count: stats.complete, 
            color: "green", 
            icon: CheckCircle,
            description: "成功完成"
          },
          { 
            title: "失败", 
            count: stats.failed, 
            color: "red", 
            icon: XCircle,
            description: "执行失败"
          },
          { 
            title: "运行中", 
            count: stats.running, 
            color: "blue", 
            icon: Activity,
            description: "正在执行"
          },
          { 
            title: "成功率", 
            count: `${stats.successRate}%`, 
            color: "purple", 
            icon: Timer,
            description: "整体成功率",
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
              <CardTitle className="text-lg">Jobs</CardTitle>
              <CardDescription>管理Kubernetes任务</CardDescription>
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
                <Briefcase className="w-4 h-4 mr-2" />
                新建Job
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
                placeholder="搜索Jobs..."
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
                <SelectItem value="complete">完成</SelectItem>
                <SelectItem value="failed">失败</SelectItem>
                <SelectItem value="running">运行中</SelectItem>
                <SelectItem value="suspended">暂停</SelectItem>
                <SelectItem value="pending">等待中</SelectItem>
              </SelectContent>
            </Select>
            <Button variant="outline" size="sm">
              <Download className="w-4 h-4 mr-2" />
              导出
            </Button>
          </div>

          {/* Jobs表格 */}
          <div className="border rounded-lg">
            <Table>
              <TableHeader>
                <TableRow className="bg-gray-50">
                  <TableHead>名称</TableHead>
                  <TableHead>命名空间</TableHead>
                  <TableHead>完成状态</TableHead>
                  <TableHead>完成率</TableHead>
                  <TableHead>持续时间</TableHead>
                  <TableHead>并行度</TableHead>
                  <TableHead>创建时间</TableHead>
                  <TableHead>操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <AnimatePresence>
                  {filteredJobs.map((job, index) => {
                    const statusDisplay = getStatusDisplay(job.status);
                    const completionPercentage = getCompletionPercentage(job.successful, job.completions);
                    return (
                      <motion.tr
                        key={job.name}
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ delay: index * 0.05 }}
                        className="hover:bg-gray-50 transition-colors duration-200"
                      >
                        <TableCell>
                          <div className="flex items-center space-x-3">
                            <div className="w-8 h-8 bg-indigo-100 rounded-lg flex items-center justify-center">
                              <Briefcase className="w-4 h-4 text-indigo-600" />
                            </div>
                            <div>
                              <div className="font-medium text-gray-900">{job.name}</div>
                              <div className="text-xs text-gray-500">
                                重试限制: {job.backoffLimit}
                              </div>
                            </div>
                          </div>
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline" className="text-xs">
                            {job.namespace}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center space-x-2">
                            <statusDisplay.icon className={`w-4 h-4 ${statusDisplay.color}`} />
                            <span className="text-sm">
                              {job.successful}/{job.completions}
                            </span>
                          </div>
                        </TableCell>
                        <TableCell>
                          <div className="space-y-1">
                            <div className="flex items-center justify-between text-xs text-gray-600">
                              <span>{completionPercentage}%</span>
                            </div>
                            <Progress value={completionPercentage} className="h-1.5" />
                          </div>
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center space-x-2">
                            <Timer className="w-4 h-4 text-gray-400" />
                            <span className="text-sm text-gray-600">{job.duration}</span>
                          </div>
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline" className="text-xs">
                            {job.parallelism}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <div className="text-sm text-gray-600">
                            <div>{getAge(job.creationTimestamp)}</div>
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
                              <DropdownMenuItem onClick={() => handleJobAction('view', job)}>
                                <Eye className="w-4 h-4 mr-2" />
                                查看详情
                              </DropdownMenuItem>
                              {job.status === 'Running' && (
                                <DropdownMenuItem onClick={() => handleJobAction('suspend', job)}>
                                  <Pause className="w-4 h-4 mr-2" />
                                  暂停
                                </DropdownMenuItem>
                              )}
                              {job.status === 'Suspended' && (
                                <DropdownMenuItem onClick={() => handleJobAction('resume', job)}>
                                  <Play className="w-4 h-4 mr-2" />
                                  恢复
                                </DropdownMenuItem>
                              )}
                              <DropdownMenuSeparator />
                              <DropdownMenuItem 
                                onClick={() => handleJobAction('delete', job)}
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

          {filteredJobs.length === 0 && !loading && (
            <div className="text-center py-8">
              <Briefcase className="w-12 h-12 text-gray-400 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">暂无Jobs</h3>
              <p className="text-gray-500 mb-4">
                {searchTerm ? '没有找到匹配的Jobs' : '当前命名空间中没有Jobs'}
              </p>
              {!searchTerm && (
                <Button>
                  <Briefcase className="w-4 h-4 mr-2" />
                  创建第一个Job
                </Button>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Job详情模态框 */}
      {showDetails && selectedJob && (
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
                  <h2 className="text-xl font-semibold">{selectedJob.name}</h2>
                  <p className="text-gray-600">Job详细信息</p>
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
                  <TabsTrigger value="execution">执行详情</TabsTrigger>
                  <TabsTrigger value="conditions">条件状态</TabsTrigger>
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
                          <p className="text-sm">{selectedJob.name}</p>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">命名空间</label>
                          <p className="text-sm">{selectedJob.namespace}</p>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">状态</label>
                          <div className="flex items-center space-x-2">
                            {(() => {
                              const display = getStatusDisplay(selectedJob.status);
                              return (
                                <>
                                  <display.icon className={`w-4 h-4 ${display.color}`} />
                                  <span className="text-sm">{selectedJob.status}</span>
                                </>
                              );
                            })()}
                          </div>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">持续时间</label>
                          <p className="text-sm">{selectedJob.duration}</p>
                        </div>
                      </CardContent>
                    </Card>
                    <Card>
                      <CardHeader>
                        <CardTitle className="text-sm">执行配置</CardTitle>
                      </CardHeader>
                      <CardContent className="space-y-3">
                        <div className="grid grid-cols-2 gap-2 text-center">
                          <div>
                            <p className="text-xs text-gray-500">完成数</p>
                            <p className="text-sm font-medium">{selectedJob.completions}</p>
                          </div>
                          <div>
                            <p className="text-xs text-gray-500">并行度</p>
                            <p className="text-sm font-medium">{selectedJob.parallelism}</p>
                          </div>
                        </div>
                        <div className="grid grid-cols-2 gap-2 text-center">
                          <div>
                            <p className="text-xs text-gray-500">重试限制</p>
                            <p className="text-sm font-medium">{selectedJob.backoffLimit}</p>
                          </div>
                          <div>
                            <p className="text-xs text-gray-500">超时时间</p>
                            <p className="text-sm font-medium">{formatDeadline(selectedJob.activeDeadlineSeconds)}</p>
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  </div>
                </TabsContent>
                <TabsContent value="execution">
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-sm">执行详情</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="space-y-4">
                        <div className="grid grid-cols-3 gap-4 text-center">
                          <div className="p-3 bg-green-50 rounded-lg">
                            <p className="text-xs text-green-600 font-medium">成功完成</p>
                            <p className="text-2xl font-bold text-green-700">{selectedJob.successful}</p>
                          </div>
                          <div className="p-3 bg-blue-50 rounded-lg">
                            <p className="text-xs text-blue-600 font-medium">总完成数</p>
                            <p className="text-2xl font-bold text-blue-700">{selectedJob.completions}</p>
                          </div>
                          <div className="p-3 bg-purple-50 rounded-lg">
                            <p className="text-xs text-purple-600 font-medium">并行度</p>
                            <p className="text-2xl font-bold text-purple-700">{selectedJob.parallelism}</p>
                          </div>
                        </div>
                        <div className="grid grid-cols-2 gap-4">
                          <div>
                            <label className="text-xs font-medium text-gray-500">开始时间</label>
                            <p className="text-sm">
                              {selectedJob.startTime 
                                ? new Date(selectedJob.startTime).toLocaleString()
                                : '未开始'
                              }
                            </p>
                          </div>
                          <div>
                            <label className="text-xs font-medium text-gray-500">完成时间</label>
                            <p className="text-sm">
                              {selectedJob.completionTime 
                                ? new Date(selectedJob.completionTime).toLocaleString()
                                : '未完成'
                              }
                            </p>
                          </div>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                </TabsContent>
                <TabsContent value="conditions">
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-sm">条件状态</CardTitle>
                    </CardHeader>
                    <CardContent>
                      {selectedJob.conditions.length > 0 ? (
                        <div className="space-y-3">
                          {selectedJob.conditions.map((condition, index) => (
                            <div key={index} className="p-3 border rounded-lg">
                              <div className="flex items-center justify-between">
                                <div className="flex items-center space-x-2">
                                  {condition.status === 'True' ? (
                                    <CheckCircle className="w-4 h-4 text-green-500" />
                                  ) : condition.status === 'False' ? (
                                    <XCircle className="w-4 h-4 text-red-500" />
                                  ) : (
                                    <Clock className="w-4 h-4 text-yellow-500" />
                                  )}
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
                            </div>
                          ))}
                        </div>
                      ) : (
                        <p className="text-sm text-gray-500">暂无条件信息</p>
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

export default JobsDashboard;
