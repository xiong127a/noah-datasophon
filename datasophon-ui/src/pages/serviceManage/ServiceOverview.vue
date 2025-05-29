<template>
  <div class="service-overview">
    <!-- 仪表盘内容 -->
    <div class="service-dashboard">
      <OverViewComponent :dashboardUrl="dashboardUrl" />
    </div>
  </div>
</template>

<script>
import { mapState } from 'vuex';
// 引入总览组件
const OverViewComponent = () => import("@/components/overview");

export default {
  name: "ServiceOverview",
  components: { OverViewComponent },
  computed: {
    ...mapState('setting', ['dashboardUrl'])
  }
};
</script>

<style lang="less" scoped>
.service-overview {
  padding: 0;
  height: 100%;
  
  .service-dashboard {
    position: relative;
    height: 100vh;
    background: #fff;
    border-radius: 0;
    box-shadow: none;
    overflow: hidden;
  }
}
</style> 