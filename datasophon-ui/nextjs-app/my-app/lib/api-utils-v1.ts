import { apiV1, API_PATHS_V1 } from './api-config-v1'
import { createClusterHeaders } from './cluster-id-header'
import type { 
  GetServiceRoleListParams,
  GetServiceRoleListResponse,
  GetAllHostParams,
  GetAllHostResponse,
  SaveServiceRoleHostMappingResponse,
  HostMapping
} from '@/types/master-role-assign'

/**
 * 版本化的集群相关API调用工具函数
 * 基于v1版本的API路径
 */

export const clusterApiV1 = {
  // 机架管理
  rack: {
    list: () => apiV1.post(API_PATHS_V1.RACK_LIST, {}),
    save: (rack: string) => apiV1.post(API_PATHS_V1.RACK_SAVE, { rack }),
    delete: (rackId: number) => apiV1.post(API_PATHS_V1.RACK_DELETE, { rackId }),
  },

  // 主机管理
  host: {
    list: (params: {
      hostname?: string
      ip?: string
      cpuArchitecture?: string
      hostState?: number
      orderField?: string
      orderType?: string
      page: number
      pageSize: number
    }) => apiV1.post(API_PATHS_V1.CLUSTER_HOST_LIST, params),
    all: () => apiV1.post(API_PATHS_V1.CLUSTER_HOST_ALL, {}),
    getRack: () => apiV1.post(API_PATHS_V1.CLUSTER_HOST_GET_RACK, {}),
    assignRack: (rack: string, hostIds: string) => 
      apiV1.post(API_PATHS_V1.CLUSTER_HOST_ASSIGN_RACK, { rack, hostIds }),
    
    // 注释：saveKubernetesHost已移除，使用新的unifiedHost API
    
    // 分析主机列表
    analysisHostList: (params: {
      ips: string
      sshUser: string
      sshPort: string
      sshPassword: string
      page: number
      pageSize: number
    }) => apiV1.post(API_PATHS_V1.ANALYSIS_HOST_LIST, params),
  },

  // 标签管理
  label: {
    list: () => apiV1.post(API_PATHS_V1.TAG_LIST, {}),
    save: (nodeLabel: string) => apiV1.post(API_PATHS_V1.TAG_SAVE, { nodeLabel }),
    delete: (nodeLabelId: number) => apiV1.post(API_PATHS_V1.TAG_DELETE, { nodeLabelId }),
    assign: (nodeLabelId: number, hostIds: string) => 
      apiV1.post(API_PATHS_V1.TAG_ASSIGN, { nodeLabelId, hostIds }),
  },

  // 告警组管理
  alert: {
    groupList: (params: {
      alertGroupName?: string
      page: number
      pageSize: number
    }) => apiV1.post(API_PATHS_V1.ALERT_GROUP_LIST, params),
  },

  // 日志审计
  log: {
    list: (params: any) => apiV1.post(API_PATHS_V1.LOG_LIST, params),
    serviceNameList: () => apiV1.get(API_PATHS_V1.LOG_SERVICE_NAME_LIST),
    moduleList: () => apiV1.get(API_PATHS_V1.LOG_MODULE_LIST),
  },

  // 集群信息
  info: {
    runningList: () => apiV1.post(API_PATHS_V1.CLUSTER_RUNNING_LIST, {}),
    detail: (clusterId: string) => apiV1.get(`${API_PATHS_V1.CLUSTER_DETAIL}/${clusterId}`),
  },

  // 集群配置
  config: {
    // 获取Kubernetes命名空间列表
    getNamespaces: (kubeConfigContent: string) => {
      return apiV1.post(API_PATHS_V1.CLUSTER_NAMESPACES, { kubeConfigContent })
    },
    // 保存Kubernetes配置
    saveKubeConfig: (clusterId: string, kubeConfigContent: string, namespace: string, customNamespace?: string) => {
      const config = { headers: createClusterHeaders(clusterId) };
      return apiV1.post(API_PATHS_V1.CLUSTER_KUBE_CONFIG, { 
        kubeConfig: kubeConfigContent, 
        namespace, 
        customNamespace 
      }, config);
    },
  },

  // Kubernetes集群管理
  kubernetes: {
    // 获取Kubernetes节点信息
    getNodes: (params: { kubeconfig: string; namespace: string }) => {
      // 调用后端API获取真实的K8S节点信息
      return apiV1.post(API_PATHS_V1.CLUSTER_K8S_NODES, params)
    },
  },

  // 主机环境校验 (Step2) - V1版本
  hostInstall: {
    // 获取安装步骤
    getInstallStep: (type: number) => 
      apiV1.get(API_PATHS_V1.GET_INSTALL_STEP, { type }),
    
    
    // 查询主机校验状态
    getHostCheckStatus: (params: {
      sshUser: string
      sshPort: number
    }) => apiV1.post(API_PATHS_V1.GET_HOST_CHECK_STATUS, params),
    
    // 查询主机环境校验是否完成
    checkCompleted: () => 
      apiV1.post(API_PATHS_V1.HOST_CHECK_COMPLETED, {}),
    
    // 清理主机检查资源
    cleanupResources: () =>
      apiV1.post(API_PATHS_V1.CLEANUP_HOST_CHECK_RESOURCES, {}),
    
    // 清理主机环境校验缓存
    clearCache: () => apiV1.get(API_PATHS_V1.CLEAR_HOST_ENV_CHECK_CACHE),
    
    // 主机agent分发进度列表
    getAgentList: (params: {
      installStateCode: number
      page: number
      pageSize: number
    }) => apiV1.post(API_PATHS_V1.DISPATCHER_HOST_AGENT_LIST, params),
    
    // 查询主机agent分发是否完成
    agentCompleted: () =>
      apiV1.post(API_PATHS_V1.DISPATCHER_HOST_AGENT_COMPLETED, {}),
    
    // 主机agent分发取消
    cancelAgent: (params: {
      ip: string
      installStateCode: number
    }) => apiV1.post(API_PATHS_V1.CANCEL_DISPATCHER_HOST_AGENT, params),
    
    // 主机agent分发重试
    restartAgent: (ips: string) =>
      apiV1.post(API_PATHS_V1.RESTART_DISPATCHER_HOST_AGENT, { ips }),
    
    // 生成主机agent操作命令
    generateAgentCommand: (params: {
      clusterHostIds: string
      commandType: string
    }) => apiV1.post(API_PATHS_V1.GENERATE_HOST_AGENT_COMMAND, params),
    
    // 生成主机服务操作命令
    generateServiceCommand: (params: {
      clusterHostIds: string
      commandType: string
    }) => apiV1.post(API_PATHS_V1.GENERATE_HOST_SERVICE_COMMAND, params),
    
    // 开始主机检查
    startCheck: () =>
      apiV1.post(API_PATHS_V1.START_HOST_CHECK, {}),
    
    // 获取主机最近日志
    getWorkerLog: (ip: string) =>
      apiV1.get(API_PATHS_V1.GET_WORKER_LOG + `?ip=${ip}`),
  },

  // 主机检查相关
  hostCheck: {
    // 重试主机环境校验
    retry: (params: {
      hostnames: string
      clusterId: string
      sshUser: string
      sshPort: string
    }) => apiV1.post(API_PATHS_V1.REHOST_CHECK, params),
    

  },

  // 服务管理相关
  service: {
    // 获取服务列表（带必需服务信息）
    // 注意：集群ID通过请求头传递，type参数指定服务类型（core/custom）
    listWithRequired: (params: {
      type: string
    }, config?: any) => apiV1.get(API_PATHS_V1.FRAME_SERVICE_LIST_WITH_REQUIRED, {
      type: params.type
    }, config),
    
    // 获取基础服务列表
    // 注意：集群ID通过请求头传递
    list: (config?: any) => apiV1.post(API_PATHS_V1.CLUSTER_SERVICE_LIST, {}, config),
  },

  // 统一主机管理相关 - 新架构
  unifiedHost: {
    // 发现主机（自动选择PVM或K8S策略）
    discover: (connectionParams: any) =>
      apiV1.post(API_PATHS_V1.HOST_DISCOVER, connectionParams),
    
    // 从Step1配置发现主机（集群配置阶段专用）
    discoverFromStep1: (step1Config: {
      clusterType: string
      // PVM parameters
      hosts?: string
      sshUser?: string
      sshPort?: string
      sshPassword?: string
      // K8S parameters
      kubeConfigContent?: string
      namespace?: string
      isCreatingNewNamespace?: boolean
      customNamespace?: string
      clusterVersion?: string
      namespaces?: string[]
      forceRefresh?: boolean
    }, config?: any) => apiV1.post(API_PATHS_V1.HOST_DISCOVER_FROM_STEP1, step1Config, config),
    
    // 校验所有主机状态（Step2下一步前的校验）
    validateForNextStep: (config?: any) => {
      return apiV1.get(API_PATHS_V1.HOST_CHECK, undefined, config)
    },
    
    // ========== 配置进度管理相关 (简化版) ==========
    

    
    // 获取主机列表（支持分页和筛选）
    list: (params: {
      page?: number
      pageSize?: number
      hostname?: string
      ip?: string
      cpuArchitecture?: string
      hostState?: number
      orderField?: string
      orderType?: string
    }, config?: any) => apiV1.get(API_PATHS_V1.HOST_LIST, params, config),
    
    // 导入主机
    import: (data: {
      selectedHosts: any[]
      connectionParams?: any
      importOptions?: any
    }) => apiV1.post(API_PATHS_V1.HOST_IMPORT, data),
    
    // 刷新主机信息
    refresh: (connectionParams: any) =>
      apiV1.post(API_PATHS_V1.HOST_REFRESH, connectionParams),
    
    // 检查连接状态
    checkConnection: (connectionParams: any) =>
      apiV1.post(API_PATHS_V1.HOST_CHECK_CONNECTION, connectionParams),
    
    // 执行主机环境检查
    performCheck: (data: {
      hostnames: string[]
      connectionParams: any
    }) => apiV1.post(API_PATHS_V1.HOST_PERFORM_CHECK, data),
    
    // 获取主机检查状态
    getCheckStatus: () =>
      apiV1.get(API_PATHS_V1.HOST_CHECK_STATUS),
    
    // 清理资源
    cleanup: () =>
      apiV1.post(API_PATHS_V1.HOST_CLEANUP, {}),
    
    // 获取支持的策略类型
    getStrategies: () =>
      apiV1.get(API_PATHS_V1.HOST_STRATEGIES),
  },

  // 服务角色分配相关 API (Step5)
  serviceRole: {
    /** 获取服务角色列表 */
    getList: async (params: GetServiceRoleListParams): Promise<GetServiceRoleListResponse> => {
      // 集群ID通过请求头传递，其他参数作为查询参数
      const headers = createClusterHeaders(params.clusterId)
      const response = await apiV1.get(API_PATHS_V1.GET_SERVICE_ROLE_LIST, {
        serviceIds: params.serviceIds,
        serviceRoleType: params.serviceRoleType
      }, {
        headers
      })
      return response.data
    },

    /** 获取所有主机列表 */
    getAllHosts: async (params: GetAllHostParams): Promise<GetAllHostResponse> => {
      // 使用GET请求，集群ID通过@ClusterId注解从请求头获取
      const headers = createClusterHeaders(params.clusterId)
      
      const response = await apiV1.get(API_PATHS_V1.GET_ALL_HOST, {
        headers
      })
      return response.data
    },

    /** 保存服务角色主机映射 */
    saveMapping: async (clusterId: string, mappings: HostMapping[]): Promise<SaveServiceRoleHostMappingResponse> => {
      const headers = createClusterHeaders(clusterId)
      const response = await apiV1.post(API_PATHS_V1.SAVE_SERVICE_ROLE_HOST_MAPPING_V2, mappings, { headers })
      return response.data
    },

    /** 获取非Master角色列表 (Step6) */
    getNonMasterRoleList: async (clusterId: string, serviceIds: string): Promise<any> => {
      const headers = createClusterHeaders(clusterId)
      const response = await apiV1.get(API_PATHS_V1.GET_NON_MASTER_ROLE_LIST, {
        serviceIds
      }, { headers })
      return response.data
    }
  },

  // Agent分发相关 API (Step3)
  agent: {
    /** 开始Agent分发 */
    startDistribution: async (clusterId: string, hostIds: string[]) => { // 修复：20位long精度问题
      const headers = createClusterHeaders(clusterId)
      const response = await apiV1.post(API_PATHS_V1.START_AGENT_DISTRIBUTION, { hostIds }, { headers })
      return response.data
    },

    /** 获取Agent分发状态 */
    getDistributionStatus: async (clusterId: string, taskId?: string) => {
      const headers = createClusterHeaders(clusterId)
      const response = await apiV1.get(API_PATHS_V1.GET_AGENT_DISTRIBUTION_STATUS, 
        taskId ? { taskId } : {}, { headers })
      return response.data
    },

    /** 重试失败的Agent分发 */
    retryDistribution: async (clusterId: string, hostIds: string[]) => { // 修复：20位long精度问题
      const headers = createClusterHeaders(clusterId)
      const response = await apiV1.post(API_PATHS_V1.RETRY_AGENT_DISTRIBUTION, { hostIds }, { headers })
      return response.data
    },

    /** 取消Agent分发 */
    cancelDistribution: async (clusterId: string, taskId: string) => {
      const headers = createClusterHeaders(clusterId)
      const response = await apiV1.post(API_PATHS_V1.CANCEL_AGENT_DISTRIBUTION, { taskId }, { headers })
      return response.data
    }
  }
}

// 版本兼容性工具
export const ApiCompatibility = {
  // 检查当前使用的API版本
  getCurrentApiVersion: () => 'v1',
  
  // 检查是否支持某个功能
  isFeatureSupported: (feature: string, version: string = 'v1') => {
    const featureMatrix: Record<string, string[]> = {
      'v1': [
        'host-install',
        'host-check', 
        'cluster-management',
        'user-management',
        'kubernetes-support'
      ]
    };
    return featureMatrix[version]?.includes(feature) || false;
  },
  
  // 获取API变更说明
  getApiChanges: () => {
    // 将来用于API版本迁移指导
    return {
      breaking: [],
      deprecated: [],
      added: []
    };
  }
};

export default clusterApiV1