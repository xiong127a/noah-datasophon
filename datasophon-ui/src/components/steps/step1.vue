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
    <div class="steps-title">为集群安装主机</div>
    <div class="steps-tips mgt20 mgb16">
      <a-icon class="steps-tips-icon mgr5" type="exclamation-circle" />
      <span v-if="!isK8sCluster">提示：使用IP或主机名输入主机列表，按逗号分隔或使用主机域批量添加主机，例如：10.3.144.[19-23]</span>
      <span v-else>提示：K8S集群配置，请上传或输入Kubernetes配置文件，然后选择命名空间</span>
    </div>
    <div class="form-content steps-body">
      <a-form :label-col="labelCol" :wrapper-col="wrapperCol" :form="form">
        
        <!-- K8S集群配置 -->
        <template v-if="isK8sCluster">
          <a-form-item label="Kubernetes配置">
            <div class="k8s-config-input-container">
              <div class="config-actions">
                <a-button 
                  type="dashed" 
                  size="small" 
                  @click="triggerFileInput"
                  :loading="fileLoading"
                >
                  <a-icon type="folder-open" />
                  从文件载入
                </a-button>
                <a-button 
                  type="link" 
                  size="small" 
                  @click="clearConfig"
                  v-if="kubeConfigContent"
                >
                  <a-icon type="clear" />
                  清空
                </a-button>
              </div>
              <a-textarea
                v-decorator="[
                  'kubeConfigContent',
                  { rules: [{ required: true, message: 'Kubernetes配置不能为空!' }] }
                ]"
                placeholder="请输入Kubernetes配置内容，或点击上方按钮从文件载入...
支持标准的 ~/.kube/config 文件（无扩展名）"
                :rows="8"
                @change="onKubeConfigChange"
                class="config-textarea"
              />
              <input
                ref="fileInput"
                type="file"
                @change="handleFileSelect"
                style="display: none"
              />
            </div>
          </a-form-item>
          
          <a-form-item label="命名空间">
            <div class="namespace-selector-container">
              <!-- 选择命名空间模式 -->
              <a-select
                v-if="!isCreatingNewNamespace"
                v-decorator="[
                  'namespace',
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
              >
                <a-select-option key="__create_new__" value="__create_new__">
                  <div class="create-namespace-option">
                    <a-icon type="plus" />
                    创建新的命名空间
                  </div>
                </a-select-option>
                <a-select-option key="divider" disabled class="namespace-divider">
                  <div class="divider-line"></div>
                </a-select-option>
                <a-select-option 
                  v-for="ns in filteredNamespaces" 
                  :key="ns" 
                  :value="ns"
                >
                  {{ ns }}
                </a-select-option>
              </a-select>
              
              <!-- 创建命名空间模式 -->
              <a-input
                v-if="isCreatingNewNamespace"
                v-decorator="[
                  'namespace',
                  { 
                    rules: [{ required: true, message: '请输入命名空间名称!' }] 
                  }
                ]"
                placeholder="请输入新的命名空间名称"
                @change="onNamespaceInputChange"
              >
                <a-icon slot="suffix" type="close-circle" @click="cancelCreateNamespace" style="cursor: pointer; color: #ccc;" />
              </a-input>
              
              <!-- 命名空间操作提示信息 -->
              <div v-if="isCreatingNewNamespace && customNamespaceInput" 
                   :class="isNamespaceExists ? 'namespace-use-tip' : 'namespace-creation-tip'">
                <a-icon 
                  :type="isNamespaceExists ? 'check-circle' : 'info-circle'" 
                  :style="{ 
                    color: isNamespaceExists ? '#52c41a' : '#1890ff', 
                    marginRight: '4px' 
                  }" 
                />
                {{ isNamespaceExists ? '将使用命名空间' : '将创建命名空间' }} 
                <span :style="{ 
                  color: isNamespaceExists ? '#52c41a' : '#1890ff', 
                  fontWeight: '500' 
                }">{{ customNamespaceInput }}</span>
              </div>
              
              <!-- 调试信息，后续可删除 -->
              <div v-if="false" style="margin-top: 8px; padding: 8px; background: #f0f0f0; font-size: 12px;">
                调试：isCreatingNewNamespace: {{ isCreatingNewNamespace }}, customNamespaceInput: '{{ customNamespaceInput }}', selectedNamespace: '{{ selectedNamespace }}'
              </div>
            </div>
          </a-form-item>

        </template>
        
        <!-- 传统集群配置 -->
        <template v-else>
          <a-form-item label :label-col="labelCol1" :wrapper-col="wrapperCol1">
            <a-textarea v-decorator="[
              'hosts',
              {initialValue: steps1.hosts, rules: [{ required: true, message: '主机列表不能为空!' }] },
            ]" placeholder="请输入主机列表..." />
          </a-form-item>
          <a-form-item label="SSH用户名">
            <a-input v-decorator="[
              `sshUser`,
              { initialValue: steps1.sshUser,rules: [{ required: true, message: `SSH用户名不能为空!` }] },
            ]" placeholder="请输入SSH用户名" />
          </a-form-item>
          <a-form-item label="SSH端口">
            <a-input v-decorator="['sshPort', {initialValue: steps1.sshPort, rules: [{ required: true, message: 'SSH端口不能为空!' }] }]" placeholder="请输入SSH端口" />
          </a-form-item>
        </template>
      </a-form>
    </div>
  </div>
</template>
<script>
import clusterAPI from '@/api/httpApi/cluster'

export default {
  inject: ["handleCancel", "currentStepsAdd", "currentStepsSub", "clusterId"],
  props: {
    steps1: Object,
  },
  data() {
    return {
      autosize1: { minRows: 2, maxRows: 4 },
      labelCol: {
        xs: { span: 24 },
        sm: { span: 3 },
      },
      wrapperCol: {
        xs: { span: 24 },
        sm: { span: 21 },
      },
      labelCol1: {
        xs: { span: 24 },
        sm: { span: 0 },
      },
      wrapperCol1: {
        xs: { span: 24 },
        sm: { span: 24 },
      },
      form: this.$form.createForm(this),
      
      // K8S相关数据
      isK8sCluster: false,
      kubeConfigContent: '',
      namespaces: [],
      selectedNamespace: '',
      namespacesLoading: false,
      showCustomNamespace: false,
      clusterInfo: null,
      fileLoading: false,
      isCreatingNewNamespace: false,
      customNamespaceInput: '',
      namespaceSearchText: '',

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
  
  async mounted() {
    await this.loadClusterInfo();
  },
  
  methods: {
    async loadClusterInfo() {
      try {
        const res = await this.$axiosGet(clusterAPI.getClusterDetail + '/' + this.clusterId);
        if (res.code === 200) {
          this.clusterInfo = res.data;
          this.isK8sCluster = res.data.depType === 'Kubernetes';
          
          // 如果是K8S集群且已有配置，设置配置内容
          if (this.isK8sCluster && res.data.kubeConfig) {
            this.kubeConfigContent = res.data.kubeConfig;
            this.form.setFieldsValue({
              kubeConfigContent: res.data.kubeConfig
            });
            // 不自动加载命名空间，等用户点击下拉框时再加载
          }
        }
      } catch (error) {
        console.error('加载集群信息失败:', error);
        this.$message.error('加载集群信息失败');
      }
    },
    
    async loadNamespaces(kubeConfig) {
      
      if (!kubeConfig) {
        console.warn('kubeConfig为空，不调用API');
        return;
      }
      
      this.namespacesLoading = true;
      try {
        const res = await this.$axiosJsonPost(clusterAPI.getKubernetesNamespaces + '/' + this.clusterId, {
          kubeConfig: kubeConfig
        });
        
        if (res.code === 200) {
          this.namespaces = res.data.namespaces || [];
          const defaultNamespace = res.data.defaultNamespace || 'datasophon';
          
          // 检查默认命名空间是否存在于列表中
          if (this.namespaces.includes(defaultNamespace)) {
            // 默认命名空间存在，直接选择
            this.selectedNamespace = defaultNamespace;
            this.isCreatingNewNamespace = false;
            this.customNamespaceInput = '';
            // 使用 $nextTick 确保DOM更新后再设置表单值
            this.$nextTick(() => {
              this.form.setFieldsValue({
                namespace: defaultNamespace
              });
            });
          } else {
            // 默认命名空间不存在，进入创建模式
            this.isCreatingNewNamespace = true;
            this.customNamespaceInput = defaultNamespace;
            this.selectedNamespace = '';
            // 使用 $nextTick 确保DOM更新后再设置表单值
            this.$nextTick(() => {
              this.form.setFieldsValue({
                namespace: defaultNamespace
              });
            });
          }
          
          // 显示集群连接成功信息
          const clusterVersion = res.data.clusterVersion || '未知版本';
          this.$message.success(`连接成功! 集群版本: ${clusterVersion}, 发现 ${this.namespaces.length} 个命名空间`);
        } else {
          this.$message.error('获取命名空间失败: ' + res.msg);
          this.namespaces = [];
          this.selectedNamespace = '';
        }
      } catch (error) {
        console.error('获取命名空间失败:', error);
        this.$message.error('连接Kubernetes集群失败');
        this.namespaces = [];
        this.selectedNamespace = '';
      } finally {
        this.namespacesLoading = false;
      }
    },
    
    triggerFileInput() {
      this.$refs.fileInput.click();
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
    
    handleFileSelect(event) {
      const file = event.target.files[0];
      if (!file) return;
      
      // 检查文件格式 - 支持无扩展名的config文件
      const fileName = file.name.toLowerCase();
      const isValidFormat = fileName.endsWith('.yaml') || 
                           fileName.endsWith('.yml') || 
                           fileName.endsWith('.conf') || 
                           fileName.endsWith('.config') ||
                           fileName === 'config'; // 支持标准的K8S config文件
      
      if (!isValidFormat) {
        this.$message.error('只支持以下格式：标准K8S配置文件(config)、.yaml、.yml、.conf、.config');
        return;
      }
      
      this.fileLoading = true;
      const reader = new FileReader();
      reader.onload = (e) => {
        const content = e.target.result;
        this.kubeConfigContent = content;
        this.form.setFieldsValue({
          kubeConfigContent: content
        });
        // 文件载入时重置命名空间状态
        this.namespaces = [];
        this.selectedNamespace = '';
        this.isCreatingNewNamespace = false;
        this.customNamespaceInput = '';
        this.$nextTick(() => {
          this.form.setFieldsValue({
            namespace: ''
          });
        });
        this.fileLoading = false;
        this.$message.success('配置文件载入成功，请点击命名空间下拉框加载命名空间');
      };
      reader.onerror = () => {
        this.$message.error('文件读取失败');
        this.fileLoading = false;
      };
      reader.readAsText(file);
      
      // 清空input的值，确保同一个文件可以重复选择
      event.target.value = '';
    },
    
    async onKubeConfigChange(e) {
      const content = e.target.value;
      this.kubeConfigContent = content;
      // 配置变化时重置命名空间状态
      if (!content.trim()) {
        this.namespaces = [];
        this.selectedNamespace = '';
        this.isCreatingNewNamespace = false;
        this.customNamespaceInput = '';
        this.$nextTick(() => {
          this.form.setFieldsValue({
            namespace: ''
          });
        });
      }
    },
    
    async onNamespaceDropdownClick() {
      
      // 只有在有配置内容且命名空间列表为空时才调用
      if (this.kubeConfigContent && this.kubeConfigContent.trim() && this.namespaces.length === 0) {
        await this.loadNamespaces(this.kubeConfigContent);
      } else {
        if (!this.kubeConfigContent) {
          console.warn('kubeConfigContent为空');
        }
        if (this.kubeConfigContent && !this.kubeConfigContent.trim()) {
          console.warn('kubeConfigContent只包含空白字符');
        }
      }
    },
    
    onNamespaceSelect(value) {
      if (value === '__create_new__') {
        // 立即设置状态，避免显示 __create_new__
        this.isCreatingNewNamespace = true;
        this.customNamespaceInput = '';
        this.selectedNamespace = '';
        
        // 立即清空表单值，然后在下一个tick再次确保
        this.form.setFieldsValue({
          namespace: ''
        });
        
        this.$nextTick(() => {
          this.form.setFieldsValue({
            namespace: ''
          });
        });
      } else {
        this.isCreatingNewNamespace = false;
        this.customNamespaceInput = '';
        this.selectedNamespace = value;
        // 确保表单值被正确设置
        this.$nextTick(() => {
          this.form.setFieldsValue({
            namespace: value
          });
        });
      }
    },

    cancelCreateNamespace() {
      this.isCreatingNewNamespace = false;
      this.customNamespaceInput = '';
      this.selectedNamespace = '';
      // 使用 $nextTick 确保组件切换完成后再清空表单值
      this.$nextTick(() => {
        this.form.setFieldsValue({
          namespace: ''
        });
      });
    },

    onNamespaceInputChange(e) {
      const value = e.target ? e.target.value : e;
      if (this.isCreatingNewNamespace) {
        this.customNamespaceInput = value || '';
        // v-decorator 会自动处理表单值更新，不需要手动调用 setFieldsValue
      }
    },

    onNamespaceSearch(value) {
      this.namespaceSearchText = value;
    },

    // 下一步时保存K8S配置
    async saveKubernetesConfig() {
      if (!this.isK8sCluster) {
        return true; // 非K8S集群直接返回成功
      }

      try {
        const formValues = await this.form.validateFields();
        
        const configData = {
          clusterId: this.clusterId,
          kubeConfig: formValues.kubeConfigContent,
          namespace: formValues.namespace
        };

        const res = await this.$axiosJsonPost(
          clusterAPI.updateClusterKubeConfig, 
          configData
        );

        if (res.code === 200) {
          this.$message.success('Kubernetes配置保存成功');
          return true;
        } else {
          this.$message.error('保存配置失败: ' + res.msg);
          return false;
        }
      } catch (error) {
        console.error('保存K8S配置失败:', error);
        this.$message.error('保存配置失败');
        return false;
      }
    },
  },
};
</script>

<style scoped>
.namespace-selector-container {
  position: relative;
}

.namespace-creation-tip {
  margin-top: 8px;
  padding: 8px 12px;
  background-color: #e6f7ff;
  border: 1px solid #91d5ff;
  border-radius: 4px;
  font-size: 12px;
  color: #1890ff;
}

.namespace-use-tip {
  margin-top: 8px;
  padding: 8px 12px;
  background-color: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 4px;
  font-size: 12px;
  color: #52c41a;
}

/* 创建新命名空间选项样式 */
.create-namespace-option {
  color: #1890ff !important;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 分割线样式 */
.namespace-divider {
  height: 1px !important;
  padding: 4px 0 !important;
  margin: 0 !important;
  line-height: 1px !important;
  cursor: default !important;
}

.divider-line {
  height: 1px;
  background-color: #f0f0f0;
  margin: 0 -12px;
}

/* 下拉菜单整体样式 */
:deep(.ant-select-dropdown-menu-item) {
  padding: 8px 12px !important;
}

:deep(.ant-select-dropdown-menu-item:hover) {
  background-color: #f5f5f5 !important;
}

/* 创建新命名空间选项的hover效果 */
:deep(.ant-select-dropdown-menu-item:hover .create-namespace-option) {
  background-color: #e6f7ff;
  border-radius: 4px;
  padding: 2px 4px;
  margin: -2px -4px;
}

/* 禁用分割线的hover效果 */
:deep(.namespace-divider:hover) {
  background-color: transparent !important;
  cursor: default !important;
}
</style>
<style lang="less" scoped>
.steps1 {
  .k8s-config-input-container {
    .config-actions {
      margin-bottom: 8px;
      display: flex;
      align-items: center;
      gap: 8px;
    }
    
    .config-textarea {
      font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
      font-size: 13px;
      line-height: 1.4;
      
      &::placeholder {
        color: #bbb;
        font-style: italic;
      }
    }
  }
  
  .namespace-selector {
    .custom-namespace-input {
      margin-top: 10px;
    }
  }
  
  .steps-tips {
    color: #666;
    font-size: 14px;
    
    .steps-tips-icon {
      color: #1890ff;
    }
  }
}
</style>