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
      <a-icon class="steps-tips-icon mgr5" type="exclamation-circle" />提示：使用IP或主机名输入主机列表，按逗号分隔或使用主机域批量添加主机，例如：10.3.144.[19-23]
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
                placeholder="请输入Kubernetes配置内容，或点击上方按钮从文件载入...&#10;支持标准的 ~/.kube/config 文件（无扩展名）"
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
            <a-select
              v-decorator="[
                'namespace',
                { 
                  initialValue: selectedNamespace,
                  rules: [{ required: true, message: '请选择命名空间!' }] 
                }
              ]"
              placeholder="请选择命名空间"
              :loading="namespacesLoading"
              @change="onNamespaceChange"
            >
              <a-select-option value="__create_new__">创建新的命名空间</a-select-option>
              <a-select-option v-for="ns in namespaces" :key="ns" :value="ns">
                {{ ns }}
              </a-select-option>
            </a-select>
          </a-form-item>
          
          <a-form-item v-if="showCustomNamespace" label="命名空间名称">
            <a-input
              v-decorator="[
                'customNamespace',
                { rules: [{ required: true, message: '请输入命名空间名称!' }] }
              ]"
              placeholder="请输入新的命名空间名称"
            />
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
    };
  },
  
  async mounted() {
    await this.loadClusterInfo();
  },
  
  methods: {
    async loadClusterInfo() {
      try {
        const res = await this.$axiosGet(global.API.getClusterDetail + '/' + this.clusterId);
        if (res.code === 200) {
          this.clusterInfo = res.data;
          this.isK8sCluster = res.data.depType === 'Kubernetes';
          
          // 如果是K8S集群且已有配置，加载命名空间
          if (this.isK8sCluster && res.data.kubeConfig) {
            this.kubeConfigContent = res.data.kubeConfig;
            this.form.setFieldsValue({
              kubeConfigContent: res.data.kubeConfig
            });
            await this.loadNamespaces(res.data.kubeConfig);
          }
        }
      } catch (error) {
        console.error('加载集群信息失败:', error);
        this.$message.error('加载集群信息失败');
      }
    },
    
    async loadNamespaces(kubeConfig) {
      if (!kubeConfig) {
        return;
      }
      
      this.namespacesLoading = true;
      try {
        const res = await this.$axiosPost(global.API.getKubernetesNamespaces, kubeConfig, {
          params: { clusterId: this.clusterId }
        });
        
        if (res.code === 200) {
          this.namespaces = res.data.namespaces || [];
          this.selectedNamespace = res.data.defaultNamespace || 'datasophon';
          this.form.setFieldsValue({
            namespace: this.selectedNamespace
          });
          
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
      this.form.setFieldsValue({
        kubeConfigContent: '',
        namespace: ''
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
        this.loadNamespaces(content);
        this.fileLoading = false;
        this.$message.success('配置文件载入成功');
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
      if (content.trim()) {
        await this.loadNamespaces(content);
      } else {
        this.namespaces = [];
        this.selectedNamespace = '';
      }
    },
    
    onNamespaceChange(value) {
      this.selectedNamespace = value;
      this.showCustomNamespace = value === '__create_new__';
      
      if (this.showCustomNamespace) {
        this.$nextTick(() => {
          this.form.setFieldsValue({
            customNamespace: ''
          });
        });
      }
    },
  },
};
</script>
<style lang="less" scoped>
</style>