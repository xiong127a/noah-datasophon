<!--
 * 主机名悬浮卡片组件
 * 这个文件包含了主机名悬浮卡片的所有代码，用于在主机环境校验页面显示主机名信息
-->
<template>
  <div class="hostname-detail-tooltip">
    <div class="hostname-detail-popup">
      <!-- 标题区域 -->
      <div class="hostname-detail-header">
        <div class="hostname-detail-header-content">
          <div class="hostname-detail-icon-wrapper">
            <div class="hostname-detail-icon-container">
              <svg 
                viewBox="0 0 24 24" 
                width="36" 
                height="36" 
                stroke="#007AFF" 
                fill="none" 
                stroke-width="1.5"
                class="hostname-detail-icon"
              >
                <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
                <polyline points="9 22 9 12 15 12 15 22" />
              </svg>
            </div>
          </div>
          <div class="hostname-detail-info">
            <div class="hostname-detail-name">
              <!-- 主机名显示区域 -->
              <div class="hostname-name-wrapper">
                <!-- 主机名加载中状态 -->
                <div v-if="isLoading('hostname')" class="hostname-loading-container">
                  <div class="hostname-loading-dots">
                    <span class="hostname-loading-dot"></span>
                    <span class="hostname-loading-dot"></span>
                    <span class="hostname-loading-dot"></span>
                  </div>
                  <span class="hostname-loading-text">获取主机名</span>
                </div>
                <!-- 主机名已加载 -->
                <span v-else class="hostname-name">
                  {{ hostInfo.hostname || (hostInfo.osInfo && hostInfo.osInfo.hostname) || '未知主机名' }}
                </span>
                <span class="hostname-ip">{{ hostInfo.ip }}</span>
              </div>
            </div>
            <div class="hostname-detail-meta">
              <!-- FQDN字段 - 独立加载动画 -->
              <div class="hostname-meta-item">
                <span class="meta-label">FQDN</span>
                <!-- FQDN加载中状态 -->
                <div v-if="isLoading('fqdn')" class="fqdn-loading-container">
                  <div class="fqdn-loading-pulse"></div>
                  <span class="fqdn-loading-text">加载中...</span>
                </div>
                <!-- FQDN已加载 -->
                <span v-else class="meta-value">
                  {{ hostInfo.fqdn || (hostInfo.osInfo && hostInfo.osInfo.fqdn) || '无FQDN' }}
                </span>
              </div>
              <!-- 集群信息 -->
              <div class="hostname-meta-item" v-if="hostInfo.cluster">
                <span class="meta-label">集群</span>
                <span class="meta-value">{{ hostInfo.cluster }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="hostname-detail-header-blur"></div>
      </div>

      <!-- 内容区域包装元素 -->
      <div class="hostname-detail-content">
        <!-- 网络信息卡片 -->
        <div class="hostname-detail-section">
          <div class="section-header">
            <span class="section-title">网络信息</span>
          </div>

          <!-- DNS服务器信息 -->
          <div class="info-item" v-if="hostInfo.osInfo && hostInfo.osInfo.dnsInfo && hostInfo.osInfo.dnsInfo.servers && hostInfo.osInfo.dnsInfo.servers.length > 0">
            <div class="info-icon dns">
              <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none">
                <path d="M22 12h-4l-3 9L9 3l-3 9H2" />
              </svg>
            </div>
            <div class="info-content">
              <div class="info-header">
                <span class="info-title">DNS服务器</span>
              </div>
              <div class="info-data">
                <div class="dns-servers">
                  <div v-for="(dns, index) in formatDnsServers(hostInfo.osInfo.dnsInfo.servers)" :key="index" class="dns-server-item">
                    <div class="dns-server-badge">
                      <span class="dns-index">{{ index + 1 }}</span>
                      <span class="dns-ip">{{ dns }}</span>
                      <span class="dns-status" :class="{ 'active': isDnsActive(dns) }">{{ isDnsActive(dns) ? '活跃' : '备用' }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <!-- DNS服务器加载中 -->
          <div class="info-item" v-else-if="isLoading('dns')">
            <div class="info-icon dns">
              <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none">
                <path d="M22 12h-4l-3 9L9 3l-3 9H2" />
              </svg>
            </div>
            <div class="info-content">
              <div class="info-header">
                <span class="info-title">DNS服务器</span>
              </div>
              <div class="info-data">
                <div class="dns-servers-loading">
                  <div class="loading-shimmer">
                    <div class="loading-line short"></div>
                    <div class="loading-line medium"></div>
                    <div class="loading-line long"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="info-item no-dns" v-else>
            <div class="info-icon dns">
              <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none">
                <path d="M22 12h-4l-3 9L9 3l-3 9H2" />
              </svg>
            </div>
            <div class="info-content">
              <div class="info-header">
                <span class="info-title">DNS服务器</span>
              </div>
              <div class="info-data">
                <div class="info-empty">
                  <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none">
                    <circle cx="12" cy="12" r="10"></circle>
                    <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"></line>
                  </svg>
                  <span>未配置DNS服务器</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Hosts文件部分 -->
        <div class="hosts-file-section">
          <div class="section-header">
            <div class="section-title">
              <a-icon type="profile" />
              <span>hosts文件</span>
            </div>
            <div class="section-actions">
              <!-- 编辑按钮，仅在非加载状态且有hosts文件内容时显示 -->
              <a-tooltip title="编辑hosts文件" v-if="!isLoading('hosts') && hostInfo.osInfo && hostInfo.osInfo.dnsInfo && hostInfo.osInfo.dnsInfo.hostsFileContent">
                <a-icon type="edit" @click="startEditingHosts" />
              </a-tooltip>
              <!-- 复制按钮，仅在非加载状态且有hosts文件内容时显示 -->
              <a-tooltip title="复制hosts文件内容" v-if="!isLoading('hosts') && hostInfo.osInfo && hostInfo.osInfo.dnsInfo && hostInfo.osInfo.dnsInfo.hostsFileContent">
                <a-icon type="copy" @click="copyHostsContent" />
              </a-tooltip>
            </div>
          </div>

          <!-- hosts文件加载中的状态 -->
          <div class="hosts-loading-section" v-if="isLoading('hosts')">
            <div class="hosts-loading-container">
              <div class="toolbar">
                <div class="loading-line loading-line-small"></div>
                <div class="loading-line loading-line-medium"></div>
              </div>
              <div class="content">
                <a-spin />
                <span class="loading-text">正在获取hosts文件内容...</span>
              </div>
            </div>
          </div>

          <!-- hosts文件内容，当不在加载中且有hosts文件内容时显示 -->
          <div class="hosts-file-content" v-else-if="hostInfo.osInfo && hostInfo.osInfo.dnsInfo && hostInfo.osInfo.dnsInfo.hostsFileContent">
            <div class="search-bar">
              <a-input-search
                placeholder="搜索hosts内容"
                v-model="hostsSearchKeyword"
                @change="searchHosts"
                style="width: 100%"
              />
            </div>
            <div class="hosts-content" ref="hostsContent">
              <pre v-html="highlightedHostsContent"></pre>
            </div>
          </div>

          <!-- 没有hosts文件内容时的显示 -->
          <div class="no-hosts-content" v-else>
            <a-empty description="暂无hosts文件内容" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'HostnameFloatingCard',
  props: {
    hostInfo: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      showSearch: false,
      searchQuery: '',
      matchCount: null,
      matchIndex: 0,
      matches: [],
      hostsLines: [],
      editHostsVisible: false,
      hostsFileContent: '',
      hostsEditLoading: false,
      isEditingHosts: false,
      hostsSearchKeyword: '',
      highlightedHostsContent: '',
    };
  },
  methods: {
    // 检查状态，如果不存在或者是loading或pending则返回true
    checkStatus(status, matchValue = 'loading') {
      if (status === null || status === undefined) {
        // 当状态为null或undefined时，如果匹配loading或pending，则返回true
        return matchValue.toLowerCase() === 'loading' || matchValue.toLowerCase() === 'pending' || matchValue.toLowerCase() === 'collecting';
      }
      
      // 将collecting和pending视为loading状态
      if (matchValue.toLowerCase() === 'loading' && 
          (status.toLowerCase() === 'collecting' || status.toLowerCase() === 'pending')) {
        return true;
      }
      
      // 不区分大小写比较
      return String(status).toLowerCase() === String(matchValue).toLowerCase();
    },
    
    // 获取loading状态
    isLoading(type) {
      if (type === 'hostname') {
        // 判断hostname是否在加载中
        // 1. 如果hostname还没有获取到，但状态不是error，认为是加载中
        const isHostnameEmpty = !this.hostInfo.hostname || this.hostInfo.hostname.trim() === '';
        // 2. 如果没有明确指定不是loading状态，默认认为是加载中
        const hasNonLoadingStatus = this.hostInfo.hostnameStatus && 
                                   !this.checkStatus(this.hostInfo.hostnameStatus, 'loading') && 
                                   !this.checkStatus(this.hostInfo.hostnameStatus, 'pending') && 
                                   !this.checkStatus(this.hostInfo.hostnameStatus, 'collecting');
        
        // 如果hostname为空，且没有明确的非loading状态，认为是在加载中
        if (isHostnameEmpty && !this.checkStatus(this.hostInfo.hostnameStatus, 'error') && !hasNonLoadingStatus) {
          return true;
        }
        
        // 如果明确指定了loading状态，则显示loading
        return this.checkStatus(this.hostInfo.hostnameStatus, 'loading');
      } else if (type === 'fqdn') {
        // 判断fqdn是否在加载中 - 类似hostname的逻辑
        const isFqdnEmpty = !this.hostInfo.fqdn || this.hostInfo.fqdn.trim() === '';
        const hasNonLoadingStatus = this.hostInfo.fqdnStatus && 
                                   !this.checkStatus(this.hostInfo.fqdnStatus, 'loading') && 
                                   !this.checkStatus(this.hostInfo.fqdnStatus, 'pending') && 
                                   !this.checkStatus(this.hostInfo.fqdnStatus, 'collecting');
        
        if (isFqdnEmpty && !this.checkStatus(this.hostInfo.fqdnStatus, 'error') && !hasNonLoadingStatus) {
          return true;
        }
        return this.checkStatus(this.hostInfo.fqdnStatus, 'loading');
      } else if (type === 'dns') {
        // 判断dns服务器是否在加载中
        // 1. 如果没有dns服务器数据，默认认为是加载中，除非明确指定了error状态
        const isDnsEmpty = !this.hostInfo.osInfo || !this.hostInfo.osInfo.dnsInfo || !this.hostInfo.osInfo.dnsInfo.servers || this.hostInfo.osInfo.dnsInfo.servers.length === 0;
        const dnsStatus = this.hostInfo.osInfo && this.hostInfo.osInfo.dnsStatus;
        const hasNonLoadingStatus = dnsStatus && 
                                   !this.checkStatus(dnsStatus, 'loading') && 
                                   !this.checkStatus(dnsStatus, 'pending') && 
                                   !this.checkStatus(dnsStatus, 'collecting');
        
        if (isDnsEmpty && !this.checkStatus(dnsStatus, 'error') && !hasNonLoadingStatus) {
          return true;
        }
        return dnsStatus ? this.checkStatus(dnsStatus, 'loading') : true;
      } else if (type === 'hosts') {
        // 判断hosts文件是否在加载中
        const dnsStatus = this.hostInfo.osInfo && this.hostInfo.osInfo.dnsStatus;
        
        // 如果明确是loading状态，返回true
        if (dnsStatus && this.checkStatus(dnsStatus, 'loading')) {
          return true;
        }
        
        // 如果已经有hosts文件内容，则不是loading状态
        if (this.hostInfo.osInfo && 
            this.hostInfo.osInfo.dnsInfo && 
            this.hostInfo.osInfo.dnsInfo.hostsFileContent) {
          return false;
        }
        
        // 如果没有hosts文件内容且没有error状态，则可能是loading状态
        const isHostsEmpty = !this.hostInfo.osInfo || 
                            !this.hostInfo.osInfo.dnsInfo || 
                            !this.hostInfo.osInfo.dnsInfo.hostsFileContent;
        
        return isHostsEmpty && !this.checkStatus(dnsStatus, 'error');
      }
      return false;
    },
    // 格式化hosts文件内容，添加语法高亮
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
          const hostnames = parts.slice(1).map(h => `<span class="hostname">${this.escapeHtml(h)}</span>`).join(' ');
          return `${ip} ${hostnames}`;
        }
        
        // 其他行保持原样
        return this.escapeHtml(line);
      });
      
      // 为整个内容添加额外的类，以提高CSS选择器权重
      return `<div class="hosts-code-content">${formattedLines.join('\n')}</div>`;
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
    
    // 切换搜索框显示状态
    toggleSearch() {
      this.showSearch = !this.showSearch;
      if (this.showSearch) {
        this.$nextTick(() => {
          if (this.$refs.searchInput) {
            this.$refs.searchInput.focus();
          }
          // 预处理文本内容用于搜索
          if (this.hostInfo && this.hostInfo.osInfo && this.hostInfo.osInfo.dnsInfo && this.hostInfo.osInfo.dnsInfo.hostsFileContent) {
            this.hostsLines = this.hostInfo.osInfo.dnsInfo.hostsFileContent.split('\n');
          }
        });
      } else {
        this.clearSearch();
      }
    },
    
    // 隐藏搜索框
    hideSearch() {
      this.showSearch = false;
      this.clearSearch();
    },
    
    // 清除搜索状态
    clearSearch() {
      this.searchQuery = '';
      this.matchCount = null;
      this.matchIndex = 0;
      this.matches = [];
      this.clearHighlights();
    },
    
    // 清除高亮
    clearHighlights() {
      if (this.$refs.codeContainer) {
        const highlights = this.$refs.codeContainer.querySelectorAll('.search-highlight');
        highlights.forEach(el => {
          const text = el.textContent;
          el.outerHTML = text;
        });
      }
    },
    
    // 搜索
    onSearch() {
      if (!this.searchQuery || !this.hostsLines.length) {
        this.clearHighlights();
        this.matchCount = null;
        this.matchIndex = 0;
        this.matches = [];
        return;
      }
      
      this.$nextTick(() => {
        this.performSearch();
      });
    },
    
    // 执行搜索，在DOM中查找并高亮匹配项
    performSearch() {
      this.clearHighlights();
      if (!this.$refs.codeContainer || !this.searchQuery) return;
      
      const codeLines = this.$refs.codeContainer.querySelectorAll('.line');
      const query = this.searchQuery.toLowerCase();
      this.matches = [];
      
      codeLines.forEach((line, lineIndex) => {
        const content = line.textContent;
        let tempContent = content;
        let offset = 0;
        let pos = tempContent.toLowerCase().indexOf(query);
        
        while (pos !== -1) {
          this.matches.push({
            lineIndex,
            line: lineIndex + 1,
            position: pos + offset,
            element: line
          });
          
          // 添加高亮
          const before = tempContent.substring(0, pos);
          const match = tempContent.substring(pos, pos + this.searchQuery.length);
          const after = tempContent.substring(pos + this.searchQuery.length);
          
          const span = document.createElement('span');
          span.className = 'search-highlight';
          span.textContent = match;
          
          tempContent = after;
          offset += before.length + match.length;
          
          // 查找下一个匹配
          pos = tempContent.toLowerCase().indexOf(query);
        }
      });
      
      this.matchCount = this.matches.length;
      this.matchIndex = this.matches.length > 0 ? 0 : -1;
      
      if (this.matches.length > 0) {
        this.highlightMatches();
        this.scrollToMatch(0);
      }
    },
    
    // 高亮所有匹配
    highlightMatches() {
      if (!this.$refs.codeContainer) return;
      
      const content = this.$refs.codeContainer.querySelector('.code-content');
      if (!content || !this.searchQuery) return;
      
      const html = content.innerHTML;
      const regex = new RegExp(this.escapeRegExp(this.searchQuery), 'gi');
      content.innerHTML = html.replace(regex, match => 
        `<span class="search-highlight">${match}</span>`
      );
    },
    
    // 转义正则表达式中的特殊字符
    escapeRegExp(string) {
      return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    },
    
    // 滚动到指定匹配项
    scrollToMatch(index) {
      if (index < 0 || index >= this.matches.length || !this.$refs.codeContainer) return;
      
      const match = this.matches[index];
      if (!match.element) return;
      
      const highlights = this.$refs.codeContainer.querySelectorAll('.search-highlight');
      if (highlights.length <= index) return;
      
      // 设置当前高亮
      highlights.forEach((el, i) => {
        if (i === index) {
          el.classList.add('current-highlight');
        } else {
          el.classList.remove('current-highlight');
        }
      });
      
      // 滚动到匹配项
      match.element.scrollIntoView({
        behavior: 'smooth',
        block: 'center'
      });
    },
    
    // 查找下一项
    findNext() {
      if (this.matchCount <= 0) return;
      
      this.matchIndex = (this.matchIndex + 1) % this.matchCount;
      this.scrollToMatch(this.matchIndex);
    },
    
    // 查找上一项
    findPrevious() {
      if (this.matchCount <= 0) return;
      
      this.matchIndex = (this.matchIndex - 1 + this.matchCount) % this.matchCount;
      this.scrollToMatch(this.matchIndex);
    },
    
    // 格式化DNS服务器列表
    formatDnsServers(dnsServers) {
      if (!dnsServers) return [];
      if (typeof dnsServers === 'string') {
        return dnsServers.split(',').map(dns => dns.trim());
      }
      return dnsServers;
    },
    
    // 判断DNS服务器是否活跃 (这里我们假设第一个DNS是活跃的)
    isDnsActive(dns) {
      if (!this.hostInfo.osInfo || !this.hostInfo.osInfo.dnsInfo || !this.hostInfo.osInfo.dnsInfo.servers) return false;
      
      const dnsServers = this.formatDnsServers(this.hostInfo.osInfo.dnsInfo.servers);
      // 假设第一个是主要DNS服务器
      return dnsServers[0] === dns;
    },
    
    // 打开编辑hosts文件对话框
    editHostsFile() {
      // 设置编辑内容为当前hosts文件内容
      this.hostsFileContent = this.hostInfo.osInfo && this.hostInfo.osInfo.dnsInfo ? this.hostInfo.osInfo.dnsInfo.hostsFileContent : '';
      // 切换到编辑模式
      this.isEditingHosts = true;
      console.log('进入hosts文件编辑模式');
    },
    
    // 取消编辑
    cancelHostsEdit() {
      // 清空编辑内容
      this.hostsFileContent = '';
      // 退出编辑模式
      this.isEditingHosts = false;
      console.log('取消hosts文件编辑');
    },
    
    // 保存编辑
    saveHostsFile() {
      if (!this.hostsFileContent) {
        this.$message.warning('Hosts文件内容不能为空');
        return;
      }
      
      this.hostsEditLoading = true;
      
      // 使用FormData格式提交
      const formData = new FormData();
      formData.append('clusterId', this.hostInfo.clusterId);
      formData.append('ip', this.hostInfo.ip);
      formData.append('hostsFileContent', this.hostsFileContent);
      
      // 使用API保存内容
      this.$axiosPost('/host/updateHostsFile', formData)
        .then(res => {
          if (res.code === 200) {
            this.$message.success('Hosts文件修改成功');
            // 更新本地数据
            this.hostInfo.osInfo.dnsInfo.hostsFileContent = this.hostsFileContent;
            // 退出编辑模式
            this.isEditingHosts = false;
          } else {
            this.$message.error(res.msg || 'Hosts文件修改失败');
          }
        })
        .catch(err => {
          console.error('修改hosts文件出错:', err);
          this.$message.error('Hosts文件修改失败: ' + (err.message || err));
        })
        .finally(() => {
          this.hostsEditLoading = false;
        });
    },
    
    // 修改提交编辑方法，使用新的saveHostsFile
    submitHostsEdit() {
      this.saveHostsFile();
    },
    
    // 复制hosts文件内容
    copyHostsFile() {
      if (!this.hostInfo.osInfo || !this.hostInfo.osInfo.dnsInfo || !this.hostInfo.osInfo.dnsInfo.hostsFileContent) {
        this.$message.warning('没有可复制的Hosts文件内容');
        return;
      }
      
      // 创建临时文本区域元素
      const textarea = document.createElement('textarea');
      textarea.value = this.hostInfo.osInfo.dnsInfo.hostsFileContent;
      document.body.appendChild(textarea);
      textarea.select();
      
      try {
        // 执行复制命令
        const successful = document.execCommand('copy');
        if (successful) {
          this.$message.success('Hosts文件内容已复制到剪贴板');
        } else {
          this.$message.error('复制失败，请手动复制');
        }
      } catch (err) {
        this.$message.error('复制失败: ' + err);
      } finally {
        // 移除临时元素
        document.body.removeChild(textarea);
      }
    },
    
    // 更新行号（当编辑器内容变化时）
    updateLineNumbers() {
      // 更新行号
      this.$forceUpdate();
      
      // 模拟编辑模式下的高亮效果
      const textArea = document.querySelector('.code-editor');
      if (textArea) {
        // 获取当前编辑内容
        const content = textArea.value;
        
        // 如果有一个显示层，可以应用相同的高亮处理
        // 这里只是提示未来可以实现的功能
        console.log('编辑内容已更新');
      }
    },
    
    // 开始编辑hosts文件
    startEditingHosts() {
      this.editHostsFile();
    },
    
    // 复制hosts文件内容
    copyHostsContent() {
      this.copyHostsFile();
    },
    
    // 搜索hosts文件内容
    searchHosts() {
      if (!this.hostsSearchKeyword || !this.hostInfo.osInfo || !this.hostInfo.osInfo.dnsInfo || !this.hostInfo.osInfo.dnsInfo.hostsFileContent) {
        this.highlightedHostsContent = this.formatHostsFile(this.hostInfo.osInfo.dnsInfo.hostsFileContent);
        return;
      }
      
      const lines = this.hostInfo.osInfo.dnsInfo.hostsFileContent.split('\n');
      const filteredLines = lines.filter(line => line.toLowerCase().includes(this.hostsSearchKeyword.toLowerCase()));
      const highlightedLines = filteredLines.map(line => {
        const regex = new RegExp(this.escapeRegExp(this.hostsSearchKeyword), 'gi');
        return line.replace(regex, match => `<span class="search-highlight">${match}</span>`);
      });
      
      this.highlightedHostsContent = `<div class="hosts-code-content">${highlightedLines.join('\n')}</div>`;
    },
  }
}
</script>

<!-- 全局样式，用于处理v-html内部的内容 -->
<style>
/* IDE编辑器全局样式 */
.code-content {
  font-family: 'JetBrains Mono', 'SF Mono', Monaco, Menlo, Consolas, 'Courier New', monospace !important;
  font-size: 12px !important;
  line-height: 1.4 !important;
  color: #d4d4d4 !important;
  white-space: pre !important;
}

.code-content .comment {
  color: #6a9955 !important;
  font-style: italic !important;
}

.code-content .ip {
  color: #4ec9b0 !important;
  font-weight: bold !important;
}

.code-content .hostname {
  color: #9cdcfe !important;
}

.line {
  height: 20px;
  line-height: 20px;
  white-space: pre;
  position: relative;
}

.line:hover {
  background-color: rgba(38, 79, 120, 0.3);
}

.line-content {
  display: inline-block;
  padding-left: 4px;
}

.empty-line {
  height: 20px;
}

.inline-comment {
  color: #6a9955;
  font-style: italic;
}

/* 搜索高亮样式 */
.search-highlight {
  background-color: rgba(255, 200, 0, 0.3);
  border-radius: 2px;
}

.current-highlight {
  background-color: rgba(255, 160, 0, 0.5);
  border-radius: 2px;
  box-shadow: 0 0 0 1px rgba(255, 160, 0, 0.8);
}
</style>

<!-- 组件局部样式 -->
<style lang="less" scoped>
.hostname-detail-tooltip {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  display: flex;
  flex-direction: column;
}

.hostname-detail-popup {
  padding: 0;
  min-width: 320px;
  max-width: 420px;
  min-height: 200px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.12), 0 2px 4px rgba(0, 0, 0, 0.05);
  background-color: #ffffff;
  animation: fadeIn 0.3s ease-in-out;
  display: flex;
  flex-direction: column;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.hostname-detail-header {
  position: relative;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4ecfb 100%);
  padding: 24px;
  display: flex;
  flex-direction: column;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.hostname-detail-header-content {
  display: flex;
  z-index: 1;
}

.hostname-detail-icon-wrapper {
  margin-right: 16px;
}

.hostname-detail-icon-container {
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.hostname-detail-icon {
  width: 36px;
  height: 36px;
}

.hostname-detail-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.hostname-detail-name {
  margin-bottom: 8px;
}

.hostname-name-wrapper {
  display: flex;
  align-items: center;
}

.hostname-name {
  font-size: 20px;
  font-weight: 600;
  color: #1d1d1f;
  margin-right: 8px;
}

.hostname-ip {
  font-size: 14px;
  color: #6e6e73;
}

.hostname-detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.hostname-meta-item {
  display: flex;
  align-items: center;
}

.meta-label {
  font-size: 12px;
  color: #86868b;
  margin-right: 6px;
}

.meta-value {
  font-size: 13px;
  color: #1d1d1f;
  font-weight: 500;
}

.hostname-detail-content {
  padding: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.hostname-detail-section {
  display: flex;
  flex-direction: column;
}

.section-header {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1d1d1f;
  display: flex;
  align-items: center;
}

.section-icon {
  display: inline-flex;
  align-items: center;
  margin-right: 8px;
  width: 20px;
  height: 20px;
  justify-content: center;
}

.section-icon svg {
  width: 16px;
  height: 16px;
  stroke: #FF9500;
}

.section-title {
  font-size: 14px;
  font-weight: 500;
  color: #1d1d1f;
  white-space: nowrap;
  background: linear-gradient(90deg, #007AFF, #5AC8FA);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 0 1px rgba(0,122,255,0.1);
}

.hosts-icon {
  background-color: rgba(255, 149, 0, 0.1);
  color: #FF9500;
}

.hosts-file-info {
  display: flex;
  align-items: center;
}

.file-badge {
  font-size: 11px;
  background-color: rgba(0, 122, 255, 0.1);
  color: #007AFF;
  padding: 2px 6px;
  border-radius: 4px;
  margin-left: 8px;
}

.file-actions {
  display: flex;
  align-items: center;
}

.action-button {
  padding: 0 4px;
  height: 24px;
  line-height: 24px;
  color: #8e8e93;
  transition: color 0.3s ease;
  
  &:hover {
    color: #007aff;
  }
}

.info-item {
  display: flex;
  margin-bottom: 16px;
}

.info-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  flex-shrink: 0;
}

.info-icon.dns {
  background-color: rgba(88, 86, 214, 0.1);
  color: #5856d6;
}

.info-content {
  flex: 1;
}

.info-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.info-title {
  font-size: 14px;
  font-weight: 500;
  color: #1d1d1f;
}

.info-data {
  color: #6e6e73;
  font-size: 13px;
}

.dns-servers {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.dns-server-item {
  position: relative;
}

.dns-server-badge {
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, rgba(88, 86, 214, 0.08) 0%, rgba(88, 86, 214, 0.03) 100%);
  border: 1px solid rgba(88, 86, 214, 0.2);
  border-radius: 8px;
  padding: 6px 12px;
  font-size: 13px;
  transition: all 0.2s ease;
  position: relative;
  overflow: hidden;
}

.dns-server-badge:hover {
  background: linear-gradient(135deg, rgba(88, 86, 214, 0.12) 0%, rgba(88, 86, 214, 0.06) 100%);
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.dns-index {
  font-weight: 600;
  font-size: 11px;
  background-color: rgba(88, 86, 214, 0.15);
  color: #5856d6;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 8px;
}

.dns-ip {
  color: #1d1d1f;
  font-weight: 500;
  flex: 1;
}

.dns-status {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  color: #8e8e93;
  background-color: rgba(142, 142, 147, 0.1);
  margin-left: 8px;
}

.dns-status.active {
  color: #34c759;
  background-color: rgba(52, 199, 89, 0.1);
}

.info-empty {
  padding: 10px;
  border-radius: 8px;
  background-color: rgba(0, 0, 0, 0.02);
  color: #8e8e93;
  font-style: italic;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.hosts-file-container {
  background-color: transparent;
  border-radius: 8px;
  max-height: 300px;
  overflow: hidden;
}

.modern-ide {
  display: flex;
  flex-direction: column;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  background-color: #1e1e1e;
  height: 300px;
}

/* IDE工具栏 */
.ide-toolbar {
  display: flex;
  align-items: center;
  padding: 0 12px;
  height: 36px;
  background-color: #252526;
  border-bottom: 1px solid #1a1a1a;
  font-size: 12px;
}

.ide-breadcrumb {
  display: flex;
  align-items: center;
  color: #a0a0a0;
}

.breadcrumb-item {
  display: flex;
  align-items: center;
  padding: 0 2px;
}

.breadcrumb-item.root svg {
  margin-right: 4px;
  color: #5bb8ff;
}

.breadcrumb-item.active {
  color: #d4d4d4;
}

.breadcrumb-separator {
  margin: 0 4px;
  color: #505050;
}

.ide-toolbar-actions {
  display: flex;
  margin-left: auto;
}

.ide-toolbar-action {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 6px;
  height: 24px;
  color: #a0a0a0;
  border-radius: 3px;
}

.ide-toolbar-action:hover {
  background-color: #3c3c3c;
  color: #ffffff;
}

/* IDE编辑区域 */
.ide-editor {
  display: flex;
  flex: 1;
  overflow: hidden;
  background-color: #1e1e1e;
}

.ide-sidebar {
  background-color: #1e1e1e;
  border-right: 1px solid #2d2d2d;
  min-width: 50px;
  overflow: hidden;
}

.gutter-container {
  display: flex;
  height: 100%;
}

.gutter-folding {
  width: 12px;
  background-color: #1e1e1e;
}

.gutter-line-numbers {
  padding: 4px 0;
  min-width: 38px;
  text-align: right;
  color: #858585;
  user-select: none;
  font-size: 12px;
  font-family: 'JetBrains Mono', 'SF Mono', Monaco, Menlo, Consolas, 'Courier New', monospace;
}

.code-container {
  flex: 1;
  overflow: auto;
  padding: 4px 0;
  background-color: #1e1e1e;
}

/* 编辑模式样式 - 保持与原始风格一致 */
.code-container.editor-mode {
  position: relative;
  background-color: #1e1e1e;
  padding: 4px 0;
}

.code-editor {
  width: 100%;
  height: 100%;
  min-height: 100px;
  padding: 0 0 0 4px;
  border: none;
  background-color: transparent;
  font-family: 'JetBrains Mono', 'SF Mono', Monaco, Menlo, Consolas, 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.4;
  color: #d4d4d4;
  outline: none;
  resize: none;
  white-space: pre;
  overflow: auto;
}

.code-editor::placeholder {
  color: #6a737d;
}

.code-editor-wrapper {
  margin: 0;
  padding: 0;
  background-color: transparent;
}

/* IDE状态栏 */
.ide-statusbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  height: 22px;
  background-color: #007acc;
  color: #ffffff;
  font-size: 11px;
}

.statusbar-left, .statusbar-right {
  display: flex;
  align-items: center;
}

.status-item {
  display: flex;
  align-items: center;
  margin-right: 12px;
}

.status-item svg {
  margin-right: 4px;
}

.statusbar-right .status-item {
  margin-right: 0;
  margin-left: 12px;
  opacity: 0.8;
}

.hostname-detail-header-blur {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 40px;
  background: linear-gradient(to bottom, rgba(255, 255, 255, 0), rgba(255, 255, 255, 0.8));
  z-index: 0;
}

/* IDE搜索框样式 */
.ide-search {
  background-color: #252526;
  padding: 8px 12px;
  border-bottom: 1px solid #1a1a1a;
}

.search-container {
  display: flex;
  align-items: center;
}

.search-input {
  flex: 1;
  height: 24px;
  border: 1px solid #3c3c3c;
  background-color: #3c3c3c;
  border-radius: 3px;
  color: #e0e0e0;
  padding: 0 8px;
  font-size: 12px;
  outline: none;
}

.search-input:focus {
  border-color: #007acc;
}

.search-controls {
  display: flex;
  align-items: center;
  margin-left: 8px;
}

.match-count {
  color: #e0e0e0;
  font-size: 12px;
  margin-right: 8px;
}

.search-actions {
  display: flex;
  align-items: center;
}

.search-button {
  width: 24px;
  height: 24px;
  background: none;
  border: none;
  color: #cccccc;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 2px;
  border-radius: 3px;
}

.search-button:hover:not(:disabled) {
  background-color: #3c3c3c;
}

.search-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.search-button.close {
  margin-left: 8px;
}

/* 移除标题栏的样式，保留工具栏的样式 */
.ide-titlebar {
  display: none;
}

.form-help-text {
  color: #888;
  font-size: 12px;
  display: flex;
  align-items: center;
  margin-top: 4px;
  
  .anticon {
    margin-right: 4px;
    font-size: 14px;
  }
}

/* 主机名加载动画样式 */
.hostname-loading-container {
  display: flex;
  align-items: center;
  height: 20px;
}

.hostname-loading-dots {
  display: flex;
  align-items: center;
  margin-right: 8px;
}

.hostname-loading-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #007AFF;
  margin: 0 2px;
  opacity: 0.2;
  animation: pulse 1.4s infinite ease-in-out;
}

.hostname-loading-dot:nth-child(1) {
  animation-delay: 0s;
}

.hostname-loading-dot:nth-child(2) {
  animation-delay: 0.2s;
}

.hostname-loading-dot:nth-child(3) {
  animation-delay: 0.4s;
}

.hostname-loading-text {
  font-size: 14px;
  color: #007AFF;
  font-weight: 500;
}

/* FQDN加载动画 */
.fqdn-loading-container {
  display: flex;
  align-items: center;
  height: 16px;
}

.fqdn-loading-pulse {
  width: 80px;
  height: 16px;
  border-radius: 4px;
  background: linear-gradient(90deg, #f0f0f0, #e0e0e0, #f0f0f0);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.fqdn-loading-text {
  font-size: 12px;
  color: #8E8E93;
  margin-left: 8px;
}

/* 动画关键帧 */
@keyframes pulse {
  0%, 80%, 100% { 
    transform: scale(0.6);
    opacity: 0.2;
  }
  40% { 
    transform: scale(1);
    opacity: 1;
  }
}

@keyframes shimmer {
  0% {
    background-position: -80px 0;
  }
  100% {
    background-position: 80px 0;
  }
}

/* 强制覆盖Modal样式确保可见 */
/deep/ .ant-modal, /deep/ .ant-modal-mask, /deep/ .ant-modal-wrap {
  display: block !important;
  visibility: visible !important;
  opacity: 1 !important;
  z-index: 1000 !important;
}

/deep/ .ant-modal-wrap {
  position: fixed !important;
  top: 0 !important;
  left: 0 !important;
  right: 0 !important;
  bottom: 0 !important;
  overflow: auto !important;
}

/deep/ .ant-modal {
  position: relative !important;
  margin: 100px auto !important;
  width: auto !important;
  max-width: 700px !important;
}

.hostlist-tooltip .info-data {
  padding: 8px 12px;
  border-radius: 8px;
}

/* 加载动画样式 */
.dns-servers-loading {
  width: 100%;
  padding: 8px 0;
}

.loading-shimmer {
  width: 100%;
  animation: shimmer 1.5s infinite linear;
  background: linear-gradient(to right, rgba(0,122,255,0.06) 4%, rgba(0,122,255,0.12) 25%, rgba(0,122,255,0.06) 36%);
  background-size: 1000px 100%;
}

.loading-line {
  height: 15px;
  margin-bottom: 8px;
  border-radius: 4px;
  background: rgba(0,122,255,0.1);
}

.loading-line.short {
  width: 30%;
}

.loading-line.medium {
  width: 60%;
}

.loading-line.long {
  width: 85%;
}

.loading-line.xshort {
  width: 10%;
}

.loading-spinner {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.spinner-text {
  margin-top: 8px;
  color: #007AFF;
  font-size: 14px;
  font-weight: 500;
}

@keyframes shimmer {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

/* Hosts文件加载动画样式 */
.hosts-loading-section {
  margin-top: 10px;
  border-radius: 8px;
  background-color: #f9f9fb;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.hosts-loading-container {
  padding: 16px;
  
  .toolbar {
    margin-bottom: 12px;
    display: flex;
    justify-content: space-between;
  }
  
  .content {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 120px;
    
    .loading-text {
      margin-top: 12px;
      color: #8c8c8c;
      font-size: 14px;
    }
  }

  .loading-line {
    height: 12px;
    border-radius: 6px;
    background: linear-gradient(90deg, #f0f0f0, #e0e0e0, #f0f0f0);
    background-size: 200% 100%;
    animation: shimmer 1.5s infinite;
    margin-bottom: 8px;

    &.loading-line-small {
      width: 60px;
    }

    &.loading-line-medium {
      width: 120px;
    }

    &.loading-line-large {
      width: 180px;
    }
  }

  @keyframes shimmer {
    0% {
      background-position: -200% 0;
    }
    100% {
      background-position: 200% 0;
    }
  }
}

.hosts-file-content {
  margin-top: 10px;
  border-radius: 8px;
  background-color: #f9f9fb;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  
  .search-bar {
    padding: 8px 16px;
    background-color: #f0f2f5;
    border-bottom: 1px solid #e8e8e8;
  }
  
  .hosts-content {
    padding: 16px;
    max-height: 300px;
    overflow-y: auto;
    font-family: 'Courier New', monospace;
    font-size: 13px;
    line-height: 1.5;
    
    pre {
      margin: 0;
      white-space: pre-wrap;
      word-wrap: break-word;
    }
  }
}

.no-hosts-content {
  margin-top: 10px;
  padding: 24px;
  background-color: #f9f9fb;
  border-radius: 8px;
  text-align: center;
}

/* 搜索高亮样式 */
:deep(.search-highlight) {
  background-color: #ffff00;
  color: #000;
  font-weight: bold;
  padding: 1px 2px;
  border-radius: 2px;
}
</style>