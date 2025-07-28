<template>
  <div class="service-list-container p-6">
    <!-- 页面头部 -->
    <div class="bg-white rounded-xl shadow-card backdrop-blur-md p-8 mb-8">
      <div>
        <h1 class="text-2xl font-semibold text-gray-900 mb-2">服务管理</h1>
        <p class="text-gray-600">管理和监控集群中的各种服务组件</p>
      </div>
    </div>

    <!-- 服务列表 -->
    <div class="bg-white rounded-xl shadow-card p-6">
      <div class="flex justify-between items-center mb-6">
        <h2 class="text-xl font-semibold text-gray-800">服务列表</h2>
        <button class="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors">
          添加服务
        </button>
      </div>

      <!-- 服务卡片网格 -->
      <div v-if="serviceList.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div 
          v-for="service in serviceList" 
          :key="service.id"
          class="bg-gray-50 rounded-lg p-6 border border-gray-200 hover:shadow-md transition-shadow"
        >
          <div class="flex items-center mb-4">
            <div class="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center mr-4">
              <svg class="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div>
              <h3 class="text-lg font-semibold text-gray-900">{{ service.name }}</h3>
              <p class="text-sm text-gray-500">{{ service.version }}</p>
            </div>
          </div>
          
          <div class="mb-4">
            <div class="flex items-center mb-2">
              <span class="text-sm text-gray-600 mr-2">状态:</span>
              <span 
                class="px-2 py-1 rounded-full text-xs font-medium"
                :class="{
                  'bg-green-100 text-green-700': service.status === 'running',
                  'bg-red-100 text-red-700': service.status === 'stopped',
                  'bg-yellow-100 text-yellow-700': service.status === 'pending'
                }"
              >
                {{ getStatusText(service.status) }}
              </span>
            </div>
            <div class="text-sm text-gray-600">
              <span>主机: {{ service.host }}</span>
            </div>
          </div>

          <div class="flex space-x-2">
            <button 
              class="flex-1 px-3 py-2 text-sm bg-blue-500 text-white rounded hover:bg-blue-600 transition-colors"
              @click="manageService(service)"
            >
              管理
            </button>
            <button 
              class="flex-1 px-3 py-2 text-sm border border-gray-300 text-gray-700 rounded hover:bg-gray-50 transition-colors"
              @click="viewLogs(service)"
            >
              日志
            </button>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="text-center py-12">
        <svg class="mx-auto h-16 w-16 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
        <h3 class="mt-4 text-lg font-medium text-gray-900">暂无服务</h3>
        <p class="mt-1 text-sm text-gray-500">开始添加服务来管理您的集群</p>
        <button class="mt-4 px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors">
          添加第一个服务
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

// 服务列表数据
const serviceList = ref([
  {
    id: 1,
    name: 'HDFS',
    version: '3.3.4',
    status: 'running',
    host: 'node1.cluster.local'
  },
  {
    id: 2,
    name: 'YARN',
    version: '3.3.4',
    status: 'running',
    host: 'node2.cluster.local'
  },
  {
    id: 3,
    name: 'Spark',
    version: '3.4.0',
    status: 'stopped',
    host: 'node3.cluster.local'
  },
  {
    id: 4,
    name: 'Kafka',
    version: '2.8.1',
    status: 'pending',
    host: 'node4.cluster.local'
  }
])

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    running: '运行中',
    stopped: '已停止',
    pending: '待启动'
  }
  return statusMap[status] || '未知'
}

// 管理服务
const manageService = (service) => {
  console.log('管理服务:', service.name)
  // 这里可以添加服务管理逻辑
}

// 查看日志
const viewLogs = (service) => {
  console.log('查看日志:', service.name)
  // 这里可以添加日志查看逻辑
}

// 组件挂载时的逻辑
onMounted(() => {
  console.log('ServiceList 组件已挂载')
})
</script>

<style scoped>
.service-list-container {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.shadow-card {
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}
</style>