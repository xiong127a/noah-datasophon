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
                  class="custom-switch"
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
                class="custom-switch"
              />
            </div>
          </div>
          
          <div class="detail-stats">
            <div class="stat-item">
              <div class="stat-label">系统运行时间</div>
              <div class="stat-value system-uptime">
                <a-icon type="hourglass" class="time-icon pulse" />
                <span class="uptime-text">{{ queueStats.systemUptime || '未知' }}</span>
              </div>
            </div>
            <div class="stat-item">
              <div class="stat-label">活跃线程</div>
              <div class="stat-value">
                <a-icon type="deployment-unit" theme="filled" class="stat-icon thread-icon" />
                {{ queueStats.totalActiveThreads || 0 }}
              </div>
            </div>
            <div class="stat-item">
              <div class="stat-label">总线程数</div>
              <div class="stat-value">
                <a-icon type="apartment" class="stat-icon" />
                {{ queueStats.totalPoolSize || 0 }}
              </div>
            </div>
            <div class="stat-item">
              <div class="stat-label">完成任务</div>
              <div class="stat-value">
                <a-icon type="check-circle" theme="filled" class="stat-icon task-icon" />
                {{ queueStats.totalCompletedTasks || 0 }}
              </div>
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
                  <div class="queue-runtime">
                    <span class="runtime-label">运行时间:</span>
                    <span class="runtime-value">{{ formatQueueTime(queueStats.queueProcessorStartTime) }}</span>
                  </div>
                  <a-popover
                    placement="topRight"
                    trigger="hover"
                    :visible="showQueuePopover"
                    @visibleChange="handleQueuePopoverVisibleChange"
                    overlayClassName="queue-detail-popover"
                  >
                    <template slot="content">
                      <div class="queue-popover-content">
                        <h3>检查任务队列详情</h3>
                        <a-table
                          :columns="queueTaskColumns"
                          :dataSource="queueTasks"
                          size="small"
                          :scroll="{ y: 300 }"
                          :pagination="{ pageSize: 5, size: 'small' }"
                          :rowKey="record => record.taskKey"
                        >
                          <template slot="status" slot-scope="text">
                            <a-tag :color="getStatusColor(text)" style="color: rgba(0, 0, 0, 0.85); font-weight: 500;">{{ text }}</a-tag>
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
                      </div>
                    </template>
                    <div 
                      class="status-light detail-status-light enhanced-light" 
                      :class="getQueueStatusClass(queueStats)"
                      @mouseenter="fetchQueueTasksOnHover"
                    />
                  </a-popover>
                </div>
              </div>
              
              <div class="detail-stats with-padding">
                <div class="stat-item">
                  <div class="stat-label">等待任务</div>
                  <div class="stat-value queue-stat">
                    <a-icon type="hourglass" class="queue-icon waiting-icon" />
                    <span>{{ queueStats.queueSize || 0 }}</span>
                  </div>
                </div>
                <div class="stat-item">
                  <div class="stat-label">正在执行</div>
                  <div class="stat-value queue-stat">
                    <a-icon type="loading" spin class="queue-icon running-icon" />
                    <span>{{ queueStats.runningTasks || 0 }}</span>
                  </div>
                </div>
                <div class="stat-item">
                  <div class="stat-label">已完成任务</div>
                  <div class="stat-value queue-stat">
                    <a-icon type="check-circle" theme="filled" class="queue-icon completed-icon" />
                    <span>{{ queueStats.completedTasks || 0 }}</span>
                  </div>
                </div>
                <div class="stat-item">
                  <div class="stat-label">队列容量</div>
                  <div class="stat-value queue-stat">
                    <a-icon type="database" class="queue-icon capacity-icon" />
                    <span>{{ queueStats.queueCapacity || 100 }}</span>
                  </div>
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
                    <div class="stat-value thread-stat">
                      <a-icon type="thunderbolt" theme="filled" class="thread-icon active-icon" />
                      <span>{{ queueStats.totalActiveThreads || 0 }}</span>
                    </div>
                  </div>
                  <div class="stat-item">
                    <div class="stat-label">总线程池大小</div>
                    <div class="stat-value thread-stat">
                      <a-icon type="cluster" class="thread-icon pool-icon" />
                      <span>{{ queueStats.totalPoolSize || 0 }}</span>
                    </div>
                  </div>
                  <div class="stat-item">
                    <div class="stat-label">总完成任务</div>
                    <div class="stat-value thread-stat">
                      <a-icon type="file-done" class="thread-icon complete-icon" />
                      <span>{{ queueStats.totalCompletedTasks || 0 }}</span>
                    </div>
                  </div>
                  <div class="stat-item">
                    <div class="stat-label">总队列任务</div>
                    <div class="stat-value thread-stat">
                      <a-icon type="ordered-list" class="thread-icon queue-icon" />
                      <span>{{ queueStats.totalQueuedTasks || 0 }}</span>
                    </div>
                  </div>
                </div>
              </div>
              
              <!-- 新增：检查任务统计 -->
              <div class="sub-section">
                <div class="sub-header with-padding">
                  <h5>检查任务统计</h5>
                </div>
                
                <div class="detail-stats with-padding">
                  <div class="stat-item">
                    <div class="stat-label">检查成功</div>
                    <div class="stat-value task-stat success-stat">
                      <a-icon type="check-square" theme="filled" class="task-icon" />
                      <span>{{ queueStats.tasksSucceeded || 0 }}</span>
                    </div>
                  </div>
                  <div class="stat-item">
                    <div class="stat-label">检查失败</div>
                    <div class="stat-value task-stat failed-stat">
                      <a-icon type="close-square" theme="filled" class="task-icon" />
                      <span>{{ queueStats.tasksFailed || 0 }}</span>
                    </div>
                  </div>
                  <div class="stat-item">
                    <div class="stat-label">平均执行时间</div>
                    <div class="stat-value timing-display">
                      <a-icon type="clock-circle" class="timing-icon avg-time-icon" />
                      <span class="timing-text">{{ queueStats.tasksAvgExecutionTime || '0秒' }}</span>
                    </div>
                  </div>
                  <div class="stat-item">
                    <div class="stat-label">最长执行时间</div>
                    <div class="stat-value timing-display">
                      <a-icon type="dashboard" class="timing-icon max-time-icon" />
                      <span class="timing-text">{{ queueStats.tasksMaxExecutionTime || '0秒' }}</span>
                    </div>
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
                  <div class="queue-runtime">
                    <span class="runtime-label">运行时间:</span>
                    <span class="runtime-value animated-time">
                      <a-icon type="clock-circle" class="runtime-icon" />
                      {{ formatQueueTime(queueStats.fixQueueProcessorStartTime) }}
                    </span>
                  </div>
                  <a-popover
                    placement="topRight"
                    trigger="hover"
                    :visible="showFixQueuePopover"
                    @visibleChange="handleFixQueuePopoverVisibleChange"
                    overlayClassName="queue-detail-popover"
                  >
                    <template slot="content">
                      <div class="queue-popover-content">
                        <h3>修复任务队列详情</h3>
                        <a-table
                          :columns="fixTaskColumns"
                          :dataSource="fixQueueTasks"
                          size="small"
                          :scroll="{ y: 300 }"
                          :pagination="{ pageSize: 5, size: 'small' }"
                          :rowKey="record => record.taskKey"
                        >
                          <template slot="status" slot-scope="text">
                            <a-tag :color="getStatusColor(text)" style="color: rgba(0, 0, 0, 0.85); font-weight: 500;">{{ text }}</a-tag>
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
                      </div>
                    </template>
                    <div 
                      class="status-light detail-status-light" 
                      :class="getFixQueueStatusClass(queueStats)"
                      @mouseenter="fetchFixQueueTasksOnHover"
                    />
                  </a-popover>
                </div>
              </div>
              
              <div class="detail-stats with-padding">
                <div class="stat-item">
                  <div class="stat-label">等待修复任务</div>
                  <div class="stat-value queue-stat">
                    <a-icon type="hourglass" class="queue-icon waiting-icon" />
                    <span>{{ queueStats.fixQueueSize || 0 }}</span>
                  </div>
                </div>
                <div class="stat-item">
                  <div class="stat-label">正在执行修复</div>
                  <div class="stat-value queue-stat">
                    <a-icon type="loading" spin class="queue-icon running-icon" />
                    <span>{{ queueStats.runningFixTasks || 0 }}</span>
                  </div>
                </div>
                <div class="stat-item">
                  <div class="stat-label">已完成修复</div>
                  <div class="stat-value queue-stat">
                    <a-icon type="check-circle" theme="filled" class="queue-icon completed-icon" />
                    <span>{{ queueStats.fixTasksProcessed || 0 }}</span>
                  </div>
                </div>
                <div class="stat-item">
                  <div class="stat-label">修复成功率</div>
                  <div class="stat-value success-rate">
                    <a-icon type="pie-chart" theme="filled" class="rate-icon animated-icon" />
                    <span class="rate-text gradient-text">{{ calculateSuccessRate(queueStats.fixTasksSucceeded, queueStats.fixTasksFailed) }}%</span>
                  </div>
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
                    <div class="stat-value thread-stat">
                      <a-icon type="thunderbolt" theme="filled" class="thread-icon active-icon" />
                      <span>{{ queueStats.fixExecutorActiveCount || 0 }}</span>
                    </div>
                  </div>
                  <div class="stat-item">
                    <div class="stat-label">总线程池大小</div>
                    <div class="stat-value thread-stat">
                      <a-icon type="cluster" class="thread-icon pool-icon" />
                      <span>{{ queueStats.totalPoolSize || 0 }}</span>
                    </div>
                  </div>
                  <div class="stat-item">
                    <div class="stat-label">总完成任务</div>
                    <div class="stat-value thread-stat">
                      <a-icon type="file-done" class="thread-icon complete-icon" />
                      <span>{{ queueStats.fixTasksProcessed || 0 }}</span>
                    </div>
                  </div>
                  <div class="stat-item">
                    <div class="stat-label">总队列任务</div>
                    <div class="stat-value thread-stat">
                      <a-icon type="ordered-list" class="thread-icon queue-icon" />
                      <span>{{ queueStats.fixExecutorQueueSize || 0 }}</span>
                    </div>
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
                    <div class="stat-value task-stat success-stat">
                      <a-icon type="check-square" theme="filled" class="task-icon" />
                      <span>{{ queueStats.fixTasksSucceeded || 0 }}</span>
                    </div>
                  </div>
                  <div class="stat-item">
                    <div class="stat-label">修复失败</div>
                    <div class="stat-value task-stat failed-stat">
                      <a-icon type="close-square" theme="filled" class="task-icon" />
                      <span>{{ queueStats.fixTasksFailed || 0 }}</span>
                    </div>
                  </div>
                  <div class="stat-item">
                    <div class="stat-label">平均执行时间</div>
                    <div class="stat-value timing-display">
                      <a-icon type="clock-circle" class="timing-icon avg-time-icon" />
                      <span class="timing-text">{{ queueStats.fixTasksAvgExecutionTime || '0秒' }}</span>
                    </div>
                  </div>
                  <div class="stat-item">
                    <div class="stat-label">最长执行时间</div>
                    <div class="stat-value timing-display">
                      <a-icon type="dashboard" class="timing-icon max-time-icon" />
                      <span class="timing-text">{{ queueStats.fixTasksMaxExecutionTime || '0秒' }}</span>
                    </div>
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
              <a-button 
                class="cleanup-connection-btn" 
                @click="cleanupConnections" 
                :loading="cleanupLoading"
              >
                <a-icon type="delete" theme="filled" />
                <span class="button-text">清理不活跃连接</span>
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
              <div class="connection-status-badge" :class="schedulerStats.connectionCleanupActive ? 'status-active' : 'status-inactive'">
                <span class="status-text">{{ schedulerStats.connectionCleanupActive ? '活跃' : '已停止' }}</span>
                <span class="status-ripple"></span>
              </div>
            </div>
            <div class="stat-item">
              <div class="stat-label">上次清理时间</div>
              <div class="stat-value cleanup-time">
                <span class="cleanup-text">{{ formatDateTime(schedulerStats.lastConnectionCleanupTime) }}</span>
              </div>
            </div>
            <div class="stat-item">
              <div class="stat-label">SSH会话缓存</div>
              <a-tooltip title="SSH会话缓存命中率，表示复用已有连接的比例。若显示为0%可能是因为连接复用功能尚未激活。">
                <div class="stat-value ssh-cache-stat">
                  <div class="cache-container">
                    <div class="cache-level" :class="getCacheLevelClass(schedulerStats.sessionCacheHitRate)">
                      <span class="cache-value">{{ schedulerStats.sessionCacheHitRate || 0 }}%</span>
                      <span class="cache-label">{{ getCacheLabel(schedulerStats.sessionCacheHitRate) }}</span>
                    </div>
                  </div>
                </div>
              </a-tooltip>
            </div>
          </div>
        </div>

        <!-- 定时任务管理 -->
        <div class="detail-section">
          <div class="section-header">
            <h4>定时任务管理</h4>
            <div class="status-control">
              <div class="status-item">
                <a-tooltip :title="schedulerActive ? '所有定时任务已激活' : '所有定时任务已暂停'">
                  <div class="status-light" :class="schedulerStatusClass" />
                </a-tooltip>
              </div>
              <a-tooltip :title="schedulerActive ? '暂停所有定时任务' : '启动所有定时任务'">
                <a-button
                  :type="schedulerActive ? 'danger' : 'primary'"
                  shape="round"
                  size="default"
                  :loading="schedulerLoading"
                  @click="toggleScheduler"
                  class="scheduler-toggle-button"
                  :class="{'active-button': schedulerActive, 'inactive-button': !schedulerActive}"
                >
                  <a-icon :type="schedulerActive ? 'pause-circle' : 'play-circle'" theme="filled" />
                  <span class="button-text">{{ schedulerActive ? '暂停全部' : '启动全部' }}</span>
                </a-button>
              </a-tooltip>
            </div>
          </div>
          
          <div class="scheduled-tasks">
            <a-alert
              type="info"
              show-icon
              style="margin-bottom: 10px;"
            >
              <template slot="message">
                您可以调整定时任务执行间隔，输入秒数后点击保存按钮。系统支持的最小间隔为1秒，建议生产环境使用更长间隔。
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
                    :max="24 * 60 * 60"
                    :disabled="saving || record.saving"
                    style="width: 100px;"
                    addon-after="秒"
                  />
                  <a-button 
                    type="primary" 
                    size="small" 
                    @click="updateTaskInterval(record)"
                    :loading="record.saving"
                    style="margin-left: 5px"
                  >
                    保存
                  </a-button>
                  <a-tooltip title="重置为默认值（任务清理和连接清理为60秒，队列健康监控为60秒，任务超时监控为30秒）">
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
                    :checked="record.active"
                    :loading="record.loading"
                    @click="toggleTask(record)"
                    class="custom-switch"
                  />
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
              <div class="connection-status-badge" :class="schedulerStats.taskCleanupActive ? 'status-active' : 'status-inactive'">
                <span class="status-text">{{ schedulerStats.taskCleanupActive ? '活跃' : '已停止' }}</span>
                <span class="status-ripple"></span>
              </div>
            </div>
            <div class="stat-item">
              <div class="stat-label">上次任务清理</div>
              <div class="stat-value cleanup-time">
                <span class="cleanup-text">{{ formatDateTime(schedulerStats.lastTaskCleanupTime) }}</span>
              </div>
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
  filters: {
    // 格式化时间为可读格式
    formatTime(milliseconds) {
      if (!milliseconds) return '未知';
      
      const seconds = Math.floor(milliseconds / 1000);
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
      showQueuePopover: false,
      showFixQueuePopover: false,
      cleanupLoading: false,
      queueTasks: [],
      fixQueueTasks: [],
      // 添加请求锁，防止请求堆积
      isRequestPending: false,
      scheduledTasks: [
        {
          id: 'taskCleanup',
          name: '任务清理',
          description: '自动清理已完成的任务记录',
          active: false,
          lastRun: '未执行',
          interval: '1小时',
          newInterval: 3600,
          saving: false
        },
        {
          id: 'connectionCleanup',
          name: '连接清理',
          description: '自动清理无效的连接记录',
          active: false,
          lastRun: '未执行',
          interval: '10分钟',
          newInterval: 600,
          saving: false
        },
        {
          id: 'queueHealthMonitor',
          name: '队列健康监控',
          description: '监控队列健康状态并自动处理异常',
          active: false,
          lastRun: '未执行',
          interval: '2分钟',
          newInterval: 120,
          saving: false
        },
        {
          id: 'taskTimeoutMonitor',
          name: '任务超时监控',
          description: '监控任务执行时间并处理超时任务',
          active: false,
          lastRun: '未执行',
          interval: '30秒',
          newInterval: 30,
          saving: false
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
          width: '20%'
        },
        {
          title: '状态',
          dataIndex: 'status',
          key: 'status',
          width: '15%',
          scopedSlots: { customRender: 'status' }
        },
        {
          title: '开始时间',
          dataIndex: 'startTime',
          key: 'startTime',
          width: '30%',
          className: 'timestamp-column',
          scopedSlots: { customRender: 'startTime' }
        },
        {
          title: '执行时长',
          dataIndex: 'duration',
          key: 'duration',
          width: '20%',
          scopedSlots: { customRender: 'duration' }
        },
        {
          title: '优先级',
          dataIndex: 'priority',
          key: 'priority',
          width: '15%'
        }
      ],
      fixTaskColumns: [
        {
          title: '主机',
          dataIndex: 'hostname',
          key: 'hostname',
          width: '20%'
        },
        {
          title: '状态',
          dataIndex: 'status',
          key: 'status',
          width: '15%',
          scopedSlots: { customRender: 'status' }
        },
        {
          title: '开始时间',
          dataIndex: 'startTime',
          key: 'startTime',
          width: '30%',
          className: 'timestamp-column',
          scopedSlots: { customRender: 'startTime' }
        },
        {
          title: '执行时长',
          dataIndex: 'duration',
          key: 'duration',
          width: '20%',
          scopedSlots: { customRender: 'duration' }
        },
        {
          title: '结果',
          dataIndex: 'result',
          key: 'result',
          width: '15%'
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
      saving: false,
    }
  },
  created() {
    // 获取应用启动时间
    this.fetchStartTime();
    // 启动定时更新运行时间
    this.startUptimeTimer();
    // 初始化加载数据
    this.fetchFullStatusOnce();
    // 默认开启自动刷新
    this.startAutoRefresh();
  },
  beforeDestroy() {
    // 组件销毁前清除定时器
    this.stopAutoRefresh();
    // 清理定时器
    if (this.uptimeTimer) {
      clearInterval(this.uptimeTimer);
    }
  },
  watch: {
    // 监听任务状态变化，自动同步总状态
    scheduledTasks: {
      handler: 'syncGlobalTaskStatus',
      deep: true
    },
    // 监听详情弹窗状态
    showDetailModal(newVal) {
      if (newVal) {
        // 详情弹窗打开时，立即获取一次数据
        this.fetchFullStatusOnce();
        // 如果设置了自动刷新，确保开启
        if (this.isAutoRefresh && !this.refreshInterval) {
          this.startAutoRefresh();
        }
      } else {
        // 详情弹窗关闭时，如果没有其他需要自动刷新的界面，可以停止刷新
        // 这里可以根据需要决定是否在关闭弹窗时停止刷新
      }
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
        return dateStr;
      }
    },
    
    // 关闭整个队列系统
    async shutdownSystem() {
      this.systemLoading = true;
      try {
        // 调用关闭系统API
        const response = await this.$axiosGet(global.API.queueManager, {
          action: 'shutdown'
        });
        
        if (response && response.code === 200) {
          this.systemActive = false;
          this.queueActive = false;
          this.schedulerActive = false;
          message.success('系统已成功关闭');
          this.lastOperationTime = new Date();
          this.lastOperator = '当前用户';
          
          // 刷新状态
          this.fetchFullStatus();
        } else {
          message.error(`系统关闭失败: ${response?.msg || '未知错误'}`);
        }
      } catch (error) {
        console.error('关闭系统异常:', error);
        message.error('系统关闭操作异常，请查看控制台日志');
      } finally {
        this.systemLoading = false;
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
      // 使用具体的颜色值而不是预设的颜色名称，以确保更好的可见性
      const statusMap = {
        '运行中': '#1890ff',     // 明亮的蓝色
        '修复中': '#1890ff',     // 明亮的蓝色
        '等待中': '#faad14',     // 明亮的黄色
        '等待修复': '#faad14',   // 明亮的黄色
        '已完成': '#52c41a',     // 明亮的绿色
        '失败': '#f5222d',       // 明亮的红色
        '取消': '#d9d9d9'        // 灰色
      };
      return statusMap[status] || '#1890ff'; // 默认使用明亮的蓝色
    },
    
    // 启动自动刷新
    startAutoRefresh() {
      if (this.isAutoRefresh && this.refreshInterval) {
        // 如果已经在自动刷新中，不需要再次启动
        console.log('自动刷新已经在运行中，无需重复启动');
        return;
      }
      
      console.log('启动自动刷新');
      this.isAutoRefresh = true;
      // 先清除可能存在的定时器
      if (this.refreshInterval) {
        clearTimeout(this.refreshInterval);
        this.refreshInterval = null;
      }
      // 使用递归的方式，确保上一个请求完成后再发起下一个
      this.scheduleNextRefresh();
    },
    
    // 安排下一次刷新
    scheduleNextRefresh() {
      if (!this.isAutoRefresh) return;
      
      console.log('安排下一次刷新，1秒后执行');
      this.refreshInterval = setTimeout(async () => {
        // 先执行数据获取
        await this.fetchFullStatusOnce();
        this.updateQueueProcessorUptime();
        
        // 然后安排下一次刷新
        if (this.isAutoRefresh) {
          this.scheduleNextRefresh();
        }
      }, 1000); // 间隔1秒
    },
    
    // 停止自动刷新
    stopAutoRefresh() {
      if (this.refreshInterval) {
        console.log('停止自动刷新');
        clearTimeout(this.refreshInterval);
        this.refreshInterval = null;
      }
      this.isAutoRefresh = false;
    },
    
    // 单次获取数据，确保不会重复请求
    async fetchFullStatusOnce() {
      // 如果已经有一个请求在进行中，则跳过此次请求
      if (this.isRequestPending) {
        console.log('跳过请求：上一个请求尚未完成');
        return;
      }
      
      try {
        console.log('开始请求队列状态');
        this.isRequestPending = true;
        await this.fetchFullStatus(false);
        console.log('队列状态请求完成');
      } finally {
        this.isRequestPending = false;
      }
    },
    
    // 切换自动刷新状态
    toggleAutoRefresh() {
      // 切换状态
      const newState = !this.isAutoRefresh;
      
      if (newState) {
        // 启用自动刷新
        this.startAutoRefresh();
        message.success('自动刷新已启用');
      } else {
        // 停用自动刷新
        this.stopAutoRefresh();
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
          // 调试信息
          console.log('接收到的后端数据结构:', {
            queueManager: response.data.queueManager,
            asyncService: response.data.asyncService,
            queueTasksCount: response.data.queueTasks ? response.data.queueTasks.length : 0,
            fixQueueTasksCount: response.data.fixQueueTasks ? response.data.fixQueueTasks.length : 0
          });
          
          if (response.data.queueManager) {
            console.log('队列管理器健康监控数据:', {
              monitorActive: response.data.queueManager.queueHealthMonitorActive,
              interval: response.data.queueManager.queueHealthMonitorInterval,
              intervalMs: response.data.queueManager.queueHealthMonitorIntervalMs,
              lastTime: response.data.queueManager.lastQueueHealthMonitorTime
            });
          }
          
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
                  
                  // 优先使用intervalMs
                  if (this.schedulerStats.taskCleanupIntervalMs) {
                    const seconds = Math.floor(this.schedulerStats.taskCleanupIntervalMs / 1000);
                    task.interval = this.formatInterval(seconds);
                    task.newInterval = seconds;
                    console.log('使用intervalMs设置任务清理间隔:', seconds, '秒');
                  }
                  // 其次使用interval
                  else if (this.schedulerStats.taskCleanupInterval) {
                    if (typeof this.schedulerStats.taskCleanupInterval === 'number') {
                      const seconds = Math.floor(this.schedulerStats.taskCleanupInterval / 1000);
                      task.interval = this.formatInterval(seconds);
                      task.newInterval = seconds;
                      console.log('使用interval数值设置任务清理间隔:', seconds, '秒');
                    } else {
                      task.interval = this.schedulerStats.taskCleanupInterval;
                      task.newInterval = this.getIntervalSeconds(this.schedulerStats.taskCleanupInterval);
                      console.log('使用interval字符串设置任务清理间隔:', task.newInterval, '秒');
                    }
                  } else {
                    task.interval = '1小时';
                    task.newInterval = 3600;
                    console.log('未找到间隔信息，使用默认值:', 3600, '秒');
                  }
                  break;
                case 'connectionCleanup':
                  task.active = this.schedulerStats.connectionCleanupActive || false;
                  task.lastRun = this.schedulerStats.lastConnectionCleanupTime;
                  
                  // 优先使用intervalMs
                  if (this.schedulerStats.connectionCleanupIntervalMs) {
                    const seconds = Math.floor(this.schedulerStats.connectionCleanupIntervalMs / 1000);
                    task.interval = this.formatInterval(seconds);
                    task.newInterval = seconds;
                    console.log('使用intervalMs设置连接清理间隔:', seconds, '秒');
                  }
                  // 其次使用interval
                  else if (this.schedulerStats.connectionCleanupInterval) {
                    if (typeof this.schedulerStats.connectionCleanupInterval === 'number') {
                      const seconds = Math.floor(this.schedulerStats.connectionCleanupInterval / 1000);
                      task.interval = this.formatInterval(seconds);
                      task.newInterval = seconds;
                      console.log('使用interval数值设置连接清理间隔:', seconds, '秒');
                    } else {
                      task.interval = this.schedulerStats.connectionCleanupInterval;
                      task.newInterval = this.getIntervalSeconds(this.schedulerStats.connectionCleanupInterval);
                      console.log('使用interval字符串设置连接清理间隔:', task.newInterval, '秒');
                    }
                  } else {
                    task.interval = '10分钟';
                    task.newInterval = 600;
                    console.log('未找到间隔信息，使用默认值:', 600, '秒');
                  }
                  break;
                case 'queueHealthMonitor':
                  // 添加调试信息
                  console.log('队列健康监控数据:', {
                    active: this.queueStats?.queueHealthMonitorActive,
                    interval: this.queueStats?.queueHealthMonitorInterval,
                    intervalMs: this.queueStats?.queueHealthMonitorIntervalMs,
                    lastRunTime: this.queueStats?.lastQueueHealthMonitorTime
                  });
                  
                  task.active = this.queueStats ? this.queueStats.queueHealthMonitorActive || false : false;
                  
                  // 确保显示正确的间隔
                  if (this.queueStats) {
                    // 优先使用intervalMs (毫秒值)
                    if (this.queueStats.queueHealthMonitorIntervalMs) {
                      const seconds = Math.floor(this.queueStats.queueHealthMonitorIntervalMs / 1000);
                      task.interval = this.formatInterval(seconds);
                      task.newInterval = seconds;
                      console.log('使用intervalMs设置间隔:', seconds, '秒');
                    }
                    // 其次使用interval (带单位的字符串或秒数)
                    else if (this.queueStats.queueHealthMonitorInterval) {
                      if (typeof this.queueStats.queueHealthMonitorInterval === 'number') {
                        // 如果是数值，假设是毫秒
                        const seconds = Math.floor(this.queueStats.queueHealthMonitorInterval / 1000);
                        task.interval = this.formatInterval(seconds);
                        task.newInterval = seconds;
                        console.log('使用interval数值设置间隔:', seconds, '秒');
                      } else {
                        // 如果是字符串，解析并转换
                        task.interval = this.queueStats.queueHealthMonitorInterval;
                        task.newInterval = this.getIntervalSeconds(this.queueStats.queueHealthMonitorInterval);
                        console.log('使用interval字符串设置间隔:', task.newInterval, '秒');
                      }
                    } else {
                      task.interval = '60秒';
                      task.newInterval = 60;
                      console.log('未找到间隔信息，使用默认值:', 60, '秒');
                    }
                  } else {
                    task.interval = '60秒';
                    task.newInterval = 60;
                    console.log('未找到队列管理器信息，使用默认值:', 60, '秒');
                  }
                  
                  // 确保正确显示上次执行时间
                  if (this.queueStats && this.queueStats.lastQueueHealthMonitorTime) {
                    task.lastRun = this.queueStats.lastQueueHealthMonitorTime;
                    console.log('设置上次执行时间:', task.lastRun);
                  } else {
                    task.lastRun = '未执行';
                    console.log('未找到上次执行时间');
                  }
                  break;
                case 'taskTimeoutMonitor':
                  // 添加调试信息
                  console.log('任务超时监控数据:', {
                    active: this.queueStats?.taskTimeoutMonitorActive,
                    interval: this.queueStats?.taskTimeoutMonitorInterval,
                    intervalMs: this.queueStats?.taskTimeoutMonitorIntervalMs,
                    lastRunTime: this.queueStats?.lastTaskTimeoutMonitorTime
                  });
                  
                  task.active = this.queueStats ? this.queueStats.taskTimeoutMonitorActive || false : false;
                  
                  // 确保显示正确的间隔
                  if (this.queueStats) {
                    // 优先使用intervalMs (毫秒值)
                    if (this.queueStats.taskTimeoutMonitorIntervalMs) {
                      const seconds = Math.floor(this.queueStats.taskTimeoutMonitorIntervalMs / 1000);
                      task.interval = this.formatInterval(seconds);
                      task.newInterval = seconds;
                      console.log('使用intervalMs设置超时监控间隔:', seconds, '秒');
                    }
                    // 其次使用interval (带单位的字符串或秒数)
                    else if (this.queueStats.taskTimeoutMonitorInterval) {
                      if (typeof this.queueStats.taskTimeoutMonitorInterval === 'number') {
                        // 如果是数值，假设是毫秒
                        const seconds = Math.floor(this.queueStats.taskTimeoutMonitorInterval / 1000);
                        task.interval = this.formatInterval(seconds);
                        task.newInterval = seconds;
                        console.log('使用interval数值设置超时监控间隔:', seconds, '秒');
                      } else {
                        // 如果是字符串，解析并转换
                        task.interval = this.queueStats.taskTimeoutMonitorInterval;
                        task.newInterval = this.getIntervalSeconds(this.queueStats.taskTimeoutMonitorInterval);
                        console.log('使用interval字符串设置超时监控间隔:', task.newInterval, '秒');
                      }
                    } else {
                      task.interval = '30秒';
                      task.newInterval = 30;
                      console.log('未找到间隔信息，使用默认值:', 30, '秒');
                    }
                  } else {
                    task.interval = '30秒';
                    task.newInterval = 30;
                    console.log('未找到队列管理器信息，使用默认值:', 30, '秒');
                  }
                  
                  // 确保正确显示上次执行时间
                  if (this.queueStats && this.queueStats.lastTaskTimeoutMonitorTime) {
                    task.lastRun = this.queueStats.lastTaskTimeoutMonitorTime;
                    console.log('设置任务超时监控上次执行时间:', task.lastRun);
                  } else {
                    task.lastRun = '未执行';
                    console.log('未找到任务超时监控上次执行时间');
                  }
                  break;
              }
            });
            
            // 数据加载完成后同步全局状态
            this.$nextTick(() => {
              this.syncGlobalTaskStatus();
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
        
        // 确保modal显示时自动刷新正常工作
        if (this.showDetailModal && !this.refreshInterval && this.isAutoRefresh) {
          this.startAutoRefresh();
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
      
      // 如果是数字（毫秒值），直接转换为秒
      if (typeof intervalText === 'number') {
        return Math.floor(intervalText / 1000);
      }
      
      try {
        if (typeof intervalText === 'string') {
          // 先尝试直接从字符串中提取数字
          const matches = intervalText.match(/(\d+)/);
          if (matches && matches[1]) {
            const value = parseInt(matches[1]);
            
            if (intervalText.includes('小时')) {
              return value * 3600;
            } else if (intervalText.includes('分钟')) {
              return value * 60;
            } else if (intervalText.includes('秒')) {
              return value;
            } else {
              // 如果没有单位，假设是秒
              return value;
            }
          }
        }
      } catch (e) {
        console.error('解析间隔错误:', e, intervalText);
        return 60;
      }
      
      return 60;
    },
    
    async toggleTask(record) {
      record.loading = true;
      
      try {
        const action = record.active ? 'pauseTask' : 'resumeTask';
        
        // 对不同的任务使用不同的范围参数
        let scope = 'ALL';
        if (record.id === 'queueHealthMonitor' || record.id === 'taskTimeoutMonitor') {
          scope = 'scheduler';  // 确保监控任务使用scheduler范围
        }
        
        const response = await this.$axiosGet(global.API.queueManager, {
          action: action,
          taskId: record.id,
          scope: scope
        });
        
        if (response && response.code === 200) {
          message.success(`任务已${record.active ? '停止' : '启动'}`);
          record.active = !record.active;
          
          // 任务状态改变后立即同步全局状态
          this.syncGlobalTaskStatus();
          
          // 稍后刷新所有状态
          setTimeout(() => this.fetchFullStatus(false), 500);
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
        let promises = []
        
        // 检查是否需要修正状态不一致
        const allTasksStopped = this.scheduledTasks.every(task => !task.active);
        const actionNeeded = (this.schedulerActive && allTasksStopped) || (!this.schedulerActive && !allTasksStopped);
        
        if (actionNeeded) {
          // 如果状态不一致，先修正状态，而不是发送不必要的请求
          this.schedulerActive = !this.schedulerActive;
          this.schedulerLoading = false;
          message.info('已同步任务状态显示');
          return;
        }
        
        // 主请求，控制所有任务
        promises.push(this.$axiosGet(global.API.queueManager, {
          action: action,
          scope: 'scheduler'
        }))
        
        if (action === 'resume') {
          // 明确启动队列健康监控
          promises.push(this.$axiosGet(global.API.queueManager, {
            action: 'resumeTask',
            taskId: 'queueHealthMonitor',
            scope: 'scheduler'
          }))
          
          // 明确启动任务超时监控
          promises.push(this.$axiosGet(global.API.queueManager, {
            action: 'resumeTask',
            taskId: 'taskTimeoutMonitor',
            scope: 'scheduler'
          }))
        } else {
          // 暂停操作：明确暂停两个监控任务
          promises.push(this.$axiosGet(global.API.queueManager, {
            action: 'pauseTask',
            taskId: 'queueHealthMonitor',
            scope: 'scheduler'
          }))
          
          promises.push(this.$axiosGet(global.API.queueManager, {
            action: 'pauseTask',
            taskId: 'taskTimeoutMonitor',
            scope: 'scheduler'
          }))
        }
        
        // 等待所有请求完成
        const responses = await Promise.all(promises)
        const mainResponse = responses[0]
        
        if (mainResponse && mainResponse.code === 200) {
          this.schedulerActive = !this.schedulerActive
          message.success(`定时任务已${this.schedulerActive ? '启用' : '暂停'}`)
          
          // 手动更新所有任务状态
          this.scheduledTasks.forEach(task => {
            task.active = this.schedulerActive;
          });
          
          // 立即刷新状态
          setTimeout(() => this.fetchFullStatus(false), 500)
        } else {
          message.error(`操作失败: ${mainResponse?.msg || '未知错误'}`)
          // 操作失败时同步状态
          this.syncGlobalTaskStatus();
        }
      } catch (error) {
        console.error('切换定时任务状态异常:', error)
        message.error('操作异常，请查看控制台日志')
        // 发生错误时同步状态
        this.syncGlobalTaskStatus();
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
        taskCleanup: 1, // 最小1秒
        connectionCleanup: 1, // 最小1秒
        queueHealthMonitor: 1, // 最小1秒
        taskTimeoutMonitor: 1 // 最小1秒
      }
      return minIntervals[taskId] || 1
    },
    
    async updateTaskInterval(record) {
      try {
        this.loading = true;
        // 设置record.saving状态为true
        record.saving = true;
        
        if (!record.newInterval || isNaN(record.newInterval)) {
          this.$message.error('请输入有效的间隔值');
          return;
        }

        // 检查最小间隔
        const minInterval = this.getMinInterval(record.id);
        const interval = parseInt(record.newInterval);
        if (interval < minInterval) {
          this.$message.warning(`${record.name}的最小间隔为${minInterval}秒`);
          record.newInterval = minInterval;
          return;
        }

        // 构建请求
        const type = record.id;
        console.log(`更新任务 ${type} 的间隔为 ${interval} 秒`);
        
        const response = await this.$axiosPost(global.API.updateTaskInterval, {
          taskId: type,
          intervalSeconds: interval
        });

        if (response && response.code === 200) {
          this.$message.success('更新间隔成功');
          
          // 立即更新UI显示的间隔，不等待fetchFullStatus
          record.interval = this.formatInterval(interval);
          
          // 延迟更新全部状态
          setTimeout(() => {
            this.fetchFullStatus(false);
          }, 500);
        } else {
          this.$message.error(`更新间隔失败: ${response?.msg || 'service is not defined'}`);
        }
      } catch (error) {
        this.$message.error(`更新间隔失败: ${error.message || 'service is not defined'}`);
        console.error('更新间隔失败', error);
      } finally {
        this.loading = false;
        // 设置record.saving状态为false
        record.saving = false;
      }
    },
    
    // 新增方法：根据服务器返回的状态更新任务状态
    updateTaskStatus(record, status) {
      if (!status || !record) return;
      
      // 根据任务ID寻找对应的状态更新
      switch(record.id) {
        case 'taskCleanup':
          if (status.asyncService) {
            console.log('更新任务清理状态:', {
              active: status.asyncService.taskCleanupActive,
              interval: status.asyncService.taskCleanupInterval,
              intervalMs: status.asyncService.taskCleanupIntervalMs,
              lastRun: status.asyncService.lastTaskCleanupTime
            });
            
            record.active = status.asyncService.taskCleanupActive || false;
            record.lastRun = status.asyncService.lastTaskCleanupTime || '未执行';
            
            // 处理间隔
            if (status.asyncService.taskCleanupIntervalMs) {
              const seconds = Math.floor(status.asyncService.taskCleanupIntervalMs / 1000);
              record.interval = this.formatInterval(seconds);
              record.newInterval = seconds;
              console.log('使用intervalMs设置任务清理间隔:', seconds, '秒');
            }
            // 其次使用interval
            else if (status.asyncService.taskCleanupInterval) {
              if (typeof status.asyncService.taskCleanupInterval === 'number') {
                const seconds = Math.floor(status.asyncService.taskCleanupInterval / 1000);
                record.interval = this.formatInterval(seconds);
                record.newInterval = seconds;
                console.log('使用interval数值设置任务清理间隔:', seconds, '秒');
              } else {
                record.interval = status.asyncService.taskCleanupInterval;
                record.newInterval = this.getIntervalSeconds(status.asyncService.taskCleanupInterval);
                console.log('使用interval字符串设置任务清理间隔:', record.newInterval, '秒');
              }
            } else {
              record.interval = '1小时';
              record.newInterval = 3600;
              console.log('未找到间隔信息，使用默认值:', 3600, '秒');
            }
          }
          break;
        case 'connectionCleanup':
          if (status.asyncService) {
            console.log('更新连接清理状态:', {
              active: status.asyncService.connectionCleanupActive,
              interval: status.asyncService.connectionCleanupInterval,
              intervalMs: status.asyncService.connectionCleanupIntervalMs,
              lastRun: status.asyncService.lastConnectionCleanupTime
            });
            
            record.active = status.asyncService.connectionCleanupActive || false;
            record.lastRun = status.asyncService.lastConnectionCleanupTime || '未执行';
            
            // 处理间隔
            if (status.asyncService.connectionCleanupIntervalMs) {
              const seconds = Math.floor(status.asyncService.connectionCleanupIntervalMs / 1000);
              record.interval = this.formatInterval(seconds);
              record.newInterval = seconds;
              console.log('使用intervalMs设置连接清理间隔:', seconds, '秒');
            }
            // 其次使用interval
            else if (status.asyncService.connectionCleanupInterval) {
              if (typeof status.asyncService.connectionCleanupInterval === 'number') {
                const seconds = Math.floor(status.asyncService.connectionCleanupInterval / 1000);
                record.interval = this.formatInterval(seconds);
                record.newInterval = seconds;
                console.log('使用interval数值设置连接清理间隔:', seconds, '秒');
              } else {
                record.interval = status.asyncService.connectionCleanupInterval;
                record.newInterval = this.getIntervalSeconds(status.asyncService.connectionCleanupInterval);
                console.log('使用interval字符串设置连接清理间隔:', record.newInterval, '秒');
              }
            } else {
              record.interval = '10分钟';
              record.newInterval = 600;
              console.log('未找到间隔信息，使用默认值:', 600, '秒');
            }
          }
          break;
        case 'queueHealthMonitor':
          if (status.queueManager) {
            console.log('更新队列健康监控状态:', {
              active: status.queueManager.queueHealthMonitorActive,
              interval: status.queueManager.queueHealthMonitorInterval,
              intervalMs: status.queueManager.queueHealthMonitorIntervalMs,
              lastRun: status.queueManager.lastQueueHealthMonitorTime
            });
            
            record.active = status.queueManager.queueHealthMonitorActive || false;
            
            // 优先使用intervalMs
            if (status.queueManager.queueHealthMonitorIntervalMs) {
              const seconds = Math.floor(status.queueManager.queueHealthMonitorIntervalMs / 1000);
              record.interval = this.formatInterval(seconds);
              record.newInterval = seconds;
              console.log('使用intervalMs设置间隔:', seconds, '秒');
            }
            // 其次使用interval (带单位的字符串或秒数)
            else if (status.queueManager.queueHealthMonitorInterval) {
              if (typeof status.queueManager.queueHealthMonitorInterval === 'number') {
                // 如果是数值，假设是毫秒
                const seconds = Math.floor(status.queueManager.queueHealthMonitorInterval / 1000);
                record.interval = this.formatInterval(seconds);
                record.newInterval = seconds;
                console.log('使用interval数值设置间隔:', seconds, '秒');
              } else {
                // 如果是字符串，解析并转换
                record.interval = status.queueManager.queueHealthMonitorInterval;
                record.newInterval = this.getIntervalSeconds(status.queueManager.queueHealthMonitorInterval);
                console.log('使用interval字符串设置间隔:', record.newInterval, '秒');
              }
            } else {
              record.interval = '60秒';
              record.newInterval = 60;
              console.log('未找到间隔信息，使用默认值:', 60, '秒');
            }
            
            // 处理上次执行时间
            if (status.queueManager.lastQueueHealthMonitorTime) {
              record.lastRun = status.queueManager.lastQueueHealthMonitorTime;
            } else {
              record.lastRun = '未执行';
            }
          }
          break;
        case 'taskTimeoutMonitor':
          if (status.queueManager) {
            console.log('更新任务超时监控状态:', {
              active: status.queueManager.taskTimeoutMonitorActive,
              interval: status.queueManager.taskTimeoutMonitorInterval,
              intervalMs: status.queueManager.taskTimeoutMonitorIntervalMs,
              lastRun: status.queueManager.lastTaskTimeoutMonitorTime
            });
            
            record.active = status.queueManager.taskTimeoutMonitorActive || false;
            
            // 优先使用intervalMs
            if (status.queueManager.taskTimeoutMonitorIntervalMs) {
              const seconds = Math.floor(status.queueManager.taskTimeoutMonitorIntervalMs / 1000);
              record.interval = this.formatInterval(seconds);
              record.newInterval = seconds;
              console.log('使用intervalMs设置超时监控间隔:', seconds, '秒');
            }
            // 其次使用interval (带单位的字符串或秒数)
            else if (status.queueManager.taskTimeoutMonitorInterval) {
              if (typeof status.queueManager.taskTimeoutMonitorInterval === 'number') {
                // 如果是数值，假设是毫秒
                const seconds = Math.floor(status.queueManager.taskTimeoutMonitorInterval / 1000);
                record.interval = this.formatInterval(seconds);
                record.newInterval = seconds;
                console.log('使用interval数值设置超时监控间隔:', seconds, '秒');
              } else {
                // 如果是字符串，解析并转换
                record.interval = status.queueManager.taskTimeoutMonitorInterval;
                record.newInterval = this.getIntervalSeconds(status.queueManager.taskTimeoutMonitorInterval);
                console.log('使用interval字符串设置超时监控间隔:', record.newInterval, '秒');
              }
            } else {
              record.interval = '30秒';
              record.newInterval = 30;
              console.log('未找到间隔信息，使用默认值:', 30, '秒');
            }
            
            // 处理上次执行时间
            if (status.queueManager.lastTaskTimeoutMonitorTime) {
              record.lastRun = status.queueManager.lastTaskTimeoutMonitorTime;
            } else {
              record.lastRun = '未执行';
            }
          }
          break;
      }
      
      console.log(`更新任务 ${record.id} 状态完成:`, record);
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
    
    // 添加重置为默认间隔的方法
    resetToDefaultInterval(record) {
      // 根据任务类型设置默认间隔值
      switch(record.id) {
        case 'taskCleanup':
          record.newInterval = 60; // 60秒
          break;
        case 'connectionCleanup':
          record.newInterval = 60; // 60秒
          break;
        case 'queueHealthMonitor':
          record.newInterval = 60; // 60秒
          break;
        case 'taskTimeoutMonitor':
          record.newInterval = 30; // 30秒
          break;
        default:
          record.newInterval = 60; // 默认60秒
      }
      
      // 直接调用更新方法，无需用户确认
      this.updateTaskInterval(record);
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
    // 添加新方法：同步全局任务状态和任务列表状态
    syncGlobalTaskStatus() {
      // 检查是否所有任务都已停止
      const allTasksStopped = this.scheduledTasks.every(task => !task.active);
      
      // 如果所有任务都已停止，但全局状态还是活跃，则更新全局状态
      if (allTasksStopped && this.schedulerActive) {
        this.schedulerActive = false;
      } 
      // 如果至少有一个任务是活跃的，但全局状态是停止的，则更新全局状态
      else if (!allTasksStopped && !this.schedulerActive) {
        this.schedulerActive = true;
      }
    },
    // 获取检查任务队列的状态灯CSS类
    getQueueStatusClass(stats) {
      if (!stats) return 'status-inactive';
      
      if (stats.runningTasks > 0) {
        return 'status-running';
      } else if (stats.queueSize > 0) {
        return 'status-waiting';
      } else if (!stats.queueProcessorThreadAlive) {
        return 'status-error';
      } else {
        return 'status-active';
      }
    },
    
    // 获取修复任务队列的状态灯CSS类
    getFixQueueStatusClass(stats) {
      if (!stats) return 'status-inactive';
      
      if (stats.runningFixTasks > 0) {
        return 'status-running';
      } else if (stats.fixQueueSize > 0) {
        return 'status-waiting';
      } else if (!stats.fixQueueProcessorThreadAlive) {
        return 'status-error';
      } else {
        return 'status-active';
      }
    },
    
    // 获取检查任务队列的悬浮提示内容
    getQueueStatusTooltip(stats) {
      if (!stats) return '无队列信息';
      
      let content = `检查任务队列: ${stats.queueSize || 0} 个等待, ${stats.runningTasks || 0} 个执行中\n`;
      
      if (this.queueTasks && this.queueTasks.length > 0) {
        content += `\n最近任务列表（${Math.min(5, this.queueTasks.length)}个):\n`;
        
        // 只显示前5个任务
        const limitedTasks = this.queueTasks.slice(0, 5);
        
        limitedTasks.forEach((task, index) => {
          content += `${index + 1}. ${task.hostname} - ${task.itemName} (${task.status})\n`;
        });
        
        if (this.queueTasks.length > 5) {
          content += `...还有 ${this.queueTasks.length - 5} 个任务\n`;
        }
      }
      
      return content;
    },
    
    // 获取修复任务队列的悬浮提示内容
    getFixQueueStatusTooltip(stats) {
      if (!stats) return '无队列信息';
      
      let content = `修复任务队列: ${stats.fixQueueSize || 0} 个等待, ${stats.runningFixTasks || 0} 个执行中\n`;
      
      if (this.fixQueueTasks && this.fixQueueTasks.length > 0) {
        content += `\n最近任务列表（${Math.min(5, this.fixQueueTasks.length)}个):\n`;
        
        // 只显示前5个任务
        const limitedTasks = this.fixQueueTasks.slice(0, 5);
        
        limitedTasks.forEach((task, index) => {
          content += `${index + 1}. ${task.hostname} - ${task.itemName} (${task.status})\n`;
        });
        
        if (this.fixQueueTasks.length > 5) {
          content += `...还有 ${this.fixQueueTasks.length - 5} 个任务\n`;
        }
      }
      
      return content;
    },
    
    // 鼠标悬停时获取检查任务队列数据
    fetchQueueTasksOnHover() {
      if (!this.queueTasks || this.queueTasks.length === 0) {
        this.fetchFullStatus(false);
      }
    },
    
    // 鼠标悬停时获取修复任务队列数据
    fetchFixQueueTasksOnHover() {
      if (!this.fixQueueTasks || this.fixQueueTasks.length === 0) {
        this.fetchFullStatus(false);
      }
    },
    
    // 处理检查队列Popover显示状态变化
    handleQueuePopoverVisibleChange(visible) {
      this.showQueuePopover = visible;
      if (visible) {
        this.fetchQueueTasksOnHover();
      }
    },
    
    // 处理修复队列Popover显示状态变化
    handleFixQueuePopoverVisibleChange(visible) {
      this.showFixQueuePopover = visible;
      if (visible) {
        this.fetchFixQueueTasksOnHover();
      }
    },
    formatQueueTime(startTimeStr) {
      if (!startTimeStr) return '未启动';
      
      try {
        // 解析起始时间
        const startTime = new Date(startTimeStr).getTime();
        if (isNaN(startTime)) return '时间格式错误';
        
        // 计算运行时间（毫秒）
        const runTimeMs = Date.now() - startTime;
        
        // 将毫秒转换为秒
        let seconds = Math.floor(runTimeMs / 1000);
        
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
        if (hours > 0 || days > 0) result += `${hours}小时 `;
        if (minutes > 0 || hours > 0 || days > 0) result += `${minutes}分钟 `;
        result += `${seconds}秒`;
        
        return result.trim();
      } catch (error) {
        console.error('格式化队列时间错误:', error);
        return '计算错误';
      }
    },
    
    // 获取缓存等级CSS类
    getCacheLevelClass(hitRate) {
      if (!hitRate || hitRate === 0) return 'level-low';
      if (hitRate < 30) return 'level-low';
      if (hitRate < 60) return 'level-medium';
      if (hitRate < 85) return 'level-good';
      return 'level-excellent';
    },
    
    // 获取缓存标签文本
    getCacheLabel(hitRate) {
      if (!hitRate || hitRate === 0) return '未使用缓存';
      if (hitRate < 30) return '缓存较少';
      if (hitRate < 60) return '缓存适中';
      if (hitRate < 85) return '缓存良好';
      return '缓存优秀';
    },
  }
}
</script>

<style lang="scss" scoped>
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

  .queue-runtime {
    display: flex;
    align-items: center;
    background: #f6f8fc;
    padding: 6px 12px;
    border-radius: 16px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
    border: 1px solid #e6f7ff;
    transition: all 0.3s ease;
    margin-right: 10px;
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 8px rgba(24, 144, 255, 0.15);
      border-color: #bae7ff;
    }

    .runtime-label {
      font-size: 12px;
      color: #8c8c8c;
      margin-right: 6px;
      font-weight: 500;
    }

    .runtime-value {
      font-size: 13px;
      color: #1890ff;
      font-weight: 600;
      font-family: 'Consolas', monospace;
      letter-spacing: 0.3px;
    }
  }

  :deep(.ant-switch) {
    @extend .custom-switch;
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

.scheduler-toggle-button {
  min-width: 120px !important;
  height: 38px !important;
  padding: 0 20px !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15) !important;
  font-weight: 500 !important;
  font-size: 14px !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  transition: all 0.3s cubic-bezier(0.645, 0.045, 0.355, 1) !important;
  
  &.active-button {
    background: linear-gradient(135deg, #ff7a45 0%, #ff4d4f 100%) !important;
    border: none !important;
    color: white !important;
    
    &:hover {
      background: linear-gradient(135deg, #ff7a45 0%, #ff7875 100%) !important;
      transform: translateY(-3px) !important;
      box-shadow: 0 8px 16px rgba(255, 77, 79, 0.3) !important;
    }
  }
  
  &.inactive-button {
    background: linear-gradient(135deg, #2f54eb 0%, #1890ff 100%) !important;
    border: none !important;
    color: white !important;
    
    &:hover {
      background: linear-gradient(135deg, #597ef7 0%, #40a9ff 100%) !important;
      transform: translateY(-3px) !important;
      box-shadow: 0 8px 16px rgba(24, 144, 255, 0.3) !important;
    }
  }
  
  &:hover {
    transform: translateY(-3px) !important;
  }
  
  &:active {
    transform: translateY(-1px) !important;
  }
  
  i {
    font-size: 18px !important;
    margin-right: 8px !important;
  }
  
  .button-text {
    font-size: 14px !important;
    line-height: 1 !important;
  }
  
  &.ant-btn-loading {
    opacity: 0.8;
  }
}

.modern-toggle-button {
  width: 40px !important;
  height: 40px !important;
  border: none !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15) !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  transition: all 0.3s cubic-bezier(0.645, 0.045, 0.355, 1) !important;
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%) !important;
  
  &:hover {
    transform: translateY(-3px) !important;
    box-shadow: 0 8px 16px rgba(24, 144, 255, 0.3) !important;
  }
  
  &:active {
    transform: translateY(-1px) !important;
    box-shadow: 0 4px 8px rgba(24, 144, 255, 0.3) !important;
  }
  
  i {
    font-size: 20px !important;
    color: #ffffff !important;
  }
  
  &.ant-btn-loading {
    background: linear-gradient(135deg, #40a9ff 0%, #1890ff 100%) !important;
  }
}

// 在样式部分添加新的样式
.custom-switch {
  &.ant-switch {
    background-color: #f0f0f0; 
    border: 1px solid #d9d9d9;
    height: 22px;
    min-width: 44px;
    line-height: 20px;

    &.ant-switch-checked {
      background-color: #1890ff;
      border-color: #1890ff;
    }
    
    .ant-switch-handle {
      width: 18px;
      height: 18px;
      top: 1px;
      left: 1px;
      background-color: #fff;
      border-radius: 50%;
      transform: translateY(0);
      border: 1px solid #d9d9d9;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
      transition: all 0.3s ease;
    }
    
    &.ant-switch-checked .ant-switch-handle {
      left: calc(100% - 19px);
      border-color: #fff;
    }
    
    .ant-switch-inner {
      margin: 0 7px 0 24px;
      color: rgba(0, 0, 0, 0.65);
      font-size: 12px;
      line-height: 20px;
    }
    
    &.ant-switch-checked .ant-switch-inner {
      margin: 0 24px 0 7px;
      color: #fff;
    }
  }
  
  &.ant-switch-small {
    height: 20px;
    min-width: 40px;
    line-height: 18px;
    
    .ant-switch-handle {
      width: 16px;
      height: 16px;
      top: 1px;
      left: 1px;
    }
    
    &.ant-switch-checked .ant-switch-handle {
      left: calc(100% - 17px);
    }
    
    .ant-switch-inner {
      margin: 0 5px 0 18px;
      font-size: 12px;
      line-height: 18px;
    }
    
    &.ant-switch-checked .ant-switch-inner {
      margin: 0 18px 0 5px;
    }
  }
}

// 系统控制开关也应用相同样式
.status-control {
  .ant-switch {
    @extend .custom-switch;
  }
}

// 详情状态灯样式
.detail-status-light {
  width: 16px;
  height: 16px;
  cursor: pointer;
  transition: transform 0.2s ease;
  border: 2px solid #fff;
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.1);
  
  &:hover {
    transform: scale(1.2);
    box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.2), 0 0 12px rgba(24, 144, 255, 0.5);
  }
}

.queue-popover-content {
  width: 800px;
  max-height: 500px;
  
  h3 {
    margin: 0 0 16px;
    font-size: 16px;
    font-weight: 600;
    color: #1a1a1a;
  }
}

:deep(.queue-detail-popover) {
  .ant-popover-inner-content {
    padding: 16px;
  }
  
  .ant-popover-arrow {
    border-color: #fff !important;
  }
}

.queue-header-stats {
  padding: 0 24px;
  margin: 10px 0;
  display: flex;
  justify-content: flex-end;
  align-items: center;

  .queue-runtime {
    display: flex;
    align-items: center;
    background: #f6f8fc;
    padding: 8px 16px;
    border-radius: 20px;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
    border: 1px solid #e6f7ff;
    transition: all 0.3s ease;
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(24, 144, 255, 0.15);
      border-color: #bae7ff;
    }

    .runtime-label {
      font-size: 13px;
      color: #8c8c8c;
      margin-right: 8px;
      font-weight: 500;
    }

    .runtime-value {
      font-size: 14px;
      color: #1890ff;
      font-weight: 600;
      font-family: 'Consolas', monospace;
      letter-spacing: 0.3px;
    }
  }
}

// 添加清理不活跃连接按钮的样式
.cleanup-connection-btn {
  min-width: 140px !important;
  height: 36px !important;
  padding: 0 16px !important;
  border: none !important;
  border-radius: 18px !important;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.12) !important;
  font-weight: 500 !important;
  font-size: 14px !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  transition: all 0.3s cubic-bezier(0.645, 0.045, 0.355, 1) !important;
  background: linear-gradient(135deg, #36cfc9 0%, #13c2c2 100%) !important;
  color: white !important;

  &:hover {
    background: linear-gradient(135deg, #5cdbd3 0%, #36cfc9 100%) !important;
    transform: translateY(-2px) !important;
    box-shadow: 0 6px 12px rgba(19, 194, 194, 0.3) !important;
  }

  &:active {
    transform: translateY(-1px) !important;
    box-shadow: 0 4px 8px rgba(19, 194, 194, 0.2) !important;
  }

  &.ant-btn-loading {
    opacity: 0.8;
    background: linear-gradient(135deg, #5cdbd3 0%, #13c2c2 100%) !important;
  }

  i {
    font-size: 16px !important;
    margin-right: 6px !important;
  }

  .button-text {
    font-size: 13px !important;
    line-height: 1 !important;
  }
}

.system-uptime {
  display: flex;
  align-items: center;
  gap: 8px;

  .time-icon {
    font-size: 16px;
    color: #1890ff;
    animation: pulse 1.5s infinite ease-in-out;
  }

  .uptime-text {
    font-family: 'Consolas', monospace;
    font-size: 14px;
    background: linear-gradient(90deg, #1890ff, #52c41a);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    font-weight: 500;
    letter-spacing: 0.5px;
  }
}

.stat-icon {
  font-size: 16px;
  margin-right: 8px;
  color: #1890ff;
}

.thread-icon {
  color: #722ed1;
}

.task-icon {
  color: #52c41a;
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

.timing-display {
  display: flex;
  align-items: center;
  gap: 4px;

  .timing-icon {
    font-size: 16px;
    color: #1890ff;
  }

  .timing-text {
    font-family: 'Consolas', monospace;
    font-size: 14px;
    color: #333;
  }
}

.avg-time-icon {
  color: #52c41a;
}

.max-time-icon {
  color: #ff4d4f;
}

.queue-stat {
  display: flex;
  align-items: center;
  gap: 4px;

  .queue-icon {
    font-size: 16px;
    margin-right: 4px;
  }

  .waiting-icon {
    color: #faad14;
  }

  .running-icon {
    color: #1890ff;
    animation: spin 1s linear infinite;
  }
  
  .completed-icon {
    color: #52c41a;
  }
  
  .capacity-icon {
    color: #722ed1;
    animation: pulse 3s infinite ease-in-out;
  }

  @keyframes spin {
    from {
      transform: rotate(0deg);
    }
    to {
      transform: rotate(360deg);
    }
  }
}

.cleanup-time {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fafafa, #f0f4f8);
  border-radius: 6px;
  padding: 6px 8px;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.05);
  width: 100%;
  transition: all 0.3s ease;
  min-height: 36px;
  overflow: hidden;

  &:hover {
    box-shadow: inset 0 1px 5px rgba(24, 144, 255, 0.1);
    background: linear-gradient(135deg, #f4f9ff, #edf6ff);
  }

  .cleanup-text {
    font-family: 'Consolas', monospace;
    font-size: 13px;
    background: linear-gradient(90deg, #1890ff, #52c41a);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    font-weight: 500;
    white-space: nowrap;
    text-shadow: 0 0 1px rgba(24, 144, 255, 0.1);
    animation: text-glow 3s infinite alternate;
    overflow: hidden;
    text-overflow: ellipsis;
    width: 100%;
    text-align: center;
  }
}

@keyframes text-glow {
  0% {
    text-shadow: 0 0 1px rgba(24, 144, 255, 0.1);
  }
  100% {
    text-shadow: 0 0 3px rgba(24, 144, 255, 0.3);
  }
}

.connection-status-badge {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  text-align: center;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  min-width: 60px;
  position: relative;
  overflow: hidden;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.status-text {
  position: relative;
  z-index: 2;
}

.status-ripple {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1;
}

.status-active {
  background: linear-gradient(90deg, #52c41a, #1ab394);
  color: #ffffff;
  box-shadow: 0 3px 10px rgba(82, 196, 26, 0.4);
  animation: pulse-active 2s infinite, glow-active 1.5s ease-in-out infinite alternate;
}

.status-active .status-ripple {
  background: radial-gradient(circle, rgba(255,255,255,0.3) 0%, rgba(255,255,255,0) 70%);
  animation: ripple 2s infinite ease-out;
}

.status-inactive {
  background: linear-gradient(90deg, #ff4d4f, #cf1322);
  color: #ffffff;
  box-shadow: 0 3px 10px rgba(255, 77, 79, 0.4);
  animation: pulse-inactive 2s infinite;
}

.status-inactive .status-ripple {
  background: radial-gradient(circle, rgba(255,255,255,0.2) 0%, rgba(255,255,255,0) 70%);
  animation: ripple 3s infinite ease-out;
}

@keyframes glow-active {
  from {
    text-shadow: 0 0 2px #fff, 0 0 4px #fff, 0 0 6px #52c41a, 0 0 8px #52c41a;
  }
  to {
    text-shadow: 0 0 4px #fff, 0 0 6px #1ab394, 0 0 8px #1ab394, 0 0 10px #1ab394;
  }
}

@keyframes ripple {
  0% {
    transform: scale(0.8);
    opacity: 0.3;
  }
  100% {
    transform: scale(1.5);
    opacity: 0;
  }
}

@keyframes pulse-active {
  0% {
    box-shadow: 0 0 0 0 rgba(82, 196, 26, 0.7);
  }
  70% {
    box-shadow: 0 0 0 8px rgba(82, 196, 26, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(82, 196, 26, 0);
  }
}

@keyframes pulse-inactive {
  0% {
    box-shadow: 0 0 0 0 rgba(255, 77, 79, 0.7);
  }
  70% {
    box-shadow: 0 0 0 8px rgba(255, 77, 79, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(255, 77, 79, 0);
  }
}

.task-stat {
  display: flex;
  align-items: center;
  gap: 6px;
  
  .task-icon {
    font-size: 16px;
  }
}

.success-stat {
  .task-icon {
    color: #52c41a;
    animation: pulse 2s infinite alternate;
  }
}

.failed-stat {
  .task-icon {
    color: #ff4d4f;
    animation: shake 1s ease-in-out infinite;
  }
}

@keyframes shake {
  0%, 100% {
    transform: translateX(0);
  }
  20%, 60% {
    transform: translateX(-2px);
  }
  40%, 80% {
    transform: translateX(2px);
  }
}

.thread-stat {
  display: flex;
  align-items: center;
  gap: 6px;
  
  .thread-icon {
    font-size: 16px;
  }
  
  .active-icon {
    color: #1890ff;
    animation: flash 2s infinite;
  }
  
  .pool-icon {
    color: #722ed1;
  }
  
  .complete-icon {
    color: #52c41a;
  }
  
  .queue-icon {
    color: #fa8c16;
  }
}

@keyframes flash {
  0%, 50%, 100% {
    opacity: 1;
  }
  25%, 75% {
    opacity: 0.5;
  }
}

.enhanced-light {
  position: relative;
  
  &::before {
    content: '';
    position: absolute;
    top: -4px;
    left: -4px;
    right: -4px;
    bottom: -4px;
    background: inherit;
    border-radius: 50%;
    filter: blur(4px);
    opacity: 0.7;
    z-index: -1;
    animation: pulse-light 2s infinite ease-in-out;
  }
}

@keyframes pulse-light {
  0% {
    transform: scale(0.9);
    opacity: 0.7;
  }
  50% {
    transform: scale(1.2);
    opacity: 0.3;
  }
  100% {
    transform: scale(0.9);
    opacity: 0.7;
  }
}

.success-rate {
  display: flex;
  align-items: center;
  gap: 8px;
  
  .rate-icon {
    font-size: 16px;
    color: #52c41a;
    animation: rotate 3s linear infinite;
  }
  
  .rate-text {
    font-family: 'Consolas', monospace;
    font-size: 14px;
    font-weight: bold;
    background: linear-gradient(90deg, #faad14, #52c41a);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
}

.animated-time {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: 'Consolas', monospace;
  color: #1890ff;
  
  .runtime-icon {
    color: #1890ff;
    animation: pulse 2s infinite;
  }
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.ssh-cache-stat {
  width: 100%;

  .cache-container {
    width: 100%;
    height: 50px;
    background: linear-gradient(135deg, #f5f5f5, #fafafa);
    border-radius: 8px;
    overflow: hidden;
    position: relative;
    box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.05);
    
    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: repeating-linear-gradient(
        45deg,
        rgba(0, 0, 0, 0.03),
        rgba(0, 0, 0, 0.03) 10px,
        rgba(0, 0, 0, 0.06) 10px,
        rgba(0, 0, 0, 0.06) 20px
      );
      opacity: 0.3;
      z-index: 1;
    }
  }

  .cache-level {
    height: 100%;
    width: 100%;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    position: relative;
    z-index: 2;
    padding: 0 16px;
    transition: all 0.5s ease;
    
    &.level-low {
      background: linear-gradient(135deg, #fff1f0, #ffccc7);
      box-shadow: 0 0 10px rgba(255, 77, 79, 0.2);
    }
    
    &.level-medium {
      background: linear-gradient(135deg, #fff7e6, #ffd591);
      box-shadow: 0 0 10px rgba(250, 173, 20, 0.2);
    }
    
    &.level-good {
      background: linear-gradient(135deg, #f6ffed, #b7eb8f);
      box-shadow: 0 0 10px rgba(82, 196, 26, 0.2);
    }
    
    &.level-excellent {
      background: linear-gradient(135deg, #e6f7ff, #91d5ff);
      box-shadow: 0 0 10px rgba(24, 144, 255, 0.2);
      animation: excellent-pulse 3s infinite alternate;
    }
    
    &:hover {
      transform: scale(1.02);
    }
    
    .cache-value {
      font-size: 18px;
      font-weight: 600;
      color: #262626;
      text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
    }
    
    .cache-label {
      font-size: 12px;
      color: #595959;
      margin-top: 2px;
    }
    
    &::after {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: linear-gradient(
        135deg,
        rgba(255, 255, 255, 0.2) 0%,
        rgba(255, 255, 255, 0) 50%,
        rgba(0, 0, 0, 0.02) 100%
      );
      z-index: -1;
    }
  }
}

@keyframes excellent-pulse {
  0% {
    box-shadow: 0 0 10px rgba(24, 144, 255, 0.2);
  }
  100% {
    box-shadow: 0 0 18px rgba(24, 144, 255, 0.4);
  }
}
</style> 