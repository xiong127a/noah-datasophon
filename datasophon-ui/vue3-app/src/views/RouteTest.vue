<template>
  <div class="route-test-container p-6">
    <div class="bg-white rounded-lg shadow-lg p-6">
      <h1 class="text-2xl font-bold mb-6">路由测试页面</h1>
      
      <!-- 当前路由信息 -->
      <div class="mb-8 p-4 bg-blue-50 rounded-lg">
        <h2 class="text-lg font-semibold mb-3">当前路由信息</h2>
        <div class="grid grid-cols-2 gap-4 text-sm">
          <div><strong>路径:</strong> {{ $route.path }}</div>
          <div><strong>名称:</strong> {{ $route.name }}</div>
          <div><strong>完整路径:</strong> {{ $route.fullPath }}</div>
          <div><strong>查询参数:</strong> {{ JSON.stringify($route.query) }}</div>
        </div>
      </div>

      <!-- 路由测试按钮 -->
      <div class="mb-8">
        <h2 class="text-lg font-semibold mb-4">路由导航测试</h2>
        <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
          <button 
            v-for="route in testRoutes" 
            :key="route.path"
            @click="navigateTo(route.path)"
            class="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 transition-colors text-sm"
          >
            {{ route.name }}
          </button>
        </div>
      </div>

      <!-- 重定向测试 -->
      <div class="mb-8">
        <h2 class="text-lg font-semibold mb-4">重定向测试</h2>
        <div class="grid grid-cols-2 md:grid-cols-3 gap-3">
          <button 
            v-for="redirect in redirectTests" 
            :key="redirect.from"
            @click="navigateTo(redirect.from)"
            class="px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600 transition-colors text-sm"
          >
            {{ redirect.from }} → {{ redirect.to }}
          </button>
        </div>
      </div>

      <!-- 所有可用路由 -->
      <div>
        <h2 class="text-lg font-semibold mb-4">所有可用路由</h2>
        <div class="overflow-x-auto">
          <table class="min-w-full border border-gray-200">
            <thead class="bg-gray-50">
              <tr>
                <th class="px-4 py-2 text-left border-b">路径</th>
                <th class="px-4 py-2 text-left border-b">名称</th>
                <th class="px-4 py-2 text-left border-b">标题</th>
                <th class="px-4 py-2 text-left border-b">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="route in allRoutes" :key="route.path" class="border-b">
                <td class="px-4 py-2 font-mono text-sm">{{ route.path }}</td>
                <td class="px-4 py-2">{{ route.name || '-' }}</td>
                <td class="px-4 py-2">{{ route.meta?.title || '-' }}</td>
                <td class="px-4 py-2">
                  <button 
                    @click="navigateTo(route.path)"
                    class="px-3 py-1 bg-blue-500 text-white rounded text-xs hover:bg-blue-600"
                  >
                    访问
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { computed } from 'vue'

const router = useRouter()

// 测试路由列表
const testRoutes = [
  { path: '/', name: '首页' },
  { path: '/cluster', name: '集群列表' },
  { path: '/cluster/storage', name: '存储管理' },
  { path: '/cluster/framework', name: '框架管理' },
  { path: '/service', name: '服务管理' },
  { path: '/host', name: '主机管理' },
  { path: '/alarm', name: '告警管理' },
  { path: '/system', name: '系统管理' },
  { path: '/system/log', name: '日志管理' },
  { path: '/user', name: '用户管理' }
]

// 重定向测试
const redirectTests = [
  { from: '/colony-manage/framework', to: '/cluster/framework' },
  { from: '/colony-manage/storage', to: '/cluster/storage' },
  { from: '/colony-manage/list', to: '/cluster' },
  { from: '/colony-manage', to: '/cluster' },
  { from: '/service-manage', to: '/service' },
  { from: '/host-manage', to: '/host' },
  { from: '/system-manage', to: '/system' },
  { from: '/user-manage', to: '/user' }
]

// 获取所有路由
const allRoutes = computed(() => {
  const routes = []
  
  function extractRoutes(routeList, parentPath = '') {
    routeList.forEach(route => {
      const fullPath = parentPath + route.path
      if (route.component && !route.redirect) {
        routes.push({
          path: fullPath,
          name: route.name,
          meta: route.meta
        })
      }
      if (route.children) {
        extractRoutes(route.children, fullPath + '/')
      }
    })
  }
  
  extractRoutes(router.getRoutes())
  return routes.filter(route => !route.path.includes('*'))
})

// 导航函数
const navigateTo = (path) => {
  console.log(`[RouteTest] 导航到: ${path}`)
  router.push(path).catch(err => {
    console.error(`[RouteTest] 导航失败:`, err)
  })
}
</script>

<style scoped>
.route-test-container {
  min-height: 100vh;
  background-color: #f5f5f5;
}
</style>