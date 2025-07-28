<template>
  <div class="alarm-list-container p-6">
    <!-- 页面头部 -->
    <div class="bg-white rounded-xl shadow-card backdrop-blur-md p-8 mb-8">
      <div>
        <h1 class="text-2xl font-semibold text-gray-900 mb-2">告警管理</h1>
        <p class="text-gray-600">监控和管理系统告警信息</p>
      </div>
    </div>

    <!-- 告警统计 -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
      <div class="bg-white rounded-lg shadow-card p-6">
        <div class="flex items-center">
          <div class="w-12 h-12 bg-red-100 rounded-lg flex items-center justify-center mr-4">
            <svg class="w-6 h-6 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
          <div>
            <div class="text-2xl font-bold text-gray-900">{{ alarmStats.critical }}</div>
            <div class="text-sm text-gray-500">严重告警</div>
          </div>
        </div>
      </div>

      <div class="bg-white rounded-lg shadow-card p-6">
        <div class="flex items-center">
          <div class="w-12 h-12 bg-orange-100 rounded-lg flex items-center justify-center mr-4">
            <svg class="w-6 h-6 text-orange-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div>
            <div class="text-2xl font-bold text-gray-900">{{ alarmStats.warning }}</div>
            <div class="text-sm text-gray-500">警告告警</div>
          </div>
        </div>
      </div>

      <div class="bg-white rounded-lg shadow-card p-6">
        <div class="flex items-center">
          <div class="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center mr-4">
            <svg class="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div>
            <div class="text-2xl font-bold text-gray-900">{{ alarmStats.info }}</div>
            <div class="text-sm text-gray-500">信息告警</div>
          </div>
        </div>
      </div>

      <div class="bg-white rounded-lg shadow-card p-6">
        <div class="flex items-center">
          <div class="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center mr-4">
            <svg class="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div>
            <div class="text-2xl font-bold text-gray-900">{{ alarmStats.resolved }}</div>
            <div class="text-sm text-gray-500">已解决</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 告警列表 -->
    <div class="bg-white rounded-xl shadow-card p-6">
      <div class="flex justify-between items-center mb-6">
        <h2 class="text-xl font-semibold text-gray-800">告警列表</h2>
        <div class="flex space-x-3">
          <select class="px-3 py-2 border border-gray-300 rounded-lg text-sm">
            <option value="all">全部级别</option>
            <option value="critical">严重</option>
            <option value="warning">警告</option>
            <option value="info">信息</option>
          </select>
          <button class="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors text-sm">
            刷新
          </button>
        </div>
      </div>

      <!-- 告警表格 -->
      <div v-if="alarmList.length > 0" class="overflow-x-auto">
        <table class="min-w-full">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">级别</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">告警内容</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">主机</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">服务</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">时间</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">状态</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">操作</th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-for="alarm in alarmList" :key="alarm.id" class="hover:bg-gray-50">
              <td class="px-6 py-4 whitespace-nowrap">
                <span 
                  class="px-2 py-1 rounded-full text-xs font-medium"
                  :class="{
                    'bg-red-100 text-red-700': alarm.level === 'critical',
                    'bg-orange-100 text-orange-700': alarm.level === 'warning',
                    'bg-blue-100 text-blue-700': alarm.level === 'info'
                  }"
                >
                  {{ getLevelText(alarm.level) }}
                </span>
              </td>
              <td class="px-6 py-4">
                <div class="text-sm font-medium text-gray-900">{{ alarm.title }}</div>
                <div class="text-sm text-gray-500">{{ alarm.description }}</div>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{{ alarm.host }}</td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{{ alarm.service }}</td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ formatTime(alarm.time) }}</td>
              <td class="px-6 py-4 whitespace-nowrap">
                <span 
                  class="px-2 py-1 rounded-full text-xs font-medium"
                  :class="{
                    'bg-green-100 text-green-700': alarm.status === 'resolved',
                    'bg-red-100 text-red-700': alarm.status === 'active'
                  }"
                >
                  {{ getStatusText(alarm.status) }}
                </span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                <button 
                  v-if="alarm.status === 'active'"
                  class="text-blue-600 hover:text-blue-900 mr-3"
                  @click="resolveAlarm(alarm)"
                >
                  解决
                </button>
                <button class="text-gray-600 hover:text-gray-900" @click="viewDetails(alarm)">
                  详情
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 空状态 -->
      <div v-else class="text-center py-12">
        <svg class="mx-auto h-16 w-16 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <h3 class="mt-4 text-lg font-medium text-gray-900">暂无告警</h3>
        <p class="mt-1 text-sm text-gray-500">系统运行正常，没有告警信息</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

// 告警统计数据
const alarmStats = ref({
  critical: 2,
  warning: 5,
  info: 8,
  resolved: 15
})

// 告警列表数据
const alarmList = ref([
  {
    id: 1,
    level: 'critical',
    title: 'HDFS DataNode 离线',
    description: 'DataNode node3.cluster.local 无法连接',
    host: 'node3.cluster.local',
    service: 'HDFS',
    time: new Date(Date.now() - 1000 * 60 * 30), // 30分钟前
    status: 'active'
  },
  {
    id: 2,
    level: 'warning',
    title: '磁盘使用率过高',
    description: '磁盘使用率达到85%',
    host: 'node1.cluster.local',
    service: 'System',
    time: new Date(Date.now() - 1000 * 60 * 60), // 1小时前
    status: 'active'
  },
  {
    id: 3,
    level: 'info',
    title: 'Spark 作业完成',
    description: 'Spark 作业 job_20231201_001 执行完成',
    host: 'node2.cluster.local',
    service: 'Spark',
    time: new Date(Date.now() - 1000 * 60 * 120), // 2小时前
    status: 'resolved'
  }
])

// 获取级别文本
const getLevelText = (level) => {
  const levelMap = {
    critical: '严重',
    warning: '警告',
    info: '信息'
  }
  return levelMap[level] || '未知'
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    active: '活跃',
    resolved: '已解决'
  }
  return statusMap[status] || '未知'
}

// 格式化时间
const formatTime = (time) => {
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(time)
}

// 解决告警
const resolveAlarm = (alarm) => {
  console.log('解决告警:', alarm.title)
  alarm.status = 'resolved'
  // 这里可以添加解决告警的逻辑
}

// 查看详情
const viewDetails = (alarm) => {
  console.log('查看告警详情:', alarm.title)
  // 这里可以添加查看详情的逻辑
}

// 组件挂载时的逻辑
onMounted(() => {
  console.log('AlarmList 组件已挂载')
})
</script>

<style scoped>
.alarm-list-container {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.shadow-card {
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}
</style>