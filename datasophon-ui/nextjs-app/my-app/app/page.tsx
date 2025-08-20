"use client"

import { useEffect, useState, useCallback } from 'react'
import { 
  Server, 
  Monitor,
  Pause,
  Play,
  RotateCcw,
  Trash2,
  MoreHorizontal,
  ChevronDown,
  Plus,
  Users,
  Settings,
  Info,
  BookOpen,
  Link,
  BarChart3
} from 'lucide-react'
import { Button } from "@/components/ui/button"
import { useCluster } from '@/hooks/useCluster'
import FinalNavbar from '@/components/layout/navbar-final'
import { clusterApiV1 } from '@/lib/api-utils-v1'
import { createClusterHeaders } from '@/lib/cluster-id-header'
import { SvgIcon } from '@/components/ui/svg-icon'

// 导入页签组件
import OverviewTab from '@/components/service-tabs/overview-tab'
import InstancesTab from '@/components/service-tabs/instances-tab'
import ConfigTab from '@/components/service-tabs/config-tab'
import ConnectionTab from '@/components/service-tabs/connection-tab'
import IntroTab from '@/components/service-tabs/intro-tab'
import GuideTab from '@/components/service-tabs/guide-tab'
import QueueTab from '@/components/service-tabs/queue-tab'

// 导入工具函数
import { hasOverviewTab, hasConnectionTab } from '@/components/service-tabs/utils/service-tab-utils'

// 服务状态枚举
enum ServiceState {
  WAIT_INSTALL = 1,
  RUNNING = 2,
  EXISTS_ALARM = 3,
  EXISTS_EXCEPTION = 4
}

// 服务数据接口 - 按Vue2项目结构定义
interface ServiceItem {
  id: string
  name: string
  serviceName: string
  icon?: string
  serviceId: string
  path: string
  serviceStateCode: ServiceState
  alertNum: number
  needRestart: boolean
  rawData: Record<string, unknown>
  menuVisible: boolean
}

// 集群数据接口
interface ClusterData {
  id: string
  clusterName: string
  clusterFrame?: string
  depType?: string
  clusterState?: string | number
}

// 服务详情页签组件
interface ServiceDetailTabsProps {
  service: ServiceItem
}

function ServiceDetailTabs({ service }: ServiceDetailTabsProps) {
  // 根据服务类型决定可用的页签
  const getAvailableTabs = useCallback(() => {
    const baseTabs = []
    
    // 检查是否有总览页签（基于Vue2项目逻辑）
    const showOverview = hasOverviewTab(service.serviceId)
    if (showOverview) {
      baseTabs.push({ key: 'overview', label: '总览', icon: BarChart3 })
    }
    
    // 基础页签
    baseTabs.push(
      { key: 'instances', label: '实例', icon: Server },
      { key: 'config', label: '配置', icon: Settings }
    )
    
    // 连接信息页签（只有部分服务支持）
    if (hasConnectionTab(service.serviceName)) {
      baseTabs.push({ key: 'connection', label: '连接信息', icon: Link })
    }
    
    // 帮助页签
    baseTabs.push(
      { key: 'intro', label: '组件介绍', icon: Info },
      { key: 'guide', label: '用户指南', icon: BookOpen }
    )
    
    // YARN服务额外有资源配置页签
    if (service.serviceName === 'YARN') {
      baseTabs.push({ key: 'queue', label: '资源配置', icon: Monitor })
    }
    
    return baseTabs
  }, [service.serviceId, service.serviceName])
  
  const tabs = getAvailableTabs()
  
  // 默认选中第一个可用页签（与Vue2逻辑一致：有总览选总览，没有选实例）
  const [activeTab, setActiveTab] = useState(tabs[0]?.key || 'instances')
  
  // 当服务切换时，重置页签
  useEffect(() => {
    const newTabs = getAvailableTabs()
    if (newTabs.length > 0) {
      setActiveTab(newTabs[0].key)
    }
  }, [service.serviceId, service.serviceName, getAvailableTabs])
  
  // 渲染页签内容
  const renderTabContent = () => {
    const commonProps = {
      serviceId: service.serviceId,
      serviceName: service.name
    }

    switch (activeTab) {
      case 'overview':
        return <OverviewTab {...commonProps} />
      case 'instances':
        return <InstancesTab {...commonProps} />
      case 'config':
        return <ConfigTab {...commonProps} />
      case 'connection':
        return <ConnectionTab {...commonProps} />
      case 'intro':
        return <IntroTab {...commonProps} />
      case 'guide':
        return <GuideTab {...commonProps} />
      case 'queue':
        return <QueueTab {...commonProps} />
      default:
        return null
    }
  }

  return (
    <div className="h-full flex flex-col">
      {/* 页签头部 */}
      <div className="border-b border-gray-200 bg-white px-6">
        <div className="flex items-center justify-between py-4">
          <h1 className="text-xl font-semibold text-gray-900">{service.name} 服务管理</h1>
          <div className="flex items-center space-x-2">
            <Button variant="outline" size="sm">
              <Link className="w-4 h-4 mr-2" />
              WebUI
            </Button>
          </div>
        </div>
        <div className="flex space-x-8">
          {tabs.map(tab => {
            const Icon = tab.icon
            return (
              <button
                key={tab.key}
                className={`flex items-center space-x-2 px-1 py-4 border-b-2 font-medium text-sm transition-colors ${
                  activeTab === tab.key
                    ? 'border-blue-500 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
                onClick={() => setActiveTab(tab.key)}
              >
                <Icon className="w-4 h-4" />
                <span>{tab.label}</span>
              </button>
            )
          })}
        </div>
      </div>
      
      {/* 页签内容区域 */}
      <div className={`flex-1 ${activeTab === 'overview' ? 'overflow-hidden' : 'overflow-y-auto bg-gray-50'}`}>
        {renderTabContent()}
      </div>
    </div>
  )
}

export default function ServiceLayout() {
  const { currentCluster, hasCluster, loading } = useCluster()

  // 服务列表状态 - 按Vue2实现
  const [services, setServices] = useState<ServiceItem[]>([])
  const [serviceLoading, setServiceLoading] = useState(false)
  const [clusterData, setClusterData] = useState<ClusterData | null>(null)

  // 选中的服务状态
  const [selectedService, setSelectedService] = useState<ServiceItem | null>(null)
  
  // 服务操作菜单状态
  const [showActionMenu, setShowActionMenu] = useState(false)
  const [actionMenuPosition, setActionMenuPosition] = useState({ top: 0, left: 0 })
  
  // 总服务选项菜单状态
  const [showServiceOptionMenu, setShowServiceOptionMenu] = useState(false)
  const [serviceOptionMenuPosition, setServiceOptionMenuPosition] = useState({ top: 0, left: 0 })
  
  // 集群总览状态
  const [dashboardLoading, setDashboardLoading] = useState(false)
  
  // 区分主页和大数据基础平台的URL
  const [mainDashboardUrl, setMainDashboardUrl] = useState<string>('')
  const [datasophonDashboardUrl, setDatasophonDashboardUrl] = useState<string>('')
  
  // 分组折叠状态
  const [businessServicesExpanded, setBusinessServicesExpanded] = useState(true) // 服务组件默认展开
  const [systemServicesExpanded, setSystemServicesExpanded] = useState(true) // 系统基础服务默认展开

  // 管理服务列表 - 按Vue2项目定义
  const managementServiceNames = ['PROMETHEUS', 'GRAFANA', 'ALERTMANAGER', 'DATASOPHON']

  // 获取集群信息
  const getClusterInfo = useCallback(async () => {
    if (!hasCluster || !currentCluster) return
    
    try {
      // 优先从运行中集群列表获取
      const response = await clusterApiV1.info.runningList()
      
      if (response.data.code === 200 && response.data.data && response.data.data.length > 0) {
        const clusterList = response.data.data
        const cluster = clusterList.find((c: Record<string, unknown>) => c.id === currentCluster.id)
        
        if (cluster) {
          setClusterData({
            id: cluster.id as string,
            clusterName: cluster.clusterName as string,
            clusterFrame: cluster.clusterFrame as string,
            depType: cluster.depType as string,
            clusterState: cluster.clusterState as string | number
          })
        }
      }
    } catch (error) {
      console.error('获取集群信息失败:', error)
    }
  }, [hasCluster, currentCluster])

  // 获取服务列表 - 按Vue2实现
  const fetchServices = useCallback(async () => {
    if (!hasCluster || !currentCluster) return

    setServiceLoading(true)
    try {
      const config = createClusterHeaders(currentCluster.id)
      const response = await clusterApiV1.service.list(config)
      
      if (response.data.code === 200 && response.data.data) {
        const apiServices = response.data.data
        
        // 处理服务数据，转换为前端需要的格式
        const processedServices: ServiceItem[] = apiServices.map((service: Record<string, unknown>, index: number) => ({
          id: service.id?.toString() || (index + 1).toString(),
          name: (service.label || service.serviceName) as string,
          serviceName: service.serviceName as string,
          icon: (typeof service.serviceName === 'string' ? service.serviceName : '').toLowerCase(),
          serviceId: service.id?.toString() || (index + 1).toString(),
          path: `/service-manage/service-list/${service.id}`,
          serviceStateCode: (service.serviceStateCode as ServiceState) || ServiceState.WAIT_INSTALL,
          alertNum: (service.alertNum as number) || 0,
          needRestart: (service.needRestart as boolean) || false,
          rawData: service,
          menuVisible: false
        }))
        
        setServices(processedServices)
      } else {
        console.warn('API返回数据格式异常，服务列表为空')
        setServices([])
      }
    } catch (error) {
      console.error('获取服务列表失败:', error)
      setServices([])
    } finally {
      setServiceLoading(false)
    }
  }, [hasCluster, currentCluster])

  // 计算服务分组 - 按Vue2实现
  const coreServices = services.filter(service => {
    const serviceNameForFilter = service.serviceName || service.name
    return !managementServiceNames.includes(serviceNameForFilter.toUpperCase())
  })

  // 计算管理服务分组，包含固定的"大数据基础平台"服务 + 后端真实数据
  const managementServices = (() => {
    // 固定的"大数据基础平台"服务 - 代表系统本身，不是模拟数据
    const datasophonService: ServiceItem = {
      id: 'datasophon',
      name: '大数据基础平台',
      serviceName: 'DATASOPHON',
      icon: 'datasophon',
      serviceId: 'datasophon',
      path: '/overview',
      serviceStateCode: ServiceState.RUNNING,
      alertNum: 0,
      needRestart: false,
      rawData: {},
      menuVisible: false
    }
    
    // 从后端获取的管理服务
    const backendManagementServices = services.filter(service => {
      const serviceNameForFilter = service.serviceName || service.name
      return managementServiceNames.includes(serviceNameForFilter.toUpperCase())
    })
    
    return [datasophonService, ...backendManagementServices]
  })()

  // 获取服务状态点样式
  const getServiceStatusInfo = (stateCode: ServiceState) => {
    switch (stateCode) {
      case ServiceState.RUNNING:
        return { 
          dotClassName: 'bg-green-500 animate-pulse'
        }
      case ServiceState.EXISTS_ALARM:
        return { 
          dotClassName: 'bg-amber-500 animate-pulse'
        }
      case ServiceState.EXISTS_EXCEPTION:
        return { 
          dotClassName: 'bg-red-500 animate-pulse'
        }
      default:
        return { 
          dotClassName: 'bg-gray-400'
        }
    }
  }

  // 获取主页集群总览URL
  const getMainDashboardUrl = useCallback(async () => {
    if (!hasCluster || !currentCluster) return

    try {
      const config = createClusterHeaders(currentCluster.id)
      const response = await clusterApiV1.overview.getDashboardUrl(currentCluster.id, config)
      
      if (response.data && response.data.code === 200) {
        setMainDashboardUrl(response.data.data)
        console.log('获取主页集群总览URL成功:', response.data.data)
      } else {
        console.error('获取主页集群总览URL失败:', response.data?.msg)
      }
    } catch (error) {
      console.error('获取主页集群总览URL失败:', error)
    }
  }, [hasCluster, currentCluster])

  // 获取大数据基础平台总览URL
  const getDatasophonDashboardUrl = useCallback(async () => {
    if (!hasCluster || !currentCluster) return

    try {
      const config = createClusterHeaders(currentCluster.id)
      const response = await clusterApiV1.overview.getDatasophonDashboard(currentCluster.id, config)
      
      if (response.data && response.data.code === 200) {
        setDatasophonDashboardUrl(response.data.data)
        console.log('获取大数据基础平台总览URL成功:', response.data.data)
      } else {
        console.error('获取大数据基础平台总览URL失败:', response.data?.msg)
      }
    } catch (error) {
      console.error('获取大数据基础平台总览URL失败:', error)
    }
  }, [hasCluster, currentCluster])

  // 加载Dashboard数据
  const loadDashboardData = useCallback(async () => {
    if (!hasCluster || !currentCluster) return

    setDashboardLoading(true)
    try {
      // 并行获取两个URL
      await Promise.all([
        getMainDashboardUrl(),
        getDatasophonDashboardUrl()
      ])
    } finally {
      setDashboardLoading(false)
    }
  }, [hasCluster, currentCluster, getMainDashboardUrl, getDatasophonDashboardUrl])

  // 处理服务项点击
  const handleServiceItemClick = (service: ServiceItem) => {
    setSelectedService(service)
  }

  // 处理服务操作菜单点击
  const handleServiceActionClick = (event: React.MouseEvent) => {
    event.stopPropagation()
    // 使用鼠标位置而不是按钮位置
    setActionMenuPosition({
      top: event.clientY + window.scrollY + 5, // 鼠标下方5px
      left: event.clientX + window.scrollX + 5  // 鼠标右侧5px
    })
    setShowActionMenu(true)
  }

  // 处理总服务选项点击
  const handleServiceOptionClick = (event: React.MouseEvent) => {
    event.stopPropagation()
    const rect = event.currentTarget.getBoundingClientRect()
    // 使用按钮位置，菜单出现在按钮下方右对齐
    setServiceOptionMenuPosition({
      top: rect.bottom + window.scrollY + 2, // 按钮下方2px
      left: rect.left + window.scrollX  // 从按钮左边界开始
    })
    setShowServiceOptionMenu(true)
  }

  // 点击外部区域关闭菜单
  const handleClickOutside = useCallback((event: MouseEvent) => {
    const target = event.target as HTMLElement
    if (!target.closest('.action-menu') && !target.closest('.action-trigger')) {
      setShowActionMenu(false)
    }
    if (!target.closest('.service-option-menu') && !target.closest('.service-option-trigger')) {
      setShowServiceOptionMenu(false)
    }
  }, [])

  useEffect(() => {
    document.addEventListener('click', handleClickOutside)
    return () => {
      document.removeEventListener('click', handleClickOutside)
    }
  }, [handleClickOutside])

  // 初始化数据
  useEffect(() => {
    if (hasCluster && currentCluster) {
      getClusterInfo()
      fetchServices()
      loadDashboardData()
    } else {
      setServices([])
      setClusterData(null)
      setSelectedService(null)
      setMainDashboardUrl('')
      setDatasophonDashboardUrl('')
    }
  }, [hasCluster, currentCluster, getClusterInfo, fetchServices, loadDashboardData])

  // 如果没有选择集群，显示空状态
  if (loading) {
  return (
      <div className="min-h-screen bg-gray-50">
        <FinalNavbar />
        <div className="flex items-center justify-center h-96">
      <div className="text-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
        <p className="text-gray-600">正在加载...</p>
      </div>
        </div>
      </div>
    )
  }

  if (!hasCluster) {
    return (
      <div className="min-h-screen bg-gray-50">
        <FinalNavbar />
        <div className="flex items-center justify-center h-96">
          <div className="text-center">
            <Server className="w-16 h-16 text-gray-400 mx-auto mb-4" />
            <h2 className="text-xl font-semibold text-gray-900 mb-2">请选择集群</h2>
            <p className="text-gray-600">请在右上角选择一个集群来开始管理您的大数据服务</p>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <FinalNavbar />
      
      {/* 主内容区域 */}
      <div className="flex flex-1">
        {/* 左侧服务列表 */}
        <div className="w-72 bg-white shadow-lg border-r border-gray-200/60 min-h-screen">
          {/* 集群信息头部 */}
          <div className="relative p-4 border-b border-gray-200/60 bg-gradient-to-br from-slate-50 via-blue-50/30 to-indigo-50/40 overflow-hidden">
            {/* 装饰性背景元素 */}
            <div className="absolute top-0 right-0 w-20 h-20 bg-gradient-to-br from-blue-100/20 to-indigo-100/30 rounded-full -translate-y-6 translate-x-6 blur-xl"></div>
            <div className="absolute bottom-0 left-0 w-16 h-16 bg-gradient-to-tr from-slate-100/30 to-blue-100/20 rounded-full translate-y-4 -translate-x-4 blur-lg"></div>
            
            <div className="relative flex items-center justify-between">
              <div className="flex-1 min-w-0">
                <div className="flex items-center space-x-2.5 mb-2">
                  <div className="relative">
                    <div className="w-2.5 h-2.5 bg-gradient-to-r from-emerald-400 to-green-500 rounded-full animate-pulse shadow-sm"></div>
                    <div className="absolute inset-0 w-2.5 h-2.5 bg-emerald-400/60 rounded-full animate-ping"></div>
                  </div>
                  <h3 className="font-bold text-gray-900 text-sm truncate tracking-tight">{clusterData?.clusterName || currentCluster?.name}</h3>
                </div>
                <div className="flex items-center space-x-2">
                  <div className="flex items-center space-x-1.5 bg-white/70 backdrop-blur-sm px-2.5 py-1 rounded-lg shadow-sm border border-white/50">
                    <div className="w-1.5 h-1.5 bg-blue-500 rounded-full"></div>
                    <span className="text-xs font-semibold text-gray-700">
                      {clusterData?.clusterFrame}
                    </span>
                  </div>
                  <div className={`px-2.5 py-1 rounded-lg text-xs font-bold shadow-sm border transition-all duration-200 ${
                    clusterData?.depType === 'KUBERNETES' 
                      ? 'bg-gradient-to-r from-blue-500 to-indigo-600 text-white border-blue-400/20 shadow-blue-100/50' 
                      : 'bg-gradient-to-r from-gray-600 to-gray-700 text-white border-gray-500/20 shadow-gray-100/50'
                  }`}>
                    {clusterData?.depType === 'KUBERNETES' ? 'Kubernetes' : 'Linux'}
                  </div>
                </div>
              </div>
              <button
                className="relative p-2 hover:bg-white/70 backdrop-blur-sm rounded-xl transition-all duration-200 shadow-sm hover:shadow-md service-option-trigger group border border-white/30 hover:border-white/60"
                onClick={handleServiceOptionClick}
              >
                <MoreHorizontal className="w-4 h-4 text-gray-600 group-hover:text-gray-800 transition-colors" />
                <div className="absolute inset-0 rounded-xl bg-gradient-to-r from-blue-500/0 to-indigo-500/0 group-hover:from-blue-500/5 group-hover:to-indigo-500/10 transition-all duration-200"></div>
              </button>
            </div>
          </div>

          {/* 服务列表 */}
          <div className="flex-1 overflow-y-auto">
            {serviceLoading ? (
              <div className="p-8 text-center">
                <div className="relative">
                  <div className="animate-spin rounded-full h-8 w-8 border-2 border-gray-200 border-t-blue-600 mx-auto mb-4"></div>
                  <div className="absolute inset-0 rounded-full h-8 w-8 border-2 border-transparent border-t-blue-400 animate-pulse mx-auto"></div>
                </div>
                <p className="text-sm text-gray-700 font-medium">正在加载服务列表...</p>
                <p className="text-xs text-gray-500 mt-1">请稍候</p>
              </div>
            ) : (
              <>
                {/* 服务组件分组 - 第三方大数据服务 */}
                <div className="px-4 py-3 bg-gradient-to-r from-blue-50/50 via-indigo-50/30 to-blue-50/40">
                  <div 
                    className="flex items-center justify-between mb-3 cursor-pointer hover:bg-white/80 backdrop-blur-sm rounded-xl px-3 py-2 transition-all duration-200 border border-transparent hover:border-blue-200/50 hover:shadow-sm"
                    onClick={() => setBusinessServicesExpanded(!businessServicesExpanded)}
                  >
                    <div className="flex items-center">
                      <div className="p-1 rounded-lg bg-gradient-to-r from-blue-500 to-indigo-600 shadow-sm mr-3">
                        <ChevronDown 
                          className={`w-3 h-3 text-white transition-transform duration-300 ${
                            businessServicesExpanded ? 'rotate-0' : '-rotate-90'
                          }`} 
                        />
                      </div>
                      <span className="text-sm font-bold text-gray-900 tracking-tight">服务组件</span>
                      <div className="ml-3 flex items-center space-x-1 bg-white/70 backdrop-blur-sm px-2.5 py-1 rounded-lg shadow-sm border border-blue-200/30">
                        <div className="w-1.5 h-1.5 bg-blue-500 rounded-full"></div>
                        <span className="text-xs font-semibold text-blue-700">
                          {coreServices.length} 个组件
                        </span>
                      </div>
                    </div>
                  </div>
                  
                  <div 
                    className={`space-y-1 transition-all duration-300 ease-in-out ${
                      businessServicesExpanded 
                        ? 'opacity-100 max-h-screen' 
                        : 'opacity-0 max-h-0 overflow-hidden'
                    }`}
                  >
                    {coreServices.length > 0 ? coreServices.map((service) => {
                      const statusInfo = getServiceStatusInfo(service.serviceStateCode)
                      
                      return (
                        <div
                          key={service.id}
                          className={`group relative flex items-center p-3 cursor-pointer transition-all duration-300 rounded-xl mx-2 mb-1 ${
                            selectedService?.id === service.id 
                              ? 'bg-gradient-to-r from-blue-50 via-blue-100/70 to-indigo-50 border-2 border-blue-300 shadow-lg shadow-blue-100/50 scale-[1.02]' 
                              : 'bg-white hover:bg-gradient-to-r hover:from-gray-50 hover:to-blue-50/30 border-2 border-gray-100 hover:border-blue-200 hover:shadow-lg hover:shadow-gray-100/50 hover:scale-[1.01]'
                          }`}
                          onClick={() => handleServiceItemClick(service)}
                        >
                          {/* 装饰性左边框 */}
                          <div className={`absolute left-0 top-2 bottom-2 w-1 rounded-r-full transition-all duration-300 ${
                            selectedService?.id === service.id ? 'bg-gradient-to-b from-blue-500 to-indigo-600' : 'bg-gray-200 group-hover:bg-blue-400'
                          }`}></div>
                          
                          <div className="relative w-10 h-10 flex-shrink-0 mr-4">
                            <div className={`w-9 h-9 bg-gradient-to-br from-white to-gray-50 rounded-xl shadow-md border flex items-center justify-center transition-all duration-200 group-hover:rotate-3 group-hover:shadow-lg ${
                              selectedService?.id === service.id 
                                ? 'border-blue-300 shadow-blue-100/50' 
                                : 'border-gray-200 group-hover:border-blue-300'
                            }`}>
                              <SvgIcon name={service.icon || service.serviceName.toLowerCase()} className="w-5 h-5" />
                            </div>
                            <div className={`absolute -top-1 -right-1 w-3.5 h-3.5 rounded-full border-2 border-white shadow-sm transition-all duration-200 ${statusInfo.dotClassName} ${
                              selectedService?.id === service.id ? 'scale-110' : 'group-hover:scale-105'
                            }`}></div>
                          </div>
                          
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center justify-between">
                              <span className="text-sm font-semibold text-gray-900 truncate">{service.name}</span>
                              <div className="flex items-center space-x-2">
                                {service.alertNum > 0 && (
                                  <div className="relative">
                                    <span className="bg-gradient-to-r from-red-500 to-red-600 text-white text-xs rounded-full px-2 py-1 min-w-[1.5rem] text-center font-bold shadow-lg animate-pulse">
                                      {service.alertNum}
                                    </span>
                                    <div className="absolute inset-0 bg-red-400 rounded-full animate-ping opacity-20"></div>
                                  </div>
                                )}
                                <button
                                  className="opacity-0 group-hover:opacity-100 p-1.5 hover:bg-blue-50 hover:shadow-md rounded-xl transition-all duration-200 action-trigger border border-transparent hover:border-blue-200"
                                  onClick={handleServiceActionClick}
                                >
                                  <MoreHorizontal className="w-4 h-4 text-gray-500" />
                                </button>
                              </div>
                            </div>
                          </div>
                        </div>
                      )
                    }) : (
                      <div className="text-center py-8 px-4">
                        <div className="w-16 h-16 bg-gradient-to-br from-blue-100 to-indigo-100 rounded-2xl mx-auto mb-4 flex items-center justify-center shadow-sm">
                          <Server className="w-8 h-8 text-blue-500" />
                        </div>
                        <h4 className="text-sm font-semibold text-gray-900 mb-2">暂无服务组件</h4>
                        <p className="text-xs text-gray-500 leading-relaxed">请安装 HDFS、YARN、Spark 等<br/>大数据处理组件来开始使用</p>
                      </div>
                    )}
                  </div>
                </div>

                {/* 系统基础服务分组 - 内置必要服务 */}
                <div className="px-4 py-3 border-t border-gray-200/30 bg-gradient-to-r from-emerald-50/50 via-green-50/30 to-emerald-50/40 mt-1">
                  <div 
                    className="flex items-center justify-between mb-3 cursor-pointer hover:bg-white/80 backdrop-blur-sm rounded-xl px-3 py-2 transition-all duration-200 border border-transparent hover:border-emerald-200/50 hover:shadow-sm"
                    onClick={() => setSystemServicesExpanded(!systemServicesExpanded)}
                  >
                    <div className="flex items-center">
                      <div className="p-1 rounded-lg bg-gradient-to-r from-emerald-500 to-green-600 shadow-sm mr-3">
                        <ChevronDown 
                          className={`w-3 h-3 text-white transition-transform duration-300 ${
                            systemServicesExpanded ? 'rotate-0' : '-rotate-90'
                          }`} 
                        />
                      </div>
                      <span className="text-sm font-bold text-gray-900 tracking-tight">系统基础服务</span>
                      <div className="ml-3 flex items-center space-x-1 bg-white/70 backdrop-blur-sm px-2.5 py-1 rounded-lg shadow-sm border border-emerald-200/30">
                        <div className="w-1.5 h-1.5 bg-emerald-500 rounded-full"></div>
                        <span className="text-xs font-semibold text-emerald-700">
                          {managementServices.length} 个服务
                        </span>
                      </div>
                    </div>
                  </div>
                  
                  <div 
                    className={`space-y-1 transition-all duration-300 ease-in-out ${
                      systemServicesExpanded 
                        ? 'opacity-100 max-h-screen' 
                        : 'opacity-0 max-h-0 overflow-hidden'
                    }`}
                  >
                    {managementServices.length > 0 ? managementServices.map((service) => {
                      const statusInfo = getServiceStatusInfo(service.serviceStateCode)
                      
                      return (
                        <div
                          key={service.id}
                          className={`group relative flex items-center p-3 cursor-pointer transition-all duration-300 rounded-xl mx-2 mb-1 ${
                            selectedService?.id === service.id 
                              ? 'bg-gradient-to-r from-emerald-50 via-emerald-100/70 to-green-50 border-2 border-emerald-300 shadow-lg shadow-emerald-100/50 scale-[1.02]' 
                              : 'bg-white hover:bg-gradient-to-r hover:from-gray-50 hover:to-emerald-50/30 border-2 border-gray-100 hover:border-emerald-200 hover:shadow-lg hover:shadow-gray-100/50 hover:scale-[1.01]'
                          }`}
                          onClick={() => handleServiceItemClick(service)}
                        >
                          {/* 装饰性左边框 */}
                          <div className={`absolute left-0 top-2 bottom-2 w-1 rounded-r-full transition-all duration-300 ${
                            selectedService?.id === service.id ? 'bg-gradient-to-b from-emerald-500 to-green-600' : 'bg-gray-200 group-hover:bg-emerald-400'
                          }`}></div>
                          
                          <div className="relative w-10 h-10 flex-shrink-0 mr-4">
                            <div className={`w-9 h-9 bg-gradient-to-br from-white to-gray-50 rounded-xl shadow-md border flex items-center justify-center transition-all duration-200 group-hover:rotate-3 group-hover:shadow-lg ${
                              selectedService?.id === service.id 
                                ? 'border-emerald-300 shadow-emerald-100/50' 
                                : 'border-gray-200 group-hover:border-emerald-300'
                            }`}>
                              <SvgIcon name={service.icon || service.serviceName.toLowerCase()} className="w-5 h-5" />
                            </div>
                            <div className={`absolute -top-1 -right-1 w-3.5 h-3.5 rounded-full border-2 border-white shadow-sm transition-all duration-200 ${statusInfo.dotClassName} ${
                              selectedService?.id === service.id ? 'scale-110' : 'group-hover:scale-105'
                            }`}></div>
                          </div>
                          
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center justify-between">
                              <span className="text-sm font-semibold text-gray-900 truncate">{service.name}</span>
                              <div className="flex items-center space-x-2">
                                {service.alertNum > 0 && (
                                  <div className="relative">
                                    <span className="bg-gradient-to-r from-red-500 to-red-600 text-white text-xs rounded-full px-2 py-1 min-w-[1.5rem] text-center font-bold shadow-lg animate-pulse">
                                      {service.alertNum}
                                    </span>
                                    <div className="absolute inset-0 bg-red-400 rounded-full animate-ping opacity-20"></div>
                                  </div>
                                )}
                                {/* 大数据基础平台不显示操作按钮，其他管理服务显示 */}
                                {service.serviceName !== 'DATASOPHON' && (
                                  <button
                                    className="opacity-0 group-hover:opacity-100 p-1.5 hover:bg-emerald-50 hover:shadow-md rounded-xl transition-all duration-200 action-trigger border border-transparent hover:border-emerald-200"
                                    onClick={handleServiceActionClick}
                                  >
                                    <MoreHorizontal className="w-4 h-4 text-gray-500" />
                                  </button>
                                )}
                              </div>
                            </div>
                          </div>
                        </div>
                      )
                    }) : (
                      <div className="text-center py-8 px-4">
                        <div className="w-16 h-16 bg-gradient-to-br from-emerald-100 to-green-100 rounded-2xl mx-auto mb-4 flex items-center justify-center shadow-sm">
                          <Monitor className="w-8 h-8 text-emerald-500" />
                        </div>
                        <h4 className="text-sm font-semibold text-gray-900 mb-2">暂无系统基础服务</h4>
                        <p className="text-xs text-gray-500 leading-relaxed">系统基础服务包括平台管理、<br/>监控告警、数据可视化等功能</p>
                      </div>
                    )}
                  </div>
                </div>
              </>
            )}
          </div>
        </div>

        {/* 右侧内容区域 */}
        <div className="flex-1">
          {selectedService && selectedService.serviceName !== 'DATASOPHON' ? (
            // 服务详情页面 - 页签模式
            <ServiceDetailTabs 
              service={selectedService} 
            />
          ) : (
            // 总览页面 - 根据选中服务使用不同URL
            <div className="relative w-full h-full">
              {dashboardLoading ? (
                <div className="flex items-center justify-center h-96">
                  <div className="text-center">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
                    <p className="text-gray-600">
                      正在加载{selectedService?.serviceName === 'DATASOPHON' ? '大数据基础平台' : '集群总览'}...
                    </p>
                  </div>
                </div>
              ) : (() => {
                // 根据选中服务决定使用哪个URL
                const currentUrl = selectedService?.serviceName === 'DATASOPHON' 
                  ? datasophonDashboardUrl 
                  : mainDashboardUrl
                const title = selectedService?.serviceName === 'DATASOPHON' 
                  ? '大数据基础平台' 
                  : '集群总览'
                
                return currentUrl ? (
                  <div className="absolute inset-0 w-full h-full">
                    <iframe
                      src={currentUrl}
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
                    />
                  </div>
                ) : (
                  <div className="flex items-center justify-center h-96">
                    <div className="text-center">
                      <Monitor className="w-16 h-16 text-gray-400 mx-auto mb-4" />
                      <h2 className="text-xl font-semibold text-gray-900 mb-2">{title}</h2>
                      <p className="text-gray-600">正在准备监控面板...</p>
                      <Button 
                        onClick={loadDashboardData}
                        className="mt-4"
                      >
                        重新加载
                      </Button>
                    </div>
                  </div>
                )
              })()}
            </div>
          )}
        </div>
      </div>

      {/* 服务操作菜单 */}
      {showActionMenu && (
        <div
          className="fixed bg-white rounded-xl shadow-2xl border border-gray-200 py-2 z-50 action-menu backdrop-blur-sm"
          style={{
            top: actionMenuPosition.top,
            left: actionMenuPosition.left,
            minWidth: '180px'
          }}
        >
          <button className="w-full px-4 py-3 text-left text-sm hover:bg-green-50 hover:text-green-700 flex items-center transition-colors rounded-lg mx-2">
            <Play className="w-4 h-4 mr-3 text-green-600" />
            <span className="font-medium">启动</span>
          </button>
          <button className="w-full px-4 py-3 text-left text-sm hover:bg-yellow-50 hover:text-yellow-700 flex items-center transition-colors rounded-lg mx-2">
            <Pause className="w-4 h-4 mr-3 text-yellow-600" />
            <span className="font-medium">停止</span>
          </button>
          <button className="w-full px-4 py-3 text-left text-sm hover:bg-blue-50 hover:text-blue-700 flex items-center transition-colors rounded-lg mx-2">
            <RotateCcw className="w-4 h-4 mr-3 text-blue-600" />
            <span className="font-medium">重启</span>
          </button>
          <button className="w-full px-4 py-3 text-left text-sm hover:bg-purple-50 hover:text-purple-700 flex items-center transition-colors rounded-lg mx-2">
            <RotateCcw className="w-4 h-4 mr-3 text-purple-600" />
            <span className="font-medium">滚动重启</span>
          </button>
          <button className="w-full px-4 py-3 text-left text-sm hover:bg-indigo-50 hover:text-indigo-700 flex items-center transition-colors rounded-lg mx-2">
            <Users className="w-4 h-4 mr-3 text-indigo-600" />
            <span className="font-medium">分配角色组</span>
          </button>
          <hr className="my-2 border-gray-100" />
          <button className="w-full px-4 py-3 text-left text-sm hover:bg-red-50 hover:text-red-700 flex items-center text-red-600 transition-colors rounded-lg mx-2">
            <Trash2 className="w-4 h-4 mr-3" />
            <span className="font-medium">删除</span>
          </button>
        </div>
      )}

      {/* 总服务选项菜单 */}
      {showServiceOptionMenu && (
        <div
          className="fixed bg-white rounded-xl shadow-2xl border border-gray-200 py-2 z-50 service-option-menu backdrop-blur-sm"
          style={{
            top: serviceOptionMenuPosition.top,
            left: serviceOptionMenuPosition.left,
            minWidth: '220px'
          }}
        >
          <button className="w-full px-4 py-3 text-left text-sm hover:bg-blue-50 hover:text-blue-700 flex items-center transition-colors rounded-lg mx-2">
            <Plus className="w-4 h-4 mr-3 text-blue-600" />
            <span className="font-medium">添加服务</span>
          </button>
          <button className="w-full px-4 py-3 text-left text-sm hover:bg-green-50 hover:text-green-700 flex items-center transition-colors rounded-lg mx-2">
            <Play className="w-4 h-4 mr-3 text-green-600" />
            <span className="font-medium">启动所有</span>
          </button>
          <button className="w-full px-4 py-3 text-left text-sm hover:bg-yellow-50 hover:text-yellow-700 flex items-center transition-colors rounded-lg mx-2">
            <Pause className="w-4 h-4 mr-3 text-yellow-600" />
            <span className="font-medium">停止所有</span>
          </button>
          <button className="w-full px-4 py-3 text-left text-sm hover:bg-blue-50 hover:text-blue-700 flex items-center transition-colors rounded-lg mx-2">
            <RotateCcw className="w-4 h-4 mr-3 text-blue-600" />
            <span className="font-medium">重启所有需要重启的服务</span>
          </button>
        </div>
      )}
    </div>
  )
}