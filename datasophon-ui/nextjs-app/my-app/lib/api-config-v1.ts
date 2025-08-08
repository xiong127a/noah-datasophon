import axios from "axios";

// API版本化配置
export const API_BASE_URL = "http://localhost:8081/ddh"; // 包含context-path
export const API_PREFIX = "/api"; // 移除重复的/ddh，因为baseURL已包含
export const API_VERSION = "v1"; // 统一版本管理
export const API_BASE = `${API_PREFIX}/${API_VERSION}`; // /api/v1

// 版本化API路径配置
export const API_PATHS_V1 = {
  // 认证相关
  LOGIN: `${API_BASE}/auth/login`, // 登录接口保持原有路径（特殊处理）
  LOGOUT: `${API_BASE}/auth/logout`,
  REFRESH_TOKEN: `${API_BASE}/auth/refreshToken`,
  USER_INFO: `${API_BASE}/auth/user-info`,

  // 集群相关 - v1
  CLUSTER_LIST: `${API_BASE}/cluster/list`,
  CLUSTER_DETAIL: `${API_BASE}/cluster/detail`,
  CLUSTER_SAVE: `${API_BASE}/cluster/save`,
  CLUSTER_UPDATE: `${API_BASE}/cluster/update`,
  CLUSTER_DELETE: `${API_BASE}/cluster/delete`,
  CLUSTER_AUTH: `${API_BASE}/cluster/user/saveClusterManager`,
  CLUSTER_SERVICE_LIST: `${API_BASE}/cluster/service/list`,
  CLUSTER_RUNNING_LIST: `${API_BASE}/cluster/runningClusterList`,
  
  // Kubernetes相关 - v1
  CLUSTER_NAMESPACES: `${API_BASE}/cluster/namespaces`,
  CLUSTER_KUBE_CONFIG: `${API_BASE}/cluster/kube-config`,
  
  // 主机管理相关 - v1
  CLUSTER_HOST_LIST: `${API_BASE}/cluster/host/list`,
  CLUSTER_HOST_ALL: `${API_BASE}/cluster/host/all`,
  CLUSTER_HOST_GET_RACK: `${API_BASE}/cluster/host/getRack`,
  CLUSTER_HOST_ASSIGN_RACK: `${API_BASE}/cluster/host/assignRack`,

  // 用户相关 - v1
  USER_LIST: `${API_BASE}/user/list`,
  USER_ALL: `${API_BASE}/user/all`,
  USER_SAVE: `${API_BASE}/user/save`,
  USER_DELETE: `${API_BASE}/user/delete`,
  USER_UPDATE: `${API_BASE}/user/update`,
  USER_CHECK_NAME: `${API_BASE}/user/checkName`,

  // 框架相关 - v1
  FRAME_LIST: `${API_BASE}/frame/list`,
  FRAME_SERVICE_DELETE: `${API_BASE}/frame/service/delete`,
  
  // 存储库相关 - v1
  PARCEL_LIST: `${API_BASE}/cluster/parcel/list`,
  PARCEL_PARSE: `${API_BASE}/cluster/parcel/parse`,
  PARCEL_PROCESS: `${API_BASE}/cluster/parcel/process`,
  PARCEL_DOWNLOAD: `${API_BASE}/cluster/parcel/download`,
  PARCEL_INSTALL: `${API_BASE}/cluster/parcel/install`,

  // 标签管理相关 - v1
  TAG_LIST: `${API_BASE}/cluster/node/label/list`,
  TAG_SAVE: `${API_BASE}/cluster/node/label/save`,
  TAG_DELETE: `${API_BASE}/cluster/node/label/delete`,
  TAG_ASSIGN: `${API_BASE}/cluster/node/label/assign`,

  // 机架管理相关 - v1
  RACK_LIST: `${API_BASE}/cluster/rack/list`,
  RACK_SAVE: `${API_BASE}/cluster/rack/save`,
  RACK_DELETE: `${API_BASE}/cluster/rack/delete`,
  RACK_ASSIGN: `${API_BASE}/cluster/host/assignRack`,

  // 日志审计相关 - v1
  LOG_LIST: `${API_BASE}/log/list`,
  LOG_SERVICE_NAME_LIST: `${API_BASE}/log/serviceNameList`,
  LOG_MODULE_LIST: `${API_BASE}/log/moduleList`,

  // 告警相关 - v1
  ALERT_GROUP_LIST: `${API_BASE}/alert/group/list`,

  // 主机环境校验相关 - v1 (Step2)
  GET_INSTALL_STEP: `${API_BASE}/host/install/getInstallStep`,
  ANALYSIS_HOST_LIST: `${API_BASE}/host/install/analysisHostList`,
  GET_HOST_CHECK_STATUS: `${API_BASE}/host/install/getHostCheckStatus`,
  HOST_CHECK_COMPLETED: `${API_BASE}/host/install/hostCheckCompleted`,
  CLEANUP_HOST_CHECK_RESOURCES: `${API_BASE}/host/install/cleanupHostCheckResources`,
  CLEAR_HOST_ENV_CHECK_CACHE: `${API_BASE}/host/install/clearHostEnvCheckCache`,
  DISPATCHER_HOST_AGENT_LIST: `${API_BASE}/host/install/dispatcherHostAgentList`,
  DISPATCHER_HOST_AGENT_COMPLETED: `${API_BASE}/host/install/dispatcherHostAgentCompleted`,
  CANCEL_DISPATCHER_HOST_AGENT: `${API_BASE}/host/install/cancelDispatcherHostAgent`,
  RESTART_DISPATCHER_HOST_AGENT: `${API_BASE}/host/install/reStartDispatcherHostAgent`,
  GENERATE_HOST_AGENT_COMMAND: `${API_BASE}/host/install/generateHostAgentCommand`,
  GENERATE_HOST_SERVICE_COMMAND: `${API_BASE}/host/install/generateHostServiceCommand`,
  START_HOST_CHECK: `${API_BASE}/host/install/startHostCheck`,
  GET_WORKER_LOG: `${API_BASE}/host/install/getWorkerLog`,
  

  
  // 主机检查相关 - v1
  REHOST_CHECK: `${API_BASE}/host/check/rehostCheck`,
  
  // 统一主机管理相关 - v1 (新架构)
  HOST_DISCOVER: `${API_BASE}/host/discover`,
  HOST_DISCOVER_FROM_STEP1: `${API_BASE}/host/discover-from-step1`,
  HOST_VALIDATE_FOR_NEXT_STEP: `${API_BASE}/host/validate-hosts-for-next-step`,
  

  HOST_LIST: `${API_BASE}/host/list`,
  HOST_IMPORT: `${API_BASE}/host/import`,
  HOST_REFRESH: `${API_BASE}/host/refresh`,
  HOST_CHECK_CONNECTION: `${API_BASE}/host/check-connection`,
  HOST_PERFORM_CHECK: `${API_BASE}/host/check`,
  HOST_CHECK_STATUS: `${API_BASE}/host/check-status`,
  HOST_CLEANUP: `${API_BASE}/host/cleanup`,
  HOST_STRATEGIES: `${API_BASE}/host/strategies`,
  
  // 服务安装相关 - v1
  GET_SERVICE_CONFIG_OPTION: `${API_BASE}/service/install/getServiceConfigOption`,
  SAVE_SERVICE_CONFIG: `${API_BASE}/service/install/saveServiceConfig`,
  SAVE_SERVICE_ROLE_HOST_MAPPING: `${API_BASE}/service/install/saveServiceRoleHostMapping`,
  LIST_SERVICE_TAB: `${API_BASE}/service/install/listServiceTab`,
};

// 创建版本化的axios实例
export const apiClientV1 = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000,
});

// 请求拦截器 - 版本化
apiClientV1.interceptors.request.use(config => {
  if (typeof window !== 'undefined') {
    // 添加认证token
    const token = localStorage.getItem('jwt_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    // 添加集群ID到请求头
    const clusterId = localStorage.getItem('clusterId');
    if (clusterId && clusterId !== '-1') {
      config.headers['x-cluster-id'] = clusterId;
    }
    
    // 添加API版本头（可选）
    config.headers['x-api-version'] = API_VERSION;
  }
  return config;
}, error => {
  return Promise.reject(error);
});

// 响应拦截器 - 版本化
apiClientV1.interceptors.response.use(
  response => response,
  error => {
    if (error.response && error.response.status === 401) {
      if (typeof window !== 'undefined') {
        // 可以尝试刷新token，或直接重定向到登录页
        // window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

// 导出版本化的API请求函数
export const apiV1 = {
  get: (url: string, params?: any, config?: any) => 
    apiClientV1.get(url, { params, ...config }),
  post: (url: string, data: any, config?: any) => {
    console.log('API V1 POST请求:', url, '数据:', data)
    return apiClientV1.post(url, data, {
      headers: {
        'Content-Type': 'application/json'
      },
      ...config
    })
  },
  put: (url: string, data: any, config?: any) => 
    apiClientV1.put(url, data, config),
  delete: (url: string, params?: any, config?: any) => 
    apiClientV1.delete(url, { params, ...config }),
};

// 版本管理工具
export const ApiVersionManager = {
  // 获取当前API版本
  getCurrentVersion: () => API_VERSION,
  
  // 检查API版本兼容性
  isVersionSupported: (version: string) => {
    const supportedVersions = ['v1']; // 将来可以扩展
    return supportedVersions.includes(version);
  },
  
  // 获取API基础路径
  getApiBase: (version: string = API_VERSION) => {
    return `${API_PREFIX}/${version}`;
  },
  
  // 构建版本化API路径
  buildApiPath: (path: string, version: string = API_VERSION) => {
    return `${API_PREFIX}/${version}/${path}`;
  }
};

export default apiV1;