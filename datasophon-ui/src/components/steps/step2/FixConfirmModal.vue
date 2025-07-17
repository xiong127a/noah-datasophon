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
    :title="title"
    :maskClosable="false"
    :destroyOnClose="true"
    :width="600"
    @cancel="$emit('cancel')"
    :footer="null"
    class="fix-confirm-modal"
  >
    <div class="modal-content">
      <!-- 警告图标 -->
      <div class="warning-icon">
        <a-icon type="warning" theme="filled" />
      </div>
      
      <!-- 确认信息 -->
      <div class="confirm-message">
        <div class="message-title">确定修复此检查项?</div>
        <div class="message-description">
          <p v-if="checkItemInfo">
            您正在尝试修复 <strong>{{ checkItemInfo.itemName }}</strong> 检查项，
            确认修复可能会导致系统配置变更。
          </p>
          <p>该操作无法撤销，请确认您了解修复操作的影响。</p>
        </div>
      </div>
    </div>
    
    <!-- 修复建议内容 -->
    <div class="fix-suggestion" v-if="checkItemInfo && checkItemInfo.fixSuggestion">
      <div class="suggestion-header">修复建议</div>
      <div class="suggestion-content" v-html="checkItemInfo.fixSuggestion"></div>
    </div>
    
    <!-- 跳过确认选项 -->
    <div class="skip-option" v-if="showSkipOption">
      <a-checkbox v-model="skipConfirm">下次不再显示确认</a-checkbox>
    </div>
    
    <!-- 按钮操作 -->
    <div class="action-buttons">
      <a-button @click="$emit('cancel')" class="cancel-button">取消</a-button>
      <a-button 
        type="primary" 
        :loading="loading" 
        @click="handleConfirm" 
        class="confirm-button"
      >
        确认修复
      </a-button>
    </div>
  </a-modal>
</template>

<script>
export default {
  name: 'FixConfirmModal',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    title: {
      type: String,
      default: '修复确认'
    },
    checkItemInfo: {
      type: Object,
      default: null
    },
    loading: {
      type: Boolean,
      default: false
    },
    showSkipOption: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      skipConfirm: false
    };
  },
  methods: {
    handleConfirm() {
      this.$emit('confirm', this.skipConfirm);
    }
  }
};
</script>

<style lang="less" scoped>
.fix-confirm-modal {
  :deep(.ant-modal-content) {
    border-radius: 12px;
    overflow: hidden;
  }
  
  :deep(.ant-modal-header) {
    border-bottom: 1px solid rgba(0, 0, 0, 0.05);
    padding: 16px 24px;
  }
  
  :deep(.ant-modal-body) {
    padding: 24px;
  }
  
  .modal-content {
    display: flex;
    margin-bottom: 24px;
    
    .warning-icon {
      margin-right: 20px;
      
      i {
        font-size: 48px;
        color: #FF9500;
      }
    }
    
    .confirm-message {
      .message-title {
        font-size: 18px;
        font-weight: 600;
        color: #1d1d1f;
        margin-bottom: 12px;
      }
      
      .message-description {
        font-size: 14px;
        color: #86868b;
        line-height: 1.6;
        
        strong {
          color: #1d1d1f;
        }
      }
    }
  }
  
  .fix-suggestion {
    margin-bottom: 24px;
    padding: 16px;
    background-color: #f5f5f7;
    border-radius: 8px;
    
    .suggestion-header {
      font-size: 15px;
      font-weight: 600;
      color: #1d1d1f;
      margin-bottom: 12px;
    }
    
    .suggestion-content {
      font-size: 14px;
      color: #1d1d1f;
      line-height: 1.6;
      
      :deep(pre) {
        background-color: rgba(0, 0, 0, 0.03);
        padding: 12px;
        border-radius: 6px;
        overflow: auto;
        margin: 8px 0;
      }
      
      :deep(code) {
        font-family: 'SF Mono', monospace;
        font-size: 13px;
      }
    }
  }
  
  .skip-option {
    margin-bottom: 24px;
  }
  
  .action-buttons {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    
    .cancel-button {
      border-radius: 8px;
    }
    
    .confirm-button {
      border-radius: 8px;
      background-color: #0071e3;
      border-color: #0071e3;
      
      &:hover {
        background-color: #147CE5;
        border-color: #147CE5;
      }
    }
  }
}
</style> 