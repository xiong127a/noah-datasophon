import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface SettingState {
  clusterId: string;
  menuData: any[];
  isCluster: boolean;
  setClusterId: (id: string) => void;
  setMenuData: (data: any[]) => void;
  setIsCluster: (isCluster: boolean) => void;
}

const useSettingStore = create<SettingState>()(
  persist(
    (set) => ({
      clusterId: '',
      menuData: [],
      isCluster: false,
      
      setClusterId: (id) => set({ clusterId: id }),
      setMenuData: (data) => set({ menuData: data }),
      setIsCluster: (isCluster) => set({ isCluster }),
    }),
    {
      name: 'datasophon-settings', // 本地存储的key名称
    }
  )
);

export default useSettingStore; 