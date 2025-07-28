<template>
  <div class="user-list-container p-6">
    <!-- 页面头部 -->
    <div class="bg-white rounded-xl shadow-card backdrop-blur-md p-8 mb-8">
      <div>
        <h1 class="text-2xl font-semibold text-gray-900 mb-2">用户管理</h1>
        <p class="text-gray-600">管理系统用户和权限</p>
      </div>
    </div>

    <!-- 用户统计 -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
      <div class="bg-white rounded-lg shadow-card p-6">
        <div class="flex items-center">
          <div class="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center mr-4">
            <svg class="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0z" />
            </svg>
          </div>
          <div>
            <div class="text-2xl font-bold text-gray-900">{{ userStats.total }}</div>
            <div class="text-sm text-gray-500">总用户数</div>
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
            <div class="text-2xl font-bold text-gray-900">{{ userStats.active }}</div>
            <div class="text-sm text-gray-500">活跃用户</div>
          </div>
        </div>
      </div>

      <div class="bg-white rounded-lg shadow-card p-6">
        <div class="flex items-center">
          <div class="w-12 h-12 bg-purple-100 rounded-lg flex items-center justify-center mr-4">
            <svg class="w-6 h-6 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
          </div>
          <div>
            <div class="text-2xl font-bold text-gray-900">{{ userStats.admin }}</div>
            <div class="text-sm text-gray-500">管理员</div>
          </div>
        </div>
      </div>

      <div class="bg-white rounded-lg shadow-card p-6">
        <div class="flex items-center">
          <div class="w-12 h-12 bg-orange-100 rounded-lg flex items-center justify-center mr-4">
            <svg class="w-6 h-6 text-orange-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
          </div>
          <div>
            <div class="text-2xl font-bold text-gray-900">{{ userStats.locked }}</div>
            <div class="text-sm text-gray-500">锁定用户</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 用户列表 -->
    <div class="bg-white rounded-xl shadow-card p-6">
      <div class="flex justify-between items-center mb-6">
        <h2 class="text-xl font-semibold text-gray-800">用户列表</h2>
        <div class="flex space-x-3">
          <input 
            v-model="searchQuery" 
            type="text" 
            placeholder="搜索用户..." 
            class="px-3 py-2 border border-gray-300 rounded-lg text-sm w-64"
          >
          <select v-model="roleFilter" class="px-3 py-2 border border-gray-300 rounded-lg text-sm">
            <option value="all">全部角色</option>
            <option value="admin">管理员</option>
            <option value="user">普通用户</option>
            <option value="guest">访客</option>
          </select>
          <button 
            @click="showAddUserModal = true" 
            class="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors text-sm"
          >
            添加用户
          </button>
        </div>
      </div>

      <!-- 用户表格 -->
      <div v-if="filteredUsers.length > 0" class="overflow-x-auto">
        <table class="min-w-full">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">用户</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">邮箱</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">角色</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">状态</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">最后登录</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">操作</th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-for="user in filteredUsers" :key="user.id" class="hover:bg-gray-50">
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="flex items-center">
                  <div class="w-10 h-10 bg-gray-300 rounded-full flex items-center justify-center mr-4">
                    <span class="text-sm font-medium text-gray-700">{{ user.name.charAt(0).toUpperCase() }}</span>
                  </div>
                  <div>
                    <div class="text-sm font-medium text-gray-900">{{ user.name }}</div>
                    <div class="text-sm text-gray-500">{{ user.username }}</div>
                  </div>
                </div>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{{ user.email }}</td>
              <td class="px-6 py-4 whitespace-nowrap">
                <span 
                  class="px-2 py-1 rounded-full text-xs font-medium"
                  :class="{
                    'bg-purple-100 text-purple-700': user.role === 'admin',
                    'bg-blue-100 text-blue-700': user.role === 'user',
                    'bg-gray-100 text-gray-700': user.role === 'guest'
                  }"
                >
                  {{ getRoleText(user.role) }}
                </span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <span 
                  class="px-2 py-1 rounded-full text-xs font-medium"
                  :class="{
                    'bg-green-100 text-green-700': user.status === 'active',
                    'bg-red-100 text-red-700': user.status === 'locked',
                    'bg-gray-100 text-gray-700': user.status === 'inactive'
                  }"
                >
                  {{ getStatusText(user.status) }}
                </span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                {{ user.lastLogin ? formatTime(user.lastLogin) : '从未登录' }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                <button 
                  class="text-blue-600 hover:text-blue-900 mr-3"
                  @click="editUser(user)"
                >
                  编辑
                </button>
                <button 
                  v-if="user.status === 'active'"
                  class="text-orange-600 hover:text-orange-900 mr-3"
                  @click="lockUser(user)"
                >
                  锁定
                </button>
                <button 
                  v-else-if="user.status === 'locked'"
                  class="text-green-600 hover:text-green-900 mr-3"
                  @click="unlockUser(user)"
                >
                  解锁
                </button>
                <button 
                  class="text-red-600 hover:text-red-900"
                  @click="deleteUser(user)"
                >
                  删除
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 空状态 -->
      <div v-else class="text-center py-12">
        <svg class="mx-auto h-16 w-16 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0z" />
        </svg>
        <h3 class="mt-4 text-lg font-medium text-gray-900">暂无用户</h3>
        <p class="mt-1 text-sm text-gray-500">没有找到符合条件的用户</p>
      </div>
    </div>

    <!-- 添加用户模态框 -->
    <div v-if="showAddUserModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-96">
        <h3 class="text-lg font-semibold mb-4">添加用户</h3>
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">用户名</label>
            <input v-model="newUser.username" type="text" class="w-full px-3 py-2 border border-gray-300 rounded-lg">
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">姓名</label>
            <input v-model="newUser.name" type="text" class="w-full px-3 py-2 border border-gray-300 rounded-lg">
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">邮箱</label>
            <input v-model="newUser.email" type="email" class="w-full px-3 py-2 border border-gray-300 rounded-lg">
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">角色</label>
            <select v-model="newUser.role" class="w-full px-3 py-2 border border-gray-300 rounded-lg">
              <option value="user">普通用户</option>
              <option value="admin">管理员</option>
              <option value="guest">访客</option>
            </select>
          </div>
        </div>
        <div class="flex justify-end space-x-3 mt-6">
          <button 
            @click="showAddUserModal = false" 
            class="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50"
          >
            取消
          </button>
          <button 
            @click="addUser" 
            class="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600"
          >
            添加
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

// 搜索和过滤
const searchQuery = ref('')
const roleFilter = ref('all')

// 模态框状态
const showAddUserModal = ref(false)

// 新用户表单
const newUser = ref({
  username: '',
  name: '',
  email: '',
  role: 'user'
})

// 用户统计数据
const userStats = ref({
  total: 12,
  active: 10,
  admin: 2,
  locked: 1
})

// 用户列表数据
const users = ref([
  {
    id: 1,
    username: 'admin',
    name: '系统管理员',
    email: 'admin@example.com',
    role: 'admin',
    status: 'active',
    lastLogin: new Date(Date.now() - 1000 * 60 * 30)
  },
  {
    id: 2,
    username: 'john.doe',
    name: '约翰·多伊',
    email: 'john.doe@example.com',
    role: 'user',
    status: 'active',
    lastLogin: new Date(Date.now() - 1000 * 60 * 60 * 2)
  },
  {
    id: 3,
    username: 'jane.smith',
    name: '简·史密斯',
    email: 'jane.smith@example.com',
    role: 'user',
    status: 'locked',
    lastLogin: new Date(Date.now() - 1000 * 60 * 60 * 24)
  },
  {
    id: 4,
    username: 'guest',
    name: '访客用户',
    email: 'guest@example.com',
    role: 'guest',
    status: 'active',
    lastLogin: null
  }
])

// 过滤后的用户列表
const filteredUsers = computed(() => {
  return users.value.filter(user => {
    const matchesSearch = user.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
                         user.username.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
                         user.email.toLowerCase().includes(searchQuery.value.toLowerCase())
    
    const matchesRole = roleFilter.value === 'all' || user.role === roleFilter.value
    
    return matchesSearch && matchesRole
  })
})

// 获取角色文本
const getRoleText = (role) => {
  const roleMap = {
    admin: '管理员',
    user: '普通用户',
    guest: '访客'
  }
  return roleMap[role] || '未知'
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    active: '活跃',
    locked: '锁定',
    inactive: '非活跃'
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

// 添加用户
const addUser = () => {
  if (!newUser.value.username || !newUser.value.name || !newUser.value.email) {
    alert('请填写所有必填字段')
    return
  }
  
  const user = {
    id: users.value.length + 1,
    ...newUser.value,
    status: 'active',
    lastLogin: null
  }
  
  users.value.push(user)
  userStats.value.total++
  userStats.value.active++
  if (user.role === 'admin') userStats.value.admin++
  
  // 重置表单
  newUser.value = {
    username: '',
    name: '',
    email: '',
    role: 'user'
  }
  
  showAddUserModal.value = false
  console.log('添加用户:', user)
}

// 编辑用户
const editUser = (user) => {
  console.log('编辑用户:', user.name)
  // 这里可以添加编辑用户的逻辑
}

// 锁定用户
const lockUser = (user) => {
  console.log('锁定用户:', user.name)
  user.status = 'locked'
  userStats.value.active--
  userStats.value.locked++
}

// 解锁用户
const unlockUser = (user) => {
  console.log('解锁用户:', user.name)
  user.status = 'active'
  userStats.value.active++
  userStats.value.locked--
}

// 删除用户
const deleteUser = (user) => {
  console.log('删除用户:', user.name)
  if (confirm(`确定要删除用户 ${user.name} 吗？`)) {
    const index = users.value.findIndex(u => u.id === user.id)
    if (index > -1) {
      users.value.splice(index, 1)
      userStats.value.total--
      if (user.status === 'active') userStats.value.active--
      if (user.status === 'locked') userStats.value.locked--
      if (user.role === 'admin') userStats.value.admin--
    }
  }
}

// 组件挂载时的逻辑
onMounted(() => {
  console.log('UserList 组件已挂载')
})
</script>

<style scoped>
.user-list-container {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.shadow-card {
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}
</style>