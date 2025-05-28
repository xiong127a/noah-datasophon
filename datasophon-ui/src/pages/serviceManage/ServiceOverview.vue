<template>
  <div class="service-overview">
    <div class="service-header">
      <div class="header-title">
        <span>集群总览</span>
      </div>
    </div>
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
  padding: 20px;
  
  .service-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding-bottom: 12px;
    border-bottom: 1px solid #e0e0e0;
    
    .header-title {
      font-size: 22px;
      font-weight: 600;
      color: #1976d2;
    }
  }
  
  .service-dashboard {
    position: relative;
    height: calc(100vh - 140px);
    background: #fff;
    border-radius: 4px;
    box-shadow: 0 1px 4px rgba(0,0,0,0.1);
    overflow: hidden;
  }
}
</style> 