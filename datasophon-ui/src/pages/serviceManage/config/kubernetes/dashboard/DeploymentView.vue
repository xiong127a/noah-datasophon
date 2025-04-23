<template>
  <div class="deployment-view">
    <a-modal
      :visible="visible"
      @update:visible="$emit('update:visible', $event)"
      :title="modalTitle"
      :width="900"
      :footer="null"
      :maskClosable="false"
      :destroyOnClose="true"
      @cancel="handleCancel"
      class="k8s-resource-modal"
    >
      <div v-if="loading" class="loading-container">
        <a-spin tip="正在加载数据..." />
      </div>
      <div v-else class="modal-content">
        <a-tabs default-active-key="details" class="resource-tabs">
          <a-tab-pane key="details" tab="详情">
            <div class="details-section">
              <div class="detail-row">
                <div class="detail-label">名称:</div>
                <div class="detail-value">{{ deploymentData.metadata.name }}</div>
              </div>
              <div class="detail-row">
                <div class="detail-label">命名空间:</div>
                <div class="detail-value">{{ deploymentData.metadata.namespace }}</div>
              </div>
              <div class="detail-row">
                <div class="detail-label">创建时间:</div>
                <div class="detail-value">{{ formatTime(deploymentData.metadata.creationTimestamp) }}</div>
              </div>
              <div v-if="deploymentData.metadata.labels" class="detail-row">
                <div class="detail-label">标签:</div>
                <div class="detail-value">
                  <a-tag v-for="(value, key) in deploymentData.metadata.labels" :key="key" color="blue">
                    {{ key }}: {{ value }}
                  </a-tag>
                </div>
              </div>
              
              <!-- Deployment状态 -->
              <div class="detail-section">
                <h3>状态</h3>
                <div class="detail-row">
                  <div class="detail-label">可用副本:</div>
                  <div class="detail-value">{{ deploymentData.status.availableReplicas || 0 }}</div>
                </div>
                <div class="detail-row">
                  <div class="detail-label">期望副本:</div>
                  <div class="detail-value">{{ deploymentData.status.replicas || 0 }}</div>
                </div>
                <div class="detail-row">
                  <div class="detail-label">更新副本:</div>
                  <div class="detail-value">{{ deploymentData.status.updatedReplicas || 0 }}</div>
                </div>
                <div class="detail-row">
                  <div class="detail-label">就绪副本:</div>
                  <div class="detail-value">{{ deploymentData.status.readyReplicas || 0 }}</div>
                </div>
              </div>
              
              <!-- 容器模板规范 -->
              <div class="detail-section">
                <h3>容器模板</h3>
                <div v-if="deploymentData.spec.template.spec.containers" class="container-section">
                  <div v-for="(container, index) in deploymentData.spec.template.spec.containers" :key="index" class="container-card">
                    <h4>容器: {{ container.name }}</h4>
                    <div class="detail-row">
                      <div class="detail-label">镜像:</div>
                      <div class="detail-value">{{ container.image }}</div>
                    </div>
                    <div v-if="container.ports && container.ports.length" class="detail-row">
                      <div class="detail-label">端口:</div>
                      <div class="detail-value">
                        <div v-for="(port, pIndex) in container.ports" :key="pIndex">
                          {{ port.name || '-' }}: {{ port.containerPort }}{{ port.protocol ? '/' + port.protocol : '' }}
                        </div>
                      </div>
                    </div>
                    <div v-if="container.resources" class="detail-row">
                      <div class="detail-label">资源:</div>
                      <div class="detail-value">
                        <div v-if="container.resources.limits">
                          限制: 
                          <span v-if="container.resources.limits.cpu">CPU: {{ container.resources.limits.cpu }}, </span>
                          <span v-if="container.resources.limits.memory">内存: {{ container.resources.limits.memory }}</span>
                        </div>
                        <div v-if="container.resources.requests">
                          请求: 
                          <span v-if="container.resources.requests.cpu">CPU: {{ container.resources.requests.cpu }}, </span>
                          <span v-if="container.resources.requests.memory">内存: {{ container.resources.requests.memory }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Deployment策略 -->
              <div class="detail-section">
                <h3>部署策略</h3>
                <div v-if="deploymentData.spec.strategy" class="detail-row">
                  <div class="detail-label">类型:</div>
                  <div class="detail-value">{{ deploymentData.spec.strategy.type }}</div>
                </div>
                <div v-if="deploymentData.spec.strategy && deploymentData.spec.strategy.rollingUpdate" class="detail-row">
                  <div class="detail-label">最大不可用:</div>
                  <div class="detail-value">{{ deploymentData.spec.strategy.rollingUpdate.maxUnavailable }}</div>
                </div>
                <div v-if="deploymentData.spec.strategy && deploymentData.spec.strategy.rollingUpdate" class="detail-row">
                  <div class="detail-label">最大超量:</div>
                  <div class="detail-value">{{ deploymentData.spec.strategy.rollingUpdate.maxSurge }}</div>
                </div>
              </div>
            </div>
          </a-tab-pane>
          
          <a-tab-pane key="pods" tab="Pods">
            <a-table 
              :dataSource="podsList" 
              :columns="podColumns" 
              :pagination="false"
              :loading="loadingPods"
              rowKey="name"
              size="middle"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <a-tag :color="getStatusColor(record.status)">{{ record.status }}</a-tag>
                </template>
                <template v-if="column.key === 'action'">
                  <a @click="viewPod(record)">查看</a>
                </template>
              </template>
            </a-table>
          </a-tab-pane>

          <a-tab-pane key="events" tab="事件">
            <a-table 
              :dataSource="eventsList" 
              :columns="eventColumns" 
              :pagination="false"
              :loading="loadingEvents"
              rowKey="uid"
              size="middle"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'type'">
                  <a-tag :color="getEventTypeColor(record.type)">{{ record.type }}</a-tag>
                </template>
                <template v-if="column.key === 'age'">
                  {{ calculateAge(record.lastTimestamp || record.firstTimestamp) }}
                </template>
              </template>
            </a-table>
          </a-tab-pane>

          <a-tab-pane key="yaml" tab="YAML">
            <div class="yaml-container">
              <pre class="yaml-content">{{ deploymentYaml }}</pre>
            </div>
          </a-tab-pane>
        </a-tabs>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { defineComponent, ref, reactive, onMounted, toRefs, computed, watch } from 'vue'
import { k8sDeploymentApi } from '@/api/service-api'
import moment from 'moment'
import yaml from 'js-yaml'

export default defineComponent({
  name: 'DeploymentView',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    namespace: {
      type: String,
      required: true
    },
    deploymentName: {
      type: String,
      required: true
    }
  },
  emits: ['update:visible', 'refresh', 'view-pod'],
  setup(props, { emit }) {
    const state = reactive({
      loading: true,
      loadingPods: false,
      loadingEvents: false,
      deploymentData: {},
      podsList: [],
      eventsList: []
    })

    const modalTitle = computed(() => {
      return `Deployment: ${props.deploymentName}`
    })

    const deploymentYaml = computed(() => {
      if (!state.deploymentData || Object.keys(state.deploymentData).length === 0) {
        return ''
      }
      try {
        return yaml.dump(state.deploymentData)
      } catch (error) {
        console.error('转换YAML出错:', error)
        return JSON.stringify(state.deploymentData, null, 2)
      }
    })

    const podColumns = [
      {
        title: '名称',
        dataIndex: 'name',
        key: 'name'
      },
      {
        title: 'IP',
        dataIndex: 'podIP',
        key: 'podIP'
      },
      {
        title: '节点',
        dataIndex: 'nodeName',
        key: 'nodeName'
      },
      {
        title: '状态',
        dataIndex: 'status',
        key: 'status'
      },
      {
        title: '创建时间',
        dataIndex: 'creationTimestamp',
        key: 'creationTimestamp',
        render: (text) => formatTime(text)
      },
      {
        title: '操作',
        key: 'action'
      }
    ]

    const eventColumns = [
      {
        title: '类型',
        dataIndex: 'type',
        key: 'type'
      },
      {
        title: '原因',
        dataIndex: 'reason',
        key: 'reason'
      },
      {
        title: '对象',
        dataIndex: 'involvedObject.name',
        key: 'objectName'
      },
      {
        title: '消息',
        dataIndex: 'message',
        key: 'message',
        ellipsis: true
      },
      {
        title: '时间',
        dataIndex: 'lastTimestamp',
        key: 'age'
      }
    ]

    // 格式化时间
    const formatTime = (timestamp) => {
      if (!timestamp) return '-'
      return moment(timestamp).format('YYYY-MM-DD HH:mm:ss')
    }

    // 计算事件发生的时间
    const calculateAge = (timestamp) => {
      if (!timestamp) return '-'
      return moment(timestamp).fromNow()
    }

    // 获取状态颜色
    const getStatusColor = (status) => {
      const statusMap = {
        'Running': 'green',
        'Pending': 'orange',
        'Succeeded': 'blue',
        'Failed': 'red',
        'Unknown': 'gray'
      }
      return statusMap[status] || 'default'
    }

    // 获取事件类型颜色
    const getEventTypeColor = (type) => {
      const typeMap = {
        'Normal': 'green',
        'Warning': 'orange'
      }
      return typeMap[type] || 'default'
    }

    // 加载Deployment详情
    const loadDeploymentDetails = async () => {
      state.loading = true
      try {
        const res = await k8sDeploymentApi.getDeploymentDetails({
          namespace: props.namespace,
          name: props.deploymentName
        })
        state.deploymentData = res.data
      } catch (error) {
        console.error('获取Deployment详情失败:', error)
      } finally {
        state.loading = false
      }
    }

    // 加载Pods列表
    const loadPods = async () => {
      state.loadingPods = true
      try {
        const res = await k8sDeploymentApi.getDeploymentPods({
          namespace: props.namespace,
          name: props.deploymentName
        })
        state.podsList = res.data.map(pod => {
          return {
            ...pod.metadata,
            status: pod.status.phase,
            podIP: pod.status.podIP,
            nodeName: pod.spec.nodeName,
            creationTimestamp: pod.metadata.creationTimestamp
          }
        })
      } catch (error) {
        console.error('获取Pods失败:', error)
      } finally {
        state.loadingPods = false
      }
    }

    // 加载事件列表
    const loadEvents = async () => {
      state.loadingEvents = true
      try {
        const res = await k8sDeploymentApi.getResourceEvents({
          namespace: props.namespace,
          kind: 'Deployment',
          name: props.deploymentName
        })
        state.eventsList = res.data
      } catch (error) {
        console.error('获取事件失败:', error)
      } finally {
        state.loadingEvents = false
      }
    }

    // 查看Pod详情
    const viewPod = (pod) => {
      emit('view-pod', {
        namespace: props.namespace,
        name: pod.name
      })
    }

    // 取消modal
    const handleCancel = () => {
      emit('update:visible', false)
    }

    // 加载全部数据
    const loadData = () => {
      loadDeploymentDetails()
      loadPods()
      loadEvents()
    }

    // 监听props变化
    watch(() => props.visible, (newVal) => {
      if (newVal) {
        loadData()
      }
    })

    // 初始加载
    onMounted(() => {
      if (props.visible) {
        loadData()
      }
    })

    return {
      ...toRefs(state),
      modalTitle,
      deploymentYaml,
      podColumns,
      eventColumns,
      formatTime,
      calculateAge,
      getStatusColor,
      getEventTypeColor,
      handleCancel,
      viewPod
    }
  }
})
</script>

<style scoped>
.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 400px;
}

.modal-content {
  min-height: 400px;
}

.details-section {
  padding: 16px;
  background-color: #fff;
}

.detail-section {
  margin-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 16px;
}

.detail-section h3 {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 16px;
  color: #333;
}

.detail-row {
  display: flex;
  margin-bottom: 12px;
}

.detail-label {
  width: 120px;
  color: #666;
  font-weight: 500;
}

.detail-value {
  flex: 1;
  word-break: break-all;
}

.container-section {
  margin-top: 16px;
}

.container-card {
  margin-bottom: 16px;
  padding: 16px;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  background-color: #fafafa;
}

.container-card h4 {
  margin-bottom: 16px;
  color: #333;
  font-weight: 500;
}

.yaml-container {
  padding: 16px;
  background-color: #fafafa;
  border-radius: 4px;
  overflow: auto;
  max-height: 500px;
}

.yaml-content {
  margin: 0;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
  font-size: 13px;
  line-height: 1.5;
  color: #333;
  white-space: pre-wrap;
}

.resource-tabs {
  margin-top: 8px;
}

.k8s-resource-modal :deep(.ant-modal-body) {
  padding: 16px;
}

.k8s-resource-modal :deep(.ant-tabs-nav) {
  margin-bottom: 16px;
}
</style> 