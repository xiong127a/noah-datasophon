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


 * @describe: step1-安装主机 
 * @Date: 2022-06-13 16:35:02
 * @LastEditTime: 2022-06-20 14:45:22
 * @FilePath: \ddh-ui\src\components\steps\step1.vue
-->
<template>
  <div class="steps1 steps">
    <div class="hero-section">
      <h1 class="hero-title">创建您的集群</h1>
      <p class="hero-subtitle">输入主机信息，开始构建您的大数据平台</p>
    </div>
    
    <div class="form-wrapper">
      <div class="form-content">
        <a-form :form="form" layout="vertical">
          <div class="host-input-section">
            <h2 class="section-title">主机列表</h2>
            <p class="section-description">
              使用IP或主机名输入主机列表，按逗号分隔或使用主机域批量添加，例如：10.3.144.[19-23]
            </p>
            <div class="host-input-container">
              <a-form-item>
                <a-textarea 
                  v-decorator="[
                    'hosts',
                    {initialValue: steps1.hosts, rules: [{ required: true, message: '请输入主机列表' }] },
                  ]" 
                  placeholder="例如：192.168.1.1,192.168.1.2 或 10.3.144.[19-23]" 
                  :autosize="{ minRows: 4, maxRows: 8 }"
                  class="host-input"
                />
              </a-form-item>
            </div>
          </div>
          
          <div class="credentials-section">
            <h2 class="section-title">连接凭证</h2>
            <p class="section-description">
              提供SSH连接信息以便系统能够连接并部署服务
            </p>
            <div class="credentials-grid">
              <a-form-item label="SSH用户名" class="form-item">
                <a-input 
                  v-decorator="[
                    'sshUser',
                    { initialValue: steps1.sshUser, rules: [{ required: true, message: '请输入SSH用户名' }] },
                  ]" 
                  placeholder="root" 
                  class="input"
                />
              </a-form-item>
              
              <a-form-item label="SSH端口" class="form-item">
                <a-input-number 
                  v-decorator="[
                    'sshPort', 
                    {initialValue: steps1.sshPort || 22, rules: [{ required: true, message: 'SSH端口不能为空' }] }
                  ]" 
                  :min="1" 
                  :max="65535" 
                  placeholder="22" 
                  class="input"
                />
              </a-form-item>
              
              <a-form-item label="SSH密码" class="form-item">
                <a-input-password 
                  v-decorator="[
                    'sshPassword', 
                    {initialValue: steps1.sshPassword, rules: [{ required: true, message: 'SSH密码不能为空' }] }
                  ]" 
                  placeholder="输入密码" 
                  class="input"
                />
              </a-form-item>
            </div>
          </div>
          
          <div class="tips-section">
            <a-icon type="info-circle" class="tips-icon" />
            <div class="tips-content">
              <p>确保所有主机可通过SSH连接，且密码一致。如需使用不同密码，请分批添加主机。</p>
            </div>
          </div>
        </a-form>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  inject: ["handleCancel", "currentStepsAdd", "currentStepsSub"],
  props: {
    steps1: Object,
  },
  data() {
    return {
      form: this.$form.createForm(this),
    };
  },
  created() {
    // 设置默认值
    setTimeout(() => {
      this.form.setFieldsValue({
        hosts: '192.168.200.21,192.168.30.200,192.168.30.201',
        sshUser: 'root',
        sshPort: 22,
        sshPassword: 'root'
      });
    });
  }
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

// 苹果设计系统字体
.apple-font() {
  font-family: "SF Pro Display", "SF Pro Icons", "PingFang SC", "Helvetica Neue", Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
}

.steps1 {
  margin: 0;
  max-width: 100%;
  background-color: @apple-white;
  overflow: hidden;
  animation: fadeIn 0.8s ease-out;
  
  .hero-section {
    text-align: center;
    margin-bottom: 3.5rem;
    
    .hero-title {
      .apple-font();
      font-size: 2.8rem;
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
      font-size: 1.4rem;
      line-height: 1.4;
      letter-spacing: 0;
      font-weight: 400;
      color: @apple-gray;
      margin: 0;
      max-width: 760px;
      margin: 0 auto;
    }
  }
  
  .form-wrapper {
    max-width: 860px;
    margin: 0 auto;
    padding: 0 1rem;
  }
  
  .form-content {
    position: relative;
    animation: slideUp 0.6s ease-out;
    animation-fill-mode: both;
    animation-delay: 0.2s;
  }
  
  .section-title {
    .apple-font();
    font-size: 1.6rem;
    font-weight: 600;
    line-height: 1.2;
    color: @apple-black;
    margin: 0 0 0.6rem 0;
  }
  
  .section-description {
    .apple-font();
    font-size: 1rem;
    line-height: 1.5;
    color: @apple-gray;
    margin: 0 0 1.5rem 0;
  }
  
  .host-input-section {
    margin-bottom: 3rem;
  }
  
  .host-input-container {
    .ant-form-item {
      margin-bottom: 0;
    }
    
    .host-input {
      .apple-font();
      font-size: 1rem;
      font-family: "SF Mono", SFMono-Regular, ui-monospace, Menlo, Monaco, Consolas, monospace;
      resize: none;
      border: none;
      background-color: @apple-gray-light;
      border-radius: 1rem;
      padding: 1.2rem;
      transition: all 0.3s;
      
      &:focus {
        outline: none;
        box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.2);
      }
      
      &::placeholder {
        color: @apple-gray;
      }
    }
  }
  
  .credentials-section {
    margin-bottom: 2.5rem;
  }
  
  .credentials-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 1.5rem;
    
    .form-item {
      margin-bottom: 0;
      
      label {
        .apple-font();
        font-size: 0.9rem;
        font-weight: 500;
        color: @apple-black;
        margin-bottom: 0.5rem;
      }
      
      .input {
        .apple-font();
        font-size: 1rem;
        height: 3rem;
        border: none;
        background-color: @apple-gray-light;
        border-radius: 0.75rem;
        padding: 0 1rem;
        width: 100%;
        transition: all 0.3s;
        
        &:focus, &:hover {
          outline: none;
          box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.2);
        }
        
        &::placeholder {
          color: @apple-gray;
        }
      }
      
      // 数字输入框特殊处理
      /deep/ .ant-input-number {
        width: 100%;
        border: none;
        background-color: @apple-gray-light;
        
        .ant-input-number-input {
          height: 3rem;
          padding: 0 1rem;
        }
        
        .ant-input-number-handler-wrap {
          opacity: 0;
          transition: opacity 0.3s;
        }
        
        &:hover .ant-input-number-handler-wrap {
          opacity: 1;
        }
      }
      
      // 密码输入框特殊处理
      /deep/ .ant-input-password {
        background-color: @apple-gray-light;
        border: none;
        border-radius: 0.75rem;
        
        .ant-input {
          height: 3rem;
          background-color: transparent;
          border: none;
          padding: 0 1rem;
          .apple-font();
          font-size: 1rem;
        }
        
        .ant-input-suffix {
          margin-right: 0.5rem;
          
          .anticon {
            color: @apple-gray;
            transition: color 0.3s;
          }
          
          &:hover .anticon {
            color: @apple-blue;
          }
        }
        
        &:focus-within {
          box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.2);
        }
      }
    }
  }
  
  .tips-section {
    display: flex;
    align-items: flex-start;
    padding: 1.2rem;
    background-color: @apple-gray-light;
    border-radius: 1rem;
    animation: fadeIn 0.8s ease-out;
    animation-delay: 0.5s;
    animation-fill-mode: both;
    
    .tips-icon {
      font-size: 1.2rem;
      color: @apple-blue;
      margin-right: 1rem;
      margin-top: 0.2rem;
    }
    
    .tips-content {
      flex: 1;
      
      p {
        .apple-font();
        font-size: 0.9rem;
        line-height: 1.5;
        color: @apple-gray;
        margin: 0;
      }
    }
  }
}

// 动画
@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// 响应式设计
@media (max-width: 768px) {
  .steps1 {
    .hero-section {
      .hero-title {
        font-size: 2.2rem;
      }
      
      .hero-subtitle {
        font-size: 1.1rem;
      }
    }
    
    .credentials-grid {
      grid-template-columns: 1fr;
    }
  }
}
</style>