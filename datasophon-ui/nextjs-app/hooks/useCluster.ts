"use client"

import { useState, useEffect } from 'react'

export interface ClusterInfo {
  id: string
  name: string
  clusterName: string
  isK8s?: boolean
  depType?: string
  clusterState?: number // 修正：后端返回的是数字类型
  clusterStateCode?: number // 保留向后兼容，但后端目前返回null
}

export const useCluster = () => {
  const [currentCluster, setCurrentCluster] = useState<ClusterInfo | null>(null)
  const [hasCluster, setHasCluster] = useState(false)
  const [loading, setLoading] = useState(true)

  // 获取当前集群信息
  const getCurrentCluster = (): ClusterInfo | null => {
    if (typeof window === 'undefined') return null
    
    const clusterId = localStorage.getItem('clusterId')
    const clusterName = localStorage.getItem('clusterName') || localStorage.getItem('current_cluster_name')
    
    if (!clusterId || clusterId === '-1' || !clusterName) {
      return null
    }

    // 尝试获取完整的集群信息
    const depType = localStorage.getItem('clusterDepType')
    const isK8sStr = localStorage.getItem('clusterIsK8s')
    const clusterStateStr = localStorage.getItem('clusterState')

    return {
      id: clusterId,
      name: clusterName,
      clusterName: clusterName,
      depType: depType || undefined,
      isK8s: isK8sStr === 'true',
      clusterState: clusterStateStr ? parseInt(clusterStateStr) : undefined,
    }
  }

  // 设置当前集群
  const setCluster = (cluster: ClusterInfo | null) => {
    if (typeof window === 'undefined') return

    if (cluster) {
      // 保存基本信息（保持向后兼容）
      localStorage.setItem('clusterId', cluster.id.toString())
      localStorage.setItem('clusterName', cluster.clusterName)
      localStorage.setItem('current_cluster_id', cluster.id.toString())
      localStorage.setItem('current_cluster_name', cluster.clusterName)
      localStorage.setItem('isCluster', 'isCluster')
      
      // 保存扩展信息，用于图标显示等
      if (cluster.depType) {
        localStorage.setItem('clusterDepType', cluster.depType)
      } else {
        localStorage.removeItem('clusterDepType')
      }
      
      if (cluster.isK8s !== undefined) {
        localStorage.setItem('clusterIsK8s', cluster.isK8s.toString())
      } else {
        localStorage.removeItem('clusterIsK8s')
      }
      
      if (cluster.clusterState !== undefined) {
        localStorage.setItem('clusterState', cluster.clusterState.toString())
      } else {
        localStorage.removeItem('clusterState')
      }
      
      setCurrentCluster(cluster)
      setHasCluster(true)
    } else {
      // 清除所有集群相关信息
      localStorage.removeItem('clusterId')
      localStorage.removeItem('clusterName')
      localStorage.removeItem('current_cluster_id')
      localStorage.removeItem('current_cluster_name')
      localStorage.removeItem('isCluster')
      localStorage.removeItem('clusterDepType')
      localStorage.removeItem('clusterIsK8s')
      localStorage.removeItem('clusterState')
      setCurrentCluster(null)
      setHasCluster(false)
    }
  }

  // 检查是否需要集群权限
  const requiresCluster = (path: string): boolean => {
    const clusterRequiredPaths = [
      '/hosts',
      '/alerts/notification-groups',
      '/alerts/alert-groups', 
      '/alerts/metrics',
      '/system/tenants',
      '/system/racks',
      '/system/tags',
      '/system/audit',
      // 注意：集群存储库和框架是全局配置，不需要集群权限
      // '/clusters/storage',
      // '/clusters/framework',
    ]
    
    return clusterRequiredPaths.some(requiredPath => path.startsWith(requiredPath))
  }

  // 初始化集群状态
  useEffect(() => {
    const cluster = getCurrentCluster()
    setCurrentCluster(cluster)
    setHasCluster(!!cluster)
    setLoading(false)
  }, [])

  return {
    currentCluster,
    hasCluster,
    loading,
    setCluster,
    requiresCluster,
    getCurrentCluster,
  }
}

export default useCluster