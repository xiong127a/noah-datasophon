/**
 * 服务页签工具函数
 */

// 定义菜单数据类型
interface MenuDataItem {
  path: string
  children?: MenuDataChild[]
}

interface MenuDataChild {
  meta?: {
    params?: {
      serviceId: string
    }
    obj?: {
      dashboardUrl?: string
    }
  }
}

/**
 * 检查服务是否有总览页签
 * 根据Vue2项目逻辑：只有当服务有dashboardUrl且不为空时才显示总览页签
 */
export function hasOverviewTab(serviceId: string): boolean {
  try {
    const menuData: MenuDataItem[] = JSON.parse(localStorage.getItem('menuData') || '[]')
    const serviceManageMenu = menuData.find((item) => item.path === 'service-manage')
    
    if (serviceManageMenu && serviceManageMenu.children) {
      const serviceItem = serviceManageMenu.children.find((item) => 
        item.meta?.params?.serviceId === serviceId
      )
      
      if (serviceItem && serviceItem.meta?.obj?.dashboardUrl) {
        const dashboardUrl = serviceItem.meta.obj.dashboardUrl
        return dashboardUrl !== undefined && dashboardUrl !== ""
      }
    }
    
    return false
  } catch (error) {
    console.error('检查总览页签失败:', error)
    return false
  }
}

/**
 * 获取服务的Dashboard URL
 */
export function getServiceDashboardUrl(serviceId: string): string | null {
  try {
    const menuData: MenuDataItem[] = JSON.parse(localStorage.getItem('menuData') || '[]')
    const serviceManageMenu = menuData.find((item) => item.path === 'service-manage')
    
    if (serviceManageMenu && serviceManageMenu.children) {
      const serviceItem = serviceManageMenu.children.find((item) => 
        item.meta?.params?.serviceId === serviceId
      )
      
      if (serviceItem && serviceItem.meta?.obj?.dashboardUrl) {
        return serviceItem.meta.obj.dashboardUrl
      }
    }
    
    return null
  } catch (error) {
    console.error('获取Dashboard URL失败:', error)
    return null
  }
}

/**
 * 检查服务是否支持连接信息页签
 */
export function hasConnectionTab(serviceName: string): boolean {
  // 这些服务支持连接信息
  const supportedServices = ['HDFS', 'YARN', 'HIVE', 'HBASE', 'KAFKA', 'SPARK', 'FLINK']
  return supportedServices.includes(serviceName.toUpperCase())
}

/**
 * 检查服务是否有WebUI
 */
export function hasWebUI(): boolean {
  // 可以根据实际需求实现WebUI检查逻辑
  // 目前返回true，表示大多数服务都有WebUI
  return true
}
