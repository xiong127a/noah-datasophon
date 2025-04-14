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
    }
  },
  setup(props) {
    const loading = ref(false)
    const connectionInfo = ref(null)
    const title = ref('ClickHouse连接信息')

    const loadConnectionInfo = async () => {
      loading.value = true
      try {
        const res = await window.$axiosPost(global.API.getConnectionInfo, {
          serviceInstanceId: props.serviceId
        })
        if (res.code === 200) {
          connectionInfo.value = res.data
        }
      } finally {
        loading.value = false
      }
    }

    onMounted(() => {
      loadConnectionInfo()
    })

    return {
      loading,
      connectionInfo,
      title,
      loadConnectionInfo
    }
  }
})
</script>

<style scoped>
/* 使用公共组件，无需额外样式 */
</style> 