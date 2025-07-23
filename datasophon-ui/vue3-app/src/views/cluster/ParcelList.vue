<template>
  <div class="parcel-list-container">
    <h1 class="page-title">组件包管理</h1>
    <div class="parcel-list-content">
      <p>组件包管理内容将在这里显示</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ParcelList'
}
</script>

<style scoped>
.parcel-list-container {
  padding: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 500;
  margin-bottom: 20px;
}

.parcel-list-content {
  background: #fff;
  padding: 24px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
</style>
