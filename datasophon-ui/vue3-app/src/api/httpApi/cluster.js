import config from '@/config'

// 后台服务地址前缀
const baseUrl = config.apiBaseUrl

export default {
  // 集群管理接口
  getColonyList: baseUrl + '/api/cluster/list',                   // 获取集群列表
  saveColony: baseUrl + '/api/cluster/save',                      // 集群保存
  updateColony: baseUrl + '/api/cluster/update',                  // 集群更新
  deleteColony: baseUrl + '/api/cluster/delete',                  // 集群删除
  authCluster: baseUrl + '/api/cluster/user/saveClusterManager',  // 集群授权
  getFrameList: baseUrl + '/api/frame/list',                      // 获取服务框架列表
  runningClusterList: baseUrl + '/api/cluster/runningClusterList',// 正在运行状态集群列表
  
  // 集群服务相关接口
  getServiceListByCluster: baseUrl + '/api/cluster/service/list', // 获取集群服务列表
  getDashboardUrl: baseUrl + '/cluster/service/dashboard/getDashboardUrl',      // 查询总览地址
  getDatasophonDashboardUrl: baseUrl + '/cluster/service/dashboard/getDatasophonDashboard', // 查询Datasophon总览地址
  
  // 集群组件相关接口
  reNameGroup: baseUrl + '/cluster/service/instance/role/group/rename',
  delGroup: baseUrl + '/cluster/service/instance/role/group/delete',
  
  // 节点标签相关接口
  saveLabel: baseUrl + '/cluster/node/label/save',
  assginLabel: baseUrl + '/cluster/node/label/assign',
  deleteLabel: baseUrl + '/cluster/node/label/delete',
  getLabelList: baseUrl + '/cluster/node/label/list',
  
  // 机架相关接口
  saveRack: baseUrl + '/cluster/rack/save',
  assginRack: baseUrl + '/api/cluster/host/assignRack',
  deleteRack: baseUrl + '/cluster/rack/delete',
  deleteClusterRack: baseUrl + '/cluster/rack/delete',
  getRackList: baseUrl + '/cluster/rack/list',
  
  // 组件包管理接口
  getParcelList: baseUrl + '/cluster/parcel/list',
  getParcelParse: baseUrl + '/cluster/parcel/parse',
  getParcelProcess: baseUrl + '/cluster/parcel/process',
  downloadComponent: baseUrl + '/cluster/parcel/download',
  installComponent: baseUrl + '/cluster/parcel/install',
  
  // 集群详情相关接口
  getClusterDetail: baseUrl + '/api/cluster/detail', // 获取集群详细信息
  
  // Kubernetes相关接口
  getKubernetesNamespaces: baseUrl + '/api/cluster/namespaces', // 获取Kubernetes命名空间列表
  updateClusterKubeConfig: baseUrl + '/api/cluster/kube-config', // 更新集群Kubernetes配置
  
  // 用户相关接口
  queryAllUser: baseUrl + '/api/user/list', // 查询所有用户
} 