/**
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 * @description Kubernetes Dashboard侧边栏菜单组件
 */

"use client";

import React, { useState } from "react";
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
  ChevronDown,
  Zap,
  Layers,
  Box,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

export type DashboardView = 
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
    count?: number;
  }[];
  collapsed?: boolean;
}

interface SidebarMenuProps {
  currentView: DashboardView;
  onViewChange: (view: DashboardView) => void;
  collapsed?: boolean;
  onToggleCollapse?: () => void;
  className?: string;
}

const defaultCategories: MenuCategory[] = [
  {
    title: "工作负载",
    icon: Activity,
    items: [
      { 
        key: 'pods', 
        label: 'Pods', 
        icon: Box, 
        description: '容器实例管理',
        count: 24
      },
      { 
        key: 'deployments', 
        label: 'Deployments', 
        icon: Layers, 
        description: '部署管理',
        count: 12
      },
      { 
        key: 'statefulsets', 
        label: 'StatefulSets', 
        icon: Database, 
        description: '有状态服务',
        count: 5
      },
      { 
        key: 'daemonsets', 
        label: 'DaemonSets', 
        icon: Grid3X3, 
        description: '守护进程',
        count: 3
      },
      { 
        key: 'replicasets', 
        label: 'ReplicaSets', 
        icon: Server, 
        description: '副本集',
        count: 15
      },
      { 
        key: 'jobs', 
        label: 'Jobs', 
        icon: Zap, 
        description: '任务管理',
        count: 7
      },
      { 
        key: 'cronjobs', 
        label: 'CronJobs', 
        icon: Clock, 
        description: '定时任务',
        count: 4
      },
    ]
  },
  {
    title: "网络服务",
    icon: Network,
    items: [
      { 
        key: 'services', 
        label: 'Services', 
        icon: Network, 
        description: '服务发现',
        count: 8
      },
      { 
        key: 'ingresses', 
        label: 'Ingresses', 
        icon: Settings, 
        description: '入口控制',
        count: 3
      },
      { 
        key: 'ingressclasses', 
        label: 'IngressClasses', 
        icon: Shield, 
        description: '入口类',
        count: 2
      },
      { 
        key: 'endpoints', 
        label: 'Endpoints', 
        icon: Zap, 
        description: '服务端点',
        count: 5
      },
    ]
  },
  {
    title: "存储配置",
    icon: HardDrive,
    items: [
      { 
        key: 'configmaps', 
        label: 'ConfigMaps', 
        icon: Settings, 
        description: '配置映射',
        count: 15
      },
      { 
        key: 'secrets', 
        label: 'Secrets', 
        icon: Shield, 
        description: '密钥管理',
        count: 8
      },
      { 
        key: 'persistentvolumes', 
        label: 'PersistentVolumes', 
        icon: HardDrive, 
        description: '持久卷',
        count: 6
      },
      { 
        key: 'persistentvolumeclaims', 
        label: 'PersistentVolumeClaims', 
        icon: Database, 
        description: '存储声明',
        count: 12
      },
      { 
        key: 'storageclasses', 
        label: 'StorageClasses', 
        icon: Monitor, 
        description: '存储类',
        count: 3
      },
    ]
  }
];

const SidebarMenu: React.FC<SidebarMenuProps> = ({
  currentView,
  onViewChange,
  collapsed = false,
  onToggleCollapse,
  className
}) => {
  const [categories, setCategories] = useState(defaultCategories);

  // 切换分类展开/折叠状态
  const toggleCategory = (categoryIndex: number) => {
    if (collapsed) return; // 侧边栏折叠时不允许操作
    
    setCategories(prev => prev.map((cat, index) => 
      index === categoryIndex 
        ? { ...cat, collapsed: !cat.collapsed }
        : cat
    ));
  };

  return (
    <div className={cn("flex flex-col h-full bg-white border-r border-gray-200", className)}>
      {/* 头部 */}
      <div className="p-4 border-b border-gray-200">
        <div className="flex items-center justify-between">
          <AnimatePresence>
            {!collapsed && (
              <motion.div
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -20 }}
                className="flex items-center space-x-3"
              >
                <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-blue-600 rounded-lg flex items-center justify-center">
                  <Grid3X3 className="w-4 h-4 text-white" />
                </div>
                <div>
                  <h2 className="text-sm font-semibold text-gray-900">Kubernetes</h2>
                  <p className="text-xs text-gray-500">资源管理</p>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
          
          {onToggleCollapse && (
            <Button
              variant="ghost"
              size="icon"
              className="w-8 h-8"
              onClick={onToggleCollapse}
            >
              {collapsed ? (
                <ChevronRight className="w-4 h-4" />
              ) : (
                <ChevronLeft className="w-4 h-4" />
              )}
            </Button>
          )}
        </div>
      </div>

      {/* 菜单内容 */}
      <div className="flex-1 overflow-y-auto py-4">
        {/* 总览菜单项 */}
        <div className="px-4 mb-6">
          <motion.div
            whileHover={{ scale: collapsed ? 1 : 1.02 }}
            whileTap={{ scale: 0.98 }}
            className={cn(
              "flex items-center p-3 rounded-xl cursor-pointer transition-all duration-200",
              currentView === 'overview'
                ? "bg-gradient-to-r from-blue-50 to-blue-100 border-2 border-blue-200"
                : "hover:bg-gray-50 border-2 border-transparent"
            )}
            onClick={() => onViewChange('overview')}
          >
            <div className={cn(
              "w-10 h-10 rounded-lg flex items-center justify-center",
              currentView === 'overview'
                ? "bg-gradient-to-br from-blue-500 to-blue-600 text-white"
                : "bg-gray-100 text-gray-600"
            )}>
              <Monitor className="w-5 h-5" />
            </div>
            
            <AnimatePresence>
              {!collapsed && (
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

        {/* 分类菜单 */}
        {categories.map((category, categoryIndex) => (
          <div key={category.title} className="px-4 mb-6">
            {/* 分类标题 */}
            <AnimatePresence>
              {!collapsed && (
                <motion.div
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  className="flex items-center justify-between mb-3 cursor-pointer"
                  onClick={() => toggleCategory(categoryIndex)}
                >
                  <div className="flex items-center">
                    <category.icon className="w-4 h-4 text-gray-400 mr-2" />
                    <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">
                      {category.title}
                    </span>
                  </div>
                  <ChevronDown 
                    className={cn(
                      "w-3 h-3 text-gray-400 transition-transform duration-200",
                      category.collapsed ? "-rotate-90" : ""
                    )} 
                  />
                </motion.div>
              )}
            </AnimatePresence>

            {/* 菜单项 */}
            <AnimatePresence>
              {(!collapsed && !category.collapsed) && (
                <motion.div
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  exit={{ opacity: 0, height: 0 }}
                  className="space-y-1 overflow-hidden"
                >
                  {category.items.map((item, itemIndex) => (
                    <motion.div
                      key={item.key}
                      initial={{ opacity: 0, x: -20 }}
                      animate={{ opacity: 1, x: 0 }}
                      exit={{ opacity: 0, x: -20 }}
                      transition={{ delay: itemIndex * 0.05 }}
                      whileHover={{ scale: 1.02 }}
                      whileTap={{ scale: 0.98 }}
                      className={cn(
                        "flex items-center p-2.5 rounded-lg cursor-pointer transition-all duration-200",
                        currentView === item.key
                          ? "bg-gradient-to-r from-blue-50 to-blue-100 border border-blue-200"
                          : "hover:bg-gray-50 border border-transparent"
                      )}
                      onClick={() => onViewChange(item.key)}
                    >
                      <div className={cn(
                        "w-8 h-8 rounded-lg flex items-center justify-center",
                        currentView === item.key
                          ? "bg-gradient-to-br from-blue-500 to-blue-600 text-white"
                          : "bg-gray-100 text-gray-600"
                      )}>
                        <item.icon className="w-4 h-4" />
                      </div>
                      
                      <div className="ml-3 flex-1 min-w-0">
                        <div className="flex items-center justify-between">
                          <span className="text-sm font-medium text-gray-900 truncate">
                            {item.label}
                          </span>
                          {item.count !== undefined && (
                            <Badge variant="secondary" className="text-xs ml-2">
                              {item.count}
                            </Badge>
                          )}
                          {item.badge && (
                            <Badge variant="outline" className="text-xs ml-2">
                              {item.badge}
                            </Badge>
                          )}
                        </div>
                        <div className="text-xs text-gray-500 truncate">
                          {item.description}
                        </div>
                      </div>
                    </motion.div>
                  ))}
                </motion.div>
              )}
            </AnimatePresence>

            {/* 折叠状态下的菜单项 */}
            {collapsed && (
              <div className="space-y-2">
                {category.items.map((item) => (
                  <motion.div
                    key={item.key}
                    whileHover={{ scale: 1.05 }}
                    whileTap={{ scale: 0.95 }}
                    className={cn(
                      "w-10 h-10 rounded-lg flex items-center justify-center cursor-pointer transition-all duration-200",
                      currentView === item.key
                        ? "bg-gradient-to-br from-blue-500 to-blue-600 text-white"
                        : "bg-gray-100 text-gray-600 hover:bg-gray-200"
                    )}
                    onClick={() => onViewChange(item.key)}
                    title={`${item.label} - ${item.description}`}
                  >
                    <item.icon className="w-5 h-5" />
                    {item.count !== undefined && item.count > 0 && (
                      <motion.div
                        initial={{ scale: 0 }}
                        animate={{ scale: 1 }}
                        className="absolute -top-1 -right-1 w-5 h-5 bg-red-500 text-white rounded-full flex items-center justify-center text-xs font-bold"
                      >
                        {item.count > 99 ? '99+' : item.count}
                      </motion.div>
                    )}
                  </motion.div>
                ))}
              </div>
            )}
          </div>
        ))}
      </div>

      {/* 底部状态 */}
      {!collapsed && (
        <div className="p-4 border-t border-gray-200">
          <div className="text-center">
            <div className="text-xs text-gray-500">
              共 {categories.reduce((total, cat) => total + cat.items.length, 0) + 1} 个面板
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default SidebarMenu;
