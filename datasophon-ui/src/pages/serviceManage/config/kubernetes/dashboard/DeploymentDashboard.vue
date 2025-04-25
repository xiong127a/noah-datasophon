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
          >
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
      deploymentColumns: [
        {
          title: '',
          dataIndex: 'status',
          key: 'status',
          width: 40,
          fixed: 'left',
          customRender: (text, record) => {
            const classNames = ['status-dot'];
            if (record?.pods?.running > 0) classNames.push('status-running');
            if (record?.pods?.pending > 0) classNames.push('status-warning');
            if (record?.pods?.failed > 0) classNames.push('status-danger');
            if (!record?.pods || (!record?.pods.running && !record?.pods.pending && !record?.pods.failed)) 
              classNames.push('status-unknown');
            return this.$createElement('span', { class: classNames.join(' ') });
          }
        },
        {
          title: '名称',
          dataIndex: 'objectMeta.name',
          key: 'name',
          width: '15%',
          fixed: 'left',
          customRender: (_, record) => {
            return this.$createElement('div', { class: 'name-cell' }, [
              this.$createElement('span', { class: 'name-text', attrs: { title: record?.objectMeta?.name || '未知' } }, record?.objectMeta?.name || '未知')
            ]);
          }
        },
        {
          title: '镜像',
          dataIndex: 'containerImages',
          key: 'image',
          width: '20%',
          customRender: (_, record) => {
            return this.$createElement('div', { class: 'image-cell', attrs: { title: record?.containerImages ? record.containerImages.join(', ') : '' } }, [
              this.$createElement('span', {}, record?.containerImages ? record.containerImages.join(', ') : '-')
            ]);
          }
        },
        {
          title: '标签',
          key: 'labels',
          width: '25%',
          customRender: (text, record) => {
            if (!record.objectMeta?.labels || Object.keys(record.objectMeta.labels).length === 0) {
              return this.$createElement('span', { class: 'empty-value' }, '-');
            }
            
            const tags = Object.entries(record.objectMeta.labels).map(([key, value]) => {
              return this.$createElement('a-tag', { 
                props: { color: 'blue' },
                class: 'label-tag',
                attrs: { title: `${key}: ${value}` },
                key: key
              }, `${key}: ${value}`);
            });
            
            return this.$createElement('div', { class: 'labels-container' }, tags);
          }
        },
        {
          title: 'Pods',
          dataIndex: 'pods',
          key: 'pods',
          width: '10%',
          customRender: (_, record) => {
            return this.$createElement('div', { class: 'pods-display' }, [
              this.$createElement('span', {}, `${record?.pods && record.pods.running !== undefined ? record.pods.running : 0} / ${record?.pods && record.pods.desired !== undefined ? record.pods.desired : 0}`)
            ]);
          }
        },
        {
          title: '创建时间',
          key: 'creationTime',
          width: '10%',
          customRender: (text, record) => {
            const timestamp = record.objectMeta?.creationTimestamp;
            if (!timestamp) return this.$createElement('span', { class: 'empty-value' }, '-');
            
            const days = this.getDaysAgo(timestamp);
            return this.$createElement('span', { 
              class: 'time-cell',
              attrs: { title: this.formatTime(timestamp) }
            }, `${days}天前`);
          }
        }
      ]
    };
  },
  mounted() {
    this.fetchDeployments();
  },
  methods: {
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
      
      // 转换为天数
      const days = Math.floor(timeDiff / (1000 * 60 * 60 * 24));
      
      return days;
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
.resource-list {
  height: 100%;
}

// 仪表板卡片样式
.k8s-dashboard-card {
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  margin-bottom: 16px;
  overflow: hidden;
  
  &.k8s-resource-card {
    .k8s-card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      height: 48px;
      padding: 0 16px;
      background-color: #f7f7f7;
      border-bottom: 1px solid #eee;
      
      .k8s-card-title {
        font-size: 16px;
        font-weight: 500;
        color: #333;
      }

      .k8s-card-actions {
        display: flex;
        gap: 12px;
        
        .k8s-action-icon {
          font-size: 16px;
          color: #999;
          cursor: pointer;
          
          &:hover {
            color: #1890ff;
          }
        }
      }
    }
    
    .k8s-card-content {
      padding: 0;
    }
  }
}

/* 表格通用样式 */
.k8s-table {
  :deep(.status-dot) {
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    margin-right: 8px;
    
    &.status-running {
      background-color: #52c41a;
    }
    
    &.status-warning {
      background-color: #faad14;
    }
    
    &.status-danger {
      background-color: #f5222d;
    }
    
    &.status-unknown {
      background-color: #d9d9d9;
    }
  }
  
  :deep(.name-cell, .image-cell, .pods-display) {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  :deep(.tag-list) {
    display: flex;
    flex-wrap: wrap;
    
    .label-tag {
      margin: 2px;
    }
  }
  
  :deep(.action-buttons) {
    white-space: nowrap;
    
    a {
      color: #1890ff;
      
      &:hover {
        color: #40a9ff;
      }
    }
  }
}

// 内部端点样式
:deep(.internal-endpoint) {
  padding: 2px 0;
  word-break: keep-all;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

// 外部端点样式
:deep(.external-endpoint) {
  display: flex;
  align-items: center;
  padding: 2px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
  
  .external-icon {
    margin-left: 4px;
    font-size: 12px;
    flex-shrink: 0;
  }
}

:deep(.labels-container) {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
        
  .label-chip {
    margin-right: 4px;
    margin-bottom: 4px;
    max-width: 100%;
    height: auto;
    line-height: 1.5;
    white-space: normal;
    word-break: break-word;
  }
}
</style> 