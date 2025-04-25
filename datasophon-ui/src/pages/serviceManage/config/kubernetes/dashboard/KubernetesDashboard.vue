<template>
  <div class="k8s-config-container">
    <!-- 顶部标题区域 -->
    <div class="page-header">
      <div class="header-icon-wrapper">
        <div class="kubernetes-logo"></div>
      </div>
      <div class="header-content">
        <h2 class="title">{{ serviceLabel || serviceName }} Kubernetes 仪表盘</h2>
        <p class="subtitle">查看和管理{{ serviceLabel || serviceName }}服务的Kubernetes资源</p>
      </div>
      <!-- 命名空间选择器 -->
      <div class="namespace-selector">
        <span class="selector-label">命名空间:</span>
        <a-select
          v-model="selectedNamespace"
          placeholder="请选择命名空间"
          :loading="namespacesLoading"
          @change="handleNamespaceChange"
          style="width: 200px"
        >
          <a-select-option value="all">所有命名空间</a-select-option>
          <a-select-option v-for="ns in namespaces" :key="ns.name" :value="ns.name">
            {{ ns.name }}
          </a-select-option>
        </a-select>
      </div>
    </div>

    <div class="k8s-dashboard-layout">
      <!-- 左侧导航菜单 -->
      <k8s-sidebar-menu 
        :active-resource="activeResource"
        :resource-counts="resourceCounts"
        @resource-change="handleResourceChange"
      />

      <!-- 右侧内容区域 -->
      <div class="content-area">
        <!-- ConfigMap列表 -->
        <config-map-dashboard
          v-if="activeResource === 'configmap'"
          :clusterId="clusterId"
          :selectedNamespace="selectedNamespace"
        />

        <!-- Deployment仪表板 -->
        <deployment-dashboard
          v-if="activeResource === 'deployments'"
          :clusterId="clusterId"
          :serviceId="serviceId"
          :selectedNamespace="selectedNamespace"
        />

        <!-- 其他资源列表 -->
        <!-- 这里将添加其他资源组件 -->
        
        <!-- Service列表 -->
        <services-dashboard 
          v-if="activeResource === 'service'"
          :clusterId="clusterId"
          :selectedNamespace="selectedNamespace"
        />

        <!-- Pod列表 -->
        <pods-dashboard
          v-if="activeResource === 'pods'" 
          :clusterId="clusterId"
          :selectedNamespace="selectedNamespace"
        />

        <!-- ReplicaSets列表 -->
        <replica-sets-dashboard
          v-if="activeResource === 'replicasets'"
          :clusterId="clusterId"
          :serviceId="serviceId"
          :selectedNamespace="selectedNamespace"
        />

        <!-- IngressClasses列表 -->
        <ingress-classes-dashboard
          v-if="activeResource === 'ingressclass'"
          :clusterId="clusterId"
          :selectedNamespace="selectedNamespace"
        />

        <!-- StorageClasses列表 -->
        <storage-classes-dashboard
          v-if="activeResource === 'storageclass'"
          :clusterId="clusterId"
        />

        <!-- PersistentVolumeClaims列表 -->
        <persistent-volume-claims-dashboard
          v-if="activeResource === 'pvc'"
          :clusterId="clusterId"
          :selectedNamespace="selectedNamespace"
        />

        <!-- PersistentVolumes列表 -->
        <persistent-volumes-dashboard
          v-if="activeResource === 'pv'"
          :clusterId="clusterId"
          :selectedNamespace="selectedNamespace"
        />

        <!-- CronJobs列表 -->
        <cron-jobs-dashboard
          v-if="activeResource === 'cronjobs'"
          :clusterId="clusterId"
          :selectedNamespace="selectedNamespace"
        />

        <!-- 其他资源列表保持不变... -->
        <!-- ... existing resources ... -->
      </div>
    </div>
  </div>
</template>

<script>
import { defineComponent, ref, reactive } from 'vue'
import dayjs from 'dayjs'
// 移除对已删除api.js的导入
// import API from '@/api';

// 导入拆分的组件
import K8sSidebarMenu from './K8sSidebarMenu.vue';
import ConfigMapDashboard from './ConfigMapDashboard.vue';
import DeploymentDashboard from './DeploymentDashboard.vue';
import ReplicaSetsDashboard from './ReplicaSetsDashboard.vue';
import IngressClassesDashboard from './IngressClassesDashboard.vue';
import StorageClassesDashboard from './StorageClassesDashboard.vue';
import PersistentVolumeClaimsDashboard from './PersistentVolumeClaimsDashboard.vue';
import PersistentVolumesDashboard from './PersistentVolumesDashboard.vue';
import PodsDashboard from './PodsDashboard.vue'
import ServicesDashboard from './ServicesDashboard.vue'
import CronJobsDashboard from './CronJobsDashboard.vue'

export default defineComponent({
  name: 'KubernetesDashboard',
  components: {
    K8sSidebarMenu,
    ConfigMapDashboard,
    DeploymentDashboard,
    ReplicaSetsDashboard,
    IngressClassesDashboard,
    StorageClassesDashboard,
    PersistentVolumeClaimsDashboard,
    PersistentVolumesDashboard,
    PodsDashboard,
    ServicesDashboard,
    CronJobsDashboard,
  },
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
    serviceLabel: {
      type: String,
      default: ''
    },
    clusterId: {
      type: Number,
      required: true,
      default: 1
    }
  },
  data() {
    return {
      namespaces: [],
      selectedNamespace: 'datasophon', // 固定使用datasophon命名空间
      namespacesLoading: false,
      activeResource: 'configmap', // 默认显示ConfigMap
      loading: false,
      
      // 资源计数
      resourceStats: {},
      resourceCounts: {},
      
      // 不再自定义API路径，使用全局API
    };
  },
  created() {
    // 不再需要初始化API路径
  },
  methods: {
    // 移除API路径初始化方法
    async fetchK8sResources() {
      // 获取命名空间列表
      this.fetchNamespaces();
        
      // 根据当前选中的资源类型加载数据
      this.updateResourceData();
    },
    // 获取命名空间
    async fetchNamespaces() {
      this.namespacesLoading = true;
      try {
        // 使用全局API对象替代导入的API
        const res = await this.$axiosGet(global.API.getK8sNamespaces, {
          clusterId: this.clusterId
        });
        
        if (res.code === 200 && res.data) {
          // 处理后端返回的命名空间数据
          this.namespaces = res.data.namespaces || [];
          
          // 设置默认命名空间和是否显示选择器
          if (res.data.defaultNamespace) {
            this.selectedNamespace = res.data.defaultNamespace;
          }
          
          // 根据showNamespaceSelector决定是否显示选择器
          if (res.data.showNamespaceSelector === false) {
            // 如果不显示选择器，可以通过样式隐藏
            document.querySelector('.namespace-selector').style.display = 'none';
          }
          
          // 加载资源统计信息
          this.fetchResourceStats();
        } else {
          this.$message.error('获取命名空间失败：' + (res.msg || '未知错误'));
        }
      } catch (error) {
        console.error('获取命名空间出错：', error);
        this.$message.error('获取命名空间失败：' + (error.message || '未知错误'));
      } finally {
        this.namespacesLoading = false;
      }
    },
    // 处理命名空间变更
    handleNamespaceChange(value) {
      this.selectedNamespace = value;
      // 刷新所有资源显示
      this.fetchResourceStats();
      // 更新当前显示的资源
      this.$nextTick(() => {
        this.updateResourceData();
      });
    },
    
    // 资源类型变化处理
    handleResourceChange(resource) {
      this.activeResource = resource;
      this.updateResourceData();
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
    
    // 获取几天前
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
    
    // 更新资源数据
    updateResourceData() {
      // 子组件会在其挂载时自动加载其特定的资源
      console.log(`Updating resource data for ${this.activeResource} in namespace ${this.selectedNamespace}`);
      
      // 触发资源统计信息刷新
      this.fetchResourceStats();
    },
    
    // 获取资源统计信息
    async fetchResourceStats() {
      try {
        const apiUrl = global.API.getK8sResourceStats;
        const params = {
          clusterId: this.clusterId,
          serviceId: this.serviceId,
          namespace: this.selectedNamespace === 'all' ? null : this.selectedNamespace
        };
        
        const res = await this.$axiosGet(apiUrl, params);
        
        if (res.code === 200 && res.data) {
          this.resourceStats = res.data;
          const data = res.data;
          this.resourceCounts = {
            deployments: data.deployments || 0,
            pods: data.pods || 0,
            services: data.services || 0,
            configMaps: data.configMaps || 0,
            ingresses: data.ingresses || 0,
            ingressClasses: data.ingressClasses || 0,
            secrets: data.secrets || 0,
            persistentVolumes: data.persistentVolumes || 0,
            persistentVolumeClaims: data.persistentVolumeClaims || 0,
            storageClasses: data.storageClasses || 0,
            cronJobs: data.cronJobs || 0,
            daemonSets: data.daemonSets || 0,
            jobs: data.jobs || 0,
            replicaSets: data.replicaSets || 0,
            replicationControllers: data.replicationControllers || 0,
            statefulSets: data.statefulSets || 0
          };
          console.log('资源统计数据加载成功:', this.resourceCounts);
        } else {
          this.$message.error('获取资源统计失败: ' + (res.msg || '未知错误'));
          this.resourceCounts = {}; // 清空数据
        }
      } catch (error) {
        console.error('获取资源统计失败:', error);
        this.$message.error('获取资源统计失败: ' + (error.message || '未知错误'));
        this.resourceCounts = {}; // 清空数据
      }
    },
    
    // 初始化空的资源计数
    initEmptyResourceCounts() {
      this.resourceCounts = {
        deployments: 0,
        pods: 0,
        services: 0,
        configMaps: 0,
        ingresses: 0,
        ingressClasses: 0,
        secrets: 0,
        persistentVolumes: 0,
        persistentVolumeClaims: 0,
        storageClasses: 0,
        cronJobs: 0,
        daemonSets: 0,
        jobs: 0,
        replicaSets: 0,
        replicationControllers: 0,
        statefulSets: 0
      };
    },
    
    // 保留其他需要的方法
    // ... 保留其他未迁移的资源加载方法 ...
  },
  mounted() {
    if (this.serviceId) {
      // 首先获取所有资源统计数据
      this.fetchResourceStats();
      // 然后获取详细资源数据
      this.fetchK8sResources();
    } else {
      console.error('serviceId is required to fetch K8s resources');
    }
  },
  beforeDestroy() {
    // 清理定时器
    if (this.chartsInterval) {
      clearInterval(this.chartsInterval);
      this.chartsInterval = null;
    }
    
    // 清理图表资源
    if (this.cpuChart && !this.cpuChart.isDisposed()) {
      this.cpuChart.dispose();
      this.cpuChart = null;
    }
    if (this.memoryChart && !this.memoryChart.isDisposed()) {
      this.memoryChart.dispose();
      this.memoryChart = null;
    }
    
    // 移除窗口大小变化监听
    window.removeEventListener('resize', this.resizeCharts);
  },
  watch: {
    serviceId(newVal) {
      if (newVal) {
        this.fetchK8sResources();
      }
    },
    activeResource(newVal, oldVal) {
      if (newVal !== oldVal) {
        this.updateResourceData();
      }
    },
    selectedNamespace(newVal, oldVal) {
      if (newVal !== oldVal) {
        this.updateResourceData();
      }
    }
  }
});
</script>

<style lang="less" scoped>
.k8s-config-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
  
  // 页面头部样式
.page-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px 0;
  
  .header-icon-wrapper {
    margin-right: 16px;
    flex-shrink: 0;
    
    .kubernetes-logo {
      width: 40px;
      height: 40px;
      background-image: url(~@/assets/images/kubernetes-logo.svg);
      background-size: contain;
      background-repeat: no-repeat;
      background-position: center;
    }
  }
  
  .header-content {
    flex-grow: 1;
    overflow: hidden;
    
    .title {
      font-size: 20px;
      margin: 0 0 4px 0;
      color: #333;
      font-weight: 500;
    }
    
    .subtitle {
      color: #666;
      margin: 0;
      font-size: 14px;
    }
  }
  
  .namespace-selector {
    margin-left: 20px;
    display: flex;
    align-items: center;
    
    .selector-label {
      margin-right: 8px;
      color: #666;
      white-space: nowrap;
    }
  }
}
  
  // 整体仪表盘布局
.k8s-dashboard-layout {
  display: flex;
    flex: 1;
    min-height: calc(100vh - 185px);
    padding: 0 24px 16px;
    
    // 左侧导航样式
.sidebar-menu {
      width: 280px;
      min-width: 280px;
      margin-right: 16px;
      background-color: #fff;
      border-radius: 4px;
      box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);

.menu-group {
        padding: 12px 0;
        border-bottom: 1px solid #f0f0f0;
        
        &:last-child {
          border-bottom: none;
}

.group-title {
  padding: 8px 16px;
          color: #999;
          font-size: 12px;
          font-weight: 500;
  text-transform: uppercase;
          letter-spacing: 0.5px;
}

.menu-item {
  display: flex;
          justify-content: space-between;
  align-items: center;
          padding: 10px 16px;
          color: #333;
  font-size: 14px;
  cursor: pointer;
          transition: all 0.2s;
          
          &:hover {
            background-color: #f5f7fa;
}

          &.active {
            background-color: #e6f7ff;
            color: #1890ff;
            border-right: 3px solid #1890ff;
            
            .item-count {
              background-color: #1890ff;
              color: #fff;
            }
}

.item-text {
  flex: 1;
}

.item-count {
            display: inline-block;
            min-width: 24px;
            height: 24px;
            line-height: 24px;
            text-align: center;
            padding: 0 6px;
            border-radius: 12px;
            background-color: #f0f0f0;
            color: #666;
  font-size: 12px;
          }
        }
      }
}

    // 右侧内容区域样式
.content-area {
  flex: 1;
      
      .resource-list {
        height: 100%;

.resource-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 16px 0;
  margin-bottom: 16px;

          h3 {
  margin: 0;
            font-size: 18px;
            font-weight: 500;
            color: #333;
}
        }
      }
    }
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
}

// 图表相关样式
.k8s-dashboard-charts {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 16px;

  .k8s-chart-card {
  flex: 1;
    min-width: 400px;
    height: 250px;
  border-radius: 4px;
    background-color: #fff;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  overflow: hidden;
    position: relative;

    .k8s-chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
      height: 48px;
      padding: 0 16px;
      background-color: #f7f7f7;
      border-bottom: 1px solid #e8e8e8;
      
      .k8s-chart-title {
  font-size: 14px;
  font-weight: 500;
        color: #333;
}

      .k8s-chart-actions {
        .k8s-action-icon {
          cursor: pointer;
          color: #999;
          transition: color 0.3s;
          
          &:hover {
            color: #1890ff;
          }
        }
      }
    }
    
    .k8s-chart-content {
      position: relative;
      height: calc(100% - 48px);
      padding: 10px 5px 10px 15px;
      
      .k8s-chart-y-label {
        position: absolute;
        left: -25px;
        top: 50%;
        transform: rotate(-90deg);
        transform-origin: center;
        font-size: 12px;
        color: #666;
        white-space: nowrap;
        z-index: 2;
        width: 80px;
        text-align: center;
}

.chart {
  width: 100%;
  height: 100%;
}
    }
  }
}

/* 响应式布局 */
@media screen and (max-width: 1200px) {
  .k8s-dashboard-container {
    .k8s-dashboard-charts {
      .k8s-chart-card {
        min-width: 300px;
      }
    }
  }
}

@media screen and (max-width: 768px) {
  .k8s-dashboard-container {
    .k8s-dashboard-charts {
      flex-direction: column;
      
      .k8s-chart-card {
        width: 100%;
      }
    }
  }
}

/* 表格通用样式 */
.k8s-table {
  .status-dot {
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
  
  .name-cell, .image-cell, .pods-display {
    white-space: nowrap;
  overflow: hidden;
    text-overflow: ellipsis;
}

  .tag-list {
  display: flex;
    flex-wrap: wrap;
    
    .label-tag {
      margin: 2px;
    }
  }
  
  .action-buttons {
    white-space: nowrap;
    
    a {
      color: #1890ff;
      
      &:hover {
        color: #40a9ff;
      }
    }
  }
}

// 额外的表格样式修复
:deep(.ant-table-thead > tr > th) {
  background-color: #fafafa;
  font-weight: 500;
  color: #333;
}

:deep(.ant-table-tbody > tr:hover > td) {
  background-color: #e6f7ff;
}

:deep(.ant-tag) {
  margin-right: 4px;
  margin-bottom: 4px;
  border-radius: 2px;
}

:deep(.ant-spin-container) {
  height: 100%;
}

:deep(.ant-empty-image) {
  margin-top: 32px;
}

:deep(.ant-table-placeholder) {
  height: 100%;
}

.k8s-dashboard {
  &-container {
    min-height: 600px;
    background-color: #fff;
    border-radius: 4px;
    padding: 16px;

    .tabs-container {
      margin-top: 16px;
}

    .service-table {
      margin-top: 16px;
      
      // 状态点样式
.status-dot {
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

        &.status-unknown {
          background-color: #d9d9d9;
        }
      }
      
      // 标签样式
      .labels-container {
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
      
      // 表格头部样式
      .ant-table-thead > tr > th {
        background-color: #f5f7fa;
  font-weight: 500;
        color: #262626;
}

      // 表格单元格样式
      .ant-table-tbody > tr > td {
        padding: 10px 16px;
        word-break: break-word;
      }
      
      // 创建时间列样式
      .ant-table-row td:last-child {
  white-space: nowrap;
      }
    }
  }
}

.service-detail-dialog {
  width: 80%;
  max-width: 900px;
}

// 端点样式
.internal-endpoint, .external-endpoint {
  padding: 2px 0;
  word-break: break-all;
}

.external-endpoint {
  display: flex;
  align-items: center;
  
  .external-icon {
    margin-left: 4px;
    font-size: 12px;
  }
}

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 8px;
}

// 表格样式优化
.ant-table {
  table-layout: fixed;
  
  .ant-table-tbody > tr > td {
    white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
    word-break: keep-all;
  }
  
  // 确保创建时间列正确显示
  .ant-table-row td:last-child {
  white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

// 为单元格内容添加工具提示
.cell-content {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: inline-block;
  max-width: 100%;
}

// 修复表格在Safari和Firefox中的显示问题
.k8s-config-container {
  .ant-table-wrapper {
    overflow-x: auto;
    
    .ant-table {
      min-width: 1000px; // 确保表格内容有足够的显示空间
      
      // 修复表头标题竖向显示问题
      .ant-table-thead > tr > th {
        white-space: nowrap !important;
        text-align: left !important;
        min-width: 100px; // 确保每列有足够宽度显示标题
      }
      
      // 特别处理创建时间列的表头
      .ant-table-thead > tr > th:last-child {
        min-width: 100px;
        white-space: nowrap !important;
        text-align: left !important;
        writing-mode: horizontal-tb !important; // 强制水平文本
        transform: none !important; // 防止任何旋转
      }
    }
    
    .ant-table-thead > tr > th,
    .ant-table-tbody > tr > td {
      padding: 10px 8px;
      vertical-align: middle;
    }
    
    .ant-table-column-title {
      word-break: keep-all;
      white-space: nowrap;
      text-align: left !important;
      writing-mode: horizontal-tb !important; // 强制水平文本
      transform: none !important; // 防止任何旋转
      display: inline-block !important; // 确保标题按照预期显示
    }
  }
}

// 内部端点样式
.internal-endpoint {
  padding: 2px 0;
  word-break: keep-all;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

// 外部端点样式
.external-endpoint {
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

.normal-column-header {
  .ant-table-column-title {
    writing-mode: horizontal-tb !important;
    transform: none !important;
    white-space: nowrap !important;
    min-width: 90px !important;
    display: inline-block !important;
  }
}

// 修改全局表头样式
.k8s-config-container {
  .ant-table-column-has-sorters {
    .ant-table-column-title {
      writing-mode: horizontal-tb !important;
      transform: none !important;
      white-space: nowrap !important;
    }
  }
}

/* 确保所有表头正常水平显示，不会竖直旋转 */
:deep(.normal-column-header) {
  .ant-table-column-title {
    display: inline-block !important;
    white-space: nowrap !important;
    overflow: visible !important;
    writing-mode: horizontal-tb !important;
    min-width: 90px !important;
    transform: none !important;
  }
}

/* 修复表格内容样式 */
:deep(.ant-table-tbody) {
  td {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

/* 全局修复表头标题垂直显示问题 */
:deep(.ant-table-thead > tr > th) {
  white-space: nowrap !important;
  text-align: left !important;
  
  .ant-table-column-title {
    display: inline-block !important;
    white-space: nowrap !important;
    writing-mode: horizontal-tb !important;
    transform: none !important;
    word-break: keep-all !important;
    min-width: auto !important;
    width: auto !important;
    max-width: 100% !important;
  }
}

:deep(.ant-table-column-has-sorters) {
  .ant-table-column-sorter {
    display: inline-flex !important;
    align-items: center !important;
    vertical-align: middle !important;
    margin-left: 4px !important;
  }
}

/* 特别针对"创建时间"列的表头 */
:deep(.ant-table-thead > tr > th.normal-column-header) {
  .ant-table-column-title {
    display: inline-block !important;
    white-space: nowrap !important;
    writing-mode: horizontal-tb !important;
    transform: none !important;
    word-break: keep-all !important;
  }
}

/* 修复表格内容溢出问题 */
:deep(.ant-table-body) {
  overflow-x: auto !important;
}

:deep(.ant-table-tbody > tr > td) {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: keep-all;
}
</style>