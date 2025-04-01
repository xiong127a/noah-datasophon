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
              <span class="hostname-name">{{ hostInfo.hostname || "未知主机" }}</span>
              <span class="hostname-ip">{{ hostInfo.ip }}</span>
            </div>
            <div class="hostname-detail-meta">
              <div class="hostname-meta-item" v-if="hostInfo.fqdn">
                <span class="meta-label">FQDN</span>
                <span class="meta-value">{{ hostInfo.fqdn }}</span>
              </div>
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
          <div class="info-item" v-if="hostInfo.osInfo && hostInfo.osInfo.dnsServers && hostInfo.osInfo.dnsServers.length > 0">
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
                  <div v-for="(dns, index) in hostInfo.osInfo.dnsServers" :key="index" class="dns-server">
                    {{ dns }}
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
                <div class="info-empty">未配置DNS服务器</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Hosts文件内容 -->
        <div class="hostname-detail-section" v-if="hostInfo.hostsFile">
          <div class="section-header">
            <span class="section-title">Hosts文件内容</span>
          </div>
          <div class="hosts-file-container">
            <pre class="hosts-file-content">{{ hostInfo.hostsFile }}</pre>
          </div>
        </div>
        <div class="hostname-detail-section" v-else>
          <div class="section-header">
            <span class="section-title">Hosts文件内容</span>
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
  methods: {
    // 检查状态方法
    checkStatus(status, expectedStatus) {
      if (!status) return expectedStatus === 'pending';
      return status.toLowerCase() === expectedStatus;
    }
  }
}
</script>

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
  gap: 4px;
}

.dns-server {
  background-color: #f5f5f7;
  padding: 4px 8px;
  border-radius: 4px;
}

.hosts-file-container {
  background-color: #f5f5f7;
  border-radius: 8px;
  padding: 12px;
  max-height: 200px;
  overflow-y: auto;
}

.hosts-file-content {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  font-size: 12px;
  margin: 0;
  white-space: pre-wrap;
  color: #1d1d1f;
}

.info-empty {
  color: #8e8e93;
  font-style: italic;
  font-size: 13px;
  padding: 4px 0;
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
</style> 