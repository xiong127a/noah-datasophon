<!--
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
-->
<template>
  <div class="table-operations">
    <!-- 左侧功能按钮区 -->
    <div class="left-operations">
      <a-button
        class="apple-button apple-outlined-button"
        @click="$emit('set-hostname')"
      >
        <a-icon type="edit" />
        <span>{{ $t('设置主机名') }}</span>
      </a-button>
      
      <a-button
        class="apple-button apple-outlined-button"
        @click="$emit('sync-hosts')"
      >
        <a-icon type="sync" />
        <span>{{ $t('同步hosts文件') }}</span>
      </a-button>
    </div>
    
    <!-- 右侧开始检查/重试/终止检查三合一按钮 -->
    <a-button
      class="apple-button"
      :class="isCheckingActive ? 'apple-danger-button' : 'apple-primary-button'"
      @click="$emit('check-action')"
    >
      <a-icon :type="isCheckingActive ? 'stop' : (hasStartedCheck ? 'redo' : 'play-circle')" />
      <span>{{ isCheckingActive ? '终止检查' : (hasStartedCheck ? '重试检查' : '开始检查') }}</span>
    </a-button>
  </div>
</template>

<script>
export default {
  name: 'TableOperations',
  props: {
    // 是否有检查正在进行中
    isCheckingActive: {
      type: Boolean,
      default: false
    },
    // 是否已开始过检查
    hasStartedCheck: {
      type: Boolean,
      default: false
    }
  }
};
</script>

<style lang="less" scoped>
// 苹果设计系统颜色
@apple-white: #ffffff;
@apple-black: #1d1d1f;
@apple-gray-light: #f5f5f7;
@apple-gray: #86868b;
@apple-blue: #0071e3;
@apple-blue-hover: #147CE5;
@apple-red: #ff453a;
@apple-green: #30d158;
@apple-yellow: #ffd60a;
@apple-orange: #ff9f0a;

.table-operations {
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  .left-operations {
    display: flex;
    gap: 8px;
  }
  
  .apple-button {
    border: none;
    border-radius: 20px;
    padding: 0 16px;
    height: 40px;
    font-size: 14px;
    font-weight: 500;
    display: flex;
    align-items: center;
    cursor: pointer;
    transition: all 0.3s ease;
    
    i {
      margin-right: 6px;
      font-size: 14px;
    }
    
    &.apple-primary-button {
      background-color: @apple-blue;
      color: @apple-white;
      
      &:hover {
        background-color: @apple-blue-hover;
      }
    }
    
    &.apple-danger-button {
      background-color: @apple-red;
      color: @apple-white;
      
      &:hover {
        background-color: darken(@apple-red, 5%);
      }
    }
    
    &.apple-outlined-button {
      background-color: transparent;
      color: @apple-blue;
      border: 1px solid @apple-blue;
      
      &:hover {
        background-color: fade(@apple-blue, 5%);
      }
    }
  }
}
</style> 