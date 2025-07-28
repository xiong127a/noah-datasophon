<template>
  <div class="system-log-container p-6">
    <!-- 页面头部 -->
    <div class="bg-white rounded-xl shadow-card backdrop-blur-md p-8 mb-8">
      <div>
        <h1 class="text-2xl font-semibold text-gray-900 mb-2">系统日志</h1>
        <p class="text-gray-600">查看和管理系统运行日志</p>
      </div>
    </div>

    <!-- 日志过滤器 -->
    <div class="bg-white rounded-xl shadow-card p-6 mb-8">
      <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">日志级别</label>
          <select v-model="filters.level" class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm">
            <option value="all">全部级别</option>
            <option value="error">错误</option>
            <option value="warn">警告</option>
            <option value="info">信息</option>
            <option value="debug">调试</option>
          </select>
        </div>
        
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">服务</label>
          <select v-model="filters.service" class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm">
            <option value="all">全部服务</option>
            <option value="hdfs">HDFS</option>
            <option value="yarn">YARN</option>
            <option value="spark">Spark</option>
            <option value="kafka">Kafka</option>
          </select>
        </div>
        
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">主机</label>
          <select v-model="filters.host" class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm">
            <option value="all">全部主机</option>
            <option value="node1">node1.cluster.local</option>
            <option value="node2">node2.cluster.local</option>
            <option value="node3">node3.cluster.local</option>
          </select>
        </div>
        
        <div class="flex items-end">
          <button 
            @click="refreshLogs" 
            class="w-full px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors text-sm"
          >
            刷新日志
          </button>
        </div>
      </div>
    </div>

    <!-- 日志列表 -->
    <div class="bg-white rounded-xl shadow-card p-6">
      <div class="flex justify-between items-center mb-6">
        <h2 class="text-xl font-semibold text-gray-800">日志记录</h2>
        <div class="flex space-x-3">
          <button 
            @click="exportLogs" 
            class="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors text-sm"
          >
            导出日志
          </button>
          <button 
            @click="clearLogs" 
            class="px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors text-sm"
          >
            清空日志
          </button>
        </div>
      </div>

      <!-- 日志表格 -->
      <div v-if="filteredLogs.length > 0" class="overflow-x-auto">
        <table class="min-w-full">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">时间</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">级别</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">服务</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">主机</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">消息</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">操作</th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-for="log in paginatedLogs" :key="log.id" class="hover:bg-gray-50">
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                {{ formatTime(log.timestamp) }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <span 
                  class="px-2 py-1 rounded-full text-xs font-medium"
                  :class="{
                    'bg-red-100 text-red-700': log.level === 'error',
                    'bg-orange-100 text-orange-700': log.level === 'warn',
                    'bg-blue-100 text-blue-700': log.level === 'info',
                    'bg-gray-100 text-gray-700': log.level === 'debug'
                  }"
                >
                  {{ log.level.toUpperCase() }}
                </span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{{ log.service }}</td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{{ log.host }}</td>
              <td class="px-6 py-4 text-sm text-gray-900">
                <div class="max-w-xs truncate" :title="log.message">{{ log.message }}</div>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                <button 
                  class="text-blue-600 hover:text-blue-900"
                  @click="viewLogDetail(log)"
                >
                  详情
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div v-if="filteredLogs.length > pageSize" class="mt-6 flex justify-between items-center">
        <div class="text-sm text-gray-700">
          显示 {{ (currentPage - 1) * pageSize + 1 }} 到 {{ Math.min(currentPage * pageSize, filteredLogs.length) }} 条，共 {{ filteredLogs.length }} 条记录
        </div>
        <div class="flex space-x-2">
          <button 
            @click="currentPage--" 
            :disabled="currentPage === 1"
            class="px-3 py-1 border border-gray-300 rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed"
          >
            上一页
          </button>
          <span class="px-3 py-1 text-sm">{{ currentPage }} / {{ totalPages }}</span>
          <button 
            @click="currentPage++" 
            :disabled="currentPage === totalPages"
            class="px-3 py-1 border border-gray-300 rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed"
          >
            下一页
          </button>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="text-center py-12">
        <svg class="mx-auto h-16 w-16 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
        <h3 class="mt-4 text-lg font-medium text-gray-900">暂无日志</h3>
        <p class="mt-1 text-sm text-gray-500">没有找到符合条件的日志记录</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

// 过滤器
const filters = ref({
  level: 'all',
  service: 'all',
  host: 'all'
})

// 分页
const currentPage = ref(1)
const pageSize = ref(20)

// 日志数据
const logs = ref([
  {
    id: 1,
    timestamp: new Date(Date.now() - 1000 * 60 * 5),
    level: 'error',
    service: 'HDFS',
    host: 'node1.cluster.local',
    message: 'DataNode connection failed: Connection refused'
  },
  {
    id: 2,
    timestamp: new Date(Date.now() - 1000 * 60 * 10),
    level: 'warn',
    service: 'YARN',
    host: 'node2.cluster.local',
    message: 'ResourceManager memory usage is high: 85%'
  },
  {
    id: 3,
    timestamp: new Date(Date.now() - 1000 * 60 * 15),
    level: 'info',
    service: 'Spark',
    host: 'node3.cluster.local',
    message: 'Spark application spark-app-001 started successfully'
  },
  {
    id: 4,
    timestamp: new Date(Date.now() - 1000 * 60 * 20),
    level: 'debug',
    service: 'Kafka',
    host: 'node1.cluster.local',
    message: 'Consumer group rebalance completed'
  },
  {
    id: 5,
    timestamp: new Date(Date.now() - 1000 * 60 * 25),
    level: 'info',
    service: 'HDFS',
    host: 'node2.cluster.local',
    message: 'Block replication completed for block_001'
  }
])

// 过滤后的日志
const filteredLogs = computed(() => {
  return logs.value.filter(log => {
    if (filters.value.level !== 'all' && log.level !== filters.value.level) {
      return false
    }
    if (filters.value.service !== 'all' && log.service.toLowerCase() !== filters.value.service) {
      return false
    }
    if (filters.value.host !== 'all' && !log.host.includes(filters.value.host)) {
      return false
    }
    return true
  })
})

// 分页后的日志
const paginatedLogs = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredLogs.value.slice(start, end)
})

// 总页数
const totalPages = computed(() => {
  return Math.ceil(filteredLogs.value.length / pageSize.value)
})

// 格式化时间
const formatTime = (time) => {
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).format(time)
}

// 刷新日志
const refreshLogs = () => {
  console.log('刷新日志')
  // 这里可以添加刷新日志的逻辑
}

// 导出日志
const exportLogs = () => {
  console.log('导出日志')
  // 这里可以添加导出日志的逻辑
}

// 清空日志
const clearLogs = () => {
  console.log('清空日志')
  if (confirm('确定要清空所有日志吗？此操作不可恢复。')) {
    logs.value = []
  }
}

// 查看日志详情
const viewLogDetail = (log) => {
  console.log('查看日志详情:', log)
  alert(`日志详情:\n\n时间: ${formatTime(log.timestamp)}\n级别: ${log.level}\n服务: ${log.service}\n主机: ${log.host}\n消息: ${log.message}`)
}

// 组件挂载时的逻辑
onMounted(() => {
  console.log('SystemLog 组件已挂载')
})
</script>

<style scoped>
.system-log-container {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.shadow-card {
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}
</style>