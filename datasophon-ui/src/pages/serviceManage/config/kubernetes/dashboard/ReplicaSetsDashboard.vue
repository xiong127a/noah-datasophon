<template>
  <div class="resource-list">
    <!-- ReplicaSets列表区域 -->
    <div class="kubernetes-dashboard-card kubernetes-resource-card">
      <div class="kubernetes-card-header">
        <span class="kubernetes-card-title">Replica Sets</span>
        <div class="kubernetes-card-actions">
          <a-icon type="bars" class="kubernetes-action-icon" />
          <a class="kubernetes-card-collapse-icon">
            <a-icon type="minus" />
          </a>
        </div>
      </div>
      <div class="kubernetes-card-content">
        <a-spin :spinning="loading">
          <a-table 
            :columns="replicaSetColumns" 
            :dataSource="replicaSets" 
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
            class="kubernetes-table"
            :table-layout="'auto'"
            :bordered="false"
            size="middle"
            @change="handleTableChange"
          >
            <template slot="name" slot-scope="text, record">
              <div style="display: flex; align-items: center; line-height: normal;">
                <StatusIndicator :resource="record" resourceType="replicaSet" />
                <div class="name-cell">
                  <span class="pod-name" :title="(record && record.objectMeta && record.objectMeta.name) || '未知'">
                    {{ (record && record.objectMeta && record.objectMeta.name) || '未知' }}
                  </span>
                </div>
              </div>
            </template>

            <template slot="image" slot-scope="text, record">
              <div class="image-cell" :title="record && record.containerImages ? record.containerImages.join(', ') : ''">
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
              <div v-if="record && record.objectMeta && record.objectMeta.labels && Object.keys(record.objectMeta.labels).length > 0" class="labels-container">
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
                <span>{{ record && record.podInfo && record.podInfo.running !== undefined ? record.podInfo.running : 0 }} / {{ record && record.podInfo && record.podInfo.desired !== undefined ? record.podInfo.desired : 0 }}</span>
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
  name: 'ReplicaSetsDashboard',
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
      replicaSets: [],
      loading: false,
      expandedLabels: {},  // 用于存储展开状态的对象
      pagination: {
        current: 1,
        pageSize: 10,
        total: 0
      },
      totalPages: 1,
      replicaSetColumns: [
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
    this.fetchReplicaSets();
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
    handleTableChange(pagination) {
      this.pagination.current = pagination.current;
      this.pagination.pageSize = pagination.pageSize;
      this.fetchReplicaSets();
    },
    async fetchReplicaSets() {
      this.loading = true;
      try {
        const params = {
          clusterId: this.clusterId,
          serviceId: this.serviceId,
          pageNum: this.pagination.current,
          pageSize: this.pagination.pageSize,
          // Only add namespace if it's not 'all'
          ...(this.selectedNamespace !== 'all' && { namespace: this.selectedNamespace })
        };
        
        const res = await this.$axiosGet(global.API.getKubernetesReplicaSets, params);
        
        if (res.code === 200) {
          // 确保获取ReplicaSets列表数组
          let replicaSetList = res.data && res.data.replicaSets ? res.data.replicaSets : [];
          
          // 处理ReplicaSets数据，确保每个项都有必要的属性
          this.replicaSets = replicaSetList.map(replicaSet => {
            // 如果replicaSet为null或undefined，返回一个空对象
            if (!replicaSet) return { objectMeta: {}, podInfo: {} };
            
            // 确保objectMeta存在
            if (!replicaSet.objectMeta) replicaSet.objectMeta = {};
            
            // 确保podInfo存在
            if (!replicaSet.podInfo) replicaSet.podInfo = {};
            
            return replicaSet;
          });
          
          // 更新分页信息
          this.pagination.total = res.data.total || 0;
          this.totalPages = res.data.totalPages || 1;
          
          console.log("处理后的replicaSets数据:", this.replicaSets);
        } else {
          console.error('Failed to fetch replicaSets:', res.msg);
          this.replicaSets = [];
        }
      } catch (error) {
        console.error('Error fetching replicaSets:', error);
        this.replicaSets = [];
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
      if (!time) return '';
      
      const date = new Date(time);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      const seconds = String(date.getSeconds()).padStart(2, '0');
      
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
    }
  },
  watch: {
    selectedNamespace() {
      this.fetchReplicaSets();
    },
    clusterId() {
      this.fetchReplicaSets();
    },
    serviceId() {
      this.fetchReplicaSets();
    }
  }
};
</script>

<style lang="less" scoped>
@import 'styles/kubernetes-table-styles.less';

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