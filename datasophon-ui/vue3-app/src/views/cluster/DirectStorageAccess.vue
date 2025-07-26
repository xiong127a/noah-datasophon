<template>
  <div class="storage-debug-page">
    <h1 class="text-2xl font-bold mb-4">存储库管理（备用页面）</h1>
    
    <div class="bg-blue-50 border border-blue-200 rounded p-4 mb-6">
      <p class="text-blue-700">这是一个备用页面，用于直接访问存储库管理。</p>
      <p class="text-blue-700 mt-2">当常规路由失败时会显示此页面。</p>
    </div>
    
    <div class="bg-white rounded-lg shadow p-6 mb-6">
      <h2 class="text-xl font-semibold mb-4">路由诊断</h2>
      
      <div class="space-y-2">
        <div class="flex">
          <span class="font-medium w-32">当前路径:</span>
          <span>{{ route.path }}</span>
        </div>
        <div class="flex">
          <span class="font-medium w-32">完整路径:</span>
          <span>{{ route.fullPath }}</span>
        </div>
        <div class="flex">
          <span class="font-medium w-32">命名路由:</span>
          <span>{{ route.name || '(无名称)' }}</span>
        </div>
      </div>
      
      <div class="mt-4">
        <button 
          @click="tryRedirect" 
          class="bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded"
        >
          尝试重定向到常规页面
        </button>
      </div>
    </div>
    
    <div class="mb-6">
      <h2 class="text-xl font-semibold mb-4">可用路由列表</h2>
      <div class="space-y-2">
        <button
          v-for="(path, index) in availablePaths"
          :key="index"
          @click="navigateTo(path)"
          class="block w-full text-left px-4 py-2 bg-gray-100 hover:bg-gray-200 rounded"
        >
          {{ path }}
        </button>
      </div>
    </div>
    
    <!-- 内嵌消息 -->
    <div class="bg-white rounded-lg shadow overflow-hidden">
      <div class="border-b border-gray-200 p-4 bg-gray-50">
        <h2 class="text-xl font-semibold">错误信息</h2>
      </div>
      <div class="p-4">
        <div class="text-red-500">
          无法加载存储库管理组件。请尝试通过以下链接之一访问：
        </div>
        <div class="mt-4 space-y-2">
          <a 
            href="/colony-manage/storage" 
            class="block px-4 py-2 bg-blue-100 hover:bg-blue-200 rounded text-blue-700"
          >
            /colony-manage/storage
          </a>
          <a 
            href="/cluster/storage" 
            class="block px-4 py-2 bg-blue-100 hover:bg-blue-200 rounded text-blue-700"
          >
            /cluster/storage
          </a>
          <a 
            href="/storage-direct" 
            class="block px-4 py-2 bg-blue-100 hover:bg-blue-200 rounded text-blue-700"
          >
            /storage-direct
          </a>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';

export default {
  name: 'DirectStorageAccess',
  setup() {
    const router = useRouter();
    const route = useRoute();

    // 可用的替代路径
    const availablePaths = ref([
      '/colony-manage/storage',
      '/colony-manage/framework',
      '/cluster/storage',
      '/cluster/framework',
      '/colony-manage/list',
      '/debug-routes',
      '/storage-direct'
    ]);

    // 尝试重定向
    const tryRedirect = () => {
      console.log('[StorageDebug] 尝试重定向到常规存储库页面');
      
      // 尝试多个可能的路径
      router.push('/colony-manage/storage').catch(err => {
        console.error('[StorageDebug] 重定向到 /colony-manage/storage 失败:', err);
        
        router.push('/cluster/storage').catch(err2 => {
          console.error('[StorageDebug] 重定向到 /cluster/storage 也失败:', err2);
          alert('无法导航到常规存储库页面，请查看控制台获取详细错误信息');
        });
      });
    };

    // 导航到指定路径
    const navigateTo = (path) => {
      router.push(path).catch(err => {
        console.error(`[StorageDebug] 导航到 ${path} 失败:`, err);
        alert(`无法导航到 ${path}，详情请查看控制台`);
      });
    };

    // 记录访问信息
    onMounted(() => {
      console.log('[StorageDebug] 直接访问存储库页面组件已加载');
      console.log('[StorageDebug] 当前路由信息:', route);
    });

    return {
      route,
      availablePaths,
      tryRedirect,
      navigateTo
    };
  }
};
</script>

<style scoped>
.storage-debug-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}
</style> 