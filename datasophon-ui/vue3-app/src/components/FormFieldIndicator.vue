<template>
  <span 
    class="relative inline-block w-1.5 h-1.5 rounded-full ml-1 transition-colors duration-300"
    :class="indicatorColor"
  >
    <span 
      class="absolute inset-0 rounded-full animate-ping transition-colors duration-300" 
      :class="pingColor"
    ></span>
  </span>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  // 字段是否已触摸/修改过
  dirty: {
    type: Boolean,
    default: false
  },
  // 字段是否有错误
  error: {
    type: Boolean,
    default: false
  },
  // 字段值
  value: {
    type: [String, Number, Boolean, Array, Object],
    default: null
  },
  // 字段是否必填
  required: {
    type: Boolean,
    default: true
  }
});

// 判断字段是否有值
const hasValue = computed(() => {
  if (props.value === null || props.value === undefined) return false;
  if (typeof props.value === 'string') return props.value.trim() !== '';
  if (Array.isArray(props.value)) return props.value.length > 0;
  if (typeof props.value === 'object') return Object.keys(props.value).length > 0;
  return !!props.value;
});

// 计算指示器颜色
const indicatorColor = computed(() => {
  // 未填写必填字段时为红色（无论是否已触摸）
  if (props.required && !hasValue.value) {
    return 'bg-red-500';
  }
  // 有错误时为黄色
  if (props.error) {
    return 'bg-yellow-500';
  }
  // 已填写且验证通过为绿色
  if (hasValue.value && !props.error) {
    return 'bg-green-500';
  }
  // 其他情况为灰色
  return 'bg-gray-300';
});

// 计算动画颜色
const pingColor = computed(() => {
  if (props.required && !hasValue.value) {
    return 'bg-red-500/20';
  }
  if (props.error) {
    return 'bg-yellow-500/20';
  }
  if (hasValue.value && !props.error) {
    return 'bg-green-500/20';
  }
  return 'bg-gray-300/20';
});
</script> 