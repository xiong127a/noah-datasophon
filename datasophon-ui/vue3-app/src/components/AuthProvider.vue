<template>
  <!-- Auth Provider Component - Provides authentication context for the application -->
  <div>
    <slot></slot>
  </div>
</template>

<script>
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { errorHandler } from '@/composables/useErrorHandler'

export default {
  name: 'AuthProvider',
  setup() {
    const router = useRouter()
    const userStore = useUserStore()

    // 认证状态监听间隔
    const authCheckInterval = ref(null)
    const LOGIN_CHECK_INTERVAL = 60000 // 60秒检查一次
    const SERIOUS_AUTH_ERRORS_COUNT = 3 // 连续严重认证错误数量阈值

    // 计数器
    const authErrorCount = ref(0)

    // 启动认证状态检查
    onMounted(() => {
      // 启动定期认证状态检查
      authCheckInterval.value = setInterval(() => {
        // 非登录页面且无效认证时，判断错误次数决定行为
        if (!userStore.isLoggedIn && router.currentRoute.value.path !== '/login') {
          authErrorCount.value++
          
          // 仅当连续多次检测到认证问题时，才考虑跳转登录页
          if (authErrorCount.value >= SERIOUS_AUTH_ERRORS_COUNT) {
            console.log('[Auth] 检测到多次认证状态失效，重定向到登录页')
            // 使用静默登出模式并重定向
            userStore.logout({ silent: true })
            router.push('/login')
            authErrorCount.value = 0 // 重置计数
          } else {
            console.log(`[Auth] 检测到认证状态异常 (${authErrorCount.value}/${SERIOUS_AUTH_ERRORS_COUNT})`)
          }
        } else {
          // 认证状态正常时重置计数
          if (authErrorCount.value > 0) {
            authErrorCount.value = 0
          }
        }
      }, LOGIN_CHECK_INTERVAL)
    })

    // 清理定时器
    onBeforeUnmount(() => {
      if (authCheckInterval.value) {
        clearInterval(authCheckInterval.value)
      }
    })
    
    // No need to return anything as this is just a provider component
  }
}
</script>

<style scoped>
/* This component doesn't need any styling as it's just a container */
</style> 