/**
 * 全局配置文件
 */

// 环境变量
const env = process.env.NODE_ENV || 'development'

// 配置对象
const config = {
  // 开发环境配置
  development: {
    apiBaseUrl: '/ddh', // API基础URL，开发环境使用代理
    userKey: 'userInfo', // 用户信息存储键名
    tokenKey: 'token', // Token存储键名
    requestTimeout: 60000, // 请求超时时间
    enableMock: true, // 是否启用Mock数据
    enableLog: true, // 是否启用日志
  },
  
  // 生产环境配置
  production: {
    apiBaseUrl: '/ddh', // API基础URL，生产环境根据实际情况配置
    userKey: 'userInfo',
    tokenKey: 'token',
    requestTimeout: 60000,
    enableMock: false,
    enableLog: false,
  },
  
  // 测试环境配置
  test: {
    apiBaseUrl: '/ddh',
    userKey: 'userInfo',
    tokenKey: 'token',
    requestTimeout: 60000,
    enableMock: true,
    enableLog: true,
  }
}

// 导出当前环境的配置
export default config[env] 