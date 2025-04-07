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


 * @describe: step6-分配服务Worker与Client角色 
 * @Date: 2022-06-13 16:35:02
 * @LastEditTime: 2022-08-15 14:07:14
 * @FilePath: \ddh-ui\src\components\steps\step6.vue
-->
<template>
  <div class="steps6">
    <div class="hero-section">
      <h1 class="hero-title">服务配置</h1>
      <p class="hero-subtitle">调整服务配置参数，确保最佳性能与稳定性</p>
    </div>
    
    <div class="service-config-container">
      <a-tabs v-model="activeKey" @change="callback" animated class="apple-tabs">
        <a-tab-pane :key="item.serviceName" v-for="(item) in serviceNames">
          <span slot="tab" class="tab-title">{{ item.serviceName }}</span>
          <div class="config-form-wrapper">
            <a-spin :spinning="loading">
              <a-form-model
                class="service-config-form"
                ref="ruleForm"
                :model="formState"
                :rules="rules"
                v-bind="layout"
              >
                <div class="config-form-container">
                  <template v-for="(configItem, index) in serviceConfig">
                    <a-form-model-item
                      :key="index"
                      :label="configItem.configName"
                      :ref="configItem.configName"
                      :prop="configItem.configName"
                      :labelCol="{ span: 8 }"
                      :wrapperCol="{ span: 16 }"
                    >
                      <a-input
                        v-if="configItem.configValueType === 'text'"
                        class="apple-input"
                        v-model="formState[configItem.configName]"
                        :placeholder="configItem.configName"
                      />
                      <a-input-number
                        v-else-if="configItem.configValueType === 'number'"
                        class="apple-input-number"
                        v-model="formState[configItem.configName]"
                        :placeholder="configItem.configName"
                      />
                      <a-select
                        v-else-if="configItem.configValueType === 'select'"
                        class="apple-select"
                        v-model="formState[configItem.configName]"
                        :placeholder="configItem.configName"
                      >
                        <a-select-option 
                          v-for="(option, optionIndex) in configItem.selectValue.split(',')" 
                          :key="optionIndex"
                          :value="option"
                        >
                          {{ option }}
                        </a-select-option>
                      </a-select>
                    </a-form-model-item>
                  </template>
                </div>
              </a-form-model>
            </a-spin>
          </div>
        </a-tab-pane>
      </a-tabs>
    </div>
  </div>
</template>
<script>
export default {
  inject: ["handleCancel", "currentStepsAdd", "currentStepsSub", "clusterId"],
  props: {
    steps4Data: Object,
  },
  data() {
    return {
      activeKey: "",
      serviceNames: [],
      serviceConfig: [],
      formState: {},
      rules: {},
      layout: {
        labelCol: { span: 4 },
        wrapperCol: { span: 14 },
      },
      loading: false,
    };
  },
  methods: {
    callback(key) {
      this.getServiceConfig(key);
    },
    getServiceConfig(serviceName) {
      this.loading = true;
      this.formState = {};
      this.rules = {};
      const service = this.serviceNames.find(item => item.serviceName === serviceName);
      
      if (!service) {
        this.loading = false;
        return;
      }
      
      const params = {
        clusterId: this.clusterId,
        serviceId: service.serviceId,
      };
      
      this.$axiosPost(global.API.getServiceConfig, params).then((res) => {
        this.loading = false;
        if (res.code === 200) {
          this.serviceConfig = res.data;
          
          // 初始化表单状态和验证规则
          this.serviceConfig.forEach(item => {
            this.formState[item.configName] = item.configValue;
            
            // 设置验证规则
            if (item.required) {
              this.rules[item.configName] = [
                { required: true, message: `请输入${item.configName}`, trigger: 'blur' }
              ];
            }
          });
        }
      });
    },
    handleSubmit(callback) {
      this.$refs.ruleForm.validate(valid => {
        if (valid) {
          const params = {
            clusterId: this.clusterId,
            serviceId: this.serviceNames.find(item => item.serviceName === this.activeKey).serviceId,
            configList: Object.keys(this.formState).map(key => ({
              configName: key,
              configValue: this.formState[key]
            }))
          };
          
          this.$axiosPost(global.API.updateServiceConfig, params).then((res) => {
            if (callback) {
              callback(res);
            }
          });
        } else {
          if (callback) {
            callback({code: 500, msg: '请正确填写表单'});
          }
        }
      });
    },
  },
  mounted() {
    this.serviceNames = this.steps4Data.serviceNames;
    if (this.serviceNames[0]) {
      this.activeKey = this.serviceNames[0].serviceName;
      this.getServiceConfig(this.activeKey);
    }
  },
};
</script>
<style lang="less" scoped>
// 苹果设计系统颜色
@apple-white: #ffffff;
@apple-black: #1d1d1f;
@apple-gray-light: #f5f5f7;
@apple-gray: #86868b;
@apple-blue: #0071e3;
@apple-blue-hover: #147CE5;
@apple-red: #ff453a;
@apple-green: #34c759;
@apple-yellow: #ffd60a;
@apple-orange: #ff9f0a;

// 苹果设计系统字体
.apple-font() {
  font-family: "SF Pro Display", "SF Pro Icons", "PingFang SC", "Helvetica Neue", Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
}

.steps6 {
  margin: 0;
  max-width: 100%;
  background-color: @apple-white;
  overflow: hidden;
  animation: fadeIn 0.8s ease-out;
  
  .hero-section {
    text-align: center;
    margin-bottom: 2.5rem;
    position: relative;

    .hero-title {
      .apple-font();
      font-size: 2.5rem;
      font-weight: 600;
      line-height: 1.1;
      letter-spacing: -0.022em;
      color: @apple-black;
      margin-bottom: 0.8rem;
      background: linear-gradient(120deg, @apple-black, #505050);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }

    .hero-subtitle {
      .apple-font();
      font-size: 1.2rem;
      line-height: 1.4;
      letter-spacing: 0;
      font-weight: 400;
      color: @apple-gray;
      margin: 0 auto 1.5rem;
      max-width: 600px;
    }
  }
  
  .service-config-container {
    border-radius: 12px;
    margin: 0 auto;
    max-width: 1000px;
    overflow: hidden;
    animation: slideUp 0.6s ease-out;
    animation-fill-mode: both;
    animation-delay: 0.2s;
    
    // 自定义标签页样式
    :deep(.apple-tabs) {
      .apple-font();
      
      .ant-tabs-bar {
        border-bottom: 1px solid #f0f0f0;
        margin-bottom: 24px;
      }
      
      .ant-tabs-nav {
        margin-bottom: 20px;
        
        .ant-tabs-tab {
          padding: 12px 16px;
          margin-right: 24px;
          transition: all 0.3s;
          
          .tab-title {
            font-size: 15px;
            font-weight: 500;
            color: @apple-gray;
            transition: color 0.3s;
          }
          
          &:hover {
            .tab-title {
              color: darken(@apple-gray, 15%);
            }
          }
          
          &.ant-tabs-tab-active {
            .tab-title {
              color: @apple-blue;
              font-weight: 600;
            }
          }
        }
        
        .ant-tabs-ink-bar {
          background-color: @apple-blue;
          height: 3px;
          border-radius: 3px;
        }
      }
      
      .ant-tabs-content {
        padding: 0 16px;
      }
    }
    
    // 配置表单容器
    .config-form-wrapper {
      background-color: @apple-white;
      border-radius: 12px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
      overflow: hidden;
      padding: 24px;
      
      .service-config-form {
        width: 100%;
        
        .config-form-container {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
          gap: 24px;
          
          // 表单项样式
          :deep(.ant-form-item) {
            margin-bottom: 24px;
            
            .ant-form-item-label {
              label {
                .apple-font();
                color: @apple-black;
                font-weight: 500;
              }
            }
            
            // 必选标记
            .ant-form-item-required::before {
              color: @apple-red;
            }
          }
        }
      }
    }
    
    // 输入框样式
    :deep(.apple-input) {
      .apple-font();
      border-radius: 8px;
      transition: all 0.3s;
      
      &:hover {
        border-color: @apple-blue-hover;
      }
      
      &:focus {
        border-color: @apple-blue;
        box-shadow: 0 0 0 2px rgba(0, 113, 227, 0.2);
      }
    }
    
    // 数字输入框样式
    :deep(.apple-input-number) {
      .apple-font();
      width: 100%;
      border-radius: 8px;
      
      .ant-input-number-handler-wrap {
        border-radius: 0 8px 8px 0;
      }
      
      &:hover {
        border-color: @apple-blue-hover;
      }
      
      &.ant-input-number-focused {
        border-color: @apple-blue;
        box-shadow: 0 0 0 2px rgba(0, 113, 227, 0.2);
      }
    }
    
    // 下拉选择框样式
    :deep(.apple-select) {
      .apple-font();
      width: 100%;
      
      .ant-select-selection {
        border-radius: 8px;
        border: 1px solid #d9d9d9;
        transition: all 0.3s;
        
        &:hover {
          border-color: @apple-blue-hover;
        }
        
        &:focus,
        &.ant-select-focused {
          border-color: @apple-blue;
          box-shadow: 0 0 0 2px rgba(0, 113, 227, 0.2);
        }
      }
    }
  }
}

// 动画
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from { transform: translateY(20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
</style>