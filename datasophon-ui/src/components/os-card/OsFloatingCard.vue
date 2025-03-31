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
                :src="getOsIconPath(osInfo.distribution)"
                :alt="osInfo.distribution"
                class="os-detail-icon"
              />
            </div>
          </div>
          <div class="os-detail-info">
            <div class="os-detail-name">
              <span class="os-name">{{ osInfo.fullName || osInfo.distribution }}</span>
              <span class="os-version" v-if="osInfo.versionId">{{ osInfo.versionId }}</span>
            </div>
            <div class="os-detail-meta">
              <div class="os-meta-item" v-if="osInfo.architecture">
                <span class="meta-label">架构</span>
                <span class="meta-value">{{ osInfo.architecture }}</span>
              </div>
              <div class="os-meta-item" v-if="osInfo.kernelVersion">
                <span class="meta-label">内核版本</span>
                <span class="meta-value">{{ osInfo.kernelVersion }}</span>
              </div>
              <div class="os-meta-item" v-if="osInfo.hostname">
                <span class="meta-label">主机名</span>
                <span class="meta-value">{{ osInfo.hostname }}</span>
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
            <div class="hardware-icon cpu" :class="{ 'loading': cpuStatus === 'loading', 'error': cpuStatus === 'error' }">
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
                  <a-icon v-if="checkStatus(cpuStatus, 'success')" type="check-circle" class="status-icon success" />
                  <a-icon v-else-if="checkStatus(cpuStatus, 'error')" type="close-circle" class="status-icon error" />
                  <a-icon v-else-if="checkStatus(cpuStatus, 'loading')" type="loading" class="status-icon loading" spin />
                  <a-icon v-else type="clock-circle" class="status-icon pending" />
                </div>
              </div>
              <div class="hardware-info" v-if="cpuStatus === 'loading'">
                <div class="loading-indicator">
                  <span></span><span></span><span></span>
                </div>
                <span class="loading-text">正在收集CPU信息...</span>
              </div>
              <div class="hardware-info error" v-else-if="cpuStatus === 'error'">
                获取CPU信息失败
              </div>
              <div class="hardware-info pending" v-else-if="cpuStatus === 'pending'">
                等待收集CPU信息
              </div>
              <div class="hardware-info" v-else>
                <template v-if="osInfo && osInfo.cpuModel">
                  <div class="info-primary">{{ osInfo.cpuModel }}</div>
                  <div class="info-secondary">
                    {{ osInfo.cpuCount || 1 }} × {{ osInfo.cpuCores || 1 }} 核心 × {{ osInfo.cpuThreadsPerCore || 1 }} 线程
                    <span v-if="osInfo.cpuFrequency && osInfo.cpuFrequency > 0" class="chip-frequency">
                      {{ osInfo.cpuFrequency.toFixed(1) }} GHz
                    </span>
                  </div>
                </template>
                <div class="info-empty" v-else>未知</div>
              </div>
            </div>
          </div>

          <!-- 内存信息 -->
          <div class="hardware-item">
            <div class="hardware-icon memory" :class="{ 'loading': memoryStatus === 'loading', 'error': memoryStatus === 'error' }">
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
                  <a-icon v-if="checkStatus(memoryStatus, 'success')" type="check-circle" class="status-icon success" />
                  <a-icon v-else-if="checkStatus(memoryStatus, 'error')" type="close-circle" class="status-icon error" />
                  <a-icon v-else-if="checkStatus(memoryStatus, 'loading')" type="loading" class="status-icon loading" spin />
                  <a-icon v-else type="clock-circle" class="status-icon pending" />
                </div>
              </div>
              <div class="hardware-info" v-if="memoryStatus === 'loading'">
                <div class="loading-indicator">
                  <span></span><span></span><span></span>
                </div>
                <span class="loading-text">正在收集内存信息...</span>
              </div>
              <div class="hardware-info error" v-else-if="memoryStatus === 'error'">
                获取内存信息失败
              </div>
              <div class="hardware-info pending" v-else-if="memoryStatus === 'pending'">
                等待收集内存信息
              </div>
              <div class="hardware-info" v-else>
                <template v-if="osInfo && osInfo.totalMemory">
                  <div class="info-primary">
                    {{ osInfo.totalMemory }} GB 内存
                  </div>
                  <div class="info-secondary">
                    已用 {{ (osInfo.totalMemory - osInfo.availableMemory).toFixed(1) }} GB，可用 {{ osInfo.availableMemory }} GB
                  </div>
                  <div class="usage-bar-container">
                    <div class="usage-bar-header">
                      <span>使用率 {{ (100 * (1 - osInfo.availableMemory / osInfo.totalMemory)).toFixed(1) }}%</span>
                      <span>{{ (osInfo.totalMemory - osInfo.availableMemory).toFixed(1) }}/{{ osInfo.totalMemory }} GB</span>
                    </div>
                    <div class="usage-bar">
                      <div 
                        class="usage-bar-fill"
                        :style="{
                          width: `${(100 * (1 - osInfo.availableMemory / osInfo.totalMemory)).toFixed(1)}%`,
                          backgroundColor: getUsageColor(100 * (1 - osInfo.availableMemory / osInfo.totalMemory))
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
            <div class="hardware-icon disk" :class="{ 'loading': diskStatus === 'loading', 'error': diskStatus === 'error' }">
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
                  <a-icon v-if="checkStatus(diskStatus, 'success')" type="check-circle" class="status-icon success" />
                  <a-icon v-else-if="checkStatus(diskStatus, 'error')" type="close-circle" class="status-icon error" />
                  <a-icon v-else-if="checkStatus(diskStatus, 'loading')" type="loading" class="status-icon loading" spin />
                  <a-icon v-else type="clock-circle" class="status-icon pending" />
                </div>
              </div>
              <div class="hardware-info" v-if="diskStatus === 'loading'">
                <div class="loading-indicator">
                  <span></span><span></span><span></span>
                </div>
                <span class="loading-text">正在收集磁盘信息...</span>
              </div>
              <div class="hardware-info error" v-else-if="diskStatus === 'error'">
                获取磁盘信息失败
              </div>
              <div class="hardware-info pending" v-else-if="diskStatus === 'pending'">
                等待收集磁盘信息
              </div>
              <div class="hardware-info" v-else>
                <template v-if="osInfo && osInfo.totalDisk">
                  <div class="info-primary">
                    {{ osInfo.totalDisk }} GB 存储空间
                  </div>
                  <div class="info-secondary">
                    已用 {{ (osInfo.totalDisk - osInfo.availableDisk).toFixed(1) }} GB，可用 {{ osInfo.availableDisk }} GB
                  </div>
                  <div class="usage-bar-container">
                    <div class="usage-bar-header">
                      <span>使用率 {{ (100 * (1 - osInfo.availableDisk / osInfo.totalDisk)).toFixed(1) }}%</span>
                      <span>{{ (osInfo.totalDisk - osInfo.availableDisk).toFixed(1) }}/{{ osInfo.totalDisk }} GB</span>
                    </div>
                    <div class="usage-bar">
                      <div 
                        class="usage-bar-fill"
                        :style="{
                          width: `${(100 * (1 - osInfo.availableDisk / osInfo.totalDisk)).toFixed(1)}%`,
                          backgroundColor: getUsageColor(100 * (1 - osInfo.availableDisk / osInfo.totalDisk))
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
            <div class="hardware-icon gpu" :class="{ 'loading': gpuStatus === 'loading', 'error': gpuStatus === 'error' }">
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
                  <a-icon v-if="checkStatus(gpuStatus, 'success')" type="check-circle" class="status-icon success" />
                  <a-icon v-else-if="checkStatus(gpuStatus, 'error')" type="close-circle" class="status-icon error" />
                  <a-icon v-else-if="checkStatus(gpuStatus, 'loading')" type="loading" class="status-icon loading" spin />
                  <a-icon v-else type="clock-circle" class="status-icon pending" />
                </div>
              </div>
              <div class="hardware-info" v-if="gpuStatus === 'loading'">
                <div class="loading-indicator">
                  <span></span><span></span><span></span>
                </div>
                <span class="loading-text">正在收集GPU信息...</span>
              </div>
              <div class="hardware-info error" v-else-if="gpuStatus === 'error'">
                获取GPU信息失败
              </div>
              <div class="hardware-info pending" v-else-if="gpuStatus === 'pending'">
                等待收集GPU信息
              </div>
              <div class="hardware-info" v-else>
                <template v-if="osInfo && osInfo.gpuInfo && !osInfo.gpuInfo.startsWith('ERROR:') && osInfo.gpuInfo !== '未检测到GPU设备'">
                  <div class="info-primary">{{ osInfo.gpuInfo }}</div>
                  <div class="info-secondary" v-if="osInfo.gpuMemory && osInfo.gpuMemory > 0">
                    {{ osInfo.gpuMemory.toFixed(1) }} GB 显存
                  </div>
                </template>
                <div class="info-empty" v-else>未检测到GPU设备</div>
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
  methods: {
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
    getOsIconPath(osType) {
      try {
        const osLower = (osType || '').toLowerCase();
        if (osLower.includes('centos')) {
          return require('@/assets/img/os-logos/centos.svg');
        } else if (osLower.includes('ubuntu')) {
          return require('@/assets/img/os-logos/ubuntu.svg');
        } else if (osLower.includes('debian')) {
          return require('@/assets/img/os-logos/debian.svg');
        } else if (osLower.includes('redhat') || osLower.includes('red hat')) {
          return require('@/assets/img/os-logos/redhat.svg');
        } else if (osLower.includes('windows')) {
          return require('@/assets/img/os-logos/windows.svg');
        } else if (osLower.includes('kylin') || osLower.includes('麒麟')) {
          return require('@/assets/img/os-logos/kylin.svg');
        } else {
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
</style> 