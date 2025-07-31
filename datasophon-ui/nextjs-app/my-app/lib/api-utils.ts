import { api, API_PATHS } from './api-config'

/**
 * 集群相关的API调用工具函数
 * 这些函数不再需要手动传递clusterId，会自动通过请求头发送
 */

export const clusterApi = {
  // 机架管理
  rack: {
    list: () => api.post(API_PATHS.RACK_LIST, {}),
    save: (rack: string) => api.post(API_PATHS.RACK_SAVE, { rack }),
    delete: (rackId: number) => api.post(API_PATHS.RACK_DELETE, { rackId }),
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
    }) => api.post(API_PATHS.CLUSTER_HOST_LIST, params),
    all: () => api.post(API_PATHS.CLUSTER_HOST_ALL, {}),
    getRack: () => api.post(API_PATHS.CLUSTER_HOST_GET_RACK, {}),
    assignRack: (rack: string, hostIds: string) => 
      api.post(API_PATHS.CLUSTER_HOST_ASSIGN_RACK, { rack, hostIds }),
  },

  // 标签管理
  label: {
    list: () => api.post(API_PATHS.TAG_LIST, {}),
    save: (nodeLabel: string) => api.post(API_PATHS.TAG_SAVE, { nodeLabel }),
    delete: (nodeLabelId: number) => api.post(API_PATHS.TAG_DELETE, { nodeLabelId }),
    assign: (nodeLabelId: number, hostIds: string) => 
      api.post(API_PATHS.TAG_ASSIGN, { nodeLabelId, hostIds }),
  },

  // 告警组管理
  alert: {
    groupList: (params: {
      alertGroupName?: string
      page: number
      pageSize: number
    }) => api.post(API_PATHS.ALERT_GROUP_LIST, params),
  },

  // 日志审计
  log: {
    list: (params: any) => api.post(API_PATHS.LOG_LIST, params),
    serviceNameList: () => api.get(API_PATHS.LOG_SERVICE_NAME_LIST),
    moduleList: () => api.get(API_PATHS.LOG_MODULE_LIST),
  },

  // 集群信息
  info: {
    runningList: () => api.post(API_PATHS.CLUSTER_RUNNING_LIST, {}),
    detail: (clusterId: number) => api.get(`${API_PATHS.CLUSTER_DETAIL}/${clusterId}`),
  },

  // 集群配置 (按照老项目的API设计)
  config: {
    // 获取Kubernetes命名空间列表 (老项目的API)
    getNamespaces: (kubeConfigContent: string) => {
      console.log('API调用 - 发送kubeConfigContent长度:', kubeConfigContent?.length)
      console.log('API调用 - 参数对象:', { kubeConfigContent })
      return api.post(API_PATHS.CLUSTER_NAMESPACES, { kubeConfigContent })
    },
    // 保存Kubernetes配置 (老项目的API)
    saveKubeConfig: (clusterId: number, kubeConfigContent: string, namespace: string) =>
      api.post(API_PATHS.CLUSTER_KUBE_CONFIG, { clusterId, kubeConfigContent, namespace }),
  }
}

export default clusterApi