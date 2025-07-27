<script setup lang="ts">
import { definePage } from 'vue-router/auto'
import ClusterList from '@/views/cluster/ClusterList.vue'

// 定义页面元数据
definePage({
  meta: {
    title: '集群管理',
    icon: 'colony',
    requiresAuth: true
  }
})
</script>

<template>
  <div class="page-container">
    <ClusterList />
  </div>
</template>

<style scoped>
.page-container {
  padding: 1rem;
}
</style> 