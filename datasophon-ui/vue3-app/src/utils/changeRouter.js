import { useSettingsStore } from '@/stores/settings'

/**
 * 切换路由并设置相关的状态
 * @param {Array} data - 集群的服务数据
 * @param {string} clusterId - 集群ID
 * @param {Router} router - Vue Router实例
 */
export const changeRouter = (data, clusterId, router) => {
  // 获取设置Store
  const settingsStore = useSettingsStore()
  
  // 设置集群相关状态
  settingsStore.setIsCluster(true)
  settingsStore.setClusterId(clusterId)
  
  // 构建菜单数据
  const menuData = buildMenuData(data)
  settingsStore.setMenuData(menuData)
  
  // 如果路由实例存在，执行路由跳转
  if (router) {
    router.push('/service-manage')
  }
}

/**
 * 构建菜单数据
 * @param {Array} serviceList - 服务列表数据
 * @returns {Array} - 处理后的菜单数据
 */
function buildMenuData(serviceList) {
  if (!serviceList || !Array.isArray(serviceList)) {
    return []
  }
  
  // 转换为菜单数据结构
  return serviceList.map(service => ({
    id: service.id,
    name: service.serviceName,
    path: `/service/${service.id}`,
    icon: service.serviceIcon || 'default-service',
    children: service.children ? buildMenuData(service.children) : []
  }))
} 