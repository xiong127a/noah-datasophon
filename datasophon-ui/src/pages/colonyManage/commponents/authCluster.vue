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
  <div class="cluster-auth-content">
    <div class="auth-top">
      <div class="blue-icon">
        <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M12 12C14.7614 12 17 9.76142 17 7C17 4.23858 14.7614 2 12 2C9.23858 2 7 4.23858 7 7C7 9.76142 9.23858 12 12 12Z" fill="white"/>
          <path d="M12 14C7.58172 14 4 17.5817 4 22H20C20 17.5817 16.4183 14 12 14Z" fill="white"/>
        </svg>
      </div>
      <div class="auth-info">
        <div class="title">集群授权管理</div>
        <div class="desc">为集群 <span>{{ detail.clusterName || '未知集群' }}</span> 分配管理员权限</div>
      </div>
    </div>
    
    <div class="auth-form">
      <div class="form-item">
        <div class="label">选择管理员：</div>
        <div class="select-container">
          <a-form :form="form">
            <a-select 
              class="admin-select"
              mode="multiple" 
              v-decorator="['userIds', { rules: [{ required: false }]}]"
              placeholder="请选择一个或多个集群管理员"
              :dropdownMatchSelectWidth="false"
              :getPopupContainer="() => $refs.selectContainer"
              dropdownClassName="admin-dropdown"
            >
              <a-select-option 
                v-for="item in userList" 
                :key="item.id"
                :value="item.id"
              >
                {{ item.username }}
              </a-select-option>
            </a-select>
          </a-form>
        </div>
      </div>
    </div>

    <div class="auth-btns">
      <a-button type="primary" @click="handleSubmit" :loading="loading" class="apple-btn apple-btn-primary">
        确认授权
      </a-button>
      <a-button @click="formCancel" class="apple-btn apple-btn-default">
        取消
      </a-button>
    </div>
    <div ref="selectContainer" class="popup-container"></div>
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
    callBack: Function
  },
  data() {
    return {
      form: this.$form.createForm(this),
      loading: false,
      userList: []
    };
  },
  methods: {
    formCancel() {
      this.$destroyAll();
    },
    handleSubmit(e) {
      e.preventDefault();
      const _this = this;
      this.form.validateFields((err, values) => {
        if (!err) {
          const params = {
            "userIds": values.userIds || ""
          };
          if (JSON.stringify(this.detail) !== '{}') {
            params.clusterId = this.detail.id;
          }
          
          this.loading = true;
          this.$axiosPost(global.API.authCluster, params)
            .then((res) => {
              this.loading = false;
              if (res.code === 200) {
                if (params.userIds.length > 0) {
                  this.$message.success('授权成功', 2);
                } else {
                  this.$message.success('取消授权成功', 2);
                }
                this.$destroyAll();
                _this.callBack();
              }
            })
            .catch(() => {
              this.loading = false;
            });
        }
      });
    },
    queryAllUser() {
      this.$axiosPost(global.API.queryAllUser, {})
        .then((res) => {
          if (res.code === 200) {
            this.userList = res.data;
          }
        });
    }
  },
  mounted() {
    this.queryAllUser();
  }
};
</script>

<style lang="less" scoped>
@apple-blue: #0A84FF;
@apple-blue-light: #5AC8FA;
@apple-blue-dark: #0062CC;
@apple-gray-1: #F2F2F7;
@apple-gray-2: #E5E5EA;
@apple-gray-3: #D1D1D6;
@apple-gray-4: #C7C7CC;
@apple-radius: 10px;
@apple-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);

.cluster-auth-content {
  padding: 0;
  position: relative;
}

.auth-top {
  display: flex;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  align-items: center;
  
  .blue-icon {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    background: linear-gradient(135deg, #0A84FF 0%, #30B0FF 100%);
    margin-right: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 4px 12px rgba(10, 132, 255, 0.25);
    
    svg {
      width: 24px;
      height: 24px;
    }
  }
  
  .auth-info {
    .title {
      font-size: 18px;
      font-weight: 600;
      color: #000000;
      margin-bottom: 6px;
    }
    
    .desc {
      font-size: 14px;
      color: #666666;
      
      span {
        color: @apple-blue;
        font-weight: 500;
      }
    }
  }
}

.auth-form {
  padding: 24px;
  
  .form-item {
    margin-bottom: 16px;
    
    .label {
      margin-bottom: 10px;
      font-weight: 500;
      font-size: 15px;
    }
    
    .select-container {
      position: relative;
      width: 100%;
    }
  }
}

.auth-btns {
  padding: 20px 24px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: center;
  gap: 16px;
  background-color: #fafafa;
}

.popup-container {
  position: absolute;
  z-index: 1060;
}
</style>

<style>
/* 全局样式 */
.auth-cluster-modal .ant-modal-content {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.auth-cluster-modal .ant-modal-body {
  padding: 0 !important;
}

.auth-cluster-modal .ant-modal-confirm-body-wrapper {
  padding: 0;
}

.auth-cluster-modal .ant-modal-confirm-body {
  padding: 0;
  margin: 0;
}

.auth-cluster-modal .ant-modal-confirm-content {
  margin: 0 !important;
  padding: 0 !important;
}

.auth-cluster-modal .ant-modal-confirm-btns {
  display: none;
}

/* 修复下拉菜单容器样式 */
.popup-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  z-index: 1050;
}

.select-container {
  position: relative;
  width: 100%;
}

/* 下拉菜单样式 */
.admin-dropdown {
  border-radius: 12px !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12) !important;
  padding: 6px !important;
  border: 1px solid rgba(0, 0, 0, 0.05) !important;
  overflow: hidden !important;
}

.admin-dropdown .ant-select-dropdown-menu {
  max-height: 240px;
}

.admin-dropdown .ant-select-dropdown-menu-item {
  padding: 10px 12px !important;
  transition: all 0.2s !important;
  border-radius: 8px !important;
  margin: 2px 4px !important;
}

.admin-dropdown .ant-select-dropdown-menu-item:hover {
  background-color: #f0f7ff !important;
}

.admin-dropdown .ant-select-dropdown-menu-item-selected {
  background-color: rgba(10, 132, 255, 0.1) !important;
  color: #0A84FF !important;
  font-weight: 600 !important;
}

/* 修复选择框样式 */
.admin-select {
  width: 100%;
}

.admin-select .ant-select-selection {
  border-radius: 10px;
  border: 1px solid #D1D1D6;
  min-height: 44px;
  padding: 6px 8px 2px;
  transition: all 0.3s;
}

.admin-select .ant-select-selection:hover {
  border-color: #0A84FF;
  box-shadow: 0 0 0 2px rgba(10, 132, 255, 0.15);
}

.admin-select .ant-select-selection--multiple .ant-select-selection__rendered {
  margin-left: 4px;
  margin-bottom: 5px;
}

.admin-select .ant-select-selection__placeholder {
  margin-left: 4px;
  color: #999;
}

/* 标签样式 - 完全重写 */
.admin-select .ant-select-selection__choice {
  background: linear-gradient(135deg, #0A84FF 0%, #30B0FF 100%) !important;
  border: none !important;
  border-radius: 8px !important;
  color: white !important;
  height: 32px !important;
  line-height: 32px !important;
  margin-top: 3px !important;
  margin-bottom: 3px !important;
  margin-right: 8px !important;
  position: relative !important;
  display: flex !important;
  align-items: center !important;
  padding-right: 28px !important;
  padding-left: 12px !important;
  box-shadow: 0 2px 6px rgba(10, 132, 255, 0.25) !important;
}

.admin-select .ant-select-selection__choice__content {
  margin: 0 !important;
  padding: 0 !important;
  font-size: 14px !important;
  font-weight: 600 !important;
  line-height: 32px !important;
  text-align: center !important;
  display: block !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
  white-space: nowrap !important;
}

.admin-select .ant-select-selection__choice__remove {
  position: absolute !important;
  right: 9px !important;
  top: 0 !important;
  height: 32px !important;
  line-height: 32px !important;
  color: white !important;
  font-size: 12px !important;
  font-weight: bold !important;
  opacity: 0.85 !important;
}

.admin-select .ant-select-selection__choice__remove:hover {
  color: white !important;
  opacity: 1 !important;
}

/* 确保选择框中的搜索输入框正确对齐 */
.admin-select .ant-select-search--inline .ant-select-search__field {
  margin-top: 7px;
  margin-bottom: 4px;
  height: 22px;
}

/* 修正模态框居中问题 */
.auth-cluster-modal {
  display: flex;
  justify-content: center;
}

.auth-cluster-modal .ant-modal {
  top: 50%;
  margin: 0 auto;
  padding-bottom: 0;
}

/* 优化按钮区域样式 */
.auth-btns {
  padding: 20px 24px !important;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: center !important;
  gap: 16px !important;
  background-color: #fafafa;
}

.apple-btn {
  min-width: 120px !important;
  height: 40px;
  border-radius: 10px;
  font-weight: 600;
  border: none;
  transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
  font-size: 14px;
  letter-spacing: 0.3px;
}

.apple-btn-primary {
  background: linear-gradient(135deg, #0A84FF 0%, #30B0FF 100%) !important;
  color: white !important;
  box-shadow: 0 4px 12px rgba(10, 132, 255, 0.3) !important;
}

.apple-btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(10, 132, 255, 0.4) !important;
  background: linear-gradient(135deg, #0078F0 0%, #30B0FF 100%) !important;
}

.apple-btn-primary:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(10, 132, 255, 0.35) !important;
  background: linear-gradient(135deg, #0062CC 0%, #0A84FF 100%) !important;
}

.apple-btn-default {
  background: #ffffff !important;
  color: #464646 !important;
  border: 1px solid #D1D1D6 !important;
}

.apple-btn-default:hover {
  background: white !important;
  color: #0A84FF !important;
  border-color: #0A84FF !important;
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05) !important;
}

.apple-btn-default:active {
  background: #F2F2F7 !important;
  color: #0062CC !important;
  transform: translateY(0);
}
</style>