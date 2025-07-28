import { create } from 'zustand';
import { getUserInfo } from '@/api/user';
import { getToken } from '@/utils/auth';

export interface UserInfo {
  id?: string;
  username?: string;
  realName?: string;
  email?: string;
  phone?: string;
  avatar?: string;
  userType?: number;  // 1: 管理员, 0: 普通用户
  state?: number;
  createTime?: string;
  updateTime?: string;
}

interface UserState {
  user: UserInfo;
  isLoggedIn: boolean;
  loading: boolean;
  loadUserInfo: () => Promise<UserInfo | null>;
  setUser: (user: UserInfo) => void;
  clearUser: () => void;
}

const useUserStore = create<UserState>((set, get) => ({
  user: {},
  isLoggedIn: !!getToken(),
  loading: false,

  loadUserInfo: async () => {
    try {
      set({ loading: true });
      const userInfo = await getUserInfo();
      if (userInfo) {
        set({ user: userInfo, isLoggedIn: true });
        return userInfo;
      }
      return null;
    } catch (error) {
      console.error('Failed to load user info:', error);
      return null;
    } finally {
      set({ loading: false });
    }
  },

  setUser: (user) => {
    set({ user, isLoggedIn: true });
  },

  clearUser: () => {
    set({ user: {}, isLoggedIn: false });
  },
}));

export default useUserStore; 