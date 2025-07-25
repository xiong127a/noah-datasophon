import { toast as sonnerToast } from 'vue-sonner';

/**
 * Vue Sonner Toast通知系统
 * 提供与原系统兼容的消息提示功能，同时扩展了更多功能
 */
export function useVueSonner() {
  /**
   * 添加一条Toast消息
   * @param message 消息内容
   * @param type 消息类型
   * @param duration 显示时长(毫秒)
   * @returns toast对象的ID
   */
  const add = (
    message: string,
    type: 'info' | 'success' | 'error' | 'warning' = 'info',
    duration: number = 3000
  ): number => {
    const id = Math.floor(Math.random() * 1000000);
    
    const options = {
      duration,
      id
    };
    
    switch (type) {
      case 'success':
        sonnerToast.success(message, options);
        break;
      case 'error':
        sonnerToast.error(message, options);
        break;
      case 'warning':
        sonnerToast.warning(message, options);
        break;
      default:
        sonnerToast(message, options);
        break;
    }
    
    return id;
  };
  
  /**
   * 移除指定ID的Toast消息
   * @param id Toast ID
   */
  const remove = (id: number): void => {
    sonnerToast.dismiss(id);
  };
  
  /**
   * 清除所有Toast消息
   */
  const clear = (): void => {
    sonnerToast.dismiss();
  };
  
  // 提供各种类型的快捷方法，与原toast系统兼容
  const toast = {
    success: (message: string, duration?: number) => add(message, 'success', duration),
    error: (message: string, duration?: number) => add(message, 'error', duration),
    warning: (message: string, duration?: number) => add(message, 'warning', duration),
    info: (message: string, duration?: number) => add(message, 'info', duration),
    // 额外提供vue-sonner特有的方法
    promise: sonnerToast.promise,
    loading: sonnerToast.loading,
    custom: sonnerToast.custom
  };
  
  return {
    toasts: [], // 为了兼容原接口，提供空数组
    add,
    remove,
    clear,
    toast
  };
}

// 创建一个全局单例实例
const vueSonnerInstance = useVueSonner();
export { vueSonnerInstance as toast }; 