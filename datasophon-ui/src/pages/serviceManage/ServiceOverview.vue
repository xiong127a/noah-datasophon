<template>
  <div class="service-overview">
    <!-- 页面标题 -->
    <div class="overview-title">
      <h1>大数据基础平台总览</h1>
    </div>
    
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
  name: "BigDataPlatformOverview",
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
  
  .overview-title {
    padding: 16px 20px;
    border-bottom: 1px solid #e8e8e8;
    background: #fff;
    
    h1 {
      margin: 0;
      font-size: 20px;
      font-weight: 500;
      color: #333;
    }
  }
  
  .service-dashboard {
    position: relative;
    height: calc(100vh - 140px); /* 减去标题和顶部导航的高度 */
    background: #fff;
    border-radius: 0;
    box-shadow: none;
    overflow: hidden;
  }
}
</style> 