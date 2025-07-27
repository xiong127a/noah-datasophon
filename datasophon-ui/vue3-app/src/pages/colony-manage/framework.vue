<script setup lang="ts">
import { definePage } from 'vue-router/auto'
import FrameworkManage from '@/views/cluster/FrameworkManage.vue'

// 定义页面元数据
definePage({
  meta: {
    title: '集群框架',
    icon: 'framework',
    requiresAuth: true
  }
})
</script>

<template>
  <div class="page-container">
    <FrameworkManage />
  </div>
</template>

<style scoped>
.page-container {
  padding: 1rem;
}
</style> 