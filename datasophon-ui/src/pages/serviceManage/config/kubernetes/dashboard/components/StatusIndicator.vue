<template>
  <span :class="statusClass" class="status-indicator"></span>
</template>

<script>
export default {
  name: 'StatusIndicator',
  props: {
    /**
     * 资源对象，可以是deployment、pod、persistentVolume等
     */
    resource: {
      type: Object,
      required: true
    },
    /**
     * 资源类型，用于确定状态判断逻辑
     * 可选值: 'deployment', 'pod', 'persistentVolume', 'persistentVolumeClaim', 'replicaSet', 'service', 'daemonset'.
     */
    resourceType: {
      type: String,
      required: true,
      validator: function(value) {
        return ['deployment', 'pod', 'persistentVolume', 'persistentVolumeClaim', 'replicaSet', 'service', 'daemonset'].includes(value);
      }
    }
  },
  computed: {
    statusClass() {
      const classNames = ['status-dot'];
      
      switch(this.resourceType) {
        case 'deployment':
          return this.getDeploymentStatusClass(this.resource);
        case 'pod':
          return this.getPodStatusClass(this.resource);
        case 'persistentVolume':
          return this.getPersistentVolumeStatusClass(this.resource);
        case 'persistentVolumeClaim':
          return this.getPersistentVolumeClaimStatusClass(this.resource);
        case 'replicaSet':
          return this.getReplicaSetStatusClass(this.resource);
        case 'service':
          return this.getServiceStatusClass(this.resource);
        case 'daemonset':
          return this.getDaemonSetStatusClass(this.resource);
        default:
          return classNames.concat(['status-unknown']).join(' ');
      }
    }
  },
  methods: {
    getDeploymentStatusClass(resource) {
      const classNames = ['status-dot'];
      if (resource?.pods?.running > 0) classNames.push('status-running');
      if (resource?.pods?.pending > 0) classNames.push('status-warning');
      if (resource?.pods?.failed > 0) classNames.push('status-danger');
      if (!resource?.pods || (!resource?.pods.running && !resource?.pods.pending && !resource?.pods.failed))
        classNames.push('status-unknown');
      return classNames.join(' ');
    },
    
    getPodStatusClass(resource) {
      const classNames = ['status-dot'];
      if (!resource || !resource.status) {
        return classNames.concat(['status-unknown']).join(' ');
      }
      
      const status = resource.status.toLowerCase();
      if (status.includes('running')) {
        classNames.push('status-running');
      } else if (status.includes('pending') || status.includes('waiting')) {
        classNames.push('status-warning');
      } else if (status.includes('error') || status.includes('failed') || status.includes('crashloopbackoff')) {
        classNames.push('status-danger');
      } else if (status.includes('completed') || status.includes('succeeded')) {
        classNames.push('succeeded');
      } else if (status.includes('terminating')) {
        classNames.push('terminating');
      } else {
        classNames.push('status-unknown');
      }
      
      return classNames.join(' ');
    },
    
    getPersistentVolumeStatusClass(resource) {
      const classNames = ['status-dot'];
      if (!resource || !resource.status) {
        return classNames.concat(['status-unknown']).join(' ');
      }
      
      const status = resource.status.toLowerCase();
      if (status === 'bound') {
        classNames.push('status-running');
      } else if (status === 'available') {
        classNames.push('status-warning');
      } else if (status === 'released') {
        classNames.push('status-info');
      } else if (status === 'failed') {
        classNames.push('status-danger');
      } else {
        classNames.push('status-unknown');
      }
      
      return classNames.join(' ');
    },
    
    getPersistentVolumeClaimStatusClass(resource) {
      const classNames = ['status-dot'];
      if (!resource || !resource.status) {
        return classNames.concat(['status-unknown']).join(' ');
      }
      
      const status = resource.status.toLowerCase();
      if (status === 'bound') {
        classNames.push('status-running');
      } else if (status === 'pending') {
        classNames.push('status-warning');
      } else if (status === 'lost') {
        classNames.push('status-danger');
      } else {
        classNames.push('status-unknown');
      }
      
      return classNames.join(' ');
    },
    
    getReplicaSetStatusClass(resource) {
      const classNames = ['status-dot'];
      if (resource?.podInfo?.running > 0) classNames.push('status-running');
      if (resource?.podInfo?.pending > 0) classNames.push('status-warning');
      if (resource?.podInfo?.failed > 0) classNames.push('status-danger');
      if (!resource?.podInfo || (!resource?.podInfo.running && !resource?.podInfo.pending && !resource?.podInfo.failed))
        classNames.push('status-unknown');
      return classNames.join(' ');
    },
    
    getServiceStatusClass(resource) {
      const classNames = ['status-dot'];
      // 服务通常只要存在就是可用的，所以默认为running状态
      classNames.push('status-running');
      return classNames.join(' ');
    },
    
    getDaemonSetStatusClass(resource) {
      const classNames = ['status-dot'];
      // DaemonSet状态逻辑与ReplicaSet类似，基于pod信息
      if (resource?.podInfo?.running > 0) classNames.push('status-running');
      if (resource?.podInfo?.pending > 0) classNames.push('status-warning');
      if (resource?.podInfo?.failed > 0) classNames.push('status-danger');
      if (!resource?.podInfo || (!resource?.podInfo.running && !resource?.podInfo.pending && !resource?.podInfo.failed))
        classNames.push('status-unknown');
      return classNames.join(' ');
    }
  }
};
</script>

<style lang="less" scoped>
/* 组件使用公共样式，不需要在这里定义重复的样式 */
</style> 