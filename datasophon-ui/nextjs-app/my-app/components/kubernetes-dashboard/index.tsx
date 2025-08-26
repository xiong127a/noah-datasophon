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
  ChevronLeft,
  ChevronRight,
  Zap,
  Layers,
  Box,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";

import NamespaceSelector from "./components/namespace-selector";
import StatusIndicator from "./components/status-indicator";

// Dashboard面板导入
import PodsDashboard from './dashboards/pods-dashboard';
import ServicesDashboard from './dashboards/services-dashboard';
import DeploymentsDashboard from './dashboards/deployments-dashboard';
import StatefulSetsDashboard from './dashboards/statefulsets-dashboard';
import DaemonSetsDashboard from './dashboards/daemonsets-dashboard';
import ReplicaSetsDashboard from './dashboards/replicasets-dashboard';
import JobsDashboard from './dashboards/jobs-dashboard';
import CronJobsDashboard from './dashboards/cronjobs-dashboard';
import IngressesDashboard from './dashboards/ingresses-dashboard';
import IngressClassesDashboard from './dashboards/ingressclasses-dashboard';
import EndpointsDashboard from './dashboards/endpoints-dashboard';
import ConfigMapsDashboard from './dashboards/configmaps-dashboard';
import SecretsDashboard from './dashboards/secrets-dashboard';
import PersistentVolumesDashboard from './dashboards/persistentvolumes-dashboard';
import PersistentVolumeClaimsDashboard from './dashboards/persistentvolumeclaims-dashboard';
import StorageClassesDashboard from './dashboards/storageclasses-dashboard';

// 导入类型定义
import type { K8sResourceStats } from '@/lib/kubernetes-api';

export interface KubernetesDashboardProps {
  clusterId: string;
  serviceId: string;
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
  | 'ingressclasses'
  | 'endpoints';

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
    title: "仪表盘",
    icon: Monitor,
    items: [
      { key: 'overview', label: '集群总览', icon: Monitor, description: '集群整体状态和资源分布' },
    ]
  },
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
      { key: 'endpoints', label: 'Endpoints', icon: Zap, description: '服务端点' },
    ]
  },
  {
    title: "存储配置",
    icon: HardDrive,
    items: [
      { key: 'configmaps', label: 'ConfigMaps', icon: Settings, description: '配置映射' },
      { key: 'secrets', label: 'Secrets', icon: Shield, description: '密钥管理' },
      { key: 'persistentvolumes', label: 'PersistentVolumes', icon: HardDrive, description: '持久卷' },
      { key: 'persistentvolumeclaims', label: 'PersistentVolumeClaims', icon: Database, description: '存储声明' },
      { key: 'storageclasses', label: 'StorageClasses', icon: Monitor, description: '存储类' },
    ]
  }
];

const KubernetesDashboard: React.FC<KubernetesDashboardProps> = ({
  clusterId,
  serviceId,
  clusterName,
  className
}) => {
  const [currentView, setCurrentView] = useState<DashboardView>('overview');
  const [selectedNamespace, setSelectedNamespace] = useState<string>('default');
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [resourceStats, setResourceStats] = useState<K8sResourceStats | null>(null);

  // 添加调试日志
  

  // 获取资源统计数据
  const fetchResourceStats = useCallback(async () => {
    if (!clusterId) return;
    

    
    try {
      // 动态导入API工具类
      const { KubernetesAPI } = await import('@/lib/kubernetes-api');
      // 集群总览应该显示所有命名空间的资源统计，而不是特定命名空间
      const stats = await KubernetesAPI.getResourceStats(clusterId, undefined, 'all');

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
  }, [clusterId]); // 集群总览不依赖命名空间，显示全局统计



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
    <div className={`flex bg-gray-50 ${className || ''}`} style={{ minHeight: '100vh' }}>
      {/* 侧边栏 - 自适应高度，与右侧内容区对齐 */}
      <motion.div
        initial={false}
        animate={{ width: sidebarCollapsed ? 72 : 280 }}
        transition={{ duration: 0.3, ease: "easeInOut" }}
        className="bg-white border-r border-gray-200 shadow-sm flex-shrink-0 flex flex-col"
        style={{ minHeight: '100vh' }}
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

        {/* 菜单项 - 完全连续的布局，填满剩余高度 */}
        <div className="flex-1 flex flex-col min-h-0">
          <div className="flex-1 px-4 py-4">
            {menuCategories.map((category) => (
              <div key={category.title} className="mb-6">
                {!sidebarCollapsed && (
                  <div className="flex items-center mb-3">
                    <category.icon className="w-4 h-4 text-gray-400 mr-2" />
                    <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">
                      {category.title}
                    </span>
                  </div>
                )}

                <div className="space-y-2">
                  {category.items.map((item) => (
                    <div
                      key={item.key}
                      className={`flex items-center px-3 py-2 rounded-lg cursor-pointer transition-colors ${
                        currentView === item.key
                          ? 'bg-blue-50 border border-blue-200'
                          : 'hover:bg-gray-50'
                      }`}
                      onClick={() => setCurrentView(item.key)}
                      title={sidebarCollapsed ? `${item.label} - ${item.description}` : undefined}
                    >
                      <div className={`w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0 ${
                        currentView === item.key
                          ? 'bg-blue-500 text-white'
                          : 'bg-gray-100 text-gray-600'
                      }`}>
                        <item.icon className="w-4 h-4" />
                      </div>
                      
                      {!sidebarCollapsed && (
                        <div className="ml-3 flex-1 min-w-0">
                          <div className="flex items-center justify-between">
                            <span className="text-sm font-medium text-gray-900 truncate">
                              {item.label}
                            </span>
                            {item.badge && (
                              <Badge variant="secondary" className="text-xs ml-2 flex-shrink-0">
                                {item.badge}
                              </Badge>
                            )}
                          </div>
                          <div className="text-xs text-gray-500 truncate">
                            {item.description}
                          </div>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            ))}
            

          </div>
          
          {/* 底部填充区域 - 确保高度延伸到与右侧内容区平齐 */}
          <div className="flex-1 min-h-[200px] bg-white"></div>
        </div>
      </motion.div>

      {/* 主内容区域 - 支持页面整体滚动 */}
      <div className="flex-1 flex flex-col">
        {/* 美化的顶部区域 */}
        <div className="bg-gradient-to-r from-white to-gray-50 border-b border-gray-200 px-8 py-6">
          <div className="max-w-7xl mx-auto">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-4">
                <div className="flex items-center space-x-3">
                  <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-blue-600 rounded-xl flex items-center justify-center shadow-md">
                    <StatusIndicator 
                      status="healthy" 
                      className="w-5 h-5 text-white"
                    />
                  </div>
                  <div>
                    <h1 className="text-2xl font-bold text-gray-900 tracking-tight">
                      {viewInfo.title}
                    </h1>
                    <p className="text-sm text-gray-600 mt-0.5">{viewInfo.description}</p>
                  </div>
                </div>
              </div>

              {/* 集群总览模式显示美化的命名空间选择器 */}
              {currentView === 'overview' && (
                <div className="flex items-center space-x-3">
                  <span className="text-sm font-medium text-gray-500">命名空间</span>
                  <NamespaceSelector
                    clusterId={clusterId}
                    value={selectedNamespace}
                    onChange={setSelectedNamespace}
                    className="min-w-[200px]"
                  />
                </div>
              )}
            </div>
          </div>
        </div>

        {/* 美化的主要内容区域 */}
        <div className="flex-1 bg-gradient-to-br from-gray-50 via-white to-gray-50">
          <div className="max-w-7xl mx-auto p-6">
            <AnimatePresence mode="wait">
              <motion.div
                key={currentView}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -10 }}
                transition={{ duration: 0.3, ease: "easeOut" }}
                className="min-h-full"
              >
              {currentView === 'overview' && (
                <div className="space-y-8">
                  {/* 集群概览标题 */}
                  <div className="flex items-center justify-between">
                    <div>
                      <h1 className="text-3xl font-bold text-gray-900">集群概览</h1>
                      <p className="text-gray-600 mt-2">查看 {clusterName} 集群的整体状态和资源分布</p>
                    </div>
                    <div className="flex items-center space-x-3">
                      <Badge variant="outline" className="px-3 py-1 text-sm">
                        <div className="w-2 h-2 bg-green-500 rounded-full mr-2"></div>
                        集群运行中
                      </Badge>
                    </div>
                  </div>

                  {/* 核心资源统计 */}
                  <div>
                    <h2 className="text-xl font-semibold text-gray-900 mb-2">核心资源</h2>
                    <p className="text-sm text-gray-500 mb-4">点击卡片可以快速导航到对应的资源管理页面</p>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                      {[
                        { 
                          title: 'Pods', 
                          count: resourceStats ? String(resourceStats.podCount || 0) : '...', 
                          subCount: resourceStats ? `运行中: ${resourceStats.runningPodCount || 0}` : '',
                          status: resourceStats && resourceStats.podCount && resourceStats.podCount > 0 && resourceStats.runningPodCount === resourceStats.podCount ? 'healthy' : 
                                  resourceStats && resourceStats.podCount && resourceStats.podCount > 0 ? 'warning' : 'healthy', 
                          icon: Box,
                          color: 'blue'
                        },
                        { 
                          title: 'Services', 
                          count: resourceStats ? String(resourceStats.serviceCount || 0) : '...', 
                          subCount: resourceStats ? `ClusterIP: ${resourceStats.clusterIpServiceCount || 0}` : '',
                          status: 'healthy', 
                          icon: Network,
                          color: 'green'
                        },
                        { 
                          title: 'Deployments', 
                          count: resourceStats ? String(resourceStats.deploymentCount || 0) : '...', 
                          subCount: resourceStats ? `可用: ${resourceStats.availableDeploymentCount || 0}` : '',
                          status: 'healthy', 
                          icon: Layers,
                          color: 'purple'
                        },
                        { 
                          title: 'StatefulSets', 
                          count: resourceStats ? String(resourceStats.statefulSetCount || 0) : '...', 
                          subCount: resourceStats ? `就绪: ${resourceStats.readyStatefulSetCount || 0}` : '',
                          status: 'healthy', 
                          icon: Database,
                          color: 'orange'
                        },
                      ].map((item, index) => (
                        <motion.div
                          key={item.title}
                          initial={{ opacity: 0, y: 20 }}
                          animate={{ opacity: 1, y: 0 }}
                          transition={{ delay: index * 0.1 }}
                          className="bg-white p-6 rounded-xl shadow-sm border border-gray-200 hover:shadow-lg transition-all duration-200 hover:scale-105 cursor-pointer"
                          onClick={() => {
                            const viewMap: Record<string, DashboardView> = {
                              'Pods': 'pods',
                              'Services': 'services', 
                              'Deployments': 'deployments',
                              'StatefulSets': 'statefulsets'
                            };
                            const targetView = viewMap[item.title];
                            if (targetView) {
                              setCurrentView(targetView);
                            }
                          }}
                        >
                          <div className="flex items-center justify-between mb-4">
                            <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${
                              item.color === 'blue' ? 'bg-blue-100 text-blue-600' :
                              item.color === 'green' ? 'bg-green-100 text-green-600' :
                              item.color === 'purple' ? 'bg-purple-100 text-purple-600' :
                              'bg-orange-100 text-orange-600'
                            }`}>
                              <item.icon className="w-6 h-6" />
                            </div>
                            <StatusIndicator status={item.status as 'healthy' | 'warning' | 'error'} size="sm" />
                          </div>
                          <div>
                            <p className="text-sm font-medium text-gray-600 mb-1">{item.title}</p>
                            <p className="text-3xl font-bold text-gray-900">{item.count}</p>
                            {item.subCount && (
                              <p className="text-sm text-gray-500 mt-2">{item.subCount}</p>
                            )}
                          </div>
                        </motion.div>
                      ))}
                    </div>
                  </div>

                  {/* 详细资源统计 */}
                  <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                    {/* 工作负载状态 */}
                    <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
                      <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
                        <Activity className="w-5 h-5 mr-2 text-blue-600" />
                        工作负载状态
                      </h3>
                      <div className="space-y-4">
                        {resourceStats && [
                          { label: 'Pods', total: resourceStats.podCount || 0, running: resourceStats.runningPodCount || 0, failed: resourceStats.failedPodCount || 0 },
                          { label: 'Deployments', total: resourceStats.deploymentCount || 0, running: resourceStats.availableDeploymentCount || 0, failed: (resourceStats.deploymentCount || 0) - (resourceStats.availableDeploymentCount || 0) },
                          { label: 'StatefulSets', total: resourceStats.statefulSetCount || 0, running: resourceStats.readyStatefulSetCount || 0, failed: (resourceStats.statefulSetCount || 0) - (resourceStats.readyStatefulSetCount || 0) },
                          { label: 'DaemonSets', total: resourceStats.daemonSetCount || 0, running: resourceStats.readyDaemonSetCount || 0, failed: (resourceStats.daemonSetCount || 0) - (resourceStats.readyDaemonSetCount || 0) },
                        ].map((item) => (
                          <div key={item.label} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                            <span className="font-medium text-gray-700">{item.label}</span>
                            <div className="flex items-center space-x-4">
                              <div className="flex items-center space-x-2">
                                <div className="w-3 h-3 bg-green-500 rounded-full"></div>
                                <span className="text-sm text-gray-600">{item.running}</span>
                              </div>
                              {item.failed > 0 && (
                                <div className="flex items-center space-x-2">
                                  <div className="w-3 h-3 bg-red-500 rounded-full"></div>
                                  <span className="text-sm text-gray-600">{item.failed}</span>
                                </div>
                              )}
                              <span className="text-sm font-medium text-gray-900">总计: {item.total}</span>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>

                    {/* 存储与配置 */}
                    <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
                      <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
                        <HardDrive className="w-5 h-5 mr-2 text-green-600" />
                        存储与配置
                      </h3>
                      <div className="grid grid-cols-2 gap-4">
                        {resourceStats && [
                          { label: 'ConfigMaps', count: resourceStats.configMapCount || 0, icon: Settings },
                          { label: 'Secrets', count: resourceStats.secretCount || 0, icon: Shield },
                          { label: 'PersistentVolumes', count: resourceStats.persistentVolumeCount || 0, icon: HardDrive },
                          { label: 'StorageClasses', count: resourceStats.storageClassCount || 0, icon: Monitor },
                        ].map((item) => (
                          <div key={item.label} className="text-center p-4 bg-gray-50 rounded-lg">
                            <item.icon className="w-8 h-8 mx-auto mb-2 text-gray-600" />
                            <p className="text-2xl font-bold text-gray-900">{item.count}</p>
                            <p className="text-sm text-gray-600">{item.label}</p>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>

                  {/* 网络服务概览 */}
                  <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
                    <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
                      <Network className="w-5 h-5 mr-2 text-purple-600" />
                      网络服务
                    </h3>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                      {resourceStats && [
                        { 
                          label: 'Services', 
                          total: resourceStats.serviceCount || 0,
                          details: [
                            { type: 'ClusterIP', count: resourceStats.clusterIpServiceCount || 0 },
                            { type: 'NodePort', count: resourceStats.nodePortServiceCount || 0 },
                            { type: 'LoadBalancer', count: resourceStats.loadBalancerServiceCount || 0 },
                          ]
                        },
                        { 
                          label: 'Ingresses', 
                          total: resourceStats.ingressCount || 0,
                          details: [
                            { type: 'Classes', count: resourceStats.ingressClassCount || 0 },
                          ]
                        },
                        { 
                          label: 'Jobs', 
                          total: resourceStats.jobCount || 0,
                          details: [
                            { type: 'Active', count: resourceStats.activeJobCount || 0 },
                            { type: 'Completed', count: resourceStats.completedJobCount || 0 },
                            { type: 'CronJobs', count: resourceStats.cronJobCount || 0 },
                          ]
                        },
                      ].map((category) => (
                        <div key={category.label} className="p-4 bg-gray-50 rounded-lg">
                          <div className="text-center mb-3">
                            <p className="text-2xl font-bold text-gray-900">{category.total}</p>
                            <p className="text-sm font-medium text-gray-600">{category.label}</p>
                          </div>
                          <div className="space-y-2">
                            {category.details.map((detail, idx) => (
                              <div key={idx} className="flex justify-between text-sm">
                                <span className="text-gray-600">{detail.type}</span>
                                <span className="font-medium text-gray-900">{detail.count}</span>
                              </div>
                            ))}
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>

                  {/* 集群健康状态 */}
                  <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
                    <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
                      <Monitor className="w-5 h-5 mr-2 text-green-600" />
                      集群健康状态
                    </h3>
                    <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                      {[
                        { 
                          label: 'Pod健康率', 
                          value: resourceStats && resourceStats.podCount && resourceStats.podCount > 0 
                            ? `${Math.round((resourceStats.runningPodCount || 0) / resourceStats.podCount * 100)}%` 
                            : '100%',
                          percentage: resourceStats && resourceStats.podCount && resourceStats.podCount > 0 
                            ? (resourceStats.runningPodCount || 0) / resourceStats.podCount * 100 
                            : 100,
                          status: resourceStats && resourceStats.podCount && resourceStats.podCount > 0 && (resourceStats.runningPodCount || 0) / resourceStats.podCount >= 0.8 ? 'healthy' : 'warning'
                        },
                        { 
                          label: 'Deployment可用率', 
                          value: resourceStats && resourceStats.deploymentCount && resourceStats.deploymentCount > 0 
                            ? `${Math.round((resourceStats.availableDeploymentCount || 0) / resourceStats.deploymentCount * 100)}%` 
                            : '100%',
                          percentage: resourceStats && resourceStats.deploymentCount && resourceStats.deploymentCount > 0 
                            ? (resourceStats.availableDeploymentCount || 0) / resourceStats.deploymentCount * 100 
                            : 100,
                          status: 'healthy'
                        },
                        { 
                          label: '存储使用', 
                          value: resourceStats ? `${resourceStats.persistentVolumeClaimCount || 0}/${resourceStats.persistentVolumeCount || 0}` : '0/0',
                          percentage: resourceStats && resourceStats.persistentVolumeCount && resourceStats.persistentVolumeCount > 0 
                            ? (resourceStats.persistentVolumeClaimCount || 0) / resourceStats.persistentVolumeCount * 100 
                            : 0,
                          status: 'healthy'
                        },
                        { 
                          label: '网络服务', 
                          value: resourceStats ? String(resourceStats.serviceCount || 0) : '0',
                          percentage: 100, // 网络服务显示为100%
                          status: 'healthy'
                        },
                      ].map((metric) => (
                        <div key={metric.label} className="p-4 bg-gray-50 rounded-lg">
                          <div className="text-center mb-3">
                            <p className="text-2xl font-bold text-gray-900 mb-1">{metric.value}</p>
                            <p className="text-sm text-gray-600">{metric.label}</p>
                          </div>
                          
                          {/* 进度条 */}
                          <div className="relative mb-2">
                            <div className="w-full bg-gray-200 rounded-full h-2">
                              <motion.div 
                                className={`h-2 rounded-full ${
                                  metric.status === 'healthy' ? 'bg-green-500' :
                                  metric.status === 'warning' ? 'bg-yellow-500' : 'bg-red-500'
                                }`}
                                initial={{ width: 0 }}
                                animate={{ width: `${Math.min(metric.percentage, 100)}%` }}
                                transition={{ duration: 1, delay: 0.2 }}
                              />
                            </div>
                          </div>
                          
                          <div className="flex justify-center">
                            <StatusIndicator status={metric.status as 'healthy' | 'warning' | 'error'} size="sm" />
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              )}

              {/* 具体的Dashboard组件 */}
              {currentView === 'pods' && (
                <PodsDashboard 
                  clusterId={clusterId}
                  serviceId={serviceId}
                  namespace={selectedNamespace} 
                />
              )}
              
              {currentView === 'services' && (
                <ServicesDashboard 
                  clusterId={clusterId}
                  serviceId={serviceId}
                  namespace={selectedNamespace} 
                />
              )}
              
              {currentView === 'deployments' && (
                <DeploymentsDashboard 
                  clusterId={clusterId}
                  serviceId={serviceId}
                  namespace={selectedNamespace} 
                />
              )}

              {currentView === 'statefulsets' && (
                <StatefulSetsDashboard 
                  clusterId={clusterId}
                  serviceId={serviceId}
                  namespace={selectedNamespace} 
                />
              )}

              {currentView === 'daemonsets' && (
                <DaemonSetsDashboard 
                  clusterId={clusterId}
                  serviceId={serviceId}
                  namespace={selectedNamespace} 
                />
              )}

              {currentView === 'replicasets' && (
                <ReplicaSetsDashboard 
                  clusterId={clusterId}
                  serviceId={serviceId}
                  namespace={selectedNamespace} 
                />
              )}

              {currentView === 'jobs' && (
                <JobsDashboard 
                  clusterId={clusterId}
                  serviceId={serviceId}
                  namespace={selectedNamespace} 
                />
              )}

              {currentView === 'cronjobs' && (
                <CronJobsDashboard 
                  clusterId={clusterId}
                  serviceId={serviceId}
                  namespace={selectedNamespace} 
                />
              )}

              {currentView === 'ingresses' && (
                <IngressesDashboard 
                  clusterId={clusterId}
                  serviceId={serviceId}
                  namespace={selectedNamespace} 
                />
              )}

              {currentView === 'ingressclasses' && (
                <IngressClassesDashboard 
                  clusterId={clusterId}
                  serviceId={serviceId}
                  namespace={selectedNamespace} 
                />
              )}

              {currentView === 'endpoints' && (
                <EndpointsDashboard 
                  clusterId={clusterId}
                  serviceId={serviceId}
                  namespace={selectedNamespace} 
                />
              )}

              {/* 存储配置Dashboard */}
              {currentView === 'configmaps' && (
                <ConfigMapsDashboard 
                  clusterId={clusterId}
                  namespace={selectedNamespace} 
                />
              )}

              {currentView === 'secrets' && (
                <SecretsDashboard 
                  clusterId={clusterId}
                  namespace={selectedNamespace} 
                />
              )}

              {currentView === 'persistentvolumes' && (
                <PersistentVolumesDashboard 
                  clusterId={clusterId}
                  namespace={selectedNamespace} 
                />
              )}

              {currentView === 'persistentvolumeclaims' && (
                <PersistentVolumeClaimsDashboard 
                  clusterId={clusterId}
                  namespace={selectedNamespace} 
                />
              )}

              {currentView === 'storageclasses' && (
                <StorageClassesDashboard 
                  clusterId={clusterId}
                  namespace={selectedNamespace} 
                />
              )}

              {/* 其他视图的占位符 */}
              {!['overview', 'pods', 'services', 'deployments', 'statefulsets', 'daemonsets', 'replicasets', 'jobs', 'cronjobs', 'ingresses', 'ingressclasses', 'endpoints', 'configmaps', 'secrets', 'persistentvolumes', 'persistentvolumeclaims', 'storageclasses'].includes(currentView) && (
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
    </div>
  );
};

export default KubernetesDashboard;
