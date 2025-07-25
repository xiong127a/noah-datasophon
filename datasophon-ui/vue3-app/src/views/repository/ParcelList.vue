<template>
  <div class="parcel-management">
    <!-- 页面头部 -->
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900">集群存储库</h1>
      <p class="text-gray-600 mt-2">管理和配置存储库，查看可用的组件包</p>
    </div>

    <!-- 存储库列表 -->
    <div class="space-y-6">
      <!-- 内置存储库 -->
      <div class="card">
        <div class="flex justify-between items-center mb-4">
          <h2 class="text-xl font-semibold text-gray-800">内置存储库</h2>
          <div class="system-default-tag">系统默认</div>
        </div>
        
        <div class="repo-content">
          <div class="flex items-center">
            <div class="mr-4">
              <i class="system-folder-icon"></i>
            </div>
            <div class="flex-1">
              <div class="text-base font-medium text-gray-800">file:///opt/datasophon/DDP/packages</div>
              <div class="flex items-center mt-1">
                <span class="status-dot success"></span>
                <span class="text-sm text-gray-600">已连接</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 第三方存储库 -->
      <div class="card">
        <div class="flex justify-between items-center mb-4">
          <h2 class="text-xl font-semibold text-gray-800">第三方存储库</h2>
          <button 
            @click="addNewRepo" 
            class="btn btn-primary flex items-center"
          >
            <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            添加存储库
          </button>
        </div>
        
        <div v-if="parcelList.length === 0" class="py-12 flex flex-col items-center justify-center bg-gray-50 rounded-lg">
          <svg class="w-16 h-16 text-gray-400 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <h3 class="text-lg font-medium text-gray-500 mb-2">暂无第三方存储库</h3>
          <p class="text-gray-500 mb-4">添加外部存储库以获取更多组件</p>
          <button 
            @click="addNewRepo" 
            class="btn btn-primary"
          >
            添加第一个存储库
          </button>
        </div>

        <div v-else class="space-y-4">
          <div 
            v-for="parcel in parcelList" 
            :key="parcel.parcelId" 
            class="bg-white border border-gray-100 rounded-lg overflow-hidden shadow-sm"
          >
            <div class="bg-gray-50 px-4 py-3 border-b border-gray-100 flex justify-between items-center">
              <div class="flex items-center">
                <i class="cloud-icon"></i>
                <span class="text-base font-medium text-gray-800 ml-2">{{ parcel.parcelName }}</span>
              </div>
              <button 
                @click="removeRepo(parcel)" 
                class="delete-button"
              >
                <i class="trash-icon"></i>
                <span>删除</span>
              </button>
            </div>

            <div class="p-4">
              <div class="mb-4">
                <div class="flex">
                  <input
                    v-model="parcel.parcelPath"
                    type="text"
                    placeholder="请输入存储库URL地址"
                    class="flex-1 border border-gray-200 rounded-l-lg px-4 py-2 focus:ring-2 focus:ring-primary focus:border-transparent"
                  />
                  <button 
                    @click="onSearch(parcel)"
                    class="btn btn-primary rounded-l-none"
                  >
                    解析
                  </button>
                </div>
              </div>

              <!-- 组件列表 -->
              <div v-if="parcel.components && parcel.components.length > 0" class="mt-6">
                <div class="section-title">可用组件</div>
                <div class="space-y-3 mt-4">
                  <div 
                    v-for="comp in parcel.components" 
                    :key="comp.name" 
                    class="bg-gray-50 rounded-lg p-4"
                  >
                    <div class="flex items-start">
                      <div class="mr-3">
                        <i class="app-icon"></i>
                      </div>
                      <div class="flex-1">
                        <div class="text-base font-medium text-gray-800">{{ comp.label }}</div>
                        <div class="flex items-center mt-1">
                          <span class="px-2 py-0.5 bg-gray-100 text-gray-700 text-xs rounded-md">版本: {{ comp.version }}</span>
                        </div>
                        <div class="text-sm text-gray-600 mt-1">{{ comp.description || '暂无描述' }}</div>
                      </div>
                      <div>
                        <button 
                          v-if="comp.state == undefined" 
                          class="btn btn-primary"
                          @click="handleDownload(comp, parcel.parcelPath)"
                          :disabled="comp.state === 'executing' && comp.step === 'download'"
                        >
                          <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                          </svg>
                          下载
                        </button>
                        <button 
                          v-else-if="comp.state === 'success' && comp.step === 'download'"
                          class="btn btn-success"
                          @click="handleInstall(comp, parcel.parcelPath)"
                          :disabled="comp.state === 'executing' && comp.step === 'install'"
                        >
                          <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                          </svg>
                          安装
                        </button>
                        <div 
                          v-else-if="comp.state === 'success' && comp.step === 'install'" 
                          class="installed-tag"
                        >
                          <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                          </svg>
                          已安装
                        </div>
                      </div>
                    </div>
                    
                    <!-- 进度条 -->
                    <div v-if="comp.state !== undefined" class="mt-3">
                      <div class="relative w-full h-2 bg-gray-100 rounded-full overflow-hidden">
                        <div 
                          class="absolute top-0 left-0 h-full transition-all duration-300 rounded-full"
                          :class="{
                            'bg-primary': comp.state === 'executing',
                            'bg-success': comp.state === 'success',
                            'bg-danger': comp.state === 'fail'
                          }"
                          :style="{ width: comp.process + '%' }"
                        ></div>
                      </div>
                      <div class="text-xs text-gray-600 mt-1">
                        {{ formatState(comp.process, comp) }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 确认删除弹窗 -->
    <div 
      v-if="confirmDialog.visible"
      class="fixed inset-0 flex items-center justify-center z-50"
    >
      <div 
        class="absolute inset-0 bg-black bg-opacity-50"
        @click="confirmDialog.visible = false"
      ></div>
      <div class="relative bg-white rounded-xl shadow-2xl max-w-md w-full p-6">
        <div class="text-center mb-6">
          <div class="w-12 h-12 bg-red-100 rounded-full mx-auto mb-4 flex items-center justify-center">
            <svg class="w-6 h-6 text-danger" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
          <h3 class="text-xl font-semibold text-gray-900">确认删除</h3>
          <p class="text-gray-600 mt-2">
            确定要删除存储库 "{{ confirmDialog.parcelName }}" 吗？
          </p>
        </div>
        <div class="flex justify-center space-x-4">
          <button 
            @click="confirmDialog.visible = false" 
            class="btn bg-gray-100 text-gray-800 hover:bg-gray-200 px-6"
          >
            取消
          </button>
          <button 
            @click="confirmDeleteRepo" 
            class="btn btn-danger px-6"
          >
            确定
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useToast } from '@/composables/useToast'
import { errorHandler } from '@/composables/useErrorHandler'
import * as parcelApi from '@/api/httpApi/parcel'

const { toast } = useToast()
const ddhParcelPath = ref("file:///opt/datasophon/DDP/packages")

interface Component {
  name: string
  label: string
  version: string
  description?: string
  state?: 'executing' | 'success' | 'fail'
  step?: 'download' | 'install'
  process?: number
  md5?: string
}

interface Parcel {
  parcelId: number | string
  parcelName: string
  parcelPath: string
  parcelFit?: number
  frame?: string
  components?: Component[]
}

// 存储库列表
const parcelList = ref<Parcel[]>([])

// 确认对话框
const confirmDialog = reactive({
  visible: false,
  parcelId: null as number | string | null,
  parcelName: ''
})

// 添加新的存储库
const addNewRepo = () => {
  const newRepo: Parcel = {
    parcelId: Date.now(),
    parcelName: `第三方存储库 ${parcelList.value.length + 1}`,
    parcelPath: '',
    parcelFit: 1,
    frame: 'DDP-1.0.0',
    components: []
  }
  parcelList.value.push(newRepo)
}

// 打开删除确认对话框
const removeRepo = (parcel: Parcel) => {
  confirmDialog.visible = true
  confirmDialog.parcelId = parcel.parcelId
  confirmDialog.parcelName = parcel.parcelName
}

// 确认删除
const confirmDeleteRepo = () => {
  if (confirmDialog.parcelId) {
    const index = parcelList.value.findIndex(p => p.parcelId === confirmDialog.parcelId)
    if (index > -1) {
      parcelList.value.splice(index, 1)
    }
  }
  confirmDialog.visible = false
}

// 获取存储库列表
onMounted(async () => {
  getParcelList()
})

// 获取存储库列表
const getParcelList = async () => {
  try {
    const res = await errorHandler.withErrorHandling(
      async () => await parcelApi.getParcelList({}),
      { defaultMessage: '获取存储库列表失败' }
    )

    if (res && res.code === 200 && res.data) {
      parcelList.value = res.data || []
    }
  } catch (error) {
    console.error('获取存储库列表失败:', error)
  }
}

// 解析存储库
const onSearch = async (parcel: Parcel) => {
  if (!parcel.parcelPath) {
    toast.warning('请输入存储库地址')
    return
  }
  
  try {
    const res = await errorHandler.withErrorHandling(
      async () => await parcelApi.getParcelParse({ url: parcel.parcelPath }),
      { defaultMessage: '解析存储库失败' }
    )

    if (res && res.code === 200 && res.data) {
      parcel.components = res.data.components
    }
  } catch (error) {
    console.error('解析存储库失败:', error)
  }
}

// 下载组件
const handleDownload = async (comp: Component, url: string) => {
  try {
    const res = await errorHandler.withErrorHandling(
      async () => await parcelApi.downloadComponent({ url, parcelName: comp.name }),
      { defaultMessage: '下载组件失败' }
    )

    if (res && res.code === 200 && res.data) {
      comp.md5 = res.data.md5
      comp.process = (res.data.process * 100)
      comp.state = res.data.state
      comp.step = res.data.step

      // 开始监控任务进度
      viewTaskLog(comp)
    }
  } catch (error) {
    console.error('下载组件失败:', error)
  }
}

// 安装组件
const handleInstall = async (comp: Component, url: string) => {
  try {
    const res = await errorHandler.withErrorHandling(
      async () => await parcelApi.installComponent({ md5: comp.md5, packageName: comp.name }),
      { defaultMessage: '安装组件失败' }
    )

    if (res && res.code === 200 && res.data) {
      comp.process = (res.data.process * 100)
      comp.state = res.data.state
      comp.step = res.data.step

      // 开始监控任务进度
      viewTaskLog(comp)
    }
  } catch (error) {
    console.error('安装组件失败:', error)
  }
}

// 查看任务进度
const taskObj = ref<Component | null>(null)
const rolllogThread = ref<number | null>(null)

const viewTaskLog = async (comp: Component) => {
  taskObj.value = comp
  
  try {
    const response = await errorHandler.withErrorHandling(
      async () => await parcelApi.getParcelProcess({ md5: comp.md5 }),
      { defaultMessage: '获取任务进度失败', showToast: false }
    )

    if (response && response.code === 200 && response.data) {
      comp.state = response.data.state
      comp.process = (response.data.process * 100)

      // 清除之前的定时器
      if (rolllogThread.value) {
        clearTimeout(rolllogThread.value)
        rolllogThread.value = null
      }

      // 任务完成
      if (response.data.process >= 100 && response.data.state !== 'executing') {
        return
      }

      // 任务未完成，继续获取进度
      if (response.data.process <= 100 && response.data.state === 'executing') {
        rolllogThread.value = window.setTimeout(() => {
          viewTaskLog(comp)
        }, 3000)
      }
    }
  } catch (error) {
    console.error('获取任务进度失败:', error)
  }
}

// 格式化状态
const formatState = (percent: number, comp: Component) => {
  if (comp.step === 'download') {
    if (comp.state === 'executing') {
      return `正在下载: ${Math.round(percent)}%`
    } else if (comp.state === 'success') {
      return '下载成功'
    } else {
      return '下载失败'
    }
  } else if (comp.step === 'install') {
    if (comp.state === 'executing') {
      return `正在安装: ${Math.round(percent)}%`
    } else if (comp.state === 'success') {
      return '安装成功'
    } else {
      return '安装失败'
    }
  }
  
  return `${Math.round(percent)}%`
}
</script>

<style scoped>
.system-default-tag {
  display: inline-block;
  background-color: rgba(10, 132, 255, 0.1);
  color: #0A84FF;
  font-size: 0.75rem;
  font-weight: 500;
  padding: 0.25rem 0.5rem;
  border-radius: 0.25rem;
}

.system-folder-icon {
  display: block;
  width: 32px;
  height: 32px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath fill='%230A84FF' d='M19.5 21a3 3 0 003-3V6a3 3 0 00-3-3h-7.8a1.2 1.2 0 01-.856-.352L9.156 1.056A3.6 3.6 0 006.6 0H4.5a3 3 0 00-3 3v15a3 3 0 003 3h15zM3 6V3a1.5 1.5 0 011.5-1.5h2.1a2.1 2.1 0 011.5.614l1.688 1.592A2.7 2.7 0 0011.7 4.5H19.5A1.5 1.5 0 0121 6v1H3V6zm0 3h18v9a1.5 1.5 0 01-1.5 1.5h-15A1.5 1.5 0 013 18V9z'/%3E%3C/svg%3E");
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
}

.cloud-icon {
  display: block;
  width: 18px;
  height: 18px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath fill='%230A84FF' d='M6.352 17.79A7.79 7.79 0 1116.846 9.2h.944a5.79 5.79 0 010 11.58H6.352zm0-2h11.438a3.79 3.79 0 000-7.58h-1.78l-.176-.854a5.79 5.79 0 10-9.482 5.944l.338.49h-.338z'/%3E%3C/svg%3E");
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
}

.trash-icon {
  display: inline-block;
  width: 16px;
  height: 16px;
  margin-right: 4px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath fill='%23FF453A' d='M17 6h5v2h-2v13a1 1 0 01-1 1H5a1 1 0 01-1-1V8H2V6h5V3a1 1 0 011-1h8a1 1 0 011 1v3zm1 2H6v12h12V8zM9 4v2h6V4H9z'/%3E%3C/svg%3E");
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
}

.app-icon {
  display: block;
  width: 24px;
  height: 24px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath fill='%238E8E93' d='M10 13H4a1 1 0 01-1-1V4a1 1 0 011-1h6a1 1 0 011 1v8a1 1 0 01-1 1zm10 0h-6a1 1 0 01-1-1V4a1 1 0 011-1h6a1 1 0 011 1v8a1 1 0 01-1 1zM10 21H4a1 1 0 01-1-1v-4a1 1 0 011-1h6a1 1 0 011 1v4a1 1 0 01-1 1zm10 0h-6a1 1 0 01-1-1v-4a1 1 0 011-1h6a1 1 0 011 1v4a1 1 0 01-1 1z'/%3E%3C/svg%3E");
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
}

.delete-button {
  display: inline-flex;
  align-items: center;
  color: white;
  border: none;
  background: #FF453A;
  padding: 0.25rem 0.75rem;
  transition: all 0.2s ease;
  border-radius: 0.375rem;
  height: 1.75rem;
  font-size: 0.875rem;
}

.delete-button:hover {
  background: #ee281d;
  box-shadow: 0 2px 4px rgba(255, 69, 58, 0.3);
}

.section-title {
  position: relative;
  padding-left: 14px;
  font-size: 1rem;
  font-weight: 600;
  color: #1d1d1f;
}

.section-title:before {
  content: '';
  position: absolute;
  left: 0;
  top: 2px;
  bottom: 2px;
  width: 4px;
  background: #0A84FF;
  border-radius: 2px;
}

.installed-tag {
  display: inline-flex;
  align-items: center;
  background: rgba(48, 209, 88, 0.1);
  border-radius: 0.375rem;
  padding: 0.25rem 0.625rem;
  color: #30D158;
  font-size: 0.875rem;
  font-weight: 500;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 0.5rem;
  background-color: #30D158;
  box-shadow: 0 0 6px rgba(48, 209, 88, 0.5);
  display: inline-block;
}
</style> 