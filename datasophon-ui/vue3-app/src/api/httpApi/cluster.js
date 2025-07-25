import { axiosPost, axiosGet, axiosDelete } from '@/utils/request'
import API_PATHS from './apiPaths'

/**
 * 获取集群列表
 */
export function getClusterList(params = {}) {
  return axiosPost(API_PATHS.getColonyList, params)
}

/**
 * 添加集群
 */
export function addColony(params) {
  return axiosPost(API_PATHS.saveColony, params)
}

/**
 * 删除集群
 */
export function deleteColony(params) {
  return axiosPost(API_PATHS.deleteColony, params)
}

/**
 * 获取集群服务列表
 */
export function getServiceListByCluster(clusterId) {
  return axiosPost(API_PATHS.getServiceListByCluster, {
    clusterId
  })
}

/**
 * 更新集群
 */
export function updateColony(params) {
  return axiosPost(API_PATHS.updateColony, params)
}

/**
 * 保存/创建集群
 */
export function saveColony(params) {
  return axiosPost(API_PATHS.saveColony, params)
}

/**
 * 获取集群框架列表
 */
export function getFrameList(params = {}) {
  return axiosPost(API_PATHS.getFrameList, params)
}

/**
 * 删除服务
 */
export function deleteService(serviceId) {
  return axiosGet(`${API_PATHS.deleteService}/${serviceId}`)
}

/**
 * 查询所有用户
 */
export function queryAllUser(params = {}) {
  return axiosPost(API_PATHS.queryAllUser, params)
}

/**
 * 集群授权
 */
export function authCluster(params = {}) {
  return axiosPost(API_PATHS.authCluster, params)
} 