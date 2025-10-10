<template>
  <div 
    class="rotated-header" 
    :class="{ 
      'rotated-header-vertical': direction === 'vertical', 
      'rotated-header-horizontal': direction === 'horizontal'
    }"
  >
    <slot></slot>
  </div>
</template>

<script>
/**
 * 旋转表头组件
 * 用于在表格中显示垂直或水平方向的表头文字
 */
export default {
  name: 'RotatedHeader',
  props: {
    // 方向：vertical(垂直) 或 horizontal(水平)
    direction: {
      type: String,
      default: 'horizontal',
      validator: (value) => ['vertical', 'horizontal'].includes(value)
    },
    // 旋转角度（仅在vertical模式下有效）
    angle: {
      type: Number,
      default: 90
    },
    // 表头宽度
    width: {
      type: [Number, String],
      default: 'auto'
    },
    // 表头高度
    height: {
      type: [Number, String],
      default: 'auto'
    }
  }
}
</script>

<style lang="less" scoped>
.rotated-header {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  
  &-vertical {
    writing-mode: vertical-rl;
    transform: rotate(180deg);
    white-space: nowrap;
    margin: 0 auto;
    display: inline-block;
    
    // 确保垂直模式下仍然有适当的尺寸
    min-height: 80px;
    text-align: center;
  }
  
  &-horizontal {
    writing-mode: horizontal-tb;
    transform: none;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}
</style> 