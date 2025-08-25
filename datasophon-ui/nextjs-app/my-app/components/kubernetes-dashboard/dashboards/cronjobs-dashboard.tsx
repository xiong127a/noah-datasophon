/**
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 * @description Kubernetes CronJobs管理面板
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
  Calendar,
  CheckCircle,
  AlertCircle,
  XCircle,
  Box,
  ChevronDown,
  ChevronRight,
  Activity,
  Timer,
  CalendarClock,
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

interface CronJobsDashboardProps {
  clusterId: string;
  namespace: string;
  className?: string;
}

interface CronJob {
  name: string;
  namespace: string;
  schedule: string;
  suspend: boolean;
  active: number;
  lastScheduleTime?: string;
  nextScheduleTime?: string;
  successfulJobsHistoryLimit: number;
  failedJobsHistoryLimit: number;
  concurrencyPolicy: string;
  startingDeadlineSeconds?: number;
  age: string;
  creationTimestamp: string;
  status: 'Active' | 'Suspended' | 'Failed' | 'Pending';
  lastSuccessfulTime?: string;
  recentJobs: Array<{
    name: string;
    status: string;
    startTime: string;
    completionTime?: string;
  }>;
}

const CronJobsDashboard: React.FC<CronJobsDashboardProps> = ({
  clusterId,
  namespace,
  className
}) => {
  const [cronJobs, setCronJobs] = useState<CronJob[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("all");
  const [selectedCronJob, setSelectedCronJob] = useState<CronJob | null>(null);
  const [showDetails, setShowDetails] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize] = useState(20);
  const [total, setTotal] = useState(0);

  // 筛选和搜索CronJobs
  const filteredCronJobs = useMemo(() => {
    return cronJobs.filter(cj => {
      const matchesSearch = 
        cj.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        cj.namespace.toLowerCase().includes(searchTerm.toLowerCase()) ||
        cj.schedule.toLowerCase().includes(searchTerm.toLowerCase());

      const matchesStatus = statusFilter === "all" || cj.status.toLowerCase() === statusFilter.toLowerCase();

      return matchesSearch && matchesStatus;
    });
  }, [cronJobs, searchTerm, statusFilter]);

  // 统计信息
  const stats = useMemo(() => {
    return {
      total: cronJobs.length,
      active: cronJobs.filter(cj => cj.status === 'Active').length,
      suspended: cronJobs.filter(cj => cj.status === 'Suspended').length,
      failed: cronJobs.filter(cj => cj.status === 'Failed').length,
      runningJobs: cronJobs.reduce((sum, cj) => sum + cj.active, 0),
      avgSuccessRate: cronJobs.length > 0 
        ? Math.round(cronJobs.filter(cj => cj.recentJobs.some(job => job.status === 'Complete')).length / cronJobs.length * 100)
        : 0
    };
  }, [cronJobs]);

  // 获取CronJobs数据
  const fetchCronJobs = async () => {
    if (!clusterId) return;
    
    setLoading(true);
    setError(null);
    try {
      const response: K8sResourceListResponse = await KubernetesAPI.getCronJobs(
        clusterId,
        namespace || undefined,
        pageNum,
        pageSize
      );

      // 转换API响应为组件需要的CronJob格式
      const convertedCronJobs: CronJob[] = response.data.map((resource: K8sResource) => {
        const spec = resource.spec as any;
        const status = resource.metadata as any;
        
        return {
          name: resource.name,
          namespace: resource.namespace,
          schedule: spec?.schedule || '0 0 * * *',
          suspend: spec?.suspend || false,
          active: status?.active?.length || 0,
          lastScheduleTime: status?.lastScheduleTime,
          nextScheduleTime: calculateNextScheduleTime(spec?.schedule),
          successfulJobsHistoryLimit: spec?.successfulJobsHistoryLimit || 3,
          failedJobsHistoryLimit: spec?.failedJobsHistoryLimit || 1,
          concurrencyPolicy: spec?.concurrencyPolicy || 'Allow',
          startingDeadlineSeconds: spec?.startingDeadlineSeconds,
          age: resource.age || '-',
          creationTimestamp: resource.creationTimestamp,
          status: determineCronJobStatus(spec?.suspend, status?.active, status),
          lastSuccessfulTime: status?.lastSuccessfulTime,
          recentJobs: (status?.active || []).map((job: any) => ({
            name: job.name || 'unknown',
            status: job.status || 'Running',
            startTime: job.startTime || new Date().toISOString(),
            completionTime: job.completionTime
          }))
        };
      });

      setCronJobs(convertedCronJobs);
      setTotal(response.total || convertedCronJobs.length);
    } catch (error) {
      console.error('获取CronJobs失败:', error);
      setError(error instanceof Error ? error.message : '获取CronJobs失败');
      setCronJobs([]);
    } finally {
      setLoading(false);
    }
  };

  // 确定CronJob状态
  const determineCronJobStatus = (suspend: boolean, active: any[], status: any): 'Active' | 'Suspended' | 'Failed' | 'Pending' => {
    if (suspend) return 'Suspended';
    if (active && active.length > 0) return 'Active';
    // 这里可以根据更多状态信息来判断Failed状态
    return 'Pending';
  };

  // 计算下次调度时间（简化版本）
  const calculateNextScheduleTime = (schedule?: string): string => {
    if (!schedule) return '未知';
    
    // 这里应该使用cron表达式解析库来计算精确的下次执行时间
    // 简化实现，仅作示例
    const now = new Date();
    const nextRun = new Date(now.getTime() + 3600000); // 假设下次运行是1小时后
    return nextRun.toISOString();
  };

  // 格式化Cron表达式描述
  const describeCronSchedule = (schedule: string): string => {
    const descriptions: Record<string, string> = {
      '0 0 * * *': '每天午夜',
      '0 0 * * 0': '每周日午夜',
      '0 0 1 * *': '每月1日午夜',
      '0 */6 * * *': '每6小时',
      '*/30 * * * *': '每30分钟',
      '0 9 * * 1-5': '工作日上午9点'
    };
    
    return descriptions[schedule] || schedule;
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
  const getStatusDisplay = (status: string, suspend: boolean = false) => {
    if (suspend) {
      return { color: 'text-orange-600', bgColor: 'bg-orange-100', icon: Pause };
    }
    
    const displays = {
      'Active': { color: 'text-green-600', bgColor: 'bg-green-100', icon: CheckCircle },
      'Suspended': { color: 'text-orange-600', bgColor: 'bg-orange-100', icon: Pause },
      'Failed': { color: 'text-red-600', bgColor: 'bg-red-100', icon: XCircle },
      'Pending': { color: 'text-yellow-600', bgColor: 'bg-yellow-100', icon: Clock }
    };
    return displays[status as keyof typeof displays] || displays['Pending'];
  };

  // 格式化时间差
  const getTimeDiff = (dateString?: string): string => {
    if (!dateString) return '-';
    
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMinutes = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMinutes / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffDays > 0) return `${diffDays}天前`;
    if (diffHours > 0) return `${diffHours}小时前`;
    if (diffMinutes > 0) return `${diffMinutes}分钟前`;
    return '刚刚';
  };

  // 获取并发策略颜色
  const getConcurrencyPolicyColor = (policy: string) => {
    const colors: Record<string, string> = {
      'Allow': 'bg-green-100 text-green-700',
      'Forbid': 'bg-red-100 text-red-700',
      'Replace': 'bg-blue-100 text-blue-700'
    };
    return colors[policy] || 'bg-gray-100 text-gray-700';
  };

  // 刷新数据
  const handleRefresh = async () => {
    await fetchCronJobs();
  };

  // CronJob操作
  const handleCronJobAction = (action: string, cronJob: CronJob) => {
    console.log(`执行操作: ${action} on CronJob: ${cronJob.name}`);
    switch (action) {
      case 'view':
        setSelectedCronJob(cronJob);
        setShowDetails(true);
        break;
      case 'suspend':
        // 实现暂停逻辑
        break;
      case 'resume':
        // 实现恢复逻辑
        break;
      case 'trigger':
        // 实现立即触发逻辑
        break;
      case 'delete':
        // 实现删除逻辑
        break;
    }
  };

  // 组件挂载和依赖更新时获取数据
  useEffect(() => {
    fetchCronJobs();
  }, [clusterId, namespace, pageNum]);

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
      <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
        {[
          { 
            title: "总计", 
            count: stats.total, 
            color: "blue", 
            icon: Box,
            description: "CronJobs总数"
          },
          { 
            title: "活跃", 
            count: stats.active, 
            color: "green", 
            icon: CheckCircle,
            description: "正常运行"
          },
          { 
            title: "暂停", 
            count: stats.suspended, 
            color: "orange", 
            icon: Pause,
            description: "已暂停"
          },
          { 
            title: "运行中任务", 
            count: stats.runningJobs, 
            color: "purple", 
            icon: Activity,
            description: "当前执行中"
          },
          { 
            title: "成功率", 
            count: `${stats.avgSuccessRate}%`, 
            color: "indigo", 
            icon: Timer,
            description: "平均成功率",
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
              <CardTitle className="text-lg">CronJobs</CardTitle>
              <CardDescription>管理Kubernetes定时任务</CardDescription>
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
                <CalendarClock className="w-4 h-4 mr-2" />
                新建CronJob
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
                placeholder="搜索CronJobs..."
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
                <SelectItem value="active">活跃</SelectItem>
                <SelectItem value="suspended">暂停</SelectItem>
                <SelectItem value="failed">失败</SelectItem>
                <SelectItem value="pending">等待中</SelectItem>
              </SelectContent>
            </Select>
            <Button variant="outline" size="sm">
              <Download className="w-4 h-4 mr-2" />
              导出
            </Button>
          </div>

          {/* CronJobs表格 */}
          <div className="border rounded-lg">
            <Table>
              <TableHeader>
                <TableRow className="bg-gray-50">
                  <TableHead>名称</TableHead>
                  <TableHead>命名空间</TableHead>
                  <TableHead>调度</TableHead>
                  <TableHead>状态</TableHead>
                  <TableHead>活跃任务</TableHead>
                  <TableHead>上次调度</TableHead>
                  <TableHead>创建时间</TableHead>
                  <TableHead>操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <AnimatePresence>
                  {filteredCronJobs.map((cj, index) => {
                    const statusDisplay = getStatusDisplay(cj.status, cj.suspend);
                    return (
                      <motion.tr
                        key={cj.name}
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ delay: index * 0.05 }}
                        className="hover:bg-gray-50 transition-colors duration-200"
                      >
                        <TableCell>
                          <div className="flex items-center space-x-3">
                            <div className="w-8 h-8 bg-teal-100 rounded-lg flex items-center justify-center">
                              <CalendarClock className="w-4 h-4 text-teal-600" />
                            </div>
                            <div>
                              <div className="font-medium text-gray-900">{cj.name}</div>
                              <div className="text-xs text-gray-500">
                                并发策略: {cj.concurrencyPolicy}
                              </div>
                            </div>
                          </div>
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline" className="text-xs">
                            {cj.namespace}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <div className="space-y-1">
                            <div className="font-mono text-xs bg-gray-100 px-2 py-1 rounded">
                              {cj.schedule}
                            </div>
                            <div className="text-xs text-gray-500">
                              {describeCronSchedule(cj.schedule)}
                            </div>
                          </div>
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center space-x-2">
                            <statusDisplay.icon className={`w-4 h-4 ${statusDisplay.color}`} />
                            <span className="text-sm">
                              {cj.suspend ? '暂停' : cj.status}
                            </span>
                          </div>
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center space-x-2">
                            <Activity className="w-4 h-4 text-gray-400" />
                            <span className="text-sm">{cj.active}</span>
                          </div>
                        </TableCell>
                        <TableCell>
                          <div className="text-sm text-gray-600">
                            {getTimeDiff(cj.lastScheduleTime)}
                          </div>
                        </TableCell>
                        <TableCell>
                          <div className="text-sm text-gray-600">
                            <div>{getAge(cj.creationTimestamp)}</div>
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
                              <DropdownMenuItem onClick={() => handleCronJobAction('view', cj)}>
                                <Eye className="w-4 h-4 mr-2" />
                                查看详情
                              </DropdownMenuItem>
                              <DropdownMenuItem onClick={() => handleCronJobAction('trigger', cj)}>
                                <Play className="w-4 h-4 mr-2" />
                                立即执行
                              </DropdownMenuItem>
                              {!cj.suspend ? (
                                <DropdownMenuItem onClick={() => handleCronJobAction('suspend', cj)}>
                                  <Pause className="w-4 h-4 mr-2" />
                                  暂停
                                </DropdownMenuItem>
                              ) : (
                                <DropdownMenuItem onClick={() => handleCronJobAction('resume', cj)}>
                                  <Play className="w-4 h-4 mr-2" />
                                  恢复
                                </DropdownMenuItem>
                              )}
                              <DropdownMenuSeparator />
                              <DropdownMenuItem 
                                onClick={() => handleCronJobAction('delete', cj)}
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

          {filteredCronJobs.length === 0 && !loading && (
            <div className="text-center py-8">
              <CalendarClock className="w-12 h-12 text-gray-400 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">暂无CronJobs</h3>
              <p className="text-gray-500 mb-4">
                {searchTerm ? '没有找到匹配的CronJobs' : '当前命名空间中没有CronJobs'}
              </p>
              {!searchTerm && (
                <Button>
                  <CalendarClock className="w-4 h-4 mr-2" />
                  创建第一个CronJob
                </Button>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      {/* CronJob详情模态框 */}
      {showDetails && selectedCronJob && (
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
                  <h2 className="text-xl font-semibold">{selectedCronJob.name}</h2>
                  <p className="text-gray-600">CronJob详细信息</p>
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
                  <TabsTrigger value="schedule">调度配置</TabsTrigger>
                  <TabsTrigger value="jobs">任务历史</TabsTrigger>
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
                          <p className="text-sm">{selectedCronJob.name}</p>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">命名空间</label>
                          <p className="text-sm">{selectedCronJob.namespace}</p>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">状态</label>
                          <div className="flex items-center space-x-2">
                            {(() => {
                              const display = getStatusDisplay(selectedCronJob.status, selectedCronJob.suspend);
                              return (
                                <>
                                  <display.icon className={`w-4 h-4 ${display.color}`} />
                                  <span className="text-sm">
                                    {selectedCronJob.suspend ? '暂停' : selectedCronJob.status}
                                  </span>
                                </>
                              );
                            })()}
                          </div>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">活跃任务</label>
                          <p className="text-sm">{selectedCronJob.active}</p>
                        </div>
                      </CardContent>
                    </Card>
                    <Card>
                      <CardHeader>
                        <CardTitle className="text-sm">调度信息</CardTitle>
                      </CardHeader>
                      <CardContent className="space-y-3">
                        <div>
                          <label className="text-xs font-medium text-gray-500">Cron表达式</label>
                          <div className="font-mono text-sm bg-gray-100 px-2 py-1 rounded mt-1">
                            {selectedCronJob.schedule}
                          </div>
                          <p className="text-xs text-gray-500 mt-1">
                            {describeCronSchedule(selectedCronJob.schedule)}
                          </p>
                        </div>
                        <div className="grid grid-cols-2 gap-2">
                          <div>
                            <label className="text-xs font-medium text-gray-500">上次调度</label>
                            <p className="text-sm">
                              {selectedCronJob.lastScheduleTime 
                                ? getTimeDiff(selectedCronJob.lastScheduleTime)
                                : '从未执行'
                              }
                            </p>
                          </div>
                          <div>
                            <label className="text-xs font-medium text-gray-500">下次调度</label>
                            <p className="text-sm">
                              {selectedCronJob.suspend ? '已暂停' : '计算中...'}
                            </p>
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  </div>
                </TabsContent>
                <TabsContent value="schedule">
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-sm">调度配置</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="space-y-4">
                        <div className="grid grid-cols-2 gap-4">
                          <div>
                            <label className="text-xs font-medium text-gray-500">并发策略</label>
                            <Badge className={`text-xs mt-1 ${getConcurrencyPolicyColor(selectedCronJob.concurrencyPolicy)}`}>
                              {selectedCronJob.concurrencyPolicy}
                            </Badge>
                          </div>
                          <div>
                            <label className="text-xs font-medium text-gray-500">启动超时</label>
                            <p className="text-sm">
                              {selectedCronJob.startingDeadlineSeconds 
                                ? `${selectedCronJob.startingDeadlineSeconds}秒`
                                : '无限制'
                              }
                            </p>
                          </div>
                        </div>
                        <div className="grid grid-cols-2 gap-4">
                          <div>
                            <label className="text-xs font-medium text-gray-500">成功任务历史限制</label>
                            <p className="text-sm">{selectedCronJob.successfulJobsHistoryLimit}</p>
                          </div>
                          <div>
                            <label className="text-xs font-medium text-gray-500">失败任务历史限制</label>
                            <p className="text-sm">{selectedCronJob.failedJobsHistoryLimit}</p>
                          </div>
                        </div>
                        <div>
                          <label className="text-xs font-medium text-gray-500">暂停状态</label>
                          <div className="flex items-center space-x-2 mt-1">
                            {selectedCronJob.suspend ? (
                              <>
                                <Pause className="w-4 h-4 text-orange-500" />
                                <span className="text-sm text-orange-600">已暂停</span>
                              </>
                            ) : (
                              <>
                                <CheckCircle className="w-4 h-4 text-green-500" />
                                <span className="text-sm text-green-600">正常运行</span>
                              </>
                            )}
                          </div>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                </TabsContent>
                <TabsContent value="jobs">
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-sm">最近的任务</CardTitle>
                    </CardHeader>
                    <CardContent>
                      {selectedCronJob.recentJobs.length > 0 ? (
                        <div className="space-y-3">
                          {selectedCronJob.recentJobs.map((job, index) => (
                            <div key={index} className="p-3 border rounded-lg">
                              <div className="flex items-center justify-between">
                                <div className="flex items-center space-x-2">
                                  {job.status === 'Complete' ? (
                                    <CheckCircle className="w-4 h-4 text-green-500" />
                                  ) : job.status === 'Failed' ? (
                                    <XCircle className="w-4 h-4 text-red-500" />
                                  ) : (
                                    <Activity className="w-4 h-4 text-blue-500" />
                                  )}
                                  <span className="font-medium text-sm">{job.name}</span>
                                </div>
                                <Badge variant="outline" className={`text-xs ${
                                  job.status === 'Complete' ? 'border-green-200 text-green-700' :
                                  job.status === 'Failed' ? 'border-red-200 text-red-700' :
                                  'border-blue-200 text-blue-700'
                                }`}>
                                  {job.status}
                                </Badge>
                              </div>
                              <div className="mt-2 text-xs text-gray-500">
                                <div>开始时间: {new Date(job.startTime).toLocaleString()}</div>
                                {job.completionTime && (
                                  <div>完成时间: {new Date(job.completionTime).toLocaleString()}</div>
                                )}
                              </div>
                            </div>
                          ))}
                        </div>
                      ) : (
                        <p className="text-sm text-gray-500">暂无任务历史</p>
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

export default CronJobsDashboard;
