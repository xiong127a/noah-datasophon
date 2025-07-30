// 用户数据类型定义
export interface User {
  id: number;
  username: string;
  email: string;
  phone: string;
  createTime: string;
  updateTime?: string;
  userType?: number; // 1表示系统管理员，普通用户为其他值
}

// 用户列表响应类型
export interface UserListResponse {
  data: User[];
  total: number;
  page: number;
  pageSize: number;
}

// 用户列表查询参数
export interface UserListParams {
  page: number;
  pageSize: number;
  username?: string; // 用户名搜索关键词
}

// 添加/编辑用户表单数据
export interface UserFormData {
  username: string;
  password?: string; // 编辑时可能不需要
  email: string;
  phone: string;
}

// 添加用户请求数据
export interface CreateUserRequest extends UserFormData {
  password: string; // 创建时密码必填
}

// 更新用户请求数据
export interface UpdateUserRequest extends UserFormData {
  id: number;
}

// 删除用户请求参数
export interface DeleteUserRequest {
  id: number;
}

// 检查用户名请求参数
export interface CheckUserNameRequest {
  username: string;
  excludeId?: number; // 编辑时排除当前用户ID
}

// API响应基础结构
export interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
  total?: number;
} 