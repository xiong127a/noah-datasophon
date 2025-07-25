<template>
  <div class="toast-container">
    <TransitionGroup 
      name="toast" 
      tag="div" 
      class="fixed top-0 right-0 z-50 p-4 space-y-2 max-w-sm flex flex-col items-end"
    >
      <div 
        v-for="toast in toasts" 
        :key="toast.id" 
        class="toast bg-white shadow-lg rounded-lg overflow-hidden border-l-4 flex items-center p-4 w-full max-w-sm transform transition-all duration-300 ease-out"
        :class="{
          'border-green-500': toast.type === 'success',
          'border-red-500': toast.type === 'error',
          'border-yellow-500': toast.type === 'warning',
          'border-blue-500': toast.type === 'info'
        }"
      >
        <div class="flex-shrink-0 mr-3">
          <!-- Success icon -->
          <svg v-if="toast.type === 'success'" class="w-5 h-5 text-green-500" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"></path>
          </svg>
          
          <!-- Error icon -->
          <svg v-if="toast.type === 'error'" class="w-5 h-5 text-red-500" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"></path>
          </svg>
          
          <!-- Warning icon -->
          <svg v-if="toast.type === 'warning'" class="w-5 h-5 text-yellow-500" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd"></path>
          </svg>
          
          <!-- Info icon -->
          <svg v-if="toast.type === 'info'" class="w-5 h-5 text-blue-500" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd"></path>
          </svg>
        </div>
        
        <div class="flex-1 pr-3">
          <p class="text-sm text-gray-700">{{ toast.message }}</p>
        </div>
        
        <button 
          @click="removeToast(toast.id)" 
          class="flex-shrink-0 text-gray-400 hover:text-gray-500 focus:outline-none"
        >
          <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"></path>
          </svg>
        </button>
      </div>
    </TransitionGroup>
  </div>
</template>

<script>
import { useToast } from '@/composables/useToast'

export default {
  name: 'Toast',
  setup() {
    // 使用Toast组合式API
    const { toasts, remove: removeToast } = useToast()
    
    return {
      toasts,
      removeToast
    }
  }
}
</script>

<style scoped>
/* 进入和离开动画 */
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from {
  transform: translateX(100%);
  opacity: 0;
}

.toast-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

/* 确保消息平滑移动 */
.toast-move {
  transition: transform 0.3s ease;
}
</style> 