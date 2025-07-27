<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { definePage } from 'vue-router/auto'

// 定义页面元数据
definePage({
  meta: {
    title: '首页',
    requiresAuth: true,
    icon: 'home'
  }
})

const title = ref('Noah大数据平台')
const description = ref('欢迎使用Noah大数据平台')
</script>

<template>
  <div class="page-container">
    <h1 class="text-2xl font-bold mb-4">{{ title }}</h1>
    <p>{{ description }}</p>
  </div>
</template>

<style scoped>
.page-container {
  padding: 1rem;
}
</style> 