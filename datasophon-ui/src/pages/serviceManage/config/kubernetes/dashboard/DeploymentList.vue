<template>
  <div class="deployment-list-container">
    <a-spin :spinning="loading">
      <template v-if="deployments.length === 0 && !loading">
        <a-empty description="暂无部署" />
      </template>
      <a-table
        v-else
        :columns="columns"
        :data-source="deployments"
        :pagination="false"
        :rowKey="record => record.uid || record.name"
        size="middle"
      >
        <template #name="{ text, record }">
          <div class="resource-name">
            <div class="resource-icon">
              <span class="k8s-icon">K8S</span>
            </div>
            <a @click="viewDeployment(record)">{{ text }}</a>
          </div>
        </template>

        <template #labels="{ text }">
          <div class="label-container">
            <a-tag v-for="(value, key) in text" :key="key" color="blue" class="label-tag">
              {{ key }}: {{ value }}
            </a-tag>
          </div>
        </template>

        <template #status="{ record }">
          <a-tag :color="getStatusColor(record)">
            {{ getStatusText(record) }}
          </a-tag>
        </template>

        <template #actions="{ record }">
          <div class="action-buttons">
            <a-button type="link" size="small" @click="viewDeployment(record)">
              查看
            </a-button>
            <a-button type="link" size="small" @click="editDeployment(record)">
              编辑
            </a-button>
          </div>
        </template>
      </a-table>
    </a-spin>

    <!-- 部署详情弹窗 -->
    <deployment-detail
      :visible="detailVisible"
      @update:visible="val => detailVisible = val"
      :deployment-name="selectedDeploymentName"
      :namespace="namespace"
      :cluster-id="clusterId"
    />
  </div>
</template>

<script>
import DeploymentDetail from './DeploymentDetail.vue';

export default {
  name: 'DeploymentList',
  components: {
    DeploymentDetail
  },
  props: {
    clusterId: {
      type: Number,
      required: true
    },
    namespace: {
      type: String,
      default: 'default'
    }
  },
  data() {
    return {
      loading: false,
      deployments: [],
      columns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          slots: { customRender: 'name' },
          width: '25%'
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          slots: { customRender: 'labels' },
          width: '20%'
        },
        {
          title: '副本',
          key: 'replicas',
          width: '10%',
          customRender: ({ record }) => {
            return `${record.readyReplicas || 0}/${record.replicas || 0}`;
          }
        },
        {
          title: '镜像',
          dataIndex: 'image',
          key: 'image',
          width: '15%',
          ellipsis: true
        },
        {
          title: '状态',
          key: 'status',
          slots: { customRender: 'status' },
          width: '10%'
        },
        {
          title: '操作',
          key: 'action',
          slots: { customRender: 'actions' },
          width: '10%',
          align: 'center'
        }
      ],
      detailVisible: false,
      selectedDeploymentName: ''
    };
  },
  watch: {
    clusterId() {
      this.fetchDeployments();
    },
    namespace() {
      this.fetchDeployments();
    }
  },
  mounted() {
    this.fetchDeployments();
  },
  methods: {
    /**
     * 获取部署列表
     * 通过K8S API获取指定集群和命名空间下的Deployment列表
     */
    async fetchDeployments() {
      if (!this.clusterId) {
        return;
      }
      
      this.loading = true;
      try {
        const params = {
          clusterId: this.clusterId,
          namespace: this.namespace
        };
        
        // 调用真实API获取数据
        const res = await this.$axiosGet(global.API.getK8sDeployments, params);
        
        /* 
        // 模拟数据
        const data = {
          code: 200,
          data: [
            {
              uid: 'deploy-1',
              name: 'nginx-deployment',
              namespace: this.namespace,
              replicas: 3,
              readyReplicas: 3,
              availableReplicas: 3,
              image: 'nginx:1.19',
              labels: {
                app: 'nginx',
                tier: 'frontend'
              },
              createTime: new Date(Date.now() - 86400000).toISOString(),
              status: 'Available'
            },
            {
              uid: 'deploy-2',
              name: 'redis-deployment',
              namespace: this.namespace,
              replicas: 2,
              readyReplicas: 2,
              availableReplicas: 2,
              image: 'redis:6.0',
              labels: {
                app: 'redis',
                tier: 'backend'
              },
              createTime: new Date(Date.now() - 172800000).toISOString(),
              status: 'Available'
            },
            {
              uid: 'deploy-3',
              name: 'mysql-deployment',
              namespace: this.namespace,
              replicas: 1,
              readyReplicas: 0,
              availableReplicas: 0,
              image: 'mysql:8.0',
              labels: {
                app: 'mysql',
                tier: 'database'
              },
              createTime: new Date(Date.now() - 259200000).toISOString(),
              status: 'Progressing'
            }
          ]
        };
        */
        
        if (res.code === 200) {
          this.deployments = res.data || [];
        } else {
          console.error('获取部署列表失败:', res.msg);
          this.$message.error('获取部署列表失败: ' + res.msg);
          this.deployments = [];
        }
      } catch (error) {
        console.error('获取部署列表失败:', error);
        this.$message.error(`获取部署列表失败: ${error.message || '未知错误'}`);
        this.deployments = [];
      } finally {
        this.loading = false;
      }
    },
    
    /**
     * 查看部署详情
     * @param {Object} deployment 部署对象
     */
    viewDeployment(deployment) {
      this.selectedDeploymentName = deployment.name;
      this.detailVisible = true;
    },
    
    /**
     * 编辑部署
     * @param {Object} deployment 部署对象
     */
    editDeployment(deployment) {
      // 实现编辑功能
      this.$message.info(`编辑部署 ${deployment.name} 的功能正在开发中`);
    },
    
    /**
     * 获取状态文本
     * @param {Object} deployment 部署对象
     * @returns {string} 状态文本
     */
    getStatusText(deployment) {
      if (!deployment) return '未知';
      
      if (deployment.readyReplicas === deployment.replicas && deployment.replicas > 0) {
        return '运行中';
      } else if (deployment.readyReplicas === 0) {
        return '未就绪';
      } else {
        return '部分就绪';
      }
    },
    
    /**
     * 获取状态颜色
     * @param {Object} deployment 部署对象
     * @returns {string} 状态颜色
     */
    getStatusColor(deployment) {
      if (!deployment) return 'default';
      
      if (deployment.readyReplicas === deployment.replicas && deployment.replicas > 0) {
        return 'green';
      } else if (deployment.readyReplicas === 0) {
        return 'red';
      } else {
        return 'orange';
      }
    },
    
    /**
     * 格式化时间
     * @param {string} time ISO时间字符串
     * @returns {string} 格式化后的时间
     */
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
  }
};
</script>

<style scoped>
@import './styles/k8s-table-styles.less';

.deployment-list-container {
  width: 100%;
  padding: 16px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

.resource-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.resource-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.k8s-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 4px;
  background-color: #1890ff;
  color: white;
  font-size: 12px;
  font-weight: bold;
}

.label-container {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.label-tag {
  margin-right: 0;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 8px;
}

:deep(.ant-table-thead > tr > th) {
  background-color: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
  font-weight: 600;
  color: #374151;
}

:deep(.ant-table-tbody > tr > td) {
  border-bottom: 1px solid #f3f4f6;
}

:deep(.ant-table-tbody > tr:hover > td) {
  background-color: #f9fafb;
}

:deep(.ant-empty) {
  margin: 32px 0;
}

:deep(.ant-spin-nested-loading) {
  width: 100%;
}

:deep(.ant-spin-container) {
  width: 100%;
}
</style> 