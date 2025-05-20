<!--
 * @describe: 带单位的输入框组件
 -->
<template>
  <div class="unit-input-container" :class="{ 'is-focused': focused }">
    <a-input
      :value="value"
      @input="handleInput"
      :placeholder="placeholder"
      :disabled="disabled"
      ref="input"
      @focus="handleFocus"
      @blur="handleBlur"
    />
    <div class="unit-display" v-if="unit">{{ unit }}</div>
  </div>
</template>

<script>
export default {
  name: 'UnitInput',
  props: {
    // 输入框的值
    value: {
      type: [String, Number],
      default: ''
    },
    // 显示的单位
    unit: {
      type: String,
      default: ''
    },
    // 占位符
    placeholder: {
      type: String,
      default: '请输入'
    },
    // 是否禁用
    disabled: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      focused: false
    }
  },
  methods: {
    // 处理输入事件
    handleInput(e) {
      this.$emit('input', e.target.value);
    },
    // 处理获取焦点事件
    handleFocus() {
      this.focused = true;
    },
    // 处理失去焦点事件
    handleBlur() {
      this.focused = false;
    },
    // 获取输入框的引用
    getInputRef() {
      return this.$refs.input;
    },
    // 聚焦输入框
    focus() {
      this.$refs.input.focus();
    }
  }
}
</script>

<style lang="less" scoped>
.unit-input-container {
  position: relative;
  display: flex;
  width: 100%;
  
  /deep/ .ant-input {
    border-top-right-radius: 0;
    border-bottom-right-radius: 0;
    border-right: none;
  }
  
  .unit-display {
    display: flex;
    align-items: center;
    justify-content: center;
    min-width: 70px;
    height: 32px;
    padding: 0 11px;
    color: rgba(0, 0, 0, 0.65);
    font-size: 14px;
    text-align: center;
    background-color: #f5f5f5;
    border: 1px solid #d9d9d9;
    border-left: none;
    border-radius: 0 4px 4px 0;
  }
  
  &:hover .unit-display {
    border-color: #40a9ff;
  }
  
  &.is-focused .unit-display {
    border-color: #40a9ff;
    box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
  }
}
</style> 