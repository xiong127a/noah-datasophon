<template>
  <div>
    <iframe v-if="reloadIframe" id="iframe" :src="dashboardUrl" class="overview-iframe" width="100%" frameborder="0"></iframe>
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
.overview-iframe {
  position: absolute;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  height: 100%;
}
</style>