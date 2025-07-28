import { useSettingsStore } from '@/stores/settings'
import type { Router } from 'vue-router'

/**
 * 服务项类型定义
 */
interface ServiceItem {
  id?: string | number;
  serviceId?: string | number;
  serviceName?: string;
  name?: string;
  label?: string;
  serviceState?: string | number;
  status?: string | number;
  version?: string;
  category?: string;
  [key: string]: any;
}

/**
 * 切换路由并设置集群相关状态
 * @param serviceList - 服务列表数据
 * @param clusterId - 集群ID
 * @param router - Vue Router实例
 */
export function changeRouter(serviceList: ServiceItem[], clusterId: string | number, router: Router) {
  try {
    const settingsStore = useSettingsStore()
    
    // 设置集群ID
    if (clusterId) {
      settingsStore.setClusterId(String(clusterId))
    }
    
    // 构建菜单数据
    const menuData = buildMenuData(serviceList)
    
    // 设置菜单数据到store
    if (menuData && menuData.length > 0) {
      settingsStore.setMenuData(menuData)
    }
    
    console.log('[changeRouter] 路由切换完成:', {
      clusterId,
      serviceCount: serviceList?.length || 0,
      menuCount: menuData?.length || 0
    })
    
  } catch (error) {
    console.error('[changeRouter] 路由切换失败:', error)
    throw error
  }
}

/**
 * 构建菜单数据结构
 * @param serviceList - 服务列表
 * @returns 菜单数据
 */
function buildMenuData(serviceList: ServiceItem[]) {
  if (!Array.isArray(serviceList)) {
    console.warn('[buildMenuData] 服务列表不是数组:', serviceList)
    return []
  }
  
  const menuData: any[] = []
  
  // 遍历服务列表，构建菜单结构
  serviceList.forEach(service => {
    if (!service) return
    
    const menuItem = {
      id: service.id || service.serviceId,
      name: service.serviceName || service.name,
      label: service.label || service.serviceName || service.name,
      status: service.serviceState || service.status,
      version: service.version,
      category: service.category || 'default',
      path: `/service/${service.serviceName || service.name}`,
      // 添加其他可能需要的字段
      ...service
    }
    
    menuData.push(menuItem)
  })
  
  // 按类别和名称排序
  menuData.sort((a, b) => {
    // 首先按类别排序
    if (a.category !== b.category) {
      return (a.category || '').localeCompare(b.category || '')
    }
    // 然后按名称排序
    return (a.name || '').localeCompare(b.name || '')
  })
  
  console.log('[buildMenuData] 构建菜单数据:', menuData)
  
  return menuData
}

/**
 * 获取服务状态文本
 * @param status - 状态码
 * @returns 状态文本
 */
export function getServiceStatusText(status: string | number): string {
  const statusMap: Record<string | number, string> = {
    1: '已安装',
    2: '运行中', 
    3: '已停止',
    4: '安装中',
    5: '安装失败',
    6: '启动中',
    7: '停止中'
  }
  
  return statusMap[status] || '未知状态'
}

/**
 * 获取服务状态样式类
 * @param status - 状态码
 * @returns 样式类名
 */
export function getServiceStatusClass(status: string | number): string {
  const classMap: Record<string | number, string> = {
    1: 'installed',
    2: 'running',
    3: 'stopped', 
    4: 'installing',
    5: 'failed',
    6: 'starting',
    7: 'stopping'
  }
  
  return classMap[status] || 'unknown'
}

export default {
  changeRouter,
  buildMenuData,
  getServiceStatusText,
  getServiceStatusClass
}