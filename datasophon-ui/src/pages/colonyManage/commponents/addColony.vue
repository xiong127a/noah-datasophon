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
  <a-modal
    :visible="true"
    :title="isEdit ? '编辑集群' : '创建新集群'"
    :maskClosable="false"
    :keyboard="false"
    :closable="true"
    :destroyOnClose="true"
    width="800px"
    :footer="null"
    :bodyStyle="{ padding: 0, maxHeight: 'calc(100vh - 100px)', overflowY: 'auto' }"
    @cancel="formCancel"
    class="apple-modal"
  >
  <div class="apple-form-container">
    <div class="form-header">
        <div class="header-shine"></div>
      <h1 class="form-title">{{ isEdit ? '编辑集群' : '创建新集群' }}</h1>
      <p class="form-subtitle">{{ isEdit ? '修改集群配置信息' : '配置您的大数据平台集群信息' }}</p>
    </div>
    
      <div class="form-content">
      <a-form :form="form" layout="vertical" class="apple-form">
          <!-- 基本信息部分 -->
          <div class="form-section">
            <div class="section-title">
              <div class="title-icon"></div>
              基本信息
          </div>
            <div class="section-description">设置集群的基本标识信息</div>
            
            <div class="form-row">
              <div class="form-field name-field">
                <div class="field-label">
                  集群名称
                  <span class="required-icon" :class="{'success-icon': formValues.clusterName}"></span>
                </div>
                <a-form-item class="custom-form-item" :class="{'has-success': formValues.clusterName}">
                <a-input 
                  v-decorator="[
                    'clusterName',
                      { 
                        rules: [
                          { required: true, message: '请输入集群名称' },
                          { max: 10, message: '不能超过10个字符' }
                        ] 
                      },
                  ]" 
                  placeholder="请输入集群名称" 
                    :maxLength="10"
                    class="compact-input"
                    @change="handleInputChange('clusterName', $event.target.value)"
                />
              </a-form-item>
              </div>
              
              <div class="form-field code-field">
                <div class="field-label">
                  集群编码
                  <span class="required-icon" :class="{'success-icon': formValues.clusterCode}"></span>
                </div>
                <a-form-item class="custom-form-item" :class="{'has-success': formValues.clusterCode}">
                <a-input 
                  v-decorator="[
                    'clusterCode',
                      { 
                        rules: [
                          { required: true, message: '请输入集群编码' },
                          { max: 10, message: '不能超过10个字符' }
                        ] 
                      },
                  ]" 
                  :disabled="isEdit"
                  placeholder="请输入集群编码" 
                    :maxLength="10"
                    class="compact-input"
                    @change="handleInputChange('clusterCode', $event.target.value)"
                />
              </a-form-item>
            </div>
          </div>
        </div>

          <!-- 技术配置部分 -->
          <div class="form-section">
            <div class="section-title">
              <div class="title-icon"></div>
              技术配置
          </div>
            <div class="section-description">选择集群的技术框架和部署方式</div>
            
            <div class="framework-row">
              <div class="field-label">
                集群框架
                <span class="required-icon" :class="{'success-icon': formValues.clusterFrame}"></span>
              </div>
              <a-form-item class="custom-form-item" :class="{'has-success': formValues.clusterFrame}">
                <div class="select-wrapper" ref="selectContainer">
                <a-select 
                v-decorator="[
                  'clusterFrame',
                      { 
                        rules: [
                          { required: true, message: '请选择集群框架' },
                        ] 
                      },
                ]"
                  placeholder="请选择集群框架"
                  :disabled="isEdit"
                    :getPopupContainer="() => $refs.selectContainer"
                    dropdownClassName="apple-dropdown"
                    class="framework-select"
                    @change="handleSelectChange('clusterFrame', $event)"
                >
                    <a-select-option 
                      :value="item.frameCode" 
                      v-for="(item, index) in filteredFrameList" 
                      :key="index"
                    >
                    {{ item.frameCode }}
                  </a-select-option>
                </a-select>
                </div>
              </a-form-item>
            </div>
            
            <!-- 部署方式选择 -->
            <div class="deployment-section">
              <div class="field-label">
                集群部署方式
                <span class="required-icon" :class="{'success-icon': formValues.depType}"></span>
              </div>
              
              <a-form-item class="deployment-form-item" :class="{'has-success': formValues.depType}">
                <!-- 隐藏字段 -->
              <a-input
                  v-decorator="['depType', { rules: [{ required: true, message: '请选择部署方式' }] }]"
                style="display: none;"
              />
                
                <div class="deployment-cards">
                  <div
                    class="deployment-card"
                    :class="{ 'selected': selectedDepType === 'PVM' }"
                    @click="selectDepType('PVM')"
                    :disabled="isEdit"
                  >
                    <div class="card-glow"></div>
                    <div class="card-icon">
                      <img src="~@/assets/img/os-logos/linux-tux.svg" alt="Linux" />
                    </div>
                    <div class="card-content">
                      <div class="card-title">裸金属/虚拟机</div>
                      <div class="card-description">部署到Linux裸金属或虚拟机上</div>
                    </div>
                    <div class="check-icon"></div>
                  </div>

                  <div
                    class="deployment-card"
                    :class="{ 'selected': selectedDepType === 'Kubernetes' }"
                    @click="selectDepType('Kubernetes')"
                    :disabled="isEdit"
                  >
                    <div class="card-glow"></div>
                    <div class="card-icon k8s-icon">
                      <img src="~@/assets/images/kubernetes-logo.svg" alt="Kubernetes" />
                    </div>
                    <div class="card-content">
                      <div class="card-title">Kubernetes</div>
                      <div class="card-description">容器化部署，支持自动化和弹性伸缩</div>
                    </div>
                    <div class="check-icon"></div>
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
          class="primary-btn"
      >
          <div class="btn-content">{{ isEdit ? '保存修改' : '创建集群' }}</div>
      </a-button>
      <a-button 
        @click.stop="formCancel"
          class="cancel-btn"
      >
          <div class="btn-content">取消</div>
      </a-button>
    </div>
  </div>
  </a-modal>
</template>
<script>
import { validateField } from '@/utils/formValidation';

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
      formValues: {
        clusterName: '',
        clusterCode: '',
        clusterFrame: '',
        depType: ''
      }
    };
  },
  computed: {
    // 判断是否为编辑模式
    isEdit() {
      return JSON.stringify(this.detail) !== '{}';
    },
    // 过滤后的框架列表，确保每个框架码不超过10个字符
    filteredFrameList() {
      return this.frameList.filter(item => item.frameCode.length <= 10);
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
          
          // 填充表单值状态
          this.formValues = {
            clusterName: val.clusterName || '',
            clusterCode: val.clusterCode || '',
            clusterFrame: val.clusterFrame || '',
            depType: val.depType || ''
          };
        }
      }
    }
  },
  methods: {
    handleInputChange(field, value) {
      // 直接设置对应字段的值状态
      if (value && value.trim() !== '') {
        this.$set(this.formValues, field, value.trim());
      } else {
        this.$set(this.formValues, field, '');
      }
    },
    handleSelectChange(field, value) {
      // 直接设置对应字段的值状态
      if (value) {
        this.$set(this.formValues, field, value);
      } else {
        this.$set(this.formValues, field, '');
      }
    },
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
      
      // 设置验证状态
      this.$set(this.formValues, 'depType', type);
    },
    formCancel() {
      // 替换$destroyAll()为触发自定义事件
      this.$emit('cancel');
    },
    handleSubmit(e) {
      const _this = this;
      e.preventDefault();
      this.form.validateFields((err, values) => {
        console.log(values);
        if (!err) {
          // 获取当前登录用户信息
          const userStr = localStorage.getItem(process.env.VUE_APP_USER_KEY);
          const currentUser = userStr ? JSON.parse(userStr) : null;
          
          const params = {
            "clusterName": values.clusterName,
            "clusterCode": values.clusterCode,
            "clusterFrame": values.clusterFrame,
            "depType": values.depType,
          };
          
          // 添加创建者信息
          if (currentUser) {
            // 设置createBy为当前用户名
            params.createBy = currentUser.username;
            
            // 如果需要将当前用户添加为集群管理员，构造clusterManagerList
            if (currentUser.id) {
              // 构造符合List<UserInfoEntity>格式的数据结构
              params.clusterManagerList = [{
                id: currentUser.id // UserInfoEntity的id字段
              }];
            }
          }
          
          // 如果当前是编辑模式，添加集群ID
          if (this.isEdit) params.id = this.detail.id;
          
          this.loading = true;
          const ajaxApi = this.isEdit ? global.API.updateColony : global.API.saveColony;
          
          this.$axiosJsonPost(ajaxApi + (this.isEdit ? "?clusterId=" + this.detail.id : ""), params).then((res) => {
            this.loading = false;
            if (res.code === 200) {
              this.$message.success('保存成功', 2);
              // 使用自定义事件代替this.$destroyAll()
              this.$emit('success');
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
            // 设置表单值
            this.form.setFieldsValue({
              clusterName: this.detail.clusterName,
              clusterFrame: this.detail.clusterFrame,
              clusterCode: this.detail.clusterCode,
              depType: this.detail.depType,
            });
            
            // 设置验证状态
            this.formValues = {
              clusterName: this.detail.clusterName || '',
              clusterCode: this.detail.clusterCode || '',
              clusterFrame: this.detail.clusterFrame || '',
              depType: this.detail.depType || ''
            };
          }
        }
      });
    }
  },
  mounted() {
    this.getFrameList();
  }
};
</script>
<style lang="less" scoped>
// 高级设计颜色变量
@form-border-radius: 16px;
@form-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
@form-text-color: #333333;
@form-text-color-secondary: #666666;
@form-text-color-tertiary: #999999;
@form-blue: #1890ff;
@form-blue-hover: #40a9ff;
@form-gray-bg: #f9fafc;
@form-border-color: #e8e8e8;
@gradient-blue: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
@required-color: #ff4d4f;
@card-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);

// 标题字体
.title-font() {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Helvetica Neue", Arial, sans-serif;
  letter-spacing: -0.02em;
  font-weight: 600;
}

.apple-form-container {
  width: 100%;
  background-color: white;
  overflow: hidden;
  border-radius: @form-border-radius;
  position: relative;
  
  .form-header {
    text-align: center;
    padding: 24px 0;
    margin-bottom: 0;
    background: @gradient-blue;
    position: relative;
    overflow: hidden;
    border-top-left-radius: @form-border-radius;
    border-top-right-radius: @form-border-radius;
    
    // 闪光效果
    .header-shine {
      position: absolute;
      top: -50%;
      left: -50%;
      width: 200%;
      height: 200%;
      background: radial-gradient(ellipse at center, rgba(255,255,255,0.3) 0%, rgba(255,255,255,0) 70%);
      transform: rotate(-30deg);
      pointer-events: none;
    }
    
    .form-title {
      .title-font();
      font-size: 22px;
      color: white;
      margin-bottom: 10px;
      text-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
      position: relative;
    }
    
    .form-subtitle {
      font-size: 14px;
      color: rgba(255, 255, 255, 0.95);
      margin: 0;
      font-weight: 400;
      position: relative;
    }
  }
  
  .form-content {
    padding: 24px;
    background-color: @form-gray-bg;
    
    .form-section {
      margin-bottom: 24px;
      background-color: white;
      border-radius: 14px;
      padding: 20px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
      transition: transform 0.3s ease, box-shadow 0.3s ease;
      
      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
      }
      
      &:last-child {
        margin-bottom: 0;
      }
      
      .section-title {
        .title-font();
        font-size: 16px;
        color: @form-text-color;
        margin-bottom: 10px;
        display: flex;
        align-items: center;
        
        .title-icon {
          width: 18px;
          height: 18px;
          background: @gradient-blue;
          border-radius: 50%;
          margin-right: 10px;
          position: relative;
          box-shadow: 0 2px 4px rgba(24, 144, 255, 0.3);
          
          &::after {
            content: '';
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            width: 8px;
            height: 8px;
            background: white;
            border-radius: 50%;
          }
        }
        }
        
      .section-description {
        font-size: 13px;
        color: @form-text-color-tertiary;
        margin-bottom: 16px;
        margin-left: 28px;
        line-height: 1.5;
        }
        
        .form-row {
        display: flex;
        gap: 20px;
        margin-bottom: 16px;
          
          @media (max-width: 768px) {
          flex-direction: column;
        }
        
        .form-field {
          flex: 1;
          
          &.name-field,
          &.code-field {
            max-width: 50%;
            
            @media (max-width: 768px) {
              max-width: 100%;
    }
  }
  
          .field-label {
            font-size: 14px;
            font-weight: 500;
            color: @form-text-color;
            margin-bottom: 8px;
            display: flex;
            align-items: center;
            gap: 4px;
          }
        }
      }
      
      .framework-row {
        margin-bottom: 24px;
        
        .field-label {
          font-size: 14px;
        font-weight: 500;
          color: @form-text-color;
          margin-bottom: 8px;
          display: flex;
          align-items: center;
          gap: 4px;
        }
        
        .select-wrapper {
          position: relative;
          width: 100%;
          max-width: 300px;
          
          .framework-select {
            width: 100%;
            height: 38px;
            border-radius: 10px;
            transition: all 0.3s cubic-bezier(0.2, 0, 0.38, 1);
            border: 1px solid #d9d9d9;
            
            &:hover {
              border-color: @form-blue;
              box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.06);
            }
            
            &:focus {
              border-color: @form-blue;
              box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
              outline: none;
          }
        }
      }
    }
    
      .deployment-section {
        .field-label {
          font-size: 14px;
          font-weight: 500;
          color: @form-text-color;
          margin-bottom: 12px;
          display: flex;
          align-items: center;
          gap: 4px;
        }
        
        .deployment-form-item {
          margin-bottom: 0;
          
          .deployment-cards {
            display: flex;
            flex-wrap: wrap;
            gap: 16px;
        
        .deployment-card {
              flex: 1;
              min-width: 250px;
          display: flex;
          align-items: center;
              padding: 20px;
              border: 1px solid @form-border-color;
              border-radius: 14px;
          cursor: pointer;
              transition: all 0.3s cubic-bezier(0.2, 0, 0.38, 1);
              background: white;
              position: relative;
              overflow: hidden;
              
              .card-glow {
                position: absolute;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                background: linear-gradient(135deg, rgba(255,255,255,0.8) 0%, rgba(255,255,255,0) 80%);
                opacity: 0;
                transition: opacity 0.3s;
              }
          
          &:hover {
                border-color: @form-blue;
                transform: translateY(-3px);
                box-shadow: @card-shadow;
                
                .card-glow {
                  opacity: 0.5;
                }
                
                .card-icon {
                  transform: scale(1.05);
                }
          }
          
          &.selected {
                border-color: @form-blue;
                background-color: rgba(24, 144, 255, 0.05);
                box-shadow: 0 0 0 1px @form-blue, @card-shadow;
                
                .card-icon {
                  transform: scale(1.05);
                  background-color: rgba(24, 144, 255, 0.1);
                  box-shadow: 0 0 0 1px rgba(24, 144, 255, 0.2), 0 4px 8px rgba(24, 144, 255, 0.2);
                }
        
                .card-title {
                  color: @form-blue;
                }
                
                .check-icon {
              opacity: 1;
              transform: scale(1);
            }
          }
          
          .card-icon {
                width: 48px;
                height: 48px;
            display: flex;
            align-items: center;
            justify-content: center;
                margin-right: 16px;
            flex-shrink: 0;
                background-color: @form-gray-bg;
                border-radius: 50%;
                transition: all 0.3s cubic-bezier(0.2, 0, 0.38, 1);
                box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
            
            img {
                  max-width: 24px;
                  max-height: 24px;
                  transition: transform 0.3s;
            }
          }
          
          .card-content {
            flex: 1;
            
            .card-title {
                  font-size: 15px;
              font-weight: 600;
                  color: @form-text-color;
                  margin-bottom: 6px;
                  transition: color 0.3s;
            }
        
            .card-description {
                  font-size: 13px;
                  color: @form-text-color-tertiary;
              line-height: 1.4;
            }
          }
          
              .check-icon {
            position: absolute;
                top: 16px;
                right: 16px;
                width: 22px;
                height: 22px;
                background: @form-blue;
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
            opacity: 0;
                transform: scale(0.5);
                transition: all 0.3s cubic-bezier(0.2, 0, 0.38, 1);
                box-shadow: 0 2px 4px rgba(24, 144, 255, 0.3);
                
                &:after {
                  content: '';
                  width: 8px;
                  height: 4px;
                  border-left: 2px solid white;
                  border-bottom: 2px solid white;
                  transform: rotate(-45deg) translate(0, -1px);
                }
              }
            }
          }
        }
            }
          }
        }
        
  .form-actions {
    display: flex;
    justify-content: center;
    padding: 20px;
    background-color: white;
    border-top: 1px solid rgba(0, 0, 0, 0.05);
    gap: 16px;
    border-bottom-left-radius: @form-border-radius;
    border-bottom-right-radius: @form-border-radius;
    
    .primary-btn,
    .cancel-btn {
      min-width: 120px;
      height: 40px;
      border-radius: 20px;
      font-size: 15px;
      font-weight: 500;
      transition: all 0.3s cubic-bezier(0.2, 0, 0.38, 1);
            display: flex;
      align-items: center;
      justify-content: center;
      overflow: hidden;
      position: relative;
      
      .btn-content {
        position: relative;
        z-index: 2;
      }
            }
            
    .primary-btn {
      background: @gradient-blue;
      border: none;
      color: white;
      box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
      
      &:before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: linear-gradient(135deg, rgba(255,255,255,0.2) 0%, rgba(255,255,255,0) 80%);
        opacity: 0;
        transition: opacity 0.3s;
      }
      
      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 16px rgba(24, 144, 255, 0.4);
        
        &:before {
          opacity: 1;
        }
      }
      
      &:active {
        transform: translateY(0);
        box-shadow: 0 4px 8px rgba(24, 144, 255, 0.3);
      }
    }
    
    .cancel-btn {
      border: 1px solid #e0e0e0;
      color: @form-text-color;
      background: white;
      
      &:hover {
        border-color: @form-blue;
        color: @form-blue;
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
      }
      
      &:active {
        transform: translateY(0);
        box-shadow: 0 2px 6px rgba(0, 0, 0, 0.03);
      }
      }
    }
  }
  
// 必填项红点样式
.required-icon {
  position: relative;
  display: inline-block;
  width: 6px;
  height: 6px;
  background-color: #ff4d4f;
  border-radius: 50%;
  margin-left: 4px;
  transition: background-color 0.3s;
  
  &::after {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%) scale(2);
    width: 100%;
    height: 100%;
    background-color: rgba(255, 77, 79, 0.2);
    border-radius: 50%;
    animation: pulse 2s infinite;
    }
    
  &.success-icon {
    background-color: #52c41a;
    
    &::after {
      background-color: rgba(82, 196, 26, 0.2);
      animation: success-pulse 2s infinite;
    }
  }
}

@keyframes pulse {
  0% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 0.7;
  }
  70% {
    transform: translate(-50%, -50%) scale(2);
    opacity: 0;
  }
  100% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 0;
    }
}

@keyframes success-pulse {
  0% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 0.7;
  }
  70% {
    transform: translate(-50%, -50%) scale(2);
    opacity: 0;
  }
  100% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 0;
  }
}

// 表单项样式
.custom-form-item {
  margin-bottom: 0;
  
  :deep(.ant-form-item-label) {
    display: none;
  }
  
  :deep(.ant-form-item-control) {
    line-height: 32px;
  }
}

.compact-input {
  width: 100%;
  height: 38px;
  border-radius: 10px;
  transition: all 0.3s cubic-bezier(0.2, 0, 0.38, 1);
  border: 1px solid #d9d9d9;
  padding: 4px 12px;
  font-size: 14px;
        
        &:hover {
    border-color: @form-blue;
    box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.06);
  }
  
  &:focus {
    border-color: @form-blue;
    box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
    outline: none;
        }
      }
</style>

<style>
/* 全局样式 */
.apple-modal .ant-modal-content {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15), 0 6px 12px rgba(0, 0, 0, 0.08);
  animation: modal-in 0.4s cubic-bezier(0.2, 0, 0.38, 1);
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

.apple-modal .ant-modal-header {
  display: none;
}

.apple-modal .ant-modal-close {
  color: white;
  z-index: 10;
}

.apple-modal .ant-modal-body {
  padding: 0 !important;
  border-radius: 16px;
  overflow: hidden;
}

/* 下拉菜单样式 */
.apple-dropdown {
  min-width: 100% !important;
  width: 100% !important;
  border-radius: 10px !important;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.12), 0 6px 12px rgba(0, 0, 0, 0.06) !important;
  padding: 8px !important;
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
  overflow: hidden !important;
  animation: dropdown-in 0.2s cubic-bezier(0.2, 0, 0.38, 1) !important;
}

@keyframes dropdown-in {
  from {
    opacity: 0;
    transform: translateY(-8px);
      }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.apple-dropdown .ant-select-dropdown-menu-item {
  border-radius: 8px !important;
  padding: 10px 14px !important;
  margin: 2px 0 !important;
  transition: all 0.2s !important;
  font-size: 14px !important;
      }
      
.apple-dropdown .ant-select-dropdown-menu-item:hover {
  background-color: #f0f7ff !important;
}

.apple-dropdown .ant-select-dropdown-menu-item-selected {
  background-color: rgba(24, 144, 255, 0.1) !important;
  color: #1890ff !important;
  font-weight: 600 !important;
}

/* 输入框和下拉框样式 */
.ant-select-selection,
.ant-input {
  border-radius: 10px !important;
  border: 1px solid #d9d9d9 !important;
  transition: all 0.3s cubic-bezier(0.2, 0, 0.38, 1) !important;
  height: 38px !important;
}

.ant-select-selection:hover,
.ant-input:hover {
  border-color: #1890ff !important;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.06) !important;
        }
        
.ant-select-focused .ant-select-selection,
.ant-select-selection:focus,
.ant-input:focus {
  border-color: #1890ff !important;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2) !important;
  outline: none !important;
}

/* 修复双重边框问题 */
.ant-form-item-control .ant-input-affix-wrapper .ant-input {
  border: none !important;
  box-shadow: none !important;
}

.ant-input-affix-wrapper {
  border-radius: 10px !important;
  border: 1px solid #d9d9d9 !important;
  transition: all 0.3s cubic-bezier(0.2, 0, 0.38, 1) !important;
}

.ant-input-affix-wrapper:hover {
  border-color: #1890ff !important;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.06) !important;
}

.ant-input-affix-wrapper-focused,
.ant-input-affix-wrapper:focus {
  border-color: #1890ff !important;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2) !important;
  outline: none !important;
        }
        
/* 成功状态下修复双重边框 */
.ant-form-item-has-success .ant-input-affix-wrapper {
  border-color: #52c41a !important;
      }
      
.ant-form-item-has-success .ant-input-affix-wrapper:hover {
  border-color: #73d13d !important;
      }

.ant-form-item-has-success .ant-input-affix-wrapper-focused,
.ant-form-item-has-success .ant-input-affix-wrapper:focus {
  border-color: #52c41a !important;
  box-shadow: 0 0 0 2px rgba(82, 196, 26, 0.2) !important;
}

/* 错误状态下修复双重边框 */
.ant-form-item-has-error .ant-input-affix-wrapper {
  border-color: #ff4d4f !important;
}

.ant-form-item-has-error .ant-input-affix-wrapper:hover {
  border-color: #ff7875 !important;
}

.ant-form-item-has-error .ant-input-affix-wrapper-focused,
.ant-form-item-has-error .ant-input-affix-wrapper:focus {
  border-color: #ff4d4f !important;
  box-shadow: 0 0 0 2px rgba(255, 77, 79, 0.2) !important;
        }

/* 表单验证消息 */
.ant-form-explain {
  font-size: 12px !important;
  margin-top: 4px !important;
  padding-left: 2px !important;
  animation: message-in 0.3s cubic-bezier(0.2, 0, 0.38, 1) !important;
}

/* 表单验证错误消息 */
.ant-form-item-has-error .ant-form-explain {
  color: #ff4d4f !important;
}

/* 表单验证成功消息 */
.ant-form-item-has-success .ant-form-explain {
  color: #52c41a !important;
}

/* 成功状态的表单项 */
.ant-form-item-has-success .ant-input,
.ant-form-item-has-success .ant-select-selection {
  border-color: #52c41a !important;
    }
    
.ant-form-item-has-success .ant-input:hover,
.ant-form-item-has-success .ant-select-selection:hover {
  border-color: #73d13d !important;
}

.ant-form-item-has-success .ant-input:focus,
.ant-form-item-has-success .ant-select-selection:focus {
  border-color: #52c41a !important;
  box-shadow: 0 0 0 2px rgba(82, 196, 26, 0.2) !important;
      }

@keyframes message-in {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 全局按钮样式 */
.ant-btn {
  border-radius: 20px !important;
  transition: all 0.3s cubic-bezier(0.2, 0, 0.38, 1) !important;
}

.ant-modal-confirm .ant-modal-confirm-btns {
  display: flex !important;
  gap: 8px !important;
  margin-top: 24px !important;
}

.ant-modal-confirm .ant-btn {
  flex: 1 !important;
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  height: 36px !important;
}

/* 模态框遮罩层 */
.apple-modal .ant-modal-mask {
  backdrop-filter: blur(10px) !important;
  -webkit-backdrop-filter: blur(10px) !important;
  background-color: rgba(0, 0, 0, 0.45) !important;
  animation: mask-in 0.2s ease-out !important;
  transition: opacity 0.2s ease-out !important;
}

.ant-modal-mask-hidden {
  opacity: 0 !important;
  transition: opacity 0.2s ease-out !important;
  backdrop-filter: none !important;
  -webkit-backdrop-filter: none !important;
  }

@keyframes mask-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* 禁用表单元素样式 */
.ant-input[disabled], .ant-select-disabled .ant-select-selection {
  background-color: #f5f5f5 !important;
  border-color: #d9d9d9 !important;
  color: rgba(0, 0, 0, 0.45) !important;
    }
    
/* 重写Select组件相关样式 */
.framework-row .select-wrapper .ant-select .ant-select-selection {
  height: 38px !important;
  border: 1px solid #d9d9d9 !important;
  border-radius: 10px !important;
  transition: all 0.3s cubic-bezier(0.2, 0, 0.38, 1) !important;
  outline: none !important;
  box-shadow: none !important;
}

.framework-row .select-wrapper .ant-select .ant-select-selection:hover {
  border-color: #1890ff !important;
}

.framework-row .select-wrapper .ant-select-focused .ant-select-selection {
  border-color: #1890ff !important;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2) !important;
  outline: none !important;
}

/* 修复Select内部元素 */
.ant-select-selection__rendered {
  line-height: 38px !important;
  margin-left: 12px !important;
}

/* 验证成功的表单项样式 */
.custom-form-item.has-success .ant-input,
.custom-form-item.has-success .ant-select-selection {
  border-color: #52c41a !important;
}

.custom-form-item.has-success .ant-input:hover,
.custom-form-item.has-success .ant-select-selection:hover {
  border-color: #73d13d !important;
}

.custom-form-item.has-success .ant-input:focus,
.custom-form-item.has-success .ant-select-selection:focus {
  border-color: #52c41a !important;
  box-shadow: 0 0 0 2px rgba(82, 196, 26, 0.2) !important;
}

/* 输入框样式强制覆盖 */
.compact-input {
  border-radius: 10px !important;
  height: 38px !important;
  transition: all 0.3s cubic-bezier(0.2, 0, 0.38, 1) !important;
  border: 1px solid #d9d9d9 !important;
}

.custom-form-item.has-success .compact-input {
  border-color: #52c41a !important;
}

/* 下拉框样式强制覆盖 */
.framework-select .ant-select-selection {
  border-radius: 10px !important;
  height: 38px !important;
  transition: all 0.3s cubic-bezier(0.2, 0, 0.38, 1) !important;
  border: 1px solid #d9d9d9 !important;
}

.custom-form-item.has-success .framework-select .ant-select-selection {
  border-color: #52c41a !important;
}

/* 卡片选择样式强制覆盖 */
.deployment-form-item.has-success .deployment-card.selected {
  border-color: #52c41a !important;
  box-shadow: 0 0 0 1px #52c41a, 0 4px 12px rgba(82, 196, 26, 0.2) !important;
}
</style>
