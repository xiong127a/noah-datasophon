import { get, post, del, put } from './http';
import API_PATHS from './apiPaths';

/**
 * 获取集群列表
 */
export const getClusterList = (params?: any) => {
  return post(API_PATHS.getClusterList, params);
};

/**
 * 保存集群
 */
export const saveCluster = (data: any) => {
  return post(API_PATHS.saveCluster, data);
};

/**
 * 更新集群
 */
export const updateCluster = (data: any) => {
  return post(API_PATHS.updateCluster, data);
};

/**
 * 删除集群
 */
export const deleteCluster = (clusterId: string | number) => {
  return post(`${API_PATHS.deleteCluster}?clusterId=${clusterId}`, [clusterId]);
};

/**
 * 授权集群
 */
export const authCluster = (data: any) => {
  return post(API_PATHS.authCluster, data);
};

/**
 * 获取集群服务列表
 */
export const getServiceListByCluster = (clusterId: string | number) => {
  return post(API_PATHS.getServiceListByCluster, { clusterId });
}; 