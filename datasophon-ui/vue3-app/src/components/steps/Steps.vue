<template>
  <div class="steps-component">
    <div class="py-6 px-8">
      <div class="flex items-center justify-between mb-8">
        <h2 class="text-xl font-medium text-gray-800">集群配置向导</h2>
        <div class="text-sm text-gray-500">
          集群ID: {{ clusterId }} | 部署类型: {{ depType }}
        </div>
      </div>
      
      <div class="steps-navigation mb-8">
        <div class="flex items-center space-x-0">
          <div
            v-for="(step, index) in steps"
            :key="index"
            class="step-item flex items-center"
          >
            <div 
              :class="[
                'step-number flex items-center justify-center h-8 w-8 rounded-full text-sm font-medium',
                currentStep > index 
                  ? 'bg-blue-500 text-white' 
                  : currentStep === index 
                    ? 'bg-blue-100 text-blue-600 border border-blue-500' 
                    : 'bg-gray-100 text-gray-500'
              ]"
            >
              <span v-if="currentStep <= index">{{ index + 1 }}</span>
              <svg v-else class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              </svg>
            </div>
            
            <div v-if="index < steps.length - 1" class="step-line flex-1 h-1" :class="currentStep > index ? 'bg-blue-500' : 'bg-gray-200'"></div>
            
            <div class="ml-2 text-sm" :class="currentStep === index ? 'text-blue-600 font-medium' : 'text-gray-500'">
              {{ step.title }}
            </div>
          </div>
        </div>
      </div>
      
      <div class="steps-content bg-white rounded-lg p-8 shadow-sm border border-gray-100">
        <div v-if="loading" class="flex flex-col items-center py-16">
          <div class="w-12 h-12 border-4 border-blue-200 border-t-blue-600 rounded-full animate-spin mb-4"></div>
          <p class="text-gray-600">加载配置信息中...</p>
        </div>
        
        <div v-else-if="error" class="flex flex-col items-center py-16 text-center">
          <div class="text-red-500 mb-4">
            <svg class="h-16 w-16 mx-auto" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
          <h3 class="text-lg font-medium text-gray-900 mb-2">加载配置信息失败</h3>
          <p class="text-gray-600 max-w-md mb-6">{{ error }}</p>
          <button 
            @click="loadClusterData" 
            class="px-4 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-600 transition-colors"
          >
            重试
          </button>
        </div>
        
        <div v-else class="step-content-area">
          <!-- Step 1: Service Selection -->
          <div v-if="currentStep === 0">
            <h3 class="text-lg font-medium text-gray-900 mb-4">选择服务</h3>
            <p class="text-gray-600 mb-6">请选择您需要部署的服务，系统将自动处理服务之间的依赖关系。</p>
            
            <!-- Placeholder for service selection -->
            <div class="p-4 bg-gray-50 rounded-md text-gray-500 text-center">
              服务选择组件 - 将在后续实现
            </div>
          </div>
          
          <!-- Step 2: Configuration -->
          <div v-if="currentStep === 1">
            <h3 class="text-lg font-medium text-gray-900 mb-4">服务配置</h3>
            <p class="text-gray-600 mb-6">请配置所选服务的相关参数，您可以使用系统推荐的默认配置或自定义配置。</p>
            
            <!-- Placeholder for configuration -->
            <div class="p-4 bg-gray-50 rounded-md text-gray-500 text-center">
              配置组件 - 将在后续实现
            </div>
          </div>
          
          <!-- Step 3: Verification -->
          <div v-if="currentStep === 2">
            <h3 class="text-lg font-medium text-gray-900 mb-4">配置验证</h3>
            <p class="text-gray-600 mb-6">系统将验证您的配置是否满足部署要求，请确认以下验证结果。</p>
            
            <!-- Placeholder for verification -->
            <div class="p-4 bg-gray-50 rounded-md text-gray-500 text-center">
              验证组件 - 将在后续实现
            </div>
          </div>
          
          <!-- Step 4: Deployment -->
          <div v-if="currentStep === 3">
            <h3 class="text-lg font-medium text-gray-900 mb-4">开始部署</h3>
            <p class="text-gray-600 mb-6">系统将根据您的配置开始部署服务，部署过程可能需要一些时间，请耐心等待。</p>
            
            <!-- Placeholder for deployment -->
            <div class="p-4 bg-gray-50 rounded-md text-gray-500 text-center">
              部署组件 - 将在后续实现
            </div>
          </div>
        </div>
      </div>
      
      <div class="flex justify-between mt-8">
        <button 
          v-if="currentStep > 0"
          @click="prevStep" 
          class="px-6 py-2 border border-gray-300 rounded-md text-gray-700 hover:border-blue-500 hover:text-blue-600 transition-colors"
        >
          上一步
        </button>
        <div v-else></div>
        
        <button 
          v-if="currentStep < steps.length - 1"
          @click="nextStep" 
          class="px-6 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-600 transition-colors"
        >
          下一步
        </button>
        <button 
          v-else
          @click="finishConfiguration" 
          class="px-6 py-2 bg-green-500 text-white rounded-md hover:bg-green-600 transition-colors"
        >
          完成配置
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { axiosPost } from '@/utils/request'
import API_PATHS from '@/api/httpApi/apiPaths'
import { useToast } from '@/composables/useToast'

export default {
  name: 'Steps',
  props: {
    clusterId: {
      type: [String, Number],
      required: true
    },
    depType: {
      type: String,
      required: true
    }
  },
  setup(props) {
    const { toast } = useToast()
    const loading = ref(true)
    const error = ref(null)
    const currentStep = ref(0)
    const steps = ref([
      { title: '选择服务' },
      { title: '服务配置' },
      { title: '配置验证' },
      { title: '开始部署' }
    ])
    const clusterConfig = ref({})

    // 加载集群配置数据
    const loadClusterData = async () => {
      loading.value = true
      error.value = null
      
      try {
        // 这里会调用真实API，但目前作为占位符实现
        // const res = await axiosPost(API_PATHS.getClusterServiceConfigs, {
        //   clusterId: props.clusterId
        // })
        
        // 模拟API调用
        await new Promise(resolve => setTimeout(resolve, 1000))
        
        // 模拟数据
        clusterConfig.value = {
          services: [
            { id: 1, name: 'HDFS', selected: false },
            { id: 2, name: 'YARN', selected: false },
            { id: 3, name: 'HIVE', selected: false }
          ],
          configs: {}
        }
        
        loading.value = false
      } catch (err) {
        console.error('Failed to load cluster data:', err)
        error.value = err.message || '加载集群配置失败，请稍后重试'
        loading.value = false
      }
    }
    
    // 下一步
    const nextStep = () => {
      if (currentStep.value < steps.value.length - 1) {
        currentStep.value++
      }
    }
    
    // 上一步
    const prevStep = () => {
      if (currentStep.value > 0) {
        currentStep.value--
      }
    }
    
    // 完成配置
    const finishConfiguration = async () => {
      try {
        // 模拟API调用
        await new Promise(resolve => setTimeout(resolve, 1000))
        
        toast.success('集群配置已保存，正在开始部署')
        
        // 在真实实现中，这里应该关闭对话框或进行其他操作
      } catch (err) {
        toast.error('配置保存失败：' + (err.message || '未知错误'))
      }
    }
    
    // 加载数据
    onMounted(() => {
      loadClusterData()
    })
    
    return {
      loading,
      error,
      currentStep,
      steps,
      clusterConfig,
      loadClusterData,
      nextStep,
      prevStep,
      finishConfiguration
    }
  }
}
</script>

<style scoped>
.steps-navigation {
  position: relative;
  padding: 0 8px;
}

.step-item {
  position: relative;
  flex: 1;
  min-width: 0;
  padding: 0 20px;
}

.step-item:first-child {
  padding-left: 0;
}

.step-item:last-child {
  padding-right: 0;
}

.step-line {
  height: 2px;
  margin: 0 4px;
  position: relative;
  top: 0;
}

.step-content-area {
  min-height: 300px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.animate-spin {
  animation: spin 1s linear infinite;
}
</style> 