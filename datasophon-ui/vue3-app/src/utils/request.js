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
    
    // 安全记录认证状态，避免undefined错误
    console.log(`[Auth] authService.isAuthenticated: ${authService.isAuthenticated ? authService.isAuthenticated : false}`);
    if (authService.token) {
      console.log(`[Auth] token from authService: ${authService.token ? '存在' : '不存在'}`);
    } else {
      console.log('[Auth] authService.token未定义');
    }
    
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
    
    // 打印响应内容的摘要
    try {
      if (response.data) {
        console.log(`[Response] ${response.config.url} 响应数据:`, {
          code: response.data.code,
          success: response.data.success,
          msg: response.data.msg,
          dataType: typeof response.data.data,
          hasData: response.data.data !== null && response.data.data !== undefined
        });
      }
    } catch (e) {
      console.warn('[Response] 无法记录响应数据:', e);
    }
    
    return response.data
  },
  error => {
    // 错误发生，进行详细日志记录
    console.error(`[Response Error] 请求失败: ${error.message}`);
    
    // 获取请求信息
    const requestInfo = error.config ? {
      url: error.config.url,
      method: error.config.method,
      headers: error.config.headers
    } : '无请求配置';
    
    console.error(`[Response Error] 请求信息:`, requestInfo);
    
    // 处理401未授权错误
    if (error.response) {
      console.error(`[Response Error] 状态码: ${error.response.status}`);
      
      // 尝试记录响应数据
      try {
        console.error(`[Response Error] 响应数据:`, error.response.data);
      } catch (e) {
        console.error('[Response Error] 无法记录响应数据');
      }
      
      // 处理特定状态码
      if (error.response.status === 401) {
        console.error('[Auth Error] 用户未授权或授权已过期');
        
        // 返回自定义错误信息，不跳转页面
        return Promise.reject({
          ...error,
          isAuthError: true,
          message: '登录已过期或权限不足，请尝试重新登录'
        })
      } else if (error.response.status === 403) {
        console.error('[Auth Error] 用户无权限访问资源');
        return Promise.reject({
          ...error,
          isForbiddenError: true,
          message: '您没有权限访问该资源'
        });
      } else if (error.response.status === 500) {
        console.error('[Server Error] 服务器内部错误');
        return Promise.reject({
          ...error,
          isServerError: true,
          message: '服务器出错，请稍后再试'
        });
      }
    } else if (error.request) {
      // 请求发出但没有收到响应
      console.error('[Response Error] 无响应:', error.request);
      return Promise.reject({
        ...error,
        isNetworkError: true,
        message: '服务器无响应，请检查网络连接'
      });
    }
    
    return Promise.reject(error)
  }
)

// 简化的请求方法，使用标准Promise接口
export function axiosPost(url, params = {}, showLoading = false) {
  // 打印完整URL用于调试
  const fullUrl = url;
  console.log(`[axiosPost] 调用 ${fullUrl} 参数:`, params);
  
  // 解决表单数据转换问题 - 确保是纯对象
  let requestData = params;
  
  // 特殊处理登录请求
  if (url === '/ddh/api/login') {
    console.log('[axiosPost] 检测到登录请求，使用特殊处理');
    // 确保请求头使用表单格式
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
          console.log(`[transformRequest] 登录请求参数转换结果: ${result}`);
          return result;
        }
      ]
    });
  }
  
  // 普通请求处理
  return service({
    method: 'post',
    url,
    data: requestData,
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
export const getToken = () => {
  if (authService && authService.token) {
    return authService.token
  }
  // 如果authService不存在或token不存在，则直接从localStorage获取
  return localStorage.getItem('auth_token')
}

// 设置认证信息
export function setAuthorization(token) {
  if (authService && authService.setToken) {
    authService.setToken(token)
  } else {
    // 如果authService不可用，则直接存储到localStorage
    localStorage.setItem('auth_token', token)
  }
}

// 移除认证信息
export function removeAuthorization() {
  if (authService && authService.logout) {
    authService.logout()
  } else {
    // 如果authService不可用，则直接从localStorage移除
    localStorage.removeItem('auth_token')
  }
}

// 检查是否有认证信息
export function checkAuthorization() {
  if (authService && typeof authService.isAuthenticated !== 'undefined') {
    return authService.isAuthenticated
  }
  // 如果authService不可用，则直接检查localStorage
  return !!localStorage.getItem('auth_token')
}

export default service