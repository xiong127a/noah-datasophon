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
                  <div class="form-item">
                    <div class="form-item-label-with-icon">
                      <span>Kubernetes配置</span>
                      <span class="required-icon" :class="{'success-icon': kubeConfigContent && kubeConfigContent.trim()}"></span>
                    </div>
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
                      <a-form-item class="no-label-form-item">
                      <a-textarea
                        v-decorator="[
                          'kubeConfigContent',
                          { rules: [{ required: true, message: 'Kubernetes配置不能为空!' }] }
                        ]"
                        placeholder="请输入Kubernetes配置内容，或点击上方按钮从文件载入...
支持标准的 ~/.kube/config 文件（无扩展名）"
                          :rows="12"
                        @change="onKubeConfigChange"
                          class="apple-textarea custom-form-item"
                          :class="{'has-success': kubeConfigContent && kubeConfigContent.trim()}"
                      />
                      </a-form-item>
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
                  </div>
                  
                  <div class="form-item">
                    <div class="form-item-label-with-icon">
                      <span>命名空间</span>
                      <span class="required-icon" :class="{'success-icon': selectedNamespace || customNamespaceInput}"></span>
                    </div>
                    <div class="namespace-selector-container">
                      <!-- 选择命名空间模式 -->
                      <template v-if="!isCreatingNewNamespace">
                        <div class="namespace-select-wrapper">
                          <!-- 自定义选择器 UI -->
                          <div 
                            class="fake-select" 
                            @click="toggleNamespaceDropdown"
                            :class="{
                              'has-value': selectedNamespace, 
                              'disabled': !kubeConfigContent || !kubeConfigContent.trim(),
                              'empty': !kubeConfigContent || !kubeConfigContent.trim()
                            }"
                          >
                            <div class="select-content">
                              <a-icon 
                                type="deployment-unit" 
                                class="namespace-icon"
                                :class="{'has-value': selectedNamespace}" 
                              />
                              <span v-if="selectedNamespace" class="selected-text">{{ selectedNamespace }}</span>
                              <span v-else class="placeholder-text">
                                <span v-if="!kubeConfigContent" class="empty-config-tip">
                                  <a-icon type="info-circle" />
                                  <span>请先输入Kubernetes配置</span>
                                </span>
                                <span v-else>请选择或搜索命名空间</span>
                              </span>
                            </div>
                            <div class="select-indicators">
                              <a-icon 
                                v-if="selectedNamespace && !namespacesLoading" 
                                type="close-circle" 
                                theme="filled"
                                class="clear-button" 
                                @click.stop="clearNamespaceSelection" 
                                title="清除选择"
                              />
                              <a-icon v-if="namespacesLoading" type="loading" class="loading-icon" />
                              <a-icon v-else type="down" class="select-arrow" :class="{'open': namespaceDropdownOpen}" />
                            </div>
                          </div>
                          
                          <!-- 自定义下拉菜单 -->
                          <transition name="fade">
                            <div class="custom-dropdown-container" v-show="namespaceDropdownOpen">
                              <div class="custom-dropdown-menu">
                                <!-- 创建新命名空间选项 -->
                                <div class="create-new-namespace-option" @click="onCreateNewNamespaceClick">
                                  <a-icon type="plus-circle" class="create-icon" />
                                  <span class="create-text">创建新的命名空间</span>
                                </div>
                                
                                <!-- 搜索框 -->
                                <div class="namespace-search-box" v-if="filteredNamespaces.length > 0">
                                  <a-input 
                                    placeholder="搜索命名空间..." 
                                    @input="onNamespaceSearch" 
                                    size="small"
                                    prefix-icon="search"
                                  />
                                </div>
                                
                                <!-- 分隔线 -->
                                <div class="namespace-divider-line" v-if="filteredNamespaces.length > 0"></div>
                                
                                <!-- 命名空间列表 -->
                                <div class="namespace-list">
                                  <div 
                                    v-for="ns in filteredNamespaces" 
                                    :key="ns"
                                    class="namespace-item"
                                    :class="{'namespace-item-selected': selectedNamespace === ns}"
                                    @click="selectNamespace(ns)"
                                  >
                                    {{ ns }}
                                  </div>
                                  <div v-if="filteredNamespaces.length === 0 && namespaceSearchText" class="no-results">
                                    没有找到匹配的命名空间
                                  </div>
                                </div>
                              </div>
                            </div>
                          </transition>
                          
                          <!-- 隐藏的表单字段 (不使用a-select) -->
                          <a-form-item style="display: none;">
                            <a-input
                              v-decorator="[
                                'namespaceSelect',
                                { 
                                  rules: [{ required: true, message: '请选择命名空间!' }] 
                                }
                              ]"
                              :value="selectedNamespace"
                            />
                          </a-form-item>
                        </div>
                      </template>
                      
                      <!-- 创建命名空间模式 -->
                      <div v-if="isCreatingNewNamespace" class="create-namespace-input-container">
                        <div class="input-with-button">
                        <a-input
                          v-decorator="[
                            'namespaceInput',
                            { 
                              rules: [{ required: true, message: '请输入命名空间名称!' }] 
                            }
                          ]"
                          placeholder="请输入新的命名空间名称"
                          @change="onNamespaceInputChange"
                            class="apple-namespace-input custom-form-item"
                            :class="{'has-success': customNamespaceInput}"
                          size="large"
                          />
                        </div>
                        <a-button type="link" class="back-to-select-btn-outside" @click="cancelCreateNamespace">
                          <a-icon type="arrow-left" /> 返回选择
                        </a-button>
                      </div>
                      
                      <!-- 隐藏字段，用于最终提交的namespace值 -->
                      <a-form-item class="hidden-form-item">
                      <a-input
                        v-decorator="[
                          'namespace',
                          { 
                            rules: [{ required: true, message: '请选择或输入命名空间!' }] 
                          }
                        ]"
                      />
                      </a-form-item>
                      
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
                  </div>
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
                  <div class="form-item">
                    <div class="form-item-label-with-icon">
                      <span>主机列表</span>
                      <span class="required-icon" :class="{'success-icon': form.getFieldValue('hosts')}"></span>
                    </div>
                    <a-form-item class="no-label-form-item">
                    <a-textarea 
                      v-decorator="[
                        'hosts',
                        {initialValue: steps1.hosts, rules: [{ required: true, message: '请输入主机列表' }] },
                      ]" 
                      placeholder="例如：192.168.1.1,192.168.1.2 或 10.3.144.[19-23]" 
                      :autosize="{ minRows: 4, maxRows: 8 }"
                        class="apple-textarea custom-form-item"
                        :class="{'has-success': form.getFieldValue('hosts')}"
                    />
                  </a-form-item>
                  </div>
                </div>
              </div>
              
              <div class="credentials-section">
                <h2 class="section-title">连接凭证</h2>
                <p class="section-description">
                  提供SSH连接信息以便系统能够连接并部署服务
                </p>
                <div class="credentials-grid">
                  <div class="form-item">
                    <div class="form-item-label-with-icon">
                      <span>SSH用户名</span>
                      <span class="required-icon" :class="{'success-icon': form.getFieldValue('sshUser')}"></span>
                    </div>
                    <a-form-item class="no-label-form-item">
                    <a-input 
                      v-decorator="[
                        'sshUser',
                        { initialValue: steps1.sshUser, rules: [{ required: true, message: '请输入SSH用户名' }] },
                      ]" 
                      placeholder="root"
                        class="custom-form-item uniform-input"
                        :class="{'has-success': form.getFieldValue('sshUser')}"
                    />
                  </a-form-item>
                  </div>
                  
                  <div class="form-item">
                    <div class="form-item-label-with-icon">
                      <span>SSH端口</span>
                      <span class="required-icon" :class="{'success-icon': form.getFieldValue('sshPort')}"></span>
                    </div>
                    <a-form-item class="no-label-form-item">
                      <a-input
                      v-decorator="[
                        'sshPort', 
                        {initialValue: steps1.sshPort || 22, rules: [{ required: true, message: 'SSH端口不能为空' }] }
                      ]" 
                      placeholder="22"
                        class="custom-form-item uniform-input"
                        :class="{'has-success': form.getFieldValue('sshPort')}"
                        type="number"
                    />
                  </a-form-item>
                  </div>
                  
                  <div class="form-item">
                    <div class="form-item-label-with-icon">
                      <span>SSH密码</span>
                      <span class="required-icon" :class="{'success-icon': form.getFieldValue('sshPassword')}"></span>
                    </div>
                    <div class="password-input-wrapper">
                      <a-form-item class="no-label-form-item">
                        <a-input
                      v-decorator="[
                        'sshPassword', 
                        {initialValue: steps1.sshPassword, rules: [{ required: true, message: 'SSH密码不能为空' }] }
                      ]" 
                          :type="passwordVisible ? 'text' : 'password'"
                      placeholder="输入密码"
                          class="custom-form-item uniform-input"
                          :class="{'has-success': form.getFieldValue('sshPassword')}"
                    />
                  </a-form-item>
                      <span class="password-eye" @click="togglePasswordVisible">
                        <a-icon :type="passwordVisible ? 'eye' : 'eye-invisible'" />
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </template>
            
            <!-- K8S模式主机信息自动获取说明 -->
            <div v-if="clusterType === 'kubernetes'" class="k8s-host-info-section">
              <h2 class="section-title">主机信息</h2>
              <div class="k8s-info-simple">
                <h3 class="info-title">自动获取节点信息</h3>
                <p class="info-desc">系统将从Kubernetes API自动获取以下信息：</p>
                
                <div class="info-grid">
                  <div class="info-item">
                    <span class="info-label">节点列表</span>
                    <span class="info-value">包含集群中所有可用的worker节点</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">CPU架构</span>
                    <span class="info-value">自动识别每个节点的处理器架构</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">节点状态</span>
                    <span class="info-value">实时获取节点运行状态和资源信息</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">网络配置</span>
                    <span class="info-value">节点的IP地址和主机名映射关系</span>
                  </div>
                </div>
              </div>
            </div>

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
      passwordVisible: false,
      namespaceDropdownOpen: false, // 控制下拉框显示状态
      dropdownStyle: {
        position: 'absolute',
        zIndex: 9999,
        width: '100%',
        boxShadow: '0 3px 15px rgba(0,0,0,0.15)',
        border: '1px solid #d9d9d9',
        borderRadius: '8px',
        background: 'white',
        marginTop: '10px', // 与输入框的间距
        overflow: 'hidden'
      },
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
    
    // 处理Select下拉框定位问题
    this.fixSelectDropdownPosition();
  },
  
  methods: {
    // 修复下拉框定位问题
    fixSelectDropdownPosition() {
      // 确保在DOM更新后执行
      this.$nextTick(() => {
        const namespaceContainer = document.querySelector('.namespace-selector-container');
        if (namespaceContainer) {
          // 创建一个style标签来强制覆盖下拉框样式
          const styleEl = document.createElement('style');
          styleEl.innerHTML = `
            .apple-namespace-dropdown {
              position: absolute !important;
              top: auto !important;
              left: auto !important;
            }
          `;
          document.head.appendChild(styleEl);
          
          // 创建定位下拉框的函数
          const positionDropdown = () => {
            const dropdown = document.querySelector('.apple-namespace-dropdown');
            const select = namespaceContainer.querySelector('.ant-select');
            if (dropdown && select) {
              // 计算下拉框的内容高度
              const dropdownHeight = dropdown.scrollHeight || 250;
              
              const selectRect = select.getBoundingClientRect();
              const viewportHeight = window.innerHeight;
              
              // 计算下拉框位置时，添加足够的间距确保完全在输入框下方
              // 默认向下展开
              let topPosition = (selectRect.bottom + 10);
              
              // 检查如果向下展开会超出视口底部，则尝试向上展开
              if (topPosition + dropdownHeight > viewportHeight && selectRect.top > dropdownHeight) {
                topPosition = (selectRect.top - dropdownHeight - 10);
              }
              
              // 确保下拉框完全在输入框下方
              dropdown.style.width = selectRect.width + 'px';
              dropdown.style.left = selectRect.left + 'px';
              dropdown.style.top = topPosition + 'px';
              
              // 应用额外样式确保它不会与其他元素重叠
              dropdown.style.position = 'fixed';
              dropdown.style.zIndex = '9999';
              dropdown.style.maxHeight = '300px';
              dropdown.style.overflow = 'auto';
              
              // 强制使下拉框与输入框完全分离
              const existingSelectPadding = parseInt(window.getComputedStyle(select).paddingBottom) || 0;
              select.style.marginBottom = '20px'; // 为选择器添加底部边距
            }
          };
          
          // 监听下拉框打开事件
          namespaceContainer.addEventListener('click', () => {
            setTimeout(positionDropdown, 50);
          });
          
          // 监听聚焦事件
          namespaceContainer.addEventListener('focus', () => {
            setTimeout(positionDropdown, 50);
          }, true);
          
          // 也为窗口大小变化添加监听
          window.addEventListener('resize', positionDropdown);
          
          // 修复下拉框展开时的滚动问题
          document.addEventListener('scroll', () => {
            const dropdown = document.querySelector('.apple-namespace-dropdown');
            if (dropdown && dropdown.style.display !== 'none') {
              positionDropdown();
            }
          }, true);
        }
      });
    },
    
    // 控制下拉框位置
    getDropdownContainer(triggerNode) {
      // 返回固定的父级容器，确保下拉框始终附着在这个容器上
      return document.querySelector('.namespace-selector-container') || document.body;
    },
    
    // 获取集群信息
    async getClusterInfo() {
      console.log('开始获取集群信息，clusterId:', this.clusterId);
      const startTime = Date.now();
      const minLoadingTime = 500; // 最少显示2秒加载状态
      
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
      this.kubeConfigContent = content || ''; // 总是更新kubeConfigContent变量
      if (content && content.trim()) {
        this.loadNamespaces();
      } else {
        // 如果内容为空，清空相关数据
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
    
    // 命名空间相关方法
    async loadNamespaces() {
      if (!this.kubeConfigContent || !this.kubeConfigContent.trim()) {
        return;
      }
      
      this.namespacesLoading = true;
      try {
        // 使用$axiosJsonPost确保发送JSON格式
        const res = await this.$axiosPost('/ddh/api/cluster/namespaces', {
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
    
    onNamespaceSearch(e) {
      // 获取输入值
      const value = typeof e === 'object' && e.target ? e.target.value : e;
      this.namespaceSearchText = value;
    },
    
    onNamespaceSelect(value) {
      this.selectedNamespace = value;
      this.isCreatingNewNamespace = false;
      this.customNamespaceInput = '';
      
      // 防止表单字段提前设置的错误
      if (this.form.getFieldDecorator) {
        this.$nextTick(() => {
          // 确保表单字段已经渲染
          setTimeout(() => {
            try {
              // 先检查字段是否存在
              const fields = this.form.getFieldsValue(['namespaceSelect', 'namespace']);
              if ('namespaceSelect' in fields) {
                this.form.setFieldsValue({
                  namespaceSelect: value,
                  namespace: value  // 设置最终的namespace值
                });
              }
            } catch(e) {
              console.error('设置表单字段值失败:', e);
            }
          }, 0);
        });
      }
    },
    
    // 处理下拉框可见性变化
    handleDropdownVisibleChange(visible) {
      this.namespaceDropdownOpen = visible;
    },
    
    // 选择命名空间
    selectNamespace(namespace) {
      // 设置选中的命名空间
      this.selectedNamespace = namespace;
      this.namespaceDropdownOpen = false;
      
      // 设置表单值
      this.$nextTick(() => {
        try {
          this.form.setFieldsValue({
            namespaceSelect: namespace,
            namespace: namespace
          });
        } catch (e) {
          console.error('设置命名空间值失败:', e);
        }
      });
    },
    
    // 清空命名空间选择
    clearNamespaceSelection(e) {
      // 阻止事件冒泡，避免触发下拉框
      if (e) {
        e.stopPropagation();
      }
      
      // 清空选择
      this.selectedNamespace = '';
      
      // 设置表单值
      this.$nextTick(() => {
        try {
          this.form.setFieldsValue({
            namespaceSelect: undefined,
            namespace: ''
          });
        } catch (e) {
          console.error('清空命名空间值失败:', e);
        }
      });
    },
    
    onCreateNewNamespaceClick(e) {
      // 阻止事件冒泡，防止关闭下拉框
      e.stopPropagation();
      
      // 关闭下拉框并切换到创建模式
      this.namespaceDropdownOpen = false;
      this.isCreatingNewNamespace = true;
      this.customNamespaceInput = '';
      
      // 在下一次DOM更新后设置表单值
      this.$nextTick(() => {
        setTimeout(() => {
          this.form.setFieldsValue({
            namespaceSelect: undefined,
            namespaceInput: '',
            namespace: ''
          });
          
          // 聚焦到输入框
          const input = document.querySelector('.apple-namespace-input');
          if (input) {
            input.focus();
          }
        }, 100);
      });
    },
    
    toggleNamespaceDropdown() {
      // 如果选择器被禁用，直接返回
      if (!this.kubeConfigContent || !this.kubeConfigContent.trim()) {
        return;
      }
      
      // 切换下拉框状态
      this.namespaceDropdownOpen = !this.namespaceDropdownOpen;
      
      // 如果是打开状态且没有加载过命名空间，则加载
      if (this.namespaceDropdownOpen && !this.namespaces.length && this.kubeConfigContent) {
        this.loadNamespaces();
      }
      
      // 点击其他区域关闭下拉框
      if (this.namespaceDropdownOpen) {
        this.$nextTick(() => {
          const closeDropdown = (e) => {
            const container = document.querySelector('.namespace-selector-container');
            if (container && !container.contains(e.target)) {
              this.namespaceDropdownOpen = false;
              document.removeEventListener('click', closeDropdown);
            }
          };
          
          // 延迟添加事件，避免立即触发
          setTimeout(() => {
            document.addEventListener('click', closeDropdown);
          }, 100);
        });
      }
    },
    
    onNamespaceDropdownClick() {
      if (!this.namespaces.length && this.kubeConfigContent) {
        this.loadNamespaces();
      }
      
      // 显示下拉框
      this.namespaceDropdownOpen = true;
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
      
      // 防止表单字段提前设置的错误
      this.$nextTick(() => {
        // 确保表单字段已经渲染
        setTimeout(() => {
          try {
            // 先检查字段是否存在
            const fields = this.form.getFieldsValue();
            this.form.resetFields(['namespaceSelect', 'namespaceInput', 'namespace']);
            
            // 打开下拉框
            this.namespaceDropdownOpen = true;
          } catch(e) {
            console.error('重置表单字段值失败:', e);
          }
        }, 0);
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
    },
    togglePasswordVisible() {
      this.passwordVisible = !this.passwordVisible;
    },
  }
};
</script>

<style lang="less" scoped>
// 苹果设计系统颜色 - 增强版
@apple-white: #ffffff;
@apple-black: #1d1d1f;
@apple-gray-light: #f5f5f7;
@apple-gray: #86868b;
@apple-blue: #0071e3;
@apple-blue-hover: #147CE5;
@apple-text-primary: #1d1d1f;
@apple-text-secondary: #86868b;
@apple-gradient: linear-gradient(135deg, #0077ED 0%, #0071e3 50%, #0066CC 100%);

// 设置全局CSS变量供组件内部使用
:root {
  --apple-white: #ffffff;
  --apple-black: #1d1d1f;
  --apple-gray-light: #f5f5f7;
  --apple-gray: #86868b;
  --apple-blue: #0071e3;
  --apple-blue-hover: #147CE5;
  --apple-blue-light: #4ca6ff;
  --apple-blue-light-bg: rgba(0, 113, 227, 0.08);
  --apple-text-primary: #1d1d1f;
  --apple-text-secondary: #86868b;
  --apple-text-tertiary: #86868b;
  --apple-border: #d2d2d7;
  --apple-radius-small: 6px;
  --apple-radius-medium: 8px;
  --apple-radius-large: 12px;
  --apple-shadow-small: 0 2px 6px rgba(0, 0, 0, 0.08);
  --apple-shadow-medium: 0 4px 12px rgba(0, 0, 0, 0.12);
  --apple-shadow-large: 0 8px 20px rgba(0, 0, 0, 0.16);
  --apple-background: #ffffff;
  --apple-success: #52c41a;
  --apple-success-bg: rgba(82, 196, 26, 0.08);
  --apple-error: #ff4d4f;
  --apple-warning: #faad14;
  --apple-link: #0071e3;
}

/* 全局修复下拉菜单样式 */
/deep/ .ant-select-dropdown {
  position: absolute !important;
  z-index: 9999 !important;
}

/deep/ .apple-namespace-dropdown {
  position: absolute !important;
  z-index: 9999 !important;
  margin-top: 5px !important; /* 增加与输入框的分隔距离 */
  box-shadow: 0 3px 15px rgba(0,0,0,0.15) !important; /* 增强阴影效果 */
  border: 1px solid rgba(0,0,0,0.1) !important;
}

/deep/ .ant-select-item-option {
  padding: 4px !important;
  margin: 4px 0 !important;
}

/* 添加全局样式修复所有下拉菜单问题 */
/deep/ .ant-select-dropdown {
  margin-top: 8px !important;
}

/deep/ .create-new-option {
  margin-top: 0 !important;
  margin-bottom: 0 !important;
}

/deep/ .ant-select-dropdown-content {
  position: relative !important;
}

/deep/ .ant-select-dropdown-placement-bottomLeft,
/deep/ .ant-select-dropdown-placement-bottomRight {
  top: auto !important;
}

/* 自定义下拉菜单样式 */
.custom-dropdown-menu {
  padding: 8px;
}

/* 创建新的命名空间选项样式 */
.create-new-namespace-option {
  display: flex;
  align-items: center;
  padding: 14px;
  margin-bottom: 10px;
  border-radius: 8px;
  background: linear-gradient(145deg, rgba(82, 196, 26, 0.05), rgba(82, 196, 26, 0.1));
  border: 1px solid rgba(82, 196, 26, 0.2);
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  
  &:hover {
    background: linear-gradient(145deg, rgba(82, 196, 26, 0.1), rgba(82, 196, 26, 0.15));
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(82, 196, 26, 0.2);
  }
  
  &:active {
    transform: translateY(0);
  }
  
  .create-icon {
    color: #52c41a;
    font-size: 18px;
    margin-right: 10px;
    text-shadow: 0 1px 3px rgba(82, 196, 26, 0.2);
  }
  
  .create-text {
    color: #389e0d;
    font-size: 14px;
    font-weight: 600;
  }
  
  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    right: 0;
    width: 24px;
    height: 24px;
    background: radial-gradient(circle, rgba(82, 196, 26, 0.2), transparent 70%);
    border-radius: 50%;
    opacity: 0.6;
  }
}

/* 分隔线样式 */
.namespace-divider-line {
  height: 1px;
  background: linear-gradient(90deg, rgba(0, 0, 0, 0.02), rgba(0, 0, 0, 0.08), rgba(0, 0, 0, 0.02));
  margin: 10px 0;
  position: relative;
  
  &::before {
    content: '';
    position: absolute;
    left: 50%;
    top: -2px;
    transform: translateX(-50%);
    width: 40px;
    height: 4px;
    background: linear-gradient(90deg, transparent, rgba(82, 196, 26, 0.1), transparent);
    border-radius: 2px;
  }
}

/* 命名空间列表容器样式 */
.namespace-list {
  max-height: 200px;
  overflow-y: auto;
  padding: 4px;
}

/* 命名空间选项样式 */
.namespace-option {
  padding: 10px;
  border-radius: 4px;
  margin: 2px 0;
  
  &:hover {
    background-color: rgba(24, 144, 255, 0.1);
  }
}

/* 修复Select选项样式 */
/deep/ .ant-select-dropdown-menu-item {
  padding: 6px 12px !important;
  border-radius: 4px !important;
  margin: 2px 0 !important;
}

/* 选择选项的样式 */
/deep/ .ant-select-dropdown-menu-item-selected,
/deep/ .ant-select-dropdown-menu-item-active {
  background-color: rgba(24, 144, 255, 0.1) !important;
  font-weight: 600 !important;
}

// 强制修复下拉框位置
/deep/ .ant-select-dropdown {
  position: absolute !important;
  left: auto !important; 
  top: auto !important;
}

// 命名空间下拉框特别样式
/deep/ .apple-namespace-dropdown {
  position: absolute !important;
  left: auto !important;
  top: auto !important;
  width: 100% !important;
  border-radius: 12px !important;
  background: rgba(255, 255, 255, 0.98) !important;
  backdrop-filter: blur(20px) !important;
  overflow: hidden;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.16) !important;
  border: 1px solid #d2d2d7 !important;
  padding: 8px !important;
}

/* 强制覆盖Ant Design下拉菜单样式 */
/deep/ .ant-select-dropdown {
  position: fixed !important;
  /* 这将确保下拉菜单附着在命名空间输入框下方 */
  top: auto !important;
  left: auto !important;
  z-index: 9999 !important; /* 确保下拉菜单在最上层 */
}

// 强制全局下拉框圆角样式
/deep/ .ant-select-selector {
  border-radius: 10px !important;
}

/deep/ .ant-select-single .ant-select-selector {
  border-radius: 10px !important;
}

// 苹果设计系统字体
.apple-font() {
  font-family: "SF Pro Display", "SF Pro Icons", "PingFang SC", "Helvetica Neue", Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
}

// 必填标记样式
.required-icon {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #ff4d4f;
  position: relative;
  margin-left: 8px;
  
  &::after {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%) scale(1);
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background-color: rgba(255, 77, 79, 0.2);
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

// 表单标签带图标布局
.form-item-label-with-icon {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  
  span:first-child {
    .apple-font();
    font-size: 0.95rem;
    font-weight: 600;
    color: @apple-black;
    line-height: 1.4;
  }
}

// 隐藏标签的表单项
.no-label-form-item {
  margin-bottom: 0 !important;
  
  /deep/ .ant-form-item-label {
    display: none !important;
  }
}

// 彻底隐藏的表单项
.hidden-form-item {
  display: none !important;
}

// 脉冲动画
@keyframes pulse {
  0% { transform: translate(-50%, -50%) scale(1); opacity: 0.7; }
  70% { transform: translate(-50%, -50%) scale(2); opacity: 0; }
  100% { transform: translate(-50%, -50%) scale(1); opacity: 0; }
}

@keyframes success-pulse {
  0% { transform: translate(-50%, -50%) scale(1); opacity: 0.7; }
  70% { transform: translate(-50%, -50%) scale(2); opacity: 0; }
  100% { transform: translate(-50%, -50%) scale(1); opacity: 0; }
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
      font-weight: 700;
      line-height: 1.1;
      letter-spacing: -0.022em;
      color: @apple-black;
      margin-bottom: 0.8rem;
      background: linear-gradient(120deg, @apple-blue, #004da3);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      text-shadow: 0 1px 1px rgba(0,0,0,0.05);
    }
    
    .hero-subtitle {
      .apple-font();
      font-size: 1.4rem;
      line-height: 1.4;
      letter-spacing: 0;
      font-weight: 400;
      color: @apple-text-secondary;
      margin: 0;
      max-width: 760px;
      margin: 0 auto;
    }
  }
  
  .form-wrapper {
    max-width: 860px;
    margin: 0 auto;
    padding: 0 1.5rem;
  }
  
  .form-content {
    position: relative;
    animation: slideUp 0.6s ease-out;
    animation-fill-mode: both;
    animation-delay: 0.2s;
  }
  
  .section-title {
    .apple-font();
    font-size: 1.7rem;
    font-weight: 600;
    line-height: 1.2;
    color: @apple-black;
    margin: 0 0 0.8rem 0;
    position: relative;
    
    &::before {
      content: '';
      position: absolute;
      left: -12px;
      top: 0.4rem;
      bottom: 0.4rem;
      width: 4px;
      border-radius: 2px;
      background: @apple-gradient;
    }
  }
  
  .section-description {
    .apple-font();
    font-size: 1rem;
    line-height: 1.5;
    color: @apple-text-secondary;
    margin: 0 0 1.8rem 0;
  }
  
  .form-item {
    margin-bottom: 1.5rem;
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
          transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
          
          &:hover {
            border-color: @apple-blue-hover;
            color: @apple-blue-hover;
            background: rgba(24, 144, 255, 0.06);
            transform: translateY(-1px);
            box-shadow: 0 2px 6px rgba(0, 113, 227, 0.15);
          }
        }
      }
      
      // K8S配置文本域样式
      /deep/ .apple-textarea {
        .apple-font();
        font-family: "SF Mono", SFMono-Regular, ui-monospace, Menlo, Monaco, Consolas, monospace !important;
        resize: vertical !important;
        border: 1px solid #d9d9d9 !important;
        background-color: white !important;
        border-radius: 10px !important;
        padding: 1.2rem !important;
        transition: all 0.3s;
        min-height: 240px !important;
        font-size: 14px !important;
        
        &:hover {
          border-color: @apple-blue !important;
          box-shadow: 0 0 0 2px rgba(0, 113, 227, 0.1) !important;
        }
        
        &:focus {
          outline: none !important;
          border-color: @apple-blue !important;
          box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.2) !important;
        }
        
        &::placeholder {
          color: @apple-gray !important;
          font-weight: 400;
        }
        
        &.has-success {
          border-color: #52c41a !important;
        
        &:focus {
            box-shadow: 0 0 0 3px rgba(82, 196, 26, 0.2) !important;
        }
        }
      }
      
      .namespace-selector-container {
        position: relative !important; /* 确保这是相对定位容器 */
        z-index: 1000; /* 提高z-index确保下拉框在顶层 */
        
        // 命名空间选择器样式
        /deep/ .apple-namespace-select {
          width: 100%;
          
          .ant-select-selector {
            .apple-font();
            border: none !important;
            background-color: @apple-gray-light !important;
            border-radius: 10px !important;
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
        
        // 全局下拉框样式修复 - 使用CSS变量确保一致性
        /deep/ .ant-select {
          .ant-select-selector {
            .apple-font();
            border: 1px solid var(--apple-border) !important;
            background-color: var(--apple-background) !important;
            border-radius: var(--apple-radius-large) !important;
            padding: 12px 16px !important;
            transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1) !important;
            min-height: 44px !important;
            display: flex;
            align-items: center;
            
            &:hover {
              border-color: var(--apple-blue) !important;
              box-shadow: 0 0 0 1px var(--apple-blue) !important;
            }
          }
          
          &.ant-select-focused .ant-select-selector {
            border-color: var(--apple-blue) !important;
            box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.15) !important;
          }
        }
        
        // 下拉菜单样式优化 - 使用CSS变量
        /deep/ .ant-select-dropdown {
          border-radius: var(--apple-radius-large) !important;
          background: rgba(255, 255, 255, 0.98) !important;
          backdrop-filter: blur(20px) !important;
          overflow: hidden;
          box-shadow: var(--apple-shadow-large) !important;
          border: 1px solid var(--apple-border) !important;
          padding: 8px !important;
          animation: apple-dropdown-fade-in 0.25s cubic-bezier(0.4, 0, 0.2, 1) !important;
          
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
                background: var(--apple-blue-light-bg);
                border: 2px solid var(--apple-blue);
                border-radius: var(--apple-radius-medium);
                transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
                
                &:hover {
                  background: var(--apple-blue-light-bg);
                  transform: translateY(-1px);
                  box-shadow: var(--apple-shadow-small);
                }
                
                .create-text {
                  color: var(--apple-blue) !important;
                  font-weight: 600 !important;
                  .apple-font();
                  font-size: 14px;
                }
              }
            
            // 覆盖默认选中样式
              &.ant-select-item-option-selected {
                background-color: transparent !important;
                
                .create-new-content {
                  background: linear-gradient(135deg, var(--apple-blue) 0%, var(--apple-blue-light) 100%) !important;
                  
                  .create-text {
                    color: white !important;
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
              background: linear-gradient(90deg, transparent, var(--apple-blue), transparent);
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
                border-radius: var(--apple-radius-medium);
                margin: 2px 0;
                
                &:hover {
                  background-color: var(--apple-blue-light-bg);
                }
                
                .namespace-icon {
                  color: var(--apple-text-secondary);
                  margin-right: 8px;
                  width: 16px;
                  height: 16px;
                  transition: all 0.2s;
                }
                
                .namespace-name {
                  color: var(--apple-text-primary);
                  .apple-font();
                  font-weight: 400;
                }
              }
          }
          
          // 选中状态
          .ant-select-item-option-selected {
            background-color: var(--apple-blue-light-bg) !important;
            
            .namespace-item {
              .namespace-icon {
                color: var(--apple-blue);
              }
              
              .namespace-name {
                color: var(--apple-blue);
                font-weight: 500;
              }
            }
          }
        }
        
        // 创建命名空间输入框容器
        .create-namespace-input-container {
          position: relative;
          
          .input-with-button {
            position: relative;
            display: flex;
            align-items: center;
            
            .apple-namespace-input {
              flex: 1;
            }
            
            .back-to-select-btn {
              position: absolute;
              right: 10px;
              top: 50%;
              transform: translateY(-50%);
              color: @apple-blue;
              font-size: 14px;
              height: 32px;
              display: flex;
              align-items: center;
              padding: 0 10px;
              z-index: 2;
              
              &:hover {
                background: rgba(0, 113, 227, 0.06);
                border-radius: 4px;
              }
              
              .anticon {
                font-size: 14px;
                margin-right: 4px;
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
            background: var(--apple-blue-light-bg);
            border: 1px solid var(--apple-blue);
            
            .tip-icon.create-icon {
              color: var(--apple-blue);
            }
            
            .tip-text {
              color: var(--apple-text-primary);
              
              .namespace-value {
                color: var(--apple-blue);
              }
            }
          }
          
          &.namespace-exists-tip {
            background: rgba(82, 196, 26, 0.08);
            border: 1px solid rgba(82, 196, 26, 0.2);
            
            .tip-icon.exists-icon {
              color: #52c41a;
            }
            
            .tip-text {
              color: var(--apple-text-primary);
              
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
    // 主机输入框样式
    /deep/ .apple-textarea {
      .apple-font();
      font-size: 15px;
      font-family: "SF Mono", SFMono-Regular, ui-monospace, Menlo, Monaco, Consolas, monospace !important;
      resize: vertical !important;
      border: 1px solid #d9d9d9 !important;
      background-color: white !important;
      border-radius: 10px !important;
      padding: 16px !important;
      transition: all 0.3s;
      min-height: 120px !important;
      
      &:hover {
        border-color: @apple-blue !important;
        box-shadow: 0 0 0 2px rgba(0, 113, 227, 0.1) !important;
      }
      
      &:focus {
        outline: none !important;
        border-color: @apple-blue !important;
        box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.2) !important;
      }
      
      &::placeholder {
        color: @apple-gray !important;
        font-weight: 400;
      }
      
      &.has-success {
        border-color: #52c41a !important;
        
        &:focus {
          box-shadow: 0 0 0 3px rgba(82, 196, 26, 0.2) !important;
        }
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
    }
  }
 
  .k8s-host-info-section {
    margin-bottom: 3rem;
    
    .k8s-info-simple {
      background: #f8f9fa;
      border-radius: 12px;
      padding: 24px;
      
      .info-title {
        font-size: 17px;
        font-weight: 600;
        margin: 0 0 8px 0;
            color: @apple-black;
      }
      
      .info-desc {
        font-size: 14px;
        color: @apple-text-secondary;
        margin-bottom: 24px;
      }
      
      .info-grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 16px;
        
        @media (max-width: 768px) {
          grid-template-columns: 1fr;
        }
        
        .info-item {
          background: white;
          border-radius: 8px;
          padding: 14px;
          display: flex;
          flex-direction: column;
          border: 1px solid #eee;
          
          .info-label {
            font-weight: 600;
            font-size: 14px;
            margin-bottom: 4px;
            color: @apple-black;
          }
          
          .info-value {
            font-size: 13px;
            color: @apple-text-secondary;
           }
         }
      }
    }
  }

  .tips-section {
    display: flex;
    align-items: flex-start;
    background: linear-gradient(145deg, #f6f6f7, #f2f2f4);
    border-radius: 12px;
    padding: 1.2rem 1.5rem;
    border: 1px solid rgba(0, 0, 0, 0.05);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
    
    .tips-icon {
      color: @apple-blue;
      font-size: 1.2rem;
      margin-right: 1rem;
      margin-top: 0.1rem;
           }
           
    .tips-content {
      flex: 1;
      
      p {
             .apple-font();
        font-size: 0.95rem;
        line-height: 1.5;
        color: @apple-text-secondary;
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

/* 统一的输入框样式 */
/deep/ .uniform-input {
  width: 100% !important;
  height: 42px !important;
  line-height: 42px !important;
  border: 1px solid #d9d9d9 !important;
  background-color: white !important;
  border-radius: 10px !important;
  transition: all 0.3s !important;
  font-size: 14px !important;
  padding: 0 !important;
           
           &:hover {
    border-color: @apple-blue !important;
    box-shadow: 0 0 0 2px rgba(0, 113, 227, 0.1) !important;
           }
           
  &:focus, &:focus-within {
    border-color: @apple-blue !important;
             box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.2) !important;
    outline: none !important;
  }
  
  &.has-success {
    border-color: #52c41a !important;
    
    &:focus, &:focus-within {
      box-shadow: 0 0 0 3px rgba(82, 196, 26, 0.2) !important;
    }
  }
}

/* 标准输入框样式覆盖 */
/deep/ .ant-input {
  height: 42px !important;
  line-height: 42px !important;
  padding: 0 12px !important;
  font-size: 14px !important;
  
  &[type="number"] {
    -moz-appearance: textfield; /* 移除火狐浏览器上下按钮 */
  }
  
  &[type="number"]::-webkit-outer-spin-button,
  &[type="number"]::-webkit-inner-spin-button {
    -webkit-appearance: none !important; /* 移除Webkit浏览器上下按钮 */
    margin: 0 !important;
  }
           }
           
/* 密码输入框样式覆盖 */
/deep/ .ant-input-password {
  padding: 0 !important;
  
           .ant-input {
    height: 42px !important;
    line-height: 42px !important;
             border: none !important;
             box-shadow: none !important;
  }
  
  .ant-input-suffix {
    margin-right: 12px !important;
    
    .anticon {
      color: rgba(0, 0, 0, 0.45) !important;
      cursor: pointer !important;
      
      &:hover {
        color: @apple-blue !important;
             }
           }
           
             .ant-input-password-icon {
      display: flex !important;
      align-items: center !important;
      justify-content: center !important;
      height: 16px !important;
      width: 16px !important;
               
               &:hover {
                 color: @apple-blue !important;
               }
             }
           }
         }

/* 命名空间图标和选项样式 */
.namespace-option {
  .namespace-item {
    display: flex;
    align-items: center;
    padding: 10px 12px;
    border-radius: 8px;
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

    .namespace-icon {
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 10px;
      height: 24px;
      width: 24px;
      color: @apple-blue;
      
      svg {
        width: 16px;
        height: 16px;
      }
    }

    .namespace-name {
      color: @apple-text-primary;
      font-size: 14px;
      font-weight: 450;
    }

    &:hover {
      background-color: rgba(0, 113, 227, 0.06);
    }
  }
}

.create-new-option {
  .create-new-content {
    display: flex;
    align-items: center;
    padding: 10px 12px;
    border-radius: 8px;
    background: rgba(82, 196, 26, 0.08);
    border: 1px solid rgba(82, 196, 26, 0.2);
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    
    .create-text {
      color: #389e0d;
      font-size: 14px;
      font-weight: 500;
      display: inline-block;
    }

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 2px 6px rgba(82, 196, 26, 0.15);
      background: rgba(82, 196, 26, 0.12);
  }
  }
}

.namespace-divider {
  height: 1px;
  background: linear-gradient(90deg, rgba(0, 113, 227, 0.1), rgba(0, 113, 227, 0.2), rgba(0, 113, 227, 0.1));
  margin: 6px 0;
        }

/* 全局命名空间下拉框样式 */
:global(.apple-namespace-dropdown) {
  position: absolute;
  width: auto !important;
  border-radius: var(--apple-radius-large) !important;
  background: rgba(255, 255, 255, 0.98) !important;
  backdrop-filter: blur(20px) !important;
  overflow: hidden;
  box-shadow: var(--apple-shadow-large) !important;
  border: 1px solid var(--apple-border) !important;
  padding: 8px !important;
  animation: apple-dropdown-fade-in 0.25s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

/* 修复输入框中的长方条问题 */
.apple-namespace-input {
  background: white !important;
  border: 1px solid #d9d9d9 !important;
  border-radius: 10px !important;
  transition: all 0.3s !important;
  
  &:hover {
    border-color: @apple-blue !important;
  }
  
  &:focus {
    border-color: @apple-blue !important;
    box-shadow: 0 0 0 3px rgba(24, 144, 255, 0.2) !important;
    outline: none !important;
  }
  
  &.has-success {
    border-color: #52c41a !important;
  }
  
  /* 去掉长方条 */
  &::after,
  &::before {
    display: none !important;
  }
}

/* 创建命名空间输入容器样式优化 */
.create-namespace-input-container {
  .input-with-button {
    position: relative;
    display: flex;
    align-items: center;
    
    .apple-namespace-input {
      flex: 1;
  }
    
    .back-to-select-btn {
      position: absolute;
      right: 10px;
      top: 50%;
      transform: translateY(-50%);
      color: @apple-blue;
      font-size: 14px;
      height: 32px;
      display: flex;
      align-items: center;
      padding: 0 10px;
      z-index: 2;
      
      &:hover {
        background: rgba(0, 113, 227, 0.06);
        border-radius: 4px;
      }
      
      .anticon {
        font-size: 14px;
        margin-right: 4px;
      }
    }
  }
}

/* 修复select控件的样式问题 */
.apple-namespace-select {
  width: 100% !important;
  
  /deep/ .ant-select-selection {
    background: white !important;
    border: 1px solid #d9d9d9 !important;
    
    .ant-select-selection__rendered {
      margin: 0 !important;
      padding-left: 12px !important;
  }
    
    /* 移除可能导致长方条问题的选择器 */
    .ant-select-selection__choice {
      background: #e6f7ff !important;
      border-color: #91d5ff !important;
      border-radius: 4px !important;
      margin: 5px 5px 0 0 !important;
      padding: 0 8px 0 8px !important;
    }
    
    .ant-select-search__field {
      margin: 0 !important;
      min-width: 5px !important;
    }
  }
}

// 返回选择按钮外部样式
.back-to-select-btn-outside {
  position: relative;
  margin-top: 8px;
  color: @apple-blue;
  font-size: 14px;
  height: 32px;
  display: flex;
  align-items: center;
  padding: 0 10px;
  
  &:hover {
    background: rgba(0, 113, 227, 0.06);
    border-radius: 4px;
  }
  
  .anticon {
    font-size: 14px;
    margin-right: 4px;
  }
}

/* 命名空间下拉菜单样式 */
.namespace-select-wrapper {
  position: relative;
  width: 100%;
  
  /* 自定义选择器样式 */
  .fake-select {
    width: 100%;
    height: 48px;
    border: 1px solid #d9d9d9;
    border-radius: 10px;
    padding: 0 16px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    background-color: #fff;
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    position: relative;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.03);
    
    &:hover {
      border-color: @apple-blue;
      box-shadow: 0 3px 8px rgba(0, 113, 227, 0.1);
    }
    
    &.has-value {
      border-color: #52c41a;
      box-shadow: 0 2px 6px rgba(82, 196, 26, 0.1);
      
      &:hover {
        border-color: #52c41a;
        box-shadow: 0 3px 8px rgba(82, 196, 26, 0.15);
      }
    }
    
    &.disabled {
      background-color: #f9f9f9;
      cursor: not-allowed;
      color: rgba(0, 0, 0, 0.25);
      border-color: #e8e8e8;
    }
    
    &.empty {
      background: linear-gradient(145deg, #f9f9f9, #f5f5f5);
    }
    
    .select-content {
      display: flex;
      align-items: center;
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      
      .namespace-icon {
        margin-right: 10px;
        font-size: 16px;
        color: #8c8c8c;
        
        &.has-value {
          color: #52c41a;
        }
      }
    }
    
    .select-indicators {
      display: flex;
      align-items: center;
      margin-left: 8px;
    }
    
    .placeholder-text {
      color: rgba(0, 0, 0, 0.35);
      font-size: 14px;
      
      .empty-config-tip {
        display: flex;
        align-items: center;
        color: #8c8c8c;
        background: linear-gradient(145deg, #f9f9fa, #f0f2f5);
        padding: 2px 8px;
        border-radius: 16px;
        
        .anticon {
          font-size: 12px;
          margin-right: 6px;
          color: #1890ff;
        }
        
        span {
          font-size: 13px;
        }
      }
    }
    
    .selected-text {
      color: rgba(0, 0, 0, 0.85);
      font-weight: 500;
      font-size: 14px;
    }
    
    .select-arrow {
      color: rgba(0, 0, 0, 0.35);
      transition: transform 0.3s;
      font-size: 14px;
      
      &.open {
        transform: rotate(180deg);
        color: @apple-blue;
      }
    }
    
    .loading-icon {
      color: @apple-blue;
      font-size: 14px;
    }
    
    .clear-button {
      color: rgba(0, 0, 0, 0.3);
      font-size: 14px;
      margin-right: 8px;
      cursor: pointer;
      transition: all 0.2s;
      
      &:hover {
        color: #ff4d4f;
        transform: scale(1.1);
      }
    }
  }
  
  /* 自定义下拉容器 */
  .custom-dropdown-container {
    position: absolute;
    left: 0;
    width: 100%;
    z-index: 9999;
    margin-top: 8px;
    border-radius: 10px;
    box-shadow: 0 3px 15px rgba(0, 0, 0, 0.15);
    border: 1px solid #d9d9d9;
    background-color: #fff;
    animation: fadeInDown 0.25s ease-out;
    overflow: hidden;
  }
  
  /* 自定义下拉菜单 */
  .custom-dropdown-menu {
    padding: 8px;
  }
  
  /* 命名空间项样式 */
  .namespace-item {
    padding: 12px 16px;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    margin: 3px 0;
    display: flex;
    align-items: center;
    position: relative;
    overflow: hidden;
    
    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 3px;
      height: 0;
      background-color: @apple-blue;
      border-radius: 0 2px 2px 0;
      transition: height 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    }
    
    &:hover {
      background-color: rgba(24, 144, 255, 0.06);
      
      &::before {
        height: 60%;
      }
    }
    
    &.namespace-item-selected {
      background-color: rgba(24, 144, 255, 0.08);
      color: @apple-blue;
      font-weight: 500;
      
      &::before {
        height: 80%;
      }
    }
  }
  
  /* 无结果提示 */
  .no-results {
    padding: 10px 12px;
    text-align: center;
    color: rgba(0, 0, 0, 0.45);
    font-style: italic;
  }
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 淡入淡出动画 */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s, transform 0.3s;
}
.fade-enter, .fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* 搜索框样式 */
.namespace-search-box {
  padding: 10px 0;
  margin: 8px 0;
  
  /deep/ .ant-input {
    border-radius: 8px;
    border: 1px solid #e8e8e8;
    height: 36px;
    padding: 8px 12px 8px 36px;
    transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
    font-size: 14px;
    background-color: #f5f7fa;
    
    &:focus, &:hover {
      border-color: @apple-blue;
      background-color: #fff;
      box-shadow: 0 2px 8px rgba(0, 113, 227, 0.08);
    }
  }
  
  /deep/ .ant-input-prefix {
    left: 12px;
    color: rgba(0, 0, 0, 0.35);
  }
  
  /deep/ .ant-input-affix-wrapper {
    position: relative;
    
    &::before {
      content: '';
      position: absolute;
      right: 12px;
      top: 50%;
      transform: translateY(-50%);
      width: 4px;
      height: 4px;
      background-color: rgba(0, 0, 0, 0.08);
      border-radius: 50%;
      opacity: 0;
      transition: opacity 0.2s ease;
    }
    
    &:focus-within::before {
      opacity: 1;
    }
  }
}

/* 命名空间下拉菜单样式 */
</style>