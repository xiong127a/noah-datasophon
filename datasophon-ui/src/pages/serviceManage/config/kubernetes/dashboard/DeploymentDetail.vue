<template>
  <div class="deployment-detail-container">
    <a-modal
      :visible="visible"
      :title="'Deployment详情: ' + (deployment?.name || '')"
      :width="800"
      :footer="null"
      @cancel="handleClose"
      :maskClosable="false"
      class="deployment-detail-modal"
    >
      <a-spin :spinning="loading">
        <a-tabs v-model="activeTab">
          <a-tab-pane key="overview" tab="概览">
            <div class="detail-section">
              <div class="section-title">基本信息</div>
              <a-descriptions :column="2" bordered size="small">
                <a-descriptions-item label="名称" :span="2">
                  {{ deployment?.name || '-' }}
                </a-descriptions-item>
                <a-descriptions-item label="创建时间" :span="2">
                  {{ formatTime(deployment?.createTime) }}
                </a-descriptions-item>
                <a-descriptions-item label="副本数" :span="2">
                  {{ deployment?.replicas || 0 }} (可用: {{ deployment?.availableReplicas || 0 }}, 就绪: {{ deployment?.readyReplicas || 0 }})
                </a-descriptions-item>
                <a-descriptions-item label="更新策略" :span="2">
                  {{ deployment?.strategy || '-' }}
                </a-descriptions-item>
              </a-descriptions>
            </div>

            <div class="detail-section">
              <div class="section-title">标签与选择器</div>
              <a-descriptions :column="1" bordered size="small">
                <a-descriptions-item label="标签">
                  <div class="tag-container" v-if="deployment?.labels && Object.keys(deployment.labels).length > 0">
                    <a-tag v-for="(value, key) in deployment.labels" :key="key" color="blue">
                      {{ key }}: {{ value }}
                    </a-tag>
                  </div>
                  <span v-else>-</span>
                </a-descriptions-item>
                <a-descriptions-item label="选择器">
                  <div class="tag-container" v-if="deployment?.selector && Object.keys(deployment.selector).length > 0">
                    <a-tag v-for="(value, key) in deployment.selector" :key="key" color="green">
                      {{ key }}: {{ value }}
                    </a-tag>
                  </div>
                  <span v-else>-</span>
                </a-descriptions-item>
              </a-descriptions>
            </div>

            <div class="detail-section">
              <div class="section-title">容器信息</div>
              <a-descriptions :column="1" bordered size="small">
                <a-descriptions-item label="镜像">
                  {{ deployment?.image || '-' }}
                </a-descriptions-item>
                <a-descriptions-item label="资源配额">
                  <div v-if="deployment?.resources">
                    <div>CPU请求: {{ deployment.resources.cpuRequest || '-' }}</div>
                    <div>CPU限制: {{ deployment.resources.cpuLimit || '-' }}</div>
                    <div>内存请求: {{ deployment.resources.memoryRequest || '-' }}</div>
                    <div>内存限制: {{ deployment.resources.memoryLimit || '-' }}</div>
                  </div>
                  <span v-else>-</span>
                </a-descriptions-item>
              </a-descriptions>
            </div>
          </a-tab-pane>

          <a-tab-pane key="yaml" tab="YAML">
            <div class="yaml-container">
              <div class="editor-actions">
                <a-button type="primary" size="small" @click="copyYaml">
                  <template #icon><copy-outlined /></template>
                  复制
                </a-button>
              </div>
              <a-textarea
                v-model="yamlContent"
                :auto-size="{ minRows: 20, maxRows: 30 }"
                readonly
                class="yaml-editor"
              />
            </div>
          </a-tab-pane>

          <a-tab-pane key="events" tab="事件">
            <a-table
              :columns="eventColumns"
              :dataSource="events"
              :pagination="false"
              :rowKey="record => record.uid || record.time"
              size="small"
              class="k8s-table"
            >
              <template #type="{ text }">
                <a-tag :color="getEventTypeColor(text)">{{ text }}</a-tag>
              </template>
              <template #time="{ text }">
                {{ formatTime(text) }}
              </template>
            </a-table>
          </a-tab-pane>
        </a-tabs>
      </a-spin>
    </a-modal>
  </div>
</template>

<script>
import { CopyOutlined } from '@ant-design/icons-vue';

export default {
  name: 'DeploymentDetail',
  components: {
    CopyOutlined
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    deploymentName: {
      type: String,
      default: ''
    },
    namespace: {
      type: String,
      default: 'default'
    },
    clusterId: {
      type: Number,
      required: true
    }
  },
  data() {
    return {
      loading: false,
      activeTab: 'overview',
      deployment: null,
      yamlContent: '',
      events: [],
      eventColumns: [
        {
          title: '类型',
          dataIndex: 'type',
          key: 'type',
          width: '10%',
          slots: { customRender: 'type' }
        },
        {
          title: '原因',
          dataIndex: 'reason',
          key: 'reason',
          width: '15%'
        },
        {
          title: '对象',
          dataIndex: 'involvedObject',
          key: 'involvedObject',
          width: '20%'
        },
        {
          title: '消息',
          dataIndex: 'message',
          key: 'message',
          width: '40%'
        },
        {
          title: '时间',
          dataIndex: 'time',
          key: 'time',
          width: '15%',
          slots: { customRender: 'time' }
        }
      ]
    };
  },
  methods: {
    // 加载Deployment详情
    async loadDeploymentDetail() {
      if (!this.deploymentName || !this.namespace || !this.clusterId) {
        return;
      }

      this.loading = true;
      try {
        // 获取Deployment详情
        // 应该添加真实API调用，未来实现
        // TODO: 实现获取Deployment详情API
        /* 
        const deploymentRes = await this.$axiosGet(global.API.getK8sDeploymentDetail, {
          clusterId: this.clusterId,
          namespace: this.namespace,
          name: this.deploymentName
        });
        
        if (deploymentRes.code === 200) {
          this.deployment = deploymentRes.data;
        } else {
          this.$message.error('获取Deployment详情失败: ' + deploymentRes.msg);
          return;
        }
        */
        
        // 暂时使用模拟数据
        this.deployment = {
          name: this.deploymentName,
          namespace: this.namespace,
          labels: {
            'app': this.deploymentName,
            'version': 'v1'
          },
          selector: {
            'app': this.deploymentName
          },
          replicas: 3,
          availableReplicas: 3,
          readyReplicas: 3,
          strategy: 'RollingUpdate',
          image: 'nginx:1.19',
          createTime: new Date(),
          resources: {
            cpuRequest: '100m',
            cpuLimit: '200m',
            memoryRequest: '128Mi',
            memoryLimit: '256Mi'
          }
        };

        // 获取YAML内容
        // TODO: 实现获取Deployment YAML的API
        /* 
        const yamlRes = await this.$axiosGet(global.API.getK8sDeploymentYaml, {
          clusterId: this.clusterId,
          namespace: this.namespace,
          name: this.deploymentName
        });
        
        if (yamlRes.code === 200) {
          this.yamlContent = yamlRes.data;
        } else {
          this.$message.error('获取Deployment YAML失败: ' + yamlRes.msg);
        }
        */
        
        // 暂时使用模拟YAML
        this.yamlContent = `apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${this.deploymentName}
  namespace: ${this.namespace}
  labels:
    app: ${this.deploymentName}
    version: v1
spec:
  replicas: 3
  selector:
    matchLabels:
      app: ${this.deploymentName}
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    metadata:
      labels:
        app: ${this.deploymentName}
    spec:
      containers:
      - name: ${this.deploymentName}
        image: nginx:1.19
        resources:
          requests:
            cpu: 100m
            memory: 128Mi
          limits:
            cpu: 200m
            memory: 256Mi
        ports:
        - containerPort: 80`;

        // 获取事件
        // TODO: 实现获取Deployment相关事件的API
        /* 
        const eventsRes = await this.$axiosGet(global.API.getK8sDeploymentEvents, {
          clusterId: this.clusterId,
          namespace: this.namespace,
          name: this.deploymentName
        });
        
        if (eventsRes.code === 200) {
          this.events = eventsRes.data;
        } else {
          this.$message.error('获取Deployment事件失败: ' + eventsRes.msg);
        }
        */
        
        // 暂时使用模拟事件数据
        this.events = [
          {
            uid: '1',
            type: 'Normal',
            reason: 'ScalingReplicaSet',
            involvedObject: `deployment/${this.deploymentName}`,
            message: `Scaled up replica set ${this.deploymentName}-abcd1234 to 3`,
            time: new Date(Date.now() - 60000)
          },
          {
            uid: '2',
            type: 'Normal',
            reason: 'Created',
            involvedObject: `pod/${this.deploymentName}-abcd1234-abc1`,
            message: 'Created container',
            time: new Date(Date.now() - 55000)
          },
          {
            uid: '3',
            type: 'Normal',
            reason: 'Started',
            involvedObject: `pod/${this.deploymentName}-abcd1234-abc1`,
            message: 'Started container',
            time: new Date(Date.now() - 54000)
          }
        ];
      } catch (error) {
        console.error('加载Deployment详情失败:', error);
        this.$message.error('加载Deployment详情失败: ' + (error.message || '未知错误'));
      } finally {
        this.loading = false;
      }
    },

    // 关闭对话框
    handleClose() {
      this.$emit('update:visible', false);
    },

    // 复制YAML
    copyYaml() {
      navigator.clipboard.writeText(this.yamlContent).then(() => {
        this.$message.success('YAML已复制到剪贴板');
      }).catch(err => {
        console.error('复制失败:', err);
        this.$message.error('复制失败: ' + err.message);
      });
    },

    // 格式化时间
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

    // 获取事件类型颜色
    getEventTypeColor(type) {
      switch (type) {
        case 'Normal':
          return 'green';
        case 'Warning':
          return 'orange';
        default:
          return 'default';
      }
    }
  },
  watch: {
    visible(newVal) {
      if (newVal) {
        this.loadDeploymentDetail();
      }
    },
    deploymentName() {
      if (this.visible) {
        this.loadDeploymentDetail();
      }
    }
  }
};
</script>

<style scoped>
@import './styles/k8s-table-styles.less';

.deployment-detail-container {
  width: 100%;
}

.deployment-detail-modal :deep(.ant-modal-content) {
  border-radius: 8px;
  overflow: hidden;
}

.deployment-detail-modal :deep(.ant-modal-header) {
  background-color: #f8f9fa;
  border-bottom: 1px solid #e9ecef;
  padding: 16px 24px;
}

.deployment-detail-modal :deep(.ant-modal-title) {
  font-size: 16px;
  font-weight: 600;
  color: #24292e;
}

.deployment-detail-modal :deep(.ant-modal-body) {
  padding: 20px;
}

.detail-section {
  margin-bottom: 20px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #24292e;
  margin-bottom: 8px;
}

.tag-container {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.yaml-container {
  position: relative;
}

.editor-actions {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 10;
}

.yaml-editor {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', 'Consolas', 'source-code-pro', monospace;
  font-size: 14px;
  line-height: 1.5;
  padding: 12px;
  background-color: #f8f9fa;
  border-radius: 4px;
}
</style> 