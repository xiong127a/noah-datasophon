/**
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-01-15
 * @description Kubernetes状态配色和样式工具函数
 */

// Pod状态配色映射
export const getPodStatusConfig = (status: string) => {
  const statusLower = status?.toLowerCase() || '';
  
  switch (statusLower) {
    case 'running':
      return {
        variant: 'default' as const,
        className: 'bg-green-100 text-green-800 border-green-200 font-medium',
        color: 'text-green-600',
        bgColor: 'bg-green-50',
        borderColor: 'border-green-200'
      };
    case 'pending':
      return {
        variant: 'secondary' as const, 
        className: 'bg-yellow-100 text-yellow-800 border-yellow-200 font-medium',
        color: 'text-yellow-600',
        bgColor: 'bg-yellow-50',
        borderColor: 'border-yellow-200'
      };
    case 'failed':
    case 'error':
      return {
        variant: 'destructive' as const,
        className: 'bg-red-100 text-red-800 border-red-200 font-medium',
        color: 'text-red-600', 
        bgColor: 'bg-red-50',
        borderColor: 'border-red-200'
      };
    case 'succeeded':
    case 'completed':
      return {
        variant: 'default' as const,
        className: 'bg-blue-100 text-blue-800 border-blue-200 font-medium',
        color: 'text-blue-600',
        bgColor: 'bg-blue-50', 
        borderColor: 'border-blue-200'
      };
    case 'terminating':
      return {
        variant: 'outline' as const,
        className: 'bg-orange-100 text-orange-800 border-orange-200 font-medium',
        color: 'text-orange-600',
        bgColor: 'bg-orange-50',
        borderColor: 'border-orange-200'
      };
    case 'crashloopbackoff':
      return {
        variant: 'destructive' as const,
        className: 'bg-red-100 text-red-800 border-red-200 font-medium animate-pulse',
        color: 'text-red-600',
        bgColor: 'bg-red-50',
        borderColor: 'border-red-200'
      };
    case 'imagepullbackoff':
    case 'errimagepull':
      return {
        variant: 'destructive' as const,
        className: 'bg-red-100 text-red-800 border-red-200 font-medium',
        color: 'text-red-600',
        bgColor: 'bg-red-50',
        borderColor: 'border-red-200'
      };
    default:
      return {
        variant: 'outline' as const,
        className: 'bg-gray-100 text-gray-800 border-gray-200 font-medium',
        color: 'text-gray-600',
        bgColor: 'bg-gray-50',
        borderColor: 'border-gray-200'
      };
  }
};

// Service状态配色映射
export const getServiceStatusConfig = (type: string) => {
  const typeLower = type?.toLowerCase() || '';
  
  switch (typeLower) {
    case 'clusterip':
      return {
        variant: 'default' as const,
        className: 'bg-green-100 text-green-800 border-green-200',
        color: 'text-green-600'
      };
    case 'nodeport':
      return {
        variant: 'secondary' as const,
        className: 'bg-blue-100 text-blue-800 border-blue-200',
        color: 'text-blue-600'
      };
    case 'loadbalancer':
      return {
        variant: 'default' as const,
        className: 'bg-purple-100 text-purple-800 border-purple-200',
        color: 'text-purple-600'
      };
    case 'externalname':
      return {
        variant: 'outline' as const,
        className: 'bg-cyan-100 text-cyan-800 border-cyan-200',
        color: 'text-cyan-600'
      };
    default:
      return {
        variant: 'outline' as const,
        className: 'bg-gray-100 text-gray-800 border-gray-200',
        color: 'text-gray-600'
      };
  }
};

// Deployment副本状态配色
export const getReplicaStatusConfig = (ready: number, desired: number) => {
  const readyRatio = desired > 0 ? ready / desired : 0;
  
  if (readyRatio === 1) {
    return {
      className: 'text-green-600 font-medium',
      bgColor: 'bg-green-50',
      borderColor: 'border-green-200'
    };
  } else if (readyRatio >= 0.5) {
    return {
      className: 'text-yellow-600 font-medium',
      bgColor: 'bg-yellow-50', 
      borderColor: 'border-yellow-200'
    };
  } else {
    return {
      className: 'text-red-600 font-medium',
      bgColor: 'bg-red-50',
      borderColor: 'border-red-200'
    };
  }
};

// 通用健康状态配色
export const getHealthStatusConfig = (isHealthy: boolean) => {
  return isHealthy ? {
    className: 'text-green-600',
    bgColor: 'bg-green-100',
    borderColor: 'border-green-200',
    indicator: 'bg-green-400'
  } : {
    className: 'text-red-600', 
    bgColor: 'bg-red-100',
    borderColor: 'border-red-200',
    indicator: 'bg-red-400'
  };
};

// PV绑定状态配色  
export const getPVStatusConfig = (phase: string) => {
  const phaseLower = phase?.toLowerCase() || '';
  
  switch (phaseLower) {
    case 'bound':
      return {
        variant: 'default' as const,
        className: 'bg-green-100 text-green-800 border-green-200',
        color: 'text-green-600'
      };
    case 'available':
      return {
        variant: 'secondary' as const,
        className: 'bg-blue-100 text-blue-800 border-blue-200', 
        color: 'text-blue-600'
      };
    case 'released':
      return {
        variant: 'outline' as const,
        className: 'bg-yellow-100 text-yellow-800 border-yellow-200',
        color: 'text-yellow-600'
      };
    case 'failed':
      return {
        variant: 'destructive' as const,
        className: 'bg-red-100 text-red-800 border-red-200',
        color: 'text-red-600'
      };
    default:
      return {
        variant: 'outline' as const,
        className: 'bg-gray-100 text-gray-800 border-gray-200',
        color: 'text-gray-600'
      };
  }
};

// Job状态配色
export const getJobStatusConfig = (active: number, succeeded: number, failed: number) => {
  if (failed > 0) {
    return {
      variant: 'destructive' as const,
      className: 'bg-red-100 text-red-800 border-red-200',
      color: 'text-red-600',
      status: 'Failed'
    };
  } else if (succeeded > 0) {
    return {
      variant: 'default' as const, 
      className: 'bg-green-100 text-green-800 border-green-200',
      color: 'text-green-600',
      status: 'Completed'
    };
  } else if (active > 0) {
    return {
      variant: 'secondary' as const,
      className: 'bg-blue-100 text-blue-800 border-blue-200',
      color: 'text-blue-600', 
      status: 'Running'
    };
  } else {
    return {
      variant: 'outline' as const,
      className: 'bg-gray-100 text-gray-800 border-gray-200',
      color: 'text-gray-600',
      status: 'Pending'
    };
  }
};

// CronJob状态配色
export const getCronJobStatusConfig = (suspend: boolean, lastScheduleTime?: string) => {
  if (suspend) {
    return {
      variant: 'outline' as const,
      className: 'bg-gray-100 text-gray-800 border-gray-200',
      color: 'text-gray-600',
      status: 'Suspended'
    };
  } else {
    return {
      variant: 'default' as const,
      className: 'bg-green-100 text-green-800 border-green-200',
      color: 'text-green-600',
      status: 'Active'
    };
  }
};

// 获取状态点指示器颜色
export const getStatusDotColor = (status: string) => {
  const config = getPodStatusConfig(status);
  
  switch (status?.toLowerCase()) {
    case 'running':
      return 'bg-green-400';
    case 'pending':
      return 'bg-yellow-400';
    case 'failed':
    case 'error':
      return 'bg-red-400';
    case 'succeeded':
    case 'completed':
      return 'bg-blue-400';
    case 'terminating':
      return 'bg-orange-400';
    default:
      return 'bg-gray-400';
  }
};
