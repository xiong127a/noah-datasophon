<template>
  <div class="service-overview-container">
    <div class="page-header">
      <div class="header-content">
        <div class="page-title">服务管理</div>
        <div class="page-description">管理和监控您的大数据服务，实现高效运维</div>
      </div>
    </div>
    
    <div class="content-section">
      <el-empty description="服务管理页面正在开发中..." />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

// 这是一个临时的服务管理页面，主要用于解决路由错误
// This is a temporary service management page to resolve routing errors

onMounted(() => {
  console.log('ServiceOverview page loaded')
})
</script>

<style scoped lang="scss">
$primary-color: #0066cc;
$text-color: #1d1d1f;
$text-color-secondary: #86868b;
$border-radius: 16px;

.service-overview-container {
  padding: 24px;
  min-height: 100vh;
  background-color: #f6f6f6;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'Helvetica Neue', Helvetica, Arial, sans-serif;
}

.page-header {
  background: rgba(255, 255, 255, 0.8);
  padding: 32px;
  border-radius: $border-radius;
  margin-bottom: 24px;
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  
  .header-content {
    .page-title {
      font-size: 28px;
      font-weight: 600;
      color: $text-color;
      margin: 0 0 8px 0;
      letter-spacing: -0.025em;
    }

    .page-description {
      color: $text-color-secondary;
      margin: 0;
      font-size: 16px;
      line-height: 1.4;
    }
  }
}

.content-section {
  background: rgba(255, 255, 255, 0.8);
  border-radius: $border-radius;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style> 