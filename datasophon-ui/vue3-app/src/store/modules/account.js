/**
 * Vuex模块 - 账号
 * 用于处理用户登录、登出、权限等相关的状态
 */
import config from '@/config'
import { axiosPost, axiosGet } from '@/utils/request'

const state = {
  token: localStorage.getItem(config.tokenKey) || '',
  user: JSON.parse(localStorage.getItem(config.userKey) || '{}'),
  permissions: [], // 用户权限列表
  roles: [], // 用户角色列表
  tenants: [], // 用户所属租户列表
  currentTenant: null, // 当前选中的租户
}

const getters = {
  // 获取token
  token: state => state.token,
  
  // 获取用户信息
  user: state => state.user,
  
  // 获取用户权限列表
  permissions: state => state.permissions,
  
  // 获取用户角色列表
  roles: state => state.roles,
  
  // 获取用户所属租户列表
  tenants: state => state.tenants,
  
  // 获取当前选中的租户
  currentTenant: state => state.currentTenant,
  
  // 用户是否是管理员
  isAdmin: state => {
    return state.user && state.user.userType === 1
  },
  
  // 用户是否已登录
  isLoggedIn: state => {
    return !!state.token && Object.keys(state.user).length > 0
  },
}

const mutations = {
  // 设置token
  setToken(state, token) {
    state.token = token
    localStorage.setItem(config.tokenKey, token)
  },
  
  // 设置用户信息
  setUser(state, user) {
    state.user = user
    localStorage.setItem(config.userKey, JSON.stringify(user))
  },
  
  // 设置用户权限列表
  setPermissions(state, permissions) {
    state.permissions = permissions
  },
  
  // 设置用户角色列表
  setRoles(state, roles) {
    state.roles = roles
  },
  
  // 设置用户所属租户列表
  setTenants(state, tenants) {
    state.tenants = tenants
  },
  
  // 设置当前选中的租户
  setCurrentTenant(state, tenant) {
    state.currentTenant = tenant
  },
  
  // 清空用户信息
  clearUser(state) {
    state.token = ''
    state.user = {}
    state.permissions = []
    state.roles = []
    state.tenants = []
    state.currentTenant = null
    localStorage.removeItem(config.tokenKey)
    localStorage.removeItem(config.userKey)
  },
}

const actions = {
  // 登录
  login({ commit }, userInfo) {
    return new Promise((resolve, reject) => {
      axiosPost('/ddh/api/login', userInfo)
        .then(res => {
          if (res.code === 200) {
            const { token, user } = res.data
            
            // 设置token和用户信息
            commit('setToken', token)
            commit('setUser', user)
            
            // 获取用户权限和角色
            dispatch('getUserPermissions')
            
            resolve(res)
          } else {
            reject(res)
          }
        })
        .catch(error => {
          reject(error)
        })
    })
  },
  
  // 登出
  logout({ commit }) {
    return new Promise((resolve, reject) => {
      axiosPost('/ddh/api/logout')
        .then(res => {
          // 清空用户信息
          commit('clearUser')
          
          resolve(res)
        })
        .catch(error => {
          // 清空用户信息
          commit('clearUser')
          
          reject(error)
        })
    })
  },
  
  // 获取用户信息
  getUserInfo({ commit }) {
    return new Promise((resolve, reject) => {
      axiosGet('/ddh/api/user-info')
        .then(res => {
          if (res.code === 200) {
            commit('setUser', res.data)
            resolve(res.data)
          } else {
            reject(res)
          }
        })
        .catch(error => {
          reject(error)
        })
    })
  },
  
  // 获取用户权限和角色
  getUserPermissions({ commit }) {
    return new Promise((resolve, reject) => {
      axiosGet('/ddh/api/user/permissions')
        .then(res => {
          if (res.code === 200) {
            const { permissions, roles } = res.data
            
            commit('setPermissions', permissions)
            commit('setRoles', roles)
            
            resolve(res.data)
          } else {
            reject(res)
          }
        })
        .catch(error => {
          reject(error)
        })
    })
  },
  
  // 获取用户所属租户列表
  getUserTenants({ commit }) {
    return new Promise((resolve, reject) => {
      axiosGet('/ddh/api/user/tenants')
        .then(res => {
          if (res.code === 200) {
            commit('setTenants', res.data)
            
            // 如果有租户列表，设置第一个为当前选中的租户
            if (res.data && res.data.length > 0) {
              commit('setCurrentTenant', res.data[0])
            }
            
            resolve(res.data)
          } else {
            reject(res)
          }
        })
        .catch(error => {
          reject(error)
        })
    })
  },
  
  // 切换当前选中的租户
  switchTenant({ commit }, tenant) {
    commit('setCurrentTenant', tenant)
    
    // 可以根据需要在切换租户后重新获取用户权限等信息
    return Promise.resolve(tenant)
  },
}

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions
} 