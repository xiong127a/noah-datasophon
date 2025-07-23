import { useRouter } from 'vue-router'
import { useSettingsStore } from '../stores/settings'

/**
 * 修改路由并存储相关信息到Pinia
 * @param {Array} data - 服务列表数据
 * @param {String} clusterId - 集群ID
 */
export function changeRouter(data, clusterId, router) {
  // 由于这是一个工具函数，不能直接使用组合式API的hooks
  // 所以我们需要在调用这个函数的组件中传入router和store实例
  const settingsStore = useSettingsStore()
  
  // 保存是否是集群状态
  settingsStore.setIsCluster(true)
  
  // 保存集群ID
  settingsStore.setClusterId(clusterId)
  
  // 构建菜单数据
  const menuData = buildMenuData(data)
  
  // 保存菜单数据
  settingsStore.setMenuData(menuData)
  
  // 如果有路由实例，导航到第一个服务页面
  if (router && menuData.length > 0) {
    router.push(menuData[0].path)
  }
}

/**
 * 构建菜单数据
 * @param {Array} data - 服务列表数据
 * @returns {Array} - 菜单数据
 */
function buildMenuData(data) {
  if (!data || !Array.isArray(data)) {
    return []
  }
  
  // 构建菜单数据
  return data.map(item => {
    return {
      name: item.serviceName,
      path: `/service/${item.serviceInstanceId}`,
      meta: {
        icon: getServiceIcon(item.serviceName),
        title: item.serviceName
      },
      component: 'ServiceDetail',
      id: item.id
    }
  })
}

/**
 * 根据服务名称获取对应的图标
 * @param {String} serviceName - 服务名称
 * @returns {String} - 图标名称
 */
function getServiceIcon(serviceName) {
  // 服务图标映射
  const iconMap = {
    'HDFS': 'hdfs',
    'YARN': 'yarn',
    'HIVE': 'hive',
    'HBASE': 'hbase',
    'SPARK': 'spark',
    'ZOOKEEPER': 'zookeeper',
    'KAFKA': 'kafka',
    'FLINK': 'flink',
    'ELASTICSEARCH': 'elasticsearch',
    'KIBANA': 'kibana',
    'RANGER': 'ranger',
    'ATLAS': 'atlas',
    'KYLIN': 'kylin',
    'DOLPHINSCHEDULER': 'dolphinscheduler',
    'AMBARI': 'ambari',
    'AZKABAN': 'azkaban',
    'HUDI': 'hudi',
    'PRESTO': 'presto',
    'TRINO': 'trino',
    'PROMETHEUS': 'prometheus',
    'GRAFANA': 'grafana',
    'AIRFLOW': 'airflow',
    'SUPERSET': 'superset',
    'ALLUXIO': 'alluxio',
    'DORIS': 'doris',
    'FLINK_SQL': 'flink',
    'MYSQL': 'mysql'
  }
  
  // 返回图标名称，如果没有匹配则返回默认图标
  return iconMap[serviceName.toUpperCase()] || 'default-service'
}

export default {
  changeRouter
} 