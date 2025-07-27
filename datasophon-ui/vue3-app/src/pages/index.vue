<script setup lang="ts">
import { useRouter, onBeforeRouteEnter } from 'vue-router/auto'

// 重定向到/home页面
onBeforeRouteEnter(() => {
  return { path: '/home' }
})
</script>

<template>
  <div>重定向到主页中...</div>
</template> 