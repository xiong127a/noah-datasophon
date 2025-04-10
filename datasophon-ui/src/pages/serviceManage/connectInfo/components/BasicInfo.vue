<!--
 * @describe: 基本信息共享组件 - 显示服务的基本连接信息
-->
<template>
  <div class="basic-info-section">
    <!-- 基本信息卡片列表 -->
    <div class="info-cards">
      <div 
        v-for="item in infoItems" 
        :key="item.label" 
        class="info-card"
      >
        <div class="info-card-content">
          <div class="info-label">{{ item.label }}</div>
          <div class="info-value" :title="isStatusValue(item.value) ? '' : '点击复制: ' + item.value">
            <!-- 状态开关样式显示true/false-->
            <template v-if="isStatusValue(item.value)">
              <div 
                class="status-badge" 
                :class="{ 
                  'status-enabled': isEnabledValue(item.value),
                  'status-disabled': !isEnabledValue(item.value) 
                }"
              >
                <a-icon :type="isEnabledValue(item.value) ? 'check-circle' : 'close-circle'" />
                <span>{{ isEnabledValue(item.value) ? 'true' : 'false' }}</span>
              </div>
            </template>
            <!-- 普通文本值 -->
            <template v-else>
              <span class="copyable-text" @click="copyText(item.value)">{{ item.value }}</span>
              <a-tooltip title="复制">
                <a-icon
                  type="copy"
                  class="action-icon copy-icon"
                  @click.stop="copyText(item.value)"
                />
              </a-tooltip>
            </template>
          </div>
        </div>
      </div>
      
      <!-- JDBC URL信息 -->
      <div 
        v-for="(jdbc, index) in jdbcItems" 
        :key="'jdbc-' + index" 
        class="info-card jdbc-card-info"
      >
        <div class="info-card-content">
          <div class="info-label">{{ jdbc.label }}</div>
          <div class="info-value" @click="copyText(jdbc.value)" :title="'点击复制: ' + jdbc.value">
            <!-- JDBC URL格式化显示 -->
            <div class="jdbc-display">
              <!-- 直接显示完整URL，包括jdbc:前缀 -->
              <span class="jdbc-link">{{ jdbc.value }}</span>
            </div>
            <a-tooltip title="复制URL">
              <a-icon
                type="copy"
                class="action-icon copy-icon"
                @click.stop="copyText(jdbc.value)"
              />
            </a-tooltip>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "BasicInfo",
  props: {
    // 基本信息项目数组
    infoItems: {
      type: Array,
      default: () => []
    },
    // JDBC URL项目数组
    jdbcItems: {
      type: Array,
      default: () => []
    }
  },
  methods: {
    // 复制文本到剪贴板
    copyText(text) {
      if (!text) return;
      
      // 创建临时textarea元素用于复制
      const textarea = document.createElement('textarea');
      textarea.value = text;
      textarea.setAttribute('readonly', '');
      textarea.style.position = 'absolute';
      textarea.style.left = '-9999px';
      document.body.appendChild(textarea);
      
      // 选择并复制文本
      const selected = document.getSelection().rangeCount > 0 
        ? document.getSelection().getRangeAt(0) 
        : false;
      textarea.select();
      document.execCommand('copy');
      document.body.removeChild(textarea);
      
      // 恢复原始选区
      if (selected) {
        document.getSelection().removeAllRanges();
        document.getSelection().addRange(selected);
      }
      
      // 显示复制成功消息
      this.$message.success('复制成功');
    },
    
    // 判断是否为状态值（已启用/未启用等）
    isStatusValue(value) {
      if (typeof value !== 'string') return false;
      // 状态关键词列表
      const statusKeys = ['是', '否', '已启用', '未启用', '启用', '禁用', '开启', '关闭', 
                           '配置不完整', '使用单实例', 'enabled', 'disabled', 'yes', 
                           'no', 'true', 'false'];
      const normalizedValue = value.toLowerCase().trim();
      return statusKeys.some(key => normalizedValue.includes(key.toLowerCase()));
    },
    
    // 判断是否为表示"已启用"的值
    isEnabledValue(value) {
      if (typeof value !== 'string') return false;
      const enabledValues = ['是', '已启用', '启用', '开启', 'enabled', 'yes', 'true'];
      const disabledValues = ['否', '未启用', '禁用', '关闭', 'disabled', 'no', 'false'];
      
      const normalizedValue = value.toLowerCase().trim();
      
      // 优先检查完全匹配
      if (enabledValues.includes(normalizedValue)) return true;
      if (disabledValues.includes(normalizedValue)) return false;
      
      // 检查部分匹配
      for (const term of enabledValues) {
        if (normalizedValue.includes(term.toLowerCase())) return true;
      }
      
      return false;
    },
    
    // 判断是否为JDBC URL
    isJdbcUrl(value) {
      return typeof value === 'string' && value.toLowerCase().startsWith('jdbc:');
    },
    
    // 格式化JDBC URL (简化版本，直接返回原始值)
    formatJdbcUrl(jdbcUrl) {
      return jdbcUrl || '';
    }
  }
};
</script>

<style lang="less" scoped>
.info-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.info-card {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04), 0 0 1px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  transition: all 0.3s ease;
  position: relative;
  border: 1px solid rgba(210, 210, 215, 0.4);
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    border-color: rgba(94, 92, 230, 0.3);
    
    .info-card-content {
      background-color: rgba(245, 245, 247, 0.5);
    }
  }
  
  .info-card-content {
    display: flex;
    flex-direction: row;
    transition: background-color 0.3s ease;
  }
  
  .info-label {
    flex: 0 0 180px;
    font-size: 14px;
    font-weight: 600;
    color: #1d1d1f;
    background: #f5f5f7;
    padding: 16px;
    border-right: 1px solid rgba(0, 0, 0, 0.1);
    display: flex;
    align-items: center;
    letter-spacing: 0.3px;
    text-shadow: 0 0 0 #1d1d1f;
  }
  
  .info-value {
    flex: 1;
    display: flex;
    align-items: center;
    padding: 16px;
    font-size: 15px;
    line-height: 1.4;
    color: #1d1d1f;
    position: relative;
    overflow: hidden;
    
    &:hover {
      background-color: rgba(0, 0, 0, 0.02);
    }
    
    &:active {
      background-color: rgba(0, 0, 0, 0.05);
    }
    
    .copyable-text {
      flex: 1;
      word-break: break-all;
      position: relative;
      cursor: pointer;
      
      &::after {
        content: ' (点击复制)';
        color: #5E5CE6;
        font-size: 12px;
        font-weight: 500;
        opacity: 0;
        display: inline-block;
        margin-left: 6px;
        transition: opacity 0.2s ease;
      }
      
      &:hover::after {
        opacity: 0.7;
      }
    }
    
    .copy-icon {
      opacity: 0;
      transition: opacity 0.2s ease, transform 0.2s ease;
      transform: scale(0.9);
      margin-left: 8px;
      z-index: 2;
    }
  }
  
  &:hover .copy-icon {
    opacity: 1;
    transform: scale(1);
  }
}

/* 状态标签样式 */
.status-badge {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  margin: 2px 0;
  cursor: default;
  
  .anticon {
    margin-right: 8px;
    font-size: 16px;
  }
  
  span {
    &::after {
      content: none !important;
    }
  }
  
  &.status-enabled {
    background-color: rgba(52, 199, 89, 0.1);
    color: #27ae60;
    border: 1px solid rgba(52, 199, 89, 0.2);
    
    &:hover {
      background-color: rgba(52, 199, 89, 0.15);
    }
  }
  
  &.status-disabled {
    background-color: rgba(142, 142, 147, 0.1);
    color: #6c6c70;
    border: 1px solid rgba(142, 142, 147, 0.2);
    
    &:hover {
      background-color: rgba(142, 142, 147, 0.15);
    }
  }
}

/* JDBC URL显示样式 */
.jdbc-display {
  display: flex;
  flex: 1;
  font-family: "SF Mono", SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", Courier, monospace;
  
  .jdbc-protocol {
    color: #8e8e93;
    font-weight: 600;
    padding-right: 2px;
    user-select: none;
  }
  
  .jdbc-url {
    flex: 1;
    color: #0071e3;
    
    :deep(.url-host) {
      color: #0071e3;
      font-weight: 500;
    }
    
    :deep(.url-param-key) {
      color: #8e8e93;
      font-weight: 500;
    }
    
    :deep(.url-param-value) {
      color: #9557e5;
    }
    
    :deep(.url-param-value-ha) {
      color: #2eb35a;
      font-weight: 600;
    }
    
    :deep(.url-param-value-secure) {
      color: #ff9500;
      font-weight: 600;
    }
  }
}

.jdbc-card-info {
  background: linear-gradient(135deg, rgba(94, 92, 230, 0.04) 0%, rgba(94, 92, 230, 0.01) 100%);
  border: 1px solid rgba(94, 92, 230, 0.15);
  
  .info-label {
    color: #5E5CE6;
    background: rgba(94, 92, 230, 0.06);
    border-right: 1px solid rgba(94, 92, 230, 0.15);
    font-weight: 600;
  }
  
  .info-value .jdbc-link {
    color: #0071e3;
    font-family: "SF Mono", SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", Courier, monospace;
    cursor: pointer;
    transition: all 0.2s ease;
    font-weight: 500;
    font-size: 14px;
    line-height: 1.5;
    display: inline-block;
    word-break: break-all;
    overflow-wrap: break-word;
    flex: 1;
    
    &:hover {
      color: #0077ED;
      text-decoration: underline;
    }
  }
}

.copy-icon {
  color: #5E5CE6;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  padding: 6px;
  background-color: transparent;
  border-radius: 50%;
  
  &:hover {
    background-color: rgba(94, 92, 230, 0.1);
    color: #5E5CE6;
    transform: scale(1.1);
  }
}
</style> 