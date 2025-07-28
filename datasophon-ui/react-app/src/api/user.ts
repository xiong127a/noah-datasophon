import { get, post, put } from './http';
import API_PATHS from './apiPaths';

/**
 * 获取当前登录用户信息
 */
export const getUserInfo = () => {
  return get(API_PATHS.getUserInfo);
};

/**
 * 获取用户列表
 */
export const getUserList = (params?: any) => {
  return post(API_PATHS.queryAllUser, params);
};

/**
 * 登录
 */
export const login = (data: { username: string; password: string }) => {
  return post(API_PATHS.login, data);
};

/**
 * 登出
 */
export const logout = () => {
  return post(API_PATHS.logout);
}; 