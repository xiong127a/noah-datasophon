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
    <!-- 加载状态 -->
    <div v-if="clusterInfoLoading" class="loading-container">
      <div class="loading-card">
        <div class="loading-spinner">
          <div class="spinner-circle"></div>
          <div class="spinner-inner">
            <div class="spinner-dot"></div>
          </div>
        </div>
        
        <h2 class="loading-title">配置您的集群</h2>
        <p class="loading-desc">正在准备集群配置环境</p>
        
        <div class="loading-progress">
          <div class="progress-bar" :style="{ width: `${loadingProgress}%` }"></div>
        </div>
      </div>
    </div>

    <!-- 主要内容 -->
    <div v-else>
      <div class="hero-section">
        <h1 class="hero-title">创建您的集群</h1>
        <p class="hero-subtitle">输入主机信息，开始构建您的大数据平台</p>
      </div>
      
      <div class="form-wrapper">
        <div class="form-content">
          <a-form :form="form" layout="vertical">
            
            <!-- K8S集群配置 -->
            <template v-if="clusterType === 'kubernetes'">
              <div class="k8s-config-section">
                <h2 class="section-title">Kubernetes配置</h2>
                <p class="section-description">
                  上传或输入Kubernetes配置文件，然后选择命名空间
                </p>
                
                <div class="k8s-config-container">
                  <a-form-item label="Kubernetes配置">
                    <div class="config-actions">
                      <a-button 
                        type="dashed" 
                        size="small" 
                        @click="triggerFileInput"
                        :loading="fileLoading"
                        class="apple-button"
                      >
                        <a-icon type="folder-open" />
                        从文件载入
                      </a-button>
                    </div>
                    <div class="textarea-wrapper">
                      <a-textarea
                        v-decorator="[
                          'kubeConfigContent',
                          { rules: [{ required: true, message: 'Kubernetes配置不能为空!' }] }
                        ]"
                        placeholder="请输入Kubernetes配置内容，或点击上方按钮从文件载入...
支持标准的 ~/.kube/config 文件（无扩展名）"
                        :rows="8"
                        @change="onKubeConfigChange"
                        class="apple-textarea"
                      />
                      <a-icon
                        v-if="kubeConfigContent"
                        type="close-circle"
                        theme="filled"
                        class="clear-btn"
                        @click="clearConfig"
                      />
                    </div>
                    <input
                      ref="fileInput"
                      type="file"
                      @change="handleFileSelect"
                      style="display: none"
                    />
                  </a-form-item>
                  
                  <a-form-item label="命名空间">
                    <div class="namespace-selector-container">
                      <!-- 选择命名空间模式 -->
                      <a-select
                        v-if="!isCreatingNewNamespace"
                        v-decorator="[
                          'namespaceSelect',
                          { 
                            rules: [{ required: true, message: '请选择命名空间!' }] 
                          }
                        ]"
                        :placeholder="!kubeConfigContent ? '请先输入Kubernetes配置' : '请选择或搜索命名空间'"
                        :loading="namespacesLoading"
                        :disabled="!kubeConfigContent || !kubeConfigContent.trim()"
                        show-search
                        :filter-option="false"
                        @search="onNamespaceSearch"
                        @select="onNamespaceSelect"
                        @click="onNamespaceDropdownClick"
                        @focus="onNamespaceDropdownClick"
                        :dropdown-match-select-width="true"
                        class="apple-namespace-select"
                        size="large"
                      >
                        <a-select-option key="__create_new__" value="__create_new__" class="create-new-option">
                          <div class="create-new-content">
                            <a-icon type="plus" class="create-icon" />
                            <span class="create-text">创建新的命名空间</span>
                          </div>
                        </a-select-option>
                        <a-select-option key="divider" disabled v-if="filteredNamespaces.length > 0" class="divider-option">
                          <div class="namespace-divider"></div>
                        </a-select-option>
                        <a-select-option 
                          v-for="ns in filteredNamespaces" 
                          :key="ns" 
                          :value="ns"
                          class="namespace-option"
                        >
                          <div class="namespace-item">
                            <a-icon type="database" class="namespace-icon" />
                            <span class="namespace-name">{{ ns }}</span>
                          </div>
                        </a-select-option>
                      </a-select>
                      
                      <!-- 创建命名空间模式 -->
                      <div v-if="isCreatingNewNamespace" class="create-namespace-input-container">
                        <a-input
                          v-decorator="[
                            'namespaceInput',
                            { 
                              rules: [{ required: true, message: '请输入命名空间名称!' }] 
                            }
                          ]"
                          placeholder="请输入新的命名空间名称"
                          @change="onNamespaceInputChange"
                          class="apple-namespace-input"
                          size="large"
                        >
                          <a-icon 
                            slot="suffix" 
                            type="close-circle" 
                            @click="cancelCreateNamespace" 
                            class="cancel-create-icon"
                          />
                        </a-input>
                      </div>
                      
                      <!-- 隐藏字段，用于最终提交的namespace值 -->
                      <a-input
                        v-decorator="[
                          'namespace',
                          { 
                            rules: [{ required: true, message: '请选择或输入命名空间!' }] 
                          }
                        ]"
                        style="display: none;"
                      />
                      
                      <!-- 命名空间操作提示信息 -->
                      <div v-if="isCreatingNewNamespace && customNamespaceInput" 
                           class="namespace-tip"
                           :class="isNamespaceExists ? 'namespace-exists-tip' : 'namespace-create-tip'">
                        <a-icon 
                          :type="isNamespaceExists ? 'check-circle' : 'info-circle'" 
                          class="tip-icon"
                          :class="isNamespaceExists ? 'exists-icon' : 'create-icon'"
                        />
                        <span class="tip-text">
                          {{ isNamespaceExists ? '将使用现有命名空间' : '将创建新命名空间' }} 
                          <span class="namespace-value">{{ customNamespaceInput }}</span>
                        </span>
                      </div>
                    </div>
                  </a-form-item>
                </div>
              </div>
            </template>
            
            <!-- 传统集群配置 -->
            <template v-else>
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
                      class="apple-textarea"
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
                    />
                  </a-form-item>
                  
                  <a-form-item label="SSH密码" class="form-item">
                    <a-input-password 
                      v-decorator="[
                        'sshPassword', 
                        {initialValue: steps1.sshPassword, rules: [{ required: true, message: 'SSH密码不能为空' }] }
                      ]" 
                      placeholder="输入密码"
                    />
                  </a-form-item>
                </div>
              </div>
            </template>
            
            <div class="tips-section">
              <a-icon type="info-circle" class="tips-icon" />
              <div class="tips-content">
                <p v-if="clusterType === 'traditional'">确保所有主机可通过SSH连接，且密码一致。如需使用不同密码，请分批添加主机。</p>
                <p v-else>确保Kubernetes集群配置正确，且具有足够的权限来创建和管理资源。</p>
              </div>
            </div>
          </a-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  inject: ["handleCancel", "currentStepsAdd", "currentStepsSub", "clusterId"],
  props: {
    steps1: Object,
    clusterInfo: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      form: this.$form.createForm(this),
      
      // 集群类型选择
      clusterType: 'traditional',
      
      // K8S相关数据
      kubeConfigContent: '',
      namespaces: [],
      selectedNamespace: '',
      namespacesLoading: false,
      showCustomNamespace: false,
      fileLoading: false,
      isCreatingNewNamespace: false,
      customNamespaceInput: '',
      namespaceSearchText: '',
      clusterInfoLoading: true, // 新增：加载状态
      loadingProgress: 0, // 新增：加载进度
    };
  },
  
  computed: {
    filteredNamespaces() {
      if (!this.namespaceSearchText) {
        return this.namespaces;
      }
      return this.namespaces.filter(ns => 
        ns.toLowerCase().includes(this.namespaceSearchText.toLowerCase())
      );
    },
    
    isNamespaceExists() {
      if (!this.customNamespaceInput || !this.namespaces.length) {
        return false;
      }
      return this.namespaces.includes(this.customNamespaceInput);
    }
  },
  
  watch: {
    steps1: {
      handler(newVal) {
        if (newVal && newVal.clusterType) {
          this.clusterType = newVal.clusterType;
        }
        if (newVal && newVal.kubeConfigContent) {
          this.kubeConfigContent = newVal.kubeConfigContent;
        }
        if (newVal && newVal.namespace) {
          this.selectedNamespace = newVal.namespace;
        }
      },
      immediate: true
    },
    
    clusterInfo: {
      handler(newVal) {
        if (newVal && Object.keys(newVal).length > 0) {
          this.initializeWithClusterInfo(newVal);
        }
      },
      immediate: true
    }
  },
  
  created() {
    console.log('Step1 created, clusterId:', this.clusterId);
    // 设置默认值
    setTimeout(() => {
      this.form.setFieldsValue({
        hosts: '192.168.30.[200-204],192.168.30.206',
        sshUser: 'root',
        sshPort: 22,
        sshPassword: 'root'
      });
    });
  },
  
  mounted() {
    console.log('Step1 mounted, 开始获取集群信息');
    // 获取集群信息以确定集群类型
    this.getClusterInfo();
  },
  
  methods: {
    // 获取集群信息
    async getClusterInfo() {
      console.log('开始获取集群信息，clusterId:', this.clusterId);
      const startTime = Date.now();
      const minLoadingTime = 2000; // 最少显示2秒加载状态
      
      // 启动进度条动画
      this.startLoadingProgress();
      
      try {
        if (!this.clusterId) {
          console.warn('clusterId 未提供，使用默认集群类型');
          this.clusterType = 'traditional';
        } else {
          const res = await this.$axiosGet(`/ddh/api/cluster/detail/${this.clusterId}`);
          if (res.code === 200 && res.data) {
            console.log('获取到集群信息:', res.data);
            // 将后端的depType映射为前端的clusterType
            if (res.data.depType === 'Kubernetes') {
              this.clusterType = 'kubernetes';
              // 如果是K8S集群且已有配置，加载已保存的配置
              if (res.data.kubeConfig) {
                this.kubeConfigContent = res.data.kubeConfig;
                this.form.setFieldsValue({
                  kubeConfigContent: res.data.kubeConfig
                });
                this.loadNamespaces();
              }
              if (res.data.namespace) {
                this.selectedNamespace = res.data.namespace;
                this.form.setFieldsValue({
                  namespace: res.data.namespace
                });
              }
            } else {
              this.clusterType = 'traditional';
            }
          } else {
            console.error('获取集群信息失败:', res.msg);
            this.clusterType = 'traditional'; // 默认为传统集群
          }
        }
      } catch (error) {
        console.error('获取集群信息异常:', error);
        this.clusterType = 'traditional'; // 默认为传统集群
      } finally {
        // 计算已经过去的时间
        const elapsed = Date.now() - startTime;
        console.log(`集群信息获取完成，耗时: ${elapsed}ms，集群类型:`, this.clusterType);
        
        // 如果时间不足minLoadingTime，则等待剩余时间
        if (elapsed < minLoadingTime) {
          const waitTime = minLoadingTime - elapsed;
          console.log(`等待剩余时间: ${waitTime}ms`);
          await new Promise(resolve => setTimeout(resolve, waitTime));
        }
        
        // 完成加载进度条
        this.loadingProgress = 100;
        
        // 等待进度条动画完成后，隐藏加载状态
        setTimeout(() => {
          console.log('隐藏加载状态，显示主界面');
          this.clusterInfoLoading = false;
        }, 500);
      }
    },
    
    // 启动进度条动画
    startLoadingProgress() {
      console.log('开始进度条动画');
      this.loadingProgress = 0;
      const interval = setInterval(() => {
        if (this.loadingProgress < 90) {
          this.loadingProgress += Math.floor(Math.random() * 10) + 1;
          if (this.loadingProgress > 90) {
            this.loadingProgress = 90;
          }
        } else {
          clearInterval(interval);
        }
      }, 200);
    },

    // 根据集群信息初始化组件
    initializeWithClusterInfo(clusterInfo) {
      console.log('正在根据集群信息初始化step1:', clusterInfo);
      
      // 设置集群类型
      if (clusterInfo.depType === 'Kubernetes') {
        this.clusterType = 'kubernetes';
        
        // 如果是K8S集群且已有配置，加载已保存的配置
        if (clusterInfo.kubeConfig) {
          this.kubeConfigContent = clusterInfo.kubeConfig;
          this.form.setFieldsValue({
            kubeConfigContent: clusterInfo.kubeConfig
          });
          this.loadNamespaces();
        }
        
        if (clusterInfo.namespace) {
          this.selectedNamespace = clusterInfo.namespace;
          // 根据命名空间是否存在决定显示模式
          if (this.namespaces.includes(clusterInfo.namespace)) {
            this.isCreatingNewNamespace = false;
            this.form.setFieldsValue({
              namespaceSelect: clusterInfo.namespace,
              namespace: clusterInfo.namespace,
              namespaceInput: ''
            });
          } else {
            this.isCreatingNewNamespace = true;
            this.customNamespaceInput = clusterInfo.namespace;
            this.form.setFieldsValue({
              namespaceInput: clusterInfo.namespace,
              namespace: clusterInfo.namespace,
              namespaceSelect: undefined
            });
          }
        }
      } else {
        this.clusterType = 'traditional';
      }
    },
    
    // K8S配置文件相关方法
    triggerFileInput() {
      this.$refs.fileInput.click();
    },
    
    handleFileSelect(event) {
      const file = event.target.files[0];
      if (!file) return;
      
      this.fileLoading = true;
      const reader = new FileReader();
      
      reader.onload = (e) => {
        this.kubeConfigContent = e.target.result;
        this.$nextTick(() => {
          this.form.setFieldsValue({
            kubeConfigContent: this.kubeConfigContent
          });
          // 确保在设置完表单值后再调用onKubeConfigChange
          this.$nextTick(() => {
            this.onKubeConfigChange();
            this.fileLoading = false;
          });
        });
      };
      
      reader.onerror = () => {
        this.$message.error('文件读取失败');
        this.fileLoading = false;
      };
      
      reader.readAsText(file);
    },
    
    clearConfig() {
      this.kubeConfigContent = '';
      this.namespaces = [];
      this.selectedNamespace = '';
      this.isCreatingNewNamespace = false;
      this.customNamespaceInput = '';
      this.$nextTick(() => {
        this.form.setFieldsValue({
          kubeConfigContent: '',
          namespace: ''
        });
      });
    },
    
    onKubeConfigChange() {
      const content = this.form.getFieldValue('kubeConfigContent');
      if (content && content.trim()) {
        this.kubeConfigContent = content;
        this.loadNamespaces();
      }
    },
    
    // 命名空间相关方法
    async loadNamespaces() {
      if (!this.kubeConfigContent || !this.kubeConfigContent.trim()) {
        return;
      }
      
      this.namespacesLoading = true;
      try {
        // 使用$axiosJsonPost确保发送JSON格式
        const res = await this.$axiosJsonPost('/ddh/api/cluster/namespaces', {
          kubeConfigContent: this.kubeConfigContent
        });
        if (res.code === 200) {
          this.namespaces = res.data.namespaces || [];
          const defaultNamespace = res.data.defaultNamespace;
          
          // 如果有默认命名空间，自动选中或设置创建模式
          if (defaultNamespace) {
            if (this.namespaces.includes(defaultNamespace)) {
              // 默认命名空间存在，直接选中
              this.selectedNamespace = defaultNamespace;
              this.isCreatingNewNamespace = false;
              this.$nextTick(() => {
                this.form.setFieldsValue({ 
                  namespaceSelect: defaultNamespace,
                  namespace: defaultNamespace,
                  namespaceInput: ''
                });
              });
            } else {
              // 默认命名空间不存在，进入创建模式
              this.isCreatingNewNamespace = true;
              this.customNamespaceInput = defaultNamespace;
              this.$nextTick(() => {
                this.form.setFieldsValue({ 
                  namespaceInput: defaultNamespace,
                  namespace: defaultNamespace,
                  namespaceSelect: undefined
                });
              });
            }
          }
        } else {
          this.$message.error(res.msg || '获取命名空间失败');
        }
      } catch (error) {
        console.error('获取命名空间失败:', error);
        this.$message.error('获取命名空间失败');
      } finally {
        this.namespacesLoading = false;
      }
    },
    
    onNamespaceSearch(value) {
      this.namespaceSearchText = value;
    },
    
    onNamespaceSelect(value) {
      if (value === '__create_new__') {
        this.isCreatingNewNamespace = true;
        this.customNamespaceInput = '';
        // 清空所有相关字段
        this.$nextTick(() => {
          this.form.setFieldsValue({
            namespaceSelect: undefined,
            namespaceInput: '',
            namespace: ''
          });
        });
      } else {
        this.selectedNamespace = value;
        this.isCreatingNewNamespace = false;
        this.customNamespaceInput = '';
        this.$nextTick(() => {
          this.form.setFieldsValue({
            namespaceSelect: value,
            namespaceInput: '',
            namespace: value  // 设置最终的namespace值
          });
        });
      }
    },
    
    onNamespaceDropdownClick() {
      if (!this.namespaces.length && this.kubeConfigContent) {
        this.loadNamespaces();
      }
    },
    
    onNamespaceInputChange(e) {
      this.customNamespaceInput = e.target.value;
      // 同时更新隐藏的namespace字段
      this.$nextTick(() => {
        this.form.setFieldsValue({
          namespace: e.target.value
        });
      });
    },
    
    cancelCreateNamespace() {
      this.isCreatingNewNamespace = false;
      this.customNamespaceInput = '';
      this.$nextTick(() => {
        this.form.setFieldsValue({
          namespaceSelect: undefined,
          namespaceInput: '',
          namespace: ''
        });
      });
    },
    
    // 保存K8S配置
    async saveKubernetesConfig() {
      try {
        const values = await new Promise((resolve, reject) => {
          this.form.validateFields((err, values) => {
            if (!err) {
              resolve(values);
            } else {
              reject(err);
            }
          });
        });
        
        const params = {
          clusterId: this.clusterId,
          kubeConfigContent: values.kubeConfigContent,
          namespace: values.namespace  // 使用最终的namespace字段
        };
        
        // 使用$axiosJsonPost确保发送JSON格式
        const res = await this.$axiosJsonPost('/ddh/api/cluster/kube-config', params);
        if (res.code === 200) {
          this.$message.success('Kubernetes配置保存成功');
          return true;
        } else {
          this.$message.error(res.msg || 'Kubernetes配置保存失败');
          return false;
        }
      } catch (error) {
        console.error('保存Kubernetes配置失败:', error);
        this.$message.error('保存Kubernetes配置失败');
        return false;
      }
    }
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
@apple-text-primary: #1d1d1f;
@apple-text-secondary: #86868b;

// 强制全局下拉框圆角样式
/deep/ .ant-select-selector {
  border-radius: 1rem !important;
}

/deep/ .ant-select-single .ant-select-selector {
  border-radius: 1rem !important;
}

// 强制下拉选项蓝色字体样式 - 全局生效
/deep/ .ant-select-dropdown .create-new-option {
  .create-new-content {
    .create-text {
      color: #0071e3 !important;
      font-weight: 600 !important;
    }
    .create-icon {
      color: #0071e3 !important;
    }
  }
}

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
  
  .loading-container {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 300px; /* Adjust as needed */
    background-color: @apple-white;
  }

  .loading-card {
    background-color: @apple-white;
    border-radius: 16px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
    padding: 2.5rem;
    text-align: center;
    width: 400px;
    max-width: 90%;
  }

  .loading-spinner {
    position: relative;
    width: 60px;
    height: 60px;
    margin: 0 auto 1.5rem;
    border-radius: 50%;
    border: 4px solid rgba(0, 0, 0, 0.1);
    border-top-color: @apple-blue;
    animation: spin 1s linear infinite;
  }

  .spinner-circle {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 40px;
    height: 40px;
    border-radius: 50%;
    border: 4px solid rgba(0, 0, 0, 0.1);
    border-top-color: @apple-blue;
    animation: spin 1s linear infinite;
  }

  .spinner-inner {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 32px;
    height: 32px;
    border-radius: 50%;
    border: 4px solid rgba(0, 0, 0, 0.1);
    border-top-color: @apple-blue;
    animation: spin 1s linear infinite;
  }

  .spinner-dot {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background-color: @apple-blue;
  }

  .loading-title {
    .apple-font();
    font-size: 1.8rem;
    font-weight: 600;
    color: @apple-black;
    margin-bottom: 0.8rem;
  }

  .loading-desc {
    .apple-font();
    font-size: 1rem;
    color: @apple-gray;
    margin-bottom: 1.5rem;
  }

  .loading-progress {
    width: 100%;
    height: 8px;
    background-color: @apple-gray-light;
    border-radius: 4px;
    overflow: hidden;
  }

  .progress-bar {
    height: 100%;
    background: linear-gradient(to right, @apple-blue, @apple-blue-hover);
    border-radius: 4px;
    transition: width 0.3s ease-in-out;
  }

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
  

  
  // K8S配置区域
  .k8s-config-section {
    margin-bottom: 3rem;
    
    .k8s-config-container {
      .config-actions {
        margin-bottom: 1rem;
        display: flex;
        gap: 12px;
        align-items: center;
        
        .apple-button {
          border-radius: 8px;
          border: 1px dashed @apple-blue;
          color: @apple-blue;
          height: 32px;
          padding: 0 16px;
          font-size: 14px;
          
          &:hover {
            border-color: @apple-blue-hover;
            color: @apple-blue-hover;
            background: rgba(24, 144, 255, 0.06);
          }
        }
        
        .apple-link-button {
          color: #ff4d4f;
          border: 1px solid #ffccc7;
          background: #fff2f0;
          border-radius: 6px;
          height: 32px;
          padding: 0 12px;
          font-size: 13px;
          transition: all 0.3s;
          
          &:hover {
            color: #ff7875;
            border-color: #ff7875;
            background: #ffebee;
          }
          
          &:focus {
            color: #ff4d4f;
            border-color: #ff4d4f;
          }
          
          .anticon {
            margin-right: 4px;
            font-size: 12px;
          }
        }
      }
      
      // K8S配置文本域样式
      /deep/ .apple-textarea {
        .apple-font();
        font-family: "SF Mono", SFMono-Regular, ui-monospace, Menlo, Monaco, Consolas, monospace !important;
        resize: none !important;
        border: none !important;
        background-color: @apple-gray-light !important;
        border-radius: 1rem !important;
        padding: 1.2rem !important;
        transition: all 0.3s;
        
        &:hover {
          background-color: darken(@apple-gray-light, 2%) !important;
          border: none !important;
        }
        
        &:focus {
          outline: none !important;
          box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.2) !important;
          background-color: @apple-white !important;
          border: none !important;
        }
        
        &::placeholder {
          color: @apple-gray !important;
          font-weight: 400;
        }
      }
      
      // 通用输入框样式
      /deep/ .ant-input {
        .apple-font();
        resize: none !important;
        border: none !important;
        background-color: @apple-gray-light !important;
        border-radius: 1rem !important;
        padding: 1.2rem !important;
        transition: all 0.3s;
        
        &:hover {
          background-color: darken(@apple-gray-light, 2%) !important;
          border: none !important;
        }
        
        &:focus {
          outline: none !important;
          box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.2) !important;
          background-color: @apple-white !important;
          border: none !important;
        }
        
        &::placeholder {
          color: @apple-gray !important;
          font-weight: 400;
        }
      }
      
      .namespace-selector-container {
        // 命名空间选择器样式
        /deep/ .apple-namespace-select {
          width: 100%;
          
          .ant-select-selector {
            .apple-font();
            border: none !important;
            background-color: @apple-gray-light !important;
            border-radius: 1rem !important;
            padding: 0.8rem 1rem !important;
            transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
            min-height: 48px !important;
            display: flex;
            align-items: center;
            
            &:hover {
              background-color: darken(@apple-gray-light, 2%) !important;
            }
          }
          
          &.ant-select-focused .ant-select-selector {
            background-color: @apple-white !important;
            box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.2) !important;
          }
          
          .ant-select-selection-placeholder {
            color: @apple-gray !important;
            font-weight: 400;
          }
          
          .ant-select-arrow {
            color: @apple-gray !important;
            transition: transform 0.3s;
          }
          
          &.ant-select-open .ant-select-arrow {
            transform: rotate(180deg);
          }
        }
        
        // 全局下拉框样式修复 - 确保圆角正确应用
        /deep/ .ant-select {
          .ant-select-selector {
            .apple-font();
            border: none !important;
            background-color: @apple-gray-light !important;
            border-radius: 1rem !important;
            padding: 0.8rem 1rem !important;
            transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
            min-height: 48px !important;
            display: flex;
            align-items: center;
            
            &:hover {
              background-color: darken(@apple-gray-light, 2%) !important;
            }
          }
          
          &.ant-select-focused .ant-select-selector {
            background-color: @apple-white !important;
            box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.2) !important;
          }
        }
        
        // 下拉菜单样式优化
        /deep/ .ant-select-dropdown {
          border-radius: 12px !important;
          overflow: hidden;
          box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12), 0 2px 8px rgba(0, 0, 0, 0.04) !important;
          border: 1px solid rgba(0, 0, 0, 0.06) !important;
          padding: 8px !important;
          animation: dropdownFadeIn 0.2s ease-out !important;
          
          // 创建新命名空间选项
          .create-new-option {
            margin-bottom: 8px !important;
            
            .ant-select-item-option-content {
              padding: 0 !important;
            }
            
            .create-new-content {
              display: flex;
              align-items: center;
              padding: 12px 16px;
              background: linear-gradient(135deg, #e3f2fd, #f0f8ff);
              border: 2px solid @apple-blue;
              border-radius: 8px;
              transition: all 0.3s;
              
              &:hover {
                background: linear-gradient(135deg, #bbdefb, #e3f2fd);
                transform: translateY(-1px);
                box-shadow: 0 4px 12px rgba(0, 113, 227, 0.2);
              }
              
              .create-icon {
                color: @apple-blue !important;
                font-size: 16px;
                margin-right: 8px;
              }
              
              .create-text {
                color: @apple-blue !important;
                font-weight: 600 !important;
                .apple-font();
                font-size: 14px;
              }
            }
            
            // 覆盖默认选中样式
            &.ant-select-item-option-selected {
              background-color: transparent !important;
              
              .create-new-content {
                background: linear-gradient(135deg, @apple-blue, lighten(@apple-blue, 5%)) !important;
                
                .create-icon,
                .create-text {
                  color: @apple-white !important;
                }
              }
            }
          }
          
          // 分隔线选项
          .divider-option {
            .ant-select-item-option-content {
              padding: 0 !important;
              margin: 8px 0 !important;
            }
            
            .namespace-divider {
              height: 2px;
              background: linear-gradient(90deg, transparent, @apple-blue, transparent);
              margin: 0;
              opacity: 0.3;
            }
          }
          
          // 普通命名空间选项
          .namespace-option {
            .ant-select-item-option-content {
              padding: 0 !important;
            }
            
            .namespace-item {
              display: flex;
              align-items: center;
              padding: 10px 16px;
              transition: all 0.2s;
              border-radius: 6px;
              margin: 2px 0;
              
              &:hover {
                background-color: rgba(0, 113, 227, 0.05);
              }
              
              .namespace-icon {
                color: @apple-gray;
                font-size: 14px;
                margin-right: 8px;
              }
              
              .namespace-name {
                color: @apple-text-primary;
                .apple-font();
                font-weight: 400;
              }
            }
          }
          
          // 选中状态
          .ant-select-item-option-selected {
            background-color: rgba(0, 113, 227, 0.1) !important;
            
            .namespace-item {
              .namespace-icon {
                color: @apple-blue;
              }
              
              .namespace-name {
                color: @apple-blue;
                font-weight: 500;
              }
            }
          }
        }
        
        // 创建命名空间输入框容器
        .create-namespace-input-container {
          position: relative;
          
          .apple-namespace-input {
            /deep/ .ant-input {
              .apple-font();
              border: none !important;
              background-color: @apple-gray-light !important;
              border-radius: 1rem !important;
              padding: 0.8rem 1rem !important;
              transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
              min-height: 48px !important;
              font-size: 1rem;
              
              &:hover {
                background-color: darken(@apple-gray-light, 2%) !important;
              }
              
              &:focus {
                background-color: @apple-white !important;
                box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.2) !important;
                outline: none !important;
              }
              
              &::placeholder {
                color: @apple-gray !important;
                font-weight: 400;
              }
            }
            
            // 关闭按钮样式
            /deep/ .ant-input-suffix {
              .cancel-create-icon {
                color: rgba(0, 0, 0, 0.25);
                cursor: pointer;
                font-size: 16px;
                transition: all 0.3s;
                
                &:hover {
                  color: #ff4d4f;
                  transform: scale(1.1);
                }
              }
            }
          }
        }
        
        // 命名空间提示信息
        .namespace-tip {
          margin-top: 0.75rem;
          padding: 0.75rem 1rem;
          border-radius: 0.75rem;
          font-size: 0.9rem;
          display: flex;
          align-items: center;
          transition: all 0.3s;
          animation: fadeInUp 0.3s ease-out;
          
          .tip-icon {
            margin-right: 8px;
            font-size: 16px;
          }
          
          .tip-text {
            .apple-font();
            font-weight: 400;
            
            .namespace-value {
              font-weight: 600;
              margin-left: 4px;
            }
          }
          
          &.namespace-create-tip {
            background: linear-gradient(135deg, rgba(0, 113, 227, 0.05), rgba(0, 113, 227, 0.02));
            border: 1px solid rgba(0, 113, 227, 0.15);
            
            .tip-icon.create-icon {
              color: @apple-blue;
            }
            
            .tip-text {
              color: @apple-text-primary;
              
              .namespace-value {
                color: @apple-blue;
              }
            }
          }
          
          &.namespace-exists-tip {
            background: linear-gradient(135deg, rgba(82, 196, 26, 0.05), rgba(82, 196, 26, 0.02));
            border: 1px solid rgba(82, 196, 26, 0.15);
            
            .tip-icon.exists-icon {
              color: #52c41a;
            }
            
            .tip-text {
              color: @apple-text-primary;
              
              .namespace-value {
                color: #52c41a;
              }
            }
          }
        }
      }
      .textarea-wrapper {
        position: relative;

        .clear-btn {
          position: absolute;
          top: 16px;
          right: 16px;
          color: rgba(0, 0, 0, 0.25);
          cursor: pointer;
          font-size: 16px;
          transition: color 0.3s;
          z-index: 10;

          &:hover {
            color: rgba(0, 0, 0, 0.45);
          }
        }
      }
      
      // 输入框后缀图标样式
      /deep/ .ant-input-suffix {
        z-index: 5;
        
        .anticon {
          color: rgba(0, 0, 0, 0.25);
          cursor: pointer;
          font-size: 16px;
          transition: color 0.3s;
          
          &:hover {
            color: rgba(0, 0, 0, 0.45);
          }
        }
      }
    }
  }
  
  .host-input-section {
    margin-bottom: 3rem;
  }
  
  .host-input-container {
    .ant-form-item {
      margin-bottom: 0;
    }
    
    // 主机输入框样式
    /deep/ .ant-input {
      .apple-font();
      font-size: 1rem;
      font-family: "SF Mono", SFMono-Regular, ui-monospace, Menlo, Monaco, Consolas, monospace !important;
      resize: none !important;
      border: none !important;
      background-color: @apple-gray-light !important;
      border-radius: 1rem !important;
      padding: 1.2rem !important;
      transition: all 0.3s;
      min-height: 120px !important;
      
      &:hover {
        background-color: darken(@apple-gray-light, 2%) !important;
        border: none !important;
      }
      
      &:focus {
        outline: none !important;
        box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.2) !important;
        background-color: @apple-white !important;
        border: none !important;
      }
      
      &::placeholder {
        color: @apple-gray !important;
        font-weight: 400;
      }
    }
  }
  
  .credentials-section {
    margin-bottom: 3rem;
    
    .credentials-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 1.5rem;
      
      // 平板端布局
      @media (max-width: 1024px) and (min-width: 769px) {
        grid-template-columns: repeat(2, 1fr);
        
        .form-item:last-child {
          grid-column: 1 / -1;
          max-width: 50%;
        }
      }
      
      // 手机端布局
      @media (max-width: 768px) {
        grid-template-columns: 1fr;
        gap: 1.2rem;
      }
      
      .form-item {
        margin-bottom: 0;
        
        // Ant Design 表单标签样式
        /deep/ .ant-form-item-label {
          padding-bottom: 0.5rem;
          
          label {
            .apple-font();
            font-size: 0.95rem;
            font-weight: 500;
            color: @apple-black;
            line-height: 1.4;
            
            &::after {
              display: none; // 移除冒号
            }
          }
        }
        
                 // 基础输入框样式
         /deep/ .ant-input {
           .apple-font();
           font-size: 1rem;
           border: none !important;
           background-color: @apple-gray-light !important;
           border-radius: 0.5rem !important;
           padding: 0.6rem 1rem !important;
           transition: all 0.3s;
           height: 2.4rem !important;
           line-height: 1.2 !important;
           
           &:hover {
             background-color: darken(@apple-gray-light, 2%) !important;
             border: none !important;
           }
           
           &:focus {
             outline: none !important;
             box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.2) !important;
             background-color: @apple-white !important;
             border: none !important;
           }
           
           &::placeholder {
             color: @apple-gray !important;
             font-weight: 400;
           }
         }
         
         // 数字输入框完整重写
         /deep/ .ant-input-number {
           .apple-font();
           width: 100% !important;
           height: 2.4rem !important;
           border: none !important;
           background-color: @apple-gray-light !important;
           border-radius: 0.5rem !important;
           transition: all 0.3s;
           
           &:hover {
             background-color: darken(@apple-gray-light, 2%) !important;
             border: none !important;
           }
           
           &:focus-within {
             outline: none !important;
             box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.2) !important;
             background-color: @apple-white !important;
             border: none !important;
           }
           
           // 内部输入框
           .ant-input-number-input {
             .apple-font();
             font-size: 1rem !important;
             border: none !important;
             background: transparent !important;
             padding: 0.6rem 1rem !important;
             height: 2.4rem !important;
             line-height: 1.2 !important;
             box-shadow: none !important;
             
             &:focus {
               border: none !important;
               box-shadow: none !important;
               outline: none !important;
             }
             
             &::placeholder {
               color: @apple-gray !important;
               font-weight: 400;
             }
           }
           
           // 控制按钮组
           .ant-input-number-handler-wrap {
             background: transparent !important;
             border: none !important;
             border-radius: 0 !important;
             opacity: 0.7;
             transition: opacity 0.3s;
           }
           
           &:hover .ant-input-number-handler-wrap {
             opacity: 1;
           }
           
           .ant-input-number-handler {
             border: none !important;
             background: transparent !important;
             width: 16px !important;
             height: 12px !important;
             color: @apple-gray !important;
             
             &:hover {
               background: @apple-blue !important;
               color: @apple-white !important;
               border-radius: 2px !important;
             }
           }
         }
         
         // 密码输入框完整重写
         /deep/ .ant-input-password {
           .apple-font();
           border: none !important;
           background-color: @apple-gray-light !important;
           border-radius: 0.5rem !important;
           transition: all 0.3s;
           height: 2.4rem !important;
           
           &:hover {
             background-color: darken(@apple-gray-light, 2%) !important;
             border: none !important;
           }
           
           &:focus-within {
             outline: none !important;
             box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.2) !important;
             background-color: @apple-white !important;
             border: none !important;
           }
           
           // 内部输入框
           .ant-input {
             .apple-font();
             font-size: 1rem !important;
             border: none !important;
             background: transparent !important;
             padding: 0.6rem 1rem !important;
             height: 2.4rem !important;
             line-height: 1.2 !important;
             box-shadow: none !important;
             
             &:focus {
               border: none !important;
               box-shadow: none !important;
               outline: none !important;
               background: transparent !important;
             }
             
             &::placeholder {
               color: @apple-gray !important;
               font-weight: 400;
             }
           }
           
           // 眼睛图标
           .ant-input-suffix {
             .ant-input-password-icon {
               color: @apple-gray !important;
               transition: color 0.3s;
               
               &:hover {
                 color: @apple-blue !important;
               }
             }
           }
         }
      }
    }
  }
  
  .tips-section {
    display: flex;
    align-items: flex-start;
    background-color: @apple-gray-light;
    border-radius: 1rem;
    padding: 1.2rem;
    
    .tips-icon {
      color: @apple-blue;
      font-size: 1.2rem;
      margin-right: 0.8rem;
      margin-top: 0.1rem;
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

// 动画效果
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes dropdownFadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
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