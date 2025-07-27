<script setup lang="ts">
import { definePage } from 'vue-router/auto'
import ParcelList from '@/views/repository/ParcelList.vue'

// 定义页面元数据
definePage({
  meta: {
    title: '存储库管理',
    icon: 'storage',
    requiresAuth: true
  }
})
</script>

<template>
  <div class="page-container">
    <ParcelList />
  </div>
</template>

<style scoped>
.page-container {
  padding: 1rem;
}
</style> 