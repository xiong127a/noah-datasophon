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
          <div v-if="showDebugPanel" class="mb-4 p-4 bg-blue-50 rounded-lg border border-blue-200">
            <div class="flex justify-between mb-2">
              <h3 class="font-bold">路由调试</h3>
              <button @click="showDebugPanel = false" class="text-xs bg-gray-300 px-2 py-1 rounded">关闭</button>
            </div>
            <p><strong>路径:</strong> {{ route.path }}</p>
            <p><strong>匹配项:</strong> {{ route.matched.map(m => m.path).join(' > ') }}</p>
          </div>
          
          <button 
            v-if="!showDebugPanel" 
            @click="showDebugPanel = true"
            class="mb-4 bg-blue-100 px-3 py-1 text-xs rounded"
          >
            显示路由信息
          </button>
          
          <suspense>
            <template #default>
              <router-view v-slot="{ Component }">
                <component :is="Component" />
              </router-view>
            </template>
            <template #fallback>
              <div class="w-full h-64 flex items-center justify-center">
                <div class="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500"></div>
                <p class="ml-4 text-gray-600">正在加载...</p>
              </div>
            </template>
          </suspense>
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

// 调试控制
const showDebugPanel = ref(false);

// 菜单项类型定义
interface MenuItem {
  path: string;
  title: string;
  icon?: string;
  rightSide?: boolean;
  children?: MenuItem[];
}

// 路由和设置store
const router = useRouter();
const route = useRoute();
const settingsStore = useSettingsStore();

// 菜单数据
const menuData = ref<MenuItem[]>([
  { path: '/', title: '首页', icon: 'home' },
  { path: '/host', title: '主机管理', icon: 'host' },
  { 
    path: '/alarm', 
    title: '告警管理', 
    icon: 'alarm',
    children: [
      { path: '/alarm/notification', title: '通知组管理', icon: 'notice' },
      { path: '/alarm/group', title: '告警组管理', icon: 'group' },
      { path: '/alarm/metric', title: '告警指标管理', icon: 'metric' },
      { path: '/alarm/help', title: '使用帮助', icon: 'help' }
    ]
  },
  { 
    path: '/system', 
    title: '系统管理', 
    icon: 'system',
    children: [
      { path: '/system/tenant', title: '租户管理', icon: 'tenant' },
      { path: '/system/user', title: '用户管理', icon: 'user' },
      { path: '/system/rack', title: '机架管理', icon: 'rack' },
      { path: '/system/label', title: '标签管理', icon: 'label' },
      { path: '/system/log', title: '日志审计', icon: 'log' }
    ]
  },
  { 
    path: '/cluster', 
    title: '集群管理', 
    icon: 'colony',
    rightSide: true,
    children: [
      { 
        path: '/cluster', 
        title: '集群管理', 
        icon: 'cluster' 
      },
      { 
        path: '/cluster/storage',
        title: '存储库管理', 
        icon: 'storage'
      },
      { 
        path: '/cluster/framework',
        title: '集群框架', 
        icon: 'framework'
      }
    ]
  },
  {
    path: '/user',
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
  
  // 对于集群管理路径做特殊处理 - 跳转到第一个子菜单
  if (key === '/cluster' && menuData.value) {
    const clusterMenu = menuData.value.find(item => item.path === '/cluster');
    if (clusterMenu && clusterMenu.children && clusterMenu.children.length > 0) {
      // 使用第一个子菜单路径
      const targetPath = clusterMenu.children[0].path;
      console.log('正在导航到集群管理子路径:', targetPath);
      router.push(targetPath).catch(err => {
        if (err.name !== 'NavigationDuplicated') {
          console.error('[路由错误]', err);
        }
      });
      return;
    }
  }
  
  // 跳转到菜单路径
  if (route.path !== key) {
    console.log(`[导航] 跳转到菜单路径: ${key}`);
    router.push(key).catch(err => {
      if (err.name !== 'NavigationDuplicated') {
        console.error('[路由错误]', err);
      }
    });
  }
};

// 路由变更
const onRouteChanged = (newPath) => {
  console.log('[路由变更] 准备导航到:', newPath);
  
  // 正常路由导航
  router.push(newPath).catch(err => {
    if (err.name === 'NavigationDuplicated') {
      // 重复导航不处理
      return;
    }
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
         (newPath.startsWith('/cluster')))) {
      console.log(`[菜单状态] 激活菜单: ${item.path} (${item.title})`);
      activeFirstMenuKey.value = item.path;
      settingsStore.setActiveFirstMenu(item.path);
      break;
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