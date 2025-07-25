import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { axiosPost, axiosGet, axiosJsonPost } from '../utils/request'

interface UserInfo {
  id?: string
  username?: string
  avatar?: string
  roles?: string[]
  userType?: number // 1 = admin, 2 = regular user, etc.
  [key: string]: any // 允许更多属性
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo>(JSON.parse(localStorage.getItem('userInfo') || '{}'))
  const loading = ref(false)
  
  // 计算属性
  const user = computed(() => userInfo.value)
  const isAdmin = computed(() => userInfo.value?.userType === 1 || userInfo.value?.roles?.includes('ADMIN'))
  const isLoggedIn = computed(() => !!token.value && !!userInfo.value?.id)
  
  function setToken(value: string) {
    token.value = value
    localStorage.setItem('token', value)
  }
  
  function setUserInfo(info: UserInfo) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }
  
  async function login(loginForm: { username: string; password: string }) {
    loading.value = true
    
    try {
      // 调用登录API - 使用表单格式，表单方式能让Spring Security正确处理请求
      const res = await axiosPost('/ddh/api/login', loginForm)
      
      if (res && res.code === 200) {
        // 打印完整的登录响应以便调试
        console.log('Login response data:', JSON.stringify(res.data, null, 2))
        
        // 后端API返回的token字段可能在data.token, data.SESSION_ID, 或直接在data中
        let tokenValue = null
        
        // 查找token (尝试所有可能的位置)
        if (res.data.token) {
          tokenValue = res.data.token
        } else if (res.data.SESSION_ID) {
          tokenValue = res.data.SESSION_ID
        } else if (res.data.authorization) {
          tokenValue = res.data.authorization
        } else if (typeof res.data === 'string' && res.data.length > 10) {
          // 有时整个data就是token
          tokenValue = res.data
        }
        
        // 确保用户数据存在
        const userData = res.data.user || res.data.USER_INFO || res.data || {}
        
        if (!tokenValue) {
          console.error('登录成功但找不到token格式，响应数据:', res.data)
          throw new Error('登录成功但未返回token')
        }
        
        // 清理token中可能存在的空格或不可见字符
        tokenValue = tokenValue.trim()
        console.log(`[Auth Debug] 原始token: "${tokenValue.substring(0, 10)}..."`);
        console.log(`[Auth Debug] token字符分析:`, 
          [...tokenValue.substring(0, 20)].map(c => ({ 
            char: c, 
            code: c.charCodeAt(0), 
            hex: c.charCodeAt(0).toString(16)
          }))
        );
        
        // 检查token是否是有效的JWT格式 (header.payload.signature)
        const isValidJwt = /^[A-Za-z0-9-_]+\.[A-Za-z0-9-_]+\.[A-Za-z0-9-_]+$/.test(tokenValue);
        console.log(`[Auth Debug] Token是否符合JWT格式: ${isValidJwt}`);
        
        // 设置token和用户信息
        console.log(`[Auth] 设置token: ${tokenValue.substring(0, 10)}...`)

        // 确保token不以Bearer开头，因为我们会在请求拦截器中添加
        if (tokenValue.startsWith('Bearer ')) {
          // 如果已经包含Bearer前缀，保持不变
          console.log('[Auth] Token已包含Bearer前缀');
        } else {
          // 不要在这里添加Bearer前缀，让请求拦截器处理
          console.log('[Auth] Token不包含Bearer前缀，将由请求拦截器添加');
        }

        setToken(tokenValue)
        setUserInfo(userData)
        
        // 显式调用setAuthorization函数以确保请求中使用token
        import('../utils/request').then(requestModule => {
          requestModule.setAuthorization(tokenValue)
          console.log('[Auth] 已通过setAuthorization设置token')
        })
        
        return res.data
      } else {
        throw new Error(res?.msg || '登录失败')
      }
    } catch (error) {
      console.error('登录过程出错:', error)
      throw error
    } finally {
      loading.value = false
    }
  }
  
  async function getUserInfo() {
    if (!token.value) return null
    
    try {
      // 获取用户信息API
      const res = await axiosGet('/ddh/api/user-info')
      
      if (res && res.code === 200) {
        setUserInfo(res.data)
        return res.data
      }
      return null
    } catch (error) {
      console.error('获取用户信息失败', error)
      return null
    }
  }
  
  async function logout() {
    try {
      if (token.value) {
        // 调用登出API
        await axiosPost('/ddh/api/logout')
      }
    } catch (error) {
      console.error('登出失败', error)
    } finally {
      // 无论API是否成功，都清除本地存储
      token.value = ''
      userInfo.value = {}
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }
  
  return {
    token,
    userInfo,
    user,
    isAdmin,
    isLoggedIn,
    loading,
    setToken,
    setUserInfo,
    login,
    getUserInfo,
    logout
  }
}) 