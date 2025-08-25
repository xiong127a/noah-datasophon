/**
 * Kubernetes Dashboard API 工具函数
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 * 日期：@date
 */

import { apiV1 } from './api-config-v1';
import { API_PATHS_V1 } from './api-config-v1';

export interface K8sNamespace {
  name: string;
  phase: string;
  creationTime: string;
  resourceVersion?: number;
  displayName: string;
  phaseText: string;
  isActive: boolean;
  statusColor: string;
  basicStats?: {
    podCount: number;
    serviceCount: number;
  };
}

export interface K8sResourceStats {
  // Pods统计
  podCount?: number;
  runningPodCount?: number;
  pendingPodCount?: number;
  failedPodCount?: number;
  succeededPodCount?: number;
  
  // Services统计  
  serviceCount?: number;
  clusterIpServiceCount?: number;
  nodePortServiceCount?: number;
  loadBalancerServiceCount?: number;
  
  // Deployments统计
  deploymentCount?: number;
  availableDeploymentCount?: number;
  unavailableDeploymentCount?: number;
  
  // ConfigMaps统计
  configMapCount?: number;
  
  // Secrets统计
  secretCount?: number;
  
  // StatefulSets统计
  statefulSetCount?: number;
  readyStatefulSetCount?: number;
  
  // DaemonSets统计
  daemonSetCount?: number;
  readyDaemonSetCount?: number;
  
  // Jobs统计
  jobCount?: number;
  completedJobCount?: number;
  activeJobCount?: number;
  failedJobCount?: number;
  
  // CronJobs统计
  cronJobCount?: number;
  activeCronJobCount?: number;
  suspendedCronJobCount?: number;
  
  // PersistentVolumes统计
  persistentVolumeCount?: number;
  boundPvCount?: number;
  availablePvCount?: number;
  
  // PersistentVolumeClaims统计
  persistentVolumeClaimCount?: number;
  boundPvcCount?: number;
  pendingPvcCount?: number;
  
  // StorageClasses统计
  storageClassCount?: number;
  
  // Ingresses统计
  ingressCount?: number;
  
  // IngressClasses统计
  ingressClassCount?: number;
  
  // ReplicaSets统计
  replicaSetCount?: number;
  readyReplicaSetCount?: number;
}

export interface K8sResource {
  name: string;
  namespace: string;
  creationTimestamp: string;
  status: string;
  labels?: Record<string, string>;
  annotations?: Record<string, string>;
  spec?: Record<string, unknown>;
  metadata?: Record<string, unknown>;
  // Pod 特定字段
  ready?: string;
  restarts?: number;
  node?: string;
  nodeName?: string;
  hostName?: string;
  host?: string;
  // Service 特定字段
  type?: string;
  clusterIp?: string;
  externalIp?: string;
  ports?: string;
  // Deployment 特定字段
  replicas?: string;
  upToDate?: number;
  available?: number;
  age?: string;
}

export interface K8sResourceListResponse {
  data: K8sResource[];
  total?: number;
  pageNum?: number;
  pageSize?: number;
}

/**
 * Kubernetes Dashboard API 工具类
 */
export class KubernetesAPI {
  
  /**
   * 获取命名空间列表（含基础资源统计）
   */
  static async getNamespaces(clusterId: string): Promise<K8sNamespace[]> {
    try {
      console.log('🌐 发送HTTP请求:', `GET ${API_PATHS_V1.K8S_NAMESPACES}`, { headers: { 'X-Cluster-Id': clusterId } });
      const response = await apiV1.get(API_PATHS_V1.K8S_NAMESPACES, {
        headers: { 'X-Cluster-Id': clusterId }
      });
      console.log('🌐 HTTP响应:', response.status, response.data);
      
      // 🚀 极简版本：只返回命名空间基础信息，性能最佳
      const namespaces: K8sNamespace[] = response.data?.data || [];
      
      console.log('🚀 极简优化生效！无统计数据负担');
      console.log('📊 命名空间列表:', `${namespaces.length}个命名空间`);
      console.log('⚡ 加载速度: 极快！');
      
      return namespaces;
    } catch (error) {
      console.error('❌ 获取命名空间失败:', error);
      throw error;
    }
  }

  /**
   * 获取资源统计
   */
  static async getResourceStats(
    clusterId: string, 
    serviceId?: string, 
    namespace?: string
  ): Promise<K8sResourceStats> {
    try {
      const params: Record<string, unknown> = {};
      if (serviceId) params.serviceId = serviceId;
      if (namespace) params.namespace = namespace;
      
      console.log('🌐 发送HTTP请求:', `GET ${API_PATHS_V1.K8S_RESOURCE_STATS}`, { params, headers: { 'X-Cluster-Id': clusterId } });
      const response = await apiV1.get(API_PATHS_V1.K8S_RESOURCE_STATS, {
        params,
        headers: { 'X-Cluster-Id': clusterId }
      });
      console.log('🌐 HTTP响应:', response.status, response.data);
      return response.data?.data || {};
    } catch (error) {
      console.error('❌ 获取资源统计失败:', error);
      throw error;
    }
  }

  /**
   * 获取Pods列表
   */
  static async getPods(
    clusterId: string,
    namespace?: string,
    serviceId?: string,
    pageNum: number = 1,
    pageSize: number = 10
  ): Promise<K8sResourceListResponse> {
    try {
      const params: Record<string, unknown> = { 
        pageNum, 
        pageSize 
      };
      if (namespace) params.namespace = namespace;
      if (serviceId) params.serviceId = serviceId;

      console.log('🌐 发送HTTP请求:', `GET ${API_PATHS_V1.K8S_PODS}`, { params, headers: { 'X-Cluster-Id': clusterId } });
      const response = await apiV1.get(API_PATHS_V1.K8S_PODS, {
        params,
        headers: { 'X-Cluster-Id': clusterId }
      });
      console.log('🌐 HTTP响应:', response.status, response.data);
      
      // 后端现在返回PageVO结构：{ data, total, pageNum, pageSize }
      const pageVO = response.data?.data || {};
      return {
        data: pageVO.data || [],
        total: pageVO.total || 0,
        pageNum: pageVO.pageNum || pageNum,
        pageSize: pageVO.pageSize || pageSize
      };
    } catch (error) {
      console.error('❌ 获取Pods失败:', error);
      throw error;
    }
  }

  /**
   * 获取Services列表
   */
  static async getServices(
    clusterId: string,
    namespace?: string,
    serviceId?: string,
    pageNum: number = 1,
    pageSize: number = 10
  ): Promise<K8sResourceListResponse> {
    try {
      const params: Record<string, unknown> = { 
        pageNum, 
        pageSize 
      };
      if (namespace) params.namespace = namespace;
      if (serviceId) params.serviceId = serviceId;

      const response = await apiV1.get(API_PATHS_V1.K8S_SERVICES, {
        params,
        headers: { 'X-Cluster-Id': clusterId }
      });
      
      console.log('🔍 Services API响应结构:', {
        code: response.data?.code,
        hasPageVO: !!response.data?.data,
        dataLength: response.data?.data?.data?.length
      });
      
      // 后端现在返回PageVO结构：{ data, total, pageNum, pageSize }
      const pageVO = response.data?.data || {};
      return {
        data: pageVO.data || [],
        total: pageVO.total || 0,
        pageNum: pageVO.pageNum || pageNum,
        pageSize: pageVO.pageSize || pageSize
      };
    } catch (error) {
      console.error('获取Services失败:', error);
      throw error;
    }
  }

  /**
   * 获取Deployments列表
   */
  static async getDeployments(
    clusterId: string,
    namespace?: string,
    serviceId?: string,
    pageNum: number = 1,
    pageSize: number = 10
  ): Promise<K8sResourceListResponse> {
    try {
      const params: Record<string, unknown> = { 
        pageNum, 
        pageSize 
      };
      if (namespace) params.namespace = namespace;
      if (serviceId) params.serviceId = serviceId;

      const response = await apiV1.get(API_PATHS_V1.K8S_DEPLOYMENTS, {
        params,
        headers: { 'X-Cluster-Id': clusterId }
      });
      
      console.log('🔍 Deployments API响应结构:', {
        code: response.data?.code,
        hasPageVO: !!response.data?.data,
        rawResponse: response.data,
        pageVOData: response.data?.data?.data,
        dataLength: response.data?.data?.data?.length
      });
      
      // 后端现在返回PageVO结构：{ data, total, pageNum, pageSize }
      const pageVO = response.data?.data || {};
      return {
        data: pageVO.data || [],
        total: pageVO.total || 0,
        pageNum: pageVO.pageNum || pageNum,
        pageSize: pageVO.pageSize || pageSize
      };
    } catch (error) {
      console.error('获取Deployments失败:', error);
      throw error;
    }
  }

  /**
   * 获取ConfigMaps列表
   */
  static async getConfigMaps(
    clusterId: string,
    namespace?: string,
    pageNum: number = 1,
    pageSize: number = 10
  ): Promise<K8sResourceListResponse> {
    try {
      const params: Record<string, unknown> = { 
        pageNum, 
        pageSize 
      };
      if (namespace) params.namespace = namespace;

      const response = await apiV1.get(API_PATHS_V1.K8S_CONFIGMAPS, {
        params,
        headers: { 'X-Cluster-Id': clusterId }
      });
      return {
        data: response.data?.data || [],
        total: response.data?.total,
        pageNum,
        pageSize
      };
    } catch (error) {
      console.error('获取ConfigMaps失败:', error);
      throw error;
    }
  }

  /**
   * 获取Secrets列表
   */
  static async getSecrets(
    clusterId: string,
    namespace?: string,
    pageNum: number = 1,
    pageSize: number = 10
  ): Promise<K8sResourceListResponse> {
    try {
      const params: Record<string, unknown> = { 
        pageNum, 
        pageSize 
      };
      if (namespace) params.namespace = namespace;

      const response = await apiV1.get(API_PATHS_V1.K8S_SECRETS, {
        params,
        headers: { 'X-Cluster-Id': clusterId }
      });
      return {
        data: response.data?.data || [],
        total: response.data?.total,
        pageNum,
        pageSize
      };
    } catch (error) {
      console.error('获取Secrets失败:', error);
      throw error;
    }
  }

  /**
   * 获取DaemonSets列表
   */
  static async getDaemonSets(
    clusterId: string,
    namespace?: string,
    serviceId?: string,
    pageNum: number = 1,
    pageSize: number = 10
  ): Promise<K8sResourceListResponse> {
    try {
      const params: Record<string, unknown> = { 
        pageNum, 
        pageSize 
      };
      if (namespace) params.namespace = namespace;
      if (serviceId) params.serviceId = serviceId;

      const response = await apiV1.get(API_PATHS_V1.K8S_DAEMONSETS, {
        params,
        headers: { 'X-Cluster-Id': clusterId }
      });
      return {
        data: response.data?.data || [],
        total: response.data?.total,
        pageNum,
        pageSize
      };
    } catch (error) {
      console.error('获取DaemonSets失败:', error);
      throw error;
    }
  }

  /**
   * 获取StatefulSets列表
   */
  static async getStatefulSets(
    clusterId: string,
    namespace?: string,
    pageNum: number = 1,
    pageSize: number = 10
  ): Promise<K8sResourceListResponse> {
    try {
      const params: Record<string, unknown> = { 
        pageNum, 
        pageSize 
      };
      if (namespace) params.namespace = namespace;

      const response = await apiV1.get(API_PATHS_V1.K8S_STATEFULSETS, {
        params,
        headers: { 'X-Cluster-Id': clusterId }
      });
      return {
        data: response.data?.data || [],
        total: response.data?.total,
        pageNum,
        pageSize
      };
    } catch (error) {
      console.error('获取StatefulSets失败:', error);
      throw error;
    }
  }

  /**
   * 获取ReplicaSets列表
   */
  static async getReplicaSets(
    clusterId: string,
    namespace?: string,
    pageNum: number = 1,
    pageSize: number = 10
  ): Promise<K8sResourceListResponse> {
    try {
      const params: Record<string, unknown> = { 
        pageNum, 
        pageSize 
      };
      if (namespace) params.namespace = namespace;

      const response = await apiV1.get(API_PATHS_V1.K8S_REPLICASETS, {
        params,
        headers: { 'X-Cluster-Id': clusterId }
      });
      return {
        data: response.data?.data || [],
        total: response.data?.total,
        pageNum,
        pageSize
      };
    } catch (error) {
      console.error('获取ReplicaSets失败:', error);
      throw error;
    }
  }

  /**
   * 获取Jobs列表
   */
  static async getJobs(
    clusterId: string,
    namespace?: string,
    serviceId?: string,
    pageNum: number = 1,
    pageSize: number = 10
  ): Promise<K8sResourceListResponse> {
    try {
      const params: Record<string, unknown> = { 
        pageNum, 
        pageSize 
      };
      if (namespace) params.namespace = namespace;
      if (serviceId) params.serviceId = serviceId;

      const response = await apiV1.get(API_PATHS_V1.K8S_JOBS, {
        params,
        headers: { 'X-Cluster-Id': clusterId }
      });
      return {
        data: response.data?.data || [],
        total: response.data?.total,
        pageNum,
        pageSize
      };
    } catch (error) {
      console.error('获取Jobs失败:', error);
      throw error;
    }
  }

  /**
   * 获取CronJobs列表
   */
  static async getCronJobs(
    clusterId: string,
    namespace?: string,
    pageNum: number = 1,
    pageSize: number = 10
  ): Promise<K8sResourceListResponse> {
    try {
      const params: Record<string, unknown> = { 
        pageNum, 
        pageSize 
      };
      if (namespace) params.namespace = namespace;

      const response = await apiV1.get(API_PATHS_V1.K8S_CRONJOBS, {
        params,
        headers: { 'X-Cluster-Id': clusterId }
      });
      return {
        data: response.data?.data || [],
        total: response.data?.total,
        pageNum,
        pageSize
      };
    } catch (error) {
      console.error('获取CronJobs失败:', error);
      throw error;
    }
  }

  /**
   * 获取PersistentVolumes列表
   */
  static async getPersistentVolumes(
    clusterId: string,
    pageNum: number = 1,
    pageSize: number = 10
  ): Promise<K8sResourceListResponse> {
    try {
      const params: Record<string, unknown> = { 
        pageNum, 
        pageSize 
      };

      const response = await apiV1.get(API_PATHS_V1.K8S_PERSISTENTVOLUMES, {
        params,
        headers: { 'X-Cluster-Id': clusterId }
      });
      return {
        data: response.data?.data || [],
        total: response.data?.total,
        pageNum,
        pageSize
      };
    } catch (error) {
      console.error('获取PersistentVolumes失败:', error);
      throw error;
    }
  }

  /**
   * 获取PersistentVolumeClaims列表
   */
  static async getPersistentVolumeClaims(
    clusterId: string,
    namespace?: string,
    pageNum: number = 1,
    pageSize: number = 10
  ): Promise<K8sResourceListResponse> {
    try {
      const params: Record<string, unknown> = { 
        pageNum, 
        pageSize 
      };
      if (namespace) params.namespace = namespace;

      const response = await apiV1.get(API_PATHS_V1.K8S_PVCS, {
        params,
        headers: { 'X-Cluster-Id': clusterId }
      });
      return {
        data: response.data?.data || [],
        total: response.data?.total,
        pageNum,
        pageSize
      };
    } catch (error) {
      console.error('获取PersistentVolumeClaims失败:', error);
      throw error;
    }
  }

  /**
   * 获取StorageClasses列表
   */
  static async getStorageClasses(
    clusterId: string,
    pageNum: number = 1,
    pageSize: number = 10
  ): Promise<K8sResourceListResponse> {
    try {
      const params: Record<string, unknown> = { 
        pageNum, 
        pageSize 
      };

      const response = await apiV1.get(API_PATHS_V1.K8S_STORAGECLASSES, {
        params,
        headers: { 'X-Cluster-Id': clusterId }
      });
      return {
        data: response.data?.data || [],
        total: response.data?.total,
        pageNum,
        pageSize
      };
    } catch (error) {
      console.error('获取StorageClasses失败:', error);
      throw error;
    }
  }

  /**
   * 获取Ingresses列表
   */
  static async getIngresses(
    clusterId: string,
    namespace?: string,
    pageNum: number = 1,
    pageSize: number = 10
  ): Promise<K8sResourceListResponse> {
    try {
      const params: Record<string, unknown> = { 
        pageNum, 
        pageSize 
      };
      if (namespace) params.namespace = namespace;

      const response = await apiV1.get(API_PATHS_V1.K8S_INGRESSES, {
        params,
        headers: { 'X-Cluster-Id': clusterId }
      });
      return {
        data: response.data?.data || [],
        total: response.data?.total,
        pageNum,
        pageSize
      };
    } catch (error) {
      console.error('获取Ingresses失败:', error);
      throw error;
    }
  }

  /**
   * 获取IngressClasses列表
   */
  static async getIngressClasses(
    clusterId: string,
    pageNum: number = 1,
    pageSize: number = 10
  ): Promise<K8sResourceListResponse> {
    try {
      const params: Record<string, unknown> = { 
        pageNum, 
        pageSize 
      };

      const response = await apiV1.get(API_PATHS_V1.K8S_INGRESSCLASSES, {
        params,
        headers: { 'X-Cluster-Id': clusterId }
      });
      return {
        data: response.data?.data || [],
        total: response.data?.total,
        pageNum,
        pageSize
      };
    } catch (error) {
      console.error('获取IngressClasses失败:', error);
      throw error;
    }
  }
}
