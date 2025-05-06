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
      this.fetchSecrets();
    },
    clusterId() {
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
          ...(this.selectedNamespace !== 'all' && { namespace: this.selectedNamespace })
        };
        
        // 使用全局API对象中定义的getK8sSecrets接口
        const res = await this.$axiosGet(global.API.getK8sSecrets, params);

        // 打印API返回的原始数据，用于调试
        console.log('API返回的原始数据:', JSON.stringify(res, null, 2));

        if (res.code === 200 && res.data) { 
          // 处理数据
          this.secrets = res.data.secrets || [];
          
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
              typeMeta: {
                ...secret.typeMeta,
                kind: 'secret'
              },
              type: secret.type || 'Unknown'
            };
          });
          
          console.log('处理后的secrets数据:', this.secrets);
        } else {
          console.error('获取Secrets列表失败:', res && res.msg ? res.msg : '未知错误');
          this.secrets = []; // 失败时清空数据
          this.$message.error(res && res.msg ? res.msg : '获取Secrets列表失败');
        }
      } catch (error) {
        console.error('获取Secrets列表异常:', error);
        this.$message.error('获取Secrets列表异常');
        this.secrets = []; // 出错时清空数据
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
    }
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
  
  .secret-name {
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

// 类型单元格样式
.type-cell {
  font-family: monospace;
  font-size: 12px;
  border-radius: 2px;
  padding: 2px 6px;
  background-color: #f5f5f5;
  color: #666;
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