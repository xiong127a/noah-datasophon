<template>
  <!-- 移除Dialog/TransitionChild，因为父组件已提供Dialog结构 -->
  <div class="w-full">
    <!-- 表单容器 -->
    <div class="w-full bg-white overflow-hidden rounded-xl relative">
      <!-- 表单头部 -->
      <div class="sticky top-0 z-10 text-center py-6 bg-gradient-to-r from-blue-500 to-blue-700 relative overflow-hidden">
        <!-- 闪光效果 -->
        <div class="absolute -top-1/2 -left-1/2 w-[200%] h-[200%] bg-radial-shine opacity-70 transform -rotate-30 pointer-events-none"></div>
        
        <h1 class="text-xl font-semibold text-white mb-2.5 relative drop-shadow-sm font-[system-ui]">
          {{ isEdit ? '编辑集群' : '创建新集群' }}
        </h1>
        <p class="text-sm text-white/95 m-0 font-normal relative">
          {{ isEdit ? '修改集群配置信息' : '配置您的大数据平台集群信息' }}
        </p>
      </div>
      
      <!-- 表单内容 -->
      <div class="p-6 bg-gray-50 max-h-[calc(80vh-120px)] overflow-y-auto">
        <form @submit.prevent="handleSubmit" ref="formRef" class="space-y-6">
          <!-- 基本信息部分 -->
          <div class="bg-white rounded-xl p-5 shadow-sm hover:translate-y-[-2px] hover:shadow transition duration-300 ease-out">
            <div class="flex items-center mb-2.5">
              <div class="w-[18px] h-[18px] bg-gradient-to-b from-blue-400 to-blue-600 rounded-full mr-2.5 relative shadow-blue-400/30 shadow-sm">
                <div class="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-2 h-2 bg-white rounded-full"></div>
              </div>
              <h2 class="text-base font-semibold text-gray-800">基本信息</h2>
            </div>
            
            <p class="text-xs text-gray-500 mb-4 ml-7">设置集群的基本标识信息</p>
            
            <div class="flex gap-5">
              <!-- 集群名称 -->
              <div class="flex-1 max-w-[50%] sm:max-w-full">
                <label class="flex items-center gap-1 text-sm font-medium text-gray-700 mb-2">
                  集群名称
                  <FormFieldIndicator 
                    :dirty="v$.clusterName.$dirty"
                    :error="v$.clusterName.$error"
                    :value="formState.clusterName"
                    :required="true"
                  />
                </label>
                
                <input
                  v-model="formState.clusterName"
                  type="text"
                  placeholder="请输入集群名称"
                  maxlength="10"
                  @blur="v$.clusterName.$touch()"
                  class="w-full h-[38px] rounded-lg border border-gray-300 px-3 py-1 text-sm
                         transition duration-300 ease-out
                         focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20
                         hover:border-blue-500 hover:shadow-sm hover:shadow-blue-500/5"
                  :class="{'border-red-500': v$.clusterName.$error, 'border-green-500': v$.clusterName.$dirty && !v$.clusterName.$error && formState.clusterName}"
                />
                <div v-if="v$.clusterName.$error" class="text-xs text-red-500 mt-1">
                  {{ v$.clusterName.$errors[0].$message }}
                </div>
              </div>
              
              <!-- 集群编码 -->
              <div class="flex-1 max-w-[50%] sm:max-w-full">
                <label class="flex items-center gap-1 text-sm font-medium text-gray-700 mb-2">
                  集群编码
                  <FormFieldIndicator 
                    :dirty="v$.clusterCode.$dirty"
                    :error="v$.clusterCode.$error"
                    :value="formState.clusterCode"
                    :required="true"
                  />
                </label>
                
                <input
                  v-model="formState.clusterCode"
                  type="text"
                  placeholder="请输入集群编码"
                  maxlength="10"
                  @blur="v$.clusterCode.$touch()"
                  class="w-full h-[38px] rounded-lg border border-gray-300 px-3 py-1 text-sm
                         transition duration-300 ease-out
                         focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20
                         hover:border-blue-500 hover:shadow-sm hover:shadow-blue-500/5"
                  :class="{'border-red-500': v$.clusterCode.$error, 'border-green-500': v$.clusterCode.$dirty && !v$.clusterCode.$error && formState.clusterCode}"
                />
                <div v-if="v$.clusterCode.$error" class="text-xs text-red-500 mt-1">
                  {{ v$.clusterCode.$errors[0].$message }}
                </div>
              </div>
            </div>
          </div>

          <!-- 集群框架部分 -->
          <div class="bg-white rounded-xl p-5 shadow-sm hover:translate-y-[-2px] hover:shadow transition duration-300 ease-out">
            <div class="flex items-center mb-2.5">
              <div class="w-[18px] h-[18px] bg-gradient-to-b from-blue-400 to-blue-600 rounded-full mr-2.5 relative shadow-blue-400/30 shadow-sm">
                <div class="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-2 h-2 bg-white rounded-full"></div>
              </div>
              <h2 class="text-base font-semibold text-gray-800">集群框架</h2>
            </div>
            
            <p class="text-xs text-gray-500 mb-4 ml-7">选择集群所使用的框架类型</p>
            
            <div class="relative w-full">
              <label class="flex items-center gap-1 text-sm font-medium text-gray-700 mb-2">
                集群框架
                <FormFieldIndicator 
                  :dirty="v$.clusterFrame.$dirty"
                  :error="v$.clusterFrame.$error"
                  :value="formState.clusterFrame"
                  :required="true"
                />
              </label>
              
              <Listbox v-model="formState.clusterFrame" @change="v$.clusterFrame.$touch()">
                <div class="relative mt-1">
                  <ListboxButton
                    class="w-full cursor-default rounded-xl bg-white py-2.5 pl-3.5 pr-10 text-left border border-gray-300 
                           focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 
                           hover:border-blue-400 transition duration-200 text-sm"
                    :class="{'border-red-500': v$.clusterFrame.$error, 'border-green-500': v$.clusterFrame.$dirty && !v$.clusterFrame.$error && formState.clusterFrame}"
                  >
                    <span class="block truncate">{{ formState.clusterFrame || '请选择集群框架' }}</span>
                    <span class="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-2">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-gray-400" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                        <path fill-rule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clip-rule="evenodd" />
                      </svg>
                    </span>
                  </ListboxButton>
                  <transition
                    enter="transition duration-200 ease-out"
                    enter-from="opacity-0 translate-y-1"
                    enter-to="opacity-100 translate-y-0"
                    leave="transition duration-150 ease-in"
                    leave-from="opacity-100 translate-y-0"
                    leave-to="opacity-0 translate-y-1"
                  >
                    <ListboxOptions
                      class="absolute z-50 mt-1.5 max-h-60 w-full overflow-auto rounded-xl bg-white py-2 text-sm 
                             shadow-lg ring-1 ring-black/5 focus:outline-none transform origin-top"
                    >
                      <ListboxOption
                        v-for="item in filteredFrameList"
                        :key="item.frameCode"
                        :value="item.frameCode"
                        v-slot="{ active, selected }"
                      >
                        <li
                          :class="[
                            active ? 'text-blue-900 bg-blue-50' : 'text-gray-900',
                            'cursor-pointer select-none relative py-2.5 pl-3.5 pr-9 hover:bg-blue-50/70'
                          ]"
                        >
                          <span
                            :class="[
                              selected ? 'font-medium' : 'font-normal',
                              'block truncate'
                            ]"
                          >
                            {{ item.frameCode }}
                          </span>
                          <span
                            v-if="selected"
                            :class="[
                              active ? 'text-blue-600' : 'text-blue-500',
                              'absolute inset-y-0 right-0 flex items-center pr-3'
                            ]"
                          >
                            <svg class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                              <path fill-rule="evenodd" d="M16.704 4.153a.75.75 0 01.143 1.052l-8 10.5a.75.75 0 01-1.127.075l-4.5-4.5a.75.75 0 011.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 011.05-.143z" clip-rule="evenodd" />
                            </svg>
                          </span>
                        </li>
                      </ListboxOption>
                    </ListboxOptions>
                  </transition>
                </div>
              </Listbox>
              
              <div v-if="v$.clusterFrame.$error" class="text-xs text-red-500 mt-1">
                {{ v$.clusterFrame.$errors[0].$message }}
              </div>
            </div>
          </div>
          
          <!-- 部署方式部分 -->
          <div class="bg-white rounded-xl p-5 shadow-sm hover:translate-y-[-2px] hover:shadow transition duration-300 ease-out">
            <div class="flex items-center mb-2.5">
              <div class="w-[18px] h-[18px] bg-gradient-to-b from-blue-400 to-blue-600 rounded-full mr-2.5 relative shadow-blue-400/30 shadow-sm">
                <div class="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-2 h-2 bg-white rounded-full"></div>
              </div>
              <h2 class="text-base font-semibold text-gray-800">部署方式</h2>
            </div>
            
            <div class="mb-2">
              <label class="flex items-center gap-1 text-sm font-medium text-gray-700 mb-2">
                部署方式
                <FormFieldIndicator 
                  :dirty="v$.depType.$dirty"
                  :error="v$.depType.$error"
                  :value="formState.depType"
                  :required="true"
                />
              </label>
            </div>
            
            <p class="text-xs text-gray-500 mb-4">选择集群部署方式（传统部署或Kubernetes容器化部署）</p>
            
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <!-- 传统部署选择 -->
              <div 
                @click="selectDepType('PVM')"
                :class="{ 'border-blue-500 bg-blue-50/50 shadow-md': formState.depType === 'PVM' }"
                class="relative flex items-center border rounded-xl p-4.5 cursor-pointer transition-all duration-300 hover:border-blue-400 hover:-translate-y-0.5 hover:shadow-sm"
              >
                <!-- 图标 -->
                <div class="w-12 h-12 flex items-center justify-center bg-gray-50 rounded-full mr-4 flex-shrink-0
                          transition duration-300 ease-out shadow-sm"
                     :class="{ 'bg-blue-100/70 shadow-blue-500/20': formState.depType === 'PVM' }">
                  <img src="@/assets/linux-tux.svg" alt="Linux" class="w-6 h-6">
                </div>
                
                <!-- 内容 -->
                <div class="flex-1">
                  <h3 class="text-[15px] font-semibold text-gray-800 mb-1.5 transition duration-300"
                      :class="{ 'text-blue-600': formState.depType === 'PVM' }">
                    传统部署
                  </h3>
                  <p class="text-xs text-gray-500 leading-snug">
                    传统部署，适合大规模稳定业务
                  </p>
                </div>
                
                <!-- 选中标记 -->
                <div v-if="formState.depType === 'PVM'"
                     class="absolute top-4 right-4 w-6 h-6 bg-blue-500 rounded-full flex items-center justify-center shadow-md shadow-blue-500/30">
                  <svg class="w-3.5 h-3.5 text-white" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M16.704 4.153a.75.75 0 01.143 1.052l-8 10.5a.75.75 0 01-1.127.075l-4.5-4.5a.75.75 0 011.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 011.05-.143z" clip-rule="evenodd" />
                  </svg>
                </div>
              </div>
              
              <!-- Kubernetes选择 -->
              <div 
                @click="selectDepType('Kubernetes')"
                :class="{ 'border-blue-500 bg-blue-50/50 shadow-md': formState.depType === 'Kubernetes' }"
                class="relative flex items-center border rounded-xl p-4.5 cursor-pointer transition-all duration-300 hover:border-blue-400 hover:-translate-y-0.5 hover:shadow-sm"
              >
                <!-- 图标 -->
                <div class="w-12 h-12 flex items-center justify-center bg-gray-50 rounded-full mr-4 flex-shrink-0
                          transition duration-300 ease-out shadow-sm"
                     :class="{ 'bg-blue-100/70 shadow-blue-500/20': formState.depType === 'Kubernetes' }">
                  <img src="@/assets/kubernetes-logo.svg" alt="Kubernetes" class="w-6 h-6">
                </div>
                
                <!-- 内容 -->
                <div class="flex-1">
                  <h3 class="text-[15px] font-semibold text-gray-800 mb-1.5 transition duration-300"
                      :class="{ 'text-blue-600': formState.depType === 'Kubernetes' }">
                    Kubernetes
                  </h3>
                  <p class="text-xs text-gray-500 leading-snug">
                    容器化部署，支持自动化和弹性伸缩
                  </p>
                </div>
                
                <!-- 选中标记 -->
                <div v-if="formState.depType === 'Kubernetes'"
                     class="absolute top-4 right-4 w-6 h-6 bg-blue-500 rounded-full flex items-center justify-center shadow-md shadow-blue-500/30">
                  <svg class="w-3.5 h-3.5 text-white" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M16.704 4.153a.75.75 0 01.143 1.052l-8 10.5a.75.75 0 01-1.127.075l-4.5-4.5a.75.75 0 011.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 011.05-.143z" clip-rule="evenodd" />
                  </svg>
                </div>
              </div>
            </div>
            
            <div v-if="v$.depType.$error" class="text-xs text-red-500 mt-1">
              {{ v$.depType.$errors[0].$message }}
            </div>
          </div>
        </form>
      </div>
      
      <!-- 按钮区域 -->
      <div class="flex justify-center py-5 px-7 bg-white border-t border-gray-100 gap-4 sticky bottom-0 shadow-inner">
        <button
          type="button"
          @click="handleSubmit"
          :disabled="loading"
          class="min-w-[120px] h-10 rounded-full text-white font-medium text-sm
                bg-gradient-to-r from-blue-500 to-blue-700 
                shadow-md shadow-blue-500/30 relative overflow-hidden
                transition-all duration-300 ease-out hover:-translate-y-0.5 hover:shadow-lg hover:shadow-blue-500/40
                focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:ring-offset-2
                active:translate-y-0 active:shadow-sm active:shadow-blue-500/30
                disabled:opacity-70 disabled:cursor-not-allowed"
        >
          <span class="relative z-10 flex items-center justify-center">
            <svg v-if="loading" class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            {{ isEdit ? '保存修改' : '创建集群' }}
          </span>
          <span class="absolute inset-0 bg-gradient-to-br from-white/30 to-transparent opacity-0 hover:opacity-100 transition-opacity duration-300"></span>
        </button>
        
        <button
          type="button"
          @click="formCancel"
          class="min-w-[120px] h-10 rounded-full text-gray-600 font-medium text-sm
                border border-gray-200 bg-white
                transition-all duration-300 ease-out hover:-translate-y-0.5 hover:border-blue-500 hover:text-blue-600 hover:shadow-sm
                focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:ring-offset-2
                active:translate-y-0"
        >
          取消
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, watch } from 'vue'
import { Dialog, DialogPanel, Listbox, ListboxButton, ListboxOptions, ListboxOption } from '@headlessui/vue'
import { useVuelidate } from '@vuelidate/core'
import { required, maxLength } from '@vuelidate/validators'
import { axiosPost, axiosJsonPost } from '@/utils/request'
import * as clusterApi from '@/api/httpApi/cluster'
import config from '@/config' // 导入配置文件
import API_PATHS from '@/api/httpApi/apiPaths' // 导入API路径
import FormFieldIndicator from '@/components/FormFieldIndicator.vue' // 导入字段验证指示器
import { useUserStore } from '@/stores/user' // 导入用户状态管理

// 获取用户状态
const userStore = useUserStore()

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
const emit = defineEmits(['cancel', 'success'])

// Refs
const formRef = ref(null)
const dialogVisible = ref(true)

// Reactive state
const loading = ref(false)
const frameList = ref([]) // 集群框架列表
const depTypeList = ['Kubernetes', 'PVM'] // 部署方式列表

// 表单状态
const formState = reactive({
  clusterName: '',
  clusterCode: '',
  clusterFrame: '',
  depType: ''
})

// 表单验证规则
const rules = {
  clusterName: { required, maxLength: maxLength(10) },
  clusterCode: { required, maxLength: maxLength(10) },
  clusterFrame: { required },
  depType: { required }
}

// 初始化验证
const v$ = useVuelidate(rules, formState)

// 计算属性
const isEdit = computed(() => {
  return JSON.stringify(props.detail) !== '{}'
})

// 过滤后的框架列表
const filteredFrameList = computed(() => {
  return frameList.value.filter(item => item.frameCode.length <= 10)
})

// 生命周期钩子
onMounted(() => {
  getFrameList()
})

// 监听器
watch(() => props.detail, (newVal) => {
  if (JSON.stringify(newVal) !== '{}') {
    // 编辑模式，设置表单值
    formState.clusterName = newVal.clusterName || ''
    formState.clusterCode = newVal.clusterCode || ''
    formState.clusterFrame = newVal.clusterFrame || ''
    formState.depType = newVal.depType || ''
    
    // 标记所有字段为已触摸，以便显示正确的验证状态
    setTimeout(() => {
      v$.value.$touch()
    }, 100)
  }
}, { immediate: true })

// 方法
const selectDepType = (type) => {
  // 在编辑模式下不允许更改部署方式
  if (isEdit.value) return
  
  formState.depType = type
  v$.value.depType.$touch() // 标记为已触摸以更新验证状态
}

const formCancel = () => {
  emit('cancel')
}

const handleSubmit = async () => {
  const isFormValid = await v$.value.$validate()
  if (isFormValid) {
    // 仅从userStore和localStorage获取用户信息，不主动调用API
    let currentUser = userStore.user
    
    // 如果userStore中没有用户信息，尝试从localStorage获取（作为备选）
    if (!currentUser || !Object.keys(currentUser).length) {
      const userStr = localStorage.getItem(config.userKey)
      currentUser = userStr ? JSON.parse(userStr) : null
      
      // 如果从localStorage获取到了用户信息，更新到store中
      if (currentUser && Object.keys(currentUser).length) {
        userStore.setUser(currentUser)
      }
    }
    
    // 如果还是没有找到用户信息，提示用户重新登录
    if (!currentUser || !currentUser.username) {
      alert('无法获取用户信息，请重新登录后再试')
      return
    }
    
    // 构建请求参数
    const params = {
      "clusterName": formState.clusterName,
      "clusterCode": formState.clusterCode,
      "clusterFrame": formState.clusterFrame,
      "depType": formState.depType,
    }
    
    // 添加创建者信息
    if (currentUser && currentUser.username) {
      // 设置createBy为当前用户名
      params.createBy = currentUser.username
      
      // 如果需要将当前用户添加为集群管理员，构造clusterManagerList
      if (currentUser.id) {
        // 构造符合List<UserInfoEntity>格式的数据结构
        params.clusterManagerList = [{
          id: currentUser.id // UserInfoEntity的id字段
        }]
      }
    }
    
    // 如果当前是编辑模式，添加集群ID
    if (isEdit.value) params.id = props.detail.id
    
    loading.value = true
    
    try {
      let res;
      if (isEdit.value) {
        res = await clusterApi.updateColony(params)
      } else {
        res = await clusterApi.saveColony(params)
      }
      loading.value = false
      
      if (res.code === 200) {
        alert('保存成功')
        // 触发成功事件
        emit('success')
        props.callBack()
      } else {
        alert(res.msg || '保存失败')
      }
    } catch (error) {
      loading.value = false
      alert('保存失败，请检查网络连接')
      console.error('保存集群失败:', error)
    }
  } else {
    // 表单验证失败
    alert('请完善表单信息')
  }
}

const getFrameList = async () => {
  try {
    // 直接调用API函数而不是将函数作为URL参数
    const res = await clusterApi.getFrameList({})
    if (res.code === 200) {
      frameList.value = res.data
    } else {
      alert(res.msg || '获取框架列表失败')
    }
  } catch (error) {
    console.error('获取框架列表失败:', error)
    alert('获取框架列表失败，请检查网络连接')
  }
}

// 初始化表单
const initForm = () => {
  // 重置表单状态
  if (!isEdit.value) {
    formState.clusterName = ''
    formState.clusterCode = ''
    formState.clusterFrame = ''
    formState.depType = ''
  }
  
  // 确保加载框架列表
  if (frameList.value.length === 0) {
    getFrameList()
  }
  
  // 重置验证状态
  v$.value.$reset()
}

// 重置表单
const resetForm = () => {
  formState.clusterName = ''
  formState.clusterCode = ''
  formState.clusterFrame = ''
  formState.depType = ''
  
  // 重置验证状态
  v$.value.$reset()
}

// 暴露给父组件的方法
defineExpose({
  initForm,
  resetForm
})
</script>

<style scoped>
/* 径向渐变背景用于闪光效果 */
.bg-radial-shine {
  background: radial-gradient(ellipse at center, rgba(255,255,255,0.4) 0%, rgba(255,255,255,0) 70%);
}

/* 集群框架下拉菜单的特定样式 */
:deep(.listbox-button) {
  border-radius: 0.75rem !important;
  padding-top: 0.625rem !important;
  padding-bottom: 0.625rem !important;
  transition: all 0.3s ease;
}

:deep(.listbox-options) {
  border-radius: 0.75rem !important;
  overflow: hidden;
  transform-origin: top;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
  margin-top: 0.375rem;
}

/* 旋转30度 */
.rotate-30 {
  transform: rotate(-30deg);
}
</style> 