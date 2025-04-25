<template>
  <div class="resource-list">
    <!-- ReplicaSets列表区域 -->
    <div class="k8s-dashboard-card k8s-resource-card">
      <div class="k8s-card-header">
        <span class="k8s-card-title">Replica Sets</span>
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
            :columns="replicaSetColumns" 
            :dataSource="replicaSets" 
            :pagination="false"
            :rowKey="record => `${record?.objectMeta?.namespace || 'unknown'}-${record?.objectMeta?.name || 'unknown'}`"
            class="k8s-table"
            :table-layout="'auto'"
            :bordered="false"
            size="middle"
          >
            <template slot="status" slot-scope="text, record">
              <span :class="getStatusClass(record)" class="status-indicator"></span>
            </template>

            <template slot="name" slot-scope="text, record">
              <span class="name-text" :title="record?.objectMeta?.name || '未知'">
                {{ record?.objectMeta?.name || '未知' }}
              </span>
            </template>

            <template slot="image" slot-scope="text, record">
              <span class="image-cell" :title="record?.containerImages ? record.containerImages.join(', ') : ''">
                {{ record?.containerImages ? record.containerImages.join(', ') : '-' }}
              </span>
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
                <span>{{ record?.podInfo && record.podInfo.running !== undefined ? record.podInfo.running : 0 }} / {{ record?.podInfo && record.podInfo.desired !== undefined ? record.podInfo.desired : 0 }}</span>
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
export default {
  name: 'ReplicaSetsDashboard',
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
      replicaSetColumns: [
        {
          title: '',
          key: 'status',
          width: '50px',
          className: 'status-column',
          scopedSlots: { customRender: 'status' }
        },
        {
          title: '名称',
          key: 'name',
          className: 'name-column',
          scopedSlots: { customRender: 'name' }
        },
        {
          title: '镜像',
          dataIndex: 'containerImages',
          key: 'image',
          className: 'image-column',
          scopedSlots: { customRender: 'image' },
          ellipsis: true
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
    getStatusClass(record) {
      const classNames = ['status-dot'];
      if (record?.podInfo?.running > 0) classNames.push('status-running');
      if (record?.podInfo?.pending > 0) classNames.push('status-warning');
      if (record?.podInfo?.failed > 0) classNames.push('status-danger');
      if (!record?.podInfo || (!record?.podInfo.running && !record?.podInfo.pending && !record?.podInfo.failed)) 
        classNames.push('status-unknown');
      return classNames.join(' ');
    },
    toggleLabelsExpand(record) {
      if (!record || !record.objectMeta || !record.objectMeta.uid) return;
      const uid = record.objectMeta.uid;
      this.$set(this.expandedLabels, uid, !this.expandedLabels[uid]);
    },
    isLabelsExpanded(record) {
      if (!record || !record.objectMeta || !record.objectMeta.uid) return false;
      return !!this.expandedLabels[record.objectMeta.uid];
    },
    async fetchReplicaSets() {
      this.loading = true;
      try {
        const res = await this.$axiosGet(global.API.getK8sReplicaSets, {
          clusterId: this.clusterId,
          serviceId: this.serviceId,
          namespace: this.selectedNamespace === 'all' ? null : this.selectedNamespace
        });
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
@import './styles/k8s-table-styles.less';

.status-indicator {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-right: 0;
}

.name-text {
  cursor: pointer;
  display: inline-block;
  max-width: 100%;
  
  &:hover {
    color: #1890ff;
    text-decoration: underline;
  }
}

.labels-container {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  
  .label-tag {
    margin-right: 0;
  }
}
</style> 