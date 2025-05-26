<template>
  <ConnectionInfoPanel
    :loading="loading"
    :service-id="serviceId"
    :service-name="serviceName"
    :connection-info="connectionInfo"
    :title="title"
    @reload="loadConnectionInfo"
  />
</template>

<script>
import { defineComponent, ref, onMounted } from 'vue'
import ConnectionInfoPanel from '../components/ConnectionInfoPanel.vue'

export default defineComponent({
  name: 'ClickHouse',
  components: {
    ConnectionInfoPanel
  },
  props: {
    serviceId: {
      type: [Number, String],
      required: true
    },
    serviceName: {
      type: String,
      required: true
    },
    // 添加connectionInfo属性，接收父组件传递的数据
    connectionInfo: {
      type: Object,
      default: null
    }
  },
  setup(props) {
    const loading = ref(false)
    // 移除本地的connectionInfo，直接使用props中的connectionInfo
    const title = ref('ClickHouse连接信息')

    // 保留loadConnectionInfo方法，但不再自动调用
    // 该方法现在仅用于手动刷新按钮
    const loadConnectionInfo = async () => {
      console.log('ClickHouse组件手动刷新请求');
      // 通知父组件重新加载数据
      // 这里发出一个事件而不是直接调用API
      loading.value = true;
      try {
        // 您可以在这里发出一个事件，让父组件重新加载数据
        // 例如：emit('refresh-request')
        // 现在暂时保留以前的逻辑，但实际上不应该直接调用API
        console.warn('应该通过父组件刷新数据，避免直接调用API');
      } finally {
        loading.value = false;
      }
    }

    // 移除onMounted中的API调用
    onMounted(() => {
      console.log('ClickHouse组件已挂载，使用父组件传递的数据');
    })

    return {
      loading,
      // 直接使用props中的connectionInfo，不返回本地变量
      // connectionInfo,
      title,
      loadConnectionInfo
    }
  }
})
</script>

<style scoped>
/* 使用公共组件，无需额外样式 */
</style> 