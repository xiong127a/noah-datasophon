<!--
 * 操作系统悬浮卡片组件
 * 这个文件包含了操作系统悬浮卡片的所有代码，用于在主机环境校验页面显示操作系统信息
-->
<template>
  <div class="os-detail-tooltip">
    <div class="os-detail-popup">
      <!-- 标题区域 -->
      <div class="os-detail-header">
        <div class="os-detail-header-content">
          <div class="os-detail-icon-wrapper">
            <div class="os-detail-icon-container">
              <img
                :src="getOsIconPath(osInfo)"
                :alt="getOsName(osInfo)"
                class="os-detail-icon"
              />
            </div>
          </div>
          <div class="os-detail-info">
            <div class="os-detail-name">
              <!-- 操作系统名称加载动画 -->
              <div v-if="!osInfo || !osInfo.distribution" class="os-name-loading">
                <div class="os-loader-container">
                  <div class="os-loader-spinner">
                    <div class="spinner-inner"></div>
                  </div>
                  <span class="os-loading-text">获取系统信息</span>
                </div>
              </div>
              <!-- 操作系统名称已加载 -->
              <template v-else>
                <span class="os-name">{{ getOsName(osInfo) }}</span>
              </template>
            </div>
            <div class="os-detail-meta">
              <!-- 架构信息 -->
              <div class="os-meta-item">
                <span class="meta-label">架构</span>
                <span v-if="osInfo && osInfo.architecture" class="meta-value">{{ osInfo.architecture }}</span>
                <div v-else class="meta-loading-pulse"></div>
              </div>
              <!-- 内核版本 -->
              <div class="os-meta-item">
                <span class="meta-label">内核版本</span>
                <span v-if="osInfo && osInfo.kernelVersion" class="meta-value">{{ osInfo.kernelVersion }}</span>
                <div v-else class="meta-loading-pulse"></div>
              </div>
              <!-- 主机名 -->
              <div class="os-meta-item">
                <span class="meta-label">主机名</span>
                <span v-if="osInfo && osInfo.hostname" class="meta-value">{{ osInfo.hostname }}</span>
                <div v-else class="meta-loading-pulse"></div>
              </div>
            </div>
          </div>
        </div>
        <div class="os-detail-header-blur"></div>
      </div>

      <!-- 内容区域包装元素 -->
      <div class="os-detail-content">
        <!-- 硬件信息卡片 -->
        <div class="os-detail-section">
          <div class="section-header">
            <span class="section-title">硬件信息</span>
          </div>

          <!-- CPU信息 -->
          <div class="hardware-item">
            <div class="hardware-icon cpu" :class="{ 'loading': getCpuStatus() === 'loading', 'error': getCpuStatus() === 'error' }">
              <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none">
                <rect x="4" y="4" width="16" height="16" rx="2" />
                <rect x="9" y="9" width="6" height="6" />
                <path d="M4 9h2" /><path d="M4 15h2" />
                <path d="M18 9h2" /><path d="M18 15h2" />
                <path d="M9 4v2" /><path d="M15 4v2" />
                <path d="M9 18v2" /><path d="M15 18v2" />
              </svg>
            </div>
            <div class="hardware-content">
              <div class="hardware-header">
                <span class="hardware-title">处理器</span>
                <div class="hardware-status">
                  <a-icon v-if="checkCpuStatus('success')" type="check-circle" class="status-icon success" />
                  <a-icon v-else-if="checkCpuStatus('error')" type="close-circle" class="status-icon error" />
                  <a-icon v-else-if="checkCpuStatus('loading')" type="loading" class="status-icon loading" spin />
                  <a-icon v-else type="clock-circle" class="status-icon pending" />
                </div>
              </div>
              <div class="hardware-info" v-if="getCpuStatus() === 'loading'">
                <div class="apple-hardware-loading">
                  <div class="apple-loading-line line1"></div>
                  <div class="apple-loading-line line2"></div>
                </div>
                <div class="loading-text cpu-loading-text">正在分析处理器信息...</div>
              </div>
              <div class="hardware-info error" v-else-if="getCpuStatus() === 'error'">
                获取CPU信息失败
              </div>
              <div class="hardware-info" v-else>
                <template v-if="osInfo && osInfo.cpuInfo">
                  <div class="info-primary">{{ osInfo.cpuInfo.model || '未知CPU' }}</div>
                  <div class="info-secondary">
                    {{ osInfo.cpuInfo.physicalCount || 1 }} × {{ osInfo.cpuInfo.cores || 1 }} 核心
                    <span v-if="osInfo.cpuInfo.logicalCores"> × {{ calculateThreadsPerCore() }} 线程</span>
                    <span v-if="osInfo.cpuInfo.frequency && osInfo.cpuInfo.frequency > 0" class="chip-frequency">
                      {{ osInfo.cpuInfo.frequency.toFixed(1) }} GHz
                    </span>
                  </div>
                </template>
                <div class="info-empty" v-else>未知</div>
              </div>
            </div>
          </div>

          <!-- 内存信息 -->
          <div class="hardware-item">
            <div class="hardware-icon memory" :class="{ 'loading': getMemoryStatus() === 'loading', 'error': getMemoryStatus() === 'error' }">
              <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none">
                <path d="M4 6h16a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1z" />
                <path d="M8 6v12" />
                <path d="M16 6v12" />
                <path d="M4 11h16" />
                <path d="M4 15h16" />
              </svg>
            </div>
            <div class="hardware-content">
              <div class="hardware-header">
                <span class="hardware-title">内存</span>
                <div class="hardware-status">
                  <a-icon v-if="checkMemoryStatus('success')" type="check-circle" class="status-icon success" />
                  <a-icon v-else-if="checkMemoryStatus('error')" type="close-circle" class="status-icon error" />
                  <a-icon v-else-if="checkMemoryStatus('loading')" type="loading" class="status-icon loading" spin />
                  <a-icon v-else type="clock-circle" class="status-icon pending" />
                </div>
              </div>
              <div class="hardware-info" v-if="getMemoryStatus() === 'loading'">
                <div class="apple-hardware-loading memory-loading">
                  <div class="apple-loading-progress">
                    <div class="apple-loading-progress-bar"></div>
                  </div>
                </div>
                <div class="loading-text memory-loading-text">正在检测内存配置...</div>
              </div>
              <div class="hardware-info error" v-else-if="getMemoryStatus() === 'error'">
                获取内存信息失败
              </div>
              <div class="hardware-info" v-else>
                <template v-if="osInfo && osInfo.memoryInfo && osInfo.memoryInfo.totalMemory">
                  <div class="info-primary">
                    {{ osInfo.memoryInfo.totalMemory }} GB 内存
                  </div>
                  <div class="info-secondary">
                    已用 {{ calculateUsedMemory().toFixed(1) }} GB，可用 {{ osInfo.memoryInfo.availableMemory || 0 }} GB
                  </div>
                  <div class="usage-bar-container">
                    <div class="usage-bar-header">
                      <span>使用率 {{ calculateMemoryUsagePercent() }}%</span>
                      <span>{{ calculateUsedMemory().toFixed(1) }}/{{ osInfo.memoryInfo.totalMemory }} GB</span>
                    </div>
                    <div class="usage-bar">
                      <div 
                        class="usage-bar-fill"
                        :style="{
                          width: `${calculateMemoryUsagePercent()}%`,
                          backgroundColor: getUsageColor(parseFloat(calculateMemoryUsagePercent()))
                        }"
                      ></div>
                    </div>
                  </div>
                </template>
                <div class="info-empty" v-else>未知</div>
              </div>
            </div>
          </div>

          <!-- 磁盘信息 -->
          <div class="hardware-item">
            <div class="hardware-icon disk" :class="{ 'loading': getDiskStatus() === 'loading', 'error': getDiskStatus() === 'error' }">
              <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none">
                <circle cx="12" cy="12" r="9" />
                <circle cx="12" cy="12" r="3" />
                <path d="M12 3v3" />
                <path d="M3 12h3" />
                <path d="M12 18v3" />
                <path d="M18 12h3" />
              </svg>
            </div>
            <div class="hardware-content">
              <div class="hardware-header">
                <span class="hardware-title">磁盘</span>
                <div class="hardware-status">
                  <a-icon v-if="checkDiskStatus('success')" type="check-circle" class="status-icon success" />
                  <a-icon v-else-if="checkDiskStatus('error')" type="close-circle" class="status-icon error" />
                  <a-icon v-else-if="checkDiskStatus('loading')" type="loading" class="status-icon loading" spin />
                  <a-icon v-else type="clock-circle" class="status-icon pending" />
                </div>
              </div>
              <div class="hardware-info" v-if="getDiskStatus() === 'loading'">
                <div class="apple-hardware-loading disk-loading">
                  <div class="apple-loading-circle">
                    <div class="apple-loading-circle-inner"></div>
                  </div>
                  <div class="apple-loading-line line3"></div>
                </div>
                <div class="loading-text disk-loading-text">正在扫描存储空间...</div>
              </div>
              <div class="hardware-info error" v-else-if="getDiskStatus() === 'error'">
                获取磁盘信息失败
              </div>
              <div class="hardware-info" v-else>
                <template v-if="osInfo && osInfo.diskInfo && osInfo.diskInfo.totalDiskSpace">
                  <div class="info-primary">
                    {{ osInfo.diskInfo.totalDiskSpace.toFixed(1) }} GB 存储空间
                  </div>
                  <div class="info-secondary">
                    已用 {{ (osInfo.diskInfo.usedDiskSpace || 0).toFixed(1) }} GB，可用 {{ (osInfo.diskInfo.availableDiskSpace || 0).toFixed(1) }} GB
                  </div>
                  <div class="usage-bar-container">
                    <div class="usage-bar-header">
                      <span>使用率 {{ calculateDiskUsagePercent() }}%</span>
                      <span>{{ (osInfo.diskInfo.usedDiskSpace || 0).toFixed(1) }}/{{ osInfo.diskInfo.totalDiskSpace.toFixed(1) }} GB</span>
                    </div>
                    <div class="usage-bar">
                      <div 
                        class="usage-bar-fill"
                        :style="{
                          width: `${calculateDiskUsagePercent()}%`,
                          backgroundColor: getUsageColor(parseFloat(calculateDiskUsagePercent()))
                        }"
                      ></div>
                    </div>
                  </div>
                </template>
                <div class="info-empty" v-else>未知</div>
              </div>
            </div>
          </div>

          <!-- GPU信息 -->
          <div class="hardware-item">
            <div class="hardware-icon gpu" :class="{ 'loading': getGpuStatus() === 'loading', 'error': getGpuStatus() === 'error' }">
              <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none">
                <rect x="2" y="4" width="20" height="16" rx="2" />
                <path d="M6 8h4v8H6z" />
                <path d="M14 8h4" />
                <path d="M14 12h4" />
                <path d="M14 16h4" />
              </svg>
            </div>
            <div class="hardware-content">
              <div class="hardware-header">
                <span class="hardware-title">图形处理器</span>
                <div class="hardware-status">
                  <a-icon v-if="checkGpuStatus('success')" type="check-circle" class="status-icon success" />
                  <a-icon v-else-if="checkGpuStatus('error')" type="close-circle" class="status-icon error" />
                  <a-icon v-else-if="checkGpuStatus('loading')" type="loading" class="status-icon loading" spin />
                  <a-icon v-else type="clock-circle" class="status-icon pending" />
                </div>
              </div>
              <div class="hardware-info" v-if="getGpuStatus() === 'loading'">
                <div class="apple-hardware-loading gpu-loading">
                  <div class="apple-loading-grid">
                    <div class="apple-loading-cell"></div>
                    <div class="apple-loading-cell"></div>
                    <div class="apple-loading-cell"></div>
                    <div class="apple-loading-cell"></div>
                  </div>
                </div>
                <div class="loading-text gpu-loading-text">正在检测图形处理器...</div>
              </div>
              <div class="hardware-info error" v-else-if="getGpuStatus() === 'error'">
                获取GPU信息失败
              </div>
              <div class="hardware-info" v-else>
                <template v-if="hasValidGpuInfo">
                  <div class="info-primary">{{ getGpuDisplayInfo() }}</div>
                  <div class="info-secondary" v-if="hasGpuMemory">
                    {{ getGpuMemorySize() }} GB 显存
                  </div>
                </template>
                <div class="info-empty" v-else>未检测到GPU设备</div>
              </div>
            </div>
          </div>

          <!-- 交换空间信息 -->
          <div class="hardware-item">
            <div class="hardware-icon swap" :class="{ 'loading': getSwapStatus() === 'loading', 'error': getSwapStatus() === 'error' }">
              <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none">
                <path d="M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z" />
                <path d="M7 8h10" />
                <path d="M7 12h10" />
                <path d="M7 16h10" />
                <path d="M17 8l-3 8" />
                <path d="M10 16l-3-8" />
              </svg>
            </div>
            <div class="hardware-content">
              <div class="hardware-header">
                <span class="hardware-title">交换空间</span>
                <div class="hardware-status">
                  <a-icon v-if="checkSwapStatus('success')" type="check-circle" class="status-icon success" />
                  <a-icon v-else-if="checkSwapStatus('error')" type="close-circle" class="status-icon error" />
                  <a-icon v-else-if="checkSwapStatus('loading')" type="loading" class="status-icon loading" spin />
                  <a-icon v-else type="clock-circle" class="status-icon pending" />
                </div>
              </div>
              <div class="hardware-info" v-if="getSwapStatus() === 'loading'">
                <div class="loading-indicator">
                  <span></span><span></span><span></span>
                </div>
                <span class="loading-text">正在收集交换空间信息...</span>
              </div>
              <div class="hardware-info error" v-else-if="getSwapStatus() === 'error'">
                获取交换空间信息失败
              </div>
              <div class="hardware-info" v-else>
                <template v-if="hasSwapEnabled">
                  <div class="info-primary">
                    {{ osInfo.swapInfo.totalSwapFormatted }} {{ osInfo.swapInfo.totalSwapUnit }} 交换空间
                  </div>
                  <div class="info-secondary">
                    已用 {{ osInfo.swapInfo.usedSwapFormatted }} {{ osInfo.swapInfo.usedSwapUnit }}，可用 {{ osInfo.swapInfo.availableSwapFormatted }} {{ osInfo.swapInfo.availableSwapUnit }}
                  </div>
                  <div 
                    class="usage-bar-container" 
                    v-if="hasValidSwapSize"
                  >
                    <div class="usage-bar-header">
                      <span>使用率 {{ calculateSwapUsagePercent() }}%</span>
                      <span>{{ osInfo.swapInfo.usedSwapFormatted }}/{{ osInfo.swapInfo.totalSwapFormatted }} {{ osInfo.swapInfo.totalSwapUnit }}</span>
                    </div>
                    <div class="usage-bar">
                      <div 
                        class="usage-bar-fill"
                        :style="{
                          width: `${calculateSwapUsagePercent()}%`,
                          backgroundColor: getUsageColor(parseFloat(calculateSwapUsagePercent()))
                        }"
                      ></div>
                    </div>
                  </div>
                </template>
                <div class="info-empty" v-else>未配置交换空间</div>
              </div>
            </div>
          </div>

          <!-- 网络信息 -->
          <div class="hardware-item">
            <div class="hardware-icon network" :class="{ 'loading': getNetworkStatus() === 'loading', 'error': getNetworkStatus() === 'error' }">
              <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none">
                <path d="M5 12.55a11 11 0 0 1 14.08 0" />
                <path d="M1.42 9a16 16 0 0 1 21.16 0" />
                <path d="M8.53 16.11a6 6 0 0 1 6.95 0" />
                <circle cx="12" cy="20" r="1" />
              </svg>
            </div>
            <div class="hardware-content">
              <div class="hardware-header">
                <span class="hardware-title">网络</span>
                <div class="hardware-status">
                  <a-icon v-if="checkNetworkStatus('success')" type="check-circle" class="status-icon success" />
                  <a-icon v-else-if="checkNetworkStatus('error')" type="close-circle" class="status-icon error" />
                  <a-icon v-else-if="checkNetworkStatus('loading')" type="loading" class="status-icon loading" spin />
                  <a-icon v-else type="clock-circle" class="status-icon pending" />
                </div>
              </div>
              <div class="hardware-info" v-if="getNetworkStatus() === 'loading'">
                <div class="loading-indicator">
                  <span></span><span></span><span></span>
                </div>
                <span class="loading-text">正在收集网络信息...</span>
              </div>
              <div class="hardware-info error" v-else-if="getNetworkStatus() === 'error'">
                获取网络信息失败
              </div>
              <div class="hardware-info" v-else>
                <template v-if="osInfo && osInfo.networkInfo && osInfo.networkInfo.interfaces && osInfo.networkInfo.interfaces.length > 0">
                  <div class="info-primary">
                    {{ osInfo.networkInfo.interfaces.length }} 个网络接口
                  </div>
                  <div class="info-secondary">
                    <div v-for="(iface, index) in osInfo.networkInfo.interfaces" :key="index" class="network-interface">
                      <div class="interface-name">
                        {{ iface.name }}
                        <span class="interface-status" :class="{ 'up': iface.enabled }">
                          {{ iface.enabled ? '已连接' : '未连接' }}
                        </span>
                      </div>
                      <div class="interface-details">
                        <span v-if="iface.ipv4Address" class="ip-address">{{ iface.ipv4Address }}</span>
                        <span v-if="iface.macAddress" class="mac-address">{{ iface.macAddress }}</span>
                        <span v-if="iface.speed" class="speed">{{ formatSpeed(iface.speed) }}</span>
                        <span v-if="iface.model" class="model">{{ iface.model }}</span>
                      </div>
                      <div class="interface-stats" v-if="iface.bytesSent || iface.bytesReceived">
                        <span class="tx">发送: {{ formatBytes(iface.bytesSent) }}</span>
                        <span class="rx">接收: {{ formatBytes(iface.bytesReceived) }}</span>
                      </div>
                    </div>
                  </div>
                </template>
                <div class="info-empty" v-else>未知</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'OsFloatingCard',
  props: {
    osInfo: {
      type: Object,
      required: true
    },
    cpuStatus: {
      type: String,
      default: 'pending'
    },
    memoryStatus: {
      type: String,
      default: 'pending'
    },
    diskStatus: {
      type: String,
      default: 'pending'
    },
    swapStatus: {
      type: String,
      default: 'pending'
    },
    gpuStatus: {
      type: String,
      default: 'pending'
    }
  },
  computed: {
    // GPU相关计算属性
    hasValidGpuInfo() {
      return this.osInfo && 
             this.osInfo.gpuInfo && 
             ((this.osInfo.gpuInfo.info && this.osInfo.gpuInfo.info !== '未检测到GPU设备') || 
              this.osInfo.gpuInfo.model);
    },
    hasGpuMemory() {
      return this.osInfo && 
             this.osInfo.gpuInfo && 
             this.osInfo.gpuInfo.memorySize && 
             this.osInfo.gpuInfo.memorySize > 0;
    },
    // 交换空间相关计算属性
    hasSwapEnabled() {
      return this.osInfo && 
             this.osInfo.swapInfo && 
             this.osInfo.swapInfo.enabled;
    },
    hasValidSwapSize() {
      return this.hasSwapEnabled && 
             this.osInfo.swapInfo.totalSwap && 
             this.osInfo.swapInfo.totalSwap > 0;
    }
  },
  methods: {
    // 获取CPU状态
    getCpuStatus() {
      // 如果osInfo为空或者没有cpuInfo但是状态不是error，则显示loading
      if ((!this.osInfo || !this.osInfo.cpuInfo) && this.cpuStatus !== 'error') {
        return 'loading';
      }
      
      // 首先尝试从osInfo.cpuStatus获取
      if (this.osInfo && this.osInfo.cpuStatus) {
        return this.osInfo.cpuStatus.toLowerCase();
      }
      
      // 如果osInfo中没有cpuStatus，则尝试从osInfo.cpuInfo.status获取
      if (this.osInfo && this.osInfo.cpuInfo && this.osInfo.cpuInfo.status) {
        return this.osInfo.cpuInfo.status.toLowerCase();
      }
      
      // 如果都没有，则使用props中的cpuStatus
      return this.cpuStatus ? this.cpuStatus.toLowerCase() : 'loading';
    },
    
    // 检查CPU状态是否等于指定状态
    checkCpuStatus(status) {
      return this.getCpuStatus() === status.toLowerCase();
    },
    
    // 原有的checkStatus方法保持不变，以兼容其他硬件项
    checkStatus(status, target) {
      if (target === 'success') {
        return status === 'success';
      }
      if (target === 'error') {
        return status === 'error';
      }
      if (target === 'loading') {
        return status === 'loading';
      }
      if (target === 'pending') {
        return status === 'pending';
      }
      return false;
    },
    getOsName(osInfo) {
      if (!osInfo) return '未知操作系统';
      
      // 优先使用fullName
      if (osInfo.fullName) {
        return osInfo.fullName;
      }
      
      // 其次使用distribution
      else if (osInfo.distribution) {
        return osInfo.distribution;
      }
      
      return '未知操作系统';
    },
    getOsIconPath(osType) {
      try {
        if (!osType) return require('@/assets/img/os-logos/linux-tux.svg');
        
        // 根据osInfo判断操作系统类型
        const distType = (osType.distributionType || '').toLowerCase();
        const distId = (osType.distributionId || '').toLowerCase();
        const distName = (osType.distribution || '').toLowerCase();
        
        // 确定主操作系统类型
        let osIconType = 'linux';
        
        if (distType === 'centos' || distId === 'centos' || distName.includes('centos')) {
          osIconType = 'centos';
        } else if (distType === 'ubuntu' || distId === 'ubuntu' || distName.includes('ubuntu')) {
          osIconType = 'ubuntu';
        } else if (distType === 'debian' || distId === 'debian' || distName.includes('debian')) {
          osIconType = 'debian';
        } else if (distType === 'redhat' || distId === 'redhat' || distName.includes('redhat')) {
          osIconType = 'redhat';
        } else if (distType === 'kylin' || distId === 'kylin' || distName.includes('kylin')) {
          osIconType = 'kylin';
        } else if (distType === 'alpine' || distId === 'alpine' || distName.includes('alpine')) {
          osIconType = 'alpine';
        }
        
        // 使用switch语句根据操作系统类型返回对应图标
        switch (osIconType) {
          case 'centos':
            return require('@/assets/img/os-logos/centos.svg');
          case 'ubuntu':
            return require('@/assets/img/os-logos/ubuntu.svg');
          case 'debian':
            return require('@/assets/img/os-logos/debian.svg');
          case 'redhat':
            return require('@/assets/img/os-logos/redhat.svg');
          case 'kylin':
            return require('@/assets/img/os-logos/kylin.png');
          case 'alpine':
            return require('@/assets/img/os-logos/alpine.svg');
          default:
            return require('@/assets/img/os-logos/linux-tux.svg');
        }
      } catch (error) {
        // 如果找不到图标文件，返回内置的数据URI
        return 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCA0OCA0OCIgZmlsbD0ibm9uZSI+PHJlY3Qgd2lkdGg9IjQ4IiBoZWlnaHQ9IjQ4IiByeD0iOCIgZmlsbD0iI2YwZjBmMCIvPjxwYXRoIGQ9Ik0yMy41IDE0QzIzLjUgMTIuMzQzMSAyNC44NDMxIDExIDI2LjUgMTFDMjguMTU2OSAxMSAyOS41IDEyLjM0MzEgMjkuNSAxNFYxNy42NzY4QzMwLjQ5MzcgMTguMTA3MiAzMS4zNjc0IDE4Ljc4NTUgMzIgMTkuNjMyVjE0QzMyIDExLjIzODYgMjkuNzYxNCA5IDI3IDlDMjQuMjM4NiA5IDIyIDExLjIzODYgMjIgMTRWMTkuNjM0QzIyLjYzMzEgMTguNzg2MSAyMy41MDc0IDE4LjEwNzEgMjQuNSAxNy42NzZWMTRIMjMuNVoiIGZpbGw9IiM1MjUyNTIiLz48cGF0aCBkPSJNMzEuOTk5OCAyOC45OUMzMi4wMDE4IDI5LjYzODkgMzEuODA3MSAzMC4yNzMzIDMxLjQ0MjkgMzAuODAyQzMxLjA3ODYgMzEuMzMwNyAzMC41NjAyIDMxLjczMDUgMjkuOTU5OCAzMS45NVYzNC43MkMzMi45MDc1IDM0LjEyMTMgMzUuMTAyIDMxLjM5NjYgMzUgMjguMjlDMzQuODk3OSAyNS4xODM0IDMyLjU1OTYgMjIuNjM5MiAyOS41IDIyLjI1VjE5LjI4QzI5LjUgMTkuMjggMzggMjEuMjggMzggMjlDMzggMzYuNzIgMjkuNTUgMzggMjkuNTUgMzhIMTkuMDNDMTkuMDMgMzggMTAuNTIgMzcuMjkgMTAuMDIgMjcuNzhDOS42OCAxOS43OSAxOS41IDE4LjI3IDE5LjUgMTguMjdWMjEuMjdDMTkuNSAyMS4yNyAxMy4wMDk4IDIyLjYxIDE0LjAyIDE5QzE1LjUgMTQgMjQuOTk5OCAxNCAyNC45OTk4IDE0QzI0Ljk5OTggMTQgMjYuOTk5OCAxNCAyOS4wMDA3IDE0Ljk5QzI5LjAwMDcgMTQuOTkgMjguOTUxNCAxNi42OTMxIDI4LjAyMDcgMTcuODJDMjUuNjgwNyAxOC40OSAyMyAyMC41MSAyMyAyNC41QzIzIDI5LjE1IDI3LjAwMDIgMzAuMTcgMjcuMDAwMiAzMS4yNVYzNC42NkMyMi42NDczIDM0LjMzMDMgMTkuMTk5MSAzMC42NjAzIDE5LjAxOTggMjYuMDZDMTkuMDE5OCAyNS44NiAxOS4wMTk4IDI1LjY2IDE5LjAxOTggMjUuNDZDMTkuMDE5OCAyMy42OTQ1IDE5LjYzOTQgMjEuOTkxMiAyMC43Mzk3IDIwLjY4MTdDMjEuODQwMSAxOS4zNzIyIDIzLjM0NDIgMTguNTUxNiAyNC45OTk4IDE4LjQyVjIyLjE5QzIzLjI4MTQgMjIuNDA1NiAyMS44NTA1IDIzLjU0MTMgMjEuMzcwOSAyNS4xN0MyMi4yMTc3IDI3LjY0MjcgMjQuNzY1OCAyOS4xMjI0IDI3LjI3MDcgMjguNThDMjcuNzk5OSAyOC40NiAyOC4zMTYyIDI4LjI5MTQgMjguODE4MyAyOC4wOEMyOS4wNTQ5IDI3Ljk4ODYgMjkuMzE2MyAyNy45OTk3IDI5LjU0OCAyOC4xMTFDMjkuNzc5NyAyOC4yMjIzIDI5Ljk2MDIgMjguNDI1MiAzMC4wNCAyOC42OEMzMC4yMSAyOS4xNTcgMzAuMzI0NCAyOS42NTMzIDMwLjM4MTcgMzAuMTU3M0MzMC41MDUzIDI5LjY3MjggMzAuNTA4NSAyOS4xNTgxIDMwLjM5MDkgMjguNjcyQzMwLjM5MDkgMjguNjcyIDMxLjk5OTggMjguOTkgMzEuOTk5OCAyOC45OVoiIGZpbGw9IiM1MjUyNTIiLz48L3N2Zz4=';
      }
    },
    getUsageColor(percentage) {
      if (percentage > 90) return '#FF3B30';  // 危险
      if (percentage > 70) return '#FF9500';  // 警告
      return '#34C759';  // 正常
    },
    calculateThreadsPerCore() {
      if (!this.osInfo || !this.osInfo.cpuInfo) return 1;
      
      const cpuInfo = this.osInfo.cpuInfo;
      const cores = cpuInfo.cores || 1;
      const physicalCount = cpuInfo.physicalCount || 1;
      const logicalCores = cpuInfo.logicalCores || cores * physicalCount;
      
      // 避免除零错误
      const totalCores = cores * physicalCount;
      if (totalCores <= 0) return 1;
      
      return Math.max(1, Math.round(logicalCores / totalCores));
    },
    calculateUsedMemory() {
      if (!this.osInfo || !this.osInfo.memoryInfo) return 0;
      
      const memInfo = this.osInfo.memoryInfo;
      const total = memInfo.totalMemory || 0;
      const available = memInfo.availableMemory || 0;
      
      return Math.max(0, total - available);
    },
    calculateMemoryUsagePercent() {
      if (!this.osInfo || !this.osInfo.memoryInfo) return "0.0";
      
      const memInfo = this.osInfo.memoryInfo;
      
      // 优先使用后端计算的使用率
      if (memInfo.usagePercent != null) {
        return memInfo.usagePercent.toFixed(1);
      }
      
      const total = memInfo.totalMemory || 0;
      // 避免除零错误
      if (total <= 0) return "0.0";
      
      const available = memInfo.availableMemory || 0;
      const usagePercent = 100 * (1 - available / total);
      
      return usagePercent.toFixed(1);
    },
    calculateDiskUsagePercent() {
      if (!this.osInfo || !this.osInfo.diskInfo) return "0.0";
      
      const diskInfo = this.osInfo.diskInfo;
      
      // 优先使用后端计算的使用率
      if (diskInfo.usagePercent != null) {
        return diskInfo.usagePercent.toFixed(1);
      }
      
      const total = diskInfo.totalDiskSpace || 0;
      // 避免除零错误
      if (total <= 0) return "0.0";
      
      const used = diskInfo.usedDiskSpace || 0;
      const usagePercent = 100 * used / total;
      
      return usagePercent.toFixed(1);
    },
    // GPU相关方法
    getGpuDisplayInfo() {
      if (!this.osInfo || !this.osInfo.gpuInfo) return "未知GPU";
      return this.osInfo.gpuInfo.model || this.osInfo.gpuInfo.info || "未知GPU";
    },
    getGpuMemorySize() {
      if (!this.hasGpuMemory) return 0;
      return this.osInfo.gpuInfo.memorySize.toFixed(1);
    },
    // 交换空间相关方法
    getSwapTotal() {
      if (!this.hasSwapEnabled) return 0;
      return parseFloat(this.osInfo.swapInfo.totalSwapFormatted || 0);
    },
    getSwapAvailable() {
      if (!this.hasSwapEnabled) return 0;
      return parseFloat(this.osInfo.swapInfo.availableSwapFormatted || 0);
    },
    getSwapUsed() {
      if (!this.hasSwapEnabled) return 0;
      return parseFloat(this.osInfo.swapInfo.usedSwapFormatted || 0);
    },
    calculateSwapUsagePercent() {
      if (!this.hasSwapEnabled) return "0.0";
      
      const swapInfo = this.osInfo.swapInfo;
      
      // 优先使用后端计算的使用率
      if (swapInfo.usagePercent != null) {
        return swapInfo.usagePercent.toFixed(1);
      }
      
      const total = swapInfo.totalSwap || 0;
      // 避免除零错误
      if (total <= 0) return "0.0";
      
      const available = swapInfo.availableSwap || 0;
      const usagePercent = 100 * (1 - available / total);
      
      return usagePercent.toFixed(1);
    },
    // 获取内存状态
    getMemoryStatus() {
      // 如果osInfo为空或者没有memoryInfo但是状态不是error，则显示loading
      if ((!this.osInfo || !this.osInfo.memoryInfo) && this.memoryStatus !== 'error') {
        return 'loading';
      }
      
      // 首先尝试从osInfo.memoryStatus获取
      if (this.osInfo && this.osInfo.memoryStatus) {
        return this.osInfo.memoryStatus.toLowerCase();
      }
      
      // 如果osInfo中没有memoryStatus，则尝试从osInfo.memoryInfo.status获取
      if (this.osInfo && this.osInfo.memoryInfo && this.osInfo.memoryInfo.status) {
        return this.osInfo.memoryInfo.status.toLowerCase();
      }
      
      // 如果都没有，则使用props中的memoryStatus
      return this.memoryStatus ? this.memoryStatus.toLowerCase() : 'loading';
    },
    
    // 检查内存状态是否等于指定状态
    checkMemoryStatus(status) {
      return this.getMemoryStatus() === status.toLowerCase();
    },
    
    // 获取磁盘状态
    getDiskStatus() {
      // 如果osInfo为空或者没有diskInfo但是状态不是error，则显示loading
      if ((!this.osInfo || !this.osInfo.diskInfo) && this.diskStatus !== 'error') {
        return 'loading';
      }
      
      // 首先尝试从osInfo.diskStatus获取
      if (this.osInfo && this.osInfo.diskStatus) {
        return this.osInfo.diskStatus.toLowerCase();
      }
      
      // 如果osInfo中没有diskStatus，则尝试从osInfo.diskInfo.status获取
      if (this.osInfo && this.osInfo.diskInfo && this.osInfo.diskInfo.status) {
        return this.osInfo.diskInfo.status.toLowerCase();
      }
      
      // 如果都没有，则使用props中的diskStatus
      return this.diskStatus ? this.diskStatus.toLowerCase() : 'loading';
    },
    
    // 检查磁盘状态是否等于指定状态
    checkDiskStatus(status) {
      return this.getDiskStatus() === status.toLowerCase();
    },
    
    // 获取GPU状态
    getGpuStatus() {
      // 如果osInfo为空或者没有gpuInfo但是状态不是error，则显示loading
      if ((!this.osInfo || !this.osInfo.gpuInfo) && this.gpuStatus !== 'error') {
        return 'loading';
      }
      
      // 首先尝试从osInfo.gpuStatus获取
      if (this.osInfo && this.osInfo.gpuStatus) {
        return this.osInfo.gpuStatus.toLowerCase();
      }
      
      // 如果osInfo中没有gpuStatus，则尝试从osInfo.gpuInfo.status获取
      if (this.osInfo && this.osInfo.gpuInfo && this.osInfo.gpuInfo.status) {
        return this.osInfo.gpuInfo.status.toLowerCase();
      }
      
      // 如果都没有，则使用props中的gpuStatus
      return this.gpuStatus ? this.gpuStatus.toLowerCase() : 'loading';
    },
    
    // 检查GPU状态是否等于指定状态
    checkGpuStatus(status) {
      return this.getGpuStatus() === status.toLowerCase();
    },
    
    // 获取交换空间状态
    getSwapStatus() {
      // 如果osInfo为空或者没有swapInfo但是状态不是error，则显示loading
      if ((!this.osInfo || !this.osInfo.swapInfo) && this.swapStatus !== 'error') {
        return 'loading';
      }
      
      // 首先尝试从osInfo.swapStatus获取
      if (this.osInfo && this.osInfo.swapStatus) {
        return this.osInfo.swapStatus.toLowerCase();
      }
      
      // 如果osInfo中没有swapStatus，则尝试从osInfo.swapInfo.status获取
      if (this.osInfo && this.osInfo.swapInfo && this.osInfo.swapInfo.status) {
        return this.osInfo.swapInfo.status.toLowerCase();
      }
      
      // 如果都没有，则使用props中的swapStatus
      return this.swapStatus ? this.swapStatus.toLowerCase() : 'loading';
    },
    
    // 检查交换空间状态是否等于指定状态
    checkSwapStatus(status) {
      return this.getSwapStatus() === status.toLowerCase();
    },
    // 获取网络状态
    getNetworkStatus() {
      // 如果osInfo为空或者没有networkInfo但是没有明确表示networkStatus是error，则显示loading
      if ((!this.osInfo || !this.osInfo.networkInfo) && 
          !(this.osInfo && this.osInfo.networkStatus === 'error')) {
        return 'loading';
      }
      
      // 首先尝试从osInfo.networkStatus获取
      if (this.osInfo && this.osInfo.networkStatus) {
        return this.osInfo.networkStatus.toLowerCase();
      }
      
      // 如果osInfo中没有networkStatus，则尝试从osInfo.networkInfo.status获取
      if (this.osInfo && this.osInfo.networkInfo && this.osInfo.networkInfo.status) {
        return this.osInfo.networkInfo.status.toLowerCase();
      }
      
      // 如果都没有，则默认为loading
      return 'loading';
    },
    
    // 检查网络状态
    checkNetworkStatus(status) {
      return this.getNetworkStatus() === status.toLowerCase();
    },
    
    formatSpeed(speed) {
      if (!speed || speed <= 0) return '';
      if (speed >= 1000) {
        return `${(speed/1000).toFixed(1)} Gbps`;
      }
      return `${speed} Mbps`;
    },
    formatBytes(bytes) {
      if (!bytes || bytes <= 0) return '0 B';
      const units = ['B', 'KB', 'MB', 'GB', 'TB'];
      let size = bytes;
      let unitIndex = 0;
      while (size >= 1024 && unitIndex < units.length - 1) {
        size /= 1024;
        unitIndex++;
      }
      return `${size.toFixed(2)} ${units[unitIndex]}`;
    }
  }
};
</script>

<style lang="less" scoped>
// Apple风格变量定义
@apple-blue: #007AFF;
@apple-blue-hover: #0062CC;
@apple-red: #FF3B30;
@apple-red-hover: #D82D22;
@apple-green: #34C759;
@apple-orange: #FF9500;
@apple-purple: #AF52DE;
@apple-gray-light: #F2F2F7;
@apple-gray: #AEAEB2;
@apple-gray-dark: #8E8E93;
@apple-black: #1D1D1F;
@apple-white: #FFFFFF;

// 操作系统详情弹出框样式
.os-detail-tooltip {
  max-width: none !important;
}

.os-detail-popup {
  padding: 0;
  min-width: 320px;
  max-width: 420px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.12), 0 2px 4px rgba(0, 0, 0, 0.05);
  background-color: #ffffff;
  animation: osFadeIn 0.3s ease-in-out;
  max-height: 80vh;
  overflow-y: auto;

  &::-webkit-scrollbar {
    width: 8px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background-color: rgba(0, 0, 0, 0.1);
    border-radius: 8px;
    border: 2px solid transparent;
    background-clip: content-box;
  }

  &::-webkit-scrollbar-thumb:hover {
    background-color: rgba(0, 0, 0, 0.2);
    border: 2px solid transparent;
    background-clip: content-box;
  }
}

// 重新设计的头部样式
.os-detail-header {
  position: relative;
  padding: 0;
  background: none;
  overflow: visible;
  margin-bottom: 12px;

  .os-detail-header-content {
    position: relative;
    z-index: 2;
    padding: 16px;
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .os-detail-header-blur {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(180deg, rgba(255,255,255,0.95) 0%, rgba(255,255,255,0.98) 100%);
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border-bottom: 1px solid rgba(0,0,0,0.05);
    z-index: 1;
  }

  .os-detail-icon-wrapper {
    margin-bottom: 8px;
    
    .os-detail-icon-container {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      background: linear-gradient(135deg, #ffffff, #f5f5f7);
      box-shadow: 0 2px 5px rgba(0,0,0,0.05);
      padding: 8px;
      display: flex;
      align-items: center;
      justify-content: center;

      .os-detail-icon {
        width: 32px;
        height: 32px;
        object-fit: contain;
      }
    }
  }

  .os-detail-info {
    .os-detail-name {
      margin-bottom: 8px;

      .os-name {
        font-size: 20px;
      }

      .os-version {
        font-size: 20px;
      }
    }

    .os-detail-meta {
      display: flex;
      flex-wrap: wrap;
      justify-content: center;
      gap: 16px;
      margin-top: 4px;

      .os-meta-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        min-width: 100px;

        .meta-label {
          font-size: 12px;
          color: #86868b;
          margin-bottom: 4px;
          text-transform: uppercase;
          letter-spacing: 0.5px;
        }

        .meta-value {
          font-size: 14px;
          color: #1d1d1f;
          font-weight: 500;
        }
      }
    }
  }
}

// 内容区域
.os-detail-content {
  padding: 0 24px 24px;
  background: #ffffff;
}

// 新增硬件信息部分样式
.os-detail-section {
  margin: 0;
  background: transparent;
  border-radius: 0;
  overflow: visible;
}

.section-header {
  padding: 8px 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: transparent;
  margin-bottom: 0;

  .section-title {
    font-size: 12px;
    font-weight: 500;
    color: #86868b;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }
}

.hardware-item {
  display: flex;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: transparent;
  transition: background-color 0.2s ease;

  &:hover {
    background: rgba(0, 0, 0, 0.02);
  }

  &:last-child {
    border-bottom: none;
  }
}

.hardware-icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  margin-right: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  color: #FFFFFF;

  svg {
    transition: transform 0.2s ease;
  }

  &:hover svg {
    transform: scale(1.1);
  }

  &.cpu {
    background: linear-gradient(135deg, #5856D6, #5E5CE6);
  }

  &.memory {
    background: linear-gradient(135deg, #32ADE6, #0A84FF);
  }

  &.disk {
    background: linear-gradient(135deg, #FF9F0A, #FF9500);
  }

  &.gpu {
    background: linear-gradient(135deg, #FF375F, #FF2D55);
  }

  &.swap {
    background: linear-gradient(135deg, #BF5AF2, #A04AD9);  // 紫色渐变
  }

  &.network {
    background: linear-gradient(135deg, #32ADE6, #0A84FF);
  }

  &.loading {
    animation: pulse 1.5s infinite;
    svg {
      animation: spin 2s linear infinite;
    }
  }

  &.error {
    background: linear-gradient(135deg, #FF453A, #FF3B30);
  }
}

.hardware-content {
  flex: 1;
  min-width: 0;
}

.hardware-header {
  margin-bottom: 4px;
}

.hardware-title {
  font-size: 13px;
}

.hardware-status {
  .status-icon {
    font-size: 14px;

    &.success { color: #34C759; }
    &.error { color: #FF3B30; }
    &.loading { color: #007AFF; }
    &.pending { color: #FF9500; }
  }
}

.hardware-info {
  &.loading {
    display: flex;
    align-items: center;
    color: #007AFF;
  }

  &.error {
    color: #FF3B30;
    font-size: 13px;
  }

  &.pending {
    color: #FF9500;
    font-size: 13px;
  }

  .info-primary {
    font-size: 13px;
    font-weight: 500;
    color: #1D1D1F;
    margin-bottom: 4px;
  }

  .info-secondary {
    font-size: 12px;
    color: #86868B;
    margin-bottom: 8px;
  }

  .info-empty {
    font-size: 13px;
    color: #86868B;
    font-style: italic;
  }

  .warning {
    display: flex;
    align-items: center;
    color: #FF9500;
  }
}

.loading-indicator {
  display: flex;
  align-items: center;
  margin-right: 8px;
  
  span {
    width: 4px;
    height: 4px;
    margin: 0 2px;
    background-color: #007AFF;
    border-radius: 50%;
    animation: bounce 1.4s infinite ease-in-out both;

    &:nth-child(1) { animation-delay: -0.32s; }
    &:nth-child(2) { animation-delay: -0.16s; }
  }
}

.usage-bar-container {
  margin-top: 6px;
}

.usage-bar {
  height: 3px;
}

.usage-bar-header {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #86868B;
  margin-bottom: 4px;
}

.usage-bar-fill {
  height: 100%;
  border-radius: 2px;
  transition: all 0.3s ease;
}

.chip-frequency {
  display: inline-block;
  padding: 2px 6px;
  background-color: rgba(0, 122, 255, 0.1);
  color: #007AFF;
  border-radius: 4px;
  font-size: 12px;
  margin-left: 6px;
}

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes osFadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.network-interface {
  margin-bottom: 8px;
  padding: 6px 8px;
  background-color: rgba(0, 0, 0, 0.02);
  border-radius: 6px;
  
  &:last-child {
    margin-bottom: 0;
  }
}

.interface-name {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
  margin-bottom: 4px;
  
  .interface-status {
    font-size: 12px;
    padding: 2px 6px;
    border-radius: 4px;
    background-color: rgba(142, 142, 147, 0.1);
    color: #8E8E93;
    
    &.up {
      background-color: rgba(52, 199, 89, 0.1);
      color: #34C759;
    }
  }
}

.interface-details {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
  color: #8E8E93;
  margin: 4px 0;
  
  .ip-address {
    color: #007AFF;
    font-family: monospace;
  }
  
  .mac-address {
    font-family: monospace;
  }
  
  .speed {
    color: #5856D6;
  }

  .model {
    color: #FF9500;
  }
}

.interface-stats {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: #8E8E93;
  margin-top: 4px;

  .tx {
    color: #34C759;
  }

  .rx {
    color: #FF3B30;
  }
}

/* 操作系统名称加载动画 */
.os-name-loading {
  display: flex;
  align-items: center;
  height: 24px;
  margin-bottom: 4px;
}

.os-loader-container {
  display: flex;
  align-items: center;
}

.os-loader-spinner {
  width: 16px;
  height: 16px;
  margin-right: 8px;
  position: relative;
}

.spinner-inner {
  width: 100%;
  height: 100%;
  border: 2px solid rgba(0, 122, 255, 0.2);
  border-top-color: #007AFF;
  border-radius: 50%;
  animation: spin 1s infinite linear;
}

.os-loading-text {
  font-size: 15px;
  font-weight: 500;
  color: #007AFF;
}

/* 操作系统版本加载动画 */
.os-version-loading {
  display: inline-block;
  margin-left: 8px;
  height: 18px;
  width: 50px;
  overflow: hidden;
  vertical-align: middle;
}

.version-loading-bar {
  width: 100%;
  height: 100%;
  border-radius: 4px;
  background: linear-gradient(90deg, rgba(0, 122, 255, 0.1), rgba(0, 122, 255, 0.2), rgba(0, 122, 255, 0.1));
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

/* 元数据加载动画 */
.meta-loading-pulse {
  width: 60px;
  height: 12px;
  border-radius: 3px;
  background: linear-gradient(90deg, #f2f2f7, #e5e5ea, #f2f2f7);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

/* 动画关键帧 */
@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

@keyframes shimmer {
  0% {
    background-position: -60px 0;
  }
  100% {
    background-position: 60px 0;
  }
}

/* Apple风格硬件信息加载动画 */
.apple-hardware-loading {
  padding: 8px 0;
  width: 100%;
}

/* CPU加载动画 */
.apple-loading-line {
  height: 10px;
  border-radius: 5px;
  background: linear-gradient(90deg, #f2f2f7, #e5e5ea, #f2f2f7);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  margin-bottom: 8px;
}

.apple-loading-line.line1 {
  width: 85%;
}

.apple-loading-line.line2 {
  width: 65%;
}

.apple-loading-line.line3 {
  width: 75%;
  margin-top: 8px;
}

/* 内存加载进度条动画 */
.memory-loading .apple-loading-progress {
  width: 100%;
  height: 36px;
  position: relative;
  overflow: hidden;
  background-color: rgba(0, 122, 255, 0.05);
  border-radius: 6px;
}

.apple-loading-progress-bar {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, 
    rgba(0, 122, 255, 0.0), 
    rgba(0, 122, 255, 0.1), 
    rgba(0, 122, 255, 0.2),
    rgba(0, 122, 255, 0.1),
    rgba(0, 122, 255, 0.0)
  );
  animation: progressBar 1.5s ease-in-out infinite;
}

/* 磁盘加载圆形动画 */
.disk-loading .apple-loading-circle {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: rgba(255, 149, 0, 0.05);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}

.apple-loading-circle-inner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 149, 0, 0.2);
  border-top-color: #FF9500;
  border-radius: 50%;
  animation: spin 1s infinite linear;
}

/* GPU加载网格动画 */
.gpu-loading .apple-loading-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-gap: 6px;
  width: 80px;
  margin-bottom: 8px;
}

.apple-loading-cell {
  height: 20px;
  border-radius: 4px;
  background-color: rgba(255, 45, 85, 0.1);
  animation: pulse 1.5s infinite alternate;
}

.apple-loading-cell:nth-child(1) {
  animation-delay: 0s;
}

.apple-loading-cell:nth-child(2) {
  animation-delay: 0.2s;
}

.apple-loading-cell:nth-child(3) {
  animation-delay: 0.4s;
}

.apple-loading-cell:nth-child(4) {
  animation-delay: 0.6s;
}

/* 交换空间加载动画 */
.hardware-info.loading .loading-indicator {
  display: flex;
  align-items: center;
  margin-right: 8px;
}

.hardware-info.loading .loading-indicator span {
  width: 4px;
  height: 4px;
  margin: 0 2px;
  background-color: #AF52DE;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.hardware-info.loading .loading-indicator span:nth-child(1) {
  animation-delay: -0.32s;
}

.hardware-info.loading .loading-indicator span:nth-child(2) {
  animation-delay: -0.16s;
}

.hardware-info.loading .loading-text {
  font-size: 13px;
  color: #AF52DE;
}

/* 网络加载动画 */
.hardware-icon.network.loading {
  animation: pulse 1.5s infinite alternate;
}

.hardware-icon.network.loading svg {
  animation: networkWave 1.5s ease-in-out infinite;
}

@keyframes networkWave {
  0% {
    transform: scale(0.8);
    opacity: 0.5;
  }
  50% {
    transform: scale(1.1);
    opacity: 1;
  }
  100% {
    transform: scale(0.8);
    opacity: 0.5;
  }
}

/* 动画关键帧 */
@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

@keyframes progressBar {
  0% { left: -100%; }
  100% { left: 100%; }
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@keyframes pulse {
  0% { opacity: 0.3; }
  100% { opacity: 0.8; }
}

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

/* CPU加载文字样式 */
.cpu-loading-text {
  font-size: 13px;
  color: #5856D6;
  margin-top: 5px;
}

/* 内存加载文字样式 */
.memory-loading-text {
  font-size: 13px;
  color: #0A84FF;
  margin-top: 8px;
}

/* 磁盘加载文字样式 */
.disk-loading-text {
  font-size: 13px;
  color: #FF9500;
  margin-top: 8px;
}

/* GPU加载文字样式 */
.gpu-loading-text {
  font-size: 13px;
  color: #FF2D55;
  margin-top: 8px;
}
</style> 