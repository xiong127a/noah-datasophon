import { apiV1, API_PATHS_V1 } from './api-config-v1'

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
    
    // 保存Kubernetes主机
    saveKubernetesHost: (clusterId: number, hosts: any[]) =>
      apiV1.post(API_PATHS_V1.SAVE_KUBERNETES_HOST + `?clusterId=${clusterId}`, hosts),
    
    // 分析主机列表
    analysisHostList: (params: {
      clusterId: number
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
    detail: (clusterId: number) => apiV1.get(`${API_PATHS_V1.CLUSTER_DETAIL}/${clusterId}`),
  },

  // 集群配置
  config: {
    // 获取Kubernetes命名空间列表
    getNamespaces: (kubeConfigContent: string) => {
      console.log('API V1调用 - 发送kubeConfigContent长度:', kubeConfigContent?.length)
      console.log('API V1调用 - 参数对象:', { kubeConfigContent })
      return apiV1.post(API_PATHS_V1.CLUSTER_NAMESPACES, { kubeConfigContent })
    },
    // 保存Kubernetes配置
    saveKubeConfig: (clusterId: number, kubeConfigContent: string, namespace: string) =>
      apiV1.post(API_PATHS_V1.CLUSTER_KUBE_CONFIG, { clusterId, kubeConfigContent, namespace }),
  },

  // 主机环境校验 (Step2) - V1版本
  hostInstall: {
    // 获取安装步骤
    getInstallStep: (type: number) => 
      apiV1.get(API_PATHS_V1.GET_INSTALL_STEP, { type }),
    
    // 解析主机列表
    analysisList: (params: {
      pageSize: number
      page: number
      clusterId: number
      hosts?: string
      sshUser?: string
      sshPort?: string
      sshPassword?: string
      kubeConfigContent?: string
    }) => apiV1.post(API_PATHS_V1.ANALYSIS_HOST_LIST, params),
    
    // 查询主机校验状态
    getHostCheckStatus: (params: {
      clusterId: number
      sshUser: string
      sshPort: number
    }) => apiV1.post(API_PATHS_V1.GET_HOST_CHECK_STATUS, params),
    
    // 查询主机环境校验是否完成
    checkCompleted: (clusterId: number) => 
      apiV1.post(API_PATHS_V1.HOST_CHECK_COMPLETED, { clusterId }),
    
    // 清理主机检查资源
    cleanupResources: (clusterId: number) =>
      apiV1.post(API_PATHS_V1.CLEANUP_HOST_CHECK_RESOURCES, { clusterId }),
    
    // 清理主机环境校验缓存
    clearCache: () => apiV1.get(API_PATHS_V1.CLEAR_HOST_ENV_CHECK_CACHE),
    
    // 主机agent分发进度列表
    getAgentList: (params: {
      clusterId: number
      installStateCode: number
      page: number
      pageSize: number
    }) => apiV1.post(API_PATHS_V1.DISPATCHER_HOST_AGENT_LIST, params),
    
    // 查询主机agent分发是否完成
    agentCompleted: (clusterId: number) =>
      apiV1.post(API_PATHS_V1.DISPATCHER_HOST_AGENT_COMPLETED, { clusterId }),
    
    // 主机agent分发取消
    cancelAgent: (params: {
      clusterId: number
      ip: string
      installStateCode: number
    }) => apiV1.post(API_PATHS_V1.CANCEL_DISPATCHER_HOST_AGENT, params),
    
    // 主机agent分发重试
    restartAgent: (clusterId: number, ips: string) =>
      apiV1.post(API_PATHS_V1.RESTART_DISPATCHER_HOST_AGENT, { clusterId, ips }),
    
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
    startCheck: (clusterId: number) =>
      apiV1.post(API_PATHS_V1.START_HOST_CHECK, { clusterId }),
    
    // 获取主机最近日志
    getWorkerLog: (ip: string, clusterId: number) =>
      apiV1.get(API_PATHS_V1.GET_WORKER_LOG, { ip, clusterId }),
  },

  // 主机检查相关
  hostCheck: {
    // 重试主机环境校验
    retry: (params: {
      hostnames: string
      clusterId: number
      sshUser: string
      sshPort: string
    }) => apiV1.post(API_PATHS_V1.REHOST_CHECK, params),
    
    // K8S模式：保存Kubernetes主机
    saveK8sHosts: (clusterId: number, hostInfoList: any[]) =>
      apiV1.post(`${API_PATHS_V1.SAVE_KUBERNETES_HOST}?clusterId=${clusterId}`, hostInfoList),
    
    // K8S模式：直接保存Kubernetes主机（完整硬件信息）
    saveK8sHostsDirect: (clusterId: number, kubernetesHosts: any[]) =>
      apiV1.post(`${API_PATHS_V1.SAVE_KUBERNETES_HOST_DIRECT}?clusterId=${clusterId}`, kubernetesHosts),
    
    // K8S模式：获取完整硬件信息
    getK8sHostsWithHardwareInfo: (clusterId: number) =>
      apiV1.get(`${API_PATHS_V1.GET_K8S_HOSTS_WITH_HARDWARE_INFO}?clusterId=${clusterId}`),
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
  getApiChanges: (fromVersion: string, toVersion: string) => {
    // 将来用于API版本迁移指导
    return {
      breaking: [],
      deprecated: [],
      added: []
    };
  }
};

export default clusterApiV1