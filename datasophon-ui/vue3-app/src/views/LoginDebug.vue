<template>
  <div class="login-debug">
    <div class="debug-container">
      <h2>登录调试页面</h2>
      
      <!-- 登录表单 -->
      <div class="login-form">
        <h3>登录测试</h3>
        <form @submit.prevent="handleLogin">
          <div class="form-group">
            <label>用户名:</label>
            <input 
              v-model="loginForm.username" 
              type="text" 
              placeholder="请输入用户名"
              required
            />
          </div>
          <div class="form-group">
            <label>密码:</label>
            <input 
              v-model="loginForm.password" 
              type="password" 
              placeholder="请输入密码"
              required
            />
          </div>
          <button type="submit" :disabled="loading">{{ loading ? '登录中...' : '登录' }}</button>
        </form>
      </div>

      <!-- 调试信息 -->
      <div class="debug-info">
        <h3>调试信息</h3>
        
        <!-- 环境信息 -->
        <div class="info-section">
          <h4>环境配置</h4>
          <pre>{{ JSON.stringify(envInfo, null, 2) }}</pre>
        </div>

        <!-- 请求信息 -->
        <div class="info-section" v-if="requestInfo">
          <h4>最后请求信息</h4>
          <pre>{{ JSON.stringify(requestInfo, null, 2) }}</pre>
        </div>

        <!-- 响应信息 -->
        <div class="info-section" v-if="responseInfo">
          <h4>最后响应信息</h4>
          <pre>{{ JSON.stringify(responseInfo, null, 2) }}</pre>
        </div>

        <!-- 错误信息 -->
        <div class="info-section error" v-if="errorInfo">
          <h4>错误信息</h4>
          <pre>{{ JSON.stringify(errorInfo, null, 2) }}</pre>
        </div>

        <!-- 存储信息 -->
        <div class="info-section">
          <h4>本地存储</h4>
          <pre>{{ JSON.stringify(storageInfo, null, 2) }}</pre>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="actions">
        <button @click="clearLogs">清除日志</button>
        <button @click="clearStorage">清除存储</button>
        <button @click="testConnection">测试连接</button>
        <button @click="refreshInfo">刷新信息</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { axiosPost, axiosGet } from '@/utils/request'

const userStore = useUserStore()
const loading = ref(false)

// 登录表单
const loginForm = reactive({
  username: 'admin',
  password: 'admin123'
})

// 调试信息
const envInfo = ref({})
const requestInfo = ref(null)
const responseInfo = ref(null)
const errorInfo = ref(null)
const storageInfo = ref({})

// 初始化环境信息
const initEnvInfo = () => {
  envInfo.value = {
    NODE_ENV: import.meta.env.NODE_ENV,
    VITE_API_BASE_URL: import.meta.env.VITE_API_BASE_URL,
    VITE_API_PREFIX: import.meta.env.VITE_API_PREFIX,
    VITE_DEBUG: import.meta.env.VITE_DEBUG,
    baseURL: window.location.origin,
    userAgent: navigator.userAgent,
    timestamp: new Date().toISOString()
  }
}

// 刷新存储信息
const refreshStorageInfo = () => {
  storageInfo.value = {
    token: localStorage.getItem('token'),
    userInfo: localStorage.getItem('userInfo'),
    sessionStorage: {
      length: sessionStorage.length,
      keys: Object.keys(sessionStorage)
    },
    localStorage: {
      length: localStorage.length,
      keys: Object.keys(localStorage)
    }
  }
}

// 处理登录
const handleLogin = async () => {
  loading.value = true
  errorInfo.value = null
  requestInfo.value = null
  responseInfo.value = null

  try {
    console.log('[LoginDebug] 开始登录测试')
    
    // 记录请求信息
    requestInfo.value = {
      url: '/ddh/api/login',
      method: 'POST',
      data: { ...loginForm },
      timestamp: new Date().toISOString(),
      headers: {
        'Content-Type': 'application/json;charset=UTF-8'
      }
    }

    // 使用 axiosPost 进行登录
    const response = await axiosPost('/ddh/api/login', loginForm)
    
    console.log('[LoginDebug] 登录响应:', response)
    
    // 记录响应信息
    responseInfo.value = {
      data: response,
      timestamp: new Date().toISOString(),
      success: true
    }

    // 刷新存储信息
    refreshStorageInfo()
    
    alert('登录成功！')
    
  } catch (error) {
    console.error('[LoginDebug] 登录失败:', error)
    
    // 记录错误信息
    errorInfo.value = {
      message: error.message,
      status: error.response?.status,
      statusText: error.response?.statusText,
      data: error.response?.data,
      headers: error.response?.headers,
      timestamp: new Date().toISOString(),
      stack: error.stack
    }
    
    alert(`登录失败: ${error.message}`)
  } finally {
    loading.value = false
  }
}

// 测试连接
const testConnection = async () => {
  try {
    console.log('[LoginDebug] 测试后端连接')
    
    const response = await axiosGet('/ddh/api/user-info')
    console.log('[LoginDebug] 连接测试成功:', response)
    alert('后端连接正常')
    
  } catch (error) {
    console.error('[LoginDebug] 连接测试失败:', error)
    alert(`连接测试失败: ${error.message}`)
  }
}

// 清除日志
const clearLogs = () => {
  requestInfo.value = null
  responseInfo.value = null
  errorInfo.value = null
  console.clear()
}

// 清除存储
const clearStorage = () => {
  localStorage.clear()
  sessionStorage.clear()
  refreshStorageInfo()
  alert('存储已清除')
}

// 刷新信息
const refreshInfo = () => {
  initEnvInfo()
  refreshStorageInfo()
}

// 组件挂载时初始化
onMounted(() => {
  initEnvInfo()
  refreshStorageInfo()
})
</script>

<style scoped>
.login-debug {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
  font-family: 'Courier New', monospace;
}

.debug-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.debug-container h2 {
  grid-column: 1 / -1;
  text-align: center;
  color: #333;
  margin-bottom: 20px;
}

.login-form {
  background: #f5f5f5;
  padding: 20px;
  border-radius: 8px;
  border: 1px solid #ddd;
}

.login-form h3 {
  margin-top: 0;
  color: #333;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
  color: #555;
}

.form-group input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-group input:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.25);
}

button {
  background: #007bff;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  margin-right: 10px;
  margin-bottom: 10px;
}

button:hover {
  background: #0056b3;
}

button:disabled {
  background: #6c757d;
  cursor: not-allowed;
}

.debug-info {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  border: 1px solid #ddd;
  max-height: 600px;
  overflow-y: auto;
}

.debug-info h3 {
  margin-top: 0;
  color: #333;
}

.info-section {
  margin-bottom: 20px;
  padding: 15px;
  background: white;
  border-radius: 4px;
  border: 1px solid #e9ecef;
}

.info-section.error {
  background: #f8d7da;
  border-color: #f5c6cb;
}

.info-section h4 {
  margin: 0 0 10px 0;
  color: #495057;
  font-size: 14px;
}

.info-section pre {
  background: #f1f3f4;
  padding: 10px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.4;
  overflow-x: auto;
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.error pre {
  background: #f5c6cb;
  color: #721c24;
}

.actions {
  grid-column: 1 / -1;
  text-align: center;
  padding: 20px;
  background: #e9ecef;
  border-radius: 8px;
}

@media (max-width: 768px) {
  .debug-container {
    grid-template-columns: 1fr;
  }
}
</style>