<!--
  队列状态指示器组件
  显示队列管理器状态，点击状态灯显示详情弹窗
-->
<template>
  <div>
    <!-- 简洁视图：只包含状态灯，去掉开关 -->
    <div class="queue-status-compact">
      <a-tooltip :title="queueStatusTooltip">
        <div 
          class="status-light" 
          :class="queueStatusClass" 
          @click="showDetailModal = true"
        />
      </a-tooltip>
    </div>

    <!-- 详情弹窗 -->
    <a-modal
      title="队列系统详情"
      :visible="showDetailModal"
      :footer="null"
      @cancel="showDetailModal = false"
      width="800px"
    >
      <div class="detail-container">
        <div class="detail-header">
          <h3>任务队列与服务状态</h3>
          <a-button type="primary" size="small" @click="fetchStatus">
            <a-icon type="reload" />刷新
          </a-button>
        </div>

        <!-- 队列总览 -->
        <div class="detail-section">
          <div class="section-header">
            <h4>队列处理总览</h4>
            <div class="status-control">
              <div class="status-item">
                <div class="status-light" :class="queueStatusClass" />
                <span>{{ queueActive ? '活跃' : '已暂停' }}</span>
              </div>
              <a-switch
                :checked="queueActive"
                :loading="queueLoading"
                @click="toggleQueue"
              />
            </div>
          </div>
          
          <div class="detail-stats">
            <div class="stat-item">
              <div class="stat-label">处理线程状态</div>
              <div class="stat-value" :class="queueStats.processorThreadAlive ? 'text-success' : 'text-danger'">
                {{ queueStats.processorThreadAlive ? '活跃' : '已停止' }}
              </div>
            </div>
            <div class="stat-item">
              <div class="stat-label">运行时间</div>
              <div class="stat-value">{{ formatDuration(queueStats.runningTime) }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">总处理任务数</div>
              <div class="stat-value">{{ queueStats.tasksProcessed || 0 }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">任务成功/失败</div>
              <div class="stat-value">
                <span class="text-success">{{ queueStats.tasksSucceeded || 0 }}</span> / 
                <span class="text-danger">{{ queueStats.tasksFailed || 0 }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 任务队列 -->
        <a-tabs defaultActiveKey="1">
          <a-tab-pane key="1" tab="检查任务队列">
            <div class="detail-section no-padding">
              <div class="section-header with-padding">
                <h4>检查任务队列</h4>
                <div class="status-control">
                  <a-button type="primary" size="small" @click="showQueueDetails = true">
                    查看详情
                  </a-button>
                </div>
              </div>
              
              <div class="detail-stats with-padding">
                <div class="stat-item">
                  <div class="stat-label">等待任务</div>
                  <div class="stat-value">{{ queueStats.queueSize || 0 }}</div>
                </div>
                <div class="stat-item">
                  <div class="stat-label">正在执行</div>
                  <div class="stat-value">{{ queueStats.runningTasks || 0 }}</div>
                </div>
                <div class="stat-item">
                  <div class="stat-label">已完成任务</div>
                  <div class="stat-value">{{ queueStats.completedTasks || 0 }}</div>
                </div>
                <div class="stat-item">
                  <div class="stat-label">队列容量</div>
                  <div class="stat-value">{{ queueStats.queueCapacity || 100 }}</div>
                </div>
              </div>
              
              <!-- 线程池状态 -->
              <div class="sub-section">
                <div class="sub-header with-padding">
                  <h5>线程池状态</h5>
                </div>
                
                <div class="detail-stats with-padding">
                  <div class="stat-item">
                    <div class="stat-label">活跃线程</div>
                    <div class="stat-value">{{ queueStats.activeThreads || 0 }}</div>
                  </div>
                  <div class="stat-item">
                    <div class="stat-label">核心线程</div>
                    <div class="stat-value">{{ queueStats.corePoolSize || 0 }}</div>
                  </div>
                  <div class="stat-item">
                    <div class="stat-label">最大线程</div>
                    <div class="stat-value">{{ queueStats.maxPoolSize || 0 }}</div>
                  </div>
                  <div class="stat-item">
                    <div class="stat-label">当前线程池大小</div>
                    <div class="stat-value">{{ queueStats.poolSize || 0 }}</div>
                  </div>
                </div>
              </div>
            </div>
          </a-tab-pane>
          
          <a-tab-pane key="2" tab="修复任务队列">
            <div class="detail-section no-padding">
              <div class="section-header with-padding">
                <h4>修复任务队列</h4>
                <div class="status-control">
                  <a-button type="primary" size="small" @click="showFixQueueDetails = true">
                    查看详情
                  </a-button>
                </div>
              </div>
              
              <div class="detail-stats with-padding">
                <div class="stat-item">
                  <div class="stat-label">等待修复任务</div>
                  <div class="stat-value">{{ queueStats.fixQueueSize || 0 }}</div>
                </div>
                <div class="stat-item">
                  <div class="stat-label">正在执行修复</div>
                  <div class="stat-value">{{ queueStats.runningFixTasks || 0 }}</div>
                </div>
                <div class="stat-item">
                  <div class="stat-label">已完成修复</div>
                  <div class="stat-value">{{ queueStats.completedFixTasks || 0 }}</div>
                </div>
                <div class="stat-item">
                  <div class="stat-label">修复成功率</div>
                  <div class="stat-value">
                    {{ calculateSuccessRate(queueStats.fixTasksSucceeded, queueStats.fixTasksFailed) }}%
                  </div>
                </div>
              </div>
              
              <!-- 执行统计 -->
              <div class="sub-section">
                <div class="sub-header with-padding">
                  <h5>修复任务统计</h5>
                </div>
                
                <div class="detail-stats with-padding">
                  <div class="stat-item">
                    <div class="stat-label">修复成功</div>
                    <div class="stat-value text-success">{{ queueStats.fixTasksSucceeded || 0 }}</div>
                  </div>
                  <div class="stat-item">
                    <div class="stat-label">修复失败</div>
                    <div class="stat-value text-danger">{{ queueStats.fixTasksFailed || 0 }}</div>
                  </div>
                  <div class="stat-item">
                    <div class="stat-label">平均执行时间</div>
                    <div class="stat-value">{{ formatDuration(queueStats.avgFixTaskTime) }}</div>
                  </div>
                  <div class="stat-item">
                    <div class="stat-label">最长执行时间</div>
                    <div class="stat-value">{{ formatDuration(queueStats.maxFixTaskTime) }}</div>
                  </div>
                </div>
              </div>
            </div>
          </a-tab-pane>
        </a-tabs>

        <!-- SSH连接管理 -->
        <div class="detail-section">
          <div class="section-header">
            <h4>SSH连接管理</h4>
            <div class="status-control">
              <a-button size="small" type="primary" @click="cleanupConnections" :loading="cleanupLoading">
                清理不活跃连接
              </a-button>
            </div>
          </div>
          
          <div class="detail-stats">
            <div class="stat-item">
              <div class="stat-label">活跃连接数</div>
              <div class="stat-value">{{ schedulerStats.connectionPoolSize || 0 }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">连接清理状态</div>
              <div class="stat-value" :class="schedulerStats.connectionCleanupActive ? 'text-success' : 'text-warning'">
                {{ schedulerStats.connectionCleanupActive ? '活跃' : '已停止' }}
              </div>
            </div>
            <div class="stat-item">
              <div class="stat-label">上次清理时间</div>
              <div class="stat-value">{{ schedulerStats.lastConnectionCleanupTime || '未执行' }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">SSH会话缓存</div>
              <div class="stat-value">{{ schedulerStats.sessionCacheHitRate || '0' }}%</div>
            </div>
          </div>
        </div>

        <!-- 定时任务管理 -->
        <div class="detail-section">
          <div class="section-header">
            <h4>定时任务管理</h4>
            <div class="status-control">
              <div class="status-item">
                <div class="status-light" :class="schedulerStatusClass" />
                <span>{{ schedulerActive ? '活跃' : '已暂停' }}</span>
              </div>
              <a-switch
                :checked="schedulerActive"
                :loading="schedulerLoading"
                @click="toggleScheduler"
              />
            </div>
          </div>
          
          <div class="scheduled-tasks">
            <a-table
              :columns="scheduledTaskColumns"
              :dataSource="scheduledTasks"
              size="small"
              :pagination="false"
              :rowKey="record => record.id"
            >
              <template slot="status" slot-scope="text, record">
                <a-badge :status="record.active ? 'success' : 'error'" />
                <span>{{ record.active ? '活跃' : '已停止' }}</span>
              </template>
              
              <template slot="action" slot-scope="text, record">
                <a-switch
                  size="small"
                  :checked="record.active"
                  :loading="record.loading"
                  @click="toggleTask(record)"
                />
              </template>
              
              <template slot="lastRun" slot-scope="text">
                {{ text || '未执行' }}
              </template>
            </a-table>
          </div>
        </div>
        
        <!-- 任务执行统计 -->
        <div class="detail-section">
          <div class="section-header">
            <h4>任务执行统计</h4>
          </div>
          
          <div class="detail-stats">
            <div class="stat-item">
              <div class="stat-label">运行中任务</div>
              <div class="stat-value">{{ schedulerStats.runningTasksCount || 0 }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">任务清理状态</div>
              <div class="stat-value" :class="schedulerStats.taskCleanupActive ? 'text-success' : 'text-warning'">
                {{ schedulerStats.taskCleanupActive ? '活跃' : '已停止' }}
              </div>
            </div>
            <div class="stat-item">
              <div class="stat-label">上次任务清理</div>
              <div class="stat-value">{{ schedulerStats.lastTaskCleanupTime || '未执行' }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">过期任务清理</div>
              <div class="stat-value">{{ schedulerStats.expiredTasksCleared || 0 }}个</div>
            </div>
          </div>
        </div>
      </div>
    </a-modal>
    
    <!-- 队列详情弹窗 -->
    <a-modal
      title="检查任务队列详情"
      :visible="showQueueDetails"
      :footer="null"
      @cancel="showQueueDetails = false"
      width="800px"
    >
      <a-table
        :columns="queueTaskColumns"
        :dataSource="queueTasks"
        size="small"
        :scroll="{ y: 400 }"
        :pagination="{ pageSize: 10 }"
        :rowKey="record => record.taskKey"
      >
        <template slot="status" slot-scope="text">
          <a-tag :color="getStatusColor(text)">{{ text }}</a-tag>
        </template>
        
        <template slot="action" slot-scope="text, record">
          <a @click="cancelTask(record)">取消</a>
        </template>
        
        <template slot="duration" slot-scope="text">
          {{ formatDuration(text) }}
        </template>
      </a-table>
    </a-modal>
    
    <!-- 修复任务队列详情弹窗 -->
    <a-modal
      title="修复任务队列详情"
      :visible="showFixQueueDetails"
      :footer="null"
      @cancel="showFixQueueDetails = false"
      width="800px"
    >
      <a-table
        :columns="fixTaskColumns"
        :dataSource="fixQueueTasks"
        size="small"
        :scroll="{ y: 400 }"
        :pagination="{ pageSize: 10 }"
        :rowKey="record => record.taskKey"
      >
        <template slot="status" slot-scope="text">
          <a-tag :color="getStatusColor(text)">{{ text }}</a-tag>
        </template>
        
        <template slot="action" slot-scope="text, record">
          <a @click="cancelFixTask(record)">取消</a>
        </template>
        
        <template slot="duration" slot-scope="text">
          {{ formatDuration(text) }}
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<script>
import { message } from 'ant-design-vue'

export default {
  name: 'QueueStatusIndicator',
  data() {
    return {
      queueStatus: {},
      queueActive: false,
      schedulerActive: false,
      queueLoading: false,
      schedulerLoading: false,
      pollInterval: null,
      showDetailModal: false,
      queueStats: {},
      schedulerStats: {},
      showQueueDetails: false,
      showFixQueueDetails: false,
      cleanupLoading: false,
      // 队列任务数据
      queueTasks: [],
      fixQueueTasks: [],
      // 定时任务数据
      scheduledTasks: [
        {
          id: 'taskCleanup',
          name: '任务清理',
          description: '清理已完成的过期任务',
          interval: '1小时',
          active: false,
          lastRun: '',
          loading: false
        },
        {
          id: 'connectionCleanup',
          name: '连接清理',
          description: '清理不活跃的SSH连接',
          interval: '10分钟',
          active: false,
          lastRun: '',
          loading: false
        },
        {
          id: 'queueHealthMonitor',
          name: '队列健康监控',
          description: '监控队列处理线程的健康状态',
          interval: '1分钟',
          active: false,
          lastRun: '',
          loading: false
        },
        {
          id: 'taskTimeoutMonitor',
          name: '任务超时监控',
          description: '检查和处理超时的任务',
          interval: '5分钟',
          active: false,
          lastRun: '',
          loading: false
        }
      ],
      // 列定义
      scheduledTaskColumns: [
        {
          title: '任务名称',
          dataIndex: 'name',
          key: 'name',
          width: '15%'
        },
        {
          title: '描述',
          dataIndex: 'description',
          key: 'description',
          width: '35%'
        },
        {
          title: '执行间隔',
          dataIndex: 'interval',
          key: 'interval',
          width: '15%'
        },
        {
          title: '状态',
          key: 'status',
          scopedSlots: { customRender: 'status' },
          width: '15%'
        },
        {
          title: '上次执行',
          dataIndex: 'lastRun',
          key: 'lastRun',
          scopedSlots: { customRender: 'lastRun' },
          width: '15%'
        },
        {
          title: '操作',
          key: 'action',
          scopedSlots: { customRender: 'action' },
          width: '10%'
        }
      ],
      queueTaskColumns: [
        {
          title: '主机',
          dataIndex: 'hostname',
          key: 'hostname',
          width: '15%'
        },
        {
          title: '检查项',
          dataIndex: 'itemName',
          key: 'itemName',
          width: '20%'
        },
        {
          title: '状态',
          dataIndex: 'status',
          key: 'status',
          width: '10%',
          scopedSlots: { customRender: 'status' }
        },
        {
          title: '开始时间',
          dataIndex: 'startTime',
          key: 'startTime',
          width: '15%'
        },
        {
          title: '执行时长',
          dataIndex: 'duration',
          key: 'duration',
          width: '15%',
          scopedSlots: { customRender: 'duration' }
        },
        {
          title: '优先级',
          dataIndex: 'priority',
          key: 'priority',
          width: '10%'
        },
        {
          title: '操作',
          key: 'action',
          scopedSlots: { customRender: 'action' },
          width: '10%'
        }
      ],
      fixTaskColumns: [
        {
          title: '主机',
          dataIndex: 'hostname',
          key: 'hostname',
          width: '15%'
        },
        {
          title: '修复项',
          dataIndex: 'itemName',
          key: 'itemName',
          width: '20%'
        },
        {
          title: '状态',
          dataIndex: 'status',
          key: 'status',
          width: '10%',
          scopedSlots: { customRender: 'status' }
        },
        {
          title: '开始时间',
          dataIndex: 'startTime',
          key: 'startTime',
          width: '15%'
        },
        {
          title: '执行时长',
          dataIndex: 'duration',
          key: 'duration',
          width: '15%',
          scopedSlots: { customRender: 'duration' }
        },
        {
          title: '结果',
          dataIndex: 'result',
          key: 'result',
          width: '10%'
        },
        {
          title: '操作',
          key: 'action',
          scopedSlots: { customRender: 'action' },
          width: '10%'
        }
      ]
    }
  },
  computed: {
    queueStatusClass() {
      return {
        'status-active': this.queueActive,
        'status-inactive': !this.queueActive
      }
    },
    schedulerStatusClass() {
      return {
        'status-active': this.schedulerActive,
        'status-inactive': !this.schedulerActive
      }
    },
    queueStatusTooltip() {
      return this.queueActive ? '队列处理：活跃（点击查看详情）' : '队列处理：已暂停（点击查看详情）'
    }
  },
  mounted() {
    this.fetchStatus()
    // 设置轮询，每30秒更新一次状态
    this.pollInterval = setInterval(() => {
      this.fetchStatus()
    }, 30000)
  },
  beforeDestroy() {
    // 组件销毁时清除轮询
    if (this.pollInterval) {
      clearInterval(this.pollInterval)
    }
  },
  methods: {
    async fetchStatus() {
      try {
        // 获取状态信息
        const response = await this.$axiosGet(global.API.queueManager + '?action=status', {})
        
        // 确保有响应
        if (!response) {
          console.error('获取状态失败: 无响应');
          return;
        }
        
        if (response.code === 200) {
          // 解析响应数据
          const data = response.data || {};
          
          // 更新队列状态
          const queueManager = data.queueManager || {};
          this.queueActive = Boolean(queueManager.queueProcessingEnabled);
          this.queueStats = queueManager;
          
          // 更新定时任务状态
          const asyncService = data.asyncService || {};
          this.schedulerActive = Boolean(asyncService.scheduledTasksEnabled);
          this.schedulerStats = asyncService;
          
          // 更新定时任务列表状态
          this.updateScheduledTasksStatus(asyncService);
          
          // 更新任务队列数据
          if (data.queueTasks) {
            this.queueTasks = data.queueTasks;
          }
          
          // 更新修复任务队列数据
          if (data.fixQueueTasks) {
            this.fixQueueTasks = data.fixQueueTasks;
          }
          
          this.queueStatus = data;
        } else {
          console.error('获取状态失败:', response.msg || '未知错误');
        }
      } catch (error) {
        console.error('获取状态异常:', error);
      }
    },
    
    // 更新定时任务状态
    updateScheduledTasksStatus(asyncService) {
      // 任务清理
      const taskCleanup = this.scheduledTasks.find(t => t.id === 'taskCleanup');
      if (taskCleanup) {
        taskCleanup.active = Boolean(asyncService.taskCleanupActive);
        taskCleanup.lastRun = asyncService.lastTaskCleanupTime;
      }
      
      // 连接清理
      const connectionCleanup = this.scheduledTasks.find(t => t.id === 'connectionCleanup');
      if (connectionCleanup) {
        connectionCleanup.active = Boolean(asyncService.connectionCleanupActive);
        connectionCleanup.lastRun = asyncService.lastConnectionCleanupTime;
      }
      
      // 队列健康监控
      const queueHealthMonitor = this.scheduledTasks.find(t => t.id === 'queueHealthMonitor');
      if (queueHealthMonitor) {
        queueHealthMonitor.active = Boolean(asyncService.queueHealthMonitorActive);
        queueHealthMonitor.lastRun = asyncService.lastQueueHealthCheckTime;
      }
      
      // 任务超时监控
      const taskTimeoutMonitor = this.scheduledTasks.find(t => t.id === 'taskTimeoutMonitor');
      if (taskTimeoutMonitor) {
        taskTimeoutMonitor.active = Boolean(asyncService.taskTimeoutMonitorActive);
        taskTimeoutMonitor.lastRun = asyncService.lastTaskTimeoutCheckTime;
      }
    },
    
    // 切换单个定时任务
    async toggleTask(task) {
      task.loading = true;
      try {
        // 构建操作
        const action = task.active ? 'pauseTask' : 'resumeTask';
        const taskId = task.id;
        
        // 发送请求
        const response = await this.$axiosGet(global.API.queueManager + `?action=${action}&taskId=${taskId}`, {});
        
        if (response && response.code === 200) {
          task.active = !task.active;
          message.success(`${task.name} 已${task.active ? '启用' : '暂停'}`);
          
          // 刷新状态
          setTimeout(() => this.fetchStatus(), 1000);
        } else {
          message.error(`操作失败: ${response?.msg || '未知错误'}`);
        }
      } catch (error) {
        console.error('切换定时任务状态异常:', error);
        message.error('操作异常，请查看控制台日志');
      } finally {
        task.loading = false;
      }
    },
    
    // 取消检查任务
    async cancelTask(task) {
      try {
        const clusterId = task.clusterId;
        const hostname = task.hostname;
        const itemId = task.itemId;
        
        const response = await this.$axiosPost(global.API.stopCheckItem, {
          clusterId: clusterId,
          hostname: hostname,
          itemId: itemId
        });
        
        if (response && response.code === 200) {
          message.success('任务已取消');
          // 刷新队列状态
          this.fetchStatus();
        } else {
          message.error(`取消任务失败: ${response?.msg || '未知错误'}`);
        }
      } catch (error) {
        console.error('取消任务异常:', error);
        message.error('操作异常，请查看控制台日志');
      }
    },
    
    // 取消修复任务
    async cancelFixTask(task) {
      try {
        const clusterId = task.clusterId;
        const hostname = task.hostname;
        const itemId = task.itemId;
        
        const response = await this.$axiosPost(global.API.stopCheckItem + '/fix', {
          clusterId: clusterId,
          hostname: hostname,
          itemId: itemId
        });
        
        if (response && response.code === 200) {
          message.success('修复任务已取消');
          // 刷新队列状态
          this.fetchStatus();
        } else {
          message.error(`取消修复任务失败: ${response?.msg || '未知错误'}`);
        }
      } catch (error) {
        console.error('取消修复任务异常:', error);
        message.error('操作异常，请查看控制台日志');
      }
    },
    
    // 格式化持续时间
    formatDuration(ms) {
      if (!ms) return '0秒';
      
      const seconds = Math.floor(ms / 1000);
      const minutes = Math.floor(seconds / 60);
      const hours = Math.floor(minutes / 60);
      
      if (hours > 0) {
        return `${hours}小时${minutes % 60}分钟`;
      } else if (minutes > 0) {
        return `${minutes}分钟${seconds % 60}秒`;
      } else {
        return `${seconds}秒`;
      }
    },
    
    // 计算成功率
    calculateSuccessRate(success, failed) {
      const total = (success || 0) + (failed || 0);
      if (total === 0) return 0;
      return Math.round((success / total) * 100);
    },
    
    // 获取状态颜色
    getStatusColor(status) {
      const statusMap = {
        'PENDING': 'blue',
        'RUNNING': 'green',
        'COMPLETED': 'success',
        'FAILED': 'error',
        'CANCELLED': 'orange',
        'TIMEOUT': 'red',
        'SKIPPED': 'purple'
      };
      
      return statusMap[status] || 'default';
    },
    
    async toggleQueue() {
      this.queueLoading = true
      try {
        const action = this.queueActive ? 'pause' : 'resume'
        // 切换队列处理状态
        const response = await this.$axiosGet(global.API.queueManager + `?action=${action}&scope=queue`, {})
        
        if (response && response.code === 200) {
          this.queueActive = !this.queueActive
          message.success(`队列处理已${this.queueActive ? '启用' : '暂停'}`)
          
          // 刷新状态
          setTimeout(() => this.fetchStatus(), 1000)
        } else {
          message.error(`操作失败: ${response?.msg || '未知错误'}`)
        }
      } catch (error) {
        console.error('切换队列状态异常:', error)
        message.error('操作异常，请查看控制台日志')
      } finally {
        this.queueLoading = false
      }
    },
    
    async toggleScheduler() {
      this.schedulerLoading = true
      try {
        const action = this.schedulerActive ? 'pause' : 'resume'
        // 切换定时任务状态
        const response = await this.$axiosGet(global.API.queueManager + `?action=${action}&scope=scheduler`, {})
        
        if (response && response.code === 200) {
          this.schedulerActive = !this.schedulerActive
          message.success(`定时任务已${this.schedulerActive ? '启用' : '暂停'}`)
          
          // 刷新状态
          setTimeout(() => this.fetchStatus(), 1000)
        } else {
          message.error(`操作失败: ${response?.msg || '未知错误'}`)
        }
      } catch (error) {
        console.error('切换定时任务状态异常:', error)
        message.error('操作异常，请查看控制台日志')
      } finally {
        this.schedulerLoading = false
      }
    },
    async cleanupConnections() {
      this.cleanupLoading = true
      try {
        const response = await this.$axiosGet(global.API.queueManager + '?action=cleanupConnections', {})
        
        if (response && response.code === 200) {
          message.success('不活跃连接清理成功')
          
          // 刷新状态
          setTimeout(() => this.fetchStatus(), 1000)
        } else {
          message.error(`操作失败: ${response?.msg || '未知错误'}`)
        }
      } catch (error) {
        console.error('清理不活跃连接异常:', error)
        message.error('操作异常，请查看控制台日志')
      } finally {
        this.cleanupLoading = false
      }
    }
  }
}
</script>

<style scoped>
/* 简洁视图样式 */
.queue-status-compact {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-light {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  transition: background-color 0.3s ease;
  cursor: pointer;
}

.status-active {
  background-color: #52c41a; /* 绿色 - 活跃 */
  box-shadow: 0 0 5px #52c41a;
}

.status-inactive {
  background-color: #ff4d4f; /* 红色 - 不活跃 */
  box-shadow: 0 0 5px #ff4d4f;
}

/* 详情弹窗样式 */
.detail-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.detail-header h3 {
  margin: 0;
}

.detail-section {
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  padding: 16px;
  background-color: #fafafa;
}

.detail-section.no-padding {
  padding: 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header.with-padding {
  padding: 16px 16px 0 16px;
}

.section-header h4 {
  margin: 0;
}

.sub-section {
  border-top: 1px solid #e8e8e8;
  margin-top: 8px;
}

.sub-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 8px 0;
}

.sub-header.with-padding {
  padding: 8px 16px 0 16px;
}

.sub-header h5 {
  margin: 0;
  font-size: 14px;
  color: rgba(0, 0, 0, 0.85);
}

.status-control {
  display: flex;
  align-items: center;
  gap: 16px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.detail-stats.with-padding {
  padding: 8px 16px 16px 16px;
}

.stat-item {
  background-color: white;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  padding: 8px 12px;
}

.stat-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 14px;
  font-weight: 500;
}

.scheduled-tasks {
  margin-top: 8px;
}

.exec-times {
  margin-top: 8px;
}

.time-item {
  display: flex;
  margin-bottom: 4px;
}

.time-label {
  flex: 0 0 120px;
  font-size: 12px;
  color: #8c8c8c;
}

.time-value {
  font-size: 12px;
}

/* 文本颜色 */
.text-success {
  color: #52c41a;
}

.text-warning {
  color: #faad14;
}

.text-danger {
  color: #ff4d4f;
}

.text-info {
  color: #1890ff;
}

/* 适配深色主题的开关样式 */
:deep(.ant-switch) {
  background-color: rgba(0, 0, 0, 0.25);
}

:deep(.ant-switch-checked) {
  background-color: #1890ff;
}

:deep(.ant-tooltip) {
  max-width: 200px;
}

/* 表格样式优化 */
:deep(.ant-table-small) {
  border-radius: 4px;
  border: 1px solid #e8e8e8;
}

:deep(.ant-table-thead > tr > th) {
  background-color: #fafafa;
}

:deep(.ant-badge-status-dot) {
  width: 8px;
  height: 8px;
}

:deep(.ant-table-pagination.ant-pagination) {
  margin: 8px 0;
}

/* 标签页样式 */
:deep(.ant-tabs-nav) {
  margin-bottom: 0;
}

:deep(.ant-tabs-tab) {
  padding: 8px 16px;
}

:deep(.ant-tabs-content) {
  background: white;
  border: 1px solid #e8e8e8;
  border-top: none;
  border-radius: 0 0 4px 4px;
}
</style> 