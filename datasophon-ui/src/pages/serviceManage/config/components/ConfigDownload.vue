<template>
  <div class="config-download-container">
    <!-- 顶部图标和标题区域 -->
    <div class="page-header">
      <div class="header-icon-wrapper">
        <a-icon type="folder-open" theme="filled" class="header-icon" />
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
          <a-icon type="file" theme="filled" class="stat-icon" />
          <div class="stat-content">
            <div class="stat-value">{{ configFiles.length }}</div>
            <div class="stat-label">配置文件总数</div>
          </div>
        </div>
        <div class="stat-card">
          <a-icon type="code" theme="filled" class="stat-icon" />
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
            <a-menu-item key="zip" @click="downloadAllConfigs('zip')">
              <a-icon type="file-zip" />
              ZIP格式
            </a-menu-item>
            <a-menu-item key="tar.gz" @click="downloadAllConfigs('tar.gz')">
              <a-icon type="file-text" />
              TAR.GZ格式
            </a-menu-item>
            <a-menu-item key="7z" @click="downloadAllConfigs('7z')">
              <a-icon type="file" />
              7Z格式
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
                <a-icon :type="getFileIcon(file.fileName)" theme="filled" class="file-icon" />
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
              <a-icon :type="getFileIcon(file.fileName)" theme="filled" />
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
      :maskClosable="true"
      class="preview-modal"
      :destroyOnClose="true"
    >
      <div class="modal-header">
        <div class="modal-file-info">
          <a-icon :type="getFileIcon(currentPreviewFile)" theme="filled" class="modal-file-icon" />
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
              <a-radio-button value="light">浅色</a-radio-button>
              <a-radio-button value="dark">深色</a-radio-button>
            </a-radio-group>
          </div>
        </div>
        <pre :class="['config-preview', {'dark-theme': previewTheme === 'dark'}]">{{ previewContent }}</pre>
      </a-spin>
    </a-modal>
  </div>
</template>

<script>
import Empty from 'ant-design-vue/lib/empty';
export default {
  name: 'ConfigDownload',
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
      previewTheme: 'light',
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
      ]
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
    }
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

    // 打包下载所有配置文件
    async downloadAllConfigs(format = 'zip') {
      this.downloadLoading = true;
      try {
        if (!global.API || !global.API.downloadAllServiceConfigFiles) {
          this.$message.error('系统配置错误，无法下载文件');
          return;
        }
        
        // 构建下载URL，添加格式参数
        const downloadUrl = `${window.location.origin}${global.API.downloadAllServiceConfigFiles}?serviceInstanceId=${this.serviceId}&format=${format}`;
        
        const link = document.createElement('a');
        link.href = downloadUrl;
        
        // 根据不同格式设置不同的文件扩展名
        const fileExtension = format === 'tar.gz' ? '.tar.gz' : `.${format}`;
        link.setAttribute('download', `${this.serviceName}_configs${fileExtension}`);
        
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        
        this.$message.success(`正在下载 ${format.toUpperCase()} 格式配置文件`);
      } catch (error) {
        this.$message.error('下载配置文件失败');
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
          return;
        }
        
        const params = {
          serviceInstanceId: this.serviceId,
          fileName: record.fileName
        };
        
        const res = await this.$axiosJsonPost(global.API.previewServiceConfigFile, params);
        
        if (res.code === 200) {
          this.previewContent = res.data || '文件内容为空';
        } else {
          this.previewContent = '获取文件内容失败: ' + (res.msg || '未知错误');
        }
      } catch (error) {
        this.previewContent = '获取文件内容失败: ' + (error && error.message ? error.message : '未知错误');
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
        xml: 'file-xml',
        json: 'file-json',
        properties: 'setting',
        yaml: 'code',
        yml: 'code',
        sh: 'console',
        txt: 'file-text',
        conf: 'setting',
        cfg: 'setting',
        ini: 'setting',
        log: 'file-text'
      };
      
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
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background-color: #e7f3ff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
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
  font-size: 34px;
  margin-bottom: 16px;
  color: #007AFF;
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
}
</style> 