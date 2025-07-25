import { ref } from 'vue'
import { useToast } from './useToast'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

// 错误处理器接口
type ErrorHandlerOptions = {
  defaultMessage?: string
  redirectOnAuthError?: boolean
  showToast?: boolean
  logError?: boolean
}

// 默认选项
const defaultOptions: ErrorHandlerOptions = {
  defaultMessage: '操作失败，请稍后再试',
  redirectOnAuthError: true,
  showToast: true,
  logError: true
}

/**
 * 错误处理工具
 * 提供统一的错误处理逻辑和API调用包装
 */
export const errorHandler = {
  // 当前是否正在处理错误（防止多次弹窗）
  isHandlingError: ref(false),

  /**
   * 包装异步操作，统一处理错误
   * @param asyncFn 需要执行的异步函数
   * @param options 错误处理选项
   */
  async withErrorHandling<T>(
    asyncFn: () => Promise<T>,
    options: ErrorHandlerOptions = {}
  ): Promise<T | null> {
    // 合并选项
    const finalOptions = { ...defaultOptions, ...options }
    
    try {
      return await asyncFn()
    } catch (error: any) {
      this.handleError(error, finalOptions)
      return null
    }
  },

  /**
   * 处理错误
   * @param error 错误对象
   * @param options 错误处理选项
   */
  handleError(error: any, options: ErrorHandlerOptions = {}): void {
    // 防止多个错误同时处理
    if (this.isHandlingError.value) return
    
    // 合并选项
    const finalOptions = { ...defaultOptions, ...options }
    const { toast } = useToast()
    
    try {
      this.isHandlingError.value = true
      
      // 记录错误
      if (finalOptions.logError) {
        console.error('[API Error]', error)
      }
      
      // 获取错误消息
      let errorMessage = finalOptions.defaultMessage as string
      let isActualAuthError = false
      
      // 尝试从错误对象中提取更详细的消息
      if (error) {
        // 更精确地判断是否为认证错误 - 只有真正的认证问题才会触发登出
        if (
          // 明确标记为认证错误
          error.isAuthError || 
          // 响应状态码为401未授权
          (error.response && error.response.status === 401) || 
          // 错误消息包含明确的认证失败关键词
          (error.message && 
           (error.message.includes('登录已过期') || 
            error.message.includes('token无效') || 
            error.message.includes('未授权') ||
            error.message.includes('unauthorized'))
          )
        ) {
          isActualAuthError = true
          errorMessage = error.message || '登录已过期或权限不足'
          
          // 记录认证错误详情
          console.log('[Auth Error] 检测到认证错误:', error)
        } else if (error.response?.data?.msg) {
          // API响应中的错误消息
          errorMessage = error.response.data.msg
        } else if (error.msg) {
          // 直接包含msg字段的错误
          errorMessage = error.msg
        } else if (error.message) {
          // 标准Error对象
          errorMessage = error.message
        }
      }
      
      // 显示错误消息
      if (finalOptions.showToast) {
        toast.error(errorMessage)
      }
      
      // 只有在确实是认证错误时才考虑重定向
      if (isActualAuthError && finalOptions.redirectOnAuthError) {
        const router = useRouter()
        const userStore = useUserStore()
        
        // 检查是否正在登出过程中，避免重复登出
        if (!userStore.isLoggingOut) {
          console.log('[Auth] 认证错误触发登出流程')
          // 使用静默登出，不调用API
          userStore.logout({ silent: true })
          router.push('/login')
        } else {
          console.log('[Auth] 已在登出过程中，跳过重复登出')
        }
      }
    } finally {
      // 延迟重置错误处理状态，防止过快连续触发
      setTimeout(() => {
        this.isHandlingError.value = false
      }, 300)
    }
  }
} 