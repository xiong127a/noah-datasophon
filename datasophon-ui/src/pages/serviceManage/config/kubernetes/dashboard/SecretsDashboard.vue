<template>
  <div class="resource-list">
    <!-- Secrets列表区域 -->
    <div class="k8s-dashboard-card k8s-resource-card">
      <div class="k8s-card-header">
        <span class="k8s-card-title">Secrets</span>
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
            :dataSource="secrets"
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
                <div class="name-cell">
                  <span class="secret-name" :title="record.objectMeta?.name || '-'">
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
              <span class="type-cell">{{ record.type || '-' }}</span>
            </template>

            <template slot="creationTime" slot-scope="text, record">
              <span class="time-cell" :title="formatTime(record.objectMeta?.creationTimestamp)">
                {{ getDaysAgo(record.objectMeta?.creationTimestamp) }}
              </span>
            </template>
          </a-table>
          
          <!-- 添加分页器 -->
          <div class="pagination-container" v-if="totalItems > 0">
            <a-pagination
              v-if="totalPages > 1"
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
export default {
  name: 'SecretsDashboard',
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
      secrets: [],
      expandedLabels: {}, // 存储每个Secret的标签展开状态
      // 分页相关数据
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
          scopedSlots: { customRender: 'labels' }
        },
        {
          title: '类别',
          dataIndex: 'type',
          key: 'type',
          className: 'type-column',
          scopedSlots: { customRender: 'type' },
          width: '150px'
        },
        {
          title: '创建时间',
          dataIndex: 'creationTime',
          key: 'creationTime',
          className: 'time-column',
          scopedSlots: { customRender: 'creationTime' },
          width: '120px'
        }
      ]
    };
  },
  mounted() {
    this.fetchSecrets();
  },
  watch: {
    selectedNamespace() {
      this.pageNum = 1; // 重置到第一页
      this.fetchSecrets();
    },
    clusterId() {
      this.pageNum = 1; // 重置到第一页
      this.fetchSecrets();
    }
  },
  methods: {
    rowKey(record) {
      return record && record.objectMeta ? 
        (record.objectMeta.uid || record.objectMeta.name) : 
        (record.name || Math.random().toString(36).substring(2));
    },
    async fetchSecrets() {
      this.loading = true;
      console.log('获取Secrets，命名空间:', this.selectedNamespace);
      
      try {
        const params = { 
          clusterId: this.clusterId,
          // 仅当命名空间不为'all'时添加命名空间参数
          ...(this.selectedNamespace !== 'all' && { namespace: this.selectedNamespace }),
          // 添加分页参数
          pageNum: this.pageNum,
          pageSize: this.pageSize
        };
        
        // 使用全局API对象中定义的getK8sSecrets接口
        const res = await this.$axiosGet(global.API.getK8sSecrets, params);

        // 打印API返回的原始数据，用于调试
        console.log('API返回的原始数据:', JSON.stringify(res, null, 2));

        if (res.code === 200 && res.data) { 
          // 处理数据
          this.secrets = res.data.secrets || [];
          
          // 设置分页相关数据
          this.totalItems = res.data.total || this.secrets.length;
          this.totalPages = res.data.totalPages || 1;
          
          console.log("处理后的Secrets数据:", this.secrets);
          console.log("分页信息:", { 
            pageNum: this.pageNum, 
            pageSize: this.pageSize, 
            totalItems: this.totalItems, 
            totalPages: this.totalPages 
          });
          
          // 确保每个Secret对象都有必要的属性
          this.secrets = this.secrets.map(secret => {
            return {
              ...secret,
              // 确保必要字段存在
              objectMeta: secret.objectMeta || { 
                name: 'Unknown', 
                namespace: 'Unknown', 
                labels: {}, 
                annotations: {}, 
                creationTimestamp: null, 
                uid: null 
              },
              // 确保typeMeta存在并正确设置kind为secret
              typeMeta: secret.typeMeta || { kind: 'secret' }
            };
          });
        } else {
          console.error('获取Secrets失败:', res ? res.msg : '未知错误');
          this.secrets = [];
          this.totalItems = 0;
          this.totalPages = 1;
        }
      } catch (error) {
        console.error('获取Secrets异常:', error);
        console.error('获取Secrets列表失败');
        this.secrets = [];
        this.totalItems = 0;
        this.totalPages = 1;
      } finally {
        this.loading = false;
      }
    },
    
    // 分页事件处理方法
    onPageChange(page) {
      this.pageNum = page;
      this.fetchSecrets();
    },
    
    onShowSizeChange(current, size) {
      this.pageNum = 1; // 重置到第一页
      this.pageSize = size;
      this.fetchSecrets();
    },
    
    // 标签展开相关方法
    isLabelsExpanded(record) {
      const recordId = record.objectMeta?.uid || 
                       record.objectMeta?.name || 
                       JSON.stringify(record.objectMeta?.labels);
      return this.expandedLabels[recordId] === true;
    },
    
    toggleLabelsExpand(record) {
      const recordId = record.objectMeta?.uid || 
                       record.objectMeta?.name || 
                       JSON.stringify(record.objectMeta?.labels);
      
      // 使用Vue的响应式API更新
      this.$set(this.expandedLabels, recordId, !this.expandedLabels[recordId]);
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
    }
  }
};
</script>

<style lang="less" scoped>
@import './styles/k8s-table-styles.less';

.resource-list {
  margin-bottom: 16px;
}

.k8s-dashboard-card {
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  margin-bottom: 16px;
  overflow: hidden;
}

.k8s-card-header {
  align-items: center;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  font-weight: 500;
  justify-content: space-between;
  padding: 12px 16px;
}

.k8s-card-title {
  font-size: 16px;
}

.k8s-card-actions {
  display: flex;
  gap: 8px;
}

.k8s-card-content {
  padding: 16px;
}

.k8s-action-icon {
  cursor: pointer;
  font-size: 14px;
  margin-right: 8px;
}

.k8s-card-collapse-icon {
  cursor: pointer;
}

.k8s-table {
  margin-bottom: 0;
}

.name-cell {
  display: flex;
  align-items: center;
}

.secret-name {
  color: #1890ff;
  cursor: pointer;
  display: inline-block;
  margin-left: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.secret-name:hover {
  color: #40a9ff;
  text-decoration: underline;
}

.labels-container {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.label-tag {
  margin: 0;
  max-width: 250px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.time-cell {
  display: inline-block;
  width: 100%;
}

/* 分页容器样式 */
.pagination-container {
  margin-top: 16px;
  text-align: right;
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
</style> 