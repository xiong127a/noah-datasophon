<template>
  <div class="container mx-auto p-4">
    <h1 class="text-2xl font-bold mb-6">使用帮助</h1>
    <div class="bg-white rounded-lg shadow p-6">
      <h2 class="text-xl font-semibold mb-4">告警系统使用指南</h2>
      <p class="mb-4">告警系统提供了全面的监控和告警功能，帮助您及时发现和处理系统异常。</p>
      
      <div class="mb-6">
        <h3 class="text-lg font-medium mb-2">主要功能</h3>
        <ul class="list-disc pl-6 space-y-2">
          <li>通知组管理：配置接收告警的人员和联系方式</li>
          <li>告警组管理：设置告警规则和阈值</li>
          <li>告警指标管理：配置需要监控的指标项目</li>
        </ul>
      </div>
      
      <div class="mb-6">
        <h3 class="text-lg font-medium mb-2">快速入门</h3>
        <ol class="list-decimal pl-6 space-y-2">
          <li>首先创建通知组，添加接收告警的人员和联系方式</li>
          <li>配置需要监控的指标项目</li>
          <li>创建告警组，设置告警规则和阈值，并关联通知组</li>
          <li>启用告警规则，系统将开始监控并发送告警</li>
        </ol>
      </div>
      
      <div class="bg-blue-50 p-4 rounded-lg">
        <h3 class="text-lg font-medium mb-2 text-blue-700">提示</h3>
        <p>如需更详细的使用指南，请参考系统文档或联系管理员获取支持。</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 告警管理-使用帮助视图
</script>

<style scoped>
/* 组件样式可以在这里添加 */
</style> 