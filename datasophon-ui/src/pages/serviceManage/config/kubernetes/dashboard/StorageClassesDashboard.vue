<template>
  <div class="resource-list">
    <!-- StorageClasses列表区域 -->
    <div class="k8s-dashboard-card k8s-resource-card">
      <div class="k8s-card-header">
        <span class="k8s-card-title">Storage Classes</span>
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
            :dataSource="storageClasses" 
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
                <a-tag v-if="record?.objectMeta?.annotations && record?.objectMeta?.annotations['storageclass.kubernetes.io/is-default-class'] === 'true'" color="blue">
                  默认
                </a-tag>
              </span>
            </template>

            <template slot="provisioner" slot-scope="text, record">
              <span class="provisioner-cell" :title="record?.provisioner || '-'">
                {{ record?.provisioner || '-' }}
              </span>
            </template>

            <template slot="parameters" slot-scope="text, record">
              <div v-if="record.parameters && Object.keys(record.parameters).length > 0" class="parameters-container">
                <a-tag 
                  v-for="(value, key) in record.parameters"
                  :key="key" 
                  color="green"
                  class="parameter-tag"
                  :title="`${key}: ${value}`"
                >
                  {{ key }}: {{ value }}
                </a-tag>
              </div>
              <span v-else class="empty-value">-</span>
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
  name: 'StorageClassesDashboard',
  props: {
    clusterId: {
      type: Number,
      required: true
    }
  },
  data() {
    return {
      storageClasses: [],
      loading: false,
      columns: [
        {
          title: '名称',
          key: 'name',
          className: 'name-column',
          scopedSlots: { customRender: 'name' }
        },
        {
          title: '提供者',
          key: 'provisioner',
          className: 'provisioner-column',
          scopedSlots: { customRender: 'provisioner' }
        },
        {
          title: '参数',
          key: 'parameters',
          className: 'parameters-column',
          scopedSlots: { customRender: 'parameters' }
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
    this.fetchStorageClasses();
  },
  methods: {
    async fetchStorageClasses() {
      this.loading = true;
      try {
        const res = await this.$axiosGet(global.API.getK8sStorageClasses, {
          clusterId: this.clusterId
        });
        
        if (res.code === 200) {
          // 确保获取StorageClasses列表数组
          let storageClassesList = res.data && res.data.items ? res.data.items : [];
          
          // 处理数据，确保每个项都有必要的属性
          this.storageClasses = storageClassesList.map(storageClass => {
            // 如果为null或undefined，返回一个空对象
            if (!storageClass) return { objectMeta: {}, parameters: {} };
            
            // 确保objectMeta存在
            if (!storageClass.objectMeta) storageClass.objectMeta = {};
            
            // 确保parameters存在
            if (!storageClass.parameters) storageClass.parameters = {};
            
            return storageClass;
          });
          
          console.log("处理后的storageClasses数据:", this.storageClasses);
        } else {
          console.error('Failed to fetch StorageClasses:', res.msg);
          this.storageClasses = [];
        }
      } catch (error) {
        console.error('Error fetching StorageClasses:', error);
        this.storageClasses = [];
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
    clusterId() {
      this.fetchStorageClasses();
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

.provisioner-cell {
  word-break: break-word;
  line-height: 1.5;
  display: block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
}

.parameters-container {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  max-width: 100%;
  
  .parameter-tag {
    margin-right: 0;
    word-break: break-word;
    max-width: 100%;
  }
}
</style> 