import axios from 'axios';

// 定义类型
type AxiosResponse<T = any> = {
  data: T;
  status: number;
  statusText: string;
  headers: any;
  config: any;
  request?: any;
};

type AxiosRequestConfig = {
  url?: string;
  method?: string;
  baseURL?: string;
  headers?: any;
  params?: any;
  data?: any;
  timeout?: number;
  transformRequest?: any;
  transformResponse?: any;
  withCredentials?: boolean;
  auth?: any;
  responseType?: string;
  xsrfCookieName?: string;
  xsrfHeaderName?: string;
  onUploadProgress?: (progressEvent: any) => void;
  onDownloadProgress?: (progressEvent: any) => void;
  maxContentLength?: number;
  validateStatus?: (status: number) => boolean;
  maxRedirects?: number;
  socketPath?: string | null;
  httpAgent?: any;
  httpsAgent?: any;
};

// 环境变量配置
// 如果将来需要切换环境，可以从环境变量中获取
const API_PREFIX = '';  // 移除/ddh/api前缀，让所有请求使用完整路径
const API_TIMEOUT = 60000;

// 简单的通知系统（后续可替换为实际UI组件）
export const notify = {
  success(message: string): void {
    console.log('Success:', message);
    // 后续可以替换为实际的UI通知组件
    alert(message);
  },
  error(message: string): void {
    console.error('Error:', message);
    // 后续可以替换为实际的UI通知组件
    alert(message);
  }
};

// 创建axios实例
const service = axios.create({
  baseURL: API_PREFIX,
  timeout: API_TIMEOUT,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
});

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 获取token
    const token = localStorage.getItem('token');
    
    // 设置token
    if (token) {
      // 使用Bearer认证方案
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data;
    
    // 如果返回的状态码不是200，说明接口请求有问题
    if (res.code !== 200) {
      // token失效
      if (res.code === 401 || res.code === 403) {
        notify.error(res.msg || '登录已过期，请重新登录');
        // 清除token
        localStorage.removeItem('token');
        localStorage.removeItem('userInfo');
        // 跳转到登录页
        setTimeout(() => {
          window.location.href = '/login';
        }, 1500);
      } else {
        notify.error(res.msg || '系统错误');
      }
      
      return Promise.reject(res);
    } else {
      return res;
    }
  },
  (error) => {
    // 处理HTTP状态码错误
    if (error.response) {
      const { status } = error.response;
      
      // 处理401/403：未授权/禁止访问
      if (status === 401 || status === 403) {
        notify.error('登录已过期或没有权限，请重新登录');
        // 清除认证信息
        localStorage.removeItem('token');
        localStorage.removeItem('userInfo');
        // 跳转到登录页
        setTimeout(() => {
          window.location.href = '/login';
        }, 1500);
      } else if (status === 404) {
        notify.error('请求的资源不存在');
      } else if (status >= 500) {
        notify.error('服务器错误，请联系管理员');
      } else {
        notify.error(error.response.data?.msg || '请求失败');
      }
    } else {
      // 请求被取消或网络错误等
      notify.error('网络错误，请检查您的网络连接');
    }
    
    return Promise.reject(error);
  }
);

// 请求类型定义
interface RequestOptions {
  showLoading?: boolean;
  // 可以根据需要添加更多选项
}

// API类
class Api {
  // 基础请求方法
  private request<T = any>(config: AxiosRequestConfig, options: RequestOptions = {}): Promise<T> {
    const { showLoading = false } = options;
    
    if (showLoading) {
      console.log('Loading started');
      // 可以添加loading状态管理
    }
    
    return new Promise((resolve, reject) => {
      service(config)
        .then((res) => {
          if (showLoading) {
            console.log('Loading ended');
            // 可以添加loading状态管理
          }
          resolve(res as unknown as T);
        })
        .catch((err) => {
          if (showLoading) {
            console.log('Loading ended');
            // 可以添加loading状态管理
          }
          reject(err);
        });
    });
  }
  
  // GET请求
  public get<T = any>(url: string, params?: any, options?: RequestOptions): Promise<T> {
    return this.request<T>({
      method: 'get',
      url,
      params
    }, options);
  }
  
  // POST请求 - JSON格式
  public post<T = any>(url: string, data?: any, options?: RequestOptions): Promise<T> {
    return this.request<T>({
      method: 'post',
      url,
      data
    }, options);
  }
  
  // POST请求 - 表单格式
  public postForm<T = any>(url: string, data?: any, options?: RequestOptions): Promise<T> {
    return this.request<T>({
      method: 'post',
      url,
      data,
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      transformRequest: [
        function(data) {
          let ret = '';
          for (let it in data) {
            ret += encodeURIComponent(it) + '=' + encodeURIComponent(data[it]) + '&';
          }
          return ret.slice(0, -1);
        }
      ]
    }, options);
  }
  
  // DELETE请求
  public delete<T = any>(url: string, params?: any, options?: RequestOptions): Promise<T> {
    return this.request<T>({
      method: 'delete',
      url,
      params
    }, options);
  }
  
  // PUT请求
  public put<T = any>(url: string, data?: any, options?: RequestOptions): Promise<T> {
    return this.request<T>({
      method: 'put',
      url,
      data
    }, options);
  }
  
  // 授权相关方法
  public setToken(token: string): void {
    localStorage.setItem('token', token);
  }
  
  public removeToken(): void {
    localStorage.removeItem('token');
  }
  
  public getToken(): string | null {
    return localStorage.getItem('token');
  }
  
  public checkAuth(): boolean {
    return !!this.getToken();
  }
}

// 创建并导出API实例
const api = new Api();
export default api;

// 导出axios实例（用于需要直接使用axios的场景）
export { service as axios }; 