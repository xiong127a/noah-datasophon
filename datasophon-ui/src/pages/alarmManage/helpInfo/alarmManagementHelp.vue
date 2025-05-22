<template>
  <div class="alarm-management-help">
    <a-card class="card-shadow">
      <markdown-doc-viewer
        service-id="-991"
        doc-type="guide"
        empty-text="暂无告警管理系统使用帮助信息"
      />
    </a-card>
  </div>
</template>

<script>
import MarkdownDocViewer from '../../serviceManage/helpInfo/MarkdownDocViewer.vue';

export default {
  name: 'AlarmManagementHelp',
  components: {
    MarkdownDocViewer
  }
}
</script>

<style lang="less">
.alarm-management-help {
  width: 100%;
}
</style> 