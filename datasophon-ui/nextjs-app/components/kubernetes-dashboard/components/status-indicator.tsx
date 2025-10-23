/**
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 * @description Kubernetes状态指示器组件
 */

"use client";

import React from "react";
import { motion } from "framer-motion";
import {
  CheckCircle,
  AlertCircle,
  XCircle,
  Clock,
  Loader2,
  Pause,
  Play,
  RefreshCw,
  Minus,
  Info
} from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

export type StatusType = 
  | 'healthy' 
  | 'warning' 
  | 'error' 
  | 'pending' 
  | 'loading'
  | 'stopped'
  | 'running'
  | 'restarting'
  | 'unknown'
  | 'info'
  // Pod状态
  | 'Running'
  | 'Pending'
  | 'Succeeded'
  | 'Failed'
  | 'Unknown'
  // Service状态
  | 'Active'
  | 'Inactive'
  // Deployment状态
  | 'Available'
  | 'Progressing'
  | 'ReplicaFailure'
  // 通用状态
  | 'Ready'
  | 'NotReady'
  | 'Ready,SchedulingDisabled'
  | 'NotReady,SchedulingDisabled'
  | 'Terminating'
  | 'Creating';

export type StatusSize = 'xs' | 'sm' | 'md' | 'lg';

interface StatusIndicatorProps {
  status: StatusType;
  size?: StatusSize;
  showText?: boolean;
  customText?: string;
  showBadge?: boolean;
  animate?: boolean;
  className?: string;
}

interface StatusConfig {
  icon: React.ComponentType<{ className?: string }>;
  color: string;
  bgColor: string;
  textColor: string;
  text: string;
  pulse?: boolean;
  spin?: boolean;
}

const statusConfigs: Record<StatusType, StatusConfig> = {
  // 基础状态
  healthy: {
    icon: CheckCircle,
    color: 'text-green-500',
    bgColor: 'bg-green-100',
    textColor: 'text-green-700',
    text: '健康',
    pulse: true
  },
  warning: {
    icon: AlertCircle,
    color: 'text-yellow-500',
    bgColor: 'bg-yellow-100',
    textColor: 'text-yellow-700',
    text: '警告',
    pulse: true
  },
  error: {
    icon: XCircle,
    color: 'text-red-500',
    bgColor: 'bg-red-100',
    textColor: 'text-red-700',
    text: '错误'
  },
  pending: {
    icon: Clock,
    color: 'text-blue-500',
    bgColor: 'bg-blue-100',
    textColor: 'text-blue-700',
    text: '等待中',
    pulse: true
  },
  loading: {
    icon: Loader2,
    color: 'text-blue-500',
    bgColor: 'bg-blue-100',
    textColor: 'text-blue-700',
    text: '加载中',
    spin: true
  },
  stopped: {
    icon: Pause,
    color: 'text-gray-500',
    bgColor: 'bg-gray-100',
    textColor: 'text-gray-700',
    text: '已停止'
  },
  running: {
    icon: Play,
    color: 'text-green-500',
    bgColor: 'bg-green-100',
    textColor: 'text-green-700',
    text: '运行中',
    pulse: true
  },
  restarting: {
    icon: RefreshCw,
    color: 'text-orange-500',
    bgColor: 'bg-orange-100',
    textColor: 'text-orange-700',
    text: '重启中',
    spin: true
  },
  unknown: {
    icon: Minus,
    color: 'text-gray-400',
    bgColor: 'bg-gray-100',
    textColor: 'text-gray-600',
    text: '未知'
  },
  info: {
    icon: Info,
    color: 'text-blue-500',
    bgColor: 'bg-blue-100',
    textColor: 'text-blue-700',
    text: '信息'
  },

  // Pod状态
  Running: {
    icon: CheckCircle,
    color: 'text-green-500',
    bgColor: 'bg-green-100',
    textColor: 'text-green-700',
    text: 'Running',
    pulse: true
  },
  Pending: {
    icon: Clock,
    color: 'text-yellow-500',
    bgColor: 'bg-yellow-100',
    textColor: 'text-yellow-700',
    text: 'Pending',
    pulse: true
  },
  Succeeded: {
    icon: CheckCircle,
    color: 'text-green-500',
    bgColor: 'bg-green-100',
    textColor: 'text-green-700',
    text: 'Succeeded'
  },
  Failed: {
    icon: XCircle,
    color: 'text-red-500',
    bgColor: 'bg-red-100',
    textColor: 'text-red-700',
    text: 'Failed'
  },
  Unknown: {
    icon: AlertCircle,
    color: 'text-gray-400',
    bgColor: 'bg-gray-100',
    textColor: 'text-gray-600',
    text: 'Unknown'
  },

  // Service状态
  Active: {
    icon: CheckCircle,
    color: 'text-green-500',
    bgColor: 'bg-green-100',
    textColor: 'text-green-700',
    text: 'Active',
    pulse: true
  },
  Inactive: {
    icon: Pause,
    color: 'text-gray-500',
    bgColor: 'bg-gray-100',
    textColor: 'text-gray-700',
    text: 'Inactive'
  },

  // Deployment状态
  Available: {
    icon: CheckCircle,
    color: 'text-green-500',
    bgColor: 'bg-green-100',
    textColor: 'text-green-700',
    text: 'Available',
    pulse: true
  },
  Progressing: {
    icon: Loader2,
    color: 'text-blue-500',
    bgColor: 'bg-blue-100',
    textColor: 'text-blue-700',
    text: 'Progressing',
    spin: true
  },
  ReplicaFailure: {
    icon: XCircle,
    color: 'text-red-500',
    bgColor: 'bg-red-100',
    textColor: 'text-red-700',
    text: 'ReplicaFailure'
  },

  // 通用状态
  Ready: {
    icon: CheckCircle,
    color: 'text-green-500',
    bgColor: 'bg-green-100',
    textColor: 'text-green-700',
    text: 'Ready',
    pulse: true
  },
  NotReady: {
    icon: AlertCircle,
    color: 'text-red-500',
    bgColor: 'bg-red-100',
    textColor: 'text-red-700',
    text: 'NotReady'
  },
  'Ready,SchedulingDisabled': {
    icon: CheckCircle,
    color: 'text-orange-500',
    bgColor: 'bg-orange-100',
    textColor: 'text-orange-700',
    text: 'Ready, SchedulingDisabled'
  },
  'NotReady,SchedulingDisabled': {
    icon: AlertCircle,
    color: 'text-red-500',
    bgColor: 'bg-red-100',
    textColor: 'text-red-700',
    text: 'NotReady, SchedulingDisabled'
  },
  Terminating: {
    icon: RefreshCw,
    color: 'text-orange-500',
    bgColor: 'bg-orange-100',
    textColor: 'text-orange-700',
    text: 'Terminating',
    spin: true
  },
  Creating: {
    icon: Loader2,
    color: 'text-blue-500',
    bgColor: 'bg-blue-100',
    textColor: 'text-blue-700',
    text: 'Creating',
    spin: true
  }
};

const sizeConfigs = {
  xs: {
    icon: 'w-3 h-3',
    container: 'w-6 h-6',
    badge: 'text-xs px-1.5 py-0.5',
    text: 'text-xs'
  },
  sm: {
    icon: 'w-4 h-4',
    container: 'w-8 h-8',
    badge: 'text-xs px-2 py-1',
    text: 'text-sm'
  },
  md: {
    icon: 'w-5 h-5',
    container: 'w-10 h-10',
    badge: 'text-sm px-2.5 py-1',
    text: 'text-sm'
  },
  lg: {
    icon: 'w-6 h-6',
    container: 'w-12 h-12',
    badge: 'text-base px-3 py-1.5',
    text: 'text-base'
  }
};

const StatusIndicator: React.FC<StatusIndicatorProps> = ({
  status,
  size = 'md',
  showText = false,
  customText,
  showBadge = false,
  animate = true,
  className
}) => {
  const config = statusConfigs[status] || statusConfigs.unknown;
  const sizeConfig = sizeConfigs[size];
  const Icon = config.icon;

  const displayText = customText || config.text;

  if (showBadge) {
    return (
      <Badge 
        variant="outline" 
        className={cn(
          'flex items-center space-x-1.5 border',
          config.bgColor,
          config.textColor,
          sizeConfig.badge,
          className
        )}
      >
        <motion.div
          animate={
            animate && config.pulse
              ? { scale: [1, 1.2, 1] }
              : animate && config.spin
              ? { rotate: 360 }
              : {}
          }
          transition={
            config.pulse
              ? { duration: 2, repeat: Infinity }
              : config.spin
              ? { duration: 2, repeat: Infinity, ease: "linear" }
              : {}
          }
        >
          <Icon className={cn(sizeConfig.icon, config.color)} />
        </motion.div>
        <span>{displayText}</span>
      </Badge>
    );
  }

  if (showText) {
    return (
      <div className={cn('flex items-center space-x-2', className)}>
        <motion.div
          className={cn(
            'flex items-center justify-center rounded-full',
            sizeConfig.container,
            config.bgColor
          )}
          animate={
            animate && config.pulse
              ? { scale: [1, 1.1, 1] }
              : {}
          }
          transition={
            config.pulse
              ? { duration: 2, repeat: Infinity }
              : {}
          }
        >
          <motion.div
            animate={
              animate && config.spin
                ? { rotate: 360 }
                : {}
            }
            transition={
              config.spin
                ? { duration: 2, repeat: Infinity, ease: "linear" }
                : {}
            }
          >
            <Icon className={cn(sizeConfig.icon, config.color)} />
          </motion.div>
        </motion.div>
        <span className={cn(sizeConfig.text, config.textColor, 'font-medium')}>
          {displayText}
        </span>
      </div>
    );
  }

  return (
    <motion.div
      className={cn(
        'flex items-center justify-center rounded-full',
        sizeConfig.container,
        config.bgColor,
        className
      )}
      animate={
        animate && config.pulse
          ? { scale: [1, 1.1, 1] }
          : {}
      }
      transition={
        config.pulse
          ? { duration: 2, repeat: Infinity }
          : {}
      }
      title={displayText}
    >
      <motion.div
        animate={
          animate && config.spin
            ? { rotate: 360 }
            : {}
        }
        transition={
          config.spin
            ? { duration: 2, repeat: Infinity, ease: "linear" }
            : {}
        }
      >
        <Icon className={cn(sizeConfig.icon, config.color)} />
      </motion.div>
    </motion.div>
  );
};

// 导出一些常用的状态组合组件
export const PodStatusIndicator: React.FC<{
  phase: 'Running' | 'Pending' | 'Succeeded' | 'Failed' | 'Unknown';
  size?: StatusSize;
  showText?: boolean;
  className?: string;
}> = ({ phase, size, showText, className }) => (
  <StatusIndicator 
    status={phase} 
    size={size} 
    showText={showText} 
    className={className} 
  />
);

export const ServiceStatusIndicator: React.FC<{
  active: boolean;
  size?: StatusSize;
  showText?: boolean;
  className?: string;
}> = ({ active, size, showText, className }) => (
  <StatusIndicator 
    status={active ? 'Active' : 'Inactive'} 
    size={size} 
    showText={showText} 
    className={className} 
  />
);

export const DeploymentStatusIndicator: React.FC<{
  available: boolean;
  progressing: boolean;
  size?: StatusSize;
  showText?: boolean;
  className?: string;
}> = ({ available, progressing, size, showText, className }) => {
  let status: StatusType = 'Available';
  if (progressing) status = 'Progressing';
  else if (!available) status = 'ReplicaFailure';
  
  return (
    <StatusIndicator 
      status={status} 
      size={size} 
      showText={showText} 
      className={className} 
    />
  );
};

export default StatusIndicator;
