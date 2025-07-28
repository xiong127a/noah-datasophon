import Cookies from 'js-cookie';

const TokenKey = 'datasophon_token';
const UserInfoKey = 'datasophon_user_info';

// Token操作
export function getToken(): string | undefined {
  return Cookies.get(TokenKey);
}

export function setToken(token: string): void {
  Cookies.set(TokenKey, token);
}

export function removeToken(): void {
  Cookies.remove(TokenKey);
}

// 用户信息操作
export interface UserInfo {
  userId: number;
  username: string;
  realName?: string;
  avatar?: string;
  roles?: string[];
  permissions?: string[];
}

export function getUserInfo(): UserInfo | null {
  const userInfoStr = localStorage.getItem(UserInfoKey);
  if (!userInfoStr) return null;
  
  try {
    return JSON.parse(userInfoStr) as UserInfo;
  } catch (error) {
    console.error('解析用户信息失败', error);
    return null;
  }
}

export function setUserInfo(userInfo: UserInfo): void {
  localStorage.setItem(UserInfoKey, JSON.stringify(userInfo));
}

export function removeUserInfo(): void {
  localStorage.removeItem(UserInfoKey);
}

// 清除所有认证信息
export function clearAuthInfo(): void {
  removeToken();
  removeUserInfo();
} 