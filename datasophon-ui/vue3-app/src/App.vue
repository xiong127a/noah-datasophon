<script setup lang="ts">
// App.vue - 根组件
import { useUserStore } from './stores/user'
import { onMounted, watch, ref, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
// 引入新的toast系统
import { toast, Toaster } from 'vue-sonner'
import AuthProvider from './components/AuthProvider.vue'
// 正确导入config
import config from './config'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const isInitializing = ref(true)
const isRefreshing = ref(false)

// 应用初始化

// 初始化应用
const initializeApp = async () => {
  try {
    console.log('[App] 应用启动，检查认证状态')
    isInitializing.value = true
    
    // 检查本地存储中是否有token
    const hasToken = localStorage.getItem('auth_token')
    console.log(`[App] localStorage token检查: ${hasToken ? '存在' : '不存在'}`)
    
    // 检查本地存储中是否有用户信息
    const hasUserInfo = localStorage.getItem('auth_user') || localStorage.getItem(config.userKey)
    console.log(`[App] localStorage 用户信息检查: ${hasUserInfo ? '存在' : '不存在'}`)
    
    if (hasToken) {
      console.log('[App] 发现本地存储的token，验证有效性')
      
      // 确保token已加载到authService中
      if (!userStore.token) {
        console.log('[App] 从localStorage同步token到userStore')
        userStore.setToken(hasToken)
      }
      
      // 如果本地已有用户信息，则不需要调用API
      if (hasUserInfo) {
        try {
          const userInfoObj = JSON.parse(hasUserInfo)
          if (userInfoObj && userInfoObj.username) {
            console.log('[App] 从本地存储加载用户信息:', userInfoObj.username)
            userStore.setUser(userInfoObj)
            
            // 如果当前在登录页，跳转到首页
            if (router.currentRoute.value.path === '/login') {
              router.push('/')
            }
            
            // 早期返回，不再调用API
            isInitializing.value = false
            return
          }
        } catch (e) {
          console.error('[App] 解析本地存储的用户信息失败', e)
        }
      }
      
      // 只有在本地没有用户信息的情况下，才调用API获取用户信息
      console.log('[App] 本地无有效用户信息，调用API获取')
      const userData = await userStore.getUserInfo()
      
      if (userData) {
        console.log('[App] 获取用户信息成功:', userData.username)
        // 如果当前在登录页，跳转到首页
        if (router.currentRoute.value.path === '/login') {
          router.push('/')
        }
      } else {
        console.log('[App] 获取用户信息失败，清除认证状态')
        // 使用静默登出，避免调用后端API
        userStore.logout({ silent: true })
        // 如果不在登录页，跳转到登录页
        if (router.currentRoute.value.path !== '/login') {
          toast({
            title: '登录已过期',
            style: { background: 'var(--warning-color)', color: 'white' }
          })
          router.push('/login')
        }
      }
    } else {
      console.log('[App] 未找到本地存储的token')
      // 确保用户处于未登录状态，使用静默清除方式
      userStore.clearUser()
      // 如果不在登录页，跳转到登录页
      if (router.currentRoute.value.path !== '/login') {
        router.push('/login')
      }
    }
  } catch (error) {
    console.error('[App] 初始化过程发生错误:', error)
    // 出现错误时，使用静默登出清除状态并跳转到登录页
    userStore.logout({ silent: true })
    if (router.currentRoute.value.path !== '/login') {
      toast({
        title: '验证失败',
        style: { background: 'var(--error-color)', color: 'white' }
      })
      router.push('/login')
    }
  } finally {
    console.log('[App] 初始化完成，设置isInitializing = false')
    isInitializing.value = false
  }
}

onMounted(async () => {
  // 应用启动时进行初始化
  await initializeApp()
  
  // 监听路由变化，在用户明确要访问登录页时清除认证
  watch(() => router.currentRoute.value.path, (newPath) => {
    // 用户主动访问登录页，则清除认证状态
    if (newPath === '/login' && userStore.isLoggedIn) {
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
      
      <!-- 使用Sonner的Toaster组件 -->
      <Toaster 
        position="top-center" 
        richColors 
        closeButton 
        expand
        theme="system"
        :duration="2000"
        :style="{ zIndex: 9999 }"
        class="global-toast-container"
      />
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

/* 确保全局toast容器在最上层并正确定位 */
.global-toast-container {
  z-index: 9999 !important;
}

/* 强制设置vue-sonner通知的位置和样式 - 符合页面深色科技风格 */
[data-sonner-toaster] {
  position: fixed !important;
  top: 20px !important;
  left: 50% !important;
  transform: translateX(-50%) !important;
  bottom: auto !important;
  z-index: 9999 !important;
  width: auto !important;
  max-width: 400px !important;
}

[data-sonner-toast] {
  position: relative !important;
  margin-bottom: 16px !important;
  min-width: 340px !important;
  padding: 20px 28px 20px 60px !important;
  border-radius: 20px !important;
  font-size: 14px !important;
  font-weight: 500 !important;
  backdrop-filter: blur(25px) !important;
  border: 1px solid rgba(255, 255, 255, 0.12) !important;
  box-shadow: 
    0 25px 50px -12px rgba(0, 0, 0, 0.5),
    0 12px 20px -8px rgba(0, 0, 0, 0.4),
    0 0 0 1px rgba(255, 255, 255, 0.08) inset,
    0 0 40px rgba(255, 255, 255, 0.03) !important;
  overflow: hidden !important;
  transform-style: preserve-3d !important;
}

/* 通知背景渐变 - 深色科技风格 */
[data-sonner-toast]::before {
  content: '' !important;
  position: absolute !important;
  top: 0 !important;
  left: 0 !important;
  right: 0 !important;
  bottom: 0 !important;
  background: linear-gradient(135deg, 
    rgba(15, 23, 42, 0.95), 
    rgba(30, 41, 59, 0.9),
    rgba(51, 65, 85, 0.85)) !important;
  z-index: -1 !important;
}

/* 成功通知样式 */
[data-sonner-toast][data-type="success"] {
  color: #ecfdf5 !important;
  border-color: rgba(34, 197, 94, 0.3) !important;
}

[data-sonner-toast][data-type="success"]::before {
  background: linear-gradient(135deg, 
    rgba(6, 78, 59, 0.95), 
    rgba(5, 46, 22, 0.9),
    rgba(20, 83, 45, 0.85)) !important;
}

/* 成功图标 - 使用伪元素避免冲突 */
[data-sonner-toast][data-type="success"] {
  position: relative !important;
}

[data-sonner-toast][data-type="success"]::after {
  content: '✓' !important;
  position: absolute !important;
  top: 50% !important;
  left: 20px !important;
  transform: translateY(-50%) !important;
  width: 24px !important;
  height: 24px !important;
  background: linear-gradient(135deg, #10b981, #059669) !important;
  border-radius: 50% !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  color: white !important;
  font-size: 12px !important;
  font-weight: bold !important;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4) !important;
  animation: successPulse 2s infinite !important;
  z-index: 10 !important;
}

/* 隐藏vue-sonner默认图标 */
[data-sonner-toast] [data-icon] {
  display: none !important;
}

/* 错误通知样式 */
[data-sonner-toast][data-type="error"] {
  color: #fef2f2 !important;
  border-color: rgba(239, 68, 68, 0.3) !important;
}

[data-sonner-toast][data-type="error"]::before {
  background: linear-gradient(135deg, 
    rgba(127, 29, 29, 0.95), 
    rgba(69, 10, 10, 0.9),
    rgba(153, 27, 27, 0.85)) !important;
}

/* 错误图标 */
[data-sonner-toast][data-type="error"]::after {
  content: '✕' !important;
  position: absolute !important;
  top: 50% !important;
  left: 20px !important;
  transform: translateY(-50%) !important;
  width: 24px !important;
  height: 24px !important;
  background: linear-gradient(135deg, #ef4444, #dc2626) !important;
  border-radius: 50% !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  color: white !important;
  font-size: 10px !important;
  font-weight: bold !important;
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.4) !important;
  animation: errorShake 2s infinite !important;
  z-index: 5 !important;
}

/* 警告通知样式 */
[data-sonner-toast][data-type="warning"] {
  color: #fffbeb !important;
  border-color: rgba(245, 158, 11, 0.3) !important;
}

[data-sonner-toast][data-type="warning"]::before {
  background: linear-gradient(135deg, 
    rgba(120, 53, 15, 0.95), 
    rgba(69, 26, 3, 0.9),
    rgba(146, 64, 14, 0.85)) !important;
}

/* 警告图标 */
[data-sonner-toast][data-type="warning"]::after {
  content: '⚠' !important;
  position: absolute !important;
  top: 50% !important;
  left: 20px !important;
  transform: translateY(-50%) !important;
  width: 24px !important;
  height: 24px !important;
  background: linear-gradient(135deg, #f59e0b, #d97706) !important;
  border-radius: 50% !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  color: white !important;
  font-size: 12px !important;
  font-weight: bold !important;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.4) !important;
  animation: warningBounce 2s infinite !important;
  z-index: 5 !important;
}

/* 信息通知样式 */
[data-sonner-toast][data-type="info"] {
  color: #eff6ff !important;
  border-color: rgba(59, 130, 246, 0.3) !important;
}

[data-sonner-toast][data-type="info"]::before {
  background: linear-gradient(135deg, 
    rgba(30, 58, 138, 0.95), 
    rgba(15, 23, 42, 0.9),
    rgba(37, 99, 235, 0.85)) !important;
}

/* 信息图标 */
[data-sonner-toast][data-type="info"]::after {
  content: 'ℹ' !important;
  position: absolute !important;
  top: 50% !important;
  left: 20px !important;
  transform: translateY(-50%) !important;
  width: 24px !important;
  height: 24px !important;
  background: linear-gradient(135deg, #3b82f6, #2563eb) !important;
  border-radius: 50% !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  color: white !important;
  font-size: 12px !important;
  font-weight: bold !important;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4) !important;
  animation: infoPulse 2s infinite !important;
  z-index: 5 !important;
}

/* 通知动画效果 */
[data-sonner-toast] {
  animation: elegantSlideIn 0.6s cubic-bezier(0.16, 1, 0.3, 1) !important;
}

@keyframes elegantSlideIn {
  0% {
    opacity: 0;
    transform: translateY(-30px) scale(0.9);
    filter: blur(4px);
  }
  50% {
    opacity: 0.8;
    transform: translateY(-5px) scale(0.98);
    filter: blur(1px);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
    filter: blur(0);
  }
}

/* 动画效果 */
@keyframes successPulse {
  0%, 100% {
    transform: translateY(-50%) scale(1);
    box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);
  }
  50% {
    transform: translateY(-50%) scale(1.05);
    box-shadow: 0 6px 20px rgba(16, 185, 129, 0.6);
  }
}

@keyframes errorShake {
  0%, 100% {
    transform: translateY(-50%) translateX(0);
  }
  25% {
    transform: translateY(-50%) translateX(-2px);
  }
  75% {
    transform: translateY(-50%) translateX(2px);
  }
}

@keyframes warningBounce {
  0%, 100% {
    transform: translateY(-50%) scale(1);
  }
  50% {
    transform: translateY(-50%) scale(1.1);
  }
}

@keyframes infoPulse {
  0%, 100% {
    transform: translateY(-50%) scale(1);
    opacity: 1;
  }
  50% {
    transform: translateY(-50%) scale(1.05);
    opacity: 0.8;
  }
}

/* 进度条倒计时动画 */
@keyframes progressCountdown {
  0% {
    width: 100%;
    opacity: 1;
  }
  90% {
    width: 10%;
    opacity: 0.8;
  }
  100% {
    width: 0%;
    opacity: 0;
  }
}

@keyframes shimmer {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}

/* 隐藏默认关闭按钮 */
[data-sonner-toast] button[data-close-button] {
  display: none !important;
}

/* 进度条容器 - 使用border实现 */
[data-sonner-toast] {
  border-bottom: 3px solid rgba(255, 255, 255, 0.2) !important;
  position: relative !important;
}

[data-sonner-toast]::before {
  content: '' !important;
  position: absolute !important;
  bottom: -3px !important;
  left: 0 !important;
  height: 3px !important;
  background: rgba(255, 255, 255, 0.4) !important;
  border-radius: 0 0 20px 20px !important;
  z-index: 1 !important;
  animation: progressCountdown 2s linear forwards !important;
}

/* 不同类型通知的进度条颜色 */
[data-sonner-toast][data-type="success"]::before {
  background: linear-gradient(90deg, 
    rgba(16, 185, 129, 0.9), 
    rgba(34, 197, 94, 0.7)) !important;
}

[data-sonner-toast][data-type="error"]::before {
  background: linear-gradient(90deg, 
    rgba(239, 68, 68, 0.9), 
    rgba(220, 38, 38, 0.7)) !important;
}

[data-sonner-toast][data-type="warning"]::before {
  background: linear-gradient(90deg, 
    rgba(245, 158, 11, 0.9), 
    rgba(217, 119, 6, 0.7)) !important;
}

[data-sonner-toast][data-type="info"]::before {
  background: linear-gradient(90deg, 
    rgba(59, 130, 246, 0.9), 
    rgba(37, 99, 235, 0.7)) !important;
}

/* 通知文本样式优化 */
[data-sonner-toast] [data-title] {
  font-weight: 600 !important;
  letter-spacing: 0.025em !important;
  margin-bottom: 4px !important;
}

[data-sonner-toast] [data-description] {
  opacity: 0.9 !important;
  line-height: 1.5 !important;
}
</style>
