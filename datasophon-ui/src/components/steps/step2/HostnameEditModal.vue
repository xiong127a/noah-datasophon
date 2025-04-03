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
    :confirm-loading="loading"
    @cancel="$emit('cancel')"
    :footer="null"
    :maskClosable="false"
    :closable="false"
    width="420px"
    class="apple-style-modal hostname-edit-modal"
    :destroyOnClose="true"
  >
    <div class="apple-modal-header">
      <div class="modal-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="#007AFF" stroke-width="1.5" width="20" height="20">
          <path d="M12 15V12m0-3h.01M3 12a9 9 0 1 0 18 0 9 9 0 0 0-18 0z" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>
      <div class="modal-title">修改主机名</div>
      <div class="modal-close" @click="$emit('cancel')">
        <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" stroke-width="1.5" fill="none">
          <line x1="18" y1="6" x2="6" y2="18"></line>
          <line x1="6" y1="6" x2="18" y2="18"></line>
        </svg>
      </div>
    </div>
    
    <div class="apple-modal-content">
      <div class="host-info-section">
        <div class="info-row">
          <div class="info-label">主机IP</div>
          <div class="info-value">{{ host ? host.ip : '' }}</div>
        </div>
        <div class="info-row">
          <div class="info-label">当前主机名</div>
          <div class="info-value current-hostname">{{ host ? (host.hostname || '未设置') : '' }}</div>
        </div>
      </div>
      
      <div class="input-section">
        <div class="input-label">新主机名</div>
        <input
          class="apple-input"
          v-model="hostnameValue"
          placeholder="请输入新的主机名"
          maxlength="64"
          ref="hostnameInput"
        />
        <div class="input-description">
          <svg viewBox="0 0 24 24" fill="none" stroke="#8E8E93" stroke-width="1.5" width="16" height="16">
            <circle cx="12" cy="12" r="10"></circle>
            <path d="M12 16v-4M12 8h.01" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span>修改主机名将通过SSH连接服务器并实际修改系统配置</span>
        </div>
      </div>
      
      <div class="apple-modal-actions">
        <button class="apple-button secondary" @click="$emit('cancel')">
          取消
        </button>
        <button 
          class="apple-button primary" 
          @click="$emit('submit', hostnameValue)" 
          :disabled="!hostnameValue || loading"
        >
          <span v-if="!loading">保存</span>
          <span v-else class="button-loader"></span>
        </button>
      </div>
    </div>
  </a-modal>
</template>

<script>
export default {
  name: 'HostnameEditModal',
  props: {
    // 是否可见
    visible: {
      type: Boolean,
      default: false
    },
    // 当前编辑的主机
    host: {
      type: Object,
      default: null
    },
    // 是否加载中
    loading: {
      type: Boolean,
      default: false
    },
    // 主机名
    hostname: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      hostnameValue: ''
    }
  },
  watch: {
    host(newVal) {
      if (newVal) {
        this.hostnameValue = newVal.hostname || '';
      }
    },
    hostname(newVal) {
      this.hostnameValue = newVal;
    },
    visible(newVal) {
      if (newVal && this.host) {
        this.hostnameValue = this.host.hostname || '';
        // 在下一个DOM更新循环后，聚焦输入框
        this.$nextTick(() => {
          if (this.$refs.hostnameInput) {
            this.$refs.hostnameInput.focus();
            this.$refs.hostnameInput.select();
          }
        });
      }
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

.hostname-edit-modal {
  .apple-modal-header {
    display: flex;
    align-items: center;
    padding: 16px 20px;
    border-bottom: 1px solid rgba(0, 0, 0, 0.05);
    font-family: "SF Pro Display", "SF Pro Icons", "Helvetica Neue", Helvetica, Arial, sans-serif;
    
    .modal-icon {
      margin-right: 12px;
    }
    
    .modal-title {
      flex: 1;
      font-size: 16px;
      font-weight: 600;
      color: @apple-black;
    }
    
    .modal-close {
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
  
  .apple-modal-content {
    padding: 20px;
    font-family: "SF Pro Display", "SF Pro Icons", "Helvetica Neue", Helvetica, Arial, sans-serif;
    
    .host-info-section {
      margin-bottom: 24px;
      
      .info-row {
        display: flex;
        margin-bottom: 12px;
        
        &:last-child {
          margin-bottom: 0;
        }
        
        .info-label {
          width: 100px;
          color: @apple-gray;
          font-size: 14px;
        }
        
        .info-value {
          flex: 1;
          font-size: 14px;
          font-weight: 500;
          color: @apple-black;
          
          &.current-hostname {
            color: @apple-blue;
          }
        }
      }
    }
    
    .input-section {
      margin-bottom: 24px;
      
      .input-label {
        font-size: 14px;
        font-weight: 500;
        color: @apple-black;
        margin-bottom: 8px;
      }
      
      .apple-input {
        width: 100%;
        height: 36px;
        padding: 8px 12px;
        font-size: 14px;
        border: 1px solid rgba(0, 0, 0, 0.1);
        border-radius: 6px;
        transition: all 0.2s ease;
        outline: none;
        
        &:focus {
          border-color: @apple-blue;
          box-shadow: 0 0 0 2px rgba(0, 113, 227, 0.2);
        }
        
        &::placeholder {
          color: @apple-gray;
        }
      }
      
      .input-description {
        display: flex;
        align-items: center;
        margin-top: 8px;
        font-size: 12px;
        color: @apple-gray;
        
        svg {
          margin-right: 6px;
          flex-shrink: 0;
        }
      }
    }
    
    .apple-modal-actions {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      
      .apple-button {
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
        
        &.primary {
          background-color: @apple-blue;
          color: @apple-white;
          
          &:hover:not(:disabled) {
            background-color: @apple-blue-hover;
          }
          
          &:disabled {
            opacity: 0.6;
            cursor: not-allowed;
          }
        }
        
        .button-loader {
          display: inline-block;
          width: 16px;
          height: 16px;
          border: 2px solid rgba(255, 255, 255, 0.3);
          border-radius: 50%;
          border-top-color: @apple-white;
          animation: spin 1s linear infinite;
        }
      }
    }
  }
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style> 