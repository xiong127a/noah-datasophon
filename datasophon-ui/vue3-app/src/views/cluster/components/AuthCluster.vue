<template>
  <div class="w-full bg-white rounded-lg overflow-hidden flex flex-col relative shadow-lg">
    <!-- 顶部蓝色线条 -->
    <div class="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-blue-400 via-blue-500 to-blue-600 z-10"></div>
    
    <!-- 顶部区域 -->
    <div class="relative overflow-hidden bg-gradient-to-br from-blue-400 to-blue-600">
      <!-- 装饰性径向渐变 -->
      <div class="absolute -top-full -left-full right-0 bottom-0 bg-radial-gradient opacity-70 transform -rotate-35 z-0"></div>
      
      <div class="relative z-10 flex items-center py-7 px-8">
        <!-- 用户图标 -->
        <div class="relative mr-5">
          <!-- 脉冲动画圈 -->
          <div class="absolute inset-[-4px] rounded-full border-2 border-white/40 animate-pulse"></div>
          <div class="absolute inset-[-8px] rounded-full border border-white/20 animate-pulse [animation-delay:500ms]"></div>
          
          <div class="w-13 h-13 bg-white/25 rounded-full flex items-center justify-center relative overflow-hidden shadow-md">
            <div class="absolute inset-0 bg-gradient-to-br from-white/40 to-transparent"></div>
            <svg class="w-6 h-6 text-white relative z-10" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 12C14.7614 12 17 9.76142 17 7C17 4.23858 14.7614 2 12 2C9.23858 2 7 4.23858 7 7C7 9.76142 9.23858 12 12 12Z" fill="currentColor"/>
              <path d="M12 14C7.58172 14 4 17.5817 4 22H20C20 17.5817 16.4183 14 12 14Z" fill="currentColor"/>
            </svg>
          </div>
        </div>
        
        <!-- 标题 -->
        <div class="text-white">
          <h1 class="text-xl font-semibold mb-2 drop-shadow-sm">集群授权管理</h1>
          <p class="text-sm opacity-95 drop-shadow-sm">
            为集群 <span class="font-semibold bg-white/25 rounded-md px-2 py-1 mx-1 shadow-sm">{{ detail.clusterName || '未知集群' }}</span> 分配管理员权限
          </p>
        </div>
      </div>
    </div>
    
    <!-- 内容区域 -->
    <div class="p-6 bg-gradient-to-b from-gray-50 to-gray-100 flex-1 relative">
      <!-- 顶部分割线效果 -->
      <div class="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/80 to-transparent"></div>

      <div class="max-w-lg mx-auto bg-white rounded-lg shadow-sm p-7 border border-blue-500/10">
        <div class="mb-5">
          <label class="block mb-3 text-sm font-medium text-gray-800 relative pl-3">
            <!-- 蓝色垂直指示条 -->
            <span class="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-[18px] bg-gradient-to-b from-blue-400 to-blue-600 rounded"></span>
            选择管理员：
          </label>
          
          <div class="relative">
            <!-- 用户选择下拉菜单 -->
            <div v-if="userListLoaded">
              <Listbox v-model="selectedUserIds" multiple>
                <div class="relative mt-1">
                  <ListboxButton class="relative w-full cursor-default rounded-lg bg-white py-2 pl-3 pr-10 text-left border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500 shadow-sm">
                    <span class="block truncate">
                      {{ selectedUserIds.length === 0 ? '请选择一个或多个集群管理员' : 
                         selectedUserIds.length === 1 ? formatSelectedUser(selectedUserIds[0]) :
                         `${formatSelectedUser(selectedUserIds[0])}等${selectedUserIds.length}人` }}
                    </span>
                    <span class="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-2">
                      <svg class="h-5 w-5 text-gray-400" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                        <path fill-rule="evenodd" d="M10 3a.75.75 0 01.55.24l3.25 3.5a.75.75 0 11-1.1 1.02L10 4.852 7.3 7.76a.75.75 0 01-1.1-1.02l3.25-3.5A.75.75 0 0110 3z" clip-rule="evenodd" />
                      </svg>
                    </span>
                  </ListboxButton>
                  <transition
                    leave-active-class="transition ease-in duration-100"
                    leave-from-class="opacity-100"
                    leave-to-class="opacity-0"
                  >
                    <ListboxOptions class="absolute z-10 mt-1 max-h-60 w-full overflow-auto rounded-md bg-white py-1 text-base shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none sm:text-sm">
                      <ListboxOption
                        v-for="user in userList"
                        :key="user.id"
                        :value="user.id"
                        v-slot="{ active, selected }"
                      >
                        <li
                          :class="[
                            active ? 'bg-blue-100 text-blue-900' : 'text-gray-900',
                            'relative cursor-default select-none py-2 pl-10 pr-4'
                          ]"
                        >
                          <span :class="[selected ? 'font-medium' : 'font-normal', 'block truncate']">
                            {{ user.username }}
                          </span>
                          <span
                            v-if="selected"
                            class="absolute inset-y-0 left-0 flex items-center pl-3 text-blue-600"
                          >
                            <svg class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                              <path fill-rule="evenodd" d="M16.704 4.153a.75.75 0 01.143 1.052l-8 10.5a.75.75 0 01-1.127.075l-4.5-4.5a.75.75 0 011.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 011.05-.143z" clip-rule="evenodd" />
                            </svg>
                          </span>
                        </li>
                      </ListboxOption>
                    </ListboxOptions>
                  </transition>
                </div>
              </Listbox>
            </div>
            <!-- 加载状态 -->
            <div v-else class="flex items-center justify-center h-11 px-4 bg-blue-50 border border-blue-200/50 rounded-lg text-gray-500">
              <div class="w-4 h-4 border-2 border-blue-100 border-t-blue-500 rounded-full mr-2.5 animate-spin"></div>
              <span>加载用户数据中...</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部按钮 -->
    <div class="px-7 py-6 bg-white border-t border-gray-200 flex justify-center">
      <div class="flex gap-4.5">
        <button 
          @click="handleSubmit" 
          :disabled="loading"
          class="min-w-[130px] h-[42px] rounded-full text-white font-medium text-sm
                 bg-gradient-to-r from-blue-400 via-blue-500 to-blue-600 
                 shadow-md shadow-blue-500/25 relative overflow-hidden
                 transition-all duration-300 transform hover:-translate-y-0.5 hover:shadow-lg hover:shadow-blue-500/30
                 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:ring-offset-2
                 active:translate-y-0 active:shadow-md active:shadow-blue-500/25"
        >
          <span class="relative z-10 flex items-center justify-center">
            <svg v-if="loading" class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            确认授权
          </span>
          <span class="absolute inset-0 bg-gradient-to-br from-white/30 to-transparent opacity-0 hover:opacity-100 transition-opacity duration-300"></span>
        </button>
        <button 
          @click="formCancel"
          class="min-w-[130px] h-[42px] rounded-full text-gray-600 font-medium text-sm
                 border border-gray-200 bg-white
                 transition-all duration-300 transform hover:-translate-y-0.5 hover:border-blue-500 hover:text-blue-600 hover:shadow-md
                 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:ring-offset-2
                 active:translate-y-0"
        >
          <span class="relative z-10">取消</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Listbox, ListboxButton, ListboxOptions, ListboxOption } from '@headlessui/vue'
import { axiosPost, axiosGet } from '@/utils/request'
import API from '@/api/httpApi/cluster'

// Props
const props = defineProps({
  detail: {
    type: Object,
    default: () => ({})
  },
  callBack: {
    type: Function,
    default: () => {}
  }
})

// Emits
const emit = defineEmits(['cancel'])

// 提前从props中提取managerIds
const extractManagerIds = (detail) => {
  if (detail && 
      detail.clusterManagerList && 
      Array.isArray(detail.clusterManagerList)) {
    return detail.clusterManagerList.map(manager => manager.id)
  }
  return []
}

// Reactive state
const loading = ref(false)
const userList = ref([])
const selectedUserIds = ref(extractManagerIds(props.detail))
const userListLoaded = ref(false)

// 格式化选中的用户名称
const formatSelectedUser = (userId) => {
  const user = userList.value.find(u => u.id === userId)
  return user ? user.username : userId
}

const formCancel = () => {
  emit('cancel')
}

// 查询所有用户
const queryAllUser = () => {
  axiosPost(API.queryAllUser, {})
    .then((res) => {
      if (res.code === 200) {
        userList.value = res.data
        // 标记用户列表已加载
        userListLoaded.value = true
      } else {
        alert(res.msg || '获取用户列表失败')
      }
    })
    .catch(() => {
      alert('获取用户列表失败，请检查网络连接')
    })
}

const handleSubmit = () => {
  // 检查clusterId是否存在
  if (!props.detail || !props.detail.id) {
    alert('缺少集群ID参数')
    return
  }
  
  // 使用selectedUserIds
  const userIds = selectedUserIds.value || []
  let userIdsString = ''
  
  // 转换用户ID数组为字符串
  if (Array.isArray(userIds)) {
    userIdsString = userIds.join(',')
  } else {
    userIdsString = userIds.toString()
  }
  
  // 构建URL查询参数
  const url = `${API.authCluster}?clusterId=${props.detail.id}&userIds=${userIdsString}`
  
  loading.value = true
  // 使用get方法，通过URL传参
  axiosGet(url)
    .then((res) => {
      loading.value = false
      if (res.code === 200) {
        if (userIds && userIds.length > 0) {
          alert('授权成功')
        } else {
          alert('取消授权成功')
        }
        // 调用callBack并关闭模态框
        if (props.callBack) {
          props.callBack()
        } else {
          // 如果没有传入callBack，则直接触发cancel事件
          emit('cancel')
        }
      } else {
        alert(res.msg || '授权失败')
      }
    })
    .catch((error) => {
      loading.value = false
      alert('授权失败，请检查网络或参数')
    })
}

// 生命周期钩子
onMounted(() => {
  queryAllUser()
})
</script>

<style scoped>
/* 径向渐变背景 */
.bg-radial-gradient {
  background: radial-gradient(circle, rgba(255, 255, 255, 0.3) 0%, rgba(255, 255, 255, 0) 70%);
}

/* 脉冲动画 */
@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 0.7;
  }
  50% {
    transform: scale(1.12);
    opacity: 0.4;
  }
  100% {
    transform: scale(1);
    opacity: 0.7;
  }
}

.animate-pulse {
  animation: pulse 2.5s infinite;
}

/* 旋转动画 */
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.animate-spin {
  animation: spin 0.8s linear infinite;
}
</style> 