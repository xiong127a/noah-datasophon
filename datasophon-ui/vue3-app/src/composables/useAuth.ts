import { computed, ref, watch } from 'vue'
import { useUserStore } from '../stores/user'

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
    console.log('[Auth] Token updated and saved to localStorage')
    
    // 更新最后认证检查时间
    this.updateLastAuthCheck()
  },
  
  /**
   * 获取认证头部
   * 用于请求拦截器
   */
  getAuthHeader() {
    // 先尝试从内存中获取token
    let currentToken = token.value;
    
    // 如果内存中没有，尝试从localStorage中获取
    if (!currentToken) {
      currentToken = localStorage.getItem('auth_token');
      
      // 如果在localStorage中找到了token，同步到内存中
      if (currentToken) {
        token.value = currentToken;
        console.log('[Auth] Token synchronized from localStorage');
      }
    }
    
    // 返回带前缀的认证头
    return {
      Authorization: currentToken ? `Bearer ${currentToken}` : ''
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
  
  // 检查token有效期（10小时）- 避免频繁调用getUserInfo
  const tokenTtl = 1000 * 60 * 60 * 10 // 10小时
  
  /**
   * 验证token是否仍然有效
   * 如果距离上次验证不到10小时，则认为有效
   * 这样可以避免频繁调用getUserInfo
   */
  const validateToken = async () => {
    // 如果没有token，直接返回false
    if (!token.value) return false
    
    // 检查上次认证时间
    const now = Date.now()
    const timeSinceLastCheck = now - lastAuthCheck.value
    
    // 如果距离上次认证检查不到设定的TTL，则认为token仍然有效
    if (timeSinceLastCheck < tokenTtl) {
      console.log(`[Auth] Token still valid, last check was ${Math.round(timeSinceLastCheck / 1000 / 60)} minutes ago`)
      return true
    }
    
    // 如果超过TTL，则尝试使用getUserInfo验证token有效性
    console.log(`[Auth] Token validation needed, last check was ${Math.round(timeSinceLastCheck / 1000 / 60)} minutes ago`)
    
    try {
      const user = await userStore.getUserInfo()
      if (user) {
        // 更新最后检查时间
        authService.updateLastAuthCheck()
        return true
      }
    } catch (error) {
      console.error('[Auth] Token validation failed:', error)
    }
    
    // 验证失败
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
    validateToken,
    getAuthHeader: authService.getAuthHeader
  }
}

// 默认导出组合式函数
export default useAuth 