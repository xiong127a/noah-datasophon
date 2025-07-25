import { ref } from 'vue'
import { useUserStore } from '../stores/user'

// 创建响应式状态
const token = ref<string | null>(null)
const isAuthenticated = ref(false)
const lastAuthCheck = ref(Date.now())

/**
 * 认证服务
 * 提供token管理和认证状态检查
 */
export const authService = {
  // 直接导出响应式状态
  token,
  isAuthenticated,
  lastAuthCheck,
  
  /**
   * 初始化认证服务
   */
  init() {
    // 从localStorage获取token
    const storedToken = localStorage.getItem('auth_token')
    if (storedToken) {
      token.value = storedToken
      isAuthenticated.value = true
      console.log('[Auth] 从localStorage恢复token')
    } else {
      console.log('[Auth] localStorage中没有找到token')
    }
    
    // 设置事件监听器以在其他标签页中同步token
    window.addEventListener('storage', (event) => {
      if (event.key === 'auth_token') {
        if (event.newValue) {
          token.value = event.newValue
          isAuthenticated.value = true
          console.log('[Auth] 从其他标签页同步token')
        } else {
          token.value = null
          isAuthenticated.value = false
          console.log('[Auth] 从其他标签页清除token')
        }
      }
    })
  },
  
  /**
   * 设置token
   * @param newToken 新token
   */
  setToken(newToken: string) {
    // 获取userStore确保token状态同步
    const userStore = useUserStore()
    
    token.value = newToken
    isAuthenticated.value = !!newToken
    localStorage.setItem('auth_token', newToken)
    
    // 同步到用户store
    userStore.setToken(newToken)
    
    console.log('[Auth] Token已设置并同步到userStore')
  },
  
  /**
   * 获取认证头
   * @returns 带有Authorization的头信息
   */
  getAuthHeader() {
    try {
      // 优先使用userStore中的token
      const userStore = useUserStore()
      let authToken = userStore.token
      
      // 如果userStore中没有token，尝试从localStorage获取
      if (!authToken) {
        authToken = localStorage.getItem('auth_token')
        
        // 如果在localStorage中找到token，同步到userStore
        if (authToken) {
          console.log('[Auth] 从localStorage同步token到userStore')
          userStore.setToken(authToken)
          token.value = authToken
          isAuthenticated.value = true
        }
      }
      
      // 使用token构建认证头
      if (authToken) {
        console.log('[Auth] 构建认证头：Bearer ' + authToken.substring(0, 10) + '...')
        return {
          Authorization: `Bearer ${authToken}`
        }
      }
      
      console.log('[Auth] 无token，返回空认证头')
      return {}
    } catch (error) {
      console.error('[Auth] 获取认证头失败:', error)
      return {}
    }
  },
  
  /**
   * 更新最后认证检查时间
   */
  updateLastAuthCheck() {
    lastAuthCheck.value = Date.now()
  },
  
  /**
   * 登出 - 调用集中的userStore登出方法
   */
  logout() {
    // 获取user store以便统一登出
    const userStore = useUserStore()
    
    // 使用userStore统一登出，避免重复调用
    userStore.logout()
    
    // 清除本地状态
    token.value = null
    isAuthenticated.value = false
    
    console.log('[Auth] 调用userStore登出')
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
    // 优先使用userStore的认证状态
    if (userStore.isLoggedIn) {
      authService.updateLastAuthCheck()
      isAuthenticated.value = true
      return true
    }
    
    // 如果userStore没有token，但本地有token
    if (token.value) {
      // 同步token到userStore
      userStore.setToken(token.value)
      authService.updateLastAuthCheck()
      isAuthenticated.value = true
      return true
    }
    
    isAuthenticated.value = false
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
    // 检查userStore是否已经在登出过程中
    if (userStore.isLoggingOut) {
      console.log('[Auth] 正在登出过程中，跳过token验证')
      return false
    }
    
    // 如果没有token，直接返回false
    if (!userStore.token && !token.value) return false
    
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
    
    // 认证服务方法
    checkAuth,
    validateToken,
    
    // 直接使用authService的方法
    init: authService.init,
    setToken: authService.setToken,
    getAuthHeader: authService.getAuthHeader,
    updateLastAuthCheck: authService.updateLastAuthCheck,
    
    // 使用userStore的登出方法
    logout: userStore.logout
  }
} 