<template>
  <div class="resource-list">
    <!-- Jobs列表区域 -->
    <div class="k8s-dashboard-card k8s-resource-card">
      <div class="k8s-card-header">
        <span class="k8s-card-title">Jobs</span>
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
            :columns="jobColumns" 
            :dataSource="jobs" 
            :pagination="false"
            :rowKey="record => `${record?.objectMeta?.namespace || 'unknown'}-${record?.objectMeta?.name || 'unknown'}`"
            class="k8s-table"
            :table-layout="'auto'"
            :bordered="false"
            :zebra-stripes="false"
            size="middle"
          >
            <template slot="name" slot-scope="text, record">
              <div style="display: flex; align-items: center; line-height: normal;">
                <StatusIndicator :resource="record" resourceType="job" />
                <div class="name-cell">
                  <span class="job-name" :title="record?.objectMeta?.name || '未知'">
                    {{ record?.objectMeta?.name || '未知' }}
                  </span>
                </div>
              </div>
            </template>

            <template slot="image" slot-scope="text, record">
              <div class="image-cell" :title="record?.containerImages ? record.containerImages.join(', ') : ''">
                <template v-if="record?.containerImages && record.containerImages.length">
                  <span class="container-image">
                    {{ record.containerImages[0] }}
                  </span>
                  <span v-if="record.containerImages.length > 1">+{{ record.containerImages.length - 1 }}</span>
                </template>
                <span v-else class="empty-value">-</span>
              </div>
            </template>

            <template slot="labels" slot-scope="text, record">
              <div v-if="record.objectMeta?.labels && Object.keys(record.objectMeta.labels).length > 0" class="labels-container">
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

            <template slot="pods" slot-scope="text, record">
              <div class="pods-display">
                <span>{{ getPodInfo(record) }}</span>
              </div>
            </template>

            <template slot="creationTime" slot-scope="text, record">
              <span class="time-cell" :title="formatTime(record.objectMeta?.creationTimestamp)">
                {{ getDaysAgo(record.objectMeta?.creationTimestamp) }}
              </span>
            </template>
          </a-table>
        </a-spin>
      </div>
    </div>
  </div>
</template>

<script>
import StatusIndicator from './components/StatusIndicator.vue';

export default {
  name: 'JobsDashboard',
  components: {
    StatusIndicator
  },
  props: {
    clusterId: {
      type: Number,
      required: true
    },
    serviceId: {
      type: [Number, String],
      required: true
    },
    selectedNamespace: {
      type: String,
      default: 'datasophon'
    }
  },
  data() {
    return {
      jobs: [],
      loading: false,
      expandedLabels: {},
      jobColumns: [
        {
          title: '名称',
          key: 'name',
          className: 'name-column',
          scopedSlots: { customRender: 'name' },
          width: '200px'
        },
        {
          title: '镜像',
          dataIndex: 'containerImages',
          key: 'image',
          className: 'image-column',
          scopedSlots: { customRender: 'image' },
          width: '200px'
        },
        {
          title: '标签',
          key: 'labels',
          className: 'labels-column',
          scopedSlots: { customRender: 'labels' }
        },
        {
          title: 'Pods',
          dataIndex: 'podInfo',
          key: 'pods',
          scopedSlots: { customRender: 'pods' }
        },
        {
          title: '创建时间',
          key: 'creationTime',
          className: 'time-column',
          scopedSlots: { customRender: 'creationTime' }
        }
      ]
    };
  },
  mounted() {
    this.fetchJobs();
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
    getPodInfo(record) {
      if (!record || !record.podInfo) return '0 / 0';
      
      const succeeded = record.podInfo.succeeded || 0;
      const desired = record.podInfo.desired || 0;
      
      return `${succeeded} / ${desired}`;
    },
    getJobStatus(record) {
      if (!record || !record.jobStatus) return 'Unknown';
      return record.jobStatus.status || 'Unknown';
    },
    async fetchJobs() {
      this.loading = true;
      try {
        const res = await this.$axiosGet(global.API.getK8sJobs, {
          clusterId: this.clusterId,
          serviceId: this.serviceId,
          // Only add namespace if it's not 'all'
          ...(this.selectedNamespace !== 'all' && { namespace: this.selectedNamespace })
        });
        
        if (res.code === 200) {
          // 确保获取Jobs列表数组，并处理数据
          let jobsList = res.data && res.data.jobs ? res.data.jobs : [];
          
          // 处理jobs数据，确保每个项都有必要的属性
          this.jobs = jobsList.map(job => {
            // 如果job为null或undefined，返回一个空对象
            if (!job) return { objectMeta: {}, podInfo: {} };
            
            // 确保objectMeta存在
            if (!job.objectMeta) job.objectMeta = {};
            
            // 确保podInfo存在
            if (!job.podInfo) job.podInfo = {};
            
            return job;
          });
          
          console.log("处理后的jobs数据:", this.jobs);
          
        } else {
          console.error('Failed to fetch jobs:', res.msg);
          this.jobs = [];
          this.$message.error(res.msg || 'Failed to fetch jobs');
        }
      } catch (error) {
        console.error('Error fetching jobs:', error);
        this.jobs = [];
        this.$message.error('Error fetching jobs');
      } finally {
        this.loading = false;
      }
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
    }
  },
  watch: {
    selectedNamespace() {
      this.fetchJobs();
    },
    clusterId() {
      this.fetchJobs();
    },
    serviceId() {
      this.fetchJobs();
    }
  }
};
</script>

<style lang="less" scoped>
@import './styles/k8s-table-styles.less';

.job-name {
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

.container-image {
  word-break: break-word !important;
  white-space: normal !important;
  overflow: visible !important;
  text-overflow: clip !important;
}

.name-cell {
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

.pods-display {
  font-weight: 500;
}

/* 覆盖KubernetesDashboard.vue中的样式 */
:deep(.ant-table-tbody > tr > td) {
  white-space: normal !important;
  word-break: break-word !important;
}
</style> 