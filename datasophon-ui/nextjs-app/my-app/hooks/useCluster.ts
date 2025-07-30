"use client"

import { useState, useEffect } from 'react'

export interface ClusterInfo {
  id: number
  name: string
  clusterName: string
  isK8s?: boolean
  depType?: string
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

    return {
      id: Number(clusterId),
      name: clusterName,
      clusterName: clusterName,
      // 这里可以从localStorage获取更多集群详细信息
    }
  }

  // 设置当前集群
  const setCluster = (cluster: ClusterInfo | null) => {
    if (typeof window === 'undefined') return

    if (cluster) {
      localStorage.setItem('clusterId', cluster.id.toString())
      localStorage.setItem('clusterName', cluster.clusterName)
      localStorage.setItem('current_cluster_id', cluster.id.toString())
      localStorage.setItem('current_cluster_name', cluster.clusterName)
      localStorage.setItem('isCluster', 'isCluster')
      setCurrentCluster(cluster)
      setHasCluster(true)
    } else {
      localStorage.removeItem('clusterId')
      localStorage.removeItem('clusterName')
      localStorage.removeItem('current_cluster_id')
      localStorage.removeItem('current_cluster_name')
      localStorage.removeItem('isCluster')
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