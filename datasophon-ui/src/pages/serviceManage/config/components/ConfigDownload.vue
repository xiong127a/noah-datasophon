<template>
  <div class="config-download-container">
    <!-- 顶部图标和标题区域 -->
    <div class="page-header">
      <div class="header-icon-wrapper">
        <div class="page-header-icon"></div>
      </div>
      <div class="header-content">
        <h2 class="title">{{ serviceName }} 配置文件管理</h2>
        <p class="subtitle">管理和下载{{ serviceName }}服务的配置文件</p>
      </div>
    </div>
    
    <!-- 文件统计和操作区域 -->
    <div class="stats-and-actions">
      <div class="stats-cards">
        <div class="stat-card">
          <div class="stat-icon-wrapper file-count-icon">
            <a-icon type="file" theme="filled" style="font-size: 20px; color: white;" />
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ configFiles.length }}</div>
            <div class="stat-label">配置文件总数</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon-wrapper file-size-icon">
            <a-icon type="database" theme="filled" style="font-size: 20px; color: white;" />
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ getTotalSize() }}</div>
            <div class="stat-label">总大小</div>
          </div>
        </div>
      </div>
      
      <!-- 打包下载按钮组改为引入组件 -->
      <download-config 
        :serviceId="serviceId"
        :serviceName="serviceName"
        ref="downloadConfig"
      />
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

    <!-- 使用拆分出来的预览组件 -->
    <config-preview
      v-model="previewVisible"
      :loading="previewLoading"
      :fileName="currentPreviewFile"
      :fileContent="previewContent"
      @download="downloadCurrentFile"
    />
  </div>
</template>

<script>
import Empty from 'ant-design-vue/lib/empty';
import ConfigPreview from './ConfigPreview.vue';
import DownloadConfig from './DownloadConfig.vue';

export default {
  name: 'ConfigDownload',
  components: {
    ConfigPreview,
    DownloadConfig
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
      emptyImage: Empty.PRESENTED_IMAGE_SIMPLE,
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
  },
  watch: {
  },
  mounted() {
    this.fetchConfigFiles();
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

    // 下载当前预览的文件
    downloadCurrentFile() {
      if (!this.currentPreviewFile) return;
      
      const file = this.configFiles.find(f => f.fileName === this.currentPreviewFile);
      if (file) {
        this.downloadSingleConfig(file);
      }
    },

    // 下载单个配置文件 - 修改为调用DownloadConfig组件的方法
    downloadSingleConfig(record) {
      if (this.$refs.downloadConfig) {
        this.$refs.downloadConfig.downloadSingleConfig(record);
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
  position: relative;
  overflow: hidden;
}

.header-icon-wrapper::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.2) 0%, transparent 50%);
  z-index: 1;
}

.page-header-icon {
  width: 40px;
  height: 40px;
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="white" d="M20,6h-8l-2-2H4C2.9,4,2,4.9,2,6v12c0,1.1,0.9,2,2,2h16c1.1,0,2-0.9,2-2V8C22,6.9,21.1,6,20,6z M20,18H4V8h16V18z"/><path fill="white" opacity="0.5" d="M4,8h16v10H4V8z"/><path fill="white" d="M12,9h4v2h-4V9z M12,12h4v2h-4V12z M12,15h4v2h-4V15z"/></svg>');
  background-repeat: no-repeat;
  background-position: center;
  background-size: contain;
  position: relative;
  z-index: 2;
  transition: transform 0.3s ease;
}

.header-icon-wrapper:hover .page-header-icon {
  transform: scale(1.1);
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
  margin: 24px 0;
}

.stats-cards {
  display: flex;
  margin-bottom: 20px;
  gap: 20px;
}

.stat-card {
  flex: 1;
  display: flex;
  align-items: center;
  padding: 16px;
  background-color: #f5f5f7;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  min-width: 180px;
}

.stat-icon-wrapper {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
}

.file-count-icon {
  background: linear-gradient(135deg, #1890ff, #0050b3);
}

.file-size-icon {
  background: linear-gradient(135deg, #52c41a, #237804);
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #000;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-top: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 85px;
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
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.search-input:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.search-input /deep/ .ant-input {
  font-size: 15px;
  padding: 12px 12px 12px 40px;
  border-radius: 12px;
  height: auto;
  border: none;
  background-color: #f5f5f7;
  transition: all 0.3s ease;
}

.search-input /deep/ .ant-input:focus {
  background-color: #ffffff;
  box-shadow: 0 0 0 2px rgba(0, 122, 255, 0.2);
}

.search-input /deep/ .ant-input:hover {
  background-color: #ffffff;
}

.search-input /deep/ .ant-input-prefix {
  color: #8e8e93;
  margin-right: 8px;
  left: 16px;
}

.search-input /deep/ .ant-input-suffix .anticon {
  color: #8e8e93;
  transition: color 0.3s;
}

.search-input /deep/ .ant-input-affix-wrapper .ant-input-clear-icon {
  color: #8e8e93;
  opacity: 0.7;
}

.search-input /deep/ .ant-input-affix-wrapper .ant-input-clear-icon:hover {
  color: #007AFF;
  opacity: 1;
}

.view-toggle {
  margin-left: auto;
}

.view-toggle /deep/ .ant-radio-button-wrapper {
  border-radius: 8px;
  overflow: hidden;
  border-color: #d1d1d6;
  color: #8e8e93;
}

.view-toggle /deep/ .ant-radio-button-wrapper:first-child {
  border-radius: 8px 0 0 8px;
}

.view-toggle /deep/ .ant-radio-button-wrapper:last-child {
  border-radius: 0 8px 8px 0;
}

.view-toggle /deep/ .ant-radio-button-wrapper-checked {
  color: #007AFF;
  border-color: #007AFF;
}

.view-toggle /deep/ .ant-radio-button-wrapper-checked::before {
  background-color: #007AFF;
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

.file-type-icon {
  width: 32px;
  height: 40px;
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
  position: relative;
  filter: drop-shadow(0px 2px 3px rgba(0, 0, 0, 0.1));
  transition: all 0.3s ease;
}

.file-item:hover .file-type-icon,
.grid-item:hover .file-type-icon {
  transform: translateY(-2px);
  filter: drop-shadow(0px 4px 5px rgba(0, 0, 0, 0.15));
}

.file-icon-default {
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="%23A6B0C3" d="M14,2H6C4.9,2,4,2.9,4,4v16c0,1.1,0.9,2,2,2h12c1.1,0,2-0.9,2-2V8L14,2z"/><path fill="white" d="M14,3v5h5v11c0,0.6-0.4,1-1,1H6c-0.6,0-1-0.4-1-1V4c0-0.6,0.4-1,1-1H14z"/><path fill="%23A6B0C3" opacity="0.4" d="M14,2l6,6h-6V2z"/></svg>');
}

.file-icon-conf, .file-icon-properties, .file-icon-ini {
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="%23FFA726" d="M14,2H6C4.9,2,4,2.9,4,4v16c0,1.1,0.9,2,2,2h12c1.1,0,2-0.9,2-2V8L14,2z"/><path fill="white" d="M14,3v5h5v11c0,0.6-0.4,1-1,1H6c-0.6,0-1-0.4-1-1V4c0-0.6,0.4-1,1-1H14z"/><path fill="%23FFA726" opacity="0.4" d="M14,2l6,6h-6V2z"/><path fill="%23FFA726" d="M10.7,15l-1.5-1.5L10.7,12l-1.4-1.4l-2.9,2.9l2.9,2.9L10.7,15z M17.6,13.5l-2.9-2.9l-1.4,1.4l1.5,1.5l-1.5,1.5l1.4,1.4L17.6,13.5z"/></svg>');
}

.file-icon-xml {
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="%235C6BC0" d="M14,2H6C4.9,2,4,2.9,4,4v16c0,1.1,0.9,2,2,2h12c1.1,0,2-0.9,2-2V8L14,2z"/><path fill="white" d="M14,3v5h5v11c0,0.6-0.4,1-1,1H6c-0.6,0-1-0.4-1-1V4c0-0.6,0.4-1,1-1H14z"/><path fill="%235C6BC0" opacity="0.4" d="M14,2l6,6h-6V2z"/><path fill="%235C6BC0" d="M10.7,15l-1.5-1.5L10.7,12l-1.4-1.4l-2.9,2.9l2.9,2.9L10.7,15z M17.6,13.5l-2.9-2.9l-1.4,1.4l1.5,1.5l-1.5,1.5l1.4,1.4L17.6,13.5z"/></svg>');
}

.file-icon-json {
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="%2326C6DA" d="M14,2H6C4.9,2,4,2.9,4,4v16c0,1.1,0.9,2,2,2h12c1.1,0,2-0.9,2-2V8L14,2z"/><path fill="white" d="M14,3v5h5v11c0,0.6-0.4,1-1,1H6c-0.6,0-1-0.4-1-1V4c0-0.6,0.4-1,1-1H14z"/><path fill="%2326C6DA" opacity="0.4" d="M14,2l6,6h-6V2z"/><path fill="%2326C6DA" d="M12,11c-1.1,0-2,0.9-2,2s0.9,2,2,2s2-0.9,2-2S13.1,11,12,11z"/></svg>');
}

.file-icon-yaml {
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="%238E24AA" d="M14,2H6C4.9,2,4,2.9,4,4v16c0,1.1,0.9,2,2,2h12c1.1,0,2-0.9,2-2V8L14,2z"/><path fill="white" d="M14,3v5h5v11c0,0.6-0.4,1-1,1H6c-0.6,0-1-0.4-1-1V4c0-0.6,0.4-1,1-1H14z"/><path fill="%238E24AA" opacity="0.4" d="M14,2l6,6h-6V2z"/><path fill="%238E24AA" d="M8,13.5l2-2l2,2l2-2l2,2v-2l-2-2l-2,2l-2-2l-2,2V13.5z"/></svg>');
}

.file-icon-shell, .file-icon-script {
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="%234CAF50" d="M14,2H6C4.9,2,4,2.9,4,4v16c0,1.1,0.9,2,2,2h12c1.1,0,2-0.9,2-2V8L14,2z"/><path fill="white" d="M14,3v5h5v11c0,0.6-0.4,1-1,1H6c-0.6,0-1-0.4-1-1V4c0-0.6,0.4-1,1-1H14z"/><path fill="%234CAF50" opacity="0.4" d="M14,2l6,6h-6V2z"/><path fill="%234CAF50" d="M10.7,15l-1.5-1.5L10.7,12l-1.4-1.4l-2.9,2.9l2.9,2.9L10.7,15z M17,15h-4v-1.5h4V15z"/></svg>');
}

.file-icon-log, .file-icon-text {
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="%23607D8B" d="M14,2H6C4.9,2,4,2.9,4,4v16c0,1.1,0.9,2,2,2h12c1.1,0,2-0.9,2-2V8L14,2z"/><path fill="white" d="M14,3v5h5v11c0,0.6-0.4,1-1,1H6c-0.6,0-1-0.4-1-1V4c0-0.6,0.4-1,1-1H14z"/><path fill="%23607D8B" opacity="0.4" d="M14,2l6,6h-6V2z"/><path fill="%23607D8B" d="M8,12h8v1.5H8V12z M8,15h8v1.5H8V15z M8,9h4v1.5H8V9z"/></svg>');
}

.file-icon-archive {
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="%23FF7043" d="M14,2H6C4.9,2,4,2.9,4,4v16c0,1.1,0.9,2,2,2h12c1.1,0,2-0.9,2-2V8L14,2z"/><path fill="white" d="M14,3v5h5v11c0,0.6-0.4,1-1,1H6c-0.6,0-1-0.4-1-1V4c0-0.6,0.4-1,1-1H14z"/><path fill="%23FF7043" opacity="0.4" d="M14,2l6,6h-6V2z"/><path fill="%23FF7043" d="M11,14.5V16h2v-1.5h1V13h-4v1.5H11z M11,9h2v1.5h-2V9z M11,6h2v1.5h-2V6z M11,12h2v-1.5h-2V12z"/></svg>');
}

.file-icon-data {
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="%232196F3" d="M14,2H6C4.9,2,4,2.9,4,4v16c0,1.1,0.9,2,2,2h12c1.1,0,2-0.9,2-2V8L14,2z"/><path fill="white" d="M14,3v5h5v11c0,0.6-0.4,1-1,1H6c-0.6,0-1-0.4-1-1V4c0-0.6,0.4-1,1-1H14z"/><path fill="%232196F3" opacity="0.4" d="M14,2l6,6h-6V2z"/><path fill="%232196F3" d="M7,13h2v5H7V13z M11,10h2v8h-2V10z M15,15h2v3h-2V15z"/></svg>');
}

.file-icon-acl {
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="%23F44336" d="M14,2H6C4.9,2,4,2.9,4,4v16c0,1.1,0.9,2,2,2h12c1.1,0,2-0.9,2-2V8L14,2z"/><path fill="white" d="M14,3v5h5v11c0,0.6-0.4,1-1,1H6c-0.6,0-1-0.4-1-1V4c0-0.6,0.4-1,1-1H14z"/><path fill="%23F44336" opacity="0.4" d="M14,2l6,6h-6V2z"/><path fill="%23F44336" d="M16,15h-3v-1.5c0-0.8-0.7-1.5-1.5-1.5S10,12.7,10,13.5V15H8v-1.5c0-1.9,1.6-3.5,3.5-3.5s3.5,1.6,3.5,3.5V15z"/></svg>');
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
  border-radius: 6px;
  border: 1px solid rgba(0, 122, 255, 0.1);
  background-color: rgba(0, 122, 255, 0.05);
  padding: 4px 10px;
  transition: all 0.3s ease;
}

.modal-action-btn:hover {
  background-color: rgba(0, 122, 255, 0.1);
  color: #0056b3;
}

.modal-close-btn {
  color: #666;
  font-size: 14px;
  border-radius: 6px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  background-color: rgba(0, 0, 0, 0.02);
  padding: 4px 10px;
  transition: all 0.3s ease;
}

.modal-close-btn:hover {
  background-color: rgba(0, 0, 0, 0.05);
  color: #333;
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
  height: 100%;
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

/* 为CodeMirror添加滚动条样式 */
.code-mirror :deep(.CodeMirror-scroll) {
  overflow: auto !important;
  overflow-x: auto !important;
  overflow-y: auto !important;
  height: auto !important;
  max-height: none !important;
}

/* 自定义滚动条样式 */
.code-mirror :deep(::-webkit-scrollbar) {
  width: 8px;
  height: 8px;
}

.code-mirror :deep(::-webkit-scrollbar-track) {
  background: rgba(0, 0, 0, 0.05);
  border-radius: 4px;
}

.code-mirror :deep(::-webkit-scrollbar-thumb) {
  background: rgba(0, 0, 0, 0.25);
  border-radius: 4px;
  transition: all 0.3s;
}

.code-mirror :deep(::-webkit-scrollbar-thumb:hover) {
  background: rgba(0, 0, 0, 0.4);
}

/* 深色主题的滚动条 */
.code-mirror :deep(.cm-s-dracula::-webkit-scrollbar-track) {
  background: rgba(255, 255, 255, 0.05);
}

.code-mirror :deep(.cm-s-dracula::-webkit-scrollbar-thumb) {
  background: rgba(255, 255, 255, 0.2);
}

.code-mirror :deep(.cm-s-dracula::-webkit-scrollbar-thumb:hover) {
  background: rgba(255, 255, 255, 0.3);
}
</style>