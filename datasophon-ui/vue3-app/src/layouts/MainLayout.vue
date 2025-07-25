<template>
  <div class="h-screen bg-background flex flex-col">
    <!-- 顶部菜单 -->
    <AppHeader 
      :firstMenu="menuData"
      :activeFirstMenuKey="activeFirstMenuKey"
      @firstMenuSelect="onFirstMenuSelect"
      @routeChanged="onRouteChanged"
    />
    
    <div class="flex flex-1 overflow-hidden">
      <!-- 主区域 -->
      <div class="flex-1 overflow-hidden flex flex-col">
        <!-- 内容区域 -->
        <main class="flex-1 overflow-auto p-6 bg-background">
          <router-view></router-view>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useSettingsStore } from '../stores/settings';
import AppHeader from '../components/header/AppHeader.vue';

// 路由和设置store
const router = useRouter();
const route = useRoute();
const settingsStore = useSettingsStore();

// 菜单数据
const menuData = ref([
  { path: '/', title: '首页', icon: 'home' },
  { path: '/host', title: '主机管理', icon: 'host' },
  { 
    path: '/alarm-manage', 
    title: '告警管理', 
    icon: 'alarm',
    children: [
      { path: '/alarm-manage/notification', title: '通知组管理', icon: 'notice' },
      { path: '/alarm-manage/group', title: '告警组管理', icon: 'group' },
      { path: '/alarm-manage/metric', title: '告警指标管理', icon: 'metric' },
      { path: '/alarm-manage/help', title: '使用帮助', icon: 'help' }
    ]
  },
  { 
    path: '/system-manage', 
    title: '系统管理', 
    icon: 'system',
    children: [
      { path: '/system-manage/tenant', title: '租户管理', icon: 'tenant' },
      { path: '/system-manage/user', title: '用户管理', icon: 'user' },
      { path: '/system-manage/rack', title: '机架管理', icon: 'rack' },
      { path: '/system-manage/label', title: '标签管理', icon: 'label' },
      { path: '/system-manage/log', title: '日志审计', icon: 'log' }
    ]
  },
  { 
    path: '/colony-manage', 
    title: '集群管理', 
    icon: 'colony',
    rightSide: true,
    children: [
      { path: '/colony-manage/list', title: '集群管理', icon: 'cluster' },
      { path: '/colony-manage/storage', title: '存储库管理', icon: 'storage' },
      { path: '/colony-manage/framework', title: '集群框架', icon: 'framework' }
    ]
  },
  {
    path: '/user-manage',
    title: '用户管理',
    icon: 'user_manager',
    rightSide: true
  }
]);

// 激活的菜单
const activeFirstMenuKey = ref('');

// 一级菜单选择
const onFirstMenuSelect = (key: string) => {
  activeFirstMenuKey.value = key;
  settingsStore.setActiveFirstMenu(key);
  
  // 对于service-manage（主页）路径做特殊处理
  if (key === '/service-manage') {
    // 直接跳转到主页而不是子菜单
    if (route.path !== '/service-manage') {
      router.push('/service-manage').catch(err => {
        if (err.name !== 'NavigationDuplicated') {
          throw err;
        }
      });
    }
    return;
  }
  
  // 特殊处理集群管理路径 - 始终导航到子路径
  if (key === '/colony-manage') {
    const targetPath = '/colony-manage/list';
    console.log('正在导航到集群管理子路径:', targetPath);
    router.push(targetPath).catch(err => {
      if (err.name !== 'NavigationDuplicated') {
        throw err;
      }
    });
    return;
  }

  // 跳转到菜单路径
  if (route.path !== key) {
    router.push(key).catch(err => {
      if (err.name !== 'NavigationDuplicated') {
        throw err;
      }
    });
  }
};

// 路由变更事件
const onRouteChanged = (path: string) => {
  console.log('路由已变更到:', path);
};

// 监听路由变化以更新菜单状态
watch(() => route.path, (newPath, oldPath) => {
  console.log(`[路由变更] 从 ${oldPath} 到 ${newPath}`);
  console.log('[路由状态] 当前路由:', route);
  
  // 更新激活的菜单
  for (const item of menuData.value) {
    if (newPath === item.path || newPath.startsWith(item.path + '/')) {
      console.log(`[菜单状态] 激活菜单: ${item.path} (${item.title})`);
      activeFirstMenuKey.value = item.path;
      settingsStore.setActiveFirstMenu(item.path);
      break;
    }
  }
  
  // 特殊处理集群管理路径
  if (newPath === '/colony-manage') {
    console.log('[路由修正] 检测到集群管理路径，自动修正到子路径');
    router.replace('/colony-manage/list').catch(err => {
      console.error('[路由错误]', err);
    });
  }
}, { immediate: true });

// 设置初始激活菜单
onMounted(() => {
  // 根据当前路由路径确定激活的菜单
  const path = route.path;
  
  // 查找匹配的一级菜单
  for (const item of menuData.value) {
    if (path === item.path || path.startsWith(item.path + '/')) {
      activeFirstMenuKey.value = item.path;
      settingsStore.setActiveFirstMenu(item.path);
      break;
    }
  }
});
</script>

<style scoped>
.router-link-active.active {
  @apply bg-blue-50 text-primary;
}
</style> 