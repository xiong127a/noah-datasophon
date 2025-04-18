<template>
  <div class="k8s-config-container">
    <a-tabs>
      <a-tab-pane key="configmap" tab="ConfigMap">
        <a-spin :spinning="loading">
          <a-table
            :columns="configMapColumns"
            :dataSource="configMaps"
            :pagination="false"
            rowKey="name"
          >
            <template #action="{ record }">
              <a @click="handleViewConfigMap(record)">查看</a>
              <a-divider type="vertical" />
              <a @click="handleEditConfigMap(record)">编辑</a>
            </template>
            <template #labels="{ text }">
              <span v-if="text">{{ text }}</span>
              <span v-else>-</span>
            </template>
            <template #time="{ text }">
              <span>{{ formatTime(text) }}</span>
            </template>
          </a-table>
        </a-spin>
      </a-tab-pane>

      <a-tab-pane key="service" tab="Service">
        <a-spin :spinning="loading">
          <a-table
            :columns="serviceColumns"
            :dataSource="services"
            :pagination="false"
          >
            <template #action="{ record }">
              <div class="action-icons">
                <a class="action-button" @click="handleViewService(record)">查看</a>
                <a class="action-button" @click="handleEditService(record)">编辑</a>
              </div>
            </template>
          </a-table>
        </a-spin>
      </a-tab-pane>

      <a-tab-pane key="pvc" tab="PVC">
        <a-spin :spinning="loading">
          <a-table
            :columns="pvcColumns"
            :dataSource="pvcs"
            :pagination="false"
          >
            <template #action="{ record }">
              <a @click="handleViewPvc(record)">查看</a>
              <a-divider type="vertical" />
              <a @click="handleEditPvc(record)">编辑</a>
            </template>
          </a-table>
        </a-spin>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script>
export default {
  name: 'K8sConfig',
  props: {
    serviceId: {
      type: [Number, String],
      required: true
    },
    serviceName: {
      type: String,
      required: true,
      default: '未知服务'
    },
    clusterId: {
      type: Number,
      required: true,
      default: 1
    }
  },
  data() {
    return {
      configMaps: [],
      services: [],
      pvcs: [],
      loading: false,
      configMapColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '25%',
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '25%',
          slots: { customRender: 'labels' }
        },
        {
          title: '创建时间',
          dataIndex: 'time',
          key: 'time',
          width: '25%',
          slots: { customRender: 'time' }
        },
        {
          title: '操作',
          key: 'action',
          width: '25%',
          scopedSlots: { customRender: 'action' },
        },
      ],
      serviceColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '30%',
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '30%',
          slots: { customRender: 'labels' }
        },
        {
          title: '类型',
          dataIndex: 'type',
          key: 'type',
          width: '20%',
        },
        {
          title: 'Cluster IP',
          dataIndex: 'clusterIP',
          key: 'clusterIP',
          width: '10%',
        },
        {
          title: '操作',
          key: 'action',
          width: '10%',
          scopedSlots: { customRender: 'action' },
        },
      ],
      pvcColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '20%',
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '20%',
          slots: { customRender: 'labels' }
        },
        {
          title: '状态',
          dataIndex: 'status',
          key: 'status',
          width: '20%',
        },
        {
          title: '容量',
          dataIndex: 'capacity',
          key: 'capacity',
          width: '20%',
        },
        {
          title: '操作',
          key: 'action',
          width: '20%',
          slots: { customRender: 'action' },
        },
      ]
    };
  },
  methods: {
    async fetchK8sResources() {
      this.loading = true;
      try {
        await Promise.all([
          this.fetchConfigMaps(),
          this.fetchServices(),
          this.fetchPvcs()
        ]);
      } catch (error) {
        console.error('Error fetching K8s resources:', error);
      } finally {
        this.loading = false;
      }
    },
    async fetchConfigMaps() {
      try {
        const res = await this.$axiosGet(global.API.getK8sConfigMaps, {
          clusterId: this.clusterId,
          serviceName: this.serviceName
        });
        if (res.code === 200) {
          this.configMaps = res.data.map(item => ({
            name: item.name,
            labels: item.labels,// 直接使用 labels 对象，而不是转换为字符串
            time: item.time
          }));
        }
        console.log(this.configMaps.labels);
      } catch (error) {
        console.error('Error fetching config maps:', error);
      }
    },async fetchServices() {
      try {
        const res = await this.$axiosGet(global.API.getK8sServices, {
          clusterId: this.clusterId,
          serviceName: this.serviceName
        });
        if (res.code === 200) {
          this.services = res.data.map(item => ({
            name: item.name,
            labels: item.labels,// 直接使用 labels 对象，而不是转换为字符串
            type: item.type,
            clusterIP: item.clusterIP,
            time: item.time
          }));
        }
        console.log(this.configMaps.labels);
      } catch (error) {
        console.error('Error fetching config maps:', error);
      }
    },async fetchPvcs() {

    },
    formatTime(time) {
      return time ? this.$dayjs(time).format('YYYY-MM-DD HH:mm') : '-'
    },
    handleViewConfigMap(record) {
      console.log(record);
      this.$info({
        title: 'ConfigMap 详情',
        width: '80%',
        content: (
          <div>
            <p><strong>名称:</strong> {record.name}</p>
            <p><strong>标签:</strong> {record.labels}</p>
            <p><strong>创建时间:</strong> {new Date(record.creationTimestamp).toLocaleString()}</p>
          </div>
        ),
        onOk() {},
      });
    },
    handleEditConfigMap(record) {
      // TODO: 实现编辑ConfigMap的逻辑
    },
    handleViewService(record) {
      // TODO: 实现查看Service的逻辑
    },
    handleEditService(record) {
      // TODO: 实现编辑Service的逻辑
    },
    handleViewPvc(record) {
      // TODO: 实现查看PVC的逻辑
    },
    handleEditPvc(record) {
      // TODO: 实现编辑PVC的逻辑
    }
  },
  mounted() {
    if (this.serviceId) {
      this.fetchK8sResources();
    } else {
      console.error('serviceId is required to fetch K8s resources');
    }
  },
  watch: {
    serviceId(newVal) {
      if (newVal) {
        this.fetchK8sResources();
      }
    }
  }
};
</script>

<style scoped>
.k8s-config-container {
  padding: 24px;
}

.page-header {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
}

.header-icon-wrapper {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, #007AFF 0%, #5AC8FA 100%);
  border-radius: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
  box-shadow: 0 6px 12px rgba(0, 122, 255, 0.2);
}

.header-icon {
  font-size: 30px;
  color: white;
}

.header-content {
  flex: 1;
}

.title {
  font-size: 28px;
  font-weight: 600;
  margin: 0 0 4px 0;
  color: #000;
  letter-spacing: -0.5px;
  line-height: 1.2;
}

.subtitle {
  font-size: 16px;
  color: #666;
  margin: 0;
}

/* 新增样式 */
.config-map-table {
  width: 100%;
  border-collapse: collapse;
}

.config-map-table th,
.config-map-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #e8e8e8;
}

.config-map-table th {
  background-color: #f9f9f9;
  font-weight: bold;
}

.config-map-table .name-column {
  width: 30%;
}

.config-map-table .labels-column {
  width: 40%;
}

.config-map-table .time-column {
  width: 20%;
}

.config-map-table .action-column {
  width: 10%;
}

.label-badge {
  display: inline-block;
  padding: 4px 8px;
  margin: 2px;
  background-color: #f0f0f0;
  border-radius: 12px;
  font-size: 12px;
  color: #333;
}

.action-icons {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.action-button {
  color: #1890ff;
  padding: 4px 8px;
  border-radius: 4px;
  background: transparent;
  border: none;
  transition: all 0.2s ease;
  font-size: 12px;

  &:hover {
    background: rgba(24, 144, 255, 0.1);
  }

  &:active {
    transform: scale(0.95);
  }
}
</style>