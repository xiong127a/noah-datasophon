<template>
  <div class="p-8 bg-white rounded-lg shadow">
    <h1 class="text-2xl font-bold mb-4">集群管理测试组件</h1>
    <p class="text-gray-600 mb-4">这是一个替代集群管理视图的测试组件</p>
    <div class="bg-blue-50 p-4 rounded-lg">
      <p class="mb-2">当前路径: {{ $route.path }}</p>
      <p class="mb-2">路由名称: {{ $route.name }}</p>
    </div>
    
    <div class="mt-8">
      <h2 class="text-xl font-semibold mb-4">路由调试</h2>
      <div class="grid grid-cols-2 gap-4">
        <div 
          v-for="link in links" 
          :key="link.path"
          class="p-4 bg-gray-50 rounded-lg border border-gray-200 hover:border-blue-500 cursor-pointer"
          @click="goToRoute(link.path)"
        >
          <h3 class="font-medium text-blue-600">{{ link.name }}</h3>
          <p class="text-sm text-gray-500">{{ link.path }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ClusterTest',
  data() {
    return {
      links: [
        { name: '集群管理', path: '/cluster' },
        { name: '存储库管理', path: '/cluster/storage' },
        { name: '集群框架', path: '/cluster/framework' },
        { name: '内联测试', path: '/test-inline' }
      ]
    }
  },
  methods: {
    goToRoute(path) {
      console.log(`[ClusterTest] 导航到: ${path}`);
      this.$router.push(path).catch(err => {
        console.error(`[ClusterTest] 导航错误:`, err);
      });
    }
  },
  mounted() {
    console.log('[ClusterTest] 组件已加载');
    console.log('[ClusterTest] 当前路径:', this.$route.path);
  }
}
</script> 