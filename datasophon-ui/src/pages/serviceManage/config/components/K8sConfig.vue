<template>
  <div class="k8s-config-container">
    <a-empty description="K8s配置待实现" />
  </div>
</template>

<script>
import { defineComponent } from 'vue';

export default defineComponent({
  name: 'K8sConfig',
  props: {
    serviceId: {
      type: [Number, String],
      required: true
    },
    serviceName: {
      type: String,
      required: true,
      default: '未知服务'
    }
  },
  setup() {
    return {};
  }
});
</script>

<style scoped>
.k8s-config-container {
  padding: 24px;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}
</style> 