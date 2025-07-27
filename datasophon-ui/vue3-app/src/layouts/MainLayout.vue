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
import { ref, onMounted, watch, nextTick } from 'vue';
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
      { 
        path: '/colony-manage/list', 
        title: '集群管理', 
        icon: 'cluster' 
      },
      { 
        path: '/colony-manage/storage',  // 直接使用原始路径
        title: '存储库管理', 
        icon: 'storage'
      },
      { 
        path: '/colony-manage/framework',  // 直接使用原始路径
        title: '集群框架', 
        icon: 'framework'
      }
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

// 路由变更
const onRouteChanged = (newPath) => {
  console.log('路由已变更到:', newPath);
  
  // 特殊处理集群管理子菜单的直接访问
  if (newPath === '/colony-manage/storage' || newPath === '/colony-manage/framework') {
    console.log(`[MainLayout] 检测到直接访问关键路径: ${newPath}，使用绝对路径导航`);
    router.push(newPath).catch(err => {
      console.error('[路由错误]', err);
    });
    return;
  }
  
  // 正常路由导航
  router.push(newPath).catch(err => {
    console.error('[路由错误]', err);
  });
};

// 监听路由变化以更新菜单状态
watch(() => route.path, (newPath, oldPath) => {
  // 更新激活的菜单
  for (const item of menuData.value) {
    if (newPath === item.path || 
        newPath.startsWith(item.path + '/') ||
        (item.path === '/colony-manage' && 
         (newPath === '/colony-manage/storage' || newPath === '/colony-manage/framework'))) {
      console.log(`[菜单状态] 激活菜单: ${item.path} (${item.title})`);
      activeFirstMenuKey.value = item.path;
      settingsStore.setActiveFirstMenu(item.path);
      
      // 特殊处理存储库和框架路由 - 确保其在集群管理菜单下高亮显示
      if (newPath === '/colony-manage/storage' || newPath === '/colony-manage/framework') {
        console.log(`[菜单状态] 特殊路径激活集群管理菜单: ${item.path}`);
        
        // 确保子菜单能够正确展开
        nextTick(() => {
          // 这里可以添加展开子菜单的代码
          console.log('[菜单状态] 确保集群管理菜单展开');
        });
      }
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
  
  // 特殊处理存储库和框架路由
  if (newPath === '/colony-manage/storage' || newPath === '/colony-manage/framework') {
    console.log(`[路由修正] 检测到特殊路径: ${newPath}，确保正确加载`);
    
    // 如果这是直接加载此路径，确保菜单状态正确
    setTimeout(() => {
      // 确保集群管理菜单被激活
      activeFirstMenuKey.value = '/colony-manage';
      settingsStore.setActiveFirstMenu('/colony-manage');
      
      // 强制刷新一次
      nextTick(() => {
        console.log('[布局更新] 强制更新菜单状态');
      });
    }, 100);
    
    // 设置本地存储，以便在其他组件或刷新后能够识别
    try {
      localStorage.setItem('lastSpecialPath', newPath);
      localStorage.setItem('lastSpecialPathTimestamp', Date.now().toString());
    } catch (e) {
      console.error('[MainLayout] 设置本地存储失败:', e);
    }
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