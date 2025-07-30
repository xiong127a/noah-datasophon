import axios from "axios";

// API基础URL和路径配置
export const API_BASE_URL = "http://192.168.200.3:8081";
export const API_PREFIX = "/ddh";
export const API_FULL_PREFIX = API_PREFIX + "/api"; // 完整API前缀 /ddh/api

// API路径配置
export const API_PATHS = {
  // 认证相关
  LOGIN: `${API_PREFIX}/api/login`,
  LOGOUT: `${API_PREFIX}/api/logout`,
  REFRESH_TOKEN: `${API_PREFIX}/api/refreshToken`,

  // 集群相关
  CLUSTER_LIST: `${API_PREFIX}/api/cluster/list`,
  CLUSTER_DETAIL: `${API_PREFIX}/api/cluster/detail`,
  CLUSTER_SAVE: `${API_PREFIX}/api/cluster/save`,
  CLUSTER_UPDATE: `${API_PREFIX}/api/cluster/update`,
  CLUSTER_DELETE: `${API_PREFIX}/api/cluster/delete`,
  CLUSTER_AUTH: `${API_PREFIX}/api/cluster/user/saveClusterManager`,
  CLUSTER_SERVICE_LIST: `${API_PREFIX}/api/cluster/service/list`,
  CLUSTER_RUNNING_LIST: `${API_PREFIX}/api/cluster/runningClusterList`,

  // 用户相关
  USER_LIST: `${API_PREFIX}/api/user/list`,
  USER_ALL: `${API_PREFIX}/api/user/all`, // 获取所有用户（集群授权使用）
  USER_SAVE: `${API_PREFIX}/api/user/save`,
  USER_DELETE: `${API_PREFIX}/api/user/delete`,
  USER_UPDATE: `${API_PREFIX}/api/user/update`,

  // 框架相关
  FRAME_LIST: `${API_PREFIX}/api/frame/list`,
  FRAME_SERVICE_DELETE: `${API_PREFIX}/api/frame/service/delete`,  // 删除框架服务
  
  // 存储库相关（集群存储库管理）
  PARCEL_LIST: `${API_PREFIX}/api/cluster/parcel/list`,           // 获取存储库列表
  PARCEL_PARSE: `${API_PREFIX}/api/cluster/parcel/parse`,         // 解析存储库URL获取组件列表
  PARCEL_PROCESS: `${API_PREFIX}/api/cluster/parcel/process`,     // 获取组件安装进度
  PARCEL_DOWNLOAD: `${API_PREFIX}/api/cluster/parcel/download`,   // 下载组件
  PARCEL_INSTALL: `${API_PREFIX}/api/cluster/parcel/install`,     // 安装组件
};

// 创建axios实例
export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000, // 30秒超时
});

// 请求拦截器 - 添加认证token
apiClient.interceptors.request.use(config => {
  if (typeof window !== 'undefined') {
    const token = localStorage.getItem('jwt_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
}, error => {
  return Promise.reject(error);
});

// 响应拦截器 - 处理常见错误
apiClient.interceptors.response.use(
  response => response,
  error => {
    // 处理401错误，可能需要刷新token或重定向到登录页
    if (error.response && error.response.status === 401) {
      if (typeof window !== 'undefined') {
        // 可以尝试刷新token，或直接重定向到登录页
        // window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

// 导出一个默认的API请求函数封装
export const api = {
  get: (url: string, params?: any) => apiClient.get(url, { params }),
  post: (url: string, data: any) => apiClient.post(url, data),
  put: (url: string, data: any) => apiClient.put(url, data),
  delete: (url: string, params?: any) => apiClient.delete(url, { params }),
};

export default api; 