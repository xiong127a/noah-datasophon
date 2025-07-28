import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios';
import { getToken, clearAuthInfo } from '@/utils/auth';
import router from '@/routes';

// 创建一个axios实例
const http: AxiosInstance = axios.create({
  baseURL: '/ddh/api', // 设置API基础URL，统一使用ddh/api前缀
  timeout: 15000, // 请求超时时间
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
http.interceptors.request.use(
  (config: AxiosRequestConfig): AxiosRequestConfig => {
    // 添加token到请求头
    const token = getToken();
    if (token && config.headers) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error: AxiosError): Promise<AxiosError> => {
    console.error('请求错误:', error);
    return Promise.reject(error);
  }
);

// 响应拦截器
http.interceptors.response.use(
  (response: AxiosResponse): any => {
    const { data } = response;
    
    // 处理不同的响应格式
    if (data.code !== undefined) {
      // 假设后端API返回的格式为 { code: number, data: any, msg: string }
      if (data.code === 200 || data.code === 0) {
        return data.data;
      }
      
      // 处理特定的错误码
      if (data.code === 401) {
        // 未授权，清除认证信息并重定向到登录页
        clearAuthInfo();
        router.navigate({ to: '/login' });
        return Promise.reject(new Error('未授权，请重新登录'));
      }
      
      if (data.code === 403) {
        router.navigate({ to: '/403' });
        return Promise.reject(new Error('无权访问'));
      }
      
      return Promise.reject(new Error(data.msg || '未知错误'));
    }
    
    // 如果响应不符合预期格式，直接返回
    return data;
  },
  (error: AxiosError): Promise<AxiosError> => {
    if (error.response) {
      const { status } = error.response;
      
      if (status === 401) {
        // 未授权，清除认证信息并重定向到登录页
        clearAuthInfo();
        router.navigate({ to: '/login' });
      } else if (status === 403) {
        router.navigate({ to: '/403' });
      }
    }
    
    return Promise.reject(error);
  }
);

// 封装GET请求
export function get<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
  return http.get(url, { params, ...config });
}

// 封装POST请求
export function post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return http.post(url, data, config);
}

// 封装PUT请求
export function put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return http.put(url, data, config);
}

// 封装DELETE请求
export function del<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
  return http.delete(url, { params, ...config });
}

export default http; 