<!--
  队列状态指示器组件
  显示队列管理器状态，点击状态灯显示详情弹窗
-->
<template>
  <div>
    <!-- 简洁视图：只包含状态灯 -->
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
      width="1000px"
    >
      <div class="detail-container">
        <div class="detail-header">
          <h3>任务队列与服务状态</h3>
          <div class="header-actions">
            <div class="action-item">
              <a-popconfirm
                title="确定要关闭整个队列系统吗？此操作将停止所有任务和定时任务。"
                @confirm="shutdownSystem"
              >
                <a-tooltip title="关闭系统">
                  <a-button type="danger" danger size="small">
                    <a-icon type="close-circle" />
                  </a-button>
                </a-tooltip>
              </a-popconfirm>
            </div>
            
            <div class="action-item">
              <a-tooltip title="手动刷新数据">
                <a-button type="primary" size="small" @click="fetchFullStatus(true)">
                  <a-icon type="reload" />
                </a-button>
              </a-tooltip>
            </div>
            
            <div class="action-item">
              <a-tooltip :title="isAutoRefresh ? '关闭自动刷新' : '开启自动刷新'">
                <a-switch
                  size="small"
                  :checked="isAutoRefresh"
                  @change="toggleAutoRefresh"
                  checkedChildren="自动刷新"
                  unCheckedChildren="手动刷新"
                />
              </a-tooltip>
            </div>
          </div>
        </div>

        <!-- 系统控制 -->
        <div class="detail-section">
          <div class="section-header">
            <h4>系统控制</h4>
            <div class="status-control">
              <a-tooltip :title="systemStatusText">
                <div class="status-light" :class="systemStatusClass" />
              </a-tooltip>
              <a-switch
                :checked="systemActive"
                :loading="systemLoading"
                @change="toggleSystem"
              />
            </div>
          </div>
          
          <div class="detail-stats">
            <div class="stat-item">
              <div class="stat-label">活跃线程</div>
              <div class="stat-value">{{ queueStats.totalActiveThreads || 0 }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">总线程数</div>
              <div class="stat-value">{{ queueStats.totalPoolSize || 0 }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">完成任务</div>
              <div class="stat-value">{{ queueStats.totalCompletedTasks || 0 }}</div>
            </div>
          </div>
        </div>

        <!-- 队列总览 -->
        <div class="detail-section">
          <div class="section-header">
            <h4>队列处理总览</h4>
          </div>
          
          <div class="detail-stats">
            <div class="stat-item">
              <div class="stat-label">总任务数</div>
              <div class="stat-value">{{ queueStats.tasksProcessed || 0 }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">成功任务</div>
              <div class="stat-value success-text">{{ queueStats.tasksSucceeded || 0 }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">失败任务</div>
              <div class="stat-value danger-text">{{ queueStats.tasksFailed || 0 }}</div>
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
                    <div class="stat-label">总活跃线程</div>
                    <div class="stat-value">{{ queueStats.totalActiveThreads || 0 }}</div>
                  </div>
                  <div class="stat-item">
                    <div class="stat-label">总线程池大小</div>
                    <div class="stat-value">{{ queueStats.totalPoolSize || 0 }}</div>
                  </div>
                  <div class="stat-item">
                    <div class="stat-label">总完成任务</div>
                    <div class="stat-value">{{ queueStats.totalCompletedTasks || 0 }}</div>
                  </div>
                  <div class="stat-item">
                    <div class="stat-label">总队列任务</div>
                    <div class="stat-value">{{ queueStats.totalQueuedTasks || 0 }}</div>
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
              <div class="stat-value">{{ formatDateTime(schedulerStats.lastConnectionCleanupTime) }}</div>
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
              <a-space>
                <a-tooltip title="暂停所有定时任务">
                  <a-button
                    type="primary"
                    size="small"
                    :disabled="!schedulerActive"
                    @click="toggleScheduler"
                  >
                    <a-icon type="pause-circle" /> 暂停全部
                  </a-button>
                </a-tooltip>
                <a-tooltip title="启动所有定时任务">
                  <a-button
                    type="primary"
                    size="small"
                    :disabled="schedulerActive"
                    @click="toggleScheduler"
                  >
                    <a-icon type="play-circle" /> 启动全部
                  </a-button>
                </a-tooltip>
              </a-space>
            </div>
          </div>
          
          <div class="scheduled-tasks">
            <a-alert
              type="info"
              show-icon
              style="margin-bottom: 10px;"
            >
              <template slot="message">
                您可以调整定时任务执行间隔，输入秒数后点击保存按钮。各任务的最小间隔各不相同，系统会自动限制。
              </template>
            </a-alert>
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
              
              <template slot="interval" slot-scope="text, record">
                <div class="interval-editor">
                  <a-input-number
                    v-model="record.newInterval"
                    :min="getMinInterval(record.id)"
                    :max="3600"
                    :step="1"
                    placeholder="请输入间隔(秒)"
                    style="width: 90px;"
                  />
                  <span class="interval-unit">秒</span>
                  <a-button 
                    type="primary" 
                    size="small" 
                    @click="updateTaskInterval(record)"
                    :loading="record.saving"
                  >
                    保存
                  </a-button>
                  <a-tooltip title="恢复默认间隔">
                    <a-button 
                      type="link" 
                      size="small" 
                      icon="undo" 
                      @click="resetToDefaultInterval(record)"
                    />
                  </a-tooltip>
                </div>
              </template>
              
              <template slot="action" slot-scope="text, record">
                <a-space>
                  <a-switch
                    size="small"
                    :checked="record.active"
                    :loading="record.loading"
                    @click="toggleTask(record)"
                  />
                  <a-popconfirm
                    title="确定要重置此任务吗？"
                    @confirm="resetTask(record)"
                  >
                    <a-button type="link" size="small" :disabled="!record.active">
                      重置
                    </a-button>
                  </a-popconfirm>
                </a-space>
              </template>
              
              <template slot="lastRun" slot-scope="text">
                {{ formatDateTime(text) }}
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
              <div class="stat-value">{{ formatDateTime(schedulerStats.lastTaskCleanupTime) }}</div>
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
      width="1000px"
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
        
        <template slot="startTime" slot-scope="text">
          {{ formatDateTime(text) }}
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
      width="1000px"
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
        
        <template slot="startTime" slot-scope="text">
          {{ formatDateTime(text) }}
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
  props: {
    queueStatus: {
      type: Object,
      default: () => ({
        queueSize: 0,
        runningTasks: 0,
        processorThreadAlive: true
      })
    }
  },
  data() {
    return {
      queueActive: true,
      schedulerActive: true,
      queueLoading: false,
      schedulerLoading: false,
      showDetailModal: false,
      queueStats: {},
      schedulerStats: {},
      showQueueDetails: false,
      showFixQueueDetails: false,
      cleanupLoading: false,
      queueTasks: [],
      fixQueueTasks: [],
      scheduledTasks: [
        {
          id: 'taskCleanup',
          name: '任务清理',
          description: '清理已完成的过期任务',
          interval: '1小时',
          newInterval: 3600,
          active: false,
          lastRun: '',
          loading: false,
          saving: false,
          defaultInterval: 3600
        },
        {
          id: 'connectionCleanup',
          name: '连接清理',
          description: '清理不活跃的SSH连接',
          interval: '10分钟',
          newInterval: 600,
          active: false,
          lastRun: '',
          loading: false,
          saving: false,
          defaultInterval: 600
        },
        {
          id: 'queueHealthMonitor',
          name: '队列健康监控',
          description: '监控队列处理线程的健康状态',
          interval: '1分钟',
          newInterval: 60,
          active: false,
          lastRun: '',
          loading: false,
          saving: false,
          defaultInterval: 60
        },
        {
          id: 'taskTimeoutMonitor',
          name: '任务超时监控',
          description: '检查和处理超时的任务',
          interval: '5分钟',
          newInterval: 300,
          active: false,
          lastRun: '',
          loading: false,
          saving: false,
          defaultInterval: 300
        }
      ],
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
          width: '25%'
        },
        {
          title: '执行间隔',
          dataIndex: 'interval',
          key: 'interval',
          width: '25%',
          scopedSlots: { customRender: 'interval' }
        },
        {
          title: '状态',
          key: 'status',
          scopedSlots: { customRender: 'status' },
          width: '10%'
        },
        {
          title: '上次执行',
          dataIndex: 'lastRun',
          key: 'lastRun',
          scopedSlots: { customRender: 'lastRun' },
          width: '15%',
          className: 'timestamp-column'
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
          width: '20%',
          className: 'timestamp-column'
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
          width: '18%',
          className: 'timestamp-column'
        },
        {
          title: '执行时长',
          dataIndex: 'duration',
          key: 'duration',
          width: '12%',
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
      ],
      systemActive: true,
      lastOperationTime: null,
      lastOperator: null,
      systemUptime: 0,
      refreshInterval: null,
      isAutoRefresh: true,
      startTime: null,
      uptime: 0,
      uptimeTimer: null,
      queueProcessorStartTime: null,
      queueProcessorUptime: 0,
      queueProcessorTimer: null,
      systemLoading: false,
    }
  },
  created() {
    // 获取应用启动时间
    this.fetchStartTime();
    // 启动定时更新运行时间
    this.startUptimeTimer();
  },
  beforeDestroy() {
    // 组件销毁前清除定时器
    this.stopAutoRefresh();
    // 清理定时器
    if (this.uptimeTimer) {
      clearInterval(this.uptimeTimer);
    }
  },
  computed: {
    queueStatusClass() {
      if (this.queueStatus.runningTasks > 0) {
        return 'status-running'
      } else if (this.queueStatus.queueSize > 0) {
        return 'status-waiting'
      } else if (!this.queueStatus.processorThreadAlive) {
        return 'status-error'
      } else if (!this.queueActive) {
        return 'status-inactive'
      } else {
        return 'status-active'
      }
    },
    schedulerStatusClass() {
      return {
        'status-active': this.schedulerActive,
        'status-inactive': !this.schedulerActive
      }
    },
    queueStatusTooltip() {
      if (this.queueStatus.runningTasks > 0) {
        return `检查队列：正在执行${this.queueStatus.runningTasks}个任务（点击查看详情）`
      } else if (this.queueStatus.queueSize > 0) {
        return `检查队列：${this.queueStatus.queueSize}个任务等待执行（点击查看详情）`
      } else if (!this.queueStatus.processorThreadAlive) {
        return '检查队列：处理线程异常（点击查看详情）'
      } else if (!this.queueActive) {
        return '检查队列：已暂停（点击查看详情）'
      } else {
        return '检查队列：空闲（点击查看详情）'
      }
    },
    systemStatusClass() {
      return {
        'status-active': this.systemActive,
        'status-inactive': !this.systemActive
      }
    },
    systemStatusText() {
      return this.systemActive ? '运行中' : '已暂停'
    },
    queueProcessorStatusClass() {
      return this.queueStats.processorThreadAlive ? 'active' : 'inactive';
    },
    queueProcessorStatusText() {
      return this.queueStats.processorThreadAlive ? '运行中' : '已停止';
    }
  },
  methods: {
    // 添加日期时间格式化方法
    formatDateTime(dateStr) {
      if (!dateStr || dateStr === '未执行') return '未执行';
      try {
        // 创建日期对象
        const date = new Date(dateStr);
        // 检查日期是否有效
        if (isNaN(date.getTime())) return dateStr;
        
        // 格式化为 yyyy-MM-dd HH:mm:ss
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        const seconds = String(date.getSeconds()).padStart(2, '0');
        
        return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
      } catch (error) {
        // 如果格式化失败，返回原始字符串
        return dateStr;
      }
    },
    
    // 格式化持续时间（毫秒转为可读格式）
    formatDuration(ms) {
      if (!ms) return '0秒';
      
      // 将毫秒转换为秒
      let seconds = Math.floor(ms / 1000);
      
      // 计算天数、小时、分钟和剩余秒数
      const days = Math.floor(seconds / 86400);
      seconds %= 86400;
      const hours = Math.floor(seconds / 3600);
      seconds %= 3600;
      const minutes = Math.floor(seconds / 60);
      seconds %= 60;
      
      // 构建输出字符串
      let result = '';
      if (days > 0) result += `${days}天 `;
      if (hours > 0) result += `${hours}小时 `;
      if (minutes > 0) result += `${minutes}分钟 `;
      if (seconds > 0 || result === '') result += `${seconds}秒`;
      
      return result.trim();
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
        '运行中': 'processing',
        '修复中': 'processing',
        '等待中': 'warning',
        '等待修复': 'warning',
        '已完成': 'success',
        '失败': 'error',
        '取消': 'default'
      };
      return statusMap[status] || 'default';
    },
    
    // 启动自动刷新
    startAutoRefresh() {
      this.isAutoRefresh = true;
      this.refreshInterval = setInterval(() => {
        this.fetchFullStatus();
        this.updateQueueProcessorUptime();
      }, 5000); // 每5秒更新一次
    },
    
    // 停止自动刷新
    stopAutoRefresh() {
      if (this.refreshInterval) {
        clearInterval(this.refreshInterval);
        this.refreshInterval = null;
      }
    },
    
    // 切换自动刷新状态
    toggleAutoRefresh() {
      this.isAutoRefresh = !this.isAutoRefresh;
      if (this.isAutoRefresh) {
        this.startAutoRefresh();
        message.success('自动刷新已启用');
      } else {
        message.success('自动刷新已停用');
      }
    },
    
    // 获取完整队列系统状态
    async fetchFullStatus(showLoading = true) {
      try {
        if (showLoading) {
          // 显示加载状态
          message.loading({ content: '正在加载数据...', duration: 0, key: 'statusLoading' });
        }
        
        const response = await this.$axiosGet(global.API.queueSystemDetails);
        
        if (response && response.code === 200 && response.data) {
          // 更新状态数据
          if (response.data.queueManager) {
            this.queueStats = response.data.queueManager;
            this.queueActive = this.queueStats.running;
          }
          
          if (response.data.asyncService) {
            this.schedulerStats = response.data.asyncService;
            this.schedulerActive = this.schedulerStats.scheduledTasksEnabled;
          }
          
          // 更新任务列表
          if (response.data.queueTasks) {
            this.queueTasks = response.data.queueTasks;
          }
          
          if (response.data.fixQueueTasks) {
            this.fixQueueTasks = response.data.fixQueueTasks;
          }
          
          // 更新系统状态
          this.systemActive = (this.queueStats && this.queueStats.running) || false;
          this.systemUptime = this.queueStats ? this.queueStats.uptime || 0 : 0;
          
          // 更新定时任务状态
          if (this.schedulerStats) {
            this.scheduledTasks.forEach(task => {
              switch(task.id) {
                case 'taskCleanup':
                  task.active = this.schedulerStats.taskCleanupActive || false;
                  task.lastRun = this.schedulerStats.lastTaskCleanupTime;
                  task.interval = this.schedulerStats.taskCleanupInterval || '1小时';
                  task.newInterval = this.getIntervalSeconds(this.schedulerStats.taskCleanupInterval);
                  break;
                case 'connectionCleanup':
                  task.active = this.schedulerStats.connectionCleanupActive || false;
                  task.lastRun = this.schedulerStats.lastConnectionCleanupTime;
                  task.interval = this.schedulerStats.connectionCleanupInterval || '10分钟';
                  task.newInterval = this.getIntervalSeconds(this.schedulerStats.connectionCleanupInterval);
                  break;
                case 'queueHealthMonitor':
                  task.active = this.queueStats ? this.queueStats.queueHealthMonitorActive || false : false;
                  task.interval = this.queueStats ? this.queueStats.queueHealthMonitorInterval || '2分钟' : '2分钟';
                  task.newInterval = this.getIntervalSeconds(this.queueStats ? this.queueStats.queueHealthMonitorInterval : null);
                  break;
                case 'taskTimeoutMonitor':
                  task.active = this.queueStats ? this.queueStats.taskTimeoutMonitorActive || false : false;
                  task.interval = this.queueStats ? this.queueStats.taskTimeoutMonitorInterval || '30秒' : '30秒';
                  task.newInterval = this.getIntervalSeconds(this.queueStats ? this.queueStats.taskTimeoutMonitorInterval : null);
                  break;
              }
            });
          }
          
          // 更新处理线程运行时间
          if (response.data.processorStartTime) {
            this.queueProcessorStartTime = new Date(response.data.processorStartTime).getTime();
            this.updateQueueProcessorUptime();
          }
        }
        
        if (showLoading) {
          // 关闭加载提示
          message.success({ content: '数据加载完成', duration: 1, key: 'statusLoading' });
        }
      } catch (error) {
        console.error('获取队列系统状态失败:', error);
        if (showLoading) {
          message.error({ content: '获取数据失败', key: 'statusLoading' });
        }
      }
    },
    
    // 从文本间隔解析秒数
    getIntervalSeconds(intervalText) {
      if (!intervalText) return 60;
      
      try {
        if (typeof intervalText === 'string') {
          if (intervalText.includes('小时')) {
            return parseInt(intervalText) * 3600;
          } else if (intervalText.includes('分钟')) {
            return parseInt(intervalText) * 60;
          } else if (intervalText.includes('秒')) {
            return parseInt(intervalText);
          }
        }
      } catch (e) {
        return 60;
      }
      
      return 60;
    },
    
    async toggleTask(record) {
      record.loading = true;
      
      try {
        const action = record.active ? 'pauseTask' : 'resumeTask';
        const response = await this.$axiosGet(global.API.queueManager, {
          action: action,
          taskId: record.id
        });
        
        if (response && response.code === 200) {
          message.success(`任务已${record.active ? '停止' : '启动'}`);
          record.active = !record.active;
          this.fetchFullStatus();
        } else {
          message.error(`操作失败: ${response?.msg || '未知错误'}`);
        }
      } catch (error) {
        console.error('切换任务状态异常:', error);
        message.error('操作异常，请查看控制台日志');
      } finally {
        record.loading = false;
      }
    },
    
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
          this.fetchFullStatus();
        } else {
          message.error(`取消任务失败: ${response?.msg || '未知错误'}`);
        }
      } catch (error) {
        console.error('取消任务异常:', error);
        message.error('操作异常，请查看控制台日志');
      }
    },
    
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
          this.fetchFullStatus();
        } else {
          message.error(`取消修复任务失败: ${response?.msg || '未知错误'}`);
        }
      } catch (error) {
        console.error('取消修复任务异常:', error);
        message.error('操作异常，请查看控制台日志');
      }
    },
    
    async toggleQueue() {
      this.queueLoading = true
      try {
        const action = this.queueActive ? 'pause' : 'resume'
        const response = await this.$axiosGet(global.API.queueManager + `?action=${action}&scope=queue`, {})
        
        if (response && response.code === 200) {
          this.queueActive = !this.queueActive
          message.success(`队列处理已${this.queueActive ? '启用' : '暂停'}`)
          
          setTimeout(() => this.fetchFullStatus(), 1000)
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
        const response = await this.$axiosGet(global.API.queueManager + `?action=${action}&scope=scheduler`, {})
        
        if (response && response.code === 200) {
          this.schedulerActive = !this.schedulerActive
          message.success(`定时任务已${this.schedulerActive ? '启用' : '暂停'}`)
          
          setTimeout(() => this.fetchFullStatus(), 1000)
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
          
          setTimeout(() => this.fetchFullStatus(), 1000)
        } else {
          message.error(`操作失败: ${response?.msg || '未知错误'}`)
        }
      } catch (error) {
        console.error('清理不活跃连接异常:', error)
        message.error('操作异常，请查看控制台日志')
      } finally {
        this.cleanupLoading = false
      }
    },
    getMinInterval(taskId) {
      const minIntervals = {
        taskCleanup: 60, // 最小1分钟
        connectionCleanup: 30, // 最小30秒
        queueHealthMonitor: 30, // 最小30秒
        taskTimeoutMonitor: 30 // 最小30秒
      }
      return minIntervals[taskId] || 30
    },
    
    async updateTaskInterval(record) {
      if (!record.newInterval || record.newInterval < this.getMinInterval(record.id)) {
        message.error(`执行间隔不能小于${this.getMinInterval(record.id)}秒`);
        return;
      }
      
      record.saving = true;
      
      try {
        let response;
        const intervalMs = record.newInterval * 1000; // 转换为毫秒
        
        switch (record.id) {
          case 'queueHealthMonitor':
            response = await this.$axiosPost(global.API.updateQueueHealthMonitorInterval, {
              intervalMs: intervalMs
            });
            break;
          case 'taskTimeoutMonitor':
            response = await this.$axiosPost(global.API.updateTaskTimeoutMonitorInterval, {
              intervalMs: intervalMs
            });
            break;
          default:
            response = await this.$axiosPost(global.API.updateTaskInterval, {
              taskId: record.id,
              intervalSeconds: record.newInterval
            });
        }
        
        if (response && response.code === 200) {
          message.success('执行间隔已更新');
          record.interval = this.formatInterval(record.newInterval);
          this.fetchFullStatus();
        } else {
          message.error(`更新失败: ${response?.msg || '未知错误'}`);
          // 恢复之前的值
          record.newInterval = this.getIntervalSeconds(record.interval);
        }
      } catch (error) {
        console.error('更新执行间隔异常:', error);
        message.error('操作异常，请查看控制台日志');
        // 恢复之前的值
        record.newInterval = this.getIntervalSeconds(record.interval);
      } finally {
        record.saving = false;
      }
    },
    
    formatInterval(seconds) {
      if (seconds >= 3600) {
        return `${Math.floor(seconds / 3600)}小时`
      } else if (seconds >= 60) {
        return `${Math.floor(seconds / 60)}分钟`
      } else {
        return `${seconds}秒`
      }
    },
    
    async resetTask(record) {
      try {
        const response = await this.$axiosGet(global.API.queueManager, {
          action: 'resetTask',
          taskId: record.id
        })
        
        if (response && response.code === 200) {
          message.success('任务已重置')
          this.fetchFullStatus()
        } else {
          message.error(`重置失败: ${response?.msg || '未知错误'}`)
        }
      } catch (error) {
        console.error('重置任务异常:', error)
        message.error('操作异常，请查看控制台日志')
      }
    },
    
    async toggleSystem() {
      this.systemLoading = true;
      try {
        const action = this.systemActive ? 'pause' : 'resume';
        const response = await this.$axiosGet(global.API.queueManager, {
          action: action,
          scope: 'all'
        });
        
        if (response && response.code === 200) {
          this.systemActive = !this.systemActive;
          this.lastOperationTime = new Date();
          this.lastOperator = '当前用户';
          message.success(`系统已${this.systemActive ? '恢复' : '暂停'}`);
          this.fetchFullStatus();
        } else {
          message.error(`操作失败: ${response?.msg || '未知错误'}`);
        }
      } catch (error) {
        console.error('切换系统状态异常:', error);
        message.error('操作异常，请查看控制台日志');
      } finally {
        this.systemLoading = false;
      }
    },
    
    async fetchStartTime() {
      try {
        const response = await this.$http.get('/api/queue/start-time');
        this.startTime = response.data.startTime;
        this.uptime = Date.now() - this.startTime;
      } catch (error) {
        console.error('获取启动时间失败:', error);
      }
    },
    startUptimeTimer() {
      this.uptimeTimer = setInterval(() => {
        if (this.startTime) {
          this.uptime = Date.now() - this.startTime;
        }
      }, 1000);
    },
    updateQueueProcessorUptime() {
      if (this.queueProcessorStartTime) {
        this.queueProcessorUptime = Date.now() - this.queueProcessorStartTime;
      }
    },
    async fetchCheckQueue() {
      this.checkQueueRefreshing = true;
      try {
        const response = await this.$http.get('/api/queue/check/tasks');
        this.checkQueueTasks = response.data;
      } catch (error) {
        console.error('获取检查任务队列失败:', error);
        message.error('获取检查任务队列失败');
      } finally {
        this.checkQueueRefreshing = false;
      }
    },
    async handleFixTask(task) {
      try {
        await this.$confirm({
          title: '确认修复',
          content: `确定要修复任务 "${task.taskName}" 吗？`,
          okText: '确定',
          cancelText: '取消',
          centered: true,
          width: 400,
          okType: 'primary',
          maskClosable: false,
          class: 'fix-confirm-dialog'
        });
        
        await this.$http.post(`/api/queue/fix/task/${task.id}`);
        message.success('修复任务已提交');
        this.fetchFixQueue();
      } catch (error) {
        if (error !== 'cancel') {
          console.error('提交修复任务失败:', error);
          message.error('提交修复任务失败');
        }
      }
    },
  }
}
</script>

<style lang="less" scoped>
.queue-status-compact {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
}

.status-light {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-right: 4px;
  box-shadow: 0 0 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.status-active {
  background-color: #52c41a;
  box-shadow: 0 0 12px rgba(82, 196, 26, 0.3);
}

.status-inactive {
  background-color: #d9d9d9;
}

.status-running {
  background-color: #1890ff;
  box-shadow: 0 0 12px rgba(24, 144, 255, 0.3);
  animation: pulse 1.5s infinite;
}

.status-waiting {
  background-color: #faad14;
  box-shadow: 0 0 12px rgba(250, 173, 20, 0.3);
}

.status-error {
  background-color: #f5222d;
  box-shadow: 0 0 12px rgba(245, 34, 45, 0.3);
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.8;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

.detail-container {
  position: relative;
  padding-bottom: 20px;
}

.detail-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #f6f8fc 0%, #f0f4f8 100%);
  margin-bottom: 24px;
  position: relative;

  h3 {
    margin: 0;
    font-size: 20px;
    font-weight: 600;
    color: #1a1a1a;
    position: relative;
    padding-left: 16px;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 4px;
      height: 20px;
      background: #1890ff;
      border-radius: 2px;
    }
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-right: 48px; // 为关闭按钮留出空间

    :deep(.ant-btn) {
      width: 36px;
      height: 36px;
      padding: 0;
      border-radius: 50%;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      transition: all 0.3s ease;
      background: #f0f0f0;
      border: 1px solid #d9d9d9;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
      }

      i {
        font-size: 16px;
        margin: 0;
      }

      &.ant-btn-dangerous,
      &[danger] {
        background: #ff4d4f;
        border-color: #ff4d4f;
        color: #ffffff;

        &:hover {
          background: #ff7875;
          border-color: #ff7875;
          color: #ffffff;
        }
      }

      &.ant-btn-primary {
        background: #e6f7ff;
        border-color: #91d5ff;
        color: #1890ff;

        &:hover {
          background: #bae7ff;
          border-color: #69c0ff;
          color: #1890ff;
        }
      }

      span + i, i + span {
        margin: 0;
      }
    }

    .button-label {
      margin-left: 8px;
      font-size: 13px;
    }

    // 分离按钮和标签样式
    .action-item {
      display: flex;
      align-items: center;
    }

    :deep(.ant-switch) {
      background: #f0f0f0;
      border: 1px solid #d9d9d9;
      height: 24px;
      min-width: 44px;

      &.ant-switch-checked {
        background: #1890ff;
        border-color: #1890ff;
      }

      .ant-switch-handle {
        width: 20px;
        height: 20px;
        top: 1px;
        left: 1px;
        background: #fff;
        border: 1px solid #d9d9d9;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
      }

      &.ant-switch-checked .ant-switch-handle {
        left: calc(100% - 21px);
        border-color: #fff;
      }
      
      // 增加文字颜色对比度
      .ant-switch-inner {
        color: #333; // 未选中状态文字颜色
      }
      
      &.ant-switch-checked .ant-switch-inner {
        color: #fff; // 选中状态文字颜色
        font-weight: 500; // 加粗
      }
    }
  }
}

:deep(.ant-modal-close) {
  top: 16px;
  right: 16px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #2f2f2f; // 改为深色背景
  color: #ffffff; // 改为白色图标
  transition: all 0.3s ease;
  z-index: 1000;

  &:hover {
    background: #000000; // 悬停时更深色
    color: #ffffff;
    transform: rotate(90deg);
  }

  .ant-modal-close-x {
    width: 32px;
    height: 32px;
    line-height: 32px;
    font-size: 16px;
  }
}

.detail-section {
  margin-top: 24px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
  background: linear-gradient(135deg, #ffffff 0%, #fafafa 100%);
}

.section-header {
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
  background: #ffffff;
  display: flex;
  justify-content: space-between;
  align-items: center;

  h4 {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: #262626;
  }
}

.detail-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  padding: 24px;
}

.stat-item {
  background: #ffffff;
  border-radius: 8px;
  padding: 20px;
  transition: all 0.3s ease;
  border: 1px solid #f0f0f0;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 2px;
    background: linear-gradient(90deg, #1890ff 0%, #69c0ff 100%);
    opacity: 0;
    transition: opacity 0.3s ease;
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);

    &::before {
      opacity: 1;
    }
  }

  .stat-label {
    font-size: 14px;
    color: #8c8c8c;
    margin-bottom: 12px;
  }

  .stat-value {
    font-size: 16px;
    color: #333;
    display: flex;
    align-items: center;
  }
}

.scheduled-tasks {
  padding: 20px;

  :deep(.ant-table) {
    background: transparent;
  }

  :deep(.ant-table-thead > tr > th) {
    background: #fafafa;
    font-weight: 600;
    padding: 12px 16px;
  }

  :deep(.ant-table-tbody > tr > td) {
    padding: 12px 16px;
  }

  :deep(.ant-table-tbody > tr:hover > td) {
    background: #f5f5f5;
  }
}

.interval-editor {
  display: flex;
  align-items: center;
  gap: 12px;

  :deep(.ant-input-number) {
    width: 100px;
    border-radius: 4px;

    &:hover, &:focus {
      border-color: #1890ff;
    }
  }

  .interval-unit {
    color: #8c8c8c;
    font-size: 14px;
  }

  :deep(.ant-btn) {
    border-radius: 4px;
    height: 32px;
    padding: 0 12px;
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }
}

:deep(.ant-modal-content) {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.ant-modal-header) {
  display: none;
}

:deep(.ant-modal-body) {
  padding: 0;
}

:deep(.ant-tabs-nav) {
  margin: 0;
  padding: 0 20px;
  background: #fafafa;
}

:deep(.ant-tabs-tab) {
  padding: 12px 0;
  margin: 0 20px 0 0;
  transition: all 0.3s ease;

  &:hover {
    color: #1890ff;
  }
}

:deep(.ant-tabs-tab-active) {
  .ant-tabs-tab-btn {
    color: #1890ff;
    font-weight: 600;
  }
}

:deep(.ant-tabs-ink-bar) {
  background: #1890ff;
  height: 3px;
  border-radius: 3px 3px 0 0;
}

:deep(.ant-alert) {
  margin-bottom: 16px;
  border-radius: 4px;
}

:deep(.ant-tag) {
  border-radius: 4px;
  padding: 4px 8px;
  font-size: 12px;
}

:deep(.ant-switch) {
  background-color: #f0f0f0;
  border: 1px solid #d9d9d9;
  height: 24px;
  min-width: 44px;

  &.ant-switch-checked {
    background: #1890ff;
    border-color: #1890ff;
  }

  .ant-switch-handle {
    width: 20px;
    height: 20px;
    top: 1px;
    left: 1px;
    background: #fff;
    border: 1px solid #d9d9d9;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  }

  &.ant-switch-checked .ant-switch-handle {
    left: calc(100% - 21px);
    border-color: #fff;
  }
  
  // 增加文字颜色对比度
  .ant-switch-inner {
    color: #333; // 未选中状态文字颜色
  }
  
  &.ant-switch-checked .ant-switch-inner {
    color: #fff; // 选中状态文字颜色
    font-weight: 500; // 加粗
  }
}

:deep(.ant-switch-handle) {
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

:deep(.ant-switch-checked .ant-switch-handle) {
  border-color: #1890ff;
  box-shadow: 0 2px 4px rgba(24, 144, 255, 0.2);
}

.text-success { color: #52c41a; }
.text-warning { color: #faad14; }
.text-danger { color: #ff4d4f; }
.text-info { color: #1890ff; }

:deep(.ant-btn-dangerous) {
  color: #ff4d4f;
  border-color: #ff4d4f;
  background: transparent;

  &:hover {
    color: #ff7875;
    border-color: #ff7875;
    background: rgba(255, 77, 79, 0.1);
  }

  &:active {
    color: #d9363e;
    border-color: #d9363e;
    background: rgba(255, 77, 79, 0.2);
  }
}

:deep(.ant-badge-status-dot) {
  width: 8px;
  height: 8px;
  box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.06);
}

:deep(.ant-popconfirm-buttons) {
  margin-top: 12px;
}

:deep(.ant-modal-close) {
  color: white;
}

:deep(.ant-modal-close:hover) {
  color: rgba(255, 255, 255, 0.85);
}

// 增强时间列的样式
.timestamp-column {
  min-width: 180px;
  white-space: nowrap;
  overflow: visible !important;
}

:deep(.ant-table-tbody > tr > td.timestamp-column) {
  white-space: nowrap !important;
  font-family: 'Consolas', 'Courier New', monospace;
  font-size: 13px;
  letter-spacing: -0.3px;
  overflow: visible !important;
  word-break: keep-all !important;
  text-overflow: unset !important;
}

:deep(.ant-table-wrapper) {
  table {
    table-layout: fixed !important;
  }
  
  .ant-table-content {
    overflow-x: auto !important;
  }
}

:deep(.ant-modal-body .ant-table-wrapper) {
  overflow-x: auto !important;
}

// 增加弹窗整体样式
:deep(.ant-modal) {
  top: 50px;
  
  .ant-modal-content {
    max-height: calc(100vh - 100px);
    display: flex;
    flex-direction: column;
  }
  
  .ant-modal-body {
    flex: 1;
    overflow: auto;
  }
}

// 增加滚动表格样式
:deep(.ant-table-scroll) {
  overflow-x: auto !important;
}

// 优化表格样式
:deep(.ant-table) {
  table {
    width: auto !important;
    min-width: 100%;
  }
  
  .ant-table-thead > tr > th {
    white-space: nowrap;
    background: #f5f7fa;
    font-weight: 500;
  }
  
  .ant-table-tbody > tr:hover > td {
    background-color: #f0f7ff;
  }
}

// 优化表格行高
:deep(.ant-table-tbody > tr > td) {
  height: 52px;
  padding: 8px 12px;
  vertical-align: middle;
}

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 8px;
  vertical-align: middle;
  
  &.active {
    background-color: #52c41a;
    box-shadow: 0 0 8px rgba(82, 196, 26, 0.4);
  }
  
  &.inactive {
    background-color: #ff4d4f;
    box-shadow: 0 0 8px rgba(255, 77, 79, 0.4);
  }
}

.success-text {
  color: #52c41a;
}

.danger-text {
  color: #ff4d4f;
}

.timestamp {
  font-family: 'Consolas', monospace;
  font-size: 13px;
  letter-spacing: 0.5px;
  white-space: nowrap;
}

:deep(.fix-confirm-dialog) {
  .ant-modal-content {
    padding: 24px;
  }
  
  .ant-modal-header {
    padding: 0;
    margin-bottom: 16px;
    border-bottom: none;
    
    .ant-modal-title {
      font-size: 16px;
      font-weight: 500;
      color: #262626;
    }
  }
  
  .ant-modal-body {
    padding: 0;
    font-size: 14px;
    color: #595959;
  }
  
  .ant-modal-footer {
    padding: 16px 0 0;
    border-top: none;
    
    .ant-btn {
      min-width: 80px;
    }
  }
}

.status-control {
  display: flex;
  align-items: center;
  gap: 16px;

  .status-light {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    transition: all 0.3s ease;
    cursor: pointer;

    &.status-active {
      background-color: #52c41a;
      box-shadow: 0 0 12px rgba(82, 196, 26, 0.3);
    }

    &.status-inactive {
      background-color: #d9d9d9;
      box-shadow: 0 0 12px rgba(0, 0, 0, 0.1);
    }
  }

  :deep(.ant-switch) {
    background: #f0f0f0;
    border: 1px solid #d9d9d9;
    height: 24px;
    min-width: 44px;

    &.ant-switch-checked {
      background: #1890ff;
      border-color: #1890ff;
    }

    .ant-switch-handle {
      width: 20px;
      height: 20px;
      top: 1px;
      left: 1px;
      background: #fff;
      border: 1px solid #d9d9d9;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    }

    &.ant-switch-checked .ant-switch-handle {
      left: calc(100% - 21px);
      border-color: #fff;
    }
  }
}

.sub-section {
  padding: 20px 24px;
  background: #ffffff;
  border-top: 1px solid #f0f0f0;

  .sub-header {
    margin-bottom: 16px;
    
    h5 {
      margin: 0;
      font-size: 14px;
      font-weight: 500;
      color: #595959;
      position: relative;
      padding-left: 12px;

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 50%;
        transform: translateY(-50%);
        width: 3px;
        height: 14px;
        background: #1890ff;
        border-radius: 2px;
      }
    }
  }

  .detail-stats {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;
    padding: 0;

    .stat-item {
      background: #fafafa;
      border-radius: 6px;
      padding: 16px;
      transition: all 0.3s ease;
      border: 1px solid #f0f0f0;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
        border-color: #e6f7ff;
      }

      .stat-label {
        font-size: 13px;
        color: #8c8c8c;
        margin-bottom: 8px;
      }

      .stat-value {
        font-size: 15px;
        color: #262626;
        font-weight: 500;
      }
    }
  }
}
</style> 