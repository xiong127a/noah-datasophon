import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { axiosPost } from '@/utils/request'
import API_PATHS from '@/api/httpApi/apiPaths'

// 用户接口定义
interface User {
  id?: number | string
  username?: string
  email?: string
  userType?: number
  [key: string]: any
}

// 登录凭证接口
interface LoginCredentials {
  username: string
  password: string
}

/**
 * 用户状态管理Store
 * 管理用户登录、用户信息和权限
 */
export const useUserStore = defineStore('user', () => {
  // 状态
  const user = ref<User | null>(null)
  const token = ref<string | null>(null)
  const loading = ref(false)
  const loginError = ref<string | null>(null)
  
  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.userType === 1)
  
  // 获取令牌
  const getToken = () => {
    return token.value
  }
  
  // 设置令牌
  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('auth_token', newToken)
  }
  
  // 设置用户信息
  const setUser = (userData: User) => {
    user.value = userData
    localStorage.setItem('auth_user', JSON.stringify(userData))
  }
  
  // 清除用户和认证信息
  const clearUser = () => {
    user.value = null
    token.value = null
    localStorage.removeItem('auth_token')
    localStorage.removeItem('auth_user')
    loginError.value = null
  }
  
  // 初始化 - 从localStorage加载状态
  const initializeFromStorage = () => {
    // 加载token
    const storedToken = localStorage.getItem('auth_token')
    if (storedToken) {
      token.value = storedToken
    }
    
    // 加载用户信息
    const storedUser = localStorage.getItem('auth_user')
    if (storedUser) {
      try {
        user.value = JSON.parse(storedUser)
      } catch (e) {
        console.error('Failed to parse stored user data', e)
        localStorage.removeItem('auth_user') // 清除无效数据
      }
    }
  }
  
  // 登录方法
  const login = async (credentials: LoginCredentials) => {
    loading.value = true
    loginError.value = null
    
    try {
      const response = await axiosPost(API_PATHS.login, credentials)
      
      if (response && response.code === 200) {
        // 提取token和用户信息
        const tokenValue = response.data?.token
        const userData = response.data?.user || {}
        
        if (!tokenValue) {
          throw new Error('Login successful but no token returned')
        }
        
        // 保存状态
        setToken(tokenValue)
        setUser(userData)
        
        return true
      } else {
        throw new Error(response?.msg || 'Login failed')
      }
    } catch (error: any) {
      loginError.value = error.message || 'Login failed'
      return false
    } finally {
      loading.value = false
    }
  }
  
  // 登出方法
  const logout = async (options = { silent: false }) => {
    try {
      // 只有在非静默模式下才调用登出API
      if (!options.silent && token.value) {
        console.log('[User] 调用登出API')
        await axiosPost(API_PATHS.logout, {})
      } else if (options.silent) {
        console.log('[User] 静默登出，跳过API调用')
      }
    } catch (error) {
      console.error('Logout API call failed', error)
    } finally {
      // 无论API是否成功，都清除本地状态
      clearUser()
    }
  }
  
  // 获取用户信息
  const getUserInfo = async () => {
    if (!token.value) {
      return null
    }
    
    try {
      const response = await axiosPost(API_PATHS.getUserInfo, {})
      
      if (response && response.code === 200) {
        setUser(response.data || {})
        return user.value
      } else {
        throw new Error('Failed to fetch user info')
      }
    } catch (error) {
      console.error('Error fetching user info', error)
      return null
    }
  }
  
  // 初始化状态
  initializeFromStorage()
  
  return {
    // 状态
    user,
    token,
    loading,
    loginError,
    
    // 计算属性
    isLoggedIn,
    isAdmin,
    
    // 方法
    login,
    logout,
    setUser,
    setToken,
    getToken,
    clearUser,
    getUserInfo
  }
}) 