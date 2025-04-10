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
          <div class="info-value" @click="copyText(item.value)" :title="'点击复制: ' + item.value">
            <span>{{ item.value }}</span>
            <a-tooltip title="复制">
              <a-icon
                type="copy"
                class="action-icon copy-icon"
                @click.stop="copyText(item.value)"
              />
            </a-tooltip>
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
            <span class="jdbc-link">{{ jdbc.value }}</span>
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
    color: #86868b;
    background: linear-gradient(to right, #f5f5f7 0%, #ffffff 100%);
    padding: 16px;
    border-right: 1px solid rgba(210, 210, 215, 0.5);
    display: flex;
    align-items: center;
  }
  
  .info-value {
    flex: 1;
    display: flex;
    align-items: center;
    padding: 16px;
    font-size: 15px;
    line-height: 1.4;
    color: #1d1d1f;
    cursor: pointer;
    transition: background-color 0.2s ease;
    position: relative;
    overflow: hidden;
    
    &:hover {
      background-color: rgba(0, 0, 0, 0.02);
    }
    
    &:active {
      background-color: rgba(0, 0, 0, 0.05);
    }
    
    span {
      flex: 1;
      word-break: break-all;
      position: relative;
      
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
    }
    
    &:hover span::after {
      opacity: 0.7;
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

.jdbc-card-info {
  background: linear-gradient(135deg, rgba(94, 92, 230, 0.04) 0%, rgba(94, 92, 230, 0.01) 100%);
  border: 1px solid rgba(94, 92, 230, 0.15);
  
  .info-label {
    color: #5E5CE6;
    background: linear-gradient(to right, rgba(94, 92, 230, 0.08) 0%, rgba(94, 92, 230, 0.02) 100%);
    border-right: 1px solid rgba(94, 92, 230, 0.15);
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