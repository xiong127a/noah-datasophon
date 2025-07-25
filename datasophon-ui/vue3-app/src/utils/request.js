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
    // 详细输出当前请求信息
    console.log(`[Request] ${config.method?.toUpperCase()} ${config.url}`);
    
    // 检查localStorage中的token
    const localToken = localStorage.getItem('auth_token');
    console.log(`[Auth] localStorage token: ${localToken ? '存在' : '不存在'}`);
    
    // 从authService获取认证头部
    const headers = authService.getAuthHeader();
    
    // 记录认证状态
    console.log(`[Auth] authService.isAuthenticated: ${authService.isAuthenticated.value}`);
    console.log(`[Auth] token from authService: ${authService.token.value ? '存在' : '不存在'}`);
    
    // 添加认证头部
    if (headers.Authorization) {
      config.headers['Authorization'] = headers.Authorization;
      console.log(`[Auth] 添加认证头: ${headers.Authorization}`);
    } else {
      console.log(`[Auth] 警告: 无法添加认证头，未找到有效token`);
      
      // 尝试从localStorage直接获取
      if (localToken) {
        config.headers['Authorization'] = `Bearer ${localToken}`;
        console.log(`[Auth] 从localStorage添加认证头: Bearer ${localToken.substring(0, 15)}...`);
      }
    }
    
    // 如果是POST请求，检查Content-Type和数据
    if (config.method === 'post') {
      console.log(`[Request] POST请求Content-Type: ${config.headers['Content-Type']}`);
      
      if (config.data) {
        if (typeof config.data === 'object') {
          console.log(`[Request] 请求数据类型: 对象, 键: ${Object.keys(config.data).join(', ')}`);
        } else {
          console.log(`[Request] 请求数据类型: ${typeof config.data}`);
        }
      } else {
        console.log(`[Request] 请求无数据`);
      }
    }
    
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
    console.log(`[Response] ${response.config.url} 状态码: ${response.status}`);
    return response.data
  },
  error => {
    // 处理401未授权错误
    if (error.response && error.response.status === 401) {
      console.error('登录已过期或没有权限');
      console.error(`[Auth Error] URL: ${error.config.url}, Method: ${error.config.method}`);
      console.error(`[Auth Error] Headers: ${JSON.stringify(error.config.headers)}`);
      
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
  console.log(`[axiosPost] 调用 ${url} 参数:`, params);
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
        const result = urlSearchParams.toString();
        console.log(`[transformRequest] 转换结果: ${result}`);
        return result;
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