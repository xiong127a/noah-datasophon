<template>
  <div>
    <!-- 日志查看弹窗 -->
    <a-modal
      v-model="logVisible"
      :title="logModalTitle"
      width="80%"
      :footer="null"
      @cancel="closeLogModal"
      class="apple-log-modal"
      :bodyStyle="{ padding: '0' }"
    >
      <div class="apple-log-container">
        <div class="apple-log-header">
          <!-- 顶部工具栏 -->
          <div class="apple-toolbar">
            <div class="toolbar-left">
              <a-button @click="refreshLog" class="apple-button refresh-button">
                <div class="button-content">
                  <div class="icon-container">
                    <a-icon type="reload" :style="{opacity: logLoading ? 0 : 1}" />
                    <div class="mini-loader" :style="{opacity: logLoading ? 1 : 0}"></div>
                  </div>
                  <span>刷新</span>
                </div>
              </a-button>
              
              <a-dropdown>
                <a-button :type="autoRefreshInterval > 0 ? 'primary' : 'default'" class="apple-button refresh-interval-button">
                  <a-icon :type="autoRefreshInterval > 0 ? 'sync' : 'clock-circle'" :spin="autoRefreshInterval > 0" />
                  <span v-if="autoRefreshInterval === 0">自动刷新</span>
                  <span v-else>{{autoRefreshInterval}}秒刷新</span>
                  <a-icon type="down" />
                </a-button>
                <a-menu slot="overlay" @click="handleAutoRefreshChange" class="apple-dropdown-menu">
                  <a-menu-item key="0">
                    <a-icon type="stop" />关闭自动刷新
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="1">
                    <a-icon type="sync" />每秒刷新
                  </a-menu-item>
                  <a-menu-item key="3">
                    <a-icon type="sync" />每3秒刷新
                  </a-menu-item>
                  <a-menu-item key="5">
                    <a-icon type="sync" />每5秒刷新
                  </a-menu-item>
                  <a-menu-item key="10">
                    <a-icon type="sync" />每10秒刷新
                  </a-menu-item>
                </a-menu>
              </a-dropdown>
            </div>
          </div>
          
          <!-- 过滤工具栏 -->
          <div class="apple-filter-toolbar">
            <div class="filter-section">
              <div class="filter-group">
                <h3 class="filter-label">日志类型</h3>
                <div class="segmented-control log-type-control">
                  <a-radio-group v-model="currentLogType" @change="handleLogTypeChange" button-style="solid" class="apple-segmented-control">
                    <a-radio-button value="all" class="segment-button">全部日志</a-radio-button>
                    <a-radio-button value="check" class="segment-button">检查日志</a-radio-button>
                    <a-radio-button value="fix" class="segment-button">修复日志</a-radio-button>
                  </a-radio-group>
                </div>
              </div>
              
              <!-- 日志级别筛选组件 -->
              <div class="filter-group" v-if="checkItem && checkItem.clusterId && showLogFilterOptions">
                <log-filter
                  ref="logFilter"
                  :clusterId="checkItem.clusterId"
                  :hostname="checkItem.ip"
                  :itemId="checkItem.id"
                  v-model="logContent"
                  hide-reset-button
                  @filter-change="handleFilterChange"
                ></log-filter>
              </div>
            </div>
          </div>
          
          <!-- 当前筛选状态指示条 -->
          <div class="apple-filter-status">
            <div class="status-pill">
              <a-icon type="filter" />
              <span>
                类型: <span class="status-value">{{ currentLogType === 'all' ? '全部' : (currentLogType === 'check' ? '检查' : '修复') }}</span>
              </span>
            </div>
            
            <div class="status-pill" v-if="checkItem && $refs.logFilter">
              <a-icon type="info-circle" />
              <span>
                级别: 
                <span class="status-value" v-if="$refs.logFilter.filterType === 'exact'">{{ $refs.logFilter.selectedLevel }}</span>
                <span class="status-value" v-else-if="$refs.logFilter.filterType === 'min'">{{ $refs.logFilter.selectedLevel }}及以上</span>
                <span class="status-value" v-else>全部</span>
              </span>
            </div>
            
            <div class="flex-spacer"></div>
            
            <!-- 日志统计信息 -->
            <div class="log-stats">
              <div class="status-pill">
                <a-icon type="file-text" />
                <span>共 <span class="status-value">{{ totalLogCount }}</span> 条日志</span>
              </div>
              
              <div class="status-pill" v-if="errorLogCount > 0">
                <a-icon type="warning" style="color: #ff3b30;" />
                <span><span class="status-value">{{ errorLogCount }}</span> 条错误</span>
              </div>
              
              <div class="status-pill" v-if="warnCount > 0">
                <a-icon type="exclamation-circle" style="color: #ff9500;" />
                <span><span class="status-value">{{ warnCount }}</span> 条警告</span>
              </div>
              
              <div class="status-pill" v-if="infoLogCount > 0">
                <a-icon type="info-circle" style="color: #0071e3;" />
                <span><span class="status-value">{{ infoLogCount }}</span> 条信息</span>
              </div>
              
              <div class="status-pill" v-if="debugCount > 0">
                <a-icon type="bug" style="color: #5856d6;" />
                <span><span class="status-value">{{ debugCount }}</span> 条调试</span>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 日志内容区域 -->
        <div class="apple-log-content" :class="{'apple-loading': logLoading}">
          <div v-if="logLoading" class="apple-loading-indicator">
            <div class="apple-spinner"></div>
            <span>正在加载...</span>
          </div>
          <pre v-html="logContent" class="apple-log-text"></pre>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script>
import LogFilter from './LogFilter.vue';

export default {
  name: 'AppleLogViewer',
  components: {
    LogFilter
  },
  props: {
    clusterId: {
      type: Number,
      required: true
    },
    visible: {
      type: Boolean,
      default: false
    },
    title: {
      type: String,
      default: '日志查看'
    },
    hostIp: {
      type: String,
      default: ''
    },
    itemId: {
      type: Number,
      default: null
    },
    itemName: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      logVisible: false,
      logModalTitle: '',
      logContent: '',
      logLoading: false,
      autoRefreshInterval: 1,
      refreshTimer: null,
      currentLogType: 'all',
      showLogFilterOptions: true,
      checkItem: null,
      currentLogIp: '',
      currentLogItemId: null,
      currentLogItemName: '',
      totalLogCount: 0,
      errorLogCount: 0,
      infoLogCount: 0,
      warnCount: 0,
      debugCount: 0
    };
  },
  watch: {
    visible(val) {
      this.logVisible = val;
      if (val) {
        this.initLogViewer();
      } else {
        this.closeLogModal();
      }
    },
    hostIp(val) {
      if (val && this.logVisible) {
        this.currentLogIp = val;
        this.updateCheckItem();
      }
    },
    itemId(val) {
      if (val && this.logVisible) {
        this.currentLogItemId = val;
        this.updateCheckItem();
      }
    },
    itemName(val) {
      if (val && this.logVisible) {
        this.currentLogItemName = val;
        this.updateLogTitle();
      }
    },
    logVisible(val) {
      this.$emit('update:visible', val);
    }
  },
  methods: {
    initLogViewer() {
      // 设置初始值
      this.currentLogIp = this.hostIp;
      this.currentLogItemId = this.itemId;
      this.currentLogItemName = this.itemName;
      
      // 更新标题和检查项信息
      this.updateLogTitle();
      this.updateCheckItem();
      
      // 设置初始日志类型为全部日志
      this.currentLogType = 'all';
      
      // 初始停止之前可能存在的自动刷新定时器
      this.stopAutoRefresh();
      
      // 等待DOM更新完成后设置初始筛选条件
      this.$nextTick(() => {
        if (this.$refs.logFilter) {
          // 设置默认筛选条件
          this.$refs.logFilter.filterType = 'min';
          this.$refs.logFilter.selectedLevel = 'INFO';
          // 手动触发筛选
          this.$refs.logFilter.applyFilter();
        }
        // 获取日志数据
        this.fetchItemLog();
        
        // 启动自动刷新
        this.startAutoRefresh();
      });
    },
    
    updateLogTitle() {
      this.logModalTitle = `日志 - 主机: ${this.currentLogIp}, 检查项: ${this.currentLogItemName}`;
    },
    
    updateCheckItem() {
      this.checkItem = {
        clusterId: this.clusterId,
        ip: this.currentLogIp,
        id: this.currentLogItemId,
        itemName: this.currentLogItemName
      };
    },
    
    // 获取检查项日志
    async fetchItemLog() {
      if (!this.currentLogIp || !this.currentLogItemId) {
        return;
      }

      this.logLoading = true;

      try {
        // 统一使用一个API进行所有筛选
        const apiUrl = '/ddh/host/check/getLog';

        // 获取级别筛选参数
        let logLevel = 'INFO'; // 默认显示INFO级别
        let filterMode = 'min'; // 默认显示INFO及以上级别

        if (this.$refs.logFilter) {
          filterMode = this.$refs.logFilter.filterType;
          logLevel = this.$refs.logFilter.selectedLevel;
        }

        // 准备请求参数
        const params = {
          clusterId: this.clusterId,
          ip: this.currentLogIp,
          itemId: this.currentLogItemId,
          logType: this.currentLogType,
          logLevel: logLevel,
          filterMode: filterMode
        };

        const res = await this.$axiosPost(apiUrl, params);

        if (res.code === 200) {
          const logContentEl = this.$el.querySelector('.apple-log-content');
          const wasScrolledToBottom = logContentEl && (
            Math.abs(
              (logContentEl.scrollHeight - logContentEl.scrollTop) - 
              logContentEl.clientHeight
            ) < 10
          );
          
          // 响应中可能包含data和logStats两个字段
          if (res.data && typeof res.data === 'object') {
            // 新的接口结构：res.data包含logContent和logStats字段
            this.logContent = res.data.logContent || '暂无日志数据';
            
            // 使用后端返回的统计数据更新日志统计信息
            if (res.data.logStats) {
              this.totalLogCount = res.data.logStats.total || 0;
              this.errorLogCount = res.data.logStats.error || 0;
              this.infoLogCount = res.data.logStats.info || 0;
              this.warnCount = res.data.logStats.warn || 0;
              this.debugCount = res.data.logStats.debug || 0;
            } else {
              // 如果没有logStats字段，通过内容计算
              this.calculateLogStats(this.logContent);
            }
          } else {
            // 旧接口结构：res.data直接是HTML内容
            this.logContent = res.data || '暂无日志数据';
            
            // 移除后端返回的日志统计区域（兼容旧版本）
            this.logContent = this.removeLogSummary(this.logContent);
            
            // 计算日志统计信息
            this.calculateLogStats(this.logContent);
          }
          
          // 在内容更新后，如果之前是在底部，则滚动到底部
          this.$nextTick(() => {
            if (wasScrolledToBottom) {
              this.scrollToBottom();
            }
          });
        } else {
          this.logContent = `获取日志失败: ${res.msg || '未知错误'}`;
          if (this.autoRefreshInterval > 0) {
            this.stopAutoRefresh(); // 如果获取失败，停止自动刷新
            this.$message.error('日志获取失败，已停止自动刷新');
          }
        }
      } catch (error) {
        console.error('获取日志失败:', error);
        this.logContent = '获取日志失败，请稍后重试';
        if (this.autoRefreshInterval > 0) {
          this.stopAutoRefresh(); // 如果获取失败，停止自动刷新
          this.$message.error('日志获取失败，已停止自动刷新');
        }
      } finally {
        this.logLoading = false;
      }
    },

    // 计算日志统计信息
    calculateLogStats(logContent) {
      if (!logContent || logContent === '暂无日志数据') {
        this.totalLogCount = 0;
        this.errorLogCount = 0;
        this.infoLogCount = 0;
        this.warnCount = 0;
        this.debugCount = 0;
        return;
      }
      
      // 将HTML转为纯文本
      const div = document.createElement('div');
      div.innerHTML = logContent;
      const plainText = div.textContent || div.innerText || '';
      
      // 按行分割，忽略空行
      const lines = plainText.split('\n').filter(line => line.trim());
      this.totalLogCount = lines.length;
      
      // 重置计数
      this.errorLogCount = 0;
      this.infoLogCount = 0;
      this.warnCount = 0;
      this.debugCount = 0;
      
      // 统计不同级别的日志
      lines.forEach(line => {
        const lowerLine = line.toLowerCase();
        // 尝试匹配常见的错误日志模式
        if (
          lowerLine.includes('error') || 
          lowerLine.includes('exception') || 
          lowerLine.includes('fatal') || 
          lowerLine.includes('failure') || 
          lowerLine.includes('failed')
        ) {
          this.errorLogCount++;
        } 
        // 尝试匹配信息日志模式
        else if (
          lowerLine.includes('info') || 
          lowerLine.includes('information') ||
          (!lowerLine.includes('warn') && !lowerLine.includes('debug'))
        ) {
          this.infoLogCount++;
        }
        else if (
          lowerLine.includes('warn')
        ) {
          this.warnCount++;
        }
        else if (
          lowerLine.includes('debug')
        ) {
          this.debugCount++;
        }
      });
    },

    // 手动刷新日志
    refreshLog() {
      this.fetchItemLog();
    },

    // 处理自动刷新间隔变化
    handleAutoRefreshChange(e) {
      const value = parseInt(e.key);

      // 停止之前的自动刷新
      this.stopAutoRefresh();

      // 设置新的自动刷新间隔
      this.autoRefreshInterval = value;

      // 如果选择了自动刷新，启动定时器
      if (value > 0) {
        this.$message.success(`已开启自动刷新(${value}秒)`);
        this.startAutoRefresh();
      } else {
        this.$message.info('已关闭自动刷新');
      }
    },

    // 启动自动刷新
    startAutoRefresh() {
      if (this.autoRefreshInterval > 0) {
        this.refreshTimer = setInterval(() => {
          this.fetchItemLog();
        }, this.autoRefreshInterval * 1000);
      }
    },

    // 停止自动刷新
    stopAutoRefresh() {
      if (this.refreshTimer) {
        clearInterval(this.refreshTimer);
        this.refreshTimer = null;
      }
    },

    // 当日志类型变化时，重新获取日志
    handleLogTypeChange() {
      // 随着系统扩展，如果添加了新的日志类型，在这里不需要特殊处理
      // 只需要：
      // 1. 在上面的日志类型选择器中添加新的a-radio-button
      // 2. 确保后端API能够处理新的日志类型参数
      // 3. 确保HostCheckServiceImpl.getCheckItemLogWithType方法支持新的日志类型

      // 直接刷新日志以应用新的筛选条件
      this.fetchItemLog();
    },

    // 处理日志级别筛选变化
    handleFilterChange() {
      this.fetchItemLog();
    },

    // 关闭日志查看弹窗
    closeLogModal() {
      this.stopAutoRefresh();
      this.logVisible = false;
      this.$emit('close');
    },

    // 滚动到日志底部
    scrollToBottom() {
      const logContent = this.$el.querySelector('.apple-log-content');
      if (logContent) {
        logContent.scrollTop = logContent.scrollHeight;
      }
    },

    // 移除后端返回的日志统计区域
    removeLogSummary(logContent) {
      if (!logContent || logContent === '暂无日志数据') {
        return logContent;
      }
      
      // 移除log-summary div
      return logContent.replace(/<div class="log-summary"[^>]*>[\s\S]*?<\/div>/, '');
    }
  },
  beforeDestroy() {
    // 清理日志刷新定时器
    this.stopAutoRefresh();
  }
}
</script>

<style lang="less" scoped>
/* 苹果风格变量 */
@apple-font: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Helvetica Neue", Arial, sans-serif;
@apple-bg: #ffffff;
@apple-bg-secondary: #f5f5f7;
@apple-blue: #0071e3;
@apple-blue-light: rgba(0, 113, 227, 0.1);
@apple-text: #1d1d1f;
@apple-text-secondary: #86868b;
@apple-border: rgba(0, 0, 0, 0.1);
@apple-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
@apple-radius: 12px;
@apple-radius-small: 8px;
@apple-transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

/* 主容器样式 */
.apple-log-modal {
  font-family: @apple-font;
  
  :deep(.ant-modal-content) {
    border-radius: @apple-radius;
    overflow: hidden;
    box-shadow: 0 20px 50px rgba(0, 0, 0, 0.12);
  }
  
  :deep(.ant-modal-header) {
    background: @apple-bg;
    border-bottom: 1px solid @apple-border;
    padding: 16px 24px;
    
    .ant-modal-title {
      font-weight: 600;
      font-size: 16px;
      color: @apple-text;
    }
  }
  
  :deep(.ant-modal-close) {
    color: @apple-text-secondary;
    transition: @apple-transition;
    
    &:hover {
      color: @apple-text;
      background: rgba(0, 0, 0, 0.05);
    }
  }
}

.apple-log-container {
  display: flex;
  flex-direction: column;
  height: 70vh;
}

/* 头部样式 */
.apple-log-header {
  background: @apple-bg;
  padding: 0;
}

.apple-toolbar {
  display: flex;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid @apple-border;
  
  .toolbar-left {
    display: flex;
    gap: 8px;
    min-width: 240px;  /* 添加最小宽度，防止按钮移动 */
  }
}

.apple-button {
  height: 32px;
  padding: 0 16px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
  border: none;
  transition: @apple-transition;
  
  .button-content {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    width: 100%;
  }
  
  .icon-container {
    position: relative;
    width: 14px;
    height: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    
    .anticon {
      position: absolute;
      top: 0;
      left: 0;
      transition: opacity 0.3s;
    }
    
    .mini-loader {
      position: absolute;
      top: 0;
      left: 0;
      transition: opacity 0.3s;
    }
  }
  
  .mini-loader {
    width: 14px;
    height: 14px;
    border: 2px solid transparent;
    border-top-color: @apple-text;
    border-left-color: @apple-text;
    border-radius: 50%;
    animation: apple-spin 0.8s linear infinite;
  }
  
  &.refresh-button {
    width: 90px !important;  /* 设置固定宽度 */
    background: @apple-bg-secondary;
    color: @apple-text;
    position: relative !important; /* 添加相对定位 */
    box-sizing: border-box !important; /* 确保宽度包含padding和border */
    
    /* 确保按钮内容水平居中 */
    span {
      position: relative;
      display: inline-block;
      min-width: 36px; /* 给文字设置最小宽度 */
      text-align: center;
    }
    
    /* 加载状态时的样式 */
    :deep(.ant-btn-loading-icon) {
      position: absolute !important;
      left: 16px !important;
    }
    
    &:hover {
      background: darken(@apple-bg-secondary, 3%);
    }
    
    &:active {
      background: darken(@apple-bg-secondary, 6%);
    }
  }
  
  &.refresh-interval-button {
    width: 130px;  /* 设置固定宽度 */
    background: @apple-bg-secondary;
    color: @apple-text;
    
    &:hover {
      background: darken(@apple-bg-secondary, 3%);
    }
    
    &[ant-click-animating-without-extra-node]::after {
      display: none;
    }
  }
  
  &.ant-btn-primary {
    background: @apple-blue;
    color: white;
    
    &:hover {
      background: darken(@apple-blue, 5%);
    }
    
    &:active {
      background: darken(@apple-blue, 10%);
    }
  }
}

.apple-dropdown-menu {
  border-radius: @apple-radius-small;
  box-shadow: @apple-shadow;
  padding: 4px;
  
  :deep(.ant-dropdown-menu-item) {
    border-radius: 6px;
    padding: 8px 12px;
    transition: @apple-transition;
    
    &:hover {
      background: @apple-bg-secondary;
    }
  }
}

/* 过滤工具栏 */
.apple-filter-toolbar {
  padding: 16px 24px;
  border-bottom: 1px solid @apple-border;
  
  .filter-section {
    display: flex;
    flex-wrap: wrap;
    gap: 24px;
  }
  
  .filter-group {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  
  .filter-label {
    font-size: 13px;
    font-weight: 600;
    color: @apple-text-secondary;
    margin: 0;
  }
}

/* 分段控制 */
.apple-segmented-control {
  background: @apple-bg-secondary;
  padding: 2px;
  border-radius: @apple-radius-small;
  display: flex;
  
  .segment-button {
    flex: 1;
    height: 30px;
    line-height: 30px;
    text-align: center;
    border: none !important;
    border-radius: 6px !important;
    color: @apple-text;
    font-size: 14px;
    transition: @apple-transition;
    background: transparent !important;
    
    &:hover {
      background: rgba(0, 0, 0, 0.03) !important;
    }
    
    &[class*="-checked"] {
      background: white !important;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
      color: @apple-blue;
      font-weight: 500;
    }
  }
}

/* 筛选状态指示 */
.apple-filter-status {
  padding: 12px 24px;
  background: @apple-bg-secondary;
  display: flex;
  gap: 16px;
  
  .status-pill {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 4px 12px;
    background: white;
    border-radius: 16px;
    font-size: 13px;
    color: @apple-text-secondary;
    
    .anticon {
      font-size: 14px;
      color: @apple-blue;
    }
    
    .status-value {
      font-weight: 600;
      color: @apple-text;
    }
  }
  
  .flex-spacer {
    flex-grow: 1;
  }
  
  .log-stats {
    display: flex;
    gap: 8px;
    margin-left: auto;
  }
}

/* 日志内容区域 */
.apple-log-content {
  flex: 1;
  overflow: auto;
  padding: 20px;
  background: @apple-bg;
  position: relative;
  min-height: 300px; /* 设置最小高度 */
  will-change: transform; /* 提示浏览器这个元素会有变化，优化渲染 */
  
  &.apple-loading {
    opacity: 0.7;
  }
}

.apple-log-text {
  margin: 0;
  font-family: "SF Mono", "Menlo", "Monaco", "Consolas", monospace;
  font-size: 13px;
  line-height: 1.5;
  color: @apple-text;
  white-space: pre-wrap;
  word-break: break-word;
  position: relative; /* 添加相对定位 */
  z-index: 1; /* 确保在加载指示器上方 */
  
  :deep(.log-info) {
    color: @apple-text;
  }
  
  :deep(.log-warn) {
    color: #ff9500;
  }
  
  :deep(.log-error) {
    color: #ff3b30;
  }
  
  :deep(.log-debug) {
    color: #5856d6;
  }
  
  :deep(.log-timestamp) {
    color: @apple-blue;
    font-weight: 500;
  }
}

/* 加载指示器 */
.apple-loading-indicator {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: @apple-text-secondary;
}

.apple-spinner {
  width: 28px;
  height: 28px;
  border: 2px solid transparent;
  border-top-color: @apple-blue;
  border-left-color: @apple-blue;
  border-radius: 50%;
  animation: apple-spin 0.8s linear infinite;
}

@keyframes apple-spin {
  to {
    transform: rotate(360deg);
  }
}
</style> 