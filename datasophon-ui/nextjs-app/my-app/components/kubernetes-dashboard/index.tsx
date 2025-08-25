/**
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 * @description Kubernetes Dashboard主入口组件
 */

"use client";

import React, { useState, useEffect, useCallback } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  Activity,
  Server,
  Database,
  Settings,
  Monitor,
  Network,
  Shield,
  HardDrive,
  Clock,
  Grid3X3,
  RefreshCw,
  Search,
  Filter,
  MoreHorizontal,
  ChevronLeft,
  ChevronRight,
  Zap,
  Layers,
  Box,
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

import SidebarMenu from "./components/sidebar-menu";
import NamespaceSelector from "./components/namespace-selector";
import StatusIndicator from "./components/status-indicator";

// Dashboard面板导入
import PodsDashboard from './dashboards/pods-dashboard';
import ServicesDashboard from './dashboards/services-dashboard';
import DeploymentsDashboard from './dashboards/deployments-dashboard';

export interface KubernetesDashboardProps {
  clusterId: string;
  clusterName: string;
  className?: string;
}

type DashboardView = 
  | 'overview'
  | 'pods'
  | 'services'
  | 'deployments'
  | 'statefulsets'
  | 'daemonsets'
  | 'replicasets'
  | 'jobs'
  | 'cronjobs'
  | 'configmaps'
  | 'secrets'
  | 'persistentvolumes'
  | 'persistentvolumeclaims'
  | 'storageclasses'
  | 'ingresses'
  | 'ingressclasses';

interface MenuCategory {
  title: string;
  icon: React.ComponentType<{ className?: string }>;
  items: {
    key: DashboardView;
    label: string;
    icon: React.ComponentType<{ className?: string }>;
    description: string;
    badge?: string;
  }[];
}

const menuCategories: MenuCategory[] = [
  {
    title: "工作负载",
    icon: Activity,
    items: [
      { key: 'pods', label: 'Pods', icon: Box, description: '容器实例管理' },
      { key: 'deployments', label: 'Deployments', icon: Layers, description: '部署管理' },
      { key: 'statefulsets', label: 'StatefulSets', icon: Database, description: '有状态服务' },
      { key: 'daemonsets', label: 'DaemonSets', icon: Grid3X3, description: '守护进程' },
      { key: 'replicasets', label: 'ReplicaSets', icon: Server, description: '副本集' },
      { key: 'jobs', label: 'Jobs', icon: Zap, description: '任务管理' },
      { key: 'cronjobs', label: 'CronJobs', icon: Clock, description: '定时任务' },
    ]
  },
  {
    title: "网络服务",
    icon: Network,
    items: [
      { key: 'services', label: 'Services', icon: Network, description: '服务发现' },
      { key: 'ingresses', label: 'Ingresses', icon: Settings, description: '入口控制' },
      { key: 'ingressclasses', label: 'IngressClasses', icon: Shield, description: '入口类' },
    ]
  },
  {
    title: "存储配置",
    icon: HardDrive,
    items: [
      { key: 'configmaps', label: 'ConfigMaps', icon: Settings, description: '配置映射' },
      { key: 'secrets', label: 'Secrets', icon: Shield, description: '密钥管理' },
      { key: 'persistentvolumes', label: 'PersistentVolumes', icon: HardDrive, description: '持久卷' },
      { key: 'persistentvolumeclaims', label: 'PVCs', icon: Database, description: '存储声明' },
      { key: 'storageclasses', label: 'StorageClasses', icon: Monitor, description: '存储类' },
    ]
  }
];

const KubernetesDashboard: React.FC<KubernetesDashboardProps> = ({
  clusterId,
  clusterName,
  className
}) => {
  const [currentView, setCurrentView] = useState<DashboardView>('overview');
  const [selectedNamespace, setSelectedNamespace] = useState<string>('default');
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [lastRefresh, setLastRefresh] = useState(new Date());
  const [resourceStats, setResourceStats] = useState<any>(null);

  // 添加调试日志
  console.log('KubernetesDashboard 组件加载:', { clusterId, clusterName, selectedNamespace, currentView });

  // 获取资源统计数据
  const fetchResourceStats = useCallback(async () => {
    if (!clusterId) return;
    
    console.log('📊 开始获取资源统计数据:', { clusterId, namespace: selectedNamespace });
    
    try {
      // 动态导入API工具类
      const { KubernetesAPI } = await import('@/lib/kubernetes-api');
      const stats = await KubernetesAPI.getResourceStats(clusterId, undefined, selectedNamespace);
      console.log('✅ 获取资源统计成功:', stats);
      setResourceStats(stats);
    } catch (error) {
      console.error('❌ 获取资源统计失败:', error);
      // 设置默认的空数据而不是模拟数据
      setResourceStats({
        podCount: 0,
        serviceCount: 0,
        deploymentCount: 0,
        runningPodCount: 0
      });
    }
  }, [clusterId, selectedNamespace]);

  // 刷新数据
  const handleRefresh = async () => {
    setIsLoading(true);
    try {
      console.log('🔄 刷新Kubernetes Dashboard数据');
      await fetchResourceStats();
      setLastRefresh(new Date());
    } catch (error) {
      console.error('❌ 刷新失败:', error);
    } finally {
      setIsLoading(false);
    }
  };

  // 组件挂载时获取数据
  useEffect(() => {
    if (clusterId) {
      fetchResourceStats();
    }
  }, [clusterId, fetchResourceStats]);

  // 获取当前视图的标题和描述
  const getCurrentViewInfo = () => {
    if (currentView === 'overview') {
      return { title: '集群总览', description: '查看集群资源概览和状态' };
    }
    
    for (const category of menuCategories) {
      const item = category.items.find(item => item.key === currentView);
      if (item) {
        return { title: item.label, description: item.description };
      }
    }
    
    return { title: '未知视图', description: '' };
  };

  const viewInfo = getCurrentViewInfo();

  return (
    <div className={`flex h-screen bg-gray-50 ${className || ''}`}>
      {/* 侧边栏 */}
      <motion.div
        initial={false}
        animate={{ width: sidebarCollapsed ? 72 : 280 }}
        transition={{ duration: 0.3, ease: "easeInOut" }}
        className="bg-white border-r border-gray-200 shadow-sm flex-shrink-0"
      >
        {/* 侧边栏头部 */}
        <div className="p-4 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <motion.div
              initial={false}
              animate={{ opacity: sidebarCollapsed ? 0 : 1 }}
              transition={{ duration: 0.2 }}
              className="flex items-center space-x-3"
            >
              <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-blue-600 rounded-lg flex items-center justify-center">
                <Grid3X3 className="w-4 h-4 text-white" />
              </div>
              <div>
                <h1 className="text-sm font-semibold text-gray-900">Kubernetes</h1>
                <p className="text-xs text-gray-500 truncate max-w-[180px]">{clusterName}</p>
              </div>
            </motion.div>
            
            <Button
              variant="ghost"
              size="icon"
              className="w-8 h-8"
              onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
            >
              {sidebarCollapsed ? (
                <ChevronRight className="w-4 h-4" />
              ) : (
                <ChevronLeft className="w-4 h-4" />
              )}
            </Button>
          </div>
        </div>

        {/* 命名空间选择器 */}
        <div className="p-4 border-b border-gray-100">
          <NamespaceSelector
            clusterId={clusterId}
            value={selectedNamespace}
            onChange={setSelectedNamespace}
            collapsed={sidebarCollapsed}
          />
        </div>

        {/* 菜单项 */}
        <div className="flex-1 overflow-y-auto py-4">
          {/* 总览 */}
          <div className="px-4 mb-6">
            <motion.div
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              className={`flex items-center p-3 rounded-xl cursor-pointer transition-all duration-200 ${
                currentView === 'overview'
                  ? 'bg-gradient-to-r from-blue-50 to-blue-100 border-2 border-blue-200'
                  : 'hover:bg-gray-50 border-2 border-transparent'
              }`}
              onClick={() => setCurrentView('overview')}
            >
              <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${
                currentView === 'overview'
                  ? 'bg-gradient-to-br from-blue-500 to-blue-600 text-white'
                  : 'bg-gray-100 text-gray-600'
              }`}>
                <Monitor className="w-5 h-5" />
              </div>
              
              <AnimatePresence>
                {!sidebarCollapsed && (
                  <motion.div
                    initial={{ opacity: 0, x: -10 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: -10 }}
                    className="ml-3 flex-1"
                  >
                    <div className="text-sm font-medium text-gray-900">集群总览</div>
                    <div className="text-xs text-gray-500">资源概览和状态</div>
                  </motion.div>
                )}
              </AnimatePresence>
            </motion.div>
          </div>

          {/* 菜单分类 */}
          {menuCategories.map((category, categoryIndex) => (
            <div key={category.title} className="px-4 mb-6">
              <AnimatePresence>
                {!sidebarCollapsed && (
                  <motion.div
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    className="flex items-center mb-3"
                  >
                    <category.icon className="w-4 h-4 text-gray-400 mr-2" />
                    <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">
                      {category.title}
                    </span>
                  </motion.div>
                )}
              </AnimatePresence>

              <div className="space-y-1">
                {category.items.map((item, itemIndex) => (
                  <motion.div
                    key={item.key}
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                    className={`flex items-center p-2.5 rounded-lg cursor-pointer transition-all duration-200 ${
                      currentView === item.key
                        ? 'bg-gradient-to-r from-blue-50 to-blue-100 border border-blue-200'
                        : 'hover:bg-gray-50 border border-transparent'
                    }`}
                    onClick={() => setCurrentView(item.key)}
                  >
                    <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${
                      currentView === item.key
                        ? 'bg-gradient-to-br from-blue-500 to-blue-600 text-white'
                        : 'bg-gray-100 text-gray-600'
                    }`}>
                      <item.icon className="w-4 h-4" />
                    </div>
                    
                    <AnimatePresence>
                      {!sidebarCollapsed && (
                        <motion.div
                          initial={{ opacity: 0, x: -10 }}
                          animate={{ opacity: 1, x: 0 }}
                          exit={{ opacity: 0, x: -10 }}
                          className="ml-3 flex-1 min-w-0"
                        >
                          <div className="flex items-center justify-between">
                            <span className="text-sm font-medium text-gray-900 truncate">
                              {item.label}
                            </span>
                            {item.badge && (
                              <Badge variant="secondary" className="text-xs">
                                {item.badge}
                              </Badge>
                            )}
                          </div>
                          <div className="text-xs text-gray-500 truncate">
                            {item.description}
                          </div>
                        </motion.div>
                      )}
                    </AnimatePresence>
                  </motion.div>
                ))}
              </div>
            </div>
          ))}
        </div>
      </motion.div>

      {/* 主内容区域 */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* 顶部工具栏 */}
        <div className="bg-white border-b border-gray-200 px-6 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              <div>
                <h1 className="text-xl font-semibold text-gray-900 flex items-center">
                  {viewInfo.title}
                  <StatusIndicator 
                    status="healthy" 
                    className="ml-3"
                  />
                </h1>
                <p className="text-sm text-gray-500 mt-1">{viewInfo.description}</p>
              </div>
            </div>

            <div className="flex items-center space-x-3">
              {/* 搜索框 */}
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
                <Input
                  placeholder="搜索资源..."
                  className="pl-10 w-64"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>

              {/* 过滤器 */}
              <Button variant="outline" size="icon">
                <Filter className="w-4 h-4" />
              </Button>

              {/* 刷新按钮 */}
              <Button
                variant="outline"
                size="icon"
                onClick={handleRefresh}
                disabled={isLoading}
              >
                <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
              </Button>

              {/* 更多操作 */}
              <Button variant="outline" size="icon">
                <MoreHorizontal className="w-4 h-4" />
              </Button>
            </div>
          </div>

          {/* 面包屑和状态信息 */}
          <div className="flex items-center justify-between mt-4 pt-4 border-t border-gray-100">
            <div className="flex items-center text-sm text-gray-500 space-x-2">
              <span>命名空间:</span>
              <Badge variant="outline" className="font-mono">
                {selectedNamespace}
              </Badge>
              <span className="mx-2">•</span>
              <span>最后更新:</span>
              <span className="font-mono">
                {lastRefresh.toLocaleTimeString()}
              </span>
            </div>
          </div>
        </div>

        {/* 主要内容区域 */}
        <div className="flex-1 overflow-auto bg-gray-50 p-6">
          <AnimatePresence mode="wait">
            <motion.div
              key={currentView}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              transition={{ duration: 0.2 }}
              className="h-full"
            >
              {currentView === 'overview' && (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
                  {/* 资源概览卡片 - 使用真实API数据 */}
                  {[
                    { 
                      title: 'Pods', 
                      count: resourceStats ? String(resourceStats.podCount || 0) : (isLoading ? '...' : '0'), 
                      status: resourceStats && resourceStats.podCount > 0 && resourceStats.runningPodCount === resourceStats.podCount ? 'healthy' : 
                              resourceStats && resourceStats.podCount > 0 ? 'warning' : 'healthy', 
                      icon: Box 
                    },
                    { 
                      title: 'Services', 
                      count: resourceStats ? String(resourceStats.serviceCount || 0) : (isLoading ? '...' : '0'), 
                      status: 'healthy', 
                      icon: Network 
                    },
                    { 
                      title: 'Deployments', 
                      count: resourceStats ? String(resourceStats.deploymentCount || 0) : (isLoading ? '...' : '0'), 
                      status: 'healthy', 
                      icon: Layers 
                    },
                    { 
                      title: 'ConfigMaps', 
                      count: resourceStats ? String(resourceStats.configMapCount || 0) : (isLoading ? '...' : '0'), 
                      status: 'healthy', 
                      icon: Server 
                    },
                  ].map((item, index) => (
                    <motion.div
                      key={item.title}
                      initial={{ opacity: 0, y: 20 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: index * 0.1 }}
                      className="bg-white p-6 rounded-xl shadow-sm border border-gray-200 hover:shadow-md transition-shadow duration-200"
                    >
                      <div className="flex items-center justify-between">
                        <div>
                          <p className="text-sm text-gray-600">{item.title}</p>
                          <p className="text-2xl font-bold text-gray-900 mt-1">{item.count}</p>
                        </div>
                        <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${
                          item.status === 'healthy' 
                            ? 'bg-green-100 text-green-600'
                            : item.status === 'warning'
                            ? 'bg-yellow-100 text-yellow-600'
                            : 'bg-red-100 text-red-600'
                        }`}>
                          <item.icon className="w-6 h-6" />
                        </div>
                      </div>
                      <div className="mt-4 flex items-center">
                        <StatusIndicator status={item.status as any} size="sm" />
                        <span className="text-xs text-gray-500 ml-2">
                          {item.status === 'healthy' ? '运行正常' : '需要注意'}
                        </span>
                      </div>
                    </motion.div>
                  ))}
                </div>
              )}

              {/* 具体的Dashboard组件 */}
              {currentView === 'pods' && (
                <PodsDashboard 
                  clusterId={clusterId} 
                  namespace={selectedNamespace} 
                />
              )}
              
              {currentView === 'services' && (
                <ServicesDashboard 
                  clusterId={clusterId} 
                  namespace={selectedNamespace} 
                />
              )}
              
              {currentView === 'deployments' && (
                <DeploymentsDashboard 
                  clusterId={clusterId} 
                  namespace={selectedNamespace} 
                />
              )}

              {/* 其他视图的占位符 */}
              {!['overview', 'pods', 'services', 'deployments'].includes(currentView) && (
                <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8 text-center">
                  <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
                    <Settings className="w-8 h-8 text-gray-400" />
                  </div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-2">
                    {viewInfo.title} Dashboard
                  </h3>
                  <p className="text-gray-500 mb-4">
                    {viewInfo.description} - 功能正在开发中
                  </p>
                  <div className="text-sm text-gray-400">
                    当前命名空间: <code className="bg-gray-100 px-2 py-1 rounded">{selectedNamespace}</code>
                  </div>
                </div>
              )}
            </motion.div>
          </AnimatePresence>
        </div>
      </div>
    </div>
  );
};

export default KubernetesDashboard;
