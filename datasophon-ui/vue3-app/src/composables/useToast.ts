import { ref } from 'vue'

interface Toast {
  id: number
  message: string
  type: 'success' | 'error' | 'warning' | 'info'
  duration: number
}

// 创建全局共享的状态
const toasts = ref<Toast[]>([])
let nextId = 0

/**
 * Toast通知系统
 * 提供简单易用的消息提示功能
 */
export function useToast() {
  /**
   * 添加一条Toast消息
   * @param message 消息内容
   * @param type 消息类型
   * @param duration 显示时长(毫秒)
   * @returns toast对象的ID
   */
  const add = (
    message: string,
    type: Toast['type'] = 'info',
    duration: number = 3000
  ): number => {
    const id = nextId++
    const toast: Toast = {
      id,
      message,
      type,
      duration
    }
    
    // 添加到列表
    toasts.value.push(toast)
    
    // 设置自动移除
    if (duration > 0) {
      setTimeout(() => {
        remove(id)
      }, duration)
    }
    
    return id
  }
  
  /**
   * 移除指定ID的Toast消息
   * @param id Toast ID
   */
  const remove = (id: number): void => {
    const index = toasts.value.findIndex(t => t.id === id)
    if (index !== -1) {
      toasts.value.splice(index, 1)
    }
  }
  
  /**
   * 清除所有Toast消息
   */
  const clear = (): void => {
    toasts.value = []
  }
  
  // 提供各种类型的快捷方法
  const toast = {
    success: (message: string, duration?: number) => add(message, 'success', duration),
    error: (message: string, duration?: number) => add(message, 'error', duration),
    warning: (message: string, duration?: number) => add(message, 'warning', duration),
    info: (message: string, duration?: number) => add(message, 'info', duration)
  }
  
  return {
    toasts,
    add,
    remove,
    clear,
    toast
  }
}

// 创建一个全局单例实例
const toastInstance = useToast()
export { toastInstance as toast } 