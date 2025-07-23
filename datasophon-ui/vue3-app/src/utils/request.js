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
    // 获取token
    const token = getToken()
    
    // 设置token
    if (token) {
      // 使用Bearer认证方案
      config.headers['Authorization'] = `Bearer ${token}`
    }
    
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    
    // 如果返回的状态码不是200，说明接口请求有问题
    if (res.code !== 200) {
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
      return res
    }
  },
  error => {
    // 特殊处理模拟登录接口
    if (error.config && error.config.url === '/ddh/api/login' && error.response && error.response.status === 401) {
      console.log('使用模拟登录接口')
      
      // 从请求数据中获取用户名和密码
      let requestData = {}
      try {
        // 尝试从请求数据中解析用户名和密码
        if (error.config.data) {
          if (error.config.headers['Content-Type'] === 'application/x-www-form-urlencoded') {
            // 处理表单提交格式
            const params = new URLSearchParams(error.config.data);
            requestData = {
              username: params.get('username'),
              password: params.get('password')
            }
          } else {
            // 处理JSON格式
            requestData = JSON.parse(error.config.data)
          }
        }
      } catch (e) {
        console.error('解析请求数据失败', e)
      }
      
      // 验证用户名和密码（只有admin/123456可以登录）
      if (requestData.username === 'admin' && requestData.password === '123456') {
        // 模拟登录成功响应，适配JWT格式
        const mockResponse = {
          code: 200,
          msg: '登录成功',
          data: {
            token: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsImlkIjoiMSIsInJvbGVzIjoiQURNSU4iLCJpYXQiOjE2MTQ5MjY2NDIsImV4cCI6MTYxNDkzMzg0Mn0.mock-signature',
            user: {
              id: '1',
              username: 'admin',
              userType: 1,
              roles: ['ADMIN'],
              avatar: ''
            }
          }
        }
        return mockResponse
      } else {
        // 返回登录失败响应
        return Promise.reject({
          code: 401,
          msg: '用户名或密码错误',
          data: null
        })
      }
    }
    
    // 处理HTTP状态码错误
    if (error.response) {
      const { status } = error.response
      
      // 处理401/403：未授权/禁止访问
      if (status === 401 || status === 403) {
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
        notify.error(error.response.data?.msg || '请求失败')
      }
    } else {
      // 请求被取消或网络错误等
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