/**
 * API路径常量
 * 集中管理所有API端点路径
 */
const API_PATHS = {
  // 认证相关
  login: '/ddh/api/login',
  logout: '/ddh/api/logout',
  getUserInfo: '/ddh/api/user/getUserInfo',
  
  // 集群相关
  getColonyList: '/ddh/api/cluster/list',
  saveColony: '/ddh/api/cluster/save',
  updateColony: '/ddh/api/cluster/update',
  deleteColony: '/ddh/api/cluster/delete',
  getFrameList: '/ddh/api/frame/list',
  deleteService: '/ddh/api/frame/service/delete',
  authCluster: '/ddh/api/cluster/user/saveClusterManager',
  
  // 服务相关
  getServiceListByCluster: '/ddh/api/service/getServiceListByCluster',
  
  // 主机相关
  getHostList: '/ddh/api/host/list',
  
  // 用户相关
  queryAllUser: '/ddh/api/user/list',
  
  // 告警相关
  getAlarmList: '/ddh/api/alarm/list',
  
  // Steps组件相关（集群配置）
  getClusterServiceConfigs: '/ddh/api/cluster/getClusterServiceConfigs',
  updateClusterServiceConfigs: '/ddh/api/cluster/updateClusterServiceConfigs',
  validateClusterConfigs: '/ddh/api/cluster/validateClusterConfigs',
  startClusterDeploy: '/ddh/api/cluster/startDeploy',

  // 存储库相关
  getParcelList: '/ddh/api/parcel/list',
  getParcelParse: '/ddh/api/parcel/parse',
  downloadComponent: '/ddh/api/parcel/download',
  installComponent: '/ddh/api/parcel/install',
  getParcelProcess: '/ddh/api/parcel/process'
}

export default API_PATHS 