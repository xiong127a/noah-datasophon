import { ref, computed } from 'vue'

interface Toast {
  id: string
  message: string
  type: 'success' | 'error' | 'info' | 'warning'
  duration: number
  createdAt: number
}

// 创建一个简单的事件总线
const toastBus = {
  callbacks: {} as Record<string, Function[]>,
  
  on(event: string, callback: Function) {
    if (!this.callbacks[event]) {
      this.callbacks[event] = []
    }
    this.callbacks[event].push(callback)
  },
  
  emit(event: string, data?: any) {
    const callbacks = this.callbacks[event]
    if (callbacks) {
      callbacks.forEach(callback => callback(data))
    }
  }
}

export function useToast() {
  const toasts = ref<Toast[]>([])
  
  // 生成唯一ID
  const generateId = () => {
    return Date.now().toString(36) + Math.random().toString(36).substring(2, 9)
  }
  
  // 添加新的toast
  const addToast = (message: string, type: Toast['type'], duration: number = 3000) => {
    const id = generateId()
    const newToast: Toast = {
      id,
      message,
      type,
      duration,
      createdAt: Date.now()
    }
    
    toasts.value.push(newToast)
    
    // 自动移除
    if (duration > 0) {
      setTimeout(() => {
        removeToast(id)
      }, duration)
    }
    
    return id
  }
  
  // 从列表中移除toast
  const removeToast = (id: string) => {
    const index = toasts.value.findIndex(t => t.id === id)
    if (index !== -1) {
      toasts.value.splice(index, 1)
    }
  }
  
  // 快捷方法
  const toast = (message: string, type: Toast['type'] = 'info', duration: number = 3000) => {
    return addToast(message, type, duration)
  }
  
  toast.success = (message: string, duration: number = 3000) => {
    return addToast(message, 'success', duration)
  }
  
  toast.error = (message: string, duration: number = 3000) => {
    return addToast(message, 'error', duration)
  }
  
  toast.info = (message: string, duration: number = 3000) => {
    return addToast(message, 'info', duration)
  }
  
  toast.warning = (message: string, duration: number = 3000) => {
    return addToast(message, 'warning', duration)
  }
  
  // 全局访问方式
  // 从任何地方调用 window.showToast('消息', 'success')
  if (typeof window !== 'undefined') {
    window.showToast = (message: string, type: Toast['type'] = 'info', duration: number = 3000) => {
      toastBus.emit('show-toast', { message, type, duration })
      return null // 全局调用时不返回id
    }
  }
  
  // 监听全局事件
  toastBus.on('show-toast', (data: { message: string, type: Toast['type'], duration: number }) => {
    const { message, type, duration } = data
    addToast(message, type, duration)
  })
  
  return {
    toasts,
    toast,
    addToast,
    removeToast
  }
}

// 为window对象添加showToast方法
declare global {
  interface Window {
    showToast: (message: string, type?: 'success' | 'error' | 'info' | 'warning', duration?: number) => string | null
  }
} 