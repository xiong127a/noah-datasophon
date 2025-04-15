<template>
  <div class="config-download-container">
    <!-- 顶部图标和标题区域 -->
    <div class="page-header">
      <div class="header-icon-wrapper">
        <div class="page-header-icon"></div>
      </div>
      <div class="header-content">
        <h2 class="title">{{ serviceName }} 配置文件</h2>
        <p class="subtitle">管理和下载{{ serviceName }}服务的配置文件</p>
      </div>
    </div>
    
    <!-- 文件统计和操作区域 -->
    <div class="stats-and-actions">
      <div class="stats-cards">
        <div class="stat-card">
          <div class="file-type-icon file-icon-config" style="width: 32px; height: 40px;"></div>
          <div class="stat-content">
            <div class="stat-value">{{ configFiles.length }}</div>
            <div class="stat-label">配置文件总数</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="file-type-icon file-icon-data" style="width: 32px; height: 40px;"></div>
          <div class="stat-content">
            <div class="stat-value">{{ getTotalSize() }}</div>
            <div class="stat-label">总大小</div>
          </div>
        </div>
      </div>
      
      <!-- 打包下载按钮组 -->
      <div class="download-actions">
        <a-dropdown :trigger="['click']" overlayClassName="download-dropdown">
          <a-button 
            type="primary" 
            class="download-all-btn"
            :loading="downloadLoading">
            <a-icon type="download" />
            打包下载
            <a-icon type="down" />
          </a-button>
          <a-menu slot="overlay">
            <a-menu-item v-for="formatInfo in formatsList" :key="formatInfo.format" @click="openDownloadModal(formatInfo.format)">
              <span class="file-menu-icon">
                <div class="file-type-icon file-icon-archive" style="width: 20px; height: 24px;"></div>
              </span>
              {{ formatInfo.format.toUpperCase() }}格式
            </a-menu-item>
          </a-menu>
        </a-dropdown>
      </div>
    </div>
    
    <!-- 搜索栏 -->
    <div class="search-section">
      <a-input-search
        placeholder="搜索配置文件..."
        v-model="searchText"
        class="search-input"
        @change="handleSearch"
        allowClear
      >
        <a-icon slot="prefix" type="search" />
      </a-input-search>
      
      <div class="view-toggle">
        <a-radio-group v-model="viewMode" buttonStyle="solid" size="small">
          <a-radio-button value="list">
            <a-icon type="unordered-list" />
          </a-radio-button>
          <a-radio-button value="grid">
            <a-icon type="appstore" />
          </a-radio-button>
        </a-radio-group>
      </div>
    </div>
    
    <!-- 列表视图 -->
    <div v-if="viewMode === 'list'" class="file-list-section">
      <div class="file-list-header">
        <div class="file-column column-name">文件名称</div>
        <div class="file-column column-desc">描述</div>
        <div class="file-column column-size">大小</div>
        <div class="file-column column-operation">操作</div>
      </div>
      
      <a-spin :spinning="loading" class="loading-spinner">
        <transition-group name="list" tag="div" class="file-list-body">
          <div 
            v-for="file in filteredFiles" 
            :key="file.fileName" 
            class="file-item"
          >
            <div class="file-column column-name">
              <div class="file-icon-wrapper">
                <div class="file-type-icon" :class="getFileIconClass(file.fileName)"></div>
              </div>
              <span class="file-name-text">{{ file.fileName }}</span>
            </div>
            <div class="file-column column-desc">{{ file.description || '无描述' }}</div>
            <div class="file-column column-size">{{ file.fileSize }}</div>
            <div class="file-column column-operation">
              <a-button 
                type="link" 
                class="action-btn preview-btn" 
                @click="previewConfig(file)"
              >
                <a-icon type="eye" />
                预览
              </a-button>
              <a-button 
                type="link" 
                class="action-btn download-btn" 
                @click="downloadSingleConfig(file)"
              >
                <a-icon type="download" />
                下载
              </a-button>
            </div>
          </div>
        </transition-group>
        
        <div v-if="filteredFiles.length === 0 && !loading" class="empty-data">
          <a-empty 
            :description="searchText ? '没有找到匹配的文件' : '暂无配置文件'" 
            :image="emptyImage"
          >
            <template v-if="searchText" #description>
              <div class="empty-search-text">
                没有找到匹配 <span class="search-term">"{{ searchText }}"</span> 的文件
              </div>
            </template>
          </a-empty>
        </div>
      </a-spin>
    </div>
    
    <!-- 网格视图 -->
    <div v-else class="file-grid-section">
      <a-spin :spinning="loading" class="loading-spinner">
        <transition-group name="grid" tag="div" class="file-grid">
          <div
            v-for="file in filteredFiles"
            :key="file.fileName"
            class="grid-item"
            @click="previewConfig(file)"
          >
            <div class="grid-item-icon">
              <div class="file-type-icon" :class="getFileIconClass(file.fileName)"></div>
            </div>
            <div class="grid-item-info">
              <div class="grid-item-name" :title="file.fileName">{{ file.fileName }}</div>
              <div class="grid-item-size">{{ file.fileSize }}</div>
            </div>
            <div class="grid-item-actions">
              <a-button 
                type="link" 
                class="grid-action-btn" 
                @click.stop="downloadSingleConfig(file)"
              >
                <a-icon type="download" />
              </a-button>
            </div>
          </div>
        </transition-group>
        
        <div v-if="filteredFiles.length === 0 && !loading" class="empty-data">
          <a-empty 
            :description="searchText ? '没有找到匹配的文件' : '暂无配置文件'" 
            :image="emptyImage"
          />
        </div>
      </a-spin>
    </div>

    <!-- 预览模态窗 -->
    <a-modal
      v-model="previewVisible"
      :title="null"
      width="70%"
      :footer="null"
      :closable="false"
      :maskClosable="true"
      class="preview-modal"
      :destroyOnClose="true"
    >
      <div class="modal-header">
        <div class="modal-file-info">
          <div class="file-type-icon" :class="getFileIconClass(currentPreviewFile)" style="margin-right: 12px;"></div>
          <span class="modal-file-name">{{ currentPreviewFile }}</span>
        </div>
        <div class="modal-actions">
          <a-button 
            type="link" 
            class="modal-action-btn" 
            @click="copyConfigContent"
          >
            <a-icon type="copy" />
            复制内容
          </a-button>
          <a-button 
            type="link" 
            class="modal-action-btn" 
            @click="downloadCurrentFile"
          >
            <a-icon type="download" />
            下载文件
          </a-button>
          <a-button 
            type="link" 
            class="modal-close-btn" 
            @click="previewVisible = false"
          >
            <a-icon type="close" />
          </a-button>
        </div>
      </div>
      
      <a-spin :spinning="previewLoading">
        <div class="syntax-header">
          <div class="syntax-info">{{ getFileType(currentPreviewFile) }}</div>
          <div class="syntax-actions">
            <a-radio-group v-model="previewTheme" buttonStyle="solid" size="small">
              <a-radio-button value="dark">深色</a-radio-button>
              <a-radio-button value="light">浅色</a-radio-button>
            </a-radio-group>
          </div>
        </div>
        <codemirror
          v-model="previewContent"
          :options="cmOptions"
          class="code-mirror"
          :style="{ height: '800px' }"
        />
      </a-spin>
    </a-modal>

    <!-- 下载配置模态窗 -->
    <a-modal
      v-model="downloadModalVisible"
      title="下载配置文件"
      width="500px"
      @ok="downloadAllConfigs"
      okText="下载"
      cancelText="取消"
      :okButtonProps="{ loading: downloadLoading }"
      :destroyOnClose="true"
      class="download-modal"
    >
      <div class="download-options">
        <div class="option-row">
          <div class="option-label">压缩格式：</div>
          <div class="option-content">
            <a-tag class="format-tag">{{ selectedFormat.toUpperCase() }}</a-tag>
            <span class="format-desc">
              {{ getFormatDescription(selectedFormat) }}
            </span>
          </div>
        </div>
        
        <a-divider />
        
        <div class="option-row">
          <div class="option-label">密码保护：</div>
          <div class="option-content">
            <a-switch v-model="usePassword" @change="handlePasswordChange" :disabled="!supportPassword" />
            <span class="option-desc">{{ usePassword ? '开启' : (supportPassword ? '关闭' : '不支持') }}</span>
          </div>
        </div>
        
        <div v-if="usePassword" class="password-section">
          <a-form-model layout="vertical" :model="passwordForm" autocomplete="off">
            <a-form-model-item label="设置密码">
              <a-input-password 
                v-model="passwordForm.password"
                placeholder="请输入密码"
                autocomplete="new-password"
                :visibilityToggle="true"
                :name="'pwd_' + Date.now()"
                readonly
                @focus="removeReadonly"
              />
            </a-form-model-item>
            <a-form-model-item label="确认密码">
              <a-input-password 
                v-model="passwordForm.confirmPassword"
                placeholder="请再次输入密码"
                autocomplete="new-password"
                :visibilityToggle="true"
                :name="'confirm_pwd_' + Date.now()"
                readonly
                @focus="removeReadonly"
              />
              <div v-if="passwordError" class="password-error" role="alert">
                <a-icon type="warning" />
                {{ passwordError }}
              </div>
            </a-form-model-item>
          </a-form-model>
          
          <div class="password-tips" role="note">
            <a-icon type="info-circle" />
            密码保护提示：
            <ul>
              <li>压缩包将使用密码进行加密保护</li>
              <li>请妥善保管密码，密码丢失将无法解压文件</li>
              <li>不同压缩格式的加密强度可能不同</li>
            </ul>
          </div>
        </div>

        <!-- 打包进度显示 -->
        <div v-if="isCompressing" class="compress-progress" role="progressbar" :aria-valuenow="compressProgress" aria-valuemin="0" aria-valuemax="100">
          <div class="progress-header">
            <span class="progress-title">正在打包文件...</span>
            <span class="progress-percent">{{ compressProgress }}%</span>
          </div>
          <a-progress 
            :percent="compressProgress" 
            :showInfo="false"
            strokeColor="#007AFF"
            class="progress-bar"
          />
          <div class="progress-tips">
            <a-icon type="loading" />
            正在压缩文件，请稍候...
          </div>
        </div>
      </div>
    </a-modal>

    <!-- 进度模态窗 -->
    <a-modal
      v-model="progressModalVisible"
      :title="null"
      width="440px"
      :footer="null"
      :closable="false"
      :maskClosable="false"
      :destroyOnClose="true"
      class="progress-modal"
      :bodyStyle="{ padding: 0 }"
    >
      <div class="progress-content">
        <div class="progress-header">
          <div class="progress-icon">
            <a-icon type="loading" spin class="spinning-icon" />
          </div>
          <div class="progress-info">
            <h3>{{ progressTitle }}</h3>
            <p>{{ progressStatusText }}</p>
          </div>
        </div>
        
        <div class="progress-bar-container">
          <div class="progress-track">
            <div class="progress-fill" :style="{ transform: `translateX(${compressProgress - 100}%)` }">
              <div class="progress-glow"></div>
            </div>
          </div>
          <div class="progress-number">{{ compressProgress }}%</div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script>
import Empty from 'ant-design-vue/lib/empty';
import { codemirror } from 'vue-codemirror';
import 'codemirror/lib/codemirror.css';
import 'codemirror/theme/dracula.css';
import 'codemirror/theme/eclipse.css';
import 'codemirror/mode/xml/xml.js';
import 'codemirror/mode/javascript/javascript.js';
import 'codemirror/mode/yaml/yaml.js';
import 'codemirror/mode/properties/properties.js';
import 'codemirror/mode/shell/shell.js';
import 'codemirror/addon/edit/matchbrackets.js';
import 'codemirror/addon/edit/closebrackets.js';
import 'codemirror/addon/fold/foldcode.js';
import 'codemirror/addon/fold/foldgutter.js';
import 'codemirror/addon/fold/brace-fold.js';
import 'codemirror/addon/fold/xml-fold.js';
import 'codemirror/addon/fold/foldgutter.css';

export default {
  name: 'ConfigDownload',
  components: {
    codemirror
  },
  props: {
    serviceId: {
      type: [Number, String],
      required: true
    },
    serviceName: {
      type: String,
      required: true,
      default: '未知服务'
    }
  },
  data() {
    return {
      configFiles: [],
      loading: false,
      downloadLoading: false,
      previewVisible: false,
      previewLoading: false,
      previewContent: '',
      currentPreviewFile: '',
      searchText: '',
      viewMode: 'list',
      previewTheme: 'dark',
      emptyImage: Empty.PRESENTED_IMAGE_SIMPLE,
      downloadModalVisible: false,
      selectedFormat: 'zip',
      usePassword: false,
      passwordForm: {
        password: '',
        confirmPassword: ''
      },
      passwordError: '',
      isCompressing: false,
      compressProgress: 0,
      progressTimer: null,
      progressModalVisible: false,
      progressStartTime: null,
      progressTitle: '正在打包配置文件',
      progressStatusText: '正在准备文件...',
      minDisplayTime: 5000, // 最小显示时间（毫秒）
      simulatedProgress: 0, // 模拟进度
      realProgress: 0, // 实际进度
      simulateTimer: null, // 模拟进度的定时器
      columns: [
        {
          title: '配置文件名称',
          dataIndex: 'fileName',
          key: 'fileName',
          width: '30%'
        },
        {
          title: '描述',
          dataIndex: 'description',
          key: 'description',
          width: '40%'
        },
        {
          title: '大小',
          dataIndex: 'fileSize',
          key: 'fileSize',
          width: '15%'
        },
        {
          title: '操作',
          key: 'operation',
          dataIndex: 'operation',
          width: '15%',
          scopedSlots: { customRender: 'operation' }
        }
      ],
      cmOptions: {
        mode: 'text/x-yaml',
        theme: 'dracula',
        lineNumbers: true,
        autoCloseBrackets: true,
        matchBrackets: true,
        foldGutter: true,
        gutters: ['CodeMirror-linenumbers', 'CodeMirror-foldgutter'],
        foldOptions: {
          widget: 'triangle',
          marker: 'triangle',
          open: 'triangle',
          closed: 'triangle'
        }
      },
      formatsList: [],
    };
  },
  computed: {
    filteredFiles() {
      if (!this.searchText) {
        return this.configFiles;
      }
      
      const searchLower = this.searchText.toLowerCase();
      return this.configFiles.filter(file => 
        file.fileName.toLowerCase().includes(searchLower) || 
        (file.description && file.description.toLowerCase().includes(searchLower))
      );
    },
    // 判断当前选择的格式是否支持密码
    supportPassword() {
      const formatInfo = this.formatsList.find(item => item.format === this.selectedFormat);
      return formatInfo && (formatInfo.supportPassword === '支持' || formatInfo.supportPassword === '需安装zip4j库');
    }
  },
  watch: {
    previewTheme(val) {
      this.cmOptions.theme = val === 'dark' ? 'dracula' : 'eclipse';
    },
    currentPreviewFile(val) {
      if (val) {
        this.updateCodeMirrorMode(val);
      }
    },
    compressProgress(val) {
      if (val < 30) {
        this.progressStatusText = '正在准备文件...';
      } else if (val < 60) {
        this.progressStatusText = '正在压缩文件...';
      } else if (val < 90) {
        this.progressStatusText = '正在打包...';
      } else {
        this.progressStatusText = '即将完成...';
      }
    },
    // 监听格式变化，如果切换到不支持密码的格式，自动关闭密码保护
    selectedFormat(newFormat) {
      const formatInfo = this.formatsList.find(item => item.format === newFormat);
      if (formatInfo && formatInfo.supportPassword === '不支持' && this.usePassword) {
        this.usePassword = false;
        this.passwordForm = {
          password: '',
          confirmPassword: ''
        };
        this.passwordError = '';
      }
    }
  },
  mounted() {
    this.fetchConfigFiles();
    this.fetchSupportedCompressFormats();
  },
  methods: {
    // 获取配置文件列表
    async fetchConfigFiles() {
      if (!this.serviceId) {
        this.$message.error('服务ID不能为空');
        return;
      }
      
      this.loading = true;
      try {
        const apiUrl = global.API.getServiceConfigFiles;
        
        const params = { serviceInstanceId: this.serviceId };
        
        if (!global.API || !global.API.getServiceConfigFiles) {
          this.$message.error('系统配置错误，无法获取服务信息');
          return;
        }
        
        const res = await this.$axiosJsonPost(apiUrl, params);
        
        if (res.code === 200) {
          this.configFiles = res.data || [];
        } else {
          this.$message.error(res.msg || '获取配置文件列表失败');
        }
      } catch (error) {
        this.$message.error(`获取配置文件列表失败: ${error ? error.message || '未知错误' : '未知错误'}`);
      } finally {
        this.loading = false;
      }
    },

    // 获取支持的压缩格式列表
    async fetchSupportedCompressFormats() {
      try {
        if (!global.API || !global.API.getSupportedCompressFormats) {
          this.$message.error('系统配置错误，无法获取支持的压缩格式');
          // 使用默认值
          this.formatsList = [
            { format: 'zip', description: '兼容性最佳，几乎所有系统都支持', supportPassword: '需安装zip4j库' },
            { format: 'tar.gz', description: 'Linux/Unix系统常用格式，压缩率高', supportPassword: '不支持' },
            { format: '7z', description: '高压缩率，标准7z格式（不支持密码保护）', supportPassword: '不支持' }
          ];
          return;
        }
        
        const res = await this.$axiosGet(global.API.getSupportedCompressFormats);
        
        if (res.code === 200) {
          this.formatsList = res.data || [];
          console.log('获取到压缩格式列表：', this.formatsList);
        } else {
          this.$message.error(res.msg || '获取支持的压缩格式失败');
          // 使用默认值
          this.formatsList = [
            { format: 'zip', description: '兼容性最佳，几乎所有系统都支持', supportPassword: '需安装zip4j库' },
            { format: 'tar.gz', description: 'Linux/Unix系统常用格式，压缩率高', supportPassword: '不支持' },
            { format: '7z', description: '高压缩率，标准7z格式（不支持密码保护）', supportPassword: '不支持' }
          ];
        }
      } catch (error) {
        this.$message.error(`获取支持的压缩格式失败: ${error ? error.message || '未知错误' : '未知错误'}`);
        // 使用默认值
        this.formatsList = [
          { format: 'zip', description: '兼容性最佳，几乎所有系统都支持', supportPassword: '需安装zip4j库' },
          { format: 'tar.gz', description: 'Linux/Unix系统常用格式，压缩率高', supportPassword: '不支持' },
          { format: '7z', description: '高压缩率，标准7z格式（不支持密码保护）', supportPassword: '不支持' }
        ];
      }
    },

    // 打开下载模态窗
    openDownloadModal(format) {
      this.selectedFormat = format;
      this.usePassword = false;
      this.passwordForm = {
        password: '',
        confirmPassword: ''
      };
      this.passwordError = '';
      this.downloadModalVisible = true;
    },
    
    // 处理密码开关变化
    handlePasswordChange(checked) {
      if (!checked) {
        this.passwordForm = {
          password: '',
          confirmPassword: ''
        };
        this.passwordError = '';
      }
    },
    
    // 获取压缩格式的描述
    getFormatDescription(format) {
      // 从API返回的格式列表中查找对应格式的描述
      const formatInfo = this.formatsList.find(item => item.format === format);
      if (formatInfo) {
        return formatInfo.description;
      }
      
      // 如果找不到，则使用默认描述
      const descriptions = {
        'zip': '兼容性最佳，几乎所有系统都支持',
        'tar.gz': 'Linux/Unix系统常用格式，压缩率高',
        '7z': '高压缩率，支持多种加密方式',
        'tar': '无压缩的归档格式，用于打包多个文件',
        'tar.xz': 'Linux系统常用高压缩率格式',
        'gz': 'GZIP格式，单文件压缩，常用于Linux系统',
        'bz2': 'BZIP2格式，高压缩率，通常用于Linux系统'
      };
      return descriptions[format] || '压缩文件格式';
    },

    // 下载单个配置文件
    downloadSingleConfig(record) {
      const { fileName } = record;
      
      if (!global.API || !global.API.downloadServiceConfigFile) {
        this.$message.error('系统配置错误，无法下载文件');
        return;
      }
      
      try {
        const downloadUrl = `${window.location.origin}${global.API.downloadServiceConfigFile}?serviceInstanceId=${this.serviceId}&fileName=${encodeURIComponent(fileName)}`;
        
        const link = document.createElement('a');
        link.href = downloadUrl;
        link.setAttribute('download', fileName);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        
        this.$message.success(`正在下载 ${fileName}`);
      } catch (error) {
        this.$message.error('下载文件失败');
      }
    },
    
    // 下载当前预览的文件
    downloadCurrentFile() {
      if (!this.currentPreviewFile) return;
      
      const file = this.configFiles.find(f => f.fileName === this.currentPreviewFile);
      if (file) {
        this.downloadSingleConfig(file);
      }
    },

    // 开始监控打包进度
    startProgressMonitor() {
      this.isCompressing = true;
      this.compressProgress = 0;
      this.simulatedProgress = 0;
      this.realProgress = 0;
      this.progressModalVisible = true;
      this.progressStartTime = Date.now();
      
      // 启动模拟进度
      this.startSimulatedProgress();
      
      // 实际调用后端API获取进度
      this.progressTimer = setInterval(async () => {
        try {
          if (!global.API || !global.API.getCompressProgress) {
            this.$message.error('系统配置错误，无法获取压缩进度');
            return;
          }
          
          const res = await this.$axiosGet(
            `${global.API.getCompressProgress}?serviceInstanceId=${this.serviceId}`
          );
          
          if (res.code === 200 && res.data !== null) {
            this.realProgress = res.data;
            console.log('实际压缩进度:', this.realProgress);
            
            // 如果实际进度已完成
            if (this.realProgress >= 100) {
              // 快速完成剩余的模拟进度
              this.completeProgress();
            }
          } else {
            this.$message.error(res.msg || '获取压缩进度失败');
          }
        } catch (error) {
          console.error('获取打包进度失败:', error);
          this.$message.error('获取压缩进度失败');
        }
      }, 1000);
    },

    // 启动模拟进度
    startSimulatedProgress() {
      const totalSteps = 200; // 增加总步数，让进度条走得更慢
      const baseInterval = 75; // 增加间隔时间
      let step = 0;
      
      this.simulateTimer = setInterval(() => {
        if (this.simulatedProgress >= 100) {
          clearInterval(this.simulateTimer);
          return;
        }
        
        // 计算下一步进度
        step++;
        // 使用缓动函数使进度变化更自然
        const progress = this.easeProgress(step / totalSteps);
        // 限制模拟进度不超过实际进度
        this.simulatedProgress = Math.min(Math.floor(progress * 95), this.realProgress);
        // 更新显示的进度
        this.compressProgress = this.simulatedProgress;
        
        // 更新状态文本
        this.updateProgressStatus(this.simulatedProgress);
      }, baseInterval);
    },

    // 缓动函数，使进度变化更自然
    easeProgress(x) {
      return x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2;
    },

    // 快速完成剩余进度
    completeProgress() {
      clearInterval(this.simulateTimer);
      
      const remainingProgress = 100 - this.simulatedProgress;
      const steps = 10;
      const stepSize = remainingProgress / steps;
      let currentStep = 0;
      
      const finishTimer = setInterval(() => {
        currentStep++;
        this.simulatedProgress = Math.min(
          this.simulatedProgress + stepSize,
          100
        );
        this.compressProgress = Math.floor(this.simulatedProgress);
        
        if (currentStep >= steps) {
          clearInterval(finishTimer);
          // 确保最小显示时间
          const elapsedTime = Date.now() - this.progressStartTime;
          if (elapsedTime >= this.minDisplayTime) {
            this.stopProgressMonitor();
          } else {
            setTimeout(() => {
              this.stopProgressMonitor();
            }, this.minDisplayTime - elapsedTime);
          }
        }
      }, 100);
    },

    // 更新进度状态文本
    updateProgressStatus(progress) {
      if (progress < 20) {
        this.progressStatusText = '正在准备文件...';
      } else if (progress < 40) {
        this.progressStatusText = '正在分析文件结构...';
      } else if (progress < 60) {
        this.progressStatusText = '正在压缩文件...';
      } else if (progress < 80) {
        this.progressStatusText = '正在优化压缩...';
      } else if (progress < 95) {
        this.progressStatusText = '正在完成打包...';
      } else {
        this.progressStatusText = '即将完成...';
      }
    },

    // 停止进度监控
    stopProgressMonitor() {
      if (this.progressTimer) {
        clearInterval(this.progressTimer);
        this.progressTimer = null;
      }
      if (this.simulateTimer) {
        clearInterval(this.simulateTimer);
        this.simulateTimer = null;
      }
      
      this.isCompressing = false;
      this.progressModalVisible = false;
      this.compressProgress = 0;
      this.simulatedProgress = 0;
      this.realProgress = 0;
      this.progressStartTime = null;
    },

    // 打包下载所有配置文件
    async downloadAllConfigs() {
      // 验证密码
      if (this.usePassword) {
        if (!this.passwordForm.password) {
          this.passwordError = '请输入密码';
          return;
        }
        if (this.passwordForm.password !== this.passwordForm.confirmPassword) {
          this.passwordError = '两次输入的密码不一致';
          return;
        }
        if (this.passwordForm.password.length < 4) {
          this.passwordError = '密码长度不能少于4位';
          return;
        }
      }
      
      // 直接执行下载，不再弹出提示
      this.executeDownload();
    },
    
    // 执行下载操作
    executeDownload() {
      this.downloadLoading = true;
      
      try {
        if (!global.API || !global.API.downloadAllServiceConfigFiles) {
          this.$message.error('系统配置错误，无法下载文件');
          return;
        }
        
        // 先启动进度监控
        this.startProgressMonitor();
        
        // 构建下载URL，添加格式参数和可选的密码参数
        let downloadUrl = `${window.location.origin}${global.API.downloadAllServiceConfigFiles}?serviceInstanceId=${this.serviceId}&format=${this.selectedFormat}`;
        
        // 如果启用了密码保护，添加密码参数
        if (this.usePassword && this.passwordForm.password) {
          downloadUrl += `&password=${encodeURIComponent(this.passwordForm.password)}`;
        }
        
        const link = document.createElement('a');
        link.href = downloadUrl;
        
        // 根据不同格式设置不同的文件扩展名
        const fileExtension = this.selectedFormat === 'tar.gz' || this.selectedFormat === 'tar.xz' ? 
                             '.' + this.selectedFormat : `.${this.selectedFormat}`;
        link.setAttribute('download', `${this.serviceName}_configs${fileExtension}`);
        
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        
        this.$message.success(`正在下载 ${this.selectedFormat.toUpperCase()} 格式配置文件`);
        this.downloadModalVisible = false;
      } catch (error) {
        this.$message.error('下载配置文件失败');
        this.stopProgressMonitor();
      } finally {
        this.downloadLoading = false;
      }
    },

    // 预览配置文件
    async previewConfig(record) {
      this.previewVisible = true;
      this.previewLoading = true;
      this.previewContent = '';
      this.currentPreviewFile = record.fileName;

      try {
        if (!global.API || !global.API.previewServiceConfigFile) {
          this.previewContent = '系统配置错误，无法获取文件内容';
          this.$message.error('系统配置错误，无法获取文件内容');
          return;
        }
        
        const params = {
          serviceInstanceId: this.serviceId,
          fileName: record.fileName
        };
        
        const res = await this.$axiosJsonPost(global.API.previewServiceConfigFile, params);
        
        if (res.code === 200) {
          // 处理空文件的情况
          if (!res.data || res.data.trim() === '') {
            this.previewContent = '// 文件内容为空';
            this.$message.warning('当前文件内容为空');
          } else {
            this.previewContent = res.data;
          }
        } else {
          this.previewContent = '获取文件内容失败';
          // 只有在有具体错误信息时才显示
          if (res.msg && res.msg !== 'null') {
            this.$message.error(res.msg);
          } else {
            this.$message.error('获取文件内容失败，请稍后重试');
          }
        }
      } catch (error) {
        this.previewContent = '获取文件内容失败';
        // 只有在有具体错误信息时才显示
        if (error && typeof error === 'object' && error.message && error.message !== 'null') {
          this.$message.error('获取文件内容失败：' + error.message);
        } else {
          this.$message.error('获取文件内容失败，请稍后重试');
        }
      } finally {
        this.previewLoading = false;
      }
    },
    
    // 复制配置内容
    copyConfigContent() {
      if (!this.previewContent) {
        this.$message.warning('没有可复制的内容');
        return;
      }
      
      const textarea = document.createElement('textarea');
      textarea.value = this.previewContent;
      document.body.appendChild(textarea);
      textarea.select();
      document.execCommand('copy');
      document.body.removeChild(textarea);
      
      this.$message.success('内容已复制到剪贴板');
    },
    
    // 处理搜索
    handleSearch() {
      // 搜索逻辑已通过计算属性实现
    },
    
    // 获取文件图标
    getFileIcon(fileName) {
      if (!fileName) return 'file';
      
      const extension = fileName.split('.').pop().toLowerCase();
      
      const iconMap = {
        // 配置文件类型
        xml: 'tags',  // 使用tags图标，因为XML是基于标签的标记语言
        options: 'setting',
        json: 'file-json',
        yaml: 'file-markdown',
        yml: 'file-markdown',
        properties: 'setting',
        conf: 'setting',
        cfg: 'setting',
        ini: 'setting',
        toml: 'setting',
        
        // 脚本文件类型
        sh: 'code',
        bash: 'code',
        zsh: 'code',
        bat: 'code',
        cmd: 'code',
        ps1: 'code',
        
        // 日志文件类型
        log: 'file-text',
        out: 'file-text',
        err: 'file-text',
        trace: 'file-text',
        
        // 数据文件类型
        csv: 'file-excel',
        tsv: 'file-excel',
        xlsx: 'file-excel',
        xls: 'file-excel',
        
        // 文本文件类型
        txt: 'file-text',
        md: 'file-markdown',
        markdown: 'file-markdown',
        rst: 'file-text',
        
        // 压缩文件类型
        zip: 'file-zip',
        tar: 'file-zip',
        gz: 'file-zip',
        bz2: 'file-zip',
        rar: 'file-zip',
        '7z': 'file-zip',
        
        // 图片文件类型
        png: 'file-image',
        jpg: 'file-image',
        jpeg: 'file-image',
        gif: 'file-image',
        svg: 'file-image',
        ico: 'file-image',
        
        // 其他常见文件类型
        pdf: 'file-pdf',
        doc: 'file-word',
        docx: 'file-word',
        ppt: 'file-ppt',
        pptx: 'file-ppt',
        html: 'file-html',
        htm: 'file-html',
        css: 'file-css',
        js: 'file-javascript',
        ts: 'file-typescript',
        java: 'file-java',
        py: 'file-python',
        go: 'file-go',
        c: 'file-c',
        cpp: 'file-cpp',
        h: 'file-c',
        hpp: 'file-cpp',
        sql: 'database',
        db: 'database',
        sqlite: 'database',
        mdb: 'database',
        accdb: 'database'
      };
      
      // 处理没有扩展名的特殊文件名
      if (!extension || extension === fileName) {
        // 一些常见的无扩展名配置文件
        const specialFiles = {
          'options': 'setting',
          'config': 'setting',
          'dockerfile': 'code',
          'makefile': 'code',
          'readme': 'file-markdown'
        };
        
        const lowerFileName = fileName.toLowerCase();
        return specialFiles[lowerFileName] || 'file';
      }
      
      return iconMap[extension] || 'file';
    },
    
    // 获取文件类型名称
    getFileType(fileName) {
      if (!fileName) return '文本文件';
      
      const extension = fileName.split('.').pop().toLowerCase();
      
      const typeMap = {
        xml: 'XML 配置文件',
        json: 'JSON 配置文件',
        properties: 'Properties 配置文件',
        yaml: 'YAML 配置文件',
        yml: 'YAML 配置文件',
        sh: 'Shell 脚本',
        txt: '文本文件',
        conf: '配置文件',
        cfg: '配置文件',
        ini: 'INI 配置文件',
        log: '日志文件'
      };
      
      return typeMap[extension] || '配置文件';
    },
    
    // 获取所有文件总大小
    getTotalSize() {
      if (!this.configFiles.length) return '0 KB';
      
      // 将所有文件大小字符串转换为KB数值并求和
      let totalKB = 0;
      this.configFiles.forEach(file => {
        const sizeStr = file.fileSize || '0 KB';
        const match = sizeStr.match(/(\d+\.?\d*)\s*([KMG]B)/i);
        
        if (match) {
          const [, size, unit] = match;
          switch (unit.toUpperCase()) {
            case 'KB':
              totalKB += parseFloat(size);
              break;
            case 'MB':
              totalKB += parseFloat(size) * 1024;
              break;
            case 'GB':
              totalKB += parseFloat(size) * 1024 * 1024;
              break;
            default:
              break;
          }
        }
      });
      
      // 转换为合适的单位显示
      if (totalKB < 1024) {
        return `${Math.round(totalKB)} KB`;
      } else if (totalKB < 1024 * 1024) {
        return `${(totalKB / 1024).toFixed(2)} MB`;
      } else {
        return `${(totalKB / (1024 * 1024)).toFixed(2)} GB`;
      }
    },

    // 更新CodeMirror的模式
    updateCodeMirrorMode(fileName) {
      const extension = fileName.split('.').pop().toLowerCase();
      let mode = 'text/plain';
      
      switch (extension) {
        case 'xml':
          mode = 'application/xml';
          break;
        case 'json':
          mode = 'application/json';
          break;
        case 'yaml':
        case 'yml':
          mode = 'text/x-yaml';
          break;
        case 'properties':
          mode = 'text/x-properties';
          break;
        case 'sh':
          mode = 'text/x-sh';
          break;
        default:
          mode = 'text/plain';
      }
      
      this.cmOptions.mode = mode;
    },

    // 获取文件图标类名
    getFileIconClass(fileName) {
      if (!fileName) return 'file-icon-default';
      
      const extension = fileName.split('.').pop().toLowerCase();
      
      // 配置文件类型
      if (['xml'].includes(extension)) {
        return 'file-icon-xml';
      } else if (['json'].includes(extension)) {
        return 'file-icon-json';
      } else if (['yaml', 'yml'].includes(extension)) {
        return 'file-icon-yaml';
      } else if (['properties', 'prop'].includes(extension)) {
        return 'file-icon-properties';
      } else if (['conf'].includes(extension)) {
        return 'file-icon-conf';
      } else if (['ini'].includes(extension)) {
        return 'file-icon-ini';
      } else if (['acl'].includes(extension)) {
        return 'file-icon-acl';
      }
      
      // 脚本文件类型
      if (['sh', 'bash', 'zsh'].includes(extension)) {
        return 'file-icon-shell';
      } else if (['bat', 'cmd', 'ps1', 'makefile'].includes(extension)) {
        return 'file-icon-script';
      }
      
      // 日志文件类型
      if (['log', 'out', 'err', 'trace'].includes(extension)) {
        return 'file-icon-log';
      }
      
      // 数据文件类型
      if (['csv', 'tsv', 'xlsx', 'xls'].includes(extension)) {
        return 'file-icon-data';
      }
      
      // 文本文件类型
      if (['txt', 'md', 'markdown', 'rst'].includes(extension)) {
        return 'file-icon-text';
      }
      
      // 压缩文件类型
      if (['zip', 'tar', 'gz', 'bz2', 'rar', '7z'].includes(extension)) {
        return 'file-icon-archive';
      }
      
      // 处理没有扩展名的特殊文件名
      if (!extension || extension === fileName) {
        // 一些常见的无扩展名配置文件
        const specialFiles = {
          'options': 'file-icon-options',
          'config': 'file-icon-conf',
          'dockerfile': 'file-icon-script',
          'makefile': 'file-icon-script',
          'readme': 'file-icon-text'
        };
        
        const lowerFileName = fileName.toLowerCase();
        return specialFiles[lowerFileName] || 'file-icon-default';
      }
      
      return 'file-icon-default';
    },

    // 移除密码输入框的readonly属性
    removeReadonly(e) {
      // 移除readonly属性以允许用户输入
      if (e && e.target) {
        e.target.removeAttribute('readonly');
      }
    }
  }
};
</script>

<style scoped>
.config-download-container {
  padding: 32px;
  background-color: #fff;
  border-radius: 16px;
  box-shadow: 0 1px 5px rgba(0, 0, 0, 0.05);
  width: 100%;
}

/* 页面标题区域 */
.page-header {
  display: flex;
  align-items: center;
  margin-bottom: 32px;
}

.header-icon-wrapper {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, #007AFF 0%, #5AC8FA 100%);
  border-radius: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
  box-shadow: 0 6px 12px rgba(0, 122, 255, 0.2);
}

.header-icon {
  font-size: 30px;
  color: white;
}

.header-content {
  flex: 1;
}

.title {
  font-size: 28px;
  font-weight: 600;
  margin: 0 0 4px 0;
  color: #000;
  letter-spacing: -0.5px;
  line-height: 1.2;
}

.subtitle {
  font-size: 16px;
  color: #666;
  margin: 0;
}

/* 统计卡片和操作区域 */
.stats-and-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.stats-cards {
  display: flex;
  gap: 16px;
}

.stat-card {
  background-color: #f9fafb;
  border-radius: 12px;
  padding: 16px;
  min-width: 150px;
  display: flex;
  align-items: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  transition: all 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.05);
}

.stat-icon {
  font-size: 24px;
  color: #007AFF;
  margin-right: 16px;
}

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: #000;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.download-actions {
  display: flex;
}

.download-all-btn {
  border-radius: 12px;
  height: 42px;
  font-weight: 500;
  font-size: 15px;
  padding: 0 22px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  border: none;
  transition: all 0.3s ease;
  background: linear-gradient(135deg, #007AFF 0%, #1F62EE 100%);
}

.download-all-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.download-all-btn:focus {
  box-shadow: 0 0 0 2px rgba(0, 122, 255, 0.3);
}

/* 搜索区域 */
.search-section {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  gap: 16px;
}

.search-input {
  flex: 1;
  border-radius: 12px;
  padding: 10px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.search-input /deep/ .ant-input {
  font-size: 15px;
  padding-left: 12px;
}

.search-input /deep/ .ant-input-prefix {
  color: #999;
}

.view-toggle {
  margin-left: auto;
}

/* 列表视图 */
.file-list-section {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #f0f0f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}

.file-list-header {
  display: flex;
  padding: 16px 24px;
  background-color: #f9fafb;
  border-bottom: 1px solid #f0f0f0;
  font-weight: 500;
  color: #333;
  font-size: 14px;
}

.file-list-body {
  max-height: 600px;
  overflow-y: auto;
}

.file-item {
  display: flex;
  padding: 16px 24px;
  transition: all 0.2s;
  border-bottom: 1px solid #f0f0f0;
  align-items: center;
}

.file-item:hover {
  background-color: #f5f9ff;
  cursor: pointer;
}

.file-item:last-child {
  border-bottom: none;
}

.file-column {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.column-name {
  width: 30%;
  color: #000;
  font-weight: 500;
  display: flex;
  align-items: center;
}

.file-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  background-color: transparent;
  border-radius: 0;
  width: auto;
  height: auto;
}

.file-icon {
  font-size: 18px;
  color: #007AFF;
}

.file-name-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.column-desc {
  width: 40%;
  color: #666;
}

.column-size {
  width: 15%;
  color: #999;
  font-size: 13px;
}

.column-operation {
  width: 15%;
  display: flex;
  justify-content: flex-end;
}

.action-btn {
  padding: 4px 8px;
  font-size: 13px;
  transition: all 0.2s;
}

.action-btn:hover {
  transform: translateY(-1px);
}

.preview-btn {
  color: #007AFF;
  margin-right: 8px;
}

.download-btn {
  color: #34C759;
}

/* 网格视图 */
.file-grid-section {
  padding: 16px 0;
}

.file-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 20px;
}

.grid-item {
  background-color: #f9fafb;
  border-radius: 16px;
  padding: 20px;
  text-align: center;
  transition: all 0.2s;
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.grid-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.07);
  cursor: pointer;
}

.grid-item:hover .grid-item-actions {
  opacity: 1;
}

.grid-item-icon {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.grid-item-icon .file-type-icon {
  width: 40px;
  height: 50px;
}

.grid-item-info {
  width: 100%;
}

.grid-item-name {
  font-weight: 500;
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.grid-item-size {
  font-size: 12px;
  color: #999;
}

.grid-item-actions {
  position: absolute;
  top: 12px;
  right: 12px;
  opacity: 0;
  transition: opacity 0.3s;
}

.grid-action-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(255, 255, 255, 0.9);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  color: #007AFF;
  padding: 0;
}

.grid-action-btn:hover {
  background-color: #fff;
  transform: scale(1.05);
}

/* 空状态 */
.empty-data {
  padding: 60px 0;
  text-align: center;
}

.empty-search-text {
  color: #666;
}

.search-term {
  font-weight: 600;
  color: #333;
}

.loading-spinner {
  width: 100%;
}

/* 预览模态窗 */
.preview-modal /deep/ .ant-modal-content {
  border-radius: 16px;
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.modal-file-info {
  display: flex;
  align-items: center;
}

.modal-file-icon {
  font-size: 22px;
  margin-right: 12px;
  color: #007AFF;
}

.modal-file-name {
  font-size: 18px;
  font-weight: 500;
  color: #333;
}

.modal-actions {
  display: flex;
  gap: 12px;
}

.modal-action-btn {
  color: #007AFF;
  font-size: 14px;
}

.modal-close-btn {
  color: #999;
  font-size: 16px;
}

.syntax-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.syntax-info {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.config-preview {
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 600px;
  overflow-y: auto;
  background-color: #f9fafb;
  padding: 24px;
  border-radius: 12px;
  font-family: 'SF Mono', SFMono-Regular, Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 14px;
  line-height: 1.6;
  color: #333;
  border: 1px solid #f0f0f0;
  transition: all 0.3s;
}

.config-preview.dark-theme {
  background-color: #1F2937;
  color: #E5E7EB;
  border-color: #374151;
}

/* 动画过渡效果 */
.list-enter-active, .list-leave-active,
.grid-enter-active, .grid-leave-active {
  transition: all 0.3s;
}

.list-enter, .list-leave-to,
.grid-enter, .grid-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

/* 响应式调整 */
@media (max-width: 1024px) {
  .stats-and-actions {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  
  .stats-cards {
    width: 100%;
  }
  
  .stat-card {
    flex: 1;
  }
  
  .download-actions {
    width: 100%;
  }
  
  .download-all-btn {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .config-download-container {
    padding: 20px;
  }
  
  .page-header {
    margin-bottom: 20px;
  }
  
  .search-section {
    flex-direction: column;
    align-items: stretch;
  }
  
  .view-toggle {
    margin-left: 0;
    margin-top: 12px;
    display: flex;
    justify-content: flex-end;
  }
  
  .header-section {
    flex-direction: column;
    align-items: flex-start;
    margin-bottom: 20px;
  }
  
  .title {
    margin-bottom: 16px;
  }
  
  .column-desc {
    display: none;
  }
  
  .column-name {
    width: 50%;
  }
  
  .column-size {
    width: 20%;
  }
  
  .column-operation {
    width: 30%;
  }
  
  .file-grid {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  }
  
  .file-icon {
    margin-right: 4px;
  }
}

.code-mirror {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
}

.code-mirror :deep(.CodeMirror) {
  height: 100%;
  font-family: 'SF Mono', SFMono-Regular, Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 14px;
  line-height: 1.6;
}

.code-mirror :deep(.CodeMirror-gutters) {
  border-right: 1px solid #f0f0f0;
  background-color: #f9fafb;
}

.code-mirror :deep(.CodeMirror-linenumber) {
  color: #999;
  padding: 0 8px;
}

.code-mirror :deep(.CodeMirror-foldgutter) {
  width: 20px;
}

.code-mirror :deep(.CodeMirror-foldgutter-open),
.code-mirror :deep(.CodeMirror-foldgutter-folded) {
  color: #999;
  cursor: pointer;
}

/* 深色主题 */
.code-mirror :deep(.cm-s-dracula) {
  background-color: #1F2937;
  color: #E5E7EB;
}

.code-mirror :deep(.cm-s-dracula .CodeMirror-gutters) {
  background-color: #111827;
  border-right: 1px solid #374151;
}

.code-mirror :deep(.cm-s-dracula .CodeMirror-linenumber) {
  color: #6B7280;
}

/* XML声明和样式表声明的样式 */
.code-mirror :deep(.cm-s-dracula .cm-meta) {
  color: #FF9D00; /* XML声明使用橙色 */
}

.code-mirror :deep(.cm-s-dracula .cm-tag) {
  color: #FF5370; /* 标签使用红色 */
}

.code-mirror :deep(.cm-s-dracula .cm-attribute) {
  color: #C792EA; /* 属性使用紫色 */
}

.code-mirror :deep(.cm-s-dracula .cm-string) {
  color: #C3E88D; /* 字符串使用绿色 */
}

/* 浅色主题 */
.code-mirror :deep(.cm-s-eclipse) {
  background-color: #f9fafb;
  color: #333;
}

.code-mirror :deep(.cm-s-eclipse .CodeMirror-gutters) {
  background-color: #f0f0f0;
  border-right: 1px solid #e0e0e0;
}

.code-mirror :deep(.cm-s-eclipse .CodeMirror-linenumber) {
  color: #999;
}

/* 浅色主题的XML样式 */
.code-mirror :deep(.cm-s-eclipse .cm-meta) {
  color: #FF6B00; /* XML声明使用橙色 */
}

.code-mirror :deep(.cm-s-eclipse .cm-tag) {
  color: #881280; /* 标签使用深紫色 */
}

.code-mirror :deep(.cm-s-eclipse .cm-attribute) {
  color: #994500; /* 属性使用棕色 */
}

.code-mirror :deep(.cm-s-eclipse .cm-string) {
  color: #1A1AA6; /* 字符串使用蓝色 */
}

/* 下载模态窗样式 */
.download-modal /deep/ .ant-modal-content {
  border-radius: 16px;
  overflow: hidden;
}

.download-modal /deep/ .ant-modal-header {
  border-bottom: 1px solid #f0f0f0;
  padding: 20px 24px;
}

.download-modal /deep/ .ant-modal-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.download-modal /deep/ .ant-modal-footer {
  border-top: 1px solid #f0f0f0;
  padding: 16px 24px;
}

.download-modal /deep/ .ant-btn-primary {
  background: linear-gradient(135deg, #007AFF 0%, #1F62EE 100%);
  border: none;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.download-options {
  padding: 8px 0;
}

.option-row {
  display: flex;
  margin-bottom: 16px;
  align-items: center;
}

.option-label {
  width: 80px;
  font-weight: 500;
  color: #333;
}

.option-content {
  flex: 1;
  display: flex;
  align-items: center;
}

.format-tag {
  font-weight: 500;
  background-color: #e6f7ff;
  color: #007AFF;
  border-color: #91d5ff;
  margin-right: 12px;
}

.format-desc {
  color: #666;
  font-size: 14px;
}

.option-desc {
  margin-left: 12px;
  color: #666;
}

.password-section {
  background-color: #f9fafb;
  border-radius: 8px;
  padding: 16px;
  margin-top: 16px;
}

.password-error {
  color: #f5222d;
  font-size: 13px;
  margin-top: 4px;
}

.password-error .anticon {
  margin-right: 6px;
}

.password-tips {
  background-color: #fffbe6;
  border: 1px solid #ffe58f;
  padding: 12px 16px;
  border-radius: 8px;
  margin-top: 16px;
  color: #874d00;
  font-size: 13px;
}

.password-tips .anticon {
  color: #faad14;
  margin-right: 8px;
}

.password-tips ul {
  margin: 8px 0 0 0;
  padding-left: 24px;
  line-height: 1.6;
}

/* 打包进度显示 */
.compress-progress {
  margin-top: 16px;
  padding: 16px;
  background-color: #f9fafb;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.progress-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.progress-percent {
  font-size: 12px;
  color: #999;
}

.progress-bar {
  margin-top: 8px;
  margin-bottom: 8px;
}

.progress-tips {
  font-size: 12px;
  color: #666;
}

/* 进度模态窗样式 */
.progress-modal {
  width: 100%;
  max-width: 500px;
  padding: 0;
}

.progress-modal /deep/ .ant-modal-content {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  overflow: hidden;
}

.progress-content {
  padding: 32px;
}

.progress-header {
  display: flex;
  align-items: center;
  margin-bottom: 28px;
}

.progress-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  background: linear-gradient(135deg, #007AFF 0%, #00C2FF 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.2);
}

.spinning-icon {
  font-size: 28px;
  color: #FFFFFF;
}

.progress-info {
  flex: 1;
}

.progress-info h3 {
  font-size: 18px;
  font-weight: 600;
  color: #000000;
  margin: 0 0 4px 0;
  line-height: 1.3;
}

.progress-info p {
  font-size: 14px;
  color: #666666;
  margin: 0;
  line-height: 1.5;
}

.progress-bar-container {
  position: relative;
  margin-top: 8px;
}

.progress-track {
  height: 6px;
  background: rgba(0, 0, 0, 0.1);
  border-radius: 3px;
  overflow: hidden;
  position: relative;
}

.progress-fill {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(90deg, #007AFF 0%, #00C2FF 100%);
  transform: translateX(-100%);
  transition: transform 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

.progress-glow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(255, 255, 255, 0.3) 50%,
    transparent 100%
  );
  animation: glow 2s linear infinite;
}

.progress-number {
  position: absolute;
  right: 0;
  top: -24px;
  font-size: 13px;
  font-weight: 500;
  color: #007AFF;
}

@keyframes glow {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}

/* 菜单图标样式 */
.file-menu-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-right: 8px;
  width: 18px;
  height: 22px;
}

/* 下拉菜单样式 */
.download-dropdown {
  min-width: 180px;
}

.download-dropdown /deep/ .ant-dropdown-menu-item {
  display: flex;
  align-items: center;
  padding: 8px 16px;
}

.download-dropdown /deep/ .ant-dropdown-menu-item:hover {
  background-color: #f5f9ff;
}

.page-header-icon {
  width: 40px;
  height: 40px;
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="white" d="M10,4H4C2.9,4,2,4.9,2,6v12c0,1.1,0.9,2,2,2h16c1.1,0,2-0.9,2-2V8c0-1.1-0.9-2-2-2h-8L10,4z"/><path fill="white" opacity="0.4" d="M9,8l-1-4H4C2.9,4,2,4.9,2,6v12c0,1.1,0.9,2,2,2h16c1.1,0,2-0.9,2-2V8c0-1.1-0.9-2-2-2H9z M21,18.01L3,18V6h7.1l1,2H21V18.01z"/></svg>');
  background-repeat: no-repeat;
  background-position: center;
  background-size: contain;
}
</style> 