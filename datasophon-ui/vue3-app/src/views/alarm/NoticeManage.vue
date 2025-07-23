<template>
  <div class="notice-manage-container">
    <h1 class="page-title">通知组</h1>
    <div class="notice-manage-content">
      <p>通知组内容将在这里显示</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'NoticeManage'
}
</script>

<style scoped>
.notice-manage-container {
  padding: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 500;
  margin-bottom: 20px;
}

.notice-manage-content {
  background: #fff;
  padding: 24px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
</style> 