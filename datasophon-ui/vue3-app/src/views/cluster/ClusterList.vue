<template>
  <div class="px-6 py-8 min-h-screen bg-gray-50">
    <!-- 页面头部横幅 -->
    <div class="bg-white rounded-xl shadow-card backdrop-blur-md p-8 mb-8">
      <div>
        <h1 class="text-2xl font-semibold text-gray-900 mb-2">集群管理</h1>
        <p class="text-gray-600">管理和监控您的大数据集群，快速部署各类服务</p>
      </div>
    </div>

    <!-- 集群卡片网格 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <!-- 现有集群卡片 -->
      <div 
        v-for="(item, index) in filteredDataSource" 
        :key="index"
        class="bg-white rounded-xl shadow-card overflow-hidden border-l-4 transition-all duration-300 hover:shadow-lg hover:-translate-y-1"
        :class="{
          'border-l-orange-500': item.depType === 'PVM',
          'border-l-blue-500': item.depType === 'Kubernetes',
          'border-l-gray-400': !item.depType || item.depType === 'default'
        }"
      >
        <!-- 集群状态标签 -->
        <div 
          class="absolute top-4 right-4 px-3 py-1 rounded-full text-xs font-medium backdrop-blur-md"
          :class="{
            'bg-green-100 text-green-700': item.clusterStateCode === 2,
            'bg-red-100 text-red-700': item.clusterStateCode === 3,
            'bg-orange-100 text-orange-700': item.clusterStateCode === 1 || item.clusterStateCode !== 2 && item.clusterStateCode !== 3
          }"
        >
          {{ item.clusterState }}
        </div>

        <!-- 集群头部 -->
        <div class="p-6 border-b border-gray-100">
          <div class="flex items-center space-x-4">
            <div class="h-10 w-10 flex items-center justify-center">
              <img v-if="item.depType === 'PVM'" src="@/assets/img/os-logos/linux-tux.svg" alt="Linux" class="w-9 h-9" />
              <img v-else-if="item.depType === 'Kubernetes'" src="@/assets/images/kubernetes-logo.svg" alt="Kubernetes" class="w-9 h-9" />
              <svg-icon v-else icon-class="colony" class="w-9 h-9 text-primary" />
            </div>
            <div class="flex-1 min-w-0">
              <h3 class="text-lg font-semibold text-gray-900 truncate">{{ item.clusterName }}</h3>
              <div class="flex flex-wrap gap-2 mt-1">
                <span class="inline-flex items-center bg-gray-100 px-2.5 py-0.5 rounded-md text-xs font-medium text-gray-800">
                  {{ getClusterTypeText(item.depType) }}
                </span>
                <span class="text-xs text-gray-500">{{ formatDate(item.createTime) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 集群内容 -->
        <div class="px-6 py-4">
          <div class="flex items-center">
            <div class="text-sm text-gray-500 mr-2">管理员</div>
            <div class="flex-1 px-3 py-1 text-sm bg-gray-50 rounded-md text-gray-900">
              {{ item.userManageName || '未分配' }}
            </div>
          </div>
        </div>

        <!-- 卡片底部按钮区域 -->
        <div class="px-6 py-4 mt-auto">
          <div class="flex flex-col space-y-3">
            <!-- 主按钮 - 进入集群 -->
            <button
              @click="getInto(item)"
              :disabled="item.clusterStateCode === 1"
              class="w-full py-2.5 px-4 rounded-lg font-medium transition duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
              :class="item.clusterStateCode === 1 
                ? 'bg-gray-400 text-gray-100 cursor-not-allowed relative overflow-hidden' 
                : 'bg-primary hover:bg-primary-600 text-white'"
            >
              <span>进入集群</span>
            </button>
            
            <!-- 次要按钮组 -->
            <div class="grid grid-cols-3 gap-2">
              <button
                @click="addColony(item)"
                :disabled="item.clusterStateCode === 2"
                class="py-2 px-3 rounded-lg border text-sm font-medium transition-colors duration-200"
                :class="item.clusterStateCode === 2 
                  ? 'border-gray-200 text-gray-400 cursor-not-allowed'
                  : 'border-gray-200 hover:border-primary hover:text-primary hover:bg-blue-50'"
              >
                <span>编辑</span>
              </button>
              
              <button
                v-if="user && user.userType === 1"
                @click="authCluster(item)"
                class="py-2 px-3 rounded-lg border border-gray-200 text-sm font-medium hover:border-primary hover:text-primary hover:bg-blue-50 transition-colors duration-200"
              >
                <span>授权</span>
              </button>
              
              <!-- 更多按钮 (使用 Headless UI Menu) -->
              <Menu as="div" class="relative">
                <MenuButton
                  class="w-full py-2 px-3 rounded-lg border border-gray-200 text-sm font-medium hover:border-primary hover:text-primary hover:bg-blue-50 transition-colors duration-200"
                  @click.stop
                >
                  更多
                </MenuButton>
                <transition
                  enter-active-class="transition duration-100 ease-out"
                  enter-from-class="transform scale-95 opacity-0"
                  enter-to-class="transform scale-100 opacity-100"
                  leave-active-class="transition duration-75 ease-in"
                  leave-from-class="transform scale-100 opacity-100"
                  leave-to-class="transform scale-95 opacity-0"
                >
                  <MenuItems class="absolute right-0 z-10 mt-2 w-40 origin-top-right rounded-md bg-white shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none">
                    <div class="py-1">
                      <MenuItem v-slot="{ active }">
                        <button
                          :class="[
                            active ? 'bg-gray-100 text-gray-900' : 'text-gray-700',
                            'flex w-full px-4 py-2 text-left text-sm',
                            item.clusterStateCode === 2 ? 'opacity-50 cursor-not-allowed' : ''
                          ]"
                          :disabled="item.clusterStateCode === 2"
                          @click="configCluster(item)"
                        >
                          配置集群
                        </button>
                      </MenuItem>
                      <MenuItem v-slot="{ active }">
                        <button
                          :class="[
                            active ? 'bg-red-50 text-red-700' : 'text-red-600',
                            'flex w-full px-4 py-2 text-left text-sm',
                            item.clusterStateCode === 2 ? 'opacity-50 cursor-not-allowed' : ''
                          ]"
                          :disabled="item.clusterStateCode === 2"
                          @click="delectColony(item)"
                        >
                          删除集群
                        </button>
                      </MenuItem>
                    </div>
                  </MenuItems>
                </transition>
              </Menu>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 创建新集群卡片 -->
      <div 
        @click="addColony({})"
        class="bg-white rounded-xl border-2 border-dashed border-gray-200 flex flex-col items-center justify-center p-6 text-center cursor-pointer transition-all duration-300 hover:border-primary hover:shadow-md hover:-translate-y-1"
      >
        <div class="bg-blue-50 w-16 h-16 rounded-full flex items-center justify-center mb-4">
          <svg class="w-8 h-8 text-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
          </svg>
        </div>
        <h3 class="text-xl font-semibold text-gray-900 mb-2">创建新集群</h3>
        <p class="text-gray-600 mb-6">快速部署一个全新的大数据集群环境</p>
        <div class="flex flex-wrap justify-center gap-2">
          <span class="bg-blue-50 text-primary text-xs font-medium px-2.5 py-1 rounded">一键部署</span>
          <span class="bg-blue-50 text-primary text-xs font-medium px-2.5 py-1 rounded">智能配置</span>
          <span class="bg-blue-50 text-primary text-xs font-medium px-2.5 py-1 rounded">高效运维</span>
        </div>
      </div>
    </div>

    <!-- 配置集群的modal (使用 Headless UI Dialog) -->
    <Dialog :open="visible" @close="visible = false" class="relative z-50">
      <div class="fixed inset-0 bg-black/30 backdrop-blur-sm" aria-hidden="true" />
      <div class="fixed inset-0 flex items-center justify-center p-4">
        <DialogPanel class="w-full max-w-6xl rounded-xl bg-white p-6 shadow-xl">
          <DialogTitle class="text-lg font-medium leading-6 text-gray-900">
            配置集群
          </DialogTitle>
          <div class="mt-4 p-6">
            <p class="text-gray-600">此功能需要Steps组件，正在开发中...</p>
            <div class="mt-6 flex justify-end">
              <button 
                class="px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary-600 transition-colors"
                @click="handleCancel"
              >
                关闭
              </button>
            </div>
          </div>
        </DialogPanel>
      </div>
    </Dialog>

    <!-- 授权集群模态框 -->
    <Dialog :open="authModalVisible" @close="handleAuthModalClose" class="relative z-50">
      <div class="fixed inset-0 bg-black/30 backdrop-blur-sm" aria-hidden="true" />
      <div class="fixed inset-0 flex items-center justify-center p-4">
        <DialogPanel class="w-full max-w-md rounded-xl bg-white p-6 shadow-xl">
          <AuthCluster 
            v-if="currentClusterForAuth" 
            :detail="currentClusterForAuth" 
            :callBack="handleAuthComplete" 
            @cancel="handleAuthModalClose"
          />
        </DialogPanel>
      </div>
    </Dialog>
    
    <!-- 编辑集群模态框 -->
    <Dialog :open="editModalVisible" @close="handleEditModalClose" class="relative z-50">
      <div class="fixed inset-0 bg-black/30 backdrop-blur-sm" aria-hidden="true" />
      <div class="fixed inset-0 flex items-center justify-center p-4">
        <DialogPanel class="w-full max-w-2xl rounded-xl bg-white p-6 shadow-xl">
          <DialogTitle class="text-lg font-medium leading-6 text-gray-900">
            {{ currentEditObj && JSON.stringify(currentEditObj) !== '{}' ? '编辑集群配置' : '创建新集群' }}
          </DialogTitle>
          <div class="mt-4">
            <AddColony 
              v-if="editModalVisible" 
              :detail="currentEditObj || {}" 
              :callBack="handleEditComplete" 
              @cancel="handleEditModalClose"
              @success="handleEditComplete"
              ref="addColonyForm"
            />
          </div>
        </DialogPanel>
      </div>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import SvgIcon from '@/components/SvgIcon.vue'
// 添加Headless UI组件导入
import { Dialog, DialogPanel, DialogTitle, Menu, MenuButton, MenuItems, MenuItem } from '@headlessui/vue'

// 导入子组件
import AddColony from './components/AddColony.vue'
import AuthCluster from './components/AuthCluster.vue'

// 导入API和请求工具
import { clusterApi } from '@/api/httpApi'
import { axiosPost } from '@/utils/request'
import { changeRouter } from '@/utils/changeRouter'
import { useToast } from '@/composables/useToast'
import { useUserStore } from '@/stores/user'
import { useSettingsStore } from '@/stores/settings'

// 获取store和toast功能
const userStore = useUserStore()
const settingsStore = useSettingsStore()
const { toast } = useToast()

// 定义API常量（后台API路径）
const API_PATHS = {
  getServiceListByCluster: '/service/getListByCluster',
  getColonyList: '/colony/queryColony',
  deleteColony: '/colony/delColony'
}

// 组件状态
const router = useRouter()
const dataSource = ref([])
const editModalVisible = ref(false)
const currentEditObj = ref(null)
const addColonyForm = ref(null)
const authModalVisible = ref(false)
const currentClusterForAuth = ref(null)
// 添加缺失的变量
const visible = ref(false)
const clusterId = ref('')
const depType = ref('')

// 计算属性
const user = computed(() => userStore.user)

// 过滤掉添加集群的占位项
const filteredDataSource = computed(() => {
  return dataSource.value.filter(item => !item.add)
})

// 生命周期钩子
onMounted(() => {
  getColonyList()
})

// 方法
const addColony = (obj) => {
  editModalVisible.value = true
  currentEditObj.value = obj
  
  // 确保模态框内容正确渲染
  nextTick(() => {
    forceRefreshModal()
  })
}

const handleEditModalClose = () => {
  editModalVisible.value = false
  currentEditObj.value = null
}

const handleEditComplete = () => {
  getColonyList()
  handleEditModalClose()
}

// 强制刷新模态框内容
const forceRefreshModal = () => {
  setTimeout(() => {
    if (addColonyForm.value) {
      const formEl = addColonyForm.value.$el
      if (formEl) {
        formEl.style.display = 'block'
        formEl.style.visibility = 'visible'
        formEl.style.opacity = '1'
        formEl.style.height = 'auto'
        formEl.style.minHeight = '300px'
      }
    }
  }, 100)
}

// 获取集群列表
const getColonyList = async () => {
  try {
    // 使用API库调用方式
    const res = await clusterApi.getClusterList({})
    if (res && res.code === 200) {
      dataSource.value = res.data || []
      processClusterData()
    }
  } catch (err) {
    console.error('API调用错误:', err)
    // 回退到直接HTTP调用
    try {
      const res = await axiosPost(API_PATHS.getColonyList, {})
      if (res && res.code === 200) {
        dataSource.value = res.data || []
        processClusterData()
      }
    } catch (fallbackErr) {
      console.error('回退API调用也失败:', fallbackErr)
      toast('获取集群列表失败', 'error')
    }
  }
}

// 处理集群数据
const processClusterData = () => {
  dataSource.value.forEach((item) => {
    let arr = []
    if (item.clusterManagerList && Array.isArray(item.clusterManagerList)) {
      item.clusterManagerList.forEach((childItem) => {
        if (childItem && childItem.username) {
          arr.push(childItem.username)
        }
      })
    }
    item.userManageName = arr.join(",")
  })
}

// 进入集群
const getInto = async (row) => {
  try {
    const res = await axiosPost(API_PATHS.getServiceListByCluster, {
      clusterId: row.id,
    })
    if (res && res.data) {
      changeRouter(res.data, row.id, router)
      router.push("/service-manage")
    }
  } catch (err) {
    console.error('进入集群失败:', err)
    toast('进入集群失败', 'error')
  }
}

// 删除集群
const delectColony = async (obj) => {
  if (!confirm(`确定要删除集群 "${obj.clusterName}" 吗？此操作不可恢复。`)) {
    return
  }
  
  try {
    // 尝试使用API库方式
    const res = await clusterApi.deleteCluster(obj.id)
    if (res && res.code === 200) {
      toast('删除成功', 'success')
      getColonyList()
    } else {
      toast(res?.msg || '删除失败', 'error')
    }
  } catch (err) {
    console.error('删除集群失败:', err)
    // 回退到直接HTTP调用
    try {
      const res = await axiosPost(API_PATHS.deleteColony, { id: obj.id })
      if (res && res.code === 200) {
        toast('删除成功', 'success')
        getColonyList()
      } else {
        toast(res?.msg || '删除失败', 'error')
      }
    } catch (fallbackErr) {
      console.error('回退API调用也失败:', fallbackErr)
      toast('删除集群失败', 'error')
    }
  }
}

// 集群授权
const authCluster = (obj) => {
  currentClusterForAuth.value = obj
  authModalVisible.value = true
}

const handleAuthModalClose = () => {
  authModalVisible.value = false
  currentClusterForAuth.value = null
}

const handleAuthComplete = () => {
  getColonyList()
  handleAuthModalClose()
}

// 配置集群
const configCluster = (row) => {
  clusterId.value = row.id
  settingsStore.setClusterId(row.id)
  visible.value = true
  depType.value = row.depType
}

const handleCancel = () => {
  visible.value = false
  getColonyList()
}

// 获取状态样式类
const getStatusClass = (statusCode) => {
  switch(statusCode) {
    case 2: return 'running'
    case 3: return 'error'
    default: return 'configured'
  }
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

// 获取集群类型样式类
const getClusterTypeClass = (depType) => {
  switch(depType) {
    case 'PVM': return 'linux-type'
    case 'Kubernetes': return 'k8s-type'
    default: return 'default-type'
  }
}

// 获取集群类型文本
const getClusterTypeText = (depType) => {
  switch(depType) {
    case 'PVM': return '裸金属/虚拟机'
    case 'Kubernetes': return 'Kubernetes'
    default: return '未知'
  }
}
</script>
