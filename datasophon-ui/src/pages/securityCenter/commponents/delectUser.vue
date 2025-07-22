<!--
/*
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


 * @Date: 2022-06-08 17:37:26
 * @LastEditTime: 2022-06-09 14:45:28
 * @FilePath: \ddh-ui\src\pages\securityCenter\commponents\delectUser.vue
-->
<template>
  <div class="delete-user-container">
    <div class="delete-message">
      <a-icon type="exclamation-circle" theme="filled" class="warning-icon" />
      <div class="message-content">
        <div class="message-title">即将删除用户</div>
        <div class="message-detail">
          您确定要删除用户 <span class="highlight">{{ detail.username }}</span> 吗？此操作不可撤销。
        </div>
      </div>
    </div>
    
    <div class="action-buttons">
      <a-button @click.stop="formCancel" class="cancel-button">取消</a-button>
      <a-button type="danger" @click.stop="handleSubmit" class="confirm-button">
        <a-icon type="delete" />
        确认删除
      </a-button>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    callBack: Function,
    sysTypeTxt: String,
    detail: Object,
  },
  data() {
    return {
      loading: false
    };
  },
  methods: {
    handleSubmit(e) {
      let self = this;
      e.preventDefault();
      
      this.loading = true;
      const params = JSON.stringify([this.detail.id]);
      
      this.$axiosPostUpload(global.API.deleteUser, params)
        .then((res) => {
          this.loading = false;
          if (res.code === 200) {
            this.$message.success("删除成功", 2);
            this.$destroyAll();
            self.callBack();
          }
        })
        .catch(() => {
          this.loading = false;
          this.$message.error("删除失败，请稍后重试", 2);
        });
    },
    formCancel() {
      this.$destroyAll();
    },
  }
};
</script>

<style lang="less" scoped>
.delete-user-container {
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'Helvetica Neue', Arial, sans-serif;
  padding: 16px 0;
  
  .delete-message {
    display: flex;
    align-items: flex-start;
    margin-bottom: 24px;
    
    .warning-icon {
      color: #ff9500;
      font-size: 24px;
      margin-right: 16px;
      margin-top: 2px;
    }
    
    .message-content {
      flex: 1;
      
      .message-title {
        font-weight: 600;
        font-size: 16px;
        color: #1f2937;
        margin-bottom: 8px;
      }
      
      .message-detail {
        color: #4b5563;
        font-size: 14px;
        line-height: 1.6;
        
        .highlight {
          font-weight: 600;
          color: #111827;
        }
      }
    }
  }
  
  .action-buttons {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    margin-top: 24px;
    padding-top: 16px;
    border-top: 1px solid #f3f4f6;
    
    .cancel-button {
      height: 36px;
      font-weight: 500;
      border-radius: 8px;
      border-color: #d1d5db;
      color: #4b5563;
      
      &:hover, &:focus {
        color: #374151;
        border-color: #9ca3af;
      }
    }
    
    .confirm-button {
      height: 36px;
      font-weight: 500;
      border-radius: 8px;
      background: #f87171;
      border-color: #f87171;
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      
      .anticon {
        margin-right: 4px;
      }
      
      &:hover, &:focus {
        background: #ef4444;
        border-color: #ef4444;
        transform: translateY(-1px);
        box-shadow: 0 2px 5px rgba(239, 68, 68, 0.2);
      }
      
      &:active {
        background: #dc2626;
        border-color: #dc2626;
        transform: translateY(0);
      }
    }
  }
}
</style>
