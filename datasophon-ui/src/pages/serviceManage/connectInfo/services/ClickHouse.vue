<template>
  <ConnectionInfoPanel
    :loading="loading"
    :service-id="serviceId"
    :service-name="serviceName"
    :connection-info="connectionInfo"
    :title="title"
    @reload="handleReload"
  />
</template>

<script>
import { defineComponent, ref, onMounted, toRefs } from 'vue'
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
    connectionInfo: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  emits: ['refresh-request'],
  setup(props, { emit }) {
    const title = ref('ClickHouse连接信息')

    // 处理刷新按钮点击，向父组件发送刷新请求事件
    const handleReload = () => {
      console.log('ClickHouse组件请求刷新数据');
      emit('refresh-request');
    }

    onMounted(() => {
      console.log('ClickHouse组件已挂载，使用父组件传递的数据');
    })

    return {
      title,
      handleReload
    }
  }
})
</script>

<style scoped>
/* 使用公共组件，无需额外样式 */
</style> 