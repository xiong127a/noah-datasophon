<template>
  <div class="overview-container">
    <iframe v-if="reloadIframe" id="iframe" :src="dashboardUrl" class="overview-iframe" frameborder="0"></iframe>
  </div>
</template>
<script>
export default {
  props: {
    dashboardUrl: String,
    reloadIframe: {
      type: Boolean,
      default: true
    }
  },

  data() {
    return {
      iframeSrc1:
        "http://172.31.96.15:3000/d/_4gf-qOZz/1-zong-lan?orgId=1&refresh=5s&fromType=et",
    };
  },

  computed: {
  },

  created() {
    console.log(this.dashboardUrl)
  }
};
</script>

<style lang="less" scoped>
.overview-container {
  position: absolute;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  height: 100%;
  overflow: visible !important;
  
  /* 隐藏滚动条 */
  &::-webkit-scrollbar {
    width: 0 !important;
    height: 0 !important;
    display: none !important;
  }
  
  scrollbar-width: none !important; /* Firefox */
  -ms-overflow-style: none !important; /* IE and Edge */
}

.overview-iframe {
  position: absolute;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  height: 100%;
  border: none;
  
  /* 隐藏滚动条 */
  &::-webkit-scrollbar {
    width: 0 !important;
    height: 0 !important;
    display: none !important;
  }
  
  scrollbar-width: none !important; /* Firefox */
  -ms-overflow-style: none !important; /* IE and Edge */
}
</style>