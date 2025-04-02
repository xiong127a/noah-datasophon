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
        
        <a-button 
          @click="resetFilter" 
          :disabled="loading"
          style="margin-left: 12px;"
        >重置筛选</a-button>
      </div>
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
    },
    hideResetButton: {
      type: Boolean,
      default: false
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
      // 当筛选类型改变时，通知父组件刷新日志
      this.$emit('filter-change', {
        filterType: this.filterType,
        selectedLevel: this.selectedLevel
      });
    },
    handleLevelChange() {
      // 级别变更时通知父组件刷新日志
      this.$emit('filter-change', {
        filterType: this.filterType,
        selectedLevel: this.selectedLevel
      });
    },
    applyFilter() {
      // 通知父组件应用筛选条件
      this.$emit('filter-change', {
        filterType: this.filterType,
        selectedLevel: this.selectedLevel
      });
    },
    resetFilter() {
      // 重置到INFO级别以上的日志
      this.filterType = 'min';
      this.selectedLevel = 'INFO';
      
      // 通知父组件应用新的筛选
      this.applyFilter();
      
      // 通知父组件重置日志类型
      this.$parent.resetLogFilter && this.$parent.resetLogFilter();
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
  display: flex;
  align-items: center;
}

.filter-options {
  display: flex;
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
  white-space: nowrap;
}
</style> 