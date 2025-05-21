<template>
  <div class="user-guide">
    <markdown-doc-viewer
      :service-id="serviceId"
      doc-type="guide"
      empty-text="暂无用户指南信息"
    />
  </div>
</template>

<script>
import MarkdownDocViewer from './MarkdownDocViewer.vue';

export default {
  name: 'UserGuide',
  components: {
    MarkdownDocViewer
  },
  props: {
    serviceId: {
      type: String,
      required: true
    }
  }
}
</script>

<style lang="less" scoped>
.user-guide {
  width: 100%;
}
</style> 