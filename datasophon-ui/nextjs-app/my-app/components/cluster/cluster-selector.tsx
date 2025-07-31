"use client"

import React, { useState, useEffect } from 'react'
import { Check, ChevronDown, Server, AlertCircle } from 'lucide-react'
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Badge } from "@/components/ui/badge"
import { useCluster, ClusterInfo } from '@/hooks/useCluster'
import { clusterApi } from "@/lib/api"

const ClusterSelector: React.FC = () => {
  const { currentCluster, hasCluster, setCluster } = useCluster()
  const [clusters, setClusters] = useState<ClusterInfo[]>([])
  const [loading, setLoading] = useState(true)

  // 获取集群列表
  const fetchClusters = async () => {
    try {
      setLoading(true)
      const response = await clusterApi.info.runningList()
      
      if (response.data.code === 200) {
        const clusterData = response.data.data || []
        setClusters(clusterData.map((cluster: any) => ({
          id: cluster.id,
          name: cluster.clusterName,
          clusterName: cluster.clusterName,
          isK8s: cluster.depType === 'kubernetes',
        })))
      }
    } catch (error) {
      console.error('获取集群列表失败:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchClusters()
  }, [])

  const handleClusterSelect = (cluster: ClusterInfo) => {
    setCluster(cluster)
    // 刷新页面以更新数据
    window.location.reload()
  }

  if (loading) {
    return (
      <div className="flex items-center space-x-2 px-3 py-2 bg-gray-50 rounded-lg">
        <div className="w-4 h-4 animate-spin rounded-full border-2 border-gray-300 border-t-blue-600"></div>
        <span className="text-sm text-gray-600">加载中...</span>
      </div>
    )
  }

  if (!hasCluster) {
    return (
      <div className="flex items-center space-x-2 px-3 py-2 bg-yellow-50 border border-yellow-200 rounded-lg">
        <AlertCircle className="w-4 h-4 text-yellow-600" />
        <span className="text-sm text-yellow-800 font-medium">未选择集群</span>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="outline" size="sm" className="ml-2 h-7">
              选择集群
              <ChevronDown className="ml-1 h-3 w-3" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-64">
            {clusters.length > 0 ? (
              clusters.map((cluster) => (
                <DropdownMenuItem
                  key={cluster.id}
                  onClick={() => handleClusterSelect(cluster)}
                  className="flex items-center justify-between p-3 cursor-pointer"
                >
                  <div className="flex items-center space-x-3">
                    <Server className={`w-4 h-4 ${cluster.isK8s ? 'text-blue-600' : 'text-green-600'}`} />
                    <div>
                      <div className="font-medium">{cluster.name}</div>
                      <div className="text-xs text-gray-500">
                        {cluster.isK8s ? 'Kubernetes' : 'Traditional'}
                      </div>
                    </div>
                  </div>
                  {cluster.isK8s && (
                    <Badge variant="secondary" className="text-xs">K8s</Badge>
                  )}
                </DropdownMenuItem>
              ))
            ) : (
              <DropdownMenuItem disabled>
                <span className="text-gray-500">暂无可用集群</span>
              </DropdownMenuItem>
            )}
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    )
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="outline" className="flex items-center space-x-2 bg-white hover:bg-gray-50">
          <Server className={`w-4 h-4 ${currentCluster?.isK8s ? 'text-blue-600' : 'text-green-600'}`} />
          <span className="font-medium">{currentCluster?.name}</span>
          {currentCluster?.isK8s && (
            <Badge variant="secondary" className="text-xs ml-1">K8s</Badge>
          )}
          <ChevronDown className="ml-2 h-4 w-4" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-64">
        <div className="px-3 py-2 text-sm font-medium text-gray-700 border-b">
          切换集群
        </div>
        {clusters.map((cluster) => (
          <DropdownMenuItem
            key={cluster.id}
            onClick={() => handleClusterSelect(cluster)}
            className="flex items-center justify-between p-3 cursor-pointer"
          >
            <div className="flex items-center space-x-3">
              <Server className={`w-4 h-4 ${cluster.isK8s ? 'text-blue-600' : 'text-green-600'}`} />
              <div>
                <div className="font-medium">{cluster.name}</div>
                <div className="text-xs text-gray-500">
                  {cluster.isK8s ? 'Kubernetes' : 'Traditional'}
                </div>
              </div>
            </div>
            <div className="flex items-center space-x-2">
              {cluster.isK8s && (
                <Badge variant="secondary" className="text-xs">K8s</Badge>
              )}
              {currentCluster?.id === cluster.id && (
                <Check className="w-4 h-4 text-green-600" />
              )}
            </div>
          </DropdownMenuItem>
        ))}
        <div className="border-t">
          <DropdownMenuItem
            onClick={() => setCluster(null)}
            className="p-3 text-red-600 cursor-pointer"
          >
            <AlertCircle className="w-4 h-4 mr-2" />
            取消选择集群
          </DropdownMenuItem>
        </div>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}

export default ClusterSelector