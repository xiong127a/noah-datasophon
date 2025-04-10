<!--
 * @describe: Kafka连接信息组件
-->
<template>
  <ConnectionInfoPanel
    :service-id="serviceId"
    :service-name="serviceName"
    :connection-info="connectionInfo"
    :titles="{
      javaTitle: 'Kafka Java客户端示例',
      pythonTitle: 'Kafka Python客户端示例',
      commandTitle: 'Kafka常用命令'
    }"
    :file-names="{
      javaFileName: 'KafkaProducerConsumer.java',
      pythonFileName: 'kafka_example.py'
    }"
    @loading-change="handleLoadingChange"
    @connection-loaded="handleConnectionLoaded"
  />
</template>

<script>
import ConnectionInfoPanel from '../components/ConnectionInfoPanel';

export default {
  name: "KafkaConnectInfo",
  components: {
    ConnectionInfoPanel
  },
  props: {
    serviceId: {
      type: [String, Number],
      required: true
    },
    serviceName: {
      type: String,
      default: ''
    },
    // 接收的连接信息对象
    connectionInfo: {
      type: Object,
      default: () => null
    }
  },
  methods: {
    // 处理加载状态变化
    handleLoadingChange(loading) {
      this.$emit('loading-change', loading);
    },
    
    // 处理连接信息加载完成
    handleConnectionLoaded(data) {
      console.log('Kafka连接信息加载完成:', data);
    }
  }
};
</script>

<style lang="less" scoped>
/* 由于使用了公共组件，这里不需要额外样式 */
</style> 