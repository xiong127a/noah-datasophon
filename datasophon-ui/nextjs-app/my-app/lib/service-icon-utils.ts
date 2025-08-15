/**
 * 服务图标工具函数
 * 统一管理服务图标逻辑 - 开发规范：服务名转小写就是图标文件名
 * 
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 * 日期：2024-01-20
 */

/**
 * 获取服务图标路径
 * 开发规范：服务名称转小写就是图标文件名
 * 
 * @param serviceName 服务名称
 * @returns 图标路径
 */
export const getServiceIconPath = (serviceName: string): string => {
  if (!serviceName) {
    return '/icons/service-default.svg'
  }
  
  // 开发规范：服务名称转小写作为图标文件名
  const iconFileName = serviceName.toLowerCase().trim()
  return `/icons/${iconFileName}.svg`
}
