import { ref, computed } from 'vue'
import { useLocalStorage } from '@vueuse/core'
import axios from 'axios'

// 认证接口定义
interface AuthUser {
  id?: number | string
  username?: string
  email?: string
  userType?: number
  [key: string]: any
}

interface AuthState {
  token: string | null
  user: AuthUser | null
  isLoading: boolean
}

// 创建认证组合式API
export function useAuth() {
  // 使用localStorage持久化认证状态
  const token = useLocalStorage<string | null>('auth_token', null)
  const user = useLocalStorage<AuthUser | null>('auth_user', null)
  const isLoading = ref(false)
  
  // 计算属性
  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.userType === 1 || false)
  
  // 登录方法
  const login = async (credentials: { username: string; password: string }): Promise<boolean> => {
    isLoading.value = true
    
    try {
      const response = await axios.post('/ddh/api/login', credentials, {
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        transformRequest: [(data) => {
          const params = new URLSearchParams()
          for (const key in data) {
            params.append(key, data[key])
          }
          return params.toString()
        }]
      })
      
      if (response.data && response.data.code === 200) {
        console.log('[Auth] 登录成功, 解析返回数据')
        
        // 提取token和用户信息
        const responseData = response.data
        let tokenValue = responseData.data?.token
        const userData = responseData.data?.user || {}
        
        if (!tokenValue) {
          throw new Error('登录成功但未返回token')
        }
        
        // 保存认证状态
        tokenValue = tokenValue.trim()
        token.value = tokenValue
        user.value = userData
        
        // 兼容旧系统 - 同时设置旧的token键
        localStorage.setItem('token', tokenValue)
        
        console.log('[Auth] 认证状态已保存')
        return true
      } else {
        throw new Error(response.data?.msg || '登录失败')
      }
    } catch (error) {
      console.error('[Auth] 登录失败:', error)
      logout() // 确保清除任何可能的部分状态
      return false
    } finally {
      isLoading.value = false
    }
  }
  
  // 注销方法
  const logout = () => {
    token.value = null
    user.value = null
    // 兼容旧系统 - 同时清除旧的token键
    localStorage.removeItem('token')
    console.log('[Auth] 已注销')
  }
  
  // 获取认证头
  const getAuthHeader = () => {
    if (!token.value) return {}
    
    return {
      'Authorization': token.value.startsWith('Bearer ') 
        ? token.value.trim() 
        : `Bearer ${token.value.trim()}`
    }
  }
  
  return {
    // 状态
    token,
    user,
    isLoading,
    isAuthenticated,
    isAdmin,
    
    // 方法
    login,
    logout,
    getAuthHeader
  }
}

// 创建全局单例实例
export const authService = useAuth() 