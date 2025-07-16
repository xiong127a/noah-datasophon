<template>
  <div class="resource-list">
    <!-- Deployments列表区域 -->
    <div class="kubernetes-dashboard-card kubernetes-resource-card">
      <div class="kubernetes-card-header">
        <span class="kubernetes-card-title">Deployments</span>
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
              :columns="deploymentColumns"
              :dataSource="deployments"
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
              :rowKey="getRowKey"
              class="kubernetes-table"
              :table-layout="'auto'"
              :bordered="false"
              size="middle"
              @change="handleTableChange"
          >
            <template slot="name" slot-scope="text, record">
              <div style="display: flex; align-items: center; line-height: normal;">
                <StatusIndicator :resource="record" resourceType="deployment" />
                <div class="name-cell">
                  <span class="pod-name" :title="record && record.objectMeta && record.objectMeta.name ? record.objectMeta.name : '未知'">
                    {{ record && record.objectMeta && record.objectMeta.name ? record.objectMeta.name : '未知' }}
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
                <span>{{ record && record.pods && record.pods.running !== undefined ? record.pods.running : 0 }} / {{ record && record.pods && record.pods.desired !== undefined ? record.pods.desired : 0 }}</span>
              </div>
            </template>

            <template slot="creationTime" slot-scope="text, record">
              <span class="time-cell" :title="formatTime(record && record.objectMeta ? record.objectMeta.creationTimestamp : null)">
                {{ getDaysAgo(record && record.objectMeta ? record.objectMeta.creationTimestamp : null) }}
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
  name: 'DeploymentDashboard',
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
      deployments: [], // 当前页显示的数据
      loading: false,
      expandedLabels: {},
      totalPages: 1, // 添加总页数字段
      // 分页配置
      pagination: {
        current: 1,
        pageSize: 10,
        total: 0,
        showSizeChanger: true,
        showQuickJumper: true,
        pageSizeOptions: ['5', '10', '20', '50']
      },
      deploymentColumns: [
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
          dataIndex: 'pods',
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
  computed: {
    getRowKey(record) {
      const namespace = record && record.objectMeta && record.objectMeta.namespace ? record.objectMeta.namespace : 'unknown';
      const name = record && record.objectMeta && record.objectMeta.name ? record.objectMeta.name : 'unknown';
      return `${namespace}-${name}`;
    }
  },
  mounted() {
    this.fetchDeployments();
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
    async fetchDeployments() {
      this.loading = true;
      try {
        // 使用后端分页API
        const res = await this.$axiosGet(global.API.getKubernetesDeployments, {
          clusterId: this.clusterId,
          serviceId: this.serviceId,
          namespace: this.selectedNamespace === 'all' ? null : this.selectedNamespace,
          pageNum: this.pagination.current,
          pageSize: this.pagination.pageSize
        });
        
        if (res.code === 200) {
          // 处理返回的当前页数据
          this.deployments = (res.data && res.data.deployments ? res.data.deployments : []).map(deploy => {
            if (!deploy) return { objectMeta: {}, pods: {} };
            if (!deploy.objectMeta) deploy.objectMeta = {};
            if (!deploy.pods) deploy.pods = {};
            return deploy;
          });
          
          // 更新分页信息（使用后端返回的总数和总页数）
          this.pagination.total = res.data.total || 0;
          this.totalPages = res.data.totalPages || 1; // 使用后端返回的总页数
          
          // 添加调试日志
          console.log("获取的deployments总数:", this.pagination.total);
          console.log("总页数:", this.totalPages);
          console.log("是否应该隐藏分页:", this.totalPages <= 1);
          
          // 强制更新视图
          this.$forceUpdate();
        } else {
          console.error('Failed to fetch deployments:', res.msg);
          this.deployments = [];
          this.pagination.total = 0;
          this.totalPages = 1;
        }
      } catch (error) {
        console.error('Error fetching deployments:', error);
        this.deployments = [];
        this.pagination.total = 0;
        this.totalPages = 1;
      } finally {
        this.loading = false;
      }
    },
    
    // 处理表格分页变化
    handleTableChange(pagination, filters, sorter) {
      this.pagination.current = pagination.current;
      this.pagination.pageSize = pagination.pageSize;
      
      // 调用API获取新页数据
      this.fetchDeployments();
    },
    handleEditDeployment(record) {
      this.$message.info(`编辑Deployment ${record.name} 的功能正在开发中`);
    },
    getDaysAgo(timestamp) {
      if (!timestamp) return '-';

      const date = new Date(timestamp);
      const now = new Date();

      const timeDiff = Math.abs(now - date);

      const days = Math.floor(timeDiff / (1000 * 60 * 60 * 24));
      const hours = Math.floor((timeDiff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
      const minutes = Math.floor((timeDiff % (1000 * 60 * 60)) / (1000 * 60));

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
      this.fetchDeployments();
    },
    clusterId() {
      this.fetchDeployments();
    },
    serviceId() {
      this.fetchDeployments();
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

/deep/ .ant-table-tbody > tr > td {
  white-space: normal !important;
  word-break: break-word !important;
}
</style> 