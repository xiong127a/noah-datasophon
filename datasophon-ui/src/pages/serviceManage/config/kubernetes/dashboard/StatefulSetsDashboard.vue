<template>
  <div class="resource-list">
    <!-- StatefulSets列表区域 -->
    <div class="k8s-dashboard-card k8s-resource-card">
      <div class="k8s-card-header">
        <span class="k8s-card-title">StatefulSets</span>
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
            :dataSource="statefulSets"
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
                <StatusIndicator :resource="record" resourceType="statefulset" />
                <div class="name-cell">
                  <span class="statefulset-name" :title="record.objectMeta?.name || '-'">
                    {{ record.objectMeta?.name || '-' }}
                  </span>
                </div>
              </div>
            </template>
            
            <template slot="images" slot-scope="text, record">
              <div v-if="record.containerImages && record.containerImages.length > 0" class="images-container">
                <div v-for="(image, idx) in record.containerImages" :key="idx" class="image-item">
                  <a-tooltip :title="image">
                    <span class="image-text">{{ getShortImageName(image) }}</span>
                  </a-tooltip>
                </div>
                <div v-if="record.initContainerImages && record.initContainerImages.length > 0" class="init-container-header">
                  Init容器:
                </div>
                <div v-for="(image, idx) in record.initContainerImages" :key="`init-${idx}`" class="image-item init-container">
                  <a-tooltip :title="image">
                    <span class="image-text">{{ getShortImageName(image) }}</span>
                  </a-tooltip>
                </div>
              </div>
              <span v-else class="empty-value">-</span>
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
            
            <template slot="pods" slot-scope="text, record">
              <div v-if="record.podInfo" class="pods-container">
                <div class="pod-status">
                  <div class="pod-count">
                    <span class="current">{{ record.podInfo.current || 0 }}</span>
                    <span class="separator">/</span>
                    <span class="desired">{{ record.podInfo.desired || 0 }}</span>
                  </div>
                  <div class="pod-details">
                    <a-tag v-if="record.podInfo.running > 0" color="green" class="pod-tag">
                      {{ record.podInfo.running }} 运行中
                    </a-tag>
                    <a-tag v-if="record.podInfo.pending > 0" color="orange" class="pod-tag">
                      {{ record.podInfo.pending }} 等待中
                    </a-tag>
                    <a-tag v-if="record.podInfo.failed > 0" color="red" class="pod-tag">
                      {{ record.podInfo.failed }} 失败
                    </a-tag>
                    <a-tag v-if="record.podInfo.succeeded > 0" color="blue" class="pod-tag">
                      {{ record.podInfo.succeeded }} 成功
                    </a-tag>
                  </div>
                </div>
                <div v-if="record.podInfo.warnings && record.podInfo.warnings.length > 0" class="pod-warnings">
                  <a-tooltip v-for="(warning, idx) in record.podInfo.warnings.slice(0, 2)" :key="idx" :title="warning.message">
                    <a-icon type="warning" theme="filled" class="warning-icon" />
                  </a-tooltip>
                  <a-tooltip v-if="record.podInfo.warnings.length > 2" :title="`还有 ${record.podInfo.warnings.length - 2} 个警告`">
                    <span class="more-warnings">+{{ record.podInfo.warnings.length - 2 }}</span>
                  </a-tooltip>
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
  name: 'StatefulSetsDashboard',
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
      statefulSets: [],
      totalItems: 0,
      totalPages: 0,
      pageNum: 1,
      pageSize: 10,
      expandedLabels: {}, // 存储每个StatefulSet的标签展开状态
      columns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          className: 'name-column',
          scopedSlots: { customRender: 'name' },
          width: '180px'
        },
        {
          title: '镜像',
          dataIndex: 'images',
          key: 'images',
          className: 'images-column',
          scopedSlots: { customRender: 'images' },
          width: '250px'
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          className: 'labels-column',
          scopedSlots: { customRender: 'labels' }
        },
        {
          title: 'Pods',
          dataIndex: 'pods',
          key: 'pods',
          className: 'pods-column',
          scopedSlots: { customRender: 'pods' },
          width: '160px'
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
    this.fetchStatefulSets();
  },
  watch: {
    selectedNamespace() {
      this.pageNum = 1; // 切换命名空间时重置页码
      this.fetchStatefulSets();
    },
    clusterId() {
      this.pageNum = 1; // 切换集群时重置页码
      this.fetchStatefulSets();
    }
  },
  methods: {
    rowKey(record) {
      return record && record.objectMeta ? 
        (record.objectMeta.uid || record.objectMeta.name) : 
        (record.name || Math.random().toString(36).substring(2));
    },
    async fetchStatefulSets() {
      this.loading = true;
      console.log('获取StatefulSets，命名空间:', this.selectedNamespace, '页码:', this.pageNum, '每页大小:', this.pageSize);
      
      try {
        const params = { 
          clusterId: this.clusterId,
          // 仅当命名空间不为'all'时添加命名空间参数
          ...(this.selectedNamespace !== 'all' && { namespace: this.selectedNamespace }),
          pageNum: this.pageNum,
          pageSize: this.pageSize
        };
        
        // 使用全局API对象中定义的getK8sStatefulSets接口
        const res = await this.$axiosGet(global.API.getK8sStatefulSets, params);

        // 打印API返回的原始数据，用于调试
        console.log('API返回的原始数据:', JSON.stringify(res, null, 2));

        if (res.code === 200 && res.data) { 
          // 处理数据
          this.statefulSets = res.data.statefulSets || [];
          this.totalItems = res.data.total || 0;
          this.totalPages = res.data.totalPages || 0;
          
          // 确保每个StatefulSet对象都有必要的属性
          this.statefulSets = this.statefulSets.map(statefulSet => {
            return {
              ...statefulSet,
              // 确保必要字段存在
              objectMeta: statefulSet.objectMeta || { 
                name: 'Unknown', 
                namespace: 'Unknown', 
                labels: {}, 
                annotations: {}, 
                creationTimestamp: null, 
                uid: null 
              },
              containerImages: statefulSet.containerImages || [],
              initContainerImages: statefulSet.initContainerImages || [],
              // 确保podInfo存在
              podInfo: statefulSet.podInfo || {
                current: 0,
                desired: 0,
                running: 0,
                pending: 0,
                failed: 0,
                succeeded: 0,
                warnings: []
              },
              // 确保typeMeta存在并正确设置kind为statefulset
              typeMeta: {
                ...statefulSet.typeMeta,
                kind: 'statefulset'
              }
            };
          });
          
          console.log('处理后的statefulSets数据:', this.statefulSets);
        } else {
          console.error(res && res.msg ? res.msg : '获取StatefulSets列表失败');
          this.statefulSets = []; // 失败时清空数据
          this.totalItems = 0;
          this.totalPages = 0;
        }
      } catch (error) {
        console.error('获取StatefulSets列表异常:', error);
        this.statefulSets = []; // 出错时清空数据
        this.totalItems = 0;
        this.totalPages = 0;
      } finally {
        this.loading = false;
      }
    },
    // 页码改变事件处理
    onPageChange(page) {
      this.pageNum = page;
      this.fetchStatefulSets();
    },
    // 页面大小改变事件处理
    onShowSizeChange(current, size) {
      this.pageNum = 1; // 重置到第一页
      this.pageSize = size;
      this.fetchStatefulSets();
    },
    getShortImageName(image) {
      if (!image) return '';
      
      // 提取镜像名称的简短版本
      // 示例：harbor.norintech.com/aip/kubeflownotebookswg/jupyter-scipy:v1.8.0 -> jupyter-scipy:v1.8.0
      const parts = image.split('/');
      return parts.length > 0 ? parts[parts.length - 1] : image;
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
  
  .statefulset-name {
    cursor: pointer;
    display: inline-block;
    max-width: 100%;
    
    &:hover {
      color: #1890ff;
      text-decoration: underline;
    }
  }
}

// 镜像容器样式
.images-container {
  display: flex;
  flex-direction: column;
  gap: 4px;
  
  .image-item {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    font-family: monospace;
    font-size: 12px;
    padding: 1px 0;
    
    &.init-container {
      opacity: 0.8;
      font-style: italic;
    }
  }
  
  .init-container-header {
    font-size: 12px;
    color: #666;
    margin-top: 4px;
    font-weight: bold;
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

// Pods容器样式
.pods-container {
  display: flex;
  flex-direction: column;
  gap: 4px;
  
  .pod-status {
    display: flex;
    flex-direction: column;
    gap: 4px;
    
    .pod-count {
      display: flex;
      align-items: center;
      gap: 2px;
      font-weight: bold;
      
      .current {
        color: #1890ff;
      }
      
      .separator {
        color: #999;
        margin: 0 2px;
      }
      
      .desired {
        color: #666;
      }
    }
    
    .pod-details {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
      
      .pod-tag {
        margin-right: 0;
        font-size: 11px;
        line-height: 16px;
        height: 18px;
      }
    }
  }
  
  .pod-warnings {
    display: flex;
    align-items: center;
    gap: 4px;
    margin-top: 4px;
    
    .warning-icon {
      color: #faad14;
      font-size: 14px;
    }
    
    .more-warnings {
      font-size: 11px;
      color: #faad14;
      background-color: #fffbe6;
      border: 1px solid #ffe58f;
      padding: 0 4px;
      border-radius: 10px;
    }
  }
}

// 分页器容器样式
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