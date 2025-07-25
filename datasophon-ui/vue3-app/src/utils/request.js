import axios from 'axios'
import { authService } from '../composables/useAuth'

// 创建axios实例
const service = axios.create({
  // 不设置baseURL，让请求走代理
  timeout: 60000, // 请求超时时间
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 从authService获取认证头部，这是VueUse推荐的方式
    const headers = authService.getAuthHeader()
    
    // 添加认证头部
    if (headers.Authorization) {
      config.headers['Authorization'] = headers.Authorization
      console.log(`[Auth] 添加认证头: ${headers.Authorization.substring(0, 15)}...`)
    }
    
    // 监控请求（可选，生产环境可移除）
    console.log(`[Request] ${config.method?.toUpperCase()} ${config.url}`);
    
    return config
  },
  error => {
    console.log("[Request Error]", error);
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    // 处理401未授权错误
    if (error.response && error.response.status === 401) {
      console.error('登录已过期或没有权限');
      
      // 返回自定义错误信息，不跳转页面
      return Promise.reject({
        ...error,
        isAuthError: true,
        message: '登录已过期或权限不足，请尝试重新登录'
      })
    }
    
    return Promise.reject(error)
  }
)

// 简化的请求方法，使用标准Promise接口
export function axiosPost(url, params = {}, showLoading = false) {
  return service({
    method: 'post',
    url,
    data: params,
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    transformRequest: [
      function(data) {
        const urlSearchParams = new URLSearchParams();
        Object.keys(data).forEach(key => {
          urlSearchParams.append(key, data[key]);
        });
        return urlSearchParams.toString();
      }
    ]
  })
}

export function axiosJsonPost(url, params = {}) {
  return service({
    method: 'post',
    url,
    data: params
  })
}

export function axiosGet(url, params = {}) {
  return service({
    method: 'get',
    url,
    params
  })
}

export function axiosDelete(url, params = {}) {
  return service({
    method: 'delete',
    url,
    params
  })
}

export function axiosPut(url, params = {}) {
  return service({
    method: 'put',
    url,
    data: params
  })
}

// 获取token函数 - 从authService获取，保持兼容性
export const getToken = () => authService.token.value

// 设置认证信息
export function setAuthorization(token) {
  authService.token.value = token
}

// 移除认证信息
export function removeAuthorization() {
  authService.logout()
}

// 检查是否有认证信息
export function checkAuthorization() {
  return authService.isAuthenticated.value
}

export default service