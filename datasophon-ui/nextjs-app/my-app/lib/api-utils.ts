import { api } from './api-config'

/**
 * 集群相关的API调用工具函数
 * 这些函数不再需要手动传递clusterId，会自动通过请求头发送
 */

export const clusterApi = {
  // 机架管理
  rack: {
    list: () => api.post('/ddh/cluster/rack/list', {}),
    save: (rack: string) => api.post('/ddh/cluster/rack/save', { rack }),
    delete: (rackId: number) => api.post('/ddh/cluster/rack/delete', { rackId }),
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
    }) => api.post('/ddh/api/cluster/host/list', params),
    all: () => api.post('/ddh/api/cluster/host/all', {}),
    getRack: () => api.post('/ddh/api/cluster/host/getRack', {}),
    assignRack: (rack: string, hostIds: string) => 
      api.post('/ddh/api/cluster/host/assignRack', { rack, hostIds }),
  },

  // 标签管理
  label: {
    list: () => api.post('/ddh/cluster/node/label/list', {}),
    save: (nodeLabel: string) => api.post('/ddh/cluster/node/label/save', { nodeLabel }),
    delete: (nodeLabelId: number) => api.post('/ddh/cluster/node/label/delete', { nodeLabelId }),
    assign: (nodeLabelId: number, hostIds: string) => 
      api.post('/ddh/cluster/node/label/assign', { nodeLabelId, hostIds }),
  },

  // 告警组管理
  alert: {
    groupList: (params: {
      alertGroupName?: string
      page: number
      pageSize: number
    }) => api.post('/ddh/alert/group/list', params),
  },

  // 日志审计
  log: {
    list: (params: any) => api.post('/ddh/api/log/list', params),
    serviceNameList: () => api.get('/ddh/api/log/serviceNameList'),
    moduleList: () => api.get('/ddh/api/log/moduleList'),
  },

  // 集群信息
  info: {
    runningList: () => api.post('/ddh/api/cluster/runningClusterList', {}),
    detail: (clusterId: number) => api.get(`/ddh/api/cluster/info/${clusterId}`),
  }
}

export default clusterApi