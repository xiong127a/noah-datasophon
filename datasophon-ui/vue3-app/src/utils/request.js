import axios from 'axios'

// 简单的通知系统
const notify = {
  success(message) {
    console.log('Success:', message);
    // 这里可以添加自定义的Toast通知实现
    // 暂时使用alert作为临时替代
    alert(message);
  },
  error(message) {
    console.error('Error:', message);
    // 这里可以添加自定义的Toast通知实现
    // 暂时使用alert作为临时替代
    alert(message);
  }
};

// 简单的加载器
const loading = {
  service() {
    console.log('Loading started');
    // 在这里可以返回一个包含close方法的对象
    return {
      close() {
        console.log('Loading ended');
      }
    };
  }
};

// 创建axios实例
const service = axios.create({
  // 不设置baseURL，让请求走代理
  // baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 60000, // 请求超时时间
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// 获取token
const getToken = () => localStorage.getItem('token')

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 添加token到请求头
    const token = getToken()
    if (token) {
      // 符合RFC 6750标准：Authorization: Bearer <token>
      // 1. 确保token本身不包含空格等特殊字符
      // 2. 确保Bearer和token之间有且仅有一个空格
      const cleanToken = token.trim(); // 移除可能的前后空格
      
      // 检查token格式，适当添加Bearer前缀
      let formattedToken;
      if (cleanToken.startsWith('Bearer ')) {
        // 已经有Bearer前缀，确保格式正确
        const parts = cleanToken.split(' ');
        if (parts.length >= 2) {
          // 确保Bearer和token之间只有一个空格
          formattedToken = `Bearer ${parts.slice(1).join('')}`;
        } else {
          formattedToken = cleanToken; // 格式异常，保持不变
        }
      } else {
        // 添加Bearer前缀
        formattedToken = `Bearer ${cleanToken}`;
      }
      
      config.headers['Authorization'] = formattedToken;
      
      // 详细的调试日志
      console.log(`[Auth Debug] 原始token长度: ${token.length}字符`);
      console.log(`[Auth Debug] 格式化后的Authorization头: "${formattedToken.substring(0, 25)}..."`);
    } else {
      console.log('[Auth Debug] No token found in localStorage')
    }
    
    // 监控请求
    console.log(`[Request] ${config.method?.toUpperCase()} ${config.url}`);
    console.log(`[Request Headers]`, config.headers);
    if (config.data) {
      console.log(`[Request Data]`, config.data);
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
    // 监控响应
    console.log(`[Response] ${response.status} ${response.config.url}`);
    console.log(`[Response Headers]`, response.headers);
    console.log(`[Response Data]`, response.data);
    
    if (response.data.code === 200) {
      console.log("[Response Success] Code:", response.data.code);
    } else {
      console.log("[Response Error] Code:", response.data.code, "Message:", response.data.msg);
    }
    
    return response.data
  },
  error => {
    // 处理CORS错误和网络错误
    if (error.message === 'Network Error') {
      console.error('[CORS Error] 可能是跨域问题或网络连接问题');
      console.error('[推荐解决方案] 检查后端CORS配置是否允许前端域名和所有请求头');
    }
    
    console.log("[Response Error]", error);
    
    // 处理401未授权错误
    if (error.response && error.response.status === 401) {
      console.error('登录已过期或没有权限，请重新登录');
      removeAuthorization();
      
      // 如果不是登录页，跳转到登录页
      const currentPath = window.location.hash.slice(1);
      if (currentPath !== '/login') {
        window.location.href = '/#/login';
      }
    }
    
    return Promise.reject(error)
  }
)

/**
 * 封装post请求 - 使用表单提交方式
 * @param {string} url - 请求地址
 * @param {object} params - 请求参数
 * @param {boolean} loading - 是否显示loading
 * @returns {Promise}
 */
export function axiosPost(url, params = {}, showLoading = false) {
  let loadingInstance = null
  
  if (showLoading) {
    loadingInstance = loading.service();
  }
  
  // 登录接口处理添加日志
  if (url.includes('/login')) {
    console.log("[Request]", "POST", url);
    console.log("[Request Headers]", {
      'Accept': 'application/json, text/plain, */*',
      'Content-Type': 'application/x-www-form-urlencoded'
    });
    console.log("[Request Data]", params);
  }
  
  return new Promise((resolve, reject) => {
    service({
      method: 'post',
      url,
      data: params,
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      transformRequest: [
        function(data) {
          // 对于登录请求，使用URLSearchParams处理
          if (url.includes('/login')) {
            const urlSearchParams = new URLSearchParams();
            Object.keys(data).forEach(key => {
              urlSearchParams.append(key, data[key]);
            });
            console.log("[Login Request] Content-Type: application/x-www-form-urlencoded");
            return urlSearchParams.toString();
          }
          
          // 对于其他请求，使用传统方式
          let ret = ''
          for (let it in data) {
            ret += encodeURIComponent(it) + '=' + encodeURIComponent(data[it]) + '&'
          }
          return ret.slice(0, -1)
        }
      ]
    })
      .then(res => {
        if (showLoading && loadingInstance) {
          loadingInstance.close()
        }
        resolve(res)
      })
      .catch(err => {
        if (showLoading && loadingInstance) {
          loadingInstance.close()
        }
        if (url.includes('/login')) {
          console.log("[Response Error]", err);
          console.log("[Response Error Details]", {
            message: err.message,
            status: err.response?.status,
            statusText: err.response?.statusText,
            data: err.response?.data,
            headers: err.response?.headers
          });
          
          if (err.response?.status === 401) {
            console.log("[Auth Error] Status: 401, Data:", err.response?.data);
          }
        }
        reject(err)
      })
  })
}

/**
 * 封装post请求 - 使用JSON方式提交
 * @param {string} url - 请求地址
 * @param {object} params - 请求参数
 * @param {boolean} loading - 是否显示loading
 * @returns {Promise}
 */
export function axiosJsonPost(url, params = {}, showLoading = false) {
  let loadingInstance = null
  
  if (showLoading) {
    loadingInstance = loading.service();
  }
  
  return new Promise((resolve, reject) => {
    service({
      method: 'post',
      url,
      data: params
    })
      .then(res => {
        if (showLoading && loadingInstance) {
          loadingInstance.close()
        }
        resolve(res)
      })
      .catch(err => {
        if (showLoading && loadingInstance) {
          loadingInstance.close()
        }
        reject(err)
      })
  })
}

/**
 * 封装get请求
 * @param {string} url - 请求地址
 * @param {object} params - 请求参数
 * @param {boolean} loading - 是否显示loading
 * @returns {Promise}
 */
export function axiosGet(url, params = {}, showLoading = false) {
  let loadingInstance = null
  
  if (showLoading) {
    loadingInstance = loading.service();
  }
  
  return new Promise((resolve, reject) => {
    service({
      method: 'get',
      url,
      params
    })
      .then(res => {
        if (showLoading && loadingInstance) {
          loadingInstance.close()
        }
        resolve(res)
      })
      .catch(err => {
        if (showLoading && loadingInstance) {
          loadingInstance.close()
        }
        reject(err)
      })
  })
}

/**
 * 封装DELETE请求
 * @param {string} url - 请求地址
 * @param {object} params - 请求参数
 * @param {boolean} loading - 是否显示loading
 * @returns {Promise}
 */
export function axiosDelete(url, params = {}, showLoading = false) {
  let loadingInstance = null
  
  if (showLoading) {
    loadingInstance = loading.service();
  }
  
  return new Promise((resolve, reject) => {
    service({
      method: 'delete',
      url,
      params
    })
      .then(res => {
        if (showLoading && loadingInstance) {
          loadingInstance.close()
        }
        resolve(res)
      })
      .catch(err => {
        if (showLoading && loadingInstance) {
          loadingInstance.close()
        }
        reject(err)
      })
  })
}

/**
 * 封装PUT请求
 * @param {string} url - 请求地址
 * @param {object} params - 请求参数
 * @param {boolean} loading - 是否显示loading
 * @returns {Promise}
 */
export function axiosPut(url, params = {}, showLoading = false) {
  let loadingInstance = null
  
  if (showLoading) {
    loadingInstance = loading.service();
  }
  
  return new Promise((resolve, reject) => {
    service({
      method: 'put',
      url,
      data: params
    })
      .then(res => {
        if (showLoading && loadingInstance) {
          loadingInstance.close()
        }
        resolve(res)
      })
      .catch(err => {
        if (showLoading && loadingInstance) {
          loadingInstance.close()
        }
        reject(err)
      })
  })
}

// 设置认证信息
export function setAuthorization(token) {
  localStorage.setItem('token', token)
}

// 移除认证信息
export function removeAuthorization() {
  localStorage.removeItem('token')
}

// 检查是否有认证信息
export function checkAuthorization() {
  return !!getToken()
}

export default service