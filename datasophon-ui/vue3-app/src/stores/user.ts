import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { axiosPost, axiosGet } from '../utils/request.js'

interface UserInfo {
  id?: string
  username?: string
  avatar?: string
  roles?: string[]
  userType?: number // 1 = admin, 2 = regular user, etc.
  [key: string]: any // 允许更多属性
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo>(JSON.parse(localStorage.getItem('userInfo') || '{}'))
  const loading = ref(false)
  
  // 计算属性
  const user = computed(() => userInfo.value)
  const isAdmin = computed(() => userInfo.value?.userType === 1 || userInfo.value?.roles?.includes('ADMIN'))
  const isLoggedIn = computed(() => !!token.value && !!userInfo.value?.id)
  
  function setToken(value: string) {
    token.value = value
    localStorage.setItem('token', value)
  }
  
  function setUserInfo(info: UserInfo) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }
  
  async function login(loginForm: { username: string; password: string }) {
    loading.value = true
    
    try {
      // 调用登录API - 使用完整路径
      const res = await axiosPost('/ddh/api/login', loginForm)
      
      if (res && res.code === 200) {
        // 设置token和用户信息
        setToken(res.data.token)
        setUserInfo(res.data.user)
        return res.data
      } else {
        throw new Error(res?.msg || '登录失败')
      }
    } finally {
      loading.value = false
    }
  }
  
  async function getUserInfo() {
    if (!token.value) return null
    
    try {
      // 获取用户信息API
      const res = await axiosGet('/ddh/api/user-info')
      
      if (res && res.code === 200) {
        setUserInfo(res.data)
        return res.data
      }
      return null
    } catch (error) {
      console.error('获取用户信息失败', error)
      return null
    }
  }
  
  async function logout() {
    try {
      if (token.value) {
        // 调用登出API
        await axiosPost('/ddh/api/logout')
      }
    } catch (error) {
      console.error('登出失败', error)
    } finally {
      // 无论API是否成功，都清除本地存储
      token.value = ''
      userInfo.value = {}
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }
  
  return {
    token,
    userInfo,
    user,
    isAdmin,
    isLoggedIn,
    loading,
    setToken,
    setUserInfo,
    login,
    getUserInfo,
    logout
  }
}) 