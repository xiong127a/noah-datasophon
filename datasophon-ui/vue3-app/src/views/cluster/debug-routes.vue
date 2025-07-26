<template>
  <div class="debug-routes-container">
    <h1 class="text-2xl font-bold mb-4">路由调试器</h1>
    
    <div class="mb-6 p-4 bg-yellow-50 border border-yellow-200 rounded">
      <h2 class="text-lg font-semibold mb-2">当前路由信息</h2>
      <div class="grid grid-cols-2 gap-2">
        <div class="font-medium">路径:</div>
        <div>{{ route.path }}</div>
        
        <div class="font-medium">完整路径:</div>
        <div>{{ route.fullPath }}</div>
        
        <div class="font-medium">名称:</div>
        <div>{{ route.name }}</div>
        
        <div class="font-medium">参数:</div>
        <div>{{ JSON.stringify(route.params) }}</div>
        
        <div class="font-medium">查询参数:</div>
        <div>{{ JSON.stringify(route.query) }}</div>
        
        <div class="font-medium">Meta:</div>
        <div>{{ JSON.stringify(route.meta) }}</div>
      </div>
    </div>
    
    <div class="mb-6">
      <h2 class="text-lg font-semibold mb-2">所有路由配置</h2>
      <div class="overflow-auto max-h-96 border rounded">
        <table class="min-w-full">
          <thead class="bg-gray-100">
            <tr>
              <th class="px-4 py-2 text-left">路径</th>
              <th class="px-4 py-2 text-left">名称</th>
              <th class="px-4 py-2 text-left">组件</th>
              <th class="px-4 py-2 text-left">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(route, index) in routes" :key="index" class="border-t">
              <td class="px-4 py-2">{{ route.path }}</td>
              <td class="px-4 py-2">{{ route.name || '-' }}</td>
              <td class="px-4 py-2">{{ getComponentName(route) }}</td>
              <td class="px-4 py-2">
                <button 
                  @click="navigateTo(route.path)"
                  class="px-3 py-1 bg-blue-500 text-white rounded hover:bg-blue-600"
                >
                  跳转
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    
    <div class="mb-6">
      <h2 class="text-lg font-semibold mb-2">路由测试</h2>
      <div class="flex gap-4 flex-wrap">
        <button 
          v-for="testPath in testPaths" 
          :key="testPath"
          @click="navigateTo(testPath)"
          class="px-3 py-2 bg-green-500 text-white rounded hover:bg-green-600"
        >
          {{ testPath }}
        </button>
      </div>
    </div>
    
    <div class="mt-6">
      <h2 class="text-lg font-semibold mb-2">手动导航</h2>
      <div class="flex items-center gap-2">
        <input 
          v-model="customPath"
          placeholder="输入路径，如 /colony-manage/storage"
          class="flex-1 px-3 py-2 border rounded"
        />
        <button 
          @click="navigateTo(customPath)"
          class="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
        >
          导航
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';

const router = useRouter();
const route = useRoute();
const customPath = ref('');

// 获取所有路由配置
const routes = computed(() => {
  return flattenRoutes(router.options.routes);
});

// 测试路径列表
const testPaths = [
  '/',
  '/home',
  '/colony-manage',
  '/colony-manage/list',
  '/colony-manage/storage',
  '/colony-manage/framework',
  '/cluster/list',
  '/cluster/storage',
  '/cluster/framework'
];

// 展平嵌套路由
function flattenRoutes(routes, parentPath = '') {
  let flatRoutes = [];
  
  routes.forEach(route => {
    // 计算完整路径
    const path = route.path.startsWith('/') 
      ? route.path 
      : `${parentPath}/${route.path}`.replace(/\/\//g, '/');
    
    // 添加当前路由
    flatRoutes.push({
      path,
      name: route.name,
      component: route.component,
      meta: route.meta || {},
      redirect: route.redirect
    });
    
    // 处理子路由
    if (route.children && route.children.length > 0) {
      const childrenRoutes = flattenRoutes(route.children, path);
      flatRoutes = flatRoutes.concat(childrenRoutes);
    }
  });
  
  return flatRoutes;
}

// 获取组件名称
function getComponentName(route) {
  if (!route.component) {
    return route.redirect ? `重定向到: ${route.redirect}` : 'No Component';
  }
  
  const componentStr = route.component.toString();
  
  // 尝试从动态导入中提取文件名
  const importMatch = componentStr.match(/import\(['"](.+?)['"]\)/);
  if (importMatch && importMatch[1]) {
    return importMatch[1].split('/').pop();
  }
  
  // 回退到显示组件函数
  return componentStr.substring(0, 50) + '...';
}

// 导航到指定路径
function navigateTo(path) {
  if (!path) return;
  
  console.log(`[Debug] 尝试导航到: ${path}`);
  router.push(path).catch(err => {
    console.error(`[Debug] 导航错误: ${err.message}`);
    alert(`导航失败: ${err.message}`);
  });
}

onMounted(() => {
  console.log('[Debug] 路由调试器已加载');
  console.log('[Debug] 当前路由:', route);
  console.log('[Debug] 所有路由配置:', routes.value);
});
</script>

<style scoped>
.debug-routes-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}
</style> 