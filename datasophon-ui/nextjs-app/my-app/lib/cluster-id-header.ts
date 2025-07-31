/**
 * 集群ID请求头处理工具
 * 用于统一管理集群ID在请求头中的传递
 */

export const CLUSTER_ID_HEADER = 'x-cluster-id'

/**
 * 当前活动的集群ID
 * 可以通过 setCurrentClusterId 设置，通过 getCurrentClusterId 获取
 */
let currentClusterId: number | null = null

/**
 * 设置当前活动的集群ID
 * @param clusterId 集群ID
 */
export function setCurrentClusterId(clusterId: number | null) {
  currentClusterId = clusterId
  // 可选：将集群ID保存到localStorage或sessionStorage
  if (typeof window !== 'undefined') {
    if (clusterId !== null) {
      sessionStorage.setItem('currentClusterId', clusterId.toString())
    } else {
      sessionStorage.removeItem('currentClusterId')
    }
  }
}

/**
 * 获取当前活动的集群ID
 * @returns 集群ID或null
 */
export function getCurrentClusterId(): number | null {
  // 如果内存中没有，尝试从sessionStorage恢复
  if (currentClusterId === null && typeof window !== 'undefined') {
    const stored = sessionStorage.getItem('currentClusterId')
    if (stored) {
      currentClusterId = parseInt(stored, 10)
    }
  }
  return currentClusterId
}

/**
 * 创建包含集群ID的请求头
 * @param clusterId 可选的集群ID，如果不提供则使用当前活动的集群ID
 * @param additionalHeaders 额外的请求头
 * @returns 包含集群ID的请求头对象
 */
export function createClusterHeaders(
  clusterId?: number | null,
  additionalHeaders: Record<string, string> = {}
): Record<string, string> {
  const clusterIdToUse = clusterId ?? getCurrentClusterId()
  
  const headers: Record<string, string> = {
    ...additionalHeaders
  }
  
  if (clusterIdToUse !== null) {
    headers[CLUSTER_ID_HEADER] = clusterIdToUse.toString()
  }
  
  return headers
}

/**
 * 获取集群ID请求头对象（仅包含集群ID）
 * @param clusterId 可选的集群ID，如果不提供则使用当前活动的集群ID
 * @returns 仅包含集群ID的请求头对象
 */
export function getClusterIdHeader(clusterId?: number | null): Record<string, string> {
  const clusterIdToUse = clusterId ?? getCurrentClusterId()
  
  if (clusterIdToUse === null) {
    return {}
  }
  
  return {
    [CLUSTER_ID_HEADER]: clusterIdToUse.toString()
  }
}

/**
 * 验证是否设置了集群ID
 * @param clusterId 可选的集群ID，如果不提供则检查当前活动的集群ID
 * @returns 是否设置了有效的集群ID
 */
export function hasValidClusterId(clusterId?: number | null): boolean {
  const clusterIdToUse = clusterId ?? getCurrentClusterId()
  return clusterIdToUse !== null && clusterIdToUse > 0
}

/**
 * 确保设置了集群ID，如果没有则抛出错误
 * @param clusterId 可选的集群ID，如果不提供则检查当前活动的集群ID
 * @throws Error 如果没有设置有效的集群ID
 */
export function ensureClusterId(clusterId?: number | null): number {
  const clusterIdToUse = clusterId ?? getCurrentClusterId()
  
  if (!hasValidClusterId(clusterIdToUse)) {
    throw new Error('集群ID未设置或无效。请先选择一个集群。')
  }
  
  return clusterIdToUse!
}

/**
 * 从URL参数中提取集群ID并设置为当前活动的集群ID
 * @param searchParams URLSearchParams对象或查询字符串
 */
export function extractAndSetClusterIdFromUrl(searchParams: URLSearchParams | string) {
  let params: URLSearchParams
  
  if (typeof searchParams === 'string') {
    params = new URLSearchParams(searchParams)
  } else {
    params = searchParams
  }
  
  const clusterIdStr = params.get('clusterId')
  if (clusterIdStr) {
    const clusterId = parseInt(clusterIdStr, 10)
    if (!isNaN(clusterId)) {
      setCurrentClusterId(clusterId)
    }
  }
}

/**
 * 清除当前集群ID
 */
export function clearCurrentClusterId() {
  setCurrentClusterId(null)
}

/**
 * 创建API请求的配置对象，自动包含集群ID请求头
 * @param config 基础配置对象
 * @param clusterId 可选的集群ID
 * @returns 包含集群ID请求头的配置对象
 */
export function createApiConfig<T extends { headers?: Record<string, string> }>(
  config: T = {} as T,
  clusterId?: number | null
): T {
  const clusterHeaders = getClusterIdHeader(clusterId)
  
  return {
    ...config,
    headers: {
      ...config.headers,
      ...clusterHeaders
    }
  }
}

// 导出常量供其他模块使用
export const CLUSTER_ID_STORAGE_KEY = 'currentClusterId'