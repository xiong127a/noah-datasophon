<!--
 * @describe: 基本信息共享组件 - 显示服务的基本连接信息
-->
<template>
  <div class="basic-info-wrapper">
    <!-- 使用分组信息（新格式） -->
    <template v-if="hasGroupedInfo">
      <!-- 基础信息 -->
      <div v-if="hasBasicInfo" class="info-section">
        <div class="section-title">
          <span>基础信息</span>
        </div>
        <div class="info-cards">
          <div 
            v-for="(item, key) in groupedInfo.basicInfo" 
            :key="`basic-${key}`"
            class="info-card" 
            :class="{ 'wide-card': isJdbcUrl(key), 'copied': copiedItem === key }"
            @click="directCopy(item.value, key, $event, item.displayName || formatLabel(key))"
            @mouseenter="handleMouseEnter(key, $event)"
            @mouseleave="handleMouseLeave()"
            @mousemove="updateMousePosition($event)"
          >
            <div class="card-label">{{ item.displayName || formatLabel(key) }}</div>
            <div class="card-value-container">
              <div 
                v-if="isSecretValue(key)" 
                class="card-value masked-value"
              >
                ••••••••
              </div>
              <div 
                v-else-if="isJdbcUrl(key)" 
                class="card-value code-value"
              >
                <span class="copyable-text">{{ item.value }}</span>
              </div>
              <div 
                v-else 
                class="card-value"
              >
                <span class="copyable-text">{{ formatValue(item.value) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 安全信息 -->
      <div v-if="hasSecurityInfo" class="info-section">
        <div class="section-title">
          <span>安全信息</span>
        </div>
        <div class="info-table">
          <div 
            v-for="(item, key) in groupedInfo.securityInfo" 
            :key="`security-${key}`"
            class="info-row"
            :class="{ 'copied': copiedItem === `security-${key}` }"
            @click="directCopy(item.value, `security-${key}`, $event, item.displayName || formatLabel(key))"
            @mouseenter="handleMouseEnter(`security-${key}`, $event)"
            @mouseleave="handleMouseLeave()"
            @mousemove="updateMousePosition($event)"
          >
            <div class="row-label">{{ item.displayName || formatLabel(key) }}</div>
            <div class="row-value-container">
              <div 
                v-if="isSecretValue(key)" 
                class="row-value masked-value"
              >
                ••••••••
              </div>
              <div 
                v-else 
                class="row-value"
              >
                <span class="copyable-text">{{ formatValue(item.value) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 连接信息 -->
      <div v-if="hasConnectInfo" class="info-section">
        <div class="section-title">
          <span>连接信息</span>
        </div>
        <div class="jdbc-cards">
          <div 
            v-for="(item, key) in groupedInfo.connectInfo" 
            :key="`connect-${key}`"
            class="jdbc-card"
            :class="{ 
              'copied': copiedItem === `connect-${key}`,
              'important-card': isImportantKey(key)
            }"
            @click="directCopy(item.value, `connect-${key}`, $event, item.displayName || formatLabel(key))"
            @mouseenter="handleMouseEnter(`connect-${key}`, $event)"
            @mouseleave="handleMouseLeave()"
            @mousemove="updateMousePosition($event)"
          >
            <div class="jdbc-label">{{ item.displayName || formatLabel(key) }}</div>
            <div class="jdbc-value-container">
              <div class="jdbc-value">
                <span class="copyable-text jdbc-link">{{ item.value }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
    
    <!-- 使用常规信息项（旧格式） -->
    <template v-else>
      <div v-if="infoItems && infoItems.length > 0" class="info-section">
        <div class="info-table">
          <div 
            v-for="(item, index) in infoItems" 
            :key="`info-${index}`"
            class="info-row"
            :class="{ 'copied': copiedItem === `item-${index}` }"
            @click="directCopy(item.value, `item-${index}`, $event, item.label || formatLabel(item.key || ''))"
            @mouseenter="handleMouseEnter(`item-${index}`, $event)"
            @mouseleave="handleMouseLeave()"
            @mousemove="updateMousePosition($event)"
          >
            <div class="row-label">{{ item.label || formatLabel(item.key || '') }}</div>
            <div class="row-value-container">
              <div 
                v-if="isSecretValueByLabel(item.label)" 
                class="row-value masked-value"
              >
                ••••••••
              </div>
              <div 
                v-else 
                class="row-value"
              >
                <span class="copyable-text">{{ formatValue(item.value) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- JDBC连接 -->
      <div v-if="jdbcItems && jdbcItems.length > 0" class="info-section">
        <div class="section-title">
          <span>JDBC 连接</span>
        </div>
        <div class="jdbc-cards">
          <div 
            v-for="(item, index) in jdbcItems" 
            :key="`jdbc-${index}`"
            class="jdbc-card"
            :class="{ 'copied': copiedItem === `jdbc-${index}` }"
            @click="directCopy(item.value, `jdbc-${index}`, $event, item.label || formatLabel(item.key || ''))"
            @mouseenter="handleMouseEnter(`jdbc-${index}`, $event)"
            @mouseleave="handleMouseLeave()"
            @mousemove="updateMousePosition($event)"
          >
            <div class="jdbc-label">{{ item.label || formatLabel(item.key || '') }}</div>
            <div class="jdbc-value-container">
              <div class="jdbc-value">
                <span class="copyable-text jdbc-link">{{ item.value }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 自定义跟随鼠标的tooltip -->
    <div class="custom-tooltip" v-show="showTooltip" :style="tooltipStyle">
      {{ tooltipText }}
    </div>
  </div>
</template>

<script>
// 导入通用复制工具 - 如果之前没有导入
import { copyText } from '@/utils/copyUtil';

export default {
  name: 'BasicInfo',
  props: {
    // 常规信息项（旧格式）
    infoItems: {
      type: Array,
      default: () => []
    },
    // JDBC连接项（旧格式）
    jdbcItems: {
      type: Array,
      default: () => []
    },
    // 分组信息（新格式）
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
      copyInProgress: false,
      // 当前鼠标悬停的项目
      hoverItemKey: '',
      // 自定义tooltip相关
      showTooltip: false,
      tooltipText: '点击复制',
      // 鼠标位置
      mouseX: 0,
      mouseY: 0
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
    },
    // tooltip样式，根据鼠标位置动态计算
    tooltipStyle() {
      return {
        left: `${this.mouseX + 15}px`,
        top: `${this.mouseY - 30}px`
      };
    }
  },
  methods: {
    // 修改复制方法，添加标题参数
    async directCopy(text, itemKey, event, title) {
      console.log('尝试复制文本:', text, '标题:', title);
      
      // 验证要复制的内容
      if (!text || String(text).trim().length === 0) {
        console.warn('没有可复制的内容');
        this.$message.warning('没有可复制的内容');
        return false;
      }

      // 强制转换为字符串
      const textToCopy = String(text);
      
      // 设置视觉反馈
      this.copiedItem = itemKey;
      
      try {
        // 方法1: 使用现代Clipboard API (最可靠)
        if (navigator.clipboard && navigator.clipboard.writeText) {
          try {
            await navigator.clipboard.writeText(textToCopy);
            console.log('成功使用Clipboard API复制:', textToCopy);
            this.showCopySuccess(itemKey, title);
            return true;
          } catch (clipboardErr) {
            console.warn('Clipboard API失败，尝试备用方法:', clipboardErr);
          }
        }
        
        // 方法2: 创建不可见文本区域并编程选择，使用隔离容器
        let tempElement = null;
        let container = null;
        
        try {
          // 创建隔离容器
          container = document.createElement('div');
          container.style.cssText = `
            position: absolute;
            left: -9999px;
            top: ${window.pageYOffset || document.documentElement.scrollTop}px;
            width: 1px;
            height: 1px;
            opacity: 0;
            overflow: hidden;
            z-index: -9999;
            pointer-events: none;
          `;
          
          // 确定使用哪种元素 - 对于普通文本使用textarea，代码文本使用pre
          tempElement = document.createElement(textToCopy.length > 500 ? 'textarea' : 'input');
          
          // 设置值
          tempElement.value = textToCopy;
          
          // 应用关键样式
          tempElement.style.cssText = `
            position: relative;
            left: 0;
            top: 0;
            width: 2em;
            height: 2em;
            padding: 0;
            opacity: 0;
            background: transparent;
            border: none;
            outline: none;
            resize: none;
            font-size: 16px;
            user-select: text;
            -webkit-user-select: text;
          `;
          
          // 添加到DOM - 使用组件元素或document.documentElement，避免使用body
          if (this.$el) {
            this.$el.appendChild(container);
            container.appendChild(tempElement);
          } else {
            document.documentElement.appendChild(container);
            container.appendChild(tempElement);
          }
          
          // 聚焦并选择文本
          tempElement.focus();
          tempElement.select();
          
          // 针对input使用选区，textarea使用select
          if (tempElement.tagName.toLowerCase() === 'input') {
            tempElement.setSelectionRange(0, textToCopy.length);
          }
          
          // 等待50ms确保选中生效
          await new Promise(resolve => setTimeout(resolve, 50));
          
          // 执行复制命令
          const copySuccess = document.execCommand('copy');
          
          if (copySuccess) {
            console.log('成功使用execCommand复制:', textToCopy);
            this.showCopySuccess(itemKey, title);
            return true;
          } else {
            console.warn('execCommand复制返回失败');
            throw new Error('复制命令执行失败');
          }
        } finally {
          // 清理临时元素和容器
          if (container) {
            if (this.$el && this.$el.contains(container)) {
              this.$el.removeChild(container);
            } else if (document.documentElement.contains(container)) {
              document.documentElement.removeChild(container);
            }
          }
        }
      } catch (err) {
        console.error('所有复制方法均失败:', err);
        this.$message.error('复制失败，请手动复制');
        this.copiedItem = '';
        return false;
      }
    },
    
    // 显示复制成功提示，添加标题参数
    showCopySuccess(itemKey, title) {
      this.copiedItem = itemKey;
      this.$message.success(`复制${title || ''}成功`);
      
      // 延迟清除复制状态
      setTimeout(() => {
        if (this.copiedItem === itemKey) {
          this.copiedItem = '';
        }
      }, 2000);
    },
    
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
    
    // 处理鼠标进入事件
    handleMouseEnter(itemKey, event) {
      this.hoverItemKey = itemKey;
      this.updateMousePosition(event);
      this.tooltipText = this.copiedItem === itemKey ? '已复制' : '点击复制';
      this.showTooltip = true;
    },
    
    // 处理鼠标离开事件
    handleMouseLeave() {
      this.hoverItemKey = '';
      this.showTooltip = false;
    },
    
    // 更新鼠标位置
    updateMousePosition(event) {
      if (!event) return;
      this.mouseX = event.clientX;
      this.mouseY = event.clientY;
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
    
    // 兼容旧版本中使用的复制方法
    simpleCopy(text, itemKey, event, title) {
      return this.directCopy(text, itemKey, event, title);
    }
  }
};
</script>

<style scoped>
.basic-info-wrapper {
  padding: 24px 0;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Helvetica Neue", Arial, sans-serif;
  position: relative; /* 为自定义tooltip添加相对定位 */
}

/* 自定义tooltip样式 */
.custom-tooltip {
  position: fixed;
  z-index: 9999;
  background-color: rgba(0, 0, 0, 0.75);
  color: white;
  padding: 6px 10px;
  border-radius: 4px;
  font-size: 13px;
  pointer-events: none;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: transform 0.1s ease, opacity 0.2s ease;
  transform: translateY(-50%);
  font-weight: 500;
  letter-spacing: 0.3px;
  backdrop-filter: blur(2px);
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
  position: relative;
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
  background-color: rgba(0, 113, 227, 0.03);
}
.info-card:active {
  background-color: rgba(0, 113, 227, 0.08);
}

.card-label {
  font-size: 14px;
  color: #86868b;
  margin-bottom: 8px;
}

.card-value-container {
  display: flex;
  align-items: center;
  width: 100%;
}

.card-value {
  font-size: 15px;
  color: #1d1d1f;
  word-break: break-word;
  line-height: 1.5;
  position: relative;
  flex: 1;
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
  background-color: rgba(0, 113, 227, 0.03);
}
.info-row:active {
  background-color: rgba(0, 113, 227, 0.08);
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
  position: relative;
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
  background-color: rgba(0, 113, 227, 0.03);
}
.jdbc-card:active {
  background-color: rgba(0, 113, 227, 0.08);
}
.jdbc-label {
  font-size: 14px;
  color: #86868b;
  margin-bottom: 10px;
}
.jdbc-value-container {
  display: flex;
  align-items: center;
  width: 100%;
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
  position: relative;
  flex: 1;
}

/* 将重要信息高亮样式应用到连接信息卡片 */
.jdbc-card.important-card {
  background-color: rgba(0, 113, 227, 0.04);
  border-color: rgba(0, 113, 227, 0.2);
  position: relative;
}
.jdbc-card.important-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(to bottom, #0071e3, #5ac8fa);
}
.jdbc-card.important-card .jdbc-label {
  color: #0071e3;
  padding-left: 8px;
}

/* 改进复制成功的视觉反馈，确保它不会覆盖重要信息的样式 */
.jdbc-card.copied {
  background-color: rgba(102, 195, 58, 0.08);
  border-color: rgba(102, 195, 58, 0.3);
  box-shadow: 0 2px 8px rgba(102, 195, 58, 0.15);
}
.jdbc-card.important-card.copied::before {
  background: linear-gradient(to bottom, #67c23a, #95e069);
}

/* 保留其他复制样式 */
.info-card.copied,
.info-row.copied {
  background-color: rgba(102, 195, 58, 0.08);
  border-color: rgba(102, 195, 58, 0.3);
  box-shadow: 0 2px 8px rgba(102, 195, 58, 0.15);
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

/* 修改鼠标悬停样式 */
.info-card, 
.info-row, 
.jdbc-card {
  user-select: none;
  cursor: pointer;
  position: relative; /* 确保相对定位，便于添加伪元素 */
  transition: all 0.25s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.info-card:hover, 
.info-row:hover, 
.jdbc-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.info-card:active, 
.info-row:active, 
.jdbc-card:active {
  transform: translateY(0);
  opacity: 0.9;
}

/* 增强卡片和行的点击效果 */
.card-value, 
.row-value, 
.jdbc-value {
  cursor: pointer;
  position: relative;
  transition: all 0.2s ease;
}

.card-value:hover, 
.row-value:hover, 
.jdbc-value:hover {
  background-color: rgba(94, 92, 230, 0.05);
}

.card-value:active, 
.row-value:active, 
.jdbc-value:active {
  background-color: rgba(94, 92, 230, 0.1);
}

/* 自定义tooltip样式增强 */
.custom-tooltip {
  position: fixed;
  z-index: 9999;
  background-color: rgba(0, 0, 0, 0.75);
  color: white;
  padding: 6px 10px;
  border-radius: 4px;
  font-size: 13px;
  pointer-events: none;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: transform 0.1s ease, opacity 0.2s ease;
  transform: translateY(-50%);
  font-weight: 500;
  letter-spacing: 0.3px;
  backdrop-filter: blur(2px);
}

/* 复制成功状态的样式 */
.info-card.copied,
.info-row.copied,
.jdbc-card.copied {
  animation: copiedFlash 1s ease;
  border-color: rgba(102, 195, 58, 0.5);
}

@keyframes copiedFlash {
  0% { background-color: rgba(102, 195, 58, 0.1); }
  50% { background-color: rgba(102, 195, 58, 0.2); }
  100% { background-color: rgba(102, 195, 58, 0.05); }
}

/* 复制文本样式优化 */
.copyable-text {
  display: inline-block;
  width: 100%;
}

/* 高亮重要连接信息 */
.jdbc-card.important-card {
  background-color: rgba(0, 113, 227, 0.04);
  border-color: rgba(0, 113, 227, 0.2);
}

/* 添加复制图标相关样式 */
.copy-icon {
  margin-left: 8px;
  color: #5E5CE6;
  cursor: pointer;
  opacity: 0.6;
  transition: all 0.3s;
  font-size: 16px;
  pointer-events: auto !important; /* 确保图标能响应点击 */
}

.copy-icon:hover {
  opacity: 1;
  transform: scale(1.1);
}

/* 添加可复制文本的样式 */
.copyable-text {
  cursor: pointer;
  transition: all 0.2s;
  user-select: text !important;
  -webkit-user-select: text !important;
  -moz-user-select: text !important;
  -ms-user-select: text !important;
}

.copyable-text:hover {
  color: #5E5CE6;
}

.copyable-text:active {
  transform: scale(0.98);
  opacity: 0.9;
}

/* 容器样式修改 */
.card-value-container,
.row-value-container,
.jdbc-value-container {
  display: flex;
  align-items: center;
  width: 100%;
}

/* JDBC URL链接样式 */
.jdbc-link {
  cursor: pointer;
  font-family: "SF Mono", SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  color: #0071e3;
  transition: all 0.2s;
  user-select: all !important;
  -webkit-user-select: all !important;
  -moz-user-select: all !important;
  -ms-user-select: all !important;
}

.jdbc-link:hover {
  text-decoration: underline;
}

/* 添加点击动画 */
.copyable-text {
  transition: all 0.15s ease;
}
</style> 