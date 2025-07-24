<template>
  <div class="relative w-screen h-screen flex justify-center items-center overflow-hidden font-sans apple-bg">
    <!-- 背景效果 -->
    <div class="absolute inset-0 bg-gradient-radial from-indigo-100 via-blue-100 to-sky-200 z-[-2]"></div>
    <div class="absolute inset-0 z-[-1]">
      <!-- 添加更多流体形状 -->
      <div class="absolute top-1/4 left-1/4 w-60 h-60 rounded-full bg-gradient-to-r from-blue-400 to-indigo-500 opacity-20 blur-3xl transform -translate-y-1/2 animate-float-slow"></div>
      <div class="absolute bottom-1/3 right-1/4 w-80 h-80 rounded-full bg-gradient-to-r from-cyan-300 to-sky-400 opacity-20 blur-3xl animate-float-medium"></div>
      <div class="absolute top-2/3 right-1/3 w-40 h-40 rounded-full bg-gradient-to-r from-indigo-300 to-purple-400 opacity-20 blur-3xl animate-float-fast"></div>
      <div class="absolute bottom-1/4 left-1/3 w-56 h-56 rounded-full bg-gradient-to-r from-blue-300 to-teal-400 opacity-15 blur-3xl animate-float-reverse"></div>
    </div>
    
    <!-- 登录卡片 -->
    <div class="w-[420px] max-w-[90%] backdrop-blur-xl bg-white/40 rounded-3xl shadow-2xl p-10 z-10 animate-card-appear border border-white/30 transition-all duration-500 ease-out hover:shadow-3xl hover:bg-white/45">
      <!-- 头部 -->
      <div class="text-center mb-10">
        <div class="mb-5">
          <img src="@/assets/img/logo.png" alt="Datasophon Logo" class="h-16 object-contain mx-auto animate-logo-appear" />
        </div>
        <h1 class="text-2xl font-semibold text-gray-800 mb-2 tracking-tight animate-title-appear">Datasophon</h1>
        <p class="text-base text-gray-600 animate-subtitle-appear">一站式大数据平台部署与管理系统</p>
      </div>
      
      <!-- 表单 -->
      <div class="mb-10">
        <form @submit.prevent="handleLogin">
          <!-- 用户名 -->
          <div class="mb-4 animate-form-item-appear">
            <div class="relative group">
              <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <svg class="w-5 h-5 text-gray-500 group-hover:text-blue-500 transition-colors duration-300" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path>
                </svg>
              </div>
              <input
                v-model="loginForm.username"
                type="text"
                placeholder="用户名"
                class="w-full h-12 pl-10 pr-3 py-2 rounded-xl text-gray-700 bg-white/60 backdrop-blur-sm border border-white/50
                     focus:border-blue-500 focus:bg-white/90 focus:ring-2 focus:ring-blue-500/30
                     hover:bg-white/80 transition-all duration-300 ease-out"
                :class="{ 'border-red-500 ring-2 ring-red-500/20': v$.username.$error }"
                @blur="v$.username.$touch"
                autocomplete="username"
                autofocus
              />
            </div>
            <div v-if="v$.username.$error" class="mt-1 text-xs text-red-500">
              {{ v$.username.$errors[0].$message }}
            </div>
          </div>
          
          <!-- 密码 -->
          <div class="mb-6 animate-form-item-appear-delay-1">
            <div class="relative group">
              <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <svg class="w-5 h-5 text-gray-500 group-hover:text-blue-500 transition-colors duration-300" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
                </svg>
              </div>
              <input
                v-model="loginForm.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="密码"
                class="w-full h-12 pl-10 pr-10 py-2 rounded-xl text-gray-700 bg-white/60 backdrop-blur-sm border border-white/50
                     focus:border-blue-500 focus:bg-white/90 focus:ring-2 focus:ring-blue-500/30
                     hover:bg-white/80 transition-all duration-300 ease-out"
                :class="{ 'border-red-500 ring-2 ring-red-500/20': v$.password.$error }"
                @blur="v$.password.$touch"
                autocomplete="current-password"
              />
              <button 
                type="button" 
                class="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-500 hover:text-gray-700 transition-colors duration-300"
                @click="showPassword = !showPassword"
              >
                <svg v-if="showPassword" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l18 18"></path>
                </svg>
                <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                </svg>
              </button>
            </div>
            <div v-if="v$.password.$error" class="mt-1 text-xs text-red-500">
              {{ v$.password.$errors[0].$message }}
            </div>
          </div>
          
          <!-- 选项 -->
          <div class="flex items-center justify-between mb-8 animate-form-item-appear-delay-2">
            <div class="flex items-center">
              <div class="relative">
                <input
                  id="remember-me"
                  type="checkbox"
                  v-model="loginForm.rememberMe"
                  class="w-4 h-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500/20 transition-all duration-300 ease-out"
                />
                <div v-if="loginForm.rememberMe" class="absolute inset-0 rounded bg-blue-500 scale-0 animate-checkbox-check"></div>
              </div>
              <label for="remember-me" class="ml-2 block text-sm text-gray-700">记住我</label>
            </div>
            <button type="button" class="text-sm text-blue-600 hover:text-blue-500 font-medium transition-colors duration-300">忘记密码?</button>
          </div>
          
          <!-- 登录按钮 -->
          <button
            type="submit"
            :disabled="userStore.loading"
            class="w-full h-12 flex justify-center items-center rounded-xl text-base font-medium text-white
                 bg-gradient-to-r from-blue-500 to-indigo-600 border-0
                 transition-all duration-300 ease-out transform
                 hover:from-blue-600 hover:to-indigo-700 hover:shadow-lg hover:shadow-blue-500/30 hover:scale-[1.02]
                 active:scale-[0.98] active:shadow-none
                 disabled:opacity-70 disabled:cursor-not-allowed disabled:hover:translate-y-0 disabled:hover:shadow-none disabled:hover:scale-100
                 animate-form-item-appear-delay-3"
          >
            <svg v-if="userStore.loading" class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            登录
          </button>
          
          <!-- 错误信息 -->
          <div v-if="errorMsg" class="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm animate-error-appear">
            {{ errorMsg }}
          </div>
        </form>
      </div>
      
      <!-- 页脚 -->
      <div class="text-center animate-footer-appear">
        <p class="text-xs text-gray-600">© {{ currentYear }} Datasophon. 保留所有权利。</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useVuelidate } from '@vuelidate/core'
import { required, minLength } from '@vuelidate/validators'
import { useUserStore } from '@/stores/user'

// 路由和状态管理
const router = useRouter()
const userStore = useUserStore()

// 响应式状态
const loginForm = reactive({
  username: 'admin',  // 默认值方便测试
  password: 'admin123', // 默认值方便测试
  rememberMe: false
})

const showPassword = ref(false)
const errorMsg = ref('')

// 表单验证规则
const rules = {
  username: { required },
  password: { required, minLength: minLength(6) }
}

const v$ = useVuelidate(rules, loginForm)

// 计算当前年份
const currentYear = computed(() => new Date().getFullYear())

// 登录处理
const handleLogin = async () => {
  const isFormCorrect = await v$.value.$validate()
  if (!isFormCorrect) return
  
  errorMsg.value = ''
    
  try {
    console.log('提交登录:', loginForm)
    // 使用用户存储的login方法
    const userData = await userStore.login({
      username: loginForm.username,
      password: loginForm.password
    })
    
    console.log('登录成功:', userData)
    // 登录成功后跳转到首页
    router.push('/')
  } catch (error) {
    console.error('登录失败:', error)
    errorMsg.value = error.message || '登录失败，请检查用户名和密码'
  }
}

// 生命周期钩子
onMounted(() => {
  // 不需要预加载背景图片，因为我们使用CSS实现背景
})
</script>

<style scoped>
/* 基础动画 */
@keyframes card-appear {
  from {
    opacity: 0;
    transform: translateY(20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes logo-appear {
  from {
    opacity: 0;
    transform: scale(0.8);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes title-appear {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes form-item-appear {
  from {
    opacity: 0;
    transform: translateX(-10px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-20px) rotate(5deg);
  }
}

@keyframes float-reverse {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(20px) rotate(-5deg);
  }
}

@keyframes checkbox-check {
  from {
    transform: scale(0);
  }
  to {
    transform: scale(1);
  }
}

@keyframes error-appear {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-card-appear {
  animation: card-appear 0.6s cubic-bezier(0.22, 1, 0.36, 1);
}

.animate-logo-appear {
  animation: logo-appear 0.5s cubic-bezier(0.22, 1, 0.36, 1) 0.1s both;
}

.animate-title-appear {
  animation: title-appear 0.5s cubic-bezier(0.22, 1, 0.36, 1) 0.2s both;
}

.animate-subtitle-appear {
  animation: title-appear 0.5s cubic-bezier(0.22, 1, 0.36, 1) 0.3s both;
}

.animate-form-item-appear {
  animation: form-item-appear 0.5s cubic-bezier(0.22, 1, 0.36, 1) 0.4s both;
}

.animate-form-item-appear-delay-1 {
  animation: form-item-appear 0.5s cubic-bezier(0.22, 1, 0.36, 1) 0.5s both;
}

.animate-form-item-appear-delay-2 {
  animation: form-item-appear 0.5s cubic-bezier(0.22, 1, 0.36, 1) 0.6s both;
}

.animate-form-item-appear-delay-3 {
  animation: form-item-appear 0.5s cubic-bezier(0.22, 1, 0.36, 1) 0.7s both;
}

.animate-footer-appear {
  animation: title-appear 0.5s cubic-bezier(0.22, 1, 0.36, 1) 0.8s both;
}

.animate-error-appear {
  animation: error-appear 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}

.animate-checkbox-check {
  animation: checkbox-check 0.2s cubic-bezier(0.22, 1, 0.36, 1) forwards;
}

.animate-float-slow {
  animation: float 20s ease-in-out infinite;
}

.animate-float-medium {
  animation: float 15s ease-in-out infinite 5s;
}

.animate-float-fast {
  animation: float 12s ease-in-out infinite 2s;
}

.animate-float-reverse {
  animation: float-reverse 17s ease-in-out infinite 3s;
}

.apple-bg {
  background-color: #f5f5f7;
}

/* 增强的阴影 */
.shadow-3xl {
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.1), 0 10px 20px -5px rgba(0, 0, 0, 0.04), 0 0 0 1px rgba(255, 255, 255, 0.5) inset;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .p-10 {
    padding: 1.5rem;
  }
  
  .mb-10 {
    margin-bottom: 1.5rem;
  }
  
  .mb-5 {
    margin-bottom: 1rem;
  }
  
  .h-16 {
    height: 3rem;
  }
}

/* 为Tailwind添加径向渐变支持 */
.bg-gradient-radial {
  background-image: radial-gradient(var(--tw-gradient-stops));
}
</style> 