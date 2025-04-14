<!-- Doris 连接信息组件 -->
<template>
  <ConnectionInfoPanel
    :service-id="props.serviceId"
    :service-name="props.serviceName"
    :connection-info="connectionInfo"
    :java-title="'Java 连接 Doris 示例'"
    :python-title="'Python 连接 Doris 示例'"
    :command-title="'Doris 常用命令'"
    @loading-state-change="handleLoadingStateChange"
    @connection-info-loaded="handleConnectionInfoLoaded"
  />
</template>

<script>
import { ref, onMounted } from 'vue'
import ConnectionInfoPanel from '../components/ConnectionInfoPanel.vue'

export default {
  name: 'DorisConnectionInfo',
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
      default: 'Doris'
    }
  },
  setup(props) {
    const connectionInfo = ref(null)
    const isLoading = ref(false)

    // 监听加载状态变化
    const handleLoadingStateChange = (loading) => {
      isLoading.value = loading
    }

    // 处理连接信息加载完成事件
    const handleConnectionInfoLoaded = (info) => {
      connectionInfo.value = info
    }

    return {
      props,
      connectionInfo,
      isLoading,
      handleLoadingStateChange,
      handleConnectionInfoLoaded
    }
  }
}
</script>

<style scoped>
/* 使用公共组件，无需额外样式 */
</style> 