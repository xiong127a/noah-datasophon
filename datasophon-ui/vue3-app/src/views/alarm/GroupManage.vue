<template>
  <div class="group-manage-container">
    <h1 class="page-title">告警组</h1>
    <div class="group-manage-content">
      <p>告警组内容将在这里显示</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'GroupManage'
}
</script>

<style scoped>
.group-manage-container {
  padding: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 500;
  margin-bottom: 20px;
}

.group-manage-content {
  background: #fff;
  padding: 24px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
</style>
