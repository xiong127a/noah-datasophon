<template>
  <div class="toast-container">
    <TransitionGroup 
      name="toast" 
      tag="div" 
      class="fixed top-0 right-0 z-50 p-4 space-y-3 max-w-md flex flex-col items-end"
    >
      <div 
        v-for="toast in toasts" 
        :key="toast.id" 
        class="toast-card"
        :class="{
          'toast-success': toast.type === 'success',
          'toast-error': toast.type === 'error',
          'toast-warning': toast.type === 'warning',
          'toast-info': toast.type === 'info'
        }"
      >
        <!-- Toast内部容器 -->
        <div class="toast-inner">
          <!-- 图标区域 -->
          <div class="toast-icon">
            <!-- Success icon -->
            <svg v-if="toast.type === 'success'" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="10" class="toast-icon-circle"></circle>
              <path d="M8 12l3 3 6-6" class="toast-icon-path"></path>
            </svg>
            
            <!-- Error icon -->
            <svg v-if="toast.type === 'error'" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="10" class="toast-icon-circle"></circle>
              <path d="M15 9l-6 6m0-6l6 6" class="toast-icon-path"></path>
            </svg>
            
            <!-- Warning icon -->
            <svg v-if="toast.type === 'warning'" viewBox="0 0 24 24">
              <path d="M12 3l9 16H3l9-16z" class="toast-icon-path"></path>
              <path d="M12 10v4m0 3v.01" class="toast-icon-dot"></path>
            </svg>
            
            <!-- Info icon -->
            <svg v-if="toast.type === 'info'" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="10" class="toast-icon-circle"></circle>
              <path d="M12 8v5m0 3v.01" class="toast-icon-path"></path>
            </svg>
          </div>
          
          <!-- 内容区域 -->
          <div class="toast-content">
            <p class="toast-message">{{ toast.message }}</p>
          </div>
          
          <!-- 关闭按钮 -->
          <button 
            @click="removeToast(toast.id)" 
            class="toast-close"
            aria-label="关闭通知"
          >
            <svg viewBox="0 0 24 24">
              <path d="M18 6L6 18M6 6l12 12"></path>
            </svg>
          </button>
        </div>
        
        <!-- 进度条 -->
        <div class="toast-progress-container">
          <div 
            class="toast-progress-bar"
            :style="{ animationDuration: `${toast.duration}ms` }"
          ></div>
        </div>
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
/* Toast容器 */
.toast-card {
  width: 100%;
  max-width: 360px;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 
    0 4px 20px rgba(0, 0, 0, 0.1),
    0 2px 8px rgba(0, 0, 0, 0.08),
    0 0 0 1px rgba(255, 255, 255, 0.5) inset;
  position: relative;
  transform-origin: right top;
  animation: toast-pulse 2s infinite alternate ease-in-out;
}

/* 脉冲动画 */
@keyframes toast-pulse {
  0% {
    box-shadow: 
      0 4px 20px rgba(0, 0, 0, 0.1),
      0 2px 8px rgba(0, 0, 0, 0.05),
      0 0 0 1px rgba(255, 255, 255, 0.5) inset;
  }
  100% {
    box-shadow: 
      0 8px 30px rgba(0, 0, 0, 0.12),
      0 4px 12px rgba(0, 0, 0, 0.06),
      0 0 0 1px rgba(255, 255, 255, 0.6) inset;
  }
}

.toast-inner {
  display: flex;
  padding: 14px 16px;
  align-items: center;
  justify-content: flex-start;
}

/* 类型特定样式 */
.toast-success {
  background: linear-gradient(135deg, rgba(240, 253, 244, 0.98), rgba(220, 252, 231, 0.98));
  border-left: 4px solid #10B981;
}

.toast-error {
  background: linear-gradient(135deg, rgba(254, 226, 226, 0.98), rgba(254, 215, 215, 0.98));
  border-left: 4px solid #DC2626;
}

.toast-warning {
  background: linear-gradient(135deg, rgba(255, 251, 235, 0.98), rgba(254, 243, 199, 0.98));
  border-left: 4px solid #F59E0B;
}

.toast-info {
  background: linear-gradient(135deg, rgba(239, 246, 255, 0.98), rgba(219, 234, 254, 0.98));
  border-left: 4px solid #3B82F6;
}

/* 图标样式 */
.toast-icon {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  margin-right: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.toast-success .toast-icon svg {
  stroke: #10B981;
}

.toast-error .toast-icon svg {
  stroke: #DC2626;
}

.toast-warning .toast-icon svg {
  stroke: #F59E0B;
}

.toast-info .toast-icon svg {
  stroke: #3B82F6;
}

.toast-icon-circle {
  fill: transparent;
}

.toast-success .toast-icon-circle {
  stroke: rgba(16, 185, 129, 0.2);
}

.toast-error .toast-icon-circle {
  stroke: rgba(239, 68, 68, 0.2);
}

.toast-warning .toast-icon-path {
  fill: rgba(245, 158, 11, 0.2);
  stroke: #F59E0B;
}

.toast-warning .toast-icon-dot {
  fill: none;
  stroke: #F59E0B;
}

.toast-info .toast-icon-circle {
  stroke: rgba(59, 130, 246, 0.2);
}

/* 文字内容样式 */
.toast-content {
  flex: 1;
  min-width: 0;
}

.toast-message {
  margin: 0;
  font-size: 0.9rem;
  line-height: 1.5;
  color: #1F2937;
  white-space: normal;
  word-break: break-word;
}

.toast-success .toast-message {
  color: #065F46;
}

.toast-error .toast-message {
  color: #B91C1C;
  font-weight: 500;
}

.toast-warning .toast-message {
  color: #92400E;
}

.toast-info .toast-message {
  color: #1E40AF;
}

/* 关闭按钮样式 */
.toast-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: transparent;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  margin-left: auto;
  padding: 0;
  transition: background-color 0.2s, transform 0.2s;
  flex-shrink: 0;
}

.toast-close:hover {
  background-color: rgba(0, 0, 0, 0.05);
  transform: scale(1.1);
}

.toast-close:active {
  transform: scale(0.95);
}

.toast-close svg {
  width: 16px;
  height: 16px;
  stroke: #9CA3AF;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

/* 进度条样式 */
.toast-progress-container {
  height: 3px;
  width: 100%;
  background-color: rgba(0, 0, 0, 0.05);
  position: relative;
  overflow: hidden;
}

.toast-progress-bar {
  height: 100%;
  width: 100%;
  transform-origin: left;
  animation: toast-progress linear forwards;
}

@keyframes toast-progress {
  from {
    transform: scaleX(1);
  }
  to {
    transform: scaleX(0);
  }
}

.toast-success .toast-progress-bar {
  background-color: #10B981;
}

.toast-error .toast-progress-bar {
  background-color: #EF4444;
}

.toast-warning .toast-progress-bar {
  background-color: #F59E0B;
}

.toast-info .toast-progress-bar {
  background-color: #3B82F6;
}

/* 进入和离开动画 */
.toast-enter-active {
  animation: toast-in 0.3s ease-out forwards;
}

.toast-leave-active {
  animation: toast-out 0.5s ease-in forwards;
}

@keyframes toast-in {
  from {
    opacity: 0;
    transform: translateX(100%) scale(0.85);
  }
  50% {
    opacity: 1;
    transform: translateX(-5%) scale(1);
  }
  to {
    opacity: 1;
    transform: translateX(0) scale(1);
  }
}

@keyframes toast-out {
  from {
    opacity: 1;
    transform: translateX(0) scale(1);
  }
  to {
    opacity: 0;
    transform: translateX(120%) scale(0.85);
  }
}

/* 确保消息平滑移动 */
.toast-move {
  transition: transform 0.5s ease;
}

/* 响应式调整 */
@media (max-width: 640px) {
  .toast-card {
    max-width: 300px;
  }
  
  .toast-inner {
    padding: 12px;
  }
  
  .toast-icon {
    width: 24px;
    height: 24px;
    margin-right: 12px;
  }
  
  .toast-message {
    font-size: 0.85rem;
  }
}
</style> 