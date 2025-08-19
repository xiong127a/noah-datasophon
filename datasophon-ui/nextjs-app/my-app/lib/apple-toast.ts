import { toast, type ExternalToast } from 'sonner'

/**
 * 苹果样式的Toast通知工具
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-20
 */

// 苹果样式的基础配置
const APPLE_BASE_STYLE = {
  background: 'rgba(255, 255, 255, 0.95)',
  backdropFilter: 'blur(20px)',
  borderRadius: '16px',
  color: '#1d1d1f',
  fontSize: '14px',
  fontWeight: '500',
  border: '1px solid rgba(0, 0, 0, 0.08)',
  boxShadow: '0 8px 32px rgba(0, 0, 0, 0.12), 0 2px 8px rgba(0, 0, 0, 0.08)',
  padding: '12px 16px',
  minWidth: '320px',
  maxWidth: '480px',
} as const

// 不同类型的样式配置
const APPLE_TOAST_STYLES = {
  success: {
    ...APPLE_BASE_STYLE,
    border: '1px solid rgba(52, 199, 89, 0.2)',
    boxShadow: '0 8px 32px rgba(52, 199, 89, 0.15), 0 2px 8px rgba(0, 0, 0, 0.08)',
  },
  error: {
    ...APPLE_BASE_STYLE,
    border: '1px solid rgba(255, 59, 48, 0.2)',
    boxShadow: '0 8px 32px rgba(255, 59, 48, 0.15), 0 2px 8px rgba(0, 0, 0, 0.08)',
  },
  warning: {
    ...APPLE_BASE_STYLE,
    border: '1px solid rgba(255, 149, 0, 0.2)',
    boxShadow: '0 8px 32px rgba(255, 149, 0, 0.15), 0 2px 8px rgba(0, 0, 0, 0.08)',
  },
  info: {
    ...APPLE_BASE_STYLE,
    border: '1px solid rgba(0, 122, 255, 0.2)',
    boxShadow: '0 8px 32px rgba(0, 122, 255, 0.15), 0 2px 8px rgba(0, 0, 0, 0.08)',
  },
} as const

// 默认配置
const DEFAULT_OPTIONS: ExternalToast = {
  duration: 4000,
  position: 'top-center',
  dismissible: true,
  closeButton: false,
}

/**
 * 苹果样式的成功通知
 */
export const appleToast = {
  success: (message: string, options?: ExternalToast) => {
    return toast.success(message, {
      ...DEFAULT_OPTIONS,
      ...options,
      style: APPLE_TOAST_STYLES.success,
      className: 'apple-toast apple-toast-success',
    })
  },

  error: (message: string, options?: ExternalToast) => {
    return toast.error(message, {
      ...DEFAULT_OPTIONS,
      duration: 6000, // 错误信息显示时间长一些
      ...options,
      style: APPLE_TOAST_STYLES.error,
      className: 'apple-toast apple-toast-error',
    })
  },

  warning: (message: string, options?: ExternalToast) => {
    return toast.warning(message, {
      ...DEFAULT_OPTIONS,
      duration: 5000,
      ...options,
      style: APPLE_TOAST_STYLES.warning,
      className: 'apple-toast apple-toast-warning',
    })
  },

  info: (message: string, options?: ExternalToast) => {
    return toast.info(message, {
      ...DEFAULT_OPTIONS,
      ...options,
      style: APPLE_TOAST_STYLES.info,
      className: 'apple-toast apple-toast-info',
    })
  },

  loading: (message: string, options?: ExternalToast) => {
    return toast.loading(message, {
      ...DEFAULT_OPTIONS,
      ...options,
      style: APPLE_TOAST_STYLES.info,
      className: 'apple-toast apple-toast-loading',
    })
  },

  promise: <T,>(
    promise: Promise<T>,
    options: {
      loading: string
      success: string | ((data: T) => string)
      error: string | ((error: Error) => string)
    }
  ) => {
    return toast.promise(promise, options)
  },
}

/**
 * API错误专用的通知函数
 */
export const apiErrorToast = {
  /**
   * 业务错误通知
   */
  business: (message: string, details?: { url?: string; code?: number }) => {
    console.error('API业务错误:', { message, ...(details || {}) })
    return appleToast.error(`❌ ${message}`, {
      description: details?.url ? `请求: ${details.url}` : undefined,
      duration: 6000,
      position: 'top-center',
      closeButton: false,
    })
  },

  /**
   * 网络错误通知
   */
  network: (message: string, status?: number) => {
    console.error('API网络错误:', { message, status: status || 'unknown' })
    return appleToast.error(`🌐 ${message}`, {
      description: status ? `状态码: ${status}` : undefined,
      duration: 6000,
      position: 'top-center',
      closeButton: false,
    })
  },

  /**
   * 认证错误通知
   */
  auth: (message: string = '登录已过期，请重新登录') => {
    console.error('API认证错误:', message)
    return appleToast.error(`🔐 ${message}`, {
      duration: 8000,
      position: 'top-center',
      closeButton: false,
    })
  },

  /**
   * 权限错误通知
   */
  permission: (message: string = '没有权限访问此资源') => {
    console.error('API权限错误:', message)
    return appleToast.error(`🚫 ${message}`, {
      duration: 6000,
      position: 'top-center',
      closeButton: false,
    })
  },
}

/**
 * 操作反馈专用的通知函数
 */
export const operationToast = {
  /**
   * 操作成功
   */
  success: (operation: string, details?: string) => {
    const message = `✅ ${operation}成功`
    return appleToast.success(message, {
      description: details,
      duration: 3000,
      position: 'top-center',
      closeButton: false,
    })
  },

  /**
   * 操作失败
   */
  failed: (operation: string, reason?: string) => {
    const message = `❌ ${operation}失败`
    return appleToast.error(message, {
      description: reason,
      duration: 5000,
      position: 'top-center',
      closeButton: false,
    })
  },

  /**
   * 操作进行中
   */
  loading: (operation: string) => {
    const message = `⏳ ${operation}中...`
    return appleToast.loading(message, {
      position: 'top-center',
      closeButton: false,
    })
  },

  /**
   * 操作警告
   */
  warning: (operation: string, warning: string) => {
    const message = `⚠️ ${operation}警告`
    return appleToast.warning(message, {
      description: warning,
      duration: 5000,
      position: 'top-center',
      closeButton: false,
    })
  },
}

// 导出默认的apple toast
export default appleToast
