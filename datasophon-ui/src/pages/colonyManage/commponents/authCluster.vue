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


 * @Date: 2022-06-08 11:38:30
 * @LastEditTime: 2022-06-15 17:06:23
 * @FilePath: \ddh-ui\src\pages\colonyManage\commponents\authCluster.vue
-->
<template>
  <div class="apple-auth-container">
    <div class="auth-description">
      <div class="auth-icon">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
          <path d="M12 12C14.7614 12 17 9.76142 17 7C17 4.23858 14.7614 2 12 2C9.23858 2 7 4.23858 7 7C7 9.76142 9.23858 12 12 12Z" fill="#4a90e2"/>
          <path d="M12 14C7.58172 14 4 17.5817 4 22H20C20 17.5817 16.4183 14 12 14Z" fill="#4a90e2"/>
        </svg>
      </div>
      <div class="auth-text">
        <h3>集群授权管理</h3>
        <p>为集群 <strong>{{ detail.clusterName || '未知集群' }}</strong> 分配管理员权限</p>
      </div>
    </div>
    
    <a-form
      :label-col="labelCol"
      :wrapper-col="wrapperCol"
      :form="form"
      class="apple-auth-form"
    >
      <a-form-item label="选择管理员">
        <a-select 
          mode="multiple" 
          v-decorator="['userIds', { rules: [{ required: false}]}]"
          placeholder="请选择一个或多个集群管理员"
          class="apple-select"
          dropdown-class-name="apple-select-dropdown"
        >
          <a-select-option :value="item.id" v-for="(item,index) in userList" :key="index">
            <div class="user-option">
              <div class="user-avatar">{{ item.username.charAt(0).toUpperCase() }}</div>
              <span class="user-name">{{ item.username }}</span>
            </div>
          </a-select-option>
        </a-select>
      </a-form-item>
    </a-form>
    
    <div class="apple-auth-actions">
      <a-button
        class="apple-btn apple-btn-primary"
        @click.stop="handleSubmit"
        :loading="loading"
      >
        <span v-if="!loading">确认授权</span>
        <span v-else>处理中...</span>
      </a-button>
      <a-button 
        class="apple-btn apple-btn-secondary"
        @click.stop="formCancel"
      >
        取消
      </a-button>
    </div>
  </div>
</template>
<script>
export default {
  props: {
    detail: {
      type: Object,
      default: function () {
        return {};
      },
    },
    callBack:Function
  },
  data() {
    return {
      labelCol: {
        xs: { span: 24 },
        sm: { span: 7 },
      },
      wrapperCol: {
        xs: { span: 24 },
        sm: { span: 17 },
      },
      form: this.$form.createForm(this),
      value1: "",
      loading: false,
      userList: [] //集群管理员列表
    };
  },
  watch: {},
  methods: {
    formCancel() {
      this.$destroyAll();
    },
    handleSubmit(e) {
      const _this = this
      e.preventDefault();
      this.form.validateFields((err, values) => {
        console.log(values);
        if (!err) {
          const params = {
            "userIds": values.userIds || ""
          }
          if (JSON.stringify(this.detail) !== '{}') params.clusterId = this.detail.id
          this.loading = true;
          this.$axiosPost(global.API.authCluster, params).then((res) => {
            this.loading = false;
            if (res.code === 200) {
              if (params.userIds.length > 0) {
                this.$message.success('授权成功', 2)
              }else{
                this.$message.success('取消授权成功', 2)
              }
              this.$destroyAll();
              _this.callBack();
            }
          }).catch((err) => {});
        }
      });
    },
    queryAllUser() {
      this.$axiosPost(global.API.queryAllUser, {}).then((res) => {
        if (res.code === 200) {
          this.userList = res.data
        }
      })
    }
  },
  mounted() {
    this.queryAllUser()
  },
};
</script>
<style lang="less" scoped>
// Apple Design System Variables - LESS变量
@apple-blue: #007AFF;
@apple-blue-light: #5AC8FA;
@apple-blue-dark: #0051D5;
@apple-gray-1: #F2F2F7;
@apple-gray-2: #E5E5EA;
@apple-gray-3: #D1D1D6;
@apple-gray-4: #C7C7CC;
@apple-gray-5: #AEAEB2;
@apple-gray-6: #8E8E93;
@apple-text-primary: #000000;
@apple-text-secondary: #3C3C43;
@apple-text-tertiary: rgba(60, 60, 67, 0.6);
@apple-background: #FFFFFF;
@apple-background-secondary: #F2F2F7;
@apple-border: #D1D1D6;
@apple-radius-small: 8px;
@apple-radius-medium: 12px;
@apple-radius-large: 16px;
@apple-shadow-small: 0 1px 3px rgba(0, 0, 0, 0.1);
@apple-shadow-medium: 0 4px 16px rgba(0, 0, 0, 0.1);
@apple-shadow-large: 0 8px 32px rgba(0, 0, 0, 0.12);
@apple-blue-light-bg: rgba(0, 122, 255, 0.05);

.apple-auth-container {
  padding: 0;
  background: @apple-background;
  border-radius: @apple-radius-large;
  
  .auth-description {
    display: flex;
    align-items: center;
    gap: 20px;
    padding: 24px 0 28px;
    border-bottom: 1px solid @apple-gray-2;
    margin-bottom: 32px;
    
    .auth-icon {
      width: 56px;
      height: 56px;
      border-radius: @apple-radius-large;
      background: linear-gradient(135deg, @apple-blue 0%, @apple-blue-light 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      box-shadow: @apple-shadow-medium;
      
      svg {
        width: 28px;
        height: 28px;
        
        path {
          fill: white;
        }
      }
    }
    
    .auth-text {
      flex: 1;
      
      h3 {
        margin: 0 0 6px;
        font-size: 20px;
        font-weight: 700;
        color: @apple-text-primary;
        font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Helvetica Neue', sans-serif;
        letter-spacing: -0.02em;
      }
      
      p {
        margin: 0;
        font-size: 15px;
        color: @apple-text-secondary;
        line-height: 1.5;
        font-weight: 400;
        
        strong {
          color: @apple-blue;
          font-weight: 700;
        }
      }
    }
  }
  
  .apple-auth-form {
    margin-bottom: 36px;
    
    /deep/ .ant-form-item {
      margin-bottom: 0;
      
      .ant-form-item-label {
        padding-bottom: 16px;
        
        label {
          color: @apple-text-primary;
          font-weight: 600;
          font-size: 16px;
          font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Helvetica Neue', sans-serif;
          letter-spacing: -0.01em;
        }
      }
      
      .ant-form-item-control {
        .apple-select {
          min-height: 52px;
          border-radius: @apple-radius-large;
          border: 1px solid @apple-border;
          background: @apple-background;
          transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
          backdrop-filter: blur(20px);
          
          &:hover {
            border-color: @apple-blue;
            box-shadow: 0 0 0 1px @apple-blue;
            background: @apple-background;
          }
          
          &.ant-select-focused {
            border-color: @apple-blue;
            box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.15);
            background: @apple-background;
          }
          
          .ant-select-selector {
            border: none !important;
            background: transparent !important;
            box-shadow: none !important;
            padding: 12px 20px;
            min-height: 50px;
            border-radius: @apple-radius-large;
            
            .ant-select-selection-placeholder {
              color: @apple-text-tertiary;
              font-size: 15px;
              line-height: 26px;
              font-weight: 400;
            }
            
            .ant-select-selection-item {
              background: linear-gradient(135deg, @apple-blue 0%, @apple-blue-light 100%);
              color: white;
              border: none;
              border-radius: @apple-radius-medium;
              padding: 6px 14px;
              margin: 3px 6px 3px 0;
              font-weight: 600;
              font-size: 14px;
              height: auto;
              line-height: 1.4;
              box-shadow: @apple-shadow-small;
              
              .ant-select-selection-item-remove {
                color: rgba(255, 255, 255, 0.8);
                font-size: 13px;
                margin-left: 8px;
                border-radius: 50%;
                width: 16px;
                height: 16px;
                display: flex;
                align-items: center;
                justify-content: center;
                transition: all 0.2s ease;
                
                &:hover {
                  color: white;
                  background: rgba(255, 255, 255, 0.2);
                }
              }
            }
            
            .ant-select-selection-search {
              margin-left: 0;
              
              .ant-select-selection-search-input {
                height: 26px;
                line-height: 26px;
                font-size: 15px;
              }
            }
          }
          
          .ant-select-arrow {
            color: @apple-text-secondary;
            font-size: 16px;
            right: 20px;
            transition: all 0.2s ease;
            
            &:hover {
              color: @apple-blue;
            }
          }
        }
      }
    }
  }
  
  .apple-auth-actions {
    display: flex;
    gap: 16px;
    justify-content: flex-end;
    padding-top: 12px;
    
    .apple-btn {
      border-radius: @apple-radius-medium;
      font-weight: 600;
      height: 44px;
      padding: 0 28px;
      font-size: 15px;
      border: none;
      transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
      font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Helvetica Neue', sans-serif;
      letter-spacing: -0.01em;
      cursor: pointer;
      
      &.apple-btn-primary {
        background: linear-gradient(135deg, @apple-blue 0%, @apple-blue-light 100%);
        color: white;
        box-shadow: @apple-shadow-medium;
        
        &:hover {
          background: linear-gradient(135deg, @apple-blue-dark 0%, @apple-blue 100%);
          transform: translateY(-2px);
          box-shadow: 0 8px 24px rgba(0, 122, 255, 0.4);
        }
        
        &:active {
          transform: translateY(-1px);
          box-shadow: @apple-shadow-medium;
        }
        
        &.ant-btn-loading {
          background: linear-gradient(135deg, @apple-blue 0%, @apple-blue-light 100%);
          
          .ant-btn-loading-icon {
            color: white;
          }
        }
      }
      
      &.apple-btn-secondary {
        background: @apple-background-secondary;
        border: 1px solid @apple-border !important;
        color: @apple-text-primary;
        
        &:hover {
          background: @apple-gray-1;
          border-color: @apple-gray-3 !important;
          color: @apple-text-primary;
          transform: translateY(-2px);
          box-shadow: @apple-shadow-small;
        }
        
        &:active {
          transform: translateY(-1px);
          background: @apple-gray-2;
        }
      }
    }
  }
}

/* 下拉选项样式 - 完全圆润的苹果风格 */
/deep/ .apple-select-dropdown {
  border-radius: @apple-radius-large;
  overflow: hidden;
  box-shadow: @apple-shadow-large;
  border: 1px solid @apple-border;
  background: @apple-background;
  backdrop-filter: blur(20px);
  
  .ant-select-dropdown-menu {
    border-radius: @apple-radius-large;
    padding: 12px;
    background: transparent;
    
    .ant-select-dropdown-menu-item {
      border-radius: @apple-radius-medium;
      margin-bottom: 4px;
      padding: 12px 16px;
      transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
      background: transparent;
      
      &:hover {
        background: @apple-gray-1;
        transform: scale(1.02);
      }
      
      &.ant-select-dropdown-menu-item-selected {
        background: linear-gradient(135deg, rgba(0, 122, 255, 0.1) 0%, rgba(90, 200, 250, 0.05) 100%);
        color: @apple-blue;
        font-weight: 600;
        border: 1px solid rgba(0, 122, 255, 0.2);
      }
      
      &:last-child {
        margin-bottom: 0;
      }
      
      .user-option {
        display: flex;
        align-items: center;
        gap: 16px;
        
        .user-avatar {
          width: 36px;
          height: 36px;
          border-radius: @apple-radius-medium;
          background: linear-gradient(135deg, @apple-blue 0%, @apple-blue-light 100%);
          color: white;
          display: flex;
          align-items: center;
          justify-content: center;
          font-weight: 700;
          font-size: 15px;
          flex-shrink: 0;
          box-shadow: @apple-shadow-small;
          letter-spacing: -0.01em;
        }
        
        .user-name {
          font-size: 15px;
          color: @apple-text-primary;
          font-weight: 500;
          font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Helvetica Neue', sans-serif;
          letter-spacing: -0.01em;
        }
      }
    }
  }
}

/* 全局下拉框箭头动画 */
/deep/ .ant-select-open .ant-select-arrow {
  transform: rotate(180deg);
}

/* 滚动条样式 */
/deep/ .apple-select-dropdown .ant-select-dropdown-menu {
  &::-webkit-scrollbar {
    width: 6px;
  }
  
  &::-webkit-scrollbar-track {
    background: transparent;
  }
  
  &::-webkit-scrollbar-thumb {
    background: @apple-gray-4;
    border-radius: 3px;
    
    &:hover {
      background: @apple-gray-5;
    }
  }
}
</style>