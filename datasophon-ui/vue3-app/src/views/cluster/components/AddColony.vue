<template>
  <Dialog 
    as="div" 
    class="relative z-50"
    :open="dialogVisible"
    @close="formCancel"
  >
    <!-- 遮罩层 -->
    <TransitionChild
      as="template"
      enter="duration-300 ease-out"
      enter-from="opacity-0"
      enter-to="opacity-100"
      leave="duration-200 ease-in"
      leave-from="opacity-100"
      leave-to="opacity-0"
    >
      <div class="fixed inset-0 bg-black/30" aria-hidden="true" />
    </TransitionChild>

    <!-- 弹窗内容 -->
    <div class="fixed inset-0 flex items-center justify-center p-4">
      <TransitionChild
        as="template"
        enter="duration-300 ease-out"
        enter-from="opacity-0 scale-95"
        enter-to="opacity-100 scale-100"
        leave="duration-200 ease-in"
        leave-from="opacity-100 scale-100"
        leave-to="opacity-0 scale-95"
      >
        <DialogPanel class="w-full max-w-3xl transform overflow-hidden rounded-2xl bg-white shadow-xl transition-all">
          <div class="w-full">
            <!-- 表单容器 -->
            <div class="w-full bg-white overflow-hidden rounded-2xl relative">
              <!-- 表单头部 -->
              <div class="text-center py-6 bg-gradient-to-r from-blue-500 to-blue-700 relative overflow-hidden">
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
              <div class="p-6 bg-gray-50">
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
                          <span class="relative inline-block w-1.5 h-1.5 bg-red-500 rounded-full ml-1" 
                                :class="{'bg-green-500': formState.clusterName}">
                            <span class="absolute inset-0 rounded-full animate-ping" 
                                  :class="formState.clusterName ? 'bg-green-500/20' : 'bg-red-500/20'"></span>
                          </span>
                        </label>
                        
                        <input
                          v-model="formState.clusterName"
                          type="text"
                          placeholder="请输入集群名称"
                          maxlength="10"
                          class="w-full h-[38px] rounded-lg border border-gray-300 px-3 py-1 text-sm
                                 transition duration-300 ease-out
                                 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20
                                 hover:border-blue-500 hover:shadow-sm hover:shadow-blue-500/5"
                        />
                        <div v-if="v$.clusterName.$error" class="text-xs text-red-500 mt-1">
                          {{ v$.clusterName.$errors[0].$message }}
                        </div>
                      </div>
                      
                      <!-- 集群编码 -->
                      <div class="flex-1 max-w-[50%] sm:max-w-full">
                        <label class="flex items-center gap-1 text-sm font-medium text-gray-700 mb-2">
                          集群编码
                          <span class="relative inline-block w-1.5 h-1.5 bg-red-500 rounded-full ml-1" 
                                :class="{'bg-green-500': formState.clusterCode}">
                            <span class="absolute inset-0 rounded-full animate-ping" 
                                  :class="formState.clusterCode ? 'bg-green-500/20' : 'bg-red-500/20'"></span>
                          </span>
                        </label>
                        
                        <input
                          v-model="formState.clusterCode"
                          type="text"
                          placeholder="请输入集群编码"
                          maxlength="10"
                          :disabled="isEdit"
                          class="w-full h-[38px] rounded-lg border border-gray-300 px-3 py-1 text-sm
                                 transition duration-300 ease-out
                                 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20
                                 hover:border-blue-500 hover:shadow-sm hover:shadow-blue-500/5
                                 disabled:bg-gray-100 disabled:text-gray-500 disabled:cursor-not-allowed"
                        />
                        <div v-if="v$.clusterCode.$error" class="text-xs text-red-500 mt-1">
                          {{ v$.clusterCode.$errors[0].$message }}
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- 技术配置部分 -->
                  <div class="bg-white rounded-xl p-5 shadow-sm hover:translate-y-[-2px] hover:shadow transition duration-300 ease-out">
                    <div class="flex items-center mb-2.5">
                      <div class="w-[18px] h-[18px] bg-gradient-to-b from-blue-400 to-blue-600 rounded-full mr-2.5 relative shadow-blue-400/30 shadow-sm">
                        <div class="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-2 h-2 bg-white rounded-full"></div>
                      </div>
                      <h2 class="text-base font-semibold text-gray-800">技术配置</h2>
                    </div>
                    
                    <p class="text-xs text-gray-500 mb-4 ml-7">选择集群的技术框架和部署方式</p>
                    
                    <!-- 集群框架选择 -->
                    <div class="mb-6">
                      <label class="flex items-center gap-1 text-sm font-medium text-gray-700 mb-2">
                        集群框架
                        <span class="relative inline-block w-1.5 h-1.5 bg-red-500 rounded-full ml-1" 
                              :class="{'bg-green-500': formState.clusterFrame}">
                          <span class="absolute inset-0 rounded-full animate-ping" 
                                :class="formState.clusterFrame ? 'bg-green-500/20' : 'bg-red-500/20'"></span>
                        </span>
                      </label>
                      
                      <div class="relative max-w-[300px]">
                        <Listbox v-model="formState.clusterFrame" :disabled="isEdit">
                          <div class="relative">
                            <ListboxButton class="relative w-full h-[38px] cursor-default rounded-lg bg-white py-2 pl-3 pr-10 text-left border border-gray-300
                                         transition duration-300 ease-out
                                         focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20
                                         hover:border-blue-500 hover:shadow-sm hover:shadow-blue-500/5
                                         disabled:bg-gray-100 disabled:text-gray-500 disabled:cursor-not-allowed">
                              <span class="block truncate text-sm">
                                {{ formState.clusterFrame || '请选择集群框架' }}
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
                                  v-for="(item, index) in filteredFrameList"
                                  :key="index"
                                  :value="item.frameCode"
                                  v-slot="{ active, selected }"
                                >
                                  <li
                                    :class="[
                                      active ? 'bg-blue-100 text-blue-900' : 'text-gray-900',
                                      'relative cursor-default select-none py-2 pl-10 pr-4'
                                    ]"
                                  >
                                    <span :class="[selected ? 'font-medium' : 'font-normal', 'block truncate']">
                                      {{ item.frameCode }}
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
                        
                        <div v-if="v$.clusterFrame.$error" class="text-xs text-red-500 mt-1">
                          {{ v$.clusterFrame.$errors[0].$message }}
                        </div>
                      </div>
                    </div>
                    
                    <!-- 部署方式选择 -->
                    <div>
                      <label class="flex items-center gap-1 text-sm font-medium text-gray-700 mb-3">
                        集群部署方式
                        <span class="relative inline-block w-1.5 h-1.5 bg-red-500 rounded-full ml-1" 
                              :class="{'bg-green-500': formState.depType}">
                          <span class="absolute inset-0 rounded-full animate-ping" 
                                :class="formState.depType ? 'bg-green-500/20' : 'bg-red-500/20'"></span>
                        </span>
                      </label>
                      
                      <div class="flex flex-wrap gap-4">
                        <!-- 裸金属/虚拟机选项 -->
                        <div 
                          @click="selectDepType('PVM')"
                          :class="[
                            'flex-1 min-w-[250px] flex items-center p-5 border rounded-xl cursor-pointer relative overflow-hidden transition duration-300 ease-out',
                            formState.depType === 'PVM' 
                              ? 'border-blue-500 bg-blue-50/50 shadow-md shadow-blue-500/10' 
                              : 'border-gray-200 bg-white hover:border-blue-500 hover:-translate-y-0.5 hover:shadow-md'
                          ]"
                          :aria-disabled="isEdit"
                        >
                          <!-- 闪光效果 -->
                          <div class="absolute inset-0 bg-gradient-to-br from-white/80 to-transparent opacity-0 hover:opacity-50 transition-opacity"></div>
                          
                          <!-- 图标 -->
                          <div class="w-12 h-12 flex items-center justify-center bg-gray-50 rounded-full mr-4 flex-shrink-0
                                    transition duration-300 ease-out shadow-sm"
                               :class="{ 'bg-blue-100/50 shadow-blue-500/20': formState.depType === 'PVM' }">
                            <img src="@/assets/img/os-logos/linux-tux.svg" alt="Linux" class="w-6 h-6">
                          </div>
                          
                          <!-- 内容 -->
                          <div class="flex-1">
                            <h3 class="text-[15px] font-semibold text-gray-800 mb-1.5 transition duration-300"
                                :class="{ 'text-blue-600': formState.depType === 'PVM' }">
                              裸金属/虚拟机
                            </h3>
                            <p class="text-xs text-gray-500 leading-snug">
                              部署到Linux裸金属或虚拟机上
                            </p>
                          </div>
                          
                          <!-- 选中标记 -->
                          <div v-if="formState.depType === 'PVM'"
                               class="absolute top-4 right-4 w-5.5 h-5.5 bg-blue-500 rounded-full flex items-center justify-center shadow-md shadow-blue-500/30">
                            <svg class="w-3 h-3 text-white" viewBox="0 0 20 20" fill="currentColor">
                              <path fill-rule="evenodd" d="M16.704 4.153a.75.75 0 01.143 1.052l-8 10.5a.75.75 0 01-1.127.075l-4.5-4.5a.75.75 0 011.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 011.05-.143z" clip-rule="evenodd" />
                            </svg>
                          </div>
                        </div>
                        
                        <!-- Kubernetes选项 -->
                        <div 
                          @click="selectDepType('Kubernetes')"
                          :class="[
                            'flex-1 min-w-[250px] flex items-center p-5 border rounded-xl cursor-pointer relative overflow-hidden transition duration-300 ease-out',
                            formState.depType === 'Kubernetes' 
                              ? 'border-blue-500 bg-blue-50/50 shadow-md shadow-blue-500/10' 
                              : 'border-gray-200 bg-white hover:border-blue-500 hover:-translate-y-0.5 hover:shadow-md'
                          ]"
                          :aria-disabled="isEdit"
                        >
                          <!-- 闪光效果 -->
                          <div class="absolute inset-0 bg-gradient-to-br from-white/80 to-transparent opacity-0 hover:opacity-50 transition-opacity"></div>
                          
                          <!-- 图标 -->
                          <div class="w-12 h-12 flex items-center justify-center bg-gray-50 rounded-full mr-4 flex-shrink-0
                                    transition duration-300 ease-out shadow-sm"
                               :class="{ 'bg-blue-100/50 shadow-blue-500/20': formState.depType === 'Kubernetes' }">
                            <img src="@/assets/images/kubernetes-logo.svg" alt="Kubernetes" class="w-6 h-6">
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
                               class="absolute top-4 right-4 w-5.5 h-5.5 bg-blue-500 rounded-full flex items-center justify-center shadow-md shadow-blue-500/30">
                            <svg class="w-3 h-3 text-white" viewBox="0 0 20 20" fill="currentColor">
                              <path fill-rule="evenodd" d="M16.704 4.153a.75.75 0 01.143 1.052l-8 10.5a.75.75 0 01-1.127.075l-4.5-4.5a.75.75 0 011.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 011.05-.143z" clip-rule="evenodd" />
                            </svg>
                          </div>
                        </div>
                      </div>
                      
                      <div v-if="v$.depType.$error" class="text-xs text-red-500 mt-1">
                        {{ v$.depType.$errors[0].$message }}
                      </div>
                    </div>
                  </div>
                </form>
              </div>
              
              <!-- 按钮区域 -->
              <div class="flex justify-center py-5 px-7 bg-white border-t border-gray-100 gap-4">
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
        </DialogPanel>
      </TransitionChild>
    </div>
  </Dialog>
</template>

<script setup>
import { ref, computed, reactive, onMounted, watch } from 'vue'
import { Dialog, DialogPanel, TransitionChild, Listbox, ListboxButton, ListboxOptions, ListboxOption } from '@headlessui/vue'
import { useVuelidate } from '@vuelidate/core'
import { required, maxLength } from '@vuelidate/validators'
import { axiosPost, axiosJsonPost } from '@/utils/request'
import * as clusterApi from '@/api/httpApi/cluster'

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
  }
}, { immediate: true })

// 方法
const selectDepType = (type) => {
  // 在编辑模式下不允许更改部署方式
  if (isEdit.value) return
  
  formState.depType = type
}

const formCancel = () => {
  emit('cancel')
}

const handleSubmit = async () => {
  const isFormValid = await v$.value.$validate()
  if (isFormValid) {
    // 获取当前登录用户信息
    const userStr = localStorage.getItem(process.env.VUE_APP_USER_KEY)
    const currentUser = userStr ? JSON.parse(userStr) : null
    
    const params = {
      "clusterName": formState.clusterName,
      "clusterCode": formState.clusterCode,
      "clusterFrame": formState.clusterFrame,
      "depType": formState.depType,
    }
    
    // 添加创建者信息
    if (currentUser) {
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
    const ajaxApi = isEdit.value ? clusterApi.updateColony : clusterApi.saveColony
    
    try {
      const res = await axiosJsonPost(ajaxApi + (isEdit.value ? "?clusterId=" + props.detail.id : ""), params)
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
    }
  } else {
    // 表单验证失败
    alert('请完善表单信息')
  }
}

const getFrameList = async () => {
  try {
    const res = await axiosPost(clusterApi.getFrameList, {})
    if (res.code === 200) {
      frameList.value = res.data
    } else {
      alert(res.msg || '获取框架列表失败')
    }
  } catch (error) {
    alert('获取框架列表失败，请检查网络连接')
  }
}
</script>

<style scoped>
/* 径向闪光背景 */
.bg-radial-shine {
  background: radial-gradient(circle, rgba(255,255,255,0.3) 0%, rgba(255,255,255,0) 70%);
}

/* 脉冲动画 */
@keyframes ping {
  75%, 100% {
    transform: scale(2);
    opacity: 0;
  }
}
.animate-ping {
  animation: ping 2s cubic-bezier(0, 0, 0.2, 1) infinite;
}

/* 适配移动端 */
@media (max-width: 640px) {
  .sm\:max-w-full {
    max-width: 100% !important;
  }
}
</style> 