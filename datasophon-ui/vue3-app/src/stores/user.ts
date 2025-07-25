import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { axiosPost } from '@/utils/request'
import API_PATHS from '@/api/httpApi/apiPaths'
import config from '@/config'

// 定义用户Store的类型
interface User {
  id?: number | string
  username?: string
  email?: string
  userType?: number
  [key: string]: any
}

interface LoginCredentials {
  username: string
  password: string
}

// 创建用户Store
export const useUserStore = defineStore('user', () => {
  // 状态
  const user = ref<User | null>(null)
  const token = ref<string | null>(null)
  const loading = ref(false)
  const loginError = ref<string | null>(null)
  
  // 登出防重复调用锁
  const isLoggingOut = ref(false)
  // 登出操作的冷却时间(ms)
  const LOGOUT_COOLDOWN = 3000

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => {
    return user.value?.userType === 1 || user.value?.userRole?.includes('ADMIN')
  })

  // 从localStorage获取token
  const getToken = () => {
    return localStorage.getItem('auth_token')
  }

  // 设置token到localStorage和store
  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('auth_token', newToken)
    console.log('[User] Token已设置')
  }

  // 设置用户信息
  const setUser = (userData: User) => {
    user.value = userData
    
    // 可选：保存用户信息到localStorage
    try {
      // 移除敏感信息
      const safeUserData = { ...userData }
      if (safeUserData.password) delete safeUserData.password
      
      localStorage.setItem('user_info', JSON.stringify(safeUserData))
    } catch (e) {
      console.error('[User] 保存用户信息失败:', e)
    }
  }

  // 清除用户信息
  const clearUser = () => {
    user.value = null
    token.value = null
    
    // 清除localStorage
    localStorage.removeItem('auth_token')
    localStorage.removeItem('user_info')
    
    console.log('[User] 用户信息和Token已清除')
  }

  // 从localStorage初始化
  const initializeFromStorage = () => {
    try {
      // 获取token
      const storedToken = getToken()
      if (storedToken) {
        token.value = storedToken
        console.log('[User] 从localStorage恢复Token')
      }
      
      // 获取用户信息
      const storedUser = localStorage.getItem('user_info')
      if (storedUser) {
        const parsedUser = JSON.parse(storedUser)
        if (parsedUser && parsedUser.username) {
          user.value = parsedUser
          console.log('[User] 从localStorage恢复用户信息')
        }
      }
    } catch (e) {
      console.error('[User] 从localStorage恢复状态失败:', e)
    }
  }

  // 登录方法
  const login = async (credentials: LoginCredentials) => {
    loading.value = true;
    loginError.value = null;
    
    try {
      console.log('[User] 开始登录请求:', credentials.username);
      // 尝试使用新的api接口调用
      let response;
      
      try {
        // 首先尝试使用API类进行登录
        const api = (window as any).$api;
        if (api && api.postForm) {
          console.log('[User] 使用API类登录');
          response = await api.postForm('/ddh/api/login', credentials);
        } else {
          // 如果API类不可用，则使用axiosPost
          console.log('[User] 使用axiosPost登录');
          response = await axiosPost(API_PATHS.login, credentials);
        }
      } catch (apiError) {
        console.error('[User] API调用失败，尝试直接使用axios:', apiError);
        // 如果以上方法都失败，则直接使用axios作为最后尝试
        const axios = (window as any).axios;
        if (axios) {
          response = await axios.post('/ddh/api/login', credentials, {
            headers: {
              'Content-Type': 'application/x-www-form-urlencoded'
            },
            transformRequest: [(data: any) => {
              let params = new URLSearchParams();
              for (let key in data) {
                params.append(key, data[key]);
              }
              return params.toString();
            }]
          });
          
          // 如果axios直接返回了带有data的响应，则提取其中的data
          if (response && response.data) {
            response = response.data;
          }
        } else {
          throw new Error('无法使用axios，请刷新页面重试');
        }
      }
      
      console.log('[User] 登录响应:', response);
      
      // 检查登录响应
      if (!response) {
        throw new Error('服务器无响应，请稍后再试');
      }
      
      // 处理成功响应
      if (response.code === 200) {
        // 提取token和用户信息
        const tokenValue = response.data?.token
        const userData = response.data?.user || {}
        
        console.log('[User] 登录成功，提取token和用户信息:', {
          hasToken: !!tokenValue,
          userData: Object.keys(userData).join(',')
        });
        
        if (!tokenValue) {
          throw new Error('登录成功但未返回token，请联系管理员');
        }
        
        // 保存状态
        setToken(tokenValue)
        setUser(userData)
        
        console.log('[User] 登录状态已保存，检查token和用户信息:', {
          token: !!token.value,
          user: !!user.value
        });
        
        return true
      } else {
        // 处理业务逻辑错误
        throw new Error(response.msg || '登录失败，请检查用户名和密码');
      }
    } catch (error: any) {
      console.error('[User] 登录错误:', error);
      
      // 设置错误信息
      if (error.response) {
        // Axios错误
        const status = error.response.status;
        if (status === 401) {
          loginError.value = '用户名或密码错误';
        } else if (status === 403) {
          loginError.value = '账户已锁定或无权限';
        } else {
          loginError.value = `服务器错误 (${status})`;
        }
      } else if (error.message) {
        // 自定义错误
        loginError.value = error.message;
      } else {
        // 未知错误
        loginError.value = '登录失败，请稍后再试';
      }
      
      return false
    } finally {
      loading.value = false
    }
  }
  
  // 登出方法 - 添加防重复调用机制
  const logout = async (options = { silent: false }) => {
    // 检查是否已在登出过程中
    if (isLoggingOut.value) {
      console.log('[User] 登出操作已在进行中，忽略重复调用')
      return
    }
    
    try {
      // 设置登出锁
      isLoggingOut.value = true
      console.log('[User] 开始登出流程')
      
      // 只有在非静默模式下且有token时才调用登出API
      if (!options.silent && token.value) {
        console.log('[User] 调用登出API')
        try {
          await axiosPost(API_PATHS.logout, {})
          console.log('[User] 登出API调用成功')
        } catch (error) {
          // 即使API调用失败也继续清除本地状态
          console.warn('[User] 登出API调用失败，但会继续清除本地状态', error)
        }
      } else if (options.silent) {
        console.log('[User] 静默登出，跳过API调用')
      }
    } finally {
      // 无论API是否成功，都清除本地状态
      clearUser()
      
      // 设置冷却期，防止短时间内重复调用
      setTimeout(() => {
        console.log('[User] 登出操作冷却期结束，解除锁定')
        isLoggingOut.value = false
      }, LOGOUT_COOLDOWN)
    }
  }
  
  // 获取用户信息
  const getUserInfo = async () => {
    if (!token.value) {
      console.log('[User] 无token，无法获取用户信息')
      return null
    }
    
    try {
      console.log('[User] 开始获取用户信息')
      const response = await axiosPost(API_PATHS.getUserInfo, {})
      
      if (response && response.code === 200) {
        // 成功获取用户信息
        console.log('[User] 获取用户信息成功:', response.data)
        
        // 保存用户信息
        setUser(response.data || {})
        
        // 返回用户信息
        return user.value
      } else {
        console.error('[User] 获取用户信息失败:', response?.msg || '未知错误')
        throw new Error(response?.msg || 'Failed to fetch user info')
      }
    } catch (error) {
      console.error('[User] 获取用户信息出错:', error)
      return null
    }
  }
  
  // 初始化状态
  initializeFromStorage()
  
  return {
    // 状态
    user,
    token,
    loading,
    loginError,
    isLoggingOut, // 导出锁状态以便其他组件可以检查
    
    // 计算属性
    isLoggedIn,
    isAdmin,
    
    // 方法
    login,
    logout,
    setUser,
    setToken,
    getToken,
    clearUser,
    getUserInfo
  }
}) 