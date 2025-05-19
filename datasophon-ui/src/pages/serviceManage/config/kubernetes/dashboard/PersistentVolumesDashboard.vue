<template>
  <div class="resource-list">
    <!-- PersistentVolumes列表区域 -->
    <div class="k8s-dashboard-card k8s-resource-card">
      <div class="k8s-card-header">
        <span class="k8s-card-title">Persistent Volumes</span>
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
            :dataSource="persistentVolumes" 
            :pagination="pagination"
            :rowKey="record => (record && record.objectMeta && record.objectMeta.uid) || Math.random().toString(36).substring(2)"
            class="k8s-table"
            :table-layout="'auto'"
            :bordered="false"
            size="middle"
            @change="handleTableChange"
          >
            <template slot="name" slot-scope="text, record">
              <div style="display: flex; align-items: center;">
                <StatusIndicator :resource="record" resourceType="persistentVolume" />
                <span class="name-text" :title="record?.objectMeta?.name || '未知'">
                  {{ record?.objectMeta?.name || '未知' }}
                </span>
              </div>
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

            <template slot="reclaimPolicy" slot-scope="text, record">
              <span :class="['reclaim-policy', getReclaimPolicyClass(record.reclaimPolicy)]">
                {{ record.reclaimPolicy || '-' }}
              </span>
            </template>

            <template slot="status" slot-scope="text, record">
              <span>{{ record.status || '-' }}</span>
            </template>

            <template slot="claim" slot-scope="text, record">
              <span class="claim-cell" v-if="record.claimRef && record.claimRef.name">
                {{ record.claimRef.namespace }}/{{ record.claimRef.name }}
              </span>
              <span v-else class="empty-value">-</span>
            </template>

            <template slot="storageClass" slot-scope="text, record">
              <span class="storage-class-cell" :title="record?.storageClass || '-'">
                {{ record?.storageClass || '-' }}
              </span>
            </template>

            <template slot="reason" slot-scope="text, record">
              <span>{{ record?.reason || '-' }}</span>
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
  name: 'PersistentVolumesDashboard',
  components: {
    StatusIndicator
  },
  props: {
    clusterId: {
      type: Number,
      required: true
    }
  },
  data() {
    return {
      persistentVolumes: [],
      loading: false,
      pagination: {
        current: 1,
        pageSize: 10,
        total: 0,
        showTotal: total => `共 ${total} 条`,
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '50', '100'],
        showQuickJumper: true,
        hideOnSinglePage: true
      },
      columns: [
        {
          title: '名称',
          key: 'name',
          className: 'name-column',
          scopedSlots: { customRender: 'name' }
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
          title: '回收策略',
          key: 'reclaimPolicy',
          className: 'reclaim-policy-column',
          scopedSlots: { customRender: 'reclaimPolicy' }
        },
        {
          title: '状态',
          key: 'status',
          className: 'status-column',
          scopedSlots: { customRender: 'status' }
        },
        {
          title: '要求',
          key: 'claim',
          className: 'claim-column',
          scopedSlots: { customRender: 'claim' }
        },
        {
          title: '存储类',
          key: 'storageClass',
          className: 'storage-class-column',
          scopedSlots: { customRender: 'storageClass' }
        },
        {
          title: '原因',
          key: 'reason',
          className: 'reason-column',
          scopedSlots: { customRender: 'reason' }
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
    this.fetchPersistentVolumes();
  },
  methods: {
    async fetchPersistentVolumes(page = this.pagination.current, pageSize = this.pagination.pageSize) {
      this.loading = true;
      try {
        const res = await this.$axiosGet(global.API.getK8sPersistentVolumes, {
          clusterId: this.clusterId,
          pageNum: page,
          pageSize: pageSize
        });
        
        if (res.code === 200) {
          // 确保获取PersistentVolumes列表数组
          let pvList = [];
          
          if (Array.isArray(res.data)) {
            pvList = res.data;
          } else if (res.data && Array.isArray(res.data.items)) {
            pvList = res.data.items;
          }
          
          // 处理数据，确保每个项都有必要的属性
          this.persistentVolumes = pvList.map(pv => {
            // 如果为null或undefined，返回一个空对象
            if (!pv) return { objectMeta: {}, capacity: {}, accessModes: [] };
            
            // 确保objectMeta存在
            if (!pv.objectMeta) pv.objectMeta = {};
            
            // 确保capacity存在
            if (!pv.capacity) pv.capacity = {};
            
            // 确保accessModes存在
            if (!pv.accessModes) pv.accessModes = [];
            
            return pv;
          });
          
          // 更新分页信息
          this.pagination.total = res.data.total || this.persistentVolumes.length;
          this.pagination.current = page;
          this.pagination.pageSize = pageSize;
          
          console.log("处理后的PersistentVolumes数据:", this.persistentVolumes);
        } else {
          console.error('Failed to fetch PersistentVolumes:', res.msg);
          this.persistentVolumes = [];
          this.pagination.total = 0;
        }
      } catch (error) {
        console.error('Error fetching PersistentVolumes:', error);
        this.persistentVolumes = [];
        this.pagination.total = 0;
      } finally {
        this.loading = false;
      }
    },
    getStatusType(status) {
      if (!status) return 'default';
      
      switch(status.toLowerCase()) {
        case 'bound': 
          return 'success';
        case 'available': 
          return 'processing';
        case 'released': 
          return 'warning';
        case 'failed': 
          return 'error';
        default: 
          return 'default';
      }
    },
    getReclaimPolicyClass(policy) {
      if (!policy) return 'policy-unknown';
      
      switch(policy.toLowerCase()) {
        case 'delete':
          return 'policy-delete';
        case 'retain':
          return 'policy-retain';
        case 'recycle':
          return 'policy-recycle';
        default:
          return 'policy-unknown';
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
    // 处理表格分页、排序、筛选变化
    handleTableChange(pagination, filters, sorter) {
      console.log('Table change:', pagination, filters, sorter);
      this.fetchPersistentVolumes(pagination.current, pagination.pageSize);
    }
  },
  watch: {
    clusterId() {
      this.fetchPersistentVolumes();
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

.capacity-cell, .storage-class-cell, .claim-cell {
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

.reclaim-policy {
  display: inline-block;
  padding: 2px 6px;
  border-radius: 2px;
  font-size: 12px;
  
  &.policy-delete {
    background-color: #fff1f0;
    color: #f5222d;
    border: 1px solid #ffa39e;
  }
  
  &.policy-retain {
    background-color: #e6f7ff;
    color: #1890ff;
    border: 1px solid #91d5ff;
  }
  
  &.policy-recycle {
    background-color: #f6ffed;
    color: #52c41a;
    border: 1px solid #b7eb8f;
  }
  
  &.policy-unknown {
    background-color: #f5f5f5;
    color: #8c8c8c;
    border: 1px solid #d9d9d9;
  }
}

.empty-value {
  color: #bfbfbf;
  font-style: italic;
}
</style> 