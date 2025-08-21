"use client"

import { useEffect, useState, useCallback } from 'react'
import { Monitor, RotateCcw, ExternalLink } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useCluster } from '@/hooks/useCluster'
import { clusterApiV1 } from '@/lib/api-utils-v1'

interface OverviewTabProps {
  serviceId: string
  serviceName: string
}

export default function OverviewTab({ serviceId, serviceName }: OverviewTabProps) {
  const [dashboardUrl, setDashboardUrl] = useState<string>('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string>('')
  const { currentCluster } = useCluster()

  // 获取服务的Dashboard URL (完全按照Vue2逻辑实现)
  const fetchDashboardUrl = useCallback(async () => {
    if (!currentCluster || !serviceId) return
    
    setLoading(true)
    setError('')
    
    try {
      console.log('正在获取Dashboard URL...', { clusterId: currentCluster.id, serviceId, serviceName })
      
      let url = ''
      
      // 1. 首先从localStorage的menuData获取 (Vue2的主要逻辑)
      const menuData = JSON.parse(localStorage.getItem('menuData') || '[]')
      const serviceManageMenus = menuData.filter((item: any) => item.path === 'service-manage')
      
      if (serviceManageMenus.length > 0) {
        serviceManageMenus[0].children?.forEach((item: any) => {
          if (item.meta?.params?.serviceId === serviceId && item.meta?.obj?.dashboardUrl) {
            url = item.meta.obj.dashboardUrl
            console.log('从menuData找到Dashboard URL:', url)
          }
        })
      }
      
      // 2. 如果menuData中没有，尝试获取集群默认Dashboard (Vue2的fallback逻辑)
      if (!url && currentCluster?.id) {
        try {
          const response = await clusterApiV1.overview.getDashboardUrl(currentCluster.id)
          if (response.data) {
            url = response.data
            console.log('从API获取集群Dashboard URL:', url)
          }
        } catch (apiError) {
          console.warn('API获取Dashboard URL失败:', apiError)
        }
      }
      
      if (url) {
        setDashboardUrl(url)
      } else {
        console.warn('未找到任何可用的Dashboard URL')
        setError(`${serviceName} 服务暂无可用的监控面板`)
      }
    } catch (err) {
      console.error('获取Dashboard URL失败:', err)
      setError('获取监控面板地址失败，请稍后重试')
    } finally {
      setLoading(false)
    }
  }, [currentCluster, serviceId, serviceName])

  // 重新加载iframe
  const handleReload = () => {
    setDashboardUrl('')
    setTimeout(() => {
      fetchDashboardUrl()
    }, 100)
  }

  useEffect(() => {
    fetchDashboardUrl()
  }, [fetchDashboardUrl])

  // 加载状态 - 苹果风格
  if (loading) {
    return (
      <div className="h-full bg-gradient-to-br from-blue-50 via-white to-indigo-50 flex items-center justify-center">
        <div className="text-center">
          <div className="relative">
            <div className="animate-spin rounded-full h-12 w-12 border-4 border-blue-100 border-t-blue-600 mx-auto mb-6 shadow-lg"></div>
            <div className="absolute inset-0 rounded-full bg-gradient-to-r from-blue-600/20 to-indigo-600/20 animate-pulse"></div>
          </div>
          <h3 className="text-lg font-semibold text-gray-800 mb-2">{serviceName}</h3>
          <p className="text-gray-600">正在加载监控面板...</p>
        </div>
      </div>
    )
  }

  // 错误状态或无URL - 苹果风格
  if (error || !dashboardUrl) {
    return (
      <div className="h-full bg-gradient-to-br from-red-50 via-white to-orange-50 flex items-center justify-center p-8">
        <div className="max-w-md w-full">
          <div className="bg-white rounded-2xl shadow-xl p-8 border border-gray-100 text-center">
            <div className="w-16 h-16 bg-gradient-to-br from-gray-100 to-gray-200 rounded-full flex items-center justify-center mx-auto mb-6 shadow-md">
              <Monitor className="w-8 h-8 text-gray-500" />
            </div>
            <h2 className="text-xl font-bold text-gray-900 mb-2">
              {serviceName} 服务总览
            </h2>
            <p className="text-gray-600 mb-6 leading-relaxed">
              {error || '该服务暂无可用的监控面板'}
            </p>
            <div className="space-y-3">
              <Button 
                onClick={handleReload}
                className="w-full bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white shadow-lg"
              >
                <RotateCcw className="w-4 h-4 mr-2" />
                重新加载
              </Button>
              <p className="text-xs text-gray-500 mt-4">
                💡 提示：监控面板需要服务正常运行且配置正确
              </p>
            </div>
          </div>
        </div>
      </div>
    )
  }

  // 显示iframe - 简洁版本，无工具栏
  return (
    <div className="relative w-full h-full bg-white rounded-lg shadow-sm overflow-hidden">
      {/* iframe内容区 - 全屏显示 */}
      <iframe
        src={dashboardUrl}
        className="w-full h-full border-none"
        frameBorder="0"
        title={`${serviceName} 服务总览`}
        allow="fullscreen"
      />
    </div>
  )
}
