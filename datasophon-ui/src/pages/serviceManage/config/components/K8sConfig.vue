<template>
  <div class="k8s-config-container">
    <!-- 顶部图标和标题区域 -->
    <div class="page-header">
      <div class="header-icon-wrapper">
        <div class="kubernetes-logo"></div>
      </div>
      <div class="header-content">
        <h2 class="title">{{ serviceName }} K8s 配置</h2>
        <p class="subtitle">管理{{ serviceName }}服务的K8s配置</p>
      </div>
    </div>

    <a-tabs>
      <a-tab-pane key="configmap" tab="ConfigMap">
        <a-spin :spinning="loading">
          <a-table
            :columns="configMapColumns"
            :dataSource="configMaps"
            :pagination="false"
          >
            <template #action="{ record }">
              <a @click="handleViewConfigMap(record)">查看</a>
              <a-divider type="vertical" />
              <a @click="handleEditConfigMap(record)">编辑</a>
            </template>
            <template #labels="{ text }">
              <div class="tag-list" v-if="text && Object.keys(text).length > 0">
                222
                {{ key }}: {{ value }}
              </div>
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
              <a @click="handleViewService(record)">查看</a>
              <a-divider type="vertical" />
              <a @click="handleEditService(record)">编辑</a>
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
          width: '20%',
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
          width: '20%',
        },
        {
          title: '操作',
          key: 'action',
          width: '10%',
          slots: { customRender: 'action' },
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
          width: '30%',
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
          width: '10%',
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
          // 确认数据结构包含 labels 和 time 字段
          this.configMaps = res.data.map(item => ({
            name: item.name,
            labels: item.labels || {}, // 确保 labels 存在
            time: item.time // 确保时间字段正确
          }));
        }
      } catch (error) {
        console.error('Error fetching config maps:', error);
      }
    },
    async fetchServices() {
      try {
        const res = await this.$axiosGet(global.API.getK8sServices, {
          clusterId: this.clusterId
        });
        if (res.code === 200) {
          this.services = res.data;
        } else {
          console.error('Failed to fetch services:', res.msg);
        }
      } catch (error) {
        console.error('Error fetching services:', error);
      }
    },
    async fetchPvcs() {
      try {
        const res = await this.$axiosGet(global.API.getK8sPvcs, {
          clusterId: this.clusterId
        });
        if (res.code === 200) {
          this.pvcs = res.data;
        } else {
          console.error('Failed to fetch PVCs:', res.msg);
        }
      } catch (error) {
        console.error('Error fetching PVCs:', error);
      }
    },
    handleViewConfigMap(record) {
      console.log(record);
      this.$info({
        title: 'ConfigMap 详情',
        width: '80%',
        content: (
          <div>
            <p><strong>名称:</strong> {record.name}</p>
            <p><strong>标签:</strong> {Object.entries(record.labels || {}).map(([key, value]) => `${key}=${value}`).join(', ')}</p>
            <p><strong>创建时间:</strong> {new Date(record.creationTimestamp).toLocaleString()}</p>
          </div>
        ),
        onOk() {},
      });
    },
    formatTime(time) {
      return time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '-'
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
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
  position: relative;
  transition: all 0.3s ease;
}

.header-icon-wrapper:hover {
  transform: translateY(-2px);
}

.kubernetes-logo {
  width: 60px;
  height: 60px;
  background-image: url('../../../../assets/images/kubernetes-logo.svg');
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
  transition: all 0.3s ease;
  filter: drop-shadow(0 6px 10px rgba(50, 108, 229, 0.3));
}

.header-icon-wrapper:hover .kubernetes-logo {
  transform: scale(1.1);
  filter: drop-shadow(0 8px 15px rgba(50, 108, 229, 0.4));
}

.k8s-logo-container {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 60px;
  height: 60px;
  background: #326CE5;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(50, 108, 229, 0.2);
}
</style>