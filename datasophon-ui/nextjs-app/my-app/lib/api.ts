/**
 * 统一API入口文件
 * 所有组件都从这里导入API配置，便于统一管理和版本切换
 */

// 导入v1版本作为默认版本
import { 
  apiV1 as api,
  apiClientV1 as apiClient,
  API_PATHS_V1 as API_PATHS,
  API_BASE_URL,
  API_PREFIX,
  API_VERSION,
  API_BASE,
  ApiVersionManager
} from './api-config-v1';

import { 
  clusterApiV1 as clusterApi,
  ApiCompatibility
} from './api-utils-v1';

// 统一导出，保持向后兼容的API接口
export {
  // 基础API客户端
  api,
  apiClient,
  
  // API路径配置
  API_PATHS,
  API_BASE_URL,
  API_PREFIX,
  API_VERSION,
  API_BASE,
  
  // 集群相关API工具
  clusterApi,
  
  // 版本管理工具
  ApiVersionManager,
  ApiCompatibility
};

// 默认导出API客户端
export default api;

/**
 * API版本说明
 * 
 * 当前版本: v1
 * 路径格式: /ddh/api/v1/{path}
 * 
 * 如需切换版本，只需修改上方的import来源即可
 * 所有组件的import无需改动
 */