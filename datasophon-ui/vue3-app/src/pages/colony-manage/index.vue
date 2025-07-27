<script setup lang="ts">
import { definePage } from 'vue-router/auto'
import { onBeforeMount } from 'vue'
import { useRouter } from 'vue-router/auto'

// 定义页面元数据
definePage({
  meta: {
    title: '集群管理',
    icon: 'colony',
    requiresAuth: true
  }
})

const router = useRouter()

// 默认重定向到列表页面
onBeforeMount(() => {
  router.replace('/colony-manage/list')
})
</script>

<template>
  <div class="redirect-container">
    <p>正在加载集群管理...</p>
  </div>
</template>

<style scoped>
.redirect-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
}
</style> 