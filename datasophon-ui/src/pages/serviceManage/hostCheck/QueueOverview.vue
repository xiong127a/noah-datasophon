<template>
  <div class="queue-overview">
    <a-card title="队列处理总览" :bordered="false">
      <div class="overview-content">
        <!-- 系统控制 -->
        <div class="status-section">
          <h4>系统控制</h4>
          <div class="status-grid">
            <div class="status-item">
              <div class="item-label">系统运行时间</div>
              <div class="item-value">{{ uptime }}</div>
            </div>
            <div class="status-item">
              <div class="item-label">活跃线程</div>
              <div class="item-value">{{ queueStats.totalActiveThreads || 0 }}</div>
            </div>
            <div class="status-item">
              <div class="item-label">总线程数</div>
              <div class="item-value">{{ queueStats.totalPoolSize || 0 }}</div>
            </div>
            <div class="status-item">
              <div class="item-label">完成任务</div>
              <div class="item-value">{{ queueStats.totalCompletedTasks || 0 }}</div>
            </div>
          </div>
        </div>
        
        <!-- 检查队列状态 -->
        <div class="status-section">
          <h4>检查队列状态</h4>
          <div class="status-grid">
            <div class="status-item">
              <div class="item-label">等待任务</div>
              <div class="item-value">{{ queueStats.queueSize || 0 }}</div>
            </div>
            <div class="status-item">
              <div class="item-label">运行中任务</div>
              <div class="item-value">{{ queueStats.runningTasksCount || 0 }}</div>
            </div>
            <div class="status-item">
              <div class="item-label">总处理任务</div>
              <div class="item-value">{{ queueStats.tasksProcessed || 0 }}</div>
            </div>
            <div class="status-item">
              <div class="item-label">成功任务</div>
              <div class="item-value">{{ queueStats.tasksSucceeded || 0 }}</div>
            </div>
            <div class="status-item">
              <div class="item-label">失败任务</div>
              <div class="item-value">{{ queueStats.tasksFailed || 0 }}</div>
            </div>
          </div>
        </div>

        <!-- 修复队列状态 -->
        <div class="status-section">
          <h4>修复队列状态</h4>
          <div class="status-grid">
            <div class="status-item">
              <div class="item-label">等待任务</div>
              <div class="item-value">{{ queueStats.fixQueueSize || 0 }}</div>
            </div>
            <div class="status-item">
              <div class="item-label">运行中任务</div>
              <div class="item-value">{{ queueStats.runningFixTasksCount || 0 }}</div>
            </div>
            <div class="status-item">
              <div class="item-label">总处理任务</div>
              <div class="item-value">{{ queueStats.fixTasksProcessed || 0 }}</div>
            </div>
            <div class="status-item">
              <div class="item-label">成功任务</div>
              <div class="item-value">{{ queueStats.fixTasksSucceeded || 0 }}</div>
            </div>
            <div class="status-item">
              <div class="item-label">失败任务</div>
              <div class="item-value">{{ queueStats.fixTasksFailed || 0 }}</div>
            </div>
          </div>
        </div>

        <!-- 线程池状态 -->
        <div class="status-section">
          <h4>线程池状态</h4>
          <div class="thread-status-grid">
            <!-- 主线程池 -->
            <div class="thread-status-group">
              <div class="thread-group-title">主线程池</div>
              <div class="thread-status-item">
                <div class="item-label">活跃线程</div>
                <div class="item-value">{{ queueStats.mainExecutorActiveCount || 0 }}</div>
              </div>
              <div class="thread-status-item">
                <div class="item-label">排队任务</div>
                <div class="item-value">{{ queueStats.mainExecutorQueueSize || 0 }}</div>
              </div>
            </div>
            
            <!-- 检查项线程池 -->
            <div class="thread-status-group">
              <div class="thread-group-title">检查项线程池</div>
              <div class="thread-status-item">
                <div class="item-label">活跃线程</div>
                <div class="item-value">{{ queueStats.itemExecutorActiveCount || 0 }}</div>
              </div>
              <div class="thread-status-item">
                <div class="item-label">排队任务</div>
                <div class="item-value">{{ queueStats.itemExecutorQueueSize || 0 }}</div>
              </div>
            </div>
            
            <!-- 修复线程池 -->
            <div class="thread-status-group">
              <div class="thread-group-title">修复线程池</div>
              <div class="thread-status-item">
                <div class="item-label">活跃线程</div>
                <div class="item-value">{{ queueStats.fixExecutorActiveCount || 0 }}</div>
              </div>
              <div class="thread-status-item">
                <div class="item-label">排队任务</div>
                <div class="item-value">{{ queueStats.fixExecutorQueueSize || 0 }}</div>
              </div>
            </div>
            
            <!-- 线程池汇总 -->
            <div class="thread-status-group summary">
              <div class="thread-group-title">线程池汇总</div>
              <div class="thread-status-item">
                <div class="item-label">总活跃线程</div>
                <div class="item-value">{{ totalActiveThreads }}</div>
              </div>
              <div class="thread-status-item">
                <div class="item-label">总排队任务</div>
                <div class="item-value">{{ totalQueueSize }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </a-card>
  </div>
</template>

<script>
export default {
  name: 'QueueOverview',
  props: {
    queueStats: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      uptimeTimer: null,
      currentUptime: 0
    }
  },
  computed: {
    totalTasks() {
      const { queueSize = 0, runningTasksCount = 0, tasksProcessed = 0 } = this.queueStats;
      return queueSize + runningTasksCount + tasksProcessed;
    },
    totalFixTasks() {
      const { fixQueueSize = 0, runningFixTasksCount = 0, fixTasksProcessed = 0 } = this.queueStats;
      return fixQueueSize + runningFixTasksCount + fixTasksProcessed;
    },
    // 计算总活跃线程数
    totalActiveThreads() {
      const mainActive = this.queueStats.mainExecutorActiveCount || 0;
      const itemActive = this.queueStats.itemExecutorActiveCount || 0;
      const fixActive = this.queueStats.fixExecutorActiveCount || 0;
      return mainActive + itemActive + fixActive;
    },
    // 计算总排队任务数
    totalQueueSize() {
      const mainQueue = this.queueStats.mainExecutorQueueSize || 0;
      const itemQueue = this.queueStats.itemExecutorQueueSize || 0;
      const fixQueue = this.queueStats.fixExecutorQueueSize || 0;
      return mainQueue + itemQueue + fixQueue;
    },
    // 格式化的运行时间
    uptime() {
      if (!this.queueStats.queueProcessorStartTime) {
        return '未知';
      }
      
      // 格式化运行时间为天时分秒格式
      const seconds = Math.floor(this.currentUptime / 1000);
      const days = Math.floor(seconds / 86400);
      const hours = Math.floor((seconds % 86400) / 3600);
      const minutes = Math.floor((seconds % 3600) / 60);
      const remainingSeconds = seconds % 60;
      
      // 构建显示字符串
      let result = '';
      if (days > 0) result += `${days}天`;
      if (hours > 0 || days > 0) result += `${hours}小时`;
      if (minutes > 0 || hours > 0 || days > 0) result += `${minutes}分`;
      result += `${remainingSeconds}秒`;
      
      return result;
    }
  },
  created() {
    this.startUptimeTimer();
  },
  beforeDestroy() {
    // 清除定时器
    if (this.uptimeTimer) {
      clearInterval(this.uptimeTimer);
    }
  },
  methods: {
    startUptimeTimer() {
      this.updateUptime();
      this.uptimeTimer = setInterval(() => {
        this.updateUptime();
      }, 1000);
    },
    updateUptime() {
      if (this.queueStats.queueProcessorStartTime) {
        const startTime = new Date(this.queueStats.queueProcessorStartTime).getTime();
        this.currentUptime = Date.now() - startTime;
      }
    }
  }
}
</script>

<style lang="less" scoped>
.queue-overview {
  margin-bottom: 20px;
  
  .overview-content {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }
  
  .status-section {
    background-color: #f9f9f9;
    border-radius: 8px;
    padding: 16px;
    
    h4 {
      margin-top: 0;
      margin-bottom: 16px;
      font-size: 16px;
      font-weight: 500;
      color: rgba(0, 0, 0, 0.85);
      border-bottom: 1px solid #e8e8e8;
      padding-bottom: 8px;
    }
  }
  
  .status-grid {
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    gap: 16px;
    
    @media (max-width: 1200px) {
      grid-template-columns: repeat(3, 1fr);
    }
    
    @media (max-width: 768px) {
      grid-template-columns: repeat(2, 1fr);
    }
  }
  
  .thread-status-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
    
    @media (max-width: 1200px) {
      grid-template-columns: repeat(2, 1fr);
    }
    
    @media (max-width: 768px) {
      grid-template-columns: 1fr;
    }
  }
  
  .thread-status-group {
    background-color: #ffffff;
    border: 1px solid #e8e8e8;
    border-radius: 6px;
    padding: 12px;
    
    &.summary {
      background-color: #e6f7ff;
      border-color: #91d5ff;
    }
  }
  
  .thread-group-title {
    font-weight: 500;
    margin-bottom: 8px;
    color: rgba(0, 0, 0, 0.85);
    font-size: 14px;
    text-align: center;
    border-bottom: 1px dashed #e8e8e8;
    padding-bottom: 6px;
  }
  
  .status-item, .thread-status-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    background-color: #ffffff;
    border-radius: 4px;
    padding: 12px 8px;
    text-align: center;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  }
  
  .thread-status-item {
    background-color: transparent;
    box-shadow: none;
    border-bottom: 1px dashed #f0f0f0;
    padding: 8px 0;
    margin-bottom: 4px;
    
    &:last-child {
      border-bottom: none;
      margin-bottom: 0;
    }
  }
  
  .item-label {
    font-size: 12px;
    color: rgba(0, 0, 0, 0.65);
    margin-bottom: 4px;
  }
  
  .item-value {
    font-size: 18px;
    font-weight: 500;
    color: rgba(0, 0, 0, 0.85);
  }
}
</style> 