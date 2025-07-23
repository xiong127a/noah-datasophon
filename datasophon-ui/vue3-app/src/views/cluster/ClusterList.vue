<template>
  <div class="cluster-list-container">
    <h1 class="page-title">集群列表</h1>
    <div class="cluster-list-content">
      <p>集群列表内容将在这里显示</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ClusterList'
}
</script>

<style scoped>
.cluster-list-container {
  padding: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 500;
  margin-bottom: 20px;
}

.cluster-list-content {
  background: #fff;
  padding: 24px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
</style>
