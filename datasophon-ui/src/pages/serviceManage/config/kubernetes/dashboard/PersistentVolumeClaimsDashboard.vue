<template>
  <div class="resource-list">
    <!-- PVC列表区域 -->
    <div class="k8s-dashboard-card k8s-resource-card">
      <div class="k8s-card-header">
        <span class="k8s-card-title">Persistent Volume Claims</span>
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
            :columns="columns" 
            :dataSource="persistentVolumeClaims" 
            :pagination="false"
            :rowKey="record => record?.objectMeta?.uid || Math.random().toString(36).substring(2)"
            class="k8s-table"
            :table-layout="'auto'"
            :bordered="false"
            size="middle"
          >
            <template slot="name" slot-scope="text, record">
              <span class="name-text" :title="record?.objectMeta?.name || '未知'">
                {{ record?.objectMeta?.name || '未知' }}
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

            <template slot="status" slot-scope="text, record">
              <a-badge 
                :status="getStatusType(record.status)" 
                :text="record.status || '-'" 
              />
            </template>

            <template slot="capacity" slot-scope="text, record">
              <span class="capacity-cell" :title="record?.capacity?.storage || '-'">
                {{ record?.capacity?.storage || '-' }}
              </span>
            </template>

            <template slot="accessModes" slot-scope="text, record">
              <div v-if="record.accessModes && record.accessModes.length > 0" class="access-modes-container">
                <a-tag 
                  v-for="(mode, idx) in record.accessModes"
                  :key="idx" 
                  color="green"
                  class="access-mode-tag"
                >
                  {{ mode }}
                </a-tag>
              </div>
              <span v-else class="empty-value">-</span>
            </template>

            <template slot="storageClass" slot-scope="text, record">
              <span class="storage-class-cell" :title="record?.storageClass || '-'">
                {{ record?.storageClass || '-' }}
              </span>
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
  name: 'PersistentVolumeClaimsDashboard',
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
      persistentVolumeClaims: [],
      loading: false,
      expandedLabels: {},
      columns: [
        {
          title: '名称',
          key: 'name',
          className: 'name-column',
          scopedSlots: { customRender: 'name' }
        },
        {
          title: '标签',
          key: 'labels',
          className: 'labels-column',
          scopedSlots: { customRender: 'labels' }
        },
        {
          title: '状态',
          key: 'status',
          className: 'status-column',
          scopedSlots: { customRender: 'status' }
        },
        {
          title: 'Volume',
          dataIndex: 'volume',
          key: 'volume'
        },
        {
          title: '容量',
          key: 'capacity',
          className: 'capacity-column',
          scopedSlots: { customRender: 'capacity' }
        },
        {
          title: '访问模式',
          key: 'accessModes',
          className: 'access-modes-column',
          scopedSlots: { customRender: 'accessModes' }
        },
        {
          title: '存储类',
          key: 'storageClass',
          className: 'storage-class-column',
          scopedSlots: { customRender: 'storageClass' }
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
    this.fetchPersistentVolumeClaims();
  },
  methods: {
    async fetchPersistentVolumeClaims() {
      this.loading = true;
      try {
        const params = { 
          clusterId: this.clusterId,
          ...(this.selectedNamespace !== 'all' && { namespace: this.selectedNamespace })
        };
        
        const res = await this.$axiosGet(global.API.getK8sPersistentVolumeClaims, params);
        
        if (res.code === 200) {
          // 确保获取PVC列表数组
          let pvcList = res.data && res.data.items ? res.data.items : [];
          
          // 处理数据，确保每个项都有必要的属性
          this.persistentVolumeClaims = pvcList.map(pvc => {
            // 如果为null或undefined，返回一个空对象
            if (!pvc) return { objectMeta: {}, capacity: {}, accessModes: [] };
            
            // 确保objectMeta存在
            if (!pvc.objectMeta) pvc.objectMeta = {};
            
            // 确保capacity存在
            if (!pvc.capacity) pvc.capacity = {};
            
            // 确保accessModes存在
            if (!pvc.accessModes) pvc.accessModes = [];
            
            return pvc;
          });
          
          console.log("处理后的persistentVolumeClaims数据:", this.persistentVolumeClaims);
        } else {
          console.error('Failed to fetch PersistentVolumeClaims:', res.msg);
          this.persistentVolumeClaims = [];
        }
      } catch (error) {
        console.error('Error fetching PersistentVolumeClaims:', error);
        this.persistentVolumeClaims = [];
      } finally {
        this.loading = false;
      }
    },
    getStatusType(status) {
      if (!status) return 'default';
      
      switch(status.toLowerCase()) {
        case 'bound': 
          return 'success';
        case 'pending': 
          return 'processing';
        case 'lost': 
          return 'error';
        default: 
          return 'default';
      }
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
    clusterId() {
      this.fetchPersistentVolumeClaims();
    },
    selectedNamespace() {
      this.fetchPersistentVolumeClaims();
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

.labels-container {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  
  .label-tag {
    margin-right: 0;
  }
}

.capacity-cell, .storage-class-cell {
  word-break: break-word;
  line-height: 1.5;
  display: block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
}

.access-modes-container {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  max-width: 100%;
  
  .access-mode-tag {
    margin-right: 0;
    word-break: break-word;
    max-width: 100%;
  }
}
</style> 