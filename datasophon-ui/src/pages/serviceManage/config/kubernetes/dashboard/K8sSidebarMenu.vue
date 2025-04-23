<template>
  <div class="sidebar-menu">
    <!-- 工作负载分组 -->
    <div class="menu-group">
      <div class="group-title">工作负载</div>
      <div class="menu-item" :class="{ active: activeResource === 'cronjobs' }" @click="handleResourceClick('cronjobs')">
        <span class="item-text">Cron Jobs</span>
        <span class="item-count">{{ resourceCounts.cronJobs || 0 }}</span>
      </div>
      <div class="menu-item" :class="{ active: activeResource === 'daemonsets' }" @click="handleResourceClick('daemonsets')">
        <span class="item-text">Daemon Sets</span>
        <span class="item-count">{{ resourceCounts.daemonSets || 0 }}</span>
      </div>
      <div class="menu-item" :class="{ active: activeResource === 'deployments' }" @click="handleResourceClick('deployments')">
        <span class="item-text">Deployments</span>
        <span class="item-count">{{ resourceCounts.deployments || 0 }}</span>
      </div>
      <div class="menu-item" :class="{ active: activeResource === 'jobs' }" @click="handleResourceClick('jobs')">
        <span class="item-text">Jobs</span>
        <span class="item-count">{{ resourceCounts.jobs || 0 }}</span>
      </div>
      <div class="menu-item" :class="{ active: activeResource === 'pods' }" @click="handleResourceClick('pods')">
        <span class="item-text">Pods</span>
        <span class="item-count">{{ resourceCounts.pods || 0 }}</span>
      </div>
      <div class="menu-item" :class="{ active: activeResource === 'replicasets' }" @click="handleResourceClick('replicasets')">
        <span class="item-text">Replica Sets</span>
        <span class="item-count">{{ resourceCounts.replicaSets || 0 }}</span>
      </div>
      <div class="menu-item" :class="{ active: activeResource === 'replicationcontrollers' }" @click="handleResourceClick('replicationcontrollers')">
        <span class="item-text">Replication Controllers</span>
        <span class="item-count">{{ resourceCounts.replicationControllers || 0 }}</span>
      </div>
      <div class="menu-item" :class="{ active: activeResource === 'statefulsets' }" @click="handleResourceClick('statefulsets')">
        <span class="item-text">Stateful Sets</span>
        <span class="item-count">{{ resourceCounts.statefulSets || 0 }}</span>
      </div>
    </div>
    
    <div class="menu-group">
      <div class="group-title">服务</div>
      <div class="menu-item" :class="{ active: activeResource === 'service' }" @click="handleResourceClick('service')">
        <span class="item-text">Services</span>
        <span class="item-count">{{ resourceCounts.services || 0 }}</span>
      </div>
      <div class="menu-item" :class="{ active: activeResource === 'ingress' }" @click="handleResourceClick('ingress')">
        <span class="item-text">Ingresses</span>
        <span class="item-count">{{ resourceCounts.ingresses || 0 }}</span>
      </div>
      <div class="menu-item" :class="{ active: activeResource === 'ingressclass' }" @click="handleResourceClick('ingressclass')">
        <span class="item-text">Ingress Classes</span>
        <span class="item-count">{{ resourceCounts.ingressClasses || 0 }}</span>
      </div>
    </div>
    
    <div class="menu-group">
      <div class="group-title">配置和存储</div>
      <div class="menu-item" :class="{ active: activeResource === 'configmap' }" @click="handleResourceClick('configmap')">
        <span class="item-text">Config Maps</span>
        <span class="item-count">{{ resourceCounts.configMaps || 0 }}</span>
      </div>
      <div class="menu-item" :class="{ active: activeResource === 'secret' }" @click="handleResourceClick('secret')">
        <span class="item-text">Secrets</span>
        <span class="item-count">{{ resourceCounts.secrets || 0 }}</span>
      </div>
      <div class="menu-item" :class="{ active: activeResource === 'pv' }" @click="handleResourceClick('pv')">
        <span class="item-text">Persistent Volumes</span>
        <span class="item-count">{{ resourceCounts.persistentVolumes || 0 }}</span>
      </div>
      <div class="menu-item" :class="{ active: activeResource === 'pvc' }" @click="handleResourceClick('pvc')">
        <span class="item-text">Persistent Volume Claims</span>
        <span class="item-count">{{ resourceCounts.persistentVolumeClaims || 0 }}</span>
      </div>
      <div class="menu-item" :class="{ active: activeResource === 'storageclass' }" @click="handleResourceClick('storageclass')">
        <span class="item-text">Storage Classes</span>
        <span class="item-count">{{ resourceCounts.storageClasses || 0 }}</span>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'K8sSidebarMenu',
  props: {
    activeResource: {
      type: String,
      required: true
    },
    resourceCounts: {
      type: Object,
      required: true,
      default: () => ({})
    }
  },
  methods: {
    handleResourceClick(resource) {
      this.$emit('resource-change', resource);
    }
  }
};
</script>

<style lang="less" scoped>
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
</style> 