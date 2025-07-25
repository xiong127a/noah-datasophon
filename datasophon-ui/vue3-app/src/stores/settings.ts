import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 菜单项类型定义
 */
interface MenuItem {
  id?: string | number;
  name?: string;
  title?: string;
  path: string;
  icon?: string;
  children?: MenuItem[];
  [key: string]: any;
}

/**
 * 设置状态Store
 * 管理应用程序设置和全局状态
 */
export const useSettingsStore = defineStore('settings', () => {
  // 集群相关状态
  const isCluster = ref(false)
  const clusterId = ref('')
  const menuData = ref<MenuItem[]>([])
  
  // 菜单状态
  const activeFirstMenu = ref(localStorage.getItem('activeFirstMenu') || '/')
  
  // 主题设置
  const theme = ref(localStorage.getItem('theme') || 'light')
  const primaryColor = ref(localStorage.getItem('primaryColor') || '#1890ff')
  
  // 布局设置
  const layout = ref(localStorage.getItem('layout') || 'side')
  const navTheme = ref(localStorage.getItem('navTheme') || 'dark')
  const contentWidth = ref(localStorage.getItem('contentWidth') || 'fixed')
  const fixedHeader = ref(localStorage.getItem('fixedHeader') === 'true')
  
  // 多语言设置
  const locale = ref(localStorage.getItem('locale') || 'zh-CN')
  
  // 设置集群相关状态
  function setIsCluster(value: boolean) {
    isCluster.value = value
  }
  
  function setClusterId(id: string) {
    clusterId.value = id
  }
  
  function setMenuData(data: MenuItem[]) {
    menuData.value = data
  }
  
  // 设置当前激活的一级菜单
  function setActiveFirstMenu(path: string) {
    activeFirstMenu.value = path
    localStorage.setItem('activeFirstMenu', path)
  }
  
  // 设置主题
  function setTheme(value: string) {
    theme.value = value
    localStorage.setItem('theme', value)
  }
  
  // 设置主色调
  function setPrimaryColor(value: string) {
    primaryColor.value = value
    localStorage.setItem('primaryColor', value)
    
    // 实际应用主题色变量
    document.documentElement.style.setProperty('--primary-color', value)
  }
  
  // 设置布局
  function setLayout(value: string) {
    layout.value = value
    localStorage.setItem('layout', value)
  }
  
  // 设置导航栏主题
  function setNavTheme(value: string) {
    navTheme.value = value
    localStorage.setItem('navTheme', value)
  }
  
  // 设置内容区宽度
  function setContentWidth(value: string) {
    contentWidth.value = value
    localStorage.setItem('contentWidth', value)
  }
  
  // 设置固定头部
  function setFixedHeader(value: boolean) {
    fixedHeader.value = value
    localStorage.setItem('fixedHeader', value.toString())
  }
  
  // 设置语言
  function setLocale(value: string) {
    locale.value = value
    localStorage.setItem('locale', value)
  }
  
  // 重置所有设置到默认值
  function resetSettings() {
    // 重置主题设置
    setTheme('light')
    setPrimaryColor('#1890ff')
    
    // 重置布局设置
    setLayout('side')
    setNavTheme('dark')
    setContentWidth('fixed')
    setFixedHeader(false)
    
    // 重置多语言设置
    setLocale('zh-CN')
  }
  
  return {
    // 集群相关状态
    isCluster,
    clusterId,
    menuData,
    
    // 菜单状态
    activeFirstMenu,
    
    // 主题设置
    theme,
    primaryColor,
    
    // 布局设置
    layout,
    navTheme,
    contentWidth,
    fixedHeader,
    
    // 多语言设置
    locale,
    
    // 设置方法
    setIsCluster,
    setClusterId,
    setMenuData,
    setActiveFirstMenu,
    setTheme,
    setPrimaryColor,
    setLayout,
    setNavTheme,
    setContentWidth,
    setFixedHeader,
    setLocale,
    resetSettings
  }
}) 