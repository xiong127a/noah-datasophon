<!--
 * @describe: Redis哨兵模式连接信息组件
-->
<template>
  <ConnectionInfoPanel
    :service-id="serviceId"
    :service-name="serviceName"
    :connection-info="connectionInfo"
    :titles="{
      javaTitle: 'Redis Sentinel Jedis连接示例',
      pythonTitle: 'Redis Sentinel Python连接示例',
      commandTitle: 'Redis Sentinel常用命令'
    }"
    :file-names="{
      javaFileName: 'RedisSentinelClient.java',
      pythonFileName: 'redis_sentinel_example.py'
    }"
    @loading-change="handleLoadingChange"
    @connection-loaded="handleConnectionLoaded"
  />
</template>

<script>
import ConnectionInfoPanel from '../components/ConnectionInfoPanel';

export default {
  name: "RedisSentinelConnectInfo",
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
      console.log('Redis Sentinel连接信息加载完成:', data);
    }
  }
};
</script>

<style lang="less" scoped>
/* 由于使用了公共组件，这里不需要额外样式 */
</style> 