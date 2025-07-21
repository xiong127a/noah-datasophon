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
  <div class="cluster-auth-content no-question-icons">
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
          <a-form :form="form" v-if="userListLoaded">
            <a-select 
              class="admin-select"
              mode="multiple" 
              :defaultValue="selectedUserIds"
              placeholder="请选择一个或多个集群管理员"
              :dropdownMatchSelectWidth="false"
              :getPopupContainer="triggerNode => triggerNode.parentElement"
              dropdownClassName="admin-dropdown"
              @change="handleUserSelectionChange"
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
          <div v-else class="loading-placeholder">
            加载用户列表中...
          </div>
        </div>
      </div>
    </div>

    <div class="auth-btns">
      <a-button type="primary" @click="handleSubmit" :loading="loading" class="auth-btn auth-btn-primary">
        确认授权
      </a-button>
      <a-button @click="formCancel" class="auth-btn auth-btn-default">
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
    callBack: Function
  },
  data() {
    // 提前从props中提取managerIds
    let selectedUserIds = [];
    if (this.detail && 
        this.detail.clusterManagerList && 
        Array.isArray(this.detail.clusterManagerList)) {
      selectedUserIds = this.detail.clusterManagerList.map(manager => manager.id);
    }
    
    return {
      form: this.$form.createForm(this),
      loading: false,
      userList: [],
      selectedUserIds: selectedUserIds, // 直接初始化为提取的值
      userListLoaded: false // 标记用户列表是否已加载
    };
  },
  methods: {
    formCancel() {
      // 不再使用$destroyAll()
      this.$emit('cancel');
    },
    // 处理用户选择变更
    handleUserSelectionChange(selectedValues) {
      this.selectedUserIds = selectedValues;
    },
    
    // 查询所有用户
    queryAllUser() {
      this.$axiosPost(global.API.queryAllUser, {})
        .then((res) => {
          if (res.code === 200) {
            this.userList = res.data;
            // 标记用户列表已加载
            this.$nextTick(() => {
              this.userListLoaded = true;
            });
          }
        });
    },
    
    handleSubmit(e) {
      e.preventDefault();
      const _this = this;
      this.form.validateFields((err, values) => {
        if (!err) {
          // 检查clusterId是否存在
          if (!this.detail || !this.detail.id) {
            this.$message.error('缺少集群ID参数', 2);
            return;
          }
          
          // 直接使用this.selectedUserIds，而不是从values中取值
          // 这样可以确保即使表单获取失败，也能使用组件内部跟踪的值
          const userIds = this.selectedUserIds || [];
          let userIdsString = '';
          
          // 转换用户ID数组为字符串
          if (Array.isArray(userIds)) {
            userIdsString = userIds.join(',');
          } else {
            userIdsString = userIds.toString();
          }
          
          // 构建URL查询参数
          const url = `${global.API.authCluster}?clusterId=${this.detail.id}&userIds=${userIdsString}`;
          
          this.loading = true;
          // 使用get方法，通过URL传参
          this.$axiosGet(url)
            .then((res) => {
              this.loading = false;
              if (res.code === 200) { 
                if (userIds && userIds.length > 0) {
                  this.$message.success('授权成功', 2);
                } else {
                  this.$message.success('取消授权成功', 2);
                }
                // 调用callBack并关闭模态框
                if (this.callBack) {
                  this.callBack();
                } else {
                  // 如果没有传入callBack，则直接触发cancel事件
                  this.$emit('cancel');
                }
              } else {
                this.$message.error(res.msg || '授权失败', 2);
              }
            })
            .catch((error) => {
              this.loading = false;
              this.$message.error('授权失败，请检查网络或参数', 2);
            });
        }
      });
    },
    // 在组件内部也添加图标清除逻辑
    removeQuestionIcons() {
      const selectors = [
        '.anticon-question-circle',
        'i.anticon',
        'svg[data-icon="question-circle"]',
        '[aria-label="icon: question-circle"]',
        '.ant-modal-confirm-title + i',
        '.ant-modal-confirm-body i',
        '.ant-modal-header i',
        '.ant-modal-body i.anticon'
      ];
      
      selectors.forEach(selector => {
        const elements = document.querySelectorAll(selector);
        elements.forEach(el => {
          if (el && el.parentNode) {
            try {
              el.style.display = 'none';
              el.style.visibility = 'hidden';
              el.style.width = '0';
              el.style.height = '0';
              el.style.position = 'absolute';
              el.style.top = '-9999px';
              el.parentNode.removeChild(el);
            } catch (e) {
              console.log('移除图标失败，但已隐藏');
            }
          }
        });
        });
    }
  },
  created() {
    // 在created钩子中提取管理员，而不是在mounted后
    if (this.detail && 
        this.detail.clusterManagerList && 
        Array.isArray(this.detail.clusterManagerList)) {
      this.selectedUserIds = this.detail.clusterManagerList.map(manager => manager.id);
    }
  },
  mounted() {
    // 原始代码
    this.queryAllUser();
    
    // 组件挂载后移除图标
    this.$nextTick(() => {
      this.removeQuestionIcons();
      // 设置多次检查，确保任何动态渲染的图标也被移除
      setTimeout(() => this.removeQuestionIcons(), 50);
      setTimeout(() => this.removeQuestionIcons(), 200);
    });
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
  max-width: 100%;
  overflow: hidden;
  width: 100%;
  margin: 0 auto; /* 确保居中 */
  background-color: white;
  min-width: 316px; /* 确保最小宽度符合设计 */
}

.auth-top {
  display: flex;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  align-items: center;
  width: 100%;
  justify-content: center;
  
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
    flex-shrink: 0;
    
    svg {
      width: 24px;
      height: 24px;
    }
  }
  
  .auth-info {
    flex: 1;
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
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  
  .form-item {
    margin-bottom: 16px;
    width: 100%;
    max-width: 350px; /* 限制表单宽度，更好看 */
    
    .label {
      margin-bottom: 10px;
      font-weight: 500;
      font-size: 15px;
      text-align: left;
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
  width: 100%;
}

.auth-btn {
  min-width: 120px !important;
  height: 40px !important;
  border-radius: 10px !important;
  font-weight: 600 !important;
  font-size: 14px !important;
  letter-spacing: 0.3px !important;
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  padding: 0 16px !important;
  box-sizing: border-box !important;
}

.auth-btn-primary {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%) !important;
  border: none !important;
  color: white !important;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.3) !important;
}

.auth-btn-primary:hover {
  background: linear-gradient(135deg, #40a9ff 0%, #1890ff 100%) !important;
  transform: translateY(-2px) !important;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.4) !important;
}

.auth-btn-primary:active {
  background: linear-gradient(135deg, #096dd9 0%, #1890ff 100%) !important;
  transform: translateY(0) !important;
  box-shadow: 0 2px 6px rgba(24, 144, 255, 0.35) !important;
}

.auth-btn-default {
  background: #ffffff !important;
  color: #464646 !important;
  border: 1px solid #e6e6e6 !important;
}

.auth-btn-default:hover {
  background: white !important;
  color: #1890ff !important;
  border-color: #1890ff !important;
  transform: translateY(-2px) !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05) !important;
}

.auth-btn-default:active {
  background: #f2f2f7 !important;
  color: #096dd9 !important;
  transform: translateY(0) !important;
}

.popup-container {
  position: absolute;
  z-index: 1060;
}

/* 添加加载占位符样式 */
.loading-placeholder {
  height: 44px;
  border: 1px solid #d9d9d9;
  border-radius: 10px;
  padding: 0 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  background-color: #f9f9f9;
}
</style>

<style>
/* 全局样式 - 彻底修复紫色容器问题 */
.auth-cluster-modal .ant-modal-content {
  border-radius: 16px !important;
  overflow: hidden !important;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15) !important;
  border: 1px solid rgba(0, 0, 0, 0.05) !important;
}

.auth-cluster-modal .ant-modal-body {
  padding: 0 !important;
}

/* 极端处理：直接隐藏所有问号图标，包括蓝色问号 */
.anticon-question-circle,
i.anticon-question-circle,
svg[data-icon="question-circle"],
[aria-label="icon: question-circle"],
svg[viewBox="64 64 896 896"][data-icon="question-circle"],
.ant-modal-confirm-title-wrap i.anticon,
.ant-modal-header i.anticon,
.ant-modal-body i.anticon {
  display: none !important;
  width: 0 !important;
  height: 0 !important;
  opacity: 0 !important;
  visibility: hidden !important;
  position: absolute !important;
  top: -9999px !important;
  left: -9999px !important;
}

/* 特定处理SVG路径 */
path[d="M512 64C264.6 64 64 264.6 64 512s200.6 448 448 448 448-200.6 448-448S759.4 64 512 64zm0 820c-205.4 0-372-166.6-372-372s166.6-372 372-372 372 166.6 372 372-166.6 372-372 372z"] {
  display: none !important;
}

path[d="M623.6 316.7C593.6 290.4 554 276 512 276s-81.6 14.5-111.6 40.7C369.2 344 352 380.7 352 420v7.6c0 4.4 3.6 8 8 8h48c4.4 0 8-3.6 8-8V420c0-44.1 43.1-80 96-80s96 35.9 96 80c0 31.1-22 59.6-56.1 72.7-21.2 8.1-39.2 22.3-52.1 40.9-13.1 19-19.9 41.8-19.9 64.9V620c0 4.4 3.6 8 8 8h48c4.4 0 8-3.6 8-8v-22.7a48.3 48.3 0 0 1 30.9-44.8c59-22.7 97.1-74.7 97.1-132.5.1-39.3-17.1-76-48.3-103.3zM472 732a40 40 0 1 0 80 0 40 40 0 1 0-80 0z"] {
  display: none !important;
}

/* 修复下拉菜单样式 */
.admin-dropdown {
  border-radius: 12px !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12) !important;
  padding: 6px !important;
  border: 1px solid rgba(0, 0, 0, 0.05) !important;
  overflow: hidden !important;
}

.admin-dropdown .ant-select-dropdown-menu {
  max-height: 240px !important;
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
  width: 100% !important;
}

.admin-select .ant-select-selection {
  border-radius: 10px !important;
  border: 1px solid #D1D1D6 !important;
  min-height: 44px !important;
  padding: 6px 8px 2px !important;
  transition: all 0.3s !important;
}

.admin-select .ant-select-selection:hover {
  border-color: #0A84FF !important;
  box-shadow: 0 0 0 2px rgba(10, 132, 255, 0.15) !important;
}

.admin-select .ant-select-selection--multiple .ant-select-selection__rendered {
  margin-left: 4px !important;
  margin-bottom: 5px !important;
}

.admin-select .ant-select-selection__placeholder {
  margin-left: 4px !important;
  color: #999 !important;
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
  margin-top: 7px !important;
  margin-bottom: 4px !important;
  height: 22px !important;
}
</style>

<style scoped>
/* 在组件内部也添加清除图标的样式 */
.no-question-icons :deep(.anticon-question-circle),
.no-question-icons :deep(i.anticon),
.no-question-icons :deep(svg[data-icon="question-circle"]),
.no-question-icons :deep([aria-label="icon: question-circle"]) {
  display: none !important;
  visibility: hidden !important;
  width: 0 !important;
  height: 0 !important;
  opacity: 0 !important;
  position: absolute !important;
  top: -9999px !important;
  left: -9999px !important;
}

/* 确保布局正确 */
.cluster-auth-content {
  width: 100%;
  background: white;
  border-radius: 16px;
  overflow: hidden;
}

/* 隐藏所有可能显示问号图标的区域 */
:deep(.ant-modal-confirm-title),
:deep(.ant-modal-title),
:deep(.ant-modal-confirm-title-wrap) {
  position: relative;
}

:deep(.ant-modal-confirm-title)::before,
:deep(.ant-modal-title)::before,
:deep(.ant-modal-confirm-title-wrap)::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: white;
  z-index: 10;
}
</style>