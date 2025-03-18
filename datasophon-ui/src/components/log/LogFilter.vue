<template>
  <div class="log-filter-container">
    <div class="filter-options">
      <div class="filter-section">
        <div class="filter-title">日志级别筛选：</div>
        <a-radio-group v-model="filterType" @change="handleFilterTypeChange">
          <a-radio-button value="all" :disabled="loading">全部日志</a-radio-button>
          <a-radio-button value="exact" :disabled="loading">仅显示选定级别</a-radio-button>
          <a-radio-button value="min" :disabled="loading">显示选定级别及以上</a-radio-button>
        </a-radio-group>
      </div>
      
      <div class="filter-section">
        <div class="filter-title">级别选择：</div>
        <a-select 
          v-model="selectedLevel" 
          style="width: 120px" 
          :disabled="filterType === 'all' || loading"
          @change="handleLevelChange"
        >
          <a-select-option value="DEBUG">DEBUG</a-select-option>
          <a-select-option value="INFO">INFO</a-select-option>
          <a-select-option value="WARN">WARN</a-select-option>
          <a-select-option value="ERROR">ERROR</a-select-option>
        </a-select>
      </div>
      
      <div class="filter-section">
        <a-button @click="resetFilter" :disabled="loading">重置筛选</a-button>
      </div>
    </div>
    
    <div class="filter-description">
      <template v-if="filterType === 'exact'">
        当前显示: <span class="highlight">{{ selectedLevel }}</span> 级别的日志
      </template>
      <template v-else-if="filterType === 'min'">
        当前显示: <span class="highlight">{{ selectedLevel }}</span> 及以上级别的日志
      </template>
      <template v-else>
        当前显示: <span class="highlight">全部</span> 日志
      </template>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'LogFilter',
  props: {
    clusterId: {
      type: Number,
      required: true
    },
    hostname: {
      type: String,
      required: true
    },
    itemId: {
      type: Number,
      required: true
    },
    value: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      filterType: 'min',  // 默认为'min'：显示选定级别及以上
      selectedLevel: 'INFO',
      loading: false
    }
  },
  methods: {
    handleFilterTypeChange() {
      // 当筛选类型改变时，立即应用筛选
      this.applyFilter();
    },
    handleLevelChange() {
      // 级别变更时直接应用筛选
      this.applyFilter();
    },
    async applyFilter() {
      if (this.filterType === 'all') {
        this.getOriginalLogContent();
        return;
      }
      
      this.loading = true;
      try {
        const exactMatch = this.filterType === 'exact';
        const response = await axios.get('/ddh/host/check-log/filtered', {
          params: {
            clusterId: this.clusterId,
            hostname: this.hostname,
            itemId: this.itemId,
            level: this.selectedLevel,
            exactMatch: exactMatch
          }
        });
        
        if (response.data && response.data.code === 200) {
          // 确保获取到的日志内容是纯文本
          let filteredLog = response.data.data || '';
          
          // 清理可能混入的HTML内容
          if (filteredLog && typeof filteredLog === 'string') {
            // 移除可能的HTML标签
            filteredLog = filteredLog.replace(/<[^>]*>/g, '');
            
            // 移除可能的UI元素描述文本（如果有特定模式可以识别）
            if (filteredLog.includes('Noah大数据基础平台')) {
              // 尝试只保留真正的日志行（通常以日期时间开头）
              const logLines = filteredLog.split('\n')
                .filter(line => /^\d{4}-\d{2}-\d{2}/.test(line.trim()))
                .join('\n');
              
              if (logLines) {
                filteredLog = logLines;
              }
            }
          }
          
          this.$emit('input', filteredLog);
        } else {
          this.$message.error(response.data?.msg || '获取筛选日志失败');
        }
      } catch (error) {
        this.$message.error('筛选日志时发生错误: ' + error.message);
      } finally {
        this.loading = false;
      }
    },
    async getOriginalLogContent() {
      this.loading = true;
      try {
        const response = await axios.get('/ddh/host/check-log/content', {
          params: {
            clusterId: this.clusterId,
            hostname: this.hostname,
            itemId: this.itemId
          }
        });
        
        if (response.data && response.data.code === 200) {
          // 确保获取到的日志内容是纯文本
          let logContent = response.data.data || '';
          
          // 清理可能混入的HTML内容
          if (logContent && typeof logContent === 'string') {
            // 移除可能的HTML标签
            logContent = logContent.replace(/<[^>]*>/g, '');
            
            // 移除可能的UI元素描述文本
            if (logContent.includes('Noah大数据基础平台')) {
              // 尝试只保留真正的日志行（通常以日期时间开头）
              const logLines = logContent.split('\n')
                .filter(line => /^\d{4}-\d{2}-\d{2}/.test(line.trim()))
                .join('\n');
              
              if (logLines) {
                logContent = logLines;
              }
            }
          }
          
          this.$emit('input', logContent);
        } else {
          this.$message.error(response.data?.msg || '获取原始日志失败');
        }
      } catch (error) {
        this.$message.error('获取日志时发生错误: ' + error.message);
      } finally {
        this.loading = false;
      }
    },
    resetFilter() {
      // 重置到INFO级别以上的日志
      this.filterType = 'min';
      this.selectedLevel = 'INFO';
      this.applyFilter();
    }
  },
  mounted() {
    // 初始加载时应用INFO级别以上的筛选
    this.applyFilter();
  }
}
</script>

<style scoped>
.log-filter-container {
  margin-bottom: 16px;
  padding: 12px;
  background-color: #f5f5f5;
  border-radius: 4px;
}

.filter-options {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
}

.filter-section {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-title {
  font-weight: 500;
  margin-right: 4px;
}

.filter-description {
  margin-top: 8px;
  color: #666;
  font-size: 12px;
}

.highlight {
  color: #1890ff;
  font-weight: bold;
}
</style> 