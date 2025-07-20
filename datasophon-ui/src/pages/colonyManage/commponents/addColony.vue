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
      <h1 class="form-title">创建新集群</h1>
      <p class="form-subtitle">配置您的大数据平台集群信息</p>
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
                  size="large"
                />
              </a-form-item>
              <a-form-item label="集群编码" class="apple-form-item">
                <a-input 
                  v-decorator="[
                    'clusterCode',
                    { rules: [{ required: true, message: '集群编码不能为空!' }] },
                  ]" 
                  placeholder="请输入集群编码" 
                  class="apple-input"
                  size="large"
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
                  size="large"
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
                        <strong>裸金属/虚拟机部署：</strong>适用于需要并行处理大规模计算任务的场景，支持将多个计算机资源组合成一个强大的计算集群，提供最佳的性能和资源控制。
                          </div>
                      <div v-else-if="selectedDepType === 'Kubernetes'">
                        <strong>Kubernetes部署：</strong>适用于需要管理和部署容器化应用程序的场景，强调的是自动化、可扩展和高可用的应用程序管理，支持微服务架构。
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
        size="large"
      >
        <a-icon type="check" />
        创建集群
      </a-button>
      <a-button 
        @click.stop="formCancel"
        class="apple-button secondary"
        size="large"
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
    callBack:Function
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
      frameList: [], //集群框架列表
      depTypeList: ['Kubernetes', 'PVM'], //部署方式列表
      depType:'',
      selectedDepType: '', // 当前选中的部署方式
    };
  },
  watch: {},
  methods: {
    tochange(val){
      this.depType = val
    },
    selectDepType(type) {
      this.selectedDepType = type
      this.depType = type
      // 手动设置表单字段值
      this.form.setFieldsValue({ depType: type })
    },
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
            "clusterName": values.clusterName,
            "clusterCode": values.clusterCode,
            "clusterFrame": values.clusterFrame,
            "depType": values.depType,
          }
          if (JSON.stringify(this.detail) !== '{}') params.id = this.detail.id
          this.loading = true;
          const ajaxApi = JSON.stringify(this.detail) !== '{}' ? global.API.updateColony : global.API.saveColony
          this.$axiosJsonPost(ajaxApi+"?clusterId="+this.detail.id, params).then((res) => {
            this.loading = false;
            if (res.code === 200) {
              this.$message.success('保存成功', 2)
              this.$destroyAll();
              _this.callBack();
            }
          }).catch((err) => {});
        }
      });
    },
    getFrameList() {
      this.$axiosPost(global.API.getFrameList, {}).then((res) => {
        if (res.code === 200) {
          this.frameList = res.data
          if (JSON.stringify(this.detail) !== '{}') {
            this.form.getFieldsValue(['clusterName', 'clusterFrame', 'clusterCode', 'depType'])
            this.form.setFieldsValue({
              clusterName:this.detail.clusterName,
              clusterFrame: this.detail.clusterFrame,
              clusterCode: this.detail.clusterCode,
              depType: this.detail.depType,
            })
            this.depType = this.detail.depType
            this.selectedDepType = this.detail.depType
          }
        }
      })
    }
  },
  mounted() {
    this.getFrameList()
  },
};
</script>
<style lang="less" scoped>
// Apple设计系统颜色变量 - 优化版
@apple-blue: #34c759;
@apple-blue-hover: #248a3d;
@apple-blue-light: rgba(52, 199, 89, 0.1);
@apple-gray: #f8f9fa;
@apple-gray-light: #fafbfc;
@apple-white: #ffffff;
@apple-black: #1a1a1a;
@apple-text-primary: #1a1a1a;
@apple-text-secondary: #6b7280;
@apple-border: rgba(52, 199, 89, 0.15);
@apple-border-light: rgba(0, 0, 0, 0.08);
@apple-radius: 16px;
@apple-radius-large: 20px;
@apple-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
@apple-shadow-hover: 0 12px 40px rgba(0, 0, 0, 0.12);

// Apple字体
.apple-font() {
  font-family: "SF Pro Display", "SF Pro Text", "PingFang SC", "Helvetica Neue", Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.apple-form-container {
  .apple-font();
  max-width: 1000px;
  margin: 0 auto;
  padding: 40px;
  background: linear-gradient(135deg, @apple-gray-light 0%, @apple-white 100%);
  border-radius: @apple-radius-large;
  box-shadow: @apple-shadow;
  
  .form-header {
    text-align: center;
    margin-bottom: 56px;
    
    .form-title {
      .apple-font();
      font-size: 2.8rem;
      font-weight: 700;
      line-height: 1.1;
      letter-spacing: -0.025em;
      color: @apple-text-primary;
      margin-bottom: 16px;
      background: linear-gradient(135deg, @apple-blue 0%, @apple-blue-hover 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
    
    .form-subtitle {
      .apple-font();
      font-size: 1.3rem;
      line-height: 1.5;
      color: @apple-text-secondary;
      margin: 0;
      font-weight: 500;
    }
  }
  
  .form-cards {
    .form-card {
      background: @apple-white;
      border: 1px solid @apple-border-light;
      border-radius: @apple-radius;
      box-shadow: @apple-shadow;
      margin-bottom: 28px;
      overflow: hidden;
      transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
      backdrop-filter: blur(10px);
      
      &:hover {
        box-shadow: @apple-shadow-hover;
        transform: translateY(-4px);
        border-color: @apple-border;
      }
      
      .card-header {
        padding: 28px 36px 20px;
        background: linear-gradient(135deg, @apple-blue-light 0%, rgba(255, 255, 255, 0.8) 100%);
        border-bottom: 1px solid @apple-border-light;
        
        .card-title {
          .apple-font();
          font-size: 1.5rem;
          font-weight: 700;
          color: @apple-blue-hover;
          margin: 0 0 10px 0;
          line-height: 1.2;
          letter-spacing: -0.01em;
        }
        
        .card-description {
          .apple-font();
          font-size: 1rem;
          color: @apple-text-secondary;
          margin: 0;
          line-height: 1.5;
          font-weight: 500;
        }
      }
      
      .card-content {
        padding: 36px;
        background: rgba(255, 255, 255, 0.6);
        
        .form-row {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 28px;
          
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
      padding-bottom: 8px;
      
      label {
        .apple-font();
        font-size: 1rem;
        font-weight: 500;
        color: @apple-text-primary;
        
        &::after {
          content: '';
        }
      }
    }
    
    // 集群框架特殊样式
    &.cluster-framework {
      grid-column: 1 / -1;
    }
    
    // 部署方式特殊样式
    &.deployment-type {
      grid-column: 1 / -1;
      
      .deployment-cards-container {
        .deployment-cards {
          display: grid;
          grid-template-columns: repeat(2, 1fr);
          gap: 16px;
          margin-bottom: 16px;
          
          @media (max-width: 768px) {
            grid-template-columns: 1fr;
          }
        }
        
        .deployment-card {
          position: relative;
          background: @apple-white;
          border: 2px solid @apple-border-light;
          border-radius: @apple-radius;
          padding: 24px;
          cursor: pointer;
          transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
          backdrop-filter: blur(10px);
          
          &:hover {
            border-color: @apple-blue;
            transform: translateY(-3px);
            box-shadow: 0 12px 32px rgba(52, 199, 89, 0.15);
          }
          
          &.selected {
            border-color: @apple-blue;
            background: @apple-blue-light;
            transform: translateY(-3px);
            box-shadow: 0 12px 32px rgba(52, 199, 89, 0.2);
        
            .card-check {
              opacity: 1;
              transform: scale(1);
            }
          }
          
          .card-icon {
            display: flex;
            align-items: center;
            justify-content: center;
            width: 48px;
            height: 48px;
            border-radius: 8px;
            margin-bottom: 16px;
            
            img {
              width: 42px;
              height: 42px;
            }
          }
          
          .card-content {
          flex: 1;
            
            .card-title {
              .apple-font();
              font-size: 1.1rem;
              font-weight: 600;
              color: @apple-text-primary;
              margin: 0 0 8px 0;
              line-height: 1.2;
        }
        
            .card-description {
              .apple-font();
              font-size: 0.9rem;
              color: @apple-text-secondary;
              margin: 0;
              line-height: 1.4;
            }
          }
          
          .card-check {
            position: absolute;
            top: 16px;
            right: 16px;
            opacity: 0;
            transform: scale(0.8);
            transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
            
            .check-icon {
              color: @apple-blue;
              font-size: 20px;
            }
          }
        }
        
        .deployment-info-panel {
          background: rgba(0, 122, 255, 0.05);
          border: 1px solid rgba(0, 122, 255, 0.1);
          border-radius: @apple-radius;
          padding: 16px;
          animation: fadeInUp 0.3s ease-out;
          
          .info-content {
            display: flex;
            align-items: flex-start;
            gap: 12px;
            
          .info-icon {
            color: @apple-blue;
              font-size: 16px;
              margin-top: 2px;
            }
            
            .info-text {
              flex: 1;
              .apple-font();
              font-size: 0.9rem;
              line-height: 1.5;
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
      border: 2px solid @apple-border;
      border-radius: 8px;
      padding: 12px 16px;
      font-size: 1rem;
      transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
      background: @apple-white;
      
      &:hover {
        border-color: @apple-blue;
      }
      
      &:focus {
        border-color: @apple-blue;
        box-shadow: 0 0 0 4px rgba(0, 122, 255, 0.1);
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
    /deep/ .ant-select-selector {
      .apple-font();
      border: 2px solid @apple-border !important;
      border-radius: 8px !important;
      padding: 8px 12px !important;
      height: auto !important;
      min-height: 48px !important;
      transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
      
      .ant-select-selection-search-input {
        height: 32px !important;
        font-size: 1rem;
      }
      
      .ant-select-selection-placeholder {
        color: @apple-text-secondary;
        font-weight: 400;
        line-height: 32px;
      }
    }
    
    &:hover /deep/ .ant-select-selector {
      border-color: @apple-blue !important;
    }
    
    &.ant-select-focused /deep/ .ant-select-selector {
      border-color: @apple-blue !important;
      box-shadow: 0 0 0 4px rgba(0, 122, 255, 0.1) !important;
    }
  }
  
  // 按钮样式
  .form-actions {
    display: flex;
    justify-content: center;
    gap: 20px;
    margin-top: 56px;
    padding-top: 36px;
    border-top: 1px solid @apple-border-light;
    
    .apple-button {
      .apple-font();
      border-radius: 12px;
      font-weight: 600;
      font-size: 1.1rem;
      padding: 16px 40px;
      height: auto;
      min-width: 160px;
      transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
      border: none;
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
      letter-spacing: -0.01em;
      
      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
      }
      
      &:active {
        transform: translateY(-1px);
      }
      
      &.primary {
        background: linear-gradient(135deg, @apple-blue 0%, @apple-blue-hover 100%);
        color: @apple-white;
        box-shadow: 0 6px 20px rgba(52, 199, 89, 0.3);
        
        &:hover {
          background: linear-gradient(135deg, @apple-blue-hover 0%, darken(@apple-blue-hover, 8%) 100%);
          color: @apple-white;
          border-color: transparent;
          box-shadow: 0 10px 28px rgba(52, 199, 89, 0.4);
        }
        
        &:focus {
          background: linear-gradient(135deg, @apple-blue-hover 0%, darken(@apple-blue-hover, 8%) 100%);
          color: @apple-white;
          border-color: transparent;
        }
      }
      
      &.secondary {
        background: rgba(255, 255, 255, 0.9);
        color: @apple-text-primary;
        border: 2px solid @apple-border-light;
        
        &:hover {
          background: @apple-gray;
          color: @apple-text-primary;
          border-color: @apple-border;
        }
        
        &:focus {
          background: @apple-gray;
          color: @apple-text-primary;
          border-color: @apple-border;
        }
      }
      
      .anticon {
        margin-right: 10px;
        font-size: 16px;
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
    padding: 24px 16px;
    
    .form-header .form-title {
      font-size: 2rem;
    }
    
    .form-cards .form-card .card-content {
      padding: 24px 20px;
    }
    
    .form-actions {
      flex-direction: column;
      align-items: center;
      
      .apple-button {
        width: 100%;
        max-width: 300px;
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
</style>
