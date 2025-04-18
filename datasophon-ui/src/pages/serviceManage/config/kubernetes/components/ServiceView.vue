<template>
  <div class="service-view">
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
        <a-spin />
      </div>
      <template v-else>
        <a-tabs>
          <a-tab-pane key="details" tab="详情">
            <div class="service-details">
              <div class="detail-section">
                <div class="section-title">基本信息</div>
                <a-descriptions :column="2" size="small" bordered>
                  <a-descriptions-item label="名称">{{ serviceData.metadata?.name }}</a-descriptions-item>
                  <a-descriptions-item label="命名空间">{{ serviceData.metadata?.namespace }}</a-descriptions-item>
                  <a-descriptions-item label="创建时间">
                    {{ formatTime(serviceData.metadata?.creationTimestamp) }}
                  </a-descriptions-item>
                  <a-descriptions-item label="集群IP">{{ serviceData.spec?.clusterIP }}</a-descriptions-item>
                  <a-descriptions-item label="服务类型">{{ serviceData.spec?.type }}</a-descriptions-item>
                  <a-descriptions-item label="外部IP" :span="2">
                    <span v-if="serviceData.status?.loadBalancer?.ingress?.length">
                      {{ serviceData.status.loadBalancer.ingress.map(ing => ing.ip || ing.hostname).join(', ') }}
                    </span>
                    <span v-else>-</span>
                  </a-descriptions-item>
                </a-descriptions>
              </div>

              <div class="detail-section">
                <div class="section-title">标签</div>
                <div class="labels-section">
                  <a-tag v-for="(value, key) in serviceData.metadata?.labels" :key="key" color="blue">
                    {{ key }}: {{ value }}
                  </a-tag>
                  <span v-if="!serviceData.metadata?.labels || Object.keys(serviceData.metadata?.labels).length === 0">
                    无标签
                  </span>
                </div>
              </div>

              <div class="detail-section">
                <div class="section-title">端口</div>
                <a-table
                  :columns="portColumns"
                  :data-source="servicePorts"
                  :pagination="false"
                  size="small"
                >
                </a-table>
              </div>

              <div class="detail-section">
                <div class="section-title">选择器</div>
                <div class="labels-section">
                  <a-tag v-for="(value, key) in serviceData.spec?.selector" :key="key" color="green">
                    {{ key }}: {{ value }}
                  </a-tag>
                  <span v-if="!serviceData.spec?.selector || Object.keys(serviceData.spec?.selector).length === 0">
                    无选择器
                  </span>
                </div>
              </div>
            </div>
          </a-tab-pane>

          <a-tab-pane key="endpoints" tab="端点" forceRender>
            <div v-if="loadingEndpoints" class="loading-container">
              <a-spin />
            </div>
            <a-table
              v-else
              :columns="endpointColumns"
              :data-source="endpointsList"
              :pagination="false"
              size="small"
            >
            </a-table>
          </a-tab-pane>

          <a-tab-pane key="events" tab="事件" forceRender>
            <div v-if="loadingEvents" class="loading-container">
              <a-spin />
            </div>
            <a-table
              v-else
              :columns="eventColumns"
              :data-source="eventsList"
              :pagination="false"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'type'">
                  <a-tag :color="getEventTypeColor(record.type)">{{ record.type }}</a-tag>
                </template>
                <template v-else-if="column.dataIndex === 'age'">
                  {{ calculateAge(record.lastTimestamp || record.eventTime) }}
                </template>
              </template>
            </a-table>
          </a-tab-pane>

          <a-tab-pane key="yaml" tab="YAML" forceRender>
            <div class="yaml-container">
              <pre>{{ serviceYaml }}</pre>
            </div>
          </a-tab-pane>
        </a-tabs>
      </template>
    </a-modal>
  </div>
</template>

<script>
import { defineComponent, reactive, onMounted, toRefs, computed, watch } from 'vue'
import { k8sServiceApi } from '@/api/service-api'
import moment from 'moment'
import yaml from 'js-yaml'

export default defineComponent({
  name: 'ServiceView',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    namespace: {
      type: String,
      required: true
    },
    serviceName: {
      type: String,
      required: true
    }
  },
  emits: ['update:visible'],
  setup(props, { emit }) {
    const state = reactive({
      loading: false,
      loadingEndpoints: false,
      loadingEvents: false,
      serviceData: {},
      endpointsList: [],
      eventsList: []
    })

    // 列定义
    const portColumns = [
      {
        title: '名称',
        dataIndex: 'name',
        key: 'name',
        width: '20%'
      },
      {
        title: '协议',
        dataIndex: 'protocol',
        key: 'protocol',
        width: '15%'
      },
      {
        title: '端口',
        dataIndex: 'port',
        key: 'port',
        width: '15%'
      },
      {
        title: '目标端口',
        dataIndex: 'targetPort',
        key: 'targetPort',
        width: '15%'
      },
      {
        title: '节点端口',
        dataIndex: 'nodePort',
        key: 'nodePort',
        width: '15%'
      }
    ]

    const endpointColumns = [
      {
        title: 'IP地址',
        dataIndex: 'ip',
        key: 'ip',
        width: '25%'
      },
      {
        title: '节点名称',
        dataIndex: 'nodeName',
        key: 'nodeName',
        width: '25%'
      },
      {
        title: '就绪状态',
        dataIndex: 'ready',
        key: 'ready',
        width: '15%'
      },
      {
        title: '端口',
        dataIndex: 'ports',
        key: 'ports'
      }
    ]

    const eventColumns = [
      {
        title: '类型',
        dataIndex: 'type',
        key: 'type',
        width: '15%'
      },
      {
        title: '原因',
        dataIndex: 'reason',
        key: 'reason',
        width: '20%'
      },
      {
        title: '对象',
        dataIndex: 'involvedObject.name',
        key: 'involvedObject',
        width: '20%'
      },
      {
        title: '消息',
        dataIndex: 'message',
        key: 'message'
      },
      {
        title: '时间',
        dataIndex: 'age',
        key: 'age',
        width: '15%'
      }
    ]

    // 计算属性
    const modalTitle = computed(() => {
      return `Service: ${props.serviceName} (${props.namespace})`
    })

    const servicePorts = computed(() => {
      if (!state.serviceData.spec?.ports) return []
      return state.serviceData.spec.ports.map(port => ({
        name: port.name || '-',
        protocol: port.protocol,
        port: port.port,
        targetPort: port.targetPort,
        nodePort: port.nodePort || '-'
      }))
    })

    const serviceYaml = computed(() => {
      try {
        if (!state.serviceData || Object.keys(state.serviceData).length === 0) return ''
        return yaml.dump(state.serviceData)
      } catch (e) {
        console.error('YAML转换错误:', e)
        return '无法生成YAML'
      }
    })

    // 方法
    const formatTime = (timestamp) => {
      if (!timestamp) return '-'
      return moment(timestamp).format('YYYY-MM-DD HH:mm:ss')
    }

    const calculateAge = (timestamp) => {
      if (!timestamp) return '-'
      const now = moment()
      const eventTime = moment(timestamp)
      const diff = now.diff(eventTime, 'seconds')
      
      if (diff < 60) return `${diff}秒前`
      if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
      if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
      return `${Math.floor(diff / 86400)}天前`
    }

    const getEventTypeColor = (type) => {
      if (type === 'Warning') return 'orange'
      if (type === 'Normal') return 'green'
      return 'blue'
    }

    const handleCancel = () => {
      emit('update:visible', false)
    }

    // 加载Service详情
    const loadServiceDetails = async () => {
      state.loading = true
      try {
        const res = await k8sServiceApi.getServiceDetails({
          namespace: props.namespace,
          name: props.serviceName
        })
        state.serviceData = res.data
      } catch (error) {
        console.error('获取Service详情失败:', error)
      } finally {
        state.loading = false
      }
    }

    // 加载Endpoints
    const loadEndpoints = async () => {
      state.loadingEndpoints = true
      try {
        const res = await k8sServiceApi.getServiceEndpoints({
          namespace: props.namespace,
          name: props.serviceName
        })
        
        // 解析端点数据
        const endpoints = []
        if (res.data.subsets && res.data.subsets.length > 0) {
          res.data.subsets.forEach(subset => {
            if (subset.addresses) {
              subset.addresses.forEach(address => {
                endpoints.push({
                  ip: address.ip,
                  nodeName: address.nodeName || '-',
                  ready: '是',
                  ports: subset.ports ? subset.ports.map(p => `${p.port}/${p.protocol}`).join(', ') : '-'
                })
              })
            }
            
            if (subset.notReadyAddresses) {
              subset.notReadyAddresses.forEach(address => {
                endpoints.push({
                  ip: address.ip,
                  nodeName: address.nodeName || '-',
                  ready: '否',
                  ports: subset.ports ? subset.ports.map(p => `${p.port}/${p.protocol}`).join(', ') : '-'
                })
              })
            }
          })
        }
        
        state.endpointsList = endpoints
      } catch (error) {
        console.error('获取Endpoints失败:', error)
      } finally {
        state.loadingEndpoints = false
      }
    }

    // 加载事件列表
    const loadEvents = async () => {
      state.loadingEvents = true
      try {
        const res = await k8sServiceApi.getResourceEvents({
          namespace: props.namespace,
          kind: 'Service',
          name: props.serviceName
        })
        state.eventsList = res.data
      } catch (error) {
        console.error('获取事件失败:', error)
      } finally {
        state.loadingEvents = false
      }
    }

    // 监听属性变化
    watch(() => props.visible, (newValue) => {
      if (newValue) {
        loadServiceDetails()
        loadEndpoints()
        loadEvents()
      }
    })

    onMounted(() => {
      if (props.visible) {
        loadServiceDetails()
        loadEndpoints()
        loadEvents()
      }
    })

    return {
      ...toRefs(state),
      modalTitle,
      portColumns,
      endpointColumns,
      eventColumns,
      servicePorts,
      serviceYaml,
      formatTime,
      calculateAge,
      getEventTypeColor,
      handleCancel
    }
  }
})
</script>

<style lang="less" scoped>
.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
}

.service-details {
  .detail-section {
    margin-bottom: 16px;

    .section-title {
      font-size: 14px;
      font-weight: 500;
      margin-bottom: 8px;
      color: var(--text-color);
    }
  }

  .labels-section {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    padding: 8px;
    border: 1px solid #f0f0f0;
    border-radius: 2px;
    background-color: #fafafa;
  }
}

.yaml-container {
  background-color: #f5f5f5;
  border-radius: 4px;
  padding: 16px;
  max-height: 500px;
  overflow: auto;

  pre {
    margin: 0;
    white-space: pre-wrap;
    word-wrap: break-word;
    font-family: "Monaco", "Menlo", "Consolas", monospace;
    font-size: 12px;
  }
}

:deep(.ant-descriptions-item-label) {
  background-color: #fafafa;
  font-weight: 500;
  width: 100px;
}

:deep(.ant-table-small) {
  font-size: 12px;
}

:deep(.k8s-resource-modal) {
  .ant-modal-header {
    border-bottom: 1px solid #f0f0f0;
    padding: 16px 24px;
    
    .ant-modal-title {
      font-weight: 500;
      font-size: 16px;
    }
  }
  
  .ant-modal-body {
    padding: 20px;
  }
}
</style> 