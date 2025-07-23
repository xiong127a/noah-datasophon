import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

// 集群类型
export interface Cluster {
  id: string | number
  name: string
  depType: 'K8S' | 'Linux'
  desc?: string
  status: 'running' | 'stopped' | 'error'
}

// 菜单项类型
export interface MenuItem {
  name: string
  path: string
  meta: {
    icon: string
    title: string
  }
  component?: string
  id?: string | number
}

export const useSettingsStore = defineStore('settings', () => {
  // 状态
  const currentCluster = ref<Cluster | null>(null)
  const clusters = ref<Cluster[]>([])
  const sidebarCollapsed = ref(false)
  const activeFirstMenu = ref<string>('')  // 使用ref正确声明
  
  // 服务菜单相关状态
  const isCluster = ref(false)
  const clusterId = ref<string | number | null>(null)
  const menuData = ref<MenuItem[]>([])
  
  // 方法
  function setCurrentCluster(cluster: Cluster) {
    currentCluster.value = cluster
  }
  
  function setClusters(newClusters: Cluster[]) {
    clusters.value = newClusters
  }
  
  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }
  
  function setActiveFirstMenu(key: string) {
    activeFirstMenu.value = key  // 使用.value访问ref的值
  }
  
  // 设置是否为集群状态
  function setIsCluster(value: boolean) {
    isCluster.value = value
  }
  
  // 设置当前集群ID
  function setClusterId(id: string | number | null) {
    clusterId.value = id
  }
  
  // 设置菜单数据
  function setMenuData(data: MenuItem[]) {
    menuData.value = data
  }
  
  // 初始化
  function init() {
    // 初始化集群列表
    clusters.value = [
      { id: '1', name: '测试集群1', depType: 'K8S', status: 'running' },
      { id: '2', name: '生产环境', depType: 'Linux', status: 'running' },
      { id: '3', name: '开发环境', depType: 'Linux', status: 'warning' as any }
    ]
    
    // 设置默认集群
    if (clusters.value.length > 0) {
      currentCluster.value = clusters.value[0]
    }
  }
  
  // 初始调用
  init()
  
  return {
    // 状态
    currentCluster,
    clusters,
    sidebarCollapsed,
    activeFirstMenu,
    isCluster,
    clusterId,
    menuData,
    
    // 方法
    setCurrentCluster,
    setClusters,
    toggleSidebar,
    setActiveFirstMenu,
    setIsCluster,
    setClusterId,
    setMenuData
  }
}) 