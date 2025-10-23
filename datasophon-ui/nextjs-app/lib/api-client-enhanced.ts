/**
 * 增强版API客户端
 * 自动处理集群ID请求头的传递
 */

import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { 
  createClusterHeaders, 
  getCurrentClusterId, 
  CLUSTER_ID_HEADER,
  ensureClusterId 
} from './cluster-id-header'
import { API_BASE_URL } from './api-config-v1'

/**
 * 增强版API客户端配置
 */
interface EnhancedApiConfig extends AxiosRequestConfig {
  clusterId?: number | null  // 可选的集群ID，如果不提供则使用全局设置
  requireClusterId?: boolean // 是否强制要求集群ID
}

/**
 * 🔧 修复Long类型精度丢失的JSON解析器
 * 将超过JavaScript安全整数范围的数字保持为字符串
 */
function parseJsonWithLongSupport(jsonString: string): any {
  try {
    // 使用正则表达式找到可能导致精度丢失的长整数
    // JavaScript安全整数范围：-(2^53-1) 到 2^53-1
    const safeMaxInt = Number.MAX_SAFE_INTEGER; // 9007199254740991
    
    // 匹配超长数字的正则表达式（15位以上的整数）
    const longIntRegex = /"?(-?\d{15,})"?/g;
    
    // 将超长整数用引号包裹，确保解析为字符串
    const processedJson = jsonString.replace(longIntRegex, (match, number) => {
      const num = Math.abs(parseInt(number));
      if (num > safeMaxInt) {
        return `"${number}"`; // 强制转为字符串
      }
      return match; // 保持原样
    });
    
    return JSON.parse(processedJson);
  } catch (error) {
    console.warn('JSON解析失败，使用原生解析:', error);
    return JSON.parse(jsonString); // 回退到原生解析
  }
}

/**
 * 创建增强版API客户端实例
 */
export function createEnhancedApiClient(): AxiosInstance {
  const client = axios.create({
    baseURL: API_BASE_URL,
    timeout: 30000,
    headers: {
      'Content-Type': 'application/json',
    },
    // 🔧 添加自定义响应转换器，处理Long类型精度丢失
    transformResponse: [
      function (data: any) {
        if (typeof data === 'string') {
          return parseJsonWithLongSupport(data);
        }
        return data;
      }
    ]
  })

  // 请求拦截器：自动添加集群ID请求头
  client.interceptors.request.use(
    (config) => {
      // 从config中提取自定义配置
      const enhancedConfig = config as EnhancedApiConfig
      const { clusterId, requireClusterId = false } = enhancedConfig

      // 清理自定义属性，避免传递给axios
      delete enhancedConfig.clusterId
      delete enhancedConfig.requireClusterId

      // 获取要使用的集群ID
      const clusterIdToUse = clusterId ?? getCurrentClusterId()

      // 如果强制要求集群ID但没有提供，抛出错误
      if (requireClusterId && (!clusterIdToUse || clusterIdToUse <= 0)) {
        throw new Error('此操作需要有效的集群ID，请先选择一个集群')
      }

      // 如果有集群ID，添加到请求头
      if (clusterIdToUse && clusterIdToUse > 0) {
        config.headers = config.headers || {}
        config.headers[CLUSTER_ID_HEADER] = clusterIdToUse.toString()
      }

      // 集群ID已添加到请求头

      return config
    },
    (error) => {
      return Promise.reject(error)
    }
  )

  // 响应拦截器：处理通用错误
  client.interceptors.response.use(
    (response: AxiosResponse) => {
      // 响应处理成功
      return response
    },
    (error) => {
      // 处理集群ID相关的错误
      if (error.response?.status === 400 && 
          error.response?.data?.msg?.includes('cluster')) {
        console.error('集群ID相关错误:', error.response.data.msg)
      }

      return Promise.reject(error)
    }
  )

  return client
}

/**
 * 默认的增强版API客户端实例
 */
export const enhancedApiClient = createEnhancedApiClient()

/**
 * 增强版API调用方法
 */
export class EnhancedApi {
  private client: AxiosInstance

  constructor(client?: AxiosInstance) {
    this.client = client || enhancedApiClient
  }

  /**
   * GET请求
   */
  async get<T = any>(
    url: string, 
    config: EnhancedApiConfig = {}
  ): Promise<AxiosResponse<T>> {
    return this.client.get<T>(url, config)
  }

  /**
   * POST请求
   */
  async post<T = any>(
    url: string, 
    data?: any, 
    config: EnhancedApiConfig = {}
  ): Promise<AxiosResponse<T>> {
    return this.client.post<T>(url, data, config)
  }

  /**
   * PUT请求
   */
  async put<T = any>(
    url: string, 
    data?: any, 
    config: EnhancedApiConfig = {}
  ): Promise<AxiosResponse<T>> {
    return this.client.put<T>(url, data, config)
  }

  /**
   * DELETE请求
   */
  async delete<T = any>(
    url: string, 
    config: EnhancedApiConfig = {}
  ): Promise<AxiosResponse<T>> {
    return this.client.delete<T>(url, config)
  }

  /**
   * PATCH请求
   */
  async patch<T = any>(
    url: string, 
    data?: any, 
    config: EnhancedApiConfig = {}
  ): Promise<AxiosResponse<T>> {
    return this.client.patch<T>(url, data, config)
  }

  /**
   * 需要集群ID的GET请求
   */
  async getWithCluster<T = any>(
    url: string, 
    clusterId?: number | null,
    config: EnhancedApiConfig = {}
  ): Promise<AxiosResponse<T>> {
    return this.get<T>(url, {
      ...config,
      clusterId,
      requireClusterId: true
    })
  }

  /**
   * 需要集群ID的POST请求
   */
  async postWithCluster<T = any>(
    url: string, 
    data?: any,
    clusterId?: number | null,
    config: EnhancedApiConfig = {}
  ): Promise<AxiosResponse<T>> {
    return this.post<T>(url, data, {
      ...config,
      clusterId,
      requireClusterId: true
    })
  }

  /**
   * 需要集群ID的PUT请求
   */
  async putWithCluster<T = any>(
    url: string, 
    data?: any,
    clusterId?: number | null,
    config: EnhancedApiConfig = {}
  ): Promise<AxiosResponse<T>> {
    return this.put<T>(url, data, {
      ...config,
      clusterId,
      requireClusterId: true
    })
  }

  /**
   * 需要集群ID的DELETE请求
   */
  async deleteWithCluster<T = any>(
    url: string, 
    clusterId?: number | null,
    config: EnhancedApiConfig = {}
  ): Promise<AxiosResponse<T>> {
    return this.delete<T>(url, {
      ...config,
      clusterId,
      requireClusterId: true
    })
  }
}

/**
 * 默认的增强版API实例
 */
export const enhancedApi = new EnhancedApi()

/**
 * 集群相关API的便捷方法
 */
export const clusterApiEnhanced = {
  /**
   * 获取集群详情
   */
  getDetail: (clusterId?: number | null) => 
    enhancedApi.getWithCluster('/api/v1/cluster/detail', clusterId),

  /**
   * 更新集群状态
   */
  updateState: (clusterState: number, clusterId?: number | null) => 
    enhancedApi.postWithCluster('/api/v1/cluster/updateClusterState', 
      null, clusterId, { 
        params: { clusterState } 
      }),

  /**
   * 更新Kubernetes配置
   */
  updateKubeConfig: (config: {
    kubeConfig: string
    namespace: string
    customNamespace?: string
  }, clusterId?: number | null) => 
    enhancedApi.postWithCluster('/api/v1/cluster/kube-config', config, clusterId),

  /**
   * 获取集群主机列表
   */
  getHostList: (params: {
    hostname?: string
    ip?: string
    page: number
    pageSize: number
  }, clusterId?: number | null) => 
    enhancedApi.getWithCluster('/api/v1/cluster/host/list', clusterId, { params }),

  /**
   * 获取集群服务列表
   */
  getServiceList: (clusterId?: number | null) => 
    enhancedApi.getWithCluster('/api/v1/cluster/service/list', clusterId),
}

// 为了向后兼容，导出原有的接口
export { enhancedApiClient as apiClient, enhancedApi as api }
export default enhancedApi