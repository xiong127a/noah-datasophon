<template>
  <div class="component-intro">
    <markdown-doc-viewer
      :service-id="serviceId"
      doc-type="component"
      empty-text="暂无组件介绍信息"
    />
  </div>
</template>

<script>
import MarkdownDocViewer from './MarkdownDocViewer.vue';

export default {
  name: 'ComponentIntro',
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
.component-intro {
  width: 100%;
}
</style> 