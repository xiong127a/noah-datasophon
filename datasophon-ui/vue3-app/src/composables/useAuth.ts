import { computed, ref, watch } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * 认证服务 - 提供认证状态管理和JWT处理
 */
const token = ref<string | null>(null)
const isAuthenticated = computed(() => !!token.value)

// 跟踪上次认证检查时间
const lastAuthCheck = ref(Date.now())

/**
 * 认证服务 
 * 提供登录、登出和认证状态管理功能
 */
export const authService = {
  // 公开响应式状态
  token,
  isAuthenticated,
  lastAuthCheck,
  
  /**
   * 初始化认证状态
   * 从localStorage加载token
   */
  init() {
    // 从本地存储中获取令牌
    const savedToken = localStorage.getItem('auth_token')
    if (savedToken) {
      token.value = savedToken
      console.log('[Auth] Token loaded from storage')
      
      // 更新最后认证检查时间
      this.updateLastAuthCheck()
    }
  },
  
  /**
   * 设置认证令牌
   * @param newToken 新的JWT令牌
   */
  setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('auth_token', newToken)
    console.log('[Auth] Token updated')
    
    // 更新最后认证检查时间
    this.updateLastAuthCheck()
  },
  
  /**
   * 获取认证头部
   * 用于请求拦截器
   */
  getAuthHeader() {
    return {
      Authorization: token.value ? `Bearer ${token.value}` : ''
    }
  },
  
  /**
   * 更新最后认证检查时间
   */
  updateLastAuthCheck() {
    lastAuthCheck.value = Date.now()
  },
  
  /**
   * 登出 - 清除认证状态
   */
  logout() {
    // 获取user store以便清除用户信息
    const userStore = useUserStore()
    
    // 清除token
    token.value = null
    localStorage.removeItem('auth_token')
    
    // 清除用户信息
    userStore.clearUser()
    
    console.log('[Auth] User logged out')
  }
}

// 初始化认证服务
authService.init()

/**
 * 导出组合式函数
 * 提供使用认证服务的简便方法
 */
export function useAuth() {
  // 获取user store
  const userStore = useUserStore()
  
  /**
   * 检查认证状态
   * @returns 当前认证状态
   */
  const checkAuth = () => {
    // 如果令牌存在，更新最后检查时间并返回true
    if (token.value) {
      authService.updateLastAuthCheck()
      return true
    }
    return false
  }
  
  return {
    // 暴露响应式状态
    token,
    isAuthenticated,
    lastAuthCheck,
    
    // 暴露方法
    login: (newToken: string) => {
      authService.setToken(newToken)
    },
    logout: authService.logout,
    checkAuth,
    getAuthHeader: authService.getAuthHeader
  }
}

// 默认导出组合式函数
export default useAuth 