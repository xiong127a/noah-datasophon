import axios from "axios";

// 防重复登录过期通知的标志位
let loginExpiredShown = false;

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
  // 服务实例相关API（基于ClusterServiceInstanceController）
  CLUSTER_SERVICE_INSTANCE_LIST: `${API_BASE}/cluster/service/instance/list`, // 服务列表API
  CLUSTER_SERVICE_INSTANCE_DELETE: `${API_BASE}/cluster/service/instance/delete`,
  CLUSTER_SERVICE_ROLE_TYPE_LIST: `${API_BASE}/cluster/service/instance/getServiceRoleType`,
  
  // 服务角色实例相关API（基于ClusterServiceRoleInstanceController）
  CLUSTER_SERVICE_ROLE_INSTANCE_LIST: `${API_BASE}/cluster/service/role/instance/list`, // 实例列表（分页）
  CLUSTER_SERVICE_ROLE_INSTANCE_DELETE: `${API_BASE}/cluster/service/role/instance/batch`, // 批量删除实例
  
  // 角色组相关API（基于实际后端实现）
  CLUSTER_SERVICE_ROLE_GROUP_LIST: `${API_BASE}/cluster/service/instance/role/group/list`,
  CLUSTER_RUNNING_LIST: `${API_BASE}/cluster/runningClusterList`,
  CLUSTER_INFO: `${API_BASE}/cluster/info`,
  
  // 集群总览 - v1
  CLUSTER_DASHBOARD_URL: `${API_BASE}/cluster/service/dashboard/getDashboardUrl`,
  DATASOPHON_DASHBOARD_URL: `${API_BASE}/cluster/service/dashboard/getDatasophonDashboard`,
  
  // Kubernetes相关 - v1
  CLUSTER_NAMESPACES: `${API_BASE}/cluster/namespaces`,
  CLUSTER_KUBE_CONFIG: `${API_BASE}/cluster/kube-config`,
  CLUSTER_K8S_NODES: `${API_BASE}/cluster/k8s/nodes`,
  
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
  FRAME_SERVICE_LIST: `${API_BASE}/frame/service/list`, // 添加服务时使用的接口
  FRAME_SERVICE_LIST_WITH_REQUIRED: `${API_BASE}/frame/service/listWithRequired`,
  
  // 服务角色分配相关 - v1 (Step5)
  GET_SERVICE_ROLE_LIST: `${API_BASE}/frame/service/role/getServiceRoleList`,
  GET_ALL_HOST: `${API_BASE}/cluster/host/all`,
  SAVE_SERVICE_ROLE_HOST_MAPPING: `${API_BASE}/service/install/saveServiceRoleHostMapping`,
  
  // Worker&Client角色分配相关 - v1 (Step6)
  GET_NON_MASTER_ROLE_LIST: `${API_BASE}/frame/service/role/getNonMasterRoleList`,
  
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

  // 自动伸缩相关 - v1（基于AutoScaleController）
  AUTO_SCALE_STATUS: `${API_BASE}/autoScale/getAutoScaleTasks`,
  AUTO_SCALE_CREATE: `${API_BASE}/autoScale/createAutoScaleTask`,
  AUTO_SCALE_UPDATE: `${API_BASE}/autoScale/updateAutoScaleTask`,

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

  // Agent分发相关 - v1 (Step3)
  START_AGENT_DISTRIBUTION: `${API_BASE}/host/agent/distribute`,
  GET_AGENT_DISTRIBUTION_STATUS: `${API_BASE}/host/agent/status`,
  RETRY_AGENT_DISTRIBUTION: `${API_BASE}/host/agent/retry`,
  CANCEL_AGENT_DISTRIBUTION: `${API_BASE}/host/agent/cancel`,
  

  
  // 主机检查相关 - v1
  REHOST_CHECK: `${API_BASE}/host/check/rehostCheck`,
  
  // 统一主机管理相关 - v1 (新架构)
  HOST_DISCOVER: `${API_BASE}/host/discover`,
  

  HOST_LIST: `${API_BASE}/host/list`,
  HOST_IMPORT: `${API_BASE}/host/import`,
  HOST_REFRESH: `${API_BASE}/host/refresh`,
  HOST_CHECK_CONNECTION: `${API_BASE}/host/check-connection`,
  HOST_PERFORM_CHECK: `${API_BASE}/host/check`,
  HOST_CLEANUP: `${API_BASE}/host/cleanup`,
  HOST_STRATEGIES: `${API_BASE}/host/strategies`,
  
  // 服务安装相关 - v1
  GET_SERVICE_CONFIG_OPTION: `${API_BASE}/service/install/getServiceConfigOption`,
  SAVE_SERVICE_CONFIG: `${API_BASE}/service/install/saveServiceConfig`,
  SAVE_SERVICE_ROLE_HOST_MAPPING_V2: `${API_BASE}/service/install/saveServiceRoleHostMapping`,
  LIST_SERVICE_TAB: `${API_BASE}/service/install/listServiceTab`,
  // 服务命令相关API（基于ClusterServiceCommandController）
  GENERATE_SERVICE_ROLE_COMMAND: `${API_BASE}/cluster/service/command/generate/role`,
  
  // 节点退役相关API
  DECOMMISSION_NODE: `${API_BASE}/cluster/service/role/instance/decommissionNode`,
  
  // 服务安装监控相关 - v1 (Step8)
  GET_SERVICE_COMMAND_LIST: `${API_BASE}/cluster/service/command/list`,
  GET_SERVICE_HOST_LIST: `${API_BASE}/cluster/service/command/host/list`,
  GET_SERVICE_ROLE_ORDER_LIST: `${API_BASE}/cluster/service/command/host/command/list`,
  GET_HOST_COMMAND_LOG: `${API_BASE}/cluster/service/command/host/command/getHostCommandLog`,
  START_EXECUTE_COMMAND: `${API_BASE}/cluster/service/command/execute`,

  // SSE日志流相关 - v1
  LOG_STREAM_SSE: `${API_BASE}/logs/stream`,

  // 主机管理相关API - v1  
  HOST_DISCOVER_STEP1: `${API_BASE}/host/discover-from-step1`,
  HOST_CHECK_VALIDATION: `${API_BASE}/host/check-hosts`,
  HOST_SAVE_DISCOVERED: `${API_BASE}/host/save-discovered-hosts`,

  // 服务文档相关 - v1
  SERVICE_DOC_GET: `${API_BASE}/service/doc/getServiceDoc`,
  SERVICE_DOC_HAS: `${API_BASE}/service/doc/hasServiceDoc`,
  SERVICE_DOC_SERVICE_NAME: `${API_BASE}/service/doc/serviceName`,
  SERVICE_DOC_IMAGE: `${API_BASE}/service/doc/image`,

  // 总览相关 - v1 (按照Vue2实际使用的API)
  GET_DASHBOARD_URL: `${API_BASE}/cluster/service/dashboard/getDashboardUrl`,
  GET_DATASOPHON_DASHBOARD: `${API_BASE}/cluster/service/dashboard/getDatasophonDashboard`,
};

/**
 * 🔧 修复Long类型精度丢失的JSON解析器
 * 将超过JavaScript安全整数范围的数字保持为字符串，特别处理ID字段
 */
function parseJsonWithLongSupport(jsonString: string): unknown {
  try {
    // JavaScript安全整数范围：-(2^53-1) 到 2^53-1
    const safeMaxInt = Number.MAX_SAFE_INTEGER; // 9007199254740991
    
    // 1. 处理ID字段 - 强制转为字符串（常见的ID字段名）
    const idFieldRegex = /"(id|ID|Id|serviceId|clusterId|hostId|roleId|instanceId|serviceInstanceId|serviceRoleInstanceId|roleGroupId)":\s*(\d+)/g;
    let processedJson = jsonString.replace(idFieldRegex, (match, fieldName, number) => {
      return `"${fieldName}":"${number}"`;
    });
    
    // 2. 处理超长数字（15位以上的整数）
    const longIntRegex = /:\s*(-?\d{15,})(?=\s*[,\]\}])/g;
    processedJson = processedJson.replace(longIntRegex, (match, number) => {
      const num = Math.abs(parseInt(number));
      if (num > safeMaxInt) {
        return `:"${number}"`;
      }
      return match;
    });
    
    // 3. 处理数组中的长整数
    const arrayLongIntRegex = /\[\s*(-?\d{15,})/g;
    processedJson = processedJson.replace(arrayLongIntRegex, (match, number) => {
      const num = Math.abs(parseInt(number));
      if (num > safeMaxInt) {
        return `["${number}"`;
      }
      return match;
    });
    
    return JSON.parse(processedJson);
  } catch (error) {
    console.warn('JSON解析失败，使用原生解析:', error);
    return JSON.parse(jsonString); // 回退到原生解析
  }
}

// 创建版本化的axios实例
export const apiClientV1 = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000,
  // 🔧 添加自定义响应转换器，处理Long类型精度丢失
  transformResponse: [
    function (data: unknown) {
      if (typeof data === 'string') {
        return parseJsonWithLongSupport(data);
      }
      return data;
    }
  ]
});

// 请求拦截器 - 版本化
apiClientV1.interceptors.request.use(config => {
  if (typeof window !== 'undefined') {
    // 添加认证token
    const token = localStorage.getItem('jwt_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    // 添加集群ID到请求头（优先使用已设置的，避免覆盖组件传递的clusterId）
    if (!config.headers['X-Cluster-Id']) {
      const clusterId = localStorage.getItem('clusterId');
      if (clusterId && clusterId !== '-1') {
        config.headers['X-Cluster-Id'] = clusterId;
      }
    }
    
    // 添加API版本头（可选）
    config.headers['X-Api-Version'] = API_VERSION;
  }
  return config;
}, error => {
  return Promise.reject(error);
});

// 响应拦截器 - 版本化（增强错误处理）
apiClientV1.interceptors.response.use(
  response => {
    // 检查业务逻辑错误（后端返回code != 200的情况）
    if (response.data && typeof response.data === 'object') {
      const { code, success, msg } = response.data;
      
      // 如果后端返回success: false或code不是200，视为业务错误
      if (success === false || (code && code !== 200)) {
        // 使用苹果样式的错误通知
        import('./apple-toast').then(({ apiErrorToast }) => {
          const errorMessage = msg || '操作失败，请重试';
          apiErrorToast.business(errorMessage, {
            url: response.config?.url,
            code
          });
        });
        
        // 创建一个带有错误信息的Error对象
        const businessError = new Error(msg || '业务操作失败') as Error & { 
          name: string; 
          response: typeof response 
        };
        businessError.name = 'BusinessError';
        businessError.response = response;
        return Promise.reject(businessError);
      }
    }
    
    return response;
  },
  error => {
    console.error('API请求错误:', error);
    
    // 使用苹果样式的错误通知
    import('./apple-toast').then(({ apiErrorToast }) => {
      if (error.response) {
        const { status, data } = error.response;
        
        switch (status) {
          case 400:
            apiErrorToast.business(data?.msg || data?.message || '请求参数错误');
            break;
          case 401:
            // 防重复显示登录过期通知
            if (!loginExpiredShown) {
              loginExpiredShown = true;
              apiErrorToast.auth('登录已过期，请重新登录');
              
              if (typeof window !== 'undefined') {
                // 清除本地存储的token
                localStorage.removeItem('jwt_token');
                localStorage.removeItem('refresh_token');
                localStorage.removeItem('user_info');
                
                // 5秒后重置标志位，允许再次显示（防止页面长时间停留的情况）
                setTimeout(() => {
                  loginExpiredShown = false;
                }, 5000);
              }
            }
            break;
          case 403:
            apiErrorToast.permission('没有权限访问此资源');
            break;
          case 404:
            apiErrorToast.network('请求的资源不存在', status);
            break;
          case 500:
            apiErrorToast.network(data?.msg || data?.message || '服务器内部错误', status);
            break;
          case 502:
            apiErrorToast.network('网关错误，服务暂时不可用', status);
            break;
          case 503:
            apiErrorToast.network('服务暂时不可用，请稍后重试', status);
            break;
          default:
            apiErrorToast.network(data?.msg || data?.message || `请求失败`, status);
        }
      } else if (error.request) {
        apiErrorToast.network('网络连接失败，请检查网络设置');
      } else {
        apiErrorToast.business(error.message || '请求配置错误');
      }
    });

    return Promise.reject(error);
  }
);

// 导出版本化的API请求函数
export const apiV1 = {
  get: (url: string, params?: Record<string, unknown>, config?: Record<string, unknown>) => {
    // 如果 params 是一个对象且包含 headers，说明这是新的调用方式
    if (params && typeof params === 'object' && params.headers && !config) {
      // 新的调用方式：apiV1.get(url, { headers: {...} })
      return apiClientV1.get(url, params)
    } else {
      // 原来的调用方式：apiV1.get(url, params, config) 或 apiV1.get(url, params)
      return apiClientV1.get(url, { params, ...config })
    }
  },
  post: (url: string, data: unknown, config?: Record<string, unknown>) => {
    // 根据数据类型设置Content-Type
    let contentTypeHeaders = {}
    if (data instanceof FormData) {
      // FormData 让浏览器自动设置multipart/form-data
      contentTypeHeaders = {}
    } else if (data instanceof URLSearchParams) {
      // URLSearchParams 设置为表单编码
      contentTypeHeaders = { 'Content-Type': 'application/x-www-form-urlencoded' }
    } else {
      // 普通对象设置为JSON
      contentTypeHeaders = { 'Content-Type': 'application/json' }
    }
    
    // 合并headers，用户传入的headers优先级更高
    const mergedConfig = {
      ...config,
      headers: {
        ...contentTypeHeaders,
        ...(config?.headers || {})
      }
    }
    
    return apiClientV1.post(url, data, mergedConfig)
  },
  put: (url: string, data: unknown, config?: Record<string, unknown>) => 
    apiClientV1.put(url, data, config),
  delete: (url: string, config?: Record<string, unknown>) => 
    apiClientV1.delete(url, config),
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