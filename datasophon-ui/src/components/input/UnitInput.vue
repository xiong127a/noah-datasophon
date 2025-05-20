<!--
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @describe: 带单位的输入框组件
 * 
 * 用法:
 * <unit-input v-model="value" unit="MB" />
 * 
 * 组件会在输入框右侧显示一个单位，样式与图片中一致
 * 单位显示在输入框内部，有独立的背景色
 */
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
    /**
     * 输入框的值
     */
    value: {
      type: [String, Number],
      default: ''
    },
    /**
     * 显示的单位（显示在输入框右侧）
     */
    unit: {
      type: String,
      default: ''
    },
    /**
     * 输入框的占位符
     */
    placeholder: {
      type: String,
      default: '请输入'
    },
    /**
     * 是否禁用输入框
     */
    disabled: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      // 是否获取焦点
      focused: false
    }
  },
  methods: {
    /**
     * 处理输入事件
     */
    handleInput(e) {
      this.$emit('input', e.target.value);
    },
    /**
     * 处理获取焦点事件
     */
    handleFocus() {
      this.focused = true;
      this.$emit('focus');
    },
    /**
     * 处理失去焦点事件
     */
    handleBlur() {
      this.focused = false;
      this.$emit('blur');
    },
    /**
     * 获取输入框的引用
     */
    getInputRef() {
      return this.$refs.input;
    },
    /**
     * 聚焦输入框
     */
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