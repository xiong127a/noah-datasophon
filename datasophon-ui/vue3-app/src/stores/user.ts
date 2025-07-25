import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { axiosPost } from '@/utils/request'
import API_PATHS from '@/api/httpApi/apiPaths'
import config from '@/config'

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
    
    // 同时保存到config.userKey下，确保兼容性
    if (config.userKey && config.userKey !== 'auth_user') {
      localStorage.setItem(config.userKey, JSON.stringify(userData))
      console.log(`[User] 同时保存用户信息到 ${config.userKey}`)
    }
  }
  
  // 清除用户和认证信息
  const clearUser = () => {
    user.value = null
    token.value = null
    localStorage.removeItem('auth_token')
    localStorage.removeItem('auth_user')
    loginError.value = null
    
    // 同时清除config.userKey下的数据，确保兼容性
    if (config.userKey && config.userKey !== 'auth_user') {
      localStorage.removeItem(config.userKey)
    }
  }
  
  // 初始化 - 从localStorage加载状态
  const initializeFromStorage = () => {
    // 加载token
    const storedToken = localStorage.getItem('auth_token')
    if (storedToken) {
      token.value = storedToken
    }
    
    // 加载用户信息，优先尝试从config.userKey加载
    let userData = null
    
    if (config.userKey) {
      const userDataFromConfig = localStorage.getItem(config.userKey)
      if (userDataFromConfig) {
        try {
          userData = JSON.parse(userDataFromConfig)
          console.log(`[User] 从 ${config.userKey} 加载用户数据`)
        } catch (e) {
          console.error(`解析 ${config.userKey} 中的用户数据失败`, e)
        }
      }
    }
    
    // 如果从config.userKey没有加载到数据，尝试从auth_user加载
    if (!userData) {
      const storedUser = localStorage.getItem('auth_user')
      if (storedUser) {
        try {
          userData = JSON.parse(storedUser)
          console.log('[User] 从 auth_user 加载用户数据')
          
          // 同步到config.userKey
          if (config.userKey && config.userKey !== 'auth_user') {
            localStorage.setItem(config.userKey, storedUser)
            console.log(`[User] 同步用户数据到 ${config.userKey}`)
          }
        } catch (e) {
          console.error('解析 auth_user 中的用户数据失败', e)
          localStorage.removeItem('auth_user')
        }
      }
    }
    
    // 设置用户数据
    if (userData) {
      user.value = userData
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
      console.log('[User] 无token，无法获取用户信息')
      return null
    }
    
    try {
      console.log('[User] 开始获取用户信息')
      const response = await axiosPost(API_PATHS.getUserInfo, {})
      
      if (response && response.code === 200) {
        // 成功获取用户信息
        console.log('[User] 获取用户信息成功:', response.data)
        
        // 保存用户信息
        setUser(response.data || {})
        
        // 返回用户信息
        return user.value
      } else {
        console.error('[User] 获取用户信息失败:', response?.msg || '未知错误')
        throw new Error(response?.msg || 'Failed to fetch user info')
      }
    } catch (error) {
      console.error('[User] 获取用户信息出错:', error)
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