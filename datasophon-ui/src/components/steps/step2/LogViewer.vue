<template>
  <div class="log-viewer">
    <div class="filter-bar">
      <a-row :gutter="16">
        <a-col :span="6">
          <a-select v-model="logLevel" style="width: 100%" placeholder="选择日志级别" allowClear>
            <a-select-option value="INFO">信息</a-select-option>
            <a-select-option value="WARNING">警告</a-select-option>
            <a-select-option value="ERROR">错误</a-select-option>
            <a-select-option value="SUCCESS">成功</a-select-option>
          </a-select>
        </a-col>
        <a-col :span="8">
          <a-range-picker
            v-model="timeRange"
            :show-time="{ format: 'HH:mm:ss' }"
            format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
            placeholder="选择时间范围"
            @change="handleTimeRangeChange"
          />
        </a-col>
        <a-col :span="6">
          <a-input 
            v-model="keyword" 
            placeholder="输入关键字搜索"
            @pressEnter="search"
            allowClear
          >
            <a-icon slot="suffix" type="search" />
          </a-input>
        </a-col>
        <a-col :span="4">
          <a-button type="primary" @click="search">搜索</a-button>
          <a-button style="margin-left: 8px" @click="resetFilters">重置</a-button>
        </a-col>
      </a-row>
    </div>

    <div class="log-content">
      <a-spin :spinning="loading">
        <a-list
          itemLayout="horizontal"
          :dataSource="logs"
          :pagination="pagination"
        >
          <a-list-item slot="renderItem" slot-scope="item" :key="item.id">
            <a-list-item-meta>
              <div slot="title">
                <span :class="'log-level log-level-' + item.level.toLowerCase()">{{ getLevelText(item.level) }}</span>
                <span class="log-time">{{ formatTime(item.timestamp) }}</span>
              </div>
              <div slot="description">
                <div class="log-source">
                  <span>主机: {{ item.hostname }}</span> | 
                  <span>检查项: {{ item.itemName }}</span>
                </div>
                <div class="log-message">{{ item.message }}</div>
              </div>
            </a-list-item-meta>
          </a-list-item>
          <div slot="footer" class="log-footer">
            <a-empty v-if="logs.length === 0" description="暂无日志数据" />
          </div>
        </a-list>
      </a-spin>
    </div>
  </div>
</template>

<script>
import moment from 'moment'

export default {
  name: 'LogViewer',
  props: {
    clusterId: {
      type: Number,
      required: true
    },
    hostname: {
      type: String,
      default: null
    },
    itemId: {
      type: Number,
      default: null
    },
    refreshInterval: {
      type: Number,
      default: 0 // 0表示不自动刷新
    }
  },
  data() {
    return {
      logs: [],
      loading: false,
      logLevel: undefined,
      timeRange: [],
      keyword: '',
      pagination: {
        onChange: page => {
          this.pagination.current = page
          this.fetchLogs()
        },
        pageSize: 10,
        current: 1,
        total: 0,
        showTotal: total => `共 ${total} 条日志`
      },
      timer: null
    }
  },
  created() {
    this.fetchLogs()
    
    // 设置自动刷新
    if (this.refreshInterval > 0) {
      this.timer = setInterval(() => {
        this.fetchLogs(false) // 静默刷新，不显示加载动画
      }, this.refreshInterval * 1000)
    }
  },
  beforeDestroy() {
    // 清除定时器
    if (this.timer) {
      clearInterval(this.timer)
    }
  },
  methods: {
    async fetchLogs(showLoading = true) {
      if (showLoading) {
        this.loading = true
      }
      
      try {
        const params = {
          clusterId: this.clusterId,
          page: this.pagination.current,
          pageSize: this.pagination.pageSize
        }
        
        // 添加可选参数
        if (this.hostname) {
          params.hostname = this.hostname
        }
        
        if (this.itemId) {
          params.itemId = this.itemId
        }
        
        if (this.logLevel) {
          params.level = this.logLevel
        }
        
        if (this.timeRange && this.timeRange.length === 2) {
          params.startTime = this.timeRange[0].format('YYYY-MM-DD HH:mm:ss')
          params.endTime = this.timeRange[1].format('YYYY-MM-DD HH:mm:ss')
        }
        
        if (this.keyword) {
          params.keyword = this.keyword
        }
        
        // 调用API获取日志
        const res = await this.$axiosPost(global.API.getCheckItemLogs, params)
        
        if (res.code === 200) {
          this.logs = res.data.logs || []
          this.pagination.total = res.data.total || 0
        } else {
          this.$message.error('获取日志失败: ' + res.msg)
        }
      } catch (error) {
        console.error('获取日志时发生错误:', error)
        this.$message.error('获取日志失败，请稍后重试')
      } finally {
        this.loading = false
      }
    },
    search() {
      this.pagination.current = 1 // 重置到第一页
      this.fetchLogs()
    },
    resetFilters() {
      this.logLevel = undefined
      this.timeRange = []
      this.keyword = ''
      this.pagination.current = 1
      this.fetchLogs()
    },
    handleTimeRangeChange(dates) {
      this.timeRange = dates
    },
    formatTime(timestamp) {
      return moment(timestamp).format('YYYY-MM-DD HH:mm:ss')
    },
    getLevelText(level) {
      const levelMap = {
        'INFO': '信息',
        'WARNING': '警告',
        'ERROR': '错误',
        'SUCCESS': '成功'
      }
      return levelMap[level] || level
    }
  }
}
</script>

<style lang="less" scoped>
.log-viewer {
  padding: 20px;
  
  .filter-bar {
    margin-bottom: 20px;
    background-color: #f8f8f8;
    padding: 16px;
    border-radius: 4px;
  }
  
  .log-content {
    background-color: #fff;
    border-radius: 4px;
    padding: 0 16px;
    border: 1px solid #e8e8e8;
    min-height: 400px;
  }
  
  .log-level {
    display: inline-block;
    padding: 2px 8px;
    border-radius: 2px;
    margin-right: 8px;
    font-weight: bold;
    
    &-info {
      background-color: #e6f7ff;
      color: #1890ff;
    }
    
    &-warning {
      background-color: #fffbe6;
      color: #faad14;
    }
    
    &-error {
      background-color: #fff1f0;
      color: #f5222d;
    }
    
    &-success {
      background-color: #f6ffed;
      color: #52c41a;
    }
  }
  
  .log-time {
    color: #999;
    font-size: 12px;
  }
  
  .log-source {
    margin-bottom: 4px;
    color: #666;
  }
  
  .log-message {
    font-family: monospace;
    white-space: pre-wrap;
    word-break: break-all;
  }
  
  .log-footer {
    padding: 16px 0;
    text-align: center;
  }
}
</style> 