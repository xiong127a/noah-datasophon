<template>
  <div class="resource-list">
    <!-- CronJobs列表区域 -->
    <div class="k8s-dashboard-card k8s-resource-card">
      <div class="k8s-card-header">
        <span class="k8s-card-title">Cron Jobs</span>
        <div class="k8s-card-actions">
          <a-icon type="bars" class="k8s-action-icon" />
          <a class="k8s-card-collapse-icon">
            <a-icon type="minus" />
          </a>
        </div>
      </div>
      <div class="k8s-card-content">
        <a-spin :spinning="loading">
          <a-table
            :columns="cronJobColumns"
            :dataSource="cronJobs"
            :pagination="{
              current: pagination.current,
              pageSize: pagination.pageSize,
              total: pagination.total,
              showSizeChanger: true,
              showQuickJumper: true,
              pageSizeOptions: ['5', '10', '20', '50'],
              hideOnSinglePage: totalPages <= 1,
              showTotal: total => `共 ${total} 条记录`
            }"
            :rowKey="record => (record && record.objectMeta && record.objectMeta.uid) || Math.random().toString(36).substring(2)"
            class="k8s-table"
            :table-layout="'auto'"
            :bordered="false"
            size="middle"
            @change="handleTableChange"
          >
            <template slot="name" slot-scope="text, record">
              <div style="display: flex; align-items: center; line-height: normal;">
                <span class="status-dot" :class="record.active > 0 ? 'status-running' : 'status-inactive'"></span>
                <div class="name-cell">
                  <span class="pod-name" :title="(record && record.objectMeta && record.objectMeta.name) || '未知'">
                    {{ (record && record.objectMeta && record.objectMeta.name) || '未知' }}
                  </span>
                </div>
              </div>
            </template>

            <template slot="namespace" slot-scope="text, record">
              <span class="namespace-cell" :title="(record && record.objectMeta && record.objectMeta.namespace) || '-'">
                {{ (record && record.objectMeta && record.objectMeta.namespace) || '-' }}
              </span>
            </template>

            <template slot="image" slot-scope="text, record">
              <div class="image-cell" :title="(record && record.containerImages) ? record.containerImages.join(', ') : ''">
                <template v-if="record && record.containerImages && record.containerImages.length">
                  <span class="container-image">
                    {{ record.containerImages[0] }}
                  </span>
                  <span v-if="record.containerImages.length > 1">+{{ record.containerImages.length - 1 }}</span>
                </template>
                <span v-else class="empty-value">-</span>
              </div>
            </template>

            <template slot="labels" slot-scope="text, record">
              <div v-if="record.objectMeta && record.objectMeta.labels && Object.keys(record.objectMeta.labels).length > 0" class="labels-container">
                <template v-if="!isLabelsExpanded(record)">
                  <a-tag
                    v-for="(entry, idx) in Object.entries(record.objectMeta.labels).slice(0, 3)"
                    :key="idx"
                    color="blue"
                    class="label-tag"
                    :title="`${entry[0]}: ${entry[1]}`"
                  >
                    {{ entry[0] }}: {{ entry[1] }}
                  </a-tag>
                  <a-button
                    v-if="Object.keys(record.objectMeta.labels).length > 3"
                    type="link"
                    size="small"
                    @click.stop="toggleLabelsExpand(record)"
                  >
                    +{{ Object.keys(record.objectMeta.labels).length - 3 }} 更多
                  </a-button>
                </template>
                <template v-else>
                  <a-tag
                    v-for="(entry, idx) in Object.entries(record.objectMeta.labels)"
                    :key="idx"
                    color="blue"
                    class="label-tag"
                    :title="`${entry[0]}: ${entry[1]}`"
                  >
                    {{ entry[0] }}: {{ entry[1] }}
                  </a-tag>
                  <a-button
                    type="link"
                    size="small"
                    @click.stop="toggleLabelsExpand(record)"
                  >
                    收起
                  </a-button>
                </template>
              </div>
              <span v-else class="empty-value">-</span>
            </template>

            <template slot="schedule" slot-scope="text, record">
              <span class="schedule-cell" :title="record.schedule || '-'">
                {{ record.schedule || '-' }}
              </span>
              <a-tooltip v-if="record.schedule" placement="top">
                <template slot="title">
                  <span>{{ formatCronSchedule(record.schedule) }}</span>
                </template>
                <a-icon type="info-circle" class="schedule-info-icon" />
              </a-tooltip>
            </template>

            <template slot="suspend" slot-scope="text, record">
              <a-tag :color="record.suspend ? 'orange' : 'green'" class="suspend-tag">
                {{ record.suspend ? '是' : '否' }}
              </a-tag>
            </template>

            <template slot="active" slot-scope="text, record">
              <span class="active-cell" :class="{'active-running': record.active > 0}">
                {{ record.active || '0' }}
              </span>
            </template>

            <template slot="lastSchedule" slot-scope="text, record">
              <span class="time-cell" :title="formatTime(record.lastSchedule)">
                {{ record.lastSchedule ? getDaysAgo(record.lastSchedule) : '-' }}
              </span>
            </template>

            <template slot="creationTime" slot-scope="text, record">
              <span class="time-cell" :title="formatTime(record && record.objectMeta && record.objectMeta.creationTimestamp)">
                {{ getDaysAgo(record && record.objectMeta && record.objectMeta.creationTimestamp) }}
              </span>
            </template>
          </a-table>
        </a-spin>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CronJobsDashboard',
  props: {
    clusterId: {
      type: Number,
      required: true
    },
    selectedNamespace: {
      type: String,
      default: 'datasophon'
    }
  },
  data() {
    return {
      cronJobs: [],
      loading: false,
      expandedLabels: {},
      totalPages: 1,
      pagination: {
        current: 1,
        pageSize: 10,
        total: 0,
        showSizeChanger: true,
        showQuickJumper: true,
        pageSizeOptions: ['5', '10', '20', '50']
      },
      cronJobColumns: [
        {
          title: '名称',
          key: 'name',
          className: 'name-column',
          scopedSlots: { customRender: 'name' },
          width: '180px'
        },
        {
          title: '命名空间',
          key: 'namespace',
          className: 'namespace-column',
          scopedSlots: { customRender: 'namespace' },
          width: '120px'
        },
        {
          title: '镜像',
          key: 'image',
          className: 'image-column',
          scopedSlots: { customRender: 'image' },
          width: '180px'
        },
        {
          title: '标签',
          key: 'labels',
          className: 'labels-column',
          scopedSlots: { customRender: 'labels' }
        },
        {
          title: '调度',
          dataIndex: 'schedule',
          key: 'schedule',
          className: 'schedule-column',
          scopedSlots: { customRender: 'schedule' },
          width: '120px'
        },
        {
          title: '暂停',
          dataIndex: 'suspend',
          key: 'suspend',
          className: 'suspend-column',
          scopedSlots: { customRender: 'suspend' },
          width: '80px'
        },
        {
          title: '运行中',
          dataIndex: 'active',
          key: 'active',
          className: 'active-column',
          scopedSlots: { customRender: 'active' },
          width: '80px'
        },
        {
          title: '最后调度',
          key: 'lastSchedule',
          className: 'last-schedule-column',
          scopedSlots: { customRender: 'lastSchedule' },
          width: '120px'
        },
        {
          title: '创建时间',
          key: 'creationTime',
          className: 'time-column',
          scopedSlots: { customRender: 'creationTime' },
          width: '120px'
        }
      ]
    };
  },
  mounted() {
    this.fetchCronJobs();
  },
  methods: {
    toggleLabelsExpand(record) {
      if (!record || !record.objectMeta || !record.objectMeta.uid) return;
      const uid = record.objectMeta.uid;
      this.$set(this.expandedLabels, uid, !this.expandedLabels[uid]);
    },
    isLabelsExpanded(record) {
      if (!record || !record.objectMeta || !record.objectMeta.uid) return false;
      return !!this.expandedLabels[record.objectMeta.uid];
    },
    async fetchCronJobs() {
      this.loading = true;
      try {
        const params = {
          clusterId: this.clusterId,
          namespace: this.selectedNamespace === 'all' ? null : this.selectedNamespace,
          pageNum: this.pagination.current,
          pageSize: this.pagination.pageSize
        };
        
        const res = await this.$axiosGet(global.API.getK8sCronJobs, params);
        
        if (res.code === 200 && res.data) {
          this.cronJobs = res.data.items || [];
          this.pagination.total = res.data.total || 0;
          this.totalPages = res.data.totalPages || 1;
        } else {
          console.error(res.msg || '获取CronJob列表失败');
          this.cronJobs = [];
          this.pagination.total = 0;
          this.totalPages = 1;
        }
      } catch (error) {
        console.error('Error fetching cronJobs:', error);
        this.cronJobs = [];
        this.pagination.total = 0;
        this.totalPages = 1;
      } finally {
        this.loading = false;
      }
    },
    formatCronSchedule(cronExpression) {
      // 简单解析cron表达式
      if (!cronExpression) return '';
      
      const parts = cronExpression.split(' ');
      if (parts.length !== 5) return '无效的cron表达式';
      
      let description = '';
      
      // 分钟
      if (parts[0] === '*') {
        description += '每分钟';
      } else if (parts[0].includes('/')) {
        const interval = parts[0].split('/')[1];
        description += `每${interval}分钟`;
      } else {
        description += `在第${parts[0]}分钟`;
      }
      
      // 小时
      if (parts[1] === '*') {
        description += '';
      } else if (parts[1].includes('/')) {
        const interval = parts[1].split('/')[1];
        description += `每${interval}小时`;
      } else {
        description += `的${parts[1]}点`;
      }
      
      // 日期
      if (parts[2] === '*') {
        description += '';
      } else if (parts[2].includes('/')) {
        const interval = parts[2].split('/')[1];
        description += `每${interval}天`;
      } else {
        description += `的${parts[2]}日`;
      }
      
      // 月份
      if (parts[3] === '*') {
        description += '';
      } else if (parts[3].includes('/')) {
        const interval = parts[3].split('/')[1];
        description += `每${interval}个月`;
      } else {
        description += `${parts[3]}月`;
      }
      
      // 星期
      if (parts[4] === '*') {
        description += '';
      } else {
        const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
        if (parts[4].includes(',')) {
          const days = parts[4].split(',').map(d => weekdays[parseInt(d) % 7]);
          description += `的${days.join(',')}`;
        } else {
          description += `的${weekdays[parseInt(parts[4]) % 7]}`;
        }
      }
      
      return description || '每分钟';
    },
    getDaysAgo(timestamp) {
      if (!timestamp) return '-';
      
      const date = new Date(timestamp);
      const now = new Date();
      
      // 计算时间差（毫秒）
      const timeDiff = Math.abs(now - date);
      
      // 转换为天数、小时、分钟
      const days = Math.floor(timeDiff / (1000 * 60 * 60 * 24));
      const hours = Math.floor((timeDiff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
      const minutes = Math.floor((timeDiff % (1000 * 60 * 60)) / (1000 * 60));
      
      // 根据时间差返回不同格式
      if (days > 0) {
        return `${days}天前`;
      } else if (hours > 0) {
        return `${hours}小时前`;
      } else if (minutes > 0) {
        return `${minutes}分钟前`;
      } else {
        return '刚刚';
      }
    },
    formatTime(time) {
      if (!time) return '-';
      return new Date(time).toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
      });
    },
    handleTableChange(pagination) {
      this.pagination.current = pagination.current;
      this.pagination.pageSize = pagination.pageSize;
      this.fetchCronJobs();
    }
  },
  watch: {
    selectedNamespace() {
      this.fetchCronJobs();
    },
    clusterId() {
      this.fetchCronJobs();
    }
  }
};
</script>

<style lang="less" scoped>
@import './styles/k8s-table-styles.less';

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 8px;
  flex-shrink: 0;
  
  &.status-running {
    background-color: #52c41a;
  }
  
  &.status-inactive {
    background-color: #d9d9d9;
  }
}

.pod-name {
  cursor: pointer;
  word-break: break-word !important;
  max-width: 100%;
  display: inline-block;
  white-space: normal !important;
  overflow: visible !important;
  text-overflow: clip !important;

  &:hover {
    color: #1890ff;
    text-decoration: underline;
  }
}

.name-cell, .namespace-cell {
  word-break: break-word !important;
  overflow-wrap: break-word !important;
  max-width: 100%;
  line-height: 1.5;
  display: block !important;
  overflow: visible !important;
  padding: 0;
  white-space: normal !important;
  text-overflow: clip !important;
}

.container-image {
  word-break: break-word !important;
  white-space: normal !important;
  overflow: visible !important;
  text-overflow: clip !important;
}

.image-cell {
  word-break: break-word !important;
  overflow-wrap: break-word !important;
  line-height: 1.5;
  display: block !important;
  overflow: visible !important;
  padding: 0;
  white-space: normal !important;
  text-overflow: clip !important;
}

.labels-container {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  
  .label-tag {
    margin-right: 0;
  }
}

.schedule-cell {
  display: inline-block;
  vertical-align: middle;
  margin-right: 4px;
}

.schedule-info-icon {
  color: #1890ff;
  font-size: 14px;
  cursor: pointer;
}

.suspend-tag {
  margin: 0;
  font-size: 12px;
  line-height: 20px;
  height: 22px;
}

.active-cell {
  font-weight: 400;
  
  &.active-running {
    font-weight: 600;
    color: #52c41a;
  }
}

/* 覆盖冲突样式 */
:deep(.ant-table-tbody > tr > td) {
  white-space: normal !important;
  word-break: break-word !important;
}
</style> 