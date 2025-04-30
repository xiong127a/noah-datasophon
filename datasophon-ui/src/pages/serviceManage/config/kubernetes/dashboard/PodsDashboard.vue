<template>
  <div class="resource-list">
    <!-- Pods列表区域 -->
    <div class="k8s-dashboard-card k8s-resource-card">
      <div class="k8s-card-header">
        <span class="k8s-card-title">Pods</span>
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
            :dataSource="pods"
            :rowKey="rowKey"
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
            class="k8s-table"
            :table-layout="'auto'"
            :bordered="false"
            :zebra-stripes="false"
            size="middle"
            @change="handleTableChange"
          >
            <template slot="name" slot-scope="text, record">
              <div style="display: flex; align-items: center; line-height: normal;">
                <StatusIndicator :resource="record" resourceType="pod" />
                <div class="name-cell">
                  <span class="pod-name" :title="record.objectMeta?.name || '-'">
                    {{ record.objectMeta?.name || '-' }}
                  </span>
                </div>
              </div>
            </template>
            
            <template slot="image" slot-scope="text, record">
                <div class="image-cell" :title="record.containerImages && record.containerImages.length ? record.containerImages.join(', ') : ''">
                  <template v-if="record.containerImages && record.containerImages.length">
                    <span class="container-image">
                      {{ record.containerImages[0] }}
                    </span>
                    <span v-if="record.containerImages.length > 1">+{{ record.containerImages.length - 1 }}</span>
                  </template>
                <span v-else class="empty-value">-</span>
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
            
            <template slot="cpuUsage" slot-scope="text, record">
              <div class="resource-value">
                <img src="@/assets/images/cpu.svg" class="resource-icon" alt="CPU" />
                <span class="resource-text">{{ formatCpuUsage(record) }}</span>
                </div>
              </template>
              
            <template slot="memoryUsage" slot-scope="text, record">
              <div class="resource-value">
                <img src="@/assets/images/memory.svg" class="resource-icon" alt="Memory" />
                <span class="resource-text">{{ formatMemoryUsage(record) }}</span>
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
import { mapState } from 'vuex'
import moment from 'moment'
import dayjs from 'dayjs'
import { Transfer, Tag, Modal } from 'ant-design-vue'
import StatusIndicator from './components/StatusIndicator.vue'

export default {
  name: 'PodsDashboard',
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
      loading: false,
      pods: [],
      expandedLabels: {}, // 存储每个Pod的标签展开状态
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
          title: '镜像',
          dataIndex: 'image',
          key: 'image',
          className: 'image-column',
          scopedSlots: { customRender: 'image' },
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
          title: '节点',
          dataIndex: 'nodeName',
          key: 'node',
          className: 'node-column',
        },
        {
          title: '重启',
          dataIndex: 'restartCount',
          key: 'restarts',
          width: '80px',
          className: 'restart-column',
        },
        {
          title: 'CPU 使用率 (cores)',
          dataIndex: 'cpuUsage',
          key: 'cpuUsage',
          scopedSlots: { customRender: 'cpuUsage' },
        },
        {
          title: '内存使用 (bytes)',
          dataIndex: 'memoryUsage',
          key: 'memoryUsage',
          scopedSlots: { customRender: 'memoryUsage' },
        },
        {
          title: '创建时间',
          dataIndex: 'creationTime',
          key: 'creationTime',
          className: 'time-column',
          scopedSlots: { customRender: 'creationTime' },
        }
      ],
    };
  },
  mounted() {
    this.fetchPods();
  },
  watch: {
    selectedNamespace() {
      this.fetchPods();
    },
    clusterId() {
      this.fetchPods();
    },
    serviceId() {
      this.fetchPods();
    }
  },
  methods: {
    rowKey(record) {
      return record && record.objectMeta ? 
        (record.objectMeta.uid || record.objectMeta.name) : 
        (record.name || Math.random().toString(36).substring(2));
    },
    async fetchPods() {
      this.loading = true;
      console.log('Fetching pods for namespace:', this.selectedNamespace);
      
      try {
        const params = { 
            clusterId: this.clusterId,
            serviceId: this.serviceId,
            // Only add namespace if it's not 'all'
            ...(this.selectedNamespace !== 'all' && { namespace: this.selectedNamespace }),
            pageNum: this.pagination.current,
            pageSize: this.pagination.pageSize
        };
        
        // 使用全局API对象中定义的getK8sPods接口
        const res = await this.$axiosGet(global.API.getK8sPods, params);

        // 打印API返回的原始数据，用于调试
        console.log('API返回的原始数据:', JSON.stringify(res, null, 2));

        if (res.code === 200 && res.data) { 
          // 检查数据结构
          let podsList = [];
          
          if (Array.isArray(res.data.pods)) {
            // 标准格式
            podsList = res.data.pods;
          } else if (Array.isArray(res.data)) {
            // 直接是数组的情况
            podsList = res.data;
          } else if (typeof res.data === 'object' && res.data !== null) {
            // 对象格式，尝试找出pods数组
            Object.keys(res.data).forEach(key => {
              if (Array.isArray(res.data[key]) && key.toLowerCase().includes('pod')) {
                podsList = res.data[key];
              }
            });
          }
          
          console.log('提取的Pods列表:', podsList);
          
          // 根据新的API响应格式处理数据
          this.pods = podsList.map(pod => {
            // 打印单个Pod的原始数据
            console.log('处理Pod数据:', pod);
            
            return {
              ...pod,
              // 确保必要字段存在，避免渲染错误
              objectMeta: pod.objectMeta || { name: 'Unknown', namespace: 'Unknown', labels: {}, annotations: {}, creationTimestamp: null, uid: null },
              status: pod.status || 'Unknown',
              nodeName: pod.nodeName || '-',
              restartCount: pod.restartCount || 0,
              // 正确映射 metrics 数据
              metrics: {
                cpuUsage: pod.metrics && pod.metrics.cpuUsage !== undefined ? pod.metrics.cpuUsage : null,
                memoryUsage: pod.metrics && pod.metrics.memoryUsage !== undefined ? pod.metrics.memoryUsage : null
              },
              // 容器镜像信息
              containerImages: pod.containerImages ? pod.containerImages : []
            };
          });
          
          // 更新分页信息
          this.pagination.total = res.data.total || 0;
          this.totalPages = res.data.totalPages || 1;
          
          // 添加调试日志
          console.log("获取的pods总数:", this.pagination.total);
          console.log("总页数:", this.totalPages);
          console.log("是否应该隐藏分页:", this.totalPages <= 1);
          
          // 如果API返回了状态统计信息，更新这些信息
          if (res.data.status) {
            this.podStatus = res.data.status;
          }
          
          console.log('处理后的pods数据:', this.pods);
        } else {
          console.error('Failed to get Pod list:', res && res.msg ? res.msg : 'Unknown error');
          this.pods = []; // Clear data on failure
          this.pagination.total = 0;
          this.totalPages = 1;
          // 使用项目的消息提示替代ArcoDesign的Message
          this.$message.error(res && res.msg ? res.msg : 'Failed to get Pod list');
        }
      } catch (error) {
        console.error('Exception while getting Pod list:', error);
        // 使用项目的消息提示
        this.$message.error('Exception while getting Pod list');
        this.pods = []; // Clear data on error
        this.pagination.total = 0;
        this.totalPages = 1;
      } finally {
        this.loading = false;
      }
    },
    formatBytes(bytes, decimals = 2) {
      if (bytes === 0) return '0 Bytes';
      const k = 1024;
      const dm = decimals < 0 ? 0 : decimals;
      const sizes = ['Bytes', 'KiB', 'MiB', 'GiB', 'TiB', 'PiB', 'EiB', 'ZiB', 'YiB'];
      const i = Math.floor(Math.log(bytes) / Math.log(k));
      return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
    },
    formatTime(time) {
      if (!time) return '-';
      return new Date(time).toLocaleString('en-US', {
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
    formatCreationTime(record) {
      const timestamp = record && record.objectMeta && record.objectMeta.creationTimestamp;
      if (!timestamp) return '-';
      const days = this.getDaysAgo(timestamp);
      return `${days} days ago`;
    },
    viewYaml(record) {
      const podName = record && record.objectMeta && record.objectMeta.name ? record.objectMeta.name : 'N/A';
      // 使用项目的消息提示
      this.$message.info(`View YAML for Pod ${podName}`);
      console.log('View YAML for:', record);
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
    handleDetail(record) {
      const podName = record && record.name ? record.name : 'N/A';
      this.$message.info(`View details for Pod ${podName}`);
      // Here you can implement the detailed view logic
      // For example, you could navigate to a detail page or open a modal
      this.viewYaml(record);
    },
    getLabelClass(key) {
      key = key.toLowerCase()
      if (key.includes('app') || key.includes('component')) return 'app-label'
      if (key.includes('tier') || key.includes('layer')) return 'tier-label'
      if (key.includes('env') || key.includes('environment')) return 'environment-label'
      if (key.includes('version') || key.includes('release')) return 'version-label'
      return ''
    },
    formatCpuUsage(record) {
      if (!record.metrics || record.metrics.cpuUsage === undefined || record.metrics.cpuUsage === null) return '-';
      const cpuMillis = parseFloat(record.metrics.cpuUsage);
      if (isNaN(cpuMillis)) return '-';
      
      // 保留小数表示，Kubernetes中CPU可以分配小数核心
      const cores = cpuMillis / 1000;
      return `${cores.toFixed(3)} cores`;
    },
    formatMemoryUsage(record) {
      if (!record.metrics || record.metrics.memoryUsage === undefined || record.metrics.memoryUsage === null) return '-';
      const memoryBytes = parseFloat(record.metrics.memoryUsage);
      if (isNaN(memoryBytes)) return '-';
      
      return this.formatBytes(memoryBytes);
    },
    handleTableChange(pagination) {
      this.pagination.current = pagination.current;
      this.pagination.pageSize = pagination.pageSize;
      this.pagination.total = pagination.total;
      this.totalPages = Math.ceil(this.pagination.total / this.pagination.pageSize);
      this.fetchPods();
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
  
.pod-name {
  cursor: pointer;
    display: inline-block;
    max-width: 100%;
    
  &:hover {
    color: #1890ff;
    text-decoration: underline;
  }
  }
}

// 镜像单元格样式
.image-cell {
  white-space: normal;
  word-break: break-word;
  line-height: 1.5;
  padding: 4px 0;
}

.container-image {
  word-break: break-word;
  display: inline-block;
  max-width: 100%;
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

// 资源值显示样式
.resource-value {
  font-size: 14px;
  padding: 4px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.resource-icon {
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.resource-text {
  color: #333333;
  font-weight: 500;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  letter-spacing: 0.2px;
}

/* 覆盖KubernetesDashboard.vue中的样式 */
:deep(.ant-table-tbody > tr > td) {
  white-space: normal !important;
  word-break: break-word !important;
}

/* 节点列不换行 */
:deep(.ant-table-tbody > tr > td.node-column) {
  white-space: nowrap !important;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 重启列居中 */
:deep(.ant-table-tbody > tr > td.restart-column) {
  text-align: center !important;
}
</style>