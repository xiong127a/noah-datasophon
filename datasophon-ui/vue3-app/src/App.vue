<script setup lang="ts">
// App.vue - 根组件
import Toast from './components/Toast.vue'
import { useUserStore } from './stores/user'
import { onMounted, watch, ref } from 'vue'
import { useRouter } from 'vue-router'
import { toast } from './composables/useToast'
import AuthProvider from './components/AuthProvider.vue'

const router = useRouter()
const userStore = useUserStore()
const isInitializing = ref(true)

// 初始化应用
const initializeApp = async () => {
  try {
    console.log('[App] 应用启动，检查认证状态')
    isInitializing.value = true
    
    // 检查本地存储中是否有token
    const hasToken = localStorage.getItem('auth_token')
    console.log(`[App] localStorage token检查: ${hasToken ? '存在' : '不存在'}`)
    
    if (hasToken) {
      console.log('[App] 发现本地存储的token，验证有效性')
      console.log(`[App] Token值: ${hasToken.substring(0, 15)}...`)
      
      // 确保token已加载到authService中
      if (!userStore.token) {
        console.log('[App] 从localStorage同步token到userStore')
        userStore.setToken(hasToken)
      }
      
      // 验证token有效性
      const userData = await userStore.getUserInfo()
      
      if (userData) {
        console.log('[App] Token有效，用户已登录:', userData.username)
        // 如果当前在登录页，跳转到首页
        if (router.currentRoute.value.path === '/login') {
          router.push('/')
        }
      } else {
        console.log('[App] Token无效，清除认证状态')
        // 清除无效的认证状态
        userStore.logout()
        // 如果不在登录页，跳转到登录页
        if (router.currentRoute.value.path !== '/login') {
          toast.toast.warning('登录已过期')
          router.push('/login')
        }
      }
    } else {
      console.log('[App] 未找到本地存储的token')
      // 确保用户处于未登录状态
      userStore.clearUser()
      // 如果不在登录页，跳转到登录页
      if (router.currentRoute.value.path !== '/login') {
        router.push('/login')
      }
    }
  } catch (error) {
    console.error('[App] 初始化过程发生错误:', error)
    // 出现错误时，清除状态并跳转到登录页
    userStore.logout()
    if (router.currentRoute.value.path !== '/login') {
      toast.toast.error('验证失败')
      router.push('/login')
    }
  } finally {
    console.log('[App] 初始化完成，设置isInitializing = false')
    isInitializing.value = false
  }
}

// 监听认证状态
onMounted(async () => {
  // 应用启动时进行初始化
  await initializeApp()
  
  // 监听路由变化，在用户明确要访问登录页时清除认证
  watch(() => router.currentRoute.value.path, (newPath) => {
    // 用户主动访问登录页，则清除认证状态
    if (newPath === '/login' && userStore.isLoggedIn) {
      console.log('[Auth] 用户访问登录页，清除现有认证状态')
      userStore.logout()
    }
  })
})
</script>

<template>
  <div class="app-container">
    <!-- 初始化加载指示器 -->
    <div v-if="isInitializing" class="app-initializing">
      <div class="loading-spinner"></div>
      <p>正在加载...</p>
    </div>
    
    <!-- 授权提供器，提供全局认证功能 -->
    <AuthProvider v-else>
      <!-- 路由视图 -->
      <router-view></router-view>
      
      <!-- Toast通知组件 -->
      <Toast />
    </AuthProvider>
  </div>
</template>

<style>
:root {
  --primary-color: #1890ff;
  --success-color: #52c41a;
  --warning-color: #faad14;
  --error-color: #f5222d;
  --font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen,
    Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
}

html, body {
  margin: 0;
  padding: 0;
  font-family: var(--font-family);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  background-color: #f0f2f5;
  color: rgba(0, 0, 0, 0.85);
}

.app-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

/* 初始化加载样式 */
.app-initializing {
  position: fixed;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: #f0f2f5;
  z-index: 9999;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(0, 0, 0, 0.1);
  border-radius: 50%;
  border-top-color: var(--primary-color);
  animation: spin 1s ease-in-out infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
