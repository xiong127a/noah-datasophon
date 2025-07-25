<script setup lang="ts">
// App.vue - 根组件
import Toast from './components/Toast.vue'
import { useToast } from './composables/useToast'
import { useUserStore } from './stores/user'
import { onMounted, onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'

const { toasts, removeToast } = useToast()
const userStore = useUserStore()
const router = useRouter()

// 全局认证监听
const authCheckInterval = ref(null)

onMounted(async () => {
  // 如果有token但没有用户信息，尝试获取用户信息
  if (localStorage.getItem('token') && !userStore.userInfo?.id) {
    try {
      await userStore.getUserInfo()
      console.log('[Auth] 用户信息已更新')
    } catch (error) {
      console.error('[Auth] 获取用户信息失败', error)
    }
  }
  
  // 定期检查认证状态
  authCheckInterval.value = setInterval(() => {
    if (router.currentRoute.value.path !== '/login' && !localStorage.getItem('token')) {
      console.warn('[Auth] 检测到认证状态丢失，重定向到登录页')
      router.push('/login')
    }
  }, 10000) // 每10秒检查一次
})

onBeforeUnmount(() => {
  if (authCheckInterval.value) {
    clearInterval(authCheckInterval.value)
  }
})
</script>

<template>
  <div class="app-container">
    <router-view></router-view>
    <Toast />
    <!-- 添加Toast组件实现 -->
    <div class="fixed top-4 right-4 z-50 flex flex-col gap-2">
      <TransitionGroup name="toast">
        <div
          v-for="toast in toasts"
          :key="toast.id"
          :class="[
            'px-4 py-2 rounded-lg shadow-lg min-w-[300px] flex items-center',
            {
              'bg-green-100 text-green-800 border-l-4 border-green-500': toast.type === 'success',
              'bg-red-100 text-red-800 border-l-4 border-red-500': toast.type === 'error',
              'bg-blue-100 text-blue-800 border-l-4 border-blue-500': toast.type === 'info',
              'bg-yellow-100 text-yellow-800 border-l-4 border-yellow-500': toast.type === 'warning',
            }
          ]"
        >
          <div class="flex-grow">{{ toast.message }}</div>
          <button 
            class="ml-2 text-gray-500 hover:text-gray-700"
            @click="removeToast(toast.id)"
          >
            &times;
          </button>
        </div>
      </TransitionGroup>
    </div>
  </div>
</template>

<style scoped>
.logo {
  height: 6em;
  padding: 1.5em;
  will-change: filter;
  transition: filter 300ms;
}
.logo:hover {
  filter: drop-shadow(0 0 2em #646cffaa);
}
.logo.vue:hover {
  filter: drop-shadow(0 0 2em #42b883aa);
}
</style>

<style>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from {
  transform: translateY(-30px);
  opacity: 0;
}

.toast-leave-to {
  transform: translateX(100%);
  opacity: 0;
}
</style>
