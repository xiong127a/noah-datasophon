"use client"

import React, { useState, useEffect } from 'react'
import { Check, ChevronDown, AlertCircle, BarChart3 } from 'lucide-react'
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
import { ClusterTypeUtil } from '@/types/cluster-type'
import Image from 'next/image'

// 集群图标组件 - 使用本地SVG图标，与集群列表保持一致
const ClusterIcon: React.FC<{ isK8s: boolean; className?: string }> = ({ isK8s, className = "w-4 h-4" }) => {
  const iconPath = isK8s 
    ? "/images/cluster/kubernetes-logo.svg" 
    : "/images/cluster/linux-tux.svg"
  
  return (
    <Image 
      src={iconPath}
      alt={isK8s ? 'Kubernetes' : 'Linux'}
      width={16}
      height={16}
      className={className}
    />
  )
}

// 紧凑型统计集成设计的样式常量
const STYLES = {
  // 主按钮样式 - 紧凑型设计，集成统计信息
  mainButton: "flex items-center space-x-3 bg-white/90 backdrop-blur-sm border border-slate-200/50 text-slate-700 font-medium shadow-sm hover:shadow-md rounded-2xl transition-all duration-200 ease-out hover:bg-white hover:border-slate-300/60 px-4 py-2 min-w-[200px]",
  
  // 统计信息容器
  statsContainer: "flex items-center space-x-4 ml-3 pl-3 border-l border-slate-200/60",
  statItem: "flex flex-col items-center",
  statNumber: "text-xs font-semibold leading-none",
  statLabel: "text-[10px] text-slate-500 leading-none mt-0.5",
  
  // 加载状态样式
  loadingContainer: "flex items-center space-x-2 px-4 py-2 bg-white/90 backdrop-blur-sm border border-slate-200/50 rounded-2xl shadow-sm min-w-[200px]",
  loadingSpinner: "w-3 h-3 animate-spin rounded-full border-2 border-slate-200 border-t-blue-500",
  loadingText: "text-sm text-slate-600 font-medium",
  
  // 未选择状态样式 - 紧凑化
  unselectedContainer: "flex items-center space-x-2 px-4 py-2 bg-amber-50/50 backdrop-blur-sm border border-amber-200/40 rounded-2xl shadow-sm min-w-[200px]",
  unselectedIcon: "w-3 h-3 text-amber-500",
  unselectedText: "text-sm text-amber-700 font-medium",
  unselectedButton: "ml-2 bg-amber-400 hover:bg-amber-500 text-white transition-all duration-200 rounded-xl px-3 py-1 text-xs font-medium shadow-sm hover:shadow-md",
  
  // 下拉菜单样式 - 现代化卡片设计
  dropdownContent: "w-80 bg-white/98 backdrop-blur-xl shadow-2xl border border-slate-200/40 rounded-3xl p-3 animate-in fade-in-0 zoom-in-95 duration-200",
  dropdownHeader: "flex items-center justify-between px-4 py-3 text-sm font-semibold text-slate-800 border-b border-slate-100/80 mb-2",
  dropdownStats: "flex items-center space-x-1 text-xs text-slate-500",
  dropdownItem: "flex items-center justify-between p-3 cursor-pointer rounded-2xl hover:bg-slate-50/80 transition-all duration-150 ease-out",
  dropdownItemActive: "flex items-center justify-between p-3 cursor-pointer rounded-2xl bg-blue-50/60 border border-blue-200/40",
  dropdownItemDanger: "p-3 text-red-500 cursor-pointer rounded-2xl hover:bg-red-50/80 transition-all duration-150 ease-out mt-2 border-t border-slate-100",
  
  // 图标样式 - 现代化色调
  checkIcon: "w-4 h-4 text-emerald-500",
  chevronIcon: "ml-2 h-3 w-3 text-slate-400 transition-transform duration-200",
  
  // Badge样式 - 精致设计
  k8sBadge: "text-[10px] ml-2 bg-blue-100/90 text-blue-700 px-1.5 py-0.5 rounded-md font-medium",
  totalClusters: "text-blue-600",
  runningClusters: "text-emerald-600"
}

const ClusterSelector: React.FC = () => {
  const { currentCluster, hasCluster, setCluster } = useCluster()
  const [clusters, setClusters] = useState<ClusterInfo[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string>('')

  // 获取集群列表并处理统计
  const fetchClusters = async () => {
    try {
      setLoading(true)
      setError('')
      const response = await clusterApi.info.runningList()
      
      if (response.data.code === 200) {
        const clusterData = response.data.data || []
        
        interface RawClusterData {
          id: string
          clusterName?: string
          depType?: string
          clusterState?: number // 后端返回的是数字：3表示运行中
          clusterStateCode?: number // 后端目前返回null
        }
        
        const processedClusters = clusterData.map((cluster: RawClusterData) => {
          try {
            return {
              id: cluster.id,
              name: cluster.clusterName || `集群-${cluster.id}`,
              clusterName: cluster.clusterName || `集群-${cluster.id}`,
              isK8s: cluster.depType ? ClusterTypeUtil.isKubernetes(cluster.depType) : false,
              depType: cluster.depType || 'PVM',
              clusterState: cluster.clusterState,
              clusterStateCode: cluster.clusterStateCode
            } as ClusterInfo
          } catch (typeError) {
            console.warn('处理集群类型时出错:', typeError, cluster)
            return {
              id: cluster.id,
              name: cluster.clusterName || `集群-${cluster.id}`,
              clusterName: cluster.clusterName || `集群-${cluster.id}`,
              isK8s: false,
              depType: 'PVM',
              clusterState: cluster.clusterState,
              clusterStateCode: cluster.clusterStateCode
            } as ClusterInfo
          }
        })
        setClusters(processedClusters)
      } else {
        setError(response.data.msg || '获取集群列表失败')
      }
    } catch (error: unknown) {
      console.error('获取集群列表失败:', error)
      const errorMessage = error instanceof Error 
        ? error.message 
        : (error as { response?: { data?: { msg?: string } }; message?: string })?.response?.data?.msg 
          || (error as { message?: string })?.message 
          || '网络请求失败，请重试'
      setError(errorMessage)
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

  // 计算统计数据
  const totalClusters = clusters.length
  const runningClusters = clusters.filter(c => 
    c.clusterState === 3 || c.clusterStateCode === 3
  ).length

  // 加载状态
  if (loading) {
    return (
      <div className={STYLES.loadingContainer}>
        <div className={STYLES.loadingSpinner}></div>
        <span className={STYLES.loadingText}>加载中...</span>
      </div>
    )
  }

  // 错误状态
  if (error) {
    return (
      <div className={STYLES.unselectedContainer}>
        <AlertCircle className={STYLES.unselectedIcon} />
        <span className={STYLES.unselectedText}>加载失败</span>
        <Button 
          className={STYLES.unselectedButton} 
          onClick={fetchClusters}
        >
          重试
        </Button>
      </div>
    )
  }

  // 未选择状态
  if (!hasCluster) {
    return (
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <div className={STYLES.unselectedContainer}>
            <AlertCircle className={STYLES.unselectedIcon} />
            <span className={STYLES.unselectedText}>未选择集群</span>
            {totalClusters > 0 && (
              <div className={STYLES.statsContainer}>
                <div className={STYLES.statItem}>
                  <span className={`${STYLES.statNumber} ${STYLES.totalClusters}`}>{totalClusters}</span>
                  <span className={STYLES.statLabel}>总数</span>
                </div>
                <div className={STYLES.statItem}>
                  <span className={`${STYLES.statNumber} ${STYLES.runningClusters}`}>{runningClusters}</span>
                  <span className={STYLES.statLabel}>运行</span>
                </div>
              </div>
            )}
            <ChevronDown className={STYLES.chevronIcon} />
          </div>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className={STYLES.dropdownContent}>
          <div className={STYLES.dropdownHeader}>
            <span>选择集群</span>
            <div className={STYLES.dropdownStats}>
              <BarChart3 className="w-3 h-3 mr-1" />
              <span>{totalClusters} 个可用</span>
            </div>
          </div>
          {clusters.length > 0 ? (
            clusters.map((cluster) => (
              <DropdownMenuItem
                key={cluster.id}
                onClick={() => handleClusterSelect(cluster)}
                className={STYLES.dropdownItem}
              >
                <div className="flex items-center space-x-3">
                  <ClusterIcon isK8s={cluster.isK8s || false} />
                  <div>
                    <div className="font-medium text-sm">{cluster.name}</div>
                    <div className="text-xs text-slate-500">
                      {cluster.isK8s ? 'Kubernetes' : '物理/虚拟机'}
                    </div>
                  </div>
                </div>
                <div className="flex items-center space-x-1">
                  {(cluster.clusterState === 3 || cluster.clusterStateCode === 3) && (
                    <div className="w-2 h-2 bg-emerald-400 rounded-full"></div>
                  )}
                  {cluster.isK8s && (
                    <Badge className={STYLES.k8sBadge}>K8s</Badge>
                  )}
                </div>
              </DropdownMenuItem>
            ))
          ) : (
            <DropdownMenuItem disabled className="p-3 text-slate-400 text-sm">
              暂无可用集群
            </DropdownMenuItem>
          )}
        </DropdownMenuContent>
      </DropdownMenu>
    )
  }

  // 已选择状态 - 紧凑型设计
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button className={STYLES.mainButton}>
          <ClusterIcon isK8s={currentCluster?.isK8s || false} />
          <span className="font-medium truncate max-w-[80px]">{currentCluster?.name}</span>
          {currentCluster?.isK8s && (
            <Badge className={STYLES.k8sBadge}>K8s</Badge>
          )}
          
          {/* 集成统计信息 */}
          <div className={STYLES.statsContainer}>
            <div className={STYLES.statItem}>
              <span className={`${STYLES.statNumber} ${STYLES.totalClusters}`}>{totalClusters}</span>
              <span className={STYLES.statLabel}>总数</span>
            </div>
            <div className={STYLES.statItem}>
              <span className={`${STYLES.statNumber} ${STYLES.runningClusters}`}>{runningClusters}</span>
              <span className={STYLES.statLabel}>运行</span>
            </div>
          </div>
          
          <ChevronDown className={STYLES.chevronIcon} />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className={STYLES.dropdownContent}>
        <div className={STYLES.dropdownHeader}>
          <span>切换集群</span>
          <div className={STYLES.dropdownStats}>
            <BarChart3 className="w-3 h-3 mr-1" />
            <span>{runningClusters}/{totalClusters} 运行中</span>
          </div>
        </div>
        {clusters.map((cluster) => (
          <DropdownMenuItem
            key={cluster.id}
            onClick={() => handleClusterSelect(cluster)}
            className={currentCluster?.id === cluster.id ? STYLES.dropdownItemActive : STYLES.dropdownItem}
          >
            <div className="flex items-center space-x-3">
              <ClusterIcon isK8s={cluster.isK8s || false} />
              <div>
                <div className="font-medium text-sm">{cluster.name}</div>
                <div className="text-xs text-slate-500">
                  {cluster.isK8s ? 'Kubernetes' : '物理/虚拟机'} 
                  {(cluster.clusterState === 3 || cluster.clusterStateCode === 3) && <span className="text-emerald-500 ml-1">● 运行中</span>}
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
        <div className={STYLES.dropdownItemDanger}>
          <DropdownMenuItem
            onClick={() => setCluster(null)}
            className="flex items-center text-red-500 hover:bg-transparent"
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