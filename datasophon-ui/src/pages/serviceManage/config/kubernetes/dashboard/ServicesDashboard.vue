<template>
  <div class="resource-list">
    <!-- Services列表区域 -->
    <div class="k8s-dashboard-card k8s-resource-card">
      <div class="k8s-card-header">
        <span class="k8s-card-title">Services</span>
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
            :columns="serviceColumns"
            :dataSource="services"
            :pagination="false"
            :rowKey="record => record.objectMeta?.uid || `${record.objectMeta?.namespace || '_'}-${record.objectMeta?.name || '_'}-${Math.random().toString(36).substring(2, 10)}`"
            class="k8s-table"
            :bordered="false"
            :table-layout="'auto'"
          >
            <template slot="name" slot-scope="text, record">
              <div style="display: flex; align-items: center; line-height: normal;">
                <span class="status-dot status-running"></span>
                <div class="name-cell">
                  <span class="pod-name" :title="record?.objectMeta?.name || '未知'">
                    {{ record?.objectMeta?.name || '未知' }}
                  </span>
                </div>
              </div>
            </template>
            
            <template slot="labels" slot-scope="text, record">
              <div v-if="record.objectMeta?.labels && Object.keys(record.objectMeta.labels).length > 0" class="labels-container">
                <template v-if="!isLabelsExpanded(record)">
                  <a-tag 
                    v-for="(entry, idx) in Object.entries(record.objectMeta.labels).slice(0, 3)"
                    :key="`${record.objectMeta?.uid || Math.random().toString(36).substring(2, 10)}-label-${entry[0]}-${idx}`" 
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
                    :key="`${record.objectMeta?.uid || Math.random().toString(36).substring(2, 10)}-label-expanded-${entry[0]}-${idx}`" 
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
            
            <template slot="type" slot-scope="text, record">
              <span class="type-cell" :title="record.type || 'NodePort'">
                {{ record.type || 'NodePort' }}
              </span>
            </template>
            
            <template slot="clusterIP" slot-scope="text, record">
              <span class="ip-cell" :title="record.clusterIP || '-'">
                {{ record.clusterIP || '-' }}
              </span>
            </template>
            
            <template slot="internalEndpoints" slot-scope="text, record">
              <div v-if="record.internalEndpoint && record.internalEndpoint.ports && record.internalEndpoint.ports.length > 0" class="endpoints-container">
                <div v-for="(port, index) in record.internalEndpoint.ports" :key="`${record.objectMeta?.uid || Math.random().toString(36).substring(2, 10)}-internal-${port.port}-${port.protocol}-${index}`">
                  <div class="internal-endpoint" :title="`${record.internalEndpoint.host}:${port.port} ${port.protocol}`">
                    {{record.internalEndpoint.host}}:{{port.port}} 
                    <a-tag :color="getProtocolColor(port.protocol)" class="protocol-tag">
                      {{port.protocol}}
                    </a-tag>
                  </div>
                  <div v-if="port.nodePort" class="internal-endpoint" :title="`${record.internalEndpoint.host}:${port.nodePort} ${port.protocol}`">
                    {{record.internalEndpoint.host}}:{{port.nodePort}} 
                    <a-tag :color="getProtocolColor(port.protocol)" class="protocol-tag">
                      {{port.protocol}}
                    </a-tag>
                  </div>
                </div>
              </div>
              <span v-else class="empty-value">-</span>
            </template>
            
            <template slot="externalEndpoints" slot-scope="text, record">
              <div v-if="record.externalEndpoints && record.externalEndpoints.length > 0" class="endpoints-container">
                <div v-for="(endpoint, endpointIndex) in record.externalEndpoints" :key="`${record.objectMeta?.uid || Math.random().toString(36).substring(2, 10)}-external-ep-${endpointIndex}`">
                  <template v-if="endpoint.ports && endpoint.ports.length > 0">
                    <div v-for="(port, portIndex) in endpoint.ports" :key="`${record.objectMeta?.uid || Math.random().toString(36).substring(2, 10)}-ext-${endpoint.host}-${port.port || port.nodePort}-${portIndex}`">
                      <a v-if="port.port" :href="`http://${endpoint.host}:${port.port}`" target="_blank" rel="noopener noreferrer" class="external-endpoint" :title="`${endpoint.host}:${port.port}`">
                        {{endpoint.host}}:{{port.port}}
                        <a-icon type="link" class="external-icon" />
                      </a>
                      <a-tag v-if="port.protocol" :color="getProtocolColor(port.protocol)" class="protocol-tag">
                        {{port.protocol}}
                      </a-tag>
                      <a v-if="!port.port && port.nodePort" :href="`http://${endpoint.host}:${port.nodePort}`" target="_blank" rel="noopener noreferrer" class="external-endpoint" :title="`${endpoint.host}:${port.nodePort}`">
                        {{endpoint.host}}:{{port.nodePort}}
                        <a-icon type="link" class="external-icon" />
                      </a>
                      <a-tag v-if="!port.port && port.nodePort && port.protocol" :color="getProtocolColor(port.protocol)" class="protocol-tag">
                        {{port.protocol}}
                      </a-tag>
                    </div>
                  </template>
                  <a v-else :href="`http://${endpoint.host}`" target="_blank" rel="noopener noreferrer" class="external-endpoint" :title="endpoint.host">
                    {{endpoint.host}}
                    <a-icon type="link" class="external-icon" />
                  </a>
                </div>
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
// import API from '@/api';

export default {
  name: 'ServicesDashboard',
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
      services: [],
      serviceTotalItems: 0,
      loading: false,
      expandedLabels: {}, // 新增: 存储标签展开状态
      serviceColumns: [
        {
          title: '名称',
          key: 'name',
          dataIndex: ['objectMeta', 'name'],
          className: 'name-column',
          scopedSlots: { customRender: 'name' },
          width: '150px'
        },
        {
          title: '标签',
          key: 'labels',
          className: 'labels-column',
          scopedSlots: { customRender: 'labels' }
        },
        {
          title: '类型',
          dataIndex: 'type',
          key: 'type',
          className: 'type-column',
          scopedSlots: { customRender: 'type' },
          width: '100px'
        },
        {
          title: '集群 IP',
          dataIndex: 'clusterIP',
          key: 'clusterIP',
          className: 'ip-column',
          scopedSlots: { customRender: 'clusterIP' },
          width: '120px'
        },
        {
          title: '内部 Endpoints',
          key: 'internalEndpoints',
          className: 'endpoints-column',
          scopedSlots: { customRender: 'internalEndpoints' },
          width: '240px'
        },
        {
          title: '外部 Endpoints',
          key: 'externalEndpoints',
          className: 'endpoints-column',
          scopedSlots: { customRender: 'externalEndpoints' },
          width: '240px'
        },
        {
          title: '创建时间',
          key: 'creationTime',
          dataIndex: ['objectMeta', 'creationTimestamp'],
          className: 'time-column',
          scopedSlots: { customRender: 'creationTime' },
          width: '120px'
        }
      ]
    };
  },
  mounted() {
    this.fetchServices();
  },
  methods: {
    // 新增：标签展开/折叠逻辑
    toggleLabelsExpand(record) {
      if (!record || !record.objectMeta || !record.objectMeta.uid) return;
      const uid = record.objectMeta.uid;
      this.$set(this.expandedLabels, uid, !this.expandedLabels[uid]);
    },
    // 新增：判断标签是否展开
    isLabelsExpanded(record) {
      if (!record || !record.objectMeta || !record.objectMeta.uid) return false;
      return !!this.expandedLabels[record.objectMeta.uid];
    },
    async fetchServices() {
      this.loading = true;
      try {
        const res = await this.$axiosGet(global.API.getK8sServices, {
          clusterId: this.clusterId,
          namespace: this.selectedNamespace === 'all' ? null : this.selectedNamespace
        });
        
        if (res.code === 200 && res.data) {
          // 处理服务数据
          this.services = res.data.services || [];
          this.serviceTotalItems = res.data.listMeta?.totalItems || 0;
        } else {
          this.services = [];
          this.serviceTotalItems = 0;
          console.error('获取服务列表失败:', res.msg);
        }
      } catch (error) {
        console.error('获取服务列表失败:', error);
        this.$message.error('获取服务列表失败');
        this.services = [];
        this.serviceTotalItems = 0;
      } finally {
        this.loading = false;
      }
    },
    handleViewService(record) {
      // TODO: 实现查看Service的逻辑
      this.$message.info(`查看Service ${record.name} 的功能正在开发中`);
    },
    handleEditService(record) {
      // TODO: 实现编辑Service的逻辑
      this.$message.info(`编辑Service ${record.name} 的功能正在开发中`);
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
    getStatusColor(record) {
      // 实现根据记录状态返回相应颜色的逻辑
      return 'blue'; // 临时返回，实际实现需要根据实际情况判断
    },
    getStatusText(record) {
      // 实现根据记录状态返回相应文本的逻辑
      return 'Running'; // 临时返回，实际实现需要根据实际情况判断
    },
    viewDeployment(record) {
      // 实现查看Deployment的逻辑
      this.$message.info(`查看Deployment ${record.name} 的功能正在开发中`);
    },
    getProtocolColor(protocol) {
      if (!protocol) return 'default';
      switch(protocol.toUpperCase()) {
        case 'TCP':
          return 'blue';
        case 'UDP':
          return 'green';
        case 'HTTP':
        case 'HTTPS':
          return 'purple';
        case 'SCTP':
          return 'orange';
        default:
          return 'default';
      }
    }
  },
  watch: {
    selectedNamespace() {
      this.fetchServices();
    },
    clusterId() {
      this.fetchServices();
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

.type-cell {
  white-space: nowrap !important;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
}

.ip-cell {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
}

.labels-container {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  
  .label-tag {
    margin-right: 0;
  }
}

.endpoints-container {
  max-width: 100%;
  word-break: break-word;
}

.internal-endpoint, .external-endpoint {
  white-space: normal !important;
  word-break: break-word !important;
  overflow: visible !important;
  text-overflow: clip !important;
  max-width: 100%;
  padding: 2px 0;
  display: block;
  line-height: 1.5;
}

.external-endpoint {
  color: #1890ff;
  text-decoration: none;
  
  &:hover {
    text-decoration: underline;
  }
  
  .external-icon {
    margin-left: 4px;
    font-size: 12px;
  }
}

.protocol-tag {
  margin-left: 4px;
  font-size: 12px;
  line-height: 18px;
  height: 20px;
  vertical-align: middle;
}

/* 覆盖冲突样式 */
:deep(.ant-table-tbody > tr > td) {
  white-space: normal !important;
  word-break: break-word !important;
}
</style> 