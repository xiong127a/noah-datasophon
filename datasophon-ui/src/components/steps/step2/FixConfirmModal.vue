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
  <a-modal
    :visible="visible"
    :footer="null"
    :maskClosable="false"
    width="460px"
    :destroyOnClose="true"
    :closable="false"
    class="fix-confirm-modal apple-card-modal"
    @cancel="$emit('update:visible', false)"
  >
    <!-- 弹窗内容 -->
    <div class="apple-card-container">
      <!-- 顶部标题区域 -->
      <div class="apple-card-header">
        <div class="apple-card-icon-wrapper">
          <div class="apple-card-icon-container warning">
            <svg viewBox="0 0 24 24" width="20" height="20" stroke="#FFFFFF" fill="none" stroke-width="2">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
              <line x1="12" y1="9" x2="12" y2="13" />
              <line x1="12" y1="17" x2="12.01" y2="17" />
            </svg>
          </div>
        </div>
        <div class="apple-card-info">
          <div class="apple-card-title">
            <span class="card-title">{{ title }}</span>
          </div>
        </div>
        <div class="apple-card-close" @click="$emit('cancel')">
          <svg viewBox="0 0 24 24" width="18" height="18" stroke="currentColor" stroke-width="2" fill="none">
            <circle cx="12" cy="12" r="10" />
            <line x1="15" y1="9" x2="9" y2="15" />
            <line x1="9" y1="9" x2="15" y2="15" />
          </svg>
        </div>
      </div>
      
      <!-- 内容区域 -->
      <div class="apple-card-content">
        <div class="apple-card-section">
          <div class="warning-message">
            <div class="message-content" v-html="content"></div>
          </div>
        </div>
      </div>
      
      <!-- 底部按钮区域 -->
      <div class="apple-card-footer">
        <button class="apple-card-button secondary" @click="$emit('cancel')">
          取消
        </button>
        <button 
          class="apple-card-button danger" 
          @click="$emit('confirm')"
          :disabled="loading"
        >
          <span v-if="!loading">确认修复</span>
          <span v-else class="apple-loader"></span>
        </button>
      </div>
    </div>
  </a-modal>
</template>

<script>
export default {
  name: 'FixConfirmModal',
  props: {
    // 是否可见
    visible: {
      type: Boolean,
      default: false
    },
    // 弹窗标题
    title: {
      type: String,
      default: '确认修复'
    },
    // 弹窗内容
    content: {
      type: String,
      default: ''
    },
    // 是否加载中
    loading: {
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

.apple-card-modal {
  .apple-card-container {
    font-family: "SF Pro Display", "SF Pro Icons", "Helvetica Neue", Helvetica, Arial, sans-serif;
    
    .apple-card-header {
      display: flex;
      align-items: center;
      padding: 16px 20px;
      border-bottom: 1px solid rgba(0, 0, 0, 0.05);
      
      .apple-card-icon-wrapper {
        margin-right: 12px;
        
        .apple-card-icon-container {
          width: 32px;
          height: 32px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          
          &.warning {
            background-color: @apple-orange;
          }
        }
      }
      
      .apple-card-info {
        flex: 1;
        
        .apple-card-title {
          font-size: 16px;
          font-weight: 600;
          color: @apple-black;
        }
      }
      
      .apple-card-close {
        color: @apple-gray;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        width: 24px;
        height: 24px;
        border-radius: 50%;
        transition: all 0.2s ease;
        
        &:hover {
          background-color: @apple-gray-light;
        }
      }
    }
    
    .apple-card-content {
      padding: 20px;
      
      .apple-card-section {
        .warning-message {
          font-size: 14px;
          line-height: 1.6;
          color: @apple-black;
          
          .message-content {
            /deep/ a {
              color: @apple-blue;
              text-decoration: none;
              
              &:hover {
                text-decoration: underline;
              }
            }
            
            /deep/ strong {
              font-weight: 600;
            }
          }
        }
      }
    }
    
    .apple-card-footer {
      display: flex;
      justify-content: flex-end;
      padding: 12px 20px 20px;
      gap: 12px;
      
      .apple-card-button {
        border: none;
        border-radius: 6px;
        padding: 8px 16px;
        font-size: 14px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s ease;
        
        &.secondary {
          background-color: @apple-gray-light;
          color: @apple-black;
          
          &:hover {
            background-color: darken(@apple-gray-light, 5%);
          }
        }
        
        &.danger {
          background-color: @apple-red;
          color: @apple-white;
          
          &:hover:not(:disabled) {
            background-color: darken(@apple-red, 5%);
          }
          
          &:disabled {
            opacity: 0.6;
            cursor: not-allowed;
          }
        }
        
        .apple-loader {
          display: inline-block;
          width: 16px;
          height: 16px;
          border: 2px solid rgba(255, 255, 255, 0.3);
          border-radius: 50%;
          border-top-color: @apple-white;
          animation: apple-spin 1s linear infinite;
        }
      }
    }
  }
}

@keyframes apple-spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style> 