<template>
  <div class="error-boundary">
    <slot v-if="!error"></slot>
    <div v-else class="error-container">
      <div class="error-card">
        <div class="error-header">
          <h2 class="error-title">{{ title || '组件加载失败' }}</h2>
        </div>
        <div class="error-body">
          <div v-if="showDetails" class="error-details">
            <pre>{{ error.stack || error.message || error }}</pre>
          </div>
          <div v-else class="error-message">
            {{ message || '加载组件时发生错误，请尝试刷新页面或联系管理员。' }}
          </div>
        </div>
        <div class="error-actions">
          <button @click="retry" class="retry-button">
            重试
          </button>
          <button @click="toggleDetails" class="details-button">
            {{ showDetails ? '隐藏详情' : '显示详情' }}
          </button>
          <button @click="navigateBack" class="back-button">
            返回上一页
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onErrorCaptured, provide } from 'vue'
import { useRouter } from 'vue-router'

export default {
  name: 'GlobalErrorBoundary',
  props: {
    title: {
      type: String,
      default: ''
    },
    message: {
      type: String,
      default: ''
    },
    showFallback: {
      type: Boolean,
      default: false
    }
  },
  setup(props, { slots }) {
    const router = useRouter()
    const error = ref(null)
    const showDetails = ref(false)
    const retryCount = ref(0)

    // 捕获子组件错误
    onErrorCaptured((err, instance, info) => {
      console.error('[ErrorBoundary] 捕获到错误:', err)
      console.error('[ErrorBoundary] 错误来源:', instance)
      console.error('[ErrorBoundary] 错误信息:', info)
      
      // 设置错误状态
      error.value = err
      
      // 阻止错误向上传播
      return false
    })

    // 切换错误详情显示状态
    const toggleDetails = () => {
      showDetails.value = !showDetails.value
    }

    // 重试加载组件
    const retry = () => {
      retryCount.value++
      error.value = null
      
      // 如果重试次数过多，提示用户可能需要刷新页面
      if (retryCount.value > 2) {
        setTimeout(() => {
          if (confirm('多次重试仍然失败，是否刷新页面？')) {
            window.location.reload()
          }
        }, 500)
      }
    }

    // 返回上一页
    const navigateBack = () => {
      try {
        router.back()
      } catch (e) {
        // 如果返回失败，尝试导航到首页
        router.push('/')
      }
    }

    // 向下提供错误状态
    provide('errorBoundaryState', {
      hasError: () => !!error.value,
      getError: () => error.value,
      setError: (err) => {
        error.value = err
      }
    })

    return {
      error,
      showDetails,
      toggleDetails,
      retry,
      navigateBack
    }
  }
}
</script>

<style scoped>
.error-boundary {
  width: 100%;
  height: 100%;
}

.error-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
  padding: 20px;
}

.error-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 600px;
  overflow: hidden;
}

.error-header {
  padding: 16px;
  background-color: #f8d7da;
  border-bottom: 1px solid #f5c6cb;
}

.error-title {
  color: #721c24;
  font-size: 18px;
  font-weight: 500;
  margin: 0;
}

.error-body {
  padding: 20px;
  min-height: 100px;
}

.error-message {
  color: #555;
}

.error-details {
  background: #f8f8f8;
  border-radius: 4px;
  padding: 12px;
  max-height: 300px;
  overflow: auto;
  font-family: monospace;
  font-size: 12px;
  white-space: pre-wrap;
  color: #666;
}

.error-actions {
  display: flex;
  justify-content: flex-end;
  padding: 16px;
  background: #f8f8f8;
  border-top: 1px solid #eee;
  gap: 8px;
}

.retry-button,
.details-button,
.back-button {
  padding: 8px 16px;
  border-radius: 4px;
  border: none;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.retry-button {
  background: #28a745;
  color: white;
}

.retry-button:hover {
  background: #218838;
}

.details-button {
  background: #6c757d;
  color: white;
}

.details-button:hover {
  background: #5a6268;
}

.back-button {
  background: #007bff;
  color: white;
}

.back-button:hover {
  background: #0069d9;
}
</style> 