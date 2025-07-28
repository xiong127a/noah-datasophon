<template>
  <div class="simple-test p-6 bg-white rounded-lg shadow">
    <h1 class="text-xl font-bold mb-2">集群管理测试组件</h1>
    <p class="mb-4 text-gray-600">当前路径: <span class="font-mono bg-gray-100 px-2 py-0.5 rounded">{{ $route.path }}</span></p>
    
    <!-- 路由信息卡片 -->
    <div class="mb-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
      <h2 class="font-semibold mb-2 text-blue-700">详细路由信息</h2>
      <div class="grid grid-cols-2 gap-4">
        <div>
          <p class="mb-1"><strong>名称:</strong> {{ $route.name || '(无名称)' }}</p>
          <p class="mb-1"><strong>完整路径:</strong> {{ $route.fullPath }}</p>
          <p class="mb-1"><strong>参数:</strong> {{ JSON.stringify($route.params) }}</p>
          <p class="mb-1"><strong>查询:</strong> {{ JSON.stringify($route.query) }}</p>
        </div>
        <div>
          <p class="mb-1"><strong>匹配项数量:</strong> {{ $route.matched.length }}</p>
          <p class="mb-1"><strong>匹配路径:</strong></p>
          <ul class="list-disc pl-5">
            <li v-for="(match, index) in $route.matched" :key="index">
              {{ match.path || '(根路径)' }}
            </li>
          </ul>
        </div>
      </div>
    </div>
    
    <!-- 路由导航按钮组 -->
    <div class="mb-6">
      <h2 class="font-semibold mb-3">路由导航测试</h2>
      <div class="flex flex-wrap gap-3">
        <button 
          v-for="link in links" 
          :key="link.path" 
          @click="$router.push(link.path)"
          class="px-3 py-2 bg-blue-100 hover:bg-blue-200 rounded transition-colors"
          :class="{ 'ring-2 ring-blue-500': $route.path === link.path }"
        >
          {{ link.name }}
        </button>
      </div>
    </div>
    
    <!-- 父组件信息 -->
    <div v-if="$route.matched.length > 1" class="mt-4 p-4 bg-green-50 border border-green-200 rounded-lg">
      <h2 class="font-semibold mb-2 text-green-700">父路由组件信息</h2>
      <p><strong>父路径:</strong> {{ $route.matched[$route.matched.length - 2]?.path || '(无)' }}</p>
      <p><strong>父名称:</strong> {{ $route.matched[$route.matched.length - 2]?.name || '(无名称)' }}</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'SimpleTest',
  data() {
    return {
      links: [
        { name: '集群管理', path: '/cluster' },
        { name: '集群管理列表', path: '/cluster/list' },
        { name: '存储库', path: '/cluster/storage' },
        { name: '集群框架', path: '/cluster/framework' },
        { name: '不存在路径', path: '/cluster/notexist' },
        { name: '返回主页', path: '/' },
      ]
    }
  },
  mounted() {
    console.log('[SimpleTest] 组件已加载');
    console.log('[SimpleTest] 当前路径:', this.$route.path);
    console.log('[SimpleTest] 路由匹配:', JSON.stringify(this.$route.matched.map(m => ({
      path: m.path,
      name: m.name
    }))));
    
    // 打印嵌套信息
    if (this.$route.matched.length > 1) {
      console.log('[SimpleTest] 是嵌套路由组件，父路径:', 
        this.$route.matched[this.$route.matched.length - 2].path);
    }
  }
}
</script>

<style scoped>
.simple-test {
  max-width: 900px;
  margin: 0 auto;
}
</style> 