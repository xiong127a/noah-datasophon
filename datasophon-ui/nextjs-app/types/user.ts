// 用户数据类型定义
export interface User {
  id: string; // 修复：20位long精度问题
  username: string;
  email: string;
  phone: string;
  createTime: string;
  updateTime?: string;
  userType?: number; // 1-管理员，2-普通用户
  bio?: string; // 个人简介
  lastLoginTime?: string; // 最后登录时间
  previousLoginTime?: string; // 上次登录时间
  avatar?: string; // 用户头像（Base64编码）
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
  userType?: number; // 用户类型: 1-管理员，2-普通用户
  bio?: string; // 个人简介
  avatar?: string; // 用户头像（Base64编码）
}

// 添加用户请求数据
export interface CreateUserRequest extends UserFormData {
  password: string; // 创建时密码必填
}

// 更新用户请求数据
export interface UpdateUserRequest extends UserFormData {
  id: string; // 修复：20位long精度问题
}

// 删除用户请求参数
export interface DeleteUserRequest {
  id: string; // 修复：20位long精度问题
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

// 用户类型枚举
export enum UserType {
  ADMIN = 1,    // 管理员
  NORMAL = 2,   // 普通用户
}

// 用户类型选项
export const USER_TYPE_OPTIONS = [
  { value: UserType.ADMIN, label: '系统管理员', color: 'amber' },
  { value: UserType.NORMAL, label: '普通用户', color: 'blue' }
]

// 默认头像类型：使用用户名首字符
export const DEFAULT_INITIALS_AVATAR = 'DEFAULT_INITIALS';

// 内置头像选项（Base64编码的SVG图像）
export const BUILT_IN_AVATARS = [
  // 默认头像：用户名首字符（放在第一位作为默认选项）
  DEFAULT_INITIALS_AVATAR,
  
  // 蓝色圆形头像
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjQiIGhlaWdodD0iNjQiIHZpZXdCb3g9IjAgMCA2NCA2NCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMzIiIGN5PSIzMiIgcj0iMzIiIGZpbGw9InVybCgjZ3JhZGllbnQxKSIvPgo8Y2lyY2xlIGN4PSIzMiIgY3k9IjI2IiByPSIxMCIgZmlsbD0id2hpdGUiLz4KPGVsbGlwc2UgY3g9IjMyIiBjeT0iNTAiIHJ4PSIxNiIgcnk9IjEyIiBmaWxsPSJ3aGl0ZSIvPgo8ZGVmcz4KPGxpbmVhckdyYWRpZW50IGlkPSJncmFkaWVudDEiIHgxPSIwIiB5MT0iMCIgeDI9IjY0IiB5Mj0iNjQiIGdyYWRpZW50VW5pdHM9InVzZXJTcGFjZU9uVXNlIj4KPHN0b3Agc3RvcC1jb2xvcj0iIzM5OGVmNCIvPgo8c3RvcCBvZmZzZXQ9IjEiIHN0b3AtY29sb3I9IiM2MzY2ZjEiLz4KPC9saW5lYXJHcmFkaWVudD4KPC9kZWZzPgo8L3N2Zz4K',
  
  // 绿色圆形头像
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjQiIGhlaWdodD0iNjQiIHZpZXdCb3g9IjAgMCA2NCA2NCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMzIiIGN5PSIzMiIgcj0iMzIiIGZpbGw9InVybCgjZ3JhZGllbnQyKSIvPgo8Y2lyY2xlIGN4PSIzMiIgY3k9IjI2IiByPSIxMCIgZmlsbD0id2hpdGUiLz4KPGVsbGlwc2UgY3g9IjMyIiBjeT0iNTAiIHJ4PSIxNiIgcnk9IjEyIiBmaWxsPSJ3aGl0ZSIvPgo8ZGVmcz4KPGxpbmVhckdyYWRpZW50IGlkPSJncmFkaWVudDIiIHgxPSIwIiB5MT0iMCIgeDI9IjY0IiB5Mj0iNjQiIGdyYWRpZW50VW5pdHM9InVzZXJTcGFjZU9uVXNlIj4KPHN0b3Agc3RvcC1jb2xvcj0iIzEwYjk4MSIvPgo8c3RvcCBvZmZzZXQ9IjEiIHN0b3AtY29sb3I9IiMzNGQ5OWMiLz4KPC9saW5lYXJHcmFkaWVudD4KPC9kZWZzPgo8L3N2Zz4K',
  
  // 紫色圆形头像
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjQiIGhlaWdodD0iNjQiIHZpZXdCb3g9IjAgMCA2NCA2NCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMzIiIGN5PSIzMiIgcj0iMzIiIGZpbGw9InVybCgjZ3JhZGllbnQzKSIvPgo8Y2lyY2xlIGN4PSIzMiIgY3k9IjI2IiByPSIxMCIgZmlsbD0id2hpdGUiLz4KPGVsbGlwc2UgY3g9IjMyIiBjeT0iNTAiIHJ4PSIxNiIgcnk9IjEyIiBmaWxsPSJ3aGl0ZSIvPgo8ZGVmcz4KPGxpbmVhckdyYWRpZW50IGlkPSJncmFkaWVudDMiIHgxPSIwIiB5MT0iMCIgeDI9IjY0IiB5Mj0iNjQiIGdyYWRpZW50VW5pdHM9InVzZXJTcGFjZU9uVXNlIj4KPHN0b3Agc3RvcC1jb2xvcj0iIzhhNWNmNiIvPgo8c3RvcCBvZmZzZXQ9IjEiIHN0b3AtY29sb3I9IiNhNzhhZmYiLz4KPC9saW5lYXJHcmFkaWVudD4KPC9kZWZzPgo8L3N2Zz4K',
  
  // 橙色圆形头像
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjQiIGhlaWdodD0iNjQiIHZpZXdCb3g9IjAgMCA2NCA2NCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMzIiIGN5PSIzMiIgcj0iMzIiIGZpbGw9InVybCgjZ3JhZGllbnQ0KSIvPgo8Y2lyY2xlIGN4PSIzMiIgY3k9IjI2IiByPSIxMCIgZmlsbD0id2hpdGUiLz4KPGVsbGlwc2UgY3g9IjMyIiBjeT0iNTAiIHJ4PSIxNiIgcnk9IjEyIiBmaWxsPSJ3aGl0ZSIvPgo8ZGVmcz4KPGxpbmVhckdyYWRpZW50IGlkPSJncmFkaWVudDQiIHgxPSIwIiB5MT0iMCIgeDI9IjY0IiB5Mj0iNjQiIGdyYWRpZW50VW5pdHM9InVzZXJTcGFjZU9uVXNlIj4KPHN0b3Agc3RvcC1jb2xvcj0iI2Y5NzMxNiIvPgo8c3RvcCBvZmZzZXQ9IjEiIHN0b3AtY29sb3I9IiNmYmI0MjYiLz4KPC9saW5lYXJHcmFkaWVudD4KPC9kZWZzPgo8L3N2Zz4K',
  
  // 粉色圆形头像
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjQiIGhlaWdodD0iNjQiIHZpZXdCb3g9IjAgMCA2NCA2NCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMzIiIGN5PSIzMiIgcj0iMzIiIGZpbGw9InVybCgjZ3JhZGllbnQ1KSIvPgo8Y2lyY2xlIGN4PSIzMiIgY3k9IjI2IiByPSIxMCIgZmlsbD0id2hpdGUiLz4KPGVsbGlwc2UgY3g9IjMyIiBjeT0iNTAiIHJ4PSIxNiIgcnk9IjEyIiBmaWxsPSJ3aGl0ZSIvPgo8ZGVmcz4KPGxpbmVhckdyYWRpZW50IGlkPSJncmFkaWVudDUiIHgxPSIwIiB5MT0iMCIgeDI9IjY0IiB5Mj0iNjQiIGdyYWRpZW50VW5pdHM9InVzZXJTcGFjZU9uVXNlIj4KPHN0b3Agc3RvcC1jb2xvcj0iI2VjNDg5OSIvPgo8c3RvcCBvZmZzZXQ9IjEiIHN0b3AtY29sb3I9IiNmNGJkZjciLz4KPC9saW5lYXJHcmFkaWVudD4KPC9kZWZzPgo8L3N2Zz4K',
  
  // 青色圆形头像
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjQiIGhlaWdodD0iNjQiIHZpZXdCb3g9IjAgMCA2NCA2NCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMzIiIGN5PSIzMiIgcj0iMzIiIGZpbGw9InVybCgjZ3JhZGllbnQ2KSIvPgo8Y2lyY2xlIGN4PSIzMiIgY3k9IjI2IiByPSIxMCIgZmlsbD0id2hpdGUiLz4KPGVsbGlwc2UgY3g9IjMyIiBjeT0iNTAiIHJ4PSIxNiIgcnk9IjEyIiBmaWxsPSJ3aGl0ZSIvPgo8ZGVmcz4KPGxpbmVhckdyYWRpZW50IGlkPSJncmFkaWVudDYiIHgxPSIwIiB5MT0iMCIgeDI9IjY0IiB5Mj0iNjQiIGdyYWRpZW50VW5pdHM9InVzZXJTcGFjZU9uVXNlIj4KPHN0b3Agc3RvcC1jb2xvcj0iIzA2YjZkNCIvPgo8c3RvcCBvZmZzZXQ9IjEiIHN0b3AtY29sb3I9IiMxOGM0ZTYiLz4KPC9saW5lYXJHcmFkaWVudD4KPC9kZWZzPgo8L3N2Zz4K',
  
  // 深蓝色圆形头像
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjQiIGhlaWdodD0iNjQiIHZpZXdCb3g9IjAgMCA2NCA2NCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMzIiIGN5PSIzMiIgcj0iMzIiIGZpbGw9InVybCgjZ3JhZGllbnQ3KSIvPgo8Y2lyY2xlIGN4PSIzMiIgY3k9IjI2IiByPSIxMCIgZmlsbD0id2hpdGUiLz4KPGVsbGlwc2UgY3g9IjMyIiBjeT0iNTAiIHJ4PSIxNiIgcnk9IjEyIiBmaWxsPSJ3aGl0ZSIvPgo8ZGVmcz4KPGxpbmVhckdyYWRpZW50IGlkPSJncmFkaWVudDciIHgxPSIwIiB5MT0iMCIgeDI9IjY0IiB5Mj0iNjQiIGdyYWRpZW50VW5pdHM9InVzZXJTcGFjZU9uVXNlIj4KPHN0b3Agc3RvcC1jb2xvcj0iIzE5NGY5OSIvPgo8c3RvcCBvZmZzZXQ9IjEiIHN0b3AtY29sb3I9IiMzYjgyZjYiLz4KPC9saW5lYXJHcmFkaWVudD4KPC9kZWZzPgo8L3N2Zz4K',
  
  // 红色圆形头像
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjQiIGhlaWdodD0iNjQiIHZpZXdCb3g9IjAgMCA2NCA2NCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMzIiIGN5PSIzMiIgcj0iMzIiIGZpbGw9InVybCgjZ3JhZGllbnQ4KSIvPgo8Y2lyY2xlIGN4PSIzMiIgY3k9IjI2IiByPSIxMCIgZmlsbD0id2hpdGUiLz4KPGVsbGlwc2UgY3g9IjMyIiBjeT0iNTAiIHJ4PSIxNiIgcnk9IjEyIiBmaWxsPSJ3aGl0ZSIvPgo8ZGVmcz4KPGxpbmVhckdyYWRpZW50IGlkPSJncmFkaWVudDgiIHgxPSIwIiB5MT0iMCIgeDI9IjY0IiB5Mj0iNjQiIGdyYWRpZW50VW5pdHM9InVzZXJTcGFjZU9uVXNlIj4KPHN0b3Agc3RvcC1jb2xvcj0iI2RjMjYyNiIvPgo8c3RvcCBvZmZzZXQ9IjEiIHN0b3AtY29sb3I9IiNlZjQ0NDQiLz4KPC9saW5lYXJHcmFkaWVudD4KPC9kZWZzPgo8L3N2Zz4K',
]

// 头像上传响应
export interface AvatarUploadResponse {
  url: string;
} 