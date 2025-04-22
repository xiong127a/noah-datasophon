// 确保全局API对象存在
if (typeof window.global === 'undefined') {
  window.global = {};
}

if (typeof window.global.API === 'undefined') {
  window.global.API = {};
}

// K8S仪表盘 - 使用window.global确保全局访问
window.global.API.getK8sNamespaces = '/api/k8s/dashboard/namespaces'
window.global.API.getK8sDeployments = '/api/k8s/dashboard/deployments'
window.global.API.getK8sDeploymentDetail = '/api/k8s/dashboard/deployment/detail'
window.global.API.getK8sDeploymentEvents = '/api/k8s/dashboard/deployment/events'
window.global.API.getK8sDeploymentMetrics = '/api/k8s/dashboard/deployment/metrics'
// 新增K8s资源统计接口
window.global.API.getK8sResourceStats = '/api/k8s/dashboard/resource-stats'
// 添加其他K8s资源接口
window.global.API.getK8sCronJobs = '/api/k8s/dashboard/cronjobs'
window.global.API.getK8sDaemonSets = '/api/k8s/dashboard/daemonsets'
window.global.API.getK8sJobs = '/api/k8s/dashboard/jobs'
window.global.API.getK8sPods = '/api/k8s/dashboard/pods'
window.global.API.getK8sReplicaSets = '/api/k8s/dashboard/replicasets'
window.global.API.getK8sReplicationControllers = '/api/k8s/dashboard/replicationcontrollers'
window.global.API.getK8sStatefulSets = '/api/k8s/dashboard/statefulsets'
window.global.API.getK8sServices = '/api/k8s/dashboard/services'
window.global.API.getK8sConfigMaps = '/api/k8s/dashboard/configmaps'

// 导出API对象，方便其他模块导入使用
export default window.global.API;