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
        class="bg-white rounded-xl shadow-card overflow-hidden border-l-4 transition-all duration-300 hover:shadow-lg hover:-translate-y-1 relative"
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
              <img v-else-if="item.depType === 'Kubernetes'" src="@/assets/kubernetes-logo.svg" alt="Kubernetes" class="w-9 h-9" />
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
              <!-- 禁用状态特殊标记 -->
              <div v-if="item.clusterStateCode === 1" class="absolute right-0 top-0">
                <div class="bg-red-500 text-white text-[10px] font-bold py-1 px-2 transform rotate-45 translate-x-[18px] -translate-y-[10px]">
                  无法访问
                </div>
              </div>
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
    <Dialog :open="visible" @close="handleCancel" class="relative z-50">
      <div class="fixed inset-0 bg-black/30 backdrop-blur-sm" aria-hidden="true" />
      <div class="fixed inset-0 flex items-center justify-center p-4">
        <DialogPanel class="w-full max-w-6xl rounded-xl bg-white shadow-xl overflow-hidden">
          <div class="flex justify-between items-center p-4 border-b border-gray-100">
          <DialogTitle class="text-lg font-medium leading-6 text-gray-900">
            配置集群
          </DialogTitle>
              <button 
                @click="handleCancel"
              class="text-gray-400 hover:text-gray-500 transition-colors"
              >
              <svg class="w-5 h-5" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
              </svg>
              </button>
          </div>
          <div class="p-6 max-h-[80vh] overflow-y-auto">
            <div v-if="stepsComponentLoaded">
              <!-- Steps组件将在此处渲染 -->
              <component :is="stepsComponent" :clusterId="clusterId" :depType="depType" />
            </div>
            <div v-else class="flex flex-col items-center justify-center py-12">
              <div class="mb-4">
                <div class="w-10 h-10 border-4 border-blue-200 border-t-blue-500 rounded-full animate-spin"></div>
              </div>
              <p class="text-gray-600">正在加载配置向导...</p>
            </div>
          </div>
        </DialogPanel>
      </div>
    </Dialog>

    <!-- 授权集群模态框 -->
    <Dialog :open="authModalVisible" @close="handleAuthModalClose" class="relative z-50">
      <div class="fixed inset-0 bg-black/30 backdrop-blur-sm" aria-hidden="true" />
      <div class="fixed inset-0 flex items-center justify-center p-4">
        <DialogPanel class="w-full max-w-md rounded-xl bg-white shadow-xl overflow-hidden">
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
        <DialogPanel class="w-full max-w-2xl rounded-xl bg-white shadow-xl overflow-hidden">
            <AddColony 
              v-if="editModalVisible" 
              :detail="currentEditObj || {}" 
              :callBack="handleEditComplete" 
              @cancel="handleEditModalClose"
              @success="handleEditComplete"
              ref="addColonyForm"
            />
        </DialogPanel>
      </div>
    </Dialog>
    
    <!-- 删除确认对话框 -->
    <Dialog :open="deleteModalVisible" @close="hideDeleteModal" class="relative z-50">
      <div class="fixed inset-0 bg-black/30 backdrop-blur-sm" aria-hidden="true" />
      <div class="fixed inset-0 flex items-center justify-center p-4">
        <DialogPanel class="w-full max-w-xs rounded-xl bg-white p-6 text-center shadow-xl">
          <DialogTitle class="text-lg font-medium text-gray-900 mb-4">
            确认删除
          </DialogTitle>
          <p class="text-sm text-gray-600 mb-6">
            确认删除当前{{ currentEditObj?.clusterName || '' }}集群？
          </p>
          <div class="flex justify-center space-x-4">
            <button 
              @click="confirmDelete"
              class="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
            >
              确定
            </button>
            <button
              @click="hideDeleteModal"
              class="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:border-gray-400 transition-colors"
            >
              取消
            </button>
          </div>
        </DialogPanel>
      </div>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, defineAsyncComponent, h } from 'vue'
import { useRouter } from 'vue-router'
import SvgIcon from '@/components/SvgIcon.vue'
import { Dialog, DialogPanel, DialogTitle, Menu, MenuButton, MenuItems, MenuItem } from '@headlessui/vue'

// 导入子组件
import AddColony from './components/AddColony.vue'
import AuthCluster from './components/AuthCluster.vue'

// 异步加载Steps组件
const stepsComponent = defineAsyncComponent(() => 
  import('../../components/steps').catch(() => {
    console.error('Failed to load Steps component')
    return { render: () => h('div', 'Failed to load component') }
  })
)

// 导入API和请求工具
import { axiosPost, axiosGet, axiosJsonPost } from '@/utils/request'
import { changeRouter } from '@/utils/changeRouter'
import { useToast } from '@/composables/useToast'
import { useUserStore } from '@/stores/user'
import { useSettingsStore } from '@/stores/settings'
import { errorHandler } from '@/composables/useErrorHandler'
import API_PATHS from '@/api/httpApi/apiPaths'
import * as clusterApi from '@/api/httpApi/cluster'

// 获取store和toast功能
const userStore = useUserStore()
const settingsStore = useSettingsStore()
const { toast } = useToast()

// 组件状态
const router = useRouter()
const dataSource = ref([])
const editModalVisible = ref(false)
const currentEditObj = ref(null)
const addColonyForm = ref(null)
const authModalVisible = ref(false)
const currentClusterForAuth = ref(null)
const visible = ref(false)
const clusterId = ref('')
const depType = ref('')
const stepsComponentLoaded = ref(false)
const deleteModalVisible = ref(false)
const clusterToDelete = ref(null)

// 计算属性
const user = computed(() => userStore.user)

// 过滤掉添加集群的占位项
const filteredDataSource = computed(() => {
  return dataSource.value.filter(item => !item.add)
})

// 生命周期钩子
onMounted(() => {
  getColonyList()
  
  // 尝试预加载Steps组件
  try {
    import('@/components/steps').then(() => {
      stepsComponentLoaded.value = true
    }).catch(err => {
      console.error('Failed to preload Steps component:', err)
    })
  } catch (err) {
    console.error('Error during Steps component import:', err)
  }
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
  // 使用错误处理包装API调用
  const result = await errorHandler.withErrorHandling(
    async () => {
      // 首先尝试使用API模块
      try {
        const res = await clusterApi.getClusterList({})
        if (res && res.code === 200) {
          dataSource.value = res.data || []
          processClusterData()
          return res
        }
        throw new Error('获取集群数据失败')
      } catch (primaryError) {
        // 如果是认证错误，直接抛出
        if (primaryError.isAuthError) {
          throw primaryError
        }
        
        // 尝试备用方法
        console.log('主API调用失败，尝试备用方法')
        const res = await axiosPost(API_PATHS.getColonyList, {})
        if (res && res.code === 200) {
          dataSource.value = res.data || []
          processClusterData()
          return res
        }
        throw new Error('获取集群列表失败')
      }
    },
    {
      defaultMessage: '无法加载集群数据，请检查网络连接',
      redirectOnAuthError: false // 不跳转登录页
    }
  )
  
  return result
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
  if (row.clusterStateCode === 1) {
    toast.warning('当前集群未配置完成，无法访问')
    return
  }
  
  try {
    // 使用错误处理
    const res = await errorHandler.withErrorHandling(
      async () => {
        return await axiosPost(API_PATHS.getServiceListByCluster, {
          clusterId: row.id,
        })
      },
      { defaultMessage: '进入集群失败' }
    )
    
    if (res && res.data) {
      changeRouter(res.data, row.id, router)
      router.push("/service-manage")
    }
  } catch (err) {
    console.error('进入集群失败:', err)
    // 错误已通过errorHandler处理
  }
}

// 显示删除确认对话框
const delectColony = (obj) => {
  currentEditObj.value = obj
  clusterToDelete.value = obj
  deleteModalVisible.value = true
}

// 隐藏删除确认对话框
const hideDeleteModal = () => {
  deleteModalVisible.value = false
  setTimeout(() => {
    clusterToDelete.value = null
  }, 300)
}

// 确认删除操作
const confirmDelete = async () => {
  if (!clusterToDelete.value) return
  
  // 使用错误处理包装API调用
  const result = await errorHandler.withErrorHandling(
    async () => {
      // 首先尝试使用API库方式
      try {
        const url = `${API_PATHS.deleteColony}?clusterId=${clusterToDelete.value.id}`
        const params = JSON.stringify([clusterToDelete.value.id])
        
        const res = await axiosJsonPost(url, params)
        if (res && res.code === 200) {
          toast.success('删除成功')
          getColonyList()
          hideDeleteModal()
          return res
        }
        throw new Error(res?.msg || '删除失败')
      } catch (err) {
        // 如果是认证错误，直接抛出
        if (err.isAuthError) throw err
        
        // 回退方式
        console.log('使用回退方式删除集群')
        const res = await axiosPost(API_PATHS.deleteColony, { id: clusterToDelete.value.id })
        if (res && res.code === 200) {
          toast.success('删除成功')
          getColonyList()
          hideDeleteModal()
          return res
        }
        throw new Error(res?.msg || '删除失败')
      }
    },
    { defaultMessage: '删除集群失败' }
  )
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
  if (row.clusterStateCode === 2) {
    toast.warning('集群正在运行中，无法进行配置')
    return
  }
  
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

// 获取集群类型文本
const getClusterTypeText = (depType) => {
  switch(depType) {
    case 'PVM': return '裸金属/虚拟机'
    case 'Kubernetes': return 'Kubernetes'
    default: return '未知'
  }
}
</script>

<style>
.shadow-card {
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06);
}
</style>
