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

export const useSettingsStore = defineStore('settings', () => {
  // 状态
  const currentCluster = ref<Cluster | null>(null)
  const clusters = ref<Cluster[]>([])
  const sidebarCollapsed = ref(false)
  const activeFirstMenu = ref<string>('')  // 使用ref正确声明
  
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
    currentCluster,
    clusters,
    sidebarCollapsed,
    activeFirstMenu,
    setCurrentCluster,
    setClusters,
    toggleSidebar,
    setActiveFirstMenu
  }
}) 