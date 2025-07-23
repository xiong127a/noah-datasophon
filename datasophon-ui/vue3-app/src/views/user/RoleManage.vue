<template>
  <div class="role-manage-container">
    <h1 class="page-title">角色管理</h1>
    <div class="role-manage-content">
      <p>角色管理内容将在这里显示</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'RoleManage'
}
</script>

<style scoped>
.role-manage-container {
  padding: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 500;
  margin-bottom: 20px;
}

.role-manage-content {
  background: #fff;
  padding: 24px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
</style>
