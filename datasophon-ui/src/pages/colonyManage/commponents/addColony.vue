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
 * @LastEditTime: 2022-06-15 17:06:20
 * @FilePath: \ddh-ui\src\pages\colonyManage\commponents\addColony.vue
-->
<template>
  <div class="apple-form-container">
    <div class="form-header">
      <h1 class="form-title">{{ isEdit ? '编辑集群' : '创建新集群' }}</h1>
      <p class="form-subtitle">{{ isEdit ? '修改集群配置信息' : '配置您的大数据平台集群信息' }}</p>
    </div>
    
    <div class="form-cards">
      <a-form :form="form" layout="vertical" class="apple-form">
        <!-- 基本信息卡片 -->
        <div class="form-card">
          <div class="card-header">
            <h3 class="card-title">基本信息</h3>
            <p class="card-description">设置集群的基本标识信息</p>
          </div>
          <div class="card-content">
            <div class="form-row">
              <a-form-item label="集群名称" class="apple-form-item">
                <a-input 
                  v-decorator="[
                    'clusterName',
                    { rules: [{ required: true, message: '集群名称不能为空!' }] },
                  ]" 
                  placeholder="请输入集群名称" 
                  class="apple-input"
                />
              </a-form-item>
              <a-form-item label="集群编码" class="apple-form-item">
                <a-input 
                  v-decorator="[
                    'clusterCode',
                    { rules: [{ required: true, message: '集群编码不能为空!' }] },
                  ]" 
                  :disabled="isEdit"
                  placeholder="请输入集群编码" 
                  class="apple-input"
                />
              </a-form-item>
            </div>
          </div>
        </div>

        <!-- 技术配置卡片 -->
        <div class="form-card">
          <div class="card-header">
            <h3 class="card-title">技术配置</h3>
            <p class="card-description">选择集群的技术框架和部署方式</p>
          </div>
          <div class="card-content">
            <a-form-item label="集群框架" class="apple-form-item" style="margin-bottom: 24px;">
                <a-select 
                v-decorator="[
                  'clusterFrame',
                  { rules: [{ required: true, message: '集群框架不能为空!' }] },
                ]"
                  placeholder="请选择集群框架"
                  class="apple-select"
                  :disabled="isEdit"
                >
                  <a-select-option :value="item.frameCode" v-for="(item, index) in frameList" :key="index">
                    {{ item.frameCode }}
                  </a-select-option>
                </a-select>
              </a-form-item>
              
              <a-form-item label="集群部署方式" class="apple-form-item deployment-type">
              <!-- 隐藏的表单字段用于验证 -->
              <a-input
                v-decorator="['depType', { rules: [{ required: true, message: '请选择集群部署方式!' }] }]"
                style="display: none;"
              />
              <div class="deployment-cards-container">
                <div class="deployment-cards">
                  <div
                    class="deployment-card"
                    :class="{ 'selected': selectedDepType === 'PVM' }"
                    @click="selectDepType('PVM')"
                    :disabled="isEdit"
                  >
                    <div class="card-icon">
                      <img src="~@/assets/img/os-logos/linux-tux.svg" alt="Bare Metal/VM" />
                    </div>
                    <div class="card-content">
                      <h4 class="card-title">裸金属/虚拟机</h4>
                      <p class="card-description">部署到Linux裸金属或虚拟机上</p>
                    </div>
                    <div class="card-check">
                      <a-icon type="check-circle" class="check-icon" />
                    </div>
                  </div>

                  <div
                    class="deployment-card"
                    :class="{ 'selected': selectedDepType === 'Kubernetes' }"
                    @click="selectDepType('Kubernetes')"
                    :disabled="isEdit"
                  >
                    <div class="card-icon k8s-icon">
                      <img src="~@/assets/images/kubernetes-logo.svg" alt="Kubernetes" />
                    </div>
                    <div class="card-content">
                      <h4 class="card-title">Kubernetes</h4>
                      <p class="card-description">容器化部署，支持自动化和弹性伸缩</p>
                    </div>
                    <div class="card-check">
                      <a-icon type="check-circle" class="check-icon" />
                    </div>
                  </div>
                </div>

                <div class="deployment-info-panel" v-if="selectedDepType">
                  <div class="info-content">
                    <div class="info-icon">
                      <a-icon type="info-circle" />
                    </div>
                    <div class="info-text">
                      <div v-if="selectedDepType === 'PVM'">
                        <strong>裸金属/虚拟机部署：</strong>适用于需要并行处理大规模计算任务的场景。
                      </div>
                      <div v-else-if="selectedDepType === 'Kubernetes'">
                        <strong>Kubernetes部署：</strong>适用于需要管理和部署容器化应用程序的场景。
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </a-form-item>
          </div>
        </div>
      </a-form>
    </div>
    
    <div class="form-actions">
      <a-button
        type="primary"
        @click.stop="handleSubmit"
        :loading="loading"
        class="apple-button primary"
      >
        <a-icon type="check" />
        {{ isEdit ? '保存修改' : '创建集群' }}
      </a-button>
      <a-button 
        @click.stop="formCancel"
        class="apple-button secondary"
      >
        <a-icon type="close" />
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
    return {
      labelCol: {
        xs: { span: 24 },
        sm: { span: 5 },
      },
      wrapperCol: {
        xs: { span: 24 },
        sm: { span: 19 },
      },
      form: this.$form.createForm(this),
      value1: "",
      loading: false,
      frameList: [], // 集群框架列表
      depTypeList: ['Kubernetes', 'PVM'], // 部署方式列表
      depType: '',
      selectedDepType: '', // 当前选中的部署方式
    };
  },
  computed: {
    // 判断是否为编辑模式
    isEdit() {
      return JSON.stringify(this.detail) !== '{}';
    }
  },
  watch: {
    detail: {
      immediate: true,
      handler(val) {
        if (JSON.stringify(val) !== '{}') {
          // 编辑模式，设置部署方式
          this.selectedDepType = val.depType;
          this.depType = val.depType;
        }
      }
    }
  },
  methods: {
    tochange(val) {
      this.depType = val;
    },
    selectDepType(type) {
      // 在编辑模式下不允许更改部署方式
      if (this.isEdit) return;
      
      this.selectedDepType = type;
      this.depType = type;
      // 手动设置表单字段值
      this.form.setFieldsValue({ depType: type });
    },
    formCancel() {
      this.$destroyAll();
    },
    handleSubmit(e) {
      const _this = this;
      e.preventDefault();
      this.form.validateFields((err, values) => {
        console.log(values);
        if (!err) {
          const params = {
            "clusterName": values.clusterName,
            "clusterCode": values.clusterCode,
            "clusterFrame": values.clusterFrame,
            "depType": values.depType,
          }
          if (this.isEdit) params.id = this.detail.id;
          this.loading = true;
          const ajaxApi = this.isEdit ? global.API.updateColony : global.API.saveColony;
          
          this.$axiosJsonPost(ajaxApi + (this.isEdit ? "?clusterId=" + this.detail.id : ""), params).then((res) => {
            this.loading = false;
            if (res.code === 200) {
              this.$message.success('保存成功', 2);
              this.$destroyAll();
              _this.callBack();
            }
          }).catch(() => {
            this.loading = false;
          });
        }
      });
    },
    getFrameList() {
      this.$axiosPost(global.API.getFrameList, {}).then((res) => {
        if (res.code === 200) {
          this.frameList = res.data;
          if (this.isEdit) {
            this.form.setFieldsValue({
              clusterName: this.detail.clusterName,
              clusterFrame: this.detail.clusterFrame,
              clusterCode: this.detail.clusterCode,
              depType: this.detail.depType,
            });
          }
        }
      });
    }
  },
  mounted() {
    this.getFrameList();
  },
};
</script>
<style lang="less" scoped>
// Apple设计系统颜色变量 - 优化版
@apple-blue: #0A84FF;
@apple-blue-hover: #0062CC;
@apple-blue-light: rgba(10, 132, 255, 0.1);
@apple-gray: #F2F2F7;
@apple-gray-light: #fafbfc;
@apple-white: #ffffff;
@apple-black: #1a1a1a;
@apple-text-primary: #1a1a1a;
@apple-text-secondary: #6b7280;
@apple-border: #D1D1D6;
@apple-border-light: rgba(0, 0, 0, 0.08);
@apple-radius: 12px; // 增加圆角值
@apple-radius-large: 16px;
@apple-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
@apple-shadow-hover: 0 8px 20px rgba(0, 0, 0, 0.12);

// Apple字体
.apple-font() {
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "PingFang SC", "Helvetica Neue", Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.apple-form-container {
  .apple-font();
  max-width: 100%;
  margin: 0 auto;
  padding: 24px; // 减小内边距
  border-radius: @apple-radius;
  
  .form-header {
    text-align: center;
    margin-bottom: 32px; // 减小下边距
    
    .form-title {
      .apple-font();
      font-size: 1.8rem; // 减小字体大小
      font-weight: 600;
      line-height: 1.1;
      color: @apple-text-primary;
      margin-bottom: 8px;
    }
    
    .form-subtitle {
      .apple-font();
      font-size: 1rem; // 减小字体大小
      line-height: 1.5;
      color: @apple-text-secondary;
      margin: 0;
      font-weight: 500;
    }
  }
  
  .form-cards {
    .form-card {
      background: @apple-white;
      border: 1px solid @apple-border;
      border-radius: @apple-radius;
      margin-bottom: 16px; // 减小卡片间距
      overflow: hidden;
      transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
      
      &:hover {
        box-shadow: @apple-shadow;
      }
      
      .card-header {
        padding: 16px 20px 12px; // 减小内边距
        border-bottom: 1px solid @apple-border-light;
        
        .card-title {
          .apple-font();
          font-size: 1.1rem; // 减小字体大小
          font-weight: 600;
          color: @apple-text-primary;
          margin: 0 0 6px 0;
          line-height: 1.2;
        }
        
        .card-description {
          .apple-font();
          font-size: 0.9rem; // 减小字体大小
          color: @apple-text-secondary;
          margin: 0;
          line-height: 1.4;
        }
      }
      
      .card-content {
        padding: 16px 20px; // 减小内边距
        
        .form-row {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 16px; // 减小表单项间距
          
          @media (max-width: 768px) {
            grid-template-columns: 1fr;
          }
        }
      }
    }
  }
  
  // 表单项样式
  .apple-form-item {
    margin-bottom: 0;
    
    /deep/ .ant-form-item-label {
      padding-bottom: 6px;
      
      label {
        .apple-font();
        font-size: 0.9rem;
        font-weight: 500;
        color: @apple-text-primary;
        
        // 替换必填星号为更优雅的样式
        &::before {
          content: '';
          display: none;
        }
        
        &::after {
          content: '';
          display: none;
        }
        
        // 必填项标签使用蓝色文字而非星号
        &.ant-form-item-required {
          &::before {
            display: none;
          }
          
          // 可以添加"必填"小标签
          &::after {
            content: '必填';
            display: inline-block;
            margin-left: 4px;
            font-size: 0.7rem;
            padding: 1px 5px;
            background-color: rgba(10, 132, 255, 0.1);
            color: @apple-blue;
            border-radius: 4px;
            line-height: 1.4;
            font-weight: normal;
            vertical-align: middle;
          }
        }
      }
    }
    
    // 部署方式特殊样式
    &.deployment-type {
      grid-column: 1 / -1;
      
      .deployment-cards-container {
        .deployment-cards {
          display: grid;
          grid-template-columns: repeat(2, 1fr);
          gap: 12px;
          margin-bottom: 12px;
          
          @media (max-width: 768px) {
            grid-template-columns: 1fr;
          }
        }
        
        .deployment-card {
          display: flex;
          align-items: center;
          position: relative;
          background: @apple-white;
          border: 1px solid @apple-border;
          border-radius: @apple-radius;
          padding: 12px 16px;
          cursor: pointer;
          transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
          height: 80px; // 限制卡片高度
          
          &:hover {
            border-color: @apple-blue;
            box-shadow: 0 4px 12px rgba(10, 132, 255, 0.15);
          }
          
          &.selected {
            border-color: @apple-blue;
            background: @apple-blue-light;
            box-shadow: 0 4px 12px rgba(10, 132, 255, 0.2);
        
            .card-check {
              opacity: 1;
              transform: scale(1);
            }
          }
          
          .card-icon {
            display: flex;
            align-items: center;
            justify-content: center;
            width: 36px;
            height: 36px;
            margin-right: 12px;
            flex-shrink: 0;
            
            img {
              width: 32px;
              height: 32px;
            }
          }
          
          .card-content {
            flex: 1;
            
            .card-title {
              .apple-font();
              font-size: 1rem;
              font-weight: 600;
              color: @apple-text-primary;
              margin: 0 0 4px 0;
              line-height: 1.2;
            }
        
            .card-description {
              .apple-font();
              font-size: 0.8rem;
              color: @apple-text-secondary;
              margin: 0;
              line-height: 1.4;
            }
          }
          
          .card-check {
            position: absolute;
            top: 12px;
            right: 12px;
            opacity: 0;
            transform: scale(0.8);
            transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
            
            .check-icon {
              color: @apple-blue;
              font-size: 18px;
            }
          }
        }
        
        .deployment-info-panel {
          background: rgba(10, 132, 255, 0.05);
          border: 1px solid rgba(10, 132, 255, 0.1);
          border-radius: @apple-radius;
          padding: 12px;
          margin-top: 8px;
          
          .info-content {
            display: flex;
            align-items: flex-start;
            gap: 8px;
            
            .info-icon {
              color: @apple-blue;
              font-size: 14px;
              margin-top: 2px;
            }
            
            .info-text {
              flex: 1;
              .apple-font();
              font-size: 0.85rem;
              line-height: 1.4;
              color: @apple-text-primary;
              
              strong {
                color: @apple-blue;
              }
            }
          }
        }
      }
    }
  }
  
  // 输入框样式
  .apple-input {
    /deep/ .ant-input {
      .apple-font();
      border: 1px solid @apple-border;
      border-radius: @apple-radius;
      padding: 8px 12px;
      height: 40px;
      font-size: 0.9rem;
      transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
      background: @apple-white;
      
      &:hover {
        border-color: @apple-blue;
      }
      
      &:focus {
        border-color: @apple-blue;
        box-shadow: 0 0 0 3px rgba(10, 132, 255, 0.1);
        outline: none;
      }
      
      &::placeholder {
        color: @apple-text-secondary;
        font-weight: 400;
      }
    }
  }
  
  // 选择框样式
  .apple-select {
    max-width: 260px; // 限制集群框架选择框宽度
    
    /deep/ .ant-select-selector {
      .apple-font();
      border: 1px solid @apple-border !important;
      border-radius: @apple-radius !important;
      padding: 4px 8px !important;
      height: 40px !important;
      transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
      
      .ant-select-selection-search-input {
        height: 32px !important;
        font-size: 0.9rem;
      }
      
      .ant-select-selection-placeholder {
        color: @apple-text-secondary;
        font-weight: 400;
        line-height: 32px;
      }
      
      .ant-select-selection-item {
        line-height: 32px;
        font-size: 0.9rem;
      }
    }
    
    &:hover /deep/ .ant-select-selector {
      border-color: @apple-blue !important;
    }
    
    &.ant-select-focused /deep/ .ant-select-selector {
      border-color: @apple-blue !important;
      box-shadow: 0 0 0 3px rgba(10, 132, 255, 0.1) !important;
    }
    
    // 下拉菜单样式
    /deep/ .ant-select-dropdown {
      border-radius: @apple-radius !important;
      box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12) !important;
      padding: 6px !important;
      border: 1px solid rgba(0, 0, 0, 0.05) !important;
      
      .ant-select-item {
        padding: 8px 12px !important;
        border-radius: 6px !important;
        transition: background 0.2s !important;
        margin: 2px 0 !important;
        font-size: 0.9rem !important;
        
        &:hover {
          background-color: #f0f7ff !important;
        }
        
        &-option-selected {
          background-color: rgba(10, 132, 255, 0.1) !important;
          color: @apple-blue !important;
          font-weight: 600 !important;
        }
      }
    }
  }
  
  // 按钮样式
  .form-actions {
    display: flex;
    justify-content: center;
    gap: 16px;
    margin-top: 32px;
    padding-top: 24px;
    border-top: 1px solid @apple-border-light;
    
    .apple-button {
      .apple-font();
      border-radius: @apple-radius;
      font-weight: 600;
      font-size: 0.95rem;
      padding: 0 24px;
      height: 40px;
      min-width: 120px;
      transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
      border: none;
      
      &:hover {
        transform: translateY(-2px);
      }
      
      &:active {
        transform: translateY(-1px);
      }
      
      &.primary {
        background: linear-gradient(135deg, #0A84FF 0%, #0062CC 100%);
        color: @apple-white;
        box-shadow: 0 4px 12px rgba(10, 132, 255, 0.25);
        
        &:hover {
          box-shadow: 0 6px 16px rgba(10, 132, 255, 0.35);
        }
        
        &:focus {
          box-shadow: 0 4px 12px rgba(10, 132, 255, 0.25);
        }
      }
      
      &.secondary {
        background: @apple-white;
        color: @apple-text-primary;
        border: 1px solid @apple-border;
        
        &:hover {
          background: @apple-gray-light;
          color: @apple-blue;
          border-color: @apple-blue;
        }
        
        &:focus {
          background: @apple-gray-light;
          border-color: @apple-blue;
        }
      }
      
      .anticon {
        margin-right: 8px;
      }
    }
  }
}

// 全局tooltip样式
/deep/ .apple-tooltip {
  .ant-tooltip-inner {
    .apple-font();
    background: rgba(0, 0, 0, 0.85);
    border-radius: 8px;
    padding: 12px 16px;
    
    .tooltip-content {
      .tooltip-item {
        margin-bottom: 8px;
        line-height: 1.5;
        
        &:last-child {
          margin-bottom: 0;
        }
        
        strong {
          color: @apple-blue;
        }
      }
    }
  }
  
  .ant-tooltip-arrow::before {
    background: rgba(0, 0, 0, 0.85);
  }
}

// 响应式设计
@media (max-width: 768px) {
  .apple-form-container {
    padding: 16px;
    
    .form-header .form-title {
      font-size: 1.5rem;
    }
    
    .form-actions {
      flex-direction: column;
      align-items: center;
      
      .apple-button {
        width: 100%;
      }
    }
  }
}

// 动画效果
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.apple-form-container {
  animation: fadeInUp 0.6s ease-out;
}

// 禁用状态样式
.apple-input {
  /deep/ .ant-input[disabled] {
    background-color: #F9F9F9;
    color: #999;
    border-color: #E5E5EA;
    cursor: not-allowed;
  }
}

.apple-select {
  /deep/ .ant-select-disabled {
    .ant-select-selector {
      background-color: #F9F9F9 !important;
      color: #999;
      border-color: #E5E5EA !important;
      cursor: not-allowed;
    }
    
    .ant-select-arrow {
      color: #C7C7CC;
    }
  }
}

.deployment-card[disabled] {
  opacity: 0.7;
  cursor: not-allowed;
  pointer-events: none;
}
</style>

<style>
/* 确保下拉菜单项有圆角 */
.ant-select-dropdown-menu-item {
  border-radius: 8px !important;
  padding: 8px 12px !important;
  transition: background 0.2s !important;
  margin: 4px 0 !important;
}

.ant-select-dropdown-menu-item:hover {
  background-color: #f0f7ff !important;
}

.ant-select-dropdown-menu-item-selected {
  background-color: rgba(10, 132, 255, 0.1) !important;
  color: #0A84FF !important;
  font-weight: 600 !important;
}

/* 强制应用圆角到下拉框本身 */
.ant-select-dropdown {
  border-radius: 12px !important;
  overflow: hidden !important;
  padding: 6px !important;
}

/* 选择框样式强化 */
.ant-select .ant-select-selector {
  border-radius: 12px !important;
  overflow: hidden !important;
}

/* 修复集群框架下拉选择器 */
.apple-select .ant-select-selector {
  border-radius: 12px !important;
  border-top-left-radius: 12px !important;
  border-top-right-radius: 12px !important;
  border-bottom-left-radius: 12px !important;
  border-bottom-right-radius: 12px !important;
}

/* 强制覆盖所有选择器样式 */
.ant-select > .ant-select-selection {
  border-radius: 12px !important;
}

/* 修复下拉箭头区域 */
.ant-select-arrow {
  right: 11px;
}
</style>
