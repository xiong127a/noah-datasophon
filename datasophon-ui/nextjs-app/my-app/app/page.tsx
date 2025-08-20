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
  Users
} from 'lucide-react'
import { Button } from "@/components/ui/button"
import { useCluster } from '@/hooks/useCluster'
import FinalNavbar from '@/components/layout/navbar-final'
import { clusterApiV1 } from '@/lib/api-utils-v1'
import { createClusterHeaders } from '@/lib/cluster-id-header'
import { SvgIcon } from '@/components/ui/svg-icon'

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
    // 使用按钮位置，菜单出现在按钮下方左对齐
    setServiceOptionMenuPosition({
      top: rect.bottom + window.scrollY + 2, // 按钮下方2px
      left: rect.right + window.scrollX - 220  // 按钮右边界左移220px (菜单宽度)
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
    <div className="min-h-screen bg-gray-50">
      <FinalNavbar />
      
      {/* 主内容区域 */}
      <div className="flex">
        {/* 左侧服务列表 */}
        <div className="w-72 bg-gradient-to-b from-gray-50 to-white shadow-lg border-r border-gray-200 min-h-screen">
          {/* 集群信息头部 */}
          <div className="p-5 border-b border-gray-200 bg-gradient-to-r from-blue-50 to-indigo-50">
            <div className="flex items-center justify-between">
              <div className="flex-1">
                <div className="flex items-center space-x-2 mb-2">
                  <div className="w-3 h-3 bg-green-400 rounded-full animate-pulse"></div>
                  <h3 className="font-bold text-gray-900 text-base">{clusterData?.clusterName || currentCluster?.name}</h3>
                </div>
                <div className="flex items-center space-x-2">
                  <span className="text-sm text-gray-600 font-medium">{clusterData?.clusterFrame}</span>
                  <span className="px-2.5 py-1 bg-gradient-to-r from-blue-500 to-indigo-600 text-white rounded-full text-xs font-medium shadow-sm">
                    {clusterData?.depType === 'KUBERNETES' ? 'Kubernetes' : 'Linux'}
                  </span>
                </div>
              </div>
              <button
                className="p-2 hover:bg-white/80 rounded-lg transition-colors shadow-sm service-option-trigger"
                onClick={handleServiceOptionClick}
              >
                <MoreHorizontal className="w-5 h-5 text-gray-600" />
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
                {/* Core Service 分组 - 始终显示 */}
                <div className="px-4 py-3">
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center">
                      <ChevronDown className="w-4 h-4 text-blue-600 mr-2" />
                      <span className="text-sm font-semibold text-gray-800 uppercase tracking-wide">Core Service</span>
                      <span className="ml-2 bg-blue-100 text-blue-600 text-xs rounded-full px-2 py-0.5">
                        {coreServices.length}
                      </span>
                    </div>
                  </div>
                  
                  <div className="space-y-1">
                    {coreServices.length > 0 ? coreServices.map((service) => {
                      const statusInfo = getServiceStatusInfo(service.serviceStateCode)
                      
                      return (
                        <div
                          key={service.id}
                          className={`group relative flex items-center p-2.5 rounded-xl cursor-pointer transition-all duration-200 ${
                            selectedService?.id === service.id 
                              ? 'bg-gradient-to-r from-blue-50 to-blue-100 border border-blue-200 shadow-sm' 
                              : 'hover:bg-gray-50 hover:shadow-sm border border-transparent'
                          }`}
                          onClick={() => handleServiceItemClick(service)}
                        >
                          <div className="flex items-center space-x-3 flex-1">
                            <div className="relative w-8 h-8 flex-shrink-0 bg-white rounded-lg shadow-sm border border-gray-100 flex items-center justify-center">
                              <SvgIcon name={service.icon || service.serviceName.toLowerCase()} className="w-5 h-5" />
                              <div className={`absolute -top-1 -right-1 w-3 h-3 rounded-full border-2 border-white ${statusInfo.dotClassName}`}></div>
                            </div>
                            <div className="flex-1 min-w-0">
                              <div className="flex items-center justify-between">
                                <span className="text-sm font-medium text-gray-900 truncate">{service.name}</span>
                                <div className="flex items-center space-x-2">
                                  {service.alertNum > 0 && (
                                    <span className="bg-red-500 text-white text-xs rounded-full px-1.5 py-0.5 min-w-[1.25rem] text-center font-medium">
                                      {service.alertNum}
                                    </span>
                                  )}
                                  <button
                                    className="opacity-0 group-hover:opacity-100 p-1 hover:bg-white hover:shadow-sm rounded-md transition-all action-trigger"
                                    onClick={handleServiceActionClick}
                                  >
                                    <MoreHorizontal className="w-4 h-4 text-gray-400" />
                                  </button>
                                </div>
                              </div>
                            </div>
                          </div>
                        </div>
                      )
                    }) : (
                      <div className="text-center py-8">
                        <div className="w-12 h-12 bg-gray-100 rounded-lg mx-auto mb-3 flex items-center justify-center">
                          <Server className="w-6 h-6 text-gray-400" />
                        </div>
                        <p className="text-sm text-gray-500">暂无核心服务</p>
                        <p className="text-xs text-gray-400 mt-1">请安装HDFS、YARN等服务</p>
                      </div>
                    )}
                  </div>
                </div>

                {/* Management 分组 - 始终显示 */}
                <div className="px-4 py-3 border-t border-gray-100">
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center">
                      <ChevronDown className="w-4 h-4 text-green-600 mr-2" />
                      <span className="text-sm font-semibold text-gray-800 uppercase tracking-wide">Management</span>
                      <span className="ml-2 bg-green-100 text-green-600 text-xs rounded-full px-2 py-0.5">
                        {managementServices.length}
                      </span>
                    </div>
                  </div>
                  
                  <div className="space-y-1">
                    {managementServices.length > 0 ? managementServices.map((service) => {
                      const statusInfo = getServiceStatusInfo(service.serviceStateCode)
                      
                      return (
                        <div
                          key={service.id}
                          className={`group relative flex items-center p-2.5 rounded-xl cursor-pointer transition-all duration-200 ${
                            selectedService?.id === service.id 
                              ? 'bg-gradient-to-r from-green-50 to-green-100 border border-green-200 shadow-sm' 
                              : 'hover:bg-gray-50 hover:shadow-sm border border-transparent'
                          }`}
                          onClick={() => handleServiceItemClick(service)}
                        >
                          <div className="flex items-center space-x-3 flex-1">
                            <div className="relative w-8 h-8 flex-shrink-0 bg-white rounded-lg shadow-sm border border-gray-100 flex items-center justify-center">
                              <SvgIcon name={service.icon || service.serviceName.toLowerCase()} className="w-5 h-5" />
                              <div className={`absolute -top-1 -right-1 w-3 h-3 rounded-full border-2 border-white ${statusInfo.dotClassName}`}></div>
                            </div>
                            <div className="flex-1 min-w-0">
                              <div className="flex items-center justify-between">
                                <span className="text-sm font-medium text-gray-900 truncate">{service.name}</span>
                                <div className="flex items-center space-x-2">
                                  {service.alertNum > 0 && (
                                    <span className="bg-red-500 text-white text-xs rounded-full px-1.5 py-0.5 min-w-[1.25rem] text-center font-medium">
                                      {service.alertNum}
                                    </span>
                                  )}
                                  {/* 大数据基础平台不显示操作按钮，其他管理服务显示 */}
                                  {service.serviceName !== 'DATASOPHON' && (
                                    <button
                                      className="opacity-0 group-hover:opacity-100 p-1 hover:bg-white hover:shadow-sm rounded-md transition-all action-trigger"
                                      onClick={handleServiceActionClick}
                                    >
                                      <MoreHorizontal className="w-4 h-4 text-gray-400" />
                                    </button>
                                  )}
                                </div>
                              </div>
                            </div>
                          </div>
                        </div>
                      )
                    }) : (
                      <div className="text-center py-8">
                        <div className="w-12 h-12 bg-gray-100 rounded-lg mx-auto mb-3 flex items-center justify-center">
                          <Monitor className="w-6 h-6 text-gray-400" />
                        </div>
                        <p className="text-sm text-gray-500">暂无管理服务</p>
                        <p className="text-xs text-gray-400 mt-1">请安装Grafana、Prometheus等服务</p>
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
            // 其他服务详情页面
            <div className="p-6">
              <h1 className="text-2xl font-bold text-gray-900 mb-6">{selectedService.name} 服务详情</h1>
              <p className="text-gray-600">服务详情页面开发中...</p>
            </div>
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