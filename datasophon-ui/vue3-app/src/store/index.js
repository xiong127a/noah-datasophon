import { createStore } from 'vuex'
import account from './modules/account'
import setting from './modules/setting'

// 创建store实例
const store = createStore({
  modules: {
    account,
    setting
  },
  state: {
    appVersion: process.env.VUE_APP_VERSION || '1.0.0',
    appName: 'Datasophon',
    loading: false,
    globalMessage: '',
  },
  getters: {
    appVersion: state => state.appVersion,
    appName: state => state.appName,
    loading: state => state.loading,
    globalMessage: state => state.globalMessage,
  },
  mutations: {
    // 设置加载状态
    setLoading(state, loading) {
      state.loading = loading
    },
    
    // 设置全局消息
    setGlobalMessage(state, message) {
      state.globalMessage = message
    },
  },
  actions: {
    // 初始化应用
    initApp({ dispatch }) {
      // 从本地存储加载设置
      dispatch('setting/loadSettings')
      
      // 获取用户信息
      if (localStorage.getItem('token')) {
        dispatch('account/getUserInfo').catch(() => {
          // 如果获取用户信息失败，清空用户信息
          dispatch('account/logout')
        })
      }
    }
  }
})

export default store 