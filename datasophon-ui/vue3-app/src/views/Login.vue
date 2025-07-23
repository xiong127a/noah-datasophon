<template>
  <div class="min-h-screen flex bg-gray-100">
    <!-- 左侧登录表单 -->
    <div class="w-full md:w-1/2 flex items-center justify-center p-6">
      <div class="w-full max-w-md">
        <div class="text-center mb-12">
          <img src="../assets/logo.svg" alt="Logo" class="h-12 mx-auto mb-4" />
          <h1 class="text-3xl font-bold text-gray-900">登录系统</h1>
          <p class="mt-2 text-gray-600">欢迎使用Noah大数据平台</p>
        </div>
        
        <div class="bg-white rounded-xl shadow-apple p-8">
          <form @submit.prevent="handleLogin">
            <!-- 用户名 -->
            <div class="mb-6">
              <label class="block text-sm font-medium text-gray-700 mb-2">用户名</label>
              <div class="relative">
                <div class="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none">
                  <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                  </svg>
                </div>
                <input
                  v-model="form.username"
                  type="text"
                  class="w-full pl-10 pr-4 py-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
                  placeholder="请输入用户名"
                  required
                />
              </div>
            </div>
            
            <!-- 密码 -->
            <div class="mb-6">
              <label class="block text-sm font-medium text-gray-700 mb-2">密码</label>
              <div class="relative">
                <div class="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none">
                  <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                  </svg>
                </div>
                <input
                  v-model="form.password"
                  type="password"
                  class="w-full pl-10 pr-4 py-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
                  placeholder="请输入密码"
                  required
                />
              </div>
            </div>
            
            <!-- 记住我 -->
            <div class="flex items-center justify-between mb-6">
              <div class="flex items-center">
                <input
                  v-model="form.remember"
                  id="remember"
                  type="checkbox"
                  class="h-4 w-4 text-primary border-gray-300 rounded focus:ring-primary"
                />
                <label for="remember" class="ml-2 block text-sm text-gray-700">
                  记住我
                </label>
              </div>
              <a href="#" class="text-sm text-primary hover:text-primary-dark">忘记密码?</a>
            </div>
            
            <!-- 登录按钮 -->
            <button
              type="submit"
              class="w-full bg-primary text-white py-3 rounded-lg hover:bg-primary-dark transition-all duration-200 flex items-center justify-center font-medium"
              :class="{ 'opacity-75 cursor-not-allowed': loading }"
              :disabled="loading"
            >
              <svg v-if="loading" class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              {{ loading ? '登录中...' : '登录' }}
            </button>
          </form>
        </div>
      </div>
    </div>
    
    <!-- 右侧背景图 -->
    <div class="hidden md:block md:w-1/2 bg-blue-600 relative">
      <div class="absolute inset-0 bg-gradient-to-br from-blue-500 to-indigo-700">
        <div class="absolute inset-0 bg-cover bg-center" style="background-image: url('../assets/login-bg.jpg'); opacity: 0.2;"></div>
      </div>
      <div class="absolute inset-0 flex flex-col items-center justify-center px-10 text-white">
        <h2 class="text-3xl font-bold mb-6">Noah大数据平台</h2>
        <p class="text-lg text-center mb-6 max-w-lg">
          基于Apache和Open Source构建的企业级大数据解决方案，使您能够轻松管理、监控和调整您的大数据基础设施。
        </p>
        <div class="grid grid-cols-3 gap-4 w-full max-w-lg">
          <div class="bg-white bg-opacity-10 backdrop-filter backdrop-blur-md p-4 rounded-lg">
            <div class="text-2xl font-bold">30+</div>
            <div class="text-sm text-gray-100">支持组件</div>
          </div>
          <div class="bg-white bg-opacity-10 backdrop-filter backdrop-blur-md p-4 rounded-lg">
            <div class="text-2xl font-bold">10K+</div>
            <div class="text-sm text-gray-100">用户</div>
          </div>
          <div class="bg-white bg-opacity-10 backdrop-filter backdrop-blur-md p-4 rounded-lg">
            <div class="text-2xl font-bold">99.9%</div>
            <div class="text-sm text-gray-100">高可用性</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

// 表单数据
const form = reactive({
  username: '',
  password: '',
  remember: false
})

// 加载状态
const loading = ref(false)

// 登录处理
const handleLogin = async () => {
  if (!form.username || !form.password) return
  
  loading.value = true
  
  try {
    // 这里应该调用实际的登录API
    // 模拟登录请求
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    // 假设登录成功
    const token = 'mock-token-' + Math.random().toString(36).substr(2)
    const userInfo = {
      id: '1',
      username: form.username,
      avatar: '',
      roles: ['admin']
    }
    
    // 存储到Pinia
    userStore.setToken(token)
    userStore.setUserInfo(userInfo)
    
    // 跳转到首页
    router.push('/')
  } catch (error) {
    console.error('登录失败:', error)
    alert('登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}
</script> 