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
        <div class="hosts-section" v-if="hostInfo.osInfo && hostInfo.osInfo.dnsInfo && hostInfo.osInfo.dnsInfo.hostsFileContent">
          <div class="section-header">
            <span class="section-title">
              <div class="section-icon hosts-icon">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                  <polyline points="14 2 14 8 20 8"></polyline>
                  <line x1="16" y1="13" x2="8" y2="13"></line>
                  <line x1="16" y1="17" x2="8" y2="17"></line>
                  <polyline points="10 9 9 9 8 9"></polyline>
                </svg>
              </div>
              Hosts文件内容
            </span>
            <div class="actions">
              <!-- 非编辑模式下显示编辑和复制按钮 -->
              <template v-if="!isEditingHosts">
                <a-button class="apple-button edit-button" @click="startEditingHosts">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" class="button-icon">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                  </svg>
                  <span>编辑</span>
                </a-button>
                <a-button class="apple-button copy-button" @click="copyHostsContent">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" class="button-icon">
                    <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                    <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                  </svg>
                  <span>复制</span>
                </a-button>
              </template>
              <!-- 编辑模式下显示保存和取消按钮 -->
              <template v-else>
                <a-button class="apple-button cancel-button" @click="cancelHostsEdit">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" class="button-icon">
                    <line x1="18" y1="6" x2="6" y2="18"></line>
                    <line x1="6" y1="6" x2="18" y2="18"></line>
                  </svg>
                  <span>取消</span>
                </a-button>
                <a-button class="apple-button save-button" @click="saveHostsFile" :loading="hostsEditLoading">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" class="button-icon" v-if="!hostsEditLoading">
                    <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
                    <polyline points="17 21 17 13 7 13 7 21"></polyline>
                    <polyline points="7 3 7 8 15 8"></polyline>
                  </svg>
                  <span>保存</span>
                </a-button>
              </template>
            </div>
          </div>
          
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
                <div class="ide-toolbar-actions">
                  <div class="ide-toolbar-action" @click="toggleSearch">
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                      <circle cx="11" cy="11" r="8"></circle>
                      <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                    </svg>
                  </div>
                </div>
              </div>
              
              <!-- 添加搜索框 -->
              <div class="ide-search" v-if="showSearch">
                <div class="search-container">
                  <input 
                    ref="searchInput"
                    v-model="searchQuery" 
                    type="text" 
                    class="search-input" 
                    placeholder="在hosts文件中搜索..." 
                    @input="onSearch"
                    @keydown.esc="hideSearch"
                  />
                  <div class="search-controls">
                    <span class="match-count" v-if="matchCount !== null">{{ matchIndex + 1 }}/{{ matchCount }}</span>
                    <div class="search-actions">
                      <button class="search-button" @click="findPrevious" :disabled="matchCount === 0">
                        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                          <line x1="12" y1="19" x2="12" y2="5"></line>
                          <polyline points="5 12 12 5 19 12"></polyline>
                        </svg>
                      </button>
                      <button class="search-button" @click="findNext" :disabled="matchCount === 0">
                        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                          <line x1="12" y1="5" x2="12" y2="19"></line>
                          <polyline points="19 12 12 19 5 12"></polyline>
                        </svg>
                      </button>
                      <button class="search-button close" @click="hideSearch">
                        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                          <line x1="18" y1="6" x2="6" y2="18"></line>
                          <line x1="6" y1="6" x2="18" y2="18"></line>
                        </svg>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
              
              <!-- 代码编辑区域 - 根据模式显示不同内容 -->
              <div class="ide-editor">
                <!-- 侧边栏 - 行号和折叠等 -->
                <div class="ide-sidebar">
                  <div class="gutter-container">
                    <div class="gutter-folding"></div>
                    <div class="gutter-line-numbers">
                      <div v-for="n in (isEditingHosts ? editingHostsContent.split('\n').length : hostInfo.osInfo.dnsInfo.hostsFileContent.split('\n').length || 1)" 
                           :key="n" 
                           class="line-number">
                        {{ n }}
                      </div>
                    </div>
                  </div>
                </div>
                
                <!-- 主代码区域 - 非编辑模式 -->
                <div class="code-container" ref="codeContainer" v-if="!isEditingHosts">
                  <div v-html="highlightedHostsContent" class="code-content"></div>
                </div>
                
                <!-- 主代码区域 - 编辑模式 -->
                <div class="code-container editor-mode" v-else>
                  <textarea 
                    v-model="editingHostsContent"
                    class="code-editor"
                    spellcheck="false"
                    @input="updateLineNumbers"
                    placeholder="请输入hosts文件内容"
                  ></textarea>
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
                    <span>{{ isEditingHosts ? "编辑中" : "只读" }}</span>
                  </div>
                  <div v-if="isEditingHosts" class="status-item editing-status">
                    <span>点击"保存"按钮应用更改</span>
                  </div>
                </div>
                <div class="statusbar-right">
                  <div class="status-item">
                    <span>UTF-8</span>
                  </div>
                  <div class="status-item">
                    <span>LF</span>
                  </div>
                  <div class="status-item">
                    <span>Plain Text</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Hosts文件加载中 -->
        <div class="hosts-section" v-else-if="isLoading('hosts')">
          <div class="section-header">
            <span class="section-title">
              <div class="section-icon hosts-icon">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                  <polyline points="14 2 14 8 20 8"></polyline>
                  <line x1="16" y1="13" x2="8" y2="13"></line>
                  <line x1="16" y1="17" x2="8" y2="17"></line>
                  <polyline points="10 9 9 9 8 9"></polyline>
                </svg>
              </div>
              Hosts文件内容
            </span>
          </div>
          <div class="hosts-file-container loading">
            <div class="modern-ide">
              <!-- IDE工具栏 - 固定部分 -->
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
              
              <!-- 加载动画 -->
              <div class="hosts-file-loading">
                <div class="hosts-file-loading-lines">
                  <div class="loading-line short"></div>
                  <div class="loading-line medium"></div>
                  <div class="loading-line long"></div>
                  <div class="loading-line medium"></div>
                  <div class="loading-line short"></div>
                  <div class="loading-line very-long"></div>
                  <div class="loading-line medium"></div>
                  <div class="loading-line short"></div>
                </div>
              </div>
              
              <!-- 底部状态栏 - 固定部分 -->
              <div class="ide-statusbar">
                <div class="statusbar-left">
                  <div class="status-item">
                    <span>加载中...</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 如果没有hosts文件 -->
        <div class="hosts-section" v-else>
          <div class="section-header">
            <span class="section-title">
              <div class="section-icon hosts-icon">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                  <polyline points="14 2 14 8 20 8"></polyline>
                  <line x1="16" y1="13" x2="8" y2="13"></line>
                  <line x1="16" y1="17" x2="8" y2="17"></line>
                  <polyline points="10 9 9 9 8 9"></polyline>
                </svg>
              </div>
              Hosts文件内容
            </span>
          </div>
          <div class="hosts-file-container">
            <div class="info-empty">暂无hosts文件内容</div>
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
      loadingStates: {
        dns: true,
        hosts: true,
      },
      dnsServers: [],
      hostsFileContent: '',
      editingHostsContent: '',
      highlightedHostsContent: '',
      hostsSearchKeyword: '',
      isEditingHosts: false,
      hostsEditLoading: false,
      showSearch: false,
      searchQuery: '',
      matchCount: null,
      matchIndex: 0,
      matches: [],
      hostsLines: [],
    };
  },
  created() {
    // 初始化hosts文件内容
    this.initializeHostsContent();
  },
  updated() {
    // 当hostInfo更新时，重新初始化hosts文件内容
    this.initializeHostsContent();
  },
  methods: {
    // 初始化hosts文件内容
    initializeHostsContent() {
      if (this.hostInfo && 
          this.hostInfo.osInfo && 
          this.hostInfo.osInfo.dnsInfo && 
          this.hostInfo.osInfo.dnsInfo.hostsFileContent) {
        // 格式化并高亮hosts文件内容
        this.highlightedHostsContent = this.formatHostsFile(this.hostInfo.osInfo.dnsInfo.hostsFileContent);
        
        // 同时初始化hostsLines用于搜索
        this.hostsLines = this.hostInfo.osInfo.dnsInfo.hostsFileContent.split('\n');
      }
    },
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
      let formattedContent = '';
      
      // 为每一行添加适当的样式
      lines.forEach((line, index) => {
        let lineClass = 'line';
        let lineContent = '';
        
        // 根据行内容添加样式类
        if (line.trim().startsWith('#')) {
          lineClass += ' comment-line';
          lineContent = this.escapeHtml(line);
        } else if (line.trim() === '') {
          lineClass += ' empty-line';
          lineContent = ' ';
        } else {
          lineClass += ' config-line';
          
          // 高亮IP地址和主机名
          const parts = line.trim().split(/\s+/);
          if (parts.length >= 2) {
            const ip = parts[0];
            const hostnames = parts.slice(1).join(' ');
            
            if (line.includes('#')) {
              // 行内注释
              const commentIndex = line.indexOf('#');
              const beforeComment = line.substring(0, commentIndex);
              const comment = line.substring(commentIndex);
              
              lineContent = this.highlightIpHostname(beforeComment) + 
                           `<span class="inline-comment">${this.escapeHtml(comment)}</span>`;
            } else {
              lineContent = `<span class="ip-address">${this.escapeHtml(ip)}</span> ` + 
                           `<span class="hostname-entry">${this.escapeHtml(hostnames)}</span>`;
            }
          } else {
            lineContent = this.escapeHtml(line);
          }
        }
        
        // 构建行HTML - 减小内容与左侧的间距
        formattedContent += `<div class="${lineClass}"><span class="line-content">${lineContent}</span></div>`;
      });
      
      return formattedContent;
    },
    
    // 高亮IP和主机名
    highlightIpHostname(text) {
      const parts = text.trim().split(/\s+/);
      if (parts.length < 2) return this.escapeHtml(text);
      
      const ip = parts[0];
      const hostnames = parts.slice(1).join(' ');
      
      return `<span class="ip-address">${this.escapeHtml(ip)}</span> <span class="hostname-entry">${this.escapeHtml(hostnames)}</span>`;
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
    
    /**
     * 切换搜索框显示状态
     */
    toggleSearch() {
      this.showSearch = !this.showSearch;
      if (this.showSearch) {
        this.$nextTick(() => {
          if (this.$refs.searchInput) {
            this.$refs.searchInput.focus();
          }
          
          // 在打开搜索时初始化 hostsLines 数组
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
      // 确保 hostsLines 数组已初始化
      if (!this.hostsLines || !this.hostsLines.length) {
        if (this.hostInfo && this.hostInfo.osInfo && this.hostInfo.osInfo.dnsInfo && this.hostInfo.osInfo.dnsInfo.hostsFileContent) {
          this.hostsLines = this.hostInfo.osInfo.dnsInfo.hostsFileContent.split('\n');
        } else {
          this.matchCount = 0;
          this.matches = [];
          this.matchIndex = -1;
          return;
        }
      }

      if (!this.searchQuery || this.searchQuery.trim() === '') {
        this.clearHighlights();
        this.matchCount = null;
        this.matchIndex = -1;
        this.matches = [];
        return;
      }

      // 查找所有匹配项
      this.matches = [];
      const codeContainer = this.$refs.codeContainer;
      if (!codeContainer) return;

      const codeLines = codeContainer.querySelectorAll('.line');
      const query = this.escapeRegExp(this.searchQuery.trim());
      const regex = new RegExp(query, 'gi');

      // 清除之前的高亮
      this.clearHighlights();

      // 遍历所有代码行查找匹配项
      codeLines.forEach((line, index) => {
        const lineContent = line.textContent || '';
        let match;
        let lastIndex = 0;
        
        // 使用正则表达式查找所有匹配项
        regex.lastIndex = 0;
        while ((match = regex.exec(lineContent)) !== null) {
          this.matches.push({
            line: index,
            lineEl: line,
            text: match[0],
            startIndex: match.index,
            endIndex: regex.lastIndex
          });
          lastIndex = regex.lastIndex;
        }
      });

      // 更新匹配数量
      this.matchCount = this.matches.length;
      
      // 如果有匹配项，高亮第一个匹配
      if (this.matches.length > 0) {
        this.matchIndex = 0;
        this.highlightCurrentMatch();
      } else {
        this.matchIndex = -1;
      }
    },
    
    // 滚动到指定匹配项
    scrollToMatch(index) {
      if (index < 0 || index >= this.matches.length || !this.$refs.codeContainer) return;
      
      const match = this.matches[index];
      if (!match || !match.lineEl) return;
      
      // 清除之前的高亮状态
      const highlights = this.$refs.codeContainer.querySelectorAll('.search-highlight');
      highlights.forEach(el => el.classList.remove('current-highlight'));
      
      // 添加当前高亮
      this.highlightCurrentMatch();
      
      // 滚动到匹配项
      match.lineEl.scrollIntoView({
        behavior: 'smooth',
        block: 'center'
      });
    },
    
    // 高亮当前匹配项
    highlightCurrentMatch() {
      if (this.matchIndex < 0 || this.matchIndex >= this.matches.length || !this.$refs.codeContainer) return;
      
      const highlights = this.$refs.codeContainer.querySelectorAll('.search-highlight');
      if (highlights.length <= this.matchIndex) return;
      
      // 清除所有当前高亮
      highlights.forEach(el => el.classList.remove('current-highlight'));
      
      // 添加当前高亮
      if (highlights[this.matchIndex]) {
        highlights[this.matchIndex].classList.add('current-highlight');
      }
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
    
    /**
     * 开始编辑hosts文件内容
     */
    startEditingHosts() {
      if (this.hostInfo && this.hostInfo.osInfo && this.hostInfo.osInfo.dnsInfo) {
        this.editingHostsContent = this.hostInfo.osInfo.dnsInfo.hostsFileContent || '';
        this.isEditingHosts = true;
      }
    },
    
    /**
     * 获取hosts文件行数
     */
    getHostsLinesCount() {
      if (!this.editingHostsContent) return 0;
      return this.editingHostsContent.split('\n').length;
    },
    
    /**
     * 复制hosts文件内容到剪贴板
     */
    copyHostsContent() {
      if (this.hostInfo && this.hostInfo.osInfo && this.hostInfo.osInfo.dnsInfo) {
        const content = this.hostInfo.osInfo.dnsInfo.hostsFileContent || '';
        
        // 使用更可靠的clipboard复制方法
        const textArea = document.createElement('textarea');
        textArea.value = content;
        textArea.style.position = 'fixed';  // 避免滚动到底部
        textArea.style.opacity = '0';
        document.body.appendChild(textArea);
        textArea.select();
        
        try {
          const successful = document.execCommand('copy');
          if (successful) {
            this.$message.success('hosts文件内容已复制到剪贴板');
          } else {
            // 尝试使用Clipboard API作为备选方法
            navigator.clipboard.writeText(content).then(() => {
              this.$message.success('hosts文件内容已复制到剪贴板');
            }).catch(err => {
              this.$message.error('复制失败，请手动选择并复制内容');
            });
          }
        } catch (err) {
          this.$message.error('复制失败，请手动选择并复制内容');
        } finally {
          document.body.removeChild(textArea);
        }
      }
    },
    
    /**
     * 保存编辑的hosts文件内容
     */
    saveHostsFile() {
      if (!this.editingHostsContent) {
        this.$message.warning('Hosts文件内容不能为空');
        return;
      }
      
      this.hostsEditLoading = true;
      
      // 使用FormData格式提交
      const formData = new FormData();
      formData.append('clusterId', this.hostInfo.clusterId);
      formData.append('ip', this.hostInfo.ip);
      formData.append('hostsFileContent', this.editingHostsContent);
      
      // 使用API保存内容
      this.$axiosPost('/host/updateHostsFile', formData)
        .then(res => {
          if (res.code === 200) {
            this.$message.success('Hosts文件修改成功');
            // 更新本地数据
            this.hostInfo.osInfo.dnsInfo.hostsFileContent = this.editingHostsContent;
            // 退出编辑模式
            this.isEditingHosts = false;
            // 重新初始化内容显示
            this.initializeHostsContent();
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
    
    /**
     * 搜索hosts文件内容
     */
    searchHosts() {
      // 如果没有搜索关键词或者没有hosts文件内容，显示全部内容
      if (!this.hostsSearchKeyword || !this.hostInfo.osInfo || !this.hostInfo.osInfo.dnsInfo || !this.hostInfo.osInfo.dnsInfo.hostsFileContent) {
        if (this.hostInfo.osInfo && this.hostInfo.osInfo.dnsInfo && this.hostInfo.osInfo.dnsInfo.hostsFileContent) {
          this.highlightedHostsContent = this.formatHostsFile(this.hostInfo.osInfo.dnsInfo.hostsFileContent);
        }
        return;
      }
      
      const lines = this.hostInfo.osInfo.dnsInfo.hostsFileContent.split('\n');
      
      // 如果启用了搜索功能，则过滤和高亮显示
      if (this.hostsSearchKeyword.trim() !== '') {
        const filteredLines = lines.filter(line => 
          line.toLowerCase().includes(this.hostsSearchKeyword.toLowerCase())
        );
        
        if (filteredLines.length > 0) {
          const highlightedLines = filteredLines.map(line => {
            const regex = new RegExp(this.escapeRegExp(this.hostsSearchKeyword), 'gi');
            return line.replace(regex, match => `<span class="search-highlight">${match}</span>`);
          });
          
          this.highlightedHostsContent = `<div class="hosts-code-content">${highlightedLines.join('\n')}</div>`;
        } else {
          // 没有匹配结果时显示提示
          this.highlightedHostsContent = '';
        }
      } else {
        // 如果搜索关键词为空，显示全部内容
        this.highlightedHostsContent = this.formatHostsFile(this.hostInfo.osInfo.dnsInfo.hostsFileContent);
      }
    },
    /**
     * 转义正则表达式中的特殊字符
     */
    escapeRegExp(string) {
      return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    },
    cancelHostsEdit() {
      this.isEditingHosts = false;
      this.editingHostsContent = '';
    },
    /**
     * 更新行号显示
     * 根据文本内容更新编辑器左侧的行号
     */
    updateLineNumbers() {
      // 这个方法会在文本编辑器内容变化时触发
      // 由于我们的行号是通过 v-for 动态生成的，不需要额外逻辑
      // 当 editingHostsContent 变化时，Vue 会自动重新计算 v-for 中的行数
      
      // 如果需要执行其他操作，可以在这里添加
      // 例如计算行数并在状态栏显示
      const lineCount = this.editingHostsContent.split('\n').length;
      
      // 在需要时，可以添加额外的状态更新
      // this.lineCount = lineCount;
    },
  }
}
</script>

<!-- 全局样式，用于处理v-html内部的内容 -->
<style>
/* IDE编辑器全局样式 */
.code-content {
  font-family: 'JetBrains Mono', 'SF Mono', Monaco, Menlo, Consolas, 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.4;
  color: #d4d4d4;
  white-space: pre;
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
  padding-left: 0;
}

.comment-line {
  color: #6a9955;
  font-style: italic;
}

.empty-line {
  height: 20px;
}

.ip-address {
  color: #4ec9b0;
}

.hostname-entry {
  color: #9cdcfe;
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

/* 添加没有搜索结果时的样式 */
.no-search-results {
  padding: 20px;
  text-align: center;
  color: #8e8e93;
  font-style: italic;
  background-color: rgba(0, 0, 0, 0.02);
  border-radius: 8px;
  margin: 10px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.no-search-results::before {
  content: "";
  display: inline-block;
  width: 16px;
  height: 16px;
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="16" height="16" stroke="%238e8e93" stroke-width="2" fill="none"><path d="M10 3a7 7 0 100 14 7 7 0 000-14z"/><path d="M21 21l-6-6"/></svg>');
  background-repeat: no-repeat;
  background-position: center;
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
  --scrollbar-opacity: 0.1; /* 初始透明度较低 */
}

.modern-ide:hover {
  --scrollbar-opacity: 0.3; /* 悬停时提高透明度 */
}

.modern-ide:active {
  --scrollbar-opacity: 0.5; /* 激活时透明度更高 */
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
  min-width: 30px;
  text-align: right;
  color: #858585;
  user-select: none;
  font-size: 12px;
  font-family: 'JetBrains Mono', 'SF Mono', Monaco, Menlo, Consolas, 'Courier New', monospace;
}

.code-container {
  flex: 1;
  overflow: auto;
  padding: 4px 0 4px 0;
  background-color: #1e1e1e;
  position: relative; /* 添加定位上下文 */
}

/* 编辑模式样式 - 保持与原始风格一致 */
.code-container.editor-mode {
  position: relative;
  background-color: #1e1e1e;
  padding: 4px 0;
  overflow: hidden;
}

.code-editor {
  width: 100%;
  height: 100%;
  min-height: 100px;
  padding: 0;
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

/* 无搜索结果样式 */
.no-search-results {
  padding: 10px 15px;
  text-align: center;
  font-style: italic;
  background-color: rgba(0, 0, 0, 0.03);
  border-radius: 4px;
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.no-search-results::before {
  content: "";
  display: inline-block;
  width: 16px;
  height: 16px;
  margin-right: 8px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%23999' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Ccircle cx='11' cy='11' r='8'%3E%3C/circle%3E%3Cline x1='21' y1='21' x2='16.65' y2='16.65'%3E%3C/line%3E%3Cline x1='8' y1='11' x2='14' y2='11'%3E%3C/line%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: center;
}

/* 使用更加苹果风格的按钮样式 */
.apple-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  padding: 0 16px;
  margin-left: 8px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 6px;
  transition: all 0.2s ease;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  border: 1px solid transparent;
}

.button-icon {
  margin-right: 6px;
}

.edit-button {
  color: #007aff;
  background-color: rgba(0, 122, 255, 0.1);
  border-color: transparent;
}

.edit-button:hover {
  background-color: rgba(0, 122, 255, 0.15);
}

.copy-button {
  color: #5856d6;
  background-color: rgba(88, 86, 214, 0.1);
  border-color: transparent;
}

.copy-button:hover {
  background-color: rgba(88, 86, 214, 0.15);
}

.cancel-button {
  color: #8e8e93;
  background-color: rgba(142, 142, 147, 0.1);
  border-color: transparent;
}

.cancel-button:hover {
  background-color: rgba(142, 142, 147, 0.15);
}

.save-button {
  color: #ffffff;
  background-color: #007aff;
  border-color: transparent;
}

.save-button:hover {
  background-color: #0071e3;
}

/* 应用透明度变量到滚动条 */
.code-container::-webkit-scrollbar-thumb,
.code-editor::-webkit-scrollbar-thumb {
  background-color: rgba(110, 110, 110, var(--scrollbar-opacity));
  transition: background-color 0.3s ease;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

.code-container::-webkit-scrollbar-thumb:hover,
.code-editor::-webkit-scrollbar-thumb:hover {
  background-color: rgba(140, 140, 140, calc(var(--scrollbar-opacity) + 0.2));
}

.code-container::-webkit-scrollbar-thumb:active,
.code-editor::-webkit-scrollbar-thumb:active {
  background-color: rgba(170, 170, 170, calc(var(--scrollbar-opacity) + 0.4));
}

/* 使滚动条更圆润，更贴近macOS风格 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
  background-color: transparent;
}

/* 滚动条轨道 */
::-webkit-scrollbar-track {
  background-color: transparent;
  border-radius: 100px;
}

::-webkit-scrollbar-thumb {
  border-radius: 100px;
  background-clip: padding-box;
  border: 2px solid transparent;
  min-height: 40px;
}

/* 滚动条角落 */
::-webkit-scrollbar-corner {
  background-color: transparent;
}

/* 为Firefox添加自定义滚动条 */
.code-container, .code-editor {
  scrollbar-width: thin;
  scrollbar-color: rgba(110, 110, 110, var(--scrollbar-opacity)) transparent;
}
</style>