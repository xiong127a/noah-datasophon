<template>
  <div class="host-manage-container">
    <h1 class="page-title">主机列表</h1>
    <div class="host-manage-content">
      <p>主机列表内容将在这里显示</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'HostManage'
}
</script>

<style scoped>
.host-manage-container {
  padding: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 500;
  margin-bottom: 20px;
}

.host-manage-content {
  background: #fff;
  padding: 24px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
</style>
