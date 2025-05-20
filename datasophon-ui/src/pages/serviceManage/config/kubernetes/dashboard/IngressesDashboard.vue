<template>
  <div class="resource-list">
    <!-- Ingresses列表区域 -->
    <div class="k8s-dashboard-card k8s-resource-card">
      <div class="k8s-card-header">
        <span class="k8s-card-title">Ingresses</span>
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
            :dataSource="ingresses"
            :rowKey="rowKey"
            :pagination="false"
            class="k8s-table"
            :table-layout="'auto'"
            :bordered="false"
            :zebra-stripes="false"
            size="middle"
          >
            <template slot="name" slot-scope="text, record">
              <div style="display: flex; align-items: center; line-height: normal;">
                <StatusIndicator :resource="record" resourceType="ingress" />
                <div class="name-cell">
                  <span class="ingress-name" :title="record.objectMeta?.name || '-'">
                    {{ record.objectMeta?.name || '-' }}
                  </span>
                </div>
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
            
            <template slot="endpoints" slot-scope="text, record">
              <div v-if="record.endpoints && record.endpoints.length" class="endpoints-container">
                <span v-for="(endpoint, idx) in record.endpoints" :key="idx" class="endpoint-item">
                  {{ endpoint.host }}
                </span>
              </div>
              <span v-else class="empty-value">-</span>
            </template>
            
            <template slot="hosts" slot-scope="text, record">
              <div v-if="record.hosts && record.hosts.length" class="hosts-container">
                <a-tag 
                  v-for="(host, idx) in record.hosts" 
                  :key="idx"
                  class="host-tag"
                >
                  {{ host }}
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
          
          <!-- 添加分页器 -->
          <div class="pagination-container">
            <a-pagination
              v-if="totalItems > 0 && totalPages > 1"
              :current="pageNum"
              :pageSize="pageSize"
              :total="totalItems"
              :showTotal="total => `共 ${total} 条记录`"
              :pageSizeOptions="['10', '20', '50', '100']"
              showSizeChanger
              @change="onPageChange"
              @showSizeChange="onShowSizeChange"
            />
          </div>
        </a-spin>
      </div>
    </div>
  </div>
</template>

<script>
import StatusIndicator from './components/StatusIndicator.vue'

export default {
  name: 'IngressesDashboard',
  components: {
    StatusIndicator
  },
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
      loading: false,
      ingresses: [],
      // 添加分页相关数据
      pageNum: 1, // 当前页码
      pageSize: 10, // 每页记录数
      totalItems: 0, // 总记录数
      totalPages: 1, // 总页数
      columns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          className: 'name-column',
          scopedSlots: { customRender: 'name' },
          width: '200px'
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          className: 'labels-column',
          scopedSlots: { customRender: 'labels' },
        },
        {
          title: 'Endpoints',
          dataIndex: 'endpoints',
          key: 'endpoints',
          className: 'endpoints-column',
          scopedSlots: { customRender: 'endpoints' },
        },
        {
          title: 'Hosts',
          dataIndex: 'hosts',
          key: 'hosts',
          className: 'hosts-column',
          scopedSlots: { customRender: 'hosts' },
        },
        {
          title: '创建时间',
          dataIndex: 'creationTime',
          key: 'creationTime',
          className: 'time-column',
          scopedSlots: { customRender: 'creationTime' },
        }
      ],
      expandedLabels: {}, // 存储每个Ingress的标签展开状态
    };
  },
  mounted() {
    this.fetchIngresses();
  },
  watch: {
    selectedNamespace() {
      // 重置分页到第一页
      this.pageNum = 1;
      this.fetchIngresses();
    },
    clusterId() {
      // 重置分页到第一页
      this.pageNum = 1;
      this.fetchIngresses();
    }
  },
  methods: {
    rowKey(record) {
      return record && record.objectMeta ? 
        (record.objectMeta.uid || record.objectMeta.name) : 
        (record.name || Math.random().toString(36).substring(2));
    },
    async fetchIngresses() {
      this.loading = true;
      console.log('Fetching ingresses for namespace:', this.selectedNamespace);
      
      try {
        const params = { 
            clusterId: this.clusterId,
            // Only add namespace if it's not 'all'
            ...(this.selectedNamespace !== 'all' && { namespace: this.selectedNamespace }),
            pageNum: this.pageNum,
            pageSize: this.pageSize
        };
        
        // 使用带分页的API
        const res = await this.$axiosGet(global.API.getK8sIngresses, params);

        // 打印API返回的原始数据，用于调试
        console.log('API返回的原始数据:', JSON.stringify(res, null, 2));

        if (res.code === 200 && res.data) { 
          // 处理数据
          let ingressesList = [];
          
          // 从API响应中提取ingresses列表
          if (Array.isArray(res.data.items)) {
            ingressesList = res.data.items;
          } else if (Array.isArray(res.data)) {
            ingressesList = res.data;
          } else if (typeof res.data === 'object' && res.data !== null) {
            // 尝试在数据对象中查找items数组
            if (res.data.items) {
              ingressesList = res.data.items;
            } else {
              // 查找包含ingress的key
              Object.keys(res.data).forEach(key => {
                if (Array.isArray(res.data[key]) && key.toLowerCase().includes('ingress')) {
                  ingressesList = res.data[key];
                }
              });
            }
          }
          
          console.log('提取的Ingresses列表:', ingressesList);
          
          // 处理ingresses数据
          this.ingresses = ingressesList.map(ingress => {
            console.log('处理Ingress数据:', ingress);
            
            return {
              ...ingress,
              // 确保必要字段存在
              objectMeta: ingress.objectMeta || { 
                name: 'Unknown', 
                namespace: 'Unknown', 
                labels: {}, 
                annotations: {}, 
                creationTimestamp: null, 
                uid: null 
              },
              endpoints: ingress.endpoints || [],
              hosts: ingress.hosts || [],
              // 确保typeMeta存在并正确设置kind为ingress
              typeMeta: {
                ...ingress.typeMeta,
                kind: 'ingress'
              }
            };
          });
          
          // 更新分页数据
          this.totalItems = res.data.total || ingressesList.length;
          this.totalPages = res.data.totalPages || 1;
          
          console.log('处理后的ingresses数据:', this.ingresses);
        } else {
          console.error('Failed to get Ingress list:', res && res.msg ? res.msg : 'Unknown error');
          this.ingresses = []; // Clear data on failure
          this.totalItems = 0;
          this.totalPages = 1;
          console.error(res && res.msg ? res.msg : 'Failed to get Ingress list');
        }
      } catch (error) {
        console.error('Exception while getting Ingress list:', error);
        this.$message.error('Exception while getting Ingress list');
        this.ingresses = []; // Clear data on error
        this.totalItems = 0;
        this.totalPages = 1;
        console.error('Exception while getting Ingress list');
      } finally {
        this.loading = false;
      }
    },
    formatTime(time) {
      if (!time) return '-';
      return new Date(time).toLocaleString('zh-CN', {
        year: 'numeric', month: '2-digit', day: '2-digit',
        hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
      });
    },
    getDaysAgo(timestamp) {
      if (!timestamp) return '-';
      const date = new Date(timestamp);
      const now = new Date();
      const timeDiff = Math.abs(now.getTime() - date.getTime());
      
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
    toggleLabelsExpand(record) {
      if (!record || !record.objectMeta || !record.objectMeta.uid) return;
      const uid = record.objectMeta.uid;
      this.$set(this.expandedLabels, uid, !this.expandedLabels[uid]);
    },
    isLabelsExpanded(record) {
      if (!record || !record.objectMeta || !record.objectMeta.uid) return false;
      return !!this.expandedLabels[record.objectMeta.uid];
    },
    // 分页事件处理方法
    onPageChange(page) {
      this.pageNum = page;
      this.fetchIngresses();
    },
    onShowSizeChange(current, size) {
      this.pageNum = 1; // 重置到第一页
      this.pageSize = size;
      this.fetchIngresses();
    },
  }
}
</script>

<style lang="less" scoped>
@import './styles/k8s-table-styles.less';

// 名称单元格样式
.name-cell {
  white-space: normal;
  word-break: break-word;
  line-height: 1.5;
  padding: 4px 0;
  
  .ingress-name {
    cursor: pointer;
    display: inline-block;
    max-width: 100%;
    
    &:hover {
      color: #1890ff;
      text-decoration: underline;
    }
  }
}

// 标签容器样式
.labels-container {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  
  .label-tag {
    max-width: 100%;
    margin-right: 0;
    white-space: normal;
  }
}

// Endpoints容器样式
.endpoints-container {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  
  .endpoint-item {
    font-family: monospace;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    padding: 2px 8px;
    border-radius: 4px;
    background-color: #e6f7ff;
    border: 1px solid #91d5ff;
    color: #1890ff;
    display: inline-block;
    margin-right: 4px;
    margin-bottom: 4px;
  }
}

// Hosts容器样式
.hosts-container {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  
  .host-tag {
    background-color: #e6f7ff;
    border-color: #91d5ff;
    color: #1890ff;
    max-width: 100%;
    white-space: normal;
    word-break: break-word;
  }
}

/* 覆盖KubernetesDashboard.vue中的样式 */
:deep(.ant-table-tbody > tr > td) {
  white-space: normal !important;
  word-break: break-word !important;
}

/* 空值样式 */
.empty-value {
  color: #999;
  font-style: italic;
}

/* 分页容器样式 */
.pagination-container {
  margin-top: 16px;
  text-align: right;
}
</style> 