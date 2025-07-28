import useSettingStore from '@/stores/useSettingStore';

/**
 * 根据后端返回的菜单数据，动态更改路由
 * @param data 后端返回的菜单数据
 * @param clusterId 集群ID
 */
export const changeRouter = (data: any[], clusterId: string) => {
  const { setMenuData, setClusterId, setIsCluster } = useSettingStore.getState();
  
  // 设置集群ID
  setClusterId(clusterId);
  
  // 设置菜单数据
  setMenuData(data);
  
  // 标记当前是集群模式
  setIsCluster(true);
}; 