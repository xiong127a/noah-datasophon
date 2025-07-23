/**
 * Vuex模块 - 设置
 * 用于存储应用全局设置和集群相关的设置信息
 */

const state = {
  isCluster: false, // 是否是集群模式
  clusterId: '', // 当前集群ID
  menuData: [], // 服务菜单数据
  theme: 'light', // 主题模式
  layout: 'side', // 布局模式
  primaryColor: '#1890ff', // 主题色
  fixedHeader: true, // 固定头部
  fixedSidebar: true, // 固定侧边栏
  contentWidth: 'fluid', // 内容区域宽度
  autoHideHeader: false, // 自动隐藏头部
  colorWeak: false, // 色弱模式
  multiTab: true, // 多页签模式
  showLogo: true, // 显示Logo
  showBreadcrumb: true, // 显示面包屑
  showTagsBar: true, // 显示标签栏
  showFooter: true, // 显示页脚
  showSearch: true, // 显示搜索框
  showLanguage: true, // 显示语言切换
  showUserInfo: true, // 显示用户信息
  showSettings: true, // 显示设置抽屉
  showNotice: true, // 显示通知
  showHelp: true, // 显示帮助
  showFullScreen: true, // 显示全屏
  showGlobalSearch: true, // 显示全局搜索
}

const getters = {
  isCluster: state => state.isCluster,
  clusterId: state => state.clusterId,
  menuData: state => state.menuData,
  theme: state => state.theme,
  layout: state => state.layout,
  primaryColor: state => state.primaryColor,
  fixedHeader: state => state.fixedHeader,
  fixedSidebar: state => state.fixedSidebar,
  contentWidth: state => state.contentWidth,
  autoHideHeader: state => state.autoHideHeader,
  colorWeak: state => state.colorWeak,
  multiTab: state => state.multiTab,
}

const mutations = {
  // 设置是否是集群模式
  setIsCluster(state, isCluster) {
    state.isCluster = isCluster
  },
  
  // 设置当前集群ID
  setClusterId(state, clusterId) {
    state.clusterId = clusterId
  },
  
  // 设置服务菜单数据
  setMenuData(state, menuData) {
    state.menuData = menuData
  },
  
  // 设置主题模式
  setTheme(state, theme) {
    state.theme = theme
  },
  
  // 设置布局模式
  setLayout(state, layout) {
    state.layout = layout
  },
  
  // 设置主题色
  setPrimaryColor(state, primaryColor) {
    state.primaryColor = primaryColor
  },
  
  // 设置固定头部
  setFixedHeader(state, fixedHeader) {
    state.fixedHeader = fixedHeader
  },
  
  // 设置固定侧边栏
  setFixedSidebar(state, fixedSidebar) {
    state.fixedSidebar = fixedSidebar
  },
  
  // 设置内容区域宽度
  setContentWidth(state, contentWidth) {
    state.contentWidth = contentWidth
  },
  
  // 设置自动隐藏头部
  setAutoHideHeader(state, autoHideHeader) {
    state.autoHideHeader = autoHideHeader
  },
  
  // 设置色弱模式
  setColorWeak(state, colorWeak) {
    state.colorWeak = colorWeak
  },
  
  // 设置多页签模式
  setMultiTab(state, multiTab) {
    state.multiTab = multiTab
  },
}

const actions = {
  // 保存设置到本地存储
  saveSettings({ state }) {
    localStorage.setItem('app_settings', JSON.stringify({
      theme: state.theme,
      layout: state.layout,
      primaryColor: state.primaryColor,
      fixedHeader: state.fixedHeader,
      fixedSidebar: state.fixedSidebar,
      contentWidth: state.contentWidth,
      autoHideHeader: state.autoHideHeader,
      colorWeak: state.colorWeak,
      multiTab: state.multiTab,
    }))
  },
  
  // 从本地存储加载设置
  loadSettings({ commit }) {
    const settings = localStorage.getItem('app_settings')
    if (settings) {
      try {
        const {
          theme,
          layout,
          primaryColor,
          fixedHeader,
          fixedSidebar,
          contentWidth,
          autoHideHeader,
          colorWeak,
          multiTab,
        } = JSON.parse(settings)
        
        commit('setTheme', theme)
        commit('setLayout', layout)
        commit('setPrimaryColor', primaryColor)
        commit('setFixedHeader', fixedHeader)
        commit('setFixedSidebar', fixedSidebar)
        commit('setContentWidth', contentWidth)
        commit('setAutoHideHeader', autoHideHeader)
        commit('setColorWeak', colorWeak)
        commit('setMultiTab', multiTab)
      } catch (e) {
        console.error('加载设置失败', e)
      }
    }
  },
}

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions
} 