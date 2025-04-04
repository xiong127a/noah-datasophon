<template>
  <a-modal
    :visible="visible"
    :title="$t('同步Hosts文件')"
    :width="750"
    :maskClosable="false"
    :destroyOnClose="true"
    @cancel="handleCancel"
    :footer="null"
    class="sync-hosts-modal"
  >
    <div class="sync-hosts-container">
      <!-- 功能介绍 -->
      <div class="feature-description">
        <div class="description-icon">
          <a-icon type="api" />
        </div>
        <div class="description-content">
          <div class="description-title">{{ $t('功能说明') }}</div>
          <div class="description-text">
            {{ $t('该功能可以将包含所有主机IP和主机名的hosts文件同步到集群的所有主机，确保集群中的每台主机都能通过主机名相互访问。在批量设置主机名后，建议执行此操作。') }}
          </div>
        </div>
      </div>
      
      <!-- Hosts文件预览 -->
      <div v-if="!taskId" class="hosts-card-container">
        <div class="hosts-preview-card">
          <a-spin :spinning="loading">
            <div class="card-title">{{ $t('Hosts文件预览') }}</div>
            
            <div class="preview-header">
              <div class="preview-subtitle">{{ $t('主机IP与主机名映射') }}</div>
              <div class="preview-info" v-if="previewData">
                <a-tag color="blue">{{ previewData.hostCount }} {{ $t('台主机') }}</a-tag>
              </div>
            </div>
            
            <!-- 采用IDE风格展示hosts文件 -->
            <div class="hosts-file-container">
              <div class="modern-ide">
                <!-- IDE工具栏 -->
                <div class="ide-toolbar">
                  <div class="ide-breadcrumb">
                    <div class="breadcrumb-item root">
                      <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
                        <polyline points="9 22 9 12 15 12 15 22"></polyline>
                      </svg>
                      <span>/</span>
                    </div>
                    <div class="breadcrumb-item">
                      <span>etc</span>
                    </div>
                    <div class="breadcrumb-separator">/</div>
                    <div class="breadcrumb-item active">
                      <span>hosts</span>
                    </div>
                  </div>
                </div>
                
                <!-- 代码编辑区域 -->
                <div class="ide-editor">
                  <!-- 侧边栏 - 行号 -->
                  <div class="ide-sidebar">
                    <div class="gutter-container">
                      <div class="gutter-line-numbers">
                        <div 
                          v-for="n in getLineCount()" 
                          :key="n" 
                          class="line-number"
                        >
                          {{ n }}
                        </div>
                      </div>
                    </div>
                  </div>
                  
                  <!-- 主代码区域 -->
                  <div class="code-container" ref="codeContainer">
                    <!-- 使用结构化数据渲染 -->
                    <div v-if="hasStructuredData()" class="code-content">
                      <div v-for="(entry, index) in previewData.hostsEntries" :key="index" class="hosts-line">
                        <!-- 注释行 -->
                        <div v-if="entry.type === 'COMMENT'" class="comment-line">
                          <span class="comment">{{ entry.comment }}</span>
                        </div>
                        <!-- IP映射行 -->
                        <div v-else-if="entry.type === 'MAPPING'" class="mapping-line">
                          <span class="ip">{{ entry.ip }}</span>
                          <span class="separator">&nbsp;&nbsp;&nbsp;&nbsp;</span>
                          <span class="hostnames">{{ formatHostnames(entry.hostnames) }}</span>
                        </div>
                      </div>
                    </div>
                    <!-- 向后兼容，使用旧的字符串内容渲染 -->
                    <div v-else-if="hostsContent" class="code-content" v-html="formatHostsFile(hostsContent)"></div>
                    <!-- 空内容处理 -->
                    <div v-else class="empty-content">{{ $t('暂无hosts文件内容') }}</div>
                  </div>
                </div>
                
                <!-- 底部状态栏 -->
                <div class="ide-statusbar">
                  <div class="statusbar-left">
                    <div class="status-item">
                      <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                        <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                        <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                      </svg>
                      <span>{{ $t('只读') }}</span>
                    </div>
                  </div>
                  <div class="statusbar-right">
                    <div class="status-item">
                      <a-icon type="info-circle" />
                      <span>{{ $t('同步后将会备份原hosts文件') }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </a-spin>
          
          <!-- 同步按钮 -->
          <div class="hosts-actions" v-if="!loading">
            <a-button @click="handleCancel">{{ $t('取消') }}</a-button>
            <a-button
              type="primary"
              :loading="syncInProgress"
              :disabled="!previewData || syncInProgress"
              @click="handleSync"
              class="sync-button"
            >{{ $t('同步到所有主机') }}</a-button>
          </div>
        </div>
      </div>
      
      <!-- 任务进度卡片 -->
      <div v-if="taskId" class="progress-card">
        <div class="card-title">{{ $t('同步进度') }}</div>
        
        <div class="progress-content">
          <!-- 进度条和状态 -->
          <div class="progress-status">
            <div class="status-header">
              <div class="status-title" v-if="taskStatus === 'IN_PROGRESS'">
                <a-icon type="sync" spin class="status-icon in-progress" />
                <span>{{ $t('正在同步') }}</span>
              </div>
              <div class="status-title" v-else-if="taskStatus === 'COMPLETED'">
                <a-icon type="check-circle" class="status-icon completed" />
                <span>{{ $t('同步完成') }}</span>
              </div>
              <div class="status-title" v-else-if="taskStatus === 'FAILED'">
                <a-icon type="close-circle" class="status-icon failed" />
                <span>{{ $t('同步失败') }}</span>
              </div>
              
              <div class="status-stats">
                <div class="stat-item completed">
                  <div class="stat-value">{{ completedCount }}</div>
                  <div class="stat-label">{{ $t('成功') }}</div>
                </div>
                <div class="stat-item failed" v-if="failedCount > 0">
                  <div class="stat-value">{{ failedCount }}</div>
                  <div class="stat-label">{{ $t('失败') }}</div>
                </div>
              </div>
            </div>
            
            <a-progress 
              :percent="percentage" 
              :status="taskStatus === 'FAILED' ? 'exception' : taskStatus === 'COMPLETED' ? 'success' : 'active'"
              :strokeColor="taskStatus === 'FAILED' ? '#ff3b30' : taskStatus === 'COMPLETED' ? '#34c759' : '#0071e3'"
              :strokeWidth="6"
            />
            
            <!-- 当前处理的主机 -->
            <div class="current-host" v-if="currentHost && taskStatus === 'IN_PROGRESS'">
              <a-tag color="processing">{{ $t('正在同步') }}: {{ currentHost }}</a-tag>
            </div>
            
            <!-- 消息通知 -->
            <div class="task-message" v-if="taskMessage">
              {{ taskMessage }}
            </div>
          </div>
          
          <!-- 完成的主机列表 -->
          <a-collapse 
            v-if="completedHosts.length > 0" 
            class="hosts-collapse"
            expandIconPosition="right"
          >
            <a-collapse-panel :header="$t('已同步主机') + ' (' + completedHosts.length + ')'" key="1" class="apple-collapse-panel">
              <div class="hosts-list">
                <a-tag 
                  v-for="host in completedHosts" 
                  :key="host" 
                  class="host-tag success-host-tag"
                >
                  {{ host }}
                </a-tag>
              </div>
            </a-collapse-panel>
          </a-collapse>
          
          <!-- 失败的主机列表 -->
          <a-collapse 
            v-if="failedHosts && Object.keys(failedHosts).length > 0" 
            class="hosts-collapse"
            expandIconPosition="right"
          >
            <a-collapse-panel :header="$t('同步失败的主机') + ' (' + Object.keys(failedHosts).length + ')'" key="2" class="apple-collapse-panel error-panel">
              <div class="failed-hosts-list">
                <div class="failed-host-item" v-for="(reason, ip) in failedHosts" :key="ip">
                  <div class="failed-host-ip">{{ ip }}</div>
                  <div class="failed-host-reason">{{ reason }}</div>
                </div>
              </div>
            </a-collapse-panel>
          </a-collapse>
        </div>
        
        <div class="progress-actions">
          <a-button @click="handleSuccess" v-if="taskStatus === 'COMPLETED'" class="sync-button">{{ $t('完成') }}</a-button>
          <a-button @click="handleCancel" v-else class="cancel-button">{{ $t('关闭') }}</a-button>
        </div>
      </div>
    </div>
  </a-modal>
</template>

<script>
import HostCheckService from './HostCheckService'

export default {
  name: 'SyncHostsFileModal',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    clusterId: {
      type: [Number, String],
      required: true
    }
  },
  data() {
    return {
      loading: false,
      syncInProgress: false,
      previewData: null,
      hostsContent: '',
      syncResult: null,
      taskId: null,
      taskStatus: null,
      percentage: 0,
      completedCount: 0,
      failedCount: 0,
      currentHost: null,
      taskMessage: null,
      completedHosts: [],
      failedHosts: {},
      pollingTimer: null
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.resetState();
        this.generatePreview()
      } else {
        this.clearPollingTimer();
      }
    }
  },
  beforeDestroy() {
    this.clearPollingTimer();
  },
  methods: {
    // 重置状态
    resetState() {
      this.previewData = null;
      this.hostsContent = '';
      this.syncResult = null;
      this.taskId = null;
      this.taskStatus = null;
      this.percentage = 0;
      this.completedCount = 0;
      this.failedCount = 0;
      this.currentHost = null;
      this.taskMessage = null;
      this.completedHosts = [];
      this.failedHosts = {};
    },
    
    // 清除轮询定时器
    clearPollingTimer() {
      if (this.pollingTimer) {
        clearInterval(this.pollingTimer);
        this.pollingTimer = null;
      }
    },
    
    // 辅助方法：通过IP和主机名格式化hosts文件行
    formatHostsFile(content) {
      if (!content) return '';
      
      // 分行处理
      const lines = content.split('\n');
      const formattedLines = lines.map(line => {
        // 处理注释行
        if (line.trim().startsWith('#')) {
          return `<span class="comment">${this.escapeHtml(line)}</span>`;
        }
        
        // 处理IP和主机名
        const parts = line.trim().split(/\s+/);
        if (parts.length >= 2 && this.isIPAddress(parts[0])) {
          const ip = `<span class="ip">${this.escapeHtml(parts[0])}</span>`;
          // 使用空格分隔，避免任何特殊字符导致解析错误
          const hostnames = parts.slice(1).map(h => 
            `<span class="hostname">${this.escapeHtml(h)}</span>`
          ).join(' ');
          
          // 使用HTML空格字符实体作为分隔符
          return `${ip}<span class="separator">&nbsp;&nbsp;&nbsp;&nbsp;</span>${hostnames}`;
        }
        
        // 其他行保持原样
        return this.escapeHtml(line);
      });
      
      // 将所有行连接起来
      return `<div class="hosts-code-content">${formattedLines.join('<br>')}</div>`;
    },
    
    // 辅助方法：判断是否为IP地址
    isIPAddress(str) {
      return /^(\d{1,3}\.){3}\d{1,3}$/.test(str);
    },
    
    // 转义HTML字符
    escapeHtml(unsafe) {
      return unsafe
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
    },
    
    // 生成hosts文件预览
    async generatePreview() {
      try {
        this.loading = true
        this.syncResult = null
        
        const res = await HostCheckService.generateHostsFilePreview(this, this.clusterId)
        
        if (res.code === 200) {
          this.previewData = res.data
          this.hostsContent = res.data.hostsContent
        } else {
          this.$message.error(res.msg || this.$t('生成预览失败'))
        }
      } catch (e) {
        console.error('Generate preview error:', e)
        this.$message.error(this.$t('生成预览失败'))
      } finally {
        this.loading = false
      }
    },
    
    // 开始轮询任务进度
    startPollingTaskProgress(taskId) {
      this.clearPollingTimer();
      this.taskId = taskId;
      
      // 立即执行一次
      this.pollTaskProgress();
      
      // 每1秒轮询一次
      this.pollingTimer = setInterval(() => {
        this.pollTaskProgress();
      }, 1000);
    },
    
    // 轮询任务进度
    async pollTaskProgress() {
      if (!this.taskId) return;
      
      try {
        const res = await HostCheckService.getTaskProgress(this, this.taskId);
        
        if (res.code === 200) {
          const progress = res.data;
          
          // 更新任务状态
          this.taskStatus = progress.status;
          this.completedCount = progress.completedCount;
          this.failedCount = progress.failedCount;
          this.percentage = progress.percentage;
          this.currentHost = progress.currentHost;
          this.taskMessage = progress.message;
          
          if (progress.completedHosts) {
            this.completedHosts = progress.completedHosts;
          }
          
          if (progress.failedHosts) {
            this.failedHosts = progress.failedHosts;
          }
          
          // 如果任务已完成，停止轮询
          if (progress.status === 'COMPLETED' || progress.status === 'FAILED') {
            this.clearPollingTimer();
          }
        } else {
          console.error('Poll task progress error:', res.msg);
          // 尝试5次后如果仍然失败，停止轮询
          this.failCount = (this.failCount || 0) + 1;
          if (this.failCount >= 5) {
            this.clearPollingTimer();
            this.taskStatus = 'FAILED';
            this.taskMessage = res.msg || this.$t('获取任务进度失败');
          }
        }
      } catch (e) {
        console.error('Poll task progress error:', e);
      }
    },

    // 同步hosts文件到所有主机
    async handleSync() {
      try {
        this.syncInProgress = true
        
        const res = await HostCheckService.syncHostsFile(this, this.clusterId)
        
        if (res.code === 200) {
          // 开始轮询任务进度
          this.startPollingTaskProgress(res.data);
        } else {
          this.$message.error(res.msg || this.$t('同步hosts文件失败'))
        }
      } catch (e) {
        console.error('Sync hosts error:', e)
        this.$message.error(this.$t('同步hosts文件失败'))
      } finally {
        this.syncInProgress = false
      }
    },
    
    // 处理成功完成
    handleSuccess() {
      this.$emit('success');
      this.$emit('close');
    },

    // 取消
    handleCancel() {
      this.$emit('close')
    },

    // 获取行数
    getLineCount() {
      if (this.previewData && this.previewData.hostsEntries) {
        return this.previewData.hostsEntries.length;
      }
      return 1;
    },

    // 判断是否有结构化数据
    hasStructuredData() {
      return this.previewData && this.previewData.hostsEntries;
    },

    // 格式化主机名
    formatHostnames(hostnames) {
      if (!hostnames || !Array.isArray(hostnames)) return '';
      return hostnames.join(' ');
    }
  }
}
</script>

<style lang="less" scoped>
.sync-hosts-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.feature-description {
  display: flex;
  align-items: flex-start;
  background-color: #f5f5f7;
  border-radius: 12px;
  padding: 16px;
}

.description-icon {
  color: #0071e3;
  font-size: 22px;
  margin-right: 12px;
  margin-top: 2px;
}

.description-content {
  flex: 1;
}

.description-title {
  font-weight: 600;
  margin-bottom: 8px;
  font-size: 16px;
  color: #1d1d1f;
}

.description-text {
  color: #6e6e73;
  line-height: 1.5;
  font-size: 14px;
}

.hosts-card-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.hosts-preview-card {
  background-color: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.card-title {
  font-weight: 600;
  margin-bottom: 16px;
  font-size: 16px;
  color: #1d1d1f;
  position: relative;
}

.card-title:after {
  content: '';
  position: absolute;
  bottom: -8px;
  left: 0;
  width: 40px;
  height: 2px;
  background-color: #0071e3;
}

.preview-header {
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.preview-subtitle {
  font-weight: 500;
  font-size: 15px;
  color: #1d1d1f;
}

.preview-info {
  font-size: 13px;
}

/* IDE 风格代码编辑器 */
.hosts-file-container {
  border-radius: 8px;
  overflow: hidden;
  background-color: #1e1e1e;
  border: 1px solid rgba(0, 0, 0, 0.1);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.modern-ide {
  display: flex;
  flex-direction: column;
  height: 100%;
  font-family: 'SF Mono', 'Menlo', 'Monaco', 'Courier New', monospace;
}

.ide-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background-color: #252526;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  height: 30px;
}

.ide-breadcrumb {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #8e8e93;
}

.breadcrumb-item {
  display: flex;
  align-items: center;
  color: #8e8e93;
}

.breadcrumb-item.root {
  padding-right: 4px;
}

.breadcrumb-item.active {
  color: #ffffff;
  font-weight: 500;
}

.breadcrumb-separator {
  margin: 0 4px;
  color: #8e8e93;
}

.ide-editor {
  display: flex;
  background-color: #1e1e1e;
  min-height: 300px;
  max-height: 350px;
  overflow: auto;
}

.ide-sidebar {
  background-color: #252526;
  padding: 4px 0;
  width: 50px;
  flex-shrink: 0;
  overflow: hidden;
}

.gutter-container {
  display: flex;
  width: 100%;
  height: 100%;
}

.gutter-line-numbers {
  width: 100%;
  text-align: right;
  padding-right: 10px;
  color: #8e8e93;
  font-size: 12px;
  line-height: 20px;
  user-select: none;
}

.line-number {
  color: #858585;
}

.code-container {
  flex: 1;
  overflow: auto;
  padding: 4px 0;
  font-size: 12px;
  line-height: 20px;
  white-space: pre;
}

.code-content {
  padding: 0 12px;
  min-height: 100%;
}

.hosts-code-content {
  color: #d4d4d4;
}

.hosts-line {
  line-height: 20px;
  white-space: pre;
}

.separator {
  display: inline-block;
  min-width: 24px;
}

.comment {
  color: #6A9955;
}

.ip {
  color: #4ec9b0;
  font-weight: bold;
}

.hostname {
  color: #9cdcfe;
}

.empty-content {
  padding: 12px;
  color: #858585;
  font-style: italic;
}

.ide-statusbar {
  display: flex;
  justify-content: space-between;
  padding: 4px 12px;
  background-color: #007acc;
  color: #ffffff;
  font-size: 12px;
  height: 24px;
}

.statusbar-left, .statusbar-right {
  display: flex;
  align-items: center;
}

.status-item {
  display: flex;
  align-items: center;
  margin-right: 16px;
}

.status-item svg, .status-item .anticon {
  margin-right: 4px;
}

.sync-result-container {
  background-color: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.sync-result-summary {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.result-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 24px;
  border-radius: 12px;
  flex: 1;
}

.result-stat.success {
  background-color: rgba(52, 199, 89, 0.1);
}

.result-stat.success .anticon {
  color: #34c759;
  font-size: 28px;
  margin-bottom: 8px;
}

.result-stat.failed {
  background-color: rgba(255, 59, 48, 0.1);
}

.result-stat.failed .anticon {
  color: #ff3b30;
  font-size: 28px;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 4px;
  color: #1d1d1f;
}

.stat-label {
  font-size: 14px;
  color: #6e6e73;
}

.failed-hosts-collapse {
  border: none;
  background-color: transparent;
}

.failed-hosts-collapse /deep/ .ant-collapse-header {
  padding: 12px 16px !important;
  background-color: #f5f5f7;
  border-radius: 8px !important;
  font-weight: 500;
  color: #1d1d1f !important;
}

.failed-hosts-collapse /deep/ .ant-collapse-content {
  border-top: none;
}

.failed-hosts-list {
  padding: 12px 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.failed-host-item {
  padding: 12px;
  border-radius: 8px;
  background-color: #f5f5f7;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.failed-host-ip {
  font-weight: 500;
  color: #1d1d1f;
  font-family: "SF Mono", "Consolas", "Monaco", monospace;
  font-size: 14px;
}

.failed-host-reason {
  color: #ff3b30;
  font-size: 13px;
}

.hosts-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}

.sync-button {
  background-color: #0071e3;
  border-color: #0071e3;
  border-radius: 8px;
  padding: 0 20px;
  height: 38px;
  font-weight: 500;
}

.sync-button:hover,
.sync-button:focus {
  background-color: #0077ED;
  border-color: #0077ED;
}

.progress-card {
  background-color: rgba(255, 255, 255, 0.8);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.08);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
}

.card-title {
  font-weight: 600;
  margin-bottom: 20px;
  font-size: 18px;
  color: #1d1d1f;
  position: relative;
}

.card-title:after {
  content: '';
  position: absolute;
  bottom: -8px;
  left: 0;
  width: 40px;
  height: 2px;
  background-color: #0071e3;
}

.progress-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.progress-status {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.status-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.status-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 500;
}

.status-icon {
  font-size: 20px;
}

.in-progress {
  color: #0071e3;
}

.completed {
  color: #34c759;
}

.failed {
  color: #ff3b30;
}

.status-stats {
  display: flex;
  gap: 24px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 14px;
  border-radius: 10px;
  transition: all 0.3s ease;
}

.stat-item.completed {
  background-color: rgba(52, 199, 89, 0.1);
}

.stat-item.failed {
  background-color: rgba(255, 59, 48, 0.1);
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 4px;
}

.stat-item.completed .stat-value {
  color: #34c759;
}

.stat-item.failed .stat-value {
  color: #ff3b30;
}

.stat-label {
  font-size: 14px;
  color: #6e6e73;
}

.current-host {
  margin-top: 16px;
}

/deep/ .ant-tag {
  border-radius: 6px;
  font-size: 13px;
  padding: 4px 10px;
  border: none;
  font-weight: 500;
}

.task-message {
  background-color: rgba(245, 247, 250, 0.7);
  padding: 16px;
  border-radius: 12px;
  color: #6e6e73;
  font-size: 14px;
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  animation: fadeIn 0.3s ease-in-out;
}

.hosts-collapse {
  border: none;
  background-color: transparent;
}

/deep/ .apple-collapse-panel {
  border: none !important;
  border-radius: 12px !important;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1.0);
}

/deep/ .apple-collapse-panel.error-panel .ant-collapse-header {
  background-color: rgba(255, 59, 48, 0.08);
  color: #1d1d1f;
}

/deep/ .ant-collapse-header {
  padding: 14px 16px !important;
  background-color: rgba(0, 113, 227, 0.08);
  border-radius: 12px !important;
  font-weight: 500;
  color: #1d1d1f !important;
  display: flex;
  align-items: center;
  transition: all 0.3s ease;
}

/deep/ .ant-collapse-header:hover {
  background-color: rgba(0, 113, 227, 0.12);
}

/deep/ .ant-collapse-arrow {
  font-size: 14px !important;
  color: #0071e3 !important;
  transition: transform 0.3s cubic-bezier(0.25, 0.1, 0.25, 1.0) !important;
}

/deep/ .ant-collapse-item-active .ant-collapse-header {
  border-bottom-left-radius: 0 !important;
  border-bottom-right-radius: 0 !important;
}

/deep/ .ant-collapse-content {
  border-top: none;
  background-color: rgba(250, 250, 252, 0.8);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  animation: slideDown 0.3s cubic-bezier(0.25, 0.1, 0.25, 1.0);
}

/deep/ .ant-collapse-content-box {
  padding: 16px !important;
}

.hosts-list {
  padding: 12px 0;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.host-tag {
  padding: 6px 12px;
  border-radius: 8px;
  font-family: "SF Mono", "Consolas", "Monaco", monospace;
  letter-spacing: 0.3px;
  transition: all 0.3s ease;
  border: none;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.success-host-tag {
  background-color: rgba(52, 199, 89, 0.15);
  color: #116329;
}

.success-host-tag:hover {
  background-color: rgba(52, 199, 89, 0.25);
  transform: translateY(-1px);
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.08);
}

.failed-hosts-list {
  padding: 12px 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.failed-host-item {
  padding: 14px;
  border-radius: 10px;
  background-color: rgba(255, 59, 48, 0.08);
  border: 1px solid rgba(255, 59, 48, 0.15);
  display: flex;
  flex-direction: column;
  gap: 8px;
  transition: all 0.3s ease;
}

.failed-host-item:hover {
  background-color: rgba(255, 59, 48, 0.12);
  transform: translateY(-1px);
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.06);
}

.failed-host-ip {
  font-weight: 500;
  color: #1d1d1f;
  font-family: "SF Mono", "Consolas", "Monaco", monospace;
  font-size: 14px;
}

.failed-host-reason {
  color: #ff3b30;
  font-size: 13px;
}

.progress-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

.cancel-button {
  border-radius: 10px;
  font-size: 14px;
  height: 38px;
  padding: 0 18px;
  border: none;
  background-color: rgba(242, 242, 242, 0.9);
  color: #1d1d1f;
  font-weight: 500;
  transition: all 0.3s ease;
}

.cancel-button:hover {
  background-color: rgba(230, 230, 230, 0.9);
  transform: translateY(-1px);
}

.sync-button {
  border-radius: 10px;
  font-size: 14px;
  height: 38px;
  padding: 0 18px;
  border: none;
  background-color: #0071e3;
  color: white;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 2px 6px rgba(0, 113, 227, 0.3);
}

.sync-button:hover {
  background-color: #0077ED;
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(0, 113, 227, 0.4);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slideDown {
  from {
    opacity: 0;
    max-height: 0;
  }
  to {
    opacity: 1;
    max-height: 1000px;
  }
}
</style>

<style>
/* 全局样式，使用苹果风格 */
.sync-hosts-modal .ant-modal-content {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.12);
}

.sync-hosts-modal .ant-modal-header {
  background-color: #ffffff;
  border-bottom: none;
  padding: 24px 24px 0;
}

.sync-hosts-modal .ant-modal-title {
  font-weight: 600;
  font-size: 20px;
  color: #1d1d1f;
  text-align: center;
}

.sync-hosts-modal .ant-modal-body {
  padding: 24px;
  background-color: #ffffff;
}

.sync-hosts-modal .ant-form-item-label label {
  color: #1d1d1f;
  font-weight: 500;
  font-size: 14px;
}

.sync-hosts-modal .ant-select-selection,
.sync-hosts-modal .ant-input,
.sync-hosts-modal .ant-input-number {
  border-radius: 8px;
  padding: 8px 12px;
  height: auto;
  border-color: #d2d2d7;
  transition: all 0.3s cubic-bezier(0.645, 0.045, 0.355, 1);
}

.sync-hosts-modal .ant-select-selection:hover,
.sync-hosts-modal .ant-input:hover,
.sync-hosts-modal .ant-input-number:hover {
  border-color: #0071e3;
}

.sync-hosts-modal .ant-select-focused .ant-select-selection,
.sync-hosts-modal .ant-input:focus,
.sync-hosts-modal .ant-input-number-focused {
  border-color: #0071e3;
  box-shadow: 0 0 0 2px rgba(0, 113, 227, 0.2);
}

.sync-hosts-modal .ant-btn {
  border-radius: 8px;
  font-size: 14px;
  height: 38px;
  padding: 0 18px;
  transition: all 0.3s cubic-bezier(0.645, 0.045, 0.355, 1);
}

.sync-hosts-modal .ant-select-dropdown {
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.sync-hosts-modal .ant-select-dropdown-menu-item {
  padding: 10px 12px;
  transition: all 0.2s;
}

.sync-hosts-modal .ant-select-dropdown-menu-item:hover {
  background-color: #f5f5f7;
}

.sync-hosts-modal .ant-select-dropdown-menu-item-selected {
  color: #0071e3;
  background-color: rgba(0, 113, 227, 0.05);
  font-weight: 500;
}

/* 修复hosts代码编辑区域的滚动样式 */
.sync-hosts-modal .code-container::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.sync-hosts-modal .code-container::-webkit-scrollbar-track {
  background: transparent;
}

.sync-hosts-modal .code-container::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
}

.sync-hosts-modal .code-container::-webkit-scrollbar-thumb:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

.sync-hosts-modal .ide-editor::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.sync-hosts-modal .ide-editor::-webkit-scrollbar-track {
  background: transparent;
}

.sync-hosts-modal .ide-editor::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
}

.sync-hosts-modal .ide-editor::-webkit-scrollbar-thumb:hover {
  background-color: rgba(255, 255, 255, 0.2);
}
</style> 