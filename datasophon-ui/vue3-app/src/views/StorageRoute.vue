<template>
  <div class="storage-route-wrapper">
    <GlobalErrorBoundary title="存储库管理" message="加载存储库组件时发生错误，系统将尝试使用备用方式加载。">
      <Suspense>
        <template #default>
          <storage-view />
        </template>
        <template #fallback>
          <div class="loading-container">
            <div class="spinner"></div>
            <div class="loading-text">加载存储库管理页面...</div>
          </div>
        </template>
      </Suspense>
    </GlobalErrorBoundary>
  </div>
</template>

<script>
import { defineComponent, defineAsyncComponent, onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import GlobalErrorBoundary from '../components/GlobalErrorBoundary.vue'

// 使用异步组件加载存储库视图，有两次重试机会
const StorageView = defineAsyncComponent({
  // 主组件加载
  loader: () => import('./repository/ParcelList.vue'),
  
  // 加载中显示
  loadingComponent: {
    template: `
      <div class="flex items-center justify-center p-12">
        <div class="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-blue-500"></div>
        <div class="ml-3 text-gray-600">加载存储库组件...</div>
      </div>
    `
  },
  
  // 加载失败时重试
  onError(error, retry, fail, attempts) {
    console.error(`[StorageRoute] 加载组件失败 (尝试 ${attempts}/3):`, error)
    
    // 最多重试两次
    if (attempts <= 2) {
      console.log(`[StorageRoute] 尝试重试...`)
      setTimeout(() => {
        retry()
      }, 1000)
    } else {
      console.error('[StorageRoute] 重试失败，使用备用组件')
      // 如果重试失败，尝试加载备用组件
      import('./cluster/DirectStorageAccess.vue')
        .then(component => {
          console.log('[StorageRoute] 成功加载备用组件')
          return component
        })
        .catch(err => {
          console.error('[StorageRoute] 备用组件也加载失败:', err)
          fail()
        })
    }
  }
})

export default {
  name: 'StorageRoute',
  components: {
    StorageView,
    GlobalErrorBoundary
  },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const isLoading = ref(true)

    onMounted(() => {
      console.log('[StorageRoute] 存储库路由组件已加载')
      console.log('[StorageRoute] 当前路由信息:', route.path)
      
      // 3秒后停止加载状态，避免无限loading
      setTimeout(() => {
        isLoading.value = false
      }, 3000)
    })

    return {
      isLoading,
      route
    }
  }
}
</script>

<style scoped>
.storage-route-wrapper {
  position: relative;
  min-height: 400px;
  width: 100%;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  padding: 40px 0;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(0, 0, 0, 0.1);
  border-radius: 50%;
  border-top-color: #1890ff;
  animation: spin 1s ease-in-out infinite;
  margin-bottom: 16px;
}

.loading-text {
  font-size: 16px;
  color: #666;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style> 