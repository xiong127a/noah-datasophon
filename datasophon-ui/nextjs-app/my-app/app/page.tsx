"use client"

import { useEffect, useState, useCallback } from 'react'
import { useRouter } from 'next/navigation'
import { 
  Server, 
  AlertTriangle,
  CheckCircle,
  ArrowRight,
  Monitor,
  Workflow,
  AlertCircle,
  Pause,
  Play,
  RotateCcw,
  Trash2,
  MoreHorizontal,
  ChevronDown,
  ChevronRight,
  Plus
} from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { useCluster } from '@/hooks/useCluster'
import FinalNavbar from '@/components/layout/navbar-final'
import { clusterApiV1 } from '@/lib/api-utils-v1'
import { createClusterHeaders } from '@/lib/cluster-id-header'
import { SvgIcon } from '@/components/ui/svg-icon'

// 服务状态枚举
enum ServiceState {
  STOPPED = 1,
  RUNNING = 2,
  WARNING = 3,
  ERROR = 4
}

// 服务数据接口 - 按Vue2项目结构定义
interface ServiceItem {
  id: string
  name: string
  serviceName: string
  icon?: string
  serviceId: string
  path: string
  serviceStateCode?: ServiceState
  alertNum?: number
  needRestart?: boolean
  rawData: Record<string, unknown>
  menuVisible?: boolean
}

// 集群信息接口
interface ClusterData {
  id: string
  clusterName: string
  clusterFrame?: string
  depType?: string
  clusterState?: string | number
}

export default function ServiceLayout() {
  const router = useRouter()
  const { currentCluster, hasCluster, loading } = useCluster()
  
  // 状态管理 - 完全按Vue2项目结构
  const [services, setServices] = useState<ServiceItem[]>([])
  const [selectedService, setSelectedService] = useState<ServiceItem | null>(null)
  const [serviceLoading, setServiceLoading] = useState(false)
  const [clusterData, setClusterData] = useState<ClusterData | null>(null)
  
  // 管理服务名称列表 - 按Vue2项目配置
  const managementServiceNames = ['ALERTMANAGER', 'PROMETHEUS', 'GRAFANA', 'PUSHGATEWAY', 'DATASOPHON']
  
  // 分组折叠状态
  const [coreGroupCollapsed, setCoreGroupCollapsed] = useState(false)
  const [managementGroupCollapsed, setManagementGroupCollapsed] = useState(false)
  
  // 服务操作菜单状态
  const [activeService, setActiveService] = useState<ServiceItem | null>(null)
  const [showActionMenu, setShowActionMenu] = useState(false)
  const [actionMenuPosition, setActionMenuPosition] = useState({ top: 0, left: 0 })
  
  // 总服务菜单状态
  const [showServiceOptionMenu, setShowServiceOptionMenu] = useState(false)
  const [serviceOptionMenuPosition, setServiceOptionMenuPosition] = useState({ top: 0, left: 0 })

  // 获取集群信息 - 按Vue2实现
  const getClusterInfo = useCallback(async () => {
    if (!hasCluster || !currentCluster) return
    
    try {
      // 优先从运行中集群列表获取
      const response = await clusterApiV1.info.runningList()
      
      if (response.data.code === 200 && response.data.data && response.data.data.length > 0) {
        const cluster = response.data.data.find((c: Record<string, unknown>) => c.id === currentCluster.id) || response.data.data[0]
        setClusterData({
          id: (cluster.id as string)?.toString() || currentCluster.id,
          clusterName: (cluster.clusterName as string) || currentCluster.name,
          clusterFrame: cluster.clusterFrame as string,
          depType: cluster.depType as string,
          clusterState: cluster.clusterState as string | number
        })
      } else {
        // 使用当前集群信息作为fallback
        setClusterData({
          id: currentCluster.id,
          clusterName: currentCluster.name,
          clusterFrame: 'DDP-1.2.1',
          depType: currentCluster.isK8s ? 'KUBERNETES' : 'PVM',
          clusterState: 'Running'
        })
      }
    } catch (error) {
      console.error('获取集群信息失败:', error)
      // 使用当前集群信息作为fallback
      setClusterData({
        id: currentCluster.id,
        clusterName: currentCluster.name,
        clusterFrame: 'DDP-1.2.1',
        depType: currentCluster.isK8s ? 'KUBERNETES' : 'PVM',
        clusterState: 'Running'
      })
    }
  }, [hasCluster, currentCluster])

  // 获取服务列表 - 按Vue2项目实现
  const fetchServices = useCallback(async () => {
    if (!hasCluster || !currentCluster) return
    
    try {
      setServiceLoading(true)
      const headers = createClusterHeaders(currentCluster.id)
      const response = await clusterApiV1.service.list({ headers })
      
      if (response.data.code === 200 && response.data.data && Array.isArray(response.data.data)) {
        // 转换数据格式 - 按Vue2项目映射
        const processedServices: ServiceItem[] = response.data.data.map((item: Record<string, unknown>, index: number) => {
          const serviceName = (item.serviceName as string) || (item.name as string)
          const displayName = (item.label as string) || serviceName
          
          return {
            id: String(item.id || index + 1),
            name: displayName,
            serviceName: serviceName,
            icon: serviceName ? serviceName.toLowerCase() : 'service-default',
            serviceId: String(item.id),
            path: `/service-manage/service-list/${item.id}`,
            serviceStateCode: (item.serviceStateCode as ServiceState) || ServiceState.RUNNING,
            alertNum: (item.alertNum as number) || 0,
            needRestart: (item.needRestart as boolean) || false,
            rawData: item,
            menuVisible: false
          }
        })
        
        setServices(processedServices)
      } else {
        console.warn('API返回数据格式异常，使用默认服务列表')
        loadDefaultServices()
      }
    } catch (error) {
      console.error('获取服务列表失败:', error)
      loadDefaultServices()
    } finally {
      setServiceLoading(false)
    }
  }, [hasCluster, currentCluster])

  // 使用默认服务列表 - 按Vue2项目fallback实现
  const loadDefaultServices = () => {
    const defaultServices: ServiceItem[] = [
      { id: '1', name: 'HDFS', serviceName: 'HDFS', icon: 'hdfs', serviceId: '1', path: '/service-manage/service-list/1', serviceStateCode: ServiceState.RUNNING, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false },
      { id: '2', name: 'YARN', serviceName: 'YARN', icon: 'yarn', serviceId: '2', path: '/service-manage/service-list/2', serviceStateCode: ServiceState.RUNNING, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false },
      { id: '3', name: 'HBase', serviceName: 'HBASE', icon: 'hbase', serviceId: '3', path: '/service-manage/service-list/3', serviceStateCode: ServiceState.RUNNING, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false },
      { id: '4', name: 'Hive', serviceName: 'HIVE', icon: 'hive', serviceId: '4', path: '/service-manage/service-list/4', serviceStateCode: ServiceState.RUNNING, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false },
      { id: '5', name: 'Spark', serviceName: 'SPARK', icon: 'spark3', serviceId: '5', path: '/service-manage/service-list/5', serviceStateCode: ServiceState.RUNNING, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false },
      { id: '6', name: 'ZooKeeper', serviceName: 'ZOOKEEPER', icon: 'zookeeper', serviceId: '6', path: '/service-manage/service-list/6', serviceStateCode: ServiceState.RUNNING, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false },
      { id: '7', name: 'Kafka', serviceName: 'KAFKA', icon: 'kafka', serviceId: '7', path: '/service-manage/service-list/7', serviceStateCode: ServiceState.RUNNING, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false },
      { id: '8', name: 'Flink', serviceName: 'FLINK', icon: 'flink', serviceId: '8', path: '/service-manage/service-list/8', serviceStateCode: ServiceState.WARNING, alertNum: 2, needRestart: false, rawData: {}, menuVisible: false },
    ]
    setServices(defaultServices)
  }

  // 计算服务分组 - 按Vue2实现
  const coreServices = services.filter(service => {
    const serviceNameForFilter = service.serviceName || service.name
    return !managementServiceNames.includes(serviceNameForFilter.toUpperCase())
  })

  const managementServices = (() => {
    // 创建硬编码的大数据基础平台服务项
    const platformService: ServiceItem = { 
      id: '0', 
      name: '大数据基础平台', 
      serviceName: 'DATASOPHON', 
      icon: 'logo', 
      path: '/service-manage', 
      serviceId: '', 
      serviceStateCode: ServiceState.RUNNING, 
      alertNum: 0, 
      needRestart: false, 
      rawData: {}, 
      menuVisible: false
    }
    
    // 过滤管理服务
    const filteredServices = services.filter(service => {
      const serviceNameForFilter = service.serviceName || service.name
      return managementServiceNames.includes(serviceNameForFilter.toUpperCase())
    })
    
    // 如果没有找到大数据基础平台，添加它
    const hasPlatform = filteredServices.some(s => (s.serviceName || '').toUpperCase() === 'DATASOPHON')
    if (!hasPlatform) {
      filteredServices.unshift(platformService)
    }
    
    return filteredServices
  })()

  // 获取服务状态图标和样式 - 按Vue2实现
  const getServiceStatusIcon = (state?: ServiceState) => {
    switch (state) {
      case ServiceState.RUNNING:
        return { 
          icon: CheckCircle, 
          className: "text-green-500 fill-current", 
          label: '正在运行' 
        }
      case ServiceState.WARNING:
        return { 
          icon: AlertTriangle, 
          className: "text-yellow-500 fill-current", 
          label: '警告' 
        }
      case ServiceState.ERROR:
        return { 
          icon: AlertCircle, 
          className: "text-red-500 fill-current", 
          label: '错误' 
        }
      default:
        return { 
          icon: () => <div className="w-4 h-4 rounded-full bg-gray-300" />, 
          className: "text-gray-400", 
          label: '已停止' 
        }
    }
  }



  // 判断当前服务是否激活
  const isActiveService = (service: ServiceItem) => {
    return selectedService?.serviceId === service.serviceId
  }

  // 处理服务项点击 - 按Vue2实现
  const handleServiceItemClick = (service: ServiceItem) => {
    setSelectedService(service)
    
    // 导航到对应的服务页面
    if (service.serviceId && service.serviceId !== '') {
      router.push(service.path || `/service-manage/service-list/${service.serviceId}`)
    } else {
      router.push(service.path || '/service-manage')
    }
  }

  // 处理服务操作菜单点击
  const handleServiceMenuClick = (service: ServiceItem, event: React.MouseEvent) => {
    event.stopPropagation()
    
    const rect = (event.target as HTMLElement).closest('button')?.getBoundingClientRect()
    if (rect) {
      setActionMenuPosition({
        top: rect.bottom + 5,
        left: rect.left
      })
    }
    
    setActiveService(service)
    setShowActionMenu(true)
  }

  // 处理总服务菜单点击
  const handleServiceOptionClick = (event: React.MouseEvent) => {
    event.stopPropagation()
    
    const rect = (event.target as HTMLElement).closest('button')?.getBoundingClientRect()
    if (rect) {
      setServiceOptionMenuPosition({
        top: rect.bottom + 5,
        left: rect.right - 192 // 菜单宽度约192px，让菜单右对齐到按钮
      })
    }
    
    setShowServiceOptionMenu(true)
  }

  // 处理服务操作
  const handleServiceAction = async (action: string, service: ServiceItem) => {
    try {
      if (action === 'delete') {
        if (!service.serviceId) return
        
        const response = await clusterApiV1.service.delete(service.serviceId)
        if (response.data.code === 200) {
          // 刷新服务列表
          await fetchServices()
          // 如果删除的是当前选中的服务，返回总览
          if (selectedService?.serviceId === service.serviceId) {
            setSelectedService(null)
            router.push('/service-manage')
          }
        }
      } else {
        // 其他操作（启动/停止/重启）需要调用相应的API
        console.log(`执行${action}操作:`, service.name)
      }
    } catch (error) {
      console.error(`${action}操作失败:`, error)
    }
    setShowActionMenu(false)
    setActiveService(null)
  }

  // 处理总服务操作
  const handleServiceOptionAction = (action: string) => {
    console.log(`执行总服务操作: ${action}`)
    setShowServiceOptionMenu(false)
  }

  // 切换分组折叠状态
  const toggleGroupCollapse = (groupType: 'core' | 'management') => {
    if (groupType === 'core') {
      setCoreGroupCollapsed(!coreGroupCollapsed)
    } else {
      setManagementGroupCollapsed(!managementGroupCollapsed)
    }
  }

  // 点击外部关闭菜单
  useEffect(() => {
    const handleClickOutside = () => {
      setShowActionMenu(false)
      setShowServiceOptionMenu(false)
    }

    if (showActionMenu || showServiceOptionMenu) {
      document.addEventListener('click', handleClickOutside)
      return () => document.removeEventListener('click', handleClickOutside)
    }
  }, [showActionMenu, showServiceOptionMenu])

  // 生命周期钩子
  useEffect(() => {
    // 检查登录状态
    const token = localStorage.getItem('jwt_token')
    if (!token) {
      router.push('/login')
      return
    }
  }, [router])

  useEffect(() => {
    if (hasCluster && currentCluster) {
      getClusterInfo()
      fetchServices()
    } else {
      setServices([])
      setClusterData(null)
      setSelectedService(null)
    }
  }, [hasCluster, currentCluster, getClusterInfo, fetchServices])

  // 渲染集群信息区域 - 紧凑版本
  const renderClusterInfo = () => (
    <div className="px-4 py-3 border-b border-gray-200 bg-white">
      <div className="flex items-center justify-between">
        <div className="flex-1 min-w-0">
          {clusterData ? (
            <>
              <div className="text-sm font-semibold text-gray-900 truncate">
                {clusterData.clusterName}
              </div>
              <div className="flex items-center gap-2 mt-1">
                {clusterData.clusterFrame && (
                  <span className="text-xs text-blue-600 bg-blue-50 px-1.5 py-0.5 rounded">
                    {clusterData.clusterFrame}
                  </span>
                )}
                {clusterData.depType && (
                  <span className="text-xs text-green-600 bg-green-50 px-1.5 py-0.5 rounded">
                    {clusterData.depType === 'KUBERNETES' ? 'Kubernetes' : 
                     clusterData.depType === 'PVM' ? 'PVM' : clusterData.depType}
                  </span>
                )}
              </div>
            </>
          ) : (
            <div className="text-sm text-gray-500">加载中...</div>
          )}
        </div>
        
        {/* 总服务操作按钮 */}
        <div className="relative">
          <Button
            variant="ghost"
            size="sm"
            onClick={handleServiceOptionClick}
            className="p-1.5 h-7 w-7"
          >
            <MoreHorizontal className="w-4 h-4" />
          </Button>
          
          {/* 总服务操作菜单 */}
          {showServiceOptionMenu && (
            <div 
              className="fixed z-50 w-48 bg-white rounded-lg shadow-lg border border-gray-200 py-1"
              style={{ 
                top: serviceOptionMenuPosition.top, 
                left: serviceOptionMenuPosition.left
              }}
              onClick={(e) => e.stopPropagation()}
            >
              <button 
                className="w-full px-3 py-2 text-left text-sm hover:bg-gray-50 flex items-center gap-2"
                onClick={() => handleServiceOptionAction('addService')}
              >
                <Plus className="w-4 h-4 text-blue-500" />
                添加服务
              </button>
              <button 
                className="w-full px-3 py-2 text-left text-sm hover:bg-gray-50 flex items-center gap-2"
                onClick={() => handleServiceOptionAction('startAll')}
              >
                <Play className="w-4 h-4 text-green-500" />
                启动所有
              </button>
              <button 
                className="w-full px-3 py-2 text-left text-sm hover:bg-gray-50 flex items-center gap-2"
                onClick={() => handleServiceOptionAction('stopAll')}
              >
                <Pause className="w-4 h-4 text-red-500" />
                停止所有
              </button>
              <button 
                className="w-full px-3 py-2 text-left text-sm hover:bg-gray-50 flex items-center gap-2"
                onClick={() => handleServiceOptionAction('restartAll')}
              >
                <RotateCcw className="w-4 h-4 text-orange-500" />
                重启所有需要重启的服务
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  )

  // 渲染服务分组 - 紧凑版本
  const renderServiceGroup = (
    title: string, 
    icon: React.ReactNode, 
    services: ServiceItem[], 
    collapsed: boolean, 
    onToggle: () => void
  ) => (
    <div className="mb-2">
      <div 
        className="flex items-center justify-between px-3 py-2 cursor-pointer hover:bg-gray-50 transition-colors"
        onClick={onToggle}
      >
        <div className="flex items-center gap-2">
          <div className="text-blue-600">
            {icon}
          </div>
          <span className="text-sm font-medium text-gray-800">{title}</span>
        </div>
        <div className="text-gray-400">
          {collapsed ? 
            <ChevronRight className="w-4 h-4" /> : 
            <ChevronDown className="w-4 h-4" />
          }
        </div>
      </div>

      {!collapsed && (
        <div className="space-y-0.5 px-2">
          {services.map((service) => (
            <div
              key={service.id}
              className={`flex items-center px-2 py-1.5 rounded cursor-pointer transition-all group hover:bg-blue-50 ${
                isActiveService(service) ? 'bg-blue-100 border-l-2 border-blue-500' : ''
              }`}
              onClick={() => handleServiceItemClick(service)}
            >
              {/* 状态指示器 */}
              <div className="w-4 flex justify-center mr-2">
                {(() => {
                  const { icon: StatusIcon, className } = getServiceStatusIcon(service.serviceStateCode)
                  return <StatusIcon className={`w-3 h-3 ${className}`} />
                })()}
              </div>

              {/* 服务图标 */}
              <div className="mr-2 flex-shrink-0">
                <SvgIcon 
                  name={service.serviceName || service.name || ''} 
                  size={16} 
                />
              </div>
              
              {/* 服务名称 */}
              <div className="flex-1 min-w-0">
                <div className="text-sm font-medium text-gray-700 truncate">
                  {service.name}
                </div>
              </div>

              {/* 右侧指示器和按钮 */}
              <div className="flex items-center gap-1">
                {/* 告警数量 */}
                {service.alertNum && service.alertNum > 0 && (
                  <div className="flex items-center gap-1 px-1.5 py-0.5 bg-yellow-100 text-yellow-700 rounded text-xs">
                    <AlertTriangle className="w-3 h-3" />
                    <span>{service.alertNum}</span>
                  </div>
                )}

                {/* 更多操作按钮 */}
                {service.serviceName !== 'PLATFORM' && service.serviceName !== 'DATASOPHON' && (
                  <div className="relative">
                    <Button
                      variant="ghost"
                      size="sm"
                      className="p-1 h-6 w-6 opacity-0 group-hover:opacity-100 transition-opacity"
                      onClick={(e) => handleServiceMenuClick(service, e)}
                    >
                      <MoreHorizontal className="w-3 h-3" />
                    </Button>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )

  // 渲染左侧服务列表 - 固定宽度，紧凑版本
  const renderServiceSidebar = () => (
    <div className="w-64 bg-white border-r border-gray-200 flex flex-col">
      {/* 集群信息区域 */}
      {renderClusterInfo()}

      {/* 服务列表区域 */}
      <div className="flex-1 overflow-y-auto py-2">
        {serviceLoading ? (
          <div className="flex items-center justify-center py-8">
            <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-600" />
          </div>
        ) : (
          <>
            {/* 核心服务组 */}
            {renderServiceGroup(
              'Core Service',
              <Server className="w-4 h-4" />,
              coreServices,
              coreGroupCollapsed,
              () => toggleGroupCollapse('core')
            )}

            {/* 管理服务组 */}
            {renderServiceGroup(
              'Management',
              <Monitor className="w-4 h-4" />,
              managementServices,
              managementGroupCollapsed,
              () => toggleGroupCollapse('management')
            )}
          </>
        )}
      </div>
    </div>
  )

  // 渲染右侧内容区域
  const renderMainContent = () => (
    <div className="flex-1 bg-gray-50">
      {selectedService ? (
        // 选中服务的详情页面
        <div className="p-6">
          <div className="max-w-4xl mx-auto">
            <div className="mb-6">
              <h1 className="text-2xl font-bold text-gray-900 mb-2">
                {selectedService.name} 服务详情
              </h1>
              <p className="text-gray-600">
                管理和监控 {selectedService.name} 服务的运行状态
              </p>
            </div>
            
            {/* 服务状态卡片 */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
              <Card>
                <CardHeader className="pb-3">
                  <CardTitle className="text-sm font-medium text-gray-600">服务状态</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="flex items-center">
                    {(() => {
                      const { icon: StatusIcon, className, label } = getServiceStatusIcon(selectedService.serviceStateCode)
                      return (
                        <>
                          <StatusIcon className={`w-5 h-5 mr-2 ${className}`} />
                          <span className="text-sm font-medium">{label}</span>
                        </>
                      )
                    })()}
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardHeader className="pb-3">
                  <CardTitle className="text-sm font-medium text-gray-600">服务类型</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="flex items-center">
                    <Workflow className="w-5 h-5 mr-2 text-blue-500" />
                    <span className="text-sm font-medium">{selectedService.serviceName}</span>
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardHeader className="pb-3">
                  <CardTitle className="text-sm font-medium text-gray-600">告警数量</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="flex items-center">
                    <AlertTriangle className="w-5 h-5 mr-2 text-yellow-500" />
                    <span className="text-sm font-medium">{selectedService.alertNum || 0}</span>
                  </div>
                </CardContent>
              </Card>
            </div>

            {/* 服务操作按钮 */}
            <div className="flex gap-4">
              <Button 
                className="flex items-center gap-2"
                onClick={() => handleServiceAction('start', selectedService)}
              >
                <Play className="w-4 h-4" />
                启动服务
              </Button>
              <Button 
                variant="secondary"
                className="flex items-center gap-2"
                onClick={() => handleServiceAction('stop', selectedService)}
              >
                <Pause className="w-4 h-4" />
                停止服务
              </Button>
              <Button 
                variant="secondary"
                className="flex items-center gap-2"
                onClick={() => handleServiceAction('restart', selectedService)}
              >
                <RotateCcw className="w-4 h-4" />
                重启服务
              </Button>
              {selectedService.serviceName !== 'DATASOPHON' && (
                <Button 
                  variant="destructive"
                  className="flex items-center gap-2"
                  onClick={() => handleServiceAction('delete', selectedService)}
                >
                  <Trash2 className="w-4 h-4" />
                  删除服务
                </Button>
              )}
            </div>
          </div>
        </div>
      ) : (
        // 默认总览页面
        <div className="p-6">
          <div className="max-w-4xl mx-auto">
            <div className="mb-8">
              <h1 className="text-3xl font-bold text-gray-900 mb-2">
                服务管理总览
              </h1>
              <p className="text-lg text-gray-600">
                管理和监控大数据集群中的各项服务
              </p>
            </div>

            {/* 统计卡片 */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
              <Card>
                <CardHeader className="pb-3">
                  <CardTitle className="text-sm font-medium text-gray-600">总服务数</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold text-gray-900">{services.length}</div>
                </CardContent>
              </Card>

              <Card>
                <CardHeader className="pb-3">
                  <CardTitle className="text-sm font-medium text-gray-600">运行中</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold text-green-600">
                    {services.filter(s => s.serviceStateCode === ServiceState.RUNNING).length}
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardHeader className="pb-3">
                  <CardTitle className="text-sm font-medium text-gray-600">告警中</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold text-yellow-600">
                    {services.filter(s => s.serviceStateCode === ServiceState.WARNING).length}
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardHeader className="pb-3">
                  <CardTitle className="text-sm font-medium text-gray-600">异常</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold text-red-600">
                    {services.filter(s => s.serviceStateCode === ServiceState.ERROR).length}
                  </div>
                </CardContent>
              </Card>
            </div>

            {/* 提示信息 */}
            <Card>
              <CardHeader>
                <CardTitle>欢迎使用服务管理</CardTitle>
                <CardDescription>
                  选择左侧的服务来查看详细信息和执行管理操作
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="flex items-center text-sm text-gray-500">
                  <ArrowRight className="w-4 h-4 mr-2" />
                  点击左侧服务列表中的任何服务来开始管理
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      )}
    </div>
  )

  if (loading) {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100">
      <div className="text-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
        <p className="text-gray-600">正在加载...</p>
      </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-white">
      <FinalNavbar />
      
      <div className="flex h-[calc(100vh-56px)]">
        {/* 左侧服务列表 - 固定宽度 */}
        {renderServiceSidebar()}
        
        {/* 右侧内容区域 */}
        {renderMainContent()}
      </div>

      {/* 服务操作菜单 */}
      {showActionMenu && activeService && (
        <div 
          className="fixed z-50 bg-white rounded-lg shadow-lg border border-gray-200 py-1 min-w-32"
          style={{ 
            top: actionMenuPosition.top, 
            left: actionMenuPosition.left 
          }}
          onClick={(e) => e.stopPropagation()}
        >
          <button 
            className="w-full px-3 py-2 text-left text-sm hover:bg-gray-50 flex items-center gap-2"
            onClick={() => handleServiceAction('start', activeService)}
          >
            <Play className="w-4 h-4 text-green-500" />
            启动
          </button>
          <button 
            className="w-full px-3 py-2 text-left text-sm hover:bg-gray-50 flex items-center gap-2"
            onClick={() => handleServiceAction('stop', activeService)}
          >
            <Pause className="w-4 h-4 text-red-500" />
            停止
          </button>
          <button 
            className="w-full px-3 py-2 text-left text-sm hover:bg-gray-50 flex items-center gap-2"
            onClick={() => handleServiceAction('restart', activeService)}
          >
            <RotateCcw className="w-4 h-4 text-orange-500" />
            重启
          </button>
          <div className="border-t border-gray-200 my-1" />
          <button 
            className="w-full px-3 py-2 text-left text-sm hover:bg-gray-50 flex items-center gap-2 text-red-600"
            onClick={() => handleServiceAction('delete', activeService)}
          >
            <Trash2 className="w-4 h-4" />
            删除
          </button>
        </div>
      )}
    </div>
  )
}