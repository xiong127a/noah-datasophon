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
    title="编辑主机名"
    :maskClosable="false"
    :destroyOnClose="true"
    :width="460"
    @cancel="$emit('cancel')"
    class="hostname-edit-modal"
  >
    <div class="modal-content">
      <!-- 编辑信息 -->
      <div class="edit-info">
        <div class="host-info">
          <div class="host-ip">
            <span class="label">主机IP：</span>
            <span class="value">{{ host.ip }}</span>
          </div>
          <div class="current-hostname">
            <span class="label">当前主机名：</span>
            <span class="value">{{ host.hostname }}</span>
          </div>
        </div>
        
        <!-- 输入新主机名 -->
        <div class="input-group">
          <a-form-item label="新主机名">
            <a-input 
              ref="hostnameInput"
              v-model="newHostname" 
              placeholder="请输入新的主机名"
              :maxLength="64"
              @keyup.enter="handleSubmit"
            />
          </a-form-item>
          <div class="input-hint" v-if="newHostname !== host.hostname">
            <a-icon type="info-circle" />
            <span>主机名将从 {{ host.hostname }} 修改为 {{ newHostname }}</span>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 底部按钮 -->
    <template slot="footer">
      <a-button @click="$emit('cancel')" class="cancel-button">取消</a-button>
      <a-button 
        type="primary" 
        :loading="loading" 
        @click="handleSubmit" 
        :disabled="!isValid"
        class="submit-button"
      >
        确认修改
      </a-button>
    </template>
  </a-modal>
</template>

<script>
export default {
  name: 'HostnameEditModal',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    host: {
      type: Object,
      default: () => ({ ip: '', hostname: '' })
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      newHostname: ''
    };
  },
  computed: {
    isValid() {
      return this.newHostname && this.newHostname.trim() && this.newHostname !== this.host.hostname;
    }
  },
  watch: {
    visible(val) {
      if (val && this.host) {
        this.newHostname = this.host.hostname || '';
        // 聚焦输入框
        this.$nextTick(() => {
          if (this.$refs.hostnameInput) {
            this.$refs.hostnameInput.focus();
            this.$refs.hostnameInput.select();
          }
        });
      }
    }
  },
  methods: {
    handleSubmit() {
      if (!this.isValid) return;
      this.$emit('submit', this.newHostname);
    }
  }
};
</script>

<style lang="less" scoped>
.hostname-edit-modal {
  :deep(.ant-modal-content) {
    border-radius: 12px;
    overflow: hidden;
  }
  
  :deep(.ant-modal-header) {
    border-bottom: 1px solid rgba(0, 0, 0, 0.05);
    padding: 16px 24px;
    
    .ant-modal-title {
      font-size: 16px;
      font-weight: 600;
      color: #1d1d1f;
    }
  }
  
  :deep(.ant-modal-body) {
    padding: 24px;
  }
  
  :deep(.ant-modal-footer) {
    border-top: 1px solid rgba(0, 0, 0, 0.05);
    padding: 12px 24px;
  }
  
  .modal-content {
    .edit-info {
      .host-info {
        margin-bottom: 20px;
        padding: 16px;
        background-color: #f5f5f7;
        border-radius: 8px;
        
        .host-ip, .current-hostname {
          display: flex;
          margin-bottom: 8px;
          font-size: 14px;
          
          &:last-child {
            margin-bottom: 0;
          }
          
          .label {
            color: #86868b;
            width: 100px;
          }
          
          .value {
            color: #1d1d1f;
            font-weight: 500;
          }
        }
      }
      
      .input-group {
        :deep(.ant-form-item-label) {
          line-height: 1.5;
          
          label {
            color: #1d1d1f;
            font-size: 14px;
          }
        }
        
        :deep(.ant-input) {
          border-radius: 8px;
          border-color: #d2d2d7;
          transition: all 0.3s;
          
          &:hover, &:focus {
            border-color: #0071e3;
            box-shadow: 0 0 0 2px rgba(0, 113, 227, 0.2);
          }
        }
        
        .input-hint {
          font-size: 13px;
          color: #0071e3;
          display: flex;
          align-items: center;
          margin-top: 8px;
          
          i {
            margin-right: 6px;
          }
        }
      }
    }
  }
  
  .cancel-button {
    border-radius: 8px;
  }
  
  .submit-button {
    border-radius: 8px;
    background-color: #0071e3;
    border-color: #0071e3;
    
    &:hover:not(:disabled) {
      background-color: #147CE5;
      border-color: #147CE5;
    }
    
    &:disabled {
      background-color: rgba(0, 113, 227, 0.5);
      border-color: rgba(0, 113, 227, 0.5);
    }
  }
}
</style> 