<script setup lang="ts">
import { definePage } from 'vue-router/auto'
import LoginComponent from '@/views/login/Login.vue'

// 定义页面元数据
definePage({
  layout: false,
  meta: {
    title: '登录',
    requiresAuth: false
  }
})
</script>

<template>
  <LoginComponent />
</template> 