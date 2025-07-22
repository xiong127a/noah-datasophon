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
  <div class="cluster-auth-modal no-question-icons">
    <!-- 顶部区域 -->
    <div class="auth-header">
      <div class="auth-header-content">
        <div class="auth-icon-wrapper">
          <div class="auth-icon">
        <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M12 12C14.7614 12 17 9.76142 17 7C17 4.23858 14.7614 2 12 2C9.23858 2 7 4.23858 7 7C7 9.76142 9.23858 12 12 12Z" fill="white"/>
          <path d="M12 14C7.58172 14 4 17.5817 4 22H20C20 17.5817 16.4183 14 12 14Z" fill="white"/>
        </svg>
      </div>
        </div>
        <div class="auth-title">
          <h1>集群授权管理</h1>
          <p>为集群 <span class="cluster-name">{{ detail.clusterName || '未知集群' }}</span> 分配管理员权限</p>
        </div>
      </div>
    </div>
    
    <!-- 内容区域 -->
    <div class="auth-content">
    <div class="auth-form">
        <div class="form-group">
          <label class="form-label">选择管理员：</label>
          <div class="select-area">
            <a-form :form="form" v-if="userListLoaded">
            <a-select 
              class="admin-select"
              mode="multiple" 
                :defaultValue="selectedUserIds"
              placeholder="请选择一个或多个集群管理员"
              :dropdownMatchSelectWidth="false"
                :getPopupContainer="triggerNode => triggerNode.parentElement"
                dropdownClassName="admin-dropdown apple-dropdown"
                @change="handleUserSelectionChange"
                optionFilterProp="children"
                optionLabelProp="children"
                :placement="'bottomLeft'"
                :dropdownStyle="{ minWidth: '250px' }"
                :maxTagCount="maxTagCount"
                :maxTagPlaceholder="tagPlaceholder"
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
            <div v-else class="loading-state">
              <div class="loading-spinner"></div>
              <span>加载用户数据中...</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部按钮 -->
    <div class="auth-footer">
      <div class="auth-actions">
        <a-button 
          type="primary" 
          @click="handleSubmit" 
          :loading="loading" 
          class="primary-btn"
        >
          <span class="btn-content">确认授权</span>
      </a-button>
        <a-button 
          @click="formCancel" 
          class="cancel-btn"
        >
          <span class="btn-content">取消</span>
      </a-button>
      </div>
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
      userListLoaded: false, // 标记用户列表是否已加载
      maxTagCount: 3,
    };
  },
  methods: {
    // 处理标签显示
    tagPlaceholder(tags) {
      return `还有 ${tags.length} 个管理员`;
    },
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
  }
};
</script>

<style lang="less" scoped>
// 主题颜色变量 - 增强版
@primary-color: #1890ff;
@primary-gradient: linear-gradient(135deg, #40a9ff 0%, #1890ff 50%, #096dd9 100%);
@primary-gradient-hover: linear-gradient(135deg, #69c0ff 0%, #40a9ff 50%, #1890ff 100%);
@primary-shadow: 0 6px 16px rgba(24, 144, 255, 0.25);
@text-color: #262626;
@text-color-secondary: #595959;
@text-color-light: #8c8c8c;
@border-color: #e8e8e8;
@border-radius-base: 10px;
@border-radius-lg: 20px;
@shadow-color: rgba(0, 0, 0, 0.08);
@shadow-color-darker: rgba(0, 0, 0, 0.12);
@box-shadow: 0 6px 16px @shadow-color;
@modal-padding: 24px;
@animation-duration: 0.3s;
@animation-easing: cubic-bezier(0.2, 0, 0.38, 1);
@component-background: #fff;

// 模态框主容器
.cluster-auth-modal {
  width: 100%;
  background-color: @component-background;
  border-radius: @border-radius-base;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: @box-shadow;
  position: relative;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 4px;
    background: @primary-gradient;
    z-index: 3;
  }
}

// 顶部区域 - 强化色彩和层次
.auth-header {
  padding: 0;
  position: relative;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: @primary-gradient;
    z-index: 0;
  }
  
  &::after {
    content: '';
    position: absolute;
    top: -100%;
    left: -100%;
    right: 0;
    bottom: 0;
    background: radial-gradient(circle, rgba(255, 255, 255, 0.3) 0%, rgba(255, 255, 255, 0) 70%);
    opacity: 0.7;
    z-index: 1;
    transform: rotate(-35deg);
  }
  
  .auth-header-content {
    position: relative;
    z-index: 2;
    display: flex;
    align-items: center;
    padding: 28px 32px;
  }
  
  .auth-icon-wrapper {
    margin-right: 20px;
    position: relative;
    
    &::before {
      content: '';
      position: absolute;
      top: -4px;
      left: -4px;
      right: -4px;
      bottom: -4px;
      border-radius: 50%;
      border: 2px solid rgba(255, 255, 255, 0.4);
      animation: pulse 2.5s infinite;
    }
    
    &::after {
      content: '';
      position: absolute;
      top: -8px;
      left: -8px;
      right: -8px;
      bottom: -8px;
      border-radius: 50%;
      border: 1px solid rgba(255, 255, 255, 0.2);
      animation: pulse 2.5s infinite 0.5s;
    }
    
    .auth-icon {
      width: 52px;
      height: 52px;
      background: rgba(255, 255, 255, 0.25);
      border-radius: 50%;
      display: flex;
      align-items: center;
    justify-content: center;
      position: relative;
      overflow: hidden;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      
      &::before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: linear-gradient(135deg, rgba(255, 255, 255, 0.4) 0%, rgba(255, 255, 255, 0) 100%);
        z-index: 1;
      }
    
    svg {
        width: 26px;
        height: 26px;
        position: relative;
        z-index: 2;
      }
    }
  }
  
  .auth-title {
    color: white;
    
    h1 {
      font-size: 22px;
      font-weight: 600;
      margin: 0 0 10px;
      text-shadow: 0 1px 3px rgba(0, 0, 0, 0.15);
    }
    
    p {
      font-size: 14px;
      margin: 0;
      opacity: 0.95;
      text-shadow: 0 1px 2px rgba(0, 0, 0, 0.15);
      
      .cluster-name {
        font-weight: 600;
        background: rgba(255, 255, 255, 0.25);
        border-radius: 6px;
        padding: 3px 8px;
        margin: 0 2px;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
      }
    }
  }
}

// 内容区域 - 强化质感
.auth-content {
  padding: @modal-padding;
  background: linear-gradient(180deg, #f8faff 0%, #f0f5ff 100%);
  flex: 1;
  position: relative;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(90deg, rgba(255, 255, 255, 0) 0%, rgba(255, 255, 255, 0.8) 50%, rgba(255, 255, 255, 0) 100%);
    z-index: 1;
}

.auth-form {
    max-width: 480px;
    margin: 0 auto;
    background-color: white;
    border-radius: @border-radius-base;
    box-shadow: 0 3px 10px rgba(0, 0, 0, 0.06);
    padding: 28px;
    border: 1px solid rgba(24, 144, 255, 0.1);
    
    .form-group {
      margin-bottom: 20px;
      
      &:last-child {
        margin-bottom: 0;
      }
      
      .form-label {
        display: block;
        margin-bottom: 12px;
        font-size: 15px;
      font-weight: 500;
        color: @text-color;
        position: relative;
        padding-left: 12px;
        
        &::before {
          content: '';
          position: absolute;
          left: 0;
          top: 50%;
          transform: translateY(-50%);
          width: 4px;
          height: 18px;
          background: @primary-gradient;
          border-radius: 2px;
        }
    }
    
      .select-area {
      position: relative;
        
        .admin-select {
      width: 100%;
    }
  }
}
  }
}

// 底部按钮区域 - 精致化
.auth-footer {
  padding: 24px 28px;
  background-color: white;
  border-top: 1px solid @border-color;
  display: flex;
  justify-content: center;
  
  .auth-actions {
    display: flex;
    gap: 18px;
}
}

// 按钮样式 - 超现代设计
.primary-btn, .cancel-btn {
  min-width: 130px;
  height: 42px;
  border-radius: @border-radius-lg !important;
  font-size: 15px;
  font-weight: 500;
  transition: all @animation-duration @animation-easing;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
  
  .btn-content {
    position: relative;
    z-index: 2;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.primary-btn {
  background: @primary-gradient !important;
  border: none !important;
  color: white !important;
  box-shadow: @primary-shadow !important;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: linear-gradient(135deg, rgba(255, 255, 255, 0.3) 0%, rgba(255, 255, 255, 0) 80%);
    opacity: 0;
    transition: opacity @animation-duration;
    z-index: 1;
}

  &:hover {
    transform: translateY(-3px) !important;
    box-shadow: 0 8px 20px rgba(24, 144, 255, 0.35) !important;
    
    &::before {
      opacity: 1;
    }
  }
  
  &:active {
    transform: translateY(-1px) !important;
    box-shadow: 0 5px 10px rgba(24, 144, 255, 0.3) !important;
  }
}

.cancel-btn {
  border: 1px solid #e0e0e0 !important;
  color: @text-color-secondary !important;
  background: white !important;
  
  &::before {
    content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
    height: 100%;
    background: linear-gradient(135deg, rgba(24, 144, 255, 0.05) 0%, rgba(24, 144, 255, 0) 80%);
    opacity: 0;
    transition: opacity @animation-duration;
    z-index: 1;
  }
  
  &:hover {
    border-color: @primary-color !important;
    color: @primary-color !important;
    transform: translateY(-3px) !important;
    box-shadow: 0 6px 12px rgba(0, 0, 0, 0.08) !important;
    
    &::before {
      opacity: 1;
}
  }
  
  &:active {
    transform: translateY(-1px) !important;
    box-shadow: 0 3px 6px rgba(0, 0, 0, 0.05) !important;
  }
}

// 加载状态 - 精美化
.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 44px;
  padding: 0 15px;
  background-color: #f9faff;
  border: 1px solid rgba(24, 144, 255, 0.15);
  border-radius: @border-radius-base;
  color: @text-color-light;
  
  .loading-spinner {
    width: 18px;
    height: 18px;
    border: 2px solid rgba(24, 144, 255, 0.1);
    border-top-color: @primary-color;
    border-radius: 50%;
    margin-right: 10px;
    animation: spin 0.8s linear infinite;
  }
}

// 动画
@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 0.7;
  }
  50% {
    transform: scale(1.12);
    opacity: 0.4;
  }
  100% {
    transform: scale(1);
    opacity: 0.7;
}
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>

<style>
/* 全局样式 - 重构下拉菜单和选择器 */
.admin-dropdown {
  border-radius: 12px !important;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15), 0 6px 12px rgba(24, 144, 255, 0.1) !important;
  padding: 8px !important;
  border: 1px solid rgba(24, 144, 255, 0.1) !important;
  overflow: hidden !important;
  animation: dropdown-slide-down 0.25s cubic-bezier(0.2, 0, 0.38, 1) !important;
  min-width: 250px !important;
  margin-top: 8px !important;
}

/* 修复下拉菜单动画和位置，确保始终向下 */
.ant-select-dropdown {
  animation-name: dropdown-slide-down !important;
  transform-origin: top !important;
}

@keyframes dropdown-slide-down {
  from {
    opacity: 0;
    transform: scaleY(0.8);
  }
  to {
    opacity: 1;
    transform: scaleY(1);
  }
}

/* 下拉菜单项样式 */
.admin-dropdown .ant-select-dropdown-menu {
  max-height: 250px !important;
  padding: 4px !important;
}

.admin-dropdown .ant-select-dropdown-menu-item {
  padding: 10px 14px !important;
  transition: all 0.2s !important;
  border-radius: 10px !important;
  margin: 2px 4px !important;
  font-size: 14px !important;
  color: #333 !important;
}

.admin-dropdown .ant-select-dropdown-menu-item:hover {
  background-color: #f0f7ff !important;
  color: #1890ff !important;
}

.admin-dropdown .ant-select-dropdown-menu-item-selected {
  background-color: rgba(24, 144, 255, 0.1) !important;
  color: #1890ff !important;
  font-weight: 500 !important;
}

.admin-dropdown .ant-select-dropdown-menu-item-active {
  background-color: rgba(24, 144, 255, 0.05) !important;
}

/* 关键修复：选择框容器样式 */
.admin-select {
  width: 100% !important;
}

.admin-select .ant-select-selection {
  border-radius: 10px !important;
  border: 1px solid #e8e8e8 !important;
  height: 44px !important; /* 固定高度 */
  min-height: 44px !important;
  max-height: 44px !important;
  padding: 0 !important; /* 移除内边距，完全由内部元素控制 */
  transition: all 0.3s cubic-bezier(0.2, 0, 0.38, 1) !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04) !important;
  position: relative !important; /* 为绝对定位子元素准备 */
}

/* 完全重构渲染区域，使用绝对定位 */
.admin-select .ant-select-selection--multiple .ant-select-selection__rendered {
  position: absolute !important;
  top: 0 !important;
  left: 0 !important;
  right: 0 !important;
  bottom: 0 !important;
  height: auto !important;
  margin: 0 !important;
  padding: 0 8px !important;
  display: flex !important;
  align-items: center !important; /* 垂直居中 */
  justify-content: flex-start !important;
  flex-wrap: nowrap !important;
  overflow: hidden !important;
}

/* 标签样式调整 */
.admin-select .ant-select-selection__choice {
  background: linear-gradient(135deg, #40a9ff 0%, #1890ff 100%) !important;
  border: none !important;
  border-radius: 14px !important;
  color: white !important;
  height: 28px !important;
  line-height: 28px !important;
  margin: 0 6px 0 0 !important; /* 只保留水平间距 */
  position: relative !important;
  display: inline-flex !important;
  align-items: center !important;
  padding: 0 28px 0 12px !important;
  box-shadow: 0 2px 6px rgba(24, 144, 255, 0.3) !important;
  animation: tag-in 0.3s cubic-bezier(0.2, 0, 0.38, 1) !important;
  max-width: 150px !important;
  transition: all 0.2s !important;
  flex-shrink: 0 !important; /* 防止标签被压缩 */
}

/* 更多标签样式调整 */
.admin-select .ant-select-selection__choice.ant-select-selection__choice__disabled {
  background: linear-gradient(135deg, #f0f0f0 0%, #e0e0e0 100%) !important;
  color: #595959 !important;
  border: none !important;
  height: 28px !important;
  line-height: 28px !important;
  padding: 0 10px !important;
  margin: 0 6px 0 0 !important; /* 只保留水平间距 */
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1) !important;
  flex-shrink: 0 !important; /* 防止标签被压缩 */
}

/* 搜索框样式调整 */
.admin-select .ant-select-search--inline {
  height: 28px !important;
  margin: 0 !important;
  padding: 0 !important;
  display: inline-flex !important;
  align-items: center !important;
  flex-shrink: 1 !important; /* 允许搜索框被压缩 */
}

.admin-select .ant-select-search--inline .ant-select-search__field {
  margin: 0 !important;
  padding: 0 8px !important;
  height: 28px !important;
  line-height: 28px !important;
  min-width: 20px !important;
}

/* 标签包装器样式，确保垂直居中 */
.admin-select .ant-select-selection__rendered > ul {
  display: flex !important;
  align-items: center !important;
  flex-wrap: nowrap !important;
  height: 44px !important;
}

/* 确保箭头垂直居中 */
.admin-select .ant-select-arrow {
  top: 50% !important;
  margin-top: -6px !important;
}

/* 防止标签文本溢出 */
.admin-select .ant-select-selection__choice__content {
  margin: 0 !important;
  padding: 0 !important;
  font-size: 13px !important;
  font-weight: 500 !important;
  line-height: 28px !important;
  text-overflow: ellipsis !important;
  white-space: nowrap !important;
  overflow: hidden !important;
}

/* 确保选择框的高度不会轻易变化，保持最多显示一行标签 */
.admin-select .ant-select-selection--multiple {
  min-height: 44px !important;
  max-height: 44px !important;
  overflow: hidden !important;
}

/* 优化多标签显示 - 修复垂直居中问题 */
.admin-select .ant-select-selection--multiple .ant-select-selection__rendered {
  margin: 0 !important;
  height: 44px !important;
  overflow: hidden !important;
  line-height: 44px !important;
  display: flex !important;
  flex-wrap: nowrap !important;
  align-items: center !important;
  justify-content: flex-start !important;
  padding: 0 8px !important;
}

/* 超级优化标签样式 - 调整边距确保居中 */
.admin-select .ant-select-selection__choice {
  background: linear-gradient(135deg, #40a9ff 0%, #1890ff 100%) !important;
  border: none !important;
  border-radius: 14px !important;
  color: white !important;
  height: 28px !important;
  line-height: 28px !important;
  margin: 0 6px 0 0 !important;
  position: relative !important;
  display: inline-flex !important;
  align-items: center !important;
  padding: 0 28px 0 12px !important;
  box-shadow: 0 2px 6px rgba(24, 144, 255, 0.3) !important;
  animation: tag-in 0.3s cubic-bezier(0.2, 0, 0.38, 1) !important;
  max-width: 150px !important;
  transition: all 0.2s !important;
}

.admin-select .ant-select-selection__choice:hover {
  box-shadow: 0 3px 8px rgba(24, 144, 255, 0.4) !important;
  transform: translateY(-1px) !important;
}

@keyframes tag-in {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.admin-select .ant-select-selection__choice__content {
  margin: 0 !important;
  padding: 0 !important;
  font-size: 13px !important;
  font-weight: 500 !important;
  line-height: 28px !important;
  text-overflow: ellipsis !important;
  white-space: nowrap !important;
  overflow: hidden !important;
}

.admin-select .ant-select-selection__choice__remove {
  position: absolute !important;
  right: 8px !important;
  top: 0 !important;
  height: 28px !important;
  width: 20px !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  color: white !important;
  font-size: 12px !important;
  opacity: 0.8 !important;
  transition: all 0.2s !important;
  transform: scale(1) !important;
}

.admin-select .ant-select-selection__choice__remove:hover {
  color: white !important;
  opacity: 1 !important;
  transform: scale(1.2) !important;
}

/* 更多标签的样式 - 调整边距确保居中 */
.admin-select .ant-select-selection__choice.ant-select-selection__choice__disabled {
  background: linear-gradient(135deg, #f0f0f0 0%, #e0e0e0 100%) !important;
  color: #595959 !important;
  border: none !important;
  height: 28px !important;
  line-height: 28px !important;
  padding: 0 10px !important;
  margin: 0 6px 0 0 !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1) !important;
}

/* 确保搜索框样式 - 调整边距确保居中 */
.admin-select .ant-select-search--inline {
  margin: 0 !important;
  height: 28px !important;
  line-height: 28px !important;
  display: inline-flex !important;
  align-items: center !important;
}

.admin-select .ant-select-search--inline .ant-select-search__field {
  margin: 0 !important;
  padding: 0 8px !important;
  height: 28px !important;
  line-height: 28px !important;
  min-width: 100px !important;
}

/* 确保下拉菜单始终在下方 */
.ant-select-dropdown--multiple.ant-select-dropdown-placement-bottomLeft,
.ant-select-dropdown.ant-select-dropdown--multiple.ant-select-dropdown-placement-bottomLeft,
.ant-select-dropdown.ant-select-dropdown-placement-bottomLeft {
  top: 100% !important;
  left: 0 !important;
  transform-origin: 0 0 !important;
}

/* 覆盖可能的上方弹出样式 */
.ant-select-dropdown.slide-up-enter.slide-up-enter-active.ant-select-dropdown-placement-topLeft,
.ant-select-dropdown.slide-up-appear.slide-up-appear-active.ant-select-dropdown-placement-topLeft {
  animation-name: dropdown-slide-down !important;
  transform-origin: bottom !important;
  top: 100% !important;
  bottom: auto !important;
}

/* 模态框动画 */
.auth-cluster-modal .ant-modal-content {
  animation: modal-in 0.35s cubic-bezier(0.2, 0, 0.38, 1) !important;
  border-radius: 16px !important;
  overflow: hidden !important;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15), 0 10px 20px rgba(0, 0, 0, 0.1) !important;
}

@keyframes modal-in {
  from {
    opacity: 0;
    transform: scale(0.96);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

/* 修复模态框边距问题 */
.ant-modal-body {
  padding: 0 !important;
}
</style>