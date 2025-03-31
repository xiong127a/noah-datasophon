<!--
 * 操作系统悬浮卡片组件
 * 这个文件包含了操作系统悬浮卡片的所有代码，用于在主机环境校验页面显示操作系统信息
-->
<template>
  <div class="os-detail-tooltip">
    <div class="os-detail-popup">
      <!-- 标题区域 -->
      <div class="os-detail-header">
        <div class="os-detail-icon-container">
          <img
            :src="getOsIconPath(osInfo.distribution)"
            :alt="osInfo.distribution"
            width="64"
            height="64"
            class="os-img-no-filter"
            style="filter: none !important; border-radius: 12px"
          />
        </div>
        <div class="os-detail-title-container">
          <h3 class="os-detail-title">{{ osInfo.fullName || `${osInfo.distribution} ${osInfo.versionId}` }}</h3>
          <div class="os-detail-subtitle" v-if="osInfo.kernelVersion">内核版本 {{ osInfo.kernelVersion }}</div>
          <div class="os-detail-subtitle" v-if="osInfo.architecture">{{ osInfo.architecture }} 架构</div>
        </div>
      </div>

      <!-- 内容区域包装元素 -->
      <div class="os-detail-content">
        <!-- 硬件信息卡片 -->
        <div class="os-detail-card">
          <div class="os-detail-card-header">
            <i class="anticon anticon-desktop" style="margin-right: 8px; color: #007AFF"></i>
            <span>硬件信息</span>
          </div>

          <!-- CPU信息 -->
          <div class="os-detail-info-row">
            <div class="os-detail-info-icon-container">
              <div
                class="os-detail-info-icon cpu"
                :style="{
                  backgroundColor: checkStatus(cpuStatus, 'success') ? 'rgba(52, 199, 89, 0.1)' :
                      checkStatus(cpuStatus, 'error') ? 'rgba(255, 59, 48, 0.1)' :
                          'rgba(0, 122, 255, 0.1)',
                  color: checkStatus(cpuStatus, 'success') ? '#34C759' :
                      checkStatus(cpuStatus, 'error') ? '#FF3B30' :
                          '#007AFF'
                }"
              >
                <i class="anticon anticon-api"></i>
              </div>
            </div>
            <div class="os-detail-info-content">
              <div class="os-detail-info-label">
                <span>处理器</span>
                <!-- 添加状态图标 -->
                <a-icon
                  v-if="checkStatus(cpuStatus, 'success')"
                  type="check-circle"
                  style="margin-left: 6px; color: #34C759; font-size: 12px"
                />
                <a-icon
                  v-else-if="checkStatus(cpuStatus, 'error')"
                  type="close-circle"
                  style="margin-left: 6px; color: #FF3B30; font-size: 12px"
                />
                <a-icon
                  v-else-if="checkStatus(cpuStatus, 'loading')"
                  type="loading"
                  style="margin-left: 6px; color: #007AFF; font-size: 12px"
                />
              </div>
              <div v-if="cpuStatus === 'loading'" class="os-detail-info-value loading">
                <div class="loading-animation"></div>
                <span>正在收集CPU信息...</span>
              </div>
              <div v-else-if="cpuStatus === 'error'" class="os-detail-info-value error">
                <a-icon type="warning" style="margin-right: 6px" />
                获取CPU信息失败
              </div>
              <div v-else-if="cpuStatus === 'pending'" class="os-detail-info-value waiting">
                <a-icon type="clock-circle" style="margin-right: 6px; color: #FAAD14" />
                等待收集CPU信息
              </div>
              <div v-else class="os-detail-info-value">
                <!-- 改为显示更多CPU详情，包括型号、数量、核心数和线程数 -->
                <div v-if="osInfo && osInfo.cpuModel">
                  <div style="font-weight: 500">{{ osInfo.cpuModel }}</div>
                  <div style="font-size: 12px; color: #666; margin-top: 4px">
                    <span>
                      {{ osInfo.cpuCount || 1 }} 个处理器 × {{ osInfo.cpuCores || 1 }} 核心/处理器 × {{ osInfo.cpuThreadsPerCore || 1 }} 线程/核心 = {{ osInfo.cpuLogicalCores || 1 }} 逻辑核心
                    </span>
                    <span v-if="osInfo.cpuFrequency && osInfo.cpuFrequency > 0" style="margin-left: 4px">
                      ({{ osInfo.cpuFrequency.toFixed(1) }} GHz)
                    </span>
                  </div>
                </div>
                <span v-else>未知</span>
              </div>
            </div>
          </div>

          <!-- 内存信息 -->
          <div class="os-detail-info-row">
            <div class="os-detail-info-icon-container">
              <div
                class="os-detail-info-icon memory"
                :style="{
                  backgroundColor: checkStatus(memoryStatus, 'success') ? 'rgba(52, 199, 89, 0.1)' :
                      checkStatus(memoryStatus, 'error') ? 'rgba(255, 59, 48, 0.1)' :
                          'rgba(0, 122, 255, 0.1)',
                  color: checkStatus(memoryStatus, 'success') ? '#34C759' :
                      checkStatus(memoryStatus, 'error') ? '#FF3B30' :
                          '#007AFF'
                }"
              >
                <i class="anticon anticon-database"></i>
              </div>
            </div>
            <div class="os-detail-info-content">
              <div class="os-detail-info-label">
                <span>内存</span>
                <!-- 添加状态图标 -->
                <a-icon
                  v-if="checkStatus(memoryStatus, 'success')"
                  type="check-circle"
                  style="margin-left: 6px; color: #34C759; font-size: 12px"
                />
                <a-icon
                  v-else-if="checkStatus(memoryStatus, 'error')"
                  type="close-circle"
                  style="margin-left: 6px; color: #FF3B30; font-size: 12px"
                />
                <a-icon
                  v-else-if="checkStatus(memoryStatus, 'loading')"
                  type="loading"
                  style="margin-left: 6px; color: #007AFF; font-size: 12px"
                />
              </div>
              <div v-if="memoryStatus === 'loading'" class="os-detail-info-value loading">
                <div class="loading-animation"></div>
                <span>正在收集内存信息...</span>
              </div>
              <div v-else-if="checkStatus(memoryStatus, 'error')" class="os-detail-info-value error">
                <a-icon type="warning" style="margin-right: 6px" />
                获取内存信息失败
              </div>
              <div v-else-if="checkStatus(memoryStatus, 'pending')" class="os-detail-info-value waiting">
                <a-icon type="clock-circle" style="margin-right: 6px; color: #FAAD14" />
                等待收集内存信息
              </div>
              <div v-else class="os-detail-info-value">
                <!-- 改为显示更详细的内存信息 -->
                <div v-if="osInfo && osInfo.totalMemory">
                  <div style="font-weight: 500">
                    总内存: {{ osInfo.totalMemory }} GB
                  </div>
                  <div style="font-size: 12px; color: #666; margin-top: 4px">
                    已用: {{ (osInfo.totalMemory - osInfo.availableMemory).toFixed(1) }} GB，可用: {{ osInfo.availableMemory }} GB
                  </div>
                  <div style="margin-top: 6px">
                    <div style="display: flex; justify-content: space-between; font-size: 12px; margin-bottom: 2px">
                      <span>使用率: {{ (100 * (1 - osInfo.availableMemory / osInfo.totalMemory)).toFixed(1) }}%</span>
                      <span>{{ (osInfo.totalMemory - osInfo.availableMemory).toFixed(1) }}/{{ osInfo.totalMemory }} GB</span>
                    </div>
                    <a-progress
                      :percent="Number((100 * (1 - osInfo.availableMemory / osInfo.totalMemory)).toFixed(1))"
                      :showInfo="false"
                      :strokeColor="{
                        '0%': '#108ee9',
                        '100%': '#87d068',
                      }"
                      :strokeWidth="6"
                    />
                  </div>
                </div>
                <span v-else>未知</span>
              </div>
            </div>
          </div>

          <!-- 磁盘信息 -->
          <div class="os-detail-info-row">
            <div class="os-detail-info-icon-container">
              <div
                class="os-detail-info-icon storage"
                :style="{
                  backgroundColor: checkStatus(diskStatus, 'success') ? 'rgba(52, 199, 89, 0.1)' :
                      checkStatus(diskStatus, 'error') ? 'rgba(255, 59, 48, 0.1)' :
                          'rgba(0, 122, 255, 0.1)',
                  color: checkStatus(diskStatus, 'success') ? '#34C759' :
                      checkStatus(diskStatus, 'error') ? '#FF3B30' :
                          '#007AFF'
                }"
              >
                <i class="anticon anticon-hdd"></i>
              </div>
            </div>
            <div class="os-detail-info-content">
              <div class="os-detail-info-label">
                <span>磁盘</span>
                <!-- 添加状态图标 -->
                <a-icon
                  v-if="checkStatus(diskStatus, 'success')"
                  type="check-circle"
                  style="margin-left: 6px; color: #34C759; font-size: 12px"
                />
                <a-icon
                  v-else-if="checkStatus(diskStatus, 'error')"
                  type="close-circle"
                  style="margin-left: 6px; color: #FF3B30; font-size: 12px"
                />
                <a-icon
                  v-else-if="checkStatus(diskStatus, 'loading')"
                  type="loading"
                  style="margin-left: 6px; color: #007AFF; font-size: 12px"
                />
              </div>
              <div v-if="diskStatus === 'loading'" class="os-detail-info-value loading">
                <div class="loading-animation"></div>
                <span>正在收集磁盘信息...</span>
              </div>
              <div v-else-if="diskStatus === 'error'" class="os-detail-info-value error">
                <a-icon type="warning" style="margin-right: 6px" />
                获取磁盘信息失败
              </div>
              <div v-else-if="diskStatus === 'pending'" class="os-detail-info-value waiting">
                <a-icon type="clock-circle" style="margin-right: 6px; color: #FAAD14" />
                等待收集磁盘信息
              </div>
              <div v-else class="os-detail-info-value">
                <!-- 改为显示更详细的磁盘信息 -->
                <div v-if="osInfo && osInfo.totalDisk">
                  <div style="font-weight: 500">
                    总空间: {{ osInfo.totalDisk }} GB
                  </div>
                  <div style="font-size: 12px; color: #666; margin-top: 4px">
                    已用: {{ (osInfo.totalDisk - osInfo.availableDisk).toFixed(1) }} GB，可用: {{ osInfo.availableDisk }} GB
                  </div>
                  <div style="margin-top: 6px">
                    <div style="display: flex; justify-content: space-between; font-size: 12px; margin-bottom: 2px">
                      <span>
                        使用率: {{ (100 * (1 - osInfo.availableDisk / osInfo.totalDisk)).toFixed(1) }}%
                      </span>
                      <span>
                        {{ (osInfo.totalDisk - osInfo.availableDisk).toFixed(1) }}/{{ osInfo.totalDisk }} GB
                      </span>
                    </div>
                    <a-progress
                      :percent="Number((100 * (1 - osInfo.availableDisk / osInfo.totalDisk)).toFixed(1))"
                      :showInfo="false"
                      :strokeColor="(100 * (1 - osInfo.availableDisk / osInfo.totalDisk) > 90) ? '#ff4d4f' :
                          (100 * (1 - osInfo.availableDisk / osInfo.totalDisk) > 70) ? '#faad14' : '#52c41a'"
                      :strokeWidth="6"
                    />
                  </div>
                </div>
                <span v-else>未知</span>
              </div>
            </div>
          </div>

          <!-- 交换空间信息 -->
          <div class="os-detail-info-row">
            <div class="os-detail-info-icon-container">
              <div
                class="os-detail-info-icon swap"
                :style="{
                  backgroundColor: checkStatus(swapStatus, 'success') ? 'rgba(52, 199, 89, 0.1)' :
                      checkStatus(swapStatus, 'error') ? 'rgba(255, 59, 48, 0.1)' :
                          'rgba(0, 122, 255, 0.1)',
                  color: checkStatus(swapStatus, 'success') ? '#34C759' :
                      checkStatus(swapStatus, 'error') ? '#FF3B30' :
                          '#007AFF'
                }"
              >
                <i class="anticon anticon-swap"></i>
              </div>
            </div>
            <div class="os-detail-info-content">
              <div class="os-detail-info-label">
                <span>交换空间</span>
                <!-- 添加状态图标 -->
                <a-icon
                  v-if="checkStatus(swapStatus, 'success')"
                  type="check-circle"
                  style="margin-left: 6px; color: #34C759; font-size: 12px"
                />
                <a-icon
                  v-else-if="checkStatus(swapStatus, 'error')"
                  type="close-circle"
                  style="margin-left: 6px; color: #FF3B30; font-size: 12px"
                />
                <a-icon
                  v-else-if="swapStatus === 'loading'"
                  type="loading"
                  style="margin-left: 6px; color: #007AFF; font-size: 12px"
                />
                <a-icon
                  v-else-if="swapStatus === 'pending'"
                  type="clock-circle"
                  style="margin-left: 6px; color: #FAAD14; font-size: 12px"
                />
              </div>
              <div v-if="swapStatus === 'loading'" class="os-detail-info-value loading">
                <div class="loading-animation"></div>
                <span>正在收集交换空间信息...</span>
              </div>
              <div v-else-if="swapStatus === 'error'" class="os-detail-info-value error">
                <a-icon type="warning" style="margin-right: 6px" />
                获取交换空间信息失败
              </div>
              <div v-else-if="swapStatus === 'pending'" class="os-detail-info-value waiting">
                <a-icon type="clock-circle" style="margin-right: 6px; color: #FAAD14" />
                等待收集交换空间信息
              </div>
              <div v-else class="os-detail-info-value">
                <template v-if="osInfo && osInfo.totalSwap !== undefined">
                  <template v-if="osInfo.totalSwap === 0">
                    未开启交换空间
                  </template>
                  <div v-else>
                    <div style="font-weight: 500">
                      总交换空间: {{ osInfo.totalSwap }} GB
                    </div>
                    <div style="font-size: 12px; color: #666; margin-top: 4px">
                      已用: {{ (osInfo.totalSwap - osInfo.availableSwap).toFixed(1) }} GB，可用: {{ osInfo.availableSwap }} GB
                    </div>
                    <div style="margin-top: 6px">
                      <div style="display: flex; justify-content: space-between; font-size: 12px; margin-bottom: 2px">
                        <span>
                          使用率: {{ (100 * (1 - osInfo.availableSwap / osInfo.totalSwap)).toFixed(1) }}%
                        </span>
                        <span>
                          {{ (osInfo.totalSwap - osInfo.availableSwap).toFixed(1) }}/{{ osInfo.totalSwap }} GB
                        </span>
                      </div>
                      <a-progress
                        :percent="Number((100 * (1 - osInfo.availableSwap / osInfo.totalSwap)).toFixed(1))"
                        :showInfo="false"
                        :strokeColor="(100 * (1 - osInfo.availableSwap / osInfo.totalSwap) > 90) ? '#ff4d4f' :
                            (100 * (1 - osInfo.availableSwap / osInfo.totalSwap) > 70) ? '#faad14' : '#52c41a'"
                        :strokeWidth="6"
                      />
                    </div>
                  </div>
                </template>
                <span v-else>未知</span>
              </div>
            </div>
          </div>

          <!-- GPU信息 -->
          <div class="os-detail-info-row">
            <div class="os-detail-info-icon-container">
              <div
                class="os-detail-info-icon gpu"
                :style="{
                  backgroundColor: checkStatus(gpuStatus, 'success') ? 'rgba(52, 199, 89, 0.1)' :
                      checkStatus(gpuStatus, 'error') ? 'rgba(255, 59, 48, 0.1)' :
                          'rgba(0, 122, 255, 0.1)',
                  color: checkStatus(gpuStatus, 'success') ? '#34C759' :
                      checkStatus(gpuStatus, 'error') ? '#FF3B30' :
                          '#007AFF'
                }"
              >
                <i class="anticon anticon-radar-chart"></i>
              </div>
            </div>
            <div class="os-detail-info-content">
              <div class="os-detail-info-label">
                <span>GPU</span>
                <!-- 添加状态图标 -->
                <a-icon
                  v-if="checkStatus(gpuStatus, 'success')"
                  type="check-circle"
                  style="margin-left: 6px; color: #34C759; font-size: 12px"
                />
                <a-icon
                  v-else-if="checkStatus(gpuStatus, 'error')"
                  type="close-circle"
                  style="margin-left: 6px; color: #FF3B30; font-size: 12px"
                />
                <a-icon
                  v-else-if="checkStatus(gpuStatus, 'loading')"
                  type="loading"
                  style="margin-left: 6px; color: #007AFF; font-size: 12px"
                />
              </div>
              <div v-if="gpuStatus === 'loading'" class="os-detail-info-value loading">
                <div class="loading-animation"></div>
                <span>正在收集GPU信息...</span>
              </div>
              <div v-else-if="gpuStatus === 'error'" class="os-detail-info-value error">
                <a-icon type="warning" style="margin-right: 6px" />
                获取GPU信息失败
              </div>
              <div v-else-if="gpuStatus === 'pending'" class="os-detail-info-value waiting">
                <a-icon type="clock-circle" style="margin-right: 6px; color: #FAAD14" />
                等待收集GPU信息
              </div>
              <div v-else class="os-detail-info-value">
                <!-- 改为显示更详细的GPU信息 -->
                <div v-if="osInfo">
                  <div v-if="osInfo.gpuInfo && !osInfo.gpuInfo.startsWith('ERROR:') && osInfo.gpuInfo !== '未检测到GPU设备'">
                    <div style="font-weight: 500">
                      {{ osInfo.gpuInfo }}
                    </div>
                    <div v-if="osInfo.gpuMemory && osInfo.gpuMemory > 0" style="font-size: 12px; color: #666; margin-top: 4px">
                      显存: {{ osInfo.gpuMemory.toFixed(1) }} GB
                    </div>
                    <div v-else style="font-size: 12px; color: #666; margin-top: 4px">显存信息未获取到</div>
                  </div>
                  <div v-else style="color: #666">未检测到GPU设备或无法获取GPU信息</div>
                </div>
                <span v-else>未知</span>
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

// 头部区域
.os-detail-header {
  padding: 20px;
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, #F5F5F7, #E5E5EA);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);

  .os-detail-icon-container {
    margin-right: 20px;

    img {
      width: 48px;
      height: 48px;
      padding: 6px;
      border-radius: 12px;
      box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
      background-color: white;
    }
  }

  .os-detail-title-container {
    color: #1D1D1F;

    .os-detail-title {
      font-size: 1.3rem;
      font-weight: 600;
      margin: 0 0 4px 0;
    }

    .os-detail-subtitle {
      font-size: 0.85rem;
      color: #86868B;
      margin: 0;
      line-height: 1.4;
    }
  }
}

// 内容区域
.os-detail-content {
  padding: 20px;
}

// 卡片样式
.os-detail-card {
  background-color: #ffffff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;

  &:last-child {
    margin-bottom: 0;
  }

  .os-detail-card-header {
    display: flex;
    align-items: center;
    padding: 15px;
    background-color: #F5F5F7;
    font-weight: 600;
    font-size: 15px;
    color: #1D1D1F;
    border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  }

  .os-detail-card-content {
    padding: 15px;
  }
}

// 信息行样式
.os-detail-info-row {
  display: flex;
  padding: 15px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);

  &:last-child {
    border-bottom: none;
  }

  .os-detail-info-icon-container {
    margin-right: 16px;
    display: flex;
    align-items: flex-start;

    .os-detail-info-icon {
      width: 36px;
      height: 36px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;

      i {
        font-size: 20px;
      }

      &.cpu {
        background-color: rgba(0, 122, 255, 0.1);
        color: #007AFF;
      }

      &.memory {
        background-color: rgba(255, 149, 0, 0.1);
        color: #FF9500;
      }

      &.storage {
        background-color: rgba(52, 199, 89, 0.1);
        color: #34C759;
      }

      &.swap {
        background-color: rgba(175, 82, 222, 0.1);
        color: #AF52DE;
      }

      &.gpu {
        background-color: rgba(255, 59, 48, 0.1);
        color: #FF3B30;
      }
    }
  }

  .os-detail-info-content {
    flex: 1;

    .os-detail-info-label {
      display: flex;
      align-items: center;
      font-size: 14px;
      color: #86868B;
      margin-bottom: 6px;
    }

    .os-detail-info-value {
      font-size: 14px;
      color: #1D1D1F;
      word-break: break-word;

      &.loading {
        display: flex;
        align-items: center;
        color: #007AFF;

        .loading-animation {
          width: 12px;
          height: 12px;
          border: 2px solid rgba(0, 122, 255, 0.3);
          border-top: 2px solid #007AFF;
          border-radius: 50%;
          margin-right: 8px;
          animation: spin 1s linear infinite;
        }
      }

      &.error {
        color: #FF3B30;
      }

      &.waiting {
        color: #FAAD14;
      }
    }
  }
}

// 动画效果
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
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