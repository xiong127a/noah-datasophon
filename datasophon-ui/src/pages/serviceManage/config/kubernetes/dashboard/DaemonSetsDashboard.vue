<template>
  <div class="resource-list">
    <!-- DaemonSets列表区域 -->
    <div class="k8s-dashboard-card k8s-resource-card">
      <div class="k8s-card-header">
        <span class="k8s-card-title">Daemon Sets</span>
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
            :columns="daemonSetColumns"
            :dataSource="daemonSets"
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
            :rowKey="record => `${(record && record.objectMeta && record.objectMeta.namespace) || 'unknown'}-${(record && record.objectMeta && record.objectMeta.name) || 'unknown'}`"
            class="k8s-table"
            :table-layout="'auto'"
            :bordered="false"
            :zebra-stripes="false"
            size="middle"
            @change="handleTableChange"
          >
            <template slot="name" slot-scope="text, record">
              <div style="display: flex; align-items: center; line-height: normal;">
                <StatusIndicator :resource="record" resourceType="daemonset" />
                <div class="name-cell">
                  <span class="pod-name" :title="(record && record.objectMeta && record.objectMeta.name) || '未知'">
                    {{ (record && record.objectMeta && record.objectMeta.name) || '未知' }}
                  </span>
                </div>
              </div>
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

            <template slot="pods" slot-scope="text, record">
              <div class="pods-display">
                <span>{{ (record && record.podInfo && record.podInfo.current !== undefined) ? record.podInfo.current : 0 }} / {{ (record && record.podInfo && record.podInfo.desired !== undefined) ? record.podInfo.desired : 0 }}</span>
              </div>
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
import StatusIndicator from './components/StatusIndicator.vue';

export default {
  name: 'DaemonSetsDashboard',
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
      daemonSets: [],
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
      daemonSetColumns: [
        {
          title: '名称',
          key: 'name',
          className: 'name-column',
          scopedSlots: { customRender: 'name' },
          width: '180px'
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
          scopedSlots: { customRender: 'pods' },
          width: '100px'
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
    this.fetchDaemonSets();
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
    async fetchDaemonSets() {
      this.loading = true;
      try {
        const params = {
          clusterId: this.clusterId,
          serviceId: this.serviceId,
          namespace: this.selectedNamespace === 'all' ? null : this.selectedNamespace,
          pageNum: this.pagination.current,
          pageSize: this.pagination.pageSize
        };
        
        const res = await this.$axiosGet(global.API.getK8sDaemonSets, params);
        
        if (res.code === 200) {
          // 确保获取DaemonSets列表数组，并处理数据，确保每个对象都有必要的属性
          let daemonSetsList = res.data && res.data.daemonSets ? res.data.daemonSets : [];

          // 处理daemonSets数据，确保每个项都有必要的属性
          this.daemonSets = daemonSetsList.map(ds => {
            // 如果ds为null或undefined，返回一个空对象
            if (!ds) return { objectMeta: {}, podInfo: {} };

            // 确保objectMeta存在
            if (!ds.objectMeta) ds.objectMeta = {};

            // 确保podInfo存在
            if (!ds.podInfo) ds.podInfo = {};

            return ds;
          });

          // 更新分页信息
          if (res.data) {
            this.pagination.total = res.data.total || 0;
            this.totalPages = res.data.totalPages || 1;
          }

          console.log("处理后的daemonSets数据:", this.daemonSets);
          console.log("分页信息:", { total: this.pagination.total, totalPages: this.totalPages });
        } else {
          console.error('Failed to fetch daemonSets:', res.msg);
          this.daemonSets = [];
          this.pagination.total = 0;
          this.totalPages = 1;
        }
      } catch (error) {
        console.error('Error fetching daemonSets:', error);
        this.daemonSets = [];
        this.pagination.total = 0;
        this.totalPages = 1;
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
    },
    handleTableChange(pagination) {
      this.pagination.current = pagination.current;
      this.pagination.pageSize = pagination.pageSize;
      this.pagination.total = pagination.total;
      this.fetchDaemonSets();
    }
  },
  watch: {
    selectedNamespace() {
      this.fetchDaemonSets();
    },
    clusterId() {
      this.fetchDaemonSets();
    },
    serviceId() {
      this.fetchDaemonSets();
    }
  }
};
</script>

<style lang="less" scoped>
@import './styles/k8s-table-styles.less';

.name-text {
  cursor: pointer;
  display: inline-block;
  max-width: 100%;

  &:hover {
    color: #1890ff;
    text-decoration: underline;
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

/* 覆盖KubernetesDashboard.vue中的样式 */
:deep(.ant-table-tbody > tr > td) {
  white-space: normal !important;
  word-break: break-word !important;
}
</style> 