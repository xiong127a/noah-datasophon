"use client"

import { useEffect, useState, useCallback } from 'react'
import { Monitor, RotateCcw } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useCluster } from '@/hooks/useCluster'

interface OverviewTabProps {
  serviceId: string
  serviceName: string
}

export default function OverviewTab({ serviceId, serviceName }: OverviewTabProps) {
  const [dashboardUrl, setDashboardUrl] = useState<string>('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string>('')
  const { currentCluster } = useCluster()

  // 获取服务的Dashboard URL
  const fetchDashboardUrl = useCallback(async () => {
    if (!currentCluster || !serviceId) return
    
    setLoading(true)
    setError('')
    
    try {
      // 从localStorage获取menuData，这是Vue2项目中的逻辑
      const menuData = JSON.parse(localStorage.getItem('menuData') || '[]')
      const serviceManageMenu = menuData.find((item: any) => item.path === 'service-manage')
      
      if (serviceManageMenu && serviceManageMenu.children) {
        const serviceItem = serviceManageMenu.children.find((item: any) => 
          item.meta?.params?.serviceId === serviceId
        )
        
        if (serviceItem && serviceItem.meta?.obj?.dashboardUrl) {
          const url = serviceItem.meta.obj.dashboardUrl
          console.log('找到Dashboard URL:', url)
          setDashboardUrl(url)
        } else {
          setError('该服务暂无监控面板')
        }
      } else {
        setError('无法获取服务配置信息')
      }
    } catch (err) {
      console.error('获取Dashboard URL失败:', err)
      setError('获取监控面板地址失败')
    } finally {
      setLoading(false)
    }
  }, [currentCluster, serviceId])

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

  // 加载状态
  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">正在加载监控面板...</p>
        </div>
      </div>
    )
  }

  // 错误状态或无URL
  if (error || !dashboardUrl) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="text-center">
          <Monitor className="w-16 h-16 text-gray-400 mx-auto mb-4" />
          <h2 className="text-xl font-semibold text-gray-900 mb-2">
            {serviceName} 服务总览
          </h2>
          <p className="text-gray-600 mb-4">
            {error || '该服务暂无可用的监控面板'}
          </p>
          <Button 
            onClick={handleReload}
            variant="outline"
          >
            <RotateCcw className="w-4 h-4 mr-2" />
            重新加载
          </Button>
        </div>
      </div>
    )
  }

  // 显示iframe
  return (
    <div className="relative w-full h-full">
      <iframe
        src={dashboardUrl}
        className="w-full h-full border-none"
        style={{
          position: 'absolute',
          left: 0,
          top: 0,
          right: 0,
          bottom: 0,
          width: '100%',
          height: '100%'
        }}
        frameBorder="0"
        title={`${serviceName} 服务总览`}
      />
    </div>
  )
}
