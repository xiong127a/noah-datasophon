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
    // 添加详细的请求日志
    console.log(`[Request] ${config.method?.toUpperCase()} ${config.url}`)
    console.log(`[Request Headers]`, config.headers)
    console.log(`[Request Data]`, config.data)
    
    // 登录接口不需要添加token
    if (config.url?.includes('/login')) {
      // 确保登录请求使用JSON格式
      if (!config.headers['Content-Type']) {
        config.headers['Content-Type'] = 'application/json;charset=UTF-8'
      }
      console.log(`[Login Request] Content-Type: ${config.headers['Content-Type']}`)
      return config;
    }
    
    // 获取token
    const token = getToken()
    
    // 设置token
    if (token) {
      // 使用Bearer认证方案
      config.headers['Authorization'] = `Bearer ${token}`
      console.log(`[Auth Token] Bearer token added`)
    } else {
      console.log(`[Auth Token] No token found`)
    }
    
    return config
  },
  error => {
    console.error(`[Request Error]`, error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    console.log(`[Response] ${response.status} ${response.config.url}`)
    console.log(`[Response Headers]`, response.headers)
    console.log(`[Response Data]`, response.data)
    
    const res = response.data
    
    // 如果返回的状态码不是200，说明接口请求有问题
    if (res.code !== 200) {
      console.error(`[Response Error] Code: ${res.code}, Message: ${res.msg}`)
      
      // token失效
      if (res.code === 401 || res.code === 403) {
        notify.error(res.msg || '登录已过期，请重新登录')
        // 清除token
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        // 跳转到登录页
        setTimeout(() => {
          window.location.href = '/login'
        }, 1500)
      } else {
        notify.error(res.msg || '系统错误')
      }
      
      return Promise.reject(res)
    } else {
      console.log(`[Response Success] Code: ${res.code}`)
      return res
    }
  },
  error => {
    console.error(`[Response Error]`, error)
    console.error(`[Response Error Details]`, {
      message: error.message,
      status: error.response?.status,
      statusText: error.response?.statusText,
      data: error.response?.data,
      headers: error.response?.headers,
      config: {
        url: error.config?.url,
        method: error.config?.method,
        headers: error.config?.headers,
        data: error.config?.data
      }
    })
    
    // 处理HTTP状态码错误
    if (error.response) {
      const { status, data } = error.response
      
      // 处理401/403：未授权/禁止访问
      if (status === 401 || status === 403) {
        console.error(`[Auth Error] Status: ${status}, Data:`, data)
        notify.error('登录已过期或没有权限，请重新登录')
        // 清除认证信息
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        // 跳转到登录页
        setTimeout(() => {
          window.location.href = '/login'
        }, 1500)
      } else if (status === 404) {
        notify.error('请求的资源不存在')
      } else if (status >= 500) {
        notify.error('服务器错误，请联系管理员')
      } else {
        notify.error(data?.msg || error.response.data?.msg || '请求失败')
      }
    } else {
      // 请求被取消或网络错误等
      console.error(`[Network Error]`, error.message)
      notify.error('网络错误，请检查您的网络连接')
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