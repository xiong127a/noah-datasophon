<template>
  <div class="resource-list">
    <!-- 顶部图表区域 (暂时隐藏) -->
    <!--
    <k8s-metrics-charts 
      :metrics-data="metricsData"
      @update-charts="updateCharts"
    />
    -->

    <!-- Deployments列表区域 -->
    <div class="k8s-dashboard-card k8s-resource-card">
      <div class="k8s-card-header">
        <span class="k8s-card-title">Deployments</span>
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
              :columns="deploymentColumns"
              :dataSource="deployments"
              :pagination="false"
              :rowKey="record => `${record?.objectMeta?.namespace || 'unknown'}-${record?.objectMeta?.name || 'unknown'}`"
              class="k8s-table"
              :table-layout="'auto'"
              :bordered="false"
              size="middle"
          >
            <template slot="status" slot-scope="text, record">
              <span :class="getStatusClass(record)" class="status-indicator"></span>
            </template>

            <template slot="name" slot-scope="text, record">
              <span class="name-text" :title="record?.objectMeta?.name || '未知'">
                {{ record?.objectMeta?.name || '未知' }}
              </span>
            </template>

            <template slot="image" slot-scope="text, record">
              <span class="image-cell" :title="record?.containerImages ? record.containerImages.join(', ') : ''">
                {{ record?.containerImages ? record.containerImages.join(', ') : '-' }}
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

            <template slot="pods" slot-scope="text, record">
              <div class="pods-display">
                <span>{{ record?.pods && record.pods.running !== undefined ? record.pods.running : 0 }} / {{ record?.pods && record.pods.desired !== undefined ? record.pods.desired : 0 }}</span>
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

    <deployment-view
        v-if="deploymentViewVisible"
        :namespace="currentDeployment.namespace"
        :deploymentName="currentDeployment.name"
        :visible="deploymentViewVisible"
        @update:visible="deploymentViewVisible = $event"
    />
  </div>
</template>

<script>
// import API from '@/api';
// import K8sMetricsCharts from './K8sMetricsCharts.vue';
import DeploymentView from './DeploymentView.vue';

export default {
  name: 'DeploymentDashboard',
  components: {
    // Remove unused component registration
    // K8sMetricsCharts,
    DeploymentView
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
      deployments: [],
      loading: false,
      deploymentViewVisible: false,
      currentDeployment: {
        namespace: '',
        name: ''
      },
      expandedLabels: {},
      deploymentColumns: [
        {
          title: '',
          key: 'status',
          width: '50px',
          className: 'status-column',
          scopedSlots: { customRender: 'status' }
        },
        {
          title: '名称',
          key: 'name',
          className: 'name-column',
          scopedSlots: { customRender: 'name' }
        },
        {
          title: '镜像',
          dataIndex: 'containerImages',
          key: 'image',
          className: 'image-column',
          scopedSlots: { customRender: 'image' },
          ellipsis: true
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
  mounted() {
    this.fetchDeployments();
  },
  methods: {
    getStatusClass(record) {
      const classNames = ['status-dot'];
      if (record?.pods?.running > 0) classNames.push('status-running');
      if (record?.pods?.pending > 0) classNames.push('status-warning');
      if (record?.pods?.failed > 0) classNames.push('status-danger');
      if (!record?.pods || (!record?.pods.running && !record?.pods.pending && !record?.pods.failed))
        classNames.push('status-unknown');
      return classNames.join(' ');
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
    async fetchDeployments() {
      this.loading = true;
      try {
        const res = await this.$axiosGet(global.API.getK8sDeployments, {
          clusterId: this.clusterId,
          serviceId: this.serviceId,
          namespace: this.selectedNamespace === 'all' ? null : this.selectedNamespace
        });
        if (res.code === 200) {
          // 确保获取部署列表数组，并处理数据，确保每个部署对象都有必要的属性
          let deployList = res.data && res.data.deployments ? res.data.deployments : [];

          // 处理deployments数据，确保每个项都有必要的属性
          this.deployments = deployList.map(deploy => {
            // 如果deploy为null或undefined，返回一个空对象
            if (!deploy) return { objectMeta: {}, pods: {} };

            // 确保objectMeta存在
            if (!deploy.objectMeta) deploy.objectMeta = {};

            // 确保pods存在
            if (!deploy.pods) deploy.pods = {};

            return deploy;
          });

          console.log("处理后的deployments数据:", this.deployments);

          // 单独测试第一个对象的数据结构
          if (this.deployments.length > 0) {
            const firstDeploy = this.deployments[0];
            console.log("第一个deployment的数据结构:", {
              name: firstDeploy.objectMeta?.name,
              namespace: firstDeploy.objectMeta?.namespace,
              images: firstDeploy.containerImages,
              pods: firstDeploy.pods
            });
          }
        } else {
          console.error('Failed to fetch deployments:', res.msg);
          this.deployments = [];
        }
      } catch (error) {
        console.error('Error fetching deployments:', error);
        this.deployments = [];
      } finally {
        this.loading = false;
      }
    },
    handleViewDeployment(record) {
      if (!record || !record.objectMeta) {
        this.$message.warning('部署信息不完整，无法查看详情');
        return;
      }

      this.currentDeployment = {
        namespace: record.objectMeta.namespace || '',
        name: record.objectMeta.name || ''
      }
      this.deploymentViewVisible = true
    },
    handleEditDeployment(record) {
      // TODO: 实现编辑Deployment的逻辑
      this.$message.info(`编辑Deployment ${record.name} 的功能正在开发中`);
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
@import './styles/k8s-table-styles.less';

.status-indicator {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-right: 0;
}

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
</style> 