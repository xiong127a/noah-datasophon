<template>
  <div class="app-container">
    <!-- 导入和使用MainLayout组件，为页面提供统一的布局 -->
    <MainLayout>
      <slot />
    </MainLayout>
  </div>
</template>

<script setup lang="ts">
import MainLayout from '../layouts/MainLayout.vue'
</script>

<style scoped>
.app-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}
</style> 