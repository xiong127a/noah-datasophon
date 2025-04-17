<!--
 * @describe: 基本信息共享组件 - 显示服务的基本连接信息
-->
<template>
  <div class="basic-info-wrapper">
    <!-- 分组信息展示（新格式） -->
    <template v-if="hasGroupedInfo">
      <!-- 基础信息组 -->
      <div v-if="hasBasicInfo" class="info-section">
        <div class="section-title">
          <span>基础信息</span>
        </div>
        <div class="info-cards">
          <div 
            v-for="(value, key) in groupedInfo.basicInfo" 
            :key="`basic-${key}-${value.value}`"
            class="info-card"
            :class="{ 'important-card': isImportantKey(key) }"
          >
            <div class="card-label">{{ value.displayName || formatLabel(key) }}</div>
            <div class="card-value" :ref="`basic-value-${key}`">
              <template v-if="!isSecretValue(key)">{{ formatValue(value.value) }}</template>
              <template v-else><span class="masked-value">••••••</span></template>
            </div>
            <button class="copy-btn" @click="copyToClipboard(value.value, value.displayName || formatLabel(key), `basic-${key}`)">
              <a-icon type="copy" />
            </button>
            <span v-if="copiedItem === `basic-${key}`" class="copied-tip">已复制</span>
          </div>
        </div>
      </div>

      <!-- 安全信息组 -->
      <div v-if="hasSecurityInfo" class="info-section">
        <div class="section-title">
          <span>安全信息</span>
        </div>
        <div class="info-cards">
          <div 
            v-for="(value, key) in groupedInfo.securityInfo" 
            :key="`security-${key}-${value.value}`"
            class="info-card"
            :class="{ 'important-card': isImportantKey(key) }"
          >
            <div class="card-label">{{ value.displayName || formatLabel(key) }}</div>
            <div class="card-value" :ref="`security-value-${key}`">
              <template v-if="!isSecretValue(key)">{{ formatValue(value.value) }}</template>
              <template v-else><span class="masked-value">••••••</span></template>
            </div>
            <button class="copy-btn" @click="copyToClipboard(value.value, value.displayName || formatLabel(key), `security-${key}`)">
              <a-icon type="copy" />
            </button>
            <span v-if="copiedItem === `security-${key}`" class="copied-tip">已复制</span>
          </div>
        </div>
      </div>

      <!-- 连接信息组 -->
      <div v-if="hasConnectInfo" class="info-section">
        <div class="section-title">
          <span>连接信息</span>
        </div>
        <div class="info-cards">
          <div 
            v-for="(value, key) in groupedInfo.connectInfo" 
            :key="`connect-${key}-${value.value}`"
            class="info-card"
            :class="{ 
              'important-card': isImportantKey(key),
              'wide-card': isJdbcUrl(key) || (value.value && value.value.length > 60)
            }"
          >
            <div class="card-label">{{ value.displayName || formatLabel(key) }}</div>
            <div class="card-value" :ref="`connect-value-${key}`" :class="{'code-value': isJdbcUrl(key)}">
              <template v-if="!isSecretValue(key)">{{ formatValue(value.value) }}</template>
              <template v-else><span class="masked-value">••••••</span></template>
            </div>
            <button class="copy-btn" @click="copyToClipboard(value.value, value.displayName || formatLabel(key), `connect-${key}`)">
              <a-icon type="copy" />
            </button>
            <span v-if="copiedItem === `connect-${key}`" class="copied-tip">已复制</span>
          </div>
        </div>
      </div>
    </template>

    <!-- 普通信息展示（旧格式） -->
    <template v-else>
      <!-- 基本信息卡片 -->
      <div v-if="infoItems.length > 0" class="info-section">
        <div class="section-title">
          <span>基本信息</span>
        </div>
        <div class="info-table">
          <div 
            v-for="(item, index) in infoItems" 
            :key="index" 
            class="info-row"
          >
            <div class="row-label">{{ item.label }}</div>
            <div class="row-value" :ref="`info-row-${index}`">
              <template v-if="!isSecretValueByLabel(item.label)">{{ item.value }}</template>
              <template v-else><span class="masked-value">••••••</span></template>
            </div>
            <button class="copy-btn small" @click="copyToClipboard(item.value, item.label, `info-${index}`)">
              <a-icon type="copy" />
            </button>
            <span v-if="copiedItem === `info-${index}`" class="copied-tip">已复制</span>
          </div>
        </div>
      </div>

      <!-- JDBC URL卡片 -->
      <div v-if="jdbcItems.length > 0" class="info-section">
        <div class="section-title">
          <span>JDBC 连接</span>
        </div>
        <div class="jdbc-cards">
          <div 
            v-for="(item, index) in jdbcItems" 
            :key="index" 
            class="jdbc-card"
          >
            <div class="jdbc-label">{{ item.label }}</div>
            <div class="jdbc-value" :ref="`jdbc-value-${index}`">{{ item.value }}</div>
            <button class="copy-btn" @click="copyToClipboard(item.value, item.label, `jdbc-${index}`)">
              <a-icon type="copy" />
            </button>
            <span v-if="copiedItem === `jdbc-${index}`" class="copied-tip">已复制</span>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
export default {
  name: 'BasicInfo',
  props: {
    // 基本信息项目数组（旧格式）
    infoItems: {
      type: Array,
      default: () => []
    },
    // JDBC连接信息数组（旧格式）
    jdbcItems: {
      type: Array,
      default: () => []
    },
    // 分组信息对象（新格式）
    groupedInfo: {
      type: Object,
      default: null
    },
    // 重要信息键名列表
    importantKeys: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      // 记录当前复制的项目（用于显示复制成功提示）
      copiedItem: '',
      // 复制状态
      copyInProgress: false
    };
  },
  computed: {
    // 是否使用分组信息（新格式）
    hasGroupedInfo() {
      return this.groupedInfo !== null;
    },
    // 是否有基础信息
    hasBasicInfo() {
      return this.groupedInfo && 
             this.groupedInfo.basicInfo && 
             Object.keys(this.groupedInfo.basicInfo).length > 0;
    },
    // 是否有安全信息
    hasSecurityInfo() {
      return this.groupedInfo && 
             this.groupedInfo.securityInfo && 
             Object.keys(this.groupedInfo.securityInfo).length > 0;
    },
    // 是否有连接信息
    hasConnectInfo() {
      return this.groupedInfo && 
             this.groupedInfo.connectInfo && 
             Object.keys(this.groupedInfo.connectInfo).length > 0;
    }
  },
  methods: {
    // 格式化显示标签
    formatLabel(key) {
      // 将下划线和连字符替换为空格，并首字母大写
      return key
        .replace(/_/g, ' ')
        .replace(/-/g, ' ')
        .split(' ')
        .map(word => word.charAt(0).toUpperCase() + word.slice(1))
        .join(' ');
    },
    
    // 格式化显示值
    formatValue(value) {
      if (value === null || value === undefined) {
        return '-';
      }
      
      if (typeof value === 'boolean') {
        return value ? '是' : '否';
      }
      
      return value;
    },
    
    // 判断是否为密码等敏感字段
    isSecretValue(key) {
      const sensitiveKeys = ['password', 'secret', 'token', 'key', '密码', '口令', '秘钥'];
      return sensitiveKeys.some(sensitive => 
        key.toLowerCase().includes(sensitive)
      );
    },
    
    // 根据标签判断是否为密码等敏感字段（旧格式使用）
    isSecretValueByLabel(label) {
      const sensitiveLabels = ['密码', '口令', 'Password', 'Secret', 'Token', 'Key'];
      return sensitiveLabels.some(sensitive => 
        label.includes(sensitive)
      );
    },
    
    // 判断是否为重要信息
    isImportantKey(key) {
      return this.importantKeys.includes(key);
    },
    
    // 判断是否为JDBC URL
    isJdbcUrl(key) {
      return key.toLowerCase().includes('jdbc') || 
             key.toLowerCase().includes('url') || 
             key.toLowerCase().includes('连接');
    },

    // 复制到剪贴板
    async copyToClipboard(text, label, itemKey) {
      if (this.copyInProgress) {
        return; // 防止重复点击
      }
      
      if (!text) {
        this.$message.warning('没有可复制的内容');
        return;
      }
      
      this.copyInProgress = true;
      
      try {
        // 使用现代API优先
        if (navigator.clipboard && navigator.clipboard.writeText) {
          await navigator.clipboard.writeText(text);
          this.showCopySuccess(itemKey || label);
          console.log('使用Clipboard API复制成功:', text);
        } else {
          // 降级方案
          this.legacyCopy(text, itemKey || label);
        }
      } catch (err) {
        console.error('Clipboard API失败，使用降级方案:', err);
        // 降级方案
        this.legacyCopy(text, itemKey || label);
      } finally {
        setTimeout(() => {
          this.copyInProgress = false;
        }, 300);
      }
    },
    
    // 降级复制方案
    legacyCopy(text, itemKey) {
      try {
        const input = document.createElement('textarea');
        input.value = text;
        input.style.position = 'fixed';
        input.style.left = '-9999px';
        input.style.top = '0';
        document.body.appendChild(input);
        input.focus();
        input.select();
        
        const successful = document.execCommand('copy');
        document.body.removeChild(input);
        
        if (successful) {
          this.showCopySuccess(itemKey);
          console.log('使用execCommand复制成功:', text);
        } else {
          this.$message.error('复制失败，请手动复制');
          console.error('execCommand复制失败');
        }
      } catch (err) {
        this.$message.error('复制失败，请手动复制');
        console.error('降级复制出错:', err);
      }
    },
    
    // 显示复制成功
    showCopySuccess(itemKey) {
      this.copiedItem = itemKey;
      this.$message.success('复制成功');
      
      // 2秒后清除提示
      setTimeout(() => {
        if (this.copiedItem === itemKey) {
          this.copiedItem = '';
        }
      }, 2000);
    }
  }
};
</script>

<style scoped>
.basic-info-wrapper {
  padding: 24px 0;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Helvetica Neue", Arial, sans-serif;
}

.info-section {
  margin-bottom: 32px;
}
.info-section:last-child {
  margin-bottom: 0;
}

.section-title {
  margin-bottom: 20px;
  position: relative;
}
.section-title span {
  font-size: 20px;
  font-weight: 500;
  color: #1d1d1f;
  letter-spacing: -0.02em;
  position: relative;
  display: inline-block;
}
.section-title span::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: -8px;
  width: 100%;
  height: 2px;
  background-color: #0071e3;
  border-radius: 1px;
}

.info-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.info-card {
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.08);
  padding: 16px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}
.info-card.wide-card {
  grid-column: 1 / -1;
}
.info-card.important-card {
  background-color: rgba(0, 113, 227, 0.04);
  border-color: rgba(0, 113, 227, 0.2);
}
.info-card.important-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(to bottom, #0071e3, #5ac8fa);
}
.info-card.important-card .card-label {
  color: #0071e3;
  padding-left: 8px;
}
.info-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.08);
}

.card-label {
  font-size: 14px;
  color: #86868b;
  margin-bottom: 8px;
}

.card-value {
  font-size: 15px;
  color: #1d1d1f;
  word-break: break-word;
  line-height: 1.5;
}
.card-value.code-value {
  font-family: "SF Mono", SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  background-color: rgba(0, 0, 0, 0.03);
  padding: 12px;
  border-radius: 6px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  margin-top: 8px;
  white-space: pre-wrap;
  max-height: 150px;
  overflow-y: auto;
}
.masked-value {
  font-family: "SF Mono", SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  letter-spacing: 2px;
  color: #86868b;
}

.info-table {
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.08);
  overflow: hidden;
}
.info-row {
  display: flex;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  position: relative;
}
.info-row:last-child {
  border-bottom: none;
}
.info-row:hover {
  background-color: #f5f5f7;
}
.row-label {
  flex: 0 0 140px;
  font-size: 14px;
  color: #6e6e73;
  padding-right: 16px;
}
.row-value {
  flex: 1;
  font-size: 14px;
  color: #1d1d1f;
  word-break: break-word;
}

.jdbc-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.jdbc-card {
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.08);
  padding: 16px;
  transition: all 0.3s ease;
  position: relative;
}
.jdbc-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.08);
}
.jdbc-label {
  font-size: 14px;
  color: #86868b;
  margin-bottom: 10px;
}
.jdbc-value {
  font-family: "SF Mono", SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  background-color: rgba(0, 0, 0, 0.03);
  padding: 12px;
  border-radius: 6px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  word-break: break-all;
  line-height: 1.5;
}

@media screen and (max-width: 1200px) {
  .info-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media screen and (max-width: 768px) {
  .info-cards {
    grid-template-columns: 1fr;
  }
  .info-row {
    flex-direction: column;
  }
  .row-label {
    flex: none;
    margin-bottom: 8px;
    padding-right: 0;
  }
}

@media screen and (max-width: 576px) {
  .basic-info-wrapper {
    padding: 16px 0;
  }
  .section-title span {
    font-size: 18px;
  }
  .info-card, .jdbc-card {
    padding: 14px;
  }
}

/* 复制按钮样式 */
.copy-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  background-color: rgba(0, 113, 227, 0.1);
  color: #0071e3;
  border: none;
  border-radius: 4px;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0.7;
  transition: all 0.2s;
}
.copy-btn:hover {
  opacity: 1;
  background-color: rgba(0, 113, 227, 0.2);
}
.copy-btn:active {
  transform: scale(0.95);
}
.copy-btn.small {
  width: 24px;
  height: 24px;
  top: 14px;
}

/* 复制成功提示 */
.copied-tip {
  position: absolute;
  top: 8px;
  right: 40px;
  font-size: 12px;
  color: #67c23a;
  background-color: rgba(103, 194, 58, 0.1);
  padding: 2px 6px;
  border-radius: 3px;
  opacity: 1;
  animation: fadeInOut 2s forwards;
}

@keyframes fadeInOut {
  0% { opacity: 0; }
  20% { opacity: 1; }
  80% { opacity: 1; }
  100% { opacity: 0; }
}
</style> 