<template>
  <div class="rack-manage-container">
    <h1 class="page-title">机架管理</h1>
    <div class="rack-manage-content">
      <p>机架管理内容将在这里显示</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'RackManage'
}
</script>

<style scoped>
.rack-manage-container {
  padding: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 500;
  margin-bottom: 20px;
}

.rack-manage-content {
  background: #fff;
  padding: 24px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
</style>
