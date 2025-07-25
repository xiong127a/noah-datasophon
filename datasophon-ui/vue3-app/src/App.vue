<script setup lang="ts">
// App.vue - 根组件
import Toast from './components/Toast.vue'
import { useUserStore } from './stores/user'
import { onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import AuthProvider from './components/AuthProvider.vue'

const router = useRouter()
const userStore = useUserStore()

// 监听认证状态
onMounted(() => {
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
    <!-- 授权提供器，提供全局认证功能 -->
    <AuthProvider>
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
</style>
