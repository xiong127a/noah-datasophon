<template>
  <div class="log-manage-container">
    <h1 class="page-title">系统日志</h1>
    <div class="log-manage-content">
      <p>系统日志内容将在这里显示</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'LogManage'
}
</script>

<style scoped>
.log-manage-container {
  padding: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 500;
  margin-bottom: 20px;
}

.log-manage-content {
  background: #fff;
  padding: 24px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
</style> 