<script setup lang="ts">
import { definePage } from 'vue-router/auto'
import NotFound from '../views/NotFound.vue'

// 定义页面元数据
definePage({
  meta: {
    title: '页面不存在',
    requiresAuth: false
  }
})
</script>

<template>
  <NotFound />
</template> 