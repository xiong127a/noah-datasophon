<template>
  <div class="direct-access-container">
    <slot></slot>
  </div>
</template>

<script setup>
import { defineComponent } from 'vue'

defineComponent({
  name: 'DirectAccessLayout'
})
</script>

<style scoped>
.direct-access-container {
  min-height: 100vh;
  padding: 20px;
}
</style> 