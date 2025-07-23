<template>
  <div class="overview-container">
    <h1 class="page-title">集群总览</h1>
    <div class="overview-content">
      <p>集群总览内容将在这里显示</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Overview'
}
</script>

<style scoped>
.overview-container {
  padding: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 500;
  margin-bottom: 20px;
}

.overview-content {
  background: #fff;
  padding: 24px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
</style>
