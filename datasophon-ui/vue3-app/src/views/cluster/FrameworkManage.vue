<template>
  <div class="container mx-auto p-4">
    <div class="flex justify-between items-center mb-6">
      <h1 class="text-2xl font-bold">集群框架</h1>
      <button class="bg-blue-500 hover:bg-blue-600 text-white py-2 px-4 rounded-lg shadow flex items-center">
        <span class="mr-2">新增框架</span>
        <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
          <path fill-rule="evenodd" d="M10 5a1 1 0 011 1v3h3a1 1 0 110 2h-3v3a1 1 0 11-2 0v-3H6a1 1 0 110-2h3V6a1 1 0 011-1z" clip-rule="evenodd" />
        </svg>
      </button>
    </div>
    
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div v-for="(framework, index) in frameworks" :key="index" class="bg-white rounded-lg shadow overflow-hidden hover:shadow-md transition-shadow duration-300">
        <div class="p-6">
          <div class="flex items-center mb-4">
            <div class="flex-shrink-0 h-12 w-12 bg-blue-100 rounded-md flex items-center justify-center">
              <svg-icon :icon-class="framework.icon" class="text-blue-600 h-8 w-8" />
            </div>
            <div class="ml-4">
              <h2 class="text-lg font-semibold text-gray-900">{{ framework.name }}</h2>
              <p class="text-sm text-gray-500">版本 {{ framework.version }}</p>
            </div>
          </div>
          
          <div class="mb-4">
            <div class="flex justify-between text-sm mb-1">
              <span class="text-gray-500">组件数量</span>
              <span class="font-medium">{{ framework.componentCount }}</span>
            </div>
            <div class="flex justify-between text-sm mb-1">
              <span class="text-gray-500">依赖服务</span>
              <span class="font-medium">{{ framework.dependencies.join(', ') }}</span>
            </div>
            <div class="flex justify-between text-sm">
              <span class="text-gray-500">最后更新</span>
              <span class="font-medium">{{ framework.lastUpdated }}</span>
            </div>
          </div>
          
          <div class="mt-6 flex justify-between">
            <div>
              <span :class="getStatusClass(framework.status)" class="px-2 py-1 text-xs font-medium rounded-full">
                {{ framework.status }}
              </span>
            </div>
            <div class="flex space-x-2">
              <button class="text-gray-500 hover:text-gray-700">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M10 12a2 2 0 100-4 2 2 0 000 4z" />
                  <path fill-rule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clip-rule="evenodd" />
                </svg>
              </button>
              <button class="text-blue-500 hover:text-blue-700">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
                </svg>
              </button>
              <button class="text-red-500 hover:text-red-700">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd" />
                </svg>
              </button>
            </div>
          </div>
        </div>
        
        <div class="px-6 py-4 bg-gray-50 border-t border-gray-100">
          <div class="flex justify-between">
            <button class="text-blue-600 hover:text-blue-800 text-sm font-medium flex items-center">
              <span>查看组件</span>
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 ml-1" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd" />
              </svg>
            </button>
            <button class="text-green-600 hover:text-green-800 text-sm font-medium flex items-center">
              <span>安装</span>
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 ml-1" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm3.293-7.707a1 1 0 011.414 0L9 10.586V3a1 1 0 112 0v7.586l1.293-1.293a1 1 0 111.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z" clip-rule="evenodd" />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 空状态 -->
    <div v-if="frameworks.length === 0" class="flex flex-col items-center justify-center py-12 bg-white rounded-lg shadow">
      <svg xmlns="http://www.w3.org/2000/svg" class="h-16 w-16 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
      </svg>
      <h3 class="mt-4 text-lg font-medium text-gray-900">暂无框架</h3>
      <p class="mt-1 text-sm text-gray-500">点击上方"新增框架"按钮添加集群框架</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

// 模拟数据
const frameworks = ref([
  {
    name: 'Hadoop',
    version: '3.3.1',
    icon: 'hadoop',
    componentCount: 5,
    dependencies: ['ZooKeeper'],
    lastUpdated: '2023-08-15',
    status: '稳定版'
  },
  {
    name: 'Spark',
    version: '3.2.0',
    icon: 'spark',
    componentCount: 3,
    dependencies: ['Hadoop', 'YARN'],
    lastUpdated: '2023-07-20',
    status: '稳定版'
  },
  {
    name: 'Flink',
    version: '1.14.2',
    icon: 'flink',
    componentCount: 4,
    dependencies: ['YARN', 'ZooKeeper'],
    lastUpdated: '2023-06-30',
    status: '稳定版'
  },
  {
    name: 'Kafka',
    version: '2.8.1',
    icon: 'kafka',
    componentCount: 2,
    dependencies: ['ZooKeeper'],
    lastUpdated: '2023-05-25',
    status: '稳定版'
  },
  {
    name: 'HBase',
    version: '2.4.6',
    icon: 'hbase',
    componentCount: 3,
    dependencies: ['Hadoop', 'ZooKeeper'],
    lastUpdated: '2023-04-10',
    status: '稳定版'
  },
  {
    name: 'Kubernetes',
    version: '1.22.0',
    icon: 'kubernetes',
    componentCount: 6,
    dependencies: [],
    lastUpdated: '2023-09-05',
    status: '测试版'
  }
]);

// 获取状态样式类
const getStatusClass = (status: string) => {
  switch (status) {
    case '稳定版':
      return 'bg-green-100 text-green-800';
    case '测试版':
      return 'bg-yellow-100 text-yellow-800';
    case '开发版':
      return 'bg-red-100 text-red-800';
    default:
      return 'bg-gray-100 text-gray-800';
  }
};
</script>

<style scoped>
/* 组件样式可以在这里添加 */
</style> 