<template>
  <div class="user-manage-container">
    <h1 class="page-title">用户管理</h1>
    <div class="user-manage-content">
      <p>用户管理内容将在这里显示</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'UserManage'
}
</script>

<style scoped>
.user-manage-container {
  padding: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 500;
  margin-bottom: 20px;
}

.user-manage-content {
  background: #fff;
  padding: 24px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
</style> 