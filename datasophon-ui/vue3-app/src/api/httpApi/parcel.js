import { axiosPost, axiosGet } from '@/utils/request'
import API_PATHS from './apiPaths'

/**
 * 获取存储库列表
 */
export function getParcelList(params = {}) {
  return axiosPost(API_PATHS.getParcelList, params)
}

/**
 * 解析存储库URL
 */
export function getParcelParse(params) {
  return axiosPost(API_PATHS.getParcelParse, params)
}

/**
 * 下载组件
 */
export function downloadComponent(params) {
  return axiosPost(API_PATHS.downloadComponent, params)
}

/**
 * 安装组件
 */
export function installComponent(params) {
  return axiosPost(API_PATHS.installComponent, params)
}

/**
 * 获取任务进度
 */
export function getParcelProcess(params) {
  return axiosGet(API_PATHS.getParcelProcess, params)
} 