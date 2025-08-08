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

// 苹果风格样式常量定义
const STYLES = {
  // 主按钮样式 - 苹果风格圆润设计
  mainButton: "flex items-center space-x-3 bg-gradient-to-r from-blue-500 to-blue-400 text-white font-medium shadow-lg shadow-blue-500/25 hover:shadow-xl hover:shadow-blue-500/30 rounded-2xl transition-all duration-300 ease-out hover:from-blue-600 hover:to-blue-500 hover:scale-105 px-6 py-3 backdrop-blur-sm",
  
  // 加载状态样式 - 柔和圆润
  loadingContainer: "flex items-center space-x-3 px-5 py-3 bg-gradient-to-r from-gray-50 to-slate-50 rounded-xl shadow-sm border-0 backdrop-blur-sm",
  loadingSpinner: "w-4 h-4 animate-spin rounded-full border-2 border-gray-200 border-t-blue-500",
  loadingText: "text-sm text-gray-700 font-medium",
  
  // 未选择状态样式 - 温和的警告风格
  unselectedContainer: "flex items-center space-x-3 px-5 py-3 bg-gradient-to-r from-amber-50/50 to-orange-50/50 rounded-xl shadow-sm border-0 backdrop-blur-sm",
  unselectedIcon: "w-4 h-4 text-amber-500",
  unselectedText: "text-sm text-amber-700 font-medium",
  unselectedButton: "ml-2 bg-gradient-to-r from-amber-400 to-orange-400 text-white hover:from-amber-500 hover:to-orange-500 transition-all duration-300 ease-out rounded-xl px-4 py-2 text-sm font-medium shadow-md hover:shadow-lg hover:scale-105",
  
  // 下拉菜单样式 - 苹果风格卡片
  dropdownContent: "w-80 bg-white/95 backdrop-blur-xl shadow-2xl shadow-gray-900/10 border-0 rounded-3xl p-2",
  dropdownHeader: "px-5 py-4 text-sm font-semibold text-gray-800 border-b border-gray-100/50 rounded-t-3xl",
  dropdownItem: "flex items-center justify-between p-4 cursor-pointer rounded-2xl mx-1 my-1 hover:bg-blue-50/80 transition-all duration-200 ease-out hover:scale-[1.02]",
  dropdownItemDanger: "p-4 text-red-500 cursor-pointer rounded-2xl mx-1 my-1 hover:bg-red-50/80 transition-all duration-200 ease-out",
  
  // 图标样式 - 更柔和的色彩
  serverIconK8s: "w-4 h-4 text-blue-500",
  serverIconTraditional: "w-4 h-4 text-emerald-500", 
  serverIconWhite: "w-4 h-4 text-white drop-shadow-sm",
  checkIcon: "w-4 h-4 text-emerald-500",
  chevronIcon: "ml-2 h-4 w-4 text-white drop-shadow-sm",
  
  // Badge样式 - 圆润设计
  k8sBadge: "text-xs ml-2 bg-blue-100/80 text-blue-700 px-2 py-1 rounded-full font-medium",
  k8sBadgeWhite: "text-xs ml-2 bg-white/25 text-white px-2 py-1 rounded-full font-medium backdrop-blur-sm"
}

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
          isK8s: ClusterTypeUtil.isKubernetes(ClusterTypeUtil.fromString(cluster.depType || 'PVM')),
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
      <div className={STYLES.loadingContainer}>
        <div className={STYLES.loadingSpinner}></div>
        <span className={STYLES.loadingText}>加载中...</span>
      </div>
    )
  }

  if (!hasCluster) {
    return (
      <div className={STYLES.unselectedContainer}>
        <AlertCircle className={STYLES.unselectedIcon} />
        <span className={STYLES.unselectedText}>未选择集群</span>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button className={STYLES.unselectedButton}>
              选择集群
              <ChevronDown className="ml-1 h-3 w-3" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className={STYLES.dropdownContent}>
            <div className={STYLES.dropdownHeader}>
              选择集群
            </div>
            {clusters.length > 0 ? (
              clusters.map((cluster) => (
                <DropdownMenuItem
                  key={cluster.id}
                  onClick={() => handleClusterSelect(cluster)}
                  className={STYLES.dropdownItem}
                >
                  <div className="flex items-center space-x-3">
                    <Server className={cluster.isK8s ? STYLES.serverIconK8s : STYLES.serverIconTraditional} />
                    <div>
                      <div className="font-medium">{cluster.name}</div>
                      <div className="text-xs text-gray-500">
                        {cluster.isK8s ? 'Kubernetes' : 'Traditional'}
                      </div>
                    </div>
                  </div>
                  {cluster.isK8s && (
                    <Badge className={STYLES.k8sBadge}>K8s</Badge>
                  )}
                </DropdownMenuItem>
              ))
            ) : (
              <DropdownMenuItem disabled className="p-3 text-gray-500 mx-1">
                暂无可用集群
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
        <Button className={STYLES.mainButton}>
          <Server className={STYLES.serverIconWhite} />
          <span className="font-medium">{currentCluster?.name}</span>
          {currentCluster?.isK8s && (
            <Badge className={STYLES.k8sBadgeWhite}>K8s</Badge>
          )}
          <ChevronDown className={STYLES.chevronIcon} />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className={STYLES.dropdownContent}>
        <div className={STYLES.dropdownHeader}>
          切换集群
        </div>
        {clusters.map((cluster) => (
          <DropdownMenuItem
            key={cluster.id}
            onClick={() => handleClusterSelect(cluster)}
            className={STYLES.dropdownItem}
          >
            <div className="flex items-center space-x-3">
              <Server className={cluster.isK8s ? STYLES.serverIconK8s : STYLES.serverIconTraditional} />
              <div>
                <div className="font-medium">{cluster.name}</div>
                <div className="text-xs text-gray-500">
                  {cluster.isK8s ? 'Kubernetes' : 'Traditional'}
                </div>
              </div>
            </div>
            <div className="flex items-center space-x-2">
              {cluster.isK8s && (
                <Badge className={STYLES.k8sBadge}>K8s</Badge>
              )}
              {currentCluster?.id === cluster.id && (
                <Check className={STYLES.checkIcon} />
              )}
            </div>
          </DropdownMenuItem>
        ))}
        <div className="border-t border-gray-100 mt-1">
          <DropdownMenuItem
            onClick={() => setCluster(null)}
            className={STYLES.dropdownItemDanger}
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