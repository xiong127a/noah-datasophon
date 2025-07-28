/**
 * API路径常量
 * 集中管理所有API端点路径
 */
const API_PATHS = {
  // 认证相关
  login: '/login',
  logout: '/logout',
  getUserInfo: '/user/getUserInfo',
  
  // 集群相关
  getClusterList: '/cluster/list',
  saveCluster: '/cluster/save',
  updateCluster: '/cluster/update',
  deleteCluster: '/cluster/delete',
  getFrameList: '/frame/list',
  deleteService: '/frame/service/delete',
  authCluster: '/cluster/user/saveClusterManager',
  
  // 服务相关
  getServiceListByCluster: '/service/getServiceListByCluster',
  
  // 主机相关
  getHostList: '/host/list',
  
  // 用户相关
  queryAllUser: '/user/list',
  
  // 告警相关
  getAlarmList: '/alarm/list',
  
  // Steps组件相关（集群配置）
  getClusterServiceConfigs: '/cluster/getClusterServiceConfigs',
  updateClusterServiceConfigs: '/cluster/updateClusterServiceConfigs',
  validateClusterConfigs: '/cluster/validateClusterConfigs',
  startClusterDeploy: '/cluster/startDeploy',

  // 存储库相关
  getParcelList: '/parcel/list',
  getParcelParse: '/parcel/parse',
  downloadComponent: '/parcel/download',
  installComponent: '/parcel/install',
  getParcelProcess: '/parcel/process'
}

export default API_PATHS; 