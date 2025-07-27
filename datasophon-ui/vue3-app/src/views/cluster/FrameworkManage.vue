<script setup>
import { ref, computed, onMounted } from 'vue';
import { Dialog, DialogPanel } from '@headlessui/vue';
import { useVueSonner } from '@/composables/useVueSonner';
import { useErrorHandler } from '@/composables/useErrorHandler';
import { getFrameList, deleteService } from '@/api/httpApi/cluster';
import { useRoute } from 'vue-router';

// 添加路由参数处理
const route = useRoute();

// 添加组件加载时的调试日志
onMounted(() => {
  // 记录路由信息，便于调试
  console.log('[FrameworkManage] 组件已加载, 路径:', route.path);
  console.log('[FrameworkManage] 完整URL:', window.location.href);
  console.log('[FrameworkManage] 查询参数:', route.query);
});
</script>

<template>
  <div class="container mx-auto p-4">
    <div class="flex justify-between items-center mb-6">
      <div>
        <h1 class="text-2xl font-bold">集群框架</h1>
        <p class="text-gray-500">查看集群中可安装的服务框架和组件</p>
      </div>
    </div>
    
    <!-- 加载中状态 -->
    <div v-if="loading" class="flex justify-center items-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500"></div>
    </div>
    
    <!-- 框架内容 -->
    <div v-else-if="frameworkList.length > 0" class="bg-white rounded-lg shadow overflow-hidden">
      <!-- 框架Tab切换 -->
      <div class="border-b border-gray-200">
        <div class="flex overflow-x-auto bg-gray-50">
          <button 
            v-for="framework in frameworkList" 
            :key="framework.frameCode"
            class="px-6 py-4 text-sm font-medium whitespace-nowrap relative"
            :class="activeFramework === framework.frameCode ? 'text-blue-600 border-blue-600' : 'text-gray-500 hover:text-gray-700 border-transparent'"
            @click="activeFramework = framework.frameCode"
          >
            {{ framework.frameCode }}
            <div v-if="activeFramework === framework.frameCode" class="absolute bottom-0 left-0 right-0 h-0.5 bg-blue-600"></div>
          </button>
        </div>
      </div>
      
      <!-- 服务卡片内容区域 -->
      <div class="p-6">
        <div v-if="currentServices.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <!-- 服务卡片 -->
          <div 
            v-for="service in currentServices" 
            :key="service.id" 
            class="bg-white rounded-lg border border-gray-200 shadow-sm hover:shadow-md transition-shadow duration-300"
          >
            <div class="p-5">
              <!-- 服务头部信息 -->
              <div class="flex items-center mb-4">
                <div class="flex-shrink-0 h-12 w-12 bg-blue-100 rounded-md flex items-center justify-center">
                  <svg-icon :icon-class="getServiceIconClass(service.serviceName)" class="text-blue-600 h-8 w-8" />
                </div>
                <div class="ml-4">
                  <h2 class="text-lg font-semibold text-gray-900">{{ service.serviceName }}</h2>
                  <div class="text-sm text-gray-500 bg-gray-100 rounded-full px-3 py-1 inline-block">{{ service.serviceVersion }}</div>
                </div>
              </div>
              
              <!-- 服务描述 -->
              <div class="mb-4 text-gray-600 text-sm h-12 overflow-hidden">
                {{ service.serviceDesc || '暂无描述' }}
              </div>
              
              <!-- 服务底部操作区 -->
              <div class="flex justify-end mt-4 pt-4 border-t border-gray-100">
                <button 
                  class="flex items-center text-red-500 hover:text-red-700 text-sm font-medium"
                  @click="confirmDelete(service)"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                  删除服务
                </button>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 空状态 -->
        <div v-else class="flex flex-col items-center justify-center py-12">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-16 w-16 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <h3 class="mt-4 text-lg font-medium text-gray-900">该框架下暂无服务组件</h3>
          <p class="mt-1 text-sm text-gray-500">请先添加服务组件</p>
        </div>
      </div>
    </div>
    
    <!-- 空状态 -->
    <div v-else class="flex flex-col items-center justify-center py-12 bg-white rounded-lg shadow">
      <svg xmlns="http://www.w3.org/2000/svg" class="h-16 w-16 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
      </svg>
      <h3 class="mt-4 text-lg font-medium text-gray-900">暂无集群框架</h3>
      <p class="mt-1 text-sm text-gray-500">请先添加集群框架</p>
    </div>
    
    <!-- 删除确认对话框 -->
    <Dialog as="div" :open="showDeleteConfirmDialog" @close="showDeleteConfirmDialog = false" class="relative z-50">
      <div class="fixed inset-0 bg-black/30" aria-hidden="true" />
      
      <div class="fixed inset-0 flex items-center justify-center p-4">
        <DialogPanel class="mx-auto max-w-md rounded-xl bg-white p-6 shadow-xl">
          <DialogTitle class="text-lg font-medium text-gray-900">确认删除</DialogTitle>
          <div class="mt-3">
            <p class="text-sm text-gray-500">
              是否确认删除 <span class="font-medium">{{ serviceToDelete?.serviceName }}</span> 服务？
            </p>
          </div>
          
          <div class="mt-5 flex justify-end space-x-3">
            <button
              type="button"
              class="inline-flex justify-center rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50"
              @click="showDeleteConfirmDialog = false"
            >
              取消
            </button>
            <button
              type="button"
              class="inline-flex justify-center rounded-md border border-transparent bg-red-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-red-700"
              @click="handleDelete"
            >
              确认删除
            </button>
          </div>
        </DialogPanel>
      </div>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { Dialog, DialogPanel, DialogTitle } from '@headlessui/vue';
import { getFrameList, deleteService } from '@/api/httpApi/cluster';
import { useVueSonner } from '@/composables/useVueSonner';
import { useErrorHandler } from '@/composables/useErrorHandler';

// 状态管理
const loading = ref(false);
const frameworkList = ref([]);
const activeFramework = ref('');
const showDeleteConfirmDialog = ref(false);
const serviceToDelete = ref(null);

// 工具实例
const { toast } = useVueSonner();
const errorHandler = useErrorHandler();

// 计算属性：获取当前选中框架的服务列表
const currentServices = computed(() => {
  if (!activeFramework.value) return [];
  const currentFramework = frameworkList.value.find(frame => frame.frameCode === activeFramework.value);
  return currentFramework ? currentFramework.frameServiceList || [] : [];
});

// 获取服务对应的图标类名
const getServiceIconClass = (serviceName) => {
  // 将服务名称转为小写
  const iconName = serviceName.toLowerCase();
  
  // 检查是否为已知服务
  const knownServices = [
    'hdfs', 'yarn', 'hbase', 'hive', 'spark', 'flink', 'kafka', 'zookeeper',
    'hadoop', 'hue', 'kylin', 'livy', 'phoenix', 'presto', 'ranger', 
    'solr', 'sqoop', 'tez', 'trino', 'elasticsearch', 'kibana', 'alluxio',
    'atlas', 'airflow', 'flume', 'oozie', 'sentry'
  ];
  
  // 如果是已知服务，返回对应的图标名
  if (knownServices.includes(iconName)) {
    return iconName;
  }
  
  // 对于未知服务，返回默认图标
  return 'service-default';
};

// 加载框架列表
const loadFrameworkList = async () => {
  loading.value = true;
  try {
    const res = await getFrameList();
    if (res.code === 200) {
      frameworkList.value = res.data || [];
      
      // 自动选择第一个框架
      if (frameworkList.value.length > 0 && !activeFramework.value) {
        activeFramework.value = frameworkList.value[0].frameCode;
      }
    } else {
      toast.error(res.msg || '获取框架列表失败');
    }
  } catch (error) {
    errorHandler.handleError(error, '获取框架列表失败');
  } finally {
    loading.value = false;
  }
};

// 确认删除
const confirmDelete = (service) => {
  showDeleteConfirmDialog.value = true;
  serviceToDelete.value = service;
};

// 删除服务
const handleDelete = async () => {
  if (!serviceToDelete.value) {
    showDeleteConfirmDialog.value = false;
    return;
  }
  
  try {
    const res = await deleteService(serviceToDelete.value.id);
    if (res.code === 200) {
      toast.success('删除服务成功');
      await loadFrameworkList(); // 重新加载数据
    } else {
      toast.error(res.msg || '删除服务失败');
    }
  } catch (error) {
    errorHandler.handleError(error, '删除服务失败');
  } finally {
    showDeleteConfirmDialog.value = false;
    serviceToDelete.value = null;
  }
};

// 组件挂载时加载数据
onMounted(() => {
  loadFrameworkList();
});
</script>

<style scoped>
/* 组件样式可以在这里添加 */
</style> 